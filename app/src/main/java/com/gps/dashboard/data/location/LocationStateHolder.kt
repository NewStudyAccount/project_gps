package com.gps.dashboard.data.location

import android.location.Location
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 应用级单例，Service 写入 GPS 数据，ViewModel / Recorder / Notification 读取。
 *
 * 位置数据流:
 *   LocationForegroundService → LocationStateHolder → GpsViewModel / TrackRecorder / Notification
 */
object LocationStateHolder {

    private val _location = MutableStateFlow<Location?>(null)
    val location: StateFlow<Location?> = _location.asStateFlow()

    private val _isServiceRunning = MutableStateFlow(false)
    val isServiceRunning: StateFlow<Boolean> = _isServiceRunning.asStateFlow()

    fun update(location: Location) {
        _location.value = location
    }

    fun setServiceRunning(running: Boolean) {
        _isServiceRunning.value = running
    }
}
