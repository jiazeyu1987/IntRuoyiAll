import argparse
import base64
import json
import math
import pathlib
import re
import subprocess
import sys
from datetime import datetime


TASK_DIR = pathlib.Path(__file__).resolve().parent
BACKUP_DIR = TASK_DIR / "output"
TENANT_ID = 1
TARGET_TEMPLATE_NAMES = ("损耗单", "过程检验记录")
DEFAULT_FILLER_USERNAME = "jiazeyu"
GRID_COLUMNS = 8


def run_mysql(sql: str, raw: bool = False) -> str:
    command = [
        "docker",
        "exec",
        "-i",
        "int-ruoyi-mysql",
        "sh",
        "-lc",
        (
            'mysql -uroot -p"$MYSQL_ROOT_PASSWORD" -D ruoyi-vue-pro '
            "--default-character-set=utf8mb4 --binary-mode"
            if raw
            else 'mysql -uroot -p"$MYSQL_ROOT_PASSWORD" -D ruoyi-vue-pro '
            "--default-character-set=utf8mb4 -N -B -r"
        ),
    ]
    completed = subprocess.run(command, input=sql, text=True, capture_output=True, encoding="utf-8")
    if completed.returncode != 0:
        sys.stderr.write(completed.stderr)
        raise SystemExit(completed.returncode)
    return completed.stdout


def sql_string(value: object) -> str:
    return "'" + str(value).replace("\\", "\\\\").replace("'", "''") + "'"


def parse_version_number(version_no: str) -> tuple[int, ...]:
    numbers = re.findall(r"\d+", version_no or "")
    return tuple(int(number) for number in numbers) or (0,)


def load_filler_user_id() -> int:
    sql = f"""
SET NAMES utf8mb4;
SELECT id
FROM system_users
WHERE tenant_id = {TENANT_ID}
  AND deleted = b'0'
  AND username = {sql_string(DEFAULT_FILLER_USERNAME)}
ORDER BY id
LIMIT 1;
"""
    output = run_mysql(sql).strip()
    if not output:
        raise RuntimeError(f"missing default filler user: {DEFAULT_FILLER_USERNAME}")
    return int(output.split("\t")[0])


def load_template_versions() -> list[dict]:
    sql = f"""
SET NAMES utf8mb4;
SELECT id, template_id, template_name, version_no, status,
       REPLACE(TO_BASE64(IFNULL(recognized_schema_json, '')), CHAR(10), ''),
       REPLACE(TO_BASE64(IFNULL(jimu_schema_json, '')), CHAR(10), ''),
       update_time
FROM bpm_form_template_version
WHERE tenant_id = {TENANT_ID}
  AND deleted = b'0'
  AND template_name IN ({",".join(sql_string(name) for name in TARGET_TEMPLATE_NAMES)})
ORDER BY template_name, id DESC;
"""
    rows = []
    for line in run_mysql(sql).splitlines():
        parts = line.split("\t")
        if len(parts) != 8:
            raise RuntimeError(f"unexpected template row shape: {line[:200]}")
        recognized_json = base64.b64decode(parts[5]).decode("utf-8")
        jimu_schema_json = base64.b64decode(parts[6]).decode("utf-8")
        rows.append(
            {
                "id": int(parts[0]),
                "templateId": int(parts[1]),
                "templateName": parts[2],
                "versionNo": parts[3],
                "status": parts[4],
                "recognizedFields": json.loads(recognized_json) if recognized_json.strip() else [],
                "jimuSchema": json.loads(jimu_schema_json) if jimu_schema_json.strip() else {},
                "updateTime": parts[7],
            }
        )
    return rows


def select_latest_targets() -> list[dict]:
    rows = load_template_versions()
    targets = []
    for template_name in TARGET_TEMPLATE_NAMES:
        candidates = [row for row in rows if row["templateName"] == template_name]
        if not candidates:
            raise RuntimeError(f"missing template: {template_name}")
        candidates.sort(
            key=lambda row: (
                row["status"] == "PUBLISHED",
                parse_version_number(row["versionNo"]),
                row["id"],
            ),
            reverse=True,
        )
        target = candidates[0]
        if target["status"] != "PUBLISHED":
            raise RuntimeError(f"{template_name} latest selected version is not PUBLISHED: {target['versionNo']}")
        if not target["recognizedFields"]:
            raise RuntimeError(f"{template_name} {target['versionNo']} has no recognized fields")
        targets.append(target)
    return targets


def field_value_type(field_type: str) -> str:
    normalized = (field_type or "").lower()
    if normalized == "number":
        return "NUMBER"
    if normalized == "date":
        return "DATE"
    if normalized == "datetime":
        return "DATETIME"
    if normalized == "checkbox":
        return "BOOLEAN"
    if normalized == "signature":
        return "SIGNATURE"
    return "STRING"


def field_component_flag(field_type: str) -> str:
    normalized = (field_type or "").lower()
    if normalized == "number":
        return "input-number"
    if normalized == "date":
        return "date"
    if normalized == "datetime":
        return "datetime"
    if normalized == "checkbox":
        return "checkbox"
    if normalized == "signature":
        return "signature"
    if normalized == "textarea":
        return "textarea"
    return "input-text"


def build_cell_rules(fields: list[dict]) -> list[dict]:
    rules = []
    for index, field in enumerate(fields):
        row_index = math.floor(index / 2) + 3
        label_column_index = 0 if index % 2 == 0 else 2
        input_column_index = label_column_index + 1
        label = str(field.get("label") or field.get("fieldCode") or "").strip()
        rules.append(
            {
                "rowIndex": row_index,
                "columnIndex": input_column_index,
                "valueType": field_value_type(str(field.get("fieldType") or "")),
                "componentFlag": field_component_flag(str(field.get("fieldType") or "")),
                "required": bool(field.get("required")),
                "label": label,
                "placeholder": "□" if str(field.get("fieldType") or "").lower() == "checkbox" else "?",
                "source": "MANUAL",
                "reviewed": True,
            }
        )
    return rules


def build_assist_mapping(target: dict, user_id: int) -> dict:
    rules = build_cell_rules(target["recognizedFields"])
    assist_rows = []
    fill_assignments = []
    for index, rule in enumerate(rules):
        grid_row_index = index // GRID_COLUMNS
        grid_column_index = index % GRID_COLUMNS
        row_key = f"ASSIST_GRID_U{user_id}_R{grid_row_index}_C{grid_column_index}"
        description = str(rule.get("label") or "").strip() or f"R{rule['rowIndex'] + 1}C{rule['columnIndex'] + 1}"
        assist_rows.append(
            {
                "rowKey": row_key,
                "description": f"默认辅助项：{description}",
                "sort": index + 1,
                "fields": [{"rowIndex": rule["rowIndex"], "columnIndex": rule["columnIndex"]}],
            }
        )
        fill_assignments.append(
            {
                "scopeKey": row_key,
                "candidateSourceType": "USERS",
                "candidateSourceIds": [user_id],
                "completionPolicy": "ANY_ONE",
                "enabled": True,
                "remark": f"默认辅助项：{description}",
            }
        )
    schema = dict(target["jimuSchema"])
    schema["assistRows"] = assist_rows
    schema["fillAssignments"] = fill_assignments
    return {"rules": rules, "assistRows": assist_rows, "fillAssignments": fill_assignments, "schema": schema}


def summarize_target(target: dict, generated: dict) -> str:
    existing_assist_rows = target["jimuSchema"].get("assistRows")
    existing_fill_assignments = target["jimuSchema"].get("fillAssignments")
    return "\t".join(
        map(
            str,
            [
                target["templateName"],
                target["versionNo"],
                target["id"],
                target["status"],
                len(target["recognizedFields"]),
                len(existing_assist_rows) if isinstance(existing_assist_rows, list) else 0,
                len(existing_fill_assignments) if isinstance(existing_fill_assignments, list) else 0,
                len(generated["assistRows"]),
                len(generated["fillAssignments"]),
            ],
        )
    )


def dry_run() -> int:
    user_id = load_filler_user_id()
    targets = select_latest_targets()
    print(f"defaultFiller={DEFAULT_FILLER_USERNAME}:{user_id}")
    print("templateName\tversionNo\trowId\tstatus\trecognizedFields\texistingAssistRows\texistingFillAssignments\tnewAssistRows\tnewFillAssignments")
    for target in targets:
        generated = build_assist_mapping(target, user_id)
        print(summarize_target(target, generated))
    return 0


def backup(targets: list[dict]) -> pathlib.Path:
    BACKUP_DIR.mkdir(exist_ok=True)
    timestamp = datetime.now().strftime("%Y%m%d%H%M%S")
    backup_path = BACKUP_DIR / f"latest-template-assist-default-backup-{timestamp}.json"
    backup_path.write_text(
        json.dumps(
            {
                "createdAt": timestamp,
                "tenantId": TENANT_ID,
                "targets": targets,
            },
            ensure_ascii=False,
            indent=2,
        ),
        encoding="utf-8",
    )
    return backup_path


def apply() -> int:
    user_id = load_filler_user_id()
    targets = select_latest_targets()
    backup_path = backup(targets)
    statements = ["SET NAMES utf8mb4;", "START TRANSACTION;"]
    for target in targets:
        generated = build_assist_mapping(target, user_id)
        statements.append(
            "UPDATE bpm_form_template_version "
            f"SET jimu_schema_json = {sql_string(json.dumps(generated['schema'], ensure_ascii=False, separators=(',', ':')))}, "
            "updater = 'codex-template-assist-default', update_time = NOW() "
            f"WHERE tenant_id = {TENANT_ID} "
            f"AND id = {target['id']} "
            f"AND template_id = {target['templateId']} "
            f"AND template_name = {sql_string(target['templateName'])} "
            f"AND version_no = {sql_string(target['versionNo'])} "
            "AND deleted = b'0';"
        )
    statements.append("COMMIT;")
    run_mysql("\n".join(statements), raw=True)
    print(f"APPLY PASS backup={backup_path}")
    return 0


def verify() -> int:
    user_id = load_filler_user_id()
    targets = select_latest_targets()
    failures = []
    print(f"defaultFiller={DEFAULT_FILLER_USERNAME}:{user_id}")
    print("templateName\tversionNo\trowId\tstatus\trecognizedFields\texistingAssistRows\texistingFillAssignments\texpectedAssistRows\texpectedFillAssignments")
    for target in targets:
        generated = build_assist_mapping(target, user_id)
        print(summarize_target(target, generated))
        assist_rows = target["jimuSchema"].get("assistRows")
        fill_assignments = target["jimuSchema"].get("fillAssignments")
        if not isinstance(assist_rows, list):
            failures.append(f"{target['templateName']} {target['versionNo']}: missing assistRows")
            continue
        if not isinstance(fill_assignments, list):
            failures.append(f"{target['templateName']} {target['versionNo']}: missing fillAssignments")
            continue
        if len(assist_rows) != len(generated["assistRows"]):
            failures.append(
                f"{target['templateName']} {target['versionNo']}: assistRows {len(assist_rows)} != {len(generated['assistRows'])}"
            )
        if len(fill_assignments) != len(generated["fillAssignments"]):
            failures.append(
                f"{target['templateName']} {target['versionNo']}: fillAssignments {len(fill_assignments)} != {len(generated['fillAssignments'])}"
            )
        row_keys = [str(row.get("rowKey") or "") for row in assist_rows if isinstance(row, dict)]
        if not row_keys or any(not row_key.startswith(f"ASSIST_GRID_U{user_id}_") for row_key in row_keys):
            failures.append(f"{target['templateName']} {target['versionNo']}: assist rowKey does not use default filler")
        assignment_user_sets = [
            assignment.get("candidateSourceIds")
            for assignment in fill_assignments
            if isinstance(assignment, dict)
        ]
        if any(ids != [user_id] for ids in assignment_user_sets):
            failures.append(f"{target['templateName']} {target['versionNo']}: fill assignment user mismatch")
    if failures:
        print("VERIFY FAIL")
        for failure in failures:
            print(f"- {failure}")
        return 1
    print("VERIFY PASS")
    return 0


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--dry-run", action="store_true")
    parser.add_argument("--apply", action="store_true")
    parser.add_argument("--verify", action="store_true")
    args = parser.parse_args()
    selected = [args.dry_run, args.apply, args.verify]
    if sum(1 for item in selected if item) != 1:
        raise SystemExit("choose exactly one of --dry-run, --apply, --verify")
    if args.dry_run:
        return dry_run()
    if args.apply:
        return apply()
    return verify()


if __name__ == "__main__":
    raise SystemExit(main())
