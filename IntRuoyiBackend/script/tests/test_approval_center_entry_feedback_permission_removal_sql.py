from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
SQL_PATH = ROOT / "sql" / "mysql" / "20260903_approval_center_entry_feedback_permission_removal.sql"


def read_sql() -> str:
    assert SQL_PATH.exists(), f"Missing SQL script: {SQL_PATH}"
    return SQL_PATH.read_text(encoding="utf-8")


def test_feedback_permission_removal_is_test_scoped_and_fails_fast() -> None:
    text = read_sql()

    assert text.splitlines()[0] == (
        "-- release-migration: allowedEnvironments=test; "
        "dependsOn=20260803_mes_frontline_pressure_pump_all_process_permission,20260807_test_tenant1_all_role_permission_sync; "
        "type=permission; riskLevel=low"
    )
    assert "'approval_center_entry'" in text
    assert "'mes:pro-feedback:frontline-pressure-pump:all-processes'" in text
    assert "`target_role`.`tenant_id` = 1" in text
    assert "SIGNAL SQLSTATE '45000'" in text
    assert "Missing or ambiguous approval center entry role for feedback permission removal" in text
    assert "Missing or ambiguous feedback button menu for approval center entry role" in text


def test_feedback_permission_removal_only_soft_deletes_the_exact_binding() -> None:
    text = read_sql()
    upper = text.upper()

    assert "UPDATE `system_role_menu` AS `role_menu`" in text
    assert "SET `role_menu`.`deleted` = b'1'" in text
    assert "`target_role`.`tenant_id` = `role_menu`.`tenant_id`" in text
    assert "Approval center entry feedback button permission removal incomplete" in text

    for forbidden in [
        "DELETE FROM `SYSTEM_ROLE_MENU`",
        "TRUNCATE TABLE",
        "DROP TABLE",
    ]:
        assert forbidden not in upper
