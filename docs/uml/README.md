# Issue Tracking System UML Artifacts

이 폴더는 Issue Tracking System 설계 문서에 사용하는 UML/PlantUML 자료를 모아 둔다. `.puml`, `.md`, `.txt`가 원본이고, PNG/SVG 같은 렌더링 산출물은 보고서 삽입이나 리뷰가 필요할 때 다시 생성한다.

## 폴더 구조

```text
docs/uml/
  README.md
  ssd-candidate-catalog.md

  _shared/
    its-uml-style.puml

  ucd/
    ucd-issue-tracking-system.puml

  domain/
    domain-model-issue-tracking-system.puml

  dcd/
    its_dcd.puml

  architecture/
    its_layer_architecture.puml
    its_layer_architecture.md
    logical-architecture-its.puml
    logical-architecture-its-detail.puml
    layer_current_implementation_details.md
    layer_implementation_details.md

  ssd/
    ssd-style.puml
    ssd-01-register-issue.puml
    ...
    ssd-27-change-priority.puml
    README.md
    all-ssd-puml.md

  sd/
    SD_README.md
    _shared/
      its-uml-style.puml
    sd_puml/
      sd-01-register-issue-detailed.puml
      ...
    sd_grasp/
      sd-01-register-issue-grasp.txt
      ...

  operation-contracts/
    required_operation_contracts.md
```

## 문서별 역할

| 위치 | 역할 |
|---|---|
| `ucd/` | Use Case Diagram |
| `domain/` | Larman style Domain Model |
| `dcd/its_dcd.puml` | Domain Model을 설계 클래스 수준으로 확장한 DCD |
| `architecture/` | 레이어 아키텍처, logical architecture, 구현 계층 설명 |
| `ssd/` | System Sequence Diagram 원본과 SSD 묶음 문서 |
| `sd/` | Sequence Diagram과 GRASP 책임 설명 |
| `operation-contracts/` | SSD/UC에 대응되는 Operation Contract |
| `ssd-candidate-catalog.md` | 보고서 제출용 SSD 후보와 선정 이유 |

## 파일명 규칙

- UCD: `ucd-issue-tracking-system.puml`
- Domain Model: `domain-model-issue-tracking-system.puml`
- DCD: `its_dcd.puml`
- Layer Architecture View: `its_layer_architecture.puml`
- Logical Architecture: `logical-architecture-its.puml`, `logical-architecture-its-detail.puml`
- SSD: `ssd-XX-kebab-case-name.puml`
- SD: `sd-XX-kebab-case-name-detailed.puml`
- GRASP 설명: `sd-XX-kebab-case-name-grasp.txt`
- 공통 스타일: `_shared/its-uml-style.puml`
- SSD 전용 스타일 확장: `ssd/ssd-style.puml`

## 제출/개발 구분

- 보고서 대표 SSD는 `ssd-01-register-issue.puml`, `ssd-05-assign-issue.puml`, `ssd-06-mark-fixed.puml`을 기준으로 한다.
- `ssd-02`부터 `ssd-27`까지는 Operation Contract, Sequence Diagram, DCD, 구현 정책 확인을 위한 reference로 유지한다.
- `ssd-13-purge-deleted-issues.puml`은 actor-goal SSD라기보다는 UC9 deleted issue retention policy reference에 가깝다.
- `ssd-22-verify-permission.puml`은 UC14 include 흐름을 설명하는 reference이며, 보호된 SSD 본문에서는 permission check를 note 또는 precondition 수준으로 둔다.
- `architecture/` 문서는 구현 구조 설명용이고, `dcd/its_dcd.puml`은 도메인 설계 클래스 중심으로 읽는다.

## 현재 구현 정책 반영 기준

UML 문서는 현재 로컬 코드의 정책 방향을 반영하되, 코드 호출 순서를 그대로 옮기는 문서는 아니다. 각 문서는 다음 수준으로 읽는다.

- `domain/`은 문제 영역의 개념과 관계를 보여준다. Java class의 모든 field/method를 넣지 않는다.
- `dcd/its_dcd.puml`은 Domain Model을 확장한 설계 클래스 다이어그램이다. 핵심 domain class, association, multiplicity, navigability, 주요 operation만 표현한다.
- `architecture/its_layer_architecture.puml`은 Controller, Service, Repository port, JDBC adapter, technical adapter, bootstrap의 계층 구조를 보여준다.
- `ssd/`는 black-box 관점이다. 내부 Controller, Service, Repository, Domain object lifeline을 넣지 않는다.
- `sd/`는 Larman style detailed sequence diagram이다. 현재 구현 방향은 반영하지만, Service/JDBC 호출 순서를 그대로 복사하지 않고 GRASP 책임 중심으로 표현한다.
- `operation-contracts/`는 SSD/UC의 system operation에 대응되는 사후조건을 정리한다. Postcondition은 완료된 결과 기준으로 작성한다.


## 렌더링

repo root에서 실행한다.

```sh
plantuml -tpng docs/uml/ucd/*.puml docs/uml/domain/*.puml docs/uml/dcd/*.puml docs/uml/architecture/*.puml docs/uml/ssd/ssd-*.puml docs/uml/sd/sd_puml/*.puml
```

SVG가 필요하면 `-tsvg`로 바꿔 실행한다.

```sh
plantuml -tsvg docs/uml/ucd/*.puml docs/uml/domain/*.puml docs/uml/dcd/*.puml docs/uml/architecture/*.puml docs/uml/ssd/ssd-*.puml docs/uml/sd/sd_puml/*.puml
```

PlantUML CLI가 없고 jar 파일을 사용할 때는 다음처럼 실행할 수 있다.

```sh
java -jar .tools/plantuml.jar -tpng docs/uml/ucd/*.puml docs/uml/domain/*.puml docs/uml/dcd/*.puml docs/uml/architecture/*.puml docs/uml/ssd/ssd-*.puml docs/uml/sd/sd_puml/*.puml
```

## 렌더링 주의사항

- `docs/uml/_shared/its-uml-style.puml`, `docs/uml/ssd/ssd-style.puml`, `docs/uml/sd/_shared/its-uml-style.puml`은 include 전용 파일이다.
- `ssd/all-ssd-puml.md`, `sd/SD_README.md`, `sd/sd_grasp/*.txt`, `operation-contracts/*.md`는 PlantUML 렌더 대상이 아니다.
- 렌더 산출물은 같은 basename의 PNG/SVG로 생성된다. 산출물은 필요할 때 다시 만들 수 있으므로 원본 문서와 `.puml` 파일을 우선 관리한다.
