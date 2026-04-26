package com.ben.periodt.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

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
    indices = [
        Index("profileId"),
        Index("syncUuid"),              // fast lookup by cloud identity
        Index(value = ["isSynced", "isDeleted"])  // fast sweep for unsynced rows
    ]
)
data class PeriodCycleEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val profileId: Int,
    val startDate: String,
    val endDate: String,
    val bleeding: String,
    val bloodColor: String,
    val painLevel: Int = 5,

    // ── Sync tracking ─────────────────────────────────────────────────────────
    // These five fields are what make a local Room record "sync-aware."
    // They are invisible to the UI — the ViewModel filters and maps
    // them away before the UI ever sees them.

    // The permanent cloud identity of this record.
    // Generated once at construction, never changes, never null.
    // This is what Device B uses to recognize a record from Device A.
    val syncUuid: String = UUID.randomUUID().toString(),

    // The server's monotonic version number for this record.
    // null  → this record has never been pushed to the server
    // 847   → the server's version of this record is 847
    // Used during conflict detection: if the server's current version
    // for this syncUuid is higher than what we stored, someone else
    // edited this record on another device.
    val serverVersion: Long? = null,

    // Timestamp of the last LOCAL change to this record.
    // Used only for local UI sorting / display — NEVER for cloud conflict resolution.
    // (Device clocks are unreliable across devices — the server's serverVersion wins.)
    val updatedAt: Long = System.currentTimeMillis(),

    // false = this record has local changes the server doesn't know about yet.
    // true  = local and server are in sync.
    // The WorkManager push job sweeps for isSynced = false.
    // Flips back to false any time the user edits this record.
    val isSynced: Boolean = false,

    // false = this record is alive and visible in the UI.
    // true  = the user deleted this record. The row stays in the DB
    //         (as a "tombstone") so the sync engine can tell the server
    //         about the deletion. Hard-deleted locally after 30 days.
    val isDeleted: Boolean = false
)