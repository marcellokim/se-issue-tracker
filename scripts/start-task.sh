#!/usr/bin/env bash
set -euo pipefail

if [[ $# -lt 2 ]]; then
    echo "Usage: $0 <issue-number> <short-slug>"
    exit 1
fi

issue_number="$1"
shift
slug_raw="$*"
slug="$(echo "$slug_raw" | tr '[:upper:]' '[:lower:]' | sed -E 's/[^a-z0-9]+/-/g; s/^-+|-+$//g')"
branch_name="feature/${issue_number}-${slug}"

repo_root="$(git rev-parse --show-toplevel)"
cd "$repo_root"

git fetch origin dev --quiet || true
git switch dev >/dev/null 2>&1 || git switch -c dev --track origin/dev
if git rev-parse --verify origin/dev >/dev/null 2>&1; then
    git pull --ff-only origin dev
fi
git switch -c "$branch_name"

echo "[ok] Created branch: $branch_name"
echo "다음 단계: 작업 후 push 하고 PR 생성"
