# 실행·배포·백업·복구

## Windows 기존 MySQL에서 계속 개발

기존 .env와 backend/uploads를 유지하고 루트에서 `.\scripts\run.ps1`로 재시작합니다. http://localhost:8080/#/board 에서 Ctrl+F5를 누릅니다. 이번 패치는 DB 컬럼 변경이 없습니다. 프론트 빌드 결과도 포함합니다. 다른 사람이 pull한 뒤 자기 DB 접속 정보를 최초 설정하면 실행할 수 있습니다.

## 로컬 Docker

Docker Engine와 Compose v2를 설치하고 루트 .env에 DB_PASSWORD와 MYSQL_ROOT_PASSWORD를 서로 다른 값으로 설정합니다.

```bash
docker compose --env-file .env up -d --build
```

http://localhost:8080 에 접속합니다. 앱 포트는 127.0.0.1에만 바인딩됩니다. Docker의 mysql_data/uploads는 Windows에서 쓰던 MySQL/backend/uploads와 별개입니다. 기존 데이터는 자동으로 이동되지 않습니다.

## 실제 Linux 서버와 HTTPS

필요한 정보는 서버 접속 권한과 공인 IP, 소유한 도메인입니다. 도메인의 A 레코드를 서버 IP로 지정하고 80/443 포트를 허용합니다. IPv6 AAAA 레코드가 있다면 실제 서버 IPv6와 일치해야 합니다. 서버에 Docker Engine/Compose v2와 Git을 설치합니다.

저장소를 서버에 clone하고 루트 .env를 생성합니다. DB_PASSWORD·MYSQL_ROOT_PASSWORD는 새로 정하고 DOMAIN에는 `board.example.com`처럼 프로토콜 없는 실제 호스트명을 넣습니다. 기존 .env의 로컬 DB_USERNAME=root 값은 Docker 앱에서 사용하지 않습니다.

```bash
bash scripts/deploy.sh
docker compose --env-file .env -f deploy/compose.yaml logs --tail=100 app proxy
```

운영 Compose는 별도 프로젝트 이름 brain-bucket-production을 사용합니다. MySQL과 앱 포트는 외부에 공개하지 않고 Caddy만 80/443을 엽니다. Caddy가 도메인의 TLS 인증서를 발급·갱신하고 앱은 Secure 세션 쿠키를 사용합니다. 실제 DNS/서버가 없으면 HTTPS 발급은 검증할 수 없습니다.

접속 후 확인할 항목:

1. https://도메인/api/auth/csrf 응답, 화면 로딩, 회원가입·로그인·로그아웃·비밀번호 변경.
2. 전단 작성·첨부·승인·검색·페이지 이동·스크랩.
3. 서버 재시작 후 DB/첨부 유지, 백업 후 별도 환경에서 복구.

Git pull 후 같은 deploy.sh를 실행하면 새 이미지를 빌드해 갱신합니다. 업데이트 전 백업하고 이전 커밋 ID를 기록합니다. 스키마 변경이 있는 버전은 소스 롤백만으로 DB를 되돌리지 않습니다. `docker compose down -v`는 DB/첨부 볼륨을 삭제하므로 운영에서 사용하지 않습니다.

## Docker DB와 첨부를 함께 백업

Linux 서버용 Bash 스크립트입니다. Docker가 없는 Windows 네이티브 MySQL에는 적용하지 않습니다. 운영 설정을 선택한 뒤 실행합니다.

```bash
BB_COMPOSE_FILE=deploy/compose.yaml bash scripts/backup.sh
```

로컬 Docker는 `bash scripts/backup.sh`를 사용합니다. 기본 위치는 backups/UTC시각 입니다. app을 잠시 정지한 상태에서 DB SQL과 uploads.tgz를 함께 만들고 SHA256SUMS를 생성한 후 app을 다시 시작합니다. 백업 도중 다른 도구로 DB를 수정하거나 마이그레이션을 실행하지 않습니다. 실패한 백업 디렉터리는 복구에 사용하지 않습니다.

백업에는 계정 해시·게시물·증빙이 포함되므로 비공개 저장소로 따로 복사하고 접근 권한을 제한합니다. backups/는 Git와 Docker 빌드에서 제외합니다. 자동 실행 주기와 보관 기간은 서버 운영 시 설정합니다.

## 복구

먼저 별도 테스트 환경에서 연습합니다. 복구는 지정한 Compose 프로젝트의 현재 DB와 첨부를 교체합니다. 복구 전 현 상태를 새 디렉터리에 백업합니다. 같은 코드 버전으로 복구한 뒤 앱 시작 시 Flyway 적용 상태를 확인합니다.

```bash
BB_COMPOSE_FILE=deploy/compose.yaml bash scripts/restore.sh backups/20261011T100000Z --confirm-replace
```

스크립트가 체크섬과 압축 경로를 확인한 뒤 app을 멈추고 SQL/첨부를 복원합니다. 중간 실패 시 app은 정지 상태로 남습니다. 오류를 해결하고 동일 백업으로 복구를 다시 수행합니다. 원본 백업은 수정하지 않습니다. Caddy 인증서 볼륨은 포함하지 않으며 DNS 조건이 맞으면 다시 발급됩니다.

## 검증 범위

Java/프론트 빌드와 H2/HTTP 자동 테스트를 수행했습니다. Compose YAML 구조, Bash 문법과 백업/복구 흐름은 점검합니다. 이 환경에는 Docker Engine와 실제 서버·도메인이 없어 컨테이너 실행, 실제 mysqldump 복구와 외부 HTTPS 접속은 아직 검증하지 않았습니다. 따라서 배포 준비 완료이며 실제 배포 완료는 아닙니다.

참고: [Docker 시작 순서](https://docs.docker.com/compose/how-tos/startup-order/), [Caddy HTTPS](https://caddyserver.com/docs/quick-starts/https), [MySQL 8 백업](https://dev.mysql.com/doc/refman/8.0/en/mysqldump.html).
