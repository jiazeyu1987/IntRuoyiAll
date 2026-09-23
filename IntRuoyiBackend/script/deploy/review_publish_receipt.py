"""Immutable review deployment receipt and fenced, replayable confirmation.

Executed on the target through the controlled PowerShell transport. A successful
deployment keeps its owner until the Java workflow's durable decision is ACKed.
"""
import base64
import contextlib
import fcntl
import hashlib
import json
import os
from pathlib import Path
import re
import subprocess
import sys
import tempfile
import urllib.request


def encode(value):
    return json.dumps(value, sort_keys=True, separators=(',', ':'), ensure_ascii=True).encode('utf-8')


def require(condition, code):
    if not condition:
        raise ValueError(code)


def read_json(path):
    require(path.is_file(), 'REVIEW_PUBLISH_RECEIPT_MISSING')
    content = path.read_bytes()
    require(len(content) <= 65536, 'REVIEW_PUBLISH_RECEIPT_TOO_LARGE')
    return json.loads(content), content


def sync_directory(path):
    descriptor = os.open(path, os.O_RDONLY | os.O_DIRECTORY)
    try:
        os.fsync(descriptor)
    finally:
        os.close(descriptor)


def write_durable(path, value):
    path.parent.mkdir(mode=0o700, parents=True, exist_ok=True)
    sync_directory(path.parent.parent)
    descriptor, temporary = tempfile.mkstemp(prefix='receipt-', dir=path.parent)
    try:
        with os.fdopen(descriptor, 'wb') as output:
            output.write(encode(value))
            output.flush()
            os.fsync(output.fileno())
        os.replace(temporary, path)
        sync_directory(path.parent)
    finally:
        if os.path.exists(temporary):
            os.unlink(temporary)


class NoRedirect(urllib.request.HTTPRedirectHandler):
    def redirect_request(self, *args, **kwargs):
        return None


def collect_runtime(binding):
    values = [line.split('=', 1)[1] for line in (Path(binding['runtimeDir']) / '.env').read_text(encoding='utf-8').splitlines()
              if line.startswith('IMAGE_TAG=')]
    require(len(values) == 1, 'REVIEW_PUBLISH_ENV_TAG_INVALID')
    images = [subprocess.check_output(['docker', 'inspect', '--format', '{{.Config.Image}}', name], encoding='utf-8', timeout=10).strip()
              for name in ['intruoyi-backend', 'intruoyi-frontend']]
    opener = urllib.request.build_opener(NoRedirect())
    with opener.open('http://127.0.0.1:48081/actuator/health', timeout=10) as response:
        require(response.status == 200, 'REVIEW_PUBLISH_HEALTH_HTTP_INVALID')
        health = json.loads(response.read(1048576))
    with opener.open('http://127.0.0.1:8081/', timeout=10) as response:
        frontend_http = response.status
    return dict(imageTag=values[0], backendImage=images[0], frontendImage=images[1],
                healthStatus=health.get('status') if isinstance(health, dict) else None, frontendHttp=frontend_http)


def verify_runtime(binding, runtime):
    tag = binding['releaseTag']
    require(runtime == dict(imageTag=tag, backendImage='intruoyi-backend:' + tag,
                            frontendImage='intruoyi-frontend:' + tag, healthStatus='UP', frontendHttp=200),
            'REVIEW_PUBLISH_RUNTIME_NOT_ACCEPTED')


def run(config, runtime_reader=collect_runtime, emit=True):
    action = config['action']
    require(action in ('prepare', 'inspect', 'ack'), 'REVIEW_PUBLISH_ACTION_INVALID')
    expected = dict(config['binding'])
    required = {'workflowId', 'operationId', 'releaseTag', 'packageDigest', 'manifestDigest',
                'targetEnvironment', 'targetHost', 'runtimeDir'}
    require(set(expected) in (required, required | {'leaseToken'}), 'REVIEW_PUBLISH_BINDING_INVALID')
    require(expected['targetEnvironment'] == 'backup', 'REVIEW_PUBLISH_TARGET_INVALID')
    require(re.fullmatch(r'(?:op-)?[a-z0-9-]{8,64}', expected['operationId']), 'REVIEW_PUBLISH_OPERATION_INVALID')
    for field in ('packageDigest', 'manifestDigest'):
        require(re.fullmatch(r'[0-9a-f]{64}', expected[field]), 'REVIEW_PUBLISH_DIGEST_INVALID')
    if action != 'inspect' or 'leaseToken' in expected:
        require(re.fullmatch(r'[0-9a-f]{32}', expected.get('leaseToken', '')), 'REVIEW_PUBLISH_OWNER_REQUIRED')
    root = Path(expected['runtimeDir'])
    require(root.is_dir() and root.is_absolute(), 'REVIEW_PUBLISH_RUNTIME_DIR_MISSING')
    prefix = root / '.runtime-control-resource'
    owner = Path(str(prefix) + '.owner')
    directory = Path(str(prefix) + '.receipts')
    receipt_path = directory / (expected['operationId'] + '.json')
    confirmed_path = directory / (expected['operationId'] + '.confirmed.json')
    historical_replay = False
    if action in ('inspect', 'ack') and receipt_path.is_file() and confirmed_path.is_file() and owner.is_file():
        preliminary, _ = read_json(receipt_path)
        current_owner = owner.read_text()
        historical_replay = bool(re.fullmatch(r'[0-9a-f]{32}', current_owner)
                                 and current_owner != preliminary.get('binding', {}).get('leaseToken'))
    # A confirmed historical result has no authority over a different live owner.
    # Read it without blocking that owner's writes; this branch must never mutate or remove an owner.
    context = contextlib.ExitStack() if historical_replay else Path(str(prefix) + '.guard').open('a+b')
    with context as guard:
        if not historical_replay:
            fcntl.flock(guard, fcntl.LOCK_EX | fcntl.LOCK_NB)
        if action == 'inspect' and not receipt_path.exists():
            require(not confirmed_path.exists(), 'REVIEW_PUBLISH_CONFIRMATION_WITHOUT_RECEIPT')
            if emit:
                print('REVIEW_PUBLISH_RECEIPT_MISSING=1')
            return None
        if action == 'prepare' and not receipt_path.exists():
            require(owner.is_file() and owner.read_text() == expected['leaseToken'], 'REVIEW_PUBLISH_OWNER_MISMATCH')
            runtime = runtime_reader(expected)
            verify_runtime(expected, runtime)
            write_durable(receipt_path, dict(schemaVersion=1, binding=expected, runtime=runtime))
        receipt, raw = read_json(receipt_path)
        require(set(receipt) == {'schemaVersion', 'binding', 'runtime'} and receipt['schemaVersion'] == 1,
                'REVIEW_PUBLISH_RECEIPT_SCHEMA_INVALID')
        binding = receipt['binding']
        require(set(binding) == required | {'leaseToken'} and all(binding.get(key) == value for key, value in expected.items()),
                'REVIEW_PUBLISH_RECEIPT_BINDING_MISMATCH')
        require(re.fullmatch(r'[0-9a-f]{32}', binding['leaseToken']), 'REVIEW_PUBLISH_RECEIPT_OWNER_INVALID')
        verify_runtime(binding, receipt['runtime'])
        digest = hashlib.sha256(raw).hexdigest()
        decision = None
        if confirmed_path.exists():
            confirmation, _ = read_json(confirmed_path)
            require(confirmation.get('receiptDigest') == digest and confirmation.get('operationId') == binding['operationId'],
                    'REVIEW_PUBLISH_CONFIRMATION_MISMATCH')
            decision = confirmation.get('confirmationDecisionDigest')
            require(isinstance(decision, str) and re.fullmatch(r'[0-9a-f]{64}', decision), 'REVIEW_PUBLISH_DECISION_INVALID')
            require(action != 'prepare', 'REVIEW_PUBLISH_ALREADY_CONFIRMED')
        if historical_replay:
            require(decision is not None, 'REVIEW_PUBLISH_CONFIRMATION_REPLAY_CHANGED')
        if action == 'ack':
            require(config.get('expectedReceiptDigest') == digest, 'REVIEW_PUBLISH_EXPECTED_RECEIPT_MISMATCH')
            requested_decision = config.get('confirmationDecisionDigest', '')
            require(re.fullmatch(r'[0-9a-f]{64}', requested_decision), 'REVIEW_PUBLISH_DECISION_REQUIRED')
            if decision is not None:
                require(decision == requested_decision, 'REVIEW_PUBLISH_DECISION_MISMATCH')
            else:
                require(owner.is_file() and owner.read_text() == binding['leaseToken'], 'REVIEW_PUBLISH_OWNER_MISMATCH')
                runtime = runtime_reader(binding)
                verify_runtime(binding, runtime)
                require(runtime == receipt['runtime'], 'REVIEW_PUBLISH_RUNTIME_CHANGED')
                write_durable(confirmed_path, dict(receiptDigest=digest, operationId=binding['operationId'],
                                                    confirmationDecisionDigest=requested_decision))
                decision = requested_decision
            # Replays after confirmation must never remove a later operation's owner.
            # A previous process may have stopped between atomic rename and directory fsync.
            if not historical_replay:
                with confirmed_path.open('rb') as confirmation_file:
                    os.fsync(confirmation_file.fileno())
                sync_directory(directory)
                if owner.is_file() and owner.read_text() == binding['leaseToken']:
                    owner.unlink()
                    sync_directory(root)
        elif decision is None:
            require(owner.is_file() and owner.read_text() == binding['leaseToken'], 'REVIEW_PUBLISH_OWNER_MISMATCH')
            runtime = runtime_reader(binding)
            verify_runtime(binding, runtime)
            require(runtime == receipt['runtime'], 'REVIEW_PUBLISH_RUNTIME_CHANGED')
        envelope = dict(schemaVersion=1, state='CONFIRMED' if decision is not None else 'AWAITING_CONFIRMATION',
                        binding=binding, runtime=receipt['runtime'], receiptDigest=digest,
                        receiptBase64=base64.b64encode(raw).decode('ascii'), confirmationDecisionDigest=decision)
        if emit:
            print('REVIEW_PUBLISH_RECEIPT_JSON=' + encode(envelope).decode('utf-8'))
        return envelope


if __name__ == '__main__':
    try:
        run(json.loads(base64.b64decode(sys.argv[1], validate=True)))
    except Exception as error:
        print('REVIEW_PUBLISH_RECEIPT_BLOCKED: ' + str(error), file=sys.stderr)
        sys.exit(1)
