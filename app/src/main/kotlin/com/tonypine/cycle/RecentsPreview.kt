package com.tonypine.cycle

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Build
import android.view.WindowManager
import androidx.annotation.RequiresApi

/**
 * Keeps her data out of the recent-apps screen while [hidden]. Android 13 and later show no snapshot
 * of the app (`setRecentsScreenshotEnabled`); earlier versions get `FLAG_SECURE`, which also leaves
 * screenshots and screen recordings of Cycle blank.
 *
 * @param sdk the phone's API level; tests pass another one.
 */
@SuppressLint("NewApi") // sdk is Build.VERSION.SDK_INT outside tests.
internal fun Activity.hideInRecents(hidden: Boolean, sdk: Int = Build.VERSION.SDK_INT) {
    when {
        sdk >= Build.VERSION_CODES.TIRAMISU -> hideRecentsScreenshot(hidden)
        hidden -> window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        else -> window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
    }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private fun Activity.hideRecentsScreenshot(hidden: Boolean) = setRecentsScreenshotEnabled(!hidden)
