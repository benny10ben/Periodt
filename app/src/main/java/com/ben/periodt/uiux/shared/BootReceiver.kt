// BootReceiver.kt
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
        if (
            intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == "android.intent.action.LOCKED_BOOT_COMPLETED"
        ) {
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
                    predictCycle(cycles) // compute latest; ignore result here [web:6]
                    ReminderScheduler.scheduleAllDailyChecks(appCtx) // both at ~8:30 AM [web:6]
                }
            }
        }
    }
}
