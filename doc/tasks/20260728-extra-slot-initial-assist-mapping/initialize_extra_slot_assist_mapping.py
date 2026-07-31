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
TARGET_SLOTS = ("LOSS_REPORT", "PROCESS_INSPECTION")
GRID_COLUMNS = 8
FILLER_USER_IDS = [795, 810]
SIGNATURE_ACTION_TYPES = {"FORM_REVIEW", "SUBMIT", "APPROVE"}


def run_mysql(sql, raw=False):
    command = [
        "docker",
        "exec",
        "-i",
        "int-ruoyi-mysql",
        "sh",
        "-lc",
        'mysql -uroot -p"$MYSQL_ROOT_PASSWORD" -D ruoyi-vue-pro '
        "--default-character-set=utf8mb4 --binary-mode" if raw else
        'mysql -uroot -p"$MYSQL_ROOT_PASSWORD" -D ruoyi-vue-pro '
        "--default-character-set=utf8mb4 -N -B -r",
    ]
    completed = subprocess.run(command, input=sql, text=True, capture_output=True, encoding="utf-8")
    if completed.returncode != 0:
        sys.stderr.write(completed.stderr)
        raise SystemExit(completed.returncode)
    return completed.stdout


def sql_string(value):
    return "'" + str(value).replace("\\", "\\\\").replace("'", "''") + "'"


def version_condition(alias, version_id):
    column = f"{alias}.batch_record_version_id" if alias else "batch_record_version_id"
    return f"{column} IS NULL" if version_id is None else f"{column} = {version_id}"


def load_targets():
    sql = """
WITH ranked AS (
  SELECT r.*,
         ROW_NUMBER() OVER (
           PARTITION BY r.form_slot_type
           ORDER BY (r.batch_record_version_id IS NULL),
                    r.batch_record_version_id DESC,
                    r.last_import_time DESC,
                    r.id DESC
         ) rn
  FROM mes_pro_batch_record_report r
  WHERE r.tenant_id = 1
    AND r.deleted = 0
    AND r.form_slot_type IN ('LOSS_REPORT','PROCESS_INSPECTION')
)
SELECT
  r.form_slot_type,
  r.id,
  r.report_id,
  r.report_code,
  r.batch_record_name,
  IFNULL(r.product_name, ''),
  IFNULL(r.table_title, ''),
  IFNULL(r.source_table_index, 0),
  IFNULL(r.batch_record_definition_id, ''),
  IFNULL(r.batch_record_version_id, ''),
  REPLACE(TO_BASE64(j.json_str), CHAR(10), '')
FROM ranked r
JOIN jimu_report j ON j.id = r.report_id
WHERE r.rn = 1
ORDER BY r.form_slot_type;
"""
    targets = []
    for line in run_mysql(sql).splitlines():
        parts = line.split("\t")
        if len(parts) != 11:
            raise RuntimeError(f"unexpected target row shape: {line[:200]}")
        targets.append(
            {
                "slot": parts[0],
                "id": int(parts[1]),
                "report_id": parts[2],
                "report_code": parts[3],
                "batch_record_name": parts[4],
                "product_name": parts[5],
                "table_title": parts[6],
                "source_table_index": int(parts[7]),
                "definition_id": int(parts[8]) if parts[8] else None,
                "version_id": int(parts[9]) if parts[9] else None,
                "root": json.loads(base64.b64decode(parts[10]).decode("utf-8")),
            }
        )
    slots = {target["slot"] for target in targets}
    missing = set(TARGET_SLOTS) - slots
    if missing:
        raise RuntimeError(f"missing latest slot targets: {sorted(missing)}")
    return targets


def iter_cells(root):
    rows = root.get("rows")
    if not isinstance(rows, dict):
        return
    for row_key, row in rows.items():
        try:
            row_index = int(row_key)
        except ValueError:
            continue
        cells = row.get("cells") if isinstance(row, dict) else None
        if not isinstance(cells, dict):
            continue
        for column_key, cell in cells.items():
            try:
                column_index = int(column_key)
            except ValueError:
                continue
            if isinstance(cell, dict):
                yield row_index, column_index, cell


def has_signature(cell):
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


def label_of(cell):
    for key in ("edhrCellRule", "fillForm", "edhrSignature"):
        value = cell.get(key)
        if isinstance(value, dict):
            label = str(value.get("label") or value.get("title") or "").strip()
            if label:
                return label
    return ""


def collect_cells(target):
    fillable = []
    signatures = []
    for row_index, column_index, cell in iter_cells(target["root"]):
        item = {"rowIndex": row_index, "columnIndex": column_index, "label": label_of(cell)}
        if has_signature(cell):
            signatures.append(item)
        if has_fill_form(cell) or has_signature(cell) or has_reviewed_rule(cell):
            fillable.append(item)
    fillable.sort(key=lambda item: (item["rowIndex"], item["columnIndex"]))
    signatures.sort(key=lambda item: (item["rowIndex"], item["columnIndex"]))
    return fillable, signatures


def choose_user(cell, signatures, user_ids):
    if not signatures:
        return user_ids[0]
    best_index = min(
        range(len(signatures)),
        key=lambda index: abs(cell["rowIndex"] - signatures[index]["rowIndex"]) * 1000
        + abs(cell["columnIndex"] - signatures[index]["columnIndex"]),
    )
    return user_ids[best_index]


def build_initialization(target):
    fillable, signatures = collect_cells(target)
    filler_count = max(1, len(signatures))
    if filler_count > len(FILLER_USER_IDS):
        raise RuntimeError(f"{target['slot']} requires {filler_count} users, only {len(FILLER_USER_IDS)} configured")
    user_ids = FILLER_USER_IDS[:filler_count]
    grouped = {user_id: [] for user_id in user_ids}
    for cell in fillable:
        grouped[choose_user(cell, signatures, user_ids)].append(cell)

    assist_rows = []
    assignments = []
    sort_index = 1
    for user_id in user_ids:
        if not grouped[user_id]:
            raise RuntimeError(f"{target['slot']} generated empty filler group {user_id}")
        for index, cell in enumerate(grouped[user_id], start=1):
            grid_row = math.ceil(index / GRID_COLUMNS)
            grid_column = ((index - 1) % GRID_COLUMNS) + 1
            row_key = f"ASSIST_GRID_U{user_id}_R{grid_row}_C{grid_column}"
            description = cell["label"] or f"R{cell['rowIndex']}C{cell['columnIndex']}"
            scope = {
                "schemaVersion": 2,
                "cells": [
                    {
                        "sourceTableIndex": 0,
                        "rowIndex": cell["rowIndex"],
                        "columnIndex": cell["columnIndex"],
                    }
                ],
            }
            assist_rows.append(
                {
                    "rowKey": row_key,
                    "description": f"自动映射：{description}",
                    "sort": sort_index,
                    "fields": [{"rowIndex": cell["rowIndex"], "columnIndex": cell["columnIndex"]}],
                }
            )
            assignments.append(
                {
                    "scopeKey": row_key,
                    "candidateSourceIds": str(user_id),
                    "fillableScopeJson": json.dumps(scope, ensure_ascii=False, separators=(",", ":")),
                }
            )
            sort_index += 1
    return {
        "signatureCount": len(signatures),
        "fillerCount": filler_count,
        "fillableCount": len(fillable),
        "assistRows": assist_rows,
        "assignments": assignments,
    }


def assignment_counts():
    sql = """
SELECT batch_record_report_id,
       IFNULL(batch_record_version_id, -1),
       SUM(CASE WHEN rule_type = 'FILL' AND scope_key <> 'ALL' THEN 1 ELSE 0 END)
FROM mes_pro_edhr_process_form_permission_rule
WHERE tenant_id = 1
  AND deleted = 0
GROUP BY batch_record_report_id, IFNULL(batch_record_version_id, -1);
"""
    result = {}
    for line in run_mysql(sql).splitlines():
        report_id, version_key, count = line.split("\t")
        result[(report_id, int(version_key))] = int(count or 0)
    return result


def verify():
    targets = load_targets()
    counts = assignment_counts()
    failures = []
    print("slot\tname\tversionId\treportId\tsignatures\tfillers\tfillable\texistingAssistRows\tscopedAssignments")
    for target in targets:
        generated = build_initialization(target)
        assist_rows = target["root"].get("edhrAssistRows")
        existing_count = len(assist_rows) if isinstance(assist_rows, list) else 0
        version_key = target["version_id"] if target["version_id"] is not None else -1
        scoped_count = counts.get((target["report_id"], version_key), 0)
        persisted_coordinates = []
        if isinstance(assist_rows, list):
            for assist_row in assist_rows:
                for field in assist_row.get("fields", []) if isinstance(assist_row, dict) else []:
                    if isinstance(field, dict):
                        persisted_coordinates.append((field.get("rowIndex"), field.get("columnIndex")))
        expected_coordinates = [
            (row["fields"][0]["rowIndex"], row["fields"][0]["columnIndex"])
            for row in generated["assistRows"]
        ]
        print(
            "\t".join(
                map(
                    str,
                    [
                        target["slot"],
                        target["batch_record_name"],
                        target["version_id"],
                        target["report_id"],
                        generated["signatureCount"],
                        generated["fillerCount"],
                        generated["fillableCount"],
                        existing_count,
                        scoped_count,
                    ],
                )
            )
        )
        if existing_count != generated["fillableCount"]:
            failures.append(f"{target['slot']}: assistRows {existing_count} != {generated['fillableCount']}")
        if scoped_count != generated["fillableCount"]:
            failures.append(f"{target['slot']}: scoped assignments {scoped_count} != {generated['fillableCount']}")
        if sorted(persisted_coordinates) != sorted(expected_coordinates):
            failures.append(f"{target['slot']}: persisted assist coverage differs")
        if len(set(persisted_coordinates)) != len(persisted_coordinates):
            failures.append(f"{target['slot']}: duplicate persisted assist cell")
    if failures:
        print("VERIFY FAIL")
        for failure in failures:
            print(f"- {failure}")
        return 1
    print("VERIFY PASS")
    return 0


def backup(targets):
    BACKUP_DIR.mkdir(exist_ok=True)
    timestamp = datetime.now().strftime("%Y%m%d%H%M%S")
    report_ids = ",".join(sql_string(target["report_id"]) for target in targets)
    payload = {
        "createdAt": timestamp,
        "tenantId": TENANT_ID,
        "targets": [
            {
                "slot": target["slot"],
                "reportId": target["report_id"],
                "batchRecordName": target["batch_record_name"],
                "batchRecordVersionId": target["version_id"],
                "json": target["root"],
            }
            for target in targets
        ],
        "permissionRulesTsv": run_mysql(
            f"""
SELECT *
FROM mes_pro_edhr_process_form_permission_rule
WHERE tenant_id = {TENANT_ID}
  AND batch_record_report_id IN ({report_ids})
  AND deleted = 0
ORDER BY batch_record_report_id, route_process_id, scope_key, id;
"""
        ),
    }
    backup_path = BACKUP_DIR / f"extra-slot-assist-mapping-backup-{timestamp}.json"
    backup_path.write_text(json.dumps(payload, ensure_ascii=False, indent=2), encoding="utf-8")
    return backup_path


def apply():
    targets = load_targets()
    backup_path = backup(targets)
    statements = ["SET NAMES utf8mb4;", "START TRANSACTION;"]
    for target in targets:
        generated = build_initialization(target)
        target["root"]["edhrAssistRows"] = generated["assistRows"]
        statements.append(
            "UPDATE jimu_report "
            f"SET json_str = {sql_string(json.dumps(target['root'], ensure_ascii=False, separators=(',', ':')))}, "
            "update_by = 'codex-init-extra-assist', update_time = NOW() "
            f"WHERE id = {sql_string(target['report_id'])} AND tenant_id = {sql_string(str(TENANT_ID))};"
        )
        statements.append(
            "DELETE FROM mes_pro_edhr_process_form_permission_rule "
            f"WHERE tenant_id = {TENANT_ID} "
            f"AND batch_record_report_id = {sql_string(target['report_id'])} "
            f"AND {version_condition('', target['version_id'])} "
            "AND rule_type IN ('FILL','EQUIPMENT_FILL','QUALITY_FILL');"
        )
        definition_value = "NULL" if target["definition_id"] is None else str(target["definition_id"])
        version_value = "NULL" if target["version_id"] is None else str(target["version_id"])
        for assignment in generated["assignments"]:
            statements.append(
                "INSERT INTO mes_pro_edhr_process_form_permission_rule ("
                "route_process_id,batch_record_report_id,batch_record_definition_id,batch_record_version_id,"
                "rule_type,scope_key,signature_cell_key,signature_role,candidate_source_type,candidate_source_ids,"
                "completion_policy,due_minutes,enabled,fillable_scope_json,remark,creator,create_time,updater,update_time,deleted,tenant_id"
                ") VALUES ("
                f"0,{sql_string(target['report_id'])},{definition_value},{version_value},"
                f"'FILL',{sql_string(assignment['scopeKey'])},'',NULL,'USERS',{sql_string(assignment['candidateSourceIds'])},"
                f"'ANY_ONE',2147483647,b'1',CAST({sql_string(assignment['fillableScopeJson'])} AS JSON),"
                "'codex extra slot initial assist mapping','codex-init-extra-assist',NOW(),"
                f"'codex-init-extra-assist',NOW(),b'0',{TENANT_ID});"
            )
    statements.append("COMMIT;")
    run_mysql("\n".join(statements), raw=True)
    print(f"APPLY PASS backup={backup_path}")
    return 0


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--verify", action="store_true")
    parser.add_argument("--apply", action="store_true")
    args = parser.parse_args()
    if args.verify == args.apply:
        raise SystemExit("choose exactly one of --verify or --apply")
    raise SystemExit(verify() if args.verify else apply())


if __name__ == "__main__":
    main()
