package com.tonypine.cycle

import android.app.Activity
import android.os.Build
import android.view.WindowManager
import androidx.activity.ComponentActivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

/** With "Lock Cycle" on, the recent-apps screen shows none of Cycle. */
@RunWith(RobolectricTestRunner::class)
class RecentsPreviewTest {
    @Test
    fun `from Android 13, the recents snapshot turns off and on`() {
        val activity = Robolectric.buildActivity(RecordingActivity::class.java).setup().get()

        activity.hideInRecents(true, sdk = Build.VERSION_CODES.TIRAMISU)
        assertEquals(listOf(false), activity.screenshotsEnabled)
        activity.hideInRecents(false, sdk = Build.VERSION_CODES.TIRAMISU)

        assertEquals(listOf(false, true), activity.screenshotsEnabled)
        assertFalse(activity.isSecure())
    }

    @Test
    fun `before Android 13, the window is secure while the lock is on`() {
        val activity = Robolectric.buildActivity(RecordingActivity::class.java).setup().get()

        activity.hideInRecents(true, sdk = Build.VERSION_CODES.S_V2)
        assertTrue(activity.isSecure())
        activity.hideInRecents(false, sdk = Build.VERSION_CODES.Q)

        assertFalse(activity.isSecure())
        assertEquals(emptyList<Boolean>(), activity.screenshotsEnabled)
    }

    private fun Activity.isSecure() = window.attributes.flags and WindowManager.LayoutParams.FLAG_SECURE != 0

    /** Records what the app asks of the recents snapshot. */
    class RecordingActivity : ComponentActivity() {
        val screenshotsEnabled = mutableListOf<Boolean>()

        override fun setRecentsScreenshotEnabled(enabled: Boolean) {
            screenshotsEnabled += enabled
        }
    }
}
