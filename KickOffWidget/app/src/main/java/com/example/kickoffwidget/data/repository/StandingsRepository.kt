package com.example.kickoffwidget.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.util.Log
import com.example.kickoffwidget.Config
import com.example.kickoffwidget.data.api.ApiFootballService
import com.example.kickoffwidget.data.local.LocalSettings
import com.example.kickoffwidget.data.models.StandingItem
import com.example.kickoffwidget.data.models.TableItem
import com.example.kickoffwidget.data.models.TeamInfo
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class StandingsRepository(private val context: Context) {
    private val TAG = "StandingsRepository"
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

    suspend fun fetchStandings(): List<StandingItem> = withContext(Dispatchers.IO) {
        val apiKey = localSettings.getApiKey()
        if (apiKey.isNullOrBlank()) {
            Log.w(TAG, "API Key is missing for standings, using mock data")
            return@withContext getMockStandings()
        }

        try {
            Log.d(TAG, "Fetching standings from football-data.org...")
            val response = apiService.getStandings(
                apiKey = apiKey,
                code = Config.COMPETITION_CODE,
                season = Config.SEASON
            )
            val standings = response.standings.filter { it.type == "TOTAL" }
            if (standings.isEmpty()) {
                Log.w(TAG, "Empty standings returned, using mock data")
                return@withContext getMockStandings()
            }

            // Cache standings flags
            val cacheDir = File(context.filesDir, "widget_cache")
            if (!cacheDir.exists()) {
                cacheDir.mkdirs()
            }

            standings.forEach { standing ->
                standing.table.forEach { tableItem ->
                    val teamId = tableItem.team.id
                    val crestUrl = tableItem.team.crest
                    if (teamId != null && !crestUrl.isNullOrBlank()) {
                        val crestFile = File(cacheDir, "crest_$teamId.png")
                        downloadAndCropLogo(crestUrl, crestFile)
                    }
                }
            }

            return@withContext standings
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch standings from API, falling back to mock data", e)
            return@withContext getMockStandings()
        }
    }

    private suspend fun getMockStandings(): List<StandingItem> = withContext(Dispatchers.IO) {
        val cacheDir = File(context.filesDir, "widget_cache")
        if (!cacheDir.exists()) {
            cacheDir.mkdirs()
        }

        // Setup mock flags/crests and download them if needed
        val mockTeams = listOf(
            MockTeam(1, "Ecuador", "ECU", "https://crests.football-data.org/ecuador.svg"),
            MockTeam(2, "Netherlands", "NED", "https://crests.football-data.org/netherlands.svg"),
            MockTeam(3, "Senegal", "SEN", "https://crests.football-data.org/senegal.svg"),
            MockTeam(4, "Qatar", "QAT", "https://crests.football-data.org/qatar.svg"),
            MockTeam(5, "England", "ENG", "https://crests.football-data.org/england.svg"),
            MockTeam(6, "USA", "USA", "https://crests.football-data.org/usa.svg"),
            MockTeam(7, "Iran", "IRN", "https://crests.football-data.org/iran.svg"),
            MockTeam(8, "Wales", "WAL", "https://crests.football-data.org/wales.svg"),
            MockTeam(9, "Argentina", "ARG", "https://crests.football-data.org/argentina.svg"),
            MockTeam(10, "Poland", "POL", "https://crests.football-data.org/poland.svg"),
            MockTeam(11, "Mexico", "MEX", "https://crests.football-data.org/mexico.svg"),
            MockTeam(12, "Saudi Arabia", "KSA", "https://crests.football-data.org/saudi_arabia.svg"),
            MockTeam(13, "France", "FRA", "https://crests.football-data.org/france.svg"),
            MockTeam(14, "Australia", "AUS", "https://crests.football-data.org/australia.svg"),
            MockTeam(15, "Tunisia", "TUN", "https://crests.football-data.org/tunisia.svg"),
            MockTeam(16, "Denmark", "DEN", "https://crests.football-data.org/denmark.svg"),
            MockTeam(17, "Japan", "JPN", "https://crests.football-data.org/japan.svg"),
            MockTeam(18, "Spain", "ESP", "https://crests.football-data.org/760.svg"),
            MockTeam(19, "Germany", "GER", "https://crests.football-data.org/germany.svg"),
            MockTeam(20, "Costa Rica", "CRC", "https://crests.football-data.org/costa_rica.svg"),
            MockTeam(21, "Morocco", "MAR", "https://crests.football-data.org/morocco.svg"),
            MockTeam(22, "Croatia", "CRO", "https://crests.football-data.org/croatia.svg"),
            MockTeam(23, "Belgium", "BEL", "https://crests.football-data.org/belgium.svg"),
            MockTeam(24, "Canada", "CAN", "https://crests.football-data.org/canada.svg"),
            MockTeam(25, "Brazil", "BRA", "https://crests.football-data.org/brazil.svg"),
            MockTeam(26, "Switzerland", "SUI", "https://crests.football-data.org/switzerland.svg"),
            MockTeam(27, "Cameroon", "CMR", "https://crests.football-data.org/cameroon.svg"),
            MockTeam(28, "Serbia", "SRB", "https://crests.football-data.org/serbia.svg"),
            MockTeam(29, "Portugal", "POR", "https://crests.football-data.org/portugal.svg"),
            MockTeam(30, "South Korea", "KOR", "https://crests.football-data.org/south_korea.svg"),
            MockTeam(31, "Uruguay", "URU", "https://crests.football-data.org/uruguay.svg"),
            MockTeam(32, "Ghana", "GHA", "https://crests.football-data.org/ghana.svg")
        )

        mockTeams.forEach { team ->
            val crestFile = File(cacheDir, "crest_${team.id}.png")
            downloadAndCropLogo(team.crestUrl, crestFile)
        }

        fun makeTeamInfo(mock: MockTeam): TeamInfo {
            val crestFile = File(cacheDir, "crest_${mock.id}.png")
            return TeamInfo(
                id = mock.id,
                name = mock.name,
                shortName = mock.name,
                tla = mock.tla,
                crest = if (crestFile.exists()) crestFile.absolutePath else mock.crestUrl
            )
        }

        // Group A
        val groupA = StandingItem(
            stage = "GROUP_STAGE",
            type = "TOTAL",
            group = "GROUP_A",
            table = listOf(
                TableItem(1, makeTeamInfo(mockTeams[0]), 3, 2, 1, 0, 7, 4),
                TableItem(2, makeTeamInfo(mockTeams[1]), 3, 1, 2, 0, 5, 2),
                TableItem(3, makeTeamInfo(mockTeams[2]), 3, 1, 0, 2, 3, -1),
                TableItem(4, makeTeamInfo(mockTeams[3]), 3, 0, 0, 3, 0, -5)
            )
        )

        // Group B
        val groupB = StandingItem(
            stage = "GROUP_STAGE",
            type = "TOTAL",
            group = "GROUP_B",
            table = listOf(
                TableItem(1, makeTeamInfo(mockTeams[4]), 3, 2, 1, 0, 7, 7),
                TableItem(2, makeTeamInfo(mockTeams[5]), 3, 1, 2, 0, 5, 1),
                TableItem(3, makeTeamInfo(mockTeams[6]), 3, 1, 0, 2, 3, -3),
                TableItem(4, makeTeamInfo(mockTeams[7]), 3, 0, 1, 2, 1, -5)
            )
        )

        // Group C
        val groupC = StandingItem(
            stage = "GROUP_STAGE",
            type = "TOTAL",
            group = "GROUP_C",
            table = listOf(
                TableItem(1, makeTeamInfo(mockTeams[8]), 3, 2, 0, 1, 6, 3),
                TableItem(2, makeTeamInfo(mockTeams[9]), 3, 1, 1, 1, 4, 0),
                TableItem(3, makeTeamInfo(mockTeams[10]), 3, 1, 1, 1, 4, -1),
                TableItem(4, makeTeamInfo(mockTeams[11]), 3, 1, 0, 2, 3, -2)
            )
        )

        // Group D
        val groupD = StandingItem(
            stage = "GROUP_STAGE",
            type = "TOTAL",
            group = "GROUP_D",
            table = listOf(
                TableItem(1, makeTeamInfo(mockTeams[12]), 3, 2, 0, 1, 6, 3),
                TableItem(2, makeTeamInfo(mockTeams[13]), 3, 2, 0, 1, 6, 1),
                TableItem(3, makeTeamInfo(mockTeams[14]), 3, 1, 1, 1, 4, -1),
                TableItem(4, makeTeamInfo(mockTeams[15]), 3, 0, 1, 2, 1, -3)
            )
        )

        // Group E
        val groupE = StandingItem(
            stage = "GROUP_STAGE",
            type = "TOTAL",
            group = "GROUP_E",
            table = listOf(
                TableItem(1, makeTeamInfo(mockTeams[16]), 3, 2, 0, 1, 6, 1),
                TableItem(2, makeTeamInfo(mockTeams[17]), 3, 1, 1, 1, 4, 6),
                TableItem(3, makeTeamInfo(mockTeams[18]), 3, 1, 1, 1, 4, 1),
                TableItem(4, makeTeamInfo(mockTeams[19]), 3, 0, 0, 3, 0, -8)
            )
        )

        // Group F
        val groupF = StandingItem(
            stage = "GROUP_STAGE",
            type = "TOTAL",
            group = "GROUP_F",
            table = listOf(
                TableItem(1, makeTeamInfo(mockTeams[20]), 3, 2, 1, 0, 7, 3),
                TableItem(2, makeTeamInfo(mockTeams[21]), 3, 1, 2, 0, 5, 3),
                TableItem(3, makeTeamInfo(mockTeams[22]), 3, 1, 1, 1, 4, -1),
                TableItem(4, makeTeamInfo(mockTeams[23]), 3, 0, 0, 3, 0, -5)
            )
        )

        // Group G
        val groupG = StandingItem(
            stage = "GROUP_STAGE",
            type = "TOTAL",
            group = "GROUP_G",
            table = listOf(
                TableItem(1, makeTeamInfo(mockTeams[24]), 3, 2, 0, 1, 6, 2),
                TableItem(2, makeTeamInfo(mockTeams[25]), 3, 2, 0, 1, 6, 1),
                TableItem(3, makeTeamInfo(mockTeams[26]), 3, 1, 1, 1, 4, 0),
                TableItem(4, makeTeamInfo(mockTeams[27]), 3, 0, 1, 2, 1, -3)
            )
        )

        // Group H
        val groupH = StandingItem(
            stage = "GROUP_STAGE",
            type = "TOTAL",
            group = "GROUP_H",
            table = listOf(
                TableItem(1, makeTeamInfo(mockTeams[28]), 3, 2, 0, 1, 6, 2),
                TableItem(2, makeTeamInfo(mockTeams[29]), 3, 1, 1, 1, 4, -2),
                TableItem(3, makeTeamInfo(mockTeams[30]), 3, 1, 1, 1, 4, 0),
                TableItem(4, makeTeamInfo(mockTeams[31]), 3, 1, 0, 2, 3, 0)
            )
        )

        return@withContext listOf(groupA, groupB, groupC, groupD, groupE, groupF, groupG, groupH)
    }

    private fun downloadAndCropLogo(logoUrl: String?, destFile: File) {
        if (logoUrl.isNullOrBlank()) return
        if (destFile.exists() && destFile.length() > 0) {
            return
        }

        try {
            Log.d(TAG, "Downloading logo for standings: $logoUrl")
            val url = URL(logoUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.setRequestProperty("User-Agent", "Mozilla/5.0")
            connection.doInput = true
            connection.connect()

            val input = connection.inputStream
            val originalBitmap: Bitmap? = if (logoUrl.endsWith(".svg", ignoreCase = true)) {
                val svg = com.caverock.androidsvg.SVG.getFromInputStream(input)
                svg.documentWidth = 200f
                svg.documentHeight = 200f
                svg.documentPreserveAspectRatio = com.caverock.androidsvg.PreserveAspectRatio.STRETCH
                val bitmap = Bitmap.createBitmap(200, 200, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)
                svg.renderToCanvas(canvas)
                bitmap
            } else {
                BitmapFactory.decodeStream(input)
            }

            if (originalBitmap != null) {
                val circleBitmap = BitmapUtils.getRoundedSquareBitmap(originalBitmap)
                FileOutputStream(destFile).use { out ->
                    circleBitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                }
                originalBitmap.recycle()
                circleBitmap.recycle()
                Log.d(TAG, "Saved standings logo to ${destFile.absolutePath}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error downloading crest logo: $logoUrl", e)
        }
    }

    private data class MockTeam(val id: Int, val name: String, val tla: String, val crestUrl: String)
}
