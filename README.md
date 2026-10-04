# Cycle

A native menstrual cycle tracking app, built for my wife and shaped by her requests.

Work is planned in Linear (team Momot, initiative "Menstrual Cycle App") and built by coding agents
through [Symphony](https://github.com/tonypine/symphony).

**Status:** the Android app is scaffolded; the MVP scope is still being planned. The stack is
recorded in [`docs/decisions/0001-stack.md`](docs/decisions/0001-stack.md).

## Project layout

| Module | What it holds |
| -- | -- |
| `app` | The Cycle app (`com.tonypine.cycle`, minSdk 29). For now, one placeholder screen. |
| `app-catalog` | A separate app (`com.tonypine.cycle.catalog`) that shows every design system token and component, one section per page, for building UI in isolation. |
| `core:designsystem` | `CycleTheme`, the Zest tokens (colour, type, shape, spacing, elevation) and the components (buttons, icon buttons, chips) on Compose Foundation, no Material. See [`docs/design/design-system.md`](docs/design/design-system.md). |
| `core:ui` | Shared app-level UI built on the design system. Empty for now. |
| `build-logic` | Gradle convention plugins shared by every module. |

## Build and run

You need JDK 21 and the Android SDK. Point Gradle at the SDK with `ANDROID_HOME` or a
`local.properties` file containing `sdk.dir=/path/to/Android/sdk` (never commit it).

```sh
./gradlew assembleDebug                       # build both apps
./gradlew :app:installDebug                   # install Cycle on a connected device or emulator
./gradlew :app-catalog:installDebug           # install the design system catalog
```

## Checks

Before a push, run the targeted checks listed under `Validation` in `AGENTS.md`. The full set, run by
CI on every PR:

```sh
./gradlew ktlintCheck lint testDebugUnitTest verifyRoborazziDebug assembleDebug
```

`./gradlew ktlintFormat` fixes most formatting issues. Any `androidx.compose.material` or
`material3` dependency, direct or transitive, fails the build.

## Screenshot tests

Roborazzi captures Compose screens on the JVM with Robolectric, in light and dark themes. Reference
images are committed under each module's `src/test/screenshots/`.

```sh
./gradlew recordRoborazziDebug   # re-record references after an intended UI change, then commit them
./gradlew verifyRoborazziDebug   # compare against the references; fails on any difference
```

On failure, the comparison images are in `<module>/build/outputs/roborazzi/`. Robolectric emulates the
SDK set in `build-logic/robolectric/robolectric.properties` for every module.

## Releases

Every merge to `main` ships a signed release APK on GitHub Releases, tagged `v<versionName>`
(e.g. `v0.1.42`, where `42` is the commit count on `main`). The flow and its options are recorded
in [`docs/decisions/0002-release-distribution.md`](docs/decisions/0002-release-distribution.md).

### One-time setup

**The release key is permanent.** The same key becomes the Google Play app signing key, so the
installed app keeps updating. If it is lost, the app has to be uninstalled and reinstalled.

1. Create the keystore (Play requires a key valid until at least 2033):

   ```sh
   keytool -genkeypair -v -keystore cycle-release.jks -alias cycle -keyalg RSA -keysize 4096 -validity 10000
   ```

2. Back up `cycle-release.jks`, the alias (`cycle`) and both passwords in the password manager
   **before** adding the secrets. With keytool's default PKCS12 format, the key password is the
   store password.
3. Add the GitHub Actions secrets. `gh secret set` prompts for the values, so the passwords stay
   out of your shell history:

   ```sh
   base64 -i cycle-release.jks | gh secret set RELEASE_KEYSTORE_BASE64
   gh secret set RELEASE_KEYSTORE_PASSWORD   # the store password
   gh secret set RELEASE_KEY_ALIAS           # cycle
   gh secret set RELEASE_KEY_PASSWORD        # the key password
   ```

4. Delete the local `cycle-release.jks`. Never commit it: `*.jks` and `*.keystore` are ignored.

### Building a release locally

`./gradlew :app:assembleRelease` builds an unsigned APK with version `0.1.0`. To sign it, set
`RELEASE_KEYSTORE_PATH` (absolute path to the `.jks`), `RELEASE_KEYSTORE_PASSWORD`,
`RELEASE_KEY_ALIAS` and `RELEASE_KEY_PASSWORD`; to version it, add
`-Pcycle.versionCode=42 -Pcycle.versionName=0.1.42`.

## Privacy

This repository is public. It holds code only: no real cycle or health data, in fixtures,
screenshots, logs or anywhere else.
