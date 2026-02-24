package com.ben.periodt.uiux

import android.app.Application
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
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
import com.ben.periodt.uiux.calendar.AddCycleDialog
import com.ben.periodt.uiux.calendar.CalendarScreen
import com.ben.periodt.uiux.overview.OverviewScreen
import com.ben.periodt.uiux.overview.SettingsDialog
import com.ben.periodt.viewmodel.PeriodViewModel
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.SpaceDashboard
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    object Calendar : Screen("calendar", "Calendar", Icons.Rounded.CalendarMonth)
    object Overview : Screen("overview", "Overview", Icons.Rounded.SpaceDashboard)
}

@Composable
fun SmoothBottomNavigation(
    screens: List<Screen>,
    currentRoute: String?,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()

    // Kept your requested solid brush logic
    val navBarBrush = if (isDark) {
        Brush.linearGradient(
            colors = listOf(Color(0xFF8089D2), Color(0xFF8089D2))
        )
    } else {
        Brush.linearGradient(
            colors = listOf(Color(0xFFFFFFFF), Color(0xFFFFFFFF))
        )
    }

    val selectedBg = if (isDark) Color(color = 0xFF1B1B1B).copy(alpha = 0.6f) else Color(color = 0XFF8089D2)
    val selectedContent = Color.White
    val unselectedContent = if (isDark) Color.White.copy(alpha = 0.5f) else Color.Black.copy(alpha = 0.3f)

    val selectedIndex = screens.indexOfFirst { it.route == currentRoute }.coerceAtLeast(0)
    val slotWidth = 95.dp
    val itemHeight = 46.dp

    val indicatorOffset by animateDpAsState(targetValue = slotWidth * selectedIndex, label = "indicatorOffset")

    Box(
        modifier = modifier
            .wrapContentWidth()
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(28.dp),
                ambientColor = Color.Black.copy(alpha = 0.6f),
                spotColor = Color.Black.copy(alpha = 0.6f)
            )
            .clip(RoundedCornerShape(28.dp))
            .background(navBarBrush)
            .padding(horizontal = 9.dp, vertical = 8.dp)
    ) {
        // Moving Indicator
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(slotWidth)
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
                val interaction = remember { MutableInteractionSource() }

                Box(
                    modifier = Modifier.width(slotWidth).height(itemHeight),
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
                        modifier = Modifier.size(24.dp) // Updated to 24.dp to match other icons
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
    // Dark Mode: Black (more dominant) to Blueberry (#2C3F70)
    // Light Mode: Meringe (#E8EBED) to Buttercream (#C8D4E5)
    val bgGradient = if (isDark) {
        Brush.linearGradient(
            0.0f to Color.Black,
            0.7f to Color.Black, // Pushes Black further into the center
            1.0f to Color(0xFF2C3F70),
            start = Offset(0f, 0f),
            end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
        )
    } else {
        Brush.linearGradient(
            colors = listOf(Color(0xFFe8ebed), Color(0xFFc8d4e5)),
            start = Offset(0f, 0f),
            end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
        )
    }

    // Updated FAB background logic: Violet (#8089D2) in Dark, White in Light
    val buttonBrush = if (isDark) Color(0xFF8089D2) else Color.White

    val navController = rememberNavController()
    val context = androidx.compose.ui.platform.LocalContext.current.applicationContext as Application
    val viewModel: PeriodViewModel = viewModel(factory = PeriodViewModel.Factory(context))
    val screens = listOf(Screen.Calendar, Screen.Overview)

    SetSystemBars(statusBarColor = Color.Transparent, darkIcons = !isDark)

    var showAddCycleDialog by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }

    val fadeInSmooth = fadeIn(tween(300, easing = FastOutSlowInEasing))
    val fadeOutSmooth = fadeOut(tween(250, easing = FastOutLinearInEasing))

    Box(modifier = Modifier.fillMaxSize().background(bgGradient)) {
        NavHost(
            navController = navController,
            startDestination = Screen.Calendar.route,
            modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.statusBars)
        ) {
            composable(Screen.Calendar.route, enterTransition = { fadeInSmooth }, exitTransition = { fadeOutSmooth }) { CalendarScreen() }
            composable(Screen.Overview.route, enterTransition = { fadeInSmooth }, exitTransition = { fadeOutSmooth }) { OverviewScreen(viewModel) }
        }

        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route
        val isOverview = currentRoute == Screen.Overview.route

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
            val gapAboveFab = 20.dp

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

            Box(modifier = Modifier.align(Alignment.CenterEnd).width(fabSize).height(fabSize + gapAboveFab + 24.dp)) {
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
                        ClickyAddIcon(tint = if (isDark) Color.White else Color(0xFF2C3F70)) {
                            showAddCycleDialog = true
                        }
                    }
                }

                AnimatedVisibility(
                    visible = isOverview,
                    enter = slideInVertically(initialOffsetY = { it }, animationSpec = tween(200, 50, FastOutSlowInEasing)) + fadeIn(tween(250, 100, LinearOutSlowInEasing)),
                    exit = slideOutVertically(targetOffsetY = { it }, animationSpec = tween(200, easing = FastOutLinearInEasing)) + fadeOut(tween(200, easing = FastOutLinearInEasing)),
                    modifier = Modifier.align(Alignment.TopCenter).offset(y = gapAboveFab * (-0.5f))
                ) {
                    IconButton(onClick = { showSettings = true }, modifier = Modifier.size(settingsIconSize + 16.dp)) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = if (isDark) Color.White else Color.Black,
                            modifier = Modifier.size(settingsIconSize)
                        )
                    }
                }
            }

            if (showSettings && isOverview) SettingsDialog(show = true, onClose = { showSettings = false })
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
}