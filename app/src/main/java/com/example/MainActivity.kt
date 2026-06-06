package com.example

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.PeopleOutline
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.MainViewModel
import com.example.ui.screens.CctvScreen
import com.example.ui.screens.ContactsScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.HighDensityOutline
import androidx.compose.ui.draw.drawBehind

class MainActivity : ComponentActivity() {

    // Dynamic Permission Requester Launcher
    private val requestPermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val recordGranted = permissions[android.Manifest.permission.RECORD_AUDIO] ?: false
        val locGranted = permissions[android.Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        if (recordGranted && locGranted) {
            Toast.makeText(this, "PanicLink Secure Shield Active. Critical permissions granted.", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Permissions partial. Hardware SOS features might require mic & location access.", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Proactively request safety audio & location sensors permissions
        requestPermissionsLauncher.launch(
            arrayOf(
                android.Manifest.permission.RECORD_AUDIO,
                android.Manifest.permission.ACCESS_FINE_LOCATION,
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )

        setContent {
            MyApplicationTheme {
                val viewModel: MainViewModel = viewModel()
                var currentTab by remember { mutableStateOf(0) }

                Scaffold(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("main_scaffold"),
                    bottomBar = {
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            tonalElevation = 0.dp,
                            modifier = Modifier
                                .windowInsetsPadding(WindowInsets.navigationBars)
                                .drawBehind {
                                    // Add a thin top border matching high density outline
                                    val strokeWidth = 1.dp.toPx()
                                    drawLine(
                                        color = HighDensityOutline,
                                        start = androidx.compose.ui.geometry.Offset(0f, 0f),
                                        end = androidx.compose.ui.geometry.Offset(size.width, 0f),
                                        strokeWidth = strokeWidth
                                    )
                                }
                        ) {
                            NavigationBarItem(
                                selected = currentTab == 0,
                                onClick = { currentTab = 0 },
                                icon = {
                                    Icon(
                                        imageVector = if (currentTab == 0) Icons.Filled.Shield else Icons.Outlined.Shield,
                                        contentDescription = "Shield SOS active hub"
                                    )
                                },
                                label = { Text("Status", style = MaterialTheme.typography.labelSmall) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = Color(0xFF1D192B),
                                    unselectedIconColor = Color(0xFF49454F),
                                    selectedTextColor = Color(0xFF1D192B),
                                    unselectedTextColor = Color(0xFF49454F),
                                    indicatorColor = Color(0xFFE8DEF8)
                                ),
                                modifier = Modifier.testTag("tab_shield")
                            )

                            NavigationBarItem(
                                selected = currentTab == 1,
                                onClick = { currentTab = 1 },
                                icon = {
                                    Icon(
                                        imageVector = if (currentTab == 1) Icons.Filled.People else Icons.Outlined.PeopleOutline,
                                        contentDescription = "Contacts roster view"
                                    )
                                },
                                label = { Text("Guardians", style = MaterialTheme.typography.labelSmall) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = Color(0xFF1D192B),
                                    unselectedIconColor = Color(0xFF49454F),
                                    selectedTextColor = Color(0xFF1D192B),
                                    unselectedTextColor = Color(0xFF49454F),
                                    indicatorColor = Color(0xFFE8DEF8)
                                ),
                                modifier = Modifier.testTag("tab_contacts")
                            )

                            NavigationBarItem(
                                selected = currentTab == 2,
                                onClick = { currentTab = 2 },
                                icon = {
                                    Icon(
                                        imageVector = if (currentTab == 2) Icons.Filled.Videocam else Icons.Outlined.Videocam,
                                        contentDescription = "Surveillance cameras setup"
                                    )
                                },
                                label = { Text("Config", style = MaterialTheme.typography.labelSmall) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = Color(0xFF1D192B),
                                    unselectedIconColor = Color(0xFF49454F),
                                    selectedTextColor = Color(0xFF1D192B),
                                    unselectedTextColor = Color(0xFF49454F),
                                    indicatorColor = Color(0xFFE8DEF8)
                                ),
                                modifier = Modifier.testTag("tab_cctv")
                            )

                            NavigationBarItem(
                                selected = currentTab == 3,
                                onClick = { currentTab = 3 },
                                icon = {
                                    Icon(
                                        imageVector = if (currentTab == 3) Icons.Filled.History else Icons.Outlined.History,
                                        contentDescription = "Safety history trail"
                                    )
                                },
                                label = { Text("Vault Logs", style = MaterialTheme.typography.labelSmall) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = Color(0xFF1D192B),
                                    unselectedIconColor = Color(0xFF49454F),
                                    selectedTextColor = Color(0xFF1D192B),
                                    unselectedTextColor = Color(0xFF49454F),
                                    indicatorColor = Color(0xFFE8DEF8)
                                ),
                                modifier = Modifier.testTag("tab_history")
                            )
                        }
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        when (currentTab) {
                            0 -> DashboardScreen(viewModel = viewModel)
                            1 -> ContactsScreen(viewModel = viewModel)
                            2 -> CctvScreen(viewModel = viewModel)
                            3 -> HistoryScreen(viewModel = viewModel)
                        }
                    }
                }
            }
        }
    }
}
