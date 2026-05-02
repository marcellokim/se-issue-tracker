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
      echo "알 수 없는 옵션: $arg" >&2
      echo "사용법: ./scripts/bootstrap-dev.sh [--with-github] [--skip-test]" >&2
      exit 1
      ;;
  esac
done

cd "$ROOT_DIR"

echo "[1/5] 필수 도구 확인"
for tool in git python3; do
  if ! command -v "$tool" >/dev/null 2>&1; then
    echo "필수 도구를 찾을 수 없습니다: $tool" >&2
    exit 1
  fi
  echo "  - $tool 확인"
done

if command -v gh >/dev/null 2>&1; then
  echo "  - gh 확인"
else
  echo "  - gh를 찾을 수 없습니다 (GitHub bootstrap은 사용할 수 없습니다)"
fi

if java_runtime_ready; then
  echo "  - 사용 가능한 Java 런타임 확인"
else
  echo "  - 사용 가능한 Java 런타임을 찾을 수 없습니다 (로컬 Gradle 검증은 건너뜁니다)"
fi

echo "[2/5] 로컬 git hook 설치"
"$ROOT_DIR/scripts/install-git-hooks.sh"

echo "[3/5] 커밋 메시지 템플릿 설정"
git config --local commit.template .gitmessage.txt
echo "  - 커밋 메시지 템플릿을 .gitmessage.txt로 설정했습니다"

echo "[4/5] 실행 권한 정리"
chmod +x gradlew scripts/*.sh .githooks/*

if [[ "$SKIP_TEST" -eq 0 ]] && java_runtime_ready; then
  echo "[5/5] 로컬 검증 실행"
  ./gradlew check
else
  echo "[5/5] 로컬 검증 건너뜀"
fi

if [[ "$RUN_GITHUB" -eq 1 ]]; then
  echo "[GitHub] GitHub 라벨/마일스톤/프로젝트 초기 설정"
  "$ROOT_DIR/scripts/bootstrap-github.sh" --create-project
fi

cat <<'MSG'

초기 세팅이 완료되었습니다.
다음 단계:
1. docs/team-setup-manual.md를 읽습니다.
2. 필요하면 ./scripts/bootstrap-github.sh --create-project 를 실행합니다.
3. GitHub에서 이슈를 만들거나 기존 이슈를 선택합니다.
4. dev 브랜치에서 feature/<issue>-<slug> 브랜치를 만들어 작업합니다.
5. PR을 열고 CI와 자동 라벨을 확인합니다.
MSG
