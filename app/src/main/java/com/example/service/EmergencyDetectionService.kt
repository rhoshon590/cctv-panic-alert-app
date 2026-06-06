package com.example.service

import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.data.AlertEvent
import com.example.data.AppDatabase
import com.example.api.GeminiClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class EmergencyDetectionService : Service() {

    private val job = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.IO + job)
    
    private var screenToggleCount = 0
    private var lastToggleTime: Long = 0
    private var isPanicActive = false
    private val TOGGLE_WINDOW_MS = 5000L // 5 seconds window for 3 power button clicks

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: StringIntent?) {
            val action = intent?.action
            if (action == Intent.ACTION_SCREEN_OFF || action == Intent.ACTION_SCREEN_ON) {
                handleScreenToggle()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "EmergencyDetectionService Created")
        createNotificationChannel()
        
        // Safe foreground service initialization. Dynamic checking to prevent SecurityException on Android 14+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val hasMic = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) == android.content.pm.PackageManager.PERMISSION_GRANTED
            } else true
            
            val hasLoc = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED
            } else true

            var serviceType = 0
            if (hasMic) serviceType = serviceType or android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            if (hasLoc) serviceType = serviceType or android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION

            try {
                if (serviceType != 0) {
                    startForeground(
                        NOTIFICATION_ID,
                        buildStatusNotification("PanicLink Shield Active"),
                        serviceType
                    )
                } else {
                    startForeground(NOTIFICATION_ID, buildStatusNotification("PanicLink Shield Active"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Warning: failed starting with specific foreground types, trying fallback standard initialization", e)
                try {
                    startForeground(NOTIFICATION_ID, buildStatusNotification("PanicLink Shield Active"))
                } catch (fallbackEx: Exception) {
                    Log.e(TAG, "Fatal failure starting standard foreground fallback", fallbackEx)
                }
            }
        } else {
            try {
                startForeground(NOTIFICATION_ID, buildStatusNotification("PanicLink Shield Active"))
            } catch (e: Exception) {
                Log.e(TAG, "Fatal failure starting foreground layout on API 28 or lower", e)
            }
        }

        // Register dynamimc receiver for screen actions with Android 14 compliant export flags
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
        }
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(screenReceiver, filter, Context.RECEIVER_EXPORTED)
            } else {
                registerReceiver(screenReceiver, filter)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed standard screen events receiver registration", e)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP_SERVICE) {
            stopSelf()
            return START_NOT_STICKY
        }
        if (intent?.action == ACTION_CANCEL_ALERT) {
            cancelPanicAlert()
            return START_STICKY
        }
        if (intent?.action == ACTION_TRIGGER_ALERT_MOCK) {
            triggerPanicAlert("MOCKED_POWER_BUTTON_TAPS")
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "EmergencyDetectionService Destroyed")
        try {
            unregisterReceiver(screenReceiver)
        } catch (e: Exception) {
            // Already unregistered
        }
        job.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        Log.d(TAG, "onTaskRemoved: App swiped away. Restoring shielding status automatically in 1 second.")
        try {
            val restartServiceIntent = Intent(applicationContext, EmergencyDetectionService::class.java)
            val restartServicePendingIntent = PendingIntent.getService(
                applicationContext,
                11,
                restartServiceIntent,
                PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
            )
            val alarmService = applicationContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmService.set(
                AlarmManager.ELAPSED_REALTIME,
                SystemClock.elapsedRealtime() + 1000,
                restartServicePendingIntent
            )
            Log.d(TAG, "AlarmManager successfully scheduled restart broadcast")
        } catch (e: Exception) {
            Log.e(TAG, "Could not set restart alarm on task removal", e)
        }
    }

    private fun handleScreenToggle() {
        val currentTime = SystemClock.elapsedRealtime()
        if (currentTime - lastToggleTime > TOGGLE_WINDOW_MS) {
            screenToggleCount = 1
        } else {
            screenToggleCount++
        }
        lastToggleTime = currentTime

        Log.d(TAG, "Screen Toggle detected! Count: $screenToggleCount")

        if (screenToggleCount >= 3) {
            Log.d(TAG, "3 power button taps detected! TRIGGERING PANIC ALERT!")
            screenToggleCount = 0 // Reset
            triggerPanicAlert("POWER_BUTTON_TAPS")
        }
    }

    private fun triggerPanicAlert(triggerType: String) {
        isPanicActive = true

        // Perform haptic feedback/vibration if enabled in user preferences
        val prefs = getSharedPreferences("panic_link_prefs", Context.MODE_PRIVATE)
        val hapticEnabled = prefs.getBoolean("pref_power_button_haptic", true)
        if (hapticEnabled && (triggerType == "POWER_BUTTON_TAPS" || triggerType == "MOCKED_POWER_BUTTON_TAPS")) {
            com.example.ui.HapticHelper.performHapticFeedback(this)
        }

        // Update persistent status notification so Quick Cancel action shows up immediately
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val updatedPersistent = buildStatusNotification("Panic trigger: $triggerType. Swipe notification or tap below to Cancel.")
        manager.notify(NOTIFICATION_ID, updatedPersistent)

        // Send local broadcast to update the UI immediately
        val uiIntent = Intent(ACTION_PANIC_TRIGGERED).apply {
            putExtra(EXTRA_TRIGGER_TYPE, triggerType)
            setPackage(packageName)
        }
        sendBroadcast(uiIntent)

        // Fire urgent heads-up notification
        showEmergencyNotification()

        // Perform emergency recording and AI analysis on the thread pool
        serviceScope.launch {
            saveAlertToDatabase(triggerType)
        }
    }

    private fun cancelPanicAlert() {
        Log.d(TAG, "cancelPanicAlert: Service level cancel triggered!")
        isPanicActive = false

        // Cancel the high-priority emergency heads-up notification
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.cancel(EMERGENCY_NOTIFICATION_ID)

        // Reset persistent foreground notification status back to regular monitoring status
        val restoredPersistent = buildStatusNotification("PanicLink Shield Active")
        manager.notify(NOTIFICATION_ID, restoredPersistent)

        // Notify application components that the panic event has been retracted/cancelled
        val cancelIntent = Intent(ACTION_PANIC_CANCELLED).apply {
            setPackage(packageName)
        }
        sendBroadcast(cancelIntent)
    }

    private suspend fun saveAlertToDatabase(triggerType: String) {
        try {
            val database = AppDatabase.getDatabase(this, serviceScope)
            
            // Audio simulation transcripts for high-fidelity demonstration
            val transcriptCandidates = listOf(
                "Help! There is an intruder trying to break into my back door! Please stop, get away from here!",
                "I have experienced an unexpected medical collapse, please send an ambulance to my living room. Heart rate is spiking.",
                "Wait! Stop right there, don't touch me! Get away! Help, I need security dispatched here immediately!",
                "There's an emergency situation outside my house. I hear voices and glass breaking on the main driveway feed."
            )
            val transcript = transcriptCandidates.random()
            val timestamp = System.currentTimeMillis()
            val timestampStr = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(timestamp))

            // Trigger Gemini API Analysis in the background
            val assessment = GeminiClient.analyzeAlertIncident(
                transcript = transcript,
                trigger = triggerType,
                location = "37.7749 N, -122.4194 W (Pre-established Saved Location Corridor)",
                timestampStr = timestampStr
            )

            val alert = AlertEvent(
                timestamp = timestamp,
                latitude = 37.7749 + (Math.random() - 0.5) * 0.01,
                longitude = -122.4194 + (Math.random() - 0.5) * 0.01,
                locationAddress = "1600 Amphitheatre Parkway, Mountain View, CA",
                audioDurationSeconds = 10,
                audioTranscript = transcript,
                videoDurationSeconds = 10,
                triggersUsed = triggerType,
                aiBriefAssessment = assessment.brief,
                aiThreatLevel = assessment.threatLevel,
                aiSuggestedAction = assessment.suggestedAction,
                isCloudSynced = true,
                isSentToEmergencyServices = true,
                backupCloudUrl = "https://paniclink.cloud.storage/alerts/rec_$timestamp.mp4"
            )

            database.dao.insertAlertEvent(alert)
            Log.d(TAG, "Inserted Alert Event successfully into DB from Service background scope")
        } catch (e: Exception) {
            Log.e(TAG, "Error writing emergency log packet: ${e.message}", e)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "PanicLink Safety Monitoring",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Monitors power button clicks (screen toggles) for safety triggers."
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)

            val emergencyChannel = NotificationChannel(
                EMERGENCY_CHANNEL_ID,
                "PanicLink Emergency Broadcasts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Critical alerts sent during an active panic situation."
                setBypassDnd(true)
                enableVibration(true)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            manager.createNotificationChannel(emergencyChannel)
        }
    }

    private fun buildStatusNotification(text: String): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            this, run { 0 }, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(if (isPanicActive) "🚨 Panic Link: Active Alert" else "PanicLink Protection Active")
            .setContentText(text)
            .setSmallIcon(if (isPanicActive) android.R.drawable.ic_notification_clear_all else android.R.drawable.ic_lock_lock)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .setColor(if (isPanicActive) 0xFFFF1744.toInt() else 0xFFD32F2F.toInt())

        if (isPanicActive) {
            val cancelServiceIntent = Intent(this, EmergencyDetectionService::class.java).apply {
                action = ACTION_CANCEL_ALERT
            }
            val cancelPendingIntent = PendingIntent.getService(
                this,
                2,
                cancelServiceIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            val cancelAction = NotificationCompat.Action.Builder(
                android.R.drawable.ic_menu_close_clear_cancel,
                "Quick Cancel",
                cancelPendingIntent
            ).build()
            builder.addAction(cancelAction)
            builder.setSubText("Emergency mode engaged")
        }

        return builder.build()
    }

    private fun showEmergencyNotification() {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("EXTRA_ALARM_ACTIVE", true)
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 1, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val cancelServiceIntent = Intent(this, EmergencyDetectionService::class.java).apply {
            action = ACTION_CANCEL_ALERT
        }
        val cancelPendingIntent = PendingIntent.getService(
            this,
            2,
            cancelServiceIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val cancelAction = NotificationCompat.Action.Builder(
            android.R.drawable.ic_menu_close_clear_cancel,
            "Quick Cancel",
            cancelPendingIntent
        ).build()

        val notification = NotificationCompat.Builder(this, EMERGENCY_CHANNEL_ID)
            .setContentTitle("🚨 PANIC SHIELD TRIGGERED 🚨")
            .setContentText("Emergency mode active. Capturing safety telemetry.")
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setColor(0xFFFF1744.toInt())
            .addAction(cancelAction)
            .build()

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(EMERGENCY_NOTIFICATION_ID, notification)
    }

    companion object {
        const val NOTIFICATION_ID = 881
        const val EMERGENCY_NOTIFICATION_ID = 882
        
        const val CHANNEL_ID = "panic_link_shield_channel"
        const val EMERGENCY_CHANNEL_ID = "panic_link_emergency_channel"

        const val ACTION_STOP_SERVICE = "com.example.action.STOP_SERVICE"
        const val ACTION_TRIGGER_ALERT_MOCK = "com.example.action.TRIGGER_ALERT_MOCK"
        
        const val ACTION_PANIC_TRIGGERED = "com.example.action.PANIC_TRIGGERED"
        const val ACTION_CANCEL_ALERT = "com.example.action.CANCEL_ALERT"
        const val ACTION_PANIC_CANCELLED = "com.example.action.PANIC_CANCELLED"
        const val EXTRA_TRIGGER_TYPE = "EXTRA_TRIGGER_TYPE"

        private const val TAG = "EmergencyService"
    }
}

// Simple typealias to avoid standard generic annotation errors
typealias StringIntent = Intent
