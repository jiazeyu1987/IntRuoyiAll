"""Explicit standard routing doubles; real original files, no live-server proof."""
import hashlib
import json
from pathlib import Path
from unittest.mock import patch

import pytest
from test_formal_capture_producer_entry import load, shared_request
from test_formal_external_callers import fixture


def request(tmp_path,action):
    value=shared_request(tmp_path,'restore-shared-services')
    value.update(schemaVersion='release-formal-capture-request/v4',action=action,
        externalScopeSha256='1'*64,externalOriginalHealthSha256='2'*64)
    return value


@pytest.mark.parametrize('action',['restore-external-services','observe-external-health'])
@pytest.mark.parametrize('fault',['none','extra','deadline','invalid-id','changed-request','phase-failure'])
def test_v4_external_dispatch_is_explicit_bound_and_private(tmp_path,capsys,action,fault):
    m=load();import formal_service_health as health
    selected=request(tmp_path,action)
    if fault=='extra':selected['hostWritersVerified']=True
    if fault=='deadline':selected['totalSeconds']=True
    if fault=='invalid-id':selected['externalOriginalHealthSha256']='unbound'
    path=tmp_path/'request.json';path.write_bytes(json.dumps(selected).encode());path.chmod(0o600)
    approved=hashlib.sha256(path.read_bytes()).hexdigest()
    if fault=='changed-request':path.write_bytes(b'{}')
    result={'state':'EXTERNAL_CALLERS_HEALTHY_INGRESS_HELD','hostWritersVerified':False,
        'productionEvidence':False,'claimReleased':False}
    class Claim:
        def __init__(self,*args,**kwargs):self.root=tmp_path
        def close(self):pass
    with patch.object(m,'require_native_runtime'),patch.object(health,'NativeNetworkProbe'), \
         patch.object(m,'FormalMaintenanceClaim',side_effect=Claim) as claimed, \
         patch.object(m,'run_external_request',create=True,return_value=result,
            side_effect=m.FormalSnapshotError('FORMAL_SHARED_THAW_REQUIRED') if fault=='phase-failure' else None) as dispatch, \
         patch.object(m,'run_shared_request') as shared,patch.object(m,'observe_shared_request') as observation, \
         patch.object(m,'capture_claimed_formal_payload') as legacy:
        code=m.main(['--request',str(path),'--expected-request-sha256',approved])
        output=json.loads(capsys.readouterr().out)
        assert code==(0 if fault=='none' else 1)
        shared.assert_not_called();observation.assert_not_called();legacy.assert_not_called()
        if fault=='none':assert output==result;dispatch.assert_called_once()
        elif fault=='phase-failure':assert output['code']=='FORMAL_SHARED_THAW_REQUIRED'
        else:claimed.assert_not_called();dispatch.assert_not_called()


@pytest.mark.parametrize('action',['restore-external-services','observe-external-health'])
@pytest.mark.parametrize('fault',['none','thaw','scope','health-hash','changed-request','phase','shared-health','caller-health','late-deadline'])
def test_v4_dependencies_checked_without_inflight_caller_observation(tmp_path,monkeypatch,action,fault):
    m=load();import formal_service_health as health;import formal_split_capture as split;import formal_ingress_control as ingress
    callers,claim,plan,transport=fixture(tmp_path)
    credential=tmp_path/'mysql.secret';credential.write_bytes(b'private-v4-sentinel\n');credential.chmod(0o600)
    selected=request(tmp_path,action);selected.update(mysqlCredentialFile=str(credential),claimRoot=str(claim.root))
    selected['claimBinding']=json.loads(claim._plan)
    callers.bind_external_callers(claim,plan,transport)
    transport.observe_http=lambda *args:{'httpStatus':200,'bytes':15,'sha256':'a'*64,
        'port':48081,'networkNamespaceInode':12,'pid':31}
    callers.bind_external_health_scope(claim,transport,guard=claim.assert_owned,seconds=30)
    selected['externalScopeSha256']=hashlib.sha256((claim.root/'external-callers-original.json').read_bytes()).hexdigest()
    selected['externalOriginalHealthSha256']=hashlib.sha256((claim.root/'external-callers-health-original.json').read_bytes()).hexdigest()
    claim._read=lambda path:Path(path).read_bytes()
    (claim.root/'shared-container-original.json').write_bytes(b'shared-original')
    (claim.root/'shared-health-original.json').write_bytes(b'health-original')
    for role in callers.ROLES:callers.stop_external_caller(claim,role,transport,guard=claim.assert_owned)
    if action=='observe-external-health':
        callers.restore_external_services_with_health(claim,transport,dependency_guard=claim.assert_owned,seconds=30)
    if fault=='scope':selected['externalScopeSha256']='0'*64
    if fault=='health-hash':selected['externalOriginalHealthSha256']='0'*64
    before=list(claim.events);actions=list(transport.actions);calls=[];clock=[0.]
    monkeypatch.setattr(m.time,'monotonic',lambda:clock[0])
    def thaw(_):
        calls.append('thaw')
        if fault=='thaw':raise m.FormalSnapshotError('FORMAL_SHARED_THAW_REQUIRED')
        return {'nativeState':'NOT_REQUESTED'}
    monkeypatch.setattr(split,'_thaw_receipt',thaw)
    monkeypatch.setattr(m,'verify_shared_request_binding',lambda *a:{'shared':b'shared-original','health':b'health-original'})
    monkeypatch.setattr(ingress,'assert_ingress',lambda *a:{'rulesSha256':'f'*64,
        'phase':'CAPTURE_RESTRICTED' if fault=='phase' else 'RECOVERY_DEPENDENCIES_PERMITTED'})
    monkeypatch.setattr(m,'PrivateDockerMySql',lambda *a,**k:object())
    monkeypatch.setattr(callers,'SshDockerTransport',lambda:transport)
    monkeypatch.setattr(callers,'guard_external_callers',lambda *a:(_ for _ in ()).throw(AssertionError('stopped guard during recovery')))
    def dependency(*args,**kwargs):
        calls.append('shared-health');kwargs['guard']();kwargs['thaw_guard']()
        if fault=='shared-health':raise m.FormalSnapshotError('FORMAL_HEALTH_RESPONSE_INVALID')
        if fault=='late-deadline':clock[0]=121.
        return {'state':'ORIGINAL_SERVICES_HEALTHY','productionEvidence':False}
    monkeypatch.setattr(health,'observe_shared_recovery_health',dependency)
    def guard():
        calls.append('request')
        if fault=='changed-request':raise m.FormalSnapshotError('FORMAL_CAPTURE_REQUEST_CHANGED')
    if fault=='caller-health':transport.observe_http=lambda *a:(_ for _ in ()).throw(m.FormalSnapshotError('FORMAL_HEALTH_RESPONSE_INVALID'))
    monkeypatch.setattr(ingress,'release_ingress',lambda *a,**k:(_ for _ in ()).throw(AssertionError('implicit release')))
    if fault!='none':
        with pytest.raises(m.FormalSnapshotError):m.run_external_request(claim,selected,guard)
    else:
        result=m.run_external_request(claim,selected,guard)
        assert result['state']=='EXTERNAL_CALLERS_HEALTHY_INGRESS_HELD'
        assert result['externalCallerHealthVerified'] is True and result['sharedDependencyHealthVerified'] is True
        assert result['claimReleased'] is False and result['fullRunnerCompleted'] is False and result['productionEvidence'] is False
        assert result['serviceActionsPerformed']==(action=='restore-external-services')
    if fault=='thaw':assert calls==['thaw']
    if fault in ('thaw','scope','health-hash','changed-request','phase','shared-health','late-deadline'):
        assert transport.actions==actions
    if action=='observe-external-health':assert transport.actions==actions and claim.events==before
    if fault=='none' and action=='restore-external-services':
        assert [a[2] for a in transport.actions[len(actions):]]==['start']*3
        assert result['externalHealth']['health']['test-rehearsal']['state']=='ORIGINALLY_STOPPED'
