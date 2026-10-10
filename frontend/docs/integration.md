# 팀원 업데이트 절차

기존 저장소에서 pull한 뒤 서버를 종료하고 저장소 최상위의 `.\scripts\run.ps1`로 다시 실행합니다. Java 21 이상·MySQL·자신의 .env DB 접속 설정은 최초 한 번 준비합니다. DB 구조 변경은 서버 시작 시 자동 적용하므로 수동 SQL 실행은 필요하지 않습니다.

frontend와 backend를 함께 사용합니다. 친구 H2의 backend/data와 계정을 덮어쓰지 않습니다. Git은 코드와 빌드된 화면을 공유하며 로컬 MySQL 계정·게시물·uploads는 공유하지 않습니다.

프론트 수정 후 `.\scripts\build.ps1`을 실행하고 frontend/src와 backend/src/main/resources/static을 함께 커밋합니다. 프론트 API 변환은 frontend/src/capture/api.ts에 있습니다.

이번 패치: Flyway 자동 업데이트, 초기 빈 DB 테이블 생성, 수동 적용 DB 호환, 회원가입 컨트롤러 유지, static 커밋 제외 수정, 실행 스크립트와 README 정리.

실제 GitHub 커밋/푸시는 사용자가 수행합니다. 커밋 예: feat: DB 자동 마이그레이션 및 팀 개발 실행 환경 정리
