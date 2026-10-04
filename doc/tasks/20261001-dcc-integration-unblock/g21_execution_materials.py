"""Prepare exact local migration inputs; no network or database execution."""
from __future__ import annotations

import argparse
import hashlib
import json
from pathlib import Path
import re

from g21_migration_scope import EXPECTED_IDS, read_json, require, sha256_file, validate_bundle, verify_backups


def sql_literal(value: str) -> str:
    if not isinstance(value, str) or not value or re.fullmatch(r'[A-Za-z0-9_./:-]+', value) is None:
        raise ValueError('Execution metadata must be a safe exact ASCII identity')
    return "'" + value + "'"


def compose_script(entries: list[dict], backend: Path, operation: str, *, repeat: bool) -> bytes:
    require(len(entries) == 19 and {row['migrationId'] for row in entries} == EXPECTED_IDS,
            'Execution composer only accepts nineteen exact task identities')
    statements = []
    for entry in entries:
        key = entry['migrationId']
        source = (backend / entry['file']).read_bytes()
        require(hashlib.sha256(source).hexdigest() == entry['sha256'], 'Execution SQL source changed: ' + key)
        begin = "SELECT JSON_OBJECT('taskPhase','sql_begin','migrationId'," + sql_literal(key) + ");\n"
        statements.extend([begin.encode(), source, b'\n'])
        if not repeat:
            # Plain INSERT, never UPSERT/UPDATE. A collision aborts rather than rewriting existing history.
            insert = "INSERT INTO infra_release_migration (release_tag,migration_id,file_name,sha256,target_environment,status,started_at,finished_at,operation_id,creator,updater,deleted,tenant_id) VALUES (" + ','.join([
                sql_literal(operation), sql_literal(key), sql_literal(entry['file']), sql_literal(entry['sha256']),
                "'test'", "'APPLIED'", 'UTC_TIMESTAMP()', 'UTC_TIMESTAMP()', sql_literal(operation),
                "'dcc-integration-task'", "'dcc-integration-task'", "b'0'", '0']) + ");\n"
            statements.append(insert.encode())
        done = "SELECT JSON_OBJECT('taskPhase','sql_complete','migrationId'," + sql_literal(key) + ");\n"
        statements.append(done.encode())
    return b''.join(statements)


def verify_prerequisite_receipt(task: Path, integration: Path) -> dict:
    receipt = read_json(task / 'g21-prerequisite-runtime-receipt.json')
    require(receipt.get('status') == 'satisfied_by_fresh_existing_target_facts_not_historical_applied'
            and receipt.get('prerequisiteCount') == 25 and receipt.get('databaseWritesExecuted') is False,
            'Twenty-five exact read-only prerequisite proofs are required')
    require(receipt.get('database') == 'ruoyi-vue-pro' and receipt.get('mysqlVersion') == '8.0.40'
            and receipt.get('sourceContainer') == 'int-ruoyi-mysql' and receipt.get('serverUuid'),
            'Actual prerequisite source identity differs')
    actual_ids = set()
    groups = receipt.get('groups')
    require(isinstance(groups, list) and {row.get('kind') for row in groups} == {'structure', 'bpm'} and len(groups) == 2,
            'Both independent prerequisite proof groups are required')
    for group in groups:
        for field, fingerprint in [('proof', 'proofSha256'), ('facts', 'factsSha256')]:
            relative = Path(group[field])
            require(len(relative.parts) == 1 and relative.name == group[field], 'Proof path escapes its task directory')
            require(sha256_file(task / relative) == group[fingerprint], 'Prerequisite proof/facts changed')
        capture = read_json(task / group['captureReceipt'])
        require(capture.get('databaseWrites') is False and capture['factsSha256'] == group['factsSha256']
                and capture['sqlSha256'] == group['querySha256'], 'Read-only capture receipt differs from proof')
        identity = capture['capture']
        require(identity['database'] == receipt['database'] and identity['serverUuid'] == receipt['serverUuid']
                and identity['version'] == receipt['mysqlVersion'], 'Prerequisite groups came from different runtime targets')
        for field, fingerprint in [('contract', 'contractSha256'), ('query', 'querySha256'), ('validator', 'validatorSha256')]:
            path = (integration / group[field]).resolve(strict=True)
            require(path.is_relative_to(integration.resolve()), 'Owner proof implementation path escaped the integration tree')
            require(sha256_file(path) == group[fingerprint], 'Owner proof implementation changed since validation: ' + field)
        proof = read_json(task / group['proof'])
        required_status = 'SATISFIED_READ_ONLY_FACTS_NOT_APPLIED' if group['kind'] == 'structure' else 'satisfied_by_existing_target_facts'
        require(proof['status'] == required_status and len(proof['prerequisites']) == group['count'], 'Prerequisite proof is not successful')
        ids = {row['migrationId'] for row in proof['prerequisites']}
        require(not ids & actual_ids and len(ids) == group['count'], 'Prerequisite identities overlap or repeat')
        actual_ids |= ids
    require(actual_ids == set(receipt['prerequisiteIds']) and len(actual_ids) == 25, 'External dependency identity coverage differs')
    return receipt


def prepare(task: Path, integration: Path, operation: str, destination: Path) -> dict:
    sql_literal(operation)
    scope = read_json(task / 'g18-approved-scope-candidate.json')
    bundle = validate_bundle(scope, read_json(task / 'g18-migration-package.json'), integration)
    proofs = verify_prerequisite_receipt(task, integration)
    require(set(bundle['externalPrerequisiteIds']) == set(proofs['prerequisiteIds']), 'Proof coverage differs from actual dependencies')
    backup_scope = read_json(task / 'g18-backup-scope.json')
    verified = verify_backups(read_json(task / 'g18-backup-receipt.json'), Path(backup_scope['backupDirectory']))
    destination = destination.resolve()
    require(destination.parent == Path(backup_scope['backupDirectory']).resolve() and not destination.exists(),
            'Execution inputs must be a new directory under the protected task backup directory')
    first = compose_script(scope['executionOrder'], integration / 'IntRuoyiBackend', operation, repeat=False)
    repeat = compose_script(scope['executionOrder'], integration / 'IntRuoyiBackend', operation, repeat=True)
    destination.mkdir()
    for name, payload in [('first.sql', first), ('repeat.sql', repeat)]:
        (destination / name).write_bytes(payload)
    result = {'status': 'prepared_verified_inputs_not_database_execution', 'operationId': operation,
              'databaseWritesExecuted': False, 'writeAuthorizationGranted': False, 'sourceDatabase': scope['sourceDatabase'],
              'rehearsalDatabase': scope['rehearsalDatabase'], 'serverUuid': proofs['serverUuid'], 'executionCount': 19,
              'scopeSha256': sha256_file(task / 'g18-approved-scope-candidate.json'),
              'packageSha256': sha256_file(task / 'g18-migration-package.json'),
              'prerequisiteReceiptSha256': sha256_file(task / 'g21-prerequisite-runtime-receipt.json'),
              'backupReceiptSha256': sha256_file(task / 'g18-backup-receipt.json'),
              'verifiedBackups': verified,
              'scripts': [{'phase': phase, 'path': str(destination / (phase + '.sql')),
                           'sha256': sha256_file(destination / (phase + '.sql'))} for phase in ['first', 'repeat']],
              'originalLedgerRule': 'Preserve every original row; first inserts only task nineteen; repeat contains no ledger writes',
              'recoveryRule': 'Only explicit task-owned new ledger keys may be separately restored; never whole-table ledger restore'}
    (destination / 'manifest.json').write_text(json.dumps(result, indent=2) + '\n', encoding='utf-8')
    return result


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--task-directory', type=Path, default=Path(__file__).resolve().parent)
    parser.add_argument('--integration-root', type=Path, required=True)
    parser.add_argument('--operation-id', required=True)
    parser.add_argument('--output-directory', type=Path, required=True)
    args = parser.parse_args()
    result = prepare(args.task_directory.resolve(strict=True), args.integration_root.resolve(strict=True), args.operation_id, args.output_directory)
    print(json.dumps({key: result[key] for key in ['status', 'operationId', 'executionCount', 'databaseWritesExecuted', 'writeAuthorizationGranted']}))


if __name__ == '__main__': main()
