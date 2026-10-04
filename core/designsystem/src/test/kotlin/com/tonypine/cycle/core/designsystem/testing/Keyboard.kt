package com.tonypine.cycle.core.designsystem.testing

import android.graphics.Insets
import android.view.View
import android.view.WindowInsets

/**
 * Applies [heightDp] of IME insets to this view's window, as the system does when the keyboard opens.
 * Robolectric has no keyboard. Call it on the main thread, with a view from the window that should
 * see the keyboard: a sheet or dialog has a window of its own.
 */
fun View.showKeyboard(heightDp: Float) {
    val height = (heightDp * resources.displayMetrics.density).toInt()
    val insets = WindowInsets.Builder()
        .setInsets(WindowInsets.Type.ime(), Insets.of(0, 0, 0, height))
        .setVisible(WindowInsets.Type.ime(), true)
        .build()
    rootView.dispatchApplyWindowInsets(insets)
}
