"""Actual owned files/child pipes; explicit native mount/watchdog protocol.

The child below never freezes anything. It only supplies exact native-stage
messages. The existing owned-ext4 suite separately exercises kernel freeze.
"""
import hashlib
import importlib
import json
import os
from pathlib import Path
import subprocess
import sys
import tempfile
import threading

import pytest

sys.path.insert(0, str(Path(__file__).parents[1] / 'linux'))


class OwnerLost(RuntimeError):
    pass


PROTOCOL = r'''
import hashlib,json,os,select,sys,time
from pathlib import Path
root=Path(sys.argv[1]);mode=sys.argv[2]
canonical=lambda value:(json.dumps(value,sort_keys=True,separators=(',',':'))+'\n').encode()
def save(name,value):
    fd=os.open(root/name,os.O_WRONLY|os.O_CREAT|os.O_EXCL,0o600)
    with os.fdopen(fd,'wb') as stream:stream.write(canonical(value));stream.flush();os.fsync(stream.fileno())
binding=json.loads(sys.stdin.readline());digest=hashlib.sha256(canonical(binding)).hexdigest()
save('000-ready.json',dict(binding=binding,bindingSha256=digest,deadlineBootSeconds=time.clock_gettime(time.CLOCK_BOOTTIME)+20))
save('005-freeze-acquired.json',dict(bindingSha256=digest))
if mode=='acquisition-wait':
    sys.stdin.read();save('010-invalidated.json',dict(status='CAPTURE_INVALIDATED'))
else:
    print('WATCHDOG_READY_NOT_FENCE_PROOF',flush=True)
    line=sys.stdin.readline() if mode!='cleanup-close-error' or select.select([sys.stdin],[],[],0.3)[0] else ''
    if line.startswith('CAPTURE_FINISH '):
        capture=json.loads((root/'physical-capture.json').read_bytes())
        if mode=='thaw-wait':
            save('019-finish-received.json',dict(status='PROTOCOL_FIXTURE_WAIT'))
            sys.stdin.read()
        else:
            save('010-copy-accepted.json',dict(captureSha256=line.strip().split()[1],bindingSha256=digest))
            save('020-thawed.json',dict(bindingSha256=digest,status='THAWED_COPY_ACCEPTED_PENDING_FORMAL_BARRIERS'))
            print('WATCHDOG_THAW_COPY_ACCEPTED_PENDING_FORMAL_BARRIERS',flush=True)
            sys.exit(0)
    save('010-invalidated.json',dict(status='CAPTURE_INVALIDATED'))
save('020-thawed.json',dict(bindingSha256=digest,status='THAWED_CAPTURE_REMAINS_INVALID'))
print('WATCHDOG_THAW_CAPTURE_INVALIDATED',flush=True)
'''


@pytest.mark.parametrize('value', [None, True, 'not-callable'])
def test_missing_native_owner_refuses_before_any_effect(tmp_path, monkeypatch, value):
    native = importlib.import_module('formal_filesystem_watchdog')
    started = []
    monkeypatch.setattr(native.subprocess, 'Popen', lambda *a, **k: started.append(a))
    with pytest.raises(native.WatchdogError, match='^WATCHDOG_OWNER_GUARD_REQUIRED$'):
        native.capture_physical_with_native_fence(tmp_path/'control', '/absent', {},
            '/absent/source', tmp_path/'output', 2, guard=value)
    assert not started and not (tmp_path/'control').exists() and not (tmp_path/'output').exists()


def test_revoked_native_owner_refuses_before_path_or_child(tmp_path, monkeypatch):
    native = importlib.import_module('formal_filesystem_watchdog')
    started = []
    monkeypatch.setattr(native.subprocess, 'Popen', lambda *a, **k: started.append(a))
    def guard(): raise OwnerLost('CURRENT_OWNER_REVOKED')
    with pytest.raises(OwnerLost, match='^CURRENT_OWNER_REVOKED$'):
        native.capture_physical_with_native_fence(tmp_path/'control', '/absent', {},
            '/absent/source', tmp_path/'output', 2, guard=guard)
    assert not started and not (tmp_path/'control').exists()


@pytest.mark.parametrize('mode', ['copy', 'before-finish', 'acquisition-wait', 'thaw-wait', 'cleanup-close-error', 'valid'])
def test_native_phase_guard_uses_owner_thread_and_retains_owned_evidence(tmp_path, monkeypatch, mode):
    native = importlib.import_module('formal_filesystem_watchdog')
    snapshot = importlib.import_module('formal_snapshot_linux')
    original_popen = subprocess.Popen
    original_copy = snapshot.copy_physical_minio_tree
    original_write = native.write_exclusive
    owner_thread = threading.get_ident()
    state = dict(revoke=False, children=[], checks=[], finished=False)
    root = tmp_path/'control';target = tmp_path/'captured'
    def start(command, **kwargs):
        assert command[1].endswith('formal_filesystem_watchdog.py')
        child = original_popen([sys.executable, '-u', '-c', PROTOCOL, str(root), mode], **kwargs)
        if mode=='cleanup-close-error':
            pipe=child.stdin
            class FailClose:
                def __getattr__(self,name):return getattr(pipe,name)
                def close(self):raise OSError('EXPLICIT_OWNED_CLOSE_FAILURE')
            child.stdin=FailClose()
            child.fixture_original_stdin=pipe
        state['children'].append(child)
        return child
    monkeypatch.setattr(native.subprocess, 'Popen', start)
    monkeypatch.setattr(native, 'observe_mount', lambda *a, **k: None)
    def copy(source, destination, *, lease_guard):
        if mode=='copy':
            calls=[]
            def during_copy():
                calls.append(1)
                if len(calls)==8:state['revoke']=True
                lease_guard()
            return original_copy(source, destination, lease_guard=during_copy)
        return original_copy(source, destination, lease_guard=lease_guard)
    monkeypatch.setattr(snapshot, 'copy_physical_minio_tree', copy)
    def write(directory, name, value):
        original_write(directory,name,value)
        if name=='physical-capture.json':
            state['finished']=True
            if mode in ('before-finish','cleanup-close-error'):state['revoke']=True
    monkeypatch.setattr(native,'write_exclusive',write)
    def guard():
        state['checks'].append(threading.get_ident())
        assert threading.get_ident()==owner_thread
        if (mode=='acquisition-wait' and (root/'005-freeze-acquired.json').exists() or
                mode=='thaw-wait' and (root/'019-finish-received.json').exists()):
            state['revoke']=True
        if state['revoke']:raise OwnerLost('CURRENT_OWNER_REVOKED')
    # /tmp and private /control are explicitly separate fixture tmpfs mounts.
    with tempfile.TemporaryDirectory(prefix='native-current-', dir='/tmp') as temporary:
        source=Path(temporary);(source/'xl.meta').write_bytes(b'EXACT_OWNED_BYTES')
        binding=dict(operationId='owned-native',bootId=Path('/proc/sys/kernel/random/boot_id').read_text().strip(),
            mount=dict(source='/dev/explicit-protocol',device='9:1',fstype='ext4',target=str(source)))
        assert source.stat().st_dev!=tmp_path.stat().st_dev
        try:
            if mode=='valid':
                result=native.capture_physical_with_native_fence(root,source,binding,source,target,2,guard=guard)
                assert result['state']=='RAW_CAPTURED_AND_THAWED_PENDING_FORMAL_BARRIERS'
                assert result['productionEvidence'] is False
                assert (target/'xl.meta').read_bytes()==b'EXACT_OWNED_BYTES'
                assert (root/'010-copy-accepted.json').is_file()
            else:
                with pytest.raises(OwnerLost,match='^CURRENT_OWNER_REVOKED$') as refused:
                    native.capture_physical_with_native_fence(root,source,binding,source,target,2,guard=guard)
                if mode=='cleanup-close-error':assert refused.value.ownerCleanupUncertain is True
                if state['children']:
                    assert (root/'010-invalidated.json').is_file()
                    assert not (root/'010-copy-accepted.json').exists()
                assert (source/'xl.meta').read_bytes()==b'EXACT_OWNED_BYTES'
                if mode=='copy':assert target.is_dir()
            assert state['checks'] and set(state['checks'])=={owner_thread}
            assert all(child.poll() is not None for child in state['children'])
        finally:
            # Fixture-only cleanup on assertion failure, never a native thaw policy.
            for child in state['children']:
                pipe=getattr(child,'fixture_original_stdin',None)
                if pipe is not None and not pipe.closed:pipe.close()
                if child.poll() is None:
                    if child.stdin is not None and not child.stdin.closed:child.stdin.close()
                    child.wait(timeout=4)


def test_shared_capture_missing_phase_safe_guard_has_no_first_effect(tmp_path, monkeypatch):
    producer=importlib.import_module('formal_capture_producer')
    calls=[]
    monkeypatch.setattr(producer,'validate_shared_request',lambda value:None)
    class Claim:
        def assert_owned(self):calls.append('claim')
    with pytest.raises(producer.FormalSnapshotError,match='^FORMAL_CAPTURE_NATIVE_OWNER_REQUIRED$'):
        producer.run_shared_request(Claim(),{'action':'capture-shared-payload','totalSeconds':60},lambda:calls.append('request'))
    assert not calls


def test_native_snapshot_wrapper_requires_and_transports_exact_owner(tmp_path, monkeypatch):
    snapshot=importlib.import_module('formal_snapshot_linux')
    calls=[]
    class Native:
        class WatchdogError(RuntimeError):pass
        def capture_physical_with_native_fence(self,*args,guard,**kwargs):
            assert guard is owner;guard();return dict(state='PROTOCOL_ONLY')
    owner=lambda:calls.append('owner')
    monkeypatch.setattr(importlib.util,'module_from_spec',lambda spec:Native())
    class Loader:
        def exec_module(self,module):pass
    monkeypatch.setattr(importlib.util,'spec_from_file_location',lambda *a:type('Spec',(),dict(loader=Loader()))())
    result=snapshot._capture_native_physical(tmp_path,'/owned',{},'/owned/source','/owned/target',2,guard=owner)
    assert result==dict(state='PROTOCOL_ONLY') and calls==['owner']


def test_native_direct_call_keeps_shorter_outer_budget_before_effect(tmp_path,monkeypatch):
    import time
    native=importlib.import_module('formal_filesystem_watchdog')
    snapshot=importlib.import_module('formal_snapshot_linux')
    started=[]
    monkeypatch.setattr(native.subprocess,'Popen',lambda *a,**k:started.append(a))
    outer=time.monotonic()+0.02
    token=snapshot.observation_deadline.set(outer)
    try:
        with pytest.raises(snapshot.FormalSnapshotError,match='^FORMAL_OBSERVATION_DEADLINE_EXCEEDED$'):
            native.capture_physical_with_native_fence(tmp_path/'control','/absent',{},
                '/absent/source',tmp_path/'output',2,guard=lambda:time.sleep(0.04),deadline=time.monotonic()+100)
        assert snapshot.observation_deadline.get()==outer
        assert not started and not (tmp_path/'control').exists()
    finally:snapshot.observation_deadline.reset(token)
