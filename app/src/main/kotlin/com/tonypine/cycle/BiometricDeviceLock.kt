package com.tonypine.cycle

import android.content.Context
import android.os.Build
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import com.tonypine.cycle.core.ui.DeviceLock
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CompletableDeferred

/**
 * The phone's lock through AndroidX Biometric's [BiometricPrompt]: her fingerprint or face first, and
 * the phone's PIN, pattern or password as the fallback, so she is never stuck when the sensor fails.
 * Everything happens on the phone; Cycle only learns whether she unlocked.
 *
 * The prompt belongs to an activity: [attach] each one in `onCreate`. A prompt still showing through
 * a rotation answers the new activity's prompt, whose callback is the same one here.
 */
class BiometricDeviceLock(context: Context) : DeviceLock {
    private val biometrics = BiometricManager.from(context.applicationContext)
    private val promptInfo = BiometricPrompt.PromptInfo.Builder()
        .setTitle(context.getString(R.string.lock_prompt_title))
        .setSubtitle(context.getString(R.string.lock_prompt_subtitle))
        .setAllowedAuthenticators(AUTHENTICATORS)
        // Face unlock needs no extra tap.
        .setConfirmationRequired(false)
        .build()

    private var activity: FragmentActivity? = null
    private var prompt: BiometricPrompt? = null
    private var showing: CompletableDeferred<Boolean>? = null

    private val callback = object : BiometricPrompt.AuthenticationCallback() {
        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) = answer(true)

        // Cancelled, too many tries, or the phone can't ask. A finger the sensor did not match is not
        // an error: the prompt stays for another try or the screen lock.
        override fun onAuthenticationError(errorCode: Int, errString: CharSequence) = answer(false)
    }

    /** The activity that shows the prompt from now on. */
    fun attach(activity: FragmentActivity) {
        this.activity = activity
        prompt = BiometricPrompt(activity, ContextCompat.getMainExecutor(activity), callback)
        activity.lifecycle.addObserver(
            object : DefaultLifecycleObserver {
                override fun onDestroy(owner: LifecycleOwner) {
                    if (this@BiometricDeviceLock.activity !== activity) return
                    this@BiometricDeviceLock.activity = null
                    prompt = null
                    // Closed for good, not rotating: no prompt will answer.
                    if (!activity.isChangingConfigurations) answer(false)
                }
            }
        )
    }

    override fun canAuthenticate(): Boolean =
        biometrics.canAuthenticate(AUTHENTICATORS) == BiometricManager.BIOMETRIC_SUCCESS

    override suspend fun authenticate(): Boolean {
        val activity = activity ?: return false
        val prompt = prompt ?: return false
        // A prompt asked for while the activity is stopping would never show, nor answer.
        if (!activity.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED) ||
            activity.supportFragmentManager.isStateSaved
        ) {
            return false
        }
        // A prompt asked for again replaces the one before.
        answer(false)
        val answer = CompletableDeferred<Boolean>().also { showing = it }
        prompt.authenticate(promptInfo)
        return try {
            answer.await()
        } catch (cancelled: CancellationException) {
            this.prompt?.cancelAuthentication()
            throw cancelled
        } finally {
            if (showing === answer) showing = null
        }
    }

    private fun answer(unlocked: Boolean) {
        val answer = showing ?: return
        showing = null
        answer.complete(unlocked)
    }

    companion object {
        /**
         * A strong biometric (fingerprint, or a face unlock the phone rates as secure) or the screen lock.
         * Android 10 (API 29, the minimum) can't combine a strong-only biometric with the screen lock:
         * its prompt has only `setDeviceCredentialAllowed`, and AndroidX rejects `BIOMETRIC_STRONG or
         * DEVICE_CREDENTIAL` below API 30. There it takes any biometric the phone's own lock screen
         * takes, and the screen lock stays the fallback.
         */
        val AUTHENTICATORS: Int = authenticators(Build.VERSION.SDK_INT)

        internal fun authenticators(sdk: Int): Int = if (sdk >= Build.VERSION_CODES.R) {
            BIOMETRIC_STRONG or DEVICE_CREDENTIAL
        } else {
            BIOMETRIC_WEAK or DEVICE_CREDENTIAL
        }
    }
}
