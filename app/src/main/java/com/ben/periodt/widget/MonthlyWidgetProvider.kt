package com.ben.periodt.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.SizeF
import android.widget.RemoteViews
import com.ben.periodt.MainActivity  // ✅ Import MainActivity
import com.ben.periodt.R
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MonthlyWidgetProvider : AppWidgetProvider() {

    companion object {
        const val ACTION_PREV = "com.ben.periodt.widget.PREV"
        const val ACTION_NEXT = "com.ben.periodt.widget.NEXT"
        const val ACTION_REFRESH = "com.ben.periodt.widget.REFRESH"

        private const val PREFS = "monthly_widget_prefs"
        private fun keyYear(id: Int) = "year_$id"
        private fun keyMonth(id: Int) = "month_$id"

        fun refreshAll(context: Context) {
            val mgr = AppWidgetManager.getInstance(context)
            val cn = ComponentName(context, MonthlyWidgetProvider::class.java)
            val ids = mgr.getAppWidgetIds(cn)
            if (ids.isEmpty()) return
            val intent = Intent(context, MonthlyWidgetProvider::class.java).apply {
                action = ACTION_REFRESH
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            }
            context.sendBroadcast(intent)
        }
    }

    private val ioScope = CoroutineScope(Dispatchers.IO)

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        ids.forEach { id ->
            ensureDefaults(context, id)
            ioScope.launch {
                val rv = buildResponsiveRemoteViews(context, manager, id)
                withContext(Dispatchers.Main) {
                    manager.updateAppWidget(id, rv)
                }
            }
        }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle?
    ) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        ioScope.launch {
            val rv = buildResponsiveRemoteViews(context, appWidgetManager, appWidgetId, newOptions)
            withContext(Dispatchers.Main) {
                appWidgetManager.updateAppWidget(appWidgetId, rv)
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val mgr = AppWidgetManager.getInstance(context)
        val cn = ComponentName(context, MonthlyWidgetProvider::class.java)
        val allIds = mgr.getAppWidgetIds(cn)
        if (allIds.isEmpty()) return

        when (intent.action) {
            ACTION_PREV, ACTION_NEXT -> {
                val shift = if (intent.action == ACTION_PREV) -1 else 1
                ioScope.launch {
                    allIds.forEach { id ->
                        val (y, m) = getYm(context, id)
                        val ym = YearMonth.of(y, m).plusMonths(shift.toLong())
                        saveYm(context, id, ym.year, ym.monthValue)
                        val rv = buildResponsiveRemoteViews(context, mgr, id)
                        withContext(Dispatchers.Main) {
                            mgr.updateAppWidget(id, rv)
                        }
                    }
                }
            }
            ACTION_REFRESH, AppWidgetManager.ACTION_APPWIDGET_UPDATE -> {
                val targetIds = intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS) ?: allIds
                ioScope.launch {
                    targetIds.forEach { id ->
                        val rv = buildResponsiveRemoteViews(context, mgr, id)
                        withContext(Dispatchers.Main) {
                            mgr.updateAppWidget(id, rv)
                        }
                    }
                }
            }
        }
    }

    /**
     * Provide a RemoteViews mapping so the launcher can pick an optimal layout per size.
     * Falls back to a single RemoteViews when mapping is not supported.
     */
    private suspend fun buildResponsiveRemoteViews(
        context: Context,
        manager: AppWidgetManager,
        appWidgetId: Int,
        options: Bundle? = null
    ): RemoteViews {
        // If launcher provides precise sizes (Android 12L+), map each to RemoteViews.
        val sizes: ArrayList<SizeF>? =
            if (Build.VERSION.SDK_INT >= 31) {
                val bundle = options ?: manager.getAppWidgetOptions(appWidgetId)
                @Suppress("DEPRECATION")
                bundle.getParcelableArrayList(AppWidgetManager.OPTION_APPWIDGET_SIZES)
            } else null

        return if (!sizes.isNullOrEmpty() && Build.VERSION.SDK_INT >= 31) {
            val mapping = sizes.associateWith { _ ->
                // For this widget the same base layout scales with weights.
                // Still rebuild content per size to adapt text sizes if desired.
                buildViews(context, appWidgetId)
            }
            RemoteViews(mapping)
        } else {
            // Fallback: single RemoteViews; launcher will stretch it.
            buildViews(context, appWidgetId)
        }
    }

    private suspend fun buildViews(context: Context, appWidgetId: Int): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_monthly)

        val (y, m) = getYm(context, appWidgetId)
        val ym = YearMonth.of(y, m)

        views.setTextViewText(
            R.id.title_month,
            "${ym.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${ym.year}"
        )
        views.setOnClickPendingIntent(R.id.btn_prev, pendingBroadcast(context, ACTION_PREV))
        views.setOnClickPendingIntent(R.id.btn_next, pendingBroadcast(context, ACTION_NEXT))

        // ✅ Add tap-to-launch: Set PendingIntent on root view (R.id.root from widget_monthly.xml)
        views.setOnClickPendingIntent(R.id.root, pendingActivityIntent(context))

        val flags = MonthlyWidgetRenderer.buildFlagsSnapshot(context, ym)

        views.removeAllViews(R.id.grid_container)

        val first = ym.atDay(1)
        val firstDow = first.dayOfWeek.value % 7
        var cursor = first.minusDays(firstDow.toLong())

        // Resolve colors from resources (values / values-night)
        val baseColor = context.getColor(R.color.widget_text)

        repeat(6) {  // 6 rows for ~6 weeks
            val row = RemoteViews(context.packageName, R.layout.widget_row_container)
            repeat(7) {  // 7 days per week
                val cell = RemoteViews(context.packageName, R.layout.widget_day_cell)
                val f = flags[cursor]
                val isCurrentMonth = cursor.monthValue == ym.monthValue

                cell.setTextViewText(R.id.day_text, cursor.dayOfMonth.toString())

                val bgRes = when {
                    f?.ovulation == true -> R.drawable.bg_day_cell_ovulation
                    f?.fertile == true -> R.drawable.bg_day_cell_fertile
                    f?.predictedPeriod == true && isCurrentMonth -> R.drawable.bg_day_cell_predicted_period
                    f?.inCycle == true && isCurrentMonth -> R.drawable.bg_day_cell_period
                    else -> R.drawable.bg_day_cell_default
                }
                cell.setInt(R.id.day_cell, "setBackgroundResource", bgRes)

                // If special states need different contrast, use onStateColor; otherwise baseColor.
                val textColor = when {
                    f?.ovulation == true -> baseColor
                    f?.fertile == true -> baseColor
                    f?.predictedPeriod == true -> baseColor
                    f?.isToday == true -> baseColor
                    isCurrentMonth -> baseColor
                    else -> baseColor
                }
                cell.setTextColor(R.id.day_text, textColor)

                cell.setViewPadding(R.id.day_cell, 2, 2, 2, 2)
                row.addView(R.id.row_inner, cell)
                cursor = cursor.plusDays(1)
            }
            views.addView(R.id.grid_container, row)
        }
        return views
    }

    // ✅ New helper: Create PendingIntent to launch MainActivity
    private fun pendingActivityIntent(ctx: Context): PendingIntent {
        val intent = Intent(ctx, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or
                if (Build.VERSION.SDK_INT >= 23) PendingIntent.FLAG_IMMUTABLE else 0
        return PendingIntent.getActivity(ctx, 0, intent, flags)
    }

    private fun ensureDefaults(ctx: Context, id: Int) {
        val p = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (!p.contains(keyYear(id)) || !p.contains(keyMonth(id))) {
            val now = LocalDate.now()
            saveYm(ctx, id, now.year, now.monthValue)
        }
    }
    private fun getYm(ctx: Context, id: Int): Pair<Int, Int> {
        val p = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return (p.getInt(keyYear(id), LocalDate.now().year) to
                p.getInt(keyMonth(id), LocalDate.now().monthValue))
    }
    private fun saveYm(ctx: Context, id: Int, year: Int, month: Int) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putInt(keyYear(id), year).putInt(keyMonth(id), month).apply()
    }
    private fun pendingBroadcast(ctx: Context, action: String): PendingIntent {
        val intent = Intent(ctx, MonthlyWidgetProvider::class.java).setAction(action)
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or
                if (Build.VERSION.SDK_INT >= 23) PendingIntent.FLAG_IMMUTABLE else 0
        return PendingIntent.getBroadcast(ctx, action.hashCode(), intent, flags)
    }
}