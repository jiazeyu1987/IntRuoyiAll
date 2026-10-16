"""Actual inspection helpers with real local clients; explicit Docker protocol.

SQL and S3 fixtures do not establish maintained-server authority or restoration.
"""
import hashlib
import importlib
import json
from pathlib import Path
import subprocess
import sys
import time

import pytest

sys.path.insert(0, str(Path(__file__).parents[1] / 'linux'))
from formal_snapshot_linux import FormalSnapshotError, observation_deadline
from test_formal_sql_restore import fixture as sql_fixture, load as sql_module


def object_target():
    target = dict(containerId='a'*64, imageId='sha256:'+'b'*64,
                  endpoint='http://172.18.0.2:9000', credentialFile='unused')
    row = dict(Id=target['containerId'], Image=target['imageId'], State={'Running': True},
        Config={'Labels': {'intruoyi.formal-restore-operation': 'owned-restore'}},
        HostConfig={'Privileged': False},
        NetworkSettings={'Networks': {'isolated': {'IPAddress': '172.18.0.2'}}})
    return target, row, 'owned-restore'


def docker_protocol(monkeypatch, row, *, delay=0, on_launch=None):
    """Substitute only Docker executable; retain real Popen/pipes/guard polling."""
    native = subprocess.Popen
    children = []
    commands = []
    def launch(command, *args, **kwargs):
        assert command[0] == 'docker'
        commands.append(command)
        script = 'import sys,time;time.sleep(float(sys.argv[1]));print(sys.argv[2])'
        child = native([sys.executable, '-I', '-c', script, str(delay), json.dumps([row])], *args, **kwargs)
        children.append(child)
        if on_launch is not None:
            on_launch()
        return child
    monkeypatch.setattr(subprocess, 'Popen', launch)
    return children, commands


@pytest.mark.parametrize('route', ['sql', 'object'])
def test_actual_target_inspection_requires_and_uses_current_guard(tmp_path, monkeypatch, route):
    if route == 'sql':
        request, _, row = sql_fixture(tmp_path)
        module, target, operation = sql_module(), request['target'], request['operationId']
    else:
        module = importlib.import_module('formal_restore_producer')
        target, row, operation = object_target()
    children, _ = docker_protocol(monkeypatch, row)
    observations = []
    def guard():
        observations.append(observation_deadline.get())
    module.bound_target(target, operation, guard=guard)
    assert observations and all(deadline is not None for deadline in observations)
    assert len(children) == 1 and children[0].poll() == 0 and children[0].stdout.closed


@pytest.mark.parametrize('route', ['sql', 'object'])
def test_actual_inspection_revocation_reaps_client(tmp_path, monkeypatch, route):
    if route == 'sql':
        request, _, row = sql_fixture(tmp_path)
        module, target, operation = sql_module(), request['target'], request['operationId']
    else:
        module = importlib.import_module('formal_restore_producer')
        target, row, operation = object_target()
    children, _ = docker_protocol(monkeypatch, row, delay=30)
    def guard():
        if children:
            raise FormalSnapshotError('FORMAL_TEST_SOURCE_REVOKED')
    started = time.monotonic()
    with pytest.raises(FormalSnapshotError, match='FORMAL_TEST_SOURCE_REVOKED'):
        module.bound_target(target, operation, guard=guard)
    assert time.monotonic()-started < 2
    assert len(children) == 1 and children[0].poll() is not None and children[0].stdout.closed


def test_normal_sql_entry_rechecks_actual_request_while_inspecting(tmp_path, monkeypatch, capsys):
    module = sql_module()
    request, path, row = sql_fixture(tmp_path)
    digest = hashlib.sha256(path.read_bytes()).hexdigest()
    monkeypatch.setattr(module, 'require_native_runtime', lambda: None)
    forbidden = []
    def no_import(*args, **kwargs):
        forbidden.append(True)
        raise AssertionError('Import must not start')
    monkeypatch.setattr(module, 'stream_import', no_import)
    children, commands = docker_protocol(monkeypatch, row, delay=30,
        on_launch=lambda: path.write_bytes(b'{}'))
    assert module.main(['--request', str(path), '--expected-request-sha256', digest]) == 1
    output = json.loads(capsys.readouterr().out)
    assert output['code'] == 'FORMAL_SQL_SOURCE_CHANGED'
    assert not forbidden and len(commands) == 1
    assert children[0].poll() is not None and children[0].stdout.closed
    assert not Path(request['evidenceRoot']).exists()


@pytest.mark.parametrize('drift', ['request', 'receipt', 'inventory'])
def test_normal_object_entry_rechecks_actual_pins_while_inspecting(tmp_path, monkeypatch, capsys, drift):
    from test_formal_s3_restore import Source
    from formal_s3_export import export_s3_versions
    module = importlib.import_module('formal_restore_producer')
    source = tmp_path/'export'
    export_s3_versions(Source(), ['yudao'], source, guard=lambda: None)  # Explicit S3 protocol fixture.
    target, row, operation = object_target()
    req = dict(schemaVersion='release-formal-object-restore-request/v2', operationId=operation,
        sourceExport=str(source), evidenceRoot=str(tmp_path/'restore'), target=target,
        bucketMapping={'yudao': 'yudao-backup-next'},
        sourceInventorySha256=hashlib.sha256((source/'versions.jsonl').read_bytes()).hexdigest(),
        sourceExportReceiptSha256=hashlib.sha256((source/'export-receipt.json').read_bytes()).hexdigest())
    path = tmp_path/'request.json'
    path.write_bytes(json.dumps(req).encode()); path.chmod(0o600)
    digest = hashlib.sha256(path.read_bytes()).hexdigest()
    monkeypatch.setattr(module, 'require_native_runtime', lambda: None)
    forbidden = []
    def no_restore(*args, **kwargs):
        forbidden.append(True)
        raise AssertionError('Restore must not start')
    monkeypatch.setattr(module, 'restore_s3_versions', no_restore)
    changed = {'request': path, 'receipt': source/'export-receipt.json',
               'inventory': source/'versions.jsonl'}[drift]
    children, commands = docker_protocol(monkeypatch, row, delay=30,
        on_launch=lambda: changed.write_bytes(b'{}'))
    assert module.main(['--request', str(path), '--expected-request-sha256', digest]) == 1
    output = json.loads(capsys.readouterr().out)
    assert output['code'] == ('FORMAL_RESTORE_REQUEST_CHANGED' if drift == 'request' else 'FORMAL_RESTORE_SOURCE_CHANGED')
    assert not forbidden and len(commands) == 1
    assert children[0].poll() is not None and children[0].stdout.closed
    assert not Path(req['evidenceRoot']).exists()


def test_sql_volume_inventory_uses_same_guard_and_reaps_stall(tmp_path, monkeypatch):
    module = sql_module()
    request, _, row = sql_fixture(tmp_path)
    target = request['target']; target['dataVolume'] = 'owned-volume'
    row['Mounts'] = [dict(Destination='/var/lib/mysql', Type='volume', Name='owned-volume', Source='/owned/volume')]
    volume = dict(Name='owned-volume', Driver='local', Options={}, Mountpoint='/owned/volume',
                  Labels={'intruoyi.formal-sql-restore-operation': request['operationId']})
    native = subprocess.Popen; children = []; commands = []
    def launch(command, *args, **kwargs):
        commands.append(command)
        stalled = command[1] == 'ps'
        payload = volume if command[1] == 'volume' else row
        child = native([sys.executable, '-I', '-c',
            'import sys,time;time.sleep(float(sys.argv[1]));print(sys.argv[2])',
            '30' if stalled else '0', json.dumps([payload])], *args, **kwargs)
        children.append(child)
        return child
    monkeypatch.setattr(subprocess, 'Popen', launch)
    def guard():
        if commands and commands[-1][1] == 'ps':
            raise FormalSnapshotError('FORMAL_TEST_SOURCE_REVOKED')
    with pytest.raises(FormalSnapshotError, match='FORMAL_TEST_SOURCE_REVOKED'):
        module.bound_target(target, request['operationId'], guard=guard)
    assert [command[1] for command in commands] == ['inspect', 'volume', 'ps']
    assert all(child.poll() is not None and child.stdout.closed for child in children)
