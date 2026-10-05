package com.tonypine.cycle.feature.settings

import android.app.KeyguardManager
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LifecycleResumeEffect

/**
 * Whether the phone has a screen lock (a PIN, pattern or password), read again each time Cycle comes
 * back to the front, so a lock she just set in the phone's settings counts on her return. Android
 * backs Cycle up only with one (`docs/decisions/0004-backup-encryption.md`). Asked of the phone
 * only; nothing is sent anywhere.
 */
@Composable
internal fun rememberHasScreenLock(): Boolean {
    val context = LocalContext.current
    var locked by remember(context) { mutableStateOf(context.hasScreenLock()) }
    LifecycleResumeEffect(context) {
        locked = context.hasScreenLock()
        onPauseOrDispose {}
    }
    return locked
}

// Without a KeyguardManager there is no telling, so no note says she has no lock.
private fun Context.hasScreenLock(): Boolean = getSystemService(KeyguardManager::class.java)?.isDeviceSecure ?: true
