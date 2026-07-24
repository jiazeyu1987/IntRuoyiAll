from __future__ import annotations

import json
import hashlib
from dataclasses import dataclass
from pathlib import Path
from typing import Any

from .contract import validate_contract_files
from .ddl import generate_tenant_clone_ddl
from .schema import check_schema_inventory_file


@dataclass(frozen=True)
class CommandResult:
    payload: dict[str, Any]
    exit_code: int


class TenantCloneError(Exception):
    def __init__(self, payload: dict[str, Any]) -> None:
        self.payload = payload
        super().__init__(payload.get("message", payload.get("errorCode", "tenant clone failed")))


def command_result(payload: dict[str, Any]) -> CommandResult:
    return CommandResult(payload, 0 if payload.get("success") is True else 1)


def error_payload(error_code: str, message: str, **fields: Any) -> dict[str, Any]:
    return {"success": False, "errorCode": error_code, "message": message, **fields}


def read_json_file(path: str | None, label: str) -> dict[str, Any]:
    if not path:
        raise TenantCloneError(
            error_payload(
                "TENANT_CLONE_REQUIRED_INPUT_MISSING",
                f"{label} path is required",
                phase="INPUT_VALIDATE",
            )
        )
    file_path = Path(path)
    if not file_path.is_file():
        raise TenantCloneError(
            error_payload(
                "TENANT_CLONE_REQUIRED_INPUT_MISSING",
                f"{label} file does not exist: {file_path}",
                phase="INPUT_VALIDATE",
            )
        )
    try:
        payload = json.loads(file_path.read_text(encoding="utf-8"))
    except json.JSONDecodeError as exc:
        raise TenantCloneError(
            error_payload(
                "TENANT_CLONE_INVALID_JSON",
                f"{label} file is not valid JSON: {file_path}: {exc}",
                phase="INPUT_VALIDATE",
            )
        ) from exc
    if not isinstance(payload, dict):
        raise TenantCloneError(
            error_payload(
                "TENANT_CLONE_INVALID_JSON",
                f"{label} file must contain a JSON object: {file_path}",
                phase="INPUT_VALIDATE",
            )
        )
    return payload


def write_json_file(path: Path, payload: dict[str, Any]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(payload, ensure_ascii=False, indent=2), encoding="utf-8")


def write_json_file_atomically(path: Path, payload: dict[str, Any]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    tmp_path = path.with_name(f"{path.name}.tmp")
    write_json_file(tmp_path, payload)
    tmp_path.replace(path)


def job_path(job_store: str | None, job_code: str) -> Path:
    if not job_store:
        raise TenantCloneError(
            error_payload(
                "TENANT_CLONE_REQUIRED_INPUT_MISSING",
                "job store path is required",
                jobCode=job_code,
                phase="JOB_STORE_VALIDATE",
            )
        )
    return Path(job_store) / f"{job_code}.json"


def load_job(job_store: str | None, job_code: str) -> dict[str, Any]:
    path = job_path(job_store, job_code)
    if not path.is_file():
        raise TenantCloneError(
            error_payload(
                "TENANT_CLONE_JOB_NOT_FOUND",
                f"job does not exist for job code: {job_code}",
                jobCode=job_code,
                phase="JOB_LOOKUP",
            )
        )
    return read_json_file(str(path), "job")


def save_job(job_store: str | None, job_code: str, job: dict[str, Any]) -> None:
    write_json_file_atomically(job_path(job_store, job_code), job)


def public_job_payload(job: dict[str, Any], **extra: Any) -> dict[str, Any]:
    payload = {
        "success": True,
        "jobId": job.get("jobId"),
        "jobCode": job.get("jobCode"),
        "status": job.get("status"),
        "sourceTenantId": job.get("sourceTenantId"),
        "targetTenantId": job.get("targetTenantId"),
        "profile": job.get("profile"),
        "mode": job.get("mode"),
    }
    payload.update(extra)
    return payload


def requested_job_contract(args: Any) -> dict[str, Any]:
    return {
        "sourceTenantId": args.source_tenant_id,
        "targetTenantId": args.target_tenant_id,
        "profile": args.profile,
        "mode": args.mode,
        "contractPath": str(Path(args.contract)),
    }


def job_contract_conflicts(existing_job: dict[str, Any], requested_job: dict[str, Any]) -> list[str]:
    return [
        field
        for field, requested_value in requested_job.items()
        if existing_job.get(field) != requested_value
    ]


def create_job(args: Any) -> CommandResult:
    if args.mode != "clear_target_then_clone":
        return command_result(
            error_payload(
                "TENANT_CLONE_UNSUPPORTED_MODE",
                f"unsupported tenant clone mode: {args.mode}",
                jobCode=args.job_code,
                phase="JOB_CREATE",
            )
        )
    read_json_file(args.contract, "contract")
    path = job_path(args.job_store, args.job_code)
    if path.exists():
        existing_job = read_json_file(str(path), "job")
        conflicts = job_contract_conflicts(existing_job, requested_job_contract(args))
        if conflicts:
            return command_result(
                error_payload(
                    "TENANT_CLONE_JOB_CODE_CONFLICT",
                    "job code already exists with a different create-job request",
                    jobCode=args.job_code,
                    status=existing_job.get("status"),
                    phase="JOB_CREATE",
                    conflictFields=conflicts,
                )
            )
        return command_result(public_job_payload(existing_job))

    job_id = "job-" + hashlib.sha256(args.job_code.encode("utf-8")).hexdigest()[:16]
    job = {
        "jobId": job_id,
        "jobCode": args.job_code,
        "sourceTenantId": args.source_tenant_id,
        "targetTenantId": args.target_tenant_id,
        "profile": args.profile,
        "mode": args.mode,
        "status": "READY",
        "currentPhase": None,
        "contractPath": str(Path(args.contract)),
        "backupIndexPath": None,
        "idMapPath": None,
    }
    write_json_file_atomically(path, job)
    return command_result(public_job_payload(job))


def status(args: Any) -> CommandResult:
    return command_result(public_job_payload(load_job(args.job_store, args.job_code)))


def precheck(args: Any) -> CommandResult:
    if args.source_tenant_id == args.target_tenant_id:
        return command_result(
            error_payload(
                "TENANT_CLONE_SAME_TENANT",
                "source tenant and target tenant must be different",
                jobCode=getattr(args, "job_code", None),
                status="PRECHECK_FAILED",
                phase="TENANT_VALIDATE",
            )
        )

    if getattr(args, "candidate_inventory", None):
        contract_payload = validate_contract_files(args.contract, args.candidate_inventory)
        if contract_payload.get("success") is not True:
            contract_payload.setdefault("jobCode", getattr(args, "job_code", None))
            contract_payload.setdefault("status", "PRECHECK_FAILED")
            return command_result(contract_payload)

    schema_payload = check_schema_inventory_file(args.schema_inventory, tenant_field=args.tenant_field)
    schema_payload.setdefault("jobCode", getattr(args, "job_code", None))
    schema_payload.setdefault("status", "READY" if schema_payload.get("success") is True else "PRECHECK_FAILED")
    if "counts" not in schema_payload:
        schema_payload["counts"] = schema_counts(args.schema_inventory)
    if schema_payload.get("success") is not True and schema_payload.get("violations"):
        tables = ", ".join(f"{item['table']}.{item['index']}" for item in schema_payload["violations"])
        schema_payload["message"] = f"unique indexes are not tenant-scoped: {tables}"
    return command_result(schema_payload)


def validate_contract(args: Any) -> CommandResult:
    return command_result(
        validate_contract_files(
            args.contract,
            args.candidate_inventory,
            require_reference_fields=args.require_reference_fields,
        )
    )


def check_schema(args: Any) -> CommandResult:
    payload = check_schema_inventory_file(args.schema_inventory, tenant_field=args.tenant_field)
    payload.setdefault("counts", schema_counts(args.schema_inventory))
    return command_result(payload)


def schema_counts(schema_inventory_path: str) -> dict[str, int]:
    inventory = read_json_file(schema_inventory_path, "schema inventory")
    tables = inventory.get("tables", [])
    if not isinstance(tables, list):
        raise TenantCloneError(
            error_payload(
                "TENANT_CLONE_SCHEMA_INVALID",
                "schema inventory tables must be a list",
                phase="SCHEMA_CHECK",
            )
        )
    return {
        "sourceTables": len(tables),
        "sourceRows": sum(int(table.get("sourceRows", 0)) for table in tables if isinstance(table, dict)),
    }


def target_existing_rows(target_counts_path: str | None) -> int:
    if not target_counts_path:
        return 0
    counts = read_json_file(target_counts_path, "target counts")
    tables = counts.get("tables", [])
    if not isinstance(tables, list):
        raise TenantCloneError(
            error_payload(
                "TENANT_CLONE_TARGET_COUNTS_INVALID",
                "target counts tables must be a list",
                phase="TARGET_COUNT_VALIDATE",
            )
        )
    return sum(int(table.get("rows", 0)) for table in tables if isinstance(table, dict))


def execute(args: Any) -> CommandResult:
    if getattr(args, "offline_data_store", None):
        return execute_offline(args)

    existing_rows = target_existing_rows(getattr(args, "target_counts", None))
    if existing_rows > 0 and not args.confirm_clear_target:
        return command_result(
            error_payload(
                "TENANT_CLONE_TARGET_NOT_EMPTY_CONFIRM_REQUIRED",
                "target tenant has existing rows; rerun with --confirm-clear-target after backup approval",
                jobCode=args.job_code,
                status="READY",
                phase="CLEAR_TARGET_CONFIRMATION",
                targetExistingRows=existing_rows,
                clearedRows=0,
            )
        )

    if existing_rows > 0 and (not getattr(args, "backup_dir", None) or not getattr(args, "backup_index", None)):
        return command_result(
            error_payload(
                "TENANT_CLONE_BACKUP_FAILED",
                "backup directory and backup index path are required before clearing target data",
                jobCode=args.job_code,
                status="READY",
                phase="BACKUP_VALIDATE",
                targetExistingRows=existing_rows,
                clearedRows=0,
            )
        )

    return command_result(
        error_payload(
            "TENANT_CLONE_WRITE_PATH_NOT_IMPLEMENTED",
            "clone write path is unavailable; complete the write implementation before executing tenant clone",
            jobCode=args.job_code,
            status="READY",
            phase="CLONE_WRITE",
            targetExistingRows=existing_rows,
            clearedRows=0,
        )
    )


def rollback(args: Any) -> CommandResult:
    if getattr(args, "offline_data_store", None):
        return rollback_offline(args)

    job_state = read_json_file(args.job_state, "job state") if getattr(args, "job_state", None) else {}
    backup_index = getattr(args, "backup_index", None) or job_state.get("backupIndexPath")
    if not backup_index:
        return command_result(
            error_payload(
                "TENANT_CLONE_BACKUP_MISSING",
                "backup index is required before rollback can restore target data",
                jobCode=args.job_code,
                status=job_state.get("status", "FAILED"),
                phase="ROLLBACK_VALIDATE",
                restoredRows=0,
            )
        )
    return command_result(
        error_payload(
            "TENANT_CLONE_RESTORE_PATH_NOT_IMPLEMENTED",
            "restore path is unavailable; complete the restore implementation before rolling back tenant clone",
            jobCode=args.job_code,
            status="ROLLING_BACK",
            phase="RESTORE_TARGET",
            restoredRows=0,
        )
    )


def schema_ddl(args: Any) -> CommandResult:
    return command_result(generate_tenant_clone_ddl(args.name))


def contract_tenant_tables(contract: dict[str, Any]) -> list[dict[str, Any]]:
    if contract.get("mode") != "clear_target_then_clone":
        raise TenantCloneError(
            error_payload(
                "TENANT_CLONE_UNSUPPORTED_MODE",
                f"unsupported tenant clone mode: {contract.get('mode')}",
                phase="CONTRACT_VALIDATE",
            )
        )
    tables = contract.get("tables")
    if not isinstance(tables, list):
        raise TenantCloneError(
            error_payload(
                "TENANT_CLONE_CONTRACT_INVALID",
                "contract.tables must be a list",
                phase="CONTRACT_VALIDATE",
            )
        )
    tenant_tables = [table for table in tables if isinstance(table, dict) and table.get("category") == "tenant_data"]
    for table in tenant_tables:
        if not isinstance(table.get("table"), str) or not isinstance(table.get("primaryKey"), str):
            raise TenantCloneError(
                error_payload(
                    "TENANT_CLONE_CONTRACT_INVALID",
                    "tenant_data tables must include table and primaryKey",
                    phase="CONTRACT_VALIDATE",
                )
            )
    return tenant_tables


def data_store_tables(data_store: dict[str, Any]) -> dict[str, list[dict[str, Any]]]:
    tables = data_store.get("tables")
    if not isinstance(tables, dict):
        raise TenantCloneError(
            error_payload(
                "TENANT_CLONE_DATA_STORE_INVALID",
                "offline data store tables must be a JSON object",
                phase="DATA_STORE_VALIDATE",
            )
        )
    for table_name, rows in tables.items():
        if not isinstance(rows, list) or not all(isinstance(row, dict) for row in rows):
            raise TenantCloneError(
                error_payload(
                    "TENANT_CLONE_DATA_STORE_INVALID",
                    f"offline data store table rows must be objects: {table_name}",
                    phase="DATA_STORE_VALIDATE",
                )
            )
    return tables


def tenant_field(table: dict[str, Any]) -> str:
    field = table.get("tenantField", "tenant_id")
    if not isinstance(field, str):
        raise TenantCloneError(
            error_payload(
                "TENANT_CLONE_CONTRACT_INVALID",
                f"tenantField must be a string for table: {table.get('table')}",
                phase="CONTRACT_VALIDATE",
            )
        )
    return field


def backup_target_rows(
    backup_dir: Path,
    job: dict[str, Any],
    tenant_tables: list[dict[str, Any]],
    tables: dict[str, list[dict[str, Any]]],
) -> tuple[Path, int]:
    backup_dir.mkdir(parents=True, exist_ok=True)
    backup_index_path = backup_dir / "backup-index.json"
    index_tables: list[dict[str, Any]] = []
    total_rows = 0
    for table in sorted(tenant_tables, key=lambda item: int(item.get("deleteOrder", 0))):
        table_name = table["table"]
        field = tenant_field(table)
        target_rows = [dict(row) for row in tables.get(table_name, []) if row.get(field) == job["targetTenantId"]]
        target_positions = [
            index for index, row in enumerate(tables.get(table_name, [])) if row.get(field) == job["targetTenantId"]
        ]
        backup_file = f"{table_name}.jsonl"
        backup_path = backup_dir / backup_file
        backup_path.write_text(
            "".join(json.dumps(row, ensure_ascii=False, separators=(",", ":")) + "\n" for row in target_rows),
            encoding="utf-8",
        )
        index_tables.append(
            {
                "table": table_name,
                "tenantField": field,
                "rows": len(target_rows),
                "file": backup_file,
                "positions": target_positions,
            }
        )
        total_rows += len(target_rows)
    write_json_file_atomically(
        backup_index_path,
        {
            "jobCode": job["jobCode"],
            "jobId": job.get("jobId"),
            "sourceTenantId": job["sourceTenantId"],
            "targetTenantId": job["targetTenantId"],
            "tables": index_tables,
        },
    )
    return backup_index_path, total_rows


def next_primary_key(rows: list[dict[str, Any]], primary_key: str) -> int:
    numeric_values = [row.get(primary_key) for row in rows if isinstance(row.get(primary_key), int)]
    return (max(numeric_values) if numeric_values else 0) + 1


def build_id_mappings(
    tenant_tables: list[dict[str, Any]],
    tables: dict[str, list[dict[str, Any]]],
    source_tenant_id: int,
) -> tuple[dict[tuple[str, str], Any], list[dict[str, str]]]:
    mappings: dict[tuple[str, str], Any] = {}
    mapping_rows: list[dict[str, str]] = []
    for table in sorted(tenant_tables, key=lambda item: int(item.get("copyOrder", 0))):
        table_name = table["table"]
        pk = table["primaryKey"]
        field = tenant_field(table)
        rows = tables.get(table_name, [])
        next_pk = next_primary_key(rows, pk)
        used = {row.get(pk) for row in rows}
        for row in rows:
            if row.get(field) != source_tenant_id:
                continue
            while next_pk in used:
                next_pk += 1
            source_pk = row.get(pk)
            mappings[(table_name, str(source_pk))] = next_pk
            mapping_rows.append(
                {
                    "table": table_name,
                    "sourcePk": str(source_pk),
                    "targetPk": str(next_pk),
                }
            )
            used.add(next_pk)
            next_pk += 1
    return mappings, mapping_rows


def should_rewrite_reference(reference: dict[str, Any]) -> bool:
    return reference.get("rewrite") == "required" or bool(reference.get("refTable")) or bool(reference.get("refPk"))


def clone_rows(
    job: dict[str, Any],
    tenant_tables: list[dict[str, Any]],
    tables: dict[str, list[dict[str, Any]]],
    mappings: dict[tuple[str, str], Any],
) -> tuple[dict[str, list[dict[str, Any]]], int]:
    cloned_tables = {table_name: [dict(row) for row in rows] for table_name, rows in tables.items()}
    cloned_rows = 0
    for table in sorted(tenant_tables, key=lambda item: int(item.get("deleteOrder", 0))):
        table_name = table["table"]
        field = tenant_field(table)
        cloned_tables[table_name] = [
            row for row in cloned_tables.get(table_name, []) if row.get(field) != job["targetTenantId"]
        ]

    for table in sorted(tenant_tables, key=lambda item: int(item.get("copyOrder", 0))):
        table_name = table["table"]
        pk = table["primaryKey"]
        field = tenant_field(table)
        source_rows = [row for row in tables.get(table_name, []) if row.get(field) == job["sourceTenantId"]]
        for row in source_rows:
            new_row = dict(row)
            new_row[field] = job["targetTenantId"]
            new_row[pk] = mappings[(table_name, str(row.get(pk)))]
            for reference in table.get("referenceFields", []):
                if not isinstance(reference, dict) or not should_rewrite_reference(reference):
                    continue
                reference_field = reference.get("field")
                ref_table = reference.get("refTable")
                if not isinstance(reference_field, str) or not isinstance(ref_table, str):
                    raise TenantCloneError(
                        error_payload(
                            "TENANT_CLONE_CONTRACT_INVALID",
                            f"reference field is invalid for table: {table_name}",
                            jobCode=job["jobCode"],
                            status="FAILED",
                            phase="REFERENCE_REWRITE",
                        )
                    )
                source_reference = row.get(reference_field)
                mapped_reference = mappings.get((ref_table, str(source_reference)))
                if mapped_reference is None:
                    raise TenantCloneError(
                        error_payload(
                            "TENANT_CLONE_MISSING_ID_MAPPING",
                            "required reference field has no source-to-target ID mapping",
                            jobCode=job["jobCode"],
                            status="FAILED",
                            phase="REFERENCE_REWRITE",
                            table=table_name,
                            field=reference_field,
                            missingSourcePk=str(source_reference),
                        )
                    )
                new_row[reference_field] = mapped_reference
            cloned_tables.setdefault(table_name, []).append(new_row)
            cloned_rows += 1
    return cloned_tables, cloned_rows


def execute_offline(args: Any) -> CommandResult:
    if not args.confirm_clear_target:
        return command_result(
            error_payload(
                "TENANT_CLONE_TARGET_NOT_EMPTY_CONFIRM_REQUIRED",
                "offline clone requires --confirm-clear-target before clearing target tenant rows",
                jobCode=args.job_code,
                status="READY",
                phase="CLEAR_TARGET_CONFIRMATION",
                clearedRows=0,
            )
        )
    if not getattr(args, "backup_dir", None):
        return command_result(
            error_payload(
                "TENANT_CLONE_BACKUP_FAILED",
                "backup directory is required before clearing target data",
                jobCode=args.job_code,
                status="READY",
                phase="BACKUP_VALIDATE",
                clearedRows=0,
            )
        )
    job = load_job(args.job_store, args.job_code)
    if job.get("mode") != "clear_target_then_clone":
        return command_result(
            error_payload(
                "TENANT_CLONE_UNSUPPORTED_MODE",
                f"unsupported tenant clone mode: {job.get('mode')}",
                jobCode=args.job_code,
                status=job.get("status", "FAILED"),
                phase="JOB_VALIDATE",
            )
        )
    contract = read_json_file(args.contract, "contract")
    tenant_tables = contract_tenant_tables(contract)
    data_store_path = Path(args.offline_data_store)
    data_store = read_json_file(str(data_store_path), "offline data store")
    tables = data_store_tables(data_store)
    backup_index_path, cleared_rows = backup_target_rows(Path(args.backup_dir), job, tenant_tables, tables)
    mappings, mapping_rows = build_id_mappings(tenant_tables, tables, job["sourceTenantId"])
    try:
        cloned_tables, cloned_rows = clone_rows(job, tenant_tables, tables, mappings)
    except TenantCloneError as exc:
        job["status"] = "FAILED"
        job["currentPhase"] = exc.payload.get("phase")
        save_job(args.job_store, args.job_code, job)
        return command_result(exc.payload)

    id_map_path = Path(args.backup_dir) / "id-map.json"
    write_json_file_atomically(
        id_map_path,
        {
            "jobCode": job["jobCode"],
            "jobId": job.get("jobId"),
            "mappings": mapping_rows,
        },
    )
    updated_store = dict(data_store)
    updated_store["tables"] = cloned_tables
    write_json_file_atomically(data_store_path, updated_store)

    job.update(
        {
            "status": "SUCCEEDED",
            "currentPhase": "VERIFYING",
            "backupIndexPath": str(backup_index_path),
            "idMapPath": str(id_map_path),
        }
    )
    save_job(args.job_store, args.job_code, job)
    return command_result(
        public_job_payload(
            job,
            phase="VERIFYING",
            clearedRows=cleared_rows,
            clonedRows=cloned_rows,
            backupIndexPath=str(backup_index_path),
            idMapPath=str(id_map_path),
        )
    )


def read_jsonl(path: Path) -> list[dict[str, Any]]:
    if not path.is_file():
        raise TenantCloneError(
            error_payload(
                "TENANT_CLONE_BACKUP_MISSING",
                f"backup file does not exist: {path}",
                phase="RESTORE_TARGET",
                restoredRows=0,
            )
        )
    rows: list[dict[str, Any]] = []
    for line in path.read_text(encoding="utf-8").splitlines():
        if not line.strip():
            continue
        row = json.loads(line)
        if not isinstance(row, dict):
            raise TenantCloneError(
                error_payload(
                    "TENANT_CLONE_BACKUP_INVALID",
                    f"backup file row must be a JSON object: {path}",
                    phase="RESTORE_TARGET",
                    restoredRows=0,
                )
            )
        rows.append(row)
    return rows


def rollback_offline(args: Any) -> CommandResult:
    if not args.confirm_restore_target:
        return command_result(
            error_payload(
                "TENANT_CLONE_RESTORE_CONFIRM_REQUIRED",
                "offline rollback requires --confirm-restore-target before restoring target tenant rows",
                jobCode=args.job_code,
                phase="ROLLBACK_CONFIRMATION",
                restoredRows=0,
            )
        )
    job = load_job(args.job_store, args.job_code)
    if job.get("status") == "ROLLED_BACK":
        return command_result(public_job_payload(job, phase="RESTORE_TARGET", restoredRows=0))
    backup_index_path = Path(getattr(args, "backup_index", None) or job.get("backupIndexPath") or "")
    if not str(backup_index_path):
        return command_result(
            error_payload(
                "TENANT_CLONE_BACKUP_MISSING",
                "backup index is required before rollback can restore target data",
                jobCode=args.job_code,
                status=job.get("status", "FAILED"),
                phase="ROLLBACK_VALIDATE",
                restoredRows=0,
            )
        )
    backup_index = read_json_file(str(backup_index_path), "backup index")
    data_store_path = Path(args.offline_data_store)
    data_store = read_json_file(str(data_store_path), "offline data store")
    tables = data_store_tables(data_store)
    index_tables = backup_index.get("tables")
    if not isinstance(index_tables, list):
        raise TenantCloneError(
            error_payload(
                "TENANT_CLONE_BACKUP_INVALID",
                "backup index tables must be a list",
                jobCode=args.job_code,
                phase="RESTORE_TARGET",
                restoredRows=0,
            )
        )

    restored_tables = {table_name: [dict(row) for row in rows] for table_name, rows in tables.items()}
    restored_rows = 0
    target_tenant_id = backup_index.get("targetTenantId", job.get("targetTenantId"))
    for table in index_tables:
        if not isinstance(table, dict) or not isinstance(table.get("table"), str) or not isinstance(table.get("file"), str):
            raise TenantCloneError(
                error_payload(
                    "TENANT_CLONE_BACKUP_INVALID",
                    "backup index table entries must include table and file",
                    jobCode=args.job_code,
                    phase="RESTORE_TARGET",
                    restoredRows=0,
                )
            )
        table_name = table["table"]
        field = table.get("tenantField", "tenant_id")
        if not isinstance(field, str):
            raise TenantCloneError(
                error_payload(
                    "TENANT_CLONE_BACKUP_INVALID",
                    f"backup index tenantField must be a string for table: {table_name}",
                    jobCode=args.job_code,
                    phase="RESTORE_TARGET",
                    restoredRows=0,
                )
            )
        backup_rows = read_jsonl(backup_index_path.parent / table["file"])
        current_rows = [
            row for row in restored_tables.get(table_name, []) if row.get(field) != target_tenant_id
        ]
        positions = table.get("positions")
        if not isinstance(positions, list) or len(positions) != len(backup_rows):
            raise TenantCloneError(
                error_payload(
                    "TENANT_CLONE_BACKUP_INVALID",
                    f"backup index positions do not match backup rows for table: {table_name}",
                    jobCode=args.job_code,
                    phase="RESTORE_TARGET",
                    restoredRows=0,
                )
            )
        for position, row in sorted(zip(positions, backup_rows), key=lambda item: int(item[0])):
            current_rows.insert(min(int(position), len(current_rows)), row)
        restored_tables[table_name] = current_rows
        restored_rows += len(backup_rows)

    updated_store = dict(data_store)
    updated_store["tables"] = restored_tables
    write_json_file_atomically(data_store_path, updated_store)
    job.update(
        {
            "status": "ROLLED_BACK",
            "currentPhase": "RESTORE_TARGET",
            "backupIndexPath": str(backup_index_path),
        }
    )
    save_job(args.job_store, args.job_code, job)
    return command_result(public_job_payload(job, phase="RESTORE_TARGET", restoredRows=restored_rows))
