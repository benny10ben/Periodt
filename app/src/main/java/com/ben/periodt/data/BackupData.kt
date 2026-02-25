package com.ben.periodt.data

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

/**
 * @Keep tells R8 directly on the class — no proguard rules file needed.
 * This is more reliable than proguard rules because it travels with the
 * source code and can't be missed or overridden by the base rules file.
 */
@Keep
data class BackupCycleDto(
    @SerializedName("startDate")  val startDate: String,
    @SerializedName("endDate")    val endDate: String,
    @SerializedName("bleeding")   val bleeding: String,
    @SerializedName("bloodColor") val bloodColor: String,
    @SerializedName("painLevel")  val painLevel: Int = 5
)

@Keep
data class BackupData(
    @SerializedName("version")   val version: Int = 1,
    @SerializedName("timestamp") val timestamp: Long = System.currentTimeMillis(),
    @SerializedName("cycles")    val cycles: List<BackupCycleDto>
)

// --- Mapping helpers ---

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