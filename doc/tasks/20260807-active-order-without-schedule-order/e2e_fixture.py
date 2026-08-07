#!/usr/bin/env python3
from __future__ import annotations

import argparse
import importlib.util
import json
from datetime import datetime, timedelta
from pathlib import Path
from typing import Any


TASK_ID = "20260807-active-order-without-schedule-order"
PREFIX = "AONS-20260807"
TENANT_ID = 122
LEADER_USERNAME = "acd04lead1"
CREATOR = "codex-aons"
WORKSPACE_ROOT = Path(__file__).resolve().parents[3]
SUMMARY_PATH = WORKSPACE_ROOT / "doc" / "tasks" / TASK_ID / "fixture-summary.json"
BASE_FIXTURE_PATH = (
    WORKSPACE_ROOT
    / "doc"
    / "tasks"
    / "20260805-process-loss-reasons"
    / "acd04_simulate_environment.py"
)


class FixtureError(RuntimeError):
    pass


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser()
    parser.add_argument(
        "--mode",
        required=True,
        choices=["preflight", "verify", "seed", "verify-seed", "verify-result", "cleanup", "verify-clean"],
    )
    return parser.parse_args()


def load_config_helper():
    spec = importlib.util.spec_from_file_location("fixture_base", BASE_FIXTURE_PATH)
    if spec is None or spec.loader is None:
        raise FixtureError(f"Cannot load local datasource helper: {BASE_FIXTURE_PATH}")
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


def connect():
    helper = load_config_helper()
    return helper.connect(helper.read_local_mysql_config(WORKSPACE_ROOT))


def fetch_one(cursor, sql: str, params: tuple[Any, ...] = ()) -> tuple[Any, ...] | None:
    cursor.execute(sql, params)
    return cursor.fetchone()


def scalar(cursor, sql: str, params: tuple[Any, ...] = ()) -> Any:
    row = fetch_one(cursor, sql, params)
    return None if row is None else row[0]


def require(condition: bool, message: str) -> None:
    if not condition:
        raise FixtureError(message)


def require_schema(cursor) -> None:
    required_columns = {
        "mes_md_item": {"id", "code", "name", "status", "tenant_id", "deleted"},
        "mes_pro_process": {"id", "code", "name", "status", "tenant_id", "deleted"},
        "mes_pro_route": {"id", "code", "name", "status", "tenant_id", "deleted"},
        "mes_pro_route_process": {"id", "route_id", "process_id", "sort", "tenant_id", "deleted"},
        "mes_pro_route_version": {
            "id", "route_id", "version_no", "active", "lifecycle_status", "route_snapshot_json",
            "active_unique_flag", "tenant_id", "deleted",
        },
        "mes_pro_route_product": {"id", "route_id", "item_id", "tenant_id", "deleted"},
        "mes_pro_work_order": {
            "id", "code", "product_id", "quantity", "planned_start_time", "status", "tenant_id", "deleted",
        },
        "mes_qa_inspection_regulation": {
            "id", "product_id", "route_id", "route_version_id", "route_process_id", "process_id",
            "lifecycle_status", "current_version_id", "tenant_id", "deleted",
        },
        "mes_qa_inspection_regulation_version": {
            "id", "regulation_id", "lifecycle_status", "final_inspection_applicable",
            "final_inspection_not_applicable_reason", "snapshot_json", "tenant_id", "deleted",
        },
        "mes_qa_inspection_regulation_item": {
            "id", "regulation_version_id", "inspection_type", "first_inspection_quantity",
            "patrol_inspection_ratio", "tenant_id", "deleted",
        },
        "mes_pro_schedule_order": {"id", "work_order_id", "tenant_id", "deleted"},
        "mes_pro_process_pool_active_order": {"id", "work_order_id", "tenant_id", "deleted"},
        "mes_pro_process_pool_active_order_process_snapshot": {"id", "active_order_id", "tenant_id", "deleted"},
        "mes_pqc_inspection_task": {"id", "active_order_id", "business_date", "tenant_id", "deleted"},
    }
    for table, expected in required_columns.items():
        cursor.execute(
            """
            SELECT column_name
              FROM information_schema.columns
             WHERE table_schema = DATABASE()
               AND table_name = %s
            """,
            (table,),
        )
        actual = {row[0] for row in cursor.fetchall()}
        missing = sorted(expected - actual)
        require(not missing, f"Missing required columns in {table}: {missing}")


def require_test_identity(cursor) -> int:
    rows = []
    cursor.execute(
        """
        SELECT u.id
          FROM system_users u
          JOIN system_tenant t ON t.id = u.tenant_id
         WHERE u.tenant_id = %s
           AND u.username = %s
           AND u.status = 0
           AND u.deleted = b'0'
           AND t.status = 0
           AND t.deleted = b'0'
        """,
        (TENANT_ID, LEADER_USERNAME),
    )
    rows = cursor.fetchall()
    require(len(rows) == 1, "Task requires exactly one enabled local test leader identity")
    return int(rows[0][0])


def fixture_counts(cursor) -> dict[str, int]:
    checks = {
        "item": ("SELECT COUNT(*) FROM mes_md_item WHERE tenant_id=%s AND code=%s", (TENANT_ID, f"{PREFIX}-ITEM")),
        "process": ("SELECT COUNT(*) FROM mes_pro_process WHERE tenant_id=%s AND code=%s", (TENANT_ID, f"{PREFIX}-P1")),
        "route": ("SELECT COUNT(*) FROM mes_pro_route WHERE tenant_id=%s AND code=%s", (TENANT_ID, f"{PREFIX}-R1")),
        "workOrder": ("SELECT COUNT(*) FROM mes_pro_work_order WHERE tenant_id=%s AND code=%s", (TENANT_ID, f"{PREFIX}-WO")),
        "regulation": (
            "SELECT COUNT(*) FROM mes_qa_inspection_regulation WHERE tenant_id=%s AND regulation_code=%s",
            (TENANT_ID, f"{PREFIX}-REG"),
        ),
    }
    return {name: int(scalar(cursor, sql, params) or 0) for name, (sql, params) in checks.items()}


def insert_row(cursor, table: str, values: dict[str, Any]) -> int:
    columns = list(values)
    placeholders = ", ".join(["%s"] * len(columns))
    cursor.execute(
        f"INSERT INTO {table} ({', '.join(columns)}) VALUES ({placeholders})",
        tuple(values[column] for column in columns),
    )
    affected = cursor.rowcount
    require(affected == 1, f"Expected one inserted row in {table}, got {affected}")
    row_id = int(cursor.lastrowid)
    require(row_id > 0, f"Missing generated id for {table}")
    return row_id


def update_one(cursor, sql: str, params: tuple[Any, ...], label: str) -> None:
    cursor.execute(sql, params)
    affected = cursor.rowcount
    require(affected == 1, f"Expected one updated row for {label}, got {affected}")


def audit_values(tenant_id: int = TENANT_ID) -> dict[str, Any]:
    return {"creator": CREATOR, "updater": CREATOR, "tenant_id": tenant_id}


def seed() -> dict[str, Any]:
    connection = connect()
    try:
        with connection.cursor() as cursor:
            require_schema(cursor)
            leader_user_id = require_test_identity(cursor)
            counts = fixture_counts(cursor)
            require(all(value == 0 for value in counts.values()), f"Fixture prefix already exists: {counts}")

            now = datetime.now().replace(microsecond=0)
            planned_end = now + timedelta(days=1)
            process_id = insert_row(cursor, "mes_pro_process", {
                "product_name": f"{PREFIX} Product",
                "code": f"{PREFIX}-P1",
                "name": f"{PREFIX} Process",
                "attention": "Task-owned E2E fixture",
                "status": 0,
                "manual_shift_capacity": 100,
                "remark": f"{PREFIX} fixture",
                **audit_values(),
            })
            item_id = insert_row(cursor, "mes_md_item", {
                "code": f"{PREFIX}-ITEM",
                "name": f"{PREFIX} Product",
                "specification": "E2E",
                "status": 0,
                "batch_flag": 1,
                "remark": f"{PREFIX} fixture",
                **audit_values(),
            })
            route_id = insert_row(cursor, "mes_pro_route", {
                "code": f"{PREFIX}-R1",
                "name": f"{PREFIX} Route",
                "description": "Task-owned no-schedule route",
                "status": 0,
                "remark": f"{PREFIX} fixture",
                **audit_values(),
            })
            route_process_id = insert_row(cursor, "mes_pro_route_process", {
                "route_id": route_id,
                "process_id": process_id,
                "sort": 10,
                "link_type": 1,
                "prepare_time": 0,
                "wait_time": 0,
                "color_code": "#409EFF",
                "key_flag": 1,
                "check_flag": 0,
                "remark": f"{PREFIX} fixture",
                **audit_values(),
            })
            snapshot = {
                "route": {"id": route_id, "code": f"{PREFIX}-R1", "name": f"{PREFIX} Route"},
                "configSnapshots": {
                    "flowGraph": {
                        "nodes": [{
                            "id": str(route_process_id),
                            "routeProcessId": route_process_id,
                            "processId": process_id,
                            "sort": 10,
                            "type": "PROCESS",
                        }],
                        "edges": [],
                    },
                    "scheduleUseConfigs": [{
                        "routeId": route_id,
                        "routeProcessId": route_process_id,
                        "useType": "SCHEDULE",
                        "enabled": True,
                        "productionQuantityFactor": 1,
                    }],
                },
            }
            route_version_id = insert_row(cursor, "mes_pro_route_version", {
                "route_id": route_id,
                "version_no": f"{PREFIX}-V1",
                "active": 1,
                "lifecycle_status": "ACTIVE",
                "route_snapshot_json": json.dumps(snapshot, ensure_ascii=False, separators=(",", ":")),
                "published_by": leader_user_id,
                "published_time": now,
                "remark": f"{PREFIX} fixture",
                **audit_values(),
            })
            route_product_id = insert_row(cursor, "mes_pro_route_product", {
                "route_id": route_id,
                "item_id": item_id,
                "quantity": 100,
                "production_time": 1,
                "time_unit_type": "HOUR",
                "remark": f"{PREFIX} fixture",
                **audit_values(),
            })
            work_order_id = insert_row(cursor, "mes_pro_work_order", {
                "code": f"{PREFIX}-WO",
                "name": f"{PREFIX} Work Order",
                "type": 1,
                "order_source_type": 1,
                "order_source_code": f"{PREFIX}-SRC",
                "product_id": item_id,
                "quantity": 100,
                "quantity_produced": 0,
                "quantity_changed": 0,
                "quantity_scheduled": 0,
                "batch_code": f"{PREFIX}-BATCH",
                "business_status": "CONFIRMED",
                "schedule_status": "UNSCHEDULED",
                "planned_start_time": now,
                "planned_end_time": planned_end,
                "request_date": now,
                "status": 1,
                "remark": f"{PREFIX} fixture",
                **audit_values(),
            })
            regulation_id = insert_row(cursor, "mes_qa_inspection_regulation", {
                "product_id": item_id,
                "route_id": route_id,
                "route_version_id": route_version_id,
                "route_process_id": route_process_id,
                "process_id": process_id,
                "owner_module": "MES",
                "regulation_code": f"{PREFIX}-REG",
                "regulation_name": f"{PREFIX} Regulation",
                "lifecycle_status": "PUBLISHED",
                **audit_values(),
            })
            regulation_version_id = insert_row(cursor, "mes_qa_inspection_regulation_version", {
                "regulation_id": regulation_id,
                "version_no": "V1",
                "lifecycle_status": "PUBLISHED",
                "published_at": now,
                "final_inspection_applicable": 0,
                "final_inspection_not_applicable_reason": "Task fixture excludes final inspection",
                "snapshot_json": json.dumps({"taskId": TASK_ID}, separators=(",", ":")),
                **audit_values(),
            })
            update_one(
                cursor,
                "UPDATE mes_qa_inspection_regulation SET current_version_id=%s, updater=%s WHERE id=%s AND tenant_id=%s",
                (regulation_version_id, CREATOR, regulation_id, TENANT_ID),
                "regulation current version",
            )
            first_item_id = insert_row(cursor, "mes_qa_inspection_regulation_item", {
                "regulation_version_id": regulation_version_id,
                "inspection_type": "FIRST",
                "item_code": f"{PREFIX}-FIRST",
                "item_name": "First inspection",
                "inspection_method": "Task fixture method",
                "standard_text": "Pass",
                "equipment_required": 0,
                "result_type": "BOOLEAN",
                "first_inspection_quantity": 1,
                **audit_values(),
            })
            patrol_item_id = insert_row(cursor, "mes_qa_inspection_regulation_item", {
                "regulation_version_id": regulation_version_id,
                "inspection_type": "PATROL",
                "item_code": f"{PREFIX}-PATROL",
                "item_name": "Patrol inspection",
                "inspection_method": "Task fixture method",
                "standard_text": "Pass",
                "equipment_required": 0,
                "result_type": "BOOLEAN",
                "patrol_inspection_ratio": 0.1,
                **audit_values(),
            })
        connection.commit()
        summary = {
            "taskId": TASK_ID,
            "prefix": PREFIX,
            "tenantId": TENANT_ID,
            "leaderUsername": LEADER_USERNAME,
            "leaderUserId": leader_user_id,
            "workOrderCode": f"{PREFIX}-WO",
            "plannedStartTime": now.isoformat(),
            "ids": {
                "processId": process_id,
                "itemId": item_id,
                "routeId": route_id,
                "routeProcessId": route_process_id,
                "routeVersionId": route_version_id,
                "routeProductId": route_product_id,
                "workOrderId": work_order_id,
                "regulationId": regulation_id,
                "regulationVersionId": regulation_version_id,
                "regulationItemIds": [first_item_id, patrol_item_id],
            },
        }
        SUMMARY_PATH.write_text(json.dumps(summary, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
        return summary
    except Exception:
        connection.rollback()
        raise
    finally:
        connection.close()


def load_fixture(cursor) -> dict[str, Any]:
    work_order = fetch_one(
        cursor,
        "SELECT id, product_id, quantity, planned_start_time, status FROM mes_pro_work_order "
        "WHERE tenant_id=%s AND code=%s AND deleted=b'0'",
        (TENANT_ID, f"{PREFIX}-WO"),
    )
    require(work_order is not None, "Fixture work order is missing")
    work_order_id, item_id, quantity, planned_start, status = work_order
    binding = fetch_one(
        cursor,
        "SELECT id, route_id FROM mes_pro_route_product WHERE tenant_id=%s AND item_id=%s AND deleted=b'0'",
        (TENANT_ID, item_id),
    )
    require(binding is not None, "Fixture product route binding is missing")
    route_product_id, route_id = binding
    versions = []
    cursor.execute(
        "SELECT id, route_snapshot_json FROM mes_pro_route_version "
        "WHERE tenant_id=%s AND route_id=%s AND active=b'1' AND lifecycle_status='ACTIVE' AND deleted=b'0'",
        (TENANT_ID, route_id),
    )
    versions = cursor.fetchall()
    require(len(versions) == 1, f"Expected one ACTIVE route version, got {len(versions)}")
    route_version_id, snapshot_json = versions[0]
    snapshot = json.loads(snapshot_json)
    nodes = snapshot.get("configSnapshots", {}).get("flowGraph", {}).get("nodes", [])
    configs = snapshot.get("configSnapshots", {}).get("scheduleUseConfigs", [])
    require(len(nodes) == 1 and len(configs) == 1, "Fixture ACTIVE route snapshot is incomplete")
    route_process_id = int(nodes[0]["routeProcessId"])
    process_id = int(nodes[0]["processId"])
    schedule_count = int(scalar(
        cursor,
        "SELECT COUNT(*) FROM mes_pro_schedule_order WHERE tenant_id=%s AND work_order_id=%s AND deleted=b'0'",
        (TENANT_ID, work_order_id),
    ) or 0)
    regulation = fetch_one(
        cursor,
        """
        SELECT id, current_version_id
          FROM mes_qa_inspection_regulation
         WHERE tenant_id=%s
           AND product_id=%s
           AND route_id=%s
           AND route_version_id=%s
           AND route_process_id=%s
           AND process_id=%s
           AND lifecycle_status='PUBLISHED'
           AND deleted=b'0'
        """,
        (TENANT_ID, item_id, route_id, route_version_id, route_process_id, process_id),
    )
    require(regulation is not None and regulation[1] is not None, "Fixture published PQC regulation is missing")
    regulation_id, regulation_version_id = regulation
    item_count = int(scalar(
        cursor,
        "SELECT COUNT(*) FROM mes_qa_inspection_regulation_item "
        "WHERE tenant_id=%s AND regulation_version_id=%s AND deleted=b'0'",
        (TENANT_ID, regulation_version_id),
    ) or 0)
    require(item_count == 2, f"Expected two PQC regulation items, got {item_count}")
    require(status == 1, f"Fixture work order is not confirmed: status={status}")
    require(float(quantity) == 100.0, f"Unexpected fixture quantity: {quantity}")
    require(planned_start is not None, "Fixture planned start is missing")
    require(schedule_count == 0, f"Fixture must have zero schedule orders, got {schedule_count}")
    return {
        "workOrderId": int(work_order_id),
        "itemId": int(item_id),
        "routeProductId": int(route_product_id),
        "routeId": int(route_id),
        "routeVersionId": int(route_version_id),
        "routeProcessId": route_process_id,
        "processId": process_id,
        "regulationId": int(regulation_id),
        "regulationVersionId": int(regulation_version_id),
        "plannedStartDate": planned_start.date().isoformat(),
    }


def verify_seed() -> dict[str, Any]:
    connection = connect()
    try:
        with connection.cursor() as cursor:
            require_schema(cursor)
            require_test_identity(cursor)
            return load_fixture(cursor)
    finally:
        connection.close()


def verify_result() -> dict[str, Any]:
    connection = connect()
    try:
        with connection.cursor() as cursor:
            fixture = load_fixture(cursor)
            cursor.execute(
                "SELECT id, leader_user_id, active_status, business_status FROM mes_pro_process_pool_active_order "
                "WHERE tenant_id=%s AND work_order_id=%s AND deleted=b'0'",
                (TENANT_ID, fixture["workOrderId"]),
            )
            orders = cursor.fetchall()
            require(len(orders) == 1, f"Expected one active-order record, got {len(orders)}")
            active_order_id = int(orders[0][0])
            snapshot = fetch_one(
                cursor,
                "SELECT production_quantity_factor_snapshot, planned_quantity_snapshot "
                "FROM mes_pro_process_pool_active_order_process_snapshot "
                "WHERE tenant_id=%s AND active_order_id=%s AND deleted=b'0'",
                (TENANT_ID, active_order_id),
            )
            require(snapshot is not None, "Active-order process snapshot is missing")
            require(float(snapshot[0]) == 1.0, f"Unexpected quantity factor snapshot: {snapshot[0]}")
            require(float(snapshot[1]) == 100.0, f"Unexpected planned quantity snapshot: {snapshot[1]}")
            cursor.execute(
                "SELECT inspection_type, business_date, planned_inspection_quantity "
                "FROM mes_pqc_inspection_task "
                "WHERE tenant_id=%s AND active_order_id=%s AND deleted=b'0' ORDER BY inspection_type, shift_code",
                (TENANT_ID, active_order_id),
            )
            tasks = cursor.fetchall()
            require(len(tasks) == 3, f"Expected three PQC tasks, got {len(tasks)}")
            require(
                all(task[1].isoformat() == fixture["plannedStartDate"] for task in tasks),
                "PQC business date does not match ERP planned start date",
            )
            require({task[0] for task in tasks} == {"FIRST", "PATROL"}, "Unexpected PQC inspection types")
            return {**fixture, "activeOrderId": active_order_id, "pqcTaskCount": len(tasks)}
    finally:
        connection.close()


def delete_where(cursor, table: str, where: str, params: tuple[Any, ...]) -> int:
    cursor.execute(f"DELETE FROM {table} WHERE {where}", params)
    return int(cursor.rowcount)


def cleanup() -> dict[str, int]:
    connection = connect()
    deleted: dict[str, int] = {}
    try:
        with connection.cursor() as cursor:
            fixture = load_fixture(cursor)
            cursor.execute(
                "SELECT id FROM mes_pro_process_pool_active_order WHERE tenant_id=%s AND work_order_id=%s",
                (TENANT_ID, fixture["workOrderId"]),
            )
            active_order_ids = [int(row[0]) for row in cursor.fetchall()]
            for active_order_id in active_order_ids:
                deleted["audit"] = deleted.get("audit", 0) + delete_where(
                    cursor,
                    "mes_pro_process_pool_team_maintenance_audit",
                    "tenant_id=%s AND target_type='ACTIVE_ORDER' AND target_id=%s",
                    (TENANT_ID, active_order_id),
                )
                deleted["pqcTask"] = deleted.get("pqcTask", 0) + delete_where(
                    cursor, "mes_pqc_inspection_task", "tenant_id=%s AND active_order_id=%s", (TENANT_ID, active_order_id)
                )
                deleted["processSnapshot"] = deleted.get("processSnapshot", 0) + delete_where(
                    cursor,
                    "mes_pro_process_pool_active_order_process_snapshot",
                    "tenant_id=%s AND active_order_id=%s",
                    (TENANT_ID, active_order_id),
                )
                deleted["activeOrder"] = deleted.get("activeOrder", 0) + delete_where(
                    cursor, "mes_pro_process_pool_active_order", "tenant_id=%s AND id=%s", (TENANT_ID, active_order_id)
                )
            deleted["regulationItem"] = delete_where(
                cursor,
                "mes_qa_inspection_regulation_item",
                "tenant_id=%s AND regulation_version_id=%s",
                (TENANT_ID, fixture["regulationVersionId"]),
            )
            update_one(
                cursor,
                "UPDATE mes_qa_inspection_regulation SET current_version_id=NULL WHERE tenant_id=%s AND id=%s",
                (TENANT_ID, fixture["regulationId"]),
                "clear regulation current version",
            )
            deleted["regulationVersion"] = delete_where(
                cursor,
                "mes_qa_inspection_regulation_version",
                "tenant_id=%s AND id=%s",
                (TENANT_ID, fixture["regulationVersionId"]),
            )
            deleted["regulation"] = delete_where(
                cursor, "mes_qa_inspection_regulation", "tenant_id=%s AND id=%s", (TENANT_ID, fixture["regulationId"])
            )
            deleted["workOrder"] = delete_where(
                cursor, "mes_pro_work_order", "tenant_id=%s AND id=%s", (TENANT_ID, fixture["workOrderId"])
            )
            deleted["routeProduct"] = delete_where(
                cursor, "mes_pro_route_product", "tenant_id=%s AND id=%s", (TENANT_ID, fixture["routeProductId"])
            )
            deleted["routeVersion"] = delete_where(
                cursor, "mes_pro_route_version", "tenant_id=%s AND id=%s", (TENANT_ID, fixture["routeVersionId"])
            )
            deleted["routeProcess"] = delete_where(
                cursor, "mes_pro_route_process", "tenant_id=%s AND id=%s", (TENANT_ID, fixture["routeProcessId"])
            )
            deleted["route"] = delete_where(
                cursor, "mes_pro_route", "tenant_id=%s AND id=%s", (TENANT_ID, fixture["routeId"])
            )
            deleted["item"] = delete_where(
                cursor, "mes_md_item", "tenant_id=%s AND id=%s", (TENANT_ID, fixture["itemId"])
            )
            deleted["process"] = delete_where(
                cursor, "mes_pro_process", "tenant_id=%s AND id=%s", (TENANT_ID, fixture["processId"])
            )
            require(deleted["regulationItem"] == 2, "Cleanup did not remove both regulation items")
            for key in [
                "regulationVersion", "regulation", "workOrder", "routeProduct", "routeVersion",
                "routeProcess", "route", "item", "process",
            ]:
                require(deleted[key] == 1, f"Cleanup expected one {key} row, got {deleted[key]}")
        connection.commit()
        if SUMMARY_PATH.exists():
            SUMMARY_PATH.unlink()
        return deleted
    except Exception:
        connection.rollback()
        raise
    finally:
        connection.close()


def verify_clean() -> dict[str, int]:
    connection = connect()
    try:
        with connection.cursor() as cursor:
            counts = fixture_counts(cursor)
            require(all(value == 0 for value in counts.values()), f"Fixture cleanup is incomplete: {counts}")
            return counts
    finally:
        connection.close()


def preflight() -> dict[str, Any]:
    connection = connect()
    try:
        with connection.cursor() as cursor:
            require_schema(cursor)
            leader_user_id = require_test_identity(cursor)
            counts = fixture_counts(cursor)
            require(all(value == 0 for value in counts.values()), f"Fixture prefix already exists: {counts}")
            return {"tenantId": TENANT_ID, "leaderUserId": leader_user_id, "existingCounts": counts}
    finally:
        connection.close()


def main() -> int:
    args = parse_args()
    actions = {
        "preflight": preflight,
        "verify": verify_seed,
        "seed": seed,
        "verify-seed": verify_seed,
        "verify-result": verify_result,
        "cleanup": cleanup,
        "verify-clean": verify_clean,
    }
    try:
        result = actions[args.mode]()
        print(json.dumps({"status": "PASS", "mode": args.mode, "result": result}, ensure_ascii=False, default=str))
        return 0
    except Exception as error:
        print(json.dumps({"status": "FAIL", "mode": args.mode, "error": str(error)}, ensure_ascii=False))
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
