# Cycle

A native menstrual cycle tracking app, built for my wife and shaped by her requests.

Work is planned in Linear (team Momot, initiative "Menstrual Cycle App") and built by coding agents
through [Symphony](https://github.com/tonypine/symphony).

**Status:** the Android app is scaffolded; the MVP scope is still being planned. The stack is
recorded in [`docs/decisions/0001-stack.md`](docs/decisions/0001-stack.md). Research on the menstrual
cycle (normal ranges, predictions, what to track, health signals, other apps) and what it means for
the app's features is in [`docs/research/`](docs/research/README.md).

## Install on the phone

Every merge to `main` publishes a signed APK. The newest one is always at
**<https://github.com/tonypine/cycle/releases/latest>**.

1. Open that link on the phone. The page shows the version (for example `v0.1.42`), what changed,
   and under **Assets** a file named `cycle-v0.1.42.apk`.
2. Tap the APK to download it.
3. Open the downloaded file and tap **Install**. The first time, Android asks to allow installs from
   the browser (or Files app): allow it, go back, and install.
4. For an update, do the same with the newest release. It installs over the current app and keeps
   its data, because every release is signed with the same key and has a higher version.

## Project layout

| Module | What it holds |
| -- | -- |
| `app` | The Cycle app (`com.tonypine.cycle`, minSdk 29): the activity, the navigation between the four tabs (Today, Calendar, History, Settings) and the data wiring. History and Settings are placeholders for now. |
| `app-catalog` | A separate app (`com.tonypine.cycle.catalog`) that shows every design system token and component, one section per page, for building UI in isolation. |
| `core:designsystem` | `CycleTheme`, the Zest tokens (colour, type, shape, spacing, elevation) and the components (buttons, icon buttons, chips) on Compose Foundation, no Material. See [`docs/design/design-system.md`](docs/design/design-system.md). |
| `core:ui` | Shared app-level UI built on the design system: the day log sheet Today and the calendar both open, and the cycle state of each calendar day. |
| `feature:calendar` | Calendar: her logged and estimated periods month by month, and the day log for any day up to today (log, fill in a forgotten period, clear a day). |
| `feature:today` | Today: her cycle day, this week, the next period estimate, the one-tap period buttons and the "missed a period?" question. |
| `build-logic` | Gradle convention plugins shared by every module. |

## Build and run

To develop on the app, build and install it from source. You need JDK 21 and the Android SDK. Point
Gradle at the SDK with `ANDROID_HOME` or a `local.properties` file containing
`sdk.dir=/path/to/Android/sdk` (never commit it).

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

## Agent sandbox

Symphony runs agents in a macOS sandbox: `$HOME` is read-only except `~/.gradle`, temp files belong
in the sandbox's own `$TMPDIR`, and Gradle has no network (the sandbox's proxy is only in
environment variables, which Java ignores). The build handles the Robolectric side itself:

- Gradle resolves Robolectric's `android-all` runtime into its cache, and Robolectric reads it from
  there through `robolectric-deps.properties`, instead of downloading it into `~/.m2` under a lock
  file in `$HOME`. `after_create` in `WORKFLOW.md` runs `writeRobolectricDeps`, which downloads it
  before an agent starts. The version is pinned as `robolectricAndroidAll` in
  `gradle/libs.versions.toml`; after a Robolectric or SDK bump, a test fails with
  `no artifacts found for DependencyJar{…}`, naming the version to pin.
- Tests write temp files under each module's `build/tmp`, not the system temp dir.

The sandbox config still has to provide:

- A JDK 21. None is on the sandbox `PATH` and `/usr/libexec/java_home` fails there, so agents set
  `JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home` by hand. Exporting
  `JAVA_HOME` in the sandbox environment removes that step.
- A writable temp dir for the Gradle daemon. Java on macOS ignores `$TMPDIR` and uses the per-user
  `/var/folders/…/T`, which the sandbox makes read-only, and every Kotlin compile writes a
  `kotlin-compiler-in-*.alive` file there. Add `-Djava.io.tmpdir=$TMPDIR` to the sandbox's
  `JAVA_TOOL_OPTIONS` (it already sets `-Djava.net.preferIPv4Stack=true`), or allow writes to
  `$(getconf DARWIN_USER_TEMP_DIR)`. The Kotlin daemon cannot write to
  `~/Library/Application Support/kotlin` either, so Kotlin compiles in the Gradle daemon instead:
  slower, but it works. The same temp dir also blocks Android Gradle plugin tasks such as
  `mergeDebugJavaResource`.
- A debug keystore. `assembleDebug` creates `~/.android/debug.keystore` when it is missing, and
  `~/.android` is read-only in the sandbox: create it once outside, or allow writes there.

## Releases

Every merge to `main` ships a GitHub Release, tagged `v<versionName>` (e.g. `v0.1.42`, where `42`
is the commit count on `main`), with two signed release APKs:

- `cycle-v<versionName>.apk`: the Cycle app (`com.tonypine.cycle`).
- `cycle-catalog-v<versionName>.apk`: the design-system catalog (`com.tonypine.cycle.catalog`), for
  reviewing UI on a phone. It has the same version and release key as the app, so it updates in
  place too. A catalog built locally with `installDebug` is debug-signed: uninstall it once before
  installing the release catalog, because Android refuses an update signed with another key.

The flow and its options are recorded in
[`docs/decisions/0002-release-distribution.md`](docs/decisions/0002-release-distribution.md).

[`.github/workflows/release.yml`](.github/workflows/release.yml) runs after `CI` passes on a push
to `main`: it builds `:app:assembleRelease` and `:app-catalog:assembleRelease` with the version and
signing inputs, checks each APK with `apksigner verify` and its package and version with
`aapt2 dump badging`, and creates the tag and the GitHub Release with both APKs and the changelog.
If a signing input is missing, the job fails naming it and publishes nothing. A second job then
posts an update to the Linear initiative "Menstrual Cycle App" with the version, the changelog and
links to both APKs and the release. If that call fails, the job fails with Linear's error and the
release stays published.

The release notes come from `scripts/release/changelog.sh`, which works the same locally:

```sh
scripts/release/changelog.sh        # notes for HEAD since the previous v* tag
scripts/release/changelog_test.sh   # its test, on a throwaway repository in $TMPDIR
```

The Linear update comes from `scripts/release/linear_update.sh`. Its `body` command prints the
update's Markdown and needs no key:

```sh
scripts/release/changelog.sh > notes.md
scripts/release/linear_update.sh body v0.1.42 \
  https://github.com/tonypine/cycle/releases/tag/v0.1.42 \
  https://github.com/tonypine/cycle/releases/download/v0.1.42/cycle-v0.1.42.apk \
  https://github.com/tonypine/cycle/releases/download/v0.1.42/cycle-catalog-v0.1.42.apk notes.md
scripts/release/linear_update_test.sh   # its test, against a stub curl: nothing reaches Linear
```

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
   out of your shell history. The key alias is not sensitive, so it is a repository variable:
   GitHub masks every occurrence of a secret's value, and a secret `cycle` would turn
   `tonypine/cycle` into `tonypine/***` in the logs and drop the release job's outputs.

   ```sh
   base64 -i cycle-release.jks | gh secret set RELEASE_KEYSTORE_BASE64
   gh secret set RELEASE_KEYSTORE_PASSWORD   # the store password
   gh secret set RELEASE_KEY_PASSWORD        # the key password
   gh variable set RELEASE_KEY_ALIAS --body cycle
   ```

4. Delete the local `cycle-release.jks`. Never commit it: `*.jks` and `*.keystore` are ignored.
5. For the Linear initiative update, create a Linear API key made only for releases. Do not reuse
   Symphony's key: give this one the narrowest permissions Linear allows that still create
   initiative updates (write access, no admin). Save it as a secret, and the initiative's ID as a
   repository variable:

   ```sh
   gh secret set LINEAR_RELEASE_API_KEY   # the release-only Linear API key
   gh variable set LINEAR_INITIATIVE_ID --body ce3b72fb-b27e-426b-85dd-efb922893a71   # "Menstrual Cycle App"
   ```

### Re-posting a Linear update

When the Linear update failed (Linear was down, or the secret or variable was missing), fix the
cause and post it for the existing release by hand. Re-running the whole release run does not
post, because the release already exists. This builds and publishes nothing:

```sh
gh workflow run release.yml -f tag=v0.1.42
```

Or in GitHub: **Actions > Release > Run workflow**, with the tag. Running it again for a tag that
already has an update posts a second one.

### Building a release locally

`./gradlew :app:assembleRelease` builds an unsigned APK with version `0.1.0`, and
`./gradlew :app-catalog:assembleRelease` the catalog's. Both apps share the release setup, from the
`cycle.android.application` convention in `build-logic`. To sign them, set
`RELEASE_KEYSTORE_PATH` (absolute path to the `.jks`), `RELEASE_KEYSTORE_PASSWORD`,
`RELEASE_KEY_ALIAS` and `RELEASE_KEY_PASSWORD`; to version them, add
`-Pcycle.versionCode=42 -Pcycle.versionName=0.1.42`.

## Privacy

This repository is public. It holds code only: no real cycle or health data, in fixtures,
screenshots, logs or anywhere else.
