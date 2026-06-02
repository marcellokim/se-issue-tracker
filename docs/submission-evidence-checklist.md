# 최종 제출 증빙 체크리스트

이 문서는 최종 제출 직전에 캡처와 패키징 증거를 빠뜨리지 않기 위한 점검표이다. 최종 산출물에는 필요한 화면 캡처와 링크만 선별해서 옮긴다.

## 기준 상태

| 항목 | 기준 |
| --- | --- |
| 기준 브랜치 | `dev` |
| 기준 커밋 | 최종 제출 직전 최신 `dev` commit |
| GitHub Project | <https://github.com/users/marcellokim/projects/1> |
| 남은 제출 gate | GitHub M4 open gate 기준으로 제출 직전 재확인 |

## 기능/정책 증빙

| 구분 | 확인할 내용 | 증거 |
| --- | --- | --- |
| UC3 이슈 검색 | 프로젝트 멤버가 역할 구분 없이 프로젝트 일반 이슈를 조회하고 reporter/assignee/verifier/status/priority/date 검색 필터를 사용할 수 있음 | PR #266, `IssueController.searchIssues`, `IssueListPresenterTest` |
| UC9 삭제 이슈 | DELETED 이슈는 일반 목록/검색에서 제외되고 PL 전용 삭제 이슈 화면에서만 조회됨 | `DeletedIssueController`, `DeletedIssueServiceTest`, Swing deleted issue QA |
| 두 UI Toolkit | JavaFX 전체 UI와 Swing 전체 UI가 같은 controller/service 계층을 사용함 | `./gradlew run`, `./gradlew runSwing`, UI navigation map, Swing QA report |
| 테스트 요구사항 | 모델, 서비스, 영속 저장소, controller, UI 단위 테스트가 CI에서 실행됨 | #25, `docs/test-gates/issue-25-model-service-persistence-gate-2026-06-02.md` |

## 최종 보고서 섹션별 증빙 인덱스

| 보고서 섹션 | 최종 원천 문서 | 필요한 캡처/로그 | 현재 상태 | 담당 gate |
| --- | --- | --- | --- | --- |
| 1. 프로젝트 개요 | `README.md`, `docs/requirements-traceability.md` | 저장소/Project 링크, 실행 환경 | 문서 준비됨 | #26 |
| 2. 요구사항 분석 | `docs/use-cases/use_case_specifications.md`, `docs/requirements-traceability.md` | UCD, UC coverage 표 | 문서 준비됨 | #26 |
| 3-6. 분석/설계 | `docs/uml/`, `docs/ooad-grasp-mvc.md` | domain model, SSD, OC, architecture, DCD/SD | 문서 준비됨 | #26 |
| 7. 구현 결과 | `docs/demo-scenario.md`, JavaFX/Swing 실행 화면 | 같은 기능의 JavaFX/Swing 캡처 | 최종 캡처 필요 | #26, #246 |
| 8. 테스트와 검증 | `docs/test-gates/`, CI 결과 | `check`, Oracle, SonarCloud, CodeQL 결과 | PR/CI 근거 준비됨 | #25 |
| 9. 협업 및 프로젝트 관리 | GitHub Project, issue, PR, milestone | Project board, M4 gate, 대표 PR | 최종 캡처 필요 | #27 |
| 10. 결론과 한계 | `docs/demo-scenario.md`, 최종 QA 결과 | 완료 범위와 남은 제출 범위 밖 항목 | 최종 문장 정리 필요 | #26 |

## GitHub 진행 이력 캡처

제출 직전 아래 화면을 캡처한다.

| 캡처 | 상태 |
| --- | --- |
| GitHub Project 보드 전체 화면 | [ ] |
| M4 milestone과 남은 gate 이슈 목록 | [ ] |
| 대표 PR 병합 이력: Swing UI, JavaFX UI, PR #266(264번 정책 수정), SonarCloud 보정 | [ ] |
| `dev` branch 최신 commit과 branch protection 필수 체크 상태 | [ ] |
| GitHub Actions: 빌드와 테스트, 워크플로우 정책 검사, Oracle 통합 테스트, SonarCloud 분석, 보안 코드 분석 | [ ] |

별도 `SonarCloud Code Analysis` App check는 branch protection 필수 체크와 구분해서 기록한다. 제출 증빙에서는 CI의 `SonarCloud 분석` workflow 통과와 branch protection 필수 체크 통과를 기준으로 삼고, 최종 제출 직전에 최신 App check 상태를 다시 확인한다.

## Swing 발표 장비 재확인

#246을 닫기 전에 발표 장비 또는 실제 시연 환경에서 아래 항목을 다시 확인한다.

| 항목 | 상태 |
| --- | --- |
| 로그인 ID 필드가 마우스 클릭으로 focus/input 가능 | [ ] |
| 로그인 password 필드가 마우스 클릭으로 focus/input 가능 | [ ] |
| Sign in 버튼이 마우스 클릭으로 동작 | [ ] |
| 주요 화면이 800x600에서 잘림 없이 표시 | [ ] |
| project, issue list, issue detail, status change, deleted issue, statistics 화면 시각 확인 | [ ] |

## 패키징 점검

최종 팀 번호와 팀원명을 넣어 제출 zip을 생성한다.

```bash
./scripts/package-submission.sh \
  --team-number "<팀번호>" \
  --member "<팀원1>" \
  --member "<팀원2>" \
  --member "<팀원3>" \
  --project-url "https://github.com/users/marcellokim/projects/1"
```

최종 보고서 PDF, 발표자료, 시연 영상은 이 소스 패키지에 넣지 않고 별도 제출물로 준비한다.

생성 후 아래 항목을 확인한다.

| 항목 | 상태 |
| --- | --- |
| `dist/` 아래 제출 zip 생성 | [ ] |
| zip 내부 `README.txt` 생성 및 실행 명령 확인 | [ ] |
| `.git`, `.gradle`, `build`, `dist`, `docs/qa/artifacts`, IDE 설정 제외 | [ ] |
| 강의 원문 PDF, 기존 zip, 개인 지침/메모 파일, 리뷰 앱/보조 도구 설정 제외 | [ ] |
| 최종 보고서 PDF, 발표자료, 시연 영상은 별도 제출물로 분리 | [ ] |

## 최종 검증 명령

```bash
./gradlew check --console=plain
./gradlew verifySubmissionMetadata --console=plain
```

Oracle local까지 확인할 경우:

```bash
./gradlew oracleLocalTest --console=plain
```
