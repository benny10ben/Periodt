package com.ben.periodt.uiux.calendar

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
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
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.ui.graphics.graphicsLayer

private val SIZE_XXS = 11.sp
private val SIZE_XS  = 12.sp
private val SIZE_MD  = 14.sp
private val SIZE_LG  = 15.sp

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddCycleDialog(
    onDismiss: () -> Unit,
    onSave: (LocalDate, LocalDate?, String, String, Int, Map<LocalDate, Triple<String, String, Int>>) -> Unit
) {
    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker   by remember { mutableStateOf(false) }
    var startDate  by remember { mutableStateOf(LocalDate.now()) }
    var endDate    by remember { mutableStateOf<LocalDate?>(null) }
    var bleeding   by remember { mutableStateOf("Medium") }
    var bloodColor by remember { mutableStateOf("Bright Red") }
    var sliderPosition by remember { mutableStateOf(5f) }
    var painLevel  by remember { mutableIntStateOf(5) }

    val bleedingOptions = listOf("Heavy", "Medium", "Light", "Spotting")
    val colorOptions    = listOf("Bright Red", "Dark Red", "Brown")

    var showDailyLog      by remember { mutableStateOf(false) }
    var selectedDayForLog by remember { mutableStateOf<LocalDate?>(null) }
    var dailyOverrides    by remember { mutableStateOf<Map<LocalDate, Triple<String, String, Int>>>(emptyMap()) }
    val dailyLogRotation  by animateFloatAsState(if (showDailyLog) 180f else 0f, label = "dailyLogRotation")

    val today = LocalDate.now()
    val cycleDays = remember(startDate, endDate) {
        val end = endDate ?: if (startDate.isBefore(today)) today else startDate
        generateSequence(startDate) { it.plusDays(1) }.takeWhile { !it.isAfter(end) }.toList()
    }

    val flowWeights        = remember { mapOf("Spotting" to 1, "Light" to 2, "Medium" to 3, "Heavy" to 4) }
    val reverseFlowWeights = remember { mapOf(1 to "Spotting", 2 to "Light", 3 to "Medium", 4 to "Heavy") }

    val derivedBleeding = remember(dailyOverrides, bleeding, cycleDays) {
        if (dailyOverrides.isEmpty() || cycleDays.isEmpty()) return@remember bleeding
        val maxWeight = cycleDays.maxOfOrNull { day -> flowWeights[dailyOverrides[day]?.first ?: bleeding] ?: 0 } ?: 0
        reverseFlowWeights[maxWeight] ?: bleeding
    }
    val derivedColor = remember(dailyOverrides, bloodColor, cycleDays) {
        if (dailyOverrides.isEmpty() || cycleDays.isEmpty()) return@remember bloodColor
        cycleDays.map { day -> dailyOverrides[day]?.second ?: bloodColor }.groupingBy { it }.eachCount().maxByOrNull { it.value }?.key ?: bloodColor
    }
    val derivedPain = remember(dailyOverrides, painLevel, cycleDays) {
        if (dailyOverrides.isEmpty() || cycleDays.isEmpty()) return@remember painLevel
        kotlin.math.round(cycleDays.sumOf { day -> dailyOverrides[day]?.third ?: painLevel }.toFloat() / cycleDays.size).toInt()
    }

    val isDark = LocalAppIsDark.current
    val containerColor  = if (isDark) Color(0xFF1B1B1B) else Color.White
    val accentColor     = if (isDark) Color(0xFFD89046) else Color(0xFF6d9567).copy(alpha = 0.6f)
    val surfaceFallback = if (isDark) Color.Black else Color.Black.copy(alpha = 0.05f)
    val pastelGreen     = Color(0xFF6d9567).copy(alpha = 0.6f)
    val pastelOrange    = Color(0xFFD89046)
    val pastelMaroon    = Color(0xFF4E1A1A)
    val pillBackground  = if (isDark) Color.White.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.05f)
    val pillTextColor   = if (isDark) Color.White else Color(0xFF1B1B1B)
    val textPrimary     = if (isDark) Color.White else Color(0xFF0F172A)
    val textSub         = if (isDark) Color.White.copy(alpha = 0.6f) else Color(0xFF1B1B1B)

    val formatter  = remember { DateTimeFormatter.ofPattern("MMM dd") }
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
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Header
            Row(
                modifier              = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Text(
                    text       = "Log Cycle",
                    fontFamily = BricolageGrotesque,
                    style      = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color      = textPrimary
                )
                Box(
                    modifier         = Modifier.size(32.dp).clip(CircleShape).background(textSub.copy(alpha = 0.1f)).clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Close, "Close", tint = textPrimary, modifier = Modifier.size(18.dp))
                }
            }

            // Date selectors
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CleanDateCard("Start Date", startDate.format(formatter), Icons.Rounded.CalendarToday, pillBackground, pillTextColor, { showStartPicker = true }, Modifier.weight(1f))
                CleanDateCard("End Date", endDate?.format(formatter) ?: "Ongoing", if (endDate == null) Icons.Rounded.Update else Icons.Rounded.EventAvailable, pillBackground, pillTextColor, { showEndPicker = true }, Modifier.weight(1f))
            }

            // Summary or pickers
            if (dailyOverrides.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Summary",
                        fontFamily = BricolageGrotesque,
                        fontWeight = FontWeight.SemiBold,
                        fontSize   = SIZE_MD,
                        color      = textPrimary
                    )
                    val annotatedSummary = androidx.compose.ui.text.buildAnnotatedString {
                        append("Based on your daily logs, this cycle has a peak flow of ")
                        pushStyle(androidx.compose.ui.text.SpanStyle(fontWeight = FontWeight.Bold, color = textPrimary))
                        append("${derivedBleeding.lowercase()} (${derivedColor.lowercase()})")
                        pop()
                        append(", with an average pain level of ")
                        pushStyle(androidx.compose.ui.text.SpanStyle(fontWeight = FontWeight.Bold, color = textPrimary))
                        append("$derivedPain/10")
                        pop()
                        append(".")
                    }
                    Text(
                        text       = annotatedSummary,
                        fontFamily = BricolageGrotesque,
                        fontSize   = SIZE_MD,
                        color      = textSub,
                        lineHeight = 20.sp
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Flow Intensity", fontFamily = BricolageGrotesque, fontWeight = FontWeight.SemiBold, fontSize = SIZE_MD, color = textPrimary)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        bleedingOptions.forEach { option ->
                            val isSelected = bleeding.equals(option, ignoreCase = true)
                            val activeBg   = when (option) { "Heavy" -> pastelMaroon; "Medium" -> pastelOrange; else -> pastelGreen }
                            EntryStylePill(option, isSelected, activeBg, Color.White, textSub, surfaceFallback) { bleeding = option }
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Blood Color", fontFamily = BricolageGrotesque, fontWeight = FontWeight.SemiBold, fontSize = SIZE_MD, color = textPrimary)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        colorOptions.forEach { option ->
                            val isSelected = bloodColor.equals(option, ignoreCase = true)
                            val activeBg   = when (option) { "Bright Red" -> pastelGreen; "Dark Red" -> Color(0xFF4E1A1A); "Brown" -> pastelOrange; else -> accentColor }
                            EntryStylePill(option, isSelected, activeBg, Color.White, textSub, surfaceFallback) { bloodColor = option }
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Cramps & Pain", fontFamily = BricolageGrotesque, fontWeight = FontWeight.SemiBold, fontSize = SIZE_MD, color = textPrimary)
                        Text(
                            "${sliderPosition.toInt()} / 10",
                            fontFamily = BricolageGrotesque,
                            fontWeight = FontWeight.Bold,
                            fontSize   = SIZE_MD,
                            color      = accentColor
                        )
                    }
                    Slider(
                        value         = sliderPosition,
                        onValueChange = { sliderPosition = it; painLevel = it.toInt() },
                        valueRange    = 0f..10f,
                        colors        = SliderDefaults.colors(thumbColor = accentColor, activeTrackColor = accentColor, inactiveTrackColor = pillBackground)
                    )
                }
            }

            // Daily log section
            if (cycleDays.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(pillBackground)
                            .clickable { showDailyLog = !showDailyLog }
                            .padding(16.dp, 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                "Log by day",
                                fontFamily = BricolageGrotesque,
                                fontWeight = FontWeight.SemiBold,
                                fontSize   = SIZE_LG,
                                color      = pillTextColor
                            )
                            Text(
                                if (dailyOverrides.isEmpty()) "Optional • uses cycle default"
                                else "${dailyOverrides.size} day${if (dailyOverrides.size > 1) "s" else ""} customized",
                                fontFamily = BricolageGrotesque,
                                fontSize   = SIZE_XS,
                                color      = if (dailyOverrides.isEmpty()) pillTextColor.copy(alpha = 0.5f) else accentColor
                            )
                        }
                        Icon(
                            Icons.Default.KeyboardArrowDown, null,
                            tint     = pillTextColor.copy(alpha = 0.5f),
                            modifier = Modifier.size(20.dp).graphicsLayer { rotationZ = dailyLogRotation }
                        )
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
                                    verticalAlignment     = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text       = dayLabel,
                                            fontFamily = BricolageGrotesque,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize   = SIZE_LG,
                                            color      = if (isCustom) textPrimary else textPrimary.copy(alpha = 0.8f)
                                        )
                                        Spacer(Modifier.height(2.dp))
                                        Text(
                                            text       = "$dayBleeding • $dayColor • Pain: $dayPain/10",
                                            fontFamily = BricolageGrotesque,
                                            fontSize   = SIZE_XS,
                                            color      = textSub.copy(alpha = 0.6f)
                                        )
                                    }
                                    Icon(Icons.Default.Edit, "Edit", tint = if (isCustom) accentColor else textSub.copy(alpha = 0.3f), modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            }

            Button(
                onClick   = { onSave(startDate, endDate, derivedBleeding, derivedColor, derivedPain, dailyOverrides) },
                modifier  = Modifier.fillMaxWidth().padding(top = 8.dp).height(56.dp),
                shape     = RoundedCornerShape(18.dp),
                colors    = ButtonDefaults.buttonColors(containerColor = accentColor, contentColor = Color.White),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
            ) {
                Text(
                    "Save Entry",
                    fontFamily = BricolageGrotesque,
                    fontSize   = SIZE_LG,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }

    // Nested day log dialog
    selectedDayForLog?.let { day ->
        val override    = dailyOverrides[day]
        val existingLog = override?.let {
            PeriodViewModel.DailyLog(id = 0, cycleId = 0, date = day, bleeding = it.first, bloodColor = it.second, painLevel = it.third)
        }
        val tempCycle = PeriodViewModel.Cycle(id = 0, startDate = startDate, endDate = endDate, bleeding = bleeding, bloodColor = bloodColor, painLevel = painLevel)

        com.ben.periodt.uiux.calendar.DayLogDialog(
            date      = day,
            cycle     = tempCycle,
            existingLog = existingLog,
            onDismiss = { selectedDayForLog = null },
            onSave    = { newBleeding, newColor, newPain ->
                dailyOverrides = dailyOverrides.toMutableMap().apply { put(day, Triple(newBleeding, newColor, newPain)) }
                selectedDayForLog = null
            },
            onClear   = {
                dailyOverrides = dailyOverrides.toMutableMap().apply { remove(day) }
                selectedDayForLog = null
            }
        )
    }

    if (showStartPicker) {
        MinimalDatePickerDialog(
            title = "Start Date", brand = accentColor,
            gradTop = pastelGreen, gradMid = pastelOrange, gradBottom = pastelMaroon,
            onGradient = Color.White, buttonContainer = surfaceFallback, buttonContent = textPrimary,
            onDismiss = { showStartPicker = false },
            onConfirm = { ms -> millisToLocalDate(ms)?.let { startDate = it }; showStartPicker = false }
        )
    }
    if (showEndPicker) {
        MinimalDatePickerDialog(
            title = "End Date", brand = accentColor,
            gradTop = pastelGreen, gradMid = pastelOrange, gradBottom = pastelMaroon,
            onGradient = Color.White, buttonContainer = surfaceFallback, buttonContent = textPrimary,
            onDismiss = { showEndPicker = false },
            onConfirm = { ms -> millisToLocalDate(ms)?.let { endDate = it }; showEndPicker = false }
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
    val zone   = remember { java.time.ZoneId.systemDefault() }

    var selectedMillis by remember {
        mutableStateOf(java.time.Instant.now().atZone(zone).toLocalDate().atStartOfDay(zone).toInstant().toEpochMilli())
    }
    var displayedYm by remember {
        mutableStateOf(java.time.Instant.ofEpochMilli(selectedMillis).atZone(zone).toLocalDate().let { d -> java.time.YearMonth.of(d.year, d.month) })
    }

    val dateFormatter = remember { java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy") }
    var manualDateText by remember {
        mutableStateOf(java.time.Instant.ofEpochMilli(selectedMillis).atZone(zone).toLocalDate().format(dateFormatter))
    }
    var isDateError by remember { mutableStateOf(false) }

    val containerColor = if (isDark) Color(0xFF1B1B1B) else Color.White
    val textPrimary    = if (isDark) Color.White else Color(0xFF1B1B1B)
    val textSub        = if (isDark) Color.White.copy(alpha = 0.6f) else Color(0xFF1b1b1b).copy(alpha = 0.6f)
    val accentColor    = if (isDark) Color(0xFFD89046) else Color(0xFF6d9567).copy(alpha = 0.6f)

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
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp)
                .animateContentSize(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness    = Spring.StiffnessMediumLow
                    )
                )
        ) {
            // Header
            Row(
                modifier              = Modifier.fillMaxWidth().padding(vertical = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Text(
                    text       = title,
                    fontFamily = BricolageGrotesque,
                    style      = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color      = textPrimary
                )
                Box(
                    modifier         = Modifier.size(32.dp).clip(CircleShape).background(textSub.copy(alpha = 0.1f)).clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Close, "Close", tint = textPrimary, modifier = Modifier.size(18.dp))
                }
            }

            OutlinedTextField(
                value         = manualDateText,
                onValueChange = { newVal ->
                    manualDateText = newVal
                    try {
                        val parsedDate = java.time.LocalDate.parse(newVal, dateFormatter)
                        selectedMillis = parsedDate.atStartOfDay(zone).toInstant().toEpochMilli()
                        displayedYm    = java.time.YearMonth.of(parsedDate.year, parsedDate.month)
                        isDateError    = false
                    } catch (e: Exception) { isDateError = true }
                },
                label         = {
                    Text(
                        "Date (DD/MM/YYYY)",
                        fontFamily = BricolageGrotesque,
                        fontSize   = SIZE_XS
                    )
                },
                isError       = isDateError,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                modifier      = Modifier.fillMaxWidth(),
                textStyle = LocalTextStyle.current.copy(
                    fontFamily = BricolageGrotesque,
                    fontSize = SIZE_LG,
                    color = textPrimary
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor     = textPrimary,
                    unfocusedTextColor   = textPrimary,
                    focusedLabelColor    = accentColor,
                    unfocusedLabelColor  = textSub,
                    focusedBorderColor   = accentColor,
                    unfocusedBorderColor = textSub.copy(alpha = 0.5f),
                    cursorColor          = accentColor,
                    errorBorderColor     = Color(0xFFEF5350),
                    selectionColors = androidx.compose.foundation.text.selection.TextSelectionColors(
                        handleColor = accentColor,
                        backgroundColor = accentColor.copy(alpha = 0.3f)
                    )
                ),
                singleLine = true
            )

            if (isDateError) {
                Text(
                    text       = "Invalid format. Use DD/MM/YYYY",
                    color      = Color(0xFFEF5350),
                    fontSize   = SIZE_XS,
                    fontFamily = BricolageGrotesque,
                    modifier   = Modifier.padding(top = 4.dp, start = 16.dp)
                )
            }

            Spacer(Modifier.height(24.dp))

            MinimalMonthPicker(
                displayedYm         = displayedYm,
                selectedMillis      = selectedMillis,
                onDisplayedYmChange = { displayedYm = it },
                onSelect            = { ms ->
                    selectedMillis = ms
                    manualDateText = java.time.Instant.ofEpochMilli(ms).atZone(zone).toLocalDate().format(dateFormatter)
                    isDateError    = false
                },
                textColor           = textPrimary,
                accentColor         = accentColor,
                weekStartsOnMonday  = false
            )

            Spacer(Modifier.height(32.dp))

            Button(
                onClick   = { if (!isDateError) onConfirm(selectedMillis) },
                modifier  = Modifier.fillMaxWidth().height(56.dp),
                shape     = RoundedCornerShape(18.dp),
                colors    = ButtonDefaults.buttonColors(
                    containerColor = if (isDateError) textSub.copy(alpha = 0.3f) else accentColor,
                    contentColor   = Color.White
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                enabled   = !isDateError
            ) {
                Text(
                    "Confirm Date",
                    fontFamily = BricolageGrotesque,
                    fontWeight = FontWeight.Bold,
                    fontSize   = SIZE_LG
                )
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
    val zone   = remember { ZoneId.systemDefault() }
    val today  = remember { LocalDate.now(zone) }
    val selectedDate = selectedMillis?.let { Instant.ofEpochMilli(it).atZone(zone).toLocalDate() }

    fun weekdayIndex(d: LocalDate): Int {
        val iso = d.dayOfWeek.value
        return if (weekStartsOnMonday) iso - 1 else (iso % 7)
    }

    val firstOfMonth  = displayedYm.atDay(1)
    val daysInMonth   = displayedYm.lengthOfMonth()
    val leadingBlanks = weekdayIndex(firstOfMonth)
    val totalCells    = leadingBlanks + daysInMonth
    val rows          = ceil(totalCells / 7f).toInt()

    // Month/year header + nav
    Row(
        modifier              = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically
    ) {
        Text(
            text       = "${displayedYm.month.name.lowercase().replaceFirstChar { it.uppercase() }} ${displayedYm.year}",
            fontFamily = BricolageGrotesque,
            fontWeight = FontWeight.Bold,
            fontSize   = SIZE_MD,
            color      = textColor
        )
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            IconButton(onClick = { onDisplayedYmChange(displayedYm.minusMonths(1)) }, modifier = Modifier.size(32.dp)) {
                Text("‹", fontFamily = BricolageGrotesque, color = textColor.copy(alpha = 0.7f), fontSize = 24.sp)
            }
            IconButton(onClick = { onDisplayedYmChange(displayedYm.plusMonths(1)) }, modifier = Modifier.size(32.dp)) {
                Text("›", fontFamily = BricolageGrotesque, color = textColor.copy(alpha = 0.7f), fontSize = 24.sp)
            }
        }
    }

    Spacer(Modifier.height(12.dp))

    // Day-of-week headers
    val labels = if (weekStartsOnMonday) listOf("MON","TUE","WED","THU","FRI","SAT","SUN")
    else listOf("SUN","MON","TUE","WED","THU","FRI","SAT")

    Row(Modifier.fillMaxWidth()) {
        labels.forEach { label ->
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Text(
                    text       = label,
                    fontFamily = BricolageGrotesque,
                    fontSize   = SIZE_XXS,
                    color      = textColor.copy(alpha = 0.5f),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }

    Spacer(Modifier.height(8.dp))

    val todayChipBg      = textColor.copy(alpha = 0.1f)
    val selectedChipBg   = accentColor
    val selectedChipText = if (isDark) Color.Black else Color.White

    var day = 1
    repeat(rows) { r ->
        Row(Modifier.fillMaxWidth()) {
            repeat(7) { c ->
                val idx = r * 7 + c
                Box(modifier = Modifier.weight(1f).aspectRatio(1f), contentAlignment = Alignment.Center) {
                    if (idx >= leadingBlanks && day <= daysInMonth) {
                        val date       = displayedYm.atDay(day)
                        val isToday    = date == today
                        val isSelected = date == selectedDate
                        val dayTextColor = if (isSelected) selectedChipText else textColor
                        val click = { val ms = date.atStartOfDay(zone).toInstant().toEpochMilli(); onSelect(ms) }

                        val chipBg = when { isSelected -> selectedChipBg; isToday -> todayChipBg; else -> Color.Transparent }
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(chipBg)
                                .clickable(onClick = click),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text       = "$day",
                                fontFamily = BricolageGrotesque,
                                color      = dayTextColor,
                                fontSize   = SIZE_MD,
                                fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                        day++
                    }
                }
            }
        }
        Spacer(Modifier.height(2.dp))
    }
}

/* ---------- Utils ---------- */
fun millisToLocalDate(millis: Long?): LocalDate? =
    millis?.let { Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate() }