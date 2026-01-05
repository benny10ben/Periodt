// ReminderScheduler.kt
package com.ben.periodt.uiux.shared

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.time.*

object ReminderScheduler {

    private fun alarmManager(context: Context) =
        context.getSystemService(Context.ALARM_SERVICE) as AlarmManager // [web:6]

    // Two distinct PendingIntents (different request codes + an extra)
    private fun pendingDailyChecker830(context: Context, type: String): PendingIntent {
        val intent = Intent(context, DailyPredictionChecker::class.java).apply {
            putExtra("windowId", "830_$type") // make Intent unique [web:85]
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or
                (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        val reqCode = if (type == "twoDay") 999202 else 999203 // distinct [web:85]
        return PendingIntent.getBroadcast(context, reqCode, intent, flags)
    }

    private fun nextAt(hour: Int, minute: Int, zone: ZoneId = ZoneId.systemDefault()): Long {
        val now = ZonedDateTime.now(zone)
        val todayTarget = now.withHour(hour).withMinute(minute).withSecond(0).withNano(0)
        val next = if (!todayTarget.isBefore(now)) todayTarget else todayTarget.plusDays(1)
        return next.toInstant().toEpochMilli() // [web:6]
    }

    // Around 8:30 AM for 2-day reminder
    fun scheduleDailyChecker830_TwoDay(context: Context) {
        val am = alarmManager(context)
        val pi = pendingDailyChecker830(context, "twoDay")
        val triggerAt = nextAt(8, 30)
        val windowMs = 20 * 60 * 1000L
        am.setWindow(AlarmManager.RTC_WAKEUP, triggerAt, windowMs, pi) // inexact [web:6]
    }

    // Around 8:30 AM for 5-day reminder
    fun scheduleDailyChecker830_FiveDay(context: Context) {
        val am = alarmManager(context)
        val pi = pendingDailyChecker830(context, "fiveDay")
        val triggerAt = nextAt(8, 30)
        val windowMs = 20 * 60 * 1000L
        am.setWindow(AlarmManager.RTC_WAKEUP, triggerAt, windowMs, pi) // inexact [web:6]
    }

    fun scheduleAllDailyChecks(context: Context) {
        scheduleDailyChecker830_TwoDay(context)
        scheduleDailyChecker830_FiveDay(context)
    }

    fun fireNow(context: Context, daysBefore: Int, targetDateText: String) {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra("daysBefore", daysBefore)
            putExtra("targetDateText", targetDateText)
            putExtra("notifId", (System.currentTimeMillis() % Int.MAX_VALUE).toInt())
        }
        context.sendBroadcast(intent) // [web:6]
    }
}
