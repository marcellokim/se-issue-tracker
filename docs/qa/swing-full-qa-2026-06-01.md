# Swing UI QA Report - 2026-06-01

## 개요

- 대상: Swing UI 전체 화면
- 관련 이슈: #246, #248
- 기준 코드: `origin/dev` 기반 `test/248-swing-full-qa`
- DB: Oracle Free `FREEPDB1`, fixed seed
- 산출물 정책: 로컬 캡처 이미지는 재현용 증거로만 사용하고 PR에는 포함하지 않음

이번 QA는 Swing UI가 백엔드/application 코드를 재사용하면서 주요 역할별 흐름을 실행할 수 있는지 확인하는 데 초점을 둔다. 자동화가 가능한 범위에서는 Oracle DB에 연결한 동일 JVM Swing 하네스로 실제 컨트롤러/서비스 경로를 탔다. 다만 WSLg 환경의 네이티브 마우스 포커스는 `java.awt.Robot`으로 재현되지 않아 최종 발표 장비에서 별도 재검증이 필요하다.

## 결론

| 영역 | 결과 | 판단 |
| --- | --- | --- |
| 앱 기동/DB 연결 | 통과 | Oracle env 누락 오류와 정상 DB 연결 경로 확인 |
| 역할별 로그인 | 부분 통과 | Admin/PL/Dev/Tester 인증 흐름 확인, 네이티브 마우스 포커스는 발표 장비 재검증 필요 |
| Admin 화면 | 통과 | 계정/프로젝트 조회 및 주요 mutation 흐름 확인 |
| PL 화면 | 통과 | 이슈 목록, 상세, 등록, 수정, 우선순위, 배정, 댓글, 의존성, 삭제 이슈, 통계 확인 |
| Dev 화면 | 통과 | 배정 이슈 조회, fixed 전환, 본인 댓글 수정/삭제 확인 |
| Tester 화면 | 통과 | fixed 이슈 reject/resolve 흐름 확인 |
| Cross-cutting | 통과 | 뒤로가기, 로그아웃, 중복 클릭 방지, dialog 취소/닫기 확인 |
| Visual QA | 부분 통과 | 주요 화면 렌더링과 800x600 최소 크기 확인, 최종 시연 환경 육안 점검 필요 |

## 수정 반영

| 항목 | 조치 |
| --- | --- |
| 로그인 초기 입력 불편 | 창 open/activate 시 로그인 ID 입력 필드로 포커스 요청을 보강 |
| 이슈 상세 헤더 가독성 | 사람이 읽는 제목을 우선 배치하고 issue id는 보조 메타데이터로 이동 |
| 버튼/테이블 기본 스타일 | 주요 버튼과 테이블 헤더의 색상, 테두리, 높이 기준을 통일 |
| 테이블 헤더 null 가능성 | 공통 스타일 helper에서 header 존재 여부를 확인 |
| 화면별 테이블 조작 | 컬럼 드래그 재정렬을 비활성화해 시연 중 표 구조가 흔들리지 않게 정리 |

## 확인 계정

| Role | Login ID | Password |
| --- | --- | --- |
| Admin | `admin` | `DemoLocalAdmin!` |
| PL | `pl1` | `DemoLocalPl1!` |
| Dev | `dev1` | `DemoLocalDev1!` |
| Tester | `tester1` | `DemoLocalTester1!` |

## 주요 시나리오

| ID | 화면 | 확인 내용 | 결과 |
| --- | --- | --- | --- |
| SW-QA-ENV-001 | Startup | DB env 누락 시 오류 안내, 정상 env 시 앱 기동 | 통과 |
| SW-QA-AUTH-001 | Login | 유효/무효 로그인, 비밀번호 초기화, 역할별 진입 | 부분 통과 |
| SW-QA-ADMIN-001 | Admin | 계정 생성/중복/수정/비활성화/활성화, 프로젝트 생성/수정/멤버 변경 | 통과 |
| SW-QA-PL-001 | PL Issue | 이슈 등록/조회/수정/우선순위/배정/댓글/의존성 | 통과 |
| SW-QA-PL-002 | Deleted Issue | 삭제 이슈 조회, restore, purge | 통과 |
| SW-QA-PL-003 | Statistics | 전체/필터/잘못된 기간 입력 | 통과 |
| SW-QA-DEV-001 | Dev Issue | mark fixed, 댓글 추가/수정/삭제 | 통과 |
| SW-QA-TESTER-001 | Tester Issue | reject, resolve | 통과 |
| SW-QA-CROSS-001 | Navigation | 뒤로가기, 로그아웃 후 역할 전환 | 통과 |
| SW-QA-CROSS-002 | Dialog | 등록/수정/배정/댓글/상태 변경 dialog 취소와 창 닫기 | 통과 |
| SW-QA-CROSS-003 | Duplicate submit | 주요 action button 연속 클릭 시 중복 실행 방지 | 통과 |
| SW-QA-VISUAL-001 | Layout | 1024x768 기본 크기와 800x600 최소 크기에서 주요 화면 렌더링 | 부분 통과 |

## 자동화 한계

WSL2/WSLg에서는 X11 캡처 도구가 없고 `java.awt.Robot` 좌표 클릭도 최소 Swing `JTextField`에서 포커스를 획득하지 못했다. 따라서 이번 문서는 네이티브 마우스 클릭을 완료로 주장하지 않는다.

발표 전 아래 항목은 실제 발표 장비에서 짧게 다시 확인한다.

| ID | 확인 항목 | 기대 결과 |
| --- | --- | --- |
| TDT-FOCUS-001 | Login ID 필드를 마우스로 클릭 후 `admin` 입력 | 첫 클릭 후 caret이 보이고 입력값이 ID 필드에 들어감 |
| TDT-FOCUS-002 | Password 필드를 마우스로 클릭 후 비밀번호 입력 | 비밀번호 필드가 입력을 받음 |
| TDT-FOCUS-003 | `Sign in` 버튼을 마우스로 클릭 | Admin dashboard로 이동 |
| TDT-VISUAL-001 | Login, Admin, PL issue list/detail, Statistics 화면 육안 확인 | 텍스트 겹침, 주요 버튼 잘림, 읽을 수 없는 disabled 상태 없음 |
| TDT-VISUAL-002 | 800x600에서 긴 제목/상세 화면 확인 | 제목 시작부가 보이고 필요한 영역은 스크롤 가능 |

## 실행 명령

```text
git fetch origin dev --prune
./gradlew check verifySubmissionMetadata --console=plain
./gradlew oracleLocalResetFixedSeed --console=plain
./gradlew oracleConnectionCheck --console=plain
./gradlew test --tests 'com.github.marcellokim.issuetracker.ui.swing.*' --console=plain
git diff --check
```

## 남은 리스크

- 네이티브 마우스/키보드 입력은 WSLg 자동화에서 신뢰 가능한 증거를 얻지 못했으므로 발표 장비에서 수동 확인 필요.
- Swing 화면은 기능 제출 기준으로 정리되었지만, JavaFX와 동일한 수준의 시각적 완성도를 보장하지는 않는다. 발표에서는 전체 UI 2종 구현과 백엔드 재사용 증명에 초점을 둔다.
