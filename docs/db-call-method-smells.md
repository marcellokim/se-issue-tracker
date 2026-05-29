# DB Call / Method Smell 정리

## 범위

현재 서비스 레이어가 `Jdbc...` 구현체를 직접 호출하는 구조는 아니다. 서비스는 repository 인터페이스를 통해 하위 레이어에 접근한다.

다만 일부 repository 메서드 이름과 호출 방식이 SQL/트랜잭션/조회 방식의 냄새를 서비스 레이어까지 노출하고 있다. 아래 8개는 기능이 당장 깨지는 문제라기보다, 유지보수성과 레이어 책임 분리를 더 깔끔하게 만들기 위한 정리 후보이다.

## 요약

| 번호 | 위치 | 냄새 | 권장 방향 |
|---|---|---|---|
| 1 | `IssueRepository.findRecommendationForAssignment` | 추천용 조회가 일반 IssueRepository에 있음 | `AssignmentRecommendationRepository`로 이동 |
| 2 | `CommentRepository.saveAndRecordIssueChange` 계열 | comment 저장과 issue history 기록 트랜잭션 구현이 메서드명에 노출됨 | 도메인 행위 이름으로 정리 |
| 3 | `IssueDependencyRepository` dependency 기록 계열 | dependency 저장/삭제와 history 기록 방식이 메서드명에 노출됨 | `recordDependencyAdded/Removed`로 정리됨 |
| 4 | `IssueRepository.softDelete/restore/purge...` | deleted issue workflow가 일반 IssueRepository에 많이 섞임 | deleted issue 전용 흐름으로 분리 검토 |
| 5 | `IssueRepository.existsByResponsibleUser` | 이름이 현재 책임인지 과거 이력까지 포함하는지 모호함 | 현재 책임 기준 이름으로 변경 |
| 6 | `IssueRepository.existsActiveAssignmentByProjectAndUser` | SQL 조건식에 가까운 이름 | 정책 중심 이름으로 변경 |
| 7 | `findActiveByRole(...).stream().anyMatch(...)` 반복 | boolean 검사를 위해 리스트를 조회함 | `exists...` 계열 repository 메서드 추가 |
| 8 | `DashboardSummaryService.summarizeProject` | 프로젝트별 다중 DB 호출 | dashboard read model 또는 batch query 검토 |

## 1. Assignment 추천용 조회가 IssueRepository에 있음

### 현재 구조

- 파일: `src/main/java/com/github/marcellokim/issuetracker/repository/IssueRepository.java`
- 메서드: `findRecommendationForAssignment(long projectId)`
- 호출 위치: `src/main/java/com/github/marcellokim/issuetracker/service/AssignmentRecommendationService.java`
- JDBC 구현: `JdbcIssueRepository.findRecommendationForAssignment(...)`
- SQL: `JdbcIssueQueries.FIND_RESOLVED_OR_CLOSED_BY_PROJECT_SQL`

### 냄새

이 메서드는 일반적인 이슈 조회라기보다 assignment 추천 모델이 학습/판단에 사용할 resolved/closed 이슈 이력을 가져오는 용도이다. 그런데 일반 `IssueRepository`에 들어가 있어 책임이 섞인다.

또한 인터페이스에 `default List.of()` 구현이 있으면 JDBC 구현이 빠져도 기능이 실패하지 않고 빈 추천 결과로 조용히 흘러갈 수 있다.

### 권장 방향

- `AssignmentRecommendationRepository`로 이동
- 예시 이름:
  - `findTrainingIssues(long projectId)`
  - `findResolvedOrClosedIssuesForRecommendation(long projectId)`
- `IssueRepository`의 default 메서드는 제거하는 편이 좋다.

## 2. CommentRepository의 `saveAndRecordIssueChange` 계열

### 현재 구조

- 파일: `src/main/java/com/github/marcellokim/issuetracker/repository/CommentRepository.java`
- 대표 메서드:
  - `saveAndRecordIssueChange(...)`
  - `deleteGeneralByIdAndRecordIssueChange(...)`
- 호출 위치: `IssueService.addComment`, `IssueService.updateComment`, `IssueService.deleteComment`

### 냄새

댓글 저장/수정/삭제와 issue history 기록을 하나의 JDBC 트랜잭션으로 묶는 것은 타당하다. 다만 `AndRecordIssueChange`라는 이름은 하위 구현 방식과 감사 기록 방식을 서비스 레이어 메서드명에 그대로 노출한다.

### 권장 방향

트랜잭션은 JDBC 구현에 유지하되, repository 메서드 이름은 도메인 행위 중심으로 바꾼다.

예시:

- `recordCommentAdded(...)`
- `recordCommentUpdated(...)`
- `recordGeneralCommentDeleted(...)`

## 3. IssueDependencyRepository의 dependency 기록 계열

### 현재 구조

- 파일: `src/main/java/com/github/marcellokim/issuetracker/repository/IssueDependencyRepository.java`
- 대표 메서드:
  - 기존: `saveAndRecordIssueChange(...)`
  - 기존: `deleteByDependencyIdAndRecordIssueChange(...)`
  - 현재: `recordDependencyAdded(...)`
  - 현재: `recordDependencyRemoved(...)`
- 호출 위치: `IssueService.addDependency`, `IssueService.removeDependency`

### 냄새

dependency 추가/삭제와 blocked issue history 기록을 같이 처리하는 정책은 맞다. 하지만 메서드명이 저장 방식과 history 기록 방식을 직접 드러낸다.

### 반영 방향

도메인 행위 중심 이름으로 정리한다.

- `recordDependencyAdded(...)`
- `recordDependencyRemoved(...)`

## 4. Deleted issue workflow가 IssueRepository에 많이 섞임

### 현재 구조

- 파일: `src/main/java/com/github/marcellokim/issuetracker/service/DeletedIssueService.java`
- 관련 repository 메서드:
  - `softDelete(...)`
  - `restore(...)`
  - `purgeDeletedById(...)`
  - `purgeDeletedBeyondLimit(...)`
- JDBC 구현 보조 파일: `JdbcIssueDeleteOperations.java`

### 냄새

deleted issue 조회, soft delete, restore, purge, FIFO 보관 정책은 일반 이슈 조회/저장보다 별도 workflow 성격이 강하다. 현재는 일반 `IssueRepository`에 deleted issue 관련 메서드가 많이 들어가 있어 repository 책임이 커진다.

### 권장 방향

기능상 현재 구조로도 동작은 가능하다. 다만 더 깔끔하게 가려면 deleted issue 전용 repository 또는 전용 operation 포트를 고려할 수 있다.

예시:

- `DeletedIssueRepository.softDelete(...)`
- `DeletedIssueRepository.restore(...)`
- `DeletedIssueRepository.purgeDeletedIssue(...)`
- `DeletedIssueRepository.purgeOverflow(...)`

## 5. `existsByResponsibleUser` 이름이 모호함

### 현재 구조

- 파일: `src/main/java/com/github/marcellokim/issuetracker/repository/IssueRepository.java`
- 메서드: `existsByResponsibleUser(String loginId)`
- 호출 위치: `AccountService`

### 냄새

현재 정책상 계정 비활성화/역할 변경 차단은 `ASSIGNED`, `FIXED` 상태의 현재 assignee/verifier 책임만 본다. 과거 감사 이력인 fixer/resolver는 차단 조건에서 제외한다.

그런데 `ResponsibleUser`라는 이름은 현재 책임인지, 과거 fixer/resolver 이력까지 포함하는지 모호하다.

### 권장 방향

정책 기준이 드러나게 이름을 바꾼다.

예시:

- `hasCurrentIssueResponsibility(String loginId)`
- `existsCurrentIssueResponsibility(String loginId)`

## 6. `existsActiveAssignmentByProjectAndUser` 이름이 SQL 조건식에 가까움

### 현재 구조

- 파일: `src/main/java/com/github/marcellokim/issuetracker/repository/IssueRepository.java`
- 메서드: `existsActiveAssignmentByProjectAndUser(long projectId, String loginId)`
- 호출 위치: `ProjectService`

### 냄새

서비스 정책은 “프로젝트 참가자를 제거하려 할 때, 해당 사용자가 현재 진행 중인 이슈 책임을 가지고 있으면 제거를 막는다”이다.

하지만 메서드명은 정책보다 SQL 조건식에 가깝다.

### 권장 방향

5번과 맞춰 책임 기준 이름으로 정리한다.

예시:

- `hasCurrentIssueResponsibility(long projectId, String loginId)`
- `existsCurrentIssueResponsibility(long projectId, String loginId)`

## 7. Active member / active PL 검사가 리스트 조회 후 stream으로 반복됨

### 현재 구조

여러 서비스에서 아래와 비슷한 패턴이 반복된다.

```java
userRepository.findActiveByRole(projectId, Role.PL).stream()
        .anyMatch(user -> user.getLoginId().equals(actor.getLoginId()));
```

### 냄새

서비스가 알고 싶은 것은 대부분 “이 사용자가 해당 프로젝트의 active member인가?” 또는 “이 사용자가 해당 프로젝트의 active PL인가?”이다.

그런데 현재는 리스트를 가져온 뒤 Java stream으로 boolean을 만든다. 이는 조회 방식이 서비스에 노출되는 형태이고, DB 입장에서도 exists query로 처리하는 편이 자연스럽다.

### 권장 방향

`UserRepository` 또는 project membership 관련 repository에 exists 계열 메서드를 추가한다.

예시:

- `existsActiveProjectMember(long projectId, String loginId)`
- `existsActiveProjectMemberWithRole(long projectId, String loginId, Role role)`

적용 후보:

- `AssignmentService`
- `DeletedIssueService`
- `IssueService`
- `IssueStateService`
- `IssueWorkflowService`
- `StatisticsService`

## 8. DashboardSummaryService의 프로젝트별 다중 DB 호출

### 현재 구조

- 파일: `src/main/java/com/github/marcellokim/issuetracker/service/DashboardSummaryService.java`
- 메서드: `summarizeProject(...)`

프로젝트 하나를 요약하기 위해 다음과 같은 조회가 여러 번 발생한다.

- `projectRepository.findParticipants(projectId)`
- `userRepository.findActiveByRole(projectId, Role.PL)`
- `userRepository.findActiveByRole(projectId, Role.DEV)`
- `userRepository.findActiveByRole(projectId, Role.TESTER)`
- `issueRepository.findByProject(projectId)`
- `issueRepository.findDeletedByProject(projectId)`
- `statisticsRepository.countByStatus(projectId)`

### 냄새

대시보드 요약은 read model 성격이 강한데, 현재는 서비스가 여러 repository 호출을 조합해 직접 summary를 만든다. 프로젝트 수가 늘어나면 N+1 형태로 DB 호출이 늘어날 수 있다.

### 권장 방향

기능상 현재 구조도 동작은 가능하다. 다만 대시보드가 주요 화면이면 별도 summary 조회를 고려할 수 있다.

예시:

- `DashboardSummaryRepository.findProjectSummariesForAdmin()`
- `DashboardSummaryRepository.findProjectSummariesForUser(String loginId)`
- 또는 프로젝트 ID 목록을 받아 batch summary를 반환하는 메서드

## 정리 우선순위

1. `findRecommendationForAssignment`를 `AssignmentRecommendationRepository`로 이동한다.
2. active member / active PL 검사를 `exists...` 메서드로 바꾼다.
3. `existsByResponsibleUser`, `existsActiveAssignmentByProjectAndUser` 이름을 현재 책임 기준으로 정리한다.
4. comment의 `...AndRecordIssueChange` 메서드명도 필요하면 같은 방식으로 도메인 행위 중심으로 바꾼다. dependency 쪽은 `recordDependencyAdded/Removed`로 정리되었다.
5. deleted issue workflow와 dashboard summary read model 분리는 이후 구조 정리 단계에서 검토한다.
