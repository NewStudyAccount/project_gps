package com.gps.dashboard.data.model

data class SatelliteInfo(
    val prn: Int,
    val cn0: Float,
    val constellation: Constellation,
    val inUse: Boolean,
    val elevation: Float = 0f,
    val azimuth: Float = 0f,
)

enum class Constellation(val label: String) {
    GPS("GPS"),
    GLONASS("GLO"),
    BEIDOU("BDS"),
    GALILEO("GAL"),
    QZSS("QZS"),
    SBAS("SBA"),
    OTHER("OTH"),
}
