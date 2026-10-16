"""Direct local checks of new diagnostics; no Docker/remote/business execution."""
import contextlib
import io
import json
import subprocess
import unittest
from unittest.mock import patch

import test_minimal_mount_order as recorded
from formal_snapshot_linux import FormalSnapshotError
from minimal_capture_worker import Runtime


class CaptureDiagnosticTests(unittest.TestCase):
    def setUp(self):
        self.fixture = recorded.MountOrderRegressionTests(methodName='runTest')
        self.fixture.setUp(); self.addCleanup(self.fixture.doCleanups)
        self.container = self.fixture.writers[0]['id']; self.runs = 0

    def runtime(self):
        runtime = self.fixture.runtime(); self.runs += 1
        runtime.root = self.fixture.root/('run-%d' % self.runs); runtime.root.mkdir()
        runtime.budget = recorded.Budget(runtime.root, runtime.limits)
        return runtime

    def failure(self, runtime, code, fn):
        with self.assertRaises(FormalSnapshotError) as error: fn()
        self.assertEqual(code, error.exception.code)
        events = [json.loads(path.read_text(encoding='utf-8')) for path in sorted(runtime.root.glob('event-*.json'))]
        event = [row for row in events if row['stage'] == 'CAPTURE_CONDITION_FAILED'][-1]
        self.assertEqual(code, event['failureCode'])
        return event

    def test_guard_names_failed_predicates_and_distinguishes_order_only(self):
        runtime = self.runtime(); runtime.phase = 'DATABASE_CAPTURE'
        row = runtime.rows[self.container]
        row.update(image='sha256:'+'f'*64, running=True, paused=True); row['mounts'].reverse()
        event = self.failure(runtime, 'MINIMAL_WRITER_RESUMED', lambda: runtime.protected(force=True))
        self.assertEqual('DATABASE_CAPTURE', event['captureStage'])
        self.assertEqual(['writer_running_false', 'writer_paused_false', 'image_id_matches_initial'], event['failedConditions'])
        self.assertEqual(self.container, event['expected']['id'])
        self.assertEqual(row['id'], event['observed']['id'])
        self.assertIs(False, event['expected']['running']); self.assertIs(False, event['expected']['paused'])
        self.assertEqual(row['image'], event['observed']['image'])
        self.assertEqual(dict(completeRecordsEqual=True, outerOrderEqual=False, outerOrderOnly=True,
            removed=[], added=[]), event['mountDifference'])

    def test_full_mount_snapshots_diff_multiplicity_and_private_fields_are_redacted(self):
        runtime = self.runtime(); row = runtime.rows[self.container]
        secret = 'UNIT_PRIVATE_SENTINEL_3c9a'
        row['mounts'][0].update(Mode='password='+secret+' with spaces',
            Options={'token': secret, 'extra': [secret]},
            Source='smb://user:'+secret+'@host/path?token='+secret)
        row['mounts'].append(row['mounts'][1].copy())
        runtime.config['privateConfiguration'] = {'password': secret}
        event = self.failure(runtime, 'MINIMAL_WRITER_RESUMED', lambda: runtime.protected(force=True))
        self.assertEqual(['complete_mount_records_match_initial'], event['failedConditions'])
        self.assertFalse(event['mountDifference']['completeRecordsEqual'])
        self.assertFalse(event['mountDifference']['outerOrderOnly'])
        self.assertEqual(2, len(event['expected']['mounts'])); self.assertEqual(3, len(event['observed']['mounts']))
        self.assertEqual(1, len(event['mountDifference']['removed'])); self.assertEqual(2, len(event['mountDifference']['added']))
        self.assertEqual({'token': '[REDACTED]', 'extra': ['[REDACTED]']}, event['observed']['mounts'][0]['Options'])
        self.assertNotIn(secret, json.dumps(event)); self.assertNotIn('privateConfiguration', event)

    def test_restore_failure_retains_diagnostic_in_original_restoration_result(self):
        runtime = self.runtime(); runtime.touched = [self.container]
        runtime.rows[self.container]['mounts'][0]['RW'] = not runtime.rows[self.container]['mounts'][0]['RW']
        with self.assertRaises(FormalSnapshotError) as error: runtime.restore()
        self.assertEqual('MINIMAL_SOURCE_RESTORATION_FAILED', error.exception.code)
        result = json.loads((runtime.root/'source-restoration.json').read_text(encoding='utf-8'))
        self.assertIs(False, result['restored']); self.assertEqual([], runtime.starts)
        row = result['errors'][0]
        self.assertEqual('WRITER_RESTORED', row['step'])
        self.assertEqual('MINIMAL_SOURCE_CONTAINER_REPLACED', row['failureCode'])
        self.assertEqual('WRITER_RESTORED', row['diagnostic']['captureStage'])
        self.assertEqual(['complete_mount_records_match_initial'], row['diagnostic']['failedConditions'])
        self.assertEqual(self.container, row['diagnostic']['observed']['id'])

    def test_identity_and_start_confirmation_report_the_original_refusals(self):
        runtime = self.runtime(); runtime.rows[self.container]['id'] = '0'*64
        event = self.failure(runtime, 'MINIMAL_CONTAINER_CHANGED', lambda: runtime.protected(force=True))
        self.assertEqual(['container_id_matches_requested'], event['failedConditions'])
        self.assertEqual('0'*64, event['observed']['id'])
        runtime = self.runtime(); runtime.start_succeeds = False; runtime.phase = 'WRITER_RESTORED'
        event = self.failure(runtime, 'MINIMAL_SOURCE_START_UNPROVEN', lambda: runtime.restore_container(self.container))
        self.assertEqual(['started_running_true'], event['failedConditions'])
        self.assertIs(True, event['expected']['running']); self.assertIs(False, event['observed']['running'])

    def test_command_records_actual_exit_codes_without_raw_stderr(self):
        runtime = self.runtime(); runtime.phase = 'STOP_WRITERS'
        secret = b'UNIT_STDERR_SECRET_7d21'
        result = subprocess.CompletedProcess([], 23, b'', b'No such container '+secret)
        with patch('minimal_capture_worker.subprocess.run', return_value=result):
            event = self.failure(runtime, 'MINIMAL_DOCKER_COMMAND_FAILED', lambda: Runtime.command(
                runtime, ['stop', '--time', '1', '--', self.container]))
        self.assertEqual(23, event['actualExitCode']); self.assertTrue(event['exitObserved'])
        self.assertEqual('DOCKER_CONTAINER_NOT_FOUND', event['errorSummary'])
        self.assertEqual(['docker_exit_zero', 'docker_stderr_empty'], event['failedConditions'])
        self.assertNotIn(secret.decode(), json.dumps(event))
        runtime.phase = 'WRITER_RESTORED'
        with patch('minimal_capture_worker.subprocess.run', return_value=subprocess.CompletedProcess([], 0, b'container', b'')):
            Runtime.command(runtime, ['start', '--', self.container], cleanup=True)
        events = [json.loads(path.read_text(encoding='utf-8')) for path in runtime.root.glob('event-*.json')]
        completed = [row for row in events if row['stage'] == 'CONTAINER_COMMAND_EXIT'][-1]
        self.assertEqual(0, completed['actualExitCode']); self.assertEqual('start', completed['command'])
        self.assertEqual('WRITER_RESTORED', completed['captureStage']); self.assertTrue(completed['cleanup'])

    def test_timeout_and_spawn_error_have_no_invented_exit_or_exception_text(self):
        secret = 'UNIT_EXCEPTION_SECRET_1d0e'
        runtime = self.runtime()
        with patch('minimal_capture_worker.subprocess.run', side_effect=subprocess.TimeoutExpired(
                ['docker'], 1, stderr=secret.encode())):
            event = self.failure(runtime, 'MINIMAL_DOCKER_TIMEOUT', lambda: Runtime.command(runtime, ['stop', '--', self.container]))
        self.assertIsNone(event['actualExitCode']); self.assertFalse(event['exitObserved'])
        self.assertNotIn(secret, json.dumps(event))
        runtime = self.runtime()
        with patch('minimal_capture_worker.subprocess.run', side_effect=PermissionError(13, secret)):
            event = self.failure(runtime, 'MINIMAL_DOCKER_START_FAILED', lambda: Runtime.command(runtime, ['start', '--', self.container]))
        self.assertIsNone(event['actualExitCode']); self.assertEqual(13, event['errno'])
        self.assertNotIn(secret, json.dumps(event))

    def test_diagnostic_write_failure_does_not_replace_the_guard_failure(self):
        runtime = self.runtime(); runtime.rows[self.container]['running'] = True
        output = io.StringIO()
        with patch.object(runtime, 'event', side_effect=PermissionError('UNIT_WRITE_SECRET')), contextlib.redirect_stderr(output):
            with self.assertRaises(FormalSnapshotError) as error: runtime.protected(force=True)
        self.assertEqual('MINIMAL_WRITER_RESUMED', error.exception.code)
        self.assertEqual('MINIMAL_PERMISSION_DENIED', error.exception.diagnostic['diagnosticWriteFailureCode'])
        self.assertIn('MINIMAL_DIAGNOSTIC_WRITE_FAILED', output.getvalue())
        self.assertNotIn('UNIT_WRITE_SECRET', output.getvalue())


if __name__ == '__main__': unittest.main()
