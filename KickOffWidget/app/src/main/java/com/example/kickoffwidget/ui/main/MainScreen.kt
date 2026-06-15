package com.example.kickoffwidget.ui.main

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavKey
import com.example.kickoffwidget.data.models.MatchCardState
import com.example.kickoffwidget.theme.AppTheme
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

// Custom colors matching the design system
val ColorPrimary = Color(0xFF865300)
val ColorPrimaryContainer = Color(0xFFF39C12)
val ColorOnPrimary = Color(0xFFFFFFFF)
val ColorPrimaryFixedDim = Color(0xFFFFB961)
val ColorOnPrimaryFixed = Color(0xFF2B1700)
val ColorSurface = Color(0xFFFFF8EF)
val ColorSurfaceContainerLow = Color(0xFFFBF3E4)
val ColorSecondary = Color(0xFF006D38)
val ColorSecondaryFixed = Color(0xFF96F7B0)
val ColorSecondaryFixedDim = Color(0xFF7BDA96)
val ColorOnSecondaryFixedVariant = Color(0xFF005228)

@Composable
fun MainScreen(
    onItemClick: (NavKey) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MainScreenViewModel = viewModel()
) {
    val apiKey by viewModel.apiKey.collectAsState()
    val matchState by viewModel.matchState.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()
    val syncError by viewModel.syncError.collectAsState()

    var inputKey by remember { mutableStateOf("") }
    
    // Update local input field once API key is loaded
    LaunchedEffect(apiKey) {
        if (apiKey != null) {
            inputKey = apiKey!!
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ColorSurface)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // App Title Ribbon/Banner
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            // Drop-block shadow for ribbon title
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .height(60.dp)
                    .background(ColorOnPrimaryFixed, shape = RoundedCornerShape(4.dp))
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .height(60.dp)
                    .offset(x = (-4).dp, y = (-4).dp)
                    .border(2.dp, ColorPrimaryFixedDim, RoundedCornerShape(4.dp))
                    .background(ColorPrimaryContainer, shape = RoundedCornerShape(4.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "KICKOFF CONFIG",
                    style = TextStyle(
                        fontFamily = AppTheme.BricolageGrotesque,
                        fontWeight = FontWeight.W800,
                        fontSize = 24.sp,
                        color = ColorOnPrimaryFixed,
                        letterSpacing = 2.sp
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // API Key Section
        TactileCard(title = "FOOTBALL-DATA.ORG CREDENTIALS") {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Enter your football-data.org v4 API token to load World Cup fixtures.",
                    style = TextStyle(
                        fontFamily = AppTheme.HankenGrotesk,
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp,
                        color = Color(0xFF534434)
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = inputKey,
                    onValueChange = { inputKey = it },
                    label = { Text("API Token", fontFamily = AppTheme.HankenGrotesk) },
                    placeholder = { Text("Paste football-data.org token here", fontFamily = AppTheme.HankenGrotesk) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ColorPrimary,
                        unfocusedBorderColor = Color(0xFF867461),
                        focusedLabelColor = ColorPrimary,
                        cursorColor = ColorPrimary,
                        focusedContainerColor = ColorSurfaceContainerLow,
                        unfocusedContainerColor = ColorSurfaceContainerLow
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = TextStyle(fontFamily = AppTheme.JetBrainsMono, fontSize = 14.sp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // SAVE & SYNC BUTTON
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .clickable(enabled = !isSyncing) {
                            if (inputKey.isNotBlank()) {
                                viewModel.saveApiKey(inputKey.trim())
                                viewModel.triggerManualSync()
                            }
                        }
                ) {
                    // Shadow layer
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(ColorOnPrimaryFixed, shape = RoundedCornerShape(8.dp))
                    )
                    // Button content layer
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .offset(x = (-4).dp, y = (-4).dp)
                            .background(
                                if (isSyncing) Color(0xFFD8C3AD) else ColorPrimaryContainer,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .border(2.dp, ColorOnPrimaryFixed, RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSyncing) {
                            CircularProgressIndicator(
                                color = ColorOnPrimaryFixed,
                                modifier = Modifier.size(24.dp)
                            )
                        } else {
                            Text(
                                text = "SAVE & SYNC DATA",
                                style = TextStyle(
                                    fontFamily = AppTheme.BricolageGrotesque,
                                    fontWeight = FontWeight.W800,
                                    fontSize = 16.sp,
                                    color = ColorOnPrimaryFixed
                                )
                            )
                        }
                    }
                }
                
                if (syncError != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Sync Error: $syncError",
                        color = Color.Red,
                        fontSize = 12.sp,
                        fontFamily = AppTheme.JetBrainsMono,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Widget Preview Section
        TactileCard(title = "WIDGET STATUS PREVIEW") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                when (val state = matchState) {
                    is MatchCardState.Loading -> {
                        CircularProgressIndicator(color = ColorPrimary, modifier = Modifier.padding(24.dp))
                        Text(
                            text = "Awaiting first synchronization...",
                            fontFamily = AppTheme.HankenGrotesk,
                            fontSize = 14.sp,
                            color = ColorPrimary
                        )
                    }
                    is MatchCardState.NoUpcomingMatch -> {
                        Text(
                            text = "No upcoming match found in league.",
                            fontFamily = AppTheme.HankenGrotesk,
                            fontSize = 14.sp,
                            color = Color(0xFFBA1A1A),
                            modifier = Modifier.padding(24.dp)
                        )
                    }
                    is MatchCardState.Match -> {
                        val pagerState = rememberPagerState(
                            initialPage = state.activeIndex,
                            pageCount = { state.cycleMatches.size }
                        )

                        // Sync pager current page with viewmodel selected index (bidirectional)
                        LaunchedEffect(pagerState.currentPage) {
                            viewModel.selectMatchIndex(pagerState.currentPage)
                        }

                        LaunchedEffect(state.activeIndex) {
                            if (state.activeIndex != pagerState.currentPage && state.activeIndex in 0 until state.cycleMatches.size) {
                                pagerState.animateScrollToPage(state.activeIndex)
                            }
                        }

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            HorizontalPager(
                                state = pagerState,
                                modifier = Modifier.fillMaxWidth()
                            ) { page ->
                                val matchDetail = state.cycleMatches[page]
                                val tempMatch = MatchCardState.Match(
                                    homeCode = matchDetail.homeCode,
                                    awayCode = matchDetail.awayCode,
                                    homeLogoPath = matchDetail.homeLogoPath,
                                    awayLogoPath = matchDetail.awayLogoPath,
                                    kickoffEpochMillis = matchDetail.kickoffEpochMillis,
                                    venueName = matchDetail.venueName,
                                    badgeText = matchDetail.badgeText,
                                    status = matchDetail.status,
                                    homeGoals = matchDetail.homeGoals,
                                    awayGoals = matchDetail.awayGoals,
                                    minute = matchDetail.minute,
                                    lastUpdatedEpochMillis = state.lastUpdatedEpochMillis,
                                    cycleMatches = state.cycleMatches,
                                    activeIndex = page
                                )
                                PreviewWidgetCard(tempMatch, onClick = {})
                            }

                            // Dot indicators
                            if (state.cycleMatches.size > 1) {
                                Row(
                                    Modifier
                                        .wrapContentHeight()
                                        .fillMaxWidth()
                                        .padding(top = 12.dp),
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    repeat(state.cycleMatches.size) { iteration ->
                                        val color = if (pagerState.currentPage == iteration) ColorPrimary else Color(0xFFD8C3AD)
                                        Box(
                                            modifier = Modifier
                                                .padding(4.dp)
                                                .clip(CircleShape)
                                                .background(color)
                                                .size(8.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Instructions Section
        TactileCard(title = "HOW TO ENABLE WIDGET") {
            Column(modifier = Modifier.padding(16.dp)) {
                val instructions = listOf(
                    "1. Close this app and go to your phone's Home Screen.",
                    "2. Long-press on any empty space on the home screen.",
                    "3. Select 'Widgets' (or 'Add widgets') from the popup menu.",
                    "4. Scroll down or search for 'KickOff Widget'.",
                    "5. Long-press the widget preview and drag it onto your home screen."
                )
                instructions.forEach { text ->
                    Text(
                        text = text,
                        style = TextStyle(
                            fontFamily = AppTheme.HankenGrotesk,
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp,
                            color = ColorOnPrimaryFixed
                        ),
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(40.dp))
    }
}

@Composable
fun TactileCard(
    title: String,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        // Shadow (back layer)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .matchParentSize()
                .background(ColorOnPrimaryFixed, shape = RoundedCornerShape(12.dp))
        )
        // Card (front layer)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .offset(x = (-4).dp, y = (-4).dp)
                .background(Color.White, shape = RoundedCornerShape(12.dp))
                .border(2.dp, ColorPrimaryFixedDim, RoundedCornerShape(12.dp))
        ) {
            // Card Header Ribbon
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ColorPrimaryContainer, shape = RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp))
                    .border(
                        width = 2.dp,
                        color = ColorOnPrimaryFixed,
                        shape = RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp)
                    )
                    .padding(vertical = 8.dp, horizontal = 16.dp)
            ) {
                Text(
                    text = title,
                    style = TextStyle(
                        fontFamily = AppTheme.BricolageGrotesque,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = ColorOnPrimaryFixed,
                        letterSpacing = 1.sp
                    )
                )
            }
            content()
        }
    }
}

@Composable
fun PreviewWidgetCard(state: MatchCardState.Match, onClick: () -> Unit) {
    val formatter = remember {
        DateTimeFormatter.ofPattern("MMM dd, yyyy - hh:mm a 'IST'")
            .withZone(ZoneId.of("Asia/Kolkata"))
    }
    val kickoffStr = formatter.format(Instant.ofEpochMilli(state.kickoffEpochMillis))

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "LIVE SYNC PREVIEW",
            fontFamily = AppTheme.BricolageGrotesque,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            color = ColorPrimary,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Mock medium widget container
        Box(
            modifier = Modifier
                .size(348.dp, 160.dp)
                .clickable { onClick() }
                .background(ColorOnPrimaryFixed, shape = RoundedCornerShape(22.dp))
        ) {
            // Shadow shifting effect
            Box(
                modifier = Modifier
                    .size(344.dp, 156.dp)
                    .offset(x = 0.dp, y = 0.dp) // shift card internally
                    .background(ColorPrimaryContainer, shape = RoundedCornerShape(22.dp))
                    .border(2.dp, ColorPrimaryFixedDim, RoundedCornerShape(22.dp))
                    .clip(RoundedCornerShape(22.dp))
            ) {
                // Top Content Area (Jungle / Botanical illustration simulator)
                // Color layer with dimmer gradient simulator
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(116.dp)
                        .background(ColorPrimary)
                ) {
                    // Dim overlay for better contrast
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0x33000000))
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(start = 20.dp, top = 8.dp, end = 20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Home Team
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.width(80.dp)
                        ) {
                            val bitmap = remember(state.homeLogoPath) {
                                try {
                                    BitmapFactory.decodeFile(state.homeLogoPath)
                                } catch (e: Exception) {
                                    null
                                }
                            }
                            Box(
                                modifier = Modifier.size(60.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(56.dp)
                                        .offset(x = 4.dp, y = 4.dp)
                                        .background(ColorOnPrimaryFixed, shape = RoundedCornerShape(12.dp))
                                )
                                Box(
                                    modifier = Modifier
                                        .size(56.dp)
                                        .background(ColorSurface, shape = RoundedCornerShape(12.dp))
                                        .border(2.dp, ColorOnPrimaryFixed, RoundedCornerShape(12.dp))
                                        .clip(RoundedCornerShape(12.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (bitmap != null) {
                                        Image(
                                            bitmap = bitmap.asImageBitmap(),
                                            contentDescription = state.homeCode,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = state.homeCode,
                                color = ColorOnPrimary,
                                fontFamily = AppTheme.JetBrainsMono,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }

                        // Center Countdown
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            val now = System.currentTimeMillis()
                            val remaining = state.kickoffEpochMillis - now
                            val isLive = state.status in setOf("1H", "2H", "HT", "ET", "P", "LIVE", "IN_PLAY", "PAUSED")
                            
                            val centerLabel = when {
                                isLive || state.status == "FT" || state.status == "FINISHED" -> {
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

                            // Tactile countdown pill (with -2 degree rotation)
                            val isScoreText = centerLabel.contains("-") && centerLabel.split("-").size == 2
                            if (isScoreText) {
                                val parts = centerLabel.split("-")
                                val homeVal = parts[0].trim()
                                val awayVal = parts[1].trim()
                                Row(
                                    modifier = Modifier.graphicsLayer { rotationZ = -2f },
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier.size(38.dp, 38.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(Color(0xFF1C0E00), shape = RoundedCornerShape(8.dp))
                                        )
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .offset(x = (-1.5).dp, y = (-1.5).dp)
                                                .background(Color(0xFFFFF9F2), shape = RoundedCornerShape(8.dp))
                                                .border(1.2.dp, Color(0xFF2B1700), RoundedCornerShape(8.dp)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = homeVal,
                                                color = Color(0xFF2B1700),
                                                fontFamily = AppTheme.JetBrainsMono,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 18.sp
                                            )
                                        }
                                    }
                                    
                                    Text(
                                        text = " : ",
                                        color = Color(0xFF2B1700),
                                        fontFamily = AppTheme.JetBrainsMono,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp,
                                        modifier = Modifier.padding(horizontal = 4.dp)
                                    )
                                    
                                    Box(
                                        modifier = Modifier.size(38.dp, 38.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(Color(0xFF1C0E00), shape = RoundedCornerShape(8.dp))
                                        )
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .offset(x = (-1.5).dp, y = (-1.5).dp)
                                                .background(Color(0xFFFFF9F2), shape = RoundedCornerShape(8.dp))
                                                .border(1.2.dp, Color(0xFF2B1700), RoundedCornerShape(8.dp)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = awayVal,
                                                color = Color(0xFF2B1700),
                                                fontFamily = AppTheme.JetBrainsMono,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 18.sp
                                            )
                                        }
                                    }
                                }
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(94.dp, 38.dp)
                                        .graphicsLayer { rotationZ = -2f }
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color(0xFF1C0E00), shape = RoundedCornerShape(10.dp))
                                    )
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .offset(x = (-1.5).dp, y = (-1.5).dp)
                                            .background(Color(0xFFFFF9F2), shape = RoundedCornerShape(10.dp))
                                            .border(1.2.dp, Color(0xFF2B1700), RoundedCornerShape(10.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = centerLabel,
                                            color = Color(0xFF2B1700),
                                            fontFamily = AppTheme.JetBrainsMono,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 18.sp
                                        )
                                    }
                                }
                            }
                             Spacer(modifier = Modifier.height(4.dp))
                              val isCaptionLive = isLive && state.status != "PAUSED" && state.status != "HT" && state.status != "FT" && state.status != "FINISHED"
                              if (isCaptionLive) {
                                  Box(
                                      modifier = Modifier
                                          .size(86.dp, 22.dp)
                                          .background(ColorOnPrimaryFixed, shape = RoundedCornerShape(6.dp))
                                  ) {
                                      Box(
                                          modifier = Modifier
                                              .size(86.dp, 22.dp)
                                              .offset(x = (-1.5).dp, y = (-1.5).dp)
                                              .background(ColorSurface, shape = RoundedCornerShape(6.dp))
                                              .border(1.dp, ColorOnPrimaryFixed, RoundedCornerShape(6.dp)),
                                          contentAlignment = Alignment.Center
                                      ) {
                                          Row(
                                              verticalAlignment = Alignment.CenterVertically
                                          ) {
                                              Text(
                                                  text = "● ",
                                                  color = Color(0xFFFF3B30),
                                                  fontFamily = AppTheme.JetBrainsMono,
                                                  fontWeight = FontWeight.Bold,
                                                  fontSize = 9.sp
                                              )
                                              val liveText = if (state.minute != null) "${state.minute}' • LIVE" else "LIVE"
                                              Text(
                                                  text = liveText,
                                                  color = Color(0xFFFF3B30),
                                                  fontFamily = AppTheme.JetBrainsMono,
                                                  fontWeight = FontWeight.Bold,
                                                  fontSize = 9.sp
                                              )
                                          }
                                      }
                                  }
                              } else {
                                  Text(
                                      text = captionLabel,
                                      color = ColorSurfaceContainerLow,
                                      fontFamily = AppTheme.JetBrainsMono,
                                      fontWeight = FontWeight.Bold,
                                      fontSize = 10.sp
                                  )
                              }
                        }

                        // Away Team
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.width(80.dp)
                        ) {
                            val bitmap = remember(state.awayLogoPath) {
                                try {
                                    BitmapFactory.decodeFile(state.awayLogoPath)
                                } catch (e: Exception) {
                                    null
                                }
                            }
                            Box(
                                modifier = Modifier.size(60.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(56.dp)
                                        .offset(x = 4.dp, y = 4.dp)
                                        .background(ColorOnPrimaryFixed, shape = RoundedCornerShape(12.dp))
                                )
                                Box(
                                    modifier = Modifier
                                        .size(56.dp)
                                        .background(ColorSurface, shape = RoundedCornerShape(12.dp))
                                        .border(2.dp, ColorOnPrimaryFixed, RoundedCornerShape(12.dp))
                                        .clip(RoundedCornerShape(12.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (bitmap != null) {
                                        Image(
                                            bitmap = bitmap.asImageBitmap(),
                                            contentDescription = state.awayCode,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = state.awayCode,
                                color = ColorOnPrimary,
                                fontFamily = AppTheme.JetBrainsMono,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                    }
                }

                // Bottom Strip Section (Green Strip)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                        .align(Alignment.BottomCenter)
                        .background(ColorSecondary)
                        .border(
                            width = 2.dp,
                            color = ColorPrimaryFixedDim,
                            shape = RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp)
                        )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                         Text(
                             text = "🏟  " + state.venueName.uppercase(Locale.getDefault()),
                             color = ColorSecondaryFixed,
                             fontFamily = AppTheme.JetBrainsMono,
                             fontWeight = FontWeight.Bold,
                             fontSize = 9.sp
                         )

                        // Mock badge shape
                        Box(
                            modifier = Modifier
                                .size(64.dp, 20.dp)
                                .background(ColorOnPrimaryFixed, shape = RoundedCornerShape(4.dp))
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp, 20.dp)
                                    .offset(x = (-1.5).dp, y = (-1.5).dp)
                                    .background(ColorSecondaryFixedDim, shape = RoundedCornerShape(4.dp))
                                    .border(1.dp, ColorSecondary, RoundedCornerShape(4.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                 Text(
                                     text = state.badgeText,
                                     color = ColorOnSecondaryFixedVariant,
                                     fontFamily = AppTheme.JetBrainsMono,
                                     fontWeight = FontWeight.Bold,
                                     fontSize = 8.sp
                                 )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Synced Info List
        Column(
            modifier = Modifier.fillMaxWidth(0.9f),
            horizontalAlignment = Alignment.Start
        ) {
            InfoRow(label = "Kickoff Time", value = kickoffStr)
            InfoRow(label = "Venue", value = state.venueName)
            InfoRow(label = "Stage / Group", value = state.badgeText)
            InfoRow(
                label = "Last Synced",
                value = DateTimeFormatter.ofPattern("hh:mm:ss a 'IST'").withZone(ZoneId.of("Asia/Kolkata")).format(Instant.ofEpochMilli(state.lastUpdatedEpochMillis))
            )
        }
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontFamily = AppTheme.HankenGrotesk,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            color = Color(0xFF534434)
        )
        Text(
            text = value,
            fontFamily = AppTheme.JetBrainsMono,
            fontWeight = FontWeight.Medium,
            fontSize = 13.sp,
            color = ColorOnPrimaryFixed,
            textAlign = TextAlign.End
        )
    }
}
