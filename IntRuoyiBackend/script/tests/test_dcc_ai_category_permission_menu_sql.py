from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]
SQL_PATH = REPO_ROOT / "sql" / "mysql" / "20260707_dcc_ai_category_permission_menu.sql"


def read_sql() -> str:
    assert SQL_PATH.exists(), "DCC AI category permission migration must exist"
    return SQL_PATH.read_text(encoding="utf-8")


def test_dcc_ai_category_permission_migration_has_release_metadata() -> None:
    sql = read_sql()

    assert sql.splitlines()[0] == (
        "-- release-migration: allowedEnvironments=test,backup,prod; "
        "dependsOn=20260626_dcc_basic_data_global_submenu; type=data; riskLevel=medium"
    )


def test_dcc_ai_category_permission_migration_repairs_required_permissions() -> None:
    sql = read_sql()

    for token in [
        "@dcc_project_code_menu_id",
        "@dcc_controlled_file_browser_menu_id",
        "'dcc:project-code:update'",
        "'dcc:controlled-file:update'",
        "DCC项目代码编辑",
        "DCC受控文件编辑",
        "source_menu.`path` = 'controlled-file/categories'",
        "target_menu.`permission` IN (",
        "existing.`tenant_id` = src.`tenant_id`",
    ]:
        assert token in sql, f"permission migration must include {token}"


def test_dcc_ai_category_permission_migration_is_non_destructive() -> None:
    sql = read_sql()
    upper = sql.upper()

    for forbidden in [
        "DELETE FROM `SYSTEM_MENU`",
        "DELETE FROM `SYSTEM_ROLE_MENU`",
        "TRUNCATE TABLE `SYSTEM_MENU`",
        "TRUNCATE TABLE `SYSTEM_ROLE_MENU`",
        "DROP TABLE `SYSTEM_MENU`",
        "DROP TABLE `SYSTEM_ROLE_MENU`",
    ]:
        assert forbidden not in upper

