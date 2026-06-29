package com.gps.dashboard.data.recorder

import android.location.Location

/**
 * 自适应采样策略：根据速度自动调整采集频率，平衡精度与电量。
 *
 * 采样规则 (同时满足时间间隔 AND 距离阈才记录，静止状态仅按时间):
 *   STATIONARY (< 0.5 m/s):  每 30s，不检查距离
 *   WALKING    (< 3 m/s):    每 5s，距离 ≥ 5m
 *   CYCLING    (< 10 m/s):   每 3s，距离 ≥ 10m
 *   DRIVING    (≥ 10 m/s):   每 1s，距离 ≥ 20m
 */
class AdaptiveSampler {

    data class SamplingConfig(
        val minIntervalMs: Long,
        val minDistanceM: Float,
    )

    enum class MotionProfile(val label: String) {
        STATIONARY("静止"),
        WALKING("步行"),
        CYCLING("骑行"),
        DRIVING("驾驶"),
    }

    private val configs = mapOf(
        MotionProfile.STATIONARY to SamplingConfig(30_000L, Float.MAX_VALUE),
        MotionProfile.WALKING to SamplingConfig(5_000L, 5f),
        MotionProfile.CYCLING to SamplingConfig(3_000L, 10f),
        MotionProfile.DRIVING to SamplingConfig(1_000L, 20f),
    )

    private var lastRecordedTime = 0L
    private var lastRecordedLocation: Location? = null
    private var currentProfile = MotionProfile.STATIONARY

    /**
     * 判断是否应该记录这个 GPS 点。
     */
    fun shouldRecord(location: Location): Boolean {
        val now = System.currentTimeMillis()
        val profile = classifyMotion(location.speed)
        currentProfile = profile
        val config = configs[profile]!!

        // 时间间隔检查
        if (now - lastRecordedTime < config.minIntervalMs) return false

        // 距离检查 (静止状态跳过距离判断)
        if (profile != MotionProfile.STATIONARY) {
            val last = lastRecordedLocation ?: return true
            if (location.distanceTo(last) < config.minDistanceM) return false
        }

        return true
    }

    /**
     * 记录成功后更新内部状态。
     */
    fun onRecorded(location: Location) {
        lastRecordedTime = System.currentTimeMillis()
        lastRecordedLocation = location
    }

    /**
     * 获取当前运动状态。
     */
    fun getCurrentProfile(): MotionProfile = currentProfile

    /**
     * 重置采样器状态。
     */
    fun reset() {
        lastRecordedTime = 0L
        lastRecordedLocation = null
        currentProfile = MotionProfile.STATIONARY
    }

    private fun classifyMotion(speed: Float): MotionProfile {
        return when {
            speed < 0.5f -> MotionProfile.STATIONARY
            speed < 3f -> MotionProfile.WALKING
            speed < 10f -> MotionProfile.CYCLING
            else -> MotionProfile.DRIVING
        }
    }
}
