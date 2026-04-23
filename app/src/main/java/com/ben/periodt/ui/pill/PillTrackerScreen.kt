package com.ben.periodt.ui.pill

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Medication
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ben.periodt.prediction.PostPillState
import com.ben.periodt.ui.calendar.components.SwipeToDeleteCard
import com.ben.periodt.ui.pill.components.ActivePackSection
import com.ben.periodt.ui.pill.components.LifeAfterPillCard
import com.ben.periodt.ui.pill.components.PillHistoryItem
import com.ben.periodt.ui.theme.BricolageGrotesque
import com.ben.periodt.ui.theme.LocalAppIsDark
import com.ben.periodt.viewmodel.PeriodViewModel

private val SIZE_MD = 14.sp
private val SIZE_LG = 15.sp
private val SIZE_XL = 20.sp

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
    val pillBackground = if (isDark) Color.White.copy(alpha = 0.1f) else Color(0xFFE8EBED).copy(alpha = 0.4f)

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
                    SwipeToDeleteCard(onDelete = { viewModel.deletePillPack(pack.id) }) { _ ->
                        PillHistoryItem(
                            pack           = pack,
                            cardBg         = cardBg,
                            textPrimary    = textPrimary,
                            textSub        = textSub,
                            pillBackground = pillBackground,
                            activeAccent   = themeAccent,
                            normalAccent   = textPrimary
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                }
            }
        }
    }
}