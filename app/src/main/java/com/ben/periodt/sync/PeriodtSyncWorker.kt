package com.ben.periodt.sync

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.ben.periodt.data.AppDatabase
import com.ben.periodt.data.DailyCycleLogEntity
import com.ben.periodt.data.PeriodCycleEntity
import com.ben.periodt.data.PillPackEntity
import com.ben.periodt.data.ProfileEntity
import com.ben.periodt.network.ApiClient
import com.ben.periodt.network.PeriodtNetworkRepository
import com.ben.periodt.network.SyncItemDto
import com.ben.periodt.network.SyncPushRequest
import com.ben.periodt.security.SecureVault
import com.ben.periodt.security.SyncCryptoManager
import com.ben.periodt.security.TokenManager
import kotlinx.coroutines.CancellationException

class PeriodtSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        Log.d("SyncWorker", "Background sync started!")

        val tokenManager = TokenManager(applicationContext)
        val secureVault = SecureVault(applicationContext)
        val apiClient = ApiClient(tokenManager)
        val networkRepo = PeriodtNetworkRepository(apiClient)
        val db = AppDatabase.getDatabase(applicationContext)
        val dao = db.periodCycleDao()
        val mapper = SyncMapper()

        // Restore session key from secure vault if the process was killed and restarted.
        if (SyncCryptoManager.sessionDataKey == null) {
            val localDek = secureVault.getAesKey()?.encoded
            if (localDek != null) {
                SyncCryptoManager.sessionDataKey = localDek
            }
        }

        if (tokenManager.getToken() == null || SyncCryptoManager.sessionDataKey == null) {
            Log.d("SyncWorker", "Missing JWT or AES Key. Skipping sync.")
            return Result.success()
        }

        return try {
            // ==========================================
            // PART 1: THE SWEEP (PUSH LOCAL CHANGES)
            // ==========================================
            val unsyncedProfiles = dao.getUnsyncedProfiles()
            val unsyncedCycles   = dao.getUnsyncedCycles()
            val unsyncedPills    = dao.getUnsyncedPillPacks()
            val unsyncedLogs     = dao.getUnsyncedDailyLogs()

            val pushItems = mutableListOf<SyncItemDto>()

            unsyncedProfiles.forEach { profile ->
                pushItems.add(mapper.toSyncItem(profile))
            }
            unsyncedCycles.forEach { entity ->
                val profile = dao.getProfileById(entity.profileId)
                if (profile != null) pushItems.add(mapper.toSyncItem(entity, profile))
            }
            unsyncedPills.forEach { entity ->
                val profile = dao.getProfileById(entity.profileId)
                if (profile != null) pushItems.add(mapper.toSyncItem(entity, profile))
            }
            unsyncedLogs.forEach { logEntity ->
                val cycleUuid = dao.getCycleSyncUuidById(logEntity.cycleId)
                if (cycleUuid != null) pushItems.add(mapper.toSyncItem(logEntity, cycleUuid))
            }

            if (pushItems.isNotEmpty()) {
                val pushResult = networkRepo.pushSyncData(SyncPushRequest(pushItems))
                pushResult.onSuccess {
                    Log.d("SyncWorker", "Successfully pushed ${pushItems.size} items.")

                    // Use a shared timestamp as the version for all items pushed in this
                    // batch. If your server returns per-item versions in its push response,
                    // use those instead — they are more precise for conflict detection.
                    val syncedAt = System.currentTimeMillis()
                    unsyncedProfiles.forEach { dao.markProfileSynced(it.profileUuid, syncedAt) }
                    unsyncedCycles.forEach   { dao.markCycleSynced(it.syncUuid, syncedAt) }
                    unsyncedPills.forEach    { dao.markPillPackSynced(it.syncUuid, syncedAt) }
                    unsyncedLogs.forEach     { dao.markDailyLogSynced(it.syncUuid, syncedAt) }
                }.onFailure {
                    Log.e("SyncWorker", "Push failed", it)
                    return Result.retry()
                }
            }

            // ==========================================
            // PART 2: THE MERGE (PULL SERVER UPDATES)
            // ==========================================
            val currentCursor = tokenManager.getSyncCursor()
            val pullResult    = networkRepo.pullSyncData(currentCursor)

            pullResult.onSuccess { response ->
                val newItems = response.items

                if (newItems.isNotEmpty()) {
                    Log.d("SyncWorker", "Pulled ${newItems.size} new items from server.")

                    newItems.forEach { item ->
                        when (item.entityType) {

                            // ── PROFILE ───────────────────────────────────────────────────────
                            SyncMapper.TYPE_PROFILE -> {
                                if (item.isDeleted == true) {
                                    val localId = dao.getProfileLocalIdByUuid(item.syncUuid)
                                    if (localId != null) dao.deleteProfileById(localId)
                                } else {
                                    val dto     = mapper.extractNetworkProfileDto(item.encryptedPayload)
                                    val localId = dao.getProfileLocalIdByUuid(dto.profileUuid)

                                    if (localId != null) {
                                        // Profile already exists locally — update it.
                                        val existing = dao.getProfileById(localId)
                                        if (existing != null) {
                                            dao.updateProfile(existing.copy(
                                                name          = dto.name,
                                                avatarColor   = dto.avatarColor,
                                                isSynced      = true,
                                                serverVersion = item.serverVersion
                                                // Note: We intentionally do NOT update createdAt here
                                            ))
                                        }
                                    } else {
                                        // Profile is new to this device.
                                        // SMART DEFAULT CLEANUP: If the auto-generated "Me" profile
                                        // is still sitting around empty, delete it so it doesn't
                                        // sit next to our downloaded profiles.
                                        val emptyProfileId = dao.getEmptyProfileId()
                                        if (emptyProfileId != null) {
                                            dao.deleteProfileById(emptyProfileId)
                                        }

                                        // Insert the pulled profile exactly as it is.
                                        dao.insertProfile(
                                            ProfileEntity(
                                                profileUuid   = dto.profileUuid,
                                                name          = dto.name,
                                                avatarColor   = dto.avatarColor,
                                                createdAt     = dto.createdAt, // FIXED: Preserve chronology!
                                                isSynced      = true,
                                                serverVersion = item.serverVersion,
                                                updatedAt     = System.currentTimeMillis()
                                            )
                                        )
                                    }
                                }
                            }

                            // ── CYCLE ─────────────────────────────────────────────────────────
                            SyncMapper.TYPE_CYCLE -> {
                                if (item.isDeleted) {
                                    dao.deleteCycleBySyncUuid(item.syncUuid)
                                } else {
                                    val dto             = mapper.extractNetworkCycleDto(item.encryptedPayload)
                                    val localId         = dao.getCycleLocalIdByUuid(item.syncUuid)
                                    val targetProfileId = dao.getProfileLocalIdByUuid(dto.profileUuid)

                                    if (targetProfileId != null) {
                                        val entityToSave = PeriodCycleEntity(
                                            id            = localId ?: 0,
                                            profileId     = targetProfileId,
                                            startDate     = dto.startDate,
                                            endDate       = dto.endDate,
                                            bleeding      = dto.bleeding,
                                            bloodColor    = dto.bloodColor,
                                            painLevel     = dto.painLevel,
                                            syncUuid      = item.syncUuid,
                                            serverVersion = item.serverVersion,
                                            isSynced      = true,
                                            updatedAt     = System.currentTimeMillis()
                                        )
                                        if (localId != null) dao.updateCycle(entityToSave)
                                        else dao.insertCycle(entityToSave)
                                    }
                                }
                            }

                            // ── PILL PACK ─────────────────────────────────────────────────────
                            SyncMapper.TYPE_PILL -> {
                                if (item.isDeleted) {
                                    dao.deletePillPackBySyncUuid(item.syncUuid)
                                } else {
                                    val dto             = mapper.extractNetworkPillDto(item.encryptedPayload)
                                    val targetProfileId = dao.getProfileLocalIdByUuid(dto.profileUuid)

                                    if (targetProfileId != null) {
                                        // Look up the existing local id first so we can do a proper
                                        // update (preserving the integer PK) rather than
                                        // delete-then-insert (which breaks in-flight UI references).
                                        val existingLocalId = dao.getPillPackLocalIdByUuid(item.syncUuid)
                                        val entityToSave = PillPackEntity(
                                            id            = existingLocalId ?: 0,
                                            profileId     = targetProfileId,
                                            startDate     = dto.startDate,
                                            pillCount     = dto.pillCount,
                                            endDate       = dto.endDate,
                                            syncUuid      = item.syncUuid,
                                            serverVersion = item.serverVersion,
                                            isSynced      = true,
                                            updatedAt     = System.currentTimeMillis()
                                        )
                                        if (existingLocalId != null) dao.updatePillPack(entityToSave)
                                        else dao.insertPillPack(entityToSave)
                                    }
                                }
                            }

                            // ── DAILY LOG ─────────────────────────────────────────────────────
                            SyncMapper.TYPE_DAILY_LOG -> {
                                if (item.isDeleted) {
                                    dao.deleteDailyLogBySyncUuid(item.syncUuid)
                                } else {
                                    val dto          = mapper.extractNetworkDailyLogDto(item.encryptedPayload)
                                    val localCycleId = dao.getCycleLocalIdByUuid(dto.cycleSyncUuid)

                                    if (localCycleId != null) {
                                        val existingLog  = dao.getDailyLogForDate(localCycleId, dto.date)
                                        val entityToSave = DailyCycleLogEntity(
                                            id            = existingLog?.id ?: 0,
                                            cycleId       = localCycleId,
                                            date          = dto.date,
                                            bleeding      = dto.bleeding,
                                            bloodColor    = dto.bloodColor,
                                            painLevel     = dto.painLevel,
                                            syncUuid      = item.syncUuid,
                                            serverVersion = item.serverVersion,
                                            isSynced      = true,
                                            updatedAt     = System.currentTimeMillis()
                                        )
                                        if (existingLog != null) dao.updateDailyLog(entityToSave)
                                        else dao.insertDailyLog(entityToSave)
                                    }
                                }
                            }
                        }
                    }
                }

                tokenManager.saveSyncCursor(response.latestCursor)

            }.onFailure {
                Log.e("SyncWorker", "Pull failed", it)
                return Result.retry()
            }

            // Hard-delete tombstones older than 30 days that have been acknowledged
            // by the server (isSynced = 1). Matches the 30-day comment in entities.
            val cutoffMs = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000)
            dao.hardDeleteStaleProfileTombstones(cutoffMs)
            dao.hardDeleteStaleCycleTombstones(cutoffMs)
            dao.hardDeleteStalePillPackTombstones(cutoffMs)
            dao.hardDeleteStaleDailyLogTombstones(cutoffMs)

            Result.success()

        } catch (e: CancellationException) {
            Log.i("SyncWorker", "Sync was cancelled (likely by a new sync request).")
            throw e
        } catch (e: Exception) {
            Log.e("SyncWorker", "Fatal sync error", e)
            Result.retry()
        }
    }
}