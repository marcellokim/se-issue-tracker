# 프로젝트 자동화/생산성 플레이북

이 문서는 **SE_Term_Project_2026-1.pdf** 과제를 수행하기 위해, 구현 이전 단계에서 적용 가능한 자동화와 운영 옵션을 정리한 문서입니다.

## 1. 자동화 목표
- 3인 팀이 같은 규칙으로 브랜치/이슈/PR을 운영한다.
- 팀원이 저장소를 clone 한 뒤 **한 번의 bootstrap** 으로 작업 가능한 상태가 된다.
- GitHub 기록이 평가 자료로 쓰일 수 있도록 이슈-PR-커밋 흐름을 명확히 남긴다.
- 최종 문서/발표/테스트 준비까지 이어질 수 있는 기본 구조를 미리 만든다.

## 2. 지금 저장소에 구현된 자동화
| 항목 | 상태 | 목적 | 사용 방법 |
| --- | --- | --- | --- |
| `scripts/bootstrap-dev.sh` | 구현 | 새 팀원 로컬 초기 세팅 | `./scripts/bootstrap-dev.sh` |
| `.githooks/pre-commit` | 구현 | `main` 직접 커밋 방지, 브랜치 규칙 유도 | bootstrap 시 자동 설치 |
| `.githooks/pre-push` | 구현 | push 전 `./gradlew test verifyRepositorySetup` 실행 | bootstrap 시 자동 설치 |
| `scripts/bootstrap-github.sh` | 구현 | label / milestone / project / repo setting / project URL variable 자동화 | `./scripts/bootstrap-github.sh --create-project` |
| `scripts/start-task.sh` | 구현 | 이슈 번호 기반 브랜치 생성 표준화 | `./scripts/start-task.sh 18 recommendation-engine` |
| `scripts/package-submission.sh` | 구현 | 제출용 zip + README.txt 자동 생성 | `./scripts/package-submission.sh --team-number ...` |
| `.github/workflows/gradle.yml` | 구현 | PR / push 시 CI 자동 실행 | GitHub Actions |
| `.github/workflows/pr-labeler.yml` | 구현 | 변경 파일 기준 PR 자동 라벨링 | GitHub Actions |
| `.github/workflows/add-to-project.yml` | 옵션 구현 | 이슈/PR를 GitHub Project에 자동 추가 | secret + variable 필요 |
| `.github/dependabot.yml` | 구현 | Gradle / Actions 의존성 자동 PR | GitHub Dependabot |
| GitHub Security & Analysis 설정 | 구현 | 보안 이벤트 자동 탐지 및 신고 경로 마련 | GitHub 저장소 설정 |
| `.editorconfig`, `.gitattributes`, `.java-version` | 구현 | 개발 환경 일관성 유지 | clone 후 자동 적용 |
| `CODEOWNERS` | 기본값 구현 | 리뷰 책임자 자동 요청 기반 | 팀 GitHub handle로 교체 |
| `build.gradle` custom tasks | 구현 | repo setup / submission metadata 검증 | `./gradlew check`, `verifySubmissionMetadata` |

## 2-1. 현재 활성화된 GitHub 보안 자동화
- Dependabot security updates: **활성화**
- Secret scanning: **활성화**
- Secret scanning push protection: **활성화**
- Private vulnerability reporting: **활성화**
- GitHub code scanning default setup: **활성화**

아래 항목은 현재 GitHub 측 제약/지원 상태 때문에 활성화되지 않았습니다.
- Secret scanning non-provider patterns
- Secret scanning validity checks

## 3. 자동화 옵션 검토 결과
### 적극 적용한 항목
1. **CI 자동화**: 테스트 실패를 PR 단계에서 빠르게 발견하기 위해 유지
2. **PR 라벨 자동화**: 문서/테스트/워크플로우 변경을 즉시 구분
3. **GitHub bootstrap 스크립트**: 반복적인 label/milestone/project/repo setting 세팅 제거
4. **로컬 git hooks + commit template**: 실수(main 직접 커밋, 미검증 push) 감소
5. **Dependabot**: 초기 프로젝트에서도 Gradle/Actions 버전 관리 부담 축소
6. **Submission packaging**: 제출 직전 수작업 감소
7. **GitHub 보안 옵션 활성화**: 저장소 수준에서 취약점 탐지/차단 기능 즉시 사용

### 문서화만 한 항목 (수동 또는 토큰 필요)
1. **Branch protection**
   - `main`: direct push 금지, PR merge only
   - `dev`: PR merge only 권장
   - Required status checks: `build` (Gradle CI workflow의 job 이름)
2. **GitHub Project 자동화 심화**
   - Project custom field(Status, Sprint, Owner) 생성
   - `Backlog -> Ready -> In Progress -> In Review -> Done` 뷰 구성
   - issue/PR 자동 추가 workflow는 준비되어 있으며 `ADD_TO_PROJECT_PAT` secret 설정 후 활성화됨 (`PROJECT_URL` variable은 bootstrap 스크립트가 자동 동기화)
3. **CODEOWNERS 실제 팀 반영**
   - 현재는 `@marcellokim` fallback
   - 팀원 GitHub ID 확정 시 즉시 교체
4. **Repository Settings**
   - Squash merge 사용 여부
   - PR review 최소 인원
   - 자동 delete branch 여부

## 4. 왜 일부 자동화는 보류했는가
| 보류 항목 | 이유 |
| --- | --- |
| 자동 Project 카드 이동 고급 워크플로우 | 기본 auto-add 수준은 구현했지만, 상태 전이까지 완전 자동화하면 과도하게 복잡해짐 |
| PR 생성 자동화 스크립트 | `gh pr create`는 유용하지만 팀 선호와 리뷰 문화에 따라 차이 큼 |
| CI 다중 OS 매트릭스 | 현재는 Java skeleton 단계라 과함 |
| 자동 릴리즈 노트 생성 | 최종 제출 과제에서는 우선순위 낮음 |
| Docker / devcontainer | 아직 앱 구조와 UI toolkit 조합이 확정되지 않아 비용 대비 이득이 작음 |
| DB 컨테이너 자동 기동 | 과제는 파일 기반 persistence도 허용하므로 성급한 DB 도입이 오버엔지니어링일 수 있음 |
| CodeQL / 대형 정적분석 파이프라인 | 구현 전 단계에서는 노이즈가 크고 유지 비용이 높음 |
| 강제 커밋 메시지 규칙 훅 | 초반 생산성을 해칠 수 있어, 템플릿 중심으로 유도 |

## 5. GitHub에서 수동으로 꼭 확인할 설정
1. **Repository > Settings > Branches**
   - `main`, `dev` 보호 규칙 생성
2. **Repository > Settings > General**
   - Auto-delete head branches 활성화 권장
   - Squash merge 기본 사용 권장
3. **Repository > Settings > Actions**
   - Actions 사용 가능 여부 확인
   - Repository secret `ADD_TO_PROJECT_PAT` 추가
   - Repository variable `PROJECT_URL`은 bootstrap 스크립트가 자동 동기화
4. **GitHub Project**
   - 뷰: Board / Table / By Assignee
   - 필드: Status, Priority, Milestone, Assignee, Linked PR

## 6. 추천 운영 흐름
1. 이슈 생성 (template 사용)
2. GitHub Project에 상태 반영
3. `dev` 최신화 후 `feature/...` 브랜치 생성
4. 구현/문서 작업
5. PR 생성 → 자동 라벨 + CI 확인
6. 리뷰 후 `dev` 병합
7. 데모 가능한 시점마다 `main` 안정화

## 7. 추천 운영 결론
현재 저장소 단계에서는 다음 조합이 가장 현실적입니다.
- 저장소 내부 자동화: **CI + hooks + commit template + bootstrap + label/milestone/project bootstrap + submission packaging**
- GitHub 수동 설정: **ruleset + required checks + project field/view 정리 + reviewer policy**
- 사람이 직접 책임질 부분: **설계 설명, UML, 발표자료, 테스트 목적 서술**

## 8. 최종 제출과 연결되는 이유
- GitHub issue / PR / project history 자체가 **평가 자료**가 됨
- CI와 테스트 기록은 **테스트 수행 내역** 정리에 직접 활용 가능
- README / docs 구조는 **프로젝트 문서, 발표자료, README.txt** 준비의 베이스가 됨
