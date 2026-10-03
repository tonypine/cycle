# Cycle

A native menstrual cycle tracking app, built for my wife and shaped by her requests.

Work is planned in Linear (team Momot, initiative "Menstrual Cycle App") and built by coding agents
through [Symphony](https://github.com/tonypine/symphony).

**Status:** the Android app is scaffolded; the MVP scope is still being planned. The stack is
recorded in [`docs/decisions/0001-stack.md`](docs/decisions/0001-stack.md).

## Project layout

| Module | What it holds |
| -- | -- |
| `app` | The Cycle app (`com.tonypine.cycle`, minSdk 29). |
| `app-catalog` | A separate app that shows every design system piece, for building UI in isolation. |
| `core:designsystem` | `CycleTheme`: colours, type and spacing on Compose Foundation, no Material. |
| `core:ui` | Shared composables built on the design system. |
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

The full set, run by CI on every PR:

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

On failure, the comparison images are in `<module>/build/outputs/roborazzi/`.

## Privacy

This repository is public. It holds code only: no real cycle or health data, in fixtures,
screenshots, logs or anywhere else.
