package com.example.urban_safety.viewmodels

import android.content.Context
import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.urban_safety.data.model.LocationData
import com.example.urban_safety.services.LocationService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LocationViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {
    private val locationService = LocationService(context)
    private val _currentLocation = MutableStateFlow<LocationData?>(null)
    val currentLocation: StateFlow<LocationData?> = _currentLocation.asStateFlow()

    init {
        startLocationUpdates()
    }

    private fun startLocationUpdates() {
        viewModelScope.launch {
            locationService.getLocationUpdates().collect { location ->
                _currentLocation.value = LocationData.fromLocation(location)
            }
        }
    }

    fun getLastKnownLocation(): LocationData? {
        val location = locationService.getLastKnownLocation()
        return location?.let { LocationData.fromLocation(it) }
    }

    fun stopLocationUpdates() {
        locationService.stopLocationUpdates()
    }

    fun updateLocation(location: Location) {
        _currentLocation.value = LocationData.fromLocation(location)
    }

    fun clearLocation() {
        _currentLocation.value = null
    }
} 