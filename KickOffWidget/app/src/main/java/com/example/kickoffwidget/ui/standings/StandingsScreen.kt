package com.example.kickoffwidget.ui.standings

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.kickoffwidget.data.models.StandingItem
import com.example.kickoffwidget.data.models.TableItem
import com.example.kickoffwidget.theme.AppTheme
import java.io.File
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
val ColorSurfaceContainer = Color(0xFFF5EDDE)
val ColorOutlineVariant = Color(0xFFD8C3AD)
val ColorOnSurfaceVariant = Color(0xFF534434)

@Composable
fun StandingsScreen(
    modifier: Modifier = Modifier,
    viewModel: StandingsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ColorSurface)
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Page Header Ribbon
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            // Drop-block shadow for title banner
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .height(60.dp)
                    .background(Color(0xFF00210D), shape = RoundedCornerShape(4.dp))
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .height(60.dp)
                    .offset(x = (-4).dp, y = (-4).dp)
                    .border(2.dp, Color(0xFFFFF8EF), RoundedCornerShape(4.dp))
                    .background(ColorSecondary, shape = RoundedCornerShape(4.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "GROUP STANDINGS",
                    style = TextStyle(
                        fontFamily = AppTheme.BricolageGrotesque,
                        fontWeight = FontWeight.W800,
                        fontSize = 20.sp,
                        color = Color(0xFFFFF8EF),
                        letterSpacing = 2.sp
                    )
                )
            }
        }

        when (val state = uiState) {
            is StandingsUiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = ColorPrimary)
                }
            }
            is StandingsUiState.Error -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Error loading standings:\n${state.message}",
                        color = Color.Red,
                        fontFamily = AppTheme.JetBrainsMono,
                        textAlign = TextAlign.Center
                    )
                }
            }
            is StandingsUiState.Success -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(24.dp),
                    contentPadding = PaddingValues(bottom = 100.dp)
                ) {
                    items(state.standings) { standing ->
                        GroupStandingCard(standing = standing)
                    }
                }
            }
        }
    }
}

@Composable
fun GroupStandingCard(standing: StandingItem) {
    val groupName = remember(standing.group) {
        standing.group?.replace("_", " ")?.uppercase(Locale.getDefault()) ?: "GROUP"
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        // Shadow (back layer)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .matchParentSize()
                .background(Color(0xFF1E1B13), shape = RoundedCornerShape(12.dp))
        )
        // Card (front layer)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .offset(x = (-4).dp, y = (-4).dp)
                .background(Color.White, shape = RoundedCornerShape(12.dp))
                .border(2.dp, ColorOutlineVariant, RoundedCornerShape(12.dp))
        ) {
            // Group Header Ribbon
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ColorPrimary, shape = RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp))
                    .border(
                        width = 2.dp,
                        color = Color(0xFF1E1B13),
                        shape = RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp)
                    )
                    .padding(vertical = 12.dp, horizontal = 16.dp)
            ) {
                Text(
                    text = groupName,
                    style = TextStyle(
                        fontFamily = AppTheme.BricolageGrotesque,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color.White,
                        letterSpacing = 1.sp
                    )
                )
            }

            // Standings Table
            StandingsTable(tableItems = standing.table)
        }
    }
}

@Composable
fun StandingsTable(tableItems: List<TableItem>) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // Table Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(ColorSurfaceContainer)
                .border(width = 1.dp, color = ColorOutlineVariant)
                .padding(vertical = 8.dp, horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "#",
                modifier = Modifier.width(24.dp),
                style = TextStyle(fontFamily = AppTheme.JetBrainsMono, fontSize = 11.sp, color = ColorOnSurfaceVariant, fontWeight = FontWeight.Bold),
                textAlign = TextAlign.Center
            )
            Text(
                text = "TEAM",
                modifier = Modifier.weight(1f),
                style = TextStyle(fontFamily = AppTheme.JetBrainsMono, fontSize = 11.sp, color = ColorOnSurfaceVariant, fontWeight = FontWeight.Bold)
            )
            Text(
                text = "P",
                modifier = Modifier.width(28.dp),
                style = TextStyle(fontFamily = AppTheme.JetBrainsMono, fontSize = 11.sp, color = ColorOnSurfaceVariant, fontWeight = FontWeight.Bold),
                textAlign = TextAlign.Center
            )
            Text(
                text = "W",
                modifier = Modifier.width(28.dp),
                style = TextStyle(fontFamily = AppTheme.JetBrainsMono, fontSize = 11.sp, color = ColorOnSurfaceVariant, fontWeight = FontWeight.Bold),
                textAlign = TextAlign.Center
            )
            Text(
                text = "D",
                modifier = Modifier.width(28.dp),
                style = TextStyle(fontFamily = AppTheme.JetBrainsMono, fontSize = 11.sp, color = ColorOnSurfaceVariant, fontWeight = FontWeight.Bold),
                textAlign = TextAlign.Center
            )
            Text(
                text = "L",
                modifier = Modifier.width(28.dp),
                style = TextStyle(fontFamily = AppTheme.JetBrainsMono, fontSize = 11.sp, color = ColorOnSurfaceVariant, fontWeight = FontWeight.Bold),
                textAlign = TextAlign.Center
            )
            Text(
                text = "GD",
                modifier = Modifier.width(32.dp),
                style = TextStyle(fontFamily = AppTheme.JetBrainsMono, fontSize = 11.sp, color = ColorOnSurfaceVariant, fontWeight = FontWeight.Bold),
                textAlign = TextAlign.Center
            )
            Text(
                text = "PTS",
                modifier = Modifier.width(36.dp),
                style = TextStyle(fontFamily = AppTheme.JetBrainsMono, fontSize = 11.sp, color = ColorOnSurfaceVariant, fontWeight = FontWeight.Bold),
                textAlign = TextAlign.Center
            )
        }

        // Table Rows
        tableItems.forEachIndexed { index, item ->
            // Highlight qualifying top 2 spots with subtle green container background
            val isQualifying = index < 2
            val rowBgColor = if (isQualifying) Color(0x1A94F4AD) else Color.Transparent
            val borderLeftWidth = if (isQualifying) 4.dp else 0.dp
            val borderLeftColor = if (isQualifying) ColorPrimary else Color.Transparent

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(rowBgColor)
                    .drawLeftBorder(borderLeftWidth, borderLeftColor)
                    .border(width = 0.5.dp, color = ColorOutlineVariant.copy(alpha = 0.5f))
                    .padding(vertical = 10.dp, horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Position
                Text(
                    text = item.position.toString(),
                    modifier = Modifier.width(24.dp),
                    style = TextStyle(
                        fontFamily = AppTheme.JetBrainsMono,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E1B13)
                    ),
                    textAlign = TextAlign.Center
                )

                // Team Flag & Name
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val bitmap = remember(item.team.crest) {
                        try {
                            if (!item.team.crest.isNullOrBlank() && File(item.team.crest).exists()) {
                                BitmapFactory.decodeFile(item.team.crest)
                            } else {
                                null
                            }
                        } catch (e: Exception) {
                            null
                        }
                    }

                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .background(ColorSurface, shape = CircleShape)
                            .border(1.2.dp, Color(0xFF1E1B13), CircleShape)
                            .clip(CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (bitmap != null) {
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = item.team.name,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Text(
                                text = item.team.tla?.take(2) ?: "??",
                                fontSize = 10.sp,
                                fontFamily = AppTheme.JetBrainsMono,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = item.team.name ?: "Unknown",
                        style = TextStyle(
                            fontFamily = AppTheme.HankenGrotesk,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color(0xFF1E1B13)
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Stats: P, W, D, L, GD, PTS
                Text(
                    text = item.playedGames.toString(),
                    modifier = Modifier.width(28.dp),
                    style = TextStyle(fontFamily = AppTheme.JetBrainsMono, fontSize = 12.sp, color = ColorOnSurfaceVariant),
                    textAlign = TextAlign.Center
                )
                Text(
                    text = item.won.toString(),
                    modifier = Modifier.width(28.dp),
                    style = TextStyle(fontFamily = AppTheme.JetBrainsMono, fontSize = 12.sp, color = ColorOnSurfaceVariant),
                    textAlign = TextAlign.Center
                )
                Text(
                    text = item.draw.toString(),
                    modifier = Modifier.width(28.dp),
                    style = TextStyle(fontFamily = AppTheme.JetBrainsMono, fontSize = 12.sp, color = ColorOnSurfaceVariant),
                    textAlign = TextAlign.Center
                )
                Text(
                    text = item.lost.toString(),
                    modifier = Modifier.width(28.dp),
                    style = TextStyle(fontFamily = AppTheme.JetBrainsMono, fontSize = 12.sp, color = ColorOnSurfaceVariant),
                    textAlign = TextAlign.Center
                )
                
                val gdPrefix = if (item.goalDifference > 0) "+" else ""
                Text(
                    text = "$gdPrefix${item.goalDifference}",
                    modifier = Modifier.width(32.dp),
                    style = TextStyle(fontFamily = AppTheme.JetBrainsMono, fontSize = 12.sp, color = ColorOnSurfaceVariant),
                    textAlign = TextAlign.Center
                )

                Text(
                    text = item.points.toString(),
                    modifier = Modifier.width(36.dp),
                    style = TextStyle(
                        fontFamily = AppTheme.JetBrainsMono,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = ColorPrimary
                    ),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

// Custom extension to draw left border for qualifying teams
fun Modifier.drawLeftBorder(width: androidx.compose.ui.unit.Dp, color: Color): Modifier {
    if (width == 0.dp) return this
    return this.drawBehind {
        val strokeWidthPx = width.toPx()
        val y = size.height
        drawLine(
            color = color,
            start = androidx.compose.ui.geometry.Offset(strokeWidthPx / 2, 0f),
            end = androidx.compose.ui.geometry.Offset(strokeWidthPx / 2, y),
            strokeWidth = strokeWidthPx
        )
    }
}
