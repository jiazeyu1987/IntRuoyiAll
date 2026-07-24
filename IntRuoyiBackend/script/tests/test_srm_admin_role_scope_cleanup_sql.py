from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]
SQL_PATH = REPO_ROOT / "sql" / "mysql" / "20260701_srm_admin_role_menu_scope_cleanup.sql"


def _read_sql() -> str:
    assert SQL_PATH.exists(), "missing SRM admin role menu scope cleanup migration"
    return SQL_PATH.read_text(encoding="utf-8")


def test_cleanup_sql_declares_expected_scope_and_dependency() -> None:
    text = _read_sql()

    required = [
        "allowedEnvironments=test,backup,prod",
        "dependsOn=20260629_srm_admin_role_visibility",
        "ensure_srm_admin_role_menu_scope_cleanup",
        "'srm_admin'",
        "991000",
        "tmp_srm_admin_menu_ids",
        "system_role_menu",
    ]

    for snippet in required:
        assert snippet in text


def test_cleanup_sql_soft_deletes_non_srm_menu_bindings_only() -> None:
    text = _read_sql()

    assert "LEFT JOIN `tmp_srm_admin_menu_ids` AS `srm_menu`" in text
    assert "`role_menu`.`deleted` = b'1'" in text
    assert "`srm_menu`.`id` IS NULL" in text
    assert "DELETE FROM `system_role_menu`" not in text


def test_cleanup_sql_keeps_srm_bindings_active_and_idempotent() -> None:
    text = _read_sql()

    assert "JOIN `tmp_srm_admin_menu_ids` AS `srm_menu`" in text
    assert "SET `role_menu`.`deleted` = b'0'" in text
    assert "INSERT INTO `system_role_menu`" in text
    assert "NOT EXISTS (" in text


def test_cleanup_sql_targets_resolved_srm_admin_role_id_instead_of_fixed_id() -> None:
    text = _read_sql()

    assert "DECLARE v_srm_admin_role_id BIGINT DEFAULT NULL;" in text
    assert "SELECT `id`\n  INTO v_srm_admin_role_id" in text
    assert "910240" not in text
