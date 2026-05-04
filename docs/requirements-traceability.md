# 요구사항 추적표

이 문서는 루트의 `SE_Term_Project_2026-1.pdf`를 기준으로, 과제 요구사항을 GitHub 이슈와 산출물로 연결하기 위한 작업용 추적표입니다. PDF 원문과 이 문서가 충돌하면 PDF 원문을 우선합니다.

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
| 이슈 배정 | [#20](https://github.com/marcellokim/se-issue-tracker/issues/20) 이슈 배정과 상태 변경 흐름 구현 | assignee 변경 테스트, PL 시나리오 |
| 이슈 상태 변경 | [#17](https://github.com/marcellokim/se-issue-tracker/issues/17), [#20](https://github.com/marcellokim/se-issue-tracker/issues/20), [#43](https://github.com/marcellokim/se-issue-tracker/issues/43), [#47](https://github.com/marcellokim/se-issue-tracker/issues/47) | 상태 전이 규칙, 잘못된 fixed 역전이, reopen 테스트 |
| fixer 기록 | [#20](https://github.com/marcellokim/se-issue-tracker/issues/20) | dev 작업 완료 시 fixer 저장 테스트 |
| 우선순위 blocker/critical/major/minor/trivial | [#17](https://github.com/marcellokim/se-issue-tracker/issues/17) | Priority enum/value object, 기본값 major 테스트 |
| 상태 new/assigned/fixed/resolved/closed/reopened/deleted | [#17](https://github.com/marcellokim/se-issue-tracker/issues/17), [#20](https://github.com/marcellokim/se-issue-tracker/issues/20), [#43](https://github.com/marcellokim/se-issue-tracker/issues/43), [#44](https://github.com/marcellokim/se-issue-tracker/issues/44), [#47](https://github.com/marcellokim/se-issue-tracker/issues/47) | IssueStatus 정의, 상태 전이 문서, soft-delete/FIFO 정리 테스트 |
| reporter의 title/description 수정 제한 | [#46](https://github.com/marcellokim/se-issue-tracker/issues/46) | assigned 전 수정 허용, assigned 이후 comment 보완 흐름 |
| PL 전용 priority 변경 | [#17](https://github.com/marcellokim/se-issue-tracker/issues/17), [#20](https://github.com/marcellokim/se-issue-tracker/issues/20) | 권한 검사, 변경 사유 comment/history |
| 이슈 dependency 관계 | [#45](https://github.com/marcellokim/se-issue-tracker/issues/45) | IssueDependency 모델, 순환 방지, 선행 이슈 해결 제약 테스트 |
| 일/월별 이슈 발생 통계 및 trend | [#21](https://github.com/marcellokim/se-issue-tracker/issues/21) 일/월별 이슈 통계와 trend 조회 구현 | 통계 서비스, 날짜 fixture, UI 표시 |
| closed/resolved 이력 기반 assignee 추천 | [#22](https://github.com/marcellokim/se-issue-tracker/issues/22) 해결 이력 기반 assignee 추천 기능 구현 | 추천 서비스, 후보 3명 결과, 추천 근거 |
| persistent storage | [#18](https://github.com/marcellokim/se-issue-tracker/issues/18) | DB 저장/조회 테스트, schema/seed 설명 |
| MVC 및 UI/로직 분리 | [#15](https://github.com/marcellokim/se-issue-tracker/issues/15), [#23](https://github.com/marcellokim/se-issue-tracker/issues/23), [#24](https://github.com/marcellokim/se-issue-tracker/issues/24) | 패키지 구조, 클래스 다이어그램, 두 UI의 공통 서비스 사용 |
| 두 개 이상의 UI Toolkit | [#23](https://github.com/marcellokim/se-issue-tracker/issues/23), [#24](https://github.com/marcellokim/se-issue-tracker/issues/24) | JavaFX UI, Swing UI, 빌드/실행 캡처 |
| JUnit 테스트 | [#25](https://github.com/marcellokim/se-issue-tracker/issues/25) 모델, 서비스, 영속 저장소 JUnit 테스트 구성 | 테스트 코드, CI 결과, 테스트 목적 설명 |

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
| 최종 README.txt, 발표 슬라이드, 문서 PDF, 영상, zip | [#26](https://github.com/marcellokim/se-issue-tracker/issues/26) | 제출 zip, README.txt, 슬라이드, 영상 |

## 팀 회의 확정 사항

- User당 직군/역할은 하나만 부여합니다.
- Reporter는 assigned 전까지만 자신이 등록한 이슈의 title/description을 수정할 수 있고, assigned 이후 정정은 comment로 남깁니다.
- Priority는 PL만 변경할 수 있으며, assigned 상태와 무관하게 변경 가능합니다.
- Dev가 fixed 처리한 이슈를 Tester가 검증 실패하면 `fixed -> assigned`로 되돌릴 수 있습니다.
- Reopen은 PL만 수행하며, reopen 후에는 assignee를 지정해 assigned 상태부터 재작업을 시작합니다.
- 불필요한 이슈는 `deleted` 상태로 soft-delete하고, deleted 이슈가 30개를 초과하면 deleted 전이 시각 기준 FIFO로 오래된 이슈부터 물리 삭제합니다.
- 이슈 dependency 관계는 구조화된 기능으로 추가합니다.
