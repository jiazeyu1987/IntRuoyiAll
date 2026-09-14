import pathlib
import re
import unittest


ROOT = pathlib.Path(__file__).resolve().parents[2]
MIGRATION = ROOT / "sql" / "mysql" / "20260817_mes_pqc_item_level_task_identity.sql"
ROLLBACK = ROOT / "sql" / "mysql" / "20260817_mes_pqc_item_level_task_identity_rollback.sql"


class MesPqcItemLevelTaskIdentitySqlTest(unittest.TestCase):

    def test_migration_adds_item_identity_and_unique_key(self):
        source = MIGRATION.read_text(encoding="utf-8")
        self.assertRegex(source, r"ADD COLUMN `qa_item_code` varchar\(64\) NOT NULL DEFAULT ''")
        self.assertRegex(
            source,
            re.compile(
                r"ADD UNIQUE KEY `uk_mes_pqc_task_item_rule_identity`[\s\S]*"
                r"`qa_process_id`,[\s\S]*`qa_item_code`,[\s\S]*`inspection_rule_key`"
            ),
        )
        self.assertIn("duplicate PQC item-level task identities exist", source)

    def test_rollback_blocks_when_item_scoped_tasks_exist(self):
        source = ROLLBACK.read_text(encoding="utf-8")
        self.assertRegex(source, r"WHERE `qa_item_code` <> ''")
        self.assertIn("run an approved data rollback before schema rollback", source)
        self.assertIn("DROP COLUMN `qa_item_code`", source)


if __name__ == "__main__":
    unittest.main()
