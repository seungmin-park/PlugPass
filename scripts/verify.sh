#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
./gradlew clean build --console=plain
python3 scripts/verify-runtime.py
