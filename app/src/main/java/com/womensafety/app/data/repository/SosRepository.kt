package com.womensafety.app.data.repository

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.telephony.SmsManager
import androidx.lifecycle.LiveData
import com.womensafety.app.data.dao.SosEventDao
import com.womensafety.app.data.firebase.FirebaseRepository
import com.womensafety.app.data.model.Contact
import com.womensafety.app.data.model.LocationData
import com.womensafety.app.data.model.SosEvent
import com.womensafety.app.utils.Constants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SosRepository(
    private val sosEventDao: SosEventDao,
    private val contactRepository: ContactRepository,
    private val locationRepository: LocationRepository,
    private val firebaseRepository: FirebaseRepository
) {

    fun getAllSosEvents(): LiveData<List<SosEvent>> = sosEventDao.getAllSosEvents()

    fun getActiveSosEvents(): LiveData<List<SosEvent>> = sosEventDao.getActiveSosEvents()

    suspend fun triggerSos(
        type: String,
        location: LocationData? = null
    ): String = withContext(Dispatchers.IO) {

        val currentLocation = location ?: locationRepository.getCurrentLocation()

        if (currentLocation == null) {
            throw Exception("Unable to get current location")
        }

        // Create SOS event
        val sosEvent = SosEvent(
            type = type,
            latitude = currentLocation.latitude,
            longitude = currentLocation.longitude,
            address = currentLocation.address,
            timestamp = System.currentTimeMillis()
        )

        // Create SOS event in Firestore
        val eventId = firebaseRepository.createSosEvent(sosEvent)

        // Save the event to the local database
        val localEvent = sosEvent.copy(id = 0, contactsNotified = listOf(eventId)) // Using contactsNotified to store remote ID
        sosEventDao.insertSosEvent(localEvent)

        eventId
    }

    suspend fun cancelSos(eventId: String) = withContext(Dispatchers.IO) {
        val event = sosEventDao.getSosEventById(eventId.toLong())
        event?.let {
            val updatedEvent = it.copy(
                isResolved = true,
                resolvedAt = System.currentTimeMillis(),
                notes = "Cancelled by user"
            )
            sosEventDao.updateSosEvent(updatedEvent)
        }
    }
    
    suspend fun resolveSos(eventId: Long, notes: String? = null) = withContext(Dispatchers.IO) {
        val event = sosEventDao.getSosEventById(eventId)
        event?.let {
            val updatedEvent = it.copy(
                isResolved = true,
                resolvedAt = System.currentTimeMillis(),
                notes = notes
            )
            sosEventDao.updateSosEvent(updatedEvent)
        }
    }
    
    fun makeEmergencyCall(context: Context, phoneNumber: String = Constants.EMERGENCY_NUMBER_POLICE) {
        try {
            val intent = Intent(Intent.ACTION_CALL).apply {
                data = Uri.parse("tel:$phoneNumber")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            // Fallback to dial intent
            val intent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:$phoneNumber")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        }
    }
    
    suspend fun getSosEventsByDateRange(startTime: Long, endTime: Long): List<SosEvent> = 
        withContext(Dispatchers.IO) {
            sosEventDao.getSosEventsByDateRange(startTime, endTime)
        }
    
    suspend fun getSosEventsByType(type: String): List<SosEvent> = 
        withContext(Dispatchers.IO) {
            sosEventDao.getSosEventsByType(type)
        }
    
    suspend fun getLatestSosEvent(): SosEvent? = 
        withContext(Dispatchers.IO) {
            sosEventDao.getLatestSosEvent()
        }
    
    suspend fun getSosEventCountSince(startTime: Long): Int = 
        withContext(Dispatchers.IO) {
            sosEventDao.getSosEventCountSince(startTime)
        }
    
    suspend fun cleanupOldSosEvents(daysToKeep: Int = 30) = 
        withContext(Dispatchers.IO) {
            val cutoffTime = System.currentTimeMillis() - (daysToKeep * 24 * 60 * 60 * 1000L)
            sosEventDao.deleteOldSosEvents(cutoffTime)
        }
}
