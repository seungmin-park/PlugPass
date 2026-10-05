#!/usr/bin/env python3
"""Use the existing runtime guard for Playwright's executed JSON results."""
import importlib.util
import json
from pathlib import Path
import sys

root = Path(__file__).resolve().parents[1]
spec = importlib.util.spec_from_file_location('verify_runtime', Path(__file__).with_name('verify-runtime.py'))
verify_runtime = importlib.util.module_from_spec(spec)
spec.loader.exec_module(verify_runtime)
report_path = Path(sys.argv[1]) if len(sys.argv) > 1 else root / 'frontend/test-results/e2e.json'
required_path = Path(sys.argv[2]) if len(sys.argv) > 2 else root / 'docs/required-browser-tests.json'
count = verify_runtime.check_browser_tests(json.loads(report_path.read_text()), json.loads(required_path.read_text()))
print(json.dumps({'browser_tests': count, 'failures': 0, 'errors': 0, 'skipped': 0}), flush=True)
