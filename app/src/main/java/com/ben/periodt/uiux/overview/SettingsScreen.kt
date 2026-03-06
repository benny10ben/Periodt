package com.ben.periodt.uiux.overview

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Help
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.ben.periodt.ui.theme.BricolageGrotesque
import com.ben.periodt.ui.theme.LocalAppIsDark
import com.ben.periodt.uiux.shared.ReminderPrefs
import com.ben.periodt.uiux.shared.ReminderScheduler
import com.ben.periodt.uiux.shared.dataStore
import com.ben.periodt.viewmodel.PeriodViewModel
import kotlinx.coroutines.launch
import nl.dionsegijn.konfetti.compose.KonfettiView
import nl.dionsegijn.konfetti.core.Party
import nl.dionsegijn.konfetti.core.PartySystem
import nl.dionsegijn.konfetti.core.Position
import nl.dionsegijn.konfetti.core.emitter.Emitter
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

const val GITHUB_REPO_URL = "https://github.com/benny10ben/Periodt/"

enum class ThemeMode { SYSTEM, LIGHT, DARK }
val THEME_MODE_KEY = stringPreferencesKey("theme_mode")
val LAST_SEEN_VERSION_KEY = androidx.datastore.preferences.core.intPreferencesKey("last_seen_version")

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: PeriodViewModel
) {
    val context        = LocalContext.current
    val uriHandler     = LocalUriHandler.current
    val scrollState    = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()

    // Feedback states
    var showExportSuccess  by remember { mutableStateOf(false) }
    var showImportSuccess  by remember { mutableStateOf(false) }
    var showClearConfirm   by remember { mutableStateOf(false) }
    var showClearSuccess   by remember { mutableStateOf(false) }
    var importResultMessage by remember { mutableStateOf("") }
    var showConfetti       by remember { mutableStateOf(false) }

    // Info dialog states
    var showFaq        by remember { mutableStateOf(false) }
    var showPrivacy    by remember { mutableStateOf(false) }
    var showAbout      by remember { mutableStateOf(false) }
    var showAlgorithm  by remember { mutableStateOf(false) }
    var showWhatsNew   by remember { mutableStateOf(false) }
    var showWidgetInfo by remember { mutableStateOf(false) }
    var showAppearance by remember { mutableStateOf(false) }

    // DataStore / reminder state
    val prefs by context.dataStore.data.collectAsState(initial = null)

    var periodEnabled    by remember(prefs) { mutableStateOf(prefs?.get(ReminderPrefs.IS_ENABLED) ?: false) }
    var periodDays       by remember(prefs) { mutableStateOf(prefs?.get(ReminderPrefs.DAYS_BEFORE) ?: 2) }
    var periodTime       by remember(prefs) { mutableStateOf(LocalTime.of(prefs?.get(ReminderPrefs.TIME_HOUR) ?: 8, prefs?.get(ReminderPrefs.TIME_MINUTE) ?: 0)) }

    var fertilityEnabled by remember(prefs) { mutableStateOf(prefs?.get(ReminderPrefs.FERTILITY_ENABLED) ?: false) }
    var fertilityDays    by remember(prefs) { mutableStateOf(prefs?.get(ReminderPrefs.FERTILITY_DAYS_BEFORE) ?: 2) }
    var fertilityTime    by remember(prefs) { mutableStateOf(LocalTime.of(prefs?.get(ReminderPrefs.FERTILITY_HOUR) ?: 8, prefs?.get(ReminderPrefs.FERTILITY_MINUTE) ?: 0)) }

    var pillEnabled      by remember(prefs) { mutableStateOf(prefs?.get(ReminderPrefs.PILL_ENABLED) ?: false) }
    var pillTime         by remember(prefs) { mutableStateOf(LocalTime.of(prefs?.get(ReminderPrefs.PILL_HOUR) ?: 8, prefs?.get(ReminderPrefs.PILL_MINUTE) ?: 0)) }

    var periodExpanded    by remember { mutableStateOf(false) }
    var fertilityExpanded by remember { mutableStateOf(false) }
    var pillExpanded      by remember { mutableStateOf(false) }

    val themeModeString by context.dataStore.data.collectAsState(initial = null)
    var themeMode by remember(themeModeString) {
        mutableStateOf(
            ThemeMode.valueOf(themeModeString?.get(THEME_MODE_KEY) ?: ThemeMode.SYSTEM.name)
        )
    }

    val powerManager       = remember { context.getSystemService(Context.POWER_SERVICE) as android.os.PowerManager }
    val isBatteryOptimized = remember { !powerManager.isIgnoringBatteryOptimizations(context.packageName) }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}

    fun checkAndRequestNotifPermission(onGranted: () -> Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val has = ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!has) { permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS); return }
        }
        onGranted()
    }

    fun saveReminders() {
        coroutineScope.launch {
            context.dataStore.edit { p ->
                p[ReminderPrefs.IS_ENABLED]            = periodEnabled
                p[ReminderPrefs.DAYS_BEFORE]           = periodDays
                p[ReminderPrefs.TIME_HOUR]             = periodTime.hour
                p[ReminderPrefs.TIME_MINUTE]           = periodTime.minute
                p[ReminderPrefs.FERTILITY_ENABLED]     = fertilityEnabled
                p[ReminderPrefs.FERTILITY_DAYS_BEFORE] = fertilityDays
                p[ReminderPrefs.FERTILITY_HOUR]        = fertilityTime.hour
                p[ReminderPrefs.FERTILITY_MINUTE]      = fertilityTime.minute
                p[ReminderPrefs.PILL_ENABLED]          = pillEnabled
                p[ReminderPrefs.PILL_HOUR]             = pillTime.hour
                p[ReminderPrefs.PILL_MINUTE]           = pillTime.minute
            }
            if (periodEnabled)    ReminderScheduler.scheduleNextReminder(context, periodTime.hour, periodTime.minute)
            else                  ReminderScheduler.cancelReminder(context)
            if (fertilityEnabled) ReminderScheduler.scheduleNextFertilityReminder(context, fertilityTime.hour, fertilityTime.minute)
            else                  ReminderScheduler.cancelFertilityReminder(context)
            if (pillEnabled)      ReminderScheduler.scheduleNextPillReminder(context, pillTime.hour, pillTime.minute)
            else                  ReminderScheduler.cancelPillReminder(context)
        }
    }

    // Launchers
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let {
            viewModel.performExport(it) { success, msg ->
                if (success) showExportSuccess = true
                else Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            viewModel.performImport(it) { success, msg ->
                if (success) {
                    importResultMessage = msg ?: "Success"
                    showImportSuccess = true
                    showConfetti = true
                } else {
                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // Theme
    val isDark = LocalAppIsDark.current

    val bgGradient = if (isDark) {
        Brush.linearGradient(
            0.0f to Color.Black, 0.7f to Color.Black, 1.0f to Color(0xFF1B1B1B),
            start = Offset(0f, 0f), end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
        )
    } else {
        Brush.linearGradient(
            colors = listOf(Color(0xFFe8ebed), Color(0xFFf2f0e3)),
            start  = Offset(0f, 0f), end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
        )
    }

    val surfaceColor = if (isDark) Color(0xFF1B1B1B).copy(alpha = 0.5f) else Color.White
    val innerPillBg  = if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.05f)
    val textPrimary  = if (isDark) Color.White else Color(0xFF0F172A)
    val textSub      = if (isDark) Color.White.copy(alpha = 0.6f) else Color(0xFF64748B)
    val accentColor  = if (isDark) Color(0xFFD89046) else Color(0xFF6d9567).copy(alpha = 0.6f)
    val fertilityAccent = if (isDark) Color(0xFFD89046) else Color(0xFF6d9567).copy(alpha = 0.6f)
    val pillAccent = Color(0xFFa68e74)

    // Layout
    Box(modifier = Modifier.fillMaxSize().background(bgGradient)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(40.dp).clip(CircleShape).clickable(onClick = onBack),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back", tint = textPrimary, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(16.dp))
                Text(
                    "Settings", fontFamily = BricolageGrotesque,
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = textPrimary
                )
            }

            // Content
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(scrollState)
                    .padding(horizontal = 16.dp)
            ) {

                // Medical disclaimer
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isDark) Color(0xFFD89046).copy(alpha = 0.1f) else Color(0xFFD89046).copy(alpha = 0.08f))
                        .padding(14.dp),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        Icons.Rounded.HealthAndSafety,
                        contentDescription = null,
                        tint = Color(0xFFD89046),
                        modifier = Modifier.size(24.dp).padding(top = 1.dp)
                    )
                    Text(
                        text = "Predictions are for informational purposes only and are not medical advice. Please consult a healthcare professional for accurate results.",
                        fontFamily = BricolageGrotesque,
                        fontSize = 13.sp,
                        color = if (isDark) Color(0xFFD89046).copy(alpha = 0.9f) else Color(0xFFB8730A),
                        lineHeight = 18.sp
                    )
                }

                Spacer(Modifier.height(20.dp))

                // 0. TOP WARNING (BATTERY)
                if (isBatteryOptimized) {
                    BatteryWarning(
                        pillBg = surfaceColor, // Using surface color so it looks like its own card
                        accentColor = accentColor,
                        textPrimary = textPrimary,
                        textSub = textSub,
                        context = context
                    )
                    Spacer(Modifier.height(24.dp))
                }

                // 1. NOTIFICATIONS
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "NOTIFICATIONS", fontFamily = BricolageGrotesque,
                        fontSize = 13.sp, fontWeight = FontWeight.Bold,
                        color = if (isDark) Color.White.copy(alpha = 0.6f) else Color.Black.copy(alpha = 0.4f),
                        modifier = Modifier.padding(start = 12.dp, bottom = 8.dp)
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {

                        // Period
                        ReminderSection(
                            title = "Period Reminders",
                            subtitle = "Notify before your next period",
                            icon = Icons.Rounded.WaterDrop,
                            iconTint = accentColor,
                            isExpanded = periodExpanded,
                            isEnabled = periodEnabled,
                            pillBg = surfaceColor,
                            textPrimary = textPrimary,
                            textSub = textSub,
                            onHeaderClick = { periodExpanded = !periodExpanded },
                            onToggleChange = { checked ->
                                checkAndRequestNotifPermission { periodEnabled = checked; saveReminders() }
                            }
                        ) {
                            ReminderDaysSelector(
                                label = "Remind me before",
                                options = listOf(1, 2, 3, 4, 7),
                                selected = periodDays,
                                accentColor = accentColor,
                                pillBg = innerPillBg,
                                textPrimary = textPrimary,
                                onSelect = { periodDays = it; saveReminders() }
                            )
                            Spacer(Modifier.height(16.dp))
                            ReminderTimePicker(
                                time = periodTime, pillBg = innerPillBg,
                                textPrimary = textPrimary, textSub = textSub, context = context,
                                onTimePicked = { periodTime = it; saveReminders() }
                            )
                        }

                        // Fertility
                        ReminderSection(
                            title = "Fertility Reminders",
                            subtitle = "Notify before your fertile window",
                            icon = Icons.Rounded.Favorite,
                            iconTint = fertilityAccent,
                            isExpanded = fertilityExpanded,
                            isEnabled = fertilityEnabled,
                            pillBg = surfaceColor,
                            textPrimary = textPrimary,
                            textSub = textSub,
                            onHeaderClick = { fertilityExpanded = !fertilityExpanded },
                            onToggleChange = { checked ->
                                checkAndRequestNotifPermission { fertilityEnabled = checked; saveReminders() }
                            }
                        ) {
                            ReminderDaysSelector(
                                label = "Remind me before ovulation",
                                options = listOf(0, 1, 2, 3, 5),
                                selected = fertilityDays,
                                accentColor = fertilityAccent,
                                pillBg = innerPillBg,
                                textPrimary = textPrimary,
                                onSelect = { fertilityDays = it; saveReminders() },
                                labelOverride = { days ->
                                    when (days) { 0 -> "Day of"; 1 -> "1d"; else -> "${days}d" }
                                }
                            )
                            Spacer(Modifier.height(16.dp))
                            ReminderTimePicker(
                                time = fertilityTime, pillBg = innerPillBg,
                                textPrimary = textPrimary, textSub = textSub, context = context,
                                onTimePicked = { fertilityTime = it; saveReminders() }
                            )
                        }

                        // Pill
                        ReminderSection(
                            title = "Pill Reminders",
                            subtitle = "Daily reminder to take your pill",
                            icon = Icons.Rounded.Medication,
                            iconTint = pillAccent,
                            isExpanded = pillExpanded,
                            isEnabled = pillEnabled,
                            pillBg = surfaceColor,
                            textPrimary = textPrimary,
                            textSub = textSub,
                            onHeaderClick = { pillExpanded = !pillExpanded },
                            onToggleChange = { checked ->
                                checkAndRequestNotifPermission { pillEnabled = checked; saveReminders() }
                            }
                        ) {
                            Text(
                                "You'll be reminded daily at the time below. The notification will show which day of your pack you're on.",
                                fontFamily = BricolageGrotesque, color = textSub, fontSize = 13.sp
                            )
                            Spacer(Modifier.height(16.dp))
                            ReminderTimePicker(
                                time = pillTime, pillBg = innerPillBg,
                                textPrimary = textPrimary, textSub = textSub, context = context,
                                onTimePicked = { pillTime = it; saveReminders() }
                            )
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))

                //2. DATA & BACKUP
                SettingsSection(title = "DATA & BACKUP", surfaceColor) {
                    SettingsItem(
                        icon = Icons.Rounded.Upload,
                        title = "Export Data",
                        subtitle = "Save unique JSON backup",
                        tint = textPrimary,
                        onClick = {
                            val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
                            exportLauncher.launch("periodt_backup_$timestamp.json")
                        }
                    )
                    SettingsItem(
                        icon = Icons.Rounded.Download,
                        title = "Import Data",
                        subtitle = "Restore and deduplicate",
                        tint = textPrimary,
                        onClick = { importLauncher.launch(arrayOf("application/json")) }
                    )
                    SettingsItem(
                        icon = Icons.Rounded.DeleteForever,
                        title = "Clear All Data",
                        subtitle = "Permanently delete all your tracked data? This cannot be undone.",
                        tint = Color(0xFFEF5350),
                        showChevron = false,
                        onClick = { showClearConfirm = true }
                    )
                }

                Spacer(Modifier.height(24.dp))

                //3. CUSTOMIZE
                SettingsSection(title = "CUSTOMIZE", surfaceColor) {
                    SettingsItem(
                        icon = Icons.Rounded.Widgets,
                        title = "Home screen widgets",
                        subtitle = "How to add to your screen",
                        tint = textPrimary,
                        onClick = { showWidgetInfo = true }
                    )
                    SettingsItem(
                        icon = Icons.Rounded.Palette,
                        title = "Appearance",
                        subtitle = when (themeMode) {
                            ThemeMode.SYSTEM -> "System default"
                            ThemeMode.LIGHT  -> "Light mode"
                            ThemeMode.DARK   -> "Dark mode"
                        },
                        tint = textPrimary,
                        onClick = { showAppearance = true }
                    )
                }

                Spacer(Modifier.height(24.dp))

                // 4. HELP CENTER
                SettingsSection(title = "HELP CENTER", surfaceColor) {
                    SettingsItem(
                        icon = Icons.Rounded.Calculate,
                        title = "How we calculate",
                        tint = textPrimary,
                        onClick = { showAlgorithm = true }
                    )
                    SettingsItem(
                        icon = Icons.AutoMirrored.Rounded.Help,
                        title = "FAQ",
                        tint = textPrimary,
                        onClick = { showFaq = true }
                    )
                    SettingsItem(
                        icon = Icons.Rounded.BugReport,
                        title = "Request a new feature",
                        subtitle = "Report issues on GitHub",
                        tint = textPrimary,
                        onClick = {
                            try { uriHandler.openUri("https://github.com/benny10ben/Periodt/issues") }
                            catch (e: Exception) { Toast.makeText(context, "Could not open browser", Toast.LENGTH_SHORT).show() }
                        }
                    )
                    SettingsItem(
                        icon = Icons.Rounded.NewReleases,
                        title = "What's new",
                        tint = textPrimary,
                        onClick = { showWhatsNew = true }
                    )
                }

                Spacer(Modifier.height(24.dp))

                //5. MORE
                SettingsSection(title = "MORE", surfaceColor) {
                    SettingsItem(
                        icon = Icons.Rounded.Info,
                        title = "About Periodt",
                        subtitle = "v1.1.6",
                        tint = textPrimary,
                        onClick = { showAbout = true }
                    )
                    SettingsItem(
                        icon = Icons.Rounded.PrivacyTip,
                        title = "Privacy Policy",
                        tint = textPrimary,
                        onClick = { showPrivacy = true }
                    )
                }

                Spacer(Modifier.height(24.dp))

                // Dedication
                Text(
                    text = "Dedicated to my love ❤️",
                    fontFamily = BricolageGrotesque,
                    fontSize = 13.sp,
                    color = if (isDark) Color.White else Color.Black,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
                Spacer(Modifier.height(24.dp))
            }
        }
    }

    // Dialogs

    if (showAppearance) {
        AppearanceDialog(
            current = themeMode,
            onSelect = { selected ->
                themeMode = selected
                coroutineScope.launch {
                    context.dataStore.edit { p -> p[THEME_MODE_KEY] = selected.name }
                }
                showAppearance = false
            },
            onDismiss = { showAppearance = false }
        )
    }

    if (showWidgetInfo) {
        ContentDialog(title = "Home Screen Widgets", onDismiss = { showWidgetInfo = false }) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("To add the Periodt widget to your home screen:", fontFamily = BricolageGrotesque, color = textPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text("1. Long-press an empty space on your home screen.", fontFamily = BricolageGrotesque, color = textSub, fontSize = 14.sp)
                Text("2. Tap the 'Widgets' button in the menu.", fontFamily = BricolageGrotesque, color = textSub, fontSize = 14.sp)
                Text("3. Scroll down to find 'Periodt'.", fontFamily = BricolageGrotesque, color = textSub, fontSize = 14.sp)
                Text("4. Long-press the widget and drag it to your desired position.", fontFamily = BricolageGrotesque, color = textSub, fontSize = 14.sp)
            }
        }
    }

    if (showAlgorithm) {
        ContentDialog(title = "How we calculate", onDismiss = { showAlgorithm = false }) {
            Text(
                text = "Periodt uses a dynamic prediction algorithm that adapts to your unique cycle over time.\n\n" +
                        "1. Smart outlier filtering\n" +
                        "Unusually long gaps — likely missed logs — are detected and excluded so they don't skew your predictions.\n\n" +
                        "2. Trend-aware cycle length\n" +
                        "Rather than a simple average, we run a weighted linear regression across your last 6 cycles to detect if your cycle is shifting shorter or longer, then blend that trend with a recency-weighted average.\n\n" +
                        "3. Regularity scoring\n" +
                        "We calculate the standard deviation of your cycle lengths and classify your pattern as Very Regular, Regular, Somewhat Irregular, or Irregular — which directly widens or narrows your prediction window.\n\n" +
                        "4. Personalised ovulation & fertile window\n" +
                        "Ovulation is estimated from your actual logged luteal phase data where available, not a fixed 14-day assumption. The fertile window expands or contracts based on prediction confidence.\n\n" +
                        "5. Post-pill awareness\n" +
                        "After stopping the pill, predictions are paused during Discovery (first 1–2 cycles) and flagged as learning during cycles 3–4, reflecting that your natural rhythm is still re-establishing.\n\n" +
                        "The more you log, the smarter and more personalised your predictions become.",
                style = MaterialTheme.typography.bodyMedium,
                fontFamily = BricolageGrotesque,
                color = textSub, lineHeight = 22.sp
            )
        }
    }

    if (showFaq) {
        ContentDialog(title = "Frequently Asked Questions", onDismiss = { showFaq = false }) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                FaqItem("Is my data secure?", "Yes. Periodt is offline-first. Your data never leaves your device unless you manually export it.", textPrimary, textSub)
                FaqItem("How do I backup my data?", "Go to Data & Backup and tap 'Export Data'. This creates a secure JSON file you can save to Google Drive or local storage.", textPrimary, textSub)
                FaqItem("What if I forget to log?", "You can log past dates anytime. Just tap the '+' button on the main screen and select the past dates.", textPrimary, textSub)
                FaqItem("Can I sync across devices?", "Not currently. To move data to a new phone, Export on the old phone and Import on the new one.", textPrimary, textSub)
            }
        }
    }

    if (showPrivacy) {
        ContentDialog(title = "Privacy Policy", onDismiss = { showPrivacy = false }) {
            Text(
                text = "Your privacy is our priority. Periodt operates completely offline.\n\n" +
                        "• No data is sent to external servers.\n" +
                        "• All cycle history and notes are stored locally in an encrypted database on this device.\n" +
                        "• We do not track location or use analytics cookies.\n\n" +
                        "If you delete the app, your data is permanently deleted unless you have created a backup.",
                style = MaterialTheme.typography.bodyMedium,
                fontFamily = BricolageGrotesque,
                color = textSub, lineHeight = 22.sp
            )
        }
    }

    if (showAbout) {
        ContentDialog(title = "About Periodt", onDismiss = { showAbout = false }) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Periodt", style = MaterialTheme.typography.headlineMedium, fontFamily = BricolageGrotesque, fontWeight = FontWeight.Bold, color = textPrimary)
                Text("v1.1.6", style = MaterialTheme.typography.labelLarge, fontFamily = BricolageGrotesque, color = textSub)
                Spacer(Modifier.height(16.dp))
                Text("Designed to be simple, private, and aesthetic.", style = MaterialTheme.typography.bodyMedium, fontFamily = BricolageGrotesque, color = textSub, lineHeight = 22.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                Spacer(Modifier.height(24.dp))
                Button(
                    onClick = { try { uriHandler.openUri("https://github.com/benny10ben/Periodt") } catch (e: Exception) { } },
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                    shape = RoundedCornerShape(50)
                ) {
                    Icon(Icons.Rounded.Code, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("View on GitHub", color = Color.White, fontFamily = BricolageGrotesque, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    if (showWhatsNew) {
        WhatsNewDialog(onDismiss = { showWhatsNew = false })
    }

    if (showExportSuccess) SuccessFeedbackDialog("Backup Saved", "Your data has been successfully exported.", onDismiss = { showExportSuccess = false })
    if (showImportSuccess) SuccessFeedbackDialog("Import Complete", importResultMessage, onDismiss = { showImportSuccess = false })
    if (showClearConfirm)  DestructiveConfirmationDialog("Clear All Data?", "Permanently delete all cycle history?", onConfirm = { viewModel.clearAllData(); showClearConfirm = false; showClearSuccess = true }, onDismiss = { showClearConfirm = false })
    if (showClearSuccess)  SuccessFeedbackDialog("Fresh Start", "All data deleted.", "Done", onDismiss = { showClearSuccess = false })

    if (showConfetti) {
        KonfettiView(
            modifier = Modifier.fillMaxSize(),
            parties = rainConfetti(),
            updateListener = object : nl.dionsegijn.konfetti.compose.OnParticleSystemUpdateListener {
                override fun onParticleSystemEnded(system: PartySystem, activeSystems: Int) {
                    if (activeSystems == 0) showConfetti = false
                }
            }
        )
    }
}

// REMINDER SUBCOMPONENTS
@Composable
private fun ReminderSection(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconTint: Color,
    isExpanded: Boolean,
    isEnabled: Boolean,
    pillBg: Color,
    textPrimary: Color,
    textSub: Color,
    onHeaderClick: () -> Unit,
    onToggleChange: (Boolean) -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    val arrowRotation by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        label = "arrow_$title"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(pillBg)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onHeaderClick)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(icon, null, tint = iconTint, modifier = Modifier.size(24.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontFamily = BricolageGrotesque, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = textPrimary)
                Text(subtitle, fontFamily = BricolageGrotesque, fontSize = 13.sp, color = textSub)
            }
            Switch(
                checked = isEnabled,
                onCheckedChange = onToggleChange,
                modifier = Modifier.scale(0.85f),
                colors = SwitchDefaults.colors(
                    checkedTrackColor   = iconTint,
                    uncheckedTrackColor = textSub.copy(alpha = 0.2f),
                    checkedThumbColor   = Color.White
                )
            )
            Icon(
                Icons.Default.KeyboardArrowDown, null,
                tint = textSub,
                modifier = Modifier.size(20.dp).graphicsLayer { rotationZ = arrowRotation }
            )
        }

        AnimatedVisibility(visible = isExpanded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
            ) {
                HorizontalDivider(color = textSub.copy(alpha = 0.1f), modifier = Modifier.padding(bottom = 16.dp))
                content()
            }
        }
    }
}

@Composable
private fun ReminderDaysSelector(
    label: String,
    options: List<Int>,
    selected: Int,
    accentColor: Color,
    pillBg: Color,
    textPrimary: Color,
    onSelect: (Int) -> Unit,
    labelOverride: ((Int) -> String)? = null
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(label, fontFamily = BricolageGrotesque, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = textPrimary)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            options.forEach { days ->
                val isSelected = selected == days
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(CircleShape)
                        .background(if (isSelected) accentColor.copy(alpha = 0.15f) else pillBg)
                        .clickable { onSelect(days) }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text       = labelOverride?.invoke(days) ?: if (days == 7) "1w" else "${days}d",
                        fontFamily = BricolageGrotesque,
                        color      = if (isSelected) accentColor else textPrimary,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        fontSize   = 13.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun ReminderTimePicker(
    time: LocalTime,
    pillBg: Color,
    textPrimary: Color,
    textSub: Color,
    context: Context,
    onTimePicked: (LocalTime) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("At time", fontFamily = BricolageGrotesque, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = textPrimary)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(pillBg)
                .clickable {
                    android.app.TimePickerDialog(
                        context, { _, h, m -> onTimePicked(LocalTime.of(h, m)) },
                        time.hour, time.minute, false
                    ).show()
                }
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(time.format(DateTimeFormatter.ofPattern("hh:mm a")), fontFamily = BricolageGrotesque, color = textPrimary, fontSize = 15.sp)
                Icon(Icons.Rounded.AccessTime, null, tint = textSub)
            }
        }
    }
}

@Composable
private fun BatteryWarning(
    pillBg: Color, accentColor: Color, textPrimary: Color,
    textSub: Color, context: Context
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp)) // Updated to match section corner radius
            .background(pillBg)
            .clickable {
                context.startActivity(
                    Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = android.net.Uri.fromParts("package", context.packageName, null)
                    }
                )
            }
            .padding(16.dp), // Increased padding for a more "card" feel
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(Icons.Rounded.WarningAmber, null, tint = accentColor, modifier = Modifier.size(24.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text("Background Restricted", fontFamily = BricolageGrotesque, color = textPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text("Tap to allow unrestricted battery usage so reminders arrive on time.", fontFamily = BricolageGrotesque, color = textSub, fontSize = 13.sp)
        }
    }
}

// SHARED COMPONENTS
@Composable
fun ContentDialog(
    title: String,
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    val isDark = LocalAppIsDark.current
    val bgGradient      = if (isDark) Brush.linearGradient(0.0f to Color.Black, 1.0f to Color(0xFF1B1B1B))
    else Brush.linearGradient(colors = listOf(Color(0xFFF8FAFC), Color(0xFFf2f0e3)))
    val surfaceFallback = if (isDark) Color(0xFF1B1B1B) else Color.White
    val textPrimary     = if (isDark) Color.White else Color(0xFF1B1B1B)

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Card(
            modifier  = Modifier.fillMaxWidth(0.9f).padding(vertical = 24.dp),
            shape     = RoundedCornerShape(26.dp),
            colors    = CardDefaults.cardColors(containerColor = surfaceFallback),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Box(modifier = Modifier.fillMaxWidth().background(bgGradient)) {
                Column(modifier = Modifier.padding(24.dp).verticalScroll(rememberScrollState())) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        Text(title, modifier = Modifier.weight(1f), fontFamily = BricolageGrotesque,
                            style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = textPrimary)
                        IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Rounded.Close, "Close", tint = textPrimary)
                        }
                    }
                    content()
                }
            }
        }
    }
}

@Composable
fun FaqItem(question: String, answer: String, primary: Color, sub: Color) {
    Column {
        Text(question, fontFamily = BricolageGrotesque, fontWeight = FontWeight.Bold, color = primary, fontSize = 15.sp)
        Spacer(Modifier.height(4.dp))
        Text(answer, fontFamily = BricolageGrotesque, color = sub, fontSize = 14.sp, lineHeight = 20.sp)
    }
}

@Composable
fun SettingsSection(
    title: String,
    surfaceColor: Color,
    content: @Composable ColumnScope.() -> Unit
) {
    val isDark = LocalAppIsDark.current

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            title, fontFamily = BricolageGrotesque, fontSize = 13.sp, fontWeight = FontWeight.Bold,
            color = if (isDark) Color.White.copy(alpha = 0.6f) else Color.Black.copy(alpha = 0.4f),
            modifier = Modifier.padding(start = 12.dp, bottom = 8.dp)
        )
        Card(
            shape     = RoundedCornerShape(24.dp),
            colors    = CardDefaults.cardColors(containerColor = surfaceColor),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            modifier  = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                content()
            }
        }
    }
}

@Composable
fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    tint: Color,
    showChevron: Boolean = true,
    onClick: () -> Unit
) {
    val isDark = LocalAppIsDark.current
    val subTextColor = if (isDark) Color.White.copy(alpha = 0.5f) else Color.Black.copy(alpha = 0.5f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontFamily = BricolageGrotesque, fontSize = 16.sp, fontWeight = FontWeight.Medium,
                color = if (isDark) Color.White else Color(0xFF0F172A))
            if (subtitle != null) {
                Text(subtitle, fontFamily = BricolageGrotesque, fontSize = 13.sp, color = subTextColor)
            }
        }
        if (showChevron) {
            Icon(Icons.Rounded.ChevronRight, null, tint = subTextColor.copy(alpha = 0.4f), modifier = Modifier.size(20.dp))
        }
    }
}

fun rainConfetti(): List<Party> = listOf(
    Party(
        speed = 0f, maxSpeed = 30f, damping = 0.9f, spread = 360,
        colors = listOf(0xf2b179.toInt(), 0xFFD89046.toInt(), 0xf4306d.toInt(), 0xb48def.toInt()),
        position = Position.Relative(0.5, 0.3),
        emitter = Emitter(duration = 100, TimeUnit.MILLISECONDS).max(100)
    )
)

// APPEARANCE DIALOG
@Composable
fun AppearanceDialog(
    current: ThemeMode,
    onSelect: (ThemeMode) -> Unit,
    onDismiss: () -> Unit
) {
    val isDark = LocalAppIsDark.current

    val bgGradient = if (isDark) {
        Brush.linearGradient(0.0f to Color.Black, 1.0f to Color(0xFF1B1B1B))
    } else {
        Brush.linearGradient(colors = listOf(Color(0xFFF8FAFC), Color(0xFFf2f0e3)))
    }
    val surfaceFallback = if (isDark) Color(0xFF1B1B1B) else Color.White
    val textPrimary     = if (isDark) Color.White else Color(0xFF0F172A)
    val accentColor     = if (isDark) Color(0xFFD89046) else Color(0xFF6d9567).copy(alpha = 0.6f)
    val pillBg          = if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.05f)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier  = Modifier.fillMaxWidth(0.9f).padding(vertical = 24.dp),
            shape     = RoundedCornerShape(26.dp),
            colors    = CardDefaults.cardColors(containerColor = surfaceFallback),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Box(modifier = Modifier.fillMaxWidth().background(bgGradient)) {
                Column(modifier = Modifier.padding(24.dp)) {

                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Appearance",
                            fontFamily = BricolageGrotesque,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = textPrimary
                        )
                        IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Rounded.Close, "Close", tint = textPrimary)
                        }
                    }

                    // Options
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        ThemeOptionRow(
                            label = "System Default",
                            isSelected = current == ThemeMode.SYSTEM,
                            textPrimary = textPrimary,
                            pillBg = pillBg,
                            accentColor = accentColor,
                            onClick = { onSelect(ThemeMode.SYSTEM) }
                        )
                        ThemeOptionRow(
                            label = "Light Mode",
                            isSelected = current == ThemeMode.LIGHT,
                            textPrimary = textPrimary,
                            pillBg = pillBg,
                            accentColor = accentColor,
                            onClick = { onSelect(ThemeMode.LIGHT) }
                        )
                        ThemeOptionRow(
                            label = "Dark Mode",
                            isSelected = current == ThemeMode.DARK,
                            textPrimary = textPrimary,
                            pillBg = pillBg,
                            accentColor = accentColor,
                            onClick = { onSelect(ThemeMode.DARK) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ThemeOptionRow(
    label: String,
    isSelected: Boolean,
    textPrimary: Color,
    pillBg: Color,
    accentColor: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (isSelected) accentColor.copy(alpha = 0.15f) else pillBg)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontFamily = BricolageGrotesque,
            color = if (isSelected) accentColor else textPrimary,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            fontSize = 15.sp
        )
        if (isSelected) {
            Icon(
                imageVector = Icons.Rounded.CheckCircle,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(22.dp)
            )
        } else {
            Icon(
                imageVector = Icons.Rounded.RadioButtonUnchecked,
                contentDescription = null,
                tint = textPrimary.copy(alpha = 0.4f),
                modifier = Modifier.size(22.dp)
            )
        }
    }
}