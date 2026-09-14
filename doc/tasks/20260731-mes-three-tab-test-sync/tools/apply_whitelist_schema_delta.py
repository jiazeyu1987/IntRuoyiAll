import json
import subprocess
import sys
from datetime import datetime, timezone
from pathlib import Path


ROOT = Path(__file__).resolve().parents[4]
ARTIFACT_DIR = ROOT / "doc" / "tasks" / "20260731-mes-three-tab-test-sync" / "artifacts"
RESULT_PATH = ARTIFACT_DIR / "whitelist-schema-delta-result.json"

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

DELTAS = {
    "mes_pro_route_flow_process_batch_record": {
        "form_definition_id": "ADD COLUMN `form_definition_id` BIGINT NULL",
    },
    "mes_pro_schedule_order": {
        "source_work_order_id": "ADD COLUMN `source_work_order_id` BIGINT NULL",
        "source_work_order_code": "ADD COLUMN `source_work_order_code` VARCHAR(64) NULL",
        "source_order_code": "ADD COLUMN `source_order_code` VARCHAR(64) NULL",
        "planned_quantity": "ADD COLUMN `planned_quantity` DECIMAL(24,6) NULL",
        "promised_delivery_date": "ADD COLUMN `promised_delivery_date` DATETIME NULL",
        "priority": "ADD COLUMN `priority` TINYINT NOT NULL DEFAULT 5",
        "active_flag": "ADD COLUMN `active_flag` TINYINT NOT NULL DEFAULT 1",
        "scheduled_quantity": "ADD COLUMN `scheduled_quantity` DECIMAL(24,6) NOT NULL DEFAULT 0.000000",
        "reported_quantity": "ADD COLUMN `reported_quantity` DECIMAL(24,6) NOT NULL DEFAULT 0.000000",
        "product_code": "ADD COLUMN `product_code` VARCHAR(64) NULL",
        "product_name": "ADD COLUMN `product_name` VARCHAR(255) NULL",
        "product_specification": "ADD COLUMN `product_specification` VARCHAR(512) NULL",
        "route_code": "ADD COLUMN `route_code` VARCHAR(64) NULL",
        "route_name": "ADD COLUMN `route_name` VARCHAR(255) NULL",
    },
    "mes_pro_schedule_order_process": {
        "source_work_order_id": "ADD COLUMN `source_work_order_id` BIGINT NULL",
        "route_id": "ADD COLUMN `route_id` BIGINT NULL",
        "workstation_id": "ADD COLUMN `workstation_id` BIGINT NULL",
        "workstation_code": "ADD COLUMN `workstation_code` VARCHAR(64) NULL",
        "workstation_name": "ADD COLUMN `workstation_name` VARCHAR(255) NULL",
        "previous_next_relation": "ADD COLUMN `previous_next_relation` VARCHAR(64) NULL",
        "scheduling_enabled": "ADD COLUMN `scheduling_enabled` BIT(1) NOT NULL DEFAULT b'1'",
        "status": "ADD COLUMN `status` TINYINT NOT NULL DEFAULT 0",
        "standard_hourly_capacity": "ADD COLUMN `standard_hourly_capacity` DECIMAL(24,6) NULL",
        "standard_shift_capacity": "ADD COLUMN `standard_shift_capacity` DECIMAL(24,6) NULL",
        "resource_quantity": "ADD COLUMN `resource_quantity` DECIMAL(24,6) NULL",
        "capacity_snapshot": "ADD COLUMN `capacity_snapshot` JSON NULL",
    },
}


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


def existing_columns():
    table_names = ",".join("'" + table + "'" for table in DELTAS)
    stdout, stderr = run(
        REMOTE_MYSQL,
        f"""
SELECT TABLE_NAME, COLUMN_NAME
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA=DATABASE()
  AND TABLE_NAME IN ({table_names})
ORDER BY TABLE_NAME, ORDINAL_POSITION
""",
        "remote schema delta scan",
    )
    lines = [line for line in stdout.splitlines() if line.strip()]
    headers = lines[0].split("\t") if lines else []
    columns = {}
    for line in lines[1:]:
        values = line.split("\t")
        row = {header: values[idx] if idx < len(values) else "" for idx, header in enumerate(headers)}
        columns.setdefault(row["TABLE_NAME"], set()).add(row["COLUMN_NAME"])
    return columns, stderr


def main():
    ARTIFACT_DIR.mkdir(parents=True, exist_ok=True)
    columns, scan_stderr = existing_columns()
    statements = []
    applied = []
    for table, deltas in DELTAS.items():
        existing = columns.get(table, set())
        for column, clause in deltas.items():
            if column not in existing:
                statements.append(f"ALTER TABLE `{table}` {clause};")
                applied.append({"table": table, "column": column})
    apply_stdout = ""
    apply_stderr = ""
    if statements:
        apply_stdout, apply_stderr = run(REMOTE_APPLY, "SET NAMES utf8mb4;\n" + "\n".join(statements), "remote whitelist schema delta apply")
    result = {
        "generated_at": datetime.now(timezone.utc).isoformat(),
        "applied": applied,
        "statement_count": len(statements),
        "apply_stdout": apply_stdout.strip(),
        "warnings": [value for value in [scan_stderr, apply_stderr] if value],
    }
    RESULT_PATH.write_text(json.dumps(result, ensure_ascii=False, indent=2), encoding="utf-8")
    print(json.dumps(result, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
