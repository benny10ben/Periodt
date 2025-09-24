package com.ben.periodt.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ben.periodt.data.AppDatabase
import com.ben.periodt.data.PeriodCycleEntity
import com.ben.periodt.uiux.shared.Prediction
import com.ben.periodt.uiux.shared.ReminderScheduler
import com.ben.periodt.uiux.shared.predictCycle
import com.ben.periodt.widget.MonthlyWidgetProvider
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate

class PeriodViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = AppDatabase.getDatabase(application).periodCycleDao()
    private val appContext = application.applicationContext

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
        // Observe prediction and ensure the daily checker is active whenever prediction is available or changes.
        viewModelScope.launch {
            prediction.filterNotNull().collect { newPred ->
                handleScheduling(newPred)
            }
        }
    }

    fun addCycle(
        start: LocalDate,
        end: LocalDate?,
        bleeding: String,
        bloodColor: String,
        painLevel: Int
    ) {
        val entity = PeriodCycleEntity(
            startDate = start.toString(),
            endDate = end?.toString() ?: "",
            bleeding = bleeding,
            bloodColor = bloodColor,
            painLevel = painLevel
        )
        viewModelScope.launch {
            dao.insertCycle(entity)
            // Refresh widgets after database change
            MonthlyWidgetProvider.refreshAll(appContext)
        }
    }

    fun deleteCycle(id: Int) = viewModelScope.launch {
        dao.deleteCycleById(id)
        // Refresh widgets after database change
        MonthlyWidgetProvider.refreshAll(appContext)
    }

    private fun ymd(date: LocalDate): Int = date.year * 10000 + date.monthValue * 100 + date.dayOfMonth

    private fun prefs() = appContext.getSharedPreferences("reminder_prefs", Context.MODE_PRIVATE)
    private fun readLastTarget(): Int? {
        val v = prefs().getInt("last_target_yyyymmdd", 0)
        return if (v == 0) null else v
    }
    private fun writeLastTarget(value: Int) {
        prefs().edit().putInt("last_target_yyyymmdd", value).apply()
    }
    private fun clearTwoDayDeliveredForAll() {
        // Safest: clear all two_day_notif_for_* keys
        val all = prefs().all
        val editor = prefs().edit()
        all.keys.filter { it.startsWith("two_day_notif_for_") }.forEach { editor.remove(it) }
        editor.apply()
    }

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

    // PeriodViewModel.kt (extend your clear function)
    private fun clearDeliveredFlags() {
        val all = prefs().all
        val editor = prefs().edit()
        all.keys.filter { it.startsWith("reminder_2d_for_") || it.startsWith("reminder_5d_for_") }
            .forEach { editor.remove(it) }
        editor.apply()
    }

    private fun handleScheduling(newPred: Prediction) {
        val last = readLastTarget()
        val newYmd = ymd(newPred.mostLikelyPeriodStart)
        if (last == null || last != newYmd) {
            clearDeliveredFlags()
            writeLastTarget(newYmd)
        }
        // Schedule both daily windows
        ReminderScheduler.scheduleAllDailyChecks(appContext) // [web:6]
    }
}