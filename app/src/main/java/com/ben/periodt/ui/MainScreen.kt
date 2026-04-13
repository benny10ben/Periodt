package com.ben.periodt.ui

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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
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
import com.ben.periodt.ui.theme.LocalAppIsDark
import com.ben.periodt.ui.theme.SetSystemBars
import com.ben.periodt.ui.calendar.AddCycleDialog
import com.ben.periodt.ui.calendar.CalendarScreen
import com.ben.periodt.ui.overview.OverviewScreen
import com.ben.periodt.reminder.RemindersDialog
import com.ben.periodt.ui.overview.SettingsScreen
import com.ben.periodt.ui.overview.THEME_MODE_KEY
import com.ben.periodt.ui.overview.ThemeMode
import com.ben.periodt.ui.overview.WhatsNewDialog
import com.ben.periodt.ui.pill.PillTrackerScreen
import com.ben.periodt.ui.pill.PillTrackingSetupDialog
import com.ben.periodt.reminder.dataStore
import com.ben.periodt.ui.profiles.AvatarDisplay
import com.ben.periodt.ui.profiles.LegacyImportDialog
import com.ben.periodt.ui.profiles.ProfileBottomSheetContent
import com.ben.periodt.ui.profiles.ProfileEditorDialog
import com.ben.periodt.viewmodel.PeriodViewModel
import com.ben.periodt.widget.CalendarWidget
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.haze
import dev.chrisbanes.haze.hazeChild
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ── Dimensions System ──

data class AppDimensions(
    val barHeight: Dp,
    val iconSize: Dp,
    val avatarSize: Dp
)

val StandardDimensions = AppDimensions(
    barHeight = 64.dp,
    iconSize = 26.dp,
    avatarSize = 34.dp
)

val CompactDimensions = AppDimensions(
    barHeight = 58.dp,
    iconSize = 24.dp,
    avatarSize = 30.dp
)

val LocalAppDimens = staticCompositionLocalOf { StandardDimensions }

// ── Navigation & Versioning ──

val LAST_SEEN_VERSION_KEY = intPreferencesKey("last_seen_version")

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
    val dimens = LocalAppDimens.current
    val navBarBg = Color(0xFF6d9567).copy(alpha = 0.5f)

    Box(
        modifier = modifier
            .wrapContentWidth()
            .height(dimens.barHeight)
            .shadow(14.dp, CircleShape, ambientColor = Color.Black.copy(alpha = 0.6f), spotColor = Color.Black.copy(alpha = 0.6f))
            .hazeChild(state = hazeState, shape = CircleShape, style = HazeStyle(blurRadius = 14.dp))
            .clip(CircleShape)
            .background(navBarBg)
    ) {
        Row(modifier = Modifier.fillMaxHeight(), verticalAlignment = Alignment.CenterVertically) {
            screens.forEach { screen ->
                val isSelected = screen.route == currentRoute
                val contentColor by animateColorAsState(
                    targetValue = if (isSelected) Color.White else Color.White.copy(alpha = 0.4f),
                    animationSpec = tween(400), label = "contentColor"
                )

                Box(
                    modifier = Modifier.width(dimens.barHeight).fillMaxHeight().clickable( // ✨ Width matches Height
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null, onClick = { onNavigate(screen.route) }
                    ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = screen.icon, contentDescription = screen.label, tint = contentColor, modifier = Modifier.size(dimens.iconSize))
                }
            }

            Box(
                modifier = Modifier.width(dimens.barHeight).fillMaxHeight().clickable( // ✨ Width matches Height
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null, onClick = onProfileClick
                ),
                contentAlignment = Alignment.Center
            ) {
                AvatarDisplay(
                    avatarString = activeProfile?.avatarColor ?: "avatar_1",
                    name = activeProfile?.name ?: "Me",
                    modifier = Modifier.size(dimens.avatarSize),
                    fontSize = if (dimens == CompactDimensions) 14.sp else 15.sp
                )
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.P)
@Composable
fun MainScreen() {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val prefs by context.dataStore.data.collectAsState(initial = null)
    val savedThemeMode = prefs?.get(THEME_MODE_KEY) ?: ThemeMode.SYSTEM.name

    val isDark = when (ThemeMode.valueOf(savedThemeMode)) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    val dimensions = if (configuration.screenHeightDp < 800) CompactDimensions else StandardDimensions

    LaunchedEffect(isDark) {
        context.getSharedPreferences("widget_prefs", Context.MODE_PRIVATE).edit().putBoolean("is_dark", isDark).apply()
        CalendarWidget.refreshAll(context)
    }

    CompositionLocalProvider(
        LocalAppIsDark provides isDark,
        LocalAppDimens provides dimensions
    ) {
        MainScreenContent(isDark = isDark)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@RequiresApi(Build.VERSION_CODES.P)
@Composable
private fun MainScreenContent(isDark: Boolean) {
    SetSystemBars(statusBarColor = Color.Transparent, darkIcons = !isDark)

    val dimens = LocalAppDimens.current
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val prefs by context.dataStore.data.collectAsState(initial = null)
    var showWhatsNew by remember { mutableStateOf(false) }

    val currentVersion = remember { context.packageManager.getPackageInfo(context.packageName, 0).longVersionCode.toInt() }

    LaunchedEffect(prefs) {
        prefs ?: return@LaunchedEffect
        val lastSeen = prefs?.get(LAST_SEEN_VERSION_KEY) ?: 0
        if (lastSeen < currentVersion) {
            delay(3000L)
            showWhatsNew = true
            coroutineScope.launch { context.dataStore.edit { p -> p[LAST_SEEN_VERSION_KEY] = currentVersion } }
        }
    }

    val fadeColor = if (isDark) Color.Black else Color.White
    val bottomFadeBrush = Brush.verticalGradient(listOf(Color.Transparent, fadeColor.copy(alpha = 0f), fadeColor.copy(alpha = 0.6f), fadeColor))

    val navController = rememberNavController()
    val viewModel: PeriodViewModel = viewModel(factory = PeriodViewModel.Factory(context.applicationContext as Application))
    val screens = listOf(Screen.Calendar, Screen.Pill, Screen.Overview)

    val activeProfile by viewModel.activeProfile.collectAsState()
    val allProfiles by viewModel.profiles.collectAsState()
    val pendingLegacyImport by viewModel.pendingLegacyImport.collectAsState()

    var showAddCycleDialog by remember { mutableStateOf(false) }
    var showAddPillDialog by remember { mutableStateOf(false) }
    var showProfileSheet by remember { mutableStateOf(false) }
    var profileToEdit by remember { mutableStateOf<PeriodViewModel.Profile?>(null) }
    var showCreateProfile by remember { mutableStateOf(false) }
    var showRemindersSheet by remember { mutableStateOf(false) }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val hazeState = remember { HazeState() }

    Box(modifier = Modifier.fillMaxSize().background(if (isDark) Color.Black else Color(0xFFF2F0E3))) {
        NavHost(
            navController = navController,
            startDestination = Screen.Calendar.route,
            modifier = Modifier.fillMaxSize().haze(state = hazeState).windowInsetsPadding(WindowInsets.statusBars)
        ) {
            composable(Screen.Calendar.route) { CalendarScreen(viewModel) }
            composable(Screen.Pill.route) { PillTrackerScreen(viewModel) }
            composable(Screen.Overview.route) { OverviewScreen(viewModel) }
            composable(
                route = Screen.Settings.route,
                enterTransition = { slideIntoContainer(towards = AnimatedContentTransitionScope.SlideDirection.Left, animationSpec = tween(400)) },
                popExitTransition = { slideOutOfContainer(towards = AnimatedContentTransitionScope.SlideDirection.Right, animationSpec = tween(400)) }
            ) { SettingsScreen(onBack = { navController.popBackStack() }, viewModel = viewModel) }
        }

        AnimatedVisibility(
            visible = currentRoute != Screen.Settings.route,
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Box(
                modifier = Modifier.fillMaxWidth().background(bottomFadeBrush).windowInsetsPadding(WindowInsets.navigationBars).padding(top = 48.dp, bottom = 24.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    // ✨ FAB now uses size(dimens.barHeight) and CircleShape for perfect circularity
                    Box(
                        modifier = Modifier
                            .size(dimens.barHeight)
                            .shadow(14.dp, CircleShape, ambientColor = Color.Black.copy(alpha = 0.6f), spotColor = Color.Black.copy(alpha = 0.6f))
                            .hazeChild(state = hazeState, shape = CircleShape, style = HazeStyle(blurRadius = 14.dp))
                            .clip(CircleShape)
                            .background(Color(0xFF6d9567).copy(alpha = 0.5f))
                            .clickable {
                                when (currentRoute) {
                                    Screen.Pill.route -> showAddPillDialog = true
                                    Screen.Overview.route -> showRemindersSheet = true
                                    else -> showAddCycleDialog = true
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        AnimatedContent(targetState = currentRoute, label = "FABIconTransition") { route ->
                            when (route) {
                                Screen.Pill.route -> CapsuleIcon(color = Color.White)
                                Screen.Overview.route -> Icon(imageVector = Icons.Rounded.Notifications, contentDescription = "Reminders", tint = Color.White, modifier = Modifier.size(dimens.iconSize))
                                else -> ClickyAddIcon(tint = Color.White)
                            }
                        }
                    }

                    SmoothBottomNavigation(
                        screens = screens, currentRoute = currentRoute, hazeState = hazeState,
                        activeProfile = activeProfile, onProfileClick = { showProfileSheet = true },
                        onNavigate = { route ->
                            navController.navigate(route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true; restoreState = true
                            }
                        }
                    )
                }
            }
        }
        // ... Sheets & Dialogs Logic remains identical ...
        if (showProfileSheet) {
            val profileListState = rememberLazyListState()
            val profileSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            ModalBottomSheet(
                onDismissRequest = { showProfileSheet = false }, sheetState = profileSheetState,
                containerColor = if (isDark) Color(0xFF1b1b1b) else Color.White,
                modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars)
            ) {
                ProfileBottomSheetContent(
                    allProfiles = allProfiles, activeProfile = activeProfile, isDark = isDark,
                    onSwitch = { viewModel.switchProfile(it); showProfileSheet = false },
                    onEdit = { profileToEdit = it }, onDelete = { viewModel.deleteProfile(it) },
                    onAddClick = { showCreateProfile = true }, onSettingsClick = { showProfileSheet = false; navController.navigate(Screen.Settings.route) },
                    listState = profileListState
                )
            }
        }

        if (showCreateProfile || profileToEdit != null) {
            ProfileEditorDialog(
                existingProfile = profileToEdit, isDark = isDark,
                onDismiss = { showCreateProfile = false; profileToEdit = null },
                onSave = { name, avatarString ->
                    if (profileToEdit == null) viewModel.createProfile(name, avatarString) {}
                    else {
                        if (profileToEdit!!.name != name) viewModel.updateProfileName(profileToEdit!!.id, name)
                        if (profileToEdit!!.avatarColor != avatarString) viewModel.updateProfileColor(profileToEdit!!.id, avatarString)
                    }
                    showCreateProfile = false; profileToEdit = null
                }
            )
        }

        if (pendingLegacyImport != null) {
            LegacyImportDialog(allProfiles, { viewModel.completeLegacyImport(it) { _, _ -> } }, { viewModel.dismissLegacyImport() })
        }

        if (showRemindersSheet) RemindersDialog(viewModel, { showRemindersSheet = false })
        if (showAddPillDialog) PillTrackingSetupDialog({ showAddPillDialog = false }, { start, count -> viewModel.enablePillTracking(start, count); showAddPillDialog = false })
        if (showAddCycleDialog) AddCycleDialog({ showAddCycleDialog = false }, { s, e, b, c, p, o -> viewModel.addCycleWithDailyLogs(s, e, b, c, p, o); showAddCycleDialog = false })
        if (showWhatsNew) WhatsNewDialog { showWhatsNew = false }
    }
}

@Composable
fun CapsuleIcon(color: Color) {
    val dimens = LocalAppDimens.current
    val scale = if (dimens == CompactDimensions) 0.9f else 1f
    Box(modifier = Modifier.size(dimens.iconSize).rotate(-45f), contentAlignment = Alignment.Center) {
        Column(modifier = Modifier.width(9.dp * scale).height(18.dp * scale).clip(RoundedCornerShape(100.dp)).border(1.2.dp, color.copy(alpha = 0.8f), RoundedCornerShape(100.dp))) {
            Box(modifier = Modifier.fillMaxWidth().weight(1f).background(color))
            Box(modifier = Modifier.fillMaxWidth().weight(1f).background(color.copy(alpha = 0.2f)))
        }
    }
}

@Composable
fun ClickyAddIcon(tint: Color) {
    val dimens = LocalAppDimens.current
    val extraSize = if (dimens == CompactDimensions) 2.dp else 4.dp
    Box(modifier = Modifier.size(dimens.iconSize), contentAlignment = Alignment.Center) {
        Icon(imageVector = Icons.Rounded.Add, contentDescription = "Add", tint = tint, modifier = Modifier.size(dimens.iconSize + extraSize))
    }
}