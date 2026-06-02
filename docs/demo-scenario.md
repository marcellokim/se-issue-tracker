# Demo Scenario

이 문서는 발표 슬라이드와 프로젝트 소개 영상에서 사용할 데모 흐름을 고정한다. 목표는 구현 담당자가 순서대로 설명하지 않아도 문제, 설계, 구현, 검증, 협업 증거가 하나의 흐름으로 보이게 만드는 것이다.

## Scope and Non-goals

### Scope

- 발표 스토리라인과 10분 발표 흐름을 정리한다.
- 30분 이내 프로젝트 소개 영상 촬영 순서를 정리한다.
- JavaFX와 Swing을 각각 완성된 UI surface로 보여 주되, UI 밖의 controller, service, domain, repository 계층을 공유한다는 점을 증명한다.
- Oracle persistence, 권한 검사, 상태 전이, 추천, 통계, 삭제 이슈 관리가 데모에서 빠지지 않게 한다.
- 실패 시 사용할 fallback demo path와 예상 질문 답변을 준비한다.

### Non-goals

- UI 구현, UI 디자인 수정, 최종 영상 편집은 이 문서 범위가 아니다.
- 최종 PDF 본문과 traceability matrix 표를 이 문서에 복제하지 않는다.
- 발표 슬라이드의 최종 시각 디자인은 별도 산출물에서 마감한다.
- 스크린샷 파일과 QA 이미지 증거는 이 문서 PR에 포함하지 않는다.

## Source Material

| 목적 | 기준 문서 |
|---|---|
| UI 흐름 | `docs/ui/javaFX/UI_navigation_map/javaFX_navigation_map.md` |
| UC 데모 매핑 | `docs/use-cases/use_case_specifications.md` |
| 실행 DB 준비 | `docs/local-oracle-testing.md` |
| seed 계정 | `docs/oracleDB-seed-password.md` |
| 설계 방어 포인트 | `docs/ooad-grasp-mvc.md` |
| 제출물 체크 | `README.md` |

## Presentation Storyline

### 10-minute presentation path

| 시간 | 내용 | 증거 |
|---|---|---|
| 0:00-1:00 | 과제 목표와 핵심 요구사항 | UC 목록, 두 UI toolkit 요구 |
| 1:00-2:30 | 아키텍처 결정 | MVC, controller/service/domain/repository 분리 |
| 2:30-5:30 | 핵심 기능 데모 | 이슈 등록, 검색, 배정, 상태 전이, 댓글 |
| 5:30-7:00 | 심화 기능 데모 | 추천, 의존성, 삭제 이슈, 통계 |
| 7:00-8:20 | JavaFX/Swing 재사용 증거 | 같은 계정과 seed data로 같은 흐름 실행 |
| 8:20-9:20 | 검증 증거 | JUnit, CI, Oracle 통합 테스트, QA 기록 |
| 9:20-10:00 | 협업 및 마감 상태 | GitHub Project, PR, issue close evidence |

### 30-minute video path

1. 프로젝트 목표와 제약을 설명한다.
2. domain model, logical architecture, DCD 중 핵심 다이어그램만 짧게 짚는다.
3. Oracle fixed seed로 같은 시작 상태를 만든다.
4. JavaFX에서 primary demo path를 한 번 실행한다.
5. Swing에서 같은 업무 흐름이 동일한 backend 계약을 사용한다는 점을 보여 준다.
6. 권한 실패와 상태 전이 실패를 한 가지씩 보여 준다.
7. 추천, 통계, 삭제 이슈 관리처럼 구현 난도가 높은 기능을 따로 보여 준다.
8. 테스트와 CI 결과, GitHub Project evidence를 제시한다.
9. 남은 리스크가 없거나 제출 범위 밖인 항목을 명확히 구분한다.

## Presenter Role Split

| 담당 | 발표 내용 | 준비물 |
|---|---|---|
| marcellokim | Swing UI, 제출 패키지, QA evidence | Swing 실행 화면, QA report 요약, package command |
| mckimR1972 | 최종 문서, traceability, 발표 흐름 | PDF 목차, UC별 구현 매핑, 슬라이드 초안 |
| msh0678 | JavaFX UI, 상태 전이, 추천/통계 구현 | JavaFX 실행 화면, 상태 전이 사례, 추천/통계 seed data |

담당은 발표 리허설 전 실제 PR/issue 담당자와 맞춰 조정한다. 발표자가 바뀌어도 아래 demo path와 command contract는 유지한다.

## Environment and Commands

### Oracle local setup

```bash
./gradlew oracleLocalStart --console=plain
./gradlew oracleLocalResetFixedSeed --console=plain

export ITS_DB_URL="jdbc:oracle:thin:@//localhost:1521/FREEPDB1"
export ITS_DB_USER="ITS_USER"
export ITS_DB_PASSWORD="ItsLocalDev2026!"

./gradlew oracleConnectionCheck --console=plain
```

기존 DB 상태를 유지하며 schema만 확인할 때는 다음 명령을 사용한다. 리허설이나 영상 촬영처럼 고정 seed가 필요한 상황에서는 위의 `oracleLocalResetFixedSeed`를 우선한다.

```bash
./gradlew oracleLocalInitializeDatabase --console=plain
```

### UI run commands

```bash
# JavaFX
./gradlew run --console=plain

# Swing
./gradlew runSwing --console=plain
```

### Verification commands

```bash
./gradlew check --console=plain
./gradlew verifySubmissionMetadata --console=plain
./gradlew oracleLocalTest --console=plain
./scripts/package-submission.sh \
  --team-number 03 \
  --member "MEMBER_1" \
  --member "MEMBER_2" \
  --member "MEMBER_3" \
  --project-url "https://github.com/users/marcellokim/projects/1"
```

패키징 명령의 팀원 이름과 프로젝트 URL은 제출 직전 최종 값으로 채운다. Oracle local 실행이 어려운 환경에서는 CI의 `Oracle 통합 테스트` job과 `oracle-통합-테스트-리포트` artifact를 최종 검증 증거로 연결한다.

## Seed Accounts

| Actor | Login ID | Initial password |
|---|---|---|
| Admin | `admin` | `DemoLocalAdmin!` |
| PL | `pl1`, `pl2` | `DemoLocalPl1!`, `DemoLocalPl2!` |
| Developer | `dev1` - `dev10` | `DemoLocalDev1!` - `DemoLocalDev10!` |
| Tester | `tester1` - `tester5` | `DemoLocalTester1!` - `DemoLocalTester5!` |

위 계정은 로컬 데모 seed용이다. DB에는 평문이 아니라 PBKDF2 기반 salt/hash 값만 저장된다.

## Primary Demo Path

### 1. Admin setup

| 단계 | Actor | 화면 | 말할 포인트 |
|---|---|---|---|
| 로그인 | Admin | 로그인 | 계정별 권한이 session과 controller 경계에서 확인됨 |
| 프로젝트 확인 | Admin | 프로젝트 관리 | Admin은 프로젝트와 멤버를 관리하지만 일반 이슈 목록을 직접 다루지 않음 |
| 멤버 관리 | Admin | 프로젝트 상세/멤버 관리 | PL, Dev, Tester 역할이 이후 권한 흐름의 기준이 됨 |

### 2. Tester reports an issue

| 단계 | Actor | 화면 | 말할 포인트 |
|---|---|---|---|
| 프로젝트 진입 | tester1 | 프로젝트 목록 | 참여 프로젝트만 조회됨 |
| 이슈 등록 | tester1 | 이슈 목록/등록 | UC1 Register Issue |
| 코멘트 추가 | tester1 | 이슈 상세 | UC2 Add Comment |

### 3. PL triage and assignment

| 단계 | Actor | 화면 | 말할 포인트 |
|---|---|---|---|
| 검색 | pl1 | 이슈 목록 | 상태, 우선순위, 제목 조건으로 이슈를 찾음 |
| 상세 확인 | pl1 | 이슈 상세 | 상세 정보, history, comment, dependency를 함께 확인 |
| 배정 | pl1 | 배정 dialog | 추천 후보는 과거 완료 이력, 상태, 이슈 내용 유사도를 반영 |
| 의존성 | pl1 | 의존성 action | 순환 또는 잘못된 상태 조합을 서비스/도메인 쪽에서 막음 |

### 4. Dev and Tester state transition

| 단계 | Actor | 화면 | 말할 포인트 |
|---|---|---|---|
| 작업 확인 | dev1 | 이슈 목록/상세 | 자신에게 배정된 이슈를 확인 |
| fixed 처리 | dev1 | 상태 변경 dialog | ASSIGNED에서 FIXED로 전이 |
| 검증 | tester1 | 이슈 상세 | verifier가 FIXED 이슈를 RESOLVED 또는 ASSIGNED로 처리 |
| close | pl1 | 이슈 상세 | PL이 RESOLVED를 CLOSED로 전이 |

### 5. Deleted issue, recommendation, statistics

| 기능 | Actor | 화면 | 말할 포인트 |
|---|---|---|---|
| 삭제 이슈 관리 | PL | 삭제 이슈 관리 | UC9는 PL 전용 화면과 권한으로 분리됨 |
| 추천 | PL | 배정 dialog | 추천은 자동 배정이 아니라 의사결정 보조임 |
| 통계 | project member | 통계 | DELETED 이슈를 제외한 프로젝트 기준 통계를 조회함 |

### 6. Same flow in two UI toolkits

JavaFX와 Swing은 역할별로 나뉜 UI가 아니라 같은 업무 기능을 각각 제공하는 별도 UI surface다. 발표에서는 같은 seed 계정과 같은 프로젝트를 사용해 다음을 비교한다.

- 로그인 후 프로젝트 목록 진입
- 이슈 목록 조회와 검색
- 이슈 상세 조회
- 상태 변경 action 노출
- 삭제 이슈 또는 통계 화면 진입

핵심 설명은 "화면 구현은 다르지만 controller, service, domain, repository 계약은 공유한다"이다.

## Fallback Demo Path

| 장애 | 대체 경로 | 설명 문장 |
|---|---|---|
| Docker 또는 Oracle local start 실패 | CI Oracle test 결과와 fixed seed 문서 제시 | 로컬 환경 문제와 구현 검증을 분리해서 설명 |
| 한 UI가 실행되지 않음 | 다른 UI에서 같은 흐름을 실행하고, 실패 UI는 QA 캡처로 보완 | 두 UI 모두 같은 backend 계약을 사용한다는 점을 유지 |
| seed data가 예상과 다름 | `oracleLocalResetFixedSeed` 후 재시작 | demo state를 고정 seed로 되돌림 |
| 추천 결과가 약함 | seed 기반 추천 설명과 repository test evidence 제시 | 추천은 후보 ranking 보조 기능임을 명확히 함 |
| GitHub Project metadata 지연 | issue/PR/milestone REST evidence와 스크린샷으로 대체 | metadata 자동화 지연은 기능 구현 검증과 분리 |

## Slide Draft

| Slide | 제목 | 핵심 메시지 | 증거 |
|---|---|---|---|
| 1 | Issue Tracker | 과제 요구사항을 UC 중심으로 구현 | UC 목록 |
| 2 | Requirements | 이슈 관리, 권한, 상태 전이, Oracle, 다중 UI | 요구사항 표 |
| 3 | Architecture | UI를 제외한 계층 재사용 | MVC/GRASP/SOLID 다이어그램 |
| 4 | Data and Persistence | Oracle schema와 fixed seed | schema, seed, connection check |
| 5 | Primary Flow | tester, PL, dev, tester, PL 흐름 | demo path |
| 6 | Advanced Flow | 추천, 의존성, 삭제 이슈, 통계 | 실행 화면 |
| 7 | JavaFX and Swing | 두 UI가 같은 application core를 사용 | side-by-side 캡처 |
| 8 | Verification | JUnit, CI, Oracle integration, QA | check 결과 |
| 9 | Collaboration | issue, PR, project, milestone 흐름 | GitHub evidence |
| 10 | Wrap-up | 제출 범위와 남은 리스크 없음 | final checklist |

## Evidence Slots

| 증거 | 담당 | 채우는 시점 |
|---|---|---|
| JavaFX primary flow 캡처 | JavaFX 담당 | UI PR merge 후 |
| Swing primary flow 캡처 | Swing 담당 | Swing QA 완료 후 |
| Oracle connection/check 로그 | persistence 담당 | 제출 패키징 전 |
| `./gradlew check` 결과 | test 담당 | 최종 freeze 전 |
| GitHub Project/PR evidence | project 담당 | 모든 필수 PR merge 후 |
| 최종 zip dry-run 결과 | package 담당 | 제출 직전 |

## Expected Questions and Defense Points

| 질문 | 답변 방향 |
|---|---|
| 왜 JavaFX와 Swing을 둘 다 만들었는가? | 과제의 다중 UI toolkit 요구를 충족하고, UI를 제외한 계층 재사용을 보여 주기 위해서임 |
| 두 UI가 역할별로 나뉜 것 아닌가? | 둘 다 로그인, 프로젝트, 이슈 흐름을 제공하며 같은 controller/service/domain/repository를 사용함 |
| 상태 전이 규칙은 어디에 있는가? | UI가 아니라 service/domain 정책에서 검사하고, UI는 available action 결과를 표시함 |
| 권한 실패는 어떻게 보장하는가? | controller/service 경계에서 actor와 project membership을 검사하고, 실패 path를 테스트와 데모로 보여 줌 |
| Oracle persistence는 어디까지 확인했는가? | schema/seed, connection check, repository integration, local Oracle runbook으로 확인함 |
| 추천은 어떤 근거로 동작하는가? | 과거 완료 이력, 상태, 이슈 내용 유사도를 사용하고, 최종 선택은 PL이 수행함 |
| DELETED 이슈는 통계에 포함되는가? | 삭제 이슈는 별도 UC9 흐름으로 다루며 통계에서는 제외함 |
| 왜 Project evidence가 필요한가? | 개인별 issue, branch, PR, review, merge 흐름을 과제 협업 증거로 보여 주기 위해서임 |

## Freeze Checklist

- [ ] 리허설/촬영 전 `oracleLocalResetFixedSeed`로 데모 DB를 고정 seed 상태로 되돌린다.
- [ ] JavaFX와 Swing에서 같은 primary demo path를 실행한다.
- [ ] 발표 슬라이드의 스크린샷 placeholder를 실제 캡처로 교체한다.
- [ ] `./gradlew check --console=plain` 결과를 최종 기록한다.
- [ ] `verifySubmissionMetadata` 결과를 확인한다.
- [ ] 최종 package dry-run을 실행한다.
- [ ] GitHub issue, PR, milestone, project evidence를 최신 상태로 맞춘다.
- [ ] 발표자가 각자 맡은 slide와 demo step을 리허설한다.
