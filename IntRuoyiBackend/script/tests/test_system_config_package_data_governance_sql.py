from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
SQL = ROOT / "sql" / "mysql" / "20260615_system_config_package_data_governance.sql"

MISSING_MENU_ROOT_IDS = (
    2161,
    6402,
    6411,
    6510,
    6515,
    6520,
    6530,
    6540,
    6545,
    6550,
    6560,
    6571,
    6580,
    980109,
    980110,
    980111,
    980112,
    980113,
    980114,
)


def read_sql() -> str:
    assert SQL.exists(), f"missing required SQL file: {SQL}"
    return SQL.read_text(encoding="utf-8")


def test_sql_is_fail_fast_and_scoped_to_local_source_tenant() -> None:
    sql = read_sql()

    assert "CREATE PROCEDURE govern_system_config_package_data" in sql
    assert "SIGNAL SQLSTATE '45000'" in sql
    assert "`tenant_id` = 1" in sql
    assert "'芋道源码'" in sql
    assert "'admin'" in sql
    assert "ROLLBACK GUIDE" in sql
    assert "ON DUPLICATE KEY UPDATE" not in sql
    assert "TRUNCATE " not in sql.upper()
    assert "DELETE FROM `system_menu`" not in sql


def test_sql_retires_missing_component_menu_subtrees_from_all_bindings() -> None:
    sql = read_sql()

    for menu_id in MISSING_MENU_ROOT_IDS:
        assert f"({menu_id}, " in sql

    for token in (
        "tmp_config_governance_missing_menu_roots",
        "tmp_config_governance_retired_menu_tree",
        "WITH RECURSIVE `menu_tree`",
        "UNION DISTINCT",
        "UPDATE `system_role_menu`",
        "UPDATE `system_menu`",
        "JSON_TABLE",
        "JSON_ARRAYAGG",
        "system_tenant_package",
    ):
        assert token in sql


def test_sql_repairs_real_references_without_fabricating_missing_post() -> None:
    sql = read_sql()

    for token in (
        "UPDATE `system_dept`",
        "`id` IN (100, 101, 102, 103, 104, 105, 107, 108, 112)",
        "`leader_user_id` = NULL",
        "UPDATE `system_post`",
        "`id` = 2",
        "UPDATE `system_user_post`",
        "UPDATE `system_user_role`",
        "tmp_config_governance_user_post_json",
    ):
        assert token in sql

    assert "INSERT INTO `system_post`" not in sql
    assert "post_id` = 1" in sql


def test_sql_restores_missing_dict_types_and_postconditions() -> None:
    sql = read_sql()

    for token in (
        "910401, '菜单类型', 'system_menu_type'",
        "910402, '数据范围', 'system_data_scope'",
        "910403, 'MES 领料出库单状态', 'mes_wm_issue_status'",
        "Missing dict type remains after governance",
        "Remaining orphan user role after governance",
        "Remaining invalid role data scope dept after governance",
        "Remaining tenant package retired menu after governance",
    ):
        assert token in sql
