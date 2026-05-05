#!/usr/bin/env bash
set -euo pipefail

repo_root="$(git rev-parse --show-toplevel)"
cd "$repo_root"
. "$repo_root/scripts/lib/git-refs.sh"

branch="$(git rev-parse --abbrev-ref HEAD)"

if [[ "$branch" == "main" || "$branch" == "dev" || ! "$branch" =~ ^(feature|docs|test|chore)/[0-9]+-[a-z0-9._-]+$ ]]; then
    echo "[중단] PR을 올릴 수 있는 작업 브랜치가 아닙니다: $branch" >&2
    echo "feature/<issue>-<slug>, docs/<issue>-<slug>, test/<issue>-<slug>, chore/<issue>-<slug> 브랜치에서 실행하세요." >&2
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

ensure_origin_fetch_ref dev
git fetch origin dev --quiet

if ! git merge-base --is-ancestor origin/dev HEAD; then
    echo "[중단] 현재 브랜치가 최신 dev를 포함하지 않습니다." >&2
    echo "git fetch origin +refs/heads/dev:refs/remotes/origin/dev && git rebase origin/dev 후 다시 실행하세요." >&2
    exit 1
fi

echo "[1/3] 로컬 검증 실행"
./gradlew check

echo "[2/3] 원격 브랜치 push"
git push -u origin HEAD

slug="${branch#*/}"
issue_number=""
if [[ "$slug" =~ ^([0-9]+)- ]]; then
    issue_number="${BASH_REMATCH[1]}"
fi

if [[ -z "$issue_number" ]]; then
    echo "[중단] 브랜치 이름에서 이슈 번호를 찾을 수 없습니다: $branch" >&2
    echo "예: feature/18-db-repository" >&2
    exit 1
fi

if ! issue_title="$(gh issue view "$issue_number" --json title -q .title 2>/dev/null)"; then
    echo "[중단] GitHub 이슈 #$issue_number 를 찾을 수 없습니다." >&2
    exit 1
fi

mark_issue_review() {
    local status_label
    while IFS= read -r status_label; do
        if [[ "$status_label" != "status:review" ]]; then
            gh issue edit "$issue_number" --remove-label "$status_label" >/dev/null
        fi
    done < <(gh issue view "$issue_number" --json labels -q '.labels[].name' | grep '^status:' || true)
    gh issue edit "$issue_number" --add-label status:review >/dev/null
}

sync_project_board() {
    if ! ./scripts/sync-project-board.sh --apply --quiet; then
        echo "[경고] 프로젝트 상태 자동 정렬에 실패했습니다. PROJECT_URL/ADD_TO_PROJECT_PAT 권한을 확인하세요." >&2
    fi
}

if pr_url="$(gh pr view "$branch" --json url -q .url 2>/dev/null)"; then
    echo "[확인] 이미 열린 PR이 있습니다: $pr_url"
    echo "[확인] 이슈 #$issue_number 상태 라벨을 review로 이동"
    mark_issue_review
    echo "[확인] GitHub 프로젝트 상태 정렬"
    sync_project_board
    exit 0
fi

title="${issue_title}"
body="## 요약
- 작업 브랜치: \`$branch\`
- 관련 이슈: #$issue_number

## 검증
- \`./gradlew check\`

## 관련 이슈
- Closes #$issue_number"

echo "[3/3] GitHub PR 생성"
pr_url="$(gh pr create --base dev --head "$branch" --title "$title" --body "$body")"
echo "$pr_url"

echo "[확인] 이슈 #$issue_number 상태 라벨을 review로 이동"
mark_issue_review

echo "[확인] GitHub 프로젝트 상태 정렬"
sync_project_board
