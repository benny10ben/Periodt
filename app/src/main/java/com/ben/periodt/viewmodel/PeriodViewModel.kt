package com.ben.periodt.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ben.periodt.data.AppDatabase
import com.ben.periodt.data.BackupData
import com.ben.periodt.data.toDto
import com.ben.periodt.data.toEntity
import com.ben.periodt.uiux.shared.Prediction
import com.ben.periodt.uiux.shared.predictCycle
import com.ben.periodt.widget.CalendarWidgetProvider
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.time.LocalDate

class PeriodViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = AppDatabase.getDatabase(application).periodCycleDao()
    private val appContext = application.applicationContext

    companion object {
        /**
         * CURRENT_EXPORT_VERSION
         *
         * History:
         *   1 → Initial release (startDate, endDate, bleeding, bloodColor)
         *   2 → Added painLevel column (DB migration 1→2)
         *
         * Increment this whenever you add new fields to BackupCycleDto
         * AND add a corresponding bridge block in performImport().
         */
        private const val CURRENT_EXPORT_VERSION = 2
    }

    // --- DATA STREAMS ---

    val cycles: StateFlow<List<Cycle>> = dao.getAllCycles()
        .map { list ->
            list.map { entity ->
                Cycle(
                    id         = entity.id,
                    startDate  = LocalDate.parse(entity.startDate),
                    endDate    = entity.endDate.takeIf { it.isNotBlank() }?.let { LocalDate.parse(it) },
                    bleeding   = entity.bleeding,
                    bloodColor = entity.bloodColor,
                    painLevel  = entity.painLevel
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val prediction: StateFlow<Prediction?> = cycles
        .map { predictCycle(it) }
        .stateIn(viewModelScope, SharingStarted.Lazily, null)

    // --- CRUD OPERATIONS ---

    fun addCycle(start: LocalDate, end: LocalDate?, bleeding: String, bloodColor: String, painLevel: Int) {
        val entity = com.ben.periodt.data.PeriodCycleEntity(
            startDate  = start.toString(),
            endDate    = end?.toString() ?: "",
            bleeding   = bleeding,
            bloodColor = bloodColor,
            painLevel  = painLevel
        )
        viewModelScope.launch {
            dao.insertCycle(entity)
            CalendarWidgetProvider.refreshAll(appContext)
        }
    }

    fun updateCycle(cycle: Cycle) {
        val entity = com.ben.periodt.data.PeriodCycleEntity(
            id         = cycle.id,
            startDate  = cycle.startDate.toString(),
            endDate    = cycle.endDate?.toString() ?: "",
            bleeding   = cycle.bleeding,
            bloodColor = cycle.bloodColor,
            painLevel  = cycle.painLevel
        )
        viewModelScope.launch {
            dao.updateCycle(entity)
            CalendarWidgetProvider.refreshAll(appContext)
        }
    }

    fun deleteCycle(id: Int) = viewModelScope.launch {
        dao.deleteCycleById(id)
        CalendarWidgetProvider.refreshAll(appContext)
    }

    // --- EXPORT & IMPORT (FUTURE-PROOFED) ---

    /**
     * EXPORT: Maps entities → DTOs, then writes versioned JSON.
     * The id field is intentionally dropped by toDto() — backup files
     * should never carry device-specific primary keys.
     */
    fun performExport(uri: Uri, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val allCycles = dao.getAllCyclesOnce()

                // FIX: Use toDto() so PeriodCycleEntity's Room annotations
                // (@Entity, @PrimaryKey) are never serialised into the file.
                val backup = BackupData(
                    version   = CURRENT_EXPORT_VERSION,
                    timestamp = System.currentTimeMillis(),
                    cycles    = allCycles.map { it.toDto() }
                )

                val jsonString = Gson().toJson(backup)
                appContext.contentResolver.openOutputStream(uri)?.use { os ->
                    OutputStreamWriter(os).use { it.write(jsonString) }
                }
                withContext(Dispatchers.Main) { onResult(true, "Export Successful") }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { onResult(false, e.localizedMessage) }
            }
        }
    }

    /**
     * IMPORT: Reads JSON, runs waterfall migration, then deduplicates before inserting.
     */
    fun performImport(uri: Uri, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // 1. Read file
                val sb = StringBuilder()
                appContext.contentResolver.openInputStream(uri)?.use { isr ->
                    BufferedReader(InputStreamReader(isr)).use { it.forEachLine { line -> sb.append(line) } }
                }

                // 2. Inspect version before full parse
                val jsonObject = JSONObject(sb.toString())
                val fileVersion = jsonObject.optInt("version", 1)

                // FIX: Reject files from a newer app version — silent data loss is worse
                // than a clear error message. Without this, a V3 file imported into a V2
                // app would succeed but silently discard all new fields.
                if (fileVersion > CURRENT_EXPORT_VERSION) {
                    withContext(Dispatchers.Main) {
                        onResult(false, "This backup is from a newer version of Periodt. Please update the app and try again.")
                    }
                    return@launch
                }

                // --- WATERFALL MIGRATION BRIDGE ---
                // Each 'if' block upgrades the raw JSON one version at a time.
                // A V1 file falls through every block until it reaches the current version.
                //
                // if (fileVersion < 2) {
                //     // painLevel was added in export V2 — back-fill with default
                //     val cycles = jsonObject.getJSONArray("cycles")
                //     for (i in 0 until cycles.length()) {
                //         val c = cycles.getJSONObject(i)
                //         if (!c.has("painLevel")) c.put("painLevel", 5)
                //     }
                // }
                //
                // if (fileVersion < 3) { /* next migration */ }
                // ---------------------------------

                // 3. Parse into DTO-backed data class
                val backup = Gson().fromJson(jsonObject.toString(), BackupData::class.java)
                val existingCycles = dao.getAllCyclesOnce()

                var importedCount = 0
                var skippedCount = 0
                backup.cycles.forEach { dto ->
                    val isDuplicate = existingCycles.any {
                        it.startDate == dto.startDate && it.endDate == dto.endDate
                    }
                    if (!isDuplicate) {
                        // FIX: toEntity() sets id = 0 so Room generates a fresh device-local ID
                        dao.insertCycle(dto.toEntity())
                        importedCount++
                    } else {
                        skippedCount++
                    }
                }

                withContext(Dispatchers.Main) {
                    val skipNote = if (skippedCount > 0) " ($skippedCount duplicates skipped)" else ""
                    if (importedCount > 0) {
                        onResult(true, "Imported $importedCount new entries$skipNote")
                        CalendarWidgetProvider.refreshAll(appContext)
                    } else {
                        onResult(false, "No new data found.$skipNote")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { onResult(false, "Invalid File: ${e.localizedMessage}") }
            }
        }
    }

    /**
     * FIX: Use dao.deleteAll() — a single DELETE query — instead of
     * fetching every row and calling deleteCycleById() N times.
     */
    fun clearAllData() {
        viewModelScope.launch(Dispatchers.IO) {
            dao.deleteAll()
            withContext(Dispatchers.Main) { CalendarWidgetProvider.refreshAll(appContext) }
        }
    }

    // --- MODELS ---

    data class Cycle(
        val id: Int,
        val startDate: LocalDate,
        val endDate: LocalDate?,
        val bleeding: String,
        val bloodColor: String,
        val painLevel: Int
    )

    class Factory(private val app: Application) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(PeriodViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return PeriodViewModel(app) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}

/**
 * =============================================================================
 * FUTURE-PROOFING GUIDANCE
 * =============================================================================
 *
 * ADDING A NEW FIELD (e.g. mood: String):
 *
 * 1. THE ENTITY: Add the column to PeriodCycleEntity.
 *    val mood: String = ""
 *
 * 2. THE DB MIGRATION: Add MIGRATION_2_3 in AppDatabase.kt.
 *    db.execSQL("ALTER TABLE period_cycles ADD COLUMN mood TEXT NOT NULL DEFAULT ''")
 *
 * 3. THE DTO: Add the field to BackupCycleDto as NON-NULLABLE.
 *    @SerializedName("mood") val mood: String = ""
 *
 *    ── WHY NON-NULLABLE? ──────────────────────────────────────────────────
 *    Do NOT use String? here. The manual bridge below guarantees the field
 *    exists in the JSON before Gson ever parses it, so the DTO never sees
 *    null. Keeping fields non-nullable means toEntity() stays dumb —
 *    no null-checks, no version awareness, just straight mapping.
 *    If you used String? instead, the null-handling leaks into toEntity()
 *    and from there into every place in your app that touches the field.
 *    ───────────────────────────────────────────────────────────────────────
 *
 * 4. THE MAPPING: Update toDto() and toEntity() in BackupData.kt.
 *
 * 5. THE EXPORT VERSION: Bump CURRENT_EXPORT_VERSION to 3.
 *
 * 6. THE IMPORT BRIDGE: Add a block in performImport():
 *    if (fileVersion < 3) {
 *        // Back-fill BEFORE Gson parses — this is what keeps the DTO non-nullable.
 *        // Old files have no "mood" field, so we inject the default here manually.
 *        val cycles = jsonObject.getJSONArray("cycles")
 *        for (i in 0 until cycles.length()) {
 *            val c = cycles.getJSONObject(i)
 *            if (!c.has("mood")) c.put("mood", "")
 *        }
 *    }
 *
 * WATERFALL RULE: Always use 'if', never 'else if'.
 * A V1 file must fall through every migration step to reach the current version.
 * This means the bridge — not Gson, not toEntity() — owns all default values.
 * =============================================================================
 */