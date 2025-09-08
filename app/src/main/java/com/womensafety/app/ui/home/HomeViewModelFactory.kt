package com.womensafety.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.womensafety.app.data.repository.ContactRepository
import com.womensafety.app.data.repository.LocationRepository
import com.womensafety.app.data.repository.SosRepository


class HomeViewModelFactory(
    private val sosRepository: SosRepository,
    private val locationRepository: LocationRepository,
    private val contactRepository: ContactRepository
) : ViewModelProvider.Factory {
    
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            return HomeViewModel(sosRepository, locationRepository, contactRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
