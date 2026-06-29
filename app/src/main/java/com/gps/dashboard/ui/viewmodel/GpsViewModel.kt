package com.gps.dashboard.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gps.dashboard.data.buffer.RingBuffer
import com.gps.dashboard.data.location.LocationStateHolder
import com.gps.dashboard.data.model.*
import com.gps.dashboard.data.repository.SatelliteRepository
import com.gps.dashboard.data.repository.SensorRepository
import com.gps.dashboard.util.AccuracyEvaluator
import com.gps.dashboard.util.AccuracyLevel
import com.gps.dashboard.util.CoordinateFormatter
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class GpsViewModel(application: Application) : AndroidViewModel(application) {

    private val sensorRepo = SensorRepository(application)
    private val satelliteRepo = SatelliteRepository(application)

    // GPS data
    private val _gpsData = MutableStateFlow(GpsData())
    val gpsData: StateFlow<GpsData> = _gpsData.asStateFlow()

    // Compass
    private val _compassHeading = MutableStateFlow(0f)
    val compassHeading: StateFlow<Float> = _compassHeading.asStateFlow()

    // Satellites
    private val _satellites = MutableStateFlow<List<SatelliteInfo>>(emptyList())
    val satellites: StateFlow<List<SatelliteInfo>> = _satellites.asStateFlow()

    // Altitude history
    private val altitudeBuffer = RingBuffer<Float>(60)
    private val _altitudeHistory = MutableStateFlow<List<Float>>(emptyList())
    val altitudeHistory: StateFlow<List<Float>> = _altitudeHistory.asStateFlow()

    // Speed unit
    private val _speedUnit = MutableStateFlow(SpeedUnit.KMH)
    val speedUnit: StateFlow<SpeedUnit> = _speedUnit.asStateFlow()

    // Satellite panel expanded
    private val _satelliteExpanded = MutableStateFlow(false)
    val satelliteExpanded: StateFlow<Boolean> = _satelliteExpanded.asStateFlow()

    // Permission state
    private val _permissionGranted = MutableStateFlow(false)
    val permissionGranted: StateFlow<Boolean> = _permissionGranted.asStateFlow()

    // Formatted values
    val formattedLatitude: StateFlow<String> = gpsData.map { data ->
        if (data.latitude == 0.0 && data.longitude == 0.0) "---"
        else CoordinateFormatter.toDMS(data.latitude, isLat = true)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "---")

    val formattedLongitude: StateFlow<String> = gpsData.map { data ->
        if (data.latitude == 0.0 && data.longitude == 0.0) "---"
        else CoordinateFormatter.toDMS(data.longitude, isLat = false)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "---")

    val formattedSpeed: StateFlow<String> = gpsData.map { data ->
        com.gps.dashboard.util.SpeedConverter.format(data.speed, _speedUnit.value)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "0.0 km/h")

    val accuracyLevel: StateFlow<AccuracyLevel> = gpsData.map { data ->
        AccuracyEvaluator.evaluate(data.accuracy)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AccuracyLevel.BAD)

    val accuracyRatio: StateFlow<Float> = gpsData.map { data ->
        AccuracyEvaluator.ratio(data.accuracy)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0f)

    val satelliteInUseCount: StateFlow<Int> = satellites.map { list ->
        list.count { it.inUse }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val constellationStats: StateFlow<Map<Constellation, Int>> = satellites.map { list ->
        list.filter { it.inUse }.groupBy { it.constellation }.mapValues { it.value.size }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val constellationTotalStats: StateFlow<Map<Constellation, Int>> = satellites.map { list ->
        list.groupBy { it.constellation }.mapValues { it.value.size }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val averageCn0: StateFlow<Float> = satellites.map { list ->
        val inUse = list.filter { it.inUse }
        if (inUse.isEmpty()) 0f else inUse.map { it.cn0 }.average().toFloat()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0f)

    val fixTypeText: StateFlow<String> = gpsData.map { data ->
        when (data.fixType) {
            FixType.FIX_3D -> "3D FIX"
            FixType.FIX_2D -> "2D FIX"
            FixType.NONE -> "NO FIX"
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "NO FIX")

    val providerText: StateFlow<String> = gpsData.map { data ->
        if (data.provider.isEmpty()) "---" else data.provider.joinToString("+")
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "---")

    val utcTimeText: StateFlow<String> = gpsData.map { data ->
        if (data.timestamp == 0L) "--:--:--"
        else {
            val sdf = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.US)
            sdf.timeZone = java.util.TimeZone.getTimeZone("Asia/Shanghai")
            sdf.format(java.util.Date(data.timestamp))
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "--:--:--")

    val hdopText: StateFlow<String> = gpsData.map { data ->
        if (data.hdop == 0f) "---" else "%.1f".format(data.hdop)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "---")

    val altitudeText: StateFlow<String> = gpsData.map { data ->
        if (data.altitude == 0.0) "---" else "%.1f".format(data.altitude)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "---")

    init {
        startLocationCollection()
        startCompassCollection()
        startSatelliteCollection()
        startAltitudeSampling()
    }

    fun onPermissionResult(granted: Boolean) {
        _permissionGranted.value = granted
    }

    fun setSpeedUnit(unit: SpeedUnit) {
        _speedUnit.value = unit
    }

    fun toggleSatellitePanel() {
        _satelliteExpanded.value = !_satelliteExpanded.value
    }

    private fun startLocationCollection() {
        viewModelScope.launch {
            LocationStateHolder.location.filterNotNull().collect { location ->
                sensorRepo.updateLocationForDeclination(
                    location.latitude, location.longitude, location.altitude
                )

                val provider = mutableSetOf<String>()
                if (location.provider == android.location.LocationManager.GPS_PROVIDER) provider.add("GPS")

                _gpsData.value = GpsData(
                    latitude = location.latitude,
                    longitude = location.longitude,
                    altitude = location.altitude,
                    speed = location.speed,
                    bearing = location.bearing,
                    accuracy = location.accuracy,
                    hdop = 0f, // not available from Location directly
                    fixType = if (location.hasAltitude()) FixType.FIX_3D else FixType.FIX_2D,
                    provider = provider,
                    timestamp = location.time,
                    trueBearing = location.bearing,
                )
            }
        }
    }

    private fun startCompassCollection() {
        viewModelScope.launch {
            sensorRepo.compassFlow.collect { compassData ->
                _compassHeading.value = compassData.heading
            }
        }
    }

    private fun startSatelliteCollection() {
        viewModelScope.launch {
            satelliteRepo.satelliteFlow.collect { list ->
                _satellites.value = list
            }
        }
    }

    private fun startAltitudeSampling() {
        viewModelScope.launch {
            while (true) {
                delay(1000L)
                val alt = _gpsData.value.altitude.toFloat()
                if (alt != 0f || !altitudeBuffer.isEmpty()) {
                    altitudeBuffer.add(alt)
                    _altitudeHistory.value = altitudeBuffer.toList()
                }
            }
        }
    }

}
