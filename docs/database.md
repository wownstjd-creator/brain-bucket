# DB 구조와 자동 업데이트

서버가 시작할 때 JPA가 DB를 사용하기 전에 Flyway가 업데이트를 수행합니다. 기존 최신 MySQL DB는 V1로 기준선을 등록하고 V2를 실행합니다. 빈 DB는 V1부터 생성합니다. 수동으로 20261010_integrate.sql을 적용한 DB도 V2의 컬럼 존재 검사로 지원합니다.

| 테이블 | 추가 컬럼 | 목적 |
| --- | --- | --- |
| posts | display_from DATE NULL | 공개 시작일, 기존 NULL은 시작 제한 없음 |
| posts | version BIGINT NOT NULL DEFAULT 0 | 수정 충돌 확인 |
| posts | display_until DATE NULL (없을 때만) | 공개 종료일 |
| approval_logs | snapshot LONGTEXT NULL | 처리 당시 게시 내용 |
| approval_logs | poster_key/evidence_key/original_key VARCHAR(512) NULL | 처리 당시 파일 저장 키 |
| approval_logs | poster_type/evidence_type/original_type VARCHAR(255) NULL | 처리 당시 MIME |
| flyway_schema_history | Flyway 관리 컬럼 | 적용 버전·성공 여부 |

기존 데이터·파일을 삭제하지 않습니다. 과거 이력의 본문이나 이미지는 소급 복원하지 않습니다. V2는 CREATE ROUTINE 권한 없이 컬럼 검사와 ALTER TABLE만 수행합니다. Flyway 이력 테이블 생성 권한도 필요합니다.

## 오류 복구

권한이나 DB 접속 오류는 원인을 해결하고 재시작합니다. MySQL에서 V2 수행 도중 실패했다면 일부 컬럼만 이미 추가됐을 수 있습니다. 컬럼 존재 검사가 있으므로 재적용해도 중복 추가하지 않습니다. 다만 Flyway의 실패 이력이 남았다면 DBA가 DB를 백업하고 실패 상태를 확인한 뒤 Flyway repair로 실패 기록을 정리해야 재실행할 수 있습니다. 성공 기록을 지우거나 clean을 실행하지 마세요. clean은 설정으로 비활성화했습니다.

새 DB 전체 생성은 MySQL용 V1 SQL, 기존 DB 컬럼 추가는 Java V2로 관리합니다. 이후 변경은 새 버전으로 추가합니다. 적용된 마이그레이션은 수정하지 않습니다.

## 배포본별 이력 호환

DB 관련 파일은 모두 루트 backend에 있습니다. `frontend/backend`의 SQL·Java 마이그레이션을 그대로 덮어쓰면 이미 적용된 V1 체크섬/V2 설명이 달라질 수 있으므로 다음 구조로 통합했습니다.

- `backend/src/main/resources/db/current/V1__initial_schema.sql`: 최근 main의 V1 원본. 빈 DB의 기본 스키마입니다.
- `backend/src/main/resources/db/legacy/V1__initial_schema.sql`: 이전 패치의 V1 원본. 해당 체크섬이 성공 이력에 있는 DB의 검증용입니다.
- `backend/src/main/java/db/migration/V2__complete_existing_schema.java`: 최근 main의 V2 원본.
- `backend/src/main/java/db/migration/V2__integrate_post_management.java`: 이전 패치의 V2 원본(체크섬 20261010).
- `backend/src/main/java/com/brainbucket/config/MigrationCompatibilityConfig.java`: 시작 시 성공 이력을 읽고 SQL 위치 및 Java V2 하나만 등록합니다.

SQL V1은 기록된 체크섬으로, Java V2는 기록된 설명으로 선택합니다. V1 기준선만 있는 DB도 지원합니다. 새 DB는 current V1/V2를 사용합니다. 성공 이력을 UPDATE하거나 repair하지 않으며, 알려지지 않은 체크섬은 정상적으로 검증 실패합니다. 기존 테이블과 사용자 데이터의 변경 범위는 두 원본 V2의 누락 컬럼 추가와 같습니다. 이번 수정으로 새 테이블/컬럼은 추가하지 않았습니다.

후속 스키마 변경 시 이미 배포한 두 원본을 편집하지 말고 V3 이후로 추가하세요. SQL 변경은 선택되는 두 SQL 위치에 같은 새 버전 파일을 넣고, Java 변경은 호환 설정의 javaMigrations 목록에 같은 새 마이그레이션을 등록해야 합니다. 어느 이력에서 시작하든 후속 버전이 똑같이 실행되도록 양쪽 경로를 테스트하세요.

확인 쿼리(읽기 전용):

```sql
SELECT version, description, type, checksum, success
FROM flyway_schema_history ORDER BY installed_rank;
```

## 이번 전달본 검증

2026-10-26 H2에서는 두 V1/V2의 성공 이력 조합 8가지에 대해 검증 성공·재실행 0건·이력 설명 보존을 확인했습니다. 알 수 없는 체크섬의 검증 실패와 빈 DB의 current 선택도 검사했으며, Spring Boot 실제 초기화가 호환 선택을 사용하는지도 두 이력으로 검사했습니다. H2 검사는 MySQL DDL 실행 검증이 아닙니다.

MySQL 전용 테스트는 폐기 가능한 MySQL에 한해서 BB_MIGRATION_TEST_URL·BB_MIGRATION_TEST_USER·BB_MIGRATION_TEST_PASSWORD를 설정한 뒤 `mvnw test`로 실행합니다. URL은 DB명이 없는 서버 주소(예: jdbc:mysql://127.0.0.1:13316/)입니다. 테스트는 임의 이름의 bb_migration_* DB를 만들고 삭제하므로 생성/삭제 권한이 필요합니다. 일반 빌드에서는 이 환경변수가 없으면 MySQL 전용 테스트를 생략합니다. 이번 환경에서는 MySQL 서버가 소켓 제한으로 시작하지 못해 실제 MySQL 검증은 수행하지 못했습니다.

## 2026-10-11 기능 마무리

작성자 삭제·승인 취소·첨부 제거, 목록 페이지/조건 조회, 비밀번호 변경은 기존 컬럼만 사용합니다. 추가 수동 SQL과 V3는 없습니다. 테이블 관계는 final-spec.md, Docker DB/첨부 백업·복구는 deployment.md에 정리했습니다.
