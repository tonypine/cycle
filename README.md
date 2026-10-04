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

## Privacy

This repository is public. It holds code only: no real cycle or health data, in fixtures,
screenshots, logs or anywhere else.
