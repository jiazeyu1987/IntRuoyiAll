"""Limited formal snapshot primitives. Local doubles cannot certify a snapshot."""
import importlib.util
from pathlib import Path
import subprocess
from unittest.mock import patch
import gzip
import pytest


from contextlib import contextmanager
import io

@contextmanager
def patch_dump_execute(module, execute):
    """Explicit client protocol double; private output files remain actual."""
    class Client:
        def __init__(self,command,**kwargs):
            self.command=command;self.options=kwargs;self.returncode=None;self.stdin=io.BytesIO()
        def communicate(self,input=None,timeout=None):
            result=execute(self.command,**dict(self.options,input=input,timeout=timeout))
            self.returncode=result.returncode;return (None,None)
        def poll(self):return self.returncode
        def kill(self):self.returncode=-9
        def wait(self,timeout=None):return self.returncode
    with patch.object(module.subprocess,'Popen',side_effect=Client) as constructor:
        yield constructor

SOURCE=Path(__file__).resolve().parents[1]/'linux/formal_snapshot_linux.py'

def load():
    spec=importlib.util.spec_from_file_location('formal_snapshot',SOURCE)
    module=importlib.util.module_from_spec(spec);spec.loader.exec_module(module);return module

def test_mysql_query_uses_stdin_secret_and_never_error_output():
    m=load();secret='owned-secret-sentinel'
    with patch.object(m.subprocess,'run',return_value=subprocess.CompletedProcess([],1,b'',secret.encode())) as run:
        with pytest.raises(m.FormalSnapshotError) as failure:
            m.PrivateDockerMySql('intruoyi-mysql',secret).query('SELECT 1;')
    args=run.call_args.args[0]
    assert secret not in repr(args)
    assert run.call_args.kwargs['input']==(secret+'\nSELECT 1;').encode()
    assert secret not in str(failure.value)
    assert failure.value.code=='FORMAL_MYSQL_QUERY_FAILED'


def test_native_query_explicit_utf8mb4_and_stdin_only_unicode():
    m=load();sql="SELECT '迁移甲😀';";secret='private-query-sentinel'
    with patch.object(m.subprocess,'run',return_value=subprocess.CompletedProcess([],0,'迁移甲😀\n'.encode(),b'')) as run:
        assert m.PrivateDockerMySql('owned-mysql',secret).query(sql)=='迁移甲😀\n'
    command=run.call_args.args[0]
    assert '--default-character-set=utf8mb4' in command
    assert secret not in repr(command) and sql not in repr(command)
    assert run.call_args.kwargs['input']==(secret+'\n'+sql).encode('utf-8')


def test_native_dump_explicit_utf8mb4(tmp_path):
    m=load()
    def run(command,**kwargs):
        assert '--default-character-set=utf8mb4' in command
        kwargs['stdout'].write("SELECT '迁移甲😀';\n".encode('utf-8'))
        return subprocess.CompletedProcess(command,0,None,b'')
    with patch_dump_execute(m,run):
        m.PrivateDockerMySql('owned-mysql','private-dump-sentinel').dump('owned_ledger',tmp_path/'dump.gz',guard=lambda:None)

@pytest.mark.parametrize('result',[0,2])
def test_dump_publishes_only_checked_bytes_and_refuses_existing(tmp_path,result):
    m=load();secret='owned-secret-sentinel'
    target=tmp_path/'snapshot.sql.gz'
    def execute(args,**kwargs):
        assert secret not in repr(args)
        assert kwargs['input']==(secret+'\n').encode()
        kwargs['stdout'].write(b'CREATE TABLE actual_dump_fixture (id INT);\n')
        return subprocess.CompletedProcess(args,result,None,b'private stderr')
    with patch_dump_execute(m,execute):
        mysql=m.PrivateDockerMySql('intruoyi-mysql',secret)
        if result:
            with pytest.raises(m.FormalSnapshotError) as failure:mysql.dump('ruoyi-vue-pro',target,guard=lambda:None)
            assert failure.value.code=='FORMAL_MYSQL_DUMP_FAILED'
            assert not target.exists()
            assert (tmp_path/'snapshot.sql.gz.raw').exists()  # retained failure evidence
        else:
            receipt=mysql.dump('ruoyi-vue-pro',target,guard=lambda:None)
            assert gzip.decompress(target.read_bytes()).startswith(b'CREATE TABLE')
            assert receipt['bytes']==target.stat().st_size and len(receipt['sha256'])==64
            with pytest.raises(m.FormalSnapshotError):mysql.dump('ruoyi-vue-pro',target,guard=lambda:None)

def test_newline_secret_refuses_before_child_process():
    m=load()
    with patch.object(m.subprocess,'run') as run:
        with pytest.raises(m.FormalSnapshotError):m.PrivateDockerMySql('intruoyi-mysql','secret\nother')
    run.assert_not_called()

def test_dump_publishes_pending_file_atomically(tmp_path):
    m=load(); target=tmp_path/'snapshot.sql.gz'; calls=[]
    def execute(args,**kwargs):
        kwargs['stdout'].write(b'ATOMIC_DUMP_BYTES\n')
        return subprocess.CompletedProcess(args,0,None,b'')
    original=m.os.link
    def link(source,destination):
        calls.append((Path(source).name,Path(destination).name))
        return original(source,destination)
    with patch_dump_execute(m,execute), patch.object(m.os,'link',side_effect=link):
        receipt=m.PrivateDockerMySql('intruoyi-mysql','secret').dump('ruoyi',target,guard=lambda:None)
    assert calls==[('snapshot.sql.gz.pending','snapshot.sql.gz')]
    assert target.exists() and receipt['bytes']==target.stat().st_size

def test_publication_race_preserves_existing_snapshot_and_failure_bytes(tmp_path):
    m=load();target=tmp_path/'snapshot.sql.gz';original=m.os.link
    def execute(args,**kwargs):
        kwargs['stdout'].write(b'OWNED_DUMP_BYTES\n')
        return subprocess.CompletedProcess(args,0,None,b'')
    def race(source,destination):
        Path(destination).write_bytes(b'EXISTING_OTHER_SNAPSHOT')
        return original(source,destination)
    with patch_dump_execute(m,execute),patch.object(m.os,'link',side_effect=race):
        with pytest.raises(m.FormalSnapshotError) as failure:
            m.PrivateDockerMySql('intruoyi-mysql','secret').dump('ruoyi',target,guard=lambda:None)
    assert failure.value.code=='FORMAL_MYSQL_DUMP_IO_FAILED'
    assert target.read_bytes()==b'EXISTING_OTHER_SNAPSHOT'
    assert (tmp_path/'snapshot.sql.gz.raw').read_bytes()==b'OWNED_DUMP_BYTES\n'
    assert gzip.decompress((tmp_path/'snapshot.sql.gz.pending').read_bytes())==b'OWNED_DUMP_BYTES\n'

@pytest.mark.parametrize('observed',['0\t1\t0\n','1\t0\t0\n','1\t1\t2\n','1\t1\t0\nextra',''])
def test_snapshot_database_observation_refuses_unfenced_or_active_transactions(observed):
    m=load()
    class Mysql:
        def query(self,sql):return observed
    with pytest.raises(m.FormalSnapshotError):m.observe_database_barrier(Mysql())

def test_database_fence_queries_real_flags_and_transactions():
    m=load();queries=[]
    class Mysql:
        def query(self,sql):queries.append(sql);return '1\t1\t0\n'
    assert m.observe_database_barrier(Mysql())=={'readOnly':True,'superReadOnly':True,'activeDatabaseTransactions':0}
    assert '@@GLOBAL.read_only' in queries[0] and '@@GLOBAL.super_read_only' in queries[0]
    assert 'information_schema.innodb_trx' in queries[0]


def test_full_snapshot_includes_scheduled_events_and_isolated_client_options(tmp_path):
    m=load(); target=tmp_path/'snapshot.sql.gz'
    def execute(args,**kwargs):
        assert '--events' in args
        assert '--routines' in args and '--triggers' in args
        assert '--no-defaults' in args and '--protocol=SOCKET' in args
        assert '--set-gtid-purged=OFF' in args
        script=args[6]
        assert 'MYSQL_TEST_LOGIN_FILE' in script and '.mylogin.cnf' in script
        assert 'owned-secret' not in repr(args)
        kwargs['stdout'].write(b'CREATE EVENT owned_event ON SCHEDULE EVERY 1 DAY DISABLE DO SELECT 1;\n')
        return subprocess.CompletedProcess(args,0,None,b'')
    with patch_dump_execute(m,execute):
        m.PrivateDockerMySql('owned-mysql','owned-secret').dump('owned_probe',target,guard=lambda:None)
    assert b'CREATE EVENT' in gzip.decompress(target.read_bytes())


def test_container_action_uses_existing_total_budget():
    m=load()
    with patch.object(m.time,'monotonic',return_value=10.):
        token=m.observation_deadline.set(12.)
        try:
            with patch.object(m.subprocess,'run',return_value=subprocess.CompletedProcess([],0,b'a'*64+b'\n',b'')) as run:
                m.run_formal_container_action('start','a'*64)
            assert 0<run.call_args.kwargs['timeout']<=2.
        finally:m.observation_deadline.reset(token)


@pytest.mark.parametrize('state',['short','expired','late'])
def test_barrier_inventory_keeps_total_budget_between_commands(state):
    import json
    m=load();clock=[13. if state=='expired' else 10.];calls=[]
    def execute(command,**kwargs):
        calls.append(command)
        assert 0<kwargs['timeout']<=2.
        if state=='late':clock[0]=13.
        stdout=(json.dumps({'id':'a'*64})+'\n').encode() if command[1]=='inspect' else b'a'*64+b'\n'
        return subprocess.CompletedProcess(command,0,stdout,b'')
    with patch.object(m.time,'monotonic',side_effect=lambda:clock[0]),patch.object(m.subprocess,'run',side_effect=execute):
        token=m.observation_deadline.set(12.)
        try:
            if state=='short':assert m.collect_docker_barrier_records()==[{'id':'a'*64}]
            else:
                with pytest.raises(m.FormalSnapshotError,match='FORMAL_DATA_DEADLINE'):m.collect_docker_barrier_records()
                assert len(calls)==(0 if state=='expired' else 1)
        finally:m.observation_deadline.reset(token)


@pytest.mark.parametrize('expired',['before','after'])
def test_container_action_does_not_dispatch_or_succeed_after_total_deadline(expired):
    m=load();clock=[13. if expired=='before' else 10.]
    def execute(*args,**kwargs):
        clock[0]=13.
        return subprocess.CompletedProcess([],0,b'a'*64+b'\n',b'')
    with patch.object(m.time,'monotonic',side_effect=lambda:clock[0]),patch.object(m.subprocess,'run',side_effect=execute) as run:
        token=m.observation_deadline.set(12.)
        try:
            with pytest.raises(m.FormalSnapshotError,match='FORMAL_DATA_DEADLINE'):
                m.run_formal_container_action('start','a'*64)
            assert run.call_count==(0 if expired=='before' else 1)
        finally:m.observation_deadline.reset(token)


def test_dump_process_preserves_outer_budget_and_remote_bound(tmp_path):
    m=load();target=tmp_path/'snapshot.sql.gz'
    def execute(args,**kwargs):
        assert 0<kwargs['timeout']<=2.
        assert 'timeout --signal=TERM' in args[6] and '2s mysqldump' in args[6]
        kwargs['stdout'].write(b'CREATE TABLE owned (id INT);\n')
        return subprocess.CompletedProcess(args,0,None,b'')
    with patch.object(m.time,'monotonic',return_value=10.),patch_dump_execute(m,execute):
        token=m.observation_deadline.set(12.)
        try:m.PrivateDockerMySql('owned-mysql','owned-private').dump('owned_probe',target,guard=lambda:None)
        finally:m.observation_deadline.reset(token)
    assert gzip.decompress(target.read_bytes()).startswith(b'CREATE TABLE')


@pytest.mark.parametrize('phase',['raw-fsync','pending-fsync','checksum'])
def test_dump_expired_stage_retains_bytes_without_publication(tmp_path,phase):
    m=load();target=tmp_path/'snapshot.sql.gz';clock=[10.];syncs=[0]
    def execute(args,**kwargs):
        kwargs['stdout'].write(b'CREATE TABLE owned (id INT);\n')
        return subprocess.CompletedProcess(args,0,None,b'')
    def sync(fd):
        syncs[0]+=1
        if phase==('raw-fsync' if syncs[0]==1 else 'pending-fsync'):clock[0]=13.
    original=m.hashlib.sha256
    class LateChecksum:
        def __init__(self):self.inner=original()
        def update(self,value):self.inner.update(value);clock[0]=13.
        def hexdigest(self):return self.inner.hexdigest()
    with patch.object(m.time,'monotonic',side_effect=lambda:clock[0]),patch_dump_execute(m,execute),patch.object(m.os,'fsync',side_effect=sync):
        token=m.observation_deadline.set(12.)
        try:
            with patch.object(m.hashlib,'sha256',side_effect=LateChecksum if phase=='checksum' else original):
                with pytest.raises(m.FormalSnapshotError,match='FORMAL_MYSQL_DUMP_TIMEOUT'):
                    m.PrivateDockerMySql('owned-mysql','owned-private').dump('owned_probe',target,guard=lambda:None)
        finally:m.observation_deadline.reset(token)
    assert not target.exists() and target.with_name(target.name+'.raw').exists()
    if phase!='raw-fsync':assert target.with_name(target.name+'.pending').exists()
