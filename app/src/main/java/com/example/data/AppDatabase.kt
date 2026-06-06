package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(entities = [Contact::class, CctvFeed::class, AlertEvent::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract val dao: DatabaseDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "panic_link_database"
                )
                .fallbackToDestructiveMigration()
                .addCallback(DatabaseCallback(scope))
                .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class DatabaseCallback(
        private val scope: CoroutineScope
    ) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                scope.launch(Dispatchers.IO) {
                    populateInitialData(database.dao)
                }
            }
        }

        private suspend fun populateInitialData(dao: DatabaseDao) {
            // Add default contacts
            dao.insertContact(
                Contact(
                    name = "Emergency Dispatcher (Simulated)",
                    phone = "911",
                    email = "dispatch@emergency-services.gov",
                    relation = "Primary Service",
                    isSilentRecipient = false,
                    proximityCategory = "IMMEDIATE",
                    relationshipCategory = "SERVICE",
                    alertTier = "TIER_1"
                )
            )
            dao.insertContact(
                Contact(
                    name = "Neighbor Bob (First Responder)",
                    phone = "+1 (555) 777-8888",
                    email = "bob.neighbor@example.com",
                    relation = "Next Door Neighbor",
                    isSilentRecipient = false,
                    proximityCategory = "IMMEDIATE",
                    relationshipCategory = "NEIGHBOR",
                    alertTier = "TIER_1"
                )
            )
            dao.insertContact(
                Contact(
                    name = "John Doe (Guardian)",
                    phone = "+1 (555) 019-2834",
                    email = "john.doe@example.com",
                    relation = "Parent",
                    isSilentRecipient = true,
                    proximityCategory = "REGIONAL",
                    relationshipCategory = "FAMILY",
                    alertTier = "TIER_2"
                )
            )
            dao.insertContact(
                Contact(
                    name = "Sarah Connor (Crisis Contact)",
                    phone = "+1 (555) 012-9876",
                    email = "sarah.connor@sky-net.net",
                    relation = "Sister",
                    isSilentRecipient = true,
                    proximityCategory = "REMOTE",
                    relationshipCategory = "FAMILY",
                    alertTier = "TIER_3"
                )
            )

            // Add default CCTV feeds with GPS coordinates near Mountain View (37.7749, -122.4194)
            dao.insertCctvFeed(
                CctvFeed(
                    name = "Smart Doorbell Front Camera",
                    location = "Main Entrance Screen",
                    streamUrl = "https://images.unsplash.com/photo-1558002038-1055907df827?auto=format&fit=crop&w=400&q=80",
                    type = "DOORBELL_LIVE",
                    isEnabled = true,
                    latitude = 37.77492,
                    longitude = -122.41938
                )
            )
            dao.insertCctvFeed(
                CctvFeed(
                    name = "Living Room Wide-Angle CCTV",
                    location = "Indoor Hallway West",
                    streamUrl = "https://images.unsplash.com/photo-1600585154340-be6161a56a0c?auto=format&fit=crop&w=400&q=80",
                    type = "LIVINGROOM_INFRARED",
                    isEnabled = true,
                    latitude = 37.77485,
                    longitude = -122.41945
                )
            )
            dao.insertCctvFeed(
                CctvFeed(
                    name = "Backyard Corridor Feed",
                    location = "Fence Outer Boundary",
                    streamUrl = "https://images.unsplash.com/photo-1582268611958-ebfd161ef9cf?auto=format&fit=crop&w=400&q=80",
                    type = "OUTDOOR_PTZ",
                    isEnabled = true, // Enable for proximity cycling demonstration
                    latitude = 37.77512,
                    longitude = -122.42011
                )
            )
            dao.insertCctvFeed(
                CctvFeed(
                    name = "Garage Outer Driveway",
                    location = "Street Entrance Feed",
                    streamUrl = "https://images.unsplash.com/photo-1506521781263-d8422e82f27a?auto=format&fit=crop&w=400&q=80",
                    type = "DRIVEWAY_CAM",
                    isEnabled = true,
                    latitude = 37.77421,
                    longitude = -122.41812
                )
            )
        }
    }
}
