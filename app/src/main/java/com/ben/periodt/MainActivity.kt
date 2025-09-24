package com.ben.periodt

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.core.content.ContextCompat
import com.ben.periodt.ui.theme.PeriodTTheme
import com.ben.periodt.uiux.MainScreen
import com.ben.periodt.uiux.onboarding.OnboardingNavigator

class MainActivity : ComponentActivity() {

    // ---- Notification permission request (Android 13+) ----
    private var onNotifResult: ((Boolean) -> Unit)? = null

    private val requestPostNotifications = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        onNotifResult?.invoke(granted)
        onNotifResult = null
    }

    fun ensureNotificationPermission(onResult: (Boolean) -> Unit) {
        if (Build.VERSION.SDK_INT < 33) {
            onResult(true)
            return
        }
        val granted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) {
            onResult(true)
        } else {
            onNotifResult = onResult
            requestPostNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        ensureReminderChannel(applicationContext) // safe to call repeatedly at startup [web:11][web:26]
        super.onCreate(savedInstanceState)
        System.loadLibrary("sqlcipher")  // ✅ Required for sqlcipher-android


        setContent {
            PeriodTTheme {
                val ctx = applicationContext
                var showOnboarding by remember { mutableStateOf(!OnboardingPrefs.isDone(ctx)) }

                Surface(color = MaterialTheme.colorScheme.background) {
                    androidx.compose.animation.Crossfade(
                        targetState = showOnboarding,
                        animationSpec = androidx.compose.animation.core.tween(durationMillis = 350),
                        label = "Onboarding->Main crossfade" // optional
                    ) { isOnboarding ->
                        if (isOnboarding) {
                            OnboardingNavigator(
                                activity = this@MainActivity,
                                onFinished = {
                                    OnboardingPrefs.setDone(ctx, true)
                                    showOnboarding = false
                                }
                            )
                        } else {
                            MainScreen()
                        }
                    }
                }
            }
        }


    }

    private fun ensureReminderChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = android.app.NotificationChannel(
                "period_reminders",
                "Period reminders",
                android.app.NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifies 2 days before predicted period"
            }
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
            nm.createNotificationChannel(channel) // idempotent at app start [web:11][web:26]
        }
    }

    object OnboardingPrefs {
        private const val NAME = "onboarding_prefs"
        private const val KEY_DONE = "onboarding_done"
        fun isDone(context: Context): Boolean =
            context.getSharedPreferences(NAME, Context.MODE_PRIVATE).getBoolean(KEY_DONE, false)
        fun setDone(context: Context, value: Boolean) {
            context.getSharedPreferences(NAME, Context.MODE_PRIVATE)
                .edit().putBoolean(KEY_DONE, value).apply()
        }
    }
}
