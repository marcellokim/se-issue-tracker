# ITS 구현 정책 통합 기록

구현 정책 내용은 `docs/assumptions.md`의 팀 가정사항과 정책 정리 안으로 통합하였다.

통합된 범위는 다음과 같다.

- UI, Controller, Service, Domain, Repository/JDBC 계층 책임
- Domain 객체의 `create(...)`, `fromPersistence(...)` 생성/복원 정책
- User, Project, Issue, Assignment, Comment, Dependency, DB/Seed, UI Toolkit 관련 구현 정책

구현 정책을 확인할 때는 `docs/assumptions.md`를 기준으로 보면 된다.
