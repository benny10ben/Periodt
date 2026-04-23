package com.ben.periodt.ui.overview.components

import com.ben.periodt.viewmodel.PeriodViewModel
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

fun bleedingSeriesDailyVM(
    cycles: List<PeriodViewModel.Cycle>,
    dailyLogs: Map<String, PeriodViewModel.DailyLog>
): Pair<List<Pair<Float, Float>>, List<String>> {
    val map       = mapOf("spotting" to 0.5f, "light" to 1f, "medium" to 2f, "heavy" to 3f)
    val formatter = DateTimeFormatter.ofPattern("MMM d")
    val points    = mutableListOf<Pair<Float, Float>>()
    val dates     = mutableListOf<String>()
    var index     = 0
    cycles.sortedBy { it.startDate }.forEach { cycle ->
        val end = cycle.endDate ?: cycle.startDate.plusDays(4); var day = cycle.startDate
        while (!day.isAfter(end)) {
            val key = "${cycle.id}|$day"
            points.add(index.toFloat() to (map[(dailyLogs[key]?.bleeding ?: cycle.bleeding).lowercase()] ?: 0f))
            dates.add(day.format(formatter)); day = day.plusDays(1); index++
        }
    }
    return points to dates
}

fun painSeriesDailyVM(
    cycles: List<PeriodViewModel.Cycle>,
    dailyLogs: Map<String, PeriodViewModel.DailyLog>
): Pair<List<Pair<Float, Float>>, List<String>> {
    val formatter = DateTimeFormatter.ofPattern("MMM d")
    val points    = mutableListOf<Pair<Float, Float>>()
    val dates     = mutableListOf<String>()
    var index     = 0
    cycles.sortedBy { it.startDate }.forEach { cycle ->
        val end = cycle.endDate ?: cycle.startDate.plusDays(4); var day = cycle.startDate
        while (!day.isAfter(end)) {
            val key = "${cycle.id}|$day"
            points.add(index.toFloat() to (dailyLogs[key]?.painLevel ?: cycle.painLevel).toFloat())
            dates.add(day.format(formatter)); day = day.plusDays(1); index++
        }
    }
    return points to dates
}

fun bloodColorDistributionVM(
    cycles: List<PeriodViewModel.Cycle>,
    dailyLogs: Map<String, PeriodViewModel.DailyLog>
): List<Pair<String, Float>> {
    val counts = mutableMapOf<String, Int>()
    cycles.forEach { cycle ->
        val end = cycle.endDate ?: cycle.startDate.plusDays(4); var day = cycle.startDate
        while (!day.isAfter(end)) {
            val key = "${cycle.id}|$day"
            val color = (dailyLogs[key]?.bloodColor ?: cycle.bloodColor).lowercase()
            counts[color] = (counts[color] ?: 0) + 1; day = day.plusDays(1)
        }
    }
    val total = counts.values.sum()
    if (total == 0) return emptyList()
    return counts.entries.sortedByDescending { it.value }.map { it.key to (it.value.toFloat() / total) }
}

fun calculateAvgCycleLength(cycles: List<PeriodViewModel.Cycle>): Int? {
    val lengths = cycles.sortedBy { it.startDate }.zipWithNext { a, b ->
        ChronoUnit.DAYS.between(a.startDate, b.startDate).toInt()
    }
    return if (lengths.isNotEmpty()) lengths.average().toInt() else null
}