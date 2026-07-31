import json
import re
import subprocess
import sys
from datetime import datetime, timezone
from pathlib import Path


ROOT = Path(__file__).resolve().parents[4]
TASK_DIR = ROOT / "doc" / "tasks" / "20260731-mes-three-tab-test-sync"
ARTIFACT_DIR = TASK_DIR / "artifacts"

LOCAL_MYSQL_CMD = [
    "docker",
    "exec",
    "-i",
    "int-ruoyi-mysql",
    "sh",
    "-lc",
    'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql -uroot --default-character-set=utf8mb4 --batch --raw ruoyi-vue-pro',
]

REMOTE_MYSQL_CMD = [
    "ssh",
    "root@172.30.30.58",
    'docker exec -i intruoyi-mysql sh -lc \'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql -uroot --default-character-set=utf8mb4 --batch --raw ruoyi-vue-pro\'',
]

TENANT_ID = 1

WHITELIST_TABLES = {
    "mes_pro_process",
    "mes_pro_process_content",
    "mes_pro_route",
    "mes_pro_route_version",
    "mes_pro_route_process",
    "mes_pro_route_process_flow_edge",
    "mes_pro_route_process_flow_layout",
    "mes_pro_route_process_flow_boundary_edge",
    "mes_pro_route_flow_config",
    "mes_pro_route_flow_process_config",
    "mes_pro_route_flow_process_batch_record",
    "mes_pro_route_schedule_config",
    "mes_pro_route_product",
    "mes_pro_route_product_bom",
    "mes_pro_edhr_work_task_assignment_rule",
    "mes_pro_schedule_order",
    "mes_pro_schedule_order_process",
    "mes_pro_schedule_order_diff",
    "mes_pro_schedule_order_daily_compare",
    "mes_pro_schedule_order_operation_log",
}


def fail(message):
    print(message, file=sys.stderr)
    raise SystemExit(1)


def run_mysql(cmd, sql, label):
    proc = subprocess.run(
        cmd,
        input=sql,
        text=True,
        encoding="utf-8",
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
        timeout=120,
    )
    stderr = proc.stderr.strip()
    if proc.returncode != 0:
        fail(f"{label} mysql failed: exit={proc.returncode}\n{stderr}")
    # OpenSSH on Windows can emit this harmless close warning after stdout is complete.
    warnings = []
    if stderr:
        warnings.append(stderr)
    return proc.stdout, warnings


def rows(cmd, sql, label):
    stdout, warnings = run_mysql(cmd, sql, label)
    lines = stdout.splitlines()
    if not lines:
        return [], warnings
    headers = lines[0].split("\t")
    result = []
    for line in lines[1:]:
        values = line.split("\t")
        row = {}
        for idx, header in enumerate(headers):
            row[header] = values[idx] if idx < len(values) else ""
        result.append(row)
    return result, warnings


def scalar_list(cmd, sql, label):
    result, warnings = rows(cmd, sql, label)
    values = []
    for row in result:
        value = next(iter(row.values()))
        if value not in ("", "NULL", None):
            values.append(int(value))
    return values, warnings


def sql_in(values):
    values = sorted({str(v) for v in values if v is not None and str(v) != ""})
    if not values:
        return "(NULL)"
    encoded = []
    for value in values:
        if re.fullmatch(r"-?\d+", value):
            encoded.append(value)
        else:
            encoded.append("'" + value.replace("'", "''") + "'")
    return "(" + ",".join(encoded) + ")"


def active_where(alias=""):
    prefix = f"{alias}." if alias else ""
    return f"{prefix}tenant_id = {TENANT_ID} AND {prefix}deleted = 0"


def source_scope_sql():
    return f"""
WITH
source_routes AS (
  SELECT id FROM mes_pro_route WHERE {active_where()}
),
source_route_versions AS (
  SELECT id FROM mes_pro_route_version WHERE {active_where()} AND route_id IN (SELECT id FROM source_routes)
),
source_route_processes AS (
  SELECT id FROM mes_pro_route_process WHERE {active_where()} AND route_id IN (SELECT id FROM source_routes)
),
source_flow_configs AS (
  SELECT id FROM mes_pro_route_flow_config WHERE {active_where()} AND route_id IN (SELECT id FROM source_routes)
),
source_flow_process_configs AS (
  SELECT id FROM mes_pro_route_flow_process_config
   WHERE {active_where()} AND (route_flow_config_id IN (SELECT id FROM source_flow_configs)
      OR route_id IN (SELECT id FROM source_routes)
      OR route_process_id IN (SELECT id FROM source_route_processes))
),
source_schedule_orders AS (
  SELECT id FROM mes_pro_schedule_order WHERE {active_where()}
),
source_schedule_order_processes AS (
  SELECT id FROM mes_pro_schedule_order_process WHERE {active_where()} AND schedule_order_id IN (SELECT id FROM source_schedule_orders)
)
"""


def whitelist_count_sql():
    scope = source_scope_sql()
    selects = [
        ("mes_pro_process", "SELECT COUNT(*) FROM mes_pro_process WHERE tenant_id=1 AND deleted=0"),
        ("mes_pro_process_content", "SELECT COUNT(*) FROM mes_pro_process_content WHERE tenant_id=1 AND deleted=0 AND process_id IN (SELECT id FROM mes_pro_process WHERE tenant_id=1 AND deleted=0)"),
        ("mes_pro_route", "SELECT COUNT(*) FROM source_routes"),
        ("mes_pro_route_version", "SELECT COUNT(*) FROM source_route_versions"),
        ("mes_pro_route_process", "SELECT COUNT(*) FROM source_route_processes"),
        ("mes_pro_route_process_flow_edge", "SELECT COUNT(*) FROM mes_pro_route_process_flow_edge WHERE tenant_id=1 AND deleted=0 AND route_id IN (SELECT id FROM source_routes)"),
        ("mes_pro_route_process_flow_layout", "SELECT COUNT(*) FROM mes_pro_route_process_flow_layout WHERE tenant_id=1 AND deleted=0 AND route_id IN (SELECT id FROM source_routes)"),
        ("mes_pro_route_process_flow_boundary_edge", "SELECT COUNT(*) FROM mes_pro_route_process_flow_boundary_edge WHERE tenant_id=1 AND deleted=0 AND route_id IN (SELECT id FROM source_routes)"),
        ("mes_pro_route_flow_config", "SELECT COUNT(*) FROM source_flow_configs"),
        ("mes_pro_route_flow_process_config", "SELECT COUNT(*) FROM source_flow_process_configs"),
        ("mes_pro_route_flow_process_batch_record", "SELECT COUNT(*) FROM mes_pro_route_flow_process_batch_record WHERE tenant_id=1 AND deleted=0 AND (route_flow_process_config_id IN (SELECT id FROM source_flow_process_configs) OR route_id IN (SELECT id FROM source_routes) OR route_process_id IN (SELECT id FROM source_route_processes))"),
        ("mes_pro_route_schedule_config", "SELECT COUNT(*) FROM mes_pro_route_schedule_config WHERE tenant_id=1 AND deleted=0 AND (route_version_id IN (SELECT id FROM source_route_versions) OR route_process_id IN (SELECT id FROM source_route_processes))"),
        ("mes_pro_route_product", "SELECT COUNT(*) FROM mes_pro_route_product WHERE tenant_id=1 AND deleted=0 AND route_id IN (SELECT id FROM source_routes)"),
        ("mes_pro_route_product_bom", "SELECT COUNT(*) FROM mes_pro_route_product_bom WHERE tenant_id=1 AND deleted=0 AND route_id IN (SELECT id FROM source_routes)"),
        ("mes_pro_edhr_work_task_assignment_rule", "SELECT COUNT(*) FROM mes_pro_edhr_work_task_assignment_rule WHERE tenant_id=1 AND deleted=0 AND scope_type='ROUTE' AND task_type='RELEASE_APPROVE' AND scope_id IN (SELECT id FROM source_routes)"),
        ("mes_pro_schedule_order", "SELECT COUNT(*) FROM source_schedule_orders"),
        ("mes_pro_schedule_order_process", "SELECT COUNT(*) FROM source_schedule_order_processes"),
        ("mes_pro_schedule_order_diff", "SELECT COUNT(*) FROM mes_pro_schedule_order_diff WHERE tenant_id=1 AND deleted=0 AND schedule_order_id IN (SELECT id FROM source_schedule_orders)"),
        ("mes_pro_schedule_order_daily_compare", "SELECT COUNT(*) FROM mes_pro_schedule_order_daily_compare WHERE tenant_id=1 AND deleted=0 AND schedule_order_id IN (SELECT id FROM source_schedule_orders)"),
        ("mes_pro_schedule_order_operation_log", "SELECT COUNT(*) FROM mes_pro_schedule_order_operation_log WHERE tenant_id=1 AND deleted=0 AND schedule_order_id IN (SELECT id FROM source_schedule_orders)"),
    ]
    union = "\nUNION ALL\n".join(
        f"SELECT '{name}' AS table_name, ({query}) AS row_count" for name, query in selects
    )
    return scope + union + "\nORDER BY table_name"


def simple_active_count_sql():
    tables = sorted(WHITELIST_TABLES)
    return "\nUNION ALL\n".join(
        f"SELECT '{table}' AS table_name, COUNT(*) AS row_count FROM {table} WHERE tenant_id={TENANT_ID} AND deleted=0"
        for table in tables
    ) + "\nORDER BY table_name"


def source_dependency_sql():
    return source_scope_sql() + """
SELECT 'batch_record_report_id' AS dep_type, CAST(dep_id AS CHAR) AS dep_id
FROM (
  SELECT batch_record_report_id AS dep_id FROM mes_pro_route_process WHERE tenant_id=1 AND deleted=0 AND route_id IN (SELECT id FROM source_routes)
  UNION SELECT batch_record_report_id FROM mes_pro_route_flow_process_config WHERE tenant_id=1 AND deleted=0 AND id IN (SELECT id FROM source_flow_process_configs)
  UNION SELECT batch_record_report_id FROM mes_pro_route_flow_process_batch_record WHERE tenant_id=1 AND deleted=0 AND (route_flow_process_config_id IN (SELECT id FROM source_flow_process_configs) OR route_id IN (SELECT id FROM source_routes) OR route_process_id IN (SELECT id FROM source_route_processes))
) d WHERE dep_id IS NOT NULL AND dep_id <> 0
UNION ALL
SELECT 'batch_record_definition_id', CAST(dep_id AS CHAR)
FROM (
  SELECT batch_record_definition_id AS dep_id FROM mes_pro_route_flow_process_batch_record WHERE tenant_id=1 AND deleted=0 AND (route_flow_process_config_id IN (SELECT id FROM source_flow_process_configs) OR route_id IN (SELECT id FROM source_routes) OR route_process_id IN (SELECT id FROM source_route_processes))
) d WHERE dep_id IS NOT NULL AND dep_id <> 0
UNION ALL
SELECT 'batch_record_version_id', CAST(dep_id AS CHAR)
FROM (
  SELECT batch_record_version_id AS dep_id FROM mes_pro_route_flow_process_batch_record WHERE tenant_id=1 AND deleted=0 AND (route_flow_process_config_id IN (SELECT id FROM source_flow_process_configs) OR route_id IN (SELECT id FROM source_routes) OR route_process_id IN (SELECT id FROM source_route_processes))
) d WHERE dep_id IS NOT NULL AND dep_id <> 0
UNION ALL
SELECT 'form_definition_id', CAST(dep_id AS CHAR)
FROM (
  SELECT form_definition_id AS dep_id FROM mes_pro_route_flow_process_batch_record WHERE tenant_id=1 AND deleted=0 AND (route_flow_process_config_id IN (SELECT id FROM source_flow_process_configs) OR route_id IN (SELECT id FROM source_routes) OR route_process_id IN (SELECT id FROM source_route_processes))
) d WHERE dep_id IS NOT NULL AND dep_id <> 0
UNION ALL
SELECT 'form_template_version_id', CAST(dep_id AS CHAR)
FROM (
  SELECT last_published_template_version_id AS dep_id FROM mes_pro_route_flow_process_batch_record WHERE tenant_id=1 AND deleted=0 AND (route_flow_process_config_id IN (SELECT id FROM source_flow_process_configs) OR route_id IN (SELECT id FROM source_routes) OR route_process_id IN (SELECT id FROM source_route_processes))
) d WHERE dep_id IS NOT NULL AND dep_id <> 0
UNION ALL
SELECT 'permission_scope_id', CAST(dep_id AS CHAR)
FROM (
  SELECT permission_scope_id AS dep_id FROM mes_pro_route_flow_process_batch_record WHERE tenant_id=1 AND deleted=0 AND (route_flow_process_config_id IN (SELECT id FROM source_flow_process_configs) OR route_id IN (SELECT id FROM source_routes) OR route_process_id IN (SELECT id FROM source_route_processes))
) d WHERE dep_id IS NOT NULL AND dep_id <> 0
UNION ALL
SELECT 'item_id', CAST(dep_id AS CHAR)
FROM (
  SELECT item_id AS dep_id FROM mes_pro_route_product WHERE tenant_id=1 AND deleted=0 AND route_id IN (SELECT id FROM source_routes)
  UNION SELECT product_id FROM mes_pro_route_product_bom WHERE tenant_id=1 AND deleted=0 AND route_id IN (SELECT id FROM source_routes)
  UNION SELECT item_id FROM mes_pro_route_product_bom WHERE tenant_id=1 AND deleted=0 AND route_id IN (SELECT id FROM source_routes)
  UNION SELECT product_id FROM mes_pro_schedule_order WHERE tenant_id=1 AND deleted=0
) d WHERE dep_id IS NOT NULL AND dep_id <> 0
UNION ALL
SELECT 'work_order_id', CAST(dep_id AS CHAR)
FROM (
  SELECT work_order_id AS dep_id FROM mes_pro_schedule_order WHERE tenant_id=1 AND deleted=0
  UNION SELECT source_work_order_id FROM mes_pro_schedule_order WHERE tenant_id=1 AND deleted=0
  UNION SELECT source_work_order_id FROM mes_pro_schedule_order_process WHERE tenant_id=1 AND deleted=0 AND schedule_order_id IN (SELECT id FROM source_schedule_orders)
  UNION SELECT work_order_id FROM mes_pro_schedule_order_diff WHERE tenant_id=1 AND deleted=0 AND schedule_order_id IN (SELECT id FROM source_schedule_orders)
) d WHERE dep_id IS NOT NULL AND dep_id <> 0
UNION ALL
SELECT 'calendar_rule_id', CAST(dep_id AS CHAR)
FROM (
  SELECT calendar_rule_id AS dep_id FROM mes_pro_route_schedule_config WHERE tenant_id=1 AND deleted=0 AND (route_version_id IN (SELECT id FROM source_route_versions) OR route_process_id IN (SELECT id FROM source_route_processes))
  UNION SELECT calendar_rule_id FROM mes_pro_schedule_order_process WHERE tenant_id=1 AND deleted=0 AND schedule_order_id IN (SELECT id FROM source_schedule_orders)
) d WHERE dep_id IS NOT NULL AND dep_id <> 0
UNION ALL
SELECT 'workstation_id', CAST(dep_id AS CHAR)
FROM (
  SELECT workstation_id AS dep_id FROM mes_pro_route_process WHERE tenant_id=1 AND deleted=0 AND route_id IN (SELECT id FROM source_routes)
  UNION SELECT workstation_id FROM mes_pro_schedule_order_process WHERE tenant_id=1 AND deleted=0 AND schedule_order_id IN (SELECT id FROM source_schedule_orders)
) d WHERE dep_id IS NOT NULL AND dep_id <> 0
UNION ALL
SELECT 'user_id', CAST(dep_id AS CHAR)
FROM (
  SELECT assignee_user_id AS dep_id FROM mes_pro_edhr_work_task_assignment_rule WHERE tenant_id=1 AND deleted=0 AND scope_type='ROUTE' AND task_type='RELEASE_APPROVE' AND scope_id IN (SELECT id FROM source_routes)
  UNION SELECT review_user_id FROM mes_pro_edhr_work_task_assignment_rule WHERE tenant_id=1 AND deleted=0 AND scope_type='ROUTE' AND task_type='RELEASE_APPROVE' AND scope_id IN (SELECT id FROM source_routes)
  UNION SELECT submitted_by FROM mes_pro_route_version WHERE tenant_id=1 AND deleted=0 AND route_id IN (SELECT id FROM source_routes)
  UNION SELECT published_by FROM mes_pro_route_version WHERE tenant_id=1 AND deleted=0 AND route_id IN (SELECT id FROM source_routes)
  UNION SELECT operator_id FROM mes_pro_schedule_order_operation_log WHERE tenant_id=1 AND deleted=0 AND schedule_order_id IN (SELECT id FROM source_schedule_orders)
) d WHERE dep_id IS NOT NULL AND dep_id <> 0
ORDER BY dep_type, dep_id
"""


def schema_sql():
    return """
SELECT TABLE_NAME, COLUMN_NAME, DATA_TYPE, COLUMN_TYPE, CHARACTER_MAXIMUM_LENGTH, IS_NULLABLE
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA=DATABASE()
  AND (
    (TABLE_NAME='mes_pro_route_version' AND COLUMN_NAME='route_snapshot_json')
    OR (TABLE_NAME='mes_pro_schedule_order' AND COLUMN_NAME='promise_date')
    OR (TABLE_NAME='mes_pro_batch_record_report' AND COLUMN_NAME IN ('form_definition_id','form_version_id','batch_record_definition_id','batch_record_version_id'))
  )
ORDER BY TABLE_NAME, COLUMN_NAME
"""


def snapshot_size_sql():
    return source_scope_sql() + """
SELECT
  COALESCE(MAX(OCTET_LENGTH(route_snapshot_json)), 0) AS max_route_snapshot_bytes,
  SUM(CASE WHEN OCTET_LENGTH(route_snapshot_json) > 65535 THEN 1 ELSE 0 END) AS route_snapshots_over_text_limit
FROM mes_pro_route_version
WHERE tenant_id=1 AND deleted=0 AND id IN (SELECT id FROM source_route_versions)
"""


def ids_sql(table, where):
    return f"SELECT id FROM {table} WHERE {where} ORDER BY id"


def key_rows_sql(table, ids, columns):
    if not ids:
        return f"SELECT {', '.join(columns)} FROM {table} WHERE 1=0"
    return f"SELECT {', '.join(columns)} FROM {table} WHERE tenant_id={TENANT_ID} AND id IN {sql_in(ids)} ORDER BY id"


def table_map(cmd, table, ids, columns, label):
    result, warnings = rows(cmd, key_rows_sql(table, ids, columns), label)
    return {str(row["id"]): row for row in result}, warnings


def collect_ids(cmd, label):
    specs = {
        "process_ids": ("mes_pro_process", "tenant_id=1 AND deleted=0"),
        "route_ids": ("mes_pro_route", "tenant_id=1 AND deleted=0"),
        "route_version_ids": ("mes_pro_route_version", "tenant_id=1 AND deleted=0 AND route_id IN (SELECT id FROM mes_pro_route WHERE tenant_id=1 AND deleted=0)"),
        "route_process_ids": ("mes_pro_route_process", "tenant_id=1 AND deleted=0 AND route_id IN (SELECT id FROM mes_pro_route WHERE tenant_id=1 AND deleted=0)"),
        "schedule_order_ids": ("mes_pro_schedule_order", "tenant_id=1 AND deleted=0"),
        "schedule_order_process_ids": ("mes_pro_schedule_order_process", "tenant_id=1 AND deleted=0 AND schedule_order_id IN (SELECT id FROM mes_pro_schedule_order WHERE tenant_id=1 AND deleted=0)"),
    }
    output = {}
    warnings = []
    for key, (table, where) in specs.items():
        values, warn = scalar_list(cmd, ids_sql(table, where), f"{label}:{key}")
        warnings.extend(warn)
        output[key] = values
    return output, warnings


def dependency_ids(dep_rows):
    grouped = {}
    for row in dep_rows:
        dep_type = row["dep_type"]
        dep_id = row["dep_id"]
        if dep_id and dep_id != "NULL":
            grouped.setdefault(dep_type, set()).add(str(dep_id))
    return {k: sorted(v) for k, v in grouped.items()}


def compare_maps(source_map, target_map, columns):
    missing = []
    mismatched = []
    for key, source_row in source_map.items():
        target_row = target_map.get(key)
        if not target_row:
            missing.append(key)
            continue
        diffs = {}
        for column in columns:
            if str(source_row.get(column, "")) != str(target_row.get(column, "")):
                diffs[column] = {
                    "source": source_row.get(column),
                    "target": target_row.get(column),
                }
        if diffs:
            mismatched.append({"id": key, "diffs": diffs})
    return missing, mismatched


def check_dependency_table(report, dep_type, table, ids, columns):
    source_map, warn = table_map(LOCAL_MYSQL_CMD, table, ids, columns, f"local:{table}")
    target_map, warn2 = table_map(REMOTE_MYSQL_CMD, table, ids, columns, f"remote:{table}")
    report["warnings"].extend(warn + warn2)
    missing, mismatched = compare_maps(source_map, target_map, [c for c in columns if c != "id"])
    return {
        "dep_type": dep_type,
        "table": table,
        "required_count": len(ids),
        "missing_ids": missing,
        "mismatched": mismatched[:50],
        "mismatch_count": len(mismatched),
    }


def external_ref_sql(remote_ids):
    schema_query = """
SELECT TABLE_NAME, COLUMN_NAME
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA=DATABASE()
  AND COLUMN_NAME IN ('tenant_id','deleted','process_id','route_id','route_process_id','schedule_order_id','schedule_order_process_id')
ORDER BY TABLE_NAME, COLUMN_NAME
"""
    schema_rows, warnings = rows(REMOTE_MYSQL_CMD, schema_query, "remote:ref-schema")
    table_cols = {}
    for row in schema_rows:
        table_cols.setdefault(row["TABLE_NAME"], set()).add(row["COLUMN_NAME"])
    ref_specs = [
        ("process_id", remote_ids["process_ids"]),
        ("route_id", remote_ids["route_ids"]),
        ("route_process_id", remote_ids["route_process_ids"]),
        ("schedule_order_id", remote_ids["schedule_order_ids"]),
        ("schedule_order_process_id", remote_ids["schedule_order_process_ids"]),
    ]
    statements = []
    for table, cols in sorted(table_cols.items()):
        if table in WHITELIST_TABLES or "tenant_id" not in cols:
            continue
        for column, values in ref_specs:
            if column not in cols or not values:
                continue
            deleted_filter = " AND deleted=0" if "deleted" in cols else ""
            statements.append(
                f"SELECT '{table}' AS table_name, '{column}' AS column_name, COUNT(*) AS active_ref_count "
                f"FROM {table} WHERE tenant_id={TENANT_ID}{deleted_filter} AND {column} IN {sql_in(values)}"
            )
    if not statements:
        return [], warnings
    result, warn2 = rows(REMOTE_MYSQL_CMD, "\nUNION ALL\n".join(statements), "remote:external-refs")
    warnings.extend(warn2)
    return [
        {
            "table": row["table_name"],
            "column": row["column_name"],
            "active_ref_count": int(row["active_ref_count"]),
        }
        for row in result
        if int(row["active_ref_count"]) > 0
    ], warnings


def main():
    ARTIFACT_DIR.mkdir(parents=True, exist_ok=True)
    report = {
        "generated_at": datetime.now(timezone.utc).isoformat(),
        "source": "local docker int-ruoyi-mysql / ruoyi-vue-pro / tenant_id=1",
        "target": "ssh root@172.30.30.58 / intruoyi-mysql / ruoyi-vue-pro / tenant_id=1",
        "warnings": [],
        "blockers": [],
    }

    for label, cmd in [("local", LOCAL_MYSQL_CMD), ("remote", REMOTE_MYSQL_CMD)]:
        schema_rows, warnings = rows(cmd, schema_sql(), f"{label}:schema")
        report["warnings"].extend(warnings)
        report.setdefault("schema", {})[label] = schema_rows

    local_counts, warnings = rows(LOCAL_MYSQL_CMD, whitelist_count_sql(), "local:source-counts")
    report["warnings"].extend(warnings)
    remote_counts, warnings = rows(REMOTE_MYSQL_CMD, simple_active_count_sql(), "remote:current-counts")
    report["warnings"].extend(warnings)
    report["source_whitelist_counts"] = {
        row["table_name"]: int(row["row_count"]) for row in local_counts
    }
    report["target_current_whitelist_counts"] = {
        row["table_name"]: int(row["row_count"]) for row in remote_counts
    }

    snapshot_rows, warnings = rows(LOCAL_MYSQL_CMD, snapshot_size_sql(), "local:snapshot-size")
    report["warnings"].extend(warnings)
    report["source_snapshot_size"] = snapshot_rows[0] if snapshot_rows else {}

    dep_rows, warnings = rows(LOCAL_MYSQL_CMD, source_dependency_sql(), "local:dependencies")
    report["warnings"].extend(warnings)
    dep_ids = dependency_ids(dep_rows)
    report["source_dependency_ids"] = dep_ids

    local_ids, warnings = collect_ids(LOCAL_MYSQL_CMD, "local")
    report["warnings"].extend(warnings)
    remote_ids, warnings = collect_ids(REMOTE_MYSQL_CMD, "remote")
    report["warnings"].extend(warnings)
    report["source_id_counts"] = {k: len(v) for k, v in local_ids.items()}
    report["target_current_id_counts"] = {k: len(v) for k, v in remote_ids.items()}

    dependency_checks = []
    dependency_checks.append(check_dependency_table(
        report,
        "batch_record_report_id",
        "mes_pro_batch_record_report",
        dep_ids.get("batch_record_report_id", []),
        ["id", "batch_record_name", "report_code", "batch_record_definition_id", "batch_record_version_id", "deleted", "tenant_id"],
    ))
    dependency_checks.append(check_dependency_table(
        report,
        "batch_record_definition_id",
        "mes_pro_batch_record_definition",
        dep_ids.get("batch_record_definition_id", []),
        ["id", "batch_record_name", "route_key", "current_version_id", "deleted", "tenant_id"],
    ))
    dependency_checks.append(check_dependency_table(
        report,
        "batch_record_version_id",
        "mes_pro_batch_record_version",
        dep_ids.get("batch_record_version_id", []),
        ["id", "definition_id", "version_no", "status", "deleted", "tenant_id"],
    ))
    dependency_checks.append(check_dependency_table(
        report,
        "form_template_version_id",
        "bpm_form_template_version",
        dep_ids.get("form_template_version_id", []),
        ["id", "template_id", "version_no", "status", "deleted", "tenant_id"],
    ))
    dependency_checks.append(check_dependency_table(
        report,
        "permission_scope_id",
        "mes_pro_edhr_permission_scope",
        dep_ids.get("permission_scope_id", []),
        ["id", "scope_name", "object_type", "object_id", "status", "deleted", "tenant_id"],
    ))
    dependency_checks.append(check_dependency_table(
        report,
        "item_id",
        "mes_md_item",
        dep_ids.get("item_id", []),
        ["id", "code", "name", "specification", "status", "deleted", "tenant_id"],
    ))
    dependency_checks.append(check_dependency_table(
        report,
        "work_order_id",
        "mes_pro_work_order",
        dep_ids.get("work_order_id", []),
        ["id", "code", "product_id", "quantity", "batch_code", "status", "deleted", "tenant_id"],
    ))
    dependency_checks.append(check_dependency_table(
        report,
        "calendar_rule_id",
        "mes_pro_schedule_calendar_rule",
        dep_ids.get("calendar_rule_id", []),
        ["id", "skip_statutory_holidays", "weekend_rest_mode", "date_shift_mode_by_date_json", "temporary_freeze_enabled", "deleted", "tenant_id"],
    ))
    dependency_checks.append(check_dependency_table(
        report,
        "workstation_id",
        "mes_md_workstation",
        dep_ids.get("workstation_id", []),
        ["id", "code", "name", "process_id", "status", "deleted", "tenant_id"],
    ))
    dependency_checks.append(check_dependency_table(
        report,
        "user_id",
        "system_users",
        dep_ids.get("user_id", []),
        ["id", "username", "nickname", "status", "deleted", "tenant_id"],
    ))
    report["dependency_checks"] = dependency_checks

    external_refs, warnings = external_ref_sql(remote_ids)
    report["warnings"].extend(warnings)
    report["target_external_active_refs"] = external_refs

    remote_schema = {
        (row["TABLE_NAME"], row["COLUMN_NAME"]): row
        for row in report["schema"]["remote"]
    }
    route_snapshot = remote_schema.get(("mes_pro_route_version", "route_snapshot_json"), {})
    if route_snapshot.get("DATA_TYPE") != "mediumtext":
        report["blockers"].append({
            "type": "schema",
            "message": "target mes_pro_route_version.route_snapshot_json is not MEDIUMTEXT",
            "current": route_snapshot,
        })
    promise_date = remote_schema.get(("mes_pro_schedule_order", "promise_date"), {})
    if promise_date.get("IS_NULLABLE") != "YES":
        report["blockers"].append({
            "type": "schema",
            "message": "target mes_pro_schedule_order.promise_date is not nullable",
            "current": promise_date,
        })
    for required_col in ["form_definition_id", "form_version_id"]:
        if ("mes_pro_batch_record_report", required_col) not in remote_schema:
            report["blockers"].append({
                "type": "schema",
                "message": f"target mes_pro_batch_record_report.{required_col} is missing",
            })

    size = report["source_snapshot_size"]
    over_limit = int(size.get("route_snapshots_over_text_limit") or 0)
    if over_limit:
        report["blockers"].append({
            "type": "schema",
            "message": "source route snapshots exceed target TEXT capacity",
            "over_text_limit_rows": over_limit,
            "max_route_snapshot_bytes": int(size.get("max_route_snapshot_bytes") or 0),
        })

    for check in dependency_checks:
        if check["missing_ids"] or check["mismatch_count"]:
            report["blockers"].append({
                "type": "dependency",
                "message": f"target dependency check failed for {check['dep_type']}",
                "table": check["table"],
                "required_count": check["required_count"],
                "missing_count": len(check["missing_ids"]),
                "mismatch_count": check["mismatch_count"],
                "missing_ids": check["missing_ids"][:50],
                "mismatched": check["mismatched"][:10],
            })

    if external_refs:
        report["blockers"].append({
            "type": "external_reference",
            "message": "target has active non-whitelist references to rows that would be replaced",
            "reference_count": len(external_refs),
            "top_references": external_refs[:50],
        })

    report_path = ARTIFACT_DIR / "preflight-report.json"
    report_path.write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding="utf-8")

    summary_lines = [
        "# Three Tab Sync Preflight Summary",
        "",
        f"- Generated: `{report['generated_at']}`",
        f"- Source whitelist rows: `{sum(report['source_whitelist_counts'].values())}`",
        f"- Target current whitelist rows: `{sum(report['target_current_whitelist_counts'].values())}`",
        f"- Blockers: `{len(report['blockers'])}`",
        "",
        "## Blockers",
    ]
    for blocker in report["blockers"]:
        summary_lines.append(f"- `{blocker['type']}`: {blocker['message']}")
    if not report["blockers"]:
        summary_lines.append("- None")
    summary_path = ARTIFACT_DIR / "preflight-summary.md"
    summary_path.write_text("\n".join(summary_lines) + "\n", encoding="utf-8")

    print(json.dumps({
        "report": str(report_path),
        "summary": str(summary_path),
        "blocker_count": len(report["blockers"]),
        "source_whitelist_total": sum(report["source_whitelist_counts"].values()),
        "target_whitelist_total": sum(report["target_current_whitelist_counts"].values()),
    }, ensure_ascii=False, indent=2))
    return 2 if report["blockers"] else 0


if __name__ == "__main__":
    raise SystemExit(main())
