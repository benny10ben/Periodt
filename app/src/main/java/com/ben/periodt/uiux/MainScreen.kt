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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.rounded.*
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
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.ben.periodt.ui.theme.BricolageGrotesque
import com.ben.periodt.ui.theme.LocalAppIsDark
import com.ben.periodt.uiux.calendar.AddCycleDialog
import com.ben.periodt.uiux.calendar.CalendarScreen
import com.ben.periodt.uiux.overview.OverviewScreen
import com.ben.periodt.uiux.overview.RemindersDialog
import com.ben.periodt.uiux.overview.SettingsScreen
import com.ben.periodt.uiux.overview.THEME_MODE_KEY
import com.ben.periodt.uiux.overview.ThemeMode
import com.ben.periodt.uiux.overview.WhatsNewDialog
import com.ben.periodt.uiux.pill.PillTrackerScreen
import com.ben.periodt.uiux.pill.PillTrackingSetupDialog
import com.ben.periodt.uiux.profiles.*
import com.ben.periodt.uiux.shared.dataStore
import com.ben.periodt.viewmodel.PeriodViewModel
import com.ben.periodt.widget.CalendarWidget
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.haze
import dev.chrisbanes.haze.hazeChild
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ── Version key ──
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
    activeProfile: PeriodViewModel.Profile?,
    onProfileClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val navBarBg          = Color(0xFF6d9567).copy(alpha = 0.5f)
    val selectedContent   = Color.White
    val unselectedContent = Color.White.copy(alpha = 0.4f)

    // Standardized "Sweet Spot" Dimensions
    val slotWidth = 60.dp
    val barHeight = 56.dp
    val navShape  = RoundedCornerShape(28.dp)

    Box(
        modifier = modifier
            .wrapContentWidth()
            .height(barHeight)
            .shadow(
                elevation    = 14.dp,
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

            // Profile Avatar Tab
            Box(
                modifier = Modifier
                    .width(slotWidth)
                    .fillMaxHeight()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication        = null,
                        onClick           = onProfileClick
                    ),
                contentAlignment = Alignment.Center
            ) {
                AvatarDisplay(
                    avatarString = activeProfile?.avatarColor ?: "avatar_1",
                    name         = activeProfile?.name ?: "Me",
                    modifier     = Modifier.size(30.dp),
                    fontSize     = 14.sp
                )
            }
        }
    }
}

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
        CalendarWidget.refreshAll(context)
    }

    CompositionLocalProvider(LocalAppIsDark provides isDark) {
        MainScreenContent(isDark = isDark)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@RequiresApi(Build.VERSION_CODES.P)
@Composable
private fun MainScreenContent(isDark: Boolean) {
    SetSystemBars(statusBarColor = Color.Transparent, darkIcons = !isDark)

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

    val bgGradient = if (isDark) {
        Brush.linearGradient(
            0.0f to Color.Black, 0.7f to Color.Black, 1.0f to Color.Black,
            start = Offset(0f, 0f), end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
        )
    } else {
        Brush.linearGradient(
            colors = listOf(Color(0xFFe8ebed), Color(0xFFf2f0e3)),
            start  = Offset(0f, 0f), end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
        )
    }

    val fadeColor = if (isDark) Color.Black else Color.White
    val bottomFadeBrush = Brush.verticalGradient(
        colors = listOf(
            Color.Transparent,
            fadeColor.copy(alpha = 0f),
            fadeColor.copy(alpha = 0.6f),
            fadeColor
        )
    )

    val navBarBg  = Color(0xFF6d9567).copy(alpha = 0.5f)
    val compactShape  = RoundedCornerShape(26.dp)

    val appContext    = context.applicationContext as Application
    val navController = rememberNavController()
    val viewModel: PeriodViewModel = viewModel(factory = PeriodViewModel.Factory(appContext))
    val screens = listOf(Screen.Calendar, Screen.Pill, Screen.Overview)

    val activeProfile by viewModel.activeProfile.collectAsState()
    val allProfiles by viewModel.profiles.collectAsState()
    val pendingLegacyImport by viewModel.pendingLegacyImport.collectAsState()

    // UI States
    var showAddCycleDialog by remember { mutableStateOf(false) }
    var showAddPillDialog  by remember { mutableStateOf(false) }
    var showProfileSheet   by remember { mutableStateOf(false) }
    var profileToEdit      by remember { mutableStateOf<PeriodViewModel.Profile?>(null) }
    var showCreateProfile  by remember { mutableStateOf(false) }
    var showRemindersSheet by remember { mutableStateOf(false) }

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
            composable(Screen.Calendar.route) { CalendarScreen(viewModel) }
            composable(Screen.Pill.route)     { PillTrackerScreen(viewModel) }
            composable(Screen.Overview.route) { OverviewScreen(viewModel) }
            composable(
                route = Screen.Settings.route,
                enterTransition = { slideIntoContainer(towards = AnimatedContentTransitionScope.SlideDirection.Left, animationSpec = tween(400)) },
                popExitTransition = { slideOutOfContainer(towards = AnimatedContentTransitionScope.SlideDirection.Right, animationSpec = tween(400)) }
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
                    .background(bottomFadeBrush)
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(top = 48.dp, bottom = 24.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    // Balanced FAB
                    val balancedShape = RoundedCornerShape(28.dp)
                    Box(
                        modifier = Modifier
                            .width(58.dp)
                            .height(56.dp)
                            .shadow(
                                elevation    = 14.dp,
                                shape        = balancedShape,
                                ambientColor = Color.Black.copy(alpha = 0.6f),
                                spotColor    = Color.Black.copy(alpha = 0.6f)
                            )
                            .hazeChild(state = hazeState, shape = balancedShape, style = HazeStyle(blurRadius = 14.dp))
                            .clip(balancedShape)
                            .background(navBarBg)
                            .clickable {
                                when (currentRoute) {
                                    Screen.Pill.route     -> showAddPillDialog = true
                                    Screen.Overview.route -> showRemindersSheet = true
                                    else                  -> showAddCycleDialog = true
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        AnimatedContent(targetState = currentRoute, label = "FABIconTransition") { route ->
                            when (route) {
                                Screen.Pill.route     -> CapsuleIcon(color = Color.White)
                                Screen.Overview.route -> Icon(
                                    imageVector        = Icons.Rounded.Notifications,
                                    contentDescription = "Reminders",
                                    tint               = Color.White,
                                    modifier           = Modifier.size(24.dp)
                                )
                                else -> ClickyAddIcon(tint = Color.White)
                            }
                        }
                    }

                    // Compact Navbar
                    SmoothBottomNavigation(
                        screens        = screens,
                        currentRoute   = currentRoute,
                        hazeState      = hazeState,
                        activeProfile  = activeProfile,
                        onProfileClick = { showProfileSheet = true },
                        onNavigate     = { route ->
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

        // ── Sheets & Dialogs ──

        if (showProfileSheet) {
            val profileListState = rememberLazyListState()

            val profileSheetState = rememberModalBottomSheetState(
                skipPartiallyExpanded = true,
                confirmValueChange = { newValue ->
                    if (newValue == SheetValue.Hidden) {
                        profileListState.firstVisibleItemIndex == 0 &&
                                profileListState.firstVisibleItemScrollOffset == 0
                    } else true
                }
            )

            ModalBottomSheet(
                onDismissRequest = { showProfileSheet = false },
                sheetState       = profileSheetState,
                containerColor   = if (isDark) Color(0xFF1b1b1b) else Color.White,
                modifier         = Modifier.windowInsetsPadding(WindowInsets.statusBars)
            ) {
                ProfileBottomSheetContent(
                    allProfiles     = allProfiles,
                    activeProfile   = activeProfile,
                    isDark          = isDark,
                    onSwitch        = { viewModel.switchProfile(it); showProfileSheet = false },
                    onEdit          = { profileToEdit = it },
                    onDelete        = { viewModel.deleteProfile(it) },
                    onAddClick      = { showCreateProfile = true },
                    onSettingsClick = {
                        showProfileSheet = false
                        navController.navigate(Screen.Settings.route)
                    },
                    listState       = profileListState
                )
            }
        }

        if (showCreateProfile || profileToEdit != null) {
            ProfileEditorDialog(
                existingProfile = profileToEdit,
                isDark          = isDark,
                onDismiss       = { showCreateProfile = false; profileToEdit = null },
                onSave          = { name, avatarString ->
                    if (profileToEdit == null) {
                        viewModel.createProfile(name, avatarString) {}
                    } else {
                        // Prevent the race condition by only updating what actually changed
                        if (profileToEdit!!.name != name) {
                            viewModel.updateProfileName(profileToEdit!!.id, name)
                        }
                        if (profileToEdit!!.avatarColor != avatarString) {
                            viewModel.updateProfileColor(profileToEdit!!.id, avatarString)
                        }
                    }
                    showCreateProfile = false; profileToEdit = null
                }
            )
        }

        if (pendingLegacyImport != null) {
            LegacyImportDialog(
                profiles = allProfiles,
                onImportToProfile = { targetProfileId ->
                    viewModel.completeLegacyImport(targetProfileId = targetProfileId) { _, _ -> }
                },
                onDismiss = { viewModel.dismissLegacyImport() }
            )
        }

        if (showRemindersSheet) {
            RemindersDialog(viewModel = viewModel, onDismiss = { showRemindersSheet = false })
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
            AddCycleDialog(onDismiss = { showAddCycleDialog = false }, onSave = { start, end, bleeding, color, pain, overrides ->
                viewModel.addCycleWithDailyLogs(start, end, bleeding, color, pain, overrides)
                showAddCycleDialog = false
            })
        }

        if (showWhatsNew) WhatsNewDialog(onDismiss = { showWhatsNew = false })
    }
}

// SMALL ICON COMPOSABLES

@Composable
fun CapsuleIcon(color: Color) {
    Box(modifier = Modifier.size(22.dp).rotate(-45f), contentAlignment = Alignment.Center) {
        Column(modifier = Modifier.width(9.dp).height(18.dp).clip(RoundedCornerShape(100.dp)).border(1.2.dp, color.copy(alpha = 0.8f), RoundedCornerShape(100.dp))) {
            Box(modifier = Modifier.fillMaxWidth().weight(1f).background(color))
            Box(modifier = Modifier.fillMaxWidth().weight(1f).background(color.copy(alpha = 0.2f)))
        }
    }
}

@Composable
fun ClickyAddIcon(tint: Color) {
    Box(modifier = Modifier.size(22.dp), contentAlignment = Alignment.Center) {
        Icon(imageVector = Icons.Rounded.Add, contentDescription = "Add", tint = tint)
    }
}