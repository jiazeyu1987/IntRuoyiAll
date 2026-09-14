#!/usr/bin/env python3
"""Sync batch-record report metadata required by the three-tab package."""

from __future__ import annotations

import csv
import datetime as dt
import json
import subprocess
from io import StringIO
from pathlib import Path

TASK_ROOT = Path(__file__).resolve().parents[1]
ARTIFACTS = TASK_ROOT / "artifacts"
RESULT_PATH = ARTIFACTS / "batch-record-report-dependency-sync-result.json"
SUMMARY_PATH = ARTIFACTS / "batch-record-report-dependency-sync-summary.md"
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

REMOTE_MYSQL_APPLY = [
    "ssh",
    "root@172.30.30.58",
    'docker exec -i intruoyi-mysql sh -lc \'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql -uroot --default-character-set=utf8mb4 ruoyi-vue-pro\'',
]


def fail(message: str) -> None:
    raise SystemExit(message)


def run(cmd: list[str], sql: str, label: str, headers: bool = True) -> tuple[list[dict[str, str]], str]:
    proc = subprocess.run(cmd, input=sql, text=True, encoding="utf-8", stdout=subprocess.PIPE, stderr=subprocess.PIPE)
    stderr = proc.stderr.strip()
    if proc.returncode != 0:
        fail(f"{label} failed with exit {proc.returncode}: {stderr[:4000]}")
    if not proc.stdout.strip():
        return [], stderr
    if not headers:
        lines = [line.split("\t") for line in proc.stdout.splitlines()]
        return [{"_": "\t".join(line)} for line in lines], stderr
    reader = csv.DictReader(StringIO(proc.stdout), delimiter="\t")
    return list(reader), stderr


def execute(sql: str, label: str) -> str:
    proc = subprocess.run(
        REMOTE_MYSQL_APPLY,
        input=sql,
        text=True,
        encoding="utf-8",
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
    )
    stderr = proc.stderr.strip()
    if proc.returncode != 0:
        fail(f"{label} failed with exit {proc.returncode}: {stderr[:4000]}")
    return stderr


def mysql_string(value: str | None) -> str:
    if value is None:
        return "NULL"
    encoded = str(value).encode("utf-8").hex()
    return f"CONVERT(UNHEX('{encoded}') USING utf8mb4)"


def base_type(column_type: str) -> str:
    return column_type.lower().split("(", 1)[0]


def sql_literal(value: str | None, column_type: str) -> str:
    if value is None or value == "NULL":
        return "NULL"
    dtype = base_type(column_type)
    if dtype == "bit":
        return "b'1'" if value in {"1", "\x01", "true", "True"} else "b'0'"
    if dtype in {"tinyint", "smallint", "mediumint", "int", "bigint", "decimal", "float", "double", "year"}:
        return "NULL" if value == "" else value
    return mysql_string(value)


def sql_string_in(values: list[str]) -> str:
    if not values:
        return "(NULL)"
    return "(" + ",".join(mysql_string(value) for value in sorted(set(values))) + ")"


def sql_hex_string_in(values: list[str]) -> str:
    if not values:
        return "(NULL)"
    encoded_values = sorted({value.encode("utf-8").hex().upper() for value in values})
    return "(" + ",".join(f"'{value}'" for value in encoded_values) + ")"


def sql_numeric_in(values: list[str | int]) -> str:
    normalized = sorted({str(value) for value in values}, key=lambda item: int(item))
    if not normalized:
        return "(NULL)"
    return "(" + ",".join(normalized) + ")"


def table_columns(cmd: list[str], table: str, label: str) -> tuple[list[dict[str, str]], str]:
    rows, warning = run(cmd, f"SHOW COLUMNS FROM `{table}`;", label)
    return rows, warning


def insertable_columns(table: str) -> tuple[list[dict[str, str]], list[str]]:
    local_cols, local_warning = table_columns(LOCAL_MYSQL, table, f"local schema {table}")
    remote_cols, remote_warning = table_columns(REMOTE_MYSQL, table, f"remote schema {table}")
    warnings = [warning for warning in [local_warning, remote_warning] if warning]
    remote_by_name = {row["Field"]: row for row in remote_cols}
    columns = []
    for row in local_cols:
        name = row["Field"]
        if name not in remote_by_name:
            continue
        extra = (remote_by_name[name].get("Extra") or "").upper()
        if "GENERATED" in extra:
            continue
        columns.append(remote_by_name[name])
    return columns, warnings


def source_rows(table: str, columns: list[dict[str, str]], where_sql: str, order_sql: str) -> tuple[list[dict[str, str]], str]:
    column_sql = ", ".join(f"`{column['Field']}`" for column in columns)
    sql = f"SELECT {column_sql} FROM `{table}` WHERE {where_sql} {order_sql};"
    rows, warning = run(LOCAL_MYSQL, sql, f"local source rows {table}")
    return rows, warning


def values_sql(row: dict[str, str], columns: list[dict[str, str]]) -> str:
    values = [sql_literal(row.get(column["Field"]), column["Type"]) for column in columns]
    return "(" + ",".join(values) + ")"


def insert_sql(table: str, rows: list[dict[str, str]], columns: list[dict[str, str]]) -> str:
    if not rows:
        return ""
    column_sql = ", ".join(f"`{column['Field']}`" for column in columns)
    row_sql = ",\n".join(values_sql(row, columns) for row in rows)
    return f"INSERT INTO `{table}` ({column_sql}) VALUES\n{row_sql};"


def missing_report_ids() -> tuple[list[str], list[str]]:
    sql = """
SELECT r.report_id
FROM (
  SELECT DISTINCT batch_record_report_id AS report_id
  FROM mes_pro_route_flow_process_batch_record
  WHERE tenant_id = 1
    AND deleted = b'0'
    AND batch_record_report_id IS NOT NULL
    AND batch_record_report_id <> ''
) r
LEFT JOIN mes_pro_batch_record_report br
  ON br.report_id = r.report_id
 AND br.tenant_id = 1
 AND br.deleted = b'0'
WHERE br.report_id IS NULL
ORDER BY r.report_id;
"""
    rows, warning = run(REMOTE_MYSQL, sql, "remote missing batch record reports")
    return [row["report_id"] for row in rows], [warning] if warning else []


def scalar_count(cmd: list[str], sql: str, label: str) -> tuple[int, str]:
    rows, warning = run(cmd, sql, label)
    if not rows:
        return 0, warning
    value = next(iter(rows[0].values()))
    return int(value or 0), warning


def main() -> None:
    ARTIFACTS.mkdir(parents=True, exist_ok=True)
    warnings: list[str] = []
    report_ids, initial_warnings = missing_report_ids()
    warnings.extend(initial_warnings)

    if not report_ids:
        summary = "# Batch Record Report Dependency Sync Summary\n\n- Missing active reports: `0`\n- Action: `skipped`\n"
        SUMMARY_PATH.write_text(summary, encoding="utf-8")
        RESULT_PATH.write_text(
            json.dumps(
                {
                    "generated_at": dt.datetime.now(dt.timezone.utc).isoformat(),
                    "status": "skipped",
                    "missing_report_ids": [],
                    "warnings": warnings,
                },
                ensure_ascii=False,
                indent=2,
            ),
            encoding="utf-8",
        )
        return

    report_in = sql_string_in(report_ids)
    report_hex_in = sql_hex_string_in(report_ids)
    definition_rows, definition_warning = run(
        LOCAL_MYSQL,
        f"""
SELECT DISTINCT batch_record_definition_id AS id
  FROM mes_pro_batch_record_report
  WHERE tenant_id = {TENANT_ID}
    AND deleted = b'0'
  AND HEX(report_id) IN {report_hex_in}
  AND batch_record_definition_id IS NOT NULL
ORDER BY id;
""",
        "local referenced batch record definitions",
    )
    version_rows, version_warning = run(
        LOCAL_MYSQL,
        f"""
SELECT DISTINCT batch_record_version_id AS id
  FROM mes_pro_batch_record_report
  WHERE tenant_id = {TENANT_ID}
    AND deleted = b'0'
  AND HEX(report_id) IN {report_hex_in}
  AND batch_record_version_id IS NOT NULL
ORDER BY id;
""",
        "local referenced batch record versions",
    )
    warnings.extend([warning for warning in [definition_warning, version_warning] if warning])
    definition_ids = [row["id"] for row in definition_rows]
    version_ids = [row["id"] for row in version_rows]

    timestamp = dt.datetime.now().strftime("%Y%m%d%H%M%S")
    backup_tables = {
        "mes_pro_batch_record_definition": f"m3brepbk_{timestamp}_def",
        "mes_pro_batch_record_version": f"m3brepbk_{timestamp}_ver",
        "mes_pro_batch_record_report": f"m3brepbk_{timestamp}_report",
    }

    definition_cols, column_warnings = insertable_columns("mes_pro_batch_record_definition")
    version_cols, version_column_warnings = insertable_columns("mes_pro_batch_record_version")
    report_cols, report_column_warnings = insertable_columns("mes_pro_batch_record_report")
    warnings.extend(column_warnings + version_column_warnings + report_column_warnings)

    definition_source, warn = source_rows(
        "mes_pro_batch_record_definition",
        definition_cols,
        f"tenant_id={TENANT_ID} AND deleted=b'0' AND id IN {sql_numeric_in(definition_ids)}",
        "ORDER BY id",
    )
    warnings.extend([warn] if warn else [])
    version_source, warn = source_rows(
        "mes_pro_batch_record_version",
        version_cols,
        f"tenant_id={TENANT_ID} AND deleted=b'0' AND id IN {sql_numeric_in(version_ids)}",
        "ORDER BY id",
    )
    warnings.extend([warn] if warn else [])
    report_source, warn = source_rows(
        "mes_pro_batch_record_report",
        report_cols,
        f"tenant_id={TENANT_ID} AND deleted=b'0' AND HEX(report_id) IN {report_hex_in}",
        "ORDER BY report_id",
    )
    warnings.extend([warn] if warn else [])

    if len(report_source) != len(report_ids):
        fail(f"source report count mismatch: expected {len(report_ids)} got {len(report_source)}")
    if len(definition_source) != len(definition_ids):
        fail(f"source definition count mismatch: expected {len(definition_ids)} got {len(definition_source)}")
    if len(version_source) != len(version_ids):
        fail(f"source version count mismatch: expected {len(version_ids)} got {len(version_source)}")

    apply_sql = [
        "START TRANSACTION;",
        f"CREATE TABLE `{backup_tables['mes_pro_batch_record_definition']}` AS SELECT * FROM `mes_pro_batch_record_definition` WHERE id IN {sql_numeric_in(definition_ids)} OR tenant_id={TENANT_ID} AND id IN {sql_numeric_in(definition_ids)};",
        f"CREATE TABLE `{backup_tables['mes_pro_batch_record_version']}` AS SELECT * FROM `mes_pro_batch_record_version` WHERE id IN {sql_numeric_in(version_ids)} OR tenant_id={TENANT_ID} AND id IN {sql_numeric_in(version_ids)};",
        f"CREATE TABLE `{backup_tables['mes_pro_batch_record_report']}` AS SELECT * FROM `mes_pro_batch_record_report` WHERE HEX(report_id) IN {report_hex_in} OR id IN {sql_numeric_in([row['id'] for row in report_source])};",
    ]
    for table, rows_to_insert, columns in [
        ("mes_pro_batch_record_definition", definition_source, definition_cols),
        ("mes_pro_batch_record_version", version_source, version_cols),
        ("mes_pro_batch_record_report", report_source, report_cols),
    ]:
        if rows_to_insert:
            apply_sql.append(insert_sql(table, rows_to_insert, columns))
    apply_sql.extend(
        [
            "COMMIT;",
        ]
    )
    apply_warning = execute("\n".join(apply_sql), "remote batch record report dependency sync")
    if apply_warning:
        warnings.append(apply_warning)

    remaining_report_ids, remaining_warnings = missing_report_ids()
    warnings.extend(remaining_warnings)
    definition_count, warn = scalar_count(
        REMOTE_MYSQL,
        f"SELECT COUNT(*) AS cnt FROM mes_pro_batch_record_definition WHERE tenant_id={TENANT_ID} AND deleted=b'0' AND id IN {sql_numeric_in(definition_ids)};",
        "remote definition postcheck",
    )
    warnings.extend([warn] if warn else [])
    version_count, warn = scalar_count(
        REMOTE_MYSQL,
        f"SELECT COUNT(*) AS cnt FROM mes_pro_batch_record_version WHERE tenant_id={TENANT_ID} AND deleted=b'0' AND id IN {sql_numeric_in(version_ids)};",
        "remote version postcheck",
    )
    warnings.extend([warn] if warn else [])
    if remaining_report_ids:
        fail(f"postcheck failed: remaining missing reports {remaining_report_ids}")
    if definition_count != len(definition_ids):
        fail(f"postcheck failed: definition count {definition_count} != {len(definition_ids)}")
    if version_count != len(version_ids):
        fail(f"postcheck failed: version count {version_count} != {len(version_ids)}")

    result = {
        "generated_at": dt.datetime.now(dt.timezone.utc).isoformat(),
        "status": "applied",
        "backup_tables": backup_tables,
        "inserted": {
            "mes_pro_batch_record_definition": len(definition_source),
            "mes_pro_batch_record_version": len(version_source),
            "mes_pro_batch_record_report": len(report_source),
        },
        "report_ids": report_ids,
        "definition_ids": definition_ids,
        "version_ids": version_ids,
        "postcheck": {
            "remaining_missing_report_ids": remaining_report_ids,
            "definition_count": definition_count,
            "version_count": version_count,
        },
        "warnings": [warning for warning in warnings if warning],
    }
    RESULT_PATH.write_text(json.dumps(result, ensure_ascii=False, indent=2), encoding="utf-8")
    SUMMARY_PATH.write_text(
        "\n".join(
            [
                "# Batch Record Report Dependency Sync Summary",
                "",
                f"- Missing active reports before sync: `{len(report_ids)}`",
                f"- Definitions inserted: `{len(definition_source)}`",
                f"- Versions inserted: `{len(version_source)}`",
                f"- Reports inserted: `{len(report_source)}`",
                f"- Missing active reports after sync: `{len(remaining_report_ids)}`",
                "",
                "## Backup Tables",
                *[f"- `{name}`" for name in backup_tables.values()],
                "",
            ]
        ),
        encoding="utf-8",
    )


if __name__ == "__main__":
    main()
