# 요구사항 추적표 (Requirements Traceability Matrix)

> 이 문서는 프로젝트 전반에 걸쳐 사용되는 **누락 방지를 위한 요구사항 목록/점검 문서**입니다. 
> 최종 보고서에 초기 상태 그대로 들어갈 문서/문장이 아니라,
> 팀이 PR을 진행하면서 점진적으로 꾸준히 갱신해 나가는 문서입니다.

## 0. 사용 방법
- 새 기능을 구현하기 전에 해당 행의 `Issue` / `Branch` 컬럼을 채웁니다.
- PR 머지 시 `PR` / `Test` / `Docs` 컬럼을 갱신합니다.
- 아직 결정 못 한 항목은 `TBD`** 로 두고 `docs/assumptions.md`에 후보안을 남깁니다.
- 상태(`Status`) 표기:
  - `[X] Not started` — 아직 시작 전
  - `[-] In progress` — Issue 또는 Branch 존재
  - `[O] Done` — PR 머지 + 테스트 + 문서 반영 완료

---

## 1. 기능 요구사항 (FR)

| ID | 요구사항 | 우선순위 | Issue | Branch | PR | Test | Docs | Status | 비고 |
|----|----------|----------|-------|--------|----|----|------|--------|------|
| F-01 | 계정 추가 (admin / PL / dev / tester) | 필수 | TBD | TBD | TBD | TBD | TBD | [X] | 데모 계정 : PL1, PL2, dev1-10, tester1-5 계정 생성 |
| F-02 | 이슈 등록 (Title, Description 필수 / reporter·reported date 자동) | 필수 | TBD | TBD | TBD | TBD | TBD | [X] | |
| F-03 | 이슈 브라우즈 (전체 목록) | 필수 | TBD | TBD | TBD | TBD | TBD | [X] | |
| F-04 | 이슈 검색 (assignee/상태/reporter 등) | 필수 | TBD | TBD | TBD | TBD | TBD | [X] | 검색 필드 조합 범위는 팀 논의 필요 |
| F-05 | 이슈 상세 정보 확인 (필드 + 코멘트 history) | 필수 | TBD | TBD | TBD | TBD | TBD | [X] | |
| F-06 | 코멘트 추가 (작성자/날짜 누적) | 필수 | TBD | TBD | TBD | TBD | TBD | [X] | |
| F-07 | 이슈 상태 변경 (new → assigned → fixed → resolved → closed / reopened) | 필수 | TBD | TBD | TBD | TBD | TBD | [X] | 권한별 허용 전이 규칙은 `assumptions.md`에 정리 |
| F-08 | 이슈 배정 (assignee 지정, 코멘트 + 상태 전이) | 필수 | TBD | TBD | TBD | TBD | TBD | [X] | PL이 dev에게 배정 |
| F-09 | Fixer 자동 기록 (fixed로 전이한 dev) | 필수 | TBD | TBD | TBD | TBD | TBD | [X] | F-07과 묶어서 구현 가능 |
| F-10 | 통계 분석 — 일/월별 이슈 발생 횟수 및 트렌드 | 필수 | TBD | TBD | TBD | TBD | TBD | [X] | 표 또는 차트 형태 결정 필요 |
| F-11 | 해결 이슈 이력 기반 assignee 자동 추천 | 필수 | TBD | TBD | TBD | TBD | TBD | [X] | 추천 알고리즘 선택 (`assumptions.md` 참고) |
| F-12 | 추천 학습 트리거 ("learn now" 등 수동 학습 버튼) | 조건부 필수 | TBD | TBD | TBD | TBD | TBD | [X] | 주기 학습 방식을 택할 경우에만 해당 |
| F-13 | (Optional) Top-3 후보 추천 표시 | 가산점 | TBD | TBD | TBD | TBD | TBD | [X] | 기능 구현 optional 사항 |

### 1-1. 이슈 필드 (Issue Field)
| ID | 필드 | 입력 방식 | 비고 |
|----|------|-----------|------|
| FD-01 | Issue Title | 사용자 입력 (필수) | |
| FD-02 | Issue Description | 사용자 입력 (필수) | |
| FD-03 | Reporter | 시스템 자동 (로그인 계정) | |
| FD-04 | Reported Date | 시스템 자동 (등록 시각) | |
| FD-05 | Fixer | 시스템 자동 (fixed 상태로 바꾼 dev) | |
| FD-06 | Assignee | PL이 지정 | |
| FD-07 | Priority | blocker / critical / major(default) / minor / trivial | |
| FD-08 | Status | new / assigned / resolved / closed / reopened (+ fixed) | Issue들의 상태 여부 — `assumptions.md`에서 명시 |
| FD-09 | Comments | 누적 추가, 날짜 포함 | |
| FD-10 | (추가) … | 팀이 추가하는 필드는 여기에 기록 후 보고서에서 명시 | |

---

## 2. 아키텍처 / 비기능 요구사항 (Architecture & NR)

| ID | 요구사항 | 검증 방법 | Status | 비고 |
|----|----------|-----------|--------|------|
| A-01 | MVC 아키텍처 적용 (UI <-> 모델 분리) | 클래스 다이어그램 + 패키지 구조 검토 | [X] | model 패키지에 UI 클래스 import 금지 |
| A-02 | UI Toolkit 2개 이상으로 UI 2개 구현 | 두 진입점 모두 빌드/실행 데모 | [X] | 후보: JavaFX + Swing (`assumptions.md`) |
| A-03 | UI 외 코드 재사용 데모 | 동일 model/controller로 두 UI 빌드 시연 | [X] | 발표 시연 필수 |
| A-04 | 영속 저장소 (DBMS 혹은 파일) | 재시작 후 데이터 유지 테스트 | [X] | 파일 vs SQLite 결정 필요 |
| A-05 | 테스트 용이한 설계 | JUnit 모델 테스트 통과 | [X] | 의존성 주입 / 인터페이스 분리 |
| A-06 | OOAD/GRASP 적용 근거 문서화 | 설계 문서 | [X] | 어떤 패턴/원칙을 어디에 썼는지 명시 |

---

## 3. 문서 산출물 (Artifact)

> 프로젝트 문서 PDF 안에 들어가야 하는 항목들입니다. 각 항목별로 작업 문서가 따로 있으면 위치를 적습니다.

| ID | 산출물 | 담당 문서 / 위치 | Status | 비고 |
|----|--------|------------------|--------|------|
| D-A | 표지 (학번/이름) | 보고서 표지 | [X] | 제출 직전 확정 |
| D-B | 프로젝트 내용 요약 (누락 기능 명시 포함) | 보고서 초록부분 | [X] | |
| D-C1 | 유스케이스 다이어그램 (전체, includes 2개 이상, extends 2개 이상) | `docs/uml/usecase-diagram.*` | [X] | includes/extends 후보 식별 필요 |
| D-C2 | 유스케이스 명세 6개 (include base/included, extend base/extending 포함) | `docs/usecases/*.md` | [X] | 6개 UC 결정 및 설명 |
| D-C3 | 도메인 모델 | `docs/uml/domain-model.*` | [X] | |
| D-C4 | SSD | `docs/uml/ssd-*.png` | [X] | SSD 2개 이상 |
| D-C5 | Operation Contract | `docs/contracts/*.md` | [X] | SSD와 매칭, OC 2개 이상 |
| D-D1 | 클래스 다이어그램 | `docs/uml/class-diagram.*` | [X] | |
| D-D2 | 시퀀스 다이어그램 | `docs/uml/sequence-*.*` | [X] | |
| D-D3 | OOAD/GRASP 적용 설명 | 보고서 §D | [X] | A-06과 연결 |
| D-E | 구현 결과 (스크린샷 + extra 기능 설명) | `docs/screenshots/` | [X] | UI 두 종 모두 캡처 |
| D-F | 테스트 수행 내역 (JUnit 코드 + 목적) | | [X] | T-* 항목과 연결 |
| D-G | GitHub 활용 요약 (history 캡처, 팀원별 기여) | `docs/screenshots/github-*` | [X] | Insights 캡처 시점 결정 필요 |

---

## 4. 테스트 (Test)

| ID | 대상 | 테스트 종류 | 위치 | Status | 비고 |
|----|------|-------------|------|--------|------|
| T-01 | Issue 도메인 모델 (상태 전이 규칙) | JUnit 단위 | `src/test/java/.../model/IssueTest.java` | [X] | F-07과 매칭 |
| T-02 | IssueRepository (저장/로드 round-trip) | JUnit 단위 | `src/test/java/.../persistence/` | [X] | A-04 검증 |
| T-03 | 검색 필터 로직 | JUnit 단위 | `src/test/java/.../service/SearchTest.java` | [X] | F-04 |
| T-04 | 통계 집계 (일/월 기준으로 카운트) | JUnit 단위 | `src/test/java/.../service/StatsTest.java` | [X] | F-10 |
| T-05 | Assignee 추천 결과 결정성 | JUnit 단위 | `src/test/java/.../service/RecommenderTest.java` | [X] | F-11 |
| T-06 | 권한별 상태 전이 허용/거부 | JUnit 단위 | TBD | [X] | F-07 + 가정 문서 |

---

## 5. GitHub 협업 기록 (GitHub Collaboration Record)

| ID | 항목 | 어디서 확인 | Status | 비고 |
|----|------|-------------|--------|------|
| G-01 | Issue -> Branch -> PR -> Review -> Merge 흐름 | GitHub Insights | [-] | 운영 원칙, README 10 관련 |
| G-02 | 팀원별 기여 통계 | Insights -> Contributors | [X] | 발표 직전 캡처 |
| G-03 | Project board 활용 기록 | GitHub Project | [-] | URL은 README에 명시되어있음 |
| G-04 | CI 통과 기록 | Actions 탭 | [-] | gradle workflow 동작 |
| G-05 | PR 리뷰 기록 (팀원 간 review) | PR 탭 | [X] | 모든 PR에 리뷰어 1명 이상 권장 |

---

## 6. 제출물 (Work Product For Submission)

| ID | 제출물 | 생성 방법 | Status | 비고 |
|----|--------|-----------|--------|------|
| S-01 | README.txt | `scripts/package-submission.sh` 자동 생성 | [-] | 스크립트는 존재, 내용은 제출 직전 확정 |
| S-02 | 발표 슬라이드 | 수동 작성 | [X] | 발표 일정: 6/3 이후로 추정 |
| S-03 | 프로젝트 문서 PDF | 보고서 빌드 | [X] | 3. 문서 산출물의 항목들 합본, 60페이지 이하로 |
| S-04 | 소스코드 + 실행파일 + JUnit + 데이터 | 빌드 산출물 + `src/` | [X] | |
| S-05 | 프로젝트 소개 동영상 | 화면 녹화 | [X] | 설계 고민 + 데모 + 테스트 목적 설명 포함, 30분 이내로 |
| S-06 | zip 파일명 형식 준수 | `package-submission.sh --team-number ... --member ...` | [-] | |

---

## 7. README / 기존 문서 정합성 점검

> README와 docs가 PDF와 어긋나는 부분이 있는지 확인하는 체크리스트.

| 점검 항목 | 결과 | 조치 필요 |
|-----------|------|-----------|
| README의 "과제 핵심 요구사항"이 PDF와 일치하는가 | [O] | - |
| README의 "최종 제출 마감"이 PDF (6/2 21:00)와 일치 | [O] | - |
| README의 제출물 목록이 PDF 가이드 라인과 일치 | [-] | - |
| `assumptions.md` 링크가 존재하는가 | [X] | 본 이슈에서 같이 생성 |
| `qna.md` 링크가 존재하는가 | [X] | 별도 이슈로 분리 권장 |
| 팀원 이름/학번 placeholder가 남아있는가 | [-] | 제출 직전에 확인 `verifySubmissionMetadata` 통과시킬 것 |

---

## 변경 이력 
-26/04/29 _초안 생성_ 