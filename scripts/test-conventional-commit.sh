#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
CHECK="$ROOT/scripts/conventional-commit.sh"

pass() {
  "$CHECK" --subject "$1"
}

fail() {
  if "$CHECK" --subject "$1" >/dev/null 2>&1; then
    echo "expected reject: $1" >&2
    exit 1
  fi
}

pass "feat: list GitHub issues for subscribed Android apps"
pass "fix(issue): keep comment field above the keyboard"
pass "feat(home)!: require a GitHub token"
pass "chore(release): 1.0.0"
pass "docs: add using guide"
pass "ci: enforce conventional commits"
pass "feat: x"
pass "ci(workflows): check PR titles"
pass "perf: debounce conversation polling"
pass "deps: bump okhttp"
pass "revert: undo broken message parse"
pass "Merge pull request #12 from rconnelly/feat-issues"
pass "Merge branch 'master' into feat-issues"
pass "Revert \"feat: list GitHub issues for subscribed Android apps\""

fail "Load GitHub issues and document the app."
fail "Feat: wrong case"
fail "feat:no-space"
fail "feat:"
fail "feat: "
fail "updated groups"
fail "chore(release) missing colon description"
fail "fix(): empty scope"

echo "conventional-commit checks passed"
