import hashlib
import io
import json
from pathlib import Path
import tempfile
import unittest
from unittest import mock
from zipfile import ZipFile

import cd_release

HEAD = 'a' * 40
TREE = 'b' * 40
RUN_ID = 101


def component_zip(jar_bytes):
    files = {'plugpass/service.jar': hashlib.sha256(jar_bytes).hexdigest()}
    record = {'component': 'plugpass', 'repository': 'seungmin-park/PlugPass', 'headSha': HEAD,
              'sourceSha': HEAD, 'treeSha': TREE, 'runId': RUN_ID, 'runAttempt': 1, 'event': 'push', 'files': files}
    buffer = io.BytesIO()
    with ZipFile(buffer, 'w') as archive:
        archive.writestr('plugpass/service.jar', jar_bytes)
        archive.writestr('component.json', json.dumps(record))
    return buffer.getvalue()


class AssembleProducerBindingTests(unittest.TestCase):
    def assemble(self, job_names, artifact_digest):
        payload = component_zip(b'verified-jar')
        run = {'id': RUN_ID, 'run_attempt': 1, 'head_sha': HEAD, 'status': 'completed', 'conclusion': 'success',
               'html_url': 'https://example.invalid/run'}
        artifact = {'id': 7, 'name': f'deployment-plugpass-{RUN_ID}-1', 'expired': False}
        if artifact_digest == 'matching':
            artifact['digest'] = 'sha256:' + hashlib.sha256(payload).hexdigest()
        responses = {
            f'repos/seungmin-park/PlugPass/actions/runs/{RUN_ID}': run,
            f'repos/seungmin-park/PlugPass/actions/runs/{RUN_ID}/jobs?filter=latest&per_page=100':
                {'jobs': [{'name': name, 'status': 'completed', 'conclusion': 'success'} for name in job_names]},
            f'repos/seungmin-park/PlugPass/git/commits/{HEAD}': {'tree': {'sha': TREE}},
            f'repos/seungmin-park/PlugPass/actions/runs/{RUN_ID}/artifacts?per_page=100': {'artifacts': [artifact]},
        }

        def download(command, stdout, check):
            stdout.write(payload)

        def git(command, cwd, text):
            return '' if command[:2] == ['git', 'status'] else HEAD

        root = Path(tempfile.mkdtemp())
        with mock.patch.object(cd_release, 'ROOT', root), \
                mock.patch.object(cd_release, 'api', side_effect=lambda path: responses[path]), \
                mock.patch.object(cd_release.subprocess, 'run', side_effect=download), \
                mock.patch.object(cd_release.subprocess, 'check_output', side_effect=git), \
                mock.patch('builtins.print'):
            return cd_release.assemble({'repositories': {'plugpass': {'runId': RUN_ID, 'head': HEAD}}}, 'plugpass')

    def test_requires_the_job_that_builds_and_uploads_the_jar(self):
        with self.assertRaisesRegex(ValueError, 'Required verification gate missing: Web and backend verify'):
            self.assemble(['PlugPass verify', 'iOS build', 'Android build'], 'matching')

    def test_rejects_an_artifact_without_a_digest(self):
        with self.assertRaisesRegex(ValueError, 'GitHub artifact digest missing: deployment-plugpass-101-1'):
            self.assemble(['Web and backend verify', 'PlugPass verify'], None)

    def test_assembles_a_producer_artifact_with_a_matching_digest(self):
        output = self.assemble(['Web and backend verify', 'PlugPass verify'], 'matching')

        manifest = json.loads((output / 'manifest.json').read_text())
        self.assertEqual(list(manifest['applications']), ['plugpass'])
        self.assertEqual(manifest['applications']['plugpass']['repositories']['plugpass']['runId'], RUN_ID)


if __name__ == '__main__':
    unittest.main()
