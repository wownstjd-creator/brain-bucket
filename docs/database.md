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

## 검증

2026-10-10 Java 21·MySQL 8.0.46에서 다음 세 경우의 Flyway 적용·재실행·Spring Boot/JPA 시작을 확인했습니다.

- 빈 DB: 전체 테이블 생성 후 V2 적용.
- 구버전 DB: 컬럼을 제거한 기존 구조에서 필요한 컬럼 추가, 기존 계정·게시물 보존.
- 수동 업데이트한 DB: 이미 있는 컬럼을 건너뛰고 성공 이력 등록.

각 경우 재실행 시 적용 개수 0, 최종 버전 2를 확인했습니다. 실제 MySQL 테스트 1개(내부 세 시나리오)와 H2/HTTP 기존 테스트 3개, 총4개가 모두 통과했습니다. 회원가입 HTTP 201과 생성 계정 로그인도 검증했습니다. JAR 패키징이 통과했습니다.

테스트는 폐기 가능한 MySQL에 한해서 BB_MIGRATION_TEST_URL·BB_MIGRATION_TEST_USER·BB_MIGRATION_TEST_PASSWORD를 설정한 뒤 `mvnw test`로 실행합니다. 테스트 계정은 테스트용 DB 생성/삭제 권한이 필요하며 임의 이름의 bb_migration_* DB를 만들고 삭제합니다. 일반 빌드에서는 환경변수가 없으면 MySQL 전용 테스트만 건너뛰고 H2/HTTP 테스트를 실행합니다.

Windows PowerShell 실행 스크립트는 코드를 검토했지만 이 Linux 환경에서 직접 실행하지 않았습니다. 사용자의 기존 MySQL DB 자체를 이번 도구 환경에서 변경하거나 GitHub에 푸시하지 않았습니다.

## 2026-10-11 기능 마무리

작성자 삭제·승인 취소·첨부 제거, 목록 페이지/조건 조회, 비밀번호 변경은 기존 컬럼만 사용합니다. 추가 수동 SQL과 V3는 없습니다. 테이블 관계는 final-spec.md, Docker DB/첨부 백업·복구는 deployment.md에 정리했습니다.
