"""Offline contract tests. No database API/connection and no business actions."""
import copy
import gzip
import hashlib
import json
from pathlib import Path
import tempfile
import unittest

from g21_migration_scope import ScopeError, validate_bundle, verify_backups

TASK = Path(__file__).resolve().parent
ROOT = Path('C:/IntRuoyi/20261001-dcc-integration')


class ScopeTests(unittest.TestCase):
    def setUp(self):
        self.scope = json.loads((TASK / 'g18-approved-scope-candidate.json').read_text(encoding='utf-8-sig'))
        self.package = json.loads((TASK / 'g18-migration-package.json').read_text(encoding='utf-8-sig'))

    def test_real_bundle_has_only_nineteen_and_never_allows_write_connection(self):
        result = validate_bundle(self.scope, self.package, ROOT)
        self.assertEqual((result['executableCount'], result['externalPrerequisiteCount'], result['minimalClosureCount']), (19, 25, 44))
        self.assertFalse(result['writeConnectionAllowed'])
        self.assertFalse(result['databaseWritesExecuted'])
        self.assertNotIn('20261003_dcc_controlled_file_activation_job_registration', [x['migrationId'] for x in result['executionOrder']])

    def test_extra_or_repeated_identity_is_rejected(self):
        for kind in ['extra', 'duplicate']:
            scope = copy.deepcopy(self.scope)
            if kind == 'extra':
                scope['executionOrder'] = self.package['executionOrder']
                scope['candidateExecutionCount'] = 45
            else:
                scope['executionOrder'][-1] = scope['executionOrder'][0]
            with self.assertRaises(ScopeError):
                validate_bundle(scope, self.package, ROOT)

    def test_path_traversal_and_checksum_drift_reject(self):
        for field, bad in [('file', '../config/gxp-audit-policy.yaml'), ('sha256', '0' * 64)]:
            package = copy.deepcopy(self.package)
            selected = self.scope['executionOrder'][0]['migrationId']
            for item in package['executionOrder']:
                if item['migrationId'] == selected:
                    item[field] = bad
            scope = copy.deepcopy(self.scope)
            scope['executionOrder'][0][field] = bad
            with self.assertRaises(ScopeError):
                validate_bundle(scope, package, ROOT)

    def test_reordered_actual_dependencies_reject(self):
        scope = copy.deepcopy(self.scope)
        a = next(i for i, x in enumerate(scope['executionOrder']) if x['migrationId'] == '20260930_dcc_c_revision_identity')
        b = next(i for i, x in enumerate(scope['executionOrder']) if x['migrationId'] == '20261002_dcc_c_initial_candidate_identity')
        scope['executionOrder'][a], scope['executionOrder'][b] = scope['executionOrder'][b], scope['executionOrder'][a]
        with self.assertRaises(ScopeError):
            validate_bundle(scope, self.package, ROOT)

    def test_missing_dependency_forged_metadata_and_semantic_equivalence_reject(self):
        for kind in ['missing', 'metadata', 'equivalence']:
            package = copy.deepcopy(self.package)
            if kind == 'missing':
                package['executionOrder'][0]['migrationId'] = 'invented_dependency'
            elif kind == 'metadata':
                package['executionOrder'][0]['dependsOn'] = ['invented_dependency']
            else:
                package['executionOrder'][0]['acceptedEquivalentSha256'] = ['0' * 64]
            with self.assertRaises(ScopeError):
                validate_bundle(self.scope, package, ROOT)

    def test_wrong_source_database_rehearsal_or_ledger_scope_reject(self):
        for field, bad in [('sourceDatabase', 'other'), ('sourceContainer', 'other'), ('rehearsalDatabase', 'ruoyi-vue-pro')]:
            scope = copy.deepcopy(self.scope); scope[field] = bad
            with self.assertRaises(ScopeError):
                validate_bundle(scope, self.package, ROOT)
        scope = copy.deepcopy(self.scope)
        scope['ledgerAppendBoundary']['preserveAllExistingRows'] = False
        with self.assertRaises(ScopeError):
            validate_bundle(scope, self.package, ROOT)

    def test_same_table_count_cannot_hide_an_unrelated_recovery_target(self):
        scope = copy.deepcopy(self.scope)
        scope['existingTablesAffected'][0] = 'system_users'
        with self.assertRaises(ScopeError):
            validate_bundle(scope, self.package, ROOT)

    def test_backup_checksum_and_directory_boundary_are_real_file_checks(self):
        with tempfile.TemporaryDirectory() as directory:
            base = Path(directory)
            receipt = {'database': 'ruoyi-vue-pro', 'tableCount': 210, 'databaseWrites': False, 'credentialsExported': False, 'artifacts': []}
            for kind in ['schema', 'data', 'affected']:
                p = base / (kind + '.sql.gz'); raw = ('-- task fixture ' + kind).encode()
                with gzip.open(p, 'wb') as output:
                    output.write(raw)
                receipt['artifacts'].append({'kind': kind, 'path': str(p), 'bytes': p.stat().st_size, 'uncompressedBytes': len(raw), 'sha256': hashlib.sha256(p.read_bytes()).hexdigest(), 'dumpExitCode': 0, 'gzipIntegrity': 'PASS'})
            self.assertEqual(len(verify_backups(receipt, base)), 3)
            forged = copy.deepcopy(receipt); forged['artifacts'][0]['sha256'] = '0' * 64
            with self.assertRaises(ScopeError):
                verify_backups(forged, base)
            with self.assertRaises(ScopeError):
                verify_backups(receipt, base.parent)


if __name__ == '__main__':
    unittest.main(verbosity=2)
