package com.ben.periodt.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "daily_cycle_logs",
    foreignKeys = [
        ForeignKey(
            entity        = PeriodCycleEntity::class,
            parentColumns = ["id"],
            childColumns  = ["cycleId"],
            onDelete      = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("cycleId"),
        Index("syncUuid"),
        Index(value = ["isSynced", "isDeleted"])
    ]
)
data class DailyCycleLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val cycleId: Int,
    val date: String,
    val bleeding: String,
    val bloodColor: String,
    val painLevel: Int = 5,

    // ── Sync tracking ─────────────────────────────────────────────────────────
    val syncUuid: String = UUID.randomUUID().toString(),
    val serverVersion: Long? = null,
    val updatedAt: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false,
    val isDeleted: Boolean = false
)