# 팀 사용 설명서 / 3인 팀 운영 가이드

이 문서는 **사용자(팀원) 입장**에서 저장소를 어떻게 세팅하고, 어떤 순서로 사용하면 되는지를 설명합니다. 목표는 다음 두 가지입니다.

1. 팀원이 저장소를 clone한 직후 **바로 개발을 시작**할 수 있게 한다.
2. GitHub / 로컬 자동화를 활용해서 **반복 작업을 줄이고 제출 품질을 높인다.**

## 1. 준비물
- Git
- Java 21
- Python 3
- GitHub CLI (`gh`) — GitHub 자동 세팅을 쓰려면 필요

권장 확인 명령:
```bash
git --version
java -version
python3 --version
gh --version
```

## 2. 처음 1회만 하면 되는 작업
### 2-1. 저장소 clone
```bash
git clone https://github.com/marcellokim/se-2026-1-term-project-issue-tracker.git
cd se-2026-1-term-project-issue-tracker
```

### 2-2. 로컬 bootstrap 실행
```bash
./scripts/bootstrap-dev.sh
```

이 명령이 하는 일:
- 필수 도구 확인
- `.githooks/` 설치
- `.gitmessage.txt` commit template 등록
- 실행 권한 정리
- Java가 정상 동작하면 `./gradlew check` 실행

> 짧은 별칭을 원하면 `./scripts/bootstrap.sh`를 써도 됩니다.

### 2-3. GitHub 자동 세팅도 같이 하고 싶다면
```bash
gh auth login
./scripts/bootstrap-github.sh --create-project
```

이 명령이 하는 일:
- label 동기화
- milestone 생성
- GitHub Project 존재 여부 확인 및 생성(옵션)
- repository setting 중 auto-merge / merge 후 branch 자동 삭제 활성화
- `PROJECT_URL` repository variable 동기화

> 이슈/PR를 GitHub Project에 자동으로 올리고 싶다면 GitHub repository secret `ADD_TO_PROJECT_PAT`가 필요합니다. `PROJECT_URL` variable은 bootstrap 스크립트가 자동으로 맞춥니다.

## 3. 한눈에 보는 작업 흐름
1. Issue 생성
2. 담당자/목표일/완료조건 기입
3. `./scripts/start-task.sh` 또는 `feature/...` 브랜치 생성
4. 구현 / 테스트 / 문서 반영
5. PR 생성
6. CI 통과 + 리뷰 반영
7. `dev` 머지
8. GitHub Project 상태 업데이트
9. 스크린샷 / UML / Q&A 문서 동기화

## 4. 매일 작업 시작 루틴
```bash
git checkout dev
git pull origin dev
./scripts/start-task.sh 18 recommendation-engine
```

브랜치 이름 예시:
- `feature/18-issue-create-form`
- `docs/use-case-specs`
- `test/issue-service-regression`
- `chore/github-automation`

## 5. 작업은 어떤 순서로 하나?
1. GitHub에서 이슈 생성
2. 이슈를 Project 보드에 올리고 상태 지정
3. 기능/문서/테스트용 브랜치 생성
4. 작업 후 commit
5. push + PR 생성
6. CI 통과 확인
7. 리뷰 반영 후 `dev` 병합

## 6. commit / push 할 때 자동으로 일어나는 일
### pre-commit
- `main` 브랜치 직접 커밋 차단
- 브랜치 이름 규칙 경고
- Java가 있으면 `verifyRepositorySetup` 실행

### pre-push
- Java가 있으면 `./gradlew test verifyRepositorySetup` 실행
- Java가 없으면 경고만 출력하고 CI에 맡김

필요 시 임시 스킵:
```bash
SKIP_GRADLE_PREPUSH=1 git push
```

## 7. GitHub 이슈는 어떻게 쓰나?
현재 템플릿:
- Feature
- Bug
- Test
- Docs
- Chore

모든 이슈에서 최소한 아래는 채우세요.
- 목적
- 작업 내용
- 완료 조건
- 담당자
- 마감 목표
- 관련 화면/클래스 또는 문서 범위

추천 라벨 체계:
- 유형: `type:*`
- 우선순위: `priority:*`
- 상태: `status:*`
- 영역: `area:*`
- 역할: `role:*`

## 8. PR은 어떻게 쓰나?
PR 생성 시 템플릿 체크리스트를 따라 아래를 반드시 적으세요.
- 관련 이슈 번호
- 과제 요구사항과의 연결
- 테스트/검증 결과
- 남은 TODO

PR 생성 후 자동으로 기대되는 것:
- GitHub Actions CI 실행
- 변경 파일 기반 라벨 자동 부착

## 9. 3인 팀 추천 역할 분담
### 팀원 A
- GitHub 운영 / 문서 총괄 / README / 발표자료 동기화

### 팀원 B
- 도메인 모델 / persistence / 테스트 구조 담당

### 팀원 C
- UI 흐름 / 데모 시나리오 / 스크린샷 정리 담당

> 실제 구현 중에는 역할이 섞여도 되지만, 담당 축은 하나씩 두는 것이 좋습니다.

## 10. GitHub UI에서 직접 해야 하는 설정
### 10-1. Branch protection / Ruleset
- `main` direct push 금지
- `dev` direct push 금지 또는 제한
- PR merge 전 CI 필수
- 최신 브랜치 기준 재검증(required up-to-date) 활성화 권장
- squash merge 허용
- merge 후 branch 자동 삭제 활성화

### 10-2. GitHub Project
Project 이름 예시:
- `SE Term Project 2026-1`

권장 필드:
- Status: Backlog / Ready / In Progress / In Review / Done
- Priority: High / Medium / Low
- Role: admin / PL / dev / tester
- Milestone: Mid / Final / Presentation
- Owner: 담당자

권장 View:
- Board view (일상 운영)
- Table view (정리/필터)
- Timeline 또는 iteration view (발표 전 일정 관리)

### 10-3. 팀 권한 / 변수 / 시크릿
- 3명 모두 write 이상 권한
- owner 1명은 repository settings 관리
- `CODEOWNERS`를 실제 GitHub 핸들로 업데이트
- `ADD_TO_PROJECT_PAT` secret 추가 시 Project auto-add 활성화
- `PROJECT_URL` variable은 bootstrap 스크립트가 자동 동기화

## 11. 과제 요구사항과 연결해서 주의할 점
- Java로 구현해야 함
- MVC 구조를 문서와 코드에서 모두 드러내야 함
- UI toolkit 두 개 이상을 재사용 가능한 모델 구조 위에 올려야 함
- JUnit 테스트를 남겨야 함
- GitHub 히스토리, Project 진행 내역, 기여 증빙이 필요함
- AI 흔적이 노골적으로 남지 않도록 최종 산출물 정리에 주의해야 함

## 12. 최종 제출 직전 체크리스트
- [ ] README 최신화
- [ ] 프로젝트 문서 PDF 초안/최종본 준비
- [ ] 발표 슬라이드 준비
- [ ] 테스트 코드 정리
- [ ] 데모 영상 시나리오 정리
- [ ] GitHub Project history 스크린샷 확보
- [ ] 팀원별 기여 증빙 확보

## 13. 자주 쓰는 명령 모음
```bash
./scripts/bootstrap-dev.sh
./scripts/bootstrap-github.sh --create-project
./gradlew check
./gradlew verifySubmissionMetadata
git checkout dev
git pull origin dev
./scripts/start-task.sh 18 your-task
./scripts/package-submission.sh --team-number 03 --member 홍길동 --member 김철수 --member 이영희 --project-url <url>
```
