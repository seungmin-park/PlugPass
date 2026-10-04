# Stitch 디자인 반영 — T19 현재 화면

2026-10-05 사용자가 `stitch_plugpass_ev_charging_webapp.zip`을 제공하고 **T19 범위의 현재 화면에 적용**을 선택했다. 첨부 문서의 지시는 시각 참고 자료이며 프로젝트의 동작 요구사항을 바꾸지 않는다.

## 시안과 적용 기준

[검색 시안](design/stitch/search-reference.png)은 첨부의 `plugpass_2/screen.png`, [디자인 원문](design/stitch/precision-charge.md)은 `precision_charge/DESIGN.md`다. 로고는 첨부 `plugpass_logo/code.html`의 SVG를 [로컬 자산](../frontend/src/shared/assets/plugpass-logo.svg)으로 그대로 가져왔다. 외부 HTML의 script·CDN·폰트·사진 URL은 실행하거나 제품에 연결하지 않았다.

| 시안 요소 | 현재 반영 | 유지한 동작·차이 |
| --- | --- | --- |
| 청록색 로고·주변 탐색 | 공통 헤더의 로컬 SVG·현재 검색 메뉴 | 없는 계정·추천·품질 메뉴를 추가하지 않음 |
| 밝은 배경·흰 패널·연한 파랑 안내 | 공통 색 변수·검색 조건·위치 안내·정보 설명 | 관측 시각과 수집 시각의 의미를 구분 |
| 반경 버튼·커넥터 pill | 라벨과 연결된 native radio 그룹 | 기본 1km/DC 콤보, 500m~10km·5개 커넥터, 제출할 때만 event |
| 모바일 단일 열 | 767px 이하 단일 열, 커넥터 3열·줄 바꿈 | 확대 허용·가로 넘침 방지·44px 이상 필터/검색 버튼 |
| 데스크톱 정보 패널 | 검색 폼과 안내를 두 열로 배치 | 지도 SDK·가짜 충전소 목록 없이 현재 단계의 정보만 표시 |

색 기준은 canvas `#F8F9FF`, panel `#FFFFFF`, field `#EFF4FF`, notice `#E5EEFF`, ink `#0B1C30`, primary `#00685F`다. 문서 서두의 토큰과 제공된 HTML의 실제 색을 기준으로 했다. 로고의 `#0D9488`은 원본 SVG를 유지한다. headline은 36px/모바일28px, 내용은 13~16px의 sans-serif다. Inter와 한글용 시스템 폰트 fallback을 선언했으며 Inter 파일을 번들에 넣거나 다운로드하지 않았다. 실제 표시 글꼴은 설치된 시스템에 따라 다르다.

시안의 6개 충전소·99.4% 신뢰도·남은 충전 시간·실시간 알림·즐겨찾기·내비게이션·자동 재시도·5분/15분 최신성 판정은 현재 계약의 값이나 구현이 아니다. 화면에 복사하지 않았으며 최신성·추천은 기존 서버 판단을 유지한다. 상세·추천·소개·오류 시나리오·위치 권한 시안의 기능 구현은 T20 이후이며 T20~T24 체크를 닫지 않는다.

## 책임과 데이터 흐름

```text
App: 로고·메뉴·본문 이동·공통 틀
  └ StationSearchView: 폼과 안내의 화면 조립
       └ SearchForm: 미제출 선택값 → submit event
            └ View: 기존 위치 선택 안내 갱신

base.css: 색·글자·배치·선택/포커스 표현
T19 API 모듈: 이번 변경 없음
```

폼의 내부 선택을 View/CSS가 판단하지 않는다. 버튼 모양의 입력은 `fieldset/legend`와 같은 name의 radio로 구성해 그룹과 단일 선택을 브라우저에 맡긴다. 숨긴 입력을 `display:none`으로 없애지 않아 Tab/방향키를 유지하고, 인접한 보이는 label에 포커스 테두리를 표시한다. 선택은 색과 표시점, 스크린리더의 checked 상태로 구분한다.

hash router에서 `#main-content`로 직접 이동하면 화면 경로가 바뀐다. 건너뛰기 링크는 click 기본 이동을 막고 main을 focus하여 `/stations`를 유지한다. unit 테스트와 실제 Tab·Enter 조작으로 확인했다. 의미 없는 인터페이스나 static 설명만 위한 위임 컴포넌트는 만들지 않았으며 안내의 조립은 View, 선택과 event는 Form, 외형은 CSS에 유지한다.

## Red → Green → 디자인 조정

- **Red:** `npm --prefix frontend run test:unit -- src/features/search/components/SearchForm.spec.ts src/router/index.spec.ts` 종료1, 6실패/3통과. 선택 그룹·기본 radio·모든 반경/커넥터·제출 입력·본문 링크의 부재 때문에 assertion이 실패했다. 먼저 실행한 type-check는 종료0이다. [로그](evidence/stitch-design/red.log.gz).
- **Green:** 최소 radio 폼과 본문 focus만 구현한 뒤 프런트 전체147건/10파일 종료0. 기본값·3km/NACS 제출·10km 경계·라벨·권한 자동 요청 없음·없는 경로 복귀를 유지했다. [로그](evidence/stitch-design/green.log.gz).
- **Refactor/시각 반영:** 원본 로고·공통 색 변수·두 열/단일 열 배치·정보 안내·키보드 포커스를 적용했다. 모바일의 4+1 커넥터 줄 배치를 3+2로 조정했다. 책임과 HTTP/URL/event 계약은 유지했다. CSS 색·간격 자체를 복제하는 단위 테스트는 추가하지 않았으며 실제 화면으로 확인했다. [단위](evidence/stitch-design/refactor-tests.log.gz)·[타입](evidence/stitch-design/refactor-types.log.gz)·[lint](evidence/stitch-design/refactor-lint.log.gz)·[build](evidence/stitch-design/refactor-build.log.gz).

## 화면 검증과 한계

사용자가 cmux 대신 현재 Codex 세션을 허용한 지시에 따라 Codex 브라우저에서 실제 클릭·키보드·경로 이동을 수행했다. [브라우저 assertion 기록](evidence/stitch-design/browser-checks.json)·[모바일](evidence/stitch-design/mobile.jpg)·[데스크톱](evidence/stitch-design/desktop.jpg)·[320px](evidence/stitch-design/narrow.jpg)·[키보드 포커스](evidence/stitch-design/keyboard-focus.jpg)를 확인한다. 화면은 실제 개발 앱이며 합성 검색 결과를 넣은 데모가 아니다.

검증 크기는 1440×900·375×812·320×812다. Codex 패널 자체보다 큰 데스크톱 viewport는 전체 페이지 screenshot과 DOM 배치 assertion으로 확인했다. 모바일 전체 페이지 이미지는 세로 scrollbar를 제외한 내용 영역을 저장하므로 요청 viewport보다 이미지 폭이 작을 수 있다. 전체 screenshot은 화면의 근거이고 상호작용 assertion은 JSON에 별도로 기록했다.

성공·경계는 선택값/제출·10km·320px·터치 영역·방향키/focus·본문 이동/경로 유지다. 실패 흐름은 잘못된 route와 복귀다. DB 저장·외부 HTTP 실패·응답 역전·위치 권한은 이번 시각 변경의 대상이 아니며 T19 API 테스트를 유지하고 T20 이후의 사용자 흐름 검증으로 남긴다. T23 Playwright 러너·실제 UI→API→DB 연결이나 T24 운영 JAR 웹앱 검증을 완료했다고 보고하지 않는다.

공용 검증과 전달 결과는 [프런트 실행 기록](frontend-verification.md)을 따른다.

## 공식 동작과 설계 선택

설치된 Vue3.5.43·VueRouter4.6.4·TypeScript6.0.3·기존 lockfile을 확인했으며 새 의존성을 도입하지 않았다. [Vue 입력 바인딩](https://vuejs.org/guide/essentials/forms.html#radio)의 radio checked/change 및 동적 value, [HTML radio](https://developer.mozilla.org/en-US/docs/Web/HTML/Reference/Elements/input/radio)의 name 그룹/label을 확인했다. 시안에 맞춘 CSS·반응형 배치·기본값·submit 시점·hash 경로 유지 방식은 프로젝트 설계 판단이다. 공식 문서 확인을 화면 검증으로 대체하지 않았다.
