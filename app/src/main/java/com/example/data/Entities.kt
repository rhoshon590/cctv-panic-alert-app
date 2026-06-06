package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "contacts")
data class Contact(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val phone: String,
    val email: String,
    val relation: String,
    val isSilentRecipient: Boolean = false,
    val proximityCategory: String = "IMMEDIATE", // "IMMEDIATE", "REGIONAL", "REMOTE"
    val relationshipCategory: String = "FAMILY", // "FAMILY", "FRIEND", "NEIGHBOR", "SERVICE"
    val alertTier: String = "TIER_1" // "TIER_1", "TIER_2", "TIER_3"
)

@Entity(tableName = "cctv_feeds")
data class CctvFeed(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val location: String,
    val streamUrl: String,
    val type: String = "MOCK", // "MOCK" or "IP_CAMERA"
    val isEnabled: Boolean = true,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0
)

@Entity(tableName = "alert_events")
data class AlertEvent(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val latitude: Double,
    val longitude: Double,
    val locationAddress: String,
    val audioDurationSeconds: Int = 10,
    val audioTranscript: String = "",
    val videoDurationSeconds: Int = 10,
    val triggersUsed: String, // "POWER_BUTTON_TAPS" or "PANIC_BUTTON"
    val aiBriefAssessment: String = "",
    val aiThreatLevel: String = "MEDIUM", // "LOW", "MEDIUM", "HIGH", "CRITICAL"
    val aiSuggestedAction: String = "",
    val isCloudSynced: Boolean = false,
    val isSentToEmergencyServices: Boolean = false,
    val backupCloudUrl: String = ""
)
