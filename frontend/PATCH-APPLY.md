# 기존 저장소에 적용하는 수정 패치

별도 프로젝트가 아닙니다. ZIP 안의 .gitignore, README.md, .env.example, compose.yaml, backend, docs, scripts를 기존 brain-bucket 저장소 최상위에 덮어쓰세요. .git, 실제 .env, uploads는 포함하지 않습니다. 기존 파일/폴더를 삭제하지 마세요.

이번 패치는 회원가입 UserController.java를 유지하고 진단용 count API만 제거합니다. 기존 MySQL·이미지·계정은 그대로 유지합니다. Flyway 시작 시 없는 컬럼만 추가하며 수동 SQL을 이미 실행한 DB도 지원합니다.

기존 서버를 Ctrl+C로 종료하고 기존 backend 폴더, DB 환경변수가 남아 있는 터미널에서 `.\mvnw.cmd spring-boot:run`으로 다시 실행하세요. SQL을 다시 실행할 필요는 없습니다. 초기 실행에는 Flyway 의존성을 다운로드하므로 인터넷이 필요합니다.

이후 팀원은 Java/MySQL·개인 접속 설정을 한 번 준비한 뒤 pull하고 저장소 최상위에서 `.\scripts\run.ps1`을 실행하면 됩니다. .env.example을 .env로 복사하고 자신의 DB_USERNAME/DB_PASSWORD를 설정합니다.

프론트 소스 변경 시 scripts/build.ps1로 빌드된 static도 갱신하고 함께 커밋합니다. 수정된 .gitignore는 static을 커밋할 수 있게 합니다. GitHub Desktop 변경 목록에 backend/src/main/resources/static의 index.html과 assets가 포함되는지 확인하세요.

커밋 Summary: feat: DB 자동 마이그레이션 및 팀 개발 실행 환경 정리

검증: Java21, MySQL8.0.46의 빈 DB/구버전/수동 적용 DB 자동 업데이트 및 데이터 보존, 재실행, Boot 시작, 회원가입 HTTP 및 기존 기능 테스트 총4개 통과. JAR 빌드 통과. Windows 스크립트 직접 실행은 사용자 환경 확인 필요.
