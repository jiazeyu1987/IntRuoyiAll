import importlib.util
import json
import subprocess
import unittest
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
MIGRATION = ROOT / 'sql/mysql/20261125_mes_pqc_aggregate_production_submit_nullable.sql'
CONTAINER = 'pqc-null-contract-20261125-0033'


class PqcAggregateNullableMysqlTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        result = subprocess.run(
            ['docker', 'inspect', '--format', '{{json .Config.Labels}}', CONTAINER],
            check=True, capture_output=True, text=True)
        labels = json.loads(result.stdout)
        if labels.get('codex.task') != 'pqc-null-contract-20261125':
            raise RuntimeError('Dedicated migration-test container ownership mismatch')
        version = cls.query('SELECT VERSION();', database=False).stdout.strip()
        if version != '8.0.39':
            raise RuntimeError('Expected isolated MySQL 8.0.39: ' + version)
        print('MYSQL_VERSION=' + version, flush=True)

    @classmethod
    def query(cls, sql, database=True, check=True):
        command = ['docker', 'exec', '-i', CONTAINER, 'mysql', '-uroot', '--batch',
                   '--raw', '--skip-column-names', '--default-character-set=utf8mb4']
        if database:
            command.append('pqc_nullable_contract')
        result = subprocess.run(command, input=sql, text=True, encoding='utf-8', capture_output=True)
        if check and result.returncode:
            raise AssertionError(result.stderr)
        return result

    def setUp(self):
        self.query('DROP DATABASE IF EXISTS pqc_nullable_contract; '
                   'CREATE DATABASE pqc_nullable_contract CHARACTER SET utf8mb4;', database=False)
        self.query('CREATE TABLE mes_pro_process_pool_pqc_record '
                   '(id bigint PRIMARY KEY, production_submit_event_id bigint NULL) ENGINE=InnoDB; '
                   'CREATE TABLE mes_pqc_process_inspection_aggregate_detail '
                   '(id bigint PRIMARY KEY, production_submit_event_id bigint NOT NULL, '
                   'UNIQUE KEY production_link (production_submit_event_id)) ENGINE=InnoDB; '
                   'CREATE TABLE review_transaction_probe (id bigint PRIMARY KEY) ENGINE=InnoDB; '
                   'INSERT INTO mes_pqc_process_inspection_aggregate_detail VALUES (1,5001);')

    def apply(self, check=True):
        return self.query(MIGRATION.read_text(encoding='utf-8'), check=check)

    def test_null_link_existing_link_and_idempotency(self):
        self.apply()
        self.apply()
        self.query('INSERT INTO mes_pqc_process_inspection_aggregate_detail (id) VALUES (2); '
                   'INSERT INTO mes_pqc_process_inspection_aggregate_detail VALUES (3,5002);')
        self.assertEqual('1\t5001\n2\tNULL\n3\t5002', self.query(
            'SELECT id,production_submit_event_id FROM mes_pqc_process_inspection_aggregate_detail ORDER BY id;'
        ).stdout.strip())
        self.assertEqual('YES', self.query(
            "SELECT IS_NULLABLE FROM information_schema.columns WHERE TABLE_SCHEMA=DATABASE() "
            "AND TABLE_NAME='mes_pqc_process_inspection_aggregate_detail' "
            "AND COLUMN_NAME='production_submit_event_id';").stdout.strip())

    def test_actual_sql_failure_rolls_back_transaction_without_weakening_constraints(self):
        self.apply()
        failure = self.query('START TRANSACTION; '
                             'INSERT INTO review_transaction_probe VALUES (10); '
                             'INSERT INTO mes_pqc_process_inspection_aggregate_detail (id) VALUES (2); '
                             'INSERT INTO mes_pqc_process_inspection_aggregate_detail VALUES (1,9001); '
                             'COMMIT;', check=False)
        self.assertNotEqual(0, failure.returncode)
        self.assertIn('Duplicate entry', failure.stderr)
        self.assertEqual('0', self.query('SELECT COUNT(*) FROM review_transaction_probe;').stdout.strip())
        self.assertEqual('1\t5001', self.query(
            'SELECT id,production_submit_event_id FROM mes_pqc_process_inspection_aggregate_detail;'
        ).stdout.strip())

    def test_source_contract_mismatch_fails_without_relaxing_target(self):
        self.query('ALTER TABLE mes_pro_process_pool_pqc_record '
                   'MODIFY production_submit_event_id bigint NOT NULL;')
        failure = self.apply(check=False)
        self.assertNotEqual(0, failure.returncode)
        self.assertIn('PQC_SOURCE_PRODUCTION_SUBMIT_CONTRACT_MISMATCH', failure.stderr)
        self.assertEqual('NO', self.query(
            "SELECT IS_NULLABLE FROM information_schema.columns WHERE TABLE_SCHEMA=DATABASE() "
            "AND TABLE_NAME='mes_pqc_process_inspection_aggregate_detail' "
            "AND COLUMN_NAME='production_submit_event_id';").stdout.strip())

    def test_release_metadata_and_dependencies(self):
        path = ROOT / 'script/release/release_migration_manifest.py'
        spec = importlib.util.spec_from_file_location('migration_manifest', path)
        module = importlib.util.module_from_spec(spec)
        spec.loader.exec_module(module)
        dependencies = [
            ROOT / 'sql/mysql/20260805_mes_pqc_process_inspection_aggregate_runtime_closure.sql',
            ROOT / 'sql/mysql/20260808_mes_pqc_record_production_submit_nullable.sql',
            ROOT / 'sql/mysql/20260805_mes_process_pool_ac_m20_pqc_review_closure.sql',
            ROOT / 'sql/mysql/20260805_mes_pqc_process_inspection_aggregate_detail.sql',
            ROOT / 'sql/mysql/20260803_mes_process_pool_pqc_structured_binding.sql',
        ]
        metadata = module._parse_metadata(MIGRATION)
        self.assertTrue(metadata['requiresTargetPreflight'])
        self.assertEqual('schema', metadata['type'])
        self.assertTrue(all(dependency.is_file() for dependency in dependencies))

    def test_target_preflight_passes_and_rejects_mismatched_source(self):
        preflight = ROOT / 'sql/mysql/target-preflight/20261125_mes_pqc_aggregate_production_submit_nullable.preflight.sql'
        self.assertIn('TARGET_PREFLIGHT_PASS:', self.query(preflight.read_text(encoding='utf-8')).stdout)
        self.query('ALTER TABLE mes_pro_process_pool_pqc_record '
                   'MODIFY production_submit_event_id bigint NOT NULL;')
        self.assertIn('TARGET_PREFLIGHT_BLOCKED:', self.query(preflight.read_text(encoding='utf-8')).stdout)


if __name__ == '__main__':
    unittest.main(verbosity=2)
