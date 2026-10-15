"""Exercise the public iOS build command; only Apple's/device tools are replaced."""
import json
import os
from pathlib import Path
import shutil
import subprocess
import sys
import tempfile
import unittest


class IosInstallTests(unittest.TestCase):
    def setUp(self):
        self.directory = tempfile.TemporaryDirectory()
        self.addCleanup(self.directory.cleanup)
        self.root = Path(self.directory.name)
        (self.root / 'scripts').mkdir()
        (self.root / 'frontend').mkdir()
        self.bin = self.root / 'bin'
        self.bin.mkdir()
        shutil.copy(Path(__file__).with_name('build-ios.sh'), self.root / 'scripts/build-ios.sh')
        self.calls_path = self.root / 'calls.jsonl'
        self.environment = {
            'PATH': str(self.bin) + os.pathsep + os.defpath,
            'PLUGPASS_TEST_CALLS': str(self.calls_path),
        }
        tool_source = f'#!{sys.executable}\n' + '''import json, os, pathlib, sys
name = pathlib.Path(sys.argv[0]).name
args = sys.argv[1:]
if name == 'xcodebuild' and args == ['-version']:
    print('Xcode 27.0\\nBuild version 27A266a')
    sys.exit(0)
with open(os.environ['PLUGPASS_TEST_CALLS'], 'a') as log:
    log.write(json.dumps({'name': name, 'args': args}) + '\\n')
operation = name
if name == 'xcrun':
    operation = 'install' if args[2:4] == ['install', 'app'] else 'launch'
if operation == os.environ.get('PLUGPASS_TEST_FAILURE'):
    print('controlled external failure', file=sys.stderr)
    sys.exit(17)
if name == 'xcodebuild':
    destination = pathlib.Path(args[args.index('-derivedDataPath') + 1])
    (destination / 'Build/Products/Debug-iphoneos/App.app').mkdir(parents=True)
'''
        for name in ('npm', 'npx', 'xcodebuild', 'codesign', 'xcrun'):
            executable = self.bin / name
            executable.write_text(tool_source)
            executable.chmod(0o755)

    def run_build(self, mode, settings):
        return subprocess.run(
            ['bash', str(self.root / 'scripts/build-ios.sh'), mode],
            cwd=self.root, env={**self.environment, **settings},
            text=True, capture_output=True, check=False,
        )

    def calls(self):
        if not self.calls_path.exists():
            return []
        return [json.loads(line) for line in self.calls_path.read_text().splitlines()]

    def test_install_requires_an_explicit_device_before_building(self):
        result = self.run_build('install', {})

        self.assertNotEqual(result.returncode, 0)
        self.assertIn('PLUGPASS_IOS_DEVICE_ID', result.stderr)
        self.assertEqual(self.calls(), [])

    def test_install_builds_and_verifies_a_signed_app_before_launching_on_selected_device(self):
        result = self.run_build('install', {
            'PLUGPASS_IOS_DEVICE_ID': 'test-connected-device',
            'PLUGPASS_IOS_TEAM_ID': 'TESTTEAM01',
        })

        self.assertEqual(result.returncode, 0, result.stderr)
        calls = self.calls()
        self.assertEqual([call['name'] for call in calls],
                         ['npm', 'npx', 'xcodebuild', 'codesign', 'xcrun', 'xcrun'])
        build_args = calls[2]['args']
        self.assertEqual(build_args[build_args.index('-destination') + 1], 'id=test-connected-device')
        self.assertIn('CODE_SIGNING_ALLOWED=YES', build_args)
        self.assertIn('DEVELOPMENT_TEAM=TESTTEAM01', build_args)
        self.assertIn('-allowProvisioningUpdates', build_args)
        self.assertIn('-allowProvisioningDeviceRegistration', build_args)
        app = 'build/native/ios-install/Build/Products/Debug-iphoneos/App.app'
        self.assertEqual(calls[3]['args'], ['--verify', '--deep', '--strict', app])
        self.assertEqual(calls[4]['args'],
                         ['devicectl', 'device', 'install', 'app', '--device', 'test-connected-device', app])
        self.assertEqual(calls[5]['args'],
                         ['devicectl', 'device', 'process', 'launch', '--device', 'test-connected-device',
                          '--terminate-existing', 'org.duckdns.plugpass.dev'])
        self.assertIn('설치·실행', result.stdout)

    def test_build_failure_never_installs_an_old_app(self):
        result = self.run_build('install', {
            'PLUGPASS_IOS_DEVICE_ID': 'test-connected-device', 'PLUGPASS_TEST_FAILURE': 'xcodebuild',
        })

        self.assertEqual(result.returncode, 17)
        self.assertNotIn('xcrun', [call['name'] for call in self.calls()])

    def test_invalid_signature_never_installs(self):
        result = self.run_build('install', {
            'PLUGPASS_IOS_DEVICE_ID': 'test-connected-device', 'PLUGPASS_TEST_FAILURE': 'codesign',
        })

        self.assertEqual(result.returncode, 17)
        self.assertNotIn('xcrun', [call['name'] for call in self.calls()])

    def test_install_failure_never_launches(self):
        result = self.run_build('install', {
            'PLUGPASS_IOS_DEVICE_ID': 'test-connected-device', 'PLUGPASS_TEST_FAILURE': 'install',
        })

        self.assertEqual(result.returncode, 17)
        self.assertEqual([call['args'][2:4] for call in self.calls() if call['name'] == 'xcrun'],
                         [['install', 'app']])

    def test_launch_failure_is_reported_as_failure(self):
        result = self.run_build('install', {
            'PLUGPASS_IOS_DEVICE_ID': 'test-connected-device', 'PLUGPASS_TEST_FAILURE': 'launch',
        })

        self.assertEqual(result.returncode, 17)
        self.assertNotIn('설치·실행', result.stdout)

    def test_device_compile_remains_unsigned_without_device_or_team(self):
        result = self.run_build('device', {})

        self.assertEqual(result.returncode, 0, result.stderr)
        calls = self.calls()
        self.assertEqual([call['name'] for call in calls], ['npm', 'npx', 'xcodebuild'])
        self.assertIn('CODE_SIGNING_ALLOWED=NO', calls[2]['args'])
        self.assertNotIn('-allowProvisioningUpdates', calls[2]['args'])
