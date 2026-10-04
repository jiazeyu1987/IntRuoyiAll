"""Offline exact-scope SQL preparation; no database connection or execution capability."""
from __future__ import annotations
import argparse
import hashlib
import importlib.util
import json
from pathlib import Path
import re
support_spec = importlib.util.spec_from_file_location('g51_readonly_preparation',
                Path(__file__).resolve().parent / 'g51-readonly-preparation.py')
support = importlib.util.module_from_spec(support_spec)
support_spec.loader.exec_module(support)
DATABASES, MIGRATIONS, REPO = support.DATABASES, support.MIGRATIONS, support.REPO

def material(database, migration, phase, operation):
    if database not in DATABASES or migration not in MIGRATIONS or phase not in {'first', 'repeat'}:
        raise ValueError('The reviewed database/migration/phase scope differs')
    if not re.fullmatch(r'[A-Za-z0-9_-]{1,80}', operation or ''):
        raise ValueError('Operation identity is not a safe explicit value')
    source = REPO / 'IntRuoyiBackend/sql/mysql' / (migration + '.sql')
    payload = source.read_bytes()
    digest = hashlib.sha256(payload).hexdigest()
    if digest != MIGRATIONS[migration]:
        raise ValueError('Reviewed SQL source changed')
    envelope = ("SELECT JSON_OBJECT('phase','SESSION_IDENTITY','database',DATABASE(),"
                "'uuid',@@server_uuid,'version',VERSION(),'intendedDatabase','" + database + "');\n")
    ledger = ''
    if phase == 'first':
        ledger = ("INSERT INTO infra_release_migration(release_tag,migration_id,file_name,sha256,"
                  "target_environment,status,started_at,finished_at,operation_id,creator,updater,deleted,tenant_id) "
                  "VALUES('" + operation + "','" + migration + "','sql/mysql/" + migration + ".sql','" + digest
                  + "','test','APPLIED',UTC_TIMESTAMP(),UTC_TIMESTAMP(),'" + operation
                  + "','dcc-g50-local-task','dcc-g50-local-task',b'0',0);\n")
    done = "SELECT JSON_OBJECT('phase','" + phase + "_COMPLETE','migrationId','" + migration + "');\n"
    return envelope.encode('utf-8') + payload + b'\n' + ledger.encode('utf-8') + done.encode('utf-8')

def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--output-directory', type=Path, required=True)
    parser.add_argument('--operation', required=True)
    arguments = parser.parse_args()
    directory = arguments.output_directory.resolve()
    if directory.exists():
        raise ValueError('Existing prepared material cannot be replaced')
    directory.mkdir(parents=True)
    files = []
    for database in DATABASES:
        target = directory / database
        target.mkdir()
        for ordinal, migration in enumerate(MIGRATIONS, start=1):
            for phase in ('first', 'repeat'):
                payload = material(database, migration, phase, arguments.operation)
                destination = target / (str(ordinal) + '-' + migration + '-' + phase + '.sql')
                destination.write_bytes(payload)
                files.append({'database': database, 'migrationId': migration, 'phase': phase,
                              'path': destination.as_posix(), 'bytes': len(payload),
                              'sha256': hashlib.sha256(payload).hexdigest(),
                              'newLedgerWrites': 1 if phase == 'first' else 0})
    result = {'status': 'EXACT_THREE_SQL_FIRST_REPEAT_MATERIAL_PREPARED_NOT_AUTHORIZED_NOT_EXECUTED',
              'operationId': arguments.operation, 'files': files, 'executionWhitelist': list(MIGRATIONS),
              'automaticDependencyReplay': False, 'skipLedgerUpdate': False, 'databaseWrites': False,
              'authorizationGranted': False,
              'executionPreconditions': ['actual user approval for the combined three-script scope',
                                        'fresh exact database identity and writer checks',
                                        'fresh immutable old-row and schema baseline',
                                        'exact absent/present first-or-repeat schema and ledger state',
                                        'no preexisting task procedure may be dropped',
                                        'clone first/repeat verification before source execution',
                                        'stop on any error; never mysql --force or automatic retry']}
    (directory / 'material-manifest.json').write_text(json.dumps(result, indent=2) + '\n', encoding='utf-8')
    print(json.dumps({'status': result['status'], 'preparedFiles': len(files), 'databaseWrites': False,
                      'manifestPath': (directory / 'material-manifest.json').as_posix()}))

if __name__ == '__main__':
    main()
