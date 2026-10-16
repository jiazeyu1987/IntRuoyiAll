import importlib.util,json
from pathlib import Path
import pytest
SOURCE=Path(__file__).resolve().parents[1]/'linux/formal_snapshot_linux.py'
UUID='0fa6c22b-5a90-11f1-ade1-0242ac150002'
def load():
    spec=importlib.util.spec_from_file_location('db_fence',SOURCE)
    m=importlib.util.module_from_spec(spec);spec.loader.exec_module(m);return m
class Mysql:
    def __init__(self):self.flags='0\t0';self.calls=[]
    def query(self,sql):
        self.calls.append(sql)
        if '@@server_uuid' in sql:return UUID+'\t'+self.flags+'\n'
        if 'SET GLOBAL super_read_only=ON' in sql:self.flags='1\t1';return ''
        if 'SET GLOBAL super_read_only=OFF' in sql:self.flags='0\t0';return ''
        return self.flags+'\t0\n'
def test_acquire_then_explicit_restore_original_flags(tmp_path):
    m=load();mysql=Mysql();intent=tmp_path/'intent.json'
    m.acquire_database_fence(mysql,UUID,intent)
    assert mysql.flags=='1\t1'
    saved=intent.read_bytes();data=json.loads(saved)
    assert data['readOnly']==0 and data['superReadOnly']==0
    m.restore_database_fence(mysql,UUID,intent)
    assert mysql.flags=='0\t0' and intent.read_bytes()==saved
@pytest.mark.parametrize('case',['wrong-uuid','already-held','existing'])
def test_acquire_refuses_before_set(tmp_path,case):
    m=load();mysql=Mysql();intent=tmp_path/'intent.json'
    if case=='already-held':mysql.flags='1\t1'
    if case=='existing':intent.write_text('KEEP')
    with pytest.raises(m.FormalSnapshotError):m.acquire_database_fence(mysql,'b'*36 if case=='wrong-uuid' else UUID,intent)
    assert not any('SET GLOBAL' in sql for sql in mysql.calls)
def test_restore_refuses_unheld_state(tmp_path):
    m=load();mysql=Mysql();intent=tmp_path/'intent.json';m.acquire_database_fence(mysql,UUID,intent)
    mysql.flags='0\t0';mysql.calls=[]
    with pytest.raises(m.FormalSnapshotError):m.restore_database_fence(mysql,UUID,intent)
    assert not any('SET GLOBAL' in sql for sql in mysql.calls)
