"""Actual file/content tests with explicit paginated S3 response doubles."""
import hashlib,importlib.util,io,json,sys,threading,urllib.error
from http.server import BaseHTTPRequestHandler,ThreadingHTTPServer
from urllib.parse import parse_qs,urlsplit
from pathlib import Path
import pytest
ROOT=Path(__file__).resolve().parents[1]/'linux'
def load():
 sys.path.insert(0,str(ROOT))
 try:
  spec=importlib.util.spec_from_file_location('s3_export',ROOT/'formal_s3_export.py')
  m=importlib.util.module_from_spec(spec);spec.loader.exec_module(m);return m
 finally:sys.path.pop(0)
class Client:
 def __init__(self,case):self.case=case;self.lists=0;self.reads=0;self.multipart_calls=[]
 def multipart_empty(self,bucket):
  self.multipart_calls.append(bucket)
  if self.case=='pending-upload' or (self.case=='late-upload' and len(self.multipart_calls)>1):
   raise load().FormalSnapshotError('FORMAL_S3_MULTIPART_UPLOAD_PENDING')
  return empty_observation(bucket)
 def versions(self,bucket,cursor):
  self.lists+=1
  if cursor:
   rows=[{'kind':'delete-marker','key':'deleted','versionId':'d','isLatest':True}]
   return rows,None
  rows=[{'kind':'version','key':'a','versionId':'1','isLatest':False,'size':3,'etag':'"etag"'},
        {'kind':'version','key':'a','versionId':'2','isLatest':True,'size':3,'etag':'"etag"'}]
  if self.case=='duplicate':rows.append(dict(rows[0]))
  if self.case=='listing-drift' and self.lists>2:rows[0]['etag']='"changed"'
  return rows,('a','2')
 def content(self,bucket,key,version):
  self.reads+=1
  if self.case=='read-fail':raise RuntimeError('ACTUAL_READ_FAILED')
  headers={'content-length':'3','etag':'"etag"','x-amz-version-id':version,'content-type':'text/plain'}
  if self.case=='wrong-version':headers['x-amz-version-id']='other'
  return io.BytesIO(b'xx' if self.case=='short' else b'abc'),headers

def empty_observation(bucket):
 return {'bucket':bucket,'state':'NO_MULTIPART_UPLOAD_OBSERVED','httpStatus':200,
         'rawSha256':hashlib.sha256(('empty:'+bucket).encode()).hexdigest(),'rawBytes':128,
         'maxUploads':1,'uploadCount':0,'isTruncated':False,'scope':'WHOLE_BUCKET_FROM_START'}

@pytest.mark.parametrize('case',['pending-upload','late-upload','missing-observer','invalid-observation','later-bucket'])
def test_export_refuses_unfinished_or_unproved_uploads(tmp_path,case):
 m=load();client=Client(case);target=tmp_path/'export';buckets=['yudao']
 expected='FORMAL_S3_MULTIPART_UPLOAD_PENDING'
 if case=='missing-observer':
  client.multipart_empty=None;expected='FORMAL_S3_MULTIPART_OBSERVATION_REQUIRED'
 elif case=='invalid-observation':
  client.multipart_empty=lambda bucket:dict(empty_observation(bucket),uploadCount=True)
  expected='FORMAL_S3_MULTIPART_LIST_INVALID'
 elif case=='later-bucket':
  buckets.append('edhr')
  def observe(bucket):
   if bucket=='edhr':raise m.FormalSnapshotError('FORMAL_S3_MULTIPART_UPLOAD_PENDING')
   return empty_observation(bucket)
  client.multipart_empty=observe
 with pytest.raises(m.FormalSnapshotError,match='^'+expected+'$'):
  m.export_s3_versions(client,buckets,target,guard=fixture_owner)
 assert not (target/'export-receipt.json').exists()
 assert json.loads((target/'failed.json').read_bytes())['code']==expected
 if case!='late-upload':assert client.reads==0
 else:assert client.reads==2

def test_export_binds_actual_empty_upload_observations(tmp_path):
 m=load();client=Client('pass');target=tmp_path/'export'
 r=m.export_s3_versions(client,['yudao'],target,guard=fixture_owner)
 proof=(target/'multipart-observations.json').read_bytes()
 assert r['multipartObservationsSha256']==hashlib.sha256(proof).hexdigest()
 assert json.loads(proof)=={'state':'MULTIPART_EMPTY_POINT_OBSERVATIONS',
  'before':[empty_observation('yudao')],'after':[empty_observation('yudao')]}
 assert client.multipart_calls==['yudao','yudao']

@pytest.mark.parametrize('case',['empty','pending','truncated','wrong-bucket','prefix','marker',
 'duplicate','bad-root','bad-namespace','entity','status','forbidden','large','next-marker'])
def test_signed_whole_bucket_multipart_empty_query(case):
 m=load();requests=[]
 ns='http://s3.amazonaws.com/doc/2006-03-01/'
 root='ListMultipartUploadsResult';extra='';bucket='yudao';truncated='false'
 if case=='pending':extra='<Upload><Key>PRIVATE_UPLOAD_KEY</Key><UploadId>PRIVATE_UPLOAD_ID</UploadId></Upload>'
 if case=='truncated':truncated='true'
 if case=='wrong-bucket':bucket='other'
 if case=='prefix':extra='<Prefix>filtered/</Prefix>'
 if case=='marker':extra='<KeyMarker>filtered</KeyMarker>'
 if case=='duplicate':extra='<IsTruncated>false</IsTruncated>'
 if case=='bad-root':root='Error'
 if case=='bad-namespace':ns='http://untrusted.invalid/'
 if case=='next-marker':extra='<NextKeyMarker>PRIVATE_MARKER</NextKeyMarker>'
 body=(('<!DOCTYPE a [<!ENTITY x "hidden">]>' if case=='entity' else '')+
       '<'+root+' xmlns="'+ns+'"><Bucket>'+bucket+'</Bucket><MaxUploads>1</MaxUploads>'+
       '<IsTruncated>'+truncated+'</IsTruncated>'+extra+'</'+root+'>').encode()
 if case=='large':body=b'X'*(1024*1024+1)
 class Handler(BaseHTTPRequestHandler):
  def log_message(self,*args):pass
  def do_GET(self):
   requests.append((self.path,dict(self.headers)))
   self.send_response(403 if case=='forbidden' else 202 if case=='status' else 200)
   self.send_header('Content-Length',str(len(body)));self.end_headers();self.wfile.write(body)
 server=ThreadingHTTPServer(('127.0.0.1',0),Handler)
 thread=threading.Thread(target=server.serve_forever,daemon=True);thread.start()
 try:
  client=m.ReadOnlyS3('http://127.0.0.1:'+str(server.server_port),'owned','owned-secret')
  if case=='empty':
   observed=client.multipart_empty('yudao')
   assert observed==dict(empty_observation('yudao'),rawSha256=hashlib.sha256(body).hexdigest(),rawBytes=len(body))
  else:
   code='FORMAL_S3_MULTIPART_UPLOAD_PENDING' if case=='pending' else 'FORMAL_S3_HTTP_403' if case=='forbidden' else 'FORMAL_S3_MULTIPART_LIST_INVALID'
   with pytest.raises(m.FormalSnapshotError,match='^'+code+'$') as raised:client.multipart_empty('yudao')
   assert 'PRIVATE' not in str(raised.value) and 'owned-secret' not in str(raised.value)
  assert len(requests)==1
  url=urlsplit(requests[0][0]);assert url.path=='/yudao'
  assert parse_qs(url.query,keep_blank_values=True)=={'uploads':[''],'max-uploads':['1']}
  assert 'AWS4-HMAC-SHA256' in requests[0][1]['Authorization']
 finally:
  server.shutdown();server.server_close();thread.join(timeout=2);assert not thread.is_alive()
@pytest.mark.parametrize('case',['pass','duplicate','read-fail','wrong-version','short','listing-drift'])
def test_full_version_export(tmp_path,case):
 m=load();client=Client(case);target=tmp_path/'export'
 if case=='pass':
  r=m.export_s3_versions(client,['yudao'],target,guard=fixture_owner)
  assert r['versionCount']==2 and r['deleteMarkerCount']==1 and r['uniqueContentCount']==1
  lines=[json.loads(x) for x in (target/'versions.jsonl').read_text().splitlines()]
  assert lines[0]['sha256']==lines[1]['sha256'] and len(lines)==3
  assert next((target/'objects/sha256').rglob('*.bin')).read_bytes()==b'abc'
  assert r['state']=='S3_EXPORTED_PENDING_RESTORE_PROOF'
 else:
  with pytest.raises(m.FormalSnapshotError):m.export_s3_versions(client,['yudao'],target,guard=fixture_owner)
  assert not (target/'export-receipt.json').exists()
  assert (target/'failed.json').exists()
 saved={p.relative_to(target).as_posix():p.read_bytes() for p in target.rglob('*') if p.is_file()}
 with pytest.raises(m.FormalSnapshotError):m.export_s3_versions(client,['yudao'],target,guard=fixture_owner)
 assert saved=={p.relative_to(target).as_posix():p.read_bytes() for p in target.rglob('*') if p.is_file()}

def test_http_failure_exposes_only_status(monkeypatch):
 m=load()
 def fail(*args,**kwargs):raise urllib.error.HTTPError('http://owned/private',403,'SECRET MESSAGE',{},None)
 monkeypatch.setattr(m,'_open_signed',fail)
 with pytest.raises(m.FormalSnapshotError,match='^FORMAL_S3_HTTP_403$'):
  m.ReadOnlyS3('http://127.0.0.1:9000','owned','owned-secret').versions('yudao',None)

def test_minio_url_encoded_key_spaces_and_literal_plus(monkeypatch):
 m=load();client=m.ReadOnlyS3('http://127.0.0.1:9000','owned','owned-secret')
 xml=b'<ListVersionsResult><Version><Key>folder%2Fname+%2B+%25.txt</Key><VersionId>v</VersionId><IsLatest>true</IsLatest><LastModified>now</LastModified><Size>1</Size><ETag>e</ETag></Version><IsTruncated>true</IsTruncated><NextKeyMarker>folder%2Fname+%2B+%25.txt</NextKeyMarker><NextVersionIdMarker>v</NextVersionIdMarker></ListVersionsResult>'
 monkeypatch.setattr(client,'_get',lambda *args,**kwargs:io.BytesIO(xml))
 rows,cursor=client.versions('yudao',None)
 assert rows[0]['key']=='folder/name + %.txt' and cursor==('folder/name + %.txt','v')


def fixture_owner():
    """Explicit owner protocol fixture; no maintained server protection."""
    pass
