package com.ovehbe.junkboy.ui.compose

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.ovehbe.junkboy.ui.compose.screens.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JunkboyApp(
    onRequestPermissions: () -> Unit,
    permissionRefreshTrigger: Int = 0
) {
    val navController = rememberNavController()
    val context = LocalContext.current

    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination

                val items = listOf(
                    BottomNavItem("dashboard", "Dashboard", Icons.Default.Home),
                    BottomNavItem("messages", "Messages", Icons.Default.Message),
                    BottomNavItem("test", "Test", Icons.Default.FilterList),
                    BottomNavItem("settings", "Settings", Icons.Default.Settings),
                    BottomNavItem("stats", "Stats", Icons.Default.Assessment)
                )

                items.forEach { item ->
                    NavigationBarItem(
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        label = { Text(item.label) },
                        selected = currentDestination?.hierarchy?.any { it.route == item.route } == true,
                        onClick = {
                            navController.navigate(item.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "dashboard",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("dashboard") {
                DashboardScreen(
                    onRequestPermissions = onRequestPermissions,
                    onNavigateToMessages = { navController.navigate("messages") },
                    onNavigateToSettings = { navController.navigate("settings") },
                    refreshTrigger = permissionRefreshTrigger
                )
            }
            composable("messages") {
                MessagesScreen()
            }
            composable("test") {
                TestFilterScreen()
            }
            composable("settings") {
                SettingsScreen()
            }
            composable("stats") {
                StatsScreen()
            }
        }
    }
}

data class BottomNavItem(
    val route: String,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
) 