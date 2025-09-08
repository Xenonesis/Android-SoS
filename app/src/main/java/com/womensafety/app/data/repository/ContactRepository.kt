package com.womensafety.app.data.repository

import androidx.lifecycle.LiveData
import com.google.firebase.auth.FirebaseAuth
import com.womensafety.app.data.dao.ContactDao
import com.womensafety.app.data.model.Contact
import com.womensafety.app.data.model.Invitation
import com.womensafety.app.data.firebase.FirebaseRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ContactRepository(
    private val contactDao: ContactDao,
    private val firebaseRepository: FirebaseRepository,
    private val auth: FirebaseAuth
) {
    
    fun getAllContacts(): LiveData<List<Contact>> = contactDao.getAllContacts()
    
    fun getPrimaryContacts(): LiveData<List<Contact>> = contactDao.getPrimaryContacts()
    
    suspend fun getContactById(id: Long): Contact? = withContext(Dispatchers.IO) {
        contactDao.getContactById(id)
    }
    
    suspend fun getContactByPhone(phoneNumber: String): Contact? = withContext(Dispatchers.IO) {
        contactDao.getContactByPhone(phoneNumber)
    }
    
    suspend fun insertContact(contact: Contact): Long = withContext(Dispatchers.IO) {
        val contactId = contactDao.insertContact(contact)
        if (!contact.isEmergencyService) {
            val invitation = Invitation(
                invitedBy = auth.currentUser?.uid ?: "",
                inviteePhone = contact.phoneNumber
            )
            firebaseRepository.createInvitation(invitation)
        }
        contactId
    }
    
    suspend fun updateContact(contact: Contact) = withContext(Dispatchers.IO) {
        contactDao.updateContact(contact)
    }
    
    suspend fun deleteContact(contact: Contact) = withContext(Dispatchers.IO) {
        contactDao.deleteContact(contact)
    }
    
    suspend fun deleteContactById(id: Long) = withContext(Dispatchers.IO) {
        contactDao.deleteContactById(id)
    }
    
    suspend fun setPrimaryContact(contactId: Long) = withContext(Dispatchers.IO) {
        // Clear all primary flags first
        contactDao.clearAllPrimaryFlags()
        // Set the selected contact as primary
        val contact = contactDao.getContactById(contactId)
        contact?.let {
            contactDao.updateContact(it.copy(isPrimary = true))
        }
    }
    
    suspend fun getContactCount(): Int = withContext(Dispatchers.IO) {
        contactDao.getContactCount()
    }
    
    suspend fun getEmergencyServiceContacts(): List<Contact> = withContext(Dispatchers.IO) {
        contactDao.getEmergencyServiceContacts()
    }
    
    suspend fun addDefaultEmergencyContacts() = withContext(Dispatchers.IO) {
        val emergencyContacts = listOf(
            Contact(
                name = "Emergency Services",
                phoneNumber = "911",
                isPrimary = false,
                isEmergencyService = true
            ),
            Contact(
                name = "Police",
                phoneNumber = "911",
                isPrimary = false,
                isEmergencyService = true
            )
        )
        
        emergencyContacts.forEach { contact ->
            val existing = contactDao.getContactByPhone(contact.phoneNumber)
            if (existing == null) {
                contactDao.insertContact(contact)
            }
        }
    }
    
    suspend fun validatePhoneNumber(phoneNumber: String): Boolean {
        return phoneNumber.isNotBlank() && 
               phoneNumber.replace(Regex("[^0-9]"), "").length >= 10
    }
    
    suspend fun formatPhoneNumber(phoneNumber: String): String {
        val digitsOnly = phoneNumber.replace(Regex("[^0-9]"), "")
        return when {
            digitsOnly.length == 10 -> "+1$digitsOnly"
            digitsOnly.length == 11 && digitsOnly.startsWith("1") -> "+$digitsOnly"
            else -> phoneNumber
        }
    }
}
