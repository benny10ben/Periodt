package com.ben.periodt.widget

import android.content.Context
import com.ben.periodt.data.AppDatabase
import com.ben.periodt.data.PeriodCycleEntity
import com.ben.periodt.uiux.shared.predictCycle
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class DayFlag(
    val inCycle: Boolean,
    val fertile: Boolean,
    val ovulation: Boolean,
    val isToday: Boolean,
    val predictedPeriod: Boolean
)

object MonthlyWidgetRenderer {

    /**
     * Builds a snapshot of day flags for the given month, fetching all cycles from Room on IO dispatcher.
     * Clamps fertile/predicted ranges to the 6-week widget grid.
     */
    suspend fun buildFlagsSnapshot(context: Context, ym: YearMonth): Map<LocalDate, DayFlag> = withContext(Dispatchers.IO) {
        val dao = AppDatabase.getDatabase(context).periodCycleDao()
        val entities: List<PeriodCycleEntity> = dao.getAllCyclesNow()  // Assumes suspend; change to getAllCycles() if this limits results

        val cycles = entities.map { e ->
            val start = LocalDate.parse(e.startDate)
            val end = e.endDate.takeIf { it.isNotBlank() }?.let(LocalDate::parse)
            com.ben.periodt.viewmodel.PeriodViewModel.Cycle(
                id = e.id,
                startDate = start,
                endDate = end,
                bleeding = e.bleeding,
                bloodColor = e.bloodColor,
                painLevel = e.painLevel
            )
        }

        val prediction = predictCycle(cycles) // single source of truth for widget snapshot

        // Date range shown in the widget (6 full weeks = 42 days)
        val first = ym.atDay(1)
        val firstDow = first.dayOfWeek.value % 7
        val widgetStartDate = first.minusDays(firstDow.toLong())
        val widgetEndDate = widgetStartDate.plusDays(41)

        // 1) Ovulation day: from prediction if present; otherwise derive from mostLikelyPeriodStart - luteal phase.
        val ovulationDay: LocalDate? = prediction?.ovulationDay ?: prediction?.mostLikelyPeriodStart?.let { mensesStart ->
            val assumedLutealDays = 14L
            mensesStart.minusDays(assumedLutealDays)
        }

        // 2) Fertile window = ovulation day and the five days before, clamped to widget range.
        val fertileSet: Set<LocalDate> = ovulationDay?.let { ovu ->
            val start = ovu.minusDays(5)
            val clampedStart = maxOf(start, widgetStartDate)
            val clampedEnd = minOf(ovu, widgetEndDate)
            if (clampedStart <= clampedEnd) datesBetweenInclusive(clampedStart, clampedEnd).toSet() else emptySet()
        } ?: emptySet()

        // 3) Predicted period range, clamped to widget range.
        val predictedPeriodSet: Set<LocalDate> = prediction?.let { pred ->
            val start = pred.mostLikelyPeriodStart
            val len = pred.periodLength ?: 5 // fallback if not provided
            val end = start.plusDays(len.toLong() - 1)
            val clampedStart = maxOf(start, widgetStartDate)
            val clampedEnd = minOf(end, widgetEndDate)
            if (clampedStart <= clampedEnd) datesBetweenInclusive(clampedStart, clampedEnd).toSet() else emptySet()
        } ?: emptySet()

        val today = LocalDate.now()
        val periodRanges = cycles.map { it.startDate to (it.endDate ?: LocalDate.MAX) }

        val out = linkedMapOf<LocalDate, DayFlag>()

        // Build flags across the 6-week grid
        var d = widgetStartDate
        while (!d.isAfter(widgetEndDate)) {
            val inCycle = periodRanges.any { (s, e) -> !d.isBefore(s) && !d.isAfter(e) }
            val fertile = d in fertileSet
            val ovulation = ovulationDay != null && d == ovulationDay
            val predictedPeriod = d in predictedPeriodSet
            out[d] = DayFlag(inCycle, fertile, ovulation, d == today, predictedPeriod)
            d = d.plusDays(1)
        }
        out
    }

    private fun datesBetweenInclusive(start: LocalDate, end: LocalDate): List<LocalDate> {
        val list = mutableListOf<LocalDate>()
        var d = start
        while (!d.isAfter(end)) {
            list.add(d)
            d = d.plusDays(1)
        }
        return list
    }
}