"""Real local pipes/processes; explicit target protocol fixtures, no server writes."""
import hashlib
import json
import subprocess
import sys
import time
from pathlib import Path
from unittest.mock import patch

import pytest

sys.path.insert(0, str(Path(__file__).parents[1] / 'linux'))
import formal_sql_restore as restore
from test_formal_sql_restore import fixture, Mysql


class Client:
    _password = 'owned-local-credential'

    def __init__(self, code):
        self.code = code

    def _command(self, program, arguments):
        assert 'timeout --signal=TERM --kill-after=5 ' in program
        assert '--default-character-set=utf8mb4' in arguments
        assert self._password not in repr(arguments)
        return [sys.executable, '-I', '-c', self.code]


@pytest.mark.parametrize('guard', [None, False, 'caller-declaration'])
def test_missing_current_guard_rejects_before_client_start(tmp_path, guard):
    path = tmp_path / 'expanded.sql'; path.write_bytes(b'SELECT 1;')
    with patch.object(restore.subprocess, 'Popen') as started:
        with pytest.raises(restore.FormalSnapshotError, match='^FORMAL_SQL_IMPORT_GUARD_REQUIRED$'):
            restore.stream_import(Client('pass'), path, hashlib.sha256(path.read_bytes()).hexdigest(),
                                  5, database='owned_probe', guard=guard)
        started.assert_not_called()


@pytest.mark.parametrize('wait', ['stdin', 'exit'])
def test_guard_rejection_during_native_wait_ends_owned_client(tmp_path, wait):
    path = tmp_path / 'expanded.sql'; raw = b'X' * (2 * 1024 * 1024); path.write_bytes(raw)
    marker = tmp_path / 'ready'
    code = ('import pathlib,sys,time; ' +
            ('sys.stdin.buffer.read(); ' if wait == 'exit' else '') +
            'pathlib.Path(' + repr(str(marker)) + ').write_bytes(b"ready"); time.sleep(30)')
    processes = []; native = subprocess.Popen
    def start(*args, **kwargs):
        process = native(*args, **kwargs); processes.append(process); return process
    def guard():
        if marker.exists():
            raise restore.FormalSnapshotError('FORMAL_SQL_SOURCE_CHANGED')
    began = time.monotonic()
    with patch.object(restore.subprocess, 'Popen', start):
        with pytest.raises(restore.FormalSnapshotError, match='^FORMAL_SQL_SOURCE_CHANGED$'):
            restore.stream_import(Client(code), path, hashlib.sha256(raw).hexdigest(), 5,
                                  database='owned_probe', guard=guard)
    assert marker.read_bytes() == b'ready'
    assert len(processes) == 1 and processes[0].poll() is not None
    assert time.monotonic() - began < 4


def test_revocation_after_first_megabyte_prevents_second_chunk(tmp_path):
    source = tmp_path / 'expanded.sql'; source.write_bytes(b'A' * (2 * 1024 * 1024))
    sent = []; permission_checks = []
    class Pipe:
        def write(self, raw): sent.append(raw); return len(raw)
        def close(self): pass
    class Process:
        stdin = Pipe(); returncode = None; killed = False
        def poll(self): return self.returncode
        def wait(self, timeout=None):
            if self.returncode is None: raise subprocess.TimeoutExpired('owned-protocol-client', timeout)
            return self.returncode
        def kill(self): self.killed = True; self.returncode = -9
    process = Process()
    def guard():
        permission_checks.append(True)
        if sum(len(part) for part in sent[1:]) >= 1024 * 1024:
            raise restore.FormalSnapshotError('FORMAL_SQL_TARGET_CHANGED')
    with patch.object(restore.subprocess, 'Popen', return_value=process):
        with pytest.raises(restore.FormalSnapshotError, match='^FORMAL_SQL_TARGET_CHANGED$'):
            restore.stream_import(Client('pass'), source, hashlib.sha256(source.read_bytes()).hexdigest(),
                                  5, database='owned_probe', guard=guard)
    assert len(permission_checks) >= 2 and process.killed
    assert sent[0] == (Client._password + '\n').encode()
    assert b''.join(sent[1:]) == b'A' * (1024 * 1024)


@pytest.mark.parametrize('change', ['source', 'target', 'self-transaction'])
def test_main_wires_import_guard_and_retains_import_failure(tmp_path, capsys, change):
    request, path, row = fixture(tmp_path)
    digest = hashlib.sha256(path.read_bytes()).hexdigest()
    mysql = Mysql(request['target']); importing = [False]; checked = []
    query = mysql.query
    def current(sql):
        if '@@server_uuid' in sql and importing[0]:
            checked.append(sql)
            if 'innodb_trx' in sql: return request['target']['databaseUuid'] + '\t0\t0\t1\tOFF\n'
            return request['target']['databaseUuid'] + '\t0\t0\tOFF\n'
        return query(sql)
    mysql.query = current
    def importer(client, expanded, checksum, seconds, *, database, guard):
        assert callable(guard)
        importing[0] = True
        try:
            guard()
            if change == 'source': (tmp_path / 'capture/011-source-data.json').write_bytes(b'{}')
            if change == 'target': row['HostConfig']['NetworkMode'] = 'host'
            guard()
        finally: importing[0] = False
        mysql.imported = True
    def observe(client, database, uuid, *, guard, scratch, **limits):
        from test_formal_data_evidence import contract_observation
        guard(); return contract_observation(request['target']['containerId'], uuid)
    with patch.object(restore, 'require_native_runtime'), \
         patch.object(restore, '_private_docker_json', return_value=row), \
         patch.object(restore, 'IsolatedTargetMySql', return_value=mysql), \
         patch.object(restore, 'stream_import', importer), patch.object(restore, 'observe_data', observe):
        result = restore.main(['--request', str(path), '--expected-request-sha256', digest])
    if change == 'self-transaction':
        assert result == 0 and checked and all('innodb_trx' not in sql for sql in checked)
        assert (tmp_path / 'evidence/restore-receipt.json').is_file()
    else:
        assert result == 1
        failed = json.loads((tmp_path / 'evidence/900-failed.json').read_bytes())
        assert failed['stage'] == 'IMPORT' and failed['targetReconciliationRequired'] is True
        assert failed['remoteImportStopConfirmed'] is False
        assert failed['code'] == ('FORMAL_DATA_SOURCE_CHANGED' if change == 'source' else 'FORMAL_SQL_NETWORK_INVALID')
        assert not (tmp_path / 'evidence/restore-receipt.json').exists()
    assert 'owned-private-secret' not in capsys.readouterr().out
