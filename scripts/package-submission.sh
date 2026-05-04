#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$repo_root"

team_number=""
output_dir="$repo_root/dist"
github_url="$(git remote get-url origin 2>/dev/null || echo 'REPO_URL_UPDATE_REQUIRED')"
project_url="${PROJECT_URL:-}"
skip_check=false
members=()

java_runtime_ready() {
    command -v java >/dev/null 2>&1 && java -version >/dev/null 2>&1
}

sanitize_member_name() {
    python3 - "$1" <<'PY'
import sys

name = sys.argv[1].replace(" ", "_")
safe = []
for ch in name:
    if ch.isalnum() or ch in {"_", "-"}:
        safe.append(ch)
print("".join(safe))
PY
}

require_tool() {
    if ! command -v "$1" >/dev/null 2>&1; then
        echo "필수 도구를 찾을 수 없습니다: $1" >&2
        exit 1
    fi
}

while [[ $# -gt 0 ]]; do
    case "$1" in
        --team-number)
            team_number="$2"
            shift 2
            ;;
        --member)
            members+=("$2")
            shift 2
            ;;
        --github-url)
            github_url="$2"
            shift 2
            ;;
        --project-url)
            project_url="$2"
            shift 2
            ;;
        --output-dir)
            output_dir="$2"
            shift 2
            ;;
        --skip-check)
            skip_check=true
            shift
            ;;
        *)
            echo "알 수 없는 옵션: $1"
            exit 1
            ;;
    esac
done

if [[ -z "$team_number" || ${#members[@]} -lt 2 ]]; then
    echo "사용법: $0 --team-number <번호> --member <팀원1> --member <팀원2> [--member <팀원3> ...] [--project-url <url>] [--skip-check]"
    exit 1
fi

require_tool rsync
require_tool zip
require_tool python3

if [[ -z "$project_url" ]] && command -v gh >/dev/null 2>&1 && gh auth status >/dev/null 2>&1; then
    repo_name="$(gh repo view --json nameWithOwner -q .nameWithOwner 2>/dev/null || true)"
    if [[ -n "$repo_name" ]]; then
        project_url="$(gh variable get PROJECT_URL --repo "$repo_name" --json value -q .value 2>/dev/null || true)"
    fi
fi

if [[ -z "$project_url" ]]; then
    project_url="PROJECT_URL_UPDATE_REQUIRED"
fi

if [[ "$output_dir" != /* ]]; then
    output_dir="$repo_root/$output_dir"
fi

output_dir_name="$(basename "$output_dir")"

if [[ "$skip_check" == false ]]; then
    if ! java_runtime_ready; then
        echo "Java가 없어 사전 검증을 수행할 수 없습니다. --skip-check 옵션을 사용하거나 JDK 21을 설치하세요."
        exit 1
    fi
    ./gradlew clean check
fi

safe_members=()
for member in "${members[@]}"; do
    safe_members+=("$(sanitize_member_name "$member")")
done

archive_name="${team_number}_$(IFS=_; echo "${safe_members[*]}")"
stage_root="$repo_root/build/submission/$archive_name"
rm -rf "$stage_root"
mkdir -p "$stage_root" "$output_dir"

rsync -a \
    --exclude '.git/' \
    --exclude '.gradle/' \
    --exclude 'build/' \
    --exclude '.[o]mx/' \
    --exclude 'dist/' \
    --exclude 'dist*/' \
    --exclude 'tmp/' \
    --exclude "$output_dir_name/" \
    --exclude 'SE_Term_Project_2026-1.pdf' \
    --exclude '*.zip' \
    "$repo_root/" "$stage_root/"

python3 - <<'PY2' "$repo_root/docs/templates/submission-readme.txt.template" "$stage_root/README.txt" "$team_number" "$github_url" "$project_url" "${members[*]}"
from pathlib import Path
import sys

template_path = Path(sys.argv[1])
out_path = Path(sys.argv[2])
team_number = sys.argv[3]
github_url = sys.argv[4]
project_url = sys.argv[5]
members = sys.argv[6]
text = template_path.read_text(encoding='utf-8')
text = text.replace('{{TEAM_NUMBER}}', team_number)
text = text.replace('{{GITHUB_URL}}', github_url)
text = text.replace('{{PROJECT_URL}}', project_url)
text = text.replace('{{MEMBERS}}', members)
out_path.write_text(text, encoding='utf-8')
PY2

(
    cd "$repo_root/build/submission"
    zip -qr "$output_dir/${archive_name}.zip" "$archive_name"
)

echo "[확인] 생성 완료: $output_dir/${archive_name}.zip"
