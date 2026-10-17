package com.gaurav.galaxyapp

import android.app.Application
import com.gaurav.galaxyapp.worker.ConfigSyncWorker

class GalaxyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Register 1-hour periodic remote config sync on network connection
        ConfigSyncWorker.schedulePeriodicSync(this)
    }
}
