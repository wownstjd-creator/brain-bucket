# 짜보 프론트엔드

이 폴더는 React·TypeScript 화면과 프론트 테스트만 관리합니다. DB 스키마·Flyway·Java API·서버 설정은 루트 backend에 있습니다. 예전 frontend/backend, frontend/scripts, frontend/compose.yaml은 제거했습니다.

루트 backend를 실행한 상태에서 이 폴더에서 다음 명령을 실행합니다(Node 22.18 이상).

```powershell
npm ci
npm test
npm run dev
```

개발 화면: http://localhost:5175. /api 요청은 Vite가 http://localhost:8080으로 전달합니다. localhost와 127.0.0.1을 섞지 마세요.

화면·API 연결 코드는 src, 정적 샘플 이미지는 public, 테스트는 tests에서 관리합니다. DB 접근 코드는 이곳에 넣지 않습니다. `src/capture/api.ts`는 HTTP 클라이언트이며 DB 구현이 아닙니다.

화면 수정 후 저장소 최상위에서 `powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\scripts\build.ps1`을 실행하면 프론트 테스트·빌드, backend static 복사, 백엔드 테스트·JAR 빌드와 runtime 갱신을 수행합니다. 개발 소스와 갱신된 static/runtime을 함께 커밋하세요. 전체 실행·DB·배포 안내는 루트 README.md와 docs를 참고하세요.
