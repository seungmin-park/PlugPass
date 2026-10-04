import importlib.util
from pathlib import Path
import unittest

spec = importlib.util.spec_from_file_location('performance_load', Path(__file__).parent / 'performance/load.py')
performance_load = importlib.util.module_from_spec(spec)
spec.loader.exec_module(performance_load)


class PerformanceSummaryTests(unittest.TestCase):
    def test_nearest_rank_percentiles_include_the_slowest_tail(self):
        self.assertEqual(performance_load.percentile(list(range(1, 101)), 95), 95)
        self.assertEqual(performance_load.percentile([19], 50), 19)

    def test_rejects_empty_measurement_instead_of_reporting_zero_latency(self):
        with self.assertRaisesRegex(ValueError, 'No measured requests'):
            performance_load.summarize([])

    def test_reports_route_latency_and_all_unexpected_failures(self):
        samples = [{'route': 'search', 'elapsed_ms': 10, 'error': None},
                   {'route': 'detail', 'elapsed_ms': 400, 'error': 'HTTP 503'},
                   {'route': 'search', 'elapsed_ms': 20, 'error': 'response mismatch'}]
        result = performance_load.summarize(samples)
        self.assertEqual(result['requests'], 3)
        self.assertEqual(result['unexpected_error_rate'], 2 / 3)
        self.assertEqual(result['p50_ms'], 20)
        self.assertEqual(result['p95_ms'], 400)
        self.assertEqual(result['routes']['search']['requests'], 2)
        self.assertFalse(result['latency_target_met'])
        self.assertFalse(result['error_target_met'])
