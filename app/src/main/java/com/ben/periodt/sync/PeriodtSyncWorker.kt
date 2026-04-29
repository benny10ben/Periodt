package com.ben.periodt.sync

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.ben.periodt.data.AppDatabase
import com.ben.periodt.data.ProfileEntity
import com.ben.periodt.network.ApiClient
import com.ben.periodt.network.PeriodtNetworkRepository
import com.ben.periodt.network.SyncItemDto
import com.ben.periodt.network.SyncPushRequest
import com.ben.periodt.security.TokenManager

class PeriodtSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        Log.d("SyncWorker", "Background sync started!")

        val tokenManager = TokenManager(applicationContext)
        val apiClient = ApiClient(tokenManager)
        val networkRepo = PeriodtNetworkRepository(apiClient)
        val db = AppDatabase.getDatabase(applicationContext)
        val dao = db.periodCycleDao()
        val mapper = SyncMapper()

        if (tokenManager.getToken() == null) {
            Log.d("SyncWorker", "No JWT found. Skipping sync.")
            return Result.success()
        }

        return try {
            // ==========================================
            // PART 1: THE SWEEP (PUSH LOCAL CHANGES)
            // ==========================================

            val unsyncedCycles = dao.getUnsyncedCycles()
            val unsyncedPills = dao.getUnsyncedPillPacks()
            val unsyncedLogs = dao.getUnsyncedDailyLogs()

            val pushItems = mutableListOf<SyncItemDto>()

            unsyncedCycles.forEach { entity ->
                val profileUuid = dao.getProfileUuidById(entity.profileId)
                if (profileUuid != null) pushItems.add(mapper.toSyncItem(entity, profileUuid))
            }

            unsyncedPills.forEach { entity ->
                val profileUuid = dao.getProfileUuidById(entity.profileId)
                if (profileUuid != null) pushItems.add(mapper.toSyncItem(entity, profileUuid))
            }

            unsyncedLogs.forEach { logEntity ->
                val cycleUuid = dao.getCycleSyncUuidById(logEntity.cycleId)
                if (cycleUuid != null) pushItems.add(mapper.toSyncItem(logEntity, cycleUuid))
            }

            if (pushItems.isNotEmpty()) {
                val pushResult = networkRepo.pushSyncData(SyncPushRequest(pushItems))
                pushResult.onSuccess {
                    Log.d("SyncWorker", "Successfully pushed ${pushItems.size} items.")
                    unsyncedCycles.forEach { dao.markCycleSynced(it.syncUuid, it.serverVersion ?: 0L) }
                    unsyncedPills.forEach { dao.markPillPackSynced(it.syncUuid, it.serverVersion ?: 0L) }
                    unsyncedLogs.forEach { dao.markDailyLogSynced(it.syncUuid, it.serverVersion ?: 0L) }
                }.onFailure {
                    Log.e("SyncWorker", "Push failed", it)
                    return Result.retry()
                }
            }

            // ==========================================
            // PART 2: THE MERGE (PULL SERVER UPDATES)
            // ==========================================

            val currentCursor = tokenManager.getSyncCursor()
            val pullResult = networkRepo.pullSyncData(currentCursor)

            pullResult.onSuccess { response ->
                val newItems = response.items

                if (newItems.isNotEmpty()) {
                    Log.d("SyncWorker", "Pulled ${newItems.size} new items from server.")

                    newItems.forEach { item ->
                        when (item.entityType) {

                            SyncMapper.TYPE_CYCLE -> {
                                if (item.isDeleted) {
                                    dao.deleteCycleBySyncUuid(item.syncUuid)
                                } else {
                                    val dto = mapper.extractNetworkCycleDto(item.encryptedPayload)
                                    val localId = dao.getCycleLocalIdByUuid(item.syncUuid)

                                    // MULTI-DEVICE FIX: Auto-create the profile if this phone doesn't have it yet!
                                    var targetProfileId = dao.getProfileLocalIdByUuid(dto.profileUuid)
                                    if (targetProfileId == null) {
                                        targetProfileId = dao.insertProfile(
                                            ProfileEntity(profileUuid = dto.profileUuid, name = "Synced Profile")
                                        ).toInt()
                                    }

                                    val entityToSave = com.ben.periodt.data.PeriodCycleEntity(
                                        profileId = targetProfileId,
                                        startDate = dto.startDate,
                                        endDate = dto.endDate,
                                        bleeding = dto.bleeding,
                                        bloodColor = dto.bloodColor,
                                        painLevel = dto.painLevel,
                                        syncUuid = item.syncUuid,
                                        serverVersion = item.serverVersion,
                                        isSynced = true,
                                        updatedAt = System.currentTimeMillis()
                                    )

                                    if (localId != null) dao.updateCycle(entityToSave.copy(id = localId))
                                    else dao.insertCycle(entityToSave)
                                }
                            }

                            SyncMapper.TYPE_PILL -> {
                                if (item.isDeleted) {
                                    dao.deletePillPackBySyncUuid(item.syncUuid)
                                } else {
                                    val dto = mapper.extractNetworkPillDto(item.encryptedPayload)

                                    var targetProfileId = dao.getProfileLocalIdByUuid(dto.profileUuid)
                                    if (targetProfileId == null) {
                                        targetProfileId = dao.insertProfile(
                                            ProfileEntity(profileUuid = dto.profileUuid, name = "Synced Profile")
                                        ).toInt()
                                    }

                                    val entityToSave = com.ben.periodt.data.PillPackEntity(
                                        profileId = targetProfileId,
                                        startDate = dto.startDate,
                                        pillCount = dto.pillCount,
                                        endDate = dto.endDate,
                                        syncUuid = item.syncUuid,
                                        serverVersion = item.serverVersion,
                                        isSynced = true,
                                        updatedAt = System.currentTimeMillis()
                                    )

                                    dao.deletePillPackBySyncUuid(item.syncUuid)
                                    dao.insertPillPack(entityToSave)
                                }
                            }

                            SyncMapper.TYPE_DAILY_LOG -> {
                                if (item.isDeleted) {
                                    dao.deleteDailyLogBySyncUuid(item.syncUuid)
                                } else {
                                    val dto = mapper.extractNetworkDailyLogDto(item.encryptedPayload)
                                    val localCycleId = dao.getCycleLocalIdByUuid(dto.cycleSyncUuid)

                                    if (localCycleId != null) {
                                        val entityToSave = com.ben.periodt.data.DailyCycleLogEntity(
                                            cycleId = localCycleId,
                                            date = dto.date,
                                            bleeding = dto.bleeding,
                                            bloodColor = dto.bloodColor,
                                            painLevel = dto.painLevel,
                                            syncUuid = item.syncUuid,
                                            serverVersion = item.serverVersion,
                                            isSynced = true,
                                            updatedAt = System.currentTimeMillis()
                                        )

                                        val existingLog = dao.getDailyLogForDate(localCycleId, dto.date)
                                        if (existingLog != null) dao.updateDailyLog(entityToSave.copy(id = existingLog.id))
                                        else dao.insertDailyLog(entityToSave)
                                    } else {
                                        Log.w("SyncWorker", "Skipped log. Parent cycle UUID not found locally: ${dto.cycleSyncUuid}")
                                    }
                                }
                            }
                        }
                    }
                }

                tokenManager.saveSyncCursor(response.latestCursor)
                Log.d("SyncWorker", "Sync complete. New cursor: ${response.latestCursor}")

            }.onFailure {
                Log.e("SyncWorker", "Pull failed", it)
                return Result.retry()
            }

            // ==========================================
            // PART 3: THE JANITOR (TOMBSTONE CLEANUP)
            // ==========================================
            val cutoffMs = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000)
            dao.hardDeleteStaleCycleTombstones(cutoffMs)
            dao.hardDeleteStalePillPackTombstones(cutoffMs)
            dao.hardDeleteStaleDailyLogTombstones(cutoffMs)

            return Result.success()

        } catch (e: Exception) {
            Log.e("SyncWorker", "Fatal sync error", e)
            return Result.retry()
        }
    }
}