"""Validate this task's exact migration bundle offline. This module cannot write a database."""
from __future__ import annotations

import argparse
import gzip
import hashlib
import json
from pathlib import Path
import re
import sys

EXPECTED_IDS = frozenset({
    '20260910_dcc_access_log_reason_capacity', '20260930_dcc_a_lifecycle',
    '20260930_dcc_a_workflow_bpmn_v4', '20260930_dcc_a_signature_workflow_round',
    '20260930_dcc_a_future_obsolete_policy', '20260930_dcc_b_project_attributes_folders',
    '20260906_dcc_new_file_lifecycle_p1', '20260930_dcc_c_revision_identity',
    '20260930_dcc_d_notification_template', '20260930_dcc_d_relations',
    '20261001_dcc_application_round_link', '20261001_dcc_b_manual_project_folder_origin',
    '20261001_dcc_b_project_file_placement', '20261001_dcc_b_project_product_audit_intent',
    '20261001_dcc_b_reserved_draft_round', '20261002_dcc_c_initial_candidate_identity',
    '20261003_dcc_approval_file_owner_snapshot', '20261003_dcc_project_reviewer_configuration',
    '20261003_dcc_revision_failed_attempt_identity',
})
BACKUP_KINDS = frozenset({'schema', 'data', 'affected'})


class ScopeError(ValueError):
    pass


def read_json(path: Path) -> dict:
    value = json.loads(path.read_text(encoding='utf-8-sig'))
    if not isinstance(value, dict):
        raise ScopeError('Expected a JSON object: ' + str(path))
    return value


def require(condition: bool, reason: str) -> None:
    if not condition:
        raise ScopeError(reason)


def sha256_file(path: Path) -> str:
    with path.open('rb') as source:
        return hashlib.file_digest(source, 'sha256').hexdigest()


def verify_backups(receipt: dict, backup_directory: Path) -> list[dict]:
    require(receipt.get('database') == 'ruoyi-vue-pro' and receipt.get('tableCount') == 210,
            'Backup source database/table scope differs')
    require(receipt.get('databaseWrites') is False and receipt.get('credentialsExported') is False,
            'Read-only protected backup receipt is required')
    rows = receipt.get('artifacts')
    require(isinstance(rows, list) and len(rows) == 3, 'Exactly three protected backup artifacts are required')
    require({row.get('kind') for row in rows} == BACKUP_KINDS, 'Backup kinds are missing or repeated')
    directory = backup_directory.resolve(strict=True)
    verified = []
    for row in rows:
        path = Path(row['path']).resolve(strict=True)
        require(path.parent == directory, 'Backup payload is outside the approved task directory')
        require(path.stat().st_size == row.get('bytes') and sha256_file(path) == row.get('sha256'),
                'Backup payload size/checksum changed: ' + row['kind'])
        require(row.get('dumpExitCode') == 0 and row.get('gzipIntegrity') == 'PASS', 'Backup dump failed')
        size = 0
        with gzip.open(path, 'rb') as payload:
            while block := payload.read(1024 * 1024):
                size += len(block)
        require(size > 0 and size == row.get('uncompressedBytes'), 'Backup expansion size/integrity changed')
        verified.append({'kind': row['kind'], 'sha256': row['sha256'], 'bytes': row['bytes']})
    return verified


def validate_bundle(scope: dict, package: dict, integration_root: Path) -> dict:
    require(scope.get('sourceDatabase') == 'ruoyi-vue-pro' and scope.get('sourceContainer') == 'int-ruoyi-mysql',
            'Only the existing local source database/container is in scope')
    require(scope.get('rehearsalDatabase') == 'dcc_intqms_g18_rehearsal', 'Rehearsal database is not the fixed task database')
    selected = scope.get('executionOrder')
    require(isinstance(selected, list) and len(selected) == scope.get('candidateExecutionCount') == 19,
            'Execution scope must contain exactly nineteen SQL files')
    ids = [row.get('migrationId') for row in selected]
    require(len(set(ids)) == 19 and set(ids) == EXPECTED_IDS, 'Execution identity whitelist differs or contains duplicates')
    require(len(scope.get('existingTablesAffected', [])) == 16 and len(set(scope['existingTablesAffected'])) == 16,
            'Original affected-table scope changed')
    require(len(scope.get('newTables', [])) == 17 and len(set(scope['newTables'])) == 17, 'New-table scope changed')
    require(not set(scope['existingTablesAffected']) & set(scope['newTables']), 'New and original tables overlap')
    ledger = scope.get('ledgerAppendBoundary', {})
    require(set(ledger.get('newMigrationIds', [])) == EXPECTED_IDS and len(ledger['newMigrationIds']) == 19,
            'Task ledger append identities differ')
    require(all(ledger.get(key) is True for key in ['preserveAllExistingRows', 'repeatDoesNotRewriteLedger', 'restoreOnlyExactTaskOwnedNewKeys']),
            'Original ledger preservation/repeat/recovery boundary is missing')
    require(package.get('policyGate') == 'passed' and package.get('closureCount') == 45,
            'The complete official dependency-policy package is required')
    entries = package.get('executionOrder')
    require(isinstance(entries, list) and len(entries) == 45, 'Dependency package count differs')
    mapping = {row.get('migrationId'): row for row in entries}
    require(len(mapping) == 45, 'Dependency package contains repeated identities')
    backend = (integration_root / 'IntRuoyiBackend').resolve(strict=True)
    sql_root = (backend / 'sql/mysql').resolve(strict=True)
    sys.path.insert(0, str(backend))
    from script.release.release_migration_manifest import _parse_metadata

    seen = set()
    active = set()
    ordered_closure = []

    def visit(key: str) -> None:
        require(key in mapping, 'Missing formal dependency: ' + key)
        require(key not in active, 'Dependency cycle: ' + key)
        if key in seen:
            return
        active.add(key)
        entry = mapping[key]
        relative = entry.get('file')
        require(isinstance(relative, str) and re.fullmatch(r'sql/mysql/[A-Za-z0-9_-]+\.sql', relative) is not None,
                'SQL path is not an exact repository-relative migration file')
        path = (backend / relative).resolve(strict=True)
        require(path.parent == sql_root and path.stem == key, 'SQL path escapes the approved root or identity differs')
        require(sha256_file(path) == entry.get('sha256'), 'Frozen migration checksum changed: ' + key)
        text = path.read_text(encoding='utf-8-sig')
        require(re.search(r'^\s*--\s*release-migration\s*:', text, re.MULTILINE) is not None,
                'Explicit official migration metadata is missing')
        require('acceptedEquivalentSha256' not in entry, 'Historical semantic files cannot be made checksum-equivalent')
        metadata = _parse_metadata(path)
        for field in ['dependsOn', 'type', 'riskLevel', 'allowedEnvironments']:
            require(metadata[field] == entry.get(field), 'Frozen metadata differs from official SQL: ' + key + '.' + field)
        require('test' in metadata['allowedEnvironments'], 'Migration excludes the actual test target')
        for dependency in metadata['dependsOn']:
            visit(dependency)
        active.remove(key)
        seen.add(key)
        ordered_closure.append(key)

    for row in selected:
        key = row['migrationId']
        require(row == mapping.get(key), 'Candidate differs from the sealed official package: ' + key)
        visit(key)
    external = seen - EXPECTED_IDS
    require(len(seen) == 44 and len(external) == 25, 'Actual executable dependency closure must be nineteen plus twenty-five')
    positions = {key: n for n, key in enumerate(ids)}
    for key in ids:
        for dependency in mapping[key]['dependsOn']:
            require(dependency not in positions or positions[dependency] < positions[key], 'Candidate dependency runs after its consumer')
    actual_tables = set()
    for row in selected:
        source = (backend / row['file']).read_text(encoding='utf-8-sig')
        actual_tables.update(re.findall(
            r'(?:ALTER\s+TABLE|CREATE\s+TABLE(?:\s+IF\s+NOT\s+EXISTS)?|INSERT\s+INTO)\s+`?((?:dcc|act|bpm|system)_[A-Za-z0-9_]+)', source, re.IGNORECASE))
    require(actual_tables == set(scope['existingTablesAffected']) | set(scope['newTables']),
            'Affected tables differ from the nineteen frozen formal SQL files')
    return {'status': 'offline_scope_verified_external_facts_pending', 'databaseWritesExecuted': False,
            'executableCount': 19, 'externalPrerequisiteCount': 25, 'minimalClosureCount': 44,
            'executionOrder': selected, 'externalPrerequisiteIds': sorted(external),
            'writeConnectionAllowed': False}


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--integration-root', type=Path, required=True)
    parser.add_argument('--task-directory', type=Path, default=Path(__file__).resolve().parent)
    parser.add_argument('--output', type=Path)
    args = parser.parse_args()
    task = args.task_directory.resolve(strict=True)
    scope = read_json(task / 'g18-approved-scope-candidate.json')
    result = validate_bundle(scope, read_json(task / 'g18-migration-package.json'), args.integration_root)
    backup_scope = read_json(task / 'g18-backup-scope.json')
    result['verifiedBackups'] = verify_backups(read_json(task / 'g18-backup-receipt.json'), Path(backup_scope['backupDirectory']))
    payload = json.dumps(result, ensure_ascii=False, indent=2) + '\n'
    if args.output:
        args.output.write_text(payload, encoding='utf-8')
    print(json.dumps({key: result[key] for key in ['status', 'executableCount', 'externalPrerequisiteCount', 'writeConnectionAllowed']}))


if __name__ == '__main__':
    main()
