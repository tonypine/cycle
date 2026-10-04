#!/usr/bin/env bash
# Tests the pre-push hook with real pushes from a throwaway git repository in $TMPDIR to a bare
# origin next to it. A fake ./gradlew records its arguments and plays ktlintCheck: it passes, or
# fails with FAKE_KTLINT=violation (style violations) or FAKE_KTLINT=broken (another error).
#
# Usage: .githooks/pre-push_test.sh
set -euo pipefail

hooks="$(cd "$(dirname "$0")" && pwd)"
tmp="$(mktemp -d "${TMPDIR:-/tmp}/pre-push-test.XXXXXX")"
trap 'rm -rf "$tmp"' EXIT

git init --quiet --bare --initial-branch=main "$tmp/origin.git"
git init --quiet --initial-branch=main "$tmp/repo"
cd "$tmp/repo"
repo="$(pwd)"
git config user.name "Pre-push Test"
git config user.email "pre-push-test@example.com"
git config commit.gpgsign false
git config core.hooksPath "$hooks"
git remote add origin "$tmp/origin.git"

cat >gradlew <<'EOF'
#!/usr/bin/env bash
echo "$*" >>"$(dirname "$0")/.gradlew-calls"
case "${FAKE_KTLINT:-pass}" in
  violation)
    echo "> Task :app:ktlintMainSourceSetCheck FAILED"
    echo "$(pwd)/app/src/main/kotlin/Sample.kt:3:5 Unnecessary long whitespace"
    echo "$(pwd)/app/src/main/kotlin/Sample.kt:3:17 Missing spacing around \"=\""
    # Replayed from the build cache, so under the root of another checkout.
    echo "/another/checkout/app/build.gradle.kts:1:1 Unexpected blank line(s) before \"plugins\""
    exit 1
    ;;
  broken)
    echo "Could not compile build-logic"
    exit 1
    ;;
esac
EOF
chmod +x gradlew
printf '%s\n' gradlew .gradlew-calls >.git/info/exclude

change() {
  mkdir -p "$(dirname "$1")"
  echo "$RANDOM" >>"$1"
  git add "$1"
  git commit --quiet --message "chore: change $1"
}

failures=0
fail() {
  echo "not ok - $1"
  sed 's/^/    /' "$tmp/out"
  failures=$((failures + 1))
}

# expect NAME RAN|SKIPPED PASS|FAIL GIT-PUSH-ARGS...
expect() {
  local name="$1" check="$2" result="$3" status=0
  rm -f .gradlew-calls
  git push "${@:4}" >"$tmp/out" 2>&1 || status=$?
  if [ "$check" = RAN ] && [ "$(cat .gradlew-calls 2>/dev/null)" != "--continue ktlintCheck" ]; then
    fail "$name: expected ./gradlew --continue ktlintCheck"
  elif [ "$check" = SKIPPED ] && [ -e .gradlew-calls ]; then
    fail "$name: expected no Gradle run"
  elif [ "$result" = PASS ] && [ "$status" -ne 0 ]; then
    fail "$name: expected the push to go through"
  elif [ "$result" = FAIL ] && [ "$status" -eq 0 ]; then
    fail "$name: expected the push to be rejected"
  else
    echo "ok - $name"
  fi
}

expect_output() {
  local name="$1" expected="$2"
  if grep -qF -- "$expected" "$tmp/out"; then
    echo "ok - $name"
  else
    fail "$name: expected output '$expected'"
  fi
}

change README.md
expect "a first push without origin/main runs the check" RAN PASS origin main

change docs/notes.md
expect "a Markdown-only push skips the check" SKIPPED PASS origin main
expect_output "a skipped push says so" "skipping ktlintCheck"

change app/src/main/kotlin/Sample.kt
expect "a Kotlin change runs the check" RAN PASS origin main

change app/build.gradle.kts
expect "a build script change runs the check" RAN PASS origin main

change build-logic/convention/gradle.properties
expect "a build-logic change runs the check" RAN PASS origin main

change .editorconfig
expect "an .editorconfig change runs the check" RAN PASS origin main

change gradle/libs.versions.toml
expect "a version catalog change runs the check" RAN PASS origin main

git switch --quiet --create docs/new-branch
change docs/branch.md
expect "a new branch is compared with its merge-base with origin/main" SKIPPED PASS origin docs/new-branch

git switch --quiet --create feat/new-kotlin main
change app/src/main/kotlin/Feature.kt
expect "a new branch with a Kotlin change runs the check" RAN PASS origin feat/new-kotlin

change app/src/main/kotlin/Sample.kt
export FAKE_KTLINT=violation
expect "a ktlint violation rejects the push" RAN FAIL origin feat/new-kotlin
expect_output "the rejection names the Kotlin file" "  app/src/main/kotlin/Sample.kt"
expect_output "the rejection names a file reported under another checkout's root" "  app/build.gradle.kts"
expect_output "the rejection shows each violation" "app/src/main/kotlin/Sample.kt:3:17 Missing spacing"
expect_output "the rejection suggests ktlintFormat" "Run ./gradlew ktlintFormat"
if grep -qF -e "$repo/" -e /another/checkout "$tmp/out"; then
  fail "the rejection prints paths relative to the repository root"
else
  echo "ok - the rejection prints paths relative to the repository root"
fi
if [ "$(git rev-parse origin/feat/new-kotlin)" = "$(git rev-parse feat/new-kotlin)" ]; then
  fail "a rejected push leaves the remote branch where it was"
else
  echo "ok - a rejected push leaves the remote branch where it was"
fi

export FAKE_KTLINT=broken
expect "a Gradle failure without violations rejects the push" RAN FAIL origin feat/new-kotlin
expect_output "the rejection shows Gradle's output" "Could not compile build-logic"

export FAKE_KTLINT=pass
expect "the push goes through once ktlint passes" RAN PASS origin feat/new-kotlin

git commit --quiet --amend --message "chore: reword the last commit"
FAKE_KTLINT=violation expect "a force push is compared with the replaced remote commit" SKIPPED PASS --force origin feat/new-kotlin

expect "deleting a remote branch skips the check" SKIPPED PASS origin --delete docs/new-branch

git tag v0.0.1 main
expect "a tag on pushed commits skips the check" SKIPPED PASS origin v0.0.1

if [ "$failures" -gt 0 ]; then
  echo "$failures failing"
  exit 1
fi
