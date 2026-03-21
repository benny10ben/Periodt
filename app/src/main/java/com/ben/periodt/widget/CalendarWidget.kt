package com.ben.periodt.widget

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.glance.*
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.*
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.color.ColorProvider
import androidx.glance.layout.*
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.ben.periodt.R
import com.ben.periodt.data.AppDatabase
import com.ben.periodt.data.PeriodCycleEntity
import com.ben.periodt.data.PillPackEntity
import com.ben.periodt.viewmodel.PeriodViewModel
import com.ben.periodt.uiux.shared.CycleRegularity
import com.ben.periodt.uiux.shared.PostPillState
import com.ben.periodt.uiux.shared.Prediction
import com.ben.periodt.uiux.shared.getPostPillState
import com.ben.periodt.uiux.shared.predictCycle
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

val MonthOffsetKey = intPreferencesKey("calendar_month_offset")

@SuppressLint("RestrictedApi")
private fun colorProvider(color: Color): ColorProvider = ColorProvider(color)

private fun PeriodCycleEntity.toDomain(): PeriodViewModel.Cycle {
    return PeriodViewModel.Cycle(
        id = id,
        startDate = LocalDate.parse(startDate),
        endDate = endDate?.takeIf { it.isNotBlank() }?.let { LocalDate.parse(it) },
        bleeding = bleeding,
        bloodColor = bloodColor,
        painLevel = painLevel
    )
}

class CalendarWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = CalendarWidget()
}

class CalendarWidget : GlanceAppWidget() {
    override val stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition

    // --- ADDED REFRESH LOGIC HERE ---
    companion object {
        suspend fun refreshAll(context: Context) {
            val manager = GlanceAppWidgetManager(context)
            val ids = manager.getGlanceIds(CalendarWidget::class.java)
            val widget = CalendarWidget()
            ids.forEach { id ->
                widget.update(context, id)
            }
        }
    }

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val dao = AppDatabase.getDatabase(context).periodCycleDao()
        val cyclesEntities = dao.getAllCyclesOnce()
        val pills = dao.getAllPillPacksOnce()
        val domainCycles = cyclesEntities.map { it.toDomain() }

        val activePack = pills.firstOrNull { it.endDate == null }
        val stopDateStr = pills.filter { it.endDate != null }.maxByOrNull { it.endDate!! }?.endDate
        val stopDate = stopDateStr?.let { LocalDate.parse(it) }

        val isTransitioning = if (activePack != null || stopDate == null) false else {
            val postPillCycles = domainCycles.filter { !it.startDate.isBefore(stopDate) }
            getPostPillState(postPillCycles) == PostPillState.DISCOVERY
        }

        val prediction: Prediction? = when {
            activePack != null -> {
                val activeStart = LocalDate.parse(activePack.startDate)
                val withdrawalStart = activeStart.plusDays(activePack.pillCount.toLong() + 2)
                Prediction(
                    minPeriodStart = withdrawalStart.minusDays(1),
                    maxPeriodStart = withdrawalStart.plusDays(1),
                    mostLikelyPeriodStart = withdrawalStart,
                    periodLength = 4,
                    ovulationDay = withdrawalStart,
                    ovulationConfidence = 1.0f,
                    fertileWindow = LocalDate.MIN..LocalDate.MIN,
                    cycleLength = activePack.pillCount + 7,
                    cycleRegularity = CycleRegularity.VERY_REGULAR
                )
            }
            isTransitioning -> null
            else -> {
                val validCycles = if (stopDate != null) domainCycles.filter { !it.startDate.isBefore(stopDate) } else domainCycles
                predictCycle(validCycles)
            }
        }

        provideContent {
            val prefs = currentState<Preferences>()
            val monthOffset = prefs[MonthOffsetKey] ?: 0
            val displayMonth = YearMonth.now().plusMonths(monthOffset.toLong())

            CalendarWidgetContent(context, displayMonth, cyclesEntities, pills, prediction)
        }
    }

    @Composable
    private fun CalendarWidgetContent(
        context: Context,
        displayMonth: YearMonth,
        cycles: List<PeriodCycleEntity>,
        pills: List<PillPackEntity>,
        prediction: Prediction?
    ) {
        val textPrimaryProvider = ColorProvider(day = Color(0xFF0F172A), night = Color(0xFFFFFFFF))
        val textMutedProvider = ColorProvider(day = Color(0xFF64748B), night = Color(0xFFFFFFFF).copy(alpha = 0.6f))

        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .appWidgetBackground()
                .background(ImageProvider(R.drawable.bg_widget_card))
                .cornerRadius(24.dp)
                .padding(horizontal = 16.dp, vertical = 16.dp)
                .clickable(actionStartActivity(ComponentName(context, "com.ben.periodt.MainActivity")))
        ) {
            Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = displayMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
                    modifier = GlanceModifier.defaultWeight().padding(start = 10.dp),
                    style = TextStyle(color = textPrimaryProvider, fontSize = 18.sp, fontWeight = FontWeight.Medium)
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "‹",
                        modifier = GlanceModifier.padding(horizontal = 16.dp, vertical = 4.dp)
                            .clickable(actionRunCallback<UpdateMonthAction>(actionParametersOf(UpdateMonthAction.IncrementKey to -1))),
                        style = TextStyle(fontSize = 24.sp, color = textMutedProvider)
                    )
                    Text(
                        text = "›",
                        modifier = GlanceModifier.padding(horizontal = 16.dp, vertical = 4.dp)
                            .clickable(actionRunCallback<UpdateMonthAction>(actionParametersOf(UpdateMonthAction.IncrementKey to 1))),
                        style = TextStyle(fontSize = 24.sp, color = textMutedProvider)
                    )
                }
            }

            Spacer(GlanceModifier.height(16.dp))

            Row(modifier = GlanceModifier.fillMaxWidth()) {
                listOf("S", "M", "T", "W", "T", "F", "S").forEach { day ->
                    Text(
                        text = day,
                        modifier = GlanceModifier.defaultWeight(),
                        style = TextStyle(color = textMutedProvider, fontSize = 12.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center)
                    )
                }
            }

            Spacer(GlanceModifier.height(8.dp))

            val firstDay = displayMonth.atDay(1)
            val startOffset = firstDay.dayOfWeek.value % 7
            var cursor = firstDay.minusDays(startOffset.toLong())

            Column(modifier = GlanceModifier.fillMaxWidth().defaultWeight()) {
                repeat(6) {
                    Row(modifier = GlanceModifier.fillMaxWidth().defaultWeight()) {
                        repeat(7) {
                            DayCellGlance(
                                modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
                                date = cursor,
                                displayMonth = displayMonth,
                                cycles = cycles,
                                pills = pills,
                                prediction = prediction,
                                textPrimaryProvider = textPrimaryProvider
                            )
                            cursor = cursor.plusDays(1)
                        }
                    }
                }
            }
        }
    }
}

@SuppressLint("RestrictedApi")
@Composable
private fun DayCellGlance(
    modifier: GlanceModifier,
    date: LocalDate,
    displayMonth: YearMonth,
    cycles: List<PeriodCycleEntity>,
    pills: List<PillPackEntity>,
    prediction: Prediction?,
    textPrimaryProvider: ColorProvider
) {
    val phase = getPhase(date, cycles, pills, prediction)
    val prevPhase = getPhase(date.minusDays(1), cycles, pills, prediction)
    val nextPhase = getPhase(date.plusDays(1), cycles, pills, prediction)
    val isStart = phase != prevPhase
    val isEnd = phase != nextPhase
    val isCurrentMonth = date.month == displayMonth.month
    val isToday = date == LocalDate.now()

    val loggedColor = ColorProvider(day = Color(0xFFCA7B77), night = Color(0xFF6E1F1C))
    val upcomingColor = ColorProvider(day = Color(0xFFA5231C), night = Color(0xFFA5231C))
    val fertileColor = ColorProvider(day = Color(0xFFA6BFA2), night = Color(0xFF4C6549))
    val pillColor = ColorProvider(day = Color(0xFFa68e74), night = Color(0xFFa68e74))

    val bgColorProvider = when (phase) {
        1 -> loggedColor
        2 -> upcomingColor
        3 -> fertileColor
        5 -> pillColor
        else -> ColorProvider(Color.Transparent)
    }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        if (phase != 0) {
            Box(
                modifier = GlanceModifier.fillMaxSize()
                    .padding(start = 0.dp, end = 0.dp, top = 4.dp, bottom = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(modifier = GlanceModifier.fillMaxSize().background(bgColorProvider).cornerRadius(100.dp)) {}
                Row(modifier = GlanceModifier.fillMaxSize()) {
                    Box(modifier = GlanceModifier.fillMaxHeight().defaultWeight().background(if (!isStart) bgColorProvider else colorProvider(Color.Transparent))) {}
                    Box(modifier = GlanceModifier.fillMaxHeight().defaultWeight().background(if (!isEnd) bgColorProvider else colorProvider(Color.Transparent))) {}
                }
            }
        }

        val finalTextColorProvider = when {
            phase != 0 -> colorProvider(Color.White)
            isCurrentMonth -> if (isToday) colorProvider(Color(0xFFD89046)) else textPrimaryProvider
            else -> colorProvider(Color.Gray.copy(alpha = 0.5f))
        }

        Text(
            text = date.dayOfMonth.toString(),
            style = TextStyle(color = finalTextColorProvider, fontSize = 14.sp, fontWeight = if (isToday || phase != 0) FontWeight.Bold else FontWeight.Normal, textAlign = TextAlign.Center)
        )
    }
}

private fun getPhase(d: LocalDate, cycles: List<PeriodCycleEntity>, pills: List<PillPackEntity>, prediction: Prediction?): Int {
    val isLogged = cycles.any { c ->
        val start = LocalDate.parse(c.startDate)
        val end = c.endDate?.takeIf { it.isNotBlank() }?.let { LocalDate.parse(it) } ?: start.plusDays(6)
        !d.isBefore(start) && !d.isAfter(end)
    }
    if (isLogged) return 1
    val inPill = pills.any { p ->
        val start = LocalDate.parse(p.startDate)
        val end = p.endDate?.takeIf { it.isNotBlank() }?.let { LocalDate.parse(it) } ?: start.plusDays((p.pillCount - 1).toLong())
        !d.isBefore(start) && !d.isAfter(end)
    }
    if (inPill) return 5
    if (prediction != null) {
        if (!d.isBefore(prediction.mostLikelyPeriodStart) && d.isBefore(prediction.mostLikelyPeriodStart.plusDays(prediction.periodLength.toLong()))) return 2
        if (prediction.fertileWindow.start != LocalDate.MIN && !d.isBefore(prediction.fertileWindow.start) && !d.isAfter(prediction.fertileWindow.endInclusive)) return 3
    }
    return 0
}

class UpdateMonthAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val increment = parameters[IncrementKey] ?: 0
        updateAppWidgetState(context, glanceId) { prefs ->
            prefs[MonthOffsetKey] = (prefs[MonthOffsetKey] ?: 0) + increment
        }
        CalendarWidget().update(context, glanceId)
    }
    companion object { val IncrementKey = ActionParameters.Key<Int>("increment") }
}