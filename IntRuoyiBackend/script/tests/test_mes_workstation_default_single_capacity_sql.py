from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]
SQL_PATH = REPO_ROOT / "sql" / "mysql" / "20260609_mes_workstation_default_single_capacity_30.sql"


def read_sql() -> str:
    return SQL_PATH.read_text(encoding="utf-8")


def test_default_single_capacity_sql_is_admin_tenant_guarded() -> None:
    text = read_sql()

    assert "UPDATE `mes_md_workstation`" in text
    assert "`tenant_id` = 1" in text
    assert "`deleted` = b'0'" in text
    assert "`single_standard_hourly_capacity` IS NULL" in text
    assert "`single_standard_hourly_capacity` = 30.00" in text


def test_default_single_capacity_sql_is_transactional_and_auditable() -> None:
    text = read_sql()

    assert "START TRANSACTION;" in text
    assert "CREATE TEMPORARY TABLE `tmp_mes_workstation_default_single_capacity_30`" in text
    assert "SELECT COUNT(*) AS pending_update_count" in text
    assert "SELECT COUNT(*) AS remaining_missing_count" in text
    assert "COMMIT;" in text
