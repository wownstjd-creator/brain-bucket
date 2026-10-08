# Brain Bucket API 연결 안내

기준: 2026-10-08 제공된 소스. 예시 ID·시간·파일 크기는 설명용이다. 로그인·임시 대자보·이미지는 사용자 로컬에서 동작 확인되었으며, 이 문서 작성 중 서버를 별도로 실행하지 않았다.

## 실행 환경

- Java 21, Spring Boot 4.1.1, Maven, MySQL.
- 기본 API 주소: `http://localhost:8080` (설정 파일에 포트 재정의 없음).
- DB: `jdbc:mysql://localhost:3306/brain_bucket`.
- DB 계정은 `DB_USERNAME`, `DB_PASSWORD` 환경변수로 설정.
- 이미지: 서버 실행 작업 디렉터리 기준 `uploads`에 저장. Git으로 이미지 파일이 자동 공유되지는 않는다.
- multipart 파일 한도 10MB, 요청 전체 한도 12MB.
- Hibernate ddl-auto: `none`.
- 기존 DB 테이블 생성 SQL/마이그레이션은 이번 첨부에 없으므로 새 PC의 DB 초기화 절차는 별도 공유 필요.

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

`/api/admin/**`는 ADMIN 권한 제한이 설정되어 있지만 승인 API는 아직 구현되어 있지 않다.

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
| showViewCount | boolean 또는 null | 생략/null이면 true |

임시 저장은 미완성 입력을 허용하며 생성 상태는 DRAFT다. 날짜 타입은 LocalDateTime으로 시간대/오프셋이 없다. 현재 생성·수정 시각은 서버 기본 시간대의 LocalDateTime.now()를 사용한다. 한국 시간 통일은 별도 구현해야 한다.

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
  "showViewCount": true,
  "status": "DRAFT",
  "viewCount": 0,
  "publishedAt": null,
  "createdAt": "2026-10-08T13:00:00",
  "updatedAt": "2026-10-08T13:00:00"
}
```
현재 상세 응답에는 imageUrl·displayUntil·반려 사유가 없다. 작성자 이미지 주소는 게시물 ID로 별도 구성한다. 내 게시물이 아니거나 삭제/미존재면 404.

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

## 추가 구현 및 팀 합의

공개 목록·상세·이미지, 이미지 교체, 승인 증빙, 승인 요청/승인/반려, 승인 당시 스냅샷, 아카이브, 검색·분류 탐색, 스크랩은 미구현이다. 상태 enum 존재는 해당 API 구현을 의미하지 않는다. 조회수 필드는 있지만 조회수 증가 처리는 제공된 상세 서비스에 없다.

제안된 규칙: eventAt=행사 일시, deadlineAt=신청 마감, 새 displayUntil=게시 종료일. 한국 시간으로 종료일까지 공개 후 다음 날 아카이브. 분류는 CLUB/CONTEST/EVENT/NOTICE/OTHER. 승인된 게시물 수정 시 비공개 후 재승인, 반려 사유 필수, 증빙은 작성자/담당자만 열람, 승인 당시 내용/이미지 보존. 이 규칙은 현재 API의 보장과 구분하며 팀 확인 및 구현이 필요하다.

연결 전 남은 정보: 프론트 origin/포트, DB 초기화 SQL, 실제 오류 응답 샘플. API 변경은 적용 전 공유하고 문서를 함께 갱신한다.
