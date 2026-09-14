"""Build and apply deterministic MDM facts for the approved registration workbook.

The module is deliberately split into a pure planner and an explicit ``--apply``
database operation.  Planning never mutates the runtime; applying requires the
named local Docker database and performs only idempotent inserts for task-owned
historical master facts.
"""

from __future__ import annotations

import argparse
import hashlib
import json
import subprocess
from pathlib import Path
from typing import Any

import pymysql

from registration_certificate_migration_commit import _parse_production_relation
from registration_certificate_migration_preflight import (
    DEFAULT_SOURCE_WORKBOOK,
    EXPECTED_SOURCE_SHA256,
    build_preflight_report,
)


TENANT_ID = 1
TENANT_NAME = "芋道源码"
SYNC_ACTOR = "registration-excel-sync"
SOURCE_HASH = EXPECTED_SOURCE_SHA256.upper()


class MasterDataSyncError(RuntimeError):
    pass


def stable_code(kind: str, name: str, source_hash: str = SOURCE_HASH) -> str:
    if kind not in {"ENT", "PROD", "PROJ"}:
        raise ValueError(f"unsupported historical code kind: {kind}")
    normalized = str(name or "").strip()
    if not normalized:
        raise ValueError("historical code source name must not be blank")
    digest = hashlib.sha256(f"{source_hash.upper()}|{normalized}".encode("utf-8")).hexdigest()[:16].upper()
    return f"HIST-REG-{kind}-{digest}"


def _enabled_by_name(rows: list[dict[str, Any]], name_key: str, name: str, type_value: str | None = None):
    return [
        row for row in rows
        if row.get("tenant_id") == TENANT_ID
        and not _is_deleted(row.get("deleted"))
        and row.get("status") == "ENABLE"
        and row.get(name_key) == name
        and (type_value is None or row.get("type") == type_value)
    ]


def _is_deleted(value: Any) -> bool:
    if value in (None, 0, False, "0", b"\x00", b"0"):
        return False
    return value in (1, True, "1", b"\x01", b"1")


def _source_facts(preflight: dict[str, Any]):
    owner_names = set()
    product_names = set()
    entrusted_names = set()
    for row in preflight["rows"]:
        normalized = row["normalized"]
        owner_names.add(normalized["owner_company_name"])
        product_names.add(normalized["product_name"])
        relation = _parse_production_relation(normalized.get("remark") or "")
        entrusted_names.update(relation["entrusted_enterprises"])
    return sorted(owner_names), sorted(product_names), sorted(entrusted_names)


def build_master_data_plan(preflight: dict[str, Any], runtime: dict[str, list[dict[str, Any]]]) -> dict[str, Any]:
    if preflight.get("source_sha256", "").upper() != SOURCE_HASH:
        raise MasterDataSyncError("source workbook hash does not match the frozen contract")
    owners, products, entrusted = _source_facts(preflight)
    enterprise_actions = []
    enterprise_refs: dict[tuple[str, str], dict[str, Any]] = {}
    for name, type_value in [(x, "OWNED_COMPANY") for x in owners] + [(x, "ENTRUSTED_PARTY") for x in entrusted]:
        matches = _enabled_by_name(runtime.get("enterprises", []), "name", name, type_value)
        if len(matches) == 1:
            ref = {"action": "REUSE", "id": matches[0]["id"], "name": name, "type": type_value,
                   "enterprise_code": matches[0]["enterprise_code"]}
        elif len(matches) == 0:
            ref = {"action": "CREATE", "id": None, "name": name, "type": type_value,
                   "enterprise_code": stable_code("ENT", f"{type_value}|{name}")}
        else:
            raise MasterDataSyncError(f"enabled enterprise mapping is multivalent: {type_value}/{name}")
        enterprise_refs[(type_value, name)] = ref
        enterprise_actions.append(ref)

    product_actions = []
    product_refs: dict[str, dict[str, Any]] = {}
    for name in products:
        canonical_code = stable_code("PROD", name)
        canonical_matches = [
            row for row in runtime.get("products", [])
            if row.get("tenant_id") == TENANT_ID and not _is_deleted(row.get("deleted"))
            and row.get("status") == "ENABLE" and row.get("product_code") == canonical_code
        ]
        if len(canonical_matches) > 1:
            raise MasterDataSyncError(f"canonical product code is multivalent: {canonical_code}")
        if len(canonical_matches) == 1:
            if canonical_matches[0].get("name_cn") != name:
                raise MasterDataSyncError(f"canonical product code collision: {canonical_code}")
            ref = {"action": "REUSE", "id": canonical_matches[0]["id"], "name_cn": name,
                   "product_code": canonical_code}
        else:
            matches = _enabled_by_name(runtime.get("products", []), "name_cn", name)
            if len(matches) == 1:
                ref = {"action": "REUSE", "id": matches[0]["id"], "name_cn": name,
                       "product_code": matches[0]["product_code"]}
            else:
                ref = {"action": "CREATE", "id": None, "name_cn": name,
                       "product_code": canonical_code}
        product_refs[name] = ref
        product_actions.append(ref)

    project_actions = []
    project_refs: dict[str, dict[str, Any] | None] = {}
    project_conflicts = []
    source_products_by_code: dict[str, set[str]] = {}
    for row in preflight["rows"]:
        code = row["normalized"].get("project_code")
        if code:
            source_products_by_code.setdefault(code, set()).add(row["normalized"]["product_name"])
    source_codes = sorted({
        row["normalized"]["project_code"]
        for row in preflight["rows"]
        if row["normalized"].get("project_code")
    })
    for code in source_codes:
        product_names_for_code = source_products_by_code[code]
        if len(product_names_for_code) != 1:
            project_refs[code] = None
            project_conflicts.append({"project_code": code, "candidate_ids": [],
                                      "reason": "SOURCE_PROJECT_CODE_HAS_MULTIPLE_PRODUCTS"})
            continue
        source_product_name = next(iter(product_names_for_code))
        source_product_ref = product_refs[source_product_name]
        matches = [
            row for row in runtime.get("projects", [])
            if row.get("tenant_id") == TENANT_ID and not _is_deleted(row.get("deleted"))
            and row.get("status") == "ENABLE" and row.get("project_code") == code
        ]
        if len(matches) == 1:
            if (matches[0].get("product_master_id") is not None
                    and (source_product_ref.get("id") is None
                         or int(matches[0]["product_master_id"]) != int(source_product_ref["id"]))):
                project_refs[code] = None
                project_conflicts.append({"project_code": code, "candidate_ids": [matches[0]["id"]],
                                          "reason": "PROJECT_CODE_PRODUCT_MISMATCH"})
                continue
            ref = {"action": "REUSE", "id": matches[0]["id"], "project_code": code,
                   "product_master_id": matches[0].get("product_master_id"),
                   "product_name": source_product_name}
            project_refs[code] = ref
        elif len(matches) == 0:
            ref = {"action": "CREATE", "id": None, "project_code": code,
                   "project_name": f"历史注册证项目-{stable_code('PROJ', code)[-12:]}",
                   "product_master_id": source_product_ref.get("id"),
                   "product_name": source_product_name}
            project_refs[code] = ref
            project_actions.append(ref)
        else:
            project_refs[code] = None
            conflict = {"project_code": code, "candidate_ids": [x["id"] for x in matches],
                        "reason": "MULTIPLE_ENABLED_TENANT_PROJECT_CODES"}
            project_conflicts.append(conflict)

    row_mappings = []
    for row in preflight["rows"]:
        normalized = row["normalized"]
        project_code = normalized.get("project_code")
        project_ref = project_refs.get(project_code) if project_code else None
        blockers = []
        if project_code and project_ref is None:
            blockers.append("PROJECT_CODE_MAPPING_CONFLICT")
        row_mappings.append({
            "source_row": row["source_row"],
            "owner_company_name": normalized["owner_company_name"],
            "owner_company_ref": enterprise_refs[("OWNED_COMPANY", normalized["owner_company_name"])],
            "product_name": normalized["product_name"],
            "product_ref": product_refs[normalized["product_name"]],
            "project_code": project_code,
            "project_ref": project_ref,
            "restricted_reasons": ["NO_ATTACHMENT_POLICY"] + (["MISSING_PROJECT_CODE"] if not project_code else []),
            "blockers": blockers,
        })

    return {
        "status": "READY" if not project_conflicts else "READY_WITH_BLOCKED_ROWS",
        "tenant_id": TENANT_ID,
        "tenant_name": TENANT_NAME,
        "source_sha256": SOURCE_HASH,
        "enterprise_actions": enterprise_actions,
        "product_actions": product_actions,
        "project_actions": project_actions,
        "project_conflicts": project_conflicts,
        "row_mappings": row_mappings,
        "commit_mappings": {
            "owner_company_ids": {
                name: enterprise_refs[("OWNED_COMPANY", name)]["id"] for name in owners
            },
            "product_master_ids": {name: product_refs[name]["id"] for name in products},
            "project_code_ids": {
                code: ref["id"] for code, ref in project_refs.items() if ref is not None and ref.get("id")
            },
            "entrusted_enterprise_ids": {
                name: enterprise_refs[("ENTRUSTED_PARTY", name)]["id"] for name in entrusted
            },
        },
        "counts": {
            "source_rows": len(preflight["rows"]),
            "enterprise_create": sum(x["action"] == "CREATE" for x in enterprise_actions),
            "product_create": sum(x["action"] == "CREATE" for x in product_actions),
            "project_create": sum(x["action"] == "CREATE" for x in project_actions),
            "restricted_rows": sum(bool(x["restricted_reasons"]) for x in row_mappings),
            "blocked_rows": sum(bool(x["blockers"]) for x in row_mappings),
        },
    }


def _docker_password(container: str) -> str:
    result = subprocess.run(
        ["docker", "inspect", "--format", "{{range .Config.Env}}{{println .}}{{end}}", container],
        check=True, capture_output=True, text=True, encoding="utf-8",
    )
    for line in result.stdout.splitlines():
        if line.startswith("MYSQL_ROOT_PASSWORD="):
            return line.split("=", 1)[1]
    raise MasterDataSyncError("MYSQL_ROOT_PASSWORD is not available in the named Docker container")


def connect_runtime(container: str, database: str):
    password = _docker_password(container)
    return pymysql.connect(host="127.0.0.1", port=23306, user="root", password=password,
                           database=database, charset="utf8mb4", autocommit=False,
                           cursorclass=pymysql.cursors.DictCursor)


def load_runtime(connection) -> dict[str, list[dict[str, Any]]]:
    with connection.cursor() as cursor:
        cursor.execute("SELECT id, enterprise_code, name, type, status, tenant_id, deleted "
                       "FROM mdm_enterprise WHERE tenant_id=%s", (TENANT_ID,))
        enterprises = list(cursor.fetchall())
        cursor.execute("SELECT id, product_code, name_cn, status, tenant_id, deleted "
                       "FROM mdm_product WHERE tenant_id=%s", (TENANT_ID,))
        products = list(cursor.fetchall())
        cursor.execute("SELECT id, project_code, product_master_id, status, tenant_id, deleted "
                       "FROM dcc_project_code WHERE tenant_id=%s", (TENANT_ID,))
        projects = list(cursor.fetchall())
    return {"enterprises": enterprises, "products": products, "projects": projects}


def apply_master_data(connection, plan: dict[str, Any]) -> dict[str, Any]:
    if plan.get("source_sha256", "").upper() != SOURCE_HASH:
        raise MasterDataSyncError("refusing to apply a plan for a different workbook hash")
    created = {"enterprise": [], "product": [], "project": []}
    with connection.cursor() as cursor:
        for action in plan["enterprise_actions"]:
            if action["action"] != "CREATE":
                continue
            cursor.execute("SELECT id, name, type, status FROM mdm_enterprise "
                           "WHERE tenant_id=%s AND enterprise_code=%s FOR UPDATE",
                           (TENANT_ID, action["enterprise_code"]))
            existing = cursor.fetchone()
            if existing:
                if existing["name"] != action["name"] or existing["type"] != action["type"] or existing["status"] != "ENABLE":
                    raise MasterDataSyncError(f"enterprise code collision: {action['enterprise_code']}")
                action["id"] = existing["id"]
                action["action"] = "REUSE"
                continue
            cursor.execute("INSERT INTO mdm_enterprise "
                           "(enterprise_code,name,type,status,revision,creator,updater,deleted,tenant_id) "
                           "VALUES (%s,%s,%s,'ENABLE',1,%s,%s,b'0',%s)",
                           (action["enterprise_code"], action["name"], action["type"], SYNC_ACTOR, SYNC_ACTOR, TENANT_ID))
            action["id"] = cursor.lastrowid
            created["enterprise"].append(action["id"])
        for action in plan["product_actions"]:
            if action["action"] != "CREATE":
                continue
            cursor.execute("SELECT id, name_cn, status FROM mdm_product "
                           "WHERE tenant_id=%s AND product_code=%s FOR UPDATE",
                           (TENANT_ID, action["product_code"]))
            existing = cursor.fetchone()
            if existing:
                if existing["name_cn"] != action["name_cn"] or existing["status"] != "ENABLE":
                    raise MasterDataSyncError(f"product code collision: {action['product_code']}")
                action["id"] = existing["id"]
                action["action"] = "REUSE"
                continue
            cursor.execute("INSERT INTO mdm_product "
                           "(product_code,name_cn,status,creator,updater,deleted,tenant_id) "
                           "VALUES (%s,%s,'ENABLE',%s,%s,b'0',%s)",
                           (action["product_code"], action["name_cn"], SYNC_ACTOR, SYNC_ACTOR, TENANT_ID))
            action["id"] = cursor.lastrowid
            created["product"].append(action["id"])
        product_ids = {x["name_cn"]: x["id"] for x in plan["product_actions"]}
        for action in plan["project_actions"]:
            if action["action"] != "CREATE":
                continue
            product_name = next((x["product_name"] for x in plan["row_mappings"]
                                 if x["project_code"] == action["project_code"]), None)
            action["product_master_id"] = product_ids.get(product_name)
            if not action["product_master_id"]:
                raise MasterDataSyncError(f"missing product mapping for project code: {action['project_code']}")
            cursor.execute("SELECT id, project_name, product_master_id, status FROM dcc_project_code "
                           "WHERE tenant_id=%s AND project_code=%s AND deleted=0 FOR UPDATE",
                           (TENANT_ID, action["project_code"]))
            existing = cursor.fetchall()
            if existing:
                raise MasterDataSyncError(f"project code became non-missing during apply: {action['project_code']}")
            cursor.execute("INSERT INTO dcc_project_code "
                           "(product_master_id,doc_control_no,project_name,project_code,status,tenant_id,deleted,creator,updater) "
                           "VALUES (%s,%s,%s,%s,'ENABLE',%s,0,%s,%s)",
                           (action["product_master_id"], action["project_name"], action["project_name"],
                            action["project_code"], TENANT_ID, SYNC_ACTOR, SYNC_ACTOR))
            action["id"] = cursor.lastrowid
            created["project"].append(action["id"])
    return created


def _json_default(value: Any):
    if isinstance(value, bytes):
        if value in (b"\x00", b"0"):
            return False
        if value in (b"\x01", b"1"):
            return True
        return value.decode("utf-8")
    raise TypeError(f"unsupported JSON value: {type(value).__name__}")


def main(argv=None):
    parser = argparse.ArgumentParser()
    parser.add_argument("--source", default=str(DEFAULT_SOURCE_WORKBOOK))
    parser.add_argument("--output", required=True)
    parser.add_argument("--database", default="ruoyi-vue-pro")
    parser.add_argument("--docker-container", default="int-ruoyi-mysql")
    parser.add_argument("--apply", action="store_true")
    args = parser.parse_args(argv)
    preflight = build_preflight_report(args.source, permissions=["system:migration:admin"])
    connection = connect_runtime(args.docker_container, args.database)
    try:
        runtime = load_runtime(connection)
        plan = build_master_data_plan(preflight, runtime)
        if args.apply:
            created = apply_master_data(connection, plan)
            connection.commit()
            plan["applied"] = True
            plan["created_ids"] = created
            plan["post_apply_runtime"] = load_runtime(connection)
        else:
            connection.rollback()
            plan["applied"] = False
        Path(args.output).write_text(
            json.dumps(plan, ensure_ascii=False, indent=2, default=_json_default) + "\n", encoding="utf-8")
    except Exception:
        connection.rollback()
        raise
    finally:
        connection.close()


if __name__ == "__main__":
    main()
