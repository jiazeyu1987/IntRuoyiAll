"""Real owned subprocess/file boundaries with explicit Docker protocol fixture.

The subprocess is local Python, not a MySQL server or exclusion proof.
"""
import gzip
import hashlib
import importlib.util
import os
from pathlib import Path
import subprocess
import sys
import threading
import time

import pytest

SOURCE = Path(__file__).resolve().parents[1]/'linux/formal_snapshot_linux.py'

def load():
    spec = importlib.util.spec_from_file_location('owned_dump_snapshot', SOURCE)
    module = importlib.util.module_from_spec(spec); spec.loader.exec_module(module)
    return module

def transport(monkeypatch, module, root, *, blocked=False, status=0, data=b'CREATE TABLE owned (id INT);\n'):
    payload = root/'fixture-payload'; payload.write_bytes(data)
    marker = root/'client-started'; secret = 'private-dump-stdin-sentinel'
    code = ('import pathlib,sys,time; '
            'assert sys.stdin.buffer.readline()==b"private-dump-stdin-sentinel\\n"; '
            'pathlib.Path(sys.argv[1]).write_text("started"); '
            + ('time.sleep(30); ' if blocked else '')
            + 'sys.stdout.buffer.write(pathlib.Path(sys.argv[2]).read_bytes()); '
            + f'sys.stdout.buffer.flush(); sys.exit({status})')
    children=[]; real_popen=subprocess.Popen
    def launch(command, **kwargs):
        assert command[:3] == ['docker','exec','-i']
        assert secret not in repr(command)
        assert '--single-transaction' in command and '--events' in command
        child=real_popen([sys.executable,'-B','-c',code,str(marker),str(payload)], **kwargs)
        children.append(child); return child
    # Old run() and corrected Popen() both cross this explicit client boundary.
    monkeypatch.setattr(module.subprocess, 'Popen', launch)
    return secret, marker, children

@pytest.mark.parametrize('guard', [None, False, 'not-a-callback'])
def test_dump_requires_guard_before_file_or_child(monkeypatch, tmp_path, guard):
    m=load(); calls=[]
    monkeypatch.setattr(m.subprocess,'Popen',lambda *a,**k:calls.append(a))
    target=tmp_path/'sql.gz'; mysql=m.PrivateDockerMySql('owned-container','private')
    with pytest.raises(m.FormalSnapshotError,match='FORMAL_MYSQL_DUMP_GUARD_REQUIRED'):
        if guard is None:mysql.dump('owned_db',target)
        else:mysql.dump('owned_db',target,guard=guard)
    assert not calls and not target.exists() and not target.with_name('sql.gz.raw').exists()


def test_admitted_dump_outlives_short_query_timeout_with_real_local_child(monkeypatch,tmp_path):
    m=load();real_popen=subprocess.Popen;children=[]
    # This executes a real local subprocess for deadline/byte behavior only.
    # It is not an actual mysqldump or native authority fixture.
    def launch(command,**kwargs):
        assert command[:3]==['docker','exec','-i']
        child=real_popen([sys.executable,'-B','-c',
            'import sys,time; sys.stdin.buffer.readline(); time.sleep(1.2); '
            'sys.stdout.buffer.write(b"scoped SQL\\n"); sys.stdout.buffer.flush()'],**kwargs)
        children.append(child);return child
    monkeypatch.setattr(m.subprocess,'Popen',launch)
    mysql=m.PrivateDockerMySql('owned-container','private',timeout=1)
    target=tmp_path/'sql.gz';deadline=time.monotonic()+4
    result=mysql.dump_until('owned_db',target,guard=lambda:None,deadline=deadline)
    assert gzip.decompress(target.read_bytes())==b'scoped SQL\n'
    assert result['sha256']==hashlib.sha256(target.read_bytes()).hexdigest()
    assert mysql.timeout==1 and len(children)==1 and children[0].returncode==0
    assert m.observation_deadline.get() is None


def test_explicit_dump_cannot_extend_original_outer_deadline(monkeypatch,tmp_path):
    m=load();secret,_,children=transport(monkeypatch,m,tmp_path,blocked=True)
    original=time.monotonic()+0.25;token=m.observation_deadline.set(original)
    try:
        with pytest.raises(m.FormalSnapshotError,match='DUMP_TIMEOUT|DATA_DEADLINE|OBSERVATION_DEADLINE_EXCEEDED'):
            m.PrivateDockerMySql('owned-container',secret,timeout=5).dump_until(
                'owned_db',tmp_path/'sql.gz',guard=lambda:None,deadline=time.monotonic()+30)
        assert m.observation_deadline.get()==original
        assert len(children)==1 and children[0].poll() is not None
    finally:m.observation_deadline.reset(token)

def test_revoked_before_export_creates_nothing(monkeypatch,tmp_path):
    m=load();calls=[];target=tmp_path/'sql.gz'
    monkeypatch.setattr(m.subprocess,'Popen',lambda *a,**k:calls.append(a))
    def guard():raise m.FormalSnapshotError('FORMAL_MAINTENANCE_OWNER_INVALID')
    with pytest.raises(m.FormalSnapshotError,match='FORMAL_MAINTENANCE_OWNER_INVALID'):
        m.PrivateDockerMySql('owned-container','private').dump('owned_db',target,guard=guard)
    assert not calls and not list(tmp_path.iterdir())

def test_revoked_while_client_waits_reaps_only_owned_client(monkeypatch,tmp_path):
    m=load(); secret,marker,children=transport(monkeypatch,m,tmp_path,blocked=True)
    target=tmp_path/'sql.gz'; caller=threading.get_ident(); seen=[]
    def guard():
        seen.append(threading.get_ident());assert seen[-1]==caller
        if marker.exists():raise m.FormalSnapshotError('FORMAL_MAINTENANCE_OWNER_INVALID')
    started=time.monotonic()
    with pytest.raises(m.FormalSnapshotError,match='FORMAL_MAINTENANCE_OWNER_INVALID') as failure:
        m.PrivateDockerMySql('owned-container',secret,timeout=5).dump('owned_db',target,guard=guard)
    assert time.monotonic()-started<2 and len(children)==1 and children[0].poll() is not None
    assert target.with_name('sql.gz.raw').exists() and not target.exists()
    assert secret not in str(failure.value) and not getattr(failure.value,'remoteStopped',False)

@pytest.mark.parametrize('phase',['compression','checksum','publication'])
def test_revoked_during_private_processing_retains_output(monkeypatch,tmp_path,phase):
    m=load();secret,_,children=transport(monkeypatch,m,tmp_path,data=b'SQL\n'+os.urandom(3*1024*1024))
    target=tmp_path/'sql.gz'; changed=[False]
    original_hash=m.hashlib.sha256
    class Checksum:
        def __init__(self):self.actual=original_hash()
        def update(self,value):self.actual.update(value);changed[0]=True
        def hexdigest(self):return self.actual.hexdigest()
    original_link=m.os.link
    def linked(source,destination):
        original_link(source,destination);changed[0]=True
    if phase=='checksum':monkeypatch.setattr(m.hashlib,'sha256',Checksum)
    if phase=='publication':monkeypatch.setattr(m.os,'link',linked)
    def guard():
        pending=target.with_name('sql.gz.pending')
        if changed[0] or (phase=='compression' and pending.exists() and pending.stat().st_size>65536):
            raise m.FormalSnapshotError('FORMAL_MAINTENANCE_OWNER_INVALID')
    with pytest.raises(m.FormalSnapshotError,match='FORMAL_MAINTENANCE_OWNER_INVALID'):
        m.PrivateDockerMySql('owned-container',secret,timeout=5).dump('owned_db',target,guard=guard)
    assert len(children)==1 and children[0].poll() is not None
    assert target.with_name('sql.gz.raw').exists() and target.with_name('sql.gz.pending').exists()
    assert target.exists() is (phase=='publication')

def test_valid_export_exact_private_bytes_and_owner_thread(monkeypatch,tmp_path):
    m=load();data='SELECT "真实字节😀";\r\n'.encode('utf-8')+os.urandom(2*1024*1024)
    secret,_,children=transport(monkeypatch,m,tmp_path,data=data);target=tmp_path/'sql.gz'
    owner=threading.get_ident();seen=[]
    def guard():seen.append(threading.get_ident());assert seen[-1]==owner
    receipt=m.PrivateDockerMySql('owned-container',secret,timeout=5).dump('owned_db',target,guard=guard)
    assert gzip.decompress(target.read_bytes())==data
    assert receipt==dict(sha256=hashlib.sha256(target.read_bytes()).hexdigest(),bytes=target.stat().st_size)
    assert len(seen)>10 and len(children)==1 and children[0].poll() is not None
    assert not target.with_name('sql.gz.raw').exists() and not target.with_name('sql.gz.pending').exists()

@pytest.mark.skipif(os.name != 'posix', reason='Linux private file modes require a POSIX filesystem')
def test_valid_export_private_posix_mode(monkeypatch,tmp_path):
    m=load();secret,_,_=transport(monkeypatch,m,tmp_path)
    target=tmp_path/'sql.gz'
    m.PrivateDockerMySql('owned-container',secret,timeout=5).dump(
        'owned_db',target,guard=lambda:None)
    assert target.stat().st_mode & 0o777 == 0o600

def test_child_failure_retains_raw_without_secret_error(monkeypatch,tmp_path):
    m=load();secret,_,children=transport(monkeypatch,m,tmp_path,status=2);target=tmp_path/'sql.gz'
    with pytest.raises(m.FormalSnapshotError,match='FORMAL_MYSQL_DUMP_FAILED') as failure:
        m.PrivateDockerMySql('owned-container',secret,timeout=5).dump('owned_db',target,guard=lambda:None)
    assert len(children)==1 and children[0].poll() is not None
    assert target.with_name('sql.gz.raw').exists() and not target.exists()
    assert secret not in str(failure.value)

def test_shorter_outer_deadline_covers_wait_and_restores_context(monkeypatch,tmp_path):
    m=load();secret,_,children=transport(monkeypatch,m,tmp_path,blocked=True);target=tmp_path/'sql.gz'
    deadline=time.monotonic()+0.25;token=m.observation_deadline.set(deadline)
    try:
        with pytest.raises(m.FormalSnapshotError,match='FORMAL_MYSQL_DUMP_TIMEOUT|FORMAL_DATA_DEADLINE|FORMAL_OBSERVATION_DEADLINE_EXCEEDED'):
            m.PrivateDockerMySql('owned-container',secret,timeout=5).dump('owned_db',target,guard=lambda:None)
        assert m.observation_deadline.get()==deadline
    finally:m.observation_deadline.reset(token)
    assert len(children)==1 and children[0].poll() is not None and not target.exists()

@pytest.mark.parametrize('observed', ['1\t1\t1\n','1\t1\t2\n','0\t1\t1\n','1\t0\t1\n','1\t1\t-1\n','1\t1\t1\nextra'])
def test_export_transaction_does_not_weaken_pre_post_zero_transactions(observed):
    m=load()
    class Mysql:
        def query(self,sql):return observed
    if observed in ('1\t1\t1\n','1\t1\t2\n'):
        actual=m.observe_database_barrier(Mysql(),dump_in_progress=True)
        assert actual==dict(readOnly=True,superReadOnly=True,activeDatabaseTransactions=int(observed.split('\t')[2]))
    else:
        with pytest.raises(m.FormalSnapshotError,match='FORMAL_DATABASE_NOT_QUIESCENT'):
            m.observe_database_barrier(Mysql(),dump_in_progress=True)
    with pytest.raises(m.FormalSnapshotError,match='FORMAL_DATABASE_NOT_QUIESCENT'):
        m.observe_database_barrier(Mysql())
