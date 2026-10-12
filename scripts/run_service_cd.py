"""검증된 백엔드 ZIP을 IAP와 제한된 SSH 명령으로 전송한다."""
import base64
import hashlib
import json
import os
from pathlib import Path
import re
import socket
import subprocess
import tempfile
import time

REPOSITORY = 'seungmin-park/PlugPass'


def metadata(environment):
    fields = [('run_id','CD_RUN_ID',r'[1-9][0-9]{0,19}'),
              ('run_attempt','CD_RUN_ATTEMPT',r'[1-9][0-9]{0,9}'),
              ('head_sha','CD_HEAD_SHA',r'[a-f0-9]{40}'),
              ('archive_digest','CD_ARCHIVE_DIGEST',r'[a-f0-9]{64}')]
    values = {}
    for name, variable, pattern in fields:
        value = environment.get(variable,'')
        if not re.fullmatch(pattern,value):
            raise ValueError('잘못된 CI metadata: ' + variable)
        values[name] = int(value) if name in ('run_id','run_attempt') else value
    for variable, expected in (('CD_PROJECT','project-687f2332-ff8d-4a13-95a'),
                               ('CD_ZONE','asia-northeast3-a'),('CD_INSTANCE','deploy-vm')):
        if environment.get(variable) != expected:
            raise ValueError('지정된 DEMP 운영 VM만 사용할 수 있습니다: ' + variable)
    return values


def remote_command(values):
    return f'deploy {values["run_id"]} {values["run_attempt"]} {values["head_sha"]} {values["archive_digest"]}'


def require_main(values, main_sha):
    if main_sha != values['head_sha']:
        raise ValueError('전송 직전 main이 바뀌었습니다. 오래된 실행물을 배포하지 않습니다')


def summary(values, code):
    if code == 0:
        state = '백엔드 전환·공개 검사·이전 요청 종료와 정리 검증 완료'
    elif code == 2:
        state = 'cleanup-incomplete: 이전 요청/프로세스 정리가 끝나지 않았습니다. status와 cleanup을 확인하세요'
    else:
        state = '백엔드 배포 실패: 실제 status와 journal을 확인하세요'
    return f'{state}\nCI: {values["run_id"]}, attempt: {values["run_attempt"]}, source: {values["head_sha"]}\n'


def _main_sha():
    return subprocess.check_output(['gh','api',f'repos/{REPOSITORY}/git/ref/heads/main','--jq','.object.sha'],
                                   text=True,timeout=30).strip()


def _host_key(text):
    match = re.fullmatch(r'ssh-ed25519 ([A-Za-z0-9+/]+={0,2})(?: [^\r\n]+)?',text.strip())
    if not match:
        raise ValueError('검증된 Ed25519 SSH host 공개 키가 필요합니다')
    raw = base64.b64decode(match[1],validate=True)
    prefix = b'\x00\x00\x00\x0bssh-ed25519\x00\x00\x00\x20'
    if len(raw) != len(prefix)+32 or not raw.startswith(prefix):
        raise ValueError('SSH host 공개 키 형식이 잘못됐습니다')
    return 'ssh-ed25519 ' + match[1]


def archive_digest(path):
    digest = hashlib.sha256()
    with path.open('rb') as stream:
        for chunk in iter(lambda: stream.read(1024 * 1024), b''):
            digest.update(chunk)
    return digest.hexdigest()


def main():
    environment = os.environ
    values = metadata(environment)
    raw = Path(os.environ['CD_ARCHIVE'])
    if not raw.is_file() or raw.stat().st_size > 256*1024**2:
        raise ValueError('제한된 검증 ZIP이 필요합니다')
    actual = archive_digest(raw)
    if actual != values['archive_digest']:
        raise ValueError('전송할 ZIP 해시가 검증한 실행물과 다릅니다')
    private_key = environment.get('CD_SSH_KEY','')
    if not private_key.startswith('-----BEGIN OPENSSH PRIVATE KEY-----'):
        raise ValueError('백엔드 전용 SSH 개인 키가 등록되지 않았습니다')
    host_key = _host_key(environment.get('CD_HOST_KEY',''))
    require_main(values,_main_sha())
    with tempfile.TemporaryDirectory(prefix='demp-backend-cd-',dir=environment.get('RUNNER_TEMP')) as directory:
        root = Path(directory)
        root.chmod(0o700)
        key, hosts = root/'key', root/'known_hosts'
        key.write_text(private_key.rstrip()+'\n')
        key.chmod(0o600)
        hosts.write_text('deploy-vm-backend-cd '+host_key+'\n')
        hosts.chmod(0o600)
        with (root/'iap.log').open('wb') as log:
            tunnel = subprocess.Popen(['gcloud','compute','start-iap-tunnel',environment['CD_INSTANCE'],'22',
                '--project',environment['CD_PROJECT'],'--zone',environment['CD_ZONE'],
                '--local-host-port=127.0.0.1:2222'],stdout=log,stderr=log)
            try:
                ready = False
                for _ in range(30):
                    if tunnel.poll() is not None:
                        raise RuntimeError('IAP 터널 프로세스가 종료됐습니다')
                    try:
                        with socket.create_connection(('127.0.0.1',2222),timeout=1):
                            ready = True
                            break
                    except OSError:
                        time.sleep(1)
                if not ready:
                    raise RuntimeError('IAP 터널 준비 시간이 초과됐습니다')
                require_main(values,_main_sha())
                with raw.open('rb') as payload:
                    result = subprocess.run(['ssh','-T','-p','2222','-i',str(key),
                        '-o','IdentitiesOnly=yes','-o','BatchMode=yes','-o','ConnectTimeout=15',
                        '-o','ServerAliveInterval=15','-o','ServerAliveCountMax=3',
                        '-o','StrictHostKeyChecking=yes','-o','HostKeyAlias=deploy-vm-backend-cd',
                        '-o','UserKnownHostsFile='+str(hosts),'plugpass-cd@127.0.0.1',
                        remote_command(values)],stdin=payload,timeout=1100)
                message = summary(values,result.returncode)
                print(message)
                if environment.get('GITHUB_STEP_SUMMARY'):
                    with open(environment['GITHUB_STEP_SUMMARY'],'a') as stream:
                        stream.write(message)
                return result.returncode
            finally:
                if tunnel.poll() is None:
                    tunnel.terminate()
                    try:
                        tunnel.wait(timeout=10)
                    except subprocess.TimeoutExpired:
                        tunnel.kill()
                        tunnel.wait()


if __name__ == '__main__':
    raise SystemExit(main())
