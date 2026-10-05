# 0005: App lock through the phone's own lock

**Status:** accepted, 2026-10-05

## Context

Cycle holds intimate health data on a phone other people sometimes hold.
[`apps-and-privacy.md`](../research/apps-and-privacy.md) lists an app lock as cheap and expected in
this category. She asked for one that is optional and off by default
([MOT-47](https://linear.app/tonypine/issue/MOT-47)), with a 30-second grace period and the phone's
screen lock as the fallback when biometrics fail.

## Decision

"Lock Cycle", a switch in Settings under Your data, makes Cycle open only after the phone's own lock.

| Area | Decision |
| -- | -- |
| Prompt | AndroidX Biometric's `BiometricPrompt` (`androidx.biometric:biometric` 1.1.0, the latest stable). Fingerprint or face first; the phone's PIN, pattern or password is always the fallback, so she is never stuck when the sensor fails. Cycle never sees what she enters, only whether she unlocked. |
| Android 11 and later | `BIOMETRIC_STRONG or DEVICE_CREDENTIAL`. |
| Android 10 (API 29, the minimum) | `BIOMETRIC_WEAK or DEVICE_CREDENTIAL`. Android 10's prompt can only allow "any biometric plus the screen lock" (`setDeviceCredentialAllowed`), and AndroidX rejects `BIOMETRIC_STRONG or DEVICE_CREDENTIAL` below API 30. This accepts what the phone's own lock screen accepts, and the screen lock stays the fallback. |
| When it locks | On a cold start (a new process), and when she comes back after more than 30 seconds in the background, counted on `elapsedRealtime` so deep sleep counts. Time spent in the phone's PIN screen during a prompt is not time away. |
| Lock screen | A full screen with the app's mark, "Cycle is locked" and "Unlock". It opens the prompt on its own once per lock. The app's screens leave composition while locked, dialogs and sheets included, so nothing of her data draws or is read by TalkBack behind it. Back leaves the app. |
| Turning it on or off | Each asks for the phone's lock first, so she can't lock herself out with a lock the phone doesn't have and nobody else turns it off. Cancelled, it stays as it was. |
| No lockout | A phone with no screen lock, fingerprint or face can't prompt: turning the lock on explains that, and a lock already on turns itself off on the next start or return, with a one-time note in Settings. |
| Recent apps | While the lock is on, the recent-apps screen shows no snapshot: `setRecentsScreenshotEnabled(false)` on Android 13 and later, `FLAG_SECURE` below, which also blanks screenshots there. |
| The activity | `MainActivity` is a `FragmentActivity`, which `BiometricPrompt` needs (`androidx.fragment` 1.9.1). |

## Options considered

- **The phone's lock through `BiometricPrompt` (chosen).** No secret of our own to store, reset or
  forget, and the same prompt she knows from banking apps. Works only on a phone with a screen lock,
  which is also what end-to-end encrypted backups need ([`0004`](0004-backup-encryption.md)).
- **A Cycle PIN of our own.** Works without a screen lock, but we would store, check and rate-limit a
  secret, and a forgotten PIN means a reset path that would also let anyone else in.
- **Framework `android.hardware.biometrics.BiometricPrompt` directly.** No dependency, but we would
  reimplement AndroidX's per-version handling and its `canAuthenticate` checks across API 29 to 37.

## Consequences

- It is an access gate, not encryption: her data on the phone is protected by the phone's own
  storage encryption as before, and the copy says only that Cycle opens with her lock.
- New dependencies, AndroidX and on the phone with no network: `androidx.biometric` 1.1.0 and
  `androidx.fragment` 1.9.1, which bring `androidx.appcompat` 1.2.0 at runtime. No Material.
- "Delete everything" clears every setting, the lock included.
- The setting is in the settings file, so an encrypted backup restores it on a new phone; that phone
  has a screen lock, since backups need one.
