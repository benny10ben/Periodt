package com.ben.periodt.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "period_cycles")
data class PeriodCycleEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val startDate: String,
    val endDate: String, // store "" if not set
    val bleeding: String,
    val bloodColor: String,
    val painLevel: Int = 5
)
