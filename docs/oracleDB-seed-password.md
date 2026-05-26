## Demo Account Password Policy

본 프로젝트의 초기 seed 데이터는 데모 및 로컬 테스트를 위해 미리 정의된 사용자 계정을 포함한다.
다만 사용자 비밀번호는 DB에 평문으로 저장하지 않고, `USER_CREDENTIALS` 테이블의 `password_salt`, `password_hash` 컬럼에 PBKDF2 기반 해시 결과로만 저장한다.

즉, `seed-oracle.sql`에는 `DemoLocalAdmin!` 같은 원래 비밀번호 문자열이 직접 들어가지 않는다.
대신 사전에 정한 데모용 초기 비밀번호를 `PasswordHasher`와 동일한 방식으로 해시한 뒤, 그 결과인 salt/hash 값만 seed SQL에 반영한다.

따라서 DB를 조회해도 원래 비밀번호를 역으로 알아낼 수 없다.
로그인 시에는 사용자가 입력한 비밀번호와 DB에 저장된 salt를 이용해 다시 해시를 계산하고, 그 결과가 DB의 `password_hash`와 일치하는지만 검증한다.

초기 데모 계정의 비밀번호 규칙은 다음과 같다.

| Login ID | Initial Password |
|---|---|
| `admin` | `DemoLocalAdmin!` |
| `pl1` | `DemoLocalPl1!` |
| `pl2` | `DemoLocalPl2!` |
| `dev1` ~ `dev10` | `DemoLocalDev1!` ~ `DemoLocalDev10!` |
| `tester1` ~ `tester5` | `DemoLocalTester1!` ~ `DemoLocalTester5!` |

이 비밀번호들은 운영용 비밀번호가 아니라, 로컬 개발과 데모 실행을 위한 고정 초기값이다.
실제 DB seed에는 위 평문 비밀번호가 저장되지 않고, 각 비밀번호에 대응하는 `password_salt`와 `password_hash`만 저장된다.

예를 들어 `admin` 계정의 경우 다음과 같은 흐름으로 처리된다.

```text
평문 초기 비밀번호: DemoLocalAdmin!
        ↓
PasswordHasher.hash(...)
        ↓
salt:hash credential 생성
        ↓
seed-oracle.sql에는 password_salt, password_hash만 저장
        ↓
로그인 시 입력 비밀번호를 같은 salt로 다시 hash
        ↓
DB의 password_hash와 비교하여 인증 성공/실패 판단