#!/usr/bin/env bash
set -euo pipefail

repo_root="$(git rev-parse --show-toplevel)"
cd "$repo_root"
. "$repo_root/scripts/lib/python.sh"

python_executable="$(resolve_python_executable)"
exec "$python_executable" scripts/lib/project_maintenance.py sync-project "$@"
