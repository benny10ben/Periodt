package com.ben.periodt

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

@RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
fun showLaunchTestNotification(context: Context) {
    val contentIntent = PendingIntent.getActivity(
        context, 0,
        Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
        PendingIntent.FLAG_UPDATE_CURRENT or if (Build.VERSION.SDK_INT >= 23) PendingIntent.FLAG_IMMUTABLE else 0
    )

    val notification = NotificationCompat.Builder(context, "period_reminders")
        .setSmallIcon(R.drawable.logo_trans)
        .setContentTitle("Design check")
        .setContentText("This is a test notification fired on launch.")
        .setStyle(NotificationCompat.BigTextStyle().bigText("This is a test notification fired on launch."))
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        .setAutoCancel(true)
        .setContentIntent(contentIntent)
        .build()

    NotificationManagerCompat.from(context).notify(100_001, notification)
}
