"""Prepare immutable local MySQL evidence and backups; this program cannot apply SQL."""
from __future__ import annotations
import argparse
import gzip
import hashlib
import json
from pathlib import Path
import re
import shutil
import subprocess
from g21_mysql_support import LocalMysql, MysqlFailure, snapshot_original_rows, compare_original_rows, sha256_file

TASK = Path(__file__).resolve().parent
REPO = TASK.parents[2]
UUID = '92ca05d0-aec8-11f1-a944-02b4e226a5ef'
DATABASES = ('ruoyi-vue-pro', 'dcc_intqms_g18_rehearsal')
MIGRATIONS = {
    '20261004_dcc_product_identity_source': '049d60e2e8f4a7999ebc213cda77154fa78c315bff296f78be127462977a2760',
    '20261005_dcc_project_folder_storage_mapping': 'afbdd3bbe5646dbb1e43f8db423e874cace07e4668bf578512f03c3009a5485a',
    '20261005_dcc_project_product_application_notify': '696bb80f60509ea453fe8c8fda07b42f8020e09415aaf2d2ed9b388ed907a810',
}
TABLES = [
    'dcc_file_directory', 'dcc_project_folder', 'dcc_project_code', 'dcc_category_directory_binding',
    'dcc_controlled_file', 'dcc_controlled_file_master', 'dcc_project_file_placement',
    'dcc_controlled_file_name_claim', 'dcc_controlled_file_source_ownership',
    'dcc_controlled_file_signature', 'system_electronic_signature', 'dcc_source_name_reservation',
    'dcc_legacy_source_name_scope', 'dcc_legacy_source_name_evidence', 'infra_release_migration',
    'system_notify_template', 'system_notify_message', 'dcc_product_catalog',
    'dcc_project_product_relation', 'dcc_project_product_create_request', 'dcc_project_access_rule',
]

def write(path: Path, value):
    path.write_text(json.dumps(value, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')

class ExactReadonlyMysql:
    def __init__(self, database):
        self.transport = LocalMysql(database)
        self.database = database
        self.identity = json.loads(self.transport.read(
            "SELECT JSON_OBJECT('database',DATABASE(),'uuid',@@server_uuid,'version',VERSION());"))
        if self.identity != {'database': database, 'uuid': UUID, 'version': '8.0.40'}:
            raise ValueError('Actual database identity differs from the reviewed scope')
    def read(self, sql):
        return self.transport.read(sql)

def dump(mysql: ExactReadonlyMysql, directory: Path, kind: str, options: list[str]):
    target = directory / (kind + '.sql.gz')
    errors = directory / (kind + '.stderr.txt')
    if target.exists() or errors.exists():
        raise ValueError('Existing backup output cannot be overwritten')
    shell = ('test -n "$MYSQL_ROOT_PASSWORD" || exit 3; export MYSQL_PWD="$MYSQL_ROOT_PASSWORD"; '
             'exec mysqldump --user=root --single-transaction --skip-lock-tables --skip-add-locks '
             '--no-tablespaces --set-gtid-purged=OFF --hex-blob "$@"')
    arguments = [mysql.transport.docker, 'exec', 'int-ruoyi-mysql', 'sh', '-c', shell,
                 'g51-readonly-backup', mysql.database, *options]
    with errors.open('xb') as error:
        process = subprocess.Popen(arguments, stdout=subprocess.PIPE, stderr=error)
        try:
            with gzip.open(target, 'xb', compresslevel=6) as backup:
                shutil.copyfileobj(process.stdout, backup)
            process.stdout.close()
        except BaseException:
            if process.poll() is None:
                process.terminate()
            process.wait()
            raise
        code = process.wait()
    if code:
        raise RuntimeError('Read-only dump failed; protected stderr retained')
    expanded = 0
    with gzip.open(target, 'rb') as backup:
        while chunk := backup.read(1024 * 1024):
            expanded += len(chunk)
    if expanded == 0:
        raise ValueError('Empty database backup')
    return {'kind': kind, 'path': target.as_posix(), 'bytes': target.stat().st_size,
            'sha256': sha256_file(target), 'uncompressedBytes': expanded,
            'dumpExitCode': code, 'gzipIntegrity': 'PASS', 'stderrBytes': errors.stat().st_size}

def prepare_database(database, directory):
    mysql = ExactReadonlyMysql(database)
    if int(mysql.read('SELECT COUNT(*) FROM information_schema.innodb_trx;').strip()) != 0:
        raise ValueError('Active transactions exist; do not seal a current baseline')
    actual_tables = set(mysql.read('SELECT TABLE_NAME FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE();').splitlines())
    if not set(TABLES).issubset(actual_tables):
        raise ValueError('A protected table is missing')
    ids = ','.join("'" + key + "'" for key in MIGRATIONS)
    pending = {
        'productSourceColumns': int(mysql.read("SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='dcc_controlled_file' AND COLUMN_NAME IN ('product_source','product_catalog_id','product_relation_id','product_create_request_id');").strip()),
        'mappingTable': int(mysql.read("SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='dcc_project_folder_storage_mapping';").strip()),
        'notificationTemplate': int(mysql.read("SELECT COUNT(*) FROM system_notify_template WHERE code='dcc-project-product-application-event';").strip()),
        'newMigrationLedgerRows': int(mysql.read("SELECT COUNT(*) FROM infra_release_migration WHERE migration_id IN (" + ids + ');').strip()),
    }
    if any(pending.values()):
        raise ValueError('An expected unexecuted migration is already partly present; review current state')
    directory.mkdir()
    definitions = {}
    for table in TABLES:
        raw = mysql.read('SHOW CREATE TABLE `' + table + '`;')
        name, delimiter, ddl = raw.partition('\t')
        if not delimiter or name != table or not ddl.startswith('CREATE TABLE '):
            raise ValueError('SHOW CREATE returned an unexpected protected table')
        definitions[table] = ddl.rstrip('\n')
    before = snapshot_original_rows(mysql, TABLES)
    write(directory / 'original-columns-row-digests.json', before)
    write(directory / 'protected-show-create.json', definitions)
    artifacts = [dump(mysql, directory, 'schema', ['--no-data']),
                 dump(mysql, directory, 'protected-data', ['--no-create-info', '--skip-triggers', *TABLES])]
    after = snapshot_original_rows(mysql, TABLES, {table: row['columns'] for table, row in before.items()})
    unchanged = compare_original_rows(before, after, {})
    for table, original in definitions.items():
        current = mysql.read('SHOW CREATE TABLE `' + table + '`;').partition('\t')[2].rstrip('\n')
        if current != original:
            raise ValueError('Protected schema changed during the backup')
    receipt = {'status': 'READONLY_CURRENT_BASELINE_AND_BACKUPS_VERIFIED_NOT_MIGRATED',
               'identity': mysql.identity, 'databaseWrites': False, 'authorizationInferred': False,
               'protectedTables': len(TABLES), 'actualTableCount': len(actual_tables),
               'targetAdditionsPresent': pending, 'artifacts': artifacts,
               'originalRowProof': unchanged,
               'originalRowDigestFile': {'path': (directory / 'original-columns-row-digests.json').as_posix(),
                                        'sha256': sha256_file(directory / 'original-columns-row-digests.json')}}
    write(directory / 'readonly-preparation-receipt.json', receipt)
    return receipt

def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--output-directory', required=True, type=Path)
    arguments = parser.parse_args()
    output = arguments.output_directory.resolve()
    if output.exists():
        raise ValueError('Refusing to reuse an existing preparation directory')
    for migration, expected in MIGRATIONS.items():
        if sha256_file(REPO / 'IntRuoyiBackend/sql/mysql' / (migration + '.sql')) != expected:
            raise ValueError('Frozen SQL changed before backup')
    output.mkdir(parents=True)
    receipts = []
    try:
        for database in DATABASES:
            receipts.append(prepare_database(database, output / database))
    except MysqlFailure as error:
        (output / 'read-error-private.stderr.txt').write_bytes(error.private_error)
        write(output / 'failed-preparation.json', {'status': 'FAILED_READONLY_PREPARATION', 'exitCode': error.exit_code,
                                                'databaseWrites': False, 'automaticRetry': False})
        raise RuntimeError('Read-only MySQL preparation failed; protected error retained') from None
    result = {'status': 'TWO_DATABASE_READONLY_BASELINES_AND_BACKUPS_VERIFIED_NOT_MIGRATED',
              'databases': receipts, 'migrationSql': MIGRATIONS,
              'databaseWrites': False, 'authorizationInferred': False, 'restorePerformed': False}
    write(output / 'combined-receipt.json', result)
    public = {'status': result['status'], 'databases': 2, 'protectedTablesPerDatabase': len(TABLES),
              'databaseWrites': False, 'authorizationInferred': False,
              'protectedReceipt': (output / 'combined-receipt.json').as_posix(),
              'protectedReceiptSha256': sha256_file(output / 'combined-receipt.json')}
    write(TASK / 'g51-readonly-preparation-root-review.json', public)
    print(json.dumps(public, ensure_ascii=False))

if __name__ == '__main__':
    main()
