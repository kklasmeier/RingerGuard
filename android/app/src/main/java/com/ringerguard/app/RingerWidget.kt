package com.ringerguard.app

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import android.view.View
import android.widget.RemoteViews
import androidx.core.content.ContextCompat

class RingerWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        appWidgetIds.forEach { id ->
            appWidgetManager.updateAppWidget(id, buildRemoteViews(context, appWidgetManager, id))
        }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle,
    ) {
        appWidgetManager.updateAppWidget(appWidgetId, buildRemoteViews(context, appWidgetManager, appWidgetId))
    }

    override fun onEnabled(context: Context) {
        ServiceScheduler.scheduleWidgetTick(context)
    }

    companion object {
        private const val EXPANDED_HEIGHT_THRESHOLD_DP = 88

        fun update(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val component = ComponentName(context, RingerWidget::class.java)
            val ids = manager.getAppWidgetIds(component)
            if (ids.isEmpty()) return
            ids.forEach { id ->
                manager.updateAppWidget(id, buildRemoteViews(context, manager, id))
            }
        }

        private fun buildRemoteViews(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int,
        ): RemoteViews {
            val prefs = AppPreferences(context)
            val expanded = isExpanded(appWidgetManager, appWidgetId)
            val layoutId = when {
                !expanded -> R.layout.ringer_widget_layout_compact
                prefs.silenceMode == RingerGuardPrefs.SILENCE_TIMED ||
                    prefs.silenceMode == RingerGuardPrefs.SILENCE_INDEFINITE ->
                    R.layout.ringer_widget_layout_expanded_status
                else -> R.layout.ringer_widget_layout_expanded
            }
            val display = RingerDisplayHelper.resolve(context)
            val views = RemoteViews(context.packageName, layoutId)
            val silenceLabel = if (expanded) {
                context.getString(R.string.action_silence_duration, prefs.graceDurationMin)
            } else {
                context.getString(R.string.widget_btn_silence_short_min, prefs.graceDurationMin)
            }
            val ringLabel = context.getString(R.string.action_ring_now_short)

            views.setImageViewResource(R.id.widget_status_dot, display.statusDotRes)
            if (expanded) {
                bindExpandedStatus(context, views, prefs, display)
            } else {
                bindCompactStatus(context, views, prefs, display)
            }

            views.setTextViewText(R.id.widget_btn_silence, silenceLabel)
            views.setTextViewText(R.id.widget_btn_indefinite, context.getString(R.string.action_indefinite))
            views.setTextViewText(R.id.widget_btn_ring, ringLabel)
            views.setInt(R.id.widget_btn_silence, "setBackgroundResource", R.drawable.widget_btn_orange)
            views.setInt(R.id.widget_btn_indefinite, "setBackgroundResource", R.drawable.widget_btn_purple)
            views.setInt(R.id.widget_btn_ring, "setBackgroundResource", R.drawable.widget_btn_green)

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

        private fun bindExpandedStatus(
            context: Context,
            views: RemoteViews,
            prefs: AppPreferences,
            display: RingerDisplayHelper.DisplayModel,
        ) {
            when (prefs.silenceMode) {
                RingerGuardPrefs.SILENCE_TIMED -> {
                    views.setViewVisibility(R.id.widget_status_label, View.VISIBLE)
                    views.setTextViewText(R.id.widget_status_label, context.getString(R.string.widget_silenced))
                    views.setTextColor(
                        R.id.widget_status_label,
                        ContextCompat.getColor(context, display.textColorRes),
                    )
                    views.setViewVisibility(R.id.widget_countdown_container, View.VISIBLE)
                    views.setViewVisibility(R.id.widget_countdown, View.VISIBLE)
                    views.setViewVisibility(R.id.widget_countdown_static, View.GONE)
                    configureCountdownChronometer(
                        views,
                        R.id.widget_countdown,
                        prefs.graceUntilMs,
                        ContextCompat.getColor(context, display.textColorRes),
                    )
                    views.setViewVisibility(R.id.widget_countdown_label, View.VISIBLE)
                    views.setTextViewText(
                        R.id.widget_countdown_label,
                        context.getString(R.string.widget_countdown_label),
                    )
                }
                RingerGuardPrefs.SILENCE_INDEFINITE -> {
                    views.setViewVisibility(R.id.widget_status_label, View.VISIBLE)
                    views.setTextViewText(R.id.widget_status_label, context.getString(R.string.widget_indefinite))
                    views.setTextColor(
                        R.id.widget_status_label,
                        ContextCompat.getColor(context, display.textColorRes),
                    )
                    views.setViewVisibility(R.id.widget_countdown_container, View.VISIBLE)
                    views.setViewVisibility(R.id.widget_countdown, View.GONE)
                    views.setViewVisibility(R.id.widget_countdown_static, View.VISIBLE)
                    views.setTextViewText(R.id.widget_countdown_static, "∞")
                    views.setTextColor(
                        R.id.widget_countdown_static,
                        ContextCompat.getColor(context, display.textColorRes),
                    )
                    views.setViewVisibility(R.id.widget_countdown_label, View.VISIBLE)
                    views.setTextViewText(
                        R.id.widget_countdown_label,
                        context.getString(R.string.widget_indefinite_label),
                    )
                }
                else -> {
                    views.setViewVisibility(R.id.widget_status_label, View.VISIBLE)
                    views.setTextViewText(R.id.widget_status_label, display.badgeText)
                    views.setTextColor(
                        R.id.widget_status_label,
                        ContextCompat.getColor(context, display.textColorRes),
                    )
                }
            }
        }

        private fun bindCompactStatus(
            context: Context,
            views: RemoteViews,
            prefs: AppPreferences,
            display: RingerDisplayHelper.DisplayModel,
        ) {
            views.setViewVisibility(R.id.widget_status_chronometer, View.GONE)
            views.setViewVisibility(R.id.widget_status_label, View.GONE)

            when (prefs.silenceMode) {
                RingerGuardPrefs.SILENCE_TIMED -> {
                    views.setViewVisibility(R.id.widget_status_chronometer, View.VISIBLE)
                    configureCountdownChronometer(
                        views,
                        R.id.widget_status_chronometer,
                        prefs.graceUntilMs,
                        ContextCompat.getColor(context, display.textColorRes),
                        9f,
                    )
                }
                RingerGuardPrefs.SILENCE_INDEFINITE -> {
                    views.setViewVisibility(R.id.widget_status_label, View.VISIBLE)
                    views.setTextViewText(
                        R.id.widget_status_label,
                        context.getString(R.string.widget_btn_indefinite_short),
                    )
                    views.setTextColor(
                        R.id.widget_status_label,
                        ContextCompat.getColor(context, display.textColorRes),
                    )
                }
                else -> {
                    if (!display.compactStatusText.isNullOrBlank()) {
                        views.setViewVisibility(R.id.widget_status_label, View.VISIBLE)
                        views.setTextViewText(R.id.widget_status_label, display.compactStatusText)
                        views.setTextColor(
                            R.id.widget_status_label,
                            ContextCompat.getColor(context, display.textColorRes),
                        )
                    }
                }
            }
        }

        private fun configureCountdownChronometer(
            views: RemoteViews,
            viewId: Int,
            graceUntilMs: Long,
            textColor: Int,
            textSizeSp: Float? = null,
        ) {
            val remainingMs = (graceUntilMs - System.currentTimeMillis()).coerceAtLeast(0L)
            views.setChronometer(
                viewId,
                SystemClock.elapsedRealtime() + remainingMs,
                null,
                true,
            )
            views.setBoolean(viewId, "setCountDown", true)
            views.setTextColor(viewId, textColor)
            if (textSizeSp != null) {
                views.setFloat(viewId, "setTextSize", textSizeSp)
            }
        }

        private fun isExpanded(appWidgetManager: AppWidgetManager, appWidgetId: Int): Boolean {
            val options = appWidgetManager.getAppWidgetOptions(appWidgetId)
            val heightDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 0)
            if (heightDp == 0) return false
            return heightDp >= EXPANDED_HEIGHT_THRESHOLD_DP
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
