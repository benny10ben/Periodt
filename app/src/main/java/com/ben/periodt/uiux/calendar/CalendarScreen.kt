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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ben.periodt.R
import com.ben.periodt.viewmodel.PeriodViewModel
import com.kizitonwose.calendar.compose.HorizontalCalendar
import com.kizitonwose.calendar.compose.rememberCalendarState
import com.kizitonwose.calendar.core.DayPosition
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.absoluteValue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen() {
    val context = LocalContext.current.applicationContext as Application
    val viewModel: PeriodViewModel = viewModel(factory = PeriodViewModel.Factory(context))
    val cycles by viewModel.cycles.collectAsState()
    val prediction by viewModel.prediction.collectAsState() // single source of truth [web:11]
    val isDark = isSystemInDarkTheme()

    // Gradient palette
    val gradTop = if (isDark) Color(0xFF7B8FA3) else Color(0xFF8FA0B1)
    val gradMid = if (isDark) Color(0xFF7288A0) else Color(0xFF8799B0)
    val gradBottom = if (isDark) Color(0xFF5A7396) else Color(0xFF6E87A7)

    val onGradient = Color.White
    val onGradientMuted = onGradient.copy(alpha = if (isDark) 0.70f else 0.55f)

    val currentMonth = remember { YearMonth.now() }
    val state = rememberCalendarState(
        startMonth = currentMonth.minusMonths(12),
        endMonth = currentMonth.plusMonths(12),
        firstVisibleMonth = currentMonth
    )

    // Entry palette
    val entrySurface = if (isDark) Color(0xFF141820) else Color(0xFFF5F7F9)
    val entrySoft    = if (isDark) Color(0xFF1B2029) else Color(0xFFE6EAF0)
    val entryText    = if (isDark) Color(0xFFF5F7FA) else Color(0xFF0F172A)
    val entrySub     = if (isDark) Color(0xFFBFC6D1) else Color(0xFF64748B)
    val entryAccent  = if (isDark) Color(0xFFF5F7FA) else Color(0xFF0F1114)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .padding(top = 4.dp)
            .padding(horizontal = 16.dp)
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
                                    val prev = state.firstVisibleMonth.yearMonth.minusMonths(1)
                                    scope.launch { state.animateScrollToMonth(prev) }
                                },
                            textAlign = TextAlign.Center
                        )

                        Text(
                            text = state.firstVisibleMonth.yearMonth.month.getDisplayName(
                                TextStyle.FULL, Locale.getDefault()
                            ).replaceFirstChar {
                                if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
                            } + " " + state.firstVisibleMonth.yearMonth.year,
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
                                    val next = state.firstVisibleMonth.yearMonth.plusMonths(1)
                                    scope.launch { state.animateScrollToMonth(next) }
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

                    // Calendar
                    HorizontalCalendar(
                        state = state,
                        dayContent = { day ->
                            val isCurrentMonth = day.position == DayPosition.MonthDate
                            val isToday = day.date == LocalDate.now()

                            val inCycle = cycles.any { c ->
                                !day.date.isBefore(c.startDate) &&
                                        (c.endDate?.let { !day.date.isAfter(it) } ?: true)
                            }

                            val isFertile = prediction?.fertileWindow?.let { day.date in it } == true
                            val isOvulation = prediction?.ovulationDay == day.date

                            // Derive predicted period range if not explicitly available in Prediction:
                            val isPredictedPeriod = prediction?.let { pred ->
                                val start = pred.mostLikelyPeriodStart
                                val len = pred.periodLength ?: 5 // fallback if not provided
                                !day.date.isBefore(start) &&
                                        day.date.isBefore(start.plusDays(len.toLong()))
                            } == true

                            val accentFill = if (isDark) Color.Black else Color.White
                            val numberText = Color.White
                            val numberTextToday =
                                if (isDark) Color.White else Color(0xFF000000)

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
                                                day.date.dayOfMonth.toString(),
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
                                                .background(Color(0xFFFF4081)) // strong pink
                                                .size(32.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                day.date.dayOfMonth.toString(),
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
                                                .background(Color(0x33FF4081)) // soft pink
                                                .size(32.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                day.date.dayOfMonth.toString(),
                                                color = numberText.copy(alpha = 0.9f),
                                                fontSize = 13.sp
                                            )
                                        }
                                    }
                                    isPredictedPeriod && isCurrentMonth -> {
                                        // Soft red highlight for predicted period window
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(Color.Red.copy(alpha = 0.2f))
                                                .size(32.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                day.date.dayOfMonth.toString(),
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
                                                day.date.dayOfMonth.toString(),
                                                color = numberText,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }
                                    else -> {
                                        val alpha = if (isCurrentMonth) 1f else 0.35f
                                        Text(
                                            day.date.dayOfMonth.toString(),
                                            color = numberText.copy(alpha = alpha),
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }
                    )

                    Spacer(Modifier.height(6.dp))
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        val listState = rememberLazyListState()

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
                bottom = 105.dp
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
                        accent = entryAccent
                    )
                }
            }
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

@Composable
private fun EntryRow(
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
    accent: Color
) {
    var expanded by remember { mutableStateOf(false) }
    val rotation by animateFloatAsState(if (expanded) 180f else 0f, label = "arrowRotation")

    fun pretty(d: String): String = runCatching {
        if (d.isBlank()) return@runCatching "Not set"
        val date = java.time.LocalDate.parse(d)
        val m = date.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())
        "$m ${date.dayOfMonth}, ${date.year}"
    }.getOrElse { d }

    fun painLabel(p: Int): String = when {
        p <= 0 -> "None"
        p in 1..3 -> "Mild ($p/10)"
        p in 4..6 -> "Moderate ($p/10)"
        p in 7..8 -> "Severe ($p/10)"
        else -> "Very severe ($p/10)"
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = surface),
        shape = RoundedCornerShape(22.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                BleedingIcon(bleeding = bleeding)
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = when (bleeding.lowercase(Locale.getDefault())) {
                            "heavy" -> "Heavy"
                            "medium" -> "Moderate"
                            "light" -> "Light"
                            else -> "Spotting"
                        },
                        color = text,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text("$monthLabel $dayNumber", color = sub, fontSize = 12.sp)
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

            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MetaPill("Bleeding", bleeding, text, sub, soft)
                MetaPill("Blood Color", bloodColor, text, sub, soft)
            }

            AnimatedVisibility(visible = expanded) {
                Column {
                    Spacer(Modifier.height(12.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Start: ${pretty(startDate)}", color = sub, fontSize = 14.sp)
                        Text("End: ${pretty(endDate)}", color = sub, fontSize = 14.sp)
                        Text("Cramps: ${painLabel(crampsPain)}", color = sub, fontSize = 14.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun MetaPill(label: String, value: String, text: Color, sub: Color, bg: Color) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(bg)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = sub, fontSize = 11.sp)
        Spacer(Modifier.width(6.dp))
        Box(
            modifier = Modifier.size(4.dp).clip(CircleShape).background(sub.copy(alpha = 0.6f))
        )
        Spacer(Modifier.width(6.dp))
        Text(
            value,
            color = text,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.widthIn(max = 120.dp)
        )
    }
}

@Composable
private fun BleedingIcon(bleeding: String) {
    val resId = when (bleeding.lowercase(Locale.getDefault())) {
        "heavy" -> R.drawable.heavy_bleeding
        "medium" -> R.drawable.medium_bleeding
        "light" -> R.drawable.light_bleeding
        else -> R.drawable.spotting
    }
    Image(
        painter = painterResource(id = resId),
        contentDescription = "Bleeding level",
        modifier = Modifier.size(20.dp),
        contentScale = ContentScale.Fit
    )
}
