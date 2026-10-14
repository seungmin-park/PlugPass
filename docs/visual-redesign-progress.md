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

## 2. 모바일 검색
- Red: StationCard/StationSearchView 대상 2건의 assertion 실패 확인.
- Green: 프런트 417건. 지도 SDK 준비 전 로딩과 검색 로딩을 혼동한 공용 테스트를 실제 SDK fake 준비를 기다리도록 보완.
- DOM 순서를 조건→지도→목록으로 변경. 지도 접기·목록 max-height/overflow 제거. 상태 요약과 native button 펼침을 추가. 핀20px, 터치44px 유지.
- headed 개발 E2E에서 버튼 겹침을 재현하고 재검색을 왼쪽 아래로 이동. 320/375/430/1440px 첫 제목 노출·순서·가로 넘침·스크롤 검사 통과. 지도 최소260px은 이전 디자인 제약으로 새170px 지도·첫 결과 노출 계약으로 교체.
- cmux 직접 카드 펼침 확인. 러너의 지도 SDK mock 화면과 실제 cmux SDK 실패 화면을 구분: 직접 브라우저에서 목록·상세 접근은 가능했고 실제 지도는 아직 미확인.
- README 전면 개편은 사용자 추가 요청으로 3단계에 포함.
