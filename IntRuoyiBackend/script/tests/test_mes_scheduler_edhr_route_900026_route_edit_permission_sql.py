from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]
SQL_PATH = REPO_ROOT / "sql" / "mysql" / "20260708_mes_scheduler_edhr_route_900026_route_edit_permission.sql"


def _read_sql() -> str:
    assert SQL_PATH.exists(), "missing scheduler eDHR route edit permission migration"
    return SQL_PATH.read_text(encoding="utf-8")


def test_scheduler_edhr_route_edit_permission_sql_grants_role_rule_to_route_scope() -> None:
    text = _read_sql()

    assert "ensure_mes_scheduler_edhr_route_900026_route_edit_permission" in text
    assert "FROM `mes_pro_route`" in text
    assert "`route`.`id` = 900026" in text
    assert "INSERT INTO `mes_pro_edhr_permission_scope`" in text
    assert "CONCAT('工艺路线:', `route`.`code`)" in text
    assert "`object_type` = 'ROUTE'" in text
    assert "`object_id` = '900026'" in text
    assert "'ROUTE_EDIT'" in text
    assert "'ROLE'" in text
    assert "_utf8mb4'排产员' COLLATE utf8mb4_unicode_ci" in text
    assert "`role`.`code` = 'mes_scheduler'" in text
    assert "`rule`.`decision` = 'ALLOW'" in text
    assert "INSERT INTO `mes_pro_edhr_permission_rule`" in text
    assert "NOT EXISTS (" in text
    assert "DELETE FROM `mes_pro_edhr_permission_rule`" not in text
    assert "DROP DATABASE" not in text.upper()


def test_scheduler_edhr_route_edit_permission_sql_fails_fast_on_missing_prerequisites() -> None:
    text = _read_sql()

    assert "SIGNAL SQLSTATE '45000'" in text
    assert "Missing route 900026 for scheduler tenant" in text
    assert "Missing enabled eDHR permission scope ROUTE:900026" in text
    assert "Missing enabled scheduler role for eDHR scope ROUTE:900026" in text
