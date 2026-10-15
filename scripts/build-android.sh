#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."

android_java_home=${PLUGPASS_ANDROID_JAVA_HOME:-${JAVA_HOME:-}}
if [[ -z "$android_java_home" && -x /usr/libexec/java_home ]]; then
  android_java_home=$(/usr/libexec/java_home -v 21 2>/dev/null || true)
fi
if [[ ! -x "$android_java_home/bin/java" ]]; then
  echo 'PLUGPASS_ANDROID_JAVA_HOME을 JDK21 경로로 설정하세요. 백엔드 JDK25 설정은 변경하지 않습니다.' >&2
  exit 1
fi
if ! "$android_java_home/bin/java" -version 2>&1 | head -1 | grep -Eq 'version "21[.\"]'; then
  echo 'Android 빌드는 JDK21을 사용합니다. PLUGPASS_ANDROID_JAVA_HOME을 확인하세요.' >&2
  exit 1
fi
if [[ -z ${ANDROID_HOME:-} || ! -d "$ANDROID_HOME/platforms/android-36" ]]; then
  echo 'ANDROID_HOME에 Android SDK 경로를 설정하고 platforms;android-36, build-tools;35.0.0을 설치하세요.' >&2
  exit 1
fi

npm --prefix frontend run build:native
(cd frontend && npx cap sync android)
(cd frontend/android && JAVA_HOME="$android_java_home" ./gradlew --no-daemon assembleDebug lintDebug --console=plain)
echo 'APK: frontend/android/app/build/outputs/apk/debug/app-debug.apk'
