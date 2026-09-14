from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
MIGRATION = ROOT / "sql" / "mysql" / "20260910_dcc_access_log_reason_capacity.sql"
BASE_SCHEMA = ROOT / "sql" / "mysql" / "20260513_dcc_base_schema.sql"
TEST_SCHEMA = ROOT / "yudao-module-dcc" / "src" / "test" / "resources" / "sql" / "create_tables.sql"


def test_access_log_reason_capacity_is_consistent() -> None:
    migration = MIGRATION.read_text(encoding="utf-8")
    base_schema = BASE_SCHEMA.read_text(encoding="utf-8")
    test_schema = TEST_SCHEMA.read_text(encoding="utf-8")

    assert migration.startswith("-- release-migration:")
    assert "MODIFY COLUMN `reason` varchar(2000)" in migration
    assert "UPDATE `dcc_controlled_file_access_log`" not in migration.upper()
    assert "`reason` varchar(2000)" in base_schema.lower()
    assert "`reason` varchar(2000)" in test_schema.lower()
