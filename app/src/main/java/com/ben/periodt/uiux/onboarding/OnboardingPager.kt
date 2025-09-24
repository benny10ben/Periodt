package com.ben.periodt.uiux.onboarding

import android.os.Build
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ben.periodt.uiux.SetSystemBars


@Composable
private fun HeroTitleLeft(
    text: String,
    modifier: Modifier = Modifier
) {

    val isDark = isSystemInDarkTheme()
    val textcolor = if (isDark) Color.Black else Color.White

    // Slightly smaller, bold, left-aligned heading
    val style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold) // M3 mapping [web:263]
    Text(
        text = text,
        style = style,
        color = textcolor,
        lineHeight = style.fontSize * 1.06f,
        textAlign = TextAlign.Start,                 // left/start aligned [web:152]
        modifier = modifier
            .fillMaxWidth()                          // so Start = left edge [web:158]
            .padding(horizontal = 10.dp)
    )
}

@Composable
private fun BodyCopyLeft(
    text: String,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val textcolor = if (isDark) Color.Black.copy(alpha = 0.78f) else Color.White.copy(alpha = 0.78f)
    val body = MaterialTheme.typography.bodyLarge
    Text(
        text = text,
        style = body,
        color = textcolor,
        lineHeight = body.fontSize * 1.45f,
        textAlign = TextAlign.Start,                 // left/start aligned [web:152]
        modifier = modifier
            .fillMaxWidth()                          // ensure left edge alignment [web:158]
            .padding(horizontal = 10.dp)
    )
}

@Composable
fun OnboardingPager(
    step: Int,
    onNext: () -> Unit,
    onBack: () -> Unit,
    onAllow: () -> Unit
) {
    // Static chrome: gradient, logo, indicator
    OnboardingRoot(step = step) {

        val isDark = isSystemInDarkTheme()
        val bg1 = if (isDark) Color.Transparent else Color.Transparent

        SetSystemBars(statusBarColor = bg1, darkIcons = !isDark)

        // Animated title/body block (pure horizontal slide)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp)
        ) {
            AnimatedContent(
                targetState = step,
                transitionSpec = {
                    val forward = initialState < targetState
                    val enterOffset: (Int) -> Int = if (forward) { w -> w } else { w -> -w }
                    val exitOffset: (Int) -> Int  = if (forward) { w -> -w } else { w -> w }

                    val enter = slideInHorizontally(
                        animationSpec = tween(260),
                        initialOffsetX = enterOffset
                    ) + fadeIn(tween(160))

                    val exit = slideOutHorizontally(
                        animationSpec = tween(260),
                        targetOffsetX = exitOffset
                    ) + fadeOut(tween(120))

                    (enter togetherWith exit).using(
                        SizeTransform(
                            clip = false,
                            sizeAnimationSpec = { _, _ -> tween(0) }
                        )
                    )
                },
                label = "OnboardingBody"
            ) { page ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 120.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Top
                ) {
                    when (page) {
                        0 -> {
                            HeroTitleLeft(
                                "Important Disclaimer",
                                modifier = Modifier.align(Alignment.Start)
                            )
                            Spacer(Modifier.height(10.dp)) // smaller gap title -> body [web:275]
                            BodyCopyLeft(
                                "This is a period tracker. Ovulation and fertile window predictions are general estimates; individual cycles vary.\nThis app is not a medical device and does not provide medical advice.",
                                modifier = Modifier.align(Alignment.Start)
                            )
                            Spacer(Modifier.height(20.dp)) // larger gap body -> buttons [web:273]
                        }
                        1 -> {
                            HeroTitleLeft("About Periodt", modifier = Modifier.align(Alignment.Start))
                            Spacer(Modifier.height(10.dp))
                            BodyCopyLeft(
                                "• No internet access required; all data stays on device.\n• No ads or trackers; privacy‑first design.\n• Open source for transparency and community review.",
                                modifier = Modifier.align(Alignment.Start)
                            )
                            Spacer(Modifier.height(20.dp))
                        }
                        2 -> {
                            val notice = if (Build.VERSION.SDK_INT >= 33)
                                "Allow notifications to receive reminder alerts."
                            else
                                "Enable reminders for timely alerts."
                            HeroTitleLeft("Enable Notifications", modifier = Modifier.align(Alignment.Start))
                            Spacer(Modifier.height(10.dp))
                            BodyCopyLeft(
                                "Turn on notifications so reminders arrive 5 and 2 days before the expected start.\n$notice",
                                modifier = Modifier.align(Alignment.Start)
                            )
                            Spacer(Modifier.height(20.dp))
                        }
                    }
                }
            }
        }

        // Fixed buttons (do not slide)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .align(Alignment.CenterHorizontally)
        ) {
            when (step) {
                0 -> {
                    Spacer(Modifier.height(24.dp))
                    ButtonRow(primary = "Understood" to onNext)
                }
                1 -> {
                    Spacer(Modifier.height(24.dp))
                    ButtonRow(
                        primary = "Continue" to onNext,
                        secondary = "Back" to onBack
                    )
                }
                2 -> {
                    Spacer(Modifier.height(24.dp))
                    ButtonRow(
                        primary = "Allow" to onAllow,
                        secondary = "Back" to onBack
                    )
                }
            }
        }
    }
}
