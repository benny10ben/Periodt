package com.ben.periodt.widget

import android.content.Context
import android.graphics.Color
import android.widget.RemoteViews
import com.ben.periodt.R
import com.ben.periodt.data.AppDatabase
import com.ben.periodt.uiux.shared.predictCycle
import com.ben.periodt.viewmodel.PeriodViewModel
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

object CalendarWidgetRenderer {

    // ── Palette (mirrors DayCellEnhanced exactly) ─────────────────────────
    private val COLOR_PERIOD_SOLID  = Color.parseColor("#A5231C")   // logged + predicted base
    private val COLOR_FERTILE_SOLID = Color.parseColor("#6d9567")   // fertile base (60 % alpha applied below)
    private val COLOR_PACK          = Color.parseColor("#a68e74")   // pill pack base

    // Alpha helpers
    private fun withAlpha(color: Int, alpha: Float): Int {
        val a = (alpha * 255).toInt().coerceIn(0, 255)
        return Color.argb(a, Color.red(color), Color.green(color), Color.blue(color))
    }

    // ── Status codes (align with DayCellEnhanced) ─────────────────────────
    // 0 = none | 1 = logged | 2 = predicted | 3 = fertile | 4 = pill-done | 5 = pill-upcoming

    suspend fun render(context: Context, widgetId: Int, year: Int, month: Int): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_calendar)
        val ym    = YearMonth.of(year, month)

        // ── 1. Data ────────────────────────────────────────────────────────
        val db  = AppDatabase.getDatabase(context)
        val dao = db.periodCycleDao()

        val rawCycles = dao.getAllCyclesOnce()
        val cycles = rawCycles.map { entity ->
            PeriodViewModel.Cycle(
                id         = entity.id,
                startDate  = LocalDate.parse(entity.startDate),
                endDate    = entity.endDate.takeIf { it.isNotBlank() }?.let { LocalDate.parse(it) },
                bleeding   = entity.bleeding,
                painLevel  = entity.painLevel,
                bloodColor = entity.bloodColor
            )
        }

        // Fetch pill packs via the same periodCycleDao (matches PeriodViewModel)
        val pillPacks: List<PeriodViewModel.PillPack> = runCatching {
            dao.getAllPillPacksOnce().map { entity ->
                PeriodViewModel.PillPack(
                    id        = entity.id,
                    startDate = LocalDate.parse(entity.startDate),
                    endDate   = entity.endDate?.takeIf { it.isNotBlank() }?.let { LocalDate.parse(it) },
                    pillCount = entity.pillCount
                )
            }
        }.getOrDefault(emptyList())

        val isOnPill  = pillPacks.isNotEmpty()
        val prediction = if (!isOnPill) predictCycle(cycles) else null

        // ── 2. Theme ───────────────────────────────────────────────────────
        // Read from SharedPreferences so it respects the in-app theme override,
        // not just the system setting. Falls back to system if not yet saved.
        val systemNightMode = (context.resources.configuration.uiMode and
                android.content.res.Configuration.UI_MODE_NIGHT_MASK) ==
                android.content.res.Configuration.UI_MODE_NIGHT_YES
        val isNightMode = context
            .getSharedPreferences("widget_prefs", android.content.Context.MODE_PRIVATE)
            .getBoolean("is_dark", systemNightMode)

        val colorPrimary = if (isNightMode) Color.WHITE else Color.BLACK
        val colorMuted   = if (isNightMode)
            Color.parseColor("#4DFFFFFF") else Color.parseColor("#4D000000")

        // ── 3. Widget background (dynamic — static XML can't react to theme) ──
        val bgColor = if (isNightMode) android.graphics.Color.parseColor("#1B1B1B")
        else            android.graphics.Color.parseColor("#FFFFFF")
        views.setInt(R.id.widget_root, "setBackgroundColor", bgColor)

        // ── 3. Header ──────────────────────────────────────────────────────
        views.setTextViewText(
            R.id.txt_month_year,
            ym.month.getDisplayName(TextStyle.FULL, Locale.getDefault()) + " " + ym.year
        )
        views.setTextColor(R.id.txt_month_year, colorPrimary)

        // ── 4. Weekday labels ──────────────────────────────────────────────
        val weekdayIds = intArrayOf(
            R.id.label_sun, R.id.label_mon, R.id.label_tue,
            R.id.label_wed, R.id.label_thu, R.id.label_fri, R.id.label_sat
        )
        for (id in weekdayIds) views.setTextColor(id, colorMuted)

        // ── 5. Status map ──────────────────────────────────────────────────
        val today      = LocalDate.now()
        val firstDay   = ym.atDay(1)
        val startOffset = firstDay.dayOfWeek.value % 7
        val gridStart  = firstDay.minusDays(startOffset.toLong())
        val gridEnd    = gridStart.plusDays(41)

        fun phaseOf(d: LocalDate): Int {
            // Priority 1: logged period
            val isLogged = cycles.any { c ->
                val end = c.endDate ?: c.startDate.plusDays(6)
                !d.isBefore(c.startDate) && !d.isAfter(end)
            }
            if (isLogged) return 1

            // Priority 2: pill pack window
            val matchingPack = pillPacks.firstOrNull { pack ->
                val end = pack.endDate ?: pack.startDate.plusDays((pack.pillCount - 1).toLong())
                !d.isBefore(pack.startDate) && !d.isAfter(end)
            }
            if (matchingPack != null) return if (d.isBefore(today)) 4 else 5

            // Priority 3: predicted / fertile (only when not on pill)
            if (prediction != null) {
                val s      = prediction.mostLikelyPeriodStart
                val length = prediction.periodLength?.toLong() ?: 5L
                if (!d.isBefore(s) && d.isBefore(s.plusDays(length))) return 2
                if (prediction.fertileWindow.start != LocalDate.MIN &&
                    prediction.fertileWindow.contains(d)) return 3
            }
            return 0
        }

        val statusMap = mutableMapOf<LocalDate, Int>()
        var mapCursor = gridStart
        while (!mapCursor.isAfter(gridEnd)) {
            statusMap[mapCursor] = phaseOf(mapCursor)
            mapCursor = mapCursor.plusDays(1)
        }

        // ── 6. Build grid ──────────────────────────────────────────────────
        views.removeAllViews(R.id.calendar_grid)

        var cursor = gridStart
        repeat(6) {
            val rowViews = RemoteViews(context.packageName, R.layout.widget_row_container)
            repeat(7) {
                val cell   = RemoteViews(context.packageName, R.layout.item_widget_day)
                val date   = cursor
                val status = statusMap[date] ?: 0
                val prev   = statusMap[date.minusDays(1)] ?: 0
                val next   = statusMap[date.plusDays(1)] ?: 0

                cell.setTextViewText(R.id.day_text, date.dayOfMonth.toString())

                // Group membership helpers (strips that flow into each other)
                val periodGroup = listOf(1, 2)   // logged + predicted share same strip group
                val pillGroup   = listOf(4, 5)   // pill-done + pill-upcoming share strip group

                val bgRes: Int = when (status) {
                    // ── Logged (pastel red ~60 %) ──────────────────────────
                    1 -> when {
                        prev !in periodGroup && next !in periodGroup -> R.drawable.cal_strip_pastel_single
                        prev !in periodGroup && next in  periodGroup -> R.drawable.cal_strip_pastel_start
                        prev in  periodGroup && next in  periodGroup -> R.drawable.cal_strip_pastel_middle
                        else                                          -> R.drawable.cal_strip_pastel_end
                    }
                    // ── Predicted (solid red) ──────────────────────────────
                    2 -> when {
                        prev !in periodGroup && next !in periodGroup -> R.drawable.cal_strip_single
                        prev !in periodGroup && next in  periodGroup -> R.drawable.cal_strip_start
                        prev in  periodGroup && next in  periodGroup -> R.drawable.cal_strip_middle
                        else                                          -> R.drawable.cal_strip_end
                    }
                    // ── Fertile (muted green, dark variant in night mode) ──
                    3 -> when {
                        prev != 3 && next != 3 -> if (isNightMode) R.drawable.cal_strip_fertile_single_dark else R.drawable.cal_strip_fertile_single
                        prev != 3 && next == 3 -> if (isNightMode) R.drawable.cal_strip_fertile_start_dark  else R.drawable.cal_strip_fertile_start
                        prev == 3 && next == 3 -> if (isNightMode) R.drawable.cal_strip_fertile_middle_dark else R.drawable.cal_strip_fertile_middle
                        else                   -> if (isNightMode) R.drawable.cal_strip_fertile_end_dark    else R.drawable.cal_strip_fertile_end
                    }
                    // ── Pill done (muted tan ~20 %) ────────────────────────
                    4 -> when {
                        prev !in pillGroup && next !in pillGroup -> R.drawable.cal_strip_pill_done_single
                        prev !in pillGroup && next in  pillGroup -> R.drawable.cal_strip_pill_done_start
                        prev in  pillGroup && next in  pillGroup -> R.drawable.cal_strip_pill_done_middle
                        else                                      -> R.drawable.cal_strip_pill_done_end
                    }
                    // ── Pill upcoming (solid tan) ──────────────────────────
                    5 -> when {
                        prev !in pillGroup && next !in pillGroup -> R.drawable.cal_strip_pill_single
                        prev !in pillGroup && next in  pillGroup -> R.drawable.cal_strip_pill_start
                        prev in  pillGroup && next in  pillGroup -> R.drawable.cal_strip_pill_middle
                        else                                      -> R.drawable.cal_strip_pill_end
                    }
                    else -> 0
                }

                // ── Strip on day_strip (full cell width → seamless) ───────
                if (bgRes != 0) {
                    cell.setImageViewResource(R.id.day_strip, bgRes)
                } else {
                    cell.setInt(R.id.day_strip, "setImageResource", 0)
                }

                // ── Text color (day_text background always transparent) ────
                val textColor = when {
                    bgRes != 0 && status == 1 -> Color.WHITE   // logged pastel
                    bgRes != 0 && status == 4 -> colorPrimary  // pill done muted
                    bgRes != 0                -> Color.WHITE   // predicted / fertile / pill upcoming
                    date.month == ym.month    -> colorPrimary
                    else                      -> colorMuted
                }
                cell.setTextColor(R.id.day_text, textColor)

                // ── Today ring — separate overlay view, always on top ──────
                // White when on a strip (any color bg), theme color when plain.
                if (date == today) {
                    val todayDrawable = if (bgRes != 0)
                        R.drawable.bg_widget_day_today_dark   // white ring on colored strip
                    else if (isNightMode)
                        R.drawable.bg_widget_day_today_dark   // white ring on dark bg
                    else
                        R.drawable.bg_widget_day_today_light  // black ring on light bg
                    cell.setViewVisibility(R.id.today_ring, android.view.View.VISIBLE)
                    cell.setImageViewResource(R.id.today_ring, todayDrawable)
                } else {
                    cell.setViewVisibility(R.id.today_ring, android.view.View.GONE)
                }

                rowViews.addView(R.id.row_inner, cell)
                cursor = cursor.plusDays(1)
            }
            views.addView(R.id.calendar_grid, rowViews)
        }

        // ── 7. Intents ─────────────────────────────────────────────────────
        views.setOnClickPendingIntent(R.id.btn_prev,    CalendarWidgetProvider.getNavigationIntent(context, widgetId, -1))
        views.setOnClickPendingIntent(R.id.btn_next,    CalendarWidgetProvider.getNavigationIntent(context, widgetId,  1))
        views.setOnClickPendingIntent(R.id.widget_root, CalendarWidgetProvider.getAppIntent(context))

        return views
    }
}