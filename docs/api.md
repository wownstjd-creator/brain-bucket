# BrainBucket API

현재 로컬 서버 주소: http://localhost:8080

이 문서는 현재 구현된 API를 기준으로 작성했습니다.
로그인, 게시물, 파일 업로드, 스크랩, 관리자 승인 API는 아직 없습니다.

## 구현된 API

| 방식 | 경로 | 용도 |
|---|---|---|
| POST | /api/auth/signup | 회원가입 |
| GET | /api/dev/users/count | 개발 확인용 회원 수 조회 |

## 회원가입

POST /api/auth/signup

로그인 없이 요청할 수 있습니다.

Content-Type: application/json

### 요청 예시

```json
{
  "email": "test@example.com",
  "password": "TestPass123!",
  "displayName": "BrainBucket"
}
```

### 입력 조건

| 항목 | 조건 |
|---|---|
| email | 필수, 이메일 형식, 최대 255자 |
| password | 필수, 8~72자, UTF-8 기준 최대 72바이트 |
| displayName | 필수, 최대 50자 |

- 이메일은 앞뒤 공백 제거 후 소문자로 저장합니다.
- 닉네임은 앞뒤 공백을 제거합니다.
- 비밀번호는 공백을 포함하여 입력한 값 그대로 해시 처리합니다.
- 회원 권한은 서버에서 USER로 지정합니다.
- 클라이언트는 role을 가입 입력 항목으로 보내지 않습니다.

### 성공 응답

HTTP 201 Created

```json
{
  "id": 1,
  "email": "test@example.com",
  "displayName": "BrainBucket",
  "role": "USER"
}
```

id는 DB가 생성하며 예시와 다를 수 있습니다.
비밀번호와 비밀번호 해시는 응답에 포함하지 않습니다.
회원가입 성공이 로그인 완료를 의미하지는 않습니다.

### 오류

| HTTP 상태 | 의미 |
|---|---|
| 400 | 필수 입력, 형식 또는 길이 조건 위반 |
| 409 | 이메일 중복 또는 DB 제약에 따른 저장 실패 |

현재 오류 응답 본문은 Spring Boot 기본 처리에 따릅니다.
프런트에서 사용할 공통 오류 JSON 형식은 추후 구현할 예정입니다.
오류 메시지가 항상 응답에 포함된다고 가정하지 않습니다.

### PowerShell 테스트

```powershell
$signupBody = @{
    email = "test@example.com"
    password = "TestPass123!"
    displayName = "BrainBucket"
} | ConvertTo-Json

Invoke-RestMethod `
    -Uri "http://localhost:8080/api/auth/signup" `
    -Method Post `
    -ContentType "application/json; charset=utf-8" `
    -Body $signupBody
```

실행 중인 서버에 테스트 회원을 실제로 저장합니다.
같은 이메일로 반복하면 409가 발생합니다.

## 회원 수 조회 — 개발 확인용

GET /api/dev/users/count

### 성공 응답

HTTP 200 OK

```json
{
  "count": 1
}
```

현재 DB의 회원 수를 반환합니다.
배포 전 제거 또는 접근 제한 대상입니다.

## 프런트와 협의할 사항

- 로그인 유지 및 인증 방식
- 프런트 개발 서버 주소와 CORS
- 전단지 목록·상세에 필요한 응답 필드
- 페이지 나누기 및 카테고리·연도 필터
- 이미지 업로드 형식과 용량 제한
- 날짜·시간 표시 기준
- 공통 오류 응답 형식

이미지 업로드는 JPG·PNG 지원을 우선 검토 중이며,
아직 업로드 API나 형식 제한은 구현되지 않았습니다.