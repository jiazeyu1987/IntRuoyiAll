"""Fresh standard original binding; private files with explicit runtime doubles."""
import copy
import hashlib
import json
import os
from pathlib import Path
from unittest.mock import patch

import pytest
from test_formal_capture_producer_entry import load
from test_formal_external_callers import fixture as external_fixture
from test_formal_split_capture import fixture as shared_fixture, ROLES


def selected(tmp_path,plan=None,rows=None,external_rows=None):
    binding={'operationId':'owned-split','sourceFingerprint':'a'*64,
        'databaseUuid':'0fa6c22b-5a90-11f1-ade1-0242ac150002','minioContainerId':'7'*64}
    return {'schemaVersion':'release-formal-capture-request/v5','action':'bind-maintenance-originals',
        'claimRoot':str(tmp_path/'claim'),'claimBinding':binding,
        'mysqlCredentialFile':str(tmp_path/'mysql.secret'),'totalSeconds':120,
        'sharedContainerIds':{role:str(i)*64 for i,role in enumerate(ROLES,1)},
        'externalCallers':plan or {},'sourceRoot':str(tmp_path/'source/minio'),
        'sourceMountBinding':{'operationId':binding['operationId'],
            'bootId':'00000000-0000-0000-0000-000000000001',
            'mount':{'source':'/dev/owned','device':'9:1','fstype':'ext4','target':'/owned/source'}},
        'expectedSharedScopeSha256':hashlib.sha256(load().snapshot._canonical_bytes(
            {role:rows[str(i)*64] for i,role in enumerate(ROLES,1)})).hexdigest() if rows else 'a'*64,
        'expectedExternalScopeSha256':hashlib.sha256(load().snapshot._canonical_bytes(
            {'plan':plan,'originals':{role:external_rows[v['containerId']] for role,v in plan.items()}})).hexdigest() if external_rows else 'b'*64,
        'expectedDatabaseFlags':{'serverUuid':binding['databaseUuid'],'readOnly':1,'superReadOnly':0}}


@pytest.mark.parametrize('fault',['none','extra','deadline','minio','mysql','flags','mount','external','runtime','drift','failure'])
def test_v5_fresh_explicit_entry_never_routes_mutating_actions(tmp_path,capsys,fault):
    m=load();import formal_service_health as health
    child=tmp_path/'external';child.mkdir();_,_,plan,_=external_fixture(child)
    value=selected(tmp_path,plan)
    # selected() needs runtime rows only for a production digest, not this dispatcher double.
    if fault=='extra':value['hostWritersVerified']=True
    if fault=='deadline':value['totalSeconds']=True
    if fault=='minio':value['sharedContainerIds']['minio']='8'*64
    if fault=='mysql':value['sharedContainerIds']['mysql']='intruoyi-mysql'
    if fault=='flags':value['expectedDatabaseFlags']['superReadOnly']=1
    if fault=='mount':value['sourceMountBinding']['operationId']='other'
    if fault=='external':value['externalCallers'].pop('backup-rehearsal')
    path=tmp_path/'request.json';path.write_bytes(m.snapshot._canonical_bytes(value));path.chmod(0o600)
    digest=hashlib.sha256(path.read_bytes()).hexdigest()
    if fault=='drift':path.write_bytes(b'{}')
    result={'state':'MAINTENANCE_ORIGINALS_BOUND_PENDING_EXCLUSION','productionEvidence':False,
        'hostWritersVerified':False,'serviceActionsPerformed':False,'fullRunnerCompleted':False}
    class Claim:
        def __init__(self,root,binding,*,resume):
            assert root==value['claimRoot'] and binding==value['claimBinding'] and resume is False
            self.root=tmp_path/'claim'
        def close(self):pass
    with patch.object(m,'require_native_runtime',side_effect=m.WatchdogError('WATCHDOG_RUNTIME_UNSUPPORTED') if fault=='runtime' else None), \
         patch.object(health,'NativeNetworkProbe'),patch.object(m,'verify_initialization_devices',create=True), \
         patch.object(m,'FormalMaintenanceClaim',side_effect=Claim) as claim, \
         patch.object(m,'run_initialization_request',create=True,return_value=result,
             side_effect=m.FormalSnapshotError('FORMAL_CALLER_RUNTIME_CHANGED') if fault=='failure' else None) as dispatch, \
         patch.object(m,'run_shared_request') as shared,patch.object(m,'run_external_request') as external, \
         patch.object(m,'observe_shared_request') as observation,patch.object(m,'capture_claimed_formal_payload') as legacy:
        code=m.main(['--request',str(path),'--expected-request-sha256',digest])
        output=json.loads(capsys.readouterr().out)
        assert code==(0 if fault=='none' else 1)
        shared.assert_not_called();external.assert_not_called();observation.assert_not_called();legacy.assert_not_called()
        if fault=='none':assert output==result;dispatch.assert_called_once()
        elif fault=='failure':assert output['code']=='FORMAL_CALLER_RUNTIME_CHANGED'
        else:claim.assert_not_called();dispatch.assert_not_called()


@pytest.mark.parametrize('fault',['none','already-bound','scope','external-scope','flags','source','request',
    'shared-health','caller-health','late-journal','late-read','final-flags-drift','final-file-drift',
    'nested-request-mutation','partial-replay'])
def test_original_binding_rechecks_all_files_and_original_runtime_without_mutation(tmp_path,monkeypatch,fault):
    m=load();import formal_service_health as health
    split,claim,mysql,source,rows,_,_,patchers=shared_fixture(tmp_path)
    ext=tmp_path/'external';ext.mkdir();callers,_,plan,transport=external_fixture(ext)
    value=selected(tmp_path,plan,rows,transport.rows);value['sourceRoot']=str(source)
    value['claimRoot']=str(claim.root)
    claim._plan=m.snapshot._canonical_bytes(value['claimBinding'])
    credential=tmp_path/'mysql.secret';credential.write_bytes(b'private-original-sentinel\n');credential.chmod(0o600)
    flags=copy.deepcopy(value['expectedDatabaseFlags']);clock=[0.];checks=[]
    monkeypatch.setattr(m.time,'monotonic',lambda:clock[0])
    monkeypatch.setattr(m.snapshot,'_database_flags',lambda *a:copy.deepcopy(flags))
    monkeypatch.setattr(m,'PrivateDockerMySql',lambda *a,**k:mysql)
    monkeypatch.setattr(callers,'SshDockerTransport',lambda:transport)
    transport.observe_http=lambda *a:{'httpStatus':200,'bytes':15,'sha256':'a'*64,
        'port':48081,'networkNamespaceInode':12,'pid':31}
    class Probe:
        def observe(self,role,record,seconds):
            if fault=='shared-health':raise m.FormalSnapshotError('FORMAL_HEALTH_RESPONSE_INVALID')
            if role=='redis':return {'response':'PONG'}
            body=b'{"status":"UP"}' if role=='backend' else b'true' if role=='onlyoffice' else b''
            if role in ('frontend','showroom'):body=b'<html>original</html>'
            return health.validate_http(role,200,body)
    monkeypatch.setattr(health,'NativeNetworkProbe',Probe)
    def devices(request):
        checks.append('devices')
        if fault=='source':raise m.FormalSnapshotError('FORMAL_CAPTURE_CONTROL_DEVICE_CHANGED')
    monkeypatch.setattr(m,'verify_initialization_devices',devices,raising=False)
    if fault=='already-bound':claim.record('external-caller-scope',{})
    if fault=='scope':value['expectedSharedScopeSha256']='0'*64
    if fault=='external-scope':value['expectedExternalScopeSha256']='0'*64
    if fault=='flags':flags['readOnly']=0
    if fault=='caller-health':transport.observe_http=lambda *a:(_ for _ in ()).throw(m.FormalSnapshotError('FORMAL_HEALTH_RESPONSE_INVALID'))
    if fault=='partial-replay':transport.observe_http=lambda *a:(_ for _ in ()).throw(m.FormalSnapshotError('FORMAL_HEALTH_RESPONSE_INVALID'))
    if fault=='final-file-drift':
        original=transport.inspect
        def drift(connection,identity):
            row=original(connection,identity)
            if identity==plan[callers.ROLES[-1]]['containerId'] and any(e['step']=='maintenance-originals-bound' for e in claim.events):
                (claim.root/'shared-health-original.json').write_bytes(b'{}')
            return row
        transport.inspect=drift
    if fault=='late-journal':
        original=claim.record
        def slow(step,details):
            original(step,details)
            if step=='maintenance-originals-bound':clock[0]=121.
        claim.record=slow
    if fault=='final-flags-drift':
        original=claim.record
        def drift(step,details):
            original(step,details)
            if step=='maintenance-originals-bound':flags['readOnly']=0
        claim.record=drift
    if fault=='late-read':
        original=claim._read
        def slow(path):
            raw=original(path)
            if path.name=='shared-container-original.json' and any(e['step']=='external-health-scope' for e in claim.events):clock[0]=121.
            return raw
        claim._read=slow
    def guard():
        checks.append('request')
        if fault=='request':raise m.FormalSnapshotError('FORMAL_CAPTURE_REQUEST_CHANGED')
        if fault=='nested-request-mutation':
            value['sharedContainerIds']['mysql']='9'*64
            value['externalCallers']['test-backend']['containerId']='9'*64
    originals=copy.deepcopy(rows);external_originals=copy.deepcopy(transport.rows)
    try:
        with patch.object(m.snapshot,'acquire_claimed_database_fence') as fence, \
             patch.object(split,'stop_shared_container') as shared_stop, \
             patch.object(callers,'stop_external_caller') as stop, \
             patch.object(callers,'guard_external_callers') as stopped_guard:
            if fault in ('none','nested-request-mutation'):
                out=m.run_initialization_request(claim,value,guard)
                assert out['state']=='MAINTENANCE_ORIGINALS_BOUND_PENDING_EXCLUSION'
                assert not out['hostWritersVerified'] and not out['serviceActionsPerformed']
                assert not out['fullRunnerCompleted'] and not out['claimReleased'] and not out['productionEvidence']
                assert out['originalDatabaseFlags']==flags
                assert len(claim.events)==5 and claim.events[-1]['step']=='maintenance-originals-bound'
                for name,digest in out['originalFileSha256'].items():
                    assert hashlib.sha256(claim._read(claim.root/name)).hexdigest()==digest
                assert callers._original_health(claim)['health']['test-rehearsal']['state']=='ORIGINALLY_STOPPED'
            else:
                expected={'already-bound':'FORMAL_CAPTURE_INITIALIZATION_STAGE_INVALID',
                    'scope':'FORMAL_CAPTURE_INITIALIZATION_SCOPE_CHANGED','external-scope':'FORMAL_CAPTURE_INITIALIZATION_SCOPE_CHANGED',
                    'flags':'FORMAL_CAPTURE_INITIALIZATION_FLAGS_CHANGED','source':'FORMAL_CAPTURE_CONTROL_DEVICE_CHANGED',
                    'request':'FORMAL_CAPTURE_REQUEST_CHANGED','shared-health':'FORMAL_HEALTH_RESPONSE_INVALID',
                    'caller-health':'FORMAL_HEALTH_RESPONSE_INVALID','late-journal':'FORMAL_DATA_DEADLINE','late-read':'FORMAL_DATA_DEADLINE',
                    'final-flags-drift':'FORMAL_CAPTURE_INITIALIZATION_FLAGS_CHANGED',
                    'final-file-drift':'FORMAL_CAPTURE_INITIALIZATION_SCOPE_CHANGED','partial-replay':'FORMAL_HEALTH_RESPONSE_INVALID'}
                with pytest.raises(m.FormalSnapshotError,match='^'+expected[fault]+'$'):
                    m.run_initialization_request(claim,value,guard)
                if fault=='partial-replay':
                    retained={p.name:p.read_bytes() for p in claim.root.iterdir() if p.is_file()}
                    before=copy.deepcopy(claim.events)
                    with pytest.raises(m.FormalSnapshotError,match='^FORMAL_CAPTURE_INITIALIZATION_STAGE_INVALID$'):
                        m.run_initialization_request(claim,value,guard)
                    assert claim.events==before
                    assert {p.name:p.read_bytes() for p in claim.root.iterdir() if p.is_file()}==retained
            fence.assert_not_called();shared_stop.assert_not_called();stop.assert_not_called();stopped_guard.assert_not_called()
        assert rows==originals and transport.rows==external_originals and transport.actions==[]
        assert not any(e['step'].startswith(('db-','split-','ingress-')) or e['step'].endswith('-stop-intent') for e in claim.events)
        if fault in ('already-bound','scope','external-scope','flags','source','request'):assert not any(e['step']=='shared-health-scope' for e in claim.events)
    finally:
        for patcher in reversed(patchers):patcher.stop()


@pytest.mark.parametrize('fault',['same-device','credential-device','source-escape','native-binding','none'])
def test_original_device_boundary_uses_real_paths_and_explicit_device_observation(tmp_path,monkeypatch,fault):
    """Real private paths; only device identity/native observation are doubles.

    Windows cannot supply two approved ext4/control devices. These tests do
    not claim native mount or production device proof.
    """
    m=load();import formal_split_capture as split
    source_mount=tmp_path/'source';source=source_mount/'minio';source.mkdir(parents=True)
    control=tmp_path/'control';control.mkdir()
    credential=control/'mysql.secret';credential.write_bytes(b'private-device-sentinel\n')
    credential.chmod(0o600)
    value=selected(tmp_path)
    value.update(sourceRoot=str(source),claimRoot=str(control/'claim'),mysqlCredentialFile=str(credential))
    value['sourceMountBinding']['mount']['target']=str(source_mount)
    if fault=='source-escape':
        outside=tmp_path/'outside';outside.mkdir();value['sourceRoot']=str(outside)
    original_stat=type(source).stat
    def observed_stat(path,*args,**kwargs):
        record=original_stat(path,*args,**kwargs)
        fields=list(record)
        if path in (source_mount,source,tmp_path/'outside'):fields[2]=10
        elif path==control:fields[2]=10 if fault=='same-device' else 20
        elif path==credential:fields[2]=30 if fault=='credential-device' else 10 if fault=='same-device' else 20
        return os.stat_result(fields)
    monkeypatch.setattr(type(source),'stat',observed_stat)
    with patch.object(split,'_observe_native_environment',
        side_effect=m.FormalSnapshotError('FORMAL_SHARED_THAW_REQUIRED') if fault=='native-binding' else None) as native:
        if fault=='none':
            m.verify_initialization_devices(value)
            native.assert_called_once_with(value['sourceMountBinding'])
        else:
            code='FORMAL_SHARED_THAW_REQUIRED' if fault=='native-binding' else 'FORMAL_CAPTURE_CONTROL_DEVICE_CHANGED'
            with pytest.raises(m.FormalSnapshotError,match='^'+code+'$'):
                m.verify_initialization_devices(value)
            if fault!='native-binding':native.assert_not_called()
