#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
COMPOSE_FILE="$ROOT_DIR/infra/local-oracle/compose.yml"
BOOTSTRAP_SQL="$ROOT_DIR/infra/local-oracle/bootstrap.sql"

ORACLE_LOCAL_IMAGE="${ORACLE_LOCAL_IMAGE:-container-registry.oracle.com/database/free:23.26.0.0-lite}"
ORACLE_LOCAL_CONTAINER="${ORACLE_LOCAL_CONTAINER:-se-issue-tracker-oracle}"
ORACLE_LOCAL_VOLUME="${ORACLE_LOCAL_VOLUME:-se_issue_tracker_oracle_data}"
ORACLE_LOCAL_PORT="${ORACLE_LOCAL_PORT:-1521}"
ORACLE_LOCAL_SYS_PASSWORD="${ORACLE_LOCAL_SYS_PASSWORD:-OracleLocal2026!}"
ORACLE_LOCAL_APP_USER="${ORACLE_LOCAL_APP_USER:-ITS_USER}"
ORACLE_LOCAL_APP_PASSWORD="${ORACLE_LOCAL_APP_PASSWORD:-ItsLocalDev2026!}"
ORACLE_LOCAL_TEST_USER="${ORACLE_LOCAL_TEST_USER:-ITS_TEST_USER}"
ORACLE_LOCAL_TEST_PASSWORD="${ORACLE_LOCAL_TEST_PASSWORD:-ItsTestLocal2026!}"
ORACLE_LOCAL_PDB="${ORACLE_LOCAL_PDB:-FREEPDB1}"
ORACLE_LOCAL_TIMEOUT="${ORACLE_LOCAL_TIMEOUT:-900}"
ORACLE_LOCAL_SQL_TIMEOUT="${ORACLE_LOCAL_SQL_TIMEOUT:-30}"
ORACLE_LOCAL_SETTLE_SECONDS="${ORACLE_LOCAL_SETTLE_SECONDS:-60}"

export ORACLE_LOCAL_IMAGE
export ORACLE_LOCAL_CONTAINER
export ORACLE_LOCAL_VOLUME
export ORACLE_LOCAL_PORT
export ORACLE_LOCAL_SYS_PASSWORD

jdbc_url() {
    echo "jdbc:oracle:thin:@//localhost:${ORACLE_LOCAL_PORT}/${ORACLE_LOCAL_PDB}"
}

compose() {
    docker compose -f "$COMPOSE_FILE" "$@"
}

require_docker() {
    if ! command -v docker >/dev/null 2>&1; then
        echo "[오류] docker CLI를 찾을 수 없습니다." >&2
        exit 1
    fi

    if ! docker info >/dev/null 2>&1; then
        echo "[오류] Docker 데몬에 연결할 수 없습니다." >&2
        exit 1
    fi

    if ! docker compose version >/dev/null 2>&1; then
        echo "[오류] docker compose를 사용할 수 없습니다." >&2
        exit 1
    fi

    if [[ ! -f "$COMPOSE_FILE" ]]; then
        echo "[오류] Compose 파일을 찾을 수 없습니다: $COMPOSE_FILE" >&2
        exit 1
    fi

}

require_bootstrap_sql() {
    if [[ ! -f "$BOOTSTRAP_SQL" ]]; then
        echo "[오류] bootstrap SQL을 찾을 수 없습니다: $BOOTSTRAP_SQL" >&2
        exit 1
    fi
}

start() {
    require_docker
    echo "[시작] 로컬 Oracle 컨테이너를 기동합니다: $ORACLE_LOCAL_CONTAINER"
    compose up -d
    echo "[정보] JDBC URL: $(jdbc_url)"
}

sqlplus_system() {
    docker exec -i "$ORACLE_LOCAL_CONTAINER" timeout "${ORACLE_LOCAL_SQL_TIMEOUT}s" sqlplus -s /nolog
}

system_connect_sql() {
    printf 'set define off\nconnect system/"%s"@//localhost:1521/%s\n' \
        "$ORACLE_LOCAL_SYS_PASSWORD" \
        "$ORACLE_LOCAL_PDB"
}

wait_for_ready() {
    require_docker
    echo "[대기] Oracle 준비 상태를 확인합니다. 제한 시간: ${ORACLE_LOCAL_TIMEOUT}초"

    local start_time
    local elapsed
    local log_ready=0
    local sql_ready=0
    start_time="$(date +%s)"

    while true; do
        elapsed="$(($(date +%s) - start_time))"
        if (( elapsed > ORACLE_LOCAL_TIMEOUT )); then
            echo "[오류] Oracle 준비 대기 시간이 초과되었습니다." >&2
            echo "로그 확인: ./scripts/oracle-local.sh logs" >&2
            exit 1
        fi

        if [[ "$(docker logs --tail 100 "$ORACLE_LOCAL_CONTAINER" 2>&1)" == *"DATABASE IS READY TO USE"* ]]; then
            log_ready=1
        fi

        if [[ "$log_ready" -eq 1 ]] && {
            system_connect_sql
            printf '%s\n' \
                "set heading off feedback off pagesize 0 verify off" \
                "select 1 from dual;" \
                "exit"
        } | sqlplus_system 2>/dev/null | grep -q '^[[:space:]]*1[[:space:]]*$'; then
            sql_ready=1
        fi

        if [[ "$log_ready" -eq 1 && "$sql_ready" -eq 1 ]]; then
            echo "[확인] Oracle 로그와 SQL 연결이 모두 준비되었습니다."
            return 0
        fi

        sleep 5
    done
}

bootstrap() {
    require_docker
    require_bootstrap_sql
    echo "[초기화] 애플리케이션/테스트 계정을 준비합니다."
    docker cp "$BOOTSTRAP_SQL" "$ORACLE_LOCAL_CONTAINER:/tmp/bootstrap.sql"
    {
        system_connect_sql
        printf '@/tmp/bootstrap.sql "%s" "%s" "%s" "%s"\n' \
            "$ORACLE_LOCAL_APP_USER" \
            "$ORACLE_LOCAL_APP_PASSWORD" \
            "$ORACLE_LOCAL_TEST_USER" \
            "$ORACLE_LOCAL_TEST_PASSWORD"
    } | sqlplus_system
    echo "[확인] 계정 준비가 완료되었습니다."
}

settle() {
    if [[ "$ORACLE_LOCAL_SETTLE_SECONDS" =~ ^[0-9]+$ ]] && (( ORACLE_LOCAL_SETTLE_SECONDS > 0 )); then
        echo "[대기] Oracle background 작업 안정화를 위해 ${ORACLE_LOCAL_SETTLE_SECONDS}초 대기합니다."
        sleep "$ORACLE_LOCAL_SETTLE_SECONDS"
    else
        echo "[대기] Oracle 안정화 대기를 건너뜁니다."
    fi
}

status() {
    echo "[상태] 로컬 Oracle 설정"
    if command -v docker >/dev/null 2>&1; then
        echo "  - Docker CLI: 사용 가능 ($(docker --version))"
    else
        echo "  - Docker CLI: 없음"
    fi

    if command -v docker >/dev/null 2>&1 && docker compose version >/dev/null 2>&1; then
        echo "  - Docker Compose: 사용 가능 ($(docker compose version --short))"
    else
        echo "  - Docker Compose: 없음"
    fi

    echo "  - 컨테이너: $ORACLE_LOCAL_CONTAINER"
    echo "  - 이미지: $ORACLE_LOCAL_IMAGE"
    echo "  - 볼륨: $ORACLE_LOCAL_VOLUME"
    echo "  - 포트: localhost:$ORACLE_LOCAL_PORT -> 1521"
    echo "  - JDBC URL: $(jdbc_url)"
    echo "  - 앱 사용자: $ORACLE_LOCAL_APP_USER"
    echo "  - 테스트 사용자: $ORACLE_LOCAL_TEST_USER"

    if command -v docker >/dev/null 2>&1 && docker ps -a --format '{{.Names}}' | grep -Fxq "$ORACLE_LOCAL_CONTAINER"; then
        echo "  - 컨테이너 상태: $(docker inspect -f '{{.State.Status}}' "$ORACLE_LOCAL_CONTAINER")"
    else
        echo "  - 컨테이너 상태: 생성되지 않음"
    fi

    cat <<'MSG'

다음 명령:
  ./gradlew oracleLocalTest --console=plain
  ./gradlew oracleLocalInitializeDatabase --console=plain
  ./gradlew oracleLocalResetFixedSeed --console=plain
  ./scripts/oracle-local.sh start
  ./scripts/oracle-local.sh wait
  ./scripts/oracle-local.sh bootstrap
  ./scripts/oracle-local.sh settle
  ./scripts/oracle-local.sh stop
  ./scripts/oracle-local.sh reset

로그 확인:
  ./scripts/oracle-local.sh logs
MSG
}

reset() {
    require_docker
    require_bootstrap_sql
    echo "[초기화] 컨테이너와 로컬 Oracle 볼륨을 삭제합니다: $ORACLE_LOCAL_VOLUME"
    compose down -v
    echo "[시작] 새 로컬 Oracle 컨테이너를 기동합니다: $ORACLE_LOCAL_CONTAINER"
    compose up -d
    wait_for_ready
    bootstrap
    settle
    echo "[확인] 로컬 Oracle 재생성이 완료되었습니다."
}

stop() {
    require_docker
    echo "[중지] 로컬 Oracle 컨테이너를 중지합니다."
    compose down
}

logs() {
    require_docker
    compose logs -f oracle-local
}

usage() {
    cat <<'MSG'
사용법: ./scripts/oracle-local.sh <command>

명령:
  start      컨테이너 기동
  wait       Oracle readiness 확인
  bootstrap  실행 중인 컨테이너에 앱/테스트 계정 준비
  settle     bootstrap 후 Oracle background 작업 안정화 대기
  status     현재 설정과 다음 명령 출력
  reset      컨테이너와 볼륨 삭제 후 계정 bootstrap
  stop       컨테이너 중지
  logs       Oracle 컨테이너 로그 팔로우
  help       도움말 출력
MSG
}

command_name="${1:-help}"
if [[ $# -gt 0 ]]; then
    shift
fi

if [[ $# -gt 0 ]]; then
    echo "[오류] 예상하지 못한 추가 인자: $*" >&2
    usage >&2
    exit 1
fi

case "$command_name" in
    start)
        start
        ;;
    wait)
        wait_for_ready
        ;;
    bootstrap)
        bootstrap
        ;;
    settle)
        settle
        ;;
    status)
        status
        ;;
    reset)
        reset
        ;;
    stop)
        stop
        ;;
    logs)
        logs
        ;;
    help|--help|-h)
        usage
        ;;
    *)
        echo "[오류] 알 수 없는 명령: $command_name" >&2
        usage >&2
        exit 1
        ;;
esac
