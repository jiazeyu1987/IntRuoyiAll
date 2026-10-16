"""Explicit Docker/restorer doubles; no real deployment evidence."""
import hashlib,importlib.util,json,sys
from pathlib import Path
from unittest.mock import patch
import pytest
from test_formal_s3_restore import Source
from formal_s3_export import export_s3_versions
ROOT=Path(__file__).parents[1]/'linux'
sys.path.insert(0,str(ROOT))

@pytest.mark.parametrize('case',['pass','endpoint','label','image','request-drift','receipt-missing','receipt-changed',
    'mapping-missing','mapping-invalid','source-inventory-drift','source-receipt-drift','receipt-mapping-drift'])
def test_bound_restore_refuses_target_changes_before_writes(tmp_path,capsys,case):
    spec=importlib.util.spec_from_file_location('restore_entry',ROOT/'formal_restore_producer.py')
    m=importlib.util.module_from_spec(spec);spec.loader.exec_module(m)
    secret=tmp_path/'credentials.json';secret.write_bytes(b'{"access":"private-access","secret":"private-secret"}');secret.chmod(0o600)
    source=tmp_path/'export'
    export_s3_versions(Source(),['yudao'],source,guard=fixture_owner)
    mapping={'yudao':'yudao-backup-next'}
    req={'schemaVersion':'release-formal-object-restore-request/v2','operationId':'owned-restore',
         'sourceExport':str(tmp_path/'export'),'evidenceRoot':str(tmp_path/'restore'),
         'bucketMapping':mapping,
         'sourceInventorySha256':hashlib.sha256((source/'versions.jsonl').read_bytes()).hexdigest(),
         'sourceExportReceiptSha256':hashlib.sha256((source/'export-receipt.json').read_bytes()).hexdigest(),
         'target':{'containerId':'a'*64,'imageId':'sha256:'+'b'*64,'endpoint':'http://172.18.0.2:9000',
                   'credentialFile':str(secret)}}
    if case=='mapping-missing':del req['bucketMapping']
    if case=='mapping-invalid':req['bucketMapping']={'other':'yudao-backup-next'}
    p=tmp_path/'request.json';p.write_bytes(json.dumps(req).encode());p.chmod(0o600);digest=hashlib.sha256(p.read_bytes()).hexdigest()
    row={'Id':'a'*64,'Image':'sha256:'+'b'*64,'State':{'Running':True},
         'Config':{'Labels':{'intruoyi.formal-restore-operation':'owned-restore'}},
         'HostConfig':{'Privileged':False},'NetworkSettings':{'Networks':{'isolated':{'IPAddress':'172.18.0.2'}}}}
    if case=='endpoint':row['NetworkSettings']['Networks']['isolated']['IPAddress']='172.18.0.3'
    if case=='label':row['Config']['Labels']['intruoyi.formal-restore-operation']='other-operation'
    if case=='image':row['Image']='sha256:'+'c'*64
    if case=='request-drift':p.write_bytes(b'{}')
    if case=='source-inventory-drift':(source/'versions.jsonl').write_bytes(b'{}\n')
    if case=='source-receipt-drift':(source/'export-receipt.json').write_bytes(b'{}\n')
    def recorded_restore(*args,**kwargs):
        evidence=tmp_path/'restore';evidence.mkdir(mode=0o700)
        receipt={'state':'VERSIONS_RESTORED_PENDING_APPLICATION_PROOF',
            'bucketMapping':mapping,'bucketMappingSha256':hashlib.sha256(m._canonical_bytes(mapping)).hexdigest(),
            'sourceInventorySha256':req['sourceInventorySha256'],
            'sourceExportReceiptSha256':req['sourceExportReceiptSha256']}
        if case=='receipt-mapping-drift':receipt['bucketMapping']={'yudao':'unapproved-target'}
        if case!='receipt-missing':
            (evidence/'restore-receipt.json').write_bytes(json.dumps({} if case=='receipt-changed' else receipt).encode())
            (evidence/'restore-receipt.json').chmod(0o600)
        return receipt
    with patch.object(m,'require_native_runtime'),patch.object(m,'_private_docker_json',return_value=row), \
         patch.object(m,'IsolatedRestoreS3') as client,patch.object(m,'restore_s3_versions',side_effect=recorded_restore) as restore:
        code=m.main(['--request',str(p),'--expected-request-sha256',digest])
        out=capsys.readouterr().out
        assert 'private-secret' not in out and 'private-access' not in out
        assert code==(0 if case=='pass' else 1)
        if case in ('receipt-missing','receipt-changed','receipt-mapping-drift'):
            restore.assert_called_once();assert not (tmp_path/'restore/entry-binding.json').exists()
        elif case!='pass':restore.assert_not_called();client.assert_not_called()
        else:
            assert json.loads(out)['state']=='VERSIONS_RESTORED_PENDING_APPLICATION_PROOF'
            proof=json.loads((tmp_path/'restore/entry-binding.json').read_bytes())
            assert proof['operationId']=='owned-restore'
            assert proof['requestSha256']==digest
            assert proof['containerId']=='a'*64 and proof['imageId']=='sha256:'+'b'*64
            assert proof['restoreReceiptSha256']==hashlib.sha256((tmp_path/'restore/restore-receipt.json').read_bytes()).hexdigest()
            assert proof['productionEvidence'] is False
            assert proof['bucketMapping']==mapping
            assert proof['bucketMappingSha256']==hashlib.sha256(m._canonical_bytes(mapping)).hexdigest()
            assert proof['sourceInventorySha256']==req['sourceInventorySha256']
            assert proof['sourceExportReceiptSha256']==req['sourceExportReceiptSha256']
            assert restore.call_args.kwargs['bucket_mapping']==mapping


def fixture_owner():
    """Explicit owner protocol fixture; no maintained server protection."""
    pass
