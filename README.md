# BrainBucket / 한눈에

친구의 React 프론트와 기존 Spring Boot·MySQL 백엔드를 통합한 대자보 서비스입니다. Java 21 이상, MySQL 8이 필요합니다. 프론트 개발·재빌드에는 Node 22.18 이상이 필요합니다.

## 기존 프로젝트에서 업데이트

1. 기존 저장소에서 변경을 커밋한 뒤 최신 코드를 pull합니다.
2. 서버를 종료하고 저장소 최상위에서 아래 명령으로 다시 실행합니다.

```powershell
.\scripts\run.ps1
```

기존 DB와 uploads는 그대로 사용합니다. 서버 시작 시 Flyway가 DB 변경을 자동 적용합니다. 별도의 SOURCE 명령은 필요하지 않습니다. 이미 수동 SQL을 실행한 DB도 컬럼을 중복 추가하지 않습니다. Git pull 자체가 실행 중인 서버를 갱신하지는 않으므로 재시작해야 합니다.

## 처음 실행하는 사람

Java 21 이상과 MySQL 8을 설치하고 MySQL 서비스를 실행합니다. 사용할 빈 DB는 처음 한 번 생성합니다.

```sql
CREATE DATABASE IF NOT EXISTS brain_bucket CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

저장소 최상위의 .env.example을 .env로 복사하고 DB_USERNAME·DB_PASSWORD를 자신의 MySQL 계정으로 수정합니다. `.env`는 커밋하지 않습니다. 자동 변경을 수행하는 DB 계정에는 해당 DB의 CREATE·ALTER·INDEX·REFERENCES와 데이터 읽기/쓰기 권한이 필요합니다.

```powershell
Copy-Item .env.example .env
# .env를 편집한 다음 실행
.\scripts\run.ps1
```

브라우저에서 http://localhost:8080/#/board 에 접속합니다. 빌드된 화면은 backend/src/main/resources/static에 포함되므로 Node 없이 화면을 사용할 수 있습니다. 회원가입으로 일반 계정을 생성합니다. 친구의 H2 계정이나 다른 사람의 MySQL 계정은 자동 복사되지 않습니다. 관리자 지정 절차는 docs/api.md를 참고하세요.

이미 JAVA_HOME·DB 환경변수를 설정했다면 backend에서 `.\mvnw.cmd spring-boot:run`으로 직접 실행할 수 있습니다. `.env`를 읽는 것은 scripts/run.ps1이며 직접 Maven을 실행할 때는 환경변수를 지정해야 합니다.

## DB 자동 업데이트

- V1: 빈 DB에 users·posts·post_files·approval_logs·bookmarks를 생성합니다.
- 기존 테이블이 있는 DB는 V1 기준선으로 등록하고 V2부터 적용합니다. 이 도입은 이 프로젝트의 기존 MySQL 스키마를 전제로 합니다. 임의의 다른 DB를 접속 대상으로 지정하지 마세요.
- V2: posts의 display_until·display_from·version, approval_logs의 snapshot·첨부 저장 키·MIME 컬럼 중 없는 컬럼만 추가합니다. 기존 행을 삭제하거나 덮어쓰지 않습니다.
- 적용 이력은 flyway_schema_history에 남으며 재시작 시 이미 적용된 버전은 건너뜁니다.
- 자동 실행 파일은 backend/src/main/resources/db/migration과 backend/src/main/java/db/migration에 있습니다. 이후 구조 변경은 새 V3, V4 버전으로 추가하며 이미 배포한 버전은 수정하지 않습니다.
- database/schema.sql과 database/migrations/20261010_integrate.sql은 수동 참고 자료입니다. 정상 실행 과정에서 따로 실행하지 않습니다. 기존 DB에 schema.sql을 다시 실행하지 마세요.
- MySQL DDL은 전체 트랜잭션 롤백이 되지 않습니다. 최초 업데이트 전 DB를 백업하세요. 실패 시 로그의 원인을 해결하고 docs/database.md의 절차를 따릅니다.

## 프론트 개발과 공유

backend를 실행한 상태에서 별도 터미널의 frontend 폴더에서 `npm ci`, `npm run dev`를 실행하면 http://localhost:5175 에서 개발할 수 있습니다. /api는 8080으로 프록시됩니다. localhost와 127.0.0.1을 섞지 마세요.

화면 소스를 수정한 뒤 저장소 최상위에서 다음을 실행합니다.

```powershell
.\scripts\build.ps1
```

프론트 빌드 결과를 backend/src/main/resources/static에 복사하고 백엔드 테스트·JAR 빌드를 실행합니다. frontend/src 변경과 static 변경을 함께 커밋해야 다른 사람이 pull 후 서버 실행만으로 최신 화면을 볼 수 있습니다. target·node_modules·frontend/dist는 제외합니다.

## 기능

- 일반 계정의 대자보 작성·임시 저장·승인 요청·요청 취소·삭제, 관리자 승인·반려.
- 작성/반려 글의 첨부 제거, 비밀번호 변경 후 현재 기기 로그아웃.
- 게시 시작/종료일, 관리자 내림·복구·일정 변경 및 처리 이력.
- 증빙·원본 multipart 저장, 작성자·관리자 전용 조회와 승인 당시 본문·첨부 이력 보존.
- 승인된 글 수정 시 공개 중단, 재승인 후 다시 공개.
- 계정별 MySQL 스크랩, 세션별 조회수, 서버 검색·분류·연도 필터와 20개 단위 페이지 조회.
- 친구 프론트의 사진 보정과 브라우저 OCR. 신청 URL·장소·행사/마감은 선택 항목이며 URL 입력 시 http/https 주소를 사용합니다.

UserController.java에는 회원가입 API가 있으므로 유지합니다. 진단용 사용자 수 조회 API는 제거했습니다.

## 데이터와 검증

DB에는 게시 내용·계정·스크랩·파일 저장 키가 저장되고 이미지 실물은 backend/uploads(또는 UPLOAD_DIR)에 저장됩니다. DB와 uploads를 함께 유지·백업해야 합니다. Git pull은 이미지와 계정 데이터를 전달하지 않습니다.

사용자 로컬에서 핵심 작성/승인/내림/복구/수정 흐름과 스크랩·조회수·검색을 확인했습니다. 후속 기능의 자동 테스트 및 검증 한계는 docs/verification.md를 참고하세요. 외부 서버·HTTPS·실제 백업 복구와 실기기 OCR은 아직 별도 검증이 필요합니다.

[API 명세](docs/api.md) · [DB](docs/database.md) · [현재 기능 명세](docs/final-spec.md) · [배포/백업/복구](docs/deployment.md) · [검증](docs/verification.md).

## Docker

.env에 DB_PASSWORD·MYSQL_ROOT_PASSWORD를 설정한 뒤 `docker compose up --build -d`로 별도 MySQL·앱 환경을 구성합니다. 빈 DB의 테이블도 앱의 Flyway가 생성하며 DB·uploads는 볼륨으로 유지됩니다. 로컬 DB를 자동 가져오지는 않습니다. Docker 실행과 외부 HTTPS 배포는 별도 검증이 필요합니다.

## 커밋

소스·문서·DB 자동 변경 파일·빌드된 static·스크립트·배포 설정을 포함합니다. .env·비밀번호·uploads·target·node_modules는 제외합니다. 제출용 피그마·명세서는 별도 작업입니다.

실제 도메인 HTTPS 배포: 루트 .env에 DOMAIN을 설정하고 Linux 서버에서 `bash scripts/deploy.sh`를 실행합니다. 준비 사항과 백업/복구 명령은 docs/deployment.md에 있습니다.
