package com.womensafety.app.ui.maps

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.womensafety.app.R
import com.womensafety.app.databinding.ActivityMapsBinding
import com.womensafety.app.utils.Constants

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var binding: ActivityMapsBinding
    private lateinit var googleMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    
    private var currentLocationMarker: Marker? = null
    private var sosEventMarkers = mutableListOf<Marker>()
    private var emergencyContactMarkers = mutableListOf<Marker>()
    
    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1001
        const val EXTRA_LATITUDE = "latitude"
        const val EXTRA_LONGITUDE = "longitude"
        const val EXTRA_TITLE = "title"
        const val EXTRA_SHOW_SOS_EVENTS = "show_sos_events"
        const val EXTRA_SHOW_EMERGENCY_CONTACTS = "show_emergency_contacts"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize location client
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Set up toolbar
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = intent.getStringExtra(EXTRA_TITLE) ?: "Map"

        // Initialize map
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        setupUI()
    }

    private fun setupUI() {
        binding.apply {
            // Map type toggle
            btnMapType.setOnClickListener {
                toggleMapType()
            }

            // Current location button
            btnCurrentLocation.setOnClickListener {
                getCurrentLocation()
            }

            // SOS button
            btnSos.setOnClickListener {
                triggerSosFromMap()
            }

            // Toggle SOS events
            btnToggleSosEvents.setOnClickListener {
                toggleSosEvents()
            }

            // Toggle emergency contacts
            btnToggleContacts.setOnClickListener {
                toggleEmergencyContacts()
            }
        }
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map

        // Configure map settings
        setupMapSettings()

        // Check for specific location from intent
        val latitude = intent.getDoubleExtra(EXTRA_LATITUDE, 0.0)
        val longitude = intent.getDoubleExtra(EXTRA_LONGITUDE, 0.0)

        if (latitude != 0.0 && longitude != 0.0) {
            // Show specific location
            val location = LatLng(latitude, longitude)
            addMarker(location, intent.getStringExtra(EXTRA_TITLE) ?: "Location", BitmapDescriptorFactory.HUE_RED)
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(location, Constants.DEFAULT_ZOOM_LEVEL))
        } else {
            // Show current location
            getCurrentLocation()
        }

        // Load additional data based on intent
        if (intent.getBooleanExtra(EXTRA_SHOW_SOS_EVENTS, false)) {
            loadSosEvents()
        }

        if (intent.getBooleanExtra(EXTRA_SHOW_EMERGENCY_CONTACTS, false)) {
            loadEmergencyContacts()
        }

        // Set up map click listener
        googleMap.setOnMapClickListener { latLng ->
            onMapClick(latLng)
        }

        // Set up marker click listener
        googleMap.setOnMarkerClickListener { marker ->
            onMarkerClick(marker)
        }
    }

    private fun setupMapSettings() {
        googleMap.apply {
            // Enable zoom controls
            uiSettings.isZoomControlsEnabled = false
            uiSettings.isCompassEnabled = true
            uiSettings.isMyLocationButtonEnabled = false

            // Set map type
            mapType = GoogleMap.MAP_TYPE_NORMAL

            // Enable location if permission granted
            enableMyLocation()
        }
    }

    private fun enableMyLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) 
            == PackageManager.PERMISSION_GRANTED) {
            googleMap.isMyLocationEnabled = true
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        }
    }

    private fun getCurrentLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) 
            != PackageManager.PERMISSION_GRANTED) {
            enableMyLocation()
            return
        }

        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            location?.let {
                val currentLatLng = LatLng(it.latitude, it.longitude)
                
                // Remove previous current location marker
                currentLocationMarker?.remove()
                
                // Add new current location marker
                currentLocationMarker = addMarker(
                    currentLatLng, 
                    "Current Location", 
                    BitmapDescriptorFactory.HUE_BLUE
                )
                
                // Move camera to current location
                googleMap.animateCamera(
                    CameraUpdateFactory.newLatLngZoom(currentLatLng, Constants.DEFAULT_ZOOM_LEVEL)
                )
            } ?: run {
                Toast.makeText(this, "Unable to get current location", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun addMarker(position: LatLng, title: String, color: Float): Marker? {
        return googleMap.addMarker(
            MarkerOptions()
                .position(position)
                .title(title)
                .icon(BitmapDescriptorFactory.defaultMarker(color))
        )
    }

    private fun toggleMapType() {
        googleMap.mapType = when (googleMap.mapType) {
            GoogleMap.MAP_TYPE_NORMAL -> GoogleMap.MAP_TYPE_SATELLITE
            GoogleMap.MAP_TYPE_SATELLITE -> GoogleMap.MAP_TYPE_HYBRID
            GoogleMap.MAP_TYPE_HYBRID -> GoogleMap.MAP_TYPE_TERRAIN
            else -> GoogleMap.MAP_TYPE_NORMAL
        }

        val mapTypeName = when (googleMap.mapType) {
            GoogleMap.MAP_TYPE_SATELLITE -> "Satellite"
            GoogleMap.MAP_TYPE_HYBRID -> "Hybrid"
            GoogleMap.MAP_TYPE_TERRAIN -> "Terrain"
            else -> "Normal"
        }
        
        Toast.makeText(this, "Map type: $mapTypeName", Toast.LENGTH_SHORT).show()
    }

    private fun triggerSosFromMap() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) 
            != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Location permission required for SOS", Toast.LENGTH_SHORT).show()
            return
        }

        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            location?.let {
                // TODO: Integrate with your SOS system
                val sosLocation = LatLng(it.latitude, it.longitude)
                
                // Add SOS marker
                addMarker(sosLocation, "SOS Alert", BitmapDescriptorFactory.HUE_RED)
                
                Toast.makeText(this, "SOS triggered at current location", Toast.LENGTH_SHORT).show()
                
                // You can add your SOS logic here
                // Example: sosRepository.triggerSos(it.latitude, it.longitude, "manual")
            }
        }
    }

    private fun loadSosEvents() {
        // TODO: Load SOS events from your database
        // This is a placeholder implementation
        val sampleSosEvents = listOf(
            LatLng(40.7128, -74.0060) to "SOS Event 1",
            LatLng(40.7589, -73.9851) to "SOS Event 2"
        )

        sosEventMarkers.clear()
        sampleSosEvents.forEach { (location, title) ->
            val marker = addMarker(location, title, BitmapDescriptorFactory.HUE_RED)
            marker?.let { sosEventMarkers.add(it) }
        }
    }

    private fun loadEmergencyContacts() {
        // TODO: Load emergency contact locations from your database
        // This is a placeholder implementation
        val sampleContacts = listOf(
            LatLng(40.7505, -73.9934) to "Emergency Contact 1",
            LatLng(40.7282, -73.7949) to "Emergency Contact 2"
        )

        emergencyContactMarkers.clear()
        sampleContacts.forEach { (location, title) ->
            val marker = addMarker(location, title, BitmapDescriptorFactory.HUE_GREEN)
            marker?.let { emergencyContactMarkers.add(it) }
        }
    }

    private fun toggleSosEvents() {
        if (sosEventMarkers.isEmpty()) {
            loadSosEvents()
            // Toggle button state - SOS events now visible
        } else {
            sosEventMarkers.forEach { it.remove() }
            sosEventMarkers.clear()
            // Toggle button state - SOS events now hidden
        }
    }

    private fun toggleEmergencyContacts() {
        if (emergencyContactMarkers.isEmpty()) {
            loadEmergencyContacts()
            // Toggle button state - contacts now visible
        } else {
            emergencyContactMarkers.forEach { it.remove() }
            emergencyContactMarkers.clear()
            // Toggle button state - contacts now hidden
        }
    }

    private fun onMapClick(latLng: LatLng) {
        // Add marker at clicked location
        addMarker(latLng, "Selected Location", BitmapDescriptorFactory.HUE_ORANGE)
        
        // Show coordinates
        Toast.makeText(
            this, 
            "Location: ${String.format("%.6f", latLng.latitude)}, ${String.format("%.6f", latLng.longitude)}", 
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun onMarkerClick(marker: Marker): Boolean {
        // Show marker info
        marker.showInfoWindow()
        
        // Move camera to marker
        googleMap.animateCamera(CameraUpdateFactory.newLatLng(marker.position))
        
        return true
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        
        when (requestCode) {
            LOCATION_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    enableMyLocation()
                    getCurrentLocation()
                } else {
                    Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        super.onBackPressedDispatcher.onBackPressed()
        return true
    }
}
