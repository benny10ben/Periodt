package com.ben.periodt

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.animation.*
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.core.content.ContextCompat
import androidx.work.*
import com.ben.periodt.network.ApiClient
import com.ben.periodt.network.PeriodtNetworkRepository
import com.ben.periodt.security.TokenManager
import com.ben.periodt.sync.PeriodtSyncWorker
import com.ben.periodt.ui.MainScreen
import com.ben.periodt.ui.onboarding.OnboardingNavigator
import com.ben.periodt.ui.theme.PeriodTTheme
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {

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

    @RequiresApi(Build.VERSION_CODES.P)
    override fun onCreate(savedInstanceState: Bundle?) {
        ensureReminderChannel(applicationContext)
        super.onCreate(savedInstanceState)
        System.loadLibrary("sqlcipher")

        enableEdgeToEdge()

        // ── 1. Network & Security Initialization ──
        val tokenManager = TokenManager(applicationContext)
        val apiClient = ApiClient(tokenManager)
        val repository = PeriodtNetworkRepository(apiClient)

        // ── 2. Background Sync Scheduling (Heartbeat + Kick) ──
        val syncConstraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        // The Heartbeat: 15-minute background loop (KEEP prevents resetting the timer)
        val syncWorkRequest = PeriodicWorkRequestBuilder<PeriodtSyncWorker>(
            15, TimeUnit.MINUTES
        )
            .setConstraints(syncConstraints)
            .build()

        WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork(
            "PeriodtBackgroundSync",
            ExistingPeriodicWorkPolicy.KEEP,
            syncWorkRequest
        )

        // The Kick: Instant sync triggered every time the app starts
        val immediateSyncRequest = OneTimeWorkRequestBuilder<PeriodtSyncWorker>()
            .setConstraints(syncConstraints)
            .build()

        WorkManager.getInstance(applicationContext).enqueueUniqueWork(
            "PeriodtImmediateSync",
            ExistingWorkPolicy.REPLACE,
            immediateSyncRequest
        )

        setContent {
            PeriodTTheme {
                val ctx = applicationContext
                var showOnboarding by remember { mutableStateOf(!OnboardingPrefs.isDone(ctx)) }

                Surface(color = Color.Transparent) {
                    AnimatedContent(
                        targetState = showOnboarding,
                        transitionSpec = {
                            if (!targetState) {
                                slideInVertically(
                                    animationSpec = tween(1000, easing = LinearOutSlowInEasing),
                                    initialOffsetY = { it }
                                ).togetherWith(
                                    slideOutVertically(
                                        animationSpec = tween(1000, easing = LinearOutSlowInEasing),
                                        targetOffsetY = { -it }
                                    )
                                ).apply {
                                    targetContentZIndex = 1f
                                }
                            } else {
                                fadeIn(tween(1000)) togetherWith fadeOut(tween(1000))
                            }
                        },
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RectangleShape),
                        label = "OnboardingToMainScroll"
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
            ).apply { description = "Notifies 2 days before predicted period" }
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
            nm.createNotificationChannel(channel)
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