#!/usr/bin/env bash
set -euo pipefail

repo_root="$(git rev-parse --show-toplevel)"
cd "$repo_root"

branch="$(git rev-parse --abbrev-ref HEAD)"

if [[ "$branch" == "main" || "$branch" == "dev" || ! "$branch" =~ ^(feature|docs|test|chore)/[a-z0-9._-]+$ ]]; then
    echo "[중단] PR을 올릴 수 있는 작업 브랜치가 아닙니다: $branch" >&2
    echo "feature/<issue>-<slug>, docs/<slug>, test/<slug>, chore/<slug> 브랜치에서 실행하세요." >&2
    exit 1
fi

if [[ -n "$(git status --porcelain)" ]]; then
    echo "[중단] 커밋되지 않은 변경이 있습니다." >&2
    echo "git add . && git commit 을 먼저 실행한 뒤 다시 시도하세요." >&2
    exit 1
fi

if ! command -v gh >/dev/null 2>&1; then
    echo "[중단] GitHub CLI(gh)가 필요합니다. 설치 후 gh auth login 을 실행하세요." >&2
    exit 1
fi

if ! gh auth status >/dev/null 2>&1; then
    echo "[중단] GitHub 로그인이 필요합니다: gh auth login" >&2
    exit 1
fi

git fetch origin dev --quiet

if ! git merge-base --is-ancestor origin/dev HEAD; then
    echo "[중단] 현재 브랜치가 최신 dev를 포함하지 않습니다." >&2
    echo "git fetch origin dev && git rebase origin/dev 후 다시 실행하세요." >&2
    exit 1
fi

echo "[1/3] 로컬 검증 실행"
./gradlew check

echo "[2/3] 원격 브랜치 push"
git push -u origin HEAD

if pr_url="$(gh pr view "$branch" --json url -q .url 2>/dev/null)"; then
    echo "[확인] 이미 열린 PR이 있습니다: $pr_url"
    exit 0
fi

slug="${branch#*/}"
issue_number=""
if [[ "$slug" =~ ^([0-9]+)- ]]; then
    issue_number="${BASH_REMATCH[1]}"
fi

title="${branch#*/}"
body="## Summary
- 작업 브랜치: \`$branch\`

## Verification
- \`./gradlew check\`"

if [[ -n "$issue_number" ]]; then
    body="$body

## Issue
- Related: #$issue_number"
fi

echo "[3/3] GitHub PR 생성"
# gh pr create
gh pr create --base dev --head "$branch" --title "$title" --body "$body"
