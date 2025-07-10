package com.ovehbe.junkboy.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.ovehbe.junkboy.ui.compose.JunkboyApp
import com.ovehbe.junkboy.ui.theme.JunkboyTheme
import com.ovehbe.junkboy.utils.PreferencesManager

class MainActivity : ComponentActivity() {
    
    private lateinit var preferencesManager: PreferencesManager
    
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        preferencesManager.setPermissionsGranted(allGranted)
        
        if (allGranted) {
            // Permissions granted, app can start filtering
            // Trigger UI refresh
            permissionRefreshTrigger++
        } else {
            // Show explanation or disable functionality
        }
    }
    
    // Add state for permission refresh trigger
    private var permissionRefreshTrigger by mutableIntStateOf(0)
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        preferencesManager = PreferencesManager(this)
        
        // Check and request permissions
        checkAndRequestPermissions()
        
        setContent {
            JunkboyTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    JunkboyApp(
                        onRequestPermissions = { checkAndRequestPermissions() },
                        permissionRefreshTrigger = permissionRefreshTrigger
                    )
                }
            }
        }
    }
    
    private fun checkAndRequestPermissions() {
        val requiredPermissions = arrayOf(
            "android.permission.RECEIVE_SMS",
            "android.permission.READ_SMS", 
            "android.permission.WRITE_SMS",
            "android.permission.SEND_SMS",
            "android.permission.RECEIVE_MMS",
            "android.permission.RECEIVE_WAP_PUSH",
            Manifest.permission.POST_NOTIFICATIONS
        )
        
        val missingPermissions = requiredPermissions.filter { permission ->
            ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED
        }
        
        if (missingPermissions.isNotEmpty()) {
            requestPermissionLauncher.launch(missingPermissions.toTypedArray())
        } else {
            preferencesManager.setPermissionsGranted(true)
        }
    }
    
    private fun hasAllPermissions(): Boolean {
        val requiredPermissions = arrayOf(
            "android.permission.RECEIVE_SMS",
            "android.permission.READ_SMS", 
            "android.permission.WRITE_SMS",
            "android.permission.SEND_SMS",
            "android.permission.RECEIVE_MMS",
            "android.permission.RECEIVE_WAP_PUSH",
            Manifest.permission.POST_NOTIFICATIONS
        )
        
        return requiredPermissions.all { permission ->
            ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
        }
    }
} 