"""Root-owned exact development audit rule; no native business DML."""
from pathlib import Path
import gzip
import hashlib
import importlib.util
import json
import re
import shutil
import subprocess
import sys
from g21_mysql_support import LocalMysql, snapshot_original_rows, compare_original_rows, sha256_file

TASK = Path(__file__).resolve().parent
ROOT = TASK.parents[2]
CHILD = ROOT / 'doc/tasks/20261002-dcc-public-backend-completion'
OUTPUT = Path('C:/IntRuoyiBackups/20261005-dcc-four-direction-runtime/g59-audit-rule')
UUID = '92ca05d0-aec8-11f1-a944-02b4e226a5ef'
SQL_SHA = '3f5cf81609b2637a08024a415fedfe8f95e74137992e4622e70d0ddb8811ab78'
RULE = 'dcc.controlled-file.lifecycle-projection.repair'
TABLES = ['gxp_audit_policy_operation', 'gxp_audit_policy_version', 'infra_release_migration',
          'dcc_controlled_file', 'dcc_controlled_file_master', 'dcc_controlled_file_signature',
          'system_electronic_signature', 'controlled_content_version_ref',
          'controlled_content_transition_audit', 'dcc_workflow_lifecycle_event']

def save(path, value):
    path.write_text(json.dumps(value, ensure_ascii=False, indent=2)+'\n', encoding='utf-8')

def scalar(db, sql):
    return int(db.read(sql).strip())

def require_fence(db):
    if scalar(db, 'SELECT COUNT(*) FROM information_schema.innodb_trx;'):
        raise RuntimeError('Another active transaction exists')
    if scalar(db, 'SELECT COUNT(*) FROM information_schema.PROCESSLIST WHERE DB=DATABASE() AND ID<>CONNECTION_ID();'):
        raise RuntimeError('Another scoped database connection exists')
    if scalar(db, "SELECT COUNT(*) FROM information_schema.ROUTINES WHERE ROUTINE_SCHEMA=DATABASE() AND ROUTINE_NAME='g59_development_repair_operation';"):
        raise RuntimeError('Task routine already exists; no pre-DROP')

def backup(db, folder):
    target, errors = folder/'protected-tables.sql.gz', folder/'dump.stderr.txt'
    shell = ('test -n "$MYSQL_ROOT_PASSWORD" || exit 3; export MYSQL_PWD="$MYSQL_ROOT_PASSWORD"; '
             'exec mysqldump --user=root --single-transaction --skip-lock-tables --skip-add-locks '
             '--no-tablespaces --set-gtid-purged=OFF --hex-blob "$@"')
    args = [db.docker, 'exec', 'int-ruoyi-mysql', 'sh', '-c', shell, 'g59-protected-backup', db.database, *TABLES]
    with errors.open('xb') as err:
        process = subprocess.Popen(args, stdout=subprocess.PIPE, stderr=err)
        with gzip.open(target, 'xb') as zipped:
            shutil.copyfileobj(process.stdout, zipped)
        process.stdout.close()
        code = process.wait()
    if code:
        raise RuntimeError('Protected backup failed; exact private errors retained')
    expanded = 0
    with gzip.open(target, 'rb') as check:
        while data := check.read(1024*1024): expanded += len(data)
    if not expanded: raise RuntimeError('Empty backup')
    return {'path': str(target), 'sha256': sha256_file(target), 'bytes': target.stat().st_size,
            'expandedBytes': expanded, 'exitCode': code, 'gzipIntegrity': 'PASS'}

def exact_rule(db):
    fields = ['tenant_id', 'policy_version', 'operation_id', 'source_type', 'source_locator', 'domain',
              'subject_type', 'action_type', 'reason_policy', 'signature_policy', 'state_policy',
              'retention_class', 'test_ids', 'owner', 'applicability']
    raw = db.read('SELECT '+','.join(fields)+",active+0,deleted+0 FROM gxp_audit_policy_operation WHERE tenant_id=1 AND operation_id='"+RULE+"';").strip()
    impact = json.loads((CHILD/'g59-repair-impact.json').read_text(encoding='utf-8-sig'))
    expected = ['1', impact['policyVersion'], RULE, 'SERVICE_METHOD', impact['operation']['sourceLocator'], 'DCC',
                'DCC_CONTROLLED_FILE', 'UPDATE', 'REQUIRED_CATEGORY_AND_TEXT', 'NOT_REQUIRED', 'PRESENT_TO_PRESENT',
                'GXP_CONTROLLED_DOCUMENT', '[G59-REPAIR-01, G59-REPAIR-02, G59-REPAIR-03]', 'dcc-owner', 'GXP', '1', '0']
    if raw.split('\t') != expected: raise RuntimeError('Exact audit rule payload differs')

def run_database(name, sql):
    db = LocalMysql(name)
    actual = json.loads(db.read("SELECT JSON_OBJECT('database',DATABASE(),'uuid',@@server_uuid,'version',VERSION());"))
    if actual != {'database': name, 'uuid': UUID, 'version': '8.0.40'}:
        raise RuntimeError('Actual database identity differs')
    require_fence(db)
    if scalar(db, "SELECT COUNT(*) FROM gxp_audit_policy_operation WHERE tenant_id=1 AND operation_id='"+RULE+"';"):
        raise RuntimeError('First-run rule already exists; review exact prior receipt')
    directory = OUTPUT/name
    directory.mkdir()
    before = snapshot_original_rows(db, TABLES)
    save(directory/'before-rows.json', before)
    backup_proof = backup(db, directory)
    context = ("SET @dcc_g59_development_only=1,@dcc_g59_config_authorized=1,@dcc_g59_writers_excluded=1,"
               "@dcc_g59_expected_uuid='"+UUID+"',@dcc_g59_expected_database='"+name+"',"
               "@dcc_g59_operator_reviewed_sql_sha='"+SQL_SHA+"';\n").encode('utf-8')
    receipts = []
    for label in ['first', 'repeat']:
        require_fence(db)
        execution = db.run_authorized_sql(context+sql, private_directory=directory, stem=label)
        exact_rule(db)
        after = snapshot_original_rows(db, TABLES, {table: row['columns'] for table,row in before.items()})
        comparison = compare_original_rows(before, after, {'gxp_audit_policy_operation': 1})
        require_fence(db)
        receipts.append({'execution': execution, 'oldRows': comparison, 'extraConfigRows': 1,
                         'businessRowsChanged': 0})
        save(directory/(label+'-rows.json'), after)
        if label == 'first': first = after
        else: compare_original_rows(first, after, {})
    return {'database': name, 'identity': actual, 'backup': backup_proof, 'steps': receipts}

def main():
    if subprocess.run(['git','branch','--show-current'], cwd=ROOT, capture_output=True, text=True, check=True).stdout.strip() != 'int_qms':
        raise RuntimeError('Actual QMS branch required')
    stopped = subprocess.run(['pwsh','-NoProfile','-Command',
                             "if(@(Get-NetTCPConnection -LocalPort 48061 -State Listen -ErrorAction SilentlyContinue).Count){exit 1};exit 0"], capture_output=True)
    if stopped.returncode: raise RuntimeError('Owned QMS backend must be stopped before writer fence')
    sql = (CHILD/'g59-repair-audit-rule.review.sql').read_bytes()
    if hashlib.sha256(sql).hexdigest() != SQL_SHA: raise RuntimeError('Reviewed SQL drift')
    if OUTPUT.exists(): raise RuntimeError('Previous execution output exists; no automatic retry')
    OUTPUT.mkdir()
    results = []
    for name in ['dcc_intqms_g18_rehearsal', 'ruoyi-vue-pro']:
        results.append(run_database(name, sql))
    result = {'status':'G59_EXACT_SINGLE_DEVELOPMENT_AUDIT_RULE_CLONE_SOURCE_FIRST_REPEAT_PASS',
              'sqlSha256':SQL_SHA, 'results':results, 'qualityApprovalRowsWritten':0,
              'nativeBusinessRowsWritten':0, 'newRulePerDatabase':1}
    save(TASK/'g59-audit-rule-root-execution.json', result)
    print(json.dumps({'status':result['status'], 'databases':2, 'executions':4,
                      'nativeBusinessRowsChanged':0, 'qualityRowsChanged':0}, ensure_ascii=False))

if __name__ == '__main__': main()
