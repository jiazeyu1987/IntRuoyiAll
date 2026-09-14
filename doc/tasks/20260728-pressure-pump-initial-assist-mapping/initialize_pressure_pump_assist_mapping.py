import argparse
import base64
import json
import math
import pathlib
import subprocess
import sys
from datetime import datetime


TASK_DIR = pathlib.Path(__file__).resolve().parent
BACKUP_DIR = TASK_DIR / "output"
TENANT_ID = 1
BATCH_RECORD_VERSION_ID = 130
GRID_COLUMNS = 8
FILLER_USER_IDS = [795, 810]
SIGNATURE_ACTION_TYPES = {"FORM_REVIEW", "SUBMIT", "APPROVE"}


def run_mysql_query(sql):
    command = [
        "docker",
        "exec",
        "-i",
        "int-ruoyi-mysql",
        "sh",
        "-lc",
        'mysql -uroot -p"$MYSQL_ROOT_PASSWORD" -D ruoyi-vue-pro '
        "--default-character-set=utf8mb4 -N -B -r",
    ]
    completed = subprocess.run(
        command,
        input=sql,
        text=True,
        capture_output=True,
        encoding="utf-8",
    )
    if completed.returncode != 0:
        sys.stderr.write(completed.stderr)
        raise SystemExit(completed.returncode)
    return completed.stdout


def run_mysql_execute(sql):
    command = [
        "docker",
        "exec",
        "-i",
        "int-ruoyi-mysql",
        "sh",
        "-lc",
        'mysql -uroot -p"$MYSQL_ROOT_PASSWORD" -D ruoyi-vue-pro '
        "--default-character-set=utf8mb4 --binary-mode",
    ]
    completed = subprocess.run(
        command,
        input=sql,
        text=True,
        capture_output=True,
        encoding="utf-8",
    )
    if completed.returncode != 0:
        sys.stderr.write(completed.stderr)
        raise SystemExit(completed.returncode)
    return completed.stdout


def sql_string(value):
    return "'" + str(value).replace("\\", "\\\\").replace("'", "''") + "'"


def load_reports():
    sql = f"""
SELECT
  r.id,
  r.report_id,
  r.report_code,
  r.batch_record_name,
  r.product_name,
  r.table_title,
  r.source_table_index,
  r.batch_record_definition_id,
  r.batch_record_version_id,
  REPLACE(TO_BASE64(j.json_str), CHAR(10), '')
FROM mes_pro_batch_record_report r
JOIN jimu_report j ON j.id = r.report_id
WHERE r.tenant_id = {TENANT_ID}
  AND r.batch_record_version_id = {BATCH_RECORD_VERSION_ID}
  AND r.form_slot_type = 'MAIN'
  AND r.deleted = 0
ORDER BY r.source_table_index;
"""
    rows = []
    for line in run_mysql_query(sql).splitlines():
        parts = line.split("\t")
        if len(parts) != 10:
            raise RuntimeError(f"unexpected report row shape: {line[:200]}")
        rows.append(
            {
                "id": int(parts[0]),
                "report_id": parts[1],
                "report_code": parts[2],
                "batch_record_name": parts[3],
                "product_name": parts[4],
                "table_title": parts[5],
                "source_table_index": int(parts[6]),
                "definition_id": int(parts[7]) if parts[7] != "NULL" else None,
                "version_id": int(parts[8]),
                "root": json.loads(base64.b64decode(parts[9]).decode("utf-8")),
            }
        )
    if len(rows) != 15:
        raise RuntimeError(f"expected 15 pressure pump reports, got {len(rows)}")
    return rows


def iter_cells(root):
    rows = root.get("rows")
    if not isinstance(rows, dict):
        return
    for row_key, row in rows.items():
        try:
            row_index = int(row_key)
        except ValueError:
            continue
        if not isinstance(row, dict):
            continue
        cells = row.get("cells")
        if not isinstance(cells, dict):
            continue
        for column_key, cell in cells.items():
            try:
                column_index = int(column_key)
            except ValueError:
                continue
            if isinstance(cell, dict):
                yield row_index, column_index, cell


def has_signature_marker(cell):
    signature = cell.get("edhrSignature") if isinstance(cell, dict) else None
    return (
        isinstance(signature, dict)
        and signature.get("enabled") is True
        and signature.get("actionType") in SIGNATURE_ACTION_TYPES
    )


def has_fill_form(cell):
    fill_form = cell.get("fillForm") if isinstance(cell, dict) else None
    return isinstance(fill_form, dict) and bool(str(fill_form.get("field") or "").strip())


def has_reviewed_rule(cell):
    rule = cell.get("edhrCellRule") if isinstance(cell, dict) else None
    return isinstance(rule, dict) and rule.get("reviewed") is True


def is_assist_fillable(cell):
    return has_fill_form(cell) or has_signature_marker(cell) or has_reviewed_rule(cell)


def cell_label(cell):
    rule = cell.get("edhrCellRule") if isinstance(cell, dict) else None
    if isinstance(rule, dict) and str(rule.get("label") or "").strip():
        return str(rule.get("label")).strip()
    fill_form = cell.get("fillForm") if isinstance(cell, dict) else None
    if isinstance(fill_form, dict) and str(fill_form.get("title") or "").strip():
        return str(fill_form.get("title")).strip()
    signature = cell.get("edhrSignature") if isinstance(cell, dict) else None
    if isinstance(signature, dict) and str(signature.get("label") or "").strip():
        return str(signature.get("label")).strip()
    return ""


def collect_report_cells(report):
    fillable = []
    signatures = []
    for row_index, column_index, cell in iter_cells(report["root"]):
        item = {
            "rowIndex": row_index,
            "columnIndex": column_index,
            "label": cell_label(cell),
        }
        if has_signature_marker(cell):
            signatures.append(item)
        if is_assist_fillable(cell):
            fillable.append(item)
    fillable.sort(key=lambda item: (item["rowIndex"], item["columnIndex"]))
    signatures.sort(key=lambda item: (item["rowIndex"], item["columnIndex"]))
    return fillable, signatures


def choose_user_for_cell(cell, signatures, user_ids):
    if not signatures:
        return user_ids[0]
    best_index = 0
    best_distance = None
    for index, signature in enumerate(signatures):
        distance = abs(cell["rowIndex"] - signature["rowIndex"]) * 1000 + abs(
            cell["columnIndex"] - signature["columnIndex"]
        )
        if best_distance is None or distance < best_distance:
            best_distance = distance
            best_index = index
    return user_ids[best_index]


def build_initialization(report):
    fillable, signatures = collect_report_cells(report)
    filler_count = max(1, len(signatures))
    if filler_count > len(FILLER_USER_IDS):
        raise RuntimeError(
            f"report {report['report_id']} needs {filler_count} filler users, "
            f"only {len(FILLER_USER_IDS)} configured"
        )
    user_ids = FILLER_USER_IDS[:filler_count]
    grouped = {user_id: [] for user_id in user_ids}
    for cell in fillable:
        grouped[choose_user_for_cell(cell, signatures, user_ids)].append(cell)

    assist_rows = []
    assignments = []
    sort_index = 1
    for user_id in user_ids:
        user_cells = grouped[user_id]
        if not user_cells:
            raise RuntimeError(f"report {report['report_id']} generated empty filler group {user_id}")
        for index, cell in enumerate(user_cells, start=1):
            grid_row = math.ceil(index / GRID_COLUMNS)
            grid_col = ((index - 1) % GRID_COLUMNS) + 1
            row_key = f"ASSIST_GRID_U{user_id}_R{grid_row}_C{grid_col}"
            description_label = cell["label"] or f"R{cell['rowIndex']}C{cell['columnIndex']}"
            assist_rows.append(
                {
                    "rowKey": row_key,
                    "description": f"自动映射：{description_label}",
                    "sort": sort_index,
                    "fields": [
                        {
                            "rowIndex": cell["rowIndex"],
                            "columnIndex": cell["columnIndex"],
                        }
                    ],
                }
            )
            assignments.append(
                {
                    "scopeKey": row_key,
                    "candidateSourceType": "USERS",
                    "candidateSourceIds": str(user_id),
                    "completionPolicy": "ANY_ONE",
                    "dueMinutes": 2147483647,
                    "enabled": 1,
                    "fillableScopeJson": json.dumps(
                        {
                            "schemaVersion": 2,
                            "cells": [
                                {
                                    "sourceTableIndex": 0,
                                    "rowIndex": cell["rowIndex"],
                                    "columnIndex": cell["columnIndex"],
                                }
                            ],
                        },
                        ensure_ascii=False,
                        separators=(",", ":"),
                    ),
                }
            )
            sort_index += 1
    return {
        "fillableCount": len(fillable),
        "signatureCount": len(signatures),
        "fillerCount": filler_count,
        "assistRows": assist_rows,
        "assignments": assignments,
    }


def load_assignment_counts():
    sql = f"""
SELECT batch_record_report_id,
       SUM(CASE WHEN rule_type = 'FILL' AND scope_key <> 'ALL' THEN 1 ELSE 0 END),
       COUNT(*)
FROM mes_pro_edhr_process_form_permission_rule
WHERE tenant_id = {TENANT_ID}
  AND batch_record_version_id = {BATCH_RECORD_VERSION_ID}
  AND deleted = 0
GROUP BY batch_record_report_id;
"""
    result = {}
    for line in run_mysql_query(sql).splitlines():
        report_id, scoped, total = line.split("\t")
        result[report_id] = {"scoped": int(scoped or 0), "total": int(total or 0)}
    return result


def verify():
    reports = load_reports()
    assignment_counts = load_assignment_counts()
    failures = []
    rows = []
    for report in reports:
        generated = build_initialization(report)
        assist_rows = report["root"].get("edhrAssistRows")
        existing_count = len(assist_rows) if isinstance(assist_rows, list) else 0
        scoped_count = assignment_counts.get(report["report_id"], {}).get("scoped", 0)
        expected_count = generated["fillableCount"]
        persisted_coordinates = []
        if isinstance(assist_rows, list):
            for assist_row in assist_rows:
                fields = assist_row.get("fields") if isinstance(assist_row, dict) else None
                if not isinstance(fields, list):
                    continue
                for field in fields:
                    if isinstance(field, dict):
                        persisted_coordinates.append((field.get("rowIndex"), field.get("columnIndex")))
        expected_coordinates = [
            (row["fields"][0]["rowIndex"], row["fields"][0]["columnIndex"])
            for row in generated["assistRows"]
        ]
        if existing_count != expected_count:
            failures.append(
                f"{report['source_table_index']} {report['table_title']}: "
                f"assistRows {existing_count} != fillable {expected_count}"
            )
        if len(set(persisted_coordinates)) != len(persisted_coordinates):
            failures.append(
                f"{report['source_table_index']} {report['table_title']}: duplicate persisted assist cell"
            )
        if sorted(persisted_coordinates) != sorted(expected_coordinates):
            failures.append(
                f"{report['source_table_index']} {report['table_title']}: persisted assist coverage differs"
            )
        if scoped_count != expected_count:
            failures.append(
                f"{report['source_table_index']} {report['table_title']}: "
                f"scoped assignments {scoped_count} != fillable {expected_count}"
            )
        rows.append(
            (
                report["source_table_index"],
                report["table_title"],
                generated["signatureCount"],
                generated["fillerCount"],
                expected_count,
                existing_count,
                scoped_count,
            )
        )
    print("idx\ttitle\tsignatures\tfillers\tfillable\texistingAssistRows\tscopedAssignments")
    for row in rows:
        print("\t".join(map(str, row)))
    if failures:
        print("VERIFY FAIL")
        for failure in failures:
            print(f"- {failure}")
        return 1
    print("VERIFY PASS")
    return 0


def backup(reports):
    BACKUP_DIR.mkdir(exist_ok=True)
    timestamp = datetime.now().strftime("%Y%m%d%H%M%S")
    payload = {
        "createdAt": timestamp,
        "tenantId": TENANT_ID,
        "batchRecordVersionId": BATCH_RECORD_VERSION_ID,
        "reports": [
            {
                "reportId": report["report_id"],
                "sourceTableIndex": report["source_table_index"],
                "tableTitle": report["table_title"],
                "json": report["root"],
            }
            for report in reports
        ],
        "permissionRulesTsv": run_mysql_query(
            f"""
SELECT *
FROM mes_pro_edhr_process_form_permission_rule
WHERE tenant_id = {TENANT_ID}
  AND batch_record_version_id = {BATCH_RECORD_VERSION_ID}
  AND deleted = 0
ORDER BY batch_record_report_id, route_process_id, scope_key, id;
"""
        ),
    }
    backup_path = BACKUP_DIR / f"pressure-pump-v130-assist-mapping-backup-{timestamp}.json"
    backup_path.write_text(json.dumps(payload, ensure_ascii=False, indent=2), encoding="utf-8")
    return backup_path


def apply():
    reports = load_reports()
    backup_path = backup(reports)
    statements = ["SET NAMES utf8mb4;", "START TRANSACTION;"]
    for report in reports:
        generated = build_initialization(report)
        report["root"]["edhrAssistRows"] = generated["assistRows"]
        updated_json = json.dumps(report["root"], ensure_ascii=False, separators=(",", ":"))
        statements.append(
            "UPDATE jimu_report "
            f"SET json_str = {sql_string(updated_json)}, update_by = 'codex-init-assist', update_time = NOW() "
            f"WHERE id = {sql_string(report['report_id'])} AND tenant_id = {sql_string(str(TENANT_ID))};"
        )
        statements.append(
            "DELETE FROM mes_pro_edhr_process_form_permission_rule "
            f"WHERE tenant_id = {TENANT_ID} "
            f"AND batch_record_report_id = {sql_string(report['report_id'])} "
            f"AND batch_record_version_id = {BATCH_RECORD_VERSION_ID} "
            "AND rule_type IN ('FILL','EQUIPMENT_FILL','QUALITY_FILL');"
        )
        for assignment in generated["assignments"]:
            statements.append(
                "INSERT INTO mes_pro_edhr_process_form_permission_rule ("
                "route_process_id,batch_record_report_id,batch_record_definition_id,batch_record_version_id,"
                "rule_type,scope_key,signature_cell_key,signature_role,candidate_source_type,candidate_source_ids,"
                "completion_policy,due_minutes,enabled,fillable_scope_json,remark,creator,create_time,updater,update_time,deleted,tenant_id"
                ") VALUES ("
                f"0,{sql_string(report['report_id'])},{report['definition_id']},{BATCH_RECORD_VERSION_ID},"
                f"'FILL',{sql_string(assignment['scopeKey'])},'',NULL,"
                f"{sql_string(assignment['candidateSourceType'])},{sql_string(assignment['candidateSourceIds'])},"
                f"{sql_string(assignment['completionPolicy'])},{assignment['dueMinutes']},b'1',"
                f"CAST({sql_string(assignment['fillableScopeJson'])} AS JSON),"
                "'codex pressure pump initial assist mapping','codex-init-assist',NOW(),"
                f"'codex-init-assist',NOW(),b'0',{TENANT_ID});"
            )
    statements.append("COMMIT;")
    run_mysql_execute("\n".join(statements))
    print(f"APPLY PASS backup={backup_path}")
    return 0


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--verify", action="store_true")
    parser.add_argument("--apply", action="store_true")
    args = parser.parse_args()
    if args.verify == args.apply:
        raise SystemExit("choose exactly one of --verify or --apply")
    if args.verify:
        raise SystemExit(verify())
    raise SystemExit(apply())


if __name__ == "__main__":
    main()
