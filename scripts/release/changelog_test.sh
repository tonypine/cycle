#!/usr/bin/env bash
# Tests changelog.sh against a throwaway git repository in $TMPDIR with synthetic commits.
#
# Usage: scripts/release/changelog_test.sh
set -euo pipefail

changelog="$(cd "$(dirname "$0")" && pwd)/changelog.sh"
repo="$(mktemp -d "${TMPDIR:-/tmp}/changelog-test.XXXXXX")"
trap 'rm -rf "$repo"' EXIT

cd "$repo"
git init --quiet --initial-branch=main
git config user.name "Changelog Test"
git config user.email "changelog-test@example.com"
git config commit.gpgsign false
git config tag.gpgsign false

commit() {
  git commit --quiet --allow-empty --message "$1"
}

failures=0
expect() {
  local name="$1" expected="$2" actual
  actual="$("$changelog" "${@:3}")"
  if [ "$actual" = "$expected" ]; then
    echo "ok - $name"
  else
    echo "not ok - $name"
    diff <(printf '%s\n' "$expected") <(printf '%s\n' "$actual") | sed 's/^/    /' || true
    failures=$((failures + 1))
  fi
}

commit "chore: initial commit"
commit "feat: log a period start date (#1)"
commit "docs: describe the release flow"
commit "fix: keep the selected day after rotation (#2)"
commit "Tidy up without a type"
commit "feat(calendar)!: start weeks on monday (#3)"

expect "no previous tag lists every commit, grouped by type" "$(cat <<'EOF'
## Features

- calendar: start weeks on monday (#3)
- log a period start date (#1)

## Fixes

- keep the selected day after rotation (#2)

## Other changes

- Tidy up without a type
- docs: describe the release flow
- chore: initial commit
EOF
)"

git tag v0.1.6
commit "fix: show the right month after a restart (#4)"
git checkout --quiet -b side
commit "test: cover the month label"
git checkout --quiet main
commit "feat: add a symptom note (#5)"
git merge --quiet --no-ff --no-edit side

expect "lists only commits since the previous tag, without merge commits" "$(cat <<'EOF'
## Features

- add a symptom note (#5)

## Fixes

- show the right month after a restart (#4)

## Other changes

- test: cover the month label
EOF
)"

git tag v0.1.10
commit "refactor: split the calendar state (#6)"

expect "omits empty groups" "$(cat <<'EOF'
## Other changes

- refactor: split the calendar state (#6)
EOF
)"

git tag v0.1.11

expect "a commit that carries its own tag compares against the tag before it" "$(cat <<'EOF'
## Other changes

- refactor: split the calendar state (#6)
EOF
)"

expect "takes an older ref" "$(cat <<'EOF'
## Features

- add a symptom note (#5)

## Fixes

- show the right month after a restart (#4)

## Other changes

- test: cover the month label
EOF
)" v0.1.10

if "$changelog" no-such-ref >/dev/null 2>&1; then
  echo "not ok - fails on an unknown ref"
  failures=$((failures + 1))
else
  echo "ok - fails on an unknown ref"
fi

if [ "$failures" -gt 0 ]; then
  echo "$failures changelog test(s) failed" >&2
  exit 1
fi
echo "all changelog tests passed"
