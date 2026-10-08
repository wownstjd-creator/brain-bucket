# Brain Bucket API 연결 안내

기준: 2026-10-08 최신 post·config 소스와 제공된 user 소스·실행 설정. 승인·반려·공개 조회·아카이브까지 개발자 로컬에서 동작 확인했다. 프론트 연결 테스트는 별도이며 이 문서 작성 시 최신 저장소를 직접 빌드하지 않았다. 예시 ID·시각·크기는 설명용이다. 실행 및 DB 준비는 [README](../README.md)를 참고한다.

## 실행 환경

- Java 21, Spring Boot 4.1.1, Maven, MySQL.
- 기본 API 주소: `http://localhost:8080` (설정 파일에 포트 재정의 없음).
- DB: `jdbc:mysql://localhost:3306/brain_bucket`.
- DB 계정은 `DB_USERNAME`, `DB_PASSWORD` 환경변수로 설정.
- 이미지: 서버 실행 작업 디렉터리 기준 `uploads`에 저장. Git으로 이미지 파일이 자동 공유되지는 않는다.
- multipart 파일 한도 10MB, 요청 전체 한도 12MB.
- Hibernate ddl-auto: `none`.
- 새 DB는 최신 database/schema.sql 사용(display_until 포함). 기존 DB는 database/migrations/20261008_add_display_until.sql 실행(이미 있으면 건너뜀). SQL 구조는 소스와 대조했으며 이번 작업에서 새 MySQL import는 실행하지 않았다. ddl-auto=none이라 자동 생성되지 않는다.

PowerShell에서 backend 디렉터리로 이동한 뒤 실행:

```powershell
$env:DB_USERNAME = "본인_DB_계정"
$env:DB_PASSWORD = "본인_DB_비밀번호"
# Maven Wrapper가 저장소에 있으면
.\mvnw.cmd spring-boot:run
# Wrapper가 없고 Maven을 설치했으면 대신
# mvn spring-boot:run
```

IntelliJ 실행 시에도 Run Configuration의 환경변수에 두 값을 지정한다.

## 인증과 CSRF

세션 쿠키 기반 인증이다. GET CSRF → 로그인 → GET CSRF 재발급 순서로 연결한다. POST·PUT·로그아웃에는 반환받은 `headerName`과 `token`을 헤더로 보낸다. 회원가입과 로그인도 CSRF 검증 대상이다. 로그인 후 세션 ID와 CSRF 토큰이 변경된다.

브라우저 fetch는 `credentials: "include"`를 사용한다. 현재 제공된 설정에는 CORS 연결 설정이 없다. 다른 포트의 프론트에서 직접 요청하려면 프론트 origin을 확정하고 CORS를 추가하거나 개발 프록시를 구성해야 한다. `localhost`와 `127.0.0.1`은 섞어 쓰지 않는다.

| 메서드 | 주소 | 접근 | 성공 |
|---|---|---|---|
| GET | `/api/auth/csrf` | 비로그인 가능 | 200 |
| POST | `/api/auth/signup` | 비로그인 가능, CSRF 필요 | 201 |
| POST | `/api/auth/login` | 비로그인 가능, CSRF 필요 | 200 |
| GET | `/api/auth/me` | 로그인 | 200 |
| POST | `/api/auth/logout` | 세션 로그아웃, CSRF 필요 | 204, 본문 없음 |
| POST | `/api/posts` | 로그인, CSRF 필요 | 201 |
| GET | `/api/posts/mine?page=0&size=20` | 로그인 | 200 |
| GET | `/api/posts/mine/{postId}` | 작성자 | 200 |
| PUT | `/api/posts/mine/{postId}` | 작성자, CSRF 필요 | 200 |
| POST | `/api/posts/mine/{postId}/poster` | 작성자, CSRF 필요 | 201 |
| GET | `/api/posts/mine/{postId}/poster` | 작성자 | 200, 이미지 |
| POST | `/api/posts/mine/{postId}/approval-request` | 작성자, CSRF | 200 |
| GET | `/api/posts/mine/{postId}/approval-logs` | 작성자 | 200 |
| POST | `/api/admin/posts/{postId}/approve` | ADMIN, CSRF | 200 |
| POST | `/api/admin/posts/{postId}/reject` | ADMIN, CSRF | 200 |
| GET | `/api/posts/public?archived=false&page=0&size=20` | 비로그인 가능 | 200 |
| GET | `/api/posts/public/{postId}` | 비로그인 가능 | 200 |
| GET | `/api/posts/public/{postId}/poster` | 비로그인 가능 | 200, 이미지 |

`/api/admin/**`는 ADMIN 권한 제한이 적용된다. 승인·반려 API는 구현되었고 검토 목록·상세·이미지 API는 아직 없다.

## 회원 API

### CSRF

GET `/api/auth/csrf`, 응답 예시:
```json
{"headerName":"X-CSRF-TOKEN","token":"발급된_토큰"}
```
헤더 이름은 고정 문자열보다 응답의 `headerName`을 사용한다.

### 회원가입

POST `/api/auth/signup`, Content-Type `application/json`:

```json
{
  "email": "test@example.com",
  "password": "TestPass123!",
  "displayName": "테스트"
}
```
이메일: 필수·이메일 형식·최대 255자. 비밀번호: 필수·8~72자이며 UTF-8 72바이트 이하. 닉네임: 필수·최대 50자. 이메일은 trim 후 소문자로 저장하고 닉네임은 trim한다. 중복 이메일은 409. 회원가입은 자동 로그인이 아니다.

201 응답 예시:

```json
{
  "id": 1,
  "email": "test@example.com",
  "displayName": "테스트",
  "role": "USER"
}
```
### 로그인 및 내 정보

POST `/api/auth/login` 요청:

```json
{
  "email": "test@example.com",
  "password": "TestPass123!"
}
```
로그인 입력: 이메일 필수·이메일 형식·최대 255자, 비밀번호 필수·최대 72자 및 UTF-8 72바이트 이하. 이메일은 trim 후 소문자로 인증한다. 잘못된 자격 증명은 401.

로그인 성공 및 GET `/api/auth/me` 응답은 같은 구조:

```json
{
  "id": 1,
  "email": "test@example.com",
  "displayName": "테스트",
  "role": "USER"
}
```
## 임시 대자보

### 생성

POST `/api/posts`, Content-Type `application/json`:

```json
{
  "title": "동아리 모집",
  "organizer": "COMA",
  "category": "CLUB",
  "description": "신입 부원을 모집합니다.",
  "location": "N110",
  "applicationUrl": "https://example.com/apply",
  "eventAt": "2026-10-20T18:00:00",
  "deadlineAt": "2026-10-18T23:59:00",
  "displayUntil": "2026-11-30",
  "showViewCount": true
}
```
201 응답:

```json
{
  "id": 2,
  "title": "동아리 모집",
  "status": "DRAFT"
}
```
| 필드 | 타입 | 입력 제한 |
|---|---|---|
| title | string 또는 null | 최대 150자 |
| organizer | string 또는 null | 최대 100자 |
| category | string 또는 null | 최대 30자, 현재 enum 검증 없음 |
| description | string 또는 null | 최대 10,000자 |
| location | string 또는 null | 최대 150자 |
| applicationUrl | string 또는 null | 최대 2,048자, 현재 URL 형식 검증 없음 |
| eventAt | 날짜시간 또는 null | `2026-10-20T18:00:00` |
| deadlineAt | 날짜시간 또는 null | 같은 형식 |
| displayUntil | 날짜 또는 null | `2026-11-30`, 임시 저장은 생략 가능 |
| showViewCount | boolean 또는 null | 생략/null이면 true |

임시 저장은 미완성 입력을 허용하며 생성 상태는 DRAFT다. 날짜 타입은 LocalDateTime으로 시간대/오프셋이 없다. 현재 생성·수정 시각은 서버 기본 시간대의 LocalDateTime.now()를 사용한다. 승인·아카이브의 날짜 판단과 승인 처리 시각은 한국 시간이며 생성·수정 시각까지 통일하는 것은 별도 작업이다.

### 내 목록

GET `/api/posts/mine?page=0&size=20`:

```json
[
  {
    "id": 2,
    "title": "동아리 모집",
    "organizer": "COMA",
    "category": "CLUB",
    "status": "DRAFT",
    "deadlineAt": "2026-10-18T23:59:00",
  "displayUntil": "2026-11-30",
    "createdAt": "2026-10-08T13:00:00"
  }
]
```
page는 0부터 시작, size 기본 20·허용 1~50. 음수 page나 범위 밖 size는 400. createdAt 내림차순 후 id 내림차순. 삭제되지 않은 내 게시물만 반환한다. 응답은 배열이며 totalElements·totalPages·hasNext는 없다. size보다 적게 오면 끝이며, size개가 오면 다음 페이지를 요청해 확인할 수 있다. 빈 결과는 `[]`다.

### 내 상세

GET `/api/posts/mine/{postId}` 응답:

```json
{
  "id": 2,
  "title": "동아리 모집",
  "organizer": "COMA",
  "category": "CLUB",
  "description": "신입 부원을 모집합니다.",
  "location": "N110",
  "applicationUrl": "https://example.com/apply",
  "eventAt": "2026-10-20T18:00:00",
  "deadlineAt": "2026-10-18T23:59:00",
  "displayUntil": "2026-11-30",
  "showViewCount": true,
  "status": "DRAFT",
  "viewCount": 0,
  "publishedAt": null,
  "createdAt": "2026-10-08T13:00:00",
  "updatedAt": "2026-10-08T13:00:00"
}
```
내 상세 응답에는 displayUntil이 포함된다. imageUrl·반려 사유는 없다. 반려 사유는 approval-logs API로 조회한다. 작성자 이미지 주소는 게시물 ID로 별도 구성한다. 내 게시물이 아니거나 삭제/미존재면 404.

### 수정

PUT `/api/posts/mine/{postId}`: 생성과 동일한 요청 JSON, 200 응답은 생성 응답과 동일 구조. DRAFT·REJECTED만 가능하며 수정하면 DRAFT가 된다. 다른 상태는 409. PATCH가 아니므로 생략 필드는 null로 덮어쓰고 showViewCount는 생략/null이면 true가 된다. 기존 값을 유지하려면 전체 입력 값을 보낸다.

## 이미지

POST `/api/posts/mine/{postId}/poster`, multipart/form-data의 `file`에 JPG·PNG를 첨부한다. 작성자·DRAFT/REJECTED 상태만 허용한다. 현재 이미 등록된 POSTER가 있으면 409이며 교체 API는 없다.

실제 이미지 형식 검사, 10MB 이하, 가로/세로 각각 10,000px 이하·총 2천만 픽셀 이하. 서버에서 다시 인코딩하여 UUID 이름으로 저장한다. 원래 파일명은 DB에 저장하며 한글 이름을 금지하는 코드가 없다. 한글 이름의 실제 브라우저 업로드는 추가 검증 대상이다. 원본 회전 메타데이터 보존은 구현되지 않았다.

201 응답 예시:

```json
{
  "id": 1,
  "postId": 2,
  "originalName": "poster.png",
  "contentType": "image/png",
  "sizeBytes": 123456,
  "imageUrl": "/api/posts/mine/2/poster"
}
```
GET imageUrl은 작성자 세션이 필요하며 image/png 또는 image/jpeg 파일을 반환한다. Cache-Control은 no-store다. 다른 작성자·없는 첨부·없는 저장 파일은 404.

```javascript
const form = new FormData();
form.append("file", selectedFile);
const response = await fetch(`${apiBase}/api/posts/mine/${postId}/poster`, {
  method: "POST",
  credentials: "include",
  headers: { [csrf.headerName]: csrf.token },
  body: form
});
if (!response.ok) throw new Error(`업로드 실패: ${response.status}`);
const result = await response.json();
```
Content-Type은 직접 지정하지 않는다. 브라우저가 boundary를 설정한다. imageUrl은 상대 경로이므로 다른 origin의 프론트에서는 API 기준 주소와 합쳐야 한다. 보호된 이미지는 로그인 쿠키를 전달하는 요청으로 불러온다.

## 오류 처리

| 코드 | 주요 상황 |
|---|---|
| 400 | 입력 검증 실패, 잘못된 날짜/페이지, 지원되지 않는 이미지 |
| 401 | 인증 필요, 잘못된 로그인 |
| 403 | CSRF 검증 실패, 담당자 권한 부족 |
| 404 | 없는/다른 작성자의 게시물, 이미지 없음 |
| 409 | 중복 이메일, 수정 불가 상태, 이미 등록된 전단지 |
| 500 | 파일 저장 실패 등 서버 오류 |

SecurityConfig의 인증/접근 거부 핸들러는 상태만 설정하므로 401·403 본문이 비어 있을 수 있다. 제공된 폴더에는 통일된 예외 응답 클래스가 없다. 다른 오류 JSON의 정확한 형태·multipart 한도 초과 응답은 실제 서버 응답 확인 후 확정한다. 프론트는 모든 오류 본문을 JSON으로 가정하지 않는다. 예: `HTTP/1.1 403 Forbidden` + 빈 본문.

## 승인 요청·반려 이력

### 승인 요청

POST `/api/posts/mine/{postId}/approval-request` — 작성자 세션·CSRF 필요, 요청 본문 없음.

- DRAFT·REJECTED만 허용하며 다른 상태는 409.
- title·organizer는 공백이 아니어야 한다.
- category는 CLUB/CONTEST/EVENT/NOTICE/OTHER 중 하나.
- displayUntil 필수, 한국 날짜 기준 오늘 또는 이후.
- POSTER 첨부 필수 및 실제 저장 파일 존재 확인.
- 필수 입력 누락은 400, 없는 저장 파일은 404.

200 응답:
```json
{"id":6,"title":"Rejection test","status":"PENDING"}
```

승인 증빙 필수 검사는 아직 없다. PENDING 게시물의 본문 수정·이미지 업로드는 현재 409다.

### 승인·반려 이력

GET `/api/posts/mine/{postId}/approval-logs` — 작성자만, CSRF 불필요.

200 응답 예시 (createdAt 내림차순 후 id 내림차순):
```json
[
  {
    "id":2,
    "decision":"REJECTED",
    "reason":"장소를 N111로 수정해주세요.",
    "createdAt":"2026-10-08T14:16:00"
  }
]
```

이력이 없으면 `[]`. 다른 작성자·없는/삭제된 게시물은 404. 수정·재요청 후에도 이력은 유지된다. 현재 status가 REJECTED일 때 최신 반려 사유를 안내하고, 과거 이력은 별도로 표시한다. 이 응답은 담당자 ID·이메일을 포함하지 않는다.

## 담당자 API

### 승인

POST `/api/admin/posts/{postId}/approve` — ADMIN·CSRF, 요청 본문 없음.

PENDING만 처리한다. 종료일 누락/경과 및 POSTER 첨부 없음은 409, 실제 파일 없음은 404. 성공 시 APPROVED로 변경하고 publishedAt을 한국 시각으로 기록하며 approval_logs에 APPROVED 이력을 저장한다.

```json
{"id":2,"title":"Updated club recruitment","status":"APPROVED"}
```

### 반려

POST `/api/admin/posts/{postId}/reject` — ADMIN·CSRF, application/json:
```json
{"reason":"장소를 N111로 수정해주세요."}
```

사유는 공백 불가·최대 2000자. 저장 시 양끝 공백 제거. PENDING만 처리하며 성공 상태는 REJECTED다. 상태 변경과 이력 저장은 하나의 트랜잭션이며 담당자 처리 시 게시물 행 잠금으로 중복 처리를 방지한다.

```json
{"id":6,"title":"Rejection test","status":"REJECTED"}
```

이미 처리된 게시물은 409. 일반 회원의 담당자 API 접근은 403. 승인 대기 목록·담당자 상세·담당자 이미지 조회는 아직 제공하지 않는다. 작성자 이미지 API를 담당자용으로 대신 사용하면 안 된다.

### 로컬 담당자 계정 준비

실제 운영 권한 부여 UI는 없다. 로컬 테스트용 계정을 회원가입한 후 **MySQL (`mysql>`)**에서 정확한 테스트 이메일에만 적용한다:
```sql
USE brain_bucket;
UPDATE users SET role = 'ADMIN' WHERE email = 'admin@example.com';
SELECT id, email, role FROM users WHERE email = 'admin@example.com';
```
권한 변경 후 로그인해야 세션 권한이 반영된다. 비밀번호는 각자 회원가입 시 정한다. 다른 개발자 DB의 테스트 계정이 자기 DB에도 있다고 가정하지 않는다.

## 공개 목록·상세·이미지

모든 공개 GET은 로그인·CSRF 없이 가능하다. APPROVED이고 삭제되지 않은 게시물만 노출한다. 미승인·숨김·삭제 게시물의 상세/이미지는 404.

### 목록

GET `/api/posts/public?archived=false&page=0&size=20`.

| 파라미터 | 기본값 | 의미 |
|---|---|---|
| archived | false | false=현재 전단, true=지난 전단 |
| page | 0 | 0부터 시작 |
| size | 20 | 1~50 |

정렬: publishedAt 내림차순, id 내림차순. 응답은 배열만 반환하며 totalElements·totalPages·hasNext는 없다. 빈 결과는 `[]`. 검색·분류·연도 파라미터는 미구현이므로 필터된 결과를 기대하지 않는다.

```json
[
  {
    "id":2,
    "title":"Updated club recruitment",
    "organizer":"COMA",
    "category":"CLUB",
    "displayUntil":"2026-11-30",
    "imageUrl":"/api/posts/public/2/poster"
  }
]
```

한국 날짜 기준 displayUntil이 오늘 이상이면 현재, 오늘보다 이전이면 지난 전단. 날짜가 바뀔 때 조회 조건으로 구분하며 삭제·상태 변경·배치 작업은 없다. displayUntil=null은 두 목록에서 제외된다.

### 상세

GET `/api/posts/public/{postId}`:
```json
{
  "id":2,
  "title":"Updated club recruitment",
  "organizer":"COMA",
  "category":"CLUB",
  "description":"Recruitment details updated.",
  "location":"N111",
  "applicationUrl":null,
  "eventAt":null,
  "deadlineAt":"2026-11-01T18:00:00",
  "displayUntil":"2026-11-30",
  "publishedAt":"2026-10-08T14:11:19",
  "archived":false,
  "imageUrl":"/api/posts/public/2/poster"
}
```

종료된 승인 전단도 상세 조회 가능하며 archived=true. 예외적으로 APPROVED에 displayUntil=null이 있으면 현재 상세 구현은 archived=false를 반환한다. 신규 승인에서는 종료일 필수 검사로 이를 막는다. 공개 상세에는 조회수 필드가 없으며 조회수 증가도 미구현이다.

### 이미지

GET `/api/posts/public/{postId}/poster` → image/png 또는 image/jpeg, Cache-Control: no-store. 종료된 승인 전단도 유지한다. POSTER만 반환하며 승인 증빙 공개 주소는 없다.

상대 imageUrl을 API 서버 기준 주소와 합쳐 사용한다. 개발 프록시 사용 시 프론트 origin에서 /api 경로를 호출할 수 있다.

## 프론트 연결 예제

아래는 자바스크립트 코드이며 PowerShell 명령이 아니다. apiBase는 로컬 API 직접 주소 또는 개발 프록시 사용 시 빈 문자열로 설정한다. 직접 다른 origin으로 요청하려면 백엔드 CORS 설정이 먼저 필요하다.

```javascript
const apiBase = "http://localhost:8080";
let csrf;

async function api(path, { method = "GET", json, form } = {}) {
  const headers = {};
  if (method !== "GET" && method !== "HEAD") {
    if (!csrf) throw new Error("CSRF 토큰을 먼저 받아주세요.");
    headers[csrf.headerName] = csrf.token;
  }
  if (json !== undefined) headers["Content-Type"] = "application/json";
  const response = await fetch(`${apiBase}${path}`, {
    method,
    credentials: "include",
    headers,
    body: form ?? (json === undefined ? undefined : JSON.stringify(json))
  });
  const text = await response.text();
  let data = text;
  if (text) {
    try { data = JSON.parse(text); } catch { /* 빈/문자열 오류도 처리 */ }
  }
  if (!response.ok) {
    const error = new Error(`HTTP ${response.status}`);
    error.status = response.status;
    error.data = data;
    throw error;
  }
  return data;
}

async function refreshCsrf() {
  csrf = await api("/api/auth/csrf");
}

async function login(email, password) {
  await refreshCsrf();
  const member = await api("/api/auth/login", {
    method: "POST", json: { email, password }
  });
  await refreshCsrf();
  return member;
}

// 회원가입은 refreshCsrf 후 signup 호출, 이어 login을 별도로 호출한다.
// await login(입력한이메일, 입력한비밀번호);
// const post = await api("/api/posts", {
//   method: "POST",
//   json: { title: "모집", organizer: "COMA", category: "CLUB",
//           displayUntil: "2026-11-30", showViewCount: true }
// });
// const form = new FormData();
// form.append("file", selectedFile);
// await api(`/api/posts/mine/${post.id}/poster`, { method: "POST", form });
// await api(`/api/posts/mine/${post.id}/approval-request`, { method: "POST" });
// const current = await api("/api/posts/public?archived=false&page=0&size=20");
```

이 함수는 JSON/텍스트 API용이다. 이미지 파일은 img 또는 별도 fetch→blob으로 읽는다. 내 이미지 fetch 시 credentials를 포함하며 blob URL은 사용 종료 시 URL.revokeObjectURL로 정리한다. 로그아웃은 POST 후 토큰과 회원 UI를 초기화한다.

## 연동 체크리스트와 알려진 차이

- 새 로컬 DB에는 계정·전단·이미지가 없다. 직접 가입·등록·승인해야 공개 목록이 생긴다.
- 서버 재시작 후 로그인/CSRF를 다시 준비한다.
- PENDING 또는 APPROVED에서 PUT/이미지 업로드는 409. 이미지 교체는 미구현이다.
- 기존 API를 쓰는 화면은 그대로 진행하며 새 기능 연결이 필요할 때 최신 main을 반영한다.
- UTF-8 응답을 Windows PowerShell이 잘못 해석하면 한글이 깨져 보일 수 있다. 로컬 반려 사유는 UTF-8 직접 읽기로 정상 확인했다. 브라우저의 실제 한글 업로드·입력도 함께 확인한다.
- 승인 증빙·스냅샷은 아직 없어 현재 승인 기능이 전체 승인 업무를 완성한 상태는 아니다.
- CORS, 담당자 조회, 이미지 교체, 승인 후 수정, 검색/분류/연도, 스크랩, 조회수는 남은 작업이다.
- API 변경은 적용 전 공유하며 DB 변경 SQL과 문서를 함께 갱신한다.
