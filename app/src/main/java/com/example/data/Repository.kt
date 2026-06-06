package com.example.data

import kotlinx.coroutines.flow.Flow

class Repository(private val dao: DatabaseDao) {

    // Contacts
    val allContacts: Flow<List<Contact>> = dao.getAllContacts()

    suspend fun insertContact(contact: Contact): Long {
        return dao.insertContact(contact)
    }

    suspend fun updateContact(contact: Contact) {
        dao.updateContact(contact)
    }

    suspend fun deleteContact(contact: Contact) {
        dao.deleteContact(contact)
    }

    suspend fun deleteContactById(id: Long) {
        dao.deleteContactById(id)
    }

    // CCTV Feeds
    val allCctvFeeds: Flow<List<CctvFeed>> = dao.getAllCctvFeeds()

    suspend fun insertCctvFeed(feed: CctvFeed): Long {
        return dao.insertCctvFeed(feed)
    }

    suspend fun updateCctvFeed(feed: CctvFeed) {
        dao.updateCctvFeed(feed)
    }

    suspend fun deleteCctvFeed(feed: CctvFeed) {
        dao.deleteCctvFeed(feed)
    }

    suspend fun deleteCctvFeedById(id: Long) {
        dao.deleteCctvFeedById(id)
    }

    // Alert Events
    val allAlertEvents: Flow<List<AlertEvent>> = dao.getAllAlertEvents()

    suspend fun insertAlertEvent(event: AlertEvent): Long {
        return dao.insertAlertEvent(event)
    }

    suspend fun updateAlertEvent(event: AlertEvent) {
        dao.updateAlertEvent(event)
    }

    suspend fun deleteAlertEventById(id: Long) {
        dao.deleteAlertEventById(id)
    }
}
