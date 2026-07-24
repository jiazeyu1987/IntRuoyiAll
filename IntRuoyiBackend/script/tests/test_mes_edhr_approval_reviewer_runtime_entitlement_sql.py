from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]
SQL_PATH = REPO_ROOT / "sql/mysql/20260720_mes_edhr_approval_reviewer_runtime_entitlement.sql"


def read_sql() -> str:
    assert SQL_PATH.exists(), "missing eDHR approval reviewer runtime entitlement migration"
    return SQL_PATH.read_text(encoding="utf-8")


def test_approval_reviewer_runtime_entitlement_declares_release_metadata() -> None:
    sql = read_sql()

    assert sql.startswith(
        "-- release-migration: allowedEnvironments=test,backup,prod; "
        "dependsOn=20260718_system_entitlement_management,20260526_edhr_approval_archive_schema_contract; "
        "type=data; riskLevel=low\n"
    )


def test_approval_reviewer_runtime_entitlement_policy_is_minimal_for_detail_page() -> None:
    sql = read_sql()

    assert "MES_EDHR_APPROVAL_REVIEWER_MINIMAL" in sql
    assert "Sources: EDHR_WORK_TASK_ASSIGNEE taskType=REVIEW/APPROVE" in sql
    for permission in {
        "mes:pro-batch-record-execution:approve",
        "mes:pro-batch-record-execution:track",
        "mes:pro-batch-record-execution:signature-query",
        "mes:pro-batch-record-execution-archive:query",
    }:
        assert permission in sql

    for forbidden_permission in {
        "mes:pro-batch-record-execution:create",
        "mes:pro-batch-record-execution:update",
        "mes:pro-batch-record-execution-archive:create",
        "mes:pro-batch-record-execution-archive:download",
        "mes:pro-edhr-work-task:update",
        "mes:pro-edhr-work-task-rule:update",
        "system:permission:assign-role-menu",
        "system:permission:assign-user-role",
    }:
        assert forbidden_permission in sql


def test_approval_reviewer_runtime_entitlement_does_not_mutate_static_roles_or_menus() -> None:
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
