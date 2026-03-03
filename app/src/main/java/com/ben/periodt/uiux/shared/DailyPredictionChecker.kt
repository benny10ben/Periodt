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
        val appCtx        = context.applicationContext
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                runCatching {
                    val dao = AppDatabase.getDatabase(appCtx).periodCycleDao()

                    val entities = dao.getAllCyclesOnce()
                    val cycles   = entities.map { e ->
                        PeriodViewModel.Cycle(
                            id         = e.id,
                            startDate  = LocalDate.parse(e.startDate),
                            endDate    = e.endDate.takeIf { it.isNotBlank() }
                                ?.let { LocalDate.parse(it) },
                            bleeding   = e.bleeding,
                            bloodColor = e.bloodColor,
                            painLevel  = e.painLevel
                        )
                    }

                    val pillPacks   = dao.getAllPillPacksOnce()
                    val isOnPill    = pillPacks.any { it.endDate == null }
                    val rawStopDate = pillPacks
                        .filter  { it.endDate != null }
                        .maxByOrNull { it.endDate!! }
                        ?.endDate
                    val stopDate    = rawStopDate?.let { LocalDate.parse(it) }

                    val postPillCycles  = if (stopDate != null)
                        cycles.filter { !it.startDate.isBefore(stopDate) }
                    else
                        cycles

                    val isTransitioning = !isOnPill && stopDate != null &&
                            isStillTransitioning(postPillCycles)

                    if (isOnPill || isTransitioning) return@runCatching

                    val validCycles = if (stopDate != null)
                        cycles.filter { !it.startDate.isBefore(stopDate) }
                    else
                        cycles

                    val prediction = predictCycle(validCycles) ?: return@runCatching

                    val target    = prediction.mostLikelyPeriodStart
                    val today     = LocalDate.now()
                    val daysLeft  = ChronoUnit.DAYS.between(today, target).toInt()
                    val dateLabel = "${target.monthValue}/${target.dayOfMonth}"

                    if (daysLeft == 5 && shouldNotify(appCtx, target, 5)) {
                        ReminderScheduler.fireNow(appCtx, daysBefore = 5, targetDateText = dateLabel)
                        markNotified(appCtx, target, 5)
                    }
                    if (daysLeft == 2 && shouldNotify(appCtx, target, 2)) {
                        ReminderScheduler.fireNow(appCtx, daysBefore = 2, targetDateText = dateLabel)
                        markNotified(appCtx, target, 2)
                    }

                }.onFailure {
                    // Swallow silently — alarm will retry tomorrow
                }
            } finally {
                ReminderScheduler.scheduleAllDailyChecks(appCtx)
                pendingResult.finish()
            }
        }
    }

    private fun prefs(ctx: Context) =
        ctx.getSharedPreferences("reminder_prefs", Context.MODE_PRIVATE)

    private fun keyFor(date: LocalDate, days: Int): String {
        val m = date.monthValue.toString().padStart(2, '0')
        val d = date.dayOfMonth.toString().padStart(2, '0')
        return "reminder_${days}d_for_${date.year}$m$d"
    }

    private fun shouldNotify(ctx: Context, date: LocalDate, days: Int): Boolean =
        !prefs(ctx).getBoolean(keyFor(date, days), false)

    private fun markNotified(ctx: Context, date: LocalDate, days: Int) =
        prefs(ctx).edit().putBoolean(keyFor(date, days), true).apply()
}