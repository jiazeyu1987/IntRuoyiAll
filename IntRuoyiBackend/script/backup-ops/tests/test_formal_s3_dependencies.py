"""Explicit signed-response doubles; no claim of frozen production proof."""
import io,urllib.error
import pytest
from test_formal_s3_export import load

@pytest.mark.parametrize('case',['exact','wrong-code','forbidden','too-large','invalid-xml'])
def test_configuration_absence_requires_exact_error_code(monkeypatch,case):
 m=load();client=m.ReadOnlyS3('http://127.0.0.1:9000','owned','owned-secret')
 raw=b'<Error><Code>NoSuchLifecycleConfiguration</Code></Error>'
 if case=='wrong-code':raw=b'<Error><Code>NoSuchBucket</Code></Error>'
 if case=='too-large':raw=b'x'*16385
 if case=='invalid-xml':raw=b'not xml'
 def request(*args,**kwargs):
  raise urllib.error.HTTPError('http://owned',403 if case=='forbidden' else 404,'PRIVATE',{},io.BytesIO(raw))
 monkeypatch.setattr(m,'_open_signed',request)
 if case=='exact':
  observation=client.absent_configuration('yudao','lifecycle','NoSuchLifecycleConfiguration')
  assert observation['httpStatus']==404 and observation['errorCode']=='NoSuchLifecycleConfiguration'
 else:
  with pytest.raises(m.FormalSnapshotError):client.absent_configuration('yudao','lifecycle','NoSuchLifecycleConfiguration')

def test_existing_configuration_refuses_even_if_empty(monkeypatch):
 m=load();client=m.ReadOnlyS3('http://127.0.0.1:9000','owned','owned-secret')
 monkeypatch.setattr(m,'_open_signed',lambda *args,**kwargs:io.BytesIO(b'<LifecycleConfiguration/>'))
 with pytest.raises(m.FormalSnapshotError):client.absent_configuration('yudao','lifecycle','NoSuchLifecycleConfiguration')

def test_observation_records_all_three_fixed_configurations(monkeypatch):
 m=load();client=m.ReadOnlyS3('http://127.0.0.1:9000','owned','owned-secret');calls=[]
 def observe(bucket,configuration,error_code):
  calls.append((bucket,configuration,error_code))
  return {'bucket':bucket,'configuration':configuration,'httpStatus':404,'errorCode':error_code,'rawSha256':'a'*64,'rawBytes':50}
 monkeypatch.setattr(client,'absent_configuration',observe)
 result=m.observe_replica_bucket_dependencies(client,['yudao'])
 assert [c[1] for c in calls]==['lifecycle','replication','encryption']
 assert result['state']=='BUCKET_DEPENDENCIES_OBSERVED_PENDING_FROZEN_BINDING'
 assert result['replicaStartupApproved'] is False

@pytest.mark.parametrize('configuration,xml,passes',[
 ('notification',b'<NotificationConfiguration/>',True),
 ('notification',b'<NotificationConfiguration><QueueConfiguration><Queue>private</Queue></QueueConfiguration></NotificationConfiguration>',False),
 ('notification',b'<Wrong/>',False),
 ('versioning',b'<VersioningConfiguration><Status>Enabled</Status></VersioningConfiguration>',True),
 ('versioning',b'<VersioningConfiguration/>',True),
 ('versioning',b'<VersioningConfiguration><Status>Enabled</Status><Status>Suspended</Status></VersioningConfiguration>',False),
 ('object-lock',b'<ObjectLockConfiguration><ObjectLockEnabled>Enabled</ObjectLockEnabled><Rule><DefaultRetention><Mode>COMPLIANCE</Mode><Days>7</Days></DefaultRetention></Rule></ObjectLockConfiguration>',True),
 ('object-lock',b'<ObjectLockConfiguration><ObjectLockEnabled>Wrong</ObjectLockEnabled></ObjectLockConfiguration>',False),
])
def test_protection_and_external_notification_observation(monkeypatch,configuration,xml,passes):
 m=load();client=m.ReadOnlyS3('http://127.0.0.1:9000','owned','owned-secret')
 monkeypatch.setattr(m,'_open_signed',lambda *args,**kwargs:io.BytesIO(xml))
 if passes:
  result=client.replica_configuration('yudao',configuration)
  assert result['configuration']==configuration and result['rawBytes']==len(xml)
 else:
  with pytest.raises(m.FormalSnapshotError):client.replica_configuration('yudao',configuration)

def test_object_lock_absence_uses_exact_error_code(monkeypatch):
 m=load();client=m.ReadOnlyS3('http://127.0.0.1:9000','owned','owned-secret')
 def request(*args,**kwargs):
  raise urllib.error.HTTPError('http://owned',404,'private',{},io.BytesIO(b'<Error><Code>ObjectLockConfigurationNotFoundError</Code></Error>'))
 monkeypatch.setattr(m,'_open_signed',request)
 assert client.replica_configuration('yudao','object-lock')['httpStatus']==404
