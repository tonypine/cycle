#!/usr/bin/env bash
# Prints the release notes for REF (default HEAD) as Markdown: the commit subjects since the
# previous v* release tag, grouped into Features, Fixes and Other changes, newest first.
# With no previous tag, it lists every commit. See docs/decisions/0002-release-distribution.md.
#
# Usage: scripts/release/changelog.sh [ref]
#
# The previous tag is the nearest v* tag reachable from REF's parent, so re-running on a commit
# that already carries its own release tag still compares against the release before it.
set -euo pipefail

ref="${1:-HEAD}"
git rev-parse --verify --quiet "$ref^{commit}" >/dev/null || {
  echo "changelog: unknown ref '$ref'" >&2
  exit 1
}

previous_tag="$(git describe --tags --abbrev=0 --match 'v*' "$ref^" 2>/dev/null || true)"
range="$ref"
if [ -n "$previous_tag" ]; then
  range="$previous_tag..$ref"
fi

# type(scope)!: description, for the conventional-commit types in AGENTS.md and any other word.
conventional='^([a-z]+)(\(([^)]*)\))?!?: (.+)$'

features=""
fixes=""
others=""
while IFS= read -r subject; do
  [ -n "$subject" ] || continue
  if [[ "$subject" =~ $conventional ]]; then
    type="${BASH_REMATCH[1]}"
    scope="${BASH_REMATCH[3]}"
    description="${BASH_REMATCH[4]}"
    if [ -n "$scope" ]; then
      description="$scope: $description"
    fi
    case "$type" in
      feat) features+="- $description"$'\n' ;;
      fix) fixes+="- $description"$'\n' ;;
      *) others+="- $subject"$'\n' ;;
    esac
  else
    others+="- $subject"$'\n'
  fi
done < <(git log --no-merges --format='%s' "$range")

section() {
  [ -n "$2" ] || return 0
  [ -z "$printed" ] || echo
  echo "## $1"
  echo
  printf '%s' "$2"
  printed=1
}

printed=""
section "Features" "$features"
section "Fixes" "$fixes"
section "Other changes" "$others"
if [ -z "$printed" ]; then
  echo "No changes since ${previous_tag:-the first commit}."
fi
