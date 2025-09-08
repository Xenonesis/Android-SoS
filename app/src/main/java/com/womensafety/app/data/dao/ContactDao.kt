package com.womensafety.app.data.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.womensafety.app.data.model.Contact

@Dao
interface ContactDao {
    
    @Query("SELECT * FROM contacts ORDER BY isPrimary DESC, name ASC")
    fun getAllContacts(): LiveData<List<Contact>>
    
    @Query("SELECT * FROM contacts WHERE isPrimary = 1")
    fun getPrimaryContacts(): LiveData<List<Contact>>
    
    @Query("SELECT * FROM contacts WHERE id = :id")
    suspend fun getContactById(id: Long): Contact?
    
    @Query("SELECT * FROM contacts WHERE phoneNumber = :phoneNumber")
    suspend fun getContactByPhone(phoneNumber: String): Contact?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContact(contact: Contact): Long
    
    @Update
    suspend fun updateContact(contact: Contact)
    
    @Delete
    suspend fun deleteContact(contact: Contact)
    
    @Query("DELETE FROM contacts WHERE id = :id")
    suspend fun deleteContactById(id: Long)
    
    @Query("UPDATE contacts SET isPrimary = 0")
    suspend fun clearAllPrimaryFlags()
    
    @Query("SELECT COUNT(*) FROM contacts")
    suspend fun getContactCount(): Int
    
    @Query("SELECT * FROM contacts WHERE isEmergencyService = 1")
    suspend fun getEmergencyServiceContacts(): List<Contact>
}
