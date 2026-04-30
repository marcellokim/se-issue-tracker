#!/usr/bin/env bash
set -euo pipefail

if [[ $# -lt 2 ]]; then
    echo "사용법: $0 <issue-number> <short-slug>"
    echo "예시: $0 16 account-role-model"
    exit 1
fi

issue_number="$1"
shift
slug_raw="$*"
slug="$(echo "$slug_raw" | tr '[:upper:]' '[:lower:]' | sed -E 's/[^a-z0-9]+/-/g; s/^-+|-+$//g')"
branch_name="feature/${issue_number}-${slug}"

repo_root="$(git rev-parse --show-toplevel)"
cd "$repo_root"

if [[ ! "$issue_number" =~ ^[0-9]+$ ]]; then
    echo "[중단] 이슈 번호는 숫자만 입력하세요: $issue_number" >&2
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

git fetch origin main dev --quiet

if ! git rev-parse --verify origin/main >/dev/null 2>&1 || ! git rev-parse --verify origin/dev >/dev/null 2>&1; then
    echo "[중단] origin/main 또는 origin/dev를 확인할 수 없습니다. 원격 저장소 상태를 확인하세요." >&2
    exit 1
fi

if ! git diff --quiet origin/main origin/dev; then
    echo "[중단] origin/main과 origin/dev의 파일 내용이 다릅니다." >&2
    echo "팀장에게 dev 기준선 정렬을 요청한 뒤 다시 시작하세요." >&2
    exit 1
fi

git switch dev >/dev/null 2>&1 || git switch -c dev --track origin/dev
git pull --ff-only origin dev

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
