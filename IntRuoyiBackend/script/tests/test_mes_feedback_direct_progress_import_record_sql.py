from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
MIGRATION_SQL = ROOT / "sql/mysql/20260718_mes_feedback_import_record_direct_progress.sql"
MES_SCHEMA_SQL = ROOT / "sql/mysql/20260512_mes_schema.sql"
MES_BASE_SCHEMA_SQL = ROOT / "sql/mysql/20260512_mes_base_schema.sql"
TEST_SCHEMA_SQL = ROOT / "yudao-module-mes/src/test/resources/sql/create_tables.sql"


def read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def test_direct_progress_import_record_migration_is_idempotent() -> None:
    sql = read(MIGRATION_SQL)

    assert sql.startswith(
        "-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260707_mes_feedback_import_record_allow_repeat_source_row; type=schema; riskLevel=medium"
    )
    for column in (
        "progress_source_type",
        "progress_quantity",
        "progress_applied_time",
        "progress_warning_code",
        "progress_warning_message",
    ):
        assert f"`{column}`" in sql
        assert "INFORMATION_SCHEMA.COLUMNS" in sql
        assert f"COLUMN_NAME = '{column}'" in sql
        assert f"ADD COLUMN `{column}`" in sql
    assert "idx_mes_feedback_import_record_direct_progress" in sql
    assert "INFORMATION_SCHEMA.STATISTICS" in sql
    assert "ADD COLUMN IF NOT EXISTS" not in sql
    assert "PREPARE create_direct_progress_index_stmt" in sql


def test_direct_progress_import_record_fields_exist_in_base_schemas() -> None:
    for path in (MES_SCHEMA_SQL, MES_BASE_SCHEMA_SQL):
        sql = read(path)
        table_start = sql.index("CREATE TABLE IF NOT EXISTS `mes_pro_feedback_import_record`")
        table_end = sql.index(") ENGINE=InnoDB", table_start)
        table_sql = sql[table_start:table_end]

        assert "`progress_source_type` varchar(64) DEFAULT NULL" in table_sql
        assert "`progress_quantity` decimal(18,6) DEFAULT NULL" in table_sql
        assert "`progress_applied_time` datetime DEFAULT NULL" in table_sql
        assert "`progress_warning_code` varchar(64) DEFAULT NULL" in table_sql
        assert "`progress_warning_message` varchar(500) DEFAULT NULL" in table_sql
        assert (
            "KEY `idx_mes_feedback_import_record_direct_progress` (`tenant_id`, `schedule_order_id`, "
            "`progress_source_type`, `attribution_status`, `schedule_order_process_id`)"
        ) in table_sql
        assert "UNIQUE KEY `uk_mes_pro_feedback_import_record_source_row`" not in table_sql


def test_direct_progress_import_record_fields_exist_in_h2_test_schema() -> None:
    sql = read(TEST_SCHEMA_SQL)
    table_start = sql.index('CREATE TABLE IF NOT EXISTS "mes_pro_feedback_import_record"')
    table_end = sql.index(");", table_start)
    table_sql = sql[table_start:table_end]

    assert '"progress_source_type" varchar(64) DEFAULT NULL' in table_sql
    assert '"progress_quantity" decimal(18,6) DEFAULT NULL' in table_sql
    assert '"progress_applied_time" timestamp DEFAULT NULL' in table_sql
    assert '"progress_warning_code" varchar(64) DEFAULT NULL' in table_sql
    assert '"progress_warning_message" varchar(500) DEFAULT NULL' in table_sql
