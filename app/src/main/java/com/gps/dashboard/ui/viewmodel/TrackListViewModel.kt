package com.gps.dashboard.ui.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gps.dashboard.GpsApplication
import com.gps.dashboard.data.model.Track
import com.gps.dashboard.data.repository.TrackRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TrackListViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as GpsApplication
    private val repository = TrackRepository(app.database)

    val tracks: StateFlow<List<Track>> = repository.getAllTracks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun deleteTrack(trackId: Long) {
        viewModelScope.launch {
            repository.deleteTrack(trackId)
        }
    }

    fun deleteAll() {
        viewModelScope.launch {
            repository.deleteAll()
        }
    }

    fun exportGpx(context: Context, trackId: Long, onResult: (Uri?) -> Unit) {
        viewModelScope.launch {
            val uri = repository.exportGpx(context, trackId)
            onResult(uri)
        }
    }

    fun exportKml(context: Context, trackId: Long, onResult: (Uri?) -> Unit) {
        viewModelScope.launch {
            val uri = repository.exportKml(context, trackId)
            onResult(uri)
        }
    }

    fun shareTrack(context: Context, uri: Uri, mimeType: String) {
        repository.shareTrack(context, uri, mimeType)
    }
}
