package com.gps.dashboard.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gps.dashboard.GpsApplication
import com.gps.dashboard.data.model.TrackPoint
import com.gps.dashboard.data.repository.TrackRepository
import com.gps.dashboard.ui.component.TrackProjection
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ReplayViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = TrackRepository((application as GpsApplication).database)

    private val _points = MutableStateFlow<List<TrackPoint>>(emptyList())
    val points: StateFlow<List<TrackPoint>> = _points.asStateFlow()

    private val _latLngPoints = MutableStateFlow<List<TrackProjection.LatLng>>(emptyList())
    val latLngPoints: StateFlow<List<TrackProjection.LatLng>> = _latLngPoints.asStateFlow()

    private val _currentIndex = MutableStateFlow(0)
    val currentIndex: StateFlow<Int> = _currentIndex.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _playbackSpeed = MutableStateFlow(1f) // 0.5x, 1x, 2x, 4x
    val playbackSpeed: StateFlow<Float> = _playbackSpeed.asStateFlow()

    private var playbackJob: Job? = null

    fun loadTrack(trackId: Long) {
        viewModelScope.launch {
            val trackPoints = repository.getTrackPoints(trackId)
            _points.value = trackPoints
            _latLngPoints.value = trackPoints.map {
                TrackProjection.LatLng(it.latitude, it.longitude)
            }
            _currentIndex.value = 0
        }
    }

    fun togglePlayPause() {
        if (_isPlaying.value) {
            pause()
        } else {
            play()
        }
    }

    fun play() {
        if (_points.value.isEmpty()) return
        _isPlaying.value = true

        playbackJob?.cancel()
        playbackJob = viewModelScope.launch {
            while (_currentIndex.value < _points.value.size - 1) {
                val baseDelay = calculateBaseDelay()
                val delayMs = (baseDelay / _playbackSpeed.value).toLong().coerceAtLeast(50L)
                delay(delayMs)
                _currentIndex.value = (_currentIndex.value + 1).coerceAtMost(_points.value.size - 1)
            }
            _isPlaying.value = false
        }
    }

    fun pause() {
        playbackJob?.cancel()
        _isPlaying.value = false
    }

    fun seekTo(index: Int) {
        _currentIndex.value = index.coerceIn(0, _points.value.size - 1)
    }

    fun setSpeed(speed: Float) {
        _playbackSpeed.value = speed
        // 如果正在播放，重启以应用新速度
        if (_isPlaying.value) {
            pause()
            play()
        }
    }

    fun skipToPrevious() {
        val points = _points.value
        if (points.isEmpty()) return
        val current = _currentIndex.value
        // 找前一个关键点（间隔至少 10 个点）
        val target = (current - 10).coerceAtLeast(0)
        _currentIndex.value = target
    }

    fun skipToNext() {
        val points = _points.value
        if (points.isEmpty()) return
        val current = _currentIndex.value
        // 找后一个关键点（间隔至少 10 个点）
        val target = (current + 10).coerceAtMost(points.size - 1)
        _currentIndex.value = target
    }

    /**
     * 计算基础延迟：根据两点间的时间差。
     */
    private fun calculateBaseDelay(): Long {
        val points = _points.value
        val idx = _currentIndex.value
        if (idx >= points.size - 1) return 1000L

        val timeDiff = points[idx + 1].timestamp - points[idx].timestamp
        return timeDiff.coerceIn(100L, 5000L) // 限制在 100ms ~ 5s
    }

    fun getCurrentPoint(): TrackPoint? {
        val points = _points.value
        val idx = _currentIndex.value
        return if (idx in points.indices) points[idx] else null
    }
}
