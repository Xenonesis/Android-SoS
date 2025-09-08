package com.womensafety.app.data.repository

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Build
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import android.os.Looper
import androidx.core.content.ContextCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.android.gms.location.*
import com.womensafety.app.data.dao.LocationDao
import com.womensafety.app.data.model.LocationData
import com.womensafety.app.utils.Constants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*

class LocationRepository(private val context: Context) {
    
    private val fusedLocationClient: FusedLocationProviderClient = 
        LocationServices.getFusedLocationProviderClient(context)
    
    private val geocoder = Geocoder(context, Locale.getDefault())
    
    private val _currentLocation = MutableLiveData<LocationData?>()
    val currentLocation: LiveData<LocationData?> = _currentLocation
    
    private val _isTracking = MutableLiveData<Boolean>(false)
    val isTracking: LiveData<Boolean> = _isTracking
    
    private var locationCallback: LocationCallback? = null
    
    suspend fun getCurrentLocation(): LocationData? = withContext(Dispatchers.IO) {
        if (!hasLocationPermission()) {
            return@withContext null
        }
        
        try {
            val location = fusedLocationClient.lastLocation.result
            location?.let { loc ->
                val address = getAddressFromLocation(loc.latitude, loc.longitude)
                LocationData(
                    latitude = loc.latitude,
                    longitude = loc.longitude,
                    accuracy = loc.accuracy,
                    altitude = if (loc.hasAltitude()) loc.altitude else null,
                    speed = if (loc.hasSpeed()) loc.speed else null,
                    bearing = if (loc.hasBearing()) loc.bearing else null,
                    address = address,
                    timestamp = System.currentTimeMillis()
                )
            }
        } catch (e: SecurityException) {
            null
        }
    }
    
    fun startLocationTracking() {
        if (!hasLocationPermission()) {
            return
        }
        
        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            Constants.LOCATION_UPDATE_INTERVAL
        ).apply {
            setMinUpdateIntervalMillis(Constants.LOCATION_FASTEST_INTERVAL)
            setMinUpdateDistanceMeters(Constants.LOCATION_DISPLACEMENT)
        }.build()
        
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    val locationData = LocationData(
                        latitude = location.latitude,
                        longitude = location.longitude,
                        accuracy = location.accuracy,
                        altitude = if (location.hasAltitude()) location.altitude else null,
                        speed = if (location.hasSpeed()) location.speed else null,
                        bearing = if (location.hasBearing()) location.bearing else null,
                        timestamp = System.currentTimeMillis(),
                        isTracking = true
                    )
                    
                    _currentLocation.postValue(locationData)
                }
            }
        }
        
        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback!!,
                Looper.getMainLooper()
            )
            _isTracking.value = true
        } catch (e: SecurityException) {
            // Handle permission error
        }
    }
    
    fun stopLocationTracking() {
        locationCallback?.let {
            fusedLocationClient.removeLocationUpdates(it)
        }
        locationCallback = null
        _isTracking.value = false
    }
    
    suspend fun getAddressFromLocation(latitude: Double, longitude: Double): String? =
        withContext(Dispatchers.IO) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    suspendCancellableCoroutine { continuation ->
                        geocoder.getFromLocation(latitude, longitude, 1) { addresses ->
                            continuation.resume(addresses.firstOrNull()?.toAddressString())
                        }
                    }
                } else {
                    @Suppress("DEPRECATION")
                    val addresses = geocoder.getFromLocation(latitude, longitude, 1)
                    addresses?.firstOrNull()?.toAddressString()
                }
            } catch (e: Exception) {
                null
            }
        }

    private fun Address.toAddressString(): String {
        return buildString {
            if (thoroughfare != null) {
                append(thoroughfare)
                if (subThoroughfare != null) {
                    append(" ${subThoroughfare}")
                }
                append(", ")
            }
            if (locality != null) {
                append("${locality}, ")
            }
            if (adminArea != null) {
                append("${adminArea} ")
            }
            if (postalCode != null) {
                append(postalCode)
            }
        }.trim().removeSuffix(",")
    }
    
    fun calculateDistance(
        lat1: Double, lon1: Double,
        lat2: Double, lon2: Double
    ): Float {
        val results = FloatArray(1)
        Location.distanceBetween(lat1, lon1, lat2, lon2, results)
        return results[0]
    }
    
    fun isLocationEnabled(): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as android.location.LocationManager
        return locationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER) ||
               locationManager.isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER)
    }
    
    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    fun getGoogleMapsUrl(latitude: Double, longitude: Double): String {
        return "https://maps.google.com/?q=$latitude,$longitude"
    }
    
    fun getLocationShareText(locationData: LocationData): String {
        val mapsUrl = getGoogleMapsUrl(locationData.latitude, locationData.longitude)
        val address = locationData.address ?: "Unknown location"
        return "My current location: $address\n$mapsUrl"
    }
    
    suspend fun saveLocationToDatabase(locationData: LocationData) {
        withContext(Dispatchers.IO) {
            try {
                // This would be injected in a real app, but for now we'll get it from context
                val database = com.womensafety.app.data.database.SosDatabase.getDatabase(context)
                database.locationDao().insertLocation(locationData)
            } catch (e: Exception) {
                // Handle database save error
            }
        }
    }
}
