"""Capture sequencing uses explicit Docker/SQL doubles; file bytes are real."""
import copy,hashlib,importlib.util,json
from pathlib import Path
from unittest.mock import patch
import pytest

SOURCE=Path(__file__).resolve().parents[1]/'linux/formal_snapshot_linux.py'
ID='a'*64


def test_native_dynamic_error_normalizes_at_standard_snapshot_boundary(tmp_path,monkeypatch):
    import importlib.util
    m=load()
    class DynamicError(RuntimeError):code='WATCHDOG_CAPTURE_INVALIDATED'
    class Module:
        WatchdogError=DynamicError
        def capture_physical_with_native_fence(self,*args,**kwargs):raise DynamicError('private never expose')
    monkeypatch.setattr(importlib.util,'module_from_spec',lambda _:Module())
    class Loader:
        def exec_module(self,module):pass
    monkeypatch.setattr(importlib.util,'spec_from_file_location',lambda *args:type('Spec',(),{'loader':Loader()})())
    with pytest.raises(m.FormalSnapshotError,match='^WATCHDOG_CAPTURE_INVALIDATED$'):
        m._capture_native_physical(tmp_path,'/owned',{},'/owned/source','/owned/target',10,guard=lambda:None)


def test_raw_file_hash_rejects_expired_outer_budget_before_open(tmp_path,monkeypatch):
    m=load();path=tmp_path/'private';path.write_bytes(b'actual-retained-bytes')
    monkeypatch.setattr(m.time,'monotonic',lambda:10.)
    token=m.observation_deadline.set(9.)
    try:
        with pytest.raises(m.FormalSnapshotError,match='FORMAL_DATA_DEADLINE'):m._hash_object(path)
        assert path.read_bytes()==b'actual-retained-bytes'
    finally:m.observation_deadline.reset(token)


def test_native_wrapper_reserves_budget_at_actual_native_boundary(tmp_path,monkeypatch):
    m=load();called=[]
    monkeypatch.setattr(m.time,'monotonic',lambda:10.)
    token=m.observation_deadline.set(13.)
    try:
        with pytest.raises(m.FormalSnapshotError,match='FORMAL_DATA_DEADLINE'):
            m._capture_native_physical(tmp_path,'/owned',{},'/owned/source','/owned/target',20,guard=lambda:None)
        assert not tmp_path.joinpath('000-ready.json').exists()
    finally:m.observation_deadline.reset(token)

def test_native_runtime_prerequisite_refuses_before_sql_and_capture_mutation(tmp_path):
    m=load();source=tmp_path/'source';source.mkdir();target=tmp_path/'capture'
    class NativeRuntimeError(RuntimeError):
        code='WATCHDOG_RUNTIME_UNSUPPORTED'
    fence={'mount':str(tmp_path),'binding':{'operationId':'unit-binding'},'seconds':3}
    with patch.object(m,'_require_native_physical_runtime',side_effect=NativeRuntimeError(),create=True), \
         patch.object(m,'observe_database_barrier') as db, \
         patch.object(m,'collect_docker_barrier_records') as docker:
        with pytest.raises(m.FormalSnapshotError) as failure:
            m.capture_formal_payload(object(),'ruoyi',source,target,ID,filesystem_fence=fence,native_guard=lambda:None)
        assert failure.value.code=='WATCHDOG_RUNTIME_UNSUPPORTED'
        db.assert_not_called();docker.assert_not_called();assert not target.exists()

def test_explicit_native_fence_runs_only_in_physical_stage(tmp_path):
    m=load();source=tmp_path/'source';source.mkdir();(source/'xl.meta').write_bytes(b'ACTUAL_PRIVATE_FILE')
    target=tmp_path/'capture';calls=[]
    class Mysql:
        def query(self,sql):calls.append('db-observation');return '1\t1\t0\n'
        def dump(self,database,path,*,guard):
            assert callable(guard);guard()
            calls.append('sql-dump');path.write_bytes(b'ACTUAL_UNIT_SQL_BYTES')
            return {'sha256':hashlib.sha256(path.read_bytes()).hexdigest(),'bytes':path.stat().st_size}
    def native(control,mount,binding,source_root,destination,seconds,*,guard):
        assert callable(guard);guard()
        assert calls[-1]=='db-observation'
        calls.append('native-physical')
        assert control==target/'native-fence' and source_root==source
        actual=m.copy_physical_minio_tree(source_root,destination)
        return {'state':'RAW_CAPTURED_AND_THAWED_PENDING_FORMAL_BARRIERS','physical':actual,
                'captureSha256':'b'*64,'productionEvidence':False}
    fence={'mount':str(tmp_path),'binding':{'operationId':'unit-binding'},'seconds':3}
    with patch.object(m,'collect_docker_barrier_records',return_value=records(source)),\
         patch.object(m,'_require_native_physical_runtime',return_value=None,create=True),\
         patch.object(m,'observe_stopped_object_containers',return_value={'hostWritersVerified':False}),\
         patch.object(m,'_capture_native_physical',side_effect=native,create=True):
        result=m.capture_formal_payload(Mysql(),'ruoyi',source,target,ID,filesystem_fence=fence,native_guard=lambda:None)
    assert calls.index('sql-dump')<calls.index('native-physical')
    assert 'db-observation' in calls[calls.index('native-physical')+1:]
    assert result['nativeCapture']['captureSha256']=='b'*64 and result['hostWritersVerified'] is False

def test_native_failure_retains_specific_code_without_payload_receipt(tmp_path):
    m=load();source=tmp_path/'source';source.mkdir();(source/'xl.meta').write_bytes(b'ACTUAL_PRIVATE_FILE')
    target=tmp_path/'capture'
    class NativeError(RuntimeError):
        code='WATCHDOG_CAPTURE_INVALIDATED'
    class Mysql:
        def query(self,sql):return '1\t1\t0\n'
        def dump(self,database,path,*,guard):
            assert callable(guard);guard()
            path.write_bytes(b'ACTUAL_UNIT_SQL_BYTES')
            return {'sha256':hashlib.sha256(path.read_bytes()).hexdigest(),'bytes':path.stat().st_size}
    fence={'mount':str(tmp_path),'binding':{'operationId':'unit-binding'},'seconds':3}
    with patch.object(m,'collect_docker_barrier_records',return_value=records(source)),\
         patch.object(m,'_require_native_physical_runtime',return_value=None,create=True),\
         patch.object(m,'observe_stopped_object_containers',return_value={'hostWritersVerified':False}),\
         patch.object(m,'_capture_native_physical',side_effect=NativeError(),create=True):
        with pytest.raises(m.FormalSnapshotError) as failure:
            m.capture_formal_payload(Mysql(),'ruoyi',source,target,ID,filesystem_fence=fence,native_guard=lambda:None)
        assert failure.value.code=='WATCHDOG_CAPTURE_INVALIDATED'
    assert json.loads((target/'900-failed.json').read_bytes())['code']=='WATCHDOG_CAPTURE_INVALIDATED'
    assert not (target/'040-payload.json').exists()
def load():
    spec=importlib.util.spec_from_file_location('formal_capture',SOURCE)
    m=importlib.util.module_from_spec(spec);spec.loader.exec_module(m);return m
def records(root):
    return [{'id':ID,'name':'/ragflow_compose-minio-1','state':{'Running':False,'ExitCode':0,'OOMKilled':False},
        'mounts':[{'Source':'/owned/source','Destination':'/data','RW':True}],'privileged':False,'capAdd':None}]

@pytest.mark.parametrize('stage',['initial','post-sql','post-copy'])
def test_capture_guard_loss_retains_failure_and_never_completes(tmp_path,stage):
    m=load();source=tmp_path/'source';source.mkdir();(source/'xl.meta').write_bytes(b'ACTUAL_PRIVATE_FILE')
    target=tmp_path/'capture';calls=[];lost=False
    def guard():
        calls.append('guard')
        if lost:raise m.FormalSnapshotError('FORMAL_MAINTENANCE_OWNER_INVALID')
    class Mysql:
        def query(self,sql):return '1\t1\t0\n'
        def dump(self,database,path,*,guard):
            assert callable(guard);guard()
            nonlocal lost
            calls.append('dump');Path(path).write_bytes(b'ACTUAL_SQL_FILE')
            if stage=='post-sql':lost=True
            return {'sha256':hashlib.sha256(Path(path).read_bytes()).hexdigest(),'bytes':Path(path).stat().st_size}
    original=m.copy_physical_minio_tree
    def physical(*args,**kwargs):
        nonlocal lost
        calls.append('copy');result=original(*args,**kwargs)
        if stage=='post-copy':lost=True
        return result
    if stage=='initial':lost=True
    with patch.object(m,'collect_docker_barrier_records',return_value=records(source)),patch.object(m,'observe_stopped_object_containers',return_value={'hostWritersVerified':False}),patch.object(m,'copy_physical_minio_tree',side_effect=physical):
        with pytest.raises(m.FormalSnapshotError) as error:
            m.capture_formal_payload(Mysql(),'ruoyi',source,target,ID,barrier_guard=guard)
        assert error.value.code=='FORMAL_MAINTENANCE_OWNER_INVALID'
    assert not (target/'040-payload.json').exists()
    if stage=='initial':assert not target.exists() and 'dump' not in calls
    else:
        assert (target/'900-failed.json').exists()
        if stage=='post-sql':assert 'copy' not in calls

@pytest.mark.parametrize('case',['success','live','sql-fail','copy-fail','docker-drift','db-drift','dump-digest'])
def test_capture_refuses_drift_and_retains_journal(tmp_path,case):
    m=load();source=tmp_path/'source';source.mkdir();(source/'xl.meta').write_bytes(b'ACTUAL_PHYSICAL_FILE')
    destination=tmp_path/'capture';observed=records(source);calls=[]
    class Mysql:
        queries=0
        def query(self,sql):
            self.queries+=1;calls.append('query')
            return '0\t0\t0\n' if case=='db-drift' and self.queries>1 else '1\t1\t0\n'
        def dump(self,database,path,*,guard):
            assert callable(guard);guard()
            calls.append('dump');Path(path).write_bytes(b'ACTUAL_LOCAL_SQL_BYTES')
            if case=='sql-fail':raise m.FormalSnapshotError('FORMAL_MYSQL_DUMP_FAILED')
            digest=hashlib.sha256(Path(path).read_bytes()).hexdigest()
            return {'sha256':'b'*64 if case=='dump-digest' else digest,'bytes':Path(path).stat().st_size}
    if case=='live':observed[0]['state']['Running']=True
    observations=0
    def collect():
        nonlocal observations
        observations+=1;current=copy.deepcopy(observed)
        if case=='docker-drift' and observations>1:current[0]['state']['ExitCode']=137
        return current
    original=m.copy_physical_minio_tree
    original_observe=m.observe_stopped_object_containers
    def mount_namespace(rows,source_path,identity):
        # Explicit Docker namespace double: local file paths differ from Linux
        # mount paths on Windows. The production predicate remains unchanged.
        assert source_path==str(source.absolute())
        return original_observe(rows,'/owned/source',identity)
    def physical(*args,**kwargs):
        calls.append('copy')
        if case=='copy-fail':raise m.FormalSnapshotError('FORMAL_PHYSICAL_COPY_IO_FAILED')
        return original(*args,**kwargs)
    with patch.object(m,'collect_docker_barrier_records',side_effect=collect),patch.object(m,'copy_physical_minio_tree',side_effect=physical),patch.object(m,'observe_stopped_object_containers',side_effect=mount_namespace):
        if case=='success':
            result=m.capture_formal_payload(Mysql(),'ruoyi',source,destination,ID)
            assert result['state']=='CAPTURED_PENDING_FULL_BARRIER_PROOF'
            assert result['hostWritersVerified'] is False and calls.index('dump')<calls.index('copy')
            assert (destination/'040-payload.json').exists()
        else:
            with pytest.raises(m.FormalSnapshotError):m.capture_formal_payload(Mysql(),'ruoyi',source,destination,ID)
            assert (destination/'900-failed.json').exists() and not (destination/'040-payload.json').exists()
            failure=json.loads((destination/'900-failed.json').read_text())
            assert failure['code']=={'live':'FORMAL_MINIO_NOT_GRACEFULLY_STOPPED',
                'sql-fail':'FORMAL_MYSQL_DUMP_FAILED','copy-fail':'FORMAL_PHYSICAL_COPY_IO_FAILED',
                'docker-drift':'FORMAL_MINIO_NOT_GRACEFULLY_STOPPED','db-drift':'FORMAL_DATABASE_NOT_QUIESCENT',
                'dump-digest':'FORMAL_CAPTURE_SQL_DIGEST_MISMATCH'}[case]
            if case in ('live','sql-fail','docker-drift','db-drift','dump-digest'):assert 'copy' not in calls
    assert not (destination/'consistency.json').exists() and not (destination/'producer-completion.json').exists()
    before={p.name:p.read_bytes() for p in destination.iterdir() if p.is_file()}
    with pytest.raises(m.FormalSnapshotError):m.capture_formal_payload(Mysql(),'ruoyi',source,destination,ID)
    assert before=={p.name:p.read_bytes() for p in destination.iterdir() if p.is_file()}
