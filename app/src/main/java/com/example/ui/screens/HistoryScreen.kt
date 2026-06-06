package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.example.data.AlertEvent
import com.example.ui.MainViewModel
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.delay

@Composable
fun HistoryScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val eventsList by viewModel.alertEvents.collectAsState()
    val playingEventId by viewModel.playingAudioEventId.collectAsState()
    val playProgress by viewModel.audioPlaybackProgress.collectAsState()

    // Passcode Security state
    var isAuthorized by remember { mutableStateOf(false) }
    var enteredPin by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf(false) }
    var showBiometricSuccessAnim by remember { mutableStateOf(false) }

    // Log screening filter
    var severityFilter by remember { mutableStateOf("ALL") } // "ALL", "CRITICAL_HIGH", "MED_LOW"

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        if (!isAuthorized) {
            // HIGH-SECURITY PASSCODE AUTHSYSTEM OVERLAY
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Security Icon with Pulse Glow simulation
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(Color(0xFF6750A4).copy(alpha = 0.1f), CircleShape)
                        .border(1.5.dp, Color(0xFF6750A4), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Secure lock indicator",
                        tint = Color(0xFF6750A4),
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "SECURE LOG TIMELINE",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onBackground,
                    letterSpacing = 1.5.sp
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Incidents contain sensitive audio recordings, location telemetry, and surveillance clips. Please enter credentials or authenticate below.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF49454F),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                Spacer(modifier = Modifier.height(26.dp))

                // Passcode entry visualization dot-grid
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 20.dp)
                ) {
                    repeat(4) { idx ->
                        val isFilled = enteredPin.length > idx
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .background(
                                    color = if (isFilled) Color(0xFF6750A4) else Color(0xFFEADDFF),
                                    shape = CircleShape
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (isFilled) Color(0xFF6750A4) else Color(0xFF9E95A5),
                                    shape = CircleShape
                                )
                        )
                    }
                }

                if (pinError) {
                    Text(
                        text = "Invalid passcode. Please try again! (Default: 1234)",
                        color = Color(0xFFB3261E),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }

                // Simple Digital Pinpad Layout
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.testTag("pinpad_container")
                ) {
                    val keys = listOf(
                        listOf("1", "2", "3"),
                        listOf("4", "5", "6"),
                        listOf("7", "8", "9"),
                        listOf("CLR", "0", "OK")
                    )

                    keys.forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            row.forEach { key ->
                                Box(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .background(Color(0xFFEADDFF).copy(alpha = 0.4f), CircleShape)
                                        .clickable {
                                            pinError = false
                                            when (key) {
                                                "CLR" -> {
                                                    if (enteredPin.isNotEmpty()) {
                                                        enteredPin = enteredPin.dropLast(1)
                                                    }
                                                }
                                                "OK" -> {
                                                    if (enteredPin == "1234" || enteredPin == "0000") {
                                                        isAuthorized = true
                                                    } else {
                                                        pinError = true
                                                        enteredPin = ""
                                                    }
                                                }
                                                else -> {
                                                    if (enteredPin.length < 4) {
                                                        enteredPin += key
                                                        // Auto submit if 4 numbers reached
                                                        if (enteredPin.length == 4) {
                                                            if (enteredPin == "1234" || enteredPin == "0000") {
                                                                isAuthorized = true
                                                            } else {
                                                                pinError = true
                                                                enteredPin = ""
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                        .testTag("pinpad_key_$key"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = key,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF21005D)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Simulative Biometric face-ID or Fingerprint authentication trigger
                Button(
                    onClick = {
                        showBiometricSuccessAnim = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEADDFF)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("biometric_unlocked_btn")
                ) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Fingerprint,
                            contentDescription = "Fingerprint verify icon",
                            tint = Color(0xFF6750A4),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Simulate TouchID / FaceID Login",
                            color = Color(0xFF21005D),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }

                Text(
                    text = "Hint: Passcode is default '1234'",
                    color = Color.Gray,
                    fontSize = 10.sp,
                    modifier = Modifier.padding(top = 8.dp)
                )

                if (showBiometricSuccessAnim) {
                    LaunchedEffect(Unit) {
                        delay(600)
                        isAuthorized = true
                        showBiometricSuccessAnim = false
                    }
                    AlertDialog(
                        onDismissRequest = {},
                        title = { Text("Biometrics Verified", fontWeight = FontWeight.Bold, color = Color(0xFF1D192B)) },
                        text = {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .background(Color(0xFF2E7D32), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.White)
                                }
                                Text("Handshake complete. AES-256 secure tunnel decapsulated.", fontSize = 12.sp)
                            }
                        },
                        confirmButton = {},
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                }
            }
        } else {
            // AUTHORIZED SECURE TIMELINE INTERFACE
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header Area with Quick Lock Button and Status Indicator
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(Color(0xFF00E676), CircleShape)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "DECRYPTED AES-256 SESSION ACTIVE",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF2E7D32),
                                letterSpacing = 0.5.sp
                            )
                        }
                        Text(
                            text = "DISTRESS INCIDENT TIMELINE",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    // Logout/Lock timeline again button
                    Box(
                        modifier = Modifier
                            .background(Color(0xFFF4EFF4), RoundedCornerShape(100.dp))
                            .border(1.dp, Color(0xFF9E95A5), RoundedCornerShape(100.dp))
                            .clickable {
                                isAuthorized = false
                                enteredPin = ""
                            }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                            .testTag("lock_session_btn"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFF6750A4), modifier = Modifier.size(12.dp))
                            Text("Lock logs", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF6750A4))
                        }
                    }
                }

                // Filter tabs & Diagnostic Injector Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf(
                            "ALL" to "All Logs",
                            "CRITICAL_HIGH" to "Critical / High",
                            "MED_LOW" to "Med / Low"
                        ).forEach { (filter, title) ->
                            val isSel = severityFilter == filter
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(
                                        color = if (isSel) Color(0xFF6750A4) else Color(0xFFEADDFF).copy(alpha = 0.4f),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable { severityFilter = filter }
                                    .padding(vertical = 8.dp)
                                    .testTag("filter_chip_$filter"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = title,
                                    color = if (isSel) Color.White else Color(0xFF21005D),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // SECURE TEST EVENT INJECTOR (Aesthetic simulation tools)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFECE6F0)),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCAC4D0))
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "DIAGNOSTIC EVENT GENERATION HUB",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF6750A4)
                            )
                            Text(
                                text = "ADMIN SIMULATOR",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier
                                    .background(Color(0xFF6750A4), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Generate realistic distress episodes to verify threat categorization, dispatch statuses, and multi-camera stream retrieval:",
                            fontSize = 10.sp,
                            color = Color(0xFF49454F)
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Button(
                                onClick = {
                                    viewModel.createSimulatedAlertEvent(
                                        triggerType = "POWER_BUTTON_TAPS",
                                        threatLevel = "CRITICAL",
                                        address = "1600 Amphitheatre Parkway, Mountain View, CA",
                                        transcript = "Help! There is an active security breach bypass attempt in our main lobby corridor! Send dispatch immediately!",
                                        briefAssessment = "The user is faced with an active high-risk home intrusion risk. High confidence security threat verbalized in vocal transcript.",
                                        suggestedAction = "Deploy urgent first responders & transmit AES streams to nearby authorities.",
                                        backupUrl = "https://images.unsplash.com/photo-1558002038-1055907df827?auto=format&fit=crop&w=400&q=80"
                                    )
                                },
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB3261E)),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(32.dp)
                                    .testTag("gen_critical_log")
                            ) {
                                Text("🚨 +Critical", fontSize = 9.sp, fontWeight = FontWeight.Black, color = Color.White)
                            }

                            Button(
                                onClick = {
                                    viewModel.createSimulatedAlertEvent(
                                        triggerType = "MANUAL_PANIC",
                                        threatLevel = "HIGH",
                                        address = "Villa Ridge Gateways, San Francisco, CA",
                                        transcript = "There is a suspicious individual lingering right under our balcony fence for the last fifteen minutes.",
                                        briefAssessment = "Potential physical reconnaissance event verified. Suspicious lingering alert with surrounding gate coverage.",
                                        suggestedAction = "Ping local tier 1 and tier 2 neighbors to initiate physical perimeter search.",
                                        backupUrl = "https://images.unsplash.com/photo-1582268611958-ebfd161ef9cf?auto=format&fit=crop&w=400&q=80"
                                    )
                                },
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE65100)),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(32.dp)
                                    .testTag("gen_high_log")
                            ) {
                                Text("⚠️ +High", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }

                            Button(
                                onClick = {
                                    viewModel.createSimulatedAlertEvent(
                                        triggerType = "POWER_BUTTON_TAPS",
                                        threatLevel = "MEDIUM",
                                        address = "855 West El Camino Real, Sunnyvale, CA",
                                        transcript = "Strange high pitch alarms going off down in our basement parking slot. Unknown cause.",
                                        briefAssessment = "General situational caution threat. Noise trigger with general building coverage.",
                                        suggestedAction = "Contact primary service and ping regional tier 1 contacts.",
                                        backupUrl = "https://images.unsplash.com/photo-1600585154340-be6161a56a0c?auto=format&fit=crop&w=400&q=80"
                                    )
                                },
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0288D1)),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(32.dp)
                                    .testTag("gen_med_log")
                            ) {
                                Text("⚡ +Medium", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }

                            Button(
                                onClick = {
                                    viewModel.createSimulatedAlertEvent(
                                        triggerType = "SILENT_PULSE",
                                        threatLevel = "LOW",
                                        address = "Silicon Valley Boulevard, San Jose, CA",
                                        transcript = "Stealth coordinate trace ping initiated.",
                                        briefAssessment = "Silent check-in tracking sequence triggered. No verbal emergency identified in transcript.",
                                        suggestedAction = "Transmit live coordinates feed to designated silent recipients.",
                                        backupUrl = "https://images.unsplash.com/photo-1506521781263-d8422e82f27a?auto=format&fit=crop&w=400&q=80"
                                    )
                                },
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF37474F)),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(32.dp)
                                    .testTag("gen_low_log")
                            ) {
                                Text("📳 +Silent", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                }

                // Filter our event list based on the user's active filter setting
                val filteredLogs = remember(eventsList, severityFilter) {
                    when (severityFilter) {
                        "CRITICAL_HIGH" -> eventsList.filter { it.aiThreatLevel == "CRITICAL" || it.aiThreatLevel == "HIGH" }
                        "MED_LOW" -> eventsList.filter { it.aiThreatLevel == "MEDIUM" || it.aiThreatLevel == "LOW" }
                        else -> eventsList
                    }
                }

                if (filteredLogs.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Inventory,
                            contentDescription = "Empty event timeline",
                            tint = Color(0xFF49454F),
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No events match this filter category",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF1D192B),
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Use the simulator panel above to instantly record logs.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF49454F),
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(filteredLogs) { event ->
                            SecureAlertEventItemCard(
                                event = event,
                                isPlaying = playingEventId == event.id,
                                playProgress = if (playingEventId == event.id) playProgress else 0f,
                                onPlayToggle = { viewModel.toggleAudioPlayback(event) },
                                onDelete = { viewModel.deleteAlertEvent(event) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SecureAlertEventItemCard(
    event: AlertEvent,
    isPlaying: Boolean,
    playProgress: Float,
    onPlayToggle: () -> Unit,
    onDelete: () -> Unit
) {
    val dateTimeStr = remember(event.timestamp) {
        val sdf = SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault())
        sdf.format(Date(event.timestamp))
    }

    // Relative elapsed time calculator
    val elapsedTimeStr = remember(event.timestamp) {
        val diffMs = System.currentTimeMillis() - event.timestamp
        val diffMins = diffMs / 60000
        val diffHours = diffMins / 60
        when {
            diffMins < 1 -> "Triggered just now"
            diffMins < 60 -> "Triggered $diffMins mins ago"
            diffHours < 24 -> "Triggered $diffHours hours ago"
            else -> {
                val days = diffHours / 24
                "Triggered $days days ago"
            }
        }
    }

    var isExpanded by remember { mutableStateOf(false) }

    // Multi-camera selection states for associated CCTV clips
    val associatedCameras = listOf(
        "CAM_01" to "Smart Doorbell Front",
        "CAM_02" to "Living Room CCTV",
        "CAM_03" to "Backyard Fence Walkway",
        "CAM_04" to "Garage Driveway Cam"
    )

    // Maps Cam code to image URL to support switching associated clips
    val cameraSampleUrls = mapOf(
        "CAM_01" to "https://images.unsplash.com/photo-1558002038-1055907df827?auto=format&fit=crop&w=400&q=80",
        "CAM_02" to "https://images.unsplash.com/photo-1600585154340-be6161a56a0c?auto=format&fit=crop&w=400&q=80",
        "CAM_03" to "https://images.unsplash.com/photo-1582268611958-ebfd161ef9cf?auto=format&fit=crop&w=400&q=80",
        "CAM_04" to "https://images.unsplash.com/photo-1506521781263-d8422e82f27a?auto=format&fit=crop&w=400&q=80"
    )

    var selectedCamKey by remember { mutableStateOf("CAM_01") }
    var streamPlayingState by remember { mutableStateOf(false) }
    var timelineScrubProgress by remember { mutableStateOf(0.42f) }

    // Dispatch system status description and styling matching threat levels
    val (statusLabel, statusColor, statusBg) = when (event.aiThreatLevel) {
        "CRITICAL" -> Triple("🚨 EMERGENCY DISPATCH ACTIVE (911 SENT)", Color(0xFFC62828), Color(0xFFFFEBEE))
        "HIGH" -> Triple("🛡️ SECURITY GUARDIANS ALERTED", Color(0xFFE65100), Color(0xFFFFF3E0))
        "MEDIUM" -> Triple("⚠️ NEIGHBORHOOD WARNING ROUTED", Color(0xFF1565C0), Color(0xFFE3F2FD))
        else -> Triple("📳 SILENT CLOUD-ONLY LOGGED", Color(0xFF37474F), Color(0xFFECEFF1))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
            .clickable { isExpanded = !isExpanded }
            .testTag("event_card_${event.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row: Trigger classification + dispatch badge + Delete option
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .background(
                                color = if (event.triggersUsed == "POWER_BUTTON_TAPS") Color(0xFFECE6F0) else Color(0xFFFFD8E4),
                                shape = RoundedCornerShape(6.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (event.triggersUsed == "POWER_BUTTON_TAPS") "POWER TAP ACTIVATION" else "MANUAL TRIGGER PANIC",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            color = if (event.triggersUsed == "POWER_BUTTON_TAPS") Color(0xFF6750A4) else Color(0xFFB3261E)
                        )
                    }

                    // Threat level tag
                    Box(
                        modifier = Modifier
                            .background(
                                color = when (event.aiThreatLevel) {
                                    "CRITICAL" -> Color(0xFFB3261E)
                                    "HIGH" -> Color(0xFFE65100)
                                    "MEDIUM" -> Color(0xFF0288D1)
                                    else -> Color(0xFF546E7A)
                                },
                                shape = RoundedCornerShape(6.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = event.aiThreatLevel,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                    }
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .size(28.dp)
                        .testTag("delete_event_button_${event.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Remove event",
                        tint = Color(0xFFB3261E).copy(alpha = 0.8f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Timestamps row (including both formatted date and dynamic relative trackers)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = dateTimeStr.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF6750A4),
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 11.sp
                )
                Text(
                    text = elapsedTimeStr,
                    fontSize = 10.sp,
                    color = Color.Gray,
                    fontWeight = FontWeight.Bold
                )
            }

            // Location coordinates information
            Text(
                text = "📍 " + event.locationAddress,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 6.dp, bottom = 4.dp)
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "GPS: ${String.format("%.5f", event.latitude)}, ${String.format("%.5f", event.longitude)}",
                    fontSize = 11.sp,
                    color = Color(0xFF49454F),
                    fontWeight = FontWeight.Bold
                )
                Box(modifier = Modifier.size(4.dp).background(Color(0xFF49454F), CircleShape))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CloudQueue, contentDescription = null, tint = Color(0xFF2E7D32), modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "SECURE CLOUD-STORED",
                        fontSize = 9.sp,
                        color = Color(0xFF2E7D32),
                        fontWeight = FontWeight.Black
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // SECURE DISPATCH STATUS BANNER
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(statusBg, RoundedCornerShape(8.dp))
                    .border(0.5.dp, statusColor.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                    .padding(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(statusColor, CircleShape)
                    )
                    Text(
                        text = statusLabel,
                        color = statusColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Emergency Distress Audio Clips replay section
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFFEADDFF), RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF7F2FA)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onPlayToggle,
                        colors = IconButtonDefaults.iconButtonColors(containerColor = Color(0xFF6750A4)),
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("play_audio_button_${event.id}")
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "Replay audio stream",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isPlaying) "DECAPPED AUDIO FLOWING..." else "10s HIGH-FIDELITY STREAM.MP3",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF21005D),
                            fontWeight = FontWeight.Black,
                            fontSize = 9.sp
                        )

                        Spacer(modifier = Modifier.height(2.dp))

                        SimulatedAudioWaveform(
                            playbackProgress = playProgress,
                            accentColor = Color(0xFF6750A4)
                        )
                    }
                }
            }

            // EXPANDABLE INTELLIGENCE PORTAL (CCTV switcher + AI Summaries)
            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))

                    // ADVANCED ASSOCIATED CCTV INTEGRATION MODULE
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "ASSOCIATED SECURITY TELEMETRY CLIPS",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Black,
                                fontSize = 10.sp
                            )
                            Text(
                                text = "MULTI-CAM DECRYPTER",
                                fontSize = 8.sp,
                                color = Color(0xFF6750A4),
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Interactive associated clips selector chips row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            associatedCameras.forEach { (key, label) ->
                                val isSelected = selectedCamKey == key
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .background(
                                            color = if (isSelected) Color(0xFF6750A4) else Color(0xFFF4EFF4),
                                            shape = RoundedCornerShape(6.dp)
                                        )
                                        .border(
                                            width = 1.dp,
                                            color = if (isSelected) Color(0xFF6750A4) else Color(0xFFCAC4D0),
                                            shape = RoundedCornerShape(6.dp)
                                        )
                                        .clickable {
                                            selectedCamKey = key
                                            // Trigger minor play seek state reset
                                            timelineScrubProgress = (30..80).random() / 100f
                                        }
                                        .padding(vertical = 6.dp)
                                        .testTag("cam_selector_${event.id}_$key"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label.split(" ").last(),
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Black,
                                        color = if (isSelected) Color.White else Color(0xFF49454F)
                                    )
                                }
                            }
                        }

                        // Video frame with real-time HUD overlays
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.Black)
                                .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                        ) {
                            // Selected video clip thumbnail URL
                            val cctvSnapshot = cameraSampleUrls[selectedCamKey] ?: event.backupCloudUrl
                            val painter = rememberAsyncImagePainter(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(cctvSnapshot.ifBlank { "https://images.unsplash.com/photo-1558002038-1055907df827?auto=format&fit=crop&w=400&q=80" })
                                    .crossfade(true)
                                    .build()
                            )
                            Image(
                                painter = painter,
                                contentDescription = "Surveillance footage associated frame replay",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )

                            // Telemetry scanning watermarks
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(
                                                Color.Black.copy(alpha = 0.4f),
                                                Color.Transparent,
                                                Color.Black.copy(alpha = 0.6f)
                                            )
                                        )
                                    )
                            )

                            // Telemetry metadata layout
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(8.dp),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .background(if (streamPlayingState) Color.Red else Color.LightGray, CircleShape)
                                        )
                                        Text(
                                            text = "REC [$selectedCamKey] • LIVE RECOVERED",
                                            color = Color.White,
                                            fontWeight = FontWeight.Black,
                                            fontSize = 8.sp,
                                            modifier = Modifier
                                                .background(Color.Black.copy(0.6f), RoundedCornerShape(2.dp))
                                                .padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                    }

                                    Text(
                                        text = "1085 Kbps • AES-SYNCED",
                                        color = Color(0xFF00E676),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 8.sp,
                                        modifier = Modifier
                                            .background(Color.Black.copy(0.6f), RoundedCornerShape(2.dp))
                                            .padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = associatedCameras.find { it.first == selectedCamKey }?.second ?: "CCTV STREAM",
                                        color = Color.LightGray,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier
                                            .background(Color.Black.copy(0.5f), RoundedCornerShape(2.dp))
                                            .padding(horizontal = 4.dp)
                                    )

                                    Text(
                                        text = "SEQ OFFSET: -${String.format("%.1fs", 10f * (1f - timelineScrubProgress))}",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 8.sp,
                                        modifier = Modifier
                                            .background(Color.Black.copy(0.5f), RoundedCornerShape(2.dp))
                                            .padding(horizontal = 4.dp)
                                    )
                                }
                            }

                            // Interactive Play Trigger simulation overlay
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    modifier = Modifier
                                        .background(Color.Black.copy(alpha = 0.75f), RoundedCornerShape(100.dp))
                                        .clickable { streamPlayingState = !streamPlayingState }
                                        .padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = if (streamPlayingState) Icons.Default.PauseCircle else Icons.Default.PlayCircle,
                                        contentDescription = "Simulate stream play button",
                                        tint = if (streamPlayingState) Color(0xFF00E676) else Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = if (streamPlayingState) "PAUSE STREAM REPLAY" else "STRETCH DECODED PLAYBACK",
                                        color = Color.White,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 8.sp
                                    )
                                }
                            }
                        }

                        // Simulation Scrubbing seeker bar matching uncompressed feed
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("0:00", fontSize = 8.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                            Slider(
                                value = timelineScrubProgress,
                                onValueChange = { timelineScrubProgress = it },
                                colors = SliderDefaults.colors(
                                    thumbColor = Color(0xFF6750A4),
                                    activeTrackColor = Color(0xFF6750A4),
                                    inactiveTrackColor = Color(0xFFEADDFF)
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(18.dp)
                                    .testTag("cctv_time_scrubber_${event.id}")
                            )
                            Text("0:10", fontSize = 8.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Security audio vocal transcription text box
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFF4EFF4), RoundedCornerShape(12.dp))
                            .border(1.dp, Color(0xFFCAC4D0), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.ReceiptLong,
                                    contentDescription = "Transcript icon",
                                    tint = Color(0xFF49454F),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "DECRYPTED MICROPHONE TRANSCRIPT",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF21005D),
                                    fontWeight = FontWeight.Black,
                                    fontSize = 9.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (event.audioTranscript.isNotBlank()) "\"${event.audioTranscript}\"" else "\"Vocal transcription processing completed. Silence or low-amplitude murmurs detected in emergency sequence.\"",
                                fontSize = 11.sp,
                                color = Color(0xFF49454F),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }

                    // GEMINI THREAT GUARDIAN SMART REPORT
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color(0xFFFFB74D), RoundedCornerShape(16.dp)),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = "Gemini logo",
                                        tint = Color(0xFFE65100),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "AI MODEL DIAGNOSTIC ASSESSMENT",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = Color(0xFFE65100),
                                        fontWeight = FontWeight.Black,
                                        fontSize = 10.sp
                                    )
                                }

                                Badge(
                                    containerColor = when (event.aiThreatLevel) {
                                        "CRITICAL" -> Color(0xFFB3261E)
                                        "HIGH" -> Color(0xFFE65100)
                                        "MEDIUM" -> Color(0xFF0288D1)
                                        else -> Color(0xFF546E7A)
                                    },
                                    contentColor = Color.White
                                ) {
                                    Text(
                                        text = event.aiThreatLevel,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Black,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = if (event.aiBriefAssessment.isNotBlank()) event.aiBriefAssessment else "Unassessed telemetry log. General status verification sequence record.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF4E342E),
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Campaign,
                                    contentDescription = "Alert action guidance icon",
                                    tint = Color(0xFFD84315),
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "DISPATCH DISCOVERY: ${if (event.aiSuggestedAction.isNotBlank()) event.aiSuggestedAction else "Archive logs securely."}",
                                    fontSize = 10.sp,
                                    color = Color(0xFFD84315),
                                    fontWeight = FontWeight.Black,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }

            if (!isExpanded) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Tap to decrypt full CCTV snapshots & system dispatches",
                        fontSize = 11.sp,
                        color = Color(0xFF6750A4),
                        fontWeight = FontWeight.ExtraBold
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.ExpandMore,
                        contentDescription = "Expand detailed details click indicator",
                        tint = Color(0xFF6750A4),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun SimulatedAudioWaveform(
    playbackProgress: Float,
    accentColor: Color
) {
    val barCount = 36
    val heights = remember {
        List(barCount) { (12..36).random() }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(28.dp)
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        heights.forEachIndexed { idx, barH ->
            val playedVal = idx.toFloat() / barCount
            val barColored = playedVal <= playbackProgress

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(barH.dp)
                    .clip(RoundedCornerShape(100.dp))
                    .background(if (barColored) accentColor else Color(0xFFDCD6D3))
            )
        }
    }
}
