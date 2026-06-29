package com.gps.dashboard

import android.app.Application
import com.gps.dashboard.data.db.TrackingDatabase
import com.gps.dashboard.data.location.LocationStateHolder
import com.gps.dashboard.service.LocationForegroundService

class GpsApplication : Application() {

    lateinit var database: TrackingDatabase
        private set

    override fun onCreate() {
        super.onCreate()
        database = TrackingDatabase.getInstance(this)
    }

    fun startLocationService() {
        LocationForegroundService.start(this)
    }

    fun stopLocationService() {
        LocationForegroundService.stop(this)
    }

    val isServiceRunning: Boolean
        get() = LocationStateHolder.isServiceRunning.value
}
