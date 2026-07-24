from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]


def test_system_password_policy_migration_adds_and_backfills_password_update_time():
    migration = (ROOT / "sql/mysql/20260525_system_password_policy.sql").read_text(encoding="utf-8")

    assert "ADD COLUMN `password_update_time` datetime" in migration
    assert "AFTER `password`" in migration
    assert "SET `password_update_time` = COALESCE(`update_time`, `create_time`, NOW())" in migration


def test_system_test_schema_contains_password_update_time():
    schema = (ROOT / "yudao-module-system/src/test/resources/sql/create_tables.sql").read_text(encoding="utf-8")

    assert '"password_update_time" timestamp default null' in schema
