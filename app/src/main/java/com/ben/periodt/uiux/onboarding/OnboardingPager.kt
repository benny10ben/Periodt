package com.ben.periodt.uiux.onboarding

import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
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
import com.ben.periodt.uiux.SetSystemBars
import com.ben.periodt.uiux.overview.ContentDialog
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingPager(
    step: Int,
    onNext: () -> Unit,
    onBack: () -> Unit,
    onAllow: () -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { 3 })
    val scope = rememberCoroutineScope()
    val isDark = isSystemInDarkTheme()

    SetSystemBars(statusBarColor = Color.Transparent, darkIcons = !isDark)

    // 1. THEME GRADIENT BACKGROUND
    val bgGradient = if (isDark) {
        Brush.linearGradient(
            0.0f to Color(0xFF1b1b1b),
            0.6f to Color.Black,
            start = Offset(0f, 0f),
            end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
        )
    } else {
        Brush.linearGradient(
            colors = listOf(Color(0xFFe8ebed), Color(0xFFf2f0e3)),
            start = Offset(0f, 0f),
            end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
        )
    }

    Box(modifier = Modifier.fillMaxSize().background(bgGradient)) {
        VerticalPager(
            state = pagerState,
            userScrollEnabled = false,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            when (page) {
                0 -> WelcomePage(
                    onGetStarted = {
                        scope.launch {
                            pagerState.animateScrollToPage(
                                page = 1,
                                animationSpec = tween(1500, easing = FastOutSlowInEasing)
                            )
                        }
                    }
                )
                1 -> FeaturesPage(
                    onNext = {
                        scope.launch {
                            pagerState.animateScrollToPage(
                                page = 2,
                                animationSpec = tween(1500, easing = FastOutSlowInEasing)
                            )
                        }
                    }
                )
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

    // Logo Box Styling
    val logoBoxColor = if (isDark) Color.Black else Color.White
    val logoBorder = if (isDark) Color(0xFF333333) else Color(0xFFE2E8F0)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .navigationBarsPadding()
            .statusBarsPadding(),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.Top
    ) {
        Spacer(Modifier.weight(1f))

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
            text = "Master Your Body’s\nNatural Rhythm",
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

@Composable
fun FeaturesPage(onNext: () -> Unit) {
    val context = LocalContext.current
    val isDark = isSystemInDarkTheme()
    val textColor = if (isDark) Color.White else Color(0xFF1B1B1B)
    val subTextColor = textColor.copy(alpha = 0.6f)

    // State to control the Dialog visibility
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

            OnboardingButton(
                text = "Next",
                onClick = onNext)

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
                    ) {
                        showTermsDialog = true // Open dialog instead of link
                    }
            )

            Spacer(Modifier.height(12.dp))
        }
    }

    // Terms and Conditions Dialog
    if (showTermsDialog) {
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

// --- PAGE 3: MODE SELECTION ---
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ModeSelectionPage(onStart: () -> Unit) {
    val isDark = isSystemInDarkTheme()
    val textColor = if (isDark) Color.White else Color(0xFF1B1B1B)
    val subTextColor = textColor.copy(alpha = 0.7f)

    val infoPagerState = rememberPagerState(pageCount = { 3 })

    // DYNAMIC COLOR SELECTION: Maps the page index to the specific card theme
    val themeColor = when (infoPagerState.currentPage) {
        0 -> Color(0xFF2A3825) // Privacy First (Deep Green)
        1 -> Color(0xFFD89046) // Smart Predictions (Pastel Orange)
        2 -> Color(0xFF4E1A1A) // Cycle Syncing (Burgundy)
        else -> textColor
    }

    // Animate the color transition across the entire indicator set
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
            modifier = Modifier.padding(horizontal = 32.dp).fillMaxWidth()
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
            modifier = Modifier.fillMaxWidth().weight(1f),
            contentPadding = PaddingValues(horizontal = 56.dp),
            pageSpacing = 24.dp
        ) { page ->
            val pageOffset = ((infoPagerState.currentPage - page) + infoPagerState.currentPageOffsetFraction).let { kotlin.math.abs(it) }
            val scale = 1f - (0.15f * pageOffset.coerceIn(0f, 1f))
            val alpha = 1f - (0.4f * pageOffset.coerceIn(0f, 1f))

            Box(
                modifier = Modifier
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        this.alpha = alpha
                    }
                    .fillMaxHeight(0.9f)
                    .aspectRatio(0.75f)
            ) {
                when (page) {
                    0 -> InfoCard("Privacy First", "All your data is stored locally on your device. We never upload your personal history to any servers.", Icons.Rounded.PrivacyTip, Color(0xFF2A3825))
                    1 -> InfoCard("Smart Predictions", "We calculate your cycle based on the average of your last 3 logs. The more you log, the more accurate we get.", Icons.Rounded.AutoAwesome, Color(0xFFD89046))
                    2 -> InfoCard("Cycle Syncing", "Get phase-specific advice on nutrition, exercise, and sleep to live in harmony with your hormones.", Icons.Rounded.SelfImprovement, Color(0xFF4E1A1A))
                }
            }
        }

        Spacer(Modifier.height(32.dp))

        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Updated Indicator: All dots now follow the animatedThemeColor
            PageIndicator(
                current = infoPagerState.currentPage,
                total = 3,
                activeColor = animatedThemeColor
            )

            Spacer(Modifier.height(32.dp))

            OnboardingButton(
                text = "Start Tracking",
                onClick = onStart)
        }
    }
}

@Composable
fun InfoCard(title: String, subtitle: String, icon: ImageVector, backgroundColor: Color) {
    Box(
        modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(36.dp)).background(backgroundColor).padding(32.dp)
    ) {
        Column(modifier = Modifier.align(Alignment.TopStart)) {
            Box(
                modifier = Modifier.size(56.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = Color.White, modifier = Modifier.size(28.dp))
            }
            Spacer(Modifier.weight(1f))
            Text(title, fontFamily = BricolageGrotesque, style = MaterialTheme.typography.headlineSmall, color = Color.White, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            Text(subtitle, fontFamily = BricolageGrotesque, style = MaterialTheme.typography.bodyLarge, color = Color.White.copy(alpha = 0.85f), lineHeight = 24.sp)
            Spacer(Modifier.height(8.dp))
        }
    }
}