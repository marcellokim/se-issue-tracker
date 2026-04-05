#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
RUN_GITHUB=0
SKIP_TEST=0

java_runtime_ready() {
  command -v java >/dev/null 2>&1 && java -version >/dev/null 2>&1
}

for arg in "$@"; do
  case "$arg" in
    --with-github)
      RUN_GITHUB=1
      ;;
    --skip-test)
      SKIP_TEST=1
      ;;
    *)
      echo "Unknown option: $arg" >&2
      echo "Usage: ./scripts/bootstrap-dev.sh [--with-github] [--skip-test]" >&2
      exit 1
      ;;
  esac
done

cd "$ROOT_DIR"

echo "[1/5] Checking required tools"
for tool in git python3; do
  if ! command -v "$tool" >/dev/null 2>&1; then
    echo "Missing required tool: $tool" >&2
    exit 1
  fi
  echo "  - found $tool"
done

if command -v gh >/dev/null 2>&1; then
  echo "  - found gh"
else
  echo "  - gh not found (GitHub bootstrap will be unavailable)"
fi

if java_runtime_ready; then
  echo "  - found a working java runtime"
else
  echo "  - usable java runtime not found (local Gradle verification will be skipped)"
fi

echo "[2/5] Installing local git hooks"
"$ROOT_DIR/scripts/install-git-hooks.sh"

echo "[3/5] Configuring commit template"
git config --local commit.template .gitmessage.txt
echo "  - commit template set to .gitmessage.txt"

echo "[4/5] Normalizing executable bits"
chmod +x gradlew scripts/*.sh .githooks/*

if [[ "$SKIP_TEST" -eq 0 ]] && java_runtime_ready; then
  echo "[5/5] Running local verification"
  ./gradlew check
else
  echo "[5/5] Skipping local verification"
fi

if [[ "$RUN_GITHUB" -eq 1 ]]; then
  echo "[GitHub] Bootstrapping GitHub labels/milestones/project"
  "$ROOT_DIR/scripts/bootstrap-github.sh" --create-project
fi

cat <<'MSG'

Bootstrap complete.
Next steps:
1. Read docs/team-setup-manual.md
2. If needed, run ./scripts/bootstrap-github.sh --create-project
3. Create or pick an issue from GitHub
4. Start work from dev -> feature/<issue>-<slug> branch
5. Open a PR and confirm CI + auto labels
MSG
