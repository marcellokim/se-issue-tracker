#!/usr/bin/env bash
set -euo pipefail

repo_root="$(git rev-parse --show-toplevel)"
cd "$repo_root"
. "$repo_root/scripts/lib/git-refs.sh"

branch="$(git rev-parse --abbrev-ref HEAD)"

if [[ "$branch" == "main" || "$branch" == "dev" || ! "$branch" =~ ^(feat|fix|docs|test|ci|chore|refactor|feature)/[0-9]+-[A-Za-z0-9._-]+$ ]]; then
    echo "[중단] PR을 올릴 수 있는 작업 브랜치가 아닙니다: $branch" >&2
    echo "feat/<issue>-<slug>, fix/<issue>-<slug>, docs/<issue>-<slug>, test/<issue>-<slug>, ci/<issue>-<slug>, chore/<issue>-<slug>, refactor/<issue>-<slug> 브랜치에서 실행하세요." >&2
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

slug="${branch#*/}"
issue_number=""
if [[ "$slug" =~ ^([0-9]+)- ]]; then
    issue_number="${BASH_REMATCH[1]}"
fi

if [[ -z "$issue_number" ]]; then
    echo "[중단] 브랜치 이름에서 이슈 번호를 찾을 수 없습니다: $branch" >&2
    echo "예: feat/18-db-repository" >&2
    exit 1
fi

if ! issue_title="$(gh issue view "$issue_number" --json title -q .title 2>/dev/null)"; then
    echo "[중단] GitHub 이슈 #$issue_number 를 찾을 수 없습니다." >&2
    exit 1
fi

normalize_pr_summary() {
    local text="$1"

    is_known_title_prefix() {
        local normalized
        normalized="$(printf '%s' "$1" | tr '[:upper:]' '[:lower:]')"

        case "$normalized" in
            feat|feature|fix|docs|test|ci|chore|refactor|기능|문서|테스트|작업|수정|버그|확인)
                return 0
                ;;
            *)
                return 1
                ;;
        esac
    }

    while [[ "$text" =~ ^[[:space:]]*\[([^]]+)\][[:space:]]*(.*)$ ]] && is_known_title_prefix "${BASH_REMATCH[1]}"; do
        text="${BASH_REMATCH[2]}"
    done

    if [[ "$text" =~ ^[[:space:]]*([A-Za-z]+)(\([A-Za-z0-9._-]+\))?:[[:space:]]*(.+)$ ]] && is_known_title_prefix "${BASH_REMATCH[1]}"; then
        text="${BASH_REMATCH[3]}"
    fi

    if [[ -z "$text" ]]; then
        text="$1"
    fi

    printf '%s' "$text"
}

pr_type="${branch%%/*}"
if [[ "$pr_type" == "feature" ]]; then
    pr_type="feat"
fi

echo "[1/3] 로컬 검증 실행"
./gradlew check

echo "[2/3] 원격 브랜치 push"
git push -u origin HEAD

current_status_labels() {
    local labels
    if ! labels="$(gh issue view "$issue_number" --json labels -q '.labels[].name')"; then
        echo "[중단] 이슈 #$issue_number 상태 라벨 조회에 실패했습니다." >&2
        return 1
    fi
    grep '^status:' <<< "$labels" || true
}

restore_issue_status_labels() {
    local previous_status_labels="$1"
    local current_labels
    local status_label

    if ! current_labels="$(current_status_labels)"; then
        return 1
    fi

    while IFS= read -r status_label; do
        if [[ -n "$status_label" ]]; then
            gh issue edit "$issue_number" --remove-label "$status_label" >/dev/null
        fi
    done <<< "$current_labels"

    while IFS= read -r status_label; do
        if [[ -n "$status_label" ]]; then
            gh issue edit "$issue_number" --add-label "$status_label" >/dev/null
        fi
    done <<< "$previous_status_labels"
}

mark_issue_review() {
    local current_labels
    local status_label

    if ! current_labels="$(current_status_labels)"; then
        return 1
    fi

    while IFS= read -r status_label; do
        if [[ "$status_label" != "status:review" ]]; then
            gh issue edit "$issue_number" --remove-label "$status_label" >/dev/null
        fi
    done <<< "$current_labels"
    gh issue edit "$issue_number" --add-label status:review >/dev/null
}

sync_project_board() {
    if ! ./scripts/sync-project-board.sh --apply --quiet; then
        echo "[경고] 프로젝트 상태 자동 정렬에 실패했습니다. PROJECT_URL/ADD_TO_PROJECT_PAT 권한을 확인하세요." >&2
    fi
}

sync_pr_metadata() {
    local pr_number="$1"
    ./scripts/lib/project_maintenance.py sync-pr-metadata --pr "$pr_number" --owner @me --apply --skip-bots
}

rollback_precreate_status_on_exit() {
    local exit_code=$?

    if [[ "${precreate_status_needs_rollback:-false}" == "true" ]]; then
        echo "[복구] PR 생성 전 단계 실패로 이슈 #$issue_number 상태 라벨을 이전 상태로 복구합니다." >&2
        if ! restore_issue_status_labels "$previous_status_labels"; then
            echo "[경고] 이슈 #$issue_number 상태 라벨 복구에 실패했습니다. 수동 확인이 필요합니다." >&2
        fi
        echo "[복구] GitHub 프로젝트 상태를 이전 이슈 라벨 기준으로 다시 정렬합니다." >&2
        sync_project_board
    fi

    exit "$exit_code"
}

if pr_url="$(gh pr view "$branch" --json url -q .url 2>/dev/null)"; then
    echo "[확인] 이미 열린 PR이 있습니다: $pr_url"
    pr_number="${pr_url##*/}"
    echo "[확인] PR 메타데이터 정렬"
    sync_pr_metadata "$pr_number"
    echo "[확인] 이슈 #$issue_number 상태 라벨을 review로 이동"
    mark_issue_review
    echo "[확인] GitHub 프로젝트 상태 정렬"
    sync_project_board
    exit 0
fi

title="${pr_type}: $(normalize_pr_summary "$issue_title")"
body="## 요약
- 작업 브랜치: \`$branch\`
- 관련 이슈: #$issue_number

## 검증
- \`./gradlew check\`

## 관련 이슈
- Closes #$issue_number"

previous_status_labels="$(current_status_labels)"
precreate_status_needs_rollback=true
trap rollback_precreate_status_on_exit EXIT

echo "[준비] PR 생성 전 이슈 #$issue_number 상태 라벨을 review로 이동"
mark_issue_review

echo "[준비] PR 생성 전 GitHub 프로젝트 상태 정렬"
sync_project_board

echo "[3/3] GitHub PR 생성"
if ! pr_url="$(gh pr create --base dev --head "$branch" --title "$title" --body "$body")"; then
    exit 1
fi
precreate_status_needs_rollback=false
trap - EXIT
echo "$pr_url"
pr_number="${pr_url##*/}"

echo "[확인] PR 메타데이터 정렬"
sync_pr_metadata "$pr_number"
