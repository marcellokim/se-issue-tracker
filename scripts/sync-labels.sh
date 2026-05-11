#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$repo_root"
. "$repo_root/scripts/lib/python.sh"

labels_file="$repo_root/config/github/labels.json"
dry_run=false
repo=""

while [[ $# -gt 0 ]]; do
    case "$1" in
        --dry-run)
            dry_run=true
            shift
            ;;
        --repo)
            repo="$2"
            shift 2
            ;;
        *)
            echo "알 수 없는 옵션: $1"
            echo "사용법: $0 [--repo owner/name] [--dry-run]"
            exit 1
            ;;
    esac
done

if ! command -v gh >/dev/null 2>&1; then
    echo "GitHub CLI(gh)가 필요합니다."
    exit 1
fi

gh auth status >/dev/null 2>&1 || {
    echo "gh auth login 후 다시 실행하세요."
    exit 1
}

if [[ -z "$repo" ]]; then
    repo="$(gh repo view --json nameWithOwner -q .nameWithOwner 2>/dev/null || true)"
fi

if [[ -z "$repo" ]]; then
    echo "대상 저장소를 확인할 수 없습니다. --repo owner/name 형태로 지정하세요."
    exit 1
fi

python_executable="$(resolve_python_executable)"
"$python_executable" - "$labels_file" "$repo" "$dry_run" <<'PY'
from __future__ import annotations

import json
import subprocess
import sys
from pathlib import Path
from urllib.parse import quote

labels_path = Path(sys.argv[1])
repo = sys.argv[2]
dry_run = sys.argv[3].lower() == "true"

labels = json.loads(labels_path.read_text(encoding="utf-8"))
current_by_name = {}
if not dry_run:
    current = subprocess.run(
        ["gh", "api", f"repos/{repo}/labels?per_page=100"],
        text=True,
        capture_output=True,
        check=True,
    )
    current_by_name = {item["name"]: item for item in json.loads(current.stdout)}

for label in labels:
    name = label["name"]
    aliases = label.get("aliases", [])
    if dry_run:
        alias_text = f" aliases={', '.join(aliases)}" if aliases else ""
        print(f"[dry-run] {name}{alias_text}")
        continue

    source_name = name if name in current_by_name else next(
        (alias for alias in aliases if alias in current_by_name),
        None,
    )

    payload = []
    for key, value in label.items():
        if key in {"name", "aliases"}:
            continue
        payload.extend(["-f", f"{key}={value}"])

    if source_name:
        encoded = quote(source_name, safe="")
        subprocess.run(
            [
                "gh",
                "api",
                "--method",
                "PATCH",
                f"repos/{repo}/labels/{encoded}",
                "-f",
                f"new_name={name}",
                *payload,
            ],
            check=True,
            text=True,
            capture_output=True,
        )
        action = "갱신" if source_name == name else f"이름변경 {source_name} ->"
        print(f"[{action}] {name}")
        current_by_name.pop(source_name, None)
        current_by_name[name] = label
    else:
        payload = ["-f", f"name={name}", *payload]
        subprocess.run(
            ["gh", "api", "--method", "POST", f"repos/{repo}/labels", *payload],
            check=True,
            text=True,
            capture_output=True,
        )
        print(f"[생성] {name}")
        current_by_name[name] = label
PY
