// ReminderReceiver.kt
package com.ben.periodt.uiux.shared

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.app.PendingIntent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.ben.periodt.MainActivity
import com.ben.periodt.R

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val daysBefore = intent.getIntExtra("daysBefore", -1)
        val dateText = intent.getStringExtra("targetDateText") ?: ""
        val id = intent.getIntExtra("notifId", (System.currentTimeMillis() % Int.MAX_VALUE).toInt())

        val canPost = if (Build.VERSION.SDK_INT >= 33) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else true
        if (!canPost) return // require runtime permission on 13+ [web:6]

        val title = when (daysBefore) {
            5 -> "Period in 5 days"
            2 -> "Period in 2 days"
            else -> "Upcoming period"
        }

        val text = if (dateText.isNotBlank()) "Expected around $dateText" else "Track to refine your prediction"

        val contentIntent = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            (PendingIntent.FLAG_UPDATE_CURRENT or if (Build.VERSION.SDK_INT >= 23) PendingIntent.FLAG_IMMUTABLE else 0)
        ) // tap opens app [web:6]

        val notification = NotificationCompat.Builder(context, "period_reminders")
            .setSmallIcon(R.drawable.logo_trans)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .build() // channel created at app start [web:26]

        NotificationManagerCompat.from(context).notify(id, notification) // show notification [web:26]
    }
}
