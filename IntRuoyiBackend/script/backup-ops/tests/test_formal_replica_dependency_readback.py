"""Real local copied metadata, explicit dependency API actors; no live MinIO."""
import hashlib,json,sys
from pathlib import Path
from unittest.mock import patch
import pytest
sys.path.insert(0,str(Path(__file__).resolve().parents[1]/'linux'))
import formal_replica_producer as producer
import formal_replica_gate as gate
import formal_snapshot_linux as snapshot
from test_formal_replica_gate import source,Client
from test_formal_replica_runtime import IMAGE


@pytest.mark.parametrize('change',[
 'pass','dynamic-absence','lifecycle-present','replication-present','encryption-present',
 'read-fails','metadata-before','metadata-during','versioning-status','versioning-bytes',
 'object-lock-retention','object-lock-absence','notification','readback-sync-fails'])
def test_normal_producer_reads_same_live_replica_before_export(tmp_path,change):
    root=source(tmp_path)
    other=root/'.minio.sys/buckets/intruoyi-test-next-edhr/.metadata.bin'
    other.mkdir(parents=True);(other/'xl.meta').write_bytes(b'EDHR_METADATA_FIXTURE')
    # Deliberately different from metadataScope's sorted order.
    buckets=['yudao','intruoyi-test-next-edhr']
    observed=gate.observe_bound_replica_dependencies(Client(),root,buckets)
    physical=snapshot.copy_physical_minio_tree(root,tmp_path/'capture')
    evidence=tmp_path/'producer';calls=[];steps=[]

    class LiveClient(Client):
        def absent_configuration(self,bucket,configuration,code):
            calls.append((bucket,configuration))
            if change==configuration+'-present':
                raise snapshot.FormalSnapshotError('FORMAL_S3_REPLICA_EXTERNAL_DEPENDENCY_PRESENT')
            if change=='read-fails':raise snapshot.FormalSnapshotError('FORMAL_S3_HTTP_403')
            row=super().absent_configuration(bucket,configuration,code)
            if change=='dynamic-absence':row.update(rawSha256='c'*64,rawBytes=91)
            if change=='metadata-during':
                (evidence/'replica-data/.minio.sys/config/config.json').write_bytes(b'DRIFT')
            return row

        def replica_configuration(self,bucket,configuration):
            calls.append((bucket,configuration))
            row=super().replica_configuration(bucket,configuration)
            if change=='versioning-status' and configuration=='versioning':row['status']='Suspended'
            if change=='versioning-bytes' and configuration=='versioning':row['rawSha256']='d'*64
            if change=='object-lock-retention' and configuration=='object-lock':row['rawSha256']='e'*64
            if change=='object-lock-absence' and configuration=='object-lock':
                row.update(httpStatus=404,enabled=False,errorCode='ObjectLockConfigurationNotFoundError')
            if change=='notification' and configuration=='notification':
                raise snapshot.FormalSnapshotError('FORMAL_REPLICA_NOTIFICATION_PRESENT')
            return row

    client=LiveClient()
    def create(*args,**kwargs):
        steps.append('create')
        return {'containerId':'1'*64,'imageId':IMAGE,'productionEvidence':False}
    def health(identity,*,guard):
        assert identity=='1'*64;steps.append('health')
        if change=='metadata-before':
            (evidence/'replica-data/.minio.sys/config/config.json').write_bytes(b'DRIFT')
    def export(actual,bucket_scope,*args,**kwargs):
        assert actual is client and bucket_scope==sorted(buckets)
        steps.append('export')
        return {'state':'S3_EXPORTED_PENDING_RESTORE_PROOF','inventorySha256':'2'*64}
    def sync(path):
        if change=='readback-sync-fails' and (evidence/'replica-dependency-readback.json').exists():
            raise OSError('OWNED_READBACK_SYNC_FAILURE')
    with patch.object(producer,'create_and_start_checked_replica',create), \
         patch.object(producer,'_wait_live',health), \
         patch.object(producer,'ContainerReadOnlyS3',return_value=client) as constructor, \
         patch.object(producer,'export_s3_versions',export),patch.object(producer,'_sync_directory',sync):
        if change in ('pass','dynamic-absence'):
            result=producer.produce_isolated_replica_export('operation-1',tmp_path/'capture',physical,
                observed,tmp_path/'env','fixture-access','fixture-secret',evidence,image_id=IMAGE,guard=fixture_owner)
            assert steps==['create','health','export']
            assert len(calls)==12 and set(calls)=={(b,c) for b in buckets for c in
                ('lifecycle','replication','encryption','notification','versioning','object-lock')}
            raw=(evidence/'replica-dependency-readback.json').read_bytes()
            readback=json.loads(raw)
            assert result['dependencyReadbackSha256']==hashlib.sha256(raw).hexdigest()
            assert readback['operationId']=='operation-1' and readback['containerId']=='1'*64
            assert readback['imageId']==IMAGE and readback['physicalTreeDigest']==physical['physicalTreeDigest']
            assert readback['sourceObservationSha256']==result['observationSha256']
            assert readback['observation']['metadataScope']==observed['metadataScope']
            if change=='dynamic-absence':
                assert readback['observation']['bucketObservations']['observations'][0]['rawBytes']==91
            assert result['state']=='REPLICA_EXPORTED_PENDING_RESTORE_AND_FORMAL_BARRIERS'
            assert result['productionEvidence'] is False and not (evidence/'producer-completion.json').exists()
        else:
            with pytest.raises((snapshot.FormalSnapshotError,OSError)):
                producer.produce_isolated_replica_export('operation-1',tmp_path/'capture',physical,
                    observed,tmp_path/'env','fixture-access','fixture-secret',evidence,image_id=IMAGE,guard=fixture_owner)
            assert steps==['create','health'] and not (evidence/'replica-export-stage.json').exists()
            assert (evidence/'replica-data').is_dir()
        constructor.assert_called_once_with('1'*64,'fixture-access','fixture-secret',timeout=60,guard=fixture_owner)


@pytest.mark.parametrize('change',['dynamic-lock-absence','wrong-absence','duplicate','missing','extra','protection-raw','order'])
def test_dependency_equivalence_preserves_exact_scope_and_protection(tmp_path,change):
    import copy
    observed=gate.observe_bound_replica_dependencies(Client(),source(tmp_path),['yudao'])
    lock=observed['startupConfigurations'][-1]
    lock.update(httpStatus=404,enabled=False,errorCode='ObjectLockConfigurationNotFoundError')
    current=copy.deepcopy(observed)
    if change=='dynamic-lock-absence':current['startupConfigurations'][-1].update(rawSha256='c'*64,rawBytes=98)
    if change=='wrong-absence':current['bucketObservations']['observations'][0]['errorCode']='NoSuchBucket'
    if change=='duplicate':current['startupConfigurations'][1]=copy.deepcopy(current['startupConfigurations'][0])
    if change=='missing':current['bucketObservations']['observations'].pop()
    if change=='extra':current['startupConfigurations'][0]['extra']=True
    if change=='protection-raw':current['startupConfigurations'][1]['rawSha256']='d'*64
    if change=='order':
        current['bucketObservations']['observations'].reverse();current['startupConfigurations'].reverse()
    if change in ('dynamic-lock-absence','order'):
        gate.verify_started_replica_dependencies(observed,current)
    else:
        with pytest.raises(snapshot.FormalSnapshotError):gate.verify_started_replica_dependencies(observed,current)


def fixture_owner():
    """Explicit owner protocol fixture; no maintained server protection."""
    pass
