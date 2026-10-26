# 기존 Git 저장소에 최종본 적용하기

이번 ZIP은 검사 보고서가 아니라 수정·빌드된 전체 프로젝트입니다. **기존 Git 저장소를 계속 사용합니다.** 파일을 하나씩 수동으로 옮길 필요는 없습니다.

## 적용 순서 — Windows

1. 실행 중인 서버와 프론트 개발 서버를 Ctrl+C로 종료합니다. 기존 작업은 먼저 커밋합니다.
2. 이 ZIP을 기존 저장소와 다른 폴더에 전체 압축 해제합니다. 예: `C:\Downloads\brain-bucket-final\brain-bucket`.
3. 압축을 푼 `brain-bucket` 폴더에서 PowerShell을 엽니다. 아래 Target을 **기존 .git이 들어 있는 저장소 최상위 경로**로 바꿔 한 번 실행합니다.

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File ".\scripts\apply-final.ps1" -Target "C:\Users\USER\OneDrive\문서\GitHub\brain-bucket"
```

스크립트는 ZIP 파일 무결성을 먼저 검사하고, 덮어쓰거나 삭제할 파일을 기존 저장소 옆 `brain-bucket.before-final-날짜-임의값` 폴더에 백업합니다. 새 파일을 복사하고 예전 중복 파일 및 이전 빌드 자산을 제거합니다. 알 수 없는 로컬 파일은 삭제하지 않습니다. `.git`, `.env`, `uploads`, MySQL 데이터는 건드리지 않습니다. 이 백업은 소스 백업이며 DB 백업은 아닙니다.

4. 이제 **기존 저장소**에서 `2-start.cmd`를 실행합니다. 최신 JAR·화면을 포함하므로 적용 직후 다시 빌드할 필요는 없습니다. 기존 MySQL과 `.env`를 그대로 사용합니다.
5. `http://localhost:8080/#/board`에서 확인합니다. PORT를 바꿨다면 그 포트를 사용합니다. 오래된 화면이 보이면 Ctrl+F5로 새로고침합니다.
6. GitHub Desktop에서 기존 저장소의 변경을 확인하고 아래 Summary로 커밋한 뒤 Push origin 합니다.

```text
fix: 프론트·백엔드 구조 정리 및 승인 버전·DB 이력 호환성 수정
```

DB 재생성, 수동 SQL 실행, 비밀번호 변경은 이번 수정에 필요하지 않습니다. 모르는 Flyway 오류가 뜨면 이력을 임의로 지우지 말고 오류 원인을 확인하세요. 알려진 두 배포 이력은 자동으로 선택하지만 임의로 바뀐 스키마/체크섬까지 복구하는 기능은 아닙니다.

## 어떤 파일을 어디로 옮겼나

| 기존 위치 | 최종 처리 |
|---|---|
| frontend/backend/src/main/java/db/migration/V2__integrate_post_management.java | backend/src/main/java/db/migration/로 통합. 이전 성공 이력 호환용 원본 유지 |
| frontend/backend/src/main/resources/db/migration/V1__initial_schema.sql | backend/src/main/resources/db/legacy/로 이동. 이전 체크섬 검증용 |
| backend/src/main/resources/db/migration/V1__initial_schema.sql | backend/src/main/resources/db/current/로 이동. 새 DB 기본 생성용 |
| frontend/backend의 UserController.java | 루트 backend에 반영. 회원가입 유지, 개발용 사용자 수 API 제거 |
| frontend/backend의 pom.xml·설정·테스트 | 더 최신인 루트 backend 버전을 유지하고 호환/검토 회귀 테스트 추가 |
| frontend/backend의 static | 오래된 빌드이므로 제거. 최신 frontend를 빌드해 루트 backend/static에 배포 |
| frontend/compose.yaml·frontend/scripts/run.ps1 | 중복 삭제. 루트 compose.yaml·scripts/run.ps1 사용 |
| frontend/docs/database.md·integration.md·verification.md | 최신 루트 docs에 통합하고 중복 삭제 |
| frontend/PATCH-APPLY.md·중복 README | 오래된 적용 문구 제거/교체. 이 문서와 루트 README 사용 |

## 최종 역할 구분

- `frontend/src`, `frontend/public`, `frontend/tests`: 화면·HTTP 호출·프론트 테스트.
- `backend/src/main/java`: API·인증·권한·서비스·DB 엔티티·Java 마이그레이션.
- `backend/src/main/resources/db`: 서버가 실행하는 SQL 마이그레이션.
- `database`: 사람이 참고할 SQL 자료. 앱 시작 때 직접 실행하지 않습니다.
- `backend/src/main/resources/static`: frontend 소스에서 생성한 배포 화면. 직접 편집하지 않습니다. 한 서버로 배포하기 위한 정상 구조입니다.
- `runtime/brain-bucket.jar`: 최신 소스와 화면으로 빌드한 실행 파일.
- 루트 `scripts`, `compose.yaml`, `Dockerfile`, `docs`: 공통 실행·빌드·배포·문서.

`frontend/src/capture/api.ts`는 DB 구현이 아니라 화면의 HTTP 클라이언트이므로 프론트에 남습니다. 파일 위치만 바꾸면 되는 수정과 API 양쪽을 함께 수정해야 하는 작업을 구분해 반영했습니다. 별도로 첨부된 ReserveSetup/BoardService 계열 예비본은 ZIP의 MySQL 엔티티·API와 다른 구현이므로 최신 서버에 섞지 않았습니다.

검증 결과와 한계는 루트 `검증 결과.md`에 정리했습니다.
