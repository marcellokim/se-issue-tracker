# SE 2026-1 Term Project - Issue Tracker

소프트웨어공학 2026-1 텀 프로젝트 **이슈 관리 시스템(ITS)** 저장소입니다.  
이 저장소는 과제 구현뿐 아니라 **GitHub 협업 흐름, 제출 준비, 문서화, 자동화 운영**까지 한 곳에서 관리하도록 구성되어 있습니다.

## 1. 과제 제출 정보
- **과제명**: SE 2026 Spring Term Project - 이슈관리 시스템 개발
- **구현 언어**: Java
- **최종 제출 마감**: 2026-06-02 21:00
- **발표 일정**: 2026-06-03 / 2026-06-08 수업 시간
- **GitHub 저장소**: <https://github.com/marcellokim/se-2026-1-term-project-issue-tracker>
- **GitHub Project**: <https://github.com/users/marcellokim/projects/1>

## 2. 팀 정보
| 구분 | 내용 |
| --- | --- |
| 저장소 관리자 | marcellokim |
| 팀 구성 | 3인 팀 |
| 추가 팀원 정보 | 최종 제출용 README.txt, 보고서 표지, 발표 자료에 맞춰 반영 |
| 작업 방식 | Issue → Branch → PR → Review → Merge |

## 3. 과제 핵심 요구사항 요약
`SE_Term_Project_2026-1.pdf` 기준 핵심 요구사항:
- 계정 추가: `admin`, `PL`, `dev`, `tester`
- 이슈 등록 / 브라우즈 / 검색 / 상세 조회 / 코멘트 / 상태 변경
- 통계 분석: 일/월별 이슈 발생 추이
- 해결 이력 기반 assignee 자동 추천
- 영속 저장소 필요(File System 또는 DB 가능)
- **MVC 아키텍처 적용 필수**
- **두 개 이상의 UI Toolkit 기반 UI 제공 필수**
- **JUnit 테스트 및 GitHub 기반 협업 기록 필수**

## 4. 현재 저장소 상태
- [x] Gradle + Java 21 기본 골격
- [x] GitHub Issue / PR 템플릿
- [x] GitHub Actions CI / PR 자동 라벨링 / Dependabot
- [x] bootstrap / git hook / label sync / submission packaging 스크립트
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
git clone https://github.com/marcellokim/se-2026-1-term-project-issue-tracker.git
cd se-2026-1-term-project-issue-tracker
./scripts/bootstrap.sh
```

### 기본 검증
```bash
./gradlew check
```

### 작업 브랜치 생성 예시
```bash
./scripts/start-task.sh 12 issue-search-ui
```

## 6. 자동화 구성
이 저장소는 코딩 전/초기 단계 생산성을 높이기 위해 아래 자동화를 포함합니다.

- **Issue Form → GitHub Project 자동 등록**
- **PR 자동 라벨링**: 변경 파일 경로 기준 라벨 부여
- **Gradle CI**: `./gradlew clean check`
- **보안 자동화**: Dependabot 보안 업데이트, secret scanning, push protection, private vulnerability reporting, GitHub code scanning 기본 설정
- **Dependabot**: Gradle / GitHub Actions 주간 업데이트 제안
- **Git hooks**: pre-commit / pre-push 검증
- **Commit template**: Lore commit protocol 템플릿 자동 적용
- **Label sync / GitHub bootstrap 스크립트**
- **Submission zip 스크립트**: 제출 형식 zip + `README.txt` 자동 생성

## 7. 주요 문서
- [사용 설명서 / 팀 운영 가이드](docs/team-setup-manual.md)
- [자동화 옵션 조사 및 적용 현황](docs/automation-playbook.md)
- [보안 정책](SECURITY.md)
- [기본 가정 문서](docs/assumptions.md)
- [Q&A / 의사결정 기록](docs/qna.md)

## 8. 저장소 구조
```text
.
├─ .github/                 # Issue/PR template, Actions, CODEOWNERS
├─ .githooks/               # pre-commit / pre-push hooks
├─ config/github/           # label / milestone 정의
├─ docs/                    # 과제 문서, 사용 가이드, UML, 스크린샷
├─ gradle/                  # Gradle wrapper
├─ scripts/                 # bootstrap, branch, label, submission automation
├─ src/main/java/           # 애플리케이션 소스
├─ src/test/java/           # JUnit 테스트
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
- `feature/<issue>-<slug>`: 개인 작업 브랜치
- 모든 작업은 **Issue 생성 → Branch 생성 → PR 리뷰 → Merge** 순서로 진행
- 구현 내용은 문서, 스크린샷, 테스트와 함께 남깁니다.

## 11. 제출 직전 반드시 확인할 것
- README / README.txt / GitHub Project 링크 최신화
- 팀원 이름, 학번, 팀 번호 최신 반영
- UML / 유스케이스 / SSD / Operation Contract / 테스트 문서 보강
- 발표 자료 / 데모 시나리오 / 동영상 정리
- AI 사용 흔적이 남지 않도록 문서와 코드 이해 여부를 팀 전체가 직접 검토
