package com.womensafety.app.ui.home

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.womensafety.app.data.model.Contact
import com.womensafety.app.data.model.LocationData
import com.womensafety.app.data.repository.ContactRepository
import com.womensafety.app.data.repository.LocationRepository
import com.womensafety.app.data.repository.SosRepository
import kotlinx.coroutines.launch

class HomeViewModel(
    private val sosRepository: SosRepository,
    private val locationRepository: LocationRepository,
    private val contactRepository: ContactRepository
) : ViewModel() {
    
    private val _sosStatus = MutableLiveData<SosStatus>(SosStatus.Idle)
    val sosStatus: LiveData<SosStatus> = _sosStatus
    
    private val _currentLocation = MutableLiveData<LocationData?>()
    val currentLocation: LiveData<LocationData?> = _currentLocation
    
    private var activeSosEventId: String? = null
    
    sealed class SosStatus {
        object Idle : SosStatus()
        data class Countdown(val seconds: Int) : SosStatus()
        object Active : SosStatus()
        data class Error(val message: String) : SosStatus()
    }
    
    init {
        observeLocationUpdates()
    }
    
    private fun observeLocationUpdates() {
        locationRepository.currentLocation.observeForever { location ->
            _currentLocation.value = location
            
            // Save location to database if it's a tracking location
            location?.let { loc ->
                if (loc.isTracking) {
                    viewModelScope.launch {
                        try {
                            // Save to database
                            locationRepository.saveLocationToDatabase(loc)
                        } catch (e: Exception) {
                            // Handle database save error silently
                        }
                    }
                }
            }
        }
    }
    
    fun startLocationUpdates() {
        locationRepository.startLocationTracking()
    }
    
    fun stopLocationUpdates() {
        locationRepository.stopLocationTracking()
    }
    
    fun updateSosCountdown(seconds: Int) {
        _sosStatus.value = SosStatus.Countdown(seconds)
    }
    
    fun triggerSos(type: String) {
        viewModelScope.launch {
            try {
                val eventId = sosRepository.triggerSos(
                    type = type,
                    location = _currentLocation.value
                )
                activeSosEventId = eventId
                _sosStatus.value = SosStatus.Active
                
                // Save SOS location to database
                _currentLocation.value?.let { location ->
                    val sosLocation = location.copy(isSosLocation = true)
                    locationRepository.saveLocationToDatabase(sosLocation)
                }
            } catch (e: Exception) {
                _sosStatus.value = SosStatus.Error(e.message ?: "Failed to trigger SOS")
            }
        }
    }
    
    fun cancelActiveSos() {
        viewModelScope.launch {
            activeSosEventId?.let { eventId ->
                try {
                    sosRepository.cancelSos(eventId)
                    activeSosEventId = null
                    _sosStatus.value = SosStatus.Idle
                } catch (e: Exception) {
                    _sosStatus.value = SosStatus.Error(e.message ?: "Failed to cancel SOS")
                }
            }
        }
    }
    
    fun resetSosStatus() {
        _sosStatus.value = SosStatus.Idle
    }
    
    fun setSosError(message: String) {
        _sosStatus.value = SosStatus.Error(message)
    }
    
    fun makeEmergencyCall(context: Context, phoneNumber: String) {
        sosRepository.makeEmergencyCall(context, phoneNumber)
    }
    
    suspend fun shareCurrentLocation(context: Context) {
        val location = _currentLocation.value
        if (location != null) {
            val shareText = locationRepository.getLocationShareText(location)
            val shareIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, shareText)
                type = "text/plain"
            }
            val chooser = Intent.createChooser(shareIntent, "Share Location")
            chooser.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(chooser)
        }
    }
    
    fun sendLocationViaWhatsApp(context: Context) {
        viewModelScope.launch {
            try {
                val location = _currentLocation.value
                if (location != null) {
                    // Get all contacts
                    val contacts = contactRepository.getAllContacts().value ?: emptyList()
                    
                    // Filter out emergency service contacts
                    val nonEmergencyContacts = contacts.filter { !it.isEmergencyService }
                    
                    if (nonEmergencyContacts.isNotEmpty()) {
                        // Create a Google Maps link
                        val mapLink = "https://www.google.com/maps?q=${location.latitude},${location.longitude}"
                        
                        // Create the message
                        val message = "🚨 Emergency - My current location: $mapLink"
                        
                        // Create the intent to send the message via WhatsApp
                        val sendIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, message)
                            type = "text/plain"
                            setPackage("com.whatsapp") // Specify WhatsApp package
                        }
                        
                        sendIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        
                        // Check if WhatsApp is installed
                        if (sendIntent.resolveActivity(context.packageManager) != null) {
                            context.startActivity(sendIntent)
                        } else {
                            // Fallback to general share if WhatsApp is not installed
                            val shareIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, message)
                                type = "text/plain"
                            }
                            val chooser = Intent.createChooser(shareIntent, "Send Location via")
                            chooser.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            context.startActivity(chooser)
                        }
                    } else {
                        // Handle case where there are no contacts
                        // Show a toast message
                        (context as? androidx.appcompat.app.AppCompatActivity)?.runOnUiThread {
                            Toast.makeText(context, "No saved contacts found", Toast.LENGTH_LONG).show()
                        }
                    }
                } else {
                    // Handle case where location is not available
                    // Show a toast message
                    (context as? androidx.appcompat.app.AppCompatActivity)?.runOnUiThread {
                        Toast.makeText(context, "Location not available", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                // Handle any errors
                e.printStackTrace()
                // Show error message
                (context as? androidx.appcompat.app.AppCompatActivity)?.runOnUiThread {
                    Toast.makeText(context, "Failed to send location", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        stopLocationUpdates()
    }
}
