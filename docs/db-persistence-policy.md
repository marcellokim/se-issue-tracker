# DB Persistence Policy

이 문서는 ITS 애플리케이션에서 Oracle DB를 초기화하고, 테스트용 고정 시드와 실제 앱 실행용 영속 DB를 구분하는 기준을 정리한다.

## 실행용 Oracle DB

애플리케이션은 JDBC를 통해 Oracle DB에 연결한다. 실행 전에 아래 환경변수를 설정한다.

```powershell
$env:ITS_DB_URL="jdbc:oracle:thin:@//localhost:1521/XEPDB1"
$env:ITS_DB_USER="ITS_USER"
$env:ITS_DB_PASSWORD="ItsLocalDev2026!"
```

일반적인 앱 실행 흐름은 다음과 같다.

```powershell
.\gradlew.bat oracleConnectionCheck
.\gradlew.bat oracleInitializeDatabase
.\gradlew.bat run
```

`oracleInitializeDatabase`는 비파괴 초기화 명령이다.

- `schema-oracle.sql`은 항상 실행한다.
- 기존 테이블/데이터를 삭제하지 않는다.
- core table이 하나도 없는 최초 실행일 때만 `seed-oracle.sql`을 실행한다.
- 이미 앱 데이터가 있으면 seed를 다시 넣지 않고 기존 DB 데이터를 그대로 사용한다.

`oracleInitializeApplicationDatabase`는 같은 동작을 하는 호환용 alias이다.

## 개발/테스트용 고정 시드 리셋

개발 중 DB를 깨끗한 고정 시드 상태로 되돌리고 싶을 때는 별도 명령을 사용한다.

```powershell
.\gradlew.bat oracleResetFixedSeed
```

`oracleResetFixedSeed`는 파괴적 리셋 명령이다.

- core table을 drop한다.
- `schema-oracle.sql`을 다시 실행한다.
- `seed-oracle.sql`을 다시 실행해서 고정 시드 A를 복원한다.

이 명령은 로컬 개발/테스트용으로만 사용한다. 데모 중 누적된 앱 데이터를 유지해야 하는 환경에서는 사용하지 않는다.

## 초기화 책임 분리

현재 초기화 정책은 다음과 같이 나뉜다.

| 목적 | Gradle task | 동작 |
| --- | --- | --- |
| DB 연결 확인 | `oracleConnectionCheck` | 환경변수 기반 Oracle 연결 가능 여부 확인 |
| 앱 실행용 초기화 | `oracleInitializeDatabase` | schema migration 실행, 최초 실행 때만 seed 삽입, 기존 데이터 보존 |
| 앱 실행용 초기화 alias | `oracleInitializeApplicationDatabase` | `oracleInitializeDatabase`와 같은 동작 |
| 개발/테스트 고정 시드 복원 | `oracleResetFixedSeed` | 기존 core table drop 후 schema와 seed 재생성 |
| Oracle repository 통합 테스트 | `oracleIntegrationTest` | 테스트 schema에서 repository 통합 테스트 수행 |

## Schema 정책

`schema-oracle.sql`은 앱 실행 중에도 반복 실행될 수 있으므로 데이터 삭제 구문을 포함하지 않는다.

- `drop table` 금지
- `truncate` 금지
- `delete from` 금지

호환 migration 또는 idempotent DDL은 `schema-oracle.sql` 안에서 반복 실행 가능하게 작성한다. 실제 테이블 drop은 `DatabaseInitializer.resetWithFixedSeed()` 경로에서만 수행한다.

중요한 schema 기준은 다음과 같다.

- `USERS.login_id`를 primary key로 사용한다.
- `USER_CREDENTIALS`는 `password_salt`, `password_hash`를 저장한다.
- `Issue.deletedAt`, `Issue.preDeleteStatus`는 DB 컬럼으로 만들지 않는다.
- 삭제/복구 기준은 `ISSUE_HISTORY` 조회로 처리한다.
- `ISSUE_DEPENDENCIES.dependency_id`에는 `SHA-256(blocking_issue_id + ":" + blocked_issue_id)` 값을 저장한다.

## Seed 정책

`seed-oracle.sql`은 기본 demo 데이터를 제공한다. 앱 실행용 초기화에서는 최초 실행 때만 seed를 넣고, 이후에는 사용자가 변경한 DB 데이터를 보존한다.

고정 시드는 다음 내용을 포함한다.

- `admin`
- `pl1`, `pl2`
- `dev1`부터 `dev10`
- `tester1`부터 `tester5`
- `project1`, `project2`
- 프로젝트별 멤버 관계와 프로젝트별 PL 1명 배정
- 추천/통계 테스트용 resolved/closed/reopened issue 샘플
- comment, status history, dependency 샘플

개발/테스트 중 seed를 항상 같은 상태로 복원해야 하면 `oracleResetFixedSeed`를 사용한다.

## Repository 통합 테스트용 격리 DB

`.\gradlew.bat test`는 기본 단위 테스트와 DB resource smoke test를 실행한다. Oracle DB에 직접 연결하는 repository 통합 테스트는 별도 명령으로 실행한다.

```powershell
$env:ITS_TEST_DB_URL="jdbc:oracle:thin:@//localhost:1521/XEPDB1"
$env:ITS_TEST_DB_USER="ITS_TEST_USER"
$env:ITS_TEST_DB_PASSWORD="ItsTestLocal2026!"

.\gradlew.bat oracleIntegrationTest
```

`oracleIntegrationTest`는 테스트 JVM에 `ITS_DB_INTEGRATION_TESTS=true`를 설정한다. `OracleRepositoryIntegrationTest`는 개발용 `ITS_USER` schema가 아니라 별도 테스트 schema를 사용해야 한다. 이 방식으로 repository CRUD 테스트를 반복 실행해도 일반 개발 DB의 seed/demo 데이터가 오염되지 않게 한다.

## 삭제 정책

UC9 기준 Issue 삭제는 soft-delete 방식으로 처리한다.

- `IssueRepository.softDelete(...)`는 issue status를 `DELETED`로 변경한다.
- 삭제 전 status는 `ISSUE_HISTORY`에 기록한다.
- 삭제된 issue에 연결된 dependency는 제거한다.
- `IssueRepository.purgeDeletedBeyondLimit(projectId, 30)`는 삭제 보관 수가 30개를 초과할 때 오래된 deleted issue부터 FIFO 순서로 물리 삭제한다.

Project 삭제는 composition 관계에 따른 hard-delete 방식으로 처리한다.

- `ProjectRepository.deleteById(projectId)`는 먼저 project member 관계를 삭제한다.
- 이후 해당 project가 소유한 issue를 삭제한다.
- issue가 소유한 comment, history, dependency는 issue foreign key cascade를 통해 함께 제거된다.
- 마지막으로 project row를 삭제한다.

이는 회의에서 결정한 "project 삭제 시 project가 소유한 issue도 함께 삭제한다"는 정책을 반영한 것이다.

## 추천 쿼리 기준

현재 추천 repository는 status가 `RESOLVED` 또는 `CLOSED`인 issue를 대상으로 `issues.fixer_login_id`, `issues.resolver_login_id`를 집계한다.

- DEV 추천은 `fixer_login_id`를 기준으로 한다.
- TESTER 추천은 `resolver_login_id`를 기준으로 한다.
- Seed history의 status transition comment는 actor가 위 요약 컬럼과 충돌하지 않게 구성한다.

추후 추천 기능의 기준을 `ISSUE_HISTORY` 단일 SSOT로 엄격히 정한다면 수정 대상은 `JdbcAssignmentRecommendationRepository`이다.
