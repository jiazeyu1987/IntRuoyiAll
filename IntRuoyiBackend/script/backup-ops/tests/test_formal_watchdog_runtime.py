"""Explicit unit runtime doubles; no filesystem freeze or server evidence."""
import importlib.util
from pathlib import Path
import subprocess
import sys
import tempfile
import unittest
from unittest.mock import patch


class NativeRuntimeGateTest(unittest.TestCase):
    def setUp(self):
        source = Path(__file__).parents[1] / 'linux' / 'formal_filesystem_watchdog.py'
        spec = importlib.util.spec_from_file_location('runtime_gate', source)
        self.module = importlib.util.module_from_spec(spec)
        spec.loader.exec_module(self.module)

    def test_old_runtime_refuses_before_managed_capture_path_or_process(self):
        with patch.object(self.module.sys, 'version_info', (3, 6, 8)), \
                patch.object(self.module, 'observe_mount') as observe, \
                patch.object(self.module.subprocess, 'Popen') as start:
            with self.assertRaisesRegex(self.module.WatchdogError, '^WATCHDOG_RUNTIME_UNSUPPORTED$'):
                self.module.capture_physical_with_native_fence('/absent/control', '/absent/mount', {},
                                                             '/absent/source', '/absent/dest', 1)
            observe.assert_not_called()
            start.assert_not_called()

    def test_missing_boottime_refuses_before_watchdog_paths(self):
        with patch.object(self.module.time, 'CLOCK_BOOTTIME', None, create=True), \
                patch.object(self.module, 'private_directory') as directory, \
                patch.object(self.module, 'write_exclusive') as record:
            with self.assertRaisesRegex(self.module.WatchdogError, '^WATCHDOG_RUNTIME_UNSUPPORTED$'):
                self.module.run_watchdog('/absent/control', '/absent/mount', {}, 1, 0)
            directory.assert_not_called()
            record.assert_not_called()

    def test_missing_linux_descriptor_flag_refuses(self):
        with patch.object(self.module.os, 'O_NOFOLLOW', None, create=True):
            with self.assertRaisesRegex(self.module.WatchdogError, '^WATCHDOG_RUNTIME_UNSUPPORTED$'):
                self.module.require_native_runtime()

    def test_real_cli_preserves_fixed_runtime_code_without_private_input(self):
        source = Path(__file__).parents[1] / 'linux' / 'formal_filesystem_watchdog.py'
        with tempfile.TemporaryDirectory() as directory:
            control = Path(directory) / 'uncreated'
            script = ('import sys,runpy; sys.version_info=(3,6,8); '
                      'sys.argv=[sys.argv[1],sys.argv[2],sys.argv[2],"1"]; '
                      'runpy.run_path(sys.argv[0],run_name="__main__")')
            result = subprocess.run([sys.executable, '-c', script, str(source), str(control)],
                                    input=b'UNUSED_PRIVATE_SENTINEL\n', capture_output=True, timeout=5)
            self.assertEqual(1, result.returncode)
            self.assertEqual(b'', result.stdout)
            self.assertEqual(b'WATCHDOG_RUNTIME_UNSUPPORTED\n', result.stderr.replace(b'\r\n', b'\n'))
            self.assertFalse(control.exists())


if __name__ == '__main__':
    unittest.main()
