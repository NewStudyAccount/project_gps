package com.gps.dashboard.data.model

enum class SpeedUnit(val label: String, val factor: Float) {
    KMH("km/h", 3.6f),
    MS("m/s", 1.0f),
    MPH("mph", 2.237f),
}
