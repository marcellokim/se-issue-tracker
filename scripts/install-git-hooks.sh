#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$repo_root"

git config --local core.hooksPath .githooks
chmod +x .githooks/pre-commit .githooks/pre-push

echo "[확인] Git hooks 경로를 .githooks로 설정했습니다"
echo "[확인] pre-commit / pre-push hook에 실행 권한을 부여했습니다"
