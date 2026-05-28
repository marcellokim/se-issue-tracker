# Statistics Controller 명세

## 범위

`StatisticsController`는 UI/API 경계에서 들어온 프로젝트 통계 조회 요청을 받는다.
현재 로그인 사용자를 확인한 뒤, 실제 통계 조회 로직은 `StatisticsService`에 위임한다.

Controller는 통계 계산, DB 접근, 세부 권한 정책을 직접 처리하지 않는다.

UI 배치 정책상 통계는 전역 대시보드나 이슈 상세 화면이 아니라, 사용자가 프로젝트를 선택한 뒤 진입한 프로젝트 화면에서 해당 프로젝트 기준으로 조회한다.
JavaFX와 Swing UI는 각각 다른 화면을 구현하더라도 동일한 Controller/Service 계층을 호출해야 한다.

관련 파일:

- `src/main/java/com/github/marcellokim/issuetracker/controller/StatisticsController.java`
- `src/main/java/com/github/marcellokim/issuetracker/service/StatisticsService.java`
- `src/main/java/com/github/marcellokim/issuetracker/repository/StatisticsRepository.java`
- `src/main/java/com/github/marcellokim/issuetracker/persistence/jdbc/JdbcStatisticsRepository.java`

## Controller 메서드

### `viewStatistics(long projectId)`

특정 프로젝트의 전체 기간 통계를 조회한다.

내부적으로 날짜/월 범위를 모두 `null`로 둔 overload 메서드를 호출한다.

```java
viewStatistics(projectId, null, null, null, null)
```

즉, 기간 필터 없이 프로젝트 전체 통계를 조회할 때 사용하는 편의 메서드다.

### `viewStatistics(
        long projectId,
        LocalDate dailyFromInclusive,
        LocalDate dailyToInclusive,
        YearMonth monthlyFromInclusive,
        YearMonth monthlyToInclusive)`

특정 프로젝트의 통계를 조회한다.

- 일별 통계는 `dailyFromInclusive`부터 `dailyToInclusive`까지의 범위를 사용한다.
- 월별 통계는 `monthlyFromInclusive`부터 `monthlyToInclusive`까지의 범위를 사용한다.
- 범위 값이 `null`이면 전체 기간 기준으로 조회한다.
- 반환 타입은 `StatisticsReportResult`다.

### `canViewStatistics(long projectId)`

현재 로그인 사용자가 해당 프로젝트의 통계를 볼 수 있는지 확인한다.

UI에서 통계 버튼 활성화/비활성화 판단에 사용할 수 있다.
단, 현재 구현에서는 로그인하지 않은 상태라면 `false`를 반환하는 것이 아니라
`SecurityException("Login is required.")`가 발생한다. 이는 내부에서
`requireCurrentUser()`를 먼저 호출하기 때문이다.

### `requireCurrentUser()`

private helper 메서드다.

`AuthenticationService.currentUser()`를 호출해 현재 로그인 사용자를 가져온다.
로그인 사용자가 없으면 다음 예외를 발생시킨다.

```text
SecurityException("Login is required.")
```

## 내부 정책 및 권한 검사

통계 조회 권한은 `StatisticsController`가 직접 판단하지 않는다.
세부 권한 검사는 `StatisticsService`와 `PermissionPolicy`에서 처리한다.

내부 정책은 다음과 같다.

- 로그인한 사용자만 통계 조회 가능.
- active 사용자만 통계 조회 가능.
- `PL`, `DEV`, `TESTER`만 통계 조회 가능.
- `ADMIN`은 이슈 통계 조회 불가.
- 사용자는 자신이 active member로 속한 프로젝트의 통계만 조회 가능.
- 다른 프로젝트의 통계는 조회할 수 없다.

서비스 레이어의 권한 검사 흐름은 다음과 같다.

```text
StatisticsService.viewStatistics(...)
-> permissionPolicy.assertCanViewStatistics(actor, projectId)
-> requireActiveProjectMember(actor, projectId)
```

`PermissionPolicy.assertCanViewStatistics(...)`는 active `PL / DEV / TESTER`인지 검사한다.

`requireActiveProjectMember(...)`는 다음 repository 메서드를 사용해 프로젝트 소속 여부를 확인한다.

```java
userRepository.findActiveByRole(projectId, actor.getRole())
```

해당 프로젝트에서 actor와 같은 role을 가진 active user 목록을 가져온 뒤,
그 목록에 actor의 loginId가 포함되어 있는지 확인한다.
포함되어 있지 않으면 다음 예외를 발생시킨다.

```text
SecurityException("Only project members can view statistics.")
```

## 하위 레이어 호출 워크플로우

전체 통계 조회 흐름은 다음과 같다.

```text
UI 또는 외부 호출
-> StatisticsController.viewStatistics(...)
-> StatisticsController.requireCurrentUser()
-> StatisticsService.viewStatistics(...)
-> PermissionPolicy.assertCanViewStatistics(...)
-> UserRepository.findActiveByRole(...)
-> StatisticsRepository.buildReport(...)
-> JdbcStatisticsRepository.buildReport(...)
-> StatisticsReport
-> StatisticsReportResult.from(...)
-> StatisticsController가 StatisticsReportResult 반환
```

Controller는 Service가 반환한 DTO/Result를 그대로 반환한다.

## 통계 리포트에 포함되는 항목

`JdbcStatisticsRepository.buildReport(...)`는 다음 통계들을 모아 `StatisticsReport`를 만든다.

### 현재 상태별 이슈 개수

사용 메서드:

```java
countByStatus(projectId)
```

의미:

- `issues.status` 기준으로 현재 이슈 상태별 개수를 센다.
- `DELETED` 상태의 이슈는 제외한다.

### 현재 우선순위별 이슈 개수

사용 메서드:

```java
countByPriority(projectId)
```

의미:

- `issues.priority` 기준으로 현재 이슈 우선순위별 개수를 센다.
- `DELETED` 상태의 이슈는 제외한다.

### 일별 이슈 등록 수

사용 메서드:

```java
countReportedIssuesByDay(projectId, dailyFromInclusive, dailyToInclusive)
```

의미:

- 일별 이슈 등록 수를 센다.
- `issues.reported_at`을 기준으로 한다.

### 월별 이슈 등록 수

사용 메서드:

```java
countReportedIssuesByMonth(projectId, monthlyFromInclusive, monthlyToInclusive)
```

의미:

- 월별 이슈 등록 수를 센다.
- `issues.reported_at`을 기준으로 한다.

### 월별 상태별 이슈 개수

사용 메서드:

```java
countByStatusByMonth(projectId, monthlyFromInclusive, monthlyToInclusive)
```

의미:

- `reported_at`의 월과 현재 `status`를 기준으로 이슈 개수를 묶는다.
- 상태 변경 이력 횟수를 세는 통계가 아니다.
- 정확한 의미는 “각 월에 등록된 이슈들의 현재 상태 분포”다.

### 월별 우선순위별 이슈 개수

사용 메서드:

```java
countByPriorityByMonth(projectId, monthlyFromInclusive, monthlyToInclusive)
```

의미:

- `reported_at`의 월과 현재 `priority`를 기준으로 이슈 개수를 묶는다.
- 우선순위 변경 이력 횟수를 세는 통계가 아니다.
- 정확한 의미는 “각 월에 등록된 이슈들의 현재 우선순위 분포”다.

### 일별 상태 변경 횟수

사용 메서드:

```java
countStatusChangesByDay(projectId, dailyFromInclusive, dailyToInclusive)
```

의미:

- 일별 상태 변경 횟수를 센다.
- `issue_history.changed_at`을 기준으로 한다.
- `action_type = 'STATUS_CHANGED'`인 이력만 센다.

### 월별 상태 변경 횟수

사용 메서드:

```java
countStatusChangesByMonth(projectId, monthlyFromInclusive, monthlyToInclusive)
```

의미:

- 월별 상태 변경 횟수를 센다.
- `issue_history.changed_at`을 기준으로 한다.
- `action_type = 'STATUS_CHANGED'`인 이력만 센다.

### 일별 코멘트 작성 수

사용 메서드:

```java
countCommentsByDay(projectId, dailyFromInclusive, dailyToInclusive)
```

의미:

- 일별 코멘트 작성 수를 센다.
- `comments.created_at`을 기준으로 한다.

### 월별 코멘트 작성 수

사용 메서드:

```java
countCommentsByMonth(projectId, monthlyFromInclusive, monthlyToInclusive)
```

의미:

- 월별 코멘트 작성 수를 센다.
- `comments.created_at`을 기준으로 한다.

## JDBC 예외 처리

`JdbcStatisticsRepository`는 JDBC 접근 시 다음 방식을 사용한다.

- `PreparedStatement`로 파라미터를 바인딩한다.
- `Connection`, `PreparedStatement`, `ResultSet`은 `try-with-resources`로 정리한다.
- `SQLException`은 `RepositoryException`으로 감싸서 던진다.

예외 변환 예:

```java
catch (SQLException exception) {
    throw new RepositoryException(errorMessage, exception);
}
```

일별/월별 통계 쿼리 실행은 다음 helper 메서드에서 공통 처리한다.

- `executeDailyQuery(...)`
- `executeMonthlyQuery(...)`

날짜/월 범위 검증은 DB 조회 전에 수행된다.

- `dailyFromInclusive > dailyToInclusive`이면 `IllegalArgumentException`.
- `monthlyFromInclusive > monthlyToInclusive`이면 `IllegalArgumentException`.

## 후속 보강 노트

아래 내용은 필수 버그 수정은 아니지만, 후속 작업에서 고려할 수 있는 사항이다.

### `DELETED` 이슈 제외

대부분의 통계 쿼리는 `DELETED` 상태의 이슈를 제외한다.

이는 통계의 의미가 “현재 visible issue 기준 통계”라면 적절하다.
반대로 “프로젝트 전체 이력 통계”가 필요하다면 deleted issue의 과거 이력까지 포함하는
별도 통계나 정책 재정의가 필요하다.

### 월별 상태/우선순위 통계의 의미

`countByStatusByMonth(...)`와 `countByPriorityByMonth(...)`는 상태 변경 횟수나
우선순위 변경 횟수를 세는 메서드가 아니다.

두 메서드는 `reported_at`의 월을 기준으로, 해당 월에 등록된 이슈들의 현재 상태 또는
현재 우선순위 분포를 보여준다.

따라서 UI나 문서에서는 다음과 같이 표현하는 것이 좋다.

- “월별 현재 상태 분포”
- “월별 현재 우선순위 분포”

다음 표현은 피하는 것이 좋다.

- “월별 상태 변경 수”
- “월별 우선순위 변경 수”

### `projectId <= 0`

현재 `StatisticsService`는 `projectId <= 0`을 명시적으로 검증하지 않는다.

정상 앱 워크플로우에서는 프로젝트 ID가 DB에 저장된 프로젝트에서 오기 때문에 항상 양수다.
따라서 기능상 문제 가능성은 낮다.

잘못된 직접 API 호출로 `projectId <= 0`이 들어와도 프로젝트 멤버십 검사에서 막힐 가능성이 높다.
다만 에러 메시지는 “projectId가 잘못됨”이 아니라 “프로젝트 멤버가 아님”으로 나올 수 있다.

에러 메시지 품질을 높이고 싶다면 `StatisticsService.viewStatistics(...)` 초반에
`requireProjectId(projectId)`를 추가할 수 있다.

### 존재하지 않는 projectId

현재는 존재하지 않는 projectId가 들어오면 “존재하지 않는 프로젝트”가 아니라
“해당 프로젝트의 active member가 아님”으로 처리된다.

현재 정책이 “유저는 자신이 속한 프로젝트의 통계만 볼 수 있다”이므로 이 처리는 허용 가능하다.

만약 에러 메시지를 더 정교하게 만들고 싶다면 `StatisticsService`에 `ProjectRepository`
의존성을 추가하고 프로젝트 존재 여부를 명시적으로 확인해야 한다.
다만 이 경우 서비스 의존성이 늘어나므로 현재 단계에서는 필수 보강 사항은 아니다.

### JDBC 생성자 null guard

`JdbcStatisticsRepository` 생성자는 `connectionProvider`에 대해 `Objects.requireNonNull`을
사용하지 않는다.

다만 현재 다른 JDBC repository들도 같은 스타일을 사용하고 있으므로, 이 파일만 단독으로
수정하기보다는 JDBC repository 전체 스타일을 통일할 때 함께 고려하는 것이 좋다.
