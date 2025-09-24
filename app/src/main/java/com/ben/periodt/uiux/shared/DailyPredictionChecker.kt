// DailyPredictionChecker.kt
package com.ben.periodt.uiux.shared

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.ben.periodt.data.AppDatabase
import com.ben.periodt.viewmodel.PeriodViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.temporal.ChronoUnit

class DailyPredictionChecker : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val appCtx = context.applicationContext
        CoroutineScope(Dispatchers.IO).launch {
            runCatching {
                val dao = AppDatabase.getDatabase(appCtx).periodCycleDao()
                val entities = dao.getAllCyclesOnce()
                val cycles = entities.map { e ->
                    PeriodViewModel.Cycle(
                        id = e.id,
                        startDate = LocalDate.parse(e.startDate),
                        endDate = e.endDate.takeIf { it.isNotBlank() }?.let { LocalDate.parse(it) },
                        bleeding = e.bleeding,
                        bloodColor = e.bloodColor,
                        painLevel = e.painLevel
                    )
                }
                val prediction = predictCycle(cycles)
                if (prediction != null) {
                    val target = prediction.mostLikelyPeriodStart
                    val daysLeft = ChronoUnit.DAYS.between(LocalDate.now(), target).toInt()
                    val dateLabel = "${target.monthValue}/${target.dayOfMonth}"

                    // DailyPredictionChecker.kt (core logic unchanged; ensure both checks run)
                    if (daysLeft == 5 && shouldNotify(appCtx, target, 5)) {
                        ReminderScheduler.fireNow(appCtx, daysBefore = 5, targetDateText = dateLabel)
                        markNotified(appCtx, target, 5)
                    }
                    if (daysLeft == 2 && shouldNotify(appCtx, target, 2)) {
                        ReminderScheduler.fireNow(appCtx, daysBefore = 2, targetDateText = dateLabel)
                        markNotified(appCtx, target, 2)
                    }
                }
            }.onFailure {
                // ignore; keep rescheduling
            }.also {
                // Always reschedule both windows for tomorrow
                ReminderScheduler.scheduleAllDailyChecks(appCtx) // [web:6]
            }
        }
    }

    private fun prefs(ctx: Context) = ctx.getSharedPreferences("reminder_prefs", Context.MODE_PRIVATE)
    private fun keyFor(date: LocalDate, days: Int) =
        "reminder_${days}d_for_${date.year}${date.monthValue.toString().padStart(2, '0')}${date.dayOfMonth.toString().padStart(2, '0')}"

    private fun shouldNotify(ctx: Context, date: LocalDate, days: Int): Boolean =
        !prefs(ctx).getBoolean(keyFor(date, days), false) // [web:6]

    private fun markNotified(ctx: Context, date: LocalDate, days: Int) {
        prefs(ctx).edit().putBoolean(keyFor(date, days), true).apply() // [web:6]
    }
}
