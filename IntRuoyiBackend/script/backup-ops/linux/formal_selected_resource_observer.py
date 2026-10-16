"""Fresh local native facts for the single selected TEST resource contract.

No approval, allocation, service action, remote transport or cached observation.
Cross-host roles require a separately approved native transport and fail here.
"""
import json
import os
from pathlib import PurePosixPath
import re
import stat
import subprocess

from formal_boot_state import _directory, _signature
from formal_filesystem_watchdog import require_native_runtime, WatchdogError
from formal_selected_resource_admission import ROLES, _identity, _path
from formal_snapshot_linux import FormalSnapshotError, observation_timeout, require

CODE = 'RELEASE_SELECTED_NATIVE_RESOURCE_OBSERVATION_INVALID'

def _check(ok):
    require(ok, CODE)

def _native(action):
    try:
        return action()
    except (OSError, ValueError, TypeError, KeyError, AttributeError, RecursionError,
            FormalSnapshotError, WatchdogError, subprocess.SubprocessError):
        raise FormalSnapshotError(CODE) from None

def _json(raw):
    def pairs(items):
        value = {}
        for key, item in items:
            _check(key not in value)
            value[key] = item
        return value
    return json.loads(raw.decode('utf-8'), object_pairs_hook=pairs,
                      parse_constant=lambda _: _check(False))

def _command(argv):
    result = subprocess.run(argv, stdin=subprocess.DEVNULL, stdout=subprocess.PIPE,
        stderr=subprocess.DEVNULL, timeout=min(5, observation_timeout(5)), check=False)
    _check(result.returncode == 0 and len(result.stdout) <= 32768)
    return result.stdout

def _scan_source(path, *, byte_limit, entry_limit, guard):
    """Descriptor-anchored bounded metadata inventory, not a writer fence."""
    _native(lambda: (_path(path), _check(type(byte_limit) is int and byte_limit >= 0 and
        type(entry_limit) is int and entry_limit >= 0 and callable(guard)), require_native_runtime()))
    guard(); observation_timeout(1)
    descriptors = []
    try:
        with _directory(path, no_atime=True) as (root_fd, root_identity):
            root_dev = os.fstat(root_fd).st_dev
            totals = dict(sourceBytes=0, sourceFiles=0, sourceEntries=0)
            def frame(fd, parent=None, name=None):
                before = os.fstat(fd)
                _check(stat.S_ISDIR(before.st_mode) and before.st_dev == root_dev)
                names = os.listdir(fd)
                _check(len(names) <= entry_limit - totals['sourceEntries'])
                return [fd, iter(names), _signature(before), parent, name]
            stack = [_native(lambda: frame(root_fd))]
            while stack:
                observation_timeout(1)
                fd, names, before, parent, directory_name = stack[-1]
                name = next(names, None)
                if name is None:
                    def unchanged():
                        _check(_signature(os.fstat(fd)) == before)
                        if parent is not None:
                            _check(_signature(os.stat(directory_name, dir_fd=parent, follow_symlinks=False)) == before)
                    _native(unchanged)
                    stack.pop()
                    if fd != root_fd:
                        os.close(fd); descriptors.remove(fd)
                    continue
                totals['sourceEntries'] += 1
                _native(lambda: _check(totals['sourceEntries'] <= entry_limit))
                if totals['sourceEntries'] % 128 == 1:
                    guard(); observation_timeout(1)
                def inspect():
                    info = os.stat(name, dir_fd=fd, follow_symlinks=False)
                    _check(info.st_dev == root_dev and (stat.S_ISREG(info.st_mode) or stat.S_ISDIR(info.st_mode)))
                    if stat.S_ISREG(info.st_mode):
                        _check(info.st_nlink == 1 and info.st_size >= 0)
                        totals['sourceBytes'] += info.st_size; totals['sourceFiles'] += 1
                        _check(totals['sourceBytes'] <= byte_limit)
                        _check(_signature(os.stat(name, dir_fd=fd, follow_symlinks=False)) == _signature(info))
                    else:
                        child = os.open(name, os.O_RDONLY|os.O_DIRECTORY|os.O_NOFOLLOW|os.O_CLOEXEC|os.O_NOATIME, dir_fd=fd)
                        descriptors.append(child)
                        _check(_signature(os.fstat(child)) == _signature(info))
                        stack.append(frame(child, fd, name))
                _native(inspect)
            guard(); observation_timeout(1)
            def final_root():
                with _directory(path, no_atime=True) as (_, current):
                    _check(current == root_identity)
            _native(final_root)
            return totals
    except OSError:
        raise FormalSnapshotError(CODE) from None
    finally:
        for fd in reversed(descriptors):
            os.close(fd)

def _observe_role(name, approved, *, guard):
    _native(lambda: (_check(name in ROLES and callable(guard)), _identity(approved, name == 'source')))
    guard(); observation_timeout(1)
    def read():
        parent = approved['parent']; mount = approved['mount']
        with _directory(mount, no_atime=True) as (mount_fd, mount_identity):
            mount_info = os.fstat(mount_fd)
            device = os.lstat(approved['device'])
            _check(stat.S_ISBLK(device.st_mode) and os.path.realpath(approved['device']) == approved['device'] and
                device.st_rdev == mount_info.st_dev == approved['fileSystemId'] and
                f'{os.major(device.st_rdev)}:{os.minor(device.st_rdev)}' == approved['majorMinor'])
            rows = _json(_command(['findmnt', '--json', '--mountpoint', mount, '--output',
                                  'SOURCE,MAJ:MIN,FSTYPE,TARGET,UUID']))
            _check(type(rows) is dict and set(rows) == {'filesystems'} and len(rows['filesystems']) == 1)
            row = rows['filesystems'][0]
            _check(type(row) is dict and set(row) == {'source','maj:min','fstype','target','uuid'} and
                row['source'] == approved['device'] and row['maj:min'] == approved['majorMinor'] and
                row['target'] == mount and row['uuid'] == approved['filesystemUuid'] and
                row['fstype'] in (('ext4',) if name == 'source' else ('ext4','xfs')))
            # Nested mounts (including bind mounts on the same device) invalidate
            # the whole source-tree and role-parent device assumptions.
            tree = _json(_command(['findmnt','--json','--submounts','--mountpoint',mount,'--output','TARGET']))
            _check(tree == {'filesystems':[{'target':mount}]})
            with _directory(parent, no_atime=True) as (fd, identity):
                info = os.fstat(fd)
                _check(info.st_dev == mount_info.st_dev and info.st_ino == approved['parentInode'])
                absent = False
                if name != 'source':
                    try:
                        os.stat(PurePosixPath(approved['path']).name, dir_fd=fd, follow_symlinks=False)
                    except FileNotFoundError:
                        absent = True
                    _check(absent)
                available = os.fstatvfs(fd)
                _check(available.f_frsize > 0 and available.f_bavail >= 0 and available.f_favail >= 0)
                actual = dict(approved)
                actual.update(fileSystemId=info.st_dev, parentInode=info.st_ino,
                    device=row['source'], majorMinor=row['maj:min'], filesystemUuid=row['uuid'], mount=row['target'])
                result = dict(identity=actual, availableBytes=available.f_bavail*available.f_frsize,
                    availableInodes=available.f_favail, readOnly=bool(available.f_flag & os.ST_RDONLY),
                    targetAbsent=absent, filesystemType=row['fstype'])
                with _directory(parent, no_atime=True) as (_, current):
                    _check(current == identity)
            with _directory(mount, no_atime=True) as (_, current):
                _check(current == mount_identity)
            return result
    result = _native(read)
    guard(); observation_timeout(1)
    return result

def _observe_recovery_runtime(runtime):
    def read():
        images = {}
        for role in ('mysql','minio'):
            image = runtime[role+'ImageId']
            actual = _command(['docker','image','inspect','--format','{{.Id}}',image]).decode('ascii').strip()
            _check(actual == image)
            images[role+'ImageId'] = actual
            name = runtime[role+'Container']
            # No missing-object exit is interpreted as absence: only a successful
            # exact container-list query with empty output proves it.
            _check(_command(['docker','container','ls','--all','--no-trunc','--format','{{.ID}}',
                             '--filter','name=^/'+re.escape(name)+'$']).strip() == b'')
        output = _command(['docker','network','inspect','--format',
            '{{json .Id}} {{json .Internal}} {{json .Containers}}',runtime['networkId']]).decode('utf-8')
        decoder = json.JSONDecoder(); values = []; remaining = output.strip()
        while remaining:
            value, end = decoder.raw_decode(remaining); values.append(value); remaining = remaining[end:].lstrip()
        _check(len(values) == 3 and values[0] == runtime['networkId'] and values[1] is True and values[2] == {})
        identity = dict(runtime); identity.update(images, networkId=values[0])
        return dict(identity=identity, containersAbsent=True, networkInternal=values[1],
                    networkMemberCount=0, publishedPortCount=0)
    return _native(read)

def observe_selected_local_resources(plan, *, expected_host, expected_machine_id, guard):
    """Only independently selected current local host; never remote fallback."""
    def preflight():
        require_native_runtime()
        _check(os.geteuid() == 0 and callable(guard) and type(plan) is dict and set(plan) == {'resources','recoveryScope'})
        roles = plan['resources']['roles']; runtime = plan['recoveryScope']['runtime']
        _check(set(roles) == set(ROLES) and all(value['host'] == expected_host and
            value['machineId'] == expected_machine_id for value in roles.values()) and
            runtime['host'] == expected_host and runtime['machineId'] == expected_machine_id)
        raw = _command(['cat','/etc/machine-id']).decode('ascii').strip()
        _check(re.fullmatch('[0-9a-f]{32}', raw) and raw == expected_machine_id)
    _native(preflight)
    guard(); observation_timeout(1)
    roles = {name: _observe_role(name, plan['resources']['roles'][name], guard=guard) for name in ROLES}
    budget = plan['resources']['budget']
    totals = _scan_source(plan['resources']['roles']['source']['path'], byte_limit=budget['sourceBytes'],
                          entry_limit=budget['requiredInodes']-5, guard=guard)
    runtime = _observe_recovery_runtime(plan['recoveryScope']['runtime'])
    guard(); observation_timeout(1)
    # Re-read identity/absence/capacity after potentially long tree traversal.
    roles = {name: _observe_role(name, plan['resources']['roles'][name], guard=guard) for name in ROLES}
    return dict(roles=roles, sourceBytes=totals['sourceBytes'], sourceFiles=totals['sourceEntries'], recoveryRuntime=runtime)
