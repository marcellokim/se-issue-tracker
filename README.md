# SE 2026-1 텀프로젝트 - 이슈 관리 시스템

소프트웨어공학 2026-1 텀 프로젝트 **이슈 관리 시스템(ITS)** 저장소입니다.  
이 저장소는 과제 구현뿐 아니라 **GitHub 협업 흐름, 제출 준비, 문서화, 자동화 운영**까지 한 곳에서 관리하도록 구성되어 있습니다.

## 1. 과제 제출 정보
- **과제명**: SE 2026 Spring Term Project - 이슈관리 시스템 개발
- **구현 언어**: Java
- **최종 제출 마감**: 2026-06-02 21:00
- **발표 일정**: 2026-06-03 / 2026-06-08 수업 시간
- **GitHub 저장소**: <https://github.com/marcellokim/se-issue-tracker>
- **GitHub Project**: <https://github.com/users/marcellokim/projects/1>

## 2. 팀 정보
| 구분 | 내용 |
| --- | --- |
| 저장소 관리자 | marcellokim |
| 팀 구성 | 3인 팀 |
| 추가 팀원 정보 | 최종 제출용 README.txt, 보고서 표지, 발표 자료에 맞춰 반영 |
| 작업 방식 | 이슈 생성 → 브랜치 생성 → PR 작성 → 리뷰 → 병합 |

## 3. 과제 핵심 요구사항 요약
루트의 `SE_Term_Project_2026-1.pdf`를 최상위 과제 요구사항 원문으로 고정합니다. 아래 요약이나 추적 문서가 애매할 때는 이 PDF가 우선입니다.

`SE_Term_Project_2026-1.pdf` 기준 핵심 요구사항:
- 계정 추가: `admin`, `PL`, `dev`, `tester`
- 이슈 등록 / 브라우즈 / 검색 / 상세 조회 / 코멘트 / 상태 변경
- 통계 분석: 일/월별 이슈 발생 추이
- 해결 이력 기반 assignee 자동 추천
- 영속 저장소 필요: 과제 원문은 File System 또는 DB를 허용하지만, 이 저장소의 표준 구현은 **DB 기반 persistence**로 진행
- **MVC 아키텍처 적용 필수**
- **두 개 이상의 UI Toolkit 기반 UI 제공 필수**
- **JUnit 테스트 및 GitHub 기반 협업 기록 필수**

## 4. 현재 저장소 상태
- [x] Gradle + Java 21 기본 골격
- [x] GitHub 이슈 / PR 템플릿
- [x] GitHub Actions CI / PR 자동 라벨링 / Dependabot
- [x] 초기 세팅 / git hook / 라벨 동기화 / 제출 패키징 스크립트
- [x] 사용자용 운영 가이드 및 자동화 옵션 문서
- [ ] 도메인 모델 구현
- [ ] UI(JavaFX + Swing) 구현
- [ ] 통계 / 추천 기능 구현
- [ ] 최종 발표 자료 / 동영상 / 프로젝트 문서 PDF 완성

## 5. 로컬 실행 및 개발 시작
### 필수 환경
- JDK 21
- Git
- Python 3
- GitHub CLI(`gh`) 권장

### 빠른 시작
```bash
git clone https://github.com/marcellokim/se-issue-tracker.git
cd se-issue-tracker
./scripts/bootstrap.sh
```

이미 `se-issue-tracker` 폴더 안에 있다면 `git clone ...`을 다시 실행하지 마세요. 기존 폴더 안에서 다시 clone하면 `se-issue-tracker/se-issue-tracker`처럼 폴더가 중복되고, 바깥 폴더에서 `./scripts/open-pr.sh`가 없다고 보일 수 있습니다.

현재 위치가 맞는지 확인하려면:
```bash
test -f scripts/open-pr.sh && echo "OK: 저장소 루트입니다"
```

`scripts/open-pr.sh`가 없다고 나오면 대부분 저장소 루트가 아닌 위치에 있는 것입니다. 아래 명령으로 현재 위치를 먼저 확인하세요.

```bash
pwd
ls
```

`se-issue-tracker` 폴더가 또 보이면 한 단계 안으로 들어가거나, 중복 clone 폴더를 정리한 뒤 다시 시작합니다.

### 기본 검증
```bash
./gradlew check
```

### 정해진 작업 흐름
일반 기능/문서/테스트 작업은 반드시 아래 흐름으로 진행합니다.

```bash
# 1. 이슈 번호를 기준으로 개인 작업 브랜치 생성
./scripts/start-task.sh 12 issue-search-ui

# 2. 작업 후 로컬 검증과 커밋
./gradlew check
git add .
git commit

# 3. dev 대상 PR 생성
./scripts/open-pr.sh
```

이 스크립트는 최신 `dev` 기준선 확인, push, PR 생성, 이슈 상태 라벨 이동, Project 정렬까지 처리합니다.

### 워크플로우 강제 규칙
이 저장소는 아래 우회 흐름을 문서상 금지하는 수준이 아니라 **로컬 hook + GitHub Actions + branch protection**으로 차단합니다.

- 일반 팀원 PR은 `dev` 대상으로만 허용
- PR head 브랜치는 `feature/<issue>-<slug>`, `docs/<issue>-<slug>`, `test/<issue>-<slug>`, `chore/<issue>-<slug>`만 허용
- `main` 대상 PR은 관리자 bypass 계정만 허용
- `main` / `dev` 직접 push와 직접 commit은 로컬 hook 및 GitHub branch protection으로 차단
- workflow guard, branch protection bootstrap, PR/start-task 스크립트, git hook 같은 우회 지점 수정은 관리자만 허용

관리자 bypass 계정은 repository variable `WORKFLOW_BYPASS_USERS`로 관리합니다. 기본 bootstrap은 저장소 owner를 bypass 계정으로 설정합니다.

저장소 관리자가 GitHub 보호 규칙을 다시 동기화해야 할 때만 실행합니다.

```bash
./scripts/bootstrap-github.sh
```

### 금지되는 흐름
아래 방식은 저장소 이력과 자동화 정합성을 깨뜨릴 수 있으므로 사용하지 않습니다.

- `main`에 직접 커밋
- `dev`에 직접 커밋
- 기능 작업 PR을 `main`으로 올리기
- `dev`에서 코드를 바로 수정한 뒤 PR 올리기
- 기존 `se-issue-tracker` 폴더 안에서 다시 `git clone ...` 실행
- `./scripts/start-task.sh` 없이 임의 브랜치 이름으로 작업 시작
- `.github/workflows/`, `.githooks/`, `scripts/start-task.sh`, `scripts/open-pr.sh`, `scripts/validate-workflow-guard.sh`, `scripts/lib/bootstrap_github.py`를 일반 작업 PR에서 수정

예외는 저장소 관리자가 릴리즈/동기화 목적을 명확히 알고 수행하는 경우뿐입니다. 일반 팀원 작업은 항상 `feature/<issue>-<slug>`, `docs/<issue>-<slug>`, `test/<issue>-<slug>`, `chore/<issue>-<slug>` 브랜치에서 시작하고 PR은 `dev`로 올립니다.

## 6. 자동화 구성
이 저장소는 코딩 전/초기 단계 생산성을 높이기 위해 아래 자동화를 포함합니다.

- **이슈 양식 → GitHub Project 자동 등록**
- **PR 자동 라벨링**: 변경 파일 경로 기준 라벨 부여
- **Gradle CI**: `./gradlew clean check`
- **보안 자동화**: Dependabot 보안 업데이트, Secret scanning, push protection, private vulnerability reporting, GitHub code scanning 기본 설정
- **Dependabot**: Gradle / GitHub Actions 주간 업데이트 제안
- **Git hook**: pre-commit / pre-push 검증
- **Workflow Guard**: `main`/`dev` 우회 PR, 잘못된 브랜치 이름, 보호 자동화 수정 시도 차단
- **작업 시작/PR 스크립트**: `start-task.sh`, `open-pr.sh`로 초보자용 Git 흐름 고정
- **Project 상태 정렬**: `sync-project-board.sh`로 이슈/PR 상태 라벨을 Project 보드에 반영
- **자동화 헬스체크**: `audit-project.sh`와 Gradle `auditAutomation`으로 문서/스크립트/Project 정합성 점검
- **커밋 메시지 템플릿**: Lore commit protocol 형식 자동 적용
- **라벨 동기화 / GitHub 초기 설정 스크립트**
- **제출 zip 스크립트**: 제출 형식 zip + `README.txt` 자동 생성

## 7. 주요 문서
- [사용 설명서 / 팀 운영 가이드](docs/team-setup-manual.md)
- [요구사항 추적표](docs/requirements-traceability.md)
- [프로젝트 관리 계획](docs/project-management-plan.md)
- [자동화 옵션 조사 및 적용 현황](docs/automation-playbook.md)
- [보안 정책](SECURITY.md)
- [기본 가정 문서](docs/assumptions.md)
- [Q&A / 의사결정 기록](docs/qna.md)

## 8. 저장소 구조
```text
.
├─ .github/                 # 이슈/PR 템플릿, Actions, CODEOWNERS
├─ .githooks/               # pre-commit / pre-push hook
├─ config/github/           # 라벨 / 마일스톤 정의
├─ docs/                    # 과제 문서, 사용 가이드, UML, 스크린샷
├─ gradle/                  # Gradle wrapper
├─ scripts/                 # 초기 세팅, 브랜치, 라벨, 제출 자동화
├─ src/main/java/           # 애플리케이션 소스
├─ src/test/java/           # JUnit 테스트
├─ SE_Term_Project_2026-1.pdf # 과제 요구사항 원문
├─ build.gradle
├─ gradle.properties
└─ README.md
```

## 9. 제출물 체크리스트
최종 zip에는 과제문 기준으로 다음이 포함되어야 합니다.
- README.txt
- 발표 슬라이드
- 프로젝트 문서 PDF (60p 이내)
- 소스코드 / 실행파일 / JUnit 테스트 / 데이터
- 프로젝트 소개 동영상

제출 패키징 예시:
```bash
./scripts/package-submission.sh \
  --team-number 03 \
  --member 홍길동 \
  --member 김철수 \
  --member 이영희 \
  --project-url https://github.com/users/marcellokim/projects/1  # 생략 시 PROJECT_URL variable 사용
```

## 10. 운영 원칙
- `main`: 제출 가능한 안정 버전
- `dev`: 통합 브랜치
- `feature/<issue>-<slug>`, `docs/<issue>-<slug>`, `test/<issue>-<slug>`, `chore/<issue>-<slug>`: 개인 작업 브랜치
- 모든 일반 작업은 **이슈 생성 → `./scripts/start-task.sh`로 개인 브랜치 생성 → 작업/검증/커밋 → `./scripts/open-pr.sh`로 `dev` 대상 PR 생성 → 리뷰 → `dev` 병합** 순서로 진행
- `main` 대상 PR, `main` 직접 커밋, `dev` 직접 커밋, 보호 자동화 수정은 일반 작업 흐름에서 금지
- 저장소 관리자는 릴리즈/보호 규칙 정비 같은 예외 상황에서만 bypass하며, bypass 계정은 `WORKFLOW_BYPASS_USERS`로 명시
- 구현 내용은 문서, 스크린샷, 테스트와 함께 남깁니다.

## 11. 제출 직전 반드시 확인할 것
- README / README.txt / GitHub Project 링크 최신화
- 팀원 이름, 학번, 팀 번호 최신 반영
- UML / 유스케이스 / SSD / Operation Contract / 테스트 문서 보강
- 발표 자료 / 데모 시나리오 / 동영상 정리
- AI 사용 흔적이 남지 않도록 문서와 코드 이해 여부를 팀 전체가 직접 검토
