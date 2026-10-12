"""Accept only the current main push CI, then verify and assemble its artifacts."""
import json
import os
from pathlib import Path
import sys
from cd_release import api, assemble

APP = 'plugpass'
REPO = 'seungmin-park/PlugPass'
run_id = int(sys.argv[1])
run = api(f'repos/{REPO}/actions/runs/{run_id}')
head = api(f'repos/{REPO}/git/ref/heads/main')['object']['sha']
if run['event'] != 'push' or run['head_branch'] != 'main' or run['head_sha'] != head or run['path'] != '.github/workflows/verify.yml':
    raise SystemExit('Only the current main push verification workflow may deploy')
output = assemble({'repositories': {APP: {'runId': run_id, 'head': head}}}, APP)
record = json.loads((output / 'manifest.json').read_text())['applications'][APP]
with open(os.environ['GITHUB_ENV'], 'a') as environment:
    environment.write(f"CD_RUN_ID={run_id}\nCD_RUN_ATTEMPT={run['run_attempt']}\nCD_HEAD_SHA={head}\n")
    environment.write(f"CD_ARCHIVE_DIGEST={record['sha256']}\nCD_ARCHIVE={output / record['archive']}\n")
