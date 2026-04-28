# 프로젝트 관리 계획

이 문서는 GitHub Project와 Issue를 과제 진행판으로 쓰기 위한 운영 기준입니다. 목표는 보기 좋은 보드를 만드는 것이 아니라, 과제 PDF 요구사항이 구현/테스트/문서/발표까지 빠지지 않고 연결되게 하는 것입니다.

## 운영 원칙

1. 모든 구현과 문서 작업은 가능한 한 Issue에서 시작합니다.
2. Issue는 과제 PDF의 요구사항이나 최종 제출 산출물과 연결되어야 합니다.
3. PR은 관련 Issue를 본문에 연결하고, 구현 결과와 검증 내역을 짧게 남깁니다.
4. Project 상태는 실제 진행 상황만 반영합니다. 보기 좋게 보이기 위해 Done으로 옮기지 않습니다.
5. 최종 보고서에 들어갈 문장은 팀원이 직접 읽고 이해한 뒤 실제 구현 내용에 맞게 다듬습니다.

## Project 상태 사용법

| Status | 의미 | 이동 기준 |
| --- | --- | --- |
| Backlog | 해야 할 일로 등록됨 | 아직 담당자/구체 작업 순서가 확정되지 않음 |
| Ready | 바로 시작 가능 | 요구사항과 완료 기준이 충분히 명확함 |
| In Progress | 작업 중 | branch 또는 draft PR이 생겼거나 담당자가 작업 중 |
| In Review | 검토 중 | PR이 열렸거나 문서/설계 리뷰 중 |
| Done | 완료 | PR merge, 문서 반영, 테스트/검증 증거가 확인됨 |

## Milestone 사용법

- **M1 - Requirements & Design Baseline**: 요구사항 추적, 유스케이스, 도메인 모델, SSD, Operation Contract
- **M2 - Core Domain & Persistence**: 모델, 서비스, 검색, 상태 전이, 저장소, 통계, 추천
- **M3 - Dual UI & Demo Flow**: JavaFX/Swing UI와 PDF 데모 시나리오
- **M4 - Verification & Submission Pack**: 테스트, 최종 문서, 발표, 영상, 제출 zip

기존의 `Setup`, `Core`, `UI`, `Test`, `Docs`, `Demo` milestone은 과거 분류용으로 남아 있으나, 앞으로 새 작업은 M1~M4 중심으로 배치합니다. 필요하면 나중에 빈 milestone을 닫아 정리합니다.

## Issue 작성 기준

좋은 Issue는 다음 질문에 답해야 합니다.

- 왜 필요한가?
- PDF의 어떤 요구사항과 연결되는가?
- 이번 Issue에서 어디까지 할 것인가?
- 완료되었다는 것을 무엇으로 확인할 것인가?
- 문서나 테스트에 어떤 흔적을 남길 것인가?

Issue 본문은 길어도 괜찮지만, 최종 보고서처럼 완성된 문장일 필요는 없습니다. 팀원이 작업을 시작할 때 헷갈리지 않는 정도가 가장 중요합니다.

## 현재 seed backlog

| Issue | Milestone | 성격 |
| --- | --- | --- |
| [#13](https://github.com/marcellokim/se-issue-tracker/issues/13) 과제 PDF 요구사항 추적표와 팀 가정 정리 | M1 | 문서/관리 |
| [#14](https://github.com/marcellokim/se-issue-tracker/issues/14) 유스케이스 다이어그램과 핵심 유스케이스 6개 명세 작성 | M1 | 분석 |
| [#15](https://github.com/marcellokim/se-issue-tracker/issues/15) 도메인 모델, SSD, Operation Contract 초안 작성 | M1 | 분석/설계 |
| [#16](https://github.com/marcellokim/se-issue-tracker/issues/16) 계정, 역할, 프로젝트 기본 모델 구현 | M2 | 구현 |
| [#17](https://github.com/marcellokim/se-issue-tracker/issues/17) 이슈, 댓글, 우선순위, 상태 전이 모델 구현 | M2 | 구현 |
| [#18](https://github.com/marcellokim/se-issue-tracker/issues/18) 파일 기반 영속 저장소와 데모 seed 데이터 준비 | M2 | 구현 |
| [#19](https://github.com/marcellokim/se-issue-tracker/issues/19) 이슈 등록, 검색, 상세 조회, 코멘트 서비스 구현 | M2 | 구현 |
| [#20](https://github.com/marcellokim/se-issue-tracker/issues/20) 이슈 배정과 상태 변경 흐름 구현 | M2 | 구현 |
| [#21](https://github.com/marcellokim/se-issue-tracker/issues/21) 일/월별 이슈 통계와 trend 조회 구현 | M2 | 구현 |
| [#22](https://github.com/marcellokim/se-issue-tracker/issues/22) 해결 이력 기반 assignee 추천 기능 구현 | M2 | 구현 |
| [#23](https://github.com/marcellokim/se-issue-tracker/issues/23) JavaFX 메인 UI로 기본 사용자 흐름 구현 | M3 | UI |
| [#24](https://github.com/marcellokim/se-issue-tracker/issues/24) Swing 보조 UI로 모델 재사용 구조 입증 | M3 | UI |
| [#25](https://github.com/marcellokim/se-issue-tracker/issues/25) 모델, 서비스, 영속 저장소 JUnit 테스트 구성 | M4 | 테스트 |
| [#26](https://github.com/marcellokim/se-issue-tracker/issues/26) 최종 프로젝트 문서, 발표 자료, 영상, 제출 패키지 준비 | M4 | 제출 |
| [#27](https://github.com/marcellokim/se-issue-tracker/issues/27) GitHub Project 자동 등록과 progress 캡처 준비 | M1 | 운영 |

## 진행 순서 제안

1. #13, #14, #15를 먼저 처리해서 요구사항과 설계 기준을 안정화합니다.
2. #16~#20으로 기본 도메인 흐름을 구현합니다.
3. #18의 seed 데이터로 PDF 데모 계정을 준비합니다.
4. #21, #22를 붙여 통계/추천 요구사항을 완성합니다.
5. #23, #24에서 UI 두 개를 같은 서비스 계층에 연결합니다.
6. #25로 핵심 모델/서비스/저장소 테스트를 보강합니다.
7. #26에서 문서/발표/영상/제출 패키지를 닫습니다.

## 자동화 메모

현재 Project URL 변수는 설정되어 있지만, user-level GitHub Project에 자동 등록하려면 `ADD_TO_PROJECT_PAT` secret이 필요합니다. 이 값은 보안 정보이므로 문서나 커밋에 적지 않습니다. 설정이 끝나면 새 Issue/PR 생성 시 Project 자동 등록 workflow가 실제로 동작하는지 #27에서 확인합니다.
