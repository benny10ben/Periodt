package com.ben.periodt.ui.onboarding

import androidx.compose.runtime.*
import kotlinx.coroutines.delay
import com.ben.periodt.MainActivity
import androidx.lifecycle.lifecycleScope
import com.ben.periodt.viewmodel.AuthViewModel

@Composable
fun OnboardingNavigator(
    activity: MainActivity,
    viewModel: AuthViewModel,
    onFinished: () -> Unit
) {
    var step by remember { mutableStateOf(0) }

    OnboardingPager(
        step = step,
        viewModel = viewModel,
        onNext = { step = (step + 1).coerceAtMost(3) },
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