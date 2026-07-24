from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]
SQL_PATH = REPO_ROOT / "sql/mysql/20260718_mes_edhr_work_task_ownership.sql"
CANDIDATE_NULLABLE_SQL_PATH = (
    REPO_ROOT / "sql/mysql/20260723_mes_edhr_assignment_rule_candidate_nullable.sql"
)


def test_work_task_ownership_migration_is_additive_and_idempotent() -> None:
    assert SQL_PATH.exists(), "missing eDHR work task ownership migration"
    sql = SQL_PATH.read_text(encoding="utf-8")

    assert sql.startswith(
        "-- release-migration: allowedEnvironments=test,backup,prod; "
        "dependsOn=20260718_system_entitlement_management; type=schema; riskLevel=medium"
    )
    assert "ensure_mes_edhr_work_task_ownership_table" in sql
    assert "ensure_mes_edhr_work_task_ownership_column" in sql
    assert "ensure_mes_edhr_work_task_ownership_index" in sql
    assert "SIGNAL SQLSTATE '45000'" in sql

    required_columns = {
        "responsibility_source_type",
        "responsibility_source_key",
        "responsibility_source_version",
        "responsibility_source_digest",
        "ownership_locked",
        "ownership_last_transferred_at",
        "ownership_last_transferred_by",
    }
    for column in required_columns:
        assert f"'{column}'" in sql
    assert "ADD COLUMN `', p_column_name, '`" in sql
    assert "ADD COLUMN `" in sql

    assert "`idx_mes_pro_edhr_work_task_resp_source`" in sql


def test_work_task_ownership_migration_does_not_grant_static_permissions() -> None:
    sql = SQL_PATH.read_text(encoding="utf-8").upper()

    forbidden_fragments = {
        "INSERT INTO `SYSTEM_USER_ROLE`",
        "UPDATE `SYSTEM_USER_ROLE`",
        "DELETE FROM `SYSTEM_USER_ROLE`",
        "INSERT INTO `SYSTEM_ROLE_MENU`",
        "UPDATE `SYSTEM_ROLE_MENU`",
        "DELETE FROM `SYSTEM_ROLE_MENU`",
        "MES:PRO-EDHR-BATCH-EXECUTION:CREATE",
        "MES:PRO-EDHR-BATCH-EXECUTION:CLOSE",
        "MES:PRO-EDHR-BATCH-EXECUTION:ARCHIVE",
        "SYSTEM:MENU:UPDATE",
    }
    for fragment in forbidden_fragments:
        assert fragment not in sql


def test_assignment_rule_candidate_nullable_migration_is_idempotent_and_scoped() -> None:
    assert CANDIDATE_NULLABLE_SQL_PATH.exists(), "missing eDHR assignment rule candidate nullable migration"
    sql = CANDIDATE_NULLABLE_SQL_PATH.read_text(encoding="utf-8")
    upper_sql = sql.upper()

    assert sql.startswith(
        "-- release-migration: allowedEnvironments=test,backup,prod; "
        "dependsOn=20260614_mes_edhr_work_task_candidate_pool.sql; type=schema; riskLevel=medium"
    )
    assert "ensure_mes_edhr_assignment_rule_candidate_nullable" in sql
    assert "information_schema.COLUMNS" in sql
    assert "TABLE_NAME = 'mes_pro_edhr_work_task_assignment_rule'" in sql
    assert "COLUMN_NAME = 'assignee_user_id'" in sql
    assert "IS_NULLABLE = 'NO'" in sql
    assert (
        "MODIFY COLUMN `assignee_user_id` bigint DEFAULT NULL COMMENT "
        "'任务责任人用户ID；候选池规则可为空'"
    ) in sql

    assert "DROP TABLE" not in upper_sql
    assert "TRUNCATE" not in upper_sql
    assert "DELETE FROM" not in upper_sql
    assert "INSERT INTO `SYSTEM_ROLE_MENU`" not in upper_sql
