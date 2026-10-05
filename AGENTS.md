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

## Research on the cycle

Before planning a feature, screen, prediction, notification or copy that touches the cycle itself,
read `docs/research/`, starting with `docs/research/product-implications.md`: what is normal, what
can be predicted and how well, what to track, when a pattern is worth a doctor's visit, and the
questions only she can answer. The app never offers contraception or a diagnosis, and labels every
estimate as one. When a ticket learns something that changes these notes, update them in the same
PR.

## UI and the design system

Feature code uses design system components and tokens from `core:designsystem` (`CycleTheme.colors`,
`.typography`, `.shapes`, `.spacing`, `.elevation`), never ad-hoc styled Foundation code or
hard-coded colours, sizes and text styles. If a screen needs something the design system lacks, add
the token or component there first, with its catalog entry, previews and tests. How to do that is in
`docs/design/design-system.md`.

## Commits and branches

- Commits: `type: message`, e.g. `feat: log period start date`. Types: `feat`, `fix`, `refactor`,
  `chore`, `test`, `docs`, `style`, `perf`. One small delivery per commit.
- Branches: `type/short-description`, lowercase and hyphenated.

## Shared skills

Reusable playbooks live under `.ai/skills/` and are shared between agents through symlinks:
`.claude/skills/` and `.codex/skills/` both point there. Add new skills under
`.ai/skills/<name>/SKILL.md`, never inside one agent's folder.

## Validation

Run from the repo root with JDK 21. Checks are split by cost: cheap, targeted checks run locally
before every push, and the slow full set runs only in CI, which is the gate.

In the Symphony agent sandbox no JDK is on `PATH`: set
`JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home`. Robolectric tests need
nothing else; the build reads Robolectric's runtime from the Gradle cache and keeps temp files under
`build/tmp`. A Kotlin compile or Android Gradle plugin task can still fail on
`/var/folders/…: Operation not permitted` until the operator gives the Gradle daemon a writable temp
dir (`Agent sandbox` in `README.md`); until then, run Gradle with
`JAVA_TOOL_OPTIONS="$JAVA_TOOL_OPTIONS -Djava.io.tmpdir=$TMPDIR"` and say so in the workpad.

Before a push, run `ktlintCheck` and the unit tests and screenshot checks of each module the change
touches, plus every module that depends on it: those render the changed code too, so their
screenshots change with it. The map follows the `projects.*` dependencies in each module's
`build.gradle.kts`; update it when a module is added:

- `:core:designsystem`: also `:core:ui`, `:feature:calendar`, `:feature:history`,
  `:feature:onboarding`, `:feature:settings`, `:feature:today`, `:app-catalog` and `:app`. The
  catalog renders every component and `app` renders the theme.
- `:core:ui`: also `:feature:calendar`, `:feature:history`, `:feature:onboarding`,
  `:feature:settings`, `:feature:today` and `:app`.
- `:core:model`: also `:core:domain`, `:core:data`, `:core:ui`, `:feature:calendar`,
  `:feature:history`, `:feature:onboarding`, `:feature:settings`, `:feature:today` and `:app`.
- `:core:domain`: also `:core:data`, `:core:ui`, `:feature:calendar`, `:feature:history`,
  `:feature:onboarding`, `:feature:settings`, `:feature:today` and `:app`.
- `:core:data`: also `:feature:calendar`, `:feature:history`, `:feature:onboarding`,
  `:feature:settings`, `:feature:today` and `:app`.
- `:feature:calendar`, `:feature:history`, `:feature:onboarding`, `:feature:settings`,
  `:feature:today`: also `:app`.
- `:app-catalog`, `:app`: nothing depends on them.

`verifyRoborazziDebug` runs a module's unit tests and verifies its screenshots. `:core:model`,
`:core:domain` and `:core:data` have no screenshots: their check is `testDebugUnitTest` (in the two
pure Kotlin modules it runs `test`). Each takes seconds:

```sh
./gradlew ktlintCheck
# a core:designsystem change, with the modules that depend on it:
./gradlew :core:designsystem:verifyRoborazziDebug :core:ui:testDebugUnitTest \
  :app-catalog:verifyRoborazziDebug :app:verifyRoborazziDebug
# a core:model change, with the modules that depend on it:
./gradlew :core:model:testDebugUnitTest :core:domain:testDebugUnitTest :core:data:testDebugUnitTest
# in the touched module, only the changed test classes, screenshots still verified
# (the dependent modules still run their full check):
./gradlew :core:designsystem:testDebugUnitTest --tests '*CycleTextFieldTest' -Proborazzi.test.verify=true
```

After an intended UI change, re-record the screenshots of the same modules, not the whole build, and
commit the new images:

```sh
./gradlew :core:designsystem:recordRoborazziDebug :app-catalog:recordRoborazziDebug \
  :app:recordRoborazziDebug
```

A versioned git hook, `.githooks/pre-push`, runs `./gradlew ktlintCheck` before every push that
changes Kotlin or Gradle files, and skips it otherwise. Symphony turns it on in each new workspace
(`after_create` in `WORKFLOW.md`); in your own checkout, run `git config core.hooksPath .githooks`
once. Never push with `--no-verify`: when the hook fails, run `./gradlew ktlintFormat`, commit and
push again. A change to the hook runs `.githooks/pre-push_test.sh`, which CI runs too.

A change under `scripts/release/` runs its tests, `scripts/release/changelog_test.sh` and
`scripts/release/linear_update_test.sh`, which CI runs too. A change to a workflow under
`.github/workflows/` is checked with `actionlint`.

CI only: the `build` job in `.github/workflows/ci.yml` runs the full set on every PR and on `main`,
and a red check goes through Symphony's CI-fix flow:

```sh
./gradlew ktlintCheck lint testDebugUnitTest verifyRoborazziDebug assembleDebug
```

The full set stays available as an optional local run for changes that reach every module, such as
`build-logic`, `gradle/libs.versions.toml` or an API change in a shared `core` module. Say why in the
workpad when you run it.

- `ktlintCheck`: formatting, including `build-logic`. `./gradlew ktlintFormat` fixes most issues.
- `lint`: Android Lint. Errors fail the build.
- `testDebugUnitTest`: JUnit and Robolectric tests, all on the JVM.
- `verifyRoborazziDebug`: compares screenshots with the references in each module's
  `src/test/screenshots/`. After an intended UI change, run `recordRoborazziDebug` for the touched
  modules and the modules that depend on them (see above) and commit the new images.
- `assembleDebug`: builds `app` and `app-catalog`. Every build also runs
  `checkNoMaterialDependencies`, which fails if `androidx.compose.material` or `material3` reaches a
  classpath, even transitively. UI is built on Compose Foundation and `core:designsystem`.
