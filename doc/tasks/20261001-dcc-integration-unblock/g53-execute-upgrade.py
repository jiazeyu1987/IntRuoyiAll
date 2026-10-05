"""Root-owned authorized exact-three migration execution with immutable old-row proofs."""
from __future__ import annotations
import argparse
import hashlib
import importlib.util
import json
from pathlib import Path
import sys
from g21_mysql_support import snapshot_original_rows, compare_original_rows, sha256_file

TASK = Path(__file__).resolve().parent
def load(name, filename):
    spec = importlib.util.spec_from_file_location(name, TASK / filename)
    module = importlib.util.module_from_spec(spec)
    sys.modules[name] = module
    spec.loader.exec_module(module)
    return module
readonly = load('g53_readonly', 'g51-readonly-preparation.py')
prepared = load('g53_material', 'g51-prepare-execution-material.py')
NEW_COLUMNS = ['product_source', 'product_catalog_id', 'product_relation_id', 'product_create_request_id']
MAPPING = 'dcc_project_folder_storage_mapping'
TEMPLATE = 'dcc-project-product-application-event'
OPERATION = 'dcc-g50-local-four-directions-20261005'

def require(condition, message):
    if not condition:
        raise ValueError(message)
def save(path, value):
    readonly.write(path, value)
def json_rows(mysql, sql):
    return [json.loads(line) for line in mysql.read(sql).splitlines()]
def columns(mysql, table):
    return json_rows(mysql, "SELECT JSON_OBJECT('name',COLUMN_NAME,'ordinal',ORDINAL_POSITION,'type',COLUMN_TYPE,'nullable',IS_NULLABLE,'default',COLUMN_DEFAULT,'extra',EXTRA,'collation',COLLATION_NAME,'fsp',DATETIME_PRECISION) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='" + table + "' ORDER BY ORDINAL_POSITION;")
def indexes(mysql, table):
    return json_rows(mysql, "SELECT JSON_OBJECT('name',INDEX_NAME,'unique',NON_UNIQUE=0,'sequence',SEQ_IN_INDEX,'column',COLUMN_NAME,'prefix',SUB_PART) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='" + table + "' ORDER BY INDEX_NAME,SEQ_IN_INDEX;")
def shape(mysql, table):
    facts = json_rows(mysql, "SELECT JSON_OBJECT('engine',ENGINE,'collation',TABLE_COLLATION) FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='" + table + "';")
    require(len(facts) == 1, 'Expected table shape is missing')
    return {'columns': columns(mysql, table), 'indexes': indexes(mysql, table), 'table': facts[0]}
def scalar(mysql, sql):
    return int(mysql.read(sql).strip())
def no_writers(mysql):
    require(scalar(mysql, 'SELECT COUNT(*) FROM information_schema.innodb_trx;') == 0, 'Active transaction exists')
    require(scalar(mysql, "SELECT COUNT(*) FROM information_schema.processlist WHERE ID<>CONNECTION_ID() AND DB IN ('ruoyi-vue-pro','dcc_intqms_g18_rehearsal');") == 0, 'Another scoped database session exists')
def no_routines(mysql):
    require(scalar(mysql, "SELECT COUNT(*) FROM information_schema.routines WHERE routine_schema=DATABASE() AND routine_name IN ('dcc_product_identity_source','g49_dcc_project_notify');") == 0, 'Existing task routine must not be dropped')
def ledger(mysql):
    keys = ','.join("'" + key + "'" for key in readonly.MIGRATIONS)
    return json_rows(mysql, "SELECT JSON_OBJECT('migrationId',migration_id,'sha256',sha256,'file',file_name,'releaseTag',release_tag,'operationId',operation_id,'environment',target_environment,'status',status,'tenant',tenant_id,'deleted',deleted+0,'creator',creator,'updater',updater,'started',CAST(started_at AS CHAR),'finished',CAST(finished_at AS CHAR),'error',error_message,'created',CAST(create_time AS CHAR),'updated',CAST(update_time AS CHAR)) FROM infra_release_migration WHERE migration_id IN (" + keys + ') ORDER BY migration_id;')
def validate_ledger(rows, completed):
    require(len(rows) == len(completed) and {row['migrationId'] for row in rows} == set(completed), 'Target ledger identity differs')
    for row in rows:
        key = row['migrationId']
        require(row['sha256'] == readonly.MIGRATIONS[key] and row['file'] == 'sql/mysql/' + key + '.sql'
                and row['releaseTag'] == OPERATION and row['operationId'] == OPERATION
                and row['environment'] == 'test' and row['status'] == 'APPLIED' and row['tenant'] == 0
                and row['deleted'] == 0 and row['creator'] == 'dcc-g50-local-task'
                and row['updater'] == 'dcc-g50-local-task' and row['started'] and row['finished'] and row['error'] is None,
                'New migration ledger payload differs')
def template(mysql):
    return json_rows(mysql, "SELECT JSON_OBJECT('id',CAST(id AS CHAR),'name',name,'code',code,'type',type,'nickname',nickname,'content',content,'params',params,'status',status,'remark',remark,'deleted',deleted+0,'creator',creator,'updater',updater,'created',CAST(create_time AS CHAR),'updated',CAST(update_time AS CHAR)) FROM system_notify_template WHERE code='" + TEMPLATE + "';")
def validate_template(rows, present):
    require(len(rows) == (1 if present else 0), 'Notification template count differs')
    if rows:
        row = rows[0]
        expected = {'name': 'DCC项目及产品申请通知', 'code': TEMPLATE, 'type': 2, 'nickname': 'DCC系统',
                    'content': '项目及产品申请《{businessTitle}》：{eventName}。说明：{reason}。请打开原申请查看。',
                    'params': '["businessTitle","eventName","reason","notifyTargetType","notifyTargetId","actionUrl"]',
                    'status': 0, 'remark': 'DCC native project-product request notification', 'deleted': 0,
                    'creator': 'dcc-seed', 'updater': 'dcc-seed'}
        require(all(row[key] == value for key, value in expected.items()), 'Notification template exact payload differs')
def validate_product_columns(rows, present):
    extra = [row for row in rows if row['name'] in NEW_COLUMNS]
    require(len(extra) == (4 if present else 0), 'Product column state differs')
    for row in extra:
        require(row['type'] == ('varchar(32)' if row['name'] == 'product_source' else 'bigint')
                and row['nullable'] == 'YES' and row['default'] is None and row['extra'] == '',
                'Product source column shape differs')
def validate_mapping_shape(value):
    require(value['table']['engine'] == 'InnoDB' and value['table']['collation'].startswith('utf8mb4_'), 'Mapping engine/charset differs')
    expected_names = ['id','tenant_id','project_code_id','project_folder_id','category_id','base_directory_id',
                      'storage_directory_id','creator','updater','create_time','update_time','deleted']
    require([row['name'] for row in value['columns']] == expected_names, 'Mapping column identity/order differs')
    for row in value['columns']:
        name = row['name']
        expected_type = 'bigint' if name.endswith('_id') or name == 'id' else ('varchar(64)' if name in {'creator','updater'} else 'bit(1)' if name == 'deleted' else 'datetime(6)')
        require(row['type'] == expected_type, 'Mapping column type differs')
        require(row['nullable'] == ('YES' if name in {'creator','updater','create_time','update_time'} else 'NO'), 'Mapping nullability differs')
        if name in {'create_time','update_time'}:
            require(row['fsp'] == 6 and str(row['default']).lower() == 'current_timestamp(6)', 'Mapping time precision differs')
            require(row['extra'] == ('DEFAULT_GENERATED' if name == 'create_time' else 'DEFAULT_GENERATED on update CURRENT_TIMESTAMP(6)'), 'Mapping update-time expression differs')
        elif name == 'deleted':
            require(row['default'] == "b'0'" and row['extra'] == '', 'Mapping soft-delete default differs')
        else:
            require(row['default'] is None and row['extra'] == ('auto_increment' if name == 'id' else ''), 'Unexpected mapping default/auto-increment')
    observed = {}
    for row in value['indexes']:
        require(row['prefix'] is None, 'Unexpected mapping index prefix')
        observed.setdefault(row['name'], {'unique': bool(row['unique']), 'columns': []})['columns'].append(row['column'])
    require(observed == {
        'PRIMARY': {'unique': True, 'columns': ['id']},
        'uk_dcc_folder_category_storage': {'unique': True, 'columns': ['tenant_id','project_folder_id','category_id']},
        'uk_dcc_storage_mapping_leaf': {'unique': True, 'columns': ['tenant_id','storage_directory_id']},
        'idx_dcc_storage_mapping_base': {'unique': False, 'columns': ['tenant_id','base_directory_id']},
        'idx_dcc_storage_mapping_project': {'unique': False, 'columns': ['tenant_id','project_code_id']},
    }, 'Mapping indexes differ')
    return True
def verify_state(mysql, baseline, original_shapes, completed):
    validate_ledger(ledger(mysql), completed)
    product_present = list(readonly.MIGRATIONS)[0] in completed
    mapping_present = list(readonly.MIGRATIONS)[1] in completed
    template_present = list(readonly.MIGRATIONS)[2] in completed
    validate_template(template(mysql), template_present)
    for table, original in original_shapes.items():
        current = shape(mysql, table)
        if table == 'dcc_controlled_file':
            validate_product_columns(current['columns'], product_present)
            current['columns'] = [row for row in current['columns'] if row['name'] not in NEW_COLUMNS]
        require(current == original, 'An original protected schema changed: ' + table)
    require(scalar(mysql, "SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='" + MAPPING + "';") == int(mapping_present), 'Mapping table state differs')
    if mapping_present:
        validate_mapping_shape(shape(mysql, MAPPING))
        require(scalar(mysql, 'SELECT COUNT(*) FROM ' + MAPPING + ';') == 0, 'Migration created business mappings')
    if product_present:
        require(scalar(mysql, 'SELECT COUNT(*) FROM dcc_controlled_file WHERE ' + ' OR '.join(name + ' IS NOT NULL' for name in NEW_COLUMNS) + ';') == 0, 'Migration backfilled product facts')
    after = snapshot_original_rows(mysql, readonly.TABLES, {name: row['columns'] for name, row in baseline.items()})
    return compare_original_rows(baseline, after, {'infra_release_migration':len(completed), 'system_notify_template':int(template_present)})

def run_database(database, output, material_root):
    no_writers(readonly.ExactReadonlyMysql(database))
    directory = output / database
    baseline_receipt = readonly.prepare_database(database, directory)
    mysql = readonly.ExactReadonlyMysql(database)
    baseline = json.loads((directory / 'original-columns-row-digests.json').read_text(encoding='utf-8'))
    original_shapes = {table: shape(mysql, table) for table in readonly.TABLES}
    save(directory / 'original-shapes.json', original_shapes)
    completed = []
    steps = []
    for ordinal, migration in enumerate(readonly.MIGRATIONS, start=1):
        for phase in ['first','repeat']:
            no_writers(mysql);no_routines(mysql)
            readonly.ExactReadonlyMysql(database)
            verify_state(mysql, baseline, original_shapes, completed)
            before_ledger, before_template = ledger(mysql), template(mysql)
            payload = prepared.material(database, migration, phase, OPERATION)
            file = material_root / database / (str(ordinal) + '-' + migration + '-' + phase + '.sql')
            require(file.read_bytes() == payload, 'Prepared execution bytes differ')
            step_dir = directory / (str(ordinal) + '-' + phase)
            step_dir.mkdir()
            save(output / 'active-phase.json', {'status':'EXECUTION_STARTED_OUTCOME_NOT_YET_VERIFIED',
                 'database':database,'migrationId':migration,'phase':phase,'sqlSha256':hashlib.sha256(payload).hexdigest(),
                 'stepDirectory':step_dir.as_posix(),'databaseWriteMayHaveOccurred':True})
            result = mysql.transport.run_authorized_sql(payload, private_directory=step_dir, stem='actual-mysql')
            raw = Path(result['stdout']).read_text(encoding='utf-8')
            messages = [json.loads(line) for line in raw.splitlines() if line.startswith('{')]
            require(len(messages) == 2 and messages[0] == {'phase':'SESSION_IDENTITY','database':database,'uuid':readonly.UUID,'version':'8.0.40','intendedDatabase':database}
                    and messages[-1] == {'phase':phase+'_COMPLETE','migrationId':migration}, 'Actual MySQL phase identity/completion differs')
            if phase == 'first':
                completed.append(migration)
            else:
                require(ledger(mysql) == before_ledger and template(mysql) == before_template, 'Repeat changed ledger or template')
            old_rows = verify_state(mysql, baseline, original_shapes, completed)
            no_routines(mysql)
            step = {'migrationId':migration,'phase':phase,'actualExecution':result,'oldRows':old_rows,
                    'currentLedger':ledger(mysql),'currentTemplate':template(mysql),'schemaVerified':True}
            save(step_dir / 'phase-receipt.json', step)
            save(output / 'active-phase.json', {'status':'PHASE_EXECUTED_AND_VERIFIED',
                 'database':database,'migrationId':migration,'phase':phase,'receipt':(step_dir / 'phase-receipt.json').as_posix()})
            steps.append({'migrationId':migration,'phase':phase,'exitCode':result['exitCode'],
                          'receiptPath':(step_dir / 'phase-receipt.json').as_posix(),
                          'receiptSha256':sha256_file(step_dir / 'phase-receipt.json')})
    receipt = {'status':'ACTUAL_THREE_MIGRATIONS_FIRST_REPEAT_PASS_OLD_ROWS_UNCHANGED',
               'database':database,'uuid':readonly.UUID,'steps':steps,'actualNewLedgerRows':3,
               'newProductColumns':4,'mappingTableRows':0,'newNotifyTemplates':1,
               'backupArtifacts':baseline_receipt['artifacts'],'databaseWrites':True,'businessHistoryChanged':False}
    save(directory / 'completed-upgrade-receipt.json', receipt)
    return receipt

def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--output-directory', type=Path, required=True)
    parser.add_argument('--material-directory', type=Path, required=True)
    arguments = parser.parse_args()
    authorization = json.loads((TASK / 'g53-user-authorization.json').read_text(encoding='utf-8-sig'))
    require(authorization['actualUserReply'] == '授权' and authorization['migrationIds'] == list(readonly.MIGRATIONS)
            and authorization['cloneFirstRepeatBeforeSource'] is True and authorization['maximumLedgerAdditionsPerDatabase'] == 3,
            'Actual user authorization does not cover this execution')
    output = arguments.output_directory.resolve()
    require(not output.exists(), 'Existing execution directory requires outcome review, not retry')
    output.mkdir(parents=True)
    try:
        clone = run_database(readonly.DATABASES[1], output, arguments.material_directory)
        require(clone['status'] == 'ACTUAL_THREE_MIGRATIONS_FIRST_REPEAT_PASS_OLD_ROWS_UNCHANGED', 'Clone rehearsal did not pass')
        source = run_database(readonly.DATABASES[0], output, arguments.material_directory)
    except BaseException as error:
        active = json.loads((output / 'active-phase.json').read_text(encoding='utf-8')) if (output / 'active-phase.json').exists() else None
        save(output / 'stopped-execution.json', {'status':'STOPPED_REQUIRES_ACTUAL_STATE_REVIEW',
              'activePhase':active,'errorType':type(error).__name__,'message':str(error),'ddlRollbackClaimed':False,'automaticRetry':False})
        raise
    receipt = {'status':'CLONE_AND_SOURCE_THREE_MIGRATIONS_FIRST_REPEAT_VERIFIED',
               'databases':[clone,source],'authorization':sha256_file(TASK / 'g53-user-authorization.json'),
               'oldBusinessHistoryChanged':False,'dependencySqlReplayed':False,'oldLedgerRewritten':False}
    save(output / 'combined-upgrade-receipt.json', receipt)
    public = {'status':receipt['status'],'databases':2,'actualMysqlExecutions':12,
              'sourceNewColumns':4,'sourceNewMappingTable':1,'sourceNewNotifyTemplate':1,
              'newLedgerRowsPerDatabase':3,'oldBusinessHistoryChanged':False,
              'protectedReceipt':(output / 'combined-upgrade-receipt.json').as_posix(),
              'protectedReceiptSha256':sha256_file(output / 'combined-upgrade-receipt.json')}
    save(TASK / 'g53-runtime-upgrade-root-review.json', public)
    print(json.dumps(public, ensure_ascii=False))

if __name__ == '__main__':
    main()
