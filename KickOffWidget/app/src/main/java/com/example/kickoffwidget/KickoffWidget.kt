package com.example.kickoffwidget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import androidx.glance.action.ActionParameters
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.updateAll
import androidx.glance.LocalContext
import com.example.kickoffwidget.utils.BitmapUtils
import com.example.kickoffwidget.data.local.LocalSettings
import com.example.kickoffwidget.data.models.MatchCardState
import com.example.kickoffwidget.theme.WidgetTheme
import java.io.File
import java.util.Locale

class KickoffWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val settings = LocalSettings(context)
        provideContent {
            val state by settings.matchStateFlow.collectAsState(initial = MatchCardState.Loading)
            WidgetContent(state)
        }
    }
}

@Composable
fun WidgetContent(state: MatchCardState) {
    when (state) {
        is MatchCardState.Loading -> {
            Box(
                modifier = GlanceModifier.fillMaxSize().background(ColorProvider(R.color.surface)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Syncing next match...",
                    style = TextStyle(
                        color = ColorProvider(R.color.primary),
                        fontSize = 14.sp,
                        fontFamily = WidgetTheme.HankenGrotesk,
                        fontWeight = FontWeight.Medium
                    )
                )
            }
        }
        is MatchCardState.NoUpcomingMatch -> {
            Box(
                modifier = GlanceModifier.fillMaxSize().background(ColorProvider(R.color.surface)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No upcoming matches",
                    style = TextStyle(
                        color = ColorProvider(R.color.primary),
                        fontSize = 14.sp,
                        fontFamily = WidgetTheme.HankenGrotesk,
                        fontWeight = FontWeight.Medium
                    )
                )
            }
        }
        is MatchCardState.Match -> {
            MatchCard(state)
        }
    }
}

@Composable
fun MatchCard(state: MatchCardState.Match) {
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .clickable(actionRunCallback<CycleMatchActionCallback>())
    ) {
        // Back Layer (Shadow for outer card)
        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .padding(start = 4.dp, top = 4.dp)
                .background(ImageProvider(R.drawable.widget_card_shadow))
        ) {}

        // Front Layer (Actual widget card content)
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .padding(end = 4.dp, bottom = 4.dp)
        ) {
            // Top content area (Botanical image backdrop)
            Box(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .defaultWeight()
                    .background(ImageProvider(R.drawable.widget_card_background))
            ) {
                // Dim overlay for better contrast
                Box(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .background(ColorProvider(R.color.widget_background_dim))
                ) {}

                Row(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .padding(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 0.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Home Team Column
                    TeamColumn(
                        logoPath = state.homeLogoPath,
                        code = state.homeCode
                    )

                    Spacer(modifier = GlanceModifier.defaultWeight())

                    // Countdown pill center Column
                    CountdownCenter(state)

                    Spacer(modifier = GlanceModifier.defaultWeight())

                    // Away Team Column
                    TeamColumn(
                        logoPath = state.awayLogoPath,
                        code = state.awayCode
                    )
                }
            }

            // Bottom Green Strip
            Box(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .background(ImageProvider(R.drawable.widget_bottom_strip))
            ) {
                Row(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Venue Information (Stadium vector + name)
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            provider = ImageProvider(R.drawable.ic_stadium),
                            contentDescription = "Stadium",
                            modifier = GlanceModifier.size(14.dp)
                        )
                        Spacer(modifier = GlanceModifier.width(4.dp))
                        Text(
                            text = state.venueName.uppercase(Locale.getDefault()),
                            style = TextStyle(
                                color = ColorProvider(R.color.secondary_fixed),
                                fontSize = 10.sp,
                                fontFamily = WidgetTheme.JetBrainsMono,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }

                    Spacer(modifier = GlanceModifier.defaultWeight())

                    // Group / Round Badge
                    GroupBadge(state.badgeText)
                }
            }
        }
    }
}

@Composable
fun TeamColumn(logoPath: String, code: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalAlignment = Alignment.CenterVertically,
        modifier = GlanceModifier.width(80.dp)
    ) {
        val logoBitmap = loadLogo(logoPath)
        
        Box(
            modifier = GlanceModifier
                .size(60.dp, 60.dp)
                .background(ImageProvider(R.drawable.widget_flag_container)),
            contentAlignment = Alignment.Center
        ) {
            if (logoBitmap != null) {
                Image(
                    provider = ImageProvider(logoBitmap),
                    contentDescription = code,
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .padding(start = 2.dp, top = 2.dp, end = 6.dp, bottom = 6.dp)
                )
            }
        }

        Spacer(modifier = GlanceModifier.height(4.dp))

        Text(
            text = code,
            style = TextStyle(
                color = ColorProvider(R.color.on_primary),
                fontSize = 16.sp,
                fontFamily = WidgetTheme.JetBrainsMono,
                fontWeight = FontWeight.Bold
            )
        )
    }
}

@Composable
fun CountdownCenter(state: MatchCardState.Match) {
    val now = System.currentTimeMillis()
    val remaining = state.kickoffEpochMillis - now

    val isLive = state.status in setOf("1H", "2H", "HT", "ET", "P", "LIVE", "IN_PLAY", "PAUSED")
    
    val centerLabel = when {
        isLive -> {
            val homeG = state.homeGoals ?: 0
            val awayG = state.awayGoals ?: 0
            "$homeG-$awayG"
        }
        state.status == "FT" || state.status == "FINISHED" -> {
            val homeG = state.homeGoals ?: 0
            val awayG = state.awayGoals ?: 0
            "$homeG-$awayG"
        }
        remaining <= 0 -> "LIVE"
        else -> {
            val formatter = java.time.format.DateTimeFormatter.ofPattern("HH:mm")
                .withZone(java.time.ZoneId.of("Asia/Kolkata"))
            formatter.format(java.time.Instant.ofEpochMilli(state.kickoffEpochMillis))
        }
    }

    val captionLabel = when {
        state.status == "FT" || state.status == "FINISHED" -> "FULL TIME"
        state.status == "PAUSED" || state.status == "HT" -> "HT • HALF TIME"
        isLive -> {
            if (state.minute != null) "${state.minute}' • LIVE" else "LIVE"
        }
        remaining <= 0 -> "LIVE"
        else -> "KICKOFF (IST)"
    }

    val context = LocalContext.current
    val density = context.resources.displayMetrics.density
    val pillBitmap = BitmapUtils.createRotatedPill(context, centerLabel, density)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            provider = ImageProvider(pillBitmap),
            contentDescription = "Countdown",
            modifier = GlanceModifier.size(112.dp, 50.dp)
        )

        Spacer(modifier = GlanceModifier.height(4.dp))

        val isCaptionLive = isLive && state.status != "PAUSED" && state.status != "HT" && state.status != "FT" && state.status != "FINISHED"
        if (isCaptionLive) {
            Box(
                modifier = GlanceModifier
                    .size(86.dp, 22.dp)
                    .background(ImageProvider(R.drawable.widget_live_badge_container)),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = GlanceModifier.padding(end = 1.5.dp, bottom = 1.5.dp)
                ) {
                    Text(
                        text = "● ",
                        style = TextStyle(
                            color = ColorProvider(R.color.live_red),
                            fontSize = 9.sp,
                            fontFamily = WidgetTheme.JetBrainsMono,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    val liveText = if (state.minute != null) "${state.minute}' • LIVE" else "LIVE"
                    Text(
                        text = liveText,
                        style = TextStyle(
                            color = ColorProvider(R.color.live_red),
                            fontSize = 9.sp,
                            fontFamily = WidgetTheme.JetBrainsMono,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
        } else {
            Text(
                text = captionLabel,
                style = TextStyle(
                    color = ColorProvider(R.color.surface_container_low),
                    fontSize = 11.sp,
                    fontFamily = WidgetTheme.JetBrainsMono,
                    fontWeight = FontWeight.Bold
                )
            )
        }
    }
}

@Composable
fun GroupBadge(badgeText: String) {
    Box(
        modifier = GlanceModifier
            .size(70.dp, 24.dp)
            .background(ImageProvider(R.drawable.widget_group_badge_container)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = badgeText,
            style = TextStyle(
                color = ColorProvider(R.color.on_secondary_fixed_variant),
                fontSize = 9.sp,
                fontFamily = WidgetTheme.JetBrainsMono,
                fontWeight = FontWeight.Bold
            ),
            modifier = GlanceModifier.padding(end = 2.dp, bottom = 2.dp)
        )
    }
}

private fun loadLogo(path: String?): Bitmap? {
    if (path == null) return null
    val file = File(path)
    if (!file.exists() || file.length() == 0L) return null
    return try {
        BitmapFactory.decodeFile(path)
    } catch (e: Exception) {
        null
    }
}

class CycleMatchActionCallback : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val settings = LocalSettings(context)
        val state = settings.getMatchState()
        if (state is MatchCardState.Match && state.cycleMatches.isNotEmpty()) {
            val nextIndex = (state.activeIndex + 1) % state.cycleMatches.size
            val nextDetail = state.cycleMatches[nextIndex]
            val updatedState = state.copy(
                homeCode = nextDetail.homeCode,
                awayCode = nextDetail.awayCode,
                homeLogoPath = nextDetail.homeLogoPath,
                awayLogoPath = nextDetail.awayLogoPath,
                kickoffEpochMillis = nextDetail.kickoffEpochMillis,
                venueName = nextDetail.venueName,
                badgeText = nextDetail.badgeText,
                status = nextDetail.status,
                homeGoals = nextDetail.homeGoals,
                awayGoals = nextDetail.awayGoals,
                minute = nextDetail.minute,
                activeIndex = nextIndex
            )
            settings.saveMatchState(updatedState)
            KickoffWidget().updateAll(context)
        }
    }
}
