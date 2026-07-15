package com.ringerguard.app

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.ringerguard.app.databinding.ActivityMainBinding
import com.ringerguard.app.ui.HomeFragment
import com.ringerguard.app.ui.LogFragment
import com.ringerguard.app.ui.ScheduleFragment
import com.ringerguard.app.ui.SettingsFragment

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ensureDefaults()
        RingerService.start(this)

        if (savedInstanceState == null) {
            showFragment(HomeFragment(), R.id.nav_home)
        }

        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> showFragment(HomeFragment(), item.itemId)
                R.id.nav_log -> showFragment(LogFragment(), item.itemId)
                R.id.nav_schedule -> showFragment(ScheduleFragment(), item.itemId)
                R.id.nav_settings -> showFragment(SettingsFragment(), item.itemId)
                else -> false
            }
        }
    }

    override fun onResume() {
        super.onResume()
        RingerService.refreshNotification(this)
    }

    private fun ensureDefaults() {
        val prefs = AppPreferences(this)
        if (prefs.restoreVolumeLevel <= 0) {
            prefs.restoreVolumeLevel = RingerRestoreHelper.effectiveRestoreVolume(this)
        }
    }

    private fun showFragment(fragment: Fragment, navId: Int): Boolean {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
        binding.bottomNav.selectedItemId = navId
        return true
    }
}
