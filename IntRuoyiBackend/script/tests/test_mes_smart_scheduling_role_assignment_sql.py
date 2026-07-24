from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]
SQL_PATH = REPO_ROOT / "sql" / "mysql" / "20260629_mes_smart_scheduling_role_assignment.sql"


def _read_sql() -> str:
    assert SQL_PATH.exists(), "missing MES smart scheduling role assignment migration"
    return SQL_PATH.read_text(encoding="utf-8")


def test_role_assignment_sql_declares_expected_users_and_roles() -> None:
    text = _read_sql()

    required = [
        "SET NAMES utf8mb4;",
        "ensure_mes_smart_scheduling_role_assignment",
        "'zhaojie'",
        "'guliya'",
        "'wuxiaolei'",
        "'zhangjiayi'",
        "'mes_scheduler'",
        "'mes_workshop_director'",
        "system_user_role",
    ]

    for snippet in required:
        assert snippet in text


def test_role_assignment_sql_restores_or_inserts_target_bindings_idempotently() -> None:
    text = _read_sql()

    assert "UPDATE `system_user_role` AS `user_role`" in text
    assert "`user_role`.`deleted` = b'0'" in text
    assert "INSERT INTO `system_user_role`" in text
    assert "NOT EXISTS (" in text
    assert "FROM `system_user_role` AS `existing`" in text
    assert "DELETE FROM `system_user_role`" not in text


def test_role_assignment_sql_only_targets_requested_accounts() -> None:
    text = _read_sql()

    assert "`user`.`username` = 'zhaojie'" in text
    assert "`user`.`username` IN ('guliya', 'wuxiaolei', 'zhangjiayi')" in text
    assert "admin" not in text
    assert "'mes_team_leader'" not in text
