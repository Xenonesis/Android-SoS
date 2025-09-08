package com.womensafety.app.utils

import android.content.Context
import android.content.Intent
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.net.Uri
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import android.util.Log
import com.google.android.gms.maps.model.LatLng
import com.womensafety.app.ui.maps.MapsActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*

/**
 * Helper class for Google Maps integration and location utilities
 */
object MapsHelper {
    
    private const val TAG = "MapsHelper"
    
    /**
     * Launch Maps Activity with specific location
     */
    fun showLocationOnMap(
        context: Context,
        latitude: Double,
        longitude: Double,
        title: String = "Location"
    ) {
        val intent = Intent(context, MapsActivity::class.java).apply {
            putExtra(MapsActivity.EXTRA_LATITUDE, latitude)
            putExtra(MapsActivity.EXTRA_LONGITUDE, longitude)
            putExtra(MapsActivity.EXTRA_TITLE, title)
        }
        context.startActivity(intent)
    }
    
    /**
     * Launch Maps Activity showing SOS events
     */
    fun showSosEventsMap(context: Context) {
        val intent = Intent(context, MapsActivity::class.java).apply {
            putExtra(MapsActivity.EXTRA_SHOW_SOS_EVENTS, true)
            putExtra(MapsActivity.EXTRA_TITLE, "SOS Events")
        }
        context.startActivity(intent)
    }
    
    /**
     * Launch Maps Activity showing emergency contacts
     */
    fun showEmergencyContactsMap(context: Context) {
        val intent = Intent(context, MapsActivity::class.java).apply {
            putExtra(MapsActivity.EXTRA_SHOW_EMERGENCY_CONTACTS, true)
            putExtra(MapsActivity.EXTRA_TITLE, "Emergency Contacts")
        }
        context.startActivity(intent)
    }
    
    /**
     * Open location in Google Maps app
     */
    fun openInGoogleMaps(context: Context, latitude: Double, longitude: Double, label: String = "") {
        val uri = if (label.isNotEmpty()) {
            Uri.parse("geo:$latitude,$longitude?q=$latitude,$longitude($label)")
        } else {
            Uri.parse("geo:$latitude,$longitude")
        }
        
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            setPackage("com.google.android.apps.maps")
        }
        
        if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent)
        } else {
            // Fallback to web version
            val webUri = Uri.parse("https://maps.google.com/?q=$latitude,$longitude")
            val webIntent = Intent(Intent.ACTION_VIEW, webUri)
            context.startActivity(webIntent)
        }
    }
    
    /**
     * Get directions to a location using Google Maps
     */
    fun getDirections(
        context: Context,
        destinationLat: Double,
        destinationLng: Double,
        destinationName: String = ""
    ) {
        val uri = Uri.parse("google.navigation:q=$destinationLat,$destinationLng")
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            setPackage("com.google.android.apps.maps")
        }
        
        if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent)
        } else {
            // Fallback to web directions
            val webUri = Uri.parse("https://maps.google.com/maps?daddr=$destinationLat,$destinationLng")
            val webIntent = Intent(Intent.ACTION_VIEW, webUri)
            context.startActivity(webIntent)
        }
    }
    
    /**
     * Share location via other apps
     */
    fun shareLocation(
        context: Context,
        latitude: Double,
        longitude: Double,
        message: String = "My current location"
    ) {
        val locationUrl = "https://maps.google.com/?q=$latitude,$longitude"
        val shareText = "$message\n$locationUrl"
        
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, shareText)
            putExtra(Intent.EXTRA_SUBJECT, "Location Share")
        }
        
        context.startActivity(Intent.createChooser(intent, "Share Location"))
    }
    
    /**
     * Calculate distance between two locations
     */
    fun calculateDistance(
        lat1: Double, lng1: Double,
        lat2: Double, lng2: Double
    ): Float {
        val results = FloatArray(1)
        Location.distanceBetween(lat1, lng1, lat2, lng2, results)
        return results[0]
    }
    
    /**
     * Format distance for display
     */
    fun formatDistance(distanceInMeters: Float): String {
        return when {
            distanceInMeters < 1000 -> "${distanceInMeters.toInt()} m"
            distanceInMeters < 10000 -> String.format("%.1f km", distanceInMeters / 1000)
            else -> "${(distanceInMeters / 1000).toInt()} km"
        }
    }
    
    /**
     * Get address from coordinates using Geocoder
     */
    suspend fun getAddressFromLocation(
        context: Context,
        latitude: Double,
        longitude: Double
    ): String? = withContext(Dispatchers.IO) {
        val geocoder = Geocoder(context, Locale.getDefault())
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
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
            Log.e(TAG, "Error getting address from location", e)
            null
        }
    }
    
    /**
     * Get coordinates from address using Geocoder
     */
    suspend fun getLocationFromAddress(
        context: Context,
        address: String
    ): LatLng? = withContext(Dispatchers.IO) {
        val geocoder = Geocoder(context, Locale.getDefault())
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                suspendCancellableCoroutine { continuation ->
                    geocoder.getFromLocationName(address, 1) { addresses ->
                        continuation.resume(addresses.firstOrNull()?.let { LatLng(it.latitude, it.longitude) })
                    }
                }
            } else {
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocationName(address, 1)
                addresses?.firstOrNull()?.let { LatLng(it.latitude, it.longitude) }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting location from address", e)
            null
        }
    }

    private fun Address.toAddressString(): String {
        return buildString {
            if (featureName != null) append("$featureName, ")
            if (thoroughfare != null) append("$thoroughfare, ")
            if (locality != null) append("$locality, ")
            if (adminArea != null) append("$adminArea, ")
            if (countryName != null) append(countryName)
        }.trimEnd(',', ' ')
    }
    
    /**
     * Check if location is within a safe zone
     */
    fun isLocationInSafeZone(
        userLat: Double,
        userLng: Double,
        safeZones: List<SafeZone>
    ): SafeZone? {
        return safeZones.find { safeZone ->
            val distance = calculateDistance(
                userLat, userLng,
                safeZone.latitude, safeZone.longitude
            )
            distance <= safeZone.radiusInMeters
        }
    }
    
    /**
     * Find nearest emergency services
     */
    fun findNearestEmergencyServices(
        userLat: Double,
        userLng: Double,
        emergencyServices: List<EmergencyService>
    ): List<EmergencyService> {
        return emergencyServices.map { service ->
            val distance = calculateDistance(
                userLat, userLng,
                service.latitude, service.longitude
            )
            service.copy(distanceInMeters = distance)
        }.sortedBy { it.distanceInMeters }
    }
    
    /**
     * Create a location sharing message for SOS
     */
    fun createSosLocationMessage(
        latitude: Double,
        longitude: Double,
        address: String? = null,
        userName: String = "Someone"
    ): String {
        val locationUrl = "https://maps.google.com/?q=$latitude,$longitude"
        val addressText = address?.let { " at $it" } ?: ""
        
        return """
            🚨 EMERGENCY ALERT 🚨
            
            $userName needs help$addressText
            
            Location: $locationUrl
            Coordinates: $latitude, $longitude
            
            Please respond immediately or contact emergency services.
        """.trimIndent()
    }
    
    /**
     * Validate coordinates
     */
    fun isValidCoordinates(latitude: Double, longitude: Double): Boolean {
        return latitude in -90.0..90.0 && longitude in -180.0..180.0
    }
}

/**
 * Data classes for Maps functionality
 */
data class SafeZone(
    val id: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val radiusInMeters: Float,
    val type: String // "police_station", "hospital", "safe_house", etc.
)

data class EmergencyService(
    val id: String,
    val name: String,
    val type: String, // "police", "hospital", "fire_station"
    val latitude: Double,
    val longitude: Double,
    val phone: String,
    val address: String,
    val distanceInMeters: Float = 0f
)
