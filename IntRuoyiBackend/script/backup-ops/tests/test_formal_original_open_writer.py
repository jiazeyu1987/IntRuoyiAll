"""Owned native filesystem and actual Ed25519; no real launch/health proof."""
import base64
import copy
import hashlib
import importlib
import json
import os
from pathlib import Path
import subprocess
import sys
import tempfile
import time

import pytest

sys.path.insert(0, str(Path(__file__).parents[1] / 'linux'))
import formal_boot_state as boot
import formal_snapshot_linux as snapshot


def sha(raw):
    return hashlib.sha256(raw).hexdigest()


def openssl(*args):
    return subprocess.check_output(['/usr/bin/openssl', *args], stderr=subprocess.DEVNULL)


@pytest.fixture
def owned():
    with tempfile.TemporaryDirectory(prefix='saved-open-', dir='/control') as directory:
        root = Path(directory)
        target_dir = root / 'boot'; target_dir.mkdir(mode=0o755)
        target = target_dir / 'state.json'
        value = dict(schemaVersion='formal-maintenance-boot/v1', instanceId='owned-backend',
            artifactSha256='a'*64, state='OPEN', epoch='', phase='NORMAL')
        original = (json.dumps(value, indent=2)+'\n').encode()
        target.write_bytes(original); target.chmod(0o644)
        binding = dict(operationId='owned-original-open', sourceFingerprint='b'*64,
            databaseUuid='00000000-0000-0000-0000-000000000001', minioContainerId='c'*64)
        claim_root = root / 'claim'
        claim = snapshot.FormalMaintenanceClaim(claim_root, binding)
        originals = {}
        for name in boot.ORIGINALS:
            raw = b'{"taskFixture":"not real source identity"}\n'
            claim._write(claim_root/name, raw); originals[name] = sha(raw)
        claim.record('maintenance-originals-bound', dict(originalFileSha256=originals)); claim.close()
        info = target.stat()
        closed = snapshot._canonical_bytes(dict(value, state='CLOSED', epoch=binding['operationId']))
        host = Path('/proc/sys/kernel/random/boot_id').read_text().strip()
        close_request = dict(schemaVersion='release-formal-capture-request/v6', action='close-durable-boot-state',
            claimRoot=str(claim_root), claimBinding=binding, totalSeconds=30, hostBootId=host,
            bootBinding=dict(role='backend', path=str(target), instanceId=value['instanceId'],
                artifactSha256='a'*64, originalOpenSha256=sha(original), closedSha256=sha(closed),
                epoch=binding['operationId'], expectedIdentity=dict(device=info.st_dev, inode=info.st_ino)))
        request = root / 'close.json'; request.write_bytes(snapshot._canonical_bytes(close_request)); request.chmod(0o600)
        boot.close_approved_boot_state(str(request), sha(request.read_bytes()))
        private = root / 'private.pem'; openssl('genpkey', '-algorithm', 'ED25519', '-out', str(private)); private.chmod(0o600)
        key = root / 'public.der'; key.write_bytes(openssl('pkey', '-in', str(private), '-pubout', '-outform', 'DER')); key.chmod(0o644)
        expected = dict(operationId=binding['operationId'], role='backend', hostBootId=host,
            containerId='d'*64, launchSha256='e'*64, artifactSha256='a'*64,
            originalOpenSha256=sha(original), closedSha256=sha(closed))
        now = int(time.time())
        payload = dict(expected, purpose='RESTORE_SAVED_ORIGINAL_OPEN', grantId='f'*32,
            actorId=1, tenantId=1, issuedAt=now-5, expiresAt=now+300)
        approval = root / 'approval.json'
        def publish(value=payload):
            message = root / 'message'; message.write_bytes(snapshot._canonical_bytes(value))
            signature = openssl('pkeyutl', '-sign', '-inkey', str(private), '-rawin', '-in', str(message))
            envelope = dict(schemaVersion='formal-original-open-approval/v1', keySha256=sha(key.read_bytes()),
                payloadBase64=base64.b64encode(message.read_bytes()).decode(), signatureBase64=base64.b64encode(signature).decode())
            approval.write_bytes(snapshot._canonical_bytes(envelope)); approval.chmod(0o600)
        publish()
        yield dict(root=root, target=target, claimRoot=claim_root, binding=binding, key=key, keySha=sha(key.read_bytes()),
            expected=expected, payload=payload, publish=publish, approval=approval, original=original, closed=closed)


def invoke(owned, *, guard=lambda: None, expected=None):
    writer = importlib.import_module('formal_original_open_writer')
    claim = snapshot.FormalMaintenanceClaim(owned['claimRoot'], owned['binding'], resume=True)
    try:
        return writer.restore_saved_original_open(claim, str(owned['approval']), str(owned['key']),
            owned['keySha'], owned['expected'] if expected is None else expected, live_guard=guard)
    finally:
        claim.close()


def events(owned):
    claim = snapshot.FormalMaintenanceClaim(owned['claimRoot'], owned['binding'], resume=True)
    try:
        return [event['step'] for event in claim.events]
    finally:
        claim.close()


def assert_no_open_write(owned):
    assert owned['target'].read_bytes() == owned['closed']
    assert not any('-open-' in step for step in events(owned))
    assert not (owned['claimRoot']/'boot-backend-open-approval-consumed.json').exists()


def test_exact_saved_bytes_persistent_consumption_and_no_service_actions(owned):
    before = owned['target'].stat().st_ino
    result = invoke(owned)
    assert owned['target'].read_bytes() == owned['original']
    assert owned['target'].stat().st_ino != before
    assert owned['target'].stat().st_mode & 0o777 == 0o644
    assert events(owned)[-3:] == ['boot-backend-open-intent', 'boot-backend-open-approval-consumed', 'boot-backend-open-restored']
    marker = owned['claimRoot']/'boot-backend-open-approval-consumed.json'
    receipt = json.loads(marker.read_bytes())
    assert receipt['approvalSha256'] == sha(owned['approval'].read_bytes())
    assert receipt['grantId'] == owned['payload']['grantId']
    assert marker.stat().st_mode & 0o777 == 0o600
    assert result['state'] == 'SAVED_ORIGINAL_OPEN_RESTORED_PENDING_COLD_START'
    assert result['approvalConsumed'] is True and result['durableOriginalOpenConfirmed'] is True
    for key in ('serviceActionsPerformed','claimReleased','productionEvidence','releaseReady','originalLaunchVerified'):
        assert result[key] is False


@pytest.mark.parametrize('field', ['operationId','role','hostBootId','containerId','launchSha256','artifactSha256','originalOpenSha256','closedSha256'])
def test_signed_binding_mismatch_refuses_before_intent(owned, field):
    expected = copy.deepcopy(owned['expected'])
    expected[field] = {'operationId':'other', 'role':'test-backend', 'hostBootId':'11111111-1111-1111-1111-111111111111'}.get(field, '1'*64)
    with pytest.raises(snapshot.FormalSnapshotError):
        invoke(owned, expected=expected)
    assert_no_open_write(owned)


@pytest.mark.parametrize('case', ['expired','signature','missing-guard','saved-bytes','saved-mode','closed-inode','target-mode','originals'])
def test_invalid_precondition_refuses_before_intent(owned, case):
    guard = lambda: None
    if case == 'expired':
        payload = dict(owned['payload'], expiresAt=int(time.time())-1, issuedAt=int(time.time())-100); owned['publish'](payload)
    if case == 'signature':
        value = json.loads(owned['approval'].read_bytes()); value['signatureBase64'] = base64.b64encode(b'x'*64).decode()
        owned['approval'].write_bytes(snapshot._canonical_bytes(value))
    if case == 'missing-guard': guard = None
    if case == 'saved-bytes': (owned['claimRoot']/'boot-backend-original-open.json').write_bytes(b'{}\n')
    if case == 'saved-mode': (owned['claimRoot']/'boot-backend-original-open.json').chmod(0o644)
    if case == 'closed-inode':
        replacement = owned['target'].parent/'replacement'; replacement.write_bytes(owned['closed']); replacement.chmod(0o644); replacement.replace(owned['target'])
    if case == 'target-mode': owned['target'].chmod(0o600)
    if case == 'originals': (owned['claimRoot']/'shared-health-original.json').write_bytes(b'{}\n')
    with pytest.raises(snapshot.FormalSnapshotError): invoke(owned, guard=guard)
    assert_no_open_write(owned)


def test_missing_close_completion_refuses(owned):
    latest = sorted(owned['claimRoot'].glob('event-*'))[-1]; latest.unlink()
    with pytest.raises(snapshot.FormalSnapshotError): invoke(owned)
    assert_no_open_write(owned)


def test_same_or_new_grant_cannot_replay_completed_action(owned):
    invoke(owned)
    before = {p.name:sha(p.read_bytes()) for p in owned['claimRoot'].iterdir() if p.is_file()}
    for new in (False, True):
        if new: owned['publish'](dict(owned['payload'], grantId='1'*32))
        with pytest.raises(snapshot.FormalSnapshotError, match='FORMAL_OPEN_TRANSITION_ALREADY_ATTEMPTED'): invoke(owned)
        assert before == {p.name:sha(p.read_bytes()) for p in owned['claimRoot'].iterdir() if p.is_file()}
    assert owned['target'].read_bytes() == owned['original']


@pytest.mark.parametrize('case', ['intent','consumed-file','pending'])
def test_partial_attempt_refuses_without_repair(owned, case):
    if case == 'intent':
        claim = snapshot.FormalMaintenanceClaim(owned['claimRoot'], owned['binding'], resume=True)
        try: claim.record('boot-backend-open-intent', dict(taskFixture='interruption'))
        finally: claim.close()
    if case == 'consumed-file':
        marker = owned['claimRoot']/'boot-backend-open-approval-consumed.json'; marker.write_bytes(b'{}\n'); marker.chmod(0o600)
    if case == 'pending':
        pending = owned['target'].parent/('.boot-open-'+sha(owned['approval'].read_bytes())+'.pending'); pending.write_bytes(owned['original']); pending.chmod(0o644)
    before = events(owned)
    with pytest.raises(snapshot.FormalSnapshotError, match='FORMAL_OPEN_TRANSITION_ALREADY_ATTEMPTED'): invoke(owned)
    assert events(owned) == before and owned['target'].read_bytes() == owned['closed']


def test_real_target_lock_excludes_second_writer(owned):
    lock = str(owned['target'])+'.writer.lock'
    code = "import fcntl,sys; f=open(sys.argv[1],'r+b'); fcntl.flock(f,fcntl.LOCK_EX); print('locked',flush=True); sys.stdin.buffer.read(1)"
    child = subprocess.Popen([sys.executable,'-B','-c',code,lock], stdin=subprocess.PIPE, stdout=subprocess.PIPE)
    try:
        assert child.stdout.readline() == b'locked\n'
        with pytest.raises(snapshot.FormalSnapshotError, match='FORMAL_BOOT_TARGET_BUSY'): invoke(owned)
        assert_no_open_write(owned)
    finally:
        child.communicate(b'x', timeout=5)
        assert child.returncode == 0


@pytest.mark.parametrize('stage', ['consume','replace','completion','after-completion'])
def test_io_and_final_drift_retain_facts_and_refuse_retry(owned, monkeypatch, stage):
    writer = importlib.import_module('formal_original_open_writer')
    original_record = snapshot.FormalMaintenanceClaim.record
    original_replace = os.replace
    if stage == 'consume':
        def record(claim, step, details):
            result = original_record(claim, step, details)
            if step == 'boot-backend-open-intent': raise snapshot.FormalSnapshotError('OWNED_INJECTED_FAILURE')
            return result
        monkeypatch.setattr(snapshot.FormalMaintenanceClaim, 'record', record)
    if stage == 'replace':
        def replace(*args, **kwargs):
            result = original_replace(*args, **kwargs)
            raise OSError('task injected failure after actual replace')
        monkeypatch.setattr(writer.os, 'replace', replace)
    if stage in ('completion','after-completion'):
        def record(claim, step, details):
            if step == 'boot-backend-open-restored' and stage == 'completion': raise OSError('task completion failure')
            result = original_record(claim, step, details)
            if step == 'boot-backend-open-restored': owned['target'].write_bytes(owned['closed'])
            return result
        monkeypatch.setattr(snapshot.FormalMaintenanceClaim, 'record', record)
    with pytest.raises(snapshot.FormalSnapshotError): invoke(owned)
    assert 'boot-backend-open-intent' in events(owned)
    if stage in ('replace','completion'): assert owned['target'].read_bytes() == owned['original']
    if stage == 'after-completion': assert 'boot-backend-open-restored' in events(owned)
    monkeypatch.undo()
    with pytest.raises(snapshot.FormalSnapshotError, match='FORMAL_OPEN_TRANSITION_ALREADY_ATTEMPTED'): invoke(owned)


def test_expiry_rechecked_after_live_guard_before_consumption(owned, monkeypatch):
    writer = importlib.import_module('formal_original_open_writer')
    value = dict(owned['payload'], expiresAt=int(time.time())+30); owned['publish'](value)
    actual = time.time()
    calls = []
    def guard():
        calls.append(True)
        if len(calls) >= 3: monkeypatch.setattr(writer.time, 'time', lambda:value['expiresAt']+1)
    with pytest.raises(snapshot.FormalSnapshotError): invoke(owned, guard=guard)
    assert owned['target'].read_bytes() == owned['closed']
    assert not (owned['claimRoot']/'boot-backend-open-approval-consumed.json').exists()


@pytest.mark.parametrize('stage', ['consumption','replace'])
def test_actual_child_exit_keeps_partial_state_and_refuses_retry(owned, stage):
    # A real process exits without Python finally/claim.close. Its OS locks are
    # released; the durable journal/marker, rather than a live lock, blocks retry.
    code = '''import json,os,sys
sys.path.insert(0,sys.argv[1])
import formal_snapshot_linux as s
import formal_original_open_writer as w
v=json.loads(sys.stdin.buffer.read())
c=s.FormalMaintenanceClaim(v['claimRoot'],v['binding'],resume=True)
if v['stage']=='consumption':
 old=s.FormalMaintenanceClaim.record
 def record(claim,step,details):
  result=old(claim,step,details)
  if step=='boot-backend-open-approval-consumed':os._exit(77)
  return result
 s.FormalMaintenanceClaim.record=record
else:
 old=os.replace
 def replace(*args,**kwargs):
  old(*args,**kwargs);os._exit(78)
 w.os.replace=replace
w.restore_saved_original_open(c,v['approval'],v['key'],v['keySha'],v['expected'],live_guard=lambda:None)
os._exit(79)
'''
    data = {key:str(owned[key]) if isinstance(owned[key],Path) else owned[key] for key in
            ('claimRoot','binding','approval','key','keySha','expected')}
    data['stage'] = stage
    child = subprocess.run([sys.executable,'-B','-c',code,str(Path(__file__).parents[1]/'linux')],
        input=json.dumps(data).encode(),capture_output=True,timeout=10)
    assert child.returncode == (77 if stage == 'consumption' else 78), child.stderr.decode()
    assert 'boot-backend-open-approval-consumed' in events(owned)
    assert 'boot-backend-open-restored' not in events(owned)
    assert owned['target'].read_bytes() == (owned['closed'] if stage == 'consumption' else owned['original'])
    with pytest.raises(snapshot.FormalSnapshotError,match='FORMAL_OPEN_TRANSITION_ALREADY_ATTEMPTED'):invoke(owned)


@pytest.mark.parametrize('point', ['consumption-file','consumption-directory','pending-file','published-directory'])
def test_actual_fsync_fault_preserves_attempt(owned, monkeypatch, point):
    writer = importlib.import_module('formal_original_open_writer')
    actual = os.fsync
    fired = []
    def sync(fd):
        path = os.readlink('/proc/self/fd/'+str(fd))
        marker = str(owned['claimRoot']/'boot-backend-open-approval-consumed.json')
        trigger = ((point == 'consumption-file' and path == marker) or
            (point == 'consumption-directory' and path == str(owned['claimRoot']) and os.path.exists(marker)) or
            (point == 'pending-file' and '/.boot-open-' in path) or
            (point == 'published-directory' and path == str(owned['target'].parent) and owned['target'].read_bytes() == owned['original']))
        if trigger and not fired:
            fired.append(True); raise OSError('task-only fsync injection')
        return actual(fd)
    monkeypatch.setattr(writer.os,'fsync',sync)
    with pytest.raises(snapshot.FormalSnapshotError):invoke(owned)
    assert fired and 'boot-backend-open-intent' in events(owned)
    assert 'boot-backend-open-restored' not in events(owned)
    if point == 'published-directory':assert owned['target'].read_bytes() == owned['original']
    monkeypatch.undo()
    with pytest.raises(snapshot.FormalSnapshotError,match='FORMAL_OPEN_TRANSITION_ALREADY_ATTEMPTED'):invoke(owned)


def test_clock_rollback_cannot_extend_verified_grant_lifetime(owned, monkeypatch):
    writer = importlib.import_module('formal_original_open_writer')
    wall = time.time(); monotonic = time.monotonic()
    payload = dict(owned['payload'], issuedAt=int(wall)-60, expiresAt=int(wall)+10); owned['publish'](payload)
    calls = []
    def guard():
        calls.append(True)
        # The first three guards are before/during offline verification. Only
        # after its successful return do elapsed time and wall time diverge.
        if len(calls) >= 4:
            monkeypatch.setattr(writer.time,'time',lambda:wall-1)
            monkeypatch.setattr(writer.time,'monotonic',lambda:monotonic+20)
    with pytest.raises(snapshot.FormalSnapshotError,match='FORMAL_OPEN_PAYLOAD_INVALID'):invoke(owned,guard=guard)
    assert_no_open_write(owned)


def test_same_grant_already_consumed_by_another_role_refuses(owned):
    marker = owned['claimRoot']/('original-open-grant-'+owned['payload']['grantId']+'.json')
    marker.write_bytes(b'{"taskFixture":"prior different role consumption"}\n'); marker.chmod(0o600)
    with pytest.raises(snapshot.FormalSnapshotError,match='FORMAL_OPEN_TRANSITION_ALREADY_ATTEMPTED'):invoke(owned)
    assert_no_open_write(owned)
