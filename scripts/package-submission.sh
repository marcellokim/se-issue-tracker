#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$repo_root"
. "$repo_root/scripts/lib/python.sh"

team_number=""
output_dir="$repo_root/dist"
github_url="$(git remote get-url origin 2>/dev/null || echo 'REPO_URL_UPDATE_REQUIRED')"
project_url="${PROJECT_URL:-}"
skip_check=false
source_only=false
final_report=""
slides=""
demo_artifact=""
demo_link=""
members=()

java_runtime_ready() {
    command -v java >/dev/null 2>&1 && java -version >/dev/null 2>&1
}

sanitize_member_name() {
    "$python_executable" - "$1" <<'PY'
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

usage() {
    cat <<'EOF'
사용법:
  ./scripts/package-submission.sh \
    --team-number <번호> \
    --member <팀원1> --member <팀원2> [--member <팀원3> ...] \
    --final-report <보고서 PDF> \
    --slides <발표자료> \
    (--demo <시연 영상/링크 문서> | --demo-link <시연 영상 URL>) \
    [--project-url <url>] [--skip-check]

리허설용으로 소스/README 패키지만 만들 때는 최종 산출물 옵션 대신 --source-only를 사용합니다.
EOF
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
        --source-only)
            source_only=true
            shift
            ;;
        --final-report)
            final_report="$2"
            shift 2
            ;;
        --slides)
            slides="$2"
            shift 2
            ;;
        --demo)
            demo_artifact="$2"
            shift 2
            ;;
        --demo-link)
            demo_link="$2"
            shift 2
            ;;
        *)
            echo "알 수 없는 옵션: $1"
            usage
            exit 1
            ;;
    esac
done

if [[ -z "$team_number" || ${#members[@]} -lt 2 ]]; then
    usage
    exit 1
fi

if [[ -n "$demo_artifact" && -n "$demo_link" ]]; then
    echo "--demo와 --demo-link는 둘 중 하나만 지정하세요." >&2
    exit 1
fi

if [[ "$source_only" == false ]]; then
    missing=()
    [[ -z "$final_report" ]] && missing+=("--final-report")
    [[ -z "$slides" ]] && missing+=("--slides")
    [[ -z "$demo_artifact" && -z "$demo_link" ]] && missing+=("--demo 또는 --demo-link")
    if [[ ${#missing[@]} -gt 0 ]]; then
        echo "최종 제출 패키지는 다음 산출물 옵션이 필요합니다: ${missing[*]}" >&2
        usage
        exit 1
    fi
fi

require_tool rsync
python_executable="$(resolve_python_executable)"

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

resolve_artifact_path() {
    local path="$1"
    if [[ "$path" != /* ]]; then
        path="$repo_root/$path"
    fi
    if [[ ! -f "$path" ]]; then
        echo "최종 산출물 파일을 찾을 수 없습니다: $path" >&2
        exit 1
    fi
    printf '%s\n' "$path"
}

copy_artifact() {
    local source_path="$1"
    local target_dir="$2"
    local resolved_path
    resolved_path="$(resolve_artifact_path "$source_path")"
    mkdir -p "$stage_root/final-artifacts/$target_dir"
    cp "$resolved_path" "$stage_root/final-artifacts/$target_dir/"
}

rsync -a \
    --exclude '.git/' \
    --exclude '.gradle/' \
    --exclude '.gemini/' \
    --exclude 'build/' \
    --exclude '.[o]mx/' \
    --exclude 'dist/' \
    --exclude 'dist*/' \
    --exclude 'tmp/' \
    --exclude 'docs/qa/artifacts/' \
    --exclude '.idea/' \
    --exclude '.vscode/' \
    --exclude '.github/copilot-instructions.md' \
    --exclude '.pr_agent.toml' \
    --exclude '.DS_Store' \
    --exclude '__pycache__/' \
    --exclude 'docs/textbook/' \
    --exclude '*:Zone.Identifier' \
    --exclude 'AGENTS.md' \
    --exclude 'MEMORY.md' \
    --exclude 'memory.md' \
    --exclude "$output_dir_name/" \
    --exclude 'SE_Term_Project_2026-1.pdf' \
    --exclude '*.zip' \
    "$repo_root/" "$stage_root/"

"$python_executable" - <<'PY2' "$repo_root/docs/templates/submission-readme.txt.template" "$stage_root/README.txt" "$team_number" "$github_url" "$project_url" "${members[*]}"
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

if [[ -n "$final_report" ]]; then
    copy_artifact "$final_report" "report"
fi

if [[ -n "$slides" ]]; then
    copy_artifact "$slides" "slides"
fi

if [[ -n "$demo_artifact" ]]; then
    copy_artifact "$demo_artifact" "demo"
fi

if [[ -n "$demo_link" ]]; then
    mkdir -p "$stage_root/final-artifacts/demo"
    printf '%s\n' "$demo_link" > "$stage_root/final-artifacts/demo/demo-video-link.txt"
fi

"$python_executable" - <<'PY3' "$repo_root/build/submission" "$archive_name" "$output_dir/${archive_name}.zip"
from pathlib import Path
import sys
import zipfile

base_dir = Path(sys.argv[1])
archive_name = sys.argv[2]
output_path = Path(sys.argv[3])
source_root = base_dir / archive_name

with zipfile.ZipFile(output_path, "w", compression=zipfile.ZIP_DEFLATED) as archive:
    for path in sorted(source_root.rglob("*")):
        if path.is_file():
            archive.write(path, path.relative_to(base_dir))
PY3

echo "[확인] 생성 완료: $output_dir/${archive_name}.zip"
