#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."

destination=${1:-simulator}
case "$destination" in
  simulator) target='generic/platform=iOS Simulator' ;;
  device) target='generic/platform=iOS' ;;
  *) echo '사용법: bash scripts/build-ios.sh [simulator|device]' >&2; exit 2 ;;
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
  CODE_SIGNING_ALLOWED=NO build
echo '서명 없는 빌드입니다. iPhone 설치는 Xcode에서 Personal Team과 기기를 선택하고 Run하세요.'
