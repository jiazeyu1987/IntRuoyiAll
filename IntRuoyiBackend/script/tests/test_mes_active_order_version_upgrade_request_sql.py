import re
from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]
SQL_PATH = REPO_ROOT / "sql" / "mysql" / "20260902_mes_active_order_version_upgrade_request.sql"
BPM_SEED_SQL_PATH = REPO_ROOT / "sql" / "mysql" / "20260902_mes_active_order_version_upgrade_bpm_seed.sql"


def _read_sql() -> str:
    assert SQL_PATH.exists(), "missing active-order version-upgrade request migration"
    return SQL_PATH.read_text(encoding="utf-8")


def _read_bpm_seed_sql() -> str:
    assert BPM_SEED_SQL_PATH.exists(), "missing active-order version-upgrade BPM seed migration"
    return BPM_SEED_SQL_PATH.read_text(encoding="utf-8")


def test_active_order_version_upgrade_request_migration_has_release_metadata() -> None:
    sql = _read_sql()

    assert sql.startswith(
        "-- release-migration: allowedEnvironments=test,backup,prod; "
        "dependsOn=20260822_mes_process_pool_active_order_completion_receipt,20260719_business_approval_policy,20260902_mes_active_order_version_upgrade_bpm_seed; "
        "type=schema; riskLevel=medium"
    )


def test_active_order_version_upgrade_request_migration_creates_persistence_contract() -> None:
    sql = _read_sql()

    required = [
        "CREATE TABLE IF NOT EXISTS `mes_pro_process_pool_active_order_version_upgrade_request`",
        "`source_active_order_id` bigint NOT NULL",
        "`source_work_order_id` bigint NOT NULL",
        "`target_active_order_id` bigint DEFAULT NULL",
        "`request_code` varchar(64) NOT NULL",
        "`idempotency_key` varchar(128) NOT NULL",
        "`request_status` varchar(32) NOT NULL",
        "`approval_status` varchar(32) NOT NULL",
        "`freeze_status` varchar(32) NOT NULL",
        "`current_snapshot_json` json NOT NULL",
        "`target_snapshot_json` json NOT NULL",
        "`snapshot_hash` char(64) NOT NULL",
        "UNIQUE KEY `uk_mes_pp_active_order_upgrade_idempotency` (`tenant_id`, `source_active_order_id`, `idempotency_key`, `deleted`)",
        "KEY `idx_mes_pp_active_order_upgrade_source_status` (`tenant_id`, `source_active_order_id`, `request_status`, `deleted`)",
    ]
    for snippet in required:
        assert snippet in sql


def test_active_order_version_upgrade_request_migration_seeds_bpm_policy() -> None:
    sql = _read_sql()

    required = [
        "bpm_business_approval_policy",
        "'MES'",
        "'MES_ACTIVE_ORDER'",
        "'VERSION_UPGRADE_RESTART'",
        "'VERSION_UPGRADE_PENDING'",
        "'BPM_REQUIRED'",
        "'mes-active-order-version-upgrade-v1'",
        "'MES_ACTIVE_ORDER_VERSION_UPGRADE_RESTART'",
        "Active order version-upgrade restart approval policy",
    ]
    for snippet in required:
        assert snippet in sql

    assert "MES active-order version-upgrade policy requires bpm_business_approval_policy" in sql
    assert "Conflicting published MES active-order version-upgrade approval policy" in sql


def test_active_order_version_upgrade_request_migration_seeds_button_permission() -> None:
    sql = _read_sql()

    required = [
        "`system_menu`",
        "'活跃订单版本升级'",
        "'mes:pro-process-pool-team-leader:version-upgrade'",
        "`parent`.`id` = 900310",
        "MES active-order version-upgrade permission menu is required",
        "`system_role_menu`",
        "'pqc_leader_permission'",
        "'super_admin'",
        "'mes:pro-process-pool-team-leader:maintain'",
    ]
    for snippet in required:
        assert snippet in sql


def test_active_order_version_upgrade_request_migration_is_additive() -> None:
    upper_sql = _read_sql().upper()

    assert "DROP TABLE" not in upper_sql
    assert "TRUNCATE TABLE" not in upper_sql
    assert "DELETE FROM" not in upper_sql
    assert re.search(r"(^|;)\s*UPDATE\s+", upper_sql) is None


def test_active_order_version_upgrade_bpm_seed_creates_process_definition() -> None:
    sql = _read_bpm_seed_sql()

    assert sql.startswith(
        "-- release-migration: allowedEnvironments=test,backup,prod; "
        "dependsOn=20260719_business_approval_policy; type=seed; riskLevel=low"
    )

    required = [
        "CREATE PROCEDURE ensure_mes_active_order_version_upgrade_bpm_seed()",
        "'mes-active-order-version-upgrade-v1'",
        "活跃订单升级重启审批",
        "activeOrderVersionUpgradeApprove",
        "flowable:candidateStrategy>10</flowable:candidateStrategy",
        "mes_route_version_admin",
        "bpm_process_definition_info",
        "act_re_procdef",
        "act_re_model",
        "act_ge_bytearray",
    ]
    for snippet in required:
        assert snippet in sql


def test_active_order_version_upgrade_bpm_seed_is_additive() -> None:
    upper_sql = _read_bpm_seed_sql().upper()

    assert "DROP TABLE" not in upper_sql
    assert "TRUNCATE TABLE" not in upper_sql
    assert "DELETE FROM" not in upper_sql
    assert re.search(r"(^|;)\s*UPDATE\s+(?!`ACT_RE_PROCDEF`)", upper_sql) is None
