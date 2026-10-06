package com.tonypine.cycle

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.tonypine.cycle.core.testing.assertNeverSaid
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The app's own strings follow the copy rules in `docs/research/product-implications.md`, in every
 * language Cycle has, like each feature's (`TodayStringsTest`).
 */
@RunWith(AndroidJUnit4::class)
class AppStringsTest {
    @Test
    fun `no string says what the app never says`() {
        assertNeverSaid(R.string::class.java)
    }
}
