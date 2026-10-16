"""Actual local bytes/pipes/processes; owner and S3/Docker protocols are explicit fixtures."""
import io,json,os,subprocess,sys,threading,time,urllib.request
from pathlib import Path
from unittest.mock import patch
import pytest
sys.path.insert(0,str(Path(__file__).resolve().parents[1]/'linux'))
import formal_snapshot_linux as snapshot
import formal_replica_producer as producer
import formal_replica_gate as gate
import formal_s3_export as s3
import formal_s3_exec_transport as transport
from test_formal_replica_runtime import IMAGE
from test_formal_s3_export import Client

def fixture_owner():
    """Protocol fixture only; never server fence evidence."""
    pass

@pytest.mark.parametrize('guard',[None,False,'approved'])
def test_producer_rejects_missing_owner_before_effects(tmp_path,guard):
    target=tmp_path/'stage'
    with patch.object(producer,'prepare_checked_minio_replica') as prepare:
        with pytest.raises(snapshot.FormalSnapshotError,match='^FORMAL_REPLICA_OWNER_GUARD_REQUIRED$'):
            producer.produce_isolated_replica_export('operation-1',tmp_path/'raw',{}, {},tmp_path/'missing-private',
                'fixture-access','fixture-secret',target,image_id=IMAGE,guard=guard)
    assert not target.exists() and prepare.call_count==0

def test_standalone_cli_refuses_before_private_read_and_dispatch(tmp_path,capsys):
    args=[]
    for name,value in {'operation-id':'operation-1','capture-root':str(tmp_path/'raw'),
        'physical-receipt':str(tmp_path/'missing-physical'),'expected-physical-sha256':'1'*64,
        'observations':str(tmp_path/'missing-observed'),'expected-observations-sha256':'2'*64,
        'credential-file':str(tmp_path/'missing-private'),'evidence-root':str(tmp_path/'stage'),'image-id':IMAGE}.items():
        args.extend(['--'+name,value])
    with patch.object(producer,'_existing_file') as private,patch.object(producer,'produce_isolated_replica_export') as dispatch:
        assert producer.main(args)==1
    result=json.loads(capsys.readouterr().out)
    assert result['code']=='FORMAL_REPLICA_CURRENT_OWNER_REQUIRED' and result['productionEvidence'] is False
    assert private.call_count==dispatch.call_count==0 and not (tmp_path/'stage').exists()

def test_prepare_propagates_current_guard_during_actual_copy(tmp_path):
    raw=tmp_path/'raw';raw.mkdir();(raw/'large').write_bytes(b'X'*(3*1024*1024))
    receipt=snapshot.copy_physical_minio_tree(raw,tmp_path/'capture')
    calls=0;replica=tmp_path/'replica'
    def revoked():
        nonlocal calls
        calls+=1
        leaf=replica/'large'
        if leaf.exists() and leaf.stat().st_size>=1024*1024:
            raise snapshot.FormalSnapshotError('FIXTURE_OWNER_REVOKED')
    with pytest.raises(snapshot.FormalSnapshotError,match='^FIXTURE_OWNER_REVOKED$'):
        snapshot.prepare_minio_replica_copy(tmp_path/'capture',replica,receipt,lease_guard=revoked)
    assert calls>8 and (replica/'large').stat().st_size==1024*1024

@pytest.mark.parametrize('at',['page','body'])
def test_export_checks_after_io_before_persisting_success_or_body(tmp_path,at):
    revoked=False
    class FixtureClient(Client):
        def versions(self,*args):
            nonlocal revoked
            result=super().versions(*args)
            if at=='page':revoked=True
            return result
        def content(self,*args):
            class Body(io.BytesIO):
                def read(self,n):
                    nonlocal revoked
                    result=super().read(n);revoked=True;return result
            response,headers=super().content(*args);response.close()
            return Body(b'abc'),headers
    def guard():
        if revoked:raise snapshot.FormalSnapshotError('FIXTURE_OWNER_REVOKED')
    target=tmp_path/'export'
    with pytest.raises(snapshot.FormalSnapshotError,match='^FIXTURE_OWNER_REVOKED$'):
        s3.export_s3_versions(FixtureClient('pass'),['yudao'],target,guard=guard)
    assert not (target/'export-receipt.json').exists()
    assert json.loads((target/'failed.json').read_bytes())['code']=='FIXTURE_OWNER_REVOKED'
    if at=='body':assert (target/'current-object.pending').read_bytes()==b''

@pytest.mark.parametrize('at',['stdin','headers','body','exit'])
def test_transport_rechecks_on_owner_thread_while_real_owned_child_blocks(tmp_path,at):
    actual_popen=subprocess.Popen;children=[];owner=threading.get_ident();calls=[];started=time.monotonic()
    def guard():
        calls.append(threading.get_ident())
        assert threading.get_ident()==owner
        if time.monotonic()-started>0.25:raise snapshot.FormalSnapshotError('FIXTURE_OWNER_REVOKED')
    def launch(args,**kwargs):
        assert args[:4]==['docker','exec','-i','1'*64]
        body=''
        if at in ('body','exit'):
            body="sys.stdout.buffer.write(b'HTTP/1.1 200 OK\\r\\nContent-Length: 4\\r\\n\\r\\n"+("D" if at=='body' else "DATA")+"');sys.stdout.buffer.flush();"
        child=actual_popen([sys.executable,'-B','-c','import sys,time;'+body+'time.sleep(60)'],**kwargs)
        if at=='stdin':
            import fcntl
            fcntl.fcntl(child.stdin.fileno(),fcntl.F_SETPIPE_SZ,4096)
        children.append(child);return child
    with patch.object(transport.subprocess,'Popen',launch):
        with pytest.raises(snapshot.FormalSnapshotError,match='^FIXTURE_OWNER_REVOKED$'):
            client=transport.ContainerReadOnlyS3('1'*64,'fixture-access','fixture-secret',timeout=30,guard=guard)
            request=urllib.request.Request('http://127.0.0.1:9000/yudao',headers={'X-Test':'X'*10000} if at=='stdin' else {},method='GET')
            with client._open(request) as response:response.read(4)
    assert time.monotonic()-started<4 and children and all(child.poll() is not None for child in children)
    assert all(child.stdin.closed and child.stdout.closed for child in children)
    assert len(calls)>=3 and set(calls)=={owner}

def test_revocation_during_real_create_wait_retains_intent_without_next_effect(tmp_path):
    from test_formal_replica_runtime import inputs
    observed,physical,proof,_=inputs(tmp_path)
    env=tmp_path/'private.env';env.write_text('MINIO_ROOT_USER=fixture-access\nMINIO_ROOT_PASSWORD=fixture-secret\n');env.chmod(0o600)
    actual_popen=subprocess.Popen;children=[];launched=None;owner=threading.get_ident()
    def guard():
        assert threading.get_ident()==owner
        if launched is not None and time.monotonic()-launched>0.25:
            raise snapshot.FormalSnapshotError('FIXTURE_OWNER_REVOKED')
    def launch(args,**kwargs):
        nonlocal launched
        assert args[:2]==['docker','create']
        launched=time.monotonic()
        child=actual_popen([sys.executable,'-B','-c','import time;time.sleep(60)'],**kwargs)
        children.append(child);return child
    evidence=tmp_path/'creator'
    # Private image observation is a protocol fixture; create wait uses a real child.
    with patch.object(gate,'_private_docker_json',return_value={'Id':IMAGE,'RepoTags':[gate.PINNED_MINIO_TAG]}), \
         patch.object(gate.subprocess,'Popen',launch),patch.object(gate,'start_checked_minio_replica') as start:
        with pytest.raises(snapshot.FormalSnapshotError,match='^FIXTURE_OWNER_REVOKED$'):
            gate.create_and_start_checked_replica('operation-1',tmp_path/'replica',physical,observed,proof,env,evidence,image_id=IMAGE,guard=guard)
    assert children and all(child.poll() is not None for child in children)
    assert (evidence/'intent.json').exists() and not (evidence/'created.json').exists() and start.call_count==0

@pytest.mark.parametrize('at',['deadline','timer'])
def test_client_created_before_response_constructor_failure_is_reclaimed(tmp_path,at):
    actual_popen=subprocess.Popen;children=[];token=None
    def launch(args,**kwargs):
        nonlocal token
        child=actual_popen([sys.executable,'-B','-c','import time;time.sleep(60)'],**kwargs)
        children.append(child)
        if at=='deadline':token=snapshot.observation_deadline.set(time.monotonic()-1)
        return child
    def timer_failed(*args,**kwargs):raise RuntimeError('FIXTURE_TIMER_START_FAILED')
    try:
        with patch.object(transport.subprocess,'Popen',launch):
            client=transport.ContainerReadOnlyS3('1'*64,'fixture-access','fixture-secret',guard=fixture_owner)
            request=urllib.request.Request('http://127.0.0.1:9000/yudao',method='GET')
            if at=='timer':
                with patch.object(transport.threading.Timer,'start',timer_failed),pytest.raises(RuntimeError,match='^FIXTURE_TIMER_START_FAILED$'):
                    client._open(request)
            else:
                with pytest.raises(snapshot.FormalSnapshotError,match='^FORMAL_DATA_DEADLINE$'):client._open(request)
        assert children and all(child.poll() is not None and child.stdin.closed and child.stdout.closed for child in children)
    finally:
        if token is not None:snapshot.observation_deadline.reset(token)
        # Failed product cleanup remains a test FAIL, never leave a test child.
        for child in children:
            if child.poll() is None:child.kill();child.wait(timeout=3)
            for stream in (child.stdin,child.stdout):
                if not stream.closed:
                    try:stream.close()
                    except OSError:pass
