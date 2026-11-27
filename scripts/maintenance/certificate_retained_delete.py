"""Fixed-input, audited disposal of explicitly approved certificates in retired tenants.

No tenant restoration, product matching, approval creation, version transition, file
operation, migration-ledger update or service restart is performed by this program.
The private bundle contains the approved IDs and captured evidence; never commit it.
"""
import argparse
import hashlib
import json
import os
from pathlib import Path
import re
import subprocess
import sys

if not __debug__:
    raise RuntimeError('Python optimization is prohibited: maintenance assertions must remain enabled')

SCOPE_SHA = '5942519b80ca65951f64d2e027906226486321adf044174b11c7639962739157'
MASTER = 'dcc_registration_certificate'
AUDIT = MASTER + '_audit'
PREFIX = 'ops-cert-disposal-20261127-v1:'


def digest(raw):
    return hashlib.sha256(raw).hexdigest()


def canonical(value):
    return json.dumps(value, ensure_ascii=False, sort_keys=True, separators=(',', ':'))


def literal(value):
    if value is None:
        return 'NULL'
    if isinstance(value, (int, float)):
        return str(value)
    return "CONVERT(X'" + str(value).encode('utf8').hex() + "' USING utf8mb4)"


def rows(raw):
    return [json.loads(line) for line in raw.splitlines()]


def expressions(baseline):
    columns = {}
    for line in baseline['columns'].splitlines():
        table, name, typ, extra = line.split('\t')
        columns.setdefault(table, []).append((name, typ, extra))
    return {table: 'JSON_OBJECT(' + ','.join(literal(n) + ',' +
            ('CAST(`' + n + '` AS UNSIGNED)' if typ.startswith('bit') else '`' + n + '`')
            for n, typ, extra in columns[table]) + ')' for table in baseline['ddl']}


def event_key(environment, certificate_id):
    return PREFIX + environment + ':' + str(certificate_id)


def detail(environment, certificate):
    return dict(commandKind='CERTIFICATE_DISPOSAL', operationType='RETIRED_TENANT_CERTIFICATE_DISPOSAL',
                actorPrincipal='SYSTEM_MAINTENANCE', businessApprovalCreated=False,
                authorization='Human confirmed both exact batches; retain audit and original attachments',
                scopeSha256=SCOPE_SHA, environment=environment,
                expectedRowVersion=certificate['row_version'],
                retainedPendingVersionId=certificate['pending_version_id'])


def load_bundle(bundle, environment):
    bundle = Path(bundle)
    manifest = json.loads((bundle / 'manifest.json').read_bytes())
    for name, checksum in manifest['files'].items():
        assert Path(name).name == name and digest((bundle / name).read_bytes()) == checksum, 'Bundle checksum mismatch'
    assert digest(Path(__file__).read_bytes()) == manifest['files']['certificate_retained_delete.py'], 'Execute the sealed worker'
    assert digest((bundle / 'scope.json').read_bytes()) == SCOPE_SHA, 'Unapproved scope'
    scope = json.loads((bundle / 'scope.json').read_bytes())[environment]
    assert scope['tenantId'] == {'prod': 163, 'test': 169}[environment]
    assert len(set(scope['ids'])) == len(scope['ids']) == {'prod': 161, 'test': 165}[environment]
    baseline = json.loads((bundle / (environment + '-full-baseline.private.json')).read_bytes())
    sources = json.loads((bundle / (environment + '-migration-sources.private.json')).read_bytes())
    assert sources['identity'] == baseline['identity'], 'Source evidence database changed'
    for table in sources['ddl']:
        assert table not in baseline['ddl'], 'Duplicate evidence table'
        baseline[table] = sources[table]
    baseline['ddl'].update(sources['ddl'])
    config = json.loads((bundle / 'profiles.private.json').read_bytes())[environment]
    assert config['host'] == {'prod': '172.30.30.57', 'test': '172.30.30.58'}[environment]
    for role in ('database', 'backend', 'frontend'):
        assert config[role] == baseline['components'][role]['name']
    return bundle, scope, baseline, config


class Database:
    def __init__(self, profile):
        self.profile = profile

    def inspect(self, name):
        return json.loads(subprocess.check_output(['docker', 'inspect', name]))[0]

    def command(self):
        environment = dict(item.split('=', 1) for item in self.inspect(self.profile['database'])['Config']['Env'])
        assert environment.get('MYSQL_DATABASE'), 'Explicit database required'
        if 'MYSQL_ROOT_PASSWORD_FILE' in environment:
            auth = 'export MYSQL_PWD="$(cat "$MYSQL_ROOT_PASSWORD_FILE")"; '
        else:
            assert environment.get('MYSQL_ROOT_PASSWORD'), 'Root credential source missing'
            auth = 'export MYSQL_PWD="$MYSQL_ROOT_PASSWORD"; '
        return ['docker', 'exec', '-i', self.profile['database'], 'sh', '-c', auth +
                'exec mysql -uroot --default-character-set=utf8mb4 --batch --raw --unbuffered --skip-column-names "$MYSQL_DATABASE"']

    def query(self, statement):
        result = subprocess.run(self.command(), input=statement.encode(), stdout=subprocess.PIPE, stderr=subprocess.PIPE)
        if result.returncode:
            raise RuntimeError('MySQL operation failed; keep writers stopped and inspect private diagnostics')
        return result.stdout.decode('utf8').rstrip('\r\n')


def capture(db, baseline):
    expr = expressions(baseline)
    queries = {'identity': 'SELECT DATABASE(),@@server_uuid,@@version;'}
    for table, expression in expr.items():
        queries[table] = 'SELECT ' + expression + ' FROM `' + table + '`;'
    raw = db.query('START TRANSACTION READ ONLY;\n' + '\n'.join(
        "SELECT 'RESULT_" + str(i) + "';\n" + statement for i, statement in enumerate(queries.values())) + '\nROLLBACK;')
    captured = {key: [] for key in queries}
    key = None
    for line in raw.splitlines():
        if line.startswith('RESULT_'):
            key = list(queries)[int(line[7:])]
        else:
            captured[key].append(line)
    return {k: '\n'.join(v) for k, v in captured.items()}


def validate_contract(db, baseline, bundle):
    expected = {x['name']: x for x in rows(baseline['triggers'])}
    current = rows(db.query("SELECT JSON_OBJECT('name',TRIGGER_NAME,'table',EVENT_OBJECT_TABLE,'body',ACTION_STATEMENT,'event',EVENT_MANIPULATION,'timing',ACTION_TIMING,'sqlMode',SQL_MODE) FROM information_schema.TRIGGERS WHERE TRIGGER_SCHEMA=DATABASE() AND EVENT_OBJECT_TABLE LIKE 'dcc_registration_certificate%' ORDER BY TRIGGER_NAME;"))
    assert {x['name'] for x in current} == set(expected), 'Certificate trigger inventory changed'
    allowed = json.loads((bundle / 'retained-trigger-metadata.json').read_bytes())['acceptedBodySha256']
    for trigger in current:
        if trigger['name'] == 'trg_dcc_reg_cert_master_immutable_bu':
            assert trigger['table'] == MASTER and trigger['event'] == 'UPDATE' and trigger['timing'] == 'BEFORE'
            assert digest(re.sub(r'[\s`]+', '', trigger['body']).encode()) in allowed, 'Unknown master trigger'
        else:
            assert trigger == expected[trigger['name']], 'Historical evidence protection changed'


def certificates(baseline, scope):
    result = [x for x in rows(baseline[MASTER]) if x['tenant_id'] == scope['tenantId'] and x['id'] in scope['ids']]
    assert len(result) == len(scope['ids']) and all(x['deleted'] == 0 for x in result), 'Scope baseline mismatch'
    return result


def compare_state(current, baseline, scope, environment):
    assert current['identity'] == baseline['identity'], 'Database identity changed'
    approved = certificates(baseline, scope)
    expected = {x['id']: x for x in approved}
    tenant = next(x for x in rows(current['system_tenant']) if x['id'] == scope['tenantId'])
    assert tenant['deleted'] == 1, 'Tenant must remain deleted'
    old_audit = rows(baseline[AUDIT])
    new_audit = [x for x in rows(current[AUDIT]) if x['event_key'].startswith(PREFIX)]
    other_audit = [x for x in rows(current[AUDIT]) if not x['event_key'].startswith(PREFIX)]
    assert sorted(map(canonical, other_audit)) == sorted(map(canonical, old_audit)), 'Existing audit changed'
    disposition = {}
    for record in new_audit:
        cid = record['certificate_id']
        assert cid in expected and cid not in disposition and record['tenant_id'] == scope['tenantId'], 'Unexpected disposal audit'
        cert = expected[cid]
        assert record['event_key'] == event_key(environment, cid)
        assert record['event_type'] == 'CERTIFICATE_DISPOSAL_SUCCEEDED' and record['actor_id'] == 0
        assert record['result'] == 'SUCCESS' and record['result_code'] == 'OK'
        assert record['owner_company_id'] == cert['owner_company_id']
        assert record['version_id'] == cert['current_version_id'] and record['snapshot_id'] == cert['current_snapshot_id']
        assert record['detail_json'] == detail(environment, cert), 'Disposal detail mismatch'
        disposition[cid] = record
    masters = {x['id']: x for x in rows(current[MASTER]) if x['tenant_id'] == scope['tenantId'] and x['id'] in expected}
    assert len(masters) == len(expected), 'Approved master missing'
    deleted = sum(x['deleted'] == 1 for x in masters.values())
    assert deleted in (0, len(expected)), 'Partial disposal state'
    completed = deleted == len(expected)
    assert len(disposition) == (len(expected) if completed else 0), 'Audit/master state mismatch'
    for cid, old in expected.items():
        new = dict(masters[cid])
        wanted = dict(old)
        if completed:
            wanted.update(deleted=1, row_version=old['row_version'] + 1, updater='0')
            assert new['update_time'] and new['update_time'] >= old['update_time']
            wanted['update_time'] = new['update_time']
        assert new == wanted, 'Approved master changed beyond disposal fields'
    outside = lambda raw: [x for x in rows(raw) if not(x['tenant_id'] == scope['tenantId'] and x['id'] in expected)]
    assert sorted(map(canonical, outside(current[MASTER]))) == sorted(map(canonical, outside(baseline[MASTER]))), 'Outside master changed'
    for table in baseline['ddl']:
        if table not in (MASTER, AUDIT):
            assert sorted(map(canonical, rows(current[table]))) == sorted(map(canonical, rows(baseline[table]))), 'Retained evidence changed: ' + table
    # These online processes must be resolved through their normal lifecycle; this
    # maintenance operation does not approve/cancel them. Both approved batches have none.
    for table, status in ((MASTER + '_change', {'PENDING_APPROVAL'}), (MASTER + '_access_request', {'SUBMITTED', 'BPM_BOUND'})):
        assert not any(x['tenant_id'] == scope['tenantId'] and x['certificate_id'] in expected and x.get('deleted', 0) == 0
                       and x['status'] in status for x in rows(current[table])), 'Pending approval/upload requires separate resolution'
    for table, field in (('dcc_data_relation', 'registration_certificate_id'), ('dcc_registration_handling_task', 'certificate_id')):
        assert not any(x['tenant_id'] == scope['tenantId'] and x.get(field) in expected and x.get('deleted', 0) == 0
                       for x in rows(current[table])), 'Live downstream reference requires separate resolution'
    return 'COMPLETED' if completed else 'ORIGINAL'


def checks_sql(baseline, scope, environment):
    expr = expressions(baseline)
    def check(condition):
        return 'INSERT INTO disposal_assert VALUES((' + condition + ')); DELETE FROM disposal_assert;\n'
    before = check('(SELECT CONCAT(DATABASE(),CHAR(9),@@server_uuid,CHAR(9),@@version))=' + literal(baseline['identity']))
    for table, expression in expr.items():
        hashes = [digest(line.encode()) for line in baseline[table].splitlines()]
        condition = 'COUNT(*)=' + str(len(hashes))
        if hashes:
            condition += ' AND SUM(SHA2(CAST(' + expression + ' AS CHAR),256) IN (' + ','.join(literal(h) for h in hashes) + '))=' + str(len(hashes))
        before += check('(SELECT ' + condition + ' FROM `' + table + '`)')
    certs = certificates(baseline, scope)
    inserts = []
    updates = []
    for cert in certs:
        values = [cert['tenant_id'], cert['owner_company_id'], cert['id'], cert['current_version_id'], cert['current_snapshot_id'],
                  event_key(environment, cert['id']), 'CERTIFICATE_DISPOSAL_SUCCEEDED', 0, 'SUCCESS', 'OK',
                  PREFIX + environment, canonical(detail(environment, cert)), 'SYSTEM_MAINTENANCE']
        inserts.append('INSERT INTO ' + AUDIT + '(tenant_id,owner_company_id,certificate_id,version_id,snapshot_id,event_key,event_type,actor_id,result,result_code,request_trace_id,detail_json,creator,occurred_at) VALUES(' + ','.join(literal(v) for v in values) + ',CURRENT_TIMESTAMP);')
        updates.append('UPDATE ' + MASTER + ' SET deleted=1,row_version=row_version+1,updater=\'0\',update_time=CURRENT_TIMESTAMP WHERE tenant_id=' + str(cert['tenant_id']) + ' AND id=' + str(cert['id']) + ' AND row_version=' + str(cert['row_version']) + ' AND deleted=0;\n' + check('ROW_COUNT()=1'))
    # SQL assertions and row locks run in the same transaction as audit + soft deletion.
    statement = "CREATE TEMPORARY TABLE disposal_assert(ok TINYINT NOT NULL CHECK(ok=1));\n"
    statement += check("GET_LOCK('intruoyi:certificate-disposal',0)=1")
    statement += 'SET SESSION TRANSACTION ISOLATION LEVEL SERIALIZABLE; START TRANSACTION;\n'
    statement += 'SELECT id FROM ' + MASTER + ' ORDER BY id FOR UPDATE;\n'
    statement += 'SELECT id FROM system_tenant WHERE id=' + str(scope['tenantId']) + ' FOR UPDATE;\n'
    statement += before + '\n'.join(inserts + updates)
    # Every historical row and outside master is checked again before COMMIT.
    for table, expression in expr.items():
        if table == MASTER:
            for cert in rows(baseline[MASTER]):
                expected = json.loads(canonical(cert))
                approved = cert['tenant_id'] == scope['tenantId'] and cert['id'] in scope['ids']
                if approved:
                    expected.update(deleted=1, row_version=cert['row_version'] + 1, updater='0')
                    wanted = 'JSON_REMOVE(CAST(' + literal(canonical(expected)) + " AS JSON),'$.update_time')"
                    actual = 'JSON_REMOVE(' + expression + ",'$.update_time')"
                else:
                    actual = expression; wanted = 'CAST(' + literal(canonical(expected)) + ' AS JSON)'
                statement += check('(SELECT BINARY CAST(' + actual + ' AS CHAR)<=>BINARY CAST(' + wanted + ' AS CHAR) FROM ' + MASTER + ' WHERE id=' + str(cert['id']) + ' AND tenant_id=' + str(cert['tenant_id']) + ')')
            statement += check('(SELECT COUNT(*) FROM ' + MASTER + ')=' + str(len(rows(baseline[MASTER]))))
        else:
            hashes = [digest(line.encode()) for line in baseline[table].splitlines()]
            where = " WHERE event_key NOT LIKE '" + PREFIX + "%\'" if table == AUDIT else ''
            condition = 'COUNT(*)=' + str(len(hashes))
            if hashes:
                condition += ' AND SUM(SHA2(CAST(' + expression + ' AS CHAR),256) IN (' + ','.join(literal(h) for h in hashes) + '))=' + str(len(hashes))
            statement += check('(SELECT ' + condition + ' FROM `' + table + '`' + where + ')')
    statement += check('(SELECT COUNT(*) FROM ' + AUDIT + " WHERE event_key LIKE '" + PREFIX + "%')=" + str(len(certs)))
    statement += "COMMIT; SELECT RELEASE_LOCK('intruoyi:certificate-disposal'); SELECT 'DISPOSAL_COMMITTED';\n"
    return statement


def save(path, value):
    with open(path, 'x', encoding='utf8') as output:
        output.write(json.dumps(value, ensure_ascii=False, indent=2))
        output.flush(); os.fsync(output.fileno())


def stopped(db, baseline):
    for role in ('backend', 'frontend'):
        current = db.inspect(baseline['components'][role]['name'])
        assert current['Id'] == baseline['components'][role]['id'] and not current['State']['Running'], 'Stop the exact approved writers first'
    current = db.inspect(baseline['components']['database']['name'])
    assert current['Id'] == baseline['components']['database']['id'] and current['State']['Running'], 'Database container identity changed'
    assert db.query("SELECT COUNT(*) FROM information_schema.PROCESSLIST WHERE ID<>CONNECTION_ID() AND COMMAND NOT IN ('Sleep','Daemon');") == '0', 'Other active SQL exists'
    assert db.query('SELECT COUNT(*) FROM information_schema.INNODB_TRX;') == '0', 'Active database transaction exists'


def main():
    parser = argparse.ArgumentParser(__doc__)
    parser.add_argument('action', choices=('plan', 'apply', 'verify', 'resume'))
    parser.add_argument('--bundle', required=True)
    parser.add_argument('--environment', choices=('test', 'prod'), required=True)
    args = parser.parse_args()
    bundle, scope, baseline, profile = load_bundle(args.bundle, args.environment)
    db = Database(profile)
    validate_contract(db, baseline, bundle)
    current = capture(db, baseline)
    state = compare_state(current, baseline, scope, args.environment)
    directory = bundle / ('execution-' + args.environment)
    directory.mkdir(exist_ok=True)
    if args.action in ('plan', 'verify'):
        if args.action == 'verify' and list(directory.glob('attempt-*')):
            stopped(db, baseline)
            for path in directory.glob('attempt-*/session.json'):
                session = json.loads(path.read_bytes())
                assert db.query('SELECT COUNT(*) FROM information_schema.PROCESSLIST WHERE ID=' + str(session['connectionId']) + ';') == '0', 'Recorded SQL connection still exists; do not replay'
        report = dict(state=state, environment=args.environment, scopeSha256=SCOPE_SHA,
                      certificates=len(scope['ids']), pendingVersionsRetained=[x['pending_version_id'] for x in certificates(baseline, scope) if x['pending_version_id'] is not None],
                      identity=baseline['identity'], expectedRowVersions={str(x['id']): x['row_version'] for x in certificates(baseline, scope)},
                      before={t: digest(canonical(sorted(map(canonical, rows(baseline[t])))).encode()) for t in baseline['ddl']},
                      observed={t: digest(canonical(sorted(map(canonical, rows(current[t])))).encode()) for t in baseline['ddl']})
        print(json.dumps(report, ensure_ascii=False))
        return
    stopped(db, baseline)
    if state == 'COMPLETED':
        print(json.dumps(dict(state='COMPLETED', replayed=False))); return
    attempts = sorted(directory.glob('attempt-*'))
    if args.action == 'apply':
        assert not attempts, 'An attempt exists; verify first, then explicitly resume only ORIGINAL state'
    else:
        assert attempts, 'No prior attempt to resume'
        for path in attempts:
            assert (path / 'session.json').exists(), 'Missing durable session identity; manual investigation required'
            session = json.loads((path / 'session.json').read_bytes())
            assert db.query('SELECT COUNT(*) FROM information_schema.PROCESSLIST WHERE ID=' + str(session['connectionId']) + ';') == '0', 'Prior SQL session still exists'
    attempt = directory / ('attempt-' + str(len(attempts) + 1).zfill(3))
    attempt.mkdir()  # Exclusive reservation: concurrent apply cannot start another transaction.
    save(attempt / 'before.json', current)
    trigger_metadata = json.loads((bundle / 'retained-trigger-metadata.json').read_bytes())
    trigger_body = db.query("SELECT ACTION_STATEMENT FROM information_schema.TRIGGERS WHERE TRIGGER_SCHEMA=DATABASE() AND TRIGGER_NAME='trg_dcc_reg_cert_master_immutable_bu';")
    assert digest(re.sub(r'[\s`]+', '', trigger_body).encode()) in trigger_metadata['acceptedBodySha256'], 'Unknown master trigger'
    # Explicit registered schema repair, under stopped writers; never edits a ledger.
    db.query((bundle / 'retained-master-trigger.sql').read_text('utf8'))
    assert db.query((bundle / 'retained-master-trigger-guard.sql').read_text('utf8')) == 'PASS'
    sql_text = checks_sql(baseline, scope, args.environment)
    assert digest(sql_text.encode()) == json.loads((bundle / 'apply-sql-sha256.json').read_bytes())[args.environment]
    process = subprocess.Popen(db.command(), stdin=subprocess.PIPE, stdout=subprocess.PIPE, stderr=open(attempt / 'mysql.stderr.private.txt', 'xb'))
    # Flush and durably record the actual session before sending ANY transactional writes.
    process.stdin.write(b'SELECT CONNECTION_ID();\n'); process.stdin.flush()
    connection_id = int(process.stdout.readline())
    save(attempt / 'session.json', dict(connectionId=connection_id, databaseIdentity=baseline['identity'], pid=os.getpid(), sqlSha256=digest(sql_text.encode())))
    stdout, _ = process.communicate(sql_text.encode())  # No client timeout; disconnect is never treated as rollback proof.
    (attempt / 'mysql.stdout.private.txt').write_bytes(stdout)
    assert process.returncode == 0 and b'DISPOSAL_COMMITTED' in stdout, 'SQL failed or outcome unknown; keep writers stopped and verify'
    after = capture(db, baseline)
    assert compare_state(after, baseline, scope, args.environment) == 'COMPLETED'
    save(attempt / 'after.json', after)
    print(json.dumps(dict(state='COMPLETED', environment=args.environment, certificates=len(scope['ids']), replayed=False)))


if __name__ == '__main__':
    main()
