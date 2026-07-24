from pathlib import Path


SQL_PATH = (
    Path(__file__).resolve().parents[2]
    / "sql"
    / "mysql"
    / "20260629_mes_schedule_order_manual_finish.sql"
)


def _read_sql() -> str:
    assert SQL_PATH.exists(), "missing schedule order manual finish migration"
    return SQL_PATH.read_text(encoding="utf-8")


def test_manual_finish_sql_adds_schedule_order_columns_and_permissions() -> None:
    sql = _read_sql()

    required_tokens = [
        "release-migration: allowedEnvironments=test,backup,prod",
        "ALTER TABLE `mes_pro_schedule_order` ADD COLUMN `manual_finished`",
        "ALTER TABLE `mes_pro_schedule_order` ADD COLUMN `manual_finished_time`",
        "ALTER TABLE `mes_pro_schedule_order` ADD COLUMN `manual_finished_by`",
        "ALTER TABLE `mes_pro_schedule_order` ADD COLUMN `manual_finished_reason`",
        "mes:pro-schedule-order:manual-finish",
        "mes:pro-schedule-order:revoke-complete",
        "排产工单人工完成",
        "排产工单撤销人工完成",
        "system_menu",
        "system_role_menu",
        "system_tenant_package",
    ]
    for token in required_tokens:
        assert token in sql


def test_manual_finish_sql_grants_scheduler_manual_finish_and_admin_revoke_only() -> None:
    sql = _read_sql()

    assert "排产员" in sql
    assert "tenant_admin" in sql
    assert "super_admin" in sql
    assert "mes:pro-schedule-order:manual-finish" in sql
    assert "mes:pro-schedule-order:revoke-complete" in sql
    assert "revoke-complete" in sql
