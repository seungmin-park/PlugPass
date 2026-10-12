"""Task-owned, loopback-only MySQL for verification; never uses an existing DB."""
from contextlib import contextmanager
import os
from pathlib import Path
import subprocess
import tempfile
import time
import uuid

ROOT = Path(__file__).resolve().parents[1]
MYSQL_IMAGE = 'mysql:8.4@sha256:6ea90827b1100f8f2ae306a539f86d2c264a26ed435a2a9f75551dd5c3aeb242'


def docker(*arguments):
    return subprocess.run(['docker', *arguments], check=True, text=True, capture_output=True).stdout.strip()


def database_ready(container):
    return subprocess.run(['docker', 'exec', container, 'sh', '-c',
                           'MYSQL_PWD="$MYSQL_PASSWORD" mysql --protocol=TCP -h127.0.0.1 -u"$MYSQL_USER" "$MYSQL_DATABASE" -Nse "SELECT 1"'],
                          capture_output=True).returncode == 0


def database_url(container):
    port = docker('port', container, '3306/tcp').split(':')[-1]
    return 'jdbc:mysql://127.0.0.1:' + port + '/plugpass?sslMode=DISABLED&allowPublicKeyRetrieval=true'


def verification_environment(parent_environment):
    blocked_prefixes = ('SPRING_', 'PLUGPASS_', 'MANAGEMENT_')
    blocked_names = {'DATA_API_KEY', 'JAVA_TOOL_OPTIONS', 'JDK_JAVA_OPTIONS', '_JAVA_OPTIONS'}
    return {name: value for name, value in parent_environment.items()
            if not name.startswith(blocked_prefixes) and name not in blocked_names}


@contextmanager
def mysql_environment():
    docker('info', '--format', '{{.ServerVersion}}')
    container = 'plugpass-mysql-verify-' + uuid.uuid4().hex[:12]
    with tempfile.TemporaryDirectory(prefix='plugpass-mysql-') as temporary:
        password = uuid.uuid4().hex
        env_file = Path(temporary) / 'mysql.env'
        env_file.write_text('MYSQL_DATABASE=plugpass\nMYSQL_USER=plugpass\nMYSQL_PASSWORD=' + password
                            + '\nMYSQL_ROOT_PASSWORD=' + uuid.uuid4().hex + '\n')
        env_file.chmod(0o600)
        created = False
        try:
            docker('run', '--detach', '--name', container, '--env-file', str(env_file),
                   '--publish', '127.0.0.1::3306', MYSQL_IMAGE, '--sql-mode=NO_ENGINE_SUBSTITUTION')
            created = True
            print('Owned MySQL container starting: ' + container, flush=True)
            deadline = time.monotonic() + 120
            while not database_ready(container):
                if time.monotonic() >= deadline:
                    raise RuntimeError('MySQL startup timeout: ' + container)
                time.sleep(.5)
            port = docker('port', container, '3306/tcp').split(':')[-1]
            environment = verification_environment(os.environ)
            environment.update({
                'PLUGPASS_DATABASE_URL': database_url(container),
                'PLUGPASS_DATABASE_USERNAME': 'plugpass',
                'PLUGPASS_DATABASE_PASSWORD': password,
                'PLUGPASS_INGESTION_ENABLED': 'false',
            })
            print('MySQL ready on loopback port ' + port + ' (ephemeral credentials)', flush=True)
            yield container, environment
        finally:
            if created:
                subprocess.run(['docker', 'rm', '--force', '--volumes', container],
                               check=True, stdout=subprocess.DEVNULL)
                print('Owned MySQL container and temporary data removed', flush=True)
