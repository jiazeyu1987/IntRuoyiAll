import json
import subprocess
import sys
from datetime import datetime, timezone
from pathlib import Path


ROOT = Path(__file__).resolve().parents[4]
ARTIFACT_DIR = ROOT / "doc" / "tasks" / "20260731-mes-three-tab-test-sync" / "artifacts"
RESULT_PATH = ARTIFACT_DIR / "whitelist-index-delta-result.json"
MAIN_SYNC_RESULT_PATH = ARTIFACT_DIR / "three-tab-whitelist-sync-result.json"

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

DROP_INDEXES = [
    ("mes_pro_route_process_flow_edge", "uk_mes_route_process_flow_target"),
    ("mes_pro_route_schedule_config", "uk_mes_pro_route_schedule_config_active_process"),
]

ADD_INDEXES_AFTER_MAIN_SYNC = [
    (
        "mes_pro_route_flow_process_batch_record",
        "uk_mes_pro_route_flow_process_report_sort",
        "UNIQUE KEY `uk_mes_pro_route_flow_process_report_sort` (`tenant_id`,`route_process_id`,`use_type`,`report_sort`,`deleted`)",
    ),
    (
        "mes_pro_route_schedule_config",
        "uk_mes_pro_route_schedule_config_item_process",
        "UNIQUE KEY `uk_mes_pro_route_schedule_config_item_process` (`tenant_id`,`route_version_id`,`item_id`,`route_process_id`,`deleted`)",
    ),
]


def fail(message):
    print(message, file=sys.stderr)
    raise SystemExit(1)


def run(cmd, sql, label):
    proc = subprocess.run(
        cmd,
        input=sql,
        text=True,
        encoding="utf-8",
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
        timeout=180,
    )
    if proc.returncode != 0:
        fail(f"{label} failed with exit {proc.returncode}: {proc.stderr[:3000]}")
    return proc.stdout, proc.stderr.strip()


def existing_unique_indexes():
    tables = sorted({table for table, _ in DROP_INDEXES} | {table for table, _, _ in ADD_INDEXES_AFTER_MAIN_SYNC})
    quoted = ",".join("'" + table + "'" for table in tables)
    stdout, stderr = run(
        REMOTE_MYSQL,
        f"""
SELECT TABLE_NAME, INDEX_NAME
FROM information_schema.STATISTICS
WHERE TABLE_SCHEMA=DATABASE()
  AND TABLE_NAME IN ({quoted})
  AND NON_UNIQUE=0
GROUP BY TABLE_NAME, INDEX_NAME
ORDER BY TABLE_NAME, INDEX_NAME
""",
        "remote unique index scan",
    )
    lines = [line for line in stdout.splitlines() if line.strip()]
    headers = lines[0].split("\t") if lines else []
    existing = set()
    for line in lines[1:]:
        values = line.split("\t")
        row = {header: values[idx] if idx < len(values) else "" for idx, header in enumerate(headers)}
        existing.add((row["TABLE_NAME"], row["INDEX_NAME"]))
    return existing, stderr


def main():
    ARTIFACT_DIR.mkdir(parents=True, exist_ok=True)
    existing, scan_stderr = existing_unique_indexes()
    statements = []
    applied = []
    for table, index_name in DROP_INDEXES:
        if (table, index_name) in existing:
            statements.append(f"ALTER TABLE `{table}` DROP INDEX `{index_name}`;")
            applied.append({"action": "drop", "table": table, "index": index_name})
    deferred_adds = [
        {"table": table, "index": index_name, "clause": clause}
        for table, index_name, clause in ADD_INDEXES_AFTER_MAIN_SYNC
        if (table, index_name) not in existing
    ]
    if MAIN_SYNC_RESULT_PATH.exists():
        for item in deferred_adds:
            statements.append(f"ALTER TABLE `{item['table']}` ADD {item['clause']};")
            applied.append({"action": "add", "table": item["table"], "index": item["index"]})
        deferred_adds = []
    stdout = ""
    apply_stderr = ""
    if statements:
        stdout, apply_stderr = run(REMOTE_APPLY, "SET NAMES utf8mb4;\n" + "\n".join(statements), "remote whitelist index delta")
    result = {
        "generated_at": datetime.now(timezone.utc).isoformat(),
        "applied": applied,
        "deferred_adds_after_main_sync": deferred_adds,
        "statement_count": len(statements),
        "apply_stdout": stdout.strip(),
        "warnings": [value for value in [scan_stderr, apply_stderr] if value],
    }
    RESULT_PATH.write_text(json.dumps(result, ensure_ascii=False, indent=2), encoding="utf-8")
    print(json.dumps(result, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
