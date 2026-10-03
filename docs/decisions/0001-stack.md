# 0001: Stack

**Status:** accepted, 2026-10-03

## Context

Cycle is built for one person first, on her Android phone, by coding agents working from Linear
tickets. It stores health data, so it should keep that data on the device. It ships as an APK at
first and moves to Google Play later, so anything Play makes permanent has to be right from day one.

## Decisions

| Area | Decision |
| -- | -- |
| Platform | Android first. Other platforms get their own native app and their own record. |
| Language and UI | Kotlin and Jetpack Compose. Single activity with Compose Navigation. |
| Design system | Material 3 underneath, wrapped in the app's own components and tokens (colors, type, shapes, spacing) so the app has its own look. **No dynamic color**: the palette belongs to the app, not the wallpaper. The visual direction and the choice between Material 3 and Material 3 Expressive come from the visual identity research. |
| Modules | The [Now in Android](https://github.com/android/nowinandroid) layout: `app`, `core:designsystem` (tokens, theme, components), `core:ui` (shared app-level UI), and one `feature:*` module per feature as features arrive. Shared Gradle setup lives in convention plugins under `build-logic`. A component catalog app shows every design system component. |
| Architecture | ViewModels expose `StateFlow`; repositories sit between them and storage. No DI framework until manual wiring hurts. |
| Minimum SDK | 29 (Android 10). `targetSdk` and `compileSdk` track the latest stable API, as Play requires. |
| Storage | Room (SQLite) for cycle data, DataStore for settings. `java.time` for dates. |
| Backup | Android's built-in backup includes the database. No custom sync, no backend. |
| Health Connect | Not now. Revisit with its own record if she asks for it. |
| Build | Gradle with the Kotlin DSL, a version catalog in `gradle/libs.versions.toml`, and the Gradle wrapper. A JDK toolchain pins the Java version. |
| Lint and format | ktlint for formatting, Android Lint for the rest. Both fail the build on errors. |
| Tests | JUnit 4 unit tests, plus Robolectric for Room and Compose UI tests, so the whole suite runs on the JVM without an emulator. Roborazzi screenshot tests render design system components and screens to PNGs, in light and dark, as visual evidence and regression baselines. |
| CI | GitHub Actions on Ubuntu runs lint, the tests and a debug build on every PR. |
| Distribution | Signed release APKs on GitHub Releases for now; Google Play later. |
| App ID | `com.tonypine.cycle`. Play makes it permanent, so it does not change. |
| Signing | One release keystore, created once and kept outside the repo (password manager plus GitHub Actions secrets). It becomes the Play signing key through Play App Signing, so the sideloaded install keeps updating once the app moves to Play. |

## Consequences

- Agents can run every check locally and in CI without an emulator or a device, and can show what the UI looks like through screenshot tests.
- Nothing leaves the device apart from Android's own backup. Adding sync, analytics, crash
  reporting or any third-party SDK that sends data needs a new record.
- Losing the release keystore means she has to reinstall the app (her data comes back from backup).
