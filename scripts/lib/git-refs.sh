#!/usr/bin/env bash

ensure_origin_fetch_ref() {
    local branch="$1"
    local refspec="+refs/heads/${branch}:refs/remotes/origin/${branch}"

    if git config --get-all remote.origin.fetch | grep -Fx '+refs/heads/*:refs/remotes/origin/*' >/dev/null; then
        return
    fi

    if ! git config --get-all remote.origin.fetch | grep -Fx "$refspec" >/dev/null; then
        git config --add remote.origin.fetch "$refspec"
    fi
}
