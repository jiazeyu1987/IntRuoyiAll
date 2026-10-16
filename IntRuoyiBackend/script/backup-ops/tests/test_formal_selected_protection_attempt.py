"""Main-owned attempt boundary: real A037/private IO, finite resource fixtures.

No actual source admission, writer fence, service action or full protection.
"""
import copy
import hashlib
import importlib
import importlib.util
import json
import os
from pathlib import Path
import subprocess
import sys
import time

import pytest

sys.path.insert(0, str(Path(__file__).parents[1] / 'linux'))
import formal_snapshot_linux as snapshot
import formal_selected_release_entry as entry
import test_selected_service_consumption_native as consumed_fixture
from formal_snapshot_linux import FormalSnapshotError


@pytest.fixture
def case(monkeypatch):
    generator = consumed_fixture.case.__wrapped__()
    value = next(generator)
    try:
        fixture = value['fixture']
        source=fixture.root/'source';source.mkdir(mode=0o700);(source/'config').mkdir(mode=0o700)
        from formal_selected_config_capture import FILES
        originals={}
        for role,relative in FILES.items():
            path=source/relative;raw=('owned-original-'+role).encode();path.write_bytes(raw);path.chmod(0o600);info=path.stat()
            originals[role]=dict(path=str(path),bytes=len(raw),sha256=hashlib.sha256(raw).hexdigest(),
                fileSystemId=info.st_dev,inode=info.st_ino,mtimeNs=info.st_mtime_ns,ctimeNs=info.st_ctime_ns)
        for role,authority in fixture.payloads.items():
            authority['sourceBinding']['instanceIdentity']=dict(target=dict(root=str(source)))
            authority['sourceBinding']['originalsSha256']={name:item['sha256'] for name,item in originals.items()}
            fixture.publish(role)
        fields=('schemaVersion','role','purpose','dispatchBinding','sourceBinding','policy')
        fixture.expected={role:{key:copy.deepcopy(body[key]) for key in fields} for role,body in fixture.payloads.items()}
        fixture.capsule['expectedBindings']=copy.deepcopy(fixture.expected)
        verified=fixture.verify();value['admitted']['authorities']=verified['authorities']
        value['capsule']=copy.deepcopy(fixture.capsule);value['capsule']['attemptId']='a'*64
        private = fixture.root / 'private-inputs'; private.mkdir(mode=0o700)
        plan = copy.deepcopy(value['admitted']['resource']['plan'])
        plan['resources']['budget']['totalSeconds'] = 30
        plan['resources']['roles']['control'].update(parent=str(fixture.root),parentInode=fixture.root.stat().st_ino,
            path=str(fixture.root/'original-copy'))
        value['admitted']['resource']['plan'] = plan
        original_refs = {}
        for role in ('resources', 'recoveryScope', 'stopAuthority', 'restoreAuthority'):
            raw = (json.dumps(plan[role]).encode() if role in plan else Path(fixture.paths[role]).read_bytes())
            path = private / (role + '.json'); path.write_bytes(raw); path.chmod(0o600)
            original_refs[role] = dict(file=path.name, sha256=hashlib.sha256(raw).hexdigest())
        dispatch = value['capsule']['expectedBindings']['stopAuthority']['dispatchBinding']
        collection = dict(schemaVersion='release-selected-test-capture-sources/v1', environment='test',
            host='172.30.30.58', targetDigest=dispatch['targetDigest'],
            deploymentBindingDigest=dispatch['deploymentBindingDigest'], originals=original_refs)
        raw = json.dumps(collection).encode(); path = private / 'test.capture-sources.json'
        path.write_bytes(raw); path.chmod(0o600)
        collection_sha = hashlib.sha256(raw).hexdigest()
        key = private / 'test.service-authority-public.der'; key.write_bytes(fixture.key.read_bytes()); key.chmod(0o644)
        value['admitted']['resource']['collectionSha256'] = collection_sha
        observation=dict(originals=originals)
        source_probe="import json,hashlib;from pathlib import Path\nv="+repr(observation)+"\nfor item in v['originals'].values():\n p=Path(item['path']);s=p.stat();raw=p.read_bytes();assert item==dict(path=str(p),bytes=len(raw),sha256=hashlib.sha256(raw).hexdigest(),fileSystemId=s.st_dev,inode=s.st_ino,mtimeNs=s.st_mtime_ns,ctimeNs=s.st_ctime_ns)\nprint(json.dumps(v))"
        request = dict(privateRoot=str(private), collectionSha256=collection_sha,
            capsule=copy.deepcopy(value['capsule']), currentBindings=copy.deepcopy(fixture.expected),
            consumptionStorage=value['storage'], attemptId='a'*64, scopeDigest=fixture.capsule['scopeDigest'],
            expectedHost='172.30.30.58', expectedMachineId=value['owner']['machineId'], owner=value['owner'],
            mysqlCommand='printf LOCK_OWNED', sourceProbe=source_probe, sourceObservation=observation)
        calls = []
        def finite_admission(private_root, collection, capsule, bindings, attempt, scope, *, expected_host, expected_machine_id, guard):
            # Actual A037 executes each time. Only resource observation is a
            # named protocol fixture; it does not admit tmpfs on a real server.
            calls.append('REAL_A037'); guard()
            verified = entry.verify_selected_service_originals(capsule, bindings, attempt, scope,
                {role:str(private / (role+'.json')) for role in ('stopAuthority','restoreAuthority')}, str(key), guard=guard)
            result = copy.deepcopy(value['admitted']); result.update(authorities=verified['authorities'], scopeDigest=scope)
            guard(); return result
        monkeypatch.setattr(entry, 'admit_selected_entry_resources', finite_admission)
        value.update(request=request, calls=calls, private=private,source=source)
        yield value
    finally:
        generator.close()


def api():
    assert importlib.util.find_spec('formal_selected_protection_attempt') is not None, 'main-owned held selected attempt API required'
    return importlib.import_module('formal_selected_protection_attempt').hold_selected_protection_attempt


def held(case, guard=lambda:None):
    return api()(case['request'], installation_guard=guard)


def record(case):
    return case['root'] / ('selected-service-attempt-'+'a'*64+'.json')


def test_fresh_same_attempt_ack_and_real_competing_process_lock(case):
    with held(case) as attempt:
        assert case['calls'] == ['REAL_A037']
        attempt.assert_pre_effect(); attempt.assert_control_owned()
        assert attempt.acknowledgement['attemptId'] == case['request']['attemptId']
        assert attempt.acknowledgement['state'] == 'SELECTED_NATIVE_ENTRY_STARTED_PROTECTION_REQUIRED'
        assert attempt.remaining_seconds() <= 30
        code = 'import fcntl,sys;f=open(sys.argv[1],"rb");\ntry:fcntl.flock(f,fcntl.LOCK_EX|fcntl.LOCK_NB)\nexcept BlockingIOError:sys.exit(73)\nsys.exit(0)'
        child = subprocess.run([sys.executable,'-I','-B','-S','-c',code,str(case['root']/'selected-service.lock')],capture_output=True,timeout=5)
        assert child.returncode == 73, child.stderr
    code = 'import fcntl,sys;f=open(sys.argv[1],"rb");fcntl.flock(f,fcntl.LOCK_EX|fcntl.LOCK_NB)'
    assert subprocess.run([sys.executable,'-I','-B','-S','-c',code,str(case['root']/'selected-service.lock')],capture_output=True,timeout=5).returncode == 0


def test_reentry_calls_real_seam_and_never_resets_started_budget(case):
    with held(case) as first:
        before=first.remaining_seconds(); raw=record(case).read_bytes(); identity=record(case).stat()
    time.sleep(0.03)
    with held(case) as second:
        assert second.acknowledgement['resumed'] is True
        assert second.remaining_seconds() < before
        assert record(case).read_bytes() == raw and record(case).stat().st_mtime_ns == identity.st_mtime_ns
    assert case['calls'] == ['REAL_A037','REAL_A037']


@pytest.mark.parametrize('fault',['attempt','scope','intent-sha','started-sha','extra','success-flag','resumed-type'])
def test_fresh_ack_exact_shape_and_binding_required_before_body(case,monkeypatch,fault):
    original=entry.run_selected_entry_request
    def corrupt(request,*,installation_guard):
        answer=original(request,installation_guard=installation_guard)
        if fault=='attempt':answer['attemptId']='b'*64
        elif fault=='scope':answer['scopeDigest']='b'*64
        elif fault=='intent-sha':answer['intentSha256']='b'*64
        elif fault=='started-sha':answer['startedSha256']='b'*64
        elif fault=='extra':answer['inventedSuccess']=True
        elif fault=='success-flag':answer['releaseReady']=True
        else:answer['resumed']=1
        return answer
    monkeypatch.setattr(entry,'run_selected_entry_request',corrupt)
    with pytest.raises(FormalSnapshotError,match='SELECTED_PROTECTION_ACK_INVALID'):
        with held(case):pytest.fail('invalid ACK entered main body')
    assert json.loads(record(case).read_bytes())['state']=='STARTED'


@pytest.mark.parametrize('fault',['request','private-original','journal','journal-inode','lock-inode','grant-marker'])
def test_control_drift_refuses_and_retains_exact_changed_bytes(case,fault):
    with pytest.raises(FormalSnapshotError):
        with held(case) as attempt:
            if fault=='request':case['request']['owner']['executorIdentity']='changed'
            elif fault=='private-original':(case['private']/'resources.json').write_bytes(b'CHANGED_ORIGINAL')
            elif fault=='journal':record(case).write_bytes(b'PARTIAL_RETAINED')
            elif fault=='journal-inode':
                raw=record(case).read_bytes();record(case).unlink();record(case).write_bytes(raw);record(case).chmod(0o600)
            elif fault=='lock-inode':
                path=case['root']/'selected-service.lock';raw=path.read_bytes();path.unlink();path.write_bytes(raw);path.chmod(0o600)
            else:next(case['root'].glob('selected-service-grant-*.json')).write_bytes(b'CORRUPT_MARKER')
            before={p.name:p.read_bytes() for p in case['root'].iterdir()}
            with pytest.raises(FormalSnapshotError):attempt.assert_control_owned()
            assert before=={p.name:p.read_bytes() for p in case['root'].iterdir()}


def test_pre_effect_still_checks_real_source_sql_but_control_guard_does_not(case,monkeypatch):
    with held(case) as attempt:
        def stopped_source(*args,**kwargs):raise FormalSnapshotError('FIXTURE_STOPPED_SOURCE')
        monkeypatch.setattr(entry,'selected_entry_current_guard',stopped_source)
        attempt.assert_control_owned()
        with pytest.raises(FormalSnapshotError,match='^FIXTURE_STOPPED_SOURCE$'):attempt.assert_pre_effect()


def test_original_installation_error_propagates_from_control_guard(case):
    revoked=False
    def install():
        if revoked:raise FormalSnapshotError('FIXTURE_CODE_CHANGED')
    with pytest.raises(FormalSnapshotError,match='^FIXTURE_CODE_CHANGED$'):
        with held(case,install) as attempt:
            revoked=True
            with pytest.raises(FormalSnapshotError,match='^FIXTURE_CODE_CHANGED$'):attempt.assert_control_owned()


@pytest.mark.parametrize('offset',[-31,5])
def test_expired_or_future_durable_started_time_cannot_be_revived(case,offset):
    entry.run_selected_entry_request(case['request'],installation_guard=lambda:None)
    path=record(case); prior=path.read_bytes(); timestamp=time.time()+offset;os.utime(path,(timestamp,timestamp))
    with pytest.raises(FormalSnapshotError,match='SELECTED_PROTECTION_BUDGET_INVALID'):
        with held(case):pytest.fail('stale/future budget entered')
    assert path.read_bytes()==prior


def test_shorter_outer_budget_is_preserved_and_restored(case):
    outer=time.monotonic()+8;token=snapshot.observation_deadline.set(outer)
    try:
        with held(case) as attempt:
            assert snapshot.observation_deadline.get() == outer
            assert attempt.remaining_seconds() <= 8
        assert snapshot.observation_deadline.get() == outer
    finally:snapshot.observation_deadline.reset(token)


def test_control_guard_rejects_expired_outer_context(case):
    with held(case) as attempt:
        token=snapshot.observation_deadline.set(time.monotonic()-1)
        try:
            with pytest.raises(FormalSnapshotError,match='^FORMAL_DATA_DEADLINE$'):attempt.assert_control_owned()
        finally:snapshot.observation_deadline.reset(token)


def test_context_restored_and_lock_released_after_body_failure(case):
    before=snapshot.observation_deadline.get()
    with pytest.raises(FormalSnapshotError,match='^MAIN_BODY_FAILED$'):
        with held(case):raise FormalSnapshotError('MAIN_BODY_FAILED')
    assert snapshot.observation_deadline.get()==before
    with held(case) as attempt:assert attempt.acknowledgement['resumed'] is True


def test_body_failure_is_not_reclassified_as_control_parse_error(case):
    with pytest.raises(ValueError,match='^ORIGINAL_BODY_ERROR$'):
        with held(case):raise ValueError('ORIGINAL_BODY_ERROR')


def test_successful_exit_rechecks_control_before_return(case):
    with pytest.raises(FormalSnapshotError):
        with held(case):record(case).write_bytes(b'EXIT_CHANGED_RETAINED')
    assert record(case).read_bytes()==b'EXIT_CHANGED_RETAINED'


def test_control_guard_cannot_move_to_another_owner_thread(case):
    import threading
    with held(case) as attempt:
        failures=[]
        def run():
            try:attempt.assert_control_owned()
            except FormalSnapshotError as error:failures.append(error.code)
        thread=threading.Thread(target=run);thread.start();thread.join(timeout=5)
        assert not thread.is_alive() and failures==['SELECTED_PROTECTION_CONTROL_CHANGED']


def test_real_source_file_change_is_not_a_frozen_control_read(case):
    with held(case) as attempt:
        (case['source']/'.env').write_bytes(b'CHANGED_SOURCE')
        attempt.assert_control_owned()
        with pytest.raises(FormalSnapshotError,match='^RELEASE_SELECTED_CURRENT_SOURCE_CHANGED$'):attempt.assert_pre_effect()


def test_pre_effect_config_capture_uses_fresh_ack_and_bound_actual_bytes(case):
    module=importlib.import_module('formal_selected_protection_attempt') if importlib.util.find_spec('formal_selected_protection_attempt') else None
    function=getattr(module,'capture_selected_pre_effect_originals',None)
    assert callable(function),'main selected author config stage must consume held attempt'
    result=function(case['request'],installation_guard=lambda:None)
    assert result['state']=='SELECTED_CONFIG_ORIGINALS_CAPTURED_PENDING_FULL_PROTECTION'
    assert result['attemptId']==case['request']['attemptId'] and result['scopeDigest']==case['request']['scopeDigest']
    assert result['fullCaptureVerified'] is False and result['continuousHostExclusionVerified'] is False
    target=case['fixture'].root/'original-copy'
    for role,item in result['originals'].items():
        assert (target/item['copyPath']).read_bytes()==Path(item['source']['path']).read_bytes()
    assert json.loads(record(case).read_bytes())['state']=='STARTED'
    with pytest.raises(FormalSnapshotError):function(case['request'],installation_guard=lambda:None)
    assert all((target/item['copyPath']).is_file() for item in result['originals'].values())


def test_pre_effect_capture_revoked_ack_never_creates_copy(case,monkeypatch):
    module=importlib.import_module('formal_selected_protection_attempt') if importlib.util.find_spec('formal_selected_protection_attempt') else None
    function=getattr(module,'capture_selected_pre_effect_originals',None)
    assert callable(function),'main selected author config stage required'
    original=entry.run_selected_entry_request
    def revoke(request,*,installation_guard):
        answer=original(request,installation_guard=installation_guard);request['mysqlCommand']='printf LOCK_LOST';return answer
    monkeypatch.setattr(entry,'run_selected_entry_request',revoke)
    with pytest.raises(FormalSnapshotError):function(case['request'],installation_guard=lambda:None)
    assert not (case['fixture'].root/'original-copy').exists()


def test_fresh_ack_gap_cannot_replace_trusted_request_even_if_sql_still_answers_owned(case,monkeypatch):
    original=entry.run_selected_entry_request
    def changed(request,*,installation_guard):
        answer=original(request,installation_guard=installation_guard)
        request['mysqlCommand']='printf LOCK_OWNED '
        return answer
    monkeypatch.setattr(entry,'run_selected_entry_request',changed)
    with pytest.raises(FormalSnapshotError,match='^SELECTED_PROTECTION_CONTROL_CHANGED$'):
        with held(case):pytest.fail('request changed across ACK gap entered')


def test_post_ack_guard_revocation_precedes_config_directory_creation(case):
    import formal_selected_protection_attempt as module
    def installed():
        if record(case).exists():raise FormalSnapshotError('FIXTURE_AFTER_STARTED_REVOCATION')
    with pytest.raises(FormalSnapshotError,match='^FIXTURE_AFTER_STARTED_REVOCATION$'):
        module.capture_selected_pre_effect_originals(case['request'],installation_guard=installed)
    assert not (case['fixture'].root/'original-copy').exists()
