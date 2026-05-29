# ProjectController API

## 범위

`ProjectController`는 프로젝트 상세 조회와 ADMIN 프로젝트 관리 API를 제공한다. ADMIN은 프로젝트 기본 정보와 참여자 정보를 조회하고 프로젝트를 관리한다. non-ADMIN 사용자는 자신이 참여한 프로젝트의 기본 정보만 조회하며, 해당 프로젝트의 관련 이슈 목록은 `IssueController.viewRelatedProjectIssues`에서 조회한다.

## 공개 오퍼레이션

| 오퍼레이션 | 서비스 호출 | 결과 |
| --- | --- | --- |
| `viewProjectNonAdminDetail(projectId)` | `ProjectService.viewProjectNonAdminDetail(projectId, currentUserId)` | `ProjectResult` |
| `viewProjectAdminDetail(projectId)` | `ProjectService.viewProjectAdminDetail(projectId, currentUserId)` | `ProjectAdminDetail` |
| `viewProjectParticipants(projectId)` | `ProjectService.viewProjectParticipants(projectId, currentUserId)` | `List<ProjectMemberResult>` |
| `createProject(name, description)` | `ProjectService.createProject(name, description, currentUserId)` | `ProjectResult` |
| `renameProject(projectId, name)` | `ProjectService.renameProject(projectId, name, currentUserId)` | `ProjectResult` |
| `changeProjectDescription(projectId, description)` | `ProjectService.changeProjectDescription(projectId, description, currentUserId)` | `ProjectResult` |
| `deleteProject(projectId)` | `ProjectService.deleteProject(projectId, currentUserId)` | `void` |
| `addProjectParticipant(projectId, loginId)` | `ProjectService.addProjectParticipant(projectId, loginId, currentUserId)` | `void` |
| `removeProjectParticipant(projectId, loginId)` | `ProjectService.removeProjectParticipant(projectId, loginId, currentUserId)` | `void` |

## 오퍼레이션 상세

모든 오퍼레이션은 현재 로그인한 사용자를 요구하고, 컨트롤러는 `user.getLoginId()`를 서비스로 전달한다. 세션이 없으면 `SecurityException("Login is required.")`를 던진다.

`ProjectResult`는 프로젝트 id, 이름, 설명, 관리자 login id, 생성일, 수정일을 포함한다.

`viewProjectNonAdminDetail`은 active non-ADMIN 프로젝트 참여자 전용 조회 API이다. ADMIN이 이 메서드를 호출하면 실패하며, ADMIN은 `viewProjectAdminDetail`을 사용해야 한다. 이 메서드는 프로젝트 기본 정보만 반환하고 이슈 목록은 반환하지 않는다.

`viewProjectAdminDetail`은 active ADMIN 전용 조회 API이다. 프로젝트 기본 정보와 참여자 목록을 함께 반환한다. ADMIN 프로젝트 화면에서는 이슈 목록을 보여주지 않는 정책이므로 `ProjectAdminDetail`에는 이슈 목록이 포함되지 않는다.

`viewProjectParticipants`는 ADMIN 전용 참여자 목록 조회 API이다. `viewProjectAdminDetail`에도 참여자 목록이 포함되어 있지만, 참여자 목록만 별도로 조회해야 하는 화면 또는 테스트 경로를 위해 유지된다.

프로젝트 생성과 수정 기능은 모두 ADMIN 전용이다. 프로젝트 생성은 blank name, blank description, 중복 name을 거부한다. 프로젝트 이름 변경은 blank name, 다른 프로젝트와 중복되는 name, 현재 이름과 같은 name을 거부한다. 프로젝트 설명 변경은 blank description과 현재 설명과 같은 description을 거부한다.

프로젝트 삭제는 ADMIN 전용이며, 존재하는 프로젝트에 대해서만 수행된다.

참여자 추가는 ADMIN 전용이다. 대상 사용자는 존재해야 하고 active 상태여야 하며, ADMIN role 사용자는 프로젝트 참여자로 추가할 수 없다. 이미 참여 중인 사용자는 중복 추가할 수 없고, 한 프로젝트에는 PL을 한 명만 배정할 수 있다.

참여자 제거는 ADMIN 전용이다. 대상 사용자가 해당 프로젝트 참여자가 아니면 실패한다. 해당 프로젝트의 `ASSIGNED` 또는 `FIXED` 상태 이슈에서 현재 assignee/verifier 책임을 가진 사용자는 제거할 수 없다. PL 참여자 제거 자체는 금지하지 않으며, 이후 ADMIN이 프로젝트 참여자를 다시 조정하는 정책이다.

## UC/OC/DCD 추적성

| API | UC/SSD/OC | DCD/domain 근거 |
| --- | --- | --- |
| `createProject` | UC13, SSD-19 create project; 필수 OC 목록에는 없는 보조 API | `docs/uml/dcd/its_dcd_ver2.puml`의 `Project` 클래스와 Admin manages 연관; 구현 `Project.create`, `ProjectResult.from` |
| `renameProject`, `changeProjectDescription` | UC13 프로젝트 관리 보조 API | `Project.rename`, `Project.changeDescription`, `ProjectResult.from` |
| `addProjectParticipant` | UC13, SSD-20 add project member; 필수 OC 목록에는 없는 보조 API | DCD `User participates in Project`; 구현 `ProjectRepository.addParticipant`, `ProjectMemberResult` |
| `removeProjectParticipant` | UC13, SSD-21 remove project member; 필수 OC 목록에는 없는 보조 API | DCD `User participates in Project`; 구현 `ProjectRepository.removeParticipant`, active assignment guard |
| `viewProjectAdminDetail`, `viewProjectNonAdminDetail`, `viewProjectParticipants`, `deleteProject` | UC13 project support APIs | DCD `Project` identity와 membership; 구현 `ProjectService.viewProjectAdminDetail`, `viewProjectNonAdminDetail`, `viewProjectParticipants`, `deleteProject` |

## 구현 및 설계 차이

| 분류 | 내용 |
| --- | --- |
| `implementation-extra` | 프로젝트 관리는 구현되어 있지만 `required_operation_contracts.md`에는 나열되어 있지 않다. |
| `ui-scope` | ADMIN 프로젝트 상세는 이슈 목록을 포함하지 않는다. non-ADMIN 프로젝트 화면의 관련 이슈 목록은 `IssueController.viewRelatedProjectIssues`에서 별도로 조회한다. |

## 권한 및 실패 요약

- 컨트롤러 경계에서 로그인이 필요하다.
- 모든 project id는 양수여야 하며, 0 이하이면 `IllegalArgumentException`을 던진다.
- `viewProjectNonAdminDetail`은 active non-ADMIN 프로젝트 참여자만 허용한다.
- `viewProjectAdminDetail`, `viewProjectParticipants`, 프로젝트 생성/수정/삭제, 참여자 추가/제거는 active ADMIN만 허용한다.
- 프로젝트 또는 사용자를 찾을 수 없으면 `IllegalArgumentException`을 던진다.
- 프로젝트 name 또는 description이 null/blank이면 `IllegalArgumentException`을 던진다.
- 프로젝트 name 중복, 현재 name과 같은 이름 변경, 현재 description과 같은 설명 변경은 `IllegalArgumentException`을 던진다.
- 참여자 추가 시 inactive user, ADMIN user, 중복 참여자, 두 번째 PL은 `IllegalArgumentException`을 던진다.
- 참여자 제거 시 대상이 프로젝트 참여자가 아니면 `IllegalArgumentException`을 던진다.
- 참여자 제거 시 대상이 해당 프로젝트의 `ASSIGNED` 또는 `FIXED` 이슈에서 현재 assignee/verifier이면 `IllegalArgumentException`을 던진다.
- 권한이 맞지 않으면 `SecurityException`을 던진다.

## 근거

- `src/main/java/com/github/marcellokim/issuetracker/controller/ProjectController.java`: `ProjectController.viewProjectNonAdminDetail`, `viewProjectAdminDetail`, `viewProjectParticipants`, `createProject`, `renameProject`, `changeProjectDescription`, `deleteProject`, `addProjectParticipant`, `removeProjectParticipant`, `requireCurrentUser`
- `src/main/java/com/github/marcellokim/issuetracker/service/ProjectService.java`: matching `ProjectService` methods
- `src/main/java/com/github/marcellokim/issuetracker/service/PermissionPolicy.java`: `PermissionPolicy.assertCanManageProject`
- `src/main/java/com/github/marcellokim/issuetracker/service/ProjectResult.java`: `ProjectResult`
- `src/main/java/com/github/marcellokim/issuetracker/service/ProjectMemberResult.java`: `ProjectMemberResult`
- `src/main/java/com/github/marcellokim/issuetracker/service/ProjectAdminDetail.java`: `ProjectAdminDetail`
