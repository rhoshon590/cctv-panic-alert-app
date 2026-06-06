package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.PowerSettingsNew
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.example.data.CctvFeed
import com.example.ui.MainViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun DashboardScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val isAlertActive by viewModel.isAlertActive.collectAsState()
    val countdownActive by viewModel.countdownActive.collectAsState()
    val alertTriggerSource by viewModel.alertTriggerSource.collectAsState()
    val isSilentMode by viewModel.isSilentAlertMode.collectAsState()
    val currentLocation by viewModel.currentLocationName.collectAsState()

    val cctvFeedsList by viewModel.cctvFeeds.collectAsState()
    var activeVerificationFeed by remember { mutableStateOf<CctvFeed?>(null) }
    val alertEvents by viewModel.alertEvents.collectAsState()

    // Cloud integrity simulation scanning state
    var isIntegrityScanning by remember { mutableStateOf(false) }
    var scanMessage by remember { mutableStateOf("Standby") }
    val coroutineScope = rememberCoroutineScope()

    // Upload & Sync Monitoring
    val audioProgress by viewModel.audioUploadProgress.collectAsState()
    val videoProgress by viewModel.videoUploadProgress.collectAsState()
    val dispatcherStatus by viewModel.dispatcherSyncStatus.collectAsState()
    val cctvStreamName by viewModel.cctvCaptureStreamName.collectAsState()
    val isPowerButtonHapticEnabled by viewModel.isPowerButtonHapticEnabled.collectAsState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. System active header (from design mockup)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "SYSTEM STATUS",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF64748B), // Slate 500
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(Color(0xFF2E7D32), CircleShape)
                        )
                        Text(
                            text = "SecureGuard Active",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1D192B)
                        )
                    }
                }
                // JD user avatar circle
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color(0xFFEADDFF), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "JD",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF21005D)
                    )
                }
            }

            // 2. Active GPS Lock card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.GpsFixed,
                        contentDescription = "GPS Tracker Active",
                        tint = if (isAlertActive) Color(0xFFD32F2F) else Color(0xFF2E7D32),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "ACTIVE SECURE GPS POSITION",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF64748B),
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = currentLocation,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1
                        )
                    }
                }
            }

            // 2b. Secure Guard Cloud Data Sync Indicator Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("cloud_sync_indicator_card"),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(
                                        if (isAlertActive) Color(0xFFFFF3E0) else Color(0xFFE8F5E9),
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isAlertActive) Icons.Default.CloudUpload else Icons.Default.CloudDone,
                                    contentDescription = "Cloud Storage Sync Indicator",
                                    tint = if (isAlertActive) Color(0xFFE65100) else Color(0xFF2E7D32),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "SECURE SYNC ROUTER",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF64748B),
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                )
                                Text(
                                    text = if (isAlertActive) "Offloading Live Dispatch Stream" else "Cloud Vault Connected",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        // Compact badge for connection state
                        Box(
                            modifier = Modifier
                                .background(
                                    if (isAlertActive) Color(0xFFFFCC80) else Color(0xFFA5D6A7),
                                    RoundedCornerShape(100.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = if (isAlertActive) "SYNC ACTIVE" else "SECURED",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Black,
                                color = if (isAlertActive) Color(0xFFE65100) else Color(0xFF1B5E20)
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

                    if (isAlertActive) {
                        // Current Realtime Live Syncing Monitor
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Streaming AES Encrypted Packets",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF49454F),
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "${(audioProgress * 100).toInt()}% • ${(videoProgress * 100).toInt()}%",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFF6750A4)
                                )
                            }

                            // Composite Progress bar
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(100.dp))
                                    .background(Color(0xFFEADDFF).copy(alpha = 0.4f))
                            ) {
                                val compositeProgress = (audioProgress + videoProgress) / 2f
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .fillMaxWidth(fraction = compositeProgress.coerceIn(0f, 1f))
                                        .background(
                                            Brush.horizontalGradient(
                                                colors = listOf(Color(0xFF6750A4), Color(0xFF388E3C))
                                            ),
                                            RoundedCornerShape(100.dp)
                                        )
                                )
                            }

                            Text(
                                text = "Tunneling video pipeline feed ($cctvStreamName) & live audio slice to secondary offsite redundant servers.",
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 10.sp,
                                color = Color(0xFF64748B),
                                lineHeight = 13.sp
                            )
                        }
                    } else {
                        // Standing Status Monitor
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                val syncedCount = alertEvents.count { it.isCloudSynced }
                                Text(
                                    text = "Offloaded Clips Archive",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF64748B),
                                    fontWeight = FontWeight.Bold
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CloudDone,
                                        contentDescription = "Synced checklist",
                                        tint = Color(0xFF2E7D32),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = if (syncedCount > 0) {
                                            "$syncedCount Incident Records Synced"
                                        } else {
                                            "0 Incident Records Standby"
                                        },
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFF1D192B)
                                    )
                                }
                            }

                            Column(
                                horizontalAlignment = Alignment.End,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "Tunnel Protocol",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF64748B),
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "TLS 1.3 / AES-256",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF6750A4)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(2.dp))

                        // Interactive Verification scan simulator
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (isIntegrityScanning) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFFEADDFF).copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                        .border(0.5.dp, Color(0xFF6750A4).copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                        color = Color(0xFF6750A4)
                                    )
                                    Text(
                                        text = scanMessage,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF21005D)
                                    )
                                }
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(36.dp)
                                        .background(Color(0xFFF4EFF4), RoundedCornerShape(8.dp))
                                        .border(1.dp, Color(0xFFCAC4D0), RoundedCornerShape(8.dp))
                                        .clickable {
                                            coroutineScope.launch {
                                                isIntegrityScanning = true
                                                scanMessage = "Opening TLS tunnel parameters..."
                                                delay(500)
                                                scanMessage = "Validating MD5 checksums of offloaded records..."
                                                delay(600)
                                                scanMessage = "100% data integrity verified successfully."
                                                delay(400)
                                                isIntegrityScanning = false
                                            }
                                        }
                                        .testTag("verify_vault_integrity_btn"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Sync,
                                            contentDescription = "Test network",
                                            tint = Color(0xFF6750A4),
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            text = "Verify Vault integrity & Sync Hashes",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color(0xFF6750A4)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 3. Triple-Tap Trigger soft red urgent banner card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.simulatePowerButtonTaps() },
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF2B8B5))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .background(Color.White.copy(alpha = 0.40f), RoundedCornerShape(10.dp))
                                .padding(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = "Panic Trigger SOS Icon",
                                tint = Color(0xFF410002),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        
                        Box(
                            modifier = Modifier
                                .background(Color(0xFF8C1D18), RoundedCornerShape(100.dp))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "PANIC ARMED",
                                color = Color.White,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                    
                    Column {
                        Text(
                            text = "Triple-Tap Trigger",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF410002)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Press power button 3x to auto-record 10s audio and notify emergency contacts with GPS.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF601410),
                            lineHeight = 15.sp
                        )
                    }
                }
            }

            // 4. Custom POLISHED SURVEILLANCE FEED CONTROLS
            Card(
                modifier = Modifier.fillMaxWidth().testTag("surveillance_hub_card"),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CellTower,
                                contentDescription = "Surveillance networks",
                                tint = Color(0xFF6750A4),
                                modifier = Modifier.size(24.dp)
                            )
                            Column {
                                Text(
                                    text = "Linked Security Feeds",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${cctvFeedsList.size} CCTV channels connected",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF49454F)
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .background(Color(0xFFE2F0D9), RoundedCornerShape(100.dp))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "MONITOR ACTIVE",
                                color = Color(0xFF2E7D32),
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    if (cctvFeedsList.isEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.VideocamOff,
                                contentDescription = null,
                                tint = Color(0xFF49454F),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "No CCTV linked. Add feeds in Config tab.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF49454F)
                            )
                        }
                    } else {
                        // Elegant list of all connected feeds
                        Column(
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            cctvFeedsList.forEach { feed ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            if (feed.isEnabled) MaterialTheme.colorScheme.surfaceVariant else Color(0xFFF1F1F1),
                                            RoundedCornerShape(12.dp)
                                        )
                                        .border(
                                            1.dp,
                                            if (feed.isEnabled) MaterialTheme.colorScheme.outlineVariant else Color(0xFFE0E0E0),
                                            RoundedCornerShape(12.dp)
                                        )
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        // Miniature Feed icon
                                        Box(
                                            modifier = Modifier
                                                .size(46.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(Color.Black),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Image(
                                                painter = rememberAsyncImagePainter(
                                                    ImageRequest.Builder(LocalContext.current)
                                                        .data(data = feed.streamUrl)
                                                        .apply(block = fun ImageRequest.Builder.() {
                                                            crossfade(true)
                                                        }).build()
                                                ),
                                                contentDescription = null,
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Crop
                                            )
                                            // Tiny status indicator dot
                                            Box(
                                                modifier = Modifier
                                                    .align(Alignment.TopStart)
                                                    .padding(2.dp)
                                                    .size(6.dp)
                                                    .background(
                                                        if (feed.isEnabled) Color(0xFF2E7D32) else Color(0xFF546E7A),
                                                        CircleShape
                                                    )
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(10.dp))

                                        Column {
                                            Text(
                                                text = feed.name,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1
                                            )
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "📍 ${feed.location}",
                                                    fontSize = 10.sp,
                                                    color = Color(0xFF49454F),
                                                    maxLines = 1
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Box(
                                                    modifier = Modifier
                                                        .background(Color(0xFFEADDFF), RoundedCornerShape(100.dp))
                                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                                ) {
                                                    Text(
                                                        text = "${String.format("%.1f", viewModel.getFeedDistanceMeters(feed))}m",
                                                        fontSize = 8.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color(0xFF21005D)
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    Button(
                                        onClick = { activeVerificationFeed = feed },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (feed.isEnabled) Color(0xFF6750A4) else Color(0xFF49454F).copy(alpha = 0.12f),
                                            contentColor = if (feed.isEnabled) Color.White else Color(0xFF49454F)
                                        ),
                                        elevation = null,
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier
                                            .height(32.dp)
                                            .testTag("request_live_feed_${feed.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.LiveTv,
                                            contentDescription = "Test Stream",
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Request Live Feed",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Audio buffer underneath
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = "Active Mic Buffers",
                                tint = Color(0xFF6750A4),
                                modifier = Modifier.size(16.dp)
                            )
                            Column {
                                Text(
                                    text = "Primary Mic Loop Buffer",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1D192B)
                                )
                                Text(
                                    text = "Ready to record 10s audio slices upon power click",
                                    fontSize = 9.sp,
                                    color = Color(0xFF49454F)
                                )
                            }
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.height(20.dp)
                        ) {
                            Box(modifier = Modifier.background(Color(0xFF6750A4), RoundedCornerShape(100.dp)).width(3.dp).height(12.dp))
                            Box(modifier = Modifier.background(Color(0xFF6750A4), RoundedCornerShape(100.dp)).width(3.dp).height(18.dp))
                            Box(modifier = Modifier.background(Color(0xFF6750A4), RoundedCornerShape(100.dp)).width(3.dp).height(8.dp))
                            Box(modifier = Modifier.background(Color(0xFF6750A4), RoundedCornerShape(100.dp)).width(3.dp).height(15.dp))
                        }
                    }
                }
            }

            // 5. Central Interactive SOS trigger (Conditional display or integrated)
            if (!isAlertActive) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "PANICLINK SHIELD LAYER",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF6750A4),
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
                    )
                    Text(
                        text = "Hold down or tap to raise silent alert",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF49454F),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    // Compact but visually active core pulsing panic activator
                    PulsingPanicIndicator(
                        onClick = { viewModel.triggerOversizedPanicButton() }
                    )
                }
            } else {
                // Active alarm progress - sort CCTV feeds by proximity before presenting
                val proximitySortedFeeds = remember(cctvFeedsList) {
                    cctvFeedsList.sortedBy { viewModel.getFeedDistanceMeters(it) }
                }

                ActiveAlertDashboard(
                    countdown = countdownActive,
                    triggerSource = alertTriggerSource,
                    isSilent = isSilentMode,
                    audioProgress = audioProgress,
                    videoProgress = videoProgress,
                    dispatcherStatus = dispatcherStatus,
                    cctvStreamName = cctvStreamName,
                    cctvFeeds = proximitySortedFeeds,
                    onRequestLiveFeed = { activeVerificationFeed = it },
                    onDismiss = { viewModel.dismissActiveAlert() },
                    viewModel = viewModel,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // 6. Silent trigger settings config
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = if (isSilentMode) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                            contentDescription = "Stealth Mode status",
                            tint = if (isSilentMode) Color(0xFFE65100) else Color(0xFF2E7D32),
                            modifier = Modifier.size(24.dp)
                        )
                        Column {
                            Text(
                                text = "Silent Dispatch Mode",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (isSilentMode) "Stealth alerts to contacts" else "Siren triggered instantly",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF49454F)
                            )
                        }
                    }
                    Switch(
                        checked = isSilentMode,
                        onCheckedChange = { viewModel.toggleSilentAlertMode(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color(0xFFE65100),
                            checkedTrackColor = Color(0xFFFFF3E0)
                        ),
                        modifier = Modifier.testTag("silent_mode_switch")
                    )
                }
            }

            // 6c. Power Button Haptic Feedback Confirmation setting
            Card(
                modifier = Modifier.fillMaxWidth().testTag("haptic_feedback_card"),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = if (isPowerButtonHapticEnabled) Icons.Default.Vibration else Icons.Default.VolumeMute,
                            contentDescription = "Haptic feedback status",
                            tint = if (isPowerButtonHapticEnabled) Color(0xFF6750A4) else Color(0xFF64748B),
                            modifier = Modifier.size(24.dp)
                        )
                        Column {
                            Text(
                                text = "Power Trigger Vibration",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (isPowerButtonHapticEnabled) "Silent confirmation pulse on" else "No physical confirmation",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF49454F)
                            )
                        }
                    }
                    Switch(
                        checked = isPowerButtonHapticEnabled,
                        onCheckedChange = { viewModel.togglePowerButtonHaptic(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color(0xFF6750A4),
                            checkedTrackColor = Color(0xFFEADDFF)
                        ),
                        modifier = Modifier.testTag("haptic_feedback_switch")
                    )
                }
            }

            // 6b. Background Reliability & Persistence card
            Card(
                modifier = Modifier.fillMaxWidth().testTag("background_reliability_card"),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = "Background Shield status",
                                tint = Color(0xFF6750A4),
                                modifier = Modifier.size(24.dp)
                            )
                            Column {
                                Text(
                                    text = "Run in Phone Background",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Always Monitoring via Active Foreground Service",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF49454F)
                                )
                            }
                        }
                        
                        Box(
                            modifier = Modifier
                                .background(Color(0xFFE8DEF8), RoundedCornerShape(100.dp))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "PERSISTENT",
                                color = Color(0xFF21005D),
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    Text(
                        text = "To safeguard protection and prevent Android from stopping the active power button listener when the app is closed, allow unrestricted background activity.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF49454F),
                        lineHeight = 15.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val context = androidx.compose.ui.platform.LocalContext.current
                        Button(
                            onClick = {
                                try {
                                    val pm = context.getSystemService(android.content.Context.POWER_SERVICE) as android.os.PowerManager
                                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                                        val isIgnoring = pm.isIgnoringBatteryOptimizations(context.packageName)
                                        if (!isIgnoring) {
                                            val intent = android.content.Intent(android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                                                data = android.net.Uri.parse("package:${context.packageName}")
                                                flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                                            }
                                            context.startActivity(intent)
                                        } else {
                                            android.widget.Toast.makeText(context, "Battery Optimization already disabled!", android.widget.Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                } catch (e: Exception) {
                                    try {
                                        val intent = android.content.Intent(android.provider.Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
                                            flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                                        }
                                        context.startActivity(intent)
                                    } catch (ex: Exception) {
                                        android.widget.Toast.makeText(context, "Search battery whitelist settings manually for PanicLink.", android.widget.Toast.LENGTH_LONG).show()
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEADDFF)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.BatteryAlert,
                                contentDescription = "Disable battery restriction",
                                tint = Color(0xFF21005D),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "Disable Optimization",
                                color = Color(0xFF21005D),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Button(
                            onClick = {
                                try {
                                    val intent = android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                        data = android.net.Uri.parse("package:${context.packageName}")
                                        flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                                    }
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    android.widget.Toast.makeText(context, "Unable to launch system settings.", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Open App Settings",
                                tint = Color(0xFF49454F),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "App Settings",
                                color = Color(0xFF1D192B),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // 7. Simulators toolbox (for verification)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "HARDWARE SIMULATION TRACK",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF64748B),
                    fontWeight = FontWeight.Bold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { viewModel.simulatePowerButtonTaps() },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier
                            .weight(1f)
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                            .testTag("simulate_power_button_taps"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PowerSettingsNew,
                            contentDescription = "Trigger Taps",
                            tint = Color(0xFF6750A4),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Power 3-Taps",
                            color = Color(0xFF1D192B),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Button(
                        onClick = { viewModel.triggerOversizedPanicButton() },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier
                            .weight(1f)
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                            .testTag("simulate_panic_tap"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Campaign,
                            contentDescription = "Instant Alert Trigger",
                            tint = Color(0xFF8C1D18),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Manual Alert",
                            color = Color(0xFF1D192B),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        if (activeVerificationFeed != null) {
            LiveFeedVerificationDialog(
                feed = activeVerificationFeed!!,
                onDismiss = { activeVerificationFeed = null }
            )
        }
    }
}

@Composable
fun PulsingPanicIndicator(
    onClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "Pulse")
    
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseScale"
    )

    val waveAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "WaveAlpha"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(260.dp)
            .clickable(onClick = onClick)
            .testTag("giant_panic_button")
    ) {
        // Outer pulsing ring
        Canvas(
            modifier = Modifier
                .size(240.dp)
                .scale(pulseScale)
        ) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFFF1744).copy(alpha = 0.25f), Color.Transparent)
                ),
                radius = size.width / 2f
            )
            drawCircle(
                color = Color(0xFFFF1744).copy(alpha = waveAlpha),
                radius = size.width / 3.4f,
                style = Stroke(width = 3.dp.toPx())
            )
        }

        // Inner solid activation trigger
        Box(
            modifier = Modifier
                .size(160.dp)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFFFF1744), Color(0xFFD50000))
                    ),
                    shape = CircleShape
                )
                .border(6.dp, Color.White.copy(alpha = 0.4f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = "Guard Badge",
                    tint = Color.White,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "TAP PANIC",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}

@Composable
fun ActiveAlertDashboard(
    countdown: Int,
    triggerSource: String,
    isSilent: Boolean,
    audioProgress: Float,
    videoProgress: Float,
    dispatcherStatus: String,
    cctvStreamName: String,
    cctvFeeds: List<CctvFeed>,
    onRequestLiveFeed: (CctvFeed) -> Unit,
    onDismiss: () -> Unit,
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val captureThumbnail by viewModel.cctvCaptureThumbnail.collectAsState()
    val currentLocationName by viewModel.currentLocationName.collectAsState()

    val radarTransition = rememberInfiniteTransition(label = "RadarSweep")
    val radarSweepAngle by radarTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "RadarSweepAngle"
    )
    val epicenterPulseRadius by radarTransition.animateFloat(
        initialValue = 4f,
        targetValue = 32f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "EpicenterPulseRadius"
    )
    val liveMarkerAlpha by radarTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "LiveMarkerAlpha"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceEvenly
    ) {
        // Upper Timer and Alarm status
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = if (isSilent) "Stealth Silent Alarm Engaged" else "🚨 AUDIBLE LOUD PANIC DISPATCH",
                style = MaterialTheme.typography.titleMedium,
                color = if (isSilent) Color(0xFFFF9100) else Color(0xFFFF1744),
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(Color(0xFFFF1744), CircleShape)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "RECORDING 10s STREAM",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.LightGray,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Animated giant digital timer
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(130.dp)
        ) {
            CircularProgressIndicator(
                progress = { countdown / 10f },
                modifier = Modifier.size(130.dp),
                color = Color(0xFFFF1744),
                strokeWidth = 10.dp,
                trackColor = Color.White.copy(alpha = 0.1f)
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "$countdown",
                    fontSize = 44.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
                Text(
                    text = "SECONDS",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Live Real-Time Upload status Monitor
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E22))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Live Mic Level Simulation indicator
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Mic stream status",
                        tint = Color(0xFF00E5FF),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Realtime Audio Sync",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.LightGray,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.width(130.dp)
                    )
                    LinearProgressIndicator(
                        progress = { audioProgress },
                        modifier = Modifier
                            .weight(1f)
                            .height(6.dp)
                            .clip(CircleShape),
                        color = Color(0xFF00E5FF),
                        trackColor = Color.White.copy(alpha = 0.05f)
                    )
                }

                // CCTV Stream video backup monitor
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Videocam,
                        contentDescription = "Cctv Sync status",
                        tint = Color(0xFF00E676),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "CCTV Link ($cctvStreamName)",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.LightGray,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.width(130.dp),
                        maxLines = 1
                    )
                    LinearProgressIndicator(
                        progress = { videoProgress },
                        modifier = Modifier
                            .weight(1f)
                            .height(6.dp)
                            .clip(CircleShape),
                        color = Color(0xFF00E676),
                        trackColor = Color.White.copy(alpha = 0.05f)
                    )
                }

                // Dispatch line connection response
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.SafetyCheck,
                            contentDescription = "Security services icon",
                            tint = Color.White.copy(alpha = 0.6f),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Responder Dispatch",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.LightGray,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        text = when (dispatcherStatus) {
                            "DISPATCHING" -> "BROADCASTING FEED"
                            "SENT_REALTIME" -> "LIVE UPLOADED 100%"
                            "CONFIRMED" -> "DISPATCH CONFIRMED"
                            else -> "PENDING UPLOAD"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = when (dispatcherStatus) {
                            "DISPATCHING" -> Color(0xFFFF9100)
                            "SENT_REALTIME" -> Color(0xFF00E5FF)
                            "CONFIRMED" -> Color(0xFF00E676)
                            else -> Color.Gray
                        },
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // --- 🚨 LIVE INCIDENT FOOTAGE & GPS COORDS DISPATCH MONITOR ---
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("live_incident_footage_card"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E22)),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.12f))
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header of position locator
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Explore,
                            contentDescription = "Alert positioning radar",
                            tint = Color(0xFFFF1744),
                            modifier = Modifier.size(20.dp)
                        )
                        Column {
                            Text(
                                text = "INCIDENT DISPATCH GEOLOCATION",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.Gray,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = "Secure Realtime Spatial Tracking",
                                style = MaterialTheme.typography.titleSmall,
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .background(Color(0xFFFF1744).copy(alpha = 0.15f), RoundedCornerShape(100.dp))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "LOCATION LOCKED",
                            color = Color(0xFFFF8A80),
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }

                HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

                // Physical Epicenter Address
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White.copy(alpha = 0.04f), RoundedCornerShape(8.dp))
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = "Alarm trigger position",
                            tint = Color(0xFFFF8A80),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = currentLocationName,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = "📍 Latitude: 37.77490° N, Longitude: -122.41940° W (Tolerance Range: +/- 3.2m)",
                        fontSize = 9.sp,
                        color = Color.Gray,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(start = 22.dp)
                    )
                }

                // Dual visual monitor section: Live Video Frame left, pulsating radar scanner right
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Pane 1: Live Video Feed Box
                    Column(
                        modifier = Modifier
                            .weight(1.2f)
                            .height(130.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.Black)
                            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp)),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            if (captureThumbnail != null) {
                                Image(
                                    painter = rememberAsyncImagePainter(
                                        ImageRequest.Builder(LocalContext.current)
                                            .data(data = captureThumbnail)
                                            .crossfade(true)
                                            .build()
                                    ),
                                    contentDescription = "Live incident video feed",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                // Fallback loading/connecting frame
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    CircularProgressIndicator(
                                        color = Color(0xFFFF1744),
                                        strokeWidth = 3.dp,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Text(
                                        text = "Connecting pipeline feed...",
                                        fontSize = 8.sp,
                                        color = Color.LightGray
                                    )
                                }
                            }

                            // Watermark / timestamp and live dot overlays
                            Row(
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(6.dp)
                                    .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 4.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .scale(liveMarkerAlpha)
                                        .background(Color(0xFFFF1744), CircleShape)
                                )
                                Text(
                                    text = "LIVE BROADCAST",
                                    fontSize = 7.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(6.dp)
                                    .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "AES-256 SYNC",
                                    fontSize = 6.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF00E676)
                                )
                            }
                        }

                        // Stream source bottom ribbon
                        Box(
                            modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color.White.copy(alpha = 0.08f))
                                        .padding(vertical = 4.dp, horizontal = 6.dp)
                        ) {
                            Text(
                                text = "SOURCE: $cctvStreamName",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1
                            )
                        }
                    }

                    // Pane 2: Pulsating GPS Radar target sweeps Canvas
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .height(130.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF141416))
                            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                            .padding(6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "EPICENTER RADAR SWEEP",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.Gray
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val centerX = size.width / 2f
                                val centerY = size.height / 2f
                                val maxRadius = minOf(size.width, size.height) / 2f - 4f

                                // Grid Coaxial circles
                                for (i in 1..3) {
                                    drawCircle(
                                        color = Color(0xFFFF1744).copy(alpha = 0.12f),
                                        radius = maxRadius * (i / 3f),
                                        style = Stroke(width = 1f)
                                    )
                                }

                                // Crossing axes
                                drawLine(
                                    color = Color(0xFFFF1744).copy(alpha = 0.08f),
                                    start = androidx.compose.ui.geometry.Offset(0f, centerY),
                                    end = androidx.compose.ui.geometry.Offset(size.width, centerY)
                                )
                                drawLine(
                                    color = Color(0xFFFF1744).copy(alpha = 0.08f),
                                    start = androidx.compose.ui.geometry.Offset(centerX, 0f),
                                    end = androidx.compose.ui.geometry.Offset(centerX, size.height)
                                )

                                // Draw sweeping radar scanning line
                                val angleRad = Math.toRadians(radarSweepAngle.toDouble())
                                val sweepEndX = centerX + maxRadius * Math.cos(angleRad).toFloat()
                                val sweepEndY = centerY + Math.sin(angleRad).toFloat() * maxRadius
                                drawLine(
                                    color = Color(0xFFFF1744).copy(alpha = 0.4f),
                                    start = androidx.compose.ui.geometry.Offset(centerX, centerY),
                                    end = androidx.compose.ui.geometry.Offset(sweepEndX, sweepEndY),
                                    strokeWidth = 2.5f
                                )

                                // Draw neighboring camera nodes dynamically mapped
                                val userLat = 37.7749
                                val userLon = -122.4194

                                cctvFeeds.take(4).forEachIndexed { index, feed ->
                                    val dx = ((feed.longitude - userLon) * 16000f).toFloat()
                                    val dy = ((feed.latitude - userLat) * 16000f).toFloat()

                                    val blipX = (centerX + dx).coerceIn(8f, size.width - 8f)
                                    val blipY = (centerY - dy).coerceIn(8f, size.height - 8f)

                                    // Nearby nodes blip
                                    drawCircle(
                                        color = if (feed.isEnabled) Color(0xFF00E676).copy(alpha = 0.7f) else Color.Gray,
                                        radius = 3.5f,
                                        center = androidx.compose.ui.geometry.Offset(blipX, blipY)
                                    )
                                }

                                // Pulsating epicenter red warning aura & core epicenter beacon dot
                                drawCircle(
                                    color = Color(0xFFFF1744).copy(alpha = 0.15f),
                                    radius = epicenterPulseRadius,
                                    center = androidx.compose.ui.geometry.Offset(centerX, centerY)
                                )
                                drawCircle(
                                    color = Color(0xFFFF1744),
                                    radius = 5f,
                                    center = androidx.compose.ui.geometry.Offset(centerX, centerY)
                                )
                            }
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(5.dp)
                                    .background(Color(0xFF00E676), CircleShape)
                            )
                            Text(
                                text = "EPICENTER SIGNAL BROADCASTING",
                                fontSize = 7.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF00E676)
                            )
                        }
                    }
                }
            }
        }

        // Brand New Tiered Contact Dispatch Center
        val contactsList by viewModel.contacts.collectAsState()
        val activeSeverity by viewModel.activeSeverityLevel.collectAsState()

        Spacer(modifier = Modifier.height(12.dp))
        Card(
            modifier = Modifier.fillMaxWidth().testTag("tiered_dispatch_card"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E22)),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.12f))
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = Color(0xFFFF8A80),
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Tiered Alert Dispatches",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Box(
                        modifier = Modifier
                            .background(Color(0xFF00E676).copy(alpha = 0.15f), RoundedCornerShape(100.dp))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "TIERED AUTO-ROUTING",
                            color = Color(0xFF00E676),
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }

                Text(
                    text = "Situation severity filters target contacts automatically. Toggle severity to simulate the tiered routing system:",
                    fontSize = 11.sp,
                    color = Color.Gray
                )

                // Severity level selector button row
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    listOf(
                        "CRITICAL" to "🚨 Critical",
                        "HIGH" to "⚠️ High",
                        "MEDIUM" to "⚡ Med",
                        "LOW" to "📳 Low/Silent"
                    ).forEach { (sev, label) ->
                        val isSel = activeSeverity == sev
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(
                                    color = if (isSel) Color(0xFFFF8A80) else Color.White.copy(alpha = 0.05f),
                                    shape = RoundedCornerShape(6.dp)
                                )
                                .clickable { viewModel.updateActiveSeverity(sev) }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                fontSize = 9.sp,
                                color = if (isSel) Color.Black else Color.LightGray,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

                val activeContacts = viewModel.getAlertedContactsForSeverity(contactsList, activeSeverity)

                if (contactsList.isEmpty()) {
                    Text(
                        text = "No emergency contacts defined. Set them up in the Guardians tab.",
                        fontSize = 11.sp,
                        color = Color.Gray,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                    )
                } else {
                    contactsList.forEach { contact ->
                        val isTriggered = activeContacts.contains(contact)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    color = if (isTriggered) Color(0xFF00E676).copy(alpha = 0.04f) else Color.Transparent,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .padding(vertical = 6.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = contact.name,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isTriggered) Color.White else Color.Gray
                                    )
                                    Box(
                                        modifier = Modifier
                                            .background(
                                                color = if (isTriggered) Color(0xFF00E676).copy(alpha = 0.15f) else Color.White.copy(alpha = 0.08f),
                                                shape = RoundedCornerShape(4.dp)
                                            )
                                            .padding(horizontal = 4.dp, vertical = 1.dp)
                                    ) {
                                        Text(
                                            text = contact.relation.ifBlank { "Responder" },
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isTriggered) Color(0xFF00E676) else Color.Gray
                                        )
                                    }
                                }
                                Text(
                                    text = "Range: ${if (contact.proximityCategory == "IMMEDIATE") "Immediate neighbor (Next door)" else if (contact.proximityCategory == "REGIONAL") "Regional (<10 mi)" else "Remote (Out of Area)"} • Level: ${if (contact.alertTier == "TIER_1") "Tier 1: Critical Only" else if (contact.alertTier == "TIER_2") "Tier 2: Med/High" else "Tier 3: Low/Silent"}",
                                    fontSize = 9.sp,
                                    color = if (isTriggered) Color.LightGray else Color.DarkGray
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                if (isTriggered) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .background(Color(0xFF00E676), CircleShape)
                                    )
                                    Text(
                                        text = "DISPATCHED",
                                        color = Color(0xFF00E676),
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                } else {
                                    Text(
                                        text = "NOT ROUTED",
                                        color = Color.DarkGray,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Live Camera verification during Alert
        if (cctvFeeds.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            Card(
                modifier = Modifier.fillMaxWidth().testTag("active_alert_cctv_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E22)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.12f))
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.VideoCameraFront,
                                contentDescription = null,
                                tint = Color(0xFFFF8A80),
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Instant Verification Channels",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        Box(
                            modifier = Modifier
                                .background(Color(0xFFE81123).copy(alpha = 0.2f), RoundedCornerShape(100.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "TAP TO SCAN",
                                color = Color(0xFFFF8A80),
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Text(
                        text = "Verify live feeds of connected areas instantly to evaluate the situation.",
                        fontSize = 10.sp,
                        color = Color.Gray
                    )

                    HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        cctvFeeds.forEachIndexed { idx, feed ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        color = if (idx == 0) Color(0xFFFF8A80).copy(alpha = 0.12f) else Color.White.copy(alpha = 0.05f),
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = if (idx == 0) Color(0xFFFF8A80) else Color.Transparent,
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Videocam,
                                        contentDescription = null,
                                        tint = if (idx == 0) Color(0xFFFF8A80) else if (feed.isEnabled) Color(0xFF00E676) else Color.Gray,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = feed.name,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White,
                                                maxLines = 1
                                            )
                                            if (idx == 0) {
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Box(
                                                    modifier = Modifier
                                                        .background(Color(0xFFFF8A80), RoundedCornerShape(100.dp))
                                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                                ) {
                                                    Text(
                                                        text = "CLOSEST / ACTIVE",
                                                        fontSize = 7.sp,
                                                        fontWeight = FontWeight.Black,
                                                        color = Color.Black
                                                    )
                                                }
                                            }
                                        }
                                        Text(
                                            text = "📍 ${feed.location} • Proximity: ${String.format("%.1fm", viewModel.getFeedDistanceMeters(feed))}",
                                            fontSize = 9.sp,
                                            color = if (idx == 0) Color(0xFFFF8A80) else Color.Gray,
                                            maxLines = 1
                                        )
                                    }
                                }

                                Button(
                                    onClick = { onRequestLiveFeed(feed) },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFFFF8A80),
                                        contentColor = Color.Black
                                    ),
                                    elevation = null,
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier
                                        .height(28.dp)
                                        .testTag("alert_request_live_feed_${feed.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.LiveTv,
                                        contentDescription = "Instant Video",
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Request Live Feed",
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Dismiss action trigger
        Button(
            onClick = onDismiss,
            colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.12f)),
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .testTag("dismiss_panic_alarm"),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Cancel alarm status",
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "DISARM & CANCEL STATUS",
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
fun LiveFeedVerificationDialog(
    feed: CctvFeed,
    onDismiss: () -> Unit
) {
    var connectionSimulating by remember(feed) { mutableStateOf(true) }
    var connectionProgress by remember(feed) { mutableStateOf(0f) }

    LaunchedEffect(feed) {
        connectionSimulating = true
        connectionProgress = 0f
        val steps = 15
        for (i in 1..steps) {
            delay(100L)
            connectionProgress = i.toFloat() / steps
        }
        connectionSimulating = false
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .testTag("live_feed_verification_dialog"),
        shape = RoundedCornerShape(24.dp),
        containerColor = Color(0xFF1C1B1F),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.LiveTv,
                    contentDescription = null,
                    tint = Color(0xFFFF8A80)
                )
                Column {
                    Text(
                        text = "LIVE FEED VERIFICATION",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFFF8A80),
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = feed.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1.5f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.Black)
                        .border(1.dp, Color(0xFF49454F), RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (connectionSimulating) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            CircularProgressIndicator(
                                progress = { connectionProgress },
                                color = Color(0xFFFF8A80),
                                strokeWidth = 5.dp,
                                modifier = Modifier.size(54.dp)
                            )
                            val statusText = when {
                                connectionProgress < 0.3f -> "Securing tunneling layer..."
                                connectionProgress < 0.6f -> "Decrypting RTSP packet stream..."
                                connectionProgress < 0.9f -> "Buffering H.264 video feed..."
                                else -> "Synchronized feed established!"
                            }
                            Text(
                                text = statusText,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.LightGray,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        Image(
                            painter = rememberAsyncImagePainter(
                                ImageRequest.Builder(LocalContext.current)
                                    .data(data = feed.streamUrl)
                                    .apply(block = fun ImageRequest.Builder.() {
                                        crossfade(true)
                                    }).build()
                            ),
                            contentDescription = "Live feed of ${feed.name}",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(12.dp)
                                .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                            val alpha by infiniteTransition.animateFloat(
                                initialValue = 0.2f,
                                targetValue = 1f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(800, easing = LinearEasing),
                                    repeatMode = RepeatMode.Reverse
                                ),
                                label = "dot"
                            )
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .scale(alpha)
                                    .background(Color.Red, CircleShape)
                            )
                            Text(
                                text = "LIVE",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(12.dp)
                                .background(Color(0xFF2E7D32), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "AES-256 SYNC",
                                color = Color.White,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.BottomCenter)
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                                    )
                                )
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "LOC: ${feed.location.uppercase()}",
                                    color = Color.LightGray,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "BANDWIDTH: 2.4 MB/S // 30 FPS",
                                    color = Color.LightGray,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Wifi,
                                    contentDescription = "Signal Strength",
                                    tint = Color(0xFF00E676),
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = "EXCELLENT",
                                    color = Color(0xFF00E676),
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF2D2C30)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = Color.LightGray,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "Emergency Verification Guideline",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = "Live feeds provide real-time environment telemetry to back up your case. This feed is encrypted and safely recorded in your cloud event logs to share with local authorities and guardians.",
                            fontSize = 10.sp,
                            color = Color.LightGray,
                            lineHeight = 14.sp
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF8A80)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .testTag("dismiss_live_verification_button")
            ) {
                Text(
                    text = "DISCONNECT FEED",
                    color = Color.Black,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
        }
    )
}
