package com.ben.periodt.ui.calendar.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ben.periodt.prediction.PostPillState
import com.ben.periodt.prediction.Prediction
import com.ben.periodt.prediction.getPostPillState
import com.ben.periodt.prediction.pretty
import com.ben.periodt.ui.theme.BricolageGrotesque
import com.ben.periodt.ui.theme.LocalAppIsDark
import com.ben.periodt.viewmodel.PeriodViewModel
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlin.math.abs

private val SIZE_XXS = 11.sp
private val SIZE_XS  = 12.sp
private val SIZE_SM  = 13.sp
private val SIZE_LG  = 15.sp

private val activeCycleMessages = listOf("Focus on self-care and hydration today.", "Warm baths and rest go a long way right now.", "Your body is doing a lot — be gentle with yourself.", "A heating pad and something comforting sounds right.", "This is a great time to slow down and recharge.", "Listen to what your body needs today.", "Dark chocolate counts as self-care. 🍫", "Rest is productive. You're allowed to take it easy.", "Check in with yourself — how are you feeling today?", "Hydrate, rest, repeat. You've got this. 💪")
private val naturalFlowMessages = listOf("Enjoy your natural flow. ✨", "Your cycle is doing its thing. ✨", "A calm phase — make the most of it. 🌿", "Good things ahead. Keep logging for better predictions.", "You're in a great window right now. 🌸", "Feeling yourself? This phase tends to be the best. ✨", "Your body is in rhythm. Stay consistent. 🌿", "The quiet before the storm — rest up and enjoy! ☀️", "Track how you feel today — patterns matter. 📊", "Energy up? Use it well. ✨")
private val pillFlowMessages    = listOf("Stay consistent with your pack! 💊", "One pill a day keeps the guesswork away. 💊", "Consistency is key — keep it up! ✨", "On track with your pack. Great work! 💊", "Remember to take your pill at the same time each day.", "Staying consistent helps your body stay regulated. 💊", "You're doing great — keep the streak going! ✨", "Same time every day is the goal. You've got this! 💊", "Your pack is on track. Stay consistent! 🌿", "Pill taken? Check. You're doing amazing. ✨")

@Composable
fun PredictionBanner(
    prediction: Prediction?, cycles: List<PeriodViewModel.Cycle>,
    isTransitioning: Boolean, isOnPill: Boolean, pillStopDate: LocalDate?,
    pillPackStartDate: LocalDate? = null, pillPackCount: Int = 21
) {
    val today = LocalDate.now()
    val now   = LocalDateTime.now()
    val isDark = LocalAppIsDark.current

    val packEndDate = remember(pillPackStartDate, pillPackCount) {
        pillPackStartDate?.plusDays((pillPackCount - 1).toLong())
    }
    val endFormatter = remember { DateTimeFormatter.ofPattern("MMM dd") }
    val postPillCycles = remember(cycles, pillStopDate) {
        if (pillStopDate != null) cycles.filter { !it.startDate.isBefore(pillStopDate) } else emptyList()
    }
    val postPillState = remember(postPillCycles, isOnPill, pillStopDate) {
        if (isOnPill || pillStopDate == null) PostPillState.NORMAL else getPostPillState(postPillCycles)
    }
    val activeCycle = cycles.firstOrNull { it.endDate == null || (today >= it.startDate && today <= it.endDate) }

    if (prediction == null && postPillState == PostPillState.NORMAL && activeCycle == null) return

    val cardBackground = if (isDark) Color(0xFF1B1B1B).copy(alpha = 0.5f) else Color.White
    val textPrimary    = if (isDark) Color.White else Color(0xFF0F172A)
    val textSecondary  = if (isDark) Color.White.copy(alpha = 0.6f) else Color(0xFF1b1b1b).copy(alpha = 0.6f)
    val pillBackground = remember(isDark, isOnPill) {
        when { isOnPill && isDark -> Color(0xFFa68e74).copy(alpha = 0.15f); isOnPill -> Color(0xFFa68e74).copy(alpha = 0.10f); isDark -> Color(0xFFD89046).copy(alpha = 0.15f); else -> Color(0xFFa5bda3).copy(alpha = 0.15f) }
    }
    val progressBrush  = remember(isDark, isOnPill) {
        val color = when { isOnPill -> Color(0xFFa68e74); isDark -> Color(0xFFD89046); else -> Color(0xFFa5bda3) }
        Brush.linearGradient(colors = listOf(color, color))
    }

    val icon: ImageVector; val accentColor: Color; val statusTitle: String; val personalMessage: String; val dateBadgeText: String
    var progTarget by remember { mutableFloatStateOf(0f) }
    var compliment by remember { mutableStateOf("") }

    if (activeCycle != null) {
        val dayOfPeriod  = ChronoUnit.DAYS.between(activeCycle.startDate, today).toInt() + 1
        val pillDayIndex = if (pillPackStartDate != null) (today.toEpochDay() - pillPackStartDate.toEpochDay()).toInt().coerceAtLeast(0) else today.toEpochDay().toInt()
        val label        = if (isOnPill) "Withdrawal Bleed" else "Period"
        icon             = Icons.Rounded.Favorite
        accentColor      = if (isOnPill) Color(0xFFa68e74) else Color(0xFFEF5350)
        statusTitle      = "$label Day $dayOfPeriod"
        personalMessage  = when (postPillState) {
            PostPillState.DISCOVERY -> "First cycle after stopping pills — recalibrating. 🌿"
            PostPillState.LEARNING  -> "Learning your natural rhythm. Keep logging! 🌿"
            else -> if (isOnPill) pillFlowMessages[pillDayIndex % pillFlowMessages.size] else activeCycleMessages[(dayOfPeriod - 1) % activeCycleMessages.size]
        }
        dateBadgeText    = when { isOnPill -> "Pill Pack"; postPillState == PostPillState.DISCOVERY -> "Discovery"; postPillState == PostPillState.LEARNING -> "Learning"; else -> "Active" }
        val totalDays    = activeCycle.endDate?.let { ChronoUnit.DAYS.between(activeCycle.startDate, it) + 1 } ?: 6L
        progTarget       = (ChronoUnit.MINUTES.between(activeCycle.startDate.atStartOfDay(), now).toFloat() / (totalDays * 1440f)).coerceIn(0f, 0.95f)
    } else if (postPillState == PostPillState.DISCOVERY) {
        icon = Icons.Rounded.AutoAwesome; accentColor = if (isDark) Color(0xFF8089D2) else Color(0xFF2C3F70)
        statusTitle = "Discovery Mode"; personalMessage = "Predictions are paused while recalibrating."; dateBadgeText = "Paused"
    } else if (postPillState == PostPillState.LEARNING) {
        icon = Icons.Rounded.AutoAwesome; accentColor = if (isDark) Color(0xFF8089D2) else Color(0xFF2C3F70)
        statusTitle = "Learning Mode"; personalMessage = "Predictions are active, but we're still refining accuracy."
        dateBadgeText = prediction?.mostLikelyPeriodStart?.pretty() ?: "Learning"
    } else if (prediction == null) {
        icon = Icons.Rounded.AutoAwesome; accentColor = if (isDark) Color(0xFFD89046) else Color(0xFFa5bda3)
        statusTitle = "Learning your rhythm"; personalMessage = "Keep tracking to unlock predictions."; dateBadgeText = "Learning"
    } else {
        val daysUntil      = ChronoUnit.DAYS.between(today, prediction.mostLikelyPeriodStart)
        val cycleTypeLabel = if (isOnPill) "withdrawal bleed" else "cycle"
        val packInfo       = if (isOnPill && packEndDate != null) {
            when { today.isAfter(packEndDate) -> "Pack finished"; today.isEqual(packEndDate) -> "Last pill today"; else -> "Pack ends on ${packEndDate.format(endFormatter)}" }
        } else null
        val quad = when {
            daysUntil < 0   -> Quadruple(Icons.Rounded.Warning,   Color(0xFFEF5350), "Late by ${abs(daysUntil)} days", "No stress — cycles can shift! 🧘‍♀️")
            daysUntil == 0L -> Quadruple(Icons.Rounded.Favorite,  if (isOnPill) Color(0xFFa68e74) else if (isDark) Color(0xFFC8D4E5) else Color(0xFF8089D2), "Starts today", if (isOnPill) "Withdrawal bleed expected today." else "Ready for your period? 🍫")
            daysUntil <= 3  -> Quadruple(Icons.Rounded.Bolt,      Color(0xFFFFB74D), "Almost time", "Rest up and stay cozy. 💧")
            else            -> {
                val message = if (packInfo != null) "$packInfo • ${pillFlowMessages[today.dayOfYear % pillFlowMessages.size]}" else naturalFlowMessages[today.dayOfYear % naturalFlowMessages.size]
                Quadruple(Icons.Rounded.Spa, if (isOnPill) Color(0xFFa68e74) else if (isDark) Color(0xFFD89046) else Color(0xFFa5bda3), "$daysUntil days until next $cycleTypeLabel", message)
            }
        }
        icon = quad.first; accentColor = quad.second; statusTitle = quad.third; personalMessage = quad.fourth
        dateBadgeText = prediction.mostLikelyPeriodStart.pretty()
    }

    val animatedProgress by animateFloatAsState(targetValue = progTarget, animationSpec = tween(1200), label = "BannerProgress")

    Card(
        shape     = RoundedCornerShape(22.dp),
        colors    = CardDefaults.cardColors(containerColor = cardBackground),
        modifier  = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(24.dp))
                Box(modifier = Modifier.clip(RoundedCornerShape(50)).background(accentColor.copy(alpha = 0.15f)).padding(horizontal = 12.dp, vertical = 6.dp)) {
                    Text(
                        text       = dateBadgeText,
                        fontFamily = BricolageGrotesque,
                        color      = accentColor,
                        fontWeight = FontWeight.SemiBold,
                        fontSize   = SIZE_XXS
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = statusTitle,     fontFamily = BricolageGrotesque, fontWeight = FontWeight.SemiBold, color = textPrimary,   fontSize = SIZE_LG)
            Text(text = personalMessage, fontFamily = BricolageGrotesque,                                  color = textSecondary, fontSize = SIZE_SM)

            AnimatedVisibility(visible = activeCycle != null) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Spacer(Modifier.height(16.dp))
                    Canvas(Modifier.fillMaxWidth().height(6.dp).clip(CircleShape)) {
                        drawRoundRect(color = pillBackground, size = size, cornerRadius = CornerRadius(50f))
                        drawRoundRect(brush = progressBrush, size = Size(animatedProgress * size.width, size.height), cornerRadius = CornerRadius(50f))
                    }
                    if (compliment.isNotEmpty()) {
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text       = compliment,
                            fontFamily = BricolageGrotesque,
                            style      = androidx.compose.ui.text.TextStyle(brush = progressBrush, fontSize = SIZE_LG, fontWeight = FontWeight.Medium)
                        )
                    }
                }
            }
        }
    }
}

private val bleedingDo   = listOf("Take it slow and rest up.", "Curl up with something cozy.", "Give yourself permission to do nothing.", "A warm bath can do wonders today.", "Journal how you're feeling today.", "Low effort, high reward — rest wins.", "Cancel what you can. Rest first.")
private val bleedingMove = listOf("Try gentle stretching or yoga.", "A slow walk outside is enough.", "Restorative yoga is perfect right now.", "Light stretching before bed tonight.", "Child's pose. That's it. That's enough.", "Breathwork counts as movement today.", "Gentle mobility work if you have energy.")
private val bleedingEat  = listOf("Comfort food rich in iron.", "Dark leafy greens and lentils today.", "Warm soups are your best friend.", "Iron-rich foods help with fatigue.", "Magnesium-rich foods ease cramps.", "Bone broth or a hearty stew.", "Dark chocolate has iron — treat yourself.")
private val follicularDo   = listOf("Plan new goals or projects.", "Start that thing you've been putting off.", "Your focus is sharp — use it.", "Write down your intentions for the month.", "Great time to learn something new.", "Energy is building — lean into it.", "Tackle your to-do list with confidence.")
private val follicularMove = listOf("Go for a run or a hike.", "Try a new workout class today.", "Cardio feels easier this week — use it.", "Push a little harder than usual.", "HIIT, cycling, or a long run all work.", "Your body is primed for challenge.", "Set a new personal record today.")
private val follicularEat  = listOf("Fresh salads and protein.", "Lean protein fuels your rising energy.", "Colorful vegetables are your best bet.", "Fermented foods support your gut now.", "Light and fresh keeps energy steady.", "Eggs, legumes, and greens are great.", "Antioxidant-rich foods are perfect now.")
private val ovulationDo    = listOf("Connect with your friends.", "Say yes to social plans today.", "Your confidence is peaking — own it.", "Great day for an important conversation.", "Collaborate, pitch, present — you've got this.", "Reach out to someone you've been meaning to.", "Charisma is up. Use it wisely. ✨")
private val ovulationMove  = listOf("Push limits with a workout.", "Your strength is at its peak today.", "HIIT, lifting, or dancing — all great.", "High-intensity feels good this week.", "Try something physically challenging.", "Spin class, climbing, or sprints.", "Your body can handle more right now.")
private val ovulationEat   = listOf("Light meals keep you going.", "Anti-inflammatory foods support ovulation.", "Raw veggies and lean proteins today.", "Zinc-rich foods are great right now.", "Hydrate well — your body needs it.", "Fibre-rich foods keep things balanced.", "Whole grains and fresh fruit are ideal.")
private val lutealDo       = listOf("Tidy up your personal space.", "Nesting mode is valid and productive.", "Wind down your schedule a little.", "Reflect on the month — what worked?", "Creative, low-key activities suit you now.", "Great time for a digital detox evening.", "Prep meals for the week ahead.")
private val lutealMove     = listOf("Pilates or strength training.", "Lower intensity feels better this week.", "A long walk clears the mind.", "Swimming or cycling are great options.", "Yoga and stretching suit this phase.", "Listen to your energy and adjust.", "Moderate movement supports your mood.")
private val lutealEat      = listOf("Complex carbs stabilize mood.", "Magnesium helps with PMS symptoms.", "Whole grains and root vegetables help.", "Reduce caffeine and sugar if you can.", "Omega-3s support mood this phase.", "Warm, nourishing meals are ideal.", "Dark chocolate for magnesium. 🍫")
private val defaultDo      = listOf("Take some time to unwind today.", "A moment of stillness goes a long way.", "Check in with yourself today.", "Do one thing that brings you joy.", "Rest and intention go hand in hand.", "Be kind to yourself today.", "Small acts of self-care add up.")
private val defaultMove    = listOf("A gentle walk is perfect.", "Movement is medicine — any amount counts.", "Stretch for 10 minutes today.", "Fresh air and a short walk.", "Even 5 minutes of movement helps.", "Put on music and move freely.", "Your pace is the right pace.")
private val defaultEat     = listOf("Stay hydrated and drink water.", "Whole foods over processed today.", "A nourishing meal changes everything.", "Eat something colourful today.", "Slow down and enjoy your food.", "Hydration is self-care.", "Listen to what your body is craving.")

@Composable
fun WellnessCardsRow(cycles: List<PeriodViewModel.Cycle>, prediction: Prediction?) {
    val today     = LocalDate.now()
    val lastCycle = cycles.maxByOrNull { it.startDate }
    var doList    = defaultDo;   var moveList = defaultMove; var eatList = defaultEat
    var doIcon    = Icons.Rounded.SelfImprovement; var moveIcon = Icons.Rounded.DirectionsWalk; var eatIcon = Icons.Rounded.LocalCafe

    val isBleeding    = lastCycle != null && (lastCycle.endDate == null || today <= lastCycle.endDate)
    val daysSinceStart = lastCycle?.let { ChronoUnit.DAYS.between(it.startDate, today).toInt() } ?: 0
    val cardIndex     = if (isBleeding) daysSinceStart else today.toEpochDay().toInt()
    val isDark = LocalAppIsDark.current

    if (lastCycle != null) {
        when {
            isBleeding               -> { doList = bleedingDo;   doIcon = Icons.Rounded.Bedtime;      moveList = bleedingMove;   moveIcon = Icons.Rounded.SelfImprovement; eatList = bleedingEat;   eatIcon = Icons.Rounded.SoupKitchen }
            daysSinceStart in 6..13  -> { doList = follicularDo; doIcon = Icons.Rounded.Checklist;    moveList = follicularMove; moveIcon = Icons.Rounded.DirectionsRun;   eatList = follicularEat; eatIcon = Icons.Rounded.Restaurant }
            daysSinceStart in 14..17 -> { doList = ovulationDo;  doIcon = Icons.Rounded.Favorite;     moveList = ovulationMove;  moveIcon = Icons.Rounded.FitnessCenter;   eatList = ovulationEat;  eatIcon = Icons.Rounded.Tapas }
            daysSinceStart in 18..28 -> { doList = lutealDo;     doIcon = Icons.Rounded.AutoAwesome;  moveList = lutealMove;     moveIcon = Icons.Rounded.SelfImprovement; eatList = lutealEat;     eatIcon = Icons.Rounded.Grain }
        }
    }

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        WellnessCardItem(title = "Do",   content = doList[cardIndex % doList.size],     icon = doIcon,   backgroundColor = if (isDark) Color(0xFF42553f) else Color(0xFFa5bda3), modifier = Modifier.weight(1f))
        WellnessCardItem(title = "Move", content = moveList[cardIndex % moveList.size], icon = moveIcon, backgroundColor = Color(0xFFD89046),                    modifier = Modifier.weight(1f))
        WellnessCardItem(title = "Eat",  content = eatList[cardIndex % eatList.size],   icon = eatIcon,  backgroundColor = Color(0xFFa68e74),                    modifier = Modifier.weight(1f))
    }
}

@Composable
fun WellnessCardItem(title: String, content: String, icon: ImageVector, backgroundColor: Color, modifier: Modifier = Modifier) {
    Card(modifier = modifier.height(165.dp), shape = RoundedCornerShape(26.dp), colors = CardDefaults.cardColors(containerColor = backgroundColor), elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)) {
        Box(modifier = Modifier.fillMaxSize().background(backgroundColor).padding(16.dp), contentAlignment = Alignment.Center) {
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(imageVector = icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                Spacer(Modifier.height(8.dp))
                Text(
                    text          = title.uppercase(),
                    fontFamily    = BricolageGrotesque,
                    color         = Color.White.copy(alpha = 0.8f),
                    fontSize      = SIZE_XXS,
                    fontWeight    = FontWeight.SemiBold,
                    letterSpacing = 1.sp
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text       = content,
                    fontFamily = BricolageGrotesque,
                    color      = Color.White,
                    textAlign  = TextAlign.Center,
                    lineHeight = 18.sp,
                    fontSize   = SIZE_XS
                )
            }
        }
    }
}

data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)