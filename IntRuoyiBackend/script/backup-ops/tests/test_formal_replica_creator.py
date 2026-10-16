import json,sys
from pathlib import Path
from unittest.mock import patch
import pytest
sys.path.insert(0,str(Path(__file__).resolve().parents[1]/'linux'))
import formal_snapshot_linux as snapshot
import formal_replica_gate as gate
from test_formal_replica_runtime import inputs,IMAGE

@pytest.mark.parametrize('change',['pass','existing-intent','wrong-image','extra-env','bad-create'])
def test_durable_creator_refuses_replay_or_prerequisite_drift(tmp_path,change):
    observed,physical,proof,_=inputs(tmp_path)
    env=tmp_path/'private.env';env.write_text('MINIO_ROOT_USER=private-access\nMINIO_ROOT_PASSWORD=private-secret\n');env.chmod(0o600)
    evidence=tmp_path/'creator'
    if change=='existing-intent':evidence.mkdir();(evidence/'intent.json').write_text('prior')
    if change=='extra-env':env.write_text(env.read_text()+'MINIO_NOTIFY_WEBHOOK_ENABLE=on\n')
    calls=[]
    def run(args,**kwargs):
        from subprocess import CompletedProcess
        calls.append(args)
        if args[1:3]==['image','inspect']:
            return CompletedProcess(args,0,json.dumps([{'Id':IMAGE if change!='wrong-image' else 'sha256:'+'2'*64,'RepoTags':[gate.PINNED_MINIO_TAG]}]).encode(),b'')
        assert args[1]=='create'
        assert (evidence/'intent.json').is_file() and not (evidence/'created.json').exists()
        return CompletedProcess(args,1 if change=='bad-create' else 0,('1'*64+'\n').encode(),b'PRIVATE_FAILURE')
    # This unit test doubles directory fsync explicitly; actual durable fsync
    # and real Docker creator are checked in the independent Linux integration.
    with patch.object(gate,'_sync_directory'),patch.object(gate,'_guarded_run',run),patch.object(gate,'start_checked_minio_replica',return_value={'state':'ISOLATED_REPLICA_STARTED_PENDING_READBACK','productionEvidence':False,'containerId':'1'*64}) as start:
        if change=='pass':
            result=gate.create_and_start_checked_replica('operation-1',tmp_path/'replica',physical,observed,proof,env,evidence,image_id=IMAGE,guard=fixture_owner)
            assert result['containerId']=='1'*64
            assert (evidence/'intent.json').exists() and (evidence/'created.json').exists() and (evidence/'started.json').exists()
            assert 'private-secret' not in ''.join(p.read_text() for p in evidence.iterdir())
            assert 'private-secret' not in repr(calls)
            assert start.call_count==1
        else:
            with pytest.raises(snapshot.FormalSnapshotError):
                gate.create_and_start_checked_replica('operation-1',tmp_path/'replica',physical,observed,proof,env,evidence,image_id=IMAGE,guard=fixture_owner)
            assert start.call_count==0
            if change=='bad-create':assert (evidence/'intent.json').exists() and not (evidence/'created.json').exists()


def fixture_owner():
    """Explicit owner protocol fixture; no maintained server protection."""
    pass
