package com.ben.periodt.uiux.calendar

import android.app.Application
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ben.periodt.viewmodel.PeriodViewModel
import com.kizitonwose.calendar.compose.HorizontalCalendar
import com.kizitonwose.calendar.compose.rememberCalendarState
import com.kizitonwose.calendar.core.DayPosition
import kotlinx.coroutines.launch
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import kotlin.math.absoluteValue
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import com.kizitonwose.calendar.compose.WeekCalendar
import com.kizitonwose.calendar.compose.weekcalendar.rememberWeekCalendarState
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.geometry.Offset
import androidx.compose.material.icons.Icons
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.ben.periodt.R
import java.time.LocalDate
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.font.FontStyle
import java.time.LocalDateTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen() {
    val context = LocalContext.current.applicationContext as Application
    val viewModel: PeriodViewModel = viewModel(factory = PeriodViewModel.Factory(context))
    val cycles by viewModel.cycles.collectAsState()
    val prediction by viewModel.prediction.collectAsState()
    val isDark = isSystemInDarkTheme()

    // Gradient palette
    val gradTop = if (isDark) Color(0xFF7B8FA3) else Color(0xFF8FA0B1)
    val gradMid = if (isDark) Color(0xFF7288A0) else Color(0xFF8799B0)
    val gradBottom = if (isDark) Color(0xFF5A7396) else Color(0xFF6E87A7)

    val onGradient = Color.White
    val onGradientMuted = onGradient.copy(alpha = if (isDark) 0.70f else 0.55f)

    val currentMonth = remember { YearMonth.now() }
    val currentDate = remember { LocalDate.now() }

    // Month State
    val state = rememberCalendarState(
        startMonth = currentMonth.minusMonths(12),
        endMonth = currentMonth.plusMonths(12),
        firstVisibleMonth = currentMonth
    )

    // Week State
    val weekState = rememberWeekCalendarState(
        startDate = currentDate.minusWeeks(52),
        endDate = currentDate.plusWeeks(52),
        firstVisibleWeekDate = currentDate
    )

    // --- NEW: Custom Scroll Tracking ---
    val listState = rememberLazyListState()
    var isCollapsed by remember { mutableStateOf(false) }

    // This intercepts swipe gestures anywhere on the screen, even if the list has only 1 item
    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val delta = available.y
                // If swiping UP, collapse the calendar
                if (delta < -15f) {
                    isCollapsed = true
                }
                // If swiping DOWN and at the very top of the list, expand the calendar
                if (delta > 15f && listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0) {
                    isCollapsed = false
                }
                // Return Offset.Zero so the LazyColumn can still scroll normally
                return Offset.Zero
            }
        }
    }

    // Sync Week view to Month view when Month changes
    LaunchedEffect(state.firstVisibleMonth) {
        if (!isCollapsed) {
            val targetMonth = state.firstVisibleMonth.yearMonth
            // Look at Thursday (day 3) to determine the "dominant" month of the current week
            val dominantDate = weekState.firstVisibleWeek.days.getOrNull(3)?.date ?: weekState.firstVisibleWeek.days.first().date
            if (YearMonth.from(dominantDate) != targetMonth) {
                weekState.scrollToWeek(targetMonth.atDay(1))
            }
        }
    }

    // Sync Month view to Week view when Week changes
    LaunchedEffect(weekState.firstVisibleWeek) {
        if (isCollapsed) {
            val dominantDate = weekState.firstVisibleWeek.days.getOrNull(3)?.date ?: weekState.firstVisibleWeek.days.first().date
            val targetMonth = YearMonth.from(dominantDate)
            if (state.firstVisibleMonth.yearMonth != targetMonth) {
                state.scrollToMonth(targetMonth)
            }
        }
    }

    // Entry palette
    val entrySurface = if (isDark) Color(0xFF141820) else Color(0xFFF5F7F9)
    val entrySoft    = if (isDark) Color(0xFF1B2029) else Color(0xFFE6EAF0)
    val entryText    = if (isDark) Color(0xFFF5F7FA) else Color(0xFF0F172A)
    val entrySub     = if (isDark) Color(0xFFBFC6D1) else Color(0xFF64748B)
    val entryAccent  = if (isDark) Color(0xFFF5F7FA) else Color(0xFF0F1114)

    // Helper Composable for Day Cells (Shared by Month and Week calendars)
    @Composable
    fun DayCell(date: LocalDate, isCurrentMonth: Boolean) {
        val isToday = date == LocalDate.now()

        val inCycle = cycles.any { c ->
            !date.isBefore(c.startDate) &&
                    (c.endDate?.let { !date.isAfter(it) } ?: true)
        }

        val isFertile = prediction?.fertileWindow?.let { date in it } == true
        val isOvulation = prediction?.ovulationDay == date

        val isPredictedPeriod = prediction?.let { pred ->
            val start = pred.mostLikelyPeriodStart
            val len = pred.periodLength ?: 5
            !date.isBefore(start) && date.isBefore(start.plusDays(len.toLong()))
        } == true

        val accentFill = if (isDark) Color.Black else Color.White
        val numberText = Color.White
        val numberTextToday = if (isDark) Color.White else Color(0xFF000000)

        Column(
            modifier = Modifier
                .size(40.dp)
                .padding(vertical = 6.dp, horizontal = 2.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            when {
                isToday -> {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(accentFill),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            date.dayOfMonth.toString(),
                            color = numberTextToday,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                isOvulation -> {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFFFF4081))
                            .size(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            date.dayOfMonth.toString(),
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                isFertile -> {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFFFF4081).copy(alpha = 0.25f))
                            .size(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            date.dayOfMonth.toString(),
                            color = numberText.copy(alpha = 0.9f),
                            fontSize = 13.sp
                        )
                    }
                }
                isPredictedPeriod && isCurrentMonth -> {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color.Red.copy(alpha = 0.2f))
                            .size(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            date.dayOfMonth.toString(),
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                inCycle && isCurrentMonth -> {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(accentFill.copy(alpha = 0.10f))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            date.dayOfMonth.toString(),
                            color = numberText,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                else -> {
                    val alpha = if (isCurrentMonth) 1f else 0.35f
                    Text(
                        date.dayOfMonth.toString(),
                        color = numberText.copy(alpha = alpha),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .padding(top = 4.dp)
            .padding(horizontal = 16.dp)
            .nestedScroll(nestedScrollConnection) // Attach the scroll interceptor here
    ) {
        // Calendar card
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(24.dp))
                    .background(Brush.verticalGradient(listOf(gradTop, gradMid, gradBottom)))
                    .background(Color.White.copy(alpha = 0.06f))
            ) {
                Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {

                    // Month header
                    val scope = rememberCoroutineScope()

                    val headerText = if (isCollapsed) {
                        val currentWeek = weekState.firstVisibleWeek
                        val dominantDate = currentWeek.days.getOrNull(3)?.date ?: currentWeek.days.first().date
                        val formatter = DateTimeFormatter.ofPattern("MMM yyyy")
                        dominantDate.format(formatter).replaceFirstChar {
                            if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
                        }
                    } else {
                        state.firstVisibleMonth.yearMonth.month.getDisplayName(
                            TextStyle.FULL, Locale.getDefault()
                        ).replaceFirstChar {
                            if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
                        } + " " + state.firstVisibleMonth.yearMonth.year
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "‹",
                            color = onGradientMuted,
                            fontSize = 20.sp,
                            modifier = Modifier
                                .width(36.dp)
                                .padding(vertical = 6.dp)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    scope.launch {
                                        if (isCollapsed) {
                                            weekState.animateScrollToWeek(weekState.firstVisibleWeek.days.first().date.minusWeeks(1))
                                        } else {
                                            state.animateScrollToMonth(state.firstVisibleMonth.yearMonth.minusMonths(1))
                                        }
                                    }
                                },
                            textAlign = TextAlign.Center
                        )

                        Text(
                            text = headerText,
                            color = onGradient,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center
                        )

                        Text(
                            "›",
                            color = onGradientMuted,
                            fontSize = 20.sp,
                            modifier = Modifier
                                .width(36.dp)
                                .padding(vertical = 6.dp)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    scope.launch {
                                        if (isCollapsed) {
                                            weekState.animateScrollToWeek(weekState.firstVisibleWeek.days.first().date.plusWeeks(1))
                                        } else {
                                            state.animateScrollToMonth(state.firstVisibleMonth.yearMonth.plusMonths(1))
                                        }
                                    }
                                },
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(Modifier.height(10.dp))

                    // Weekday labels
                    val weekdayLabels = listOf("SUN", "MON", "TUE", "WED", "THU", "FRI", "SAT")
                    Row(Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 2.dp)
                    ) {
                        weekdayLabels.forEach {
                            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                                Text(
                                    it,
                                    color = onGradientMuted,
                                    fontSize = 11.sp,
                                    letterSpacing = 0.5.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    // Animated Calendar Switcher
                    AnimatedContent(
                        targetState = isCollapsed,
                        transitionSpec = {
                            fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300))
                        },
                        label = "calendar_collapse"
                    ) { collapsed ->
                        if (collapsed) {
                            WeekCalendar(
                                state = weekState,
                                dayContent = { day ->
                                    DayCell(date = day.date, isCurrentMonth = true)
                                }
                            )
                        } else {
                            HorizontalCalendar(
                                state = state,
                                dayContent = { day ->
                                    DayCell(
                                        date = day.date,
                                        isCurrentMonth = day.position == DayPosition.MonthDate
                                    )
                                }
                            )
                        }
                    }

                    Spacer(Modifier.height(6.dp))
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        var cycleToEdit by remember { mutableStateOf<PeriodViewModel.Cycle?>(null) }
        LaunchedEffect(cycles.size) {
            if (cycles.isNotEmpty()) {
                listState.animateScrollToItem(0)
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(24.dp)),
            state = listState,
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(
                top = 8.dp,
                bottom = 115.dp
            )
        ){
            items(cycles, key = { it.id }) { cycle ->
                SwipeToDeleteCard(
                    onDelete = { viewModel.deleteCycle(cycle.id) }
                ) {
                    EntryRow(
                        monthLabel = cycle.startDate.month.getDisplayName(
                            TextStyle.SHORT,
                            Locale.getDefault()
                        ).uppercase(),
                        dayNumber = cycle.startDate.dayOfMonth.toString(),
                        startDate = cycle.startDate.toString(),
                        endDate = cycle.endDate?.toString() ?: "",
                        bleeding = cycle.bleeding,
                        bloodColor = cycle.bloodColor,
                        crampsPain = cycle.painLevel,
                        surface = entrySurface,
                        soft = entrySoft,
                        text = entryText,
                        sub = entrySub,
                        accent = entryAccent,
                        onEditClick = { cycleToEdit = cycle }
                    )
                }
            }
        }

        if (cycleToEdit != null) {
            EditCycleDialog(
                cycle = cycleToEdit!!,
                surface = entrySurface,
                text = entryText,
                sub = entrySub,
                accent = entryAccent,
                onDismiss = { cycleToEdit = null },
                onSave = { updatedCycle ->
                    viewModel.updateCycle(updatedCycle)
                    cycleToEdit = null
                }
            )
        }
    }
}

// ---------- Swipe row, entry row ----------
@Composable
fun SwipeToDeleteCard(
    onDelete: () -> Unit,
    content: @Composable () -> Unit
) {
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()

    val offsetX = remember { Animatable(0f) }
    val revealDp = 180.dp
    val deleteThreshold = with(density) { revealDp.toPx() }
    val maxRevealPx = with(density) { (revealDp * 1.2f).toPx() }

    var widthPx by remember { mutableStateOf(0f) }

    val itemShape = RoundedCornerShape(22.dp)

    val bounceSpring = spring<Float>(
        dampingRatio = Spring.DampingRatioLowBouncy,
        stiffness = Spring.StiffnessMedium
    )

    val isRevealed by remember {
        derivedStateOf { offsetX.value.absoluteValue > deleteThreshold / 4f }
    }

    Card(
        shape = itemShape,
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .onSizeChanged { widthPx = it.width.toFloat() }
    ) {
        Box {
            // Background
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .graphicsLayer { shape = itemShape; clip = true }
                    .background(
                        Color.Red.copy(alpha = if (offsetX.value.absoluteValue > 10f) 0.8f else 0f)
                    )
                    .padding(horizontal = 35.dp),
                contentAlignment = if (offsetX.value >= 0f) Alignment.CenterStart else Alignment.CenterEnd
            ) {
                if (isRevealed) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (offsetX.value < 0f) Spacer(Modifier.weight(1f))
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                        if (offsetX.value > 0f) Spacer(Modifier.weight(1f))
                    }

                    val sideWidth = 140.dp
                    val overlayModifier = if (offsetX.value >= 0f) {
                        Modifier.fillMaxHeight().width(sideWidth).align(Alignment.CenterStart)
                    } else {
                        Modifier.fillMaxHeight().width(sideWidth).align(Alignment.CenterEnd)
                    }

                    Box(
                        modifier = overlayModifier
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
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

            // Foreground
            Box(
                modifier = Modifier
                    .graphicsLayer { shape = itemShape; clip = true }
                    .offset { IntOffset(offsetX.value.toInt(), 0) }
                    .pointerInput(Unit) {
                        detectHorizontalDragGestures(
                            onDragEnd = {
                                scope.launch {
                                    offsetX.stop()
                                    val target = when {
                                        offsetX.value <= -deleteThreshold -> -widthPx
                                        offsetX.value >= deleteThreshold -> widthPx
                                        else -> {
                                            val anchors = listOf(-deleteThreshold / 2f, 0f, deleteThreshold / 2f)
                                            anchors.minBy { kotlin.math.abs(it - offsetX.value) }
                                        }
                                    }
                                    if (kotlin.math.abs(target) == kotlin.math.abs(widthPx) && widthPx > 0f) {
                                        offsetX.animateTo(target, bounceSpring)
                                        onDelete()
                                    } else {
                                        offsetX.animateTo(target, bounceSpring)
                                    }
                                }
                            }
                        ) { change, dragAmount ->
                            change.consume()
                            scope.launch {
                                val newValue = (offsetX.value + dragAmount).coerceIn(-maxRevealPx, maxRevealPx)
                                offsetX.snapTo(newValue)
                            }
                        }
                    }
            ) {
                content()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditCycleDialog(
    cycle: PeriodViewModel.Cycle,
    surface: Color,
    text: Color,
    sub: Color,
    accent: Color,
    onDismiss: () -> Unit,
    onSave: (PeriodViewModel.Cycle) -> Unit
) {
    // Local state for all editable fields
    var startDate by remember { mutableStateOf(cycle.startDate) }
    var endDate by remember { mutableStateOf(cycle.endDate) }
    var bleeding by remember { mutableStateOf(cycle.bleeding) }
    var bloodColor by remember { mutableStateOf(cycle.bloodColor) }
    var painLevel by remember { mutableIntStateOf(cycle.painLevel) }

    // State for showing date pickers
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }

    val formatter = remember { DateTimeFormatter.ofPattern("MMM dd, yyyy") }

    // Theme colors matching AddCycleDialog
    val isDark = androidx.compose.foundation.isSystemInDarkTheme()
    val gradTop = if (isDark) Color(0xFF7B8FA3) else Color(0xFF8FA0B1)
    val gradMid = if (isDark) Color(0xFF7288A0) else Color(0xFF8799B0)
    val gradBottom = if (isDark) Color(0xFF5A7396) else Color(0xFF6E87A7)
    val onGradient = Color.White
    val contentBg = if (isDark) Color.Black else Color.White
    val textPrimary = if (isDark) Color(0xFFF5F7FA) else Color(0xFF0F172A)
    val textSub = if (isDark) Color(0xFFBFC6D1) else Color(0xFF64748B)

    val bleedingOptions = listOf("Heavy", "Medium", "Light", "Spotting")
    val colorOptions = listOf("Bright Red", "Dark Red", "Brown")

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = true)
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(24.dp))
                    .background(androidx.compose.ui.graphics.Brush.verticalGradient(listOf(gradTop, gradMid, gradBottom)))
                    .background(Color.White.copy(alpha = 0.06f))
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {

                    // --- CENTERED TITLE ---
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 18.dp, start = 22.dp, end = 22.dp, bottom = 12.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "Edit Cycle",
                            color = onGradient,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp
                            ),
                            textAlign = TextAlign.Center
                        )
                    }

                    // Content Box
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(contentBg)
                            .padding(20.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // --- DATES SECTION ---
                            Column {
                                Text("Dates", color = textPrimary, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                Spacer(Modifier.height(10.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Start", color = textSub, fontSize = 12.sp)
                                        Spacer(Modifier.height(4.dp))
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(textPrimary.copy(alpha = 0.05f))
                                                .clickable { showStartDatePicker = true }
                                                .padding(12.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(startDate.format(formatter), color = textPrimary, fontSize = 13.sp)
                                        }
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("End", color = textSub, fontSize = 12.sp)
                                        Spacer(Modifier.height(4.dp))
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(textPrimary.copy(alpha = 0.05f))
                                                .clickable { showEndDatePicker = true }
                                                .padding(12.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(endDate?.format(formatter) ?: "Ongoing", color = textPrimary, fontSize = 13.sp)
                                        }
                                    }
                                }
                            }

                            // --- BLEEDING INTENSITY (Segmented Style) ---
                            Text("Bleeding", color = textPrimary, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                bleedingOptions.forEach { option ->
                                    val isSelected = bleeding.equals(option, ignoreCase = true)
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(if (isSelected) gradBottom else textPrimary.copy(alpha = 0.05f))
                                            .clickable { bleeding = option }
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = option,
                                            color = if (isSelected) onGradient else textPrimary,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }

                            // --- BLOOD COLOR (Segmented Style) ---
                            Text("Blood Color", color = textPrimary, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                colorOptions.forEach { option ->
                                    val isSelected = bloodColor.equals(option, ignoreCase = true)
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(if (isSelected) gradBottom else textPrimary.copy(alpha = 0.05f))
                                            .clickable { bloodColor = option }
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = option,
                                            color = if (isSelected) onGradient else textPrimary,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }

                            // --- PAIN LEVEL ---
                            Text("Cramps / Pain", color = textPrimary, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Text("Level: $painLevel / 10", color = textPrimary, fontSize = 14.sp)
                            Slider(
                                value = painLevel.toFloat(),
                                onValueChange = { painLevel = it.toInt() },
                                valueRange = 0f..10f,
                                steps = 9,
                                colors = SliderDefaults.colors(
                                    thumbColor = gradBottom,
                                    activeTrackColor = gradBottom,
                                    inactiveTrackColor = textPrimary.copy(alpha = 0.1f)
                                )
                            )
                        }
                    }

                    // --- BOTTOM ACTIONS ---
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        TextButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp)
                                .clip(RoundedCornerShape(26.dp))
                                .background(Color.White.copy(alpha = 0.15f)),
                            colors = ButtonDefaults.textButtonColors(contentColor = onGradient)
                        ) {
                            Text("Close", fontWeight = FontWeight.Medium)
                        }

                        Button(
                            onClick = {
                                onSave(
                                    cycle.copy(
                                        startDate = startDate,
                                        endDate = endDate,
                                        bleeding = bleeding,
                                        bloodColor = bloodColor,
                                        painLevel = painLevel
                                    )
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = contentBg,
                                contentColor = if (isDark) Color.White else Color.Black
                            ),
                            shape = RoundedCornerShape(26.dp)
                        ) {
                            Text("Save", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // Reuse pickers
    if (showStartDatePicker) {
        MinimalDatePickerDialog(
            title = "Pick Start Date",
            brand = MaterialTheme.colorScheme.primary,
            gradTop = gradTop, gradMid = gradMid, gradBottom = gradBottom,
            onGradient = onGradient,
            buttonContainer = if (isDark) Color.Black else Color.White,
            buttonContent = if (isDark) Color.White else Color.Black,
            onDismiss = { showStartDatePicker = false },
            onConfirm = { ms ->
                millisToLocalDate(ms)?.let { startDate = it }
                showStartDatePicker = false
            }
        )
    }
    if (showEndDatePicker) {
        MinimalDatePickerDialog(
            title = "Pick End Date",
            brand = MaterialTheme.colorScheme.primary,
            gradTop = gradTop, gradMid = gradMid, gradBottom = gradBottom,
            onGradient = onGradient,
            buttonContainer = if (isDark) Color.Black else Color.White,
            buttonContent = if (isDark) Color.White else Color.Black,
            onDismiss = { showEndDatePicker = false },
            onConfirm = { ms ->
                millisToLocalDate(ms)?.let {
                    if (!it.isBefore(startDate)) {
                        endDate = it
                    }
                }
                showEndDatePicker = false
            }
        )
    }
}

@Composable
fun EntryRow(
    monthLabel: String,
    dayNumber: String,
    startDate: String,
    endDate: String,
    bleeding: String,
    bloodColor: String,
    crampsPain: Int,
    surface: Color,
    soft: Color,
    text: Color,
    sub: Color,
    accent: Color,
    onEditClick: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val rotation by animateFloatAsState(if (expanded) 180f else 0f, label = "arrowRotation")

    // Theme Gradient for Progress Bar
    val isDark = isSystemInDarkTheme()
    val gradTop = if (isDark) Color(0xFF7B8FA3) else Color(0xFF8FA0B1)
    val gradMid = if (isDark) Color(0xFF7288A0) else Color(0xFF8799B0)
    val gradBottom = if (isDark) Color(0xFF5A7396) else Color(0xFF6E87A7)
    val themeGradient = remember { Brush.horizontalGradient(listOf(gradTop, gradMid, gradBottom)) }

    // --- Formatters & Helpers ---
    fun pretty(d: String): String = runCatching {
        if (d.isBlank()) return@runCatching "Not set"
        val date = LocalDate.parse(d)
        val m = date.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())
        "$m ${date.dayOfMonth}, ${date.year}"
    }.getOrElse { d }

    fun shortPretty(d: String): String = runCatching {
        if (d.isBlank()) return@runCatching "?"
        val date = LocalDate.parse(d)
        "${date.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())} ${date.dayOfMonth}"
    }.getOrElse { d }

    fun painLabel(p: Int): String = when {
        p <= 0 -> "None"
        p in 1..3 -> "Mild ($p/10)"
        p in 4..6 -> "Moderate ($p/10)"
        p in 7..8 -> "Severe ($p/10)"
        else -> "Very severe ($p/10)"
    }

    // --- Precise Time-Based Progress Calculation ---
    val startDt = remember(startDate) { runCatching { LocalDate.parse(startDate) }.getOrNull() }
    val endDt = remember(endDate) { runCatching { if (endDate.isNotBlank()) LocalDate.parse(endDate) else null }.getOrNull() }

    // Grab the exact current time down to the minute
    val now = LocalDateTime.now()
    val today = now.toLocalDate()

    var progTarget = 0f
    var statusText = "Status unknown"
    var totalDaysForBar = 0L
    var elapsedDays = 0L
    var compliment = ""

    if (startDt != null) {
        val startDateTime = startDt.atStartOfDay() // Assumes cycle starts at midnight

        if (endDt == null) {
            // ONGOING CYCLE
            elapsedDays = ChronoUnit.DAYS.between(startDt, today) + 1
            statusText = "Day $elapsedDays • Ongoing"
            totalDaysForBar = maxOf(6L, elapsedDays + 2L) // Dynamic projection for dots

            // Calculate exact progress smoothly using minutes instead of whole days
            val elapsedMinutes = ChronoUnit.MINUTES.between(startDateTime, now)
            val totalMinutesForBar = totalDaysForBar * 24f * 60f
            progTarget = (elapsedMinutes.toFloat() / totalMinutesForBar).coerceIn(0f, 0.95f)

            compliment = when (elapsedDays) {
                1L -> "Take it easy today. You've got this! 💙"
                2L -> "Be kind to yourself today. 🍵"
                3L -> "You're doing great, keep resting. ✨"
                4L -> "Almost through the toughest part! 💪"
                5L -> "Home stretch! Keep it up! 🌸"
                else -> "Listen to your body. You're doing great! 🙌"
            }
        } else {
            // BOUNDED / COMPLETED CYCLE
            totalDaysForBar = ChronoUnit.DAYS.between(startDt, endDt) + 1

            if (today.isAfter(endDt)) {
                statusText = "$totalDaysForBar Days • Completed"
                progTarget = 1f
                compliment = "Cycle completed. Awesome job! 🌟"
            } else if (today.isBefore(startDt)) {
                statusText = "Upcoming"
                progTarget = 0f
                compliment = "Coming up soon! Prepare your comfort kit. 🎒"
            } else {
                val daysLeft = ChronoUnit.DAYS.between(today, endDt)
                statusText = if (daysLeft == 0L) "Ends today" else "$daysLeft days left"
                elapsedDays = ChronoUnit.DAYS.between(startDt, today) + 1

                // Calculate exact progress smoothly using minutes
                val elapsedMinutes = ChronoUnit.MINUTES.between(startDateTime, now)
                val totalMinutesForBar = totalDaysForBar * 24f * 60f
                progTarget = (elapsedMinutes.toFloat() / totalMinutesForBar).coerceIn(0f, 1f)

                compliment = when (elapsedDays) {
                    1L -> "Day 1: Take it easy today. You've got this! 💙"
                    2L -> "Day 2: Remember to stay hydrated. 🍵"
                    totalDaysForBar -> "Final day! You're almost at the finish line. 🎉"
                    else -> "Day $elapsedDays: You're doing great! ✨"
                }
            }
        }
    }

    val animatedProgress by animateFloatAsState(
        targetValue = progTarget,
        animationSpec = tween(1200),
        label = "progressAnim"
    )

    // --- UI Render ---
    Card(
        colors = CardDefaults.cardColors(containerColor = surface),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { expanded = !expanded }
            )
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            // Header Row
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                BleedingIcon(bleeding = bleeding)
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = when (bleeding.lowercase(Locale.getDefault())) {
                            "heavy" -> "Heavy Bleeding"
                            "medium" -> "Moderate Bleeding"
                            "light" -> "Light Bleeding"
                            else -> "Spotting"
                        },
                        color = text,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text("$monthLabel $dayNumber", color = sub, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }

                IconButton(onClick = onEditClick) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit Entry", tint = text, modifier = Modifier.size(18.dp))
                }

                IconButton(onClick = { expanded = !expanded }) {
                    Icon(
                        imageVector = Icons.Filled.ArrowDropDown,
                        contentDescription = if (expanded) "Collapse" else "Expand",
                        tint = text,
                        modifier = Modifier.size(24.dp).graphicsLayer { rotationZ = rotation }
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            // Progress Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(statusText, color = text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    MetaPill("Color", bloodColor, text, sub, soft)
                }
            }

            Spacer(Modifier.height(8.dp))

            // Custom Dotted Canvas Progress Bar
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(20.dp)
                    .clip(CircleShape)
            ) {
                val width = size.width
                val height = size.height

                // 1. Draw background track
                drawRect(color = soft, size = size)

                // 2. Draw gradient fill based on smoothly animated progress
                drawRect(
                    brush = themeGradient,
                    size = Size(animatedProgress * width, height)
                )

                // 3. Draw small dots for daily break points
                if (totalDaysForBar > 1) {
                    val segmentWidth = width / totalDaysForBar
                    val dotRadius = 2.dp.toPx()

                    for (i in 1 until totalDaysForBar.toInt()) {
                        val xPos = i * segmentWidth
                        drawCircle(
                            color = surface,
                            radius = dotRadius,
                            center = Offset(xPos, height / 2f)
                        )
                    }
                }
            }

            Spacer(Modifier.height(6.dp))

            // Motivational Compliment
            if (compliment.isNotEmpty()) {
                Text(
                    text = compliment,
                    color = gradTop,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    fontStyle = FontStyle.Italic
                )
            }

            // Expanded Content (Info Grid)
            AnimatedVisibility(visible = expanded) {
                Column {
                    Spacer(Modifier.height(14.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(16.dp))
                                .background(soft)
                                .padding(12.dp)
                        ) {
                            Text("Duration", color = sub, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "${shortPretty(startDate)} - ${if(endDate.isNotBlank()) shortPretty(endDate) else "Ongoing"}",
                                color = text,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(16.dp))
                                .background(soft)
                                .padding(12.dp)
                        ) {
                            Text("Cramps / Pain", color = sub, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = painLabel(crampsPain),
                                color = text,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MetaPill(label: String, value: String, text: Color, sub: Color, bg: Color) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp)) // Modern squared-off pill
            .background(bg)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(sub.copy(alpha = 0.5f)))
        Spacer(Modifier.width(6.dp))
        Text(
            value,
            color = text,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.widthIn(max = 100.dp)
        )
    }
}

@Composable
fun BleedingIcon(bleeding: String) {
    val resId = when (bleeding.lowercase(Locale.getDefault())) {
        "heavy" -> R.drawable.heavy_bleeding
        "medium" -> R.drawable.medium_bleeding
        "light" -> R.drawable.light_bleeding
        else -> R.drawable.spotting
    }

    // Wrapped in a soft background box to make the icon pop
    Box(
        modifier = Modifier
            .size(42.dp)
            .clip(CircleShape)
            .background(Color.Gray.copy(alpha = 0.1f)),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = resId),
            contentDescription = "Bleeding level",
            modifier = Modifier.size(24.dp),
            contentScale = ContentScale.Fit
        )
    }
}
