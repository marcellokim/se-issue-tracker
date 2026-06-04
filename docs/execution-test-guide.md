# 실행 및 테스트 가이드

이 문서는 로컬 실행, JUnit 테스트, Oracle 통합 테스트, 데모 seed 계정 정보를 한 곳에서 확인하기 위한 가이드이다.

## 1. 기본 실행 환경

- Java 21 이상
- Gradle Wrapper 사용
- 기본 UI: JavaFX
- 보조 UI: Swing
- 기본 저장소: Oracle DB + JDBC Repository

JavaFX와 Swing은 화면 구현만 다르며, 로그인 이후의 Controller, Service, Domain, Repository/JDBC 계층은 공통으로 재사용한다.

## 2. Oracle 없이 가능한 빠른 검증

Oracle DB가 없는 환경에서도 컴파일과 대부분의 단위 테스트는 실행할 수 있다.

```powershell
.\gradlew.bat compileJava --console=plain
.\gradlew.bat compileTestJava --console=plain
.\gradlew.bat test --console=plain
.\gradlew.bat check --console=plain
```

특정 테스트 파일만 실행할 경우에는 다음처럼 `--tests`를 사용한다.

```powershell
.\gradlew.bat test --tests "*IssueWorkflowTest" --console=plain
.\gradlew.bat test --tests "*IssueServiceTest" --console=plain
.\gradlew.bat test --tests "*JdbcIssueQueriesTest" --console=plain
```

특정 테스트 메서드만 실행할 경우에는 클래스명과 메서드명을 함께 지정한다.

```powershell
.\gradlew.bat test --tests "*JdbcIssueQueriesTest.defaultSearchExcludesDeletedIssues" --console=plain --rerun-tasks
```

Oracle 환경 변수가 설정되어 있지 않으면 `oracleIntegrationTest`는 일반 `check` 과정에서 skip될 수 있다. 이 경우는 Oracle 통합 테스트를 실행하지 않았다는 의미이지, 일반 단위 테스트 실패를 의미하지 않는다.

## 3. Oracle DB 연결 실행

이미 로컬 Oracle XE 또는 Oracle Free DB가 준비되어 있다면 다음 환경 변수를 설정한 뒤 실행한다.

```powershell
$env:ITS_DB_URL="jdbc:oracle:thin:@//localhost:1521/XEPDB1"
$env:ITS_DB_USER="ITS_USER"
$env:ITS_DB_PASSWORD="ItsLocalDev2026!"

.\gradlew.bat oracleConnectionCheck --console=plain
.\gradlew.bat run --console=plain
```

고정 seed 상태로 DB를 초기화한 뒤 실행하려면 다음 명령을 사용한다.

```powershell
$env:ITS_DB_URL="jdbc:oracle:thin:@//localhost:1521/XEPDB1"
$env:ITS_DB_USER="ITS_USER"
$env:ITS_DB_PASSWORD="ItsLocalDev2026!"

.\gradlew.bat oracleConnectionCheck --console=plain
.\gradlew.bat oracleResetFixedSeed --console=plain
.\gradlew.bat run --console=plain
```

Swing UI는 같은 DB 환경 변수에서 다음 명령으로 실행한다.

```powershell
.\gradlew.bat runSwing --console=plain
```

## 4. Oracle Docker 로컬 테스트

Docker 기반 Oracle Free 환경을 사용할 경우 기본값은 다음과 같다.

| 항목 | 값 |
|---|---|
| container | `se-issue-tracker-oracle` |
| image | `container-registry.oracle.com/database/free:23.26.0.0-lite` |
| port | `1521` |
| PDB | `FREEPDB1` |
| app schema | `ITS_USER` / `ItsLocalDev2026!` |
| test schema | `ITS_TEST_USER` / `ItsTestLocal2026!` |

Docker Oracle을 시작하고 통합 테스트까지 실행하려면 다음 명령을 사용한다.

```powershell
.\gradlew.bat oracleLocalTest --console=plain
```

앱 실행용 schema를 준비하려면 다음 명령을 사용한다.

```powershell
.\gradlew.bat oracleLocalInitializeDatabase --console=plain
$env:ITS_DB_URL="jdbc:oracle:thin:@//localhost:1521/FREEPDB1"
$env:ITS_DB_USER="ITS_USER"
$env:ITS_DB_PASSWORD="ItsLocalDev2026!"
.\gradlew.bat oracleConnectionCheck --console=plain
.\gradlew.bat run --console=plain
```

Docker Oracle의 앱 DB를 고정 seed로 되돌리려면 다음 명령을 사용한다.

```powershell
.\gradlew.bat oracleLocalResetFixedSeed --console=plain
```

컨테이너와 volume까지 초기화해야 할 때만 다음 명령을 사용한다.

```powershell
.\gradlew.bat oracleLocalReset --console=plain
```

## 5. 제출 전 검증 명령

최종 제출 직전에는 최소한 다음 명령을 실행한다.

```powershell
.\gradlew.bat check --console=plain
.\gradlew.bat verifySubmissionMetadata --console=plain
```

Oracle 환경까지 확인할 수 있다면 다음 명령을 추가로 실행한다.

```powershell
.\gradlew.bat oracleLocalTest --console=plain
```

## 6. 데모 seed 계정

초기 seed 데이터의 로그인 비밀번호는 평문으로 DB에 저장하지 않고, `USER_CREDENTIALS` 테이블의 `password_salt`, `password_hash` 값으로 저장한다. 아래 값은 데모와 로컬 실행을 위한 초기 입력값이다.

| Login ID | Initial Password |
|---|---|
| `admin` | `DemoLocalAdmin!` |
| `pl1` | `DemoLocalPl1!` |
| `pl2` | `DemoLocalPl2!` |
| `dev1` ~ `dev10` | `DemoLocalDev1!` ~ `DemoLocalDev10!` |
| `tester1` ~ `tester5` | `DemoLocalTester1!` ~ `DemoLocalTester5!` |

이 비밀번호는 운영용 비밀번호가 아니라 로컬 개발과 데모 실행을 위한 고정 초기값이다.
