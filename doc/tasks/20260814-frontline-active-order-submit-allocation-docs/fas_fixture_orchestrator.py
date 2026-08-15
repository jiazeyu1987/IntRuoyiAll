#!/usr/bin/env python3
"""Own, verify, and remove the fixed-tenant P5 real-E2E fixture.

The manifest contains identifiers and public fixture labels only. Database
connection material and login material are read at runtime and never emitted.
"""

from __future__ import annotations

import argparse
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
    "mes_pro_process_pool_team_employee_profile",
    "mes_pro_process_pool_team_employee_binding",
    "mes_pro_work_order",
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
    "mes_pro_batch_record_execution_signature",
    "mes_md_auto_code_record",
}
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
    parser.add_argument("--self-test", action="store_true")
    args = parser.parse_args()
    if not args.self_test and not args.action:
        parser.error("action is required")
    if args.action and not args.manifest:
        parser.error("--manifest is required")
    if args.action in {"verify", "cleanup"} and not args.result:
        parser.error("--result is required")
    return args


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
    run_id = str(manifest.get("runId") or "")
    if not run_id.startswith(DATA_PREFIX):
        raise FixtureError("Manifest runId is not task-owned")
    if int(id_text(manifest.get("tenant", {}).get("id"), "tenant.id")) != TENANT_ID:
        raise FixtureError("Manifest tenant.id mismatch")
    if manifest.get("tenant", {}).get("name") != TENANT_NAME:
        raise FixtureError("Manifest tenant.name mismatch")
    assert_no_sensitive_keys(manifest)


def base_values(tenant_id: int = TENANT_ID) -> dict[str, Any]:
    return {"creator": CREATOR, "updater": CREATOR, "tenant_id": tenant_id}


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
        ("SELECT COUNT(*) FROM mes_pro_process_pool_team_employee_profile WHERE tenant_id=%s AND (creator=%s OR employee_code LIKE %s OR remark LIKE %s)", (TENANT_ID, CREATOR, prefix_like, prefix_like)),
        ("SELECT COUNT(*) FROM mes_pro_process_pool_team_employee_binding WHERE tenant_id=%s AND (creator=%s OR remark LIKE %s)", (TENANT_ID, CREATOR, prefix_like)),
        ("SELECT COUNT(*) FROM mes_pro_work_order WHERE tenant_id=%s AND (creator=%s OR code LIKE %s OR remark LIKE %s)", (TENANT_ID, CREATOR, prefix_like, prefix_like)),
        ("SELECT COUNT(*) FROM mes_pro_process_pool_active_order WHERE tenant_id=%s AND (creator=%s OR remark LIKE %s)", (TENANT_ID, CREATOR, prefix_like)),
        ("SELECT COUNT(*) FROM mes_pro_process_pool_active_order_process_snapshot WHERE tenant_id=%s AND creator=%s", (TENANT_ID, CREATOR)),
        ("SELECT COUNT(*) FROM mes_pro_feedback WHERE tenant_id=%s AND code LIKE %s", (TENANT_ID, prefix_like)),
        ("SELECT COUNT(*) FROM mes_md_auto_code_record WHERE tenant_id=%s AND result LIKE %s", (TENANT_ID, prefix_like)),
        ("SELECT COUNT(*) FROM mes_pro_process_pool_event WHERE tenant_id=%s AND CAST(raw_payload AS CHAR) LIKE %s", (TENANT_ID, f"%{DATA_PREFIX}%")),
        ("SELECT COUNT(*) FROM system_login_log WHERE tenant_id=%s AND (username LIKE 'fasfl%%' OR username LIKE 'fasld%%')", (TENANT_ID,)),
        ("SELECT COUNT(*) FROM system_operate_log WHERE tenant_id=%s AND (action LIKE %s OR extra LIKE %s)", (TENANT_ID, f"%{DATA_PREFIX}%", f"%{DATA_PREFIX}%")),
    ]
    return sum(int(one(cur, sql, params) or 0) for sql, params in checks)


def prepare_fixture(manifest_path: Path) -> dict[str, Any]:
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
            if not tenant or tenant[0] != TENANT_NAME or int(tenant[1]) != 0 or int(tenant[2]) != TENANT_PACKAGE_ID:
                raise FixtureError("Fixed local test tenant 122 is missing, disabled, renamed, or assigned to another package")
            menu_ids = sorted(set(FRONTLINE_MENU_IDS + LEADER_MENU_IDS))
            cur.execute(
                "SELECT id FROM system_menu WHERE id IN ({}) AND status=0 AND deleted=b'0'".format(
                    ",".join(["%s"] * len(menu_ids))
                ),
                tuple(menu_ids),
            )
            if {int(row[0]) for row in cur.fetchall()} != set(menu_ids):
                raise FixtureError("Required frontline or production-leader menus are unavailable")
            cur.execute(
                "SELECT password FROM system_users WHERE tenant_id=%s AND username=%s AND status=0 AND deleted=b'0' LIMIT 1",
                (TENANT_ID, "admin"),
            )
            hash_source = cur.fetchone()
            if not hash_source or not hash_source[0]:
                raise FixtureError("The fixed test tenant local-default hash source is unavailable")

            role_ids: list[int] = []
            role_menu_ids: list[int] = []
            user_ids: list[int] = []
            user_role_ids: list[int] = []
            signature_grant_ids: list[int] = []

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
            route_process_id = insert_row(cur, "mes_pro_route_process", {
                "route_id": route_id, "process_id": process_id, "workstation_id": workstation_id,
                "sort": 1, "link_type": 1, "prepare_time": 0, "wait_time": 0,
                "color_code": "#409EFF", "key_flag": 1, "check_flag": 0,
                "remark": f"{run_id} route process", **base_values(),
            })
            route_snapshot = {
                "route": {"id": route_id, "code": f"{run_code}-R", "name": "FAS E2E工艺路线"},
                "configSnapshots": {"flowGraph": {"nodes": [{
                    "id": str(route_process_id), "routeProcessId": route_process_id,
                    "processId": process_id, "type": "PROCESS",
                }], "edges": []}, "routeStartProductionLeaders": [{
                    "productionLineId": route_id, "candidateSourceType": "USERS",
                    "candidateSourceIds": [user_ids[1]],
                }]},
            }
            route_version_id = insert_row(cur, "mes_pro_route_version", {
                "route_id": route_id, "version_no": "1.0", "active": 1, "lifecycle_status": "ACTIVE",
                "route_snapshot_json": json.dumps(route_snapshot, ensure_ascii=False, separators=(",", ":")),
                "published_by": user_ids[1], "published_time": now.replace(tzinfo=None),
                "remark": f"{run_id} active route version",
                **base_values(),
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
                    "process_id": process_id, "erp_fixed_quantity_snapshot": planned_quantity,
                    "production_quantity_factor_snapshot": 1, "planned_quantity_snapshot": planned_quantity,
                    **base_values(),
                })
                work_order_ids.append(work_order_id)
                work_order_codes.append(work_order_code)
                active_order_ids.append(active_order_id)
                active_snapshot_ids.append(active_snapshot_id)

            feedback_code = f"{run_id}-Q"
            manifest = {
                "schemaVersion": "fas-fixture-v1", "cleanupContract": "fas-cleanup-v1",
                "taskId": TASK_ID, "runId": run_id, "preparedAt": now.isoformat(),
                "tenant": {"id": str(TENANT_ID), "name": TENANT_NAME},
                "accounts": {"frontlineUsername": frontline_username, "leaderUsername": leader_username},
                "orders": {
                    "o1": {"activeOrderId": str(active_order_ids[0]), "workOrderId": str(work_order_ids[0]), "workOrderCode": work_order_codes[0], "plannedQuantity": planned_quantities[0]},
                    "o2": {"activeOrderId": str(active_order_ids[1]), "workOrderId": str(work_order_ids[1]), "workOrderCode": work_order_codes[1], "plannedQuantity": planned_quantities[1]},
                },
                "context": {
                    "routeId": str(route_id), "routeProcessId": str(route_process_id), "processId": str(process_id),
                    "actualEmployeeId": str(user_ids[0]), "submitQuantity": 10, "feedbackCode": feedback_code,
                },
                "owned": {
                    "userIds": [str(value) for value in user_ids], "roleIds": [str(value) for value in role_ids],
                    "roleMenuIds": [str(value) for value in role_menu_ids], "userRoleIds": [str(value) for value in user_role_ids],
                    "signatureGrantIds": [str(value) for value in signature_grant_ids],
                    "processId": str(process_id), "workstationId": str(workstation_id), "routeId": str(route_id),
                    "routeProcessId": str(route_process_id), "routeVersionId": str(route_version_id),
                    "employeeProfileId": str(employee_profile_id), "employeeBindingId": str(employee_binding_id),
                    "itemId": str(item_id), "workOrderIds": [str(value) for value in work_order_ids],
                    "activeOrderIds": [str(value) for value in active_order_ids],
                    "activeSnapshotIds": [str(value) for value in active_snapshot_ids],
                },
            }
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
    if any(len(values) != 2 for values in [user_ids, role_ids, active_order_ids, active_snapshot_ids]):
        raise FixtureError("Fixture must own exactly two users, roles, active orders, and process snapshots")
    conn = connect(read_local_mysql_config(workspace_root()))
    try:
        with conn.cursor() as cur:
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
            cur.execute(
                "SELECT route_snapshot_json FROM mes_pro_route_version WHERE tenant_id=%s AND id=%s AND route_id=%s AND active=b'1' AND lifecycle_status='ACTIVE' AND deleted=b'0'",
                (TENANT_ID, int(id_text(owned.get("routeVersionId"), "owned.routeVersionId")), route_id),
            )
            route_version = cur.fetchone()
            if not route_version:
                raise FixtureError("Task-owned active route version is missing")
            snapshot = json.loads(route_version[0])
            leaders = snapshot.get("configSnapshots", {}).get("routeStartProductionLeaders", [])
            if leaders != [{"productionLineId": route_id, "candidateSourceType": "USERS", "candidateSourceIds": [user_ids[1]]}]:
                raise FixtureError("Route-start production-leader snapshot is not exact")
            flow_nodes = snapshot.get("configSnapshots", {}).get("flowGraph", {}).get("nodes", [])
            if len(flow_nodes) != 1 or int(flow_nodes[0].get("routeProcessId") or 0) != route_process_id \
                    or int(flow_nodes[0].get("processId") or 0) != process_id:
                raise FixtureError("Route-version flowGraph does not contain the exact formal process identity")
            cur.execute(
                "SELECT employee_profile_id,employee_user_id FROM mes_pro_process_pool_team_employee_binding WHERE tenant_id=%s AND id=%s AND leader_user_id=%s AND process_id=%s AND enabled=b'1' AND deleted=b'0'",
                (TENANT_ID, int(id_text(owned.get("employeeBindingId"), "owned.employeeBindingId")), user_ids[1], process_id),
            )
            binding = cur.fetchone()
            if not binding or int(binding[0]) != int(id_text(owned.get("employeeProfileId"), "owned.employeeProfileId")) or int(binding[1]) != user_ids[0]:
                raise FixtureError("Formal frontline employee binding is missing")
            cur.execute(
                "SELECT id,work_order_id,erp_fixed_quantity_snapshot,active_status,business_status,sort_order FROM mes_pro_process_pool_active_order WHERE tenant_id=%s AND id IN (%s,%s) AND leader_user_id=%s AND route_id=%s AND deleted=b'0' ORDER BY sort_order",
                (TENANT_ID, *active_order_ids, user_ids[1], route_id),
            )
            orders = cur.fetchall()
            if len(orders) != 2 or [int(row[0]) for row in orders] != active_order_ids:
                raise FixtureError("O1/O2 active orders are missing or reordered")
            if [int(row[2]) for row in orders] != [6, 20] or any(row[3] != "ACTIVE" or row[4] != "ACTIVE" for row in orders):
                raise FixtureError("O1/O2 capacity or active state mismatch")
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
    return {
        "status": "READY", "fixtureVerified": True, "permissionsVerified": True,
        "taskDataVerified": True, "cleanupReady": True, "taskId": TASK_ID,
        "runId": manifest["runId"], "tenantId": str(TENANT_ID),
    }


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
    user_ids = id_list(owned.get("userIds") or [], "owned.userIds")
    role_ids = id_list(owned.get("roleIds") or [], "owned.roleIds")
    work_order_ids = id_list(owned.get("workOrderIds") or [], "owned.workOrderIds")
    active_order_ids = id_list(owned.get("activeOrderIds") or [], "owned.activeOrderIds")
    feedback_code = str(manifest["context"]["feedbackCode"])
    event_ids: list[int] = []
    feedback_ids: list[int] = []
    pool_ids: list[int] = []
    signature_ids: list[int] = []
    review_ids: list[int] = []
    if scenario_state and scenario_state.get("eventId"):
        event_ids.append(int(id_text(scenario_state["eventId"], "scenario eventId")))
    conn = connect(read_local_mysql_config(workspace_root()))
    deleted_count = 0
    conn.begin()
    try:
        with conn.cursor() as cur:
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
                deleted_count += delete_ids(cur, "mes_pro_batch_record_execution_signature", sorted(set(signature_ids)))
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
                ("mes_pro_work_order", work_order_ids),
                ("mes_pro_route_version", [int(id_text(owned.get("routeVersionId"), "owned.routeVersionId"))]),
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
                ("mes_pro_process_pool_active_order_process_snapshot", id_list(owned.get("activeSnapshotIds") or [], "owned.activeSnapshotIds")),
                ("mes_pro_route", [int(id_text(owned.get("routeId"), "owned.routeId"))]),
                ("mes_pro_route_process", [int(id_text(owned.get("routeProcessId"), "owned.routeProcessId"))]),
                ("mes_pro_route_version", [int(id_text(owned.get("routeVersionId"), "owned.routeVersionId"))]),
            ]
            remaining_task_data_count += sum(count_exact(cur, table, values) for table, values in checks)
            remaining_task_data_count += int(one(cur, "SELECT COUNT(*) FROM mes_pro_feedback WHERE tenant_id=%s AND code=%s", (TENANT_ID, feedback_code)) or 0)
            remaining_task_data_count += int(one(cur, "SELECT COUNT(*) FROM mes_md_auto_code_record WHERE tenant_id=%s AND result=%s", (TENANT_ID, feedback_code)) or 0)
            remaining_task_data_count += int(one(cur, "SELECT COUNT(*) FROM system_login_log WHERE tenant_id=%s AND (user_id IN (%s,%s) OR username IN (%s,%s))", (TENANT_ID, *user_ids, manifest["accounts"]["frontlineUsername"], manifest["accounts"]["leaderUsername"])) or 0)
            remaining_task_data_count += int(one(cur, "SELECT COUNT(*) FROM system_operate_log WHERE tenant_id=%s AND user_id IN (%s,%s)", (TENANT_ID, *user_ids)) or 0)
            remaining_task_data_count += int(one(cur, "SELECT COUNT(*) FROM dcc_electronic_signature_authorization_audit WHERE tenant_id=%s AND (target_user_id IN (%s,%s) OR operator_id IN (%s,%s))", (TENANT_ID, *user_ids, *user_ids)) or 0)
            remaining_task_data_count += count_exact(cur, "mes_pro_process_pool_submission_review", review_ids)
            remaining_task_data_count += count_exact(cur, "mes_pro_batch_record_execution_signature", sorted(set(signature_ids)))
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
    return {
        "status": "CLEAN" if remaining_task_data_count == 0 else "DIRTY",
        "cleanupPerformed": True, "cleanupVerified": remaining_task_data_count == 0,
        "remainingTaskDataCount": remaining_task_data_count, "deletedRowCount": deleted_count,
        "taskId": TASK_ID, "runId": manifest["runId"], "tenantId": str(TENANT_ID),
    }


def self_test() -> None:
    sample = {
        "schemaVersion": "fas-fixture-v1", "cleanupContract": "fas-cleanup-v1",
        "taskId": TASK_ID, "runId": f"{DATA_PREFIX}SELFTEST",
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
    }.issubset(REQUIRED_TABLES)
    try:
        assert_no_sensitive_keys({"accessToken": "redacted"})
    except FixtureError:
        pass
    else:
        raise AssertionError("Sensitive-key rejection self-test did not fail")
    print(json.dumps({"status": "PASS", "taskId": TASK_ID}, ensure_ascii=False))


def main() -> int:
    args = parse_args()
    if args.self_test:
        self_test()
        return 0
    manifest_path = Path(args.manifest).resolve()
    result_path = Path(args.result).resolve() if args.result else None
    if args.action == "prepare":
        manifest = prepare_fixture(manifest_path)
        result = {
            "status": "PREPARED", "taskId": TASK_ID, "runId": manifest["runId"],
            "tenantId": str(TENANT_ID), "manifest": str(manifest_path),
        }
    else:
        manifest = read_json(manifest_path, "fixture manifest")
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
