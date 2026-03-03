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

    suspend fun render(context: Context, widgetId: Int, year: Int, month: Int): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_calendar)
        val ym = YearMonth.of(year, month)

        // 1. Data Retrieval
        val dao = AppDatabase.getDatabase(context).periodCycleDao()

        // Fix: Use getAllCyclesOnce() if getAllCyclesNow() is missing from your DAO
        val rawCycles = dao.getAllCyclesOnce()

        val cycles = rawCycles.map { entity -> // Explicitly named 'entity' to fix 'it' errors
            PeriodViewModel.Cycle(
                id = entity.id,
                startDate = LocalDate.parse(entity.startDate),
                endDate = entity.endDate.takeIf { d -> d.isNotBlank() }?.let { d -> LocalDate.parse(d) },
                bleeding = entity.bleeding,
                painLevel = entity.painLevel,
                bloodColor = entity.bloodColor
            )
        }

        // Reverting to your previous simple prediction for the widget
        val prediction = predictCycle(cycles)

        // 2. FORCE COLOR RESOLUTION (The Fix)
        // Check if system is in Night Mode manually
        val isNightMode = (context.resources.configuration.uiMode and
                android.content.res.Configuration.UI_MODE_NIGHT_MASK) ==
                android.content.res.Configuration.UI_MODE_NIGHT_YES

        // Manually define colors based on the check
        val colorPrimary = if (isNightMode) Color.WHITE else Color.BLACK
        // Muted is 30% white (0x4DFFFFFF) or 30% black (0x4D000000)
        val colorMuted = if (isNightMode) Color.parseColor("#4DFFFFFF") else Color.parseColor("#4D000000")

        // 3. Header
        views.setTextViewText(R.id.txt_month_year, ym.month.getDisplayName(TextStyle.FULL, Locale.getDefault()) + " " + ym.year)
        views.setTextColor(R.id.txt_month_year, colorPrimary)

        // 4. Force Weekday Labels (SUN, MON...) to Correct Color
        val weekdayIds = intArrayOf(R.id.label_sun, R.id.label_mon, R.id.label_tue, R.id.label_wed, R.id.label_thu, R.id.label_fri, R.id.label_sat)
        for (id in weekdayIds) {
            views.setTextColor(id, colorMuted)
        }

        // 5. Grid Logic
        val firstDay = ym.atDay(1)
        val startOffset = firstDay.dayOfWeek.value % 7
        val gridStart = firstDay.minusDays(startOffset.toLong())
        val gridEnd = gridStart.plusDays(41)

        val statusMap = mutableMapOf<LocalDate, Int>()
        var mapCursor = gridStart
        while (!mapCursor.isAfter(gridEnd)) {
            val isLogged = cycles.any {
                val end = it.endDate ?: it.startDate.plusDays(6)
                !mapCursor.isBefore(it.startDate) && !mapCursor.isAfter(end)
            }
            val isPredicted = prediction?.let { p ->
                !mapCursor.isBefore(p.mostLikelyPeriodStart) &&
                        mapCursor.isBefore(p.mostLikelyPeriodStart.plusDays((p.periodLength ?: 5).toLong()))
            } == true
            val isFertile = prediction?.fertileWindow?.contains(mapCursor) == true

            statusMap[mapCursor] = when {
                isLogged -> 1
                isPredicted -> 2
                isFertile -> 3
                else -> 0
            }
            mapCursor = mapCursor.plusDays(1)
        }

        // 6. Build Grid
        views.removeAllViews(R.id.calendar_grid)

        var cursor = gridStart
        repeat(6) {
            val rowViews = RemoteViews(context.packageName, R.layout.widget_row_container)
            repeat(7) {
                val cell = RemoteViews(context.packageName, R.layout.item_widget_day)
                val date = cursor
                val status = statusMap[date] ?: 0
                val prevStatus = statusMap[date.minusDays(1)] ?: 0
                val nextStatus = statusMap[date.plusDays(1)] ?: 0

                cell.setTextViewText(R.id.day_text, date.dayOfMonth.toString())

                val bgRes = when (status) {
                    1 -> { // Logged
                        val grp = listOf(1, 2)
                        when {
                            prevStatus !in grp && nextStatus !in grp -> R.drawable.cal_strip_pastel_single
                            prevStatus !in grp && nextStatus in grp -> R.drawable.cal_strip_pastel_start
                            prevStatus in grp && nextStatus in grp -> R.drawable.cal_strip_pastel_middle
                            prevStatus in grp && nextStatus !in grp -> R.drawable.cal_strip_pastel_end
                            else -> 0
                        }
                    }
                    2 -> { // Predicted
                        val grp = listOf(1, 2)
                        when {
                            prevStatus !in grp && nextStatus !in grp -> R.drawable.cal_strip_single
                            prevStatus !in grp && nextStatus in grp -> R.drawable.cal_strip_start
                            prevStatus in grp && nextStatus in grp -> R.drawable.cal_strip_middle
                            prevStatus in grp && nextStatus !in grp -> R.drawable.cal_strip_end
                            else -> 0
                        }
                    }
                    3 -> { // Fertile
                        when {
                            prevStatus != 3 && nextStatus != 3 -> R.drawable.cal_strip_fertile_single
                            prevStatus != 3 && nextStatus == 3 -> R.drawable.cal_strip_fertile_start
                            prevStatus == 3 && nextStatus == 3 -> R.drawable.cal_strip_fertile_middle
                            prevStatus == 3 && nextStatus != 3 -> R.drawable.cal_strip_fertile_end
                            else -> 0
                        }
                    }
                    else -> 0
                }

                if (bgRes != 0) {
                    cell.setInt(R.id.day_text, "setBackgroundResource", bgRes)
                    // Status 1 (Pastel) uses dynamic Black/White; Solid Red/Green uses fixed White
                    cell.setTextColor(R.id.day_text, if (status == 1) colorPrimary else Color.WHITE)
                } else if (date == LocalDate.now()) {
                    cell.setInt(R.id.day_text, "setBackgroundResource", R.drawable.bg_widget_day_today)
                    cell.setTextColor(R.id.day_text, colorPrimary)
                } else {
                    cell.setInt(R.id.day_text, "setBackgroundResource", 0)
                    cell.setTextColor(R.id.day_text, if (date.month == ym.month) colorPrimary else colorMuted)
                }

                rowViews.addView(R.id.row_inner, cell)
                cursor = cursor.plusDays(1)
            }
            views.addView(R.id.calendar_grid, rowViews)
        }

        // Intents
        views.setOnClickPendingIntent(R.id.btn_prev, CalendarWidgetProvider.getNavigationIntent(context, widgetId, -1))
        views.setOnClickPendingIntent(R.id.btn_next, CalendarWidgetProvider.getNavigationIntent(context, widgetId, 1))
        views.setOnClickPendingIntent(R.id.widget_root, CalendarWidgetProvider.getAppIntent(context))

        return views
    }
}