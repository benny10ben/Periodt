// uiux/shared/PredictionUtils.kt
package com.ben.periodt.uiux.shared

import com.ben.periodt.viewmodel.PeriodViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sqrt

data class Prediction(
    val minPeriodStart: LocalDate,
    val maxPeriodStart: LocalDate,
    val mostLikelyPeriodStart: LocalDate,
    val periodLength: Int,
    val ovulationDay: LocalDate,
    val ovulationConfidence: Float,
    val fertileWindow: ClosedRange<LocalDate>,
    val cycleLength: Int,
    val cycleRegularity: CycleRegularity
)

enum class CycleRegularity {
    VERY_REGULAR, REGULAR, SOMEWHAT_IRREGULAR, IRREGULAR
}

internal fun predictCycle(cycles: List<PeriodViewModel.Cycle>): Prediction? {
    if (cycles.isEmpty()) return null
    val sorted = cycles.sortedBy { it.startDate }
    val last = sorted.last()
    val lastStart = last.startDate
    val periodLen = calculatePeriodLength(sorted)
    return if (sorted.size == 1) {
        createFirstCyclePrediction(lastStart, periodLen)
    } else {
        createAdvancedPrediction(sorted, lastStart, periodLen)
    }
}

private fun calculatePeriodLength(cycles: List<PeriodViewModel.Cycle>): Int {
    val periodLengths = cycles.mapNotNull { cycle ->
        cycle.endDate?.let { endDate ->
            ChronoUnit.DAYS.between(cycle.startDate, endDate).toInt().coerceIn(1, 10)
        }
    }
    return if (periodLengths.isNotEmpty()) {
        val sorted = periodLengths.sorted()
        val median = if (sorted.size % 2 == 0) {
            (sorted[sorted.size / 2 - 1] + sorted[sorted.size / 2]) / 2
        } else sorted[sorted.size / 2]
        median.coerceIn(3, 8)
    } else 5
}

private fun createFirstCyclePrediction(lastStart: LocalDate, periodLen: Int): Prediction {
    val avgCycleLength = 28
    val stdDeviation = 4
    val minStart = lastStart.plusDays((avgCycleLength - stdDeviation * 2).toLong())
    val maxStart = lastStart.plusDays((avgCycleLength + stdDeviation * 2).toLong())
    val mostLikelyStart = lastStart.plusDays(avgCycleLength.toLong())
    val lutealPhaseLength = 14
    val ovulationDay = mostLikelyStart.minusDays(lutealPhaseLength.toLong())
    val fertileStart = ovulationDay.minusDays(6)
    val fertileEnd = ovulationDay.plusDays(1)
    return Prediction(
        minPeriodStart = minStart,
        maxPeriodStart = maxStart,
        mostLikelyPeriodStart = mostLikelyStart,
        periodLength = periodLen,
        ovulationDay = ovulationDay,
        ovulationConfidence = 0.3f,
        fertileWindow = fertileStart..fertileEnd,
        cycleLength = avgCycleLength,
        cycleRegularity = CycleRegularity.IRREGULAR
    )
}

private data class CycleStatistics(
    val mean: Double,
    val median: Int,
    val standardDeviation: Double,
    val weightedAverage: Double
)

private fun calculateCycleStatistics(cycleLengths: List<Int>): CycleStatistics {
    val sorted = cycleLengths.sorted()
    val mean = cycleLengths.average()
    val median = if (sorted.size % 2 == 0) {
        (sorted[sorted.size / 2 - 1] + sorted[sorted.size / 2]) / 2
    } else sorted[sorted.size / 2]
    val variance = cycleLengths.map { (it - mean).pow(2) }.average()
    val stdDev = sqrt(variance)
    val weights = (1..cycleLengths.size).map { it.toDouble() }
    val weightedSum = cycleLengths.zip(weights).sumOf { (len, w) -> len * w }
    val weightedAvg = weightedSum / weights.sum()
    return CycleStatistics(mean, median, stdDev, weightedAvg)
}

private fun determineCycleRegularity(stdDev: Double): CycleRegularity = when {
    stdDev <= 2.0 -> CycleRegularity.VERY_REGULAR
    stdDev <= 4.0 -> CycleRegularity.REGULAR
    stdDev <= 6.0 -> CycleRegularity.SOMEWHAT_IRREGULAR
    else -> CycleRegularity.IRREGULAR
}

// Simple linear regression implementation for trend prediction
private data class LinearRegressionResult(val slope: Double, val intercept: Double)

private fun simpleLinearRegression(x: List<Double>, y: List<Double>): LinearRegressionResult {
    val n = x.size
    val sumX = x.sum()
    val sumY = y.sum()
    val sumXY = x.zip(y).sumOf { (xi, yi) -> xi * yi }
    val sumX2 = x.sumOf { it * it }
    val denominator = n * sumX2 - sumX * sumX
    val slope = if (denominator != 0.0) (n * sumXY - sumX * sumY) / denominator else 0.0
    val intercept = (sumY - slope * sumX) / n
    return LinearRegressionResult(slope, intercept)
}

private fun predictWithTrend(cycleLengths: List<Int>): Pair<Double, Double> {  // Returns (blendedLength, slope)
    if (cycleLengths.size < 3) {
        val stats = calculateCycleStatistics(cycleLengths)
        return Pair(stats.weightedAverage, 0.0)
    }
    val x = (1..cycleLengths.size).map { it.toDouble() }
    val y = cycleLengths.map { it.toDouble() }
    val reg = simpleLinearRegression(x, y)
    val nextX = (cycleLengths.size + 1).toDouble()
    val predicted = reg.intercept + reg.slope * nextX
    val stats = calculateCycleStatistics(cycleLengths)
    val blended = 0.7 * predicted + 0.3 * stats.weightedAverage
    return Pair(blended, reg.slope)
}

private data class OvulationPrediction(val day: LocalDate, val confidence: Float)

private fun predictOvulation(
    cycles: List<PeriodViewModel.Cycle>,
    nextPeriodStart: LocalDate,
    regularity: CycleRegularity,
    trendSlope: Double = 0.0
): OvulationPrediction {
    val lutealPhases = mutableListOf<Int>()
    cycles.windowed(2) { (current, next) ->
        current.endDate?.let { endDate ->
            val cycleLength = ChronoUnit.DAYS.between(current.startDate, next.startDate).toInt()
            val follicular = ChronoUnit.DAYS.between(current.startDate, endDate).toInt()
            val estimated = cycleLength - follicular - 14
            if (estimated in 10..18) lutealPhases.add(estimated)
        }
    }
    val lutealPhase = if (lutealPhases.isNotEmpty()) {
        lutealPhases.average().toInt().coerceIn(10, 16)
    } else when (regularity) {
        CycleRegularity.VERY_REGULAR, CycleRegularity.REGULAR -> 14
        CycleRegularity.SOMEWHAT_IRREGULAR -> 13
        CycleRegularity.IRREGULAR -> 12
    }
    var confidence = when {
        lutealPhases.size >= 3 && regularity == CycleRegularity.VERY_REGULAR -> 0.85f
        lutealPhases.size >= 2 && regularity == CycleRegularity.REGULAR -> 0.75f
        cycles.size >= 4 && regularity != CycleRegularity.IRREGULAR -> 0.65f
        cycles.size >= 3 -> 0.55f
        else -> 0.40f
    }
    // Boost confidence for stable trend
    val trendBoost = if (kotlin.math.abs(trendSlope) < 1.0) 0.05f else 0f
    confidence = kotlin.math.min(0.95f, confidence + trendBoost)
    return OvulationPrediction(nextPeriodStart.minusDays(lutealPhase.toLong()), confidence)
}

private fun calculateFertileWindow(
    ovulationDay: LocalDate,
    confidence: Float,
    regularity: CycleRegularity
): ClosedRange<LocalDate> {
    val pre = when {
        confidence > 0.8f && regularity == CycleRegularity.VERY_REGULAR -> 5
        confidence > 0.6f && regularity != CycleRegularity.IRREGULAR -> 5
        else -> 6
    }
    val post = if (confidence > 0.7f) 1 else 2
    return ovulationDay.minusDays(pre.toLong())..ovulationDay.plusDays(post.toLong())
}

private fun createAdvancedPrediction(
    cycles: List<PeriodViewModel.Cycle>,
    lastStart: LocalDate,
    periodLen: Int
): Prediction {
    val recent = if (cycles.size > 6) cycles.takeLast(6) else cycles
    val starts = recent.map { it.startDate }
    val cycleLengths = starts.zipWithNext { a, b -> ChronoUnit.DAYS.between(a, b).toInt() }
    if (cycleLengths.isEmpty()) return createFirstCyclePrediction(lastStart, periodLen)
    val (blendedLength, slope) = predictWithTrend(cycleLengths)
    val stats = calculateCycleStatistics(cycleLengths)
    val regularity = determineCycleRegularity(stats.standardDeviation)
    val mostLikelyStart = lastStart.plusDays(blendedLength.toLong())
    val (minStart, maxStart) = when (regularity) {
        CycleRegularity.VERY_REGULAR -> {
            val next = mostLikelyStart
            Pair(next.minusDays(1), next.plusDays(1))
        }
        CycleRegularity.REGULAR -> {
            val next = mostLikelyStart
            val v = maxOf(2, stats.standardDeviation.toInt())
            Pair(next.minusDays(v.toLong()), next.plusDays(v.toLong()))
        }
        else -> {
            val next = mostLikelyStart
            var v = maxOf(3, stats.standardDeviation.toInt())
            v = (v * 1.5).toInt()
            Pair(next.minusDays(v.toLong()), next.plusDays(v.toLong()))
        }
    }
    val ov = predictOvulation(recent, mostLikelyStart, regularity, slope)
    val fertile = calculateFertileWindow(ov.day, ov.confidence, regularity)
    return Prediction(
        minPeriodStart = minStart,
        maxPeriodStart = maxStart,
        mostLikelyPeriodStart = mostLikelyStart,
        periodLength = periodLen,
        ovulationDay = ov.day,
        ovulationConfidence = ov.confidence,
        fertileWindow = fertile,
        cycleLength = blendedLength.toInt(),
        cycleRegularity = regularity
    )
}

internal fun LocalDate.pretty(): String {
    val fmt = DateTimeFormatter.ofPattern("MMM d")
    return this.format(fmt)
}

internal fun CycleRegularity.getDisplayName(): String = when (this) {
    CycleRegularity.VERY_REGULAR -> "Very regular"
    CycleRegularity.REGULAR -> "Regular"
    CycleRegularity.SOMEWHAT_IRREGULAR -> "Somewhat irregular"
    CycleRegularity.IRREGULAR -> "Irregular"
}

internal fun getCycleConfidence(regularity: CycleRegularity): Float = when (regularity) {
    CycleRegularity.VERY_REGULAR -> 0.9f
    CycleRegularity.REGULAR -> 0.75f
    CycleRegularity.SOMEWHAT_IRREGULAR -> 0.6f
    CycleRegularity.IRREGULAR -> 0.4f
}

internal fun getConfidenceLabel(confidence: Float): String = when {
    confidence >= 0.8f -> "High"
    confidence >= 0.6f -> "Good"
    confidence >= 0.4f -> "Fair"
    else -> "Low"
}