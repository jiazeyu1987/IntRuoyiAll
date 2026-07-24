from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]
MIGRATION_SQL = REPO_ROOT / "sql/mysql/20260614_dcc_master_directory_identity.sql"
BASE_SCHEMA_SQL = REPO_ROOT / "sql/mysql/20260513_dcc_base_schema.sql"
RUNTIME_REPAIR_SQL = REPO_ROOT / "sql/mysql/20260515_dcc_runtime_schema_repair.sql"
TEST_SCHEMA_SQL = REPO_ROOT / "yudao-module-dcc/src/test/resources/sql/create_tables.sql"


def _read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def _compact_sql(text: str) -> str:
    return " ".join(text.split())


def test_migration_backfills_master_directory_id_before_reindexing():
    migration = _read(MIGRATION_SQL)

    assert "ADD COLUMN `directory_id` bigint DEFAULT NULL" in migration
    assert "current_active_controlled_file_id" in migration
    assert "active_directory_id" in migration
    assert "any_directory_id" in migration
    assert "DROP INDEX `uk_dcc_controlled_file_master_chain`" in migration
    assert (
        "ADD UNIQUE KEY `uk_dcc_controlled_file_master_chain` "
        "(`category_id`, `directory_id`, `file_name`)"
    ) in migration


def test_schema_files_keep_dcc_master_unique_per_directory():
    expected_columns = "(`category_id`, `directory_id`, `file_name`)"
    for sql_path in (BASE_SCHEMA_SQL, RUNTIME_REPAIR_SQL, TEST_SCHEMA_SQL):
        sql = _compact_sql(_read(sql_path))
        lower_sql = sql.lower()
        assert "`directory_id` bigint" in lower_sql
        assert "`uk_dcc_controlled_file_master_chain`" in lower_sql
        assert expected_columns in lower_sql
