#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."

destination=${1:-simulator}
signing_options=(CODE_SIGNING_ALLOWED=NO)
case "$destination" in
  simulator) target='generic/platform=iOS Simulator' ;;
  device) target='generic/platform=iOS' ;;
  install)
    if [[ -z ${PLUGPASS_IOS_DEVICE_ID:-} ]]; then
      echo '설치할 기기의 UDID를 PLUGPASS_IOS_DEVICE_ID로 지정하세요. xcrun xctrace list devices로 확인하세요.' >&2
      exit 2
    fi
    target="id=$PLUGPASS_IOS_DEVICE_ID"
    signing_options=(CODE_SIGNING_ALLOWED=YES CODE_SIGN_STYLE=Automatic
      -allowProvisioningUpdates -allowProvisioningDeviceRegistration)
    if [[ -n ${PLUGPASS_IOS_TEAM_ID:-} ]]; then
      signing_options+=("DEVELOPMENT_TEAM=$PLUGPASS_IOS_TEAM_ID")
    fi
    ;;
  *) echo '사용법: bash scripts/build-ios.sh [simulator|device|install]' >&2; exit 2 ;;
esac

if ! xcode_version=$(xcodebuild -version 2>/dev/null); then
  echo 'Xcode 26 이상이 필요합니다. App Store에서 설치하고 첫 실행 설정을 완료하세요.' >&2
  echo 'Xcode > Settings > Locations > Command Line Tools에서 설치한 Xcode를 선택하세요.' >&2
  exit 1
fi
xcode_major=$(printf '%s\n' "$xcode_version" | sed -n 's/^Xcode \([0-9]*\).*/\1/p')
if [[ ! "$xcode_major" =~ ^[0-9]+$ ]] || (( xcode_major < 26 )); then
  echo 'Capacitor 8은 Xcode 26 이상이 필요합니다.' >&2
  exit 1
fi

npm --prefix frontend run build:native
(cd frontend && npx cap sync ios)
xcodebuild -project frontend/ios/App/App.xcodeproj -scheme App \
  -configuration Debug -destination "$target" \
  -derivedDataPath "build/native/ios-$destination" \
  "${signing_options[@]}" build

if [[ "$destination" == install ]]; then
  app='build/native/ios-install/Build/Products/Debug-iphoneos/App.app'
  codesign --verify --deep --strict "$app"
  xcrun devicectl device install app --device "$PLUGPASS_IOS_DEVICE_ID" "$app"
  xcrun devicectl device process launch --device "$PLUGPASS_IOS_DEVICE_ID" \
    --terminate-existing org.duckdns.plugpass.dev
  echo '기기 설치·실행 명령이 완료됐습니다. 실제 화면과 사용자 흐름은 iPhone에서 확인하세요.'
  exit 0
fi
echo '서명 없는 빌드입니다. iPhone 설치는 Apple 계정/Team 설정 후 npm --prefix frontend run install:ios로 진행하세요.'
