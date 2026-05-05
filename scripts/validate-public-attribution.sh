#!/usr/bin/env bash
set -euo pipefail

usage() {
    echo "사용법: $0 --commit-msg <file> | --staged | --all" >&2
}

fail_hit() {
    local target="$1"
    local line="$2"
    echo "[public-attribution] 공개 이력에 남기면 안 되는 명시적 AI attribution 문구가 감지되었습니다: ${target}:${line}" >&2
    exit 1
}

scan_path() {
    local path="$1"
    local line_number=0
    local line

    [[ -f "$path" ]] || return 0
    grep -Iq . "$path" || return 0

    while IFS= read -r line || [[ -n "$line" ]]; do
        line_number=$((line_number + 1))
        if [[ "$line" =~ ^[[:space:]]*([Cc]o-[Aa]uthored-[Bb]y|[Gg]enerated-[Bb]y)[[:space:]]*: ]]; then
            fail_hit "$path" "$line_number"
        fi

        if [[ "$line" =~ ([Gg]enerated|[Cc]reated|[Ww]ritten|[Aa]uthored|[Aa]ssisted)[[:space:]]+(by|BY|By|with|WITH|With|using|USING|Using)[[:space:]]+(ChatGPT|chatgpt|Codex|codex|OpenAI|openai|Claude|claude|Anthropic|anthropic|OMX|omx|oh-my-codex|Oh-my-codex|OH-MY-CODEX) ]]; then
            fail_hit "$path" "$line_number"
        fi
    done < "$path"
}

scan_staged() {
    local path

    while IFS= read -r -d '' path; do
        scan_path "$path"
    done < <(git diff --cached --name-only -z --diff-filter=ACMR)
}

scan_all_tracked() {
    local path

    while IFS= read -r -d '' path; do
        scan_path "$path"
    done < <(git ls-files -z)
}

if [[ $# -lt 1 ]]; then
    usage
    exit 2
fi

case "$1" in
    --commit-msg)
        if [[ $# -ne 2 ]]; then
            usage
            exit 2
        fi
        scan_path "$2"
        ;;
    --staged)
        scan_staged
        ;;
    --all)
        scan_all_tracked
        ;;
    *)
        usage
        exit 2
        ;;
esac
