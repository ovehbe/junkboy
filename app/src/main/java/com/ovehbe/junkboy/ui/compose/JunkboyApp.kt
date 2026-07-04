package com.ovehbe.junkboy.ui.compose

import android.util.Log
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.ovehbe.junkboy.R
import com.ovehbe.junkboy.ui.compose.screens.*
import com.ovehbe.junkboy.ui.theme.DesignColors
import com.ovehbe.junkboy.utils.PreferencesManager
import com.ovehbe.junkboy.utils.SmsAppManager

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun JunkboyApp(
    onRequestPermissions: () -> Unit,
    permissionRefreshTrigger: Int = 0,
    checkPermissions: () -> Boolean = { true }
) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val density = LocalDensity.current
    val smsAppManager = remember { SmsAppManager(context) }
    val preferencesManager = remember { PreferencesManager(context) }

    // Check if Hub is enabled (disabled by default)
    var isHubEnabled by remember { mutableStateOf(preferencesManager.isHubEnabled()) }
    var isDefaultSmsApp by remember {
        mutableStateOf(smsAppManager.isJunkboyDefaultSmsAppFast())
    }

    // Pending SMS address for internal navigation (avoids infinite loop from sms: Intent)
    var pendingSmsAddress by remember { mutableStateOf<String?>(null) }

    // Track keyboard visibility using snapshotFlow to avoid per-frame recomposition
    val imeInsets = WindowInsets.ime
    var isKeyboardVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        snapshotFlow { imeInsets.getBottom(density) }
            .collect { bottom ->
                isKeyboardVisible = bottom > 0
            }
    }

    // Force reread keyboard offset when keyboard visibility changes
    // This ensures we always have the latest value from preferences
    var keyboardOffsetDp by remember { mutableStateOf(0.dp) }

    LaunchedEffect(isKeyboardVisible) {
        val offset = preferencesManager.getKeyboardOffset()
        keyboardOffsetDp = if (isKeyboardVisible && offset > 0) offset.dp else 0.dp
        Log.d("JunkboyApp", "Keyboard visible: $isKeyboardVisible, offset: $offset dp")
    }

    // Refresh hub enabled state when returning to app
    LaunchedEffect(permissionRefreshTrigger) {
        isHubEnabled = preferencesManager.isHubEnabled()
        isDefaultSmsApp = smsAppManager.isJunkboyDefaultSmsAppFast()
    }

    // Determine start destination
    val startDestination = when {
        isHubEnabled -> "hub"
        isDefaultSmsApp -> "sms"
        else -> "messages"
    }

    // Main column that fills the entire screen
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Main content takes all available space minus the keyboard offset spacer
        Box(
            modifier = Modifier.weight(1f)
        ) {
            Scaffold(
                bottomBar = {
                    NavigationBar(
                        containerColor = DesignColors.NavigationBackground,
                        contentColor = DesignColors.NavigationActive,
                        tonalElevation = 0.dp,
                        modifier = Modifier
                            .navigationBarsPadding()
                            .height(72.dp)
                    ) {
                        val navBackStackEntry by navController.currentBackStackEntryAsState()
                        val currentDestination = navBackStackEntry?.destination

                        // Navigation items: (Hub if enabled), (SMS if default), Filtered, Menu
                        val items = buildList {
                            if (isHubEnabled) {
                                add(BottomNavItem("hub", stringResource(R.string.nav_hub), Icons.Default.Inbox))
                            }
                            if (isDefaultSmsApp) {
                                add(BottomNavItem("sms", stringResource(R.string.nav_sms), Icons.Default.Message))
                            }
                            add(BottomNavItem("messages", stringResource(R.string.nav_filtered), Icons.Default.FilterList))
                            add(BottomNavItem("menu", stringResource(R.string.nav_menu), Icons.Default.Menu))
                        }

                        items.forEach { item ->
                            val isSelected = currentDestination?.hierarchy?.any { it.route == item.route } == true
                            
                            NavigationBarItem(
                                icon = { 
                                    // Custom smaller indicator behind the icon only
                                    Box(
                                        modifier = Modifier
                                            .size(width = 48.dp, height = 28.dp)
                                            .background(
                                                color = if (isSelected) DesignColors.ActiveBackground else Color.Transparent,
                                                shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp)
                                            ),
                                        contentAlignment = androidx.compose.ui.Alignment.Center
                                    ) {
                                        Icon(
                                            item.icon, 
                                            contentDescription = item.label,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                },
                                label = { 
                                    Text(
                                        item.label,
                                        style = MaterialTheme.typography.labelSmall
                                    ) 
                                },
                                selected = isSelected,
                                onClick = {
                                    navController.navigate(item.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = DesignColors.NavigationActive,
                                    selectedTextColor = DesignColors.NavigationActive,
                                    unselectedIconColor = DesignColors.NavigationInactive,
                                    unselectedTextColor = DesignColors.NavigationInactive,
                                    indicatorColor = DesignColors.NavigationBackground
                                )
                            )
                        }
                    }
                }
            ) { innerPadding ->
                NavHost(
                    navController = navController,
                    startDestination = startDestination,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    composable("hub") {
                        HubScreen(
                            permissionRefreshTrigger = permissionRefreshTrigger,
                            isDefaultSmsApp = isDefaultSmsApp,
                            onOpenSmsConversation = { address ->
                                pendingSmsAddress = address
                                navController.navigate("sms") {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                    composable("sms") {
                        SmsScreen(
                            isDefaultSmsApp = isDefaultSmsApp,
                            pendingAddress = pendingSmsAddress,
                            onPendingAddressConsumed = { pendingSmsAddress = null }
                        )
                    }
                    composable("messages") {
                        MessagesScreen(
                            isDefaultSmsApp = isDefaultSmsApp,
                            onOpenSmsConversation = { address ->
                                pendingSmsAddress = address
                                navController.navigate("sms") {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                    composable("menu") {
                        MenuScreen(
                            onRequestPermissions = onRequestPermissions,
                            onNavigateToMessages = { navController.navigate("messages") },
                            onNavigateToSettings = { navController.navigate("settings") },
                            onNavigateToStats = { navController.navigate("stats") },
                            onNavigateToTest = { navController.navigate("test") },
                            onNavigateToDashboard = { navController.navigate("dashboard") },
                            isDefaultSmsApp = isDefaultSmsApp,
                            refreshTrigger = permissionRefreshTrigger,
                            checkPermissions = checkPermissions
                        )
                    }
                    composable("dashboard") {
                        DashboardScreen(
                            onRequestPermissions = onRequestPermissions,
                            onNavigateToMessages = { navController.navigate("messages") },
                            onNavigateToSettings = { navController.navigate("settings") },
                            isDefaultSmsApp = isDefaultSmsApp,
                            refreshTrigger = permissionRefreshTrigger
                        )
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
        
        // KEYBOARD OFFSET SPACER - at the VERY BOTTOM of the app
        // This pushes the entire UI upward when keyboard is visible
        // Works globally for ALL input fields across ALL screens
        if (keyboardOffsetDp > 0.dp) {
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(keyboardOffsetDp)
                    .background(DesignColors.Background)
            )
        }
    }
}

data class BottomNavItem(
    val route: String,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)
