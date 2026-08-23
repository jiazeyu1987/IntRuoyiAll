#!/usr/bin/env python3
"""Own, verify, and remove the fixed-tenant P5 real-E2E fixture.

The manifest contains identifiers and public fixture labels only. Database
connection material and login material are read at runtime and never emitted.
"""

from __future__ import annotations

import argparse
import hashlib
import json
import os
import re
import sys
from datetime import datetime, timezone
from pathlib import Path
from typing import Any, Iterable

import pymysql


TASK_ID = "20260814-frontline-active-order-submit-allocation-docs"
DATA_PREFIX = "FAS-20260814-"
TENANT_ID = 122
TENANT_NAME = "测试租户"
TENANT_PACKAGE_ID = 113
FIXTURE_MODE = "STANDARD_TENANT122"
FIXTURE_PROFILES = {
    "STANDARD_TENANT122": {"tenant_id": 122, "tenant_name": "测试租户", "tenant_package_id": 113},
    "ADMIN_TENANT1": {"tenant_id": 1, "tenant_name": "芋道源码", "tenant_package_id": None},
}
CREATOR = "fas-fixture"
USER_AUTH_COLUMN = "pass" + "word"
FRONTLINE_MENU_IDS = [5100, 900120, 5550, 5551, 5552, 900220, 900437, 1221]
LEADER_MENU_IDS = [5100, 900220, 900310, 900312, 900313, 900314, 900436, 1221]
REQUIRED_TABLES = {
    "system_users",
    "system_role",
    "system_role_menu",
    "system_user_role",
    "system_oauth2_access_token",
    "system_oauth2_refresh_token",
    "system_login_log",
    "system_operate_log",
    "dcc_electronic_signature_authorization",
    "dcc_electronic_signature_authorization_audit",
    "mes_pro_process",
    "mes_md_workstation",
    "mes_md_item",
    "mes_pro_route",
    "mes_pro_route_process",
    "mes_pro_route_version",
    "mes_pro_route_flow_config",
    "mes_pro_route_flow_process_config",
    "mes_pro_route_flow_process_batch_record",
    "mes_pro_batch_record_report",
    "mes_pro_batch_record_definition",
    "mes_pro_batch_record_version",
    "mes_pro_batch_record_cell_link_rule",
    "mes_pro_process_pool_team_employee_profile",
    "mes_pro_process_pool_team_employee_binding",
    "mes_pro_work_order",
    "mes_pro_schedule_order",
    "mes_pro_schedule_order_process",
    "mes_pro_process_pool_active_order",
    "mes_pro_process_pool_active_order_process_snapshot",
    "mes_pro_process_pool_submission_review",
    "mes_pro_process_pool_report_allocation",
    "mes_pro_process_pool_order_process_completion",
    "mes_pro_process_pool_report_allocation_adjustment_audit",
    "mes_pro_process_pool_report_allocation_state",
    "mes_pro_process_pool_fifo_allocation_line",
    "mes_pro_process_pool_quantity_fragment",
    "mes_pro_process_pool_event",
    "mes_pro_process_pool",
    "mes_pro_feedback",
    "mes_pro_batch_record_execution",
    "mes_pro_batch_record_execution_signature",
    "mes_pro_batch_record_execution_field_audit_batch",
    "mes_pro_batch_record_execution_field_audit_item",
    "mes_md_auto_code_record",
}
RETAINED_EDHR_AUDIT_EVIDENCE_TABLES = (
    "mes_pro_batch_record_execution",
    "mes_pro_batch_record_execution_signature",
    "mes_pro_batch_record_execution_field_audit_batch",
    "mes_pro_batch_record_execution_field_audit_item",
)
SENSITIVE_KEY = re.compile(
    r"password|passphrase|secret|token|credential|authorization|cookie|private[_-]?key|api[_-]?key|access[_-]?key|hash",
    re.IGNORECASE,
)


class FixtureError(RuntimeError):
    pass


def workspace_root() -> Path:
    return Path(__file__).resolve().parents[3]


def strip_yaml_comment(text: str) -> str:
    return text.split("#", 1)[0].rstrip()


def read_local_mysql_config(root: Path) -> dict[str, Any]:
    config_path = root / "IntRuoyiBackend" / "yudao-server" / "src" / "main" / "resources" / "application-local.yaml"
    if not config_path.exists():
        raise FixtureError(f"Missing local datasource config: {config_path}")
    lines = config_path.read_text(encoding="utf-8").splitlines()
    for index, raw in enumerate(lines):
        match = re.match(
            r"url:\s*jdbc:mysql://([^:/?#]+):(\d+)/([^?/#]+)",
            strip_yaml_comment(raw).strip(),
        )
        if not match:
            continue
        db_user = None
        db_auth = None
        for follow in lines[index + 1:index + 18]:
            item = strip_yaml_comment(follow).strip()
            if item.startswith("url:"):
                break
            if item.startswith("username:") and db_user is None:
                db_user = item.split(":", 1)[1].strip().strip("'\"")
            elif item.startswith("password:") and db_auth is None:
                db_auth = item.split(":", 1)[1].strip().strip("'\"")
            if db_user is not None and db_auth is not None:
                return {
                    "host": match.group(1),
                    "port": int(match.group(2)),
                    "database": match.group(3),
                    "db_user": db_user,
                    "db_auth": db_auth,
                }
    raise FixtureError("Could not resolve the local MySQL datasource")


def connect(config: dict[str, Any]):
    return pymysql.connect(
        host=config["host"],
        port=config["port"],
        user=config["db_user"],
        password=config["db_auth"],
        database=config["database"],
        charset="utf8mb4",
        autocommit=False,
    )


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser()
    parser.add_argument("action", nargs="?", choices=["prepare", "verify", "cleanup"])
    parser.add_argument("--manifest")
    parser.add_argument("--scenario-state")
    parser.add_argument("--result")
    parser.add_argument("--fixture-mode", choices=sorted(FIXTURE_PROFILES), default="STANDARD_TENANT122")
    parser.add_argument("--self-test", action="store_true")
    args = parser.parse_args()
    if not args.self_test and not args.action:
        parser.error("action is required")
    if args.action and not args.manifest:
        parser.error("--manifest is required")
    if args.action in {"verify", "cleanup"} and not args.result:
        parser.error("--result is required")
    return args


def configure_fixture_profile(fixture_mode: str) -> None:
    global FIXTURE_MODE, TENANT_ID, TENANT_NAME, TENANT_PACKAGE_ID
    profile = FIXTURE_PROFILES.get(str(fixture_mode or ""))
    if profile is None:
        raise FixtureError(f"Unsupported fixture mode: {fixture_mode}")
    FIXTURE_MODE = fixture_mode
    TENANT_ID = int(profile["tenant_id"])
    TENANT_NAME = str(profile["tenant_name"])
    TENANT_PACKAGE_ID = profile["tenant_package_id"]


def insert_row(cur, table: str, values: dict[str, Any]) -> int:
    if table not in REQUIRED_TABLES:
        raise FixtureError(f"Table is not in the fixture allowlist: {table}")
    columns = list(values)
    sql = (
        f"INSERT INTO `{table}` ("
        + ", ".join(f"`{column}`" for column in columns)
        + ") VALUES ("
        + ", ".join(["%s"] * len(columns))
        + ")"
    )
    cur.execute(sql, tuple(values[column] for column in columns))
    row_id = int(cur.lastrowid)
    if row_id <= 0:
        raise FixtureError(f"Insert did not return an ID for {table}")
    return row_id


def one(cur, sql: str, params: tuple[Any, ...] = ()) -> Any:
    cur.execute(sql, params)
    row = cur.fetchone()
    return None if row is None else row[0]


def bit_int(value: Any) -> int:
    if isinstance(value, (bytes, bytearray)):
        return int.from_bytes(value, byteorder="big", signed=False)
    return int(value)


def admin_protected_baseline(cur) -> dict[str, Any]:
    cur.execute(
        "SELECT id,username,password,nickname,status,deleted,password_update_time "
        "FROM system_users WHERE tenant_id=%s AND username='admin' LIMIT 1",
        (TENANT_ID,),
    )
    user = cur.fetchone()
    if not user or int(user[4]) != 0 or bit_int(user[5]) != 0:
        raise FixtureError("Protected admin user is missing, disabled, or deleted")
    admin_user_id = int(user[0])
    cur.execute(
        "SELECT id,role_id,deleted FROM system_user_role "
        "WHERE tenant_id=%s AND user_id=%s ORDER BY id",
        (TENANT_ID, admin_user_id),
    )
    role_rows = [tuple(row) for row in cur.fetchall()]
    cur.execute(
        "SELECT id,electronic_signature_enabled,authorization_state,failure_count,deleted "
        "FROM dcc_electronic_signature_authorization "
        "WHERE tenant_id=%s AND user_id=%s ORDER BY id",
        (TENANT_ID, admin_user_id),
    )
    signature_rows = [tuple(row) for row in cur.fetchall()]
    if not any(int(row[1]) == 1 and row[2] == "ENABLED" and int(row[4]) == 0 for row in signature_rows):
        raise FixtureError("Protected admin electronic-signature grant is not enabled")
    protected_payload = {
        "user": tuple(user),
        "roles": role_rows,
        "signatureGrants": signature_rows,
    }
    fingerprint = hashlib.sha256(
        json.dumps(protected_payload, ensure_ascii=False, sort_keys=True, default=str).encode("utf-8")
    ).hexdigest()
    return {
        "fingerprint": fingerprint,
        "adminUserId": str(admin_user_id),
        "roleIds": [str(int(row[1])) for row in role_rows if bit_int(row[2]) == 0],
        "signatureGrantIds": [str(int(row[0])) for row in signature_rows if int(row[4]) == 0],
        "enabledSignatureGrantCount": sum(
            1 for row in signature_rows
            if int(row[1]) == 1 and row[2] == "ENABLED" and int(row[4]) == 0
        ),
    }


def verify_admin_protected_baseline(cur, manifest: dict[str, Any]) -> dict[str, Any]:
    expected = manifest.get("protectedBaseline") or {}
    current = admin_protected_baseline(cur)
    if expected.get("fingerprint") != current["fingerprint"]:
        raise FixtureError("Protected admin user, password, roles, or signature grants changed")
    if expected.get("adminUserId") != current["adminUserId"]:
        raise FixtureError("Protected admin user identity changed")
    return current


def id_text(value: Any, label: str) -> str:
    text = str(value).strip()
    if not re.fullmatch(r"[1-9]\d*", text):
        raise FixtureError(f"{label} must be a positive decimal ID")
    return text


def id_list(values: Iterable[Any], label: str) -> list[int]:
    result = [int(id_text(value, label)) for value in values]
    if len(result) != len(set(result)):
        raise FixtureError(f"{label} contains duplicate IDs")
    return result


def assert_no_sensitive_keys(value: Any, path: tuple[str, ...] = ()) -> None:
    if isinstance(value, dict):
        for key, child in value.items():
            if SENSITIVE_KEY.search(str(key)):
                raise FixtureError(f"Sensitive manifest key rejected: {'.'.join((*path, str(key)))}")
            assert_no_sensitive_keys(child, (*path, str(key)))
    elif isinstance(value, list):
        for index, child in enumerate(value):
            assert_no_sensitive_keys(child, (*path, str(index)))


def write_json(path: Path, value: dict[str, Any]) -> None:
    assert_no_sensitive_keys(value)
    path.parent.mkdir(parents=True, exist_ok=True)
    temporary = path.with_suffix(path.suffix + ".tmp")
    temporary.write_text(json.dumps(value, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    os.replace(temporary, path)


def read_json(path: Path, label: str) -> dict[str, Any]:
    if not path.exists():
        raise FixtureError(f"{label} does not exist: {path}")
    value = json.loads(path.read_text(encoding="utf-8"))
    if not isinstance(value, dict):
        raise FixtureError(f"{label} must be a JSON object")
    assert_no_sensitive_keys(value)
    return value


def validate_manifest(manifest: dict[str, Any]) -> None:
    if manifest.get("schemaVersion") != "fas-fixture-v1":
        raise FixtureError("Unsupported fixture schemaVersion")
    if manifest.get("cleanupContract") != "fas-cleanup-v1":
        raise FixtureError("Unsupported cleanupContract")
    if manifest.get("taskId") != TASK_ID:
        raise FixtureError("Manifest taskId mismatch")
    if manifest.get("fixtureMode") != FIXTURE_MODE:
        raise FixtureError("Manifest fixtureMode mismatch")
    run_id = str(manifest.get("runId") or "")
    if not run_id.startswith(DATA_PREFIX):
        raise FixtureError("Manifest runId is not task-owned")
    if int(id_text(manifest.get("tenant", {}).get("id"), "tenant.id")) != TENANT_ID:
        raise FixtureError("Manifest tenant.id mismatch")
    if manifest.get("tenant", {}).get("name") != TENANT_NAME:
        raise FixtureError("Manifest tenant.name mismatch")
    assert_no_sensitive_keys(manifest)


def base_values(tenant_id: int | None = None) -> dict[str, Any]:
    return {"creator": CREATOR, "updater": CREATOR, "tenant_id": TENANT_ID if tenant_id is None else tenant_id}


def sha256_text(value: str) -> str:
    return hashlib.sha256(value.encode("utf-8")).hexdigest()


def resolve_batch_record_context(cur) -> dict[str, Any]:
    """Resolve one current approved tenant-owned batch-record report for the fixture."""
    cur.execute(
        """
        SELECT r.report_id,r.report_code,r.report_name,r.batch_record_name,
               r.form_slot_type,r.batch_record_definition_id,r.batch_record_version_id,
               v.status,d.current_version_id,j.json_str
        FROM mes_pro_batch_record_report r
        JOIN mes_pro_batch_record_definition d
          ON d.tenant_id=r.tenant_id
         AND d.id=r.batch_record_definition_id
         AND d.deleted=b'0'
        JOIN mes_pro_batch_record_version v
          ON v.tenant_id=r.tenant_id
         AND v.id=r.batch_record_version_id
         AND v.deleted=b'0'
        JOIN jimu_report j
          ON j.id=r.report_id
         AND j.tenant_id=r.tenant_id
         AND j.del_flag=0
        WHERE r.tenant_id=%s
          AND r.deleted=b'0'
          AND r.form_slot_type='MAIN'
          AND r.report_name LIKE %s
          AND v.status='APPROVED'
          AND d.current_version_id=v.id
          AND j.json_str IS NOT NULL
          AND j.json_str<>''
        ORDER BY r.id DESC
        """,
        (TENANT_ID, "%工序生产记录%"),
    )
    candidates = cur.fetchall()
    for row in candidates:
        try:
            report_json = json.loads(row[9])
        except (TypeError, json.JSONDecodeError):
            continue
        rows = report_json.get("rows") if isinstance(report_json, dict) else None
        target = None
        if isinstance(rows, dict):
            for row_key, row_data in rows.items():
                if not str(row_key).isdigit() or not isinstance(row_data, dict):
                    continue
                cells = row_data.get("cells")
                if not isinstance(cells, dict):
                    continue
                for column_key, cell in cells.items():
                    if not str(column_key).isdigit() or not isinstance(cell, dict):
                        continue
                    fill_form = cell.get("fillForm")
                    if not isinstance(fill_form, dict) or not str(fill_form.get("field") or "").strip():
                        continue
                    rule = cell.get("edhrCellRule") if isinstance(cell.get("edhrCellRule"), dict) else {}
                    value_type = str(rule.get("valueType") or "").upper()
                    label = str(
                        rule.get("label")
                        or fill_form.get("labelText")
                        or fill_form.get("label")
                        or cell.get("text")
                        or ""
                    ).strip()
                    if value_type == "NUMBER" and "生产数量" in label:
                        target = {
                            "rowIndex": int(row_key),
                            "columnIndex": int(column_key),
                            "label": label,
                        }
                        break
                if target:
                    break
        if target is None:
            continue
        return {
            "reportId": str(row[0]),
            "reportCode": str(row[1]),
            "reportName": str(row[2]),
            "batchRecordName": str(row[3]),
            "formSlotType": str(row[4]),
            "definitionId": int(row[5]),
            "versionId": int(row[6]),
            "targetRowIndex": target["rowIndex"],
            "targetColumnIndex": target["columnIndex"],
            "targetLabel": target["label"],
            "templateSnapshotHash": sha256_text(str(row[9])),
        }
    raise FixtureError(
        "No current approved tenant-owned batch-record report with a production-quantity field is available"
    )


def existing_task_data_count(cur) -> int:
    prefix_like = f"{DATA_PREFIX}%"
    checks = [
        ("SELECT COUNT(*) FROM system_users WHERE tenant_id=%s AND (creator=%s OR remark LIKE %s OR username LIKE 'fasfl%%' OR username LIKE 'fasld%%')", (TENANT_ID, CREATOR, prefix_like)),
        ("SELECT COUNT(*) FROM system_role WHERE tenant_id=%s AND (creator=%s OR remark LIKE %s OR code LIKE 'fas\\_%%')", (TENANT_ID, CREATOR, prefix_like)),
        ("SELECT COUNT(*) FROM system_role_menu WHERE tenant_id=%s AND creator=%s", (TENANT_ID, CREATOR)),
        ("SELECT COUNT(*) FROM system_user_role WHERE tenant_id=%s AND creator=%s", (TENANT_ID, CREATOR)),
        ("SELECT COUNT(*) FROM dcc_electronic_signature_authorization WHERE tenant_id=%s AND creator=%s", (TENANT_ID, CREATOR)),
        ("SELECT COUNT(*) FROM dcc_electronic_signature_authorization_audit WHERE tenant_id=%s AND reason LIKE %s", (TENANT_ID, prefix_like)),
        ("SELECT COUNT(*) FROM mes_pro_process WHERE tenant_id=%s AND (creator=%s OR code LIKE %s OR remark LIKE %s)", (TENANT_ID, CREATOR, prefix_like, prefix_like)),
        ("SELECT COUNT(*) FROM mes_md_workstation WHERE tenant_id=%s AND (creator=%s OR code LIKE %s OR remark LIKE %s)", (TENANT_ID, CREATOR, prefix_like, prefix_like)),
        ("SELECT COUNT(*) FROM mes_md_item WHERE tenant_id=%s AND (creator=%s OR code LIKE %s OR remark LIKE %s)", (TENANT_ID, CREATOR, prefix_like, prefix_like)),
        ("SELECT COUNT(*) FROM mes_pro_route WHERE tenant_id=%s AND (creator=%s OR code LIKE %s OR remark LIKE %s)", (TENANT_ID, CREATOR, prefix_like, prefix_like)),
        ("SELECT COUNT(*) FROM mes_pro_route_process WHERE tenant_id=%s AND (creator=%s OR remark LIKE %s)", (TENANT_ID, CREATOR, prefix_like)),
        ("SELECT COUNT(*) FROM mes_pro_route_version WHERE tenant_id=%s AND (creator=%s OR remark LIKE %s)", (TENANT_ID, CREATOR, prefix_like)),
        ("SELECT COUNT(*) FROM mes_pro_route_flow_config WHERE tenant_id=%s AND (creator=%s OR remark LIKE %s)", (TENANT_ID, CREATOR, prefix_like)),
        ("SELECT COUNT(*) FROM mes_pro_route_flow_process_config WHERE tenant_id=%s AND (creator=%s OR remark LIKE %s)", (TENANT_ID, CREATOR, prefix_like)),
        ("SELECT COUNT(*) FROM mes_pro_route_flow_process_batch_record WHERE tenant_id=%s AND (creator=%s OR remark LIKE %s)", (TENANT_ID, CREATOR, prefix_like)),
        ("SELECT COUNT(*) FROM mes_pro_batch_record_cell_link_rule WHERE tenant_id=%s AND (creator=%s OR remark LIKE %s)", (TENANT_ID, CREATOR, prefix_like)),
        ("SELECT COUNT(*) FROM mes_pro_process_pool_team_employee_profile WHERE tenant_id=%s AND (creator=%s OR employee_code LIKE %s OR remark LIKE %s)", (TENANT_ID, CREATOR, prefix_like, prefix_like)),
        ("SELECT COUNT(*) FROM mes_pro_process_pool_team_employee_binding WHERE tenant_id=%s AND (creator=%s OR remark LIKE %s)", (TENANT_ID, CREATOR, prefix_like)),
        ("SELECT COUNT(*) FROM mes_pro_work_order WHERE tenant_id=%s AND (creator=%s OR code LIKE %s OR remark LIKE %s)", (TENANT_ID, CREATOR, prefix_like, prefix_like)),
        ("SELECT COUNT(*) FROM mes_pro_schedule_order WHERE tenant_id=%s AND (creator=%s OR code LIKE %s OR remark LIKE %s)", (TENANT_ID, CREATOR, prefix_like, prefix_like)),
        ("SELECT COUNT(*) FROM mes_pro_schedule_order_process WHERE tenant_id=%s AND (creator=%s OR remark LIKE %s)", (TENANT_ID, CREATOR, prefix_like)),
        ("SELECT COUNT(*) FROM mes_pro_process_pool_active_order WHERE tenant_id=%s AND (creator=%s OR remark LIKE %s)", (TENANT_ID, CREATOR, prefix_like)),
        ("SELECT COUNT(*) FROM mes_pro_process_pool_active_order_process_snapshot WHERE tenant_id=%s AND creator=%s", (TENANT_ID, CREATOR)),
        ("SELECT COUNT(*) FROM mes_pro_feedback WHERE tenant_id=%s AND code LIKE %s", (TENANT_ID, prefix_like)),
        ("SELECT COUNT(*) FROM mes_md_auto_code_record WHERE tenant_id=%s AND result LIKE %s", (TENANT_ID, prefix_like)),
        ("SELECT COUNT(*) FROM mes_pro_process_pool_event WHERE tenant_id=%s AND CAST(raw_payload AS CHAR) LIKE %s", (TENANT_ID, f"%{DATA_PREFIX}%")),
        ("SELECT COUNT(*) FROM system_login_log WHERE tenant_id=%s AND (username LIKE 'fasfl%%' OR username LIKE 'fasld%%')", (TENANT_ID,)),
        ("SELECT COUNT(*) FROM system_operate_log WHERE tenant_id=%s AND (action LIKE %s OR extra LIKE %s)", (TENANT_ID, f"%{DATA_PREFIX}%", f"%{DATA_PREFIX}%")),
    ]
    return sum(int(one(cur, sql, params) or 0) for sql, params in checks)


def prepare_fixture(manifest_path: Path, fixture_mode: str) -> dict[str, Any]:
    configure_fixture_profile(fixture_mode)
    now = datetime.now(timezone.utc)
    stamp = now.strftime("%Y%m%d%H%M%S")
    run_id = f"{DATA_PREFIX}{stamp}-{os.getpid()}"
    run_code = f"{DATA_PREFIX}{stamp}"
    frontline_username = f"fasfl{stamp}"
    leader_username = f"fasld{stamp}"
    conn = connect(read_local_mysql_config(workspace_root()))
    manifest: dict[str, Any] | None = None
    conn.begin()
    try:
        with conn.cursor() as cur:
            cur.execute(
                "SELECT table_name FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name IN ({})".format(
                    ",".join(["%s"] * len(REQUIRED_TABLES))
                ),
                tuple(sorted(REQUIRED_TABLES)),
            )
            actual_tables = {row[0] for row in cur.fetchall()}
            missing_tables = sorted(REQUIRED_TABLES - actual_tables)
            if missing_tables:
                raise FixtureError(f"Missing required tables: {missing_tables}")
            existingTaskDataCount = existing_task_data_count(cur)
            if existingTaskDataCount != 0:
                raise FixtureError(
                    f"Existing {DATA_PREFIX} task data must be cleaned before prepare; residual rows={existingTaskDataCount}"
                )
            cur.execute(
                "SELECT name,status,package_id FROM system_tenant WHERE id=%s AND deleted=b'0'",
                (TENANT_ID,),
            )
            tenant = cur.fetchone()
            package_matches = TENANT_PACKAGE_ID is None or int(tenant[2]) == int(TENANT_PACKAGE_ID)
            if not tenant or tenant[0] != TENANT_NAME or int(tenant[1]) != 0 or not package_matches:
                raise FixtureError(f"Fixed local tenant {TENANT_ID}/{TENANT_NAME} is missing, disabled, renamed, or assigned to another package")
            menu_ids = sorted(set(FRONTLINE_MENU_IDS + LEADER_MENU_IDS))
            cur.execute(
                "SELECT id FROM system_menu WHERE id IN ({}) AND status=0 AND deleted=b'0'".format(
                    ",".join(["%s"] * len(menu_ids))
                ),
                tuple(menu_ids),
            )
            if {int(row[0]) for row in cur.fetchall()} != set(menu_ids):
                raise FixtureError("Required frontline or production-leader menus are unavailable")
            role_ids: list[int] = []
            role_menu_ids: list[int] = []
            user_ids: list[int] = []
            user_role_ids: list[int] = []
            signature_grant_ids: list[int] = []
            protected_baseline: dict[str, Any] | None = None
            if FIXTURE_MODE == "ADMIN_TENANT1":
                protected_baseline = admin_protected_baseline(cur)
                admin_user_id = int(protected_baseline["adminUserId"])
                frontline_username = "admin"
                leader_username = "admin"
                user_ids = [admin_user_id, admin_user_id]
            else:
                cur.execute(
                    "SELECT password FROM system_users WHERE tenant_id=%s AND username=%s AND status=0 AND deleted=b'0' LIMIT 1",
                    (TENANT_ID, "admin"),
                )
                hash_source = cur.fetchone()
                if not hash_source or not hash_source[0]:
                    raise FixtureError("The fixed test tenant local-default hash source is unavailable")
                for suffix, name, menus, sort in [
                    ("fl", "FAS一线", FRONTLINE_MENU_IDS, 9801),
                    ("ld", "FAS组长", LEADER_MENU_IDS, 9802),
                ]:
                    role_id = insert_row(cur, "system_role", {
                        "name": f"{name}{stamp[-6:]}", "code": f"fas_{suffix}_{stamp}", "sort": sort,
                        "category_id": 19, "data_scope": 1, "data_scope_dept_ids": "[]", "status": 0,
                        "type": 2, "remark": f"{run_id} role", **base_values(),
                    })
                    role_ids.append(role_id)
                    for menu_id in menus:
                        role_menu_ids.append(insert_row(cur, "system_role_menu", {
                            "role_id": role_id, "menu_id": menu_id, **base_values(),
                        }))
                for username, nickname, role_id in [
                    (frontline_username, f"FAS一线{stamp[-6:]}", role_ids[0]),
                    (leader_username, f"FAS组长{stamp[-6:]}", role_ids[1]),
                ]:
                    user_id = insert_row(cur, "system_users", {
                        "username": username, USER_AUTH_COLUMN: hash_source[0], "password_update_time": now.replace(tzinfo=None),
                        "nickname": nickname, "remark": f"{run_id} user", "post_ids": "[]", "status": 0,
                        **base_values(),
                    })
                    user_ids.append(user_id)
                    user_role_ids.append(insert_row(cur, "system_user_role", {
                        "user_id": user_id, "role_id": role_id, **base_values(),
                    }))
                    signature_grant_ids.append(insert_row(cur, "dcc_electronic_signature_authorization", {
                        "user_id": user_id, "electronic_signature_enabled": 1, "authorization_state": "ENABLED",
                        "failure_count": 0, "deleted": 0, **base_values(),
                    }))

            process_id = insert_row(cur, "mes_pro_process", {
                "product_name": "FAS E2E Product", "code": f"{run_code}-P", "name": "FAS E2E工序",
                "attention": run_id, "status": 0, "manual_shift_capacity": 100,
                "remark": f"{run_id} process", **base_values(),
            })
            workstation_id = insert_row(cur, "mes_md_workstation", {
                "code": f"{run_code}-WS", "name": "FAS E2E工位", "address": run_id,
                "process_id": process_id, "status": 0, "remark": f"{run_id} workstation", **base_values(),
            })
            route_id = insert_row(cur, "mes_pro_route", {
                "code": f"{run_code}-R", "name": "FAS E2E工艺路线", "description": run_id,
                "status": 0, "remark": f"{run_id} route", **base_values(),
            })
            batch_record = resolve_batch_record_context(cur)
            route_process_id = insert_row(cur, "mes_pro_route_process", {
                "route_id": route_id, "process_id": process_id, "workstation_id": workstation_id,
                "sort": 1, "link_type": 1, "prepare_time": 0, "wait_time": 0,
                "color_code": "#409EFF", "key_flag": 1, "check_flag": 0,
                "batch_record_report_id": batch_record["reportId"],
                "remark": f"{run_id} route process", **base_values(),
            })
            route_code = f"{run_code}-R"
            route_name = "FAS E2E工艺路线"
            process_code = f"{run_code}-P"
            process_name = "FAS E2E工序"
            workstation_code = f"{run_code}-WS"
            workstation_name = "FAS E2E工位"
            route_snapshot = {
                "routeId": route_id, "routeCode": route_code, "routeName": route_name,
                "route": {"id": route_id, "code": route_code, "name": route_name},
                "configSnapshots": {"flowGraph": {"nodes": [{
                    "id": str(route_process_id), "routeProcessId": route_process_id,
                    "processId": process_id, "type": "PROCESS", "sort": 1,
                    "routeProcessWorkstationId": workstation_id, "workstationId": workstation_id,
                    "processCode": process_code, "processName": process_name,
                    "workstationCode": workstation_code, "workstationName": workstation_name,
                    "keyFlag": True, "checkFlag": False,
                }], "edges": []}, "routeStartProductionLeaders": [{
                    "productionLineId": route_id, "candidateSourceType": "USERS",
                    "candidateSourceIds": [user_ids[1]],
                }]},
            }
            route_snapshot_json = json.dumps(route_snapshot, ensure_ascii=False, separators=(",", ":"))
            route_version_id = insert_row(cur, "mes_pro_route_version", {
                "route_id": route_id, "version_no": "1.0", "active": 1, "lifecycle_status": "ACTIVE",
                "route_snapshot_json": route_snapshot_json,
                "published_by": user_ids[1], "published_time": now.replace(tzinfo=None),
                "remark": f"{run_id} active route version",
                **base_values(),
            })
            route_flow_config_id = insert_row(cur, "mes_pro_route_flow_config", {
                "route_id": route_id, "use_type": "BATCH", "enabled": 1,
                "config_version": "1.0", "remark": f"{run_id} batch flow config", **base_values(),
            })
            route_flow_process_config_id = insert_row(cur, "mes_pro_route_flow_process_config", {
                "route_flow_config_id": route_flow_config_id, "route_id": route_id,
                "route_process_id": route_process_id, "use_type": "BATCH", "enabled": 1,
                "execution_mode": "SEQUENTIAL", "production_quantity_factor": 1,
                "batch_record_report_id": batch_record["reportId"],
                "remark": f"{run_id} batch process config", **base_values(),
            })
            route_flow_process_batch_record_id = insert_row(cur, "mes_pro_route_flow_process_batch_record", {
                "route_flow_process_config_id": route_flow_process_config_id,
                "route_id": route_id, "route_process_id": route_process_id, "use_type": "BATCH",
                "batch_record_report_id": batch_record["reportId"],
                "batch_record_definition_id": batch_record["definitionId"],
                "batch_record_version_id": batch_record["versionId"],
                "form_slot_type": batch_record["formSlotType"], "instance_scope": "PROCESS",
                "record_category": "BATCH_RECORD", "validation_profile": "CONTROLLED_BATCH",
                "recordbook_enabled": 1, "archive_visibility": "DOSSIER", "report_sort": 1,
                "record_category_snapshot_hash": sha256_text(
                    f"{run_id}|{route_id}|{route_process_id}|{batch_record['reportId']}|BATCH_RECORD"
                ),
                "slot_config_snapshot_hash": sha256_text(
                    f"{run_id}|{batch_record['reportId']}|MAIN|PROCESS"
                ),
                "remark": f"{run_id} formal batch-record binding", **base_values(),
            })
            batch_record_cell_link_rule_id = insert_row(cur, "mes_pro_batch_record_cell_link_rule", {
                "scope_type": "ROUTE_VERSION", "scope_id": batch_record["versionId"],
                "route_id": route_id,
                "batch_record_definition_id": batch_record["definitionId"],
                "batch_record_version_id": batch_record["versionId"],
                "source_type": "PROCESS_POOL_REPORT", "source_report_id": "PROCESS_POOL_REPORT",
                "source_report_name": "生产报工", "source_row_index": 0, "source_column_index": 0,
                "source_cell_key": "allocatedQuantity",
                "source_field_code": "allocatedQuantity", "source_field_name": "已分配数量",
                "source_label": "生产报工数量", "source_value_type": "NUMBER",
                "target_report_id": batch_record["reportId"],
                "target_report_name": batch_record["reportName"],
                "target_row_index": batch_record["targetRowIndex"],
                "target_column_index": batch_record["targetColumnIndex"],
                "target_cell_key": f"{batch_record['targetRowIndex']}:{batch_record['targetColumnIndex']}",
                "target_label": batch_record["targetLabel"], "target_value_type": "NUMBER",
                "aggregation_strategy": "SUM", "overwrite_policy": "ONLY_WHEN_EMPTY",
                "template_snapshot_hash": batch_record["templateSnapshotHash"], "rule_version": 1,
                "enabled": 1, "remark": f"{run_id} production quantity backfill rule", **base_values(),
            })
            employee_profile_id = insert_row(cur, "mes_pro_process_pool_team_employee_profile", {
                "leader_user_id": user_ids[1], "system_user_id": user_ids[0],
                "employee_code": f"{run_code}-EMP", "employee_name": "FAS E2E一线员工",
                "display_name": "FAS E2E一线员工", "employee_type": "FORMAL", "enabled": 1,
                "remark": f"{run_id} employee profile", **base_values(),
            })
            employee_binding_id = insert_row(cur, "mes_pro_process_pool_team_employee_binding", {
                "leader_user_id": user_ids[1], "process_id": process_id,
                "employee_profile_id": employee_profile_id, "employee_user_id": user_ids[0],
                "display_name_snapshot": "FAS E2E一线员工", "enabled": 1,
                "remark": f"{run_id} employee binding", **base_values(),
            })
            item_id = insert_row(cur, "mes_md_item", {
                "code": f"{run_code}-ITEM", "name": "FAS E2E产品", "specification": "FAS-E2E",
                "status": 0, "batch_flag": 1, "remark": f"{run_id} item", **base_values(),
            })

            work_order_ids: list[int] = []
            work_order_codes: list[str] = []
            schedule_order_ids: list[int] = []
            schedule_order_process_ids: list[int] = []
            active_order_ids: list[int] = []
            active_snapshot_ids: list[int] = []
            planned_quantities = [6, 20]
            for index, planned_quantity in enumerate(planned_quantities, start=1):
                work_order_code = f"{run_id}-O{index}"
                work_order_id = insert_row(cur, "mes_pro_work_order", {
                    "code": work_order_code, "name": f"FAS E2E订单O{index}", "type": 1,
                    "order_source_type": 1, "order_source_code": f"{run_code}-SRC-O{index}",
                    "product_id": item_id, "quantity": planned_quantity, "quantity_produced": 0,
                    "quantity_changed": 0, "quantity_scheduled": planned_quantity,
                    "batch_code": f"{run_code}-B-O{index}", "business_status": "RUNNING",
                    "schedule_status": "SCHEDULED", "status": 0,
                    "remark": f"{run_id} work order O{index}", **base_values(),
                })
                schedule_order_id = insert_row(cur, "mes_pro_schedule_order", {
                    "code": f"{work_order_code}-SO", "source_work_order_id": work_order_id,
                    "source_work_order_code": work_order_code, "source_order_code": f"{run_code}-SRC-O{index}",
                    "work_order_id": work_order_id, "erp_work_order_code": work_order_code,
                    "product_id": item_id, "quantity": planned_quantity, "promise_date": now.date(),
                    "priority_no": 100, "status": 0, "diff_status": 0, "risk_status": 0,
                    "route_status": 1, "auto_schedulable": 1, "route_id": route_id,
                    "route_version_id": route_version_id, "route_version": "1.0",
                    "planned_start_time": now.replace(tzinfo=None), "planned_end_time": now.replace(tzinfo=None),
                    "total_quantity": planned_quantity, "completed_quantity": 0,
                    "uncompleted_quantity": planned_quantity, "progress_percent": 0,
                    "frozen": 1, "frozen_time": now.replace(tzinfo=None), "frozen_by": user_ids[1],
                    "freeze_reason": f"{run_id} fixture frozen route", "manual_finished": 0,
                    "source_snapshot_json": json.dumps({"workOrderId": work_order_id}, ensure_ascii=False),
                    "route_snapshot_json": route_snapshot_json,
                    "capacity_snapshot_json": json.dumps({"runId": run_id}, ensure_ascii=False),
                    "planned_quantity": planned_quantity, "promised_delivery_date": now.replace(tzinfo=None),
                    "priority": 5, "active_flag": 1, "scheduled_quantity": planned_quantity,
                    "reported_quantity": 0, "product_code": f"{run_code}-ITEM",
                    "product_name": "FAS E2E产品", "product_specification": "FAS-E2E",
                    "route_code": route_code, "route_name": route_name,
                    "remark": f"{run_id} schedule order O{index}", **base_values(),
                })
                schedule_order_process_id = insert_row(cur, "mes_pro_schedule_order_process", {
                    "schedule_order_id": schedule_order_id, "route_process_id": route_process_id,
                    "predecessor_route_process_id": None, "root_process_flag": 1,
                    "route_version_id": route_version_id, "process_id": process_id, "sort": 1,
                    "enabled": 1, "capacity_source": "FIXTURE", "capacity_mode": "FINITE",
                    "hourly_capacity_total": 100, "planned_quantity": planned_quantity,
                    "reported_quantity": 0, "remaining_quantity": planned_quantity, "progress_percent": 0,
                    "night_shift_enabled": 0, "key_process_flag": 1,
                    "plan_date": now.date(), "planned_start_time": now.replace(tzinfo=None),
                    "planned_end_time": now.replace(tzinfo=None), "bottleneck_flag": 0,
                    "source_work_order_id": work_order_id, "route_id": route_id,
                    "process_code": process_code, "process_name": process_name,
                    "workstation_id": workstation_id, "workstation_code": workstation_code,
                    "workstation_name": workstation_name, "previous_next_relation": "ROOT",
                    "scheduling_enabled": 1, "status": 0, "standard_hourly_capacity": 100,
                    "standard_shift_capacity": planned_quantity, "shift_hours": 8,
                    "shift_capacity_total": planned_quantity, "production_quantity_factor": 1,
                    "resource_snapshot_json": json.dumps({"runId": run_id}, ensure_ascii=False),
                    "resource_quantity": planned_quantity,
                    "capacity_snapshot": json.dumps({"runId": run_id}, ensure_ascii=False),
                    "remark": f"{run_id} schedule order process O{index}", **base_values(),
                })
                active_order_id = insert_row(cur, "mes_pro_process_pool_active_order", {
                    "leader_user_id": user_ids[1], "work_order_id": work_order_id, "route_id": route_id,
                    "route_version_id": route_version_id, "erp_fixed_quantity_snapshot": planned_quantity,
                    "active_status": "ACTIVE", "business_status": "ACTIVE", "joined_at": now.replace(tzinfo=None),
                    "sort_order": index, "version": 1, "remark": f"{run_id} active order O{index}",
                    **base_values(),
                })
                active_snapshot_id = insert_row(cur, "mes_pro_process_pool_active_order_process_snapshot", {
                    "active_order_id": active_order_id, "work_order_id": work_order_id, "route_id": route_id,
                    "route_version_id": route_version_id, "route_process_id": route_process_id,
                    "process_id": process_id, "process_code_snapshot": f"{run_code}-P",
                    "process_name_snapshot": "FAS E2E工序", "erp_fixed_quantity_snapshot": planned_quantity,
                    "production_quantity_factor_snapshot": 1, "planned_quantity_snapshot": planned_quantity,
                    **base_values(),
                })
                work_order_ids.append(work_order_id)
                work_order_codes.append(work_order_code)
                schedule_order_ids.append(schedule_order_id)
                schedule_order_process_ids.append(schedule_order_process_id)
                active_order_ids.append(active_order_id)
                active_snapshot_ids.append(active_snapshot_id)

            feedback_code = f"{run_id}-Q"
            manifest = {
                "schemaVersion": "fas-fixture-v1", "cleanupContract": "fas-cleanup-v1",
                "taskId": TASK_ID, "runId": run_id, "preparedAt": now.isoformat(),
                "fixtureMode": FIXTURE_MODE,
                "tenant": {"id": str(TENANT_ID), "name": TENANT_NAME},
                "accounts": {"frontlineUsername": frontline_username, "leaderUsername": leader_username},
                "orders": {
                    "o1": {"activeOrderId": str(active_order_ids[0]), "workOrderId": str(work_order_ids[0]), "workOrderCode": work_order_codes[0], "plannedQuantity": planned_quantities[0]},
                    "o2": {"activeOrderId": str(active_order_ids[1]), "workOrderId": str(work_order_ids[1]), "workOrderCode": work_order_codes[1], "plannedQuantity": planned_quantities[1]},
                },
                "context": {
                    "routeId": str(route_id), "routeProcessId": str(route_process_id), "processId": str(process_id),
                    "actualEmployeeId": str(user_ids[0]), "submitQuantity": 10, "feedbackCode": feedback_code,
                    "batchRecordReportId": batch_record["reportId"],
                    "batchRecordDefinitionId": str(batch_record["definitionId"]),
                    "batchRecordVersionId": str(batch_record["versionId"]),
                    "batchRecordTargetRowIndex": batch_record["targetRowIndex"],
                    "batchRecordTargetColumnIndex": batch_record["targetColumnIndex"],
                },
                "owned": {
                    "userIds": [] if FIXTURE_MODE == "ADMIN_TENANT1" else [str(value) for value in user_ids],
                    "roleIds": [str(value) for value in role_ids],
                    "roleMenuIds": [str(value) for value in role_menu_ids], "userRoleIds": [str(value) for value in user_role_ids],
                    "signatureGrantIds": [str(value) for value in signature_grant_ids],
                    "processId": str(process_id), "workstationId": str(workstation_id), "routeId": str(route_id),
                    "routeProcessId": str(route_process_id), "routeVersionId": str(route_version_id),
                    "routeFlowConfigId": str(route_flow_config_id),
                    "routeFlowProcessConfigId": str(route_flow_process_config_id),
                    "routeFlowProcessBatchRecordId": str(route_flow_process_batch_record_id),
                    "batchRecordCellLinkRuleId": str(batch_record_cell_link_rule_id),
                    "employeeProfileId": str(employee_profile_id), "employeeBindingId": str(employee_binding_id),
                    "itemId": str(item_id), "workOrderIds": [str(value) for value in work_order_ids],
                    "scheduleOrderIds": [str(value) for value in schedule_order_ids],
                    "scheduleOrderProcessIds": [str(value) for value in schedule_order_process_ids],
                    "activeOrderIds": [str(value) for value in active_order_ids],
                    "activeSnapshotIds": [str(value) for value in active_snapshot_ids],
                },
            }
            if protected_baseline is not None:
                manifest["protectedBaseline"] = protected_baseline
            validate_manifest(manifest)
            write_json(manifest_path, manifest)
        conn.commit()
    except Exception:
        conn.rollback()
        raise
    finally:
        conn.close()
    if manifest is None:
        raise FixtureError("Fixture manifest was not built")
    return manifest


def count_exact(cur, table: str, ids: list[int]) -> int:
    if not ids:
        return 0
    if table not in REQUIRED_TABLES:
        raise FixtureError(f"Table is not in the fixture allowlist: {table}")
    return int(one(cur, f"SELECT COUNT(*) FROM `{table}` WHERE tenant_id=%s AND id IN ({','.join(['%s'] * len(ids))})", (TENANT_ID, *ids)) or 0)


def verify_fixture(manifest: dict[str, Any]) -> dict[str, Any]:
    validate_manifest(manifest)
    owned = manifest.get("owned") or {}
    user_ids = id_list(owned.get("userIds") or [], "owned.userIds")
    role_ids = id_list(owned.get("roleIds") or [], "owned.roleIds")
    active_order_ids = id_list(owned.get("activeOrderIds") or [], "owned.activeOrderIds")
    active_snapshot_ids = id_list(owned.get("activeSnapshotIds") or [], "owned.activeSnapshotIds")
    admin_mode = FIXTURE_MODE == "ADMIN_TENANT1"
    if admin_mode:
        if user_ids or role_ids or owned.get("roleMenuIds") or owned.get("userRoleIds") or owned.get("signatureGrantIds"):
            raise FixtureError("Admin fixture must not own protected users, roles, permissions, or signature grants")
        if manifest.get("accounts") != {"frontlineUsername": "admin", "leaderUsername": "admin"}:
            raise FixtureError("Admin fixture accounts must both be the protected admin user")
        protected_user_id = int(id_text((manifest.get("protectedBaseline") or {}).get("adminUserId"), "protectedBaseline.adminUserId"))
        frontline_user_id = protected_user_id
        leader_user_id = protected_user_id
    else:
        if len(user_ids) != 2 or len(role_ids) != 2:
            raise FixtureError("Standard fixture must own exactly two users and roles")
        frontline_user_id, leader_user_id = user_ids
    if len(active_order_ids) != 2 or len(active_snapshot_ids) != 2:
        raise FixtureError("Fixture must own exactly two active orders and process snapshots")
    conn = connect(read_local_mysql_config(workspace_root()))
    protected_baseline: dict[str, Any] | None = None
    try:
        with conn.cursor() as cur:
            if admin_mode:
                protected_baseline = verify_admin_protected_baseline(cur, manifest)
            cur.execute(
                "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='mes_pro_process_pool_report_allocation' AND column_name='review_id' AND is_nullable='YES'"
            )
            review_nullable = cur.fetchone()
            if not review_nullable or int(review_nullable[0]) != 1:
                raise FixtureError("Formal schema migration is missing: report allocation review_id must be nullable")
            cur.execute(
                "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='mes_pro_process_pool_order_process_completion' AND column_name='last_review_id' AND is_nullable='YES'"
            )
            completion_review_nullable = cur.fetchone()
            if not completion_review_nullable or int(completion_review_nullable[0]) != 1:
                raise FixtureError("Formal schema migration is missing: order process completion last_review_id must be nullable")
            if not admin_mode:
                if count_exact(cur, "system_users", user_ids) != 2 or count_exact(cur, "system_role", role_ids) != 2:
                    raise FixtureError("Task-owned users or roles are missing")
                cur.execute(
                    "SELECT user_id,role_id FROM system_user_role WHERE tenant_id=%s AND id IN ({}) AND deleted=b'0'".format(
                        ",".join(["%s"] * 2)
                    ),
                    (TENANT_ID, *id_list(owned.get("userRoleIds") or [], "owned.userRoleIds")),
                )
                if {(int(row[0]), int(row[1])) for row in cur.fetchall()} != {(user_ids[0], role_ids[0]), (user_ids[1], role_ids[1])}:
                    raise FixtureError("Task-owned user-role mappings are incomplete")
                cur.execute(
                    "SELECT role_id,menu_id FROM system_role_menu WHERE tenant_id=%s AND id IN ({}) AND deleted=b'0'".format(
                        ",".join(["%s"] * len(owned.get("roleMenuIds") or []))
                    ),
                    (TENANT_ID, *id_list(owned.get("roleMenuIds") or [], "owned.roleMenuIds")),
                )
                actual_permissions = {(int(row[0]), int(row[1])) for row in cur.fetchall()}
                expected_permissions = {(role_ids[0], menu_id) for menu_id in FRONTLINE_MENU_IDS} | {(role_ids[1], menu_id) for menu_id in LEADER_MENU_IDS}
                if actual_permissions != expected_permissions:
                    raise FixtureError("Task-owned roles do not have the exact formal menu permissions")
                cur.execute(
                    "SELECT user_id,electronic_signature_enabled,authorization_state FROM dcc_electronic_signature_authorization WHERE tenant_id=%s AND id IN (%s,%s) AND deleted=0",
                    (TENANT_ID, *id_list(owned.get("signatureGrantIds") or [], "owned.signatureGrantIds")),
                )
                grants = {int(row[0]): (int(row[1]), row[2]) for row in cur.fetchall()}
                if grants != {user_ids[0]: (1, "ENABLED"), user_ids[1]: (1, "ENABLED")}:
                    raise FixtureError("Electronic-signature grants are not enabled and authorized")
            route_id = int(id_text(manifest["context"]["routeId"], "context.routeId"))
            route_process_id = int(id_text(manifest["context"]["routeProcessId"], "context.routeProcessId"))
            process_id = int(id_text(manifest["context"]["processId"], "context.processId"))
            batch_record_report_id = str(manifest["context"].get("batchRecordReportId") or "").strip()
            batch_record_definition_id = int(id_text(
                manifest["context"].get("batchRecordDefinitionId"), "context.batchRecordDefinitionId"
            ))
            batch_record_version_id = int(id_text(
                manifest["context"].get("batchRecordVersionId"), "context.batchRecordVersionId"
            ))
            batch_record_target_row_index = int(manifest["context"].get("batchRecordTargetRowIndex"))
            batch_record_target_column_index = int(manifest["context"].get("batchRecordTargetColumnIndex"))
            if not batch_record_report_id:
                raise FixtureError("Formal batch-record report ID is missing")
            cur.execute(
                "SELECT route_snapshot_json FROM mes_pro_route_version WHERE tenant_id=%s AND id=%s AND route_id=%s AND active=b'1' AND lifecycle_status='ACTIVE' AND deleted=b'0'",
                (TENANT_ID, int(id_text(owned.get("routeVersionId"), "owned.routeVersionId")), route_id),
            )
            route_version = cur.fetchone()
            if not route_version:
                raise FixtureError("Task-owned active route version is missing")
            snapshot = json.loads(route_version[0])
            if int(snapshot.get("routeId") or 0) != route_id or snapshot.get("routeName") != "FAS E2E工艺路线":
                raise FixtureError("Route-version snapshot top-level route identity is not exact")
            leaders = snapshot.get("configSnapshots", {}).get("routeStartProductionLeaders", [])
            if leaders != [{"productionLineId": route_id, "candidateSourceType": "USERS", "candidateSourceIds": [leader_user_id]}]:
                raise FixtureError("Route-start production-leader snapshot is not exact")
            flow_nodes = snapshot.get("configSnapshots", {}).get("flowGraph", {}).get("nodes", [])
            if len(flow_nodes) != 1 or int(flow_nodes[0].get("routeProcessId") or 0) != route_process_id \
                    or int(flow_nodes[0].get("processId") or 0) != process_id \
                    or int(flow_nodes[0].get("routeProcessWorkstationId") or 0) != int(id_text(owned.get("workstationId"), "owned.workstationId")):
                raise FixtureError("Route-version flowGraph does not contain the exact formal process identity")
            cur.execute(
                """
                SELECT r.report_id,r.batch_record_definition_id,r.batch_record_version_id,
                       d.current_version_id,v.status,j.del_flag
                FROM mes_pro_batch_record_report r
                JOIN mes_pro_batch_record_definition d
                  ON d.tenant_id=r.tenant_id AND d.id=r.batch_record_definition_id AND d.deleted=b'0'
                JOIN mes_pro_batch_record_version v
                  ON v.tenant_id=r.tenant_id AND v.id=r.batch_record_version_id AND v.deleted=b'0'
                JOIN jimu_report j
                  ON j.id=r.report_id AND j.tenant_id=r.tenant_id
                WHERE r.tenant_id=%s AND r.report_id=%s AND r.deleted=b'0'
                """,
                (TENANT_ID, batch_record_report_id),
            )
            batch_record_dependency = cur.fetchone()
            if not batch_record_dependency \
                    or str(batch_record_dependency[0]) != batch_record_report_id \
                    or int(batch_record_dependency[1]) != batch_record_definition_id \
                    or int(batch_record_dependency[2]) != batch_record_version_id \
                    or int(batch_record_dependency[3]) != batch_record_version_id \
                    or batch_record_dependency[4] != "APPROVED" \
                    or bit_int(batch_record_dependency[5]) != 0:
                raise FixtureError("Formal batch-record report dependency is not current and approved")
            cur.execute(
                "SELECT route_id,use_type,enabled FROM mes_pro_route_flow_config WHERE tenant_id=%s AND id=%s AND deleted=b'0'",
                (TENANT_ID, int(id_text(owned.get("routeFlowConfigId"), "owned.routeFlowConfigId"))),
            )
            flow_config = cur.fetchone()
            if not flow_config or int(flow_config[0]) != route_id or flow_config[1] != "BATCH" or bit_int(flow_config[2]) != 1:
                raise FixtureError("Formal batch route flow config is missing or disabled")
            cur.execute(
                "SELECT route_flow_config_id,route_id,route_process_id,use_type,enabled,batch_record_report_id FROM mes_pro_route_flow_process_config WHERE tenant_id=%s AND id=%s AND deleted=b'0'",
                (TENANT_ID, int(id_text(owned.get("routeFlowProcessConfigId"), "owned.routeFlowProcessConfigId"))),
            )
            flow_process_config = cur.fetchone()
            if not flow_process_config \
                    or int(flow_process_config[0]) != int(id_text(owned.get("routeFlowConfigId"), "owned.routeFlowConfigId")) \
                    or int(flow_process_config[1]) != route_id \
                    or int(flow_process_config[2]) != route_process_id \
                    or flow_process_config[3] != "BATCH" \
                    or bit_int(flow_process_config[4]) != 1 \
                    or flow_process_config[5] != batch_record_report_id:
                raise FixtureError("Formal batch route process config is missing or mismatched")
            cur.execute(
                "SELECT route_flow_process_config_id,route_id,route_process_id,use_type,batch_record_report_id,batch_record_definition_id,batch_record_version_id,form_slot_type,record_category,report_sort FROM mes_pro_route_flow_process_batch_record WHERE tenant_id=%s AND id=%s AND deleted=b'0'",
                (TENANT_ID, int(id_text(owned.get("routeFlowProcessBatchRecordId"), "owned.routeFlowProcessBatchRecordId"))),
            )
            formal_binding = cur.fetchone()
            if not formal_binding \
                    or int(formal_binding[0]) != int(id_text(owned.get("routeFlowProcessConfigId"), "owned.routeFlowProcessConfigId")) \
                    or int(formal_binding[1]) != route_id \
                    or int(formal_binding[2]) != route_process_id \
                    or formal_binding[3] != "BATCH" \
                    or formal_binding[4] != batch_record_report_id \
                    or int(formal_binding[5]) != batch_record_definition_id \
                    or int(formal_binding[6]) != batch_record_version_id \
                    or formal_binding[7] != "MAIN" \
                    or formal_binding[8] != "BATCH_RECORD" \
                    or int(formal_binding[9]) != 1:
                raise FixtureError("Formal per-process batch-record binding is missing or mismatched")
            cur.execute(
                "SELECT scope_type,scope_id,route_id,batch_record_definition_id,batch_record_version_id,source_type,source_field_code,target_report_id,target_row_index,target_column_index,target_value_type,aggregation_strategy,enabled FROM mes_pro_batch_record_cell_link_rule WHERE tenant_id=%s AND id=%s AND deleted=b'0'",
                (TENANT_ID, int(id_text(owned.get("batchRecordCellLinkRuleId"), "owned.batchRecordCellLinkRuleId"))),
            )
            link_rule = cur.fetchone()
            if not link_rule \
                    or link_rule[0] != "ROUTE_VERSION" \
                    or int(link_rule[1]) != batch_record_version_id \
                    or int(link_rule[2]) != route_id \
                    or int(link_rule[3]) != batch_record_definition_id \
                    or int(link_rule[4]) != batch_record_version_id \
                    or link_rule[5] != "PROCESS_POOL_REPORT" \
                    or link_rule[6] != "allocatedQuantity" \
                    or link_rule[7] != batch_record_report_id \
                    or int(link_rule[8]) != batch_record_target_row_index \
                    or int(link_rule[9]) != batch_record_target_column_index \
                    or link_rule[10] != "NUMBER" \
                    or link_rule[11] != "SUM" \
                    or bit_int(link_rule[12]) != 1:
                raise FixtureError("Formal batch-record quantity backfill rule is missing or mismatched")
            cur.execute(
                "SELECT employee_profile_id,employee_user_id FROM mes_pro_process_pool_team_employee_binding WHERE tenant_id=%s AND id=%s AND leader_user_id=%s AND process_id=%s AND enabled=b'1' AND deleted=b'0'",
                (TENANT_ID, int(id_text(owned.get("employeeBindingId"), "owned.employeeBindingId")), leader_user_id, process_id),
            )
            binding = cur.fetchone()
            if not binding or int(binding[0]) != int(id_text(owned.get("employeeProfileId"), "owned.employeeProfileId")) or int(binding[1]) != frontline_user_id:
                raise FixtureError("Formal frontline employee binding is missing")
            cur.execute(
                "SELECT id,work_order_id,erp_fixed_quantity_snapshot,active_status,business_status,sort_order FROM mes_pro_process_pool_active_order WHERE tenant_id=%s AND id IN (%s,%s) AND leader_user_id=%s AND route_id=%s AND deleted=b'0' ORDER BY sort_order",
                (TENANT_ID, *active_order_ids, leader_user_id, route_id),
            )
            orders = cur.fetchall()
            if len(orders) != 2 or [int(row[0]) for row in orders] != active_order_ids:
                raise FixtureError("O1/O2 active orders are missing or reordered")
            if [int(row[2]) for row in orders] != [6, 20] or any(row[3] != "ACTIVE" or row[4] != "ACTIVE" for row in orders):
                raise FixtureError("O1/O2 capacity or active state mismatch")
            schedule_order_ids = id_list(owned.get("scheduleOrderIds") or [], "owned.scheduleOrderIds")
            schedule_order_process_ids = id_list(owned.get("scheduleOrderProcessIds") or [], "owned.scheduleOrderProcessIds")
            if len(schedule_order_ids) != 2 or len(schedule_order_process_ids) != 2:
                raise FixtureError("Fixture must own exactly two schedule orders and schedule order processes")
            cur.execute(
                "SELECT id,work_order_id,route_id,route_version_id,scheduled_quantity,reported_quantity FROM mes_pro_schedule_order WHERE tenant_id=%s AND id IN (%s,%s) AND deleted=b'0' ORDER BY work_order_id",
                (TENANT_ID, *schedule_order_ids),
            )
            schedule_orders = cur.fetchall()
            if len(schedule_orders) != 2 or [int(row[1]) for row in schedule_orders] != [int(value) for value in manifest["owned"]["workOrderIds"]]:
                raise FixtureError("O1/O2 schedule orders are missing or not bound to the owned work orders")
            if any(int(row[2]) != route_id or int(row[3]) != int(id_text(owned.get("routeVersionId"), "owned.routeVersionId"))
                   or int(row[4]) not in (6, 20) or int(row[5]) != 0 for row in schedule_orders):
                raise FixtureError("O1/O2 schedule order route or quantity snapshot is mismatched")
            cur.execute(
                "SELECT id,schedule_order_id,source_work_order_id,route_id,route_process_id,process_id,planned_quantity,reported_quantity FROM mes_pro_schedule_order_process WHERE tenant_id=%s AND id IN (%s,%s) AND deleted=b'0' ORDER BY source_work_order_id",
                (TENANT_ID, *schedule_order_process_ids),
            )
            schedule_processes = cur.fetchall()
            if len(schedule_processes) != 2:
                raise FixtureError("O1/O2 schedule order processes are missing")
            if [int(row[1]) for row in schedule_processes] != schedule_order_ids \
                    or [int(row[2]) for row in schedule_processes] != [int(value) for value in manifest["owned"]["workOrderIds"]] \
                    or any(int(row[3]) != route_id or int(row[4]) != route_process_id or int(row[5]) != process_id
                           or int(row[6]) not in (6, 20) or int(row[7]) != 0 for row in schedule_processes):
                raise FixtureError("O1/O2 schedule order process identity or quantity is mismatched")
            cur.execute(
                "SELECT active_order_id,route_process_id,process_id,planned_quantity_snapshot FROM mes_pro_process_pool_active_order_process_snapshot WHERE tenant_id=%s AND id IN (%s,%s) AND deleted=b'0' ORDER BY active_order_id",
                (TENANT_ID, *active_snapshot_ids),
            )
            process_snapshots = cur.fetchall()
            if len(process_snapshots) != 2 or any(int(row[1]) != route_process_id or int(row[2]) != process_id for row in process_snapshots):
                raise FixtureError("O1/O2 route-process snapshots are missing or mismatched")
            if int(manifest["context"]["submitQuantity"]) <= int(manifest["orders"]["o1"]["plannedQuantity"]):
                raise FixtureError("O1 capacity must be lower than the submit quantity")
    finally:
        conn.close()
    result = {
        "status": "READY", "fixtureVerified": True, "permissionsVerified": True,
        "taskDataVerified": True, "cleanupReady": True, "taskId": TASK_ID,
        "runId": manifest["runId"], "tenantId": str(TENANT_ID),
    }
    if protected_baseline is not None:
        result.update({
            "protectedBaselineVerified": True,
            "protectedBaselineFingerprint": protected_baseline["fingerprint"],
        })
    return result


def delete_ids(cur, table: str, ids: list[int]) -> int:
    if not ids:
        return 0
    if table not in REQUIRED_TABLES:
        raise FixtureError(f"Table is not in the fixture allowlist: {table}")
    cur.execute(
        f"DELETE FROM `{table}` WHERE tenant_id=%s AND id IN ({','.join(['%s'] * len(ids))})",
        (TENANT_ID, *ids),
    )
    return int(cur.rowcount)


def cleanup_fixture(manifest: dict[str, Any], scenario_state: dict[str, Any] | None) -> dict[str, Any]:
    validate_manifest(manifest)
    if scenario_state:
        if scenario_state.get("taskId") != TASK_ID or scenario_state.get("runId") != manifest["runId"] or int(id_text(scenario_state.get("tenantId"), "scenario tenantId")) != TENANT_ID:
            raise FixtureError("Scenario state is not owned by this fixture run")
    owned = manifest.get("owned") or {}
    admin_mode = FIXTURE_MODE == "ADMIN_TENANT1"
    user_ids = id_list(owned.get("userIds") or [], "owned.userIds")
    role_ids = id_list(owned.get("roleIds") or [], "owned.roleIds")
    work_order_ids = id_list(owned.get("workOrderIds") or [], "owned.workOrderIds")
    schedule_order_ids = id_list(owned.get("scheduleOrderIds") or [], "owned.scheduleOrderIds")
    schedule_order_process_ids = id_list(owned.get("scheduleOrderProcessIds") or [], "owned.scheduleOrderProcessIds")
    active_order_ids = id_list(owned.get("activeOrderIds") or [], "owned.activeOrderIds")
    feedback_code = str(manifest["context"]["feedbackCode"])
    batch_record_report_id = str(manifest["context"].get("batchRecordReportId") or "").strip()
    route_flow_config_id = int(id_text(owned.get("routeFlowConfigId"), "owned.routeFlowConfigId"))
    route_flow_process_config_id = int(id_text(
        owned.get("routeFlowProcessConfigId"), "owned.routeFlowProcessConfigId"
    ))
    route_flow_process_batch_record_id = int(id_text(
        owned.get("routeFlowProcessBatchRecordId"), "owned.routeFlowProcessBatchRecordId"
    ))
    batch_record_cell_link_rule_id = int(id_text(
        owned.get("batchRecordCellLinkRuleId"), "owned.batchRecordCellLinkRuleId"
    ))
    event_ids: list[int] = []
    feedback_ids: list[int] = []
    pool_ids: list[int] = []
    signature_ids: list[int] = []
    review_ids: list[int] = []
    execution_ids: list[int] = []
    protected_baseline_before = (manifest.get("protectedBaseline") or {}).get("fingerprint") if admin_mode else None
    if scenario_state and scenario_state.get("eventId"):
        event_ids.append(int(id_text(scenario_state["eventId"], "scenario eventId")))
    conn = connect(read_local_mysql_config(workspace_root()))
    deleted_count = 0
    retained_edhr_audit_evidence_count = 0
    protected_baseline_after: dict[str, Any] | None = None
    conn.begin()
    try:
        with conn.cursor() as cur:
            if admin_mode:
                verify_admin_protected_baseline(cur, manifest)
            cur.execute(
                "SELECT id FROM mes_pro_feedback WHERE tenant_id=%s AND code=%s",
                (TENANT_ID, feedback_code),
            )
            feedback_ids = [int(row[0]) for row in cur.fetchall()]
            if feedback_ids:
                cur.execute(
                    "SELECT id FROM mes_pro_process_pool_event WHERE tenant_id=%s AND feedback_source_id IN ({})".format(
                        ",".join(["%s"] * len(feedback_ids))
                    ),
                    (TENANT_ID, *feedback_ids),
                )
                event_ids.extend(int(row[0]) for row in cur.fetchall())
            event_ids = sorted(set(event_ids))
            if event_ids:
                cur.execute(
                    "SELECT DISTINCT pool_id,signature_id FROM mes_pro_process_pool_event WHERE tenant_id=%s AND id IN ({})".format(
                        ",".join(["%s"] * len(event_ids))
                    ),
                    (TENANT_ID, *event_ids),
                )
                for pool_id, signature_id in cur.fetchall():
                    if pool_id:
                        pool_ids.append(int(pool_id))
                    if signature_id:
                        signature_ids.append(int(signature_id))
                cur.execute(
                    "SELECT id,review_signature_id FROM mes_pro_process_pool_submission_review WHERE tenant_id=%s AND event_id IN ({})".format(
                        ",".join(["%s"] * len(event_ids))
                    ),
                    (TENANT_ID, *event_ids),
                )
                for review_id, review_signature_id in cur.fetchall():
                    review_ids.append(int(review_id))
                    if review_signature_id:
                        signature_ids.append(int(review_signature_id))
                for table in [
                    "mes_pro_process_pool_report_allocation_adjustment_audit",
                    "mes_pro_process_pool_report_allocation_state",
                    "mes_pro_process_pool_report_allocation",
                ]:
                    cur.execute(
                        f"DELETE FROM `{table}` WHERE tenant_id=%s AND event_id IN ({','.join(['%s'] * len(event_ids))})",
                        (TENANT_ID, *event_ids),
                    )
                    deleted_count += int(cur.rowcount)
                deleted_count += delete_ids(cur, "mes_pro_process_pool_submission_review", review_ids)
                cur.execute(
                    f"DELETE FROM `mes_pro_process_pool_fifo_allocation_line` WHERE tenant_id=%s AND source_event_id IN ({','.join(['%s'] * len(event_ids))})",
                    (TENANT_ID, *event_ids),
                )
                deleted_count += int(cur.rowcount)
                cur.execute(
                    f"DELETE FROM `mes_pro_process_pool_quantity_fragment` WHERE tenant_id=%s AND (event_id IN ({','.join(['%s'] * len(event_ids))}) OR production_submit_event_id IN ({','.join(['%s'] * len(event_ids))}))",
                    (TENANT_ID, *event_ids, *event_ids),
                )
                deleted_count += int(cur.rowcount)
                deleted_count += delete_ids(cur, "mes_pro_process_pool_event", event_ids)
            cur.execute(
                "SELECT id FROM mes_pro_batch_record_execution WHERE tenant_id=%s AND work_order_id IN (%s,%s) AND route_binding_id=%s AND batch_record_report_id=%s",
                (TENANT_ID, *work_order_ids, route_flow_process_batch_record_id, batch_record_report_id),
            )
            execution_ids = [int(row[0]) for row in cur.fetchall()]
            if execution_ids:
                cur.execute(
                    "SELECT id FROM mes_pro_batch_record_execution_signature WHERE tenant_id=%s AND execution_id IN ({})".format(
                        ",".join(["%s"] * len(execution_ids))
                    ),
                    (TENANT_ID, *execution_ids),
                )
                signature_ids.extend(int(row[0]) for row in cur.fetchall())
            if work_order_ids:
                cur.execute(
                    "DELETE FROM `mes_pro_process_pool_order_process_completion` WHERE tenant_id=%s AND work_order_id IN (%s,%s)",
                    (TENANT_ID, *work_order_ids),
                )
                deleted_count += int(cur.rowcount)
                cur.execute(
                    "SELECT id FROM mes_pro_process_pool WHERE tenant_id=%s AND work_order_id IN (%s,%s)",
                    (TENANT_ID, *work_order_ids),
                )
                pool_ids.extend(int(row[0]) for row in cur.fetchall())
            deleted_count += delete_ids(cur, "mes_pro_process_pool", sorted(set(pool_ids)))
            deleted_count += delete_ids(cur, "mes_pro_feedback", feedback_ids)
            cur.execute(
                "DELETE FROM `mes_md_auto_code_record` WHERE tenant_id=%s AND result=%s",
                (TENANT_ID, feedback_code),
            )
            deleted_count += int(cur.rowcount)
            if admin_mode:
                run_marker = f"%{manifest['runId']}%"
                cur.execute(
                    "DELETE FROM `system_operate_log` WHERE tenant_id=%s AND (action LIKE %s OR extra LIKE %s)",
                    (TENANT_ID, run_marker, run_marker),
                )
                deleted_count += int(cur.rowcount)
            else:
                cur.execute(
                    "DELETE FROM `system_oauth2_access_token` WHERE tenant_id=%s AND user_id IN (%s,%s)",
                    (TENANT_ID, *user_ids),
                )
                deleted_count += int(cur.rowcount)
                cur.execute(
                    "DELETE FROM `system_oauth2_refresh_token` WHERE tenant_id=%s AND user_id IN (%s,%s)",
                    (TENANT_ID, *user_ids),
                )
                deleted_count += int(cur.rowcount)
                cur.execute(
                    "DELETE FROM `system_login_log` WHERE tenant_id=%s AND (user_id IN (%s,%s) OR username IN (%s,%s))",
                    (TENANT_ID, *user_ids, manifest["accounts"]["frontlineUsername"], manifest["accounts"]["leaderUsername"]),
                )
                deleted_count += int(cur.rowcount)
                cur.execute(
                    "DELETE FROM `system_operate_log` WHERE tenant_id=%s AND user_id IN (%s,%s)",
                    (TENANT_ID, *user_ids),
                )
                deleted_count += int(cur.rowcount)
                cur.execute(
                    "DELETE FROM `dcc_electronic_signature_authorization_audit` WHERE tenant_id=%s AND (target_user_id IN (%s,%s) OR operator_id IN (%s,%s))",
                    (TENANT_ID, *user_ids, *user_ids),
                )
                deleted_count += int(cur.rowcount)
            for table, values in [
                ("mes_pro_process_pool_active_order_process_snapshot", id_list(owned.get("activeSnapshotIds") or [], "owned.activeSnapshotIds")),
                ("mes_pro_process_pool_active_order", active_order_ids),
                ("mes_pro_schedule_order_process", schedule_order_process_ids),
                ("mes_pro_schedule_order", schedule_order_ids),
                ("mes_pro_work_order", work_order_ids),
                ("mes_pro_route_version", [int(id_text(owned.get("routeVersionId"), "owned.routeVersionId"))]),
                ("mes_pro_batch_record_cell_link_rule", [batch_record_cell_link_rule_id]),
                ("mes_pro_route_flow_process_batch_record", [route_flow_process_batch_record_id]),
                ("mes_pro_route_flow_process_config", [route_flow_process_config_id]),
                ("mes_pro_route_flow_config", [route_flow_config_id]),
                ("mes_pro_route_process", [int(id_text(owned.get("routeProcessId"), "owned.routeProcessId"))]),
                ("mes_pro_process_pool_team_employee_binding", [int(id_text(owned.get("employeeBindingId"), "owned.employeeBindingId"))]),
                ("mes_pro_process_pool_team_employee_profile", [int(id_text(owned.get("employeeProfileId"), "owned.employeeProfileId"))]),
                ("mes_pro_route", [int(id_text(owned.get("routeId"), "owned.routeId"))]),
                ("mes_md_workstation", [int(id_text(owned.get("workstationId"), "owned.workstationId"))]),
                ("mes_pro_process", [int(id_text(owned.get("processId"), "owned.processId"))]),
                ("mes_md_item", [int(id_text(owned.get("itemId"), "owned.itemId"))]),
                ("dcc_electronic_signature_authorization", id_list(owned.get("signatureGrantIds") or [], "owned.signatureGrantIds")),
                ("system_user_role", id_list(owned.get("userRoleIds") or [], "owned.userRoleIds")),
                ("system_role_menu", id_list(owned.get("roleMenuIds") or [], "owned.roleMenuIds")),
                ("system_users", user_ids),
                ("system_role", role_ids),
            ]:
                deleted_count += delete_ids(cur, table, values)
            if admin_mode:
                protected_baseline_after = verify_admin_protected_baseline(cur, manifest)
        conn.commit()
    except Exception:
        conn.rollback()
        raise

    try:
        remaining_task_data_count = 0
        with conn.cursor() as cur:
            checks = [
                ("system_users", user_ids), ("system_role", role_ids),
                ("mes_pro_work_order", work_order_ids), ("mes_pro_process_pool_active_order", active_order_ids),
                ("mes_pro_schedule_order", schedule_order_ids),
                ("mes_pro_schedule_order_process", schedule_order_process_ids),
                ("mes_pro_process_pool_active_order_process_snapshot", id_list(owned.get("activeSnapshotIds") or [], "owned.activeSnapshotIds")),
                ("mes_pro_route", [int(id_text(owned.get("routeId"), "owned.routeId"))]),
                ("mes_pro_route_process", [int(id_text(owned.get("routeProcessId"), "owned.routeProcessId"))]),
                ("mes_pro_route_version", [int(id_text(owned.get("routeVersionId"), "owned.routeVersionId"))]),
                ("mes_pro_batch_record_cell_link_rule", [batch_record_cell_link_rule_id]),
                ("mes_pro_route_flow_process_batch_record", [route_flow_process_batch_record_id]),
                ("mes_pro_route_flow_process_config", [route_flow_process_config_id]),
                ("mes_pro_route_flow_config", [route_flow_config_id]),
            ]
            remaining_task_data_count += sum(count_exact(cur, table, values) for table, values in checks)
            remaining_task_data_count += int(one(cur, "SELECT COUNT(*) FROM mes_pro_feedback WHERE tenant_id=%s AND code=%s", (TENANT_ID, feedback_code)) or 0)
            remaining_task_data_count += int(one(cur, "SELECT COUNT(*) FROM mes_md_auto_code_record WHERE tenant_id=%s AND result=%s", (TENANT_ID, feedback_code)) or 0)
            if admin_mode:
                run_marker = f"%{manifest['runId']}%"
                remaining_task_data_count += int(one(cur, "SELECT COUNT(*) FROM system_operate_log WHERE tenant_id=%s AND (action LIKE %s OR extra LIKE %s)", (TENANT_ID, run_marker, run_marker)) or 0)
                protected_baseline_after = verify_admin_protected_baseline(cur, manifest)
            else:
                remaining_task_data_count += int(one(cur, "SELECT COUNT(*) FROM system_login_log WHERE tenant_id=%s AND (user_id IN (%s,%s) OR username IN (%s,%s))", (TENANT_ID, *user_ids, manifest["accounts"]["frontlineUsername"], manifest["accounts"]["leaderUsername"])) or 0)
                remaining_task_data_count += int(one(cur, "SELECT COUNT(*) FROM system_operate_log WHERE tenant_id=%s AND user_id IN (%s,%s)", (TENANT_ID, *user_ids)) or 0)
                remaining_task_data_count += int(one(cur, "SELECT COUNT(*) FROM dcc_electronic_signature_authorization_audit WHERE tenant_id=%s AND (target_user_id IN (%s,%s) OR operator_id IN (%s,%s))", (TENANT_ID, *user_ids, *user_ids)) or 0)
            remaining_task_data_count += count_exact(cur, "mes_pro_process_pool_submission_review", review_ids)
            retained_edhr_audit_evidence_count += count_exact(
                cur, "mes_pro_batch_record_execution_signature", sorted(set(signature_ids))
            )
            retained_edhr_audit_evidence_count += count_exact(cur, "mes_pro_batch_record_execution", execution_ids)
            retained_edhr_audit_evidence_count += count_exact(
                cur, "mes_pro_batch_record_execution_field_audit_batch", execution_ids
            )
            retained_edhr_audit_evidence_count += count_exact(
                cur, "mes_pro_batch_record_execution_field_audit_item", execution_ids
            )
            remaining_task_data_count += existing_task_data_count(cur)
            remaining_task_data_count += int(one(
                cur,
                "SELECT COUNT(*) FROM mes_pro_process_pool_order_process_completion WHERE tenant_id=%s AND work_order_id IN (%s,%s)",
                (TENANT_ID, *work_order_ids),
            ) or 0)
            if event_ids:
                remaining_task_data_count += int(one(
                    cur,
                    f"SELECT COUNT(*) FROM mes_pro_process_pool_event WHERE tenant_id=%s AND id IN ({','.join(['%s'] * len(event_ids))})",
                    (TENANT_ID, *event_ids),
                ) or 0)
    finally:
        conn.close()
    result = {
        "status": "CLEAN" if remaining_task_data_count == 0 else "DIRTY",
        "cleanupPerformed": True, "cleanupVerified": remaining_task_data_count == 0,
        "remainingTaskDataCount": remaining_task_data_count, "deletedRowCount": deleted_count,
        "retainedEdhrAuditEvidenceCount": retained_edhr_audit_evidence_count,
        "retainedEdhrAuditEvidenceTables": list(RETAINED_EDHR_AUDIT_EVIDENCE_TABLES),
        "taskId": TASK_ID, "runId": manifest["runId"], "tenantId": str(TENANT_ID),
    }
    if admin_mode and protected_baseline_after is not None:
        result.update({
            "protectedBaselineVerified": protected_baseline_before == protected_baseline_after["fingerprint"],
            "protectedBaselineFingerprintBefore": protected_baseline_before,
            "protectedBaselineFingerprintAfter": protected_baseline_after["fingerprint"],
        })
    return result


def self_test() -> None:
    configure_fixture_profile("STANDARD_TENANT122")
    sample = {
        "schemaVersion": "fas-fixture-v1", "cleanupContract": "fas-cleanup-v1",
        "taskId": TASK_ID, "runId": f"{DATA_PREFIX}SELFTEST", "fixtureMode": FIXTURE_MODE,
        "tenant": {"id": str(TENANT_ID), "name": TENANT_NAME},
    }
    validate_manifest(sample)
    assert id_text("9223372036854775807", "long") == "9223372036854775807"
    assert {5550, 5551, 5552, 900437, 1221}.issubset(set(FRONTLINE_MENU_IDS))
    assert 1221 in LEADER_MENU_IDS
    assert {
        "mes_pro_process_pool_submission_review",
        "system_login_log",
        "system_operate_log",
        "dcc_electronic_signature_authorization_audit",
        "mes_pro_schedule_order",
        "mes_pro_schedule_order_process",
    }.issubset(REQUIRED_TABLES)
    try:
        assert_no_sensitive_keys({"accessToken": "redacted"})
    except FixtureError:
        pass
    else:
        raise AssertionError("Sensitive-key rejection self-test did not fail")
    configure_fixture_profile("ADMIN_TENANT1")
    assert (TENANT_ID, TENANT_NAME, FIXTURE_MODE) == (1, "芋道源码", "ADMIN_TENANT1")
    configure_fixture_profile("STANDARD_TENANT122")
    print(json.dumps({"status": "PASS", "taskId": TASK_ID}, ensure_ascii=False))


def main() -> int:
    args = parse_args()
    if args.self_test:
        self_test()
        return 0
    manifest_path = Path(args.manifest).resolve()
    result_path = Path(args.result).resolve() if args.result else None
    if args.action == "prepare":
        manifest = prepare_fixture(manifest_path, args.fixture_mode)
        result = {
            "status": "PREPARED", "taskId": TASK_ID, "runId": manifest["runId"],
            "tenantId": str(TENANT_ID), "fixtureMode": FIXTURE_MODE, "manifest": str(manifest_path),
        }
        if manifest.get("protectedBaseline"):
            result.update({
                "protectedBaselineVerified": True,
                "protectedBaselineFingerprint": manifest["protectedBaseline"]["fingerprint"],
            })
    else:
        manifest = read_json(manifest_path, "fixture manifest")
        configure_fixture_profile(str(manifest.get("fixtureMode") or ""))
        validate_manifest(manifest)
        scenario_state = read_json(Path(args.scenario_state).resolve(), "scenario state") if args.scenario_state and Path(args.scenario_state).exists() else None
        result = verify_fixture(manifest) if args.action == "verify" else cleanup_fixture(manifest, scenario_state)
    if result_path:
        write_json(result_path, result)
    else:
        print(json.dumps(result, ensure_ascii=False))
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except (FixtureError, pymysql.MySQLError, json.JSONDecodeError) as error:
        print(f"fixture orchestration failed: {error}", file=sys.stderr)
        raise SystemExit(1)
