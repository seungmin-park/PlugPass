#!/usr/bin/env python3
"""Deterministic closed-loop HTTP baseline; Python standard library only."""
import argparse
from concurrent.futures import ThreadPoolExecutor
import hashlib
import http.client
import json
import math
from pathlib import Path
import platform
import re
import subprocess
import threading
import time
import urllib.request

NOW = '2026-10-05T00:00:00Z'
QUERY = '?latitude=37.5&longitude=127&radiusMeters=1000&connector=DC_COMBO&limit=20'
ROUTES = {
    'search_edge': '/api/v1/stations' + QUERY,
    'search_center': '/api/v1/stations?latitude=37.55&longitude=127.05&radiusMeters=1000&connector=DC_COMBO&limit=20',
    'detail': '/api/v1/stations/1',
    'recommendation': '/api/v1/recommendations' + QUERY + '&excludeStationId=1',
}


def percentile(samples, percent):
    if not samples:
        raise ValueError('No measured requests')
    return sorted(samples)[max(0, math.ceil(len(samples) * percent / 100) - 1)]


def summarize(samples):
    if not samples:
        raise ValueError('No measured requests')
    latencies = [sample['elapsed_ms'] for sample in samples]
    errors = sum(sample['error'] is not None for sample in samples)
    routes = {}
    for route in sorted({sample['route'] for sample in samples}):
        selected = [sample for sample in samples if sample['route'] == route]
        routes[route] = {'requests': len(selected), 'errors': sum(sample['error'] is not None for sample in selected),
                         'p50_ms': percentile([sample['elapsed_ms'] for sample in selected], 50),
                         'p95_ms': percentile([sample['elapsed_ms'] for sample in selected], 95)}
    return {'requests': len(samples), 'p50_ms': percentile(latencies, 50), 'p95_ms': percentile(latencies, 95),
            'unexpected_errors': errors, 'unexpected_error_rate': errors / len(samples), 'routes': routes,
            'latency_target_met': percentile(latencies, 95) <= 300, 'error_target_met': errors / len(samples) < .01}


def get(base, path):
    with urllib.request.urlopen(base + path, timeout=60) as response:
        if response.status != 200:
            raise ValueError('HTTP ' + str(response.status))
        return json.load(response)


def distance(latitude, longitude, other_latitude, other_longitude):
    latitude_delta = math.radians(other_latitude - latitude)
    longitude_delta = math.radians(other_longitude - longitude)
    haversine = math.sin(latitude_delta / 2) ** 2 + math.cos(math.radians(latitude)) * math.cos(math.radians(other_latitude)) * math.sin(longitude_delta / 2) ** 2
    return 6371008.8 * 2 * math.atan2(math.sqrt(haversine), math.sqrt(1 - haversine))


def expected_candidates(latitude, longitude):
    candidates = [(index + 1, distance(latitude, longitude, 37.5 + (index // 100) * .001, 127 + (index % 100) * .001))
                  for index in range(10000)]
    return sorted((candidate for candidate in candidates if candidate[1] <= 1000), key=lambda candidate: (candidate[1], candidate[0]))


def assert_search(body, latitude, longitude):
    assert body['dataReady'] is True and body['lastSuccessfulRunAt'] == NOW
    expected = expected_candidates(latitude, longitude)[:20]
    assert len(body['stations']) == len(expected)
    for station, (station_id, expected_distance) in zip(body['stations'], expected):
        index = station_id - 1
        assert station['id'] == station_id and station['providerStationId'] == str(index)
        assert station['provider'] == 'PERF' and station['name'] == 'generated-' + str(index)
        assert abs(station['distanceMeters'] - expected_distance) < .00001
        assert station['compatibleChargerCount'] == 4
        assert station['reportedAvailableCount'] == (0 if index % 4 == 3 else 4)
        assert len(station['chargers']) == 4
        for charger in station['chargers']:
            assert charger['connectorCode'] == '04' and charger['limitYn'] == 'N'
            assert charger['status'] == ('OCCUPIED' if index % 4 == 3 else 'AVAILABLE')
            assert charger['freshness'] == ('UNVERIFIED' if index % 4 == 1 else 'STALE' if index % 4 == 2 else 'RECENT')
            assert charger['collectedAt'] == NOW


def assert_recommendation(body):
    candidates = [(station_id, meters) for station_id, meters in expected_candidates(37.5, 127) if station_id != 1]
    groups = {'preferred': [item for item in candidates if (item[0] - 1) % 4 == 0][:20],
              'requiresConfirmation': [item for item in candidates if (item[0] - 1) % 4 == 1][:20],
              'excluded': [item for item in candidates if (item[0] - 1) % 4 in (2, 3)][:20]}
    for group, expected in groups.items():
        assert len(body[group]) == len(expected)
        for candidate, (station_id, meters) in zip(body[group], expected):
            assert candidate['id'] == station_id and candidate['id'] != 1
            assert candidate['name'] == 'generated-' + str(station_id - 1)
            assert abs(candidate['distanceMeters'] - meters) < .00001
            remainder = (station_id - 1) % 4
            assert candidate['reasonCodes'] == (['RECENT_AVAILABLE'] if remainder == 0 else
                    ['UNVERIFIED_AVAILABLE', 'SOURCE_OBSERVED_AT_MISSING'] if remainder == 1 else
                    ['MAX_AGE_EXCEEDED'] if remainder == 2 else ['NOT_AVAILABLE'])


def verify_correctness(base):
    responses = {route: get(base, path) for route, path in ROUTES.items()}
    assert_search(responses['search_edge'], 37.5, 127)
    assert_search(responses['search_center'], 37.55, 127.05)
    detail = responses['detail']
    assert detail['id'] == 1 and detail['providerStationId'] == '0' and detail['name'] == 'generated-0'
    assert len(detail['chargers']) == 5
    assert [charger['chargerId'] for charger in detail['chargers']] == ['01', '02', '03', '04', '05']
    for charger in detail['chargers']:
        assert charger['status'] == 'AVAILABLE' and charger['freshness'] == 'RECENT'
        assert charger['sourceObservedAt'] == NOW and charger['collectedAt'] == NOW
    assert_recommendation(responses['recommendation'])
    return responses


def run_phase(port, expected, users, seconds):
    if seconds <= 0:
        return []
    barrier = threading.Barrier(users + 1)
    phase = {}
    def worker(worker_id):
        connection = http.client.HTTPConnection('127.0.0.1', port, timeout=60)
        samples = []
        sequence = worker_id
        barrier.wait()
        try:
            while time.monotonic() < phase['deadline']:
                route = list(ROUTES)[sequence % len(ROUTES)]
                started = time.monotonic()
                error = None
                try:
                    connection.request('GET', ROUTES[route])
                    response = connection.getresponse()
                    body = response.read()
                    elapsed = (time.monotonic() - started) * 1000
                    if response.status != 200:
                        error = 'HTTP ' + str(response.status)
                    elif json.loads(body) != expected[route]:
                        error = 'response mismatch'
                except (OSError, http.client.HTTPException, ValueError) as failure:
                    elapsed = (time.monotonic() - started) * 1000
                    error = type(failure).__name__ + ': ' + str(failure)
                    connection.close()
                    connection = http.client.HTTPConnection('127.0.0.1', port, timeout=60)
                samples.append({'route': route, 'elapsed_ms': elapsed, 'error': error,
                                'worker': worker_id, 'started_seconds': started - phase['started']})
                sequence += 1
        finally:
            connection.close()
        return samples
    with ThreadPoolExecutor(max_workers=users) as executor:
        futures = [executor.submit(worker, worker_id) for worker_id in range(users)]
        phase['started'] = time.monotonic()
        phase['deadline'] = phase['started'] + seconds
        barrier.wait()
        return [sample for future in futures for sample in future.result()]


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--output', type=Path, required=True)
    parser.add_argument('--users', type=int, default=20)
    parser.add_argument('--warmup', type=int, default=30)
    parser.add_argument('--seconds', type=int, default=180)
    arguments = parser.parse_args()
    if arguments.users < 1 or arguments.warmup < 0 or arguments.seconds < 1:
        parser.error('users/seconds must be positive; warmup must be nonnegative')
    root = Path(__file__).resolve().parents[2]
    output = arguments.output.resolve()
    output.mkdir(parents=True, exist_ok=True)
    # Existing output is rejected so failures cannot leave an old success report.
    if (output / 'result.json').exists():
        parser.error('Use a fresh output directory')
    subprocess.run(['./gradlew', 'performanceClasspath', '--console=plain'], cwd=root, check=True)
    classpath = (root / 'build/performance-classpath.txt').read_text().strip()
    source = subprocess.check_output(['git', 'rev-parse', 'HEAD'], cwd=root, text=True).strip()
    dirty = bool(subprocess.check_output(['git', 'status', '--porcelain'], cwd=root, text=True).strip())
    with (output / 'server.log').open('w') as log:
        process = subprocess.Popen(['java', '-Xms512m', '-Xmx2g', '-cp', classpath, 'performance.PerformanceApplication',
                                    '--server.port=0', '--server.address=127.0.0.1'], cwd=root, stdout=log, stderr=subprocess.STDOUT)
        try:
            deadline = time.monotonic() + 120
            port = None
            while time.monotonic() < deadline:
                if process.poll() is not None:
                    raise RuntimeError('Performance server exited; see server.log')
                text = (output / 'server.log').read_text()
                match = re.search(r'Tomcat started on port (\d+)', text)
                if match and 'PERFORMANCE_DATA_READY' in text:
                    port = int(match.group(1))
                    break
                time.sleep(.2)
            if port is None:
                raise RuntimeError('Performance server startup timeout')
            base = 'http://127.0.0.1:' + str(port)
            metadata = get(base, '/__performance')
            assert metadata['stations'] == 10000 and metadata['chargers'] == 50000
            assert metadata['statisticsEnabled'] is True
            expected = verify_correctness(base)
            (output / 'correctness-before.json').write_text(json.dumps(expected, ensure_ascii=False, indent=2))
            sql = {}
            for route, path in ROUTES.items():
                before = get(base, '/__performance')
                assert get(base, path) == expected[route]
                after = get(base, '/__performance')
                sql[route] = {'statements': after['statements'] - before['statements'],
                              'entities_loaded': after['entitiesLoaded'] - before['entitiesLoaded']}
            print('SQL baseline: ' + json.dumps(sql), flush=True)
            print('Warmup: ' + str(arguments.warmup) + ' seconds, users=' + str(arguments.users), flush=True)
            warmup = run_phase(port, expected, arguments.users, arguments.warmup)
            print('Measurement: ' + str(arguments.seconds) + ' seconds', flush=True)
            started = time.monotonic()
            samples = run_phase(port, expected, arguments.users, arguments.seconds)
            elapsed = time.monotonic() - started
            (output / 'samples.json').write_text(json.dumps(samples, indent=2))
            # Write raw measurements before final assertions so a failure remains diagnosable.
            after = verify_correctness(base)
            (output / 'correctness-after.json').write_text(json.dumps(after, ensure_ascii=False, indent=2))
            assert after == expected
            final_metadata = get(base, '/__performance')
            assert final_metadata['stations'] == 10000 and final_metadata['chargers'] == 50000
            result = {'source_sha': source, 'source_dirty': dirty, 'pid': process.pid, 'port': port,
                      'environment': {'platform': platform.platform(), 'machine': platform.machine(), 'python': platform.python_version()},
                      'server': metadata, 'users': arguments.users, 'warmup_seconds': arguments.warmup,
                      'measurement_seconds': arguments.seconds, 'elapsed_with_drain_seconds': elapsed,
                      'dataset': 'grid-v1', 'seed': 0, 'station_count': 10000, 'charger_count': 50000,
                      'full_baseline': (arguments.users, arguments.warmup, arguments.seconds) == (20, 30, 180),
                      'sql_per_request': sql, 'correctness_before_after': True,
                      'warmup_requests': len(warmup), 'warmup_errors': sum(item['error'] is not None for item in warmup),
                      'summary': summarize(samples),
                      'samples_sha256': hashlib.sha256((output / 'samples.json').read_bytes()).hexdigest()}
            (output / 'result.json').write_text(json.dumps(result, indent=2))
            print(json.dumps(result['summary']), flush=True)
            if result['summary']['unexpected_errors']:
                raise RuntimeError('Unexpected HTTP/response failures; see samples.json')
        finally:
            process.terminate()
            try:
                process.wait(timeout=10)
            except subprocess.TimeoutExpired:
                process.kill()
                process.wait(timeout=5)
            print('Owned performance server stopped: ' + str(process.returncode), flush=True)


if __name__ == '__main__':
    main()
