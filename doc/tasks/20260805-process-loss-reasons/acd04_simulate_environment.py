#!/usr/bin/env python3
"""Prepare local AC-D04 acceptance fixtures for process loss reason checks.

The script reads the local Spring datasource configuration only when
--db-source local-config is provided. It never prints passwords or hashes.
"""

from __future__ import annotations

import argparse
import json
import re
from datetime import datetime
from pathlib import Path
from typing import Any

import pymysql


TASK_ID = "20260805-process-loss-reasons"
TENANT_ID = 122
TENANT_PACKAGE_ID = 113
PREFIX = "ACD04-20260805"
CREATOR = "codex-acd04"

LEADER_MENU_IDS = [5100, 900220, 900436, 900314]
WORKER_MENU_IDS = [5100, 900120, 5550, 5551, 5552]
REQUIRED_PACKAGE_MENU_IDS = sorted(set(LEADER_MENU_IDS + WORKER_MENU_IDS))


class FixtureError(RuntimeError):
    pass


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser()
    parser.add_argument("--db-source", choices=["local-config"], required=True)
    parser.add_argument("--tenant-id", type=int, default=TENANT_ID)
    parser.add_argument("--tenant-package-id", type=int, default=TENANT_PACKAGE_ID)
    parser.add_argument("--prefix", default=PREFIX)
    parser.add_argument("--output", default=f"doc/tasks/{TASK_ID}/fixture-summary.json")
    return parser.parse_args()


def workspace_root() -> Path:
    return Path(__file__).resolve().parents[3]


def read_local_mysql_config(root: Path) -> dict[str, Any]:
    config_path = root / "IntRuoyiBackend" / "yudao-server" / "src" / "main" / "resources" / "application-local.yaml"
    if not config_path.exists():
        raise FixtureError(f"Missing local datasource config: {config_path}")
    lines = config_path.read_text(encoding="utf-8").splitlines()
    for index, raw in enumerate(lines):
        line = strip_yaml_comment(raw).strip()
        match = re.match(r"url:\s*jdbc:mysql://([^:/?#]+):(\d+)/([^?/#]+)", line)
        if not match:
            continue
        username = None
        password = None
        for follow in lines[index + 1:index + 18]:
            item = strip_yaml_comment(follow).strip()
            if not item:
                continue
            if item.startswith("url:"):
                break
            if item.startswith("username:") and username is None:
                username = item.split(":", 1)[1].strip()
            elif item.startswith("password:") and password is None:
                password = item.split(":", 1)[1].strip()
            if username is not None and password is not None:
                return {
                    "host": match.group(1),
                    "port": int(match.group(2)),
                    "database": match.group(3),
                    "user": username,
                    "password": password,
                }
    raise FixtureError("Could not resolve local MySQL datasource from application-local.yaml")


def strip_yaml_comment(text: str) -> str:
    return text.split("#", 1)[0].rstrip()


def connect(config: dict[str, Any]):
    return pymysql.connect(
        host=config["host"],
        port=config["port"],
        user=config["user"],
        password=config["password"],
        database=config["database"],
        charset="utf8mb4",
        autocommit=False,
    )


def one(cur, sql: str, params: tuple[Any, ...] = ()) -> Any:
    cur.execute(sql, params)
    row = cur.fetchone()
    return None if row is None else row[0]


def table_exists(cur, table: str) -> bool:
    return bool(one(
        cur,
        "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name=%s",
        (table,),
    ))


def column_exists(cur, table: str, column: str) -> bool:
    return bool(one(
        cur,
        """
        SELECT COUNT(*)
          FROM information_schema.columns
         WHERE table_schema=DATABASE()
           AND table_name=%s
           AND column_name=%s
        """,
        (table, column),
    ))


def column_nullable(cur, table: str, column: str) -> bool:
    value = one(
        cur,
        """
        SELECT is_nullable
          FROM information_schema.columns
         WHERE table_schema=DATABASE()
           AND table_name=%s
           AND column_name=%s
        """,
        (table, column),
    )
    return value == "YES"


def index_exists(cur, table: str, index: str) -> bool:
    return bool(one(
        cur,
        """
        SELECT COUNT(*)
          FROM information_schema.statistics
         WHERE table_schema=DATABASE()
           AND table_name=%s
           AND index_name=%s
        """,
        (table, index),
    ))


def ensure_schema(conn) -> list[str]:
    operations: list[str] = []
    with conn.cursor() as cur:
        for table in ["mes_pro_process_pool_defect_reason", "mes_pro_feedback"]:
            if not table_exists(cur, table):
                raise FixtureError(f"Missing required table: {table}")

        if index_exists(cur, "mes_pro_process_pool_defect_reason", "uk_mes_pp_defect_reason"):
            cur.execute("DROP INDEX `uk_mes_pp_defect_reason` ON `mes_pro_process_pool_defect_reason`")
            operations.append("drop uk_mes_pp_defect_reason")

        if not column_nullable(cur, "mes_pro_process_pool_defect_reason", "leader_user_id"):
            cur.execute(
                """
                ALTER TABLE `mes_pro_process_pool_defect_reason`
                  MODIFY COLUMN `leader_user_id` bigint DEFAULT NULL
                  COMMENT 'last maintainer user id; LOSS reasons are route-process scoped'
                """
            )
            operations.append("make defect_reason.leader_user_id nullable")

        if not index_exists(cur, "mes_pro_process_pool_defect_reason", "uk_mes_pp_loss_reason_route_process"):
            cur.execute(
                """
                ALTER TABLE `mes_pro_process_pool_defect_reason`
                  ADD UNIQUE KEY `uk_mes_pp_loss_reason_route_process`
                  (`tenant_id`, `route_process_id`, `reason_type`, `reason_code`, `deleted`)
                """
            )
            operations.append("add uk_mes_pp_loss_reason_route_process")

        if not index_exists(cur, "mes_pro_process_pool_defect_reason", "idx_mes_pp_loss_reason_route_process"):
            cur.execute(
                """
                ALTER TABLE `mes_pro_process_pool_defect_reason`
                  ADD KEY `idx_mes_pp_loss_reason_route_process`
                  (`tenant_id`, `route_process_id`, `reason_type`, `enabled`)
                """
            )
            operations.append("add idx_mes_pp_loss_reason_route_process")

        feedback_columns = [
            ("loss_reason_id", "bigint DEFAULT NULL COMMENT 'loss reason id snapshot source' AFTER `other_scrap_quantity`"),
            ("loss_reason_code_snapshot", "varchar(64) DEFAULT NULL COMMENT 'loss reason code snapshot' AFTER `loss_reason_id`"),
            ("loss_reason_name_snapshot", "varchar(255) DEFAULT NULL COMMENT 'loss reason name snapshot' AFTER `loss_reason_code_snapshot`"),
        ]
        for column, ddl in feedback_columns:
            if not column_exists(cur, "mes_pro_feedback", column):
                cur.execute(f"ALTER TABLE `mes_pro_feedback` ADD COLUMN `{column}` {ddl}")
                operations.append(f"add mes_pro_feedback.{column}")
    conn.commit()
    return operations


def validate_schema(conn) -> None:
    with conn.cursor() as cur:
        if not column_nullable(cur, "mes_pro_process_pool_defect_reason", "leader_user_id"):
            raise FixtureError("leader_user_id is still not nullable after schema preparation")
        for column in ["loss_reason_id", "loss_reason_code_snapshot", "loss_reason_name_snapshot"]:
            if not column_exists(cur, "mes_pro_feedback", column):
                raise FixtureError(f"mes_pro_feedback missing column after schema preparation: {column}")
        for index in ["uk_mes_pp_loss_reason_route_process", "idx_mes_pp_loss_reason_route_process"]:
            if not index_exists(cur, "mes_pro_process_pool_defect_reason", index):
                raise FixtureError(f"mes_pro_process_pool_defect_reason missing index: {index}")


def insert_row(cur, table: str, values: dict[str, Any]) -> int:
    columns = list(values)
    sql = (
        f"INSERT INTO `{table}` ("
        + ", ".join(f"`{column}`" for column in columns)
        + ") VALUES ("
        + ", ".join(["%s"] * len(columns))
        + ")"
    )
    cur.execute(sql, tuple(values[column] for column in columns))
    return int(cur.lastrowid)


def update_row(cur, table: str, row_id: int, values: dict[str, Any]) -> None:
    if not values:
        return
    sql = (
        f"UPDATE `{table}` SET "
        + ", ".join(f"`{column}`=%s" for column in values)
        + ", `updater`=%s, `update_time`=NOW() WHERE `id`=%s"
    )
    cur.execute(sql, tuple(values.values()) + (CREATOR, row_id))


def ensure_by_select(
    cur,
    table: str,
    where_sql: str,
    where_params: tuple[Any, ...],
    insert_values: dict[str, Any],
    update_values: dict[str, Any] | None = None,
) -> int:
    cur.execute(f"SELECT id FROM `{table}` WHERE {where_sql} ORDER BY id LIMIT 1", where_params)
    row = cur.fetchone()
    if row:
        row_id = int(row[0])
        update_row(cur, table, row_id, update_values or {})
        return row_id
    return insert_row(cur, table, insert_values)


def ensure_preconditions(cur, tenant_id: int, package_id: int) -> None:
    cur.execute(
        "SELECT name, status, package_id FROM system_tenant WHERE id=%s AND deleted=b'0'",
        (tenant_id,),
    )
    tenant = cur.fetchone()
    if not tenant:
        raise FixtureError(f"Missing tenant {tenant_id}")
    if int(tenant[1]) != 0:
        raise FixtureError(f"Tenant {tenant_id} is not enabled")
    if int(tenant[2]) != package_id:
        raise FixtureError(f"Tenant {tenant_id} package mismatch: expected {package_id}, got {tenant[2]}")

    cur.execute(
        "SELECT menu_ids, status FROM system_tenant_package WHERE id=%s AND deleted=b'0'",
        (package_id,),
    )
    package = cur.fetchone()
    if not package or int(package[1]) != 0:
        raise FixtureError(f"Tenant package {package_id} is missing or disabled")
    package_menu_ids = set(int(value) for value in re.findall(r"\d+", package[0] or ""))
    missing_package_menus = [menu_id for menu_id in REQUIRED_PACKAGE_MENU_IDS if menu_id not in package_menu_ids]
    if missing_package_menus:
        raise FixtureError(f"Tenant package {package_id} missing menus: {missing_package_menus}")

    cur.execute(
        "SELECT id FROM system_menu WHERE id IN ({}) AND deleted=b'0'".format(
            ", ".join(["%s"] * len(REQUIRED_PACKAGE_MENU_IDS))
        ),
        tuple(REQUIRED_PACKAGE_MENU_IDS),
    )
    actual_menu_ids = {int(row[0]) for row in cur.fetchall()}
    missing_menus = [menu_id for menu_id in REQUIRED_PACKAGE_MENU_IDS if menu_id not in actual_menu_ids]
    if missing_menus:
        raise FixtureError(f"Missing required system_menu rows: {missing_menus}")


def ensure_role(cur, tenant_id: int, code: str, name: str, sort: int, menu_ids: list[int]) -> int:
    role_id = ensure_by_select(
        cur,
        "system_role",
        "tenant_id=%s AND code=%s",
        (tenant_id, code),
        {
            "name": name,
            "code": code,
            "sort": sort,
            "category_id": 19,
            "data_scope": 1,
            "data_scope_dept_ids": "[]",
            "status": 0,
            "type": 2,
            "remark": f"{PREFIX} fixture role",
            "creator": CREATOR,
            "updater": CREATOR,
            "tenant_id": tenant_id,
        },
        {"name": name, "sort": sort, "status": 0, "deleted": 0},
    )
    for menu_id in menu_ids:
        ensure_by_select(
            cur,
            "system_role_menu",
            "tenant_id=%s AND role_id=%s AND menu_id=%s",
            (tenant_id, role_id, menu_id),
            {
                "role_id": role_id,
                "menu_id": menu_id,
                "creator": CREATOR,
                "updater": CREATOR,
                "tenant_id": tenant_id,
            },
            {"deleted": 0},
        )
    return role_id


def ensure_user(cur, tenant_id: int, username: str, nickname: str, dept_id: int | None, post_ids_text: str = "[]") -> int:
    password_hash = one(
        cur,
        "SELECT password FROM system_users WHERE tenant_id=%s AND username='admin' AND deleted=b'0' ORDER BY id LIMIT 1",
        (tenant_id,),
    )
    if not password_hash:
        raise FixtureError(f"Missing tenant {tenant_id} admin password hash source")
    return ensure_by_select(
        cur,
        "system_users",
        "tenant_id=%s AND username=%s",
        (tenant_id, username),
        {
            "username": username,
            "password": password_hash,
            "nickname": nickname,
            "dept_id": dept_id,
            "post_ids": post_ids_text,
            "status": 0,
            "remark": f"{PREFIX} fixture user",
            "password_update_time": datetime.now().strftime("%Y-%m-%d %H:%M:%S"),
            "creator": CREATOR,
            "updater": CREATOR,
            "tenant_id": tenant_id,
        },
        {
            "password": password_hash,
            "nickname": nickname,
            "dept_id": dept_id,
            "post_ids": post_ids_text,
            "password_update_time": datetime.now().strftime("%Y-%m-%d %H:%M:%S"),
            "status": 0,
            "deleted": 0,
        },
    )


def ensure_user_role(cur, tenant_id: int, user_id: int, role_id: int) -> None:
    ensure_by_select(
        cur,
        "system_user_role",
        "tenant_id=%s AND user_id=%s AND role_id=%s",
        (tenant_id, user_id, role_id),
        {
            "user_id": user_id,
            "role_id": role_id,
            "creator": CREATOR,
            "updater": CREATOR,
            "tenant_id": tenant_id,
        },
        {"deleted": 0},
    )


def ensure_post(cur, tenant_id: int, code: str, name: str) -> int:
    return ensure_by_select(
        cur,
        "system_post",
        "tenant_id=%s AND code=%s",
        (tenant_id, code),
        {
            "code": code,
            "name": name,
            "sort": 910500,
            "status": 0,
            "remark": f"{PREFIX} fixture post",
            "creator": CREATOR,
            "updater": CREATOR,
            "tenant_id": tenant_id,
        },
        {"name": name, "status": 0, "deleted": 0},
    )


def ensure_user_post(cur, tenant_id: int, user_id: int, post_id: int) -> None:
    ensure_by_select(
        cur,
        "system_user_post",
        "tenant_id=%s AND user_id=%s AND post_id=%s",
        (tenant_id, user_id, post_id),
        {
            "user_id": user_id,
            "post_id": post_id,
            "creator": CREATOR,
            "updater": CREATOR,
            "tenant_id": tenant_id,
        },
        {"deleted": 0},
    )


def ensure_process(cur, tenant_id: int, code: str, name: str) -> int:
    return ensure_by_select(
        cur,
        "mes_pro_process",
        "tenant_id=%s AND code=%s",
        (tenant_id, code),
        {
            "product_name": "ACD04 Demo Product",
            "code": code,
            "name": name,
            "attention": "ACD04 local fixture",
            "status": 0,
            "manual_shift_capacity": 100,
            "remark": f"{PREFIX} fixture process",
            "creator": CREATOR,
            "updater": CREATOR,
            "tenant_id": tenant_id,
        },
        {"name": name, "status": 0, "deleted": 0},
    )


def ensure_workstation(cur, tenant_id: int, code: str, name: str, process_id: int) -> int:
    return ensure_by_select(
        cur,
        "mes_md_workstation",
        "tenant_id=%s AND code=%s",
        (tenant_id, code),
        {
            "code": code,
            "name": name,
            "address": "ACD04 local fixture",
            "process_id": process_id,
            "status": 0,
            "remark": f"{PREFIX} fixture workstation",
            "creator": CREATOR,
            "updater": CREATOR,
            "tenant_id": tenant_id,
        },
        {"name": name, "process_id": process_id, "status": 0, "deleted": 0},
    )


def ensure_workstation_worker(cur, tenant_id: int, workstation_id: int, post_id: int) -> None:
    ensure_by_select(
        cur,
        "mes_md_workstation_worker",
        "tenant_id=%s AND workstation_id=%s AND post_id=%s",
        (tenant_id, workstation_id, post_id),
        {
            "workstation_id": workstation_id,
            "post_id": post_id,
            "quantity": 1,
            "remark": f"{PREFIX} fixture workstation worker",
            "creator": CREATOR,
            "updater": CREATOR,
            "tenant_id": tenant_id,
        },
        {"quantity": 1, "deleted": 0},
    )


def ensure_route(cur, tenant_id: int, code: str, name: str) -> int:
    return ensure_by_select(
        cur,
        "mes_pro_route",
        "tenant_id=%s AND code=%s",
        (tenant_id, code),
        {
            "code": code,
            "name": name,
            "description": "ACD04 local fixture route",
            "status": 0,
            "remark": f"{PREFIX} fixture route",
            "creator": CREATOR,
            "updater": CREATOR,
            "tenant_id": tenant_id,
        },
        {"name": name, "status": 0, "deleted": 0},
    )


def ensure_route_process(
    cur,
    tenant_id: int,
    route_id: int,
    process_id: int,
    workstation_id: int,
    sort: int,
    key_flag: bool,
) -> int:
    return ensure_by_select(
        cur,
        "mes_pro_route_process",
        "tenant_id=%s AND route_id=%s AND process_id=%s AND sort=%s",
        (tenant_id, route_id, process_id, sort),
        {
            "route_id": route_id,
            "process_id": process_id,
            "workstation_id": workstation_id,
            "sort": sort,
            "link_type": 1,
            "prepare_time": 0,
            "wait_time": 0,
            "color_code": "#409EFF",
            "key_flag": 1 if key_flag else 0,
            "check_flag": 0,
            "remark": f"{PREFIX} fixture route process",
            "creator": CREATOR,
            "updater": CREATOR,
            "tenant_id": tenant_id,
        },
        {
            "workstation_id": workstation_id,
            "link_type": 1,
            "key_flag": 1 if key_flag else 0,
            "check_flag": 0,
            "deleted": 0,
        },
    )


def ensure_route_version(
    cur,
    tenant_id: int,
    route_id: int,
    version_no: str,
    route_code: str,
    route_name: str,
    route_process_ids: list[int],
    leader_user_ids: list[int],
) -> int:
    leaders = []
    if leader_user_ids:
        leaders.append({
            "productionLineId": route_id,
            "candidateSourceType": "USERS",
            "candidateSourceIds": leader_user_ids,
        })
    snapshot = {
        "route": {"id": route_id, "code": route_code, "name": route_name},
        "flowGraph": {
            "nodes": [
                {"id": str(route_process_id), "routeProcessId": route_process_id, "type": "PROCESS"}
                for route_process_id in route_process_ids
            ],
            "edges": [],
        },
        "configSnapshots": {
            "routeStartProductionLeaders": leaders,
        },
    }
    return ensure_by_select(
        cur,
        "mes_pro_route_version",
        "tenant_id=%s AND route_id=%s AND version_no=%s",
        (tenant_id, route_id, version_no),
        {
            "route_id": route_id,
            "version_no": version_no,
            "active": 1,
            "lifecycle_status": "ACTIVE",
            "route_snapshot_json": json.dumps(snapshot, ensure_ascii=False, separators=(",", ":")),
            "published_by": leader_user_ids[0] if leader_user_ids else None,
            "published_time": datetime.now(),
            "remark": f"{PREFIX} fixture active version",
            "creator": CREATOR,
            "updater": CREATOR,
            "tenant_id": tenant_id,
        },
        {
            "active": 1,
            "lifecycle_status": "ACTIVE",
            "route_snapshot_json": json.dumps(snapshot, ensure_ascii=False, separators=(",", ":")),
            "published_by": leader_user_ids[0] if leader_user_ids else None,
            "published_time": datetime.now(),
            "deleted": 0,
        },
    )


def ensure_employee_profile(cur, tenant_id: int, leader_user_id: int, system_user_id: int) -> int:
    return ensure_by_select(
        cur,
        "mes_pro_process_pool_team_employee_profile",
        "tenant_id=%s AND leader_user_id=%s AND employee_code=%s",
        (tenant_id, leader_user_id, f"{PREFIX}-EMP"),
        {
            "leader_user_id": leader_user_id,
            "system_user_id": system_user_id,
            "employee_code": f"{PREFIX}-EMP",
            "employee_name": "ACD04 Fixture Worker",
            "employee_type": "FORMAL",
            "enabled": 1,
            "remark": f"{PREFIX} fixture employee profile",
            "creator": CREATOR,
            "updater": CREATOR,
            "tenant_id": tenant_id,
        },
        {"system_user_id": system_user_id, "employee_name": "ACD04 Fixture Worker", "enabled": 1, "deleted": 0},
    )


def ensure_employee_binding(
    cur,
    tenant_id: int,
    leader_user_id: int,
    process_id: int,
    employee_profile_id: int,
    employee_user_id: int,
) -> int:
    return ensure_by_select(
        cur,
        "mes_pro_process_pool_team_employee_binding",
        "tenant_id=%s AND leader_user_id=%s AND process_id=%s AND employee_profile_id=%s",
        (tenant_id, leader_user_id, process_id, employee_profile_id),
        {
            "leader_user_id": leader_user_id,
            "process_id": process_id,
            "employee_profile_id": employee_profile_id,
            "employee_user_id": employee_user_id,
            "enabled": 1,
            "remark": f"{PREFIX} fixture employee binding",
            "creator": CREATOR,
            "updater": CREATOR,
            "tenant_id": tenant_id,
        },
        {"employee_user_id": employee_user_id, "enabled": 1, "deleted": 0},
    )


def ensure_item(cur, tenant_id: int) -> int:
    return ensure_by_select(
        cur,
        "mes_md_item",
        "tenant_id=%s AND code=%s",
        (tenant_id, f"{PREFIX}-ITEM"),
        {
            "code": f"{PREFIX}-ITEM",
            "name": "ACD04 Fixture Product",
            "specification": "ACD04",
            "status": 0,
            "batch_flag": 1,
            "remark": f"{PREFIX} fixture item",
            "creator": CREATOR,
            "updater": CREATOR,
            "tenant_id": tenant_id,
        },
        {"name": "ACD04 Fixture Product", "status": 0, "deleted": 0},
    )


def ensure_work_order(cur, tenant_id: int, item_id: int) -> int:
    return ensure_by_select(
        cur,
        "mes_pro_work_order",
        "tenant_id=%s AND code=%s",
        (tenant_id, f"{PREFIX}-WO"),
        {
            "code": f"{PREFIX}-WO",
            "name": "ACD04 Fixture Work Order",
            "type": 1,
            "order_source_type": 1,
            "order_source_code": f"{PREFIX}-SRC",
            "product_id": item_id,
            "quantity": 100,
            "quantity_produced": 0,
            "quantity_changed": 0,
            "quantity_scheduled": 100,
            "batch_code": f"{PREFIX}-BATCH",
            "business_status": "RUNNING",
            "schedule_status": "SCHEDULED",
            "status": 0,
            "remark": f"{PREFIX} fixture work order",
            "creator": CREATOR,
            "updater": CREATOR,
            "tenant_id": tenant_id,
        },
        {"product_id": item_id, "status": 0, "deleted": 0},
    )


def ensure_task(
    cur,
    tenant_id: int,
    work_order_id: int,
    workstation_id: int,
    route_id: int,
    process_id: int,
    item_id: int,
) -> int:
    return ensure_by_select(
        cur,
        "mes_pro_task",
        "tenant_id=%s AND code=%s",
        (tenant_id, f"{PREFIX}-TASK-P1"),
        {
            "code": f"{PREFIX}-TASK-P1",
            "name": "ACD04 Fixture Task P1",
            "work_order_id": work_order_id,
            "workstation_id": workstation_id,
            "route_id": route_id,
            "process_id": process_id,
            "item_id": item_id,
            "quantity": 100,
            "produced_quantity": 0,
            "qualify_quantity": 0,
            "unqualify_quantity": 0,
            "changed_quantity": 0,
            "status": 0,
            "remark": f"{PREFIX} fixture task",
            "creator": CREATOR,
            "updater": CREATOR,
            "tenant_id": tenant_id,
        },
        {
            "work_order_id": work_order_id,
            "workstation_id": workstation_id,
            "route_id": route_id,
            "process_id": process_id,
            "item_id": item_id,
            "status": 0,
            "deleted": 0,
        },
    )


def ensure_loss_reason(
    cur,
    tenant_id: int,
    route_process_id: int,
    process_id: int,
    code: str,
    name: str,
    enabled: bool,
) -> int:
    return ensure_by_select(
        cur,
        "mes_pro_process_pool_defect_reason",
        "tenant_id=%s AND route_process_id=%s AND reason_type='LOSS' AND reason_code=%s",
        (tenant_id, route_process_id, code),
        {
            "leader_user_id": None,
            "route_process_id": route_process_id,
            "process_id": process_id,
            "reason_type": "LOSS",
            "reason_code": code,
            "reason_name": name,
            "enabled": 1 if enabled else 0,
            "remark": f"{PREFIX} fixture loss reason",
            "creator": CREATOR,
            "updater": CREATOR,
            "tenant_id": tenant_id,
        },
        {
            "leader_user_id": None,
            "process_id": process_id,
            "reason_name": name,
            "enabled": 1 if enabled else 0,
            "deleted": 0,
        },
    )


def seed_fixture(conn, tenant_id: int, package_id: int, prefix: str) -> dict[str, Any]:
    global PREFIX
    PREFIX = prefix
    with conn.cursor() as cur:
        ensure_preconditions(cur, tenant_id, package_id)

        dept_id = one(
            cur,
            "SELECT id FROM system_dept WHERE tenant_id=%s AND deleted=b'0' ORDER BY parent_id, sort, id LIMIT 1",
            (tenant_id,),
        )

        leader_role_id = ensure_role(cur, tenant_id, f"{prefix.lower().replace('-', '')}plr", "ACD04生产组长", 910501, LEADER_MENU_IDS)
        worker_role_id = ensure_role(cur, tenant_id, f"{prefix.lower().replace('-', '')}worker", "ACD04报工员工", 910502, WORKER_MENU_IDS)
        post_id = ensure_post(cur, tenant_id, f"{prefix}-POST", f"{prefix} Worker Post")

        leader_a_id = ensure_user(cur, tenant_id, "acd04lead1", "ACD04 Leader A", dept_id)
        leader_b_id = ensure_user(cur, tenant_id, "acd04lead2", "ACD04 Leader B", dept_id)
        worker_id = ensure_user(cur, tenant_id, "acd04worker", "ACD04 Worker", dept_id, f"[{post_id}]")
        for user_id in [leader_a_id, leader_b_id]:
            ensure_user_role(cur, tenant_id, user_id, leader_role_id)
        ensure_user_role(cur, tenant_id, worker_id, worker_role_id)
        ensure_user_post(cur, tenant_id, worker_id, post_id)

        process_a_id = ensure_process(cur, tenant_id, f"{prefix}-P1", "ACD04 Route Process One")
        process_b_id = ensure_process(cur, tenant_id, f"{prefix}-P2", "ACD04 Route Process Two")
        process_out_id = ensure_process(cur, tenant_id, f"{prefix}-OUT", "ACD04 Unauthorized Process")

        workstation_a_id = ensure_workstation(cur, tenant_id, f"{prefix}-WS1", "ACD04 Workstation One", process_a_id)
        workstation_b_id = ensure_workstation(cur, tenant_id, f"{prefix}-WS2", "ACD04 Workstation Two", process_b_id)
        workstation_out_id = ensure_workstation(cur, tenant_id, f"{prefix}-WSX", "ACD04 Unauthorized Workstation", process_out_id)
        ensure_workstation_worker(cur, tenant_id, workstation_a_id, post_id)
        ensure_workstation_worker(cur, tenant_id, workstation_b_id, post_id)

        route_a_id = ensure_route(cur, tenant_id, f"{prefix}-R1", "ACD04 Authorized Route")
        route_out_id = ensure_route(cur, tenant_id, f"{prefix}-R2", "ACD04 Unauthorized Route")
        route_process_a_id = ensure_route_process(cur, tenant_id, route_a_id, process_a_id, workstation_a_id, 10, True)
        route_process_b_id = ensure_route_process(cur, tenant_id, route_a_id, process_b_id, workstation_b_id, 20, False)
        route_process_out_id = ensure_route_process(cur, tenant_id, route_out_id, process_out_id, workstation_out_id, 10, True)
        route_version_a_id = ensure_route_version(
            cur,
            tenant_id,
            route_a_id,
            f"{prefix}-V1",
            f"{prefix}-R1",
            "ACD04 Authorized Route",
            [route_process_a_id, route_process_b_id],
            [leader_a_id, leader_b_id],
        )
        route_version_out_id = ensure_route_version(
            cur,
            tenant_id,
            route_out_id,
            f"{prefix}-V1",
            f"{prefix}-R2",
            "ACD04 Unauthorized Route",
            [route_process_out_id],
            [],
        )

        profile_id = ensure_employee_profile(cur, tenant_id, leader_a_id, worker_id)
        binding_a_id = ensure_employee_binding(cur, tenant_id, leader_a_id, process_a_id, profile_id, worker_id)
        binding_b_id = ensure_employee_binding(cur, tenant_id, leader_a_id, process_b_id, profile_id, worker_id)

        item_id = ensure_item(cur, tenant_id)
        work_order_id = ensure_work_order(cur, tenant_id, item_id)
        task_id = ensure_task(cur, tenant_id, work_order_id, workstation_a_id, route_a_id, process_a_id, item_id)

        reason_enabled_id = ensure_loss_reason(
            cur,
            tenant_id,
            route_process_a_id,
            process_a_id,
            f"{prefix}-LOSS-OK",
            "ACD04 enabled loss reason",
            True,
        )
        reason_disabled_id = ensure_loss_reason(
            cur,
            tenant_id,
            route_process_a_id,
            process_a_id,
            f"{prefix}-LOSS-OFF",
            "ACD04 disabled loss reason",
            False,
        )
        reason_cross_id = ensure_loss_reason(
            cur,
            tenant_id,
            route_process_b_id,
            process_b_id,
            f"{prefix}-LOSS-CROSS",
            "ACD04 cross-process loss reason",
            True,
        )

    conn.commit()
    return {
        "tenantId": tenant_id,
        "tenantPackageId": package_id,
        "prefix": prefix,
        "users": {
            "leaderA": {"id": leader_a_id, "username": "acd04lead1"},
            "leaderB": {"id": leader_b_id, "username": "acd04lead2"},
            "worker": {"id": worker_id, "username": "acd04worker"},
        },
        "roles": {"leaderRoleId": leader_role_id, "workerRoleId": worker_role_id},
        "postId": post_id,
        "route": {
            "authorizedRouteId": route_a_id,
            "authorizedRouteVersionId": route_version_a_id,
            "unauthorizedRouteId": route_out_id,
            "unauthorizedRouteVersionId": route_version_out_id,
        },
        "processes": {
            "processAId": process_a_id,
            "processBId": process_b_id,
            "unauthorizedProcessId": process_out_id,
        },
        "routeProcesses": {
            "routeProcessAId": route_process_a_id,
            "routeProcessBId": route_process_b_id,
            "unauthorizedRouteProcessId": route_process_out_id,
        },
        "workstations": {
            "workstationAId": workstation_a_id,
            "workstationBId": workstation_b_id,
            "unauthorizedWorkstationId": workstation_out_id,
        },
        "employeeProfileId": profile_id,
        "employeeBindingIds": [binding_a_id, binding_b_id],
        "itemId": item_id,
        "workOrderId": work_order_id,
        "taskId": task_id,
        "lossReasons": {
            "enabledReasonId": reason_enabled_id,
            "disabledReasonId": reason_disabled_id,
            "crossProcessReasonId": reason_cross_id,
        },
        "frontendUrl": "http://127.0.0.1:8093",
        "backendUrl": "http://127.0.0.1:48093",
        "password": "not-recorded; use local tenant admin-compatible test password via environment only",
    }


def main() -> int:
    args = parse_args()
    root = workspace_root()
    db_config = read_local_mysql_config(root)
    summary_path = root / args.output

    conn = connect(db_config)
    try:
        schema_operations = ensure_schema(conn)
        validate_schema(conn)
        fixture = seed_fixture(conn, args.tenant_id, args.tenant_package_id, args.prefix)
        output = {
            "taskId": TASK_ID,
            "generatedAt": datetime.now().isoformat(timespec="seconds"),
            "db": {
                "host": db_config["host"],
                "port": db_config["port"],
                "database": db_config["database"],
                "user": db_config["user"],
                "password": "redacted",
            },
            "schemaOperations": schema_operations,
            "fixture": fixture,
            "cleanupScope": {
                "tenantId": args.tenant_id,
                "exactPrefix": args.prefix,
                "ownedUsernames": ["acd04lead1", "acd04lead2", "acd04worker"],
                "ownedCreator": CREATOR,
            },
        }
        summary_path.parent.mkdir(parents=True, exist_ok=True)
        summary_path.write_text(json.dumps(output, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
        print(f"ACD04_FIXTURE_READY {summary_path}")
        print(f"tenant={args.tenant_id} leaderA=acd04lead1 leaderB=acd04lead2 worker=acd04worker")
        print(
            "routeProcessA={routeProcessAId} routeProcessB={routeProcessBId} enabledReason={enabledReasonId}".format(
                routeProcessAId=fixture["routeProcesses"]["routeProcessAId"],
                routeProcessBId=fixture["routeProcesses"]["routeProcessBId"],
                enabledReasonId=fixture["lossReasons"]["enabledReasonId"],
            )
        )
        return 0
    finally:
        conn.close()


if __name__ == "__main__":
    raise SystemExit(main())
