from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
BASELINE_SQL = ROOT / "sql" / "mysql" / "ruoyi-vue-pro.sql"
DCC_BASE_SQL = ROOT / "sql" / "mysql" / "20260513_dcc_base_schema.sql"
MIGRATION_SQL = ROOT / "sql" / "mysql" / "20260626_role_management_split_rename_navigation.sql"


def read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def test_role_management_split_baseline_contract():
    source = read(BASELINE_SQL)

    for token in [
        "VALUES (101, '角色管理', '', 1, 2, 1, 'role', 'ep:user', '', NULL",
        "VALUES (900183, '权限角色', '', 2, 1, 101, 'permission-role'",
        "VALUES (104, '组织角色', '', 2, 2, 101, 'organization-role'",
        "900183, '', '#', '', NULL, 0, b'1', b'1', b'1'",
        "VALUES (1008, '角色查询', 'system:role:query', 3, 1, 900183",
        "VALUES (1065, '设置用户角色', 'system:permission:assign-user-role', 3, 8, 900183",
        "100,101,900183,104,102,1126"
    ]:
        assert token in source, f"ruoyi-vue-pro.sql 必须包含角色管理三分基线契约: {token}"


def test_role_management_split_dcc_seed_contract():
    source = read(DCC_BASE_SQL)

    for token in [
        "SELECT 6804, '审批角色', 'dcc:controlled-file:position:manage', 2, 3, 101, 'approval-role'",
        "WHERE `permission` = 'dcc:controlled-file:position:manage'"
    ]:
        assert token in source, f"20260513_dcc_base_schema.sql 必须包含审批角色种子契约: {token}"


def test_role_management_split_increment_contract():
    source = read(MIGRATION_SQL)

    assert source.startswith(
        "-- release-migration: allowedEnvironments=test,backup,prod; "
        "dependsOn=20260513_dcc_base_schema; type=data; riskLevel=medium\n"
    )

    for token in [
        "INSERT INTO `system_menu`",
        "SELECT 900183, '权限角色'",
        "UPDATE `system_menu`",
        "`id` = 101",
        "`menu_id` IN (101, 1008, 1009, 1010, 1011, 1012, 1063, 1064, 1065)",
        "`menu_id` IN (104, 1021, 1022, 1023, 1024, 1025)",
        "`menu_id` IN (900183, 104, 1021, 1022, 1023, 1024, 1025, 6804)",
        "JSON_CONTAINS",
        "CAST('900183' AS JSON)",
        "CAST('6804' AS JSON)"
    ]:
        assert token in source, f"20260626_role_management_split_rename_navigation.sql 必须包含迁移契约: {token}"
