from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]
MIGRATION_PATH = REPO_ROOT / "sql" / "mysql" / "20260902_infra_config_value_expand_for_kingdee_connection.sql"
BASE_SCHEMA_PATH = REPO_ROOT / "sql" / "mysql" / "ruoyi-vue-pro.sql"


def read_migration() -> str:
    assert MIGRATION_PATH.exists(), "missing infra_config value expansion migration"
    return MIGRATION_PATH.read_text(encoding="utf-8")


def test_infra_config_value_expansion_migration_declares_safe_release_contract() -> None:
    text = read_migration()

    assert text.splitlines()[0] == (
        "-- release-migration: allowedEnvironments=test,backup,prod; "
        "dependsOn=; "
        "type=schema; riskLevel=low"
    )
    assert "SET NAMES utf8mb4;" in text
    assert "ALTER TABLE `infra_config`" in text
    assert "MODIFY COLUMN `value` varchar(2000)" in text
    assert "CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci" in text
    assert "NOT NULL DEFAULT '' COMMENT '参数键值'" in text


def test_infra_config_value_expansion_migration_has_fail_fast_schema_guards() -> None:
    text = read_migration()
    upper_text = text.upper()

    assert "ensure_infra_config_value_expand_20260902" in text
    assert "information_schema.COLUMNS" in text
    assert "TABLE_NAME = 'infra_config'" in text
    assert "COLUMN_NAME = 'value'" in text
    assert "Missing infra_config.value column for Kingdee connection config expansion" in text
    assert "Unexpected infra_config.value column type before Kingdee connection config expansion" in text
    assert "infra_config.value expansion to varchar(2000) failed" in text
    assert "SIGNAL SQLSTATE '45000'" in text

    for forbidden in [
        "DELETE FROM",
        "TRUNCATE TABLE",
        "DROP TABLE",
        "DROP TEMPORARY TABLE",
        "UPDATE `INFRA_CONFIG`",
        "INSERT INTO `INFRA_CONFIG`",
    ]:
        assert forbidden not in upper_text


def test_base_schema_uses_expanded_infra_config_value_column() -> None:
    schema = BASE_SCHEMA_PATH.read_text(encoding="utf-8")

    assert "`value` varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '参数键值'" in schema
    assert "`value` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '参数键值'" not in schema
