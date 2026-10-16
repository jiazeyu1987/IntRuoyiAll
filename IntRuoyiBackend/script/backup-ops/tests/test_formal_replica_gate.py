"""Local real metadata files and explicit API doubles; not host-fence proof."""
import hashlib,json,sys
from pathlib import Path
from unittest.mock import patch
import pytest
ROOT=Path(__file__).resolve().parents[1]/'linux'
sys.path.insert(0,str(ROOT))
import formal_snapshot_linux as snapshot

def load():
 import importlib.util
 spec=importlib.util.spec_from_file_location('gate',ROOT/'formal_replica_gate.py')
 m=importlib.util.module_from_spec(spec);spec.loader.exec_module(m);return m

def source(tmp_path):
 root=tmp_path/'source';p=root/'.minio.sys/buckets/yudao/.metadata.bin';p.mkdir(parents=True)
 (p/'xl.meta').write_bytes(b'ACTUAL_METADATA_FILE_FIXTURE')
 (root/'.minio.sys/config').mkdir();(root/'.minio.sys/config/config.json').write_bytes(b'PRIVATE_CONFIG_FIXTURE')
 return root

class Client:
 def absent_configuration(self,bucket,configuration,code):
  return {'bucket':bucket,'configuration':configuration,'httpStatus':404,'errorCode':code,'rawSha256':'a'*64,'rawBytes':60}
 def replica_configuration(self,bucket,configuration):
  base={'bucket':bucket,'configuration':configuration,'rawSha256':'b'*64,'rawBytes':60,'httpStatus':200}
  if configuration=='notification':return dict(base,externalNotificationEntries=0)
  if configuration=='versioning':return dict(base,status='Enabled')
  return dict(base,enabled=True)

def test_missing_protection_and_notification_refuses_before_copy(tmp_path):
 m=load();root=source(tmp_path);observed=m.observe_bound_replica_dependencies(Client(),root,['yudao'])
 observed.pop('startupConfigurations',None)
 expected=snapshot.copy_physical_minio_tree(root,tmp_path/'capture');target=tmp_path/'replica'
 with pytest.raises(snapshot.FormalSnapshotError):m.prepare_checked_minio_replica(tmp_path/'capture',target,expected,observed,guard=fixture_owner)
 assert not target.exists()

@pytest.mark.parametrize('change',['pass','metadata','batch','tier','extra-bucket','missing-observation','scope'])
def test_checked_copy_refuses_unknown_or_changed_dependencies(tmp_path,change):
 m=load();root=source(tmp_path)
 observed=m.observe_bound_replica_dependencies(Client(),root,['yudao'])
 expected=snapshot.copy_physical_minio_tree(root,tmp_path/'capture')
 if change=='metadata':(tmp_path/'capture/.minio.sys/buckets/yudao/.metadata.bin/xl.meta').write_bytes(b'CHANGED')
 if change=='batch':(tmp_path/'capture/.minio.sys/batch-jobs').mkdir()
 if change=='tier':(tmp_path/'capture/.minio.sys/config/tier-config.bin').write_bytes(b'TIER')
 if change=='extra-bucket':(tmp_path/'capture/.minio.sys/buckets/other').mkdir()
 if change=='missing-observation':observed['bucketObservations']['observations'].pop()
 if change=='scope':observed['bucketObservations']['buckets']=['other']
 target=tmp_path/'replica'
 if change=='pass':
  result=m.prepare_checked_minio_replica(tmp_path/'capture',target,expected,observed,guard=fixture_owner)
  assert result['state']=='LOCAL_REPLICA_DEPENDENCIES_BOUND_PENDING_STARTUP_ISOLATION'
  assert result['productionEvidence'] is False
 else:
  with pytest.raises(snapshot.FormalSnapshotError):m.prepare_checked_minio_replica(tmp_path/'capture',target,expected,observed,guard=fixture_owner)
  assert not target.exists()

@pytest.mark.parametrize('path',['batch-jobs','config/tier-config.bin'])
def test_internal_dependency_refuses_before_api(tmp_path,path):
 m=load();root=source(tmp_path);p=root/'.minio.sys'/path;p.parent.mkdir(exist_ok=True);p.write_bytes(b'JOB')
 with patch.object(Client,'absent_configuration',side_effect=AssertionError('API_MUST_NOT_RUN')):
  with pytest.raises(snapshot.FormalSnapshotError):m.observe_bound_replica_dependencies(Client(),root,['yudao'])

def test_metadata_drift_during_actual_queries_refuses_observation(tmp_path):
 m=load();root=source(tmp_path);original=Client.absent_configuration
 def change(self,*args):
  value=original(self,*args);(root/'.minio.sys/buckets/yudao/.metadata.bin/xl.meta').write_bytes(b'DRIFT');return value
 with patch.object(Client,'absent_configuration',change):
  with pytest.raises(snapshot.FormalSnapshotError):m.observe_bound_replica_dependencies(Client(),root,['yudao'])

def test_usage_cache_is_not_bucket_configuration_but_remains_in_capture(tmp_path):
 m=load();root=source(tmp_path);observed=m.observe_bound_replica_dependencies(Client(),root,['yudao'])
 cache=root/'.minio.sys/buckets/.usage-cache.bin';cache.mkdir();(cache/'xl.meta').write_bytes(b'USAGE_CACHE')
 per_bucket=root/'.minio.sys/buckets/yudao/.usage-cache.bin';per_bucket.mkdir();(per_bucket/'xl.meta').write_bytes(b'PER_BUCKET_USAGE')
 expected=snapshot.copy_physical_minio_tree(root,tmp_path/'capture')
 result=m.prepare_checked_minio_replica(tmp_path/'capture',tmp_path/'replica',expected,observed,guard=fixture_owner)
 assert result['metadataScope']==observed['metadataScope']
 assert (tmp_path/'replica/.minio.sys/buckets/.usage-cache.bin/xl.meta').read_bytes()==b'USAGE_CACHE'

@pytest.mark.parametrize('raw',[b'\x01\x00\x01\x00',b'\x01\x00\x01\x00\x80',b'\x02\x00\x01\x00'])
def test_stopped_minio_mrf_requires_exact_empty_pinned_header(tmp_path,raw):
 m=load();root=source(tmp_path);observed=m.observe_bound_replica_dependencies(Client(),root,['yudao'])
 heal=root/'.minio.sys/buckets/.heal/mrf';heal.mkdir(parents=True);(heal/'list.bin').write_bytes(raw)
 expected=snapshot.copy_physical_minio_tree(root,tmp_path/'capture');target=tmp_path/'replica'
 if raw==b'\x01\x00\x01\x00':
  m.prepare_checked_minio_replica(tmp_path/'capture',target,expected,observed,guard=fixture_owner)
  assert (target/'.minio.sys/buckets/.heal/mrf/list.bin').read_bytes()==raw
 else:
  with pytest.raises(snapshot.FormalSnapshotError):m.prepare_checked_minio_replica(tmp_path/'capture',target,expected,observed,guard=fixture_owner)
  assert not target.exists()


def fixture_owner():
    """Explicit owner protocol fixture; no maintained server protection."""
    pass
