from pathlib import Path
import re
import unittest
SQL=Path(__file__).resolve().parents[2]/'sql/mysql/20261015_dcc_application_custom_matrix.sql'
class CustomApplicationDdlTest(unittest.TestCase):
    def setUp(self): self.sql=SQL.read_text(encoding='utf-8')
    def test_three_exact_nullable_fields_are_guarded(self):
        for name,shape in [('draft_approval_source_mode','varchar(32)'),('draft_application_custom_matrix_json','longtext'),('draft_application_context_key','varchar(128)')]:
            self.assertIn("CALL ensure_dcc_application_custom_column('"+name+"','"+shape+"');",self.sql)
        self.assertIn('information_schema.COLUMNS',self.sql)
        self.assertIn("COLUMN_TYPE=expected_type",self.sql)
        self.assertIn("IS_NULLABLE='YES' AND COLUMN_DEFAULT IS NULL",self.sql)
        self.assertIn('CUSTOM_APPLICATION_COLUMN_DRIFT',self.sql)
    def test_route_version_only_nullability_changes_no_public_default(self):
        self.assertIn('MODIFY COLUMN `route_version_no` INT NULL;',self.sql)
        self.assertIn("AND IS_NULLABLE='NO'",self.sql)
        self.assertIn('CUSTOM_APPLICATION_ROUTE_VERSION_DRIFT',self.sql)
        self.assertIn('CUSTOM_APPLICATION_ROUTE_VERSION_NOT_NULLABLE',self.sql)
        self.assertNotRegex(self.sql,r'\bDEFAULT\s+(?:1|\x27PUBLIC_MATRIX\x27)')
    def test_no_business_data_rewrite_or_public_matrix_mutation(self):
        text='\n'.join(line for line in self.sql.splitlines() if not line.lstrip().startswith('--'))
        self.assertNotRegex(text,r'\b(?:UPDATE|INSERT|DELETE|TRUNCATE)\b')
        self.assertNotIn('ADD COLUMN IF NOT EXISTS',text)
        self.assertNotIn('dcc_category_approval_route',text)
    def test_dependency_and_guarded_procedure_invocation(self):
        self.assertIn('dependsOn=20261015_dcc_category_approval_source_mode,20261012_dcc_application_execution_contract',self.sql)
        self.assertEqual(len(re.findall(r"^CALL ensure_dcc_application_custom_column",self.sql,re.M)),3)
        self.assertIn('CALL ensure_dcc_custom_route_version_nullable();',self.sql)
        self.assertIn("SIGNAL SQLSTATE '45000'",self.sql)
if __name__=='__main__':unittest.main()
