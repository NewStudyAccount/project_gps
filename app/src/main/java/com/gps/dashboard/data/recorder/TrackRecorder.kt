package com.gps.dashboard.data.recorder

import android.location.Location
import com.gps.dashboard.data.db.TrackingDatabase
import com.gps.dashboard.data.location.LocationStateHolder
import com.gps.dashboard.data.model.Track
import com.gps.dashboard.data.model.TrackPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 轨迹录制器：从 LocationStateHolder 收集 GPS 数据，按自适应策略写入 Room。
 */
class TrackRecorder(
    private val db: TrackingDatabase,
    private val sampler: AdaptiveSampler,
) {

    enum class State { IDLE, RECORDING, PAUSED }

    data class TrackStats(
        val duration: Long = 0L,       // 毫秒
        val distance: Float = 0f,      // 米
        val pointCount: Int = 0,
        val currentSpeed: Float = 0f,  // m/s
        val avgSpeed: Float = 0f,      // m/s
        val motionProfile: AdaptiveSampler.MotionProfile = AdaptiveSampler.MotionProfile.STATIONARY,
    )

    private val _state = MutableStateFlow(State.IDLE)
    val state: StateFlow<State> = _state.asStateFlow()

    private val _currentTrackStats = MutableStateFlow(TrackStats())
    val currentTrackStats: StateFlow<TrackStats> = _currentTrackStats.asStateFlow()

    private var currentTrackId: Long? = null
    private var job: Job? = null
    private var startTime = 0L
    private var totalDistance = 0f
    private var maxSpeed = 0f
    private var pointCount = 0
    private var lastLocation: Location? = null

    /**
     * 开始录制。创建 Track 记录并启动 GPS 收集协程。
     */
    fun startRecording(scope: CoroutineScope) {
        if (_state.value == State.RECORDING) return

        sampler.reset()
        startTime = System.currentTimeMillis()
        totalDistance = 0f
        maxSpeed = 0f
        pointCount = 0
        lastLocation = null

        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        val trackName = "${sdf.format(Date(startTime))} 轨迹"

        scope.launch {
            val track = Track(
                name = trackName,
                startTime = startTime,
            )
            currentTrackId = db.trackDao().insert(track)
            _state.value = State.RECORDING

            // 启动 GPS 收集
            job = scope.launch {
                LocationStateHolder.location.filterNotNull().collect { location ->
                    if (_state.value != State.RECORDING) return@collect
                    processLocation(location)
                }
            }
        }
    }

    /**
     * 暂停录制。
     */
    fun pauseRecording() {
        if (_state.value != State.RECORDING) return
        _state.value = State.PAUSED
    }

    /**
     * 恢复录制。
     */
    fun resumeRecording() {
        if (_state.value != State.PAUSED) return
        _state.value = State.RECORDING
    }

    /**
     * 停止录制。更新 Track 统计数据并返回 trackId。
     */
    suspend fun stopRecording(): Long? {
        val trackId = currentTrackId ?: return null

        job?.cancel()
        job = null

        val endTime = System.currentTimeMillis()
        val duration = endTime - startTime

        // 更新 Track 统计
        val track = db.trackDao().getById(trackId)
        if (track != null) {
            val avgSpeed = if (duration > 0) totalDistance / (duration / 1000f) else 0f
            db.trackDao().update(
                track.copy(
                    endTime = endTime,
                    totalDistance = totalDistance,
                    totalDuration = duration,
                    avgSpeed = avgSpeed,
                    maxSpeed = maxSpeed,
                    pointCount = pointCount,
                    avgAccuracy = 0f, // TODO: 计算平均精度
                )
            )
        }

        _state.value = State.IDLE
        _currentTrackStats.value = TrackStats()
        currentTrackId = null

        return trackId
    }

    /**
     * 获取当前 trackId。
     */
    fun getCurrentTrackId(): Long? = currentTrackId

    private suspend fun processLocation(location: Location) {
        if (!sampler.shouldRecord(location)) return

        val trackId = currentTrackId ?: return

        // 计算距离
        val last = lastLocation
        if (last != null) {
            val dist = location.distanceTo(last)
            totalDistance += dist
        }

        // 更新最大速度
        if (location.speed > maxSpeed) {
            maxSpeed = location.speed
        }

        // 写入 TrackPoint
        val point = TrackPoint(
            trackId = trackId,
            latitude = location.latitude,
            longitude = location.longitude,
            altitude = location.altitude,
            speed = location.speed,
            bearing = location.bearing,
            accuracy = location.accuracy,
            timestamp = location.time,
            isOriginal = true,
        )
        db.trackPointDao().insert(point)

        pointCount++
        lastLocation = location
        sampler.onRecorded(location)

        // 更新统计 Flow
        val duration = System.currentTimeMillis() - startTime
        _currentTrackStats.value = TrackStats(
            duration = duration,
            distance = totalDistance,
            pointCount = pointCount,
            currentSpeed = location.speed,
            avgSpeed = if (duration > 0) totalDistance / (duration / 1000f) else 0f,
            motionProfile = sampler.getCurrentProfile(),
        )
    }
}
