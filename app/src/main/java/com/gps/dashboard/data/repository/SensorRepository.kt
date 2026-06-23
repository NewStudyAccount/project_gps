package com.gps.dashboard.data.repository

import android.content.Context
import android.hardware.GeomagneticField
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.gps.dashboard.data.model.CompassData
import com.gps.dashboard.data.model.SensorAccuracy
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class SensorRepository(private val context: Context) {

    private val sensorManager =
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

    private val gravity = FloatArray(3)
    private val geomagnetic = FloatArray(3)
    private val rotationMatrix = FloatArray(9)
    private val orientation = FloatArray(3)

    private val alpha = 0.15f

    private var lastLatitude = 0.0
    private var lastLongitude = 0.0
    private var lastAltitude = 0.0

    fun updateLocationForDeclination(lat: Double, lon: Double, alt: Double) {
        lastLatitude = lat
        lastLongitude = lon
        lastAltitude = alt
    }

    val compassFlow: Flow<CompassData> = callbackFlow {
        var sensorAccuracy = SensorAccuracy.UNRELIABLE

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                when (event.sensor.type) {
                    Sensor.TYPE_ACCELEROMETER -> {
                        gravity[0] = alpha * gravity[0] + (1 - alpha) * event.values[0]
                        gravity[1] = alpha * gravity[1] + (1 - alpha) * event.values[1]
                        gravity[2] = alpha * gravity[2] + (1 - alpha) * event.values[2]
                    }

                    Sensor.TYPE_MAGNETIC_FIELD -> {
                        geomagnetic[0] = alpha * geomagnetic[0] + (1 - alpha) * event.values[0]
                        geomagnetic[1] = alpha * geomagnetic[1] + (1 - alpha) * event.values[1]
                        geomagnetic[2] = alpha * geomagnetic[2] + (1 - alpha) * event.values[2]
                    }
                }

                val success = SensorManager.getRotationMatrix(
                    rotationMatrix, null, gravity, geomagnetic
                )

                if (success) {
                    SensorManager.getOrientation(rotationMatrix, orientation)
                    var azimuth = Math.toDegrees(orientation[0].toDouble()).toFloat()

                    // 磁偏角修正 → 真北
                    val geoField = GeomagneticField(
                        lastLatitude.toFloat(),
                        lastLongitude.toFloat(),
                        lastAltitude.toFloat(),
                        System.currentTimeMillis()
                    )
                    azimuth = (azimuth + geoField.declination + 360) % 360

                    trySend(CompassData(heading = azimuth, accuracy = sensorAccuracy))
                }
            }

            override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
                sensorAccuracy = when (accuracy) {
                    SensorManager.SENSOR_STATUS_ACCURACY_HIGH -> SensorAccuracy.HIGH
                    SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM -> SensorAccuracy.MEDIUM
                    SensorManager.SENSOR_STATUS_ACCURACY_LOW -> SensorAccuracy.LOW
                    else -> SensorAccuracy.UNRELIABLE
                }
            }
        }

        accelerometer?.let {
            sensorManager.registerListener(
                listener, it, SensorManager.SENSOR_DELAY_UI
            )
        }
        magnetometer?.let {
            sensorManager.registerListener(
                listener, it, SensorManager.SENSOR_DELAY_UI
            )
        }

        awaitClose {
            sensorManager.unregisterListener(listener)
        }
    }
}
