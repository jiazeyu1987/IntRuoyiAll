#!/usr/bin/env python3
"""Read-only repair authorization gate for P0 runtime backfill blockers."""

from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path
from typing import Any

SCRIPT_DIR = Path(__file__).resolve().parent
sys.path.insert(0, str(SCRIPT_DIR))

from verify_p0_runtime_backfill_sources import (  # noqa: E402
    SOURCE_CHECKS,
    collect_backfill_source_audit,
)
from verify_p0_runtime_migration import (  # noqa: E402
    REQUIRED_ENV,
    RuntimeVerifierError,
    connect,
    load_config,
)
from verify_p0_runtime_migration_apply_preflight import (  # noqa: E402
    PREFLIGHT_CHECKS,
    collect_backfill_blockers,
)


SOURCE_AUDIT_SCRIPT = "verify_p0_runtime_backfill_sources.py"
APPLY_PREFLIGHT_SCRIPT = "verify_p0_runtime_migration_apply_preflight.py"

REPAIR_PLAN_CHECKS = [
    "no_database_write_from_plan_gate",
    "formal_source_audit_must_pass_before_backfill",
    "business_authorization_required_for_any_historical_write",
    "backup_and_rollback_required_before_authorized_repair",
    "post_repair_verification_chain_required",
]

ACCEPTABLE_FORMAL_SOURCES = [
    "unique mes_pro_process_pool_event row with event_type PRODUCTION_SUBMIT",
    "unique mes_pro_edhr_recordbook_entry row with stable idempotency_key",
    "unique mes_pro_edhr_recordbook_event row linked to the formal entry",
    "signed business reconstruction file that names source system, owner, tenant, table, primary key, and target field",
]

AUTHORIZATION_REQUIREMENTS = [
    "business owner approval for the exact tenant and row scope",
    "DBA approval for the exact runtime database and maintenance window",
    "task-owned backup evidence with restore command and checksum",
    "row-level repair manifest with old value, new value, formal source reference, and reviewer",
    "dry-run output showing the same target row count as the approved manifest",
]

ROLLBACK_REQUIREMENTS = [
    "pre-repair export for every target table and primary key",
    "reversible restore script reviewed against the same row manifest",
    "post-rollback verification query for every repaired column",
    "explicit owner sign-off before removing backup artifacts",
]

POST_REPAIR_VERIFICATION = [
    SOURCE_AUDIT_SCRIPT,
    APPLY_PREFLIGHT_SCRIPT,
    "verify_p0_runtime_migration.py",
    "p0-production-execution-loop-real.e2e.js",
    "verify_p0_completion_gate.py",
]


def detail_blockers(blockers: list[dict[str, Any]]) -> list[dict[str, Any]]:
    return [blocker for blocker in blockers if blocker.get("code") != "P0_RUNTIME_BACKFILL_SOURCE_BLOCKED"]


def blocker_codes(blockers: list[dict[str, Any]]) -> list[str]:
    return sorted({str(blocker.get("code")) for blocker in blockers if blocker.get("code")})


def count_blocked_rows(blockers: list[dict[str, Any]]) -> int:
    return sum(int(blocker.get("count") or 0) for blocker in blockers)


def build_repair_plan(
    preflight_blockers: list[dict[str, Any]],
    source_blockers: list[dict[str, Any]],
    source_summary: dict[str, Any],
) -> tuple[str, list[dict[str, Any]], dict[str, Any]]:
    source_detail = detail_blockers(source_blockers)
    blockers: list[dict[str, Any]] = []

    if preflight_blockers:
        blockers.append(
            {
                "code": "P0_RUNTIME_BACKFILL_REPAIR_AUTHORIZATION_REQUIRED",
                "count": count_blocked_rows(preflight_blockers),
                "sourceBlockerCodes": blocker_codes(preflight_blockers),
                "message": "Runtime P0 migration blockers require explicit authorization before any historical repair.",
            }
        )

    if source_detail:
        blockers.append(
            {
                "code": "P0_RUNTIME_BACKFILL_REPAIR_UNDERIVABLE_SOURCE",
                "count": count_blocked_rows(source_detail),
                "sourceBlockerCodes": blocker_codes(source_detail),
                "message": "Some historical P0 blockers cannot be reconstructed from unique formal structured sources.",
            }
        )

    status = "PASS" if not blockers else "BLOCKED"
    if blockers:
        blockers = [
            {
                "code": "P0_RUNTIME_BACKFILL_REPAIR_PLAN_BLOCKED",
                "message": "The runtime repair plan is not executable until formal sources, approval, backup, and rollback are complete.",
            },
            {
                "code": "P0_RUNTIME_BACKFILL_REPAIR_NO_DB_WRITE",
                "message": "This gate is read-only and must not execute schema changes, DML, default filling, or synthetic repair.",
            },
            *blockers,
        ]

    plan = {
        "decision": "NO_REPAIR_EXECUTION_FROM_THIS_GATE" if blockers else "NO_RUNTIME_REPAIR_BLOCKERS_DETECTED",
        "databaseWriteAllowed": False,
        "businessAuthorizationRequired": bool(preflight_blockers),
        "sourceReconstructionRequired": bool(source_detail),
        "blockedRowCount": count_blocked_rows(preflight_blockers),
        "underivableRowCount": count_blocked_rows(source_detail),
        "preflightBlockerCodes": blocker_codes(preflight_blockers),
        "sourceBlockerCodes": blocker_codes(source_detail),
        "sourceSummary": source_summary,
        "acceptableFormalSources": ACCEPTABLE_FORMAL_SOURCES,
        "authorizationRequirements": AUTHORIZATION_REQUIREMENTS,
        "rollbackRequirements": ROLLBACK_REQUIREMENTS,
        "postRepairVerification": POST_REPAIR_VERIFICATION,
        "requiredSequence": [
            "capture exact runtime blockers from read-only gates",
            "obtain signed business and DBA authorization",
            "capture task-owned backup and rollback evidence",
            "execute only the approved row manifest in the approved maintenance window",
            "rerun source audit, apply preflight, runtime migration verifier, real E2E, and completion gate",
        ],
    }
    return status, blockers, plan


def contract_payload() -> dict[str, Any]:
    return {
        "requiredEnv": REQUIRED_ENV,
        "repairPlanChecks": REPAIR_PLAN_CHECKS,
        "sourceChecks": SOURCE_CHECKS,
        "preflightChecks": PREFLIGHT_CHECKS,
        "acceptableFormalSources": ACCEPTABLE_FORMAL_SOURCES,
        "authorizationRequirements": AUTHORIZATION_REQUIREMENTS,
        "rollbackRequirements": ROLLBACK_REQUIREMENTS,
        "postRepairVerification": POST_REPAIR_VERIFICATION,
        "sourceAuditScript": SOURCE_AUDIT_SCRIPT,
        "applyPreflightScript": APPLY_PREFLIGHT_SCRIPT,
    }


def run_plan() -> tuple[str, list[dict[str, Any]], dict[str, Any], dict[str, Any]]:
    config = load_config()
    with connect(config) as connection:
        with connection.cursor() as cursor:
            preflight_blockers = collect_backfill_blockers(cursor, config.database)
            source_blockers, source_summary = collect_backfill_source_audit(cursor, config.database)
    status, blockers, plan = build_repair_plan(preflight_blockers, source_blockers, source_summary)
    runtime = {"database": config.database, "host": config.host, "port": config.port}
    return status, blockers, plan, runtime


def emit(payload: dict[str, Any]) -> None:
    print(json.dumps(payload, ensure_ascii=False, indent=2, sort_keys=True))


def main() -> int:
    parser = argparse.ArgumentParser(description="Verify P0 runtime backfill repair authorization plan")
    parser.add_argument("--print-contract", action="store_true")
    args = parser.parse_args()

    if args.print_contract:
        emit({"status": "PASS", **contract_payload()})
        return 0

    try:
        status, blockers, plan, runtime = run_plan()
        emit({"status": status, "runtime": runtime, "repairPlan": plan, "blockers": blockers, **contract_payload()})
        return 0 if status == "PASS" else 2
    except RuntimeVerifierError as exc:
        emit({"status": "BLOCKED", "blockers": [{"code": exc.code, "message": exc.message}], **contract_payload()})
        return 2
    except Exception as exc:
        emit(
            {
                "status": "FAIL",
                "blockers": [{"code": "P0_RUNTIME_BACKFILL_REPAIR_PLAN_FAILED", "message": str(exc)}],
                **contract_payload(),
            }
        )
        return 1


if __name__ == "__main__":
    sys.exit(main())
