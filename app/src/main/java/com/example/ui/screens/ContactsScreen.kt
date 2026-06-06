package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Contact
import com.example.ui.MainViewModel

@Composable
fun ContactsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val contactsList by viewModel.contacts.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    // Dialog form state
    var contactName by remember { mutableStateOf("") }
    var contactPhone by remember { mutableStateOf("") }
    var contactEmail by remember { mutableStateOf("") }
    var contactRelation by remember { mutableStateOf("") }
    var silentRecipient by remember { mutableStateOf(false) }
    var proximitySelection by remember { mutableStateOf("IMMEDIATE") }
    var relationshipSelection by remember { mutableStateOf("FAMILY") }
    var tierSelection by remember { mutableStateOf("TIER_1") }

    // List Grouping mode: "ALL" | "PROXIMITY" | "RELATIONSHIP" | "TIER"
    var groupMode by remember { mutableStateOf("ALL") }

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
            // Header Description Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "TRUSTED EMERGENCY CONTACTS",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFF6750A4),
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Define trusted responders and assign customizable alert tiers based on status/proximity. Emergency situations automatically trigger tiered alert actions.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF49454F)
                    )
                }
            }

            // Custom Layout Grouping selector bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf(
                    "ALL" to "All",
                    "PROXIMITY" to "By Proximity",
                    "RELATIONSHIP" to "By Relation",
                    "TIER" to "By Alert Tier"
                ).forEach { (mode, title) ->
                    Box(
                        modifier = Modifier
                            .background(
                                color = if (groupMode == mode) Color(0xFF6750A4) else Color(0xFFEADDFF).copy(alpha = 0.5f),
                                shape = RoundedCornerShape(100.dp)
                            )
                            .clickable { groupMode = mode }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                            .testTag("group_chip_$mode"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = title,
                            color = if (groupMode == mode) Color.White else Color(0xFF21005D),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            if (contactsList.isEmpty()) {
                // Empty state
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.PeopleOutline,
                        contentDescription = "Empty contacts list",
                        tint = Color(0xFF49454F),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No emergency contacts defined yet",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF1D192B),
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Tap the floating button below to link a trusted responder",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF49454F),
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    when (groupMode) {
                        "PROXIMITY" -> {
                            val immediate = contactsList.filter { it.proximityCategory == "IMMEDIATE" }
                            val regional = contactsList.filter { it.proximityCategory == "REGIONAL" }
                            val remote = contactsList.filter { it.proximityCategory == "REMOTE" }

                            if (immediate.isNotEmpty()) {
                                item { GroupHeader("📍 Immediate Proximity (Next-Door/Within 50m)", immediate.size) }
                                items(immediate) { contact ->
                                    ContactItemCard(contact = contact, onDelete = { viewModel.deleteContact(contact) })
                                }
                            }
                            if (regional.isNotEmpty()) {
                                item { GroupHeader("🏙️ Regional Proximity (City Limits / <10 mi)", regional.size) }
                                items(regional) { contact ->
                                    ContactItemCard(contact = contact, onDelete = { viewModel.deleteContact(contact) })
                                }
                            }
                            if (remote.isNotEmpty()) {
                                item { GroupHeader("🌐 Remote Proximity (Out-of-Area / Digital Contacts)", remote.size) }
                                items(remote) { contact ->
                                    ContactItemCard(contact = contact, onDelete = { viewModel.deleteContact(contact) })
                                }
                            }
                        }
                        "RELATIONSHIP" -> {
                            val family = contactsList.filter { it.relationshipCategory == "FAMILY" }
                            val friend = contactsList.filter { it.relationshipCategory == "FRIEND" }
                            val neighbor = contactsList.filter { it.relationshipCategory == "NEIGHBOR" }
                            val service = contactsList.filter { it.relationshipCategory == "SERVICE" }

                            if (family.isNotEmpty()) {
                                item { GroupHeader("🏠 Family Members & Guardians", family.size) }
                                items(family) { contact ->
                                    ContactItemCard(contact = contact, onDelete = { viewModel.deleteContact(contact) })
                                }
                            }
                            if (friend.isNotEmpty()) {
                                item { GroupHeader("🤝 Friends & Trusted Companions", friend.size) }
                                items(friend) { contact ->
                                    ContactItemCard(contact = contact, onDelete = { viewModel.deleteContact(contact) })
                                }
                            }
                            if (neighbor.isNotEmpty()) {
                                item { GroupHeader("🚪 Neighbors & Local Responders", neighbor.size) }
                                items(neighbor) { contact ->
                                    ContactItemCard(contact = contact, onDelete = { viewModel.deleteContact(contact) })
                                }
                            }
                            if (service.isNotEmpty()) {
                                item { GroupHeader("🛡️ Public Authorities & Dispatch Services", service.size) }
                                items(service) { contact ->
                                    ContactItemCard(contact = contact, onDelete = { viewModel.deleteContact(contact) })
                                }
                            }
                        }
                        "TIER" -> {
                            val t1 = contactsList.filter { it.alertTier == "TIER_1" }
                            val t2 = contactsList.filter { it.alertTier == "TIER_2" }
                            val t3 = contactsList.filter { it.alertTier == "TIER_3" }

                            if (t1.isNotEmpty()) {
                                item { GroupHeader("🚨 Tier 1: Critical Situation Alerts", t1.size, "Dispatched immediately during sirens and active loud panic events") }
                                items(t1) { contact ->
                                    ContactItemCard(contact = contact, onDelete = { viewModel.deleteContact(contact) })
                                }
                            }
                            if (t2.isNotEmpty()) {
                                item { GroupHeader("⚠️ Tier 2: Medium Threat Escalations", t2.size, "Contact receives warning updates during situational threats") }
                                items(t2) { contact ->
                                    ContactItemCard(contact = contact, onDelete = { viewModel.deleteContact(contact) })
                                }
                            }
                            if (t3.isNotEmpty()) {
                                item { GroupHeader("📳 Tier 3: Low-Severity / Silent Coordinate Pings", t3.size, "Silent status and coordinate track updates only") }
                                items(t3) { contact ->
                                    ContactItemCard(contact = contact, onDelete = { viewModel.deleteContact(contact) })
                                }
                            }
                        }
                        else -> {
                            items(contactsList) { contact ->
                                ContactItemCard(contact = contact, onDelete = { viewModel.deleteContact(contact) })
                            }
                        }
                    }
                }
            }
        }

        // Floating Action Button to Add Contacts
        FloatingActionButton(
            onClick = {
                contactName = ""
                contactPhone = ""
                contactEmail = ""
                contactRelation = ""
                silentRecipient = false
                proximitySelection = "IMMEDIATE"
                relationshipSelection = "FAMILY"
                tierSelection = "TIER_1"
                showAddDialog = true
            },
            containerColor = Color(0xFF6750A4),
            contentColor = Color.White,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
                .testTag("add_contact_fab"),
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add trusted responder")
        }

        // Add Contact Dialog Modal
        if (showAddDialog) {
            AlertDialog(
                onDismissRequest = { showAddDialog = false },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.PersonAdd,
                            contentDescription = "Add icon",
                            tint = Color(0xFF6750A4),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Add Guardian Contact", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1D192B))
                    }
                },
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = contactName,
                            onValueChange = { contactName = it },
                            label = { Text("Responder Full Name") },
                            placeholder = { Text("e.g. Sister Sarah") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF6750A4),
                                focusedLabelColor = Color(0xFF6750A4)
                            ),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("contact_name_input")
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = contactRelation,
                                onValueChange = { contactRelation = it },
                                label = { Text("Rel / Role") },
                                placeholder = { Text("Spouse") },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF6750A4),
                                    focusedLabelColor = Color(0xFF6750A4)
                                ),
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("contact_relation_input")
                            )

                            OutlinedTextField(
                                value = contactPhone,
                                onValueChange = { contactPhone = it },
                                label = { Text("Phone") },
                                placeholder = { Text("+1...") },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF6750A4),
                                    focusedLabelColor = Color(0xFF6750A4)
                                ),
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1.2f)
                                    .testTag("contact_phone_input")
                            )
                        }

                        OutlinedTextField(
                            value = contactEmail,
                            onValueChange = { contactEmail = it },
                            label = { Text("Email (Secure live stream share)") },
                            placeholder = { Text("e.g. sister@example.com") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF6750A4),
                                focusedLabelColor = Color(0xFF6750A4)
                            ),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("contact_email_input")
                        )

                        // 3 categories styling selection using custom aesthetic selectors
                        Text("Proximity Range", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color(0xFF6750A4))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            listOf("IMMEDIATE" to "Next Door", "REGIONAL" to "City", "REMOTE" to "Remote").forEach { (value, label) ->
                                val isChosen = proximitySelection == value
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .background(
                                            color = if (isChosen) Color(0xFF6750A4) else Color(0xFFF4EFF4),
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .clickable { proximitySelection = value }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 11.sp,
                                        color = if (isChosen) Color.White else Color(0xFF49454F),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Text("Relationship Group", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color(0xFF6750A4))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            listOf("FAMILY" to "Family", "FRIEND" to "Friend", "NEIGHBOR" to "Neighbor", "SERVICE" to "Authority").forEach { (value, label) ->
                                val isChosen = relationshipSelection == value
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .background(
                                            color = if (isChosen) Color(0xFF6750A4) else Color(0xFFF4EFF4),
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .clickable { relationshipSelection = value }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 10.sp,
                                        color = if (isChosen) Color.White else Color(0xFF49454F),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Text("Situation Alert Dispatch level", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color(0xFF6750A4))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            listOf("TIER_1" to "Tier 1 (Critical)", "TIER_2" to "Tier 2 (Med)", "TIER_3" to "Tier 3 (Silent)").forEach { (value, label) ->
                                val isChosen = tierSelection == value
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .background(
                                            color = if (isChosen) Color(0xFF6750A4) else Color(0xFFF4EFF4),
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .clickable { tierSelection = value }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 10.sp,
                                        color = if (isChosen) Color.White else Color(0xFF49454F),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "Include in Silent Alerts",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF49454F)
                            )
                            Checkbox(
                                checked = silentRecipient,
                                onCheckedChange = { silentRecipient = it },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = Color(0xFF6750A4)
                                ),
                                modifier = Modifier.testTag("silent_recipient_checkbox")
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (contactName.isNotBlank() && contactPhone.isNotBlank()) {
                                viewModel.addContact(
                                    name = contactName,
                                    phone = contactPhone,
                                    email = contactEmail,
                                    relation = contactRelation,
                                    isSilentRecipient = silentRecipient,
                                    proximityCategory = proximitySelection,
                                    relationshipCategory = relationshipSelection,
                                    alertTier = tierSelection
                                )
                                showAddDialog = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6750A4)),
                        modifier = Modifier.testTag("save_contact_button")
                    ) {
                        Text("Add Contact", fontWeight = FontWeight.Bold, color = Color.White)
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
fun GroupHeader(title: String, count: Int, subtitle: String? = null) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp, bottom = 4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = title,
                fontSize = 13.sp,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF6750A4)
            )
            Box(
                modifier = Modifier
                    .background(Color(0xFFEADDFF), RoundedCornerShape(100.dp))
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "$count",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF21005D)
                )
            }
        }
        if (subtitle != null) {
            Text(
                text = subtitle,
                fontSize = 10.sp,
                color = Color(0xFF49454F),
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

@Composable
fun ContactItemCard(
    contact: Contact,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Circular identity badge
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .background(Color(0xFFEADDFF), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = contact.name.take(2).uppercase(),
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF21005D),
                        fontSize = 16.sp
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = contact.name,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 15.sp
                        )
                        Box(
                            modifier = Modifier
                                .background(Color(0xFFE8DEF8), RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = contact.relation.ifBlank { "Responder" },
                                color = Color(0xFF21005D),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Default.Phone, contentDescription = null, tint = Color(0xFF49454F), modifier = Modifier.size(12.dp))
                            Text(text = contact.phone, color = Color(0xFF49454F), fontSize = 12.sp)
                        }

                        if (contact.email.isNotBlank()) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(Icons.Default.Email, contentDescription = null, tint = Color(0xFF49454F), modifier = Modifier.size(12.dp))
                                Text(text = contact.email, color = Color(0xFF49454F), fontSize = 12.sp, maxLines = 1)
                            }
                        }
                    }

                    // Multi-Dimensional Category Pills
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Proximity category pill
                        val (proxColor, proxBg, proxLabel) = when (contact.proximityCategory) {
                            "IMMEDIATE" -> Triple(Color(0xFF2E7D32), Color(0xFFE8F5E9), "📍 Local Responder")
                            "REGIONAL" -> Triple(Color(0xFF1565C0), Color(0xFFE3F2FD), "🏙️ Regional")
                            else -> Triple(Color(0xFF7B1FA2), Color(0xFFF3E5F5), "🌐 Remote")
                        }
                        Box(
                            modifier = Modifier
                                .background(proxBg, RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(text = proxLabel, color = proxColor, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }

                        // Tier Category pill
                        val (tierColor, tierBg, tierLabel) = when (contact.alertTier) {
                            "TIER_1" -> Triple(Color(0xFFC62828), Color(0xFFFFEBEE), "Tier 1: Critical Only")
                            "TIER_2" -> Triple(Color(0xFFEF6C00), Color(0xFFFFF3E0), "Tier 2: Med Level")
                            else -> Triple(Color(0xFF455A64), Color(0xFFECEFF1), "Tier 3: Silent Tracking")
                        }
                        Box(
                            modifier = Modifier
                                .background(tierBg, RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(text = tierLabel, color = tierColor, fontSize = 9.sp, fontWeight = FontWeight.Black)
                        }
                    }
                }
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier.testTag("delete_contact_button_${contact.id}")
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = "Remove contact",
                    tint = Color(0xFF8C1D18)
                )
            }
        }
    }
}
