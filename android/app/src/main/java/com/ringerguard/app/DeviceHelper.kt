package com.ringerguard.app

import android.content.Context
import android.os.Build

object DeviceHelper {
    fun isSamsung(): Boolean = Build.MANUFACTURER.equals("samsung", ignoreCase = true)
}
