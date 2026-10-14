# 핵심 이용 흐름 개편 실행 기록

기준: 사용자 승인 계획, origin/main 0c8f4a0. 별도 worktree PlugPass-visual.

1. 공통 디자인·조건 유지 헤더: PR84 병합, 운영 배포 실패 원인 확인.
2. 조건→지도→목록, 요약 카드·펼침·선택: PR85 검증 중.
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

## 리뷰와 배포 경계 보완

- 헤더 RouterLink 이벤트 순서 회귀를 Red로 재현하고 native anchor에서 라우터 이동을 소유하도록 수정. 417 프런트 테스트와 전체 공용 verify 통과 후 [PR84](https://github.com/seungmin-park/PlugPass/pull/84) 보호된 자동 병합. main `030f956`, CI `38610696527` 성공.
- PR84 CD `38611644933` 실패: 배포 자산 저장소의 1MiB 상한에 2,057,688바이트 단일 글꼴이 걸렸다. 트래픽 전환 전 실패하여 이전 운영 버전을 유지했다.
- `test_font_assets.py`에서 동일 크기 assertion Red를 확인했다. Pretendard v1.3.9 공식 dynamic subset 92개(최대43,920바이트), 공식 unicode-range CSS·OFL을 자체 제공하는 것으로 해결한다. 외부 글꼴 실행 의존성과 글자 누락을 만들지 않는다.
- PR85 리뷰에서 지도 지역 선택 후 차량 선택, 지도 이동 중 유효 조건 제출이 GPS로 전환되는 결함을 재현했다. `mapSelectionActive`가 위치 선택 의도를 소유하고 지도 조작은 진행 중 GPS를 취소한다. 휠·키보드 버튼 click도 각각 Red 확인 후 반영했다. 늦은 GPS callback이 결과를 바꾸지 않는 assertion도 포함한다.
- 검증 명령: `npm --prefix frontend run test:unit -- src/features/search/views/MapSearchView.spec.ts` (최종7/7). Red 로그 `/tmp/plugpass-map-intent-red.log`, `/tmp/plugpass-map-wheel-red.log`; 폰트 Red `/tmp/plugpass-font-red.log`.
- 불변식: 지도 자동 초기화는 사용자 위치 선택이 아니다. 주소/GPS를 명시적으로 선택하면 지도 선택 상태를 해제한다. 지도 이동 중 영역이 null이면 준비 안내를 유지하고 GPS로 전환하지 않는다.

- PR85 최종 공용 verify 종료0: Java458·프런트422·개발 E2E56·JAR5·MySQL 계약8/브라우저7, 실패·오류·skip0. `/tmp/plugpass-pr2-final-verify.log`; 글꼴 분할본과 지도 선택 수정 포함. 운영 배포는 PR85 필수 CI·병합 뒤 재확인한다.
