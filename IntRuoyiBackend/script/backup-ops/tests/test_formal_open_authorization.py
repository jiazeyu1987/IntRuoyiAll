"""Actual Ed25519/OpenSSL verification, task-only keys; no real server grant."""
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

import pytest

sys.path.insert(0, str(Path(__file__).parents[1] / 'linux'))


def run(*args):
    return subprocess.check_output(['/usr/bin/openssl', *args], stderr=subprocess.DEVNULL)


@pytest.fixture
def production_a(approval):
    root, key, path, _, _, _, publish = approval
    binding = dict(environment='prod', operationId='prod-owned-release', releaseTag='owned-release',
        attemptId='1'*64, leaseTokenDigest='2'*64, packageDigest='3'*64, manifestDigest='4'*64,
        planDigest='5'*64, approvedSourceTargetSha256='6'*64, sourceBindingSha256='7'*64,
        hostBootId=Path('/proc/sys/kernel/random/boot_id').read_text().strip())
    payload = dict(binding, purpose='RESTORE_PRODUCTION_SAVED_ORIGINAL_STATE', grantId='8'*32,
                   policy=dict(originalStateOnly=True, preserveStoppedAndAbsent=True, preserveUnfinishedRecords=True, noBootStateWrites=True),
                   actorId=17, tenantId=122, issuedAt=1000, expiresAt=1600)
    def signed(value=payload):
        envelope = publish(value)
        envelope['schemaVersion'] = 'formal-production-a-resume-approval/v1'
        path.write_text(json.dumps(envelope), encoding='ascii')
        return envelope
    envelope = signed()
    return root, key, path, binding, payload, envelope, signed


def verify_production_a(fixture, *, now=1200, guard=lambda: None):
    _, key, path, binding, _, envelope, _ = fixture
    module = importlib.import_module('formal_open_authorization')
    return module.verify_production_a_resume_approval(str(path), str(key), envelope['keySha256'],
        binding, now_seconds=now, guard=guard)


def test_production_a_distinct_purpose_and_readonly(production_a):
    root, _, path, binding, payload, _, _ = production_a
    before = {p.name: p.read_bytes() for p in root.iterdir()}
    result = verify_production_a(production_a)
    assert {k: result[k] for k in binding} == binding
    assert result['state'] == 'PRODUCTION_A_RESUME_VERIFIED_PENDING_CONSUMPTION'
    assert result['purpose'] == payload['purpose']
    assert result['approvalSha256'] == hashlib.sha256(path.read_bytes()).hexdigest()
    assert result['approvalConsumed'] is False and result['serviceActionsPerformed'] is False
    assert result['releaseReady'] is False
    assert before == {p.name: p.read_bytes() for p in root.iterdir()}


@pytest.mark.parametrize('case', ['old-open', 'close', 'test', 'operation', 'attempt', 'lease',
    'package', 'manifest', 'plan', 'target', 'source', 'boot', 'actor-bool', 'expired', 'ttl',
    'extra', 'missing', 'signature', 'schema', 'key', 'late-binding', 'policy-false', 'policy-int'])
def test_production_a_rejects_invalid_or_drifting_authority(production_a, case):
    _, _, path, binding, payload, envelope, publish = production_a
    value = copy.deepcopy(payload)
    changes = {'old-open': ('purpose', 'RESTORE_SAVED_ORIGINAL_OPEN'), 'close': ('purpose', 'CLOSE'),
        'test': ('environment', 'test'), 'operation': ('operationId', 'prod-other-release'),
        'attempt': ('attemptId', 'a'*64), 'lease': ('leaseTokenDigest', 'a'*64),
        'package': ('packageDigest', 'a'*64), 'manifest': ('manifestDigest', 'a'*64),
        'plan': ('planDigest', 'a'*64), 'target': ('approvedSourceTargetSha256', 'a'*64),
        'source': ('sourceBindingSha256', 'a'*64), 'boot': ('hostBootId', '00000000-0000-0000-0000-000000000000'),
        'actor-bool': ('actorId', True), 'expired': ('expiresAt', 1200), 'ttl': ('expiresAt', 4601)}
    if case in changes:
        field, changed = changes[case]; value[field] = changed; publish(value)
    elif case == 'extra': value['unbound'] = True; publish(value)
    elif case == 'missing': del value['sourceBindingSha256']; publish(value)
    elif case.startswith('policy-'):
        value['policy']['originalStateOnly'] = False if case == 'policy-false' else 1; publish(value)
    elif case in ('signature', 'schema', 'key'):
        field = {'signature': 'signatureBase64', 'schema': 'schemaVersion', 'key': 'keySha256'}[case]
        envelope[field] = {'signature': base64.b64encode(b'0'*64).decode(),
            'schema': 'formal-original-open-approval/v1', 'key': 'a'*64}[case]
        path.write_text(json.dumps(envelope), encoding='ascii')
    calls = []
    def guard():
        calls.append(True)
        if case == 'late-binding' and len(calls) == 2: binding['attemptId'] = 'b'*64
    with pytest.raises(importlib.import_module('formal_snapshot_linux').FormalSnapshotError):
        verify_production_a(production_a, guard=guard)


def test_production_a_original_open_verifier_refuses_new_envelope(production_a):
    module = importlib.import_module('formal_open_authorization')
    _, key, path, _, _, envelope, _ = production_a
    old = dict(operationId='owned-original-restore', role='backend',
        hostBootId='0fa6c22b-5a90-11f1-ade1-0242ac150002', containerId='a'*64,
        launchSha256='b'*64, artifactSha256='c'*64, originalOpenSha256='d'*64, closedSha256='e'*64)
    with pytest.raises(importlib.import_module('formal_snapshot_linux').FormalSnapshotError):
        module.verify_original_open_approval(str(path), str(key), envelope['keySha256'], old,
                                            now_seconds=1200, guard=lambda: None)


@pytest.mark.parametrize('case', ['approval', 'binding'])
def test_production_a_terminal_key_read_cannot_hide_prior_drift(production_a, monkeypatch, case):
    module = importlib.import_module('formal_open_authorization')
    _, key, path, binding, _, _, _ = production_a
    original = module._read_secure; calls = []
    def late_read(selected, *args, **kwargs):
        pair = original(selected, *args, **kwargs)
        if str(selected) == str(key):
            calls.append(True)
            if len(calls) == 2:
                if case == 'approval': path.write_bytes(path.read_bytes()+b' ')
                else: binding['attemptId'] = 'b'*64
        return pair
    monkeypatch.setattr(module, '_read_secure', late_read)
    with pytest.raises(importlib.import_module('formal_snapshot_linux').FormalSnapshotError):
        verify_production_a(production_a)


def test_production_a_intent_failure_reserves_grant_across_claims(production_a, monkeypatch):
    import time
    snapshot = importlib.import_module('formal_snapshot_linux')
    module = importlib.import_module('formal_open_authorization')
    root, key, path, binding, payload, envelope, publish = production_a
    now = int(time.time()); payload.update(issuedAt=now-2, expiresAt=now+600); publish(payload)
    registry = root/'registry'; registry.mkdir(mode=0o700)
    plan = dict(operationId=binding['operationId'], sourceFingerprint='9'*64,
        databaseUuid='0fa6c22b-5a90-11f1-ade1-0242ac150002', minioContainerId='a'*64)
    first = snapshot.FormalMaintenanceClaim(root/'first', plan)
    def failed_intent(*args): raise snapshot.FormalSnapshotError('OWNED_INTENT_WRITE_FAILED')
    monkeypatch.setattr(first, 'record', failed_intent)
    def invoke(claim):
        return module.consume_production_a_resume_approval(claim, str(registry), str(path), str(key),
            envelope['keySha256'], binding, guard=lambda: None)
    try:
        with pytest.raises(snapshot.FormalSnapshotError, match='OWNED_INTENT_WRITE_FAILED'): invoke(first)
        assert len(list(registry.iterdir())) == 1
        second = snapshot.FormalMaintenanceClaim(root/'second', plan)
        try:
            with pytest.raises(snapshot.FormalSnapshotError): invoke(second)
            assert second.events == []
        finally: second.close()
    finally: first.close()


@pytest.mark.parametrize('case', ['registry', 'binding', 'approval', 'journal', 'expiry', 'deadline'])
def test_production_a_terminal_marker_read_cannot_hide_prior_drift(production_a, monkeypatch, case):
    import time
    snapshot = importlib.import_module('formal_snapshot_linux')
    boot = importlib.import_module('formal_boot_state')
    module = importlib.import_module('formal_open_authorization')
    root, key, path, binding, payload, envelope, publish = production_a
    now = int(time.time()); payload.update(issuedAt=now-2, expiresAt=now+600); publish(payload)
    registry = root/'registry'; registry.mkdir(mode=0o700)
    plan = dict(operationId=binding['operationId'], sourceFingerprint='9'*64,
        databaseUuid='0fa6c22b-5a90-11f1-ade1-0242ac150002', minioContainerId='a'*64)
    original = boot._read_at; calls = []
    def late_marker(directory, name, *args, **kwargs):
        pair = original(directory, name, *args, **kwargs)
        if name.startswith('production-a-resume-grant-'):
            calls.append(True)
            if len(calls) == 2:
                if case == 'registry': registry.rename(root/'retained-registry'); registry.mkdir(mode=0o700)
                elif case == 'binding': binding['attemptId'] = 'b'*64
                elif case == 'approval': path.write_bytes(path.read_bytes()+b' ')
                elif case == 'journal': claim.record('shared-recovery-intent', {})
                elif case == 'expiry': monkeypatch.setattr(module.time, 'time', lambda: now+601)
                elif case == 'deadline': snapshot.observation_deadline.set(time.monotonic()-1)
        return pair
    monkeypatch.setattr(boot, '_read_at', late_marker)
    claim = snapshot.FormalMaintenanceClaim(root/'claim', plan)
    deadline_token = snapshot.observation_deadline.set(None)
    try:
        with pytest.raises(snapshot.FormalSnapshotError):
            module.consume_production_a_resume_approval(claim, str(registry), str(path), str(key),
                envelope['keySha256'], binding, guard=lambda: None)
    finally: claim.close(); snapshot.observation_deadline.reset(deadline_token)


def test_production_a_verifier_expiry_after_terminal_file_reads_is_refused(production_a, monkeypatch):
    module = importlib.import_module('formal_open_authorization')
    original = module._production_a_terminal_files
    real_clock = module.time.monotonic
    def late_files(*args):
        original(*args)
        expired_clock = real_clock()+500
        monkeypatch.setattr(module.time, 'monotonic', lambda: expired_clock)
    monkeypatch.setattr(module, '_production_a_terminal_files', late_files)
    with pytest.raises(importlib.import_module('formal_snapshot_linux').FormalSnapshotError):
        verify_production_a(production_a)


@pytest.mark.parametrize('case', ['success-replay', 'cross-claim', 'late-failure', 'expired', 'wrong-operation', 'late-journal'])
def test_production_a_consumption_is_persistent_nonreplayable_and_no_effect(production_a, case):
    import time
    snapshot = importlib.import_module('formal_snapshot_linux')
    module = importlib.import_module('formal_open_authorization')
    root, key, path, binding, payload, envelope, publish = production_a
    now = int(time.time()); payload.update(issuedAt=now-2, expiresAt=now+600)
    if case == 'expired': payload['expiresAt'] = now
    publish(payload)
    registry = root/'registry'; registry.mkdir(mode=0o700)
    plan = dict(operationId=binding['operationId'], sourceFingerprint='9'*64,
                databaseUuid='0fa6c22b-5a90-11f1-ade1-0242ac150002', minioContainerId='a'*64)
    if case == 'wrong-operation': plan['operationId'] = 'prod-other'
    def invoke(claim, guard=lambda: None):
        return module.consume_production_a_resume_approval(claim, str(registry), str(path), str(key),
            envelope['keySha256'], binding, guard=guard)
    claim = snapshot.FormalMaintenanceClaim(root/'claim', plan)
    try:
        if case in ('expired', 'wrong-operation'):
            with pytest.raises(snapshot.FormalSnapshotError): invoke(claim)
            assert list(registry.iterdir()) == [] and claim.events == []
            return
        if case == 'late-journal':
            calls = []
            def changing_guard():
                calls.append(True)
                if len(calls) == 2: claim.record('shared-recovery-intent', {})
            with pytest.raises(snapshot.FormalSnapshotError): invoke(claim, changing_guard)
            assert list(registry.iterdir()) == []
            assert [e['step'] for e in claim.events] == ['shared-recovery-intent']
            return
        def late_guard():
            if list(registry.iterdir()): raise snapshot.FormalSnapshotError('OWNED_LATE_FAILURE')
        if case == 'late-failure':
            with pytest.raises(snapshot.FormalSnapshotError, match='OWNED_LATE_FAILURE'):
                invoke(claim, late_guard)
            assert len(list(registry.iterdir())) == 1
        else:
            result = invoke(claim)
            assert result['approvalConsumed'] is True and result['serviceActionsPerformed'] is False
            assert result['releaseReady'] is False
            assert [e['step'] for e in claim.events] == ['production-a-resume-intent', 'production-a-resume-consumed']
        before = {p.name: p.read_bytes() for p in registry.iterdir()}
        with pytest.raises(snapshot.FormalSnapshotError): invoke(claim)
        assert before == {p.name: p.read_bytes() for p in registry.iterdir()}
        if case == 'cross-claim':
            other = snapshot.FormalMaintenanceClaim(root/'other-claim', plan)
            try:
                with pytest.raises(snapshot.FormalSnapshotError): invoke(other)
                assert other.events == []
            finally: other.close()
    finally: claim.close()


@pytest.fixture
def approval():
    with tempfile.TemporaryDirectory(prefix='open-', dir='/control') as directory:
        root = Path(directory)
        private = root / 'private.pem'
        run('genpkey', '-algorithm', 'ED25519', '-out', str(private))
        private.chmod(0o600)
        key = root / 'public.der'
        key.write_bytes(run('pkey', '-in', str(private), '-pubout', '-outform', 'DER'))
        key.chmod(0o644)
        binding = dict(operationId='owned-original-restore', role='backend',
            hostBootId='0fa6c22b-5a90-11f1-ade1-0242ac150002', containerId='a'*64,
            launchSha256='b'*64, artifactSha256='c'*64, originalOpenSha256='d'*64, closedSha256='e'*64)
        payload = dict(binding, purpose='RESTORE_SAVED_ORIGINAL_OPEN', grantId='f'*32,
            actorId=1, tenantId=1, issuedAt=1000, expiresAt=1600)
        path = root / 'approval.json'
        def publish(value=payload, *, raw=None):
            message = root / 'message'
            message.write_bytes(raw if raw is not None else json.dumps(value, separators=(',', ':')).encode())
            signature = run('pkeyutl', '-sign', '-inkey', str(private), '-rawin', '-in', str(message))
            envelope = dict(schemaVersion='formal-original-open-approval/v1',
                keySha256=hashlib.sha256(key.read_bytes()).hexdigest(),
                payloadBase64=base64.b64encode(message.read_bytes()).decode(),
                signatureBase64=base64.b64encode(signature).decode())
            path.write_text(json.dumps(envelope), encoding='ascii'); path.chmod(0o600)
            return envelope
        envelope = publish()
        yield root, key, path, binding, payload, envelope, publish


def verify(fixture, *, now=1200, guard=lambda: None):
    _, key, path, binding, _, envelope, _ = fixture
    module = importlib.import_module('formal_open_authorization')
    return module.verify_original_open_approval(str(path), str(key), envelope['keySha256'],
        binding, now_seconds=now, guard=guard)


def test_valid_approval_is_read_only_and_bound(approval):
    root, _, path, binding, payload, _, _ = approval
    before = {p.name: p.read_bytes() for p in root.iterdir()}
    calls = []
    result = verify(approval, guard=lambda: calls.append(True))
    assert result == dict(binding, state='ORIGINAL_OPEN_APPROVAL_VERIFIED_PENDING_CONSUMPTION',
        grantId=payload['grantId'], actorId=1, tenantId=1, issuedAt=1000, expiresAt=1600,
        approvalSha256=hashlib.sha256(path.read_bytes()).hexdigest(),
        keySha256=approval[5]['keySha256'], approvalConsumed=False, serviceActionsPerformed=False,
        productionEvidence=False, releaseReady=False)
    assert len(calls) >= 2
    assert before == {p.name: p.read_bytes() for p in root.iterdir()}


@pytest.mark.parametrize('case', ['purpose','actor-zero','actor-bool','tenant-zero','grant','operation',
    'role','host','container','launch','artifact','original','closed','issued-future','expired',
    'ttl-zero','ttl-long','extra','missing','duplicate','non-ascii','nonfinite','second-json'])
def test_signed_invalid_payload_is_refused(approval, case):
    _, _, _, binding, original, _, publish = approval
    value = copy.deepcopy(original); raw = None
    changes = {'purpose':('purpose','CLOSE'), 'actor-zero':('actorId',0), 'actor-bool':('actorId',True),
        'tenant-zero':('tenantId',0), 'grant':('grantId','G'*32), 'operation':('operationId','another'),
        'role':('role','frontend'), 'host':('hostBootId','11111111-1111-1111-1111-111111111111'),
        'container':('containerId','1'*64), 'launch':('launchSha256','1'*64),
        'artifact':('artifactSha256','1'*64), 'original':('originalOpenSha256','1'*64),
        'closed':('closedSha256','1'*64), 'issued-future':('issuedAt',1201),
        'expired':('expiresAt',1200), 'ttl-zero':('expiresAt',1000), 'ttl-long':('expiresAt',4601)}
    if case in changes:field, changed = changes[case]; value[field] = changed
    if case == 'extra':value['unused'] = True
    if case == 'missing':del value['closedSha256']
    if case == 'duplicate':raw = json.dumps(value).replace('"actorId": 1', '"actorId": 1, "actorId": 1').encode()
    if case == 'non-ascii':value['operationId'] = '\u4e2d'
    if case == 'nonfinite':raw = json.dumps(value).replace('"actorId": 1', '"actorId": NaN').encode()
    if case == 'second-json':raw = json.dumps(value).encode() + b'{}'
    publish(value, raw=raw)
    module = importlib.import_module('formal_open_authorization')
    with pytest.raises(module.FormalSnapshotError, match='^FORMAL_OPEN_PAYLOAD_INVALID$'):
        verify(approval)


@pytest.mark.parametrize('case', ['signature','payload','key-hint','signature-base64','payload-base64',
    'schema','extra','duplicate','approval-mode','key-mode','approval-symlink','key-symlink','hardlink'])
def test_forgery_or_untrusted_file_is_refused(approval, case):
    root, key, path, _, _, envelope, _ = approval
    value = copy.deepcopy(envelope)
    if case == 'signature':value['signatureBase64'] = base64.b64encode(b'x'*64).decode()
    if case == 'payload':value['payloadBase64'] = base64.b64encode(base64.b64decode(value['payloadBase64']).replace(b'1000', b'1001')).decode()
    if case == 'key-hint':value['keySha256'] = '1'*64
    if case == 'signature-base64':value['signatureBase64'] += '\n'
    if case == 'payload-base64':value['payloadBase64'] += '='
    if case == 'schema':value['schemaVersion'] = 'other'
    if case == 'extra':value['ignored'] = 'no'
    path.write_text(json.dumps(value), encoding='ascii')
    if case == 'duplicate':path.write_text(json.dumps(value).replace('"schemaVersion":', '"keySha256":"'+'a'*64+'","schemaVersion":'), encoding='ascii')
    if case == 'approval-mode':path.chmod(0o644)
    if case == 'key-mode':key.chmod(0o600)
    if case in ('approval-symlink','key-symlink'):
        target = path if case == 'approval-symlink' else key
        saved = root / 'saved'; target.rename(saved); target.symlink_to(saved)
    if case == 'hardlink':os.link(path, root/'linked')
    module = importlib.import_module('formal_open_authorization')
    with pytest.raises(module.FormalSnapshotError, match='^FORMAL_(OPEN|BOOT)_[A-Z_]+$'):
        verify(approval)


@pytest.mark.parametrize('case', ['key','approval','guard','expected','time-bool','time-float','wrong-trust'])
def test_drift_and_invalid_verifier_inputs_refuse(approval, case):
    _, key, path, binding, _, envelope, _ = approval
    module = importlib.import_module('formal_open_authorization')
    calls = []
    def guard():
        calls.append(True)
        if len(calls) == 2:
            if case == 'key':key.write_bytes(key.read_bytes()+b'x')
            if case == 'approval':path.write_bytes(path.read_bytes()+b' ')
            if case == 'guard':raise module.FormalSnapshotError('OWNED_GUARD_REFUSED')
    if case == 'expected':binding['extra'] = True
    now = True if case == 'time-bool' else 1200.0 if case == 'time-float' else 1200
    trust = '1'*64 if case == 'wrong-trust' else envelope['keySha256']
    with pytest.raises(module.FormalSnapshotError, match='^(FORMAL_(OPEN|BOOT)_[A-Z_]+|OWNED_GUARD_REFUSED)$'):
        module.verify_original_open_approval(str(path), str(key), trust, binding, now_seconds=now, guard=guard)


def test_wrong_actual_public_key_refused(approval):
    root, key, _, _, _, envelope, _ = approval
    other = root/'other.pem'; run('genpkey','-algorithm','ED25519','-out',str(other))
    key.write_bytes(run('pkey','-in',str(other),'-pubout','-outform','DER'))
    envelope['keySha256'] = hashlib.sha256(key.read_bytes()).hexdigest()
    value = json.loads(approval[2].read_bytes()); value['keySha256'] = envelope['keySha256']
    approval[2].write_text(json.dumps(value), encoding='ascii')
    module = importlib.import_module('formal_open_authorization')
    with pytest.raises(module.FormalSnapshotError, match='^FORMAL_OPEN_SIGNATURE_INVALID$'):
        verify(approval)


@pytest.mark.parametrize('elapsed_seconds', [2.0, 0.2])
def test_expiry_crossed_during_last_guard_is_refused(approval, monkeypatch, elapsed_seconds):
    module = importlib.import_module('formal_open_authorization')
    elapsed = [0.0]
    monkeypatch.setattr(module.time, 'monotonic', lambda: elapsed[0])
    calls = []
    def guard():
        calls.append(True)
        if len(calls) == 2:elapsed[0] = elapsed_seconds
    with pytest.raises(module.FormalSnapshotError, match='^FORMAL_OPEN_PAYLOAD_INVALID$'):
        verify(approval, now=1599, guard=guard)


def test_verification_preserves_file_and_ancestor_atime(approval):
    root, key, path, *_ = approval
    selected = [key, path, root, root.parent]
    for item in selected:
        info = item.stat(); os.utime(item, ns=(1_000_000_000, info.st_mtime_ns))
    before = {str(item):item.stat().st_atime_ns for item in selected}
    verify(approval)
    assert before == {str(item):item.stat().st_atime_ns for item in selected}
