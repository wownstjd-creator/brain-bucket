# BrainBucket / 한눈에 통합본

2026-10-10 업로드된 brain-bucket1010.zip의 MySQL 백엔드를 기준으로, 친구의 프론트 및 추가 기능을 통합한 소스입니다. 기존 회원 인증·API·multipart·파일 저장을 유지합니다. 친구의 H2 DB와 관리자 계정은 포함하지 않습니다.

## 적용 순서

1. 기존 프로젝트·MySQL DB·backend/uploads를 백업합니다. ZIP은 별도 폴더에 풉니다.
2. 기존 MySQL DB에는 schema.sql을 다시 실행하지 않습니다. 기존 display_until 변경이 적용된 상태에서 아래 변경 SQL만 실행합니다.
3. 기존 데이터를 사용하려면 기존 backend/uploads를 새 backend/uploads로 복사합니다. DB만 복사하면 이미지 파일이 없습니다.
4. 새 폴더의 backend에서 DB 환경변수를 지정하고 실행합니다.
5. ZIP에 빌드된 화면이 backend/src/main/resources/static에 포함되어 있어 http://localhost:8080/#/board로 바로 접속할 수 있습니다. 프론트 소스를 수정하려면 frontend에서 npm ci 후 npm run dev로 http://localhost:5175에 접속합니다.
6. 기존 MySQL의 계정으로 로그인합니다. 회원가입한 계정을 ADMIN으로 바꾸려면 기존 API 문서의 SQL을 참고합니다.
7. 기능 검증 후 변경 파일을 기존 저장소의 통합 브랜치에 반영합니다. 기존 backend/.../user/UserController.java는 진단용 사용자 수 API라 이번 소스에서 삭제했습니다. 기존 폴더에 덮어쓰는 경우 그 파일도 삭제합니다.

저장소 최상위에서 MySQL에 접속 후 mysql> 창에서:

```sql
USE brain_bucket;
SOURCE database/migrations/20261010_integrate.sql;
SHOW COLUMNS FROM posts LIKE 'display_from';
SHOW COLUMNS FROM posts LIKE 'version';
```

새 빈 DB는 database/schema.sql을 사용합니다. 변경 SQL은 컬럼 존재 여부를 확인하여 기존 데이터를 삭제하지 않습니다. ALTER/CREATE ROUTINE 권한이 필요하며 MySQL DDL은 전체 원자적 롤백이 아닙니다. 실행 오류가 있으면 원인을 해결한 뒤 재시도합니다. 임시 프로시저가 남았다면 DROP PROCEDURE IF EXISTS bb_add_column 후 실행할 수 있습니다.

PowerShell 1 — backend (Java 21 이상):

```powershell
cd backend
$env:DB_USERNAME = "본인_DB_계정"
$env:DB_PASSWORD = "본인_DB_비밀번호"
.\mvnw.cmd spring-boot:run
```

PowerShell 2 — frontend (Node 22.18 이상 또는 24):

```powershell
cd frontend
npm ci
npm run dev
```

프론트 개발 프록시는 8080 백엔드로 /api를 전달합니다. 두 창을 유지합니다. localhost와 127.0.0.1을 섞지 마세요. 기존 8180 H2 시연 서버와 이번 8080 MySQL 서버는 별개입니다.

## 통합 기능

- 작성·이미지 교체·승인/반려·관리자 조회·공개 조회·아카이브 유지.
- displayFrom 게시 시작일, 미래 예약 전단의 공개 목록·상세·이미지 차단.
- 관리자 내림·복구·행사/마감/게시 기간 변경, 사유·변경 전후 이력. 복구는 publishedAt 유지.
- 증빙·입력 원본의 multipart 저장 및 작성자·관리자 전용 조회. 새 승인 요청·승인은 증빙 필수.
- 승인/반려 당시 본문·이미지 저장 키·MIME 보존. 참조된 옛 이미지는 교체 후에도 유지.
- APPROVED 본문 수정 시 DRAFT로 전환되어 공개 중단, 재승인 필요. HIDDEN·PENDING 수정 차단.
- 선택적 version 검사, 이미지 변경 시 version 증가, 게시물 행 잠금으로 상태 전이 직렬화.
- 서버 검색·분류·종료일 연도 필터와 페이지 처리.
- 계정 스크랩: 브라우저 저장에서 MySQL bookmarks로 전환. 기존 브라우저 스크랩 자동 이관은 없음.
- 조회수: 공개 상세/전단 열기에서 세션당 1회 집계. 비로그인 가능, CSRF 필요. 로그인 계정·사람별 정확한 순 방문자 집계는 아님.
- 주요 생성/수정/이력 시각 한국 시간 통일, http/https 신청 링크 검사.
- 친구 화면 디자인·사진 보정·브라우저 OCR 유지. API 변환은 frontend/src/capture/api.ts에 통합.

## 테스트 및 남은 확인

프론트 TypeScript/Vite 빌드와 13개 테스트, Java 컴파일 및 3개 테스트 클래스의 테스트가 통과했습니다. H2의 MySQL 호환 모드에서 실제 JPA·파일 저장·HTTP 세션/CSRF를 검증했습니다. 실제 MySQL 변경 SQL 실행, 기존 데이터 마이그레이션, 브라우저 화면·카메라·OCR 실기기 및 Docker 배포는 별도 확인이 필요합니다. 기존 승인 이력에는 과거 본문/이미지를 소급 복원하지 않습니다.

자세한 API: docs/api.md. 확인 항목: docs/verification.md. 제출용 피그마·명세서는 별도 작업입니다.

## 빌드·배포 준비

PowerShell에서 .\scripts\build.ps1 실행 시 프론트 산출물을 Spring 정적 자원에 넣고 테스트·JAR 빌드를 수행합니다. JAVA_HOME·Node·MySQL 실행 환경을 준비해야 합니다. 최종 JAR은 backend/target에 생성되며 DB 환경변수를 넣고 java -jar로 실행합니다. 이 경우 화면과 API를 같은 8080 주소에서 제공합니다.

Dockerfile과 compose.yaml도 포함했습니다. .env.example을 .env로 복사하고 비밀번호를 설정한 뒤 docker compose up --build -d로 별도 새 MySQL 환경을 구성할 수 있습니다. DB·업로드는 볼륨으로 유지합니다. 기존 로컬 DB를 자동 가져오지는 않습니다. 외부 운영은 HTTPS 프록시를 구성한 뒤 COOKIE_SECURE=true로 설정하세요. Docker 실행은 이 환경에서 검증하지 않았습니다.

커밋에 포함: 소스·문서·변경 SQL·배포 설정. 제외: DB 비밀번호, .env, uploads, target, node_modules. 관리자 권한·직인의 실제 운영 절차는 학교와 별도 협의합니다.
