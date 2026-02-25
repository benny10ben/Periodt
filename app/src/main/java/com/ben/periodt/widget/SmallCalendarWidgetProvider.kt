package com.ben.periodt.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate

class SmallCalendarWidgetProvider : AppWidgetProvider() {

    companion object {
        private const val ACTION_NAV_SMALL = "com.ben.periodt.widget.NAV_SMALL"

        fun getNavIntent(context: Context, widgetId: Int, offset: Int): PendingIntent {
            val intent = Intent(context, SmallCalendarWidgetProvider::class.java).apply {
                action = ACTION_NAV_SMALL
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                putExtra("offset", offset)
            }
            return PendingIntent.getBroadcast(context, widgetId * 20 + offset, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        }
    }

    private val scope = CoroutineScope(Dispatchers.IO)

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        appWidgetIds.forEach { id ->
            val prefs = context.getSharedPreferences("small_widget_prefs", Context.MODE_PRIVATE)
            val y = prefs.getInt("year_$id", LocalDate.now().year)
            val m = prefs.getInt("month_$id", LocalDate.now().monthValue)

            scope.launch {
                val views = SmallWidgetRenderer.render(context, id, y, m)
                appWidgetManager.updateAppWidget(id, views)
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val mgr = AppWidgetManager.getInstance(context)

        if (intent.action == ACTION_NAV_SMALL) {
            val id = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1)
            val offset = intent.getIntExtra("offset", 0)

            if (id != -1) {
                val prefs = context.getSharedPreferences("small_widget_prefs", Context.MODE_PRIVATE)
                val y = prefs.getInt("year_$id", LocalDate.now().year)
                val m = prefs.getInt("month_$id", LocalDate.now().monthValue)
                val nextDate = LocalDate.of(y, m, 1).plusMonths(offset.toLong())

                prefs.edit().putInt("year_$id", nextDate.year).putInt("month_$id", nextDate.monthValue).apply()
                onUpdate(context, mgr, intArrayOf(id))
            }
        } else if (intent.action == Intent.ACTION_CONFIGURATION_CHANGED) {
            val ids = mgr.getAppWidgetIds(ComponentName(context, SmallCalendarWidgetProvider::class.java))
            onUpdate(context, mgr, ids)
        }
    }
}