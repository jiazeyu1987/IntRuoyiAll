"""Local owner protocol only: actual files/FDs, explicit phase actors.

No native JVM, nft, database, service, signature or full exclusion attestation.
Windows uses the same explicit import-only fcntl bridge as the adjacent entry
tests. The production full-source refusal is exercised separately; only the
finite tail protocol tests replace that refusal, never via production inputs.
"""
import contextlib
import copy
import hashlib
import importlib
import os
from pathlib import Path, PurePosixPath
import sys
import types

import pytest

sys.path.insert(0, str(Path(__file__).parents[1] / 'linux'))
import formal_capture_producer as producer


def sha(raw):
    return hashlib.sha256(raw).hexdigest()


def write(path, value):
    raw = producer.snapshot._canonical_bytes(value)
    path.write_bytes(raw)
    path.chmod(0o600)
    return raw


@pytest.fixture
def protocol(tmp_path, monkeypatch):
    if os.name == 'nt':
        unavailable = types.ModuleType('fcntl')
        def refused(*args, **kwargs):
            raise AssertionError('Local owner protocol cannot perform native flock')
        unavailable.flock = refused
        monkeypatch.setitem(sys.modules, 'fcntl', unavailable)
    launch = importlib.import_module('formal_original_launch')
    capture = importlib.import_module('formal_original_launch_capture')
    writer = importlib.import_module('formal_original_open_writer')
    ingress = importlib.import_module('formal_ingress_control')
    health = importlib.import_module('formal_service_health')
    split = importlib.import_module('formal_split_capture')
    ledger = importlib.import_module('formal_source_migration_ledger')
    boot_module = importlib.import_module('formal_boot_state')
    host_module = importlib.import_module('formal_host_admission')
    configs = importlib.import_module('formal_selected_config_capture')
    root = tmp_path / 'claim'; root.mkdir()
    if os.name == 'nt':
        # Only the POSIX socket spelling validator needs the POSIX path type.
        # All private files and held descriptor below remain actual Windows IO.
        monkeypatch.setattr(capture,'Path',PurePosixPath)
        absolute=boot_module._absolute
        def local_claim_path_bridge(path):
            if path==str(root):return root
            return absolute(path)
        monkeypatch.setattr(boot_module,'_absolute',local_claim_path_bridge)
        def local_receipt_file_bridge(claim):
            path=claim.root/'shared-dependencies-health.json'
            claim.assert_owned(); raw=path.read_bytes()
            assert 0<len(raw)<=32768 and len(raw)==path.stat().st_size
            return raw
        monkeypatch.setattr(health,'read_shared_dependency_receipt',local_receipt_file_bridge)
    credential = tmp_path / 'credential'; credential.write_bytes(b'owned-secret\n'); credential.chmod(0o600)
    binding = dict(operationId='prod-owner-protocol', sourceFingerprint='a'*64,
                   databaseUuid='00000000-0000-0000-0000-000000000001', minioContainerId='b'*64)
    boot = '00000000-0000-0000-0000-000000000002'
    source = dict(environment='prod', database='ruoyi', databaseUuid=binding['databaseUuid'],
                  targetFingerprint=binding['sourceFingerprint'])
    stopped = dict(Running=False, Paused=False, Restarting=False, Dead=False, OOMKilled=False,
                   ExitCode=0, StartedAt='2026-10-07T00:00:00Z', FinishedAt='2026-10-07T00:01:00Z')
    original_state = dict(stopped, Running=True, FinishedAt='0001-01-01T00:00:00Z')
    shared = {role:dict(id=(str(i)*64), image='sha256:'+'c'*64, state=copy.deepcopy(original_state))
              for i, role in enumerate(sorted(split.ROLES), 1)}
    shared['minio']['id'] = binding['minioContainerId']
    shared_raw = write(root/'shared-container-original.json', shared)
    health_sha = sha(write(root/'shared-health-original.json', dict(owned='health')))
    external_sha = sha(write(root/'external-callers-original.json', dict(owned='callers')))
    old_rules = sha(write(root/'ingress-installed.json', [dict(table=dict(handle=17))]))
    write(root/'ingress-plan.json',dict(bootId=boot,networkNamespace='net:[11]',
        hostAddresses=['192.0.2.1','2001:db8::1'],hostPorts=[8080],
        targets=[dict(address='10.0.0.3',ports=[3306])]))
    cap = dict(schemaVersion='release-formal-capture-request/v12', action='capture-shared-payload',
        claimRoot=str(root), claimBinding=binding, mysqlContainerId=shared['mysql']['id'],
        mysqlCredentialFile=str(credential), totalSeconds=90, sharedScopeSha256=sha(shared_raw),
        originalHealthSha256=health_sha, ingressRulesSha256=old_rules, database='ruoyi',
        sourceRoot='/owned/source/minio', sourceScope=source,
        dataLimits=dict(max_rows=10, max_nodes=10, max_bytes=1024, seconds=60),
        rejectionProbe=dict(table='owned', column='id'),
        filesystemFence=dict(mount='/owned/source', seconds=1, binding=dict(operationId=binding['operationId'],
            bootId=boot, mount=dict(source='/dev/owned', device='9:1', fstype='ext4', target='/owned/source'))))
    cap_path = tmp_path/'capture.json'; cap_raw = write(cap_path, cap)
    config_root=tmp_path/'original-config';config_root.mkdir();(config_root/'config').mkdir()
    config_originals={}
    for role,relative in configs.FILES.items():
        path=config_root/relative;path.write_bytes(('PRIVATE_CONFIG_'+role).encode());info=path.stat()
        config_originals[role]=dict(path=str(path),bytes=info.st_size,sha256=sha(path.read_bytes()),
            fileSystemId=info.st_dev,inode=info.st_ino,mtimeNs=info.st_mtime_ns,ctimeNs=info.st_ctime_ns)
    config_observation=tmp_path/'config-originals.json';config_raw=write(config_observation,config_originals)
    target = tmp_path/'source-target.json'; target_raw = write(target, dict(owned='target',root=str(config_root),
        environment='prod',host='192.0.2.57',machineId='9'*32,database='ruoyi',
        databaseServerUuid=binding['databaseUuid']))
    release_operation=dict(schemaVersion='release-deployment-operation/v1',attemptId='8'*64,
        revision=2,state='STARTED',phase='production-backup',writeAttempted=True,
        binding=dict(scope=dict(environment='prod',host='192.0.2.57',machineId='9'*32,database='ruoyi',
            serverUuid=binding['databaseUuid']),lockOperationId=binding['operationId'],releaseTag='owner-protocol',
            leaseTokenDigest='7'*64,packageDigest='6'*64,manifestDigest='5'*64,planDigest='4'*64),
        before={},target={},phases=[dict(name='production-backup',status='STARTED',
            startedAt='2026-10-09T00:00:00Z',completedAt=None)],verification=None,observation=None,originalError=None,
        observationErrors=[],finalizationErrors=[],receiptDigest=None,history=None,completion=None)
    release_path=tmp_path/'deployment-operation.json';release_raw=write(release_path,release_operation)
    approval = tmp_path/'signed-open.json'; write(approval, dict(owned='signature-boundary'))
    key = tmp_path/'public-key'; key.write_bytes(b'owned-public-key'); key.chmod(0o644)
    expected = dict(operationId=binding['operationId'], role='backend', hostBootId=boot,
        containerId=shared['backend']['id'], launchSha256='d'*64, artifactSha256='e'*64,
        originalOpenSha256='f'*64, closedSha256='0'*64)
    launch_request = dict(schemaVersion='release-formal-capture-request/v11', action='observe-running-original-jvm',
        claimRoot=str(root), claimBinding=binding, totalSeconds=60, hostBootId=boot, role='backend',
        expectedLaunchSha256=expected['launchSha256'], jvmBinding=dict(instanceId='owned-backend',
            artifactSha256=expected['artifactSha256'], socket=dict(path='/owned/control.sock',
                identity=dict(device=1, inode=2), pid=55, uid=0, gid=0)))
    fields = {k:copy.deepcopy(cap[k]) for k in ('claimRoot','claimBinding','mysqlContainerId',
        'mysqlCredentialFile','totalSeconds','sharedScopeSha256','originalHealthSha256')}
    fields.update(schemaVersion='release-formal-capture-request/v7', action='restore-shared-dependencies')
    gate_path=tmp_path/'deployment-gate.json'
    gate=dict(schemaVersion='release-deployment-gate/v1',scope=copy.deepcopy(release_operation['binding']['scope']),
        binding=copy.deepcopy(release_operation['binding']),revision=release_operation['revision'],
        state=release_operation['state'],attemptId=release_operation['attemptId'],
        operationDigest=sha(release_raw),receiptDigest=None)
    gate_raw=write(gate_path,gate)
    context_path=tmp_path/'deployment-context.json'
    operation_input=dict(schemaVersion='release-formal-current-deployment-input/v1',
        operation=dict(path=str(release_path),sha256=sha(release_raw)),
        gate=dict(path=str(gate_path),sha256=sha(gate_raw)),
        expectedAttemptId=release_operation['attemptId'],expectedLeaseTokenDigest=release_operation['binding']['leaseTokenDigest'])
    owner = dict(schemaVersion='release-formal-original-lifecycle-owner-input/v4',
        action='capture-and-restore-original-backend', totalSeconds=90,
        captureRequest=dict(path=str(cap_path), sha256=sha(cap_raw)),
        approvedSourceTarget=dict(path=str(target), sha256=sha(target_raw)), launchRequest=launch_request,
        originalConfigs=dict(path=str(config_observation),sha256=sha(config_raw)),
        releasePackage={k:release_operation['binding'][k] for k in ('releaseTag','packageDigest','manifestDigest','planDigest')},
        recoveryFlows=[dict(source='10.0.0.2', destination='10.0.0.3', ports=[3306])],
        dependencyAuthor=dict(schemaVersion='release-formal-dependency-author-input/v1',
            ingressRulesSource='current-transition-readback', requestFields=fields),
        openApproval=dict(path=str(approval), sha256=sha(approval.read_bytes()), trustedKeyPath=str(key),
            trustedKeySha256=sha(key.read_bytes()), binding=expected))
    owner_path = tmp_path/'owner.json'
    state = dict(calls=[], held=False, fd=None, stopped=stopped, old_rules=old_rules,
                 phases=[], deadlines=[], stop_error=False, receipt_error=False, drift=None, expired=False,
                 capture_prepared=True)
    class Claim:
        def __init__(self, selected, selected_binding, *, resume):
            assert selected == str(root) and selected_binding == binding and resume is True
            self.root=root; self._plan=producer.snapshot._canonical_bytes(binding)
            self.events=[dict(step='external-caller-scope',details=dict(scopeSha256=external_sha))]
            self.events.append(dict(step='boot-backend-bound',details=dict(hostBootId=boot,
                binding=dict(role='backend',path='/owned/boot.json',instanceId=state.get('boot_instance','owned-backend'),
                    artifactSha256=expected['artifactSha256'],originalOpenSha256=expected['originalOpenSha256'],
                    closedSha256=expected['closedSha256'],epoch=binding['operationId']))))
            if state.get('capture_prepared'):
                for step in ('db-fence-intent','db-fenced','host-originals-bound'):
                    self.events.append(dict(step=step,details=dict(originalSha256='a'*64)))
                if state.get('derived_bound',True):
                    self.events.append(dict(step='host-derived-originals-bound',details=dict(owned='derived-protocol')))
                for unit in ('crond','dnf-makecache','systemd-tmpfiles-clean','unbound-anchor','cockpit'):
                    # Already masked/inactive entries legitimately have only closed.
                    suffixes=('-closed',) if unit=='cockpit' else ('-mask-intent','-masked','-stop-intent','-closed')
                    for suffix in suffixes:self.events.append(dict(step='host-'+unit+suffix,details={}))
                for prefix in ('boot-backend','live-backend','external-test-backend'):
                    pair=('-stop-intent','-stopped') if prefix.startswith('external') else ('-close-intent','-closed')
                    for suffix in pair:self.events.append(dict(step=prefix+suffix,details={}))
            self.events.extend(copy.deepcopy(state.get('pre_events',[])))
            self.events=[event for event in self.events if event['step'] not in state.get('remove_events',set())]
            for step in state.get('move_events_to_end',[]):
                selected=[event for event in self.events if event['step']==step]
                self.events=[event for event in self.events if event['step']!=step]+selected
            state['claim']=self; state['calls'].append('claim')
        def assert_owned(self):
            assert not state.get('claim_closed', False)
        def _read(self, path):
            return Path(path).read_bytes()
        def close(self):
            assert not state['held']; state['claim_closed']=True; state['calls'].append('close-claim')
    class Retained:
        original=dict(sha256=expected['artifactSha256'])
        def assert_unchanged(self, *, deadline):
            assert state['held']; os.fstat(state['fd']); assert deadline>clock()
    @contextlib.contextmanager
    def held(role, cid, image, selected_boot, *, deadline):
        assert (role,cid,image,selected_boot)==('backend',shared['backend']['id'],shared['backend']['image'],boot)
        assert deadline<=clock()+60
        state['held']=True; state['fd']=os.open(key,os.O_RDONLY); state['calls'].append('hold')
        try:
            yield dict(role='backend', bootId=boot, inspect=dict(containerId=cid,imageId=image,
                digests=dict(launchSha256=expected['launchSha256'])), currentJar=dict(sha256=expected['artifactSha256'])), Retained()
        finally:
            os.close(state['fd']); state['held']=False; state['calls'].append('close-fd')
    class Recovery:
        def __init__(self, original, retained, socket, instance, *, deadline):
            assert state['held'] and instance=='owned-backend' and socket==launch_request['jvmBinding']['socket']
            self.original=original; self.retained=retained; self.phase='RUNNING'; state['checks']=self
            state['calls'].append('recovery-checks')
        def stopped(self, runtime, *, deadline):
            assert state['held'] and runtime==stopped
            if state['stop_error']:raise producer.FormalSnapshotError('OWNED_STOP_MISMATCH')
            self.phase='STOPPED'; state['calls'].append('stopped')
    def clock():
        return 500.0 if state['expired'] else 100.0
    def thaw(claim):
        assert claim is state['claim']; state['calls'].append('thaw')
        if state.get('thaw_error'):raise producer.FormalSnapshotError('OWNED_THAW_FAILURE')
        return dict(nativeState='THAWED', operationId=binding['operationId'])
    def phase(claim, request, guard, *, approved_source=None,native_guard=None):
        assert claim is state['claim'] and state['held']; os.fstat(state['fd']); guard()
        state['deadlines'].append(producer.snapshot.observation_deadline.get())
        state['phases'].append(copy.deepcopy(request)); state['calls'].append(request['action'])
        if request['action']=='capture-shared-payload':
            assert callable(native_guard);native_guard()
            assert approved_source==source and state['checks'].phase=='RUNNING'
            claim.events.append(dict(step='shared-backend-stopped', details=dict(runtime=copy.deepcopy(stopped))))
            if state['drift']:state['drift']()
            if state.get('expire_after_capture'):state['expired']=True
            return dict(state='SHARED_PAYLOAD_CAPTURED_INGRESS_HELD')
        assert request['ingressRulesSha256']==state['rules_sha'] and approved_source is None
        assert state['checks'].phase=='STOPPED'
        raw=write(root/'shared-dependencies-health.json',dict(owned='real-receipt-file'))
        claim.events.append(dict(step='shared-dependencies-ready',details=dict(sha256='9'*64 if state['receipt_error'] else sha(raw))))
        return dict(state='DEPENDENCIES_RESTORED_PENDING_OPEN_APPROVAL')
    def permit(claim, flows, *, guard, thaw_guard):
        assert claim is state['claim'] and flows==owner['recoveryFlows']; guard(); thaw_guard()
        assert state['checks'].phase=='STOPPED'; state['calls'].append('transition')
        raw=write(root/'ingress-recovery-installed.json',[dict(table=dict(handle=23)),dict(rule=dict(handle=29))])
        state['rules_sha']=sha(raw)
        claim.events.append(dict(step='ingress-recovery-permitted',details=dict(rulesSha256=sha(raw),
            planSha256=sha(producer.snapshot._canonical_bytes(flows)))))
        return dict(phase='RECOVERY_DEPENDENCIES_PERMITTED',rulesSha256=sha(raw),tableHandle=23)
    def full_sources(claim):
        assert claim is state['claim']; state['calls'].append('finite-test-only-full-source-boundary')
    class Sources:
        def __init__(self, claim, selected, rules_sha, caller_sha):
            assert selected==expected and rules_sha==state['rules_sha'] and caller_sha==external_sha
        def __call__(self):state['calls'].append('source-checks')
    def opened(claim,mysql,path,key_path,key_sha,selected,*,expected_receipt_sha256,source_guard,timeout):
        source_guard(); assert state['held']; state['calls'].append('signed-open')
        assert (path,key_path,key_sha,selected)==(str(approval),str(key),sha(key.read_bytes()),expected)
        assert expected_receipt_sha256==sha((root/'shared-dependencies-health.json').read_bytes())
        for step in ('open-intent','open-approval-consumed','open-restored'):
            claim.events.append(dict(step='boot-backend-'+step,details={}))
        return dict(state='SAVED_ORIGINAL_OPEN_RESTORED')
    class Completed:
        def __init__(self,claim,*args,deadline):
            assert [e['step'] for e in claim.events[-3:]]==['boot-backend-open-'+x for x in ('intent','approval-consumed','restored')]
            state['calls'].append('completed-open'); self.claim=claim
    def once(claim,opened_checks,checks,**kwargs):
        assert opened_checks.claim is claim is state['claim'] and checks is state['checks'] and state['held']
        kwargs['guard'](); kwargs['thaw_guard'](); os.fstat(state['fd']); state['calls'].append('once')
        assert kwargs['expected_rules_sha256']==state['rules_sha']
        assert kwargs['expected_external_scope_sha256']==external_sha
        assert state['calls'].count('once')==1
        return dict(state='ORIGINAL_BACKEND_RUNTIME_RECORDED_PENDING_FULL_COLD_CONFIRMATION')
    monkeypatch.setattr(producer,'require_native_runtime',lambda:None)
    class FiniteHostSourceBoundary:
        """Explicit protocol only; native sealed-file tests are in host entry suite."""
        def __init__(self,claim):
            assert claim is state['claim'];self.claim=claim
            if not any(e['step']=='host-derived-originals-bound' for e in claim.events):
                raise producer.FormalSnapshotError('FORMAL_HOST_DERIVED_ORIGINALS_REQUIRED')
        def prepare(self,*,guard,deadline):
            guard();assert deadline>clock()
            if state.get('derived_changed'):
                raise producer.FormalSnapshotError('FORMAL_HOST_DERIVED_ORIGINAL_BINDING_CHANGED')
            state['calls'].append('host-source-prepare')
        def assert_native(self,*,deadline):
            assert deadline>clock();state['calls'].append('host-source-native')
    monkeypatch.setattr(host_module,'ClosedHostAdmissionChecks',FiniteHostSourceBoundary)
    monkeypatch.setattr(producer,'FormalMaintenanceClaim',Claim)
    monkeypatch.setattr(producer,'run_shared_request',phase)
    monkeypatch.setattr(producer,'verify_approved_source_storage',
        lambda *args:dict(owned=(state.get('storage_device',root.stat().st_dev+123),2)))
    monkeypatch.setattr(producer,'PrivateDockerMySql',lambda *args,**kwargs:object())
    monkeypatch.setattr(ledger,'approved_source_scope',lambda *args:copy.deepcopy(source))
    monkeypatch.setattr(launch,'hold_original_launch',held)
    monkeypatch.setattr(launch,'OriginalLaunchRecoveryChecks',Recovery)
    monkeypatch.setattr(ingress,'permit_recovery_dependencies',permit)
    monkeypatch.setattr(split,'_thaw_receipt',thaw)
    monkeypatch.setattr(writer,'OriginalOpenSourceChecks',Sources)
    monkeypatch.setattr(writer,'restore_saved_original_open_with_dependency_health',opened)
    monkeypatch.setattr(writer,'CompletedOriginalOpenChecks',Completed)
    monkeypatch.setattr(capture,'restore_original_backend_once',once)
    def original_images(claim,destination,*,guard,timeout):
        assert claim is state['claim'] and state['held'] and state['checks'].phase=='RUNNING'
        assert destination==root/'payload'/'original-application-images.tar' and 0<timeout<=owner['totalSeconds']
        guard();state['calls'].append('original-images')
        if state.get('image_error'):raise producer.FormalSnapshotError('FORMAL_ORIGINAL_IMAGE_CONTENT_CHANGED')
        destination.parent.mkdir(exist_ok=True);destination.write_bytes(b'EXPLICIT_IMAGE_PROTOCOL_BYTES')
        return dict(sha256=sha(destination.read_bytes()),bytes=destination.stat().st_size,
            images={role:shared[role]['image'] for role in ('backend','frontend')},
            sharedScopeSha256=sha(shared_raw),productionEvidence=False,hostWritersVerified=False)
    monkeypatch.setattr(capture,'capture_original_application_images',original_images)
    def config_check(source,expected,destination,result,*,guard):
        guard();state['calls'].append('config-readback')
        assert source==str(config_root) and expected==config_originals
        for role,relative in configs.FILES.items():
            content=(config_root/relative).read_bytes()
            if sha(content)!=expected[role]['sha256'] or (destination/(role+'.bin')).read_bytes()!=content:
                raise producer.FormalSnapshotError('FORMAL_SELECTED_CONFIG_SOURCE_CHANGED')
        assert producer.snapshot._canonical_bytes(result)==(destination/'config-capture-stage.json').read_bytes()
        if state.get('config_late_error'):raise producer.FormalSnapshotError('FORMAL_SELECTED_CONFIG_COPY_CHANGED')
        guard()
        paths=tuple(config_root/relative for relative in configs.FILES.values())+tuple(
            destination/(role+'.bin') for role in configs.FILES)+(destination/'config-capture-stage.json',
            config_root,config_root/'config',destination)
        def identity(path):
            info=path.lstat()
            return (info.st_dev,info.st_ino,info.st_mode,info.st_size,info.st_mtime_ns,info.st_ctime_ns)
        identities=tuple((path,identity(path)) for path in paths)
        names=frozenset(path.name for path in destination.iterdir())
        def identity_check():
            producer.require(all(identity(path)==saved for path,saved in identities),
                'FORMAL_SELECTED_CONFIG_COPY_CHANGED')
            producer.require(frozenset(path.name for path in destination.iterdir())==names,
                'FORMAL_SELECTED_CONFIG_COPY_CHANGED')
        return dict(configReceiptSha256=sha((destination/'config-capture-stage.json').read_bytes())),identity_check
    def config_copy(source,expected,destination,*,guard):
        guard();state['calls'].append('original-configs')
        assert state['held'] and destination==root/'original-configs'
        if state.get('config_error'):raise producer.FormalSnapshotError('FORMAL_SELECTED_CONFIG_SOURCE_CHANGED')
        destination.mkdir(mode=0o700)
        rows={}
        for role,relative in configs.FILES.items():
            content=(config_root/relative).read_bytes();(destination/(role+'.bin')).write_bytes(content)
            rows[role]=dict(source=expected[role],copyPath=role+'.bin',copySha256=sha(content),copyBytes=len(content))
        result=dict(state='SELECTED_CONFIG_ORIGINALS_CAPTURED_PENDING_FULL_PROTECTION',originals=rows,
            originalObservationSha256=sha(producer.snapshot._canonical_bytes(expected)),
            fullCaptureVerified=False,continuousHostExclusionVerified=False,productionEvidence=False)
        write(destination/'config-capture-stage.json',result);return result
    monkeypatch.setattr(configs,'capture_selected_config_originals',config_copy)
    monkeypatch.setattr(configs,'verify_selected_config_originals',config_check,raising=False)
    monkeypatch.setattr(producer.time,'monotonic',clock)
    def invoke(*, finite_tail=True):
        raw=write(owner_path,owner)
        if finite_tail:
            monkeypatch.setattr(producer,'_require_original_lifecycle_full_sources',full_sources,raising=False)
        context_raw=write(context_path,operation_input)
        return producer.run_approved_original_lifecycle_owner(str(owner_path),sha(raw),
            deployment_context_path=str(context_path),expected_deployment_context_sha256=sha(context_raw))
    return types.SimpleNamespace(invoke=invoke,state=state,owner=owner,path=owner_path,root=root,
        cap_path=cap_path,cap=cap,target=target,approval=approval,launch=launch,health=health,
        config_observation=config_observation,config_root=config_root,config_originals=config_originals,
        release_path=release_path,release_operation=release_operation,operation_input=operation_input,
        context_path=context_path,gate=gate,gate_path=gate_path,
        original_full_gate=getattr(producer,'_require_original_lifecycle_full_sources',None))


def test_explicit_entry_exists_before_any_phase():
    assert callable(getattr(producer,'run_approved_original_lifecycle_owner',None)), 'Missing explicit lifecycle owner entry'


def test_finite_protocol_keeps_same_fd_and_claim_through_exact_once(protocol):
    result=protocol.invoke()
    calls=protocol.state['calls']
    expected=['hold','recovery-checks','capture-shared-payload','stopped','transition',
        'restore-shared-dependencies','signed-open','completed-open','once','close-fd','close-claim']
    assert [call for call in calls if call in expected]==expected
    assert result['state']=='ORIGINAL_BACKEND_RUNTIME_RECORDED_PENDING_FULL_COLD_CONFIRMATION'
    for key in ('fullRunnerCompleted','productionEvidence','fullHostFence','fullDrain','releaseReady','claimReleased','coldStartConfirmed'):
        assert result[key] is False
    assert protocol.state['deadlines']==[190.0,190.0]
    with pytest.raises(OSError):os.fstat(protocol.state['fd'])


def test_dependency_recipe_uses_actual_handle_readback_and_new_sha(protocol):
    before=copy.deepcopy(protocol.owner['dependencyAuthor'])
    result=protocol.invoke()
    derived=protocol.state['phases'][1]
    assert derived['ingressRulesSha256']==protocol.state['rules_sha']!=protocol.state['old_rules']
    assert result['dependencyRequestSha256']==sha(producer.snapshot._canonical_bytes(derived))
    assert protocol.owner['dependencyAuthor']==before


@pytest.mark.parametrize('field',['capture','owner','target','approval','same-bytes-new-inode'])
def test_changed_approved_source_refuses_before_transition(protocol,field):
    path={'capture':protocol.cap_path,'owner':protocol.path,'target':protocol.target,'approval':protocol.approval,
          'same-bytes-new-inode':protocol.path}[field]
    def drift():
        if field=='same-bytes-new-inode':
            data=path.read_bytes();path.rename(path.with_suffix('.old'));path.write_bytes(data);path.chmod(0o600)
        else:path.write_bytes(b'{}\n')
    protocol.state['drift']=drift
    with pytest.raises(producer.FormalSnapshotError,match='LIFECYCLE_INPUT_CHANGED'):protocol.invoke()
    assert 'transition' not in protocol.state['calls'] and 'once' not in protocol.state['calls']
    assert protocol.state['claim_closed'] and not protocol.state['held']


@pytest.mark.parametrize('fault',['claim','old-v7-sha','recipe-source','flow-extra','jvm-artifact','open-cid'])
def test_unbound_stage_input_refuses_before_capture(protocol,fault):
    if fault=='claim':protocol.owner['dependencyAuthor']['requestFields']['claimBinding']['operationId']='other'
    if fault=='old-v7-sha':protocol.owner['dependencyAuthor']['requestFields']['ingressRulesSha256']='1'*64
    if fault=='recipe-source':protocol.owner['dependencyAuthor']['ingressRulesSource']='capture-rules'
    if fault=='flow-extra':protocol.owner['recoveryFlows'][0]['actorId']=1
    if fault=='jvm-artifact':protocol.owner['launchRequest']['jvmBinding']['artifactSha256']='1'*64
    if fault=='open-cid':protocol.owner['openApproval']['binding']['containerId']='a'*64
    with pytest.raises(producer.FormalSnapshotError):protocol.invoke()
    assert 'capture-shared-payload' not in protocol.state['calls']


@pytest.mark.parametrize('fault',['stop','thaw','deadline','receipt'])
def test_failure_does_not_enter_next_phase_or_replay(protocol,fault):
    if fault=='stop':protocol.state['stop_error']=True
    if fault=='thaw':protocol.state['thaw_error']=True
    if fault=='deadline':protocol.state['expire_after_capture']=True
    if fault=='receipt':protocol.state['receipt_error']=True
    with pytest.raises(producer.FormalSnapshotError):protocol.invoke()
    assert 'signed-open' not in protocol.state['calls'] and 'once' not in protocol.state['calls']
    assert protocol.state['calls'].count('capture-shared-payload')<=1
    assert protocol.state['claim_closed'] and not protocol.state['held']


def test_normal_owner_refuses_missing_full_sources_before_service_mutations(protocol):
    with pytest.raises(producer.FormalSnapshotError,match='LIFECYCLE_FULL_SOURCES_REQUIRED'):
        protocol.invoke(finite_tail=False)
    assert 'capture-shared-payload' not in protocol.state['calls']
    assert 'transition' not in protocol.state['calls']
    assert 'restore-shared-dependencies' not in protocol.state['calls']
    assert 'signed-open' not in protocol.state['calls'] and 'once' not in protocol.state['calls']
    assert not any('-open-' in e['step'] for e in protocol.state['claim'].events)
    assert not any(e['step'] in {'shared-backend-stopped','ingress-recovery-permitted',
        'shared-dependencies-ready'} for e in protocol.state['claim'].events)


def test_owner_budget_includes_approved_input_preparation(protocol,monkeypatch):
    read=producer.read_private
    def expire_during_preparation(path,limit):
        raw=read(path,limit)
        if Path(path)==protocol.cap_path:protocol.state['expired']=True
        return raw
    monkeypatch.setattr(producer,'read_private',expire_during_preparation)
    with pytest.raises(producer.FormalSnapshotError,match='LIFECYCLE_DEADLINE_EXCEEDED'):protocol.invoke()
    assert 'claim' not in protocol.state['calls']


def test_original_boot_instance_is_bound_before_capture(protocol):
    protocol.state['boot_instance']='other-instance'
    with pytest.raises(producer.FormalSnapshotError,match='LIFECYCLE_SOURCE_BINDING_CHANGED'):protocol.invoke()
    assert 'capture-shared-payload' not in protocol.state['calls']


def test_owner_accepts_existing_capture_preparation_not_readonly_observer_stage(protocol):
    protocol.state['capture_prepared']=True
    result=protocol.invoke()
    assert result['state']=='ORIGINAL_BACKEND_RUNTIME_RECORDED_PENDING_FULL_COLD_CONFIRMATION'


@pytest.mark.parametrize('fault',['unknown','db-intent','host-mask','host-stop','boot-close','live-close',
    'external-stop','capture-history','recovery-history','open-history','missing-fence'])
def test_owner_rejects_incomplete_or_wrong_pre_capture_history(protocol,fault):
    removals={'db-intent':'db-fenced','host-mask':'host-crond-masked','host-stop':'host-crond-closed',
        'boot-close':'boot-backend-closed','live-close':'live-backend-closed',
        'external-stop':'external-test-backend-stopped'}
    additions={'unknown':'unregistered-stage','capture-history':'split-sql-captured',
        'recovery-history':'ingress-recovery-permitted','open-history':'boot-backend-open-intent'}
    if fault in removals:protocol.state['remove_events']={removals[fault]}
    elif fault=='missing-fence':protocol.state['remove_events']={'db-fence-intent','db-fenced'}
    else:protocol.state['pre_events']=[dict(step=additions[fault],details={})]
    with pytest.raises(producer.FormalSnapshotError,match='LIFECYCLE_PRE_CAPTURE_STAGE_INVALID'):protocol.invoke()
    assert 'hold' not in protocol.state['calls'] and 'capture-shared-payload' not in protocol.state['calls']


@pytest.mark.parametrize('field',['capture','owner','target','approval','key','credential'])
def test_owner_requires_all_continuous_inputs_on_claim_control_device(protocol,monkeypatch,field):
    selected={'capture':protocol.cap_path,'owner':protocol.path,'target':protocol.target,
        'approval':protocol.approval,'key':Path(protocol.owner['openApproval']['trustedKeyPath']),
        'credential':Path(protocol.cap['mysqlCredentialFile'])}[field]
    native_stat=Path.stat
    def explicit_device_boundary(path,*args,**kwargs):
        info=native_stat(path,*args,**kwargs)
        if path==selected:
            changed={name:getattr(info,name) for name in dir(info) if name.startswith('st_')}
            changed['st_dev']=info.st_dev+456
            return types.SimpleNamespace(**changed)
        return info
    monkeypatch.setattr(Path,'stat',explicit_device_boundary)
    with pytest.raises(producer.FormalSnapshotError,match='LIFECYCLE_CONTROL_DEVICE_CHANGED'):protocol.invoke()
    assert 'hold' not in protocol.state['calls'] and 'capture-shared-payload' not in protocol.state['calls']


def test_owner_refuses_control_inputs_on_source_freeze_device(protocol):
    protocol.state['storage_device']=protocol.root.stat().st_dev
    with pytest.raises(producer.FormalSnapshotError,match='LIFECYCLE_CONTROL_DEVICE_CHANGED'):protocol.invoke()
    assert 'hold' not in protocol.state['calls'] and 'capture-shared-payload' not in protocol.state['calls']


@pytest.mark.parametrize('fault',['bound-only','close-pair-without-bound','bound-after-close'])
def test_owner_requires_complete_ordered_boot_close_chain(protocol,fault):
    if fault=='bound-only':protocol.state['remove_events']={'boot-backend-close-intent','boot-backend-closed'}
    elif fault=='close-pair-without-bound':
        protocol.state['pre_events']=[dict(step='boot-test-backend'+suffix,details={})
            for suffix in ('-close-intent','-closed')]
    else:protocol.state['move_events_to_end']=['boot-backend-bound']
    with pytest.raises(producer.FormalSnapshotError,match='LIFECYCLE_PRE_CAPTURE_STAGE_INVALID'):protocol.invoke()
    assert 'hold' not in protocol.state['calls'] and 'capture-shared-payload' not in protocol.state['calls']


def test_main_explicit_owner_route_does_not_accept_source_flag_or_observer(protocol,monkeypatch,capsys):
    raw=write(protocol.path,protocol.owner)
    calls=[]
    monkeypatch.setattr(producer,'run_approved_original_lifecycle_owner',
        lambda *args,**kwargs:calls.append((args,kwargs)) or dict(state='FINITE_ONLY'))
    context_raw=write(protocol.context_path,protocol.operation_input)
    args=['--request',str(protocol.path),'--expected-request-sha256',sha(raw),'--run-original-lifecycle-owner',
        '--deployment-context',str(protocol.context_path),
        '--expected-deployment-context-sha256',sha(context_raw)]
    assert producer.main(args)==0
    assert calls==[((str(protocol.path),sha(raw)),dict(deployment_context_path=str(protocol.context_path),
        expected_deployment_context_sha256=sha(context_raw)))]
    assert producer.main(args+['--observe-boot-state'])==1
    assert producer.main(args+['--approved-source-target',str(protocol.target)])==1
    assert len(calls)==1
    capsys.readouterr()


def test_owner_missing_derived_original_refuses_before_hold(protocol):
    protocol.state['derived_bound']=False
    with pytest.raises(producer.FormalSnapshotError,match='DERIVED_ORIGINALS_REQUIRED'):protocol.invoke()
    assert 'hold' not in protocol.state['calls'] and 'capture-shared-payload' not in protocol.state['calls']


def test_owner_consumer_drift_refuses_before_hold(protocol):
    protocol.state['derived_changed']=True
    with pytest.raises(producer.FormalSnapshotError,match='DERIVED_ORIGINAL_BINDING_CHANGED'):protocol.invoke()
    assert 'hold' not in protocol.state['calls'] and 'capture-shared-payload' not in protocol.state['calls']

def test_owner_original_image_failure_stops_before_recovery_and_retains_source_refusal(protocol):
    protocol.state['image_error']=True
    with pytest.raises(producer.FormalSnapshotError,match='ORIGINAL_IMAGE_CONTENT_CHANGED'):
        protocol.invoke()
    calls=protocol.state['calls']
    assert calls.count('original-images')==1 and 'transition' not in calls and 'signed-open' not in calls
    assert protocol.state['claim_closed'] and not protocol.state['held']

def test_owner_original_images_complete_before_recovery_under_same_claim(protocol):
    result=protocol.invoke();calls=protocol.state['calls']
    assert calls.index('capture-shared-payload')<calls.index('original-images')<calls.index('transition')
    raw=(protocol.root/'payload'/'050-original-images.json').read_bytes()
    receipt=producer._strict_json(raw)
    assert receipt['sha256']==sha((protocol.root/'payload'/'original-application-images.tar').read_bytes())
    assert receipt['productionEvidence'] is False and result['fullRunnerCompleted'] is False


def test_owner_original_configs_complete_before_service_effects_and_reread_at_end(protocol):
    result=protocol.invoke();calls=protocol.state['calls']
    assert calls.index('original-configs')<calls.index('capture-shared-payload')
    assert calls.index('config-readback')<calls.index('capture-shared-payload')
    assert calls.count('config-readback') >= 3
    assert calls.index('config-readback', calls.index('once')) < calls.index('close-fd')
    assert calls[-2:]==['close-fd','close-claim']
    assert result['originalConfigReceiptSha256']==sha((protocol.root/'original-configs/config-capture-stage.json').read_bytes())
    assert result['productionEvidence'] is False


@pytest.mark.parametrize('fault',['missing','wrong-sha','observation-drift','old-version','capture-failure','late-failure'])
def test_owner_original_config_failures_refuse_and_preserve_before_or_after(protocol,fault):
    if fault=='missing':protocol.owner.pop('originalConfigs')
    elif fault=='wrong-sha':protocol.owner['originalConfigs']['sha256']='1'*64
    elif fault=='observation-drift':protocol.config_observation.write_bytes(b'CHANGED_ORIGINAL_CONFIG_OBSERVATION')
    elif fault=='old-version':protocol.owner['schemaVersion']='release-formal-original-lifecycle-owner-input/v1'
    elif fault=='capture-failure':protocol.state['config_error']=True
    else:protocol.state['config_late_error']=True
    with pytest.raises(producer.FormalSnapshotError):protocol.invoke()
    if fault!='late-failure':assert 'capture-shared-payload' not in protocol.state['calls']
    else:assert (protocol.root/'original-configs/config-capture-stage.json').exists()


def test_owner_original_config_full_source_refusal_remains_before_any_copy(protocol):
    with pytest.raises(producer.FormalSnapshotError,match='FULL_SOURCES_REQUIRED'):protocol.invoke(finite_tail=False)
    assert 'original-configs' not in protocol.state['calls'] and 'capture-shared-payload' not in protocol.state['calls']


def test_owner_release_package_binding_is_retained_with_same_claim_without_completion(protocol):
    result=protocol.invoke()
    path=protocol.root/'original-release-binding.json'
    raw=path.read_bytes();binding=producer._strict_json(raw)
    assert binding['deploymentOperationSha256']==sha(protocol.release_path.read_bytes())
    assert binding['claimBindingSha256']==sha(producer.snapshot._canonical_bytes(protocol.cap['claimBinding']))
    assert binding['deploymentAttemptId']==protocol.release_operation['attemptId']
    assert binding['package']=={key:protocol.release_operation['binding'][key]
        for key in ('releaseTag','packageDigest','manifestDigest','planDigest')}
    assert binding['sourceFingerprint']==protocol.cap['claimBinding']['sourceFingerprint']
    assert binding['fullCaptureVerified'] is False and binding['productionEvidence'] is False
    assert result['releaseBindingSha256']==sha(raw) and result['releaseReady'] is False


@pytest.mark.parametrize('fault',['missing','wrong-sha','old-version','claim-operation','release-tag',
    'scope-environment','scope-host','scope-machine','scope-database','scope-uuid','scope-extra',
    'package','manifest','plan','lease','attempt','state','write-attempted','revision','phase',
    'operation-schema','binding-missing','receipt','completion'])
def test_owner_release_package_binding_refuses_invalid_original_before_any_effect(protocol,fault):
    op=protocol.release_operation
    if fault=='missing':protocol.operation_input.pop('operation')
    elif fault=='wrong-sha':protocol.operation_input['operation']['sha256']='1'*64
    elif fault=='old-version':protocol.owner['schemaVersion']='release-formal-original-lifecycle-owner-input/v2'
    else:
        if fault=='claim-operation':op['binding']['lockOperationId']='prod-other-package'
        elif fault=='release-tag':op['binding']['releaseTag']='other-package'
        elif fault.startswith('scope-'):
            key={'scope-environment':'environment','scope-host':'host','scope-machine':'machineId',
                'scope-database':'database','scope-uuid':'serverUuid','scope-extra':'extra'}[fault]
            op['binding']['scope'][key]='other'
        elif fault in ('package','manifest','plan','lease'):
            key={'package':'packageDigest','manifest':'manifestDigest','plan':'planDigest','lease':'leaseTokenDigest'}[fault]
            op['binding'][key]='not-a-sha'
        elif fault=='attempt':op['attemptId']='1'*63
        elif fault=='state':op['state']='COMMITTED'
        elif fault=='write-attempted':op['writeAttempted']=False
        elif fault=='revision':op['revision']=True
        elif fault=='phase':op['phase']='preparing'
        elif fault=='operation-schema':op['schemaVersion']='release-deployment-operation/v0'
        elif fault=='binding-missing':op['binding'].pop('packageDigest')
        elif fault=='receipt':op['receiptDigest']='2'*64
        elif fault=='completion':op['completion']={'lockApplied':True}
        raw=write(protocol.release_path,op)
        protocol.operation_input['operation']['sha256']=sha(raw)
    with pytest.raises(producer.FormalSnapshotError):protocol.invoke()
    assert 'claim' not in protocol.state['calls'] and 'hold' not in protocol.state['calls']
    assert not (protocol.root/'original-release-binding.json').exists()


@pytest.mark.parametrize('fault',['bytes','same-bytes-new-inode','bound-copy'])
def test_owner_release_package_binding_late_drift_stops_before_recovery(protocol,fault):
    path=protocol.root/'original-release-binding.json' if fault=='bound-copy' else protocol.release_path
    def drift():
        if fault=='same-bytes-new-inode':
            raw=path.read_bytes();path.rename(path.with_suffix('.old'));path.write_bytes(raw);path.chmod(0o600)
        else:path.write_bytes(b'CHANGED_RELEASE_ORIGINAL')
    protocol.state['drift']=drift
    with pytest.raises(producer.FormalSnapshotError):protocol.invoke()
    assert 'capture-shared-payload' in protocol.state['calls']
    assert 'original-images' not in protocol.state['calls'] and 'transition' not in protocol.state['calls']
    assert protocol.state['claim_closed'] and not protocol.state['held']


def test_owner_release_binding_keeps_unconditional_full_source_refusal(protocol):
    with pytest.raises(producer.FormalSnapshotError,match='FULL_SOURCES_REQUIRED'):
        protocol.invoke(finite_tail=False)
    assert not (protocol.root/'original-release-binding.json').exists()
    assert 'hold' not in protocol.state['calls'] and 'capture-shared-payload' not in protocol.state['calls']


def test_fixed_recipe_is_unchanged_when_current_operation_is_created_later(protocol):
    protocol.release_path.unlink()
    approved_raw=write(protocol.path,protocol.owner)
    approved_sha=sha(approved_raw)
    assert 'deploymentOperation' not in protocol.owner
    protocol.release_operation['attemptId']='3'*64
    operation_raw=write(protocol.release_path,protocol.release_operation)
    protocol.operation_input['operation']['sha256']=sha(operation_raw)
    protocol.operation_input['expectedAttemptId']='3'*64
    protocol.gate['attemptId']='3'*64;protocol.gate['operationDigest']=sha(operation_raw)
    protocol.operation_input['gate']['sha256']=sha(write(protocol.gate_path,protocol.gate))
    result=protocol.invoke()
    assert protocol.path.read_bytes()==approved_raw and sha(protocol.path.read_bytes())==approved_sha
    binding=producer._strict_json((protocol.root/'original-release-binding.json').read_bytes())
    assert binding['deploymentOperationSha256']==sha(operation_raw)
    assert binding['deploymentAttemptId']=='3'*64
    assert result['releaseReady'] is False and result['productionEvidence'] is False


@pytest.mark.parametrize('fault',['legacy-embedded','v3','absent-path','absent-sha','non-sha'])
def test_separate_current_operation_input_refuses_legacy_or_incomplete(protocol,fault):
    if fault=='legacy-embedded':protocol.owner['deploymentOperation']=dict(protocol.operation_input)
    elif fault=='v3':protocol.owner['schemaVersion']='release-formal-original-lifecycle-owner-input/v3'
    elif fault=='absent-path':protocol.operation_input['operation']['path']=None
    elif fault=='absent-sha':protocol.operation_input['operation']['sha256']=None
    else:protocol.operation_input['operation']['sha256']='not-a-sha'
    with pytest.raises(producer.FormalSnapshotError):protocol.invoke()
    assert not protocol.state['calls'] and not (protocol.root/'original-release-binding.json').exists()


@pytest.mark.parametrize('route',[[],['--observe-boot-state'],['--observe-original-launch'],['--bind-host-derived-originals']])
def test_current_operation_flags_are_rejected_before_generic_reads_on_non_owner_routes(protocol,monkeypatch,capsys,route):
    calls=[]
    monkeypatch.setattr(producer,'read_private',lambda *args:calls.append(args))
    monkeypatch.setattr(producer,'run_approved_host_derived_binding',lambda *args:calls.append(args))
    args=['--request',str(protocol.path),'--expected-request-sha256','a'*64,
        '--deployment-context',str(protocol.context_path),
        '--expected-deployment-context-sha256','b'*64,*route]
    assert producer.main(args)==1
    assert calls==[]
    capsys.readouterr()


@pytest.mark.parametrize('operation_flags',[[],['--deployment-context','/private/context.json'],
    ['--expected-deployment-context-sha256','a'*64]])
def test_owner_cli_missing_current_operation_pair_refuses_before_owner(protocol,monkeypatch,capsys,operation_flags):
    calls=[]
    monkeypatch.setattr(producer,'run_approved_original_lifecycle_owner',lambda *args,**kwargs:calls.append(args))
    args=['--request',str(protocol.path),'--expected-request-sha256','a'*64,'--run-original-lifecycle-owner',*operation_flags]
    assert producer.main(args)==1
    assert calls==[]
    capsys.readouterr()


@pytest.mark.parametrize('fault',['package','manifest','plan','tag','package-extra','attempt','lease',
    'gate-digest','gate-attempt','gate-revision','gate-state','gate-binding','gate-receipt','gate-extra',
    'context-schema','context-extra','context-no-gate','context-no-attempt','context-no-lease'])
def test_current_operation_flags_cross_binding_rejects_self_hashed_wrong_values(protocol,fault):
    if fault in ('package','manifest','plan','tag'):
        key={'package':'packageDigest','manifest':'manifestDigest','plan':'planDigest','tag':'releaseTag'}[fault]
        protocol.owner['releasePackage'][key]='different-release' if fault=='tag' else '9'*64
    elif fault=='package-extra':protocol.owner['releasePackage']['extra']='unapproved'
    elif fault=='attempt':protocol.operation_input['expectedAttemptId']='9'*64
    elif fault=='lease':protocol.operation_input['expectedLeaseTokenDigest']='9'*64
    elif fault.startswith('gate-'):
        key={'gate-digest':'operationDigest','gate-attempt':'attemptId','gate-revision':'revision',
            'gate-state':'state','gate-binding':'binding','gate-receipt':'receiptDigest','gate-extra':'extra'}[fault]
        value={'gate-revision':True,'gate-state':'COMMITTED','gate-binding':{},'gate-receipt':'1'*64}
        protocol.gate[key]=value.get(fault,'9'*64)
        protocol.operation_input['gate']['sha256']=sha(write(protocol.gate_path,protocol.gate))
    elif fault=='context-schema':protocol.operation_input['schemaVersion']='invalid'
    elif fault=='context-extra':protocol.operation_input['extra']=True
    else:protocol.operation_input.pop({'context-no-gate':'gate','context-no-attempt':'expectedAttemptId',
        'context-no-lease':'expectedLeaseTokenDigest'}[fault])
    with pytest.raises(producer.FormalSnapshotError):protocol.invoke()
    assert 'claim' not in protocol.state['calls'] and not (protocol.root/'original-release-binding.json').exists()


@pytest.mark.parametrize('item',['context','gate'])
def test_current_operation_flags_late_context_or_gate_drift_refuses_recovery(protocol,item):
    path=protocol.context_path if item=='context' else protocol.gate_path
    protocol.state['drift']=lambda:path.write_bytes(b'CHANGED_CURRENT_DEPLOYMENT_ORIGINAL')
    with pytest.raises(producer.FormalSnapshotError):protocol.invoke()
    assert 'capture-shared-payload' in protocol.state['calls']
    assert 'transition' not in protocol.state['calls'] and protocol.state['claim_closed']


@pytest.mark.parametrize('fault',['operation-bytes','operation-replace','binding-bytes','binding-replace'])
def test_owner_release_package_binding_final_observer_drift_refuses_return(protocol,monkeypatch,fault):
    configs=importlib.import_module('formal_selected_config_capture')
    original=configs.verify_selected_config_originals
    reads=0
    def final(*args,**kwargs):
        nonlocal reads
        result=original(*args,**kwargs);reads+=1
        if reads==3:
            path=protocol.release_path if fault.startswith('operation-') else protocol.root/'original-release-binding.json'
            if fault.endswith('replace'):
                raw=path.read_bytes();path.rename(path.with_suffix('.last-old'));path.write_bytes(raw);path.chmod(0o600)
            else:path.write_bytes(b'FINAL_CHANGED_BINDING')
        return result
    monkeypatch.setattr(configs,'verify_selected_config_originals',final)
    with pytest.raises(producer.FormalSnapshotError,match='INPUT_CHANGED'):protocol.invoke()
    assert reads==3 and 'once' in protocol.state['calls']
    assert protocol.state['claim_closed'] and not protocol.state['held']


@pytest.mark.parametrize('fault',['source-bytes','source-replace','copy-bytes','copy-replace',
    'receipt-bytes','config-mode','config-replace','copy-extra'])
def test_owner_release_package_binding_final_input_read_config_drift_refuses_return(protocol,monkeypatch,fault):
    configs=importlib.import_module('formal_selected_config_capture')
    original=configs.verify_selected_config_originals;reads=0;finished=False
    def verified(*args,**kwargs):
        nonlocal reads,finished
        result=original(*args,**kwargs);reads+=1;finished=reads==3
        return result
    monkeypatch.setattr(configs,'verify_selected_config_originals',verified)
    native=producer.read_private;changed=False
    if fault=='config-mode' and os.name=='nt':
        # Windows chmod does not implement POSIX directory permissions. This
        # owner test uses an explicit metadata boundary; the Linux file suite
        # separately changes and verifies the actual native mode.
        native_lstat=Path.lstat
        def lstat(path,*args,**kwargs):
            info=native_lstat(path,*args,**kwargs)
            if changed and path==protocol.config_root/'config':
                return types.SimpleNamespace(**{key:getattr(info,key) for key in
                    ('st_dev','st_ino','st_size','st_mtime_ns','st_ctime_ns')},st_mode=(info.st_mode & ~0o777)|0o755)
            return info
        monkeypatch.setattr(Path,'lstat',lstat)
    def read(path,*args,**kwargs):
        nonlocal changed
        raw=native(path,*args,**kwargs)
        if not changed and finished and Path(path)==protocol.path:
            changed=True
            source=protocol.config_root;target=protocol.root/'original-configs'
            if fault=='config-mode':(source/'config').chmod(0o755)
            elif fault=='config-replace':
                (source/'config').rename(source/'retained-config');(source/'config').mkdir(mode=0o700)
                for name in ('application-instance.yaml','data.env'):
                    (source/'retained-config'/name).rename(source/'config'/name)
            elif fault=='copy-extra':(target/'extra.bin').write_bytes(b'FINAL_EXTRA')
            else:
                selected=source/'.env' if fault.startswith('source') else target/'application-env.bin'
                if fault=='receipt-bytes':selected=target/'config-capture-stage.json'
                if fault.endswith('replace'):
                    old=selected.read_bytes();selected.rename(selected.with_suffix('.final-old'))
                    selected.write_bytes(old);selected.chmod(0o600)
                else:selected.write_bytes(b'FINAL_CONFIG_CHANGED')
        return raw
    monkeypatch.setattr(producer,'read_private',read)
    with pytest.raises(producer.FormalSnapshotError):protocol.invoke()
    assert changed and 'once' in protocol.state['calls']
    assert protocol.state['claim_closed'] and not protocol.state['held']
