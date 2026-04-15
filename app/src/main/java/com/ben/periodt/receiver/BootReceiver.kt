package com.ben.periodt.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.ben.periodt.data.AppDatabase
import com.ben.periodt.reminder.ReminderPrefs
import com.ben.periodt.reminder.ReminderScheduler
import com.ben.periodt.reminder.dataStore
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

                // Load database to fetch all profiles
                System.loadLibrary("sqlcipher")
                val dao = AppDatabase.getDatabase(appCtx).periodCycleDao()
                val profiles = dao.getAllProfilesOnce()

                // Loop through EVERY profile and restore its specific alarms
                for (profile in profiles) {
                    val profileId = profile.id

                    if (prefs[ReminderPrefs.periodEnabled(profileId)] == true) {
                        ReminderScheduler.scheduleNextReminder(
                            context = appCtx,
                            profileId = profileId,
                            hour = prefs[ReminderPrefs.periodHour(profileId)] ?: 8,
                            minute = prefs[ReminderPrefs.periodMinute(profileId)] ?: 0
                        )
                    }

                    if (prefs[ReminderPrefs.fertilityEnabled(profileId)] == true) {
                        ReminderScheduler.scheduleNextFertilityReminder(
                            context = appCtx,
                            profileId = profileId,
                            hour = prefs[ReminderPrefs.fertilityHour(profileId)] ?: 8,
                            minute = prefs[ReminderPrefs.fertilityMinute(profileId)] ?: 0
                        )
                    }

                    if (prefs[ReminderPrefs.pillEnabled(profileId)] == true) {
                        ReminderScheduler.scheduleNextPillReminder(
                            context = appCtx,
                            profileId = profileId,
                            hour = prefs[ReminderPrefs.pillHour(profileId)] ?: 8,
                            minute = prefs[ReminderPrefs.pillMinute(profileId)] ?: 0
                        )
                    }
                }

            } catch (e: Throwable) {
                Log.e("BootReceiver", "Boot reschedule failed: ${e.message}", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}