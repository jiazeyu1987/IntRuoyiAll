"""Migration regression using only disposable MySQL schemas."""
from pathlib import Path
import unittest
import uuid
from test_dcc_file_type_taxonomy_menu_restore_mysql import mysql

ROOT = Path(__file__).resolve().parents[2]
class OptionalTemplateTaxonomyMySQLTest(unittest.TestCase):
    def test_migration_and_unique_constraints(self):
        schema = 'codex_template_' + uuid.uuid4().hex
        mysql(f'CREATE DATABASE `{schema}`;')
        self.addCleanup(lambda: mysql(f'DROP DATABASE `{schema}`;'))
        def run(sql, success=True):
            return mysql(sql, schema, success)
        run((ROOT / 'sql/mysql/20260910_dcc_project_file_template.sql').read_text(encoding='utf-8'))
        run("INSERT INTO dcc_project_file_template_item(project_code_id,file_type_taxonomy_id,file_name,tenant_id) VALUES(1,10,'Existing',1)")
        before = run('SELECT id,project_code_id,file_type_taxonomy_id,file_name,tenant_id FROM dcc_project_file_template_item').stdout
        run((ROOT / 'sql/mysql/20261117_dcc_project_file_template_optional_taxonomy.sql').read_text(encoding='utf-8'))
        self.assertEqual(before, run('SELECT id,project_code_id,file_type_taxonomy_id,file_name,tenant_id FROM dcc_project_file_template_item').stdout)
        def insert(project, tenant, taxonomy, name, success=True):
            return run(f"INSERT INTO dcc_project_file_template_item(project_code_id,file_type_taxonomy_id,file_name,tenant_id) VALUES({project},{taxonomy},'{name}',{tenant})", success)
        insert(1, 1, 'NULL', 'Unclassified')
        self.assertNotEqual(insert(1, 1, 'NULL', 'unclassified', False).returncode, 0)
        insert(2, 1, 'NULL', 'Unclassified')
        insert(1, 2, 'NULL', 'Unclassified')
        run("UPDATE dcc_project_file_template_item SET deleted=1 WHERE project_code_id=1 AND tenant_id=1 AND file_type_taxonomy_id IS NULL")
        insert(1, 1, 'NULL', 'Unclassified')
        self.assertNotEqual(insert(1, 1, '10', 'Existing', False).returncode, 0)

if __name__ == '__main__':
    unittest.main(verbosity=2)
