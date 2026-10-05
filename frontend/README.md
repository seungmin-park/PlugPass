# PlugPass 웹앱

Node26.7.0을 사용한다. `npm ci` 후 `npm run dev`로 실행하고 `/app/index.html`을 연다.
개발 서버는127.0.0.1:5173이며 `/api`를 Spring의127.0.0.1:8080으로 전달한다.

- `npm run test:unit`: 단위·컴포넌트 assertion
- `npm run type-check`: Vue/TypeScript 타입
- `npm run lint`: ESLint, 경고0
- `npm run build`: 타입 검사 성공 후 `/app/` base의 dist 산출물

저장소 공용 명령은 `bash scripts/verify.sh`다. 검색→상세→같은 조건의 대체 후보→다른 상세를 사용할 수 있다. 상세 직접 링크는 조건 없이도 조회하며, 대체 후보로 이동하기 전 검색 조건을 선택한다. 후보와 수집 상태는 독립적으로 재시도한다. 운영JAR 웹앱 패키징은 T24에서 검증한다.

의존성 재설치나 프런트 설정이 다른 브랜치로 전환한 뒤에는 켜 둔 개발 서버를 종료하고 `npm run dev`로 다시 시작한다. 최종 검증은 새 서버의 실제 화면으로 확인한다.

T23 브라우저 검증은 저장소 루트에서 `bash scripts/frontend-e2e.sh`로 실행한다.
Playwright1.63.0의 Chromium이 필요하며 최초 설치는 `cd frontend && npx playwright install chromium`이다.
로컬은 headed, CI는 headless다. 실제 합성 공급자 HTTP→수집→H2와 Vue 개발 서버를 시작하고 종료 시 자신이 만든 프로세스만 정리한다.
기본 포트는 백엔드18180·프런트5183이며 `PLUGPASS_E2E_BACKEND_PORT`·`PLUGPASS_E2E_FRONTEND_PORT`로 변경한다.
`--serve`는 실제 cmux 클릭 검증용으로 서버를 유지한다. `npm run test:e2e` 단독 실행은 준비된 서버가 필요하다.
서버 로그는 `build/verification/frontend-e2e/`, JSON/trace는 `frontend/test-results/`, HTML은 `frontend/playwright-report/`다.
실패 화면 spec의 HTTP/위치 응답 제어와 실제 DB 연결 journey를 구분한다. 실공공 API 인증 검증을 대신하지 않는다.
