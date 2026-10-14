# 핵심 이용 흐름 개편 실행 기록

기준: 사용자 승인 계획, origin/main 0c8f4a0. 별도 worktree PlugPass-visual.

1. 공통 디자인·조건 유지 헤더: 진행 중.
2. 조건→지도→목록, 요약 카드·펼침·선택: 대기.
3. 상세 수집 계약·규격·시각·제한 안내: 대기.

공유 경계: 1의 공통 토큰은 2·3 화면에 적용. 2의 카드 요약은 3의 상태 의미를 유지해야 함.
개별 수신 시각으로 지연을 계산하지 않고 서버 collection을 사용한다.
cmux workspace:1000070003 / 검증 surface:1000070011.

## 1. 공통 디자인
- Red: `npm --prefix frontend run test:unit -- src/router/index.spec.ts`, 2 assertion 실패(메뉴명/조건 펼침).
- Green: 전체 Vitest 416/416, type-check/lint 통과. 기존 baseline 414/414.
- 헤더는 검증된 URL query를 링크에 전달하고 검색 화면에서는 native details/summary를 열어 focus. Ctrl/Cmd 등의 기본 링크 동작 유지.
- Pretendard Variable v1.3.9 원본과 OFL을 로컬 asset으로 제공. 일반 본문과 배경 대비를 계산하며 색 외 문구도 유지.
- cmux에서 실제 검색 결과·헤더 클릭 확인. 공용 verify는 KAKAO_JS_KEY 주입 정보 확인 중.
