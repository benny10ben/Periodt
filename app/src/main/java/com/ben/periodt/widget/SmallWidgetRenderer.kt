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

object SmallWidgetRenderer {

    suspend fun render(context: Context, widgetId: Int, year: Int, month: Int): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_small)
        val ym = YearMonth.of(year, month)

        // 1. Data Retrieval
        val dao = AppDatabase.getDatabase(context).periodCycleDao()

        // Use getAllCyclesOnce() to match the standard database retrieval pattern
        val rawCycles = dao.getAllCyclesOnce()

        // FIX: Explicitly name the lambda parameter 'entity' to resolve mapping errors
        val cycles = rawCycles.map { entity ->
            PeriodViewModel.Cycle(
                id = entity.id,
                startDate = LocalDate.parse(entity.startDate),
                endDate = entity.endDate.takeIf { d -> d.isNotBlank() }?.let { d -> LocalDate.parse(d) },
                bleeding = entity.bleeding,
                painLevel = entity.painLevel,
                bloodColor = entity.bloodColor
            )
        }

        val prediction = predictCycle(cycles)

        // 2. Set Header with Year for context
        val monthName = ym.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())
        views.setTextViewText(R.id.txt_month_label, "$monthName ${ym.year}")

        // 3. Colors and Theme Awareness
        val isNight = (context.resources.configuration.uiMode and
                android.content.res.Configuration.UI_MODE_NIGHT_MASK) ==
                android.content.res.Configuration.UI_MODE_NIGHT_YES

        val colorPrimary = if (isNight) Color.WHITE else Color.BLACK

        // Your specific 10% translucent #1B1B1B
        val colorEmpty = Color.parseColor("#9A1B1B1B")

        val colorPastel = Color.parseColor("#F08080") // Logged
        val colorSolid = Color.parseColor("#A5231C")  // Predicted
        //val colorFertile = Color.parseColor("#4CAF50")
        val colorTransparent = Color.TRANSPARENT

        views.setTextColor(R.id.txt_month_label, colorPrimary)
        views.setInt(R.id.btn_prev_small, "setColorFilter", colorPrimary)
        views.setInt(R.id.btn_next_small, "setColorFilter", colorPrimary)

        // 4. Build 7x6 Grid
        views.removeAllViews(R.id.calendar_grid)
        val firstDay = ym.atDay(1)
        val startOffset = firstDay.dayOfWeek.value % 7
        val gridStart = firstDay.minusDays(startOffset.toLong())

        var cursor = gridStart
        repeat(6) {
            val row = RemoteViews(context.packageName, R.layout.widget_row_container)
            repeat(7) {
                val cell = RemoteViews(context.packageName, R.layout.item_small_cell)
                val date = cursor

                // --- COLOR DETERMINATION LOGIC ---
                val isLogged = cycles.any {
                    val end = it.endDate ?: it.startDate.plusDays(6)
                    !date.isBefore(it.startDate) && !date.isAfter(end)
                }

                val isPredicted = prediction?.let { p ->
                    val start = p.mostLikelyPeriodStart
                    val length = (p.periodLength ?: 5).toLong()
                    !date.isBefore(start) && date.isBefore(start.plusDays(length))
                } == true

                val isFertile = prediction?.fertileWindow?.contains(date) == true

                val cellColor = when {
                    isLogged -> colorPastel
                    isPredicted -> colorSolid
                    //isFertile -> colorFertile
                    date.month == ym.month -> colorEmpty
                    else -> colorTransparent
                }
                // ---------------------------------

                // Apply the color to the ImageView
                cell.setInt(R.id.cell_view, "setColorFilter", cellColor)

                // Handle Visibility for days outside the grid month
                if (cellColor == colorTransparent) {
                    cell.setInt(R.id.cell_view, "setImageAlpha", 0)
                } else {
                    cell.setInt(R.id.cell_view, "setImageAlpha", 255)
                }

                row.addView(R.id.row_inner, cell)
                cursor = cursor.plusDays(1)
            }
            views.addView(R.id.calendar_grid, row)
        }

        // 5. Navigation Intents
        views.setOnClickPendingIntent(R.id.btn_prev_small, SmallCalendarWidgetProvider.getNavIntent(context, widgetId, -1))
        views.setOnClickPendingIntent(R.id.btn_next_small, SmallCalendarWidgetProvider.getNavIntent(context, widgetId, 1))
        views.setOnClickPendingIntent(R.id.widget_root, CalendarWidgetProvider.getAppIntent(context))

        return views
    }
}