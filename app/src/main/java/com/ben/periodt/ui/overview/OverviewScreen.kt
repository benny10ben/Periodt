package com.ben.periodt.ui.overview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.ben.periodt.prediction.PostPillState
import com.ben.periodt.prediction.calculatePeriodLength
import com.ben.periodt.prediction.getConfidenceLabel
import com.ben.periodt.prediction.getCycleConfidence
import com.ben.periodt.prediction.getDisplayName
import com.ben.periodt.prediction.pretty
import com.ben.periodt.ui.overview.components.BloodColorPieChart
import com.ben.periodt.ui.overview.components.CombinedStatsCard
import com.ben.periodt.ui.overview.components.MinimalChartCard
import com.ben.periodt.ui.overview.components.ScrollableLineChart
import com.ben.periodt.ui.overview.components.UpcomingBannerEnhanced
import com.ben.periodt.ui.overview.components.bleedingSeriesDailyVM
import com.ben.periodt.ui.overview.components.bloodColorDistributionVM
import com.ben.periodt.ui.overview.components.calculateAvgCycleLength
import com.ben.periodt.ui.overview.components.painSeriesDailyVM
import com.ben.periodt.ui.theme.LocalAppIsDark
import com.ben.periodt.viewmodel.PeriodViewModel
import kotlin.math.round

@Composable
fun OverviewScreen(viewModel: PeriodViewModel) {
    val cycles        by viewModel.cycles.collectAsState()
    val rawPrediction by viewModel.prediction.collectAsState()
    val prediction    = rawPrediction

    val isOnPill      by viewModel.isOnPill.collectAsState()
    val postPillState by viewModel.postPillState.collectAsState()
    val pillStopDate  by viewModel.pillStopDate.collectAsState()

    val postPillCycles = remember(cycles, pillStopDate) {
        if (pillStopDate != null) cycles.filter { !it.startDate.isBefore(pillStopDate) } else emptyList()
    }
    val discoveryCycle  = (postPillCycles.size + 1).coerceIn(1, 4)
    val isDiscoveryMode = postPillState == PostPillState.DISCOVERY
    val isLearningMode  = postPillState == PostPillState.LEARNING

    val screenScroll = rememberScrollState()
    val isDark       = LocalAppIsDark.current

    val gradTop         = if (isDark) Color(0xFF7B8FA3) else Color(0xFF8FA0B1)
    val gradMid         = if (isDark) Color(0xFF7288A0) else Color(0xFF8799B0)
    val gradBottom      = if (isDark) Color(0xFF5A7396) else Color(0xFF6E87A7)
    val onGradient      = Color.White
    val onGradientMuted = onGradient.copy(alpha = if (isDark) 0.70f else 0.55f)

    val surface = if (isDark) Color(0xFF141820) else Color(0xFFF5F7F9)
    val textCol = if (isDark) Color(0xFFF5F7FA) else Color(0xFF0F172A)
    val subCol  = if (isDark) Color(0xFFBFC6D1) else Color(0xFF1b1b1b)

    val bleedingChartColor = Color(0xFFD89046)
    val painChartColor     = if (isDark) Color(0xFF42553f) else Color(0xFFa5bda3)

    val avgPeriodLength = calculatePeriodLength(cycles).takeIf { cycles.any { c -> c.endDate != null } }
    val avgCycleLength  = calculateAvgCycleLength(cycles)

    val dailyLogs by viewModel.dailyLogs.collectAsState()

    val recentTrends = remember(cycles, dailyLogs) {
        val recentCycles = cycles.sortedBy { it.startDate }.takeLast(6)
        if (recentCycles.isEmpty()) return@remember null

        var totalPain = 0; var totalDays = 0
        val bleedingCounts = mutableMapOf<String, Int>()
        val colorCounts    = mutableMapOf<String, Int>()

        recentCycles.forEach { cycle ->
            val end = cycle.endDate ?: cycle.startDate.plusDays(4)
            var day = cycle.startDate
            while (!day.isAfter(end)) {
                val key = "${cycle.id}|$day"
                val b   = dailyLogs[key]?.bleeding   ?: cycle.bleeding
                val c   = dailyLogs[key]?.bloodColor ?: cycle.bloodColor
                val p   = dailyLogs[key]?.painLevel  ?: cycle.painLevel
                bleedingCounts[b] = (bleedingCounts[b] ?: 0) + 1
                colorCounts[c]    = (colorCounts[c]    ?: 0) + 1
                totalPain += p; totalDays++
                day = day.plusDays(1)
            }
        }
        if (totalDays == 0) return@remember null
        Triple(
            bleedingCounts.maxByOrNull { it.value }?.key ?: "Medium",
            colorCounts.maxByOrNull    { it.value }?.key ?: "Bright Red",
            round(totalPain.toFloat() / totalDays).toInt()
        )
    }
    val recentCyclesCount = minOf(cycles.size, 6)

    Box(modifier = Modifier.fillMaxSize().background(Color.Transparent)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(screenScroll)
                .padding(horizontal = 16.dp)
                .padding(bottom = 90.dp)
                .padding(top = 16.dp)
        ) {
//            if (!isOnPill && !isDiscoveryMode && !isLearningMode) {
//                RecentTrendsBanner(trends = recentTrends, cycleCount = recentCyclesCount)
//            }

            CombinedStatsCard(
                totalCycles = "${cycles.size}",
                avgPeriod   = avgPeriodLength?.let { "$it d" } ?: "0",
                avgCycle    = avgCycleLength?.let  { "$it d" } ?: "0"
            )

            Spacer(Modifier.height(14.dp))

            if (prediction != null || isDiscoveryMode) {
                UpcomingBannerEnhanced(
                    title           = if (isOnPill) "Withdrawal bleed" else "Upcoming period",
                    windowText      = prediction?.let { "${it.minPeriodStart.pretty()} – ${it.maxPeriodStart.pretty()}" } ?: "",
                    mostLikely      = prediction?.let { "Most likely: ${it.mostLikelyPeriodStart.pretty()}" } ?: "",
                    badge           = if (isOnPill) "Pill Pack" else "",
                    confidence      = prediction?.let { getCycleConfidence(it.cycleRegularity) } ?: 0f,
                    confidenceLabel = prediction?.cycleRegularity?.getDisplayName() ?: "",
                    gradTop = gradTop, gradMid = gradMid, gradBottom = gradBottom,
                    onGradient = onGradient, onGradientMuted = onGradientMuted,
                    mostLikelyDate  = prediction?.mostLikelyPeriodStart,
                    isDiscoveryMode = isDiscoveryMode,
                    isLearningMode  = isLearningMode,
                    isOnPill        = isOnPill,
                    discoveryCycle  = discoveryCycle
                )
            } else {
                UpcomingBannerEnhanced(
                    title = "Upcoming period", windowText = "Not enough data",
                    mostLikely = "Track more cycles for predictions", badge = "",
                    confidence = 0f, confidenceLabel = "No data",
                    gradTop = gradTop, gradMid = gradMid, gradBottom = gradBottom,
                    onGradient = onGradient, onGradientMuted = onGradientMuted
                )
            }

            if (prediction != null && !isOnPill && !isDiscoveryMode && !isLearningMode) {
                Spacer(Modifier.height(14.dp))
                UpcomingBannerEnhanced(
                    title           = "Fertile window",
                    windowText      = "${prediction.fertileWindow.start.pretty()} – ${prediction.fertileWindow.endInclusive.pretty()}",
                    mostLikely      = "Ovulation: ${prediction.ovulationDay.pretty()}",
                    badge           = "Confidence ${(prediction.ovulationConfidence * 100).toInt()}%",
                    confidence      = prediction.ovulationConfidence,
                    confidenceLabel = getConfidenceLabel(prediction.ovulationConfidence),
                    gradTop = gradTop, gradMid = gradMid, gradBottom = gradBottom,
                    onGradient = onGradient, onGradientMuted = onGradientMuted
                )
            }

            Spacer(Modifier.height(14.dp))

            MinimalChartCard("Bleeding intensity", surface, textCol) {
                val (bleedingPoints, bleedingDates) = remember(cycles, dailyLogs) { bleedingSeriesDailyVM(cycles, dailyLogs) }
                ScrollableLineChart(
                    points = bleedingPoints, dates = bleedingDates, lineColor = bleedingChartColor,
                    yLabels = listOf("S", "L", "M", "H"), yMax = 3f, showArea = true,
                    gridColor  = if (isDark) Color(0xFF2A2F36) else Color(0xFFEAEAEA),
                    axisColor  = if (isDark) Color(0xFF343A43) else Color(0xFFE0E0E0),
                    labelColor = subCol, surface = surface,
                    modifier = Modifier.fillMaxWidth().height(220.dp)
                )
            }

            Spacer(Modifier.height(14.dp))

            MinimalChartCard("Pain level", surface, textCol) {
                val (painPoints, painDates) = remember(cycles, dailyLogs) { painSeriesDailyVM(cycles, dailyLogs) }
                ScrollableLineChart(
                    points = painPoints, dates = painDates, lineColor = painChartColor,
                    yLabels = (0..10 step 2).map { "$it" }, yMax = 10f, showArea = true,
                    gridColor  = if (isDark) Color(0xFF2A2F36) else Color(0xFFEAEAEA),
                    axisColor  = if (isDark) Color(0xFF343A43) else Color(0xFFE0E0E0),
                    labelColor = subCol, surface = surface,
                    modifier = Modifier.fillMaxWidth().height(220.dp)
                )
            }

            Spacer(Modifier.height(14.dp))

            MinimalChartCard("Blood color", surface, textCol) {
                BloodColorPieChart(
                    data       = remember(cycles, dailyLogs) { bloodColorDistributionVM(cycles, dailyLogs) },
                    surface    = surface,
                    labelColor = subCol,
                    modifier   = Modifier.fillMaxWidth().wrapContentHeight().padding(horizontal = 8.dp)
                )
            }

            Spacer(Modifier.height(12.dp))
            Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.systemBars))
        }
    }
}