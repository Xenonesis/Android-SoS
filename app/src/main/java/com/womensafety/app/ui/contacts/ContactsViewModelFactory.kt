package com.womensafety.app.ui.contacts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.womensafety.app.data.repository.ContactRepository

class ContactsViewModelFactory(
    private val contactRepository: ContactRepository
) : ViewModelProvider.Factory {
    
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ContactsViewModel::class.java)) {
            return ContactsViewModel(contactRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
