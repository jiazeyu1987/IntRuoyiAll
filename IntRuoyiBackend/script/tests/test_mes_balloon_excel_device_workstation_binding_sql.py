from pathlib import Path
import re


SQL_PATH = Path("sql/mysql/20260717_mes_balloon_excel_device_workstation_binding.sql")
PREFLIGHT_PATH = Path("sql/mysql/target-preflight/20260717_mes_balloon_excel_device_workstation_binding.preflight.sql")


def read_sql() -> str:
    assert SQL_PATH.exists(), "Excel device-to-workstation binding migration must exist"
    return SQL_PATH.read_text(encoding="utf-8")


def executable_sql(sql: str) -> str:
    return "\n".join(line for line in sql.splitlines() if not re.match(r"^\s*--", line)).upper()


def test_balloon_excel_workstation_binding_has_release_contract_and_scope():
    sql = read_sql()

    for token in [
        "release-migration:",
        "allowedEnvironments=test,backup,prod",
        "dependsOn=20260708_mes_balloon_process_device_capacity",
        "type=data",
        "riskLevel=medium",
        "SET @target_tenant_id = 1",
        "ROUTE-XLSX-00001",
        "ROUTE-XLSX-00002",
        "SET @target_route_process_count = 49",
        "SET @minimum_workstation_machine_count = 1",
    ]:
        assert token in sql, f"migration must include token: {token}"

    assert "tenant_id = 122" not in sql
    assert "SET @target_workstation_machine_count = 83" not in sql


def test_balloon_excel_workstation_binding_creates_reusable_current_process_workstations():
    sql = read_sql()

    for token in [
        "tmp_balloon_excel_workstation_seed",
        "CONCAT(REPLACE(route.`code`, 'ROUTE-', 'WS-'), '-', LPAD(route_process.`sort`, 2, '0'))",
        "mes_md_workstation",
        "workstation.`process_id` = seed.`process_id`",
        "workstation.`code` = seed.`workstation_code`",
        "workstation process conflict",
        "shift_hours",
        "10.50",
    ]:
        assert token in sql, f"migration must create/reuse current process workstation via: {token}"


def test_balloon_excel_workstation_binding_links_excel_devices_as_station_machines():
    sql = read_sql()

    for token in [
        "tmp_balloon_excel_workstation_machine_seed",
        "mes_dv_machinery_process",
        "mp.`line_name` = seed.`product_name`",
        "mp.`process_id` = seed.`process_id`",
        "mes_md_workstation_machine",
        "pre_existing_binding_id` IS NULL",
        "workstation machine quantity conflict",
    ]:
        assert token in sql, f"migration must bind Excel equipment resources via: {token}"


def test_balloon_excel_workstation_binding_uses_target_derived_machine_count():
    sql = read_sql()

    assert "v_target_workstation_machine_count < @minimum_workstation_machine_count" in sql
    assert "balloon Excel workstation machine count mismatch" not in sql
    assert "COUNT(*)\n      INTO v_target_workstation_machine_count" in sql


def test_balloon_excel_target_preflight_checks_machine_seed_before_deploy():
    assert PREFLIGHT_PATH.exists(), "target preflight must exist for workstation machine binding"
    preflight = PREFLIGHT_PATH.read_text(encoding="utf-8")

    for token in [
        "TARGET_PREFLIGHT_PASS:20260717_mes_balloon_excel_device_workstation_binding",
        "ROUTE-XLSX-00001",
        "ROUTE-XLSX-00002",
        "mes_dv_machinery_process",
        "mes_dv_machinery",
        "COUNT(DISTINCT CONCAT(route_process.`id`, ':', mp.`machinery_id`))",
        "> 0",
    ]:
        assert token in preflight, f"target preflight must validate machine seed via: {token}"


def test_balloon_excel_workstation_binding_binds_all_target_route_processes_and_backs_up():
    sql = read_sql()

    for token in [
        "mes_balloon_excel_workstation_binding_20260717",
        "mes_balloon_excel_workstation_created_20260717",
        "mes_balloon_excel_workstation_machine_created_20260717",
        "UPDATE `mes_pro_route_process` route_process",
        "SET route_process.`workstation_id` = seed.`workstation_id`",
        "target route process workstation conflict",
        "final_missing_workstation_count",
        "final_bound_route_process_count",
    ]:
        assert token in sql, f"migration must backup and bind route processes via: {token}"


def test_balloon_excel_workstation_binding_is_non_destructive_and_not_historical():
    upper_sql = executable_sql(read_sql())

    for forbidden in [
        "TRUNCATE",
        "DROP TABLE",
        "DELETE FROM MES_MD_WORKSTATION",
        "DELETE FROM MES_MD_WORKSTATION_MACHINE",
        "DELETE FROM MES_PRO_ROUTE_PROCESS",
        "OLD_PROCESS",
        "SELECTLISTBYCODESIGNOREDELETED",
    ]:
        assert forbidden not in upper_sql
