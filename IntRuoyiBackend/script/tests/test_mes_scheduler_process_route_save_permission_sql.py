from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]
SQL_PATH = REPO_ROOT / "sql" / "mysql" / "20260708_mes_scheduler_process_route_save_permission.sql"


def _read_sql() -> str:
    assert SQL_PATH.exists(), "missing scheduler process route save permission migration"
    return SQL_PATH.read_text(encoding="utf-8")


def test_scheduler_process_route_save_permission_sql_grants_only_button_permission() -> None:
    text = _read_sql()

    assert "ensure_mes_scheduler_process_route_save_permission" in text
    assert "900122" in text
    assert "mes:pro-schedule-route:update" in text
    assert "(`name` = '排产员' OR `code` = 'mes_scheduler')" in text
    assert "`role_menu`.`menu_id` = 900122" in text
    assert "INSERT INTO `system_role_menu`" in text
    assert "NOT EXISTS (" in text
    assert "DELETE FROM `system_role_menu`" not in text
    assert "DROP DATABASE" not in text.upper()


def test_scheduler_process_route_save_permission_sql_fails_fast_on_missing_prerequisites() -> None:
    text = _read_sql()

    assert "SIGNAL SQLSTATE '45000'" in text
    assert "Missing enabled process schedule route save permission menu 900122" in text
    assert "Missing enabled scheduler role; cannot grant process schedule route save permission" in text
