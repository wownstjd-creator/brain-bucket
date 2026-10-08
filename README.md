# BrainBucket — 브레인버킷

QR로 접속해 로그인 없이 교내 대자보를 열람하는 모바일 중심 게시·승인·아카이빙 서비스입니다. 회원은 전단지를 작성하고, 담당자는 승인하거나 사유를 남겨 반려합니다.

## 지금 프론트에서 연결할 수 있는 기능

**백엔드 전체 완료를 기다리지 않고 아래 기능부터 연결할 수 있습니다.**

| 화면·흐름 | 현재 사용 가능한 기능 |
|---|---|
| 회원가입·로그인 | 회원가입, 세션 로그인, 내 정보, 로그아웃, CSRF |
| 내 전단지 | 임시 생성, 목록, 상세, 전체 수정 |
| 이미지 등록 | JPG·PNG 업로드, 작성자 이미지 조회 |
| 승인 요청 | 필수 입력 검사 후 DRAFT/REJECTED → PENDING |
| 반려 안내 | 작성자 승인·반려 이력, 반려 사유 조회 |
| 반려 후 수정 | REJECTED → 수정 시 DRAFT → 재요청 시 PENDING |
| 현재 전단 | 비로그인 공개 목록·상세·이미지 |
| 지난 전단 | 한국 날짜 기준 종료된 승인 전단 목록·상세·이미지 |
| 담당자 처리 | ADMIN 승인·반려, 처리 이력 저장 |

위 기능은 2026-10-08 백엔드 개발자 로컬 테스트에서 확인했습니다. 프론트 화면 연결 및 공동 환경 테스트는 별도입니다.

요청 주소·필드·JSON 예제는 **[API 연결 문서](docs/api.md)**를 기준으로 사용합니다.

## 아직 구현되지 않은 기능

- 담당자 승인 대기 목록·상세·이미지 조회: 승인/반려 버튼 API만 구현되어 있습니다.
- 승인 증빙 업로드·조회 및 승인 당시 내용·이미지 보존.
- 기존 전단지 이미지 교체.
- 승인된 전단 수정·재승인: 현재 APPROVED 게시물 수정은 409로 차단합니다.
- 검색·분류·연도 필터, 스크랩, 조회수 증가.
- 프론트 origin에 맞춘 CORS 설정, 배포.
- 날짜시간 전체의 한국 시간 통일: 승인·아카이브 판단은 한국 시간이며, 기존 생성·수정 시각은 서버 기본 시간대입니다.

에브리타임·인스타그램 연동과 직인 자동 인식은 추후 범위입니다.

## 기술 및 폴더

- Java 21 / Spring Boot 4.1.1 / Spring Data JPA / Spring Security.
- MySQL 8.0 / Maven Wrapper / 세션 쿠키 인증 / BCrypt.
- 2인 개발: 백엔드·DB / 디자인·프론트엔드.

| 경로 | 역할 |
|---|---|
| backend/ | Spring Boot 코드 및 설정 |
| frontend/ | 프론트 작업 영역 |
| database/schema.sql | 기존 테이블 생성 SQL |
| docs/api.md | 프론트 연결 기준 및 API 예제 |
| database/migrations/20261008_add_display_until.sql | 기존 DB 종료일 컬럼 추가 |

## 로컬 실행 — 처음 받는 사람

각 개발자는 자신의 MySQL과 이미지 저장 폴더를 사용합니다. 코드를 Pull해도 다른 개발자의 회원·게시물·이미지가 자동 복사되지는 않습니다. `localhost`는 접속 중인 컴퓨터 자신의 주소입니다.

### 1. DB 초기화

Java 21과 MySQL 8.0을 설치하고 저장소를 clone합니다. 저장소 최상위에서 **PowerShell**을 열어 MySQL에 접속합니다.

```powershell
& "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe" --user=root --password
```

설치 경로·계정은 본인 환경에 맞게 변경합니다. 비밀번호 입력 후 **`mysql>` 창에서** 실행합니다.

```sql
CREATE DATABASE IF NOT EXISTS brain_bucket
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;
USE brain_bucket;
SOURCE database/schema.sql;
SHOW TABLES;
SHOW COLUMNS FROM posts LIKE 'display_until';
```

`schema.sql`은 빈 DB 초기화용입니다. 기존 DB에 반복 실행하지 않습니다. 최신 schema.sql에는 posts.display_until이 포함되어 있습니다. 구조를 소스와 대조했으며 이번 문서 작업에서 새 MySQL에 실제 import 테스트를 하지는 않았습니다.

### 2. 기존 DB 업데이트

이미 DB를 만들어 사용 중인 사람은 schema.sql을 다시 실행하지 않고, 저장소 최상위에서 연 MySQL 창에서 아래를 실행합니다.

```sql
USE brain_bucket;
SOURCE database/migrations/20261008_add_display_until.sql;
EXIT;
```

변경 SQL은 컬럼이 있으면 건너뛰므로 반복 실행해도 같은 컬럼을 추가하지 않습니다. 기존 데이터를 삭제하지 않으며 새 컬럼의 기존 값은 null입니다. **개발자 본인 DB는 이미 컬럼을 추가했다면 추가 작업이 필요 없습니다.** 새 DB를 최신 schema.sql로 만들었다면 변경 SQL도 필요 없습니다.

Hibernate 설정은 `ddl-auto=none`이므로 코드를 Pull하는 것만으로 DB 구조가 갱신되지 않습니다.

### 3. 백엔드 실행

저장소 최상위의 **PowerShell (`PS ...>`)**에서 실행합니다.

```powershell
cd backend
$env:DB_USERNAME = "본인의_MySQL_계정"
$env:DB_PASSWORD = "본인의_MySQL_비밀번호"
.\mvnw.cmd spring-boot:run
```

첫 실행은 의존성 다운로드가 필요합니다. 새 PowerShell에서는 환경변수를 다시 설정합니다. IntelliJ에서는 backend를 열고 실행 구성에 같은 환경변수를 설정합니다.

- `Started ...` 및 8080 시작 메시지 확인 후 요청합니다.
- 백엔드 주소: `http://localhost:8080`.
- 실행 터미널 종료: Ctrl+C.
- 서버 재시작 후 기존 세션을 기대하지 말고 다시 로그인합니다.
- 업로드 폴더: 실행 작업 디렉터리 기준 `uploads/`.

### 4. 비로그인 공개 API 확인

PowerShell에서 실행합니다.

```powershell
Invoke-RestMethod -Uri "http://localhost:8080/api/posts/public?archived=false&page=0&size=20"
```

새 DB에는 승인 전단이 없으므로 빈 배열이 정상입니다. 브라우저용 홈페이지는 프론트가 제공하며 백엔드 루트 주소에 화면이 없을 수 있습니다.

## 프론트 연결 순서

1. 개발 프록시 또는 CORS 설정을 맞춥니다. 현재 직접 다른 포트에서 요청하는 CORS 설정은 미구현입니다. 프론트 실행 origin(예: http://localhost:5173)을 백엔드 개발자에게 공유합니다.
2. CSRF → 회원가입/로그인 → 로그인 후 CSRF 재발급 → 내 전단 작성·이미지 연결.
3. 승인 요청·상태 표시·반려 이력 연결. PENDING과 APPROVED는 현재 수정 불가입니다.
4. 공개 목록·상세·이미지·지난 전단 연결. 공개 GET은 로그인 없이 사용할 수 있습니다.
5. 담당자 화면은 검토 조회 API가 추가될 때 연결합니다. 로컬 승인 테스트는 API 문서의 계정 권한 준비 절차를 사용합니다.

쿠키를 위해 fetch에 `credentials: "include"`를 지정합니다. API 주소·JSON 필드명은 문서와 일치시킵니다. 모든 오류가 JSON 본문을 갖는 것은 아닙니다.

## 함께 작업하는 방법

- 백엔드는 실행 확인한 기능 단위로 main에 푸시합니다. 프론트는 자기 브랜치에서 현재 받은 버전으로 작업합니다.
- 작은 커밋마다 업데이트할 필요는 없습니다. 다음 기능이 연결 가능해졌다는 공유를 받으면 최신 main을 프론트 브랜치에 반영합니다.
- 프론트 브랜치에서 단순 Pull만 하면 원격 main이 자동 합쳐지는 것은 아닙니다. GitHub Desktop에서 Fetch 후 **Branch → Merge into current branch → origin/main** 등으로 최신 main을 반영합니다.
- 프론트 PR이 main에 합쳐지면 백엔드 개발자도 작업 전에 main을 Pull합니다.
- 기존 API 주소·필드·응답 구조 변경은 적용 전 협의합니다. 새 기능은 문서와 구현/검증 상태를 함께 공유합니다.
- DB 변경은 SQL도 함께 공유합니다. docs/api.md는 백엔드가 우선 관리합니다.

## 커밋에 포함·제외할 것

포함: Java 코드, README.md, docs/api.md, DB 스키마/변경 SQL.

제외: uploads의 테스트 이미지, target, 실제 DB 비밀번호·비밀 설정. 기존 .gitignore가 각 경로를 제외하는지 확인합니다. 이미 추적 중인 파일은 .gitignore 추가만으로 추적 해제되지 않습니다.

개발 확인용 `/api/dev/users/count`는 배포 전 제거하거나 제한합니다.
