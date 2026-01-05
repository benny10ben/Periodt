package com.ben.periodt.uiux

import android.app.Application
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.ben.periodt.viewmodel.PeriodViewModel
import com.ben.periodt.uiux.calendar.CalendarScreen
import com.ben.periodt.uiux.calendar.AddCycleDialog
import com.ben.periodt.uiux.overview.OverviewScreen
import com.ben.periodt.uiux.overview.SettingsDialog

sealed class Screen(val route: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    object Calendar : Screen("calendar", "Calendar", Icons.Default.DateRange)
    object Overview : Screen("overview", "Overview", Icons.Default.Info)
}

@Composable
fun SmoothBottomNavigation(
    screens: List<Screen>,
    currentRoute: String?,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()

    val gradLeft   = if (isDark) Color(0xFF7B8FA3) else Color(0xFF8FA0B1)
    val gradMiddle = if (isDark) Color(0xFF7288A0) else Color(0xFF8799B0)
    val gradRight  = if (isDark) Color(0xFF5A7396) else Color(0xFF6E87A7)

    val selectedBg = if (isDark) Color.Black else Color.White
    val selectedContent = if (isDark) Color.White else Color.Black
    val unselectedContent = Color.White

    val selectedIndex = screens.indexOfFirst { it.route == currentRoute }.coerceAtLeast(0)

    val iconOnlySlotWidth = 95.dp
    val itemHeight = 46.dp
    val slotWidth = iconOnlySlotWidth

    val indicatorOffset by androidx.compose.animation.core.animateDpAsState(targetValue = slotWidth * selectedIndex, label = "indicatorOffset")
    val indicatorWidth by androidx.compose.animation.core.animateDpAsState(targetValue = iconOnlySlotWidth, label = "indicatorWidth")

    Box(
        modifier = modifier
            .wrapContentWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(Brush.horizontalGradient(listOf(gradLeft, gradMiddle, gradRight)))
            .padding(horizontal = 9.dp, vertical = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(indicatorWidth)
                .offset(x = indicatorOffset)
                .clip(RoundedCornerShape(28.dp))
                .background(selectedBg)
                .zIndex(-1f)
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            screens.forEachIndexed { index, screen ->
                val isSelected = index == selectedIndex
                val contentColor by androidx.compose.animation.animateColorAsState(
                    targetValue = if (isSelected) selectedContent else unselectedContent,
                    label = "contentColor"
                )
                val interaction = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }

                Box(
                    modifier = Modifier
                        .width(slotWidth)
                        .height(itemHeight),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable(
                                interactionSource = interaction,
                                indication = null,
                                role = androidx.compose.ui.semantics.Role.Tab,
                                onClick = { onNavigate(screen.route) }
                            )
                    )
                    Icon(
                        imageVector = screen.icon,
                        contentDescription = screen.label,
                        tint = contentColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun MainScreen() {
    val isDark = isSystemInDarkTheme()
    val bg = if (isDark) Color.Black else Color.White
    val navController = rememberNavController()
    val context = androidx.compose.ui.platform.LocalContext.current.applicationContext as Application
    val viewModel: PeriodViewModel = viewModel(factory = PeriodViewModel.Factory(context))
    val screens = listOf(Screen.Calendar, Screen.Overview)

    val bg1 = if (isDark) Color.Transparent else Color.Transparent
    SetSystemBars(statusBarColor = bg1, darkIcons = !isDark)

    var showAddCycleDialog by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }

    // Smoother fade transitions with easing
    val fadeInSmooth: EnterTransition = fadeIn(
        animationSpec = tween(
            durationMillis = 300,
            easing = FastOutSlowInEasing
        )
    )
    val fadeOutSmooth: ExitTransition = fadeOut(
        animationSpec = tween(
            durationMillis = 250,
            easing = FastOutLinearInEasing
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bg)
    ) {
        NavHost(
            navController = navController,
            startDestination = Screen.Calendar.route,
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
        ) {
            composable(
                route = Screen.Calendar.route,
                enterTransition = { fadeInSmooth },
                exitTransition = { fadeOutSmooth },
                popEnterTransition = { fadeInSmooth },
                popExitTransition = { fadeOutSmooth }
            ) { CalendarScreen() }

            composable(
                route = Screen.Overview.route,
                enterTransition = { fadeInSmooth },
                exitTransition = { fadeOutSmooth },
                popEnterTransition = { fadeInSmooth },
                popExitTransition = { fadeOutSmooth }
            ) { OverviewScreen(viewModel) }
        }

        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route
        val isOverview = currentRoute == Screen.Overview.route

        // Bottom overlay: nav at start, actions at end
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = 35.dp, vertical = 20.dp)
        ) {
            val barHeight = 60.dp
            val fabSize = 58.dp
            val settingsIconSize = 27.dp
            val gapAboveFab = 20.dp // how far above the + the settings icon should rest

            // Bottom navigation (left)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .height(barHeight)
                    .widthIn(max = 300.dp)
            ) {
                SmoothBottomNavigation(
                    screens = screens,
                    currentRoute = currentRoute,
                    onNavigate = { route ->
                        navController.navigate(route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    modifier = Modifier.align(Alignment.CenterStart)
                )
            }

            // Right: Box stacking settings over the FAB to animate from behind
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .width(fabSize)                         // anchor width matches FAB
                    .height(fabSize + gapAboveFab + 24.dp)  // extra room to slide above
            ) {
                // FAB pinned at bottom
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .size(fabSize)
                        .zIndex(1f),
                    shape = CircleShape,
                    shadowElevation = 1.dp,
                    tonalElevation = 1.dp,
                    color = Color.Transparent,
                    onClick = { showAddCycleDialog = true }
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(
                                brush = Brush.verticalGradient(
                                    listOf(
                                        Color(0xFF8FA0B1),
                                        Color(0xFF8799B0),
                                        Color(0xFF6E87A7)
                                    )
                                )
                            )
                            .background(Color.White.copy(alpha = 0.06f)),
                        contentAlignment = Alignment.Center
                    ) {
                        ClickyAddIcon(
                            tint = if (isDark) Color.Black else Color.White
                        ) { showAddCycleDialog = true }
                    }
                }

                // Settings icon: slides up and rests just above the FAB with smoother animation
                AnimatedVisibility(
                    visible = isOverview,
                    enter = slideInVertically(
                        // start from deeper behind FAB for longer travel
                        initialOffsetY = { fullHeight -> (fullHeight * 0.9f).toInt() },
                        animationSpec = tween(
                            durationMillis = 200,
                            delayMillis = 50, // slight delay for smoother feel
                            easing = FastOutSlowInEasing
                        )
                    ) + fadeIn(
                        animationSpec = tween(
                            durationMillis = 250,
                            delayMillis = 100,
                            easing = LinearOutSlowInEasing
                        )
                    ),
                    exit = slideOutVertically(
                        targetOffsetY = { fullHeight -> (fullHeight * 0.9f).toInt() },
                        animationSpec = tween(
                            durationMillis = 200,
                            easing = FastOutLinearInEasing
                        )
                    ) + fadeOut(
                        animationSpec = tween(
                            durationMillis = 200,
                            easing = FastOutLinearInEasing
                        )
                    ),
                    modifier = Modifier
                        .align(Alignment.TopCenter)  // top of anchor box
                        .offset(y = gapAboveFab * (-0.5f)) // nudge up so glyph sits above +, not on it
                ) {
                    IconButton(
                        onClick = { showSettings = true },
                        modifier = Modifier.size(settingsIconSize + 16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = if (isDark) Color.White else Color.Black,
                            modifier = Modifier.size(settingsIconSize)
                        )
                    }
                }
            }

            // Dialogs
            if (showSettings && isOverview) {
                SettingsDialog(show = true, onClose = { showSettings = false })
            }
            if (showAddCycleDialog) {
                AddCycleDialog(
                    onDismiss = { showAddCycleDialog = false },
                    onSave = { startDate, endDate, bleeding, bloodColor, painLevel ->
                        viewModel.addCycle(startDate, endDate, bleeding, bloodColor, painLevel)
                        showAddCycleDialog = false
                    }
                )
            }
        }
    }
}
