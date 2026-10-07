# BrainBucket API

로컬 서버: http://localhost:8080

## 현재 구현된 API

| 방식 | 경로 | 기능 | 로그인 필요 |
|---|---|---|---|
| GET | /api/auth/csrf | CSRF 토큰 발급 | 아니요 |
| POST | /api/auth/signup | 회원가입 | 아니요 |
| POST | /api/auth/login | 로그인 | 아니요 |
| GET | /api/auth/me | 현재 회원 정보 | 예 |
| POST | /api/auth/logout | 로그아웃 | 아니요 |
| GET | /api/dev/users/count | 개발용 회원 수 조회 | 아니요 |

로그아웃은 미로그인 상태에서도 처리될 수 있습니다.
전단지·파일·스크랩·관리자 승인 API는 아직 구현 전입니다.

## 인증과 CSRF

서버 세션과 쿠키로 로그인 상태를 유지합니다.

클라이언트의 요청 순서:
1. GET /api/auth/csrf로 토큰을 받습니다.
2. 받은 쿠키를 유지합니다.
3. POST 요청에 응답의 headerName을 이름으로, token을 값으로 넣습니다.
4. 로그인 또는 로그아웃 성공 후 CSRF 토큰을 다시 받습니다.

회원가입과 로그인도 CSRF 토큰이 필요합니다.
CSRF 토큰은 로그인 증명이 아닙니다.

브라우저 fetch에서는 쿠키를 전달하기 위해 credentials: "include"를 사용합니다.
별도 출처의 프런트 연결을 위한 CORS 설정은 아직 구현 전입니다.

## CSRF 토큰

GET /api/auth/csrf

성공: 200 OK

```json
{
  "headerName": "X-CSRF-TOKEN",
  "token": "서버가 발급한 문자열"
}
```

토큰을 코드에 고정하거나 Git에 저장하지 않습니다.

## 회원가입

POST /api/auth/signup

Content-Type: application/json
CSRF 헤더와 동일 세션의 쿠키 필요

```json
{
  "email": "test@example.com",
  "password": "TestPass123!",
  "displayName": "BrainBucket"
}
```

입력 조건:
- email: 필수, 이메일 형식, 최대 255자
- password: 필수, 8~72자, UTF-8 기준 최대 72바이트
- displayName: 필수, 최대 50자

이메일은 앞뒤 공백 제거 후 소문자로 저장합니다.
닉네임은 앞뒤 공백을 제거합니다.
비밀번호는 BCrypt 해시로 저장합니다.
권한은 서버가 USER로 지정합니다.

성공: 201 Created

```json
{
  "id": 1,
  "email": "test@example.com",
  "displayName": "BrainBucket",
  "role": "USER"
}
```

회원가입 성공 후 자동으로 로그인되지는 않습니다.
비밀번호와 해시는 응답에 포함하지 않습니다.

## 로그인

POST /api/auth/login

Content-Type: application/json
CSRF 헤더와 동일 세션의 쿠키 필요

```json
{
  "email": "test@example.com",
  "password": "TestPass123!"
}
```

성공: 200 OK

```json
{
  "id": 1,
  "email": "test@example.com",
  "displayName": "BrainBucket",
  "role": "USER"
}
```

로그인 성공 시 세션 ID를 변경하고 인증 정보를 세션에 저장합니다.
기존 CSRF 토큰을 초기화하므로 다음 POST 요청 전에 새 토큰을 받습니다.
JWT는 반환하지 않습니다.

## 현재 회원 조회

GET /api/auth/me

로그인한 세션의 쿠키 필요

성공: 200 OK
응답 형식은 로그인 성공 응답과 같습니다.

로그인하지 않았거나 세션이 만료되면 401을 반환합니다.

## 로그아웃

POST /api/auth/logout

CSRF 헤더와 동일 세션의 쿠키 필요
요청 본문은 필요하지 않습니다.

성공: 204 No Content
응답 본문은 없습니다.

인증 정보와 세션을 정리합니다.
로그아웃 후 /api/auth/me는 401을 반환합니다.

## 개발용 회원 수 조회

GET /api/dev/users/count

성공: 200 OK

```json
{"count": 1}
```

배포 전 제거 또는 접근 제한 대상입니다.

## 오류 상태

| 상태 | 의미 |
|---|---|
| 400 | 입력 형식 또는 길이 조건 위반 |
| 401 | 로그인 실패 또는 로그인 필요 |
| 403 | CSRF 검사 실패 또는 권한 부족 |
| 409 | 회원가입 이메일 중복 또는 DB 제약에 따른 저장 실패 |

오류 본문은 아직 공통 JSON 형식으로 통일하지 않았습니다.
빈 본문이 올 수도 있으므로 HTTP 상태 코드를 기준으로 처리합니다.

## PowerShell 회원가입 테스트

서버를 실행한 상태에서 사용합니다.
동일 이메일로 반복하면 409가 발생합니다.

```powershell
$csrf = Invoke-RestMethod -Uri "http://localhost:8080/api/auth/csrf" -SessionVariable brainSession -ErrorAction Stop

$headers = @{}
$headers[$csrf.headerName] = $csrf.token

$body = @{
    email = "test@example.com"
    password = "TestPass123!"
    displayName = "BrainBucket"
} | ConvertTo-Json

Invoke-RestMethod -Uri "http://localhost:8080/api/auth/signup" -Method Post -WebSession $brainSession -Headers $headers -ContentType "application/json" -Body $body -ErrorAction Stop
```

## PowerShell 로그인·로그아웃 테스트

위 테스트 회원이 DB에 있어야 합니다.

```powershell
$csrf = Invoke-RestMethod -Uri "http://localhost:8080/api/auth/csrf" -SessionVariable brainSession -ErrorAction Stop

$headers = @{}
$headers[$csrf.headerName] = $csrf.token

$body = @{
    email = "test@example.com"
    password = "TestPass123!"
} | ConvertTo-Json

Invoke-RestMethod -Uri "http://localhost:8080/api/auth/login" -Method Post -WebSession $brainSession -Headers $headers -ContentType "application/json" -Body $body -ErrorAction Stop

Invoke-RestMethod -Uri "http://localhost:8080/api/auth/me" -WebSession $brainSession -ErrorAction Stop

$csrf = Invoke-RestMethod -Uri "http://localhost:8080/api/auth/csrf" -WebSession $brainSession -ErrorAction Stop

$headers = @{}
$headers[$csrf.headerName] = $csrf.token

Invoke-RestMethod -Uri "http://localhost:8080/api/auth/logout" -Method Post -WebSession $brainSession -Headers $headers -ErrorAction Stop

# 아래 요청은 로그아웃 확인용이며 401이 정상입니다.
Invoke-RestMethod -Uri "http://localhost:8080/api/auth/me" -WebSession $brainSession
```