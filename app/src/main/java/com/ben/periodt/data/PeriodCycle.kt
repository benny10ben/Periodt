package com.ben.periodt.data

import java.time.LocalDate

data class PeriodCycle(
    val id: Int,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val bleeding: String, // Heavy, Medium, Light, Spotting
    val bloodColor: String // Bright Red, Dark Red, Brown
)
