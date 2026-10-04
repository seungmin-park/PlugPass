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


if __name__ == '__main__':
    unittest.main()
