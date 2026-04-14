package com.ben.periodt.ui.onboarding

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ben.periodt.R
import com.ben.periodt.ui.theme.BricolageGrotesque
import com.ben.periodt.ui.theme.LocalAppIsDark
import com.ben.periodt.ui.theme.SetSystemBars
import com.ben.periodt.ui.overview.ContentDialog
import kotlin.math.abs

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun OnboardingPager(
    step: Int,
    onNext: () -> Unit,
    onBack: () -> Unit,
    onAllow: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    SetSystemBars(statusBarColor = Color.Transparent, darkIcons = !isDark)

    OnboardingRoot {
        AnimatedContent(
            targetState = step,
            transitionSpec = {
                if (targetState > initialState) {
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
                    slideInVertically(
                        animationSpec = tween(1000, easing = LinearOutSlowInEasing),
                        initialOffsetY = { -it }
                    ).togetherWith(
                        slideOutVertically(
                            animationSpec = tween(1000, easing = LinearOutSlowInEasing),
                            targetOffsetY = { it }
                        )
                    ).apply {
                        targetContentZIndex = 1f
                    }
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .clip(RectangleShape),
            label = "onboarding_content"
        ) { page ->
            when (page) {
                0 -> WelcomePage(onGetStarted = onNext)
                1 -> FeaturesPage(onNext = onNext)
                2 -> ModeSelectionPage(onStart = onAllow)
            }
        }
    }
}

// --- PAGE 1: WELCOME ---
@Composable
fun WelcomePage(onGetStarted: () -> Unit) {
    val isDark = isSystemInDarkTheme()
    val titleColor = if (isDark) Color.White else Color(0xFF1B1B1B)
    val logoBoxColor = if (isDark) Color.Black else Color.White
    val logoBorder = if (isDark) Color(0xFF333333) else Color(0xFFE2E8F0)

    var showLanguageDialog by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 20.dp)
    ) {
        Row(
            modifier = Modifier
                .statusBarsPadding()
                .padding(start = 24.dp, top = 16.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { showLanguageDialog = true },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "🇺🇸", fontSize = 18.sp)
            Spacer(Modifier.width(8.dp))
            Text(
                text = "English",
                fontFamily = BricolageGrotesque,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = titleColor.copy(alpha = 0.8f)
            )
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = null,
                tint = titleColor.copy(alpha = 0.5f),
                modifier = Modifier.size(20.dp)
            )
        }

        if (showLanguageDialog) {
            CompositionLocalProvider(LocalAppIsDark provides isSystemInDarkTheme()) {
                ContentDialog(
                    title = "Choose Language",
                    onDismiss = { showLanguageDialog = false }
                ) {
                    Column {
                        LanguageItem("English (US)", "🇺🇸", true, titleColor) {
                            showLanguageDialog = false
                        }
                        listOf(
                            "日本語" to "🇯🇵",
                            "한국어" to "🇰🇷",
                            "Español" to "🇪🇸",
                            "Français" to "🇫🇷",
                            "Deutsch" to "🇩🇪"
                        ).forEach { (lang, flag) ->
                            LanguageItem(lang, flag, false, titleColor) {}
                        }
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.Top
        ) {
            Spacer(Modifier.weight(1.5f))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(logoBoxColor)
                        .border(1.dp, logoBorder, RoundedCornerShape(24.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.logo),
                        contentDescription = "Periodt Logo",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
                Spacer(Modifier.width(16.dp))
                Text(
                    text = "Periodt.",
                    fontFamily = BricolageGrotesque,
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                    color = titleColor
                )
            }

            Spacer(Modifier.weight(1.5f))

            Text(
                text = "Master Your Body's\nNatural Rhythm",
                fontFamily = BricolageGrotesque,
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Normal,
                textAlign = TextAlign.Start,
                color = titleColor
            )

            Spacer(Modifier.height(48.dp))

            OnboardingButton(text = "Get Started", onClick = onGetStarted)

            Spacer(Modifier.height(48.dp))
        }
    }
}

@Composable
fun LanguageItem(
    name: String,
    flag: String,
    isEnabled: Boolean,
    titleColor: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(enabled = isEnabled) { onClick() }
            .padding(vertical = 12.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = flag, fontSize = 20.sp, modifier = Modifier.alpha(if (isEnabled) 1f else 0.4f))
        Spacer(Modifier.width(16.dp))
        Text(
            text = name,
            fontFamily = BricolageGrotesque,
            fontSize = 16.sp,
            color = if (isEnabled) titleColor else titleColor.copy(alpha = 0.3f),
            modifier = Modifier.weight(1f)
        )
        if (!isEnabled) {
            Text(
                text = "Soon",
                fontFamily = BricolageGrotesque,
                fontSize = 12.sp,
                color = titleColor.copy(alpha = 0.2f)
            )
        }
    }
}

// --- PAGE 2: FEATURES ---
@Composable
fun FeaturesPage(onNext: () -> Unit) {
    val isDark = isSystemInDarkTheme()
    val textColor = if (isDark) Color.White else Color(0xFF1B1B1B)
    val subTextColor = textColor.copy(alpha = 0.6f)

    var showTermsDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
            .statusBarsPadding()
    ) {
        Box(
            modifier = Modifier
                .weight(1.2f)
                .fillMaxWidth()
                .clip(RectangleShape)
                .graphicsLayer {
                    compositingStrategy = CompositingStrategy.Offscreen
                }
                .drawWithContent {
                    drawContent()
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black),
                            startY = size.height * 0.6f,
                            endY = size.height
                        ),
                        blendMode = BlendMode.DstOut
                    )
                }
        ) {
            AnimatedIconBackground()
        }

        Column(
            horizontalAlignment = Alignment.Start,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 32.dp)
        ) {
            Text(
                text = "Smarter Habits\nfor Every Day.",
                fontFamily = BricolageGrotesque,
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Normal,
                color = textColor,
                lineHeight = 42.sp,
                letterSpacing = (-0.5).sp
            )

            Spacer(Modifier.height(40.dp))

            OnboardingButton(text = "Next", onClick = onNext)

            Spacer(Modifier.height(24.dp))

            val annotatedString = buildAnnotatedString {
                withStyle(SpanStyle(color = subTextColor)) {
                    append("By clicking \"Next\", you agree to our ")
                }
                withStyle(
                    SpanStyle(
                        color = textColor,
                        fontWeight = FontWeight.SemiBold,
                        textDecoration = TextDecoration.Underline
                    )
                ) {
                    append("Terms & Conditions")
                }
            }

            BasicText(
                text = annotatedString,
                style = MaterialTheme.typography.bodySmall.copy(
                    textAlign = TextAlign.Center,
                    fontFamily = BricolageGrotesque
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { showTermsDialog = true }
            )

            Spacer(Modifier.height(12.dp))
        }
    }

    if (showTermsDialog) {
        CompositionLocalProvider(LocalAppIsDark provides isSystemInDarkTheme()) {
            ContentDialog(
                title = "Terms & Conditions",
                onDismiss = { showTermsDialog = false }
            ) {
                Text(
                    text = "By using Periodt., you agree that your data is stored locally on this device. " +
                            "We do not collect, sell, or share your personal health information.\n\n" +
                            "1. Data Privacy: Your history remains yours.\n" +
                            "2. Usage: This app provides insights, not medical advice.\n" +
                            "3. Backups: You are responsible for exporting your data backups.",
                    fontFamily = BricolageGrotesque,
                    color = subTextColor,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
            }
        }
    }
}

// --- PAGE 3: MODE SELECTION ---
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ModeSelectionPage(onStart: () -> Unit) {
    val isDark = isSystemInDarkTheme()
    val textColor = if (isDark) Color.White else Color(0xFF1B1B1B)
    val subTextColor = textColor.copy(alpha = 0.7f)

    // Expanded to 5 cards to include Profiles and Pills
    val infoPagerState = rememberPagerState(pageCount = { 5 })

    val themeColor = when (infoPagerState.currentPage) {
        0 -> if (isDark) Color(0xFF42553f) else Color(0xFFa5bda3)
        1 -> Color(0xFFD89046) // Smart Predictions (Orange)
        2 -> Color(0xFF4E1A1A) // Cycle Syncing (Maroon)
        3 -> if (isDark) Color(0xFF42553f) else Color(0xFFa5bda3)
        4 -> Color(0xFFA68E74) // Pill Tracking (Sand/Taupe)
        else -> textColor
    }

    val animatedThemeColor by animateColorAsState(
        targetValue = themeColor,
        animationSpec = tween(500),
        label = "syncIndicatorColor"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 24.dp, bottom = 36.dp)
            .navigationBarsPadding()
            .statusBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 32.dp)
                .fillMaxWidth()
        ) {
            Spacer(Modifier.height(24.dp))
            Text(
                text = "Your Guide to\nPeriodt.",
                fontFamily = BricolageGrotesque,
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = textColor,
                lineHeight = 44.sp
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = "Swipe to learn how we protect and predict.",
                fontFamily = BricolageGrotesque,
                style = MaterialTheme.typography.bodyLarge,
                color = subTextColor
            )
        }

        Spacer(Modifier.height(40.dp))

        HorizontalPager(
            state = infoPagerState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentPadding = PaddingValues(horizontal = 48.dp),
            pageSpacing = 16.dp
        ) { page ->
            val pageOffset = ((infoPagerState.currentPage - page) + infoPagerState.currentPageOffsetFraction)
                .let { abs(it) }
            val scale = 1f - (0.15f * pageOffset.coerceIn(0f, 1f))
            val cardAlpha = 1f - (0.4f * pageOffset.coerceIn(0f, 1f))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.9f)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    }
                    .alpha(cardAlpha),
                contentAlignment = Alignment.Center
            ) {
                when (page) {
                    0 -> InfoCard("Privacy First", "All your data is stored locally on your device. We never upload your personal history to any servers.", Icons.Rounded.PrivacyTip, if (isDark) Color(0xFF42553f) else Color(0xFFa5bda3))
                    1 -> InfoCard("Smart Predictions", "We calculate your cycle based on the average of your last 3 logs. The more you log, the more accurate we get.", Icons.Rounded.AutoAwesome, Color(0xFFD89046))
                    2 -> InfoCard("Cycle Syncing", "Get phase-specific advice on nutrition, exercise, and sleep to live in harmony with your hormones.", Icons.Rounded.SelfImprovement, Color(0xFF4E1A1A))
                    3 -> InfoCard("Multi-Profile", "Track cycles for yourself, family, or partners effortlessly from one app with seamless profile switching.", Icons.Rounded.Group, if (isDark) Color(0xFF42553f) else Color(0xFFa5bda3))
                    4 -> InfoCard("Daily Pills", "Never miss a dose. Set customizable daily reminders for your birth control, vitamins, or supplements.", Icons.Rounded.Medication, Color(0xFFA68E74))
                }
            }
        }

        Spacer(Modifier.height(32.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            PageIndicator(
                current = infoPagerState.currentPage,
                total = 5,
                activeColor = animatedThemeColor
            )
            Spacer(Modifier.height(32.dp))
            OnboardingButton(text = "Start Tracking", onClick = onStart)
        }
    }
}

@Composable
fun InfoCard(title: String, subtitle: String, icon: ImageVector, backgroundColor: Color) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(32.dp))
            .background(backgroundColor)
            .padding(24.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = Color.White, modifier = Modifier.size(28.dp))
            }

            Spacer(Modifier.weight(1f))

            Text(
                title,
                fontFamily = BricolageGrotesque,
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            Text(
                subtitle,
                fontFamily = BricolageGrotesque,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.85f),
                lineHeight = 22.sp
            )
        }
    }
}