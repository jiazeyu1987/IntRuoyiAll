"""Actual native signing/verification with explicit TEST binding fixtures; no effects."""
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
STOP = ['quiesce-selected-writers', 'set-selected-mysql-read-only', 'stop-selected-mysql',
        'stop-selected-minio', 'freeze-approved-selected-source']
RESTORE = dict(originalStateOnly=True, preserveAbsent=True, retainOnUnknownNewWrites=True,
               requireIndependentOriginalOpen=True)
CODE = 'RELEASE_SELECTED_SERVICE_APPROVAL_INVALID'
BINDING_FIELDS = ('schemaVersion', 'role', 'purpose', 'dispatchBinding', 'sourceBinding', 'policy')


def run(*args):
    return subprocess.check_output(['/usr/bin/openssl', *args], stderr=subprocess.DEVNULL)


@pytest.fixture(params=['stopAuthority', 'restoreAuthority'])
def selected(request):
    with tempfile.TemporaryDirectory(prefix='selected-approval-', dir='/control') as directory:
        root = Path(directory)
        private = root / 'private.pem'
        run('genpkey', '-algorithm', 'ED25519', '-out', str(private))
        private.chmod(0o600)
        key = root / 'public.der'
        key.write_bytes(run('pkey', '-in', str(private), '-pubout', '-outform', 'DER'))
        key.chmod(0o644)
        role = request.param
        dispatch = dict(workflowId='selected-workflow', operationId='selected-operation', releaseTag='selected-package',
                        packageDigest='1'*64, manifestDigest='2'*64, targetDigest='3'*64, deploymentBindingDigest='4'*64)
        scope = dict(environment='test', host='172.30.30.58', machineId='5'*32, database='selected_test',
                     serverUuid='11111111-1111-1111-1111-111111111111')
        source = dict(scope=scope, instanceIdentity=dict(taskFixture='explicit current identity, not actual admission'),
                      configSha256='6'*64, envSha256='7'*64,
                      originalsSha256={name: '8'*64 for name in
                          ('application-config', 'application-env', 'data-env', 'application-compose', 'data-compose')},
                      containers={name: dict(id='9'*64, imageId='sha256:'+'a'*64,
                          volume=dict(taskFixture='explicit current volume, not real resource approval')) for name in ('mysql', 'minio')},
                      applicationsSha256='b'*64)
        payload = dict(schemaVersion='release-selected-test-service-authority/v1', role=role,
                       purpose='CLOSE_SELECTED_SOURCE_FOR_CAPTURE' if role == 'stopAuthority' else 'RESTORE_SELECTED_SAVED_SOURCE_STATE',
                       dispatchBinding=dispatch, sourceBinding=source, grantId='c'*32, actorId=1001, tenantId=122,
                       issuedAt=1000, expiresAt=1600, policy=dict(actions=STOP) if role == 'stopAuthority' else RESTORE)
        approval = root / 'approval.json'
        def publish(value=payload, *, raw=None):
            message = root / 'message'
            message.write_bytes(raw if raw is not None else json.dumps(value, separators=(',', ':')).encode())
            signature = run('pkeyutl', '-sign', '-inkey', str(private), '-rawin', '-in', str(message))
            envelope = dict(schemaVersion='release-selected-test-service-approval/v1',
                            keySha256=hashlib.sha256(key.read_bytes()).hexdigest(),
                            payloadBase64=base64.b64encode(message.read_bytes()).decode(),
                            signatureBase64=base64.b64encode(signature).decode())
            approval.write_text(json.dumps(envelope), encoding='utf-8')
            approval.chmod(0o600)
            return envelope
        envelope = publish()
        yield dict(root=root, key=key, path=approval, expected=copy.deepcopy(payload), envelope=envelope, publish=publish)


def verifier():
    module = importlib.import_module('formal_open_authorization')
    fn = getattr(module, 'verify_selected_service_approval', None)
    assert callable(fn), 'native consumer must verify explicit selected service approval'
    return module, fn


def verify(selected, *, now=1200, guard=lambda: None, expected=None, trust=None):
    module, fn = verifier()
    return fn(str(selected['path']), str(selected['key']),
              selected['envelope']['keySha256'] if trust is None else trust,
              {key: selected['expected'][key] for key in BINDING_FIELDS} if expected is None else expected,
              now_seconds=now, guard=guard)


def test_real_two_purposes_verify_complete_current_binding_without_consumption(selected):
    before = {p.name: p.read_bytes() for p in selected['root'].iterdir()}
    calls = []
    value = verify(selected, guard=lambda: calls.append(True))
    assert value['state'] == 'SELECTED_SERVICE_APPROVAL_VERIFIED_PENDING_CONSUMPTION'
    assert value['authority'] == selected['expected']
    assert value['approvalSha256'] == hashlib.sha256(selected['path'].read_bytes()).hexdigest()
    assert value['keySha256'] == selected['envelope']['keySha256']
    assert value['approvalConsumed'] is False and value['serviceActionsPerformed'] is False and value['releaseReady'] is False
    assert len(calls) == 2 and before == {p.name: p.read_bytes() for p in selected['root'].iterdir()}


@pytest.mark.parametrize('fault', ['signature', 'wrong-public-key', 'key-hint', 'plain-policy', 'old-open-schema',
                                 'duplicate-envelope', 'duplicate-payload', 'second-json', 'nonfinite', 'padded-base64'])
def test_signature_and_independent_trust_are_mandatory(selected, fault):
    module, _ = verifier()
    path = selected['path']; value = copy.deepcopy(selected['envelope'])
    if fault == 'signature': value['signatureBase64'] = base64.b64encode(b'x'*64).decode()
    elif fault == 'wrong-public-key':
        other = selected['root'] / 'other.pem'
        run('genpkey', '-algorithm', 'ED25519', '-out', str(other))
        selected['key'].write_bytes(run('pkey', '-in', str(other), '-pubout', '-outform', 'DER'))
        value['keySha256'] = hashlib.sha256(selected['key'].read_bytes()).hexdigest()
        selected['envelope']['keySha256'] = value['keySha256']
    elif fault == 'key-hint': value['keySha256'] = 'd'*64
    elif fault == 'plain-policy': value = selected['expected']
    elif fault == 'old-open-schema': value['schemaVersion'] = 'formal-original-open-approval/v1'
    elif fault == 'duplicate-payload':
        raw = json.dumps(selected['expected']).replace('"actorId": 1001', '"actorId": 1001,"actorId":1001').encode()
        value = selected['publish'](raw=raw)
    elif fault == 'second-json': value = selected['publish'](raw=json.dumps(selected['expected']).encode()+b'{}')
    elif fault == 'nonfinite': value = selected['publish'](raw=json.dumps(selected['expected']).replace('"actorId": 1001', '"actorId": NaN').encode())
    elif fault == 'padded-base64': value['payloadBase64'] += '='
    raw = json.dumps(value).encode()
    if fault == 'duplicate-envelope': raw = b'{"schemaVersion":"PRIVATE_SENTINEL",'+raw[1:]
    path.write_bytes(raw)
    with pytest.raises(module.FormalSnapshotError, match='^'+CODE+'$'): verify(selected)


@pytest.mark.parametrize('fault', ['task', 'source', 'scope', 'role', 'purpose', 'policy', 'actor-bool',
                                 'grant', 'future', 'expired', 'ttl', 'extra'])
def test_signed_stale_or_broader_authority_is_refused(selected, fault):
    module, _ = verifier()
    value = copy.deepcopy(selected['expected'])
    if fault == 'task': value['dispatchBinding']['operationId'] = 'other'
    elif fault == 'source': value['sourceBinding']['containers']['minio']['volume']['taskFixture'] = 'changed'
    elif fault == 'scope': value['sourceBinding']['scope']['environment'] = 'prod'
    elif fault == 'role': value['role'] = 'restoreAuthority' if value['role']=='stopAuthority' else 'stopAuthority'
    elif fault == 'purpose': value['purpose'] = 'RESTORE_SAVED_ORIGINAL_OPEN'
    elif fault == 'policy': value['policy'] = dict(actions=['stop-all-host-services'])
    elif fault == 'actor-bool': value['actorId'] = True
    elif fault == 'grant': value['grantId'] = 'X'*32
    elif fault == 'future': value['issuedAt'] = 1201
    elif fault == 'expired': value['expiresAt'] = 1200
    elif fault == 'ttl': value['expiresAt'] = value['issuedAt'] + 3601
    elif fault == 'extra': value['approved'] = True
    selected['publish'](value)
    with pytest.raises(module.FormalSnapshotError, match='^'+CODE+'$'): verify(selected)


@pytest.mark.parametrize('fault', ['approval-mode', 'key-mode', 'approval-symlink', 'key-symlink', 'hardlink',
                                 'approval-drift', 'key-drift', 'guard', 'time-bool', 'wrong-pin', 'expected-typed-source'])
def test_private_custody_current_expected_and_guard_remain_required(selected, fault):
    module, _ = verifier()
    if fault == 'approval-mode': selected['path'].chmod(0o644)
    elif fault == 'key-mode': selected['key'].chmod(0o600)
    elif fault in ('approval-symlink', 'key-symlink'):
        target = selected['path'] if fault == 'approval-symlink' else selected['key']
        saved = selected['root'] / 'saved'; target.rename(saved); target.symlink_to(saved)
    elif fault == 'hardlink': os.link(selected['path'], selected['root'] / 'linked')
    if fault == 'expected-typed-source': selected['expected']['sourceBinding']['containers']['minio']['id'] = True
    calls = []
    def guard():
        calls.append(True)
        if len(calls) == 2:
            if fault == 'approval-drift': selected['path'].write_bytes(selected['path'].read_bytes()+b' ')
            if fault == 'key-drift': selected['key'].write_bytes(selected['key'].read_bytes()+b'x')
            if fault == 'guard': raise module.FormalSnapshotError('OWNED_GUARD_REFUSED')
    with pytest.raises(module.FormalSnapshotError, match='^('+CODE+'|OWNED_GUARD_REFUSED)$'):
        verify(selected, now=True if fault == 'time-bool' else 1200, guard=guard,
               trust='d'*64 if fault == 'wrong-pin' else None)


def test_last_guard_expiry_and_source_drift_refuse(selected, monkeypatch):
    module, _ = verifier()
    elapsed = [0.0]; monkeypatch.setattr(module.time, 'monotonic', lambda: elapsed[0])
    calls = []
    def guard():
        calls.append(True)
        if len(calls) == 2: elapsed[0] = 2.0
    with pytest.raises(module.FormalSnapshotError, match='^'+CODE+'$'): verify(selected, now=1599, guard=guard)
    elapsed[0] = 0.0; calls.clear()
    def source_guard():
        calls.append(True)
        if len(calls) == 2: selected['expected']['dispatchBinding']['operationId'] = 'changed-during-verification'
    with pytest.raises(module.FormalSnapshotError, match='^'+CODE+'$'): verify(selected, guard=source_guard)


def test_selected_validation_does_not_change_original_or_directory_atime(selected):
    paths = [selected['root'], selected['root'].parent, selected['key'], selected['path']]
    for path in paths:
        value = path.stat(); os.utime(path, ns=(1_000_000_000, value.st_mtime_ns))
    before = {str(path): path.stat().st_atime_ns for path in paths}
    verify(selected)
    assert before == {str(path): path.stat().st_atime_ns for path in paths}


def test_selected_payload_includes_server_metadata_above_request_body_limit(selected):
    # The issuer accepts a 16384-byte request, then adds schema/purpose/grant and
    # server actor/time. A valid issued payload can therefore exceed 16384 bytes.
    value = copy.deepcopy(selected['expected'])
    value['sourceBinding']['instanceIdentity']['explicitFixturePadding'] = ''
    size = len(json.dumps(value, separators=(',', ':')).encode())
    value['sourceBinding']['instanceIdentity']['explicitFixturePadding'] = 'x'*(16448-size)
    raw = json.dumps(value, separators=(',', ':')).encode()
    assert 16384 < len(raw) <= 17408
    request = {key: value[key] for key in ('role', 'dispatchBinding', 'sourceBinding', 'policy')}
    request['validitySeconds'] = 600
    assert len(json.dumps(request, separators=(',', ':')).encode()) <= 16384
    selected['expected'] = copy.deepcopy(value)
    selected['publish'](value)
    assert verify(selected)['authority'] == value


@pytest.mark.parametrize('fault', ['purpose', 'policy', 'scope', 'bool-container-id', 'bool-actor', 'extra'])
def test_even_matching_expected_cannot_authorize_broader_policy_or_typed_identity(selected, fault):
    module, _ = verifier()
    value = copy.deepcopy(selected['expected'])
    if fault == 'purpose': value['purpose'] = 'RESTORE_SAVED_ORIGINAL_OPEN'
    elif fault == 'policy': value['policy'] = dict(actions=['stop-all-host-services'])
    elif fault == 'scope': value['sourceBinding']['scope']['host'] = '172.30.30.5'
    elif fault == 'bool-container-id': value['sourceBinding']['containers']['mysql']['id'] = True
    elif fault == 'bool-actor': value['actorId'] = True
    else: value['approved'] = True
    selected['expected'] = copy.deepcopy(value)
    selected['publish'](value)
    with pytest.raises(module.FormalSnapshotError, match='^'+CODE+'$'): verify(selected)


@pytest.mark.parametrize('field', ['grantId', 'actorId', 'tenantId', 'issuedAt', 'expiresAt'])
def test_caller_metadata_is_not_an_independent_expected_source(selected, field):
    module, _ = verifier()
    binding = {key: selected['expected'][key] for key in BINDING_FIELDS}
    binding[field] = selected['expected'][field]
    with pytest.raises(module.FormalSnapshotError, match='^'+CODE+'$'):
        verify(selected, expected=binding)


def test_old_full_expected_is_refused_instead_of_compatibility_trust(selected):
    module, _ = verifier()
    with pytest.raises(module.FormalSnapshotError, match='^'+CODE+'$'):
        verify(selected, expected=selected['expected'])


def test_independent_current_binding_accepts_only_authenticated_issuer_metadata(selected):
    binding = copy.deepcopy({key: selected['expected'][key] for key in BINDING_FIELDS})
    assert not set(binding).intersection({'grantId', 'actorId', 'tenantId', 'issuedAt', 'expiresAt'})
    issued = copy.deepcopy(selected['expected'])
    issued.update(grantId='e'*32, actorId=777, tenantId=321, issuedAt=1100, expiresAt=1700)
    selected['publish'](issued)
    result = verify(selected, expected=binding)
    assert result['authority'] == issued
    assert result['approvalConsumed'] is False and result['serviceActionsPerformed'] is False


def test_forged_issuer_metadata_is_refused_even_with_exact_independent_binding(selected):
    module, _ = verifier()
    envelope = copy.deepcopy(selected['envelope'])
    issued = copy.deepcopy(selected['expected'])
    issued.update(grantId='e'*32, actorId=777, tenantId=321, issuedAt=1100, expiresAt=1700)
    envelope['payloadBase64'] = base64.b64encode(json.dumps(issued).encode()).decode()
    selected['path'].write_bytes(json.dumps(envelope).encode())
    with pytest.raises(module.FormalSnapshotError, match='^'+CODE+'$'):
        verify(selected)


def test_independent_expected_is_frozen_before_first_guard(selected):
    module, _ = verifier()
    binding = copy.deepcopy({key: selected['expected'][key] for key in BINDING_FIELDS})
    def guard():
        binding['sourceBinding']['containers']['minio']['volume']['taskFixture'] = 'rewritten current source'
    with pytest.raises(module.FormalSnapshotError, match='^'+CODE+'$'):
        verify(selected, expected=binding, guard=guard)


@pytest.mark.parametrize('signed_value', [True, 1.0])
def test_valid_nested_json_types_cannot_replace_independent_integer_identity(selected, signed_value):
    module, _ = verifier()
    selected['expected']['sourceBinding']['instanceIdentity']['explicitTypedFixture'] = 1
    value = copy.deepcopy(selected['expected'])
    value['sourceBinding']['instanceIdentity']['explicitTypedFixture'] = signed_value
    selected['publish'](value)
    with pytest.raises(module.FormalSnapshotError, match='^'+CODE+'$'):
        verify(selected)
