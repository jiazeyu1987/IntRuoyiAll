import re
from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]
SQL_PATH = REPO_ROOT / "sql" / "mysql" / "20260617_mes_scheduler_role_smart_scheduling_tab.sql"


def _read_sql() -> str:
    assert SQL_PATH.exists(), "missing scheduler role smart scheduling tab migration"
    return SQL_PATH.read_text(encoding="utf-8")


def test_scheduler_role_smart_scheduling_sql_targets_existing_scheduler_roles() -> None:
    text = _read_sql()

    required_snippets = [
        "SET NAMES utf8mb4;",
        "ensure_mes_scheduler_role_smart_scheduling_tab",
        "SIGNAL SQLSTATE '45000'",
        "`role`.`name` IN ('排产员', '计划员', '生产计划员', '排产员/计划员')",
        "`role`.`code` IN ('planner', 'scheduler', 'mes_planner', 'mes_scheduler', 'production_planner', 'production_scheduler')",
        "tmp_mes_scheduler_role_allowed_menu_ids",
        "WITH RECURSIVE `smart_menu_tree` AS",
        "`menu`.`id` = 900120",
    ]

    for snippet in required_snippets:
        assert snippet in text


def test_scheduler_role_smart_scheduling_sql_grants_smart_scheduling_tree_idempotently() -> None:
    text = _read_sql()

    assert re.search(
        r"INSERT INTO `system_role_menu`[\s\S]*?FROM `tmp_mes_scheduler_role_targets` AS `target_role`[\s\S]*?"
        r"JOIN `tmp_mes_scheduler_role_allowed_menu_ids` AS `allowed_menu`[\s\S]*?"
        r"JOIN `system_menu` AS `smart_menu`[\s\S]*?"
        r"`smart_menu`\.`id` = `allowed_menu`\.`menu_id`",
        text,
    )
    assert re.search(
        r"NOT EXISTS \([\s\S]*?FROM `system_role_menu` AS `existing`[\s\S]*?"
        r"`existing`\.`role_id` = `target_role`\.`role_id`[\s\S]*?"
        r"`existing`\.`menu_id` = `smart_menu`\.`id`[\s\S]*?"
        r"`existing`\.`tenant_id` = `target_role`\.`tenant_id`[\s\S]*?"
        r"`existing`\.`deleted` = b'0'",
        text,
    )
    assert "DELETE FROM `system_role_menu`" not in text


def test_scheduler_role_smart_scheduling_sql_removes_non_smart_menus_from_scheduler_role_only() -> None:
    text = _read_sql()

    assert re.search(
        r"UPDATE `system_role_menu` AS `role_menu`[\s\S]*?"
        r"JOIN `tmp_mes_scheduler_role_targets` AS `target_role`[\s\S]*?"
        r"`target_role`\.`role_id` = `role_menu`\.`role_id`[\s\S]*?"
        r"`target_role`\.`tenant_id` = `role_menu`\.`tenant_id`[\s\S]*?"
        r"LEFT JOIN `tmp_mes_scheduler_role_allowed_menu_ids` AS `allowed_menu`[\s\S]*?"
        r"`allowed_menu`\.`menu_id` = `role_menu`\.`menu_id`[\s\S]*?"
        r"SET `role_menu`\.`deleted` = b'1'[\s\S]*?"
        r"`role_menu`\.`deleted` = b'0'[\s\S]*?"
        r"`allowed_menu`\.`menu_id` IS NULL",
        text,
    )
    assert "DELETE FROM `system_role_menu`" not in text


def test_scheduler_role_smart_scheduling_sql_keeps_tenant_package_gate() -> None:
    text = _read_sql()

    assert "JSON_VALID(`tenant_package`.`menu_ids`)" in text
    assert "JSON_CONTAINS(CAST(`tenant_package`.`menu_ids` AS JSON), CAST('900120' AS JSON), '$')" in text
    assert "JSON_CONTAINS(CAST(`tenant_package`.`menu_ids` AS JSON), CAST(CONCAT('', `allowed_menu`.`menu_id`) AS JSON), '$')" in text
    assert "Missing MES smart scheduling core menu ids 900120/5590/5580/5262/5540" in text


def test_scheduler_role_smart_scheduling_sql_noops_when_no_target_roles_exist() -> None:
    text = _read_sql()

    assert "Missing enabled MES scheduler/planner role with smart scheduling tenant package" not in text
    assert re.search(
        r"IF NOT EXISTS \([\s\S]*?FROM `tmp_mes_scheduler_role_targets`[\s\S]*?\) THEN[\s\S]*?"
        r"DROP TEMPORARY TABLE IF EXISTS `tmp_mes_scheduler_role_targets`[\s\S]*?"
        r"DROP TEMPORARY TABLE IF EXISTS `tmp_mes_scheduler_role_allowed_menu_ids`[\s\S]*?"
        r"LEAVE `?ensure_mes_scheduler_role_smart_scheduling_tab`?",
        text,
    ), "migration must no-op and clean temporary tables when no scheduler/planner role is eligible"
