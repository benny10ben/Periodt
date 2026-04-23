package com.ben.periodt.ui.settings

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.edit
import com.ben.periodt.reminder.dataStore
import com.ben.periodt.ui.profiles.components.LegacyImportDialog
import com.ben.periodt.ui.settings.components.AlgoRegularityTable
import com.ben.periodt.ui.settings.components.AlgoStep
import com.ben.periodt.ui.settings.components.AppearanceDialog
import com.ben.periodt.ui.settings.components.ContentDialog
import com.ben.periodt.ui.settings.components.DestructiveConfirmationDialog
import com.ben.periodt.ui.settings.components.FaqItem
import com.ben.periodt.ui.settings.components.SettingsItem
import com.ben.periodt.ui.settings.components.SettingsSection
import com.ben.periodt.ui.settings.components.SuccessFeedbackDialog
import com.ben.periodt.ui.settings.components.THEME_MODE_KEY
import com.ben.periodt.ui.settings.components.ThemeMode
import com.ben.periodt.ui.settings.components.WhatsNewDialog
import com.ben.periodt.ui.settings.components.rainConfetti
import com.ben.periodt.ui.theme.BricolageGrotesque
import com.ben.periodt.ui.theme.LocalAppIsDark
import com.ben.periodt.viewmodel.PeriodViewModel
import com.ben.periodt.widget.CalendarWidget
import kotlinx.coroutines.launch
import nl.dionsegijn.konfetti.compose.KonfettiView
import nl.dionsegijn.konfetti.compose.OnParticleSystemUpdateListener
import nl.dionsegijn.konfetti.core.PartySystem
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

const val GITHUB_REPO_URL = "https://github.com/benny10ben/Periodt/"

private val SIZE_XS = 12.sp
private val SIZE_SM = 13.sp
private val SIZE_MD = 14.sp
private val SIZE_LG = 15.sp

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: PeriodViewModel
) {
    val context        = LocalContext.current
    val uriHandler     = LocalUriHandler.current
    val scrollState    = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()

    // Observe ViewModel States for Legacy Import
    val pendingLegacy by viewModel.pendingLegacyImport.collectAsState()
    val allProfiles   by viewModel.profiles.collectAsState()

    // Feedback states
    var showExportSuccess   by remember { mutableStateOf(false) }
    var showImportSuccess   by remember { mutableStateOf(false) }
    var showClearConfirm    by remember { mutableStateOf(false) }
    var showClearSuccess    by remember { mutableStateOf(false) }
    var importResultMessage by remember { mutableStateOf("") }
    var showConfetti        by remember { mutableStateOf(false) }

    // Legacy success states
    var showLegacySuccess    by remember { mutableStateOf(false) }
    var legacySuccessMessage by remember { mutableStateOf("") }

    // Info dialog states
    var showFaq        by remember { mutableStateOf(false) }
    var showPrivacy    by remember { mutableStateOf(false) }
    var showAbout      by remember { mutableStateOf(false) }
    var showAlgorithm  by remember { mutableStateOf(false) }
    var showWhatsNew   by remember { mutableStateOf(false) }
    var showWidgetInfo by remember { mutableStateOf(false) }
    var showAppearance by remember { mutableStateOf(false) }

    val themeModeString by context.dataStore.data.collectAsState(initial = null)
    var themeMode by remember(themeModeString) {
        mutableStateOf(
            ThemeMode.valueOf(themeModeString?.get(THEME_MODE_KEY) ?: ThemeMode.SYSTEM.name)
        )
    }

    // Launchers
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let {
            viewModel.performExport(it) { success: Boolean, msg: String? ->
                if (success) showExportSuccess = true
                else Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            viewModel.performImport(it) { success: Boolean, msg: String? ->
                if (success) {
                    importResultMessage = msg ?: "Success"
                    showImportSuccess = true
                    showConfetti = true
                } else if (msg != null) {
                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    val isDark = LocalAppIsDark.current
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

    val surfaceColor = if (isDark) Color(0xFF1B1B1B).copy(alpha = 0.5f) else Color.White
    val textPrimary  = if (isDark) Color.White else Color(0xFF1B1B1B)
    val textSub      = if (isDark) Color.White.copy(alpha = 0.6f) else Color(0xFF1b1b1b).copy(alpha = 0.6f)
    val accentColor  = if (isDark) Color(0xFFD89046) else Color(0xFFa5bda3)

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
                        .clip(RoundedCornerShape(24.dp))
                        .background(accentColor.copy(alpha = 0.1f))
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier.size(36.dp).clip(CircleShape).background(accentColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.HealthAndSafety, contentDescription = null, tint = accentColor, modifier = Modifier.size(20.dp))
                    }
                    Text(
                        text = "Predictions are for informational purposes only and are not medical advice.",
                        fontFamily = BricolageGrotesque,
                        fontSize = SIZE_SM,
                        color = if (isDark) accentColor else Color(0xFF435C3C),
                        lineHeight = 18.sp,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(Modifier.height(24.dp))

                // 1. DATA & BACKUP
                SettingsSection(title = "Data & Backup", surfaceColor) {
                    SettingsItem(
                        icon = Icons.Rounded.Upload,
                        title = "Export Data",
                        subtitle = "Save unique JSON backup",
                        tint = textPrimary,
                        onClick = {
                            val timestamp = LocalDateTime.now()
                                .format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
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
                        subtitle = "Permanently delete everything?",
                        tint = Color(0xFFEF5350),
                        showChevron = false,
                        onClick = { showClearConfirm = true }
                    )
                }

                Spacer(Modifier.height(24.dp))

                // 2. CUSTOMIZE
                SettingsSection(title = "Customize", surfaceColor) {
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
                            ThemeMode.LIGHT -> "Light mode"
                            ThemeMode.DARK -> "Dark mode"
                        },
                        tint = textPrimary,
                        onClick = { showAppearance = true }
                    )
                }

                Spacer(Modifier.height(24.dp))

                // 3. HELP CENTER
                SettingsSection(title = "Help Center", surfaceColor) {
                    SettingsItem(
                        icon = Icons.Rounded.Calculate,
                        title = "How we calculate",
                        tint = textPrimary,
                        onClick = { showAlgorithm = true })
                    SettingsItem(
                        icon = Icons.AutoMirrored.Rounded.Help,
                        title = "FAQ",
                        tint = textPrimary,
                        onClick = { showFaq = true })
                    SettingsItem(
                        icon = Icons.Rounded.BugReport,
                        title = "Request a feature",
                        subtitle = "Report issues on GitHub",
                        tint = textPrimary,
                        onClick = {
                            try {
                                uriHandler.openUri("https://github.com/benny10ben/Periodt/issues")
                            } catch (e: Exception) {
                                Toast.makeText(
                                    context,
                                    "Could not open browser",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    )
                    SettingsItem(
                        icon = Icons.Rounded.NewReleases,
                        title = "What's new",
                        tint = textPrimary,
                        onClick = { showWhatsNew = true })
                }

                Spacer(Modifier.height(24.dp))

                // 4. MORE
                SettingsSection(title = "More", surfaceColor) {
                    SettingsItem(
                        icon = Icons.Rounded.Info,
                        title = "About Periodt",
                        subtitle = "v1.2.1",
                        tint = textPrimary,
                        onClick = { showAbout = true })
                    SettingsItem(
                        icon = Icons.Rounded.PrivacyTip,
                        title = "Privacy Policy",
                        tint = textPrimary,
                        onClick = { showPrivacy = true })
                }

                Spacer(Modifier.height(32.dp))
                Text(
                    text = "Dedicated to my love ❤️",
                    fontFamily = BricolageGrotesque,
                    fontSize = SIZE_SM,
                    color = textPrimary.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
                Spacer(Modifier.height(24.dp))
            }
        }
    }

    // --- SHEET TRIGGERS ---
    if (showAppearance) {
        AppearanceDialog(
            current = themeMode,
            onSelect = { selected ->
                coroutineScope.launch {
                    context.dataStore.edit { p -> p[THEME_MODE_KEY] = selected.name }
                    CalendarWidget.refreshAll(context)
                }
                showAppearance = false
            },
            onDismiss = { showAppearance = false }
        )
    }

    if (showWidgetInfo) {
        ContentDialog(title = "Home Screen Widgets", onDismiss = { showWidgetInfo = false }) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "To add the Periodt widget to your home screen:",
                    fontFamily = BricolageGrotesque,
                    color = textPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = SIZE_LG
                )
                val instructions = listOf(
                    "Long-press an empty space on your home screen.",
                    "Tap the 'Widgets' button in the menu.",
                    "Scroll down to find 'Periodt'.",
                    "Drag the widget to your desired position."
                )
                instructions.forEachIndexed { i, text ->
                    Text(
                        "${i + 1}. $text",
                        fontFamily = BricolageGrotesque,
                        color = textSub,
                        fontSize = SIZE_MD
                    )
                }
            }
        }
    }

    if (showAlgorithm) {
        ContentDialog(title = "How we calculate", onDismiss = { showAlgorithm = false }) {
            Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                Text(
                    text = "Periodt learns your rhythm over time. Here's how each prediction is built — the more you log, the sharper it gets.",
                    fontFamily = BricolageGrotesque,
                    color = textSub,
                    fontSize = SIZE_SM,
                    lineHeight = 20.sp
                )

                HorizontalDivider(color = textSub.copy(alpha = 0.12f))

                AlgoStep(
                    number = 1,
                    title = "Outlier filtering",
                    description = "Gaps spanning two or more cycles are treated as missed logs, not real long cycles, so they never skew your predictions.",
                    formula = "threshold = max(median × 2,  50 days)",
                    textPrimary = textPrimary,
                    textSub = textSub,
                    accentColor = accentColor
                )
                AlgoStep(
                    number = 2,
                    title = "Trend detection",
                    description = "Linear regression across your last 6 cycles detects if your rhythm is gradually shifting. That trend is blended with a recency-weighted average.",
                    formula = "prediction = 0.7 × trend + 0.3 × weighted avg",
                    textPrimary = textPrimary,
                    textSub = textSub,
                    accentColor = accentColor
                )
                AlgoStep(
                    number = 3,
                    title = "Regularity scoring",
                    description = "The standard deviation of your cycle lengths sets how wide the prediction window is.",
                    textPrimary = textPrimary,
                    textSub = textSub,
                    accentColor = accentColor
                ) {
                    AlgoRegularityTable(
                        textPrimary = textPrimary,
                        textSub = textSub,
                        isDark = isDark
                    )
                }
                AlgoStep(
                    number = 4,
                    title = "Ovulation & fertile window",
                    description = "Ovulation is back-calculated from your own logged luteal phase data where available — not a fixed assumption.",
                    formula = "ovulation = next period start − luteal phase",
                    textPrimary = textPrimary,
                    textSub = textSub,
                    accentColor = accentColor
                )

                Text(
                    text = "The more cycles you log, the more personalised your predictions become.",
                    fontFamily = BricolageGrotesque,
                    color = textSub.copy(alpha = 0.7f),
                    fontSize = SIZE_XS,
                    lineHeight = 18.sp
                )
            }
        }
    }

    if (showFaq) {
        ContentDialog(title = "FAQ", onDismiss = { showFaq = false }) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                FaqItem(
                    "Is my data secure?",
                    "Yes. Periodt is offline-first. Your data never leaves your device unless you manually export it.",
                    textPrimary,
                    textSub
                )
                FaqItem(
                    "How do I backup my data?",
                    "Go to Data & Backup and tap 'Export Data' to create a secure JSON file.",
                    textPrimary,
                    textSub
                )
                FaqItem(
                    "Can I sync across devices?",
                    "Not currently. To move data, Export on the old phone and Import on the new one.",
                    textPrimary,
                    textSub
                )
            }
        }
    }

    if (showPrivacy) {
        ContentDialog(title = "Privacy Policy", onDismiss = { showPrivacy = false }) {
            Text(
                text = "Your privacy is our priority. Periodt operates completely offline.\n\n" +
                        "• No data is sent to external servers.\n" +
                        "• All cycle history is stored in an encrypted database on this device.\n" +
                        "• We do not track location or use analytics cookies.\n\n" +
                        "If you delete the app, your data is permanently deleted.",
                fontFamily = BricolageGrotesque,
                color = textSub,
                fontSize = SIZE_MD,
                lineHeight = 22.sp
            )
        }
    }

    if (showAbout) {
        ContentDialog(title = "About", onDismiss = { showAbout = false }) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    "Periodt",
                    style = MaterialTheme.typography.headlineMedium,
                    fontFamily = BricolageGrotesque,
                    fontWeight = FontWeight.Bold,
                    color = textPrimary
                )
                Text(
                    "Designed to be simple, private, and aesthetic.",
                    fontFamily = BricolageGrotesque,
                    color = textSub,
                    fontSize = SIZE_MD,
                    textAlign = TextAlign.Center
                )
                Button(
                    onClick = {
                        try {
                            uriHandler.openUri(GITHUB_REPO_URL)
                        } catch (e: Exception) {
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp).height(56.dp),
                    shape = RoundedCornerShape(18.dp),
                ) {
                    Icon(Icons.Rounded.Code, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "View on GitHub",
                        color = Color.White,
                        fontFamily = BricolageGrotesque,
                        fontWeight = FontWeight.Bold,
                        fontSize = SIZE_MD
                    )
                }
            }
        }
    }

    // --- Feedback Dialogs ---
    if (showWhatsNew) WhatsNewDialog(onDismiss = { showWhatsNew = false })
    if (showExportSuccess) SuccessFeedbackDialog(
        "Backup Saved",
        "Your data has been successfully exported.",
        onDismiss = { showExportSuccess = false })
    if (showImportSuccess) SuccessFeedbackDialog(
        "Import Complete",
        importResultMessage,
        onDismiss = { showImportSuccess = false })
    if (showClearConfirm) DestructiveConfirmationDialog(
        "Clear All Data?",
        "Permanently delete all cycle history?",
        onConfirm = { viewModel.clearAllData(); showClearConfirm = false; showClearSuccess = true },
        onDismiss = { showClearConfirm = false })
    if (showClearSuccess) SuccessFeedbackDialog(
        "Fresh Start",
        "All data deleted.",
        "Done",
        onDismiss = { showClearSuccess = false })

    // --- Legacy Import Dialog & Flow ---
    if (pendingLegacy != null) {
        LegacyImportDialog(
            profiles = allProfiles,
            onImportToProfile = { selectedProfileId: Int? ->
                viewModel.completeLegacyImport(selectedProfileId) { success: Boolean, message: String? ->
                    if (success) {
                        legacySuccessMessage = message ?: "Legacy data successfully imported!"
                        showLegacySuccess = true
                        showConfetti = true
                    } else {
                        Toast.makeText(context, message ?: "Import failed", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            onDismiss = { viewModel.dismissLegacyImport() }
        )
    }

    if (showLegacySuccess) {
        SuccessFeedbackDialog(
            title = "Import Complete",
            message = legacySuccessMessage,
            onDismiss = { showLegacySuccess = false }
        )
    }

    if (showConfetti) {
        KonfettiView(
            modifier = Modifier.fillMaxSize(),
            parties = rainConfetti(),
            updateListener = object : OnParticleSystemUpdateListener {
                override fun onParticleSystemEnded(system: PartySystem, activeSystems: Int) { if (activeSystems == 0) showConfetti = false }
            }
        )
    }
}