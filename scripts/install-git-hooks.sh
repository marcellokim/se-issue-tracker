#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$repo_root"

git config --local core.hooksPath .githooks
chmod +x .githooks/pre-commit .githooks/pre-push

echo "[ok] Git hooks path configured: .githooks"
echo "[ok] pre-commit / pre-push hooks are executable"
