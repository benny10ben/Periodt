// uiux/onboarding/OnboardingNavigator.kt
package com.ben.periodt.uiux.onboarding

import androidx.compose.runtime.*
import kotlinx.coroutines.delay
import com.ben.periodt.MainActivity
import androidx.lifecycle.lifecycleScope

@Composable
fun OnboardingNavigator(
    activity: MainActivity,
    onFinished: () -> Unit
) {
    var step by remember { mutableStateOf(0) }

    OnboardingPager(
        step = step,
        onNext = { step = (step + 1).coerceAtMost(2) },
        onBack = { step = (step - 1).coerceAtLeast(0) },
        onAllow = {
            activity.ensureNotificationPermission {
                activity.lifecycleScope.launchWhenResumed {
                    delay(150)
                    onFinished()
                }
            }
        }
    )
}
