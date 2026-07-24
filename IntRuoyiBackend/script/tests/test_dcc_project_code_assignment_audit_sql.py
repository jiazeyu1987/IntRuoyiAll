from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]
SQL_PATH = REPO_ROOT / "sql" / "mysql" / "20260712_dcc_project_code_assignment_audit.sql"
ASSIGNEE_MENU_REPAIR_SQL_PATH = (
    REPO_ROOT / "sql" / "mysql" / "20260713_dcc_project_code_assignment_assignee_menu_repair.sql"
)


def read_sql() -> str:
    assert SQL_PATH.exists(), "DCC project-code assignment audit migration must exist"
    return SQL_PATH.read_text(encoding="utf-8")


def read_assignee_menu_repair_sql() -> str:
    assert ASSIGNEE_MENU_REPAIR_SQL_PATH.exists(), (
        "DCC project-code assignment assignee menu repair migration must exist"
    )
    return ASSIGNEE_MENU_REPAIR_SQL_PATH.read_text(encoding="utf-8")


def test_dcc_project_code_assignment_audit_migration_has_release_metadata() -> None:
    sql = read_sql()

    assert sql.splitlines()[0] == (
        "-- release-migration: allowedEnvironments=test,backup,prod; "
        "dependsOn=20260710_dcc_product_catalog_database; type=schema; riskLevel=medium"
    )


def test_dcc_project_code_assignment_audit_migration_creates_required_tables() -> None:
    sql = read_sql()

    for table in [
        "`dcc_project_code_assignment`",
        "`dcc_project_code_assignment_file`",
        "`dcc_controlled_file_metadata_change`",
        "`dcc_controlled_file_metadata_change_item`",
    ]:
        assert f"CREATE TABLE IF NOT EXISTS {table}" in sql, f"migration must create {table}"


def test_dcc_project_code_assignment_audit_migration_scopes_permissions() -> None:
    sql = read_sql()

    for token in [
        "'dcc:project-code-assignment:assign'",
        "'dcc:project-code-assignment:query'",
        "'dcc:project-code-assignment:revoke'",
        "'dcc:project-code-assignment:execute'",
        "'dcc:project-code-assignment:audit:query'",
        "target_menu.`path` = 'controlled-file/project-code-assignments/mine'",
        "role_admin.`code` IN ('super_admin', 'doc_control', 'wenkong')",
        "target_menu.`path` = 'controlled-file/project-code-assignment-audit'",
    ]:
        assert token in sql, f"migration must include scoped permission contract {token}"


def test_dcc_project_code_assignment_audit_migration_is_non_destructive() -> None:
    sql = read_sql().upper()

    for forbidden in [
        "TRUNCATE TABLE",
        "DROP TABLE",
        "DELETE FROM `DCC_CONTROLLED_FILE`",
        "DELETE FROM `SYSTEM_MENU`",
        "DELETE FROM `SYSTEM_ROLE_MENU`",
    ]:
        assert forbidden not in sql, f"migration must not contain destructive SQL: {forbidden}"


def test_dcc_project_code_assignment_assignee_menu_repair_has_release_metadata() -> None:
    sql = read_assignee_menu_repair_sql()

    assert sql.splitlines()[0] == (
        "-- release-migration: allowedEnvironments=test,backup,prod; "
        "dependsOn=20260712_dcc_project_code_assignment_audit; type=menu; riskLevel=low"
    )


def test_dcc_project_code_assignment_assignee_menu_repair_grants_only_controlled_entry() -> None:
    sql = read_assignee_menu_repair_sql()

    for token in [
        "`dcc_project_code_assignment` assignment",
        "`system_user_role` user_role",
        "target_menu.`path` = 'controlled-file/project-code-assignments/mine'",
        "root_menu.`path` = '/dcc'",
        "leaf_menu.`path` = 'project-code'",
        "parent_menu.`path` = '/mdm'",
    ]:
        assert token in sql, f"assignee repair migration must include {token}"

    for forbidden_permission in [
        "dcc:project-code:create",
        "dcc:project-code:update",
        "dcc:project-code:delete",
        "dcc:project-code:import",
        "dcc:project-code:export",
    ]:
        assert forbidden_permission not in sql, (
            f"assignee repair migration must not grant management permission {forbidden_permission}"
        )


def test_dcc_project_code_assignment_assignee_menu_repair_is_non_destructive() -> None:
    sql = read_assignee_menu_repair_sql().upper()

    for forbidden in [
        "TRUNCATE TABLE",
        "DROP TABLE",
        "DELETE FROM `DCC_PROJECT_CODE_ASSIGNMENT`",
        "DELETE FROM `SYSTEM_MENU`",
        "DELETE FROM `SYSTEM_ROLE_MENU`",
        "UPDATE `SYSTEM_ROLE_MENU`",
    ]:
        assert forbidden not in sql, f"assignee repair migration must not contain destructive SQL: {forbidden}"
