package com.ben.periodt.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PeriodCycleDao {

    // Observable for screens
    @Query("SELECT * FROM period_cycles ORDER BY startDate ASC")
    fun getAllCycles(): Flow<List<PeriodCycleEntity>>

    // One-shot suspend (e.g., BootReceiver/work)
    @Query("SELECT * FROM period_cycles ORDER BY startDate ASC")
    suspend fun getAllCyclesOnce(): List<PeriodCycleEntity>

    // Synchronous one-shot (widget only; call on Dispatchers.IO)
    @Query("SELECT * FROM period_cycles ORDER BY startDate DESC")
    fun getAllCyclesNow(): List<PeriodCycleEntity>

    @Insert
    suspend fun insertCycle(entity: PeriodCycleEntity)

    @Query("DELETE FROM period_cycles WHERE id = :id")
    suspend fun deleteCycleById(id: Int)
}
