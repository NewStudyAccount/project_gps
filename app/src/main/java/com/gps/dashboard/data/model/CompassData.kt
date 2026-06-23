package com.gps.dashboard.data.model

data class CompassData(
    val heading: Float = 0f,
    val accuracy: SensorAccuracy = SensorAccuracy.UNRELIABLE,
)

enum class SensorAccuracy {
    UNRELIABLE, LOW, MEDIUM, HIGH
}
