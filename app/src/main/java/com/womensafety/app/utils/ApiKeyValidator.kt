package com.womensafety.app.utils

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.util.Log

/**
 * Utility class to validate and manage API keys
 */
object ApiKeyValidator {
    
    private const val TAG = "ApiKeyValidator"
    
    /**
     * Validate Google Maps API key configuration
     */
    fun validateMapsApiKey(context: Context): ValidationResult {
        return try {
            val appInfo: ApplicationInfo = context.packageManager
                .getApplicationInfo(context.packageName, PackageManager.GET_META_DATA)
            
            val apiKey = appInfo.metaData?.getString("com.google.android.geo.API_KEY")
            
            when {
                apiKey.isNullOrEmpty() -> {
                    ValidationResult(
                        isValid = false,
                        message = "❌ Maps API key not found in AndroidManifest.xml"
                    )
                }
                apiKey.startsWith("AIza") && apiKey.length >= 35 -> {
                    ValidationResult(
                        isValid = true,
                        message = "✅ Maps API key is properly configured",
                        apiKey = apiKey
                    )
                }
                else -> {
                    ValidationResult(
                        isValid = false,
                        message = "❌ Maps API key format appears invalid"
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error validating Maps API key", e)
            ValidationResult(
                isValid = false,
                message = "❌ Error reading Maps API key: ${e.message}"
            )
        }
    }
    
    /**
     * Get Google Maps API key from manifest
     */
    fun getMapsApiKey(context: Context): String? {
        return try {
            val appInfo: ApplicationInfo = context.packageManager
                .getApplicationInfo(context.packageName, PackageManager.GET_META_DATA)
            appInfo.metaData?.getString("com.google.android.geo.API_KEY")
        } catch (e: Exception) {
            Log.e(TAG, "Error getting Maps API key", e)
            null
        }
    }
    
    /**
     * Check if Google Play Services are available
     */
    fun isGooglePlayServicesAvailable(context: Context): Boolean {
        return try {
            val googleApiAvailability = com.google.android.gms.common.GoogleApiAvailability.getInstance()
            val resultCode = googleApiAvailability.isGooglePlayServicesAvailable(context)
            resultCode == com.google.android.gms.common.ConnectionResult.SUCCESS
        } catch (e: Exception) {
            Log.e(TAG, "Error checking Google Play Services", e)
            false
        }
    }
    
    /**
     * Get comprehensive Maps configuration status
     */
    fun getMapsConfigurationStatus(context: Context): ConfigurationStatus {
        val apiKeyValidation = validateMapsApiKey(context)
        val playServicesAvailable = isGooglePlayServicesAvailable(context)
        
        return ConfigurationStatus(
            apiKeyValid = apiKeyValidation.isValid,
            apiKeyMessage = apiKeyValidation.message,
            apiKey = apiKeyValidation.apiKey?.let { "${it.take(20)}..." },
            playServicesAvailable = playServicesAvailable,
            overallStatus = apiKeyValidation.isValid && playServicesAvailable
        )
    }
    
    /**
     * Data classes for validation results
     */
    data class ValidationResult(
        val isValid: Boolean,
        val message: String,
        val apiKey: String? = null
    )
    
    data class ConfigurationStatus(
        val apiKeyValid: Boolean,
        val apiKeyMessage: String,
        val apiKey: String?,
        val playServicesAvailable: Boolean,
        val overallStatus: Boolean
    ) {
        fun getStatusMessage(): String {
            return buildString {
                appendLine("🗺️ Google Maps Configuration Status")
                appendLine("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                appendLine("📋 API Key: ${if (apiKeyValid) "✅ Valid" else "❌ Invalid"}")
                if (apiKey != null) appendLine("🔑 Key: $apiKey")
                appendLine("📱 Play Services: ${if (playServicesAvailable) "✅ Available" else "❌ Not Available"}")
                appendLine("🎯 Overall Status: ${if (overallStatus) "✅ Ready" else "❌ Not Ready"}")
                appendLine()
                appendLine("📝 Details:")
                appendLine(apiKeyMessage)
                if (!playServicesAvailable) {
                    appendLine("❌ Google Play Services required for Maps functionality")
                }
            }
        }
    }
}
