# Issue Tracking System UML Artifacts

이 폴더는 Issue Tracking System 설계에 사용하는 UML PlantUML 파일을 모은다. PNG/SVG는 리뷰나 최종 PDF 삽입이 필요할 때 PlantUML로 생성한다.

## 폴더 구조

```text
docs/uml/
  README.md
  _shared/
    its-uml-style.puml
  ucd/
    ucd-issue-tracking-system.puml
  domain/
    domain-model-issue-tracking-system.puml
  architecture/
    logical-architecture-its.puml
    logical-architecture-its-detail.puml
  ssd/
    ssd-style.puml
    ssd-01-register-issue.puml
    ...
    ssd-27-change-priority.puml
    README.md
    all-ssd-puml.md
  ssd-candidate-catalog.md
```

## 파일명 규칙

- UCD: `ucd-issue-tracking-system.puml`
- Domain Model: `domain-model-issue-tracking-system.puml`
- Logical Architecture Overview: `logical-architecture-its.puml`
- Logical Architecture Detail: `logical-architecture-its-detail.puml`
- SSD: `ssd-XX-kebab-case-name.puml`
- 공통 스타일: `_shared/its-uml-style.puml`
- SSD 전용 스타일 확장: `ssd/ssd-style.puml`

## 제출/개발 구분

- 제출 대표 SSD는 `ssd-01-register-issue.puml`, `ssd-05-assign-issue.puml`이다.
- `ssd-02`-`ssd-27`은 Operation Contract, Sequence Diagram, DCD, 구현 정책 확인을 위한 개발 reference이다.
- `ssd-13-purge-deleted-issues.puml`은 actor-goal SSD가 아니라 UC9 deleted issue retention policy reference이다.
- `ssd-22-verify-permission.puml`은 UC14 include 흐름을 설명하는 reference이며, protected SSD 본문에서는 permission check를 note/precondition으로 둔다.

## 렌더링

repo root에서 실행한다.

```sh
plantuml -tpng docs/uml/ucd/*.puml docs/uml/domain/*.puml docs/uml/architecture/*.puml docs/uml/ssd/*.puml
```

`docs/uml/ssd/ssd-style.puml`은 include 전용 파일이므로 `no image` 경고가 날 수 있다. 실제 SSD 27개 PNG가 생성되면 정상이다.

렌더 산출물은 같은 basename으로 생성되며, 필요할 때 다시 만들 수 있다.
