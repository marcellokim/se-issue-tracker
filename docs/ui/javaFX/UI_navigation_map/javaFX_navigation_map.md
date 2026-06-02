# JavaFX UI Navigation Map

## 화면 구성

### Admin 영역 (빨간 계열)

| # | 화면 | 접근 Actor | 진입 조건 |
|---|---|---|---|
| 1 | Admin 대시보드 | Admin | Admin으로 로그인 |
| 2 | 계정 관리 | Admin | 대시보드에서 진입 |
| 3 | 계정 생성 (모달) | Admin | 계정 관리에서 버튼 |
| 4 | 계정 상세/수정 | Admin | 계정 관리에서 사용자 선택 |
| 5 | 프로젝트 관리 | Admin | 대시보드에서 진입 |
| 6 | 프로젝트 생성 (모달) | Admin | 프로젝트 관리에서 버튼 |
| 7 | 프로젝트 상세 | Admin | 프로젝트 관리에서 선택 |
| 8 | 프로젝트 수정 | Admin | 프로젝트 상세에서 진입 |
| 9 | 멤버 관리 | Admin | 프로젝트 상세에서 진입 |

### Auth User 영역 (파란 계열)

| # | 화면 | 접근 Actor | 진입 조건 |
|---|---|---|---|
| 1 | 로그인 | 전체 | 앱 시작 시 |
| 2 | 프로젝트 목록 | PL, Dev, Tester | 로그인 성공 |
| 3 | 이슈 목록 | 프로젝트 멤버 | 프로젝트 선택 |
| 4 | 이슈 상세 | 프로젝트 멤버 | 이슈 클릭 |
| 5 | 삭제 이슈 관리 | PL | 이슈 목록에서 진입 |
| 6 | 통계 | PL, Dev, Tester | 이슈 목록에서 진입 |

Non-admin 사용자가 프로젝트를 선택하면 이슈 목록 화면은 프로젝트 내부 화면 역할을 한다. 따라서 화면 진입 시 `ProjectController.viewProjectNonAdminDetail(projectId)`로 프로젝트 기본 정보를 조회하고, `IssueController.viewProjectIssues(projectId)`로 프로젝트 전체 이슈 목록을 조회한다.

## 이슈 상세 화면 — 버튼 활성화 제어

이슈 상세 화면 진입 시 `IssueController.viewAvailableActions(issueId)`를 호출하여 `IssueWorkflowActions`를 받는다. UI는 이 boolean 값으로만 버튼 표시 여부를 결정한다. 비즈니스 규칙을 UI에 중복 구현하지 않는다.

| 버튼 | 활성화 조건 (IssueWorkflowActions) | 호출 API |
|---|---|---|
| FIXED 처리 | `canMarkFixed` | IssueStateController.changeStatus(FIXED) |
| RESOLVED 처리 | `canResolve` | IssueStateController.changeStatus(RESOLVED) |
| 반려 (reject fix) | `canRejectFix` | IssueStateController.changeStatus(ASSIGNED) |
| CLOSED 처리 | `canClose` | IssueStateController.changeStatus(CLOSED) |
| REOPENED 처리 | `canReopen` | IssueStateController.changeStatus(REOPENED) |
| 배정 | `canAssign` | AssignmentController.assignIssue() |
| 재배정 | `canReassign` | AssignmentController.reassignIssue() |
| 검증자 변경 | `canChangeVerifier` | AssignmentController.changeVerifier() |
| 이슈 수정 | `canUpdateIssue` | IssueController.updateIssue() |
| 우선순위 변경 | `canChangePriority` | IssueController.changePriority() |
| 의존성 추가 | `canAddDependency` | IssueController.addDependency() |
| 의존성 제거 | `canRemoveDependency` | IssueController.removeDependency() |
| 코멘트 추가 | `canAddComment` | IssueController.addComment() |
| 이슈 삭제 | `canSoftDelete` | DeletedIssueController.deleteIssue() |

코멘트 수정/삭제는 개별 코멘트 단위로 `canUpdateComment(issueId, commentId)`, `canDeleteComment(issueId, commentId)`를 호출한다.

## 삭제 이슈 관리 화면

- `DeletedIssueController.getMaxRetentionLimit()` → 보관 한도 (30)
- `DeletedIssueController.viewDeletedIssues(projectId)` → 삭제 이슈 목록
- 표시: "삭제 이슈 N/30"
- 복구: `restoreIssue(issueId, comment)`
- 영구 삭제: `purgeDeletedIssue(issueId)`

## 화면 전이 흐름

```
로그인
  ├── Admin ──→ Admin 대시보드
  │               ├──→ 계정 관리 ──→ 계정 생성 / 계정 상세(수정)
  │               └──→ 프로젝트 관리 ──→ 프로젝트 생성 / 프로젝트 상세
  │                                         ├──→ 프로젝트 수정/삭제
  │                                         └──→ 멤버 추가/제거
  │
  └── PL|Dev|Tester ──→ 프로젝트 목록 ──→ 이슈 목록(프로젝트 기본정보 + 전체 이슈) ──→ 이슈 상세
                                            │               │
                                            │               ├──→ 상태 변경 (viewAvailableActions 기반)
                                            │               ├──→ 배정/재배정 (KNN 추천)
                                            │               ├──→ 코멘트 추가/수정/삭제
                                            │               ├──→ 의존성 추가/제거
                                            │               ├──→ 이슈 수정 (reporter, NEW/REOPENED)
                                            │               ├──→ 우선순위 변경 (PL)
                                            │               └──→ 이슈 삭제 (PL, canSoftDelete)
                                            │
                                            ├──→ 이슈 등록 (canRegisterIssue)
                                            ├──→ 삭제 이슈 관리 (PL) [N/30 표시]
                                            └──→ 통계
```

## 과제 데모 시나리오 매핑 (2.2절)

| 데모 단계 | Actor | 화면 흐름 | 호출 API |
|---|---|---|---|
| Admin이 project1 생성 + 계정 추가 | Admin | Admin 대시보드 → 프로젝트 생성 → 멤버 추가 | createProject, addProjectParticipant |
| tester1이 이슈 등록 + 코멘트 | Tester | 이슈 목록 → 이슈 등록 → 이슈 상세 → 코멘트 | registerIssue, addComment |
| PL1이 브라우즈 + 검색 + 배정 | PL | 이슈 목록 (검색) → 이슈 상세 → 배정 | searchIssues, startAssignment, assignIssue |
| dev1이 fixed | Dev | 이슈 목록 → 이슈 상세 → 상태 변경 | changeStatus(FIXED) |
| tester1이 resolved | Tester | 이슈 상세 → 상태 변경 | changeStatus(RESOLVED) |
| PL1이 closed | PL | 이슈 상세 → 상태 변경 | changeStatus(CLOSED) |
| PL2가 추천 배정 (optional) | PL | 이슈 상세 → 배정 (KNN 추천) | startAssignment, assignIssue |

## 다이어그램

`javafx-navigation-map.puml` 참조.
- 파란색: Auth User 화면 (screen)
- 빨간색: Admin 화면 (admin)
- 초록색: 액션 (action)
- 노란색: 모달 (modal)