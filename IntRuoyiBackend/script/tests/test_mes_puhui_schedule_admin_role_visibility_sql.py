import re
from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]
SQL_PATH = REPO_ROOT / "sql" / "mysql" / "20260718_mes_puhui_schedule_admin_role_visibility.sql"


def _read_sql() -> str:
    assert SQL_PATH.exists(), "missing Puhui schedule admin role visibility migration"
    return SQL_PATH.read_text(encoding="utf-8")


def test_puhui_schedule_admin_sql_declares_role_menu_and_guards() -> None:
    text = _read_sql()

    required = [
        "SET NAMES utf8mb4;",
        "ensure_mes_puhui_schedule_admin_role_visibility",
        "'璞慧排产管理员'",
        "'mes_puhui_schedule_admin'",
        "`id` = 5100",
        "`id` = 900120",
        "`id` = 900104",
        "`path` = '/mes/pro/puhui-schedule'",
        "`component_name` = 'MesProPuhuiSchedule'",
        "SIGNAL SQLSTATE '45000'",
        "system_role",
        "system_role_menu",
        "system_user_role",
    ]

    for snippet in required:
        assert snippet in text


def test_puhui_schedule_admin_sql_grants_only_minimum_menu_tree() -> None:
    text = _read_sql()

    assert "tmp_mes_puhui_schedule_admin_menu_ids" in text
    assert "SELECT 5100 AS `menu_id`" in text
    assert "UNION ALL SELECT 900120" in text
    assert "UNION ALL SELECT 900104" in text
    assert "INSERT INTO `system_role_menu`" in text
    assert "JOIN `tmp_mes_puhui_schedule_admin_menu_ids` AS `puhui_menu`" in text
    assert "DELETE FROM `system_role_menu`" not in text
    assert "DELETE FROM `system_user_role`" not in text


def test_puhui_schedule_admin_sql_revokes_puhui_menu_from_non_target_roles() -> None:
    text = _read_sql()

    assert "UPDATE `system_role_menu` AS `role_menu`" in text
    assert "`role_menu`.`menu_id` = 900104" in text
    assert "`puhui_role`.`role_id` IS NULL" in text
    assert "`role_menu`.`deleted` = b'1'" in text
    assert "mes-puhui-schedule-admin-role" in text


def test_puhui_schedule_admin_sql_is_idempotent_and_resolves_role_dynamically() -> None:
    text = _read_sql()

    assert re.search(
        r"SELECT COALESCE\(MAX\(`existing_role`\.`id`\), 910299\)[\s\S]*?"
        r"\+ ROW_NUMBER\(\) OVER \(ORDER BY `target_tenant`\.`tenant_id`\)",
        text,
    )
    assert "WHERE NOT EXISTS (" in text
    assert "SELECT `role`.`id` AS `role_id`" in text
    assert "`role`.`code` = 'mes_puhui_schedule_admin'" in text
    assert "SET `role_menu`.`deleted` = b'0'" in text
    assert "FROM `system_role_menu` AS `existing`" in text
