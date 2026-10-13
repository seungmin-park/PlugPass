#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
: "${KAKAO_JS_KEY:?카카오 JavaScript 키가 필요합니다. 1Password 환경 또는 CI secret을 확인하세요.}"
export VITE_KAKAO_JS_KEY="$KAKAO_JS_KEY"
python3 -m unittest discover -s scripts -p 'test_*.py' -v
./gradlew frontendInstall --console=plain
if [[ ${CI:-} == true ]]; then
  node frontend/node_modules/playwright/cli.js install --with-deps chromium
fi
npm --prefix frontend run test:unit
npm --prefix frontend run type-check
npm --prefix frontend run lint
./gradlew clean build --console=plain
bash scripts/frontend-e2e.sh
python3 scripts/verify-browser.py
python3 scripts/verify-runtime.py
./gradlew performanceClasspath --console=plain
python3 scripts/verify-memory-restart.py
python3 scripts/verify-mysql.py
