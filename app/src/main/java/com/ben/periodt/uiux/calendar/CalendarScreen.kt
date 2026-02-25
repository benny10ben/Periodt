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
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.EventAvailable
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.LocalCafe
import androidx.compose.material.icons.rounded.Opacity
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ben.periodt.ui.theme.BricolageGrotesque
import com.ben.periodt.uiux.shared.Prediction
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
import androidx.compose.material.icons.rounded.Grain
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import com.ben.periodt.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen() {
    val context = LocalContext.current.applicationContext as Application
    val viewModel: PeriodViewModel = viewModel(factory = PeriodViewModel.Factory(context))
    val cycles by viewModel.cycles.collectAsState()
    val prediction by viewModel.prediction.collectAsState()
    val isDark = isSystemInDarkTheme()

    val sortedCycles = remember(cycles) { cycles.sortedByDescending { it.startDate } }
    val currentMonth = remember { YearMonth.now() }
    val currentDate = remember { LocalDate.now() }

    // --- STATES ---
    val state = rememberCalendarState(
        startMonth = currentMonth.minusMonths(12),
        endMonth = currentMonth.plusMonths(12),
        firstVisibleMonth = currentMonth
    )
    val weekState = rememberWeekCalendarState(
        startDate = currentDate.minusWeeks(52),
        endDate = currentDate.plusWeeks(52),
        firstVisibleWeekDate = currentDate
    )

    val listState = rememberLazyListState()
    var isCollapsed by remember { mutableStateOf(false) }

    // --- SMOOTH SCROLL LOGIC ---
    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val delta = available.y

                // 1. Swiping UP (Collapse)
                if (delta < 0 && !isCollapsed) {
                    isCollapsed = true
                    // Consuming the scroll ensures the list DOESN'T scroll while the calendar is shrinking.
                    // This forces the UI to prioritize the resize animation.
                    return Offset(0f, available.y)
                }

                // 2. Swiping DOWN (Expand)
                if (delta > 0 && isCollapsed && listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0) {
                    isCollapsed = false
                    // Consume scroll to prevent list from pulling down before calendar expands
                    return Offset(0f, available.y)
                }

                return Offset.Zero
            }
        }
    }

    // --- SYNC LOGIC (Unchanged) ---
    LaunchedEffect(state.firstVisibleMonth) {
        if (!isCollapsed) {
            val targetMonth = state.firstVisibleMonth.yearMonth
            val dominantDate = weekState.firstVisibleWeek.days.getOrNull(3)?.date ?: weekState.firstVisibleWeek.days.first().date
            if (YearMonth.from(dominantDate) != targetMonth) weekState.scrollToWeek(targetMonth.atDay(1))
        }
    }
    LaunchedEffect(weekState.firstVisibleWeek) {
        if (isCollapsed) {
            val dominantDate = weekState.firstVisibleWeek.days.getOrNull(3)?.date ?: weekState.firstVisibleWeek.days.first().date
            val targetMonth = YearMonth.from(dominantDate)
            if (state.firstVisibleMonth.yearMonth != targetMonth) state.scrollToMonth(targetMonth)
        }
    }

    // --- COLORS ---
    val entrySurface = if (isDark) Color(0xFF1B1B1B) else Color.White
    val entryText = if (isDark) Color.White else Color(0xFF0F172A)
    val entrySub = if (isDark) Color.White.copy(alpha = 0.6f) else Color(0xFF64748B)

    var cycleToEdit by remember { mutableStateOf<PeriodViewModel.Cycle?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .nestedScroll(nestedScrollConnection)
    ) {
        Spacer(Modifier.height(8.dp))
        CalendarLegend()

        // --- WRAPPER FOR SMOOTH RESIZE ---
        // animateContentSize() here is the magic. It tells the Column:
        // "When the child (CalendarCard) changes size, animate my own size smoothly."
        // This pushes the LazyColumn below it up or down with a spring animation.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .animateContentSize(
                    animationSpec = tween(durationMillis = 300, easing = LinearOutSlowInEasing)
                )
        ) {
            CalendarCard(isCollapsed, state, weekState, cycles, prediction)
        }

        // --- CONTENT LIST ---
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f) // Fills remaining space dynamically
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)),
            state = listState,
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 115.dp)
        ) {
            item { WellnessCardsRow(cycles = cycles, prediction = prediction) }
            item { PredictionBanner(prediction, cycles, entrySurface, entryText, entrySub) }

            items(sortedCycles, key = { it.id }) { cycle ->
                SwipeToDeleteCard(onDelete = { viewModel.deleteCycle(cycle.id) }) { isSwiping ->
                    EntryRow(
                        monthLabel = cycle.startDate.month.getDisplayName(TextStyle.SHORT, Locale.getDefault()).uppercase(),
                        dayNumber = cycle.startDate.dayOfMonth.toString(),
                        startDate = cycle.startDate.toString(),
                        endDate = cycle.endDate?.toString() ?: "",
                        bleeding = cycle.bleeding,
                        bloodColor = cycle.bloodColor,
                        crampsPain = cycle.painLevel,
                        surface = entrySurface,
                        soft = Color.Transparent,
                        text = entryText,
                        sub = entrySub,
                        accent = entryText,
                        isSwiping = isSwiping,
                        onEditClick = { cycleToEdit = cycle }
                    )
                }
            }
        }

        if (cycleToEdit != null) {
            EditCycleDialog(
                cycle = cycleToEdit!!,
                onDismiss = { cycleToEdit = null },
                onSave = { viewModel.updateCycle(it); cycleToEdit = null }
            )
        }
    }
}

@Composable
fun CalendarLegend() {
    val isDark = isSystemInDarkTheme()
    val textSub = if (isDark) Color.White.copy(alpha = 0.6f) else Color(0xFF64748B)

    // Using your established colors from the provided code
    val ColorPeriodSolid = Color(0xFFA5231C)
    val ColorFertileSolid = Color(0xFF2A3825)

    // Center the entire legend row
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 1. Completed Period (Alpha applied)
        LegendItem(
            color = ColorPeriodSolid.copy(alpha = 0.6f),
            label = "Logged",
            textColor = textSub
        )

        Spacer(Modifier.width(16.dp))

        // 2. Fertile Window
        LegendItem(
            color = ColorFertileSolid,
            label = "Fertile",
            textColor = textSub
        )

        Spacer(Modifier.width(16.dp))

        // 3. Upcoming Period (Solid)
        LegendItem(
            color = ColorPeriodSolid,
            label = "Predicted",
            textColor = textSub
        )
    }
}

@Composable
private fun LegendItem(color: Color, label: String, textColor: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(width = 20.dp, height = 8.dp)
                .clip(RoundedCornerShape(100.dp)) // Matching your "Stadium" strip radius
                .background(color)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = label,
            fontFamily = BricolageGrotesque,
            fontSize = 12.sp,
            color = textColor,
            fontWeight = FontWeight.Medium
        )
    }
}

private val ColorPeriodSolid = Color(0xFFA5231C)
private val ColorFertileSolid = Color(0xFF2A3825)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarCard(
    isCollapsed: Boolean,
    state: CalendarState,
    weekState: WeekCalendarState,
    cycles: List<PeriodViewModel.Cycle>,
    prediction: Prediction?
) {
    val isDark = isSystemInDarkTheme()
    val scope = rememberCoroutineScope()

    // Pure Black Theme Background
    val backgroundBrush = if (isDark) {
        Brush.linearGradient(colors = listOf(Color(0xFF1b1b1b), Color(0xFF1b1b1b)))
    } else {
        Brush.linearGradient(colors = listOf(Color(0xFFffffff), Color(0xFFffffff)))
    }

    val onCardContent = if (isDark) Color.White else Color.Black
    val onCardContentMuted = onCardContent.copy(alpha = 0.70f)

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
    ) {
        Box(
            modifier = Modifier
                .background(backgroundBrush)
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            Column {
                // Header Title Logic
                val headerText = if (isCollapsed) {
                    val currentWeek = weekState.firstVisibleWeek
                    val dominantDate = currentWeek.days.getOrNull(3)?.date ?: currentWeek.days.first().date
                    dominantDate.format(DateTimeFormatter.ofPattern("MMM yyyy"))
                } else {
                    state.firstVisibleMonth.yearMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault()) + " " + state.firstVisibleMonth.yearMonth.year
                }

                // Header Row
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text("‹", color = onCardContentMuted, fontSize = 24.sp, fontFamily = BricolageGrotesque, modifier = Modifier.width(40.dp).clickable {
                        scope.launch { if (isCollapsed) weekState.animateScrollToWeek(weekState.firstVisibleWeek.days.first().date.minusWeeks(1)) else state.animateScrollToMonth(state.firstVisibleMonth.yearMonth.minusMonths(1)) }
                    }, textAlign = TextAlign.Center)

                    Text(headerText, fontFamily = BricolageGrotesque, color = onCardContent, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold), modifier = Modifier.weight(1f), textAlign = TextAlign.Center)

                    Text("›", color = onCardContentMuted, fontSize = 24.sp, fontFamily = BricolageGrotesque, modifier = Modifier.width(40.dp).clickable {
                        scope.launch { if (isCollapsed) weekState.animateScrollToWeek(weekState.firstVisibleWeek.days.first().date.plusWeeks(1)) else state.animateScrollToMonth(state.firstVisibleMonth.yearMonth.plusMonths(1)) }
                    }, textAlign = TextAlign.Center)
                }

                Spacer(Modifier.height(10.dp))

                // Days of Week
                Row(Modifier.fillMaxWidth()) {
                    listOf("SUN", "MON", "TUE", "WED", "THU", "FRI", "SAT").forEach { label ->
                        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                            Text(label, color = onCardContentMuted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, fontFamily = BricolageGrotesque)
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                // View Switcher (Snap Animation)
                AnimatedContent(
                    targetState = isCollapsed,
                    label = "calendar_collapse",
                    transitionSpec = {
                        fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300))
                    }
                ) { collapsed ->
                    if (collapsed) {
                        WeekCalendar(state = weekState, dayContent = { DayCellEnhanced(it.date, true, cycles, prediction) })
                    } else {
                        HorizontalCalendar(state = state, dayContent = { DayCellEnhanced(it.date, it.position == DayPosition.MonthDate, cycles, prediction) })
                    }
                }
                Spacer(Modifier.height(6.dp))
            }
        }
    }
}

// --- UPDATED DAY CELL WITH INFINITE STRIP FIX ---
@Composable
fun DayCellEnhanced(
    date: LocalDate,
    isCurrentMonth: Boolean,
    cycles: List<PeriodViewModel.Cycle>,
    prediction: Prediction?
) {
    val isDark = isSystemInDarkTheme()

    val ovulationBg = if (isDark) Color(0xFF1B1B1B) else Color.White
    val ovulationText = if (isDark) Color.White else Color(0xFF1B1B1B)

    val isOvulation = prediction?.ovulationDay == date
    val isToday = date == LocalDate.now()

    fun checkPhase(d: LocalDate): Int {
        val isLoggedPeriod = cycles.any { c ->
            val start = c.startDate
            val end = c.endDate ?: start.plusDays(6)
            !d.isBefore(start) && !d.isAfter(end)
        }
        if (isLoggedPeriod) return 1

        if (prediction?.let { p ->
                val s = p.mostLikelyPeriodStart
                val l = p.periodLength ?: 5
                !d.isBefore(s) && d.isBefore(s.plusDays(l.toLong()))
            } == true) return 2

        if (prediction?.fertileWindow?.contains(d) == true) return 3

        return 0
    }

    val currentPhase = checkPhase(date)
    val prevPhase = checkPhase(date.minusDays(1))
    val nextPhase = checkPhase(date.plusDays(1))

    val isStart = currentPhase != prevPhase
    val isEnd = currentPhase != nextPhase

    val stripRadius = 100.dp

    val shape = when {
        currentPhase == 0 -> CircleShape
        isStart && isEnd -> RoundedCornerShape(stripRadius)
        isStart -> RoundedCornerShape(topStart = stripRadius, bottomStart = stripRadius)
        isEnd -> RoundedCornerShape(topEnd = stripRadius, bottomEnd = stripRadius)
        else -> RectangleShape
    }

    val padding = when {
        currentPhase == 0 -> PaddingValues(2.dp)
        isStart && isEnd -> PaddingValues(start = 4.dp, top = 4.dp, end = 4.dp, bottom = 4.dp)
        isStart -> PaddingValues(start = 4.dp, top = 4.dp, end = 0.dp, bottom = 4.dp)
        isEnd -> PaddingValues(start = 0.dp, top = 4.dp, end = 4.dp, bottom = 4.dp)
        else -> PaddingValues(start = 0.dp, top = 4.dp, end = 0.dp, bottom = 4.dp)
    }

    val bgColor = when (currentPhase) {
        1 -> ColorPeriodSolid.copy(alpha = 0.6f)
        2 -> ColorPeriodSolid
        3 -> ColorFertileSolid
        else -> Color.Transparent
    }

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .padding(padding)
            .clip(shape)
            .background(bgColor),
        contentAlignment = Alignment.Center
    ) {
        if (isOvulation) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .shadow(if (isDark) 0.dp else 8.dp, CircleShape, spotColor = ovulationBg)
                    .background(ovulationBg, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = date.dayOfMonth.toString(),
                    color = ovulationText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = BricolageGrotesque
                )
            }
        } else if (isToday && currentPhase == 0) {
            // --- HAND-DRAWN SKETCH LOGIC ---
            Box(
                modifier = Modifier
                    .size(42.dp) // Larger container to let the oval "breathe"
                    .padding(2.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.bg_widget_day_today),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        // This makes it an oval like your reference
                        .scale(scaleX = 1.3f, scaleY = 1.0f),
                        // Rotating slightly makes it look hand-drawn, not computer-generated
                    colorFilter = ColorFilter.tint(if (isDark) Color.White else Color.Black)
                )

                DayText(date, isCurrentMonth, false)
            }
        } else {
            val isHighlighted = currentPhase != 0
            DayText(date, isCurrentMonth, isHighlighted)
        }
    }
}

@Composable
fun DayText(date: LocalDate, isCurrentMonth: Boolean, isHighlighted: Boolean) {
    val isDark = isSystemInDarkTheme()

    val alpha = if (isHighlighted || isCurrentMonth) 1f else 0.3f

    val color = if (isDark) {
        if (isHighlighted) Color.White else Color.White.copy(alpha = alpha)
    } else {
        if (isHighlighted) Color.White else Color.Black.copy(alpha = alpha)
    }

    Text(
        text = date.dayOfMonth.toString(),
        color = color,
        fontSize = 14.sp,
        fontWeight = if (isHighlighted) FontWeight.Bold else FontWeight.Normal,
        fontFamily = BricolageGrotesque
    )
}

// ---------- Swipe row, entry row ----------
@Composable
fun SwipeToDeleteCard(
    onDelete: () -> Unit,
    content: @Composable (Boolean) -> Unit
) {
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()

    val offsetX = remember { Animatable(0f) }
    val revealDp = 80.dp // The "Pause" or "Sticky" width
    val revealPx = with(density) { revealDp.toPx() }

    // Threshold to trigger the actual deletion (swipe further to delete)
    val deleteThreshold = with(density) { 180.dp.toPx() }
    val maxRevealPx = with(density) { 220.dp.toPx() }

    var widthPx by remember { mutableStateOf(0f) }
    val itemShape = RoundedCornerShape(22.dp)

    val bounceSpring = spring<Float>(
        dampingRatio = Spring.DampingRatioLowBouncy,
        stiffness = Spring.StiffnessMedium
    )

    val isRevealed by remember {
        derivedStateOf { offsetX.value.absoluteValue > revealPx / 2f }
    }

    val isSwiping by remember {
        derivedStateOf { offsetX.value.absoluteValue > 2f }
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
            // Background (Red delete area)
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .graphicsLayer { shape = itemShape; clip = true }
                    .background(
                        Color.Red.copy(alpha = if (offsetX.value.absoluteValue > 10f) 0.8f else 0f)
                    )
                    .padding(horizontal = 24.dp),
                contentAlignment = if (offsetX.value >= 0f) Alignment.CenterStart else Alignment.CenterEnd
            ) {
                if (isRevealed) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )

                    // This Box makes the red area clickable to delete once paused
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(revealDp)
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

            // Foreground (The Content)
            Box(
                modifier = Modifier
                    .graphicsLayer { shape = itemShape; clip = true }
                    .offset { IntOffset(offsetX.value.toInt(), 0) }
                    .pointerInput(Unit) {
                        detectHorizontalDragGestures(
                            onDragEnd = {
                                scope.launch {
                                    val currentOff = offsetX.value
                                    val target = when {
                                        // 1. Swipe far enough -> DELETE
                                        currentOff <= -deleteThreshold -> -widthPx
                                        currentOff >= deleteThreshold -> widthPx

                                        // 2. Swipe past half-reveal -> PAUSE IN MIDDLE
                                        currentOff <= -revealPx / 2f -> -revealPx
                                        currentOff >= revealPx / 2f -> revealPx

                                        // 3. Otherwise -> RESET
                                        else -> 0f
                                    }

                                    if (kotlin.math.abs(target) == widthPx && widthPx > 0f) {
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
                content(isSwiping)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EditCycleDialog(
    cycle: PeriodViewModel.Cycle,
    onDismiss: () -> Unit,
    onSave: (PeriodViewModel.Cycle) -> Unit
) {
    // --- STATE MANAGEMENT ---
    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker by remember { mutableStateOf(false) }

    var startDate by remember { mutableStateOf(cycle.startDate) }
    var endDate by remember { mutableStateOf(cycle.endDate) }
    var bleeding by remember { mutableStateOf(cycle.bleeding) }
    var bloodColor by remember { mutableStateOf(cycle.bloodColor) }

    var sliderPosition by remember { mutableStateOf(cycle.painLevel.toFloat()) }
    var painLevel by remember { mutableIntStateOf(cycle.painLevel) }

    val bleedingOptions = listOf("Heavy", "Medium", "Light", "Spotting")
    val colorOptions = listOf("Bright Red", "Dark Red", "Brown")

    // --- THEME & COLORS ---
    val isDark = isSystemInDarkTheme()

    // Dark Mode background: Pure Black to Deep Gray
    val contentSurface = if (isDark) {
        Brush.linearGradient(
            0.0f to Color.Black,
            1.0f to Color(0xFF1B1B1B)
        )
    } else {
        Brush.linearGradient(
            colors = listOf(Color(0xFFF8FAFC), Color(0xFFf2f0e3))
        )
    }

    // Yellow accent for Slider and Buttons in Dark Mode
    val accentColor = if (isDark) Color(0xFFD89046) else Color(0xFF2A3825)
    val surfaceFallback = if (isDark) Color.Black else Color.White

    val pastelGreen = Color(0xFF2A3825)
    val pastelOrange = Color(0xFFD89046)
    val pastelMaroon = Color(0xFF4E1A1A)

    val pillBackground = if (isDark) Color.White.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.05f)
    val pillTextColor = if (isDark) Color.White else Color(0xFF1B1B1B)

    val textPrimary = if (isDark) Color.White else Color(0xFF0F172A)
    val textSub = if (isDark) Color.White.copy(alpha = 0.6f) else Color(0xFF64748B)

    val formatter = remember { DateTimeFormatter.ofPattern("MMM dd") }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(vertical = 24.dp) ,
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = surfaceFallback),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(contentSurface)
            ) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp, start = 24.dp, end = 24.dp, bottom = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Edit Entry",
                        fontFamily = BricolageGrotesque,
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
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
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = textPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    // Date Selectors
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        CleanDateCard(
                            label = "Start Date",
                            date = startDate.format(formatter),
                            icon = Icons.Rounded.CalendarToday,
                            bg = pillBackground,
                            textColor = pillTextColor,
                            onClick = { showStartPicker = true },
                            modifier = Modifier.weight(1f)
                        )
                        CleanDateCard(
                            label = "End Date",
                            date = endDate?.format(formatter) ?: "Ongoing",
                            icon = if (endDate == null) Icons.Rounded.Update else Icons.Rounded.EventAvailable,
                            bg = pillBackground,
                            textColor = pillTextColor,
                            onClick = { showEndPicker = true },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Flow Intensity
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "Flow Intensity",
                            fontFamily = BricolageGrotesque,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = textPrimary
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            bleedingOptions.forEach { option ->
                                val isSelected = bleeding.equals(option, ignoreCase = true)
                                val activePillColor = when(option) {
                                    "Heavy" -> pastelMaroon
                                    "Medium" -> pastelOrange
                                    else -> pastelGreen
                                }

                                EntryStylePill(
                                    text = option,
                                    isSelected = isSelected,
                                    activeBg = activePillColor,
                                    activeText = if (isDark) Color.White else Color.White,
                                    inactiveText = textSub,
                                    surface = surfaceFallback,
                                    onClick = { bleeding = option }
                                )
                            }
                        }
                    }

// Blood Color
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "Blood Color",
                            fontFamily = BricolageGrotesque,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = textPrimary
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            colorOptions.forEach { option ->
                                val isSelected = bloodColor.equals(option, ignoreCase = true)

                                // Assigning specific colors based on the blood color option
                                val activePillColor = when(option) {
                                    "Bright Red" -> pastelGreen // Using your existing light red/pink
                                    "Dark Red"   -> Color(0xFF4E1A1A) // Deep Maroon for contrast
                                    "Brown"      -> pastelOrange // Using your existing orange/brown
                                    else         -> accentColor
                                }

                                EntryStylePill(
                                    text = option,
                                    isSelected = isSelected,
                                    activeBg = activePillColor,
                                    activeText = Color.White,
                                    inactiveText = textSub,
                                    surface = surfaceFallback,
                                    onClick = { bloodColor = option }
                                )
                            }
                        }
                    }

                    // Pain Level Slider
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Cramps & Pain",
                                fontFamily = BricolageGrotesque,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = textPrimary
                            )
                            Text(
                                text = "${sliderPosition.toInt()} / 10",
                                fontFamily = BricolageGrotesque,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = accentColor
                            )
                        }
                        Slider(
                            value = sliderPosition,
                            onValueChange = {
                                sliderPosition = it
                                painLevel = it.toInt()
                            },
                            valueRange = 0f..10f,
                            colors = SliderDefaults.colors(
                                thumbColor = accentColor,
                                activeTrackColor = accentColor,
                                inactiveTrackColor = pillBackground
                            )
                        )
                    }

                    // Action Button
                    Button(
                        onClick = {
                            onSave(cycle.copy(
                                startDate = startDate,
                                endDate = endDate,
                                bleeding = bleeding,
                                bloodColor = bloodColor,
                                painLevel = painLevel
                            ))
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .padding(bottom = 8.dp),
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = accentColor,
                            contentColor = Color.White // FORCED WHITE ONLY
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                    ) {
                        Text(
                            text = "Update Entry",
                            fontFamily = BricolageGrotesque,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
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

@Composable
fun EntryStylePill(
    text: String,
    isSelected: Boolean,
    activeBg: Color,
    activeText: Color,
    inactiveText: Color,
    surface: Color,
    onClick: () -> Unit
) {
    val bg = if (isSelected) activeBg else surface
    val txt = if (isSelected) activeText else inactiveText

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontFamily = BricolageGrotesque,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
            color = txt
        )
    }
}

@Composable
fun CleanDateCard(
    label: String,
    date: String,
    icon: ImageVector,
    bg: Color,
    textColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Text(
            text = label,
            fontFamily = BricolageGrotesque,
            style = MaterialTheme.typography.labelSmall,
            color = textColor.copy(alpha = 0.7f),
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(4.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = date,
                fontFamily = BricolageGrotesque,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = textColor
            )
            Icon(imageVector = icon, contentDescription = null, tint = textColor.copy(alpha = 0.6f), modifier = Modifier.size(16.dp))
        }
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
    isSwiping: Boolean = false,
    onEditClick: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val rotation by animateFloatAsState(if (expanded) 180f else 0f, label = "arrowRotation")

    val isDark = isSystemInDarkTheme()

    val cardBackground = if (isDark) Color(0xFF1B1B1B) else Color.White
    val pillBackground = if (isDark) Color(0xFFE8EBED).copy(alpha = 0.1f) else Color(0xFFE8EBED).copy(alpha = 0.4f)

    val progressBrush = remember(isDark) {
        Brush.linearGradient(
            colors = if (isDark) {
                listOf(Color(0xFFD89046), Color(0xFFD89046))
            } else {
                listOf(Color(0xFF2A3825), Color(0xFF2A3825))
            }
        )
    }

    val primaryTextColor = if (isDark) Color.White else Color(0xFF1B1B1B)
    val secondaryTextColor = if (isDark) Color.White.copy(alpha = 0.6f) else Color(0xFF64748B)
    val pillTextColor = if (isDark) Color.White else Color(0xFF1B1B1B)

    fun shortPretty(d: String): String = runCatching {
        if (d.isBlank()) return@runCatching "?"
        val date = LocalDate.parse(d)
        "${date.month.getDisplayName(java.time.format.TextStyle.SHORT, Locale.getDefault())} ${date.dayOfMonth}"
    }.getOrElse { d }

    fun painLabel(p: Int): String = when {
        p <= 0 -> "None"
        p in 1..3 -> "Mild ($p/10)"
        p in 4..6 -> "Moderate ($p/10)"
        p in 7..8 -> "Severe ($p/10)"
        else -> "Very severe ($p/10)"
    }

    val startDt = remember(startDate) { runCatching { LocalDate.parse(startDate) }.getOrNull() }
    val endDt = remember(endDate) { runCatching { if (endDate.isNotBlank()) LocalDate.parse(endDate) else null }.getOrNull() }
    val now = LocalDateTime.now()
    val today = now.toLocalDate()

    var progTarget = 0f
    var statusText = "Status unknown"
    var compliment = ""
    val isOngoing = endDt == null || !today.isAfter(endDt)

    if (startDt != null) {
        val startDateTime = startDt.atStartOfDay()
        val totalDays: Long

        if (endDt == null) {
            val elapsedDays = ChronoUnit.DAYS.between(startDt, today) + 1
            statusText = "Day $elapsedDays • Ongoing"
            totalDays = maxOf(6L, elapsedDays + 2L)
            val elapsedMinutes = ChronoUnit.MINUTES.between(startDateTime, now)
            progTarget = (elapsedMinutes.toFloat() / (totalDays * 1440f)).coerceIn(0f, 0.95f)
        } else {
            totalDays = ChronoUnit.DAYS.between(startDt, endDt) + 1
            if (today.isAfter(endDt)) {
                statusText = "$totalDays Days • Completed"
                progTarget = 1f
            } else {
                statusText = "${ChronoUnit.DAYS.between(today, endDt)} days left"
                val elapsedMinutes = ChronoUnit.MINUTES.between(startDateTime, now)
                progTarget = (elapsedMinutes.toFloat() / (totalDays * 1440f)).coerceIn(0f, 1f)
            }
        }

        compliment = when {
            progTarget >= 1f -> "Cycle completed. You did it! 🌟"
            progTarget > 0.8f -> "You're stronger than you know. 💪"
            progTarget > 0.5f -> "Halfway through. Keep going! 🌈"
            progTarget > 0.2f -> "Taking it day by day. 🗓️"
            else -> "Fresh start. Sending love! 💖"
        }
    }

    val animatedProgress by animateFloatAsState(targetValue = progTarget, animationSpec = tween(1200))

    Card(
        colors = CardDefaults.cardColors(containerColor = cardBackground),
        shape = RoundedCornerShape(22.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { expanded = !expanded }
            )
    ) {
        // Reduced vertical padding to 16.dp for a slimmer profile
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 20.dp)) {

            // Header Row: Dropdown icon aligned to title text
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = bleeding.replaceFirstChar { it.uppercase() },
                            fontFamily = BricolageGrotesque,
                            color = primaryTextColor,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            tint = primaryTextColor.copy(alpha = 0.6f),
                            modifier = Modifier
                                .size(24.dp)
                                .graphicsLayer { rotationZ = rotation }
                        )
                    }
                    Text(
                        text = "$monthLabel $dayNumber",
                        fontFamily = BricolageGrotesque,
                        color = secondaryTextColor,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Normal
                    )
                }
            }

            Spacer(Modifier.height(6.dp))

            // Status and Pills
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Text(
                    text = statusText,
                    fontFamily = BricolageGrotesque,
                    color = primaryTextColor,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Normal
                )
                MetaPill("Color", bloodColor, pillTextColor, pillBackground)
            }

            AnimatedVisibility(visible = isOngoing || expanded) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Spacer(Modifier.height(14.dp))
                    Canvas(Modifier.fillMaxWidth().height(6.dp).clip(CircleShape)) {
                        drawRoundRect(
                            color = pillBackground.copy(alpha = 0.4f),
                            size = size,
                            cornerRadius = CornerRadius(50f)
                        )
                        drawRoundRect(
                            brush = progressBrush,
                            size = Size(animatedProgress * size.width, size.height),
                            cornerRadius = CornerRadius(50f)
                        )
                    }

                    if (compliment.isNotEmpty()) {
                        Spacer(Modifier.height(10.dp))
                        // Highlighted Quotation: Centered and enlarged
                        Text(
                            text = compliment,
                            fontFamily = BricolageGrotesque,
                            textAlign = TextAlign.Start,
                            modifier = Modifier.fillMaxWidth(),
                            style = androidx.compose.ui.text.TextStyle(
                                brush = progressBrush,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                            )
                        )
                    }
                }
            }

            AnimatedVisibility(visible = expanded) {
                Column {
                    Spacer(Modifier.height(18.dp))
                    Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(10.dp)) {
                        InfoBox("Duration", "${shortPretty(startDate)} - ${if(endDate.isNotBlank()) shortPretty(endDate) else "Ongoing"}", pillBackground, pillTextColor)
                        InfoBox("Cramps / Pain", painLabel(crampsPain), pillBackground, pillTextColor)
                    }
                    Spacer(Modifier.height(16.dp))
                    OutlinedButton(
                        onClick = onEditClick,
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(50),
                        border = BorderStroke(1.dp, secondaryTextColor.copy(alpha = 0.3f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = primaryTextColor)
                    ) {
                        Icon(Icons.Default.Edit, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Edit Entry",
                            fontFamily = BricolageGrotesque,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RowScope.InfoBox(
    label: String,
    value: String,
    bg: Color,
    textColor: Color
) {
    Column(
        modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(20.dp))
            .background(bg)
            .padding(vertical = 14.dp, horizontal = 16.dp)
    ) {
        Text(
            text = label,
            fontFamily = BricolageGrotesque, // Applied custom font
            color = textColor.copy(alpha = 0.7f),
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = value,
            fontFamily = BricolageGrotesque, // Applied custom font
            color = textColor,
            fontSize = 13.sp,
            fontWeight = FontWeight.Normal
        )
    }
}

@Composable
fun MetaPill(label: String, value: String, textColor: Color, bg: Color) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(bg)
            .padding(horizontal = 14.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = value,
            fontFamily = BricolageGrotesque, // Applied custom font
            color = textColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.widthIn(max = 120.dp)
        )
    }
}

@Composable
fun PredictionBanner(
    prediction: Prediction?,
    cycles: List<PeriodViewModel.Cycle>,
    surface: Color,
    text: Color,
    subText: Color
) {
    if (prediction == null) return
    val today = LocalDate.now()
    val isBleeding = cycles.any { it.endDate == null || (today >= it.startDate && (it.endDate != null && today <= it.endDate)) }
    if (isBleeding) return

    val daysUntil = ChronoUnit.DAYS.between(today, prediction.mostLikelyPeriodStart)
    val isDark = isSystemInDarkTheme()

    val cardBackground = if (isDark) Color(0xFF1B1B1B) else Color.White
    val textPrimary = if (isDark) Color.White else Color(0xFF0F172A)
    val textSecondary = if (isDark) Color.White.copy(alpha = 0.6f) else Color(0xFF64748B)

    val pillBackground = if (isDark) Color(0xFFE8EBED).copy(alpha = 0.1f) else Color(0xFFE8EBED).copy(alpha = 0.4f)
    val pillTextColor = if (isDark) Color.White else Color(0xFF1B1B1B)

    // Using Extended Icons now
    val (icon, accentColor, statusTitle, personalMessage) = when {
        daysUntil < 0 -> Quadruple(Icons.Rounded.Warning, Color(0xFFEF5350), "Late by ${kotlin.math.abs(daysUntil)} days", "Don't panic! 🧘‍♀️")
        daysUntil == 0L -> Quadruple(Icons.Rounded.Opacity, if (isDark) Color(0xFFC8D4E5) else Color(0xFF8089D2), "Starts today", "Have your kit ready! 🍫")
        daysUntil <= 3 -> Quadruple(Icons.Rounded.Bolt, Color(0xFFFFB74D), "Almost time", "Hydrate and rest up. 💧")
        daysUntil <= 7 -> Quadruple(Icons.Rounded.Event, if (isDark) Color(0xFF8089D2) else Color(0xFF2C3F70), "Coming soon", "Stock up on snacks? 🍪")
        else -> Quadruple(Icons.Rounded.Spa, Color(0xFF66BB6A), "$daysUntil days until next cycle", "Relax and enjoy. ✨")
    }

    Card(
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = cardBackground),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 20.dp, vertical = 18.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon, // Using ImageVector from Library
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = statusTitle,
                    fontFamily = BricolageGrotesque,
                    fontWeight = FontWeight.SemiBold,
                    color = textPrimary,
                    fontSize = 16.sp
                )
                Text(
                    text = personalMessage,
                    fontFamily = BricolageGrotesque,
                    fontWeight = FontWeight.Normal,
                    style = MaterialTheme.typography.bodySmall,
                    color = textSecondary
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(pillBackground)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = prediction.mostLikelyPeriodStart.pretty(),
                    fontFamily = BricolageGrotesque,
                    fontWeight = FontWeight.Normal,
                    color = pillTextColor,
                    fontSize = 11.sp,
                )
            }
        }
    }
}

@Composable
fun WellnessCardsRow(
    cycles: List<PeriodViewModel.Cycle>,
    prediction: Prediction?
) {
    val today = LocalDate.now()
    val lastCycle = cycles.maxByOrNull { it.startDate }

    // Defaults
    var doText = "Take some time to unwind today."
    var doIcon = Icons.Rounded.SelfImprovement

    var moveText = "A gentle walk is perfect."
    var moveIcon = Icons.Rounded.DirectionsWalk

    var eatText = "Stay hydrated and drink water."
    var eatIcon = Icons.Rounded.LocalCafe

    if (lastCycle != null) {
        val daysSinceStart = ChronoUnit.DAYS.between(lastCycle.startDate, today).toInt()
        val isBleeding = lastCycle.endDate == null || today <= lastCycle.endDate

        if (isBleeding) {
            doText = "Take it slow and rest up."
            doIcon = Icons.Rounded.Bedtime
            moveText = "Try gentle stretching or yoga."
            moveIcon = Icons.Rounded.SelfImprovement
            eatText = "Comfort food rich in iron."
            eatIcon = Icons.Rounded.SoupKitchen
        } else if (daysSinceStart in 6..13) {
            doText = "Plan new goals or projects."
            doIcon = Icons.Rounded.Checklist
            moveText = "Go for a run or a hike."
            moveIcon = Icons.Rounded.DirectionsRun
            eatText = "Fresh salads and protein."
            eatIcon = Icons.Rounded.Restaurant
        } else if (daysSinceStart in 14..17) {
            doText = "Connect with your friends."
            doIcon = Icons.Rounded.Favorite
            moveText = "Push limits with a workout."
            moveIcon = Icons.Rounded.FitnessCenter
            eatText = "Light meals keep you going."
            eatIcon = Icons.Rounded.Tapas
        } else if (daysSinceStart in 18..28) {
            doText = "Tidy up your personal space."
            doIcon = Icons.Rounded.AutoAwesome
            moveText = "Pilates or strength training."
            moveIcon = Icons.Rounded.SelfImprovement
            eatText = "Complex carbs stabilize mood."
            eatIcon = Icons.Rounded.Grain
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        WellnessCardItem(
            title = "Do",
            content = doText,
            icon = doIcon,
            backgroundColor = Color(0xFF2A3825), // Applied Privacy Card Green
            modifier = Modifier.weight(1f),
        )
        WellnessCardItem(
            title = "Move",
            content = moveText,
            icon = moveIcon,
            backgroundColor = Color(0xFFD89046), // Applied Prediction Card Orange
            modifier = Modifier.weight(1f)
        )
        WellnessCardItem(
            title = "Eat",
            content = eatText,
            icon = eatIcon,
            backgroundColor = Color(0xFF4E1A1A), // Applied Tips Card Maroon
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun WellnessCardItem(
    title: String,
    content: String,
    icon: ImageVector,
    backgroundColor: Color, // New parameter for card identity
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()

    // Base card color matches the passed onboarding color
    val cardBg = backgroundColor
    // Title, content, and icon made white to ensure contrast on colored backgrounds
    val titleColor = Color.White.copy(alpha = 0.8f)
    val contentColor = Color.White
    val iconTint = Color.White

    Card(
        modifier = modifier.height(165.dp),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(cardBg)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(24.dp)
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    text = title.uppercase(),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.sp
                    ),
                    fontFamily = BricolageGrotesque,
                    color = titleColor,
                    fontSize = 11.sp
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    text = content,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Normal
                    ),
                    fontFamily = BricolageGrotesque,
                    color = contentColor,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                )
            }
        }
    }
}
// Simple helper class for the 'when' block return values
data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
