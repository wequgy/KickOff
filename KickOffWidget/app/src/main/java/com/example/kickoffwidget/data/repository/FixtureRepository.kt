package com.example.kickoffwidget.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.util.Log
import com.example.kickoffwidget.Config
import com.example.kickoffwidget.data.api.ApiFootballService
import com.example.kickoffwidget.data.local.LocalSettings
import com.example.kickoffwidget.data.models.MatchItem
import com.example.kickoffwidget.data.models.MatchCardState
import com.example.kickoffwidget.data.models.MatchDetail
import com.example.kickoffwidget.utils.BitmapUtils
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.time.Instant
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class FixtureRepository(private val context: Context) {
    private val TAG = "FixtureRepository"
    private val localSettings = LocalSettings(context)
    private val json = Json { ignoreUnknownKeys = true }
    
    private val apiService: ApiFootballService by lazy {
        val okHttpClient = OkHttpClient.Builder().build()
        Retrofit.Builder()
            .baseUrl(Config.API_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(ApiFootballService::class.java)
    }

    private fun parseUtcDate(dateStr: String): Long {
        return try {
            Instant.parse(dateStr).toEpochMilli()
        } catch (e: Exception) {
            0L
        }
    }

    suspend fun fetchNextMatchCard(): MatchCardState = withContext(Dispatchers.IO) {
        val apiKey = localSettings.getApiKey()
        if (apiKey.isNullOrBlank()) {
            Log.w(TAG, "API Key is missing")
            return@withContext MatchCardState.NoUpcomingMatch
        }

        try {
            Log.d(TAG, "Fetching fixtures from football-data.org...")
            val response = apiService.getMatches(
                apiKey = apiKey,
                code = Config.COMPETITION_CODE,
                season = Config.SEASON
            )
            
            val matches = response.matches
            if (matches.isEmpty()) {
                Log.i(TAG, "No matches returned from API. Generating mock match...")
                return@withContext generateMockMatch()
            }

            // 1. Calculate time windows for the 12:00 PM to 12:00 PM cycle
            val nowMillis = System.currentTimeMillis()
            val now = Instant.ofEpochMilli(nowMillis)
                .atZone(java.time.ZoneId.of("Asia/Kolkata"))
            
            val today12PM = now.withHour(12).withMinute(0).withSecond(0).withNano(0)
            
            var (start, end) = if (now.isBefore(today12PM)) {
                today12PM.minusDays(1) to today12PM
            } else {
                today12PM to today12PM.plusDays(1)
            }
            
            var startMillis = start.toInstant().toEpochMilli()
            var endMillis = end.toInstant().toEpochMilli()
            
            var matchesInWindow = matches.filter {
                val t = parseUtcDate(it.utcDate)
                t in startMillis until endMillis
            }
            
            // 2. If all matches in this window are finished, transition to next cycle early
            if (matchesInWindow.isNotEmpty() && matchesInWindow.all { it.status == "FINISHED" }) {
                Log.d(TAG, "All matches in current cycle finished. Shifting window early.")
                if (now.isBefore(today12PM)) {
                    start = today12PM
                    end = today12PM.plusDays(1)
                } else {
                    start = today12PM.plusDays(1)
                    end = today12PM.plusDays(2)
                }
                startMillis = start.toInstant().toEpochMilli()
                endMillis = end.toInstant().toEpochMilli()
                matchesInWindow = matches.filter {
                    val t = parseUtcDate(it.utcDate)
                    t in startMillis until endMillis
                }
            }

            // 3. Fallback: if no matches in window, find the next day that has matches and use its 12 PM - 12 PM cycle
            if (matchesInWindow.isEmpty()) {
                Log.d(TAG, "No matches in window, looking for next match day...")
                val upcoming = matches.filter { it.status != "FINISHED" }
                    .sortedBy { parseUtcDate(it.utcDate) }
                if (upcoming.isNotEmpty()) {
                    val firstMatchTime = parseUtcDate(upcoming.first().utcDate)
                    val matchZdt = Instant.ofEpochMilli(firstMatchTime)
                        .atZone(java.time.ZoneId.of("Asia/Kolkata"))
                    
                    val match12PM = matchZdt.withHour(12).withMinute(0).withSecond(0).withNano(0)
                    val (mStart, mEnd) = if (matchZdt.isBefore(match12PM)) {
                        match12PM.minusDays(1) to match12PM
                    } else {
                        match12PM to match12PM.plusDays(1)
                    }
                    val mStartMillis = mStart.toInstant().toEpochMilli()
                    val mEndMillis = mEnd.toInstant().toEpochMilli()
                    
                    matchesInWindow = matches.filter {
                        val t = parseUtcDate(it.utcDate)
                        t in mStartMillis until mEndMillis
                    }
                }
            }

            if (matchesInWindow.isEmpty()) {
                Log.w(TAG, "No matches found. Generating mock match...")
                return@withContext generateMockMatch()
            }

            // Sort matches in cycle by date ascending
            val sortedCycleMatches = matchesInWindow.sortedBy { parseUtcDate(it.utcDate) }

            // 4. Download logos and build MatchDetail list
            val cacheDir = File(context.filesDir, "widget_cache")
            if (!cacheDir.exists()) {
                cacheDir.mkdirs()
            }

            val matchDetails = sortedCycleMatches.mapIndexed { index, m ->
                val homeLogoFile = File(cacheDir, "home_${m.homeTeam.id}.png")
                val awayLogoFile = File(cacheDir, "away_${m.awayTeam.id}.png")
                
                downloadAndCropLogo(m.homeTeam.crest, homeLogoFile)
                downloadAndCropLogo(m.awayTeam.crest, awayLogoFile)

                val kickoffTime = parseUtcDate(m.utcDate)
                val homeCode = m.homeTeam.tla ?: getFallbackCode(m.homeTeam.name)
                val awayCode = m.awayTeam.tla ?: getFallbackCode(m.awayTeam.name)
                val badgeText = getBadgeText(m)
                val venueName = getVenueForMatch(m.id, m.homeTeam.name ?: "TBD")

                MatchDetail(
                    homeCode = homeCode.uppercase(Locale.getDefault()),
                    awayCode = awayCode.uppercase(Locale.getDefault()),
                    homeLogoPath = homeLogoFile.absolutePath,
                    awayLogoPath = awayLogoFile.absolutePath,
                    kickoffEpochMillis = kickoffTime,
                    venueName = venueName.uppercase(Locale.getDefault()),
                    badgeText = badgeText.uppercase(Locale.getDefault()),
                    status = m.status,
                    homeGoals = m.score?.fullTime?.home,
                    awayGoals = m.score?.fullTime?.away,
                    minute = m.minute
                )
            }

            // 5. Select active match index
            val cachedState = try { localSettings.getMatchState() } catch (e: Exception) { null }
            var activeIndex = -1
            if (cachedState is MatchCardState.Match) {
                val cachedMatchKeys = cachedState.cycleMatches.map { "${it.homeCode}-${it.awayCode}-${it.kickoffEpochMillis}" }
                val newMatchKeys = matchDetails.map { "${it.homeCode}-${it.awayCode}-${it.kickoffEpochMillis}" }
                if (cachedMatchKeys == newMatchKeys) {
                    activeIndex = cachedState.activeIndex
                }
            }

            if (activeIndex == -1 || activeIndex >= matchDetails.size) {
                val liveStatuses = setOf("IN_PLAY", "LIVE", "PAUSED", "1H", "2H", "HT", "ET", "P")
                val upcomingStatuses = setOf("TIMED", "SCHEDULED")

                activeIndex = matchDetails.indexOfFirst { it.status in liveStatuses }
                if (activeIndex == -1) {
                    activeIndex = matchDetails.indexOfFirst { it.status in upcomingStatuses }
                }
                if (activeIndex == -1) {
                    activeIndex = 0
                }
            }

            val activeDetail = matchDetails[activeIndex]

            val matchState = MatchCardState.Match(
                homeCode = activeDetail.homeCode,
                awayCode = activeDetail.awayCode,
                homeLogoPath = activeDetail.homeLogoPath,
                awayLogoPath = activeDetail.awayLogoPath,
                kickoffEpochMillis = activeDetail.kickoffEpochMillis,
                venueName = activeDetail.venueName,
                badgeText = activeDetail.badgeText,
                status = activeDetail.status,
                homeGoals = activeDetail.homeGoals,
                awayGoals = activeDetail.awayGoals,
                minute = activeDetail.minute,
                lastUpdatedEpochMillis = System.currentTimeMillis(),
                cycleMatches = matchDetails,
                activeIndex = activeIndex
            )

            // Cache it locally
            localSettings.saveMatchState(matchState)
            return@withContext matchState

        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync match data, falling back to cache or mock match", e)
            val cachedState = localSettings.getMatchState()
            if (cachedState is MatchCardState.Match) {
                return@withContext cachedState
            }
            return@withContext generateMockMatch()
        }
    }

    private suspend fun generateMockMatch(): MatchCardState.Match {
        val cacheDir = File(context.filesDir, "widget_cache")
        if (!cacheDir.exists()) {
            cacheDir.mkdirs()
        }

        val mockHomeLogoFile = File(cacheDir, "home_mock_esp.png")
        val mockAwayLogoFile = File(cacheDir, "away_mock_cpv.png")

        val spainUrl = "https://crests.football-data.org/760.svg"
        val capeVerdeUrl = "https://crests.football-data.org/cape_verde.svg"

        downloadAndCropLogo(spainUrl, mockHomeLogoFile)
        downloadAndCropLogo(capeVerdeUrl, mockAwayLogoFile)

        // Spain vs Cape Verde: June 15, 2026, 16:00 UTC (1781539200000L)
        val kickoffEpochMillis = if (System.currentTimeMillis() < 1781539200000L) {
            1781539200000L
        } else {
            System.currentTimeMillis() + (2 * 3600000 + 45 * 60000)
        }

        val mockDetailTimed = MatchDetail(
            homeCode = "ESP",
            awayCode = "CPV",
            homeLogoPath = mockHomeLogoFile.absolutePath,
            awayLogoPath = mockAwayLogoFile.absolutePath,
            kickoffEpochMillis = kickoffEpochMillis,
            venueName = "ATLANTA STADIUM",
            badgeText = "GROUP H",
            status = "TIMED",
            homeGoals = null,
            awayGoals = null,
            minute = null
        )

        val mockDetailLive = MatchDetail(
            homeCode = "ESP",
            awayCode = "CPV",
            homeLogoPath = mockHomeLogoFile.absolutePath,
            awayLogoPath = mockAwayLogoFile.absolutePath,
            kickoffEpochMillis = System.currentTimeMillis() - (74 * 60 * 1000), // started 74 mins ago
            venueName = "ATLANTA STADIUM",
            badgeText = "GROUP H",
            status = "IN_PLAY",
            homeGoals = 2,
            awayGoals = 1,
            minute = 74
        )

        val mockState = MatchCardState.Match(
            homeCode = "ESP",
            awayCode = "CPV",
            homeLogoPath = mockHomeLogoFile.absolutePath,
            awayLogoPath = mockAwayLogoFile.absolutePath,
            kickoffEpochMillis = kickoffEpochMillis,
            venueName = "ATLANTA STADIUM",
            badgeText = "GROUP H",
            status = "TIMED",
            homeGoals = null,
            awayGoals = null,
            minute = null,
            lastUpdatedEpochMillis = System.currentTimeMillis(),
            cycleMatches = listOf(mockDetailTimed, mockDetailLive),
            activeIndex = 0
        )

        localSettings.saveMatchState(mockState)
        return mockState
    }

    private fun getFallbackCode(name: String?): String {
        if (name == null) return "TBD"
        return name.trim().replace(" ", "").take(3).uppercase(Locale.getDefault())
    }

    private fun getBadgeText(match: MatchItem): String {
        val group = match.group
        if (!group.isNullOrBlank()) {
            return group.replace("_", " ").uppercase(Locale.getDefault())
        }
        val stage = match.stage
        if (!stage.isNullOrBlank()) {
            return stage.replace("_", " ").uppercase(Locale.getDefault())
        }
        return "WORLD CUP"
    }

    private fun getVenueForMatch(matchId: Long, homeTeam: String): String {
        return when (homeTeam.lowercase(Locale.getDefault())) {
            "spain" -> "Atlanta Stadium"
            "belgium" -> "Seattle Stadium"
            "saudi arabia" -> "Miami Stadium"
            "iran" -> "Los Angeles Stadium"
            "france" -> "New York NJ Stadium"
            "argentina" -> "Dallas Stadium"
            "mexico" -> "Estadio Azteca"
            "canada" -> "Toronto Stadium"
            "usa" -> "Los Angeles Stadium"
            else -> "World Cup Stadium"
        }
    }

    private fun downloadAndCropLogo(logoUrl: String?, destFile: File) {
        if (logoUrl.isNullOrBlank()) return
        if (destFile.exists() && destFile.length() > 0) {
            // Already downloaded
            return
        }

        try {
            Log.d(TAG, "Downloading logo: $logoUrl")
            val url = URL(logoUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.setRequestProperty("User-Agent", "Mozilla/5.0")
            connection.doInput = true
            connection.connect()
            
            val input = connection.inputStream
            val originalBitmap: Bitmap? = if (logoUrl.endsWith(".svg", ignoreCase = true)) {
                // Parse SVG using AndroidSVG library
                val svg = com.caverock.androidsvg.SVG.getFromInputStream(input)
                svg.documentWidth = 200f
                svg.documentHeight = 200f
                svg.documentPreserveAspectRatio = com.caverock.androidsvg.PreserveAspectRatio.STRETCH
                val bitmap = Bitmap.createBitmap(200, 200, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)
                svg.renderToCanvas(canvas)
                bitmap
            } else {
                // Parse standard image
                BitmapFactory.decodeStream(input)
            }
            
            if (originalBitmap != null) {
                val circleBitmap = BitmapUtils.getRoundedSquareBitmap(originalBitmap)
                FileOutputStream(destFile).use { out ->
                    circleBitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                }
                originalBitmap.recycle()
                circleBitmap.recycle()
                Log.d(TAG, "Saved rounded square logo to ${destFile.absolutePath}")
            } else {
                Log.e(TAG, "Failed to decode bitmap from $logoUrl")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error downloading and processing logo: $logoUrl", e)
        }
    }
}
