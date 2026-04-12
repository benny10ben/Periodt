package com.ben.periodt.uiux.calendar

import android.app.Application
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material.icons.rounded.DirectionsRun
import androidx.compose.material.icons.rounded.DirectionsWalk
import androidx.compose.material.icons.rounded.EventAvailable
import androidx.compose.material.icons.rounded.EventRepeat
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Grain
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.LocalCafe
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.SelfImprovement
import androidx.compose.material.icons.rounded.SoupKitchen
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material.icons.rounded.Tapas
import androidx.compose.material.icons.rounded.Update
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ben.periodt.ui.theme.BricolageGrotesque
import com.ben.periodt.uiux.shared.PostPillState
import com.ben.periodt.uiux.shared.Prediction
import com.ben.periodt.uiux.shared.getPostPillState
import com.ben.periodt.uiux.shared.pretty
import com.ben.periodt.viewmodel.PeriodViewModel
import com.kizitonwose.calendar.compose.CalendarState
import com.kizitonwose.calendar.compose.HorizontalCalendar
import com.kizitonwose.calendar.compose.WeekCalendar
import com.kizitonwose.calendar.compose.rememberCalendarState
import com.kizitonwose.calendar.compose.weekcalendar.WeekCalendarState
import com.kizitonwose.calendar.compose.weekcalendar.rememberWeekCalendarState
import com.kizitonwose.calendar.core.DayPosition
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.absoluteValue
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import com.ben.periodt.ui.theme.LocalAppIsDark
import java.time.DayOfWeek
import com.kizitonwose.calendar.core.daysOfWeek

private val SIZE_XXS = 11.sp
private val SIZE_XS  = 12.sp
private val SIZE_SM  = 13.sp
private val SIZE_MD  = 14.sp
private val SIZE_LG  = 15.sp
private val SIZE_XL  = 20.sp
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(viewModel: PeriodViewModel) {
    val cycles by viewModel.cycles.collectAsState()
    val prediction by viewModel.prediction.collectAsState()
    val isDark = LocalAppIsDark.current
    val isTransitioning by viewModel.isTransitioning.collectAsState()
    val isOnPill by viewModel.isOnPill.collectAsState()
    val pillStartDate by viewModel.pillPackStartDate.collectAsState()
    val pillStopDate by viewModel.pillStopDate.collectAsState()
    val pillPacks by viewModel.pillPacks.collectAsState()
    val dailyLogs by viewModel.dailyLogs.collectAsState()
    var dayLogTarget by remember { mutableStateOf<Pair<PeriodViewModel.Cycle, LocalDate>?>(null) }

    var showFullHistory by remember { mutableStateOf(false) }

    val sortedCycles = remember(cycles, isOnPill, pillStartDate, pillStopDate, showFullHistory) {
        val rawSorted = cycles.sortedByDescending { it.startDate }
        val wallDate = if (isOnPill) pillStartDate else pillStopDate
        if (wallDate != null && !showFullHistory) rawSorted.filter { !it.startDate.isBefore(wallDate) }
        else rawSorted
    }

    val hasHiddenHistory = remember(cycles, isOnPill, pillStartDate, pillStopDate) {
        val wallDate = if (isOnPill) pillStartDate else pillStopDate
        wallDate != null && cycles.any { it.startDate.isBefore(wallDate) }
    }

    var visibleCyclesCount by remember { mutableIntStateOf(3) }
    val displayedCycles = remember(sortedCycles, visibleCyclesCount) { sortedCycles.take(visibleCyclesCount) }

    val currentMonth   = remember { YearMonth.now() }
    val currentDate    = remember { LocalDate.now() }
    val firstDayOfWeek = remember { DayOfWeek.SUNDAY }

    val state = rememberCalendarState(
        startMonth        = currentMonth.minusMonths(12),
        endMonth          = currentMonth.plusMonths(12),
        firstVisibleMonth = currentMonth,
        firstDayOfWeek    = firstDayOfWeek
    )

    val weekState = rememberWeekCalendarState(
        startDate            = currentDate.minusWeeks(52),
        endDate              = currentDate.plusWeeks(52),
        firstVisibleWeekDate = currentDate,
        firstDayOfWeek       = firstDayOfWeek
    )

    val listState  = rememberLazyListState()
    var isCollapsed by remember { mutableStateOf(false) }
    val scope      = rememberCoroutineScope()

    LaunchedEffect(isCollapsed) {
        if (isCollapsed) {
            val visibleMonth = state.firstVisibleMonth.yearMonth
            val currentMonth = YearMonth.from(currentDate)

            val targetDate = if (visibleMonth == currentMonth) {
                // 1. If looking at the current month, snap exactly to today's week
                currentDate
            } else {
                // 2. If looking at another month, snap to the first week of THAT month
                state.firstVisibleMonth.weekDays.flatten()
                    .firstOrNull { it.position == DayPosition.MonthDate }?.date ?: currentDate
            }

            weekState.animateScrollToWeek(targetDate)
        } else {
            // When expanding, stay on the month of the currently viewed week
            val targetDate = weekState.firstVisibleWeek.days.getOrNull(3)?.date ?: currentDate
            state.animateScrollToMonth(YearMonth.from(targetDate))
        }
    }

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val delta = available.y
                if (delta < 0 && !isCollapsed) { isCollapsed = true; return Offset.Zero }
                if (delta > 0 && isCollapsed && listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0) {
                    isCollapsed = false; return Offset.Zero
                }
                return Offset.Zero
            }
        }
    }

    val entrySurface = if (isDark) Color(0xFF1B1B1B) else Color.White
    val entryText    = if (isDark) Color.White else Color(0xFF0F172A)
    val accentColor  = if (isDark) Color(0xFFD89046) else Color(0xFF6d9567).copy(alpha = 0.6f)

    var cycleToEdit by remember { mutableStateOf<PeriodViewModel.Cycle?>(null) }

    val postPillState  by viewModel.postPillState.collectAsState()
    val isLearningMode = postPillState == PostPillState.LEARNING

    LaunchedEffect(Unit) { viewModel.refreshState() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .nestedScroll(nestedScrollConnection)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .animateContentSize(animationSpec = tween(durationMillis = 300, easing = LinearOutSlowInEasing))
        ) {
            CalendarCard(
                isCollapsed     = isCollapsed,
                state           = state,
                weekState       = weekState,
                cycles          = cycles,
                prediction      = prediction,
                isTransitioning = isTransitioning,
                isLearningMode  = isLearningMode,
                isOnPill        = isOnPill,
                pillPacks       = pillPacks,
                firstDayOfWeek  = firstDayOfWeek,
                dailyLogs       = dailyLogs,
                onDayTapped     = { cycle, date -> dayLogTarget = Pair(cycle, date) }
            )
        }

        LazyColumn(
            modifier            = Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)),
            state               = listState,
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding      = PaddingValues(bottom = 115.dp)
        ) {

            item { WellnessCardsRow(cycles = cycles, prediction = prediction) }

            item {
                PredictionBanner(
                    prediction        = prediction,
                    cycles            = cycles,
                    isTransitioning   = isTransitioning,
                    isOnPill          = isOnPill,
                    pillStopDate      = pillStopDate,
                    pillPackStartDate = viewModel.pillPackStartDate.collectAsState().value,
                    pillPackCount     = viewModel.pillPackCount.collectAsState().value
                )
            }

            item {
                Text(
                    text       = "Cycle history",
                    fontFamily = BricolageGrotesque,
                    fontWeight = FontWeight.Bold,
                    fontSize   = SIZE_XL,
                    color      = entryText,
                    textAlign  = TextAlign.Center,
                    modifier   = Modifier.fillMaxWidth().padding(top = 16.dp)
                )
                Spacer(Modifier.height(10.dp))
            }

            items(displayedCycles, key = { it.id }) { cycle ->
                SwipeToDeleteCard(onDelete = { viewModel.deleteCycle(cycle.id) }) { isSwiping ->
                    EntryRow(
                        monthLabel     = cycle.startDate.month
                            .getDisplayName(TextStyle.FULL, Locale.getDefault())
                            .lowercase().replaceFirstChar { it.uppercase() },
                        dayNumber      = cycle.startDate.dayOfMonth.toString(),
                        startDate      = cycle.startDate.toString(),
                        endDate        = cycle.endDate?.toString() ?: "",
                        bleeding       = cycle.bleeding,
                        bloodColor     = cycle.bloodColor,
                        crampsPain     = cycle.painLevel,
                        surface        = entrySurface,
                        soft           = Color.Transparent,
                        text           = entryText,
                        sub            = entryText.copy(alpha = 0.6f),
                        accent         = entryText,
                        isSwiping      = isSwiping,
                        customDayCount = dailyLogs.count { (key, _) -> key.startsWith("${cycle.id}|") },
                        onEditClick    = { cycleToEdit = cycle }
                    )
                }
            }

            item {
                Column(
                    modifier            = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (sortedCycles.size > visibleCyclesCount) {
                        TextButton(
                            onClick = { visibleCyclesCount += 3 },
                            shape   = RoundedCornerShape(12.dp),
                            colors  = ButtonDefaults.textButtonColors(contentColor = accentColor)
                        ) {
                            Icon(Icons.Rounded.Update, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Load 3 More", fontFamily = BricolageGrotesque, fontWeight = FontWeight.SemiBold, fontSize = SIZE_MD)
                        }
                    }

                    if (hasHiddenHistory && !showFullHistory && sortedCycles.size <= visibleCyclesCount) {
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = { showFullHistory = true },
                            border  = BorderStroke(1.dp, accentColor.copy(alpha = 0.3f)),
                            shape   = RoundedCornerShape(50),
                            colors  = ButtonDefaults.outlinedButtonColors(contentColor = entryText)
                        ) {
                            Icon(Icons.Rounded.History, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Show Pre-Pill History", fontFamily = BricolageGrotesque, fontWeight = FontWeight.SemiBold, fontSize = SIZE_MD)
                        }
                    }
                }
            }
        }

        cycleToEdit?.let { cycle ->
            EditCycleDialog(
                cycle             = cycle,
                existingDailyLogs = dailyLogs.values.filter { it.cycleId == cycle.id },
                onDismiss         = { cycleToEdit = null },
                onSave            = { updated, overrides ->
                    viewModel.updateCycleWithDailyLogs(updated, overrides)
                    cycleToEdit = null
                }
            )
        }

        dayLogTarget?.let { (cycle, date) ->
            val existingLog = dailyLogs["${cycle.id}|$date"]
            DayLogDialog(
                date        = date,
                cycle       = cycle,
                existingLog = existingLog,
                onDismiss   = { dayLogTarget = null },
                onSave      = { bleeding, bloodColor, painLevel ->
                    viewModel.upsertDailyLog(cycle.id, date, bleeding, bloodColor, painLevel)
                    dayLogTarget = null
                },
                onClear     = {
                    viewModel.deleteDailyLog(cycle.id, date)
                    dayLogTarget = null
                }
            )
        }
    }
}

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
    val accentColor     = if (isDark) Color(0xFFD89046) else Color(0xFF6d9567).copy(alpha = 0.6f)
    val surfaceFallback = if (isDark) Color.Black else Color.Black.copy(alpha = 0.05f)
    val pillBackground  = if (isDark) Color.White.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.05f)
    val textPrimary     = if (isDark) Color.White else Color(0xFF0F172A)
    val textSub         = if (isDark) Color.White.copy(alpha = 0.6f) else Color(0xFF1b1b1b).copy(alpha = 0.6f)

    val pastelGreen  = Color(0xFF6d9567).copy(alpha = 0.6f)
    val pastelOrange = Color(0xFFD89046)
    val pastelMaroon = Color(0xFF4E1A1A)

    val bleedingOptions = listOf("Heavy", "Medium", "Light", "Spotting")
    val colorOptions    = listOf("Bright Red", "Dark Red", "Brown")

    var bleeding      by remember { mutableStateOf(existingLog?.bleeding   ?: cycle.bleeding) }
    var bloodColor    by remember { mutableStateOf(existingLog?.bloodColor ?: cycle.bloodColor) }
    var painLevel     by remember { mutableIntStateOf(existingLog?.painLevel ?: cycle.painLevel) }
    var sliderPosition by remember { mutableFloatStateOf(painLevel.toFloat()) }

    val formatter    = remember { DateTimeFormatter.ofPattern("MMM d") }
    val dayOfWeek    = remember { date.dayOfWeek.getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale.getDefault()) }

    // 🔥 THE FIX: Dynamically check if the current selections differ from the cycle defaults
    val isOverridden = existingLog != null ||
            bleeding != cycle.bleeding ||
            bloodColor != cycle.bloodColor ||
            painLevel != cycle.painLevel

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = sheetState,
        containerColor   = containerColor,
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
                modifier              = Modifier.fillMaxWidth().padding(bottom = 4.dp),
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
                    modifier         = Modifier.size(32.dp).clip(CircleShape).background(textSub.copy(alpha = 0.1f)).clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Close, null, tint = textPrimary, modifier = Modifier.size(18.dp))
                }
            }

            // Flow Intensity
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

            // Blood Color
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

            // Pain Level
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

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick   = { onSave(bleeding, bloodColor, painLevel) },
                    modifier  = Modifier.fillMaxWidth().padding(top = 8.dp).height(56.dp),
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
                    TextButton(onClick = onClear, modifier = Modifier.fillMaxWidth()) {
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

@Composable
fun CalendarLegend(
    isOnPill: Boolean = false,
    pillPacks: List<PeriodViewModel.PillPack> = emptyList()
) {
    val isDark           = LocalAppIsDark.current
    val textSub          = if (isDark) Color.White.copy(alpha = 0.6f) else Color(0xFF1b1b1b).copy(alpha = 0.6f)
    val colorPeriodSolid = Color(0xFFA5231C)
    val packColor        = Color(0xFFa68e74)

    Row(
        modifier              = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment     = Alignment.CenterVertically
    ) {
        if (isOnPill) {
            LegendItem(color = colorPeriodSolid.copy(alpha = 0.6f), label = "Logged",     textColor = textSub)
            Spacer(Modifier.width(12.dp))
            LegendItem(color = packColor.copy(alpha = 0.2f),        label = "Pills Done", textColor = textSub)
            Spacer(Modifier.width(12.dp))
            LegendItem(color = packColor,                           label = "Pills Left", textColor = textSub)
        } else {
            LegendItem(color = colorPeriodSolid.copy(alpha = 0.6f), label = "Logged",   textColor = textSub)
            Spacer(Modifier.width(12.dp))
            LegendItem(color = ColorFertileSolid,                   label = "Fertile",  textColor = textSub)
            Spacer(Modifier.width(12.dp))
            LegendItem(color = colorPeriodSolid,                    label = "Upcoming", textColor = textSub)
        }
    }
}

@Composable
private fun LegendItem(color: Color, label: String, textColor: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(width = 20.dp, height = 8.dp).clip(RoundedCornerShape(100.dp)).background(color))
        Spacer(Modifier.width(6.dp))
        Text(
            text       = label,
            fontFamily = BricolageGrotesque,
            fontSize   = SIZE_XS,
            color      = textColor,
            fontWeight = FontWeight.Medium
        )
    }
}

private val ColorFertileSolid = Color(0xFF6d9567).copy(alpha = 0.6f)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarCard(
    isCollapsed: Boolean,
    state: CalendarState,
    weekState: WeekCalendarState,
    cycles: List<PeriodViewModel.Cycle>,
    prediction: Prediction?,
    isTransitioning: Boolean = false,
    isLearningMode: Boolean = false,
    isOnPill: Boolean = false,
    pillPacks: List<PeriodViewModel.PillPack> = emptyList(),
    firstDayOfWeek: DayOfWeek = DayOfWeek.SUNDAY,
    dailyLogs: Map<String, PeriodViewModel.DailyLog> = emptyMap(),
    onDayTapped: (PeriodViewModel.Cycle, LocalDate) -> Unit = { _, _ -> }
) {
    val isDark = LocalAppIsDark.current
    val scope  = rememberCoroutineScope()

    val backgroundBrush    = if (isDark) Color(0xFF1B1B1B).copy(alpha = 0.5f) else Color.White
    val onCardContent      = if (isDark) Color.White else Color.Black
    val onCardContentMuted = onCardContent.copy(alpha = 0.70f)

    val daysOfWeek = remember(firstDayOfWeek) { daysOfWeek(firstDayOfWeek = firstDayOfWeek) }

    Card(
        shape    = RoundedCornerShape(24.dp),
        colors   = CardDefaults.cardColors(containerColor = Color.Transparent),
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
    ) {
        Box(modifier = Modifier.background(backgroundBrush).padding(horizontal = 14.dp, vertical = 12.dp)) {
            Column {
                // Header
                val headerText = if (isCollapsed) {
                    val currentWeek  = weekState.firstVisibleWeek
                    val dominantDate = currentWeek.days.getOrNull(3)?.date ?: currentWeek.days.first().date
                    dominantDate.format(DateTimeFormatter.ofPattern("MMM yyyy"))
                } else {
                    state.firstVisibleMonth.yearMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault()) +
                            " " + state.firstVisibleMonth.yearMonth.year
                }

                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text       = headerText,
                        fontFamily = BricolageGrotesque,
                        color      = onCardContent,
                        style      = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        modifier   = Modifier.padding(start = 4.dp)
                    )
                    Spacer(Modifier.width(12.dp))

                    Text(
                        "‹",
                        color     = onCardContentMuted,
                        fontSize  = 24.sp,
                        modifier  = Modifier.size(36.dp).clip(CircleShape).clickable {
                            scope.launch {
                                if (isCollapsed) weekState.animateScrollToWeek(weekState.firstVisibleWeek.days.first().date.minusWeeks(1))
                                else state.animateScrollToMonth(state.firstVisibleMonth.yearMonth.minusMonths(1))
                            }
                        }.wrapContentSize(Alignment.Center),
                        textAlign = TextAlign.Center
                    )

                    Text(
                        "›",
                        color     = onCardContentMuted,
                        fontSize  = 24.sp,
                        modifier  = Modifier.size(36.dp).clip(CircleShape).clickable {
                            scope.launch {
                                if (isCollapsed) weekState.animateScrollToWeek(weekState.firstVisibleWeek.days.first().date.plusWeeks(1))
                                else state.animateScrollToMonth(state.firstVisibleMonth.yearMonth.plusMonths(1))
                            }
                        }.wrapContentSize(Alignment.Center),
                        textAlign = TextAlign.Center
                    )

                    Spacer(Modifier.weight(1f))

                    Box(
                        modifier         = Modifier.size(32.dp).clip(CircleShape).clickable {
                            scope.launch {
                                if (isCollapsed) weekState.animateScrollToWeek(LocalDate.now())
                                else state.animateScrollToMonth(YearMonth.now())
                            }
                        },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.EventRepeat, null, tint = onCardContent, modifier = Modifier.size(16.dp))
                    }
                }

                Spacer(Modifier.height(14.dp))

                // Day-of-week headers
                Row(Modifier.fillMaxWidth()) {
                    daysOfWeek.forEach { dayOfWeek ->
                        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                            Text(
                                text       = dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()).uppercase(),
                                color      = onCardContentMuted,
                                fontSize   = SIZE_XXS,
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = BricolageGrotesque
                            )
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                AnimatedContent(targetState = isCollapsed, label = "CalendarType") { collapsed ->
                    if (collapsed) {
                        WeekCalendar(state = weekState, dayContent = { weekDay ->
                            DayCellEnhanced(
                                date            = weekDay.date,
                                isCurrentMonth  = true,
                                cycles          = cycles,
                                prediction      = prediction,
                                isTransitioning = isTransitioning,
                                isLearningMode  = isLearningMode,
                                isOnPill        = isOnPill,
                                pillPacks       = pillPacks,
                                dailyLogs       = dailyLogs,
                                onDayTapped     = onDayTapped
                            )
                        })
                    } else {
                        HorizontalCalendar(state = state, dayContent = { calendarDay ->
                            DayCellEnhanced(
                                date            = calendarDay.date,
                                isCurrentMonth  = calendarDay.position == DayPosition.MonthDate,
                                cycles          = cycles,
                                prediction      = prediction,
                                isTransitioning = isTransitioning,
                                isLearningMode  = isLearningMode,
                                isOnPill        = isOnPill,
                                pillPacks       = pillPacks,
                                dailyLogs       = dailyLogs,
                                onDayTapped     = onDayTapped
                            )
                        })
                    }
                }

                AnimatedVisibility(visible = !isCollapsed, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
                    Column {
                        Spacer(Modifier.height(12.dp))
                        CalendarLegend(isOnPill = isOnPill, pillPacks = pillPacks)
                        Spacer(Modifier.height(4.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun DayCellEnhanced(
    date: LocalDate,
    isCurrentMonth: Boolean,
    cycles: List<PeriodViewModel.Cycle>,
    prediction: Prediction?,
    isTransitioning: Boolean = false,
    isLearningMode: Boolean = false,
    isOnPill: Boolean = false,
    pillPacks: List<PeriodViewModel.PillPack> = emptyList(),
    dailyLogs: Map<String, PeriodViewModel.DailyLog> = emptyMap(),
    onDayTapped: (PeriodViewModel.Cycle, LocalDate) -> Unit = { _, _ -> }
) {
    val isDark = LocalAppIsDark.current
    val isToday = date == LocalDate.now()
    val today   = LocalDate.now()

    val themeAccent      = if (isDark) Color(0xFFD89046) else Color(0xFF6d9567).copy(alpha = 0.6f)
    val starAccent       = if (isDark) Color(0xFF8089D2) else Color(0xFF2C3F70)
    val colorPeriodSolid = Color(0xFFA5231C)
    val packColor        = Color(0xFFa68e74)
    val colorOvulationBg = if (isDark) Color.White.copy(alpha = 0.2f) else Color.Black.copy(alpha = 0.2f)

    val matchingPack = pillPacks.firstOrNull { pack ->
        val end = pack.endDate ?: pack.startDate.plusDays((pack.pillCount - 1).toLong())
        !date.isBefore(pack.startDate) && !date.isAfter(end)
    }
    val isInPillWindow   = matchingPack != null
    val isPillWindowPast = isInPillWindow && date.isBefore(today)
    val isOvulationDay   = !isOnPill && prediction?.ovulationDay == date

    fun checkPhase(d: LocalDate): Int {
        val isLoggedPeriod = cycles.any { c ->
            val start = c.startDate; val end = c.endDate ?: start.plusDays(6)
            !d.isBefore(start) && !d.isAfter(end)
        }
        if (isLoggedPeriod) return 1
        val inWindow = pillPacks.any { pack ->
            val end = pack.endDate ?: pack.startDate.plusDays((pack.pillCount - 1).toLong())
            !d.isBefore(pack.startDate) && !d.isAfter(end)
        }
        if (inWindow) return 5
        if (pillPacks.isEmpty() && !isTransitioning && prediction != null) {
            val s            = prediction.mostLikelyPeriodStart
            val windowLength = prediction.periodLength?.toLong() ?: 5L
            if (!d.isBefore(s) && d.isBefore(s.plusDays(windowLength))) return 2
            if (!isOnPill && prediction.fertileWindow.start != LocalDate.MIN && prediction.fertileWindow.contains(d)) return 3
        }
        return 0
    }

    val currentPhase = checkPhase(date)
    val owningCycle  = if (currentPhase == 1) {
        cycles.firstOrNull { c -> val end = c.endDate ?: c.startDate.plusDays(6); !date.isBefore(c.startDate) && !date.isAfter(end) }
    } else null
    val hasOverride  = owningCycle != null && dailyLogs.containsKey("${owningCycle.id}|$date")

    val prevPhase    = checkPhase(date.minusDays(1))
    val nextPhase    = checkPhase(date.plusDays(1))
    val isStart      = currentPhase != prevPhase
    val isEnd        = currentPhase != nextPhase
    val stripRadius  = 100.dp

    val shape = when {
        currentPhase == 0 -> CircleShape
        isStart && isEnd  -> CircleShape
        isStart           -> RoundedCornerShape(topStart = stripRadius, bottomStart = stripRadius)
        isEnd             -> RoundedCornerShape(topEnd = stripRadius, bottomEnd = stripRadius)
        else              -> RectangleShape
    }
    val padding = when {
        currentPhase == 0 -> PaddingValues(2.dp)
        isStart && isEnd  -> PaddingValues(4.dp)
        isStart           -> PaddingValues(start = 4.dp, top = 4.dp, bottom = 4.dp)
        isEnd             -> PaddingValues(end = 4.dp, top = 4.dp, bottom = 4.dp)
        else              -> PaddingValues(vertical = 4.dp)
    }
    val bgColor = when (currentPhase) {
        1    -> colorPeriodSolid.copy(alpha = 0.6f)
        2    -> colorPeriodSolid
        3    -> ColorFertileSolid
        5    -> if (isPillWindowPast) packColor.copy(alpha = 0.2f) else packColor
        else -> Color.Transparent
    }
    val isHighlighted = when (currentPhase) { 5 -> !isPillWindowPast; 0 -> false; else -> true }

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .then(
                if (owningCycle != null) Modifier.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onDayTapped(owningCycle, date) }
                else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(modifier = Modifier.fillMaxSize().padding(padding).clip(shape).background(bgColor))

        if (isOvulationDay) {
            Box(modifier = Modifier.size(35.dp).clip(CircleShape).background(colorOvulationBg))
        }

        DayText(date, isCurrentMonth, isHighlighted = isHighlighted)

        if (hasOverride) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 10.dp)
                    .size(4.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.85f))
            )
        }

        if (isToday) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(
                    color  = when {
                        currentPhase != 0               -> Color.White
                        isTransitioning || isLearningMode -> starAccent
                        pillPacks.isNotEmpty()           -> themeAccent
                        isDark                           -> Color.White
                        else                             -> Color.Black
                    },
                    radius = size.minDimension / 2.6f,
                    style  = Stroke(width = 2.dp.toPx())
                )
            }
        }
    }
}

@Composable
fun DayText(date: LocalDate, isCurrentMonth: Boolean, isHighlighted: Boolean) {
    val isDark = LocalAppIsDark.current
    val alpha  = if (isHighlighted || isCurrentMonth) 1f else 0.3f
    val color  = if (isDark) {
        if (isHighlighted) Color.White else Color.White.copy(alpha = alpha)
    } else {
        if (isHighlighted) Color.White else Color.Black.copy(alpha = alpha)
    }
    Text(
        text       = date.dayOfMonth.toString(),
        color      = color,
        fontSize   = SIZE_MD,
        fontWeight = if (isHighlighted) FontWeight.Bold else FontWeight.Normal,
        fontFamily = BricolageGrotesque
    )
}

// ---------- Swipe row, entry row ----------
@Composable
fun SwipeToDeleteCard(onDelete: () -> Unit, content: @Composable (Boolean) -> Unit) {
    val density     = LocalDensity.current
    val scope       = rememberCoroutineScope()
    val offsetX     = remember { Animatable(0f) }
    val revealDp    = 80.dp
    val revealPx    = with(density) { revealDp.toPx() }
    val deleteThreshold = with(density) { 180.dp.toPx() }
    val maxRevealPx = with(density) { 220.dp.toPx() }
    var widthPx by remember { mutableStateOf(0f) }
    val itemShape   = RoundedCornerShape(22.dp)
    val bounceSpring = spring<Float>(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMedium)
    val isRevealed  by remember { derivedStateOf { offsetX.value.absoluteValue > revealPx / 2f } }
    val isSwiping   by remember { derivedStateOf { offsetX.value.absoluteValue > 2f } }

    Card(
        shape     = itemShape,
        colors    = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier  = Modifier.fillMaxWidth().onSizeChanged { widthPx = it.width.toFloat() }
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier.matchParentSize().graphicsLayer { shape = itemShape; clip = true }
                    .background(Color.Red.copy(alpha = if (offsetX.value.absoluteValue > 10f) 0.8f else 0f))
                    .padding(horizontal = 24.dp),
                contentAlignment = if (offsetX.value >= 0f) Alignment.CenterStart else Alignment.CenterEnd
            ) {
                if (isRevealed) {
                    Icon(Icons.Default.Delete, "Delete", tint = Color.White, modifier = Modifier.size(24.dp))
                    Box(
                        modifier = Modifier.fillMaxHeight().width(revealDp).clickable(
                            interactionSource = remember { MutableInteractionSource() }, indication = null
                        ) {
                            scope.launch {
                                val target = if (offsetX.value >= 0f) widthPx else -widthPx
                                offsetX.animateTo(target, bounceSpring)
                                onDelete()
                            }
                        }
                    )
                }
            }

            Box(
                modifier = Modifier.fillMaxWidth().graphicsLayer { shape = itemShape; clip = true }
                    .offset { IntOffset(offsetX.value.toInt(), 0) }
                    .pointerInput(Unit) {
                        detectHorizontalDragGestures(
                            onDragEnd = {
                                scope.launch {
                                    val currentOff = offsetX.value
                                    val target = when {
                                        currentOff <= -deleteThreshold -> -widthPx
                                        currentOff >= deleteThreshold  ->  widthPx
                                        currentOff <= -revealPx / 2f  -> -revealPx
                                        currentOff >= revealPx / 2f   ->  revealPx
                                        else                           ->  0f
                                    }
                                    if (kotlin.math.abs(target) == widthPx && widthPx > 0f) {
                                        offsetX.animateTo(target, bounceSpring); onDelete()
                                    } else { offsetX.animateTo(target, bounceSpring) }
                                }
                            }
                        ) { change, dragAmount ->
                            change.consume()
                            scope.launch { offsetX.snapTo((offsetX.value + dragAmount).coerceIn(-maxRevealPx, maxRevealPx)) }
                        }
                    }
            ) { content(isSwiping) }
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

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = containerColor, modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars)
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
            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text       = "Edit Entry",
                    fontFamily = BricolageGrotesque,
                    style      = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color      = textPrimary
                )
                Box(modifier = Modifier.size(32.dp).clip(CircleShape).background(textSub.copy(alpha = 0.1f)).clickable(onClick = onDismiss), contentAlignment = Alignment.Center) {
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
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(pillBackground).clickable { showDailyLog = !showDailyLog }.padding(16.dp, 14.dp),
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
                        Icon(Icons.Default.KeyboardArrowDown, null, tint = pillTextColor.copy(alpha = 0.5f), modifier = Modifier.size(20.dp).graphicsLayer { rotationZ = dailyLogRotation })
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
                onClick   = { onSave(cycle.copy(startDate = startDate, endDate = endDate, bleeding = derivedBleeding, bloodColor = derivedColor, painLevel = derivedPain), dailyOverrides) },
                modifier  = Modifier.fillMaxWidth().padding(top = 8.dp).height(56.dp),
                shape     = RoundedCornerShape(18.dp),
                colors    = ButtonDefaults.buttonColors(containerColor = accentColor, contentColor = Color.White),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
            ) {
                Text(
                    "Update Entry",
                    fontFamily = BricolageGrotesque,
                    fontSize   = SIZE_LG,
                    fontWeight = FontWeight.Bold
                )
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

@Composable
fun EntryRow(
    monthLabel: String, dayNumber: String, startDate: String, endDate: String,
    bleeding: String, bloodColor: String, crampsPain: Int,
    surface: Color, soft: Color, text: Color, sub: Color, accent: Color,
    isSwiping: Boolean = false, customDayCount: Int = 0, onEditClick: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val rotation by animateFloatAsState(if (expanded) 180f else 0f, label = "arrowRotation")

    val isDark             = LocalAppIsDark.current
    val cardBackground     = if (isDark) Color(0xFF1B1B1B).copy(alpha = 0.5f) else Color.White
    val pillBackground     = if (isDark) Color(0xFFE8EBED).copy(alpha = 0.1f) else Color(0xFFE8EBED).copy(alpha = 0.4f)
    val primaryTextColor   = if (isDark) Color.White else Color(0xFF1B1B1B)
    val secondaryTextColor = if (isDark) Color.White.copy(alpha = 0.6f) else Color(0xFF1b1b1b).copy(alpha = 0.6f)
    val pillTextColor      = if (isDark) Color.White else Color(0xFF1B1B1B)

    val capitalizedBleeding = remember(bleeding) {
        bleeding.lowercase(java.util.Locale.getDefault()).replaceFirstChar { it.titlecase(java.util.Locale.getDefault()) }
    }
    fun shortPretty(d: String): String = runCatching {
        if (d.isBlank()) return@runCatching "?"
        val date = java.time.LocalDate.parse(d)
        "${date.month.getDisplayName(java.time.format.TextStyle.SHORT, java.util.Locale.getDefault())} ${date.dayOfMonth}"
    }.getOrElse { d }

    val startDt = remember(startDate) { runCatching { java.time.LocalDate.parse(startDate) }.getOrNull() }
    val endDt   = remember(endDate)   { runCatching { if (endDate.isNotBlank()) java.time.LocalDate.parse(endDate) else null }.getOrNull() }
    val today   = LocalDate.now()

    val statusText = remember(startDt, endDt) {
        if (startDt != null) {
            if (endDt == null) "Day ${ChronoUnit.DAYS.between(startDt, today) + 1} • Ongoing"
            else "${ChronoUnit.DAYS.between(startDt, endDt) + 1} Days • Completed"
        } else "Status unknown"
    }

    Card(
        colors    = CardDefaults.cardColors(containerColor = cardBackground),
        shape     = RoundedCornerShape(22.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier  = Modifier.fillMaxWidth().clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = { expanded = !expanded })
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(42.dp).clip(CircleShape).background(pillBackground), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.History, null, tint = primaryTextColor, modifier = Modifier.size(24.dp))
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text       = "$monthLabel $dayNumber",
                        fontFamily = BricolageGrotesque,
                        color      = primaryTextColor,
                        fontSize   = SIZE_LG,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text       = statusText,
                        fontFamily = BricolageGrotesque,
                        color      = secondaryTextColor,
                        fontSize   = SIZE_SM,
                        fontWeight = FontWeight.Normal
                    )
                }
                Icon(Icons.Default.KeyboardArrowDown, null, tint = primaryTextColor.copy(alpha = 0.6f), modifier = Modifier.size(24.dp).graphicsLayer { rotationZ = rotation })
            }

            AnimatedVisibility(visible = expanded) {
                Column {
                    Spacer(Modifier.height(18.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(10.dp)) {
                            InfoBox("Duration", "${shortPretty(startDate)} - ${if (endDate.isNotBlank()) shortPretty(endDate) else "Ongoing"}", pillBackground, pillTextColor)
                            InfoBox(if (customDayCount > 0) "Avg Pain" else "Pain", "$crampsPain/10", pillBackground, pillTextColor)
                        }
                        Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(10.dp)) {
                            InfoBox(if (customDayCount > 0) "Peak Flow" else "Flow", capitalizedBleeding, pillBackground, pillTextColor)
                            InfoBox(if (customDayCount > 0) "Main Color" else "Color", bloodColor, pillBackground, pillTextColor)
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    OutlinedButton(
                        onClick = onEditClick,
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape    = RoundedCornerShape(18.dp),
                        border   = BorderStroke(1.dp, secondaryTextColor.copy(alpha = 0.3f)),
                        colors   = ButtonDefaults.outlinedButtonColors(contentColor = primaryTextColor)
                    ) {
                        Icon(Icons.Default.Edit, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Edit Entry",
                            fontFamily = BricolageGrotesque,
                            fontSize   = SIZE_LG,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RowScope.InfoBox(label: String, value: String, bg: Color, textColor: Color) {
    Column(
        modifier = Modifier.weight(1f).clip(RoundedCornerShape(20.dp)).background(bg).padding(vertical = 14.dp, horizontal = 16.dp)
    ) {
        Text(
            text       = label,
            fontFamily = BricolageGrotesque,
            color      = textColor.copy(alpha = 0.7f),
            fontSize   = SIZE_XXS,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text       = value,
            fontFamily = BricolageGrotesque,
            color      = textColor,
            fontSize   = SIZE_SM,
            fontWeight = FontWeight.Normal
        )
    }
}

// ─── Wellness card copy lists (unchanged) ────────────────────────────────────
private val activeCycleMessages = listOf("Focus on self-care and hydration today.", "Warm baths and rest go a long way right now.", "Your body is doing a lot — be gentle with yourself.", "A heating pad and something comforting sounds right.", "This is a great time to slow down and recharge.", "Listen to what your body needs today.", "Dark chocolate counts as self-care. 🍫", "Rest is productive. You're allowed to take it easy.", "Check in with yourself — how are you feeling today?", "Hydrate, rest, repeat. You've got this. 💪")
private val naturalFlowMessages = listOf("Enjoy your natural flow. ✨", "Your cycle is doing its thing. ✨", "A calm phase — make the most of it. 🌿", "Good things ahead. Keep logging for better predictions.", "You're in a great window right now. 🌸", "Feeling yourself? This phase tends to be the best. ✨", "Your body is in rhythm. Stay consistent. 🌿", "The quiet before the storm — rest up and enjoy! ☀️", "Track how you feel today — patterns matter. 📊", "Energy up? Use it well. ✨")
private val pillFlowMessages    = listOf("Stay consistent with your pack! 💊", "One pill a day keeps the guesswork away. 💊", "Consistency is key — keep it up! ✨", "On track with your pack. Great work! 💊", "Remember to take your pill at the same time each day.", "Staying consistent helps your body stay regulated. 💊", "You're doing great — keep the streak going! ✨", "Same time every day is the goal. You've got this! 💊", "Your pack is on track. Stay consistent! 🌿", "Pill taken? Check. You're doing amazing. ✨")

@Composable
fun PredictionBanner(
    prediction: Prediction?, cycles: List<PeriodViewModel.Cycle>,
    isTransitioning: Boolean, isOnPill: Boolean, pillStopDate: LocalDate?,
    pillPackStartDate: LocalDate? = null, pillPackCount: Int = 21
) {
    val today = LocalDate.now()
    val now   = LocalDateTime.now()
    val isDark = LocalAppIsDark.current

    val packEndDate = remember(pillPackStartDate, pillPackCount) {
        pillPackStartDate?.plusDays((pillPackCount - 1).toLong())
    }
    val endFormatter = remember { java.time.format.DateTimeFormatter.ofPattern("MMM dd") }
    val postPillCycles = remember(cycles, pillStopDate) {
        if (pillStopDate != null) cycles.filter { !it.startDate.isBefore(pillStopDate) } else emptyList()
    }
    val postPillState = remember(postPillCycles, isOnPill, pillStopDate) {
        if (isOnPill || pillStopDate == null) PostPillState.NORMAL else getPostPillState(postPillCycles)
    }
    val activeCycle = cycles.firstOrNull { it.endDate == null || (today >= it.startDate && today <= it.endDate) }

    if (prediction == null && postPillState == PostPillState.NORMAL && activeCycle == null) return

    val cardBackground = if (isDark) Color(0xFF1B1B1B).copy(alpha = 0.5f) else Color.White
    val textPrimary    = if (isDark) Color.White else Color(0xFF0F172A)
    val textSecondary  = if (isDark) Color.White.copy(alpha = 0.6f) else Color(0xFF1b1b1b).copy(alpha = 0.6f)
    val pillBackground = remember(isDark, isOnPill) {
        when { isOnPill && isDark -> Color(0xFFa68e74).copy(alpha = 0.15f); isOnPill -> Color(0xFFa68e74).copy(alpha = 0.10f); isDark -> Color(0xFFD89046).copy(alpha = 0.15f); else -> Color(0xFF6d9567).copy(alpha = 0.10f) }
    }
    val pillTextColor  = remember(isDark, isOnPill) { when { isOnPill -> Color(0xFFa68e74); isDark -> Color(0xFFD89046); else -> Color(0xFF6d9567) } }
    val progressBrush  = remember(isDark, isOnPill) {
        val color = when { isOnPill -> Color(0xFFa68e74); isDark -> Color(0xFFD89046); else -> Color(0xFF6d9567).copy(alpha = 0.6f) }
        Brush.linearGradient(colors = listOf(color, color))
    }

    val icon: ImageVector; val accentColor: Color; val statusTitle: String; val personalMessage: String; val dateBadgeText: String
    var progTarget by remember { mutableFloatStateOf(0f) }
    var compliment by remember { mutableStateOf("") }

    if (activeCycle != null) {
        val dayOfPeriod  = ChronoUnit.DAYS.between(activeCycle.startDate, today).toInt() + 1
        val pillDayIndex = if (pillPackStartDate != null) (today.toEpochDay() - pillPackStartDate.toEpochDay()).toInt().coerceAtLeast(0) else today.toEpochDay().toInt()
        val label        = if (isOnPill) "Withdrawal Bleed" else "Period"
        icon             = Icons.Rounded.Favorite
        accentColor      = if (isOnPill) Color(0xFFa68e74) else Color(0xFFEF5350)
        statusTitle      = "$label Day $dayOfPeriod"
        personalMessage  = when (postPillState) {
            PostPillState.DISCOVERY -> "First cycle after stopping pills — recalibrating. 🌿"
            PostPillState.LEARNING  -> "Learning your natural rhythm. Keep logging! 🌿"
            else -> if (isOnPill) pillFlowMessages[pillDayIndex % pillFlowMessages.size] else activeCycleMessages[(dayOfPeriod - 1) % activeCycleMessages.size]
        }
        dateBadgeText    = when { isOnPill -> "Pill Pack"; postPillState == PostPillState.DISCOVERY -> "Discovery"; postPillState == PostPillState.LEARNING -> "Learning"; else -> "Active" }
        val totalDays    = activeCycle.endDate?.let { ChronoUnit.DAYS.between(activeCycle.startDate, it) + 1 } ?: 6L
        progTarget       = (ChronoUnit.MINUTES.between(activeCycle.startDate.atStartOfDay(), now).toFloat() / (totalDays * 1440f)).coerceIn(0f, 0.95f)
    } else if (postPillState == PostPillState.DISCOVERY) {
        icon = Icons.Rounded.AutoAwesome; accentColor = if (isDark) Color(0xFF8089D2) else Color(0xFF2C3F70)
        statusTitle = "Discovery Mode"; personalMessage = "Predictions are paused while recalibrating."; dateBadgeText = "Paused"
    } else if (postPillState == PostPillState.LEARNING) {
        icon = Icons.Rounded.AutoAwesome; accentColor = if (isDark) Color(0xFF8089D2) else Color(0xFF2C3F70)
        statusTitle = "Learning Mode"; personalMessage = "Predictions are active, but we're still refining accuracy."
        dateBadgeText = prediction?.mostLikelyPeriodStart?.pretty() ?: "Learning"
    } else if (prediction == null) {
        icon = Icons.Rounded.AutoAwesome; accentColor = if (isDark) Color(0xFFD89046) else Color(0xFF6d9567).copy(alpha = 0.6f)
        statusTitle = "Learning your rhythm"; personalMessage = "Keep tracking to unlock predictions."; dateBadgeText = "Learning"
    } else {
        val daysUntil      = ChronoUnit.DAYS.between(today, prediction.mostLikelyPeriodStart)
        val cycleTypeLabel = if (isOnPill) "withdrawal bleed" else "cycle"
        val packInfo       = if (isOnPill && packEndDate != null) {
            when { today.isAfter(packEndDate) -> "Pack finished"; today.isEqual(packEndDate) -> "Last pill today"; else -> "Pack ends on ${packEndDate.format(endFormatter)}" }
        } else null
        val quad = when {
            daysUntil < 0   -> Quadruple(Icons.Rounded.Warning,   Color(0xFFEF5350), "Late by ${kotlin.math.abs(daysUntil)} days", "No stress — cycles can shift! 🧘‍♀️")
            daysUntil == 0L -> Quadruple(Icons.Rounded.Favorite,  if (isOnPill) Color(0xFFa68e74) else if (isDark) Color(0xFFC8D4E5) else Color(0xFF8089D2), "Starts today", if (isOnPill) "Withdrawal bleed expected today." else "Ready for your period? 🍫")
            daysUntil <= 3  -> Quadruple(Icons.Rounded.Bolt,      Color(0xFFFFB74D), "Almost time", "Rest up and stay cozy. 💧")
            else            -> {
                val message = if (packInfo != null) "$packInfo • ${pillFlowMessages[today.dayOfYear % pillFlowMessages.size]}" else naturalFlowMessages[today.dayOfYear % naturalFlowMessages.size]
                Quadruple(Icons.Rounded.Spa, if (isOnPill) Color(0xFFa68e74) else if (isDark) Color(0xFFD89046) else Color(0xFF6d9567).copy(alpha = 0.6f), "$daysUntil days until next $cycleTypeLabel", message)
            }
        }
        icon = quad.first; accentColor = quad.second; statusTitle = quad.third; personalMessage = quad.fourth
        dateBadgeText = prediction.mostLikelyPeriodStart.pretty()
    }

    val animatedProgress by animateFloatAsState(targetValue = progTarget, animationSpec = tween(1200), label = "BannerProgress")

    Card(
        shape     = RoundedCornerShape(22.dp),
        colors    = CardDefaults.cardColors(containerColor = cardBackground),
        modifier  = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(24.dp))
                Box(modifier = Modifier.clip(RoundedCornerShape(50)).background(accentColor.copy(alpha = 0.15f)).padding(horizontal = 12.dp, vertical = 6.dp)) {
                    Text(
                        text       = dateBadgeText,
                        fontFamily = BricolageGrotesque,
                        color      = accentColor,
                        fontWeight = FontWeight.SemiBold,
                        fontSize   = SIZE_XXS
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = statusTitle,     fontFamily = BricolageGrotesque, fontWeight = FontWeight.SemiBold, color = textPrimary,   fontSize = SIZE_LG)
            Text(text = personalMessage, fontFamily = BricolageGrotesque,                                  color = textSecondary, fontSize = SIZE_SM)

            AnimatedVisibility(visible = activeCycle != null) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Spacer(Modifier.height(16.dp))
                    Canvas(Modifier.fillMaxWidth().height(6.dp).clip(CircleShape)) {
                        drawRoundRect(color = pillBackground, size = size, cornerRadius = CornerRadius(50f))
                        drawRoundRect(brush = progressBrush, size = Size(animatedProgress * size.width, size.height), cornerRadius = CornerRadius(50f))
                    }
                    if (compliment.isNotEmpty()) {
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text       = compliment,
                            fontFamily = BricolageGrotesque,
                            style      = androidx.compose.ui.text.TextStyle(brush = progressBrush, fontSize = SIZE_LG, fontWeight = FontWeight.Medium)
                        )
                    }
                }
            }
        }
    }
}

// ─── Wellness phase copy lists (unchanged) ───────────────────────────────────
private val bleedingDo   = listOf("Take it slow and rest up.", "Curl up with something cozy.", "Give yourself permission to do nothing.", "A warm bath can do wonders today.", "Journal how you're feeling today.", "Low effort, high reward — rest wins.", "Cancel what you can. Rest first.")
private val bleedingMove = listOf("Try gentle stretching or yoga.", "A slow walk outside is enough.", "Restorative yoga is perfect right now.", "Light stretching before bed tonight.", "Child's pose. That's it. That's enough.", "Breathwork counts as movement today.", "Gentle mobility work if you have energy.")
private val bleedingEat  = listOf("Comfort food rich in iron.", "Dark leafy greens and lentils today.", "Warm soups are your best friend.", "Iron-rich foods help with fatigue.", "Magnesium-rich foods ease cramps.", "Bone broth or a hearty stew.", "Dark chocolate has iron — treat yourself.")
private val follicularDo   = listOf("Plan new goals or projects.", "Start that thing you've been putting off.", "Your focus is sharp — use it.", "Write down your intentions for the month.", "Great time to learn something new.", "Energy is building — lean into it.", "Tackle your to-do list with confidence.")
private val follicularMove = listOf("Go for a run or a hike.", "Try a new workout class today.", "Cardio feels easier this week — use it.", "Push a little harder than usual.", "HIIT, cycling, or a long run all work.", "Your body is primed for challenge.", "Set a new personal record today.")
private val follicularEat  = listOf("Fresh salads and protein.", "Lean protein fuels your rising energy.", "Colorful vegetables are your best bet.", "Fermented foods support your gut now.", "Light and fresh keeps energy steady.", "Eggs, legumes, and greens are great.", "Antioxidant-rich foods are perfect now.")
private val ovulationDo    = listOf("Connect with your friends.", "Say yes to social plans today.", "Your confidence is peaking — own it.", "Great day for an important conversation.", "Collaborate, pitch, present — you've got this.", "Reach out to someone you've been meaning to.", "Charisma is up. Use it wisely. ✨")
private val ovulationMove  = listOf("Push limits with a workout.", "Your strength is at its peak today.", "HIIT, lifting, or dancing — all great.", "High-intensity feels good this week.", "Try something physically challenging.", "Spin class, climbing, or sprints.", "Your body can handle more right now.")
private val ovulationEat   = listOf("Light meals keep you going.", "Anti-inflammatory foods support ovulation.", "Raw veggies and lean proteins today.", "Zinc-rich foods are great right now.", "Hydrate well — your body needs it.", "Fibre-rich foods keep things balanced.", "Whole grains and fresh fruit are ideal.")
private val lutealDo       = listOf("Tidy up your personal space.", "Nesting mode is valid and productive.", "Wind down your schedule a little.", "Reflect on the month — what worked?", "Creative, low-key activities suit you now.", "Great time for a digital detox evening.", "Prep meals for the week ahead.")
private val lutealMove     = listOf("Pilates or strength training.", "Lower intensity feels better this week.", "A long walk clears the mind.", "Swimming or cycling are great options.", "Yoga and stretching suit this phase.", "Listen to your energy and adjust.", "Moderate movement supports your mood.")
private val lutealEat      = listOf("Complex carbs stabilize mood.", "Magnesium helps with PMS symptoms.", "Whole grains and root vegetables help.", "Reduce caffeine and sugar if you can.", "Omega-3s support mood this phase.", "Warm, nourishing meals are ideal.", "Dark chocolate for magnesium. 🍫")
private val defaultDo      = listOf("Take some time to unwind today.", "A moment of stillness goes a long way.", "Check in with yourself today.", "Do one thing that brings you joy.", "Rest and intention go hand in hand.", "Be kind to yourself today.", "Small acts of self-care add up.")
private val defaultMove    = listOf("A gentle walk is perfect.", "Movement is medicine — any amount counts.", "Stretch for 10 minutes today.", "Fresh air and a short walk.", "Even 5 minutes of movement helps.", "Put on music and move freely.", "Your pace is the right pace.")
private val defaultEat     = listOf("Stay hydrated and drink water.", "Whole foods over processed today.", "A nourishing meal changes everything.", "Eat something colourful today.", "Slow down and enjoy your food.", "Hydration is self-care.", "Listen to what your body is craving.")

@Composable
fun WellnessCardsRow(cycles: List<PeriodViewModel.Cycle>, prediction: Prediction?) {
    val today     = LocalDate.now()
    val lastCycle = cycles.maxByOrNull { it.startDate }
    var doList    = defaultDo;   var moveList = defaultMove; var eatList = defaultEat
    var doIcon    = Icons.Rounded.SelfImprovement; var moveIcon = Icons.Rounded.DirectionsWalk; var eatIcon = Icons.Rounded.LocalCafe

    val isBleeding    = lastCycle != null && (lastCycle.endDate == null || today <= lastCycle.endDate)
    val daysSinceStart = lastCycle?.let { ChronoUnit.DAYS.between(it.startDate, today).toInt() } ?: 0
    val cardIndex     = if (isBleeding) daysSinceStart else today.toEpochDay().toInt()

    if (lastCycle != null) {
        when {
            isBleeding               -> { doList = bleedingDo;   doIcon = Icons.Rounded.Bedtime;      moveList = bleedingMove;   moveIcon = Icons.Rounded.SelfImprovement; eatList = bleedingEat;   eatIcon = Icons.Rounded.SoupKitchen }
            daysSinceStart in 6..13  -> { doList = follicularDo; doIcon = Icons.Rounded.Checklist;    moveList = follicularMove; moveIcon = Icons.Rounded.DirectionsRun;   eatList = follicularEat; eatIcon = Icons.Rounded.Restaurant }
            daysSinceStart in 14..17 -> { doList = ovulationDo;  doIcon = Icons.Rounded.Favorite;     moveList = ovulationMove;  moveIcon = Icons.Rounded.FitnessCenter;   eatList = ovulationEat;  eatIcon = Icons.Rounded.Tapas }
            daysSinceStart in 18..28 -> { doList = lutealDo;     doIcon = Icons.Rounded.AutoAwesome;  moveList = lutealMove;     moveIcon = Icons.Rounded.SelfImprovement; eatList = lutealEat;     eatIcon = Icons.Rounded.Grain }
        }
    }

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        WellnessCardItem(title = "Do",   content = doList[cardIndex % doList.size],     icon = doIcon,   backgroundColor = Color(0xFF6d9567).copy(alpha = 0.4f), modifier = Modifier.weight(1f))
        WellnessCardItem(title = "Move", content = moveList[cardIndex % moveList.size], icon = moveIcon, backgroundColor = Color(0xFFD89046),                    modifier = Modifier.weight(1f))
        WellnessCardItem(title = "Eat",  content = eatList[cardIndex % eatList.size],   icon = eatIcon,  backgroundColor = Color(0xFFa68e74),                    modifier = Modifier.weight(1f))
    }
}

@Composable
fun WellnessCardItem(title: String, content: String, icon: ImageVector, backgroundColor: Color, modifier: Modifier = Modifier) {
    Card(modifier = modifier.height(165.dp), shape = RoundedCornerShape(26.dp), colors = CardDefaults.cardColors(containerColor = backgroundColor), elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)) {
        Box(modifier = Modifier.fillMaxSize().background(backgroundColor).padding(16.dp), contentAlignment = Alignment.Center) {
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(imageVector = icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                Spacer(Modifier.height(8.dp))
                Text(
                    text          = title.uppercase(),
                    fontFamily    = BricolageGrotesque,
                    color         = Color.White.copy(alpha = 0.8f),
                    fontSize      = SIZE_XXS,
                    fontWeight    = FontWeight.SemiBold,
                    letterSpacing = 1.sp
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text       = content,
                    fontFamily = BricolageGrotesque,
                    color      = Color.White,
                    textAlign  = TextAlign.Center,
                    lineHeight = 18.sp,
                    fontSize   = SIZE_XS
                )
            }
        }
    }
}

data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)