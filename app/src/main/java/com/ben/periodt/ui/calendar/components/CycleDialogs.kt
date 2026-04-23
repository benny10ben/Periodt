package com.ben.periodt.ui.calendar.components

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.EventAvailable
import androidx.compose.material.icons.rounded.Update
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ben.periodt.ui.theme.BricolageGrotesque
import com.ben.periodt.ui.theme.LocalAppIsDark
import com.ben.periodt.viewmodel.PeriodViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.round

private val SIZE_XS = 12.sp
private val SIZE_SM = 13.sp
private val SIZE_MD = 14.sp
private val SIZE_LG = 15.sp

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DayLogDialog(
    date: LocalDate,
    cycle: PeriodViewModel.Cycle,
    existingLog: PeriodViewModel.DailyLog?,
    onDismiss: () -> Unit,
    onSave: (bleeding: String, bloodColor: String, painLevel: Int) -> Unit,
    onClear: () -> Unit
) {
    val isDark = LocalAppIsDark.current

    val containerColor  = if (isDark) Color(0xFF1B1B1B) else Color.White
    val accentColor     = if (isDark) Color(0xFFD89046) else Color(0xFFa5bda3)
    val surfaceFallback = if (isDark) Color.Black else Color.Black.copy(alpha = 0.05f)
    val pillBackground  = if (isDark) Color.White.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.05f)
    val textPrimary     = if (isDark) Color.White else Color(0xFF0F172A)
    val textSub         = if (isDark) Color.White.copy(alpha = 0.6f) else Color(0xFF1b1b1b).copy(alpha = 0.6f)

    val pastelGreen  = if (isDark) Color(0xFF42553f) else Color(0xFFa5bda3)
    val pastelOrange = Color(0xFFa68e74)
    val pastelMaroon = Color(0xFF4E1A1A)

    val bleedingOptions = listOf("Heavy", "Medium", "Light", "Spotting")
    val colorOptions    = listOf("Bright Red", "Dark Red", "Brown")

    var bleeding      by remember { mutableStateOf(existingLog?.bleeding   ?: cycle.bleeding) }
    var bloodColor    by remember { mutableStateOf(existingLog?.bloodColor ?: cycle.bloodColor) }
    var painLevel     by remember { mutableIntStateOf(existingLog?.painLevel ?: cycle.painLevel) }
    var sliderPosition by remember { mutableFloatStateOf(painLevel.toFloat()) }

    val formatter    = remember { DateTimeFormatter.ofPattern("MMM d") }
    val dayOfWeek    = remember { date.dayOfWeek.getDisplayName(java.time.format.TextStyle.FULL, Locale.getDefault()) }

    val isOverridden = existingLog != null ||
            bleeding != cycle.bleeding ||
            bloodColor != cycle.bloodColor ||
            painLevel != cycle.painLevel

    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()

    val thresholdPx = remember(configuration.screenHeightDp) {
        with(density) { (configuration.screenHeightDp.dp * 0.20f).toPx() }
    }
    var expandedOffset by remember { mutableFloatStateOf(0f) }

    class SheetStateHolder { var state: SheetState? = null }
    val sheetHolder = remember { SheetStateHolder() }

    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { targetValue ->
            if (targetValue == SheetValue.Hidden) {
                try {
                    val currentState = sheetHolder.state
                    if (currentState != null) {
                        val currentOffset = currentState.requireOffset()
                        val dragDistance = currentOffset - expandedOffset
                        dragDistance <= 10f || dragDistance >= thresholdPx
                    } else true
                } catch (e: Exception) { true }
            } else true
        }
    )
    sheetHolder.state = sheetState

    LaunchedEffect(sheetState.currentValue) {
        if (sheetState.currentValue == SheetValue.Expanded) {
            try { expandedOffset = sheetState.requireOffset() } catch (e: Exception) {}
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = sheetState,
        containerColor   = containerColor,
        modifier         = Modifier.windowInsetsPadding(WindowInsets.statusBars)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
                .padding(bottom = 16.dp)
                .animateContentSize(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness    = Spring.StiffnessMediumLow
                    )
                )
        ) {
            Row(
                modifier              = Modifier.fillMaxWidth().padding(bottom = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text       = "$dayOfWeek, ${date.format(formatter)}",
                        fontFamily = BricolageGrotesque,
                        style      = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                        color      = textPrimary
                    )
                    Text(
                        text       = if (isOverridden) "Custom log active" else "Using cycle default",
                        fontFamily = BricolageGrotesque,
                        fontSize   = SIZE_XS,
                        color      = if (isOverridden) accentColor else textSub
                    )
                }
                Box(
                    modifier = Modifier.size(32.dp).clip(CircleShape).background(textSub.copy(alpha = 0.1f))
                        .clickable { coroutineScope.launch { sheetState.hide(); onDismiss() } },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Close, null, tint = textPrimary, modifier = Modifier.size(18.dp))
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text       = "Flow Intensity",
                        fontFamily = BricolageGrotesque,
                        fontWeight = FontWeight.SemiBold,
                        fontSize   = SIZE_MD,
                        color      = textPrimary
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        bleedingOptions.forEach { option ->
                            val isSelected      = bleeding.equals(option, ignoreCase = true)
                            val activePillColor = when (option) { "Heavy" -> pastelMaroon; "Medium" -> pastelOrange; else -> pastelGreen }
                            EntryStylePill(option, isSelected, activePillColor, Color.White, textSub, surfaceFallback) { bleeding = option }
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text       = "Blood Color",
                        fontFamily = BricolageGrotesque,
                        fontWeight = FontWeight.SemiBold,
                        fontSize   = SIZE_MD,
                        color      = textPrimary
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        colorOptions.forEach { option ->
                            val isSelected      = bloodColor.equals(option, ignoreCase = true)
                            val activePillColor = when (option) { "Bright Red" -> pastelGreen; "Dark Red" -> Color(0xFF4E1A1A); "Brown" -> pastelOrange; else -> accentColor }
                            EntryStylePill(option, isSelected, activePillColor, Color.White, textSub, surfaceFallback) { bloodColor = option }
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text       = "Cramps & Pain",
                            fontFamily = BricolageGrotesque,
                            fontWeight = FontWeight.SemiBold,
                            fontSize   = SIZE_MD,
                            color      = textPrimary
                        )
                        Text(
                            text       = "$painLevel / 10",
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

            Column(
                modifier = Modifier.padding(top = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick   = {
                        coroutineScope.launch {
                            sheetState.hide()
                            onSave(bleeding, bloodColor, painLevel)
                        }
                    },
                    modifier  = Modifier.fillMaxWidth().height(56.dp),
                    shape     = RoundedCornerShape(18.dp),
                    colors    = ButtonDefaults.buttonColors(containerColor = accentColor, contentColor = Color.White),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                ) {
                    Text(
                        text       = "Save Day Log",
                        fontFamily = BricolageGrotesque,
                        fontWeight = FontWeight.Bold,
                        fontSize   = SIZE_LG
                    )
                }

                if (isOverridden) {
                    TextButton(
                        onClick = {
                            coroutineScope.launch {
                                sheetState.hide()
                                onClear()
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text       = "Reset to cycle default",
                            fontFamily = BricolageGrotesque,
                            color      = textSub,
                            fontSize   = SIZE_MD
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EditCycleDialog(
    cycle: PeriodViewModel.Cycle,
    onDismiss: () -> Unit,
    onSave: (PeriodViewModel.Cycle, Map<LocalDate, Triple<String, String, Int>>) -> Unit,
    existingDailyLogs: List<PeriodViewModel.DailyLog> = emptyList()
) {
    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker   by remember { mutableStateOf(false) }
    var startDate by remember { mutableStateOf(cycle.startDate) }
    var endDate   by remember { mutableStateOf(cycle.endDate) }
    var bleeding  by remember { mutableStateOf(cycle.bleeding) }
    var bloodColor by remember { mutableStateOf(cycle.bloodColor) }
    var sliderPosition by remember { mutableStateOf(cycle.painLevel.toFloat()) }
    var painLevel by remember { mutableIntStateOf(cycle.painLevel) }

    val bleedingOptions = listOf("Heavy", "Medium", "Light", "Spotting")
    val colorOptions    = listOf("Bright Red", "Dark Red", "Brown")

    var showDailyLog       by remember { mutableStateOf(false) }
    var selectedDayForLog  by remember { mutableStateOf<LocalDate?>(null) }
    var dailyOverrides by remember {
        mutableStateOf<Map<LocalDate, Triple<String, String, Int>>>(
            existingDailyLogs.associate { log -> log.date to Triple(log.bleeding, log.bloodColor, log.painLevel) }
        )
    }
    val dailyLogRotation by animateFloatAsState(if (showDailyLog) 180f else 0f, label = "dailyLogRotation")

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
        round(cycleDays.sumOf { day -> dailyOverrides[day]?.third ?: painLevel }.toFloat() / cycleDays.size).toInt()
    }

    val isDark = LocalAppIsDark.current
    val containerColor  = if (isDark) Color(0xFF1B1B1B) else Color.White
    val accentColor     = if (isDark) Color(0xFFD89046) else Color(0xFFa5bda3)
    val surfaceFallback = if (isDark) Color.Black else Color.Black.copy(alpha = 0.05f)
    val pastelGreen     = if (isDark) Color(0xFF42553f) else Color(0xFFa5bda3)
    val pastelOrange    = Color(0xFFD89046)
    val pastelMaroon    = Color(0xFF4E1A1A)
    val pillBackground  = if (isDark) Color.White.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.05f)
    val pillTextColor   = if (isDark) Color.White else Color(0xFF1B1B1B)
    val textPrimary     = if (isDark) Color.White else Color(0xFF0F172A)
    val textSub         = if (isDark) Color.White.copy(alpha = 0.6f) else Color(0xFF1B1B1B)

    val formatter  = remember { DateTimeFormatter.ofPattern("MMM dd") }

    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()

    val scrollState = rememberScrollState()

    val thresholdPx = remember(configuration.screenHeightDp) {
        with(density) { (configuration.screenHeightDp.dp * 0.20f).toPx() }
    }
    var expandedOffset by remember { mutableFloatStateOf(0f) }

    class SheetStateHolder { var state: SheetState? = null }
    val sheetHolder = remember { SheetStateHolder() }

    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { targetValue ->
            if (targetValue == SheetValue.Hidden) {
                try {
                    val currentState = sheetHolder.state
                    if (currentState != null) {
                        val currentOffset = currentState.requireOffset()
                        val dragDistance = currentOffset - expandedOffset
                        dragDistance <= 10f || dragDistance >= thresholdPx
                    } else true
                } catch (e: Exception) { true }
            } else true
        }
    )
    sheetHolder.state = sheetState

    LaunchedEffect(sheetState.currentValue) {
        if (sheetState.currentValue == SheetValue.Expanded) {
            try { expandedOffset = sheetState.requireOffset() } catch (e: Exception) {}
        }
    }

    LaunchedEffect(showDailyLog) {
        if (showDailyLog) {
            delay(150)
            scrollState.animateScrollTo(
                value = scrollState.maxValue,
                animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing)
            )
        }
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
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
                .padding(bottom = 16.dp)
                .animateContentSize(spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMediumLow))
        ) {
            Row(
                modifier              = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Text(
                    text       = "Edit Entry",
                    fontFamily = BricolageGrotesque,
                    style      = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color      = textPrimary
                )
                Box(
                    modifier         = Modifier.size(32.dp).clip(CircleShape).background(textSub.copy(alpha = 0.1f))
                        .clickable { coroutineScope.launch { sheetState.hide(); onDismiss() } },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Close, "Close", tint = textPrimary, modifier = Modifier.size(18.dp))
                }
            }

            Column(
                modifier = Modifier.weight(1f, fill = false).verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    CleanDateCard("Start Date", startDate.format(formatter), Icons.Rounded.CalendarToday, pillBackground, pillTextColor, { showStartPicker = true }, Modifier.weight(1f))
                    CleanDateCard("End Date", endDate?.format(formatter) ?: "Ongoing", if (endDate == null) Icons.Rounded.Update else Icons.Rounded.EventAvailable, pillBackground, pillTextColor, { showEndPicker = true }, Modifier.weight(1f))
                }

                if (dailyOverrides.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Summary", fontFamily = BricolageGrotesque, fontWeight = FontWeight.SemiBold, fontSize = SIZE_MD, color = textPrimary)
                        val annotatedSummary = buildAnnotatedString {
                            append("Based on your daily logs, this cycle has a peak flow of ")
                            pushStyle(SpanStyle(fontWeight = FontWeight.Bold, color = textPrimary))
                            append("${derivedBleeding.lowercase()} (${derivedColor.lowercase()})")
                            pop()
                            append(", with an average pain level of ")
                            pushStyle(SpanStyle(fontWeight = FontWeight.Bold, color = textPrimary))
                            append("$derivedPain/10")
                            pop()
                            append(".")
                        }
                        Text(text = annotatedSummary, fontFamily = BricolageGrotesque, fontSize = SIZE_MD, color = textSub, lineHeight = 20.sp)
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
                            Text("${sliderPosition.toInt()} / 10", fontFamily = BricolageGrotesque, fontWeight = FontWeight.Bold, fontSize = SIZE_MD, color = accentColor)
                        }
                        Slider(
                            value         = sliderPosition,
                            onValueChange = { sliderPosition = it; painLevel = it.toInt() },
                            valueRange    = 0f..10f,
                            colors        = SliderDefaults.colors(thumbColor = accentColor, activeTrackColor = accentColor, inactiveTrackColor = pillBackground)
                        )
                    }
                }

                if (cycleDays.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(pillBackground).clickable { showDailyLog = !showDailyLog }.padding(16.dp, 14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment     = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Log by day", fontFamily = BricolageGrotesque, fontWeight = FontWeight.SemiBold, fontSize = SIZE_LG, color = pillTextColor)
                                Text(if (dailyOverrides.isEmpty()) "Optional • uses cycle default" else "${dailyOverrides.size} day${if (dailyOverrides.size > 1) "s" else ""} customized", fontFamily = BricolageGrotesque, fontSize = SIZE_XS, color = if (dailyOverrides.isEmpty()) pillTextColor.copy(alpha = 0.5f) else accentColor)
                            }
                            Icon(Icons.Default.KeyboardArrowDown, null, tint = pillTextColor.copy(alpha = 0.5f), modifier = Modifier.size(20.dp).graphicsLayer { rotationZ = dailyLogRotation })
                        }

                        if (showDailyLog) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                cycleDays.forEach { day ->
                                    val override = dailyOverrides[day]
                                    val isCustom = override != null
                                    Row(
                                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(if (isCustom) accentColor.copy(alpha = 0.12f) else pillBackground).clickable { selectedDayForLog = day }.padding(horizontal = 16.dp, vertical = 12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment     = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(text = day.format(DateTimeFormatter.ofPattern("EEE, MMM d")), fontFamily = BricolageGrotesque, fontWeight = FontWeight.SemiBold, fontSize = SIZE_LG, color = if (isCustom) textPrimary else textPrimary.copy(alpha = 0.8f))
                                            Spacer(Modifier.height(2.dp))
                                            Text(text = "${override?.first ?: bleeding} • ${override?.second ?: bloodColor} • Pain: ${override?.third ?: painLevel}/10", fontFamily = BricolageGrotesque, fontSize = SIZE_XS, color = textSub.copy(alpha = 0.6f))
                                        }
                                        Icon(Icons.Default.Edit, "Edit", tint = if (isCustom) accentColor else textSub.copy(alpha = 0.3f), modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Button(
                onClick   = { coroutineScope.launch { sheetState.hide(); onSave(cycle.copy(startDate = startDate, endDate = endDate, bleeding = derivedBleeding, bloodColor = derivedColor, painLevel = derivedPain), dailyOverrides) } },
                modifier  = Modifier.fillMaxWidth().padding(top = 24.dp).height(56.dp),
                shape     = RoundedCornerShape(18.dp),
                colors    = ButtonDefaults.buttonColors(containerColor = accentColor, contentColor = Color.White),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
            ) {
                Text("Update Entry", fontFamily = BricolageGrotesque, fontSize = SIZE_LG, fontWeight = FontWeight.Bold)
            }
        }
    }

    selectedDayForLog?.let { day ->
        val override   = dailyOverrides[day]
        val existingLog = override?.let {
            PeriodViewModel.DailyLog(id = 0, cycleId = cycle.id, date = day, bleeding = it.first, bloodColor = it.second, painLevel = it.third)
        }
        val tempCycle = cycle.copy(startDate = startDate, endDate = endDate, bleeding = bleeding, bloodColor = bloodColor, painLevel = painLevel)

        DayLogDialog(
            date      = day, cycle = tempCycle, existingLog = existingLog,
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
        // Assume MinimalDatePickerDialog is available via imports (from com.ben.periodt.ui.components or similar)
        // Ensure you import it at the top if needed, based on your app's structure.
        MinimalDatePickerDialog(
            title = "Start Date",
            brand = accentColor,
            gradTop = pastelGreen,
            gradMid = pastelOrange,
            gradBottom = pastelMaroon,
            onGradient = Color.White,
            buttonContainer = surfaceFallback,
            buttonContent = textPrimary,
            onDismiss = { showStartPicker = false },
            onConfirm = { ms ->
                millisToLocalDate(ms)?.let { startDate = it }; showStartPicker = false
            }
        )
    }
    if (showEndPicker) {
        MinimalDatePickerDialog(
            title = "End Date",
            brand = accentColor,
            gradTop = pastelGreen,
            gradMid = pastelOrange,
            gradBottom = pastelMaroon,
            onGradient = Color.White,
            buttonContainer = surfaceFallback,
            buttonContent = textPrimary,
            onDismiss = { showEndPicker = false },
            onConfirm = { ms -> millisToLocalDate(ms)?.let { endDate = it }; showEndPicker = false }
        )
    }
}

@Composable
fun EntryStylePill(
    text: String, isSelected: Boolean, activeBg: Color, activeText: Color,
    inactiveText: Color, surface: Color, onClick: () -> Unit
) {
    Box(
        modifier = Modifier.clip(RoundedCornerShape(50)).background(if (isSelected) activeBg else surface).clickable(onClick = onClick).padding(horizontal = 18.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text       = text,
            fontFamily = BricolageGrotesque,
            fontSize   = SIZE_SM,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
            color      = if (isSelected) activeText else inactiveText
        )
    }
}

@Composable
fun CleanDateCard(
    label: String, date: String, icon: ImageVector,
    bg: Color, textColor: Color, onClick: () -> Unit, modifier: Modifier = Modifier
) {
    Column(modifier = modifier.clip(RoundedCornerShape(20.dp)).background(bg).clickable(onClick = onClick).padding(16.dp)) {
        Text(
            text       = label,
            fontFamily = BricolageGrotesque,
            fontSize   = SIZE_XS,
            color      = textColor.copy(alpha = 0.7f),
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text(
                text       = date,
                fontFamily = BricolageGrotesque,
                fontSize   = SIZE_LG,
                fontWeight = FontWeight.SemiBold,
                color      = textColor
            )
            Icon(imageVector = icon, contentDescription = null, tint = textColor.copy(alpha = 0.6f), modifier = Modifier.size(16.dp))
        }
    }
}