#!/usr/bin/env bash

resolve_python_executable() {
    local candidate

    if [[ -n "${PYTHON_EXECUTABLE:-}" ]]; then
        if command -v "$PYTHON_EXECUTABLE" >/dev/null 2>&1; then
            printf '%s\n' "$PYTHON_EXECUTABLE"
            return 0
        fi
        echo "[중단] PYTHON_EXECUTABLE을 찾을 수 없습니다: $PYTHON_EXECUTABLE" >&2
        return 1
    fi

    for candidate in python3 python py; do
        if command -v "$candidate" >/dev/null 2>&1; then
            printf '%s\n' "$candidate"
            return 0
        fi
    done

    echo "[중단] Python 실행 파일을 찾을 수 없습니다. python3, python, py 중 하나를 설치하세요." >&2
    return 1
}
