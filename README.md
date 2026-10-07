# BrainBucket — 브레인버킷

QR로 접근하는 모바일 중심 교내 전단지 게시·승인·아카이빙 서비스.

로그인 없이 전단지를 열람하고, 회원은 전단지를 게시하거나
스크랩할 수 있는 서비스를 목표로 개발하고 있습니다.

## 기술 및 역할

- 백엔드: Java 21, Spring Boot, Spring Data JPA
- DB: MySQL 8.0
- 빌드: Maven Wrapper
- 2인 개발: 백엔드·DB / 디자인·프런트엔드
- 학교의 게시 허가 및 직인 승인 절차는 함께 협의 예정

## 현재 구현 상태

구현 및 실행 확인:
- Spring Boot와 MySQL 연결
- 회원가입 API
- 이메일 형식 및 필수 입력 검증
- 이메일 중복 검사
- BCrypt 비밀번호 해시 저장
- 개발 확인용 회원 수 조회

DB 테이블 생성:
- users: 회원
- posts: 전단지 게시물
- post_files: 첨부파일 위치와 정보
- bookmarks: 회원별 스크랩
- approval_logs: 승인·반려 이력

구현 예정:
- 로그인·로그아웃 및 권한 관리
- 전단지 등록·조회·수정
- 최신순 목록 및 지난 전단지 아카이브
- 이미지 업로드
- 스크랩 및 조회수
- 관리자 승인·반려

테이블 생성과 API 구현은 별개입니다.
현재 게시물·파일·스크랩·승인 기능의 API는 아직 구현되지 않았습니다.

## 폴더 구성

- backend/: Spring Boot 프로젝트
- database/schema.sql: 테이블 생성 SQL
- docs/api.md: 현재 API 문서
- frontend/: 프런트엔드 작업 예정

## 실행 준비

1. Java 21과 MySQL 8.0을 설치합니다.
2. 저장소를 clone합니다.
3. MySQL 서비스를 실행합니다.

각 개발자는 자신의 로컬 MySQL을 사용합니다.
DB 계정과 비밀번호, 회원 데이터는 GitHub로 공유하지 않습니다.

## DB 생성

저장소 최상위 폴더에서 PowerShell을 열고 MySQL에 접속합니다.

```powershell
& "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe" --user=root --password
```

MySQL 설치 위치와 사용자 이름이 다르면 본인 환경에 맞게 변경합니다.
비밀번호 입력 후 아래 SQL을 실행합니다.

```sql
CREATE DATABASE IF NOT EXISTS brain_bucket
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE brain_bucket;

SOURCE database/schema.sql;

SHOW TABLES;

EXIT;
```

schema.sql은 빈 DB에 테이블을 처음 생성할 때 사용하는 파일입니다.
이미 테이블이 있는 DB에는 반복 실행하지 않습니다.
회원 등 실제 데이터는 포함되어 있지 않습니다.

## 백엔드 실행 — Windows PowerShell

위와 같은 저장소 최상위 폴더에서 실행합니다.

```powershell
cd backend

$env:DB_USERNAME = "본인의_MySQL_계정"
$env:DB_PASSWORD = "본인의_MySQL_비밀번호"

.\mvnw.cmd spring-boot:run
```

환경변수 값은 본인 환경에 맞게 입력합니다.
첫 실행 시 Maven이 필요한 라이브러리를 다운로드하므로 인터넷 연결이 필요합니다.

콘솔에 `Started BrainBucketBackendApplication`이 나오면 실행 완료입니다.

- 서버 주소: http://localhost:8080
- 종료: 실행 중인 터미널에서 Ctrl+C

환경변수는 현재 PowerShell 세션에 설정됩니다.
새 터미널을 열면 다시 설정해야 합니다.
비밀번호를 코드나 Git에 저장하지 않습니다.

IntelliJ에서는 backend 폴더를 프로젝트로 열고,
BrainBucketBackendApplication 실행 구성의 환경변수에
DB_USERNAME과 DB_PASSWORD를 설정한 뒤 실행할 수도 있습니다.

## 동작 확인

브라우저에서 아래 주소에 접속합니다.

http://localhost:8080/api/dev/users/count

빈 DB라면 다음 결과가 나옵니다.

```json
{"count":0}
```

회원가입 요청과 응답 예시는 docs/api.md를 참고합니다.

## 개발 시 참고

- 공개 열람을 목표로 하지만 전단지 API는 아직 구현 전입니다.
- 현재 로그인 및 관리자 권한 검사는 구현 전입니다.
- 현재 실행 환경은 로컬 개발용입니다.
- localhost는 접속하는 기기 자신의 주소입니다.
  친구의 컴퓨터나 휴대폰에서 내 서버에 접속하는 주소가 아닙니다.
- 프런트엔드의 별도 포트 접속을 위한 CORS 설정은 추후 협의합니다.
- 개발 확인용 회원 수 API는 배포 전에 제거하거나 접근을 제한합니다.