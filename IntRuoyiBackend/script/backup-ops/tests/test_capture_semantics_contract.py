"""Pure source metadata/ownership checks, without a simulated S3/MySQL runtime."""
import copy
import hashlib
import json
from pathlib import Path
import sys
import unittest

sys.path.insert(0, str(Path(__file__).parents[1]/'linux'))
import formal_capture_semantics as semantics
import formal_capture_incremental as capture
import formal_capture_recovery_inputs as materials
import formal_normal_owner as owner
import formal_snapshot_linux as snapshot
import test_capture_series_contract as contract_tests


def reference(path, raw):
    return dict(path=path, bytes=len(raw), sha256=hashlib.sha256(raw).hexdigest())


class SemanticContractTests(unittest.TestCase):
    def recipe(self):
        scope = reference('/provisioned/scope.json', b'scope')
        return dict(schemaVersion='backup-capture-reference-recipe/v1', sourceIdentity='source-A',
            complete=True, coverageScopePin=scope, maxRows=10,
            referenceQueries=[dict(table='files', columns=['bucket', 'path', 'version'])],
            objectBindings=[dict(table='files', bucket={'column': 'bucket'}, keyColumn='path',
                versionColumn='version', nullable=False, keyEncoding='RAW_KEY', urlPrefix=None)]), scope

    def test_query_recipe_rejects_sql_injection_and_unapproved_scope(self):
        recipe, scope = self.recipe()
        semantics.validate_reference_recipe(recipe, 'source-A', scope)
        for field, value in (('referenceQueries', [dict(table='files`;DROP TABLE files', columns=['path'])]),
                             ('objectBindings', []), ('complete', False), ('sourceIdentity', 'other')):
            with self.assertRaises(snapshot.FormalSnapshotError):
                semantics.validate_reference_recipe({**recipe, field: value}, 'source-A', scope)

    def test_hex_rows_preserve_null_empty_and_unicode(self):
        query = dict(table='files', columns=['path', 'version'])
        actual = semantics.decode_rows('E4B8ADE69687\tNULL\n\t00\n', query, 2)
        self.assertEqual(actual, [dict(table='files', path='中文', version=None),
                                  dict(table='files', path='', version='\0')])
        for raw in ('FF\tNULL\n', '0\tNULL\n', '00\n'):
            with self.assertRaises(snapshot.FormalSnapshotError): semantics.decode_rows(raw, query, 2)
        with self.assertRaises(snapshot.FormalSnapshotError): semantics.decode_rows('00\tNULL\n'*3, query, 2)

    def test_deleted_latest_does_not_replace_explicit_version_reference(self):
        recipe, _ = self.recipe()
        version = dict(bucket='bucket', key='k', versionId='v1', kind='version', isLatest=False, size=4, sha256='a'*64)
        deleted = dict(bucket='bucket', key='k', versionId='v2', kind='delete-marker', isLatest=True)
        rows = [dict(table='files', bucket='bucket', path='k', version='v1')]
        refs = semantics.resolve_object_references(rows, recipe['objectBindings'], [version, deleted])
        self.assertEqual(refs, [dict(bucket='bucket', key='k', sourceVersionId='v1', bytes=4, sha256='a'*64)])
        with self.assertRaises(snapshot.FormalSnapshotError):
            semantics.resolve_object_references([{**rows[0], 'version': None}], recipe['objectBindings'], [version, deleted])

    def test_changed_parent_size_refuses_and_unchanged_content_is_not_copied(self):
        sha = hashlib.sha256(b'kept').hexdigest()
        parent = {sha: reference('/repository/p1/content.bin', b'kept')}
        self.assertEqual(semantics.content_ownership(dict(sha256=sha, bytes=4), parent), 'PARENT')
        self.assertEqual(semantics.content_ownership(dict(sha256='b'*64, bytes=2), parent), 'CAPTURE')
        with self.assertRaises(snapshot.FormalSnapshotError):
            semantics.content_ownership(dict(sha256=sha, bytes=5), parent)

    def test_url_reference_decodes_unicode_and_refuses_invalid_escaping(self):
        recipe, _ = self.recipe(); binding = recipe['objectBindings'][0]
        binding.update(keyEncoding='URL', urlPrefix='http://127.0.0.1:9000/bucket/')
        row = dict(table='files', bucket='bucket', path=binding['urlPrefix']+'%E4%B8%AD%E6%96%87%2Fa', version=None)
        version = dict(bucket='bucket', key='中文/a', versionId='v1', kind='version', isLatest=True, size=1, sha256='c'*64)
        self.assertEqual(semantics.resolve_object_references([row], [binding], [version])[0]['key'], '中文/a')
        for path in ('%FF', '%ZZ', 'key?query=1'):
            with self.assertRaises(snapshot.FormalSnapshotError):
                semantics.resolve_object_references([{**row, 'path': binding['urlPrefix']+path}], [binding], [version])

    def test_object_metadata_change_refuses_before_content_is_accepted(self):
        row = dict(size=3, etag='etag', versionId='v1')
        headers = {'content-length': '3', 'etag': 'etag', 'x-amz-version-id': 'v1', 'content-type': 'text/plain'}
        self.assertEqual(semantics.content_record(row, headers), {'content-type': 'text/plain'})
        for field, value in (('content-length', '4'), ('etag', 'changed'), ('x-amz-version-id', 'v2'),
                             ('x-amz-server-side-encryption', 'AES256')):
            with self.assertRaises(snapshot.FormalSnapshotError):
                semantics.content_record(row, {**headers, field: value})

    def fixture(self):
        point, entries, supplemental, records = contract_tests.CaptureSeriesContractTests().material_fixture()
        old = reference('/repository/p1/objects/kept.bin', b'kept')
        new_path = '/capture/semantic-capture/objects/sha256/'
        new = reference(new_path+hashlib.sha256(b'new').hexdigest()[:2]+'/'+hashlib.sha256(b'new').hexdigest()+'.bin', b'new')
        parent = dict(schemaVersion='backup-capture-object-index/v1', backupId='p1', generationId='g1',
            sourceIdentity='source-A', recoveryPoint='parent-point', pointType='FULL', parentExportIndexPin=None,
            inventoryPin=reference('/repository/p1/versions.jsonl', b'parent'),
            exportReceiptPin=reference('/repository/p1/receipt.json', b'receipt'), contentPins={old['sha256']: old},
            changedContentSha256=[old['sha256']], repositoryBound=True,
            manifestPin=reference('/repository/p1/manifest.json', b'parent-manifest'))
        parent_pin = reference('/repository/p1/object-content-index.json', owner.canonical(parent))
        objects = []
        for version, content in (('v1', old), ('v2', new)):
            objects.append(dict(bucket='bucket', key=version, versionId=version, kind='version', isLatest=True,
                lastModified='2026-10-10T00:00:00Z', size=content['bytes'], etag=version, sha256=content['sha256'],
                repositoryPath='objects/sha256/'+content['sha256'][:2]+'/'+content['sha256']+'.bin', headers={}))
        object_path = supplemental['objectInventoryPin']['path']
        records[object_path] = b''.join(capture.canonical(row) for row in objects)
        supplemental['objectInventoryPin'] = reference(object_path, records[object_path])
        receipt_path = supplemental['objectExportReceiptPin']['path']
        receipt = json.loads(records[receipt_path]); receipt.update(buckets=['bucket'], inventoryBytes=len(records[object_path]),
            inventorySha256=supplemental['objectInventoryPin']['sha256'], versionCount=2, totalVersionBytes=7, uniqueContentCount=2)
        records[receipt_path] = capture.canonical(receipt)
        supplemental['objectExportReceiptPin'] = reference(receipt_path, records[receipt_path])
        source = dict(schemaVersion='backup-capture-object-index/v1', backupId='p2', generationId='g1',
            sourceIdentity='source-A', recoveryPoint=point['commonRecoveryPoint'], pointType='INCREMENTAL',
            parentExportIndexPin=parent_pin, inventoryPin=supplemental['objectInventoryPin'],
            exportReceiptPin=supplemental['objectExportReceiptPin'],
            contentPins={old['sha256']: old, new['sha256']: new}, changedContentSha256=[new['sha256']],
            repositoryBound=False, manifestPin=None)
        records['/capture/semantic-capture/object-content-index.json'] = capture.canonical(source)
        records['/capture/semantic-capture/supplement.json'] = capture.canonical(supplemental)
        records[new['path']] = b'new'
        for path, raw in records.items():
            if path.startswith('/capture/'):
                entries[path.removeprefix('/capture/')] = 'file|'+str(len(raw))+'|'+owner.sha(raw)
        inputs = materials.capture_recovery_inputs(point, source_root='/capture', entries=entries, supplemental=supplemental)
        files = {name: {**value, 'path': '/nas/p2/'+name} for name, value in inputs['archiveFilePins'].items()}
        files['capture-recovery-inputs.json'] = reference('/nas/p2/capture-recovery-inputs.json', capture.canonical(inputs))
        identity_raw = records['/provisioned/identity.json']
        manifest = dict(schemaVersion='RepositoryManifest/v1', backupId='p2', generationId='g1', pointType='INCREMENTAL',
            commonRecoveryPoint=inputs['recoveryPoint'], sourceIdentityPin=reference('/provisioned/identity.json', identity_raw),
            versionPins=supplemental['versionPins'], payloadRefs=list(files.values()))
        args = dict(manifest_pin=reference('/nas/p2/manifest.json', owner.canonical(manifest)),
            archived_file_pins=files, original_records=records, parent_object_index=parent)
        return inputs, manifest, args, old, new

    def test_repository_handoff_retains_parent_bytes_and_rebases_only_new_content(self):
        inputs, manifest, args, old, new = self.fixture()
        output = materials.assemble_repository_capture_records(inputs, manifest, **args)
        index = output['objectContentIndex']
        self.assertEqual(index['contentPins'][old['sha256']], old)
        self.assertTrue(index['contentPins'][new['sha256']]['path'].startswith('/nas/p2/'))
        self.assertEqual(index['manifestPin'], args['manifest_pin'])
        self.assertTrue(index['repositoryBound'])
        self.assertEqual(output['recoveryMaterial']['objects']['inventoryPin'], args['archived_file_pins']['semantic-capture/versions.jsonl'])
        self.assertFalse(any(value == old for value in args['archived_file_pins'].values()))

    def test_repository_handoff_rejects_parent_substitution_and_unarchived_new_content(self):
        inputs, manifest, args, old, new = self.fixture()
        changed = copy.deepcopy(args['parent_object_index']); changed['contentPins'][old['sha256']]['path'] = '/different/file'
        with self.assertRaises(snapshot.FormalSnapshotError):
            materials.assemble_repository_capture_records(inputs, manifest, **{**args, 'parent_object_index': changed})
        changed_files = copy.deepcopy(args['archived_file_pins'])
        del changed_files[new['path'].removeprefix('/capture/')]
        with self.assertRaises(snapshot.FormalSnapshotError):
            materials.assemble_repository_capture_records(inputs, manifest, **{**args, 'archived_file_pins': changed_files})


if __name__ == '__main__': unittest.main(verbosity=2)
