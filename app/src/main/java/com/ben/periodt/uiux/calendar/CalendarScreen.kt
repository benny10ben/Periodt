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
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.EventAvailable
import androidx.compose.material.icons.rounded.Healing
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material.icons.rounded.Update
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.font.FontStyle
import com.ben.periodt.uiux.shared.CycleRegularity
import com.ben.periodt.uiux.shared.Prediction
import com.ben.periodt.uiux.shared.UpcomingBannerEnhanced
import com.ben.periodt.uiux.shared.getDisplayName
import com.ben.periodt.uiux.shared.pretty
import java.time.LocalDateTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen() {
    val context = LocalContext.current.applicationContext as Application
    val viewModel: PeriodViewModel = viewModel(factory = PeriodViewModel.Factory(context))
    val cycles by viewModel.cycles.collectAsState()
    val prediction by viewModel.prediction.collectAsState()
    val isDark = isSystemInDarkTheme()

    // --- SORTING LOGIC: Latest at top, Oldest at bottom ---
    val sortedCycles = remember(cycles) {
        cycles.sortedByDescending { it.startDate }
    }

    // Gradient palette (Shared for Calendar and Banner)
    val gradTop = if (isDark) Color(0xFF7B8FA3) else Color(0xFF8FA0B1)
    val gradMid = if (isDark) Color(0xFF7288A0) else Color(0xFF8799B0)
    val gradBottom = if (isDark) Color(0xFF5A7396) else Color(0xFF6E87A7)

    val onGradient = Color.White
    val onGradientMuted = onGradient.copy(alpha = if (isDark) 0.70f else 0.55f)

    val currentMonth = remember { YearMonth.now() }
    val currentDate = remember { LocalDate.now() }

    // Calendar States
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

    // Scroll & Collapse Logic
    val listState = rememberLazyListState()
    var isCollapsed by remember { mutableStateOf(false) }

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

    // Sync effects
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

    // UI Palettes
    val entrySurface = if (isDark) Color(0xFF141820) else Color(0xFFF5F7F9)
    val entrySoft    = if (isDark) Color(0xFF1B2029) else Color(0xFFE6EAF0)
    val entryText    = if (isDark) Color(0xFFF5F7FA) else Color(0xFF0F172A)
    val entrySub     = if (isDark) Color(0xFFBFC6D1) else Color(0xFF64748B)
    val entryAccent  = if (isDark) Color(0xFFF5F7FA) else Color(0xFF0F1114)

    @Composable
    fun DayCell(date: LocalDate, isCurrentMonth: Boolean) {
        val isToday = date == LocalDate.now()
        val inCycle = cycles.any { c -> !date.isBefore(c.startDate) && (c.endDate?.let { !date.isAfter(it) } ?: true) }
        val isFertile = prediction?.fertileWindow?.let { date in it } == true
        val isOvulation = prediction?.ovulationDay == date
        val isPredictedPeriod = prediction?.let { pred ->
            val start = pred.mostLikelyPeriodStart
            val len = pred.periodLength ?: 5
            !date.isBefore(start) && date.isBefore(start.plusDays(len.toLong()))
        } == true

        val accentFill = if (isDark) Color.Black else Color.White
        val numberTextToday = if (isDark) Color.White else Color(0xFF000000)

        Column(
            modifier = Modifier.size(40.dp).padding(vertical = 6.dp, horizontal = 2.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            when {
                isToday -> {
                    Box(Modifier.size(32.dp).clip(RoundedCornerShape(6.dp)).background(accentFill), contentAlignment = Alignment.Center) {
                        Text(date.dayOfMonth.toString(), color = numberTextToday, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
                isOvulation -> {
                    Box(Modifier.clip(RoundedCornerShape(6.dp)).background(Color(0xFFFF4081)).size(32.dp), contentAlignment = Alignment.Center) {
                        Text(date.dayOfMonth.toString(), color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
                isFertile -> {
                    Box(Modifier.clip(RoundedCornerShape(6.dp)).background(Color(0xFFFF4081).copy(alpha = 0.25f)).size(32.dp), contentAlignment = Alignment.Center) {
                        Text(date.dayOfMonth.toString(), color = Color.White.copy(alpha = 0.9f), fontSize = 13.sp)
                    }
                }
                isPredictedPeriod && isCurrentMonth -> {
                    Box(Modifier.clip(RoundedCornerShape(6.dp)).background(Color.Red.copy(alpha = 0.2f)).size(32.dp), contentAlignment = Alignment.Center) {
                        Text(date.dayOfMonth.toString(), color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    }
                }
                inCycle && isCurrentMonth -> {
                    Box(Modifier.clip(RoundedCornerShape(6.dp)).background(accentFill.copy(alpha = 0.10f)).padding(horizontal = 6.dp, vertical = 2.dp), contentAlignment = Alignment.Center) {
                        Text(date.dayOfMonth.toString(), color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    }
                }
                else -> {
                    val alpha = if (isCurrentMonth) 1f else 0.35f
                    Text(date.dayOfMonth.toString(), color = Color.White.copy(alpha = alpha), fontSize = 13.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().background(Color.Transparent).padding(top = 4.dp).padding(horizontal = 16.dp).nestedScroll(nestedScrollConnection)
    ) {
        // --- 1. Calendar Section ---
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
        ) {
            Box(Modifier.clip(RoundedCornerShape(24.dp)).background(Brush.verticalGradient(listOf(gradTop, gradMid, gradBottom))).background(Color.White.copy(alpha = 0.06f))) {
                Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
                    val scope = rememberCoroutineScope()
                    val headerText = if (isCollapsed) {
                        val currentWeek = weekState.firstVisibleWeek
                        val dominantDate = currentWeek.days.getOrNull(3)?.date ?: currentWeek.days.first().date
                        dominantDate.format(DateTimeFormatter.ofPattern("MMM yyyy")).replaceFirstChar { it.titlecase(Locale.getDefault()) }
                    } else {
                        state.firstVisibleMonth.yearMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault()).replaceFirstChar { it.titlecase(Locale.getDefault()) } + " " + state.firstVisibleMonth.yearMonth.year
                    }

                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Text("‹", color = onGradientMuted, fontSize = 20.sp, modifier = Modifier.width(36.dp).clickable(null, null) {
                            scope.launch { if (isCollapsed) weekState.animateScrollToWeek(weekState.firstVisibleWeek.days.first().date.minusWeeks(1)) else state.animateScrollToMonth(state.firstVisibleMonth.yearMonth.minusMonths(1)) }
                        }, textAlign = TextAlign.Center)
                        Text(text = headerText, color = onGradient, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                        Text("›", color = onGradientMuted, fontSize = 20.sp, modifier = Modifier.width(36.dp).clickable(null, null) {
                            scope.launch { if (isCollapsed) weekState.animateScrollToWeek(weekState.firstVisibleWeek.days.first().date.plusWeeks(1)) else state.animateScrollToMonth(state.firstVisibleMonth.yearMonth.plusMonths(1)) }
                        }, textAlign = TextAlign.Center)
                    }

                    Spacer(Modifier.height(10.dp))
                    val weekdayLabels = listOf("SUN", "MON", "TUE", "WED", "THU", "FRI", "SAT")
                    Row(Modifier.fillMaxWidth().padding(horizontal = 2.dp)) {
                        weekdayLabels.forEach { Box(Modifier.weight(1f), contentAlignment = Alignment.Center) { Text(it, color = onGradientMuted, fontSize = 11.sp) } }
                    }
                    Spacer(Modifier.height(8.dp))
                    AnimatedContent(targetState = isCollapsed, label = "calendar_collapse") { collapsed ->
                        if (collapsed) WeekCalendar(state = weekState, dayContent = { DayCell(it.date, true) })
                        else HorizontalCalendar(state = state, dayContent = { DayCell(it.date, it.position == DayPosition.MonthDate) })
                    }
                    Spacer(Modifier.height(6.dp))
                }
            }
        }

        // --- 2. Personalized Banner Section ---
        PredictionBanner(
            prediction = prediction,
            cycles = cycles,
            surface = entrySurface,
            text = entryText,
            subText = entrySub
        )

        Spacer(Modifier.height(8.dp))

        // --- 3. Cycle History List ---
        var cycleToEdit by remember { mutableStateOf<PeriodViewModel.Cycle?>(null) }
        LaunchedEffect(sortedCycles.size) { if (sortedCycles.isNotEmpty()) listState.animateScrollToItem(0) }

        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(24.dp)),
            state = listState,
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 115.dp)
        ){
            items(sortedCycles, key = { it.id }) { cycle ->
                SwipeToDeleteCard(onDelete = { viewModel.deleteCycle(cycle.id) }) {
                    EntryRow(
                        monthLabel = cycle.startDate.month.getDisplayName(TextStyle.SHORT, Locale.getDefault()).uppercase(),
                        dayNumber = cycle.startDate.dayOfMonth.toString(),
                        startDate = cycle.startDate.toString(),
                        endDate = cycle.endDate?.toString() ?: "",
                        bleeding = cycle.bleeding,
                        bloodColor = cycle.bloodColor,
                        crampsPain = cycle.painLevel,
                        surface = entrySurface, soft = entrySoft, text = entryText, sub = entrySub, accent = entryAccent,
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EditCycleDialog(
    cycle: PeriodViewModel.Cycle,
    onDismiss: () -> Unit,
    onSave: (PeriodViewModel.Cycle) -> Unit
) {
    // --- State (Initialized with existing cycle data) ---
    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker by remember { mutableStateOf(false) }

    var startDate by remember { mutableStateOf(cycle.startDate) }
    var endDate by remember { mutableStateOf(cycle.endDate) }
    var bleeding by remember { mutableStateOf(cycle.bleeding) }
    var bloodColor by remember { mutableStateOf(cycle.bloodColor) }
    var painLevel by remember { mutableIntStateOf(cycle.painLevel) }

    // --- Resources ---
    val bleedingOptions = listOf("Heavy", "Medium", "Light", "Spotting")
    val colorOptions = listOf("Bright Red", "Dark Red", "Brown")

    // --- Aesthetic Palette (Matches AddCycleDialog) ---
    val isDark = isSystemInDarkTheme()
    val gradTop = if (isDark) Color(0xFF7B8FA3) else Color(0xFF8FA0B1)
    val gradMid = if (isDark) Color(0xFF7288A0) else Color(0xFF8799B0)
    val gradBottom = if (isDark) Color(0xFF5A7396) else Color(0xFF6E87A7)

    val onGradient = Color.White
    val contentSurface = if (isDark) Color.Black else Color.White
    val textPrimary = if (isDark) Color(0xFFF5F7FA) else Color(0xFF0F172A)
    val textSub = if (isDark) Color(0xFFBFC6D1) else Color(0xFF64748B)

    val formatter = remember { DateTimeFormatter.ofPattern("MMM dd") }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        // --- MAIN CARD ---
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .padding(vertical = 24.dp),
            shape = RoundedCornerShape(26.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(26.dp))
                    .background(Brush.verticalGradient(listOf(gradTop, gradMid, gradBottom)))
                    .background(Color.White.copy(alpha = 0.08f))
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {

                    // --- HEADER ---
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 24.dp, start = 24.dp, end = 24.dp, bottom = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Edit Cycle", // Updated Title
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                            color = onGradient
                        )
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.2f))
                                .clickable(onClick = onDismiss),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = onGradient,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // --- CONTENT AREA ---
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                            .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                            .background(contentSurface)
                            .padding(24.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {

                        // 1. Date Selectors
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            DateGlassCard(
                                label = "Started",
                                date = startDate.format(formatter),
                                icon = Icons.Rounded.CalendarToday,
                                textColor = textPrimary,
                                subColor = textSub,
                                onClick = { showStartPicker = true },
                                modifier = Modifier.weight(1f)
                            )
                            DateGlassCard(
                                label = "Ended",
                                date = endDate?.format(formatter) ?: "Ongoing",
                                icon = if (endDate == null) Icons.Rounded.Update else Icons.Rounded.EventAvailable,
                                textColor = if (endDate == null) gradBottom else textPrimary,
                                subColor = textSub,
                                onClick = { showEndPicker = true },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        HorizontalDivider(color = textSub.copy(alpha = 0.1f))

                        // 2. Bleeding
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            SectionLabel("Flow Intensity", Icons.Rounded.WaterDrop, gradBottom)
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                bleedingOptions.forEach { option ->
                                    ModernSelectionPill(
                                        text = option,
                                        isSelected = bleeding.equals(option, ignoreCase = true),
                                        activeColor = gradBottom,
                                        textColor = textPrimary,
                                        onClick = { bleeding = option }
                                    )
                                }
                            }
                        }

                        // 3. Color
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            SectionLabel("Color", Icons.Rounded.Palette, gradBottom)
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                colorOptions.forEach { option ->
                                    ModernSelectionPill(
                                        text = option,
                                        isSelected = bloodColor.equals(option, ignoreCase = true),
                                        activeColor = gradBottom,
                                        textColor = textPrimary,
                                        onClick = { bloodColor = option }
                                    )
                                }
                            }
                        }

                        HorizontalDivider(color = textSub.copy(alpha = 0.1f))

                        // 4. Pain Slider
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                SectionLabel("Pain Level", Icons.Rounded.Healing, gradBottom)
                                Text(
                                    text = "$painLevel / 10",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = gradBottom
                                )
                            }
                            Slider(
                                value = painLevel.toFloat(),
                                onValueChange = { painLevel = it.toInt() },
                                valueRange = 0f..10f,
                                steps = 9,
                                colors = SliderDefaults.colors(
                                    thumbColor = gradBottom,
                                    activeTrackColor = gradBottom,
                                    inactiveTrackColor = textSub.copy(alpha = 0.2f)
                                )
                            )
                        }

                        Spacer(Modifier.height(4.dp))

                        // 5. Update Button
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
                                .fillMaxWidth()
                                .height(54.dp)
                                .shadow(8.dp, RoundedCornerShape(34.dp), ambientColor = gradBottom.copy(alpha = 0.5f), spotColor = gradBottom.copy(alpha = 0.5f)),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                            contentPadding = PaddingValues(0.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Brush.horizontalGradient(listOf(gradTop, gradBottom))),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "Update Entry", // Updated Text
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // --- Reuse Date Pickers ---
    if (showStartPicker) {
        MinimalDatePickerDialog(
            title = "Start Date",
            brand = gradBottom, gradTop = gradTop, gradMid = gradMid, gradBottom = gradBottom,
            onGradient = Color.White,
            buttonContainer = contentSurface,
            buttonContent = textPrimary,
            onDismiss = { showStartPicker = false },
            onConfirm = { ms -> millisToLocalDate(ms)?.let { startDate = it }; showStartPicker = false }
        )
    }
    if (showEndPicker) {
        MinimalDatePickerDialog(
            title = "End Date",
            brand = gradBottom, gradTop = gradTop, gradMid = gradMid, gradBottom = gradBottom,
            onGradient = Color.White,
            buttonContainer = contentSurface,
            buttonContent = textPrimary,
            onDismiss = { showEndPicker = false },
            onConfirm = { ms -> millisToLocalDate(ms)?.let { endDate = it }; showEndPicker = false }
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

    val isDark = isSystemInDarkTheme()
    val gradTop = if (isDark) Color(0xFF7B8FA3) else Color(0xFF8FA0B1)
    val gradMid = if (isDark) Color(0xFF7288A0) else Color(0xFF8799B0)
    val gradBottom = if (isDark) Color(0xFF5A7396) else Color(0xFF6E87A7)
    val themeGradient = remember { Brush.horizontalGradient(listOf(gradTop, gradMid, gradBottom)) }

    // --- Helpers ---
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

    val startDt = remember(startDate) { runCatching { LocalDate.parse(startDate) }.getOrNull() }
    val endDt = remember(endDate) { runCatching { if (endDate.isNotBlank()) LocalDate.parse(endDate) else null }.getOrNull() }
    val now = LocalDateTime.now()
    val today = now.toLocalDate()

    var progTarget = 0f
    var statusText = "Status unknown"
    var totalDaysForBar = 0L
    var compliment = ""
    val isOngoing = endDt == null || !today.isAfter(endDt)

    if (startDt != null) {
        val startDateTime = startDt.atStartOfDay()
        if (endDt == null) {
            val elapsedDays = ChronoUnit.DAYS.between(startDt, today) + 1
            statusText = "Day $elapsedDays • Ongoing"
            totalDaysForBar = maxOf(6L, elapsedDays + 2L)
            val elapsedMinutes = ChronoUnit.MINUTES.between(startDateTime, now)
            progTarget = (elapsedMinutes.toFloat() / (totalDaysForBar * 1440f)).coerceIn(0f, 0.95f)
            compliment = "Listen to your body. You're doing great! 🙌"
        } else {
            totalDaysForBar = ChronoUnit.DAYS.between(startDt, endDt) + 1
            if (today.isAfter(endDt)) {
                statusText = "$totalDaysForBar Days • Completed"
                progTarget = 1f
                compliment = "Cycle completed. Awesome job! 🌟"
            } else {
                val elapsedDays = ChronoUnit.DAYS.between(startDt, today) + 1
                statusText = "${ChronoUnit.DAYS.between(today, endDt)} days left"
                val elapsedMinutes = ChronoUnit.MINUTES.between(startDateTime, now)
                progTarget = (elapsedMinutes.toFloat() / (totalDaysForBar * 1440f)).coerceIn(0f, 1f)
                compliment = "Day $elapsedDays: You're doing great! ✨"
            }
        }
    }

    val animatedProgress by animateFloatAsState(targetValue = progTarget, animationSpec = tween(1200))

    Card(
        colors = CardDefaults.cardColors(containerColor = surface),
        shape = RoundedCornerShape(18.dp),
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
            // Header Row: Minimalist Floating Icons
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                BleedingIcon(bleeding = bleeding)

                Spacer(Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = bleeding.replaceFirstChar { it.uppercase() },
                        color = text,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text("$monthLabel $dayNumber", color = sub, fontSize = 12.sp)
                }

                // Naked Chevron
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = text.copy(alpha = 0.7f),
                    modifier = Modifier
                        .size(24.dp)
                        .graphicsLayer { rotationZ = rotation }
                )
            }

            Spacer(Modifier.height(14.dp))

            // Meta Info (Always visible Status and Color)
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Text(statusText, color = text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                MetaPill("Color", bloodColor, text, sub, soft)
            }

            // Progress Bar (Visible if Ongoing or Expanded)
            AnimatedVisibility(visible = isOngoing || expanded) {
                Column {
                    Spacer(Modifier.height(12.dp))
                    Canvas(Modifier.fillMaxWidth().height(20.dp).clip(CircleShape)) {
                        drawRect(color = soft, size = size)
                        drawRect(brush = themeGradient, size = Size(animatedProgress * size.width, size.height))
                        if (totalDaysForBar > 1) {
                            val seg = size.width / totalDaysForBar
                            for (i in 1 until totalDaysForBar.toInt()) {
                                drawCircle(color = surface, radius = 2.dp.toPx(), center = Offset(i * seg, size.height / 2f))
                            }
                        }
                    }
                    if (compliment.isNotEmpty()) {
                        Spacer(Modifier.height(6.dp))
                        Text(text = compliment, color = gradTop, fontSize = 12.sp, fontStyle = FontStyle.Italic)
                    }
                }
            }

            // Expanded content
            AnimatedVisibility(visible = expanded) {
                Column {
                    Spacer(Modifier.height(14.dp))
                    Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(8.dp)) {
                        InfoBox("Duration", "${shortPretty(startDate)} - ${if(endDate.isNotBlank()) shortPretty(endDate) else "Ongoing"}", soft, sub, text)
                        InfoBox("Cramps / Pain", painLabel(crampsPain), soft, sub, text)
                    }
                    Spacer(Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = onEditClick,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, sub.copy(alpha = 0.3f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = text)
                    ) {
                        Icon(Icons.Default.Edit, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Edit Entry", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
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
    sub: Color,
    text: Color
) {
    Column(
        modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(16.dp))
            .background(bg)
            .padding(12.dp)
    ) {
        Text(label, color = sub, fontSize = 11.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(4.dp))
        Text(value, color = text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
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

    Image(
        painter = painterResource(id = resId),
        contentDescription = "Bleeding level",
        modifier = Modifier.size(28.dp),
        contentScale = ContentScale.Fit
    )
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
    val isBleeding = cycles.any { it.endDate == null || (today >= it.startDate && today <= it.endDate) }
    if (isBleeding) return

    val daysUntil = ChronoUnit.DAYS.between(today, prediction.mostLikelyPeriodStart)

    val (icon, accentColor, statusTitle, personalMessage) = when {
        daysUntil < 0 -> Quadruple(Icons.Rounded.Warning, Color(0xFFEF5350), "Late by ${kotlin.math.abs(daysUntil)} days", "Don't panic! 🧘‍♀️")
        daysUntil == 0L -> Quadruple(Icons.Rounded.WaterDrop, Color(0xFFFF4081), "Starts today", "Have your kit ready! 🍫")
        daysUntil <= 3 -> Quadruple(Icons.Rounded.Bolt, Color(0xFFFFB74D), "Almost time", "Hydrate and rest up. 💧")
        daysUntil <= 7 -> Quadruple(Icons.Rounded.CalendarToday, Color(0xFF42A5F5), "Coming soon", "Stock up on snacks? 🍪")
        else -> Quadruple(Icons.Rounded.Spa, Color(0xFF66BB6A), "$daysUntil days until next cycle", "Relax and enjoy. ✨")
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = surface),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Naked Icon
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(28.dp)
            )

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(statusTitle, fontWeight = FontWeight.Bold, color = text, fontSize = 15.sp)
                Text(personalMessage, style = MaterialTheme.typography.bodySmall, color = subText)
            }

            // Small Pill for the date (Kept this as it adds a nice functional touch)
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(subText.copy(alpha = 0.08f))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(prediction.mostLikelyPeriodStart.pretty(), color = subText, fontSize = 10.sp)
            }
        }
    }
}

// Simple helper class for the 'when' block return values
data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
