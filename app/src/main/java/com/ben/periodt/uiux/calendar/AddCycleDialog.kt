package com.ben.periodt.uiux.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.EventAvailable
import androidx.compose.material.icons.rounded.Update
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.ben.periodt.ui.theme.BricolageGrotesque
import com.ben.periodt.ui.theme.LocalAppIsDark
import com.ben.periodt.viewmodel.PeriodViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.ceil
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddCycleDialog(
    onDismiss: () -> Unit,
    onSave: (LocalDate, LocalDate?, String, String, Int, Map<LocalDate, Triple<String, String, Int>>) -> Unit
) {
    // --- STATE MANAGEMENT ---
    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker by remember { mutableStateOf(false) }

    var startDate by remember { mutableStateOf(LocalDate.now()) }
    var endDate by remember { mutableStateOf<LocalDate?>(null) }
    var bleeding by remember { mutableStateOf("Medium") }
    var bloodColor by remember { mutableStateOf("Bright Red") }

    var sliderPosition by remember { mutableStateOf(5f) }
    var painLevel by remember { mutableIntStateOf(5) }

    val bleedingOptions = listOf("Heavy", "Medium", "Light", "Spotting")
    val colorOptions = listOf("Bright Red", "Dark Red", "Brown")

    // --- DAILY LOG STATE ---
    var showDailyLog by remember { mutableStateOf(false) }
    var selectedDayForLog by remember { mutableStateOf<LocalDate?>(null) }
    var dailyOverrides by remember { mutableStateOf<Map<LocalDate, Triple<String, String, Int>>>(emptyMap()) }

    val today = LocalDate.now()
    val cycleDays = remember(startDate, endDate) {
        val end = endDate ?: if (startDate.isBefore(today)) today else startDate
        generateSequence(startDate) { it.plusDays(1) }
            .takeWhile { !it.isAfter(end) }
            .toList()
    }

    val flowWeights = remember { mapOf("Spotting" to 1, "Light" to 2, "Medium" to 3, "Heavy" to 4) }
    val reverseFlowWeights = remember { mapOf(1 to "Spotting", 2 to "Light", 3 to "Medium", 4 to "Heavy") }

    val derivedBleeding = remember(dailyOverrides, bleeding, cycleDays) {
        if (dailyOverrides.isEmpty() || cycleDays.isEmpty()) return@remember bleeding
        val maxWeight = cycleDays.maxOfOrNull { day ->
            val dailyFlow = dailyOverrides[day]?.first ?: bleeding
            flowWeights[dailyFlow] ?: 0
        } ?: 0
        reverseFlowWeights[maxWeight] ?: bleeding
    }

    val derivedColor = remember(dailyOverrides, bloodColor, cycleDays) {
        if (dailyOverrides.isEmpty() || cycleDays.isEmpty()) return@remember bloodColor
        val allColors = cycleDays.map { day -> dailyOverrides[day]?.second ?: bloodColor }
        allColors.groupingBy { it }.eachCount().maxByOrNull { it.value }?.key ?: bloodColor
    }

    val derivedPain = remember(dailyOverrides, painLevel, cycleDays) {
        if (dailyOverrides.isEmpty() || cycleDays.isEmpty()) return@remember painLevel
        val totalPain = cycleDays.sumOf { day -> dailyOverrides[day]?.third ?: painLevel }
        kotlin.math.round(totalPain.toFloat() / cycleDays.size).toInt()
    }

    // --- THEME & COLORS ---
    val isDark = LocalAppIsDark.current
    val contentSurface = if (isDark) Brush.linearGradient(0.0f to Color.Black, 1.0f to Color(0xFF1B1B1B))
    else Brush.linearGradient(colors = listOf(Color(0xFFF8FAFC), Color(0xFFf2f0e3)))
    val accentColor = if (isDark) Color(0xFFD89046) else Color(0xFF6d9567).copy(alpha = 0.6f)
    val surfaceFallback = if (isDark) Color.Black else Color.White
    val pastelGreen = Color(0xFF6d9567).copy(alpha = 0.6f)
    val pastelOrange = Color(0xFFD89046)
    val pastelMaroon = Color(0xFF4E1A1A)
    val pillBackground = if (isDark) Color.White.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.05f)
    val pillTextColor = if (isDark) Color.White else Color(0xFF1B1B1B)
    val textPrimary = if (isDark) Color.White else Color(0xFF0F172A)
    val textSub = if (isDark) Color.White.copy(alpha = 0.6f) else Color(0xFF1B1B1B)

    val formatter = remember { DateTimeFormatter.ofPattern("MMM dd") }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Card(
            modifier = Modifier.fillMaxWidth(0.92f).padding(vertical = 24.dp),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = surfaceFallback),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth().background(contentSurface)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 24.dp, start = 24.dp, end = 24.dp, bottom = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Log Cycle", fontFamily = BricolageGrotesque, style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold), color = textPrimary)
                    Box(modifier = Modifier.size(32.dp).clip(CircleShape).background(textSub.copy(alpha = 0.1f)).clickable(onClick = onDismiss), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Close, "Close", tint = textPrimary, modifier = Modifier.size(18.dp))
                    }
                }

                Column(modifier = Modifier.verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
                    // Date Selectors
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        CleanDateCard("Start Date", startDate.format(formatter), Icons.Rounded.CalendarToday, pillBackground, pillTextColor, { showStartPicker = true }, Modifier.weight(1f))
                        CleanDateCard("End Date", endDate?.format(formatter) ?: "Ongoing", if (endDate == null) Icons.Rounded.Update else Icons.Rounded.EventAvailable, pillBackground, pillTextColor, { showEndPicker = true }, Modifier.weight(1f))
                    }

                    // --- SUMMARY OR PICKERS ---
                    if (dailyOverrides.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Summary", fontFamily = BricolageGrotesque, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = textPrimary)

                            val annotatedSummary = androidx.compose.ui.text.buildAnnotatedString {
                                append("Based on your daily logs, this cycle has a peak flow of ")
                                pushStyle(androidx.compose.ui.text.SpanStyle(fontWeight = FontWeight.Bold, color = textPrimary))
                                append(derivedBleeding.lowercase())
                                append(" (${derivedColor.lowercase()})")
                                pop()
                                append(", with an average pain level of ")
                                pushStyle(androidx.compose.ui.text.SpanStyle(fontWeight = FontWeight.Bold, color = textPrimary))
                                append("$derivedPain/10")
                                pop()
                                append(".")
                            }

                            Text(
                                text = annotatedSummary,
                                fontFamily = BricolageGrotesque,
                                fontSize = 14.sp,
                                color = textSub,
                                lineHeight = 20.sp
                            )
                        }
                    } else {
                        // Flow Intensity
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("Flow Intensity", fontFamily = BricolageGrotesque, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = textPrimary)
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                bleedingOptions.forEach { option ->
                                    val isSelected = bleeding.equals(option, ignoreCase = true)
                                    val activeBg = when(option) { "Heavy" -> pastelMaroon; "Medium" -> pastelOrange; else -> pastelGreen }
                                    EntryStylePill(option, isSelected, activeBg, Color.White, textSub, surfaceFallback) { bleeding = option }
                                }
                            }
                        }

                        // Blood Color
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("Blood Color", fontFamily = BricolageGrotesque, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = textPrimary)
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                colorOptions.forEach { option ->
                                    val isSelected = bloodColor.equals(option, ignoreCase = true)
                                    val activeBg = when(option) { "Bright Red" -> pastelGreen; "Dark Red" -> Color(0xFF4E1A1A); "Brown" -> pastelOrange; else -> accentColor }
                                    EntryStylePill(option, isSelected, activeBg, Color.White, textSub, surfaceFallback) { bloodColor = option }
                                }
                            }
                        }

                        // Pain Level
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text("Cramps & Pain", fontFamily = BricolageGrotesque, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = textPrimary)
                                Text("${sliderPosition.toInt()} / 10", fontFamily = BricolageGrotesque, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = accentColor)
                            }
                            Slider(value = sliderPosition, onValueChange = { sliderPosition = it; painLevel = it.toInt() }, valueRange = 0f..10f, colors = SliderDefaults.colors(thumbColor = accentColor, activeTrackColor = accentColor, inactiveTrackColor = pillBackground))
                        }
                    }

                    // --- DAILY LOG SECTION ---
                    if (cycleDays.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(pillBackground).clickable { showDailyLog = !showDailyLog }.padding(16.dp, 14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Log by day", fontFamily = BricolageGrotesque, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = pillTextColor)
                                    Text(
                                        if (dailyOverrides.isEmpty()) "Optional • uses cycle default" else "${dailyOverrides.size} day${if (dailyOverrides.size > 1) "s" else ""} customized",
                                        fontFamily = BricolageGrotesque, fontSize = 12.sp, color = if (dailyOverrides.isEmpty()) pillTextColor.copy(alpha = 0.5f) else accentColor
                                    )
                                }
                                Text(if (showDailyLog) "▲" else "▼", color = pillTextColor.copy(alpha = 0.5f), fontSize = 12.sp, fontFamily = BricolageGrotesque)
                            }

                            if (showDailyLog) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    cycleDays.forEach { day ->
                                        val override    = dailyOverrides[day]
                                        val dayBleeding = override?.first  ?: bleeding
                                        val dayColor    = override?.second ?: bloodColor
                                        val dayPain     = override?.third  ?: painLevel
                                        val dayLabel    = day.format(DateTimeFormatter.ofPattern("EEE, MMM d"))
                                        val isCustom    = override != null

                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(14.dp))
                                                .background(if (isCustom) accentColor.copy(alpha = 0.12f) else pillBackground)
                                                .clickable { selectedDayForLog = day }
                                                .padding(horizontal = 16.dp, vertical = 12.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text(
                                                    text = dayLabel,
                                                    fontFamily = BricolageGrotesque,
                                                    fontWeight = FontWeight.SemiBold,
                                                    fontSize = 14.sp,
                                                    color = if (isCustom) textPrimary else textPrimary.copy(alpha = 0.8f)
                                                )
                                                Spacer(Modifier.height(2.dp))
                                                Text(
                                                    text = "$dayBleeding • $dayColor • Pain: $dayPain/10",
                                                    fontFamily = BricolageGrotesque,
                                                    fontSize = 12.sp,
                                                    color = textSub.copy(alpha = 0.6f)
                                                )
                                            }
                                            Icon(
                                                imageVector = Icons.Default.Edit,
                                                contentDescription = "Edit",
                                                tint = if (isCustom) accentColor else textSub.copy(alpha = 0.3f),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Button(
                        onClick = { onSave(startDate, endDate, derivedBleeding, derivedColor, derivedPain, dailyOverrides) },
                        modifier = Modifier.fillMaxWidth().height(56.dp).padding(bottom = 8.dp),
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor, contentColor = Color.White),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                    ) {
                        Text("Save Entry", fontFamily = BricolageGrotesque, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // --- NESTED DAY LOG DIALOG ---
    selectedDayForLog?.let { day ->
        val override = dailyOverrides[day]
        val existingLog = override?.let {
            PeriodViewModel.DailyLog(id = 0, cycleId = 0, date = day, bleeding = it.first, bloodColor = it.second, painLevel = it.third)
        }
        val tempCycle = PeriodViewModel.Cycle(id = 0, startDate = startDate, endDate = endDate, bleeding = bleeding, bloodColor = bloodColor, painLevel = painLevel)

        DayLogDialog(
            date = day,
            cycle = tempCycle,
            existingLog = existingLog,
            onDismiss = { selectedDayForLog = null },
            onSave = { newBleeding, newColor, newPain ->
                dailyOverrides = dailyOverrides.toMutableMap().apply { put(day, Triple(newBleeding, newColor, newPain)) }
                selectedDayForLog = null
            },
            onClear = {
                dailyOverrides = dailyOverrides.toMutableMap().apply { remove(day) }
                selectedDayForLog = null
            }
        )
    }

    // --- DATE PICKERS ---
    if (showStartPicker) {
        MinimalDatePickerDialog(
            title = "Start Date",
            brand = accentColor,
            gradTop = pastelGreen, gradMid = pastelOrange, gradBottom = pastelMaroon,
            onGradient = Color.White,
            buttonContainer = surfaceFallback,
            buttonContent = textPrimary,
            onDismiss = { showStartPicker = false },
            onConfirm = { ms ->
                millisToLocalDate(ms)?.let { startDate = it }
                showStartPicker = false
            }
        )
    }
    if (showEndPicker) {
        MinimalDatePickerDialog(
            title = "End Date",
            brand = accentColor,
            gradTop = pastelGreen, gradMid = pastelOrange, gradBottom = pastelMaroon,
            onGradient = Color.White,
            buttonContainer = surfaceFallback,
            buttonContent = textPrimary,
            onDismiss = { showEndPicker = false },
            onConfirm = { ms ->
                millisToLocalDate(ms)?.let { endDate = it }
                showEndPicker = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MinimalDatePickerDialog(
    title: String,
    brand: Color,
    gradTop: Color,
    gradMid: Color,
    gradBottom: Color,
    onGradient: Color,
    buttonContainer: Color,
    buttonContent: Color,
    onDismiss: () -> Unit,
    onConfirm: (Long) -> Unit
) {
    val isDark = LocalAppIsDark.current
    val zone = remember { ZoneId.systemDefault() }

    // Default to today
    var selectedMillis by remember {
        mutableStateOf(Instant.now().atZone(zone).toLocalDate().atStartOfDay(zone).toInstant().toEpochMilli())
    }
    var displayedYm by remember {
        mutableStateOf(
            Instant.ofEpochMilli(selectedMillis).atZone(zone).toLocalDate().let { d ->
                YearMonth.of(d.year, d.month)
            }
        )
    }

    val contentSurface = if (isDark) {
        Brush.linearGradient(0.0f to Color.Black, 1.0f to Color(0xFF1B1B1B))
    } else {
        Brush.linearGradient(colors = listOf(Color(0xFFF8FAFC), Color(0xFFf2f0e3)))
    }

    val surfaceFallback = if (isDark) Color.Black else Color.White
    val textPrimary = if (isDark) Color.White else Color(0xFF1B1B1B)
    val accentColor = if (isDark) Color(0xFFD89046) else Color(0xFF6d9567).copy(alpha = 0.6f)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = true)
    ) {
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = surfaceFallback),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            modifier = Modifier.fillMaxWidth().wrapContentHeight()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(contentSurface)
                    .padding(horizontal = 16.dp, vertical = 24.dp)
            ) {
                Text(
                    text = title,
                    fontFamily = BricolageGrotesque,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, fontSize = 22.sp),
                    color = textPrimary,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
                    textAlign = TextAlign.Center
                )

                MinimalMonthPicker(
                    displayedYm = displayedYm,
                    selectedMillis = selectedMillis,
                    onDisplayedYmChange = { displayedYm = it },
                    onSelect = { ms -> selectedMillis = ms },
                    textColor = textPrimary,
                    accentColor = accentColor,
                    // FIXED: Changed to false to match your main CalendarScreen
                    weekStartsOnMonday = false
                )

                Spacer(Modifier.height(24.dp))

                Button(
                    onClick = { onConfirm(selectedMillis) },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor, contentColor = Color.White),
                    shape = RoundedCornerShape(50)
                ) {
                    Text("Confirm Date", fontFamily = BricolageGrotesque, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun MinimalMonthPicker(
    displayedYm: YearMonth,
    selectedMillis: Long?,
    onDisplayedYmChange: (YearMonth) -> Unit,
    onSelect: (Long) -> Unit,
    textColor: Color,
    accentColor: Color,
    weekStartsOnMonday: Boolean
) {
    val isDark = LocalAppIsDark.current
    val zone = remember { ZoneId.systemDefault() }
    val today = remember { LocalDate.now(zone) }
    val selectedDate = selectedMillis?.let { Instant.ofEpochMilli(it).atZone(zone).toLocalDate() }

    fun weekdayIndex(d: LocalDate): Int {
        val iso = d.dayOfWeek.value
        return if (weekStartsOnMonday) iso - 1 else (iso % 7)
    }

    val firstOfMonth = displayedYm.atDay(1)
    val daysInMonth = displayedYm.lengthOfMonth()
    val leadingBlanks = weekdayIndex(firstOfMonth)
    val totalCells = leadingBlanks + daysInMonth
    val rows = ceil(totalCells / 7f).toInt()

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "${displayedYm.month.name.lowercase().replaceFirstChar { it.uppercase() }} ${displayedYm.year}",
            fontFamily = BricolageGrotesque,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = textColor
        )

        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            IconButton(onClick = { onDisplayedYmChange(displayedYm.minusMonths(1)) }, modifier = Modifier.size(32.dp)) {
                Text(
                    text = "‹",
                    fontFamily = BricolageGrotesque,
                    color = textColor.copy(alpha = 0.7f),
                    fontSize = 24.sp
                )
            }
            IconButton(onClick = { onDisplayedYmChange(displayedYm.plusMonths(1)) }, modifier = Modifier.size(32.dp)) {
                Text(
                    text = "›",
                    fontFamily = BricolageGrotesque,
                    color = textColor.copy(alpha = 0.7f),
                    fontSize = 24.sp
                )
            }
        }
    }

    Spacer(Modifier.height(12.dp))

    val labels = if (weekStartsOnMonday)
        listOf("MON","TUE","WED","THU","FRI","SAT","SUN")
    else
        listOf("SUN","MON","TUE","WED","THU","FRI","SAT")

    Row(Modifier.fillMaxWidth()) {
        labels.forEach {
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Text(
                    text = it,
                    fontFamily = BricolageGrotesque,
                    style = MaterialTheme.typography.labelSmall,
                    color = textColor.copy(alpha = 0.5f),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }

    Spacer(Modifier.height(8.dp))

    val todayChipBg = textColor.copy(alpha = 0.1f)
    val selectedChipBg = accentColor
    // UPDATED: In dark mode, text on the yellow chip is Black for contrast
    val selectedChipText = if (isDark) Color.Black else Color.White

    var day = 1
    repeat(rows) { r ->
        Row(Modifier.fillMaxWidth()) {
            repeat(7) { c ->
                val idx = r * 7 + c
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(1f),
                    contentAlignment = Alignment.Center
                ) {
                    if (idx >= leadingBlanks && day <= daysInMonth) {
                        val date = displayedYm.atDay(day)
                        val isToday = date == today
                        val isSelected = date == selectedDate

                        val dayTextColor = when {
                            isSelected -> selectedChipText
                            isToday -> textColor
                            else -> textColor
                        }

                        val click = {
                            val ms = date.atStartOfDay(zone).toInstant().toEpochMilli()
                            onSelect(ms)
                        }

                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(selectedChipBg)
                                    .clickable(onClick = click),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "$day",
                                    fontFamily = BricolageGrotesque,
                                    color = dayTextColor,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        } else if (isToday) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(todayChipBg)
                                    .clickable(onClick = click),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "$day",
                                    fontFamily = BricolageGrotesque,
                                    color = dayTextColor,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .clickable(onClick = click),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "$day",
                                    fontFamily = BricolageGrotesque,
                                    color = dayTextColor,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                        day++
                    }
                }
            }
        }
        Spacer(Modifier.height(2.dp))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DailyLogRow(
    label: String,
    bleeding: String,
    bloodColor: String,
    painLevel: Int,
    isCustom: Boolean,
    accentColor: Color,
    pillBackground: Color,
    pillTextColor: Color,
    textSub: Color,
    surfaceFallback: Color,
    onBleedingChange: (String) -> Unit,
    onColorChange: (String) -> Unit,
    onPainChange: (Int) -> Unit,
    onClear: () -> Unit
) {
    val bleedingOptions = listOf("Heavy", "Medium", "Light", "Spotting")
    val colorOptions    = listOf("Bright Red", "Dark Red", "Brown")
    var expanded by remember { mutableStateOf(false) }

    val pastelGreen  = Color(0xFF6d9567).copy(alpha = 0.6f)
    val pastelOrange = Color(0xFFD89046)
    val pastelMaroon = Color(0xFF4E1A1A)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(pillBackground)
    ) {
        // Row header — always visible
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text       = label,
                    fontFamily = BricolageGrotesque,
                    fontWeight = FontWeight.SemiBold,
                    fontSize   = 13.sp,
                    color      = pillTextColor
                )
                Text(
                    text       = "$bleeding • $bloodColor • Pain: $painLevel/10",
                    fontFamily = BricolageGrotesque,
                    fontSize   = 11.sp,
                    color      = if (isCustom) accentColor else pillTextColor.copy(alpha = 0.45f)
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment     = Alignment.CenterVertically
            ) {
                if (isCustom) {
                    Text(
                        text      = "Reset",
                        fontFamily = BricolageGrotesque,
                        fontSize  = 11.sp,
                        color     = textSub,
                        modifier  = Modifier
                            .clip(RoundedCornerShape(50))
                            .clickable(onClick = onClear)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                Text(
                    text       = if (expanded) "▲" else "▼",
                    color      = pillTextColor.copy(alpha = 0.4f),
                    fontSize   = 11.sp,
                    fontFamily = BricolageGrotesque
                )
            }
        }

        // Expanded pickers
        if (expanded) {
            Column(
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Bleeding
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement   = Arrangement.spacedBy(6.dp)
                ) {
                    bleedingOptions.forEach { option ->
                        val isSelected = bleeding.equals(option, ignoreCase = true)
                        val activePillColor = when (option) {
                            "Heavy"  -> pastelMaroon
                            "Medium" -> pastelOrange
                            else     -> pastelGreen
                        }
                        EntryStylePill(
                            text         = option,
                            isSelected   = isSelected,
                            activeBg     = activePillColor,
                            activeText   = Color.White,
                            inactiveText = textSub,
                            surface      = surfaceFallback,
                            onClick      = { onBleedingChange(option) }
                        )
                    }
                }

                // Blood color
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement   = Arrangement.spacedBy(6.dp)
                ) {
                    colorOptions.forEach { option ->
                        val isSelected = bloodColor.equals(option, ignoreCase = true)
                        val activePillColor = when (option) {
                            "Bright Red" -> pastelGreen
                            "Dark Red"   -> Color(0xFF4E1A1A)
                            "Brown"      -> pastelOrange
                            else         -> accentColor
                        }
                        EntryStylePill(
                            text         = option,
                            isSelected   = isSelected,
                            activeBg     = activePillColor,
                            activeText   = Color.White,
                            inactiveText = textSub,
                            surface      = surfaceFallback,
                            onClick      = { onColorChange(option) }
                        )
                    }
                }

                // Daily Pain Slider
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Cramps & Pain", fontFamily = BricolageGrotesque, fontSize = 13.sp, color = textSub, fontWeight = FontWeight.SemiBold)
                        Text("$painLevel / 10", fontFamily = BricolageGrotesque, fontSize = 13.sp, color = accentColor, fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = painLevel.toFloat(),
                        onValueChange = { onPainChange(it.toInt()) },
                        valueRange = 0f..10f,
                        colors = SliderDefaults.colors(
                            thumbColor = accentColor,
                            activeTrackColor = accentColor,
                            inactiveTrackColor = surfaceFallback
                        )
                    )
                }
            }
        }
    }
}
/* ---------- Utils ---------- */

fun millisToLocalDate(millis: Long?): LocalDate? =
    millis?.let { Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate() }
