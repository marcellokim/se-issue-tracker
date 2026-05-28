# ProjectController API

## Scope

`ProjectController` exposes ADMIN project-management APIs. The static `create(...)` method is a factory; the remaining public methods are UI-facing operations.

## Public Operations

| Operation | Service call | Result |
| --- | --- | --- |
| `create(authenticationService, projectService)` | `new ProjectController(...)` | `ProjectController` |
| `viewProjects()` | `ProjectService.viewProjects(currentUserId)` | `List<ProjectResult>` |
| `viewProject(projectId)` | `ProjectService.viewProject(projectId, currentUserId)` | `ProjectResult` |
| `viewProjectParticipants(projectId)` | `ProjectService.viewProjectParticipants(projectId, currentUserId)` | `List<ProjectMemberResult>` |
| `viewProjectDetail(projectId)` | `ProjectService.viewProjectDetail(projectId, currentUserId)` | `ProjectDetail` |
| `createProject(name, description)` | `ProjectService.createProject(name, description, currentUserId)` | `ProjectResult` |
| `deleteProject(projectId)` | `ProjectService.deleteProject(projectId, currentUserId)` | `void` |
| `addProjectParticipant(projectId, loginId)` | `ProjectService.addProjectParticipant(projectId, loginId, currentUserId)` | `void` |
| `removeProjectParticipant(projectId, loginId)` | `ProjectService.removeProjectParticipant(projectId, loginId, currentUserId)` | `void` |

## Operation Details

All UI operations require a current user and pass `user.getLoginId()` to the service. `ProjectResult` contains project id, name, description, manager login id, created date, and updated time. `ProjectDetail` combines project, participants, and issues.

Project operations require positive project ids. Project creation rejects blank and duplicate names. Adding participants rejects inactive users, ADMIN users, duplicates, and a second PL. Removing participants rejects unknown participants and users with active assignment/verifier responsibility in the project.

## UC/OC/DCD Traceability

| API | UC/SSD/OC | DCD/domain evidence |
| --- | --- | --- |
| `createProject` | UC13, SSD-19 create project; supporting API, not required OC | `Project` class and Admin manages association in `docs/uml/dcd/its_dcd_ver2.puml`; implementation `Project.create`, `ProjectResult.from` |
| `addProjectParticipant` | UC13, SSD-20 add project member; supporting API, not required OC | DCD `User participates in Project`; implementation `ProjectRepository.addParticipant`, `ProjectMemberResult` |
| `removeProjectParticipant` | UC13, SSD-21 remove project member; supporting API, not required OC | DCD `User participates in Project`; implementation `ProjectRepository.removeParticipant`, active assignment guard |
| view/delete project operations | UC13 admin support APIs | DCD `Project`, `Issue` composition/reference by `projectId`; implementation `ProjectService.viewProjectDetail`, `deleteProject` |

## Implementation And Design Gaps

| Classification | Detail |
| --- | --- |
| `implementation-extra` | Project administration is implemented but not listed in `required_operation_contracts.md`. |

## Permission And Failure Summary

- Requires login at controller boundary.
- Requires `PermissionPolicy.assertCanManageProject`, which allows only active ADMIN users.
- Throws `IllegalArgumentException` for non-positive project ids, missing projects/users, blank names, duplicate names/participants, inactive users, ADMIN participant, second PL, and active assignment responsibility on removal.
- Throws `SecurityException` when actor is not ADMIN.

## Evidence

- `src/main/java/com/github/marcellokim/issuetracker/controller/ProjectController.java`: `ProjectController.create`, `viewProjects`, `viewProject`, `viewProjectParticipants`, `viewProjectDetail`, `createProject`, `deleteProject`, `addProjectParticipant`, `removeProjectParticipant`, `requireCurrentUser`
- `src/main/java/com/github/marcellokim/issuetracker/service/ProjectService.java`: matching `ProjectService` methods
- `src/main/java/com/github/marcellokim/issuetracker/service/PermissionPolicy.java`: `PermissionPolicy.assertCanManageProject`
- `src/main/java/com/github/marcellokim/issuetracker/service/ProjectResult.java`: `ProjectResult`
- `src/main/java/com/github/marcellokim/issuetracker/service/ProjectMemberResult.java`: `ProjectMemberResult`
- `src/main/java/com/github/marcellokim/issuetracker/service/ProjectDetail.java`: `ProjectDetail`
