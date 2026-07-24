from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
MIGRATION_SQL = ROOT / "sql/mysql/20260707_mes_feedback_import_record_allow_repeat_source_row.sql"
MES_SCHEMA_SQL = ROOT / "sql/mysql/20260512_mes_schema.sql"
MES_BASE_SCHEMA_SQL = ROOT / "sql/mysql/20260512_mes_base_schema.sql"


def read(path: Path) -> str:
    return path.read_text(encoding="utf-8")




def test_feedback_import_record_repeat_source_row_migration_has_release_metadata() -> None:
    sql = read(MIGRATION_SQL)

    assert sql.startswith(
        "-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=; type=schema; riskLevel=medium"
    )
def test_feedback_import_record_repeat_source_row_migration_is_idempotent() -> None:
    sql = read(MIGRATION_SQL)

    assert "TABLE_NAME = 'mes_pro_feedback_import_record'" in sql
    assert "INDEX_NAME = 'uk_mes_pro_feedback_import_record_source_row'" in sql
    assert "DROP INDEX `uk_mes_pro_feedback_import_record_source_row`" in sql
    assert "INDEX_NAME = 'idx_mes_pro_feedback_import_record_source_row'" in sql
    assert "CREATE INDEX `idx_mes_pro_feedback_import_record_source_row`" in sql
    assert "`source_file_sha256`, `sheet_name`, `row_no`" in sql
    assert "PREPARE drop_source_row_unique_stmt" in sql
    assert "PREPARE create_source_row_index_stmt" in sql


def test_feedback_import_record_source_row_is_not_unique_in_base_schemas() -> None:
    for path in (MES_SCHEMA_SQL, MES_BASE_SCHEMA_SQL):
        sql = read(path)
        table_start = sql.index("CREATE TABLE IF NOT EXISTS `mes_pro_feedback_import_record`")
        table_end = sql.index(") ENGINE=InnoDB", table_start)
        table_sql = sql[table_start:table_end]

        assert "UNIQUE KEY `uk_mes_pro_feedback_import_record_source_row`" not in table_sql
        assert "KEY `idx_mes_pro_feedback_import_record_source_row` (`source_file_sha256`, `sheet_name`, `row_no`)" in table_sql
