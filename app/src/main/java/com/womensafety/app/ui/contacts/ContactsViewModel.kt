package com.womensafety.app.ui.contacts

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.womensafety.app.data.model.Contact
import com.womensafety.app.data.repository.ContactRepository
import kotlinx.coroutines.launch

class ContactsViewModel(
    private val contactRepository: ContactRepository
) : ViewModel() {
    
    val contacts: LiveData<List<Contact>> = contactRepository.getAllContacts()
    
    fun addContact(name: String, phoneNumber: String) {
        viewModelScope.launch {
            val formattedPhone = contactRepository.formatPhoneNumber(phoneNumber)
            val contact = Contact(
                name = name.trim(),
                phoneNumber = formattedPhone,
                isPrimary = false
            )
            contactRepository.insertContact(contact)
        }
    }
    
    fun updateContact(contact: Contact) {
        viewModelScope.launch {
            val updatedContact = contact.copy(
                updatedAt = System.currentTimeMillis()
            )
            contactRepository.updateContact(updatedContact)
        }
    }
    
    fun deleteContact(contact: Contact) {
        viewModelScope.launch {
            contactRepository.deleteContact(contact)
        }
    }
    
    fun setPrimaryContact(contactId: Long) {
        viewModelScope.launch {
            contactRepository.setPrimaryContact(contactId)
        }
    }
    
    fun callContact(context: Context, contact: Contact) {
        try {
            val intent = Intent(Intent.ACTION_CALL).apply {
                data = Uri.parse("tel:${contact.phoneNumber}")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            // Fallback to dial intent
            val intent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:${contact.phoneNumber}")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        }
    }
}
