"""Exact query/proof contracts. SQL transport is explicit; not formal capture or E2E."""
import copy
import hashlib
import importlib
from pathlib import Path
import sys

import pytest

sys.path.insert(0, str(Path(__file__).parents[1] / 'linux'))
from formal_snapshot_linux import FormalSnapshotError, _canonical_bytes
from test_formal_data_evidence import contract_observation

UUID = '11111111-1111-1111-1111-111111111111'
TABLE = 'infra_release_migration'
KINDS = {
    'id': 'bigint', 'release_tag': 'varchar(128)', 'target_environment': 'varchar(32)',
    'status': 'varchar(32)', 'started_at': 'datetime', 'finished_at': 'datetime',
    'error_message': 'text', 'operation_id': 'varchar(128)', 'creator': 'varchar(64)',
    'create_time': 'datetime', 'updater': 'varchar(64)', 'update_time': 'datetime',
    'deleted': 'bit(1)', 'tenant_id': 'bigint', 'migration_id': 'varchar(191)',
    'file_name': 'varchar(512)', 'sha256': 'varchar(64)',
}


def module():
    return importlib.import_module('formal_source_migration_ledger')


def refresh(value, field):
    value[field] = hashlib.sha256(_canonical_bytes({k: v for k, v in value.items() if k != field})).hexdigest()


def fixture():
    rows = [
        ['1', 'prod', '001', 'sql/001.sql', 'a' * 64, 'APPLIED', '0', 'release-a', 'op-a', 'b' * 64],
        ['2', 'test', '001', 'sql/001.sql', 'a' * 64, 'FAILED', '0', 'release-b', 'op-b', 'c' * 64],
        ['3', 'backup', '001', 'sql/001.sql', 'd' * 64, 'APPLIED', '1', 'release-c', 'op-c', 'e' * 64],
    ]
    columns = [[TABLE, name, str(i + 1), kind.split('(')[0], kind,
                'utf8mb4' if kind.startswith(('varchar', 'text')) else None,
                'utf8mb4_unicode_ci' if kind.startswith(('varchar', 'text')) else None,
                'NO', None, '', ''] for i, (name, kind) in enumerate(KINDS.items())]
    proof = contract_observation('a' * 64, UUID)
    proof['schema']['catalog']['TABLES'].append([TABLE, 'BASE TABLE'])
    proof['schema']['catalog']['COLUMNS'].extend(columns)
    # Every declared physical table has its own complete native-shaped DDL.
    # The schema comparator must never authorize a catalog-only placeholder.
    proof['schema']['definitions']['TABLE'][TABLE]=[TABLE,
        'CREATE TABLE `'+TABLE+'` (\n'+',\n'.join(
            '  `'+c[1]+'` '+c[4]+(' COLLATE '+c[6] if c[6] else '')+' NOT NULL'
            for c in columns)+'\n) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci']
    proof['schema']['objectCounts']['TABLE']=2
    multiset = hashlib.sha256(b'row-multiset/v1\n')
    for token in sorted(hashlib.sha256(b'complete-row/v1:' + row[-1].encode() + b'\n').digest() for row in rows):
        multiset.update(token)
    proof['tables'][TABLE] = {'rows': len(rows), 'jsonNodes': 0, 'multisetSha256': multiset.hexdigest()}
    refresh(proof['schema'], 'schemaSha256')
    # schemaSha256 intentionally excludes the observation identity and outer observation digest.
    proof['schema']['schemaSha256'] = hashlib.sha256(_canonical_bytes({k: v for k, v in proof['schema'].items()
        if k not in ('schemaSha256', 'observationIdentity', 'observationSha256')})).hexdigest()
    refresh(proof['schema'], 'observationSha256')
    refresh(proof, 'observationSha256')
    # Native DESCRIBE is untyped; metadata decoding is the typed authority.
    describe = [[c[1], c[4], c[7], '', 'NULL' if c[8] is None else c[8], c[9]] for c in columns]
    calls = []

    class Client:
        container = 'a' * 64

        def schema_query(self, sql, **kwargs):
            calls.append(sql)
            assert kwargs['database'] == 'owned_probe' and kwargs['expected_uuid'] == UUID
            if sql == 'DESCRIBE `infra_release_migration`;':
                return copy.deepcopy(describe)
            if ' FROM information_schema.COLUMNS ' in sql:
                assert 'TABLE_SCHEMA=DATABASE()' in sql
                return [['N' if cell is None else 'V'+cell.encode('utf-8').hex().upper()
                    for cell in row] for row in self.column_metadata]
            assert sql.startswith('SELECT ') and ' FROM `infra_release_migration`' in sql
            assert 'WHERE' not in sql and 'LIMIT' not in sql
            assert all('`' + name + '`' in sql for name in KINDS), 'Every field participates in the typed row digest'
            return copy.deepcopy(rows)

    client=Client()
    client.column_metadata=copy.deepcopy(columns)
    return client, proof, rows, describe, calls


def observe(client, proof, guard=lambda: None):
    return module().observe_migration_ledger(client, 'owned_probe', UUID,
        source_environment='prod', full_data=proof, guard=guard, max_rows=1000)


def test_complete_projection_is_described_and_binds_every_physical_row():
    client, proof, rows, _, calls = fixture()
    guarded = []
    result = observe(client, proof, lambda: guarded.append(True))
    assert calls[0] == 'DESCRIBE `infra_release_migration`;' and len(calls) == 3
    assert ' FROM information_schema.COLUMNS ' in calls[1]
    assert len(guarded) >= 4
    assert result['sourceEnvironment'] == 'prod'
    assert result['observationIdentity'] == proof['observationIdentity']
    assert result['tableMultiset'] == proof['tables'][TABLE]
    assert result['fullDataObservationSha256'] == proof['observationSha256']
    assert [(r['id'], r['targetEnvironment'], r['status'], r['deleted']) for r in result['rows']] == [
        (1, 'prod', 'APPLIED', False), (2, 'test', 'FAILED', False), (3, 'backup', 'APPLIED', True)]
    assert all(r['fullRowSha256'] == hashlib.sha256(
        b'complete-row/v1:' + original[-1].encode() + b'\n').hexdigest()
        for r, original in zip(result['rows'], rows))
    assert result['applicationVerified'] is False and result['productionEvidence'] is False


@pytest.mark.parametrize('default', [None,'NULL','CURRENT_TIMESTAMP'])
def test_native_describe_default_uses_fresh_typed_metadata(default):
    client,proof,_,describe,calls=fixture()
    column=next(c for c in proof['schema']['catalog']['COLUMNS'] if c[0]==TABLE and c[1]=='started_at')
    column[8]=default
    client.column_metadata=copy.deepcopy([c for c in proof['schema']['catalog']['COLUMNS'] if c[0]==TABLE])
    next(d for d in describe if d[0]=='started_at')[4]='NULL' if default is None else default
    proof['schema']['schemaSha256']=hashlib.sha256(_canonical_bytes({k:v for k,v in proof['schema'].items()
        if k not in ('schemaSha256','observationIdentity','observationSha256')})).hexdigest()
    refresh(proof['schema'],'observationSha256');refresh(proof,'observationSha256')
    result=observe(client,proof)
    assert result['fullDataObservationSha256']==proof['observationSha256']
    assert len(calls)==3 and ' FROM information_schema.COLUMNS ' in calls[1]


@pytest.mark.parametrize('field,replacement', [(8,'NULL'),(9,'DEFAULT_GENERATED'),(10,'1'),
    (2,'19'),(7,'YES'),(4,'bigint unsigned'),(5,'ascii'),(6,'ascii_bin')])
def test_native_describe_typed_default_or_schema_drift_refuses_before_rows(field,replacement):
    client,proof,_,_,calls=fixture()
    client.column_metadata[0][field]=replacement
    with pytest.raises(FormalSnapshotError,match='FORMAL_SOURCE_LEDGER_SCHEMA_CHANGED'):
        observe(client,proof)
    assert len(calls)==2 and ' FROM information_schema.COLUMNS ' in calls[1]


@pytest.mark.parametrize('fault', ['missing-table', 'view', 'missing-column', 'extra-column',
    'column-type', 'describe', 'duplicate-id', 'duplicate-environment-key', 'bad-sha', 'bad-token',
    'row-count', 'multiset', 'json-nodes', 'wrong-cid', 'wrong-uuid', 'extra-row', 'bool-limit'])
def test_incomplete_or_changed_source_never_becomes_an_empty_or_applied_ledger(fault):
    client, proof, rows, describe, calls = fixture()
    if fault == 'missing-table':
        del proof['tables'][TABLE]
        proof['schema']['catalog']['TABLES'].pop()
    if fault == 'view': proof['schema']['catalog']['TABLES'][-1][1] = 'VIEW'
    if fault == 'missing-column': proof['schema']['catalog']['COLUMNS'].pop()
    if fault == 'extra-column': proof['schema']['catalog']['COLUMNS'].append([TABLE, 'unapproved', '18', 'int', 'int', None, None, 'NO', None, '', ''])
    if fault == 'column-type': proof['schema']['catalog']['COLUMNS'][-1][4] = 'varchar(512)'
    if fault == 'describe': describe[-1][0] = 'wrong'
    if fault == 'duplicate-id': rows[1][0] = rows[0][0]
    if fault == 'duplicate-environment-key': rows[1][1] = rows[0][1]
    if fault == 'bad-sha': rows[0][4] = 'bad'
    if fault == 'bad-token': rows[0][-1] = 'bad'
    if fault == 'row-count': proof['tables'][TABLE]['rows'] += 1
    if fault == 'multiset': proof['tables'][TABLE]['multisetSha256'] = 'f' * 64
    if fault == 'json-nodes': proof['tables'][TABLE]['jsonNodes'] = 1
    if fault == 'wrong-cid': client.container = 'f' * 64
    if fault == 'wrong-uuid': proof['observationIdentity']['databaseUuid'] = '22222222-2222-2222-2222-222222222222'
    if fault == 'extra-row': rows.append(rows[-1][:])
    with pytest.raises(FormalSnapshotError, match='FORMAL_SOURCE_LEDGER_'):
        module().observe_migration_ledger(client, 'owned_probe', UUID, source_environment='prod',
            full_data=proof, guard=lambda: None, max_rows=True if fault == 'bool-limit' else 1000)
    if fault == 'missing-table': assert not calls


def test_same_physical_rows_reordered_have_one_durable_projection():
    client, proof, rows, _, _ = fixture()
    original = observe(client, proof)
    rows.reverse()
    assert observe(client, proof) == original


@pytest.mark.parametrize('environment', ['', None, True, 'production', 'test', 'backup'])
def test_source_environment_is_explicit_formal_authority(environment):
    client, proof, _, _, calls = fixture()
    with pytest.raises(FormalSnapshotError, match='FORMAL_SOURCE_LEDGER_'):
        module().observe_migration_ledger(client, 'owned_probe', UUID, source_environment=environment,
            full_data=proof, guard=lambda: None, max_rows=1000)
    assert not calls


def test_guard_failure_is_not_hidden_or_given_a_success_digest():
    client, proof, _, _, calls = fixture()
    def guard(): raise FormalSnapshotError('FORMAL_OWNER_LOST')
    with pytest.raises(FormalSnapshotError, match='FORMAL_OWNER_LOST'): observe(client, proof, guard)
    assert not calls


@pytest.mark.parametrize('fault', ['none', 'after-changed', 'claim-uuid', 'sql-sha', 'source-scope', 'source-fingerprint', 'extra-ledger-field'])
def test_binding_keeps_exact_before_after_claim_and_dump(fault):
    client, proof, _, _, _ = fixture()
    before = observe(client, proof)
    after = copy.deepcopy(before)
    claim = {'operationId': 'owned-source-ledger', 'sourceFingerprint': 'c' * 64,
             'databaseUuid': UUID, 'minioContainerId': 'e' * 64}
    scope = {'schemaVersion': 'release-formal-approved-source/v1', 'environment': 'prod',
             'targetRawSha256': 'd' * 64, 'targetFingerprint': 'c' * 64,
             'database': 'owned_probe', 'databaseUuid': UUID}
    sql = 'b' * 64
    if fault == 'after-changed': after['rows'][0]['status'] = 'FAILED'
    if fault == 'claim-uuid': claim['databaseUuid'] = '22222222-2222-2222-2222-222222222222'
    if fault == 'sql-sha': sql = 'bad'
    if fault == 'source-scope': scope['environment'] = 'test'
    if fault == 'source-fingerprint': claim['sourceFingerprint'] = 'f'*64
    if fault == 'extra-ledger-field': before['success'] = True
    if fault == 'none':
        result = module().bind_migration_ledger(_canonical_bytes(claim), before, after, sql, scope)
        assert result['sourceScope'] == scope and result['sqlSha256'] == sql
        assert result['claimBindingSha256'] == hashlib.sha256(_canonical_bytes(claim)).hexdigest()
        assert result['sourceBeforeFileSha256'] == hashlib.sha256(_canonical_bytes(before)).hexdigest()
        assert result['sourceAfterFileSha256'] == hashlib.sha256(_canonical_bytes(after)).hexdigest()
        assert result['ledgerObservationSha256'] == before['observationSha256']
    else:
        with pytest.raises(FormalSnapshotError, match='FORMAL_SOURCE_LEDGER_'):
            module().bind_migration_ledger(_canonical_bytes(claim), before, after, sql, scope)
