package com.ben.periodt.uiux.pill

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ben.periodt.ui.theme.BricolageGrotesque
import com.ben.periodt.ui.theme.LocalAppIsDark
import com.ben.periodt.uiux.calendar.SwipeToDeleteCard
import com.ben.periodt.uiux.shared.PostPillState
import com.ben.periodt.uiux.shared.pretty
import com.ben.periodt.viewmodel.PeriodViewModel
import java.time.LocalDate
import java.time.temporal.ChronoUnit

private val SIZE_XXS = 11.sp
private val SIZE_SM  = 13.sp
private val SIZE_MD  = 14.sp
private val SIZE_LG  = 15.sp
private val SIZE_XL  = 20.sp

@Composable
fun PillTrackerScreen(viewModel: PeriodViewModel) {
    val isOnPill      by viewModel.isOnPill.collectAsState()
    val postPillState by viewModel.postPillState.collectAsState()
    val startDate     by viewModel.pillPackStartDate.collectAsState()
    val pillCount     by viewModel.pillPackCount.collectAsState()
    val pillHistory   by viewModel.pillPacks.collectAsState()

    val isDark = LocalAppIsDark.current

    val textPrimary    = if (isDark) Color.White else Color(0xFF0F172A)
    val textSub        = if (isDark) Color.White.copy(alpha = 0.6f) else Color(0xFF1b1b1b).copy(alpha = 0.6f)
    val cardBg         = if (isDark) Color(0xFF1B1B1B).copy(alpha = 0.5f) else Color.White
    val pillBackground = if (isDark) Color(0xFFE8EBED).copy(alpha = 0.1f) else Color(0xFFE8EBED).copy(alpha = 0.4f)

    val starAccent  = if (isDark) Color(0xFF8089D2) else Color(0xFF2C3F70)
    val themeAccent = Color(0xFFa68e74)

    val activeAccent = when (postPillState) {
        PostPillState.DISCOVERY, PostPillState.LEARNING -> starAccent
        else -> themeAccent
    }

    LaunchedEffect(Unit) { viewModel.refreshState() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(bottom = 120.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(32.dp))

        AnimatedContent(targetState = Pair(isOnPill, postPillState), label = "HeaderState") { (active, pillState) ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier         = Modifier.size(80.dp).clip(CircleShape).background(Color.Transparent),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when {
                            active                               -> Icons.Rounded.Medication
                            pillState == PostPillState.DISCOVERY -> Icons.Rounded.AutoAwesome
                            pillState == PostPillState.LEARNING  -> Icons.Rounded.AutoAwesome
                            else                                 -> Icons.Rounded.Medication
                        },
                        contentDescription = null,
                        modifier           = Modifier.size(40.dp),
                        tint               = activeAccent
                    )
                }

                Spacer(Modifier.height(24.dp))

                Text(
                    text = when {
                        active                               -> "Pill Tracking Active"
                        pillState == PostPillState.DISCOVERY -> "Discovery Mode"
                        pillState == PostPillState.LEARNING  -> "Learning Mode"
                        else                                 -> "Pill Mode"
                    },
                    fontSize   = 28.sp,
                    fontFamily = BricolageGrotesque,
                    fontWeight = FontWeight.Bold,
                    color      = textPrimary
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    text = when {
                        active -> "Your active pill pack is being tracked.\nWithdrawal bleed date is predicted automatically."
                        pillState == PostPillState.DISCOVERY -> "Your body is recalibrating after the pill.\nPredictions are paused until your first cycle is logged."
                        pillState == PostPillState.LEARNING  -> "Predictions are live but still refining.\nKeep logging to improve accuracy."
                        else -> "Tap the button below to set up your pack."
                    },
                    fontSize   = SIZE_LG,
                    fontFamily = BricolageGrotesque,
                    color      = textSub,
                    textAlign  = TextAlign.Center,
                    lineHeight = 24.sp,
                    modifier   = Modifier.padding(horizontal = 16.dp)
                )
            }
        }

        Spacer(Modifier.height(40.dp))

        LifeAfterPillCard(
            cardBg        = cardBg,
            textPrimary   = textPrimary,
            textSub       = textSub,
            postPillState = postPillState
        )

        if (isOnPill && startDate != null) {
            Spacer(Modifier.height(32.dp))
            ActivePackSection(
                startDate      = startDate!!,
                pillCount      = pillCount,
                cardBg         = cardBg,
                pillBackground = pillBackground,
                textPrimary    = textPrimary,
                textSub        = textSub,
                accentColor    = themeAccent,
                onStop         = { viewModel.stopPillTracking() }
            )
        }

        Spacer(Modifier.height(48.dp))

        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text       = "Pill History",
                fontFamily = BricolageGrotesque,
                fontWeight = FontWeight.Bold,
                fontSize   = SIZE_XL,
                color      = textPrimary,
                modifier   = Modifier.align(Alignment.CenterHorizontally)
            )

            Spacer(Modifier.height(16.dp))

            if (pillHistory.isEmpty()) {
                Box(
                    modifier         = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No pill history yet.",
                        color      = textSub,
                        fontFamily = BricolageGrotesque,
                        fontSize   = SIZE_MD
                    )
                }
            } else {
                pillHistory.sortedByDescending { it.startDate }.forEach { pack ->
                    SwipeToDeleteCard(onDelete = { viewModel.deletePillPack(pack.id) }) { isSwiping ->
                        PillHistoryItem(
                            pack           = pack,
                            cardBg         = cardBg,
                            textPrimary    = textPrimary,
                            textSub        = textSub,
                            pillBackground = pillBackground,
                            activeAccent   = themeAccent,
                            normalAccent   = textPrimary,
                            isSwiping      = isSwiping
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                }
            }
        }
    }
}

@Composable
private fun ActivePackSection(
    startDate: LocalDate,
    pillCount: Int,
    cardBg: Color,
    pillBackground: Color,
    textPrimary: Color,
    textSub: Color,
    accentColor: Color,
    onStop: () -> Unit
) {
    val isDark       = LocalAppIsDark.current
    val daysElapsed  = ChronoUnit.DAYS.between(startDate, LocalDate.now()).toInt().coerceAtLeast(0)
    val daysTaken    = minOf(daysElapsed + 1, pillCount)
    val progressTarget = (daysTaken.toFloat() / pillCount.toFloat()).coerceIn(0f, 1f)

    val animatedProgress by animateFloatAsState(
        targetValue   = progressTarget,
        animationSpec = tween(1200),
        label         = "PillProgress"
    )

    val progressBrush = remember(isDark, accentColor) {
        Brush.linearGradient(colors = listOf(accentColor, accentColor))
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Card(
            colors    = CardDefaults.cardColors(containerColor = cardBg),
            shape     = RoundedCornerShape(22.dp),
            elevation = CardDefaults.cardElevation(0.dp),
            modifier  = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 18.dp)) {
                // Top row
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier         = Modifier.size(42.dp).clip(CircleShape).background(pillBackground),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.Medication, null, tint = textPrimary, modifier = Modifier.size(24.dp))
                    }

                    Spacer(Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Active Pill Pack",
                            fontFamily = BricolageGrotesque,
                            color      = textPrimary,
                            fontSize   = SIZE_LG,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            "Started ${startDate.pretty()}",
                            fontFamily = BricolageGrotesque,
                            color      = textSub,
                            fontSize   = SIZE_SM
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(pillBackground)
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            "$pillCount Pills",
                            fontFamily = BricolageGrotesque,
                            fontWeight = FontWeight.SemiBold,
                            color      = textPrimary,
                            fontSize   = SIZE_XXS
                        )
                    }
                }

                Spacer(Modifier.height(20.dp))

                // Progress label row
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Text(
                        "Pack Progress",
                        fontFamily = BricolageGrotesque,
                        fontWeight = FontWeight.SemiBold,
                        color      = textPrimary,
                        fontSize   = SIZE_MD
                    )
                    Text(
                        "$daysTaken / $pillCount",
                        fontFamily = BricolageGrotesque,
                        color      = textSub,
                        fontSize   = SIZE_SM
                    )
                }

                Spacer(Modifier.height(12.dp))

                Canvas(
                    Modifier
                        .fillMaxWidth(0.95f)
                        .height(6.dp)
                        .clip(CircleShape)
                        .align(Alignment.CenterHorizontally)
                ) {
                    drawRoundRect(color = pillBackground.copy(alpha = 0.4f), size = size, cornerRadius = androidx.compose.ui.geometry.CornerRadius(50f))
                    drawRoundRect(brush = progressBrush, size = androidx.compose.ui.geometry.Size(animatedProgress * size.width, size.height), cornerRadius = androidx.compose.ui.geometry.CornerRadius(50f))
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        Button(
            onClick   = onStop,
            modifier  = Modifier.fillMaxWidth().height(56.dp),
            shape     = RoundedCornerShape(100.dp),
            colors    = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF5350).copy(alpha = 0.1f)),
            elevation = ButtonDefaults.buttonElevation(0.dp)
        ) {
            Icon(Icons.Rounded.StopCircle, null, tint = Color(0xFFEF5350))
            Spacer(Modifier.width(8.dp))
            Text(
                "Stop Taking Pills",
                fontSize   = SIZE_LG,
                fontFamily = BricolageGrotesque,
                fontWeight = FontWeight.Bold,
                color      = Color(0xFFEF5350)
            )
        }
    }
}

@Composable
fun LifeAfterPillCard(
    cardBg: Color,
    textPrimary: Color,
    textSub: Color,
    postPillState: PostPillState
) {
    var expanded by remember { mutableStateOf(false) }
    val rotation by animateFloatAsState(if (expanded) 180f else 0f, label = "arrowRotation")

    Card(
        colors    = CardDefaults.cardColors(containerColor = cardBg),
        shape     = RoundedCornerShape(22.dp),
        elevation = CardDefaults.cardElevation(0.dp),
        modifier  = Modifier
            .fillMaxWidth()
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = { expanded = !expanded })
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = when (postPillState) {
                        PostPillState.DISCOVERY -> "What is Discovery Mode?"
                        PostPillState.LEARNING  -> "What is Learning Mode?"
                        PostPillState.NORMAL    -> "Life after the pill"
                    },
                    fontFamily = BricolageGrotesque,
                    color      = textPrimary,
                    fontSize   = SIZE_LG,
                    fontWeight = FontWeight.SemiBold,
                    modifier   = Modifier.weight(1f)
                )
                Icon(
                    Icons.Default.KeyboardArrowDown, null,
                    tint     = textPrimary.copy(alpha = 0.6f),
                    modifier = Modifier.size(24.dp).graphicsLayer { rotationZ = rotation }
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column {
                    Spacer(Modifier.height(18.dp))
                    when (postPillState) {
                        PostPillState.DISCOVERY -> {
                            InfoRow("Why predictions are paused", "Hormonal birth control suppresses ovulation. After stopping, your body needs to restart signaling — this can take weeks to months.", textPrimary, textSub)
                            Spacer(Modifier.height(16.dp))
                            InfoRow("How predictions unlock", "Log your first natural period after stopping. Once you have it, the app enters Learning Mode and predictions go live.", textPrimary, textSub)
                            Spacer(Modifier.height(16.dp))
                            InfoRow("What to expect", "Your first few cycles may be irregular. Spotting is common, and your body typically settles within 3 to 6 months.", textPrimary, textSub)
                        }
                        PostPillState.LEARNING -> {
                            InfoRow("Predictions are live", "The app has enough data to make predictions, but treat them as estimates until we learn your new natural rhythm.", textPrimary, textSub)
                            Spacer(Modifier.height(16.dp))
                            InfoRow("How to reach full accuracy", "Log at least 4 natural cycles after stopping. This gives the algorithm a solid baseline to work with.", textPrimary, textSub)
                        }
                        PostPillState.NORMAL -> {
                            InfoRow("Pill Pack Tracking", "The app tracks your active pills and breaks to help you stay consistent with your specific pack settings.", textPrimary, textSub)
                            Spacer(Modifier.height(16.dp))
                            InfoRow("Stopping the Pill", "If you stop tracking, the app enters Discovery Mode to observe your body's natural rhythm as it restabilizes.", textPrimary, textSub)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoRow(title: String, body: String, textPrimary: Color, textSub: Color) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text       = title,
            fontFamily = BricolageGrotesque,
            fontWeight = FontWeight.SemiBold,
            fontSize   = SIZE_MD,
            color      = textPrimary
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text       = body,
            fontFamily = BricolageGrotesque,
            fontSize   = SIZE_SM,
            color      = textSub,
            lineHeight = 20.sp
        )
    }
}

@Composable
fun PillHistoryItem(
    pack: PeriodViewModel.PillPack,
    cardBg: Color,
    textPrimary: Color,
    textSub: Color,
    pillBackground: Color,
    activeAccent: Color,
    normalAccent: Color,
    isSwiping: Boolean = false
) {
    Card(
        shape     = RoundedCornerShape(22.dp),
        colors    = CardDefaults.cardColors(containerColor = cardBg),
        modifier  = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier         = Modifier.size(42.dp).clip(CircleShape).background(pillBackground),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector        = if (pack.endDate == null) Icons.Rounded.Medication else Icons.Rounded.History,
                    contentDescription = null,
                    tint               = if (pack.endDate == null) activeAccent else normalAccent,
                    modifier           = Modifier.size(22.dp)
                )
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text       = if (pack.endDate == null) "Active Pack" else "Completed Pack",
                    fontFamily = BricolageGrotesque,
                    fontWeight = FontWeight.SemiBold,
                    fontSize   = SIZE_LG,
                    color      = if (pack.endDate == null) activeAccent else textPrimary
                )
                Text(
                    text       = "${pack.startDate.pretty()} — ${pack.endDate?.pretty() ?: "Ongoing"}",
                    fontFamily = BricolageGrotesque,
                    fontSize   = SIZE_SM,
                    color      = textSub
                )
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(pillBackground)
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    "${pack.pillCount} pills",
                    fontFamily = BricolageGrotesque,
                    fontSize   = SIZE_XXS,
                    color      = textSub
                )
            }
        }
    }
}