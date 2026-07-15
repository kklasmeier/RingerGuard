package com.ringerguard.app

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews

class RingerWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        appWidgetIds.forEach { id ->
            appWidgetManager.updateAppWidget(id, buildRemoteViews(context))
        }
    }

    override fun onEnabled(context: Context) {
        ServiceScheduler.scheduleWidgetTick(context)
    }

    companion object {
        fun update(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val component = ComponentName(context, RingerWidget::class.java)
            val ids = manager.getAppWidgetIds(component)
            if (ids.isEmpty()) return
            ids.forEach { id ->
                manager.updateAppWidget(id, buildRemoteViews(context))
            }
        }

        private fun buildRemoteViews(context: Context): RemoteViews {
            val prefs = AppPreferences(context)
            val views = RemoteViews(context.packageName, R.layout.ringer_widget_layout)
            val silenceLabel = context.getString(R.string.action_silence_duration, prefs.graceDurationMin)
            views.setTextViewText(R.id.widget_btn_silence, silenceLabel)

            when (prefs.silenceMode) {
                RingerGuardPrefs.SILENCE_TIMED -> {
                    views.setTextViewText(R.id.widget_status_badge, context.getString(R.string.widget_silenced))
                    views.setViewVisibility(R.id.widget_countdown_container, View.VISIBLE)
                    views.setTextViewText(
                        R.id.widget_countdown,
                        GracePeriodHelper.formatRemaining(prefs.graceUntilMs),
                    )
                    views.setTextViewText(R.id.widget_countdown_label, context.getString(R.string.widget_countdown_label))
                    views.setFloat(R.id.widget_btn_silence, "setAlpha", 1f)
                    views.setFloat(R.id.widget_btn_indefinite, "setAlpha", 0.4f)
                    views.setFloat(R.id.widget_btn_ring, "setAlpha", 1f)
                }
                RingerGuardPrefs.SILENCE_INDEFINITE -> {
                    views.setTextViewText(R.id.widget_status_badge, context.getString(R.string.widget_indefinite))
                    views.setViewVisibility(R.id.widget_countdown_container, View.VISIBLE)
                    views.setTextViewText(R.id.widget_countdown, "∞")
                    views.setTextViewText(R.id.widget_countdown_label, context.getString(R.string.widget_indefinite_label))
                    views.setFloat(R.id.widget_btn_silence, "setAlpha", 0.4f)
                    views.setFloat(R.id.widget_btn_indefinite, "setAlpha", 1f)
                    views.setFloat(R.id.widget_btn_ring, "setAlpha", 1f)
                }
                else -> {
                    views.setTextViewText(R.id.widget_status_badge, context.getString(R.string.widget_ringing))
                    views.setViewVisibility(R.id.widget_countdown_container, View.GONE)
                    views.setFloat(R.id.widget_btn_silence, "setAlpha", 1f)
                    views.setFloat(R.id.widget_btn_indefinite, "setAlpha", 1f)
                    views.setFloat(R.id.widget_btn_ring, "setAlpha", 0.4f)
                }
            }

            views.setOnClickPendingIntent(
                R.id.widget_btn_silence,
                actionPendingIntent(context, RingerGuardPrefs.ACTION_SILENCE_TIMED, 10),
            )
            views.setOnClickPendingIntent(
                R.id.widget_btn_indefinite,
                actionPendingIntent(context, RingerGuardPrefs.ACTION_SILENCE_INDEFINITE, 11),
            )
            views.setOnClickPendingIntent(
                R.id.widget_btn_ring,
                actionPendingIntent(context, RingerGuardPrefs.ACTION_RING_NOW, 12),
            )
            return views
        }

        private fun actionPendingIntent(context: Context, action: String, requestCode: Int): PendingIntent {
            val intent = Intent(context, WidgetActionReceiver::class.java).apply { this.action = action }
            return PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        }
    }
}
