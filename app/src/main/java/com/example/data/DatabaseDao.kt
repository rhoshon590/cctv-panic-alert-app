package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface DatabaseDao {

    // Contacts
    @Query("SELECT * FROM contacts ORDER BY name ASC")
    fun getAllContacts(): Flow<List<Contact>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContact(contact: Contact): Long

    @Update
    suspend fun updateContact(contact: Contact)

    @Delete
    suspend fun deleteContact(contact: Contact)

    @Query("DELETE FROM contacts WHERE id = :id")
    suspend fun deleteContactById(id: Long)

    // CCTV Feeds
    @Query("SELECT * FROM cctv_feeds ORDER BY id DESC")
    fun getAllCctvFeeds(): Flow<List<CctvFeed>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCctvFeed(feed: CctvFeed): Long

    @Update
    suspend fun updateCctvFeed(feed: CctvFeed)

    @Delete
    suspend fun deleteCctvFeed(feed: CctvFeed)

    @Query("DELETE FROM cctv_feeds WHERE id = :id")
    suspend fun deleteCctvFeedById(id: Long)

    // Alert Events
    @Query("SELECT * FROM alert_events ORDER BY timestamp DESC")
    fun getAllAlertEvents(): Flow<List<AlertEvent>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlertEvent(event: AlertEvent): Long

    @Update
    suspend fun updateAlertEvent(event: AlertEvent)

    @Query("DELETE FROM alert_events WHERE id = :id")
    suspend fun deleteAlertEventById(id: Long)
}
