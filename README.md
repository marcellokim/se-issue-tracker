# SE 2026-1 Term Project Issue Tracker

## 프로젝트 개요
소프트웨어공학 2026-1 텀 프로젝트용 이슈 트래커 저장소입니다. GitHub Issue, Pull Request, Project, CI를 중심으로 팀 작업 내역과 진행 상황을 관리합니다.

## 팀원
| 이름 | 역할 |
| --- | --- |
| marcellokim | 저장소 소유자 / 역할 확정 예정 |
| TBD | 역할 확정 예정 |
| TBD | 역할 확정 예정 |

## 기술 스택
- Java 21
- Gradle
- JavaFX + Swing
- GitHub Issues / Pull Requests / Actions / Projects

## 브랜치 전략
- `main`: 항상 제출 가능한 안정 버전
- `dev`: 통합 브랜치
- `feature/...`: 개인 작업 브랜치

작업 규칙:
1. `main` 직접 push 금지
2. `dev`는 가능한 한 PR로만 병합
3. 모든 작업은 `feature/...`에서 시작

브랜치 예시:
- `feature/auth-model`
- `feature/javafx-issue-list`
- `feature/swing-admin-panel`
- `feature/recommendation-service`

## 실행 방법
```bash
git clone https://github.com/marcellokim/se-2026-1-term-project-issue-tracker.git
cd se-2026-1-term-project-issue-tracker
./gradlew test
```

개발 시작 예시:
```bash
git checkout dev
git pull
git checkout -b feature/your-task-name
```

## 폴더 구조
```text
.
├─ README.md
├─ .gitignore
├─ build.gradle
├─ settings.gradle
├─ gradlew
├─ gradlew.bat
├─ src/
│  ├─ main/java/
│  └─ test/java/
├─ docs/
│  ├─ assumptions.md
│  ├─ use-cases/
│  ├─ uml/
│  ├─ screenshots/
│  └─ qna.md
└─ .github/
   ├─ ISSUE_TEMPLATE/
   ├─ PULL_REQUEST_TEMPLATE.md
   └─ workflows/
      └─ gradle.yml
```

## GitHub Project 링크
- 예정 이름: `SE Term Project 2026-1`
- 현재 상태: GitHub Project 생성 전
