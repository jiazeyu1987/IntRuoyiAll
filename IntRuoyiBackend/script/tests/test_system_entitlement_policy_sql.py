from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]
SQL_PATH = REPO_ROOT / "sql/mysql/20260718_system_entitlement_management.sql"
POLICY_DO_PATH = (
    REPO_ROOT
    / "yudao-module-system/src/main/java/cn/iocoder/yudao/module/system/dal/dataobject/permission/SystemEntitlementPolicyDO.java"
)


def test_entitlement_migration_creates_ledger_tables_and_policy_seed() -> None:
    sql = SQL_PATH.read_text(encoding="utf-8")

    required_tables = {
        "system_entitlement_policy",
        "system_entitlement_claim",
        "system_entitlement_grant",
        "system_entitlement_audit_event",
    }
    for table in required_tables:
        assert table in sql

    assert "MES_EDHR_FILLER_MINIMAL" in sql
    assert "EDHR_PROCESS_FORM_FILLER" in sql
    assert "EDHR_WORK_TASK_ASSIGNEE" in sql


def test_entitlement_migration_declares_release_metadata() -> None:
    sql = SQL_PATH.read_text(encoding="utf-8")

    assert sql.startswith(
        "-- release-migration: allowedEnvironments=test,backup,prod; "
        "dependsOn=; type=schema; riskLevel=medium\n"
    )


def test_entitlement_policy_seed_is_minimal_and_does_not_mutate_static_roles() -> None:
    sql = SQL_PATH.read_text(encoding="utf-8")

    required_permissions = {
        "mes:pro-edhr-batch-execution:query",
        "mes:pro-edhr-batch-execution:update",
        "mes:pro-batch-record-execution:query",
        "mes:pro-batch-record-execution:update",
        "mes:pro-batch-record-execution:track",
        "mes:pro-batch-record-execution:signature-query",
        "mes:pro-edhr-work-task:query",
    }
    for permission in required_permissions:
        assert permission in sql

    forbidden_permissions = {
        "mes:pro-edhr-batch-execution:create",
        "mes:pro-edhr-batch-execution:close",
        "mes:pro-edhr-batch-execution:archive",
        "mes:pro-edhr-batch-execution:overview",
        "mes:pro-edhr-change:void",
        "mes:pro-edhr-batch-execution:quality-reject",
        "mes:pro-edhr-work-task:update",
        "mes:pro-edhr-work-task-rule:update",
        "system:menu:update",
        "system:permission:assign-role-menu",
        "system:permission:assign-user-role",
    }
    for permission in forbidden_permissions:
        assert permission not in sql

    forbidden_static_role_mutations = {
        "INSERT INTO `system_user_role`",
        "UPDATE `system_user_role`",
        "DELETE FROM `system_user_role`",
        "INSERT INTO `system_role_menu`",
        "UPDATE `system_role_menu`",
        "DELETE FROM `system_role_menu`",
    }
    normalized_sql = sql.upper()
    for statement in forbidden_static_role_mutations:
        assert statement.upper() not in normalized_sql


def test_entitlement_policy_do_ignores_tenant_interceptor_for_global_policy_table() -> None:
    source = POLICY_DO_PATH.read_text(encoding="utf-8")

    assert "import cn.iocoder.yudao.framework.tenant.core.aop.TenantIgnore;" in source
    assert "@TenantIgnore" in source
