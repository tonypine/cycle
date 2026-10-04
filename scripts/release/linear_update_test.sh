#!/usr/bin/env bash
# Tests linear_update.sh with synthetic release data. The post cases run against a stub curl on
# PATH, so nothing reaches Linear.
#
# Usage: scripts/release/linear_update_test.sh

# The expected strings hold a literal $HOME, $input and backticks on purpose.
# shellcheck disable=SC2016
set -euo pipefail

script="$(cd "$(dirname "$0")" && pwd)/linear_update.sh"
work="$(mktemp -d "${TMPDIR:-/tmp}/linear-update-test.XXXXXX")"
trap 'rm -rf "$work"' EXIT

failures=0
pass() { echo "ok - $1"; }
fail() {
  echo "not ok - $1"
  failures=$((failures + 1))
}
check() {
  local name="$1"
  shift
  if "$@" >/dev/null; then pass "$name"; else fail "$name"; fi
}

tag="v0.1.42"
release_url="https://github.com/example/cycle/releases/tag/v0.1.42"
apk_url="https://github.com/example/cycle/releases/download/v0.1.42/cycle-v0.1.42.apk"
changelog="$work/changelog.md"
cat >"$changelog" <<'EOF'
## Features

- show "today" with a `CycleCard` (#7)
- keep a backslash \ and a $HOME literal (#8)

## Fixes

- don't crash on 'quotes' (#9)
EOF
args=("$tag" "$release_url" "$apk_url" "$changelog")

# Body ------------------------------------------------------------------------------------------

cat >"$work/expected.md" <<'EOF'
# Cycle v0.1.42

**[Download cycle-v0.1.42.apk](https://github.com/example/cycle/releases/download/v0.1.42/cycle-v0.1.42.apk)** · [GitHub Release](https://github.com/example/cycle/releases/tag/v0.1.42)

## Features

- show "today" with a `CycleCard` (#7)
- keep a backslash \ and a $HOME literal (#8)

## Fixes

- don't crash on 'quotes' (#9)
EOF
expected_body="$(cat "$work/expected.md")"
actual_body="$("$script" body "${args[@]}")"
if [ "$actual_body" = "$expected_body" ]; then
  pass "body has the version heading, the APK and release links, and the changelog as is"
else
  fail "body has the version heading, the APK and release links, and the changelog as is"
  diff <(printf '%s\n' "$expected_body") <(printf '%s\n' "$actual_body") | sed 's/^/    /' || true
fi

# Payload ---------------------------------------------------------------------------------------

export LINEAR_INITIATIVE_ID="00000000-0000-4000-8000-000000000000"
payload="$("$script" payload "${args[@]}")"

check "payload is valid JSON" jq --exit-status . <<<"$payload"
check "payload is one line: newlines in the body are escaped" [ "$(printf '%s\n' "$payload" | wc -l | tr -d ' ')" = 1 ]
check "payload escapes quotes and backslashes and keeps backticks" \
  grep --quiet --fixed-strings 'show \"today\" with a `CycleCard`' <<<"$payload"
check "payload escapes a backslash" grep --quiet --fixed-strings 'a backslash \\ and a $HOME literal' <<<"$payload"
check "payload body decodes back to the Markdown body" \
  [ "$(jq --raw-output .variables.input.body <<<"$payload")" = "$expected_body" ]
check "payload targets the initiative, on track" jq --exit-status \
  '.variables.input == {initiativeId: "00000000-0000-4000-8000-000000000000", body: .variables.input.body, health: "onTrack"}' \
  <<<"$payload"
check "payload calls initiativeUpdateCreate with an input variable" jq --exit-status \
  '.query | test("initiativeUpdateCreate\\(input: \\$input\\)")' <<<"$payload"

if (unset LINEAR_INITIATIVE_ID && "$script" payload "${args[@]}") >/dev/null 2>"$work/err"; then
  fail "payload fails without LINEAR_INITIATIVE_ID"
else
  check "payload fails without LINEAR_INITIATIVE_ID" grep --quiet 'LINEAR_INITIATIVE_ID is not set' "$work/err"
fi

# Post, against a stub curl ---------------------------------------------------------------------

# The stub records the headers and the request it gets, then answers with STUB_STATUS and
# STUB_RESPONSE, or exits STUB_EXIT like a curl that cannot connect.
mkdir "$work/bin"
cat >"$work/bin/curl" <<'EOF'
#!/usr/bin/env bash
set -euo pipefail
: >"$STUB_DIR/headers"
while [ "$#" -gt 0 ]; do
  case "$1" in
    --header)
      case "$2" in
        @*) cat "${2#@}" >>"$STUB_DIR/headers" ;;
        *) printf '%s\n' "$2" >>"$STUB_DIR/headers" ;;
      esac
      shift 2
      ;;
    --data-binary) cat >"$STUB_DIR/request"; shift 2 ;;
    --output) output="$2"; shift 2 ;;
    --write-out | --max-time) shift 2 ;;
    *) printf '%s\n' "$1" >"$STUB_DIR/url"; shift ;;
  esac
done
if [ "${STUB_EXIT:-0}" != 0 ]; then
  echo "curl: (6) Could not resolve host: api.linear.app" >&2
  exit "$STUB_EXIT"
fi
printf '%s' "$STUB_RESPONSE" >"$output"
printf '%s' "$STUB_STATUS"
EOF
chmod +x "$work/bin/curl"
export STUB_DIR="$work"
export LINEAR_RELEASE_API_KEY="synthetic-linear-key-for-tests"

# Runs post with the stub answering STATUS and RESPONSE; leaves the exit code in post_exit and the
# combined output in $work/out.
post() {
  rm -f "$work/request" "$work/headers" "$work/url"
  post_exit=0
  STUB_STATUS="$1" STUB_RESPONSE="$2" STUB_EXIT="${3:-0}" PATH="$work/bin:$PATH" \
    "$script" post "${args[@]}" >"$work/out" 2>&1 || post_exit=$?
}
key_not_printed() { ! grep --quiet --fixed-strings "$LINEAR_RELEASE_API_KEY" "$work/out"; }

post 200 '{"data":{"initiativeUpdateCreate":{"success":true,"initiativeUpdate":{"url":"https://linear.app/example/initiative/update/1"}}}}'
check "post succeeds" [ "$post_exit" = 0 ]
check "post prints the update URL" grep --quiet 'https://linear.app/example/initiative/update/1' "$work/out"
check "post sends the payload to the Linear API" [ "$(cat "$work/request")" = "$payload" ]
check "post calls https://api.linear.app/graphql" [ "$(cat "$work/url")" = "https://api.linear.app/graphql" ]
check "post sends the key in the Authorization header" \
  grep --quiet --line-regexp --fixed-strings "Authorization: $LINEAR_RELEASE_API_KEY" "$work/headers"
check "post never prints the key" key_not_printed

post 401 '{"errors":[{"message":"Authentication required, not authenticated"}]}'
check "post fails on an HTTP error" [ "$post_exit" = 1 ]
check "post names the HTTP status and the API error" \
  grep --quiet 'HTTP 401: Authentication required, not authenticated' "$work/out"
check "post never prints the key on an HTTP error" key_not_printed

post 502 '<html>Bad gateway</html>'
check "post fails on an HTTP error without JSON" [ "$post_exit" = 1 ]
check "post shows a non-JSON error body" grep --quiet 'HTTP 502: <html>Bad gateway</html>' "$work/out"

post 200 '{"data":null,"errors":[{"message":"Entity not found: Initiative"},{"message":"second"}]}'
check "post fails on GraphQL errors with HTTP 200" [ "$post_exit" = 1 ]
check "post shows every GraphQL error" grep --quiet 'returned an error: Entity not found: Initiative; second' "$work/out"

post 200 '{"data":{"initiativeUpdateCreate":{"success":false,"initiativeUpdate":null}}}'
check "post fails on success false" [ "$post_exit" = 1 ]
check "post says the update was not created" grep --quiet 'did not create the update (success: false)' "$work/out"

post "" "" 6
check "post fails when curl cannot connect" [ "$post_exit" = 1 ]
check "post says it could not reach Linear" grep --quiet 'could not reach the Linear API' "$work/out"

saved_key="$LINEAR_RELEASE_API_KEY"
LINEAR_RELEASE_API_KEY=""
post 200 '{}'
check "post fails without LINEAR_RELEASE_API_KEY" [ "$post_exit" = 1 ]
check "post names the missing secret" grep --quiet 'LINEAR_RELEASE_API_KEY is not set' "$work/out"
check "post does not call curl without a key" [ ! -e "$work/url" ]
LINEAR_RELEASE_API_KEY="$saved_key"

saved_initiative="$LINEAR_INITIATIVE_ID"
LINEAR_INITIATIVE_ID=""
post 200 '{}'
check "post fails without LINEAR_INITIATIVE_ID" [ "$post_exit" = 1 ]
check "post does not call curl without an initiative" [ ! -e "$work/url" ]
LINEAR_INITIATIVE_ID="$saved_initiative"

# Arguments -------------------------------------------------------------------------------------

if "$script" body "$tag" "$release_url" "$apk_url" "$work/missing.md" >/dev/null 2>&1; then
  fail "fails on a missing changelog file"
else
  pass "fails on a missing changelog file"
fi
if "$script" publish "${args[@]}" >/dev/null 2>&1; then
  fail "fails on an unknown command"
else
  pass "fails on an unknown command"
fi

if [ "$failures" -gt 0 ]; then
  echo "$failures linear update test(s) failed" >&2
  exit 1
fi
echo "all linear update tests passed"
