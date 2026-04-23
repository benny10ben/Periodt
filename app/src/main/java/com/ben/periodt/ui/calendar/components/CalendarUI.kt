package com.ben.periodt.ui.calendar.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.EventRepeat
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ben.periodt.prediction.Prediction
import com.ben.periodt.ui.theme.BricolageGrotesque
import com.ben.periodt.ui.theme.LocalAppIsDark
import com.ben.periodt.viewmodel.PeriodViewModel
import com.kizitonwose.calendar.compose.CalendarState
import com.kizitonwose.calendar.compose.HorizontalCalendar
import com.kizitonwose.calendar.compose.WeekCalendar
import com.kizitonwose.calendar.compose.weekcalendar.WeekCalendarState
import com.kizitonwose.calendar.core.DayPosition
import com.kizitonwose.calendar.core.daysOfWeek
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

private val SIZE_XXS = 11.sp
private val SIZE_MD  = 14.sp

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

    val themeAccent      = if (isDark) Color(0xFFD89046) else Color(0xFFa5bda3)
    val starAccent       = if (isDark) Color(0xFF8089D2) else Color(0xFF2C3F70)
    val colorPeriodSolid = Color(0xFFA5231C)
    val packColor        = Color(0xFFa68e74)
    val colorOvulationBg = if (isDark) Color.White.copy(alpha = 0.2f) else Color.Black.copy(alpha = 0.2f)
    val ColorFertileSolid = if (isDark) Color(0xFF42553f) else Color(0xFFa5bda3)

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
                        currentPhase != 0                 -> Color.White
                        isTransitioning || isLearningMode -> starAccent
                        pillPacks.isNotEmpty()            -> themeAccent
                        isDark                            -> Color.White
                        else                              -> Color.Black
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

@Composable
fun CalendarLegend(
    isOnPill: Boolean = false,
    pillPacks: List<PeriodViewModel.PillPack> = emptyList()
) {
    val isDark           = LocalAppIsDark.current
    val textSub          = if (isDark) Color.White.copy(alpha = 0.6f) else Color(0xFF1b1b1b).copy(alpha = 0.6f)
    val colorPeriodSolid = Color(0xFFA5231C)
    val packColor        = Color(0xFFa68e74)
    val ColorFertileSolid = if (isDark) Color(0xFF42553f) else Color(0xFFa5bda3)

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
            fontSize   = 11.sp,
            color      = textColor,
            fontWeight = FontWeight.Medium
        )
    }
}