package com.gps.dashboard.data.model

data class GpsData(
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val altitude: Double = 0.0,
    val speed: Float = 0f,
    val bearing: Float = 0f,
    val accuracy: Float = 0f,
    val hdop: Float = 0f,
    val fixType: FixType = FixType.NONE,
    val provider: Set<String> = emptySet(),
    val timestamp: Long = 0L,
    val trueBearing: Float = 0f,
)

enum class FixType {
    NONE, FIX_2D, FIX_3D
}
