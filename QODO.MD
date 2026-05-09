# Qodo Review Instructions

Qodo는 이 저장소의 Pull Request 리뷰, 요약, 워크스루, 발견 사항, 권장 조치, CI 실패 분석, 자동 업데이트 코멘트를 가능한 한 한국어로 작성한다.

영어를 유지해도 되는 예외는 코드 식별자, 파일 경로, 제품 고정 UI 문구, 번역할 수 없는 서비스 템플릿뿐이다.

출력 언어 기준:

- 사람이 읽는 문장은 한국어로 작성한다.
- 제목, 요약, inline finding, summary finding, `/checks` 응답, 자동 재리뷰 응답도 한국어로 작성한다.
- 심각도 라벨, 제품 고정 섹션명, 코드 블록, stack trace, 명령어, 파일 경로는 원문을 유지할 수 있다.
- 영어 원문을 유지해야 할 때도 설명 문장은 한국어로 덧붙인다.

리뷰 우선순위:

1. 실제 결함, 요구사항 누락, 테스트 누락, 권한/상태 전이 오류를 우선한다.
2. 단순 스타일 지적은 동작 위험이 있을 때만 남긴다.
3. 과제 PDF 기준, 회의 결정, GitHub 이슈/마일스톤 정책과 충돌하는 변경을 우선적으로 지적한다.
4. 회의 결정 기준은 다음과 같다.
   - User당 직군은 하나만 가진다.
   - Title/Description은 reporter만 수정 가능하며 assigned 전까지만 가능하다.
   - Priority는 PL만 변경 가능하고 assigned 상태와 무관하게 변경 가능하다.
   - 잘못된 fixed 처리는 tester가 dev에게 되돌릴 수 있다.
   - Reopen은 PL만 가능하며 assigned부터 다시 시작한다.
   - Deleted 상태와 FIFO 삭제 정책을 사용한다.
   - Issue dependency를 지원한다.
