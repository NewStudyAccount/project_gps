package com.gps.dashboard.util

import kotlin.math.abs
import kotlin.math.floor

object CoordinateFormatter {

    fun toDMS(decimal: Double, isLat: Boolean): String {
        val absVal = abs(decimal)
        val degrees = floor(absVal).toInt()
        val minutesTotal = (absVal - degrees) * 60
        val minutes = floor(minutesTotal).toInt()
        val seconds = (minutesTotal - minutes) * 60

        val direction = if (isLat) {
            if (decimal >= 0) "N" else "S"
        } else {
            if (decimal >= 0) "E" else "W"
        }

        return "${degrees}°${minutes}'${"%.1f".format(seconds)}\" $direction"
    }
}
