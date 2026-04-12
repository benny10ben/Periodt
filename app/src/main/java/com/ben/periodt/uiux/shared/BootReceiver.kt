package com.ben.periodt.uiux.shared

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.ben.periodt.viewmodel.ACTIVE_PROFILE_ID_KEY
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val validActions = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            "android.intent.action.LOCKED_BOOT_COMPLETED",
            Intent.ACTION_MY_PACKAGE_REPLACED
        )
        if (intent.action !in validActions) return

        // LOCKED_BOOT fires before the user unlocks — DataStore (credential-encrypted)
        // is inaccessible at that point and will throw. Skip it; BOOT_COMPLETED
        // fires again after unlock and will handle the reschedule.
        if (intent.action == "android.intent.action.LOCKED_BOOT_COMPLETED") return

        val appCtx = context.applicationContext
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val prefs = appCtx.dataStore.data.first()
                val activeProfileId = prefs[ACTIVE_PROFILE_ID_KEY] ?: 1

                if (prefs[ReminderPrefs.periodEnabled(activeProfileId)] == true) {
                    ReminderScheduler.scheduleNextReminder(
                        context = appCtx,
                        hour = prefs[ReminderPrefs.periodHour(activeProfileId)] ?: 8,
                        minute = prefs[ReminderPrefs.periodMinute(activeProfileId)] ?: 0
                    )
                }

                if (prefs[ReminderPrefs.fertilityEnabled(activeProfileId)] == true) {
                    ReminderScheduler.scheduleNextFertilityReminder(
                        context = appCtx,
                        hour = prefs[ReminderPrefs.fertilityHour(activeProfileId)] ?: 8,
                        minute = prefs[ReminderPrefs.fertilityMinute(activeProfileId)] ?: 0
                    )
                }

                if (prefs[ReminderPrefs.pillEnabled(activeProfileId)] == true) {
                    ReminderScheduler.scheduleNextPillReminder(
                        context = appCtx,
                        hour = prefs[ReminderPrefs.pillHour(activeProfileId)] ?: 8,
                        minute = prefs[ReminderPrefs.pillMinute(activeProfileId)] ?: 0
                    )
                }

            } catch (e: Throwable) {
                android.util.Log.e("BootReceiver", "Boot reschedule failed: ${e.message}", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}