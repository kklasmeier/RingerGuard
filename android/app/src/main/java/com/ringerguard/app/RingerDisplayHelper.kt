package com.ringerguard.app

import android.content.Context

object RingerDisplayHelper {
    data class DisplayModel(
        val title: String,
        val badgeText: String,
        val backgroundColorRes: Int,
        val textColorRes: Int,
        val statusDotRes: Int,
        val statusIconRes: Int,
        val compactStatusText: String?,
        val showCountdownInWidget: Boolean,
        val widgetCountdown: String?,
        val dimSilenceButton: Boolean,
        val dimIndefiniteButton: Boolean,
        val dimEnableButton: Boolean,
    )

    fun resolve(context: Context): DisplayModel {
        val prefs = AppPreferences(context)
        val live = RingerStatusHelper.read(context)

        when (prefs.silenceMode) {
            RingerGuardPrefs.SILENCE_TIMED -> {
                if (RingerStatusHelper.isUserRingerEnabled(live)) {
                    return liveDisplay(context, live)
                }
                val remaining = GracePeriodHelper.formatRemaining(prefs.graceUntilMs)
                return DisplayModel(
                    title = context.getString(R.string.status_silenced, remaining),
                    badgeText = context.getString(R.string.widget_silenced),
                    backgroundColorRes = R.color.rg_status_orange_bg,
                    textColorRes = R.color.rg_orange,
                    statusDotRes = R.drawable.status_dot_orange,
                    statusIconRes = R.drawable.ic_ringer_off_24,
                    compactStatusText = null,
                    showCountdownInWidget = false,
                    widgetCountdown = remaining,
                    dimSilenceButton = false,
                    dimIndefiniteButton = true,
                    dimEnableButton = false,
                )
            }
            RingerGuardPrefs.SILENCE_INDEFINITE -> {
                if (RingerStatusHelper.isUserRingerEnabled(live)) {
                    return liveDisplay(context, live)
                }
                return DisplayModel(
                    title = context.getString(R.string.status_indefinite),
                    badgeText = context.getString(R.string.widget_indefinite),
                    backgroundColorRes = R.color.rg_status_purple_bg,
                    textColorRes = R.color.rg_purple,
                    statusDotRes = R.drawable.status_dot_purple,
                    statusIconRes = R.drawable.ic_indefinite_24,
                    compactStatusText = context.getString(R.string.widget_btn_indefinite_short),
                    showCountdownInWidget = false,
                    widgetCountdown = null,
                    dimSilenceButton = true,
                    dimIndefiniteButton = false,
                    dimEnableButton = false,
                )
            }
        }

        return liveDisplay(context, live)
    }

    private fun liveDisplay(context: Context, live: RingerStatusHelper.LiveStatus): DisplayModel {
        val targetVolume = RingerRestoreHelper.effectiveRestoreVolume(context)

        return when {
            live.isSilent -> DisplayModel(
                title = context.getString(R.string.status_ringer_off),
                badgeText = context.getString(R.string.widget_ringer_off),
                backgroundColorRes = R.color.rg_status_orange_bg,
                textColorRes = R.color.rg_orange,
                statusDotRes = R.drawable.status_dot_orange,
                statusIconRes = R.drawable.ic_ringer_off_24,
                compactStatusText = context.getString(R.string.widget_ringer_off),
                showCountdownInWidget = false,
                widgetCountdown = null,
                dimSilenceButton = false,
                dimIndefiniteButton = false,
                dimEnableButton = false,
            )
            live.isVibrate -> DisplayModel(
                title = context.getString(R.string.status_vibrate_only),
                badgeText = context.getString(R.string.widget_vibrate_only),
                backgroundColorRes = R.color.rg_status_orange_bg,
                textColorRes = R.color.rg_orange,
                statusDotRes = R.drawable.status_dot_orange,
                statusIconRes = R.drawable.ic_vibrate_24,
                compactStatusText = context.getString(R.string.widget_vibrate_only),
                showCountdownInWidget = false,
                widgetCountdown = null,
                dimSilenceButton = false,
                dimIndefiniteButton = false,
                dimEnableButton = false,
            )
            live.currentVolume < targetVolume -> DisplayModel(
                title = context.getString(
                    R.string.status_volume_low,
                    live.currentVolume,
                    live.maxVolume,
                ),
                badgeText = context.getString(
                    R.string.widget_volume_low,
                    live.currentVolume,
                    live.maxVolume,
                ),
                backgroundColorRes = R.color.rg_status_orange_bg,
                textColorRes = R.color.rg_orange,
                statusDotRes = R.drawable.status_dot_orange,
                statusIconRes = R.drawable.ic_volume_low_24,
                compactStatusText = context.getString(
                    R.string.widget_volume_low,
                    live.currentVolume,
                    live.maxVolume,
                ),
                showCountdownInWidget = false,
                widgetCountdown = null,
                dimSilenceButton = false,
                dimIndefiniteButton = false,
                dimEnableButton = false,
            )
            live.dndOn -> DisplayModel(
                title = context.getString(R.string.status_dnd_on),
                badgeText = context.getString(R.string.widget_dnd_on),
                backgroundColorRes = R.color.rg_status_orange_bg,
                textColorRes = R.color.rg_orange,
                statusDotRes = R.drawable.status_dot_orange,
                statusIconRes = R.drawable.ic_dnd_24,
                compactStatusText = context.getString(R.string.widget_dnd_on),
                showCountdownInWidget = false,
                widgetCountdown = null,
                dimSilenceButton = false,
                dimIndefiniteButton = false,
                dimEnableButton = false,
            )
            else -> DisplayModel(
                title = context.getString(R.string.status_ringer_on),
                badgeText = context.getString(R.string.widget_ringer_on),
                backgroundColorRes = R.color.rg_status_green_bg,
                textColorRes = R.color.rg_green,
                statusDotRes = R.drawable.status_dot_green,
                statusIconRes = R.drawable.ic_ringer_on_24,
                compactStatusText = null,
                showCountdownInWidget = false,
                widgetCountdown = null,
                dimSilenceButton = false,
                dimIndefiniteButton = false,
                dimEnableButton = true,
            )
        }
    }

    fun refreshAllSurfaces(context: Context) {
        RingerWidget.update(context)
        RingerService.refreshNotification(context)
    }
}
