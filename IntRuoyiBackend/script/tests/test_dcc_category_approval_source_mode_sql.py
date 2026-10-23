from pathlib import Path
import re
import unittest

SQL = Path(__file__).resolve().parents[2] / 'sql/mysql/20261015_dcc_category_approval_source_mode.sql'

class CategoryApprovalSourceMigrationTest(unittest.TestCase):
    def setUp(self):
        self.sql = SQL.read_text(encoding='utf-8')

    def test_metadata_has_real_existing_dependency(self):
        self.assertRegex(self.sql, r'-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260719_dcc_file_type_taxonomy; type=schema; riskLevel=medium')

    def test_add_column_is_information_schema_guarded_without_runtime_default(self):
        self.assertIn('information_schema.COLUMNS', self.sql)
        self.assertNotRegex(self.sql, r'ADD COLUMN IF NOT EXISTS|DEFAULT\s+[\x27\x22]PUBLIC_MATRIX')
        guard = self.sql.index('IF source_column_count = 0 THEN')
        add = self.sql.index('ADD COLUMN `approval_source_mode`')
        self.assertLess(guard, add)
        self.assertRegex(self.sql, r'ADD COLUMN `approval_source_mode` VARCHAR\(32\) NULL')

    def test_only_new_column_branch_configures_existing_rows_once(self):
        block = self.sql[self.sql.index('IF source_column_count = 0 THEN'):self.sql.index('ELSE', self.sql.index('IF source_column_count = 0 THEN'))]
        self.assertRegex(block, r'UPDATE `dcc_file_category`\s+SET `approval_source_mode` = \'PUBLIC_MATRIX\'\s+WHERE `approval_source_mode` IS NULL;')
        self.assertEqual(len(re.findall(r'\bUPDATE\s+`', self.sql, re.I)), 1)
        self.assertNotIn("SET `approval_source_mode` = 'APPLICATION_CUSTOM'", self.sql)

    def test_existing_column_branch_rejects_unknown_and_never_overwrites_custom(self):
        self.assertIn('CATEGORY_APPROVAL_SOURCE_SCHEMA_INVALID', self.sql)
        self.assertIn('CATEGORY_APPROVAL_SOURCE_VALUE_INVALID', self.sql)
        self.assertRegex(self.sql, r'BINARY `approval_source_mode` NOT IN \(\x27PUBLIC_MATRIX\x27, \x27APPLICATION_CUSTOM\x27\)')
        self.assertIn("SIGNAL SQLSTATE '45000'", self.sql)

    def test_only_category_is_a_mutation_target(self):
        targets = re.findall(r'\b(?:ALTER TABLE|UPDATE|INSERT INTO|DELETE FROM)\s+`([^`]+)`', self.sql, re.I)
        self.assertTrue(targets)
        self.assertEqual(set(targets), {'dcc_file_category'})
        self.assertNotRegex(self.sql, r'\b(?:TRUNCATE|DROP TABLE)\b')

    def test_final_shape_is_not_null_without_default_and_procedure_is_called(self):
        self.assertRegex(self.sql, r'MODIFY COLUMN `approval_source_mode` VARCHAR\(32\) NOT NULL')
        self.assertIn('CALL migrate_dcc_category_approval_source_mode();', self.sql)
        self.assertGreater(self.sql.rindex('DROP PROCEDURE IF EXISTS migrate_dcc_category_approval_source_mode'), self.sql.index('CALL migrate_dcc_category_approval_source_mode();'))

if __name__ == '__main__':
    unittest.main()
