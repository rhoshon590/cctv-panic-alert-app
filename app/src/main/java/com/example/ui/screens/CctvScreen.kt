package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.example.data.CctvFeed
import com.example.ui.MainViewModel

@Composable
fun CctvScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val feedsList by viewModel.cctvFeeds.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    // Forms
    var feedName by remember { mutableStateOf("") }
    var feedLocation by remember { mutableStateOf("") }
    var feedUrl by remember { mutableStateOf("") }
    var feedEnabled by remember { mutableStateOf(true) }
    var feedLatitude by remember { mutableStateOf("") }
    var feedLongitude by remember { mutableStateOf("") }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Stats & Instructions
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "INTEGRATED CCTV FEEDS",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFF6750A4),
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Link your smart home dashboard, RTSP streams, IP Cameras, or video doorbells. When an alert event triggers, feeds from the checked active channels will automatically record live video frames and stream clips directly to cloud security.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF49454F)
                    )
                }
            }

            if (feedsList.isEmpty()) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.VideocamOff,
                        contentDescription = "Empty surveillance feeds",
                        tint = Color(0xFF49454F),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No CCTV camera links connected yet",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF1D192B),
                        fontWeight = FontWeight.Bold,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Tap the floating button to attach a custom IP stream or doorbell camera feed",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF49454F),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(feedsList) { feed ->
                        CctvFeedCardItem(
                            feed = feed,
                            onToggleEnabled = { viewModel.updateCctvFeed(feed.copy(isEnabled = !feed.isEnabled)) },
                            onDelete = { viewModel.deleteCctvFeed(feed) }
                        )
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = {
                feedName = ""
                feedLocation = ""
                feedUrl = ""
                feedEnabled = true
                feedLatitude = ""
                feedLongitude = ""
                showAddDialog = true
            },
            containerColor = Color(0xFF6750A4),
            contentColor = Color.White,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
                .testTag("add_cctv_feed_fab"),
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(Icons.Default.Videocam, contentDescription = "Add safety CCTV feed")
        }

        if (showAddDialog) {
            AlertDialog(
                onDismissRequest = { showAddDialog = false },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AddHomeWork,
                            contentDescription = "Attach security video",
                            tint = Color(0xFF6750A4),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Link Safety CCTV Feed", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1D192B))
                    }
                },
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = feedName,
                            onValueChange = { feedName = it },
                            label = { Text("Camera Name") },
                            placeholder = { Text("e.g. Front Doorbell, Hallway dome") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF6750A4),
                                focusedLabelColor = Color(0xFF6750A4)
                            ),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("cctv_name_input")
                        )

                        OutlinedTextField(
                            value = feedLocation,
                            onValueChange = { feedLocation = it },
                            label = { Text("Camera Location / Area") },
                            placeholder = { Text("e.g. Backyard fence, main lobby") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF6750A4),
                                focusedLabelColor = Color(0xFF6750A4)
                            ),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("cctv_location_input")
                        )

                        OutlinedTextField(
                            value = feedUrl,
                            onValueChange = { feedUrl = it },
                            label = { Text("RTSP / Stream Web Endpoint (Optional)") },
                            placeholder = { Text("e.g. rtsps://192.168.1.1:554/live") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF6750A4),
                                focusedLabelColor = Color(0xFF6750A4)
                            ),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("cctv_url_input")
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = feedLatitude,
                                onValueChange = { feedLatitude = it },
                                label = { Text("Latitude (Optional)") },
                                placeholder = { Text("e.g. 37.7749") },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF6750A4),
                                    focusedLabelColor = Color(0xFF6750A4)
                                ),
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("cctv_lat_input")
                            )

                            OutlinedTextField(
                                value = feedLongitude,
                                onValueChange = { feedLongitude = it },
                                label = { Text("Longitude (Optional)") },
                                placeholder = { Text("e.g. -122.4194") },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF6750A4),
                                    focusedLabelColor = Color(0xFF6750A4)
                                ),
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("cctv_lon_input")
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (feedName.isNotBlank() && feedLocation.isNotBlank()) {
                                viewModel.addCctvFeed(
                                    name = feedName,
                                    location = feedLocation,
                                    streamUrl = feedUrl,
                                    isEnabled = feedEnabled,
                                    latitude = feedLatitude.toDoubleOrNull(),
                                    longitude = feedLongitude.toDoubleOrNull()
                                )
                                showAddDialog = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6750A4)),
                        modifier = Modifier.testTag("save_cctv_feed_button")
                    ) {
                        Text("Link Camera", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showAddDialog = false }
                    ) {
                        Text("Cancel", color = Color(0xFF6750A4))
                    }
                },
                containerColor = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(24.dp)
            )
        }
    }
}

@Composable
fun CctvFeedCardItem(
    feed: CctvFeed,
    onToggleEnabled: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column {
            // CCTV Live Video Simulator Thumbnail
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .background(Color.Black)
            ) {
                // Async load image
                val painter = rememberAsyncImagePainter(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(feed.streamUrl)
                        .crossfade(true)
                        .build()
                )
                
                Image(
                    painter = painter,
                    contentDescription = "CCTV camera stream view thumbnail",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Holographic dark overlay gradient
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))
                            )
                        )
                )

                // Top stream tags overlay
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .background(
                                color = if (feed.isEnabled) Color(0xFFE2F0D9) else Color(0xFFECEFF1),
                                shape = RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(
                                        color = if (feed.isEnabled) Color(0xFF2E7D32) else Color(0xFF546E7A),
                                        shape = CircleShape
                                    )
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (feed.isEnabled) "SECURE SYNC: ON" else "STANDBY",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (feed.isEnabled) Color(0xFF2E7D32) else Color(0xFF546E7A)
                            )
                        }
                    }

                    Text(
                        text = "SECURE LINK IPS-AES-256",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.5f)
                    )
                }

                // Bottom tags overlay
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomStart)
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = feed.name,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            fontSize = 14.sp
                        )
                        Text(
                            text = feed.location.uppercase(),
                            fontSize = 10.sp,
                            color = Color.LightGray.copy(alpha = 0.7f),
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Wifi,
                            contentDescription = "Strong Signal Strength Indicator",
                            tint = Color(0xFF2E7D32),
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "2.4 MB/s • 30 FPS",
                            fontSize = 10.sp,
                            color = Color.LightGray,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // Lower card actions panel
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.SettingsVoice,
                        contentDescription = null,
                        tint = Color(0xFF49454F),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Mic: ",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF49454F)
                    )
                    Text(
                        text = if (feed.isEnabled) "ARMED" else "STANDBY",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = if (feed.isEnabled) Color(0xFF2E7D32) else Color(0xFF546E7A)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Armed",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF49454F),
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    
                    Switch(
                        checked = feed.isEnabled,
                        onCheckedChange = { onToggleEnabled() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color(0xFF6750A4),
                            checkedTrackColor = Color(0xFFEADDFF),
                            uncheckedThumbColor = Color(0xFF79747E),
                            uncheckedTrackColor = Color(0xFFE7E0EC)
                        ),
                        modifier = Modifier
                            .scale(0.85f)
                            .testTag("cctv_toggle_switch_${feed.id}")
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(24.dp).testTag("delete_cctv_button_${feed.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Remove camera feed",
                            tint = Color(0xFF8C1D18),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}
