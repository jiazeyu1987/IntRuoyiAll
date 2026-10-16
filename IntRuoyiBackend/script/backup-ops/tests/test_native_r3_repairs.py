"""Offline byte/property protocol checks, never native authority or host acceptance.

Manager responses and private-file acquisition are explicit test boundaries.
No grant/lease is signed, no host command, credential or capture is invoked.
"""
import ast
from contextlib import contextmanager
import hashlib
import json
from pathlib import Path
import re
import sys
import time
from types import SimpleNamespace

import pytest

sys.path.insert(0, str(Path(__file__).parents[1] / 'linux'))
import formal_backup_lab as lab
import formal_normal_owner as owner
import formal_normal_owner_assembly as assembly
import formal_normal_owner_entry as entry
import formal_snapshot_linux as snapshot

PIN = dict(path='/private/fixture.json', bytes=1, sha256='a'*64)
W03 = Path('C:/Users/BJB110/.codex/worktrees/instance-snapshot-integration/IntRuoyiMaintance/ops/recovery')


def offline_host_module():
    # Compile actual native observation logic without importing Linux-only fcntl.
    # Each OS/manager boundary below is a declared protocol fixture, never a live proof.
    tree=ast.parse((Path(__file__).parents[1]/'linux/formal_host_admission.py').read_bytes())
    functions={'_manager_file_state','_closed_manager','_source','_step','_event'}
    constants={'PROPERTIES','CLOSE_ORDER','ASSOCIATED','UNIT_FILE_STATES'}
    nodes=[]
    for node in tree.body:
        if isinstance(node,ast.FunctionDef) and node.name in functions:nodes.append(node)
        elif isinstance(node,ast.Assign) and any(isinstance(t,ast.Name) and t.id in constants for t in node.targets):nodes.append(node)
        elif isinstance(node,ast.ClassDef) and node.name in ('NativeHostAdmission','ClosedHostAdmissionChecks'):
            wanted={'observe','_unit_file_state'} if node.name=='NativeHostAdmission' else {'_observe_entries'}
            nodes.append(ast.ClassDef(name=node.name,bases=[],keywords=[],decorator_list=[],
                body=[n for n in node.body if isinstance(n,ast.FunctionDef) and n.name in wanted]))
    boundary=SimpleNamespace()
    namespace=dict(boot=boundary,time=time,re=re,FormalSnapshotError=snapshot.FormalSnapshotError,
        _need=lambda value,code:snapshot.require(value,'FORMAL_HOST_'+code),
        _digest=lambda value:hashlib.sha256(snapshot._canonical_bytes(value)).hexdigest())
    exec(compile(ast.fix_missing_locations(ast.Module(body=nodes,type_ignores=[])), '<offline-actual-host-logic>', 'exec'),namespace)
    return SimpleNamespace(**namespace),boundary


host,boot=offline_host_module()


def w03_claim_bytes(value):
    # Execute only the independently implemented pure encoder, not its runtime.
    tree = ast.parse((W03 / 'repository_native_lab.py').read_bytes())
    function = next(n for n in tree.body if isinstance(n, ast.FunctionDef) and n.name == 'claim_plan_bytes')
    namespace = {'json': json}
    exec(compile(ast.Module(body=[function], type_ignores=[]), '<W03-pinned-claim-encoder>', 'exec'), namespace)
    return namespace['claim_plan_bytes'](value)


def templates():
    scope = dict(schemaVersion='backup-lab-source-scope/v1', environment='backup-lab',
        targetRawSha256='a'*64, targetFingerprint='b'*64, database='owned_probe',
        databaseUuid='11111111-1111-1111-1111-111111111111', labContextPin=PIN)
    claim = dict(operationId='offline-claim', sourceFingerprint=scope['targetFingerprint'],
        databaseUuid=scope['databaseUuid'], minioContainerId='c'*64)
    recipe = dict(schemaVersion='backup-lab-normal-owner-recipe-template/v1', action='serve-approved-backup-owner',
        totalSeconds=60, attribution=dict(taskId=entry.TASK_ID, moduleGroup='W02', ownerThreadId=entry.OWNER_THREAD_ID),
        executionContractPin=dict(PIN, sha256=entry.EXECUTION_CONTRACT_SHA256), creationResultsPin=PIN,
        claimRoot='/private/claim', claimBinding=claim, nativeAssemblyProgramPin=PIN, nativeAssemblyConfigPin=PIN,
        nativeInitializationProgramPin=PIN, nativeInitializationConfigPin=PIN)
    config = dict(schemaVersion='backup-lab-native-assembly-template/v1',
        binding={k:lab.LAB_ID if k=='releaseTag' else 'a'*64 for k in owner.BINDING_FIELDS},
        programPin=PIN, inputReferences={k:PIN for k in assembly.INPUT_NAMES}, controlRoot='/private/control',
        registryRoot='/private/registry', maintenanceMilliseconds=60000,
        **{k:PIN for k in assembly.DOCUMENT_NAMES})
    capture = dict(schemaVersion='backup-lab-capture-request-template/v1', action='capture-shared-payload',
        claimRoot=recipe['claimRoot'], claimBinding=claim, mysqlContainerId='d'*64,
        mysqlCredentialFile=PIN['path'], totalSeconds=60, sharedScopeSha256='e'*64,
        originalHealthSha256='f'*64, ingressRulesSha256='a'*64, database=scope['database'], sourceRoot='/private/data',
        dataLimits=dict(max_rows=1, max_nodes=1, max_bytes=4096, seconds=1),
        rejectionProbe=dict(table='owned_probe', column='id'),
        filesystemFence=dict(mount='/private/mount', seconds=1, binding=dict(operationId=claim['operationId'],
            bootId=scope['databaseUuid'], mount=dict(source='/dev/loop99', device='7:99', fstype='ext4', target='/private/mount'))),
        sourceScope={k:'backup-lab-source-scope-template/v1' if k=='schemaVersion' else v
            for k,v in scope.items() if k!='labContextPin'})
    return scope, recipe, config, capture


def test_actual_late_derivation_uses_only_native_claim_bytes(monkeypatch):
    scope, recipe, config, capture = templates()
    authority = object.__new__(lab.LabAuthority)
    authority.context_pin = PIN
    # A protocol boundary for the pure derivation test only, not an authority grant.
    monkeypatch.setattr(lab.LabAuthority, 'check', lambda self:None)
    result = lab.derive_owner_records(recipe, config, capture, authority=authority, source_scope=scope)
    raw = w03_claim_bytes(recipe['claimBinding'])
    assert raw == snapshot._canonical_bytes(recipe['claimBinding']) and raw.endswith(b'\n')
    assert result['originalBinding']['claimBindingSha256'] == hashlib.sha256(raw).hexdigest()
    assert result['originalBinding']['claimBindingSha256'] != hashlib.sha256(owner.canonical(recipe['claimBinding'])).hexdigest()
    assert 'labContextPin' not in recipe and 'labContextPin' not in config and 'labContextPin' not in capture
    assert w03_claim_bytes({'name':'测试'}) == b'{"name":"\\u6d4b\\u8bd5"}\n'


@pytest.mark.parametrize('wire_plan', [False, True])
def test_actual_assembly_claim_gate_accepts_native_and_rejects_wire(wire_plan):
    _, recipe, config, capture = templates()
    raw=owner.canonical(recipe['claimBinding']) if wire_plan else w03_claim_bytes(recipe['claimBinding'])
    def check():return assembly.validate_capture_claim(capture,claim_root=capture['claimRoot'],claim_plan=raw,
        credential_path=config['inputReferences']['mysql_credential']['path'])
    if wire_plan:
        with pytest.raises(snapshot.FormalSnapshotError,match='BINDING_INVALID'):check()
    else:check()
    assert b'validate_capture_claim(capture,claim_root=claim.root,claim_plan=claim._plan,' in Path(assembly.__file__).read_bytes()


@pytest.fixture
def native_protocol(monkeypatch):
    native = object.__new__(host.NativeHostAdmission)
    native.persistent = Path('/private/persistent')
    state = dict(property='enabled', method='enabled', load='loaded', fragment='/usr/lib/systemd/system/crond.service',
        active='active', sub='running', pid='11', mask=None)
    @contextmanager
    def directory(*args,**kwargs):yield (None, None)
    monkeypatch.setattr(boot, '_directory', directory, raising=False)
    native.mask_identity = lambda unit:state['mask']
    native._link_at = lambda directory,unit:None
    native._configuration = lambda paths:[dict(path=p,sha256='a'*64,signature=[]) for p in paths]
    native._configuration_signatures = lambda files:files
    native._links = lambda unit:'b'*64
    native._bus_property = lambda *args:b'a(sasbttttuii) 0\n'
    native._bus_run = lambda *args,**kwargs:('s "'+state['method']+'"\n').encode()
    def run(args,*unused):
        if args[0]!='show':return b''
        props = dict(Id='crond.service',Names='crond.service',LoadState=state['load'],ActiveState=state['active'],
            SubState=state['sub'],UnitFileState=state['property'],FragmentPath=state['fragment'],DropInPaths='',Job='0',
            MainPID=state['pid'],ControlPID='0',KillMode='process',KillSignal='15',SendSIGKILL='no',
            TimeoutStopUSec='infinity',PartOf='',BindsTo='',Conflicts='',PropagatesStopTo='')
        return ('\n'.join(k+'='+v for k,v in props.items())+'\n').encode()
    native._run = run
    original = native.observe('crond.service', time.monotonic()+5)
    return native, state, original


@pytest.mark.parametrize('property_state', ['bad', '', 'unknown', 'generated'])
@pytest.mark.parametrize('signatures_only', [False, True])
def test_original_never_bypasses_invalid_native_property(native_protocol, property_state, signatures_only):
    native, state, original = native_protocol
    state.update(property=property_state, method='masked-runtime', mask={'owned':True},
        load='masked', fragment='/dev/null', active='inactive', sub='dead', pid='0')
    with pytest.raises(snapshot.FormalSnapshotError, match='PROPERTY_UNSUPPORTED'):
        native.observe('crond.service', time.monotonic()+5, original, signatures_only=signatures_only)


def test_safe_pre_reload_stop_then_correct_closed_and_restore(native_protocol):
    native, state, original = native_protocol
    state.update(method='masked-runtime', mask={'owned':True})
    # Keep actual show property and safe stop contract from the original loaded object.
    before_stop = native.observe('crond.service', time.monotonic()+5, original)
    host._source(before_stop, original)
    assert before_stop['stopContractSha256'] == original['stopContractSha256']
    with pytest.raises(snapshot.FormalSnapshotError, match='CLOSED_ENTRY_CHANGED'):host._closed_manager(before_stop)
    state.update(active='inactive', sub='dead', pid='0')
    stopped = native.observe('crond.service', time.monotonic()+5, original)
    host._source(stopped, original)
    with pytest.raises(snapshot.FormalSnapshotError, match='CLOSED_ENTRY_CHANGED'):host._closed_manager(stopped)
    state.update(property='masked-runtime', load='masked', fragment='/dev/null')
    closed = native.observe('crond.service', time.monotonic()+5, original)
    host._source(closed, original);host._closed_manager(closed)
    state.update(property='enabled', method='enabled', load='loaded', fragment=original['loadedConfigurationPaths'][0], mask=None)
    restored_inactive = native.observe('crond.service', time.monotonic()+5, original)
    host._source(restored_inactive, original)
    state.update(active='active', sub='running', pid='11')
    assert native.observe('crond.service', time.monotonic()+5, original) == original


@pytest.mark.parametrize('property_state,method_state', [('enabled','masked-runtime'), ('masked-runtime','enabled')])
def test_post_reload_requires_both_property_and_method(native_protocol, property_state, method_state):
    native, state, original = native_protocol
    state.update(property=property_state, method=method_state, mask={'owned':True},
        load='masked', fragment='/dev/null', active='inactive', sub='dead', pid='0')
    with pytest.raises(snapshot.FormalSnapshotError, match='ORIGINAL_MANAGER_FILE_STATE_CHANGED'):
        native.observe('crond.service', time.monotonic()+5, original)


def test_restore_cannot_use_stale_manager_method_after_owned_mask_removed(native_protocol):
    native,state,original=native_protocol
    state.update(method='masked-runtime',mask=None,active='inactive',sub='dead',pid='0')
    with pytest.raises(snapshot.FormalSnapshotError,match='ORIGINAL_MANAGER_FILE_STATE_CHANGED'):
        native.observe('crond.service',time.monotonic()+5,original)


def test_closed_capture_calls_actual_native_property_check(native_protocol):
    native, state, original = native_protocol
    state.update(property='bad', method='masked-runtime', mask={'owned':True},
        load='masked', fragment='/dev/null', active='inactive', sub='dead', pid='0')
    claim = SimpleNamespace(events=[dict(step=host._step('crond.service','-closed'),details=dict(ownedMask=state['mask']))])
    guard = object.__new__(host.ClosedHostAdmissionChecks)
    guard.claim=claim;guard.native=native;guard.plan=dict(units={'crond.service':original})
    # The first real native read rejects bad before any later entry/peer work.
    for signatures_only in (False,True):
        with pytest.raises(snapshot.FormalSnapshotError, match='PROPERTY_UNSUPPORTED'):
            guard._observe_entries(time.monotonic()+5, signatures_only=signatures_only)
