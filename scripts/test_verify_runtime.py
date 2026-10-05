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


class BrowserEvidenceTests(unittest.TestCase):
    def report(self, status='passed', expected_status='passed'):
        return {'errors': [], 'suites': [{'specs': [{'file': 'journey.spec.ts', 'ok': True,
            'tests': [{'expectedStatus': expected_status, 'status': 'expected',
                       'results': [{'status': status, 'errors': [], 'retry': 0}]}]}]}]}

    def test_accepts_real_passed_browser_result(self):
        self.assertEqual(verify_runtime.check_browser_tests(self.report(), ['journey.spec.ts']), 1)

    def test_rejects_zero_browser_tests(self):
        with self.assertRaisesRegex(RuntimeError, 'Zero browser tests'):
            verify_runtime.check_browser_tests({'suites': []}, [])

    def test_rejects_missing_required_browser_spec(self):
        with self.assertRaisesRegex(RuntimeError, 'Missing required browser'):
            verify_runtime.check_browser_tests(self.report(), ['journey.spec.ts', 'missing.spec.ts'])

    def test_rejects_skipped_browser_test(self):
        with self.assertRaisesRegex(RuntimeError, 'Browser result not passed'):
            verify_runtime.check_browser_tests(self.report('skipped'), ['journey.spec.ts'])

    def test_rejects_failed_browser_test_despite_ok_summary(self):
        with self.assertRaisesRegex(RuntimeError, 'Browser result not passed'):
            verify_runtime.check_browser_tests(self.report('failed'), ['journey.spec.ts'])

    def test_rejects_browser_execution_error(self):
        report = self.report()
        report['errors'] = [{'message': 'worker failed'}]
        with self.assertRaisesRegex(RuntimeError, 'Browser runner errors'):
            verify_runtime.check_browser_tests(report, ['journey.spec.ts'])

    def test_rejects_expected_failure_as_success(self):
        with self.assertRaisesRegex(RuntimeError, 'Browser expected status not passed'):
            verify_runtime.check_browser_tests(self.report('failed', 'failed'), ['journey.spec.ts'])

    def test_rejects_collected_but_unexecuted_test(self):
        report = self.report()
        report['suites'][0]['specs'][0]['tests'][0]['results'] = []
        with self.assertRaisesRegex(RuntimeError, 'Browser test not executed'):
            verify_runtime.check_browser_tests(report, ['journey.spec.ts'])

    def test_checks_nested_describe_suites(self):
        report = {'suites': [{'suites': self.report()['suites']}]}
        self.assertEqual(verify_runtime.check_browser_tests(report, ['journey.spec.ts']), 1)

    def test_rejects_retry_that_masks_first_failure(self):
        report = self.report()
        report['suites'][0]['specs'][0]['tests'][0]['results'].insert(0, {'status': 'failed', 'errors': [], 'retry': 0})
        with self.assertRaisesRegex(RuntimeError, 'Browser test repeated'):
            verify_runtime.check_browser_tests(report, ['journey.spec.ts'])


if __name__ == '__main__':
    unittest.main()
