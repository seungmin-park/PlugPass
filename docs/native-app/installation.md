# iPhone부터 설치하기

현재 구현/검증 상태는 [tasks.md](tasks.md)를 먼저 확인한다. 프로젝트 생성, 시뮬레이터 빌드, iPhone 설치, 실제 지도·GPS 확인은 서로 다른 단계다. N01 앱 기반에는 아직 native 지도·GPS·Preferences·운영 API 연결이 없다.

## Swift와 Xcode의 차이

```text
Vue 화면 + Swift native 코드
       ↓ Xcode의 iOS SDK로 빌드
       ↓ Apple Personal Team으로 서명
       ↓ 연결한 iPhone에 설치
```

Swift는 언어이고 Xcode는 iOS SDK·빌드·서명·설치 도구다. Mac용 Command Line Tools나 Swift 컴파일러만 설치해도 iPhone SDK가 생기는 것은 아니다. Capacitor 8은 Xcode 26 이상, iOS 15 이상을 요구한다. 현재 앱은 Swift/SPM 프로젝트이며 CocoaPods 설치는 필요 없다. [Capacitor iOS](https://capacitorjs.com/docs/ios), [8 전환 안내](https://capacitorjs.com/docs/updating/8-0).

## 1. Mac 준비 (사용자 조작)

1. Mac의 **App Store**에서 **Xcode**를 검색해 설치한다.
2. Xcode를 한 번 실행한다. 약관·관리자 인증은 직접 처리한다. 추가 플랫폼 선택이 나오면 iOS 개발에 필요한 구성요소를 설치한다.
3. **Xcode → Settings → Locations → Command Line Tools**에서 설치한 Xcode를 선택한다.
4. **Xcode → Settings → Accounts**에서 Apple 계정을 추가한다. 비밀번호·인증 코드를 채팅이나 저장소에 넣지 않는다. 무료 Personal Team으로 직접 설치할 수 있고 유료 Developer Program 가입은 이번 목표에 필요 없다.

터미널에서 `xcodebuild -version`이 실제 Xcode 버전을 보여야 한다. `/Library/Developer/CommandLineTools` 관련 오류면 2~3단계를 확인한다. [Apple 무료 계정 범위](https://developer.apple.com/help/account/basics/about-your-developer-account).

## 2. 앱 빌드와 프로젝트 열기

저장소의 Node26.7.0/npm11 환경에서 실행한다. 작업 중에는 현재 [tasks.md](tasks.md)의 worktree 경로를 사용한다.

```bash
npm --prefix frontend ci
npm --prefix frontend run sync:native
npm --prefix frontend run open:ios
```

열리는 프로젝트는 `frontend/ios/App/App.xcodeproj`다. 현재 작업본은 `/private/tmp/plugpass-native-app/frontend/ios/App/App.xcodeproj`다. Finder에서 `⌘⇧G`로 이 파일이 있는 폴더 `/private/tmp/plugpass-native-app/frontend/ios/App`로 이동해 `App.xcodeproj`를 열어도 된다. HTML/JS/CSS는 `frontend/dist-native`에서 앱 안으로 복사한다. 운영 웹사이트를 원격으로 여는 `server.url`은 사용하지 않는다. 프런트 코드를 수정했다면 다시 sync해야 설치 앱에도 반영된다.

서명 없이 컴파일만 확인할 때:

```bash
npm --prefix frontend run build:ios
npm --prefix frontend run build:ios:device
```

첫 명령은 simulator SDK, 둘째는 device SDK로 컴파일한다. 산출물은 `build/native/ios-simulator`, `build/native/ios-device`다. **서명하지 않은 device 빌드만으로 iPhone에 설치할 수 없다.** 실제 설치는 다음 단계에서 Xcode가 처리한다.

## 3. iPhone 연결·서명·설치 (사용자 조작)

1. iPhone을 케이블로 연결하고 잠금을 해제한다. Mac/iPhone의 **신뢰** 확인과 기기 암호 입력은 직접 처리한다.
2. Xcode 왼쪽 프로젝트 **App → TARGETS App → Signing & Capabilities**에서 **Automatically manage signing**을 켜고 **Team**에 자신의 **Personal Team**을 선택한다.
3. Bundle Identifier는 `org.duckdns.plugpass.dev`다. 다른 계정에서 이미 사용 중이라는 오류가 나오면 임의로 바꾸지 말고 작업 기록과 카카오 iOS 등록값을 함께 조정한다.
4. Xcode 상단 실행 대상에서 연결한 **실제 iPhone**을 선택한다. Simulator 선택과 구분한다.
5. 기기에서 개발자 모드가 필요하다고 나오면 **설정 → 개인정보 보호 및 보안 → 개발자 모드**를 켜고 안내대로 재시작/확인한다. 이 항목은 Xcode에 기기를 연결한 뒤 표시될 수 있다.
6. Xcode의 **Run(▶)**으로 빌드·설치한다. 개발자 신뢰 안내가 나오면 iPhone의 해당 설정 화면에서 직접 확인한다.
7. 앱이 열리면 기기 모델·iOS 버전·빌드 commit을 기록한다. 지도/위치/운영 검색 검증은 별도 체크리스트를 이어간다.

Signing의 Team 선택 등 개인 설정 변경과 `xcuserdata`, 인증서, provisioning profile은 커밋하지 않는다. 팀 ID·기기 식별자를 공개 로그에 남기지 않는다.

## 4. 무료 서명 만료 후 재설치

무료 Personal Team provisioning profile은 발급 후 7일에 만료된다. 같은 Mac·Apple 계정으로 프로젝트를 열고 iPhone을 연결해 **동일한 Bundle ID와 Team**으로 다시 Run한다. 앱을 먼저 삭제할 필요는 없다. 앱을 삭제하면 앱 내부에 저장한 차량 설정이 사라질 수 있다. 최신 소스라면 2단계의 build/sync부터 다시 실행한다. 무료 계정의 앱/기기 수 제한도 [Apple 안내](https://developer.apple.com/help/account/basics/about-your-developer-account)를 따른다.

## Android는 기기 확보 후

Android는 Java21·compile/target SDK36·AGP8.13 기본 Build Tools35.0.0을 사용한다. 백엔드 Java25를 바꾸지 않고 Android 명령 프로세스에만 JDK21 경로를 전달한다.

```bash
export PLUGPASS_ANDROID_JAVA_HOME=/설치한/JDK21/경로
export ANDROID_HOME=/설치한/Android/SDK/경로
npm --prefix frontend run build:android
```

APK는 `frontend/android/app/build/outputs/apk/debug/app-debug.apk`다. `adb install -r`로 자신의 개발 기기에 설치할 수 있다. 기기 승인·USB 디버깅은 직접 처리한다. 현재 사용자는 Android 기기가 없어 실기기 검증 대기다.

## 실패 지점별 의미

| 관찰 | 의미와 다음 조치 |
| --- | --- |
| Xcode 요구 안내 후 exit1 | 코드 컴파일 전 도구 부족. Xcode 설치/Command Line Tools 선택 |
| Signing/Team 오류 | 무료 계정·기기·Bundle ID/서명 설정 확인. 인증 우회 금지 |
| 설치 후 빈 화면 | 실제 설치 앱의 자산/JS 오류를 확인. 브라우저 통과로 대체하지 않음 |
| native 지도 인증 실패 | 카카오 native 키·Bundle ID 등록 확인. REST/JavaScript 키를 대신 넣지 않음 |
| API 조회 실패 | N03 API/CORS/운영 상태 확인. ATS나 TLS 검증을 끄지 않음 |
