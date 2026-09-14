import re
from pathlib import Path


BACKEND_ROOT = Path(__file__).resolve().parents[2]
MIGRATION_PATH = (
    BACKEND_ROOT
    / "sql"
    / "mysql"
    / "20260830_mes_process_pool_idi_device_parameter_rules.sql"
)


def read_migration() -> str:
    assert MIGRATION_PATH.exists(), f"missing migration: {MIGRATION_PATH}"
    return MIGRATION_PATH.read_text(encoding="utf-8")


def test_migration_declares_release_recovery_and_dependency_contract() -> None:
    sql = read_migration()

    assert sql.startswith(
        "-- release-migration: allowedEnvironments=test,backup,prod; "
        "dependsOn=20260811_mes_process_pool_cleaning_process_parameter_data; "
        "type=data; riskLevel=medium\n"
    )
    assert "-- Recovery:" in sql
    assert "-- Rollback:" in sql


def test_migration_locks_idi_project_by_business_identity() -> None:
    sql = read_migration()
    normalized = " ".join(sql.split())

    assert "project.`project_code` = 'IDI'" in normalized
    assert "project.`project_name` = '按压式球囊扩充压力泵'" in normalized
    assert "target_route.`code` = 'RT000028-IDI'" in normalized
    assert "source_route.`code` = 'RT000028'" in normalized
    assert re.search(r"dcc_project_code_id`?\s*=\s*129", normalized, re.IGNORECASE) is None


def test_migration_fails_fast_when_source_or_target_is_not_unique() -> None:
    sql = read_migration()
    normalized = " ".join(sql.split())

    for expected in (
        "Expected one active IDI DCC project code",
        "Expected one active IDI target route binding",
        "Expected one active pressure-pump source route",
        "IDI target device binding has no source parameter rules",
        "IDI source parameter rules are not unique",
    ):
        assert expected in sql
    assert normalized.count("SIGNAL SQLSTATE '45000'") >= 5


def test_migration_copies_source_rules_to_the_current_target_route_process() -> None:
    sql = read_migration()
    normalized = " ".join(sql.split())

    assert re.search(
        r"INSERT\s+INTO\s+`mes_pro_process_pool_device_parameter_rule`",
        normalized,
        re.IGNORECASE,
    )
    assert "target_route_process.`id`" in normalized
    assert "target_route_process.`process_id`" in normalized
    assert "target_device.`id`" in normalized
    assert "source_route_process.`process_id` = target_route_process.`process_id`" in normalized
    assert "source_rule.`leader_user_id` = target_binding.`leader_user_id`" in normalized
    assert "source_rule.`device_id` = target_device.`id`" in normalized
    assert "existing_rule.`route_process_id` = target_route_process.`id`" in normalized
    assert "existing_rule.`parameter_code` = source_rule.`parameter_code`" in normalized
