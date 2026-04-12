package com.ben.periodt.uiux.overview

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.foundation.interaction.MutableInteractionSource

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
    val containerColor = if (isDark) Color(0xFF1B1B1B) else Color.White
    val innerPillBg     = if (isDark) Color.White.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.05f)
    val surfaceFallback = if (isDark) Color.Black else Color.Black.copy(alpha = 0.05f)
    val textPrimary     = if (isDark) Color.White else Color(0xFF1B1B1B)
    val textSub         = if (isDark) Color.White.copy(alpha = 0.6f) else Color(0xFF1b1b1b).copy(alpha = 0.6f)
    val accentColor     = if (isDark) Color(0xFFD89046) else Color(0xFF6d9567).copy(alpha = 0.6f)
    val fertilityAccent = if (isDark) Color(0xFFD89046) else Color(0xFF6d9567).copy(alpha = 0.6f)
    val pillAccent      = Color(0xFFa68e74)

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var enableAnimations by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(400)
        enableAnimations = true
    }

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
                .then(
                    if (enableAnimations) {
                        Modifier.animateContentSize(
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness    = Spring.StiffnessMediumLow
                            )
                        )
                    } else {
                        Modifier
                    }
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
    var showDialog by remember { mutableStateOf(false) }
    val isDark = LocalAppIsDark.current

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
                .clickable { showDialog = true }
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

    if (showDialog) {
        PeriodtWheelTimeSheet(
            initialTime = time,
            isDark = isDark,
            accentColor = accentColor,
            onDismiss = { showDialog = false },
            onConfirm = { newTime ->
                onTimePicked(newTime)
                showDialog = false
            }
        )
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PeriodtWheelTimeSheet(
    initialTime: LocalTime,
    isDark: Boolean,
    accentColor: Color,
    onDismiss: () -> Unit,
    onConfirm: (LocalTime) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val containerColor = if (isDark) Color(0xFF1B1B1B) else Color.White
    val textPrimary    = if (isDark) Color.White else Color(0xFF1B1B1B)
    val textSub        = if (isDark) Color.White.copy(alpha = 0.6f) else Color(0xFF1b1b1b).copy(alpha = 0.6f)
    val rowBg          = if (isDark) Color.White.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.05f)

    var isAm by remember { mutableStateOf(initialTime.hour < 12) }
    var hour by remember { mutableStateOf(if (initialTime.hour % 12 == 0) 12 else initialTime.hour % 12) }
    var minute by remember { mutableStateOf(initialTime.minute) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = containerColor,
        modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars),
        dragHandle = { BottomSheetDefaults.DragHandle(color = textSub.copy(alpha = 0.2f)) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .navigationBarsPadding()
                .padding(bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Select Time",
                    fontFamily = BricolageGrotesque,
                    style = MaterialTheme.typography.headlineSmall,
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
                    Icon(Icons.Default.Close, null, tint = textPrimary, modifier = Modifier.size(18.dp))
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(0.85f),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // AM/PM Picker
                WheelPicker(
                    items = listOf("AM", "PM"),
                    selectedIndex = if (isAm) 0 else 1,
                    onItemSelected = { isAm = (it == 0) },
                    textPrimary = textPrimary,
                    textSub = textSub,
                    selectedSize = 22f,
                    unselectedSize = 16f
                )

                // Hour Picker
                WheelPicker(
                    items = (1..12).map { it.toString().padStart(2, '0') },
                    selectedIndex = hour - 1,
                    onItemSelected = { hour = it + 1 },
                    textPrimary = textPrimary,
                    textSub = textSub
                )

                Text(":", fontFamily = BricolageGrotesque, fontSize = 28.sp, color = textPrimary, fontWeight = FontWeight.Bold)

                // Minute Picker
                WheelPicker(
                    items = (0..59).map { it.toString().padStart(2, '0') },
                    selectedIndex = minute,
                    onItemSelected = { minute = it },
                    textPrimary = textPrimary,
                    textSub = textSub
                )
            }

            Button(
                onClick = {
                    val finalHour = when {
                        isAm && hour == 12 -> 0
                        !isAm && hour < 12 -> hour + 12
                        else -> hour
                    }
                    onConfirm(LocalTime.of(finalHour, minute))
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = accentColor,
                    contentColor = Color.White
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
            ) {
                Text(
                    "Save Time",
                    fontFamily = BricolageGrotesque,
                    fontWeight = FontWeight.Bold,
                    fontSize = SIZE_LG
                )
            }
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun WheelPicker(
    items: List<String>,
    selectedIndex: Int,
    onItemSelected: (Int) -> Unit,
    textPrimary: Color,
    textSub: Color,
    selectedSize: Float = 28f,
    unselectedSize: Float = 20f
) {
    val itemHeight = 50.dp
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = selectedIndex)
    val snapBehavior = androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior(lazyListState = listState)

    val coroutineScope = rememberCoroutineScope()

    val centerIndex by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val visibleItemsInfo = layoutInfo.visibleItemsInfo
            if (visibleItemsInfo.isEmpty()) return@derivedStateOf -1

            val viewportCenter = layoutInfo.viewportEndOffset / 2
            val closestItem = visibleItemsInfo.minByOrNull {
                kotlin.math.abs((it.offset + (it.size / 2)) - viewportCenter)
            }
            (closestItem?.index ?: 1) - 1
        }
    }

    LaunchedEffect(centerIndex, listState.isScrollInProgress) {
        if (!listState.isScrollInProgress && centerIndex in items.indices) {
            onItemSelected(centerIndex)
        }
    }

    LazyColumn(
        state = listState,
        flingBehavior = snapBehavior,
        modifier = Modifier.height(itemHeight * 3).width(64.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item { Spacer(modifier = Modifier.height(itemHeight)) }

        items(items.size) { index ->
            val isSelected = centerIndex == index

            val animatedFontSize by animateFloatAsState(
                targetValue = if (isSelected) selectedSize else unselectedSize,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessLow
                ),
                label = "fontSizeAnim"
            )

            val animatedColor by animateColorAsState(
                targetValue = if (isSelected) textPrimary else textSub.copy(alpha = 0.3f),
                animationSpec = tween(150),
                label = "colorAnim"
            )

            Box(
                modifier = Modifier
                    .height(itemHeight)
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        coroutineScope.launch {
                            listState.animateScrollToItem(index)
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = items[index],
                    fontFamily = BricolageGrotesque,
                    fontSize = animatedFontSize.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = animatedColor
                )
            }
        }

        item { Spacer(modifier = Modifier.height(itemHeight)) }
    }
}