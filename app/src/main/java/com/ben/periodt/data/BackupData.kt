package com.ben.periodt.data

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

@Keep
data class BackupCycleDto(
    @SerializedName("startDate")  val startDate: String,
    @SerializedName("endDate")    val endDate: String,
    @SerializedName("bleeding")   val bleeding: String,
    @SerializedName("bloodColor") val bloodColor: String,
    @SerializedName("painLevel")  val painLevel: Int = 5
)

@Keep
data class BackupPillState(
    @SerializedName("isOnPill")        val isOnPill: Boolean = false,
    @SerializedName("isTransitioning") val isTransitioning: Boolean = false,
    @SerializedName("packStartDate")   val packStartDate: String? = null,
    @SerializedName("pillCount")       val pillCount: Int = 21,
    @SerializedName("pillStopDate")    val pillStopDate: String? = null
)

@Keep
data class PillPackDto(
    @SerializedName("startDate") val startDate: String,
    @SerializedName("pillCount") val pillCount: Int,
    @SerializedName("endDate")   val endDate: String?
)

@Keep
data class BackupData(
    @SerializedName("version")     val version: Int = CURRENT_VERSION,
    @SerializedName("timestamp")   val timestamp: Long = System.currentTimeMillis(),
    @SerializedName("cycles")      val cycles: List<BackupCycleDto>,
    @SerializedName("pillHistory") val pillHistory: List<PillPackDto>? = null
) {
    companion object {
        /**
         * Export format version history:
         * 1 → Initial release
         * 2 → Added painLevel to cycles
         * 3 → Moved pill state from SharedPreferences to DB (pillHistory)
         */
        const val CURRENT_VERSION = 3
    }
}

// ── Mappers ──────────────────────────────────────────────────────────────────

fun PillPackEntity.toDto() = PillPackDto(startDate, pillCount, endDate)
fun PillPackDto.toEntity() = PillPackEntity(startDate = startDate, pillCount = pillCount, endDate = endDate)

fun PeriodCycleEntity.toDto() = BackupCycleDto(
    startDate  = startDate,
    endDate    = endDate,
    bleeding   = bleeding,
    bloodColor = bloodColor,
    painLevel  = painLevel
)

fun BackupCycleDto.toEntity() = PeriodCycleEntity(
    id         = 0,
    startDate  = startDate,
    endDate    = endDate,
    bleeding   = bleeding,
    bloodColor = bloodColor,
    painLevel  = painLevel
)