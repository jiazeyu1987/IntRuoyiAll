import copy,json,sys
from pathlib import Path
from unittest.mock import patch
import pytest
sys.path.insert(0,str(Path(__file__).resolve().parents[1]/'linux'))
import formal_snapshot_linux as snapshot
import formal_replica_gate as gate

IMAGE='sha256:064117214caceaa8d8a90ef7caa58f2b2aeb316b5156afe9ee8da5b4d83e12c8'

def inputs(tmp_path):
    from test_formal_replica_gate import source,Client
    root=source(tmp_path);observed=gate.observe_bound_replica_dependencies(Client(),root,['yudao'])
    physical=snapshot.copy_physical_minio_tree(root,tmp_path/'capture')
    proof=gate.prepare_checked_minio_replica(tmp_path/'capture',tmp_path/'replica',physical,observed,guard=fixture_owner)
    record={'Id':'1'*64,'Image':IMAGE,'State':{'Status':'created','Running':False},
      'Config':{'Entrypoint':['/usr/bin/minio'],'Cmd':['server','/data'],
       'Env':['PATH=/usr/bin','MINIO_ROOT_USER=private','MINIO_ROOT_PASSWORD=private'],
       'Labels':{'intruoyi.formal-replica-operation':'operation-1'}},
      'HostConfig':{'NetworkMode':'none','ReadonlyRootfs':True,'Privileged':False,'CapAdd':None,
       'CapDrop':['ALL'],'SecurityOpt':['no-new-privileges'],'PidMode':'','IpcMode':'private',
       'Devices':[],'PortBindings':{},'RestartPolicy':{'Name':'no','MaximumRetryCount':0},
       'Tmpfs':{'/tmp':'rw,noexec,nosuid,size=67108864'}},
      'Mounts':[{'Type':'bind','Source':str(tmp_path/'replica'),'Destination':'/data','RW':True}]}
    return observed,physical,proof,record

@pytest.mark.parametrize('change',['pass','network','privileged','mount','extra-mount','image','command',
    'environment','port','capability','restart','running','owner','proof','tree'])
def test_start_requires_actual_stopped_resource_and_bound_tree(tmp_path,change):
    observed,physical,proof,record=inputs(tmp_path)
    if change=='network':record['HostConfig']['NetworkMode']='host'
    if change=='privileged':record['HostConfig']['Privileged']=True
    if change=='mount':record['Mounts'][0]['Source']=str(tmp_path/'source')
    if change=='extra-mount':record['Mounts'].append({'Destination':'/var/run/docker.sock'})
    if change=='image':record['Image']='sha256:'+'2'*64
    if change=='command':record['Config']['Cmd']=['server','/original']
    if change=='environment':record['Config']['Env'].append('MINIO_NOTIFY_WEBHOOK_ENABLE=on')
    if change=='port':record['HostConfig']['PortBindings']={'9000/tcp':[{'HostPort':'9000'}]}
    if change=='capability':record['HostConfig']['CapAdd']=['SYS_ADMIN']
    if change=='restart':record['HostConfig']['RestartPolicy']['Name']='always'
    if change=='running':record['State']={'Status':'running','Running':True}
    if change=='owner':record['Config']['Labels']['intruoyi.formal-replica-operation']='other'
    if change=='proof':proof['observationSha256']='3'*64
    if change=='tree':(tmp_path/'replica/.minio.sys/config/config.json').write_bytes(b'DRIFT')
    calls=[]
    def run(args,**kwargs):
        calls.append(args)
        from subprocess import CompletedProcess
        if args[1:3]==['image','inspect']:
            return CompletedProcess(args,0,json.dumps([{'Id':IMAGE,'RepoTags':[gate.PINNED_MINIO_TAG],'Config':{'Env':['PATH=/usr/bin']}}]).encode(),b'')
        if args[1]=='inspect':return CompletedProcess(args,0,json.dumps([copy.deepcopy(record)]).encode(),b'')
        assert args==['docker','start','1'*64]
        return CompletedProcess(args,0,('1'*64+'\n').encode(),b'')
    with patch.object(gate,'_guarded_run',run):
        if change=='pass':
            result=gate.start_checked_minio_replica('1'*64,'operation-1',tmp_path/'replica',physical,observed,proof,image_id=IMAGE,guard=fixture_owner)
            assert result['state']=='ISOLATED_REPLICA_STARTED_PENDING_READBACK'
            assert result['productionEvidence'] is False
        else:
            with pytest.raises(snapshot.FormalSnapshotError):
                gate.start_checked_minio_replica('1'*64,'operation-1',tmp_path/'replica',physical,observed,proof,image_id=IMAGE,guard=fixture_owner)
            assert not any(args[1]=='start' for args in calls)


def fixture_owner():
    """Explicit owner protocol fixture; no maintained server protection."""
    pass
