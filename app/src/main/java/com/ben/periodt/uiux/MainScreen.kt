package com.ben.periodt.uiux

import android.app.Application
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SpaceDashboard
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.ben.periodt.uiux.calendar.AddCycleDialog
import com.ben.periodt.uiux.calendar.CalendarScreen
import com.ben.periodt.uiux.overview.OverviewScreen
import com.ben.periodt.uiux.overview.SettingsScreen
import com.ben.periodt.uiux.pill.PillTrackerScreen
import com.ben.periodt.uiux.pill.PillTrackingSetupDialog
import com.ben.periodt.viewmodel.PeriodViewModel
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import dev.chrisbanes.haze.hazeChild

// --- NAVIGATION CONFIGURATION ---
sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    object Calendar : Screen("calendar", "Calendar", Icons.Rounded.CalendarMonth)
    object Pill : Screen("pill", "Pills", Icons.Default.Medication)
    object Overview : Screen("overview", "Overview", Icons.Rounded.SpaceDashboard)
    object Settings : Screen("settings", "Settings", Icons.Rounded.Settings)
}

@Composable
fun SmoothBottomNavigation(
    screens: List<Screen>,
    currentRoute: String?,
    onNavigate: (String) -> Unit,
    hazeState: HazeState,
    modifier: Modifier = Modifier
) {
    val navBarBg = Color(0xFF2A3825).copy(alpha = 0.5f)
    val selectedContent = Color.White
    val unselectedContent = Color.White.copy(alpha = 0.4f)

    val slotWidth = 72.dp
    val barHeight = 58.dp
    val navShape = RoundedCornerShape(29.dp)

    Box(
        modifier = modifier
            .wrapContentWidth()
            .height(barHeight)
            .shadow(
                elevation = 16.dp,
                shape = navShape,
                ambientColor = Color.Black.copy(alpha = 0.6f),
                spotColor = Color.Black.copy(alpha = 0.6f)
            )
            .hazeChild(state = hazeState, shape = navShape)
            .clip(navShape)
            .background(navBarBg)
    ) {
        Row(
            modifier = Modifier.fillMaxHeight(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            screens.forEach { screen ->
                val isSelected = screen.route == currentRoute

                val contentColor by animateColorAsState(
                    targetValue = if (isSelected) selectedContent else unselectedContent,
                    animationSpec = tween(durationMillis = 400),
                    label = "contentColor"
                )

                Box(
                    modifier = Modifier
                        .width(slotWidth)
                        .fillMaxHeight()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { onNavigate(screen.route) }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = screen.icon,
                        contentDescription = screen.label,
                        tint = contentColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun MainScreen() {
    val isDark = isSystemInDarkTheme()

    val bgGradient = if (isDark) {
        Brush.linearGradient(
            0.0f to Color.Black,
            0.7f to Color.Black,
            1.0f to Color(0xFF1b1b1b),
            start = Offset(0f, 0f),
            end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
        )
    } else {
        Brush.linearGradient(
            colors = listOf(Color(0xFFe8ebed), Color(0xFFf2f0e3)),
            start = Offset(0f, 0f),
            end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
        )
    }

    val navBarBg = Color(0xFF2A3825).copy(alpha = 0.5f)
    val fabShape = RoundedCornerShape(29.dp)

    val navController = rememberNavController()
    val context = androidx.compose.ui.platform.LocalContext.current.applicationContext as Application
    val viewModel: PeriodViewModel = viewModel(factory = PeriodViewModel.Factory(context))
    val screens = listOf(Screen.Calendar, Screen.Pill, Screen.Overview)

    var showAddCycleDialog by remember { mutableStateOf(false) }
    var showAddPillDialog by remember { mutableStateOf(false) }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Create the HazeState bridge
    val hazeState = remember { HazeState() }

    Box(modifier = Modifier.fillMaxSize().background(bgGradient)) {
        NavHost(
            navController = navController,
            startDestination = Screen.Calendar.route,
            modifier = Modifier
                .fillMaxSize()
                .haze(state = hazeState) // Capture the background content for blurring
                .windowInsetsPadding(WindowInsets.statusBars)
        ) {
            composable(Screen.Calendar.route) { CalendarScreen() }
            composable(Screen.Pill.route) { PillTrackerScreen(viewModel) }
            composable(Screen.Overview.route) { OverviewScreen(viewModel) }

            // Added slide animations for the Settings Screen
            composable(
                route = Screen.Settings.route,
                enterTransition = {
                    slideIntoContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Left,
                        animationSpec = tween(400)
                    )
                },
                popExitTransition = {
                    slideOutOfContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Right,
                        animationSpec = tween(400)
                    )
                }
            ) {
                SettingsScreen(onBack = { navController.popBackStack() }, viewModel = viewModel)
            }
        }

        val showBottomUi = currentRoute != Screen.Settings.route

        AnimatedVisibility(
            visible = showBottomUi,
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(horizontal = 24.dp, vertical = 24.dp)
            ) {
                Box(modifier = Modifier.align(Alignment.CenterStart).width(60.dp).height(58.dp)) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .shadow(
                                elevation = 16.dp,
                                shape = fabShape,
                                ambientColor = Color.Black.copy(alpha = 0.6f),
                                spotColor = Color.Black.copy(alpha = 0.6f)
                            )
                            // 1. Apply the haze effect to the FAB
                            .hazeChild(state = hazeState, shape = fabShape)
                            .clip(fabShape)
                            // 2. Apply the translucent background
                            .background(navBarBg)
                            // 3. Make it clickable
                            .clickable {
                                when (currentRoute) {
                                    Screen.Pill.route -> showAddPillDialog = true
                                    Screen.Overview.route -> navController.navigate(Screen.Settings.route)
                                    else -> showAddCycleDialog = true
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        val iconColor = Color.White
                        AnimatedContent(targetState = currentRoute, label = "FABIconTransition") { route ->
                            when (route) {
                                Screen.Pill.route -> CapsuleIcon(color = iconColor)
                                Screen.Overview.route -> Icon(
                                    imageVector = Icons.Rounded.Settings,
                                    contentDescription = "Settings",
                                    tint = iconColor,
                                    modifier = Modifier.size(24.dp)
                                )
                                else -> ClickyAddIcon(tint = iconColor)
                            }
                        }
                    }
                }

                // --- NAVBAR ON THE RIGHT ---
                Box(modifier = Modifier.align(Alignment.BottomEnd).height(58.dp)) {
                    SmoothBottomNavigation(
                        screens = screens,
                        currentRoute = currentRoute,
                        hazeState = hazeState, // Pass the state to the navbar
                        onNavigate = { route ->
                            navController.navigate(route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }

        if (showAddPillDialog) {
            PillTrackingSetupDialog(
                onDismiss = { showAddPillDialog = false },
                onSave = { startDate, pillCount ->
                    viewModel.enablePillTracking(startDate, pillCount)
                    showAddPillDialog = false
                }
            )
        }

        if (showAddCycleDialog) {
            AddCycleDialog(
                onDismiss = { showAddCycleDialog = false },
                onSave = { start, end, bleed, color, pain ->
                    viewModel.addCycle(start, end, bleed, color, pain)
                    showAddCycleDialog = false
                }
            )
        }
    }
}

@Composable
fun CapsuleIcon(color: Color) {
    Box(
        modifier = Modifier
            .size(24.dp)
            .rotate(-45f),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .width(10.dp)
                .height(20.dp)
                .clip(RoundedCornerShape(100.dp))
                .border(1.5.dp, color.copy(alpha = 0.8f), RoundedCornerShape(100.dp))
        ) {
            Box(modifier = Modifier.fillMaxWidth().weight(1f).background(color))
            Box(modifier = Modifier.fillMaxWidth().weight(1f).background(color.copy(alpha = 0.2f)))
        }
    }
}

// Updated to use the Add (+) icon and removed the redundant click listener
@Composable
fun ClickyAddIcon(tint: Color) {
    Box(modifier = Modifier.size(24.dp), contentAlignment = Alignment.Center) {
        Icon(imageVector = Icons.Rounded.Add, contentDescription = "Add", tint = tint)
    }
}