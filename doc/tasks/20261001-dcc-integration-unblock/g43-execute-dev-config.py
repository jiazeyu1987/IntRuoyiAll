"""Root exact local dev audit configuration; no quality approval rows or business actions."""
from pathlib import Path
import gzip
import hashlib
import importlib.util
import json
import subprocess
import sys

ROOT = Path(__file__).resolve().parent
REPO = Path('C:/IntRuoyi/20261001-dcc-integration')
TASK = REPO / 'doc/tasks/20261002-dcc-public-backend-completion'
OUT = Path('C:/IntRuoyiBackups/20261003-dcc-integration/g43-development-config-run')
STAGE = 'DELIVERY'


def sha(path):
    return hashlib.sha256(Path(path).read_bytes()).hexdigest()


def load(name, path):
    spec = importlib.util.spec_from_file_location(name, path)
    module = importlib.util.module_from_spec(spec)
    sys.modules[name] = module
    spec.loader.exec_module(module)
    return module


def main():
    global STAGE
    if OUT.exists():
        raise ValueError('Existing execution receipts; inspect actual outcome before any further action')
    manifest = TASK / 'g43-dev-config26-delivery-fingerprints.json'
    if sha(manifest) != 'd527c6b99ad0ecebbbdac8c2cf775d7b9511da912e2d3cd721e83d272ca7bbbb':
        raise ValueError('Dev configuration delivery changed')
    delivery = json.loads(manifest.read_text())
    for item in delivery['assets']:
        path = Path(item['path'])
        if not path.is_absolute():
            path = REPO / path
        if sha(path) != item['sha256'] or path.stat().st_size != item['bytes']:
            raise ValueError('Reviewed dev configuration drift')
    config = load('g43_root_config', TASK / 'g43-dev-config26.py')
    support = load('g43_root_mysql', ROOT / 'g21_mysql_support.py')
    db = support.LocalMysql('ruoyi-vue-pro')
    # Existing fixed local CLI transport is pinned to Docker int-ruoyi-mysql and this source DB.
    plan = config.old.package()
    prefix = "SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci; SET time_zone='+08:00';\n"

    def read_session(sql):
        result = subprocess.run(db.command(), input=(prefix + sql).encode(), capture_output=True)
        if result.returncode or result.stderr:
            raise ValueError('Current exact local read failed; no raw diagnostics printed')
        return [json.loads(line) for line in result.stdout.decode('utf-8').splitlines() if line]

    def capture():
        facts = read_session("SELECT JSON_OBJECT('database',DATABASE(),'serverUuid',@@server_uuid,'mysqlVersion',@@version,'timeZone',@@session.time_zone,'connectionCharset',@@character_set_connection,'connectionCollation',@@collation_connection,'transactions',(SELECT COUNT(*) FROM information_schema.INNODB_TRX t JOIN information_schema.PROCESSLIST p ON p.ID=t.trx_mysql_thread_id WHERE p.ID<>CONNECTION_ID()),'otherConnections',(SELECT COUNT(*) FROM information_schema.PROCESSLIST WHERE ID<>CONNECTION_ID() AND USER NOT IN ('event_scheduler','system user')),'enabledEvents',(SELECT COUNT(*) FROM information_schema.EVENTS WHERE EVENT_SCHEMA=DATABASE() AND STATUS='ENABLED'),'tables',(SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME IN ('gxp_audit_policy_operation','gxp_audit_policy_version','dcc_legacy_source_name_scope','dcc_legacy_source_name_evidence','dcc_source_name_reservation') AND ENGINE='InnoDB'),'sidecarLedger',(SELECT COUNT(*) FROM infra_release_migration WHERE migration_id='20261003_dcc_legacy_source_name_occupancy' AND sha256='621fe041064ac07c5bfe995abd556c3f9436b0253c883a3b7d2b33014e68de2a' AND status='APPLIED' AND target_environment='test' AND deleted=b'0'));\n")[0]
        if any(facts[k] != 0 for k in ['transactions', 'otherConnections', 'enabledEvents']) or facts['tables'] != 5 or facts['sidecarLedger'] != 1:
            raise ValueError('Current writers or schema prerequisite differs')
        environment = {k: facts[k] for k in ['database', 'serverUuid', 'mysqlVersion', 'timeZone', 'connectionCharset', 'connectionCollation']}
        environment.update(connectionHost='127.0.0.1', connectionPort=23306, tenantId=1, sourceSchemaReady=True, allApplicationWritersExcluded=True)
        payload = {}
        for table, key in [('gxp_audit_policy_operation', 'operationRows'), ('gxp_audit_policy_version', 'qualityVersionRows')]:
            columns = db.read("SELECT COLUMN_NAME FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='" + table + "' ORDER BY ORDINAL_POSITION;\n").splitlines()
            if not columns or any(not column.replace('_', '').isalnum() for column in columns):
                raise ValueError('Current policy columns invalid')
            pairs = []
            for column in columns:
                expression = '`' + column + '`' + ('+0' if column in ['active', 'deleted'] else '')
                pairs.append("'" + column + "'," + expression)
            payload[key] = read_session('SELECT JSON_OBJECT(' + ','.join(pairs) + ') FROM ' + table + ' ORDER BY id;\n')
        return {'environment': environment, **payload}

    STAGE = 'CURRENT_READONLY_CAPTURE'
    before = capture()
    STAGE = 'PURE_EXACT26_PREFLIGHT'
    prepared = config.plan(before['operationRows'], plan, config.DESCRIPTOR, before['environment'], write_authorized=True)
    if prepared['insertCount'] != 26:
        raise ValueError('Expected fresh exact26 absent configuration; no silent replay')
    if db.read("SELECT COUNT(*) FROM information_schema.ROUTINES WHERE ROUTINE_SCHEMA=DATABASE() AND ROUTINE_NAME='dcc_g43_dev26_config';\n").strip() != '0':
        raise ValueError('Deployment helper collision')
    if '--preflight-only' in sys.argv:
        print(json.dumps({'status': 'ACTUAL_DEV26_PREFLIGHT_PASS_NO_WRITES', 'plannedInserts': 26}))
        return
    STAGE = 'BACKUP_AND_EXECUTION'
    OUT.mkdir()
    (OUT / 'before.json').write_text(json.dumps(before, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
    # Fixed readonly dump of the two tables touched/protected; no credential in arguments or output.
    shell = 'test -n "$MYSQL_ROOT_PASSWORD" || exit 3; export MYSQL_PWD="$MYSQL_ROOT_PASSWORD"; exec mysqldump --user=root --default-character-set=utf8mb4 --single-transaction --skip-lock-tables --skip-triggers --skip-routines --skip-events --set-gtid-purged=OFF --no-tablespaces ruoyi-vue-pro gxp_audit_policy_operation gxp_audit_policy_version'
    dump = subprocess.run([db.docker, 'exec', '-i', 'int-ruoyi-mysql', 'sh', '-c', shell], capture_output=True)
    if dump.returncode or dump.stderr or b'-- Dump completed on ' not in dump.stdout:
        raise ValueError('Policy backup failed before execution')
    backup = OUT / 'original-policy-tables.sql.gz'
    with gzip.open(backup, 'xb') as stream:
        stream.write(dump.stdout)
    receipt = {'status': 'PREPARED_EXACT26_DEV_CONFIGURATION', 'developmentOnly': True, 'actualQualityApproved': False,
               'beforeSha256': sha(OUT / 'before.json'), 'backupSha256': sha(backup), 'steps': [], 'writeAttempted': False}
    output = OUT / 'receipt.json'

    def save():
        output.write_text(json.dumps(receipt, indent=2) + '\n', encoding='utf-8')

    save()
    reviewed_sql = TASK / 'g43-dev-config26.review.sql'
    if sha(reviewed_sql) != 'cea0fcd4413dfee1f6175089fe35205ee167e9c1df8f05f38eded9e455ba7c23':
        raise ValueError('SQL drift after review')
    authorized = ("SET @dcc_g43_dev_only=1; SET @dcc_g43_write_authorized=1; SET @dcc_g43_policy_sha='" + config.old.POLICY_SHA + "'; SET @dcc_g43_coverage_sha='" + config.old.COVERAGE_SHA + "';\n").encode() + reviewed_sql.read_bytes()
    try:
        for phase, original, expected in [('first', before, 26), ('repeat', None, 0)]:
            current = capture()
            if phase == 'first' and current != before:
                raise ValueError('Baseline drift after backup')
            previous = current
            receipt.update(status='EXECUTING_' + phase.upper(), writeAttempted=True)
            save()
            material = OUT / (phase + '.sql')
            material.write_bytes(authorized)
            result = db.run_authorized_sql(authorized, private_directory=OUT, stem=phase)
            after = capture()
            proof = config.postflight(previous['operationRows'], after['operationRows'], previous['qualityVersionRows'], after['qualityVersionRows'], plan, config.DESCRIPTOR, after['environment'], expected_inserts=expected)
            (OUT / (phase + '-after.json')).write_text(json.dumps(after, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
            receipt['steps'].append({'phase': phase, 'exitCode': result['exitCode'], 'materialSha256': sha(material), 'insertedOperations': expected, 'qualityVersionInserts': 0, 'postflight': proof})
            save()
        receipt.update(status='ACTUAL_DEV26_FIRST_REPEAT_PASS_NO_QUALITY_REGISTRATION', qualityRecordsUnchanged=True, oldOperationsUnchanged=True)
        save()
        (ROOT / 'g43-dev-config-root-review.json').write_text(json.dumps({'status': receipt['status'], 'insertedOperations': 26, 'repeatInserts': 0,
            'qualityVersionInserts': 0, 'developmentOnly': True, 'executionReceipt': str(output), 'receiptSha256': sha(output),
            'actualQualityApproved': False, 'legacyActivationExecuted': False}, indent=2) + '\n', encoding='utf-8')
        print(json.dumps({'status': receipt['status'], 'insertedOperations': 26, 'qualityVersionInserts': 0}))
    except Exception as error:
        receipt.update(status='STOPPED_PRESERVE_ACTUAL_CONFIG_EFFECTS_REVIEW_REQUIRED', errorType=type(error).__name__, automaticRetryAllowed=False)
        save()
        raise


if __name__ == '__main__':
    try:
        main()
    except Exception as error:
        safe_reason = str(error) if type(error) is ValueError else None
        print(json.dumps({'status': 'DEV_CONFIG_EXECUTION_STOPPED', 'errorType': type(error).__name__, 'stage': STAGE, 'safeReason': safe_reason}))
        raise SystemExit(1) from None
