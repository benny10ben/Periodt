package com.ben.periodt.uiux.shared

import com.ben.periodt.prediction.CycleRegularity
import com.ben.periodt.prediction.predictCycle
import com.ben.periodt.viewmodel.PeriodViewModel
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class PredictionTests {

    // Helper to create cycles that match your PeriodViewModel.Cycle exactly
    private fun createTestCycle(start: LocalDate, end: LocalDate?): PeriodViewModel.Cycle {
        return PeriodViewModel.Cycle(
            id = 0,
            startDate = start,
            endDate = end,
            bleeding = "Medium",
            bloodColor = "Red",
            painLevel = 1
        )
    }

    @Test
    fun `test predictCycle with perfectly regular data`() {
        val baseDate = LocalDate.of(2026, 1, 1)
        val cycles = listOf(
            createTestCycle(baseDate, baseDate.plusDays(5)),
            createTestCycle(baseDate.plusDays(28), baseDate.plusDays(33)),
            createTestCycle(baseDate.plusDays(56), baseDate.plusDays(61)),
            createTestCycle(baseDate.plusDays(84), baseDate.plusDays(89))
        )

        val prediction = predictCycle(cycles)

        assertNotNull("Prediction should not be null", prediction)
        // Checks your determineCycleRegularity logic (stdDev will be 0.0)
        assertEquals(CycleRegularity.VERY_REGULAR, prediction?.cycleRegularity)
        assertEquals(28, prediction?.cycleLength)

        val expectedDate = baseDate.plusDays(84).plusDays(28)
        assertEquals(expectedDate, prediction?.mostLikelyPeriodStart)
    }

    @Test
    fun `test linear regression trend prediction`() {
        val baseDate = LocalDate.of(2026, 1, 1)
        val cycles = listOf(
            createTestCycle(baseDate, baseDate.plusDays(5)),
            createTestCycle(baseDate.plusDays(28), baseDate.plusDays(33)),
            createTestCycle(baseDate.plusDays(57), baseDate.plusDays(62)),
            createTestCycle(baseDate.plusDays(87), baseDate.plusDays(92))
        )

        val prediction = predictCycle(cycles)

        // With an increasing trend (28, 29, 30), your blended prediction
        // (0.7 * predicted + 0.3 * weightedAvg) should result in >= 29
        assertTrue("Cycle length should reflect upward trend", prediction!!.cycleLength >= 29)
    }

    @Test
    fun `test ovulation and fertile window logic`() {
        val baseDate = LocalDate.of(2026, 1, 1)
        val cycles = listOf(
            createTestCycle(baseDate, baseDate.plusDays(5)),
            createTestCycle(baseDate.plusDays(28), baseDate.plusDays(33)),
            createTestCycle(baseDate.plusDays(56), baseDate.plusDays(61))
        )

        val prediction = predictCycle(cycles)

        val expectedOvulation = prediction!!.mostLikelyPeriodStart.minusDays(14)
        assertEquals("Ovulation day should be 14 days before next period", expectedOvulation, prediction.ovulationDay)

        assertTrue("Fertile window must include ovulation day",
            prediction.fertileWindow.contains(prediction.ovulationDay))
    }
}