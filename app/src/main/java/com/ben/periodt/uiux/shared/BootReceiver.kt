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

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val validActions = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            "android.intent.action.LOCKED_BOOT_COMPLETED",
            Intent.ACTION_MY_PACKAGE_REPLACED
        )
        if (intent.action !in validActions) return

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

                    if (!isOnPill && !isTransitioning) {
                        val validCycles = if (stopDate != null)
                            cycles.filter { !it.startDate.isBefore(stopDate) }
                        else
                            cycles
                        predictCycle(validCycles)
                    }

                }.onFailure {
                    // DB may not be ready on very early boots — alarms still
                    // reschedule below so the checker retries tomorrow
                }
            } finally {
                ReminderScheduler.scheduleAllDailyChecks(appCtx)
                pendingResult.finish()
            }
        }
    }
}