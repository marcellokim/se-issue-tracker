# SSD PUML 목록

이 폴더는 전체 SSD 후보의 PlantUML 파일을 관리한다. PNG/SVG는 팀 리뷰와 최종 PDF 삽입이 필요할 때 PlantUML로 생성한다.

제출 문서에는 `ssd-01-register-issue`, `ssd-05-assign-issue`, `ssd-06-mark-fixed`를 대표 SSD로 사용한다. 나머지는 개발 기준 정합성 확인과 Operation Contract/시퀀스 다이어그램 작성 보조용 reference이며, 최종 PDF 본문에는 전부 넣지 않는다.

## 제출용 선별 기준

- 제출 대표 SSD: `ssd-01-register-issue`, `ssd-05-assign-issue`, `ssd-06-mark-fixed`
- 개발 reference SSD: `02`-`04`, `07`-`27`
- 내부 정책 reference: `ssd-13-purge-deleted-issues`
- UC14 권한 검사는 UC 명세와 Operation Contract에서 다루며, SSD에서는 필요한 경우 system 내부 처리나 짧은 note로만 표현한다. `ssd-22-verify-permission`은 개발 참고용으로만 둔다.
- PNG/SVG는 필요할 때 PlantUML로 다시 생성한다.

## 렌더 검증

repo root에서 다음 명령으로 렌더링한다.

```sh
plantuml -tpng docs/artifact/ssd/*.puml
```

`ssd-style.puml`은 include 전용 파일이므로 `no image` 경고가 날 수 있다. SSD 27개 이미지가 생성되면 정상이다.

Operation Contract는 SSD 확정 이후 별도 문서에서 작성한다.

| No | SSD | PUML |
| --- | --- | --- |
| 01 | UC1 이슈 등록 | [PUML](ssd-01-register-issue.puml) |
| 02 | UC2 코멘트 추가 | [PUML](ssd-02-add-comment.puml) |
| 03 | UC3 이슈 검색/브라우즈 | [PUML](ssd-03-search-issues.puml) |
| 04 | UC4 이슈 상세 조회 | [PUML](ssd-04-view-issue-details.puml) |
| 05 | UC5 Assign / Update Issue Assignment | [PUML](ssd-05-assign-issue.puml) |
| 06 | UC6 Fixed 처리 | [PUML](ssd-06-mark-fixed.puml) |
| 07 | UC6 Resolve 처리 | [PUML](ssd-07-resolve-issue.puml) |
| 08 | UC6 Fix 거절 | [PUML](ssd-08-reject-fix.puml) |
| 09 | UC6 Close 처리 | [PUML](ssd-09-close-issue.puml) |
| 10 | UC6 Reopen 처리 | [PUML](ssd-10-reopen-issue.puml) |
| 11 | UC8 Assignment Candidate 추천 Reference | [PUML](ssd-11-recommend-assignees.puml) |
| 12 | UC9 Delete Closed/New Issue | [PUML](ssd-12-delete-issue.puml) |
| 13 | UC9 삭제 이슈 보관 정책 Reference | [PUML](ssd-13-purge-deleted-issues.puml) |
| 14 | UC10 이슈 통계 조회 | [PUML](ssd-14-view-issue-statistics.puml) |
| 15 | UC11 로그인 | [PUML](ssd-15-log-in.puml) |
| 16 | UC12 계정 생성 | [PUML](ssd-16-create-account.puml) |
| 17 | UC12 계정 관리 - 계정 역할 변경 | [PUML](ssd-17-update-account.puml) |
| 18 | UC12 계정 비활성화 | [PUML](ssd-18-deactivate-account.puml) |
| 19 | UC13 프로젝트 생성 | [PUML](ssd-19-create-project.puml) |
| 20 | UC13 프로젝트 참여자 추가 | [PUML](ssd-20-add-project-member.puml) |
| 21 | UC13 프로젝트 참여자 제거 | [PUML](ssd-21-remove-project-member.puml) |
| 22 | UC14 권한 검사 | [PUML](ssd-22-verify-permission.puml) |
| 23 | UC15 이슈 수정 | [PUML](ssd-23-edit-issue.puml) |
| 24 | UC7 의존성 추가 | [PUML](ssd-24-add-dependency.puml) |
| 25 | UC7 의존성 제거 | [PUML](ssd-25-remove-dependency.puml) |
| 26 | UC9 Restore Closed/New Issue | [PUML](ssd-26-restore-deleted-issue.puml) |
| 27 | UC16 우선순위 변경 | [PUML](ssd-27-change-priority.puml) |
