"""Standalone additive MySQL contract; runtime first/repeat remains Root-owned."""
from pathlib import Path
import re
import unittest

SQL=Path(__file__).resolve().parents[2]/'sql/mysql/20261003_dcc_legacy_source_name_occupancy.sql'

class LegacyNameSchemaContract(unittest.TestCase):
    def setUp(self): self.sql=SQL.read_text(encoding='utf-8')
    def test_exact_three_additive_sidecars_and_no_historical_dml(self):
        self.assertEqual(['dcc_legacy_source_name_scope','dcc_legacy_source_name_evidence','dcc_source_name_reservation'],re.findall(r'CREATE TABLE IF NOT EXISTS (\w+)',self.sql))
        self.assertNotRegex(re.sub(r'--[^\n]*','',self.sql),r'\b(?:UPDATE|INSERT|DELETE|DROP|ALTER|REPLACE|TRUNCATE)\b')
    def test_full_exact_generated_key_and_unique_namespace(self):
        self.assertEqual(2,len(re.findall(r'VARBINARY\(1024\) GENERATED ALWAYS AS \(CONVERT\(source_original_file_name USING BINARY\)\) STORED',self.sql)))
        self.assertIn('UNIQUE KEY uk_dcc_source_name_registry (tenant_id, source_name_key)',self.sql)
        self.assertNotRegex(self.sql,r'LOWER\(|TRIM\(|source_name_key\(\d+\)')
    def test_microsecond_timestamp_defaults_match_column_precision(self):
        self.assertEqual(8,len(re.findall(r'\bDATETIME\(6\)',self.sql)))
        self.assertEqual(2,self.sql.count('DEFAULT CURRENT_TIMESTAMP(6)'))
        self.assertNotRegex(self.sql,r'\bDATETIME\b(?!\(6\))|CURRENT_TIMESTAMP(?!\(6\))')
    def test_explicit_character_index_engine_and_real_proof_checks(self):
        self.assertEqual(3,self.sql.count('ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin'))
        self.assertEqual(7,len(re.findall(r'\bCHECK\s*\(',self.sql)))
        self.assertIn("bytes_status='MATCH' AND expected_sha256=actual_sha256 AND expected_size=actual_size",self.sql)
        for column in ['source_path','storage_type','storage_endpoint','storage_bucket','storage_region','storage_path_style']:
            self.assertRegex(self.sql,rf'\b{column}\b')
    def test_metadata_dependencies_remain_separate_from_frozen19(self):
        self.assertTrue(self.sql.startswith('-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260930_dcc_c_revision_identity,20260811_dcc_source_ownership; type=schema; riskLevel=medium'))

if __name__=='__main__':unittest.main()
