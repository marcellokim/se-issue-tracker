# 11. 코멘트 추가/수정/삭제

## 진입 조건

- Actor: 프로젝트 멤버
- 선행 화면: 이슈 상세
- 전달 값: `issueId`

## 화면 동작 API

### 1. 코멘트 추가

- **호출 시점**: 코멘트 입력 후 제출
- **메서드**: `IssueController.addComment(issueId, content)`
- **파라미터**:

| 이름 | 타입 | 필수 | 설명 |
|---|---|---|---|
| issueId | long | Y | 이슈 ID |
| content | String | Y | 코멘트 내용 |

- **반환**: `CommentResult`
- **후속**: 코멘트 영역 새로고침 (`viewComments`)

### 2. 코멘트 수정

- **호출 시점**: 수정 버튼 클릭 (`canUpdateComment=true`인 코멘트만)
- **메서드**: `IssueController.updateComment(issueId, commentId, content)`
- **파라미터**: issueId (long), commentId (long), content (String)
- **반환**: `CommentResult`
- **권한**: 작성자 본인만

### 3. 코멘트 삭제

- **호출 시점**: 삭제 버튼 클릭 (`canDeleteComment=true`인 코멘트만)
- **메서드**: `IssueController.deleteComment(issueId, commentId)`
- **파라미터**: issueId (long), commentId (long)
- **반환**: void
- **권한**: 작성자 본인, GENERAL purpose만 삭제 가능. STATUS_CHANGE 코멘트는 삭제 불가.

## 화면 전이

| 동작 | 다음 화면 |
|---|---|
| 추가/수정/삭제 완료 | 이슈 상세 코멘트 영역 새로고침 |
