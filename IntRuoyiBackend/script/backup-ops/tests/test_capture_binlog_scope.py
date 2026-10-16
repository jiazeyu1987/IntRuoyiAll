"""Local event-policy checks; not MySQL/native/NAS capture acceptance."""
from pathlib import Path
import sys
import time
from types import SimpleNamespace

import pytest

sys.path.insert(0, str(Path(__file__).parents[1] / 'linux'))
import formal_binlog_scope as scope
import formal_snapshot_linux as snapshot
import formal_capture_incremental as capture
from formal_capture_budget import active_budget


def transaction(database='business'):
    return [('Gtid', 'SET @@SESSION.GTID_NEXT= uuid:1'),
            ('Query', 'use `business`; BEGIN'),
            ('Table_map', f'table_id: 42 ({database}.orders)'),
            ('Write_rows', 'table_id: 42 flags: STMT_END_F'),
            ('Xid', 'COMMIT /* xid=11 */')]


def inspect_events(events):
    state = scope.RowEventScope('business')
    for kind, info in events:
        state.observe(kind, info)
    state.complete()
    return state


def test_actual_table_mapping_scopes_all_row_effects():
    events = transaction()
    events.insert(4, ('Update_rows', 'table_id: 42'))
    events.insert(5, ('Delete_rows', 'table_id: 42 flags: STMT_END_F'))
    state = inspect_events(events)
    assert state.row_count == 3 and state.event_count == 7


@pytest.mark.parametrize('database', ['removed_database', 'mysql', 'sys', 'other_business'])
def test_historical_foreign_or_system_table_refuses_even_if_database_is_now_absent(database):
    with pytest.raises(snapshot.FormalSnapshotError, match='DATABASE_OUT_OF_SCOPE'):
        inspect_events(transaction(database))


@pytest.mark.parametrize('sql', [
    'CREATE DATABASE removed_database', 'DROP DATABASE removed_database',
    'INSERT INTO removed_database.orders VALUES (1)',
    "CREATE USER 'new_user'@'%' IDENTIFIED BY 'not-an-actual-credential'",
    "GRANT ALL ON *.* TO 'new_user'@'%'", "REVOKE ALL ON *.* FROM 'new_user'@'%'",
    'SET GLOBAL read_only=OFF', 'CREATE TABLE orders (id INT)',
    'BEGIN; DELETE FROM mysql.user', 'BEGIN /* hidden statement */',
])
def test_all_unproved_statement_or_authority_effects_refuse(sql):
    with pytest.raises(snapshot.FormalSnapshotError, match='QUERY_SCOPE_UNPROVEN'):
        inspect_events([('Gtid', 'gtid'), ('Query', 'use `business`; ' + sql)])


@pytest.mark.parametrize('kind', ['Transaction_payload', 'Anonymous_Gtid', 'Execute_load_query',
                                  'User_var', 'Unknown'])
def test_hidden_or_unknown_event_scope_refuses(kind):
    with pytest.raises(snapshot.FormalSnapshotError, match='EVENT_SCOPE_UNPROVEN'):
        inspect_events([(kind, 'unproved event')])


def test_rows_require_this_transaction_original_table_map():
    with pytest.raises(snapshot.FormalSnapshotError, match='SCOPE_UNPROVEN'):
        inspect_events(transaction() + [('Gtid', 'gtid'), ('Query', 'BEGIN'),
                                       ('Write_rows', 'table_id: 42')])


def test_incomplete_transaction_refuses():
    with pytest.raises(snapshot.FormalSnapshotError, match='TRANSACTION_INCOMPLETE'):
        inspect_events(transaction()[:-1])


def test_nonexecuting_rows_annotation_still_requires_actual_scope():
    events = transaction()
    events.insert(2, ('Rows_query', 'INSERT INTO business.orders VALUES (1)'))
    assert inspect_events(events).row_count == 1
    events[3] = ('Table_map', 'table_id: 42 (mysql.user)')
    with pytest.raises(snapshot.FormalSnapshotError, match='DATABASE_OUT_OF_SCOPE'):
        inspect_events(events)


def test_paged_exact_positions_and_final_boundary():
    events = transaction() * 60
    rows = [f'mysql-bin.000001\t{4+i*100}\t{kind}\t7\t{104+i*100}\t{info}'
            for i, (kind, info) in enumerate(events)]
    calls = []
    class EventRows:
        # Explicit local input fixture for the server row protocol, no DB calls.
        def query(self, query):
            calls.append(query)
            start = int(query.split(' FROM ')[1].split(' ')[0])
            offset = (start - 4) // 100
            return '\n'.join(rows[offset:offset+256]) + '\n'
    segment = dict(file='mysql-bin.000001', startPosition=4, stopPosition=30004)
    proof = scope.inspect_binlog_scope(EventRows(), [segment], 'business',
                                      guard=lambda: None, deadline=time.monotonic()+10)
    assert len(calls) == 2 and 'FROM 25604 LIMIT 256;' in calls[1]
    assert proof['segments'][0]['eventCount'] == 300
    assert proof['segments'][0]['rowEventCount'] == 60
    assert proof['databaseScope'] == ['business']


@pytest.mark.parametrize('row', [
    'mysql-bin.000001\t5\tGtid\t7\t100\tgtid',
    'mysql-bin.000001\t4\tGtid\t7\t999\tgtid',
    'mysql-bin.000002\t4\tGtid\t7\t100\tgtid',
    'mysql-bin.000001\t4\tGtid\t7\t4\tgtid', '',
])
def test_missing_overlapping_foreign_file_or_truncated_range_refuses(row):
    class EventRows:
        def query(self, query): return row
    segment = dict(file='mysql-bin.000001', startPosition=4, stopPosition=100)
    with pytest.raises(snapshot.FormalSnapshotError):
        scope.inspect_binlog_scope(EventRows(), [segment], 'business',
                                   guard=lambda: None, deadline=time.monotonic()+10)


def test_revoked_or_expired_scope_guard_precedes_event_query():
    class NoRead:
        def query(self, query): raise AssertionError('query must not start')
    segment = dict(file='mysql-bin.000001', startPosition=4, stopPosition=100)
    with pytest.raises(snapshot.FormalSnapshotError, match='SCOPE_DEADLINE'):
        scope.inspect_binlog_scope(NoRead(), [segment], 'business',
                                   guard=lambda: None, deadline=time.monotonic()-1)


def local_series(monkeypatch, point_type):
    # Isolated unit input: no approved owner, real DB or runtime claim.
    series = object.__new__(capture.CaptureSeries)
    series.spec = dict(pointType=point_type)
    series.check = lambda: None
    series.binlog_database_scope = ['business']
    series.parent = dict(databaseEnd=dict(file='mysql-bin.000001', position=4,
        databaseUuid='11111111-1111-1111-1111-111111111111',
        serverVersion='8.0.35', executedGtidSet=''))
    current = {**series.parent['databaseEnd'], 'position': 504}
    monkeypatch.setattr(capture, 'position', lambda mysql, *, guard: current)
    return series


def test_full_capture_forwards_original_remaining_admission(monkeypatch,tmp_path):
    series = local_series(monkeypatch, 'FULL')
    outer = time.monotonic()+10; budget = outer+10; calls = []
    class MysqlInput:
        timeout = 5
        def dump_until(self, database, destination, *, guard, deadline):
            calls.append((database, destination, guard, deadline))
            return dict(bytes=1, sha256='a'*64)
    token = active_budget.set(SimpleNamespace(deadline=budget))
    observation = snapshot.observation_deadline.set(outer)
    guard = lambda: None
    try:
        result = series.database(MysqlInput(), 'business', tmp_path/'sql.gz', guard=guard)
        assert calls == [('business', tmp_path/'sql.gz', guard, outer)]
        assert result['bytes'] == 1 and series.database_delta == dict(kind='SQL_FULL',segments=[])
    finally:
        snapshot.observation_deadline.reset(observation); active_budget.reset(token)


@pytest.mark.parametrize('event_type,info', [
    ('Query', "CREATE USER 'new_user'@'%'"),
    ('Query', "GRANT ALL ON *.* TO 'new_user'@'%'"),
    ('Table_map', 'table_id: 42 (removed_database.orders)'),
])
def test_capture_rejects_actual_historical_scope_before_executable_export(
        monkeypatch,tmp_path,event_type,info):
    series = local_series(monkeypatch, 'INCREMENTAL')
    class MysqlInput:
        def query(self, query):
            if query == 'SHOW BINARY LOGS;':return 'mysql-bin.000001\t504\tNo\n'
            events = [('Gtid','gtid'), ('Query','BEGIN'), (event_type,info)]
            return '\n'.join(f'mysql-bin.000001\t{4+i*100}\t{kind}\t7\t{104+i*100}\t{text}'
                for i,(kind,text) in enumerate(events))
    def forbidden_export(*args,**kwargs):raise AssertionError('SQL export must not start')
    monkeypatch.setattr(capture.subprocess,'Popen',forbidden_export)
    deadline = time.monotonic()+10
    token = active_budget.set(SimpleNamespace(deadline=deadline))
    observation = snapshot.observation_deadline.set(deadline)
    try:
        with pytest.raises(snapshot.FormalSnapshotError, match='QUERY_SCOPE_UNPROVEN|DATABASE_OUT_OF_SCOPE'):
            series.database(MysqlInput(),'business',tmp_path/'sql.gz',guard=lambda:None)
        assert not list(tmp_path.iterdir())
    finally:
        snapshot.observation_deadline.reset(observation); active_budget.reset(token)
