import sys
from pathlib import Path
from unittest.mock import patch
import pytest
sys.path.insert(0,str(Path(__file__).resolve().parents[1]/'linux'))
from formal_snapshot_linux import FormalSnapshotError
from test_formal_replica_runtime import inputs,IMAGE

@pytest.mark.parametrize('change',['pass','export-fails','raw-drift','existing'])
def test_producer_actual_stage_dispatch_never_emits_formal_completion(tmp_path,change):
    import formal_replica_producer as p
    from test_formal_replica_gate import source,Client
    import formal_snapshot_linux as s
    import formal_replica_gate as g
    root=source(tmp_path);observed=g.observe_bound_replica_dependencies(Client(),root,['yudao'])
    physical=s.copy_physical_minio_tree(root,tmp_path/'capture')
    evidence=tmp_path/'producer'
    if change=='existing':evidence.mkdir()
    steps=[]
    def create(*args,**kwargs):
        steps.append('create')
        return {'containerId':'1'*64,'imageId':IMAGE,'productionEvidence':False}
    def export(*args,**kwargs):
        steps.append('export')
        if change=='export-fails':raise FormalSnapshotError('FORMAL_S3_HTTP_403')
        if change=='raw-drift':(tmp_path/'capture/.minio.sys/config/config.json').write_bytes(b'DRIFT')
        return {'state':'S3_EXPORTED_PENDING_RESTORE_PROOF','inventorySha256':'2'*64}
    with patch.object(p,'_wait_live'),patch.object(p,'create_and_start_checked_replica',create),patch.object(p,'export_s3_versions',export),patch.object(p,'ContainerReadOnlyS3',return_value=Client()),patch.object(p,'_sync_directory'):
        if change=='pass':
            result=p.produce_isolated_replica_export('operation-1',tmp_path/'capture',physical,observed,
                tmp_path/'env', 'access','secret',evidence,image_id=IMAGE,guard=fixture_owner)
            assert steps==['create','export'] and result['productionEvidence'] is False
            assert result['state']=='REPLICA_EXPORTED_PENDING_RESTORE_AND_FORMAL_BARRIERS'
            assert not (evidence/'producer-completion.json').exists()
        else:
            with pytest.raises(FormalSnapshotError):p.produce_isolated_replica_export('operation-1',tmp_path/'capture',physical,observed,
                tmp_path/'env','access','secret',evidence,image_id=IMAGE,guard=fixture_owner)
            assert not (evidence/'replica-export-stage.json').exists()


def fixture_owner():
    """Explicit owner protocol fixture; no maintained server protection."""
    pass
