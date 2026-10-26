# 다른 PC에서 실행하기

## Windows에서 처음 실행

1. JDK 21 이상과 MySQL 8을 설치하고 MySQL 서비스를 실행합니다. PowerShell의 `java -version`과 `JAVA_HOME`이 같은 JDK를 가리키는지 확인합니다.
2. MySQL에서 빈 DB를 한 번 만듭니다.

```sql
CREATE DATABASE IF NOT EXISTS brain_bucket CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

3. 저장소 최상위에서 `.env.example`을 `.env`로 복사합니다. 기존 `.env`가 있다면 덮어쓰지 않습니다. `DB_USERNAME`, `DB_PASSWORD`를 이 PC의 MySQL 계정에 맞게 바꿉니다. 서버 주소나 포트가 다르면 `DATABASE_URL`도 바꿉니다.
4. 저장소 최상위에서 실행합니다. 첫 실행은 Maven과 라이브러리 다운로드를 위해 인터넷 연결이 필요합니다.

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\run.ps1
```

5. `http://localhost:8080/#/board`에 접속합니다. 화면 빌드가 포함되어 있으므로 단순 실행에는 Node 설치가 필요하지 않습니다.

DB 계정에는 해당 DB의 CREATE·ALTER·INDEX·REFERENCES 및 데이터 읽기/쓰기 권한이 필요합니다. 서버가 Flyway로 빈 DB의 테이블을 만들거나 이 프로젝트의 기존 DB에 누락된 컬럼만 추가합니다. 계정·게시물·이미지는 Git으로 전달되지 않으며, 처음에는 회원가입으로 계정을 만듭니다. 관리자 지정은 api.md를 따릅니다.

## 기존 데이터를 옮길 때

DB 백업과 uploads 폴더를 한 쌍으로 옮깁니다. 기존 DB에 `database/schema.sql`을 다시 실행하지 않습니다. 업데이트 전 백업하고 서버를 종료한 뒤 코드를 갱신하여 다시 실행합니다. DB 변경 이력은 `flyway_schema_history`에 기록됩니다. 임의의 다른 서비스 DB를 연결하지 않습니다.

## Docker로 실행

Docker가 있는 환경에서는 `.env`의 `DB_PASSWORD`·`MYSQL_ROOT_PASSWORD`를 설정한 뒤 `docker compose up --build -d`를 실행합니다. 호스트 PC에 Java·Node·MySQL을 별도로 설치할 필요가 없습니다. Docker 이미지 다운로드와 빌드에는 인터넷이 필요합니다. 기존 로컬 MySQL 데이터가 Docker로 자동 복사되지는 않습니다.

## 문제 확인

- Java 관련 오류: JDK 21 이상, JAVA_HOME, 새 터미널의 java -version을 확인합니다.
- DB 연결 거부: MySQL 서비스, 주소·포트, DB 생성 여부를 확인합니다.
- Access denied: 해당 PC의 DB 계정·비밀번호·권한을 확인합니다.
- Flyway 실패: database.md의 복구 절차를 확인합니다. 데이터가 있는 DB를 삭제하거나 Flyway clean을 실행하지 않습니다.
- 이미지가 없음: DB뿐 아니라 uploads도 복원했는지 확인합니다.

## 검증 명령

일반 검증은 backend에서 `mvnw.cmd verify`입니다. H2/HTTP 테스트가 실행되고 MySQL 전용 테스트는 아래 환경변수가 없으면 건너뜁니다.

MySQL 마이그레이션 테스트는 반드시 별도의 폐기 가능한 테스트 서버에서 실행합니다. 테스트는 임의 이름의 `bb_migration_*` DB를 생성·삭제하며, 테스트 계정에는 DB 생성·삭제 권한이 필요합니다.

```powershell
$env:BB_MIGRATION_TEST_URL='jdbc:mysql://127.0.0.1:13317/'
$env:BB_MIGRATION_TEST_USER='테스트계정'
$env:BB_MIGRATION_TEST_PASSWORD='테스트서버비밀번호'
.\mvnw.cmd -Dtest=MySqlMigrationTests test
```

URL은 DB 이름을 포함하지 않는 서버 주소입니다. 빈 DB·구버전 DB·수동 업데이트 DB의 자동 변경, 데이터 보존, 실제 Spring Boot/JPA 시작, 재실행 시 변경 0건을 검사합니다.

2026-10-13 전달본의 검증 결과는 루트의 검증 결과.md를 참고하세요. 기본 실행은 루트의 2-start.cmd이며, 이 문서의 Maven 실행은 소스 개발용입니다.
