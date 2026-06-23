package com.gps.dashboard.util

enum class AccuracyLevel {
    GOOD, MEDIUM, BAD
}

object AccuracyEvaluator {

    fun evaluate(accuracyMeters: Float): AccuracyLevel = when {
        accuracyMeters <= 5f -> AccuracyLevel.GOOD
        accuracyMeters <= 15f -> AccuracyLevel.MEDIUM
        else -> AccuracyLevel.BAD
    }

    fun ratio(accuracyMeters: Float): Float = when {
        accuracyMeters <= 5f -> 1f - (accuracyMeters / 5f) * 0.3f   // 1.0 ~ 0.7
        accuracyMeters <= 15f -> 0.7f - ((accuracyMeters - 5f) / 10f) * 0.4f // 0.7 ~ 0.3
        else -> 0.3f - ((accuracyMeters - 15f) / 35f).coerceAtMost(1f) * 0.3f // 0.3 ~ 0.0
    }
}
