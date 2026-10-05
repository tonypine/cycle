package com.tonypine.cycle

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tonypine.cycle.core.designsystem.CycleIcons
import com.tonypine.cycle.core.designsystem.CycleTheme
import com.tonypine.cycle.core.designsystem.EmptyState
import com.tonypine.cycle.core.designsystem.EmptyStateAction
import com.tonypine.cycle.core.designsystem.EmptyStateIcon

/**
 * The app behind "Lock Cycle": nothing until the setting is read, then the [LockScreen] while
 * [appLock] is locked, and [content] once she unlocked. [content] leaves composition while locked,
 * with its dialogs and sheets, so nothing of her data draws, flashes or is read by TalkBack behind
 * the lock screen. The lock screen opens the phone's prompt on its own the first time; Back leaves
 * the app.
 */
@Composable
fun LockGate(appLock: AppLock, content: @Composable () -> Unit) {
    val locked by appLock.locked.collectAsStateWithLifecycle()
    when (locked) {
        null -> Unit

        true -> {
            LockScreen(onUnlock = appLock::unlock)
            LaunchedEffect(Unit) { appLock.onLockScreenShown() }
        }

        false -> content()
    }
}

/**
 * "Cycle is locked": the app's mark, a calm line saying how to open it, and "Unlock", which opens the
 * phone's prompt. Fills the screen on `surface`, inside the system bars.
 */
@Composable
fun LockScreen(onUnlock: () -> Unit, modifier: Modifier = Modifier) {
    EmptyState(
        title = stringResource(R.string.lock_title),
        body = stringResource(R.string.lock_body),
        modifier = modifier
            .fillMaxSize()
            .background(CycleTheme.colors.surface)
            .windowInsetsPadding(WindowInsets.safeDrawing),
        illustration = { EmptyStateIcon(painterResource(R.drawable.ic_app_mark)) },
        action = EmptyStateAction(stringResource(R.string.lock_unlock), onUnlock, icon = CycleIcons.Lock)
    )
}

@Preview
@Composable
private fun LockScreenPreview() {
    CycleTheme { LockScreen(onUnlock = {}) }
}
