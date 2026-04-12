package com.ben.periodt.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "daily_cycle_logs",
    foreignKeys = [
        ForeignKey(
            entity        = PeriodCycleEntity::class,
            parentColumns = ["id"],
            childColumns  = ["cycleId"],
            onDelete      = ForeignKey.CASCADE  // auto-deletes logs if parent cycle is deleted
        )
    ],
    indices = [Index("cycleId")]
)
data class DailyCycleLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val cycleId: Int,
    val date: String,
    val bleeding: String,
    val bloodColor: String,
    val painLevel: Int = 5
)