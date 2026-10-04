#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
python3 -m unittest discover -s scripts -p 'test_*.py' -v
npm --prefix frontend ci
npm --prefix frontend run test:unit
npm --prefix frontend run type-check
npm --prefix frontend run lint
npm --prefix frontend run build
./gradlew clean build --console=plain
python3 scripts/verify-runtime.py
./gradlew performanceClasspath --console=plain
python3 scripts/verify-memory-restart.py
