# 기본 가정

이 문서는 텀 프로젝트 초기에 합의한 기본 가정을 정리합니다. 구현이 진행되면서 바뀌면 PR과 함께 갱신합니다.

## 상태 전이 정의
상태 이름은 두 층에서 분리해서 사용합니다.

### 애플리케이션 이슈 상태
과제 PDF의 최소 상태 목록과 팀 회의 확정 상태를 함께 반영합니다.

기본 흐름:

`new -> assigned -> fixed -> resolved -> closed`

보조 상태/흐름:
- `fixed`는 Dev가 수정 완료를 주장한 중간 상태입니다.
- Tester가 fixed 이슈를 검증 성공하면 `resolved`로 전이합니다.
- Tester가 fixed 이슈를 검증 실패하면 `assigned`로 되돌리고 실패 사유를 comment/history에 남깁니다.
- `reopened`는 resolved/closed 이슈를 PL이 다시 작업 대상으로 판단할 때 사용합니다. Reopen 후에는 PL이 assignee를 지정해 `assigned` 상태부터 재작업을 시작합니다.
- `deleted`는 불필요한 이슈의 soft-delete 상태입니다. deleted 이슈가 30개를 초과하면 deleted 전이 시각 기준 FIFO로 오래된 이슈부터 물리 삭제합니다.

### 권한 및 수정 정책
- User당 직군/역할은 하나만 부여합니다.
- Reporter는 assigned 전까지만 자신의 이슈 title/description을 수정할 수 있습니다.
- assigned 이후 title/description 정정과 추가 정보는 comment로 남깁니다.
- Priority는 PL만 변경할 수 있으며, assigned 상태와 무관하게 변경 가능합니다.
- 이슈 dependency 관계는 comment가 아니라 구조화된 데이터로 관리합니다.

### GitHub Project 작업 상태
GitHub Project에서 팀 작업을 관리할 때는 다음 흐름을 사용합니다.

`대기 -> 준비됨 -> 진행 중 -> 리뷰 중 -> 완료`

보조 규칙:
- 구현 전에는 가능한 한 GitHub 이슈를 생성합니다.
- PR이 열리면 Project 상태는 `리뷰 중`으로 이동합니다.
- PR이 머지되고 관련 이슈가 닫히면 `완료`로 이동합니다.

## 저장소 방식 (DB 기반 영속 저장소)
과제 원문은 File System 또는 DBMS 사용을 모두 허용하지만, 이 저장소의 표준 구현은 DB 기반 persistence로 고정합니다.

- 외부 서버가 필요 없는 내장형 DB를 우선합니다.
- Java 구현에서는 JDBC repository 계층을 표준 경계로 둡니다.
- schema, seed 데이터, 예외 처리 정책은 #18 구현과 함께 문서화합니다.

## UI 툴킷
초기 가정 UI 구성은 다음과 같습니다.

- 사용자용 메인 UI: JavaFX
- 관리자/보조 도구 UI: Swing

혼합 사용 시 공통 도메인 모델과 서비스 계층은 UI 프레임워크와 분리합니다.

## 추천 방식 (휴리스틱)
추천 기능은 초기에는 휴리스틱 기반으로 구현한다고 가정합니다.

예시 기준:
- 우선순위
- 최근 활동 여부
- 상태
- 담당 가능성 또는 관련 태그

초기 단계에서는 설명 가능한 규칙 기반 접근을 우선하고, 추후 필요 시 개선합니다.

## 권한 가정
초기 권한 모델은 단순화합니다.

- 일반 사용자
- 관리자

필요 시 아래 항목을 확장합니다.
- 생성/수정/삭제 권한
- 상태 변경 권한
- 관리자 전용 화면 접근 권한
