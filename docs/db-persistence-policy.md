# DB 영속성 정책

이 문서는 #18 DB 기반 영속 저장소 작업에서 사용하는 Oracle 연결 방식,
테스트 DB 분리 방식, schema/seed 정책, repository 조회 기준을 정리한다.

## 실행용 Oracle DB

애플리케이션은 JDBC를 통해 Oracle DB에 연결한다. 앱 실행 또는 로컬 개발용
schema 초기화를 하기 전에 아래 환경변수를 설정한다.

```powershell
$env:ITS_DB_URL="jdbc:oracle:thin:@//localhost:1521/XEPDB1"
$env:ITS_DB_USER="ITS_USER"
$env:ITS_DB_PASSWORD="ItsLocalDev2026!"
```

자주 사용하는 명령어는 다음과 같다.

```powershell
.\gradlew.bat oracleConnectionCheck
.\gradlew.bat oracleInitializeDatabase
.\gradlew.bat run
```

`oracleInitializeDatabase`는 classpath에 포함된 SQL 파일을 아래 순서로 실행한다.

1. `db/oracle/schema-oracle.sql`
2. `db/oracle/seed-oracle.sql`

## Repository 통합 테스트용 격리 DB

`.\gradlew.bat test`는 기본 단위 테스트와 DB resource smoke test를 실행한다.
Oracle DB에 직접 연결하는 통합 테스트는 명시적으로 실행하지 않는 한 기본 테스트에서
건너뛴다.

#18 repository 검증 증거를 남기기 위해서는 일반 개발용 `ITS_USER` schema가 아니라
별도의 Oracle 테스트 schema/user를 사용한다. 아래 환경변수는 개발용 DB 계정이 아닌
테스트 전용 계정을 가리켜야 한다.

```powershell
$env:ITS_TEST_DB_URL="jdbc:oracle:thin:@//localhost:1521/XEPDB1"
$env:ITS_TEST_DB_USER="ITS_TEST_USER"
$env:ITS_TEST_DB_PASSWORD="ItsTestLocal2026!"

.\gradlew.bat oracleIntegrationTest
```

`oracleIntegrationTest`는 테스트 JVM에 `ITS_DB_INTEGRATION_TESTS=true`를 설정한다.
`OracleRepositoryIntegrationTest`는 `ITS_TEST_DB_USER`, `ITS_TEST_DB_PASSWORD`를
사용해서 테스트용 schema에 연결한다. 이 방식으로 repository CRUD 테스트를 반복
실행해도 일반 개발 DB의 seed/demo 데이터가 오염되지 않는다.

## Schema 초기화 정책

현재 #18 schema 기준으로 schema initializer는 반복 실행 가능해야 한다. 필요한
테이블과 인덱스가 없으면 생성한다. 다만 이전 schema 시도에서 만들어진 호환되지 않는
legacy 컬럼이 감지될 경우에는 관련 테이블을 재구성할 수 있다.

현재 중요한 schema 결정은 다음과 같다.

- `USERS.login_id`를 사용자 Primary Key로 사용한다.
- `USER_CREDENTIALS`는 `password_salt`, `password_hash`를 저장한다.
- `Issue.deletedAt`, `Issue.preDeleteStatus`는 DB 컬럼으로 만들지 않는다.
- 삭제/복구 기준은 `ISSUE_HISTORY` 조회로 처리한다.
- `ISSUE_DEPENDENCIES.dependency_id`에는 `SHA-256(blocking_issue_id + ":" + blocked_issue_id)` 값을 저장한다.

## Seed 정책

`seed-oracle.sql`은 `merge` 문을 사용한다. 따라서 seed를 여러 번 실행해도 핵심
demo 데이터가 계속 중복으로 쌓이지 않는다.

Seed 데이터는 다음 내용을 포함한다.

- `admin`
- `pl1`, `pl2`
- `dev1`부터 `dev10`
- `tester1`부터 `tester5`
- `project1`, `project2`
- 프로젝트별 멤버 관계
- 프로젝트당 PL 1명 배정
- 추천/통계 테스트에 사용할 resolved/closed/reopened 이슈 샘플
- 추천/통계 demo에 사용할 comment, status history, dependency 샘플

## 삭제 정책

UC9 기준 Issue 삭제는 soft-delete 방식으로 처리한다.

- `IssueRepository.softDelete(...)`는 이슈 status를 `DELETED`로 변경한다.
- 삭제 전 status는 `ISSUE_HISTORY`에 기록한다.
- 삭제된 이슈에 연결된 dependency는 제거한다.
- `IssueRepository.purgeDeletedBeyondLimit(projectId, 30)`은 삭제 보관함이 30개를
  초과할 때 오래된 deleted issue부터 FIFO 순서로 물리 삭제한다.

Project 삭제는 composition 관계에 따른 hard-delete 정리로 처리한다.

- `ProjectRepository.deleteById(projectId)`는 먼저 project member 관계를 삭제한다.
- 이후 해당 project가 소유한 issue를 삭제한다.
- issue가 소유한 comment, history, dependency는 issue foreign key cascade를 통해
  함께 제거된다.
- 마지막으로 project row를 삭제한다.

이는 회의에서 결정된 "project 삭제 시 project가 소유한 issue도 함께 삭제한다"는
정책을 반영한 것이다.

## 추천 쿼리 기준

현재 추천 repository는 status가 `RESOLVED` 또는 `CLOSED`인 이슈를 대상으로
`issues.fixer_login_id`, `issues.resolver_login_id`를 집계한다.

이 두 컬럼은 완료 이력의 요약값으로 해석한다.

- DEV 추천은 `fixer_login_id`를 기준으로 한다.
- TESTER 추천은 `resolver_login_id`를 기준으로 한다.
- Seed history는 status transition comment와 actor가 위 요약 컬럼과 어긋나지 않게
  구성한다.

추후 팀에서 추천 기능의 기준을 `ISSUE_HISTORY` 단일 SSOT로 엄격히 정하기로 하면,
수정 대상은 `JdbcAssignmentRecommendationRepository`이다.
