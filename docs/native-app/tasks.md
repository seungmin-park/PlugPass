# Android·iOS 앱 상세 작업 추적

계획: [plan.md](plan.md). `[x]`는 해당 항목의 관측 근거가 있는 완료만 뜻한다. `진행`, `대기`, `미확인`을 전체 완료와 구분한다.

## 현재 진행

- 2026-10-15: N01 iOS 실기기 설치 관문 통과. 두 native 프로젝트·묶음 자산 생성 완료, iOS simulator/device unsigned 빌드 성공. iPhone16에 Personal Team 서명·설치·프로세스 실행 성공, 사용자가 검색 화면과 차량 선택 정상 확인. 지도/외부 앱은 후속 구현이며 미확인.
- 작업 위치: `/private/tmp/plugpass-native-app`, `feat/native-app-foundation`, 기준 `bb3f106`.
- cmux 검증: `workspace:1000090002`, `surface:1000090016`. 사용 중인 호출 터미널에는 실행 명령을 보내지 않는다.
- 사용자 최신 지시: **Android 폰은 현재 없음, iOS 먼저**. Android 실기기 설치/검증은 대기. Android SDK는 사용자 직접 약관 동의 후 설치 완료.
- 사용자 후속 지시: **브라우저는 headless 실행**. 이후 실행은 `CI=1`로 진행하고 명령/로그는 cmux pane에 유지한다. 첫 공용 verify는 지시 확인 시 이미 headed 종료 0이었다.
- Xcode27.0 설치를 확인했고 `DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer`로 전역 선택 변경 없이 simulator/device 모두 컴파일했다. 첫 환경 실패는 아래 기록에 보존한다.
- 사용자 조작은 [iPhone 설치 안내](installation.md) 3단계. **iPhone16/iOS27.0.1 연결·paired·개발자 모드 enabled 확인**, Apple 계정·Personal Team 선택 뒤 CLI 자동 서명/설치/실행 완료. 사용자 개인 Team 설정은 로컬 변경으로 유지하고 커밋하지 않는다.
- 앱 기반 [PR89](https://github.com/seungmin-park/PlugPass/pull/89), 서명 commit `0ed44a4`의 원격 web/backend·iOS·Android·필수 PlugPass verify 모두 성공. headless 설치 명령을 포함한 최종 공용 verify도 종료0. iOS 실기기 설치 관문을 통과했으므로 최종 변경 push 뒤 같은 head의 CI·병합을 확인한다.

## N00 계획·사전 조사

- [x] 기존 코드/패키지/지도/위치/API/차량 저장 경계 확인.
- [x] 별도 모바일 plan.md와 상세 tasks.md 작성.
- [x] 깨끗한 main 기준 worktree·브랜치 생성.
- [x] 호출 cmux 확인·전용 검증 pane 생성.
- [x] Capacitor8 요구·8.5.3 실제 의존성·기본 SPM·Apple Personal Team 공식 출처/안내 갱신. Kakao iOS SPM 시작 문서 확인; 실제 SDK 도입 검증은 N02.
- [x] 기존 프런트 단위 기준선 431건 통과. 변경 후 타입·lint도 통과.

## N01 앱 기반·설치 경로

- [x] N01.1 Red: 앱 outDir=dist·HTML /app/assets를 실제 assertion 실패 2건으로 확인, 웹 경로 1건 통과.
- [x] N01.2 Green: Capacitor core/cli/android/ios 8.5.3 고정, native 빌드 분리. 대상 3건·전체 434건·타입/lint 통과.
- [x] N01.3 Android Java 프로젝트 생성: 앱 ID·표시 이름·Java21·SDK36, 생성 명령 종료 0. APK 빌드와는 구분.
- [x] N01.4 iOS Swift/SPM 프로젝트 생성: 앱 ID·표시 이름, Personal Team 정보 없음. 생성 명령 종료 0. 이후 컴파일 결과는 N01.8a.
- [x] N01.5 build/sync·Android Java21 경계·iOS unsigned build 명령과 설치 안내 추가. 최초 도구 부족 exit1, 설치 후 성공은 N01.8a와 실행 기록으로 구분.
- [x] N01.6 iOS .app의 local index.html·상대 자산 3개가 dist-native와 바이트 일치, server.url 없음. 기존 웹 공용 회귀 통과. 앱 운영 API 주소 주입은 N03에서 별도 구현.
- [x] N01.7a Android assembleDebug/lintDebug 종료0·APK 생성. native 단위/실기기 테스트로 세지 않음.
- [ ] N01.7b SDK 기능의 native 단위 검증은 N02 구현에서 추가.
- [x] N01.8a iOS simulator/device unsigned 빌드 성공, Xcode27.0/iOS27 SDK.
- [x] N01.8b Personal Team 사용자 설정·자동 서명. 실제 embedded profile의 앱ID 일치·기기1대·7일 만료 확인.
- [x] N01.8c headless 설치 명령: 기기 명시·서명 빌드·서명 검사·설치·실행 순서. 외부 도구 실패 시 후속 실행 중단 6건 Red→전체7건 Green. 실제 설치는 N01.10.
- [ ] N01.9 Android 실기기 설치·공통 화면 확인.
- [x] N01.10a iPhone 실기기 설치·프로세스 실행: `IOS_DEVICE_INSTALL_EXIT=0`.
- [x] N01.10b iPhone 공통 화면과 차량 선택 조작 확인. 사용자 실기기 응답: “검색 화면과 차량 선택이 정상”. 자동 UI assertion으로 바꾸어 세지 않음.
- [x] N01.11a 변경 자체 리뷰·공용 verify·작은 PR. 개인 Team/IDE 변경은 커밋 대상에서 제외.
- [ ] N01.11b 최종 head의 CI·실제 병합 기록.

## N02 native 지도 (N01 이후)

- [ ] N02.1 Red: 비동기 지도 명령/준비·실패·선택·이동 이벤트·종료 후 이벤트 무시.
- [ ] N02.2 공통 StationMapSession 변경과 웹 adapter 회귀.
- [ ] N02.3 TypeScript Capacitor bridge: 입력/이벤트 검증·세션별 수명·오류/재시도.
- [ ] N02.4 Android Kakao SDK Java 구현: 인증·지도/마커·카메라/영역·수명.
- [ ] N02.5 iOS Kakao SDK Swift/SPM 구현: 인증·지도/마커·카메라/영역·수명.
- [ ] N02.6 지도 DOM/native 배치·잘림·스크롤·resize·키보드·페이지 이탈.
- [ ] N02.7 앱 지도 버튼을 native 사각형 바깥에 배치, 선택 상태는 Vue에 유지.
- [ ] N02.8 로딩/인증 실패와 재시도, 목록·상세 접근 유지.
- [ ] N02.9 카카오 Android 패키지/서명 해시·iOS Bundle ID 등록.
- [ ] N02.10 Android 실기기 지도/배경·이동/확대·마커·스크롤·상세/복귀.
- [ ] N02.11 iPhone 실기기 동일 흐름. **N03/N04 연결 전 관문.**
- [ ] N02.12 native 단위/빌드·전체 회귀·PR/CI/병합.

## N03 운영 API·주소 검색 (지도 관문 통과 후)

- [ ] N03.1 Red: 앱 HTTPS API 주소·취소·timeout·DTO 검증 유지.
- [ ] N03.2 Red: CORS 허용 2 origin/조회·preflight, 임의/null/origin·변경 메서드 거부.
- [ ] N03.3 서버 CORS 최소 구현·MVC 실제 HTTP 경계 검증.
- [ ] N03.4 앱 주소 검색은 REST, 웹 SDK 유지. 정상/빈/인증/429/timeout·응답 역전.
- [ ] N03.5 운영 REST 키 존재/설정 확인 (값 비노출).
- [ ] N03.6 서버 검증·배포, 운영 HTTP CORS·주소 검색 확인.
- [ ] N03.7 두 실기기 실제 주소 선택→지도 이동→실데이터 검색.
- [ ] N03.8 전체 회귀·PR/CI/병합.

## N04 위치·저장·길찾기·복귀

- [ ] N04.1 Red: 플랫폼 선택은 앱 시작 경계, 화면에는 플랫폼 분기 없음.
- [ ] N04.2 native GPS: 권한 확인/요청·10초·좌표 검증·권한 거부/서비스 꺼짐/timeout.
- [ ] N04.3 늦은 GPS·주소 응답이 현재 지도 선택을 덮지 않음.
- [ ] N04.4 Preferences 비동기 읽기 이후 자동 검색. 차량 규칙/형식 재사용.
- [ ] N04.5 저장·삭제 실패, 손상값, 재시작 보존, 웹 저장과 독립.
- [ ] N04.6 Android ACTION_VIEW + geo 목적지, 없음/실행 실패 처리.
- [ ] N04.7 iOS kakaomap scheme, 미설치/실패 시 웹·설치 버튼, 웹 타이머 미사용.
- [ ] N04.8 Android 뒤로: 패널→이전 화면→배경. listener 해제 검증.
- [ ] N04.9 복귀 시 선택·조건 보존, 복귀만으로 GPS/검색 재실행 금지.
- [ ] N04.10 두 기기 설치/미설치·권한/서비스/timeout·오프라인·복귀 실제 확인.
- [ ] N04.11 전체 회귀·PR/CI/병합.

## N05 전달·최종 판정

- [ ] N05.1 safe area·홈 표시줄·키보드·글자 확대·주요 44px 터치.
- [ ] N05.2 CI Android/iOS 빌드·단위 검사, 서명/비밀 제외.
- [ ] N05.3 Android APK/해시·iOS 프로젝트·직접 설치/7일 만료 재설치 문서.
- [ ] N05.4 기기 모델·OS·앱 commit·환경·동작별 assertion/관찰·한계 기록.
- [ ] N05.5 기존 웹 전체 단위/타입/lint/build·개발 E2E·JAR·MySQL 통과.
- [ ] N05.6 루트 tasks/기능 지도/verify 스킬에 실제 모바일 검증 연결.
- [ ] N05.7 최종 PR·같은 head CI·보호 규칙·실제 병합 확인.
- [ ] N05.8 두 실기기 전체 핵심 흐름과 재설치 절차 확인 후 전체 완료.

## 실행 기록

명령·로그·테스트 건수·실패 이유를 아래에 누적한다. 아직 실행하지 않은 항목은 통과로 추정하지 않는다.

| 단계 | 명령/관찰 | 결과와 한계 |
| --- | --- | --- |
| 시작 | `git status --short --branch` | clean main `bb3f106` |
| 환경 | `xcode-select -p`, `xcodebuild -version` | CommandLineTools만 있음. 전체 Xcode 필요, iOS 빌드 미실행 |
| 환경 | `command -v adb`, SDK 디렉터리 | Android SDK/adb 없음, 설치/빌드 미실행 |
| 환경 | Java/Node 확인 | 백엔드 Java25.0.4.1, Android용 Java21.0.7 사용 가능, Node26.7.0 |
| pane | `cmux identify --json`, `new-split ... --focus false` | 호출 workspace에 검증 surface 생성, 포커스 임의 이동 없음 |
| 기준선 | `npm --prefix frontend run test:unit` | 431건/42파일 통과, `build/verification/native-app/baseline-unit.log` |
| N01 Red | `npm --prefix frontend run test:unit -- tests/nativeBuild.spec.ts` | 2실패/1통과, 실제 outDir와 빌드 HTML 경로 assertion. `n01-build-red.log` |
| N01 Green | 동일 대상 명령 | 3건 통과, `n01-build-green.log` |
| N01 회귀 | 전체 unit/type-check/lint | 434건/43파일, 타입·lint 종료 0. `n01-unit.log`, `n01-type.log`, `n01-lint.log` |
| 생성 | `npm run build:native`, `npx cap add android`, `npx cap add ios` | 상대 자산 빌드·두 프로젝트 생성 종료 0. `n01-native-build.log`, `n01-projects.log`. 설치 완료 아님 |
| Android SDK | 공식 ARM CLI SHA256 확인 → SDK36/build-tools36/platform-tools 설치 | 사용자가 cmux 약관 화면에서 직접 y 입력. adb 연결 기기 0대. 이후 iOS 우선으로 전환 |
| iOS 빌드 시도 | `npm --prefix frontend run build:ios` | exit1, Xcode 전체 앱 없음. `n01-ios-build.log`. Swift/SPM 프로젝트 생성 성공과 컴파일 성공을 구분 |
| iOS 빌드 | `DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer npm --prefix frontend run build:ios` 및 `build:ios:device` | 각각 종료0·BUILD SUCCEEDED. `/private/tmp/plugpass-native-evidence/ios-build.log`, `ios-device-build.log`. 서명/설치 미실행 |
| iOS 묶음 검사 | 생성 App.app의 capacitor.config.json/index.html/실제 자산 비교 | 앱ID 일치·server.url 없음·HTML이 참조하는 상대 JS/CSS/SVG 3개가 dist-native와 바이트 일치. 실제 WebView 렌더링 증거는 아님 |
| Android 빌드 | `PLUGPASS_ANDROID_JAVA_HOME=<JDK21> ANDROID_HOME=<SDK> npm --prefix frontend run build:android` | 종료0·132 task·APK6.9MB. 로그 `/private/tmp/plugpass-native-evidence/android-build.log`, lint 보고서 `frontend/android/app/build/reports/lint-results-debug.html`. upstream Capacitor lint baseline은 그대로 사용 |
| APK 묶음 검사 | 실제 zip의 config·index.html 비교 | 앱ID 일치·server.url 없음·dist-native index와 바이트 일치. SHA256 `046f267c7efa3c3d25bc851957682c51b9131871f61a549f8647dd46fe2cc862`. 기기 실행 미확인 |
| 공용 회귀 | `KAKAO_JS_KEY=test-js-key bash scripts/verify.sh` | cmux 종료 0, frontend434·개발 브라우저57·JAR5·MySQL8·재시작 뒤 브라우저7. 실제 카카오 인증은 mock 범위 밖. 원본 `/private/tmp/plugpass-native-evidence/verify.log` |
| 결과 집계 | JUnit XML 직접 집계 | Java462·MySQL8, 실패/오류/skip 모두0. 프런트434건, Python35건 |
| 기기 연결 | Xcode devicectl list devices | iPhone16/iOS27.0.1, paired, developerMode enabled. 앱 설치/렌더링 관찰과는 구분 |
| headless 공용 회귀 | `CI=1 KAKAO_JS_KEY=test-js-key bash scripts/verify.sh` | cmux `N01_HEADLESS_VERIFY_EXIT=0`. 전체 기존 assertion 유지, `/private/tmp/plugpass-native-evidence/verify-headless.log` |
| 원격 CI | PR89, head `0ed44a4`, run38744592556 | web/backend·iOS·Android·필수 PlugPass verify 4개 성공. 실제 설치 검증 아님 |
| 서명 시도 | `xcodebuild ... -allowProvisioningUpdates build` | `Signing for App requires a development team`, BUILD FAILED. 인증을 우회하지 않음. `signing-probe.log` |
| 설치 명령 Red | `python3 -m unittest discover -s scripts -p test_ios_install.py -v` | install 모드 누락 때문에 assertion 6실패, 기존 unsigned 동작1통과. `ios-install-red.log` |
| 설치 명령 Green | 동일 대상 명령 | 7건 통과. 외부 Apple/device 도구를 경계에서 대체한 CLI 순서·실패 전파 검증, 실기기 설치 성공과 구분. `ios-install-green.log` |
| 설치 명령 전체 | `python3 -m unittest discover -s scripts -p 'test_*.py' -v` | 42건 통과. `ios-install-python-all.log` |
| iPhone 설치 | `PLUGPASS_IOS_DEVICE_ID=<UDID> PLUGPASS_IOS_TEAM_ID=<Team> npm --prefix frontend run install:ios` | 서명 BUILD SUCCEEDED → codesign 성공 → 설치 → Launched application. cmux `IOS_DEVICE_INSTALL_EXIT=0`. 개인정보를 포함한 원본 로그는 로컬 `ios-install-device.log`에만 보관 |
| 실제 서명 | 앱 embedded.mobileprovision 읽기 | 앱ID 일치·기기1대·생성 2026-10-15 13:55:13 UTC, 만료 2026-10-22 13:55:13 UTC(한국 22:55:13). 공개 가능한 요약만 `ios-profile-summary.json`에 기록 |
| iPhone 화면 | 사용자가 설치된 앱의 검색 화면·차량 선택 직접 조작 | 정상 응답. 앱에 묶인 공통 화면 관찰이며 지도/GPS/운영 API 성공 증거는 아님 |
| 최종 공용 회귀 | `CI=1 KAKAO_JS_KEY=test-js-key bash scripts/verify.sh` | cmux `N01_INSTALL_VERIFY_EXIT=0`. Java462·MySQL8·frontend434·Python42, 개발 브라우저57·JAR5·MySQL 재시작 후 브라우저7. 실패/오류/skip0. `verify-install-headless.log` |

초기 N01 로그는 공용 verify의 clean 전에 `/private/tmp/plugpass-native-evidence/n01/`에 보존했다.

## 검토·판단

- 상태 소유: Vue(Store/컴포넌트)는 선택/검색, native adapter는 기기 자원. 테스트로 계약을 먼저 고정한다.
- API DTO·DB schema 변경 예정 없음. 저장 규칙·웹 동작 유지 여부는 회귀로 확인한다.
- 순차 구현을 유지하고 별도 에이전트 병렬화는 실행하지 않는다.
- 환경 부족을 테스트 실패나 기능 Red로 포장하지 않는다. 실기기 관문이 남으면 전체 완료 표시를 하지 않는다.
- 기본 생성기의 `2+2=4`와 예제 패키지명 검사는 제거했다. 이를 native 업무 단위 테스트로 셀 수 없기 때문이다. 현재 native CI는 컴파일/lint이며 SDK 기능 단위·실기기 검증은 후속이다.
- `npm audit`는 CLI 개발 의존성의 xcode→uuid 경로에 moderate 3개를 보고했다(운영 Vue 의존성 아님). 강제 하향/추측성 override는 적용하지 않았다. N02 이후 SDK 도입 시 재검토한다. 원본 `n01/npm-audit.json`.
- N01 CI 컴파일을 먼저 연결한다. 필수 PlugPass verify는 web/ios/android의 성공을 모두 요구한다. native 기능 테스트는 구현 시 추가하며 compile 성공을 실기기 통과로 바꾸지 않는다.
- Red→Green: native 모드의 outDir/HTML 경로 실제 실패 후 Vite의 mode 기반 설정만 변경했다. Refactor는 기존 Vitest가 같은 Vite 설정 함수를 호출하도록 조정한 범위다. 화면·상태·API DTO·DB는 변경하지 않았다.
- headless 설치: 기존 build-ios 명령에 install 모드를 추가했다. 서명 없는 빌드와 산출물 폴더를 분리해 이전 unsigned 앱을 설치하지 않고, 명시한 기기로만 설치한다. 외부 도구 실패는 원래 종료 코드를 보존한다. 별도 설치 프레임워크/전역 Xcode 설정 변경은 불필요해 도입하지 않았다.
- 이름/책임 검토: Vite는 자산 경로/산출물, Capacitor 설정은 앱ID/묶음 위치, build-ios/build-android는 도구 경계와 빌드를 소유한다. 추측성 상태 관리/플랫폼 인터페이스는 아직 추가하지 않았다. 기존 웹 공개 경로는 공용 회귀로 확인했다.
