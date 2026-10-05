package com.tonypine.cycle.core.ui

/**
 * The phone's own lock, which "Lock Cycle" asks for: its fingerprint or face first, and its PIN,
 * pattern or password when those fail or are not set up. Cycle never sees what she enters. It is an
 * access gate: her data on the phone is not encrypted with it.
 */
interface DeviceLock {
    /** Whether the phone can ask: it has a screen lock, or a fingerprint or face she set up. */
    fun canAuthenticate(): Boolean

    /**
     * Shows the phone's prompt. True once she unlocked; false if she cancelled it, it failed or the
     * phone can't ask. Call it on the main thread.
     */
    suspend fun authenticate(): Boolean
}
