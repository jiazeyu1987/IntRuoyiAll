import io,sys,urllib.error
from pathlib import Path
from unittest.mock import patch
import pytest
sys.path.insert(0,str(Path(__file__).resolve().parents[1]/'linux'))
from formal_snapshot_linux import FormalSnapshotError

class Process:
    def __init__(self,raw,exit_code=0):
        self.stdin=io.BytesIO();self.stdout=io.BytesIO(raw);self.exit_code=exit_code;self.killed=False
    def wait(self,timeout=None):return self.exit_code
    def poll(self):return self.exit_code
    def kill(self):self.killed=True

def load():
    import formal_s3_exec_transport as module
    return module

def test_streamed_response_keeps_credentials_off_argv_and_bounds_curl():
    m=load();p=Process(b'HTTP/1.1 200 OK\r\nContent-Length: 4\r\nx-amz-version-id: v1\r\n\r\nDATA')
    # Preserve private stdin for test observation when adapter closes it.
    class Input(io.BytesIO):
        def close(self):self.written=self.getvalue()
    p.stdin=Input()
    with patch.object(m.subprocess,'Popen',return_value=p) as call:
        client=m.ContainerReadOnlyS3('1'*64,'private-access','private-secret',timeout=5,guard=fixture_owner)
        response,headers=client.content('yudao','folder/附件 + %.txt','v1')
        assert response.read(2)==b'DA' and response.read(2)==b'TA' and response.read(2)==b''
        response.close();assert headers['x-amz-version-id']=='v1'
        argv=call.call_args.args[0]
        assert argv==['docker','exec','-i','1'*64,'curl','--disable','--config','-']
        assert 'private' not in ' '.join(argv)
        assert b'max-time = 5' in p.stdin.written
        assert b'Authorization:' in p.stdin.written and b'private-access' in p.stdin.written
        assert b'location' not in p.stdin.written

@pytest.mark.parametrize('raw,exit_code',[
    (b'HTTP/1.1 302 Found\r\nLocation: http://external\r\n\r\n',0),
    (b'HTTP/1.1 200 OK\r\nContent-Length: 4\r\n\r\nDA',0),
    (b'HTTP/1.1 200 OK\r\nContent-Length: 4\r\n\r\nDATA',28),
    (b'HTTP/1.1 200 OK\r\nContent-Length: 4\r\nContent-Length: 5\r\n\r\nDATA',0)])
def test_redirect_truncation_transfer_or_duplicate_headers_never_success(raw,exit_code):
    m=load();p=Process(raw,exit_code)
    with patch.object(m.subprocess,'Popen',return_value=p):
        with pytest.raises((FormalSnapshotError,urllib.error.HTTPError)):
            response=m.ContainerReadOnlyS3('1'*64,'access','secret',timeout=5,guard=fixture_owner)._get('yudao')
            with response:
                while response.read(2):pass

def test_exact_absence_code_uses_existing_config_reader():
    m=load();raw=b'<Error><Code>NoSuchLifecycleConfiguration</Code></Error>'
    p=Process(b'HTTP/1.1 404 Not Found\r\nContent-Length: '+str(len(raw)).encode()+b'\r\n\r\n'+raw)
    with patch.object(m.subprocess,'Popen',return_value=p):
        result=m.ContainerReadOnlyS3('1'*64,'access','secret',timeout=5,guard=fixture_owner).absent_configuration('yudao','lifecycle','NoSuchLifecycleConfiguration')
        assert result['httpStatus']==404 and result['rawBytes']==len(raw)

def test_short_configuration_read_still_requires_successful_process_exit():
    m=load();raw=b'<VersioningConfiguration><Status>Enabled</Status></VersioningConfiguration>'
    p=Process(b'HTTP/1.1 200 OK\r\nContent-Length: '+str(len(raw)).encode()+b'\r\n\r\n'+raw,28)
    with patch.object(m.subprocess,'Popen',return_value=p):
        with pytest.raises(FormalSnapshotError):
            m.ContainerReadOnlyS3('1'*64,'access','secret',timeout=5,guard=fixture_owner).replica_configuration('yudao','versioning')

def test_repeated_vary_list_fields_are_combined_without_relaxing_lengths():
    m=load();p=Process(b'HTTP/1.1 200 OK\r\nContent-Length: 4\r\nVary: Origin\r\nVary: Accept-Encoding\r\n\r\nDATA')
    with patch.object(m.subprocess,'Popen',return_value=p):
        with m.ContainerReadOnlyS3('1'*64,'access','secret',timeout=5,guard=fixture_owner)._get('yudao') as response:
            assert response.headers['vary']=='Origin, Accept-Encoding'
            assert response.read(4)==b'DATA'


def fixture_owner():
    """Explicit owner protocol fixture; no maintained server protection."""
    pass
