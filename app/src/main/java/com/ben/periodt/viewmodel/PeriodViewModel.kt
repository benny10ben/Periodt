package com.ben.periodt.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ben.periodt.data.*
import com.ben.periodt.uiux.shared.PostPillState
import com.ben.periodt.uiux.shared.Prediction
import com.ben.periodt.uiux.shared.getPostPillState
import com.ben.periodt.uiux.shared.isStillTransitioning
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

    private val dao        = AppDatabase.getDatabase(application).periodCycleDao()
    private val appContext = application.applicationContext

    // Data streams

    val cycles: StateFlow<List<Cycle>> = dao.getAllCycles()
        .map { list -> list.map { it.toDomain() } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val pillPacks: StateFlow<List<PillPack>> = dao.getAllPillPacks()
        .map { list ->
            list.map { entity ->
                PillPack(
                    id        = entity.id,
                    startDate = LocalDate.parse(entity.startDate),
                    pillCount = entity.pillCount,
                    endDate   = entity.endDate?.takeIf { it.isNotBlank() }?.let { LocalDate.parse(it) }
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // Derived reactive states

    val isOnPill: StateFlow<Boolean> = pillPacks.map { packs ->
        packs.any { it.endDate == null }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val pillPackStartDate: StateFlow<LocalDate?> = pillPacks.map { packs ->
        packs.firstOrNull { it.endDate == null }?.startDate
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val pillPackCount: StateFlow<Int> = pillPacks.map { packs ->
        packs.firstOrNull { it.endDate == null }?.pillCount ?: 21
    }.stateIn(viewModelScope, SharingStarted.Eagerly, 21)

    val pillStopDate: StateFlow<LocalDate?> = pillPacks.map { packs ->
        packs.filter { it.endDate != null }.maxByOrNull { it.endDate!! }?.endDate
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    // The full three-state post-pill status. Use this in UI components
    // that need to distinguish between discovery, learning, and normal.
    val postPillState: StateFlow<PostPillState> = combine(cycles, isOnPill, pillStopDate) { cycleList, onPill, stopDate ->
        if (onPill || stopDate == null) PostPillState.NORMAL
        else {
            val postPillCycles = cycleList.filter { !it.startDate.isBefore(stopDate) }
            getPostPillState(postPillCycles)
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, PostPillState.NORMAL)

    // Kept for backward compatibility with CalendarScreen, DayCellEnhanced,
    // PredictionBanner, and the broadcast receivers. True only in DISCOVERY.
    val isTransitioning: StateFlow<Boolean> = postPillState.map { state ->
        state == PostPillState.DISCOVERY
    }.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val prediction: StateFlow<Prediction?> = combine(
        cycles, isOnPill, isTransitioning, pillStopDate, pillPackStartDate, pillPackCount
    ) { args ->
        val cycleList     = args[0] as List<Cycle>
        val onPill        = args[1] as Boolean
        val transitioning = args[2] as Boolean
        val stopDate      = args[3] as LocalDate?
        val activeStart   = args[4] as LocalDate?
        val activeCount   = args[5] as Int

        when {
            onPill && activeStart != null -> {
                val predictedDate = activeStart.plusDays(activeCount.toLong())
                Prediction(
                    minPeriodStart        = predictedDate,
                    maxPeriodStart        = predictedDate.plusDays(2),
                    mostLikelyPeriodStart = predictedDate,
                    periodLength          = 5,
                    ovulationDay          = predictedDate,
                    ovulationConfidence   = 0.0f,
                    fertileWindow         = LocalDate.MIN..LocalDate.MIN,
                    cycleLength           = activeCount + 7,
                    cycleRegularity       = com.ben.periodt.uiux.shared.CycleRegularity.VERY_REGULAR
                )
            }
            // DISCOVERY — no prediction
            transitioning -> null
            // LEARNING and NORMAL — both produce predictions
            // LEARNING will naturally have lower confidence and wider windows
            // because there are fewer cycles for the algorithm to work with
            else -> {
                val validCycles = if (stopDate != null)
                    cycleList.filter { !it.startDate.isBefore(stopDate) }
                else cycleList
                predictCycle(validCycles)
            }
        }
    }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.Lazily, null)

    init {
        checkAndAutoStopPillPack()
    }

    // Pill logic

    fun refreshState() = checkAndAutoStopPillPack()

    private fun checkAndAutoStopPillPack() {
        viewModelScope.launch {
            val activePack  = pillPacks.value.firstOrNull { it.endDate == null } ?: return@launch
            val packEndDate = activePack.startDate.plusDays((activePack.pillCount - 1).toLong())
            if (LocalDate.now().isAfter(packEndDate)) {
                dao.updatePillPackEndDate(activePack.id, packEndDate.toString())
                CalendarWidgetProvider.refreshAll(appContext)
            }
        }
    }

    fun enablePillTracking(startDate: LocalDate, count: Int) {
        viewModelScope.launch {
            val packEndDate = startDate.plusDays((count - 1).toLong())
            val isPast      = LocalDate.now().isAfter(packEndDate)
            dao.insertPillPack(
                PillPackEntity(
                    startDate = startDate.toString(),
                    pillCount = count,
                    endDate   = if (isPast) packEndDate.toString() else null
                )
            )
            CalendarWidgetProvider.refreshAll(appContext)
        }
    }

    fun stopPillTracking() {
        viewModelScope.launch {
            val activePack  = pillPacks.value.firstOrNull { it.endDate == null } ?: return@launch
            val today       = LocalDate.now()
            val packEndDate = activePack.startDate.plusDays((activePack.pillCount - 1).toLong())
            val finalStop   = if (today.isAfter(packEndDate)) packEndDate else today
            dao.updatePillPackEndDate(activePack.id, finalStop.toString())
            CalendarWidgetProvider.refreshAll(appContext)
        }
    }

    fun deletePillPack(id: Int) {
        viewModelScope.launch {
            dao.deletePillPackById(id)
            CalendarWidgetProvider.refreshAll(appContext)
        }
    }

    // CRUD

    fun addCycle(start: LocalDate, end: LocalDate?, bleeding: String, bloodColor: String, painLevel: Int) {
        viewModelScope.launch {
            dao.insertCycle(
                PeriodCycleEntity(
                    startDate  = start.toString(),
                    endDate    = end?.toString() ?: "",
                    bleeding   = bleeding,
                    bloodColor = bloodColor,
                    painLevel  = painLevel
                )
            )
            CalendarWidgetProvider.refreshAll(appContext)
        }
    }

    fun updateCycle(cycle: Cycle) {
        viewModelScope.launch {
            dao.updateCycle(
                PeriodCycleEntity(
                    id         = cycle.id,
                    startDate  = cycle.startDate.toString(),
                    endDate    = cycle.endDate?.toString() ?: "",
                    bleeding   = cycle.bleeding,
                    bloodColor = cycle.bloodColor,
                    painLevel  = cycle.painLevel
                )
            )
            CalendarWidgetProvider.refreshAll(appContext)
        }
    }

    fun deleteCycle(id: Int) = viewModelScope.launch {
        dao.deleteCycleById(id)
        CalendarWidgetProvider.refreshAll(appContext)
    }

    // Export / Import

    fun performExport(uri: Uri, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val backup = BackupData(
                    version     = BackupData.CURRENT_VERSION,
                    timestamp   = System.currentTimeMillis(),
                    cycles      = dao.getAllCyclesOnce().map { it.toDto() },
                    pillHistory = dao.getAllPillPacksOnce().map { it.toDto() }
                )
                val json = Gson().toJson(backup)
                appContext.contentResolver.openOutputStream(uri)?.use { os ->
                    OutputStreamWriter(os).use { it.write(json) }
                }
                withContext(Dispatchers.Main) { onResult(true, "Export successful") }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { onResult(false, e.localizedMessage) }
            }
        }
    }

    fun performImport(uri: Uri, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val raw = buildString {
                    appContext.contentResolver.openInputStream(uri)?.use { stream ->
                        BufferedReader(InputStreamReader(stream)).forEachLine { append(it) }
                    }
                }
                val jsonObject  = JSONObject(raw)
                val fileVersion = jsonObject.optInt("version", 1)

                if (fileVersion > BackupData.CURRENT_VERSION) {
                    withContext(Dispatchers.Main) { onResult(false, "Update the app to import this file.") }
                    return@launch
                }

                if (fileVersion < 3 && jsonObject.has("pillState")) {
                    val v2    = jsonObject.getJSONObject("pillState")
                    val start = v2.optString("packStartDate", "")
                    if (start.isNotBlank()) {
                        val alreadyExists = dao.getAllPillPacksOnce().any { it.startDate == start }
                        if (!alreadyExists) {
                            dao.insertPillPack(
                                PillPackEntity(
                                    startDate = start,
                                    pillCount = v2.optInt("pillCount", 21),
                                    endDate   = v2.optString("pillStopDate", null)
                                        ?.takeIf { it.isNotBlank() }
                                )
                            )
                        }
                    }
                }

                val backup = Gson().fromJson(raw, BackupData::class.java)

                val existingPacks = dao.getAllPillPacksOnce()
                backup.pillHistory?.forEach { dto ->
                    if (existingPacks.none { it.startDate == dto.startDate }) {
                        dao.insertPillPack(dto.toEntity())
                    }
                }

                val existingCycles = dao.getAllCyclesOnce()
                backup.cycles.forEach { dto ->
                    if (existingCycles.none { it.startDate == dto.startDate }) {
                        dao.insertCycle(dto.toEntity())
                    }
                }

                val activePack = dao.getAllPillPacksOnce().firstOrNull { it.endDate == null }
                if (activePack != null) {
                    val packEndDate = LocalDate.parse(activePack.startDate)
                        .plusDays((activePack.pillCount - 1).toLong())
                    if (LocalDate.now().isAfter(packEndDate)) {
                        dao.updatePillPackEndDate(activePack.id, packEndDate.toString())
                    }
                }

                withContext(Dispatchers.Main) {
                    onResult(true, "Import successful")
                    CalendarWidgetProvider.refreshAll(appContext)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { onResult(false, "Error: ${e.localizedMessage}") }
            }
        }
    }

    fun clearAllData() {
        viewModelScope.launch(Dispatchers.IO) {
            dao.deleteAll()
            appContext
                .getSharedPreferences("reminder_prefs", Context.MODE_PRIVATE)
                .edit().clear().apply()
            withContext(Dispatchers.Main) {
                CalendarWidgetProvider.refreshAll(appContext)
            }
        }
    }

    // Domain models

    data class Cycle(
        val id: Int,
        val startDate: LocalDate,
        val endDate: LocalDate?,
        val bleeding: String,
        val bloodColor: String,
        val painLevel: Int
    )

    data class PillPack(
        val id: Int,
        val startDate: LocalDate,
        val pillCount: Int,
        val endDate: LocalDate?
    )

    class Factory(private val app: Application) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return PeriodViewModel(app) as T
        }
    }
}

private fun PeriodCycleEntity.toDomain() = PeriodViewModel.Cycle(
    id         = id,
    startDate  = LocalDate.parse(startDate),
    endDate    = endDate.takeIf { it.isNotBlank() }?.let { LocalDate.parse(it) },
    bleeding   = bleeding,
    bloodColor = bloodColor,
    painLevel  = painLevel
)