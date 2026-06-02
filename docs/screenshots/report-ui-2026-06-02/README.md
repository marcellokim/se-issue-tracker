# 보고서용 UI 스크린샷

최종 보고서 구현 결과 파트에 사용할 JavaFX/Swing 화면 캡처 모음이다. 두 UI는 같은 기능 범위를 비교할 수 있도록 동일한 순서와 파일명으로 정리하였다.

| 순서 | 기능 | JavaFX | Swing | 보고서 연결 포인트 |
| --- | --- | --- | --- | --- |
| 01 | 로그인 | [01-login.png](javafx/01-login.png) | [01-login.png](swing/01-login.png) | 시스템 실행 진입점, 계정별 로그인 |
| 02 | Admin 대시보드 | [02-admin-dashboard.png](javafx/02-admin-dashboard.png) | [02-admin-dashboard.png](swing/02-admin-dashboard.png) | Admin이 프로젝트/계정 관리자인 점 |
| 03 | 프로젝트 참여자 관리 | [03-project-participants.png](javafx/03-project-participants.png) | [03-project-participants.png](swing/03-project-participants.png) | 프로젝트 기반 권한과 참여자 관리 |
| 04 | 프로젝트 목록 | [04-project-list.png](javafx/04-project-list.png) | [04-project-list.png](swing/04-project-list.png) | Non-admin 사용자의 프로젝트 접근 범위 |
| 05 | 프로젝트 이슈 목록 | [05-project-issue-list.png](javafx/05-project-issue-list.png) | [05-project-issue-list.png](swing/05-project-issue-list.png) | 프로젝트 멤버의 일반 이슈 조회 |
| 06 | 이슈 등록 | [06-register-issue-form.png](javafx/06-register-issue-form.png) | [06-register-issue-form.png](swing/06-register-issue-form.png) | UC1 Register Issue |
| 07 | 이슈 상세 | [07-issue-detail.png](javafx/07-issue-detail.png) | [07-issue-detail.png](swing/07-issue-detail.png) | UC4 View Issue Detail, 댓글/이력/가능 액션 |
| 08 | 이슈 배정 | [08-assignment-form.png](javafx/08-assignment-form.png) | [08-assignment-form.png](swing/08-assignment-form.png) | UC5 Assignment, 추천 후보와 전체 후보 |
| 09 | 상태 변경 | [09-status-change-form.png](javafx/09-status-change-form.png) | [09-status-change-form.png](swing/09-status-change-form.png) | UC6 Change Issue State, 사유 댓글 |
| 10 | 의존성 관리 | [10-dependency-detail.png](javafx/10-dependency-detail.png) | [10-dependency-detail.png](swing/10-dependency-detail.png) | UC7 Manage Dependency |
| 11 | 삭제 이슈 관리 | [11-deleted-issues.png](javafx/11-deleted-issues.png) | [11-deleted-issues.png](swing/11-deleted-issues.png) | UC9 Deleted Issue Management |
| 12 | 통계 | [12-statistics.png](javafx/12-statistics.png) | [12-statistics.png](swing/12-statistics.png) | UC10 Statistics, 상태/우선순위 분포 |

## 사용 기준

- 보고서에는 기능별로 1장씩 선별하고, JavaFX/Swing 비교가 필요한 기능은 두 장을 나란히 배치한다.
- 이슈 등록, 배정, 상태 변경, 삭제 이슈 관리는 입력 화면과 결과 설명을 함께 붙이면 UC/SSD/OC 설명과 연결하기 쉽다.
- 로그인, 프로젝트 조회, 상세 조회, 통계는 결과 화면 중심으로 사용한다.
