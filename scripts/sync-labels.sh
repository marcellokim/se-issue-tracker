#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$repo_root"

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
            echo "Unknown option: $1"
            echo "Usage: $0 [--repo owner/name] [--dry-run]"
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

python3 - "$labels_file" "$repo" "$dry_run" <<'PY'
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

for label in labels:
    name = label["name"]
    if dry_run:
        print(f"[dry-run] {name}")
        continue

    encoded = quote(name, safe="")
    probe = subprocess.run(
        ["gh", "api", f"repos/{repo}/labels/{encoded}"],
        text=True,
        capture_output=True,
    )

    payload = []
    for key, value in label.items():
        payload.extend(["-f", f"{key}={value}"])

    if probe.returncode == 0:
        subprocess.run(
            ["gh", "api", "--method", "PATCH", f"repos/{repo}/labels/{encoded}", *payload],
            check=True,
            text=True,
            capture_output=True,
        )
        print(f"[updated] {name}")
    else:
        subprocess.run(
            ["gh", "api", "--method", "POST", f"repos/{repo}/labels", *payload],
            check=True,
            text=True,
            capture_output=True,
        )
        print(f"[created] {name}")
PY
