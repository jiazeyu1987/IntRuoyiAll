"""Local algorithm/interface checks; no native/NAS or disaster recovery claim."""
import copy
import base64
import hashlib
import json
import os
from pathlib import Path
import sys
import tempfile
import time
import unittest

sys.path.insert(0, str(Path(__file__).parents[1]/'linux'))
import formal_capture_incremental as capture
import formal_normal_owner as owner
import formal_normal_owner_entry as entry
import formal_snapshot_linux as snapshot
import formal_normal_owner_assembly as assembly
import formal_native_owner_transport as transport
import formal_capture_failure_recovery as recovery
import formal_capture_recovery_inputs as materials

UUID = '11111111-1111-1111-1111-111111111111'


def position(file, offset, gtids=''):
    return dict(databaseUuid=UUID, serverVersion='8.0.35', file=file, position=offset, executedGtidSet=gtids)


class CaptureSeriesContractTests(unittest.TestCase):
    def test_real_incremental_range_excludes_parent_prefix(self):
        logs = [dict(file='mysql-bin.000041', bytes=900), dict(file='mysql-bin.000042', bytes=400)]
        actual = capture.binlog_ranges(logs, position(logs[0]['file'], 650, UUID+':1-5'),
                                      position(logs[1]['file'], 200, UUID+':1-7'))
        self.assertEqual(actual, [dict(file=logs[0]['file'], startPosition=650, stopPosition=900),
                                  dict(file=logs[1]['file'], startPosition=4, stopPosition=200)])

    def test_empty_delta_does_not_request_full_backup(self):
        point = position('mysql-bin.000001', 800)
        self.assertEqual(capture.binlog_ranges([dict(file=point['file'], bytes=800)], point, point), [])

    def test_purged_parent_and_log_gap_refuse(self):
        start, end = position('mysql-bin.000041', 400), position('mysql-bin.000043', 150)
        for logs in ([dict(file=end['file'], bytes=300)],
                     [dict(file=start['file'], bytes=900), dict(file=end['file'], bytes=300)]):
            with self.assertRaisesRegex(snapshot.FormalSnapshotError, 'BINLOG_GAP'):
                capture.binlog_ranges(logs, start, end)

    def test_server_identity_or_gtid_regression_refuse(self):
        logs = [dict(file='mysql-bin.000001', bytes=500)]
        start = position(logs[0]['file'], 100, UUID+':1-10')
        for end in (position(logs[0]['file'], 200, UUID+':1-9'),
                    {**position(logs[0]['file'], 200), 'databaseUuid': '22222222-2222-2222-2222-222222222222'}):
            with self.assertRaises(snapshot.FormalSnapshotError): capture.binlog_ranges(logs, start, end)

    def test_boolean_position_and_overlapping_gtids_refuse(self):
        with self.assertRaises(snapshot.FormalSnapshotError): capture.validate_position(position('mysql-bin.000001', True))
        with self.assertRaisesRegex(snapshot.FormalSnapshotError, 'GTID_INVALID'):
            capture.gtid_intervals(UUID+':1-5:5-7')

    def test_file_delta_has_only_changed_content_and_real_deletions(self):
        same = dict(kind='file', bytes=3, sha256='a'*64)
        old = {'keep': same, 'update': same, 'gone': same}
        current = {'keep': same, 'update': dict(kind='file', bytes=4, sha256='b'*64), 'added': same}
        changed, removed = capture.calculate_file_delta(old, current)
        self.assertEqual(set(changed), {'update', 'added'}); self.assertEqual(removed, ['gone'])

    def test_file_directory_replacement_requires_removal_first(self):
        item = dict(kind='file', bytes=1, sha256='a'*64)
        old = {'x': dict(kind='directory'), 'x/child': item, 'y': item}
        current = {'x': item, 'y': dict(kind='directory'), 'y/child': item}
        changed, removed = capture.calculate_file_delta(old, current)
        self.assertEqual(removed, ['x/child', 'x', 'y']); self.assertEqual(set(changed), {'x', 'y', 'y/child'})

    def test_inventory_escape_and_missing_parent_refuse(self):
        item = dict(kind='file', bytes=0, sha256='a'*64)
        for value in ({'../x': item}, {'a//b': item}, {'a/b': item}, {'a\\b': item}):
            with self.assertRaises(snapshot.FormalSnapshotError): capture.validate_file_inventory(value)

    def test_actual_file_scan_detects_changed_content(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory).resolve(); (root/'nested').mkdir(); (root/'nested/a').write_bytes(b'abc')
            inventory = capture.scan_files(root, guard=lambda: None)
            self.assertEqual(inventory['nested/a']['sha256'], hashlib.sha256(b'abc').hexdigest())
            (root/'nested/a').write_bytes(b'changed')
            self.assertNotEqual(capture.scan_files(root, guard=lambda: None), inventory)

    @unittest.skipUnless(os.name == 'posix', 'Linux fstat/path identity requires the native runtime')
    def test_native_pinned_bytes_detect_changes(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory).resolve()
            path = root/'parent.json'; path.write_bytes(b'{"value":1}')
            reference = dict(path=str(path), bytes=path.stat().st_size, sha256=hashlib.sha256(path.read_bytes()).hexdigest())
            self.assertEqual(capture.read_pin(reference), {'value': 1})
            path.write_bytes(b'{"value":2}')
            with self.assertRaisesRegex(snapshot.FormalSnapshotError, 'PARENT_CHANGED'): capture.read_pin(reference)

    def test_manifest_read_matches_frozen_fields(self):
        reference = dict(path='/original/manifest', bytes=100, sha256='a'*64)
        raw = owner.canonical(dict(schemaVersion='release-production-backup-manifest-read/v1', manifestSha256='a'*64, offset=32))
        self.assertEqual(owner.validate_manifest_read(raw, reference), 32)
        with self.assertRaises(snapshot.FormalSnapshotError):
            owner.validate_manifest_read(owner.canonical(dict(schemaVersion='release-production-backup-manifest-read/v1',
                binding={}, manifestReference=reference, offset=32)), reference)

    def test_control_replay_and_guard_after_archive_switch_refuse(self):
        binding = {k: 'a'*64 for k in owner.BINDING_FIELDS}; binding['releaseTag'] = 'unit-contract'
        ref = dict(path='/original/request', bytes=1, sha256='b'*64)
        request = dict(schemaVersion=owner.CONTROL_SCHEMA, binding=binding, sequence=1, action='guard', payloadReference=None,
                       budget=dict(remainingMilliseconds=1000, observationReference=ref))
        control = owner.ControlSequence(binding); control.accept(owner.canonical(request), 'BOUND_READONLY')
        with self.assertRaisesRegex(snapshot.FormalSnapshotError, 'SEQUENCE_INVALID'):
            control.accept(owner.canonical(request), 'BOUND_READONLY')
        request['sequence'] = 2
        with self.assertRaisesRegex(snapshot.FormalSnapshotError, 'SEQUENCE_INVALID'):
            control.accept(owner.canonical(request), 'ARCHIVE_SERVING')

    def test_full_round_trip_is_subtracted_and_boolean_budget_refuses(self):
        now = time.monotonic()
        self.assertAlmostEqual(owner.remaining_deadline(5000, now-2, now, now+10), now+3, places=4)
        with self.assertRaises(snapshot.FormalSnapshotError): owner.remaining_deadline(True, now, now, now+10)

    def test_old_author_identity_is_not_reused(self):
        current = dict(taskId=entry.TASK_ID, moduleGroup=entry.MODULE_GROUP, ownerThreadId=entry.OWNER_THREAD_ID)
        contract = dict(schemaVersion='backup-restore-execution-contract/v1', taskId='20261010-backup-restore-modular-development',
                        status='frozen_for_code_development', assignments=[dict(group='W02', taskId=entry.TASK_ID,
                        functionIds=sorted(entry.FUNCTION_IDS))])
        creation = dict(schemaVersion='backup-restore-thread-creation-results/v1', records=[dict(moduleId='W02', taskId=entry.TASK_ID,
                        threadId=entry.OWNER_THREAD_ID)])
        entry.validate_attribution(contract, creation, current)
        with self.assertRaisesRegex(snapshot.FormalSnapshotError, 'AUTHORITY_INVALID'):
            entry.validate_attribution(contract, creation, {**current, 'ownerThreadId': 'historical-author'})

    def test_capture_writer_requires_original_budget(self):
        with self.assertRaisesRegex(snapshot.FormalSnapshotError, 'OUTPUT_ADMISSION_REQUIRED'):
            capture._writer(Path('unused'), 'proof')

    def test_native_observation_rejects_replay_and_changed_payload(self):
        raw = owner.canonical({'observed': 'ORIGINAL'})
        reference = dict(path='/control/original.json', bytes=len(raw), sha256=owner.sha(raw))
        reply = dict(schemaVersion='backup-native-observation-reply/v1', nonce='n', sequence=3,
                     bindingSha256='a'*64, reference=reference, payloadBase64=base64.b64encode(raw).decode('ascii'))
        arguments = dict(nonce='n', sequence=3, binding_sha256='a'*64, expected_reference=reference)
        self.assertEqual(transport.validate_reply(owner.canonical(reply), **arguments), (raw, reference))
        for change in ({'nonce': 'old'}, {'sequence': 2}, {'sequence': True},
                       {'bindingSha256': 'b'*64}, {'payloadBase64': base64.b64encode(b'changed').decode('ascii')}):
            with self.assertRaises(snapshot.FormalSnapshotError):
                transport.validate_reply(owner.canonical({**reply, **change}), **arguments)

    def test_native_observation_rejects_invalid_base64_and_extra_fields(self):
        reply = dict(schemaVersion='backup-native-observation-reply/v1', nonce='n', sequence=1,
                     bindingSha256='a'*64, reference=dict(path='/control/proof', bytes=1, sha256=owner.sha(b'x')),
                     payloadBase64='%%%')
        arguments = dict(nonce='n', sequence=1, binding_sha256='a'*64, expected_reference=None)
        for value in (reply, {**reply, 'approved': True}):
            with self.assertRaises(snapshot.FormalSnapshotError):
                transport.validate_reply(owner.canonical(value), **arguments)

    def test_assembly_binding_is_explicit_and_not_embedded_in_source_context(self):
        reference = dict(path='/control/pinned.json', bytes=1, sha256='a'*64)
        original_binding = {name: ('release-1' if name == 'releaseTag' else 'a'*64) for name in owner.BINDING_FIELDS}
        config = dict(schemaVersion='backup-normal-owner-assembly/v1', binding=original_binding,
            programPin=reference, inputReferences={name: reference for name in assembly.INPUT_NAMES},
            controlRoot='/control/output', registryRoot='/control/registry', maintenanceMilliseconds=1000,
            **{name: reference for name in assembly.DOCUMENT_NAMES})
        self.assertIs(assembly.validate_configuration(config), config)
        missing = dict(config); del missing['binding']
        with self.assertRaises(snapshot.FormalSnapshotError): assembly.validate_configuration(missing)
        with self.assertRaises(snapshot.FormalSnapshotError):
            assembly.validate_configuration({**config, 'maintenanceMilliseconds': True})

    def test_failure_recovery_does_not_replay_pending_actions(self):
        self.assertEqual(recovery.action_state([], 'start-intent', 'started'), 'NEW')
        self.assertEqual(recovery.action_state(['start-intent'], 'start-intent', 'started'), 'PENDING')
        self.assertEqual(recovery.action_state(['start-intent', 'started'], 'start-intent', 'started'), 'COMPLETE')
        for names in (['started'], ['started', 'start-intent'], ['start-intent', 'start-intent']):
            with self.assertRaises(snapshot.FormalSnapshotError):
                recovery.action_state(names, 'start-intent', 'started')

    def material_fixture(self):
        inventory = {'changed': dict(kind='file', bytes=3, sha256=owner.sha(b'new')),
                     'same': dict(kind='file', bytes=3, sha256=owner.sha(b'old'))}
        end = position('mysql-bin.000001', 200)
        delta_raw = b'-- IntRuoyi binlog incremental;\n'
        point = dict(schemaVersion='backup-capture-point/v1', backupId='p2', generationId='g1', pointType='INCREMENTAL',
            sourceIdentity='source-A', catalogPin=dict(path='/source/catalog', bytes=1, sha256='a'*64),
            parentPointPin=dict(path='/repository/p1', bytes=1, sha256='b'*64),
            parentRepositoryPointPin=dict(path='/repository/point1', bytes=1, sha256='c'*64), databaseEnd=end,
            databaseDelta=dict(kind='BINLOG_SQL', segments=[], parentPosition=end,
                rawPin=dict(path='/capture/payload/database.sql.incremental.raw', bytes=len(delta_raw), sha256=owner.sha(delta_raw))),
            fileInventory=inventory, fileDelta=dict(changedPaths=['changed'], deletedPaths=[]),
            sourceRestored=False, disasterRecoveryVerified=False,
            commonRecoveryPoint='sha256:'+owner.sha(capture.canonical(dict(database=end, files=inventory))))
        bodies = {'payload/capture-content-point.json': capture.canonical(point),
            'payload/capture-file-inventory.json': capture.canonical(inventory), 'payload/database.sql.gz': b'compressed',
            'payload/database.sql.incremental.raw': delta_raw, 'payload/011-source-data.json': b'{}',
            'recovery-materials/index.json': b'{}', 'payload/original-application-images.tar': b'image',
            'original-configs/config-capture-stage.json': b'{}', 'payload/minio-physical/changed': b'new'}
        entries = {name: 'file|'+str(len(raw))+'|'+owner.sha(raw) for name, raw in bodies.items()}
        documents = {
            '/provisioned/identity.json': dict(sourceFingerprint='source-A', databaseUuid=UUID),
            '/provisioned/version.json': dict(version='unit-contract-version'),
            '/capture/semantic-capture/references.json': dict(schemaVersion='RecoveryReferenceInventory/v1',
                complete=True, recoveryPoint=point['commonRecoveryPoint'], databaseRows=[], objectReferences=[]),
            '/capture/semantic-capture/derived.json': dict(schemaVersion='RecoveryDerivedInventory/v1', complete=True,
                recoveryPoint=point['commonRecoveryPoint'], classes={name: dict(classification='NOT_PRESENT',
                    requiredForBusiness=False, payloadPins=[], executorKey=None) for name in ('redis', 'queue')}),
            '/capture/semantic-capture/versions.jsonl': b'',
            '/capture/semantic-capture/multipart-observations.json': dict(state='UNIT_METADATA_ONLY')}
        records = {path: value if type(value) is bytes else capture.canonical(value) for path, value in documents.items()}
        def ref(path): return dict(path=path, bytes=len(records[path]), sha256=owner.sha(records[path]))
        records['/provisioned/scope.json'] = owner.canonical(dict(sourceIdentityPin=ref('/provisioned/identity.json'),
            endpointHosts=['127.0.0.1']))
        objects = ref('/capture/semantic-capture/versions.jsonl')
        records['/capture/semantic-capture/export-receipt.json'] = capture.canonical(dict(
            state='S3_EXPORTED_PENDING_RESTORE_PROOF', buckets=['test'], inventoryBytes=0,
            inventorySha256=objects['sha256'], versionCount=0, deleteMarkerCount=0, totalVersionBytes=0,
            uniqueContentCount=0, multipartObservationsSha256=ref('/capture/semantic-capture/multipart-observations.json')['sha256']))
        supplemental = dict(sourceFingerprint='source-A', sourceScopePin=ref('/provisioned/scope.json'),
            versionPins=[ref('/provisioned/version.json')], referenceInventoryPin=ref('/capture/semantic-capture/references.json'),
            derivedInventoryPin=ref('/capture/semantic-capture/derived.json'), objectInventoryPin=objects,
            objectExportReceiptPin=ref('/capture/semantic-capture/export-receipt.json'))
        records['/capture/semantic-capture/supplement.json'] = capture.canonical(supplemental)
        records['/capture/semantic-capture/object-content-index.json'] = capture.canonical(dict(
            schemaVersion='backup-capture-object-index/v1', backupId='p2', generationId='g1', sourceIdentity='source-A',
            recoveryPoint=point['commonRecoveryPoint'], pointType='INCREMENTAL', parentExportIndexPin=None,
            inventoryPin=objects, exportReceiptPin=supplemental['objectExportReceiptPin'], contentPins={},
            changedContentSha256=[], repositoryBound=False, manifestPin=None))
        for path, raw in records.items():
            if path.startswith('/capture/'):
                entries[path.removeprefix('/capture/')] = 'file|'+str(len(raw))+'|'+owner.sha(raw)
        return point, entries, supplemental, records

    def test_recovery_inputs_contain_only_changed_file_payloads(self):
        point, entries, supplemental, _ = self.material_fixture()
        inputs = materials.capture_recovery_inputs(point, source_root='/capture', entries=entries, supplemental=supplemental)
        self.assertEqual(set(inputs['filePayloadPins']), {'changed'})
        self.assertEqual(inputs['database']['method'], 'BINLOG_SQL')
        self.assertEqual(inputs['database']['startPosition'], point['databaseDelta']['parentPosition'])
        self.assertEqual(inputs['recoveryPoint'], point['commonRecoveryPoint'])
        entries['payload/minio-physical/same'] = 'file|3|'+owner.sha(b'old')
        with self.assertRaises(snapshot.FormalSnapshotError):
            materials.capture_recovery_inputs(point, source_root='/capture', entries=entries, supplemental=supplemental)

    def test_recovery_inputs_reject_changed_point_or_missing_sql(self):
        point, entries, supplemental, _ = self.material_fixture()
        for changed_point, changed_entries in (({**point, 'commonRecoveryPoint': 'unbound'}, entries),
                (point, {name: value for name, value in entries.items() if name != 'payload/database.sql.gz'})):
            with self.assertRaises(snapshot.FormalSnapshotError):
                materials.capture_recovery_inputs(changed_point, source_root='/capture', entries=changed_entries, supplemental=supplemental)

    def test_repository_material_rebases_verified_files_and_requires_real_supplement(self):
        point, entries, supplemental, records = self.material_fixture()
        inputs = materials.capture_recovery_inputs(point, source_root='/capture', entries=entries, supplemental=supplemental)
        files = {name: {**ref, 'path': '/nas/'+name} for name, ref in inputs['archiveFilePins'].items()}
        inputs_raw = capture.canonical(inputs)
        files['capture-recovery-inputs.json'] = dict(path='/nas/capture-recovery-inputs.json', bytes=len(inputs_raw),
                                                  sha256=owner.sha(inputs_raw))
        identity_raw = records['/provisioned/identity.json']
        identity = dict(path='/provisioned/identity.json', bytes=len(identity_raw), sha256=owner.sha(identity_raw))
        manifest = dict(schemaVersion='RepositoryManifest/v1', backupId=inputs['backupId'], generationId='g1',
            pointType='INCREMENTAL', commonRecoveryPoint=inputs['recoveryPoint'], payloadRefs=list(files.values()),
            sourceIdentityPin=identity, versionPins=supplemental['versionPins'])
        raw = owner.canonical(manifest)
        manifest_pin = dict(path='/nas/manifest.json', bytes=len(raw), sha256=owner.sha(raw))
        arguments = dict(manifest_pin=manifest_pin, archived_file_pins=files, supplemental=supplemental,
                         original_records=records)
        result = materials.assemble_repository_recovery_material(inputs, manifest, **arguments)
        self.assertEqual(result['database']['sqlPin'], files['payload/database.sql.gz'])
        self.assertEqual(result['objects']['filePayloadPins'], {'changed': files['payload/minio-physical/changed']})
        self.assertEqual(result['objects']['inventoryPin'], files['semantic-capture/versions.jsonl'])
        self.assertEqual(result['referenceInventoryPin'], files['semantic-capture/references.json'])
        self.assertEqual(result['payloadPins'], manifest['payloadRefs'])
        with self.assertRaises(snapshot.FormalSnapshotError):
            materials.assemble_repository_recovery_material(inputs, manifest, **{**arguments, 'supplemental': {}})
        changed = copy.deepcopy(files); changed['payload/database.sql.gz']['sha256'] = 'f'*64
        with self.assertRaises(snapshot.FormalSnapshotError):
            materials.assemble_repository_recovery_material(inputs, manifest, **{**arguments, 'archived_file_pins': changed})


if __name__ == '__main__': unittest.main(verbosity=2)
