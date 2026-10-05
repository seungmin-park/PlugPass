import importlib.util
from pathlib import Path
import tempfile
import unittest


spec = importlib.util.spec_from_file_location('verify_runtime', Path(__file__).with_name('verify-runtime.py'))
verify_runtime = importlib.util.module_from_spec(spec)
spec.loader.exec_module(verify_runtime)


class RequiredTestSuiteTests(unittest.TestCase):
    def test_rejects_missing_required_suite_even_when_other_tests_pass(self):
        with tempfile.TemporaryDirectory() as temporary:
            reports = Path(temporary)
            (reports / 'TEST-present.xml').write_text(
                '<testsuite name="present" tests="1" failures="0" errors="0" skipped="0"/>')
            with self.assertRaisesRegex(RuntimeError, 'Missing required test suites: missing'):
                verify_runtime.check_tests(reports, {'present', 'missing'})

    def test_accepts_required_suites_and_new_optional_suites(self):
        with tempfile.TemporaryDirectory() as temporary:
            reports = Path(temporary)
            self.write_report(reports, 'required', tests=2)
            self.write_report(reports, 'new')
            self.assertEqual(verify_runtime.check_tests(reports, {'required'}), 3)

    def test_rejects_absent_reports(self):
        with tempfile.TemporaryDirectory() as temporary:
            with self.assertRaisesRegex(RuntimeError, 'No test reports'):
                verify_runtime.check_tests(Path(temporary), {'required'})

    def test_rejects_empty_suite(self):
        with tempfile.TemporaryDirectory() as temporary:
            reports = Path(temporary)
            self.write_report(reports, 'required')
            self.write_report(reports, 'empty', tests=0)
            with self.assertRaisesRegex(RuntimeError, 'Empty suite: empty'):
                verify_runtime.check_tests(reports, {'required'})

    def test_rejects_zero_tests(self):
        with tempfile.TemporaryDirectory() as temporary:
            reports = Path(temporary)
            self.write_report(reports, 'required', tests=0)
            with self.assertRaisesRegex(RuntimeError, 'Zero tests executed'):
                verify_runtime.check_tests(reports, {'required'})

    def test_rejects_skipped_tests(self):
        with tempfile.TemporaryDirectory() as temporary:
            reports = Path(temporary)
            self.write_report(reports, 'required', skipped=1)
            with self.assertRaisesRegex(RuntimeError, 'skipped: required'):
                verify_runtime.check_tests(reports, {'required'})

    def test_rejects_failed_tests(self):
        with tempfile.TemporaryDirectory() as temporary:
            reports = Path(temporary)
            self.write_report(reports, 'required', failures=1)
            with self.assertRaisesRegex(RuntimeError, 'failures: required'):
                verify_runtime.check_tests(reports, {'required'})

    def test_rejects_test_errors(self):
        with tempfile.TemporaryDirectory() as temporary:
            reports = Path(temporary)
            self.write_report(reports, 'required', errors=1)
            with self.assertRaisesRegex(RuntimeError, 'errors: required'):
                verify_runtime.check_tests(reports, {'required'})

    @staticmethod
    def write_report(directory, name, tests=1, failures=0, errors=0, skipped=0):
        (directory / ('TEST-' + name + '.xml')).write_text(
            '<testsuite name="{}" tests="{}" failures="{}" errors="{}" skipped="{}"/>'.format(
                name, tests, failures, errors, skipped))

class JourneyEvidenceTests(unittest.TestCase):
    def test_accepts_all_required_http_responses(self):
        import json
        with tempfile.TemporaryDirectory() as temporary:
            directory = Path(temporary)
            fields = {'search': ('stations', 2), 'detail': ('chargers', 1),
                      'alternative': ('requiresConfirmation', 1), 'stale': ('excluded', 1),
                      'failure': ('chargers', 1), 'recovered': ('preferred', 1), 'empty': ('preferred', 0)}
            for name, (field, count) in fields.items():
                (directory / (name + '.json')).write_text(json.dumps({field: [{}] * count}))
            self.assertTrue(verify_runtime.check_journey_evidence(directory))

    def test_rejects_missing_http_journey_evidence(self):
        with tempfile.TemporaryDirectory() as temporary:
            with self.assertRaisesRegex(RuntimeError, 'Missing charging journey evidence'):
                verify_runtime.check_journey_evidence(Path(temporary))

    def test_rejects_empty_http_response_evidence(self):
        with tempfile.TemporaryDirectory() as temporary:
            directory = Path(temporary)
            for name in ('search', 'detail', 'alternative', 'stale', 'failure', 'recovered', 'empty'):
                (directory / (name + '.json')).write_text('{}')
            with self.assertRaisesRegex(RuntimeError, 'Invalid charging journey evidence'):
                verify_runtime.check_journey_evidence(directory)


class PackagedFixtureTests(unittest.TestCase):
    def test_rejects_browser_demo_in_production_jar(self):
        with self.assertRaisesRegex(RuntimeError, 'Test fixture packaged'):
            verify_runtime.check_packaged_test_fixtures(['BOOT-INF/classes/e2e/ChargingDemoApplication.class'])

    def test_rejects_reliability_probe_in_production_jar(self):
        with self.assertRaisesRegex(RuntimeError, 'Test fixture packaged'):
            verify_runtime.check_packaged_test_fixtures(['BOOT-INF/classes/reliability/RestartProbeApplication.class'])

    def test_accepts_production_classes(self):
        verify_runtime.check_packaged_test_fixtures(['BOOT-INF/classes/com/plugpass/PlugPassApplication.class'])


class FrontendEvidenceTests(unittest.TestCase):
    def test_accepts_executed_required_frontend_assertions(self):
        report = {'success': True, 'testResults': [{'name': '/repo/frontend/src/SearchForm.spec.ts',
                  'assertionResults': [{'status': 'passed'}, {'status': 'passed'}]}]}
        self.assertEqual(verify_runtime.check_frontend_tests(report, ['src/SearchForm.spec.ts']), 2)

    def test_rejects_zero_frontend_assertions(self):
        with self.assertRaisesRegex(RuntimeError, 'Zero frontend tests'):
            verify_runtime.check_frontend_tests({'success': True, 'testResults': []}, [])

    def test_rejects_missing_required_frontend_file(self):
        report = {'success': True, 'testResults': [{'name': '/repo/frontend/src/other.spec.ts',
                  'assertionResults': [{'status': 'passed'}]}]}
        with self.assertRaisesRegex(RuntimeError, 'Missing required frontend'):
            verify_runtime.check_frontend_tests(report, ['src/SearchForm.spec.ts'])

    def test_rejects_failed_frontend_assertion_even_if_summary_says_success(self):
        report = {'success': True, 'testResults': [{'name': '/repo/frontend/src/SearchForm.spec.ts',
                  'assertionResults': [{'status': 'failed'}]}]}
        with self.assertRaisesRegex(RuntimeError, 'Frontend assertion not passed'):
            verify_runtime.check_frontend_tests(report, ['src/SearchForm.spec.ts'])

    def test_rejects_skipped_frontend_assertion(self):
        report = {'success': True, 'testResults': [{'name': '/repo/frontend/src/SearchForm.spec.ts',
                  'assertionResults': [{'status': 'pending'}]}]}
        with self.assertRaisesRegex(RuntimeError, 'Frontend assertion not passed'):
            verify_runtime.check_frontend_tests(report, ['src/SearchForm.spec.ts'])


if __name__ == '__main__':
    unittest.main()
