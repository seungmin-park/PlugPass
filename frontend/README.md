# PlugPass 웹앱

Node26.7.0을 사용한다. `npm ci` 후 `npm run dev`로 실행하고 `/app/index.html`을 연다.
개발 서버는127.0.0.1:5173이며 `/api`를 Spring의127.0.0.1:8080으로 전달한다.

- `npm run test:unit`: 단위·컴포넌트 assertion
- `npm run type-check`: Vue/TypeScript 타입
- `npm run lint`: ESLint, 경고0
- `npm run build`: 타입 검사 성공 후 `/app/` base의 dist 산출물

저장소 공용 명령은 `bash scripts/verify.sh`다. 검색→상세→같은 조건의 대체 후보→다른 상세를 사용할 수 있다. 상세 직접 링크는 조건 없이도 조회하며, 대체 후보로 이동하기 전 검색 조건을 선택한다. 후보와 수집 상태는 독립적으로 재시도한다. 운영JAR 웹앱 패키징은 T24에서 검증한다.

의존성 재설치나 프런트 설정이 다른 브랜치로 전환한 뒤에는 켜 둔 개발 서버를 종료하고 `npm run dev`로 다시 시작한다. 최종 검증은 새 서버의 실제 화면으로 확인한다.
