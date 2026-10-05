#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
backend_port=${PLUGPASS_E2E_BACKEND_PORT:-18180}
frontend_port=${PLUGPASS_E2E_FRONTEND_PORT:-5183}
output="$PWD/build/verification/frontend-e2e"
mkdir -p "$output"
backend_pid=''
frontend_pid=''
log_pid=''
cleanup() {
  for owned_pid in "$log_pid" "$frontend_pid" "$backend_pid"; do
    if [[ -n "$owned_pid" ]]; then kill "$owned_pid" 2>/dev/null || true; fi
  done
  for owned_pid in "$log_pid" "$frontend_pid" "$backend_pid"; do
    if [[ -n "$owned_pid" ]]; then wait "$owned_pid" 2>/dev/null || true; fi
  done
}
trap cleanup EXIT
trap 'exit 130' INT
trap 'exit 143' TERM
python3 - "$backend_port" "$frontend_port" <<'PY'
import socket
import sys
for port in map(int, sys.argv[1:]):
    with socket.socket() as server:
        server.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
        server.bind(('127.0.0.1', port))
PY
./gradlew chargingDemoClasspath --console=plain
: >"$output/backend.log"
: >"$output/frontend.log"
java -cp "$(cat build/charging-demo-classpath.txt)" e2e.ChargingDemoApplication \
  --server.address=127.0.0.1 --server.port="$backend_port" >"$output/backend.log" 2>&1 &
backend_pid=$!
(
  cd frontend
  export PLUGPASS_API_TARGET="http://127.0.0.1:$backend_port"
  exec node node_modules/vite/bin/vite.js --host 127.0.0.1 --port "$frontend_port" --strictPort
) >"$output/frontend.log" 2>&1 &
frontend_pid=$!
tail -n +1 -f "$output/backend.log" "$output/frontend.log" &
log_pid=$!
python3 - "$backend_port" "$frontend_port" "$output/backend.log" <<'PY'
import json
from pathlib import Path
import sys
import time
import urllib.error
import urllib.request
backend, frontend, logfile = sys.argv[1:]
deadline = time.monotonic() + 60
while time.monotonic() < deadline:
    try:
        with urllib.request.urlopen(f'http://127.0.0.1:{backend}/api/v1/stations?latitude=37.5&longitude=127&radiusMeters=1000&connector=DC_COMBO&limit=20', timeout=1) as response:
            data = json.load(response)
        with urllib.request.urlopen(f'http://127.0.0.1:{frontend}/app/index.html', timeout=1) as response:
            html = response.read().decode()
        if 'CHARGING_DEMO_READY' in Path(logfile).read_text() and data['dataReady'] and len(data['stations']) == 2 and '/@vite/client' in html:
            print('E2E 준비 완료: 실제 HTTP 수집·H2 2곳, Vue 개발 모드')
            break
    except (OSError, ValueError, urllib.error.URLError):
        pass
    time.sleep(.2)
else:
    raise RuntimeError('E2E 서버 준비 실패: backend.log/frontend.log를 확인하세요')
PY
export PLUGPASS_E2E_BASE_URL="http://127.0.0.1:$frontend_port"
if [[ ${1:-} == --serve ]]; then
  echo "화면 검증 서버 실행 중: $PLUGPASS_E2E_BASE_URL/app/index.html#/stations (Ctrl+C로 종료)"
  wait "$backend_pid"
else
  npm --prefix frontend run test:e2e -- "$@"
fi
