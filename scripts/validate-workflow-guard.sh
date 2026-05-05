#!/usr/bin/env bash
set -euo pipefail

actor="${GITHUB_ACTOR:-}"
event_name="${GITHUB_EVENT_NAME:-}"
base_ref="${GITHUB_BASE_REF:-}"
head_ref="${GITHUB_HEAD_REF:-}"
ref_name="${GITHUB_REF_NAME:-}"
changed_files_path="${CHANGED_FILES_PATH:-}"
bypass_users="${WORKFLOW_BYPASS_USERS:-}"

trim() {
    local value="$1"
    value="${value#"${value%%[![:space:]]*}"}"
    value="${value%"${value##*[![:space:]]}"}"
    printf '%s' "$value"
}

is_admin_bypass_actor() {
    local user
    IFS=',' read -r -a users <<< "$bypass_users"
    for user in "${users[@]}"; do
        user="$(trim "$user")"
        if [[ -n "$user" && "$actor" == "$user" ]]; then
            return 0
        fi
    done
    return 1
}

is_work_branch() {
    local branch="$1"
    [[ "$branch" =~ ^(feature|docs|test|chore)/[0-9]+-[a-z0-9._-]+$ ]]
}

is_guarded_file() {
    case "$1" in
        .github/workflows/workflow-guard.yml|\
        scripts/validate-workflow-guard.sh|\
        scripts/bootstrap-github.sh|\
        scripts/lib/bootstrap_github.py|\
        scripts/start-task.sh|\
        scripts/open-pr.sh|\
        .githooks/pre-commit|\
        .githooks/pre-push|\
        .github/CODEOWNERS)
            return 0
            ;;
        *)
            return 1
            ;;
    esac
}

fail() {
    echo "[워크플로우-보호] $*" >&2
    exit 1
}

check_guarded_file_changes() {
    local path

    if [[ -z "$changed_files_path" || ! -f "$changed_files_path" ]]; then
        return
    fi

    if is_admin_bypass_actor; then
        return
    fi

    while IFS= read -r path; do
        if is_guarded_file "$path"; then
            fail "관리자 외에는 워크플로우/브랜치 보호 우회 지점을 수정할 수 없습니다: $path"
        fi
    done < "$changed_files_path"
}

if [[ -z "$actor" ]]; then
    fail "GITHUB_ACTOR를 확인할 수 없습니다."
fi

check_guarded_file_changes

case "$event_name" in
    pull_request|pull_request_target)
        if [[ "$base_ref" == "main" ]]; then
            if is_admin_bypass_actor; then
                echo "[워크플로우-보호] 관리자 우회 PR 허용: $actor -> main"
                exit 0
            fi
            fail "일반 작업 PR은 main이 아니라 dev로 올려야 합니다."
        fi

        if [[ "$base_ref" != "dev" ]]; then
            fail "PR 대상 브랜치는 dev여야 합니다. 현재 대상: ${base_ref:-unknown}"
        fi

        if [[ "$head_ref" == "main" || "$head_ref" == "dev" ]]; then
            fail "main/dev 브랜치를 head로 PR을 만들 수 없습니다. start-task.sh로 작업 브랜치를 만드세요."
        fi

        if ! is_work_branch "$head_ref"; then
            fail "작업 브랜치 형식은 feature|docs|test|chore/<issue>-<slug> 여야 합니다: ${head_ref:-unknown}"
        fi

        echo "[워크플로우-보호] dev 대상 작업 PR 허용: $head_ref"
        ;;
    push)
        if [[ "$ref_name" == "main" || "$ref_name" == "dev" ]]; then
            if is_admin_bypass_actor; then
                echo "[워크플로우-보호] 관리자 보호 브랜치 push 감지: $actor -> $ref_name"
                exit 0
            fi
            fail "main/dev 직접 push는 금지입니다. 작업 브랜치에서 dev 대상 PR을 사용하세요."
        fi
        echo "[워크플로우-보호] 보호 브랜치 push가 아닙니다: ${ref_name:-unknown}"
        ;;
    workflow_dispatch)
        if is_admin_bypass_actor; then
            echo "[워크플로우-보호] 관리자 수동 실행 허용: $actor"
            exit 0
        fi
        fail "워크플로우 보호 수동 실행은 관리자만 허용합니다."
        ;;
    *)
        fail "지원하지 않는 이벤트입니다: ${event_name:-unknown}"
        ;;
esac
