import json
import subprocess
import sys
from datetime import datetime, timezone
from pathlib import Path


ROOT = Path(__file__).resolve().parents[4]
TASK_DIR = ROOT / "doc" / "tasks" / "20260731-mes-three-tab-test-sync"
ARTIFACT_DIR = TASK_DIR / "artifacts"
PRECHECK_REPORT = ARTIFACT_DIR / "preflight-report.json"
PLAN_PATH = ARTIFACT_DIR / "remaining-dependency-remap-plan.json"
RESULT_PATH = ARTIFACT_DIR / "remaining-authorized-blockers-sync-result.json"
SUMMARY_PATH = ARTIFACT_DIR / "remaining-authorized-blockers-summary.md"
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

REMOTE_APPLY = [
    "ssh",
    "root@172.30.30.58",
    'docker exec -i intruoyi-mysql sh -lc \'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql -uroot --default-character-set=utf8mb4 ruoyi-vue-pro\'',
]

SYNC_TABLES = {
    "bpm_form_template_version": {
        "dep_type": "form_template_version_id",
        "identity_columns": ["template_id", "version_no", "status", "deleted", "tenant_id"],
    },
    "mes_pro_edhr_permission_scope": {
        "dep_type": "permission_scope_id",
        "identity_columns": ["scope_name", "object_type", "object_id", "status", "deleted", "tenant_id"],
    },
    "mes_pro_schedule_calendar_rule": {
        "dep_type": "calendar_rule_id",
        "identity_columns": [
            "skip_statutory_holidays",
            "weekend_rest_mode",
            "date_shift_mode_by_date_json",
            "temporary_freeze_enabled",
            "deleted",
            "tenant_id",
        ],
    },
    "mes_md_workstation": {
        "dep_type": "workstation_id",
        "identity_columns": ["code", "name", "process_id", "status", "deleted", "tenant_id"],
    },
}

BACKUP_SUFFIX = {
    "bpm_form_template_version": "bpm_ftv",
    "mes_pro_edhr_permission_scope": "edhr_scope",
    "mes_pro_schedule_calendar_rule": "cal_rule",
    "mes_md_workstation": "workstation",
}


def fail(message):
    print(message, file=sys.stderr)
    raise SystemExit(1)


def run(cmd, sql, label, timeout=180, headers=True):
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
        fail(f"{label} failed with exit {proc.returncode}: {proc.stderr[:3000]}")
    warnings = []
    if proc.stderr.strip():
        warnings.append(proc.stderr.strip())
    if headers:
        return parse_rows(proc.stdout), warnings
    return [line for line in proc.stdout.splitlines() if line.strip()], warnings


def apply_remote(sql, label, timeout=240):
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
        fail(f"{label} failed with exit {proc.returncode}: {proc.stderr[:4000]}")
    return proc.stdout, proc.stderr.strip()


def parse_rows(stdout):
    lines = [line for line in stdout.splitlines() if line.strip()]
    if not lines:
        return []
    headers = lines[0].split("\t")
    return [
        {header: values[idx] if idx < len(values) else "" for idx, header in enumerate(headers)}
        for values in (line.split("\t") for line in lines[1:])
    ]


def sql_in(values):
    values = [str(value) for value in values if str(value) != ""]
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


def load_precheck():
    if not PRECHECK_REPORT.exists():
        fail(f"missing preflight report: {PRECHECK_REPORT}")
    return json.loads(PRECHECK_REPORT.read_text(encoding="utf-8"))


def table_columns(cmd, table, label):
    sql = f"""
SELECT COLUMN_NAME, DATA_TYPE, COLUMN_TYPE, ORDINAL_POSITION
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='{table}'
ORDER BY ORDINAL_POSITION
"""
    return run(cmd, sql, label)


def assert_same_columns(table, warnings):
    local_cols, local_warnings = table_columns(LOCAL_MYSQL, table, f"local schema {table}")
    remote_cols, remote_warnings = table_columns(REMOTE_MYSQL, table, f"remote schema {table}")
    warnings.extend(local_warnings + remote_warnings)
    local_names = [row["COLUMN_NAME"] for row in local_cols]
    remote_names = [row["COLUMN_NAME"] for row in remote_cols]
    if local_names != remote_names:
        fail(f"schema column mismatch for {table}: local={local_names}, remote={remote_names}")
    return local_cols


def source_rows(table, ids, columns, warnings):
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
    rows = {}
    for line in lines:
        row = json.loads(line)
        rows[str(row["id"])] = row
    missing = sorted(set(map(str, ids)) - set(rows))
    if missing:
        fail(f"source {table} missing ids: {missing}")
    return rows


def target_rows(table, ids, warnings, tenant_only=False):
    if not ids:
        return []
    tenant_clause = f"tenant_id={TENANT_ID} AND " if tenant_only else ""
    sql = f"SELECT * FROM `{table}` WHERE {tenant_clause}id IN {sql_in(ids)} ORDER BY id, tenant_id"
    rows, warn = run(REMOTE_MYSQL, sql, f"remote target rows {table}")
    warnings.extend(warn)
    return rows


def max_and_auto(table, warnings):
    sql = f"""
SELECT
  (SELECT COALESCE(MAX(id), 0) FROM `{table}`) AS max_id,
  (SELECT COALESCE(AUTO_INCREMENT, 1) FROM information_schema.TABLES
    WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='{table}') AS auto_increment_value
"""
    rows, warn = run(REMOTE_MYSQL, sql, f"remote max id {table}")
    warnings.extend(warn)
    return int(rows[0]["max_id"]), int(rows[0]["auto_increment_value"])


def normalize(value):
    if value is None:
        return "NULL"
    text = str(value)
    if text == "\x00":
        return "0"
    if text == "\x01":
        return "1"
    return text


def row_identity(row, columns):
    return {column: normalize(row.get(column)) for column in columns}


def source_identity(row, columns):
    return {column: normalize(row.get(column)) for column in columns}


def find_next_id(table, reserved, warnings):
    max_id, auto_id = max_and_auto(table, warnings)
    candidate = max(max_id + 1, auto_id)
    existing = target_rows(table, range(candidate, candidate + 100), warnings)
    used = {int(row["id"]) for row in existing} | {int(value) for value in reserved}
    while candidate in used:
        candidate += 1
    return str(candidate)


def schema_actions_needed(warnings):
    rows, warn = run(
        REMOTE_MYSQL,
        """
SELECT TABLE_NAME, COLUMN_NAME, DATA_TYPE, COLUMN_TYPE, IS_NULLABLE
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA=DATABASE()
  AND (
    (TABLE_NAME='mes_pro_route_version' AND COLUMN_NAME='route_snapshot_json')
    OR (TABLE_NAME='mes_pro_schedule_order' AND COLUMN_NAME='promise_date')
    OR (TABLE_NAME='mes_pro_batch_record_report' AND COLUMN_NAME IN ('form_definition_id','form_version_id'))
    OR (TABLE_NAME='mes_md_workstation' AND COLUMN_NAME='single_standard_hourly_capacity')
  )
ORDER BY TABLE_NAME, COLUMN_NAME
""",
        "remote schema action scan",
    )
    warnings.extend(warn)
    current = {(row["TABLE_NAME"], row["COLUMN_NAME"]): row for row in rows}
    actions = []
    route_snapshot = current.get(("mes_pro_route_version", "route_snapshot_json"), {})
    if route_snapshot.get("DATA_TYPE") != "mediumtext":
        actions.append("ALTER TABLE `mes_pro_route_version` MODIFY COLUMN `route_snapshot_json` MEDIUMTEXT NULL")
    promise_date = current.get(("mes_pro_schedule_order", "promise_date"), {})
    if promise_date.get("IS_NULLABLE") != "YES":
        actions.append("ALTER TABLE `mes_pro_schedule_order` MODIFY COLUMN `promise_date` DATE NULL")
    if ("mes_pro_batch_record_report", "form_definition_id") not in current:
        actions.append("ALTER TABLE `mes_pro_batch_record_report` ADD COLUMN `form_definition_id` BIGINT NULL AFTER `batch_record_version_id`")
    if ("mes_pro_batch_record_report", "form_version_id") not in current:
        after_col = "form_definition_id" if ("mes_pro_batch_record_report", "form_definition_id") in current else "batch_record_version_id"
        actions.append(f"ALTER TABLE `mes_pro_batch_record_report` ADD COLUMN `form_version_id` BIGINT NULL AFTER `{after_col}`")
    workstation_capacity = current.get(("mes_md_workstation", "single_standard_hourly_capacity"), {})
    if workstation_capacity.get("COLUMN_TYPE") != "decimal(16,6)":
        actions.append("ALTER TABLE `mes_md_workstation` MODIFY COLUMN `single_standard_hourly_capacity` DECIMAL(16,6) NULL")
    return actions


def build_plan(precheck):
    warnings = []
    dep_ids = precheck.get("source_dependency_ids", {})
    schema_actions = schema_actions_needed(warnings)
    columns = {table: assert_same_columns(table, warnings) for table in SYNC_TABLES}
    source = {
        table: source_rows(table, dep_ids.get(spec["dep_type"], []), columns[table], warnings)
        for table, spec in SYNC_TABLES.items()
    }
    plan = {
        "generated_at": datetime.now(timezone.utc).isoformat(),
        "schema_actions": schema_actions,
        "remap": {"mes_md_workstation": {}},
        "preserve": {
            "bpm_form_template_version": [],
            "mes_pro_edhr_permission_scope": [],
            "mes_pro_schedule_calendar_rule": ["1"],
            "mes_md_workstation": [],
        },
        "insert_ids": {
            "bpm_form_template_version": [],
            "mes_pro_edhr_permission_scope": [],
            "mes_md_workstation": [],
        },
        "update_ids": {"mes_pro_schedule_calendar_rule": ["1"]},
        "already_exact_ids": {
            "bpm_form_template_version": [],
            "mes_pro_edhr_permission_scope": [],
            "mes_pro_schedule_calendar_rule": [],
            "mes_md_workstation": [],
        },
        "warnings": warnings,
    }

    for table in ["bpm_form_template_version", "mes_pro_edhr_permission_scope"]:
        ids = sorted(source[table], key=int)
        existing = target_rows(table, ids, warnings)
        if any(row.get("tenant_id") != str(TENANT_ID) for row in existing):
            fail(f"{table} has cross-tenant primary key conflicts: {existing}")
        same_tenant = {str(row["id"]): row for row in existing if row.get("tenant_id") == str(TENANT_ID)}
        identity_cols = SYNC_TABLES[table]["identity_columns"]
        for source_id, source_row in source[table].items():
            target_row = same_tenant.get(source_id)
            if target_row and row_identity(target_row, identity_cols) == source_identity(source_row, identity_cols):
                plan["already_exact_ids"][table].append(source_id)
            elif target_row:
                fail(f"{table} id {source_id} exists in tenant 1 with different identity; no remap authorized for this table")
            else:
                plan["preserve"][table].append(source_id)
                plan["insert_ids"][table].append(source_id)

    workstation_source_ids = sorted(source["mes_md_workstation"], key=int)
    existing_workstations = target_rows("mes_md_workstation", workstation_source_ids, warnings)
    identity_cols = SYNC_TABLES["mes_md_workstation"]["identity_columns"]
    for source_id in workstation_source_ids:
        same_id_rows = [row for row in existing_workstations if row["id"] == source_id]
        same_tenant = [row for row in same_id_rows if row.get("tenant_id") == str(TENANT_ID)]
        exact = same_tenant and row_identity(same_tenant[0], identity_cols) == source_identity(source["mes_md_workstation"][source_id], identity_cols)
        if exact:
            plan["already_exact_ids"]["mes_md_workstation"].append(source_id)
        elif same_id_rows:
            target_id = find_next_id("mes_md_workstation", set(workstation_source_ids), warnings)
            plan["remap"]["mes_md_workstation"][source_id] = target_id
            plan["insert_ids"]["mes_md_workstation"].append(target_id)
        else:
            plan["preserve"]["mes_md_workstation"].append(source_id)
            plan["insert_ids"]["mes_md_workstation"].append(source_id)
    return plan, source, columns


def insert_statement(table, row, columns, id_map=None):
    id_map = id_map or {}
    mapped = dict(row)
    source_id = str(row["id"])
    if source_id in id_map:
        mapped["id"] = id_map[source_id]
    names = [col["COLUMN_NAME"] for col in columns]
    literals = [sql_literal(mapped.get(col["COLUMN_NAME"]), col["DATA_TYPE"]) for col in columns]
    return f"INSERT INTO `{table}` ({', '.join(f'`{name}`' for name in names)}) VALUES ({', '.join(literals)});"


def update_statement(table, row, columns):
    assignments = []
    for col in columns:
        name = col["COLUMN_NAME"]
        if name == "id":
            continue
        assignments.append(f"`{name}` = {sql_literal(row.get(name), col['DATA_TYPE'])}")
    return f"UPDATE `{table}` SET {', '.join(assignments)} WHERE id={int(row['id'])} AND tenant_id={TENANT_ID};"


def pending_ids(plan, source):
    warnings = []
    pending_insert = {table: [] for table in ["bpm_form_template_version", "mes_pro_edhr_permission_scope", "mes_md_workstation"]}
    pending_update = {"mes_pro_schedule_calendar_rule": []}
    for table in pending_insert:
        id_map = plan["remap"].get(table, {})
        target_ids = sorted({id_map.get(source_id, source_id) for source_id in source[table]}, key=int)
        rows = target_rows(table, target_ids, warnings, tenant_only=True)
        by_id = {str(row["id"]): row for row in rows}
        identity_cols = SYNC_TABLES[table]["identity_columns"]
        for source_id, source_row in source[table].items():
            target_id = id_map.get(source_id, source_id)
            target_row = by_id.get(str(target_id))
            if not target_row:
                pending_insert[table].append(str(target_id))
            elif row_identity(target_row, identity_cols) != source_identity(source_row, identity_cols):
                fail(f"{table} target id {target_id} exists but does not match source identity")

    calendar_rows = target_rows("mes_pro_schedule_calendar_rule", ["1"], warnings, tenant_only=True)
    calendar_by_id = {str(row["id"]): row for row in calendar_rows}
    calendar_source = source["mes_pro_schedule_calendar_rule"].get("1")
    if calendar_source:
        current = calendar_by_id.get("1")
        if not current or row_identity(current, SYNC_TABLES["mes_pro_schedule_calendar_rule"]["identity_columns"]) != source_identity(calendar_source, SYNC_TABLES["mes_pro_schedule_calendar_rule"]["identity_columns"]):
            pending_update["mes_pro_schedule_calendar_rule"].append("1")
    return pending_insert, pending_update, warnings


def existing_backup_tables(warnings):
    rows, warn = run(
        REMOTE_MYSQL,
        """
SELECT TABLE_NAME
FROM information_schema.TABLES
WHERE TABLE_SCHEMA=DATABASE()
  AND TABLE_NAME LIKE 'm3rembk_%'
ORDER BY TABLE_NAME
""",
        "remote remaining backup table scan",
    )
    warnings.extend(warn)
    latest = {}
    for row in rows:
        name = row["TABLE_NAME"]
        for table, suffix in BACKUP_SUFFIX.items():
            if name.endswith("_" + suffix):
                latest[table] = name
    return latest


def build_apply_sql(plan, source, columns, pending_insert, pending_update):
    timestamp = datetime.now(timezone.utc).strftime("%Y%m%d%H%M%S")
    backup_tables = {table: f"m3rembk_{timestamp}_{suffix}" for table, suffix in BACKUP_SUFFIX.items()}
    sql_parts = ["SET NAMES utf8mb4;"]
    sql_parts.extend(action + ";" for action in plan["schema_actions"])
    for table, backup in backup_tables.items():
        source_ids = set(source[table])
        insert_ids = set(plan.get("insert_ids", {}).get(table, []))
        update_ids = set(plan.get("update_ids", {}).get(table, []))
        touched_ids = source_ids | insert_ids | update_ids
        sql_parts.append(f"DROP TABLE IF EXISTS `{backup}`;")
        sql_parts.append(f"CREATE TABLE `{backup}` LIKE `{table}`;")
        sql_parts.append(
            f"INSERT INTO `{backup}` SELECT * FROM `{table}` WHERE tenant_id={TENANT_ID} AND id IN {sql_in(touched_ids)};"
        )
    sql_parts.append("START TRANSACTION;")
    for table in ["bpm_form_template_version", "mes_pro_edhr_permission_scope", "mes_md_workstation"]:
        id_map = plan["remap"].get(table, {})
        pending_targets = set(pending_insert.get(table, []))
        for source_id, row in sorted(source[table].items(), key=lambda item: int(item[0])):
            target_id = id_map.get(source_id, source_id)
            if target_id in pending_targets:
                sql_parts.append(insert_statement(table, row, columns[table], id_map))
    if pending_update["mes_pro_schedule_calendar_rule"]:
        sql_parts.append(update_statement(
            "mes_pro_schedule_calendar_rule",
            source["mes_pro_schedule_calendar_rule"]["1"],
            columns["mes_pro_schedule_calendar_rule"],
        ))
    sql_parts.append("COMMIT;")
    return "\n".join(sql_parts) + "\n", backup_tables


def verify(plan, source):
    warnings = []
    checks = {}
    for table in SYNC_TABLES:
        id_map = plan["remap"].get(table, {})
        target_ids = sorted({id_map.get(source_id, source_id) for source_id in source[table]}, key=int)
        rows = target_rows(table, target_ids, warnings, tenant_only=True)
        by_id = {str(row["id"]): row for row in rows}
        missing = []
        mismatched = []
        identity_cols = SYNC_TABLES[table]["identity_columns"]
        for source_id, source_row in source[table].items():
            target_id = id_map.get(source_id, source_id)
            target_row = by_id.get(str(target_id))
            if not target_row:
                missing.append({"source_id": source_id, "target_id": target_id})
            elif row_identity(target_row, identity_cols) != source_identity(source_row, identity_cols):
                mismatched.append({
                    "source_id": source_id,
                    "target_id": target_id,
                    "source": source_identity(source_row, identity_cols),
                    "target": row_identity(target_row, identity_cols),
                })
        checks[table] = {"missing": missing, "mismatched": mismatched}
    failed = {table: value for table, value in checks.items() if value["missing"] or value["mismatched"]}
    if failed:
        fail(f"remaining dependency verification failed: {json.dumps(failed, ensure_ascii=False, indent=2)}")
    return checks, warnings


def write_summary(plan, result):
    lines = [
        "# Remaining Authorized Blockers Summary",
        "",
        f"- Generated: `{result['generated_at']}`",
        f"- Schema actions: `{len(plan['schema_actions'])}`",
        f"- Form template versions inserted: `{len(plan['insert_ids']['bpm_form_template_version'])}`",
        f"- Permission scopes inserted: `{len(plan['insert_ids']['mes_pro_edhr_permission_scope'])}`",
        f"- Calendar rows updated: `{len(plan['update_ids']['mes_pro_schedule_calendar_rule'])}`",
        f"- Workstation remaps: `{len(plan['remap']['mes_md_workstation'])}`",
        "",
        "## Workstation Remap",
    ]
    if plan["remap"]["mes_md_workstation"]:
        for source_id, target_id in sorted(plan["remap"]["mes_md_workstation"].items(), key=lambda item: int(item[0])):
            lines.append(f"- `{source_id}` -> `{target_id}`")
    else:
        lines.append("- None")
    SUMMARY_PATH.write_text("\n".join(lines) + "\n", encoding="utf-8")


def main():
    ARTIFACT_DIR.mkdir(parents=True, exist_ok=True)
    precheck = load_precheck()
    if PLAN_PATH.exists():
        plan = json.loads(PLAN_PATH.read_text(encoding="utf-8"))
        source_warnings = []
        columns = {table: assert_same_columns(table, source_warnings) for table in SYNC_TABLES}
        source = {
            table: source_rows(table, precheck.get("source_dependency_ids", {}).get(spec["dep_type"], []), columns[table], source_warnings)
            for table, spec in SYNC_TABLES.items()
        }
        plan.setdefault("warnings", []).extend(source_warnings)
        plan_source = "existing"
    else:
        plan, source, columns = build_plan(precheck)
        PLAN_PATH.write_text(json.dumps(plan, ensure_ascii=False, indent=2), encoding="utf-8")
        plan_source = "generated"

    pending_insert, pending_update, pending_warnings = pending_ids(plan, source)
    pending_total = sum(len(values) for values in pending_insert.values()) + sum(len(values) for values in pending_update.values())
    schema_actions = schema_actions_needed(pending_warnings)
    plan["schema_actions"] = schema_actions
    PLAN_PATH.write_text(json.dumps(plan, ensure_ascii=False, indent=2), encoding="utf-8")
    if schema_actions:
        pending_total += len(schema_actions)
    if pending_total:
        apply_sql, backup_tables = build_apply_sql(plan, source, columns, pending_insert, pending_update)
        stdout, stderr = apply_remote(apply_sql, "remote remaining authorized blockers apply")
    else:
        backup_tables = existing_backup_tables(pending_warnings)
        stdout = "SKIPPED: remaining authorized blockers already match plan"
        stderr = ""

    checks, verify_warnings = verify(plan, source)
    result = {
        "generated_at": datetime.now(timezone.utc).isoformat(),
        "plan_source": plan_source,
        "plan": str(PLAN_PATH),
        "summary": str(SUMMARY_PATH),
        "backup_tables": backup_tables,
        "pending_insert_ids": pending_insert,
        "pending_update_ids": pending_update,
        "pending_total": pending_total,
        "postcheck": checks,
        "apply_stdout": stdout.strip(),
        "warnings": [stderr] if stderr else [],
    }
    result["warnings"].extend(pending_warnings + verify_warnings)
    RESULT_PATH.write_text(json.dumps(result, ensure_ascii=False, indent=2), encoding="utf-8")
    write_summary(plan, result)
    print(json.dumps({
        "plan_source": plan_source,
        "plan": str(PLAN_PATH),
        "summary": str(SUMMARY_PATH),
        "result": str(RESULT_PATH),
        "pending_total": pending_total,
        "schema_actions": len(plan["schema_actions"]),
        "workstation_remaps": len(plan["remap"]["mes_md_workstation"]),
        "backup_tables": backup_tables,
    }, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
