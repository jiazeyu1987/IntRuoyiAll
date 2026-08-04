#!/usr/bin/env python3
"""Read-only manifest gate for authorized P0 runtime repair packages."""

from __future__ import annotations

import argparse
import json
import os
import sys
from pathlib import Path
from typing import Any

SCRIPT_DIR = Path(__file__).resolve().parent
sys.path.insert(0, str(SCRIPT_DIR))

from verify_p0_runtime_migration import REQUIRED_ENV  # noqa: E402


ALLOWED_TARGETS = {
    ("mes_pro_process_pool_pqc_record", "production_submit_event_id"),
    ("mes_pro_process_pool_event", "event_idempotency_key"),
    ("mes_pro_process_pool_event", "recordbook_entry_id"),
    ("mes_pro_process_pool_quantity_fragment", "production_submit_event_id"),
}

ALLOWED_FORMAL_SOURCE_TYPES = {
    "MES_PRO_PROCESS_POOL_EVENT.PRODUCTION_SUBMIT",
    "MES_PRO_EDHR_RECORDBOOK_ENTRY",
    "MES_PRO_EDHR_RECORDBOOK_EVENT",
    "SIGNED_BUSINESS_RECONSTRUCTION",
}

REQUIRED_TOP_LEVEL = {
    "authorization",
    "backupEvidence",
    "rollbackEvidence",
    "dryRun",
    "entries",
}

REQUIRED_AUTHORIZATION = {
    "businessApprovalId",
    "businessOwner",
    "dbaApprovalId",
    "dbaOwner",
    "maintenanceWindow",
}

REQUIRED_BACKUP = {
    "backupId",
    "backupLocation",
    "checksum",
    "capturedAt",
}

REQUIRED_ROLLBACK = {
    "rollbackPlanId",
    "restoreCommandReference",
    "verificationQueryReference",
}

REQUIRED_DRY_RUN = {
    "targetRowCount",
    "manifestEntryCount",
}

REQUIRED_ENTRY = {
    "tenantId",
    "table",
    "primaryKey",
    "targetColumn",
    "oldValue",
    "newValue",
    "formalSourceType",
    "formalSourceId",
    "reason",
    "reviewer",
}

MANIFEST_SCHEMA = {
    "authorization": sorted(REQUIRED_AUTHORIZATION),
    "backupEvidence": sorted(REQUIRED_BACKUP),
    "rollbackEvidence": sorted(REQUIRED_ROLLBACK),
    "dryRun": sorted(REQUIRED_DRY_RUN),
    "entries": sorted(REQUIRED_ENTRY),
}


def non_empty(value: Any) -> bool:
    if value is None:
        return False
    if isinstance(value, str):
        return value.strip() not in {"", "--", "MISSING", "UNKNOWN", "TODO"}
    return True


def missing_keys(mapping: Any, required: set[str]) -> list[str]:
    if not isinstance(mapping, dict):
        return sorted(required)
    return sorted(key for key in required if key not in mapping or not non_empty(mapping.get(key)))


def load_manifest(path: Path) -> tuple[dict[str, Any] | None, list[dict[str, Any]]]:
    if not path.exists():
        return None, [
            {
                "code": "P0_RUNTIME_BACKFILL_REPAIR_MANIFEST_MISSING",
                "path": str(path),
                "message": "A row-level repair manifest JSON file is required before any runtime repair can be considered.",
            }
        ]
    try:
        payload = json.loads(path.read_text(encoding="utf-8"))
    except Exception as exc:
        return None, [
            {
                "code": "P0_RUNTIME_BACKFILL_REPAIR_MANIFEST_INVALID_JSON",
                "path": str(path),
                "message": str(exc),
            }
        ]
    if not isinstance(payload, dict):
        return None, [
            {
                "code": "P0_RUNTIME_BACKFILL_REPAIR_MANIFEST_INVALID_JSON",
                "path": str(path),
                "message": "Repair manifest must be a JSON object.",
            }
        ]
    return payload, []


def validate_manifest(payload: dict[str, Any]) -> list[dict[str, Any]]:
    blockers: list[dict[str, Any]] = []
    top_missing = missing_keys(payload, REQUIRED_TOP_LEVEL)
    if top_missing:
        blockers.append(
            {
                "code": "P0_RUNTIME_BACKFILL_REPAIR_MANIFEST_BLOCKED",
                "missing": top_missing,
                "message": "Repair manifest is missing required top-level sections.",
            }
        )

    authorization_missing = missing_keys(payload.get("authorization"), REQUIRED_AUTHORIZATION)
    if authorization_missing:
        blockers.append(
            {
                "code": "P0_RUNTIME_BACKFILL_REPAIR_MANIFEST_AUTHORIZATION_MISSING",
                "missing": authorization_missing,
                "message": "Business and DBA authorization evidence is required.",
            }
        )

    backup_missing = missing_keys(payload.get("backupEvidence"), REQUIRED_BACKUP)
    if backup_missing:
        blockers.append(
            {
                "code": "P0_RUNTIME_BACKFILL_REPAIR_MANIFEST_BACKUP_MISSING",
                "missing": backup_missing,
                "message": "Backup evidence is required before runtime repair.",
            }
        )

    rollback_missing = missing_keys(payload.get("rollbackEvidence"), REQUIRED_ROLLBACK)
    if rollback_missing:
        blockers.append(
            {
                "code": "P0_RUNTIME_BACKFILL_REPAIR_MANIFEST_ROLLBACK_MISSING",
                "missing": rollback_missing,
                "message": "Rollback evidence is required before runtime repair.",
            }
        )

    entries = payload.get("entries")
    if not isinstance(entries, list) or not entries:
        blockers.append(
            {
                "code": "P0_RUNTIME_BACKFILL_REPAIR_MANIFEST_ENTRY_INVALID",
                "message": "Repair manifest entries must be a non-empty list.",
            }
        )
        entries = []

    seen_targets: set[tuple[str, str, str, str]] = set()
    for index, entry in enumerate(entries):
        if not isinstance(entry, dict):
            entry_missing = sorted(REQUIRED_ENTRY)
        else:
            entry_missing = []
            for key in REQUIRED_ENTRY:
                if key not in entry:
                    entry_missing.append(key)
                elif key != "oldValue" and not non_empty(entry.get(key)):
                    entry_missing.append(key)
        if entry_missing:
            blockers.append(
                {
                    "code": "P0_RUNTIME_BACKFILL_REPAIR_MANIFEST_ENTRY_INVALID",
                    "entryIndex": index,
                    "missing": entry_missing,
                    "message": "Every repair manifest entry requires row identity, old/new values, formal source, reason, and reviewer.",
                }
            )
            continue
        table = str(entry.get("table"))
        column = str(entry.get("targetColumn"))
        target = (table, column)
        if target not in ALLOWED_TARGETS:
            blockers.append(
                {
                    "code": "P0_RUNTIME_BACKFILL_REPAIR_MANIFEST_ENTRY_INVALID",
                    "entryIndex": index,
                    "table": table,
                    "targetColumn": column,
                    "message": "Repair manifest entry targets a field outside the P0 runtime backfill scope.",
                }
            )
        source_type = str(entry.get("formalSourceType"))
        if source_type not in ALLOWED_FORMAL_SOURCE_TYPES:
            blockers.append(
                {
                    "code": "P0_RUNTIME_BACKFILL_REPAIR_MANIFEST_FORMAL_SOURCE_INVALID",
                    "entryIndex": index,
                    "formalSourceType": source_type,
                    "message": "Repair manifest entry must use an approved formal source type.",
                }
            )
        unique_key = (str(entry.get("tenantId")), table, str(entry.get("primaryKey")), column)
        if unique_key in seen_targets:
            blockers.append(
                {
                    "code": "P0_RUNTIME_BACKFILL_REPAIR_MANIFEST_ENTRY_INVALID",
                    "entryIndex": index,
                    "message": "Repair manifest contains duplicate target rows for the same tenant, table, primary key, and column.",
                }
            )
        seen_targets.add(unique_key)

    dry_run = payload.get("dryRun")
    dry_run_missing = missing_keys(dry_run, REQUIRED_DRY_RUN)
    if dry_run_missing:
        blockers.append(
            {
                "code": "P0_RUNTIME_BACKFILL_REPAIR_MANIFEST_DRY_RUN_MISMATCH",
                "missing": dry_run_missing,
                "message": "Dry-run row counts must be present and match the manifest entry count.",
            }
        )
    elif isinstance(dry_run, dict):
        try:
            target_row_count = int(dry_run.get("targetRowCount"))
            manifest_entry_count = int(dry_run.get("manifestEntryCount"))
        except (TypeError, ValueError):
            blockers.append(
                {
                    "code": "P0_RUNTIME_BACKFILL_REPAIR_MANIFEST_DRY_RUN_MISMATCH",
                    "message": "Dry-run row counts must be integers.",
                }
            )
        else:
            if target_row_count != len(entries) or manifest_entry_count != len(entries):
                blockers.append(
                    {
                        "code": "P0_RUNTIME_BACKFILL_REPAIR_MANIFEST_DRY_RUN_MISMATCH",
                        "targetRowCount": target_row_count,
                        "manifestEntryCount": manifest_entry_count,
                        "actualEntryCount": len(entries),
                        "message": "Dry-run row counts must equal the number of manifest entries.",
                    }
                )

    return blockers


def contract_payload() -> dict[str, Any]:
    return {
        "requiredEnv": REQUIRED_ENV,
        "repairManifestSchema": MANIFEST_SCHEMA,
        "allowedTargets": sorted(f"{table}.{column}" for table, column in ALLOWED_TARGETS),
        "allowedFormalSourceTypes": sorted(ALLOWED_FORMAL_SOURCE_TYPES),
        "manifestPathEnv": "P0_RUNTIME_BACKFILL_REPAIR_MANIFEST",
        "databaseWriteAllowed": False,
    }


def emit(payload: dict[str, Any]) -> None:
    print(json.dumps(payload, ensure_ascii=False, indent=2, sort_keys=True))


def resolve_manifest_path(args: argparse.Namespace) -> Path | None:
    raw_path = args.manifest or os.environ.get("P0_RUNTIME_BACKFILL_REPAIR_MANIFEST")
    if not raw_path or not raw_path.strip():
        return None
    return Path(raw_path).resolve()


def main() -> int:
    parser = argparse.ArgumentParser(description="Verify a P0 runtime backfill repair manifest")
    parser.add_argument("--print-contract", action="store_true")
    parser.add_argument("--manifest")
    args = parser.parse_args()

    if args.print_contract:
        emit({"status": "PASS", **contract_payload()})
        return 0

    manifest_path = resolve_manifest_path(args)
    if manifest_path is None:
        emit(
            {
                "status": "BLOCKED",
                "blockers": [
                    {
                        "code": "P0_RUNTIME_BACKFILL_REPAIR_MANIFEST_MISSING",
                        "message": "Set P0_RUNTIME_BACKFILL_REPAIR_MANIFEST or pass --manifest before runtime repair.",
                    },
                    {
                        "code": "P0_RUNTIME_BACKFILL_REPAIR_MANIFEST_NO_DB_WRITE",
                        "message": "This gate is read-only and does not perform runtime repair.",
                    },
                ],
                **contract_payload(),
            }
        )
        return 2

    payload, load_blockers = load_manifest(manifest_path)
    blockers = load_blockers
    if payload is not None:
        blockers.extend(validate_manifest(payload))
    if blockers:
        blockers = [
            {
                "code": "P0_RUNTIME_BACKFILL_REPAIR_MANIFEST_NO_DB_WRITE",
                "message": "This gate is read-only and does not perform runtime repair.",
            },
            *blockers,
        ]
    status = "PASS" if not blockers else "BLOCKED"
    emit(
        {
            "status": status,
            "manifestPath": str(manifest_path),
            "entryCount": len(payload.get("entries", [])) if isinstance(payload, dict) else 0,
            "blockers": blockers,
            **contract_payload(),
        }
    )
    return 0 if status == "PASS" else 2


if __name__ == "__main__":
    sys.exit(main())
