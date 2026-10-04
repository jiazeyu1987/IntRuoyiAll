"""B migration/schema contracts. Reads source files only; never connects to a database."""
from pathlib import Path
import sys
import unittest

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT))
from script.release.release_migration_manifest import _parse_metadata

class DccBSchemaContract(unittest.TestCase):
    def setUp(self):
        self.migration = ROOT / "sql/mysql/20260930_dcc_b_project_attributes_folders.sql"
        self.sql = self.migration.read_text(encoding="utf-8")
        self.schema = (ROOT / "yudao-module-dcc/src/test/resources/sql/create_tables.sql").read_text(encoding="utf-8")
        self.clean = (ROOT / "yudao-module-dcc/src/test/resources/sql/clean.sql").read_text(encoding="utf-8")

    def test_metadata_declares_real_dependencies(self):
        metadata = _parse_metadata(self.migration)
        self.assertEqual(metadata["type"], "schema")
        self.assertEqual(metadata["dependsOn"], ["20260920_dcc_project_product_create_approval", "20260910_dcc_project_file_template"])
        for name in metadata["dependsOn"]:
            self.assertTrue((ROOT / "sql/mysql" / (name + ".sql")).is_file())

    def test_history_has_no_business_backfill(self):
        upper = self.sql.upper()
        for text in ["INSERT INTO", "UPDATE DCC_PROJECT_CODE", "UPDATE DCC_CONTROLLED_FILE", "DELETE FROM", "TRUNCATE"]:
            self.assertNotIn(text, upper)
        for column in ["project_leader_user_id", "default_attributes_json", "folder_template_id", "folder_template_snapshot_json"]:
            self.assertIn("column_name = '" + column + "'", self.sql)
            self.assertIn(column, self.schema)

    def test_tables_and_cleanup_are_aligned(self):
        for table in ["dcc_project_application_attributes", "dcc_folder_template", "dcc_folder_template_history", "dcc_project_folder"]:
            self.assertIn("CREATE TABLE IF NOT EXISTS " + table, self.sql)
            self.assertIn("CREATE TABLE IF NOT EXISTS " + table, self.schema)
            self.assertIn("DELETE FROM " + table + ";", self.clean)
        self.assertIn("UNIQUE(tenant_id, application_type, application_id, application_round)", self.sql)
        self.assertIn("UNIQUE(tenant_id, project_code_id, source_node_key)", self.sql)

    def test_application_snapshots_do_not_write_public_file_models(self):
        source = (ROOT / "yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/projectcode/attributes/DccProjectAttributesService.java").read_text(encoding="utf-8")
        self.assertNotIn("DccControlledFileMapper", source)
        self.assertNotIn("DccControlledFileDO", source)
        self.assertIn('"OBSOLETE"', source)
        self.assertIn("getDefaultSourceJson", source)
        self.assertIn("getActualAttributesJson", source)
    def test_explicit_project_file_placement_is_additive_and_never_converts_legacy_ids(self):
        path=ROOT / "sql/mysql/20261001_dcc_b_project_file_placement.sql"
        sql=path.read_text(encoding="utf-8")
        self.assertEqual(_parse_metadata(path)["dependsOn"], ["20260930_dcc_b_project_attributes_folders"])
        for field in ["project_folder_id","storage_directory_id","controlled_file_id","tenant_id"]:
            self.assertIn(field,sql); self.assertIn(field,self.schema)
        self.assertIn("UNIQUE (tenant_id, controlled_file_id)",sql)
        self.assertIn("DELETE FROM dcc_project_file_placement;",self.clean)
        for statement in ["INSERT INTO","UPDATE DCC_CONTROLLED_FILE","DELETE FROM","TRUNCATE"]:
            self.assertNotIn(statement,sql.upper())

    def test_manual_project_folder_origin_is_nullable_without_faking_template_or_backfill(self):
        path=ROOT / "sql/mysql/20261001_dcc_b_manual_project_folder_origin.sql"
        self.assertTrue(path.is_file(), "人工项目目录不能伪造templateId或sourceNodeKey")
        sql=path.read_text(encoding="utf-8")
        self.assertEqual(_parse_metadata(path)["dependsOn"], ["20260930_dcc_b_project_attributes_folders"])
        self.assertIn("MODIFY COLUMN source_template_id BIGINT NULL",sql)
        self.assertIn("MODIFY COLUMN source_node_key VARCHAR(64) NULL",sql)
        for statement in ["INSERT INTO","UPDATE DCC_","DELETE FROM","TRUNCATE"]:
            self.assertNotIn(statement,sql.upper())

    def test_product_audit_intent_migration_never_fabricates_historical_reasons_or_attempts(self):
        path=ROOT / "sql/mysql/20261001_dcc_b_project_product_audit_intent.sql"
        sql=path.read_text(encoding="utf-8")
        self.assertEqual(_parse_metadata(path)["dependsOn"],["20260930_dcc_b_project_attributes_folders"])
        for field in ["creation_reason","write_attempt_no","write_reason","write_operator_user_id"]:
            self.assertIn("column_name = '"+field+"'",sql)
            self.assertIn(field,self.schema)
        for statement in ["INSERT INTO","UPDATE DCC_","DELETE FROM","ADD COLUMN IF NOT EXISTS"]:
            self.assertNotIn(statement,sql.upper())
    def test_reserved_draft_migration_keeps_real_bpm_unique_keys_and_history(self):
        path=ROOT / "sql/mysql/20261001_dcc_b_reserved_draft_round.sql"
        sql=path.read_text(encoding="utf-8")
        self.assertEqual(_parse_metadata(path)["dependsOn"],["20261001_dcc_application_round_link"])
        self.assertIn("bpm_round VARCHAR(64) COLLATE utf8mb4_bin NULL",sql)
        for field in ["source_application_id","source_application_round"]:
            self.assertIn("column_name = '"+field+"'",sql);self.assertIn(field,self.schema)
        for statement in ["INSERT INTO","UPDATE DCC_","DELETE FROM","DROP INDEX","DROP KEY"]:
            self.assertNotIn(statement,sql.upper())

if __name__ == "__main__":
    unittest.main(verbosity=2)
