package com.example.kickoffwidget

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.example.kickoffwidget.theme.AppTheme
import com.example.kickoffwidget.ui.main.MainScreen
import com.example.kickoffwidget.ui.standings.StandingsScreen

// Theme colors matching the design system
private val ColorSurfaceContainer = Color(0xFFF5EDDE)
private val ColorOutlineVariant = Color(0xFFD8C3AD)
private val ColorPrimaryContainer = Color(0xFFF39C12)
private val ColorOnPrimaryFixed = Color(0xFF2B1700)
private val ColorOnSurfaceVariant = Color(0xFF534434)
private val ColorPrimary = Color(0xFF865300)

@Composable
fun MainNavigation() {
  val backStack = rememberNavBackStack(Main)
  val currentKey = backStack.lastOrNull()

  Scaffold(
      bottomBar = {
          BottomNavigationBar(
              currentKey = currentKey,
              onTabSelected = { targetKey ->
                  if (currentKey != targetKey) {
                      if (backStack.isNotEmpty()) {
                          backStack[0] = targetKey
                          while (backStack.size > 1) {
                              backStack.removeAt(backStack.size - 1)
                          }
                      } else {
                          backStack.add(targetKey)
                      }
                  }
              }
          )
      }
  ) { innerPadding ->
      NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        entryProvider =
          entryProvider {
            entry<Main> {
              MainScreen(
                  onItemClick = { navKey -> backStack.add(navKey) },
                  modifier = Modifier
                      .padding(innerPadding)
                      .safeDrawingPadding()
              )
            }
            entry<Standings> {
              StandingsScreen(
                  modifier = Modifier
                      .padding(innerPadding)
                      .safeDrawingPadding()
              )
            }
          },
      )
  }
}

@Composable
fun BottomNavigationBar(
    currentKey: androidx.navigation3.runtime.NavKey?,
    onTabSelected: (androidx.navigation3.runtime.NavKey) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
            .background(ColorSurfaceContainer, shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            .border(
                width = 2.dp,
                color = ColorOutlineVariant,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            // Tab 1: Configuration / Home
            val isMainActive = currentKey is Main
            BottomBarItem(
                isActive = isMainActive,
                icon = "⚙️",
                label = "WIDGET CONFIG",
                onClick = { onTabSelected(Main) }
            )

            // Tab 2: Group Standings
            val isStandingsActive = currentKey is Standings
            BottomBarItem(
                isActive = isStandingsActive,
                icon = "🏆",
                label = "STANDINGS",
                onClick = { onTabSelected(Standings) }
            )
        }
    }
}

@Composable
fun BottomBarItem(
    isActive: Boolean,
    icon: String,
    label: String,
    onClick: () -> Unit
) {
    if (isActive) {
        // Active Tab with tactile drop-block design
        Box(
            modifier = Modifier
                .width(150.dp)
                .height(48.dp)
                .clickable { onClick() }
        ) {
            // Shadow (back layer)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(ColorOnPrimaryFixed, shape = RoundedCornerShape(12.dp))
            )
            // Front button layer
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .offset(x = (-2).dp, y = (-2).dp)
                    .background(ColorPrimaryContainer, shape = RoundedCornerShape(12.dp))
                    .border(2.dp, ColorOnPrimaryFixed, RoundedCornerShape(12.dp)),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "$icon  ",
                    fontSize = 18.sp
                )
                Text(
                    text = label,
                    style = TextStyle(
                        fontFamily = AppTheme.BricolageGrotesque,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = ColorOnPrimaryFixed,
                        letterSpacing = 0.5.sp
                    )
                )
            }
        }
    } else {
        // Inactive Tab
        Row(
            modifier = Modifier
                .width(150.dp)
                .height(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .clickable { onClick() }
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = "$icon  ",
                fontSize = 16.sp,
                color = ColorOnSurfaceVariant
            )
            Text(
                text = label,
                style = TextStyle(
                    fontFamily = AppTheme.HankenGrotesk,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = ColorOnSurfaceVariant,
                    letterSpacing = 0.5.sp
                )
            )
        }
    }
}
