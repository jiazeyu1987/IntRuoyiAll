"""Real persistent IO with real A037 signatures; resource/owner are protocol fixtures.

These tests never grant tmpfs real resource admission or execute source effects.
"""
import copy
import importlib
import importlib.util
import json
import os
from pathlib import Path
import sys
import time

import pytest

sys.path.insert(0,str(Path(__file__).parents[1]/'linux'))
from formal_snapshot_linux import FormalSnapshotError
import test_selected_release_entry_native as native_fixture


@pytest.fixture
def case():
    fixture=native_fixture.SelectedReleaseEntryNativeTest('test_real_both_signatures_accept_issuer_metadata_without_consumption')
    fixture.setUp()
    try:
        verified=fixture.verify()
        root=fixture.root/'consumption';root.mkdir(mode=0o700)
        storage=dict(root=str(root),fileSystemId=root.stat().st_dev,rootInode=root.stat().st_ino)
        # Only the admission result protocol is supplied here. Actual A037 and
        # private state file IO execute, independently of real server admission.
        admitted=dict(verified,state='SELECTED_NATIVE_ENTRY_POINT_IN_TIME_ADMITTED',attemptId='a'*64,
            resource=dict(state='SELECTED_RESOURCE_POINT_IN_TIME_ADMITTED',collectionSha256='c'*64,
                plan=dict(resources=dict(roles=dict(control=dict(fileSystemId=root.stat().st_dev)),budget=dict(totalSeconds=1),
                    issuedAt=int(time.time())-1,expiresAt=int(time.time())+300),
                    recoveryScope=dict(issuedAt=int(time.time())-1,expiresAt=int(time.time())+300))))
        capsule=copy.deepcopy(fixture.capsule);capsule['attemptId']='a'*64
        tag=verified['authorities']['stopAuthority']['dispatchBinding']['releaseTag']
        owner=dict(verified['authorities']['stopAuthority']['sourceBinding']['scope'],
            lockOperationId='test-'+tag,releaseTag=tag,leaseTokenDigest='d'*64,
            executorHost='fixture-host',executorPid='321',executorIdentity='fixture-id')
        yield dict(root=root,storage=storage,admitted=admitted,capsule=capsule,owner=owner,fixture=fixture)
    finally:fixture.doCleanups()


def consume(case,guard=lambda:None):
    assert importlib.util.find_spec('formal_selected_service_consumption') is not None, 'durable selected author seam required'
    module=importlib.import_module('formal_selected_service_consumption')
    return module.consume_selected_service_authorities(case['storage'],case['admitted'],case['capsule'],case['owner'],guard=guard)


def test_real_prepared_grants_and_started_readback_before_any_effect(case):
    events=[]
    result=consume(case,guard=lambda:events.append(tuple(sorted(p.name for p in case['root'].iterdir()))))
    assert result['state']=='SELECTED_NATIVE_ENTRY_STARTED_PROTECTION_REQUIRED'
    assert result['grantConsumed'] is True and result['startedPersisted'] is True
    assert result['sourceEffectsPerformed'] is False and result['releaseReady'] is False
    attempt=case['root']/('selected-service-attempt-'+'a'*64+'.json')
    stored=json.loads(attempt.read_bytes());assert stored['state']=='STARTED'
    assert stored['intent']['attemptId']=='a'*64
    grants=list(case['root'].glob('selected-service-grant-*.json'));assert len(grants)==2
    assert all(json.loads(p.read_bytes())['attemptId']=='a'*64 for p in grants)
    assert any(len(items)==2 for items in events), 'PREPARED must precede grant markers'
    assert all(p.stat().st_mode&0o777==0o600 for p in case['root'].iterdir())
    before={p.name:p.read_bytes() for p in case['root'].iterdir()}
    resumed=consume(case)
    assert resumed['resumed'] is True and resumed['attemptId']=='a'*64
    assert before=={p.name:p.read_bytes() for p in case['root'].iterdir()}


@pytest.mark.parametrize('fault',['attempt','binding','storage-inode','storage-filesystem','grant-same','authority-drift'])
def test_replay_binding_and_storage_drift_never_overwrite_original(case,fault):
    consume(case);before={p.name:p.read_bytes() for p in case['root'].iterdir()}
    if fault=='attempt':case['admitted']['attemptId']=case['capsule']['attemptId']='b'*64
    elif fault=='binding':case['owner']['leaseTokenDigest']='e'*64
    elif fault=='storage-inode':case['storage']['rootInode']+=1
    elif fault=='storage-filesystem':case['storage']['fileSystemId']+=1
    elif fault=='grant-same':case['admitted']['authorities']['restoreAuthority']['grantId']=case['admitted']['authorities']['stopAuthority']['grantId']
    else:case['admitted']['authorities']['stopAuthority']['sourceBinding']['configSha256']='f'*64
    with pytest.raises(FormalSnapshotError):consume(case)
    assert before=={p.name:p.read_bytes() for p in case['root'].iterdir()}


def test_guard_refusal_before_prepared_leaves_no_consumption(case):
    def guard():raise FormalSnapshotError('RELEASE_CAPTURE_LOCK_LOST')
    with pytest.raises(FormalSnapshotError,match='^RELEASE_CAPTURE_LOCK_LOST$'):consume(case,guard)
    assert list(case['root'].iterdir())==[]


def test_interrupted_prepared_resumes_only_same_exact_attempt(case):
    def guard():
        if list(case['root'].glob('selected-service-attempt-*.json')):
            raise FormalSnapshotError('OWNED_INTERRUPTION')
    with pytest.raises(FormalSnapshotError,match='^OWNED_INTERRUPTION$'):consume(case,guard)
    path=next(case['root'].glob('selected-service-attempt-*.json'))
    assert json.loads(path.read_bytes())['state']=='PREPARED'
    assert not list(case['root'].glob('selected-service-grant-*.json'))
    assert consume(case)['startedPersisted'] is True


def test_partial_or_orphan_record_remains_blocking(case):
    path=case['root']/('selected-service-grant-'+'a'*32+'.json')
    path.write_bytes(b'{"partial":');path.chmod(0o600)
    before={p.name:p.read_bytes() for p in case['root'].iterdir()}
    with pytest.raises(FormalSnapshotError):consume(case)
    assert before=={p.name:p.read_bytes() for p in case['root'].iterdir()}


def test_real_other_process_lock_refuses_concurrent_consumer(case):
    import fcntl
    import subprocess
    consume(case)
    path=case['root']/'selected-service.lock'
    process=subprocess.Popen([sys.executable,'-I','-B','-S','-c',
        'import fcntl,sys; f=open(sys.argv[1],"rb");fcntl.flock(f,fcntl.LOCK_EX);print("LOCKED",flush=True);sys.stdin.read()',str(path)],
        stdin=subprocess.PIPE,stdout=subprocess.PIPE,stderr=subprocess.PIPE)
    try:
        assert process.stdout.readline()==b'LOCKED\n'
        with pytest.raises(FormalSnapshotError,match='^RELEASE_SELECTED_CONSUMPTION_BUSY$'):consume(case)
    finally:
        process.stdin.close();assert process.wait(timeout=5)==0


def test_authority_drift_during_io_refuses_before_started_and_retains_prepared(case):
    def guard():
        if list(case['root'].glob('selected-service-attempt-*.json')):
            case['admitted']['authorities']['restoreAuthority']['expiresAt']=0
    with pytest.raises(FormalSnapshotError):consume(case,guard)
    path=next(case['root'].glob('selected-service-attempt-*.json'))
    assert json.loads(path.read_bytes())['state']=='PREPARED'
    assert not list(case['root'].glob('selected-service-grant-*.json'))


def test_real_child_crash_retains_prepared_and_exact_same_attempt_resumes(case):
    import subprocess
    payload={key:case[key] for key in ('storage','admitted','capsule','owner')}
    code='''import json,os,sys
sys.path.insert(0,sys.argv[1])
from formal_selected_service_consumption import consume_selected_service_authorities
from pathlib import Path
r=json.loads(sys.stdin.buffer.read());root=Path(r['storage']['root'])
def guard():
 if list(root.glob('selected-service-attempt-*.json')):os._exit(73)
consume_selected_service_authorities(**r,guard=guard)
'''
    result=subprocess.run([sys.executable,'-I','-B','-S','-c',code,str(Path(__file__).parents[1]/'linux')],
        input=json.dumps(payload).encode(),capture_output=True,timeout=10)
    assert result.returncode==73 and result.stdout==b'' and result.stderr==b''
    assert json.loads(next(case['root'].glob('selected-service-attempt-*.json')).read_bytes())['state']=='PREPARED'
    assert consume(case)['resumed'] is True


def test_actual_readback_corruption_is_retained_and_never_acknowledged(case):
    def guard():
        paths=list(case['root'].glob('selected-service-attempt-*.json'))
        if paths:paths[0].write_bytes(b'UNCONFIRMED_BYTES')
    with pytest.raises(FormalSnapshotError):consume(case,guard)
    path=next(case['root'].glob('selected-service-attempt-*.json'))
    assert path.read_bytes()==b'UNCONFIRMED_BYTES'
    before={p.name:p.read_bytes() for p in case['root'].iterdir()}
    with pytest.raises(FormalSnapshotError):consume(case)
    assert before=={p.name:p.read_bytes() for p in case['root'].iterdir()}


def test_real_signed_clock_expiry_while_prepared_keeps_effects_blocked(case):
    fixture=case['fixture'];expiry=int(time.time())+5
    fixture.payloads['restoreAuthority']['expiresAt']=expiry;fixture.publish('restoreAuthority')
    verified=fixture.verify()
    case['admitted']['authorities']=verified['authorities']
    case['capsule']=copy.deepcopy(fixture.capsule);case['capsule']['attemptId']='a'*64
    def guard():
        if list(case['root'].glob('selected-service-attempt-*.json')):
            time.sleep(max(0,expiry-time.time()+0.05))
    with pytest.raises(FormalSnapshotError):consume(case,guard)
    assert json.loads(next(case['root'].glob('selected-service-attempt-*.json')).read_bytes())['state']=='PREPARED'
    assert not list(case['root'].glob('selected-service-grant-*.json'))


def test_two_real_consumers_cannot_start_same_grants_in_different_attempts(case):
    import subprocess
    code='''import json,sys
sys.path.insert(0,sys.argv[1])
from formal_selected_service_consumption import consume_selected_service_authorities
from formal_snapshot_linux import FormalSnapshotError
r=json.loads(sys.stdin.buffer.read())
try:print(json.dumps(consume_selected_service_authorities(**r,guard=lambda:None)))
except FormalSnapshotError as e:print(e.code,file=sys.stderr);sys.exit(2)
'''
    processes=[]
    try:
        for attempt in ('a'*64,'b'*64):
            payload=copy.deepcopy({k:case[k] for k in ('storage','admitted','capsule','owner')})
            payload['admitted']['attemptId']=payload['capsule']['attemptId']=attempt
            child=subprocess.Popen([sys.executable,'-I','-B','-S','-c',code,str(Path(__file__).parents[1]/'linux')],
                stdin=subprocess.PIPE,stdout=subprocess.PIPE,stderr=subprocess.PIPE)
            child.stdin.write(json.dumps(payload).encode());child.stdin.close();child.stdin=None
            processes.append(child)
        answers=[child.communicate(timeout=10) for child in processes]
        assert sorted(child.returncode for child in processes)==[0,2],answers
        assert len(list(case['root'].glob('selected-service-attempt-*.json')))==1
        assert len(list(case['root'].glob('selected-service-grant-*.json')))==2
    finally:
        for child in processes:
            if child.poll() is None:child.kill();child.wait(timeout=5)
