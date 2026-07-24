from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
SQL_PATH = ROOT / "sql" / "mysql" / "20260721_admin_full_scope_role_standardization.sql"


def read_sql() -> str:
    assert SQL_PATH.exists(), "missing full-scope admin role standardization migration"
    return SQL_PATH.read_text(encoding="utf-8")


def test_migration_metadata_roles_and_fail_fast_guards() -> None:
    sql = read_sql()

    assert sql.splitlines()[0] == (
        "-- release-migration: allowedEnvironments=test,backup,prod; "
        "dependsOn=20260718_bpm_admin_role_assignment,20260721_approval_center_admin_role_scope,"
        "20260714_dcc_controlled_file_logs_consolidation,20260714_unified_signature_records_menu; "
        "type=menu; riskLevel=medium"
    )
    assert "SET NAMES utf8mb4;" in sql
    assert "START TRANSACTION;" in sql
    assert "ensure_admin_full_scope_role_standardization_20260721" in sql
    assert "SIGNAL SQLSTATE '45000'" in sql

    for required_guard in [
        "Missing enabled tenant 1 admin user",
        "Missing tenant 1 menu role category",
        "Missing tenant 1 bpm_admin role",
        "Missing tenant 1 approval_admin role",
        "Missing enabled full-scope admin menu",
    ]:
        assert required_guard in sql


def test_all_full_scope_roles_use_admin_names_and_scope_remarks() -> None:
    sql = read_sql()

    expected_roles = {
        "bpm_admin": (
            "BPM管理员",
            "BPM管理员角色；可管理表单中心、流程模型、流程实例、流程任务和流程配置，并可查看或取消全量 BPM 流程实例。",
        ),
        "approval_admin": (
            "审批中心管理员",
            "审批中心管理员角色；可全量查看统一审批中心任务、详情、轨迹、导出和统计，普通用户仍只看本人发起、本人审批、抄送或授权相关审批信息。",
        ),
        "audit_admin": (
            "审计管理员",
            "审计管理员角色；可全量查询、导出系统日志、操作审计、审批日志和签名证据账本，普通用户仍按本人相关或对象级授权查看审计信息。",
        ),
    }

    for code, (name, remark) in expected_roles.items():
        assert f"code = '{code}'" in sql or f"'{code}'" in sql
        assert name in sql
        assert remark in sql

    assert "审批管理员" not in sql
    assert "审批中心全量可见管理员角色" not in sql
    assert "审批中心流程管理菜单及 BPM 配置维护权限" not in sql


def test_admin_user_is_assigned_to_each_full_scope_admin_role() -> None:
    sql = read_sql()

    assert "WHERE username = 'admin'" in sql
    assert "v_admin_user_id" in sql
    for role_var in ["v_bpm_admin_role_id", "v_approval_admin_role_id", "v_audit_admin_role_id"]:
        assert f"SELECT v_admin_user_id, {role_var}" in sql

    assert "INSERT INTO system_user_role" in sql
    assert "UPDATE system_user_role" in sql
    assert "WHERE NOT EXISTS (" in sql


def test_audit_admin_receives_system_audit_approval_and_signature_ledger_menus() -> None:
    sql = read_sql()

    assert "tmp_audit_admin_expected_menu" in sql
    expected_menu_ids = [
        108,
        500,
        501,
        1040,
        1042,
        1043,
        1045,
        1083,
        1078,
        1088,
        1082,
        1084,
        1085,
        1086,
        1089,
        1093,
        1107,
        1108,
        1109,
        2130,
        2141,
        2142,
        6800,
        6818,
        990225,
        900220,
        900356,
        900357,
        900218,
        900411,
        6815,
        900026,
    ]
    for menu_id in expected_menu_ids:
        assert f"SELECT {menu_id}" in sql

    for permission in [
        "system:operate-log:query",
        "system:operate-log:export",
        "system:login-log:query",
        "infra:api-access-log:query",
        "infra:api-error-log:query",
        "dcc:controlled-file:audit:query",
        "dcc:project-code-assignment:audit:query",
        "mes:pro-edhr-flow-intervention:event-query",
        "signature-governance:policy:query",
        "mes:pro-batch-record-execution:signature-query",
    ]:
        assert permission in sql


def test_audit_admin_expected_permission_temp_table_uses_system_menu_collation() -> None:
    sql = read_sql()

    start = sql.index("CREATE TEMPORARY TABLE tmp_audit_admin_expected_permission")
    end = sql.index("INSERT INTO tmp_audit_admin_expected_permission", start)
    table_ddl = sql[start:end]

    assert "permission VARCHAR(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL PRIMARY KEY" in table_ddl


def test_project_code_assignment_audit_permission_is_not_bound_to_preferred_menu_id() -> None:
    sql = read_sql()

    start = sql.index("INSERT INTO tmp_audit_admin_expected_menu")
    end = sql.index("DROP TEMPORARY TABLE IF EXISTS tmp_audit_admin_expected_permission", start)
    expected_menu_block = sql[start:end]

    assert "SELECT 990226" not in expected_menu_block
    assert "SELECT 'dcc:project-code-assignment:audit:query'" in sql


def test_migration_soft_restricts_full_scope_audit_menus_to_audit_and_super_admin() -> None:
    sql = read_sql()
    upper_sql = sql.upper()

    for forbidden in [
        "DELETE FROM SYSTEM_ROLE_MENU",
        "DELETE FROM `SYSTEM_ROLE_MENU`",
        "DELETE FROM SYSTEM_USER_ROLE",
        "DELETE FROM `SYSTEM_USER_ROLE`",
        "TRUNCATE TABLE SYSTEM_ROLE_MENU",
        "DROP TABLE SYSTEM_ROLE_MENU",
    ]:
        assert forbidden not in upper_sql

    assert "SET role_menu.deleted = b'1'" in sql
    assert "tmp_audit_admin_restricted_menu" in sql
    assert "JOIN tmp_audit_admin_expected_permission expected_permission" in sql
    assert "expected_permission.permission = menu.permission" in sql
    assert "JOIN tmp_audit_admin_restricted_menu restricted_menu" in sql
    assert "role.code NOT IN ('audit_admin', 'super_admin')" in sql
    assert "SET role_menu.deleted = b'0'" in sql
    assert "INSERT INTO system_role_menu" in sql
    assert "COMMIT;" in sql
