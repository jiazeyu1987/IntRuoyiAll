from __future__ import annotations

import json
import subprocess
import sys
from pathlib import Path
from typing import Any


REPO_ROOT = Path(__file__).resolve().parents[2]
SOURCE_TENANT_ID = 1
TARGET_TENANT_ID = 162
PROFILE = "all-copyable-tenant-data"
JOB_CODE = "20260525-yudao-to-yingtai"


def run_tenant_clone(*args: str) -> subprocess.CompletedProcess[str]:
    return subprocess.run(
        [sys.executable, "-X", "utf8", "-m", "script.tenant_clone", *args],
        cwd=REPO_ROOT,
        capture_output=True,
        text=True,
        encoding="utf-8",
        errors="replace",
        check=False,
    )


def parse_json_stdout(completed: subprocess.CompletedProcess[str]) -> dict[str, Any]:
    try:
        payload = json.loads(completed.stdout)
    except json.JSONDecodeError as exc:
        raise AssertionError(
            "tenant clone CLI must write a JSON result to stdout; "
            f"returncode={completed.returncode}, stdout={completed.stdout!r}, stderr={completed.stderr!r}"
        ) from exc
    assert isinstance(payload, dict), f"tenant clone CLI JSON result must be an object: {payload!r}"
    return payload


def write_json(path: Path, payload: dict[str, Any]) -> Path:
    path.write_text(json.dumps(payload, ensure_ascii=False, indent=2), encoding="utf-8")
    return path


def write_valid_contract(path: Path) -> Path:
    return write_json(
        path,
        {
            "version": 1,
            "profile": PROFILE,
            "sourceTenantId": SOURCE_TENANT_ID,
            "targetTenantId": TARGET_TENANT_ID,
            "mode": "clear_target_then_clone",
            "idStrategy": "snowflake",
            "tables": [
                {
                    "table": "system_dept",
                    "category": "tenant_data",
                    "primaryKey": "id",
                    "tenantField": "tenant_id",
                    "copyOrder": 100,
                    "deleteOrder": 900,
                    "includeFields": "*",
                    "excludeFields": ["create_time", "update_time", "deleted"],
                    "referenceFields": [],
                    "uniqueKeys": [
                        {"name": "uk_system_dept_tenant_code", "fields": ["tenant_id", "code"]}
                    ],
                    "onMissingReference": "fail",
                },
                {
                    "table": "system_operate_log",
                    "category": "excluded",
                    "reason": "audit_log_not_business_seed",
                },
                {
                    "table": "bpm_process_instance",
                    "category": "runtime_excluded",
                    "reason": "runtime_state_not_cloneable",
                },
            ],
        },
    )


def write_contract_missing_table_and_reference(path: Path) -> Path:
    return write_json(
        path,
        {
            "version": 1,
            "profile": PROFILE,
            "sourceTenantId": SOURCE_TENANT_ID,
            "targetTenantId": TARGET_TENANT_ID,
            "mode": "clear_target_then_clone",
            "idStrategy": "snowflake",
            "tables": [
                {
                    "table": "system_users",
                    "category": "tenant_data",
                    "primaryKey": "id",
                    "tenantField": "tenant_id",
                    "copyOrder": 200,
                    "deleteOrder": 800,
                    "includeFields": "*",
                    "excludeFields": ["create_time", "update_time", "deleted"],
                    "referenceFields": [],
                    "uniqueKeys": [
                        {"name": "uk_system_users_tenant_username", "fields": ["tenant_id", "username"]}
                    ],
                    "onMissingReference": "fail",
                }
            ],
        },
    )


def write_schema_inventory_with_global_unique_index(path: Path) -> Path:
    return write_json(
        path,
        {
            "tables": [
                {
                    "table": "dcc_file_category",
                    "tenantField": "tenant_id",
                    "sourceRows": 3,
                    "primaryKey": ["id"],
                    "uniqueIndexes": [
                        {"name": "PRIMARY", "columns": ["id"], "primary": True},
                        {"name": "uk_dcc_file_category_code", "columns": ["code"], "primary": False},
                    ],
                },
                {
                    "table": "system_dept",
                    "tenantField": "tenant_id",
                    "sourceRows": 4,
                    "primaryKey": ["id"],
                    "uniqueIndexes": [
                        {"name": "uk_system_dept_tenant_code", "columns": ["tenant_id", "code"], "primary": False}
                    ],
                },
            ]
        },
    )


def write_contract_candidate_inventory(path: Path) -> Path:
    return write_json(
        path,
        {
            "tenantTables": [
                {
                    "table": "system_users",
                    "tenantField": "tenant_id",
                    "primaryKey": "id",
                    "referenceCandidates": [
                        {"field": "dept_id", "refTable": "system_dept", "refPk": "id", "format": "scalar"}
                    ],
                },
                {
                    "table": "system_dept",
                    "tenantField": "tenant_id",
                    "primaryKey": "id",
                    "referenceCandidates": [],
                },
            ]
        },
    )


def write_target_counts(path: Path, rows: int = 2) -> Path:
    return write_json(
        path,
        {
            "targetTenantId": TARGET_TENANT_ID,
            "tables": [
                {"table": "system_dept", "tenantField": "tenant_id", "rows": rows},
            ],
        },
    )


def write_job_without_backup(path: Path) -> Path:
    return write_json(
        path,
        {
            "jobCode": JOB_CODE,
            "sourceTenantId": SOURCE_TENANT_ID,
            "targetTenantId": TARGET_TENANT_ID,
            "profile": PROFILE,
            "mode": "clear_target_then_clone",
            "status": "FAILED",
            "currentPhase": "CLONING",
            "backupIndexPath": None,
        },
    )


def read_json(path: Path) -> dict[str, Any]:
    payload = json.loads(path.read_text(encoding="utf-8"))
    assert isinstance(payload, dict), f"JSON fixture must contain an object: {path}"
    return payload


def write_ready_job_store(job_store: Path, *, backup_index_path: Path | None = None, status: str = "READY") -> Path:
    job_store.mkdir(parents=True, exist_ok=True)
    return write_json(
        job_store / f"{JOB_CODE}.json",
        {
            "jobCode": JOB_CODE,
            "sourceTenantId": SOURCE_TENANT_ID,
            "targetTenantId": TARGET_TENANT_ID,
            "profile": PROFILE,
            "mode": "clear_target_then_clone",
            "status": status,
            "currentPhase": None,
            "backupIndexPath": str(backup_index_path) if backup_index_path else None,
        },
    )


def write_offline_clone_contract(path: Path, *, include_parent_table: bool = True) -> Path:
    tables: list[dict[str, Any]] = []
    if include_parent_table:
        tables.append(
            {
                "table": "system_dept",
                "category": "tenant_data",
                "primaryKey": "id",
                "tenantField": "tenant_id",
                "copyOrder": 100,
                "deleteOrder": 900,
                "includeFields": "*",
                "excludeFields": ["create_time", "update_time", "deleted"],
                "referenceFields": [],
                "uniqueKeys": [
                    {"name": "uk_system_dept_tenant_code", "fields": ["tenant_id", "code"]}
                ],
                "onMissingReference": "fail",
            }
        )
    tables.append(
        {
            "table": "system_users",
            "category": "tenant_data",
            "primaryKey": "id",
            "tenantField": "tenant_id",
            "copyOrder": 200,
            "deleteOrder": 800,
            "includeFields": "*",
            "excludeFields": ["create_time", "update_time", "deleted"],
            "referenceFields": [
                {
                    "field": "dept_id",
                    "refTable": "system_dept",
                    "refPk": "id",
                    "rewrite": "required",
                }
            ],
            "uniqueKeys": [
                {"name": "uk_system_users_tenant_username", "fields": ["tenant_id", "username"]}
            ],
            "onMissingReference": "fail",
        }
    )
    return write_json(
        path,
        {
            "version": 1,
            "profile": PROFILE,
            "sourceTenantId": SOURCE_TENANT_ID,
            "targetTenantId": TARGET_TENANT_ID,
            "mode": "clear_target_then_clone",
            "idStrategy": "snowflake",
            "tables": tables,
        },
    )


def offline_clone_data_payload(*, missing_parent_reference: bool = False) -> dict[str, Any]:
    source_dept_id = 1001
    target_dept_id = 2001
    other_dept_id = 3001
    return {
        "tables": {
            "system_dept": [
                {
                    "id": source_dept_id,
                    "tenant_id": SOURCE_TENANT_ID,
                    "code": "SRC-DEPT",
                    "name": "source department",
                },
                {
                    "id": target_dept_id,
                    "tenant_id": TARGET_TENANT_ID,
                    "code": "OLD-DEPT",
                    "name": "target department before clone",
                },
                {
                    "id": other_dept_id,
                    "tenant_id": 999,
                    "code": "OTHER-DEPT",
                    "name": "other tenant department",
                },
            ],
            "system_users": [
                {
                    "id": 5001,
                    "tenant_id": SOURCE_TENANT_ID,
                    "username": "source-user",
                    "dept_id": 999999 if missing_parent_reference else source_dept_id,
                },
                {
                    "id": 6001,
                    "tenant_id": TARGET_TENANT_ID,
                    "username": "target-user-before-clone",
                    "dept_id": target_dept_id,
                },
                {
                    "id": 7001,
                    "tenant_id": 999,
                    "username": "other-tenant-user",
                    "dept_id": other_dept_id,
                },
            ],
        }
    }


def write_offline_data_store(path: Path, *, missing_parent_reference: bool = False) -> Path:
    return write_json(path, offline_clone_data_payload(missing_parent_reference=missing_parent_reference))


def rows_for_tenant(data_store: dict[str, Any], table: str, tenant_id: int) -> list[dict[str, Any]]:
    tables = data_store["tables"]
    rows = tables[table]
    assert isinstance(rows, list), f"{table} rows must be a list"
    return [row for row in rows if row.get("tenant_id") == tenant_id]
