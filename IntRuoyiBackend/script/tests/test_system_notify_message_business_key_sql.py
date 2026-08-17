from pathlib import Path
import re

from script.release.release_preflight_plan import build_preflight_plan


REPO_ROOT = Path(__file__).resolve().parents[2]
MIGRATION_SQL = REPO_ROOT / "sql/mysql/20260815_system_notify_message_business_key.sql"
FULL_SCHEMA_SQL = REPO_ROOT / "sql/mysql/ruoyi-vue-pro.sql"
H2_SCHEMA_SQL = (
    REPO_ROOT
    / "yudao-module-system/src/test/resources/sql/create_tables.sql"
)


def _read(path: Path) -> str:
    assert path.exists(), f"missing required SQL contract: {path.relative_to(REPO_ROOT)}"
    return path.read_text(encoding="utf-8")


def _notify_table_block(sql: str, create_marker: str, end_marker: str) -> str:
    start = sql.index(create_marker)
    end = sql.index(end_marker, start)
    return sql[start:end]


def _release_dependencies(sql: str) -> set[str]:
    match = re.search(r"^-- release-migration: .*?dependsOn=([^;]*);", sql, re.MULTILINE)
    assert match, "release migration metadata with dependsOn is required"
    return {value.strip() for value in match.group(1).split(",") if value.strip()}


def test_migration_declares_release_metadata_and_non_destructive_scope() -> None:
    sql = _read(MIGRATION_SQL)

    assert sql.startswith(
        "-- release-migration: allowedEnvironments=test,backup,prod; "
        "dependsOn=; "
        "type=schema; riskLevel=medium"
    )
    upper_sql = sql.upper()
    assert "DROP TABLE" not in upper_sql
    assert "TRUNCATE TABLE" not in upper_sql
    assert "DELETE FROM" not in upper_sql
    assert "UPDATE `SYSTEM_NOTIFY_MESSAGE`" not in upper_sql


def test_code_only_plan_does_not_require_prior_notify_data_repair() -> None:
    schema_migration = {
        "migrationId": MIGRATION_SQL.stem,
        "file": f"sql/mysql/{MIGRATION_SQL.name}",
        "sha256": "b" * 64,
        "type": "schema",
        "allowedEnvironments": ["test", "backup", "prod"],
        "dependsOn": sorted(_release_dependencies(_read(MIGRATION_SQL))),
    }
    prior_data_repair = {
        "migrationId": "20260715_showroom_notify_template_garbled_repair",
        "file": "sql/mysql/20260715_showroom_notify_template_garbled_repair.sql",
        "sha256": "a" * 64,
        "type": "data",
        "allowedEnvironments": ["test", "backup", "prod"],
        "dependsOn": [],
    }

    plan = build_preflight_plan(
        [prior_data_repair, schema_migration],
        {},
        target_environment="test",
        publish_scope="code-only",
    )

    assert plan["status"] == "passed"
    assert [item["action"] for item in plan["items"]] == [
        "SKIP_SCOPE_EXCLUDED",
        "APPLY",
    ]


def test_migration_fails_fast_on_missing_or_conflicting_schema() -> None:
    sql = _read(MIGRATION_SQL)

    required = [
        "information_schema.TABLES",
        "information_schema.COLUMNS",
        "information_schema.STATISTICS",
        "SIGNAL SQLSTATE '45000'",
        "SYSTEM_NOTIFY_MESSAGE_TABLE_MISSING",
        "SYSTEM_NOTIFY_MESSAGE_BUSINESS_KEY_COLUMN_CONFLICT",
        "SYSTEM_NOTIFY_MESSAGE_BUSINESS_KEY_INDEX_CONFLICT",
        "SYSTEM_NOTIFY_MESSAGE_BUSINESS_KEY_DUPLICATES",
        "CHARACTER_MAXIMUM_LENGTH = 255",
        "IS_NULLABLE = 'YES'",
        "ADD COLUMN `business_key` varchar(255) NULL",
        "ADD UNIQUE KEY `uk_system_notify_message_tenant_business_key` "
        "(`tenant_id`, `business_key`)",
    ]
    for snippet in required:
        assert snippet in sql


def test_h2_fixture_has_nullable_business_key_and_tenant_unique_constraint() -> None:
    sql = _read(H2_SCHEMA_SQL)
    table = _notify_table_block(
        sql,
        'CREATE TABLE IF NOT EXISTS "system_notify_message"',
        "COMMENT '站内信消息表';",
    )

    assert re.search(r'"business_key"\s+varchar\(255\)\s+DEFAULT NULL', table)
    assert (
        'CONSTRAINT "uk_system_notify_message_tenant_business_key" '
        'UNIQUE ("tenant_id", "business_key")'
    ) in table


def test_full_schema_matches_nullable_column_and_tenant_unique_key() -> None:
    sql = _read(FULL_SCHEMA_SQL)
    table = _notify_table_block(
        sql,
        "CREATE TABLE `system_notify_message`",
        "COMMENT = '站内信消息表';",
    )

    assert "`business_key` varchar(255)" in table
    assert "NULL DEFAULT NULL" in table
    assert (
        "UNIQUE KEY `uk_system_notify_message_tenant_business_key` "
        "(`tenant_id`, `business_key`)"
    ) in table


def test_existing_full_schema_rows_keep_null_business_key() -> None:
    sql = _read(FULL_SCHEMA_SQL)
    records_start = sql.index("-- Records of system_notify_message")
    records_end = sql.index("-- Table structure for system_notify_template", records_start)
    records = sql[records_start:records_end]

    insert_lines = [line for line in records.splitlines() if line.startswith("INSERT INTO")]
    assert insert_lines
    assert all("`business_key`" not in line for line in insert_lines)
