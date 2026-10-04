# Working in this repository

Cycle is a native menstrual cycle tracking app, built for one person (my wife) from the requests she
files in Linear. Work arrives as Linear tickets in team Momot (MOT) and is built by coding agents
through Symphony. A human reviews every PR.

## The repo is public, the data is not

This app handles health data. Nothing real goes in a commit, PR, issue comment or log:

- No real cycle, symptom, mood or other health data, in fixtures, screenshots, test data or logs.
  Use obviously synthetic data.
- No tokens, keystores, signing keys, `.env` files or `local.properties`.
- Default to local-first storage on the device. Any feature that sends data off the device (sync,
  analytics, crash reporting, third-party SDKs) needs an explicit decision in a ticket first.

## Platform and stack

Native only. Android comes first. The specific stack (language, UI toolkit, storage, minimum SDK,
build tooling) is recorded in `docs/decisions/0001-stack.md` once decided. Until that file exists,
do not pick a stack on your own.

Decisions that shape the whole codebase go in `docs/decisions/` as numbered records
(`NNNN-short-title.md`): what was decided, the options considered, and why.

## Commits and branches

- Commits: `type: message`, e.g. `feat: log period start date`. Types: `feat`, `fix`, `refactor`,
  `chore`, `test`, `docs`, `style`, `perf`. One small delivery per commit.
- Branches: `type/short-description`, lowercase and hyphenated.

## Shared skills

Reusable playbooks live under `.ai/skills/` and are shared between agents through symlinks:
`.claude/skills/` and `.codex/skills/` both point there. Add new skills under
`.ai/skills/<name>/SKILL.md`, never inside one agent's folder.

## Validation

Run from the repo root with JDK 21. Every command must pass before a change is called done; CI
(`.github/workflows/ci.yml`) runs the same set on every PR:

```sh
./gradlew ktlintCheck lint testDebugUnitTest verifyRoborazziDebug assembleDebug
```

- `ktlintCheck`: formatting, including `build-logic`. `./gradlew ktlintFormat` fixes most issues.
- `lint`: Android Lint. Errors fail the build.
- `testDebugUnitTest`: JUnit and Robolectric tests, all on the JVM.
- `verifyRoborazziDebug`: compares screenshots with the references in each module's
  `src/test/screenshots/`. After an intended UI change, run `./gradlew recordRoborazziDebug` and
  commit the new images.
- `assembleDebug`: builds `app` and `app-catalog`. Every build also runs
  `checkNoMaterialDependencies`, which fails if `androidx.compose.material` or `material3` reaches a
  classpath, even transitively. UI is built on Compose Foundation and `core:designsystem`.
