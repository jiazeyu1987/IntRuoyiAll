import hashlib
import importlib.util
import json
import subprocess
import sys
from datetime import datetime, timezone
from pathlib import Path


ROOT = Path(__file__).resolve().parents[4]
TASK_DIR = ROOT / "doc" / "tasks" / "20260731-mes-three-tab-test-sync"
ARTIFACT_DIR = TASK_DIR / "artifacts"
PREFLIGHT_SCRIPT = TASK_DIR / "tools" / "three_tab_sync_preflight.py"
PREFLIGHT_REPORT = ARTIFACT_DIR / "preflight-report.json"
DEPENDENCY_REMAP_PLAN = ARTIFACT_DIR / "dependency-remap-plan.json"
REMAINING_REMAP_PLAN = ARTIFACT_DIR / "remaining-dependency-remap-plan.json"
RESULT_PATH = ARTIFACT_DIR / "three-tab-whitelist-sync-result.json"
SUMMARY_PATH = ARTIFACT_DIR / "three-tab-whitelist-sync-summary.md"
TENANT_ID = 1

LOCAL_MYSQL = [
    "docker",
    "exec",
    "-i",
    "int-ruoyi-mysql",
    "sh",
    "-lc",
    'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql -uroot --default-character-set=utf8mb4 --batch --raw ruoyi-vue-pro',
]

LOCAL_MYSQL_NO_HEADERS = [
    "docker",
    "exec",
    "-i",
    "int-ruoyi-mysql",
    "sh",
    "-lc",
    'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql -uroot --default-character-set=utf8mb4 --batch --raw --skip-column-names ruoyi-vue-pro',
]

REMOTE_MYSQL = [
    "ssh",
    "root@172.30.30.58",
    'docker exec -i intruoyi-mysql sh -lc \'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql -uroot --default-character-set=utf8mb4 --batch --raw ruoyi-vue-pro\'',
]

REMOTE_MYSQL_NO_HEADERS = [
    "ssh",
    "root@172.30.30.58",
    'docker exec -i intruoyi-mysql sh -lc \'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql -uroot --default-character-set=utf8mb4 --batch --raw --skip-column-names ruoyi-vue-pro\'',
]

REMOTE_APPLY = [
    "ssh",
    "root@172.30.30.58",
    'docker exec -i intruoyi-mysql sh -lc \'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql -uroot --default-character-set=utf8mb4 ruoyi-vue-pro\'',
]

INSERT_ORDER = [
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
]

DELETE_ORDER = list(reversed(INSERT_ORDER))

BACKUP_SUFFIX = {
    "mes_pro_process": "proc",
    "mes_pro_process_content": "proccont",
    "mes_pro_route": "route",
    "mes_pro_route_version": "routever",
    "mes_pro_route_process": "routeproc",
    "mes_pro_route_process_flow_edge": "flowedge",
    "mes_pro_route_process_flow_layout": "flowlay",
    "mes_pro_route_process_flow_boundary_edge": "boundedge",
    "mes_pro_route_flow_config": "flowconf",
    "mes_pro_route_flow_process_config": "flowpconf",
    "mes_pro_route_flow_process_batch_record": "flowpbrec",
    "mes_pro_route_schedule_config": "schedconf",
    "mes_pro_route_product": "routeprod",
    "mes_pro_route_product_bom": "routebom",
    "mes_pro_edhr_work_task_assignment_rule": "assignrule",
    "mes_pro_schedule_order": "schedord",
    "mes_pro_schedule_order_process": "schedproc",
    "mes_pro_schedule_order_diff": "scheddiff",
    "mes_pro_schedule_order_daily_compare": "dailycomp",
    "mes_pro_schedule_order_operation_log": "oplog",
}

ID_SELECTS = {
    "mes_pro_process": "SELECT id FROM mes_pro_process WHERE tenant_id=1 AND deleted=0",
    "mes_pro_process_content": "SELECT id FROM mes_pro_process_content WHERE tenant_id=1 AND deleted=0 AND process_id IN (SELECT id FROM mes_pro_process WHERE tenant_id=1 AND deleted=0)",
    "mes_pro_route": "SELECT id FROM source_routes",
    "mes_pro_route_version": "SELECT id FROM source_route_versions",
    "mes_pro_route_process": "SELECT id FROM source_route_processes",
    "mes_pro_route_process_flow_edge": "SELECT id FROM mes_pro_route_process_flow_edge WHERE tenant_id=1 AND deleted=0 AND route_id IN (SELECT id FROM source_routes)",
    "mes_pro_route_process_flow_layout": "SELECT id FROM mes_pro_route_process_flow_layout WHERE tenant_id=1 AND deleted=0 AND route_id IN (SELECT id FROM source_routes)",
    "mes_pro_route_process_flow_boundary_edge": "SELECT id FROM mes_pro_route_process_flow_boundary_edge WHERE tenant_id=1 AND deleted=0 AND route_id IN (SELECT id FROM source_routes)",
    "mes_pro_route_flow_config": "SELECT id FROM source_flow_configs",
    "mes_pro_route_flow_process_config": "SELECT id FROM source_flow_process_configs",
    "mes_pro_route_flow_process_batch_record": "SELECT id FROM mes_pro_route_flow_process_batch_record WHERE tenant_id=1 AND deleted=0 AND (route_flow_process_config_id IN (SELECT id FROM source_flow_process_configs) OR route_id IN (SELECT id FROM source_routes) OR route_process_id IN (SELECT id FROM source_route_processes))",
    "mes_pro_route_schedule_config": "SELECT id FROM mes_pro_route_schedule_config WHERE tenant_id=1 AND deleted=0 AND route_process_id IN (SELECT id FROM source_route_processes)",
    "mes_pro_route_product": "SELECT id FROM mes_pro_route_product WHERE tenant_id=1 AND deleted=0 AND route_id IN (SELECT id FROM source_routes)",
    "mes_pro_route_product_bom": "SELECT id FROM mes_pro_route_product_bom WHERE tenant_id=1 AND deleted=0 AND route_id IN (SELECT id FROM source_routes)",
    "mes_pro_edhr_work_task_assignment_rule": "SELECT id FROM mes_pro_edhr_work_task_assignment_rule WHERE tenant_id=1 AND deleted=0 AND scope_type='ROUTE' AND task_type='RELEASE_APPROVE' AND scope_id IN (SELECT id FROM source_routes)",
    "mes_pro_schedule_order": "SELECT id FROM source_schedule_orders",
    "mes_pro_schedule_order_process": "SELECT id FROM source_schedule_order_processes",
    "mes_pro_schedule_order_diff": "SELECT id FROM mes_pro_schedule_order_diff WHERE tenant_id=1 AND deleted=0 AND schedule_order_id IN (SELECT id FROM source_schedule_orders)",
    "mes_pro_schedule_order_daily_compare": "SELECT id FROM mes_pro_schedule_order_daily_compare WHERE tenant_id=1 AND deleted=0 AND schedule_order_id IN (SELECT id FROM source_schedule_orders)",
    "mes_pro_schedule_order_operation_log": "SELECT id FROM mes_pro_schedule_order_operation_log WHERE tenant_id=1 AND deleted=0 AND schedule_order_id IN (SELECT id FROM source_schedule_orders)",
}

USER_REF_COLUMNS = {
    "mes_pro_edhr_work_task_assignment_rule": ["assignee_user_id", "review_user_id"],
    "mes_pro_route_version": ["submitted_by", "published_by"],
    "mes_pro_schedule_order_operation_log": ["operator_id"],
}

WORK_ORDER_REF_COLUMNS = {
    "mes_pro_schedule_order": ["work_order_id", "source_work_order_id"],
    "mes_pro_schedule_order_process": ["source_work_order_id"],
    "mes_pro_schedule_order_diff": ["work_order_id"],
}

WORKSTATION_REF_COLUMNS = {
    "mes_pro_route_process": ["workstation_id"],
    "mes_pro_schedule_order_process": ["workstation_id"],
}

JSON_REMAP_TABLES = {
    "mes_pro_route_version",
    "mes_pro_route_process",
    "mes_pro_route_flow_process_config",
    "mes_pro_route_flow_process_batch_record",
    "mes_pro_schedule_order",
    "mes_pro_schedule_order_process",
    "mes_pro_schedule_order_operation_log",
}


def fail(message):
    print(message, file=sys.stderr)
    raise SystemExit(1)


def load_preflight_module():
    spec = importlib.util.spec_from_file_location("three_tab_preflight", PREFLIGHT_SCRIPT)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


def parse_rows(stdout):
    lines = [line for line in stdout.splitlines() if line.strip()]
    if not lines:
        return []
    headers = lines[0].split("\t")
    return [
        {header: values[idx] if idx < len(values) else "" for idx, header in enumerate(headers)}
        for values in (line.split("\t") for line in lines[1:])
    ]


def run(cmd, sql, label, timeout=240, headers=True):
    proc = subprocess.run(
        cmd,
        input=sql,
        text=True,
        encoding="utf-8",
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
        timeout=timeout,
    )
    if proc.returncode != 0:
        fail(f"{label} failed with exit {proc.returncode}: {proc.stderr[:4000]}")
    warnings = []
    if proc.stderr.strip():
        warnings.append(proc.stderr.strip())
    if headers:
        return parse_rows(proc.stdout), warnings
    return [line for line in proc.stdout.splitlines() if line.strip()], warnings


def apply_remote(sql, label, timeout=600):
    proc = subprocess.run(
        REMOTE_APPLY,
        input=sql,
        text=True,
        encoding="utf-8",
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
        timeout=timeout,
    )
    if proc.returncode != 0:
        fail(f"{label} failed with exit {proc.returncode}: {proc.stderr[:5000]}")
    return proc.stdout, proc.stderr.strip()


def sql_in(values):
    values = sorted({str(value) for value in values if str(value) != ""}, key=lambda item: int(item))
    if not values:
        return "(NULL)"
    return "(" + ",".join(str(int(value)) for value in values) + ")"


def mysql_string(value):
    if value is None:
        return "NULL"
    encoded = str(value).encode("utf-8").hex()
    return f"CONVERT(UNHEX('{encoded}') USING utf8mb4)"


def sql_literal(value, dtype):
    if value is None:
        return "NULL"
    if dtype == "bit":
        return "b'1'" if str(value) in {"1", "\x01", "true", "True"} else "b'0'"
    if dtype in {
        "tinyint",
        "smallint",
        "mediumint",
        "int",
        "bigint",
        "decimal",
        "float",
        "double",
        "year",
    }:
        text = str(value)
        return "NULL" if text.upper() == "NULL" or text == "" else text
    return mysql_string(value)


def normalize(value):
    if value is None:
        return "NULL"
    text = str(value)
    if text == "\x00":
        return "0"
    if text == "\x01":
        return "1"
    return text


def load_json(path):
    if not path.exists():
        return {}
    return json.loads(path.read_text(encoding="utf-8"))


def load_remaps():
    dependency = load_json(DEPENDENCY_REMAP_PLAN)
    remaining = load_json(REMAINING_REMAP_PLAN)
    return {
        "user": {str(k): str(v) for k, v in dependency.get("remap", {}).get("system_users", {}).items()},
        "work_order": {str(k): str(v) for k, v in dependency.get("remap", {}).get("mes_pro_work_order", {}).items()},
        "workstation": {str(k): str(v) for k, v in remaining.get("remap", {}).get("mes_md_workstation", {}).items()},
    }


def check_preflight_clean():
    report = load_json(PREFLIGHT_REPORT)
    blockers = report.get("blockers", [])
    if blockers:
        fail(f"preflight still has blockers: {json.dumps(blockers, ensure_ascii=False, indent=2)}")


def table_columns(cmd, table, label):
    rows, warnings = run(
        cmd,
        f"""
SELECT COLUMN_NAME, DATA_TYPE, EXTRA, GENERATION_EXPRESSION, ORDINAL_POSITION
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='{table}'
ORDER BY ORDINAL_POSITION
""",
        label,
    )
    insertable_rows = [
        row for row in rows
        if "GENERATED" not in row.get("EXTRA", "").upper() and not row.get("GENERATION_EXPRESSION")
    ]
    return insertable_rows, warnings


def assert_same_columns(table, warnings):
    local_cols, local_warnings = table_columns(LOCAL_MYSQL, table, f"local schema {table}")
    remote_cols, remote_warnings = table_columns(REMOTE_MYSQL, table, f"remote schema {table}")
    warnings.extend(local_warnings + remote_warnings)
    local_names = [row["COLUMN_NAME"] for row in local_cols]
    remote_names = [row["COLUMN_NAME"] for row in remote_cols]
    missing_remote = [name for name in local_names if name not in remote_names]
    if missing_remote:
        fail(f"schema column mismatch for {table}: remote missing source columns {missing_remote}")
    extra_remote = [name for name in remote_names if name not in local_names]
    if extra_remote:
        warnings.append(f"{table} has remote-only columns ignored by source-column sync: {extra_remote}")
    return local_cols


def source_scope_sql(preflight):
    return preflight.source_scope_sql()


def source_ids(preflight, table, warnings):
    sql = source_scope_sql(preflight) + ID_SELECTS[table] + "\nORDER BY id"
    rows, warn = run(LOCAL_MYSQL, sql, f"local source ids {table}")
    warnings.extend(warn)
    return [row["id"] for row in rows]


def source_rows(preflight, table, ids, columns, warnings):
    if not ids:
        return {}
    exprs = []
    for col in columns:
        name = col["COLUMN_NAME"]
        dtype = col["DATA_TYPE"]
        if dtype == "bit":
            expr = f"'{name}', IF(`{name}` IS NULL, NULL, CAST(`{name}` + 0 AS CHAR))"
        elif dtype in {"datetime", "timestamp"}:
            expr = f"'{name}', DATE_FORMAT(`{name}`, '%Y-%m-%d %H:%i:%s')"
        elif dtype == "date":
            expr = f"'{name}', DATE_FORMAT(`{name}`, '%Y-%m-%d')"
        else:
            expr = f"'{name}', IF(`{name}` IS NULL, NULL, CAST(`{name}` AS CHAR))"
        exprs.append(expr)
    sql = f"""
SELECT JSON_OBJECT({", ".join(exprs)}) AS row_json
FROM `{table}`
WHERE tenant_id={TENANT_ID} AND id IN {sql_in(ids)}
ORDER BY id
"""
    lines, warn = run(LOCAL_MYSQL_NO_HEADERS, sql, f"local source rows {table}", headers=False)
    warnings.extend(warn)
    parsed = {}
    for line in lines:
        row = json.loads(line)
        parsed[str(row["id"])] = transform_row(table, row)
    missing = sorted(set(map(str, ids)) - set(parsed), key=int)
    if missing:
        fail(f"source rows missing for {table}: {missing}")
    return parsed


def apply_ref_map(value, mapping):
    text = normalize(value)
    if text in mapping:
        return mapping[text]
    return value


def transform_text_value(value, all_remaps):
    if value is None:
        return value
    text = str(value)
    for mapping in all_remaps:
        for source_id, target_id in mapping.items():
            text = text.replace(str(source_id), str(target_id))
    return text


def transform_row(table, row):
    remaps = CURRENT_REMAPS
    for column in USER_REF_COLUMNS.get(table, []):
        if column in row:
            row[column] = apply_ref_map(row[column], remaps["user"])
    for column in WORK_ORDER_REF_COLUMNS.get(table, []):
        if column in row:
            row[column] = apply_ref_map(row[column], remaps["work_order"])
    for column in WORKSTATION_REF_COLUMNS.get(table, []):
        if column in row:
            row[column] = apply_ref_map(row[column], remaps["workstation"])
    if table in JSON_REMAP_TABLES:
        for key, value in list(row.items()):
            if isinstance(value, str) and (value.startswith("{") or value.startswith("[") or len(value) > 200):
                row[key] = transform_text_value(value, [remaps["user"], remaps["work_order"], remaps["workstation"]])
    return row


def insert_statement(table, row, columns):
    names = [col["COLUMN_NAME"] for col in columns]
    literals = [sql_literal(row.get(col["COLUMN_NAME"]), col["DATA_TYPE"]) for col in columns]
    return f"INSERT INTO `{table}` ({', '.join(f'`{name}`' for name in names)}) VALUES ({', '.join(literals)});"


def target_rows(table, ids, columns, warnings):
    if not ids:
        return {}
    exprs = []
    for col in columns:
        name = col["COLUMN_NAME"]
        dtype = col["DATA_TYPE"]
        if dtype == "bit":
            expr = f"'{name}', IF(`{name}` IS NULL, NULL, CAST(`{name}` + 0 AS CHAR))"
        elif dtype in {"datetime", "timestamp"}:
            expr = f"'{name}', DATE_FORMAT(`{name}`, '%Y-%m-%d %H:%i:%s')"
        elif dtype == "date":
            expr = f"'{name}', DATE_FORMAT(`{name}`, '%Y-%m-%d')"
        else:
            expr = f"'{name}', IF(`{name}` IS NULL, NULL, CAST(`{name}` AS CHAR))"
        exprs.append(expr)
    sql = f"""
SELECT JSON_OBJECT({", ".join(exprs)}) AS row_json
FROM `{table}`
WHERE tenant_id={TENANT_ID} AND id IN {sql_in(ids)}
ORDER BY id
"""
    lines, warn = run(REMOTE_MYSQL_NO_HEADERS, sql, f"remote target rows {table}", headers=False)
    warnings.extend(warn)
    parsed = {}
    for line in lines:
        row = json.loads(line)
        parsed[str(row["id"])] = row
    return parsed


def active_target_count(table, warnings):
    rows, warn = run(
        REMOTE_MYSQL,
        f"SELECT COUNT(*) AS row_count FROM `{table}` WHERE tenant_id={TENANT_ID} AND deleted=0",
        f"remote active count {table}",
    )
    warnings.extend(warn)
    return int(rows[0]["row_count"])


def build_apply_sql(table_ids, table_rows, table_columns_map):
    timestamp = datetime.now(timezone.utc).strftime("%Y%m%d%H%M%S")
    backup_tables = {table: f"m3syncbk_{timestamp}_{BACKUP_SUFFIX[table]}" for table in INSERT_ORDER}
    sql_parts = ["SET NAMES utf8mb4;"]
    for table in DELETE_ORDER:
        backup = backup_tables[table]
        ids = table_ids[table]
        id_clause = f" OR id IN {sql_in(ids)}" if ids else ""
        where = f"tenant_id={TENANT_ID} AND (deleted=0{id_clause})"
        sql_parts.append(f"DROP TABLE IF EXISTS `{backup}`;")
        sql_parts.append(f"CREATE TABLE `{backup}` AS SELECT * FROM `{table}` WHERE {where};")
    sql_parts.append("START TRANSACTION;")
    for table in DELETE_ORDER:
        ids = table_ids[table]
        id_clause = f" OR id IN {sql_in(ids)}" if ids else ""
        where = f"tenant_id={TENANT_ID} AND (deleted=0{id_clause})"
        sql_parts.append(f"DELETE FROM `{table}` WHERE {where};")
    for table in INSERT_ORDER:
        for source_id in sorted(table_rows[table], key=int):
            sql_parts.append(insert_statement(table, table_rows[table][source_id], table_columns_map[table]))
    sql_parts.append("COMMIT;")
    return "\n".join(sql_parts) + "\n", backup_tables


def row_hash(rows):
    payload = json.dumps(rows, ensure_ascii=False, sort_keys=True, separators=(",", ":"))
    return hashlib.sha256(payload.encode("utf-8")).hexdigest()


def verify(table_ids, table_rows, table_columns_map):
    warnings = []
    checks = {}
    for table in INSERT_ORDER:
        remote_rows = target_rows(table, table_ids[table], table_columns_map[table], warnings)
        source_rows_by_id = table_rows[table]
        missing = sorted(set(source_rows_by_id) - set(remote_rows), key=int)
        extra_active = active_target_count(table, warnings) - len(source_rows_by_id)
        mismatched = []
        for row_id, source_row in source_rows_by_id.items():
            target_row = remote_rows.get(row_id)
            if not target_row:
                continue
            source_identity = {key: normalize(value) for key, value in source_row.items()}
            target_identity = {key: normalize(value) for key, value in target_row.items()}
            if source_identity != target_identity:
                mismatched.append(row_id)
        checks[table] = {
            "source_count": len(source_rows_by_id),
            "target_count": len(remote_rows),
            "active_extra_count": extra_active,
            "missing": missing[:20],
            "mismatched": mismatched[:20],
            "source_hash": row_hash(source_rows_by_id),
            "target_hash": row_hash(remote_rows),
        }
    failed = {
        table: check
        for table, check in checks.items()
        if check["missing"] or check["mismatched"] or check["active_extra_count"] != 0 or check["source_hash"] != check["target_hash"]
    }
    if failed:
        fail(f"three-tab whitelist verification failed: {json.dumps(failed, ensure_ascii=False, indent=2)[:5000]}")
    return checks, warnings


def main():
    ARTIFACT_DIR.mkdir(parents=True, exist_ok=True)
    check_preflight_clean()
    preflight = load_preflight_module()
    warnings = []
    table_columns_map = {table: assert_same_columns(table, warnings) for table in INSERT_ORDER}
    table_ids = {table: source_ids(preflight, table, warnings) for table in INSERT_ORDER}
    table_rows = {
        table: source_rows(preflight, table, table_ids[table], table_columns_map[table], warnings)
        for table in INSERT_ORDER
    }
    apply_sql, backup_tables = build_apply_sql(table_ids, table_rows, table_columns_map)
    stdout, stderr = apply_remote(apply_sql, "remote three-tab whitelist replacement")
    checks, verify_warnings = verify(table_ids, table_rows, table_columns_map)
    result = {
        "generated_at": datetime.now(timezone.utc).isoformat(),
        "backup_tables": backup_tables,
        "table_checks": checks,
        "apply_stdout": stdout.strip(),
        "warnings": ([stderr] if stderr else []) + warnings + verify_warnings,
    }
    RESULT_PATH.write_text(json.dumps(result, ensure_ascii=False, indent=2), encoding="utf-8")
    lines = [
        "# Three Tab Whitelist Sync Summary",
        "",
        f"- Generated: `{result['generated_at']}`",
        f"- Tables replaced: `{len(INSERT_ORDER)}`",
        f"- Source rows inserted: `{sum(len(table_rows[table]) for table in INSERT_ORDER)}`",
        f"- Backup tables: `{len(backup_tables)}`",
        "",
        "## Row Counts",
    ]
    for table in INSERT_ORDER:
        lines.append(f"- `{table}`: `{checks[table]['source_count']}`")
    SUMMARY_PATH.write_text("\n".join(lines) + "\n", encoding="utf-8")
    print(json.dumps({
        "result": str(RESULT_PATH),
        "summary": str(SUMMARY_PATH),
        "tables": len(INSERT_ORDER),
        "inserted_rows": sum(len(table_rows[table]) for table in INSERT_ORDER),
        "backup_tables": backup_tables,
    }, ensure_ascii=False, indent=2))


CURRENT_REMAPS = load_remaps()


if __name__ == "__main__":
    main()
