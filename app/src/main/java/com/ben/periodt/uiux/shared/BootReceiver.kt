package com.ben.periodt.uiux.shared

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
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

                if (prefs[ReminderPrefs.IS_ENABLED] == true) {
                    ReminderScheduler.scheduleNextReminder(appCtx,
                        prefs[ReminderPrefs.TIME_HOUR] ?: 8,
                        prefs[ReminderPrefs.TIME_MINUTE] ?: 0)
                }
                if (prefs[ReminderPrefs.FERTILITY_ENABLED] == true) {
                    ReminderScheduler.scheduleNextFertilityReminder(appCtx,
                        prefs[ReminderPrefs.FERTILITY_HOUR] ?: 8,
                        prefs[ReminderPrefs.FERTILITY_MINUTE] ?: 0)
                }
                if (prefs[ReminderPrefs.PILL_ENABLED] == true) {
                    ReminderScheduler.scheduleNextPillReminder(appCtx,
                        prefs[ReminderPrefs.PILL_HOUR] ?: 8,
                        prefs[ReminderPrefs.PILL_MINUTE] ?: 0)
                }
            } catch (e: Throwable) {
                android.util.Log.e("BootReceiver", "Boot reschedule failed: ${e.message}", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}