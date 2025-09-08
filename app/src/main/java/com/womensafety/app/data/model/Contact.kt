package com.womensafety.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "contacts")
data class Contact(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val phoneNumber: String,
    val isPrimary: Boolean = false,
    val isEmergencyService: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

data class ContactWithStatus(
    val contact: Contact,
    val isOnline: Boolean = false,
    val lastSeen: Long? = null
)
