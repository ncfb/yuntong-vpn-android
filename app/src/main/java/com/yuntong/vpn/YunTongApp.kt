package com.yuntong.vpn

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class YunTongApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Hilt auto-injects, no manual setup needed
    }
}
