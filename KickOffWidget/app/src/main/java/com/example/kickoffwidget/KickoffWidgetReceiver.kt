package com.example.kickoffwidget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import com.example.kickoffwidget.data.sync.FixtureSyncScheduler

class KickoffWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = KickoffWidget()

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        FixtureSyncScheduler.start(context)
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: android.appwidget.AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        FixtureSyncScheduler.start(context)
    }

    override fun onDisabled(context: Context) {
        FixtureSyncScheduler.stop(context)
        super.onDisabled(context)
    }
}
