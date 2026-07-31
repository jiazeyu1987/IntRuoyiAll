from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
SQL_PATH = ROOT / "sql" / "mysql" / "20260725_mes_edhr_recordbook_global_setting.sql"
CONFIG_KEY = "mes.edhr.recordbook.global.enabled"


def read_sql() -> str:
    assert SQL_PATH.exists(), "missing eDHR recordbook global setting migration"
    return SQL_PATH.read_text(encoding="utf-8")


def test_migration_metadata_and_fail_fast_guards() -> None:
    sql = read_sql()

    assert sql.splitlines()[0] == (
        "-- release-migration: allowedEnvironments=test,backup,prod; "
        "dependsOn=20260721_mes_edhr_golden_finger_admin_permission; "
        "type=config-seed; riskLevel=medium"
    )
    assert "SET NAMES utf8mb4;" in sql
    assert "START TRANSACTION;" in sql
    assert "ensure_mes_edhr_recordbook_global_setting_20260725" in sql
    assert "SIGNAL SQLSTATE '45000'" in sql

    for required_guard in [
        "Duplicate eDHR recordbook global setting config",
        "Missing infra_config table",
        "Invalid eDHR recordbook global setting config value",
    ]:
        assert required_guard in sql


def test_migration_seeds_enabled_config_as_visible_system_config() -> None:
    sql = read_sql()

    assert CONFIG_KEY in sql
    assert "INSERT INTO infra_config" in sql
    assert "'eDHR 记录本全局开关'" in sql
    assert "'mes'" in sql
    assert "'true'" in sql
    assert "`type`" in sql
    assert "`visible`" in sql
    assert "COMMIT;" in sql


def test_migration_is_idempotent_and_non_destructive() -> None:
    sql = read_sql()
    upper_sql = sql.upper()

    assert "ON DUPLICATE KEY UPDATE" in upper_sql or "WHERE NOT EXISTS" in upper_sql

    for forbidden in [
        "DELETE FROM INFRA_CONFIG",
        "DELETE FROM `INFRA_CONFIG`",
        "TRUNCATE TABLE INFRA_CONFIG",
        "DROP TABLE INFRA_CONFIG",
    ]:
        assert forbidden not in upper_sql


if __name__ == "__main__":
    test_migration_metadata_and_fail_fast_guards()
    test_migration_seeds_enabled_config_as_visible_system_config()
    test_migration_is_idempotent_and_non_destructive()
    print("PASS: eDHR recordbook global setting SQL contract")
