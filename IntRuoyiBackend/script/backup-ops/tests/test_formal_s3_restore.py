"""Real private export files; explicit isolated S3 target double."""
import io,json,sys
from pathlib import Path
import pytest
sys.path.insert(0,str(Path(__file__).resolve().parents[1]/'linux'))
import formal_s3_export as m
from test_formal_s3_export import empty_observation

class Source:
 def multipart_empty(self,bucket):return empty_observation(bucket)
 def versions(self,bucket,cursor):
  return [dict(kind='delete-marker',key='a',versionId='d',isLatest=True),
          dict(kind='version',key='a',versionId='2',isLatest=False,size=3,etag='"e"'),
          dict(kind='version',key='a',versionId='1',isLatest=False,size=3,etag='"e"')],None
 def content(self,bucket,key,version):
  return io.BytesIO(b'new' if version=='2' else b'old'),{'content-length':'3','etag':'"e"',
   'x-amz-version-id':version,'content-type':'text/plain','x-amz-meta-example':'kept'}


def approved_restore(client,source,evidence):
 """Fixture explicitly approves current export and an identity bucket map."""
 receipt=json.loads((source/'export-receipt.json').read_bytes())
 return m.restore_s3_versions(client,source,evidence,
  bucket_mapping={bucket:bucket for bucket in receipt['buckets']},
  source_inventory_sha256=m.hashlib.sha256((source/'versions.jsonl').read_bytes()).hexdigest(),
  source_export_receipt_sha256=m.hashlib.sha256((source/'export-receipt.json').read_bytes()).hexdigest())

class Target:
 def __init__(self,case):self.case=case;self.rows=[];self.objects={};self.writes=0
 def empty_versioned(self,bucket):return self.case!='nonempty'
 def put_version(self,bucket,key,path,headers,size,sha):
  self.writes+=1
  if self.case=='write-fail':raise RuntimeError('owned failure')
  version='target-'+str(self.writes)
  self.rows.append(dict(kind='version',key=key,versionId=version,isLatest=False))
  self.objects[version]=(path.read_bytes(),dict(headers));return version
 def delete_marker(self,bucket,key):
  self.writes+=1;version='target-'+str(self.writes)
  self.rows.append(dict(kind='delete-marker',key=key,versionId=version,isLatest=True));return version
 def content(self,bucket,key,version):
  raw,headers=self.objects[version]
  if self.case=='readback-drift':raw=b'bad'
  return io.BytesIO(raw),dict(headers,**{'x-amz-version-id':version,'content-length':str(len(raw))})
 def versions(self,bucket,cursor):return list(reversed(self.rows)),None

@pytest.mark.parametrize('case',['pass','nonempty','tamper','write-fail','readback-drift','protected'])
def test_restore_exact_versions_and_tombstones(tmp_path,case):
 source=tmp_path/'export';m.export_s3_versions(Source(),['yudao'],source,guard=fixture_owner)
 if case=='tamper':next((source/'objects').rglob('*.bin')).write_bytes(b'bad')
 if case=='protected':
  rows=[json.loads(x) for x in (source/'versions.jsonl').read_text().splitlines()]
  rows[1]['headers']['x-amz-object-lock-legal-hold']='ON'
  raw=b''.join(m._canonical_bytes(x) for x in rows);(source/'versions.jsonl').write_bytes(raw)
  receipt=json.loads((source/'export-receipt.json').read_text())
  receipt.update(inventorySha256=m.hashlib.sha256(raw).hexdigest(),inventoryBytes=len(raw))
  (source/'export-receipt.json').write_bytes(m._canonical_bytes(receipt))
 target=Target(case);evidence=tmp_path/'restore'
 if case=='pass':
  receipt=approved_restore(target,source,evidence)
  assert receipt['state']=='VERSIONS_RESTORED_PENDING_APPLICATION_PROOF'
  maps=[json.loads(x) for x in (evidence/'version-mapping.jsonl').read_text().splitlines()]
  assert [x['sourceVersionId'] for x in maps]==['1','2','d']
  assert [x['targetVersionId'] for x in maps]==['target-1','target-2','target-3']
  assert target.objects['target-1'][0]==b'old' and target.objects['target-2'][0]==b'new'
  assert target.objects['target-2'][1]['x-amz-meta-example']=='kept'
 else:
  with pytest.raises(m.FormalSnapshotError):approved_restore(target,source,evidence)
  assert not (evidence/'restore-receipt.json').exists() and (evidence/'failed.json').exists()
  if case in ('nonempty','tamper','protected'):assert target.writes==0
 writes=target.writes
 with pytest.raises((m.FormalSnapshotError,FileExistsError)):approved_restore(target,source,evidence)
 assert target.writes==writes

@pytest.mark.parametrize('case',['duplicate-receipt-key','duplicate-row-key','nan','extra-receipt','empty-bucket'])
def test_strict_manifest_before_any_write(tmp_path,case):
 source=tmp_path/'export';client=Source()
 if case=='empty-bucket':client.versions=lambda bucket,cursor:([],None)
 m.export_s3_versions(client,['yudao'],source,guard=fixture_owner)
 target=Target(case);evidence=tmp_path/'restore'
 if case=='empty-bucket':
  result=approved_restore(target,source,evidence)
  assert result['versionCount']==0 and target.writes==0;return
 receipt_path=source/'export-receipt.json';receipt=json.loads(receipt_path.read_text())
 if case=='duplicate-receipt-key':receipt_path.write_text(receipt_path.read_text().rstrip()[:-1]+',"versionCount":2}\n')
 elif case=='extra-receipt':receipt['unexpected']=True;receipt_path.write_bytes(m._canonical_bytes(receipt))
 else:
  lines=(source/'versions.jsonl').read_text().splitlines()
  lines[0]=lines[0][:-1]+(',"isLatest":true}' if case=='duplicate-row-key' else ',"size":NaN}')
  raw=('\n'.join(lines)+'\n').encode();(source/'versions.jsonl').write_bytes(raw)
  receipt.update(inventorySha256=m.hashlib.sha256(raw).hexdigest(),inventoryBytes=len(raw))
  receipt_path.write_bytes(m._canonical_bytes(receipt))
 with pytest.raises(m.FormalSnapshotError):approved_restore(target,source,evidence)
 assert target.writes==0 and not (evidence/'restore-receipt.json').exists()

def test_inventory_not_reread_per_object(tmp_path,monkeypatch):
 source=tmp_path/'export';m.export_s3_versions(Source(),['yudao'],source,guard=fixture_owner)
 approval={'bucket_mapping':{'yudao':'yudao'},
  'source_inventory_sha256':m.hashlib.sha256((source/'versions.jsonl').read_bytes()).hexdigest(),
  'source_export_receipt_sha256':m.hashlib.sha256((source/'export-receipt.json').read_bytes()).hexdigest()}
 inventory=source/'versions.jsonl';original=Path.open;reads=[]
 def tracked(path,*args,**kwargs):
  if path==inventory:reads.append(path)
  return original(path,*args,**kwargs)
 monkeypatch.setattr(Path,'open',tracked)
 m.restore_s3_versions(Target('pass'),source,tmp_path/'restore',**approval)
 assert len(reads)<=2

@pytest.mark.parametrize('case',['missing-proof','changed-proof','extra-proof','wrong-bucket','pending','duplicate-bucket','late-drift'])
def test_restore_pins_empty_upload_original_before_each_write(tmp_path,case):
 source=tmp_path/'export';m.export_s3_versions(Source(),['yudao'],source,guard=fixture_owner)
 proof_path=source/'multipart-observations.json'
 proof=json.loads(proof_path.read_bytes());receipt_path=source/'export-receipt.json'
 receipt=json.loads(receipt_path.read_bytes());target=Target('pass');evidence=tmp_path/'restore'
 if case=='missing-proof':proof_path.unlink()
 elif case=='changed-proof':proof_path.write_bytes(proof_path.read_bytes()+b' ')
 elif case!='late-drift':
  if case=='extra-proof':proof['unexpected']=True
  elif case=='wrong-bucket':proof['after'][0]['bucket']='other'
  elif case=='pending':proof['before'][0]['uploadCount']=1
  elif case=='duplicate-bucket':proof['after'].append(dict(proof['after'][0]))
  raw=m._canonical_bytes(proof);proof_path.write_bytes(raw)
  receipt['multipartObservationsSha256']=m.hashlib.sha256(raw).hexdigest()
  receipt_path.write_bytes(m._canonical_bytes(receipt))
 else:
  original=target.put_version
  def put(*args):
   result=original(*args);proof_path.write_bytes(proof_path.read_bytes()+b' ');return result
  target.put_version=put
 with pytest.raises(m.FormalSnapshotError):approved_restore(target,source,evidence)
 assert target.writes==(1 if case=='late-drift' else 0)
 assert not (evidence/'restore-receipt.json').exists()

@pytest.mark.parametrize('case',['pending','invalid','empty'])
def test_empty_versioned_target_queries_unfinished_uploads_before_versions(monkeypatch,case):
 client=m.IsolatedRestoreS3('http://127.0.0.1:9000','owned','owned-secret',approved_endpoint='http://127.0.0.1:9000')
 calls=[]
 monkeypatch.setattr(client,'_get',lambda *args,**kwargs:io.BytesIO(b'<VersioningConfiguration><Status>Enabled</Status></VersioningConfiguration>'))
 def observe(bucket):
  calls.append(('multipart',bucket))
  if case=='pending':raise m.FormalSnapshotError('FORMAL_S3_MULTIPART_UPLOAD_PENDING')
  value=empty_observation(bucket)
  if case=='invalid':value['uploadCount']=True
  return value
 monkeypatch.setattr(client,'multipart_empty',observe,raising=False)
 monkeypatch.setattr(client,'versions',lambda bucket,cursor:(calls.append(('versions',bucket)) or ([],None)))
 if case=='empty':
  assert client.empty_versioned('yudao') is True
  assert calls==[('multipart','yudao'),('versions','yudao')]
 else:
  code='FORMAL_S3_MULTIPART_UPLOAD_PENDING' if case=='pending' else 'FORMAL_S3_MULTIPART_LIST_INVALID'
  with pytest.raises(m.FormalSnapshotError,match='^'+code+'$'):client.empty_versioned('yudao')
  assert calls==[('multipart','yudao')]


def fixture_owner():
    """Explicit owner protocol fixture; no maintained server protection."""
    pass
