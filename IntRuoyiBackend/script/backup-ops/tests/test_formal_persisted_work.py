"""Fixed read-only SQL/approval boundary contracts; native DB tested separately."""
import hashlib
import importlib
import json
from pathlib import Path
import sys
import time

import pytest

sys.path.insert(0, str(Path(__file__).parents[1] / 'linux'))
import formal_snapshot_linux as snapshot

UUID = '00000000-0000-0000-0000-000000000001'


def module():
    return importlib.import_module('formal_persisted_work')


def test_exact_seven_sources_and_readonly_transaction_sql():
    m = module()
    assert len(m.SOURCES) == 7
    sql = m.snapshot_sql('owned_probe', 900)
    assert 'SET TRANSACTION READ ONLY' in sql
    assert 'WITH CONSISTENT SNAPSHOT' in sql
    assert 'SELECT @@server_uuid' not in sql  # Identity joins the marked response.
    assert 'PERSISTED_BEGIN' in sql and 'PERSISTED_END' in sql
    assert 'MAX_EXECUTION_TIME(900)' in sql
    assert not any(word in sql.upper() for word in (' UPDATE ', ' DELETE ', ' INSERT ', ' LIMIT '))
    assert 'BINARY' in sql
    for source in m.SOURCES.values():
        assert source['table'] in sql
        assert 'tenant_id' in source['columns'] and 'deleted' in source['columns']


@pytest.mark.parametrize('database', ['x`;DROP DATABASE x', 'a.b', '', None, True, 'x\n'])
def test_invalid_catalog_refuses_before_sql(database):
    with pytest.raises(snapshot.FormalSnapshotError, match='FORMAL_PERSISTED_DATABASE_INVALID'):
        module().snapshot_sql(database, 100)


def response(m, counts=None):
    rows = ['PERSISTED_BEGIN\t%s\towned_probe' % UUID]
    for name in m.SOURCES:
        values = (counts or {}).get(name, [0] * 7)
        rows.append('\t'.join([name, *map(str, values)]))
    rows.append('PERSISTED_END\t%s\towned_probe' % UUID)
    return '\n'.join(rows) + '\n'


def test_empty_selected_snapshot_does_not_attest_full_drain():
    m = module(); result = m.parse_snapshot(response(m), 'owned_probe', UUID)
    assert result['selectedTablesTerminalConfirmed'] is True
    assert result['fullDrain'] is False and result['releaseReady'] is False
    assert result['countsSha256'] == hashlib.sha256(snapshot._canonical_bytes(result['counts'])).hexdigest()


def test_failed_outcome_is_terminal_but_never_business_success():
    m = module()
    result = m.parse_snapshot(response(m, {'nas-transfer': [2, 2, 0, 2, 0, 0, 2]}), 'owned_probe', UUID)
    assert result['selectedTablesTerminalConfirmed'] is True
    assert result['failedOutcomeCount'] == 2
    assert result['releaseReady'] is False


@pytest.mark.parametrize('row', [[1, 1, 1, 0, 0, 0, 0], [1, 0, 0, 1, 0, 1, 0]])
def test_pending_and_inconsistent_refuse_selected_terminal_claim(row):
    m = module(); result = m.parse_snapshot(response(m, {'upload-ticket': row}), 'owned_probe', UUID)
    assert result['selectedTablesTerminalConfirmed'] is False


@pytest.mark.parametrize('kind', ['missing', 'duplicate', 'foreign', 'negative', 'bool', 'fraction', 'overflow',
                                  'conservation', 'unknown', 'uuid', 'catalog', 'extra', 'no-end'])
def test_partial_or_invalid_raw_snapshot_refuses(kind):
    m = module(); raw = response(m); lines = raw.splitlines()
    if kind == 'missing': lines.pop(1)
    elif kind == 'duplicate': lines.insert(2, lines[1])
    elif kind == 'foreign': lines[1] = lines[1].replace(next(iter(m.SOURCES)), 'foreign')
    elif kind in ('negative', 'bool', 'fraction', 'overflow'):
        cells = lines[1].split('\t'); cells[1] = {'negative':'-1','bool':'True','fraction':'1.0','overflow':str(2**63)}[kind]
        lines[1] = '\t'.join(cells)
    elif kind in ('conservation', 'unknown'):
        cells = lines[1].split('\t'); cells[1] = '1'
        if kind == 'unknown': cells[5] = '1'
        lines[1] = '\t'.join(cells)
    elif kind == 'uuid': lines[-1] = lines[-1].replace(UUID, '00000000-0000-0000-0000-000000000002')
    elif kind == 'catalog': lines[-1] = lines[-1].replace('owned_probe', 'foreign')
    elif kind == 'extra': lines.append('unexpected')
    elif kind == 'no-end': lines.pop()
    with pytest.raises(snapshot.FormalSnapshotError):
        m.parse_snapshot('\n'.join(lines) + '\n', 'owned_probe', UUID)


class BoundaryMySql:
    container = 'c' * 64
    def __init__(self, m, *, issue=None): self.m=m; self.issue=issue; self.calls=[]
    def query(self, sql):
        self.calls.append(sql)
        if sql.startswith('SHOW TABLES'): return '\n'.join(s['table'] for s in self.m.SOURCES.values())+'\n'
        if sql.startswith('DESCRIBE'):
            table = sql.split('`')[-2]
            source = next(s for s in self.m.SOURCES.values() if s['table'] == table)
            columns = list(source['columns'])
            if self.issue == 'column': columns.remove('tenant_id')
            return ''.join(c+'\tvarchar(64)\tYES\t\tNULL\t\n' for c in columns)
        if 'information_schema.TABLES' in sql:
            return ''.join(s['table']+'\t'+('MyISAM' if self.issue=='engine' else 'InnoDB')+'\n' for s in self.m.SOURCES.values())
        if self.issue == 'query': raise snapshot.FormalSnapshotError('FORMAL_MYSQL_QUERY_TIMEOUT')
        return response(self.m)


@pytest.mark.parametrize('issue', ['column', 'engine', 'query'])
def test_missing_schema_or_query_failure_never_returns_partial_result(issue):
    m=module(); mysql=BoundaryMySql(m,issue=issue)
    with pytest.raises(snapshot.FormalSnapshotError):
        m.observe_selected_work(mysql, 'owned_probe', UUID, guard=lambda:None, seconds=10)


def test_last_callback_schema_drift_refuses():
    m=module(); mysql=BoundaryMySql(m); calls=[]
    def guard():
        calls.append(1)
        if len(calls)==3: mysql.issue='column'
    with pytest.raises(snapshot.FormalSnapshotError):
        m.observe_selected_work(mysql, 'owned_probe', UUID, guard=guard, seconds=10)


def test_final_schema_query_budget_exhausted():
    m=module(); mysql=BoundaryMySql(m); actual=mysql.query; calls=[]
    def query(sql):
        raw=actual(sql)
        if 'information_schema.TABLES' in sql:
            calls.append(1)
            if len(calls)==2: snapshot.observation_deadline.set(time.monotonic()-1)
        return raw
    mysql.query=query
    with pytest.raises(snapshot.FormalSnapshotError,match='FORMAL_DATA_DEADLINE'):
        m.observe_selected_work(mysql, 'owned_probe', UUID, guard=lambda:None, seconds=10)
    assert calls==[1,1]


def test_actual_capture_cli_has_explicit_v9_route(tmp_path, monkeypatch, capsys):
    import formal_capture_producer as capture
    request={'schemaVersion':'release-formal-capture-request/v9','action':'observe-selected-persisted-work'}
    path=tmp_path/'request.json'; path.write_text(json.dumps(request)); path.chmod(0o600)
    m=module(); calls=[]
    monkeypatch.setattr(capture,'require_native_runtime',lambda:None)
    monkeypatch.setattr(m,'run_approved_persisted_observation',lambda p,d: calls.append((p,d)) or {'releaseReady':False})
    digest=hashlib.sha256(path.read_bytes()).hexdigest()
    assert capture.main(['--request',str(path),'--expected-request-sha256',digest])==0
    assert calls==[(str(path),digest)] and json.loads(capsys.readouterr().out)=={'releaseReady':False}
