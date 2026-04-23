package com.ben.periodt.ui.calendar

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Update
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ben.periodt.prediction.PostPillState
import com.ben.periodt.ui.calendar.components.CalendarCard
import com.ben.periodt.ui.calendar.components.DayLogDialog
import com.ben.periodt.ui.calendar.components.EditCycleDialog
import com.ben.periodt.ui.calendar.components.EntryRow
import com.ben.periodt.ui.calendar.components.PredictionBanner
import com.ben.periodt.ui.calendar.components.SwipeToDeleteCard
import com.ben.periodt.ui.calendar.components.WellnessCardsRow
import com.ben.periodt.ui.theme.BricolageGrotesque
import com.ben.periodt.ui.theme.LocalAppIsDark
import com.ben.periodt.viewmodel.PeriodViewModel
import com.kizitonwose.calendar.compose.rememberCalendarState
import com.kizitonwose.calendar.compose.weekcalendar.rememberWeekCalendarState
import com.kizitonwose.calendar.core.DayPosition
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

private val SIZE_MD  = 14.sp
private val SIZE_XL  = 20.sp

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
                currentDate
            } else {
                state.firstVisibleMonth.weekDays.flatten()
                    .firstOrNull { it.position == DayPosition.MonthDate }?.date ?: currentDate
            }

            weekState.animateScrollToWeek(targetDate)
        } else {
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
    val accentColor  = if (isDark) Color(0xFFD89046) else Color(0xFFa5bda3)

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