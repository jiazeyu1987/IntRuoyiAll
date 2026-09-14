import re
from pathlib import Path


BACKEND_ROOT = Path(__file__).resolve().parents[2]
MIGRATION_PATH = (
    BACKEND_ROOT
    / "sql"
    / "mysql"
    / "20260805_mes_process_pool_device_parameter_route_process_constraints.sql"
)


def read_migration() -> str:
    assert MIGRATION_PATH.exists(), f"missing migration: {MIGRATION_PATH}"
    return MIGRATION_PATH.read_text(encoding="utf-8")


def test_migration_declares_complete_release_and_recovery_contract() -> None:
    sql = read_migration()

    assert sql.startswith(
        "-- release-migration: allowedEnvironments=test,backup,prod; "
        "dependsOn=20260731_mes_process_pool_team_leader_p1_runtime_config; "
        "type=schema; riskLevel=medium\n"
    )
    assert "-- Recovery:" in sql
    assert "-- Rollback blocker:" in sql


def test_migration_fails_fast_for_nulls_across_all_history() -> None:
    sql = read_migration()
    normalized = " ".join(sql.split())

    null_guard_match = re.search(
        r"SELECT COUNT\(\*\) INTO v_null_rule_count "
        r"FROM `mes_pro_process_pool_device_parameter_rule` "
        r"WHERE `route_process_id` IS NULL OR `default_value` IS NULL",
        normalized,
        re.IGNORECASE,
    )
    assert null_guard_match is not None

    signal_position = normalized.index("SIGNAL SQLSTATE '45000'", null_guard_match.end())
    guard_sql = normalized[null_guard_match.start() : signal_position]
    assert "deleted" not in guard_sql.lower()
    assert "formal data governance" in normalized


def test_migration_enforces_route_process_target_and_unique_identity() -> None:
    sql = read_migration()
    normalized = " ".join(sql.split())

    assert (
        "MODIFY COLUMN `route_process_id` bigint NOT NULL COMMENT '工艺路线工序ID'"
        in normalized
    )
    assert (
        "MODIFY COLUMN `default_value` decimal(24,6) NOT NULL COMMENT '目标值'"
        in normalized
    )
    assert "DROP INDEX `uk_mes_pp_device_parameter_rule`" in normalized
    assert (
        "ADD UNIQUE KEY `uk_mes_pp_device_parameter_route_process` "
        "(`tenant_id`, `route_process_id`, `device_id`, `parameter_code`, `deleted`)"
        in normalized
    )
    assert (
        "ADD UNIQUE KEY `uk_mes_pp_device_parameter_rule` "
        "(`tenant_id`, `process_id`, `device_id`, `parameter_code`, `deleted`)"
        not in normalized
    )


def test_migration_never_guesses_or_backfills_missing_values() -> None:
    sql = read_migration()

    assert re.search(
        r"\bUPDATE\s+`?mes_pro_process_pool_device_parameter_rule`?",
        sql,
        re.IGNORECASE,
    ) is None
    assert re.search(
        r"\bINSERT\s+INTO\s+`?mes_pro_process_pool_device_parameter_rule`?",
        sql,
        re.IGNORECASE,
    ) is None
    for forbidden in (
        "COALESCE(`route_process_id`",
        "IFNULL(`route_process_id`",
        "COALESCE(`default_value`",
        "IFNULL(`default_value`",
        "ORDER BY `id` LIMIT 1",
    ):
        assert forbidden not in sql
