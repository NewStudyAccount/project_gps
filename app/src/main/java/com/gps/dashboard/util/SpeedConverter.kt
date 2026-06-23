package com.gps.dashboard.util

import com.gps.dashboard.data.model.SpeedUnit

object SpeedConverter {

    fun format(metersPerSecond: Float, unit: SpeedUnit): String {
        val converted = metersPerSecond * unit.factor
        return "%.1f %s".format(converted, unit.label)
    }

    fun convert(metersPerSecond: Float, unit: SpeedUnit): Float {
        return metersPerSecond * unit.factor
    }
}
