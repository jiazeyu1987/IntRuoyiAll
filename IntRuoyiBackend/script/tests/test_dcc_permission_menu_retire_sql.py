from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]
DCC_BASE_SQL = REPO_ROOT / "sql" / "mysql" / "20260513_dcc_base_schema.sql"
RENAME_SQL = REPO_ROOT / "sql" / "mysql" / "20260626_dcc_permission_menu_rename.sql"
RETIRE_SQL = REPO_ROOT / "sql" / "mysql" / "20260626_dcc_access_rule_menu_retire.sql"


def read(path: Path) -> str:
    assert path.exists(), f"{path.name} must exist."
    return path.read_text(encoding="utf-8")


def test_dcc_base_schema_renames_permission_menu_and_retires_access_rule_menu() -> None:
    text = read(DCC_BASE_SQL)

    assert "SELECT 6802, 'DCC访问规则'" in text
    assert "'controlled-file/access-rules'" in text
    assert "DccControlledFileAccessRules', 1, b'0'" in text

    assert "SELECT 6803, '文控权限'" in text
    assert "'controlled-file/categories'" in text
    assert "DccControlledFileCategories', 0, b'1'" in text


def test_dcc_permission_menu_rename_sql_is_non_destructive_and_targets_6803() -> None:
    text = read(RENAME_SQL)
    upper = text.upper()

    assert text.splitlines()[0] == (
        "-- release-migration: allowedEnvironments=test,backup,prod; "
        "dependsOn=20260513_dcc_base_schema; type=data; riskLevel=low"
    )
    assert "SET `name` = '文控权限'" in text
    assert "`id` = 6803" in text
    assert "`path` = 'controlled-file/categories'" in text

    for forbidden in ["DELETE FROM `SYSTEM_MENU`", "TRUNCATE TABLE `SYSTEM_MENU`", "DROP TABLE `SYSTEM_MENU`"]:
        assert forbidden not in upper


def test_dcc_access_rule_menu_retire_sql_is_non_destructive_and_targets_6802() -> None:
    text = read(RETIRE_SQL)
    upper = text.upper()

    assert text.splitlines()[0] == (
        "-- release-migration: allowedEnvironments=test,backup,prod; "
        "dependsOn=20260513_dcc_base_schema; type=data; riskLevel=low"
    )
    assert "SET `visible` = b'0'" in text
    assert "`status` = 1" in text
    assert "`id` = 6802" in text
    assert "`path` = 'controlled-file/access-rules'" in text

    for forbidden in ["DELETE FROM `SYSTEM_MENU`", "TRUNCATE TABLE `SYSTEM_MENU`", "DROP TABLE `SYSTEM_MENU`"]:
        assert forbidden not in upper
