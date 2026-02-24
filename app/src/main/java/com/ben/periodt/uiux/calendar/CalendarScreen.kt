package com.ben.periodt.uiux.calendar

import android.app.Application
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
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

    // This connection listens to ANY scroll on the LazyColumn below
    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val delta = available.y
                if (delta < -15f) isCollapsed = true
                if (delta > 15f && listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0) {
                    isCollapsed = false
                }
                return Offset.Zero
            }
        }
    }

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

    // Aesthetic Palette
    val entrySurface = if (isDark) Color(0xFF1B1B1B) else Color.White
    val entryText = if (isDark) Color.White else Color(0xFF0F172A)
    val entrySub = if (isDark) Color.White.copy(alpha = 0.6f) else Color(0xFF64748B)

    var cycleToEdit by remember { mutableStateOf<PeriodViewModel.Cycle?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            // Attach the scroll listener to the parent column
            .nestedScroll(nestedScrollConnection)
    ) {
        // 1. Calendar (Stays pinned at top, collapses based on scroll below)
        CalendarCard(isCollapsed, state, weekState, cycles, prediction)

        // 2. SCROLLABLE CONTENT AREA
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)),
            state = listState,
            verticalArrangement = Arrangement.spacedBy(6.dp),
            contentPadding = PaddingValues(top = 0.dp, bottom = 115.dp)
        ) {

            // Item 1: Prediction Banner
            item {
                PredictionBanner(prediction, cycles, entrySurface, entryText, entrySub)
            }

            // Item 2: Wellness Cards
            item {
                WellnessCardsRow(cycles = cycles, prediction = prediction)
            }

            // Item 3: Spacer
            item {
                Spacer(Modifier.height(4.dp))
            }

            // Item 4+: Cycle History
            items(sortedCycles, key = { it.id }) { cycle ->
                // The SwipeToDeleteCard now provides 'isSwiping' to its content
                SwipeToDeleteCard(onDelete = { viewModel.deleteCycle(cycle.id) }) { isSwiping ->
                    EntryRow(
                        // FIX: Explicitly use java.time.format.TextStyle here
                        monthLabel = cycle.startDate.month.getDisplayName(java.time.format.TextStyle.SHORT, Locale.getDefault()).uppercase(),
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
                        isSwiping = isSwiping, // Pass the swipe state here
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

    // --- Dynamic Background Logic ---
    val backgroundBrush = if (isDark) {
        Brush.linearGradient(
            colors = listOf(Color(0xFF000000), Color(0xFF8089D2))
        )
    } else {
        Brush.linearGradient(
            colors = listOf(Color(0xFFa9b5c4), Color(0xFF8089D2))
        )
    }

    val onCardContent = Color.White
    val onCardContentMuted = onCardContent.copy(alpha = 0.70f)

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
    ) {
        Box(
            modifier = Modifier
                .background(backgroundBrush)
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            Column {
                val headerText = if (isCollapsed) {
                    val currentWeek = weekState.firstVisibleWeek
                    val dominantDate = currentWeek.days.getOrNull(3)?.date ?: currentWeek.days.first().date
                    dominantDate.format(DateTimeFormatter.ofPattern("MMM yyyy"))
                        .replaceFirstChar { it.titlecase(Locale.getDefault()) }
                } else {
                    state.firstVisibleMonth.yearMonth.month
                        .getDisplayName(TextStyle.FULL, Locale.getDefault())
                        .replaceFirstChar { it.titlecase(Locale.getDefault()) } + " " + state.firstVisibleMonth.yearMonth.year
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "‹",
                        color = onCardContentMuted,
                        fontSize = 24.sp,
                        fontFamily = BricolageGrotesque, // Applied font
                        modifier = Modifier
                            .width(40.dp)
                            .clickable {
                                scope.launch {
                                    if (isCollapsed) weekState.animateScrollToWeek(weekState.firstVisibleWeek.days.first().date.minusWeeks(1))
                                    else state.animateScrollToMonth(state.firstVisibleMonth.yearMonth.minusMonths(1))
                                }
                            },
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = headerText,
                        fontFamily = BricolageGrotesque, // Applied font
                        color = onCardContent,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = "›",
                        color = onCardContentMuted,
                        fontSize = 24.sp,
                        fontFamily = BricolageGrotesque, // Applied font
                        modifier = Modifier
                            .width(40.dp)
                            .clickable {
                                scope.launch {
                                    if (isCollapsed) weekState.animateScrollToWeek(weekState.firstVisibleWeek.days.first().date.plusWeeks(1))
                                    else state.animateScrollToMonth(state.firstVisibleMonth.yearMonth.plusMonths(1))
                                }
                            },
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(Modifier.height(10.dp))

                val weekdayLabels = listOf("SUN", "MON", "TUE", "WED", "THU", "FRI", "SAT")
                Row(Modifier.fillMaxWidth().padding(horizontal = 2.dp)) {
                    weekdayLabels.forEach { label ->
                        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                            Text(
                                text = label,
                                color = onCardContentMuted,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = BricolageGrotesque // Applied font
                            )
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                AnimatedContent(targetState = isCollapsed, label = "calendar_collapse") { collapsed ->
                    if (collapsed) {
                        WeekCalendar(
                            state = weekState,
                            dayContent = { DayCell(it.date, true, cycles, prediction) }
                        )
                    } else {
                        HorizontalCalendar(
                            state = state,
                            dayContent = { DayCell(it.date, it.position == DayPosition.MonthDate, cycles, prediction) }
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
            }
        }
    }
}

@Composable
fun DayCell(date: LocalDate, isCurrentMonth: Boolean, cycles: List<PeriodViewModel.Cycle>, prediction: Prediction?) {
    val isToday = date == LocalDate.now()
    val inCycle = cycles.any { c -> !date.isBefore(c.startDate) && (c.endDate?.let { !date.isAfter(it) } ?: true) }
    val isFertile = prediction?.fertileWindow?.let { date in it } == true
    val isOvulation = prediction?.ovulationDay == date
    val isPredictedPeriod = prediction?.let { pred ->
        val start = pred.mostLikelyPeriodStart
        val len = pred.periodLength ?: 5
        !date.isBefore(start) && date.isBefore(start.plusDays(len.toLong()))
    } == true

    val isDark = isSystemInDarkTheme()
    val accentFill = if (isDark) Color.Black else Color.White
    val textOnAccent = if (isDark) Color.White else Color.Black

    Column(
        modifier = Modifier.size(40.dp).padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        when {
            isToday -> {
                Box(Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)).background(accentFill), contentAlignment = Alignment.Center) {
                    Text(
                        text = date.dayOfMonth.toString(),
                        color = textOnAccent,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = BricolageGrotesque // Applied font
                    )
                }
            }
            isOvulation -> {
                Box(Modifier.size(32.dp).clip(CircleShape).background(Color(0xFFFF4081)), contentAlignment = Alignment.Center) {
                    Text(
                        text = date.dayOfMonth.toString(),
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = BricolageGrotesque // Applied font
                    )
                }
            }
            isFertile -> {
                Box(Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFFFF4081).copy(alpha = 0.25f)), contentAlignment = Alignment.Center) {
                    Text(
                        text = date.dayOfMonth.toString(),
                        color = Color.White,
                        fontSize = 13.sp,
                        fontFamily = BricolageGrotesque // Applied font
                    )
                }
            }
            isPredictedPeriod && isCurrentMonth -> {
                Box(Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)).background(Color.Red.copy(alpha = 0.25f)), contentAlignment = Alignment.Center) {
                    Text(
                        text = date.dayOfMonth.toString(),
                        color = Color.White,
                        fontSize = 13.sp,
                        fontFamily = BricolageGrotesque // Applied font
                    )
                }
            }
            inCycle && isCurrentMonth -> {
                Box(Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)).background(accentFill.copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
                    Text(
                        text = date.dayOfMonth.toString(),
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = BricolageGrotesque // Applied font
                    )
                }
            }
            else -> {
                val alpha = if (isCurrentMonth) 1f else 0.3f
                Text(
                    text = date.dayOfMonth.toString(),
                    color = Color.White.copy(alpha = alpha),
                    fontSize = 13.sp,
                    fontFamily = BricolageGrotesque // Applied font
                )
            }
        }
    }
}

// ---------- Swipe row, entry row ----------
@Composable
fun SwipeToDeleteCard(
    onDelete: () -> Unit,
    content: @Composable (Boolean) -> Unit // Now accepts a boolean
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

    // Determine if the card is currently being swiped/moved
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

                    // Click handler for the red background actions...
                    val sideWidth = 140.dp
                    val overlayModifier = if (offsetX.value >= 0f) {
                        Modifier.fillMaxHeight().width(sideWidth).align(Alignment.CenterStart)
                    } else {
                        Modifier.fillMaxHeight().width(sideWidth).align(Alignment.CenterEnd)
                    }

                    Box(
                        modifier = overlayModifier.clickable(
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
                                    // Drag end logic (snapping)
                                    val target = when {
                                        offsetX.value <= -deleteThreshold -> -widthPx
                                        offsetX.value >= deleteThreshold -> widthPx
                                        else -> 0f
                                    }
                                    if (kotlin.math.abs(target) == kotlin.math.abs(widthPx) && widthPx > 0f) {
                                        offsetX.animateTo(target, bounceSpring)
                                        onDelete()
                                    } else {
                                        offsetX.animateTo(0f, bounceSpring)
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
                // Pass the swiping state to the content
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

    val isDark = isSystemInDarkTheme()

    val contentSurface = if (isDark) Color(0xFF1B1B1B) else Color.White
    val pillBackground = if (isDark) Color(0xFFE8EBED).copy(alpha = 0.1f) else Color(0xFFE8EBED).copy(alpha = 0.4f)
    val pillTextColor = if (isDark) Color.White else Color(0xFF2C3F70)
    val accentColor = if (isDark) Color(0xFF8089D2) else Color(0xFF1B1B1B)

    val textPrimary = if (isDark) Color.White else Color(0xFF0F172A)
    val textSub = if (isDark) Color.White.copy(alpha = 0.6f) else Color(0xFF64748B)

    val dialogBrush = if (isDark) {
        Brush.linearGradient(listOf(Color(0xFFC8D4E5), Color(0xFF8089D2)))
    } else {
        Brush.linearGradient(listOf(Color(0xFF2C3F70), Color(0xFF2C3F70)))
    }

    val formatter = remember { DateTimeFormatter.ofPattern("MMM dd") }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .padding(vertical = 24.dp),
            shape = RoundedCornerShape(26.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(26.dp))
                    .background(dialogBrush)
                    .padding(0.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(contentSurface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 24.dp, start = 24.dp, end = 24.dp, bottom = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Edit Cycle",
                            fontFamily = BricolageGrotesque,
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
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
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            CleanDateCard(
                                label = "Started",
                                date = startDate.format(formatter),
                                icon = Icons.Rounded.CalendarToday,
                                bg = pillBackground,
                                textColor = pillTextColor,
                                onClick = { showStartPicker = true },
                                modifier = Modifier.weight(1f)
                            )
                            CleanDateCard(
                                label = "Ended",
                                date = endDate?.format(formatter) ?: "Ongoing",
                                icon = if (endDate == null) Icons.Rounded.Update else Icons.Rounded.EventAvailable,
                                bg = pillBackground,
                                textColor = pillTextColor,
                                onClick = { showEndPicker = true },
                                modifier = Modifier.weight(1f)
                            )
                        }

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
                                    EntryStylePill(
                                        text = option,
                                        isSelected = bleeding.equals(option, ignoreCase = true),
                                        activeBg = pillBackground,
                                        activeText = pillTextColor,
                                        inactiveText = textSub,
                                        surface = contentSurface,
                                        onClick = { bleeding = option }
                                    )
                                }
                            }
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(
                                text = "Color",
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
                                    EntryStylePill(
                                        text = option,
                                        isSelected = bloodColor.equals(option, ignoreCase = true),
                                        activeBg = pillBackground,
                                        activeText = pillTextColor,
                                        inactiveText = textSub,
                                        surface = contentSurface,
                                        onClick = { bloodColor = option }
                                    )
                                }
                            }
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Pain Level",
                                    fontFamily = BricolageGrotesque,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = textPrimary
                                )
                                Text(
                                    text = "${sliderPosition.toInt()} / 10",
                                    fontFamily = BricolageGrotesque,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold,
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
                                    inactiveTrackColor = pillBackground.copy(alpha = 0.3f)
                                )
                            )
                        }

                        Button(
                            onClick = {
                                onSave(cycle.copy(startDate = startDate, endDate = endDate, bleeding = bleeding, bloodColor = bloodColor, painLevel = painLevel))
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(50),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = accentColor,
                                contentColor = Color.White
                            ),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                        ) {
                            Text(
                                text = "Update Entry",
                                fontFamily = BricolageGrotesque,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }

    if (showStartPicker) {
        MinimalDatePickerDialog(
            title = "Start Date",
            brand = accentColor, gradTop = Color.Gray, gradMid = Color.Gray, gradBottom = Color.Gray,
            onGradient = Color.White, buttonContainer = contentSurface, buttonContent = textPrimary,
            onDismiss = { showStartPicker = false },
            onConfirm = { ms -> millisToLocalDate(ms)?.let { startDate = it }; showStartPicker = false }
        )
    }
    if (showEndPicker) {
        MinimalDatePickerDialog(
            title = "End Date",
            brand = accentColor, gradTop = Color.Gray, gradMid = Color.Gray, gradBottom = Color.Gray,
            onGradient = Color.White, buttonContainer = contentSurface, buttonContent = textPrimary,
            onDismiss = { showEndPicker = false },
            onConfirm = { ms -> millisToLocalDate(ms)?.let { endDate = it }; showEndPicker = false }
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

    // --- INSTANT VISIBILITY LOGIC ---
    // No animation. If swiping in dark mode, alpha is 1f immediately.
    // Otherwise, it is 0.6f (or white in light mode).
    val cardBackground = if (isDark) {
        if (isSwiping) Color(0xFF1B1B1B) else Color(0xFF1B1B1B).copy(alpha = 0.6f)
    } else {
        Color.White
    }
    // --------------------------------

    val pillBackground = if (isDark) Color(0xFFE8EBED).copy(alpha = 0.1f) else Color(0xFFE8EBED).copy(alpha = 0.4f)
    val progressBrush = remember {
        Brush.linearGradient(
            colors = listOf(Color(0xFFa9b5c4), Color(0xFF8089D2))
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
            progTarget > 0.9f -> "So close to the finish line! 🏁"
            progTarget > 0.8f -> "You're stronger than you know. 💪"
            progTarget > 0.7f -> "The end is in sight! ✨"
            progTarget > 0.6f -> "Be kind to yourself today. 🌷"
            progTarget > 0.5f -> "Halfway through. Keep going! 🌈"
            progTarget > 0.4f -> "Doing great, listen to your body. 🎧"
            progTarget > 0.3f -> "One breath at a time. 🍃"
            progTarget > 0.2f -> "Taking it day by day. 🗓️"
            progTarget > 0.1f -> "You've got this. 💫"
            else -> "Fresh start. Sending love! 💖"
        }
    }

    val animatedProgress by animateFloatAsState(targetValue = progTarget, animationSpec = tween(1200))

    Card(
        colors = CardDefaults.cardColors(containerColor = cardBackground),
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
        Column(Modifier.fillMaxWidth().padding(20.dp)) {
            // Header Row
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = bleeding.replaceFirstChar { it.uppercase() },
                        fontFamily = BricolageGrotesque,
                        color = primaryTextColor,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "$monthLabel $dayNumber",
                        fontFamily = BricolageGrotesque,
                        color = secondaryTextColor,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Normal
                    )
                }

                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = primaryTextColor.copy(alpha = 0.6f),
                    modifier = Modifier
                        .size(26.dp)
                        .graphicsLayer { rotationZ = rotation }
                )
            }

            Spacer(Modifier.height(18.dp))

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
                Column {
                    Spacer(Modifier.height(16.dp))
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
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = compliment,
                            fontFamily = BricolageGrotesque,
                            style = androidx.compose.ui.text.TextStyle(
                                brush = progressBrush,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Normal,
                                fontStyle = FontStyle.Italic
                            )
                        )
                    }
                }
            }

            AnimatedVisibility(visible = expanded) {
                Column {
                    Spacer(Modifier.height(20.dp))
                    Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(10.dp)) {
                        InfoBox("Duration", "${shortPretty(startDate)} - ${if(endDate.isNotBlank()) shortPretty(endDate) else "Ongoing"}", pillBackground, pillTextColor)
                        InfoBox("Cramps / Pain", painLabel(crampsPain), pillBackground, pillTextColor)
                    }
                    Spacer(Modifier.height(16.dp))
                    OutlinedButton(
                        onClick = onEditClick,
                        modifier = Modifier.fillMaxWidth().height(50.dp),
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

    val cardBackground = if (isDark) Color(0xFF1B1B1B).copy(alpha = 0.6f) else Color.White
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
    var doIcon = Icons.Rounded.SelfImprovement // From Extended Library

    var moveText = "A gentle walk is perfect."
    var moveIcon = Icons.Rounded.DirectionsWalk

    var eatText = "Stay hydrated and drink water."
    var eatIcon = Icons.Rounded.LocalCafe

    if (lastCycle != null) {
        val daysSinceStart = ChronoUnit.DAYS.between(lastCycle.startDate, today).toInt()
        val isBleeding = lastCycle.endDate == null || today <= lastCycle.endDate

        if (isBleeding) {
            doText = "Take it slow and rest up."
            doIcon = Icons.Rounded.Bedtime // Moon/Sleep
            moveText = "Try gentle stretching or yoga."
            moveIcon = Icons.Rounded.SelfImprovement
            eatText = "Comfort food rich in iron."
            eatIcon = Icons.Rounded.SoupKitchen // Warm food
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
            modifier = Modifier.weight(1f),
        )
        WellnessCardItem(
            title = "Move",
            content = moveText,
            icon = moveIcon,
            modifier = Modifier.weight(1f)
        )
        WellnessCardItem(
            title = "Eat",
            content = eatText,
            icon = eatIcon,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun WellnessCardItem(
    title: String,
    content: String,
    icon: ImageVector, // Back to ImageVector for the library
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()

    val cardBg = if (isDark) Color(0xFF1B1B1B).copy(alpha = 0.6f) else Color.White
    val titleColor = Color(0xFF8089D2)
    val contentColor = if (isDark) Color.White.copy(alpha = 0.9f) else Color(0xFF1B1B1B).copy(alpha = 0.8f)
    val iconTint = if (isDark) Color.White else Color.Black

    Card(
        modifier = modifier.height(160.dp),
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
