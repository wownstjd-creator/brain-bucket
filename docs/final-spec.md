# 현재 구현 명세 — 2026-10-11

## 계정과 권한

USER는 자신의 초안·첨부·승인 요청·취소·삭제·스크랩을 관리합니다. ADMIN은 전단과 증빙을 조회하고 승인/반려·내림/복구·게시 일정을 관리합니다. 다른 사용자의 글을 작성자 삭제 API로 삭제할 수 없습니다. 비밀번호는 BCrypt 해시로 저장합니다. 자체 가입/세션 로그인만 구현했고 학교 로그인·메일 인증은 없습니다.

비밀번호 변경은 현재 비밀번호 확인 후 8자 이상/UTF-8 72바이트 이하/기존과 다른 값을 요구합니다. 변경하면 현재 기기 세션을 종료합니다. 다른 기기의 이미 발급된 세션은 이 기능으로 종료하지 않습니다. 다음 로그인부터 변경된 비밀번호를 사용합니다. 메일 비밀번호 재설정은 아직 없습니다.

## 게시물 상태

| 동작 | 이전 | 이후 | 권한 |
| --- | --- | --- | --- |
| 초안 생성 | 없음 | DRAFT | 로그인 사용자 |
| 승인 요청 | DRAFT/REJECTED | PENDING | 작성자, 필수 본문·기간·전단·증빙 충족 |
| 요청 취소 | PENDING | DRAFT | 작성자, 최신 version |
| 승인/반려 | PENDING | APPROVED/REJECTED | 관리자 |
| 승인 글 수정·임시 저장 | APPROVED | DRAFT | 작성자, 재승인 필요 |
| 내림/복구 | APPROVED/HIDDEN | HIDDEN/APPROVED | 관리자, 사유 기록 |
| 삭제 | 삭제되지 않은 상태 | deleted_at 기록 | 작성자, 최신 version |

승인 상태라도 게시 시작 전에는 공개되지 않습니다. 종료된 승인 글은 아카이브에 포함됩니다. 작성자 삭제 후 목록·공개 상세·현재 이미지 접근을 차단하고 스크랩에서 제외합니다. 삭제는 관리자 복구 대상이 아닙니다. 처리 이력의 본문·첨부는 작성자/관리자 조회용으로 보존합니다.

## 입력·첨부

신청 URL·장소·행사/마감 시각은 선택입니다. 빈 URL은 null이며 입력하면 http/https 주소여야 합니다. 임시 저장은 제목/주최자/내용 확인 체크를 강제하지 않고 승인 요청 시 검증합니다. 날짜만 있는 행사는 00:00, 마감은 23:59입니다. 시간만 있는 값은 허용하지 않습니다.

POSTER/EVIDENCE/ORIGINAL은 서버 파일로 저장하고 DB에는 저장 키/MIME/크기를 기록합니다. 서버 파일 한도는 10MB입니다. DRAFT/REJECTED의 첨부를 제거할 수 있고 처리 이력에서 참조하는 실제 파일은 유지합니다. 버전 충돌은 409와 새로고침 안내로 처리합니다.

## 조회

공개·아카이브 검색/분류/연도 조건은 서버에서 적용하고 한 페이지 20개를 조회합니다. 내 글/관리 목록은 카드 API로 version과 첨부 유무를 함께 받아 상세 HTTP 요청을 글 수만큼 반복하지 않습니다. 관리 상태 필터도 서버에서 적용합니다. 스크랩 목록은 공개 가능한 저장 글만 서버에서 검색·분류·페이지 조회합니다. 마지막 페이지가 정확히 20개이면 다음 빈 페이지로 갈 수 있으며 전체 개수 조회는 제공하지 않습니다.

## DB 관계

| 테이블 | 주요 데이터 | 관계 |
| --- | --- | --- |
| users | email UNIQUE, password_hash, display_name, role | 작성자·검토자·스크랩 사용자 |
| posts | author_id, 본문, status, display_from/until, version, view_count, deleted_at | users 1:N posts |
| post_files | post_id, file_kind, storage_key, content_type, size_bytes | posts 1:N files, 글·종류당 한 개 |
| approval_logs | post_id, reviewer_id, decision, reason, snapshot, 당시 첨부 키·MIME | posts 1:N logs, users 1:N logs |
| bookmarks | user_id, post_id, created_at | users:N posts, 복합 기본키 |
| flyway_schema_history | 적용 버전과 성공 여부 | 스키마 변경 이력 |

이번 목록/계정 기능은 기존 컬럼만 사용합니다. V1/V2를 수정하지 않습니다. DB 생성·변경 절차는 database.md, 파일/DB 백업은 deployment.md를 따릅니다.

## 아직 기획/환경이 필요한 범위

학교 로그인, 메일 인증/비밀번호 재설정, 직인 자동 검증, 템플릿/누끼, 실제 외부 배포와 백업 복구 시연, 실기기 OCR 품질 확인은 완료하지 않았습니다. 친구의 피그마 시연 문서는 위 실제 동작과 맞춰 별도로 준비합니다.
