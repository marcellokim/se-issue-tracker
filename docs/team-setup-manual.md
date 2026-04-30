# 팀 사용 설명서 / 3인 팀 운영 가이드

이 문서는 **3인 팀이 이 저장소를 실제로 어떻게 써야 하는지**를 단계별로 설명하는 실무형 안내서입니다.  
목표는 단순합니다.

1. 새 팀원이 저장소를 clone한 직후 **헷갈리지 않고 바로 작업을 시작**하게 한다.
2. GitHub/로컬 자동화를 활용해 **반복 작업을 줄이고 제출 품질을 일정하게 유지**한다.
3. 과제 평가에 중요한 **GitHub 이력, 문서, 테스트, 제출 증적**을 놓치지 않게 한다.

---

## Git 초보용 5분 흐름

평소에는 아래 두 스크립트만 기억하면 됩니다.

```bash
# 1. 작업 시작: dev 최신화, 기준선 확인, 개인 브랜치 생성까지 자동 처리
./scripts/start-task.sh <이슈번호> <작업이름>

# 2. 코드/문서 수정 후 커밋
./gradlew check
git add .
git commit

# 3. PR 올리기: 검증, push, dev 대상 PR 생성까지 자동 처리
./scripts/open-pr.sh
```

비유:
- `main` = 교수님께 낼 최종본
- `dev` = 팀 작업을 합치는 공용 합본
- `feature/...` = 내 개인 작업 공간
- PR = 내 작업을 공용 합본에 넣어 달라는 요청

규칙:
- `main`에는 직접 작업하지 않습니다.
- `dev`에도 직접 커밋하지 않습니다.
- 내 작업은 `feature/...`, `docs/...`, `test/...`, `chore/...` 브랜치에서 합니다.
- PR은 항상 `dev`로 올립니다.

---

## 0. 이 문서가 답하는 질문
이 문서는 아래 질문에 답하도록 작성되었습니다.

- 처음 clone한 뒤 무엇부터 해야 하나?
- `main`, `dev`, `feature/*`는 어떻게 써야 하나?
- 이슈/PR/리뷰/CI는 어떤 흐름으로 돌아가나?
- GitHub Project는 누가 어떻게 관리하나?
- 제출용 zip은 어떻게 만들고 무엇을 확인해야 하나?
- 보안 이슈나 권한 문제는 어디서 해결하나?

---

## 1. 저장소 운영 원칙 한눈에 보기

### 브랜치 역할
- `main`: **제출 가능한 안정 버전**
- `dev`: **통합 브랜치**
- `feature/<issue>-<slug>`: 기능 작업 브랜치
- `docs/<slug>`: 문서 작업 브랜치
- `test/<slug>`: 테스트 보강 브랜치
- `chore/<slug>`: 자동화/환경설정/리팩터링 브랜치

### 기본 흐름
1. GitHub 이슈 생성
2. 브랜치 생성
3. 구현/문서/테스트 작업
4. PR 생성
5. CI 확인
6. 리뷰 승인
7. `dev` 머지
8. 안정화 시점에 `main` 반영

### 팀 규칙 핵심
- 기능 작업은 **이슈에서 시작**합니다.
- `main` 직접 작업은 하지 않습니다.
- PR에는 **검증 결과와 문서 반영 여부**를 남깁니다.
- 화면/설계/테스트 증적은 가능한 한 **같은 시점에 축적**합니다.

---

## 2. 처음 1회만 하면 되는 준비

### 2-1. 필수 도구
- Git
- JDK 21
- Python 3
- GitHub CLI(`gh`) 권장
- IDE(IntelliJ IDEA 또는 Eclipse 권장)

권장 확인 명령:
```bash
git --version
java -version
python3 --version
gh --version
```

### 2-2. 저장소 clone
```bash
git clone https://github.com/marcellokim/se-issue-tracker.git
cd se-issue-tracker
```

### 2-3. 로컬 bootstrap 실행
```bash
./scripts/bootstrap.sh
```

실제로 내부에서 수행하는 일:
1. 필수 도구 존재 확인
2. `.githooks/` 설치
3. `.gitmessage.txt` commit template 등록
4. 스크립트 실행 권한 정리
5. Java가 있으면 `./gradlew check` 실행

원본 스크립트를 직접 쓰고 싶다면:
```bash
./scripts/bootstrap-dev.sh
```

### 2-4. 저장소 관리자가 GitHub 설정도 맞춰야 한다면
이 단계는 저장소 관리자 1명만 실행합니다. 일반 팀원은 실행하지 않아도 됩니다.

```bash
gh auth login
./scripts/bootstrap-github.sh --create-project
```

이 스크립트가 맞춰주는 항목:
- label 동기화
- 마일스톤 동기화
- GitHub Project 존재 여부 확인/생성
- 자동 병합 허용(`allow_auto_merge=true`)
- 병합 후 브랜치 삭제(`delete_branch_on_merge=true`)
- `PROJECT_URL` repository variable 동기화

> 참고: `PR/이슈 -> Project 자동 추가`를 완전히 활성화하려면 `ADD_TO_PROJECT_PAT` secret이 추가로 필요합니다.

---

## 3. 현재 자동화가 실제로 해주는 일

### 로컬 자동화
| 항목 | 역할 |
| --- | --- |
| `scripts/bootstrap.sh` | 새 팀원 초기 세팅 |
| `scripts/start-task.sh` | 기준선 확인 + 이슈 번호 기반 브랜치 생성 |
| `scripts/open-pr.sh` | 검증 + push + `dev` 대상 PR 생성 + Project 상태 정렬 |
| `scripts/audit-project.sh` | main/dev, 문서, 이슈, Project 정합성 점검 |
| `scripts/sync-project-board.sh` | 이슈/PR 라벨 기준으로 Project 상태 정렬 |
| `scripts/package-submission.sh` | 제출 zip + `README.txt` 자동 생성 |
| `.githooks/pre-commit` | 위험한 브랜치 작업/설정 누락 방지 |
| `.githooks/pre-push` | push 전 테스트/기본 검증 |
| `.gitmessage.txt` | Lore commit protocol 메시지 템플릿 |

### GitHub 자동화
| 항목 | 역할 |
| --- | --- |
| 이슈 양식 | 형식 통일, Project 자동 등록 |
| PR 템플릿 | 검증/문서/증빙 누락 방지 |
| Gradle CI | `build` 체크 제공 |
| PR Labeler | 변경 파일 기준 라벨 자동 분류 |
| Project 정합성 유지 | 이슈/PR 이벤트와 매일 00:17 KST에 Project 상태 점검/정렬 |
| Dependabot | 의존성/Actions 업데이트 추적 |
| Security 설정 | secret/push protection/code scanning/vulnerability reporting |

---

## 4. 일상 작업 흐름

### 4-1. 작업 시작 전
1. GitHub 이슈 번호를 확인합니다.
2. 작업 이름을 짧은 영어 단어로 정합니다. 예: `account-role-model`
3. 작업트리에 커밋되지 않은 변경이 없는지 확인합니다.

### 4-2. 브랜치 생성
```bash
./scripts/start-task.sh 18 recommendation-engine
```

예시 결과:
```text
feature/18-recommendation-engine
```

### 4-3. 작업 중 체크 포인트
- 도메인/서비스/UI 분리가 깨지지 않는지 확인
- 문서/스크린샷이 필요한 변경인지 체크
- 테스트 추가가 필요한 변경인지 체크
- PR 크기가 너무 커지지 않는지 확인

### 4-4. PR 올리기 전
```bash
./gradlew check
git add .
git commit
./scripts/open-pr.sh
```

추가 점검:
- README 반영이 필요한가?
- `docs/qna.md` 또는 UML 업데이트가 필요한가?
- 이 변경이 과제 요구사항 중 무엇에 대응하는가?
- 스크린샷이 필요한가?

---

## 5. 이슈 작성 가이드

### 현재 제공되는 이슈 유형
- `feature`
- `bug`
- `test`
- `docs`
- `chore`

### 모든 이슈에 공통으로 적어야 하는 것
- 목적
- 작업 내용
- 완료 조건
- 담당자
- 목표일
- 관련 화면/클래스/문서 범위

### 좋은 이슈 예시
- `feature`: “PL이 new 상태 이슈를 조회하고 assignee를 지정할 수 있도록 한다”
- `bug`: “fixed -> resolved 상태 전환 후 reporter 검색이 누락되는 문제 수정”
- `docs`: “SSD 2개와 Operation Contract 2개를 문서에 추가”
- `chore`: “GitHub project 워크플로우 및 label 정리”

---

## 6. PR 작성 가이드

### PR에 반드시 들어가야 하는 것
- 관련 이슈 번호 (`Closes #...`)
- 이번 변경의 목적
- 검증 결과
- 과제 요구사항 영향
- 문서 반영 여부
- 리뷰 포인트

### 리뷰어가 보고 싶어 하는 정보
- 왜 이 변경이 필요한가?
- 어떤 테스트를 했는가?
- 문서도 같이 갱신했는가?
- 이후 후속 작업은 무엇인가?

### 리뷰 승인 전 확인할 것
- CI 통과 여부
- 미해결 댓글 여부
- 오래된 리뷰 상태 여부
- 브랜치가 최신인지 여부

---

## 7. git hooks가 막을 수 있는 것

### pre-commit
- `main` 브랜치 직접 커밋 방지
- 권장 브랜치 이름 형식 안내
- Java가 있으면 `verifyRepositorySetup` 실행

### pre-push
- Java가 있으면 `./gradlew test verifyRepositorySetup` 실행
- Java가 없으면 경고 후 CI에 위임

### 임시 스킵이 필요한 경우
정말 필요한 경우에만:
```bash
SKIP_GRADLE_PREPUSH=1 git push
```

> 단, 이 경우 push 후 GitHub Actions 결과를 반드시 직접 확인해야 합니다.

---

## 8. GitHub Project 운영법

### 현재 권장 상태 흐름
`대기 -> 준비됨 -> 진행 중 -> 리뷰 중 -> 완료`

### 각 상태의 의미
- **대기**: 아직 바로 시작하지 않는 일감
- **준비됨**: 시작 조건이 갖춰진 일감
- **진행 중**: 브랜치 작업 중
- **리뷰 중**: PR 생성, 리뷰/QA 진행 중
- **완료**: 병합 완료, 문서 반영 완료

### 권장 필드
- Type
- Priority
- Owner
- 마일스톤

### 권장 운영 방식
- 이슈 생성 직후 Project에 반영
- PR 생성 시 `scripts/open-pr.sh`가 이슈 라벨을 `status:review`로 바꾸고 Project 상태를 `리뷰 중`으로 이동
- 병합 후 `완료`로 이동
- 데모 직전에는 `마일스톤` 기준으로 필터링

### 관리자가 한 번에 점검할 때
```bash
./scripts/audit-project.sh
```

Project 상태만 다시 맞출 때:
```bash
./scripts/sync-project-board.sh --apply
```

---

## 9. 3인 팀 역할 분담 예시

### 역할 예시 A
- **팀원 A**: GitHub 운영 / 문서 총괄 / README / 발표자료 동기화
- **팀원 B**: 도메인 모델 / persistence / 테스트 구조
- **팀원 C**: UI 흐름 / 데모 시나리오 / 스크린샷 정리

### 역할 예시 B
- **A**: 관리자/PL 기능
- **B**: dev/tester 기능 + persistence
- **C**: 통계/추천 + 테스트/문서

### 중요한 원칙
- 특정 1명만 아는 영역을 만들지 않기
- 발표/Q&A 대응을 위해 전체 구조를 모두가 이해하기
- 기능/테스트/문서를 가능한 한 함께 움직이기

---

## 10. 보안 및 권한 관련 안내

### 현재 활성화된 보안 기능
- Dependabot 보안 업데이트
- Secret scanning
- Secret scanning push protection
- Private vulnerability reporting
- GitHub code scanning 기본 설정

### 민감한 보안 이슈가 생기면
공개 이슈에 쓰지 말고 `SECURITY.md` 절차를 따릅니다.

### 팀원 권한 추천
- 일반 팀원: **write**
- 저장소 관리자: 설정 관리 가능한 상위 권한

> 실제 `CODEOWNERS`와 리뷰 정책은 팀원 GitHub ID가 모두 확정된 뒤 갱신하는 것이 좋습니다.

---

## 11. 제출 직전 체크리스트

### 문서
- [ ] README 최신화
- [ ] 프로젝트 문서 PDF 최신화
- [ ] 유스케이스 명세 포함 여부 확인
- [ ] UML/SSD/Operation Contract 포함 여부 확인
- [ ] Q&A/가정 문서 반영 여부 확인

### 코드/테스트
- [ ] `./gradlew check`
- [ ] 핵심 JUnit 테스트 정리
- [ ] 데모 데이터/계정 준비

### GitHub 증빙
- [ ] Project history 스크린샷 확보
- [ ] 팀원별 PR/이슈/commit 증빙 확보
- [ ] 마일스톤별 진행 흐름 정리

### 제출물
- [ ] 발표 슬라이드
- [ ] 동영상
- [ ] `README.txt`
- [ ] 최종 zip 생성

---

## 12. 제출 zip 생성 방법

기본 예시:
```bash
./scripts/package-submission.sh \
  --team-number 03 \
  --member 홍길동 \
  --member 김철수 \
  --member 이영희 \
  --project-url https://github.com/users/marcellokim/projects/1
```

### 이 스크립트가 하는 일
- Gradle check 실행(기본)
- 제출용 staging 디렉터리 생성
- `README.txt` 자동 생성
- zip 파일 생성

### `--skip-check`를 쓰는 경우
- JDK가 없어서 임시로 생성만 하고 싶을 때
- CI는 통과했지만 로컬 환경이 제한적일 때

단, 최종 제출 직전에는 **반드시 정상 검증 후 다시 생성**하는 것을 권장합니다.

---

## 13. 자주 쓰는 명령 모음
```bash
# 최초 세팅
./scripts/bootstrap.sh

# 저장소 관리자용 GitHub 설정 동기화
./scripts/bootstrap-github.sh --create-project

# 작업 브랜치 생성
./scripts/start-task.sh 18 recommendation-engine

# PR 생성
./scripts/open-pr.sh

# 자동화/Project 정합성 점검
./scripts/audit-project.sh

# Project 상태만 수동 정렬
./scripts/sync-project-board.sh --apply

# 기본 검증
./gradlew check

# 제출 metadata placeholder 검사
./gradlew verifySubmissionMetadata

# 제출 zip 생성
./scripts/package-submission.sh --team-number 03 --member 홍길동 --member 김철수 --member 이영희 --project-url <url>
```

---

## 14. 자주 생기는 문제와 해결법

### 1) Java가 없어서 Gradle이 안 됨
- JDK 21 설치
- `java -version` 확인
- IDE SDK도 21로 통일

### 2) 승인했는데 PR이 안 머지됨
보통 아래 중 하나입니다.
- CI가 아직 도는 중
- 새 커밋 때문에 이전 승인 stale 처리됨
- unresolved comment가 있음
- 승인자의 권한이 read여서 유효 승인으로 안 잡힘

### 3) Project 자동 추가가 안 됨
- `PROJECT_URL` variable 확인
- `ADD_TO_PROJECT_PAT` secret 확인
- 워크플로 로그 확인

### 4) 제출 zip 이름/경로가 이상함
- `--member` 값에 특수문자가 많은지 확인
- `--output-dir` 지정 여부 확인
- 생성된 `README.txt` 내용 확인

---

## 15. 이 문서를 언제 갱신해야 하나
아래가 바뀌면 이 문서도 갱신하세요.
- 브랜치 전략 변경
- GitHub Project 운영 방식 변경
- 제출 방식 변경
- 스크립트 사용법 변경
- 보안/권한 정책 변경

문서는 **마지막에 몰아서 쓰는 것이 아니라**, 운영 규칙이 바뀌는 즉시 같이 업데이트하는 것이 좋습니다.
