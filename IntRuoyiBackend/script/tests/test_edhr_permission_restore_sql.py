from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]
SQL_PATH = REPO_ROOT / "sql" / "mysql" / "20260524_edhr_permission_restore.sql"


def test_edhr_permission_restore_sql_is_present() -> None:
    text = SQL_PATH.read_text(encoding="utf-8")

    for permission in [
        "mes:pro-batch-record-template:query",
        "mes:pro-batch-record-template:import",
        "mes:pro-batch-record-template:update",
        "mes:pro-batch-record-template:delete",
        "mes:pro-batch-record-execution:query",
        "mes:pro-batch-record-execution:create",
        "mes:pro-batch-record-execution:update",
    ]:
        assert permission in text


def test_edhr_permission_restore_sql_repairs_package_and_role_bindings() -> None:
    text = SQL_PATH.read_text(encoding="utf-8")

    assert "WITH RECURSIVE `edhr_menu_tree`" in text
    assert "`child`.`parent_id` = `parent`.`id`" in text
    assert "JSON_TABLE(" in text
    assert "UPDATE `system_tenant_package`" in text
    assert "UPDATE `system_role_menu`" in text
    assert "`role_menu`.`deleted` = b'0'" in text
    assert "INSERT INTO `system_role_menu`" in text
    assert "WHERE NOT EXISTS (" in text


def test_edhr_permission_restore_sql_is_scoped_to_existing_soft_deleted_edhr_rows() -> None:
    text = SQL_PATH.read_text(encoding="utf-8")

    assert "`role_menu`.`deleted` = b'1'" in text
    assert "`role`.`code` = 'tenant_admin'" in text
    assert "`tenant`.`package_id` <> 0" in text
    assert "tenant`.`id` = 122" not in text
    assert "DELETE FROM `system_role_menu`" not in text
