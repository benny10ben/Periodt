package com.ben.periodt.sync

import androidx.annotation.Keep
import com.ben.periodt.data.DailyCycleLogEntity
import com.ben.periodt.data.PeriodCycleEntity
import com.ben.periodt.data.PillPackEntity
import com.ben.periodt.network.SyncItemDto
import com.ben.periodt.security.CryptoEngine
import com.google.gson.Gson

@Keep
data class NetworkCycleDto(
    val profileUuid: String,
    val startDate: String,
    val endDate: String,
    val bleeding: String,
    val bloodColor: String,
    val painLevel: Int
)

@Keep
data class NetworkPillPackDto(
    val profileUuid: String,
    val startDate: String,
    val pillCount: Int,
    val endDate: String?
)

@Keep
data class NetworkDailyLogDto(
    val cycleSyncUuid: String,
    val date: String,
    val bleeding: String,
    val bloodColor: String,
    val painLevel: Int
)

class SyncMapper {
    private val gson = Gson()

    companion object {
        const val TYPE_CYCLE = "CYCLE"
        const val TYPE_PILL = "PILL"
        const val TYPE_DAILY_LOG = "DAILY_LOG"
    }

    // ==========================================
    // PUSH: Local Entities -> Network DTOs
    // ==========================================

    fun toSyncItem(entity: PeriodCycleEntity, profileUuid: String): SyncItemDto {
        val payload = NetworkCycleDto(profileUuid, entity.startDate, entity.endDate, entity.bleeding, entity.bloodColor, entity.painLevel)
        val encryptedPayload = CryptoEngine.encrypt(gson.toJson(payload))
        return SyncItemDto(entity.syncUuid, TYPE_CYCLE, encryptedPayload, entity.isDeleted, entity.serverVersion)
    }

    fun toSyncItem(entity: PillPackEntity, profileUuid: String): SyncItemDto {
        val payload = NetworkPillPackDto(profileUuid, entity.startDate, entity.pillCount, entity.endDate)
        val encryptedPayload = CryptoEngine.encrypt(gson.toJson(payload))
        return SyncItemDto(entity.syncUuid, TYPE_PILL, encryptedPayload, entity.isDeleted, entity.serverVersion)
    }

    fun toSyncItem(entity: DailyCycleLogEntity, cycleSyncUuid: String): SyncItemDto {
        val payload = NetworkDailyLogDto(cycleSyncUuid, entity.date, entity.bleeding, entity.bloodColor, entity.painLevel)
        val encryptedPayload = CryptoEngine.encrypt(gson.toJson(payload))
        return SyncItemDto(entity.syncUuid, TYPE_DAILY_LOG, encryptedPayload, entity.isDeleted, entity.serverVersion)
    }

    // ==========================================
    // PULL: Network DTOs -> Local JSON Payloads
    // ==========================================

    fun extractNetworkCycleDto(encryptedPayload: String): NetworkCycleDto {
        val decryptedJson = CryptoEngine.decrypt(encryptedPayload)
        return gson.fromJson(decryptedJson, NetworkCycleDto::class.java)
    }

    fun extractNetworkPillDto(encryptedPayload: String): NetworkPillPackDto {
        val decryptedJson = CryptoEngine.decrypt(encryptedPayload)
        return gson.fromJson(decryptedJson, NetworkPillPackDto::class.java)
    }

    fun extractNetworkDailyLogDto(encryptedPayload: String): NetworkDailyLogDto {
        val decryptedJson = CryptoEngine.decrypt(encryptedPayload)
        return gson.fromJson(decryptedJson, NetworkDailyLogDto::class.java)
    }
}