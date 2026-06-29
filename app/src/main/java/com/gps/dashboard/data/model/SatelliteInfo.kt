package com.gps.dashboard.data.model

import androidx.compose.ui.graphics.Color

data class SatelliteInfo(
    val prn: Int,
    val cn0: Float,
    val constellation: Constellation,
    val inUse: Boolean,
    val elevation: Float = 0f,
    val azimuth: Float = 0f,
)

enum class Constellation(val label: String, val color: Color) {
    GPS("GPS", Color(0xFF2196F3)),       // 蓝色
    GLONASS("GLO", Color(0xFFF44336)),   // 红色
    BEIDOU("BDS", Color(0xFFFF9800)),    // 橙色
    GALILEO("GAL", Color(0xFF4CAF50)),   // 绿色
    QZSS("QZS", Color(0xFF9C27B0)),      // 紫色
    SBAS("SBA", Color(0xFF00BCD4)),      // 青色
    OTHER("OTH", Color(0xFF757575)),     // 灰色
}
