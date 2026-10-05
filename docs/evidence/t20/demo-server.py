from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path
import os
import signal
import subprocess
import sys
import threading
import time

ROOT = Path.cwd().resolve()
OUT = ROOT / 'docs/evidence/t20'
LOG_OUT = Path(os.environ.get('T20_DEMO_LOG_DIR', str(OUT)))
LOG_OUT.mkdir(parents=True, exist_ok=True)
collect = sys.argv[1] if len(sys.argv) > 1 else 'true'
processes = []
threads = []
class Provider(BaseHTTPRequestHandler):
    def do_GET(self):
        body = (OUT / 'provider.xml').read_bytes()
        self.send_response(200)
        self.send_header('Content-Type', 'application/xml')
        self.send_header('Content-Length', str(len(body)))
        self.end_headers()
        self.wfile.write(body)
        print('Local synthetic provider: HTTP200, 2 chargers', flush=True)
    def log_message(self, *_):
        pass

def stream(process, name):
    with (LOG_OUT / (name + '-' + collect + '.log')).open('w') as log:
        for line in process.stdout:
            log.write(line)
            log.flush()
            print('[' + name + '] ' + line, end='', flush=True)

def start(args, cwd, name):
    print('T20 ' + name + ' command: ' + ' '.join('[build/performance-classpath.txt]' if index == 2 and name == 'backend' else value for index, value in enumerate(args) if 'service-key=' not in value), flush=True)
    process = subprocess.Popen(args, cwd=cwd, stdout=subprocess.PIPE, stderr=subprocess.STDOUT,
        text=True, start_new_session=True)
    processes.append(process)
    thread = threading.Thread(target=stream, args=(process, name), daemon=True)
    threads.append(thread)
    thread.start()
    return process

provider = ThreadingHTTPServer(('127.0.0.1', 0), Provider)
threading.Thread(target=provider.serve_forever, daemon=True).start()
try:
    classpath = (ROOT / 'build/performance-classpath.txt').read_text().strip()
    backend = start(['java', '-cp', classpath, 'reliability.RestartProbeApplication',
        '--server.address=127.0.0.1', '--server.port=8080', '--restart-probe.collect=' + collect,
        '--plugpass.public-data.endpoint=http://127.0.0.1:' + str(provider.server_port) + '/fixture',
        '--plugpass.public-data.service-key=synthetic-t20', '--plugpass.public-data.page-size=10'], ROOT, 'backend')
    vite = start(['/Users/seungmin/.asdf/installs/nodejs/26.7.0/bin/node', 'node_modules/vite/bin/vite.js',
        '--host', '127.0.0.1', '--port', '5173', '--strictPort'], ROOT / 'frontend', 'frontend')
    deadline = time.monotonic() + 60
    while True:
        if backend.poll() is not None or vite.poll() is not None:
            raise RuntimeError('Owned demo process exited during startup')
        content = (LOG_OUT / ('backend-' + collect + '.log')).read_text()
        if 'RESTART_PROBE_READY' in content:
            print('T20 DEMO READY http://localhost:5173/app/index.html#/stations collect=' + collect, flush=True)
            break
        if time.monotonic() > deadline:
            raise RuntimeError('Demo startup exceeded 60 seconds')
        time.sleep(.2)
    while all(process.poll() is None for process in processes):
        time.sleep(.5)
except KeyboardInterrupt:
    print('T20 stopping owned demo processes', flush=True)
finally:
    for process in processes:
        if process.poll() is None:
            os.killpg(process.pid, signal.SIGTERM)
    for process in processes:
        try:
            process.wait(timeout=10)
        except subprocess.TimeoutExpired:
            os.killpg(process.pid, signal.SIGKILL)
            process.wait(timeout=5)
        print('Owned demo PID ' + str(process.pid) + ' exit ' + str(process.returncode), flush=True)
    provider.shutdown()
    provider.server_close()
    for thread in threads:
        thread.join(timeout=2)
