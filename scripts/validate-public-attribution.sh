#!/usr/bin/env bash
set -euo pipefail

usage() {
    echo "사용법: $0 --commit-msg <file> | --staged | --all" >&2
}

blocked_terms() {
    printf '%s\n' \
        "$(printf 'Co-authored-%s:' by)" \
        "$(printf 'Generated-%s:' by)" \
        "$(printf 'O%sX' m)" \
        "$(printf 'oh-my-%s%s' co dex)" \
        "$(printf 'Co%s' dex)" \
        "$(printf 'Chat%s' GPT)" \
        "$(printf 'Open%s' "$(printf '\101\111')")" \
        "$(printf 'Clau%s' de)" \
        "$(printf 'Anthro%s' pic)"
}

fail_hit() {
    local target="$1"
    echo "[public-attribution] 공개 이력에 남기면 안 되는 도구/공동작성자 표기가 감지되었습니다: $target" >&2
    exit 1
}

scan_path() {
    local path="$1"
    local term

    [[ -f "$path" ]] || return 0
    grep -Iq . "$path" || return 0

    while IFS= read -r term; do
        if grep -IiqF "$term" "$path"; then
            fail_hit "$path"
        fi
    done < <(blocked_terms)
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
