from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]
SQL_PATH = REPO_ROOT / "sql/mysql/20260719_mes_edhr_release_lifecycle_adapter.sql"


def read_sql() -> str:
    assert SQL_PATH.exists(), "missing eDHR release lifecycle adapter migration"
    return SQL_PATH.read_text(encoding="utf-8")


def test_release_lifecycle_adapter_policy_and_notify_seed_are_minimal() -> None:
    sql = read_sql()

    assert sql.startswith(
        "-- release-migration: allowedEnvironments=test,backup,prod; "
        "dependsOn=20260718_system_entitlement_management,20260611_mes_edhr_work_task_flow; "
        "type=data; riskLevel=low\n"
    )
    assert "MES_EDHR_RELEASE_APPROVER_MINIMAL" in sql
    assert "MES_EDHR_RELEASE_APPROVE_TASK_ASSIGNED" in sql
    assert "RELEASE_APPROVE" in sql
    for permission in {
        "bpm:task:query",
        "bpm:task:update",
        "mes:pro-edhr-work-task:query",
        "mes:pro-edhr-batch-execution:query",
        "mes:pro-edhr-release:query",
        "mes:pro-edhr-release:approve",
        "mes:pro-edhr-release:reject",
        "mes:pro-edhr-release:event-query",
    }:
        assert permission in sql
    for template_param in {
        '"workOrderCode"',
        '"batchCode"',
        '"processName"',
        '"actionUrl"',
        '"workTaskId"',
        '"reason"',
    }:
        assert template_param in sql


def test_release_lifecycle_adapter_sql_does_not_mutate_static_roles_or_users() -> None:
    sql = read_sql()
    normalized_sql = sql.upper()

    for forbidden in {
        "INSERT INTO `SYSTEM_ROLE_MENU`",
        "UPDATE `SYSTEM_ROLE_MENU`",
        "DELETE FROM `SYSTEM_ROLE_MENU`",
        "INSERT INTO `SYSTEM_USER_ROLE`",
        "UPDATE `SYSTEM_USER_ROLE`",
        "DELETE FROM `SYSTEM_USER_ROLE`",
        "INSERT INTO `SYSTEM_ROLE`",
        "INSERT INTO `SYSTEM_MENU`",
        "UPDATE `SYSTEM_MENU`",
        "ON DUPLICATE KEY UPDATE",
        "INSERT IGNORE",
    }:
        assert forbidden not in normalized_sql
