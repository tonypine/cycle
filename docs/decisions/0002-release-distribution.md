# 0002: Release distribution

**Status:** accepted, 2026-10-04

## Context

Cycle has one user, who installs signed APKs from GitHub Releases ([`0001`](0001-stack.md)). Work
lands on `main` as squash-merged PRs, one ticket each, and she should get each change as soon as it
merges. Android only installs an APK over the current one when it is signed with the same key and
its `versionCode` is higher, so every release needs a new, larger `versionCode` and the one
permanent release key.

## Decisions

| Area | Decision |
| -- | -- |
| Cadence | Every merge to `main` ships a release. No manual release step. |
| Workflow | `.github/workflows/release.yml` runs on `workflow_run` of `CI` when CI succeeds for a push to `main`, and builds that run's `head_sha`. It reuses the CI gate instead of running the full check set a second time per merge. Pushes to `main` never cancel a running CI run, and the release job's `concurrency` group never cancels, so releases go out in merge order. The job's outputs (version, tag, release URL, APK URL, changelog) feed later jobs, because a release created with `GITHUB_TOKEN` does not trigger `on: release` workflows. |
| `versionCode` | The number of commits on `main`: `git rev-list --count HEAD`. With squash merges and no force pushes on `main`, it only ever increases. The release job checks out the full history (`fetch-depth: 0`) to count it. |
| `versionName` | `<major>.<minor>.<versionCode>`, e.g. `0.1.42`. `major.minor` lives in `app/build.gradle.kts` (`versionMajorMinor`, starts at `0.1`) and changes by hand when a release deserves it. |
| Build inputs | Gradle properties `cycle.versionCode` and `cycle.versionName`. Without `cycle.versionName`, Gradle derives it from `cycle.versionCode`. Without either, local and PR builds use `1` and `0.1.0`. |
| Tag | `v<versionName>`, e.g. `v0.1.42`, on the merge commit. |
| Where APKs live | A GitHub Release per tag, with the signed release APK attached. |
| Changelog | The release notes list the conventional-commit subjects (`feat: …`, `fix: …`) on `main` since the previous release tag, grouped into Features, Fixes and Other changes; one subject per merged PR. The first release lists every commit. `scripts/release/changelog.sh` builds them, and runs the same way locally. |
| Linear | Each release posts an update to the Linear initiative "Menstrual Cycle App" with the version, the changelog and a link to the GitHub Release. |
| Signing | The `release` build type signs with the keystore from the environment variables `RELEASE_KEYSTORE_PATH`, `RELEASE_KEYSTORE_PASSWORD`, `RELEASE_KEY_ALIAS` and `RELEASE_KEY_PASSWORD`. With none set, the release APK is unsigned. With only some set, the build fails, so a missing secret cannot ship an unsigned APK. Debug builds never use the release key. |
| Release key | Created once with `keytool -genkeypair -v -keystore cycle-release.jks -alias cycle -keyalg RSA -keysize 4096 -validity 10000` (Play requires a key valid until at least 2033). The `.jks` and its passwords go into the password manager before anything else. The same key becomes the Google Play app signing key through Play App Signing, so the sideloaded install keeps updating. Losing it means reinstalling the app. |
| Secrets | GitHub Actions secrets: `RELEASE_KEYSTORE_BASE64` (the `.jks`, base64-encoded; the job decodes it to a file under `$RUNNER_TEMP` and points `RELEASE_KEYSTORE_PATH` at it), `RELEASE_KEYSTORE_PASSWORD`, `RELEASE_KEY_ALIAS`, `RELEASE_KEY_PASSWORD`, and `LINEAR_API_KEY` for the initiative update. |

## Options considered

- **`versionCode` from the workflow run number.** Increases too, but resets when the workflow file is
  renamed or recreated, and ties the app's version to CI bookkeeping rather than to `main`.
- **`versionCode` from the date** (for example `yyMMddHH`). Two merges in the same hour collide, and
  the number says nothing about what changed.
- **A version bumped by hand in a file.** Needs a bump commit per release, which agents forget and
  parallel PRs conflict on.
- **Semantic versions computed from commit types** (semantic-release and similar). Adds tooling for
  one user and a pre-1.0 app. Commit count already gives a unique, ordered patch number.
- **Releases on a manual tag or a schedule.** Delays every change for no benefit while there is one
  user and every PR is reviewed before merge.
- **Signing inputs as Gradle properties.** Passwords on a command line show up in process listings,
  and a `gradle.properties` holding them is one commit away from leaking. Environment variables
  come straight from Actions secrets.
- **Signing in a `build-logic` convention.** Only `app` is released; `app-catalog` is not
  distributed. The logic stays in `app/build.gradle.kts` until a second app ships.

## Consequences

- `main` must keep squash merges and must never be force pushed or rewritten, or `versionCode` can
  go down and the next release will not install as an update.
- A merge that changes nothing in the app still ships a release with a new version. That is fine
  for one user and keeps the rule simple.
- Re-running the release job for the same commit produces the same version and tag, and keeps the
  release it already published. The job refuses a `versionCode` that is not above every existing
  release tag.
- GitHub keeps one running and one pending run per concurrency group. If a third merge lands while
  two are still queued, the pending one is replaced and that merge gets no release of its own; its
  changes appear in the next release's notes.
- The release key cannot be rotated without reinstalling. Its backup in the password manager is the
  only copy outside GitHub, and GitHub secrets cannot be read back.
