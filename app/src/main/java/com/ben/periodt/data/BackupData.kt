package com.ben.periodt.data

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

// ── Per-day log ───────────────────────────────────────────────────────────────

@Keep
data class DailyLogDto(
    @SerializedName("cycleId")    val cycleId: Int,
    @SerializedName("date")       val date: String,
    @SerializedName("bleeding")   val bleeding: String,
    @SerializedName("bloodColor") val bloodColor: String,
    @SerializedName("painLevel")  val painLevel: Int = 5
)

// ── Cycle ─────────────────────────────────────────────────────────────────────

@Keep
data class BackupCycleDto(
    @SerializedName("startDate")  val startDate: String,
    @SerializedName("endDate")    val endDate: String,
    @SerializedName("bleeding")   val bleeding: String,
    @SerializedName("bloodColor") val bloodColor: String,
    @SerializedName("painLevel")  val painLevel: Int = 5
)

// ── Pill pack ─────────────────────────────────────────────────────────────────

@Keep
data class PillPackDto(
    @SerializedName("startDate") val startDate: String,
    @SerializedName("pillCount") val pillCount: Int,
    @SerializedName("endDate")   val endDate: String?
)

// ── v4 pill state (kept only for reading old backups) ─────────────────────────

@Keep
data class BackupPillState(
    @SerializedName("isOnPill")        val isOnPill: Boolean = false,
    @SerializedName("isTransitioning") val isTransitioning: Boolean = false,
    @SerializedName("packStartDate")   val packStartDate: String? = null,
    @SerializedName("pillCount")       val pillCount: Int = 21,
    @SerializedName("pillStopDate")    val pillStopDate: String? = null
)

// ── Profile bundle (v5) ───────────────────────────────────────────────────────

@Keep
data class BackupProfileDto(
    @SerializedName("profileUuid")  val profileUuid: String,   // permanent identity for import matching
    @SerializedName("name")         val name: String,
    @SerializedName("avatarColor")  val avatarColor: String = "#D89046",
    @SerializedName("cycles")       val cycles: List<BackupCycleDto>,
    @SerializedName("pillHistory")  val pillHistory: List<PillPackDto>,
    @SerializedName("dailyLogs")    val dailyLogs: List<DailyLogDto>
)

// ── Root backup object ────────────────────────────────────────────────────────

@Keep
data class BackupData(
    @SerializedName("version")   val version: Int = CURRENT_VERSION,
    @SerializedName("timestamp") val timestamp: Long = System.currentTimeMillis(),

    // ── v5: all data lives inside profile bundles ──────────────────────────
    @SerializedName("profiles")  val profiles: List<BackupProfileDto>? = null,

    // ── v1-v4 flat fields: only populated when reading old backups ─────────
    // Never written by v5 export. Gson will populate these when parsing an
    // old file so the ViewModel import logic can detect and handle them.
    @SerializedName("cycles")      val cycles: List<BackupCycleDto>? = null,
    @SerializedName("pillHistory") val pillHistory: List<PillPackDto>? = null,
    @SerializedName("dailyLogs")   val dailyLogs: List<DailyLogDto>? = null
) {
    companion object {
        /**
         * Export format version history:
         * 1 → Initial release
         * 2 → Added painLevel to cycles
         * 3 → Moved pill state from SharedPreferences to DB (pillHistory)
         * 4 → Added per-day bleeding/color/pain overrides (dailyLogs)
         * 5 → Multi-profile support — data wrapped in profile bundles
         */
        const val CURRENT_VERSION = 5

        /** True when this backup uses the old flat (pre-profile) format. */
        fun BackupData.isLegacy(): Boolean = profiles == null
    }
}

// ── Mappers ───────────────────────────────────────────────────────────────────

fun PillPackEntity.toDto() = PillPackDto(startDate, pillCount, endDate)
fun PillPackDto.toEntity(profileId: Int) = PillPackEntity(
    profileId = profileId,
    startDate = startDate,
    pillCount = pillCount,
    endDate   = endDate
)

fun PeriodCycleEntity.toDto() = BackupCycleDto(
    startDate  = startDate,
    endDate    = endDate,
    bleeding   = bleeding,
    bloodColor = bloodColor,
    painLevel  = painLevel
)

fun BackupCycleDto.toEntity(profileId: Int) = PeriodCycleEntity(
    id         = 0,
    profileId  = profileId,
    startDate  = startDate,
    endDate    = endDate,
    bleeding   = bleeding,
    bloodColor = bloodColor,
    painLevel  = painLevel
)

fun DailyCycleLogEntity.toDto() = DailyLogDto(
    cycleId    = cycleId,
    date       = date,
    bleeding   = bleeding,
    bloodColor = bloodColor,
    painLevel  = painLevel
)

fun DailyLogDto.toEntity(resolvedCycleId: Int) = DailyCycleLogEntity(
    cycleId    = resolvedCycleId,
    date       = date,
    bleeding   = bleeding,
    bloodColor = bloodColor,
    painLevel  = painLevel
)

fun ProfileEntity.toBackupDto(
    cycles: List<BackupCycleDto>,
    pillHistory: List<PillPackDto>,
    dailyLogs: List<DailyLogDto>
) = BackupProfileDto(
    profileUuid = profileUuid,
    name        = name,
    avatarColor = avatarColor,
    cycles      = cycles,
    pillHistory = pillHistory,
    dailyLogs   = dailyLogs
)