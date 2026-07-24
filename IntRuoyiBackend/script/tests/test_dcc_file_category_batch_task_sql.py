from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]
MIGRATION_SQL = REPO_ROOT / "sql/mysql/20260710_dcc_file_category_batch_task.sql"
TEST_SCHEMA_SQL = REPO_ROOT / "yudao-module-dcc/src/test/resources/sql/create_tables.sql"


def test_file_category_batch_task_migration_is_additive_and_idempotent():
    sql = MIGRATION_SQL.read_text(encoding="utf-8")

    assert sql.startswith(
        "-- release-migration: allowedEnvironments=test,backup,prod; "
        "dependsOn=20260706_dcc_batch_recognition_existing_record_policy; "
        "type=schema; riskLevel=medium"
    )
    assert "information_schema.COLUMNS" in sql
    assert "information_schema.STATISTICS" in sql
    for column in (
        "recognition_type",
        "unclassified_count",
        "ambiguous_count",
        "conflict_count",
    ):
        assert f"COLUMN_NAME = '{column}'" in sql
    assert "idx_dcc_batch_recognition_task_type_status" in sql
    assert "DROP TABLE" not in sql.upper()
    assert "DROP COLUMN" not in sql.upper()


def test_file_category_batch_task_columns_exist_in_test_schema():
    sql = TEST_SCHEMA_SQL.read_text(encoding="utf-8")

    assert "`recognition_type` varchar(32) NOT NULL DEFAULT 'BASIC_INFO'" in sql
    assert "`unclassified_count` bigint NOT NULL DEFAULT 0" in sql
    assert "`ambiguous_count` bigint NOT NULL DEFAULT 0" in sql
    assert "`conflict_count` bigint NOT NULL DEFAULT 0" in sql
    assert "idx_dcc_batch_recognition_task_type_status" in sql
