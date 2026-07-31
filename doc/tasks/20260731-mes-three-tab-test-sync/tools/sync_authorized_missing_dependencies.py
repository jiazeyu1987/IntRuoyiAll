import json
import subprocess
import sys
from datetime import datetime, timezone
from pathlib import Path


ROOT = Path(__file__).resolve().parents[4]
TASK_DIR = ROOT / "doc" / "tasks" / "20260731-mes-three-tab-test-sync"
ARTIFACT_DIR = TASK_DIR / "artifacts"
TENANT_ID = 1

TABLE_IDS = {
    "mes_md_item": ["924005"],
    "system_users": ["910269"],
    "mes_pro_work_order": [
        "925473",
        "925477",
        "925483",
        "925671",
        "925675",
        "925685",
        "925689",
        "925693",
        "925716",
        "925721",
        "925724",
        "925729",
        "925732",
        "925809",
        "925813",
        "925816",
        "925820",
        "925825",
        "925827",
        "925828",
        "925829",
        "925830",
        "925843",
        "925850",
        "925854",
        "925858",
        "925864",
        "925865",
        "925866",
        "925867",
        "925868",
        "925874",
        "925877",
    ],
}

LOCAL_MYSQL = [
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
    'docker exec -i intruoyi-mysql sh -lc \'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql -uroot --default-character-set=utf8mb4 --batch --raw --skip-column-names ruoyi-vue-pro\'',
]


def fail(message):
    print(message, file=sys.stderr)
    raise SystemExit(1)


def id_list(ids):
    return "(" + ",".join(str(int(value)) for value in ids) + ")"


def run(cmd, sql=None, stdin_bytes=None, label="command", timeout=120):
    proc = subprocess.run(
        cmd,
        input=stdin_bytes if stdin_bytes is not None else sql,
        text=stdin_bytes is None,
        encoding=None if stdin_bytes is not None else "utf-8",
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
        timeout=timeout,
    )
    if proc.returncode != 0:
        stderr = proc.stderr.decode("utf-8", errors="replace") if isinstance(proc.stderr, bytes) else proc.stderr
        fail(f"{label} failed with exit {proc.returncode}: {stderr[:2000]}")
    stdout = proc.stdout.decode("utf-8", errors="replace") if isinstance(proc.stdout, bytes) else proc.stdout
    stderr = proc.stderr.decode("utf-8", errors="replace") if isinstance(proc.stderr, bytes) else proc.stderr
    return stdout, stderr


def mysql_rows(cmd, sql, label):
    stdout, stderr = run(cmd, sql=sql, label=label)
    rows = []
    for line in stdout.splitlines():
        if line.strip():
            rows.append(line.split("\t"))
    return rows, stderr


def count_rows(cmd, table, ids, label):
    sql = f"SELECT COUNT(*) FROM {table} WHERE tenant_id={TENANT_ID} AND id IN {id_list(ids)};"
    rows, stderr = mysql_rows(cmd, sql, label)
    return int(rows[0][0]) if rows else 0, stderr


def dump_table(table, ids):
    where = f"tenant_id={TENANT_ID} AND id IN {id_list(ids)}"
    cmd = [
        "docker",
        "exec",
        "int-ruoyi-mysql",
        "sh",
        "-lc",
        (
            'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysqldump -uroot '
            "--default-character-set=utf8mb4 --no-create-info --complete-insert "
            "--skip-triggers --skip-add-locks --skip-disable-keys --compact "
            f"ruoyi-vue-pro {table} --where=\"{where}\""
        ),
    ]
    proc = subprocess.run(cmd, stdout=subprocess.PIPE, stderr=subprocess.PIPE, timeout=120)
    if proc.returncode != 0:
        fail(f"source dump failed for {table}: {proc.stderr.decode('utf-8', errors='replace')[:2000]}")
    return proc.stdout


def remote_apply(sql_bytes):
    cmd = [
        "ssh",
        "root@172.30.30.58",
        'docker exec -i intruoyi-mysql sh -lc \'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql -uroot --default-character-set=utf8mb4 ruoyi-vue-pro\'',
    ]
    return run(cmd, stdin_bytes=sql_bytes, label="remote apply authorized dependency SQL", timeout=180)


def main():
    ARTIFACT_DIR.mkdir(parents=True, exist_ok=True)
    timestamp = datetime.now(timezone.utc).strftime("%Y%m%d%H%M%S")
    backup_tables = {
        table: f"mes_three_tab_dep_backup_{timestamp}_{table}"
        for table in TABLE_IDS
    }
    result = {
        "generated_at": datetime.now(timezone.utc).isoformat(),
        "target": "172.30.30.58 / ruoyi-vue-pro / tenant_id=1",
        "scope": {
            table: {"ids": ids, "expected_insert_count": len(ids)}
            for table, ids in TABLE_IDS.items()
        },
        "backup_tables": backup_tables,
        "precheck": {},
        "postcheck": {},
        "warnings": [],
    }

    for table, ids in TABLE_IDS.items():
        source_count, source_stderr = count_rows(LOCAL_MYSQL, table, ids, f"local source count {table}")
        target_count, target_stderr = count_rows(REMOTE_MYSQL, table, ids, f"remote target count {table}")
        if source_stderr:
            result["warnings"].append({"stage": f"source_count:{table}", "stderr": source_stderr})
        if target_stderr:
            result["warnings"].append({"stage": f"target_count:{table}", "stderr": target_stderr})
        result["precheck"][table] = {
            "source_count": source_count,
            "target_existing_count": target_count,
        }
        if source_count != len(ids):
            fail(f"source count mismatch for {table}: expected {len(ids)}, got {source_count}")
        if target_count != 0:
            fail(f"target already has rows for {table}: expected 0, got {target_count}")

    source_product_sql = """
SELECT DISTINCT product_id
FROM mes_pro_work_order
WHERE tenant_id=1
  AND id IN {work_order_ids}
  AND product_id IS NOT NULL
  AND product_id <> 0
ORDER BY product_id;
""".format(work_order_ids=id_list(TABLE_IDS["mes_pro_work_order"]))
    source_product_rows, _ = mysql_rows(LOCAL_MYSQL, source_product_sql, "local work order product ids")
    source_product_ids = [row[0] for row in source_product_rows]
    target_product_sql = (
        "SELECT COUNT(*) FROM mes_md_item "
        f"WHERE tenant_id=1 AND deleted=0 AND id IN {id_list(source_product_ids)};"
    )
    target_product_count, _ = mysql_rows(REMOTE_MYSQL, target_product_sql, "remote work order product dependency")
    if len(source_product_ids) != int(target_product_count[0][0]):
        fail(
            "work order product dependencies are not aligned between source and target; "
            f"source={len(source_product_ids)}, target={target_product_count[0][0]}"
        )
    result["precheck"]["work_order_product_dependency_ids"] = source_product_ids
    result["precheck"]["work_order_product_dependency_count"] = int(target_product_count[0][0])

    backup_sql_parts = []
    for table, ids in TABLE_IDS.items():
        backup = backup_tables[table]
        backup_sql_parts.append(f"DROP TABLE IF EXISTS `{backup}`;")
        backup_sql_parts.append(f"CREATE TABLE `{backup}` LIKE `{table}`;")
        backup_sql_parts.append(
            f"INSERT INTO `{backup}` SELECT * FROM `{table}` "
            f"WHERE tenant_id={TENANT_ID} AND id IN {id_list(ids)};"
        )
    backup_sql_parts.append("SELECT 'BACKUP_READY';")
    _, backup_stderr = run(REMOTE_MYSQL, sql="\n".join(backup_sql_parts), label="remote backup authorized dependency rows")
    if backup_stderr:
        result["warnings"].append({"stage": "backup", "stderr": backup_stderr})

    dump_bytes = b"\n".join(
        dump_table(table, ids)
        for table, ids in TABLE_IDS.items()
    )
    apply_sql = (
        b"SET NAMES utf8mb4;\n"
        b"START TRANSACTION;\n"
        + dump_bytes
        + b"\nCOMMIT;\n"
    )
    _, apply_stderr = remote_apply(apply_sql)
    if apply_stderr:
        result["warnings"].append({"stage": "apply", "stderr": apply_stderr})

    for table, ids in TABLE_IDS.items():
        target_count, target_stderr = count_rows(REMOTE_MYSQL, table, ids, f"remote post count {table}")
        if target_stderr:
            result["warnings"].append({"stage": f"post_count:{table}", "stderr": target_stderr})
        result["postcheck"][table] = {"target_count": target_count}
        if target_count != len(ids):
            fail(f"postcheck count mismatch for {table}: expected {len(ids)}, got {target_count}")

    result_path = ARTIFACT_DIR / "authorized-missing-dependency-sync-result.json"
    result_path.write_text(json.dumps(result, ensure_ascii=False, indent=2), encoding="utf-8")
    print(json.dumps({
        "result": str(result_path),
        "inserted_counts": {table: len(ids) for table, ids in TABLE_IDS.items()},
        "backup_tables": backup_tables,
    }, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
