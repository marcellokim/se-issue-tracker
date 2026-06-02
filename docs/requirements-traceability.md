# 요구사항 추적표

이 문서는 팀이 보유한 과제 요구사항을 기준으로, GitHub 이슈와 산출물을 연결하기 위한 작업용 추적표입니다. 과제 원문과 이 문서가 충돌하면 과제 원문을 우선합니다. 원문 PDF는 현재 소스 트리와 제출용 source zip에 포함하지 않고 이후 커밋에서도 재추가하지 않습니다.

최종 보고서 문장을 대신하는 문서가 아니라, 팀이 구현 중 누락을 확인하고 PR/테스트/문서 위치를 이어 붙이기 위한 관리 문서입니다. 구현이 진행되면 관련 PR, 테스트 클래스, 스크린샷 위치를 계속 갱신합니다.

## 기준 일정

| 단계 | 마일스톤 | 목표 | GitHub 마일스톤 |
| --- | --- | --- | --- |
| M1 | 요구사항 및 설계 기준선 | 요구사항, 유스케이스, 도메인 모델, SSD, Operation Contract 기준선 | [M1](https://github.com/marcellokim/se-issue-tracker/milestone/7) |
| M2 | 핵심 도메인 및 영속 저장소 | 핵심 모델, 서비스, DB 저장소, 검색/상태/통계/추천 | [M2](https://github.com/marcellokim/se-issue-tracker/milestone/8) |
| M3 | 다중 UI 및 데모 흐름 | JavaFX + Swing UI, PDF 데모 시나리오 실행 | [M3](https://github.com/marcellokim/se-issue-tracker/milestone/9) |
| M4 | 검증 및 제출 패키지 | JUnit, 최종 문서, 발표/영상, 제출 패키지 | [M4](https://github.com/marcellokim/se-issue-tracker/milestone/10) |

## 기능 요구사항 추적

| PDF 요구사항 | 현재 작업 단위 | 확인할 산출물 |
| --- | --- | --- |
| 계정 추가: admin, PL, dev, tester | [#16](https://github.com/marcellokim/se-issue-tracker/issues/16) 계정, 역할, 프로젝트 기본 모델 구현 | Account/Role/Project 모델, 초기 데이터, 단위 테스트 |
| demo용 계정: admin, PL1, PL2, dev1~10, tester1~5 | [#18](https://github.com/marcellokim/se-issue-tracker/issues/18) DB 기반 영속 저장소와 데모 초기 데이터 준비 | seed 생성 코드, DB schema, 데모 실행 절차 |
| Admin이 project1 추가 | [#16](https://github.com/marcellokim/se-issue-tracker/issues/16), [#18](https://github.com/marcellokim/se-issue-tracker/issues/18) | Project 모델/저장소, 초기 project1 데이터 |
| 이슈 등록 | [#19](https://github.com/marcellokim/se-issue-tracker/issues/19) 이슈 등록, 검색, 상세 조회, 코멘트 서비스 구현 | IssueService 등록 테스트, UI 등록 화면 |
| reporter 자동 저장 | [#17](https://github.com/marcellokim/se-issue-tracker/issues/17), [#19](https://github.com/marcellokim/se-issue-tracker/issues/19) | 등록 서비스 테스트, Issue 필드 검증 |
| reported date 자동 저장 | [#17](https://github.com/marcellokim/se-issue-tracker/issues/17), [#19](https://github.com/marcellokim/se-issue-tracker/issues/19) | 시간 처리 테스트, 문서화된 시간 기준 |
| 이슈 브라우즈 및 검색 | [#19](https://github.com/marcellokim/se-issue-tracker/issues/19) | assignee/status/reporter 검색 테스트, 목록 UI |
| 이슈 상세 정보 확인 | [#19](https://github.com/marcellokim/se-issue-tracker/issues/19) | 상세 조회 서비스, 댓글 history 표시 |
| 이슈 코멘트 추가 | [#17](https://github.com/marcellokim/se-issue-tracker/issues/17), [#19](https://github.com/marcellokim/se-issue-tracker/issues/19) | Comment 모델, 코멘트 누적 테스트 |
| 이슈 배정 | [#20](https://github.com/marcellokim/se-issue-tracker/issues/20) 이슈 배정과 상태 변경 흐름 구현 | assignee/verifier 지정 및 변경 테스트, PL 시나리오 |
| 이슈 상태 변경 | [#17](https://github.com/marcellokim/se-issue-tracker/issues/17), [#20](https://github.com/marcellokim/se-issue-tracker/issues/20), [#43](https://github.com/marcellokim/se-issue-tracker/issues/43), [#47](https://github.com/marcellokim/se-issue-tracker/issues/47) | 상태 전이 규칙, 잘못된 fixed 역전이, reopen 테스트 |
| fixer 기록 | [#20](https://github.com/marcellokim/se-issue-tracker/issues/20) | dev 작업 완료 시 fixer 저장 테스트 |
| 우선순위 blocker/critical/major/minor/trivial | [#17](https://github.com/marcellokim/se-issue-tracker/issues/17) | Priority enum/value object, 기본값 major 테스트 |
| 상태 new/assigned/fixed/resolved/closed/reopened/deleted | [#17](https://github.com/marcellokim/se-issue-tracker/issues/17), [#20](https://github.com/marcellokim/se-issue-tracker/issues/20), [#43](https://github.com/marcellokim/se-issue-tracker/issues/43), [#44](https://github.com/marcellokim/se-issue-tracker/issues/44), [#47](https://github.com/marcellokim/se-issue-tracker/issues/47) | IssueStatus 정의, 상태 전이 문서, soft-delete/FIFO 정리 테스트 |
| reporter의 title/description 수정 제한 | [#46](https://github.com/marcellokim/se-issue-tracker/issues/46) | assigned 전 수정 허용, assigned 이후 comment 보완 흐름 |
| PL 전용 priority 변경 | [#17](https://github.com/marcellokim/se-issue-tracker/issues/17), [#20](https://github.com/marcellokim/se-issue-tracker/issues/20) | 권한 검사, 변경 사유 comment/history |
| 이슈 dependency 관계 | [#45](https://github.com/marcellokim/se-issue-tracker/issues/45) | IssueDependency 모델, 순환 방지, 선행 이슈 해결 제약 테스트 |
| 일/월별 이슈 발생 통계 및 trend | [#21](https://github.com/marcellokim/se-issue-tracker/issues/21) 일/월별 이슈 통계와 trend 조회 구현 | 통계 서비스, 날짜 fixture, UI 표시 |
| closed/resolved 이력 기반 assignee/verifier 추천 | [#22](https://github.com/marcellokim/se-issue-tracker/issues/22) 해결 이력 기반 assignee 추천 기능 구현 | UC8 배정 후보 추천 서비스, 후보 3명 결과, 추천 근거 |
| persistent storage | [#18](https://github.com/marcellokim/se-issue-tracker/issues/18) | DB 저장/조회 테스트, schema/seed 설명 |
| MVC 및 UI/로직 분리 | [#15](https://github.com/marcellokim/se-issue-tracker/issues/15), [#23](https://github.com/marcellokim/se-issue-tracker/issues/23), [#24](https://github.com/marcellokim/se-issue-tracker/issues/24) | 패키지 구조, 클래스 다이어그램, 두 UI의 공통 서비스 사용 |
| 두 개 이상의 UI Toolkit | [#23](https://github.com/marcellokim/se-issue-tracker/issues/23), [#24](https://github.com/marcellokim/se-issue-tracker/issues/24) | JavaFX UI, Swing UI, 빌드/실행 캡처 |
| JUnit 테스트 | [#25](https://github.com/marcellokim/se-issue-tracker/issues/25) 모델, 서비스, 영속 저장소 JUnit 테스트 구성 | 테스트 코드, CI 결과, 테스트 목적 설명 |

## UC별 구현 인수 기준

상태 값은 현재 `origin/dev`와 열린 PR 기준으로 적는다. `완료`는 코드와 테스트 근거가 `dev`에 들어간 상태이고, `PR 대기`는 구현 또는 증빙은 있으나 아직 `dev`에 병합되지 않은 상태이다.

| UC | 인수 기준 | Controller/API 근거 | 주요 테스트 근거 | UI/문서 근거 | 현재 상태 |
| --- | --- | --- | --- | --- | --- |
| UC1 Register Issue | active 프로젝트 멤버가 title/description/priority로 이슈를 등록하고 reporter, reportedDate, NEW status가 기록됨 | `IssueController.registerIssue`, `IssueService.registerIssue` | `IssueCreationTest`, `IssueServiceTest`, `IssueControllerTest` | JavaFX `07-issue-list`, Swing `IssueListPanel`; PR 230, PR 255 | 완료 |
| UC2 Add Comment | 권한 있는 사용자가 이슈에 코멘트를 추가하고 writer/date/history가 기록됨 | `IssueController.addComment`, `viewComments`, `updateComment`, `deleteComment` | `CommentTest`, `IssueServiceTest`, `IssueControllerTest`, `IssueComment*Test` | JavaFX `11-comment`, Swing comment action; PR 235 | 완료 |
| UC3 Search / Browse Issues | 프로젝트 멤버는 역할 구분 없이 프로젝트 일반 이슈를 조회하고, reporter/assignee/verifier/status/priority/date 조건으로 검색하며 DELETED 이슈는 제외함. Swing 목록 UI는 keyword/status/priority 검색 범위를 대표 증거로 둠 | `IssueController.viewProjectIssues`, `IssueController.searchIssues` | `IssueServiceTest`, `IssueControllerTest`, `IssueListPresenterTest` | JavaFX `07-issue-list`, Swing issue list keyword/status/priority; PR 230, PR 255, PR 266 | 완료 |
| UC4 View Issue Detail | 사용자가 접근 가능한 이슈 상세, 댓글, 히스토리, 의존성, 가능한 action을 조회함 | `IssueController.viewIssueDetail`, `viewAvailableActions` | `IssueServiceTest`, `IssueControllerTest`, `IssueDetailPresenterTest` | JavaFX `08-issue-detail`, Swing issue detail; PR 231, PR 255 | 완료 |
| UC5 Assign / Update Issue Assignment | active PL이 NEW/REOPENED 이슈 배정, ASSIGNED 재배정, FIXED verifier 변경을 수행함 | `AssignmentController.startAssignment`, `assignIssue`, `reassignIssue`, `changeVerifier` | `AssignmentServiceTest`, `AssignmentControllerTest`, `IssueWorkflowServiceTest` | JavaFX `10-assignment`, Swing assignment dialog; PR 89, PR 234 | 완료 |
| UC6 Change Issue State | 역할과 상태 조건에 맞게 ASSIGNED/FIXED/RESOLVED/CLOSED/REOPENED 전이를 수행하고, unresolved blocking issue가 남은 FIXED 이슈의 RESOLVED 전이를 거부하며 사유 코멘트를 기록함 | `IssueStateController.changeStatus` | `IssueStateServiceTest`, `IssueStateControllerTest`, `IssueWorkflowTest` | JavaFX `09-status-change`, Swing status dialog; PR 89, PR 233 | 완료 |
| UC7 Manage Dependency | PL이 의존성을 추가/제거하고 동일 프로젝트, 자기참조, 순환, 중복 관계를 검사함. unresolved blocking issue 검사는 UC6 상태 전이 책임으로 분리함 | `IssueController.addDependency`, `removeDependency`, `viewProjectDependencies` | `IssueDependencyTest`, `IssueServiceTest`, `IssueDependency*Test` | JavaFX `12-dependency`, dependency graph; Swing dependency action; PR 236, PR 244, PR 251 | 완료 |
| UC8 Recommend Assignment Candidates | UC5 시작 시 이슈 상태와 완료 이력/유사도 기반 후보를 제공함 | `AssignmentController.startAssignment` | `AssignmentRecommendationServiceTest`, `KNNAssignmentRecommendationTest`, `AssignmentControllerTest` | JavaFX `10-assignment`, Swing assignment dialog; PR 180, PR 234 | 완료 |
| UC9 Manage Deleted Issue | active PL만 삭제 이슈 조회, NEW/CLOSED soft delete, restore, purge를 수행하고 DELETED 정보를 일반 목록에서 숨김 | `DeletedIssueController.viewDeletedIssues`, `deleteIssue`, `restoreIssue`, `purgeDeletedIssue` | `DeletedIssueServiceTest`, `DeletedIssueControllerTest`, `DeletedIssuePanelTest` | JavaFX `14-deleted-issue`, Swing deleted issue panel; PR 241, PR 252, PR 255 | 완료 |
| UC10 Statistics | 프로젝트 기준으로 DELETED 이슈를 제외한 일/월별 추이와 상태/우선순위 분포를 조회함 | `StatisticsController.viewStatistics` | `StatisticsServiceTest`, `StatisticsControllerTest`, `StatisticsPresenterTest` | JavaFX `15-statistics`, Swing statistics view; PR 237, PR 245 | 완료 |
| UC11 Log In | active 계정이 로그인하고 실패/로그아웃 시 세션 상태가 올바르게 처리됨 | `AuthenticationController.login`, `logout` | `AuthenticationServiceTest`, `AuthenticationControllerTest`, `LoginPresenterTest`, `LoginPanelTest` | JavaFX `01-login`, Swing login; PR 217, PR 221 | 완료 |
| UC12 Manage Accounts | ADMIN이 계정 생성, 이름 변경, 역할 변경, 활성/비활성 처리를 수행하고 책임자 계정 보호 규칙을 지킴 | `AccountController.createAccount`, `renameAccount`, `changeAccountRole`, `activateAccount`, `deactivateAccount` | `AccountServiceTest`, `AccountControllerTest`, `AccountManagement*Test` | JavaFX `03-account-manage`, Swing account management; PR 158, PR 222, PR 228 | 완료 |
| UC13 Manage Projects | ADMIN이 프로젝트 생성/수정/삭제와 참여자 추가/제거를 수행하고 active assignment 제약을 지킴 | `ProjectController.createProject`, `renameProject`, `changeProjectDescription`, `deleteProject`, `addProjectParticipant`, `removeProjectParticipant` | `ProjectServiceTest`, `ProjectControllerTest`, `ProjectManagement*Test`, `ProjectDetail*Test` | JavaFX `04-project-manage`, `05-project-detail`; Swing project management/detail; PR 118, PR 224, PR 227, PR 228 | 완료 |
| UC14 Verify Permission | protected operation이 로그인, 역할, active project membership, active PL/Admin 권한을 service/policy에서 검사함 | `PermissionPolicy` 호출 경로, 각 controller의 current user 요구 | `PermissionPolicyTest`, `ArchitectureBoundaryTest`, controller/service tests | API 명세의 권한/정책 섹션, `docs/ooad-grasp-mvc.md` | 완료 |
| UC15 Edit Issue | reporter가 NEW/REOPENED 이슈의 title/description을 수정하고 priority/status 변경은 별도 UC로 분리됨 | `IssueController.updateIssue` | `IssueEditTest`, `IssueServiceTest`, `IssueControllerTest`, `IssueEdit*Test` | JavaFX `13-issue-edit`, Swing issue edit/delete entry; PR 240, PR 252 | 완료 |
| UC16 Change Priority | active PL이 이슈 priority를 변경하고 변경 이력을 남김 | `IssueController.changePriority` | `IssueServiceTest`, `IssueControllerTest`, `IssueEditDialogsTest` | JavaFX `13-issue-edit`, Swing issue edit action; PR 158, PR 240, PR 252 | 완료 |

## Open issue / PR 추적

| 이슈 | 닫는 범위 | 연결 UC/산출물 | 현재 판단 |
| --- | --- | --- | --- |
| [#24](https://github.com/marcellokim/se-issue-tracker/issues/24) Swing 전체 UI 데모 흐름 parent | Swing sub-issue와 데모 가능한 전체 UI 관리 | UC1~UC16 Swing presentation | 구현 sub-issue와 QA PR은 `dev`에 병합됨. #246의 발표 장비 수동 재확인만 남음 |
| [#25](https://github.com/marcellokim/se-issue-tracker/issues/25) 테스트 gate | 모델, 서비스, 영속 저장소, controller, UI 테스트 증빙 | JUnit 제출 요구사항 | 최신 `dev`에서 `./gradlew check verifySubmissionMetadata`와 CI Oracle/SonarCloud workflow 통과. 별도 SonarCloud App coverage check는 branch protection 필수 항목이 아님 |
| [#26](https://github.com/marcellokim/se-issue-tracker/issues/26) 최종 제출 패키지 gate | README.txt, source zip, 실행/검증 절차 | 소스 제출 산출물 | package exclude 정책과 README.txt 템플릿은 반영됨. 최종 PDF/slide/video는 별도 제출물로 관리 |
| [#27](https://github.com/marcellokim/se-issue-tracker/issues/27) GitHub Project 증빙 | Project/milestone/issue/PR/CI 캡처 | 협업 및 진행 이력 증빙 | REST/local evidence 기준으로 최종 캡처 목록을 관리함. 제출 직전 보드, milestone, issue, PR, CI 화면을 다시 캡처 |
| [#246](https://github.com/marcellokim/se-issue-tracker/issues/246) Swing acceptance smoke/evidence | Oracle local 실행, role별 route smoke, screenshot/evidence 경로 | Swing demo evidence | `docs/qa/swing-full-qa-2026-06-01.md`에 기능 QA 반영. 발표 장비의 마우스 포커스/시각 점검만 수동 확인 필요 |

## 제출 전 남은 gate

| Gate | 완료 조건 | 현재 필요한 다음 행동 |
| --- | --- | --- |
| Swing 발표 장비 재확인 | 로그인 마우스 포커스와 800x600 주요 화면 육안 확인 | #246에 남은 target desktop retest 결과를 기록 |
| 제출 패키지 제외 정책 | QA artifact, 개인 IDE 설정, 과제/강의 원문, 개인 작업 메모 파일이 zip에서 제외됨 | 최종 `package-submission.sh` 실행으로 산출물 이름과 README.txt 확인 |
| Traceability freeze | UC별 구현/API/test/UI evidence가 한 문서에서 추적됨 | #107은 병합 완료. 최종 PDF로 옮길 때 표 길이만 조정 |
| Test gate | `./gradlew check`, `verifySubmissionMetadata`, CI 결과 확보 | 최신 `dev` 로컬 검증과 CI Oracle/SonarCloud workflow는 통과. 별도 SonarCloud App coverage check는 참고 항목으로 기록 |
| Final package | README.txt, PDF, slides, demo video, source/executable/test/data zip | #100/#101/#26에서 PDF/slide/video/package 생성 |

## 문서/제출 요구사항 추적

| PDF 요구사항 | 현재 작업 단위 | 확인할 산출물 |
| --- | --- | --- |
| GitHub 프로젝트 페이지 생성 및 수행 기록 | [#27](https://github.com/marcellokim/se-issue-tracker/issues/27) GitHub Project 자동 등록과 진행 이력 캡처 준비 | Project URL, 진행 이력 스크린샷, PR/이슈 기록 |
| 요구 정의 및 분석 | [#13](https://github.com/marcellokim/se-issue-tracker/issues/13), [#14](https://github.com/marcellokim/se-issue-tracker/issues/14) | 요구사항 추적표, 유스케이스 문서 |
| 전체 유스케이스 다이어그램 | [#14](https://github.com/marcellokim/se-issue-tracker/issues/14) | 유스케이스 다이어그램 이미지/원본 |
| include 2개 이상, extend 2개 이상 | [#14](https://github.com/marcellokim/se-issue-tracker/issues/14) | 다이어그램 및 명세 내 관계 설명 |
| 유스케이스 명세 6개 | [#14](https://github.com/marcellokim/se-issue-tracker/issues/14) | `docs/use-cases/` 명세 6개 |
| 도메인 모델 | [#15](https://github.com/marcellokim/se-issue-tracker/issues/15) | 도메인 모델 다이어그램/설명 |
| SSD 2개 | [#15](https://github.com/marcellokim/se-issue-tracker/issues/15) | SSD 2개 |
| Operation Contract 2개 이상 | [#15](https://github.com/marcellokim/se-issue-tracker/issues/15) | Operation Contract 2개 이상 |
| 클래스/시퀀스 다이어그램 및 설계 설명 | [#15](https://github.com/marcellokim/se-issue-tracker/issues/15), [#23](https://github.com/marcellokim/se-issue-tracker/issues/23), [#24](https://github.com/marcellokim/se-issue-tracker/issues/24) | 설계 UML, MVC 설명, UI 분리 근거 |
| OOAD/GRASP/설계 원칙 설명 | [#15](https://github.com/marcellokim/se-issue-tracker/issues/15), [#39](https://github.com/marcellokim/se-issue-tracker/issues/39) | 설계 결정 기록, 클래스 책임 설명 |
| 구현 결과 및 스크린샷 | [#23](https://github.com/marcellokim/se-issue-tracker/issues/23), [#24](https://github.com/marcellokim/se-issue-tracker/issues/24), [#26](https://github.com/marcellokim/se-issue-tracker/issues/26) | 화면 캡처, 데모 순서 |
| 테스트 수행 내역 | [#25](https://github.com/marcellokim/se-issue-tracker/issues/25), [#26](https://github.com/marcellokim/se-issue-tracker/issues/26) | 테스트 코드, 테스트 목적 설명 |
| 최종 README.txt와 소스 zip | [#26](https://github.com/marcellokim/se-issue-tracker/issues/26) | 소스 zip, README.txt |
