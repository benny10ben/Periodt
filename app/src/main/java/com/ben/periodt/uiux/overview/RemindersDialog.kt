package com.ben.periodt.uiux.overview

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.datastore.preferences.core.edit
import com.ben.periodt.ui.theme.BricolageGrotesque
import com.ben.periodt.ui.theme.LocalAppIsDark
import com.ben.periodt.uiux.shared.ReminderPrefs
import com.ben.periodt.uiux.shared.ReminderScheduler
import com.ben.periodt.uiux.shared.dataStore
import com.ben.periodt.viewmodel.PeriodViewModel
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.time.format.DateTimeFormatter

private val SIZE_SM = 13.sp
private val SIZE_MD = 14.sp
private val SIZE_LG = 15.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemindersDialog(
    viewModel: PeriodViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val activeProfileId by viewModel.activeProfileId.collectAsState()
    val prefs by context.dataStore.data.collectAsState(initial = null)

    // --- MIGRATION LOGIC (Legacy to Profile-Scoped) ---
    LaunchedEffect(prefs, activeProfileId) {
        prefs?.let { p ->
            val scopedKey = ReminderPrefs.periodEnabled(activeProfileId)
            val legacyKey = ReminderPrefs.IS_ENABLED

            if (p[scopedKey] == null && p[legacyKey] != null) {
                context.dataStore.edit { editPrefs ->
                    editPrefs[ReminderPrefs.periodEnabled(activeProfileId)]       = p[ReminderPrefs.IS_ENABLED] ?: false
                    editPrefs[ReminderPrefs.periodDaysBefore(activeProfileId)]    = p[ReminderPrefs.DAYS_BEFORE] ?: 2
                    editPrefs[ReminderPrefs.periodHour(activeProfileId)]          = p[ReminderPrefs.TIME_HOUR] ?: 8
                    editPrefs[ReminderPrefs.periodMinute(activeProfileId)]        = p[ReminderPrefs.TIME_MINUTE] ?: 0

                    editPrefs[ReminderPrefs.fertilityEnabled(activeProfileId)]    = p[ReminderPrefs.FERTILITY_ENABLED] ?: false
                    editPrefs[ReminderPrefs.fertilityDaysBefore(activeProfileId)] = p[ReminderPrefs.FERTILITY_DAYS_BEFORE] ?: 2
                    editPrefs[ReminderPrefs.fertilityHour(activeProfileId)]       = p[ReminderPrefs.FERTILITY_HOUR] ?: 8
                    editPrefs[ReminderPrefs.fertilityMinute(activeProfileId)]     = p[ReminderPrefs.FERTILITY_MINUTE] ?: 0

                    editPrefs[ReminderPrefs.pillEnabled(activeProfileId)]         = p[ReminderPrefs.PILL_ENABLED] ?: false
                    editPrefs[ReminderPrefs.pillHour(activeProfileId)]            = p[ReminderPrefs.PILL_HOUR] ?: 8
                    editPrefs[ReminderPrefs.pillMinute(activeProfileId)]          = p[ReminderPrefs.PILL_MINUTE] ?: 0
                }
            }
        }
    }

    // --- SCOPED REMINDER STATES ---
    var periodEnabled by remember(prefs, activeProfileId) { mutableStateOf(prefs?.get(ReminderPrefs.periodEnabled(activeProfileId)) ?: prefs?.get(ReminderPrefs.IS_ENABLED) ?: false) }
    var periodDays    by remember(prefs, activeProfileId) { mutableStateOf(prefs?.get(ReminderPrefs.periodDaysBefore(activeProfileId)) ?: prefs?.get(ReminderPrefs.DAYS_BEFORE) ?: 2) }
    var periodTime    by remember(prefs, activeProfileId) { mutableStateOf(LocalTime.of(prefs?.get(ReminderPrefs.periodHour(activeProfileId)) ?: prefs?.get(ReminderPrefs.TIME_HOUR) ?: 8, prefs?.get(ReminderPrefs.periodMinute(activeProfileId)) ?: prefs?.get(ReminderPrefs.TIME_MINUTE) ?: 0)) }

    var fertilityEnabled by remember(prefs, activeProfileId) { mutableStateOf(prefs?.get(ReminderPrefs.fertilityEnabled(activeProfileId)) ?: prefs?.get(ReminderPrefs.FERTILITY_ENABLED) ?: false) }
    var fertilityDays    by remember(prefs, activeProfileId) { mutableStateOf(prefs?.get(ReminderPrefs.fertilityDaysBefore(activeProfileId)) ?: prefs?.get(ReminderPrefs.FERTILITY_DAYS_BEFORE) ?: 2) }
    var fertilityTime    by remember(prefs, activeProfileId) { mutableStateOf(LocalTime.of(prefs?.get(ReminderPrefs.fertilityHour(activeProfileId)) ?: prefs?.get(ReminderPrefs.FERTILITY_HOUR) ?: 8, prefs?.get(ReminderPrefs.fertilityMinute(activeProfileId)) ?: prefs?.get(ReminderPrefs.FERTILITY_MINUTE) ?: 0)) }

    var pillEnabled by remember(prefs, activeProfileId) { mutableStateOf(prefs?.get(ReminderPrefs.pillEnabled(activeProfileId)) ?: prefs?.get(ReminderPrefs.PILL_ENABLED) ?: false) }
    var pillTime    by remember(prefs, activeProfileId) { mutableStateOf(LocalTime.of(prefs?.get(ReminderPrefs.pillHour(activeProfileId)) ?: prefs?.get(ReminderPrefs.PILL_HOUR) ?: 8, prefs?.get(ReminderPrefs.pillMinute(activeProfileId)) ?: prefs?.get(ReminderPrefs.PILL_MINUTE) ?: 0)) }

    val powerManager       = remember { context.getSystemService(Context.POWER_SERVICE) as android.os.PowerManager }
    val isBatteryOptimized = remember { !powerManager.isIgnoringBatteryOptimizations(context.packageName) }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}

    fun checkAndRequestNotifPermission(onGranted: () -> Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val has = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            if (!has) { permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS); return }
        }
        onGranted()
    }

    fun saveReminders() {
        coroutineScope.launch {
            context.dataStore.edit { p ->
                p[ReminderPrefs.periodEnabled(activeProfileId)]            = periodEnabled
                p[ReminderPrefs.periodDaysBefore(activeProfileId)]         = periodDays
                p[ReminderPrefs.periodHour(activeProfileId)]               = periodTime.hour
                p[ReminderPrefs.periodMinute(activeProfileId)]             = periodTime.minute

                p[ReminderPrefs.fertilityEnabled(activeProfileId)]         = fertilityEnabled
                p[ReminderPrefs.fertilityDaysBefore(activeProfileId)]      = fertilityDays
                p[ReminderPrefs.fertilityHour(activeProfileId)]            = fertilityTime.hour
                p[ReminderPrefs.fertilityMinute(activeProfileId)]          = fertilityTime.minute

                p[ReminderPrefs.pillEnabled(activeProfileId)]              = pillEnabled
                p[ReminderPrefs.pillHour(activeProfileId)]                 = pillTime.hour
                p[ReminderPrefs.pillMinute(activeProfileId)]               = pillTime.minute
            }
            if (periodEnabled)    ReminderScheduler.scheduleNextReminder(context, periodTime.hour, periodTime.minute)
            else                  ReminderScheduler.cancelReminder(context)
            if (fertilityEnabled) ReminderScheduler.scheduleNextFertilityReminder(context, fertilityTime.hour, fertilityTime.minute)
            else                  ReminderScheduler.cancelFertilityReminder(context)
            if (pillEnabled)      ReminderScheduler.scheduleNextPillReminder(context, pillTime.hour, pillTime.minute)
            else                  ReminderScheduler.cancelPillReminder(context)
        }
    }

    // Colors
    val isDark = LocalAppIsDark.current
    val containerColor  = if (isDark) Color(0xFF1B1B1B) else Color(0xFFF8FAFC)
    val innerPillBg     = if (isDark) Color.White.copy(alpha = 0.06f) else Color.Black.copy(alpha = 0.04f)
    val surfaceFallback = if (isDark) Color(0xFF2C2C2C) else Color.White
    val textPrimary     = if (isDark) Color.White else Color(0xFF1B1B1B)
    val textSub         = if (isDark) Color.White.copy(alpha = 0.6f) else Color(0xFF1b1b1b).copy(alpha = 0.6f)
    val accentColor     = if (isDark) Color(0xFFD89046) else Color(0xFF6d9567).copy(alpha = 0.6f)
    val fertilityAccent = if (isDark) Color(0xFFD89046) else Color(0xFF6d9567).copy(alpha = 0.6f)
    val pillAccent      = Color(0xFFa68e74)

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = containerColor,
        modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .navigationBarsPadding()
                .padding(bottom = 16.dp)
                .verticalScroll(rememberScrollState())
                .animateContentSize(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness    = Spring.StiffnessMediumLow
                    )
                ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Text(
                    "Reminders",
                    modifier = Modifier.weight(1f),
                    fontFamily = BricolageGrotesque,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = textPrimary
                )
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(textSub.copy(alpha = 0.1f))
                        .clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.Close, "Close", tint = textPrimary, modifier = Modifier.size(18.dp))
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {

                if (isBatteryOptimized) {
                    BatteryWarning(
                        pillBg = innerPillBg, accentColor = accentColor,
                        textPrimary = textPrimary, textSub = textSub, context = context
                    )
                }

                // Period
                ReminderSection(
                    title = "Period", subtitle = "Notify before your next period",
                    icon = Icons.Rounded.WaterDrop, iconTint = accentColor,
                    isEnabled = periodEnabled, pillBg = innerPillBg,
                    textPrimary = textPrimary, textSub = textSub,
                    onToggleChange = { checked -> checkAndRequestNotifPermission { periodEnabled = checked; saveReminders() } }
                ) {
                    ReminderDaysSelector(
                        label = "Remind me before", options = listOf(1, 2, 3, 4, 7),
                        selected = periodDays, accentColor = accentColor, pillBg = surfaceFallback,
                        textPrimary = textPrimary, onSelect = { periodDays = it; saveReminders() }
                    )
                    ReminderTimePicker(
                        time = periodTime, pillBg = surfaceFallback, accentColor = accentColor,
                        textPrimary = textPrimary, context = context,
                        onTimePicked = { periodTime = it; saveReminders() }
                    )
                }

                // Fertility
                ReminderSection(
                    title = "Fertility", subtitle = "Notify before your fertile window",
                    icon = Icons.Rounded.Favorite, iconTint = fertilityAccent,
                    isEnabled = fertilityEnabled, pillBg = innerPillBg,
                    textPrimary = textPrimary, textSub = textSub,
                    onToggleChange = { checked -> checkAndRequestNotifPermission { fertilityEnabled = checked; saveReminders() } }
                ) {
                    ReminderDaysSelector(
                        label = "Remind me before ovulation", options = listOf(0, 1, 2, 3, 5),
                        selected = fertilityDays, accentColor = fertilityAccent, pillBg = surfaceFallback,
                        textPrimary = textPrimary, onSelect = { fertilityDays = it; saveReminders() },
                        labelOverride = { days -> when (days) { 0 -> "Day of"; 1 -> "1d"; else -> "${days}d" } }
                    )
                    ReminderTimePicker(
                        time = fertilityTime, pillBg = surfaceFallback, accentColor = fertilityAccent,
                        textPrimary = textPrimary, context = context,
                        onTimePicked = { fertilityTime = it; saveReminders() }
                    )
                }

                // Pill
                ReminderSection(
                    title = "Daily Pill", subtitle = "Reminder to take your pill",
                    icon = Icons.Rounded.Medication, iconTint = pillAccent,
                    isEnabled = pillEnabled, pillBg = innerPillBg,
                    textPrimary = textPrimary, textSub = textSub,
                    onToggleChange = { checked -> checkAndRequestNotifPermission { pillEnabled = checked; saveReminders() } }
                ) {
                    ReminderTimePicker(
                        time = pillTime, pillBg = surfaceFallback, accentColor = pillAccent,
                        textPrimary = textPrimary, context = context,
                        onTimePicked = { pillTime = it; saveReminders() }
                    )
                }
            }
        }
    }
}

// --- SUB-COMPONENTS ---

@Composable
private fun ReminderSection(
    title: String, subtitle: String, icon: ImageVector, iconTint: Color,
    isEnabled: Boolean, pillBg: Color, textPrimary: Color, textSub: Color,
    onToggleChange: (Boolean) -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(pillBg)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onToggleChange(!isEnabled) }
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(iconTint.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = iconTint, modifier = Modifier.size(22.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    fontFamily = BricolageGrotesque,
                    fontWeight = FontWeight.Bold,
                    fontSize = SIZE_LG,
                    color = textPrimary
                )
                Text(
                    subtitle,
                    fontFamily = BricolageGrotesque,
                    fontSize = SIZE_SM,
                    color = textSub
                )
            }
            Switch(
                checked = isEnabled, onCheckedChange = onToggleChange,
                modifier = Modifier.scale(0.85f),
                colors = SwitchDefaults.colors(
                    checkedTrackColor   = iconTint,
                    uncheckedTrackColor = textSub.copy(alpha = 0.2f),
                    checkedThumbColor   = Color.White,
                    uncheckedThumbColor = textSub.copy(alpha = 0.8f),
                    uncheckedBorderColor = Color.Transparent
                )
            )
        }

        AnimatedVisibility(
            visible = isEnabled,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp, bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                HorizontalDivider(color = textSub.copy(alpha = 0.1f))
                content()
            }
        }
    }
}

@Composable
private fun ReminderDaysSelector(
    label: String, options: List<Int>, selected: Int,
    accentColor: Color, pillBg: Color, textPrimary: Color,
    onSelect: (Int) -> Unit, labelOverride: ((Int) -> String)? = null
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            label,
            fontFamily = BricolageGrotesque,
            fontWeight = FontWeight.SemiBold,
            fontSize = SIZE_SM,
            color = textPrimary
        )
        // Segmented Control Style
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(pillBg)
                .padding(4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            options.forEach { days ->
                val isSelected = selected == days
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) accentColor else Color.Transparent)
                        .clickable { onSelect(days) }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = labelOverride?.invoke(days) ?: if (days == 7) "1w" else "${days}d",
                        fontFamily = BricolageGrotesque,
                        color = if (isSelected) Color.White else textPrimary,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        fontSize = SIZE_SM
                    )
                }
            }
        }
    }
}

@Composable
private fun ReminderTimePicker(
    time: LocalTime, pillBg: Color, accentColor: Color,
    textPrimary: Color, context: Context,
    onTimePicked: (LocalTime) -> Unit
) {
    // Inline Time Picker
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "Time",
            fontFamily = BricolageGrotesque,
            fontWeight = FontWeight.SemiBold,
            fontSize = SIZE_SM,
            color = textPrimary
        )
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(pillBg)
                .clickable {
                    android.app.TimePickerDialog(
                        context, { _, h, m -> onTimePicked(LocalTime.of(h, m)) },
                        time.hour, time.minute, false
                    ).show()
                }
                .padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            Text(
                time.format(DateTimeFormatter.ofPattern("hh:mm a")),
                fontFamily = BricolageGrotesque,
                fontWeight = FontWeight.Bold,
                color = accentColor,
                fontSize = SIZE_MD
            )
        }
    }
}

@Composable
private fun BatteryWarning(
    pillBg: Color, accentColor: Color,
    textPrimary: Color, textSub: Color, context: Context
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(pillBg)
            .clickable {
                context.startActivity(
                    Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = android.net.Uri.fromParts("package", context.packageName, null)
                    }
                )
            }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(Icons.Rounded.WarningAmber, null, tint = accentColor, modifier = Modifier.size(24.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "Background Restricted",
                fontFamily = BricolageGrotesque,
                color = textPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = SIZE_MD
            )
            Text(
                "Tap to allow unrestricted battery usage so reminders arrive on time.",
                fontFamily = BricolageGrotesque,
                color = textSub,
                fontSize = SIZE_SM
            )
        }
    }
}