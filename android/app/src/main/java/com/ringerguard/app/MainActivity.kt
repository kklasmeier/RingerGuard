package com.ringerguard.app

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.ringerguard.app.databinding.ActivityMainBinding
import com.ringerguard.app.ui.ForegroundRefreshable
import com.ringerguard.app.ui.HomeFragment
import com.ringerguard.app.ui.LogFragment
import com.ringerguard.app.ui.ScheduleFragment
import com.ringerguard.app.ui.SettingsFragment

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val homeFragment by lazy { HomeFragment() }
    private val logFragment by lazy { LogFragment() }
    private val scheduleFragment by lazy { ScheduleFragment() }
    private val settingsFragment by lazy { SettingsFragment() }

    private val refreshHandler = Handler(Looper.getMainLooper())
    private val refreshRunnable = object : Runnable {
        override fun run() {
            performForegroundRefresh()
            refreshHandler.postDelayed(this, FOREGROUND_REFRESH_MS)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        applyStatusBarInsets()

        ensureDefaults()
        RingerService.start(this)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, homeFragment, TAG_HOME)
                .commit()
        }

        binding.bottomNav.setOnItemSelectedListener { item ->
            val fragment: Fragment = when (item.itemId) {
                R.id.nav_home -> homeFragment
                R.id.nav_log -> logFragment
                R.id.nav_schedule -> scheduleFragment
                R.id.nav_settings -> settingsFragment
                else -> return@setOnItemSelectedListener false
            }
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, fragment, fragmentTag(item.itemId))
                .commit()
            true
        }
    }

    override fun onResume() {
        super.onResume()
        RingerService.refreshNotification(this)
        RingerWidget.update(this)
        performForegroundRefresh()
        refreshHandler.post(refreshRunnable)
    }

    override fun onPause() {
        refreshHandler.removeCallbacks(refreshRunnable)
        super.onPause()
    }

    private fun performForegroundRefresh() {
        val prefs = AppPreferences(this)
        if (prefs.silenceMode == RingerGuardPrefs.SILENCE_TIMED) {
            RingerWidget.update(this)
        }
        (supportFragmentManager.findFragmentById(R.id.fragment_container) as? ForegroundRefreshable)
            ?.onForegroundRefresh()
    }

    private fun applyStatusBarInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.toolbar) { view, insets ->
            val statusBars = insets.getInsets(WindowInsetsCompat.Type.statusBars())
            view.setPadding(view.paddingLeft, statusBars.top, view.paddingRight, view.paddingBottom)
            insets
        }
        ViewCompat.requestApplyInsets(binding.toolbar)
    }

    private fun ensureDefaults() {
        val prefs = AppPreferences(this)
        if (prefs.restoreVolumeLevel <= 0) {
            prefs.restoreVolumeLevel = RingerRestoreHelper.effectiveRestoreVolume(this)
        }
    }

    private fun fragmentTag(itemId: Int): String = when (itemId) {
        R.id.nav_home -> TAG_HOME
        R.id.nav_log -> TAG_LOG
        R.id.nav_schedule -> TAG_SCHEDULE
        R.id.nav_settings -> TAG_SETTINGS
        else -> TAG_HOME
    }

    companion object {
        private const val TAG_HOME = "home"
        private const val TAG_LOG = "log"
        private const val TAG_SCHEDULE = "schedule"
        private const val TAG_SETTINGS = "settings"
        private const val FOREGROUND_REFRESH_MS = 1_000L
    }
}
