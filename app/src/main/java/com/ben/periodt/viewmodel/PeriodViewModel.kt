package com.ben.periodt.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ben.periodt.data.AppDatabase
import com.ben.periodt.data.BackupData
import com.ben.periodt.data.PeriodCycleEntity
import com.ben.periodt.uiux.shared.Prediction
import com.ben.periodt.uiux.shared.ReminderScheduler
import com.ben.periodt.uiux.shared.predictCycle
// ✅ Import your Widget Provider
import com.ben.periodt.widget.CalendarWidgetProvider
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.time.LocalDate

class PeriodViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = AppDatabase.getDatabase(application).periodCycleDao()
    private val appContext = application.applicationContext

    // ... (StateFlows remain same) ...
    val cycles: StateFlow<List<Cycle>> = dao.getAllCycles()
        .map { list ->
            list.map { entity ->
                Cycle(
                    id = entity.id,
                    startDate = LocalDate.parse(entity.startDate),
                    endDate = entity.endDate.takeIf { it.isNotBlank() }?.let { LocalDate.parse(it) },
                    bleeding = entity.bleeding,
                    bloodColor = entity.bloodColor,
                    painLevel = entity.painLevel
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val prediction: StateFlow<Prediction?> = cycles
        .map { predictCycle(it) }
        .stateIn(viewModelScope, SharingStarted.Lazily, null)

    init {
        viewModelScope.launch {
            prediction.filterNotNull().collect { newPred ->
                handleScheduling(newPred)
            }
        }
    }

    // ✅ ADD REFRESH HERE
    fun addCycle(start: LocalDate, end: LocalDate?, bleeding: String, bloodColor: String, painLevel: Int) {
        val entity = PeriodCycleEntity(
            startDate = start.toString(),
            endDate = end?.toString() ?: "",
            bleeding = bleeding,
            bloodColor = bloodColor,
            painLevel = painLevel
        )
        viewModelScope.launch {
            dao.insertCycle(entity)
            CalendarWidgetProvider.refreshAll(appContext) // <--- ADDED
        }
    }

    // ✅ ADD REFRESH HERE
    fun updateCycle(cycle: Cycle) {
        val entity = PeriodCycleEntity(
            id = cycle.id,
            startDate = cycle.startDate.toString(),
            endDate = cycle.endDate?.toString() ?: "",
            bleeding = cycle.bleeding,
            bloodColor = cycle.bloodColor,
            painLevel = cycle.painLevel
        )
        viewModelScope.launch {
            dao.updateCycle(entity)
            CalendarWidgetProvider.refreshAll(appContext) // <--- ADDED
        }
    }

    // ✅ ADD REFRESH HERE
    fun deleteCycle(id: Int) = viewModelScope.launch {
        dao.deleteCycleById(id)
        CalendarWidgetProvider.refreshAll(appContext) // <--- ADDED
    }

    fun performExport(uri: Uri, onResult: (Boolean, String?) -> Unit) {
        // ... (Export logic remains same) ...
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val allCycles = dao.getAllCyclesOnce()
                val backup = BackupData(cycles = allCycles)
                val jsonString = Gson().toJson(backup)

                appContext.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    OutputStreamWriter(outputStream).use { writer ->
                        writer.write(jsonString)
                    }
                }
                withContext(Dispatchers.Main) { onResult(true, "Export Successful") }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { onResult(false, e.localizedMessage) }
            }
        }
    }

    // ✅ ADD REFRESH HERE (Inside Import)
    fun performImport(uri: Uri, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // ... (Import logic reading file) ...
                val sb = StringBuilder()
                appContext.contentResolver.openInputStream(uri)?.use { inputStream ->
                    BufferedReader(InputStreamReader(inputStream)).use { reader ->
                        var line: String? = reader.readLine()
                        while (line != null) {
                            sb.append(line)
                            line = reader.readLine()
                        }
                    }
                }

                val backup = Gson().fromJson(sb.toString(), BackupData::class.java)
                val existingCycles = dao.getAllCyclesOnce()

                var importedCount = 0

                // ... (Insert logic) ...
                backup.cycles.forEach { incoming ->
                    val isDuplicate = existingCycles.any { existing ->
                        existing.startDate == incoming.startDate && existing.endDate == incoming.endDate
                    }
                    if (!isDuplicate) {
                        dao.insertCycle(incoming.copy(id = 0))
                        importedCount++
                    }
                }

                withContext(Dispatchers.Main) {
                    if (importedCount > 0) {
                        onResult(true, "Imported $importedCount new entries")
                        CalendarWidgetProvider.refreshAll(appContext) // <--- ADDED
                    } else {
                        onResult(false, "No new data found.")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { onResult(false, "Invalid File: ${e.localizedMessage}") }
            }
        }
    }

    // ✅ ADD REFRESH HERE
    fun clearAllData() {
        viewModelScope.launch(Dispatchers.IO) {
            val all = dao.getAllCyclesOnce()
            all.forEach { dao.deleteCycleById(it.id) }

            withContext(Dispatchers.Main) {
                CalendarWidgetProvider.refreshAll(appContext) // <--- ADDED
            }
        }
    }

    // ... (Rest of file remains same) ...
    private fun ymd(date: LocalDate): Int = date.year * 10000 + date.monthValue * 100 + date.dayOfMonth
    private fun prefs() = appContext.getSharedPreferences("reminder_prefs", Context.MODE_PRIVATE)
    // ...
    private fun handleScheduling(newPred: Prediction) { /*...*/ }

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