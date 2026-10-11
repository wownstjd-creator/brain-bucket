# BrainBucket 통합 API — 2026-10-10

기준: brain-bucket1010.zip + 친구 프론트 통합. 세션 쿠키·CSRF 인증, MySQL, 파일 저장, 기본 포트 8080. 프론트 5175의 개발 프록시 또는 정적 파일을 포함한 단일 JAR로 같은 origin을 사용합니다.

## 인증

GET /api/auth/csrf → POST /api/auth/login → GET /api/auth/csrf 재발급. 변경 요청은 응답의 headerName/token을 헤더로 보내고 credentials:include를 사용합니다. FormData는 Content-Type을 직접 지정하지 않습니다. 204는 본문이 없습니다.

| 메서드 | 주소 | 권한/응답 |
|---|---|---|
| GET | /api/auth/csrf | 비로그인 / 토큰 |
| POST | /api/auth/signup | 비로그인+CSRF / 201 |
| POST | /api/auth/login | 비로그인+CSRF / 회원 정보 |
| GET | /api/auth/me | 로그인 / 회원 정보 |
| POST | /api/auth/logout | 로그인+CSRF / 204 |

signup은 email/password/displayName, login은 email/password입니다. signup 자동 로그인 없음. 비밀번호 8~72자·UTF-8 72바이트 제한, 이메일 최대255자·중복409, 닉네임 최대50자. 일반 사용자는 ADMIN API에 403입니다.

## 작성·조회·수정

| 메서드 | 주소 | 설명 |
|---|---|---|
| POST | /api/posts | DRAFT 생성, 201 |
| GET | /api/posts/mine?page=0&size=20 | 내 목록 |
| GET | /api/posts/mine/{id} | 내 상세 |
| PUT | /api/posts/mine/{id} | 전체 수정 |

생성/수정 JSON:

```json
{"title":"동아리 모집","organizer":"COMA","category":"CLUB","description":"모집 안내","location":"N111","applicationUrl":"https://example.com","eventAt":null,"deadlineAt":null,"displayFrom":"2026-10-20","displayUntil":"2026-11-30","showViewCount":true,"version":0}
```

- 날짜시간은 오프셋 없는 한국 시각 LocalDateTime, 날짜는 yyyy-MM-dd.
- displayFrom은 추가된 시작일, displayUntil은 기존 종료일 유지. 시작일 null은 시작 제한 없음(기존 API 호환).
- 임시 저장에서 필수 내용 생략 가능. title150/organizer100/category30/description10000/location150/URL2048자 제한. 신청 링크는 http/https 호스트가 있는 주소만 허용.
- 시작일 > 종료일은400. version 생략은 기존 요청 호환, 지정 시 불일치409. 프론트는 상세 응답의 최신 version을 사용.
- 전체 수정이므로 생략한 내용 필드는 null로 덮어씀. DRAFT/REJECTED/APPROVED 수정 가능, 수정 후 DRAFT. PENDING/HIDDEN은409.
- 생성·수정 응답은 기존 {id,title,status}. 프론트는 저장 후 상세를 다시 조회.
- 상세는 기존 필드에 displayFrom/version/hasEvidence/hasOriginal이 추가됨.
- 내 목록 page>=0, size1~50, 기본20. 응답 배열. 삭제되지 않은 본인 게시물만.

## 파일

| 메서드 | 주소 | 용도 |
|---|---|---|
| POST | /api/posts/mine/{id}/poster | 기존 최초 포스터 등록(201), 이미 있으면409 |
| PUT | /api/posts/mine/{id}/poster | 기존 포스터 교체(200), 없으면404 |
| GET | /api/posts/mine/{id}/poster | 작성자 포스터 조회 |
| PUT | /api/posts/mine/{id}/attachments/{kind}?version=0 | 통합 첨부 등록/교체(200) |
| GET | /api/posts/mine/{id}/attachments/{kind} | 작성자 또는 ADMIN 첨부 조회 |

kind는 poster/evidence/original, multipart file 필드. JPG/PNG 10MB 이하, 각 변10000픽셀·전체2000만픽셀 한도. 서버 재인코딩·UUID 저장. 새 통합 PUT은 최초 등록과 교체 모두 처리. 변경은 본인 DRAFT/REJECTED만, 다른 상태409. APPROVED는 먼저 본문 수정하여 DRAFT로 전환해야 함. 이미지 변경도 게시물 version을 증가시킴.

응답: {id,postId,originalName,contentType,sizeBytes,imageUrl}. 새 첨부의 imageUrl은 attachments 주소. 원본·증빙은 공개 API가 없음. 검증 실패400, 접근 대상 없음404. DB 롤백 시 새 파일 정리, 커밋 후 옛 파일 정리. 과거 승인 이력이 참조하는 옛 파일은 보존. 불확실한 트랜잭션 결과에서는 파일 유지.

## 승인 및 이력

| 메서드 | 주소 | 용도 |
|---|---|---|
| POST | /api/posts/mine/{id}/approval-request | 본인 승인 요청, 본문 없음 |
| GET | /api/posts/mine/{id}/approval-logs | 기존 결정·사유·시각 이력 |
| GET | /api/posts/mine/{id}/history | 스냅샷·담당자명·당시 파일 URL 포함 이력, 본인/ADMIN |
| GET | /api/posts/mine/{id}/history/{logId}/{kind} | 본인/ADMIN 당시 파일 |
| POST | /api/admin/posts/{id}/approve | ADMIN 승인, 본문 없음 |
| POST | /api/admin/posts/{id}/reject | ADMIN 반려, JSON {reason} |

요청은 DRAFT/REJECTED만, title·organizer·유효 category(CLUB/CONTEST/EVENT/NOTICE/OTHER)·오늘 이후 종료일·포스터와 증빙 실제 파일 필수. 누락400, 파일 없음404. APPROVED/HIDDEN/PENDING 요청409. 승인은 PENDING만 처리, 종료일 및 이미지/증빙 검증 후 APPROVED. 반려 사유1~2000자. 상태 변경은 행 잠금 적용. 수정·재요청 후 과거 이력 유지.

새 history 항목: {id,decision,reason,createdAt,reviewerName,snapshot,imageUrl,evidenceUrl,originalUrl}. 당시 URL이 null이면 보존 파일 없음. 기존 이력의 snapshot은 null일 수 있음. 원래 요청자의 이메일·비밀번호는 반환하지 않음.

## 관리자 조회·게시 관리

| 메서드 | 주소 | 설명 |
|---|---|---|
| GET | /api/admin/posts?page=0&size=20 | 기존 PENDING 심사 목록 |
| GET | /api/admin/posts/{id} | 기존 {authorId,post,imageUrl} 상세 |
| GET | /api/admin/posts/{id}/poster | 관리자 포스터 |
| GET | /api/admin/posts/managed?status=ALL&page=0&size=20 | 관리 목록 요약 {id,title,status,version} |
| POST | /api/admin/posts/{id}/hide | APPROVED→HIDDEN |
| POST | /api/admin/posts/{id}/restore | HIDDEN→APPROVED, 기존 publishedAt 유지 |
| PUT | /api/admin/posts/{id}/schedule | 게시 기간·행사/마감 변경 |

managed status는 ALL/PENDING/APPROVED/HIDDEN/REJECTED, ALL에서도 DRAFT 제외. page>=0,size1~50. 프론트는 목록 후 기존 관리자 상세 조회. 기존 심사 목록은 작성 시각 오름차순, 관리 목록은 id내림차순.

hide/restore JSON: {"reason":"주최 측 요청","version":3}. schedule JSON:

```json
{"displayFrom":"2026-10-20","displayUntil":"2026-11-30","eventAt":null,"deadlineAt":null,"reason":"일정 변경","version":3}
```

사유1~2000자. schedule 시작/종료일 필수, 시작<=종료. DRAFT 변경409, 다른 상태 유지. 행사/마감 null은 값 제거. 승인 여부와 실제 기간을 별도 취급하므로 과거 종료일을 설정해 아카이브로 이동 가능. 모든 변경 이력에 당시 본문과 파일 기록, schedule은 변경 후 내용도 기록. HIDDEN 수정·재신청 우회 불가.

## 공개·검색

GET /api/posts/public?archived=false&page=0&size=20&search=모집&category=CLUB&year=2026

- 로그인 불필요. APPROVED·미삭제·시작일 도달한 게시물만.
- 시작일 미도달은 상세·이미지도404. 종료일 null은 공개 제외.
- 현재: 종료일>=한국 오늘, 지난 전단: 종료일<오늘. 연도는 종료일의 연도.
- search 최대200자, 제목·주최·설명 대소문자 무시 부분 일치. %, _는 문자 그대로 처리.
- category는 코드 값. year1900~9998, page>=0,size1~50. publishedAt/id내림차순.
- 목록 배열은 기존 필드 외 displayFrom/version/description/location/applicationUrl/eventAt/deadlineAt/publishedAt/status/viewCount 추가.
- GET /api/posts/public/{id}: 상세, archived 포함. GET /api/posts/public/{id}/poster: 이미지.
- 친구 화면은 전체 페이지를 받아 기존 필터 UI 사용. 서버 필터 API도 제공. 대규모 데이터에서는 화면도 서버 필터+페이지 방식으로 변경 필요.

## 계정 스크랩

GET /api/bookmarks?page=0&size=20: 공개 가능한 스크랩 전단 목록.
GET /api/bookmarks/ids: 해당 계정의 전체 저장 ID 배열(비공개 ID도 유지).
PUT /api/bookmarks/{id}: 저장204, 중복 저장도204. 공개 가능한 전단만 추가.
DELETE /api/bookmarks/{id}: 해제204, 이미 없어도204.

로그인 필요, 변경CSRF. 다른 계정과 분리. 숨김 전단은 목록에서 제외되며 복구하면 재표시. 원래 브라우저 스크랩 자동 이관 없음. 실제 최신 공개 데이터로 상세 확인.

## 조회수

POST /api/posts/public/{id}/view: 비로그인 가능하나 CSRF 헤더·세션 쿠키 필요. 세션당 한 전단1회. 공개 가능한 전단만, 숨김/예약/삭제404. 응답 {viewCount:숫자 또는 null}. showViewCount=false면 수치는 내부 집계하되 공개 응답null. GET 조회만으로는 증가하지 않음. 같은 세션 저장 ID는 최대1000개 후 초기화되어 아주 많은 전단을 본 세션에서는 재집계 가능. 봇 방지/정확한 사람 수 통계는 별도 범위.

## 오류와 로컬 ADMIN

400 입력/파일 검증, 401 로그인, 403 권한/CSRF, 404 대상 없음·비공개, 409 상태·버전 충돌, 413 multipart 한도, 500 저장 실패 등. 빈 오류 본문도 있으므로 항상 JSON이라고 가정하지 않음.

```sql
USE brain_bucket;
UPDATE users SET role='ADMIN' WHERE email='본인_테스트_관리자_이메일';
```

권한 변경 후 로그아웃·다시 로그인. 기존 개발용 사용자 수 API는 제거. DB 비밀번호·실제 계정 자료는 Git에 저장하지 않음.


## 2026-10-11 입력 및 오류 보완

- 임시 저장 시 제목·주최자는 비워둘 수 있습니다. 승인 요청 시에만 필수입니다.
- 신청 URL이 비어 있으면 null을 전송합니다. 조회수 공개 여부는 수정 화면에서 유지/선택합니다.
- 날짜만 입력하면 화면에 안내한 대로 행사는 00:00, 마감은 23:59로 저장됩니다.
- 처리 가능한 API 오류는 status/message/fields JSON을 제공합니다. 검증 오류는 fields에 항목별 메시지를 포함합니다. 업로드 크기 초과는 413, 수정 충돌은 409입니다. 보안 필터의 인증/권한 응답은 기존 401/403을 유지합니다.
