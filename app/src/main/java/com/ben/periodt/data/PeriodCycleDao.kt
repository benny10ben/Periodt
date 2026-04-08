package com.ben.periodt.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PeriodCycleDao {

    // --- PERIOD CYCLES ---
    @Query("SELECT * FROM period_cycles ORDER BY startDate ASC")
    fun getAllCycles(): Flow<List<PeriodCycleEntity>>

    @Query("SELECT * FROM period_cycles ORDER BY startDate ASC")
    suspend fun getAllCyclesOnce(): List<PeriodCycleEntity>

    @Insert
    suspend fun insertCycle(entity: PeriodCycleEntity)

    @Update
    suspend fun updateCycle(entity: PeriodCycleEntity)

    @Query("DELETE FROM period_cycles WHERE id = :id")
    suspend fun deleteCycleById(id: Int)

    @Query("DELETE FROM period_cycles")
    suspend fun deleteAllCycles()

    // --- PILL PACKS ---
    @Query("SELECT * FROM pill_packs ORDER BY startDate DESC")
    fun getAllPillPacks(): Flow<List<PillPackEntity>>

    @Insert
    suspend fun insertPillPack(entity: PillPackEntity)

    @Query("UPDATE pill_packs SET endDate = :endDate WHERE id = :id")
    suspend fun updatePillPackEndDate(id: Int, endDate: String)

    @Query("DELETE FROM pill_packs WHERE id = :id")
    suspend fun deletePillPackById(id: Int)

    @Query("DELETE FROM pill_packs")
    suspend fun deleteAllPillPacks()

    @Query("SELECT * FROM pill_packs ORDER BY startDate ASC")
    suspend fun getAllPillPacksOnce(): List<PillPackEntity>

    // --- DAILY CYCLE LOGS ---
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

    // Global Wipe
    @Transaction
    suspend fun deleteAll() {
        deleteAllCycles()
        deleteAllPillPacks()
        deleteAllDailyLogs()  // add this line
    }

    @Query("DELETE FROM daily_cycle_logs")
    suspend fun deleteAllDailyLogs()

    @Query("SELECT * FROM daily_cycle_logs ORDER BY date ASC")
    fun getAllDailyLogs(): Flow<List<DailyCycleLogEntity>>
}