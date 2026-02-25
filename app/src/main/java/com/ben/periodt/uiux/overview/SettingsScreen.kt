package com.ben.periodt.uiux.overview

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Help
import androidx.compose.material.icons.automirrored.rounded.Message
import androidx.compose.material.icons.rounded.*
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.ben.periodt.ui.theme.BricolageGrotesque
import com.ben.periodt.viewmodel.PeriodViewModel
import nl.dionsegijn.konfetti.compose.KonfettiView
import nl.dionsegijn.konfetti.core.Party
import nl.dionsegijn.konfetti.core.PartySystem
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import nl.dionsegijn.konfetti.core.Position
import nl.dionsegijn.konfetti.core.emitter.Emitter
import java.util.concurrent.TimeUnit

const val GITHUB_REPO_URL = "https://github.com/benny10ben/Periodt/"

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: PeriodViewModel
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val scrollState = rememberScrollState()

    // --- Feedback States ---
    var showExportSuccess by remember { mutableStateOf(false) }
    var showImportSuccess by remember { mutableStateOf(false) }
    var showClearConfirm by remember { mutableStateOf(false) }
    var showClearSuccess by remember { mutableStateOf(false) }
    var importResultMessage by remember { mutableStateOf("") }
    var showConfetti by remember { mutableStateOf(false) }

    // --- Info Dialog States ---
    var showFaq by remember { mutableStateOf(false) }
    var showPrivacy by remember { mutableStateOf(false) }
    var showAbout by remember { mutableStateOf(false) }
    var showAlgorithm by remember { mutableStateOf(false) }
    var showWhatsNew by remember { mutableStateOf(false) }
    var showWidgetInfo by remember { mutableStateOf(false) }

    // --- Launchers ---
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let {
            viewModel.performExport(it) { success, msg ->
                if (success) {
                    showExportSuccess = true
                } else {
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                }
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

    // --- Theme & Colors (UPDATED) ---
    val isDark = isSystemInDarkTheme()

    // SYNCHRONIZED BACKGROUND: Pure Black to Dark Grey
    val bgGradient = if (isDark) {
        Brush.linearGradient(
            0.0f to Color.Black,
            0.7f to Color.Black,
            1.0f to Color(0xFF1B1B1B),
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

    // SOLID SURFACES: High contrast, no transparency in Dark Mode
    val surfaceColor = if (isDark) Color(0xFF1B1B1B) else Color.White
    val textPrimary = if (isDark) Color.White else Color(0xFF0F172A)
    val textSub = if (isDark) Color.White.copy(alpha = 0.6f) else Color(0xFF64748B)
    val accentColor = if (isDark) Color(0xFFD89046) else Color(0xFF2A3825)

    // --- Main Layout ---
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgGradient)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
        ) {
            // -- Header --
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onBack),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Back",
                        tint = textPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "Settings",
                    fontFamily = BricolageGrotesque,
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = textPrimary
                )
            }

            // -- Content List --
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(scrollState)
                    .padding(horizontal = 16.dp)
            ) {

                // 1. Data Management
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

                // 2. Customize
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
                        tint = textPrimary,
                        onClick = { Toast.makeText(context, "Coming soon!", Toast.LENGTH_SHORT).show() }
                    )
                }

                Spacer(Modifier.height(24.dp))

                // 3. Help Center
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
                            try {
                                uriHandler.openUri("https://github.com/benny10ben/Periodt/issues")
                            } catch (e: Exception) {
                                Toast.makeText(context, "Could not open browser", Toast.LENGTH_SHORT).show()
                            }
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

                // 4. More
                SettingsSection(title = "MORE", surfaceColor) {
                    SettingsItem(
                        icon = Icons.Rounded.Info,
                        title = "About Periodt",
                        subtitle = "v1.0.5",
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

                Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
                Spacer(Modifier.height(24.dp))
            }
        }
    }

    // --- Dialog Triggers ---
    if (showWidgetInfo) {
        ContentDialog(title = "Home Screen Widgets", onDismiss = { showWidgetInfo = false }) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(text = "To add the Periodt widget to your home screen:", fontFamily = BricolageGrotesque, color = textPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
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
                text = "Periodt uses a dynamic prediction algorithm designed to adapt to your unique cycle.\n\n" +
                        "1. We start with a standard 28-day cycle model.\n\n" +
                        "2. As you log more data, we calculate the average length of your last 3 completed cycles.\n\n" +
                        "3. This rolling average is used to predict your next period start date and fertile window.\n\n" +
                        "The more you log, the smarter it gets.",
                style = MaterialTheme.typography.bodyMedium,
                fontFamily = BricolageGrotesque,
                color = textSub,
                lineHeight = 22.sp
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
                color = textSub,
                lineHeight = 22.sp
            )
        }
    }

    if (showAbout) {
        ContentDialog(title = "About Periodt", onDismiss = { showAbout = false }) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "Periodt", style = MaterialTheme.typography.headlineMedium, fontFamily = BricolageGrotesque, fontWeight = FontWeight.Bold, color = textPrimary)
                Text(text = "v1.0.5", style = MaterialTheme.typography.labelLarge, fontFamily = BricolageGrotesque, color = textSub)
                Spacer(Modifier.height(16.dp))
                Text(text = "Designed to be simple, private, and aesthetic. Developed with ❤️ by Ben.", style = MaterialTheme.typography.bodyMedium, fontFamily = BricolageGrotesque, color = textSub, lineHeight = 22.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                Spacer(Modifier.height(24.dp))
                Button(onClick = { try { uriHandler.openUri("https://github.com/benny10ben/Periodt") } catch (e: Exception) { } }, colors = ButtonDefaults.buttonColors(containerColor = accentColor), shape = RoundedCornerShape(50)) {
                    Icon(imageVector = Icons.Rounded.Code, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("View on GitHub", color = Color.White, fontFamily = BricolageGrotesque, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    if (showWhatsNew) {
        WhatsNewDialog(onDismiss = { showWhatsNew = false })
    }

    // --- System Feedback Dialogs ---
    if (showExportSuccess) SuccessFeedbackDialog("Backup Saved", "Your data has been successfully exported.", onDismiss = { showExportSuccess = false })
    if (showImportSuccess) SuccessFeedbackDialog("Import Complete", importResultMessage, onDismiss = { showImportSuccess = false })
    if (showClearConfirm) DestructiveConfirmationDialog("Clear All Data?", "Permanently delete all cycle history?", onConfirm = { viewModel.clearAllData(); showClearConfirm = false; showClearSuccess = true }, onDismiss = { showClearConfirm = false })
    if (showClearSuccess) SuccessFeedbackDialog("Fresh Start", "All data deleted.", "Done", onDismiss = { showClearSuccess = false })

    // --- Confetti Effect ---
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

// --- Reusable Components ---

@Composable
fun ContentDialog(
    title: String,
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    val isDark = isSystemInDarkTheme()

    // 1. UPDATED GRADIENT: Removed blue, now matches MainScreen/Settings
    val bgGradient = if (isDark) {
        Brush.linearGradient(
            0.0f to Color.Black,
            1.0f to Color(0xFF1B1B1B)
        )
    } else {
        Brush.linearGradient(
            colors = listOf(Color(0xFFF8FAFC), Color(0xFFf2f0e3))
        )
    }

    // UPDATED: Standardized colors to your current palette
    val surfaceFallback = if (isDark) Color(0xFF1B1B1B) else Color.White
    val textPrimary = if (isDark) Color.White else Color(0xFF1B1B1B)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .padding(vertical = 24.dp),
            shape = RoundedCornerShape(26.dp),
            colors = CardDefaults.cardColors(containerColor = surfaceFallback),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(bgGradient)
            ) {
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = title,
                            modifier = Modifier.weight(1f),
                            fontFamily = BricolageGrotesque,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = textPrimary
                        )
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Close",
                                tint = textPrimary
                            )
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
        Text(text = question, fontFamily = BricolageGrotesque, fontWeight = FontWeight.Bold, color = primary, fontSize = 15.sp)
        Spacer(Modifier.height(4.dp))
        Text(text = answer, fontFamily = BricolageGrotesque, fontWeight = FontWeight.Normal, color = sub, fontSize = 14.sp, lineHeight = 20.sp)
    }
}

@Composable
fun SettingsSection(
    title: String,
    surfaceColor: Color,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            fontFamily = BricolageGrotesque,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            // UPDATED: Higher contrast for section titles in dark mode
            color = if (isSystemInDarkTheme()) Color.White.copy(alpha = 0.6f) else Color.Black.copy(alpha = 0.4f),
            modifier = Modifier.padding(start = 12.dp, bottom = 8.dp)
        )
        Card(
            shape = RoundedCornerShape(24.dp),
            // Uses the solid surfaceColor passed from SettingsScreen
            colors = CardDefaults.cardColors(containerColor = surfaceColor),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            modifier = Modifier.fillMaxWidth()
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
    val isDark = isSystemInDarkTheme()
    val subTextColor = if (isDark) Color.White.copy(alpha = 0.5f) else Color.Black.copy(alpha = 0.5f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            // Tint now matches the primary text color (White or Dark Blue)
            tint = tint,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontFamily = BricolageGrotesque,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = if (isDark) Color.White else Color(0xFF0F172A)
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    fontFamily = BricolageGrotesque,
                    fontSize = 13.sp,
                    color = subTextColor
                )
            }
        }
        if (showChevron) {
            Icon(
                imageVector = Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = subTextColor.copy(alpha = 0.4f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

fun rainConfetti(): List<Party> {
    return listOf(
        Party(
            speed = 0f,
            maxSpeed = 30f,
            damping = 0.9f,
            spread = 360,
            // Explicitly cast to Int to resolve the Long mismatch
            colors = listOf(
                0xf2b179.toInt(),
                0xFFD89046.toInt(),
                0xf4306d.toInt(),
                0xb48def.toInt()
            ),
            position = Position.Relative(0.5, 0.3),
            emitter = Emitter(duration = 100, TimeUnit.MILLISECONDS).max(100)
        )
    )
}
