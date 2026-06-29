package com.gps.dashboard.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gps.dashboard.GpsApplication
import com.gps.dashboard.backtrack.BacktrackEngine
import com.gps.dashboard.backtrack.BacktrackState
import com.gps.dashboard.backtrack.Status
import com.gps.dashboard.data.location.LocationStateHolder
import com.gps.dashboard.data.model.TrackPoint
import com.gps.dashboard.data.repository.TrackRepository
import com.gps.dashboard.ui.component.TrackProjection
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch

class BacktrackViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = TrackRepository((application as GpsApplication).database)

    private val _trackPoints = MutableStateFlow<List<TrackPoint>>(emptyList())
    val trackPoints: StateFlow<List<TrackPoint>> = _trackPoints.asStateFlow()

    private val _latLngPoints = MutableStateFlow<List<TrackProjection.LatLng>>(emptyList())
    val latLngPoints: StateFlow<List<TrackProjection.LatLng>> = _latLngPoints.asStateFlow()

    private val _backtrackState = MutableStateFlow(BacktrackState())
    val backtrackState: StateFlow<BacktrackState> = _backtrackState.asStateFlow()

    private val _isBacktracking = MutableStateFlow(false)
    val isBacktracking: StateFlow<Boolean> = _isBacktracking.asStateFlow()

    private var engine: BacktrackEngine? = null
    private var locationJob: Job? = null

    fun loadTrack(trackId: Long) {
        viewModelScope.launch {
            val points = repository.getTrackPoints(trackId)
            _trackPoints.value = points
            _latLngPoints.value = points.map {
                TrackProjection.LatLng(it.latitude, it.longitude)
            }
            engine = BacktrackEngine(_latLngPoints.value)
        }
    }

    fun startBacktrack() {
        if (_latLngPoints.value.size < 2) return
        _isBacktracking.value = true
        _backtrackState.value = BacktrackState()

        locationJob?.cancel()
        locationJob = viewModelScope.launch {
            LocationStateHolder.location.filterNotNull().collect { location ->
                if (!_isBacktracking.value) return@collect
                val state = engine?.update(location.latitude, location.longitude)
                    ?: return@collect
                _backtrackState.value = state

                if (state.status == Status.COMPLETE) {
                    _isBacktracking.value = false
                }
            }
        }
    }

    fun stopBacktrack() {
        locationJob?.cancel()
        _isBacktracking.value = false
        _backtrackState.value = BacktrackState()
    }
}
