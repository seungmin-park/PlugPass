# T53 모바일 길찾기 외부 앱 연결

사용자 기대: 검색 카드의 `길찾기`를 한 번 누르면 웹 내부 앱 선택 패널을 거치지 않고 기기의 지도 앱으로 목적지를 전달한다. iPhone은 카카오맵을 기본으로 열고 미설치 시 앱스토어 대안을 제공한다. Android는 기기가 처리 가능한 지도 앱을 고르게 한다.

## 플랫폼 경계

- Android: `geo:` 공통 위치 요청을 패키지 지정 없는 Chrome `intent:` 링크로 전달한다. 처리 앱이 없을 때의 브라우저 fallback은 카카오맵 Play 스토어다. 운영체제에 이미 기본 앱이 있으면 곧바로 열릴 수 있고, 설치된 앱이 둘 이상이어도 특정 앱의 `geo:` 지원 여부에 따라 선택 목록은 달라진다. 웹은 Android 네이티브 `Intent.createChooser()`를 호출하거나 ‘한 번만/항상’ 창을 강제로 띄울 수 없다. 공통 `geo:`는 목적지 위치 전달이며 앱별 자동차 경로 화면 시작은 해당 앱의 처리에 맡긴다.
- iOS: 공식 `kakaomap://route?ep=...&by=car`를 사용자 클릭에서 연다. 앱 전환으로 페이지가 숨겨지지 않으면 2.5초 뒤 카카오맵 앱스토어로 이동을 시도한다. 웹에서는 앱 설치 여부를 확정적으로 조회할 수 없으므로 설치/미설치·Safari 확인창·앱 내 브라우저의 실제 결과는 iPhone에서 재검증해야 한다. 상세와 지도 마커 패널에는 설치·웹 지도 대안을 남긴다.
- PC: 기존 웹 지도를 새 탭으로 여는 경로와 선택 패널을 유지한다.

근거: [Android 지도 공통 인텐트](https://developer.android.com/guide/components/intents-common#Maps), [Chrome의 웹 인텐트·fallback](https://developer.chrome.com/docs/android/intents), [Android 네이티브 chooser](https://developer.android.com/training/basics/intents/sending), [카카오맵의 Android `geo:` 지원](https://apis.map.kakao.com/android_v2/docs/api-guide/urlscheme/), [카카오맵 iOS URL Scheme](https://apis.map.kakao.com/ios_v2/docs/getting-started/urlscheme/). 카카오 공식 주소의 iOS Safari 미설치 동작에 관한 [카카오 개발자 문의](https://devtalk.kakao.com/t/url-scheme/149792)도 확인했다.

## 책임과 검증

`navigationLinks`가 목적지 검증·플랫폼별 URL 생성, `navigationHandoff`가 iOS 대체 이동의 취소 조건을 소유한다. 검색 카드·상세·마커 패널은 동일한 링크를 보여 주고 사용자 클릭을 전달한다. 서버 API·저장 계약은 바꾸지 않는다.

Red: Android 공통 링크·iPhone 직접 링크가 없다는 단위 assertion 실패, 검색 카드가 링크 대신 선택 버튼인 컴포넌트 assertion 실패, iOS 앱 미전환 대체 동작 assertion 실패를 확인했다. Android `intent://`가 `geo://`로 해석될 수 있는 문제도 URL assertion 실패로 재현해 `intent:` 형식으로 수정했다. Green: 대상 단위 39건, headless Chromium의 iPhone·Android·PC 흐름 3건 통과. 브라우저 테스트는 목적지 URL과 내부 패널 미표시를 검증하며 실제 OS 선택창·앱 실행을 성공이라고 주장하지 않는다.

최종 `CI=1 KAKAO_JS_KEY=test-js-key bash scripts/verify.sh`는 두 기능이 함께 있는 최신 main 기반 소스에서 현재 cmux 보조 pane에 표시하며 실행했고 종료 코드 0이었다. 프런트 단위 414건, 개발 Chromium 57건, 배포 JAR Chromium 5건, MySQL 8건과 재시작 뒤 Chromium 7건이 통과했다. 로그는 `/tmp/plugpass-navigation-verify-final2.log`다.

[PR #82](https://github.com/seungmin-park/PlugPass/pull/82)의 CI와 [main CI](https://github.com/seungmin-park/PlugPass/actions/runs/38553895806)가 성공했고, 보호 규칙 아래 자동 병합된 `c8fcbfb`의 [운영 배포](https://github.com/seungmin-park/PlugPass/actions/runs/38554572680)도 성공했다. 배포 후 시흥 좌표의 실제 운영 검색 결과 9개를 headless Chromium에서 iPhone·Android 사용자 환경으로 열어 첫 카드가 각각 `kakaomap://route?ep=...&by=car`, `intent:0,0?q=...#Intent;scheme=geo;...` 링크를 가지며 PlugPass 선택 창이 열리지 않음을 확인했다. cmux 검증 pane 종료 코드 0, 로그 `/tmp/plugpass-live-t53.log`. URL과 웹 동작 검증이며 실제 iPhone·Android에서 앱 설치 여부, 앱 전환, 마켓 이동, 시스템 선택 창의 모양은 아직 검증하지 않았다.
