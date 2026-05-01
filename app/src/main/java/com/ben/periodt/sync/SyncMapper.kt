package com.ben.periodt.sync

import androidx.annotation.Keep
import com.ben.periodt.data.DailyCycleLogEntity
import com.ben.periodt.data.PeriodCycleEntity
import com.ben.periodt.data.PillPackEntity
import com.ben.periodt.data.ProfileEntity
import com.ben.periodt.network.SyncItemDto
import com.ben.periodt.security.SyncCryptoManager
import com.google.gson.Gson

@Keep
data class NetworkCycleDto(
    val profileUuid: String,
    val profileName: String? = null,
    val profileColor: String? = null,
    val startDate: String, val endDate: String,
    val bleeding: String, val bloodColor: String, val painLevel: Int
)

@Keep
data class NetworkPillPackDto(
    val profileUuid: String,
    val profileName: String? = null,
    val profileColor: String? = null,
    val startDate: String, val pillCount: Int, val endDate: String?
)

@Keep
data class NetworkDailyLogDto(
    val cycleSyncUuid: String, val date: String, val bleeding: String, val bloodColor: String, val painLevel: Int
)

@Keep
data class NetworkProfileDto(
    val profileUuid: String,
    val name: String,
    val avatarColor: String,
    val createdAt: Long
)

class SyncMapper {
    private val gson = Gson()

    companion object {
        const val TYPE_PROFILE = "PROFILE"
        const val TYPE_CYCLE = "CYCLE"
        const val TYPE_PILL = "PILL"
        const val TYPE_DAILY_LOG = "DAILY_LOG"
    }

// --- PUSH ---

    // 2. Update all Push mappers to include entity.updatedAt AND pass createdAt in Profile payload
    fun toSyncItem(entity: ProfileEntity): SyncItemDto {
        val payload = NetworkProfileDto(entity.profileUuid, entity.name, entity.avatarColor, entity.createdAt)
        val encryptedPayload = SyncCryptoManager.encryptPayload(gson.toJson(payload))
        return SyncItemDto(entity.profileUuid, TYPE_PROFILE, encryptedPayload, entity.isDeleted, entity.serverVersion, entity.updatedAt)
    }

    fun toSyncItem(entity: PeriodCycleEntity, profile: ProfileEntity): SyncItemDto {
        val payload = NetworkCycleDto(profile.profileUuid, profile.name, profile.avatarColor, entity.startDate, entity.endDate, entity.bleeding, entity.bloodColor, entity.painLevel)
        val encryptedPayload = SyncCryptoManager.encryptPayload(gson.toJson(payload))
        return SyncItemDto(entity.syncUuid, TYPE_CYCLE, encryptedPayload, entity.isDeleted, entity.serverVersion, entity.updatedAt)
    }

    fun toSyncItem(entity: PillPackEntity, profile: ProfileEntity): SyncItemDto {
        val payload = NetworkPillPackDto(profile.profileUuid, profile.name, profile.avatarColor, entity.startDate, entity.pillCount, entity.endDate)
        val encryptedPayload = SyncCryptoManager.encryptPayload(gson.toJson(payload))
        return SyncItemDto(entity.syncUuid, TYPE_PILL, encryptedPayload, entity.isDeleted, entity.serverVersion, entity.updatedAt)
    }

    fun toSyncItem(entity: DailyCycleLogEntity, cycleSyncUuid: String): SyncItemDto {
        val payload = NetworkDailyLogDto(cycleSyncUuid, entity.date, entity.bleeding, entity.bloodColor, entity.painLevel)
        val encryptedPayload = SyncCryptoManager.encryptPayload(gson.toJson(payload))
        return SyncItemDto(entity.syncUuid, TYPE_DAILY_LOG, encryptedPayload, entity.isDeleted, entity.serverVersion, entity.updatedAt)
    }

    // --- PULL ---
    fun extractNetworkProfileDto(encryptedPayload: String): NetworkProfileDto {
        val decryptedJson = SyncCryptoManager.decryptPayload(encryptedPayload)
        return gson.fromJson(decryptedJson, NetworkProfileDto::class.java)
    }

    fun extractNetworkCycleDto(encryptedPayload: String): NetworkCycleDto {
        val decryptedJson = SyncCryptoManager.decryptPayload(encryptedPayload)
        return gson.fromJson(decryptedJson, NetworkCycleDto::class.java)
    }

    fun extractNetworkPillDto(encryptedPayload: String): NetworkPillPackDto {
        val decryptedJson = SyncCryptoManager.decryptPayload(encryptedPayload)
        return gson.fromJson(decryptedJson, NetworkPillPackDto::class.java)
    }

    fun extractNetworkDailyLogDto(encryptedPayload: String): NetworkDailyLogDto {
        val decryptedJson = SyncCryptoManager.decryptPayload(encryptedPayload)
        return gson.fromJson(decryptedJson, NetworkDailyLogDto::class.java)
    }
}