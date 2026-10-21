package com.ovehbe.junkboy.ui.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.ovehbe.junkboy.utils.SmsAppManager

/** Default-SMS-app state that is re-read on every resume, e.g. after the system role dialog closes. */
@Composable
fun rememberIsDefaultSmsApp(smsAppManager: SmsAppManager): Boolean {
    var isDefault by remember { mutableStateOf(smsAppManager.isJunkboyDefaultSmsApp()) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, smsAppManager) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) isDefault = smsAppManager.isJunkboyDefaultSmsApp()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    return isDefault
}
