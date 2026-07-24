from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]
SQL_PATH = REPO_ROOT / "sql" / "mysql" / "20260714_dcc_controlled_file_logs_consolidation.sql"


def read_sql() -> str:
    assert SQL_PATH.exists(), "DCC controlled-file logs consolidation SQL must exist."
    return SQL_PATH.read_text(encoding="utf-8")


def executable_sql(sql: str) -> str:
    return "\n".join(
        line for line in sql.splitlines()
        if not line.lstrip().startswith("--")
    ).upper()


def test_controlled_file_log_menu_replaces_visible_legacy_pages() -> None:
    sql = read_sql()

    for required in [
        "release-migration: allowedEnvironments=test,backup,prod",
        "SET NAMES utf8mb4",
        "SET @dcc_controlled_file_log_menu_id := 6818",
        "文控日志",
        "dcc:controlled-file:log:query",
        "controlled-file/logs",
        "dcc/controlled-file/logs/index",
        "DccControlledFileLogs",
        "Missing DCC controlled-file log menu 6818",
    ]:
        assert required in sql

    assert "`type` = 2" in sql
    assert "`parent_id` = 6800" in sql
    assert "`visible` = b'1'" in sql
    assert "Legacy DCC audit or assignment page menu still visible" in sql

    for legacy_route in [
        "controlled-file/audit",
        "controlled-file/project-code-assignment-audit",
        "controlled-file/project-code-assignments/mine",
        "dcc/controlled-file/audit/index",
        "dcc/controlled-file/project-code-assignment-audit/index",
        "dcc/controlled-file/project-code-assignments/mine/index",
    ]:
        assert legacy_route in sql


def test_legacy_backend_permissions_are_retained_as_non_route_permissions() -> None:
    sql = read_sql()

    for required in [
        "DCC受控文件审计查询权限",
        "DCC项目代码修正追溯查询权限",
        "DCC项目代码修正执行权限",
        "dcc:controlled-file:audit:query",
        "dcc:project-code-assignment:audit:query",
        "dcc:project-code-assignment:execute",
        "`type` = 3",
        "`path` = ''",
        "`component` = ''",
        "`component_name` = ''",
        "`visible` = b'0'",
        "Missing legacy DCC controlled-file audit permission",
        "Missing legacy DCC project-code assignment audit permission",
        "Missing legacy DCC project-code assignment execute permission",
    ]:
        assert required in sql


def test_roles_with_legacy_entries_are_granted_new_log_menu_without_destructive_sql() -> None:
    sql = read_sql()
    upper_sql = executable_sql(sql)

    for required in [
        "tmp_dcc_controlled_file_log_source_roles",
        "system_role_menu",
        "source_role.`role_id`, @dcc_controlled_file_log_menu_id",
        "existing.`tenant_id` = source_role.`tenant_id`",
        "SIGNAL SQLSTATE '45000'",
        "CALL `ensure_dcc_controlled_file_logs_consolidation`()",
    ]:
        assert required in sql

    assert "DELETE FROM" not in upper_sql
    assert "TRUNCATE" not in upper_sql
    assert "DROP TABLE `SYSTEM_MENU`" not in upper_sql
    assert "DROP TABLE `SYSTEM_ROLE_MENU`" not in upper_sql
