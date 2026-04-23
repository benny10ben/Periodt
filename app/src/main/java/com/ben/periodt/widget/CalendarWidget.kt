package com.ben.periodt.widget

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.*
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.*
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.color.ColorProvider
import androidx.glance.layout.*
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.ben.periodt.R
import com.ben.periodt.data.AppDatabase
import com.ben.periodt.data.PeriodCycleEntity
import com.ben.periodt.data.PillPackEntity
import com.ben.periodt.data.ProfileEntity
import com.ben.periodt.ui.settings.components.THEME_MODE_KEY
import com.ben.periodt.ui.settings.components.ThemeMode
import com.ben.periodt.viewmodel.PeriodViewModel
import com.ben.periodt.prediction.PostPillState
import com.ben.periodt.prediction.Prediction
import com.ben.periodt.reminder.dataStore
import com.ben.periodt.prediction.getPostPillState
import com.ben.periodt.prediction.predictCycle
import com.google.gson.Gson
import kotlinx.coroutines.flow.firstOrNull
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

val MonthOffsetKey = intPreferencesKey("calendar_month_offset")
val ProfileIdKey = intPreferencesKey("widget_profile_id")
val ThemeModeWidgetKey = stringPreferencesKey("widget_theme_mode")

val ProfilesJsonKey = stringPreferencesKey("widget_profiles_json")
val CyclesJsonKey = stringPreferencesKey("widget_cycles_json")
val PillsJsonKey = stringPreferencesKey("widget_pills_json")

@SuppressLint("RestrictedApi")
private fun themeColorProvider(light: Color, dark: Color, mode: ThemeMode): ColorProvider {
    return when (mode) {
        ThemeMode.LIGHT -> ColorProvider(light, light)
        ThemeMode.DARK -> ColorProvider(dark, dark)
        ThemeMode.SYSTEM -> ColorProvider(day = light, night = dark)
    }
}

@SuppressLint("RestrictedApi")
private fun colorProvider(color: Color): ColorProvider = ColorProvider(color)

private fun PeriodCycleEntity.toDomain(): PeriodViewModel.Cycle {
    return PeriodViewModel.Cycle(
        id = id,
        startDate = LocalDate.parse(startDate),
        endDate = endDate?.takeIf { it.isNotBlank() }?.let { LocalDate.parse(it) },
        bleeding = bleeding,
        bloodColor = bloodColor,
        painLevel = painLevel
    )
}

class CalendarWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = CalendarWidget()
}

class CalendarWidget : GlanceAppWidget() {
    override val stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition

    companion object {
        suspend fun refreshAll(context: Context) {
            val manager = GlanceAppWidgetManager(context)
            val ids = manager.getGlanceIds(CalendarWidget::class.java)
            val gson = Gson()

            // 1. Fetch fresh Data from DB
            val dao = AppDatabase.getDatabase(context).periodCycleDao()
            val freshProfiles = dao.getAllProfilesOnce()
            val freshCycles = dao.getAllCyclesOnce()
            val freshPills = dao.getAllPillPacksOnce()

            // 2. Fetch fresh Theme from DataStore
            val themePrefs = context.dataStore.data.firstOrNull()
            val themeModeStr = themePrefs?.get(THEME_MODE_KEY) ?: ThemeMode.SYSTEM.name

            // 3. Write EVERYTHING into the widget's internal reactive state
            ids.forEach { id ->
                updateAppWidgetState(context, id) { prefs ->
                    prefs[ThemeModeWidgetKey] = themeModeStr
                    prefs[ProfilesJsonKey] = gson.toJson(freshProfiles)
                    prefs[CyclesJsonKey] = gson.toJson(freshCycles)
                    prefs[PillsJsonKey] = gson.toJson(freshPills)
                }
            }

            CalendarWidget().updateAll(context)
        }
    }

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        // Initial fallback fetch (used only when the widget is placed for the very first time)
        val dao = AppDatabase.getDatabase(context).periodCycleDao()
        val initialProfiles = dao.getAllProfilesOnce()
        val initialCycles = dao.getAllCyclesOnce()
        val initialPills = dao.getAllPillPacksOnce()

        provideContent {
            val prefs = currentState<Preferences>()
            val gson = Gson()

            // ✨ Read reactive Theme
            val themeModeStr = prefs[ThemeModeWidgetKey] ?: ThemeMode.SYSTEM.name
            val themeMode = try { ThemeMode.valueOf(themeModeStr) } catch (e: Exception) { ThemeMode.SYSTEM }

            // ✨ Read reactive Data (with fallback to initial fetch)
            val profilesStr = prefs[ProfilesJsonKey]
            val activeProfiles = if (profilesStr != null) gson.fromJson(profilesStr, Array<ProfileEntity>::class.java).toList() else initialProfiles

            val cyclesStr = prefs[CyclesJsonKey]
            val activeCycles = if (cyclesStr != null) gson.fromJson(cyclesStr, Array<PeriodCycleEntity>::class.java).toList() else initialCycles

            val pillsStr = prefs[PillsJsonKey]
            val activePills = if (pillsStr != null) gson.fromJson(pillsStr, Array<PillPackEntity>::class.java).toList() else initialPills

            val profileId = prefs[ProfileIdKey]

            if (profileId == null) {
                ProfileSelectionContent(activeProfiles, themeMode)
            } else {
                val cyclesEntities = activeCycles.filter { it.profileId == profileId }
                val pills = activePills.filter { it.profileId == profileId }
                val domainCycles = cyclesEntities.map { it.toDomain() }

                val activePack = pills.firstOrNull { it.endDate == null }
                val stopDateStr = pills.filter { it.endDate != null }.maxByOrNull { it.endDate!! }?.endDate
                val stopDate = stopDateStr?.let { LocalDate.parse(it) }

                val isTransitioning = if (activePack != null || stopDate == null) false else {
                    val postPillCycles = domainCycles.filter { !it.startDate.isBefore(stopDate) }
                    getPostPillState(postPillCycles) == PostPillState.DISCOVERY
                }

                val prediction: Prediction? = when {
                    activePack != null -> null
                    isTransitioning -> null
                    else -> {
                        val validCycles = if (stopDate != null) domainCycles.filter { !it.startDate.isBefore(stopDate) } else domainCycles
                        predictCycle(validCycles)
                    }
                }

                val monthOffset = prefs[MonthOffsetKey] ?: 0
                val displayMonth = YearMonth.now().plusMonths(monthOffset.toLong())

                CalendarWidgetContent(context, displayMonth, cyclesEntities, pills, prediction, themeMode)
            }
        }
    }

    @Composable
    private fun ProfileSelectionContent(profiles: List<ProfileEntity>, themeMode: ThemeMode) {
        val textPrimaryProvider = themeColorProvider(Color(0xFF0F172A), Color(0xFFFFFFFF), themeMode)
        val rowBgProvider = themeColorProvider(Color(0xFFF1F5F9), Color(0xFF252525), themeMode)
        val widgetBackground = themeColorProvider(Color.White, Color.Black, themeMode)

        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .appWidgetBackground()
                .background(widgetBackground)
                .cornerRadius(24.dp)
                .padding(20.dp)
        ) {
            Text(
                text = "Select Profile",
                style = TextStyle(color = textPrimaryProvider, fontSize = 20.sp, fontWeight = FontWeight.Bold),
                modifier = GlanceModifier.padding(bottom = 16.dp).fillMaxWidth()
            )

            LazyColumn(modifier = GlanceModifier.fillMaxSize()) {
                items(profiles) { profile ->
                    Column(modifier = GlanceModifier.fillMaxWidth().padding(bottom = 10.dp)) {
                        Row(
                            modifier = GlanceModifier
                                .fillMaxWidth()
                                .background(rowBgProvider)
                                .cornerRadius(16.dp)
                                .clickable(actionRunCallback<SelectProfileAction>(actionParametersOf(SelectProfileAction.ProfileIdParam to profile.id)))
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val avatarRes = when(profile.avatarColor) {
                                "avatar_1" -> R.drawable.avatar_1
                                "avatar_2" -> R.drawable.avatar_2
                                "avatar_3" -> R.drawable.avatar_3
                                "avatar_4" -> R.drawable.avatar_4
                                "avatar_5" -> R.drawable.avatar_5
                                "avatar_6" -> R.drawable.avatar_6
                                "avatar_7" -> R.drawable.avatar_7
                                "avatar_8" -> R.drawable.avatar_8
                                else -> 0
                            }

                            if (avatarRes != 0) {
                                Image(
                                    provider = ImageProvider(avatarRes),
                                    contentDescription = null,
                                    modifier = GlanceModifier.size(32.dp).cornerRadius(100.dp)
                                )
                                Spacer(GlanceModifier.width(12.dp))
                            }

                            Text(
                                text = profile.name,
                                style = TextStyle(color = textPrimaryProvider, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                            )
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun CalendarWidgetContent(
        context: Context,
        displayMonth: YearMonth,
        cycles: List<PeriodCycleEntity>,
        pills: List<PillPackEntity>,
        prediction: Prediction?,
        themeMode: ThemeMode
    ) {
        val textPrimaryProvider = themeColorProvider(Color(0xFF0F172A), Color(0xFFFFFFFF), themeMode)
        val textMutedProvider = themeColorProvider(Color(0xFF64748B), Color(0xFFFFFFFF).copy(alpha = 0.6f), themeMode)
        val widgetBackground = themeColorProvider(Color.White, Color.Black, themeMode)

        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .appWidgetBackground()
                .background(widgetBackground)
                .cornerRadius(24.dp)
                .padding(horizontal = 16.dp, vertical = 16.dp)
                .clickable(actionStartActivity(ComponentName(context, "com.ben.periodt.MainActivity")))
        ) {
            Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = displayMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
                    modifier = GlanceModifier.defaultWeight().padding(start = 10.dp),
                    style = TextStyle(color = textPrimaryProvider, fontSize = 18.sp, fontWeight = FontWeight.Medium)
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "‹",
                        modifier = GlanceModifier.padding(horizontal = 16.dp, vertical = 4.dp)
                            .clickable(actionRunCallback<UpdateMonthAction>(actionParametersOf(UpdateMonthAction.IncrementKey to -1))),
                        style = TextStyle(fontSize = 24.sp, color = textMutedProvider)
                    )
                    Text(
                        text = "›",
                        modifier = GlanceModifier.padding(horizontal = 16.dp, vertical = 4.dp)
                            .clickable(actionRunCallback<UpdateMonthAction>(actionParametersOf(UpdateMonthAction.IncrementKey to 1))),
                        style = TextStyle(fontSize = 24.sp, color = textMutedProvider)
                    )
                }
            }

            Spacer(GlanceModifier.height(16.dp))

            Row(modifier = GlanceModifier.fillMaxWidth()) {
                listOf("S", "M", "T", "W", "T", "F", "S").forEach { day ->
                    Text(
                        text = day,
                        modifier = GlanceModifier.defaultWeight(),
                        style = TextStyle(color = textMutedProvider, fontSize = 12.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center)
                    )
                }
            }

            Spacer(GlanceModifier.height(8.dp))

            val firstDay = displayMonth.atDay(1)
            val startOffset = firstDay.dayOfWeek.value % 7
            var cursor = firstDay.minusDays(startOffset.toLong())

            Column(modifier = GlanceModifier.fillMaxWidth().defaultWeight()) {
                repeat(6) {
                    Row(modifier = GlanceModifier.fillMaxWidth().defaultWeight()) {
                        repeat(7) {
                            DayCellGlance(
                                modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
                                date = cursor,
                                displayMonth = displayMonth,
                                cycles = cycles,
                                pills = pills,
                                prediction = prediction,
                                themeMode = themeMode,
                                textPrimaryProvider = textPrimaryProvider
                            )
                            cursor = cursor.plusDays(1)
                        }
                    }
                }
            }
        }
    }
}

@SuppressLint("RestrictedApi")
@Composable
private fun DayCellGlance(
    modifier: GlanceModifier,
    date: LocalDate,
    displayMonth: YearMonth,
    cycles: List<PeriodCycleEntity>,
    pills: List<PillPackEntity>,
    prediction: Prediction?,
    themeMode: ThemeMode,
    textPrimaryProvider: ColorProvider
) {
    val phase = getPhase(date, cycles, pills, prediction)
    val prevPhase = getPhase(date.minusDays(1), cycles, pills, prediction)
    val nextPhase = getPhase(date.plusDays(1), cycles, pills, prediction)
    val isStart = phase != prevPhase
    val isEnd = phase != nextPhase
    val isCurrentMonth = date.month == displayMonth.month
    val isToday = date == LocalDate.now()

    val loggedColor = themeColorProvider(Color(0xFFCA7B77), Color(0xFF6E1F1C), themeMode)
    val upcomingColor = themeColorProvider(Color(0xFFA5231C), Color(0xFFA5231C), themeMode)
    val fertileColor = themeColorProvider(Color(0xFFA6BFA2), Color(0xFF4C6549), themeMode)
    val pillColor = themeColorProvider(Color(0xFFa68e74), Color(0xFFa68e74), themeMode)

    val today = LocalDate.now()
    val isPillPast = phase == 5 && date.isBefore(today)
    val pillFadedColor = themeColorProvider(Color(0xFFa68e74).copy(alpha = 0.2f), Color(0xFFa68e74).copy(alpha = 0.2f), themeMode)

    val bgColorProvider = when {
        phase == 1 -> loggedColor
        phase == 2 -> upcomingColor
        phase == 3 -> fertileColor
        phase == 5 && isPillPast -> pillFadedColor
        phase == 5 -> pillColor
        else -> ColorProvider(Color.Transparent)
    }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        if (phase != 0) {
            Box(
                modifier = GlanceModifier.fillMaxSize()
                    .padding(start = 0.dp, end = 0.dp, top = 4.dp, bottom = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(modifier = GlanceModifier.fillMaxSize().background(bgColorProvider).cornerRadius(100.dp)) {}
                Row(modifier = GlanceModifier.fillMaxSize()) {
                    Box(modifier = GlanceModifier.fillMaxHeight().defaultWeight().background(if (!isStart) bgColorProvider else colorProvider(Color.Transparent))) {}
                    Box(modifier = GlanceModifier.fillMaxHeight().defaultWeight().background(if (!isEnd) bgColorProvider else colorProvider(Color.Transparent))) {}
                }
            }
        }

        val finalTextColorProvider = when {
            phase != 0 -> colorProvider(Color.White)
            isCurrentMonth -> if (isToday) themeColorProvider(Color(0xFFD89046), Color(0xFFD89046), themeMode) else textPrimaryProvider
            else -> themeColorProvider(Color.Gray.copy(alpha = 0.5f), Color.Gray.copy(alpha = 0.5f), themeMode)
        }

        Text(
            text = date.dayOfMonth.toString(),
            style = TextStyle(color = finalTextColorProvider, fontSize = 14.sp, fontWeight = if (isToday || phase != 0) FontWeight.Bold else FontWeight.Normal, textAlign = TextAlign.Center)
        )
    }
}

private fun getPhase(d: LocalDate, cycles: List<PeriodCycleEntity>, pills: List<PillPackEntity>, prediction: Prediction?): Int {
    val isLogged = cycles.any { c ->
        val start = LocalDate.parse(c.startDate)
        val end = c.endDate?.takeIf { it.isNotBlank() }?.let { LocalDate.parse(it) } ?: start.plusDays(6)
        !d.isBefore(start) && !d.isAfter(end)
    }
    if (isLogged) return 1
    val inPill = pills.any { p ->
        val start = LocalDate.parse(p.startDate)
        val end = p.endDate?.takeIf { it.isNotBlank() }?.let { LocalDate.parse(it) } ?: start.plusDays((p.pillCount - 1).toLong())
        !d.isBefore(start) && !d.isAfter(end)
    }
    if (inPill) return 5
    if (prediction != null) {
        if (!d.isBefore(prediction.mostLikelyPeriodStart) && d.isBefore(prediction.mostLikelyPeriodStart.plusDays(prediction.periodLength.toLong()))) return 2
        if (prediction.fertileWindow.start != LocalDate.MIN && !d.isBefore(prediction.fertileWindow.start) && !d.isAfter(prediction.fertileWindow.endInclusive)) return 3
    }
    return 0
}

class UpdateMonthAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val increment = parameters[IncrementKey] ?: 0
        updateAppWidgetState(context, glanceId) { prefs ->
            prefs[MonthOffsetKey] = (prefs[MonthOffsetKey] ?: 0) + increment
        }
        CalendarWidget().update(context, glanceId)
    }
    companion object { val IncrementKey = ActionParameters.Key<Int>("increment") }
}

class SelectProfileAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val profileId = parameters[ProfileIdParam] ?: return
        updateAppWidgetState(context, glanceId) { prefs ->
            prefs[ProfileIdKey] = profileId
        }
        CalendarWidget().update(context, glanceId)
    }

    companion object {
        val ProfileIdParam = ActionParameters.Key<Int>("profile_id")
    }
}