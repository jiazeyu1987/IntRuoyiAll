from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
SQL_PATH = ROOT / "sql" / "mysql" / "20260921_infra_idle_logout_config.sql"
CONFIG_KEY = "system.login.idle-timeout-minutes"


def read_sql() -> str:
    assert SQL_PATH.exists(), "missing idle logout config SQL migration"
    return SQL_PATH.read_text(encoding="utf-8")


def test_migration_metadata_and_fail_fast_guards() -> None:
    sql = read_sql()

    assert sql.splitlines()[0] == (
        "-- release-migration: allowedEnvironments=test,backup,prod; "
        "dependsOn=; type=config; riskLevel=low"
    )
    assert "SET NAMES utf8mb4;" in sql
    assert "START TRANSACTION;" in sql
    assert "SIGNAL SQLSTATE '45000'" in sql

    for required_guard in [
        "Missing infra_config table",
        "Duplicate idle logout config",
        "Invalid idle logout config value",
    ]:
        assert required_guard in sql
    assert "value IS NULL" in sql


def test_migration_seeds_visible_system_config() -> None:
    sql = read_sql()

    assert CONFIG_KEY in sql
    assert "INSERT INTO infra_config" in sql
    assert "'后台登录-空闲自动退出分钟数'" in sql
    assert "'login-security'" in sql
    assert "'15'" in sql
    assert "`type`" in sql
    assert "`visible`" in sql
    assert "b'1'" in sql
    assert "COMMIT;" in sql


def test_migration_is_idempotent_and_non_destructive() -> None:
    sql = read_sql()
    upper_sql = sql.upper()

    assert "WHERE NOT EXISTS" in upper_sql
    for forbidden in [
        "DELETE FROM INFRA_CONFIG",
        "DELETE FROM `INFRA_CONFIG`",
        "TRUNCATE TABLE INFRA_CONFIG",
        "DROP TABLE INFRA_CONFIG",
        "ON DUPLICATE KEY UPDATE",
    ]:
        assert forbidden not in upper_sql


if __name__ == "__main__":
    test_migration_metadata_and_fail_fast_guards()
    test_migration_seeds_visible_system_config()
    test_migration_is_idempotent_and_non_destructive()
    print("PASS: idle logout config SQL contract")
