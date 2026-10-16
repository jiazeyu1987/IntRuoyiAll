"""Full private source semantics and actual native /proc boundaries."""
import copy
import hashlib
import importlib
import json
import os
import select
from pathlib import Path
import subprocess
import sys
import time

import pytest

sys.path.insert(0, str(Path(__file__).parents[1] / 'linux'))

def module():
    return importlib.import_module('formal_original_launch')

CID = 'a' * 64
IMAGE = 'sha256:' + 'b' * 64

def fixture():
    return {'Id': CID, 'Image': IMAGE, 'Path': '/bin/sh', 'Args': ['-c', 'exec java'],
        'Config': {'Env': ['PRIVATE=never-emit-me', 'ORDER=one', 'ORDER=two'],
                   'Cmd': ['sh', '-c', 'exec java'], 'Entrypoint': None},
        'HostConfig': {'ReadonlyRootfs': False, 'RestartPolicy': {'Name': 'unless-stopped'},
                       'FutureDockerField': {'raw': [None, 'one', 'two']}},
        'Mounts': [{'Type': 'bind', 'Source': '/private', 'Destination': '/config',
                    'RW': False, 'Propagation': 'rprivate'}],
        'State': {'Pid': 123, 'StartedAt': '2026-10-04T00:00:00Z', 'Running': True,
                  'Paused': False, 'Restarting': False, 'Dead': False, 'OOMKilled': False}}

def project(value=None):
    return module().inspect_projection(json.dumps([fixture() if value is None else value]).encode(), CID, IMAGE)

def test_private_sources_only_emit_complete_digests():
    result = project()
    assert 'never-emit-me' not in json.dumps(result)
    assert set(result['digests']) == {'configSha256', 'hostConfigSha256', 'imageSha256',
                                     'pathSha256', 'argsSha256', 'mountsSha256', 'launchSha256'}
    assert all(len(v) == 64 for v in result['digests'].values())

@pytest.mark.parametrize('field', ['Config', 'HostConfig', 'Image', 'Path', 'Args', 'Mounts'])
def test_each_full_field_change_is_visible(field):
    original = fixture(); changed = copy.deepcopy(original)
    if field == 'Image':
        changed[field] = 'sha256:' + 'c' * 64
        with pytest.raises(module().LaunchObservationError, match='IMAGE_CHANGED'):
            project(changed)
        return
    if field in ('Config', 'HostConfig'): changed[field]['previouslyUnknown'] = ['secret', None]
    elif field == 'Mounts': changed[field][0]['Propagation'] = 'shared'
    elif field == 'Args': changed[field].append('-Dprivate=secret')
    else: changed[field] = '/changed'
    assert project(changed)['digests']['launchSha256'] != project(original)['digests']['launchSha256']

@pytest.mark.parametrize('change', ['order', 'duplicate', 'null'])
def test_env_array_semantics_remain_exact(change):
    value = fixture()
    if change == 'order': value['Config']['Env'].reverse()
    elif change == 'duplicate': value['Config']['Env'].append(value['Config']['Env'][0])
    else: value['Config']['Env'] = None
    assert project(value)['digests']['configSha256'] != project()['digests']['configSha256']

def test_full_mount_row_order_only_is_stable():
    value = fixture(); value['Mounts'].append({'Type': 'volume', 'Name': 'owned', 'Destination': '/data', 'RW': True})
    other = copy.deepcopy(value); other['Mounts'].reverse()
    assert project(value)['digests'] == project(other)['digests']

@pytest.mark.parametrize('raw', [b'[{"Id":"a","Id":"b"}]', b'[NaN]', b'[Infinity]', b'[]', b'[{},{}]', b'{}', b'['])
def test_invalid_private_json_is_fixed_and_redacted(raw):
    with pytest.raises(module().LaunchObservationError) as error:
        module().inspect_projection(raw, CID, IMAGE)
    assert str(error.value).startswith('ORIGINAL_LAUNCH_')

@pytest.mark.parametrize('change', ['missing', 'wrong-id', 'pid-bool', 'paused', 'dead', 'stopped'])
def test_unstable_identity_refuses(change):
    value = fixture()
    if change == 'missing': value.pop('HostConfig')
    elif change == 'wrong-id': value['Id'] = 'd' * 64
    elif change == 'pid-bool': value['State']['Pid'] = True
    else: value['State'][{'paused': 'Paused', 'dead': 'Dead', 'stopped': 'Running'}[change]] = change != 'stopped'
    with pytest.raises(module().LaunchObservationError): project(value)

def test_actual_native_process_and_environment_not_emitted():
    m = module()
    child = subprocess.Popen([sys.executable, '-c', 'import time; print("READY",flush=True); time.sleep(15)'],
                             env={**os.environ, 'LAUNCH_PRIVATE_TEST': 'never-emit-me',
                                  'LAUNCH_LARGE_TEST': 'x' * 70000}, stdout=subprocess.PIPE)
    try:
        assert select.select([child.stdout], [], [], 2)[0] and child.stdout.readline() == b'READY\n'
        first = m.process_snapshot(child.pid, deadline=time.monotonic() + 3)
        second = m.process_snapshot(child.pid, deadline=time.monotonic() + 3)
        assert first == second and first['pid'] == child.pid and first['startTicks'] > 0
        assert 'never-emit-me' not in json.dumps(first)
        assert first['cmdlineSha256'] and first['environSha256']
        assert first['environSha256'] == hashlib.sha256(Path('/proc/' + str(child.pid) + '/environ').read_bytes()).hexdigest()
    finally:
        child.terminate(); child.wait(timeout=3)
        child.stdout.close()
    with pytest.raises(m.LaunchObservationError): m.process_snapshot(child.pid, deadline=time.monotonic() + 1)

@pytest.mark.parametrize('body', ['import time; time.sleep(3)',
    'import sys; sys.stdout.write("x"*10000)',
    'import sys; sys.stderr.write("secret-value")', 'raise SystemExit(7)'])
def test_actual_bounded_command_refuses_timeout_output_warning_exit(body):
    m = module()
    with pytest.raises(m.LaunchObservationError) as error:
        m._bounded_command([sys.executable, '-c', body], deadline=time.monotonic() + .4, limit=100)
    assert 'secret-value' not in str(error.value)

def test_expired_deadline_refuses_before_launch():
    with pytest.raises(module().LaunchObservationError, match='DEADLINE'):
        module()._bounded_command(['/definitely-not-run'], deadline=time.monotonic() - 1, limit=100)

def test_same_observation_rejects_complete_process_drift():
    first = {'inspect': project(), 'process': {'pid': 123, 'environSha256': 'a'}}
    changed = copy.deepcopy(first); changed['process']['environSha256'] = 'b'
    with pytest.raises(module().LaunchObservationError, match='SOURCE_CHANGED'):
        module().require_same_observation(first, changed)

@pytest.mark.parametrize('args', [
    ['java', '-cp', '/owned/classes', 'OtherMain', '-jar', 'app.jar'],
    ['java', 'OtherMain', '-jar', 'app.jar'],
    ['java', '--class-path', '-jar', 'app.jar'],
    ['java', '--module=other/main', '-jar', 'app.jar'],
    ['java', '--source', '17', '-jar', 'app.jar'],
    ['java', '-version', '-jar', 'app.jar'],
    ['java', '-jar', 'other.jar'],
])
def test_jar_as_foreign_main_argument_is_refused(args):
    raw = ('\0'.join(args) + '\0').encode()
    with pytest.raises(module().LaunchObservationError, match='JAVA_LAUNCH_UNAVAILABLE'):
        module().java_jar_entry(raw, '/usr/bin/java', '/yudao-server')

def test_self_contained_original_jvm_options_and_application_args():
    args = ['java', '-Xms512m', '-Xmx1g', '-Dprivate=never-emit-me', '-XX:+UseG1GC',
            '-jar', 'app.jar', '--spring.profiles.active=prod']
    assert module().java_jar_entry(('\0'.join(args) + '\0').encode(), '/usr/bin/java', '/yudao-server') == '/yudao-server/app.jar'
