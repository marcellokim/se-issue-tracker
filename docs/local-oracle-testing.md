# 로컬 Oracle Docker 테스트

이 문서는 팀원이 같은 명령으로 Oracle repository 통합 테스트를 재현하고, GitHub Actions에서도 같은 계약으로 Oracle 테스트를 자동 실행하기 위한 runbook이다.

## 빠른 실행

macOS/Linux:

```bash
./gradlew oracleLocalTest --console=plain
```

Windows PowerShell:

```powershell
.\gradlew.bat oracleLocalTest --console=plain
```

`oracleLocalTest`는 Docker 컨테이너를 시작하고, readiness를 기다린 뒤, 앱 schema와 테스트 schema 계정을 bootstrap하고, Oracle background 작업 안정화 대기 후 `oracleLocalIntegrationTest`를 실행한다. 테스트 schema의 실제 table drop/schema/seed reset은 `OracleRepositoryIntegrationTest`가 `ITS_TEST_DB_*`로 연결한 schema에서 직접 수행한다.

## 기본 연결값

| 항목 | 기본값 |
| --- | --- |
| 컨테이너 | `se-issue-tracker-oracle` |
| 이미지 | `container-registry.oracle.com/database/free:23.26.0.0-lite` |
| 볼륨 | `se_issue_tracker_oracle_data` |
| 포트 | `1521` |
| Docker Free PDB | `FREEPDB1` |
| Docker JDBC URL | `jdbc:oracle:thin:@//localhost:1521/FREEPDB1` |
| 앱 schema 사용자 | `ITS_USER` |
| 앱 schema 비밀번호 | `ItsLocalDev2026!` |
| 테스트 schema 사용자 | `ITS_TEST_USER` |
| 테스트 schema 비밀번호 | `ItsTestLocal2026!` |
| SYS 비밀번호 | `OracleLocal2026!` |

`ITS_USER`와 `ITS_TEST_USER`는 Oracle schema 계정이다. 애플리케이션 로그인 계정이 아니다. seed된 앱 로그인 비밀번호는 [oracleDB-seed-password.md](oracleDB-seed-password.md)에 정리되어 있다.

최신 코드의 일반 Oracle XE 예시는 `XEPDB1`을 사용하지만, 이 Docker Free 경로는 `FREEPDB1`을 명시한다. 로컬 Docker task는 `ITS_DB_URL` 또는 `ITS_TEST_DB_URL`을 자동으로 `FREEPDB1` URL로 맞춘다.

## Gradle task 계약

| 목적 | 명령 | 동작 |
| --- | --- | --- |
| 상태 확인 | `./gradlew oracleLocalStatus` | Docker CLI, Compose, 컨테이너, 포트, JDBC URL 확인 |
| 컨테이너 시작 | `./gradlew oracleLocalStart` | Compose로 Oracle Free 컨테이너 시작 |
| readiness 대기 | `./gradlew oracleLocalWait` | Oracle 로그와 SQL 연결 준비 확인 |
| schema 계정 준비 | `./gradlew oracleLocalBootstrap` | 앱/test schema 사용자 생성 또는 unlock |
| 안정화 대기 | `./gradlew oracleLocalStabilize` | Oracle Free 첫 기동 직후 listener/background 작업 안정화 대기 |
| 통합 테스트 | `./gradlew oracleLocalTest` | TEST schema에서 Oracle repository 통합 테스트 실행 |
| 앱 DB 비파괴 초기화 | `./gradlew oracleLocalInitializeDatabase` | `ITS_USER` schema에 schema migration 실행, 최초 실행 때만 seed 삽입 |
| 앱 DB 고정 seed 리셋 | `./gradlew oracleLocalResetFixedSeed` | `ITS_USER` schema core table drop 후 schema/seed 재생성 |
| 컨테이너/볼륨 리셋 | `./gradlew oracleLocalReset` | Docker volume 삭제 후 새 컨테이너와 schema 계정 재생성 |
| 컨테이너 중지 | `./gradlew oracleLocalStop` | Compose down |

`oracleLocalReset`은 Docker 컨테이너와 volume을 지우는 명령이다. `oracleLocalResetFixedSeed`는 앱 schema의 table과 seed를 리셋하는 명령이다. 둘을 같은 의미로 쓰지 않는다.

## 실행 옵션

| 환경변수 | 기본값 | 설명 |
| --- | --- | --- |
| `ORACLE_LOCAL_PORT` | `1521` | host Oracle listener port |
| `ORACLE_LOCAL_PDB` | `FREEPDB1` | Docker Oracle PDB/service name |
| `ORACLE_LOCAL_IMAGE` | `container-registry.oracle.com/database/free:23.26.0.0-lite` | 실행할 Oracle Free image |
| `ORACLE_LOCAL_TIMEOUT` | `900` | readiness 전체 대기 시간(초) |
| `ORACLE_LOCAL_SQL_TIMEOUT` | `30` | readiness/bootstrap SQLPlus probe 1회 제한 시간(초) |
| `ORACLE_LOCAL_SETTLE_SECONDS` | `60` | bootstrap 후 통합 테스트 전 안정화 대기 시간(초) |
| `ORACLE_LOCAL_CONNECT_RETRIES` | `12` | Docker test task에서 Oracle listener transient 오류 연결 재시도 횟수 |
| `ORACLE_LOCAL_CONNECT_RETRY_DELAY_MS` | `5000` | 연결 재시도 간격(ms) |

Oracle Free는 로그에 ready가 찍힌 뒤에도 listener handler가 순간적으로 부족할 수 있다. Docker 경로는 `ORA-12514`, `ORA-12516`, `ORA-12519`, `ORA-12520` 연결 오류를 제한적으로 재시도한다. 그래도 재현되면 Docker 리소스를 늘리거나 `ORACLE_LOCAL_SETTLE_SECONDS`, `ORACLE_LOCAL_CONNECT_RETRIES`를 더 크게 잡는다.

## 앱 실행용 DB 준비

CLI demo 또는 UI를 Docker DB에 연결하려면 앱 schema를 준비하고 `ITS_DB_*`를 설정한다.

macOS/Linux:

```bash
./gradlew oracleLocalInitializeDatabase --console=plain
export ITS_DB_URL="jdbc:oracle:thin:@//localhost:1521/FREEPDB1"
export ITS_DB_USER="ITS_USER"
export ITS_DB_PASSWORD="ItsLocalDev2026!"
./gradlew run --args="--cli-demo"
./gradlew run --args="--login-check admin DemoLocalAdmin!"
```

Windows PowerShell:

```powershell
.\gradlew.bat oracleLocalInitializeDatabase --console=plain
$env:ITS_DB_URL="jdbc:oracle:thin:@//localhost:1521/FREEPDB1"
$env:ITS_DB_USER="ITS_USER"
$env:ITS_DB_PASSWORD="ItsLocalDev2026!"
.\gradlew.bat run --args="--cli-demo"
.\gradlew.bat run --args="--login-check admin DemoLocalAdmin!"
```

앱 시작 경로는 `DatabaseInitializer.initializeApplication()`을 호출하므로 기존 앱 데이터가 있으면 seed를 다시 넣지 않는다. 고정 demo seed로 되돌려야 할 때만 `oracleLocalResetFixedSeed`를 사용한다.

## CI 자동 실행

GitHub Actions의 `빌드와 테스트` workflow에는 별도 `Oracle 통합 테스트` job이 있다. 이 job은 표준 `빌드와 테스트` job이 통과한 뒤 다음 순서로 실행된다.

1. `container-registry.oracle.com/database/free:23.26.0.0-lite` 이미지를 사용한다.
2. `ORACLE_LOCAL_TIMEOUT=900`, `ORACLE_LOCAL_SQL_TIMEOUT=30`, `ORACLE_LOCAL_SETTLE_SECONDS=60`, `ORACLE_LOCAL_CONNECT_RETRIES=12`, `ORACLE_LOCAL_CONNECT_RETRY_DELAY_MS=5000`으로 `./gradlew oracleLocalTest --console=plain`을 실행한다.
3. 실패 시 Oracle 컨테이너 로그를 출력한다.
4. `oracleLocalIntegrationTest` 테스트 리포트를 artifact로 업로드한다.

기본 `./gradlew check --console=plain`은 Docker-free 경로를 유지한다. Oracle 환경변수가 없으면 `oracleIntegrationTest`는 skip된다. CI에서 Oracle 자동 실행이 필요한 경우에는 `oracleLocalTest`를 명시적으로 실행한다.

이번 경로는 Oracle integration test 자동 실행을 추가한다. JaCoCo/Sonar coverage report에 `oracleLocalIntegrationTest` 결과를 합산하는 정책은 변경하지 않는다. CI job은 실패 로그와 테스트 리포트 업로드 후 Oracle 컨테이너와 volume을 정리한다.

## Docker Desktop / Colima

이 저장소의 계약은 Docker Desktop이 아니라 Docker CLI와 Compose다. 팀원은 Docker Desktop, Colima, WSL Docker Desktop integration 중 하나를 사용할 수 있다.

macOS에서 Colima를 쓰는 경우:

```bash
colima start --cpu 4 --memory 8 --disk 80
./gradlew oracleLocalTest --console=plain
```

Docker context가 Colima로 잡히지 않는 환경이면 현재 shell에서 `DOCKER_HOST`를 명시한다.

```bash
export DOCKER_HOST="unix://$HOME/.colima/default/docker.sock"
./gradlew oracleLocalTest --console=plain
```

`docker info`가 `docker-credential-osxkeychain` 누락으로 실패하면 Docker credential helper 설정이 깨진 상태다. Docker Desktop/Colima 설정을 고치거나, 현재 shell에서 유효한 Docker config와 `DOCKER_HOST`를 명시한 뒤 다시 실행한다.

Apple Silicon + Colima에서 기본 이미지 pull 또는 실행이 실패하면 Oracle Database Free arm64 tag를 명시한다.

```bash
ORACLE_LOCAL_IMAGE=container-registry.oracle.com/database/free:23.26.0.0-lite-arm64 \
  ./gradlew oracleLocalTest --console=plain
```

## 포트와 이미지 override

로컬 1521 포트가 이미 사용 중이면 `ORACLE_LOCAL_PORT`를 지정한다.

```bash
ORACLE_LOCAL_PORT=1522 ./gradlew oracleLocalTest --console=plain
```

이 경우 Docker JDBC URL은 `jdbc:oracle:thin:@//localhost:1522/FREEPDB1`이다. 직접 `oracleIntegrationTest`를 실행할 때도 `ITS_TEST_DB_URL`을 같은 값으로 맞춘다.

이미지 tag 제공 상태나 플랫폼 지원이 바뀌어 pull이 실패하면 `ORACLE_LOCAL_IMAGE`로 override한다.

```bash
ORACLE_LOCAL_IMAGE=<image> ./gradlew oracleLocalTest --console=plain
```

## 장애 확인

상태 확인:

```bash
./gradlew oracleLocalStatus --console=plain
```

로그 follow:

```bash
./scripts/oracle-local.sh logs
```

Windows PowerShell:

```powershell
.\scripts\oracle-local.ps1 logs
```

이미지 pull 실패, Docker daemon 미실행, 포트 충돌이면 Docker 설정을 고친 뒤 `oracleLocalTest`를 다시 실행한다. schema/seed 상태가 꼬였다고 판단되면 앱 schema만 되돌릴 때는 `oracleLocalResetFixedSeed`, 컨테이너와 volume까지 새로 만들 때는 `oracleLocalReset`을 사용한다.
