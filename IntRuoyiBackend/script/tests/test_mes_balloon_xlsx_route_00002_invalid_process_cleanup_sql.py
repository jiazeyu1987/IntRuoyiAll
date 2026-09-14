from pathlib import Path
import re


SQL_PATH = Path("sql/mysql/20260716_mes_balloon_xlsx_route_00002_invalid_process_cleanup.sql")
WORKSTATION_BINDING_SQL_PATH = Path("sql/mysql/20260717_mes_balloon_excel_device_workstation_binding.sql")


def read_sql(path: Path = SQL_PATH) -> str:
    assert path.exists(), f"migration must exist: {path}"
    return path.read_text(encoding="utf-8")


def executable_sql(sql: str) -> str:
    return "\n".join(line for line in sql.splitlines() if not re.match(r"^\s*--", line)).upper()


def test_cleanup_migration_runs_before_balloon_workstation_binding():
    assert SQL_PATH.name < WORKSTATION_BINDING_SQL_PATH.name
    binding_sql = read_sql(WORKSTATION_BINDING_SQL_PATH)
    assert "SET @target_route_process_count = 49" in binding_sql


def test_cleanup_migration_has_test_only_release_contract_and_exact_scope():
    sql = read_sql()

    for token in [
        "release-migration:",
        "allowedEnvironments=test",
        "dependsOn=20260710_mes_route_schedule_config_unification",
        "type=data",
        "riskLevel=medium",
        "SET @target_tenant_id = 1",
        "SET @target_route_hex = '524F5554452D584C53582D3030303032'",
        "SET @target_sort = 26",
        "SET @target_process_code_hex = '42333230'",
        "HEX(route.`code`) = @target_route_hex",
        "HEX(process.`code`) = @target_process_code_hex",
    ]:
        assert token in sql, f"cleanup migration must include token: {token}"


def test_cleanup_migration_backs_up_every_mutated_table():
    sql = read_sql()

    for token in [
        "mes_balloon_xlsx_route_00002_invalid_process_cleanup_20260716",
        "record_type",
        "source_table",
        "source_id",
        "old_next_process_id",
        "old_enabled",
        "old_deleted",
        "route_process",
        "previous_route_process",
        "route_flow_process_config",
        "route_schedule_config",
        "legacy_route_use_process_config",
        "schedule_order_process",
    ]:
        assert token in sql, f"cleanup migration must backup mutation evidence via: {token}"


def test_cleanup_migration_fails_fast_on_unexpected_or_reported_data():
    sql = read_sql()

    for token in [
        "IF v_target_route_process_count <> 1 THEN",
        "IF v_route_00002_active_process_count <> 26 THEN",
        "IF v_target_schedule_reported_count <> 0 THEN",
        "balloon XLSX route 00002 invalid process cleanup target mismatch",
        "balloon XLSX route 00002 invalid process cleanup has reported schedule data",
    ]:
        assert token in sql, f"cleanup migration must fail fast via: {token}"


def test_cleanup_migration_soft_deletes_invalid_route_and_derived_records():
    sql = read_sql()

    for token in [
        "SET previous_route_process.`next_process_id` = NULL",
        "SET target_route_process.`deleted` = b'1'",
        "SET flow_config.`deleted` = b'1'",
        "SET schedule_config.`deleted` = b'1'",
        "SET legacy_config.`deleted` = b'1'",
        "SET schedule_process.`enabled` = b'0'",
        "schedule_process.`deleted` = b'1'",
    ]:
        assert token in sql, f"cleanup migration must soft-delete safely via: {token}"


def test_cleanup_migration_is_non_destructive():
    upper_sql = executable_sql(read_sql())

    for forbidden in [
        "DELETE FROM",
        "TRUNCATE",
        "DROP TABLE",
        "DROP DATABASE",
    ]:
        assert forbidden not in upper_sql
