#!/usr/bin/env bash
# Posts a release's update to the Linear initiative "Menstrual Cycle App": a heading with the
# version, a direct APK download link, a link to the GitHub Release and the changelog. See
# docs/decisions/0002-release-distribution.md.
#
# Usage:
#   scripts/release/linear_update.sh body|payload|post <tag> <release-url> <apk-url> <changelog-file>
#
#   body     prints the update's Markdown
#   payload  prints the GraphQL request as JSON; needs LINEAR_INITIATIVE_ID
#   post     sends it to the Linear API and prints the update's URL; needs LINEAR_INITIATIVE_ID
#            and LINEAR_RELEASE_API_KEY. The key only goes into the Authorization header, which
#            curl reads from a file descriptor, so it is never on a command line or in the output.
#
# A failed call (no connection, an HTTP error, GraphQL errors or success false) exits 1 with the
# API's error message.
set -euo pipefail

linear_api_url="https://api.linear.app/graphql"

# shellcheck disable=SC2016 # $input is a GraphQL variable, not a shell one.
mutation='mutation ($input: InitiativeUpdateCreateInput!) {
  initiativeUpdateCreate(input: $input) {
    success
    initiativeUpdate { url }
  }
}'

fail() {
  echo "::error title=Linear update::$1" >&2
  exit 1
}

usage() {
  echo "Usage: $0 body|payload|post <tag> <release-url> <apk-url> <changelog-file>" >&2
  exit 2
}

[ "$#" -eq 5 ] || usage
command="$1"
tag="$2"
release_url="$3"
apk_url="$4"
changelog_file="$5"
[ -n "$tag" ] || fail "the release tag is empty"
[ -n "$release_url" ] || fail "the release URL is empty"
[ -n "$apk_url" ] || fail "the APK URL is empty"
[ -r "$changelog_file" ] || fail "cannot read the changelog file '$changelog_file'"

body() {
  printf '# Cycle %s\n\n' "$tag"
  printf '**[Download %s](%s)** · [GitHub Release](%s)\n\n' "${apk_url##*/}" "$apk_url" "$release_url"
  cat "$changelog_file"
}

payload() {
  [ -n "${LINEAR_INITIATIVE_ID:-}" ] ||
    fail "the repository variable LINEAR_INITIATIVE_ID is not set. Add it as described in README.md (Releases > One-time setup)."
  jq --null-input --compact-output \
    --arg query "$mutation" \
    --arg initiativeId "$LINEAR_INITIATIVE_ID" \
    --arg body "$(body)" \
    '{query: $query, variables: {input: {initiativeId: $initiativeId, body: $body, health: "onTrack"}}}'
}

post() {
  [ -n "${LINEAR_RELEASE_API_KEY:-}" ] ||
    fail "the repository secret LINEAR_RELEASE_API_KEY is not set. Add it as described in README.md (Releases > One-time setup)."
  local request status errors success url
  request="$(payload)"
  # Global, so the EXIT trap still sees it after post returns.
  response="$(mktemp "${TMPDIR:-/tmp}/linear-response.XXXXXX")"
  trap 'rm -f "$response"' EXIT

  if ! status="$(curl --silent --show-error --max-time 30 \
    --header 'Content-Type: application/json' \
    --header @<(printf 'Authorization: %s\n' "$LINEAR_RELEASE_API_KEY") \
    --data-binary @- \
    --output "$response" \
    --write-out '%{http_code}' \
    "$linear_api_url" <<<"$request")"; then
    fail "could not reach the Linear API"
  fi

  errors="$(jq --raw-output '[.errors[]?.message] | join("; ")' "$response" 2>/dev/null)" ||
    errors="$(head -c 500 "$response" | tr '\n' ' ')"
  case "$status" in
    2??) ;;
    *) fail "the Linear API returned HTTP $status: ${errors:-no error message}" ;;
  esac
  [ -z "$errors" ] || fail "the Linear API returned an error: $errors"

  success="$(jq --raw-output '.data.initiativeUpdateCreate.success' "$response")"
  [ "$success" = "true" ] || fail "Linear did not create the update (success: $success)"
  url="$(jq --raw-output '.data.initiativeUpdateCreate.initiativeUpdate.url // empty' "$response")"
  echo "Posted the Linear initiative update for $tag: ${url:-no URL returned}"
}

case "$command" in
  body | payload | post) "$command" ;;
  *) usage ;;
esac
