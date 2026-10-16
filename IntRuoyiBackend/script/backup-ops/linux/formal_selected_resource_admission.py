"""Selected TEST private-original and finite resource admission, without effects.

The observer is trusted executor code doing fresh native reads, never a request
field or cached report. This gate does not consume authority, persist STARTED,
prove continuous exclusion, or complete capture/restoration. Its detached plan
must be guarded again at each subsequent effect boundary.
"""
import hashlib
import json
import os
from pathlib import PurePosixPath
import re
import time

import formal_snapshot_linux as snapshot
from formal_boot_state import _directory, _read_secure
from formal_filesystem_watchdog import require_native_runtime
from formal_open_authorization import _selected_binding, SELECTED_ACTIONS
from formal_snapshot_linux import FormalSnapshotError, require

CODE = 'RELEASE_SELECTED_RESOURCE_ADMISSION_INVALID'
ROLES = ('source', 'control', 'capture', 'recoveryMysql', 'recoveryMinio')
ORIGINALS = ('resources', 'stopAuthority', 'restoreAuthority', 'recoveryScope')
IDENTITY = ('host', 'machineId', 'mount', 'device', 'majorMinor', 'filesystemUuid',
            'fileSystemId', 'parent', 'parentInode', 'path')
BUDGET = ('sourceBytes', 'sqlBytes', 'metadataBytes', 'growthBytes', 'retainedBytes',
          'reserveBytes', 'requiredInodes', 'copyBytesPerSecond', 'freezeSeconds', 'totalSeconds')
RUNTIME = ('host', 'machineId', 'database', 'mysqlContainer', 'minioContainer', 'networkId',
           'mysqlImageId', 'minioImageId')
COMMON = ('schemaVersion', 'dispatchBinding', 'sourceBinding', 'issuedAt', 'expiresAt')


def _check(value):
    require(value, CODE)


def _fields(value, fields):
    _check(type(value) is dict and set(value) == set(fields))


def _hex(value, length=64):
    return type(value) is str and re.fullmatch('[0-9a-f]{'+str(length)+'}', value) is not None


def _integer(value, minimum=0):
    return type(value) is int and minimum <= value <= 2**63-1


def _canonical(value):
    return json.dumps(value, sort_keys=True, separators=(',', ':'), allow_nan=False, ensure_ascii=True).encode('ascii')


def _json(raw):
    def pairs(items):
        result = {}
        for key, value in items:
            _check(key not in result)
            result[key] = value
        return result
    def constant(_):
        raise FormalSnapshotError(CODE)
    return json.loads(raw.decode('utf-8'), object_pairs_hook=pairs, parse_constant=constant)


def _internal(action):
    """Redact native custody/shape errors; external guard/observer failures propagate."""
    try:
        return action()
    except (OSError, FormalSnapshotError, ValueError, UnicodeError, TypeError, KeyError,
            AttributeError, RecursionError, OverflowError):
        raise FormalSnapshotError(CODE) from None


def _path(value):
    _check(type(value) is str and value.startswith('/') and value != '/' and
           len(value.encode('utf-8')) <= 4096 and '\0' not in value and
           str(PurePosixPath(value)) == value and not value.startswith('//') and
           all(part not in ('', '.', '..') and len(part.encode('utf-8')) <= 255 for part in value.split('/')[1:]))
    return PurePosixPath(value)


def _host(value):
    _check(type(value) is str and re.fullmatch(r'[0-9]+\.[0-9]+\.[0-9]+\.[0-9]+', value) and
           all(str(int(part)) == part and 0 <= int(part) <= 255 for part in value.split('.')))


def _identity(value, source):
    _fields(value, IDENTITY)
    _host(value['host'])
    _check(_hex(value['machineId'], 32) and _integer(value['fileSystemId'], 1) and
           _integer(value['parentInode'], 1) and type(value['majorMinor']) is str and
           re.fullmatch('[0-9]+:[0-9]+', value['majorMinor']) and type(value['filesystemUuid']) is str and
           re.fullmatch('[0-9a-f]{8}(-[0-9a-f]{4}){3}-[0-9a-f]{12}', value['filesystemUuid']))
    device = _path(value['device'])
    _check(PurePosixPath('/dev') in device.parents)
    mount, parent, path = (_path(value[key]) for key in ('mount', 'parent', 'path'))
    _check(parent == mount or mount in parent.parents)
    _check(path == parent if source else path.parent == parent)


def _filesystem(role):
    return role['machineId'], role['fileSystemId']


def _validate_plan(resources, recovery, expected_dispatch, expected_source):
    # Reuse the existing current-binding contract, without approval metadata.
    _selected_binding(dict(schemaVersion='release-selected-test-service-authority/v1', role='stopAuthority',
        purpose='CLOSE_SELECTED_SOURCE_FOR_CAPTURE', dispatchBinding=expected_dispatch,
        sourceBinding=expected_source, policy=dict(actions=SELECTED_ACTIONS)))
    for value, schema, extra in ((resources, 'release-selected-test-resources/v1', ('roles', 'budget')),
            (recovery, 'release-selected-test-recovery-scope/v1', ('runtime', 'policy'))):
        _fields(value, COMMON + extra)
        _check(value['schemaVersion'] == schema and
               _canonical(value['dispatchBinding']) == _canonical(expected_dispatch) and
               _canonical(value['sourceBinding']) == _canonical(expected_source) and
               _integer(value['issuedAt'], 1) and _integer(value['expiresAt'], 1) and
               1 <= value['expiresAt']-value['issuedAt'] <= 3600)
    roles = resources['roles']; budget = resources['budget']
    _fields(roles, ROLES); _fields(budget, BUDGET)
    _check(all(_integer(value) for value in budget.values()) and
           all(budget[key] > 0 for key in ('metadataBytes', 'reserveBytes', 'requiredInodes',
                                           'copyBytesPerSecond', 'freezeSeconds', 'totalSeconds')) and
           budget['freezeSeconds'] <= 300 and budget['totalSeconds'] <= 7200)
    for name, role in roles.items():
        _identity(role, name == 'source')
    source = roles['source']; current = expected_source['scope']
    target = expected_source['instanceIdentity']['target']; storage = target['minioStorage']
    _fields(storage, ('mode', 'mount', 'device', 'majorMinor', 'filesystemUuid', 'fileSystemId', 'sourceDirectory'))
    _check(target['schemaVersion'] == 'release-instance-target/v3' and storage['mode'] == 'local-bind' and
           target['host'] == current['host'] == source['host'] and current['machineId'] == source['machineId'] and
           source['path'] == storage['sourceDirectory'] and storage['sourceDirectory'] == storage['mount']+'/minio' and
           re.fullmatch('/mnt/[A-Za-z0-9_.-]+', storage['mount']) and
           all(_canonical(source[key]) == _canonical(storage[key]) for key in
               ('mount', 'device', 'majorMinor', 'filesystemUuid', 'fileSystemId')))
    volume = expected_source['containers']['minio']['volume']; proof = volume['bindProof']
    _fields(proof, ('sourceDirectory', 'mount', 'device', 'majorMinor', 'filesystemUuid', 'fileSystemId', 'inode'))
    _check(all(_canonical(proof[key]) == _canonical(storage[key]) for key in proof if key != 'inode') and
           type(proof['inode']) is int and type(volume['inode']) is int and
           proof['inode'] == volume['inode'] == source['parentInode'] and
           type(volume['fileSystemId']) is int and volume['fileSystemId'] == source['fileSystemId'] and
           _integer(expected_source['containers']['mysql']['volume']['fileSystemId'], 1) and
           expected_source['containers']['mysql']['volume']['fileSystemId'] != source['fileSystemId'])
    _check(all(roles[name]['host'] == source['host'] and roles[name]['machineId'] == source['machineId']
               for name in ('control', 'capture')) and
           _filesystem(roles['control']) == _filesystem(roles['capture']) != _filesystem(source))
    for name in ('recoveryMysql', 'recoveryMinio'):
        _check(_filesystem(roles[name]) not in (_filesystem(source), _filesystem(roles['capture'])))
    # Identity consistency and lexical/known inode aliases cannot make two roles independent.
    for index, left in enumerate(ROLES):
        for right in ROLES[index+1:]:
            a, b = roles[left], roles[right]
            _check((a['host'] == b['host']) == (a['machineId'] == b['machineId']))
            if a['machineId'] == b['machineId']:
                p, q = PurePosixPath(a['path']), PurePosixPath(b['path'])
                _check(p != q and p not in q.parents and q not in p.parents)
                # Native identifiers on one machine cannot be relabelled with
                # another filesystem ID to evade separation or aggregation.
                for key in ('device', 'majorMinor', 'filesystemUuid'):
                    _check((a[key] == b[key]) == (_filesystem(a) == _filesystem(b)))
            if _filesystem(a) == _filesystem(b):
                _check(all(a[key] == b[key] for key in ('device', 'majorMinor', 'filesystemUuid')))
                _check(a['parentInode'] != b['parentInode'] or
                       (left != 'source' and right != 'source' and PurePosixPath(a['path']).name != PurePosixPath(b['path']).name))
    runtime = recovery['runtime']; _fields(runtime, RUNTIME)
    for name in ('recoveryMysql', 'recoveryMinio'):
        _check(runtime['host'] == roles[name]['host'] and runtime['machineId'] == roles[name]['machineId'])
    _check(runtime['database'] == current['database'] and _hex(runtime['networkId']) and
           runtime['mysqlContainer'] != runtime['minioContainer'] and
           all(type(runtime[key]) is str and re.fullmatch('[A-Za-z0-9][A-Za-z0-9_.-]{0,127}', runtime[key]) and
               runtime[key] not in target['containers'].values() for key in ('mysqlContainer', 'minioContainer')) and
           all(runtime[role+'ImageId'] == expected_source['containers'][role]['imageId'] for role in ('mysql', 'minio')))
    _fields(recovery['policy'], ('preservePhysicalVersions', 'preserveProtectionMetadata', 'startApplication', 'retainFailedArtifacts'))
    _check(_canonical(recovery['policy']) == _canonical(dict(preservePhysicalVersions=True,
        preserveProtectionMetadata=True, startApplication=False, retainFailedArtifacts=True)))
    s, d, m, g, h = (budget[key] for key in ('sourceBytes', 'sqlBytes', 'metadataBytes', 'growthBytes', 'retainedBytes'))
    needed = dict(source=g, control=m, capture=s+g+d+m+h, recoveryMysql=d+g, recoveryMinio=s+g)
    rate = budget['copyBytesPerSecond']
    _check((s+g+rate-1)//rate+12 <= budget['freezeSeconds'] and
           (needed['capture']+needed['recoveryMysql']+needed['recoveryMinio']+rate-1)//rate+12 <= budget['totalSeconds'])
    return needed


def _validate_observation(value, plan, needed):
    _fields(value, ('roles', 'sourceBytes', 'sourceFiles', 'recoveryRuntime'))
    _fields(value['roles'], ROLES)
    resources = plan['resources']; budget = resources['budget']
    _check(_integer(value['sourceBytes']) and _integer(value['sourceFiles']) and
           value['sourceBytes'] <= budget['sourceBytes'] and value['sourceFiles']+5 <= budget['requiredInodes'])
    groups = {}
    for name in ROLES:
        observed = value['roles'][name]; approved = resources['roles'][name]
        _fields(observed, ('identity', 'availableBytes', 'availableInodes', 'readOnly', 'targetAbsent', 'filesystemType'))
        _check(_canonical(observed['identity']) == _canonical(approved) and
               _integer(observed['availableBytes']) and _integer(observed['availableInodes']) and
               observed['readOnly'] is False and observed['targetAbsent'] is (name != 'source') and
               observed['filesystemType'] in (('ext4',) if name == 'source' else ('ext4', 'xfs')))
        key = _filesystem(approved)
        group = groups.setdefault(key, dict(bytes=0, inodes=0, free=observed['availableBytes'], freeInodes=observed['availableInodes']))
        group['bytes'] += needed[name]; group['inodes'] += budget['requiredInodes']
        group['free'] = min(group['free'], observed['availableBytes'])
        group['freeInodes'] = min(group['freeInodes'], observed['availableInodes'])
    _check(all(group['free'] >= group['bytes']+budget['reserveBytes'] and group['freeInodes'] >= group['inodes']
               for group in groups.values()))
    runtime = value['recoveryRuntime']
    _fields(runtime, ('identity', 'containersAbsent', 'networkInternal', 'networkMemberCount', 'publishedPortCount'))
    _check(_canonical(runtime['identity']) == _canonical(plan['recoveryScope']['runtime']) and
           runtime['containersAbsent'] is True and runtime['networkInternal'] is True and
           type(runtime['networkMemberCount']) is int and runtime['networkMemberCount'] == 0 and
           type(runtime['publishedPortCount']) is int and runtime['publishedPortCount'] == 0)


def _read_inputs(private_root, collection_sha256, dispatch):
    root = _path(private_root)
    with _directory(str(root), no_atime=True) as (fd, identity):
        _check(os.fstat(fd).st_mode & 0o7777 == 0o700)
    path = str(root/'test.capture-sources.json')
    original = _read_secure(path, 0o600, 16384, no_atime=True)
    _check(hashlib.sha256(original[0]).hexdigest() == collection_sha256)
    collection = _json(original[0])
    _fields(collection, ('schemaVersion', 'environment', 'host', 'targetDigest', 'deploymentBindingDigest', 'originals'))
    _check(collection['schemaVersion'] == 'release-selected-test-capture-sources/v1' and collection['environment'] == 'test' and
           collection['host'] == '172.30.30.58' and all(collection[key] == dispatch[key] for key in ('targetDigest', 'deploymentBindingDigest')))
    _fields(collection['originals'], ORIGINALS)
    names = set(); values = {}; held = {path: original}
    for role in ORIGINALS:
        ref = collection['originals'][role]; _fields(ref, ('file', 'sha256'))
        _check(type(ref['file']) is str and re.fullmatch(r'[A-Za-z0-9][A-Za-z0-9_.-]{0,119}\.json', ref['file']) and
               ref['file'] != 'test.capture-sources.json' and ref['file'] not in names and _hex(ref['sha256']))
        names.add(ref['file'])
        path = str(root/ref['file']); contents = _read_secure(path, 0o600, 65536, no_atime=True)
        _check(hashlib.sha256(contents[0]).hexdigest() == ref['sha256'])
        held[path] = contents
        if role in ('resources', 'recoveryScope'): values[role] = _json(contents[0])
    return values, held, identity, {role:collection['originals'][role]['sha256'] for role in ORIGINALS}


def admit_selected_test_resources(private_root, collection_sha256, expected_dispatch, expected_source, *, observe, guard):
    """Read-only finite gate; required callbacks are independently trusted code."""
    def preflight():
        require_native_runtime()
        _check(os.geteuid() == 0 and _hex(collection_sha256) and callable(observe) and callable(guard))
        return _canonical(expected_dispatch), _canonical(expected_source)
    dispatch_bytes, source_bytes = _internal(preflight)
    fixed_dispatch, fixed_source = _json(dispatch_bytes), _json(source_bytes)
    started = time.monotonic(); wall = time.time()
    token = snapshot.observation_deadline.set(started+snapshot.observation_timeout(30))
    try:
        guard(); snapshot.observation_timeout(1)
        plan, held, root_identity, original_pins = _internal(lambda: _read_inputs(private_root, collection_sha256, fixed_dispatch))
        needed = _internal(lambda: _validate_plan(plan['resources'], plan['recoveryScope'], fixed_dispatch, fixed_source))
        plan_bytes = _canonical(plan)
        def validity():
            elapsed = time.monotonic()-started; now = time.time()
            _check(elapsed >= 0 and now >= wall and abs((now-wall)-elapsed) < 2)
            budget = plan['resources']['budget']['totalSeconds']
            _check(all(value['issuedAt'] <= now and max(now, wall+elapsed)+budget < value['expiresAt'] for value in plan.values()))
            snapshot.observation_timeout(1)
        _internal(validity)
        for _ in range(2):
            observed = observe(_json(plan_bytes))
            _internal(lambda: _validate_observation(observed, plan, needed))
            guard(); _internal(validity)
        def final_read():
            _check(_canonical(expected_dispatch) == dispatch_bytes and _canonical(expected_source) == source_bytes)
            with _directory(private_root, no_atime=True) as (_, current): _check(current == root_identity)
            for path, original in held.items():
                _check(_read_secure(path, 0o600, 65536, no_atime=True) == original)
            validity()
        _internal(final_read)
        return dict(state='SELECTED_RESOURCE_POINT_IN_TIME_ADMITTED', collectionSha256=collection_sha256,
            originalsSha256=original_pins, plan=_json(plan_bytes), grantConsumed=False, startedPersisted=False,
            sourceEffectsPerformed=False, continuousProtectionProven=False, recoveryCompleted=False, releaseReady=False)
    finally:
        snapshot.observation_deadline.reset(token)
