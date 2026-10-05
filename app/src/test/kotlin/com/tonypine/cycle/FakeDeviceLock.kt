package com.tonypine.cycle

import com.tonypine.cycle.core.ui.DeviceLock
import kotlinx.coroutines.CompletableDeferred

/**
 * The phone's lock in tests: [available] says whether the phone can ask, and each prompt waits
 * until the test [answer]s it, as she would.
 */
class FakeDeviceLock(var available: Boolean = true) : DeviceLock {
    /** How many times the prompt was asked for. */
    var prompts = 0
        private set
    private var showing: CompletableDeferred<Boolean>? = null

    /** Whether a prompt is waiting for her. */
    val prompting: Boolean get() = showing != null

    override fun canAuthenticate(): Boolean = available

    override suspend fun authenticate(): Boolean {
        prompts++
        if (!available) return false
        val prompt = CompletableDeferred<Boolean>().also { showing = it }
        return prompt.await()
    }

    /** She unlocked, or cancelled the prompt. */
    fun answer(unlocked: Boolean) {
        val prompt = checkNotNull(showing) { "No prompt is showing" }
        showing = null
        prompt.complete(unlocked)
    }
}
