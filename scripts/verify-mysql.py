#!/usr/bin/env python3
"""Run the deployment contract against an isolated real MySQL, without skips."""
import argparse
from contextlib import contextmanager
import hashlib
import json
import re
import runpy
import subprocess
import time
import urllib.request

from mysql_environment import ROOT, MYSQL_IMAGE, database_ready, database_url, docker, mysql_environment

QUERY = '/api/v1/stations?latitude=37.5&longitude=127&radiusMeters=1000&connector=DC_COMBO&limit=20'
CHECKS = runpy.run_path(str(ROOT / 'scripts/verify-runtime.py'))


@contextmanager
def application(command, environment, log_path, ready_marker):
    with log_path.open('w') as log:
        process = subprocess.Popen(command + [
            '--spring.profiles.active=deployment', '--server.address=127.0.0.1', '--server.port=0',
            '--management.server.address=127.0.0.1', '--management.server.port=0',
            '--plugpass.ingestion.schedule.enabled=false',
        ], cwd=ROOT, env=environment, stdout=log, stderr=subprocess.STDOUT)
        try:
            deadline = time.monotonic() + 60
            while time.monotonic() < deadline:
                if process.poll() is not None:
                    raise RuntimeError('MySQL application exited: ' + str(log_path))
                content = log_path.read_text()
                ports = re.findall(r'Tomcat started on port (\d+)', content)
                if ports and ready_marker in content:
                    base = 'http://127.0.0.1:' + ports[0]
                    print('MySQL application ready: ' + base + ' — ' + log_path.name, flush=True)
                    yield base
                    return
                time.sleep(.2)
            raise RuntimeError('MySQL application startup timeout: ' + str(log_path))
        finally:
            process.terminate()
            try:
                process.wait(timeout=10)
            except subprocess.TimeoutExpired:
                process.kill()
                process.wait(timeout=5)
            print('Owned MySQL application stopped: ' + str(process.returncode), flush=True)


def get_json(base, path):
    with urllib.request.urlopen(base + path, timeout=5) as response:
        assert response.status == 200
        return json.load(response)


def verify_journey(base):
    search = get_json(base, QUERY)
    assert search['dataReady'] is True and search['lastSuccessfulRunAt'] == '2026-10-05T00:00:00Z'
    assert [station['providerStationId'] for station in search['stations']] == ['browser_first', 'browser_second']
    assert all(station['compatibleChargerCount'] == 1 for station in search['stations'])
    first, second = search['stations']
    detail = get_json(base, '/api/v1/stations/' + str(first['id']))
    assert detail['name'] == '데모 가까운 충전소'
    assert len(detail['chargers']) == 1
    charger = detail['chargers'][0]
    assert charger['status'] == 'AVAILABLE' and charger['sourceObservedAt'] is None
    assert charger['freshness'] == 'UNVERIFIED'
    recommendation = get_json(base, '/api/v1/recommendations' + QUERY[QUERY.index('?'):]
                              + '&excludeStationId=' + str(first['id']))
    assert recommendation['preferred'] == [] and recommendation['excluded'] == []
    assert [candidate['id'] for candidate in recommendation['requiresConfirmation']] == [second['id']]
    return {'search': search, 'detail': detail, 'recommendation': recommendation}


def verify_restart(container, environment, output, serve):
    classpath = (ROOT / 'build/charging-demo-classpath.txt').read_text().strip()
    with application(['java', '-cp', classpath, 'mysql.MySqlIngestionApplication'], environment,
                     output / 'ingestion.log', 'CHARGING_DEMO_READY') as base:
        before = verify_journey(base)
    docker('restart', container)
    deadline = time.monotonic() + 60
    while not database_ready(container):
        if time.monotonic() > deadline:
            raise RuntimeError('MySQL restart timeout')
        time.sleep(.5)
    environment['PLUGPASS_DATABASE_URL'] = database_url(container)
    print('MySQL restarted; starting the shipped JAR without recollection', flush=True)
    jar = ROOT / 'build/libs/plugpass-0.0.1-SNAPSHOT.jar'
    missing_environment = dict(environment)
    for variable in ('PLUGPASS_DATABASE_URL', 'PLUGPASS_DATABASE_PASSWORD', 'SPRING_DATASOURCE_URL',
                     'SPRING_DATASOURCE_PASSWORD', 'SPRING_APPLICATION_JSON'):
        missing_environment.pop(variable, None)
    with (output / 'missing-database.log').open('w') as log:
        missing = subprocess.run(['java', '-jar', str(jar), '--spring.profiles.active=deployment',
                                  '--spring.main.web-application-type=none', '--plugpass.ingestion.schedule.enabled=false'],
                                 cwd=ROOT, env=missing_environment, stdout=log,
                                 stderr=subprocess.STDOUT, timeout=30)
    assert missing.returncode != 0, 'Deployment silently started without required DB configuration'
    with application(['java', '-jar', str(jar)], environment, output / 'packaged.log',
                     'Started PlugPassApplication') as base:
        after = verify_journey(base)
        assert after == before, 'Search/detail/recommendation changed across DB and application restart'
        with urllib.request.urlopen(base + '/app/index.html', timeout=5) as response:
            assert response.status == 200 and '/app/assets/' in response.read().decode()
        with urllib.request.urlopen(base + '/docs/index.html', timeout=5) as response:
            assert response.read() == (ROOT / 'build/docs/asciidoc/index.html').read_bytes()
        browser_environment = dict(environment, PLUGPASS_E2E_BASE_URL=base)
        subprocess.run(['npm', '--prefix', 'frontend', 'run', 'test:e2e', '--',
                        '--config=playwright.mysql.config.ts'], cwd=ROOT, env=browser_environment, check=True)
        browser_count = CHECKS['check_browser_tests'](
            json.loads((ROOT / 'frontend/test-results/mysql.json').read_text()), ['charging-journey.spec.ts'])
        for name, body in [('before', before), ('after', after)]:
            (output / (name + '.json')).write_text(json.dumps(body, ensure_ascii=False, indent=2) + '\n')
        if serve:
            print('MYSQL_BROWSER_READY ' + base + '/app/index.html#/stations', flush=True)
            print('Press Ctrl+C in this verification pane to stop the owned app and DB.', flush=True)
            while True:
                time.sleep(1)
    return {'separate_jvms': 2, 'mysql_restarted': True, 'data_preserved': True,
            'readiness_preserved': True, 'packaged_jar_journey': True, 'browser_tests': browser_count,
            'missing_database_config_rejected': True,
            'jar_sha256': hashlib.sha256(jar.read_bytes()).hexdigest()}


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--tests')
    parser.add_argument('--tests-only', action='store_true')
    parser.add_argument('--serve', action='store_true')
    arguments = parser.parse_args()
    output = ROOT / 'build/verification' / ('mysql-manual' if arguments.serve else 'mysql')
    output.mkdir(parents=True, exist_ok=True)
    (output / 'result.json').unlink(missing_ok=True)
    with mysql_environment() as (container, environment):
        version = docker('exec', container, 'sh', '-c',
                         'MYSQL_PWD="$MYSQL_PASSWORD" mysql -u"$MYSQL_USER" "$MYSQL_DATABASE" -Nse "SELECT VERSION()"')
        command = ['./gradlew', 'mysqlTest', '--rerun-tasks', '--console=plain']
        if arguments.tests:
            command.extend(['--tests', arguments.tests])
        subprocess.run(command, cwd=ROOT, env=environment, check=True)
        count = CHECKS['check_tests'](ROOT / 'build/test-results/mysqlTest', ['mysql.MySqlDeploymentTests'])
        result = {'mysql_tests': count, 'image': MYSQL_IMAGE, 'mysql_version': version}
        if not arguments.tests_only:
            subprocess.run(['./gradlew', 'chargingDemoClasspath', '--console=plain'], cwd=ROOT,
                           env=environment, check=True)
            result.update(verify_restart(container, environment, output, arguments.serve))
        (output / 'result.json').write_text(json.dumps(result, indent=2) + '\n')
        print(json.dumps(result), flush=True)


if __name__ == '__main__':
    main()
