package com.gps.dashboard.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gps.dashboard.GpsApplication
import com.gps.dashboard.data.db.TrackingDatabase
import com.gps.dashboard.data.location.LocationStateHolder
import com.gps.dashboard.data.recorder.AdaptiveSampler
import com.gps.dashboard.data.recorder.TrackRecorder
import com.gps.dashboard.data.repository.TrackRepository
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class TrackingViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as GpsApplication
    private val db = app.database
    private val sampler = AdaptiveSampler()
    private val recorder = TrackRecorder(db, sampler)
    private val repository = TrackRepository(db)

    val recordingState: StateFlow<TrackRecorder.State> = recorder.state
    val trackStats: StateFlow<TrackRecorder.TrackStats> = recorder.currentTrackStats

    fun startRecording() {
        recorder.startRecording(viewModelScope)
    }

    fun pauseRecording() {
        recorder.pauseRecording()
    }

    fun resumeRecording() {
        recorder.resumeRecording()
    }

    fun stopRecording() {
        viewModelScope.launch {
            recorder.stopRecording()
        }
    }
}
