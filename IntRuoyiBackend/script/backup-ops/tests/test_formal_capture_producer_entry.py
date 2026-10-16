"""Explicit orchestration doubles, private actual files; not live formal proof."""
import base64
import hashlib
import stat
import time
import importlib.util
import json
import os
from pathlib import Path
import subprocess
import sys
import types
import io
import tarfile
from unittest.mock import patch
import pytest

LINUX = Path(__file__).parents[1] / 'linux'
sys.path.insert(0, str(LINUX))


def load():
    spec = importlib.util.spec_from_file_location('capture_entry', LINUX / 'formal_capture_producer.py')
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


@pytest.mark.parametrize('case', ['pass', 'changed-request', 'wrong-operation', 'runtime', 'capture-failed'])
def test_private_request_dispatches_same_claim_and_preserves_failure(tmp_path, capsys, case):
    module = load()
    credential = tmp_path / 'mysql.secret'
    credential.write_bytes(b'private-entry-sentinel\n'); credential.chmod(0o600)
    binding = {'operationId':'owned-entry', 'sourceFingerprint':'a'*64,
               'databaseUuid':'0fa6c22b-5a90-11f1-ade1-0242ac150002', 'minioContainerId':'b'*64}
    fence = {'mount':'/owned/ext4', 'seconds':3, 'binding':{'operationId':'owned-entry', 'bootId':'fixture', 'mount':{}}}
    if case == 'wrong-operation':
        fence['binding']['operationId'] = 'other-entry'
    request = {'schemaVersion':'release-formal-capture-request/v1', 'claimRoot':str(tmp_path/'claim'),
               'claimBinding':binding, 'database':'ruoyi', 'mysqlContainerId':'c'*64,
               'mysqlCredentialFile':str(credential), 'sourceRoot':'/owned/ext4/minio', 'filesystemFence':fence}
    path = tmp_path / 'request.json';path.write_text(json.dumps(request));path.chmod(0o600)
    approved = hashlib.sha256(path.read_bytes()).hexdigest()
    if case == 'changed-request':path.write_text('{}')
    class Claim:
        def __init__(self, root, selected, *, resume):
            assert root==request['claimRoot'] and selected==binding and resume is True
        def close(self):pass
    expected={'state':'CAPTURED_PENDING_FULL_BARRIER_PROOF','hostWritersVerified':False}
    with patch.object(module,'require_native_runtime',side_effect=module.WatchdogError('WATCHDOG_RUNTIME_UNSUPPORTED') if case=='runtime' else None), \
         patch.object(module,'FormalMaintenanceClaim',side_effect=Claim) as claim, \
         patch.object(module,'PrivateDockerMySql') as mysql, \
         patch.object(module,'capture_claimed_formal_payload',side_effect=module.FormalSnapshotError('WATCHDOG_CAPTURE_INVALIDATED') if case=='capture-failed' else None,return_value=expected) as capture:
        result=module.main(['--request',str(path),'--expected-request-sha256',approved])
        out=capsys.readouterr().out
        assert 'private-entry-sentinel' not in out
        assert result==(0 if case=='pass' else 1)
        if case=='pass':
            assert json.loads(out)==expected
            assert capture.call_args.kwargs['filesystem_fence']==fence
        elif case=='runtime':assert json.loads(out)['code']=='WATCHDOG_RUNTIME_UNSUPPORTED'
        elif case=='capture-failed':assert json.loads(out)['code']=='WATCHDOG_CAPTURE_INVALIDATED'
        if case in ('changed-request','wrong-operation','runtime'):
            claim.assert_not_called();capture.assert_not_called()


def test_standard_entry_keeps_specific_runtime_failure_without_legacy_config(tmp_path):
    # Real CLI child, explicit capability double. No Docker or server execution.
    entry=LINUX/'backup_ops_linux.py'
    code='import sys,runpy,pathlib;entry=sys.argv.pop(1);sys.path.insert(0,str(pathlib.Path(entry).parent));sys.version_info=(3,6,8);runpy.run_path(entry,run_name="__main__")'
    result=subprocess.run([sys.executable,'-c',code,str(entry),'--mode','formal-capture',
                           '--request',str(tmp_path/'absent'),'--expected-request-sha256','a'*64],
                          capture_output=True,timeout=10)
    assert result.returncode==1
    assert json.loads(result.stdout)['code']=='WATCHDOG_RUNTIME_UNSUPPORTED'
    assert result.stderr==b''


@pytest.mark.parametrize('case',['pass','extra','bad-deadline','wrong-id','changed-request','failure','health-runtime'])
def test_v2_observation_is_explicit_bound_readonly_dispatch(tmp_path,capsys,case):
    module=load();import formal_service_health as health;credential=tmp_path/'mysql.secret'
    credential.write_bytes(b'private-v2-sentinel\n');credential.chmod(0o600)
    request={'schemaVersion':'release-formal-capture-request/v2','action':'observe-shared-health',
        'claimRoot':str(tmp_path/'claim'),
        'claimBinding':{'operationId':'owned-v2','sourceFingerprint':'a'*64,
            'databaseUuid':'00000000-0000-0000-0000-000000000001','minioContainerId':'b'*64},
        'mysqlContainerId':'c'*64,'mysqlCredentialFile':str(credential),'healthSeconds':10,
        'sharedScopeSha256':'d'*64,'originalHealthSha256':'e'*64,'ingressRulesSha256':'f'*64}
    if case=='extra':request['hostWritersVerified']=True
    if case=='bad-deadline':request['healthSeconds']=True
    if case=='wrong-id':request['mysqlContainerId']='mysql'
    path=tmp_path/'request.json';path.write_text(json.dumps(request));path.chmod(0o600)
    approved=hashlib.sha256(path.read_bytes()).hexdigest()
    if case=='changed-request':path.write_text('{}')
    expected={'state':'ORIGINAL_SERVICES_HEALTHY_INGRESS_HELD','productionEvidence':False,
              'hostWritersVerified':False,'serviceActionsPerformed':False,'claimReleased':False}
    class Claim:
        def __init__(self,root,selected,*,resume):
            assert selected==request['claimBinding'] and resume
            self.root=tmp_path
        def close(self):pass
    with patch.object(health,'NativeNetworkProbe',side_effect=module.FormalSnapshotError('FORMAL_HEALTH_NATIVE_RUNTIME_REQUIRED') if case=='health-runtime' else None), \
         patch.object(module,'require_native_runtime'),patch.object(module,'FormalMaintenanceClaim',side_effect=Claim) as claim, \
         patch.object(module,'observe_shared_request',return_value=expected,
            side_effect=module.FormalSnapshotError('FORMAL_HEALTH_NAMESPACE_OPEN_OS_ERROR_13') if case=='failure' else None) as observe, \
         patch.object(module,'capture_claimed_formal_payload') as capture:
        result=module.main(['--request',str(path),'--expected-request-sha256',approved])
        value=json.loads(capsys.readouterr().out)
        assert result==(0 if case=='pass' else 1)
        assert 'private-v2-sentinel' not in json.dumps(value)
        capture.assert_not_called()
        if case=='pass':assert value==expected;observe.assert_called_once()
        elif case=='failure':assert value['code']=='FORMAL_HEALTH_NAMESPACE_OPEN_OS_ERROR_13'
        elif case=='health-runtime':
            assert value['code']=='FORMAL_HEALTH_NATIVE_RUNTIME_REQUIRED';claim.assert_not_called();observe.assert_not_called()
        else:claim.assert_not_called();observe.assert_not_called()


def test_v2_observation_rejects_missing_thaw_before_any_shared_read(tmp_path,monkeypatch):
    module=load();import formal_split_capture as split;import formal_ingress_control as ingress
    calls=[]
    claim=type('Claim',(),{'assert_owned':lambda self:calls.append('claim')})()
    def fail(_):
        calls.append('thaw');raise module.FormalSnapshotError('FORMAL_SHARED_THAW_REQUIRED')
    monkeypatch.setattr(split,'_thaw_receipt',fail)
    monkeypatch.setattr(ingress,'assert_ingress',lambda *a,**k:calls.append('ingress'))
    monkeypatch.setattr(module,'PrivateDockerMySql',lambda *a,**k:calls.append('mysql'))
    with pytest.raises(module.FormalSnapshotError,match='FORMAL_SHARED_THAW_REQUIRED'):
        module.observe_shared_request(claim,{'healthSeconds':10},lambda:None)
    assert calls==['thaw']


def shared_request(tmp_path,action):
    value={'schemaVersion':'release-formal-capture-request/v3','action':action,
        'claimRoot':str(tmp_path/'claim'),
        'claimBinding':{'operationId':'owned-v3','sourceFingerprint':'a'*64,
            'databaseUuid':'00000000-0000-0000-0000-000000000001','minioContainerId':'b'*64},
        'mysqlContainerId':'c'*64,'mysqlCredentialFile':str(tmp_path/'mysql.secret'),
        'totalSeconds':120,'sharedScopeSha256':'d'*64,'originalHealthSha256':'e'*64,
        'ingressRulesSha256':'f'*64}
    if action=='restore-shared-dependencies':value['schemaVersion']='release-formal-capture-request/v7'
    if action=='capture-shared-payload':value.update(database='owned_probe',sourceRoot='/owned/source/minio',
        dataLimits={'max_rows':1000,'max_nodes':10000,'max_bytes':1024*1024,'seconds':30},
        rejectionProbe={'table':'probe','column':'id'},
        filesystemFence={'mount':'/owned/source','seconds':20,'binding':{'operationId':'owned-v3',
            'bootId':'00000000-0000-0000-0000-000000000001',
            'mount':{'source':'/dev/owned','device':'9:1','fstype':'ext4','target':'/owned/source'}}})
    return value


SHARED_REGISTERED_FILES = (
    'shared-container-original.json', 'shared-health-original.json', 'database-original.json',
    'external-callers-original.json', 'ingress-plan.json', 'ingress-installed.json',
    'ingress-recovery-plan.json', 'ingress-recovery-installed.json')


def registered_recovery_fixture(tmp_path, monkeypatch, *, fault=None, stage=None):
    """Actual private files and replacements; explicit Docker/nft/systemd protocol actors.

    Windows cannot run the POSIX secure reader: only that platform uses a named
    file protocol bridge. Linux runs the real reader under a private /control tree.
    This fixture cannot grant actual service recovery or complete exclusion proof.
    """
    m = load()
    if os.name == 'nt':
        # Import-only protocol boundary. No successful native lock operation
        # is simulated; Linux proof uses the actual fcntl and secure reader.
        unavailable_lock = types.ModuleType('fcntl')
        def refuse_native_lock(*args, **kwargs):
            raise AssertionError('Windows protocol fixture cannot perform a native POSIX lock')
        unavailable_lock.flock = refuse_native_lock
        monkeypatch.setitem(sys.modules, 'fcntl', unavailable_lock)
    import formal_boot_state as boot
    import formal_split_capture as split
    import formal_service_health as health
    import formal_ingress_control as ingress
    import formal_external_callers as callers
    import formal_host_admission as host
    root = tmp_path / 'claim'
    root.mkdir(mode=0o700)
    request = shared_request(tmp_path, 'restore-shared-dependencies')
    credential = Path(request['mysqlCredentialFile'])
    credential.write_bytes(b'owned-contract-secret\n')
    credential.chmod(0o600)
    originals = {}
    for name in SHARED_REGISTERED_FILES:
        raw = m.snapshot._canonical_bytes({'ownedOriginal': name})
        (root / name).write_bytes(raw)
        (root / name).chmod(0o600)
        originals[name] = raw
    sha = lambda raw: hashlib.sha256(raw).hexdigest()
    request['sharedScopeSha256'] = sha(originals['shared-container-original.json'])
    request['originalHealthSha256'] = sha(originals['shared-health-original.json'])
    request['ingressRulesSha256'] = sha(originals['ingress-recovery-installed.json'])
    events = [dict(step='external-caller-scope', details={
        'scopeSha256': sha(originals['external-callers-original.json'])})]
    events.append(dict(step='db-fenced', details={
        'originalSha256': sha(originals['database-original.json']), 'reconciled': False}))
    calls = []
    state = {'hostClosed': True, 'restored': False, 'faultApplied': False}

    class Claim:
        def __init__(self):
            self.root = root
            self.events = events
            self._plan = m.snapshot._canonical_bytes(request['claimBinding'])
        def assert_owned(self):
            calls.append('owner')
        def _read(self, path):
            return Path(path).read_bytes()
        def record(self, step, details):
            self.events.append(dict(step=step, details=details))
            calls.append(step)

    claim = Claim()

    def apply_fault():
        if state['faultApplied']:
            return
        state['faultApplied'] = True
        if fault == 'caller-scope-missing':
            events[:] = [e for e in events if e['step'] != 'external-caller-scope']
        elif fault == 'host-open':
            state['hostClosed'] = False
        elif fault and fault.startswith('missing:'):
            (root / fault.split(':', 1)[1]).unlink()
        elif fault and fault.startswith('replace:'):
            path = root / fault.split(':', 1)[1]
            # Keep the old inode alive: same bytes must not rebase the source.
            path.rename(path.with_suffix('.retained'))
            path.write_bytes(originals[path.name])
            path.chmod(0o600)

    class ClosedHostProtocol:
        def __init__(self, selected):
            assert selected is claim
            calls.append('host:init')
            check_host()
        def prepare(self, *, guard, deadline):
            guard()
            check_host()
            calls.append('host:prepare')
        def assert_native(self, *, deadline):
            check_host()
            calls.append('host:native')
            if stage == 'last-native':
                apply_fault()

    def check_host():
        if not state['hostClosed']:
            raise m.FormalSnapshotError('FORMAL_HOST_ADMISSION_CLOSED_ENTRY_CHANGED')

    if os.name == 'nt':
        def windows_private_file_protocol(path, mode, limit, *, no_atime):
            info = os.stat(path, follow_symlinks=False)
            raw = Path(path).read_bytes()
            assert len(raw) <= limit
            return raw, boot._signature(info)
        monkeypatch.setattr(boot, '_read_secure', windows_private_file_protocol)
    monkeypatch.setattr(host, 'ClosedHostAdmissionChecks', ClosedHostProtocol)
    monkeypatch.setattr(m, 'verify_shared_request_binding', lambda *args: dict(
        shared=originals['shared-container-original.json'], health=originals['shared-health-original.json']))
    monkeypatch.setattr(ingress, 'assert_ingress', lambda *args: dict(
        phase='RECOVERY_DEPENDENCIES_PERMITTED', rulesSha256=request['ingressRulesSha256']))

    def external_observer(*args):
        calls.append('callers:observe')
        value = dict(scopeSha256=sha(originals['external-callers-original.json']),
            state='KNOWN_EXTERNAL_CALLERS_STOPPED_PENDING_FULL_EXCLUSION', productionEvidence=False)
        if fault == 'wrong-caller-state':
            value['state'] = 'CALLERS_RUNNING'
        if fault == 'wrong-caller-sha':
            value['scopeSha256'] = '0' * 64
        return value

    monkeypatch.setattr(callers, 'guard_external_callers', external_observer)
    monkeypatch.setattr(m, 'PrivateDockerMySql', lambda *args, **kwargs: calls.append('mysql-client'))

    def thaw(_):
        calls.append('thaw')
        if state['restored'] and stage == 'final-thaw':
            apply_fault()
        return dict(nativeState='NOT_REQUESTED')
    monkeypatch.setattr(split, '_thaw_receipt', thaw)

    def restore_protocol(selected, mysql, *, guard, thaw_guard, timeout):
        assert selected is claim
        guard()
        claim.record('shared-recovery-intent', {'scopeSha256': request['sharedScopeSha256']})
        if stage == 'after-intent':
            apply_fault()
        guard()
        calls.append('start:mysql')
        if stage == 'after-mysql':
            apply_fault()
        guard()
        calls.append('start:redis')
        guard()
        claim.record('shared-dependencies-restored', {'healthSha256': 'a' * 64})
        if stage == 'after-receipt':
            apply_fault()
        state['restored'] = True
        return dict(state='DEPENDENCIES_HEALTHY_PENDING_ORIGINAL_OPEN', productionEvidence=False)
    monkeypatch.setattr(health, 'restore_shared_dependencies_with_health', restore_protocol)
    if stage == 'before':
        apply_fault()
    return m, claim, request, calls


@pytest.mark.parametrize('fault', ['caller-scope-missing', 'host-open', 'wrong-caller-state',
    'wrong-caller-sha'] + ['missing:' + name for name in SHARED_REGISTERED_FILES])
def test_registered_shared_recovery_missing_sources_refuse_before_intent(tmp_path, monkeypatch, fault):
    m, claim, request, calls = registered_recovery_fixture(tmp_path, monkeypatch, fault=fault, stage='before')
    with pytest.raises(m.FormalSnapshotError):
        m.run_shared_request(claim, request, lambda: None)
    assert 'shared-recovery-intent' not in calls
    assert 'mysql-client' not in calls
    assert not any(item.startswith('start:') for item in calls)


@pytest.mark.parametrize('name', SHARED_REGISTERED_FILES)
@pytest.mark.parametrize('stage', ['after-intent', 'after-mysql', 'after-receipt', 'last-native'])
def test_registered_shared_recovery_same_byte_replacement_never_rebases(tmp_path, monkeypatch, name, stage):
    m, claim, request, calls = registered_recovery_fixture(tmp_path, monkeypatch,
        fault='replace:' + name, stage=stage)
    with pytest.raises(m.FormalSnapshotError):
        m.run_shared_request(claim, request, lambda: None)
    if stage in ('after-intent', 'last-native'):
        assert not any(item.startswith('start:') for item in calls)
    elif stage == 'after-mysql':
        assert calls.count('start:mysql') == 1 and 'start:redis' not in calls
    else:
        assert calls.count('start:mysql') == calls.count('start:redis') == 1
        assert 'shared-dependencies-restored' in calls


@pytest.mark.parametrize('stage', ['after-intent', 'after-mysql', 'after-receipt', 'final-thaw'])
def test_registered_shared_recovery_host_reopens_refuse_next_action_or_receipt(tmp_path, monkeypatch, stage):
    m, claim, request, calls = registered_recovery_fixture(tmp_path, monkeypatch, fault='host-open', stage=stage)
    with pytest.raises(m.FormalSnapshotError):
        m.run_shared_request(claim, request, lambda: None)
    if stage == 'after-intent':
        assert not any(item.startswith('start:') for item in calls)
    elif stage == 'after-mysql':
        assert calls.count('start:mysql') == 1 and 'start:redis' not in calls
    else:
        assert calls.count('start:mysql') == calls.count('start:redis') == 1
        assert 'shared-dependencies-restored' in calls


def test_registered_shared_recovery_bounded_positive_remains_finite(tmp_path, monkeypatch):
    m, claim, request, calls = registered_recovery_fixture(tmp_path, monkeypatch)
    result = m.run_shared_request(claim, request, lambda: None)
    assert result['state'] == 'DEPENDENCIES_RESTORED_PENDING_OPEN_APPROVAL'
    assert result['fullRunnerCompleted'] is result['productionEvidence'] is result['claimReleased'] is False
    assert calls.count('host:init') == 1
    assert calls.count('start:mysql') == calls.count('start:redis') == 1


@pytest.mark.parametrize('fault',['target','extra','boot','device','fstype'])
def test_v3_invalid_native_binding_refuses_before_claim(tmp_path,capsys,fault):
    m=load();value=shared_request(tmp_path,'capture-shared-payload');binding=value['filesystemFence']['binding']
    if fault=='target':binding['mount']['target']='/owned/different-valid-mount'
    if fault=='extra':binding['unapproved']=True
    if fault=='boot':binding['bootId']='not-boot'
    if fault=='device':binding['mount']['device']='guess'
    if fault=='fstype':binding['mount']['fstype']='xfs'
    path=tmp_path/'request.json';path.write_bytes(json.dumps(value).encode());path.chmod(0o600)
    with patch.object(m,'require_native_runtime'),patch.object(m,'FormalMaintenanceClaim') as claim:
        assert m.main(['--request',str(path),'--expected-request-sha256',hashlib.sha256(path.read_bytes()).hexdigest()])==1
        assert json.loads(capsys.readouterr().out)['code']=='FORMAL_CAPTURE_REQUEST_INVALID'
        claim.assert_not_called()


@pytest.mark.parametrize('fault',['none','flags','runtime','transaction'])
def test_actual_pre_stop_barrier_has_no_stopped_event_requirement(tmp_path,monkeypatch,fault):
    from test_formal_split_capture import fixture,ROLES
    split,claim,mysql,source,rows,guard,guards,patchers=fixture(tmp_path)
    m=load();uuid=json.loads(claim._plan)['databaseUuid'];calls=[]
    monkeypatch.setattr(m.snapshot,'_claimed_database_original',lambda _:(uuid,{},'b'*64))
    monkeypatch.setattr(m.snapshot,'_database_flags',lambda *a:{'serverUuid':uuid,'readOnly':1,'superReadOnly':0 if fault=='flags' else 1})
    def barrier(*args):
        calls.append('live-transactions')
        if fault=='transaction':raise m.FormalSnapshotError('FORMAL_DATABASE_NOT_QUIESCENT')
    monkeypatch.setattr(m.snapshot,'observe_database_barrier',barrier)
    try:
        split.bind_shared_container_scope(claim,{role:str(i)*64 for i,role in enumerate(ROLES,1)})
        claim.record('db-fenced',{'originalSha256':'b'*64,'reconciled':False})
        if fault=='runtime':rows[mysql.container]['state']['StartedAt']='changed'
        before=list(claim.events)
        if fault=='none':m.verify_shared_database_before_stop(claim,mysql);assert calls==['live-transactions']
        else:
            with pytest.raises(m.FormalSnapshotError):m.verify_shared_database_before_stop(claim,mysql)
        assert claim.events==before and not any(e['step'].endswith('-stopped') for e in claim.events)
    finally:
        for patcher in reversed(patchers):patcher.stop()


@pytest.mark.parametrize('fault',['none','flags','health-roles','health-schema','health-claim','scope-id','db-stage','page-proof'])
def test_v3_original_binding_is_complete_before_mutation(tmp_path,monkeypatch,fault):
    from test_formal_split_capture import fixture,ROLES
    import formal_service_health as health
    split,claim,mysql,source,rows,guard,guards,patchers=fixture(tmp_path)
    m=load();request=shared_request(tmp_path,'restore-shared-services')
    request['claimBinding']=json.loads(claim._plan);request['mysqlContainerId']=mysql.container
    credential=Path(request['mysqlCredentialFile']);credential.write_bytes(b'owned-private\n');credential.chmod(0o600)
    flags={'serverUuid':request['claimBinding']['databaseUuid'],'readOnly':0,'superReadOnly':0}
    monkeypatch.setattr(m.snapshot,'_claimed_database_original',lambda _:(flags['serverUuid'],flags,'c'*64))
    try:
        originals=split.bind_shared_container_scope(claim,{role:str(i)*64 for i,role in enumerate(ROLES,1)})
        request['sharedScopeSha256']=__import__('hashlib').sha256(claim._read(claim.root/'shared-container-original.json')).hexdigest()
        report={'schemaVersion':'formal-shared-health-original/v1','claimSha256':hashlib.sha256(claim._plan).hexdigest(),
            'sharedScopeSha256':request['sharedScopeSha256'],'redisAuthentication':'none','observedAt':'2026-10-03T00:00:00+00:00',
            'health':{role:{'state':'HEALTHY','proof':{'protocol':'mysql','flags':dict(flags)} if role=='mysql' else
                {'protocol':'redis','authentication':'none','response':'PONG','networkNamespaceInode':12,'pid':99} if role=='redis' else
                {'httpStatus':200,'bytes':20,'sha256':'b'*64,'networkNamespaceInode':12,'pid':99}} for role in ROLES}}
        if fault=='flags':report['health']['mysql']['proof']['flags']['superReadOnly']=1
        if fault=='health-roles':del report['health']['redis']
        if fault=='health-schema':report['schemaVersion']='unexpected'
        if fault=='health-claim':report['claimSha256']='0'*64
        if fault=='page-proof':del report['health']['frontend']['proof']['sha256']
        if fault=='scope-id':request['mysqlContainerId']='f'*64
        healthraw=m.snapshot._canonical_bytes(report);claim._write(claim.root/'shared-health-original.json',healthraw)
        request['originalHealthSha256']=hashlib.sha256(healthraw).hexdigest()
        claim.record('shared-health-scope',{'sha256':request['originalHealthSha256']})
        if fault!='db-stage':claim.record('db-fenced',{'originalSha256':'c'*64,'reconciled':False})
        before=list(claim.events)
        if fault=='none':assert m.verify_shared_request_binding(claim,request)['health']==healthraw
        else:
            with pytest.raises(m.FormalSnapshotError):m.verify_shared_request_binding(claim,request)
        assert claim.events==before
    finally:
        for patcher in reversed(patchers):patcher.stop()


@pytest.mark.parametrize('action',['capture-shared-payload','restore-shared-dependencies'])
@pytest.mark.parametrize('fault',['none','extra','deadline','invalid-id','changed-request','phase-failure'])
def test_shared_capture_or_dependency_dispatch_is_explicit_and_retained(tmp_path,capsys,action,fault):
    m=load();import formal_service_health as health
    selected=shared_request(tmp_path,action)
    if fault=='extra':selected['hostWritersVerified']=True
    if fault=='deadline':selected['totalSeconds']=True
    if fault=='invalid-id':selected['mysqlContainerId']='mysql'
    path=tmp_path/'request.json';path.write_bytes(json.dumps(selected).encode());path.chmod(0o600)
    approved=hashlib.sha256(path.read_bytes()).hexdigest()
    if fault=='changed-request':path.write_bytes(b'{}')
    result={'state':'SHARED_PHASE_COMPLETE_INGRESS_HELD','hostWritersVerified':False,
            'productionEvidence':False,'claimReleased':False}
    class Claim:
        def __init__(self,*args,**kwargs):self.root=tmp_path
        def close(self):pass
    with patch.object(m,'require_native_runtime'),patch.object(health,'NativeNetworkProbe'), \
         patch.object(m,'FormalMaintenanceClaim',side_effect=Claim) as claimed, \
         patch.object(m,'run_shared_request',create=True,return_value=result,
            side_effect=m.FormalSnapshotError('FORMAL_SHARED_THAW_REQUIRED') if fault=='phase-failure' else None) as dispatch, \
         patch.object(m,'capture_claimed_formal_payload') as legacy:
        code=m.main(['--request',str(path),'--expected-request-sha256',approved])
        output=json.loads(capsys.readouterr().out)
        assert code==(0 if fault=='none' else 1)
        legacy.assert_not_called()
        if fault=='none':assert output==result;dispatch.assert_called_once()
        elif fault=='phase-failure':assert output['code']=='FORMAL_SHARED_THAW_REQUIRED'
        else:claimed.assert_not_called();dispatch.assert_not_called()


@pytest.mark.parametrize('action,controlled',[('capture-shared-payload',False),('restore-shared-dependencies',False),('capture-shared-payload',True)])
@pytest.mark.parametrize('fault',['none','thaw','scope','changed-request','stop','sql','raw','restore','late-deadline','wrong-ingress-phase','db-fence'])
def test_shared_phase_order_failure_and_no_implicit_release(tmp_path,monkeypatch,action,fault,controlled):
    m=load();import formal_split_capture as split;import formal_service_health as health;import formal_ingress_control as ingress
    request=shared_request(tmp_path,action);credential=Path(request['mysqlCredentialFile'])
    approved_source=None
    if controlled:
        approved_source=dict(environment='prod',database=request['database'],
            databaseUuid=request['claimBinding']['databaseUuid'],targetFingerprint=request['claimBinding']['sourceFingerprint'])
        request.update(schemaVersion='release-formal-capture-request/v12',sourceScope=approved_source)
    credential.write_bytes(b'private-v3-sentinel\n');credential.chmod(0o600)
    calls=[];clock=[0.]
    monkeypatch.setattr(m.time,'monotonic',lambda:clock[0])
    claim=type('Claim',(),{'root':tmp_path,'assert_owned':lambda self:None,'events':[],
        '_plan':m.snapshot._canonical_bytes(request['claimBinding'])})()
    def record(step,details):calls.append(step);claim.events.append({'step':step,'details':details})
    claim.record=record
    claim._read=lambda path:b'original-private-binding'
    def thaw(_):
        calls.append('thaw')
        if fault=='thaw':raise m.FormalSnapshotError('FORMAL_SHARED_THAW_REQUIRED')
        return {'nativeState':'NOT_REQUESTED'}
    monkeypatch.setattr(split,'_thaw_receipt',thaw)
    monkeypatch.setattr(m,'verify_shared_request_binding',lambda *args:
        (_ for _ in ()).throw(m.FormalSnapshotError('FORMAL_CAPTURE_SHARED_SCOPE_CHANGED')) if fault=='scope' else
        {'shared':b'original-private-binding','health':b'original-private-binding'},raising=False)
    correct_phase='CAPTURE_RESTRICTED' if action=='capture-shared-payload' else 'RECOVERY_DEPENDENCIES_PERMITTED'
    monkeypatch.setattr(ingress,'assert_ingress',lambda *a,**k:{'rulesSha256':'f'*64,
        'phase':'WRONG_PHASE' if fault=='wrong-ingress-phase' else correct_phase})
    class RegisteredSourcesPhaseProtocol:
        """Legacy order test boundary; dedicated tests above check real pins."""
        def __init__(self, selected, value):
            assert selected is claim and value is request
        def assert_valid(self, *, guard, deadline):
            guard()
            held = ingress.assert_ingress(claim)
            m.require(held['phase'] == 'RECOVERY_DEPENDENCIES_PERMITTED' and
                held['rulesSha256'] == request['ingressRulesSha256'], 'FORMAL_CAPTURE_INGRESS_CHANGED')
            m.require(claim._read(claim.root/'shared-container-original.json') == b'original-private-binding' and
                claim._read(claim.root/'shared-health-original.json') == b'original-private-binding',
                'FORMAL_CAPTURE_SHARED_SCOPE_CHANGED')
    monkeypatch.setattr(m, 'SharedDependencySourceChecks', RegisteredSourcesPhaseProtocol)
    def barrier(*args):
        calls.append('barrier')
        if fault=='db-fence':raise m.FormalSnapshotError('FORMAL_DATABASE_FENCE_ACQUIRE_FAILED')
    monkeypatch.setattr(m,'verify_shared_database_before_stop',barrier,raising=False)
    def completed(claim,*,expected_shared_scope_sha256,guard):
        guard();calls.append('controlled-completed')
        assert expected_shared_scope_sha256==request['sharedScopeSha256']
        return {'state':'CONTROLLED_PREFENCE_STOPS_OBSERVED'}
    monkeypatch.setattr(split,'assert_controlled_prefence_stops',completed,raising=False)
    monkeypatch.setattr(m,'PrivateDockerMySql',lambda *a,**k:type('Mysql',(),{'container':'c'*64})())
    def guard():
        calls.append('request')
        if fault=='changed-request':raise m.FormalSnapshotError('FORMAL_CAPTURE_REQUEST_CHANGED')
    def stop(claim,role,**kwargs):
        calls.append('stop:'+role)
        if fault=='stop':raise m.FormalSnapshotError('FORMAL_CONTAINER_ACTION_FAILED')
    def sql(*args,**kwargs):
        calls.append('sql')
        if fault=='sql':raise m.FormalSnapshotError('FORMAL_MYSQL_DUMP_FAILED')
        return dict(state='SQL_CAPTURED_PENDING_RAW_AND_FULL_BARRIERS',sourceScope=approved_source,sourceMigrationEvidence={})
    def raw(*args,**kwargs):
        calls.append('raw')
        if fault=='raw':raise m.FormalSnapshotError('FORMAL_SPLIT_NATIVE_NOT_VERIFIED')
        if fault=='late-deadline':clock[0]=121.
        return dict(state='CAPTURED_PENDING_FULL_BARRIER_PROOF',hostWritersVerified=False,sourceScope=approved_source,sourceMigrationEvidence={})
    def restore(*args,**kwargs):
        calls.append('restore');kwargs['thaw_guard']()
        if fault=='restore':raise m.FormalSnapshotError('FORMAL_HEALTH_RESPONSE_INVALID')
        if fault=='late-deadline':clock[0]=121.
        return {'state':'ORIGINAL_SERVICES_HEALTHY','productionEvidence':False}
    monkeypatch.setattr(split,'stop_shared_container',stop);monkeypatch.setattr(split,'capture_sql_phase',sql)
    protected=type('CompleteActionProtocol',(),{'assert_role':lambda *a,**k:None,'action':lambda *a,**k:None})()
    def protected_stop(claim,role,**kwargs):
        assert kwargs['container_actions'] is protected
        return stop(claim,role,**kwargs)
    monkeypatch.setattr(split,'stop_controlled_capture_database',protected_stop,raising=False)
    monkeypatch.setattr(split,'capture_raw_phase',raw);monkeypatch.setattr(health,'restore_shared_dependencies_with_health',restore)
    monkeypatch.setattr(ingress,'release_ingress',lambda *a,**k:(_ for _ in ()).throw(AssertionError('implicit release')))
    invalid=fault in ['thaw','scope','changed-request','late-deadline','wrong-ingress-phase'] or action=='capture-shared-payload' and fault in ['stop','sql','raw','db-fence'] or action=='restore-shared-dependencies' and fault=='restore'
    runner=m.run_controlled_shared_capture_request if controlled else m.run_shared_request
    extra={'approved_source':approved_source,'container_actions':protected} if controlled else {}
    if invalid:
        with pytest.raises(m.FormalSnapshotError):runner(claim,request,guard,native_guard=claim.assert_owned,**extra)
    else:
        result=runner(claim,request,guard,native_guard=claim.assert_owned,**extra)
        assert result['productionEvidence'] is False and result['hostWritersVerified'] is False and result['claimReleased'] is False
    if fault=='thaw':assert calls==['thaw']
    if fault=='db-fence' and action=='capture-shared-payload':assert not any(x.startswith('stop:') for x in calls)
    if fault=='wrong-ingress-phase':assert not any(x.startswith('stop:') or x=='restore' for x in calls)
    if action=='capture-shared-payload' and not invalid:
        ordered=[x for x in calls if x.startswith('stop:') or x in ['sql','raw','restore']]
        assert ordered==([] if controlled else ['stop:'+role for role in split.STOP_ORDER[:-1]])+['sql','stop:mysql','raw']
    if action=='restore-shared-dependencies':
        assert not any(x.startswith('stop:') or x in ['sql','raw'] for x in calls)
        if not invalid:assert result['state']=='DEPENDENCIES_RESTORED_PENDING_OPEN_APPROVAL'

def original_image_module(monkeypatch):
    if os.name=='nt' and 'fcntl' not in sys.modules:
        boundary=types.ModuleType('fcntl')
        boundary.flock=lambda *args:(_ for _ in ()).throw(AssertionError('no native flock in image protocol'))
        monkeypatch.setitem(sys.modules,'fcntl',boundary)
    import formal_original_launch_capture as module
    return module


def production_a_source_inputs(module):
    files={name:{'path':'/approved/originals/'+name,'sha256':hashlib.sha256(name.encode()).hexdigest()}
        for name in module.PRODUCTION_A_SOURCE_FILES}
    inventory=dict(schemaVersion='release-formal-production-a-original-sources/v1',
        claimBindingSha256='1'*64,approvedSourceTargetSha256='2'*64,
        hostBootId='0fa6c22b-5a90-11f1-ade1-0242ac150002',files=files)
    release=dict(schemaVersion='release-formal-original-package-binding/v2',
        claimBindingSha256='1'*64,approvedSourceTargetSha256='2'*64,deploymentAttemptId='3'*64,
        leaseTokenDigest='4'*64,scope={'environment':'prod'},
        package=dict(releaseTag='approved-release',packageDigest='5'*64,manifestDigest='6'*64,planDigest='7'*64))
    return release,inventory


@pytest.mark.parametrize('fault',['none','missing-boot','invalid-boot','missing-source','wrong-claim','existing-release',
    'existing-inspect','prepare-failure','fd-replaced','source-inode','receipt-drift','deadline','retained-budget'])
def test_production_a_normal_preparation_keeps_original_owner_epoch(tmp_path,monkeypatch,fault):
    """Finite orchestration contract, not native FD/SQL/daemon/approval proof.

    Existing native sealers are explicit protocol actors in this Windows test.
    Real file bytes/inode replacements and the pure inventory binding run here.
    """
    m=load()
    if os.name=='nt' and 'fcntl' not in sys.modules:
        unavailable=types.ModuleType('fcntl')
        unavailable.flock=lambda *a:(_ for _ in ()).throw(AssertionError('no native flock in preparation protocol'))
        monkeypatch.setitem(sys.modules,'fcntl',unavailable)
    import formal_boot_state as boot
    import formal_controlled_container_sources as controlled
    root=tmp_path/'normal-claim';root.mkdir();calls=[]
    binding=dict(operationId='prod-approved-release',sourceFingerprint='a'*64,
        databaseUuid='00000000-0000-0000-0000-000000000001',minioContainerId='b'*64)
    class Claim:
        def __init__(self):
            self.root=root;self._plan=m.snapshot._canonical_bytes(binding);self._fd=47
            self._lock=root/'ownership.lock';self.events=[]
        def assert_owned(self):assert self._fd is not None
        def _read(self,path):return Path(path).read_bytes()
        def _write(self,path,raw):
            with Path(path).open('xb') as out:out.write(raw)
    claim=Claim();(root/'binding.json').write_bytes(claim._plan);claim._lock.write_bytes(b'')
    source_names=m.PRODUCTION_A_SOURCE_FILES-{'database-prefence-original.json'}-{
        'boot-'+r+'-original.json' for r in ('backend','test-backend','test-rehearsal','backup-backend','backup-rehearsal')}-{
        'controlled-'+r+'-inspect.json' for r in controlled.ControlledSharedContainerActions.ROLES}
    refs={}
    for name in sorted(source_names):
        path=root/name
        if name!='binding.json':path.write_bytes(m.snapshot._canonical_bytes(dict(explicitOriginalProtocol=name)))
        refs[name]=dict(path=str(path),sha256=hashlib.sha256(path.read_bytes()).hexdigest())
    release,_=production_a_source_inputs(m)
    release['claimBindingSha256']=hashlib.sha256(claim._plan).hexdigest()
    release['approvedSourceTargetSha256']=refs['approved-source-target.json']['sha256']
    release['productionEvidence']=False
    release_raw=m.snapshot._canonical_bytes(release)
    roles=('backend','test-backend','test-rehearsal','backup-backend','backup-rehearsal')
    boot_sources={role:dict(schemaVersion='release-formal-production-a-boot-source/v1',role=role,
        peer='local',machineIdSha256='c'*64,bootId=binding['databaseUuid'],path='/approved/'+role+'/boot.json') for role in roles}
    selections={role:dict(present=True,containerId=hashlib.sha256(role.encode()).hexdigest(),
        imageId='sha256:'+'d'*64,name='/'+role) for role in controlled.ControlledSharedContainerActions.ROLES}
    mysql=object();deadline=m.time.monotonic()+30;transport=object()
    if fault=='missing-boot':del boot_sources['backend']
    if fault=='invalid-boot':boot_sources['backend']=None
    if fault=='missing-source':del refs['host-originals.json']
    if fault=='existing-release':(root/'original-release-binding.json').write_bytes(b'RETAIN_EXISTING')
    if fault=='existing-inspect':(root/'controlled-backend-inspect.json').write_bytes(b'RETAIN_EXISTING')
    if fault=='deadline':deadline=m.time.monotonic()-1
    monkeypatch.setattr(m,'FormalMaintenanceClaim',Claim)
    monkeypatch.setattr(m,'require_native_runtime',lambda:None)
    monkeypatch.setattr(m.os,'geteuid',lambda:0,raising=False)
    def secure(path,mode,limit,*,no_atime):
        raw=Path(path).read_bytes();assert len(raw)<=limit
        return raw,boot._signature(Path(path).stat())
    monkeypatch.setattr(boot,'_read_secure',secure)
    actual_fstat=m.os.fstat
    monkeypatch.setattr(m.os,'fstat',lambda fd:claim._lock.stat() if fd in (47,48) else actual_fstat(fd))
    def guard():
        claim.assert_owned();calls.append('guard')
        if fault=='retained-budget':assert m.snapshot.observation_deadline.get()==deadline
    def seal_boot(owner,role,source,*,guard,deadline,connection=None):
        assert owner is claim and claim._fd==47 and connection is None
        guard();calls.append('boot:'+role)
        if fault=='prepare-failure':raise m.FormalSnapshotError('EXPLICIT_BOOT_SOURCE_REFUSAL')
        path=root/('boot-'+role+'-original.json');claim._write(path,m.snapshot._canonical_bytes(source))
        if fault=='fd-replaced':claim._fd=48
        return dict(path=str(path),sha256=hashlib.sha256(path.read_bytes()).hexdigest())
    def seal_inspect(path,selected,*,guard,deadline,transport):
        assert transport is globals_transport;guard();calls.append('inspect:'+selected['name'])
        claim._write(path,m.snapshot._canonical_bytes(selected))
        return dict(path=str(path),source=selected)
    globals_transport=transport
    def prefence(owner,observer,*,guard):
        assert owner is claim and observer is mysql;guard();calls.append('prefence')
        raw=m.snapshot._canonical_bytes(dict(serverUuid=binding['databaseUuid'],readOnly=0,superReadOnly=0))
        path=root/'database-prefence-original.json';claim._write(path,raw)
        return dict(path=str(path),sha256=hashlib.sha256(raw).hexdigest())
    def inventory(owner,selected_release,paths,mappings,*,guard,deadline):
        assert owner is claim and claim._fd==47 and selected_release==release_raw
        assert set(paths)==m.PRODUCTION_A_SOURCE_FILES and mappings==boot_sources
        guard();calls.append('inventory')
        value=dict(schemaVersion='release-formal-production-a-original-sources/v1',
            claimBindingSha256=release['claimBindingSha256'],approvedSourceTargetSha256=release['approvedSourceTargetSha256'],
            hostBootId=binding['databaseUuid'],files={name:dict(path='/protocol/'+name,
                sha256=hashlib.sha256(Path(path).read_bytes()).hexdigest()) for name,path in paths.items()})
        raw=m.snapshot._canonical_bytes(value);path=root/'production-a-source-inventory.json';claim._write(path,raw)
        expected=m.production_a_resume_binding(selected_release,raw,hashlib.sha256(raw).hexdigest())
        return dict(path=str(path),sha256=hashlib.sha256(raw).hexdigest(),binding=expected)
    monkeypatch.setattr(m,'seal_production_a_boot_original',seal_boot)
    monkeypatch.setattr(controlled,'capture_controlled_container_source',seal_inspect)
    monkeypatch.setattr(m,'seal_production_a_database_prefence_original',prefence)
    monkeypatch.setattr(m,'seal_production_a_source_inventory',inventory)
    def execute():
        return m.prepare_production_a_normal_sources(object() if fault=='wrong-claim' else claim,
            release_binding_raw=release_raw,source_refs=refs,boot_sources=boot_sources,
            controlled_sources=selections,mysql=mysql,guard=guard,deadline=deadline,
            boot_connections={},docker_sources=transport)
    if fault in ('none','source-inode','receipt-drift','retained-budget'):
        prepared,retained=execute();retained()
        assert prepared['state']=='A_ORIGINAL_SOURCES_PREPARED_PENDING_INDEPENDENT_AUTHORITY'
        assert prepared['inventory']['binding']['sourceBindingSha256']==prepared['inventory']['sha256']
        assert prepared['deadline']==deadline and claim._fd==47 and len(prepared['controlledOriginals'])==7
        assert len([c for c in calls if c.startswith('boot:')])==5
        assert len([c for c in calls if c.startswith('inspect:')])==7
        assert calls.index('prefence')<calls.index('inventory')
        assert not (root/'database-original.json').exists() and claim.events==[]
        assert all(prepared[k] is False for k in ('approvalConsumed','productionEvidence','claimReleased','releaseReady'))
        if fault=='source-inode':
            path=Path(refs['host-originals.json']['path']);raw=path.read_bytes();path.rename(root/'retained-old-host');path.write_bytes(raw)
        if fault=='receipt-drift':prepared['inventory']['binding']['attemptId']='9'*64
        if fault in ('source-inode','receipt-drift'):
            with pytest.raises(m.FormalSnapshotError,match='FORMAL_PRODUCTION_A_NORMAL_PREPARATION_CHANGED'):retained()
    else:
        with pytest.raises(m.FormalSnapshotError):execute()
        assert 'inventory' not in calls and claim._fd is not None
        if fault=='existing-release':assert (root/'original-release-binding.json').read_bytes()==b'RETAIN_EXISTING'
        if fault=='existing-inspect':assert (root/'controlled-backend-inspect.json').read_bytes()==b'RETAIN_EXISTING'


def test_production_a_normal_preparation_unsupported_host_refuses_before_inputs():
    m=load()
    with pytest.raises(m.WatchdogError,match='WATCHDOG_RUNTIME_UNSUPPORTED'):
        m.prepare_production_a_normal_sources(None,release_binding_raw=None,source_refs=None,
            boot_sources=None,controlled_sources=None,mysql=None,guard=None,deadline=1)


def test_production_a_inventory_uses_pre_fence_database_original():
    m=load()
    assert 'database-original.json' not in m.PRODUCTION_A_SOURCE_FILES
    assert 'database-prefence-original.json' in m.PRODUCTION_A_SOURCE_FILES
    release,inventory=production_a_source_inputs(m)
    release_raw=m.snapshot._canonical_bytes(release)
    source_raw=m.snapshot._canonical_bytes(inventory)
    result=m.production_a_resume_binding(release_raw,source_raw,hashlib.sha256(source_raw).hexdigest())
    assert result['sourceBindingSha256']==hashlib.sha256(source_raw).hexdigest()


@pytest.mark.parametrize('fault',['none','existing-prefence','existing-database-original','changed-flags','guard'])
def test_production_a_prefence_database_original_is_sealed_before_database_fence(tmp_path,monkeypatch,fault):
    m=load()
    if os.name=='nt' and 'fcntl' not in sys.modules:
        unavailable=types.ModuleType('fcntl')
        unavailable.flock=lambda *args:(_ for _ in ()).throw(AssertionError('native flock is outside this protocol test'))
        monkeypatch.setitem(sys.modules,'fcntl',unavailable)
    import formal_boot_state as boot
    root=tmp_path/'claim';root.mkdir()
    binding={'operationId':'owned-prefence','sourceFingerprint':'a'*64,
        'databaseUuid':'0fa6c22b-5a90-11f1-ade1-0242ac150002','minioContainerId':'b'*64}
    class Claim:
        def __init__(self):self.root=root;self._plan=m.snapshot._canonical_bytes(binding);self.events=[]
        def assert_owned(self):pass
        def _write(self,path,raw):
            with Path(path).open('xb') as output:output.write(raw)
            os.chmod(path,0o600)
    claim=Claim();prefence=root/'database-prefence-original.json';postfence=root/'database-original.json'
    if fault=='existing-prefence':prefence.write_bytes(b'RETAIN_EXISTING')
    if fault=='existing-database-original':postfence.write_bytes(b'RETAIN_EXISTING')
    flags={'serverUuid':binding['databaseUuid'],'readOnly':0,'superReadOnly':0}
    changed=dict(flags,readOnly=1);samples=[flags,changed] if fault=='changed-flags' else [flags,flags]
    reads=[]
    def database_flags(mysql,identity):
        reads.append((mysql,identity))
        return samples[min(len(reads)-1,len(samples)-1)]
    monkeypatch.setattr(m,'FormalMaintenanceClaim',Claim)
    monkeypatch.setattr(m,'require_native_runtime',lambda:None)
    monkeypatch.setattr(m.snapshot,'_database_flags',database_flags)
    def secure(path,mode,limit,*,no_atime):
        selected=Path(path);raw=selected.read_bytes()
        return raw,boot._signature(os.stat(selected,follow_symlinks=False))
    monkeypatch.setattr(boot,'_read_secure',secure)
    guard_calls=[]
    def guard():
        guard_calls.append(True)
        if fault=='guard':raise m.FormalSnapshotError('TASK_TEST_PREFENCE_GUARD_FAILED')
    if fault=='none':
        result=m.seal_production_a_database_prefence_original(claim,object(),guard=guard)
        raw=m.snapshot._canonical_bytes(flags)
        assert prefence.read_bytes()==raw and not postfence.exists()
        assert result['path']==str(prefence) and result['sha256']==hashlib.sha256(raw).hexdigest()
        assert result['flags']==flags and len(result['identity'])==9
        assert len(reads)==2 and reads[0][1]==reads[1][1]==binding['databaseUuid']
        assert claim.events==[]
    else:
        with pytest.raises(m.FormalSnapshotError):
            m.seal_production_a_database_prefence_original(claim,object(),guard=guard)
        assert not prefence.exists() if fault!='existing-prefence' else prefence.read_bytes()==b'RETAIN_EXISTING'
        assert not postfence.exists() if fault!='existing-database-original' else postfence.read_bytes()==b'RETAIN_EXISTING'
        assert len(reads)==(2 if fault=='changed-flags' else 0)


@pytest.mark.parametrize('fault',['none','absent','preseal-drift','postseal-drift','existing-target','guard','wrong-role','remote-peer','wrong-source-record'])
def test_production_a_boot_original_is_readonly_and_retained(tmp_path,monkeypatch,fault):
    m=load()
    if os.name=='nt' and 'fcntl' not in sys.modules:
        unavailable=types.ModuleType('fcntl')
        unavailable.flock=lambda *args:(_ for _ in ()).throw(AssertionError('native flock is outside this protocol test'))
        monkeypatch.setitem(sys.modules,'fcntl',unavailable)
    import formal_boot_state as boot
    root=tmp_path/'claim';root.mkdir()
    class Claim:
        def __init__(self):self.root=root;self.events=[];self.device=os.stat(root).st_dev
        def assert_owned(self):pass
        def _write(self,path,raw):
            with Path(path).open('xb') as output:output.write(raw)
            os.chmod(path,0o600)
    claim=Claim();path=root/'boot-backend-original.json'
    if fault=='existing-target':path.write_bytes(b'RETAIN_EXISTING');path.chmod(0o600)
    source=dict(schemaVersion='release-formal-production-a-boot-source/v1',role='backend',peer='local',
        machineIdSha256='a'*64,bootId='00000000-0000-0000-0000-000000000001',path='/approved/backend/boot.json')
    if fault=='wrong-role':source['role']='other'
    if fault=='remote-peer':source['peer']='172.30.30.58'
    raw=b'{"state":"OPEN","epoch":""}\n'
    record=dict(schemaVersion='release-formal-production-a-boot-original/v1',role='backend',peer='local',
        machineIdSha256=source['machineIdSha256'],bootId=source['bootId'],path=source['path'],
        sourceParentIdentity=[[1,2,0,0,stat.S_IFDIR|0o755]],present=True,
        originalBase64=base64.b64encode(raw).decode('ascii'),sha256=hashlib.sha256(raw).hexdigest(),
        sourceFileIdentity=[1,3,0,0,stat.S_IFREG|0o644,1,len(raw),4,5])
    absent=dict(record,present=False,originalBase64=None,sha256=None,sourceFileIdentity=None)
    saved=absent if fault=='absent' else record
    samples=[saved,saved,saved]
    if fault=='wrong-source-record':samples=[dict(record,path='/other/boot.json')]*3
    changed=b'changed'
    if fault=='preseal-drift':samples[1]=dict(record,originalBase64=base64.b64encode(changed).decode(),sha256=hashlib.sha256(changed).hexdigest(),sourceFileIdentity=[1,3,0,0,stat.S_IFREG|0o644,1,len(changed),4,5])
    if fault=='postseal-drift':samples[2]=dict(record,originalBase64=base64.b64encode(changed).decode(),sha256=hashlib.sha256(changed).hexdigest(),sourceFileIdentity=[1,3,0,0,stat.S_IFREG|0o644,1,len(changed),4,5])
    reads=[];guard_calls=[]
    def sample(selected,*,guard,deadline):
        reads.append(selected)
        return samples[min(len(reads)-1,len(samples)-1)]
    monkeypatch.setattr(m,'require_native_runtime',lambda:None)
    monkeypatch.setattr(m,'FormalMaintenanceClaim',Claim)
    monkeypatch.setattr(m,'_sample_production_a_boot_original_source',sample,raising=False)
    monkeypatch.setattr(m.snapshot,'observation_timeout',lambda seconds:seconds)
    monkeypatch.setattr(boot,'_read_secure',lambda selected,mode,limit,*,no_atime:
        (Path(selected).read_bytes(),boot._signature(os.stat(selected,follow_symlinks=False))))
    def guard():
        guard_calls.append(True)
        if fault=='guard':raise m.FormalSnapshotError('TASK_TEST_BOOT_GUARD_FAILED')
    deadline=time.monotonic()+10
    if fault in ('none','absent'):
        result=m.seal_production_a_boot_original(claim,'backend',source,guard=guard,deadline=deadline)
        assert result['path']==str(path) and result['sha256']==hashlib.sha256(path.read_bytes()).hexdigest()
        assert result['identity'][0]==claim.device and claim.events==[]
        sealed=json.loads(path.read_bytes())
        assert sealed==saved and sealed['present'] is (fault=='none')
        assert len(reads)==3 and len(guard_calls)>=1
    else:
        with pytest.raises(m.FormalSnapshotError):
            m.seal_production_a_boot_original(claim,'backend',source,guard=guard,deadline=deadline)
        assert path.exists()==(fault in ('existing-target','postseal-drift'))
        if fault=='existing-target':assert path.read_bytes()==b'RETAIN_EXISTING'
        if fault=='postseal-drift':assert json.loads(path.read_bytes())==record
        assert claim.events==[]


def test_production_a_boot_original_actual_unsupported_runtime_refuses_before_reads():
    if sys.platform=='linux':pytest.skip('Requires actual Windows runtime')
    m=load()
    for call in (lambda:m.seal_production_a_boot_original(None,'backend',None,guard=None,deadline=1),
                 lambda:m._sample_production_a_boot_original_source(None,guard=None,deadline=1)):
        with pytest.raises(m.WatchdogError,match='WATCHDOG_RUNTIME_UNSUPPORTED'):call()


def test_production_a_boot_original_record_rejects_ambiguous_absence():
    m=load();base=dict(schemaVersion='release-formal-production-a-boot-original/v1',role='backend',peer='local',
        machineIdSha256='a'*64,bootId='00000000-0000-0000-0000-000000000001',path='/approved/backend/boot.json',
        sourceParentIdentity=[[1,2,0,0,stat.S_IFDIR|0o755]],present=False,
        originalBase64=None,sha256=None,sourceFileIdentity=None)
    assert m._validate_production_a_boot_original(base,'backend') is base
    for change in (dict(path=None),dict(sourceParentIdentity=[]),dict(originalBase64=''),
                   dict(sha256='a'*64),dict(sourceFileIdentity=[1])):
        with pytest.raises(m.FormalSnapshotError):m._validate_production_a_boot_original(dict(base,**change),'backend')


@pytest.mark.parametrize('fault',['none','actual-bytes','intent-flags','intent-sha','intent-types','wrong-uuid',
                                   'prefence-noncanonical','actual-noncanonical'])
def test_production_a_database_original_must_match_sealed_prefence_bytes(fault):
    m=load();identity='0fa6c22b-5a90-11f1-ade1-0242ac150002'
    flags={'serverUuid':identity,'readOnly':0,'superReadOnly':0}
    prefence=m.snapshot._canonical_bytes(flags);actual=prefence
    intent={'originalFlags':flags,'originalSha256':hashlib.sha256(prefence).hexdigest()}
    if fault=='actual-bytes':
        actual=m.snapshot._canonical_bytes(dict(flags,readOnly=1))
    elif fault=='intent-flags':
        intent['originalFlags']=dict(flags,readOnly=1)
    elif fault=='intent-sha':
        intent['originalSha256']='a'*64
    elif fault=='intent-types':
        intent['originalFlags']=dict(flags,readOnly=False)
    elif fault=='wrong-uuid':
        identity='00000000-0000-0000-0000-000000000001'
    elif fault=='prefence-noncanonical':
        prefence=b'{"serverUuid":"'+flags['serverUuid'].encode()+b'","readOnly":0,"superReadOnly":0}\n'
    elif fault=='actual-noncanonical':
        actual=prefence+b'\n'
    if fault=='none':
        assert m.verify_production_a_database_fence_original(prefence,actual,intent,identity)==flags
    else:
        with pytest.raises(m.FormalSnapshotError):
            m.verify_production_a_database_fence_original(prefence,actual,intent,identity)


@pytest.mark.parametrize('fault',['none','inventory-sha','missing-host','missing-docker','missing-boot',
    'missing-config','missing-prefence','extra','duplicate-path','path','digest','claim-drift','target-drift','test','lease-lf'])
def test_production_a_resume_binding_uses_whole_approved_inventory(fault):
    m=load();release,inventory=production_a_source_inputs(m)
    if fault=='missing-host':inventory['files'].pop('host-derived-originals.json')
    if fault=='missing-docker':inventory['files'].pop('controlled-backend-inspect.json')
    if fault=='missing-boot':inventory['files'].pop('boot-backend-original.json')
    if fault=='missing-config':inventory['files'].pop('original-configs.json')
    if fault=='missing-prefence':inventory['files'].pop('database-prefence-original.json')
    if fault=='extra':inventory['files']['other.json']=dict(path='/approved/other',sha256='a'*64)
    if fault=='duplicate-path':inventory['files']['host-originals.json']['path']=inventory['files']['binding.json']['path']
    if fault=='path':inventory['files']['binding.json']['path']='/approved/../binding.json'
    if fault=='digest':inventory['files']['binding.json']['sha256']='a'*64+'\n'
    if fault=='claim-drift':inventory['claimBindingSha256']='8'*64
    if fault=='target-drift':inventory['approvedSourceTargetSha256']='8'*64
    if fault=='test':release['scope']['environment']='test'
    if fault=='lease-lf':release['leaseTokenDigest']+='\n'
    release_raw=m.snapshot._canonical_bytes(release);source_raw=m.snapshot._canonical_bytes(inventory)
    pin=hashlib.sha256(source_raw).hexdigest() if fault!='inventory-sha' else '9'*64
    if fault=='none':
        result=m.production_a_resume_binding(release_raw,source_raw,pin)
        assert result==dict(environment='prod',operationId='prod-approved-release',releaseTag='approved-release',
            attemptId='3'*64,leaseTokenDigest='4'*64,packageDigest='5'*64,manifestDigest='6'*64,planDigest='7'*64,
            approvedSourceTargetSha256='2'*64,sourceBindingSha256=pin,hostBootId=inventory['hostBootId'])
    else:
        with pytest.raises(m.FormalSnapshotError):m.production_a_resume_binding(release_raw,source_raw,pin)


def test_production_a_resume_owner_refuses_actual_unsupported_runtime_before_reads(tmp_path):
    if sys.platform=='linux':pytest.skip('Unsupported-runtime check requires the actual non-Linux host')
    m=load()
    with pytest.raises(m.WatchdogError,match='WATCHDOG_RUNTIME_UNSUPPORTED'):
        m.ProductionAResumeChecks(None,b'',b'','a'*64,{},guard=None,thaw_guard=None)
    with pytest.raises(m.WatchdogError,match='WATCHDOG_RUNTIME_UNSUPPORTED'):
        m.ProductionAResumeChecks.control_phase_guard(None,maintenance_end=1,deadline=1)
    assert not list(tmp_path.iterdir())

def original_image_archive(path,fault=None):
    files={};entries=[];ids={}
    for index,role in enumerate(('backend','frontend')):
        layer=(b'OLD_ACTUAL_LAYER_'+role.encode())*(index+1)
        config=json.dumps(dict(os='linux',architecture='amd64',
            rootfs=dict(type='layers',diff_ids=['sha256:'+hashlib.sha256(layer).hexdigest()])),
            sort_keys=True).encode()
        image='sha256:'+hashlib.sha256(config).hexdigest();ids[role]=image
        name=image[7:]+'.json';layer_path=role+'/layer.tar'
        files[name]=config;files[layer_path]=layer
        entries.append(dict(Config=name,RepoTags=None,Layers=[layer_path]))
    if fault=='config':files[entries[0]['Config']]+=b' '
    if fault=='layer':files[entries[0]['Layers'][0]]=b'WRONG_LAYER'
    if fault=='missing-layer':files.pop(entries[0]['Layers'][0])
    if fault=='wrong-image':ids['backend']='sha256:'+'f'*64
    if fault=='extra-image':entries.append(entries[0])
    files['manifest.json']=json.dumps(entries).encode()
    with tarfile.open(path,'w') as archive:
        for name,content in files.items():
            item=tarfile.TarInfo(name);item.size=len(content)
            archive.addfile(item,io.BytesIO(content))
        if fault=='duplicate':
            item=tarfile.TarInfo('manifest.json');item.size=len(files['manifest.json'])
            archive.addfile(item,io.BytesIO(files['manifest.json']))
        if fault=='unsafe-path':
            item=tarfile.TarInfo('../outside');item.size=1;archive.addfile(item,io.BytesIO(b'X'))
    return ids

@pytest.mark.parametrize('fault',[None,'config','layer','missing-layer','wrong-image','extra-image',
                                  'duplicate','unsafe-path','source-drift','lost-guard','existing','export-fail',
                                  'running-guard-lost','stderr','archive-drift'])
def test_original_image_export_uses_exact_ids_and_rechecks_actual_archive(tmp_path,monkeypatch,fault):
    module=original_image_module(monkeypatch);fixture=tmp_path/'docker-protocol.tar'
    ids=original_image_archive(fixture,fault);target=tmp_path/'original-images.tar';calls=[];children=[]
    roles={role:dict(id=(str(i+1)*64),image=image) for i,(role,image) in enumerate(ids.items())}
    class Claim:
        def __init__(self):self.root=tmp_path;self.events=[]
        def assert_owned(self):calls.append('claim')
        def _read(self,path):return path.read_bytes()
        def record(self,*args):calls.append(('event',args))
    claim=Claim();raw=module.snapshot._canonical_bytes(roles)
    (tmp_path/'shared-container-original.json').write_bytes(raw)
    def scope(selected,*,stopped):
        assert selected is claim and stopped is True
        return roles,roles,hashlib.sha256(raw).hexdigest()
    monkeypatch.setattr(module.split,'_scope',scope)
    native=subprocess.Popen
    def docker_fixture(argv,**kwargs):
        calls.append(('docker',argv))
        assert argv==['/usr/bin/docker','--host','unix:///var/run/docker.sock','image','save',*sorted(set(ids.values()))]
        script='import pathlib,sys;sys.stdout.buffer.write(pathlib.Path(sys.argv[1]).read_bytes());sys.stdout.flush();sys.exit(int(sys.argv[2]))'
        if fault=='stderr':script='import sys;sys.stderr.write("explicit protocol failure");'+script
        if fault=='running-guard-lost':script='import time;time.sleep(15);'+script
        child=native([sys.executable,'-I','-B','-c',script,str(fixture),'1' if fault=='export-fail' else '0'],**kwargs)
        children.append(child);return child
    monkeypatch.setattr(module,'_image_export_process',docker_fixture,raising=False)
    checks=0
    def guard():
        nonlocal checks
        checks+=1
        if fault=='source-drift' and any(isinstance(item,tuple) and item[0]=='docker' for item in calls):
            roles['frontend']['image']='sha256:'+'e'*64
        if fault=='lost-guard' and checks>1:raise module.FormalSnapshotError('OWNED_GUARD_LOST')
        if fault=='running-guard-lost' and children:raise module.FormalSnapshotError('OWNED_GUARD_LOST')
    native_hash=module.snapshot._hash_object;hashes=0
    def hash_bytes(path,*,guard=None):
        nonlocal hashes
        if Path(path)==target:
            hashes+=1
            if fault=='archive-drift' and hashes==2:
                with target.open('ab') as changed:changed.write(b'LATE_CHANGED_BYTES')
        return native_hash(path,guard=guard)
    monkeypatch.setattr(module.snapshot,'_hash_object',hash_bytes)
    if fault=='existing':target.write_bytes(b'OLD_ARCHIVE_KEEP')
    if fault is None:
        result=module.capture_original_application_images(claim,target,guard=guard,timeout=10)
        assert result['sha256']==hashlib.sha256(target.read_bytes()).hexdigest()
        assert result['bytes']==target.stat().st_size and result['images']==ids
        assert result['productionEvidence'] is False and result['hostWritersVerified'] is False
        assert len([item for item in calls if isinstance(item,tuple) and item[0]=='docker'])==1
    else:
        with pytest.raises(module.FormalSnapshotError):
            module.capture_original_application_images(claim,target,guard=guard,timeout=10)
        if fault=='existing':assert target.read_bytes()==b'OLD_ARCHIVE_KEEP'
        if fault not in ('existing','lost-guard'):assert target.exists()
    assert fixture.exists()
    assert all(child.poll() is not None for child in children)

def test_original_image_capture_is_in_owner_after_full_source_gate_and_before_recovery():
    source=(LINUX/'formal_capture_producer.py').read_text(encoding='utf-8')
    body=source[source.index('def run_approved_original_lifecycle_owner'):source.index('\ndef main(')]
    assert 'capture_original_application_images' in body
    assert body.index('_require_original_lifecycle_full_sources(claim)')<body.index('capture_original_application_images')
    assert body.index('capture_original_application_images')<body.index('permit_recovery_dependencies')


# Finite native file/claim verification. SQL remains an explicit boundary actor;
# these cases cannot prove actual SQL flags, writer exclusion or production data.
NATIVE_A_PREFENCE_TESTS = r'''"""Real Linux files, flock, journals and signed A consumption; explicit SQL actor."""
import base64
import hashlib
import json
import os
from pathlib import Path
import subprocess
import sys
import tempfile
import time

import pytest

sys.path.insert(0, '/workspace/linux')
import formal_capture_producer as producer
import formal_snapshot_linux as snapshot


def digest(raw):
    return hashlib.sha256(raw).hexdigest()


class DatabaseProtocol:
    """SQL wire actor only; native files and the ownership claim are real."""
    def __init__(self, identity, read_only=0):
        self.identity=identity;self.read_only=read_only;self.super_read_only=0
        self.queries=[];self.sets=0;self.drift=False
    def query(self, sql):
        self.queries.append(sql)
        if sql=='SELECT @@server_uuid, @@GLOBAL.read_only, @@GLOBAL.super_read_only;':
            selected=self.read_only
            if self.drift and len(self.queries)>1:selected=1-selected
            return '%s\t%d\t%d\n'%(self.identity,selected,self.super_read_only)
        if sql=='SET GLOBAL read_only=ON; SET GLOBAL super_read_only=ON;':
            self.sets+=1;self.read_only=1;self.super_read_only=1
            return ''
        if sql.startswith('SELECT @@GLOBAL.read_only, @@GLOBAL.super_read_only,'):
            assert self.read_only==self.super_read_only==1
            return '1\t1\t0\n'
        raise AssertionError('TASK_TEST_UNAPPROVED_SQL_WIRE')


def openssl(*args):
    result=subprocess.run(['/usr/bin/openssl',*args],capture_output=True,timeout=5)
    assert result.returncode==0 and not result.stderr, 'TASK_TEST_OPENSSL_FAILED'
    return result.stdout


@pytest.fixture
def native_case():
    assert sys.platform=='linux' and os.geteuid()==0, 'TASK_TEST_NATIVE_LINUX_ROOT_REQUIRED'
    producer.require_native_runtime()
    assert Path('/control').is_dir() and Path('/control').stat().st_mode&0o777==0o700
    token=snapshot.observation_deadline.set(time.monotonic()+30)
    claim=None
    try:
        with tempfile.TemporaryDirectory(prefix='p2-prefence-',dir='/control') as directory:
            root=Path(directory)
            binding=dict(operationId='prod-owned-prefence',sourceFingerprint='9'*64,
                databaseUuid='0fa6c22b-5a90-11f1-ade1-0242ac150002',minioContainerId='a'*64)
            claim=snapshot.FormalMaintenanceClaim(root/'claim',binding)
            database=DatabaseProtocol(binding['databaseUuid'])
            registry=root/'registry';registry.mkdir(mode=0o700)
            yield dict(root=root,claim=claim,binding=binding,database=database,registry=registry)
            claim.close();claim=None
    finally:
        if claim is not None:claim.close()
        snapshot.observation_deadline.reset(token)


def construct(case):
    claim=case['claim'];root=case['root']
    files={}
    for name in producer.PRODUCTION_A_SOURCE_FILES:
        path=claim.root/name
        if name not in ('binding.json','database-prefence-original.json'):
            claim._write(path,snapshot._canonical_bytes(dict(contractFixture=name)))
        files[name]=dict(path=str(path),sha256=digest(path.read_bytes()))
    release=dict(schemaVersion='release-formal-original-package-binding/v2',
        claimBindingSha256=digest(claim._plan),
        approvedSourceTargetSha256=files['approved-source-target.json']['sha256'],
        deploymentAttemptId='1'*64,leaseTokenDigest='2'*64,scope=dict(environment='prod'),
        package=dict(releaseTag='owned-prefence',packageDigest='3'*64,manifestDigest='4'*64,planDigest='5'*64))
    release_raw=snapshot._canonical_bytes(release)
    claim._write(claim.root/'original-release-binding.json',release_raw)
    inventory=dict(schemaVersion='release-formal-production-a-original-sources/v1',
        claimBindingSha256=digest(claim._plan),approvedSourceTargetSha256=release['approvedSourceTargetSha256'],
        hostBootId=Path('/proc/sys/kernel/random/boot_id').read_text().strip(),files=files)
    source_raw=snapshot._canonical_bytes(inventory)
    expected=producer.production_a_resume_binding(release_raw,source_raw,digest(source_raw))
    private=root/'task-only-private.pem'
    openssl('genpkey','-algorithm','ED25519','-out',str(private));private.chmod(0o600)
    key=root/'task-only-public.der'
    key.write_bytes(openssl('pkey','-in',str(private),'-pubout','-outform','DER'));key.chmod(0o644)
    now=int(time.time())
    payload=dict(expected,purpose='RESTORE_PRODUCTION_SAVED_ORIGINAL_STATE',grantId='8'*32,
        actorId=17,tenantId=122,issuedAt=now-1,expiresAt=now+120,
        policy=dict(originalStateOnly=True,preserveStoppedAndAbsent=True,
            preserveUnfinishedRecords=True,noBootStateWrites=True))
    message=root/'task-only-payload.json'
    message.write_bytes(json.dumps(payload,separators=(',',':')).encode('ascii'));message.chmod(0o600)
    signature=openssl('pkeyutl','-sign','-inkey',str(private),'-rawin','-in',str(message))
    approval=root/'task-only-approval.json'
    envelope=dict(schemaVersion='formal-production-a-resume-approval/v1',keySha256=digest(key.read_bytes()),
        payloadBase64=base64.b64encode(message.read_bytes()).decode(),
        signatureBase64=base64.b64encode(signature).decode())
    approval.write_bytes(json.dumps(envelope).encode('ascii'));approval.chmod(0o600)
    info=case['registry'].stat()
    resume=dict(registry=dict(path=str(case['registry']),identity={k:getattr(info,'st_'+k)
        for k in ('dev','ino','uid','gid','mode')}),approval=dict(path=str(approval),sha256=digest(approval.read_bytes())),
        trustedKey=dict(path=str(key),sha256=digest(key.read_bytes())))
    return producer.ProductionAResumeChecks(claim,release_raw,source_raw,digest(source_raw),resume,
        guard=claim.assert_owned,thaw_guard=claim.assert_owned)


@pytest.mark.parametrize('fault',['none','read-only','existing-prefence','existing-original','flags-drift','closed-claim','late-guard'])
def test_native_prefence_seal_does_not_precreate_fence_original(native_case,fault):
    case=native_case;claim=case['claim'];database=case['database']
    path=claim.root/'database-prefence-original.json';actual=claim.root/'database-original.json'
    if fault=='read-only':database.read_only=1
    if fault=='existing-prefence':claim._write(path,b'RETAIN_PRIVATE_ORIGINAL')
    if fault=='existing-original':claim._write(actual,b'RETAIN_ACTUAL_ORIGINAL')
    if fault=='flags-drift':database.drift=True
    if fault=='closed-claim':claim.close()
    def guard():
        claim.assert_owned()
        if fault=='late-guard' and path.exists():
            raise snapshot.FormalSnapshotError('TASK_TEST_LATE_GUARD_FAILED')
    if fault in ('none','read-only'):
        result=producer.seal_production_a_database_prefence_original(claim,database,guard=guard)
        assert result['flags']['readOnly']==(1 if fault=='read-only' else 0)
        assert result['path']==str(path) and result['sha256']==digest(path.read_bytes())
        assert result['identity'][0]==claim.root.stat().st_dev
        assert not actual.exists() and claim.events==[]
        assert all(query.startswith('SELECT @@server_uuid') for query in database.queries)
        with pytest.raises(snapshot.FormalSnapshotError,match='FORMAL_MAINTENANCE_BUSY'):
            snapshot.FormalMaintenanceClaim(claim.root,case['binding'],resume=True)
    else:
        with pytest.raises(snapshot.FormalSnapshotError):
            producer.seal_production_a_database_prefence_original(claim,database,guard=guard)
        assert path.exists()==(fault in ('existing-prefence','late-guard'))
        assert actual.exists()==(fault=='existing-original')
        if fault=='existing-prefence':assert path.read_bytes()==b'RETAIN_PRIVATE_ORIGINAL'
        if fault=='existing-original':assert actual.read_bytes()==b'RETAIN_ACTUAL_ORIGINAL'
        if fault=='late-guard':assert json.loads(path.read_bytes())['serverUuid']==case['binding']['databaseUuid']
    assert database.sets==0


@pytest.mark.parametrize('fault',['none','drift-before-acquire','replace-original','remove-original',
    'replace-prefence','unrecorded-original','control-replace','closed-owner','prefence-field-drift'])
def test_native_same_owner_binds_real_fence_original_to_prefence_source(native_case,fault):
    case=native_case;claim=case['claim'];database=case['database']
    producer.seal_production_a_database_prefence_original(claim,database,guard=claim.assert_owned)
    actual=claim.root/'database-original.json';prefence=claim.root/'database-prefence-original.json'
    if fault=='unrecorded-original':claim._write(actual,prefence.read_bytes())
    if fault=='unrecorded-original':
        before=actual.read_bytes()
        with pytest.raises(snapshot.FormalSnapshotError):construct(case)
        assert actual.read_bytes()==before and list(case['registry'].iterdir())==[] and claim.events==[]
        assert database.sets==0
        return
    checks=construct(case)
    assert not actual.exists()
    checks();registry={p.name:p.read_bytes() for p in case['registry'].iterdir()}
    if fault=='replace-prefence':
        prefence.rename(case['root']/'retained-prefence.json')
        claim._write(prefence,(case['root']/'retained-prefence.json').read_bytes())
    if fault=='drift-before-acquire':database.read_only=1
    if fault=='prefence-field-drift':checks.database_prefence_raw=b'{}\n'
    if fault in ('replace-prefence','prefence-field-drift'):
        with pytest.raises(snapshot.FormalSnapshotError):
            snapshot.acquire_claimed_database_fence(claim,database,guard=checks)
        assert not actual.exists() and database.sets==0
    elif fault=='drift-before-acquire':
        with pytest.raises(snapshot.FormalSnapshotError):
            snapshot.acquire_claimed_database_fence(claim,database,guard=checks)
        assert actual.exists() and any(e['step']=='db-fence-intent' for e in claim.events)
        assert not any(e['step']=='db-fenced' for e in claim.events) and database.sets==0
    else:
        flags=snapshot.acquire_claimed_database_fence(claim,database,guard=checks)
        assert flags==json.loads(prefence.read_bytes()) and actual.read_bytes()==prefence.read_bytes()
        assert database.sets==1
        if fault in ('replace-original','control-replace'):
            actual.rename(case['root']/'retained-original.json')
            claim._write(actual,(case['root']/'retained-original.json').read_bytes())
        if fault=='remove-original':actual.rename(case['root']/'retained-original.json')
        if fault=='closed-owner':claim.close()
        if fault=='control-replace':
            with pytest.raises(snapshot.FormalSnapshotError):
                checks.control_phase_guard(maintenance_end=time.time()+90,deadline=time.monotonic()+15)
        elif fault in ('replace-original','remove-original','closed-owner'):
            with pytest.raises((snapshot.FormalSnapshotError,OSError)):checks()
        else:
            checks()
            native=checks.control_phase_guard(maintenance_end=time.time()+90,deadline=time.monotonic()+15)
            native()
            with pytest.raises(snapshot.FormalSnapshotError,match='FORMAL_MAINTENANCE_BUSY'):
                snapshot.FormalMaintenanceClaim(claim.root,case['binding'],resume=True)
    assert registry=={p.name:p.read_bytes() for p in case['registry'].iterdir()}
    assert not any(e['step'].endswith(('-open-intent','-open-restored')) for e in claim.events)
'''


# Extracted unchanged into the task-specific actual Linux runner.
# Eleven new cases only; prior seventeen grant checks are not collected.
NATIVE_A_CONTROL_PHASE_TESTS = '"""Prepared native verification; not executed without this task\'s Docker window.\n\nUses actual Linux/root files, flock, boot ID, Ed25519/OpenSSL and grant registry.\nThe 23 inventory payloads are explicit contract fixtures, not production origins.\nNo Docker socket, SQL, network, service actions, freeze, or production grants.\n"""\nimport base64\nimport hashlib\nimport json\nimport os\nfrom pathlib import Path\nimport subprocess\nimport sys\nimport tempfile\nimport time\n\nimport pytest\n\nsys.path.insert(0, \'/workspace/linux\')\n\n\ndef digest(raw):\n    return hashlib.sha256(raw).hexdigest()\n\n\ndef openssl(*args):\n    result = subprocess.run([\'/usr/bin/openssl\', *args], capture_output=True, timeout=5)\n    assert result.returncode == 0, \'TASK_TEST_OPENSSL_FAILED\'\n    return result.stdout\n\n\n@pytest.fixture\ndef native_case():\n    # Fail before any fixture writes on unsupported hosts; no skip or substitute.\n    assert sys.platform == \'linux\' and os.geteuid() == 0, \'TASK_TEST_NATIVE_LINUX_ROOT_REQUIRED\'\n    import formal_capture_producer as producer\n    import formal_snapshot_linux as snapshot\n    import formal_boot_state as boot\n    producer.require_native_runtime()\n    assert Path(\'/control\').is_dir() and Path(\'/control\').stat().st_mode & 0o777 == 0o700\n    token = snapshot.observation_deadline.set(time.monotonic() + 30)\n    claim = None\n    try:\n        with tempfile.TemporaryDirectory(prefix=\'p2-resume-\', dir=\'/control\') as directory:\n            root = Path(directory)\n            binding = dict(operationId=\'prod-owned-p2\', sourceFingerprint=\'9\' * 64,\n                databaseUuid=\'0fa6c22b-5a90-11f1-ade1-0242ac150002\', minioContainerId=\'a\' * 64)\n            claim = snapshot.FormalMaintenanceClaim(root / \'claim\', binding)\n            registry = root / \'registry\'\n            registry.mkdir(mode=0o700)\n            files = {}\n            for name in producer.PRODUCTION_A_SOURCE_FILES:\n                path = claim.root / name\n                if name != \'binding.json\':\n                    claim._write(path, snapshot._canonical_bytes(dict(contractFixture=name)))\n                files[name] = dict(path=str(path), sha256=digest(path.read_bytes()))\n            release = dict(schemaVersion=\'release-formal-original-package-binding/v2\',\n                claimBindingSha256=digest(claim._plan),\n                approvedSourceTargetSha256=files[\'approved-source-target.json\'][\'sha256\'],\n                deploymentAttemptId=\'1\' * 64, leaseTokenDigest=\'2\' * 64,\n                scope=dict(environment=\'prod\'),\n                package=dict(releaseTag=\'owned-p2\', packageDigest=\'3\' * 64,\n                    manifestDigest=\'4\' * 64, planDigest=\'5\' * 64))\n            release_raw = snapshot._canonical_bytes(release)\n            claim._write(claim.root / \'original-release-binding.json\', release_raw)\n            inventory = dict(schemaVersion=\'release-formal-production-a-original-sources/v1\',\n                claimBindingSha256=digest(claim._plan),\n                approvedSourceTargetSha256=release[\'approvedSourceTargetSha256\'],\n                hostBootId=Path(\'/proc/sys/kernel/random/boot_id\').read_text().strip(), files=files)\n            source_raw = snapshot._canonical_bytes(inventory)\n            expected = producer.production_a_resume_binding(release_raw, source_raw, digest(source_raw))\n            private = root / \'task-only-private.pem\'\n            openssl(\'genpkey\', \'-algorithm\', \'ED25519\', \'-out\', str(private))\n            private.chmod(0o600)\n            key = root / \'task-only-public.der\'\n            key.write_bytes(openssl(\'pkey\', \'-in\', str(private), \'-pubout\', \'-outform\', \'DER\'))\n            key.chmod(0o644)\n            now = int(time.time())\n            payload = dict(expected, purpose=\'RESTORE_PRODUCTION_SAVED_ORIGINAL_STATE\',\n                grantId=\'8\' * 32, actorId=17, tenantId=122, issuedAt=now - 2, expiresAt=now + 5,\n                policy=dict(originalStateOnly=True, preserveStoppedAndAbsent=True,\n                    preserveUnfinishedRecords=True, noBootStateWrites=True))\n            message = root / \'task-only-payload.json\'\n            message.write_bytes(json.dumps(payload, separators=(\',\', \':\')).encode(\'ascii\'))\n            message.chmod(0o600)\n            signature = openssl(\'pkeyutl\', \'-sign\', \'-inkey\', str(private), \'-rawin\', \'-in\', str(message))\n            approval = root / \'task-only-approval.json\'\n            envelope = dict(schemaVersion=\'formal-production-a-resume-approval/v1\',\n                keySha256=digest(key.read_bytes()), payloadBase64=base64.b64encode(message.read_bytes()).decode(),\n                signatureBase64=base64.b64encode(signature).decode())\n            approval.write_bytes(json.dumps(envelope).encode(\'ascii\'))\n            approval.chmod(0o600)\n            info = registry.stat()\n            resume = dict(registry=dict(path=str(registry), identity={k: getattr(info, \'st_\' + k)\n                for k in (\'dev\', \'ino\', \'uid\', \'gid\', \'mode\')}),\n                approval=dict(path=str(approval), sha256=digest(approval.read_bytes())),\n                trustedKey=dict(path=str(key), sha256=digest(key.read_bytes())))\n            owner_file = root / \'actual-test-owner-state\'\n            owner_file.write_bytes(b\'held\'); owner_file.chmod(0o600)\n            owner_pin = boot._read_secure(owner_file, 0o600, 32, no_atime=True)\n\n            def guard():\n                claim.assert_owned()\n                snapshot.observation_timeout(1)\n                snapshot.require(boot._read_secure(owner_file, 0o600, 32, no_atime=True) == owner_pin,\n                    \'TASK_TEST_OWNER_CHANGED\')\n\n            def thaw_guard():\n                # This grant-only fixture never requested or performed a freeze.\n                # It must not be reported as a positive actual thaw verification.\n                claim.assert_owned()\n                snapshot.require(not any(e[\'step\'] == \'split-raw-intent\' for e in claim.events),\n                    \'TASK_TEST_FREEZE_STATE_NOT_APPROVED\')\n\n            def construct():\n                return producer.ProductionAResumeChecks(claim, release_raw, source_raw, digest(source_raw),\n                    resume, guard=guard, thaw_guard=thaw_guard)\n\n            yield dict(producer=producer, snapshot=snapshot, claim=claim, registry=registry,\n                key=key, approval=approval, owner_file=owner_file, construct=construct,\n                expected=expected, source=claim.root / \'host-derived-originals.json\')\n            claim.close()\n            claim = None\n    finally:\n        if claim is not None:\n            claim.close()\n        snapshot.observation_deadline.reset(token)\n\n\n@pytest.mark.parametrize(\'fault\', [\'none\', \'journal-append\', \'source-replaced\',\n    \'journal-replaced\', \'registry-replaced\', \'claim-closed\', \'binding-changed\',\n    \'deadline-expired\', \'maintenance-expired\', \'signed-expired\', \'other-thread\'])\ndef test_control_phase_checks_only_unfrozen_originals_under_same_owner(native_case, fault):\n    case = native_case\n    checks = case[\'construct\']()\n    claim = case[\'claim\']\n    before = {p.name: p.read_bytes() for p in case[\'registry\'].iterdir()}\n    original_events = list(claim.events)\n    # Actual source/SQL/thaw observers belong outside the critical phase.\n    # Raising hooks prove this finite guard does not invoke those interfaces;\n    # this fixture proves neither writer exclusion nor actual service thaw.\n    def forbidden_runtime_observer():\n        raise AssertionError(\'STOPPED_OR_FROZEN_RUNTIME_OBSERVER_IN_CONTROL_PHASE\')\n    checks.guard = forbidden_runtime_observer\n    checks.thaw_guard = forbidden_runtime_observer\n    if fault == \'maintenance-expired\':\n        with pytest.raises(case[\'snapshot\'].FormalSnapshotError):\n            checks.control_phase_guard(maintenance_end=time.time()-1,\n                deadline=time.monotonic()+15)\n        assert claim.events == original_events\n        return\n    guard = checks.control_phase_guard(maintenance_end=time.time()+90,\n        deadline=time.monotonic()+15)\n    assert callable(guard)\n    if fault == \'journal-append\':\n        # Only the actual owner can extend its validated retained journal.\n        claim.record(\'split-raw-intent\', {\'testOnly\': True})\n        guard()\n        guard()\n        assert claim.events[-1][\'step\'] == \'split-raw-intent\'\n    elif fault == \'none\':\n        guard()\n        assert claim.events == original_events\n        binding = json.loads(claim._plan)\n        with pytest.raises(case[\'snapshot\'].FormalSnapshotError, match=\'FORMAL_MAINTENANCE_BUSY\'):\n            case[\'snapshot\'].FormalMaintenanceClaim(claim.root, binding, resume=True)\n    else:\n        if fault in (\'source-replaced\', \'journal-replaced\'):\n            path = case[\'source\'] if fault == \'source-replaced\' else claim.root/\'event-000001.json\'\n            saved = case[\'key\'].parent/(\'saved-\'+path.name)\n            path.rename(saved)\n            path.write_bytes(saved.read_bytes())\n            path.chmod(0o600)\n        elif fault == \'registry-replaced\':\n            case[\'registry\'].rename(case[\'registry\'].with_name(\'registry.saved\'))\n            case[\'registry\'].mkdir(mode=0o700)\n        elif fault == \'claim-closed\':\n            claim.close()\n        elif fault == \'binding-changed\':\n            checks.binding[\'attemptId\'] = \'f\'*64\n        elif fault == \'deadline-expired\':\n            case[\'snapshot\'].observation_deadline.set(time.monotonic()-1)\n        elif fault == \'signed-expired\':\n            # Actual signed expiry and wall/monotonic clocks, no clock double.\n            until = checks.consumed[\'expiresAt\']+0.03\n            assert 0 < until-time.time() <= 6\n            while time.time() < until:\n                time.sleep(min(0.05, max(0, until-time.time())))\n        if fault == \'other-thread\':\n            import threading\n            failures = []\n            def call_from_other_thread():\n                try: guard()\n                except Exception as error: failures.append(error)\n            child = threading.Thread(target=call_from_other_thread)\n            child.start(); child.join(timeout=5)\n            assert not child.is_alive()\n            assert len(failures) == 1 and isinstance(failures[0], case[\'snapshot\'].FormalSnapshotError)\n        else:\n            with pytest.raises(case[\'snapshot\'].FormalSnapshotError): guard()\n    retained_registry = case[\'registry\'].with_name(\'registry.saved\') if fault == \'registry-replaced\' else case[\'registry\']\n    assert before == {p.name:p.read_bytes() for p in retained_registry.iterdir()}\n    assert not any(e[\'step\'].endswith((\'-open-intent\', \'-open-restored\')) for e in claim.events)\n'

NATIVE_A_BOOT_SOURCE_TESTS = '"""Finite new boot-source tests; actual native files/claim, explicit machine fixture.\n\n/etc is a private tmpfs in the dedicated container. Its machine-id is a fixture\nread by the unchanged fixed native reader, never an actual production identity.\nNo daemon, Docker socket, SQL, SSH, service, boot transition, freeze or NAS.\n"""\nimport hashlib\nimport json\nimport os\nfrom pathlib import Path\nimport stat\nimport sys\nimport tempfile\nimport time\n\nimport pytest\n\nsys.path.insert(0,\'/workspace/linux\')\nimport formal_capture_producer as producer\nimport formal_snapshot_linux as snapshot\nimport formal_boot_state as boot\n\n\ndef digest(raw):return hashlib.sha256(raw).hexdigest()\n\n\n@pytest.fixture\ndef native_case():\n    assert sys.platform==\'linux\' and os.geteuid()==0,\'TASK_TEST_NATIVE_LINUX_ROOT_REQUIRED\'\n    producer.require_native_runtime()\n    assert stat.S_IMODE(Path(\'/control\').stat().st_mode)==0o700\n    assert stat.S_IMODE(Path(\'/etc\').stat().st_mode)==0o700\n    machine=Path(\'/etc/machine-id\')\n    assert not machine.exists(),\'TASK_TEST_PRIVATE_MACHINE_FIXTURE_EXISTS\'\n    raw=b\'8b1c7c0b8b764ad0a0cd2d82be41baf5\\n\'\n    with machine.open(\'xb\') as out:out.write(raw)\n    machine.chmod(0o444)\n    token=snapshot.observation_deadline.set(time.monotonic()+15)\n    claim=None\n    try:\n        with tempfile.TemporaryDirectory(prefix=\'p2-boot-\',dir=\'/control\') as directory:\n            root=Path(directory)\n            source_root=root/\'actual-source\';source_root.mkdir(mode=0o755)\n            source_path=source_root/\'boot.json\'\n            original=b\'{ "state": "OPEN", "epoch": "", "unknown": [1,2] }\\n\'\n            with source_path.open(\'xb\') as out:out.write(original)\n            source_path.chmod(0o644)\n            binding=dict(operationId=\'prod-owned-boot-source\',sourceFingerprint=\'a\'*64,\n                databaseUuid=\'0fa6c22b-5a90-11f1-ade1-0242ac150002\',minioContainerId=\'b\'*64)\n            claim=snapshot.FormalMaintenanceClaim(root/\'claim\',binding)\n            source=dict(schemaVersion=\'release-formal-production-a-boot-source/v1\',role=\'backend\',peer=\'local\',\n                machineIdSha256=digest(raw),bootId=Path(\'/proc/sys/kernel/random/boot_id\').read_text().strip(),\n                path=str(source_path))\n            yield dict(root=root,claim=claim,binding=binding,source=source,path=source_path,\n                raw=original,machine=machine,deadline=time.monotonic()+10)\n            claim.close();claim=None\n    finally:\n        if claim is not None:claim.close()\n        machine.unlink()\n        snapshot.observation_deadline.reset(token)\n\n\ndef seal(case,role=\'backend\',guard=None):\n    return producer.seal_production_a_boot_original(case[\'claim\'],role,case[\'source\'],\n        guard=case[\'claim\'].assert_owned if guard is None else guard,deadline=case[\'deadline\'])\n\n\n@pytest.mark.parametrize(\'role\',[\'backend\',\'test-backend\',\'test-rehearsal\',\'backup-backend\',\'backup-rehearsal\'])\ndef test_native_approved_local_role_preserves_actual_boot_bytes(native_case,role):\n    case=native_case;case[\'source\'][\'role\']=role\n    before=case[\'path\'].stat();events=list(case[\'claim\'].events)\n    result=seal(case,role)\n    saved=case[\'claim\'].root/(\'boot-\'+role+\'-original.json\')\n    raw,identity=boot._read_secure(saved,0o600,16384,no_atime=True)\n    original=json.loads(raw)\n    assert result==dict(path=str(saved),sha256=digest(raw),identity=list(identity))\n    assert original[\'present\'] is True and original[\'sha256\']==digest(case[\'raw\'])\n    import base64\n    assert base64.b64decode(original[\'originalBase64\'],validate=True)==case[\'raw\']\n    assert original[\'sourceFileIdentity\']==list(boot._signature(before))\n    assert case[\'path\'].read_bytes()==case[\'raw\'] and boot._signature(case[\'path\'].stat())==boot._signature(before)\n    assert case[\'claim\'].events==events\n    with pytest.raises(snapshot.FormalSnapshotError,match=\'FORMAL_MAINTENANCE_BUSY\'):\n        snapshot.FormalMaintenanceClaim(case[\'claim\'].root,case[\'binding\'],resume=True)\n\n\n@pytest.mark.parametrize(\'kind\',[\'absent-final-file\',\'present-empty-file\'])\ndef test_native_boot_source_distinguishes_actual_absence_from_empty(native_case,kind):\n    case=native_case\n    if kind==\'absent-final-file\':case[\'path\'].unlink()\n    else:case[\'path\'].write_bytes(b\'\')\n    result=seal(case)\n    original=json.loads(Path(result[\'path\']).read_bytes())\n    assert bool(original[\'sourceParentIdentity\'])\n    if kind==\'absent-final-file\':\n        assert original[\'present\'] is False and not case[\'path\'].exists()\n        assert all(original[k] is None for k in (\'originalBase64\',\'sha256\',\'sourceFileIdentity\'))\n    else:\n        assert original[\'present\'] is True and original[\'originalBase64\']==\'\'\n        assert original[\'sha256\']==digest(b\'\') and case[\'path\'].read_bytes()==b\'\'\n        assert original[\'sourceFileIdentity\'][6]==0\n    assert case[\'claim\'].events==[]\n\n\n@pytest.mark.parametrize(\'fault\',[\'missing-parent\',\'wrong-machine\',\'wrong-boot\',\'remote-peer\',\'symlink\',\n    \'hardlink\',\'world-writable\',\'fifo\',\'existing-original\',\'closed-claim\',\'deadline\',\'boot-transition\',\n    \'delete-during-read\',\'preseal-drift\',\'postseal-drift\',\'late-guard\'])\ndef test_native_boot_source_refuses_uncertainty_and_retains_original(native_case,fault):\n    case=native_case;path=case[\'path\'];claim=case[\'claim\'];source=case[\'source\']\n    target=claim.root/\'boot-backend-original.json\'\n    if fault==\'missing-parent\':source[\'path\']=str(case[\'root\']/\'missing-parent\'/\'boot.json\')\n    if fault==\'wrong-machine\':source[\'machineIdSha256\']=\'c\'*64\n    if fault==\'wrong-boot\':source[\'bootId\']=\'00000000-0000-0000-0000-000000000001\'\n    if fault==\'remote-peer\':source[\'peer\']=\'172.30.30.58\'\n    if fault in (\'symlink\',\'hardlink\',\'fifo\'):\n        path.unlink()\n        if fault==\'fifo\':os.mkfifo(path,0o644)\n        else:\n            actual=case[\'root\']/\'actual-retained-boot.json\';actual.write_bytes(case[\'raw\']);actual.chmod(0o644)\n            if fault==\'symlink\':path.symlink_to(actual)\n            else:os.link(actual,path)\n    if fault==\'world-writable\':path.chmod(0o666)\n    if fault==\'existing-original\':claim._write(target,b\'RETAIN_EXISTING\')\n    if fault==\'closed-claim\':claim.close()\n    if fault==\'deadline\':case[\'deadline\']=time.monotonic()-1\n    if fault==\'boot-transition\':claim.record(\'boot-backend-bound\',dict(taskTestOnly=True))\n    calls=[];mutated=[]\n    def guard():\n        claim.assert_owned();calls.append(True)\n        if not mutated and ((fault in (\'delete-during-read\',\'preseal-drift\') and len(calls)==3) or\n                            (fault in (\'postseal-drift\',\'late-guard\') and target.exists())):\n            mutated.append(True)\n            if fault==\'late-guard\':raise snapshot.FormalSnapshotError(\'TASK_TEST_LATE_GUARD_FAILED\')\n            if fault==\'delete-during-read\':path.unlink()\n            else:path.write_bytes(b\'ACTUAL_CHANGED_BOOT_BYTES\')\n    with pytest.raises(snapshot.FormalSnapshotError):seal(case,guard=guard)\n    retained=fault in (\'existing-original\',\'postseal-drift\',\'late-guard\')\n    assert target.exists() is retained\n    if fault==\'existing-original\':assert target.read_bytes()==b\'RETAIN_EXISTING\'\n    if fault in (\'postseal-drift\',\'late-guard\'):\n        original=json.loads(target.read_bytes())\n        assert original[\'sha256\']==digest(case[\'raw\']) and original[\'present\'] is True\n        assert stat.S_IMODE(target.stat().st_mode)==0o600\n    if fault not in (\'closed-claim\',\'boot-transition\'):assert claim.events==[]\n    if fault not in (\'symlink\',\'hardlink\',\'fifo\',\'delete-during-read\',\'preseal-drift\',\'postseal-drift\'):\n        assert path.read_bytes()==case[\'raw\']\n'


# Appended to the owned producer test; explicit wire actors, no actual SSH.
@pytest.mark.parametrize('fault',['none','absent','wrong-peer','wrong-boot','wrong-role',
    'wrong-key','late-key','changed-connection','wrong-record','noncanonical','transport-failed',
    'deadline','guard','changed-source'])
def test_production_a_remote_boot_transport_is_bound_and_one_shot(tmp_path,monkeypatch,fault):
    m=load()
    if os.name=='nt' and 'fcntl' not in sys.modules:
        unavailable=types.ModuleType('fcntl')
        unavailable.flock=lambda *args:(_ for _ in ()).throw(AssertionError('native flock outside protocol test'))
        monkeypatch.setitem(sys.modules,'fcntl',unavailable)
    import formal_external_callers as callers
    import formal_controlled_container_sources as controlled
    keys=b'task-specific host-key protocol bytes\n'
    key_sha=hashlib.sha256(keys).hexdigest()
    source=dict(schemaVersion='release-formal-production-a-boot-source/v1',role='test-backend',
        peer='172.30.30.58',machineIdSha256='a'*64,
        bootId='00000000-0000-0000-0000-000000000001',path='/approved/test/boot.json')
    connection=dict(host=source['peer'],port=22,user='root',bootId=source['bootId'],
        knownHostsFile='/approved/known-hosts',knownHostsSha256=key_sha,dockerEndpoint='unix:///var/run/docker.sock')
    if fault=='wrong-peer':connection['host']='172.30.30.59'
    if fault=='wrong-boot':connection['bootId']='00000000-0000-0000-0000-000000000002'
    if fault=='wrong-role':source['role']='backup-backend'
    if fault=='wrong-key':connection['knownHostsSha256']='0'*64
    original=b'{ "state": "OPEN", "unknown": true }\n'
    record=dict(schemaVersion='release-formal-production-a-boot-original/v1',
        **{k:source[k] for k in ('role','peer','machineIdSha256','bootId','path')},
        sourceParentIdentity=[[1,2,0,0,stat.S_IFDIR|0o755]],present=True,
        originalBase64=base64.b64encode(original).decode('ascii'),sha256=hashlib.sha256(original).hexdigest(),
        sourceFileIdentity=[1,3,0,0,stat.S_IFREG|0o644,1,len(original),4,5])
    if fault=='absent':record.update(present=False,originalBase64=None,sha256=None,sourceFileIdentity=None)
    if fault=='wrong-record':record['path']='/other/boot.json'
    wire=m.snapshot._canonical_bytes(record)
    if fault=='noncanonical':wire=json.dumps(record).encode()
    calls=[];key_reads=[]
    def key_reader(selected):
        key_reads.append(True)
        return b'ACTUAL_KEY_DRIFT' if fault=='late-key' and calls else keys
    def execute(argv,body,*,deadline,action):
        calls.append((argv,body))
        assert action is False and deadline>time.monotonic()
        if fault=='transport-failed':raise controlled.ControlledSourceError('CONTROLLED_CONTAINER_COMMAND_UNCERTAIN')
        if fault=='changed-connection':transport._connection['knownHostsFile']='/other/keys'
        if fault=='changed-source':source['path']='/other/source.json'
        return wire
    def guard():
        if fault=='guard':raise m.FormalSnapshotError('TASK_TEST_REMOTE_BOOT_GUARD_FAILED')
    monkeypatch.setattr(m,'require_native_runtime',lambda:None)
    monkeypatch.setattr(callers,'_known_hosts_bytes',key_reader)
    monkeypatch.setattr(controlled,'_execute_controlled_script',execute)
    deadline=time.monotonic()-1 if fault=='deadline' else time.monotonic()+10
    if fault in ('none','absent'):
        transport=m.SshProductionABootSource(connection)
        result=transport(source,guard=guard,deadline=deadline)
        assert result==record and len(calls)==1
        argv,body=calls[0]
        assert argv[-2:]==['root@172.30.30.58','/bin/bash --noprofile --norc -s']
        assert 'StrictHostKeyChecking=yes' in argv and 'ConnectionAttempts=1' in argv
        assert b'/usr/bin/python3 -I -B -' in body and b'SSH_CONNECTION' in body
        assert b'FORMAL_A_BOOT_READER' in body and b'/usr/bin/docker' not in body
        assert len(key_reads)>=2
    else:
        with pytest.raises(m.FormalSnapshotError):
            transport=m.SshProductionABootSource(connection)
            transport(source,guard=guard,deadline=deadline)
        assert len(calls)<=1
        if fault in ('wrong-peer','wrong-boot','wrong-role','wrong-key','deadline','guard'):assert calls==[]


@pytest.mark.parametrize('fault',['none','preseal-drift','postseal-drift','missing-connection','local-with-connection'])
def test_production_a_remote_boot_seal_keeps_exact_peer_and_original(tmp_path,monkeypatch,fault):
    m=load()
    if os.name=='nt' and 'fcntl' not in sys.modules:
        unavailable=types.ModuleType('fcntl')
        unavailable.flock=lambda *args:(_ for _ in ()).throw(AssertionError('native flock outside protocol test'))
        monkeypatch.setitem(sys.modules,'fcntl',unavailable)
    import formal_boot_state as boot
    root=tmp_path/'claim';root.mkdir()
    class Claim:
        def __init__(self):self.root=root;self.events=[]
        def assert_owned(self):pass
        def _write(self,path,raw):
            with Path(path).open('xb') as output:output.write(raw)
            os.chmod(path,0o600)
    claim=Claim()
    source=dict(schemaVersion='release-formal-production-a-boot-source/v1',role='test-backend',peer='172.30.30.58',
        machineIdSha256='a'*64,bootId='00000000-0000-0000-0000-000000000001',path='/approved/test/boot.json')
    connection={'explicitProtocolConnection':True}
    if fault=='missing-connection':connection=None
    if fault=='local-with-connection':source['peer']='local'
    original=b'KEEP_EXACT_REMOTE_BYTES\n'
    record=dict(schemaVersion='release-formal-production-a-boot-original/v1',
        **{k:source[k] for k in ('role','peer','machineIdSha256','bootId','path')},
        sourceParentIdentity=[[1,2,0,0,stat.S_IFDIR|0o755]],present=True,
        originalBase64=base64.b64encode(original).decode('ascii'),sha256=hashlib.sha256(original).hexdigest(),
        sourceFileIdentity=[1,3,0,0,stat.S_IFREG|0o644,1,len(original),4,5])
    reads=[]
    class RemoteWireActor:
        def __init__(self,selected):assert selected==connection and selected is not None
        def __call__(self,selected,*,guard,deadline):
            guard();reads.append(True)
            if (fault=='preseal-drift' and len(reads)>=2) or (fault=='postseal-drift' and len(reads)>=3):
                return dict(record,sourceFileIdentity=[1,4,0,0,stat.S_IFREG|0o644,1,len(original),4,5])
            return record
    def forbidden_local(*args,**kwargs):raise AssertionError('remote source cannot execute local sampler')
    monkeypatch.setattr(m,'require_native_runtime',lambda:None)
    monkeypatch.setattr(m,'FormalMaintenanceClaim',Claim)
    monkeypatch.setattr(m,'SshProductionABootSource',RemoteWireActor,raising=False)
    monkeypatch.setattr(m,'_sample_production_a_boot_original_source',forbidden_local)
    monkeypatch.setattr(boot,'_read_secure',lambda path,mode,limit,*,no_atime:
        (Path(path).read_bytes(),boot._signature(os.stat(path,follow_symlinks=False))))
    target=root/'boot-test-backend-original.json'
    if fault=='none':
        result=m.seal_production_a_boot_original(claim,'test-backend',source,
            guard=claim.assert_owned,deadline=time.monotonic()+10,connection=connection)
        assert result['path']==str(target) and json.loads(target.read_bytes())==record
        assert len(reads)==3 and claim.events==[]
    else:
        with pytest.raises(m.FormalSnapshotError):
            m.seal_production_a_boot_original(claim,'test-backend',source,
                guard=claim.assert_owned,deadline=time.monotonic()+10,connection=connection)
        assert target.exists() is (fault=='postseal-drift')
        if target.exists():assert json.loads(target.read_bytes())==record
        if fault in ('missing-connection','local-with-connection'):assert reads==[]
        assert claim.events==[]

NATIVE_A_REMOTE_BOOT_SOURCE_TESTS = '"""Actual Linux files/process/FD/flock, explicit SSH envelope and machine fixtures.\n\nRuns the production read-only remote body via the actual bounded local shell\nclient. This is not actual SSH, an authenticated peer, or production identity.\nNo daemon, socket, SQL, service, boot transition, freeze, network or NAS.\n"""\nimport hashlib\nimport json\nimport os\nfrom pathlib import Path\nimport stat\nimport sys\nimport tempfile\nimport time\n\nimport pytest\n\nsys.path.insert(0,\'/workspace/linux\')\nimport formal_capture_producer as producer\nimport formal_snapshot_linux as snapshot\nimport formal_boot_state as boot\nimport formal_controlled_container_sources as controlled\n\n\ndef digest(raw):return hashlib.sha256(raw).hexdigest()\n\n\n@pytest.fixture\ndef native_case(monkeypatch):\n    assert sys.platform==\'linux\' and os.geteuid()==0,\'TASK_TEST_NATIVE_LINUX_ROOT_REQUIRED\'\n    producer.require_native_runtime()\n    assert stat.S_IMODE(Path(\'/control\').stat().st_mode)==0o700\n    assert stat.S_IMODE(Path(\'/etc\').stat().st_mode)==0o700\n    machine=Path(\'/etc/machine-id\')\n    assert not machine.exists(),\'TASK_TEST_PRIVATE_MACHINE_FIXTURE_EXISTS\'\n    machine_raw=b\'8b1c7c0b8b764ad0a0cd2d82be41baf5\\n\'\n    with machine.open(\'xb\') as out:out.write(machine_raw)\n    machine.chmod(0o444)\n    token=snapshot.observation_deadline.set(time.monotonic()+15)\n    claim=None\n    actual_executor=controlled._execute_controlled_script\n    try:\n        with tempfile.TemporaryDirectory(prefix=\'p2-remote-boot-\',dir=\'/control\') as directory:\n            root=Path(directory)\n            source_root=root/\'actual-source\';source_root.mkdir(mode=0o755)\n            path=source_root/\'boot.json\'\n            original=b\'{ "state": "OPEN", "unknown": [1,2] }\\n\'\n            with path.open(\'xb\') as out:out.write(original)\n            path.chmod(0o644)\n            keys=root/\'known-hosts\';keys.write_bytes(b\'explicit task-only host-key fixture\\n\');keys.chmod(0o644)\n            binding=dict(operationId=\'prod-owned-remote-boot\',sourceFingerprint=\'a\'*64,\n                databaseUuid=\'0fa6c22b-5a90-11f1-ade1-0242ac150002\',minioContainerId=\'b\'*64)\n            claim=snapshot.FormalMaintenanceClaim(root/\'claim\',binding)\n            source=dict(schemaVersion=\'release-formal-production-a-boot-source/v1\',role=\'test-backend\',peer=\'172.30.30.58\',\n                machineIdSha256=digest(machine_raw),bootId=Path(\'/proc/sys/kernel/random/boot_id\').read_text().strip(),path=str(path))\n            connection=dict(host=source[\'peer\'],port=22,user=\'root\',bootId=source[\'bootId\'],\n                knownHostsFile=str(keys),knownHostsSha256=digest(keys.read_bytes()),dockerEndpoint=\'unix:///var/run/docker.sock\')\n            case=dict(root=root,claim=claim,binding=binding,source=source,path=path,raw=original,\n                connection=connection,deadline=time.monotonic()+10,fault=None,calls=[])\n            def explicit_ssh_envelope(argv,body,*,deadline,action):\n                assert action is False\n                assert argv[-2:]==[\'root@\'+case[\'connection\'][\'host\'],\'/bin/bash --noprofile --norc -s\']\n                assert \'StrictHostKeyChecking=yes\' in argv and \'ConnectionAttempts=1\' in argv\n                case[\'calls\'].append(True)\n                peer=\'172.30.30.59\' if case[\'fault\']==\'ssh-peer\' else case[\'connection\'][\'host\']\n                fixture=(\'SSH_CONNECTION="192.0.2.10 43210 \'+peer+\' 22"\\n\').encode()\n                if case[\'fault\']==\'stderr\':body+=b\'printf TASK_TEST_UNEXPECTED_DIAGNOSTIC >&2\\n\'\n                result=actual_executor([\'/bin/bash\',\'--noprofile\',\'--norc\',\'-s\'],fixture+body,\n                    deadline=deadline,action=False)\n                if case[\'fault\']==\'wrong-wire\':\n                    row=json.loads(result);row[\'path\']=\'/other/boot.json\';result=snapshot._canonical_bytes(row)\n                if (case[\'fault\']==\'preseal-drift\' and len(case[\'calls\'])==1) or (\n                        case[\'fault\']==\'postseal-drift\' and len(case[\'calls\'])==2):\n                    path.write_bytes(b\'ACTUAL_SOURCE_CHANGED_DURING_SAMPLING\')\n                return result\n            monkeypatch.setattr(controlled,\'_execute_controlled_script\',explicit_ssh_envelope)\n            yield case\n            claim.close();claim=None\n    finally:\n        if claim is not None:claim.close()\n        machine.unlink()\n        snapshot.observation_deadline.reset(token)\n\n\ndef seal(case,guard=None):\n    return producer.seal_production_a_boot_original(case[\'claim\'],case[\'source\'][\'role\'],case[\'source\'],\n        guard=case[\'claim\'].assert_owned if guard is None else guard,deadline=case[\'deadline\'],connection=case[\'connection\'])\n\n\n@pytest.mark.parametrize(\'role\',[\'test-backend\',\'test-rehearsal\',\'backup-backend\',\'backup-rehearsal\'])\ndef test_native_remote_reader_body_and_claim_keep_exact_source(native_case,role):\n    case=native_case;case[\'source\'][\'role\']=role\n    peer=\'172.30.30.58\' if role.startswith(\'test-\') else \'172.30.30.59\'\n    case[\'source\'][\'peer\']=peer;case[\'connection\'][\'host\']=peer\n    before=boot._signature(case[\'path\'].stat())\n    result=seal(case)\n    saved=json.loads(Path(result[\'path\']).read_bytes())\n    assert saved[\'peer\']==peer and saved[\'role\']==role and saved[\'sha256\']==digest(case[\'raw\'])\n    assert saved[\'sourceFileIdentity\']==list(before) and case[\'path\'].read_bytes()==case[\'raw\']\n    assert boot._signature(case[\'path\'].stat())==before and case[\'claim\'].events==[] and len(case[\'calls\'])==3\n    assert stat.S_IMODE(Path(result[\'path\']).stat().st_mode)==0o600\n    with pytest.raises(snapshot.FormalSnapshotError,match=\'FORMAL_MAINTENANCE_BUSY\'):\n        snapshot.FormalMaintenanceClaim(case[\'claim\'].root,case[\'binding\'],resume=True)\n\n\n@pytest.mark.parametrize(\'kind\',[\'absent-final-file\',\'present-empty-file\'])\ndef test_native_remote_reader_body_distinguishes_absence_from_empty(native_case,kind):\n    case=native_case\n    if kind==\'absent-final-file\':case[\'path\'].unlink()\n    else:case[\'path\'].write_bytes(b\'\')\n    result=seal(case);record=json.loads(Path(result[\'path\']).read_bytes())\n    assert bool(record[\'sourceParentIdentity\'])\n    if kind==\'absent-final-file\':\n        assert record[\'present\'] is False and all(record[k] is None for k in (\'originalBase64\',\'sha256\',\'sourceFileIdentity\'))\n    else:assert record[\'present\'] is True and record[\'originalBase64\']==\'\' and record[\'sha256\']==digest(b\'\')\n    assert len(case[\'calls\'])==3 and case[\'claim\'].events==[]\n\n\n@pytest.mark.parametrize(\'fault\',[\'missing-parent\',\'wrong-machine\',\'wrong-boot\',\'ssh-peer\',\'symlink\',\n    \'hardlink\',\'world-writable\',\'fifo\',\'deadline\',\'late-guard\',\'stderr\',\'wrong-wire\'])\ndef test_native_remote_reader_actual_failure_is_retained_without_local_substitute(native_case,fault):\n    case=native_case;case[\'fault\']=fault;path=case[\'path\'];source=case[\'source\']\n    if fault==\'missing-parent\':source[\'path\']=str(case[\'root\']/\'missing-parent\'/\'boot.json\')\n    if fault==\'wrong-machine\':source[\'machineIdSha256\']=\'c\'*64\n    if fault==\'wrong-boot\':\n        source[\'bootId\']=\'00000000-0000-0000-0000-000000000001\';case[\'connection\'][\'bootId\']=source[\'bootId\']\n    if fault in (\'symlink\',\'hardlink\',\'fifo\'):\n        path.unlink()\n        if fault==\'fifo\':os.mkfifo(path,0o644)\n        else:\n            retained=case[\'root\']/\'retained-source\';retained.write_bytes(case[\'raw\']);retained.chmod(0o644)\n            if fault==\'symlink\':path.symlink_to(retained)\n            else:os.link(retained,path)\n    if fault==\'world-writable\':path.chmod(0o666)\n    if fault==\'deadline\':case[\'deadline\']=time.monotonic()-1\n    target=case[\'claim\'].root/\'boot-test-backend-original.json\'\n    def guard():\n        case[\'claim\'].assert_owned()\n        if fault==\'late-guard\' and target.exists():raise snapshot.FormalSnapshotError(\'TASK_TEST_LATE_GUARD_FAILED\')\n    with pytest.raises(snapshot.FormalSnapshotError):seal(case,guard)\n    assert target.exists() is (fault==\'late-guard\')\n    if target.exists():assert json.loads(target.read_bytes())[\'sha256\']==digest(case[\'raw\'])\n    assert case[\'claim\'].events==[]\n    assert len(case[\'calls\'])==(0 if fault==\'deadline\' else 2 if fault==\'late-guard\' else 1)\n\n\n@pytest.mark.parametrize(\'fault\',[\'preseal-drift\',\'postseal-drift\'])\ndef test_native_remote_reader_source_drift_retains_postseal_original(native_case,fault):\n    case=native_case;case[\'fault\']=fault\n    with pytest.raises(snapshot.FormalSnapshotError):seal(case)\n    target=case[\'claim\'].root/\'boot-test-backend-original.json\'\n    assert target.exists() is (fault==\'postseal-drift\')\n    if target.exists():assert json.loads(target.read_bytes())[\'sha256\']==digest(case[\'raw\'])\n    assert case[\'claim\'].events==[] and len(case[\'calls\'])==(2 if fault==\'preseal-drift\' else 3)\n\n\n@pytest.mark.parametrize(\'kind\',[\'same-local-entry\',\'remote-source-rejected-by-local-reader\'])\ndef test_native_local_branch_of_changed_sealer_stays_bound(native_case,kind):\n    case=native_case\n    if kind==\'same-local-entry\':\n        case[\'source\'][\'peer\']=\'local\';case[\'source\'][\'role\']=\'backend\';case[\'connection\']=None\n        result=seal(case)\n        assert json.loads(Path(result[\'path\']).read_bytes())[\'sha256\']==digest(case[\'raw\'])\n    else:\n        with pytest.raises(snapshot.FormalSnapshotError):\n            producer._sample_production_a_boot_original_source(case[\'source\'],\n                guard=case[\'claim\'].assert_owned,deadline=case[\'deadline\'])\n    assert case[\'calls\']==[] and case[\'claim\'].events==[]\n'



@pytest.mark.parametrize('fault',['none','missing-file','extra-file','duplicate-path','invalid-path',
    'source-missing','read-failure','guard','closed-claim','deadline','boolean-deadline',
    'existing-inventory','future-database','fenced','binding-bytes','release-bytes','target-sha',
    'prefence-types','missing-boot-map','wrong-boot-record','changed-source-map',
    'changed-boot-map','preseal-drift','postseal-drift','late-guard','directory-drift','changed-fd'])
def test_production_a_inventory_author_seals_actual_originals(tmp_path,monkeypatch,request,fault):
    """Actual Windows files, explicit native directory/claim/peer protocols."""
    m=load()
    if os.name=='nt' and 'fcntl' not in sys.modules:
        unavailable=types.ModuleType('fcntl')
        unavailable.flock=lambda *a:(_ for _ in ()).throw(AssertionError('native flock outside wire contract'))
        monkeypatch.setitem(sys.modules,'fcntl',unavailable)
    import formal_boot_state as boot
    from contextlib import contextmanager
    root=tmp_path/'claim';root.mkdir()
    host_boot='0fa6c22b-5a90-11f1-ade1-0242ac150002'
    binding=dict(operationId='prod-inventory',sourceFingerprint='a'*64,databaseUuid=host_boot,minioContainerId='b'*64)
    class Claim:
        def __init__(self):self.root=root;self._plan=m.snapshot._canonical_bytes(binding);self.events=[];self.closed=False
        def assert_owned(self):
            if self.closed:raise m.FormalSnapshotError('TASK_TEST_CLAIM_CLOSED')
        def _write(self,path,raw):
            with Path(path).open('xb') as out:out.write(raw)
            Path(path).chmod(0o600)
    claim=Claim();sources={};boot_sources={}
    claim._lock=root/'maintenance.lock';claim._fd=os.open(claim._lock,os.O_RDWR|os.O_CREAT|os.O_EXCL,0o600)
    request.addfinalizer(lambda:os.close(claim._fd))
    for name in sorted(m.PRODUCTION_A_SOURCE_FILES):
        raw=m.snapshot._canonical_bytes(dict(explicitWireOriginal=name))
        if name=='binding.json':raw=claim._plan
        if name=='database-prefence-original.json':raw=m.snapshot._canonical_bytes(dict(serverUuid=host_boot,readOnly=0,superReadOnly=0))
        if name.startswith('boot-'):
            role=name[5:-14]
            source=dict(schemaVersion='release-formal-production-a-boot-source/v1',role=role,peer='local',
                machineIdSha256='c'*64,bootId=host_boot,path='/approved/boot/'+role+'.json')
            boot_sources[role]=source
            record=dict(schemaVersion='release-formal-production-a-boot-original/v1',
                **{k:source[k] for k in ('role','peer','machineIdSha256','bootId','path')},
                sourceParentIdentity=[[1,2,0,0,stat.S_IFDIR|0o755]],present=False,
                originalBase64=None,sha256=None,sourceFileIdentity=None)
            raw=m.snapshot._canonical_bytes(record)
        claim._write(root/name,raw);sources[name]='/originals/'+name
    release=dict(schemaVersion='release-formal-original-package-binding/v2',claimBindingSha256=hashlib.sha256(claim._plan).hexdigest(),
        approvedSourceTargetSha256=hashlib.sha256((root/'approved-source-target.json').read_bytes()).hexdigest(),
        deploymentAttemptId='1'*64,leaseTokenDigest='2'*64,scope=dict(environment='prod'),
        package=dict(releaseTag='inventory-wire',packageDigest='3'*64,manifestDigest='4'*64,planDigest='5'*64))
    release_raw=m.snapshot._canonical_bytes(release);claim._write(root/'original-release-binding.json',release_raw)
    target=root/'production-a-source-inventory.json';deadline=time.monotonic()+10
    if fault=='missing-file':sources.pop('host-originals.json')
    if fault=='extra-file':sources['extra.json']='/originals/extra.json'
    if fault=='duplicate-path':sources['host-originals.json']=sources['binding.json']
    if fault=='invalid-path':sources['host-originals.json']='/originals/../host-originals.json'
    if fault=='source-missing':(root/'host-originals.json').unlink()
    if fault=='closed-claim':claim.closed=True
    if fault=='deadline':deadline=time.monotonic()-1
    if fault=='boolean-deadline':deadline=True
    if fault=='existing-inventory':claim._write(target,b'RETAIN_EXISTING')
    if fault=='future-database':claim._write(root/'database-original.json',b'RETAIN_FUTURE')
    if fault=='fenced':claim.events=[dict(step='db-fence-intent',sequence=1,details={})]
    if fault=='binding-bytes':(root/'binding.json').write_bytes(b'CHANGED_BINDING')
    if fault=='release-bytes':(root/'original-release-binding.json').write_bytes(release_raw+b'\n')
    if fault=='target-sha':release['approvedSourceTargetSha256']='f'*64;release_raw=m.snapshot._canonical_bytes(release);(root/'original-release-binding.json').write_bytes(release_raw)
    if fault=='prefence-types':(root/'database-prefence-original.json').write_bytes(m.snapshot._canonical_bytes(dict(serverUuid=host_boot,readOnly=False,superReadOnly=0)))
    if fault=='missing-boot-map':boot_sources.pop('backend')
    if fault=='wrong-boot-record':
        path=root/'boot-backend-original.json';value=json.loads(path.read_bytes());value['path']='/other/boot.json';path.write_bytes(m.snapshot._canonical_bytes(value))
    reads=[];guards=[];directory_checks=[]
    def absolute(path):return root/Path(path).name
    @contextmanager
    def directory(path,*,no_atime=False):yield None,((root.stat().st_dev,root.stat().st_ino,0,0,stat.S_IFDIR|0o700),)
    def unchanged(path,chain):
        directory_checks.append(True)
        if fault=='directory-drift' and target.exists():raise m.FormalSnapshotError('TASK_TEST_DIRECTORY_CHANGED')
    def secure(path,mode,limit,*,no_atime):
        path=Path(path);reads.append(path.name)
        assert mode==0o600 and no_atime is True
        if fault=='read-failure' and path.name=='host-originals.json':raise OSError('TASK_TEST_READ_FAILURE')
        raw=path.read_bytes();assert 0<len(raw)<=limit
        return raw,boot._signature(path.stat())
    class BootPeerProtocol:
        def read_text(self,*,encoding):return host_boot
    monkeypatch.setattr(m,'Path',lambda path:BootPeerProtocol())
    monkeypatch.setattr(m,'require_native_runtime',lambda:None)
    monkeypatch.setattr(m,'FormalMaintenanceClaim',Claim)
    monkeypatch.setattr(m.os,'geteuid',lambda:0,raising=False)
    monkeypatch.setattr(m.snapshot,'observation_timeout',lambda seconds:seconds)
    monkeypatch.setattr(boot,'_absolute',absolute)
    monkeypatch.setattr(boot,'_directory',directory)
    monkeypatch.setattr(boot,'_directory_unchanged',unchanged)
    monkeypatch.setattr(boot,'_read_secure',secure)
    def guard():
        guards.append(True)
        if fault=='guard':raise m.FormalSnapshotError('TASK_TEST_GUARD_FAILED')
        if fault=='changed-source-map':sources['host-originals.json']='/other/original.json'
        if fault=='changed-boot-map':boot_sources['backend']['path']='/other/boot.json'
        if fault=='changed-fd':
            previous=claim._fd;claim._fd=os.dup(previous);os.close(previous)
        # At each pass boundary, mutate an already observed actual file.
        if fault=='preseal-drift' and reads and not target.exists():(root/'host-originals.json').write_bytes(b'CHANGED_PRESEAL')
        if fault=='postseal-drift' and target.exists():(root/'host-originals.json').write_bytes(b'CHANGED_POSTSEAL')
        if fault=='late-guard' and target.exists():raise m.FormalSnapshotError('TASK_TEST_LATE_GUARD_FAILED')
    if fault=='none':
        result=m.seal_production_a_source_inventory(claim,release_raw,sources,boot_sources,guard=guard,deadline=deadline)
        raw=target.read_bytes();value=json.loads(raw)
        assert result['path']==str(target) and result['sha256']==hashlib.sha256(raw).hexdigest()
        assert value['hostBootId']==host_boot and set(value['files'])==m.PRODUCTION_A_SOURCE_FILES
        for name,ref in value['files'].items():assert ref==dict(path=sources[name],sha256=hashlib.sha256((root/name).read_bytes()).hexdigest())
        assert result['binding']==m.production_a_resume_binding(release_raw,raw,result['sha256'])
        assert result['productionEvidence'] is False and result['approvalConsumed'] is False and claim.events==[]
        assert not (root/'database-original.json').exists() and len(guards)>=3
    else:
        with pytest.raises(m.FormalSnapshotError):
            m.seal_production_a_source_inventory(claim,release_raw,sources,boot_sources,guard=guard,deadline=deadline)
        retained=fault in ('existing-inventory','postseal-drift','late-guard','directory-drift')
        assert target.exists() is retained
        if fault=='existing-inventory':assert target.read_bytes()==b'RETAIN_EXISTING'
        if fault in ('postseal-drift','late-guard','directory-drift'):
            assert json.loads(target.read_bytes())['files']['host-originals.json']['sha256']==hashlib.sha256(m.snapshot._canonical_bytes(dict(explicitWireOriginal='host-originals.json'))).hexdigest()


def test_production_a_inventory_author_actual_unsupported_runtime_refuses_before_inputs():
    if sys.platform=='linux':pytest.skip('Requires actual Windows runtime')
    m=load()
    with pytest.raises(m.WatchdogError,match='WATCHDOG_RUNTIME_UNSUPPORTED'):
        m.seal_production_a_source_inventory(None,None,None,None,guard=None,deadline=None)

NATIVE_A_SOURCE_INVENTORY_TESTS = '"""Actual Linux files/FD/flock and inventory-to-A-consumer; source inputs fixtures.\n\nThe 23 private originals are explicit test inputs, not production observations.\nSigned authority uses a task-only key, not an actual administrator approval.\nNo daemon/SQL/service/SSH/boot transition/freeze/network/NAS.\n"""\nimport base64\nimport hashlib\nimport json\nimport os\nfrom pathlib import Path\nimport stat\nimport subprocess\nimport sys\nimport tempfile\nimport time\n\nimport pytest\n\nsys.path.insert(0,\'/workspace/linux\')\nimport formal_capture_producer as producer\nimport formal_snapshot_linux as snapshot\nimport formal_boot_state as boot\n\n\ndef digest(raw):return hashlib.sha256(raw).hexdigest()\n\n\ndef write(path,raw):\n    with path.open(\'xb\') as out:out.write(raw)\n    path.chmod(0o600)\n\n\n@pytest.fixture\ndef native_case():\n    assert sys.platform==\'linux\' and os.geteuid()==0,\'TASK_TEST_NATIVE_LINUX_ROOT_REQUIRED\'\n    producer.require_native_runtime()\n    assert stat.S_IMODE(Path(\'/control\').stat().st_mode)==0o700\n    token=snapshot.observation_deadline.set(time.monotonic()+20)\n    claim=None\n    try:\n        with tempfile.TemporaryDirectory(prefix=\'p2-inventory-\',dir=\'/control\') as directory:\n            root=Path(directory);originals=root/\'originals\';originals.mkdir(mode=0o700)\n            host_boot=Path(\'/proc/sys/kernel/random/boot_id\').read_text().strip()\n            binding=dict(operationId=\'prod-owned-inventory\',sourceFingerprint=\'a\'*64,\n                databaseUuid=\'0fa6c22b-5a90-11f1-ade1-0242ac150002\',minioContainerId=\'b\'*64)\n            claim=snapshot.FormalMaintenanceClaim(root/\'claim\',binding)\n            sources={};boot_sources={}\n            for name in sorted(producer.PRODUCTION_A_SOURCE_FILES):\n                path=claim.root/\'binding.json\' if name==\'binding.json\' else originals/name\n                raw=claim._plan if name==\'binding.json\' else snapshot._canonical_bytes(dict(explicitOriginalFixture=name))\n                if name==\'database-prefence-original.json\':\n                    raw=snapshot._canonical_bytes(dict(serverUuid=binding[\'databaseUuid\'],readOnly=0,superReadOnly=0))\n                if name.startswith(\'boot-\'):\n                    role=name[5:-14];parent=root/(\'boot-input-\'+role);parent.mkdir(mode=0o700)\n                    source=dict(schemaVersion=\'release-formal-production-a-boot-source/v1\',role=role,peer=\'local\',\n                        machineIdSha256=\'c\'*64,bootId=host_boot,path=str(parent/\'boot.json\'))\n                    boot_sources[role]=source\n                    with boot._directory(parent,no_atime=True) as (_,chain):\n                        record=dict(schemaVersion=\'release-formal-production-a-boot-original/v1\',\n                            **{k:source[k] for k in (\'role\',\'peer\',\'machineIdSha256\',\'bootId\',\'path\')},\n                            sourceParentIdentity=[list(row) for row in chain],present=False,\n                            originalBase64=None,sha256=None,sourceFileIdentity=None)\n                    raw=snapshot._canonical_bytes(record)\n                if name!=\'binding.json\':write(path,raw)\n                sources[name]=str(path)\n            release=dict(schemaVersion=\'release-formal-original-package-binding/v2\',claimBindingSha256=digest(claim._plan),\n                approvedSourceTargetSha256=digest(Path(sources[\'approved-source-target.json\']).read_bytes()),\n                deploymentAttemptId=\'1\'*64,leaseTokenDigest=\'2\'*64,scope=dict(environment=\'prod\'),\n                package=dict(releaseTag=\'owned-inventory\',packageDigest=\'3\'*64,manifestDigest=\'4\'*64,planDigest=\'5\'*64))\n            release_raw=snapshot._canonical_bytes(release);claim._write(claim.root/\'original-release-binding.json\',release_raw)\n            yield dict(root=root,claim=claim,binding=binding,sources=sources,boot_sources=boot_sources,\n                originals=originals,release_raw=release_raw,deadline=time.monotonic()+15)\n            claim.close();claim=None\n    finally:\n        if claim is not None:claim.close()\n        snapshot.observation_deadline.reset(token)\n\n\ndef seal(case,guard=None):\n    return producer.seal_production_a_source_inventory(case[\'claim\'],case[\'release_raw\'],case[\'sources\'],case[\'boot_sources\'],\n        guard=case[\'claim\'].assert_owned if guard is None else guard,deadline=case[\'deadline\'])\n\n\n@pytest.mark.parametrize(\'fault\',[\'none\',\'missing\',\'mode\',\'symlink\',\'hardlink\',\'parent-replaced\',\'inode-replaced\',\n    \'postseal-drift\',\'terminal-drift\',\'closed-claim\',\'deadline\',\'existing-target\',\'future-database\',\'changed-map\',\'wrong-device\'])\ndef test_native_inventory_preserves_complete_actual_private_files_and_identity(native_case,fault):\n    case=native_case;claim=case[\'claim\'];path=Path(case[\'sources\'][\'host-originals.json\'])\n    target=claim.root/\'production-a-source-inventory.json\';before=path.read_bytes();guards=[]\n    if fault==\'missing\':path.unlink()\n    if fault==\'mode\':path.chmod(0o666)\n    if fault in (\'symlink\',\'hardlink\'):\n        retained=case[\'root\']/\'retained-source.json\';path.rename(retained)\n        if fault==\'symlink\':path.symlink_to(retained)\n        else:os.link(retained,path)\n    if fault==\'deadline\':case[\'deadline\']=time.monotonic()-1\n    if fault==\'existing-target\':write(target,b\'RETAIN_EXISTING\')\n    if fault==\'future-database\':claim._write(claim.root/\'database-original.json\',b\'RETAIN_FUTURE\')\n    separate=None\n    if fault==\'wrong-device\':\n        separate=Path(\'/separate\')/(\'owned-\'+case[\'root\'].name+\'.json\');write(separate,before)\n        assert separate.stat().st_dev!=claim.root.stat().st_dev\n        case[\'sources\'][\'host-originals.json\']=str(separate)\n    def guard():\n        guards.append(True);claim.assert_owned()\n        if fault==\'closed-claim\' and len(guards)==2:claim.close()\n        if fault==\'changed-map\':case[\'sources\'][\'host-originals.json\']=str(case[\'root\']/\'other.json\')\n        if fault==\'inode-replaced\' and len(guards)==2:path.unlink();write(path,before)\n        if fault==\'parent-replaced\' and len(guards)==2:\n            retained=case[\'root\']/\'retained-originals\';case[\'originals\'].rename(retained)\n            case[\'originals\'].mkdir(mode=0o700)\n            for original in retained.iterdir():write(case[\'originals\']/original.name,original.read_bytes())\n        if fault==\'postseal-drift\' and target.exists():path.write_bytes(b\'CHANGED_POSTSEAL\')\n        if fault==\'terminal-drift\' and len(guards)==5:path.write_bytes(b\'CHANGED_AFTER_LAST_CONTENT_PASS\')\n    try:\n        if fault==\'none\':\n            signatures={name:boot._signature(Path(selected).stat()) for name,selected in case[\'sources\'].items()}\n            result=seal(case,guard);raw=target.read_bytes();inventory=json.loads(raw)\n            assert result[\'sha256\']==digest(raw) and result[\'identity\']==list(boot._signature(target.stat()))\n            assert stat.S_IMODE(target.stat().st_mode)==0o600 and len(inventory[\'files\'])==23\n            for name,ref in inventory[\'files\'].items():\n                assert ref==dict(path=case[\'sources\'][name],sha256=digest(Path(ref[\'path\']).read_bytes()))\n                assert boot._signature(Path(ref[\'path\']).stat())==signatures[name]\n            assert claim.events==[] and not (claim.root/\'database-original.json\').exists()\n            assert result[\'approvalConsumed\'] is False and result[\'productionEvidence\'] is False\n            with pytest.raises(snapshot.FormalSnapshotError,match=\'FORMAL_MAINTENANCE_BUSY\'):\n                snapshot.FormalMaintenanceClaim(claim.root,case[\'binding\'],resume=True)\n        else:\n            with pytest.raises(snapshot.FormalSnapshotError):seal(case,guard)\n            assert target.exists() is (fault in (\'existing-target\',\'postseal-drift\',\'terminal-drift\'))\n            if fault==\'existing-target\':assert target.read_bytes()==b\'RETAIN_EXISTING\'\n            if fault in (\'postseal-drift\',\'terminal-drift\'):\n                assert json.loads(target.read_bytes())[\'files\'][\'host-originals.json\'][\'sha256\']==digest(before)\n    finally:\n        if separate is not None:separate.unlink()\n\n\ndef openssl(*args):\n    result=subprocess.run([\'/usr/bin/openssl\',*args],capture_output=True,timeout=5)\n    assert result.returncode==0 and not result.stderr,\'TASK_TEST_OPENSSL_FAILED\'\n    return result.stdout\n\n\ndef test_native_authored_inventory_enters_existing_signed_consumer_with_same_owner(native_case):\n    case=native_case;claim=case[\'claim\'];root=case[\'root\'];fd=claim._fd\n    result=seal(case);source_raw=Path(result[\'path\']).read_bytes();expected=result[\'binding\']\n    registry=root/\'registry\';registry.mkdir(mode=0o700)\n    private=root/\'task-only-private.pem\'\n    openssl(\'genpkey\',\'-algorithm\',\'ED25519\',\'-out\',str(private));private.chmod(0o600)\n    key=root/\'task-only-public.der\';key.write_bytes(openssl(\'pkey\',\'-in\',str(private),\'-pubout\',\'-outform\',\'DER\'));key.chmod(0o644)\n    now=int(time.time())\n    payload=dict(expected,purpose=\'RESTORE_PRODUCTION_SAVED_ORIGINAL_STATE\',grantId=\'8\'*32,\n        actorId=17,tenantId=122,issuedAt=now-1,expiresAt=now+120,\n        policy=dict(originalStateOnly=True,preserveStoppedAndAbsent=True,preserveUnfinishedRecords=True,noBootStateWrites=True))\n    message=root/\'task-only-payload.json\';write(message,json.dumps(payload,separators=(\',\',\':\')).encode(\'ascii\'))\n    signature=openssl(\'pkeyutl\',\'-sign\',\'-inkey\',str(private),\'-rawin\',\'-in\',str(message))\n    approval=root/\'task-only-approval.json\'\n    envelope=dict(schemaVersion=\'formal-production-a-resume-approval/v1\',keySha256=digest(key.read_bytes()),\n        payloadBase64=base64.b64encode(message.read_bytes()).decode(),signatureBase64=base64.b64encode(signature).decode())\n    write(approval,json.dumps(envelope).encode(\'ascii\'));info=registry.stat()\n    resume=dict(registry=dict(path=str(registry),identity={k:getattr(info,\'st_\'+k) for k in (\'dev\',\'ino\',\'uid\',\'gid\',\'mode\')}),\n        approval=dict(path=str(approval),sha256=digest(approval.read_bytes())),trustedKey=dict(path=str(key),sha256=digest(key.read_bytes())))\n    checks=producer.ProductionAResumeChecks(claim,case[\'release_raw\'],source_raw,result[\'sha256\'],resume,\n        guard=claim.assert_owned,thaw_guard=claim.assert_owned)\n    checks();control=checks.control_phase_guard(maintenance_end=time.time()+90,deadline=time.monotonic()+10);control()\n    assert claim._fd==fd and checks.binding==expected\n    assert [event[\'step\'] for event in claim.events]==[\'production-a-resume-intent\',\'production-a-resume-consumed\']\n    assert checks.consumed[\'serviceActionsPerformed\'] is False and not (claim.root/\'database-original.json\').exists()\n    assert len(list(registry.iterdir()))==1\n    with pytest.raises(snapshot.FormalSnapshotError,match=\'FORMAL_MAINTENANCE_BUSY\'):\n        snapshot.FormalMaintenanceClaim(claim.root,case[\'binding\'],resume=True)\n'



@pytest.mark.parametrize('fault',['none','sql','mysql-stop','raw','thaw-after-capture',
    'live-owner-after-restore','mysql-start','flags','writer-start','critical-authority',
    'before-authority','wrong-resume','wrong-claim','wrong-actions','changed-request',
    'changed-flows','wrong-transition','prefence-drift','existing-intent','credential-bytes','credential-inode','wrong-binding-original','missing-binding-original'])
def test_production_a_shared_lifecycle_orders_real_phase_runners(tmp_path,monkeypatch,fault):
    """Real phase runners; explicit SQL/Docker/nft/claim/approval protocol actors.

    Guards fail if queried while their service is stopped or source is frozen.
    This Windows contract neither signs an actual grant nor performs service IO.
    """
    m=load()
    if os.name=='nt' and 'fcntl' not in sys.modules:
        unavailable=types.ModuleType('fcntl')
        unavailable.flock=lambda *a:(_ for _ in ()).throw(AssertionError('native flock outside protocol contract'))
        monkeypatch.setitem(sys.modules,'fcntl',unavailable)
    import formal_split_capture as split
    import formal_ingress_control as ingress
    import formal_external_callers as callers
    import formal_controlled_container_sources as controlled
    import formal_original_launch_capture as launch_capture
    import formal_selected_config_capture as config_capture
    request=shared_request(tmp_path,'capture-shared-payload')
    request['schemaVersion']='release-formal-capture-request/v12'
    source=dict(schemaVersion='release-formal-approved-source/v1',environment='prod',
        targetRawSha256='7'*64,targetFingerprint=request['claimBinding']['sourceFingerprint'],
        database=request['database'],databaseUuid=request['claimBinding']['databaseUuid'])
    request['sourceScope']=source
    credential=Path(request['mysqlCredentialFile']);credential.write_bytes(b'task-only-lifecycle-input\n')
    root=Path(request['claimRoot']);root.mkdir()
    shared_original={role:dict(image='sha256:'+hashlib.sha256(role.encode()).hexdigest()) for role in split.ROLES}
    shared_raw=m.snapshot._canonical_bytes(shared_original);health_raw=b'EXPLICIT_HEALTH_ORIGINAL'
    config_root=tmp_path/'original-source';(config_root/'config').mkdir(parents=True)
    config_originals={}
    for role,relative in config_capture.FILES.items():
        path=config_root/relative;raw=('explicit-original-'+role).encode();path.write_bytes(raw)
        info=path.stat();config_originals[role]=dict(path=str(path),bytes=len(raw),sha256=hashlib.sha256(raw).hexdigest(),
            fileSystemId=info.st_dev,inode=info.st_ino,mtimeNs=info.st_mtime_ns,ctimeNs=info.st_ctime_ns)
    target_raw=m.snapshot._canonical_bytes(dict(root='/task/config-root',explicitProtocol=True))
    source['targetRawSha256']=hashlib.sha256(target_raw).hexdigest()
    request['sharedScopeSha256']=hashlib.sha256(shared_raw).hexdigest()
    request['originalHealthSha256']=hashlib.sha256(health_raw).hexdigest()
    files={'shared-container-original.json':shared_raw,'shared-health-original.json':health_raw,
        'ingress-plan.json':b'{}','approved-source-target.json':target_raw,
        'original-configs.json':m.snapshot._canonical_bytes(config_originals)}
    for name,raw in files.items():(root/name).write_bytes(raw)
    state=dict(mysql_running=True,frozen=False,flags_restored=False,closed=False,clock=1.,wall=100.)
    calls=[];flows=[dict(source='192.0.2.1',destination='192.0.2.2',ports=[3306])]
    image_path=root/'payload'/'original-application-images.tar'
    config_copies=root/'original-configs';config_checks=[];config_identities={}
    class Claim:
        def __init__(self):
            self.root=root;self._plan=m.snapshot._canonical_bytes(request['claimBinding']);self.events=[];self._fd=37
        def assert_owned(self):
            assert not state['closed'];assert self._fd==37
        def _read(self,path):return Path(path).read_bytes()
        def record(self,step,details):
            assert step not in [e['step'] for e in self.events]
            self.events.append(dict(sequence=len(self.events)+1,step=step,details=details))
    claim=Claim()
    files['binding.json']=claim._plan
    (root/'binding.json').write_bytes(claim._plan)
    monkeypatch.setattr(m,'FormalMaintenanceClaim',Claim)
    import formal_boot_state as boot
    from contextlib import contextmanager
    # Explicit Windows secure-reader/parent protocol, actual bytes/inode only.
    # The native batch uses the unchanged real rooted secure reader.
    manifest_reads=set()
    def credential_secure(path,mode,limit,*,no_atime):
        if Path(path)==root/'original-release-binding.json' or Path(path).parent==config_copies:
            assert not any(c.startswith('start:') for c in calls) or state['mysql_running']
            manifest_reads.add(Path(path).relative_to(root).as_posix())
        return Path(path).read_bytes(),boot._signature(Path(path).stat())
    @contextmanager
    def credential_parent(path,*,no_atime):
        yield None,((str(path),Path(path).stat().st_ino),)
    monkeypatch.setattr(boot,'_read_secure',credential_secure)
    monkeypatch.setattr(boot,'_directory',credential_parent)
    monkeypatch.setattr(boot,'_directory_unchanged',lambda *a:None)
    def protocol_absolute(value):
        if value=='/task/config-root':return config_root
        if value.startswith('/explicit/'):return root/value.rsplit('/',1)[-1]
        return Path(value)
    monkeypatch.setattr(boot,'_absolute',protocol_absolute)
    actual_signature=boot._signature
    def protocol_signature(info):
        pair=list(actual_signature(info))
        # Explicit private-mode protocol actor; other actual inode/bytes remain.
        if stat.S_ISREG(pair[4]):pair[4]=stat.S_IFREG|0o600
        if fault=='image-mode' and image_path.exists() and info.st_ino==image_path.stat().st_ino:
            pair[4]=stat.S_IFREG|0o666
        return tuple(pair)
    monkeypatch.setattr(boot,'_signature',protocol_signature)
    monkeypatch.setattr(m,'require_native_runtime',lambda:None)
    monkeypatch.setattr(m.os,'geteuid',lambda:0,raising=False)
    monkeypatch.setattr(m.time,'monotonic',lambda:state['clock'])
    monkeypatch.setattr(m.time,'time',lambda:state['wall'])
    class ResumeProtocol:
        def __init__(self):
            self.claim=claim;self.consumed=dict(issuedAt=90,expiresAt=500)
            release=dict(schemaVersion='release-formal-original-package-binding/v2',
                claimBindingSha256=hashlib.sha256(claim._plan).hexdigest(),approvedSourceTargetSha256=source['targetRawSha256'],
                deploymentAttemptId='1'*64,leaseTokenDigest='2'*64,scope=dict(environment='prod'),
                package=dict(releaseTag='task-only-contract',packageDigest='3'*64,manifestDigest='4'*64,planDigest='5'*64))
            inventory=dict(schemaVersion='release-formal-production-a-original-sources/v1',
                claimBindingSha256=release['claimBindingSha256'],approvedSourceTargetSha256=source['targetRawSha256'],
                hostBootId=request['filesystemFence']['binding']['bootId'],
                files={name:dict(path='/explicit/'+name,sha256=hashlib.sha256(files[name]).hexdigest()
                    if name in files else '8'*64) for name in m.PRODUCTION_A_SOURCE_FILES})
            inventory_raw=m.snapshot._canonical_bytes(inventory)
            (root/'production-a-source-inventory.json').write_bytes(inventory_raw)
            # The actual pure author returns the same shape consumed by A.
            # It has no claimBindingSha256 field; private binding remains pinned.
            self.binding=m.production_a_resume_binding(m.snapshot._canonical_bytes(release),
                inventory_raw,hashlib.sha256(inventory_raw).hexdigest())
            files['original-release-binding.json']=m.snapshot._canonical_bytes(release)
            (root/'original-release-binding.json').write_bytes(files['original-release-binding.json'])
            self.pins={root/name:(0o600,16777216,(raw,boot._signature((root/name).stat()))) for name,raw in files.items()}
        def __call__(self):
            assert state['mysql_running'] and not state['frozen'],'SQL owner queried while stopped/frozen'
            calls.append('live-owner')
            if fault=='before-authority' or fault=='live-owner-after-restore' and state['flags_restored']:
                raise m.FormalSnapshotError('TASK_TEST_LIVE_OWNER_REFUSED')
        def control_phase_guard(self,*,maintenance_end,deadline):
            assert maintenance_end==400 and deadline==121
            def current():
                claim.assert_owned();calls.append('control')
                if fault=='critical-authority' and not state['mysql_running']:
                    raise m.FormalSnapshotError('TASK_TEST_CRITICAL_AUTHORITY_REFUSED')
                if fault=='prefence-drift' and any(e['step']=='db-fence-intent' for e in claim.events):
                    raise m.FormalSnapshotError('FORMAL_PRODUCTION_A_DATABASE_ORIGINAL_CHANGED')
            current();return current
    checks=ResumeProtocol();monkeypatch.setattr(m,'ProductionAResumeChecks',ResumeProtocol)
    if fault=='missing-inventory':(root/'production-a-source-inventory.json').unlink()
    if fault=='inventory-bytes':(root/'production-a-source-inventory.json').write_bytes(b'CHANGED_INVENTORY')
    if fault in ('config-not-approved','target-not-approved'):
        (root/('original-configs.json' if fault=='config-not-approved' else 'approved-source-target.json')).write_bytes(b'UNAPPROVED')
    def config_capture_actor(selected_root,selected_originals,destination,*,guard):
        guard();assert selected_root==config_root and selected_originals==config_originals
        assert not any(c.startswith('stop:') for c in calls);calls.append('configs')
        if fault=='config-capture':raise m.FormalSnapshotError('TASK_TEST_CONFIG_CAPTURE_REFUSED')
        destination.mkdir()
        for role,relative in config_capture.FILES.items():
            original=config_root/relative;copied=destination/(role+'.bin');copied.write_bytes(original.read_bytes())
            config_identities[original]=(original.read_bytes(),actual_signature(original.stat()))
            config_identities[copied]=(copied.read_bytes(),actual_signature(copied.stat()))
        result=dict(state='SELECTED_CONFIG_ORIGINALS_CAPTURED_PENDING_FULL_PROTECTION',productionEvidence=False)
        receipt=destination/'config-capture-stage.json';receipt.write_bytes(m.snapshot._canonical_bytes(result))
        config_identities[receipt]=(receipt.read_bytes(),actual_signature(receipt.stat()))
        return result
    def config_readback_actor(selected_root,selected_originals,destination,result,*,guard):
        guard();assert selected_root==config_root and selected_originals==config_originals and destination==config_copies
        config_checks.append(state['flags_restored']);calls.append('config-readback')
        if fault=='config-before-restore' and image_path.exists() or fault=='config-after-restore' and state['flags_restored']:
            raise m.FormalSnapshotError('TASK_TEST_CONFIG_READBACK_REFUSED')
        def identity():
            for path,pair in config_identities.items():
                m.require((path.read_bytes(),actual_signature(path.stat()))==pair,'TASK_TEST_CONFIG_IDENTITY_REFUSED')
        identity();return dict(configReceiptSha256=hashlib.sha256(m.snapshot._canonical_bytes(result)).hexdigest(),productionEvidence=False),identity
    monkeypatch.setattr(config_capture,'capture_selected_config_originals',config_capture_actor)
    monkeypatch.setattr(config_capture,'verify_selected_config_originals',config_readback_actor)
    def images_actor(selected,destination,*,guard,timeout):
        guard();assert selected is claim and destination==image_path
        assert 'raw' in calls and not state['mysql_running'] and not state['frozen']
        assert not any(c.startswith('start:') for c in calls);calls.append('images')
        if fault=='images-export':raise m.FormalSnapshotError('TASK_TEST_IMAGE_EXPORT_REFUSED')
        raw=b'EXPLICIT_DOCKER_IMAGE_EXPORT_PROTOCOL';destination.parent.mkdir(exist_ok=True);destination.write_bytes(raw)
        images={role:shared_original[role]['image'] for role in ('backend','frontend')}
        result=dict(sha256=hashlib.sha256(raw).hexdigest(),bytes=len(raw),images=images,
            sharedScopeSha256=request['sharedScopeSha256'],productionEvidence=False,hostWritersVerified=False)
        if fault=='image-digest':result['sha256']='0'*64
        if fault=='image-size':result['bytes']+=1
        if fault=='image-original':result['images']=dict(images,backend='sha256:'+'0'*64)
        if fault=='image-scope':result['sharedScopeSha256']='0'*64
        if fault.startswith('payload-export-'):
            mutate_payload(fault[len('payload-export-'):])
        guard();return result
    monkeypatch.setattr(launch_capture,'capture_original_application_images',images_actor)
    # Actual pure adapter constructor and its existing byte encoder. Only
    # service actions elsewhere in this contract are explicit protocol actors.
    originals={role:dict(path='/explicit/'+role+'-inspect.json',
        source=dict(present=True,containerId=hashlib.sha256(role.encode()).hexdigest()),
        unicodeSentinel='\u539f\u4ef6') for role in controlled.ControlledSharedContainerActions.ROLES}
    actions=controlled.ControlledSharedContainerActions(originals,object())
    monkeypatch.setattr(m,'read_credential',lambda path:'task-only-lifecycle-input')
    class MysqlProtocol:
        def __init__(self,container,*a,**k):self.container=container
    monkeypatch.setattr(m,'PrivateDockerMySql',MysqlProtocol)
    def source_guard():
        assert not state['frozen'],'source observer called while frozen'
        assert not state.get('source_phase_finished'),'closed-source observer reused after restoration'
        calls.append('source')
        if fault=='changed-request' and any(e['step']=='db-fenced' for e in claim.events):request['database']='changed'
        if fault=='changed-flows' and not state['mysql_running']:flows[0]['ports']=[9999]
    def thaw(selected):
        assert selected is claim and not state['frozen'],'thaw observer called while frozen'
        if fault=='thaw-after-capture' and any(e['step']=='split-captured' for e in claim.events):
            raise m.FormalSnapshotError('FORMAL_SHARED_THAW_REQUIRED')
        return dict(nativeState='THAWED_COPY_ACCEPTED_PENDING_FORMAL_BARRIERS')
    monkeypatch.setattr(split,'_thaw_receipt',thaw)
    ingress_state=dict(phase='CAPTURE_RESTRICTED',rulesSha256=request['ingressRulesSha256'],tableHandle=17)
    monkeypatch.setattr(ingress,'assert_ingress',lambda *a,**k:dict(ingress_state))
    monkeypatch.setattr(ingress,'render_recovery_policy',lambda *a:None)
    monkeypatch.setattr(callers,'guard_external_callers',lambda *a:dict(state='KNOWN_EXTERNAL_CALLERS_STOPPED_PENDING_FULL_EXCLUSION'))
    monkeypatch.setattr(m,'verify_shared_request_binding',lambda *a:dict(shared=shared_raw,health=health_raw))
    monkeypatch.setattr(m,'verify_shared_database_before_stop',lambda *a:None)
    def stop_writer(selected,role,**kwargs):
        kwargs['guard']();calls.append('stop:'+role)
        if role==split.STOP_ORDER[0] and fault in ('credential-bytes','credential-inode'):
            original=credential.read_bytes()
            if fault=='credential-inode':credential.rename(credential.with_suffix('.retained'))
            credential.write_bytes(original if fault=='credential-inode' else b'CHANGED_PRIVATE_INPUT\n')
        claim.record('shared-'+role+'-stop-intent',dict(explicitProtocol=True))
        claim.record('shared-'+role+'-stopped',dict(explicitProtocol=True))
        if role==split.STOP_ORDER[0] and fault in ('config-source-after-stop','config-copy-after-stop'):
            if fault=='config-source-after-stop':(config_root/config_capture.FILES['data-env']).write_bytes(b'CHANGED_SOURCE')
            elif config_copies.exists():(config_copies/'data-env.bin').write_bytes(b'CHANGED_COPY')
        kwargs['guard']()
    monkeypatch.setattr(split,'stop_controlled_shared_writer',stop_writer)
    monkeypatch.setattr(split,'assert_controlled_prefence_stops',lambda *a,**k:k['guard']())
    def fence(selected,mysql,*,guard):
        guard();calls.append('fence');claim.record('db-fence-intent',dict(explicitProtocol=True))
        guard();claim.record('db-fenced',dict(explicitProtocol=True));guard()
    monkeypatch.setattr(m.snapshot,'acquire_claimed_database_fence',fence)
    payload=root/'payload';raw_root=payload/'minio-physical'
    proof_names=('006-database-rejection-before.json','010-sql-started.json','011-source-data.json',
        '012-source-data-after.json','013-source-migration-ledger.json','014-source-migration-ledger-after.json',
        '015-source-data-binding.json','016-source-migration-binding.json','018-database-rejection-after.json')
    proof_originals={}
    def proof_readback(names,code):
        m.require(all((payload/name).read_bytes()==proof_originals[name] for name in names),code)
        return {name:proof_originals[name] for name in names}
    def data_readback(selected,plan,sha,reference):
        assert selected==payload and plan==claim._plan
        assert reference['bindingSha256']==hashlib.sha256(proof_originals['015-source-data-binding.json']).hexdigest()
        return dict(explicitSourceData=True),proof_readback(
            ('011-source-data.json','012-source-data-after.json','015-source-data-binding.json'),'FORMAL_DATA_SOURCE_CHANGED')
    def ledger_readback(selected,plan,sha,reference,data,scope):
        assert selected==payload and plan==claim._plan and scope==source and data==dict(explicitSourceData=True)
        assert reference['bindingSha256']==hashlib.sha256(proof_originals['016-source-migration-binding.json']).hexdigest()
        return {},proof_readback(('013-source-migration-ledger.json','014-source-migration-ledger-after.json',
            '016-source-migration-binding.json'),'FORMAL_SOURCE_LEDGER_SOURCE_CHANGED')
    def rejection_readback(selected,receipt):
        assert selected is claim
        for phase,number in (('before','006'),('after','018')):
            name=number+'-database-rejection-'+phase+'.json'
            assert receipt['databaseRejectionEvidence'][phase+'Sha256']==hashlib.sha256(proof_originals[name]).hexdigest()
        proof_readback(('006-database-rejection-before.json','018-database-rejection-after.json'),'FORMAL_SPLIT_REJECTION_CHANGED')
    # Source-observation semantics remain explicit protocol actors; their actual
    # retained file bytes and SQL-reference digests cannot change in this seam.
    monkeypatch.setattr(split,'read_source_data',data_readback)
    monkeypatch.setattr(split,'read_source_ledger',ledger_readback)
    monkeypatch.setattr(split,'_read_rejections',rejection_readback)
    def mutate_payload(kind):
        paths={'sql':payload/'database.sql.gz','raw':raw_root/'objects'/'object.bin',
            'proof':payload/'015-source-data-binding.json','sql-receipt':payload/'020-sql.json',
            'raw-receipt':payload/'040-payload.json'}
        if kind.endswith('-inode'):
            path=paths[kind[:-6]];original=path.read_bytes()
            path.rename(tmp_path/('retained-'+path.name));path.write_bytes(original)
        elif kind=='child':(raw_root/'empty'/'unbound.bin').write_bytes(b'UNBOUND')
        elif kind=='parent':
            import shutil
            retained=tmp_path/'retained-raw';raw_root.rename(retained);shutil.copytree(retained,raw_root)
        else:paths[kind].write_bytes(b'CAPTURED_PAYLOAD_DRIFT')
    def sql(*a,**kwargs):
        kwargs['guard']();calls.append('sql')
        if fault=='sql':raise m.FormalSnapshotError('TASK_TEST_SQL_REFUSED')
        payload.mkdir()
        for name in proof_names:
            (payload/name).write_bytes(m.snapshot._canonical_bytes(dict(explicitCaptureProof=name)))
        (payload/'010-sql-started.json').write_bytes(m.snapshot._canonical_bytes(
            dict(state='SQL_CAPTURING',sharedScopeSha256=request['sharedScopeSha256'])))
        proof_originals.update({name:(payload/name).read_bytes() for name in proof_names})
        import gzip
        sql_bytes=gzip.compress(b'-- actual local SQL fixture\nSELECT 1;\n',mtime=0)
        (payload/'database.sql.gz').write_bytes(sql_bytes)
        result=dict(state='SQL_CAPTURED_PENDING_RAW_AND_FULL_BARRIERS',sourceScope=source,sourceMigrationEvidence={},
            database=dict(sha256=hashlib.sha256(sql_bytes).hexdigest(),bytes=len(sql_bytes)))
        result.update(sourceDataEvidence=dict(bindingSha256=hashlib.sha256(proof_originals['015-source-data-binding.json']).hexdigest()),
            sourceMigrationEvidence=dict(bindingSha256=hashlib.sha256(proof_originals['016-source-migration-binding.json']).hexdigest()),
            databaseRejectionEvidence={phase+'Sha256':hashlib.sha256(proof_originals[number+'-database-rejection-'+phase+'.json']).hexdigest()
                for phase,number in (('before','006'),('after','018'))})
        if fault=='payload-sql-digest':result['database']['sha256']='0'*64
        receipt=m.snapshot._canonical_bytes(result);(payload/'020-sql.json').write_bytes(receipt)
        claim.record('split-sql-intent',{})
        claim.record('split-sql-captured',dict(sqlReceiptSha256=hashlib.sha256(receipt).hexdigest(),
            sharedScopeSha256=request['sharedScopeSha256']))
        return result
    monkeypatch.setattr(split,'capture_sql_phase',sql)
    def stop_mysql(selected,role,**kwargs):
        assert role=='mysql';kwargs['guard']()
        claim.record('shared-mysql-stop-intent',{})
        kwargs['guard']();calls.append('stop:mysql');state['mysql_running']=False
        kwargs['guard']()
        if fault=='mysql-stop':raise m.FormalSnapshotError('TASK_TEST_MYSQL_STOP_UNCERTAIN')
        claim.record('shared-mysql-stopped',{})
    monkeypatch.setattr(split,'stop_controlled_capture_database',stop_mysql)
    def raw(*a,**kwargs):
        kwargs['guard']();calls.append('raw');state['frozen']=True
        kwargs['native_guard']();assert not state['mysql_running'];state['frozen']=False
        if fault=='raw':raise m.FormalSnapshotError('TASK_TEST_RAW_REFUSED')
        (raw_root/'objects').mkdir(parents=True);(raw_root/'empty').mkdir()
        raw_bytes=b'ACTUAL_LOCAL_RAW_CAPTURE';(raw_root/'objects'/'object.bin').write_bytes(raw_bytes)
        digest=hashlib.sha256()
        for row in (['directory','empty'],['directory','objects'],
            ['objects/object.bin','sha256:'+hashlib.sha256(raw_bytes).hexdigest(),len(raw_bytes)]):
            digest.update((json.dumps(row,ensure_ascii=False,separators=(',',':'))+'\n').encode())
        physical=dict(physicalTreeDigest=digest.hexdigest(),fileCount=1,totalBytes=len(raw_bytes),hostWritersVerified=False)
        if fault=='payload-raw-digest':physical['physicalTreeDigest']='0'*64
        sql_result=json.loads((payload/'020-sql.json').read_bytes())
        result=dict(state='CAPTURED_PENDING_FULL_BARRIER_PROOF',sourceScope=source,
            sourceDataEvidence=sql_result['sourceDataEvidence'],sourceMigrationEvidence=sql_result['sourceMigrationEvidence'],
            database=sql_result['database'],physical=physical,
            splitSqlReceiptSha256=hashlib.sha256(m.snapshot._canonical_bytes(sql_result)).hexdigest())
        receipt=m.snapshot._canonical_bytes(result);(payload/'040-payload.json').write_bytes(receipt)
        claim.record('split-raw-intent',{})
        claim.record('split-captured',dict(payloadReceiptSha256=hashlib.sha256(receipt).hexdigest(),
            state='CAPTURED_PENDING_FULL_BARRIER_PROOF'))
        if fault=='payload-journal':claim.events[-1]['details']['payloadReceiptSha256']='0'*64
        if fault=='payload-missing-proof':(payload/'015-source-data-binding.json').unlink()
        if fault=='payload-sql-receipt':mutate_payload('sql-receipt')
        if fault.startswith('payload-boundary-'):
            name={'payload-boundary-data':'011-source-data.json','payload-boundary-ledger':'013-source-migration-ledger.json',
                'payload-boundary-rejection':'006-database-rejection-before.json','payload-boundary-sql-start':'010-sql-started.json'}[fault]
            (payload/name).write_bytes(b'PROOF_CHANGED_BEFORE_EPOCH_RETENTION')
        kwargs['guard']()
        return result
    monkeypatch.setattr(split,'capture_raw_phase',raw)
    def transition(selected,value,*,guard,thaw_guard):
        if fault=='handoff-manifest-none':
            # The original capture manifest must exist before permitting any
            # service restart, not be reconstructed after recovery.
            assert claim._fd==37 and not any(c.startswith('start:') for c in calls)
            assert {'original-release-binding.json','original-configs/config-capture-stage.json',
                *('original-configs/'+role+'.bin' for role in config_capture.FILES)}<=manifest_reads
        if image_path.exists() and fault in ('image-inode','image-before-restore'):
            raw=image_path.read_bytes()
            if fault=='image-inode':image_path.rename(image_path.with_suffix('.retained'));image_path.write_bytes(raw)
            else:image_path.write_bytes(b'CHANGED_IMAGE_BEFORE_START')
        guard();thaw_guard();calls.append('transition')
        claim.record('ingress-recovery-intent',{})
        rules_raw=m.snapshot._canonical_bytes([dict(table=dict(handle=18))])
        (root/'ingress-recovery-installed.json').write_bytes(rules_raw)
        ingress_state.update(phase='RECOVERY_DEPENDENCIES_PERMITTED',tableHandle=18,
            rulesSha256=hashlib.sha256(rules_raw).hexdigest())
        claim.record('ingress-recovery-permitted',dict(rulesSha256=ingress_state['rulesSha256'],
            planSha256=hashlib.sha256(m.snapshot._canonical_bytes(value)).hexdigest()))
        guard();thaw_guard()
        return dict(ingress_state,phase='WRONG' if fault=='wrong-transition' else ingress_state['phase'])
    monkeypatch.setattr(ingress,'permit_recovery_dependencies',transition)
    def begin(selected,*,guard,thaw_guard):
        guard();thaw_guard();claim.record('shared-recovery-intent',{})
    monkeypatch.setattr(split,'begin_shared_recovery',begin)
    def start(selected,role,action,**kwargs):
        assert action=='start';kwargs['guard']();calls.append('start:'+role)
        if fault=='mysql-start' and role=='mysql' or fault=='writer-start' and role=='redis':
            claim.record('shared-'+role+'-start-intent',{})
            raise m.FormalSnapshotError('TASK_TEST_START_UNCERTAIN')
        if role=='mysql':state['mysql_running']=True
        else:assert state['flags_restored'] and calls[calls.index('flags')+1:].count('live-owner')>0
        claim.record('shared-'+role+'-restored',{})
        kwargs['guard']()
    monkeypatch.setattr(split,'_shared_action',start)
    def flags(selected,mysql,*,guard,thaw_guard):
        guard();calls.append('flags')
        if fault=='flags':raise m.FormalSnapshotError('TASK_TEST_FLAGS_UNCERTAIN')
        state['flags_restored']=True;claim.record('shared-db-restored',{})
        if fault=='image-after-restore' and image_path.exists():image_path.write_bytes(b'CHANGED_IMAGE_AFTER_FLAGS')
        guard()
    monkeypatch.setattr(split,'restore_shared_database_flags',flags)
    monkeypatch.setattr(split,'verify_shared_restoration',lambda *a,**k:dict(state='ORIGINAL_SERVICES_RESTORED_PENDING_CALLER_PROOF'))
    if fault=='wrong-resume':checks=object()
    if fault=='wrong-claim':checks.claim=object()
    if fault=='wrong-actions':actions=object()
    if fault=='existing-intent':claim.record('shared-backend-stop-intent',{})
    if fault=='wrong-binding-original':checks.pins[root/'binding.json']=(0o600,16384,(b'CHANGED',()))
    if fault=='missing-binding-original':del checks.pins[root/'binding.json']
    call=lambda:m.run_production_a_shared_lifecycle(claim,request,approved_source=source,
        resume_checks=checks,container_actions=actions,source_guard=source_guard,
        maintenance_end=400,recovery_flows=flows)
    original_faults={'missing-inventory','inventory-bytes','config-not-approved','target-not-approved','config-capture',
        'config-before-restore','images-export','image-digest','image-size','image-original','image-scope','image-mode',
        'image-inode','image-before-restore','config-after-restore','image-after-restore','config-source-after-stop',
        'config-copy-after-stop'}
    if fault in ('none','original-content') or fault.startswith('handoff-'):
        bundle=call()
        assert type(bundle) is tuple and len(bundle)==5,'original capture epoch handoff required'
        result,content_guard,retained_thaw,mysql,deadline=bundle
        assert callable(content_guard) and callable(retained_thaw) and type(mysql) is MysqlProtocol and deadline==121
        expected=['configs']+['stop:'+r for r in split.STOP_ORDER[:-1]]+['fence','sql','stop:mysql','raw','images','transition']+[
            'start:mysql','flags']+['start:'+r for r in split.RESTORE_ORDER[1:]]
        assert [c for c in calls if c.startswith(('stop:','start:')) or c in ('configs','fence','sql','raw','images','transition','flags')]==expected
        assert len(config_checks)>=3 and config_checks[0] is False and config_checks[-1] is True
        assert result['originalImages']['images']=={role:shared_original[role]['image'] for role in ('backend','frontend')}
        assert result['originalConfigs']['configReceiptSha256']==hashlib.sha256((config_copies/'config-capture-stage.json').read_bytes()).hexdigest()
        assert result['state']=='A_SHARED_CAPTURED_AND_ORIGINAL_SERVICES_RESTORED_PENDING_REMAINING_OWNER'
        assert result['claimReleased'] is False and result['fullRunnerCompleted'] is False and result['productionEvidence'] is False
        assert claim._fd==37 and not state['closed']
        if fault.startswith('handoff-'):
            # The closure must retain its old identities and deadline, never
            # reconstruct an epoch after services and ingress change phase.
            state['source_phase_finished']=True
            ingress_state.update(phase='RELEASED',present=False)
            before=(calls.count('source'),calls.count('live-owner'))
            original_context=m.snapshot.observation_deadline.get()
            event_count=len(claim.events)
            if fault=='handoff-config-source':
                (config_root/config_capture.FILES['data-env']).write_bytes(b'AFTER_RETURN_SOURCE_DRIFT')
            if fault=='handoff-config-copy':
                (config_copies/'data-env.bin').write_bytes(b'AFTER_RETURN_COPY_DRIFT')
            if fault=='handoff-image-bytes':image_path.write_bytes(b'AFTER_RETURN_IMAGE_DRIFT')
            if fault=='handoff-image-inode':
                raw=image_path.read_bytes();image_path.rename(image_path.with_suffix('.retained'));image_path.write_bytes(raw)
            if fault=='handoff-image-receipt':
                (image_path.parent/'050-original-images.json').write_bytes(b'AFTER_RETURN_RECEIPT_DRIFT')
            if fault=='handoff-inventory':
                (root/'production-a-source-inventory.json').write_bytes(b'AFTER_RETURN_INVENTORY_DRIFT')
            if fault=='handoff-credential':credential.write_bytes(b'AFTER_RETURN_CREDENTIAL_DRIFT')
            if fault=='handoff-deadline':state['clock']=122
            if fault=='handoff-manifest-release':(root/'original-release-binding.json').write_bytes(b'RELEASE_DRIFT')
            if fault=='handoff-manifest-release-inode':
                path=root/'original-release-binding.json';original=path.read_bytes()
                path.rename(tmp_path/'retained-release');path.write_bytes(original)
            if fault=='handoff-manifest-entries':result['capturedArchiveEntries']['payload/database.sql.gz']='file|0|'+'0'*64
            if fault=='handoff-manifest-replaced':result['capturedArchiveEntries']={}
            if fault.startswith('handoff-payload-') and fault!='handoff-payload-none':
                if fault=='handoff-payload-object':result['capture']['payload']['physical']['totalBytes']+=1
                else:mutate_payload(fault[len('handoff-payload-'):])
            expected={
                'handoff-config-source':'TASK_TEST_CONFIG_IDENTITY_REFUSED',
                'handoff-config-copy':'TASK_TEST_CONFIG_IDENTITY_REFUSED',
                'handoff-image-bytes':'FORMAL_PRODUCTION_A_ORIGINAL_IMAGE_CHANGED',
                'handoff-image-inode':'FORMAL_PRODUCTION_A_ORIGINAL_IMAGE_CHANGED',
                'handoff-image-receipt':'FORMAL_PRODUCTION_A_ORIGINAL_IMAGE_CHANGED',
                'handoff-inventory':'FORMAL_PRODUCTION_A_ORIGINAL_INVENTORY_CHANGED',
                'handoff-credential':'FORMAL_PRODUCTION_A_SHARED_CREDENTIAL_CHANGED',
                'handoff-deadline':'FORMAL_CAPTURE_DEADLINE_EXCEEDED',
            }
            if fault.startswith('handoff-payload-') and fault!='handoff-payload-none':
                expected[fault]='FORMAL_PRODUCTION_A_CAPTURED_PAYLOAD_CHANGED'
            if fault.startswith('handoff-manifest-') and fault!='handoff-manifest-none':
                expected[fault]='FORMAL_PRODUCTION_A_CAPTURE_ARCHIVE_CHANGED'
            if fault in expected:
                with pytest.raises(m.FormalSnapshotError,match='^'+expected[fault]+'$'):content_guard()
            elif fault=='handoff-earlier-outer':
                outer_token=m.snapshot.observation_deadline.set(0.5)
                try:
                    with pytest.raises(m.FormalSnapshotError,match='^FORMAL_CAPTURE_DEADLINE_EXCEEDED$'):content_guard()
                    assert m.snapshot.observation_deadline.get()==0.5
                finally:m.snapshot.observation_deadline.reset(outer_token)
            else:
                assert fault in ('handoff-none','handoff-payload-none','handoff-manifest-none')
                content_guard();retained_thaw();content_guard()
                if fault=='handoff-manifest-none':
                    actual={'original-release-binding.json':'file|'+str((root/'original-release-binding.json').stat().st_size)+'|'+hashlib.sha256((root/'original-release-binding.json').read_bytes()).hexdigest()}
                    for selected in (payload,config_copies):
                        for path in [selected,*selected.rglob('*')]:
                            name=path.relative_to(root).as_posix()
                            actual[name]='directory' if path.is_dir() else 'file|'+str(path.stat().st_size)+'|'+hashlib.sha256(path.read_bytes()).hexdigest()
                    assert result['capturedArchiveEntries']==actual
                if fault=='handoff-payload-none':
                    entries=result['capturedPayloadEntries'];actual={}
                    for path in payload.rglob('*'):
                        if path.name in ('original-application-images.tar','050-original-images.json'):continue
                        name=path.relative_to(root).as_posix()
                        actual[name]='directory' if path.is_dir() else 'file|'+str(path.stat().st_size)+'|'+hashlib.sha256(path.read_bytes()).hexdigest()
                    assert entries==dict(actual,payload='directory')
                    assert entries['payload/minio-physical/empty']=='directory'
            assert m.snapshot.observation_deadline.get()==original_context
            assert (calls.count('source'),calls.count('live-owner'))==before
            assert len(claim.events)==event_count and claim._fd==37 and not state['closed']
    else:
        if fault.startswith('payload-'):
            expected='FORMAL_PHYSICAL_CAPTURE_CHANGED' if fault=='payload-raw-digest' else 'FORMAL_PRODUCTION_A_CAPTURED_PAYLOAD_CHANGED'
            expected={'payload-boundary-data':'FORMAL_DATA_SOURCE_CHANGED',
                'payload-boundary-ledger':'FORMAL_SOURCE_LEDGER_SOURCE_CHANGED',
                'payload-boundary-rejection':'FORMAL_SPLIT_REJECTION_CHANGED'}.get(fault,expected)
            with pytest.raises(m.FormalSnapshotError,match='^'+expected+'$'):call()
        else:
            with pytest.raises(m.FormalSnapshotError):call()
        assert claim._fd==37 and not state['closed']
        if fault in ('before-authority','wrong-resume','wrong-claim','wrong-actions','existing-intent'):
            assert not any(c.startswith(('stop:','start:')) for c in calls)
        if fault in ('sql','mysql-stop','raw','thaw-after-capture','critical-authority','changed-request','changed-flows','prefence-drift','credential-bytes','credential-inode'):
            assert not any(c.startswith('start:') for c in calls)
        if fault in original_faults-{'config-after-restore','image-after-restore'}:
            assert not any(c.startswith('start:') for c in calls)
        if fault.startswith('payload-'):assert not any(c.startswith('start:') for c in calls)
        if fault in ('missing-inventory','inventory-bytes','config-not-approved','target-not-approved','config-capture'):
            assert not any(c.startswith('stop:') for c in calls)
        if fault in ('live-owner-after-restore','mysql-start','flags','wrong-transition'):
            assert not any(c.startswith('start:') and c!='start:mysql' for c in calls)


@pytest.mark.parametrize('fault',['original-content','missing-inventory','inventory-bytes','config-not-approved',
    'target-not-approved','config-capture','config-before-restore','images-export','image-digest','image-size',
    'image-original','image-scope','image-mode','image-inode','image-before-restore','config-after-restore',
    'image-after-restore','config-source-after-stop','config-copy-after-stop'])
def test_production_a_original_capture_guards_precede_service_restart(tmp_path,monkeypatch,fault):
    test_production_a_shared_lifecycle_orders_real_phase_runners(tmp_path,monkeypatch,fault)


@pytest.mark.parametrize('fault',['handoff-none','handoff-config-source','handoff-config-copy',
    'handoff-image-bytes','handoff-image-inode','handoff-image-receipt','handoff-inventory',
    'handoff-credential','handoff-deadline','handoff-earlier-outer'])
def test_production_a_shared_capture_epoch_handoff(tmp_path,monkeypatch,fault):
    test_production_a_shared_lifecycle_orders_real_phase_runners(tmp_path,monkeypatch,fault)


@pytest.mark.parametrize('fault',['handoff-payload-none','payload-sql-digest','payload-raw-digest',
    'payload-journal','payload-missing-proof','payload-sql-receipt','payload-export-sql-inode',
    'payload-export-raw-inode','payload-export-proof','payload-export-child','payload-export-parent',
    'handoff-payload-sql','handoff-payload-sql-inode','handoff-payload-raw','handoff-payload-raw-inode',
    'handoff-payload-proof','handoff-payload-sql-receipt','handoff-payload-raw-receipt',
    'handoff-payload-child','handoff-payload-parent','handoff-payload-object'])
def test_production_a_captured_payload_retains_capture_epoch(tmp_path,monkeypatch,fault):
    test_production_a_shared_lifecycle_orders_real_phase_runners(tmp_path,monkeypatch,fault)


@pytest.mark.parametrize('fault',['handoff-manifest-none','handoff-manifest-release',
    'handoff-manifest-release-inode','handoff-manifest-entries','handoff-manifest-replaced'])
def test_production_a_complete_manifest_retains_original_capture(tmp_path,monkeypatch,fault):
    test_production_a_shared_lifecycle_orders_real_phase_runners(tmp_path,monkeypatch,fault)


@pytest.mark.parametrize('fault',['payload-boundary-data','payload-boundary-ledger',
    'payload-boundary-rejection','payload-boundary-sql-start'])
def test_production_a_archive_epoch_rechecks_original_proof_references(tmp_path,monkeypatch,fault):
    test_production_a_shared_lifecycle_orders_real_phase_runners(tmp_path,monkeypatch,fault)


def test_production_a_shared_lifecycle_actual_unsupported_runtime_refuses_before_inputs():
    m=load()
    if sys.platform=='linux' and os.geteuid()==0:return
    with pytest.raises(m.WatchdogError,match='WATCHDOG_RUNTIME_UNSUPPORTED'):
        m.run_production_a_shared_lifecycle(None,None,approved_source=None,resume_checks=None,
            container_actions=None,source_guard=None,maintenance_end=None,recovery_flows=None)

NATIVE_A_SHARED_LIFECYCLE_TESTS = '"""Real Linux FD/flock/control pins/signature; explicit service/SQL/nft actors.\n\nThis tests the new phase wiring, not actual SQL, Docker, thaw or host protection.\nOriginal contents, five machine mappings and signed administrator are task-only\nfixtures. No actual daemon/service/SSH/freeze/server/NAS actions occur.\n"""\nimport base64\nimport hashlib\nimport json\nimport os\nfrom pathlib import Path\nimport stat\nimport subprocess\nimport sys\nimport tempfile\nimport time\n\nimport pytest\n\nsys.path.insert(0,\'/workspace/linux\')\nimport formal_capture_producer as producer\nimport formal_snapshot_linux as snapshot\nimport formal_boot_state as boot\nimport formal_split_capture as split\nimport formal_ingress_control as ingress\nimport formal_external_callers as callers\nimport formal_controlled_container_sources as controlled\nfrom formal_source_migration_ledger import approved_source_scope\n\n\ndef digest(raw):return hashlib.sha256(raw).hexdigest()\n\n\ndef write(path,raw,mode=0o600):\n    with path.open(\'xb\') as out:out.write(raw)\n    path.chmod(mode)\n\n\ndef openssl(*args):\n    result=subprocess.run([\'/usr/bin/openssl\',*args],capture_output=True,timeout=5)\n    assert result.returncode==0 and not result.stderr,\'TASK_TEST_OPENSSL_FAILED\'\n    return result.stdout\n\n\n@pytest.fixture\ndef native_case():\n    assert sys.platform==\'linux\' and os.geteuid()==0\n    producer.require_native_runtime()\n    token=snapshot.observation_deadline.set(time.monotonic()+20)\n    claim=None\n    try:\n        with tempfile.TemporaryDirectory(prefix=\'p2-shared-join-\',dir=\'/control\') as directory:\n            root=Path(directory);host_boot=Path(\'/proc/sys/kernel/random/boot_id\').read_text().strip()\n            target=dict(schemaVersion=\'release-formal-target/v1\',environment=\'prod\',host=\'192.0.2.17\',\n                machineId=\'6\'*32,project=\'task-only-project\',root=\'/task/source\',dataMount=\'/task/source\',\n                dataDevice=\'/dev/task-fixture\',dataRoot=\'/task/source/data\',releaseRoot=\'/task/release\',\n                frontendPort=81,backendPort=82,database=\'task_probe\',\n                databaseServerUuid=\'0fa6c22b-5a90-11f1-ade1-0242ac150002\',\n                containers={role:\'task-only-\'+role for role in (\'mysql\',\'redis\',\'minio\',\'backend\',\'frontend\')})\n            target_raw=snapshot._canonical_bytes(target)\n            binding=dict(operationId=\'prod-task-shared-join\',sourceFingerprint=digest(target_raw),\n                databaseUuid=target[\'databaseServerUuid\'],minioContainerId=\'b\'*64)\n            claim=snapshot.FormalMaintenanceClaim(root/\'claim\',binding)\n            fd=claim._fd;signature=boot._signature(os.fstat(fd))\n            source=approved_source_scope(target_raw,digest(target_raw),claim._plan,target[\'database\'])\n            prefence=dict(serverUuid=binding[\'databaseUuid\'],readOnly=0,superReadOnly=0)\n            sources={};boot_sources={}\n            for name in sorted(producer.PRODUCTION_A_SOURCE_FILES):\n                path=claim.root/name\n                raw=claim._plan if name==\'binding.json\' else snapshot._canonical_bytes(dict(explicitOriginalFixture=name))\n                if name==\'approved-source-target.json\':raw=target_raw\n                if name==\'database-prefence-original.json\':raw=snapshot._canonical_bytes(prefence)\n                if name==\'ingress-plan.json\':raw=b\'{}\'\n                if name.startswith(\'boot-\'):\n                    role=name[5:-14];parent=root/(\'boot-fixture-\'+role);parent.mkdir(mode=0o700)\n                    mapping=dict(schemaVersion=\'release-formal-production-a-boot-source/v1\',role=role,peer=\'local\',\n                        machineIdSha256=\'c\'*64,bootId=host_boot,path=str(parent/\'boot.json\'))\n                    boot_sources[role]=mapping\n                    with boot._directory(parent,no_atime=True) as (_,chain):\n                        raw=snapshot._canonical_bytes(dict(schemaVersion=\'release-formal-production-a-boot-original/v1\',\n                            **{k:mapping[k] for k in (\'role\',\'peer\',\'machineIdSha256\',\'bootId\',\'path\')},\n                            sourceParentIdentity=[list(row) for row in chain],present=False,\n                            originalBase64=None,sha256=None,sourceFileIdentity=None))\n                if name!=\'binding.json\':write(path,raw)\n                sources[name]=str(path)\n            release=dict(schemaVersion=\'release-formal-original-package-binding/v2\',claimBindingSha256=digest(claim._plan),\n                approvedSourceTargetSha256=digest(target_raw),deploymentAttemptId=\'1\'*64,leaseTokenDigest=\'2\'*64,\n                scope=dict(environment=\'prod\'),package=dict(releaseTag=\'task-only-shared-join\',\n                    packageDigest=\'3\'*64,manifestDigest=\'4\'*64,planDigest=\'5\'*64))\n            release_raw=snapshot._canonical_bytes(release);claim._write(claim.root/\'original-release-binding.json\',release_raw)\n            sealed=producer.seal_production_a_source_inventory(claim,release_raw,sources,boot_sources,\n                guard=claim.assert_owned,deadline=time.monotonic()+15)\n            registry=root/\'registry\';registry.mkdir(mode=0o700)\n            key_private=root/\'task-only-private.pem\'\n            openssl(\'genpkey\',\'-algorithm\',\'ED25519\',\'-out\',str(key_private));key_private.chmod(0o600)\n            public=root/\'task-only-public.der\'\n            write(public,openssl(\'pkey\',\'-in\',str(key_private),\'-pubout\',\'-outform\',\'DER\'),0o644)\n            now=int(time.time())\n            payload=dict(sealed[\'binding\'],purpose=\'RESTORE_PRODUCTION_SAVED_ORIGINAL_STATE\',grantId=\'8\'*32,\n                actorId=17,tenantId=122,issuedAt=now-1,expiresAt=now+120,\n                policy=dict(originalStateOnly=True,preserveStoppedAndAbsent=True,\n                    preserveUnfinishedRecords=True,noBootStateWrites=True))\n            message=root/\'task-only-payload.json\';write(message,json.dumps(payload,separators=(\',\',\':\')).encode(\'ascii\'))\n            signature_raw=openssl(\'pkeyutl\',\'-sign\',\'-inkey\',str(key_private),\'-rawin\',\'-in\',str(message))\n            approval=root/\'task-only-approval.json\'\n            envelope=dict(schemaVersion=\'formal-production-a-resume-approval/v1\',keySha256=digest(public.read_bytes()),\n                payloadBase64=base64.b64encode(message.read_bytes()).decode(),\n                signatureBase64=base64.b64encode(signature_raw).decode())\n            write(approval,json.dumps(envelope).encode(\'ascii\'));info=registry.stat()\n            resume=dict(registry=dict(path=str(registry),identity={k:getattr(info,\'st_\'+k)\n                for k in (\'dev\',\'ino\',\'uid\',\'gid\',\'mode\')}),approval=dict(path=str(approval),sha256=digest(approval.read_bytes())),\n                trustedKey=dict(path=str(public),sha256=digest(public.read_bytes())))\n            state=dict(mysql_running=True,frozen=False,flags_restored=False,fault=None,calls=[])\n            def live_owner():\n                claim.assert_owned()\n                assert state[\'mysql_running\'] and not state[\'frozen\'],\'SQL callback during stopped/frozen phase\'\n                state[\'calls\'].append(\'live-owner\')\n                if state[\'fault\']==\'live-after-restore\' and state[\'flags_restored\']:\n                    raise snapshot.FormalSnapshotError(\'TASK_TEST_LIVE_OWNER_REFUSED\')\n            checks=producer.ProductionAResumeChecks(claim,release_raw,Path(sealed[\'path\']).read_bytes(),sealed[\'sha256\'],\n                resume,guard=live_owner,thaw_guard=claim.assert_owned)\n            credential=root/\'mysql-credential\';write(credential,b\'task-only-shared-join-input\\n\')\n            originals={role:dict(path=sources[\'controlled-\'+role+\'-inspect.json\'],\n                source=dict(present=True,containerId=digest(role.encode()))) for role in split.ROLES}\n            actions=controlled.ControlledSharedContainerActions(originals,object())\n            request=dict(schemaVersion=\'release-formal-capture-request/v12\',action=\'capture-shared-payload\',\n                claimRoot=str(claim.root),claimBinding=binding,mysqlContainerId=\'d\'*64,mysqlCredentialFile=str(credential),\n                totalSeconds=120,sharedScopeSha256=digest((claim.root/\'shared-container-original.json\').read_bytes()),\n                originalHealthSha256=digest((claim.root/\'shared-health-original.json\').read_bytes()),ingressRulesSha256=\'f\'*64,\n                database=target[\'database\'],sourceRoot=\'/task/source/minio\',sourceScope=source,\n                dataLimits=dict(max_rows=1000,max_nodes=10000,max_bytes=1048576,seconds=30),\n                rejectionProbe=dict(table=\'task_probe\',column=\'id\'),\n                filesystemFence=dict(mount=\'/task/source\',seconds=3,binding=dict(operationId=binding[\'operationId\'],\n                    bootId=host_boot,mount=dict(source=\'/dev/task-fixture\',device=\'9:1\',fstype=\'ext4\',target=\'/task/source\'))))\n            yield dict(root=root,claim=claim,fd=fd,signature=signature,source=source,prefence=prefence,state=state,\n                checks=checks,actions=actions,request=request,credential=credential,approval=approval,registry=registry)\n            claim.close();claim=None\n    finally:\n        if claim is not None:claim.close()\n        snapshot.observation_deadline.reset(token)\n\n\n@pytest.mark.parametrize(\'fault\',[\'none\',\'sql\',\'mysql-stop\',\'raw\',\'thaw\',\'live-after-restore\',\n    \'mysql-start\',\'flags\',\'writer-start\',\'prefence-drift\',\'approval-drift\',\'changed-request\',\n    \'credential-bytes\',\'credential-inode\',\'credential-mode\',\'closed-owner\'])\ndef test_native_a_phase_join_retains_real_authority_fd_and_refuses_next_action(native_case,monkeypatch,fault):\n    case=native_case;claim=case[\'claim\'];state=case[\'state\'];state[\'fault\']=fault;calls=state[\'calls\']\n    req=case[\'request\'];source=case[\'source\'];root=claim.root\n    shared_raw=(root/\'shared-container-original.json\').read_bytes();health_raw=(root/\'shared-health-original.json\').read_bytes()\n    flows=[dict(source=\'192.0.2.1\',destination=\'192.0.2.2\',ports=[3306])]\n    ingress_state=dict(phase=\'CAPTURE_RESTRICTED\',rulesSha256=req[\'ingressRulesSha256\'],tableHandle=17)\n    monkeypatch.setattr(ingress,\'assert_ingress\',lambda *a,**k:dict(ingress_state))\n    monkeypatch.setattr(ingress,\'render_recovery_policy\',lambda *a:None)\n    monkeypatch.setattr(callers,\'guard_external_callers\',lambda *a:dict(state=\'KNOWN_EXTERNAL_CALLERS_STOPPED_PENDING_FULL_EXCLUSION\'))\n    monkeypatch.setattr(producer,\'verify_shared_request_binding\',lambda *a:dict(shared=shared_raw,health=health_raw))\n    monkeypatch.setattr(producer,\'verify_shared_database_before_stop\',lambda *a:None)\n    class SqlProtocol:\n        def __init__(self,container,*a,**k):self.container=container\n    monkeypatch.setattr(producer,\'PrivateDockerMySql\',SqlProtocol)\n    def source_guard():\n        assert not state[\'frozen\'],\'source callback during frozen phase\'\n        claim.assert_owned();calls.append(\'source\')\n        if fault==\'changed-request\' and any(e[\'step\']==\'db-fenced\' for e in claim.events):req[\'database\']=\'changed\'\n    def thaw(selected):\n        assert selected is claim and not state[\'frozen\'],\'thaw observer during frozen phase\'\n        if fault==\'thaw\' and any(e[\'step\']==\'split-captured\' for e in claim.events):\n            raise snapshot.FormalSnapshotError(\'FORMAL_SHARED_THAW_REQUIRED\')\n        return dict(nativeState=\'THAWED_COPY_ACCEPTED_PENDING_FORMAL_BARRIERS\')\n    monkeypatch.setattr(split,\'_thaw_receipt\',thaw)\n    def stop_writer(selected,role,**kwargs):\n        kwargs[\'guard\']();calls.append(\'stop:\'+role)\n        claim.record(\'shared-\'+role+\'-stop-intent\',dict(explicitProtocol=True))\n        if role==split.STOP_ORDER[0] and fault in (\'credential-bytes\',\'credential-inode\'):\n            credential=case[\'credential\'];raw=credential.read_bytes()\n            if fault==\'credential-inode\':credential.rename(credential.with_suffix(\'.retained\'));write(credential,raw)\n            else:credential.write_bytes(b\'CHANGED_PRIVATE_INPUT\\n\')\n        claim.record(\'shared-\'+role+\'-stopped\',dict(explicitProtocol=True));kwargs[\'guard\']()\n    monkeypatch.setattr(split,\'stop_controlled_shared_writer\',stop_writer)\n    monkeypatch.setattr(split,\'assert_controlled_prefence_stops\',lambda *a,**k:k[\'guard\']())\n    def fence(selected,mysql,*,guard):\n        guard();calls.append(\'fence\');flags=dict(case[\'prefence\'])\n        if fault==\'prefence-drift\':flags[\'readOnly\']=1\n        raw=snapshot._canonical_bytes(flags);claim._write(root/\'database-original.json\',raw)\n        claim.record(\'db-fence-intent\',dict(originalFlags=flags,originalSha256=digest(raw)))\n        guard();claim.record(\'db-fenced\',dict(originalSha256=digest(raw),reconciled=False));guard()\n    monkeypatch.setattr(snapshot,\'acquire_claimed_database_fence\',fence)\n    def sql(*a,**kwargs):\n        kwargs[\'guard\']();calls.append(\'sql\')\n        if fault==\'sql\':raise snapshot.FormalSnapshotError(\'TASK_TEST_SQL_REFUSED\')\n        claim.record(\'split-sql-intent\',{});claim.record(\'split-sql-captured\',{})\n        return dict(state=\'SQL_CAPTURED_PENDING_RAW_AND_FULL_BARRIERS\',sourceScope=source,sourceMigrationEvidence={})\n    monkeypatch.setattr(split,\'capture_sql_phase\',sql)\n    def stop_mysql(selected,role,**kwargs):\n        assert role==\'mysql\';kwargs[\'guard\']();claim.record(\'shared-mysql-stop-intent\',{})\n        kwargs[\'guard\']();calls.append(\'stop:mysql\');state[\'mysql_running\']=False\n        if fault==\'closed-owner\':claim.close()\n        if fault==\'approval-drift\':case[\'approval\'].write_bytes(b\'CHANGED_TASK_ONLY_APPROVAL\')\n        kwargs[\'guard\']()\n        if fault==\'mysql-stop\':raise snapshot.FormalSnapshotError(\'TASK_TEST_STOP_UNCERTAIN\')\n        claim.record(\'shared-mysql-stopped\',{})\n    monkeypatch.setattr(split,\'stop_controlled_capture_database\',stop_mysql)\n    def raw(*a,**kwargs):\n        kwargs[\'guard\']();calls.append(\'raw\');state[\'frozen\']=True\n        kwargs[\'native_guard\']();state[\'frozen\']=False\n        if fault==\'raw\':raise snapshot.FormalSnapshotError(\'TASK_TEST_RAW_REFUSED\')\n        claim.record(\'split-raw-intent\',{});claim.record(\'split-captured\',{});kwargs[\'guard\']()\n        return dict(state=\'CAPTURED_PENDING_FULL_BARRIER_PROOF\',sourceScope=source,sourceMigrationEvidence={})\n    monkeypatch.setattr(split,\'capture_raw_phase\',raw)\n    def transition(selected,value,*,guard,thaw_guard):\n        guard();thaw_guard();calls.append(\'transition\');claim.record(\'ingress-recovery-intent\',{})\n        rules=snapshot._canonical_bytes([dict(table=dict(handle=18))]);claim._write(root/\'ingress-recovery-installed.json\',rules)\n        ingress_state.update(phase=\'RECOVERY_DEPENDENCIES_PERMITTED\',tableHandle=18,rulesSha256=digest(rules))\n        claim.record(\'ingress-recovery-permitted\',dict(rulesSha256=digest(rules),planSha256=digest(snapshot._canonical_bytes(value))))\n        guard();thaw_guard();return dict(ingress_state)\n    monkeypatch.setattr(ingress,\'permit_recovery_dependencies\',transition)\n    def begin(selected,*,guard,thaw_guard):\n        guard();thaw_guard();claim.record(\'shared-recovery-intent\',{})\n    monkeypatch.setattr(split,\'begin_shared_recovery\',begin)\n    def start(selected,role,action,**kwargs):\n        assert action==\'start\';kwargs[\'guard\']();calls.append(\'start:\'+role)\n        if fault==\'mysql-start\' and role==\'mysql\' or fault==\'writer-start\' and role==\'redis\':\n            claim.record(\'shared-\'+role+\'-start-intent\',{})\n            raise snapshot.FormalSnapshotError(\'TASK_TEST_START_UNCERTAIN\')\n        if role==\'mysql\':state[\'mysql_running\']=True\n        else:assert state[\'flags_restored\'] and \'live-owner\' in calls[calls.index(\'flags\')+1:]\n        claim.record(\'shared-\'+role+\'-restored\',{});kwargs[\'guard\']()\n    monkeypatch.setattr(split,\'_shared_action\',start)\n    def flags(selected,mysql,*,guard,thaw_guard):\n        guard();calls.append(\'flags\')\n        if fault==\'flags\':raise snapshot.FormalSnapshotError(\'TASK_TEST_FLAGS_UNCERTAIN\')\n        state[\'flags_restored\']=True;claim.record(\'shared-db-restored\',{});guard()\n    monkeypatch.setattr(split,\'restore_shared_database_flags\',flags)\n    monkeypatch.setattr(split,\'verify_shared_restoration\',lambda *a,**k:dict(state=\'ORIGINAL_SERVICES_RESTORED_PENDING_CALLER_PROOF\'))\n    if fault==\'credential-mode\':case[\'credential\'].chmod(0o666)\n    call=lambda:producer.run_production_a_shared_lifecycle(claim,req,approved_source=source,\n        resume_checks=case[\'checks\'],container_actions=case[\'actions\'],source_guard=source_guard,\n        maintenance_end=time.time()+90,recovery_flows=flows)\n    if fault==\'none\':\n        result=call()\n        order=[\'stop:\'+r for r in split.STOP_ORDER[:-1]]+[\'fence\',\'sql\',\'stop:mysql\',\'raw\',\'transition\',\n            \'start:mysql\',\'flags\']+[\'start:\'+r for r in split.RESTORE_ORDER[1:]]\n        assert [c for c in calls if c.startswith((\'stop:\',\'start:\')) or c in (\'fence\',\'sql\',\'raw\',\'transition\',\'flags\')]==order\n        assert result[\'claimReleased\'] is False and result[\'fullRunnerCompleted\'] is False and result[\'productionEvidence\'] is False\n    else:\n        with pytest.raises(snapshot.FormalSnapshotError):call()\n        if fault not in (\'live-after-restore\',\'mysql-start\',\'flags\',\'writer-start\'):\n            assert not any(c.startswith(\'start:\') for c in calls)\n        if fault in (\'live-after-restore\',\'mysql-start\',\'flags\'):\n            assert not any(c.startswith(\'start:\') and c!=\'start:mysql\' for c in calls)\n        if fault==\'credential-mode\':assert not any(c.startswith(\'stop:\') for c in calls)\n    assert len(list(case[\'registry\'].iterdir()))==1\n    assert sum(e[\'step\']==\'production-a-resume-consumed\' for e in claim.events)==1\n    if fault==\'closed-owner\':assert claim._fd is None\n    else:\n        assert claim._fd==case[\'fd\'] and boot._signature(os.fstat(claim._fd))==case[\'signature\']\n        with pytest.raises(snapshot.FormalSnapshotError,match=\'FORMAL_MAINTENANCE_BUSY\'):\n            snapshot.FormalMaintenanceClaim(claim.root,json.loads(claim._plan),resume=True)\n'


def test_production_a_shared_lifecycle_accepts_existing_authorization_binding(tmp_path,monkeypatch):
    test_production_a_shared_lifecycle_orders_real_phase_runners(tmp_path,monkeypatch,'none')

NATIVE_A_SHARED_LIFECYCLE_TESTS_V2 = '"""Real Linux FD/flock/control pins/signature; explicit service/SQL/nft actors.\n\nThis tests the new phase wiring, not actual SQL, Docker, thaw or host protection.\nOriginal contents, five machine mappings and signed administrator are task-only\nfixtures. No actual daemon/service/SSH/freeze/server/NAS actions occur.\n"""\nimport base64\nimport hashlib\nimport json\nimport os\nfrom pathlib import Path\nimport stat\nimport subprocess\nimport sys\nimport tempfile\nimport time\n\nimport pytest\n\nsys.path.insert(0,\'/workspace/linux\')\nimport formal_capture_producer as producer\nimport formal_snapshot_linux as snapshot\nimport formal_boot_state as boot\nimport formal_split_capture as split\nimport formal_ingress_control as ingress\nimport formal_external_callers as callers\nimport formal_controlled_container_sources as controlled\nfrom formal_source_migration_ledger import approved_source_scope\n\n\ndef digest(raw):return hashlib.sha256(raw).hexdigest()\n\n\ndef write(path,raw,mode=0o600):\n    with path.open(\'xb\') as out:out.write(raw)\n    path.chmod(mode)\n\n\ndef openssl(*args):\n    result=subprocess.run([\'/usr/bin/openssl\',*args],capture_output=True,timeout=5)\n    assert result.returncode==0 and not result.stderr,\'TASK_TEST_OPENSSL_FAILED\'\n    return result.stdout\n\n\n@pytest.fixture\ndef native_case():\n    assert sys.platform==\'linux\' and os.geteuid()==0\n    producer.require_native_runtime()\n    token=snapshot.observation_deadline.set(time.monotonic()+20)\n    claim=None\n    try:\n        with tempfile.TemporaryDirectory(prefix=\'p2-shared-join-\',dir=\'/control\') as directory:\n            root=Path(directory);host_boot=Path(\'/proc/sys/kernel/random/boot_id\').read_text().strip()\n            target=dict(schemaVersion=\'release-formal-target/v1\',environment=\'prod\',host=\'192.0.2.17\',\n                machineId=\'6\'*32,project=\'task-only-project\',root=\'/task/source\',dataMount=\'/task/source\',\n                dataDevice=\'/dev/task-fixture\',dataRoot=\'/task/source/data\',releaseRoot=\'/task/release\',\n                frontendPort=81,backendPort=82,database=\'task_probe\',\n                databaseServerUuid=\'0fa6c22b-5a90-11f1-ade1-0242ac150002\',\n                containers={role:\'task-only-\'+role for role in (\'mysql\',\'redis\',\'minio\',\'backend\',\'frontend\')})\n            target_raw=snapshot._canonical_bytes(target)\n            binding=dict(operationId=\'prod-task-only-shared-join\',sourceFingerprint=digest(json.dumps(target,sort_keys=True,separators=(\',\',\':\'),ensure_ascii=False).encode(\'utf-8\')),\n                databaseUuid=target[\'databaseServerUuid\'],minioContainerId=\'b\'*64)\n            claim=snapshot.FormalMaintenanceClaim(root/\'claim\',binding)\n            fd=claim._fd;signature=boot._signature(os.fstat(fd))\n            source=approved_source_scope(target_raw,digest(target_raw),claim._plan,target[\'database\'])\n            prefence=dict(serverUuid=binding[\'databaseUuid\'],readOnly=0,superReadOnly=0)\n            sources={};boot_sources={}\n            for name in sorted(producer.PRODUCTION_A_SOURCE_FILES):\n                path=claim.root/name\n                raw=claim._plan if name==\'binding.json\' else snapshot._canonical_bytes(dict(explicitOriginalFixture=name))\n                if name==\'approved-source-target.json\':raw=target_raw\n                if name==\'database-prefence-original.json\':raw=snapshot._canonical_bytes(prefence)\n                if name==\'ingress-plan.json\':raw=b\'{}\'\n                if name.startswith(\'boot-\'):\n                    role=name[5:-14];parent=root/(\'boot-fixture-\'+role);parent.mkdir(mode=0o700)\n                    mapping=dict(schemaVersion=\'release-formal-production-a-boot-source/v1\',role=role,peer=\'local\',\n                        machineIdSha256=\'c\'*64,bootId=host_boot,path=str(parent/\'boot.json\'))\n                    boot_sources[role]=mapping\n                    with boot._directory(parent,no_atime=True) as (_,chain):\n                        raw=snapshot._canonical_bytes(dict(schemaVersion=\'release-formal-production-a-boot-original/v1\',\n                            **{k:mapping[k] for k in (\'role\',\'peer\',\'machineIdSha256\',\'bootId\',\'path\')},\n                            sourceParentIdentity=[list(row) for row in chain],present=False,\n                            originalBase64=None,sha256=None,sourceFileIdentity=None))\n                if name!=\'binding.json\':write(path,raw)\n                sources[name]=str(path)\n            release=dict(schemaVersion=\'release-formal-original-package-binding/v2\',claimBindingSha256=digest(claim._plan),\n                approvedSourceTargetSha256=digest(target_raw),deploymentAttemptId=\'1\'*64,leaseTokenDigest=\'2\'*64,\n                scope=dict(environment=\'prod\'),package=dict(releaseTag=\'task-only-shared-join\',\n                    packageDigest=\'3\'*64,manifestDigest=\'4\'*64,planDigest=\'5\'*64))\n            release_raw=snapshot._canonical_bytes(release);claim._write(claim.root/\'original-release-binding.json\',release_raw)\n            sealed=producer.seal_production_a_source_inventory(claim,release_raw,sources,boot_sources,\n                guard=claim.assert_owned,deadline=time.monotonic()+15)\n            registry=root/\'registry\';registry.mkdir(mode=0o700)\n            key_private=root/\'task-only-private.pem\'\n            openssl(\'genpkey\',\'-algorithm\',\'ED25519\',\'-out\',str(key_private));key_private.chmod(0o600)\n            public=root/\'task-only-public.der\'\n            write(public,openssl(\'pkey\',\'-in\',str(key_private),\'-pubout\',\'-outform\',\'DER\'),0o644)\n            now=int(time.time())\n            payload=dict(sealed[\'binding\'],purpose=\'RESTORE_PRODUCTION_SAVED_ORIGINAL_STATE\',grantId=\'8\'*32,\n                actorId=17,tenantId=122,issuedAt=now-1,expiresAt=now+120,\n                policy=dict(originalStateOnly=True,preserveStoppedAndAbsent=True,\n                    preserveUnfinishedRecords=True,noBootStateWrites=True))\n            message=root/\'task-only-payload.json\';write(message,json.dumps(payload,separators=(\',\',\':\')).encode(\'ascii\'))\n            signature_raw=openssl(\'pkeyutl\',\'-sign\',\'-inkey\',str(key_private),\'-rawin\',\'-in\',str(message))\n            approval=root/\'task-only-approval.json\'\n            envelope=dict(schemaVersion=\'formal-production-a-resume-approval/v1\',keySha256=digest(public.read_bytes()),\n                payloadBase64=base64.b64encode(message.read_bytes()).decode(),\n                signatureBase64=base64.b64encode(signature_raw).decode())\n            write(approval,json.dumps(envelope).encode(\'ascii\'));info=registry.stat()\n            resume=dict(registry=dict(path=str(registry),identity={k:getattr(info,\'st_\'+k)\n                for k in (\'dev\',\'ino\',\'uid\',\'gid\',\'mode\')}),approval=dict(path=str(approval),sha256=digest(approval.read_bytes())),\n                trustedKey=dict(path=str(public),sha256=digest(public.read_bytes())))\n            state=dict(mysql_running=True,frozen=False,flags_restored=False,fault=None,calls=[])\n            def live_owner():\n                claim.assert_owned()\n                assert state[\'mysql_running\'] and not state[\'frozen\'],\'SQL callback during stopped/frozen phase\'\n                state[\'calls\'].append(\'live-owner\')\n                if state[\'fault\']==\'live-after-restore\' and state[\'flags_restored\']:\n                    raise snapshot.FormalSnapshotError(\'TASK_TEST_LIVE_OWNER_REFUSED\')\n            checks=producer.ProductionAResumeChecks(claim,release_raw,Path(sealed[\'path\']).read_bytes(),sealed[\'sha256\'],\n                resume,guard=live_owner,thaw_guard=claim.assert_owned)\n            credential=root/\'mysql-credential\';write(credential,b\'task-only-shared-join-input\\n\')\n            originals={role:dict(path=sources[\'controlled-\'+role+\'-inspect.json\'],\n                source=dict(present=True,containerId=digest(role.encode()))) for role in split.ROLES}\n            actions=controlled.ControlledSharedContainerActions(originals,object())\n            request=dict(schemaVersion=\'release-formal-capture-request/v12\',action=\'capture-shared-payload\',\n                claimRoot=str(claim.root),claimBinding=binding,mysqlContainerId=\'d\'*64,mysqlCredentialFile=str(credential),\n                totalSeconds=120,sharedScopeSha256=digest((claim.root/\'shared-container-original.json\').read_bytes()),\n                originalHealthSha256=digest((claim.root/\'shared-health-original.json\').read_bytes()),ingressRulesSha256=\'f\'*64,\n                database=target[\'database\'],sourceRoot=\'/task/source/minio\',sourceScope=source,\n                dataLimits=dict(max_rows=1000,max_nodes=10000,max_bytes=1048576,seconds=30),\n                rejectionProbe=dict(table=\'task_probe\',column=\'id\'),\n                filesystemFence=dict(mount=\'/task/source\',seconds=3,binding=dict(operationId=binding[\'operationId\'],\n                    bootId=host_boot,mount=dict(source=\'/dev/task-fixture\',device=\'9:1\',fstype=\'ext4\',target=\'/task/source\'))))\n            yield dict(root=root,claim=claim,fd=fd,signature=signature,source=source,prefence=prefence,state=state,\n                checks=checks,actions=actions,request=request,credential=credential,approval=approval,registry=registry)\n            claim.close();claim=None\n    finally:\n        if claim is not None:claim.close()\n        snapshot.observation_deadline.reset(token)\n\n\n@pytest.mark.parametrize(\'fault\',[\'none\',\'sql\',\'mysql-stop\',\'raw\',\'thaw\',\'live-after-restore\',\n    \'mysql-start\',\'flags\',\'writer-start\',\'prefence-drift\',\'approval-drift\',\'changed-request\',\n    \'credential-bytes\',\'credential-inode\',\'credential-mode\',\'closed-owner\'])\ndef test_native_a_phase_join_retains_real_authority_fd_and_refuses_next_action(native_case,monkeypatch,fault):\n    case=native_case;claim=case[\'claim\'];state=case[\'state\'];state[\'fault\']=fault;calls=state[\'calls\']\n    req=case[\'request\'];source=case[\'source\'];root=claim.root\n    shared_raw=(root/\'shared-container-original.json\').read_bytes();health_raw=(root/\'shared-health-original.json\').read_bytes()\n    flows=[dict(source=\'192.0.2.1\',destination=\'192.0.2.2\',ports=[3306])]\n    ingress_state=dict(phase=\'CAPTURE_RESTRICTED\',rulesSha256=req[\'ingressRulesSha256\'],tableHandle=17)\n    monkeypatch.setattr(ingress,\'assert_ingress\',lambda *a,**k:dict(ingress_state))\n    monkeypatch.setattr(ingress,\'render_recovery_policy\',lambda *a:None)\n    monkeypatch.setattr(callers,\'guard_external_callers\',lambda *a:dict(state=\'KNOWN_EXTERNAL_CALLERS_STOPPED_PENDING_FULL_EXCLUSION\'))\n    monkeypatch.setattr(producer,\'verify_shared_request_binding\',lambda *a:dict(shared=shared_raw,health=health_raw))\n    monkeypatch.setattr(producer,\'verify_shared_database_before_stop\',lambda *a:None)\n    class SqlProtocol:\n        def __init__(self,container,*a,**k):self.container=container\n    monkeypatch.setattr(producer,\'PrivateDockerMySql\',SqlProtocol)\n    def source_guard():\n        assert not state[\'frozen\'],\'source callback during frozen phase\'\n        claim.assert_owned();calls.append(\'source\')\n        if fault==\'changed-request\' and any(e[\'step\']==\'db-fenced\' for e in claim.events):req[\'database\']=\'changed\'\n    def thaw(selected):\n        assert selected is claim and not state[\'frozen\'],\'thaw observer during frozen phase\'\n        if fault==\'thaw\' and any(e[\'step\']==\'split-captured\' for e in claim.events):\n            raise snapshot.FormalSnapshotError(\'FORMAL_SHARED_THAW_REQUIRED\')\n        return dict(nativeState=\'THAWED_COPY_ACCEPTED_PENDING_FORMAL_BARRIERS\')\n    monkeypatch.setattr(split,\'_thaw_receipt\',thaw)\n    def stop_writer(selected,role,**kwargs):\n        kwargs[\'guard\']();calls.append(\'stop:\'+role)\n        claim.record(\'shared-\'+role+\'-stop-intent\',dict(explicitProtocol=True))\n        if role==split.STOP_ORDER[0] and fault in (\'credential-bytes\',\'credential-inode\'):\n            credential=case[\'credential\'];raw=credential.read_bytes()\n            if fault==\'credential-inode\':credential.rename(credential.with_suffix(\'.retained\'));write(credential,raw)\n            else:credential.write_bytes(b\'CHANGED_PRIVATE_INPUT\\n\')\n        claim.record(\'shared-\'+role+\'-stopped\',dict(explicitProtocol=True));kwargs[\'guard\']()\n    monkeypatch.setattr(split,\'stop_controlled_shared_writer\',stop_writer)\n    monkeypatch.setattr(split,\'assert_controlled_prefence_stops\',lambda *a,**k:k[\'guard\']())\n    def fence(selected,mysql,*,guard):\n        guard();calls.append(\'fence\');flags=dict(case[\'prefence\'])\n        if fault==\'prefence-drift\':flags[\'readOnly\']=1\n        raw=snapshot._canonical_bytes(flags);claim._write(root/\'database-original.json\',raw)\n        claim.record(\'db-fence-intent\',dict(originalFlags=flags,originalSha256=digest(raw)))\n        guard();claim.record(\'db-fenced\',dict(originalSha256=digest(raw),reconciled=False));guard()\n    monkeypatch.setattr(snapshot,\'acquire_claimed_database_fence\',fence)\n    def sql(*a,**kwargs):\n        kwargs[\'guard\']();calls.append(\'sql\')\n        if fault==\'sql\':raise snapshot.FormalSnapshotError(\'TASK_TEST_SQL_REFUSED\')\n        claim.record(\'split-sql-intent\',{});claim.record(\'split-sql-captured\',{})\n        return dict(state=\'SQL_CAPTURED_PENDING_RAW_AND_FULL_BARRIERS\',sourceScope=source,sourceMigrationEvidence={})\n    monkeypatch.setattr(split,\'capture_sql_phase\',sql)\n    def stop_mysql(selected,role,**kwargs):\n        assert role==\'mysql\';kwargs[\'guard\']();claim.record(\'shared-mysql-stop-intent\',{})\n        kwargs[\'guard\']();calls.append(\'stop:mysql\');state[\'mysql_running\']=False\n        if fault==\'closed-owner\':claim.close()\n        if fault==\'approval-drift\':case[\'approval\'].write_bytes(b\'CHANGED_TASK_ONLY_APPROVAL\')\n        kwargs[\'guard\']()\n        if fault==\'mysql-stop\':raise snapshot.FormalSnapshotError(\'TASK_TEST_STOP_UNCERTAIN\')\n        claim.record(\'shared-mysql-stopped\',{})\n    monkeypatch.setattr(split,\'stop_controlled_capture_database\',stop_mysql)\n    def raw(*a,**kwargs):\n        kwargs[\'guard\']();calls.append(\'raw\');state[\'frozen\']=True\n        kwargs[\'native_guard\']();state[\'frozen\']=False\n        if fault==\'raw\':raise snapshot.FormalSnapshotError(\'TASK_TEST_RAW_REFUSED\')\n        claim.record(\'split-raw-intent\',{});claim.record(\'split-captured\',{});kwargs[\'guard\']()\n        return dict(state=\'CAPTURED_PENDING_FULL_BARRIER_PROOF\',sourceScope=source,sourceMigrationEvidence={})\n    monkeypatch.setattr(split,\'capture_raw_phase\',raw)\n    def transition(selected,value,*,guard,thaw_guard):\n        guard();thaw_guard();calls.append(\'transition\');claim.record(\'ingress-recovery-intent\',{})\n        rules=snapshot._canonical_bytes([dict(table=dict(handle=18))]);claim._write(root/\'ingress-recovery-installed.json\',rules)\n        ingress_state.update(phase=\'RECOVERY_DEPENDENCIES_PERMITTED\',tableHandle=18,rulesSha256=digest(rules))\n        claim.record(\'ingress-recovery-permitted\',dict(rulesSha256=digest(rules),planSha256=digest(snapshot._canonical_bytes(value))))\n        guard();thaw_guard();return dict(ingress_state)\n    monkeypatch.setattr(ingress,\'permit_recovery_dependencies\',transition)\n    def begin(selected,*,guard,thaw_guard):\n        guard();thaw_guard();claim.record(\'shared-recovery-intent\',{})\n    monkeypatch.setattr(split,\'begin_shared_recovery\',begin)\n    def start(selected,role,action,**kwargs):\n        assert action==\'start\';kwargs[\'guard\']();calls.append(\'start:\'+role)\n        if fault==\'mysql-start\' and role==\'mysql\' or fault==\'writer-start\' and role==\'redis\':\n            claim.record(\'shared-\'+role+\'-restore-intent\',{})\n            raise snapshot.FormalSnapshotError(\'TASK_TEST_START_UNCERTAIN\')\n        if role==\'mysql\':state[\'mysql_running\']=True\n        else:assert state[\'flags_restored\'] and \'live-owner\' in calls[calls.index(\'flags\')+1:]\n        claim.record(\'shared-\'+role+\'-restored\',{});kwargs[\'guard\']()\n    monkeypatch.setattr(split,\'_shared_action\',start)\n    def flags(selected,mysql,*,guard,thaw_guard):\n        guard();calls.append(\'flags\')\n        if fault==\'flags\':raise snapshot.FormalSnapshotError(\'TASK_TEST_FLAGS_UNCERTAIN\')\n        state[\'flags_restored\']=True;claim.record(\'shared-db-restored\',{});guard()\n    monkeypatch.setattr(split,\'restore_shared_database_flags\',flags)\n    monkeypatch.setattr(split,\'verify_shared_restoration\',lambda *a,**k:dict(state=\'ORIGINAL_SERVICES_RESTORED_PENDING_CALLER_PROOF\'))\n    if fault==\'credential-mode\':case[\'credential\'].chmod(0o666)\n    call=lambda:producer.run_production_a_shared_lifecycle(claim,req,approved_source=source,\n        resume_checks=case[\'checks\'],container_actions=case[\'actions\'],source_guard=source_guard,\n        maintenance_end=time.time()+90,recovery_flows=flows)\n    if fault==\'none\':\n        result=call()\n        order=[\'stop:\'+r for r in split.STOP_ORDER[:-1]]+[\'fence\',\'sql\',\'stop:mysql\',\'raw\',\'transition\',\n            \'start:mysql\',\'flags\']+[\'start:\'+r for r in split.RESTORE_ORDER[1:]]\n        assert [c for c in calls if c.startswith((\'stop:\',\'start:\')) or c in (\'fence\',\'sql\',\'raw\',\'transition\',\'flags\')]==order\n        assert result[\'claimReleased\'] is False and result[\'fullRunnerCompleted\'] is False and result[\'productionEvidence\'] is False\n    else:\n        expected=dict(sql=\'TASK_TEST_SQL_REFUSED\',**{\n            \'mysql-stop\':\'TASK_TEST_STOP_UNCERTAIN\',\'raw\':\'TASK_TEST_RAW_REFUSED\',\n            \'thaw\':\'FORMAL_SHARED_THAW_REQUIRED\',\'live-after-restore\':\'TASK_TEST_LIVE_OWNER_REFUSED\',\n            \'mysql-start\':\'TASK_TEST_START_UNCERTAIN\',\'flags\':\'TASK_TEST_FLAGS_UNCERTAIN\',\n            \'writer-start\':\'TASK_TEST_START_UNCERTAIN\',\'prefence-drift\':\'FORMAL_PRODUCTION_A_DATABASE_ORIGINAL_CHANGED\',\n            \'approval-drift\':\'FORMAL_PRODUCTION_A_RESUME_CONTROL_CHANGED\',\n            \'changed-request\':\'FORMAL_PRODUCTION_A_SHARED_INPUT_CHANGED\',\n            \'credential-bytes\':\'FORMAL_PRODUCTION_A_SHARED_CREDENTIAL_CHANGED\',\n            \'credential-inode\':\'FORMAL_PRODUCTION_A_SHARED_CREDENTIAL_CHANGED\',\n            \'credential-mode\':\'FORMAL_BOOT_FILE_UNTRUSTED\',\n            \'closed-owner\':\'FORMAL_PRODUCTION_A_RESUME_CONTROL_CHANGED\'})\n        with pytest.raises(snapshot.FormalSnapshotError,match=\'^\'+expected[fault]+\'$\'):call()\n        # Refusal must reach this scenario\'s intended seam; an earlier typed\n        # error is a failure, not evidence of safe uncertainty handling.\n        reached={\'sql\':\'sql\',\'mysql-stop\':\'stop:mysql\',\'raw\':\'raw\',\'thaw\':\'raw\',\n            \'live-after-restore\':\'flags\',\'mysql-start\':\'start:mysql\',\'flags\':\'flags\',\n            \'writer-start\':\'start:redis\',\'prefence-drift\':\'fence\',\'approval-drift\':\'stop:mysql\',\n            \'changed-request\':\'fence\',\'credential-bytes\':\'stop:frontend\',\'credential-inode\':\'stop:frontend\',\n            \'closed-owner\':\'stop:mysql\'}\n        if fault in reached:assert reached[fault] in calls\n        if fault not in (\'live-after-restore\',\'mysql-start\',\'flags\',\'writer-start\'):\n            assert not any(c.startswith(\'start:\') for c in calls)\n        if fault in (\'live-after-restore\',\'mysql-start\',\'flags\'):\n            assert not any(c.startswith(\'start:\') and c!=\'start:mysql\' for c in calls)\n        if fault==\'credential-mode\':assert not any(c.startswith(\'stop:\') for c in calls)\n    assert len(list(case[\'registry\'].iterdir()))==1\n    assert sum(e[\'step\']==\'production-a-resume-consumed\' for e in claim.events)==1\n    if fault==\'closed-owner\':assert claim._fd is None\n    else:\n        assert claim._fd==case[\'fd\'] and boot._signature(os.fstat(claim._fd))==case[\'signature\']\n        with pytest.raises(snapshot.FormalSnapshotError,match=\'FORMAL_MAINTENANCE_BUSY\'):\n            snapshot.FormalMaintenanceClaim(claim.root,json.loads(claim._plan),resume=True)\n'



def test_production_a_shared_lifecycle_native_fixture_uses_actual_source_and_signed_binding_contract():
    """Pure validation of the actual prepared native fixture before admission."""
    import ast
    from formal_source_migration_ledger import approved_source_scope
    m=load();tree=ast.parse(NATIVE_A_SHARED_LIFECYCLE_TESTS_V2)
    fn=next(n for n in tree.body if isinstance(n,ast.FunctionDef) and n.name=='native_case')
    assignments={n.targets[0].id:n for n in ast.walk(fn) if isinstance(n,ast.Assign)
        and len(n.targets)==1 and isinstance(n.targets[0],ast.Name)}
    namespace=dict(snapshot=m.snapshot,json=json,digest=lambda raw:hashlib.sha256(raw).hexdigest())
    for name in ('target','target_raw','binding'):
        exec(compile(ast.Module(body=[assignments[name]],type_ignores=[]),'<prepared-pure-fixture>','exec'),namespace)
    raw=namespace['target_raw'];plan=m.snapshot._canonical_bytes(namespace['binding'])
    source=approved_source_scope(raw,hashlib.sha256(raw).hexdigest(),plan,namespace['target']['database'])
    assert source['targetFingerprint']==namespace['binding']['sourceFingerprint']
    assert source['targetFingerprint']!=source['targetRawSha256']
    namespace['claim']=types.SimpleNamespace(_plan=plan)
    exec(compile(ast.Module(body=[assignments['release']],type_ignores=[]),'<prepared-pure-fixture>','exec'),namespace)
    release=namespace['release']
    assert namespace['binding']['operationId']=='prod-'+release['package']['releaseTag']
    inventory=dict(schemaVersion='release-formal-production-a-original-sources/v1',
        claimBindingSha256=hashlib.sha256(plan).hexdigest(),approvedSourceTargetSha256=hashlib.sha256(raw).hexdigest(),
        hostBootId='0fa6c22b-5a90-11f1-ade1-0242ac150002',
        files={name:dict(path='/explicit/'+name,sha256='8'*64) for name in m.PRODUCTION_A_SOURCE_FILES})
    inventory_raw=m.snapshot._canonical_bytes(inventory)
    actual=m.production_a_resume_binding(m.snapshot._canonical_bytes(release),inventory_raw,hashlib.sha256(inventory_raw).hexdigest())
    assert actual['operationId']==namespace['binding']['operationId'] and 'claimBindingSha256' not in actual


def test_production_a_shared_lifecycle_accepts_actual_controlled_action_pin(tmp_path,monkeypatch):
    test_production_a_shared_lifecycle_orders_real_phase_runners(tmp_path,monkeypatch,'none')

NATIVE_A_SHARED_LIFECYCLE_TESTS_V3 = '"""Real Linux FD/flock/control pins/signature; explicit service/SQL/nft actors.\n\nThis tests the new phase wiring, not actual SQL, Docker, thaw or host protection.\nOriginal contents, five machine mappings and signed administrator are task-only\nfixtures. No actual daemon/service/SSH/freeze/server/NAS actions occur.\n"""\nimport base64\nimport hashlib\nimport json\nimport os\nfrom pathlib import Path\nimport stat\nimport subprocess\nimport sys\nimport tempfile\nimport time\n\nimport pytest\n\nsys.path.insert(0,\'/workspace/linux\')\nimport formal_capture_producer as producer\nimport formal_snapshot_linux as snapshot\nimport formal_boot_state as boot\nimport formal_split_capture as split\nimport formal_ingress_control as ingress\nimport formal_external_callers as callers\nimport formal_controlled_container_sources as controlled\nfrom formal_source_migration_ledger import approved_source_scope\n\n\ndef digest(raw):return hashlib.sha256(raw).hexdigest()\n\n\ndef write(path,raw,mode=0o600):\n    with path.open(\'xb\') as out:out.write(raw)\n    path.chmod(mode)\n\n\ndef openssl(*args):\n    result=subprocess.run([\'/usr/bin/openssl\',*args],capture_output=True,timeout=5)\n    assert result.returncode==0 and not result.stderr,\'TASK_TEST_OPENSSL_FAILED\'\n    return result.stdout\n\n\n@pytest.fixture\ndef native_case():\n    assert sys.platform==\'linux\' and os.geteuid()==0\n    producer.require_native_runtime()\n    token=snapshot.observation_deadline.set(time.monotonic()+45)\n    claim=None\n    try:\n        with tempfile.TemporaryDirectory(prefix=\'p2-shared-join-\',dir=\'/control\') as directory:\n            root=Path(directory);host_boot=Path(\'/proc/sys/kernel/random/boot_id\').read_text().strip()\n            target=dict(schemaVersion=\'release-formal-target/v1\',environment=\'prod\',host=\'192.0.2.17\',\n                machineId=\'6\'*32,project=\'task-only-project\',root=\'/task/source\',dataMount=\'/task/source\',\n                dataDevice=\'/dev/task-fixture\',dataRoot=\'/task/source/data\',releaseRoot=\'/task/release\',\n                frontendPort=81,backendPort=82,database=\'task_probe\',\n                databaseServerUuid=\'0fa6c22b-5a90-11f1-ade1-0242ac150002\',\n                containers={role:\'task-only-\'+role for role in (\'mysql\',\'redis\',\'minio\',\'backend\',\'frontend\')})\n            target_raw=snapshot._canonical_bytes(target)\n            binding=dict(operationId=\'prod-task-only-shared-join\',sourceFingerprint=digest(json.dumps(target,sort_keys=True,separators=(\',\',\':\'),ensure_ascii=False).encode(\'utf-8\')),\n                databaseUuid=target[\'databaseServerUuid\'],minioContainerId=\'b\'*64)\n            claim=snapshot.FormalMaintenanceClaim(root/\'claim\',binding)\n            fd=claim._fd;signature=boot._signature(os.fstat(fd))\n            source=approved_source_scope(target_raw,digest(target_raw),claim._plan,target[\'database\'])\n            prefence=dict(serverUuid=binding[\'databaseUuid\'],readOnly=0,superReadOnly=0)\n            sources={};boot_sources={}\n            for name in sorted(producer.PRODUCTION_A_SOURCE_FILES):\n                path=claim.root/name\n                raw=claim._plan if name==\'binding.json\' else snapshot._canonical_bytes(dict(explicitOriginalFixture=name))\n                if name==\'approved-source-target.json\':raw=target_raw\n                if name==\'database-prefence-original.json\':raw=snapshot._canonical_bytes(prefence)\n                if name==\'ingress-plan.json\':raw=b\'{}\'\n                if name.startswith(\'boot-\'):\n                    role=name[5:-14];parent=root/(\'boot-fixture-\'+role);parent.mkdir(mode=0o700)\n                    mapping=dict(schemaVersion=\'release-formal-production-a-boot-source/v1\',role=role,peer=\'local\',\n                        machineIdSha256=\'c\'*64,bootId=host_boot,path=str(parent/\'boot.json\'))\n                    boot_sources[role]=mapping\n                    with boot._directory(parent,no_atime=True) as (_,chain):\n                        raw=snapshot._canonical_bytes(dict(schemaVersion=\'release-formal-production-a-boot-original/v1\',\n                            **{k:mapping[k] for k in (\'role\',\'peer\',\'machineIdSha256\',\'bootId\',\'path\')},\n                            sourceParentIdentity=[list(row) for row in chain],present=False,\n                            originalBase64=None,sha256=None,sourceFileIdentity=None))\n                if name!=\'binding.json\':write(path,raw)\n                sources[name]=str(path)\n            release=dict(schemaVersion=\'release-formal-original-package-binding/v2\',claimBindingSha256=digest(claim._plan),\n                approvedSourceTargetSha256=digest(target_raw),deploymentAttemptId=\'1\'*64,leaseTokenDigest=\'2\'*64,\n                scope=dict(environment=\'prod\'),package=dict(releaseTag=\'task-only-shared-join\',\n                    packageDigest=\'3\'*64,manifestDigest=\'4\'*64,planDigest=\'5\'*64))\n            release_raw=snapshot._canonical_bytes(release);claim._write(claim.root/\'original-release-binding.json\',release_raw)\n            sealed=producer.seal_production_a_source_inventory(claim,release_raw,sources,boot_sources,\n                guard=claim.assert_owned,deadline=time.monotonic()+15)\n            registry=root/\'registry\';registry.mkdir(mode=0o700)\n            key_private=root/\'task-only-private.pem\'\n            openssl(\'genpkey\',\'-algorithm\',\'ED25519\',\'-out\',str(key_private));key_private.chmod(0o600)\n            public=root/\'task-only-public.der\'\n            write(public,openssl(\'pkey\',\'-in\',str(key_private),\'-pubout\',\'-outform\',\'DER\'),0o644)\n            now=int(time.time())\n            payload=dict(sealed[\'binding\'],purpose=\'RESTORE_PRODUCTION_SAVED_ORIGINAL_STATE\',grantId=\'8\'*32,\n                actorId=17,tenantId=122,issuedAt=now-1,expiresAt=now+120,\n                policy=dict(originalStateOnly=True,preserveStoppedAndAbsent=True,\n                    preserveUnfinishedRecords=True,noBootStateWrites=True))\n            message=root/\'task-only-payload.json\';write(message,json.dumps(payload,separators=(\',\',\':\')).encode(\'ascii\'))\n            signature_raw=openssl(\'pkeyutl\',\'-sign\',\'-inkey\',str(key_private),\'-rawin\',\'-in\',str(message))\n            approval=root/\'task-only-approval.json\'\n            envelope=dict(schemaVersion=\'formal-production-a-resume-approval/v1\',keySha256=digest(public.read_bytes()),\n                payloadBase64=base64.b64encode(message.read_bytes()).decode(),\n                signatureBase64=base64.b64encode(signature_raw).decode())\n            write(approval,json.dumps(envelope).encode(\'ascii\'));info=registry.stat()\n            resume=dict(registry=dict(path=str(registry),identity={k:getattr(info,\'st_\'+k)\n                for k in (\'dev\',\'ino\',\'uid\',\'gid\',\'mode\')}),approval=dict(path=str(approval),sha256=digest(approval.read_bytes())),\n                trustedKey=dict(path=str(public),sha256=digest(public.read_bytes())))\n            state=dict(mysql_running=True,frozen=False,flags_restored=False,fault=None,calls=[])\n            def live_owner():\n                claim.assert_owned()\n                assert state[\'mysql_running\'] and not state[\'frozen\'],\'SQL callback during stopped/frozen phase\'\n                state[\'calls\'].append(\'live-owner\')\n                if state[\'fault\']==\'live-after-restore\' and state[\'flags_restored\']:\n                    raise snapshot.FormalSnapshotError(\'TASK_TEST_LIVE_OWNER_REFUSED\')\n            checks=producer.ProductionAResumeChecks(claim,release_raw,Path(sealed[\'path\']).read_bytes(),sealed[\'sha256\'],\n                resume,guard=live_owner,thaw_guard=claim.assert_owned)\n            credential=root/\'mysql-credential\';write(credential,b\'task-only-shared-join-input\\n\')\n            originals={role:dict(path=sources[\'controlled-\'+role+\'-inspect.json\'],\n                source=dict(present=True,containerId=digest(role.encode()))) for role in split.ROLES}\n            actions=controlled.ControlledSharedContainerActions(originals,object())\n            request=dict(schemaVersion=\'release-formal-capture-request/v12\',action=\'capture-shared-payload\',\n                claimRoot=str(claim.root),claimBinding=binding,mysqlContainerId=\'d\'*64,mysqlCredentialFile=str(credential),\n                totalSeconds=120,sharedScopeSha256=digest((claim.root/\'shared-container-original.json\').read_bytes()),\n                originalHealthSha256=digest((claim.root/\'shared-health-original.json\').read_bytes()),ingressRulesSha256=\'f\'*64,\n                database=target[\'database\'],sourceRoot=\'/task/source/minio\',sourceScope=source,\n                dataLimits=dict(max_rows=1000,max_nodes=10000,max_bytes=1048576,seconds=30),\n                rejectionProbe=dict(table=\'task_probe\',column=\'id\'),\n                filesystemFence=dict(mount=\'/task/source\',seconds=3,binding=dict(operationId=binding[\'operationId\'],\n                    bootId=host_boot,mount=dict(source=\'/dev/task-fixture\',device=\'9:1\',fstype=\'ext4\',target=\'/task/source\'))))\n            yield dict(root=root,claim=claim,fd=fd,signature=signature,source=source,prefence=prefence,state=state,\n                checks=checks,actions=actions,request=request,credential=credential,approval=approval,registry=registry)\n            claim.close();claim=None\n    finally:\n        if claim is not None:claim.close()\n        snapshot.observation_deadline.reset(token)\n\n\n@pytest.mark.parametrize(\'fault\',[\'none\',\'sql\',\'mysql-stop\',\'raw\',\'thaw\',\'live-after-restore\',\n    \'mysql-start\',\'flags\',\'writer-start\',\'prefence-drift\',\'approval-drift\',\'changed-request\',\n    \'credential-bytes\',\'credential-inode\',\'credential-mode\',\'closed-owner\'])\ndef test_native_a_phase_join_retains_real_authority_fd_and_refuses_next_action(native_case,monkeypatch,fault):\n    case=native_case;claim=case[\'claim\'];state=case[\'state\'];state[\'fault\']=fault;calls=state[\'calls\']\n    req=case[\'request\'];source=case[\'source\'];root=claim.root\n    shared_raw=(root/\'shared-container-original.json\').read_bytes();health_raw=(root/\'shared-health-original.json\').read_bytes()\n    flows=[dict(source=\'192.0.2.1\',destination=\'192.0.2.2\',ports=[3306])]\n    ingress_state=dict(phase=\'CAPTURE_RESTRICTED\',rulesSha256=req[\'ingressRulesSha256\'],tableHandle=17)\n    monkeypatch.setattr(ingress,\'assert_ingress\',lambda *a,**k:dict(ingress_state))\n    monkeypatch.setattr(ingress,\'render_recovery_policy\',lambda *a:None)\n    monkeypatch.setattr(callers,\'guard_external_callers\',lambda *a:dict(state=\'KNOWN_EXTERNAL_CALLERS_STOPPED_PENDING_FULL_EXCLUSION\'))\n    monkeypatch.setattr(producer,\'verify_shared_request_binding\',lambda *a:dict(shared=shared_raw,health=health_raw))\n    monkeypatch.setattr(producer,\'verify_shared_database_before_stop\',lambda *a:None)\n    class SqlProtocol:\n        def __init__(self,container,*a,**k):self.container=container\n    monkeypatch.setattr(producer,\'PrivateDockerMySql\',SqlProtocol)\n    def source_guard():\n        assert not state[\'frozen\'],\'source callback during frozen phase\'\n        claim.assert_owned();calls.append(\'source\')\n        if fault==\'changed-request\' and any(e[\'step\']==\'db-fenced\' for e in claim.events):req[\'database\']=\'changed\'\n    def thaw(selected):\n        assert selected is claim and not state[\'frozen\'],\'thaw observer during frozen phase\'\n        if fault==\'thaw\' and any(e[\'step\']==\'split-captured\' for e in claim.events):\n            raise snapshot.FormalSnapshotError(\'FORMAL_SHARED_THAW_REQUIRED\')\n        return dict(nativeState=\'THAWED_COPY_ACCEPTED_PENDING_FORMAL_BARRIERS\')\n    monkeypatch.setattr(split,\'_thaw_receipt\',thaw)\n    def stop_writer(selected,role,**kwargs):\n        kwargs[\'guard\']();calls.append(\'stop:\'+role)\n        claim.record(\'shared-\'+role+\'-stop-intent\',dict(explicitProtocol=True))\n        if role==split.STOP_ORDER[0] and fault in (\'credential-bytes\',\'credential-inode\'):\n            credential=case[\'credential\'];raw=credential.read_bytes()\n            if fault==\'credential-inode\':credential.rename(credential.with_suffix(\'.retained\'));write(credential,raw)\n            else:credential.write_bytes(b\'CHANGED_PRIVATE_INPUT\\n\')\n        claim.record(\'shared-\'+role+\'-stopped\',dict(explicitProtocol=True));kwargs[\'guard\']()\n    monkeypatch.setattr(split,\'stop_controlled_shared_writer\',stop_writer)\n    monkeypatch.setattr(split,\'assert_controlled_prefence_stops\',lambda *a,**k:k[\'guard\']())\n    def fence(selected,mysql,*,guard):\n        guard();calls.append(\'fence\');flags=dict(case[\'prefence\'])\n        if fault==\'prefence-drift\':flags[\'readOnly\']=1\n        raw=snapshot._canonical_bytes(flags);claim._write(root/\'database-original.json\',raw)\n        claim.record(\'db-fence-intent\',dict(originalFlags=flags,originalSha256=digest(raw)))\n        guard();claim.record(\'db-fenced\',dict(originalSha256=digest(raw),reconciled=False));guard()\n    monkeypatch.setattr(snapshot,\'acquire_claimed_database_fence\',fence)\n    def sql(*a,**kwargs):\n        kwargs[\'guard\']();calls.append(\'sql\')\n        if fault==\'sql\':raise snapshot.FormalSnapshotError(\'TASK_TEST_SQL_REFUSED\')\n        claim.record(\'split-sql-intent\',{});claim.record(\'split-sql-captured\',{})\n        return dict(state=\'SQL_CAPTURED_PENDING_RAW_AND_FULL_BARRIERS\',sourceScope=source,sourceMigrationEvidence={})\n    monkeypatch.setattr(split,\'capture_sql_phase\',sql)\n    def stop_mysql(selected,role,**kwargs):\n        assert role==\'mysql\';kwargs[\'guard\']();claim.record(\'shared-mysql-stop-intent\',{})\n        kwargs[\'guard\']();calls.append(\'stop:mysql\');state[\'mysql_running\']=False\n        if fault==\'closed-owner\':claim.close()\n        if fault==\'approval-drift\':case[\'approval\'].write_bytes(b\'CHANGED_TASK_ONLY_APPROVAL\')\n        kwargs[\'guard\']()\n        if fault==\'mysql-stop\':raise snapshot.FormalSnapshotError(\'TASK_TEST_STOP_UNCERTAIN\')\n        claim.record(\'shared-mysql-stopped\',{})\n    monkeypatch.setattr(split,\'stop_controlled_capture_database\',stop_mysql)\n    def raw(*a,**kwargs):\n        kwargs[\'guard\']();calls.append(\'raw\');state[\'frozen\']=True\n        kwargs[\'native_guard\']();state[\'frozen\']=False\n        if fault==\'raw\':raise snapshot.FormalSnapshotError(\'TASK_TEST_RAW_REFUSED\')\n        claim.record(\'split-raw-intent\',{});claim.record(\'split-captured\',{});kwargs[\'guard\']()\n        return dict(state=\'CAPTURED_PENDING_FULL_BARRIER_PROOF\',sourceScope=source,sourceMigrationEvidence={})\n    monkeypatch.setattr(split,\'capture_raw_phase\',raw)\n    def transition(selected,value,*,guard,thaw_guard):\n        guard();thaw_guard();calls.append(\'transition\');claim.record(\'ingress-recovery-intent\',{})\n        rules=snapshot._canonical_bytes([dict(table=dict(handle=18))]);claim._write(root/\'ingress-recovery-installed.json\',rules)\n        ingress_state.update(phase=\'RECOVERY_DEPENDENCIES_PERMITTED\',tableHandle=18,rulesSha256=digest(rules))\n        claim.record(\'ingress-recovery-permitted\',dict(rulesSha256=digest(rules),planSha256=digest(snapshot._canonical_bytes(value))))\n        guard();thaw_guard();return dict(ingress_state)\n    monkeypatch.setattr(ingress,\'permit_recovery_dependencies\',transition)\n    def begin(selected,*,guard,thaw_guard):\n        guard();thaw_guard();claim.record(\'shared-recovery-intent\',{})\n    monkeypatch.setattr(split,\'begin_shared_recovery\',begin)\n    def start(selected,role,action,**kwargs):\n        assert action==\'start\';kwargs[\'guard\']();calls.append(\'start:\'+role)\n        if fault==\'mysql-start\' and role==\'mysql\' or fault==\'writer-start\' and role==\'redis\':\n            claim.record(\'shared-\'+role+\'-restore-intent\',{})\n            raise snapshot.FormalSnapshotError(\'TASK_TEST_START_UNCERTAIN\')\n        if role==\'mysql\':state[\'mysql_running\']=True\n        else:assert state[\'flags_restored\'] and \'live-owner\' in calls[calls.index(\'flags\')+1:]\n        claim.record(\'shared-\'+role+\'-restored\',{});kwargs[\'guard\']()\n    monkeypatch.setattr(split,\'_shared_action\',start)\n    def flags(selected,mysql,*,guard,thaw_guard):\n        guard();calls.append(\'flags\')\n        if fault==\'flags\':raise snapshot.FormalSnapshotError(\'TASK_TEST_FLAGS_UNCERTAIN\')\n        state[\'flags_restored\']=True;claim.record(\'shared-db-restored\',{});guard()\n    monkeypatch.setattr(split,\'restore_shared_database_flags\',flags)\n    monkeypatch.setattr(split,\'verify_shared_restoration\',lambda *a,**k:dict(state=\'ORIGINAL_SERVICES_RESTORED_PENDING_CALLER_PROOF\'))\n    if fault==\'credential-mode\':case[\'credential\'].chmod(0o666)\n    call=lambda:producer.run_production_a_shared_lifecycle(claim,req,approved_source=source,\n        resume_checks=case[\'checks\'],container_actions=case[\'actions\'],source_guard=source_guard,\n        maintenance_end=time.time()+90,recovery_flows=flows)\n    if fault==\'none\':\n        result=call()\n        order=[\'stop:\'+r for r in split.STOP_ORDER[:-1]]+[\'fence\',\'sql\',\'stop:mysql\',\'raw\',\'transition\',\n            \'start:mysql\',\'flags\']+[\'start:\'+r for r in split.RESTORE_ORDER[1:]]\n        assert [c for c in calls if c.startswith((\'stop:\',\'start:\')) or c in (\'fence\',\'sql\',\'raw\',\'transition\',\'flags\')]==order\n        assert result[\'claimReleased\'] is False and result[\'fullRunnerCompleted\'] is False and result[\'productionEvidence\'] is False\n    else:\n        expected=dict(sql=\'TASK_TEST_SQL_REFUSED\',**{\n            \'mysql-stop\':\'TASK_TEST_STOP_UNCERTAIN\',\'raw\':\'TASK_TEST_RAW_REFUSED\',\n            \'thaw\':\'FORMAL_SHARED_THAW_REQUIRED\',\'live-after-restore\':\'TASK_TEST_LIVE_OWNER_REFUSED\',\n            \'mysql-start\':\'TASK_TEST_START_UNCERTAIN\',\'flags\':\'TASK_TEST_FLAGS_UNCERTAIN\',\n            \'writer-start\':\'TASK_TEST_START_UNCERTAIN\',\'prefence-drift\':\'FORMAL_PRODUCTION_A_DATABASE_ORIGINAL_CHANGED\',\n            \'approval-drift\':\'FORMAL_PRODUCTION_A_RESUME_CONTROL_CHANGED\',\n            \'changed-request\':\'FORMAL_PRODUCTION_A_SHARED_INPUT_CHANGED\',\n            \'credential-bytes\':\'FORMAL_PRODUCTION_A_SHARED_CREDENTIAL_CHANGED\',\n            \'credential-inode\':\'FORMAL_PRODUCTION_A_SHARED_CREDENTIAL_CHANGED\',\n            \'credential-mode\':\'FORMAL_BOOT_FILE_UNTRUSTED\',\n            \'closed-owner\':\'FORMAL_PRODUCTION_A_RESUME_CONTROL_CHANGED\'})\n        with pytest.raises(snapshot.FormalSnapshotError,match=\'^\'+expected[fault]+\'$\'):call()\n        # Refusal must reach this scenario\'s intended seam; an earlier typed\n        # error is a failure, not evidence of safe uncertainty handling.\n        reached={\'sql\':\'sql\',\'mysql-stop\':\'stop:mysql\',\'raw\':\'raw\',\'thaw\':\'raw\',\n            \'live-after-restore\':\'flags\',\'mysql-start\':\'start:mysql\',\'flags\':\'flags\',\n            \'writer-start\':\'start:redis\',\'prefence-drift\':\'fence\',\'approval-drift\':\'stop:mysql\',\n            \'changed-request\':\'fence\',\'credential-bytes\':\'stop:frontend\',\'credential-inode\':\'stop:frontend\',\n            \'closed-owner\':\'stop:mysql\'}\n        if fault in reached:assert reached[fault] in calls\n        if fault not in (\'live-after-restore\',\'mysql-start\',\'flags\',\'writer-start\'):\n            assert not any(c.startswith(\'start:\') for c in calls)\n        if fault in (\'live-after-restore\',\'mysql-start\',\'flags\'):\n            assert not any(c.startswith(\'start:\') and c!=\'start:mysql\' for c in calls)\n        if fault==\'credential-mode\':assert not any(c.startswith(\'stop:\') for c in calls)\n    assert len(list(case[\'registry\'].iterdir()))==1\n    assert sum(e[\'step\']==\'production-a-resume-consumed\' for e in claim.events)==1\n    if fault==\'closed-owner\':assert claim._fd is None\n    else:\n        assert claim._fd==case[\'fd\'] and boot._signature(os.fstat(claim._fd))==case[\'signature\']\n        with pytest.raises(snapshot.FormalSnapshotError,match=\'FORMAL_MAINTENANCE_BUSY\'):\n            snapshot.FormalMaintenanceClaim(claim.root,json.loads(claim._plan),resume=True)\n'



@pytest.mark.parametrize('fault',['none','shared-health','external','external-readback','host-first',
    'host-second','host-prefix','ingress','uncertain-release','changed-content','owner-fd','control',
    'live-owner','wrong-shared','unfinished-shared','replay','archive-ingress','archive-health',
    'post-release-health','terminal-readback'])
def test_production_a_original_participants_recover_with_retained_archive_guard(tmp_path,monkeypatch,fault):
    """Existing Windows orchestration profile; no actual flock/service/clock proof."""
    import copy
    m=load()
    if os.name=='nt' and 'fcntl' not in sys.modules:
        boundary=types.ModuleType('fcntl')
        boundary.flock=lambda *a:(_ for _ in ()).throw(AssertionError('native flock outside protocol contract'))
        monkeypatch.setitem(sys.modules,'fcntl',boundary)
    import formal_split_capture as split
    import formal_service_health as health
    import formal_external_callers as external
    import formal_host_admission as host
    import formal_ingress_control as ingress
    calls=[];state=dict(content=True,control=True,live=True,released=False,health_busy=False,fd=41,archive_fault_armed=False)
    class Claim:
        root=tmp_path;_plan=b'EXPLICIT_CLAIM_PROTOCOL';_fd=41
        events=[dict(step=s,details={}) for s in ['split-captured','shared-db-restored']+
            ['shared-'+r+'-restored' for r in split.RESTORE_ORDER]]
        def assert_owned(self):
            if self._fd!=state['fd']:raise m.FormalSnapshotError('TASK_RESTORATION_OWNER_CHANGED')
        def record(self,step,details):self.events.append(dict(step=step,details=details))
    claim=Claim();monkeypatch.setattr(m,'FormalMaintenanceClaim',Claim)
    checks=object.__new__(m.ProductionAResumeChecks);checks.claim=claim;checks.consumed={'explicitProtocol':True}
    def control():
        calls.append('control');claim.assert_owned()
        if not state['control']:raise m.FormalSnapshotError('TASK_RESTORATION_CONTROL_CHANGED')
    def live(_):
        calls.append('live');claim.assert_owned()
        if not state['live']:raise m.FormalSnapshotError('TASK_RESTORATION_LIVE_CHANGED')
    monkeypatch.setattr(m.ProductionAResumeChecks,'control_phase_guard',lambda *a,**k:control)
    monkeypatch.setattr(m.ProductionAResumeChecks,'__call__',live)
    monkeypatch.setattr(m,'require_native_runtime',lambda:None)
    monkeypatch.setattr(m.os,'geteuid',lambda:0,raising=False)
    def content():
        calls.append('content')
        if not state['content']:raise m.FormalSnapshotError('TASK_RESTORATION_CONTENT_CHANGED')
    def thaw():calls.append('thaw')
    original={u:dict(loadState='loaded',mask=None,persistentMask=None,active='inactive',sub='dead',
        mainPid=0,controlPid=0,unitFileState='enabled',configurationSha256='a'*64,
        configurationFiles=[],loadedConfigurationPaths=['/usr/lib/systemd/system/'+u],linksSha256='b'*64,
        stopContractSha256='c'*64,timerSemanticsSha256='d'*64 if u.endswith('.timer') else None)
        for u in host.CLOSE_ORDER}
    originals=dict(host={'explicitCapabilities':True},units=copy.deepcopy(original))
    current=copy.deepcopy(original)
    for u in host.CLOSE_ORDER:
        mask=dict(explicitOwnedMask=u);current[u].update(mask=mask,loadState='masked',
            loadedConfigurationPaths=['/dev/null'])
        claim.record(host._step(u,'-closed'),dict(ownedMask=mask))
    class HostNative:
        def capabilities(self,deadline):return dict(originals['host'])
        def observe(self,unit,deadline,expected):
            calls.append('observe-host:'+unit);return copy.deepcopy(current[unit])
    native=HostNative();transport=object();mysql=object();probe=object();nft=object()
    monkeypatch.setattr(host,'_bound',lambda selected:copy.deepcopy(originals))
    def forbidden(*a,**k):raise AssertionError('stopped-only observer crossed recovery intent')
    monkeypatch.setattr(external,'guard_external_callers',forbidden)
    def shared_health(selected,db,*,guard,thaw_guard,**kwargs):
        assert selected is claim and db is mysql and kwargs['probe'] is probe
        assert not state['health_busy'],'recursive shared health guard'
        state['health_busy']=True
        try:
            guard();thaw_guard();calls.append('shared-health')
            if fault=='shared-health' or fault=='post-release-health' and state['released'] or \
                fault=='archive-health' and state['archive_fault_armed']:
                raise m.FormalSnapshotError('TASK_RESTORATION_HEALTH_CHANGED')
            if fault=='control':state['control']=False
            if fault=='live-owner':state['live']=False
            guard()
            return dict(state='ORIGINAL_SERVICES_HEALTHY',productionEvidence=False,
                health=dict(explicitReadback='after-release' if state['released'] else 'before-release'))
        finally:state['health_busy']=False
    monkeypatch.setattr(health,'observe_shared_recovery_health',shared_health)
    def restore_external(selected,actual,*,dependency_guard,**kwargs):
        assert selected is claim and actual is transport
        dependency_guard();calls.append('external');claim.record('external-recovery-intent',{})
        if fault=='changed-content':state['content']=False
        if fault=='owner-fd':claim._fd=99
        dependency_guard()
        if fault=='external':raise m.FormalSnapshotError('TASK_RESTORATION_EXTERNAL_CHANGED')
        return dict(state='EXTERNAL_CALLERS_HEALTHY_INGRESS_HELD',productionEvidence=False,claimReleased=False,
            health=dict(explicitReadback='after-release' if state['released'] else 'before-release'))
    def external_health(selected,actual,*,dependency_guard,**kwargs):
        assert selected is claim and actual is transport
        dependency_guard();calls.append('external-health')
        if fault=='external-readback':raise m.FormalSnapshotError('TASK_RESTORATION_EXTERNAL_HEALTH_CHANGED')
        return dict(state='EXTERNAL_CALLERS_HEALTHY_INGRESS_HELD',productionEvidence=False,claimReleased=False,
            health=dict(explicitReadback='after-release' if state['released'] else 'before-release'))
    monkeypatch.setattr(external,'restore_external_services_with_health',restore_external)
    monkeypatch.setattr(external,'observe_external_restoration_health',external_health)
    def restore_host(selected,unit,*,guard,recovery_guard,**kwargs):
        assert selected is claim and kwargs['native'] is native
        guard();recovery_guard();calls.append('restore-host:'+unit)
        claim.record(host._step(unit,'-restore-intent'),{})
        if fault=='host-first' and unit==host.RESTORE_ORDER[0] or fault=='host-second' and unit==host.RESTORE_ORDER[1]:
            raise m.FormalSnapshotError('TASK_RESTORATION_HOST_UNCERTAIN')
        current[unit]=copy.deepcopy(original[unit]);guard();recovery_guard()
        claim.record(host._step(unit,'-restored'),{});guard()
        if fault=='host-prefix' and unit==host.RESTORE_ORDER[0]:current[unit]['controlPid']=907
        return dict(state='HOST_ENTRY_RESTORED_DERIVED_WORK_PENDING')
    monkeypatch.setattr(host,'restore_host_admission',restore_host)
    def held(selected,*,native):
        assert selected is claim and native is nft
        assert not state['released'],'held-only observer used after actual release'
        return dict(phase='RECOVERY_DEPENDENCIES_PERMITTED',rulesSha256='e'*64,table='owned-table',tableHandle=18)
    def release(selected,health_guard,*,native):
        assert selected is claim and native is nft
        health_guard();calls.append('release-ingress');claim.record('ingress-release-intent',{})
        if fault=='ingress':raise m.FormalSnapshotError('TASK_RESTORATION_INGRESS_FAILED')
        state['released']=True
        if fault=='uncertain-release':raise m.FormalSnapshotError('TASK_RESTORATION_INGRESS_UNCERTAIN')
        claim.record('ingress-released',dict(table='owned-table'))
        return dict(state='INGRESS_RELEASED',table='owned-table')
    def released(selected,*,native):
        assert selected is claim and native is nft
        return dict(state='RELEASED',present=False,table='owned-table',actionReplayed=False)
    monkeypatch.setattr(ingress,'assert_ingress',held)
    monkeypatch.setattr(ingress,'release_ingress',release)
    monkeypatch.setattr(ingress,'observe_ingress_reconciliation',released)
    shared=dict(state='A_SHARED_CAPTURED_AND_ORIGINAL_SERVICES_RESTORED_PENDING_REMAINING_OWNER',
        ingressRulesSha256='e'*64,productionEvidence=False,fullRunnerCompleted=False,releaseReady=False,claimReleased=False)
    if fault=='wrong-shared':shared['state']='NOT_CAPTURED'
    if fault=='unfinished-shared':claim.events=[e for e in claim.events if e['step']!='shared-db-restored']
    if fault=='replay':claim.record('external-recovery-intent',{})
    def run():
        if fault.startswith('compose-'):
            request=object();source=object();actions=object();flows=object();source_guard=lambda:None
            retained_deadline=time.monotonic()+30;maintenance_end=time.time()+60
            def capture(selected,actual,**kwargs):
                assert selected is claim and actual is request
                assert kwargs==dict(approved_source=source,resume_checks=checks,container_actions=actions,
                    source_guard=source_guard,maintenance_end=maintenance_end,recovery_flows=flows)
                calls.append('shared-capture')
                if fault=='compose-shared-failure':raise m.FormalSnapshotError('TASK_COMPOSE_CAPTURE_FAILED')
                if fault=='compose-content-failure':state['content']=False
                if fault=='compose-expired':return shared,content,thaw,mysql,time.monotonic()-1
                if fault=='compose-bundle':return shared
                return shared,content,thaw,mysql,retained_deadline
            monkeypatch.setattr(m,'run_production_a_shared_lifecycle',capture)
            wrapped,archive=m.run_production_a_original_capture_recovery(claim,request,
                approved_source=source,resume_checks=checks,container_actions=actions,source_guard=source_guard,
                maintenance_end=maintenance_end,recovery_flows=flows,external_transport=transport,
                host_native=native,health_probe=probe,ingress_native=nft)
            assert wrapped['state']=='A_ORIGINAL_CAPTURE_AND_RECOVERY_COMPLETED_PENDING_NAS'
            assert wrapped['sharedCapture'] is shared
            assert all(wrapped[k] is False for k in ('productionEvidence','releaseReady','fullRunnerCompleted','claimReleased','nasArchiveVerified'))
            assert calls.index('shared-capture')<calls.index('content')<calls.index('external')
            assert callable(archive)
            return wrapped['originalParticipants'],archive
        return m.restore_production_a_original_participants(claim,mysql,shared,
            resume_checks=checks,original_content_guard=content,thaw_guard=thaw,external_transport=transport,
            host_native=native,health_probe=probe,ingress_native=nft,maintenance_end=time.time()+60,
            deadline=time.monotonic()+30)
    errors={'shared-health':'TASK_RESTORATION_HEALTH_CHANGED','external':'TASK_RESTORATION_EXTERNAL_CHANGED',
        'external-readback':'TASK_RESTORATION_EXTERNAL_HEALTH_CHANGED','host-first':'TASK_RESTORATION_HOST_UNCERTAIN',
        'host-second':'TASK_RESTORATION_HOST_UNCERTAIN','host-prefix':'FORMAL_HOST_PREVIOUS_ENTRY_CHANGED',
        'ingress':'TASK_RESTORATION_INGRESS_FAILED','uncertain-release':'TASK_RESTORATION_INGRESS_UNCERTAIN',
        'changed-content':'TASK_RESTORATION_CONTENT_CHANGED','owner-fd':'TASK_RESTORATION_OWNER_CHANGED',
        'control':'TASK_RESTORATION_CONTROL_CHANGED','live-owner':'TASK_RESTORATION_LIVE_CHANGED',
        'wrong-shared':'FORMAL_PRODUCTION_A_RESTORATION_PREREQUISITE_REQUIRED',
        'unfinished-shared':'FORMAL_PRODUCTION_A_RESTORATION_PREREQUISITE_REQUIRED',
        'replay':'FORMAL_PRODUCTION_A_RESTORATION_REPLAY_REFUSED',
        'post-release-health':'TASK_RESTORATION_HEALTH_CHANGED',
        'compose-shared-failure':'TASK_COMPOSE_CAPTURE_FAILED',
        'compose-content-failure':'TASK_RESTORATION_CONTENT_CHANGED',
        'compose-bundle':'FORMAL_PRODUCTION_A_CAPTURE_RECOVERY_INVALID',
        'compose-expired':'FORMAL_CAPTURE_DEADLINE_EXCEEDED'}
    if fault in errors:
        with pytest.raises(m.FormalSnapshotError,match='^'+errors[fault]+'$'):run()
        if fault in ('shared-health','control','live-owner','wrong-shared','unfinished-shared','replay'):
            assert 'external' not in calls
        if fault in ('external','external-readback','changed-content','owner-fd'):
            assert not any(c.startswith('restore-host:') for c in calls)
        if fault.startswith('host-'):assert 'release-ingress' not in calls
        if fault=='host-prefix':assert 'restore-host:'+host.RESTORE_ORDER[1] not in calls
        if fault=='uncertain-release':assert not any(e['step']=='ingress-released' for e in claim.events)
        if fault=='post-release-health':
            assert state['released'] and any(e['step']=='ingress-released' for e in claim.events)
            assert claim._fd==41
        if fault.startswith('compose-'):
            assert 'external' not in calls and 'release-ingress' not in calls and claim._fd==41
    else:
        receipt,archive_guard=run()
        assert receipt['state']=='ORIGINAL_PARTICIPANTS_RESTORED_INGRESS_RELEASED_PENDING_NAS'
        assert all(receipt[k] is False for k in ('productionEvidence','releaseReady','fullRunnerCompleted','claimReleased','nasArchiveVerified'))
        assert [c for c in calls if c.startswith('restore-host:')]==['restore-host:'+u for u in host.RESTORE_ORDER]
        assert calls.index('external')<calls.index('restore-host:'+host.RESTORE_ORDER[0])<calls.index('release-ingress')
        assert claim._fd==41 and callable(archive_guard)
        if fault=='terminal-readback':
            assert receipt['sharedHealth']['health']['explicitReadback']=='after-release'
            assert receipt['externalHealth']['health']['explicitReadback']=='after-release'
        if fault=='archive-ingress':
            monkeypatch.setattr(ingress,'observe_ingress_reconciliation',lambda *a,**k:dict(state='HELD',present=True,table='owned-table',actionReplayed=False))
            with pytest.raises(m.FormalSnapshotError,match='^FORMAL_PRODUCTION_A_RESTORATION_INGRESS_CHANGED$'):archive_guard()
        elif fault=='archive-health':
            state['archive_fault_armed']=True
            with pytest.raises(m.FormalSnapshotError,match='^TASK_RESTORATION_HEALTH_CHANGED$'):archive_guard()
        else:archive_guard();assert claim._fd==41


@pytest.mark.parametrize('fault',['compose-none','compose-shared-failure','compose-content-failure',
    'compose-bundle','compose-expired'])
def test_production_a_capture_recovery_consumes_same_epoch_handoff(tmp_path,monkeypatch,fault):
    test_production_a_original_participants_recover_with_retained_archive_guard(tmp_path,monkeypatch,fault)


def test_production_a_original_participants_unsupported_native_refuses_before_inputs():
    m=load()
    with pytest.raises(m.WatchdogError,match='WATCHDOG_RUNTIME_UNSUPPORTED'):
        m.restore_production_a_original_participants(None,None,None,resume_checks=None,
            original_content_guard=None,thaw_guard=None,external_transport=None,host_native=None,
            health_probe=None,ingress_native=None,maintenance_end=None,deadline=None)


def test_production_a_shared_lifecycle_prepared_v3_has_actual_credential_and_phase_budget(tmp_path):
    """Validate exact prepared fixture literals without invoking native fixtures."""
    import ast
    tree=ast.parse(NATIVE_A_SHARED_LIFECYCLE_TESTS_V3)
    fn=next(n for n in tree.body if isinstance(n,ast.FunctionDef) and n.name=='native_case')
    tokens=[n for n in ast.walk(fn) if isinstance(n,ast.Call) and isinstance(n.func,ast.Attribute)
        and n.func.attr=='set' and n.args and isinstance(n.args[0],ast.BinOp)]
    assert len(tokens)==1 and ast.literal_eval(tokens[0].args[0].right)==45
    # Production reserves freeze seconds + 20 for its pre-capture phases;
    # the outer fixture's former 20 seconds could never satisfy that contract.
    requests=[n.value for n in ast.walk(fn) if isinstance(n,ast.Assign)
        and len(n.targets)==1 and isinstance(n.targets[0],ast.Name) and n.targets[0].id=='request']
    assert len(requests)==1
    fence=next(k.value for k in requests[0].keywords if k.arg=='filesystemFence')
    seconds=ast.literal_eval(next(k.value for k in fence.keywords if k.arg=='seconds'))
    assert 45>seconds+20
    credentials=[n.args[1].value for n in ast.walk(fn) if isinstance(n,ast.Call)
        and isinstance(n.func,ast.Name) and n.func.id=='write' and len(n.args)>1
        and isinstance(n.args[1],ast.Constant) and isinstance(n.args[1].value,bytes)
        and b'shared-join-input' in n.args[1].value]
    assert len(credentials)==1
    path=tmp_path/'actual-prepared-credential';path.write_bytes(credentials[0]);path.chmod(0o600)
    assert load().read_credential(path)=='task-only-shared-join-input'

NATIVE_A_SHARED_LIFECYCLE_TESTS_V4 = '"""Real Linux FD/flock/control pins/signature; explicit service/SQL/nft actors.\n\nThis tests the new phase wiring, not actual SQL, Docker, thaw or host protection.\nOriginal contents, five machine mappings and signed administrator are task-only\nfixtures. No actual daemon/service/SSH/freeze/server/NAS actions occur.\n"""\nimport base64\nimport hashlib\nimport json\nimport os\nfrom pathlib import Path\nimport stat\nimport subprocess\nimport sys\nimport tempfile\nimport time\n\nimport pytest\n\nsys.path.insert(0,\'/workspace/linux\')\nimport formal_capture_producer as producer\nimport formal_snapshot_linux as snapshot\nimport formal_boot_state as boot\nimport formal_split_capture as split\nimport formal_ingress_control as ingress\nimport formal_external_callers as callers\nimport formal_controlled_container_sources as controlled\nfrom formal_source_migration_ledger import approved_source_scope\n\n\ndef digest(raw):return hashlib.sha256(raw).hexdigest()\n\n\ndef write(path,raw,mode=0o600):\n    with path.open(\'xb\') as out:out.write(raw)\n    path.chmod(mode)\n\n\ndef openssl(*args):\n    result=subprocess.run([\'/usr/bin/openssl\',*args],capture_output=True,timeout=5)\n    assert result.returncode==0 and not result.stderr,\'TASK_TEST_OPENSSL_FAILED\'\n    return result.stdout\n\n\n@pytest.fixture\ndef native_case():\n    assert sys.platform==\'linux\' and os.geteuid()==0\n    producer.require_native_runtime()\n    token=snapshot.observation_deadline.set(time.monotonic()+45)\n    claim=None\n    try:\n        with tempfile.TemporaryDirectory(prefix=\'p2-shared-join-\',dir=\'/control\') as directory:\n            root=Path(directory);host_boot=Path(\'/proc/sys/kernel/random/boot_id\').read_text().strip()\n            target=dict(schemaVersion=\'release-formal-target/v1\',environment=\'prod\',host=\'192.0.2.17\',\n                machineId=\'6\'*32,project=\'task-only-project\',root=\'/task/source\',dataMount=\'/task/source\',\n                dataDevice=\'/dev/task-fixture\',dataRoot=\'/task/source/data\',releaseRoot=\'/task/release\',\n                frontendPort=81,backendPort=82,database=\'task_probe\',\n                databaseServerUuid=\'0fa6c22b-5a90-11f1-ade1-0242ac150002\',\n                containers={role:\'task-only-\'+role for role in (\'mysql\',\'redis\',\'minio\',\'backend\',\'frontend\')})\n            target_raw=snapshot._canonical_bytes(target)\n            binding=dict(operationId=\'prod-task-only-shared-join\',sourceFingerprint=digest(json.dumps(target,sort_keys=True,separators=(\',\',\':\'),ensure_ascii=False).encode(\'utf-8\')),\n                databaseUuid=target[\'databaseServerUuid\'],minioContainerId=\'b\'*64)\n            claim=snapshot.FormalMaintenanceClaim(root/\'claim\',binding)\n            fd=claim._fd;signature=boot._signature(os.fstat(fd))\n            source=approved_source_scope(target_raw,digest(target_raw),claim._plan,target[\'database\'])\n            prefence=dict(serverUuid=binding[\'databaseUuid\'],readOnly=0,superReadOnly=0)\n            sources={};boot_sources={}\n            for name in sorted(producer.PRODUCTION_A_SOURCE_FILES):\n                path=claim.root/name\n                raw=claim._plan if name==\'binding.json\' else snapshot._canonical_bytes(dict(explicitOriginalFixture=name))\n                if name==\'approved-source-target.json\':raw=target_raw\n                if name==\'database-prefence-original.json\':raw=snapshot._canonical_bytes(prefence)\n                if name==\'ingress-plan.json\':raw=b\'{}\'\n                if name.startswith(\'boot-\'):\n                    role=name[5:-14];parent=root/(\'boot-fixture-\'+role);parent.mkdir(mode=0o700)\n                    mapping=dict(schemaVersion=\'release-formal-production-a-boot-source/v1\',role=role,peer=\'local\',\n                        machineIdSha256=\'c\'*64,bootId=host_boot,path=str(parent/\'boot.json\'))\n                    boot_sources[role]=mapping\n                    with boot._directory(parent,no_atime=True) as (_,chain):\n                        raw=snapshot._canonical_bytes(dict(schemaVersion=\'release-formal-production-a-boot-original/v1\',\n                            **{k:mapping[k] for k in (\'role\',\'peer\',\'machineIdSha256\',\'bootId\',\'path\')},\n                            sourceParentIdentity=[list(row) for row in chain],present=False,\n                            originalBase64=None,sha256=None,sourceFileIdentity=None))\n                if name!=\'binding.json\':write(path,raw)\n                sources[name]=str(path)\n            release=dict(schemaVersion=\'release-formal-original-package-binding/v2\',claimBindingSha256=digest(claim._plan),\n                approvedSourceTargetSha256=digest(target_raw),deploymentAttemptId=\'1\'*64,leaseTokenDigest=\'2\'*64,\n                scope=dict(environment=\'prod\'),package=dict(releaseTag=\'task-only-shared-join\',\n                    packageDigest=\'3\'*64,manifestDigest=\'4\'*64,planDigest=\'5\'*64))\n            release_raw=snapshot._canonical_bytes(release);claim._write(claim.root/\'original-release-binding.json\',release_raw)\n            sealed=producer.seal_production_a_source_inventory(claim,release_raw,sources,boot_sources,\n                guard=claim.assert_owned,deadline=time.monotonic()+15)\n            registry=root/\'registry\';registry.mkdir(mode=0o700)\n            key_private=root/\'task-only-private.pem\'\n            openssl(\'genpkey\',\'-algorithm\',\'ED25519\',\'-out\',str(key_private));key_private.chmod(0o600)\n            public=root/\'task-only-public.der\'\n            write(public,openssl(\'pkey\',\'-in\',str(key_private),\'-pubout\',\'-outform\',\'DER\'),0o644)\n            now=int(time.time())\n            payload=dict(sealed[\'binding\'],purpose=\'RESTORE_PRODUCTION_SAVED_ORIGINAL_STATE\',grantId=\'8\'*32,\n                actorId=17,tenantId=122,issuedAt=now-1,expiresAt=now+120,\n                policy=dict(originalStateOnly=True,preserveStoppedAndAbsent=True,\n                    preserveUnfinishedRecords=True,noBootStateWrites=True))\n            message=root/\'task-only-payload.json\';write(message,json.dumps(payload,separators=(\',\',\':\')).encode(\'ascii\'))\n            signature_raw=openssl(\'pkeyutl\',\'-sign\',\'-inkey\',str(key_private),\'-rawin\',\'-in\',str(message))\n            approval=root/\'task-only-approval.json\'\n            envelope=dict(schemaVersion=\'formal-production-a-resume-approval/v1\',keySha256=digest(public.read_bytes()),\n                payloadBase64=base64.b64encode(message.read_bytes()).decode(),\n                signatureBase64=base64.b64encode(signature_raw).decode())\n            write(approval,json.dumps(envelope).encode(\'ascii\'));info=registry.stat()\n            resume=dict(registry=dict(path=str(registry),identity={k:getattr(info,\'st_\'+k)\n                for k in (\'dev\',\'ino\',\'uid\',\'gid\',\'mode\')}),approval=dict(path=str(approval),sha256=digest(approval.read_bytes())),\n                trustedKey=dict(path=str(public),sha256=digest(public.read_bytes())))\n            state=dict(mysql_running=True,frozen=False,flags_restored=False,fault=None,calls=[])\n            def live_owner():\n                claim.assert_owned()\n                assert state[\'mysql_running\'] and not state[\'frozen\'],\'SQL callback during stopped/frozen phase\'\n                state[\'calls\'].append(\'live-owner\')\n                if state[\'fault\']==\'live-after-restore\' and state[\'flags_restored\']:\n                    raise snapshot.FormalSnapshotError(\'TASK_TEST_LIVE_OWNER_REFUSED\')\n            checks=producer.ProductionAResumeChecks(claim,release_raw,Path(sealed[\'path\']).read_bytes(),sealed[\'sha256\'],\n                resume,guard=live_owner,thaw_guard=claim.assert_owned)\n            credential=root/\'mysql-credential\';write(credential,b\'task-only-shared-join-input\\n\')\n            originals={role:dict(path=sources[\'controlled-\'+role+\'-inspect.json\'],\n                source=dict(present=True,containerId=digest(role.encode()))) for role in split.ROLES}\n            actions=controlled.ControlledSharedContainerActions(originals,object())\n            request=dict(schemaVersion=\'release-formal-capture-request/v12\',action=\'capture-shared-payload\',\n                claimRoot=str(claim.root),claimBinding=binding,mysqlContainerId=\'d\'*64,mysqlCredentialFile=str(credential),\n                totalSeconds=120,sharedScopeSha256=digest((claim.root/\'shared-container-original.json\').read_bytes()),\n                originalHealthSha256=digest((claim.root/\'shared-health-original.json\').read_bytes()),ingressRulesSha256=\'f\'*64,\n                database=target[\'database\'],sourceRoot=\'/task/source/minio\',sourceScope=source,\n                dataLimits=dict(max_rows=1000,max_nodes=10000,max_bytes=1048576,seconds=30),\n                rejectionProbe=dict(table=\'task_probe\',column=\'id\'),\n                filesystemFence=dict(mount=\'/task/source\',seconds=3,binding=dict(operationId=binding[\'operationId\'],\n                    bootId=host_boot,mount=dict(source=\'/dev/task-fixture\',device=\'9:1\',fstype=\'ext4\',target=\'/task/source\'))))\n            yield dict(root=root,claim=claim,fd=fd,signature=signature,source=source,prefence=prefence,state=state,\n                checks=checks,actions=actions,request=request,credential=credential,approval=approval,registry=registry)\n            claim.close();claim=None\n    finally:\n        if claim is not None:claim.close()\n        snapshot.observation_deadline.reset(token)\n\n\n@pytest.mark.parametrize(\'fault\',[\'none\',\'sql\',\'mysql-stop\',\'raw\',\'thaw\',\'live-after-restore\',\n    \'mysql-start\',\'flags\',\'writer-start\',\'prefence-drift\',\'approval-drift\',\'changed-request\',\n    \'credential-bytes\',\'credential-inode\',\'credential-mode\',\'closed-owner\'])\ndef test_native_a_phase_join_retains_real_authority_fd_and_refuses_next_action(native_case,monkeypatch,fault):\n    case=native_case;claim=case[\'claim\'];state=case[\'state\'];state[\'fault\']=fault;calls=state[\'calls\']\n    req=case[\'request\'];source=case[\'source\'];root=claim.root\n    shared_raw=(root/\'shared-container-original.json\').read_bytes();health_raw=(root/\'shared-health-original.json\').read_bytes()\n    flows=[dict(source=\'192.0.2.1\',destination=\'192.0.2.2\',ports=[3306])]\n    ingress_state=dict(phase=\'CAPTURE_RESTRICTED\',rulesSha256=req[\'ingressRulesSha256\'],tableHandle=17)\n    monkeypatch.setattr(ingress,\'assert_ingress\',lambda *a,**k:dict(ingress_state))\n    monkeypatch.setattr(ingress,\'render_recovery_policy\',lambda *a:None)\n    monkeypatch.setattr(callers,\'guard_external_callers\',lambda *a:dict(state=\'KNOWN_EXTERNAL_CALLERS_STOPPED_PENDING_FULL_EXCLUSION\'))\n    monkeypatch.setattr(producer,\'verify_shared_request_binding\',lambda *a:dict(shared=shared_raw,health=health_raw))\n    monkeypatch.setattr(producer,\'verify_shared_database_before_stop\',lambda *a:None)\n    class SqlProtocol:\n        def __init__(self,container,*a,**k):self.container=container\n    monkeypatch.setattr(producer,\'PrivateDockerMySql\',SqlProtocol)\n    def source_guard():\n        assert not state[\'frozen\'],\'source callback during frozen phase\'\n        claim.assert_owned();calls.append(\'source\')\n        if fault==\'changed-request\' and any(e[\'step\']==\'db-fenced\' for e in claim.events):req[\'database\']=\'changed\'\n    def thaw(selected):\n        assert selected is claim and not state[\'frozen\'],\'thaw observer during frozen phase\'\n        if fault==\'thaw\' and any(e[\'step\']==\'split-captured\' for e in claim.events):\n            raise snapshot.FormalSnapshotError(\'FORMAL_SHARED_THAW_REQUIRED\')\n        return dict(nativeState=\'THAWED_COPY_ACCEPTED_PENDING_FORMAL_BARRIERS\')\n    monkeypatch.setattr(split,\'_thaw_receipt\',thaw)\n    def stop_writer(selected,role,**kwargs):\n        kwargs[\'guard\']();calls.append(\'stop:\'+role)\n        claim.record(\'shared-\'+role+\'-stop-intent\',dict(explicitProtocol=True))\n        if role==split.STOP_ORDER[0] and fault in (\'credential-bytes\',\'credential-inode\'):\n            credential=case[\'credential\'];raw=credential.read_bytes()\n            if fault==\'credential-inode\':credential.rename(credential.with_suffix(\'.retained\'));write(credential,raw)\n            else:credential.write_bytes(b\'CHANGED_PRIVATE_INPUT\\n\')\n        claim.record(\'shared-\'+role+\'-stopped\',dict(explicitProtocol=True));kwargs[\'guard\']()\n    monkeypatch.setattr(split,\'stop_controlled_shared_writer\',stop_writer)\n    monkeypatch.setattr(split,\'assert_controlled_prefence_stops\',lambda *a,**k:k[\'guard\']())\n    def fence(selected,mysql,*,guard):\n        guard();calls.append(\'fence\');flags=dict(case[\'prefence\'])\n        if fault==\'prefence-drift\':flags[\'readOnly\']=1\n        raw=snapshot._canonical_bytes(flags);claim._write(root/\'database-original.json\',raw)\n        claim.record(\'db-fence-intent\',dict(originalFlags=flags,originalSha256=digest(raw)))\n        guard();claim.record(\'db-fenced\',dict(originalSha256=digest(raw),reconciled=False));guard()\n    monkeypatch.setattr(snapshot,\'acquire_claimed_database_fence\',fence)\n    def sql(*a,**kwargs):\n        kwargs[\'guard\']();calls.append(\'sql\')\n        if fault==\'sql\':raise snapshot.FormalSnapshotError(\'TASK_TEST_SQL_REFUSED\')\n        claim.record(\'split-sql-intent\',{});claim.record(\'split-sql-captured\',{})\n        return dict(state=\'SQL_CAPTURED_PENDING_RAW_AND_FULL_BARRIERS\',sourceScope=source,sourceMigrationEvidence={})\n    monkeypatch.setattr(split,\'capture_sql_phase\',sql)\n    def stop_mysql(selected,role,**kwargs):\n        assert role==\'mysql\';kwargs[\'guard\']();claim.record(\'shared-mysql-stop-intent\',{})\n        kwargs[\'guard\']();calls.append(\'stop:mysql\');state[\'mysql_running\']=False\n        if fault==\'closed-owner\':claim.close()\n        if fault==\'approval-drift\':case[\'approval\'].write_bytes(b\'CHANGED_TASK_ONLY_APPROVAL\')\n        kwargs[\'guard\']()\n        if fault==\'mysql-stop\':raise snapshot.FormalSnapshotError(\'TASK_TEST_STOP_UNCERTAIN\')\n        claim.record(\'shared-mysql-stopped\',{})\n    monkeypatch.setattr(split,\'stop_controlled_capture_database\',stop_mysql)\n    def raw(*a,**kwargs):\n        kwargs[\'guard\']();calls.append(\'raw\');state[\'frozen\']=True\n        kwargs[\'native_guard\']();state[\'frozen\']=False\n        if fault==\'raw\':raise snapshot.FormalSnapshotError(\'TASK_TEST_RAW_REFUSED\')\n        claim.record(\'split-raw-intent\',{});claim.record(\'split-captured\',{});kwargs[\'guard\']()\n        return dict(state=\'CAPTURED_PENDING_FULL_BARRIER_PROOF\',sourceScope=source,sourceMigrationEvidence={})\n    monkeypatch.setattr(split,\'capture_raw_phase\',raw)\n    def transition(selected,value,*,guard,thaw_guard):\n        guard();thaw_guard();calls.append(\'transition\');claim.record(\'ingress-recovery-intent\',{})\n        rules=snapshot._canonical_bytes([dict(table=dict(handle=18))]);claim._write(root/\'ingress-recovery-installed.json\',rules)\n        ingress_state.update(phase=\'RECOVERY_DEPENDENCIES_PERMITTED\',tableHandle=18,rulesSha256=digest(rules))\n        claim.record(\'ingress-recovery-permitted\',dict(rulesSha256=digest(rules),planSha256=digest(snapshot._canonical_bytes(value))))\n        guard();thaw_guard();return dict(ingress_state)\n    monkeypatch.setattr(ingress,\'permit_recovery_dependencies\',transition)\n    def begin(selected,*,guard,thaw_guard):\n        guard();thaw_guard();claim.record(\'shared-recovery-intent\',{})\n    monkeypatch.setattr(split,\'begin_shared_recovery\',begin)\n    def start(selected,role,action,**kwargs):\n        assert action==\'start\';kwargs[\'guard\']();calls.append(\'start:\'+role)\n        if fault==\'mysql-start\' and role==\'mysql\' or fault==\'writer-start\' and role==\'redis\':\n            claim.record(\'shared-\'+role+\'-restore-intent\',{})\n            raise snapshot.FormalSnapshotError(\'TASK_TEST_START_UNCERTAIN\')\n        if role==\'mysql\':state[\'mysql_running\']=True\n        else:assert state[\'flags_restored\'] and \'live-owner\' in calls[calls.index(\'flags\')+1:]\n        claim.record(\'shared-\'+role+\'-restored\',{});kwargs[\'guard\']()\n    monkeypatch.setattr(split,\'_shared_action\',start)\n    def flags(selected,mysql,*,guard,thaw_guard):\n        guard();calls.append(\'flags\')\n        if fault==\'flags\':raise snapshot.FormalSnapshotError(\'TASK_TEST_FLAGS_UNCERTAIN\')\n        state[\'flags_restored\']=True;claim.record(\'shared-db-restored\',{});guard()\n    monkeypatch.setattr(split,\'restore_shared_database_flags\',flags)\n    monkeypatch.setattr(split,\'verify_shared_restoration\',lambda *a,**k:dict(state=\'ORIGINAL_SERVICES_RESTORED_PENDING_CALLER_PROOF\'))\n    if fault==\'credential-mode\':case[\'credential\'].chmod(0o666)\n    call=lambda:producer.run_production_a_shared_lifecycle(claim,req,approved_source=source,\n        resume_checks=case[\'checks\'],container_actions=case[\'actions\'],source_guard=source_guard,\n        maintenance_end=time.time()+90,recovery_flows=flows)\n    if fault==\'none\':\n        result=call()\n        order=[\'stop:\'+r for r in split.STOP_ORDER[:-1]]+[\'fence\',\'sql\',\'stop:mysql\',\'raw\',\'transition\',\n            \'start:mysql\',\'flags\']+[\'start:\'+r for r in split.RESTORE_ORDER[1:]]\n        assert [c for c in calls if c.startswith((\'stop:\',\'start:\')) or c in (\'fence\',\'sql\',\'raw\',\'transition\',\'flags\')]==order\n        assert result[\'claimReleased\'] is False and result[\'fullRunnerCompleted\'] is False and result[\'productionEvidence\'] is False\n    else:\n        expected=dict(sql=\'TASK_TEST_SQL_REFUSED\',**{\n            \'mysql-stop\':\'TASK_TEST_STOP_UNCERTAIN\',\'raw\':\'TASK_TEST_RAW_REFUSED\',\n            \'thaw\':\'FORMAL_SHARED_THAW_REQUIRED\',\'live-after-restore\':\'FORMAL_PRODUCTION_A_RESUME_CONTROL_CHANGED\',\n            \'mysql-start\':\'TASK_TEST_START_UNCERTAIN\',\'flags\':\'TASK_TEST_FLAGS_UNCERTAIN\',\n            \'writer-start\':\'TASK_TEST_START_UNCERTAIN\',\'prefence-drift\':\'FORMAL_PRODUCTION_A_DATABASE_ORIGINAL_CHANGED\',\n            \'approval-drift\':\'FORMAL_PRODUCTION_A_RESUME_CONTROL_CHANGED\',\n            \'changed-request\':\'FORMAL_PRODUCTION_A_SHARED_INPUT_CHANGED\',\n            \'credential-bytes\':\'FORMAL_PRODUCTION_A_SHARED_CREDENTIAL_CHANGED\',\n            \'credential-inode\':\'FORMAL_PRODUCTION_A_SHARED_CREDENTIAL_CHANGED\',\n            \'credential-mode\':\'FORMAL_BOOT_FILE_UNTRUSTED\',\n            \'closed-owner\':\'FORMAL_MAINTENANCE_OWNER_INVALID\'})\n        with pytest.raises(snapshot.FormalSnapshotError,match=\'^\'+expected[fault]+\'$\'):call()\n        # Refusal must reach this scenario\'s intended seam; an earlier typed\n        # error is a failure, not evidence of safe uncertainty handling.\n        reached={\'sql\':\'sql\',\'mysql-stop\':\'stop:mysql\',\'raw\':\'raw\',\'thaw\':\'raw\',\n            \'live-after-restore\':\'flags\',\'mysql-start\':\'start:mysql\',\'flags\':\'flags\',\n            \'writer-start\':\'start:redis\',\'prefence-drift\':\'fence\',\'approval-drift\':\'stop:mysql\',\n            \'changed-request\':\'fence\',\'credential-bytes\':\'stop:frontend\',\'credential-inode\':\'stop:frontend\',\n            \'closed-owner\':\'stop:mysql\'}\n        if fault in reached:assert reached[fault] in calls\n        if fault not in (\'live-after-restore\',\'mysql-start\',\'flags\',\'writer-start\'):\n            assert not any(c.startswith(\'start:\') for c in calls)\n        if fault in (\'live-after-restore\',\'mysql-start\',\'flags\'):\n            assert not any(c.startswith(\'start:\') and c!=\'start:mysql\' for c in calls)\n        if fault==\'credential-mode\':assert not any(c.startswith(\'stop:\') for c in calls)\n    assert len(list(case[\'registry\'].iterdir()))==1\n    assert sum(e[\'step\']==\'production-a-resume-consumed\' for e in claim.events)==1\n    if fault==\'closed-owner\':assert claim._fd is None\n    else:\n        assert claim._fd==case[\'fd\'] and boot._signature(os.fstat(claim._fd))==case[\'signature\']\n        with pytest.raises(snapshot.FormalSnapshotError,match=\'FORMAL_MAINTENANCE_BUSY\'):\n            snapshot.FormalMaintenanceClaim(claim.root,json.loads(claim._plan),resume=True)\n'

NATIVE_A_SHARED_LIFECYCLE_TESTS_V5 = '"""Real Linux FD/flock/control pins/signature; explicit service/SQL/nft actors.\n\nThis tests the new phase wiring, not actual SQL, Docker, thaw or host protection.\nOriginal contents, five machine mappings and signed administrator are task-only\nfixtures. No actual daemon/service/SSH/freeze/server/NAS actions occur.\n"""\nimport base64\nimport hashlib\nimport json\nimport os\nfrom pathlib import Path\nimport stat\nimport subprocess\nimport sys\nimport tempfile\nimport time\n\nimport pytest\n\nsys.path.insert(0,\'/workspace/linux\')\nimport formal_capture_producer as producer\nimport formal_snapshot_linux as snapshot\nimport formal_boot_state as boot\nimport formal_split_capture as split\nimport formal_ingress_control as ingress\nimport formal_external_callers as callers\nimport formal_controlled_container_sources as controlled\nfrom formal_source_migration_ledger import approved_source_scope\n\n\ndef digest(raw):return hashlib.sha256(raw).hexdigest()\n\n\ndef write(path,raw,mode=0o600):\n    with path.open(\'xb\') as out:out.write(raw)\n    path.chmod(mode)\n\n\ndef openssl(*args):\n    result=subprocess.run([\'/usr/bin/openssl\',*args],capture_output=True,timeout=5)\n    assert result.returncode==0 and not result.stderr,\'TASK_TEST_OPENSSL_FAILED\'\n    return result.stdout\n\n\n@pytest.fixture\ndef native_case():\n    assert sys.platform==\'linux\' and os.geteuid()==0\n    producer.require_native_runtime()\n    token=snapshot.observation_deadline.set(time.monotonic()+45)\n    claim=None\n    try:\n        with tempfile.TemporaryDirectory(prefix=\'p2-shared-join-\',dir=\'/control\') as directory:\n            root=Path(directory);host_boot=Path(\'/proc/sys/kernel/random/boot_id\').read_text().strip()\n            target=dict(schemaVersion=\'release-formal-target/v1\',environment=\'prod\',host=\'192.0.2.17\',\n                machineId=\'6\'*32,project=\'task-only-project\',root=\'/task/source\',dataMount=\'/task/source\',\n                dataDevice=\'/dev/task-fixture\',dataRoot=\'/task/source/data\',releaseRoot=\'/task/release\',\n                frontendPort=81,backendPort=82,database=\'task_probe\',\n                databaseServerUuid=\'0fa6c22b-5a90-11f1-ade1-0242ac150002\',\n                containers={role:\'task-only-\'+role for role in (\'mysql\',\'redis\',\'minio\',\'backend\',\'frontend\')})\n            target_raw=snapshot._canonical_bytes(target)\n            binding=dict(operationId=\'prod-task-only-shared-join\',sourceFingerprint=digest(json.dumps(target,sort_keys=True,separators=(\',\',\':\'),ensure_ascii=False).encode(\'utf-8\')),\n                databaseUuid=target[\'databaseServerUuid\'],minioContainerId=\'b\'*64)\n            claim=snapshot.FormalMaintenanceClaim(root/\'claim\',binding)\n            fd=claim._fd;signature=boot._signature(os.fstat(fd))\n            source=approved_source_scope(target_raw,digest(target_raw),claim._plan,target[\'database\'])\n            prefence=dict(serverUuid=binding[\'databaseUuid\'],readOnly=0,superReadOnly=0)\n            sources={};boot_sources={}\n            for name in sorted(producer.PRODUCTION_A_SOURCE_FILES):\n                path=claim.root/name\n                raw=claim._plan if name==\'binding.json\' else snapshot._canonical_bytes(dict(explicitOriginalFixture=name))\n                if name==\'approved-source-target.json\':raw=target_raw\n                if name==\'database-prefence-original.json\':raw=snapshot._canonical_bytes(prefence)\n                if name==\'ingress-plan.json\':raw=b\'{}\'\n                if name.startswith(\'boot-\'):\n                    role=name[5:-14];parent=root/(\'boot-fixture-\'+role);parent.mkdir(mode=0o700)\n                    mapping=dict(schemaVersion=\'release-formal-production-a-boot-source/v1\',role=role,peer=\'local\',\n                        machineIdSha256=\'c\'*64,bootId=host_boot,path=str(parent/\'boot.json\'))\n                    boot_sources[role]=mapping\n                    with boot._directory(parent,no_atime=True) as (_,chain):\n                        raw=snapshot._canonical_bytes(dict(schemaVersion=\'release-formal-production-a-boot-original/v1\',\n                            **{k:mapping[k] for k in (\'role\',\'peer\',\'machineIdSha256\',\'bootId\',\'path\')},\n                            sourceParentIdentity=[list(row) for row in chain],present=False,\n                            originalBase64=None,sha256=None,sourceFileIdentity=None))\n                if name!=\'binding.json\':write(path,raw)\n                sources[name]=str(path)\n            release=dict(schemaVersion=\'release-formal-original-package-binding/v2\',claimBindingSha256=digest(claim._plan),\n                approvedSourceTargetSha256=digest(target_raw),deploymentAttemptId=\'1\'*64,leaseTokenDigest=\'2\'*64,\n                scope=dict(environment=\'prod\'),package=dict(releaseTag=\'task-only-shared-join\',\n                    packageDigest=\'3\'*64,manifestDigest=\'4\'*64,planDigest=\'5\'*64))\n            release_raw=snapshot._canonical_bytes(release);claim._write(claim.root/\'original-release-binding.json\',release_raw)\n            sealed=producer.seal_production_a_source_inventory(claim,release_raw,sources,boot_sources,\n                guard=claim.assert_owned,deadline=time.monotonic()+15)\n            registry=root/\'registry\';registry.mkdir(mode=0o700)\n            key_private=root/\'task-only-private.pem\'\n            openssl(\'genpkey\',\'-algorithm\',\'ED25519\',\'-out\',str(key_private));key_private.chmod(0o600)\n            public=root/\'task-only-public.der\'\n            write(public,openssl(\'pkey\',\'-in\',str(key_private),\'-pubout\',\'-outform\',\'DER\'),0o644)\n            now=int(time.time())\n            payload=dict(sealed[\'binding\'],purpose=\'RESTORE_PRODUCTION_SAVED_ORIGINAL_STATE\',grantId=\'8\'*32,\n                actorId=17,tenantId=122,issuedAt=now-1,expiresAt=now+120,\n                policy=dict(originalStateOnly=True,preserveStoppedAndAbsent=True,\n                    preserveUnfinishedRecords=True,noBootStateWrites=True))\n            message=root/\'task-only-payload.json\';write(message,json.dumps(payload,separators=(\',\',\':\')).encode(\'ascii\'))\n            signature_raw=openssl(\'pkeyutl\',\'-sign\',\'-inkey\',str(key_private),\'-rawin\',\'-in\',str(message))\n            approval=root/\'task-only-approval.json\'\n            envelope=dict(schemaVersion=\'formal-production-a-resume-approval/v1\',keySha256=digest(public.read_bytes()),\n                payloadBase64=base64.b64encode(message.read_bytes()).decode(),\n                signatureBase64=base64.b64encode(signature_raw).decode())\n            write(approval,json.dumps(envelope).encode(\'ascii\'));info=registry.stat()\n            resume=dict(registry=dict(path=str(registry),identity={k:getattr(info,\'st_\'+k)\n                for k in (\'dev\',\'ino\',\'uid\',\'gid\',\'mode\')}),approval=dict(path=str(approval),sha256=digest(approval.read_bytes())),\n                trustedKey=dict(path=str(public),sha256=digest(public.read_bytes())))\n            state=dict(mysql_running=True,frozen=False,flags_restored=False,fault=None,calls=[])\n            def live_owner():\n                claim.assert_owned()\n                assert state[\'mysql_running\'] and not state[\'frozen\'],\'SQL callback during stopped/frozen phase\'\n                state[\'calls\'].append(\'live-owner\')\n                if state[\'fault\']==\'live-after-restore\' and state[\'flags_restored\']:\n                    raise snapshot.FormalSnapshotError(\'TASK_TEST_LIVE_OWNER_REFUSED\')\n            checks=producer.ProductionAResumeChecks(claim,release_raw,Path(sealed[\'path\']).read_bytes(),sealed[\'sha256\'],\n                resume,guard=live_owner,thaw_guard=claim.assert_owned)\n            credential=root/\'mysql-credential\';write(credential,b\'task-only-shared-join-input\\n\')\n            originals={role:dict(path=sources[\'controlled-\'+role+\'-inspect.json\'],\n                source=dict(present=True,containerId=digest(role.encode()))) for role in split.ROLES}\n            actions=controlled.ControlledSharedContainerActions(originals,object())\n            request=dict(schemaVersion=\'release-formal-capture-request/v12\',action=\'capture-shared-payload\',\n                claimRoot=str(claim.root),claimBinding=binding,mysqlContainerId=\'d\'*64,mysqlCredentialFile=str(credential),\n                totalSeconds=120,sharedScopeSha256=digest((claim.root/\'shared-container-original.json\').read_bytes()),\n                originalHealthSha256=digest((claim.root/\'shared-health-original.json\').read_bytes()),ingressRulesSha256=\'f\'*64,\n                database=target[\'database\'],sourceRoot=\'/task/source/minio\',sourceScope=source,\n                dataLimits=dict(max_rows=1000,max_nodes=10000,max_bytes=1048576,seconds=30),\n                rejectionProbe=dict(table=\'task_probe\',column=\'id\'),\n                filesystemFence=dict(mount=\'/task/source\',seconds=3,binding=dict(operationId=binding[\'operationId\'],\n                    bootId=host_boot,mount=dict(source=\'/dev/task-fixture\',device=\'9:1\',fstype=\'ext4\',target=\'/task/source\'))))\n            yield dict(root=root,claim=claim,fd=fd,signature=signature,source=source,prefence=prefence,state=state,\n                checks=checks,actions=actions,request=request,credential=credential,approval=approval,registry=registry)\n            claim.close();claim=None\n    finally:\n        if claim is not None:claim.close()\n        snapshot.observation_deadline.reset(token)\n\n\n@pytest.mark.parametrize(\'fault\',[\'none\',\'sql\',\'mysql-stop\',\'raw\',\'thaw\',\'live-after-restore\',\n    \'mysql-start\',\'flags\',\'writer-start\',\'prefence-drift\',\'approval-drift\',\'changed-request\',\n    \'credential-bytes\',\'credential-inode\',\'credential-mode\',\'closed-owner\'])\ndef test_native_a_phase_join_retains_real_authority_fd_and_refuses_next_action(native_case,monkeypatch,fault):\n    case=native_case;claim=case[\'claim\'];state=case[\'state\'];state[\'fault\']=fault;calls=state[\'calls\']\n    req=case[\'request\'];source=case[\'source\'];root=claim.root\n    shared_raw=(root/\'shared-container-original.json\').read_bytes();health_raw=(root/\'shared-health-original.json\').read_bytes()\n    flows=[dict(source=\'192.0.2.1\',destination=\'192.0.2.2\',ports=[3306])]\n    ingress_state=dict(phase=\'CAPTURE_RESTRICTED\',rulesSha256=req[\'ingressRulesSha256\'],tableHandle=17)\n    monkeypatch.setattr(ingress,\'assert_ingress\',lambda *a,**k:dict(ingress_state))\n    monkeypatch.setattr(ingress,\'render_recovery_policy\',lambda *a:None)\n    monkeypatch.setattr(callers,\'guard_external_callers\',lambda *a:dict(state=\'KNOWN_EXTERNAL_CALLERS_STOPPED_PENDING_FULL_EXCLUSION\'))\n    monkeypatch.setattr(producer,\'verify_shared_request_binding\',lambda *a:dict(shared=shared_raw,health=health_raw))\n    monkeypatch.setattr(producer,\'verify_shared_database_before_stop\',lambda *a:None)\n    class SqlProtocol:\n        def __init__(self,container,*a,**k):self.container=container\n    monkeypatch.setattr(producer,\'PrivateDockerMySql\',SqlProtocol)\n    def source_guard():\n        assert not state[\'frozen\'],\'source callback during frozen phase\'\n        claim.assert_owned();calls.append(\'source\')\n        if fault==\'changed-request\' and any(e[\'step\']==\'db-fenced\' for e in claim.events):req[\'database\']=\'changed\'\n    def thaw(selected):\n        assert selected is claim and not state[\'frozen\'],\'thaw observer during frozen phase\'\n        if fault==\'thaw\' and any(e[\'step\']==\'split-captured\' for e in claim.events):\n            raise snapshot.FormalSnapshotError(\'FORMAL_SHARED_THAW_REQUIRED\')\n        return dict(nativeState=\'THAWED_COPY_ACCEPTED_PENDING_FORMAL_BARRIERS\')\n    monkeypatch.setattr(split,\'_thaw_receipt\',thaw)\n    def stop_writer(selected,role,**kwargs):\n        kwargs[\'guard\']();calls.append(\'stop:\'+role)\n        claim.record(\'shared-\'+role+\'-stop-intent\',dict(explicitProtocol=True))\n        if role==split.STOP_ORDER[0] and fault in (\'credential-bytes\',\'credential-inode\'):\n            credential=case[\'credential\'];raw=credential.read_bytes()\n            if fault==\'credential-inode\':credential.rename(credential.with_suffix(\'.retained\'));write(credential,raw)\n            else:credential.write_bytes(b\'CHANGED_PRIVATE_INPUT\\n\')\n        claim.record(\'shared-\'+role+\'-stopped\',dict(explicitProtocol=True));kwargs[\'guard\']()\n    monkeypatch.setattr(split,\'stop_controlled_shared_writer\',stop_writer)\n    monkeypatch.setattr(split,\'assert_controlled_prefence_stops\',lambda *a,**k:k[\'guard\']())\n    def fence(selected,mysql,*,guard):\n        guard();calls.append(\'fence\');flags=dict(case[\'prefence\'])\n        if fault==\'prefence-drift\':flags[\'readOnly\']=1\n        raw=snapshot._canonical_bytes(flags);claim._write(root/\'database-original.json\',raw)\n        claim.record(\'db-fence-intent\',dict(originalFlags=flags,originalSha256=digest(raw)))\n        guard();claim.record(\'db-fenced\',dict(originalSha256=digest(raw),reconciled=False));guard()\n    monkeypatch.setattr(snapshot,\'acquire_claimed_database_fence\',fence)\n    def sql(*a,**kwargs):\n        kwargs[\'guard\']();calls.append(\'sql\')\n        if fault==\'sql\':raise snapshot.FormalSnapshotError(\'TASK_TEST_SQL_REFUSED\')\n        claim.record(\'split-sql-intent\',{});claim.record(\'split-sql-captured\',{})\n        return dict(state=\'SQL_CAPTURED_PENDING_RAW_AND_FULL_BARRIERS\',sourceScope=source,sourceMigrationEvidence={})\n    monkeypatch.setattr(split,\'capture_sql_phase\',sql)\n    def stop_mysql(selected,role,**kwargs):\n        assert role==\'mysql\';kwargs[\'guard\']();claim.record(\'shared-mysql-stop-intent\',{})\n        kwargs[\'guard\']();calls.append(\'stop:mysql\');state[\'mysql_running\']=False\n        if fault==\'closed-owner\':claim.close()\n        if fault==\'approval-drift\':case[\'approval\'].write_bytes(b\'CHANGED_TASK_ONLY_APPROVAL\')\n        kwargs[\'guard\']()\n        if fault==\'mysql-stop\':raise snapshot.FormalSnapshotError(\'TASK_TEST_STOP_UNCERTAIN\')\n        claim.record(\'shared-mysql-stopped\',{})\n    monkeypatch.setattr(split,\'stop_controlled_capture_database\',stop_mysql)\n    def raw(*a,**kwargs):\n        kwargs[\'guard\']();calls.append(\'raw\');state[\'frozen\']=True\n        kwargs[\'native_guard\']();state[\'frozen\']=False\n        if fault==\'raw\':raise snapshot.FormalSnapshotError(\'TASK_TEST_RAW_REFUSED\')\n        claim.record(\'split-raw-intent\',{});claim.record(\'split-captured\',{});kwargs[\'guard\']()\n        return dict(state=\'CAPTURED_PENDING_FULL_BARRIER_PROOF\',sourceScope=source,sourceMigrationEvidence={})\n    monkeypatch.setattr(split,\'capture_raw_phase\',raw)\n    def transition(selected,value,*,guard,thaw_guard):\n        guard();thaw_guard();calls.append(\'transition\');claim.record(\'ingress-recovery-intent\',{})\n        rules=snapshot._canonical_bytes([dict(table=dict(handle=18))]);claim._write(root/\'ingress-recovery-installed.json\',rules)\n        ingress_state.update(phase=\'RECOVERY_DEPENDENCIES_PERMITTED\',tableHandle=18,rulesSha256=digest(rules))\n        claim.record(\'ingress-recovery-permitted\',dict(rulesSha256=digest(rules),planSha256=digest(snapshot._canonical_bytes(value))))\n        guard();thaw_guard();return dict(ingress_state)\n    monkeypatch.setattr(ingress,\'permit_recovery_dependencies\',transition)\n    def begin(selected,*,guard,thaw_guard):\n        guard();thaw_guard();claim.record(\'shared-recovery-intent\',{})\n    monkeypatch.setattr(split,\'begin_shared_recovery\',begin)\n    def start(selected,role,action,**kwargs):\n        assert action==\'start\';kwargs[\'guard\']();calls.append(\'start:\'+role)\n        if fault==\'mysql-start\' and role==\'mysql\' or fault==\'writer-start\' and role==\'redis\':\n            claim.record(\'shared-\'+role+\'-restore-intent\',{})\n            raise snapshot.FormalSnapshotError(\'TASK_TEST_START_UNCERTAIN\')\n        if role==\'mysql\':state[\'mysql_running\']=True\n        else:assert state[\'flags_restored\'] and \'live-owner\' in calls[calls.index(\'flags\')+1:]\n        claim.record(\'shared-\'+role+\'-restored\',{});kwargs[\'guard\']()\n    monkeypatch.setattr(split,\'_shared_action\',start)\n    def flags(selected,mysql,*,guard,thaw_guard):\n        guard();calls.append(\'flags\')\n        if fault==\'flags\':raise snapshot.FormalSnapshotError(\'TASK_TEST_FLAGS_UNCERTAIN\')\n        state[\'flags_restored\']=True;claim.record(\'shared-db-restored\',{});guard()\n    monkeypatch.setattr(split,\'restore_shared_database_flags\',flags)\n    monkeypatch.setattr(split,\'verify_shared_restoration\',lambda *a,**k:dict(state=\'ORIGINAL_SERVICES_RESTORED_PENDING_CALLER_PROOF\'))\n    if fault==\'credential-mode\':case[\'credential\'].chmod(0o666)\n    def call():\n        try:\n            return producer.run_production_a_shared_lifecycle(claim,req,approved_source=source,\n                resume_checks=case[\'checks\'],container_actions=case[\'actions\'],source_guard=source_guard,\n                maintenance_end=time.time()+90,recovery_flows=flows)\n        except snapshot.FormalSnapshotError as error:\n            # Report actual failing production-frame clocks; no clock patch,\n            # threshold change, expected-code downgrade, retry or swallowing.\n            clocks=[];trace=error.__traceback__\n            while trace is not None:\n                frame=trace.tb_frame;values=frame.f_locals\n                if frame.f_code.co_filename==producer.__file__:\n                    sample={key:values[key] for key in (\'elapsed\',\'wall\',\'now_wall\',\'wall_origin\',\n                        \'clock_origin\',\'clock_end\',\'authority_end\',\'issued\',\'expires\',\'deadline\')\n                        if key in values and type(values[key]) in (int,float)}\n                    if sample:\n                        sample.update(function=frame.f_code.co_name,line=trace.tb_lineno)\n                        selected=values.get(\'self\')\n                        if type(selected) is producer.ProductionAResumeChecks:\n                            sample.update(ownerWall=selected._wall,ownerMonotonic=selected._clock,\n                                issuedAt=selected.consumed[\'issuedAt\'],expiresAt=selected.consumed[\'expiresAt\'])\n                        clocks.append(sample)\n                trace=trace.tb_next\n            print(\'ACTUAL_NATIVE_CLOCK_FAILURE \'+json.dumps(dict(error=str(error),clocks=clocks,\n                flagsRestored=state[\'flags_restored\'],mysqlRunning=state[\'mysql_running\'],\n                steps=[event[\'step\'] for event in claim.events],calls=calls)),flush=True)\n            raise\n    if fault==\'none\':\n        result=call()\n        order=[\'stop:\'+r for r in split.STOP_ORDER[:-1]]+[\'fence\',\'sql\',\'stop:mysql\',\'raw\',\'transition\',\n            \'start:mysql\',\'flags\']+[\'start:\'+r for r in split.RESTORE_ORDER[1:]]\n        assert [c for c in calls if c.startswith((\'stop:\',\'start:\')) or c in (\'fence\',\'sql\',\'raw\',\'transition\',\'flags\')]==order\n        assert result[\'claimReleased\'] is False and result[\'fullRunnerCompleted\'] is False and result[\'productionEvidence\'] is False\n    else:\n        expected=dict(sql=\'TASK_TEST_SQL_REFUSED\',**{\n            \'mysql-stop\':\'TASK_TEST_STOP_UNCERTAIN\',\'raw\':\'TASK_TEST_RAW_REFUSED\',\n            \'thaw\':\'FORMAL_SHARED_THAW_REQUIRED\',\'live-after-restore\':\'TASK_TEST_LIVE_OWNER_REFUSED\',\n            \'mysql-start\':\'TASK_TEST_START_UNCERTAIN\',\'flags\':\'TASK_TEST_FLAGS_UNCERTAIN\',\n            \'writer-start\':\'TASK_TEST_START_UNCERTAIN\',\'prefence-drift\':\'FORMAL_PRODUCTION_A_DATABASE_ORIGINAL_CHANGED\',\n            \'approval-drift\':\'FORMAL_PRODUCTION_A_RESUME_CONTROL_CHANGED\',\n            \'changed-request\':\'FORMAL_PRODUCTION_A_SHARED_INPUT_CHANGED\',\n            \'credential-bytes\':\'FORMAL_PRODUCTION_A_SHARED_CREDENTIAL_CHANGED\',\n            \'credential-inode\':\'FORMAL_PRODUCTION_A_SHARED_CREDENTIAL_CHANGED\',\n            \'credential-mode\':\'FORMAL_BOOT_FILE_UNTRUSTED\',\n            \'closed-owner\':\'FORMAL_MAINTENANCE_OWNER_INVALID\'})\n        with pytest.raises(snapshot.FormalSnapshotError,match=\'^\'+expected[fault]+\'$\'):call()\n        # Refusal must reach this scenario\'s intended seam; an earlier typed\n        # error is a failure, not evidence of safe uncertainty handling.\n        reached={\'sql\':\'sql\',\'mysql-stop\':\'stop:mysql\',\'raw\':\'raw\',\'thaw\':\'raw\',\n            \'live-after-restore\':\'flags\',\'mysql-start\':\'start:mysql\',\'flags\':\'flags\',\n            \'writer-start\':\'start:redis\',\'prefence-drift\':\'fence\',\'approval-drift\':\'stop:mysql\',\n            \'changed-request\':\'fence\',\'credential-bytes\':\'stop:frontend\',\'credential-inode\':\'stop:frontend\',\n            \'closed-owner\':\'stop:mysql\'}\n        if fault in reached:assert reached[fault] in calls\n        if fault==\'live-after-restore\':assert state[\'flags_restored\'] and \'live-owner\' in calls[calls.index(\'flags\')+1:]\n        if fault not in (\'live-after-restore\',\'mysql-start\',\'flags\',\'writer-start\'):\n            assert not any(c.startswith(\'start:\') for c in calls)\n        if fault in (\'live-after-restore\',\'mysql-start\',\'flags\'):\n            assert not any(c.startswith(\'start:\') and c!=\'start:mysql\' for c in calls)\n        if fault==\'credential-mode\':assert not any(c.startswith(\'stop:\') for c in calls)\n    assert len(list(case[\'registry\'].iterdir()))==1\n    assert sum(e[\'step\']==\'production-a-resume-consumed\' for e in claim.events)==1\n    if fault==\'closed-owner\':assert claim._fd is None\n    else:\n        assert claim._fd==case[\'fd\'] and boot._signature(os.fstat(claim._fd))==case[\'signature\']\n        with pytest.raises(snapshot.FormalSnapshotError,match=\'FORMAL_MAINTENANCE_BUSY\'):\n            snapshot.FormalMaintenanceClaim(claim.root,json.loads(claim._plan),resume=True)\n'

NATIVE_A_ORIGINAL_CONTENT_TESTS_V1 = '"""Real Linux FD/flock/control pins/signature; explicit service/SQL/nft actors.\n\nThis tests the new phase wiring, not actual SQL, Docker, thaw or host protection.\nOriginal contents, five machine mappings and signed administrator are task-only\nfixtures. No actual daemon/service/SSH/freeze/server/NAS actions occur.\n"""\nimport base64\nimport hashlib\nimport json\nimport io\nimport tarfile\nimport os\nfrom pathlib import Path\nimport stat\nimport subprocess\nimport sys\nimport tempfile\nimport time\n\nimport pytest\n\nsys.path.insert(0,\'/workspace/linux\')\nimport formal_capture_producer as producer\nimport formal_snapshot_linux as snapshot\nimport formal_boot_state as boot\nimport formal_split_capture as split\nimport formal_ingress_control as ingress\nimport formal_external_callers as callers\nimport formal_controlled_container_sources as controlled\nimport formal_selected_config_capture as configs\nimport formal_original_launch_capture as images\nfrom formal_source_migration_ledger import approved_source_scope\n\n\ndef digest(raw):return hashlib.sha256(raw).hexdigest()\n\n\ndef write(path,raw,mode=0o600):\n    with path.open(\'xb\') as out:out.write(raw)\n    path.chmod(mode)\n\n\ndef openssl(*args):\n    result=subprocess.run([\'/usr/bin/openssl\',*args],capture_output=True,timeout=5)\n    assert result.returncode==0 and not result.stderr,\'TASK_TEST_OPENSSL_FAILED\'\n    return result.stdout\n\n\ndef original_image_archive(path,fault=None):\n    files={};entries=[];ids={}\n    for index,role in enumerate((\'backend\',\'frontend\')):\n        layer=(b\'OLD_ACTUAL_LAYER_\'+role.encode())*(index+1)\n        config=json.dumps(dict(os=\'linux\',architecture=\'amd64\',\n            rootfs=dict(type=\'layers\',diff_ids=[\'sha256:\'+hashlib.sha256(layer).hexdigest()])),\n            sort_keys=True).encode()\n        image=\'sha256:\'+hashlib.sha256(config).hexdigest();ids[role]=image\n        name=image[7:]+\'.json\';layer_path=role+\'/layer.tar\'\n        files[name]=config;files[layer_path]=layer\n        entries.append(dict(Config=name,RepoTags=None,Layers=[layer_path]))\n    if fault==\'config\':files[entries[0][\'Config\']]+=b\' \'\n    if fault==\'layer\':files[entries[0][\'Layers\'][0]]=b\'WRONG_LAYER\'\n    if fault==\'missing-layer\':files.pop(entries[0][\'Layers\'][0])\n    if fault==\'wrong-image\':ids[\'backend\']=\'sha256:\'+\'f\'*64\n    if fault==\'extra-image\':entries.append(entries[0])\n    files[\'manifest.json\']=json.dumps(entries).encode()\n    with tarfile.open(path,\'w\') as archive:\n        for name,content in files.items():\n            item=tarfile.TarInfo(name);item.size=len(content)\n            archive.addfile(item,io.BytesIO(content))\n        if fault==\'duplicate\':\n            item=tarfile.TarInfo(\'manifest.json\');item.size=len(files[\'manifest.json\'])\n            archive.addfile(item,io.BytesIO(files[\'manifest.json\']))\n        if fault==\'unsafe-path\':\n            item=tarfile.TarInfo(\'../outside\');item.size=1;archive.addfile(item,io.BytesIO(b\'X\'))\n    return ids\n\n\n@pytest.fixture\ndef native_case():\n    assert sys.platform==\'linux\' and os.geteuid()==0\n    producer.require_native_runtime()\n    token=snapshot.observation_deadline.set(time.monotonic()+45)\n    claim=None\n    try:\n        with tempfile.TemporaryDirectory(prefix=\'p2-shared-join-\',dir=\'/control\') as directory:\n            root=Path(directory);host_boot=Path(\'/proc/sys/kernel/random/boot_id\').read_text().strip()\n            config_root=root/\'source\';config_root.mkdir(mode=0o700);(config_root/\'config\').mkdir(mode=0o700)\n            config_originals={}\n            for role,relative in configs.FILES.items():\n                path=config_root/relative;content=(\'EXPLICIT_NATIVE_CONFIG_\'+role).encode();write(path,content)\n                info=path.stat();config_originals[role]=dict(path=str(path),bytes=len(content),sha256=digest(content),\n                    fileSystemId=info.st_dev,inode=info.st_ino,mtimeNs=info.st_mtime_ns,ctimeNs=info.st_ctime_ns)\n            fixture_archive=root/\'explicit-docker-image-protocol.tar\'\n            image_ids=original_image_archive(fixture_archive);fixture_archive.chmod(0o600)\n            shared_originals={role:dict(id=\'b\'*64 if role==\'minio\' else \'d\'*64 if role==\'mysql\' else digest(role.encode()),\n                name=\'/explicit-\'+role,image=image_ids.get(role,\'sha256:\'+digest(role.encode())),mounts=[],restartPolicy={},\n                state=dict(Running=True,Paused=False,Restarting=False,Dead=False,OOMKilled=False,ExitCode=0,\n                    StartedAt=\'2026-10-01T00:00:00Z\',FinishedAt=\'0001-01-01T00:00:00Z\')) for role in split.ROLES}\n            target=dict(schemaVersion=\'release-formal-target/v1\',environment=\'prod\',host=\'192.0.2.17\',\n                machineId=\'6\'*32,project=\'task-only-project\',root=str(config_root),dataMount=str(config_root),\n                dataDevice=\'/dev/task-fixture\',dataRoot=str(config_root/\'data\'),releaseRoot=\'/task/release\',\n                frontendPort=81,backendPort=82,database=\'task_probe\',\n                databaseServerUuid=\'0fa6c22b-5a90-11f1-ade1-0242ac150002\',\n                containers={role:\'task-only-\'+role for role in (\'mysql\',\'redis\',\'minio\',\'backend\',\'frontend\')})\n            target_raw=snapshot._canonical_bytes(target)\n            binding=dict(operationId=\'prod-task-only-shared-join\',sourceFingerprint=digest(json.dumps(target,sort_keys=True,separators=(\',\',\':\'),ensure_ascii=False).encode(\'utf-8\')),\n                databaseUuid=target[\'databaseServerUuid\'],minioContainerId=\'b\'*64)\n            claim=snapshot.FormalMaintenanceClaim(root/\'claim\',binding)\n            fd=claim._fd;signature=boot._signature(os.fstat(fd))\n            source=approved_source_scope(target_raw,digest(target_raw),claim._plan,target[\'database\'])\n            prefence=dict(serverUuid=binding[\'databaseUuid\'],readOnly=0,superReadOnly=0)\n            sources={};boot_sources={}\n            for name in sorted(producer.PRODUCTION_A_SOURCE_FILES):\n                path=claim.root/name\n                raw=claim._plan if name==\'binding.json\' else snapshot._canonical_bytes(dict(explicitOriginalFixture=name))\n                if name==\'approved-source-target.json\':raw=target_raw\n                if name==\'database-prefence-original.json\':raw=snapshot._canonical_bytes(prefence)\n                if name==\'ingress-plan.json\':raw=b\'{}\'\n                if name==\'original-configs.json\':raw=snapshot._canonical_bytes(config_originals)\n                if name==\'shared-container-original.json\':raw=snapshot._canonical_bytes(shared_originals)\n                if name.startswith(\'boot-\'):\n                    role=name[5:-14];parent=root/(\'boot-fixture-\'+role);parent.mkdir(mode=0o700)\n                    mapping=dict(schemaVersion=\'release-formal-production-a-boot-source/v1\',role=role,peer=\'local\',\n                        machineIdSha256=\'c\'*64,bootId=host_boot,path=str(parent/\'boot.json\'))\n                    boot_sources[role]=mapping\n                    with boot._directory(parent,no_atime=True) as (_,chain):\n                        raw=snapshot._canonical_bytes(dict(schemaVersion=\'release-formal-production-a-boot-original/v1\',\n                            **{k:mapping[k] for k in (\'role\',\'peer\',\'machineIdSha256\',\'bootId\',\'path\')},\n                            sourceParentIdentity=[list(row) for row in chain],present=False,\n                            originalBase64=None,sha256=None,sourceFileIdentity=None))\n                if name!=\'binding.json\':write(path,raw)\n                sources[name]=str(path)\n            release=dict(schemaVersion=\'release-formal-original-package-binding/v2\',claimBindingSha256=digest(claim._plan),\n                approvedSourceTargetSha256=digest(target_raw),deploymentAttemptId=\'1\'*64,leaseTokenDigest=\'2\'*64,\n                scope=dict(environment=\'prod\'),package=dict(releaseTag=\'task-only-shared-join\',\n                    packageDigest=\'3\'*64,manifestDigest=\'4\'*64,planDigest=\'5\'*64))\n            release_raw=snapshot._canonical_bytes(release);claim._write(claim.root/\'original-release-binding.json\',release_raw)\n            claim.record(\'shared-container-scope\',dict(scopeSha256=digest((claim.root/\'shared-container-original.json\').read_bytes())))\n            sealed=producer.seal_production_a_source_inventory(claim,release_raw,sources,boot_sources,\n                guard=claim.assert_owned,deadline=time.monotonic()+15)\n            registry=root/\'registry\';registry.mkdir(mode=0o700)\n            key_private=root/\'task-only-private.pem\'\n            openssl(\'genpkey\',\'-algorithm\',\'ED25519\',\'-out\',str(key_private));key_private.chmod(0o600)\n            public=root/\'task-only-public.der\'\n            write(public,openssl(\'pkey\',\'-in\',str(key_private),\'-pubout\',\'-outform\',\'DER\'),0o644)\n            now=int(time.time())\n            payload=dict(sealed[\'binding\'],purpose=\'RESTORE_PRODUCTION_SAVED_ORIGINAL_STATE\',grantId=\'8\'*32,\n                actorId=17,tenantId=122,issuedAt=now-1,expiresAt=now+120,\n                policy=dict(originalStateOnly=True,preserveStoppedAndAbsent=True,\n                    preserveUnfinishedRecords=True,noBootStateWrites=True))\n            message=root/\'task-only-payload.json\';write(message,json.dumps(payload,separators=(\',\',\':\')).encode(\'ascii\'))\n            signature_raw=openssl(\'pkeyutl\',\'-sign\',\'-inkey\',str(key_private),\'-rawin\',\'-in\',str(message))\n            approval=root/\'task-only-approval.json\'\n            envelope=dict(schemaVersion=\'formal-production-a-resume-approval/v1\',keySha256=digest(public.read_bytes()),\n                payloadBase64=base64.b64encode(message.read_bytes()).decode(),\n                signatureBase64=base64.b64encode(signature_raw).decode())\n            write(approval,json.dumps(envelope).encode(\'ascii\'));info=registry.stat()\n            resume=dict(registry=dict(path=str(registry),identity={k:getattr(info,\'st_\'+k)\n                for k in (\'dev\',\'ino\',\'uid\',\'gid\',\'mode\')}),approval=dict(path=str(approval),sha256=digest(approval.read_bytes())),\n                trustedKey=dict(path=str(public),sha256=digest(public.read_bytes())))\n            state=dict(mysql_running=True,frozen=False,flags_restored=False,fault=None,calls=[])\n            def live_owner():\n                claim.assert_owned()\n                assert state[\'mysql_running\'] and not state[\'frozen\'],\'SQL callback during stopped/frozen phase\'\n                state[\'calls\'].append(\'live-owner\')\n                if state[\'fault\']==\'live-after-restore\' and state[\'flags_restored\']:\n                    raise snapshot.FormalSnapshotError(\'TASK_TEST_LIVE_OWNER_REFUSED\')\n            checks=producer.ProductionAResumeChecks(claim,release_raw,Path(sealed[\'path\']).read_bytes(),sealed[\'sha256\'],\n                resume,guard=live_owner,thaw_guard=claim.assert_owned)\n            credential=root/\'mysql-credential\';write(credential,b\'task-only-shared-join-input\\n\')\n            originals={role:dict(path=sources[\'controlled-\'+role+\'-inspect.json\'],\n                source=dict(present=True,containerId=digest(role.encode()))) for role in split.ROLES}\n            actions=controlled.ControlledSharedContainerActions(originals,object())\n            request=dict(schemaVersion=\'release-formal-capture-request/v12\',action=\'capture-shared-payload\',\n                claimRoot=str(claim.root),claimBinding=binding,mysqlContainerId=\'d\'*64,mysqlCredentialFile=str(credential),\n                totalSeconds=120,sharedScopeSha256=digest((claim.root/\'shared-container-original.json\').read_bytes()),\n                originalHealthSha256=digest((claim.root/\'shared-health-original.json\').read_bytes()),ingressRulesSha256=\'f\'*64,\n                database=target[\'database\'],sourceRoot=\'/task/source/minio\',sourceScope=source,\n                dataLimits=dict(max_rows=1000,max_nodes=10000,max_bytes=1048576,seconds=30),\n                rejectionProbe=dict(table=\'task_probe\',column=\'id\'),\n                filesystemFence=dict(mount=\'/task/source\',seconds=3,binding=dict(operationId=binding[\'operationId\'],\n                    bootId=host_boot,mount=dict(source=\'/dev/task-fixture\',device=\'9:1\',fstype=\'ext4\',target=\'/task/source\'))))\n            yield dict(root=root,claim=claim,fd=fd,signature=signature,source=source,prefence=prefence,state=state,\n                checks=checks,actions=actions,request=request,credential=credential,approval=approval,registry=registry,\n                config_root=config_root,fixture_archive=fixture_archive,image_ids=image_ids,shared_originals=shared_originals)\n            claim.close();claim=None\n    finally:\n        if claim is not None:claim.close()\n        snapshot.observation_deadline.reset(token)\n\n\n@pytest.mark.parametrize(\'fault\',[\'none\', \'config-source\', \'config-copy\', \'config-mode\', \'config-root-mode\', \'config-existing\', \'config-after-flags\', \'image-layer\', \'image-config\', \'image-export-nonzero\', \'image-export-stderr\', \'image-inode\', \'image-mode\', \'image-receipt\', \'image-after-flags\', \'approval-after-images\', \'live-after-restore\', \'inventory-after-seal\', \'closed-owner\', \'image-child-owner\'])\ndef test_native_original_content_precedes_restart_and_retains_owner(native_case,monkeypatch,fault):\n    case=native_case;claim=case[\'claim\'];state=case[\'state\'];state[\'fault\']=fault;calls=state[\'calls\']\n    req=case[\'request\'];source=case[\'source\'];root=claim.root\n    config_root=case[\'config_root\'];image_path=root/\'payload\'/\'original-application-images.tar\'\n    children=[];changed=set()\n    if fault==\'config-mode\':(config_root/\'.env\').chmod(0o666)\n    if fault==\'config-root-mode\':config_root.chmod(0o755)\n    if fault==\'config-source\':(config_root/\'.env\').write_bytes(b\'CHANGED_SOURCE_ORIGINAL\')\n    if fault==\'config-existing\':\n        (root/\'original-configs\').mkdir(mode=0o700);write(root/\'original-configs\'/\'retained\',b\'RETAIN\')\n    if fault==\'inventory-after-seal\':(root/\'production-a-source-inventory.json\').write_bytes(b\'CHANGED_INVENTORY\')\n    if fault in (\'image-layer\',\'image-config\'):\n        original_image_archive(case[\'fixture_archive\'],\'layer\' if fault==\'image-layer\' else \'config\')\n    def inspect_actor(identity):\n        assert not state[\'frozen\'];claim.assert_owned()\n        originals=case[\'shared_originals\'];matches=[v for v in originals.values() if v[\'id\']==identity]\n        assert len(matches)==1\n        row=json.loads(snapshot._canonical_bytes(matches[0]));row[\'state\'].update(Running=False,FinishedAt=\'2026-10-02T00:00:00Z\')\n        return row\n    monkeypatch.setattr(snapshot,\'inspect_formal_container\',inspect_actor)\n    native_process=subprocess.Popen\n    def image_process(argv,**kwargs):\n        assert argv==[\'/usr/bin/docker\',\'--host\',\'unix:///var/run/docker.sock\',\'image\',\'save\',*sorted(set(case[\'image_ids\'].values()))]\n        assert \'raw\' in calls and not state[\'frozen\'] and not state[\'mysql_running\']\n        assert not any(c.startswith(\'start:\') for c in calls);calls.append(\'images\')\n        script=\'import pathlib,sys;sys.stdout.buffer.write(pathlib.Path(sys.argv[1]).read_bytes());sys.stdout.flush();sys.exit(int(sys.argv[2]))\'\n        if fault==\'image-export-stderr\':script=\'import sys;sys.stderr.write("EXPLICIT_PROTOCOL_STDERR");\'+script\n        if fault==\'image-child-owner\':script=\'import time;time.sleep(15);\'+script\n        child=native_process([sys.executable,\'-I\',\'-B\',\'-c\',script,str(case[\'fixture_archive\']),\n            \'1\' if fault==\'image-export-nonzero\' else \'0\'],**kwargs)\n        children.append(child);return child\n    monkeypatch.setattr(images,\'_image_export_process\',image_process)\n    shared_raw=(root/\'shared-container-original.json\').read_bytes();health_raw=(root/\'shared-health-original.json\').read_bytes()\n    flows=[dict(source=\'192.0.2.1\',destination=\'192.0.2.2\',ports=[3306])]\n    ingress_state=dict(phase=\'CAPTURE_RESTRICTED\',rulesSha256=req[\'ingressRulesSha256\'],tableHandle=17)\n    monkeypatch.setattr(ingress,\'assert_ingress\',lambda *a,**k:dict(ingress_state))\n    monkeypatch.setattr(ingress,\'render_recovery_policy\',lambda *a:None)\n    monkeypatch.setattr(callers,\'guard_external_callers\',lambda *a:dict(state=\'KNOWN_EXTERNAL_CALLERS_STOPPED_PENDING_FULL_EXCLUSION\'))\n    monkeypatch.setattr(producer,\'verify_shared_request_binding\',lambda *a:dict(shared=shared_raw,health=health_raw))\n    monkeypatch.setattr(producer,\'verify_shared_database_before_stop\',lambda *a:None)\n    class SqlProtocol:\n        def __init__(self,container,*a,**k):self.container=container\n    monkeypatch.setattr(producer,\'PrivateDockerMySql\',SqlProtocol)\n    def source_guard():\n        assert not state[\'frozen\'],\'source callback during frozen phase\'\n        claim.assert_owned();calls.append(\'source\')\n        if fault==\'changed-request\' and any(e[\'step\']==\'db-fenced\' for e in claim.events):req[\'database\']=\'changed\'\n        if fault==\'config-copy\' and any(e[\'step\']==\'shared-frontend-stopped\' for e in claim.events) and fault not in changed:\n            changed.add(fault);(root/\'original-configs\'/\'data-env.bin\').write_bytes(b\'CHANGED_COPY_AFTER_STOP\')\n        if children and fault==\'image-child-owner\':raise snapshot.FormalSnapshotError(\'TASK_TEST_IMAGE_CHILD_OWNER_REFUSED\')\n        if children and fault==\'approval-after-images\' and fault not in changed:\n            changed.add(fault);case[\'approval\'].write_bytes(b\'CHANGED_APPROVAL_AFTER_EXPORT\')\n        if children and fault==\'image-mode\' and fault not in changed:\n            changed.add(fault);image_path.chmod(0o666)\n        if fault==\'image-receipt\' and (image_path.parent/\'050-original-images.json\').exists() and fault not in changed:\n            changed.add(fault);(image_path.parent/\'050-original-images.json\').write_bytes(b\'CHANGED_IMAGE_RECEIPT\')\n    def thaw(selected):\n        assert selected is claim and not state[\'frozen\'],\'thaw observer during frozen phase\'\n        if fault==\'thaw\' and any(e[\'step\']==\'split-captured\' for e in claim.events):\n            raise snapshot.FormalSnapshotError(\'FORMAL_SHARED_THAW_REQUIRED\')\n        return dict(nativeState=\'THAWED_COPY_ACCEPTED_PENDING_FORMAL_BARRIERS\')\n    monkeypatch.setattr(split,\'_thaw_receipt\',thaw)\n    def stop_writer(selected,role,**kwargs):\n        kwargs[\'guard\']();calls.append(\'stop:\'+role)\n        claim.record(\'shared-\'+role+\'-stop-intent\',dict(explicitProtocol=True))\n        if role==split.STOP_ORDER[0] and fault in (\'credential-bytes\',\'credential-inode\'):\n            credential=case[\'credential\'];raw=credential.read_bytes()\n            if fault==\'credential-inode\':credential.rename(credential.with_suffix(\'.retained\'));write(credential,raw)\n            else:credential.write_bytes(b\'CHANGED_PRIVATE_INPUT\\n\')\n        claim.record(\'shared-\'+role+\'-stopped\',dict(explicitProtocol=True));kwargs[\'guard\']()\n    monkeypatch.setattr(split,\'stop_controlled_shared_writer\',stop_writer)\n    monkeypatch.setattr(split,\'assert_controlled_prefence_stops\',lambda *a,**k:k[\'guard\']())\n    def fence(selected,mysql,*,guard):\n        guard();calls.append(\'fence\');flags=dict(case[\'prefence\'])\n        if fault==\'prefence-drift\':flags[\'readOnly\']=1\n        raw=snapshot._canonical_bytes(flags);claim._write(root/\'database-original.json\',raw)\n        claim.record(\'db-fence-intent\',dict(originalFlags=flags,originalSha256=digest(raw)))\n        guard();claim.record(\'db-fenced\',dict(originalSha256=digest(raw),reconciled=False));guard()\n    monkeypatch.setattr(snapshot,\'acquire_claimed_database_fence\',fence)\n    def sql(*a,**kwargs):\n        kwargs[\'guard\']();calls.append(\'sql\')\n        if fault==\'sql\':raise snapshot.FormalSnapshotError(\'TASK_TEST_SQL_REFUSED\')\n        claim.record(\'split-sql-intent\',{});claim.record(\'split-sql-captured\',{})\n        return dict(state=\'SQL_CAPTURED_PENDING_RAW_AND_FULL_BARRIERS\',sourceScope=source,sourceMigrationEvidence={})\n    monkeypatch.setattr(split,\'capture_sql_phase\',sql)\n    def stop_mysql(selected,role,**kwargs):\n        assert role==\'mysql\';kwargs[\'guard\']();claim.record(\'shared-mysql-stop-intent\',{})\n        kwargs[\'guard\']();calls.append(\'stop:mysql\');state[\'mysql_running\']=False\n        if fault==\'closed-owner\':claim.close()\n        if fault==\'approval-drift\':case[\'approval\'].write_bytes(b\'CHANGED_TASK_ONLY_APPROVAL\')\n        kwargs[\'guard\']()\n        if fault==\'mysql-stop\':raise snapshot.FormalSnapshotError(\'TASK_TEST_STOP_UNCERTAIN\')\n        claim.record(\'shared-mysql-stopped\',{})\n    monkeypatch.setattr(split,\'stop_controlled_capture_database\',stop_mysql)\n    def raw(*a,**kwargs):\n        kwargs[\'guard\']();calls.append(\'raw\');state[\'frozen\']=True\n        kwargs[\'native_guard\']();state[\'frozen\']=False\n        if fault==\'raw\':raise snapshot.FormalSnapshotError(\'TASK_TEST_RAW_REFUSED\')\n        (root/\'payload\').mkdir(mode=0o700)\n        claim.record(\'split-raw-intent\',{});claim.record(\'split-captured\',{});kwargs[\'guard\']()\n        return dict(state=\'CAPTURED_PENDING_FULL_BARRIER_PROOF\',sourceScope=source,sourceMigrationEvidence={})\n    monkeypatch.setattr(split,\'capture_raw_phase\',raw)\n    def transition(selected,value,*,guard,thaw_guard):\n        if fault==\'image-inode\':\n            raw=image_path.read_bytes();image_path.rename(image_path.with_suffix(\'.retained\'));write(image_path,raw)\n        guard();thaw_guard();calls.append(\'transition\');claim.record(\'ingress-recovery-intent\',{})\n        rules=snapshot._canonical_bytes([dict(table=dict(handle=18))]);claim._write(root/\'ingress-recovery-installed.json\',rules)\n        ingress_state.update(phase=\'RECOVERY_DEPENDENCIES_PERMITTED\',tableHandle=18,rulesSha256=digest(rules))\n        claim.record(\'ingress-recovery-permitted\',dict(rulesSha256=digest(rules),planSha256=digest(snapshot._canonical_bytes(value))))\n        guard();thaw_guard();return dict(ingress_state)\n    monkeypatch.setattr(ingress,\'permit_recovery_dependencies\',transition)\n    def begin(selected,*,guard,thaw_guard):\n        guard();thaw_guard();claim.record(\'shared-recovery-intent\',{})\n    monkeypatch.setattr(split,\'begin_shared_recovery\',begin)\n    def start(selected,role,action,**kwargs):\n        assert action==\'start\';kwargs[\'guard\']();calls.append(\'start:\'+role)\n        if fault==\'mysql-start\' and role==\'mysql\' or fault==\'writer-start\' and role==\'redis\':\n            claim.record(\'shared-\'+role+\'-restore-intent\',{})\n            raise snapshot.FormalSnapshotError(\'TASK_TEST_START_UNCERTAIN\')\n        if role==\'mysql\':state[\'mysql_running\']=True\n        else:assert state[\'flags_restored\'] and \'live-owner\' in calls[calls.index(\'flags\')+1:]\n        claim.record(\'shared-\'+role+\'-restored\',{});kwargs[\'guard\']()\n    monkeypatch.setattr(split,\'_shared_action\',start)\n    def flags(selected,mysql,*,guard,thaw_guard):\n        guard();calls.append(\'flags\')\n        if fault==\'flags\':raise snapshot.FormalSnapshotError(\'TASK_TEST_FLAGS_UNCERTAIN\')\n        state[\'flags_restored\']=True;claim.record(\'shared-db-restored\',{})\n        if fault==\'config-after-flags\':(config_root/\'.env\').write_bytes(b\'CHANGED_CONFIG_AFTER_FLAGS\')\n        if fault==\'image-after-flags\':image_path.write_bytes(b\'CHANGED_IMAGE_AFTER_FLAGS\')\n        guard()\n    monkeypatch.setattr(split,\'restore_shared_database_flags\',flags)\n    monkeypatch.setattr(split,\'verify_shared_restoration\',lambda *a,**k:dict(state=\'ORIGINAL_SERVICES_RESTORED_PENDING_CALLER_PROOF\'))\n    if fault==\'credential-mode\':case[\'credential\'].chmod(0o666)\n    def call():\n        try:\n            return producer.run_production_a_shared_lifecycle(claim,req,approved_source=source,\n                resume_checks=case[\'checks\'],container_actions=case[\'actions\'],source_guard=source_guard,\n                maintenance_end=time.time()+90,recovery_flows=flows)\n        except snapshot.FormalSnapshotError as error:\n            # Report actual failing production-frame clocks; no clock patch,\n            # threshold change, expected-code downgrade, retry or swallowing.\n            clocks=[];trace=error.__traceback__\n            while trace is not None:\n                frame=trace.tb_frame;values=frame.f_locals\n                if frame.f_code.co_filename==producer.__file__:\n                    sample={key:values[key] for key in (\'elapsed\',\'wall\',\'now_wall\',\'wall_origin\',\n                        \'clock_origin\',\'clock_end\',\'authority_end\',\'issued\',\'expires\',\'deadline\')\n                        if key in values and type(values[key]) in (int,float)}\n                    if sample:\n                        sample.update(function=frame.f_code.co_name,line=trace.tb_lineno)\n                        selected=values.get(\'self\')\n                        if type(selected) is producer.ProductionAResumeChecks:\n                            sample.update(ownerWall=selected._wall,ownerMonotonic=selected._clock,\n                                issuedAt=selected.consumed[\'issuedAt\'],expiresAt=selected.consumed[\'expiresAt\'])\n                        clocks.append(sample)\n                trace=trace.tb_next\n            print(\'ACTUAL_NATIVE_CLOCK_FAILURE \'+json.dumps(dict(error=str(error),clocks=clocks,\n                flagsRestored=state[\'flags_restored\'],mysqlRunning=state[\'mysql_running\'],\n                steps=[event[\'step\'] for event in claim.events],calls=calls)),flush=True)\n            raise\n    if fault==\'none\':\n        result,content_guard,thaw_guard,mysql,deadline=call()\n        order=[\'stop:\'+r for r in split.STOP_ORDER[:-1]]+[\'fence\',\'sql\',\'stop:mysql\',\'raw\',\'transition\',\n            \'start:mysql\',\'flags\']+[\'start:\'+r for r in split.RESTORE_ORDER[1:]]\n        assert [c for c in calls if c.startswith((\'stop:\',\'start:\')) or c in (\'fence\',\'sql\',\'raw\',\'transition\',\'flags\')]==order\n        assert result[\'originalImages\'][\'images\']==case[\'image_ids\']\n        assert result[\'originalConfigs\'][\'productionEvidence\'] is False\n        assert calls.index(\'images\')<calls.index(\'start:mysql\')\n        assert result[\'claimReleased\'] is False and result[\'fullRunnerCompleted\'] is False and result[\'productionEvidence\'] is False\n    else:\n        expected=dict(sql=\'TASK_TEST_SQL_REFUSED\',**{\n            \'mysql-stop\':\'TASK_TEST_STOP_UNCERTAIN\',\'raw\':\'TASK_TEST_RAW_REFUSED\',\n            \'thaw\':\'FORMAL_SHARED_THAW_REQUIRED\',\'live-after-restore\':\'TASK_TEST_LIVE_OWNER_REFUSED\',\n            \'mysql-start\':\'TASK_TEST_START_UNCERTAIN\',\'flags\':\'TASK_TEST_FLAGS_UNCERTAIN\',\n            \'writer-start\':\'TASK_TEST_START_UNCERTAIN\',\'prefence-drift\':\'FORMAL_PRODUCTION_A_DATABASE_ORIGINAL_CHANGED\',\n            \'approval-drift\':\'FORMAL_PRODUCTION_A_RESUME_CONTROL_CHANGED\',\n            \'changed-request\':\'FORMAL_PRODUCTION_A_SHARED_INPUT_CHANGED\',\n            \'credential-bytes\':\'FORMAL_PRODUCTION_A_SHARED_CREDENTIAL_CHANGED\',\n            \'credential-inode\':\'FORMAL_PRODUCTION_A_SHARED_CREDENTIAL_CHANGED\',\n            \'credential-mode\':\'FORMAL_BOOT_FILE_UNTRUSTED\',\n            \'closed-owner\':\'FORMAL_MAINTENANCE_OWNER_INVALID\'})\n        expected.update({\n            \'config-source\':\'FORMAL_SELECTED_CONFIG_SOURCE_CHANGED\',\n            \'config-copy\':\'FORMAL_SELECTED_CONFIG_COPY_CHANGED\',\n            \'config-mode\':\'FORMAL_BOOT_FILE_UNTRUSTED\',\'config-root-mode\':\'FORMAL_SELECTED_CONFIG_CAPTURE_INVALID\',\n            \'config-existing\':\'FORMAL_SNAPSHOT_EXISTS\',\'config-after-flags\':\'FORMAL_SELECTED_CONFIG_SOURCE_CHANGED\',\n            \'image-layer\':\'FORMAL_ORIGINAL_IMAGE_CONTENT_CHANGED\',\'image-config\':\'FORMAL_ORIGINAL_IMAGE_CONTENT_CHANGED\',\n            \'image-export-nonzero\':\'FORMAL_ORIGINAL_IMAGE_EXPORT_FAILED\',\'image-export-stderr\':\'FORMAL_ORIGINAL_IMAGE_EXPORT_FAILED\',\n            \'image-inode\':\'FORMAL_PRODUCTION_A_ORIGINAL_IMAGE_CHANGED\',\'image-mode\':\'FORMAL_PRODUCTION_A_ORIGINAL_IMAGE_REQUIRED\',\n            \'image-receipt\':\'FORMAL_PRODUCTION_A_ORIGINAL_IMAGE_CHANGED\',\'image-after-flags\':\'FORMAL_PRODUCTION_A_ORIGINAL_IMAGE_CHANGED\',\n            \'approval-after-images\':\'FORMAL_PRODUCTION_A_RESUME_CONTROL_CHANGED\',\n            \'inventory-after-seal\':\'FORMAL_PRODUCTION_A_ORIGINAL_INVENTORY_CHANGED\',\n            \'image-child-owner\':\'TASK_TEST_IMAGE_CHILD_OWNER_REFUSED\'})\n        with pytest.raises(snapshot.FormalSnapshotError,match=\'^\'+expected[fault]+\'$\'):call()\n        # Refusal must reach this scenario\'s intended seam; an earlier typed\n        # error is a failure, not evidence of safe uncertainty handling.\n        reached={\'sql\':\'sql\',\'mysql-stop\':\'stop:mysql\',\'raw\':\'raw\',\'thaw\':\'raw\',\n            \'live-after-restore\':\'flags\',\'mysql-start\':\'start:mysql\',\'flags\':\'flags\',\n            \'writer-start\':\'start:redis\',\'prefence-drift\':\'fence\',\'approval-drift\':\'stop:mysql\',\n            \'changed-request\':\'fence\',\'credential-bytes\':\'stop:frontend\',\'credential-inode\':\'stop:frontend\',\n            \'closed-owner\':\'stop:mysql\'}\n        if fault in reached:assert reached[fault] in calls\n        if fault==\'live-after-restore\':assert state[\'flags_restored\'] and \'live-owner\' in calls[calls.index(\'flags\')+1:]\n        if fault.startswith(\'image-\') and fault not in (\'image-after-flags\',):assert \'images\' in calls\n        if fault not in (\'live-after-restore\',\'mysql-start\',\'flags\',\'writer-start\',\'config-after-flags\',\'image-after-flags\'):\n            assert not any(c.startswith(\'start:\') for c in calls)\n        if fault in (\'live-after-restore\',\'mysql-start\',\'flags\'):\n            assert not any(c.startswith(\'start:\') and c!=\'start:mysql\' for c in calls)\n        if fault==\'credential-mode\':assert not any(c.startswith(\'stop:\') for c in calls)\n    assert all(child.poll() is not None for child in children),\'ACTUAL_EXPORT_CHILD_NOT_TERMINAL\'\n    if fault in (\'config-source\',\'config-mode\',\'config-root-mode\',\'config-existing\',\'inventory-after-seal\'):\n        assert not any(c.startswith(\'stop:\') for c in calls)\n    if fault in (\'config-after-flags\',\'image-after-flags\'):\n        assert state[\'flags_restored\'] and not any(c.startswith(\'start:\') and c!=\'start:mysql\' for c in calls)\n    assert len(list(case[\'registry\'].iterdir()))==1\n    assert sum(e[\'step\']==\'production-a-resume-consumed\' for e in claim.events)==1\n    if fault==\'closed-owner\':assert claim._fd is None\n    else:\n        assert claim._fd==case[\'fd\'] and boot._signature(os.fstat(claim._fd))==case[\'signature\']\n        with pytest.raises(snapshot.FormalSnapshotError,match=\'FORMAL_MAINTENANCE_BUSY\'):\n            snapshot.FormalMaintenanceClaim(claim.root,json.loads(claim._plan),resume=True)\n'
