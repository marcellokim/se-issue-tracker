#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"
. "$ROOT_DIR/scripts/lib/python.sh"

if ! command -v gh >/dev/null 2>&1; then
  echo "gh CLI가 필요합니다. https://cli.github.com/ 에서 설치하세요." >&2
  exit 1
fi

gh auth status -h github.com >/dev/null

python_executable="$(resolve_python_executable)"
"$python_executable" "$ROOT_DIR/scripts/lib/bootstrap_github.py" "$@"
