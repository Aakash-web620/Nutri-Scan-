package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Kitchen
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Kitchen
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.FridgeRecipesScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.ScannerScreen
import com.example.ui.theme.Emerald100
import com.example.ui.theme.Emerald600
import com.example.ui.theme.Emerald700
import com.example.ui.theme.NutriVisionTheme
import com.example.ui.viewmodel.AppTab
import com.example.ui.viewmodel.NutriViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: NutriViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NutriVisionTheme {
                NutriVisionApp(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun NutriVisionApp(viewModel: NutriViewModel) {
    val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()

    // Handle back button to return to dashboard
    BackHandler(enabled = currentTab != AppTab.DASHBOARD) {
        viewModel.navigateTo(AppTab.DASHBOARD)
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NutriBottomNavigationBar(
                currentTab = currentTab,
                onTabSelected = { viewModel.navigateTo(it) }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Crossfade(targetState = currentTab, label = "tab_transition") { tab ->
                when (tab) {
                    AppTab.DASHBOARD -> DashboardScreen(viewModel = viewModel)
                    AppTab.SCANNER -> ScannerScreen(viewModel = viewModel)
                    AppTab.FRIDGE -> FridgeRecipesScreen(viewModel = viewModel)
                    AppTab.PROFILE -> ProfileScreen(viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
fun NutriBottomNavigationBar(
    currentTab: AppTab,
    onTabSelected: (AppTab) -> Unit
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp,
        modifier = Modifier.testTag("nutri_bottom_nav_bar")
    ) {
        NavigationBarItem(
            selected = currentTab == AppTab.DASHBOARD,
            onClick = { onTabSelected(AppTab.DASHBOARD) },
            icon = {
                Icon(
                    imageVector = if (currentTab == AppTab.DASHBOARD) Icons.Default.Dashboard else Icons.Outlined.Dashboard,
                    contentDescription = "Dashboard"
                )
            },
            label = {
                Text(
                    text = "Dashboard",
                    fontWeight = if (currentTab == AppTab.DASHBOARD) FontWeight.Bold else FontWeight.Normal
                )
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Emerald700,
                selectedTextColor = Emerald700,
                indicatorColor = Emerald100
            ),
            modifier = Modifier.testTag("nav_tab_dashboard")
        )

        NavigationBarItem(
            selected = currentTab == AppTab.SCANNER,
            onClick = { onTabSelected(AppTab.SCANNER) },
            icon = {
                Icon(
                    imageVector = if (currentTab == AppTab.SCANNER) Icons.Default.CameraAlt else Icons.Outlined.CameraAlt,
                    contentDescription = "Scanner"
                )
            },
            label = {
                Text(
                    text = "Scanner",
                    fontWeight = if (currentTab == AppTab.SCANNER) FontWeight.Bold else FontWeight.Normal
                )
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Emerald700,
                selectedTextColor = Emerald700,
                indicatorColor = Emerald100
            ),
            modifier = Modifier.testTag("nav_tab_scanner")
        )

        NavigationBarItem(
            selected = currentTab == AppTab.FRIDGE,
            onClick = { onTabSelected(AppTab.FRIDGE) },
            icon = {
                Icon(
                    imageVector = if (currentTab == AppTab.FRIDGE) Icons.Default.Kitchen else Icons.Outlined.Kitchen,
                    contentDescription = "Fridge AI"
                )
            },
            label = {
                Text(
                    text = "Fridge AI",
                    fontWeight = if (currentTab == AppTab.FRIDGE) FontWeight.Bold else FontWeight.Normal
                )
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Emerald700,
                selectedTextColor = Emerald700,
                indicatorColor = Emerald100
            ),
            modifier = Modifier.testTag("nav_tab_fridge")
        )

        NavigationBarItem(
            selected = currentTab == AppTab.PROFILE,
            onClick = { onTabSelected(AppTab.PROFILE) },
            icon = {
                Icon(
                    imageVector = if (currentTab == AppTab.PROFILE) Icons.Default.Person else Icons.Outlined.Person,
                    contentDescription = "Profile"
                )
            },
            label = {
                Text(
                    text = "Profile",
                    fontWeight = if (currentTab == AppTab.PROFILE) FontWeight.Bold else FontWeight.Normal
                )
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Emerald700,
                selectedTextColor = Emerald700,
                indicatorColor = Emerald100
            ),
            modifier = Modifier.testTag("nav_tab_profile")
        )
    }
}
