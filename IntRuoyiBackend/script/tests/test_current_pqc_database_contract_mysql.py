import json
import argparse
import sys
import unittest
import importlib
from pathlib import Path
import test_pqc_patrol_rule_mysql as migration_tests

parser = argparse.ArgumentParser(add_help=False)
parser.add_argument('--writer-evidence-dir', type=Path)
parser.add_argument('--maintenance-root', type=Path)
arguments, remaining = parser.parse_known_args()
SQL = migration_tests.SQL
CONTRACT = SQL / 'database-contracts/mes-pqc-rule-key.sql'
MARKER = 'DATABASE_CONTRACT_PASS:mes-pqc-rule-key'

class CurrentContractMysqlTest(migration_tests.PatrolRuleMysqlTest):
    def current(self):
        return self.query(CONTRACT.read_text(encoding='utf-8')).stdout.strip()

    def test_current_contract_rejects_old_then_accepts_upgrade_and_rechecks_drift(self):
        self.old_check()
        self.assertNotEqual(MARKER, self.current())
        before = self.snapshot()
        self.migration()
        self.assertEqual(MARKER, self.current())
        self.assertEqual(before, self.snapshot())
        self.migration()
        self.assertEqual(MARKER, self.current())
        self.query('ALTER TABLE mes_pqc_inspection_task ALTER CHECK chk_mes_pqc_inspection_rule_key NOT ENFORCED;')
        self.assertNotEqual(MARKER, self.current())
        self.assertEqual(before, self.snapshot())

    def test_current_contract_rejects_missing_and_wrong_columns(self):
        self.assertNotEqual(MARKER, self.current())
        self.migration()
        self.query('ALTER TABLE mes_pqc_inspection_task MODIFY inspection_rule_key VARCHAR(128) NOT NULL;')
        self.assertNotEqual(MARKER, self.current())

    def test_current_contract_rejects_narrowed_and_widened_checks(self):
        for values in ["'FIRST','PATROL','FINAL'", "'FIRST','PATROL','PATROL_AM','PATROL_PM','FINAL','BOGUS'",
                       "'FIRST','P ATROL','PATROL_AM','PATROL_PM','FINAL'",
                       "'FIRST','PAT(ROL)','PATROL_AM','PATROL_PM','FINAL'",
                       "'FIRST','PAT_utf8mb4ROL','PATROL_AM','PATROL_PM','FINAL'"]:
            self.setUp()
            self.query('DELETE FROM mes_pqc_inspection_task;')
            self.query('ALTER TABLE mes_pqc_inspection_task ADD CONSTRAINT chk_mes_pqc_inspection_rule_key CHECK (inspection_rule_key IN ('+values+'));')
            self.assertNotEqual(MARKER, self.current())

    def test_additional_same_column_check_is_blocked_but_unrelated_check_is_allowed(self):
        self.migration()
        self.query('ALTER TABLE mes_pqc_inspection_task ADD CONSTRAINT chk_other_quantity CHECK (planned_inspection_quantity > 0);')
        self.assertEqual(MARKER, self.current())
        self.query("ALTER TABLE mes_pqc_inspection_task ADD CONSTRAINT chk_unknown_patrol CHECK (inspection_rule_key <> 'PATROL');")
        self.assert_rejected(self.patrol_insert())
        self.assertNotEqual(MARKER, self.current())

    def test_actual_java_writer_values_match_current_mysql_contract(self):
        self.assertIsNotNone(arguments.writer_evidence_dir, 'Run Maven writer regressions first and provide --writer-evidence-dir; no synthetic replacement permitted')
        self.migration()
        observed = set()
        for source, expected in [('active-order', {'FIRST','PATROL'}), ('patrol-round', {'PATROL'}), ('final-inspection', {'FINAL'})]:
            values = json.loads((arguments.writer_evidence_dir / (source+'.json')).read_text(encoding='utf-8'))
            self.assertEqual(expected, set(values))
            for value in values:
                self.assertRegex(value, r'^[A-Z_]+$')
                # Insert a complete real-schema task using the value captured at the Java mapper boundary.
                self.query(self.patrol_insert().replace("'PATROL'", "'"+value+"'"))
                self.assertEqual(MARKER, self.current())
                self.query('DELETE FROM mes_pqc_inspection_task WHERE id=101;')
                observed.add(value)
        self.assertEqual({'FIRST','PATROL','FINAL'}, observed)
        self.assert_rejected(self.patrol_insert().replace("'PATROL'", "'ILLEGAL'"))

    def test_maintenance_generic_verifier_against_real_mysql(self):
        self.assertIsNotNone(arguments.maintenance_root, 'Provide --maintenance-root for the actual maintenance module; no copied verifier permitted')
        sys.path.insert(0, str(arguments.maintenance_root.resolve()))
        verifier = importlib.import_module('ops.release.release_database_contract')
        root = SQL.parents[1]
        descriptor, artifacts = verifier.collect(root)
        entries = [descriptor['catalog'], *descriptor['checks']]
        self.assertEqual({entry['file'] for entry in entries}, set(artifacts))
        manifest = {'databaseContract': descriptor, 'files': [
            {'name': entry['file'], 'sha256': entry['sha256']} for entry in entries]}
        execute = lambda sql: self.query(sql).stdout
        self.old_check()
        before = self.snapshot()
        with self.assertRaisesRegex(ValueError, 'mes-pqc-rule-key'):
            verifier.verify(root, manifest, execute)
        print('GENERIC_VERIFIER_OLD_CHECK_BLOCKED', flush=True)
        self.migration()
        result = verifier.verify(root, manifest, execute)
        self.assertEqual('PASS', result['status'])
        self.assertEqual(before, self.snapshot())
        print('GENERIC_VERIFIER_UPGRADED_PASS', json.dumps(result), flush=True)
        self.query("ALTER TABLE mes_pqc_inspection_task ADD CONSTRAINT chk_extra_patrol CHECK (inspection_rule_key <> 'PATROL');")
        with self.assertRaisesRegex(ValueError, 'mes-pqc-rule-key'):
            verifier.verify(root, manifest, execute)
        self.assertEqual(before, self.snapshot())
        print('GENERIC_VERIFIER_EXTRA_CHECK_BLOCKED', flush=True)

if __name__ == '__main__':
    unittest.main(argv=[sys.argv[0]]+remaining, verbosity=2)
