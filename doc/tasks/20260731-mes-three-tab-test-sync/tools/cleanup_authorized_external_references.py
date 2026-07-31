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
RESULT_PATH = ARTIFACT_DIR / "external-reference-cleanup-result.json"
SUMMARY_PATH = ARTIFACT_DIR / "external-reference-cleanup-summary.md"
TENANT_ID = 1

REMOTE_APPLY = [
    "ssh",
    "root@172.30.30.58",
    'docker exec -i intruoyi-mysql sh -lc \'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql -uroot --default-character-set=utf8mb4 ruoyi-vue-pro\'',
]

REMOTE_MYSQL = [
    "ssh",
    "root@172.30.30.58",
    'docker exec -i intruoyi-mysql sh -lc \'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql -uroot --default-character-set=utf8mb4 --batch --raw ruoyi-vue-pro\'',
]


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


def run_remote(sql, label, timeout=180):
    proc = subprocess.run(
        REMOTE_MYSQL,
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
    return parse_rows(proc.stdout), warnings


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


def sql_in(values):
    values = sorted({str(value) for value in values if str(value) != ""}, key=lambda item: int(item))
    if not values:
        return "(NULL)"
    return "(" + ",".join(str(int(value)) for value in values) + ")"


def external_reference_groups(preflight):
    remote_ids, warnings = preflight.collect_ids(preflight.REMOTE_MYSQL_CMD, "remote-cleanup")
    refs, warnings2 = preflight.external_ref_sql(remote_ids)
    warnings.extend(warnings2)
    return refs, remote_ids, warnings


def table_columns(tables):
    quoted = ",".join("'" + table + "'" for table in tables)
    rows, warnings = run_remote(
        f"""
SELECT TABLE_NAME, COLUMN_NAME, COLUMN_KEY
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA=DATABASE()
  AND TABLE_NAME IN ({quoted})
ORDER BY TABLE_NAME, ORDINAL_POSITION
""",
        "remote external cleanup schema",
    )
    table_cols = {}
    for row in rows:
        table_cols.setdefault(row["TABLE_NAME"], set()).add(row["COLUMN_NAME"])
    return table_cols, warnings


def backup_suffix(table):
    cleaned = table
    for prefix in ["mes_pro_", "mes_md_", "mes_dv_"]:
        cleaned = cleaned.replace(prefix, "")
    cleaned = cleaned.replace("_legacy_20260709", "_legacy")
    cleaned = "".join(part[:4] for part in cleaned.split("_") if part)
    return cleaned[:24]


def build_cleanup_sql(refs, remote_ids, table_cols):
    timestamp = datetime.now(timezone.utc).strftime("%Y%m%d%H%M%S")
    by_table = {}
    ref_values = {
        "process_id": remote_ids["process_ids"],
        "route_id": remote_ids["route_ids"],
        "route_process_id": remote_ids["route_process_ids"],
        "schedule_order_id": remote_ids["schedule_order_ids"],
        "schedule_order_process_id": remote_ids["schedule_order_process_ids"],
    }
    for ref in refs:
        table = ref["table"]
        column = ref["column"]
        by_table.setdefault(table, set()).add(column)
    backup_tables = {
        table: f"m3extbk_{timestamp}_{backup_suffix(table)}"
        for table in by_table
    }
    sql_parts = ["SET NAMES utf8mb4;"]
    for table, columns in sorted(by_table.items()):
        cols = table_cols.get(table, set())
        if "deleted" not in cols:
            fail(f"external reference table {table} has no deleted column; hard delete is not allowed")
        predicates = [
            f"`{column}` IN {sql_in(ref_values[column])}"
            for column in sorted(columns)
            if ref_values.get(column)
        ]
        if not predicates:
            continue
        where = f"tenant_id={TENANT_ID} AND deleted=b'0' AND (" + " OR ".join(predicates) + ")"
        backup = backup_tables[table]
        sql_parts.append(f"DROP TABLE IF EXISTS `{backup}`;")
        sql_parts.append(f"CREATE TABLE `{backup}` LIKE `{table}`;")
        sql_parts.append(f"INSERT INTO `{backup}` SELECT * FROM `{table}` WHERE {where};")
        sql_parts.append(f"UPDATE `{table}` SET deleted=b'1' WHERE {where};")
    sql_parts.append("SELECT 'EXTERNAL_REFERENCE_CLEANUP_DONE';")
    return "\n".join(sql_parts) + "\n", backup_tables


def existing_external_backup_tables(warnings):
    rows, warn = run_remote(
        """
SELECT TABLE_NAME
FROM information_schema.TABLES
WHERE TABLE_SCHEMA=DATABASE()
  AND TABLE_NAME LIKE 'm3extbk_%'
ORDER BY TABLE_NAME
""",
        "remote existing external backup tables",
    )
    warnings.extend(warn)
    return [row["TABLE_NAME"] for row in rows]


def main():
    ARTIFACT_DIR.mkdir(parents=True, exist_ok=True)
    preflight = load_preflight_module()
    refs, remote_ids, warnings = external_reference_groups(preflight)
    if not refs:
        backup_tables = existing_external_backup_tables(warnings)
        result = {
            "generated_at": datetime.now(timezone.utc).isoformat(),
            "status": "skipped",
            "reason": "no active external references",
            "backup_tables": backup_tables,
            "warnings": warnings,
        }
        RESULT_PATH.write_text(json.dumps(result, ensure_ascii=False, indent=2), encoding="utf-8")
        lines = [
            "# External Reference Cleanup Summary",
            "",
            "- Active references: `0`",
            "- Action: `skipped`",
            f"- Existing backup tables: `{len(backup_tables)}`",
            "",
            "## Existing Backup Tables",
        ]
        lines.extend(f"- `{table}`" for table in backup_tables)
        SUMMARY_PATH.write_text("\n".join(lines) + "\n", encoding="utf-8")
        print(json.dumps(result, ensure_ascii=False, indent=2))
        return
    tables = sorted({ref["table"] for ref in refs})
    table_cols, schema_warnings = table_columns(tables)
    warnings.extend(schema_warnings)
    apply_sql, backup_tables = build_cleanup_sql(refs, remote_ids, table_cols)
    stdout, stderr = apply_remote(apply_sql, "remote authorized external reference cleanup")
    refs_after, _, after_warnings = external_reference_groups(preflight)
    warnings.extend(after_warnings)
    if refs_after:
        fail(f"external references remain after cleanup: {json.dumps(refs_after, ensure_ascii=False, indent=2)}")
    result = {
        "generated_at": datetime.now(timezone.utc).isoformat(),
        "status": "applied",
        "reference_groups_before": refs,
        "reference_groups_after": refs_after,
        "backup_tables": backup_tables,
        "apply_stdout": stdout.strip(),
        "warnings": ([stderr] if stderr else []) + warnings,
    }
    RESULT_PATH.write_text(json.dumps(result, ensure_ascii=False, indent=2), encoding="utf-8")
    lines = [
        "# External Reference Cleanup Summary",
        "",
        f"- Generated: `{result['generated_at']}`",
        f"- Reference groups before: `{len(refs)}`",
        f"- Reference groups after: `{len(refs_after)}`",
        f"- Backup tables: `{len(backup_tables)}`",
        "",
        "## Backup Tables",
    ]
    for table, backup in sorted(backup_tables.items()):
        lines.append(f"- `{table}` -> `{backup}`")
    SUMMARY_PATH.write_text("\n".join(lines) + "\n", encoding="utf-8")
    print(json.dumps({
        "result": str(RESULT_PATH),
        "summary": str(SUMMARY_PATH),
        "reference_groups_before": len(refs),
        "reference_groups_after": len(refs_after),
        "backup_tables": backup_tables,
    }, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
