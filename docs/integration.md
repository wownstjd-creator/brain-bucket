# 친구에게 공유할 변경

최종 백엔드 기준은 MySQL main입니다. 친구 frontend를 함께 가져왔고 API 어댑터를 변경했습니다. 화면 디자인·보정·OCR은 유지했습니다.

- frontend/src/capture/api.ts: 한글 분류→기존 코드, postingStart/End→displayFrom/Until, Base64→Blob/FormData, 기존 승인·공개 주소, 관리자 상세 응답 변환, 전체 목록 페이지 조회.
- App.tsx: 새 역사 이미지 URL, 관리자 상태 조회, 조회수 표시.
- BoardExperience.tsx: MySQL 계정 스크랩 연결, 전단 열기 조회수 호출.
- vite.config.ts: 8080 개발 프록시.

이 통합본의 frontend와 backend를 함께 사용하세요. 친구의 H2용 backend/data/관리자 계정을 main에 덮어쓰지 않습니다. 프론트 기능 검증과 실제 MySQL 검증이 완료된 뒤 통합 브랜치를 main에 합칩니다.

이 작업에서 실제 GitHub 커밋/푸시는 수행하지 않았습니다. 커밋 예: feat: MySQL 기반 전단 게시 관리 및 프론트 통합
