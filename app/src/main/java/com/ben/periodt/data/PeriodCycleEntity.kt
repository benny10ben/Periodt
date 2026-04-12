package com.ben.periodt.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "period_cycles",
    foreignKeys = [
        ForeignKey(
            entity        = ProfileEntity::class,
            parentColumns = ["id"],
            childColumns  = ["profileId"],
            onDelete      = ForeignKey.CASCADE
        )
    ],
    indices = [Index("profileId")]
)
data class PeriodCycleEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val profileId: Int,
    val startDate: String,
    val endDate: String,
    val bleeding: String,
    val bloodColor: String,
    val painLevel: Int = 5
)