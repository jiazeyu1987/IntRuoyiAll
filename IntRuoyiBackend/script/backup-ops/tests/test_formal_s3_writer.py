"""Real HTTP transport and real files, independently inspecting streamed requests."""
import hashlib,json,sys,threading
from pathlib import Path
from http.server import BaseHTTPRequestHandler,ThreadingHTTPServer
import pytest
sys.path.insert(0,str(Path(__file__).resolve().parents[1]/'linux'))
import formal_s3_export as m

@pytest.mark.parametrize('case',['pass','digest','redirect','wrong-status','binding'])
def test_streaming_signed_write(tmp_path,case):
 requests=[]
 class Handler(BaseHTTPRequestHandler):
  def log_message(self,*args):pass
  def do_PUT(self):
   size=int(self.headers['Content-Length']);raw=self.rfile.read(size)
   requests.append((self.path,dict(self.headers),hashlib.sha256(raw).hexdigest(),len(raw)))
   self.send_response(307 if case=='redirect' else 500 if case=='wrong-status' else 200)
   self.send_header('x-amz-version-id','actual-owned-version');self.send_header('Content-Length','0');self.end_headers()
  def do_DELETE(self):
   requests.append((self.path,dict(self.headers),'',0));self.send_response(204)
   self.send_header('x-amz-version-id','actual-owned-delete');self.send_header('x-amz-delete-marker','true')
   self.send_header('Content-Length','0');self.end_headers()
 server=ThreadingHTTPServer(('127.0.0.1',0),Handler)
 thread=threading.Thread(target=server.serve_forever,daemon=True);thread.start()
 try:
  endpoint='http://127.0.0.1:'+str(server.server_port)
  path=tmp_path/'payload.bin';raw=b'owned-stream\n'*200000;path.write_bytes(raw);sha=hashlib.sha256(raw).hexdigest()
  if case=='binding':
   with pytest.raises(m.FormalSnapshotError):m.IsolatedRestoreS3(endpoint,'owned','secret',approved_endpoint=endpoint+'/other')
   assert requests==[];return
  client=m.IsolatedRestoreS3(endpoint,'owned','secret',approved_endpoint=endpoint)
  if case=='pass':
   assert client.put_version('yudao','folder/附件 + %.bin',path,{'content-type':'application/octet-stream'},len(raw),sha)=='actual-owned-version'
   assert requests[0][0]=='/yudao/folder/%E9%99%84%E4%BB%B6%20%2B%20%25.bin'
   assert requests[0][2:]==(sha,len(raw))
   headers={k.lower():v for k,v in requests[0][1].items()}
   assert headers['x-amz-content-sha256']==sha and 'content-type;host;x-amz-content-sha256;x-amz-date' in headers['authorization']
   assert client.delete_marker('yudao','a')=='actual-owned-delete'
  else:
   with pytest.raises(m.FormalSnapshotError):client.put_version('yudao','a',path,{},len(raw),'0'*64 if case=='digest' else sha)
   assert len(requests)==(0 if case=='digest' else 1)
 finally:server.shutdown();server.server_close();thread.join(timeout=2)

def test_signed_get_refuses_redirect_before_second_request():
 requests=[]
 class Handler(BaseHTTPRequestHandler):
  def log_message(self,*args):pass
  def do_GET(self):
   requests.append(self.path)
   if self.path.startswith('/yudao'):
    self.send_response(307);self.send_header('Location','/redirected');self.send_header('Content-Length','0');self.end_headers()
   else:self.send_response(200);self.send_header('Content-Length','0');self.end_headers()
 server=ThreadingHTTPServer(('127.0.0.1',0),Handler);thread=threading.Thread(target=server.serve_forever,daemon=True);thread.start()
 try:
  client=m.ReadOnlyS3('http://127.0.0.1:'+str(server.server_port),'owned','owned-secret')
  with pytest.raises(m.FormalSnapshotError,match='FORMAL_S3_HTTP_307'):client.content('yudao','a','v')
  assert requests==['/yudao/a?versionId=v']
 finally:server.shutdown();server.server_close();thread.join(timeout=2)
