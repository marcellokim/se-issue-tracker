# 저장소 리뷰 언어 지침

이 저장소의 자동 리뷰, 요약, 제안, 체크 실패 분석, 채팅 응답은 가능한 한 한국어로 작성한다.

예외:
- 코드 식별자
- 파일 경로
- 명령어
- stack trace
- 제품이 고정으로 제공하는 UI 라벨

리뷰는 실제 결함, 요구사항 누락, 테스트 누락, 권한/상태 전이 오류를 우선한다. 단순 표현이나 스타일 지적은 동작 위험이 있을 때만 남긴다.

## 설계 문서 확인 기준

코드 수정이나 리뷰 전에 현재 설계를 먼저 확인한다. 최소한 `docs/uml/README.md`에서 UML 산출물 구성을 보고, 작업 범위에 맞는 UCD, domain model, logical architecture, SSD, SD, DCD, Operation Contract 문서를 확인한다.

구현 구조 판단은 다이어그램만으로 끝내지 않고 `docs/uml/dcd/dcd_implementation_details.md`, `docs/uml/dcd/dcd_current_implementation_details.md`, 실제 controller -> service -> domain/repository 호출 경로를 함께 대조한다. 설계 문서와 코드가 다르면 현재 구현 기준, 과제 요구사항, PR 목적을 분리해서 설명한다.
