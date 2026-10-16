"""Local unit/process checks. These fixtures are never live backup acceptance."""
import copy
import hashlib
import io
import json
import os
from pathlib import Path
import subprocess
import sys
import tarfile
import tempfile
import time
import unittest

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from formal_capture_incremental import binlog_ranges, calculate_file_delta, scan_files, write_record
from formal_capture_recovery_inputs import original_bytes
from formal_snapshot_linux import FormalSnapshotError
from minimal_capture_entry import validate_completion
from minimal_capture_support import (Budget, Originals, actual_pin, document, exclusive_json,
    validate_job)
from minimal_capture_worker import failure_code, validate_image_export


class MinimalCaptureTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory(prefix='w02-minimal-capture-')
        self.root = Path(self.temp.name).resolve()
        self.config = dict(schemaVersion='minimal-source-capture-config/v1', sourceConfigRef='unit-source',
            archiveRoot=str(self.root), sourceLockPath=str(self.root/'source.lock'),
            tools=dict(docker=str(self.root/'missing-tools'/'docker'), ip=str(self.root/'missing-tools'/'ip')),
            limits=dict(workSeconds=10, cleanupSeconds=3, clientWaitSeconds=1, maxWrittenBytes=1024*1024,
                minimumFreeBytes=1, stopSeconds=1, querySeconds=1, readySeconds=1, dataMaxRows=10,
                dataMaxNodes=100, dataMaxBytes=1024, dataSeconds=1),
            source=dict(sourceIdentity='unit-source', sourceFingerprint='unit-source',
                databaseUuid='01234567-1234-1234-1234-0123456789ab', database='unit_source',
                mysqlContainerId='a'*64, minioContainerId='b'*64, minioDataPath=str(self.root/'source-minio'),
                minioMountDestination='/data', writerContainerIds=['c'*64], applicationImageIds=['sha256:'+'d'*64],
                passwordPin=self.reference(), objectSourcePin=self.reference(),
                configurationPins=[dict(role='application-config', reference=self.reference())],
                materialPins=[dict(purpose='deployment', reference=self.reference())]), parent=None)
        self.request = dict(schemaVersion='SmallAppBackupUiRequest/v1', operationId='unit-full', action='FULL',
            sourceConfigRef='unit-source', parentChainRef=None, recoveryPointRef=None, targetConfigRef=None)

    def tearDown(self): self.temp.cleanup()

    def reference(self): return dict(path=str(self.root/'unused-input'), bytes=1, sha256='0'*64)

    def expect_code(self, expected, fn):
        with self.assertRaises(FormalSnapshotError) as error: fn()
        self.assertEqual(expected, error.exception.code)

    def test_minimum_full_accepts_no_signing_or_lifecycle_inputs(self):
        self.assertEqual(self.root/'unit-full', validate_job(self.config, self.request))

    def test_seven_field_request_is_unchanged_and_restore_is_not_capture(self):
        for change in ({'action': 'RESTORE'}, {'targetConfigRef': 'target'}, {'extra': True}):
            request = dict(self.request, **change)
            self.expect_code('MINIMAL_CAPTURE_REQUEST_INVALID', lambda: validate_job(self.config, request))

    def test_full_rejects_parent(self):
        self.request['parentChainRef'] = 'old-chain'
        self.expect_code('MINIMAL_FULL_PARENT_REFUSED', lambda: validate_job(self.config, self.request))

    def test_incremental_requires_explicit_matching_parent(self):
        self.request.update(action='INCREMENTAL', parentChainRef='parent-chain')
        self.expect_code('MINIMAL_INCREMENTAL_PARENT_REQUIRED', lambda: validate_job(self.config, self.request))
        self.config['parent'] = dict(parentChainRef='parent-chain', capturePointPin=self.reference(),
            repositoryPointPin=self.reference(), objectContentIndexPin=self.reference())
        validate_job(self.config, self.request)

    def test_explicit_source_selection_and_container_ids(self):
        self.request['sourceConfigRef'] = 'other'
        self.expect_code('MINIMAL_CAPTURE_CONFIG_INVALID', lambda: validate_job(self.config, self.request))
        self.request['sourceConfigRef'] = 'unit-source'
        self.config['source']['writerContainerIds'] = [self.config['source']['mysqlContainerId']]
        self.expect_code('MINIMAL_CONTAINER_ID_INVALID', lambda: validate_job(self.config, self.request))

    def test_operation_id_cannot_escape_archive_root(self):
        self.request['operationId'] = '../outside'
        self.expect_code('MINIMAL_CAPTURE_REQUEST_INVALID', lambda: validate_job(self.config, self.request))

    def test_limits_are_finite_positive_and_not_boolean(self):
        self.config['limits']['workSeconds'] = True
        self.expect_code('MINIMAL_LIMITS_INVALID', lambda: validate_job(self.config, self.request))

    def test_exclusive_output_does_not_overwrite(self):
        path = self.root/'record.json'; pin = exclusive_json(path, {'original': 1})
        with self.assertRaises(FileExistsError): exclusive_json(path, {'replacement': 1})
        self.assertEqual({'original': 1}, document(path)[0])
        self.assertEqual(pin, actual_pin(path))

    def test_pinned_original_rejects_content_replacement(self):
        path = self.root/'material'; path.write_bytes(b'original')
        pin = actual_pin(path); originals = Originals(lambda: None)
        self.assertEqual(b'original', originals.read(pin))
        path.write_bytes(b'changed!')
        with self.assertRaises(FormalSnapshotError): originals.check()

    def test_duplicate_json_keys_are_rejected(self):
        path = self.root/'invalid.json'; path.write_bytes(b'{"a":1,"a":2}')
        with self.assertRaises(FormalSnapshotError): document(path)

    def test_budget_limits_cumulative_bytes_and_output_scope(self):
        limits = dict(self.config['limits'], maxWrittenBytes=3)
        budget = Budget(self.root, limits)
        with budget.open(self.root/'one', 'config') as sink: sink.write(b'123')
        with budget.open(self.root/'two', 'config') as sink:
            self.expect_code('MINIMAL_WRITE_LIMIT', lambda: sink.write(b'4'))
        self.expect_code('MINIMAL_OUTPUT_SCOPE_INVALID', lambda: budget.target(self.root.parent/'outside'))

    def test_budget_exclusive_dump_publication_preserves_pin(self):
        budget = Budget(self.root, self.config['limits'])
        with budget.open(self.root/'pending', 'sql-gzip') as sink: sink.write(b'compressed-data')
        budget.link(self.root/'pending', self.root/'database.sql.gz')
        budget.unlink_complete(self.root/'pending')
        self.assertEqual(1, (self.root/'database.sql.gz').stat().st_nlink)
        self.assertEqual(b'compressed-data', original_bytes(actual_pin(self.root/'database.sql.gz')))

    def test_written_records_use_existing_capture_canonical_format(self):
        with Budget(self.root, self.config['limits']).activate():
            pin = write_record(self.root/'point.json', {'z': 2, 'a': 1})
        self.assertEqual(b'{"a":1,"z":2}\n', original_bytes(pin))

    def test_real_filesystem_delta_keeps_changed_and_deleted_paths(self):
        source = self.root/'files'; source.mkdir(); (source/'dir').mkdir()
        (source/'dir'/'same').write_bytes(b'same'); (source/'delete').write_bytes(b'old')
        (source/'change').write_bytes(b'before'); before = scan_files(source, guard=lambda: None)
        (source/'delete').unlink(); (source/'change').write_bytes(b'after'); (source/'new').write_bytes(b'new')
        after = scan_files(source, guard=lambda: None)
        changed, deleted = calculate_file_delta(before, after)
        self.assertEqual({'change', 'new'}, set(changed)); self.assertEqual(['delete'], deleted)

    def test_binlog_continuity_rejects_missing_rotation(self):
        start = dict(databaseUuid=self.config['source']['databaseUuid'], serverVersion='8.0.39',
            file='mysql-bin.000001', position=4, executedGtidSet='')
        end = dict(start, file='mysql-bin.000003', position=20)
        with self.assertRaises(FormalSnapshotError):
            binlog_ranges([dict(file='mysql-bin.000001', bytes=100), dict(file='mysql-bin.000003', bytes=20)], start, end)

    def test_image_export_validates_actual_config_digest(self):
        image = b'{"architecture":"amd64","os":"linux"}'
        checksum = hashlib.sha256(image).hexdigest(); path = self.root/'images.tar'
        with tarfile.open(path, 'w') as archive:
            for name, raw in (('manifest.json', json.dumps([dict(Config=checksum+'.json', Layers=[])]).encode()),
                    (checksum+'.json', image)):
                member = tarfile.TarInfo(name); member.size = len(raw); archive.addfile(member, io.BytesIO(raw))
        validate_image_export(path, ['sha256:'+checksum], lambda: None)
        self.expect_code('MINIMAL_IMAGE_EXPORT_CHANGED', lambda: validate_image_export(path,
            ['sha256:'+'f'*64], lambda: None))

    def test_no_completion_file_cannot_be_success(self):
        with self.assertRaises(FileNotFoundError): validate_completion(self.root, self.request, time.monotonic()+5)

    def test_expired_output_validation_is_not_success(self):
        self.expect_code('MINIMAL_OUTPUT_VALIDATION_TIMEOUT', lambda: validate_completion(
            self.root, self.request, time.monotonic()-1))

    def test_exception_diagnostics_do_not_emit_secret_text(self):
        self.assertEqual('MINIMAL_CAPTURE_INTERNAL_ERROR', failure_code(RuntimeError('secret-value')))

    def test_real_cli_child_failure_query_and_no_replay(self):
        # On Linux the deliberately absent tool path blocks before Docker; on Windows
        # the native-runtime refusal occurs before capture. No live service is touched.
        config_path = self.root/'config.json'; request_path = self.root/'request.json'
        exclusive_json(config_path, self.config); exclusive_json(request_path, self.request)
        entry = Path(__file__).resolve().parents[1]/'minimal_capture_entry.py'
        def run(output, request=request_path, configuration=config_path):
            return subprocess.run([sys.executable, '-B', str(entry), '--configuration', str(configuration),
                '--request', str(request), '--output', str(output)], capture_output=True, timeout=15)
        first = run(self.root/'first.json')
        self.assertIn(first.returncode, (1, 3), first.stderr.decode())
        result = document(self.root/'first.json')[0]
        for index in range(20):
            if result['state'] == 'FAILED_RETAINED': break
            time.sleep(.1)
            attempt = run(self.root/('query-%s.json' % index))
            self.assertIn(attempt.returncode, (1, 3), attempt.stderr.decode())
            result = document(self.root/('query-%s.json' % index))[0]
        self.assertEqual('FAILED_RETAINED', result['state']); self.assertEqual(1, result['actualExitCode'])
        self.assertEqual(self.request['operationId'], result['requestId'])
        original_job = (self.root/'unit-full'/'job-started.json').read_bytes()
        duplicate_request = self.root/'request-copy.json'; exclusive_json(duplicate_request, self.request)
        query = run(self.root/'same-operation-new-request-path.json', duplicate_request)
        self.assertEqual(1, query.returncode, query.stderr.decode())
        self.assertEqual(original_job, (self.root/'unit-full'/'job-started.json').read_bytes())
        changed = copy.deepcopy(self.config); changed['limits']['maxWrittenBytes'] += 1
        changed_path = self.root/'changed-config.json'; exclusive_json(changed_path, changed)
        reused = run(self.root/'reused-operation.json', configuration=changed_path)
        self.assertEqual(2, reused.returncode); self.assertIn(b'MINIMAL_OPERATION_ID_REUSED', reused.stderr)
        existing = (self.root/'first.json').read_bytes()
        refused = run(self.root/'first.json')
        self.assertEqual(2, refused.returncode); self.assertEqual(existing, (self.root/'first.json').read_bytes())


if __name__ == '__main__': unittest.main()
