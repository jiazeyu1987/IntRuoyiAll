"""One-time, explicitly local process renumbering. Default: verify then roll back.

Run with --apply to commit. A JSON preimage and ID-based code mapping are saved
before any update under target/process-gx-renumber-<timestamp>. Historical
snapshots/import payloads are deliberately retained. Requires pymysql and the
local int-ruoyi-mysql container published on 127.0.0.1:23306.
"""

import argparse
from collections import defaultdict
from datetime import datetime
import hashlib
import json
from pathlib import Path
import subprocess

import pymysql


REFERENCES = {
    "mes_dv_machinery_process": ("process_id", "process_code"),
    "mes_pro_edhr_traveler_template": ("applicable_process_id", "applicable_process_code"),
}
HISTORY = [
    "mes_pro_route_process",
    "mes_pro_route_version",
    "mes_pro_schedule_order_process",
    "mes_pro_feedback_import_record",
    "mes_pro_feedback_surplus_pool",
    "mes_pro_edhr_batch_execution_task",
    "mes_pro_edhr_traveler_instance",
    "mes_pro_process_pool_active_order_process_snapshot",
    "mes_pro_mes_process_catalog",
]


def encode(value):
    return json.dumps(value, ensure_ascii=False, sort_keys=True, default=str).encode("utf-8")


def digest(value):
    return hashlib.sha256(encode(value)).hexdigest()


def require(condition, message):
    if not condition:
        raise RuntimeError(message)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--apply", action="store_true")
    args = parser.parse_args()
    ports = subprocess.run(
        ["docker", "port", "int-ruoyi-mysql", "3306/tcp"],
        capture_output=True, text=True, check=True,
    ).stdout
    require("0.0.0.0:23306" in ports or "127.0.0.1:23306" in ports,
            "Local MySQL container is not published on the authorized port")
    password = subprocess.run(
        ["docker", "exec", "int-ruoyi-mysql", "printenv", "MYSQL_ROOT_PASSWORD"],
        capture_output=True, text=True, check=True,
    ).stdout.rstrip("\r\n")
    connection = pymysql.connect(
        host="127.0.0.1", port=23306, user="root", password=password,
        database="ruoyi-vue-pro", charset="utf8mb4", autocommit=False,
        cursorclass=pymysql.cursors.DictCursor,
    )
    output = Path(__file__).resolve().parents[1] / "target" / (
        "process-gx-renumber-" + datetime.now().strftime("%Y%m%d-%H%M%S-%f"))
    try:
        with connection.cursor() as cursor:
            cursor.execute("SET SESSION innodb_lock_wait_timeout=15")
            cursor.execute("SET TRANSACTION ISOLATION LEVEL SERIALIZABLE")
            connection.begin()

            def read(table, lock=False):
                cursor.execute(f"SELECT * FROM `{table}` ORDER BY id" +
                               (" FOR UPDATE" if lock else ""))
                return list(cursor.fetchall())

            processes = read("mes_pro_process", True)
            references = {table: read(table, True) for table in REFERENCES}
            # This local schema does not have the newer code-only DHR bindings.
            # Refuse to run unchanged if that prerequisite changes.
            cursor.execute("SELECT COUNT(*) AS n FROM information_schema.TABLES "
                           "WHERE TABLE_SCHEMA=DATABASE() "
                           "AND TABLE_NAME='mes_pro_edhr_dhr_template_binding'")
            require(cursor.fetchone()["n"] == 0,
                    "DHR code-only binding table exists; analyze and migrate its references first")
            history_before = {table: digest(read(table)) for table in HISTORY}
            counters = defaultdict(int)
            mapping = []
            for row in sorted(processes, key=lambda r: (r["tenant_id"], int.from_bytes(r["deleted"], "big")
                                                       if isinstance(r["deleted"], bytes) else int(r["deleted"]), r["id"])):
                tenant = row["tenant_id"]
                counters[tenant] += 1
                mapping.append({"id": row["id"], "tenant_id": tenant,
                                "old_code": row["code"], "new_code": f"GX-{counters[tenant]:04d}"})
            by_id = {(row["tenant_id"], row["id"]): row for row in mapping}
            output.mkdir(parents=True)
            backup = {"database": "127.0.0.1:23306/ruoyi-vue-pro",
                      "processes": processes, "references": references,
                      "mapping": mapping, "history_sha256": history_before}
            backup_path = output / "before.json"
            backup_path.write_bytes(encode(backup))
            require(backup_path.read_bytes() == encode(backup), "Backup read-back failed")

            # Vacate GX codes first to avoid transient collisions during swaps.
            temporary = {row["id"]: f"GXRENUMBER20261021-{row['id']}" for row in mapping}
            require(not any(row["code"] in temporary.values() for row in processes),
                    "Temporary code namespace is occupied")
            for row in mapping:
                cursor.execute("UPDATE mes_pro_process SET code=%s,update_time=update_time "
                               "WHERE id=%s AND tenant_id=%s",
                               (temporary[row["id"]], row["id"], row["tenant_id"]))
            for row in mapping:
                cursor.execute("UPDATE mes_pro_process SET code=%s,update_time=update_time "
                               "WHERE id=%s AND tenant_id=%s",
                               (row["new_code"], row["id"], row["tenant_id"]))

            updated = {}
            for table, (id_column, code_column) in REFERENCES.items():
                expected = []
                updated[table] = 0
                for old in references[table]:
                    row = dict(old)
                    target = by_id.get((row["tenant_id"], row[id_column]))
                    if target is not None and row[code_column] != target["new_code"]:
                        cursor.execute(f"UPDATE `{table}` SET `{code_column}`=%s,update_time=update_time "
                                       "WHERE id=%s AND tenant_id=%s",
                                       (target["new_code"], row["id"], row["tenant_id"]))
                        row[code_column] = target["new_code"]
                        updated[table] += 1
                    expected.append(row)
                require(read(table) == expected, f"Reference verification failed: {table}")

            expected = []
            for old in processes:
                row = dict(old)
                row["code"] = by_id[(row["tenant_id"], row["id"])]["new_code"]
                row["gx_unique_code"] = row["code"]
                expected.append(row)
            require(read("mes_pro_process") == expected, "Process fields changed beyond codes")
            require(history_before == {table: digest(read(table)) for table in HISTORY},
                    "Historical or route data changed")
            if args.apply:
                connection.commit()
            else:
                connection.rollback()
            report = {"committed": args.apply, "process_count": len(processes),
                      "tenant_counts": dict(counters), "references_updated": updated,
                      "history_tables_verified": len(HISTORY), "backup": str(backup_path)}
            (output / "result.json").write_bytes(encode(report))
            print(json.dumps(report, ensure_ascii=False))
    except BaseException:
        connection.rollback()
        raise
    finally:
        connection.close()


if __name__ == "__main__":
    main()
