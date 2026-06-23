package com.gps.dashboard.data.repository

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.GnssStatus
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat
import com.gps.dashboard.data.model.Constellation
import com.gps.dashboard.data.model.SatelliteInfo
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class SatelliteRepository(private val context: Context) {

    private val locationManager =
        context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    val satelliteFlow: Flow<List<SatelliteInfo>> = callbackFlow {
        val hasPermission = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasPermission) {
            close()
            return@callbackFlow
        }

        val mainHandler = Handler(Looper.getMainLooper())

        val gnssCallback = object : GnssStatus.Callback() {
            override fun onSatelliteStatusChanged(status: GnssStatus) {
                val satellites = (0 until status.satelliteCount).map { i ->
                    SatelliteInfo(
                        prn = status.getSvid(i),
                        cn0 = status.getCn0DbHz(i),
                        constellation = mapConstellation(status.getConstellationType(i)),
                        inUse = status.usedInFix(i),
                        elevation = status.getElevationDegrees(i),
                        azimuth = status.getAzimuthDegrees(i),
                    )
                }
                trySend(satellites)
            }

            override fun onStarted() {}
            override fun onStopped() {}
        }

        // 空的 LocationListener，仅用于激活 GPS 引擎
        val locationListener = object : LocationListener {
            override fun onLocationChanged(location: Location) {}
            override fun onStatusChanged(provider: String?, status: Int, extras: android.os.Bundle?) {}
            override fun onProviderEnabled(provider: String) {}
            override fun onProviderDisabled(provider: String) {}
        }

        try {
            // 注册卫星状态回调
            locationManager.registerGnssStatusCallback(gnssCallback, mainHandler)

            // 请求位置更新以激活 GPS 引擎，否则卫星回调不会触发
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                1000L,
                0f,
                locationListener
            )
        } catch (e: SecurityException) {
            close(e)
        }

        awaitClose {
            locationManager.unregisterGnssStatusCallback(gnssCallback)
            locationManager.removeUpdates(locationListener)
        }
    }

    private fun mapConstellation(type: Int): Constellation = when (type) {
        GnssStatus.CONSTELLATION_GPS -> Constellation.GPS
        GnssStatus.CONSTELLATION_GLONASS -> Constellation.GLONASS
        GnssStatus.CONSTELLATION_BEIDOU -> Constellation.BEIDOU
        GnssStatus.CONSTELLATION_GALILEO -> Constellation.GALILEO
        GnssStatus.CONSTELLATION_QZSS -> Constellation.QZSS
        GnssStatus.CONSTELLATION_SBAS -> Constellation.SBAS
        else -> Constellation.OTHER
    }
}
