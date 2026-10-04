#!/usr/bin/env python3
"""Check real test evidence and the packaged application's public HTTP surface."""
import json
import os
from pathlib import Path
import re
import subprocess
import time
import urllib.error
import urllib.request
import xml.etree.ElementTree as ET
from zipfile import ZipFile


def require(condition, message):
    if not condition:
        raise RuntimeError(message)


def check_tests(directory, required_suites):
    reports = list(directory.glob('TEST-*.xml'))
    require(reports, 'No test reports: an empty suite is not a pass')
    suites = [ET.parse(report).getroot() for report in reports]
    count = sum(int(suite.attrib['tests']) for suite in suites)
    require(count > 0, 'Zero tests executed')
    for suite in suites:
        require(int(suite.attrib['tests']) > 0, f"Empty suite: {suite.attrib['name']}")
        for field in ('failures', 'errors', 'skipped'):
            require(int(suite.attrib[field]) == 0, f"{field}: {suite.attrib['name']}")
    missing = sorted(set(required_suites) - {suite.attrib['name'] for suite in suites})
    require(not missing, 'Missing required test suites: ' + ', '.join(missing))
    return count


def check_http(base, expected):
    with urllib.request.urlopen(base + '/actuator/health', timeout=5) as response:
        health = json.load(response)
        require(response.status == 200 and health.get('status') == 'UP', 'Health is not UP')
        require('components' not in health, 'Health details exposed')
    with urllib.request.urlopen(base + '/docs/index.html', timeout=5) as response:
        require(response.status == 200 and response.read() == expected, 'Served docs differ from generated docs')
    with urllib.request.urlopen(base + '/api/v1/stations?latitude=37.5&longitude=126.6&radiusMeters=1000&connector=DC_COMBO', timeout=5) as response:
        search = json.loads(response.read())
        require(response.status == 200 and search == {'dataReady': False, 'lastSuccessfulRunAt': None, 'stations': []}, 'Initial station search mismatch')
    with urllib.request.urlopen(base + '/api/v1/recommendations?latitude=0&longitude=0&radiusMeters=1000&connector=DC_COMBO', timeout=5) as response:
        recommendation = json.loads(response.read())
        require(response.status == 200 and recommendation == {'preferred': [], 'requiresConfirmation': [], 'excluded': []}, 'Initial recommendation mismatch')
    try:
        urllib.request.urlopen(base + '/api/v1/stations?latitude=91&longitude=126.6&radiusMeters=1000&connector=DC_COMBO', timeout=5)
        raise RuntimeError('Invalid station query accepted')
    except urllib.error.HTTPError as error:
        require(error.code == 400 and json.loads(error.read())['code'] == 'INVALID_REQUEST', 'Station validation mismatch')
    try:
        urllib.request.urlopen(base + '/api/v1/stations/9223372036854775807', timeout=5)
        raise RuntimeError('Missing station accepted')
    except urllib.error.HTTPError as error:
        body = json.loads(error.read())
        require(error.code == 404 and body == {'code': 'STATION_NOT_FOUND', 'message': '충전소를 찾을 수 없습니다', 'fields': {}}, 'Station detail 404 mismatch')
    try:
        with urllib.request.urlopen(base + '/actuator/env', timeout=5):
            raise RuntimeError('Environment endpoint exposed')
    except urllib.error.HTTPError as error:
        require(error.code == 404, f'Unexpected env status: {error.code}')
    try:
        with urllib.request.urlopen(base + '/actuator/metrics', timeout=5):
            raise RuntimeError('Metrics unexpectedly exposed in default configuration')
    except urllib.error.HTTPError as error:
        require(error.code == 404, f'Unexpected default metrics status: {error.code}')


def main():
    root = Path(__file__).resolve().parents[1]
    evidence = root / 'build/verification'
    evidence.mkdir(parents=True, exist_ok=True)
    result_file = evidence / 'runtime.json'
    result_file.unlink(missing_ok=True)
    required_suites = json.loads((root / 'docs/required-test-suites.json').read_text())
    require(isinstance(required_suites, list) and required_suites, 'Required test suite list is empty or invalid')
    require(all(isinstance(name, str) and name.strip() for name in required_suites), 'Invalid required test suite name')
    require(len(required_suites) == len(set(required_suites)), 'Duplicate required test suite names')
    count = check_tests(root / 'build/test-results/test', required_suites)
    jars = [jar for jar in (root / 'build/libs').glob('*.jar') if not jar.name.endswith('-plain.jar')]
    require(len(jars) == 1, 'Expected exactly one application JAR')
    expected = (root / 'build/docs/asciidoc/index.html').read_bytes()
    require(expected and b'Unresolved directive' not in expected, 'Invalid generated docs')
    with ZipFile(jars[0]) as archive:
        require(archive.read('BOOT-INF/classes/static/docs/index.html') == expected, 'Packaged docs mismatch')
        require(not any('PersistenceProbe' in name for name in archive.namelist()), 'Test fixture packaged')
    java = str(Path(os.environ['JAVA_HOME']) / 'bin/java') if os.environ.get('JAVA_HOME') else 'java'
    log_file = evidence / 'server.log'
    with log_file.open('w') as log:
        process = subprocess.Popen(
            [java, '-jar', str(jars[0]), '--server.port=0', '--server.address=127.0.0.1'],
            cwd=root, stdout=log, stderr=subprocess.STDOUT,
        )
        try:
            deadline = time.monotonic() + 60
            port = None
            while time.monotonic() < deadline:
                require(process.poll() is None, f'Server exited; see {log_file}')
                match = re.search(r'Tomcat started on port (\d+)', log_file.read_text())
                if match:
                    port = int(match.group(1))
                    break
                time.sleep(0.2)
            require(port is not None, f'Server startup timeout; see {log_file}')
            check_http(f'http://127.0.0.1:{port}', expected)
            result = {'tests': count, 'failures': 0, 'errors': 0, 'skipped': 0,
                      'health': 'UP', 'env_http': 404, 'station_search_http': 200, 'station_validation_http': 400, 'station_detail_missing_http': 404, 'docs_match': True,
                      'recommendations_http': 200, 'default_metrics_http': 404, 'test_fixtures_packaged': False}
            result_file.write_text(json.dumps(result, indent=2) + '\n')
            print(json.dumps(result), flush=True)
        finally:
            process.terminate()
            try:
                process.wait(timeout=10)
            except subprocess.TimeoutExpired:
                process.kill()
                process.wait(timeout=5)
            print(f'Verification server stopped: {process.returncode}', flush=True)


if __name__ == '__main__':
    main()
