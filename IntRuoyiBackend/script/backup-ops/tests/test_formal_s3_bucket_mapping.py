"""Real immutable export files and a bucket-aware S3 protocol double.

These tests prove mapping semantics, not real application/storage acceptance.
"""
import copy
import hashlib
import io
import json
import sys
from pathlib import Path
import pytest

sys.path.insert(0, str(Path(__file__).resolve().parents[1] / 'linux'))
import formal_s3_export as m
from test_formal_s3_export import empty_observation


class Source:
    def multipart_empty(self,bucket):return empty_observation(bucket)
    def versions(self, bucket, cursor):
        if bucket == 'source-empty':
            return [], None
        return [dict(kind='delete-marker', key='same/key', versionId='deleted', isLatest=True),
                dict(kind='version', key='same/key', versionId='new', isLatest=False, size=3, etag='"e"'),
                dict(kind='version', key='same/key', versionId='old', isLatest=False, size=3, etag='"e"')], None

    def content(self, bucket, key, version):
        raw = (b'one' if bucket == 'source-one' else b'two') if version == 'new' else b'old'
        headers = {'content-length': '3', 'etag': '"e"', 'x-amz-version-id': version,
                   'content-type': 'text/plain', 'x-amz-meta-source': bucket}
        if bucket == 'source-one':
            headers.update({'x-amz-object-lock-mode': 'COMPLIANCE',
                            'x-amz-object-lock-retain-until-date': '2030-01-01T00:00:00Z',
                            'x-amz-object-lock-legal-hold': 'ON'})
        return io.BytesIO(raw), headers


class BucketTarget:
    def __init__(self, mapping, *, empty=True):
        self.allowed = set(mapping.values())
        self.calls = []
        self.objects = {}
        self.rows = {bucket: [] for bucket in self.allowed}
        self.empty = empty
        self.writes = 0

    def checked(self, operation, bucket):
        assert bucket in self.allowed, 'source or unapproved bucket used'
        self.calls.append((operation, bucket))

    def empty_versioned(self, bucket):
        self.checked('empty', bucket)
        return self.empty

    def replica_configuration(self, bucket, configuration):
        self.checked(configuration, bucket)
        return {'enabled': True}

    def put_version(self, bucket, key, path, headers, size, sha):
        self.checked('put', bucket)
        self.writes += 1
        version = 'target-' + str(self.writes)
        self.objects[(bucket, key, version)] = (path.read_bytes(), dict(headers))
        for row in self.rows[bucket]:
            if row['key'] == key:
                row['isLatest'] = False
        self.rows[bucket].append(dict(kind='version', key=key, versionId=version, isLatest=True))
        return version

    def delete_marker(self, bucket, key):
        self.checked('delete', bucket)
        self.writes += 1
        version = 'target-' + str(self.writes)
        for row in self.rows[bucket]:
            if row['key'] == key:
                row['isLatest'] = False
        self.rows[bucket].append(dict(kind='delete-marker', key=key, versionId=version, isLatest=True))
        return version

    def content(self, bucket, key, version):
        self.checked('content', bucket)
        raw, headers = self.objects[(bucket, key, version)]
        return io.BytesIO(raw), dict(headers, **{'x-amz-version-id': version, 'content-length': str(len(raw))})

    def versions(self, bucket, cursor):
        self.checked('versions', bucket)
        return list(reversed(self.rows[bucket])), None


MAPPING = {'source-one': 'backup-one', 'source-two': 'backup-two', 'source-empty': 'backup-empty'}


def exported(tmp_path):
    source = tmp_path / 'export'
    m.export_s3_versions(Source(), list(MAPPING), source,guard=fixture_owner)
    return source


def approved(source):
    return {'source_inventory_sha256': hashlib.sha256((source/'versions.jsonl').read_bytes()).hexdigest(),
            'source_export_receipt_sha256': hashlib.sha256((source/'export-receipt.json').read_bytes()).hexdigest()}


def test_exact_bucket_mapping_preserves_versions_protection_and_source(tmp_path):
    source = exported(tmp_path)
    before = {path.relative_to(source): path.read_bytes() for path in source.rglob('*') if path.is_file()}
    target = BucketTarget(MAPPING)
    receipt = m.restore_s3_versions(target, source, tmp_path / 'restore', bucket_mapping=MAPPING, **approved(source))
    assert target.writes == 6
    assert receipt['bucketMapping'] == MAPPING
    assert receipt['bucketMappingSha256'] == hashlib.sha256(m._canonical_bytes(MAPPING)).hexdigest()
    assert receipt['sourceExportReceiptSha256'] == hashlib.sha256(before[Path('export-receipt.json')]).hexdigest()
    maps = [json.loads(line) for line in (tmp_path / 'restore/version-mapping.jsonl').read_bytes().splitlines()]
    assert [(row['sourceBucket'], row['targetBucket']) for row in maps] == [('source-one', 'backup-one')] * 3 + [('source-two', 'backup-two')] * 3
    assert [row['sourceVersionId'] for row in maps] == ['old', 'new', 'deleted'] * 2
    assert ('empty', 'backup-empty') in target.calls and ('versions', 'backup-empty') in target.calls
    assert ('object-lock', 'backup-one') in target.calls
    assert target.objects[('backup-one', 'same/key', 'target-2')][0] == b'one'
    assert target.objects[('backup-two', 'same/key', 'target-5')][0] == b'two'
    assert target.objects[('backup-one', 'same/key', 'target-2')][1]['x-amz-object-lock-legal-hold'] == 'ON'
    assert before == {path.relative_to(source): path.read_bytes() for path in source.rglob('*') if path.is_file()}


@pytest.mark.parametrize('mapping', [None, {}, {'source-one': 'backup-one'},
    dict(MAPPING, unexpected='backup-extra'), dict(MAPPING, **{'source-two': 'backup-one'}),
    dict(MAPPING, **{'source-one': 'INVALID'}), dict(MAPPING, **{'source-one': 123})])
def test_invalid_mapping_refuses_before_s3_calls(tmp_path, mapping):
    source = exported(tmp_path)
    target = BucketTarget(MAPPING)
    with pytest.raises(m.FormalSnapshotError, match='FORMAL_S3_BUCKET_MAPPING_INVALID'):
        m.restore_s3_versions(target, source, tmp_path / 'restore', bucket_mapping=mapping, **approved(source))
    assert target.calls == [] and target.writes == 0
    assert (tmp_path / 'restore/failed.json').is_file()
    assert not (tmp_path / 'restore/restore-receipt.json').exists()


def test_mapping_snapshot_cannot_be_mutated_by_client(tmp_path):
    source = exported(tmp_path)
    mapping = copy.deepcopy(MAPPING)
    target = BucketTarget(mapping)
    original = target.empty_versioned
    def changed(bucket):
        mapping['source-one'] = 'unapproved-target'
        return original(bucket)
    target.empty_versioned = changed
    receipt = m.restore_s3_versions(target, source, tmp_path / 'restore', bucket_mapping=mapping, **approved(source))
    assert receipt['bucketMapping'] == MAPPING
    assert all(bucket in MAPPING.values() for _, bucket in target.calls)


def test_approved_export_refuses_self_consistent_replacement_before_s3_calls(tmp_path):
    source=exported(tmp_path)
    approval=approved(source)
    receipt=json.loads((source/'export-receipt.json').read_bytes())
    receipt['buckets']=['source-one']
    (source/'export-receipt.json').write_bytes(m._canonical_bytes(receipt))
    target=BucketTarget(MAPPING)
    with pytest.raises(m.FormalSnapshotError,match='FORMAL_S3_APPROVED_EXPORT_CHANGED'):
        m.restore_s3_versions(target,source,tmp_path/'restore',bucket_mapping=MAPPING,**approval)
    assert target.calls==[] and target.writes==0


def fixture_owner():
    """Explicit owner protocol fixture; no maintained server protection."""
    pass
