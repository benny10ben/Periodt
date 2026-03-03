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
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.ui.zIndex
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
import com.ben.periodt.uiux.overview.WhatsNewDialog
import com.ben.periodt.uiux.pill.PillTrackerScreen
import com.ben.periodt.uiux.pill.PillTrackingSetupDialog
import com.ben.periodt.viewmodel.PeriodViewModel
import kotlinx.coroutines.delay

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
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val smoothSpec = tween<Dp>(durationMillis = 500, easing = FastOutSlowInEasing)

    val navBarBg = if (isDark) Color(0xFF2A3825) else Color(0xFFFFFFFF)
    val selectionPillBg = if (isDark) Color(0xFF1B1B1B) else Color(0xFF2A3825)

    val selectedContent = Color.White
    val unselectedContent = if (isDark) Color.White.copy(alpha = 0.5f) else Color.Black.copy(alpha = 0.3f)

    val selectedIndex = screens.indexOfFirst { it.route == currentRoute }.coerceAtLeast(0)

    val paddingValue = 6.dp
    val slotWidth = 72.dp
    val barHeight = 58.dp
    val pillWidth = 62.dp

    val indicatorOffset by animateDpAsState(
        targetValue = (slotWidth * selectedIndex),
        animationSpec = smoothSpec,
        label = "indicatorOffset"
    )

    Box(
        modifier = modifier
            .wrapContentWidth()
            .height(barHeight)
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(29.dp),
                ambientColor = Color.Black.copy(alpha = 0.4f),
                spotColor = Color.Black.copy(alpha = 0.4f)
            )
            .clip(RoundedCornerShape(29.dp))
            .background(navBarBg)
            .padding(all = paddingValue)
    ) {
        Box(
            modifier = Modifier
                .offset(x = indicatorOffset + (slotWidth - pillWidth) / 2)
                .align(Alignment.CenterStart)
                .width(pillWidth)
                .fillMaxHeight()
                .clip(RoundedCornerShape(23.dp))
                .background(selectionPillBg)
        )

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

    // --- VISUAL SETUP ---
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

    val buttonBrush = if (isDark) {
        Brush.linearGradient(colors = listOf(Color(0xFF2A3825), Color(0xFF2A3825)))
    } else {
        Brush.linearGradient(colors = listOf(Color(0xFFFFFFFF), Color(0xFFFFFFFF)))
    }

    // --- NAVIGATION & STATE ---
    val navController = rememberNavController()
    val context = androidx.compose.ui.platform.LocalContext.current.applicationContext as Application
    val viewModel: PeriodViewModel = viewModel(factory = PeriodViewModel.Factory(context))
    val screens = listOf(Screen.Calendar, Screen.Pill, Screen.Overview)

    var showAddCycleDialog by remember { mutableStateOf(false) }
    var showAddPillDialog by remember { mutableStateOf(false) }
    var showWhatsNew by remember { mutableStateOf(false) }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    SetSystemBars(statusBarColor = Color.Transparent, darkIcons = !isDark)

    Box(modifier = Modifier.fillMaxSize().background(bgGradient)) {
        // --- PAGE CONTENT ---
        NavHost(
            navController = navController,
            startDestination = Screen.Calendar.route,
            modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.statusBars)
        ) {
            composable(Screen.Calendar.route) { CalendarScreen() }
            composable(Screen.Pill.route) { PillTrackerScreen(viewModel) }
            composable(Screen.Overview.route) { OverviewScreen(viewModel) }
            composable(Screen.Settings.route) {
                SettingsScreen(onBack = { navController.popBackStack() }, viewModel = viewModel)
            }
        }

        // --- BOTTOM UI (NAV + FAB) ---
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
                // Navigation Bar
                Box(modifier = Modifier.align(Alignment.BottomStart).height(58.dp)) {
                    SmoothBottomNavigation(
                        screens = screens,
                        currentRoute = currentRoute,
                        onNavigate = { route ->
                            navController.navigate(route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }

                // --- HARMONIZED SMART FAB ---
                Box(modifier = Modifier.align(Alignment.CenterEnd).width(60.dp).height(58.dp)) {
                    Surface(
                        modifier = Modifier
                            .fillMaxSize()
                            .shadow(
                                elevation = 8.dp, // Matches Navbar
                                shape = CircleShape,
                                ambientColor = Color.Black.copy(alpha = 0.4f),
                                spotColor = Color.Black.copy(alpha = 0.4f)
                            ),
                        shape = CircleShape,
                        color = Color.Transparent,
                        onClick = {
                            when (currentRoute) {
                                Screen.Pill.route -> showAddPillDialog = true // Triggers Dialog
                                Screen.Overview.route -> navController.navigate(Screen.Settings.route)
                                else -> showAddCycleDialog = true
                            }
                        }
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize().background(buttonBrush),
                            contentAlignment = Alignment.Center
                        ) {
                            val iconColor = if (isDark) Color.White else Color(0xFF2A3825)

                            AnimatedContent(targetState = currentRoute, label = "FABIconTransition") { route ->
                                when (route) {
                                    // Icons standardized to 24.dp
                                    Screen.Pill.route -> CapsuleIcon(color = iconColor)
                                    Screen.Overview.route -> Icon(
                                        imageVector = Icons.Rounded.Settings,
                                        contentDescription = "Settings",
                                        tint = iconColor,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    else -> ClickyAddIcon(tint = iconColor) {
                                        showAddCycleDialog = true
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showAddPillDialog) {
            PillTrackingSetupDialog(
                onDismiss = { showAddPillDialog = false },
                onSave = { startDate, pillCount ->
                    // Let the ViewModel handle saving and updating the state!
                    viewModel.enablePillTracking(startDate, pillCount)
                    showAddPillDialog = false
                    navController.navigate(Screen.Pill.route) {
                        popUpTo(Screen.Pill.route) { inclusive = true }
                    }
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

/**
 * A custom-drawn capsule icon standardized to 24.dp to match Navbar icons
 */
@Composable
fun CapsuleIcon(color: Color) {
    Box(
        modifier = Modifier
            .size(24.dp) // Size matched to Nav icons
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