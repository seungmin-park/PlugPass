# PlugPass 웹앱

Node26.7.0을 사용한다. `npm ci` 후 `npm run dev`로 실행하고 `/app/index.html`을 연다.
개발 서버는127.0.0.1:5173이며 `/api`를 Spring의127.0.0.1:8080으로 전달한다.

- `npm run test:unit`: 단위·컴포넌트 assertion
- `npm run type-check`: Vue/TypeScript 타입
- `npm run lint`: ESLint, 경고0
- `npm run build`: `/app/` base의 dist 산출물

저장소 공용 명령은 `bash scripts/verify.sh`다. 현재 T18은 검색 조건 폼·hash 이동·검증 기반이며, 실제 검색 화면 연결은 T20 이후다. 운영JAR 웹앱 패키징은 T24에서 검증한다.
