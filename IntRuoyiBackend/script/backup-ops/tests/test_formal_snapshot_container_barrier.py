"""Actual-record boundary tests, not a six-writer production attestation."""
import copy,importlib.util
from pathlib import Path
import pytest

SOURCE=Path(__file__).resolve().parents[1]/'linux/formal_snapshot_linux.py'
SOURCE_ROOT='/opt/intruoyi/runtime/data/minio'
IDENTITY='a'*64
def load():
    s=importlib.util.spec_from_file_location('formal_container_barrier',SOURCE)
    m=importlib.util.module_from_spec(s);s.loader.exec_module(m);return m
def records():
    return [{'id':IDENTITY,'name':'/ragflow_compose-minio-1',
        'state':{'Running':False,'ExitCode':0,'OOMKilled':False},
        'mounts':[{'Source':SOURCE_ROOT,'Destination':'/data','RW':True}],
        'privileged':False,'capAdd':None}]

def test_stopped_bound_container_observation_is_explicitly_partial():
    result=load().observe_stopped_object_containers(records(),SOURCE_ROOT,IDENTITY)
    assert result['boundContainerId']==IDENTITY
    assert result['hostWritersVerified'] is False

@pytest.mark.parametrize('case',['minio-running','parent-rw','descendant-rw','socket','privileged','admin-cap','wrong-id','nonzero','oom','missing','duplicate'])
def test_live_or_unbound_object_writers_refuse(case):
    m=load();data=records()
    if case=='minio-running':data[0]['state']['Running']=True
    if case=='wrong-id':data[0]['id']='b'*64
    if case=='nonzero':data[0]['state']['ExitCode']=137
    if case=='oom':data[0]['state']['OOMKilled']=True
    if case=='missing':data=[]
    if case=='duplicate':data.append(copy.deepcopy(data[0]))
    if case in ('parent-rw','descendant-rw','socket','privileged','admin-cap'):
        actor=copy.deepcopy(records()[0]);actor['id']='c'*64;actor['name']='/actor';actor['state']['Running']=True
        actor['mounts']=[]
        if case=='parent-rw':actor['mounts']=[{'Source':'/opt/intruoyi/runtime','RW':True}]
        if case=='descendant-rw':actor['mounts']=[{'Source':SOURCE_ROOT+'/yudao','RW':True}]
        if case=='socket':actor['mounts']=[{'Source':'/var/run/docker.sock','RW':True}]
        if case=='privileged':actor['privileged']=True
        if case=='admin-cap':actor['capAdd']=['SYS_ADMIN']
        data.append(actor)
    with pytest.raises(m.FormalSnapshotError):m.observe_stopped_object_containers(data,SOURCE_ROOT,IDENTITY)

def test_readonly_source_reader_can_remain_running():
    m=load();data=records();reader=copy.deepcopy(data[0]);reader['id']='c'*64
    reader['name']='/reader';reader['state']['Running']=True;reader['mounts'][0]['RW']=False;data.append(reader)
    assert m.observe_stopped_object_containers(data,SOURCE_ROOT,IDENTITY)['hostWritersVerified'] is False
