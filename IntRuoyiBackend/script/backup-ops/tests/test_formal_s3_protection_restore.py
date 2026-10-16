import io,sys,json
from pathlib import Path
import pytest
sys.path.insert(0,str(Path(__file__).resolve().parents[1]/'linux'))
import formal_s3_export as m
from test_formal_s3_restore import Source,Target,approved_restore

class ProtectedSource(Source):
    def content(self,*args):
        raw,headers=super().content(*args)
        headers.update({'x-amz-object-lock-mode':'COMPLIANCE','x-amz-object-lock-retain-until-date':'2030-01-01T00:00:00Z',
                        'x-amz-object-lock-legal-hold':'ON'})
        return raw,headers

class ProtectedTarget(Target):
    def replica_configuration(self,bucket,configuration):
        return {'bucket':bucket,'configuration':configuration,'httpStatus':200,'enabled':self.case!='unprotected-target'}
    def content(self,*args):
        raw,headers=super().content(*args)
        if self.case=='protection-drift':headers['x-amz-object-lock-legal-hold']='OFF'
        return raw,headers

@pytest.mark.parametrize('case',['pass','unprotected-target','protection-drift','missing-date','invalid-mode','sse'])
def test_exact_protection_restoration_requires_protected_target_before_any_write(tmp_path,case):
    source=tmp_path/'export';m.export_s3_versions(ProtectedSource(),['yudao'],source,guard=fixture_owner)
    if case in ('missing-date','invalid-mode','sse'):
        rows=[json.loads(x) for x in (source/'versions.jsonl').read_text().splitlines()]
        headers=rows[1]['headers']
        if case=='missing-date':headers.pop('x-amz-object-lock-retain-until-date')
        if case=='invalid-mode':headers['x-amz-object-lock-mode']='UNKNOWN'
        if case=='sse':headers['x-amz-server-side-encryption']='aws:kms'
        raw=b''.join(m._canonical_bytes(x) for x in rows);(source/'versions.jsonl').write_bytes(raw)
        receipt=json.loads((source/'export-receipt.json').read_text());receipt.update(inventoryBytes=len(raw),inventorySha256=m.hashlib.sha256(raw).hexdigest())
        (source/'export-receipt.json').write_bytes(m._canonical_bytes(receipt))
    target=ProtectedTarget(case)
    if case=='pass':
        result=approved_restore(target,source,tmp_path/'evidence')
        assert result['state']=='VERSIONS_RESTORED_PENDING_APPLICATION_PROOF'
        assert target.objects['target-1'][1]['x-amz-object-lock-legal-hold']=='ON'
    else:
        with pytest.raises(m.FormalSnapshotError):approved_restore(target,source,tmp_path/'evidence')
        assert not (tmp_path/'evidence/restore-receipt.json').exists()
        if case!='protection-drift':assert target.writes==0


def fixture_owner():
    """Explicit owner protocol fixture; no maintained server protection."""
    pass
