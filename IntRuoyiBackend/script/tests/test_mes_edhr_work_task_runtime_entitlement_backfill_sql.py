from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]
SQL_PATH = REPO_ROOT / "sql/mysql/20260720_mes_edhr_work_task_runtime_entitlement_backfill.sql"


def read_sql() -> str:
    return SQL_PATH.read_text(encoding="utf-8")


def test_work_task_runtime_entitlement_backfill_declares_release_metadata() -> None:
    sql = read_sql()

    assert sql.startswith(
        "-- release-migration: allowedEnvironments=test,backup,prod; "
        "dependsOn=20260718_system_entitlement_management,20260718_mes_edhr_work_task_ownership,"
        "20260719_mes_edhr_release_lifecycle_adapter,"
        "20260720_mes_edhr_filler_tracking_signature_entitlement,"
        "20260720_mes_edhr_approval_reviewer_runtime_entitlement; type=data; riskLevel=medium\n"
    )


def test_work_task_runtime_entitlement_backfill_targets_only_active_runtime_tasks() -> None:
    sql = read_sql()
    normalized_sql = sql.replace("`", "")

    assert "EDHR_WORK_TASK_ASSIGNEE" in sql
    assert "MES_EDHR_FILLER_MINIMAL" in sql
    assert "MES_EDHR_APPROVAL_REVIEWER_MINIMAL" in sql
    assert "MES_EDHR_RELEASE_APPROVER_MINIMAL" in sql
    assert "task_type IN ('FILL', 'REWORK', 'REVIEW', 'APPROVE', 'RELEASE_APPROVE')" in normalized_sql
    assert "status IN ('TODO', 'OVERDUE')" in normalized_sql
    assert "candidate_user_snapshot" in sql
    assert "JSON_TABLE" in sql
    assert "WHEN task.task_type IN ('REVIEW', 'APPROVE') THEN 'MES_EDHR_APPROVAL_REVIEWER_MINIMAL'" in normalized_sql
    assert "WHERE TRUE\n  ON DUPLICATE KEY UPDATE" in normalized_sql
    assert "COLLATE utf8mb4_unicode_ci" in sql


def test_work_task_runtime_entitlement_backfill_fails_fast_on_bad_prerequisites() -> None:
    sql = read_sql()

    required_messages = {
        "Missing eDHR runtime entitlement policy",
        "Missing eDHR runtime entitlement menu",
        "Active eDHR runtime work task has empty candidate pool",
        "Missing or disabled eDHR runtime work task user",
    }
    for message in required_messages:
        assert message in sql
    assert "SIGNAL SQLSTATE '45000'" in sql


def test_work_task_runtime_entitlement_backfill_uses_ledger_only() -> None:
    sql = read_sql()

    assert "system_entitlement_claim" in sql
    assert "system_entitlement_grant" in sql
    assert "system_entitlement_audit_event" in sql
    assert "WORK_TASK|" in sql
    assert "active_claim_count" in sql

    forbidden_static_mutations = {
        "system_user_role",
        "system_role_menu",
        "INSERT INTO system_role",
        "INSERT INTO `system_role`",
    }
    normalized_sql = sql.lower()
    for forbidden in forbidden_static_mutations:
        assert forbidden.lower() not in normalized_sql
