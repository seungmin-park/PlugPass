#!/usr/bin/env python3
"""Separate JVMs: default memory H2 loses data; real recollection restores readiness."""
from contextlib import contextmanager
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
import json
from pathlib import Path
import re
import subprocess
import threading
import time
import urllib.request

ROOT = Path(__file__).resolve().parents[1]
QUERY = '/api/v1/stations?latitude=37.569620&longitude=126.641973&radiusMeters=1000&connector=DC_CHADEMO&limit=20'


class FixtureProvider(BaseHTTPRequestHandler):
    def do_GET(self):
        body = (ROOT / 'src/test/resources/publicdata/normal.xml').read_bytes()
        self.send_response(200)
        self.send_header('Content-Type', 'application/xml')
        self.send_header('Content-Length', str(len(body)))
        self.end_headers()
        self.wfile.write(body)

    def log_message(self, *_):
        pass


@contextmanager
def running_server(classpath, provider_port, collect, log_path):
    with log_path.open('w') as log:
        process = subprocess.Popen([
            'java', '-cp', classpath, 'reliability.RestartProbeApplication',
            '--server.address=127.0.0.1', '--server.port=0',
            '--plugpass.public-data.endpoint=http://127.0.0.1:' + str(provider_port) + '/fixture',
            '--plugpass.public-data.service-key=synthetic-restart-probe',
            '--plugpass.public-data.page-size=10', '--restart-probe.collect=' + collect,
        ], cwd=ROOT, stdout=log, stderr=subprocess.STDOUT)
        try:
            deadline = time.monotonic() + 60
            while time.monotonic() < deadline:
                if process.poll() is not None:
                    raise RuntimeError('Restart probe exited: ' + str(log_path))
                content = log_path.read_text()
                port = re.search(r'Tomcat started on port (\d+)', content)
                if port and 'RESTART_PROBE_READY' in content:
                    yield 'http://127.0.0.1:' + port.group(1)
                    return
                time.sleep(.2)
            raise RuntimeError('Restart probe startup timeout: ' + str(log_path))
        finally:
            process.terminate()
            try:
                process.wait(timeout=10)
            except subprocess.TimeoutExpired:
                process.kill()
                process.wait(timeout=5)
            print('Owned restart probe stopped: ' + str(process.returncode), flush=True)


def search(base):
    with urllib.request.urlopen(base + QUERY, timeout=5) as response:
        assert response.status == 200
        return json.load(response)


def verify_collected(body):
    assert body['dataReady'] is True and isinstance(body['lastSuccessfulRunAt'], str)
    assert len(body['stations']) == 1
    station = body['stations'][0]
    assert station['providerStationId'] == '28260005'
    assert station['compatibleChargerCount'] == 1 and len(station['chargers']) == 1
    charger = station['chargers'][0]
    assert charger['chargerId'] == '02' and charger['status'] == 'AVAILABLE'
    assert charger['sourceObservedAt'] is None and charger['freshness'] == 'UNVERIFIED'
    assert charger['reasonCode'] == 'SOURCE_OBSERVED_AT_MISSING'


def main():
    output = ROOT / 'build/verification/memory-restart'
    output.mkdir(parents=True, exist_ok=True)
    (output / 'result.json').unlink(missing_ok=True)
    classpath = (ROOT / 'build/performance-classpath.txt').read_text().strip()
    provider = ThreadingHTTPServer(('127.0.0.1', 0), FixtureProvider)
    thread = threading.Thread(target=provider.serve_forever, daemon=True)
    thread.start()
    try:
        with running_server(classpath, provider.server_port, 'true', output / 'before.log') as base:
            before = search(base)
            verify_collected(before)
        with running_server(classpath, provider.server_port, 'false', output / 'after.log') as base:
            after = search(base)
            assert after == {'dataReady': False, 'lastSuccessfulRunAt': None, 'stations': []}
        with running_server(classpath, provider.server_port, 'true', output / 'recollected.log') as base:
            recollected = search(base)
            verify_collected(recollected)
        for name, body in [('before', before), ('after', after), ('recollected', recollected)]:
            (output / (name + '.json')).write_text(json.dumps(body, ensure_ascii=False, indent=2) + '\n')
        result = {'separate_jvms': 3, 'memory_data_lost': True, 'readiness_reset': True, 'recollection_ready': True}
        (output / 'result.json').write_text(json.dumps(result, indent=2) + '\n')
        print(json.dumps(result), flush=True)
    finally:
        provider.shutdown()
        provider.server_close()
        thread.join(timeout=5)


if __name__ == '__main__':
    main()
