package com.ringerguard.app.ui

/** Called once per second while [com.ringerguard.app.MainActivity] is in the foreground. */
fun interface ForegroundRefreshable {
    fun onForegroundRefresh()
}
