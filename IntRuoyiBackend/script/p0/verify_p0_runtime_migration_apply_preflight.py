#!/usr/bin/env python3
"""Read-only preflight for applying P0 runtime migrations."""

from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path
from typing import Any

SCRIPT_DIR = Path(__file__).resolve().parent
sys.path.insert(0, str(SCRIPT_DIR))

from verify_p0_runtime_migration import (  # noqa: E402
    REQUIRED_ENV,
    RuntimeVerifierError,
    connect,
    load_config,
)


PREFLIGHT_CHECKS = [
    "pqc_record_requires_formal_production_submit_event_backfill",
    "production_submit_requires_event_idempotency_key_backfill",
    "production_submit_requires_recordbook_entry_backfill",
    "quantity_fragment_requires_formal_submit_root_backfill",
    "production_submit_event_idempotency_duplicate",
]


def fetch_count(cursor: Any, sql: str, params: tuple[Any, ...] = ()) -> int:
    cursor.execute(sql, params)
    row = cursor.fetchone()
    if not row:
        return 0
    if "count" in row:
        return int(row["count"])
    if "COUNT(*)" in row:
        return int(row["COUNT(*)"])
    return int(next(iter(row.values())))


def has_column(cursor: Any, database: str, table: str, column: str) -> bool:
    return (
        fetch_count(
            cursor,
            """
            SELECT COUNT(*) AS count
            FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = %s
              AND TABLE_NAME = %s
              AND COLUMN_NAME = %s
            """,
            (database, table, column),
        )
        > 0
    )


def active_count(cursor: Any, table: str, where: str = "1 = 1") -> int:
    return fetch_count(
        cursor,
        f"SELECT COUNT(*) AS count FROM `{table}` WHERE `deleted` = b'0' AND ({where})",
    )


def collect_backfill_blockers(cursor: Any, database: str) -> list[dict[str, Any]]:
    blockers: list[dict[str, Any]] = []

    if has_column(cursor, database, "mes_pro_process_pool_pqc_record", "production_submit_event_id"):
        pqc_missing = active_count(
            cursor,
            "mes_pro_process_pool_pqc_record",
            "`production_submit_event_id` IS NULL",
        )
    else:
        pqc_missing = active_count(cursor, "mes_pro_process_pool_pqc_record")
    if pqc_missing > 0:
        blockers.append(
            {
                "code": "P0_RUNTIME_APPLY_PREFLIGHT_PQC_BACKFILL_REQUIRED",
                "table": "mes_pro_process_pool_pqc_record",
                "column": "production_submit_event_id",
                "count": pqc_missing,
                "message": "PQC records require formal production_submit_event_id backfill before NOT NULL migration.",
            }
        )

    event_has_idempotency = has_column(cursor, database, "mes_pro_process_pool_event", "event_idempotency_key")
    if event_has_idempotency:
        missing_idempotency = active_count(
            cursor,
            "mes_pro_process_pool_event",
            "`event_type` = 'PRODUCTION_SUBMIT' AND (`event_idempotency_key` IS NULL OR TRIM(`event_idempotency_key`) = '')",
        )
    else:
        missing_idempotency = active_count(
            cursor,
            "mes_pro_process_pool_event",
            "`event_type` = 'PRODUCTION_SUBMIT'",
        )
    if missing_idempotency > 0:
        blockers.append(
            {
                "code": "P0_RUNTIME_APPLY_PREFLIGHT_EVENT_IDEMPOTENCY_BACKFILL_REQUIRED",
                "table": "mes_pro_process_pool_event",
                "column": "event_idempotency_key",
                "count": missing_idempotency,
                "message": "Production submit events require formal event_idempotency_key backfill before idempotency migration.",
            }
        )

    if has_column(cursor, database, "mes_pro_process_pool_event", "recordbook_entry_id"):
        missing_recordbook = active_count(
            cursor,
            "mes_pro_process_pool_event",
            "`event_type` = 'PRODUCTION_SUBMIT' AND `recordbook_entry_id` IS NULL",
        )
    else:
        missing_recordbook = active_count(
            cursor,
            "mes_pro_process_pool_event",
            "`event_type` = 'PRODUCTION_SUBMIT'",
        )
    if missing_recordbook > 0:
        blockers.append(
            {
                "code": "P0_RUNTIME_APPLY_PREFLIGHT_RECORDBOOK_BACKFILL_REQUIRED",
                "table": "mes_pro_process_pool_event",
                "column": "recordbook_entry_id",
                "count": missing_recordbook,
                "message": "Production submit events require formal recordbook_entry_id backfill before idempotency migration.",
            }
        )

    if has_column(cursor, database, "mes_pro_process_pool_quantity_fragment", "production_submit_event_id"):
        fragment_root_missing = fetch_count(
            cursor,
            """
            SELECT COUNT(*) AS count
            FROM `mes_pro_process_pool_quantity_fragment` fragment
            LEFT JOIN `mes_pro_process_pool_event` event
              ON event.`tenant_id` = fragment.`tenant_id`
             AND event.`id` = fragment.`event_id`
             AND event.`deleted` = b'0'
            WHERE fragment.`deleted` = b'0'
              AND (
                  fragment.`production_submit_event_id` IS NULL
                  OR event.`id` IS NULL
                  OR event.`event_type` <> 'PRODUCTION_SUBMIT'
                  OR fragment.`production_submit_event_id` <> fragment.`event_id`
              )
            """,
        )
    else:
        fragment_root_missing = fetch_count(
            cursor,
            """
            SELECT COUNT(*) AS count
            FROM `mes_pro_process_pool_quantity_fragment` fragment
            LEFT JOIN `mes_pro_process_pool_event` event
              ON event.`tenant_id` = fragment.`tenant_id`
             AND event.`id` = fragment.`event_id`
             AND event.`deleted` = b'0'
            WHERE fragment.`deleted` = b'0'
              AND (event.`id` IS NULL OR event.`event_type` <> 'PRODUCTION_SUBMIT')
            """,
        )
    if fragment_root_missing > 0:
        blockers.append(
            {
                "code": "P0_RUNTIME_APPLY_PREFLIGHT_FRAGMENT_ROOT_BACKFILL_REQUIRED",
                "table": "mes_pro_process_pool_quantity_fragment",
                "column": "production_submit_event_id",
                "count": fragment_root_missing,
                "message": "Quantity fragments require formal PRODUCTION_SUBMIT root event backfill before NOT NULL migration.",
            }
        )

    if event_has_idempotency:
        duplicate_count = fetch_count(
            cursor,
            """
            SELECT COUNT(*) AS count
            FROM (
                SELECT `tenant_id`, `event_type`, `work_order_id`, `route_process_id`, `process_id`,
                       `actual_employee_id`, `event_idempotency_key`, `deleted`
                FROM `mes_pro_process_pool_event`
                WHERE `deleted` = b'0'
                  AND `event_type` = 'PRODUCTION_SUBMIT'
                  AND `event_idempotency_key` IS NOT NULL
                  AND TRIM(`event_idempotency_key`) <> ''
                GROUP BY `tenant_id`, `event_type`, `work_order_id`, `route_process_id`, `process_id`,
                         `actual_employee_id`, `event_idempotency_key`, `deleted`
                HAVING COUNT(*) > 1
            ) duplicate_groups
            """,
        )
        if duplicate_count > 0:
            blockers.append(
                {
                    "code": "P0_RUNTIME_APPLY_PREFLIGHT_EVENT_IDEMPOTENCY_DUPLICATE",
                    "table": "mes_pro_process_pool_event",
                    "index": "uk_mes_pro_process_pool_event_idem",
                    "count": duplicate_count,
                    "message": "Duplicate production submit idempotency groups would block the unique index migration.",
                }
            )

    return blockers


def contract_payload() -> dict[str, Any]:
    return {
        "requiredEnv": REQUIRED_ENV,
        "preflightChecks": PREFLIGHT_CHECKS,
        "formalColumns": [
            "production_submit_event_id",
            "event_idempotency_key",
            "recordbook_entry_id",
            "review_signature_id",
            "review_signature_user_id",
            "review_signature_snapshot_json",
        ],
    }


def run_preflight() -> tuple[str, list[dict[str, Any]], dict[str, Any]]:
    config = load_config()
    with connect(config) as connection:
        with connection.cursor() as cursor:
            blockers = collect_backfill_blockers(cursor, config.database)
    status = "PASS" if not blockers else "BLOCKED"
    if blockers:
        blockers = [
            {
                "code": "P0_RUNTIME_APPLY_PREFLIGHT_BLOCKED",
                "message": "Runtime P0 migrations require formal backfill or duplicate cleanup before safe application.",
            },
            *blockers,
        ]
    return status, blockers, {"database": config.database, "host": config.host, "port": config.port}


def emit(payload: dict[str, Any]) -> None:
    print(json.dumps(payload, ensure_ascii=False, indent=2, sort_keys=True))


def main() -> int:
    parser = argparse.ArgumentParser(description="Verify P0 runtime migration apply preflight")
    parser.add_argument("--print-contract", action="store_true")
    args = parser.parse_args()

    if args.print_contract:
        emit({"status": "PASS", **contract_payload()})
        return 0

    try:
        status, blockers, runtime = run_preflight()
        emit({"status": status, "runtime": runtime, "blockers": blockers, **contract_payload()})
        return 0 if status == "PASS" else 2
    except RuntimeVerifierError as exc:
        emit({"status": "BLOCKED", "blockers": [{"code": exc.code, "message": exc.message}], **contract_payload()})
        return 2
    except Exception as exc:
        emit({"status": "FAIL", "blockers": [{"code": "P0_RUNTIME_APPLY_PREFLIGHT_FAILED", "message": str(exc)}], **contract_payload()})
        return 1


if __name__ == "__main__":
    sys.exit(main())
