package com.ben.periodt.reminder

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.ben.periodt.MainActivity
import com.ben.periodt.R
import com.ben.periodt.data.AppDatabase
import com.ben.periodt.ui.shared.isStillTransitioning
import com.ben.periodt.ui.shared.predictCycle
import com.ben.periodt.viewmodel.PeriodViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

// ── 1. DATASTORE ──────────────────────────────────────────────────────────────

val Context.dataStore by preferencesDataStore(name = "reminder_prefs")

object ReminderPrefs {

    // ── Per-profile key factories ─────────────────────────────────────────────
    // Keys are prefixed with "p{profileId}_" so every profile has its own
    // independent reminder settings in the same DataStore file.

    fun periodEnabled(id: Int)       = booleanPreferencesKey("p${id}_is_enabled")
    fun periodDaysBefore(id: Int)    = intPreferencesKey("p${id}_days_before")
    fun periodHour(id: Int)          = intPreferencesKey("p${id}_time_hour")
    fun periodMinute(id: Int)        = intPreferencesKey("p${id}_time_minute")

    fun fertilityEnabled(id: Int)    = booleanPreferencesKey("p${id}_fertility_enabled")
    fun fertilityDaysBefore(id: Int) = intPreferencesKey("p${id}_fertility_days_before")
    fun fertilityHour(id: Int)       = intPreferencesKey("p${id}_fertility_time_hour")
    fun fertilityMinute(id: Int)     = intPreferencesKey("p${id}_fertility_time_minute")

    fun pillEnabled(id: Int)         = booleanPreferencesKey("p${id}_pill_enabled")
    fun pillHour(id: Int)            = intPreferencesKey("p${id}_pill_hour")
    fun pillMinute(id: Int)          = intPreferencesKey("p${id}_pill_minute")

    // ── Legacy global keys (U0) ───────────────────────────────────────────────
    // Never written by U1+. Only read during migration in SettingsScreen so
    // existing users don't lose their reminder settings on first upgrade.

    val IS_ENABLED            = booleanPreferencesKey("is_enabled")
    val DAYS_BEFORE           = intPreferencesKey("days_before")
    val TIME_HOUR             = intPreferencesKey("time_hour")
    val TIME_MINUTE           = intPreferencesKey("time_minute")
    val FERTILITY_ENABLED     = booleanPreferencesKey("fertility_enabled")
    val FERTILITY_DAYS_BEFORE = intPreferencesKey("fertility_days_before")
    val FERTILITY_HOUR        = intPreferencesKey("fertility_time_hour")
    val FERTILITY_MINUTE      = intPreferencesKey("fertility_time_minute")
    val PILL_ENABLED          = booleanPreferencesKey("pill_reminder_enabled")
    val PILL_HOUR             = intPreferencesKey("pill_reminder_hour")
    val PILL_MINUTE           = intPreferencesKey("pill_reminder_minute")
}

// ── 2. SCHEDULER ──────────────────────────────────────────────────────────────
//
// Request codes are derived from the profile ID so each profile gets its own
// independent alarm slot. Without this, all profiles share a single alarm and
// only the currently-active profile ever receives notifications.
//
//   Period    alarms: 1000 + profileId
//   Fertility alarms: 2000 + profileId
//   Pill      alarms: 3000 + profileId

private const val EXTRA_PROFILE_ID = "profile_id"

object ReminderScheduler {

    fun scheduleNextReminder(context: Context, profileId: Int, hour: Int, minute: Int) =
        schedule(context, hour, minute, 1000 + profileId, profileId, ModernReminderReceiver::class.java)

    fun cancelReminder(context: Context, profileId: Int) =
        cancel(context, 1000 + profileId, profileId, ModernReminderReceiver::class.java)

    fun scheduleNextFertilityReminder(context: Context, profileId: Int, hour: Int, minute: Int) =
        schedule(context, hour, minute, 2000 + profileId, profileId, FertilityReminderReceiver::class.java)

    fun cancelFertilityReminder(context: Context, profileId: Int) =
        cancel(context, 2000 + profileId, profileId, FertilityReminderReceiver::class.java)

    fun scheduleNextPillReminder(context: Context, profileId: Int, hour: Int, minute: Int) =
        schedule(context, hour, minute, 3000 + profileId, profileId, PillReminderReceiver::class.java)

    fun cancelPillReminder(context: Context, profileId: Int) =
        cancel(context, 3000 + profileId, profileId, PillReminderReceiver::class.java)

    private fun <T : BroadcastReceiver> schedule(
        context: Context, hour: Int, minute: Int,
        requestCode: Int, profileId: Int, receiverClass: Class<T>
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pending      = pendingFor(context, requestCode, profileId, receiverClass)

        val now    = LocalDateTime.now()
        var target = now.withHour(hour).withMinute(minute).withSecond(0).withNano(0)
        if (!now.isBefore(target)) target = target.plusDays(1)

        val triggerMs = target.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms())
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMs, pending)
            else
                alarmManager.setWindow(AlarmManager.RTC_WAKEUP, triggerMs, 10 * 60 * 1000L, pending)
        } else {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMs, pending)
        }
    }

    private fun <T : BroadcastReceiver> cancel(
        context: Context, requestCode: Int, profileId: Int, receiverClass: Class<T>
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(pendingFor(context, requestCode, profileId, receiverClass))
    }

    private fun <T : BroadcastReceiver> pendingFor(
        context: Context, requestCode: Int, profileId: Int, receiverClass: Class<T>
    ): PendingIntent = PendingIntent.getBroadcast(
        context, requestCode,
        Intent(context, receiverClass).putExtra(EXTRA_PROFILE_ID, profileId),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
}

// ── 3. PERIOD RECEIVER ────────────────────────────────────────────────────────

class ModernReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        val appCtx        = context.applicationContext
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Read profileId from the Intent — not from ACTIVE_PROFILE_ID_KEY.
                // Using the active profile key meant only the currently-selected
                // profile ever fired, breaking all other profiles' reminders.
                val profileId = intent.getIntExtra(EXTRA_PROFILE_ID, -1)
                if (profileId == -1) return@launch

                val prefs      = appCtx.dataStore.data.first()
                val isEnabled  = prefs[ReminderPrefs.periodEnabled(profileId)] ?: false
                val daysBefore = prefs[ReminderPrefs.periodDaysBefore(profileId)] ?: 2
                val hour       = prefs[ReminderPrefs.periodHour(profileId)] ?: 8
                val minute     = prefs[ReminderPrefs.periodMinute(profileId)] ?: 0

                if (!isEnabled) return@launch
                // Reschedule for tomorrow using the same profile's alarm slot
                ReminderScheduler.scheduleNextReminder(appCtx, profileId, hour, minute)

                System.loadLibrary("sqlcipher")
                val dao = AppDatabase.getDatabase(appCtx).periodCycleDao()

                // Fetch profile name for personalised notification text
                val profileName = dao.getProfileById(profileId)?.name
                val namePrefix  = profileName?.let { possessive(it) } ?: "Your"

                val cycles = dao.getCyclesForProfileOnce(profileId).map { e ->
                    PeriodViewModel.Cycle(
                        id         = e.id,
                        startDate  = LocalDate.parse(e.startDate),
                        endDate    = e.endDate.takeIf { it.isNotBlank() }?.let { LocalDate.parse(it) },
                        bleeding   = e.bleeding,
                        bloodColor = e.bloodColor,
                        painLevel  = e.painLevel
                    )
                }
                val pillPacks = dao.getPillPacksForProfileOnce(profileId).map { e ->
                    PeriodViewModel.PillPack(
                        id        = e.id,
                        startDate = LocalDate.parse(e.startDate),
                        pillCount = e.pillCount,
                        endDate   = e.endDate?.takeIf { it.isNotBlank() }?.let { LocalDate.parse(it) }
                    )
                }

                val activePack   = pillPacks.firstOrNull { it.endDate == null }
                val pillStopDate = pillPacks.filter { it.endDate != null }
                    .maxByOrNull { it.endDate!! }?.endDate

                var predictedStart: LocalDate? = null

                if (activePack != null) {
                    predictedStart = activePack.startDate.plusDays((activePack.pillCount + 2).toLong())
                } else {
                    val validCycles = if (pillStopDate != null)
                        cycles.filter { !it.startDate.isBefore(pillStopDate) } else cycles
                    if (pillStopDate == null || !isStillTransitioning(validCycles)) {
                        predictedStart = predictCycle(validCycles)?.mostLikelyPeriodStart
                    }
                }

                if (predictedStart != null) {
                    val daysUntil = ChronoUnit.DAYS.between(LocalDate.now(), predictedStart).toInt()
                    if (daysUntil == daysBefore) {
                        fireNotification(
                            context     = appCtx,
                            channelId   = "period_reminders_v2",
                            channelName = "Period Reminders",
                            title       = if (daysBefore == 1) "Period starting tomorrow"
                            else "Period in $daysBefore days",
                            text        = "$namePrefix cycle is predicted to start around ${
                                predictedStart.format(DateTimeFormatter.ofPattern("MMM d"))
                            }."
                        )
                    }
                }
            } catch (e: Throwable) {
                Log.e("PeriodReceiver", "Crash: ${e.message}", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}

// ── 4. FERTILITY RECEIVER ─────────────────────────────────────────────────────

class FertilityReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        val appCtx        = context.applicationContext
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val profileId = intent.getIntExtra(EXTRA_PROFILE_ID, -1)
                if (profileId == -1) return@launch

                val prefs      = appCtx.dataStore.data.first()
                val isEnabled  = prefs[ReminderPrefs.fertilityEnabled(profileId)] ?: false
                val daysBefore = prefs[ReminderPrefs.fertilityDaysBefore(profileId)] ?: 2
                val hour       = prefs[ReminderPrefs.fertilityHour(profileId)] ?: 8
                val minute     = prefs[ReminderPrefs.fertilityMinute(profileId)] ?: 0

                if (!isEnabled) return@launch
                ReminderScheduler.scheduleNextFertilityReminder(appCtx, profileId, hour, minute)

                System.loadLibrary("sqlcipher")
                val dao = AppDatabase.getDatabase(appCtx).periodCycleDao()

                val profileName = dao.getProfileById(profileId)?.name
                val namePrefix  = profileName?.let { possessive(it) } ?: "Your"

                val cycles = dao.getCyclesForProfileOnce(profileId).map { e ->
                    PeriodViewModel.Cycle(
                        id         = e.id,
                        startDate  = LocalDate.parse(e.startDate),
                        endDate    = e.endDate.takeIf { it.isNotBlank() }?.let { LocalDate.parse(it) },
                        bleeding   = e.bleeding,
                        bloodColor = e.bloodColor,
                        painLevel  = e.painLevel
                    )
                }
                val pillPacks = dao.getPillPacksForProfileOnce(profileId).map { e ->
                    PeriodViewModel.PillPack(
                        id        = e.id,
                        startDate = LocalDate.parse(e.startDate),
                        pillCount = e.pillCount,
                        endDate   = e.endDate?.takeIf { it.isNotBlank() }?.let { LocalDate.parse(it) }
                    )
                }

                val isOnPill     = pillPacks.any { it.endDate == null }
                val pillStopDate = pillPacks.filter { it.endDate != null }
                    .maxByOrNull { it.endDate!! }?.endDate
                if (isOnPill) return@launch

                val validCycles = if (pillStopDate != null)
                    cycles.filter { !it.startDate.isBefore(pillStopDate) } else cycles
                if (pillStopDate != null && isStillTransitioning(validCycles)) return@launch

                val prediction   = predictCycle(validCycles) ?: return@launch
                val ovulationDay = prediction.ovulationDay
                val daysUntil    = ChronoUnit.DAYS.between(LocalDate.now(), ovulationDay).toInt()

                if (daysUntil == daysBefore) {
                    val dateStr = ovulationDay.format(DateTimeFormatter.ofPattern("MMM d"))
                    val (notifTitle, notifText) = when {
                        daysUntil == 0 ->
                            "Ovulation Day" to
                                    "$namePrefix predicted ovulation day is today."
                        daysUntil <= 2 ->
                            "Peak Fertility" to
                                    "$namePrefix fertile window is at its peak. Ovulation expected on $dateStr."
                        else ->
                            "Fertile Window Approaching" to
                                    "$namePrefix fertile window is opening soon. Ovulation predicted for $dateStr."
                    }
                    fireNotification(
                        context     = appCtx,
                        channelId   = "fertility_reminders_v1",
                        channelName = "Fertility",
                        title       = notifTitle,
                        text        = notifText
                    )
                }
            } catch (e: Throwable) {
                Log.e("FertilityReceiver", "Crash: ${e.message}", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}

// ── 5. PILL RECEIVER ──────────────────────────────────────────────────────────

class PillReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        val appCtx        = context.applicationContext
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val profileId = intent.getIntExtra(EXTRA_PROFILE_ID, -1)
                if (profileId == -1) return@launch

                val prefs     = appCtx.dataStore.data.first()
                val isEnabled = prefs[ReminderPrefs.pillEnabled(profileId)] ?: false
                val hour      = prefs[ReminderPrefs.pillHour(profileId)] ?: 8
                val minute    = prefs[ReminderPrefs.pillMinute(profileId)] ?: 0

                if (!isEnabled) return@launch
                ReminderScheduler.scheduleNextPillReminder(appCtx, profileId, hour, minute)

                System.loadLibrary("sqlcipher")
                val dao = AppDatabase.getDatabase(appCtx).periodCycleDao()

                val profileName = dao.getProfileById(profileId)?.name
                val namePrefix  = profileName?.let { possessive(it) } ?: "Your"

                val pillPacks = dao.getPillPacksForProfileOnce(profileId).map { e ->
                    PeriodViewModel.PillPack(
                        id        = e.id,
                        startDate = LocalDate.parse(e.startDate),
                        pillCount = e.pillCount,
                        endDate   = e.endDate?.takeIf { it.isNotBlank() }?.let { LocalDate.parse(it) }
                    )
                }

                val activePack = pillPacks.firstOrNull { it.endDate == null } ?: return@launch
                val today      = LocalDate.now()
                val dayNumber  = (ChronoUnit.DAYS.between(activePack.startDate, today) + 1)
                    .toInt().coerceIn(1, activePack.pillCount)

                fireNotification(
                    context     = appCtx,
                    channelId   = "pill_reminders_v1",
                    channelName = "Pill Reminders",
                    title       = "Time to take $namePrefix pill 💊",
                    text        = "Day $dayNumber of ${activePack.pillCount} — stay consistent!"
                )
            } catch (e: Throwable) {
                Log.e("PillReceiver", "Crash: ${e.message}", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}

// ── 6. SHARED HELPERS ─────────────────────────────────────────────────────────

/**
 * Returns the English possessive form of a name.
 * "Sophie" → "Sophie's", "James" → "James'"
 */
private fun possessive(name: String): String =
    if (name.endsWith("s", ignoreCase = true)) "$name'" else "$name's"

private fun fireNotification(
    context: Context,
    channelId: String,
    channelName: String,
    title: String,
    text: String
) {
    val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        notificationManager.createNotificationChannel(
            NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_HIGH)
                .apply { description = channelName }
        )
    }

    val contentIntent = PendingIntent.getActivity(
        context, 0,
        Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        },
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val notification = NotificationCompat.Builder(context, channelId)
        .setSmallIcon(R.drawable.logo_trans)
        .setContentTitle(title)
        .setContentText(text)
        .setAutoCancel(true)
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setContentIntent(contentIntent)
        .build()

    notificationManager.notify(
        (System.currentTimeMillis() % Int.MAX_VALUE).toInt(), notification
    )
}