package com.ben.periodt.uiux

import android.app.Application
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SpaceDashboard
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.ben.periodt.uiux.overview.SettingsScreen // Ensure this is imported
import com.ben.periodt.uiux.overview.WhatsNewDialog
import com.ben.periodt.viewmodel.PeriodViewModel
import kotlinx.coroutines.delay

sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    object Calendar : Screen("calendar", "Calendar", Icons.Rounded.CalendarMonth)
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

    // --- THE STEADY GLIDE SPEC ---
    // Match this to your MainScreen (700ms) to ensure the bar and screen move together
    val smoothSpec = tween<Dp>(
        durationMillis = 700,
        easing = FastOutSlowInEasing // Prevents the jumpy/bouncy feel
    )

    val navBarBrush = if (isDark) {
        Brush.linearGradient(colors = listOf(Color(0xFF2A3825), Color(0xFF2A3825)))
    } else {
        Brush.linearGradient(colors = listOf(Color(0xFFFFFFFF), Color(0xFFFFFFFF)))
    }

    val selectedBg = if (isDark) Color(0xFF1B1B1B) else Color(0xFF2A3825)
    val selectedContent = Color.White
    val unselectedContent = if (isDark) Color.White.copy(alpha = 0.5f) else Color.Black.copy(alpha = 0.3f)

    val selectedIndex = screens.indexOfFirst { it.route == currentRoute }.coerceAtLeast(0)
    val slotWidth = 95.dp
    val itemHeight = 46.dp

    // Updated offset animation with the smoothSpec
    val indicatorOffset by animateDpAsState(
        targetValue = slotWidth * selectedIndex,
        animationSpec = smoothSpec,
        label = "indicatorOffset"
    )

    Box(
        modifier = modifier
            .wrapContentWidth()
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(28.dp),
                ambientColor = Color.Black.copy(alpha = 0.4f),
                spotColor = Color.Black.copy(alpha = 0.4f)
            )
            .clip(RoundedCornerShape(28.dp))
            .background(navBarBrush)
            .padding(horizontal = 9.dp, vertical = 8.dp)
    ) {
        // The Sliding Background Pill
        Box(
            modifier = Modifier
                .height(itemHeight)
                .width(slotWidth)
                .offset(x = indicatorOffset)
                .clip(RoundedCornerShape(22.dp)) // Slightly tighter corner for the inner pill
                .background(selectedBg)
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            screens.forEachIndexed { index, screen ->
                val isSelected = index == selectedIndex

                // Content color also needs a smooth fade to match the motion
                val contentColor by animateColorAsState(
                    targetValue = if (isSelected) selectedContent else unselectedContent,
                    animationSpec = tween(durationMillis = 500),
                    label = "contentColor"
                )

                val interaction = remember { MutableInteractionSource() }

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
                                onClick = { onNavigate(screen.route) }
                            )
                    )
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

    // UPDATED SCREEN BACKGROUNDS (Diagonal Gradient)
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

    val navController = rememberNavController()
    val context = androidx.compose.ui.platform.LocalContext.current.applicationContext as Application
    val viewModel: PeriodViewModel = viewModel(factory = PeriodViewModel.Factory(context))
    val screens = listOf(Screen.Calendar, Screen.Overview)

    // Auto-show What's New on update
    var showWhatsNew by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val currentVersion = context.packageManager
            .getPackageInfo(context.packageName, 0)
            .versionCode
        val lastSeen = VersionPrefs.getLastSeenVersion(context)
        if (currentVersion > lastSeen) {
            VersionPrefs.setLastSeenVersion(context, currentVersion)
            delay(3000)
            showWhatsNew = true
        }
    }
    SetSystemBars(statusBarColor = Color.Transparent, darkIcons = !isDark)

    var showAddCycleDialog by remember { mutableStateOf(false) }

    val fadeInSmooth = fadeIn(tween(300, easing = FastOutSlowInEasing))
    val fadeOutSmooth = fadeOut(tween(250, easing = FastOutLinearInEasing))

    Box(modifier = Modifier.fillMaxSize().background(bgGradient)) {
        NavHost(
            navController = navController,
            startDestination = Screen.Calendar.route,
            modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.statusBars)
        ) {
            // Calendar Screen
            composable(Screen.Calendar.route, enterTransition = { fadeInSmooth }, exitTransition = { fadeOutSmooth }) {
                CalendarScreen()
            }

            // Overview Screen
            composable(Screen.Overview.route, enterTransition = { fadeInSmooth }, exitTransition = { fadeOutSmooth }) {
                OverviewScreen(viewModel)
            }

            // Settings Screen (Slides in from right)
            composable(
                route = Screen.Settings.route,
                enterTransition = { slideInHorizontally(initialOffsetX = { it }) },
                exitTransition = { slideOutHorizontally(targetOffsetX = { it }) }
            ) {
                SettingsScreen(
                    onBack = { navController.popBackStack() },
                    viewModel = viewModel
                )
            }
        }

        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route
        val isOverview = currentRoute == Screen.Overview.route
        // Hide bottom bar/FAB when on Settings screen
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
                    .padding(horizontal = 35.dp, vertical = 20.dp)
            ) {
                val barHeight = 60.dp
                val fabSize = 58.dp
                val settingsIconSize = 27.dp
                val gapAboveFab = 20.dp

                // Bottom Navigation Bar
                Box(modifier = Modifier.align(Alignment.BottomStart).height(barHeight).widthIn(max = 300.dp)) {
                    SmoothBottomNavigation(
                        screens = screens,
                        currentRoute = currentRoute,
                        onNavigate = { route ->
                            navController.navigate(route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        modifier = Modifier.align(Alignment.CenterStart)
                    )
                }

                // FAB & Settings Button Area
                Box(modifier = Modifier.align(Alignment.CenterEnd).width(fabSize).height(fabSize + gapAboveFab + 24.dp)) {

                    // Floating Action Button (Add Cycle)
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .size(fabSize)
                            .zIndex(1f)
                            .shadow(
                                elevation = 8.dp,
                                shape = CircleShape,
                                ambientColor = Color.Black.copy(alpha = 0.6f),
                                spotColor = Color.Black.copy(alpha = 0.6f)
                            ),
                        shape = CircleShape,
                        color = Color.Transparent,
                        onClick = { showAddCycleDialog = true }
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                                .background(buttonBrush),
                            contentAlignment = Alignment.Center
                        ) {
                            ClickyAddIcon(tint = if (isDark) Color.White else Color(0xFF2A3825)) {
                                showAddCycleDialog = true
                            }
                        }
                    }

                    // Settings Button (Transitions to new screen now)
                    AnimatedVisibility(
                        visible = isOverview,
                        enter = slideInVertically(initialOffsetY = { it }, animationSpec = tween(200, 50, FastOutSlowInEasing)) + fadeIn(tween(250, 100, LinearOutSlowInEasing)),
                        exit = slideOutVertically(targetOffsetY = { it }, animationSpec = tween(200, easing = FastOutLinearInEasing)) + fadeOut(tween(200, easing = FastOutLinearInEasing)),
                        modifier = Modifier.align(Alignment.TopCenter).offset(y = gapAboveFab * (-0.5f))
                    ) {
                        IconButton(
                            onClick = { navController.navigate(Screen.Settings.route) },
                            modifier = Modifier.size(settingsIconSize + 16.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings",
                                tint = if (isDark) Color.White else Color(0xFF2A3825),
                                modifier = Modifier.size(settingsIconSize)
                            )
                        }
                    }
                }
            }
        }

        // Add Cycle Dialog (Global)
        if (showAddCycleDialog) {
            AddCycleDialog(
                onDismiss = { showAddCycleDialog = false },
                onSave = { start, end, bleed, color, pain ->
                    viewModel.addCycle(start, end, bleed, color, pain)
                    showAddCycleDialog = false
                }
            )
        }
        if (showWhatsNew) {
            WhatsNewDialog(onDismiss = { showWhatsNew = false })
        }
    }
}