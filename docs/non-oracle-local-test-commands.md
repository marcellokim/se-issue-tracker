# Oracle 없는 팀원용 로컬 테스트 명령어

이 문서는 Oracle DB 계정이나 로컬 Oracle 설치가 없는 팀원이 PR #91 변경을 확인할 때 사용할 수 있는 PowerShell/VSC 터미널 명령어를 정리한 것이다.

SonarCloud 분석 명령은 제외한다.

## 1. 현재 브랜치와 작업 상태 확인

```powershell
git status --short --branch
git log -3 --oneline
```

## 2. main 코드 컴파일 확인

```powershell
.\gradlew.bat compileJava --console=plain
```

## 3. test 코드 컴파일 확인

```powershell
.\gradlew.bat compileTestJava --console=plain
```

## 4. #18 핵심 리소스/서비스 스모크 테스트

Oracle DB 없이 실행 가능하다.

```powershell
.\gradlew.bat test --rerun-tasks `
  --tests com.github.marcellokim.issuetracker.persistence.PersistenceResourceSmokeTest `
  --tests com.github.marcellokim.issuetracker.service.AuthenticationServiceTest `
  --tests com.github.marcellokim.issuetracker.technical.PasswordHasherTest `
  --tests com.github.marcellokim.issuetracker.MainSmokeTest `
  --console=plain
```

## 5. 도메인 테스트

Oracle DB 없이 실행 가능하다.

```powershell
.\gradlew.bat test --rerun-tasks `
  --tests com.github.marcellokim.issuetracker.domain.IssueAssignmentRoleTest `
  --tests com.github.marcellokim.issuetracker.domain.IssueChangeTest `
  --tests com.github.marcellokim.issuetracker.domain.IssueCommentTest `
  --tests com.github.marcellokim.issuetracker.domain.IssueCreationTest `
  --tests com.github.marcellokim.issuetracker.domain.IssueDependencyTest `
  --tests com.github.marcellokim.issuetracker.domain.IssueFixResolveTest `
  --tests com.github.marcellokim.issuetracker.domain.IssueHistoryTest `
  --tests com.github.marcellokim.issuetracker.domain.DomainValueObjectsTest `
  --tests com.github.marcellokim.issuetracker.domain.UserTest `
  --console=plain
```

## 6. 서비스/정책 테스트

Oracle DB 없이 실행 가능하다.

```powershell
.\gradlew.bat test --rerun-tasks `
  --tests com.github.marcellokim.issuetracker.service.AssignmentRecommendationServiceTest `
  --tests com.github.marcellokim.issuetracker.service.AuthenticationServiceTest `
  --tests com.github.marcellokim.issuetracker.service.PermissionPolicyTest `
  --console=plain
```

## 7. 기술 유틸리티 테스트

Oracle DB 없이 실행 가능하다.

```powershell
.\gradlew.bat test --rerun-tasks `
  --tests com.github.marcellokim.issuetracker.technical.PasswordHasherTest `
  --tests com.github.marcellokim.issuetracker.technical.SessionStoreTest `
  --console=plain
```

## 8. 커버리지 보강 테스트 묶음

PR #91의 Sonar coverage 보강을 위해 추가/수정된 테스트 묶음이다. Oracle DB 없이 실행 가능하다.

```powershell
.\gradlew.bat test --rerun-tasks `
  --tests com.github.marcellokim.issuetracker.service.PermissionPolicyTest `
  --tests com.github.marcellokim.issuetracker.domain.IssueHistoryTest `
  --tests com.github.marcellokim.issuetracker.domain.IssueDependencyTest `
  --tests com.github.marcellokim.issuetracker.domain.IssueCommentTest `
  --tests com.github.marcellokim.issuetracker.domain.DomainValueObjectsTest `
  --tests com.github.marcellokim.issuetracker.service.AuthenticationServiceTest `
  --tests com.github.marcellokim.issuetracker.technical.PasswordHasherTest `
  --tests com.github.marcellokim.issuetracker.technical.SessionStoreTest `
  --console=plain
```

## 9. Oracle 통합 테스트가 스킵되는지 확인

Oracle 환경변수를 비운 뒤 실행한다. Oracle이 없는 환경에서는 `oracleIntegrationTest`가 실행되지 않고 SKIPPED 또는 실행 대상 없음 상태로 끝나야 한다.

```powershell
Remove-Item Env:ITS_TEST_DB_URL -ErrorAction SilentlyContinue
Remove-Item Env:ITS_TEST_DB_USER -ErrorAction SilentlyContinue
Remove-Item Env:ITS_TEST_DB_PASSWORD -ErrorAction SilentlyContinue
Remove-Item Env:ITS_DB_INTEGRATION_TESTS -ErrorAction SilentlyContinue

.\gradlew.bat oracleIntegrationTest --rerun-tasks --console=plain
```

## 10. 공백/라인 끝 문제 확인

```powershell
git diff --check origin/dev
```

## 주의

다음 명령은 Windows 로컬에서 `OpenPrScriptFailureTest`의 Git Bash fixture 문제로 실패할 수 있다.

```powershell
.\gradlew.bat test
.\gradlew.bat check
.\gradlew.bat test jacocoTestReport
```

이 실패는 PR #91의 DB/repository 코드나 Oracle 없는 팀원용 선별 테스트 실패를 의미하지 않는다. Oracle 없는 팀원은 위의 선별 테스트 명령어를 기준으로 검증하면 된다.
