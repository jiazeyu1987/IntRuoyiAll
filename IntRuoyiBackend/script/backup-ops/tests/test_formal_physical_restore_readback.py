"""Actual private exported files; explicit S3 source protocol, no server proof."""
import copy
import hashlib
import importlib
import io
import json
from pathlib import Path
import sys

import pytest

sys.path.insert(0, str(Path(__file__).parents[1]/'linux'))
from formal_s3_export import export_s3_versions
from formal_snapshot_linux import FormalSnapshotError, _canonical_bytes
from test_formal_s3_export import empty_observation


class Source:
    def __init__(self, suffix=''):
        self.suffix=suffix
    def multipart_empty(self,bucket):
        # Response-specific XML hashes may differ while both valid observations are empty.
        return dict(empty_observation(bucket),rawSha256=hashlib.sha256((bucket+self.suffix).encode()).hexdigest())
    def versions(self,bucket,cursor):
        return [dict(kind='delete-marker',key='same/key',versionId='delete',isLatest=True,
                    lastModified='2026-10-01T00:00:03.000Z'),
                dict(kind='version',key='same/key',versionId='v2',isLatest=False,size=3,etag='"e"',
                    lastModified='2026-10-01T00:00:02.000Z'),
                dict(kind='version',key='same/key',versionId='v1',isLatest=False,size=3,etag='"e"',
                    lastModified='2026-10-01T00:00:01.000Z')],None
    def content(self,bucket,key,version):
        return io.BytesIO(b'new' if version=='v2' else b'old'),{
            'content-length':'3','etag':'"e"','x-amz-version-id':version,'content-type':'text/plain',
            'x-amz-meta-original':'kept','x-amz-object-lock-legal-hold':'ON',
            'x-amz-object-lock-mode':'GOVERNANCE','x-amz-object-lock-retain-until-date':'2026-12-01T00:00:00Z'}


def fixture_owner():
    """Explicit callback protocol fixture, not continuous maintained-host protection."""
    pass


def pair(tmp_path):
    roots=[tmp_path/'source',tmp_path/'restored']
    for root,suffix in zip(roots,('', 'readback')):
        export_s3_versions(Source(suffix),['yudao','edhr'],root,guard=fixture_owner)
    return roots


def pins(root):
    return dict(inventory=hashlib.sha256((root/'versions.jsonl').read_bytes()).hexdigest(),
                receipt=hashlib.sha256((root/'export-receipt.json').read_bytes()).hexdigest())


def compare(roots,*,guard=fixture_owner,approved=None):
    module=importlib.import_module('formal_physical_restore_readback')
    source,restored=approved or [pins(root) for root in roots]
    return module.compare_physical_restore_exports(*roots,
        source_inventory_sha256=source['inventory'],source_export_receipt_sha256=source['receipt'],
        restored_inventory_sha256=restored['inventory'],restored_export_receipt_sha256=restored['receipt'],
        guard=guard)


def edit_rows(root, mutate):
    rows=[json.loads(line) for line in (root/'versions.jsonl').read_bytes().splitlines()]
    mutate(rows)
    raw=b''.join(_canonical_bytes(row) for row in rows)
    (root/'versions.jsonl').write_bytes(raw)
    receipt=json.loads((root/'export-receipt.json').read_bytes())
    receipt.update(inventoryBytes=len(raw),inventorySha256=hashlib.sha256(raw).hexdigest())
    (root/'export-receipt.json').write_bytes(_canonical_bytes(receipt))


def test_physical_readback_preserves_exact_history_and_metadata(tmp_path):
    roots=pair(tmp_path)
    before=[pins(root) for root in roots]
    result=compare(roots)
    assert result['state']=='PHYSICAL_VERSION_READBACK_MATCH_PENDING_FULL_RESTORE_PROOF'
    assert result['versionCount']==4 and result['deleteMarkerCount']==2 and result['totalVersionBytes']==12
    assert result['buckets']==['edhr','yudao'] and result['exactVersionIdentityVerified'] is True
    assert result['physicalCaptureVerified'] is False and result['productionEvidence'] is False
    assert result['continuousHostExclusionVerified'] is False and result['applicationVerified'] is False
    assert [pins(root) for root in roots]==before


@pytest.mark.parametrize('case',['version-id','latest','time','etag','metadata','protection','order',
                                'missing','duplicate','kind','content-sha','path','extra','bool-size'])
def test_physical_readback_refuses_identity_or_metadata_drift(tmp_path,case):
    roots=pair(tmp_path)
    def mutate(rows):
        row=rows[1]
        if case=='version-id':row['versionId']='recreated-id'
        elif case=='latest':row['isLatest']=True
        elif case=='time':row['lastModified']='2026-10-02T00:00:02.000Z'
        elif case=='etag':row['etag']='"changed"'
        elif case=='metadata':row['headers']['x-amz-meta-original']='changed'
        elif case=='protection':row['headers']['x-amz-object-lock-legal-hold']='OFF'
        elif case=='order':rows[1],rows[2]=rows[2],rows[1]
        elif case=='missing':rows.pop()
        elif case=='duplicate':rows.append(copy.deepcopy(row))
        elif case=='kind':rows[0]['kind']='version'
        elif case=='content-sha':row['sha256']='a'*64
        elif case=='path':row['repositoryPath']='../escape.bin'
        elif case=='extra':row['approved']=True
        elif case=='bool-size':row['size']=True
    edit_rows(roots[1],mutate)
    with pytest.raises(FormalSnapshotError):compare(roots)


@pytest.mark.parametrize('case',['receipt-pin','inventory-pin','sidecar','content','count','bucket',
                                'multipart','missing-time','source-content','hardlink','symlink'])
def test_physical_readback_rechecks_actual_export_bytes(tmp_path,case):
    roots=pair(tmp_path);approved=[pins(root) for root in roots]
    root=roots[1]
    if case=='receipt-pin':(root/'export-receipt.json').write_bytes(b'{}')
    elif case=='inventory-pin':(root/'versions.jsonl').write_bytes(b'{}\n')
    elif case=='sidecar':(root/'multipart-observations.json').write_bytes(b'{}')
    elif case in ('content','source-content'):
        next((roots[0] if case=='source-content' else root).glob('objects/sha256/*/*.bin')).write_bytes(b'bad')
    elif case in ('count','bucket'):
        receipt=json.loads((root/'export-receipt.json').read_bytes())
        if case=='count':receipt['versionCount']=True
        else:receipt['buckets']=['other']
        (root/'export-receipt.json').write_bytes(_canonical_bytes(receipt));approved[1]=pins(root)
    elif case=='multipart':
        value=json.loads((root/'multipart-observations.json').read_bytes());value['after'][0]['uploadCount']=1
        raw=_canonical_bytes(value);(root/'multipart-observations.json').write_bytes(raw)
        receipt=json.loads((root/'export-receipt.json').read_bytes());receipt['multipartObservationsSha256']=hashlib.sha256(raw).hexdigest()
        (root/'export-receipt.json').write_bytes(_canonical_bytes(receipt));approved[1]=pins(root)
    elif case=='missing-time':
        edit_rows(root,lambda rows:rows[1].pop('lastModified'));approved[1]=pins(root)
    elif case=='hardlink':
        import os
        os.link(root/'versions.jsonl',tmp_path/'alias')
    elif case=='symlink':
        path=root/'versions.jsonl';raw=path.read_bytes();path.unlink()
        alias=tmp_path/'outside';alias.write_bytes(raw);alias.chmod(0o600);path.symlink_to(alias)
    with pytest.raises(FormalSnapshotError):compare(roots,approved=approved)


def test_physical_readback_requires_guard_before_read(tmp_path):
    roots=pair(tmp_path)
    with pytest.raises(FormalSnapshotError,match='FORMAL_PHYSICAL_READBACK_OWNER_REQUIRED'):
        compare(roots,guard=None)


def test_physical_readback_preserves_current_owner_refusal(tmp_path):
    roots=pair(tmp_path);calls=[]
    def revoked():
        calls.append(True)
        if len(calls)==5:raise FormalSnapshotError('FORMAL_TEST_OWNER_REVOKED')
    with pytest.raises(FormalSnapshotError,match='FORMAL_TEST_OWNER_REVOKED'):
        compare(roots,guard=revoked)
