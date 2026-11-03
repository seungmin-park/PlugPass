from pathlib import Path
import re
import unittest

WORKFLOW = Path(__file__).resolve().parents[1] / '.github/workflows/verify.yml'


def job_blocks(text):
    jobs = text.split('\njobs:\n', 1)[1]
    names = re.findall(r'^  ([a-z][a-z0-9_-]*):\n', jobs, flags=re.MULTILINE)
    blocks = re.split(r'^  [a-z][a-z0-9_-]*:\n', jobs, flags=re.MULTILINE)[1:]
    return dict(zip(names, blocks))


class DeploymentRunWorkflowTests(unittest.TestCase):
    def test_deploy_trusted_run_contains_only_the_jar_producer_and_required_gate(self):
        jobs = job_blocks(WORKFLOW.read_text())

        self.assertEqual(sorted(jobs), ['verify', 'web'])
        self.assertIn('name: Web and backend verify', jobs['web'])
        self.assertIn('name: PlugPass verify', jobs['verify'])
        self.assertIn('needs: [web]', jobs['verify'])

    def test_required_gate_still_fails_unless_the_producer_succeeded(self):
        gate = job_blocks(WORKFLOW.read_text())['verify']

        self.assertIn('if: always()', gate)
        self.assertIn('test "$WEB_RESULT" = success', gate)
        self.assertNotIn('IOS_RESULT', gate)
        self.assertNotIn('ANDROID_RESULT', gate)


if __name__ == '__main__':
    unittest.main()
