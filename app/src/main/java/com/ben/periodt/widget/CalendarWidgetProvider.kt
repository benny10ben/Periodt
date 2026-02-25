package com.ben.periodt.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import com.ben.periodt.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate

class CalendarWidgetProvider : AppWidgetProvider() {

    companion object {
        const val ACTION_NAV = "com.ben.periodt.widget.NAV"
        const val EXTRA_OFFSET = "offset"

        // Helper to create Nav intents
        fun getNavigationIntent(context: Context, widgetId: Int, offset: Int): PendingIntent {
            val intent = Intent(context, CalendarWidgetProvider::class.java).apply {
                action = ACTION_NAV
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                putExtra(EXTRA_OFFSET, offset)
            }
            return PendingIntent.getBroadcast(
                context,
                widgetId * 10 + offset, // Unique ID to prevent intent overlap
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        // Intent to open the main app
        fun getAppIntent(context: Context): PendingIntent {
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            return PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        // Static refresh triggered from ViewModel
        fun refreshAll(context: Context) {
            val intent = Intent(context, CalendarWidgetProvider::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            }
            val ids = AppWidgetManager.getInstance(context)
                .getAppWidgetIds(ComponentName(context, CalendarWidgetProvider::class.java))

            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            context.sendBroadcast(intent)
        }
    }

    private val scope = CoroutineScope(Dispatchers.IO)

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        // Update each widget instance
        appWidgetIds.forEach { id -> updateWidget(context, appWidgetManager, id) }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        val appWidgetManager = AppWidgetManager.getInstance(context)
        val componentName = ComponentName(context, CalendarWidgetProvider::class.java)
        val ids = appWidgetManager.getAppWidgetIds(componentName)

        when (intent.action) {
            // ✅ Handle theme changes (Light/Dark mode flip)
            Intent.ACTION_CONFIGURATION_CHANGED -> {
                onUpdate(context, appWidgetManager, ids)
            }

            // Handle month navigation (Prev/Next)
            ACTION_NAV -> {
                val widgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
                val offset = intent.getIntExtra(EXTRA_OFFSET, 0)

                if (widgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                    scope.launch {
                        val prefs = context.getSharedPreferences("widget_prefs", Context.MODE_PRIVATE)
                        val keyY = "year_$widgetId"
                        val keyM = "month_$widgetId"

                        val currentY = prefs.getInt(keyY, LocalDate.now().year)
                        val currentM = prefs.getInt(keyM, LocalDate.now().monthValue)

                        // Calculate the new month target
                        val date = LocalDate.of(currentY, currentM, 1).plusMonths(offset.toLong())

                        // Save the state so it persists on next update
                        prefs.edit()
                            .putInt(keyY, date.year)
                            .putInt(keyM, date.monthValue)
                            .apply()

                        // Trigger visual re-render
                        val views = CalendarWidgetRenderer.render(context, widgetId, date.year, date.monthValue)
                        appWidgetManager.updateAppWidget(widgetId, views)
                    }
                }
            }
        }
    }

    private fun updateWidget(context: Context, manager: AppWidgetManager, id: Int) {
        scope.launch {
            val prefs = context.getSharedPreferences("widget_prefs", Context.MODE_PRIVATE)
            val y = prefs.getInt("year_$id", LocalDate.now().year)
            val m = prefs.getInt("month_$id", LocalDate.now().monthValue)

            // Get the RemoteViews from the Renderer
            val views = CalendarWidgetRenderer.render(context, id, y, m)
            manager.updateAppWidget(id, views)
        }
    }
}