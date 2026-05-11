#!/usr/bin/env bash
set -euo pipefail

usage() {
    echo "사용법: $0 [--type feat|fix|docs|test|ci|chore|refactor] <issue-number> <short-slug>" >&2
    echo "예시: $0 16 account-role-model" >&2
    echo "예시: $0 --type docs 16 update-readme" >&2
}

branch_type="feat"

while [[ $# -gt 0 ]]; do
    case "$1" in
        --type)
            if [[ $# -lt 2 ]]; then
                usage
                exit 1
            fi
            branch_type="$2"
            shift 2
            ;;
        --type=*)
            branch_type="${1#--type=}"
            shift
            ;;
        --help|-h)
            usage
            exit 0
            ;;
        *)
            break
            ;;
    esac
done

if [[ $# -lt 2 ]]; then
    usage
    exit 1
fi

issue_number="$1"
shift
slug_raw="$*"
slug="$(echo "$slug_raw" | tr '[:upper:]' '[:lower:]' | sed -E 's/[^a-z0-9]+/-/g; s/^-+|-+$//g')"
branch_name="${branch_type}/${issue_number}-${slug}"

repo_root="$(git rev-parse --show-toplevel)"
cd "$repo_root"
. "$repo_root/scripts/lib/git-refs.sh"

if [[ ! "$issue_number" =~ ^[0-9]+$ ]]; then
    echo "[중단] 이슈 번호는 숫자만 입력하세요: $issue_number" >&2
    exit 1
fi

if [[ ! "$branch_type" =~ ^(feat|fix|docs|test|ci|chore|refactor)$ ]]; then
    echo "[중단] 작업 브랜치 타입은 feat, fix, docs, test, ci, chore, refactor 중 하나여야 합니다: $branch_type" >&2
    exit 1
fi

if [[ -z "$slug" ]]; then
    echo "[중단] 작업 이름을 영문/숫자로 구분할 수 없습니다: $slug_raw" >&2
    exit 1
fi

if [[ -n "$(git status --porcelain)" ]]; then
    echo "[중단] 작업트리에 커밋되지 않은 변경이 있습니다." >&2
    echo "먼저 커밋하거나 stash 한 뒤 다시 실행하세요." >&2
    exit 1
fi

ensure_origin_fetch_ref main
ensure_origin_fetch_ref dev
git fetch origin --quiet

if ! git rev-parse --verify origin/main >/dev/null 2>&1 || ! git rev-parse --verify origin/dev >/dev/null 2>&1; then
    echo "[중단] origin/main 또는 origin/dev를 확인할 수 없습니다. 원격 저장소 상태를 확인하세요." >&2
    exit 1
fi

if ! git merge-base --is-ancestor origin/main origin/dev; then
    echo "[중단] origin/dev가 origin/main을 포함하지 않습니다." >&2
    echo "dev는 main보다 앞설 수 있지만, main의 최신 안정 기준선은 포함해야 합니다." >&2
    echo "팀장에게 main/dev 분기 상태 확인을 요청한 뒤 다시 시작하세요." >&2
    exit 1
fi

git switch dev >/dev/null 2>&1 || git switch -c dev --track origin/dev
git merge --ff-only origin/dev

git switch -c "$branch_name"

echo "[확인] 브랜치 생성: $branch_name"
cat <<'MSG'

이제 할 일:
1. 코드/문서를 수정합니다.
2. ./gradlew check
3. git add .
4. git commit
5. ./scripts/open-pr.sh
MSG
