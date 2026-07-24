from pathlib import Path
import re


SQL_PATH = Path("sql/mysql/20260709_mes_rt000006_batch_record_mapping.sql")


def read_sql() -> str:
    assert SQL_PATH.exists(), "RT000006 batch record mapping migration must exist"
    return SQL_PATH.read_text(encoding="utf-8")


def executable_sql(sql: str) -> str:
    return "\n".join(line for line in sql.splitlines() if not re.match(r"^\s*--", line)).upper()


def test_rt000006_mapping_script_targets_only_confirmed_pressure_pump_route():
    sql = read_sql()

    for token in [
        "release-migration:",
        "ensure_mes_rt000006_batch_record_mapping",
        "922067",
        "RT000006",
        "球囊扩张压力泵",
        "mes_pro_route_use_config",
        "mes_pro_route_use_process_config",
        "mes_pro_route_use_process_batch_record",
        "mes_pro_edhr_process_form_permission_rule",
        "mes_pro_batch_record_report",
    ]:
        assert token in sql, f"migration must include token: {token}"

    assert "E2E-WORD-1783433099306" not in sql


def test_rt000006_mapping_script_is_name_based_and_uses_existing_roles():
    sql = read_sql()

    for token in [
        "process.`name` COLLATE utf8mb4_unicode_ci = report.`report_name` COLLATE utf8mb4_unicode_ci",
        "压力泵生产填写员",
        "压力泵质量填写员",
        "压力泵设备填写员",
        "'FILL'",
        "'QUALITY_FILL'",
        "'EQUIPMENT_FILL'",
        "'ROLE'",
        "`candidate_source_ids`",
    ]:
        assert token in sql, f"migration must map by existing process/report/role data: {token}"


def test_rt000006_mapping_script_fails_fast_for_missing_prerequisites():
    upper_sql = executable_sql(read_sql())

    for token in [
        "SIGNAL SQLSTATE '45000'",
        "MISSING RT000006 PRESSURE PUMP ROUTE",
        "MISSING RT000006 PRESSURE PUMP ROLE",
        "MISSING RT000006 BATCH RECORD REPORT",
        "DUPLICATE RT000006 BATCH RECORD REPORT",
        "RT000006 BATCH RECORD SORT CONFLICT",
        "INCOMPLETE RT000006 BATCH RECORD MAPPING",
    ]:
        assert token in upper_sql, f"migration must fail fast with token: {token}"


def test_rt000006_mapping_script_is_idempotent_and_non_destructive():
    upper_sql = executable_sql(read_sql())

    for token in [
        "LEFT JOIN",
        "IS NULL",
        "WHERE NOT EXISTS",
        "UPDATE `MES_PRO_ROUTE_USE_PROCESS_CONFIG`",
        "UPDATE `MES_PRO_EDHR_PROCESS_FORM_PERMISSION_RULE`",
    ]:
        assert token in upper_sql, f"migration must be idempotent with token: {token}"

    for forbidden in [
        "TRUNCATE",
        "DELETE FROM",
        "DROP COLUMN",
        "DROP TABLE",
    ]:
        assert forbidden not in upper_sql
