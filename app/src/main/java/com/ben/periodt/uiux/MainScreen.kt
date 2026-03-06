package com.ben.periodt.uiux

import android.app.Application
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.ben.periodt.ui.theme.LocalAppIsDark
import com.ben.periodt.uiux.calendar.AddCycleDialog
import com.ben.periodt.uiux.calendar.CalendarScreen
import com.ben.periodt.uiux.overview.OverviewScreen
import com.ben.periodt.uiux.overview.SettingsScreen
import com.ben.periodt.uiux.overview.ThemeMode
import com.ben.periodt.uiux.overview.THEME_MODE_KEY
import com.ben.periodt.uiux.overview.WhatsNewDialog
import com.ben.periodt.uiux.pill.PillTrackerScreen
import com.ben.periodt.uiux.pill.PillTrackingSetupDialog
import com.ben.periodt.uiux.shared.dataStore
import com.ben.periodt.viewmodel.PeriodViewModel
import com.ben.periodt.widget.CalendarWidgetProvider
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.haze
import dev.chrisbanes.haze.hazeChild
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ── Version key (shared with SettingsScreen) ──────────────────────────────────
val LAST_SEEN_VERSION_KEY = intPreferencesKey("last_seen_version")

// --- NAVIGATION CONFIGURATION ---
sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    object Calendar : Screen("calendar", "Calendar", Icons.Rounded.CalendarMonth)
    object Pill     : Screen("pill",     "Pills",    Icons.Default.Medication)
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
    val navBarBg          = Color(0xFF6d9567).copy(alpha = 0.5f)
    val selectedContent   = Color.White
    val unselectedContent = Color.White.copy(alpha = 0.4f)

    val slotWidth = 72.dp
    val barHeight = 58.dp
    val navShape  = RoundedCornerShape(29.dp)

    Box(
        modifier = modifier
            .wrapContentWidth()
            .height(barHeight)
            .shadow(
                elevation    = 16.dp,
                shape        = navShape,
                ambientColor = Color.Black.copy(alpha = 0.6f),
                spotColor    = Color.Black.copy(alpha = 0.6f)
            )
            .hazeChild(state = hazeState, shape = navShape, style = HazeStyle(blurRadius = 14.dp))
            .clip(navShape)
            .background(navBarBg)
    ) {
        Row(
            modifier          = Modifier.fillMaxHeight(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            screens.forEach { screen ->
                val isSelected = screen.route == currentRoute

                val contentColor by animateColorAsState(
                    targetValue   = if (isSelected) selectedContent else unselectedContent,
                    animationSpec = tween(durationMillis = 400),
                    label         = "contentColor"
                )

                Box(
                    modifier = Modifier
                        .width(slotWidth)
                        .fillMaxHeight()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication        = null,
                            onClick           = { onNavigate(screen.route) }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector        = screen.icon,
                        contentDescription = screen.label,
                        tint               = contentColor,
                        modifier           = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

// ROOT COMPOSABLE — resolves the theme once for the entire app
@RequiresApi(Build.VERSION_CODES.P)
@Composable
fun MainScreen() {
    val context = LocalContext.current

    val prefs by context.dataStore.data.collectAsState(initial = null)
    val savedThemeMode = prefs?.get(THEME_MODE_KEY) ?: ThemeMode.SYSTEM.name

    val systemIsDark = isSystemInDarkTheme()
    val isDark = when (ThemeMode.valueOf(savedThemeMode)) {
        ThemeMode.LIGHT  -> false
        ThemeMode.DARK   -> true
        ThemeMode.SYSTEM -> systemIsDark
    }

    LaunchedEffect(isDark) {
        context.getSharedPreferences("widget_prefs", Context.MODE_PRIVATE)
            .edit().putBoolean("is_dark", isDark).apply()
        CalendarWidgetProvider.refreshAll(context)
    }

    CompositionLocalProvider(LocalAppIsDark provides isDark) {
        MainScreenContent(isDark = isDark)
    }
}

// MAIN LAYOUT
@RequiresApi(Build.VERSION_CODES.P)
@Composable
private fun MainScreenContent(isDark: Boolean) {

    SetSystemBars(
        statusBarColor = Color.Transparent,
        darkIcons      = !isDark
    )

    // ── What's New auto-show ───────────────────────────────────────────────
    val context        = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val prefs          by context.dataStore.data.collectAsState(initial = null)
    var showWhatsNew   by remember { mutableStateOf(false) }

    val currentVersion = remember {
        context.packageManager.getPackageInfo(context.packageName, 0).longVersionCode.toInt()
    }

    LaunchedEffect(prefs) {
        prefs ?: return@LaunchedEffect
        val lastSeen = prefs?.get(LAST_SEEN_VERSION_KEY) ?: 0
        if (lastSeen < currentVersion) {
            delay(3000L)
            showWhatsNew = true
            coroutineScope.launch {
                context.dataStore.edit { p -> p[LAST_SEEN_VERSION_KEY] = currentVersion }
            }
        }
    }
    // ──────────────────────────────────────────────────────────────────────

    val bgGradient = if (isDark) {
        Brush.linearGradient(
            0.0f to Color.Black,
            0.7f to Color.Black,
            1.0f to Color(0xFF1b1b1b),
            start = Offset(0f, 0f),
            end   = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
        )
    } else {
        Brush.linearGradient(
            colors = listOf(Color(0xFFe8ebed), Color(0xFFf2f0e3)),
            start  = Offset(0f, 0f),
            end    = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
        )
    }

    val navBarBg  = Color(0xFF6d9567).copy(alpha = 0.5f)
    val fabShape  = RoundedCornerShape(29.dp)

    val appContext    = context.applicationContext as Application
    val navController = rememberNavController()
    val viewModel: PeriodViewModel = viewModel(factory = PeriodViewModel.Factory(appContext))
    val screens = listOf(Screen.Calendar, Screen.Pill, Screen.Overview)

    var showAddCycleDialog by remember { mutableStateOf(false) }
    var showAddPillDialog  by remember { mutableStateOf(false) }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val hazeState = remember { HazeState() }

    Box(modifier = Modifier.fillMaxSize().background(bgGradient)) {
        NavHost(
            navController    = navController,
            startDestination = Screen.Calendar.route,
            modifier         = Modifier
                .fillMaxSize()
                .haze(state = hazeState)
                .windowInsetsPadding(WindowInsets.statusBars)
        ) {
            composable(Screen.Calendar.route) { CalendarScreen() }
            composable(Screen.Pill.route)     { PillTrackerScreen(viewModel) }
            composable(Screen.Overview.route) { OverviewScreen(viewModel) }

            composable(
                route             = Screen.Settings.route,
                enterTransition   = {
                    slideIntoContainer(
                        towards       = AnimatedContentTransitionScope.SlideDirection.Left,
                        animationSpec = tween(400)
                    )
                },
                popExitTransition = {
                    slideOutOfContainer(
                        towards       = AnimatedContentTransitionScope.SlideDirection.Right,
                        animationSpec = tween(400)
                    )
                }
            ) {
                SettingsScreen(onBack = { navController.popBackStack() }, viewModel = viewModel)
            }
        }

        val showBottomUi = currentRoute != Screen.Settings.route

        AnimatedVisibility(
            visible  = showBottomUi,
            enter    = slideInVertically { it } + fadeIn(),
            exit     = slideOutVertically { it } + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(bottom = 24.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(18.dp),
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    // FAB
                    Box(
                        modifier = Modifier
                            .width(60.dp)
                            .height(58.dp)
                            .shadow(
                                elevation    = 16.dp,
                                shape        = fabShape,
                                ambientColor = Color.Black.copy(alpha = 0.6f),
                                spotColor    = Color.Black.copy(alpha = 0.6f)
                            )
                            .hazeChild(state = hazeState, shape = fabShape, style = HazeStyle(blurRadius = 14.dp))
                            .clip(fabShape)
                            .background(navBarBg)
                            .clickable {
                                when (currentRoute) {
                                    Screen.Pill.route     -> showAddPillDialog = true
                                    Screen.Overview.route -> navController.navigate(Screen.Settings.route)
                                    else                  -> showAddCycleDialog = true
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        AnimatedContent(targetState = currentRoute, label = "FABIconTransition") { route ->
                            when (route) {
                                Screen.Pill.route     -> CapsuleIcon(color = Color.White)
                                Screen.Overview.route -> Icon(
                                    imageVector        = Icons.Rounded.Settings,
                                    contentDescription = "Settings",
                                    tint               = Color.White,
                                    modifier           = Modifier.size(24.dp)
                                )
                                else -> ClickyAddIcon(tint = Color.White)
                            }
                        }
                    }

                    // Navbar
                    SmoothBottomNavigation(
                        screens      = screens,
                        currentRoute = currentRoute,
                        hazeState    = hazeState,
                        onNavigate   = { route ->
                            navController.navigate(route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState    = true
                            }
                        }
                    )
                }
            }
        }

        // ── Dialogs ───────────────────────────────────────────────────────
        if (showAddPillDialog) {
            PillTrackingSetupDialog(
                onDismiss = { showAddPillDialog = false },
                onSave    = { startDate, pillCount ->
                    viewModel.enablePillTracking(startDate, pillCount)
                    showAddPillDialog = false
                }
            )
        }

        if (showAddCycleDialog) {
            AddCycleDialog(
                onDismiss = { showAddCycleDialog = false },
                onSave    = { start, end, bleed, color, pain ->
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

// SMALL ICON COMPOSABLES

@Composable
fun CapsuleIcon(color: Color) {
    Box(
        modifier         = Modifier.size(24.dp).rotate(-45f),
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

@Composable
fun ClickyAddIcon(tint: Color) {
    Box(modifier = Modifier.size(24.dp), contentAlignment = Alignment.Center) {
        Icon(imageVector = Icons.Rounded.Add, contentDescription = "Add", tint = tint)
    }
}