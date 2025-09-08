package com.womensafety.app.testing

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.womensafety.app.databinding.ActivityMapsTestBinding
import com.womensafety.app.utils.MapsHelper
import com.womensafety.app.utils.ApiKeyValidator

class MapsTestActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityMapsTestBinding
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMapsTestBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupUI()
    }
    
    private fun setupUI() {
        // Show initial configuration status
        showConfigurationStatus()
        
        binding.apply {
            btnTestCurrentLocation.setOnClickListener {
                testCurrentLocationMap()
            }
            
            btnTestSpecificLocation.setOnClickListener {
                testSpecificLocationMap()
            }
            
            btnTestSosEvents.setOnClickListener {
                testSosEventsMap()
            }
            
            btnTestEmergencyContacts.setOnClickListener {
                testEmergencyContactsMap()
            }
            
            btnTestLocationSharing.setOnClickListener {
                testLocationSharing()
            }
            
            btnTestGoogleMapsApp.setOnClickListener {
                testGoogleMapsApp()
            }
            
            btnTestDirections.setOnClickListener {
                testDirections()
            }
            
            btnTestDistanceCalculation.setOnClickListener {
                testDistanceCalculation()
            }
            
            btnValidateApiKey.setOnClickListener {
                validateApiKeyConfiguration()
            }
        }
    }
    
    private fun showConfigurationStatus() {
        val status = ApiKeyValidator.getMapsConfigurationStatus(this)
        showResult(status.getStatusMessage())
    }
    
    private fun validateApiKeyConfiguration() {
        val status = ApiKeyValidator.getMapsConfigurationStatus(this)
        showResult("🔍 API Key Validation Results:")
        showResult(status.getStatusMessage())
        
        if (status.overallStatus) {
            showResult("🎉 Maps configuration is ready for testing!")
        } else {
            showResult("⚠️ Please fix configuration issues before testing Maps functionality")
        }
    }
    
    private fun testCurrentLocationMap() {
        try {
            startActivity(android.content.Intent(this, com.womensafety.app.ui.maps.MapsActivity::class.java))
            showResult("✅ Current Location Map: Launched successfully")
        } catch (e: Exception) {
            showResult("❌ Current Location Map: Failed - ${e.message}")
        }
    }
    
    private fun testSpecificLocationMap() {
        try {
            // Test with New York coordinates
            MapsHelper.showLocationOnMap(
                this,
                40.7128,
                -74.0060,
                "Test Location - New York"
            )
            showResult("✅ Specific Location Map: Launched successfully")
        } catch (e: Exception) {
            showResult("❌ Specific Location Map: Failed - ${e.message}")
        }
    }
    
    private fun testSosEventsMap() {
        try {
            MapsHelper.showSosEventsMap(this)
            showResult("✅ SOS Events Map: Launched successfully")
        } catch (e: Exception) {
            showResult("❌ SOS Events Map: Failed - ${e.message}")
        }
    }
    
    private fun testEmergencyContactsMap() {
        try {
            MapsHelper.showEmergencyContactsMap(this)
            showResult("✅ Emergency Contacts Map: Launched successfully")
        } catch (e: Exception) {
            showResult("❌ Emergency Contacts Map: Failed - ${e.message}")
        }
    }
    
    private fun testLocationSharing() {
        try {
            // Test with sample coordinates
            MapsHelper.shareLocation(
                this,
                40.7128,
                -74.0060,
                "🧪 Test Location Share - This is a test"
            )
            showResult("✅ Location Sharing: Launched successfully")
        } catch (e: Exception) {
            showResult("❌ Location Sharing: Failed - ${e.message}")
        }
    }
    
    private fun testGoogleMapsApp() {
        try {
            // Test opening Google Maps app
            MapsHelper.openInGoogleMaps(
                this,
                40.7128,
                -74.0060,
                "Test Location"
            )
            showResult("✅ Google Maps App: Launched successfully")
        } catch (e: Exception) {
            showResult("❌ Google Maps App: Failed - ${e.message}")
        }
    }
    
    private fun testDirections() {
        try {
            // Test directions to sample location
            MapsHelper.getDirections(
                this,
                40.7589,
                -73.9851,
                "Test Destination"
            )
            showResult("✅ Directions: Launched successfully")
        } catch (e: Exception) {
            showResult("❌ Directions: Failed - ${e.message}")
        }
    }
    
    private fun testDistanceCalculation() {
        try {
            // Calculate distance between two points
            val distance = MapsHelper.calculateDistance(
                40.7128, -74.0060,  // New York
                40.7589, -73.9851   // Times Square
            )
            
            val formattedDistance = MapsHelper.formatDistance(distance)
            showResult("✅ Distance Calculation: $formattedDistance between test points")
        } catch (e: Exception) {
            showResult("❌ Distance Calculation: Failed - ${e.message}")
        }
    }
    
    private fun showResult(message: String) {
        binding.tvResults.append("$message\n\n")
        binding.scrollView.post {
            binding.scrollView.fullScroll(android.view.View.FOCUS_DOWN)
        }
        
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
