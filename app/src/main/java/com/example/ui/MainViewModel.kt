package com.example.ui

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.example.api.GeminiClient
import com.example.service.EmergencyDetectionService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext
    private val database = AppDatabase.getDatabase(context, viewModelScope)
    private val repository = Repository(database.dao)

    // Flow Data
    val contacts: StateFlow<List<Contact>> = repository.allContacts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val cctvFeeds: StateFlow<List<CctvFeed>> = repository.allCctvFeeds
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val alertEvents: StateFlow<List<AlertEvent>> = repository.allAlertEvents
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Active Alert Dashboard State ---
    private val _isAlertActive = MutableStateFlow(false)
    val isAlertActive = _isAlertActive.asStateFlow()

    private val _countdownActive = MutableStateFlow(0)
    val countdownActive = _countdownActive.asStateFlow()

    private val _alertTriggerSource = MutableStateFlow("")
    val alertTriggerSource = _alertTriggerSource.asStateFlow()

    private val _isSilentAlertMode = MutableStateFlow(false)
    val isSilentAlertMode = _isSilentAlertMode.asStateFlow()

    private val sharedPrefs = context.getSharedPreferences("panic_link_prefs", Context.MODE_PRIVATE)

    private val _isPowerButtonHapticEnabled = MutableStateFlow(
        sharedPrefs.getBoolean("pref_power_button_haptic", true)
    )
    val isPowerButtonHapticEnabled = _isPowerButtonHapticEnabled.asStateFlow()

    private val _activeSeverityLevel = MutableStateFlow("CRITICAL") // "LOW", "MEDIUM", "HIGH", "CRITICAL"
    val activeSeverityLevel = _activeSeverityLevel.asStateFlow()

    fun updateActiveSeverity(severity: String) {
        _activeSeverityLevel.value = severity
    }

    fun getAlertedContactsForSeverity(contactsList: List<Contact>, severity: String): List<Contact> {
        return when (severity) {
            "CRITICAL" -> contactsList
            "HIGH" -> contactsList.filter { it.alertTier == "TIER_1" || it.alertTier == "TIER_2" }
            "MEDIUM" -> contactsList.filter { it.alertTier == "TIER_1" }
            "LOW" -> contactsList.filter { it.alertTier == "TIER_3" || it.isSilentRecipient }
            else -> contactsList
        }
    }

    // Upload & Dispatch Progress States
    private val _audioUploadProgress = MutableStateFlow(0f)
    val audioUploadProgress = _audioUploadProgress.asStateFlow()

    private val _videoUploadProgress = MutableStateFlow(0f)
    val videoUploadProgress = _videoUploadProgress.asStateFlow()

    private val _dispatcherSyncStatus = MutableStateFlow("PENDING") // "PENDING", "DISPATCHING", "SENT_REALTIME", "CONFIRMED"
    val dispatcherSyncStatus = _dispatcherSyncStatus.asStateFlow()

    // CCTV event connection feed
    private val _cctvCaptureStreamName = MutableStateFlow("Default Entrance")
    val cctvCaptureStreamName = _cctvCaptureStreamName.asStateFlow()

    private val _cctvCaptureThumbnail = MutableStateFlow<String?>(null)
    val cctvCaptureThumbnail = _cctvCaptureThumbnail.asStateFlow()

    // Temporary active location simulation
    private val _currentLocationName = MutableStateFlow("1600 Amphitheatre Pkwy, Mountain View, CA")
    val currentLocationName = _currentLocationName.asStateFlow()

    // Audio Playback in history
    private val _playingAudioEventId = MutableStateFlow<Long?>(null)
    val playingAudioEventId = _playingAudioEventId.asStateFlow()

    private val _audioPlaybackProgress = MutableStateFlow(0f)
    val audioPlaybackProgress = _audioPlaybackProgress.asStateFlow()

    private var countdownJob: Job? = null
    private var playbackJob: Job? = null
    private var player: MediaPlayer? = null

    // Broadcast receiver to detect the background service panic launch and cancel commands
    private val serviceReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                EmergencyDetectionService.ACTION_PANIC_TRIGGERED -> {
                    val trigger = intent.getStringExtra(EmergencyDetectionService.EXTRA_TRIGGER_TYPE) ?: "POWER_BUTTON_TAPS"
                    startAlertSequence(trigger)
                }
                EmergencyDetectionService.ACTION_PANIC_CANCELLED -> {
                    dismissActiveAlertInternal()
                }
            }
        }
    }

    init {
        // Register receiver for background service triggers with Android 14 compliant export flags
        val filter = IntentFilter().apply {
            addAction(EmergencyDetectionService.ACTION_PANIC_TRIGGERED)
            addAction(EmergencyDetectionService.ACTION_PANIC_CANCELLED)
        }
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(serviceReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                context.registerReceiver(serviceReceiver, filter)
            }
        } catch (e: Exception) {
            Log.e("MainViewModel", "Failed to register local panic safety receiver safely", e)
        }

        // Make sure background service is started
        try {
            val serviceIntent = Intent(context, EmergencyDetectionService::class.java)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
        } catch (e: Exception) {
            Log.e("MainViewModel", "Could not start active shield service", e)
        }
    }

    override fun onCleared() {
        super.onCleared()
        try {
            context.unregisterReceiver(serviceReceiver)
        } catch (e: Exception) {
            // Already unregistered
        }
        player?.release()
    }

    // --- Actions ---

    fun toggleSilentAlertMode(enabled: Boolean) {
        _isSilentAlertMode.value = enabled
    }

    fun togglePowerButtonHaptic(enabled: Boolean) {
        _isPowerButtonHapticEnabled.value = enabled
        sharedPrefs.edit().putBoolean("pref_power_button_haptic", enabled).apply()
    }

    fun triggerOversizedPanicButton() {
        startAlertSequence("PANIC_BUTTON")
    }

    fun simulatePowerButtonTaps() {
        startAlertSequence("POWER_BUTTON_TAPS")
    }

    fun getFeedDistanceMeters(feed: CctvFeed): Double {
        val userLat = 37.7749
        val userLon = -122.4194
        val latDiff = (feed.latitude - userLat) * 111000.0
        val lonDiff = (feed.longitude - userLon) * 111000.0 * Math.cos(Math.toRadians(userLat))
        return Math.hypot(latDiff, lonDiff)
    }

    private fun startAlertSequence(triggerSource: String) {
        if (_isAlertActive.value) return // Already running
        _isAlertActive.value = true
        _alertTriggerSource.value = triggerSource

        // Perform haptic haptic feedback confirmation on power button trigger if enabled
        if (_isPowerButtonHapticEnabled.value && (triggerSource == "POWER_BUTTON_TAPS" || triggerSource == "MOCKED_POWER_BUTTON_TAPS")) {
            HapticHelper.performHapticFeedback(context)
        }

        // Filter and sort enabled feeds by proximity from alert epicenter
        val sortedFeeds = cctvFeeds.value.filter { it.isEnabled }
            .sortedBy { getFeedDistanceMeters(it) }

        val closestFeed = sortedFeeds.firstOrNull()
        _cctvCaptureStreamName.value = closestFeed?.let { "${it.name} (${String.format("%.1fm", getFeedDistanceMeters(it))})" } ?: "Emergency Mobile Cam"
        _cctvCaptureThumbnail.value = closestFeed?.streamUrl ?: "https://images.unsplash.com/photo-1558002038-1055907df827?auto=format&fit=crop&w=400&q=80"

        // Cancel existing timer
        countdownJob?.cancel()
        _countdownActive.value = 10
        _audioUploadProgress.value = 0f
        _videoUploadProgress.value = 0f
        _dispatcherSyncStatus.value = "DISPATCHING"

        countdownJob = viewModelScope.launch(Dispatchers.Main) {
            for (i in 10 downTo 1) {
                _countdownActive.value = i
                
                // Automatically cycle through nearby feeds based on GPS proximity, prioritizing the closest camera stream
                if (sortedFeeds.isNotEmpty()) {
                    val cycleIndex = ((10 - i) / 2) % sortedFeeds.size
                    val currentCycleFeed = sortedFeeds[cycleIndex]
                    _cctvCaptureStreamName.value = "${currentCycleFeed.name} (Proximity: ${String.format("%.1fm", getFeedDistanceMeters(currentCycleFeed))})"
                    _cctvCaptureThumbnail.value = currentCycleFeed.streamUrl
                }

                // Simulate bytes and live sync values streaming packet-by-packet
                _audioUploadProgress.value = (10 - i + 1) / 10f
                _videoUploadProgress.value = (10 - i + 0.5f) / 10f
                delay(1000)
            }

            // Realtime confirm dispatch to primary emergency line
            _audioUploadProgress.value = 1.0f
            _videoUploadProgress.value = 1.0f
            _dispatcherSyncStatus.value = "SENT_REALTIME"

            delay(800)
            _dispatcherSyncStatus.value = "CONFIRMED"
            delay(1200)

            _isAlertActive.value = false
            
            // Auto clean up service status indicators
            try {
                val serviceIntent = Intent(context, EmergencyDetectionService::class.java).apply {
                    action = EmergencyDetectionService.ACTION_CANCEL_ALERT
                }
                context.startService(serviceIntent)
            } catch (e: Exception) {
                Log.e("MainViewModel", "Failed to clear service status naturally", e)
            }
        }
    }

    private fun dismissActiveAlertInternal() {
        countdownJob?.cancel()
        _isAlertActive.value = false
    }

    fun dismissActiveAlert() {
        dismissActiveAlertInternal()
        
        // Notify the service that alert cancellation was requested from the UI
        try {
            val serviceIntent = Intent(context, EmergencyDetectionService::class.java).apply {
                action = EmergencyDetectionService.ACTION_CANCEL_ALERT
            }
            context.startService(serviceIntent)
        } catch (e: Exception) {
            Log.e("MainViewModel", "Failed to send dismiss intent to service", e)
        }
    }

    // --- CRUD Operations ---

    fun addContact(
        name: String,
        phone: String,
        email: String,
        relation: String,
        isSilentRecipient: Boolean,
        proximityCategory: String = "IMMEDIATE",
        relationshipCategory: String = "FAMILY",
        alertTier: String = "TIER_1"
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertContact(
                Contact(
                    name = name,
                    phone = phone,
                    email = email,
                    relation = relation,
                    isSilentRecipient = isSilentRecipient,
                    proximityCategory = proximityCategory,
                    relationshipCategory = relationshipCategory,
                    alertTier = alertTier
                )
            )
        }
    }

    fun updateContact(contact: Contact) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateContact(contact)
        }
    }

    fun deleteContact(contact: Contact) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteContact(contact)
        }
    }

    fun addCctvFeed(
        name: String,
        location: String,
        streamUrl: String,
        isEnabled: Boolean = true,
        latitude: Double? = null,
        longitude: Double? = null
    ) {
        val finalUrl = streamUrl.ifBlank {
            // Default elegant camera graphic asset url
            "https://images.unsplash.com/photo-1558002038-1055907df827?auto=format&fit=crop&w=400&q=80"
        }
        viewModelScope.launch(Dispatchers.IO) {
            // Use assigned coordinates or assign realistic nearby coordinates from the epicenter
            val finalLat = latitude ?: (37.7749 + (Math.random() - 0.5) * 0.003)
            val finalLon = longitude ?: (-122.4194 + (Math.random() - 0.5) * 0.003)
            repository.insertCctvFeed(
                CctvFeed(
                    name = name,
                    location = location,
                    streamUrl = finalUrl,
                    type = "EXTERNAL_IP",
                    isEnabled = isEnabled,
                    latitude = finalLat,
                    longitude = finalLon
                )
            )
        }
    }

    fun updateCctvFeed(feed: CctvFeed) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateCctvFeed(feed)
        }
    }

    fun deleteCctvFeed(feed: CctvFeed) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteCctvFeed(feed)
        }
    }

    fun deleteAlertEvent(event: AlertEvent) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteAlertEventById(event.id)
        }
    }

    // --- Playback Controls ---

    fun toggleAudioPlayback(event: AlertEvent) {
        if (_playingAudioEventId.value == event.id) {
            // Stop
            playbackJob?.cancel()
            _playingAudioEventId.value = null
            _audioPlaybackProgress.value = 0f
        } else {
            // Start mock waveform play simulation
            playbackJob?.cancel()
            _playingAudioEventId.value = event.id
            _audioPlaybackProgress.value = 0f

            playbackJob = viewModelScope.launch(Dispatchers.Main) {
                val segments = 40
                for (i in 0..segments) {
                    _audioPlaybackProgress.value = i.toFloat() / segments
                    delay((event.audioDurationSeconds * 1000L) / segments)
                }
                _playingAudioEventId.value = null
                _audioPlaybackProgress.value = 0f
            }
        }
    }

    fun createSimulatedAlertEvent(
        triggerType: String,
        threatLevel: String,
        address: String,
        transcript: String,
        briefAssessment: String,
        suggestedAction: String,
        backupUrl: String
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertAlertEvent(
                AlertEvent(
                    timestamp = System.currentTimeMillis() - (0..7200000).random(), // past 2 hours
                    latitude = 37.7749 + (Math.random() - 0.5) * 0.005,
                    longitude = -122.4194 + (Math.random() - 0.5) * 0.005,
                    locationAddress = address,
                    audioTranscript = transcript,
                    triggersUsed = triggerType,
                    aiBriefAssessment = briefAssessment,
                    aiThreatLevel = threatLevel,
                    aiSuggestedAction = suggestedAction,
                    isCloudSynced = true,
                    isSentToEmergencyServices = (threatLevel == "CRITICAL" || threatLevel == "HIGH"),
                    backupCloudUrl = backupUrl
                )
            )
        }
    }
}
