package com.ben.periodt.data

import com.google.gson.annotations.SerializedName

/**
 * Sealed hierarchy for all records that travel to/from the cloud server.
 * These are serialized to JSON, encrypted, and stored in the server's database.
 * * CRITICAL RULE: No local integer IDs anywhere in this hierarchy.
 * All relationships reference parent entities by their UUID.
 */
sealed class SyncPayload {
    abstract val payloadVersion: Int
}

// ── Profile ───────────────────────────────────────────────────────────────────

data class ProfileSyncPayload(
    @SerializedName("payloadVersion") override val payloadVersion: Int = 1,
    @SerializedName("profileUuid")    val profileUuid: String,
    @SerializedName("name")           val name: String,
    @SerializedName("avatarColor")    val avatarColor: String,
    @SerializedName("createdAt")      val createdAt: Long
) : SyncPayload()

// ── Cycle ─────────────────────────────────────────────────────────────────────

data class CycleSyncPayload(
    @SerializedName("payloadVersion") override val payloadVersion: Int = 1,
    @SerializedName("syncUuid")       val syncUuid: String,
    @SerializedName("profileUuid")    val profileUuid: String,
    @SerializedName("startDate")      val startDate: String,
    @SerializedName("endDate")        val endDate: String,
    @SerializedName("bleeding")       val bleeding: String,
    @SerializedName("bloodColor")     val bloodColor: String,
    @SerializedName("painLevel")      val painLevel: Int
) : SyncPayload()

// ── Pill Pack ─────────────────────────────────────────────────────────────────

data class PillPackSyncPayload(
    @SerializedName("payloadVersion") override val payloadVersion: Int = 1,
    @SerializedName("syncUuid")       val syncUuid: String,
    @SerializedName("profileUuid")    val profileUuid: String,
    @SerializedName("startDate")      val startDate: String,
    @SerializedName("pillCount")      val pillCount: Int,
    @SerializedName("endDate")        val endDate: String?
) : SyncPayload()

// ── Daily Cycle Log ───────────────────────────────────────────────────────────

data class DailyLogSyncPayload(
    @SerializedName("payloadVersion") override val payloadVersion: Int = 1,
    @SerializedName("syncUuid")       val syncUuid: String,
    @SerializedName("cycleSyncUuid")  val cycleSyncUuid: String,
    @SerializedName("date")           val date: String,
    @SerializedName("bleeding")       val bleeding: String,
    @SerializedName("bloodColor")     val bloodColor: String,
    @SerializedName("painLevel")      val painLevel: Int
) : SyncPayload()

// ==============================================================================
// ── MAPPERS (Entity -> SyncPayload) ───────────────────────────────────────────
// ==============================================================================

fun ProfileEntity.toSyncPayload() = ProfileSyncPayload(
    profileUuid = profileUuid,
    name        = name,
    avatarColor = avatarColor,
    createdAt   = createdAt
)

fun PeriodCycleEntity.toSyncPayload(profileUuid: String) = CycleSyncPayload(
    syncUuid    = syncUuid,
    profileUuid = profileUuid,
    startDate   = startDate,
    endDate     = endDate,
    bleeding    = bleeding,
    bloodColor  = bloodColor,
    painLevel   = painLevel
)

fun PillPackEntity.toSyncPayload(profileUuid: String) = PillPackSyncPayload(
    syncUuid    = syncUuid,
    profileUuid = profileUuid,
    startDate   = startDate,
    pillCount   = pillCount,
    endDate     = endDate
)

fun DailyCycleLogEntity.toSyncPayload(cycleSyncUuid: String) = DailyLogSyncPayload(
    syncUuid      = syncUuid,
    cycleSyncUuid = cycleSyncUuid,
    date          = date,
    bleeding      = bleeding,
    bloodColor    = bloodColor,
    painLevel     = painLevel
)