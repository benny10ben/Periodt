package com.ben.periodt.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PeriodCycleDao {

    // ── PROFILES ──────────────────────────────────────────────────────────────

    @Query("SELECT * FROM profiles ORDER BY createdAt ASC")
    fun getAllProfiles(): Flow<List<ProfileEntity>>

    @Query("SELECT * FROM profiles ORDER BY createdAt ASC")
    suspend fun getAllProfilesOnce(): List<ProfileEntity>

    @Query("SELECT * FROM profiles WHERE id = :id LIMIT 1")
    suspend fun getProfileById(id: Int): ProfileEntity?

    @Insert
    suspend fun insertProfile(entity: ProfileEntity): Long   // returns the new profile's id

    @Update
    suspend fun updateProfile(entity: ProfileEntity)

    @Query("DELETE FROM profiles WHERE id = :id")
    suspend fun deleteProfileById(id: Int)

    // ── PERIOD CYCLES ─────────────────────────────────────────────────────────

    // Profile-scoped live stream — used by the ViewModel for the active profile
    @Query("SELECT * FROM period_cycles WHERE profileId = :profileId ORDER BY startDate ASC")
    fun getCyclesForProfile(profileId: Int): Flow<List<PeriodCycleEntity>>

    // Profile-scoped one-shot — used by export and reminder receivers
    @Query("SELECT * FROM period_cycles WHERE profileId = :profileId ORDER BY startDate ASC")
    suspend fun getCyclesForProfileOnce(profileId: Int): List<PeriodCycleEntity>

    // Global one-shot — kept for export (exports ALL profiles at once)
    @Query("SELECT * FROM period_cycles ORDER BY startDate ASC")
    suspend fun getAllCyclesOnce(): List<PeriodCycleEntity>

    // Legacy global stream — kept so nothing breaks before ViewModel is updated
    @Query("SELECT * FROM period_cycles ORDER BY startDate ASC")
    fun getAllCycles(): Flow<List<PeriodCycleEntity>>

    @Insert
    suspend fun insertCycle(entity: PeriodCycleEntity)

    @Update
    suspend fun updateCycle(entity: PeriodCycleEntity)

    @Query("DELETE FROM period_cycles WHERE id = :id")
    suspend fun deleteCycleById(id: Int)

    @Query("DELETE FROM period_cycles WHERE profileId = :profileId")
    suspend fun deleteCyclesForProfile(profileId: Int)

    @Query("DELETE FROM period_cycles")
    suspend fun deleteAllCycles()

    // ── PILL PACKS ────────────────────────────────────────────────────────────

    // Profile-scoped live stream
    @Query("SELECT * FROM pill_packs WHERE profileId = :profileId ORDER BY startDate DESC")
    fun getPillPacksForProfile(profileId: Int): Flow<List<PillPackEntity>>

    // Profile-scoped one-shot
    @Query("SELECT * FROM pill_packs WHERE profileId = :profileId ORDER BY startDate ASC")
    suspend fun getPillPacksForProfileOnce(profileId: Int): List<PillPackEntity>

    // Global one-shot — kept for export
    @Query("SELECT * FROM pill_packs ORDER BY startDate ASC")
    suspend fun getAllPillPacksOnce(): List<PillPackEntity>

    // Legacy global stream
    @Query("SELECT * FROM pill_packs ORDER BY startDate DESC")
    fun getAllPillPacks(): Flow<List<PillPackEntity>>

    @Insert
    suspend fun insertPillPack(entity: PillPackEntity)

    @Query("UPDATE pill_packs SET endDate = :endDate WHERE id = :id")
    suspend fun updatePillPackEndDate(id: Int, endDate: String)

    @Query("DELETE FROM pill_packs WHERE id = :id")
    suspend fun deletePillPackById(id: Int)

    @Query("DELETE FROM pill_packs WHERE profileId = :profileId")
    suspend fun deletePillPacksForProfile(profileId: Int)

    @Query("DELETE FROM pill_packs")
    suspend fun deleteAllPillPacks()

    // ── DAILY CYCLE LOGS ──────────────────────────────────────────────────────
    // daily_cycle_logs has no profileId — it inherits profile ownership through
    // its parent cycle via CASCADE DELETE. No changes needed here.

    @Query("SELECT * FROM daily_cycle_logs WHERE cycleId = :cycleId ORDER BY date ASC")
    fun getDailyLogsForCycle(cycleId: Int): Flow<List<DailyCycleLogEntity>>

    @Query("SELECT * FROM daily_cycle_logs ORDER BY date ASC")
    suspend fun getAllDailyLogsOnce(): List<DailyCycleLogEntity>

    @Query("SELECT * FROM daily_cycle_logs WHERE cycleId = :cycleId AND date = :date LIMIT 1")
    suspend fun getDailyLogForDate(cycleId: Int, date: String): DailyCycleLogEntity?

    @Insert
    suspend fun insertDailyLog(entity: DailyCycleLogEntity)

    @Update
    suspend fun updateDailyLog(entity: DailyCycleLogEntity)

    @Query("DELETE FROM daily_cycle_logs WHERE cycleId = :cycleId AND date = :date")
    suspend fun deleteDailyLog(cycleId: Int, date: String)

    @Query("DELETE FROM daily_cycle_logs WHERE cycleId = :cycleId")
    suspend fun deleteDailyLogsForCycle(cycleId: Int)

    @Query("DELETE FROM daily_cycle_logs")
    suspend fun deleteAllDailyLogs()

    @Query("SELECT * FROM daily_cycle_logs ORDER BY date ASC")
    fun getAllDailyLogs(): Flow<List<DailyCycleLogEntity>>

    // ── GLOBAL WIPE ───────────────────────────────────────────────────────────

    @Transaction
    suspend fun deleteAll() {
        deleteAllCycles()       // cascades to daily_cycle_logs automatically
        deleteAllPillPacks()
        deleteAllDailyLogs()    // safety net in case cascade didn't catch any
        deleteAllProfiles()
    }

    @Query("DELETE FROM profiles")
    suspend fun deleteAllProfiles()

    // ── PROFILE WIPE (single profile) ────────────────────────────────────────
    // Deleting the profile row itself triggers CASCADE DELETE on period_cycles,
    // which then cascades to daily_cycle_logs. The two manual deletes below are
    // a safety net for pill_packs which has its own FK to profiles.

    @Transaction
    suspend fun deleteProfile(profileId: Int) {
        deletePillPacksForProfile(profileId)  // pill_packs FK to profiles
        deleteProfileById(profileId)          // cascades to period_cycles → daily_cycle_logs
    }
}