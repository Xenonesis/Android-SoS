package com.womensafety.app.utils

object Constants {
    
    // Notification Channels
    const val NOTIFICATION_CHANNEL_SOS = "sos_channel"
    const val NOTIFICATION_CHANNEL_TRACKING = "tracking_channel"
    const val NOTIFICATION_CHANNEL_COMMUNITY = "community_channel"
    
    // Notification IDs
    const val NOTIFICATION_ID_SOS = 1001
    const val NOTIFICATION_ID_TRACKING = 1002
    const val NOTIFICATION_ID_COMMUNITY = 1003
    
    // Permissions
    const val PERMISSION_REQUEST_LOCATION = 1001
    const val PERMISSION_REQUEST_SMS = 1002
    const val PERMISSION_REQUEST_PHONE = 1003
    const val PERMISSION_REQUEST_NOTIFICATION = 1004
    
    // Location
    const val LOCATION_UPDATE_INTERVAL = 10000L // 10 seconds
    const val LOCATION_FASTEST_INTERVAL = 5000L // 5 seconds
    const val LOCATION_DISPLACEMENT = 10f // 10 meters
    
    // Shake Detection
    const val SHAKE_THRESHOLD_GRAVITY = 2.7f
    const val SHAKE_SLOP_TIME_MS = 500
    const val SHAKE_COUNT_RESET_TIME_MS = 3000
    const val SHAKE_REQUIRED_COUNT = 3
    
    // SOS
    const val SOS_COUNTDOWN_SECONDS = 5
    const val SOS_AUTO_CALL_DELAY = 10000L // 10 seconds
    
    // Database
    const val DATABASE_NAME = "sos_database"
    const val DATABASE_VERSION = 1
    
    // Firebase Collections
    const val COLLECTION_USERS = "users"
    const val COLLECTION_SOS_EVENTS = "sos_events"
    const val COLLECTION_COMMUNITY_ALERTS = "community_alerts"
    const val COLLECTION_LOCATIONS = "locations"
    
    // Shared Preferences
    const val PREFS_NAME = "sos_prefs"
    const val PREF_SHAKE_ENABLED = "shake_enabled"
    const val PREF_SHAKE_SENSITIVITY = "shake_sensitivity"
    const val PREF_AUTO_CALL_ENABLED = "auto_call_enabled"
    const val PREF_COMMUNITY_ALERTS_ENABLED = "community_alerts_enabled"
    const val PREF_USER_ID = "user_id"
    const val PREF_USER_NAME = "user_name"
    const val PREF_USER_PHONE = "user_phone"
    
    // Intent Actions
    const val ACTION_SOS_TRIGGERED = "com.womensafety.app.SOS_TRIGGERED"
    const val ACTION_SOS_CANCELLED = "com.womensafety.app.SOS_CANCELLED"
    const val ACTION_TRACKING_STARTED = "com.womensafety.app.TRACKING_STARTED"
    const val ACTION_TRACKING_STOPPED = "com.womensafety.app.TRACKING_STOPPED"
    
    // Intent Extras
    const val EXTRA_LOCATION_LAT = "location_lat"
    const val EXTRA_LOCATION_LNG = "location_lng"
    const val EXTRA_LOCATION_ADDRESS = "location_address"
    const val EXTRA_SOS_TYPE = "sos_type"
    
    // SOS Types
    const val SOS_TYPE_MANUAL = "manual"
    const val SOS_TYPE_SHAKE = "shake"
    const val SOS_TYPE_PANIC_BUTTON = "panic_button"
    const val SOS_TYPE_ROUTE_DEVIATION = "route_deviation"
    
    // Emergency Numbers
    const val EMERGENCY_NUMBER_POLICE = "911"
    const val EMERGENCY_NUMBER_FIRE = "911"
    const val EMERGENCY_NUMBER_MEDICAL = "911"
    
    // Map
    const val DEFAULT_ZOOM_LEVEL = 15f
    const val ROUTE_DEVIATION_THRESHOLD = 500f // 500 meters
    
    // Bluetooth
    const val BLUETOOTH_DEVICE_NAME = "SOS_Button"
    const val BLUETOOTH_UUID = "00001101-0000-1000-8000-00805F9B34FB"
    
    // Network
    const val NETWORK_TIMEOUT = 30000L // 30 seconds
    const val RETRY_ATTEMPTS = 3
    
    // Work Manager
    const val WORK_LOCATION_SYNC = "location_sync_work"
    const val WORK_SOS_RETRY = "sos_retry_work"
    
    // File Paths
    const val LOG_FILE_NAME = "sos_logs.txt"
    const val BACKUP_FILE_NAME = "sos_backup.json"
}
