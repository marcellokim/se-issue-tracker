# 프로젝트 자동화 / 생산성 플레이북

이 문서는 **실 구현 전에 무엇을 자동화해 두었고, 무엇은 일부러 자동화하지 않았는지**를 정리한 운영 기준 문서입니다.  
목표는 “자동화를 많이 넣는 것”이 아니라, **과제 특성에 맞게 생산성은 높이고 리스크는 줄이는 것**입니다.

---

## 0. 이 문서의 사용 목적
이 문서는 아래 상황에서 참고합니다.

- 새 팀원이 “무엇이 자동으로 되고, 무엇은 사람이 해야 하는지” 알고 싶을 때
- GitHub 설정을 다시 점검해야 할 때
- 과제 특성상 자동화를 어디까지 허용할지 팀 기준을 맞추고 싶을 때
- 제출 직전 누락된 자동화/증빙이 없는지 확인하고 싶을 때

---

## 1. 자동화 설계 원칙

### 우리가 자동화한 것
- 반복 작업
- 누락되기 쉬운 체크리스트
- GitHub 협업 이력 정리
- 제출 패키징
- 보안/설정 baseline

### 일부러 자동화하지 않은 것
- UML 설계 근거 생성
- 유스케이스 서술 자동 생성
- 발표/Q&A 답변 자동 생성
- 테스트 목적/의도 설명 자동 생성

이유:
- 이 과제는 **설계 이해도, 문서 충실도, 발표/Q&A**도 평가 대상이기 때문
- 과도한 자동화는 오히려 과제 리스크가 될 수 있기 때문

---

## 2. 현재 구현된 자동화 전체 목록

### 2-1. 로컬 개발 자동화
| 항목 | 상태 | 목적 | 사용 방법 |
| --- | --- | --- | --- |
| `scripts/bootstrap.sh` | 구현 | 빠른 초기 세팅 진입점 | `./scripts/bootstrap.sh` |
| `scripts/bootstrap-dev.sh` | 구현 | 새 팀원 로컬 초기 세팅 | `./scripts/bootstrap-dev.sh` |
| `.githooks/pre-commit` | 구현 | 위험한 브랜치 작업/설정 누락 방지 | bootstrap 시 자동 설치 |
| `.githooks/pre-push` | 구현 | push 전 테스트/기본 검증 | bootstrap 시 자동 설치 |
| `.gitmessage.txt` | 구현 | Lore commit protocol 강제 유도 | bootstrap 시 자동 적용 |
| `scripts/start-task.sh` | 구현 | 기준선 확인 후 브랜치 이름 표준화 | `./scripts/start-task.sh 18 recommendation-engine` |
| `scripts/open-pr.sh` | 구현 | 검증, push, PR 생성, 이슈 review 라벨 이동, Project 정렬을 한 번에 처리 | `./scripts/open-pr.sh` |
| `scripts/audit-project.sh` | 구현 | main/dev, 문서, DB 표준, GitHub 이슈/Project 정합성 점검 | `./scripts/audit-project.sh` |
| `scripts/sync-project-board.sh` | 구현 | 이슈/PR 상태 라벨을 GitHub Project 상태로 반영 | `./scripts/sync-project-board.sh --apply` |
| `scripts/package-submission.sh` | 구현 | 제출용 zip + `README.txt` 자동 생성 | `./scripts/package-submission.sh --team-number ...` |

`Project 정합성 유지` 워크플로우는 이슈/PR 이벤트가 짧은 시간에 몰릴 때 중복 실행을 취소하고,
GitHub GraphQL 잔여량이 낮으면 Project 정렬을 성공 상태로 건너뛴다. 이 경우 저장소나 Project
데이터가 망가진 것이 아니라 외부 API 한도 보호가 동작한 것이므로, 다음 예약 실행 또는 수동
`workflow_dispatch`에서 다시 정렬한다.

### 2-2. 저장소/구성 자동화
| 항목 | 상태 | 목적 |
| --- | --- | --- |
| `.editorconfig` | 구현 | 줄바꿈/기본 포맷 통일 |
| `.gitattributes` | 구현 | 텍스트/바이너리 처리 일관성 |
| `.java-version` | 구현 | Java 버전 기준 명시 |
| `gradle.properties` | 구현 | Gradle 실행 baseline 설정 |
| `build.gradle` custom tasks | 구현 | 저장소 준비 상태/제출 metadata 검증 |

### 2-3. GitHub 협업 자동화
| 항목 | 상태 | 목적 |
| --- | --- | --- |
| 이슈 양식 | 구현 | 이슈 입력 형식 통일 |
| chore 템플릿 | 구현 | 자동화/환경설정 작업 분리 |
| PR Template | 구현 | 검증/문서/과제 영향 체크 |
| PR Labeler | 구현 | 변경 영역 자동 분류 |
| Project 자동 추가 워크플로우 | 부분 구현 | 이슈/PR를 Project에 자동 추가 |
| Project 정합성 유지 워크플로우 | 구현 | 이슈/PR 이벤트와 매일 00:17 KST에 Project 상태 정렬/점검 |
| CODEOWNERS | 기본 구현 | 리뷰 책임자 구조 준비 |
| bootstrap GitHub 스크립트 | 구현 | label/마일스톤/project/variable/repo setting 정렬 |

### 2-4. 보안 자동화
| 항목 | 상태 | 비고 |
| --- | --- | --- |
| Dependabot 보안 업데이트 | 활성화 | 의존성 취약점 알림/PR |
| Secret scanning | 활성화 | 민감정보 탐지 |
| Secret scanning push protection | 활성화 | push 시점 차단 |
| Private vulnerability reporting | 활성화 | 비공개 보안 신고 |
| Code scanning 기본 설정 | 활성화 | 현재는 GitHub가 감지한 언어 기준으로 동작 |

---

## 3. 현재 활성화된 GitHub 보안 자동화 상세

### 이미 켜진 것
- Dependabot 보안 업데이트
- Secret scanning
- Secret scanning push protection
- Private vulnerability reporting
- Code scanning 기본 설정

### 현재 비활성 상태인 것
- Secret scanning non-provider patterns
- Secret scanning validity checks

이 둘은 GitHub의 지원/플랜/저장소 상태에 따라 바로 활성화되지 않을 수 있습니다.

---

## 4. GitHub bootstrap 스크립트가 실제로 해주는 일
이 스크립트는 저장소 설정을 변경하므로 저장소 관리자만 실행합니다.

`./scripts/bootstrap-github.sh --create-project`

### 자동으로 맞추는 항목
- label 동기화
- 마일스톤 동기화
- GitHub Project 확인/생성
- `PROJECT_URL` variable 동기화
- 자동 병합 허용(`allow_auto_merge=true`)
- 병합 후 브랜치 삭제(`delete_branch_on_merge=true`)

### 자동으로 맞추지 않는 항목
- 브랜치 protection 세부 규칙 전체
- CODEOWNERS 실제 사용자 ID 반영
- `ADD_TO_PROJECT_PAT` secret
- Project board view 세부 구성

즉, bootstrap은 **반복 세팅을 줄이는 역할**이고, 최종 운영 정책까지 완전히 대신하진 않습니다.

---

## 5. 왜 이 조합이 과제에 맞는가

### 과제 요구사항과 연결되는 이점
1. **GitHub history 확보**
   - Issue / PR / review / 병합 이력이 남음
2. **문서와 코드 동시 추적**
   - README, docs, tests를 함께 운영 가능
3. **제출 준비 자동화**
   - zip / README.txt 생성 실수 감소
4. **설정 누락 방지**
   - hooks / bootstrap / verification tasks로 사전 차단
5. **보안 사고 예방**
   - Secret scanning, push protection, vulnerability reporting 적용

---

## 6. 일부러 보류한 자동화와 이유
| 항목 | 보류 이유 |
| --- | --- |
| 완전 자동 머지 | 리뷰와 발표 책임이 남아 있어서 사람이 승인해야 함 |
| Project 커스텀 필드 전체 자동 설계 | GitHub UI 변경 가능성이 있어 Status 정렬까지만 자동화 |
| CI 다중 OS 매트릭스 | Java skeleton 단계에서는 과함 |
| Docker / devcontainer | 현재 단계에선 유지비가 더 큼 |
| 외부 DB 컨테이너 자동 기동 | 과제 규모상 서버형 DB보다 내장형 DB가 단순함 |
| custom CodeQL 워크플로우 | default setup으로 먼저 시작하는 편이 부담이 적음 |
| 강제 commit rule 훅 강화 | 초반 생산성을 과하게 떨어뜨릴 수 있음 |

---

## 7. 사람이 직접 해야 하는 항목
자동화가 있어도 아래는 사람이 책임져야 합니다.

### 문서/설계
- 유스케이스 명세
- UML 설명
- SSD / Operation Contract 설명
- 설계 패턴 적용 이유
- 발표/Q&A 준비

### GitHub 운영
- Project 뷰 다듬기
- 팀원 권한 조정
- CODEOWNERS 실제 ID 반영
- 최종 마일스톤 정리

### 제출 품질 관리
- 데모 시나리오 검토
- 발표 자료 완성도
- README.txt 최종 점검

---

## 8. 현재 기준에서 “남은 수동 체크”
아래는 자동화가 전부 대체하지 않는 항목입니다.

1. **`ADD_TO_PROJECT_PAT` secret 추가 여부 결정**: 없으면 Project 자동 추가/정렬 워크플로우가 건너뜁니다.
2. **CODEOWNERS 실제 팀원 ID 반영**
3. **GitHub Project Board/Table 최종 뷰 정리**
4. **최종 제출 직전 `./gradlew check` + 패키징 재실행**
5. **문서/발표/Q&A 산출물의 사람 검토**

---

## 9. 추천 운영 흐름
1. bootstrap 실행
2. 저장소 관리자만 GitHub bootstrap 실행
3. 이슈 생성
4. `./scripts/start-task.sh <issue> <slug>`로 브랜치 생성
5. 구현/문서/테스트
6. `./scripts/open-pr.sh`로 검증과 PR 생성
7. 필요하면 `./scripts/audit-project.sh`로 전체 정합성 점검
8. CI 확인
9. 리뷰/승인
10. `dev` 병합
11. 제출 직전 `main` 안정화 및 패키징

---

## 10. 구현 시작 전에 꼭 확인할 것
- [ ] 팀원 모두 Java 21 / Git / gh 준비
- [ ] GitHub Project 접근 가능
- [ ] `dev`에서 브랜치 생성 흐름 정상 작동
- [ ] PR 시 `build` 체크가 실제로 보임
- [ ] issue template / PR template이 default 브랜치 기준으로 최신 상태
- [ ] 보안 옵션(특히 Secret scanning / push protection) 활성 상태 유지

---

## 11. 유지보수 팁

### 문서를 갱신해야 하는 순간
- 스크립트 사용법이 바뀔 때
- 브랜치 전략이 바뀔 때
- 리뷰 정책이 바뀔 때
- 제출 형식이 바뀔 때
- GitHub Project 운영 방식이 바뀔 때

### 자동화를 점검해야 하는 순간
- CI가 자주 깨질 때
- 팀원이 새로 합류할 때
- Project 상태와 이슈 라벨이 어긋날 때
- GitHub Actions 경고(예: deprecated runtime)가 뜰 때
- 보안 기능 상태가 바뀔 때

---

## 12. 핵심 결론
이 저장소의 자동화는 **“실 구현 전에 필요한 생산성/품질/보안 baseline”** 을 만드는 데 초점을 맞췄습니다.

즉,
- 반복 작업은 자동화하고
- 제출 실수는 줄이고
- GitHub 협업 이력은 잘 남기고
- 설계/문서/발표의 핵심 판단은 사람이 맡도록 설계했습니다.

이것이 현재 과제 단계에서 가장 현실적인 자동화 전략입니다.
