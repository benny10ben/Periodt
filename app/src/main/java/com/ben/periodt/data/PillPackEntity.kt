package com.ben.periodt.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "pill_packs",
    foreignKeys = [
        ForeignKey(
            entity        = ProfileEntity::class,
            parentColumns = ["id"],
            childColumns  = ["profileId"],
            onDelete      = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("profileId"),
        Index("syncUuid"),
        Index(value = ["isSynced", "isDeleted"])
    ]
)
data class PillPackEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val profileId: Int,
    val startDate: String,
    val pillCount: Int,
    val endDate: String? = null,

    // ── Sync tracking ─────────────────────────────────────────────────────────
    val syncUuid: String = UUID.randomUUID().toString(),
    val serverVersion: Long? = null,
    val updatedAt: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false,
    val isDeleted: Boolean = false
)