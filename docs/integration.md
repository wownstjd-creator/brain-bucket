# 팀 개발과 폴더 역할 — 2026-10-26

기준은 이번 main ZIP의 MySQL 백엔드와 React 프론트입니다. 다른 예비본의 H2 엔티티·자동 관리자 계정 생성 코드를 섞지 않습니다.

- 화면·HTTP API 호출: frontend/src. api.ts의 필드명/경로 변환은 프론트 책임입니다.
- DB·인증·권한·API: backend/src. SQL/Java 마이그레이션과 DB 접속 설정도 이곳에서 관리합니다.
- frontend/backend·frontend/scripts·frontend/compose.yaml·frontend/docs는 중복을 제거하고 루트 backend/scripts/compose.yaml/docs로 통합했습니다.
- backend/static은 빌드된 화면, runtime은 빌드된 실행 파일입니다. 소스를 수정한 후 루트 scripts/build.ps1로 함께 갱신합니다.

이미 최초 환경 준비를 마친 팀원은 pull 후 서버를 종료하고 `2-start.cmd`로 재시작합니다. pull만으로 실행 중인 서버가 바뀌지는 않습니다. 최초 한 번 Java21 이상·MySQL8·개인 .env 설정이 필요합니다. Git은 계정 데이터나 uploads를 공유하지 않습니다.

이번 ZIP을 기존 저장소에 처음 적용할 때는 APPLY-FINAL.md를 따릅니다. 적용 스크립트가 덮어쓰기와 중복 파일 삭제를 함께 처리하며 기존 Git 연결·DB·업로드를 유지합니다.

이번 변경은 승인/반려 version 필수 검사, 승인 요청 version 전달, 포스터 조회 잠금 제거, 두 배포 DB 이력 호환 선택, 개발용 사용자 수 API 제거입니다. API 요청 규격은 api.md, DB 파일 위치와 이력 정책은 database.md, 검증 결과는 verification.md를 참고하세요.

GitHub 커밋·푸시는 사용자가 수행합니다. Summary: `fix: 프론트·백엔드 구조 정리 및 승인 버전·DB 이력 호환성 수정`.
