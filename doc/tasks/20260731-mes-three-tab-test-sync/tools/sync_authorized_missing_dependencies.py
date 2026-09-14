import json
import subprocess
import sys
from datetime import datetime, timezone
from pathlib import Path


ROOT = Path(__file__).resolve().parents[4]
TASK_DIR = ROOT / "doc" / "tasks" / "20260731-mes-three-tab-test-sync"
ARTIFACT_DIR = TASK_DIR / "artifacts"
TENANT_ID = 1

PRECHECK_REPORT = ARTIFACT_DIR / "preflight-report.json"
PLAN_PATH = ARTIFACT_DIR / "dependency-remap-plan.json"
SUMMARY_PATH = ARTIFACT_DIR / "dependency-remap-summary.md"
RESULT_PATH = ARTIFACT_DIR / "authorized-dependency-sync-result.json"

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

DEPENDENCY_TABLES = {
    "mes_md_item": {
        "dep_type": "item_id",
        "identity_columns": ["code", "name", "specification", "status", "deleted", "tenant_id"],
    },
    "system_users": {
        "dep_type": "user_id",
        "identity_columns": ["username", "nickname", "status", "deleted", "tenant_id"],
    },
    "mes_pro_work_order": {
        "dep_type": "work_order_id",
        "identity_columns": ["code", "product_id", "quantity", "batch_code", "status", "deleted", "tenant_id"],
    },
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
        fail(f"{label} failed with exit {proc.returncode}: {proc.stderr[:2000]}")
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
        fail(f"{label} failed with exit {proc.returncode}: {proc.stderr[:3000]}")
    return proc.stdout, proc.stderr.strip()


def parse_rows(stdout):
    lines = [line for line in stdout.splitlines() if line.strip()]
    if not lines:
        return []
    headers = lines[0].split("\t")
    parsed = []
    for line in lines[1:]:
        values = line.split("\t")
        parsed.append({header: values[idx] if idx < len(values) else "" for idx, header in enumerate(headers)})
    return parsed


def sql_in(values):
    values = [str(value) for value in values]
    if not values:
        return "(NULL)"
    return "(" + ",".join(str(int(value)) for value in values) + ")"


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
    rows, warnings = run(cmd, sql, label)
    return rows, warnings


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
        fail(f"source {table} missing required ids: {missing}")
    return rows


def target_rows(table, ids, warnings, tenant_only=False):
    if not ids:
        return []
    tenant_filter = f"tenant_id={TENANT_ID} AND " if tenant_only else ""
    sql = f"SELECT * FROM `{table}` WHERE {tenant_filter}id IN {sql_in(ids)} ORDER BY id, tenant_id"
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


def existing_remap_backup_tables(warnings):
    sql = """
SELECT TABLE_NAME
FROM information_schema.TABLES
WHERE TABLE_SCHEMA=DATABASE()
  AND TABLE_NAME LIKE 'mes_three_tab_dep_remap_backup_%'
ORDER BY TABLE_NAME
"""
    rows, warn = run(REMOTE_MYSQL, sql, "remote existing remap backup tables")
    warnings.extend(warn)
    latest = {}
    for row in rows:
        name = row["TABLE_NAME"]
        for table in DEPENDENCY_TABLES:
            if name.endswith("_" + table):
                latest[table] = name
    return latest


def row_identity(row, columns):
    return {column: normalize(row.get(column)) for column in columns}


def normalize(value):
    if value is None:
        return "NULL"
    text = str(value)
    if text == "\x00":
        return "0"
    if text == "\x01":
        return "1"
    return text


def source_identity(row, columns):
    return {column: normalize(row.get(column)) for column in columns}


def find_next_ids(table, count, reserved, warnings):
    max_id, auto_id = max_and_auto(table, warnings)
    candidate = max(max_id + 1, auto_id)
    existing_rows = target_rows(table, range(candidate, candidate + count + 200), warnings)
    existing = {int(row["id"]) for row in existing_rows}
    reserved_ints = {int(value) for value in reserved}
    selected = []
    while len(selected) < count:
        if candidate not in existing and candidate not in reserved_ints:
            selected.append(candidate)
            reserved_ints.add(candidate)
        candidate += 1
    return selected


def load_source_and_columns(precheck):
    warnings = []
    source_dep_ids = precheck.get("source_dependency_ids", {})
    columns = {table: assert_same_columns(table, warnings) for table in DEPENDENCY_TABLES}
    source = {
        table: source_rows(table, source_dep_ids.get(spec["dep_type"], []), columns[table], warnings)
        for table, spec in DEPENDENCY_TABLES.items()
    }
    return source, columns, warnings


def build_plan(precheck):
    source, columns, warnings = load_source_and_columns(precheck)

    plan = {
        "generated_at": datetime.now(timezone.utc).isoformat(),
        "target": "172.30.30.58 / ruoyi-vue-pro / tenant_id=1",
        "strategy": "preserve source IDs when globally free or already exact in tenant 1; assign deterministic next free IDs for global conflicts or tenant-1 same-ID mismatches",
        "remap": {"system_users": {}, "mes_pro_work_order": {}},
        "preserve": {"mes_md_item": [], "system_users": [], "mes_pro_work_order": []},
        "insert_ids": {"mes_md_item": [], "system_users": [], "mes_pro_work_order": []},
        "already_exact_ids": {"mes_md_item": [], "system_users": [], "mes_pro_work_order": []},
        "business_key_duplicates": {"system_users": [], "mes_pro_work_order": [], "mes_md_item": []},
        "warnings": warnings,
    }

    item_ids = sorted(source["mes_md_item"], key=int)
    existing_items = target_rows("mes_md_item", item_ids, warnings)
    existing_item_by_id = {str(row["id"]): row for row in existing_items if row.get("tenant_id") == str(TENANT_ID)}
    for source_id in item_ids:
        target_row = existing_item_by_id.get(source_id)
        if target_row and row_identity(target_row, DEPENDENCY_TABLES["mes_md_item"]["identity_columns"]) == source_identity(source["mes_md_item"][source_id], DEPENDENCY_TABLES["mes_md_item"]["identity_columns"]):
            plan["already_exact_ids"]["mes_md_item"].append(source_id)
        else:
            plan["preserve"]["mes_md_item"].append(source_id)
            plan["insert_ids"]["mes_md_item"].append(source_id)

    user_ids = sorted(source["system_users"], key=int)
    existing_users = target_rows("system_users", user_ids, warnings)
    user_conflicts = []
    for source_id in user_ids:
        same_id_rows = [row for row in existing_users if row["id"] == source_id]
        same_tenant = [row for row in same_id_rows if row.get("tenant_id") == str(TENANT_ID)]
        exact = same_tenant and row_identity(same_tenant[0], DEPENDENCY_TABLES["system_users"]["identity_columns"]) == source_identity(source["system_users"][source_id], DEPENDENCY_TABLES["system_users"]["identity_columns"])
        if exact:
            plan["already_exact_ids"]["system_users"].append(source_id)
        elif same_id_rows:
            user_conflicts.append(source_id)
        else:
            plan["preserve"]["system_users"].append(source_id)
            plan["insert_ids"]["system_users"].append(source_id)
    new_user_ids = find_next_ids("system_users", len(user_conflicts), set(user_ids), warnings)
    for source_id, target_id in zip(user_conflicts, new_user_ids):
        plan["remap"]["system_users"][source_id] = str(target_id)
        plan["insert_ids"]["system_users"].append(str(target_id))

    work_order_ids = sorted(source["mes_pro_work_order"], key=int)
    existing_work_orders = target_rows("mes_pro_work_order", work_order_ids, warnings)
    work_order_conflicts = []
    for source_id in work_order_ids:
        same_id_rows = [row for row in existing_work_orders if row["id"] == source_id]
        same_tenant = [row for row in same_id_rows if row.get("tenant_id") == str(TENANT_ID)]
        exact = same_tenant and row_identity(same_tenant[0], DEPENDENCY_TABLES["mes_pro_work_order"]["identity_columns"]) == source_identity(source["mes_pro_work_order"][source_id], DEPENDENCY_TABLES["mes_pro_work_order"]["identity_columns"])
        if exact:
            plan["already_exact_ids"]["mes_pro_work_order"].append(source_id)
        elif same_id_rows:
            work_order_conflicts.append(source_id)
        else:
            plan["preserve"]["mes_pro_work_order"].append(source_id)
            plan["insert_ids"]["mes_pro_work_order"].append(source_id)
    reserved_work_order_ids = set(work_order_ids) | set(plan["preserve"]["mes_pro_work_order"]) | set(plan["already_exact_ids"]["mes_pro_work_order"])
    new_work_order_ids = find_next_ids("mes_pro_work_order", len(work_order_conflicts), reserved_work_order_ids, warnings)
    for source_id, target_id in zip(work_order_conflicts, new_work_order_ids):
        plan["remap"]["mes_pro_work_order"][source_id] = str(target_id)
        plan["insert_ids"]["mes_pro_work_order"].append(str(target_id))

    collect_business_key_duplicates(plan, source, warnings)
    return plan, source, columns


def pending_insert_ids(plan, source):
    warnings = []
    pending = {"mes_md_item": [], "system_users": [], "mes_pro_work_order": []}
    for table in pending:
        id_map = plan["remap"].get(table, {})
        target_ids = sorted({id_map.get(source_id, source_id) for source_id in source[table]}, key=int)
        rows = target_rows(table, target_ids, warnings, tenant_only=True)
        by_id = {str(row["id"]): row for row in rows}
        identity_cols = DEPENDENCY_TABLES[table]["identity_columns"]
        for source_id, source_row in sorted(source[table].items(), key=lambda item: int(item[0])):
            target_id = id_map.get(source_id, source_id)
            target_row = by_id.get(str(target_id))
            if not target_row:
                pending[table].append(str(target_id))
                continue
            if row_identity(target_row, identity_cols) != source_identity(source_row, identity_cols):
                fail(
                    "target row exists but does not match source identity: "
                    f"table={table}, source_id={source_id}, target_id={target_id}"
                )
    return pending, warnings


def collect_business_key_duplicates(plan, source, warnings):
    usernames = sorted({row["username"] for row in source["system_users"].values() if row.get("username")})
    if usernames:
        quoted = ",".join(hex_string(value) for value in usernames)
        rows, warn = run(
            REMOTE_MYSQL,
            f"SELECT id, username, nickname, tenant_id FROM system_users WHERE tenant_id={TENANT_ID} AND HEX(username) IN ({quoted}) ORDER BY username, id",
            "remote user business duplicate scan",
        )
        warnings.extend(warn)
        plan["business_key_duplicates"]["system_users"] = rows

    item_codes = sorted({row["code"] for row in source["mes_md_item"].values() if row.get("code")})
    if item_codes:
        quoted = ",".join(hex_string(value) for value in item_codes)
        rows, warn = run(
            REMOTE_MYSQL,
            f"SELECT id, code, name, tenant_id FROM mes_md_item WHERE tenant_id={TENANT_ID} AND HEX(code) IN ({quoted}) ORDER BY code, id",
            "remote item business duplicate scan",
        )
        warnings.extend(warn)
        plan["business_key_duplicates"]["mes_md_item"] = rows

    work_order_codes = sorted({row["code"] for row in source["mes_pro_work_order"].values() if row.get("code")})
    if work_order_codes:
        quoted = ",".join(hex_string(value) for value in work_order_codes)
        rows, warn = run(
            REMOTE_MYSQL,
            "SELECT id, code, product_id, quantity, batch_code, tenant_id "
            f"FROM mes_pro_work_order WHERE tenant_id={TENANT_ID} AND HEX(code) IN ({quoted}) ORDER BY code, id",
            "remote work order business duplicate scan",
        )
        warnings.extend(warn)
        plan["business_key_duplicates"]["mes_pro_work_order"] = rows


def mysql_string(value):
    if value is None:
        return "NULL"
    encoded = str(value).encode("utf-8").hex()
    return f"CONVERT(UNHEX('{encoded}') USING utf8mb4)"


def hex_string(value):
    return "'" + str(value).encode("utf-8").hex().upper() + "'"


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


def insert_statement(table, row, columns, id_map):
    mapped = dict(row)
    source_id = str(row["id"])
    if source_id in id_map:
        mapped["id"] = id_map[source_id]
    if table == "mes_pro_work_order":
        parent_id = str(mapped.get("parent_id") or "0")
        if parent_id in id_map:
            mapped["parent_id"] = id_map[parent_id]
    names = [col["COLUMN_NAME"] for col in columns]
    literals = [sql_literal(mapped.get(col["COLUMN_NAME"]), col["DATA_TYPE"]) for col in columns]
    quoted_names = ", ".join(f"`{name}`" for name in names)
    return f"INSERT INTO `{table}` ({quoted_names}) VALUES ({', '.join(literals)});"


def build_apply_sql(plan, source, columns, pending_ids):
    timestamp = datetime.now(timezone.utc).strftime("%Y%m%d%H%M%S")
    backup_tables = {
        table: f"mes_three_tab_dep_remap_backup_{timestamp}_{table}"
        for table in DEPENDENCY_TABLES
    }
    sql_parts = ["SET NAMES utf8mb4;"]
    for table in DEPENDENCY_TABLES:
        backup = backup_tables[table]
        touched_ids = set(plan["insert_ids"][table]) | set(source[table].keys())
        sql_parts.append(f"DROP TABLE IF EXISTS `{backup}`;")
        sql_parts.append(f"CREATE TABLE `{backup}` LIKE `{table}`;")
        sql_parts.append(
            f"INSERT INTO `{backup}` SELECT * FROM `{table}` "
            f"WHERE tenant_id={TENANT_ID} AND id IN {sql_in(touched_ids)};"
        )
    sql_parts.append("START TRANSACTION;")
    for table in ["mes_md_item", "system_users", "mes_pro_work_order"]:
        id_map = plan["remap"].get(table, {})
        if table == "mes_md_item":
            id_map = {}
        rows_by_source_id = source[table]
        insert_targets = set(pending_ids.get(table, []))
        for source_id in sorted(rows_by_source_id, key=int):
            target_id = id_map.get(source_id, source_id)
            if target_id not in insert_targets:
                continue
            sql_parts.append(insert_statement(table, rows_by_source_id[source_id], columns[table], id_map))
    sql_parts.append("COMMIT;")
    return "\n".join(sql_parts) + "\n", backup_tables


def verify_plan(plan, source):
    warnings = []
    checks = {}
    for table in ["mes_md_item", "system_users", "mes_pro_work_order"]:
        target_ids = sorted(set(plan["insert_ids"][table]) | set(plan["already_exact_ids"][table]), key=int)
        rows = target_rows(table, target_ids, warnings, tenant_only=True)
        by_id = {str(row["id"]): row for row in rows}
        missing = []
        mismatched = []
        id_map = plan["remap"].get(table, {})
        identity_cols = DEPENDENCY_TABLES[table]["identity_columns"]
        for source_id, source_row in sorted(source[table].items(), key=lambda item: int(item[0])):
            target_id = id_map.get(source_id, source_id)
            target_row = by_id.get(str(target_id))
            if not target_row:
                missing.append({"source_id": source_id, "target_id": str(target_id)})
                continue
            if row_identity(target_row, identity_cols) != source_identity(source_row, identity_cols):
                mismatched.append({
                    "source_id": source_id,
                    "target_id": str(target_id),
                    "source": source_identity(source_row, identity_cols),
                    "target": row_identity(target_row, identity_cols),
                })
        checks[table] = {"missing": missing, "mismatched": mismatched}
    failed = {table: check for table, check in checks.items() if check["missing"] or check["mismatched"]}
    if failed:
        fail(f"postcheck failed: {json.dumps(failed, ensure_ascii=False, indent=2)}")
    return checks, warnings


def write_plan_summary(plan):
    lines = [
        "# Dependency Remap Summary",
        "",
        f"- Generated: `{plan['generated_at']}`",
        f"- Item preserved inserts: `{len(plan['insert_ids']['mes_md_item'])}`",
        f"- User remaps: `{len(plan['remap']['system_users'])}`",
        f"- Work order remaps: `{len(plan['remap']['mes_pro_work_order'])}`",
        f"- Work order preserved inserts: `{len(plan['preserve']['mes_pro_work_order'])}`",
        f"- Work order already exact: `{len(plan['already_exact_ids']['mes_pro_work_order'])}`",
        "",
        "## User Remap",
    ]
    if plan["remap"]["system_users"]:
        for source_id, target_id in sorted(plan["remap"]["system_users"].items(), key=lambda item: int(item[0])):
            lines.append(f"- `{source_id}` -> `{target_id}`")
    else:
        lines.append("- None")
    lines.extend(["", "## Work Order Remaps"])
    if plan["remap"]["mes_pro_work_order"]:
        for source_id, target_id in sorted(plan["remap"]["mes_pro_work_order"].items(), key=lambda item: int(item[0])):
            lines.append(f"- `{source_id}` -> `{target_id}`")
    else:
        lines.append("- None")
    lines.extend(["", "## Business Key Duplicate Notice"])
    lines.append(
        f"- Existing tenant-1 work orders with matching source codes: `{len(plan['business_key_duplicates']['mes_pro_work_order'])}`"
    )
    lines.append("- Existing rows are not overwritten or reused; remapped dependencies are inserted under deterministic IDs.")
    SUMMARY_PATH.write_text("\n".join(lines) + "\n", encoding="utf-8")


def main():
    ARTIFACT_DIR.mkdir(parents=True, exist_ok=True)
    precheck = load_precheck()
    if PLAN_PATH.exists():
        source, columns, source_warnings = load_source_and_columns(precheck)
        plan = json.loads(PLAN_PATH.read_text(encoding="utf-8"))
        plan.setdefault("warnings", []).extend(source_warnings)
        plan_source = "existing"
    else:
        plan, source, columns = build_plan(precheck)
        PLAN_PATH.write_text(json.dumps(plan, ensure_ascii=False, indent=2), encoding="utf-8")
        plan_source = "generated"
    write_plan_summary(plan)

    pending_ids, pending_warnings = pending_insert_ids(plan, source)
    pending_total = sum(len(values) for values in pending_ids.values())
    if pending_total:
        apply_sql, backup_tables = build_apply_sql(plan, source, columns, pending_ids)
        stdout, stderr = apply_remote(apply_sql, "remote authorized dependency remap apply")
    else:
        backup_tables = existing_remap_backup_tables(pending_warnings)
        stdout = "SKIPPED: dependency rows already match dependency-remap-plan.json"
        stderr = ""
    checks, verify_warnings = verify_plan(plan, source)
    result = {
        "generated_at": datetime.now(timezone.utc).isoformat(),
        "plan_source": plan_source,
        "plan": str(PLAN_PATH),
        "summary": str(SUMMARY_PATH),
        "backup_tables": backup_tables,
        "pending_insert_ids": pending_ids,
        "postcheck": checks,
        "apply_stdout": stdout.strip(),
        "warnings": [stderr] if stderr else [],
    }
    result["warnings"].extend(pending_warnings + verify_warnings)
    RESULT_PATH.write_text(json.dumps(result, ensure_ascii=False, indent=2), encoding="utf-8")
    print(json.dumps({
        "plan_source": plan_source,
        "plan": str(PLAN_PATH),
        "summary": str(SUMMARY_PATH),
        "result": str(RESULT_PATH),
        "user_remaps": len(plan["remap"]["system_users"]),
        "work_order_remaps": len(plan["remap"]["mes_pro_work_order"]),
        "work_order_preserved_inserts": len(plan["preserve"]["mes_pro_work_order"]),
        "pending_insert_total": pending_total,
        "backup_tables": backup_tables,
    }, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
