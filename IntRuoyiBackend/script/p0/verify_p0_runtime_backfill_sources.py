#!/usr/bin/env python3
"""Read-only audit for formal P0 runtime backfill sources."""

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


SOURCE_CHECKS = [
    "pqc_requires_unique_formal_production_submit_event",
    "production_submit_idempotency_requires_formal_recordbook_source",
    "production_submit_recordbook_entry_requires_existing_formal_entry",
    "quantity_fragment_requires_existing_production_submit_event",
]

ENTRY_SOURCE_TYPES = (
    "MES_RECORDBOOK_ENTRY",
    "MES_PRO_EDHR_RECORD_BOOK_ENTRY",
)
EVENT_SOURCE_TYPE = "MES_PRO_EDHR_RECORD_BOOK_EVENT"


def fetch_one(cursor: Any, sql: str, params: tuple[Any, ...] = ()) -> dict[str, Any] | None:
    cursor.execute(sql, params)
    return cursor.fetchone()


def fetch_int(cursor: Any, sql: str, params: tuple[Any, ...] = ()) -> int:
    row = fetch_one(cursor, sql, params)
    if not row:
        return 0
    for value in row.values():
        return int(value or 0)
    return 0


def has_column(cursor: Any, database: str, table: str, column: str) -> bool:
    return fetch_int(
        cursor,
        """
        SELECT COUNT(*) AS count
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = %s
          AND TABLE_NAME = %s
          AND COLUMN_NAME = %s
        """,
        (database, table, column),
    ) > 0


def has_table(cursor: Any, database: str, table: str) -> bool:
    return fetch_int(
        cursor,
        """
        SELECT COUNT(*) AS count
        FROM information_schema.TABLES
        WHERE TABLE_SCHEMA = %s
          AND TABLE_NAME = %s
        """,
        (database, table),
    ) > 0


def int_value(row: dict[str, Any], key: str) -> int:
    return int(row.get(key) or 0)


def row_from_sql(cursor: Any, sql: str, params: tuple[Any, ...] = ()) -> dict[str, int]:
    row = fetch_one(cursor, sql, params) or {}
    return {key: int(value or 0) for key, value in row.items()}


def require_source_tables(cursor: Any, database: str) -> list[dict[str, Any]]:
    blockers = []
    for table in ("mes_pro_edhr_recordbook_entry", "mes_pro_edhr_recordbook_event"):
        if not has_table(cursor, database, table):
            blockers.append(
                {
                    "code": "P0_RUNTIME_BACKFILL_SOURCE_TABLE_MISSING",
                    "table": table,
                    "message": "Formal recordbook source table is required before deriving P0 backfill values.",
                }
            )
    return blockers


def audit_pqc_sources(cursor: Any, database: str) -> tuple[dict[str, int], list[dict[str, Any]]]:
    has_submit_column = has_column(cursor, database, "mes_pro_process_pool_pqc_record", "production_submit_event_id")
    target_filter = ""
    if has_submit_column:
        target_filter = """
          AND (
              pqc.`production_submit_event_id` IS NULL
              OR pqc.`production_submit_event_id` <= 0
              OR NOT EXISTS (
                  SELECT 1
                  FROM `mes_pro_process_pool_event` existing_submit
                  WHERE existing_submit.`tenant_id` = pqc.`tenant_id`
                    AND existing_submit.`id` = pqc.`production_submit_event_id`
                    AND existing_submit.`deleted` = b'0'
                    AND existing_submit.`event_type` = 'PRODUCTION_SUBMIT'
              )
          )
        """

    stats = row_from_sql(
        cursor,
        f"""
        SELECT
            COUNT(*) AS targetRows,
            SUM(CASE WHEN candidate_count = 1 THEN 1 ELSE 0 END) AS derivableRows,
            SUM(CASE WHEN candidate_count = 0 THEN 1 ELSE 0 END) AS noCandidateRows,
            SUM(CASE WHEN candidate_count > 1 THEN 1 ELSE 0 END) AS multipleCandidateRows
        FROM (
            SELECT pqc.`id`, COUNT(submit_event.`id`) AS candidate_count
            FROM `mes_pro_process_pool_pqc_record` pqc
            JOIN `mes_pro_process_pool_event` pqc_event
              ON pqc_event.`tenant_id` = pqc.`tenant_id`
             AND pqc_event.`id` = pqc.`event_id`
             AND pqc_event.`deleted` = b'0'
             AND pqc_event.`event_type` = 'PQC_INSPECTION'
            LEFT JOIN `mes_pro_process_pool_event` submit_event
              ON submit_event.`tenant_id` = pqc.`tenant_id`
             AND submit_event.`deleted` = b'0'
             AND submit_event.`event_type` = 'PRODUCTION_SUBMIT'
             AND submit_event.`work_order_id` = pqc.`work_order_id`
             AND submit_event.`route_id` = pqc.`route_id`
             AND submit_event.`route_process_id` = pqc.`route_process_id`
             AND submit_event.`process_id` = pqc.`process_id`
            WHERE pqc.`deleted` = b'0'
            {target_filter}
            GROUP BY pqc.`id`
        ) candidate_summary
        """,
    )
    underivable = int_value(stats, "noCandidateRows") + int_value(stats, "multipleCandidateRows")
    blockers = []
    if underivable > 0:
        blockers.append(
            {
                "code": "P0_RUNTIME_BACKFILL_PQC_SOURCE_UNDERIVABLE",
                "table": "mes_pro_process_pool_pqc_record",
                "column": "production_submit_event_id",
                "count": underivable,
                "noCandidateRows": int_value(stats, "noCandidateRows"),
                "multipleCandidateRows": int_value(stats, "multipleCandidateRows"),
                "message": "PQC records cannot be uniquely bound to a formal PRODUCTION_SUBMIT event by structured context.",
            }
        )
    return stats, blockers


def recordbook_entry_join(has_recordbook_entry_column: bool) -> str:
    entry_id_terms = [
        "(event.`recordbook_source_type` IN %s AND entry.`id` = event.`recordbook_source_id`)",
        "(event.`recordbook_source_type` = %s AND entry.`id` = recordbook_event.`entry_id`)",
    ]
    if has_recordbook_entry_column:
        entry_id_terms.insert(0, "entry.`id` = event.`recordbook_entry_id`")
    return " OR ".join(entry_id_terms)


def audit_recordbook_sources(cursor: Any, database: str) -> tuple[dict[str, int], dict[str, int], list[dict[str, Any]]]:
    has_idempotency_column = has_column(cursor, database, "mes_pro_process_pool_event", "event_idempotency_key")
    has_recordbook_entry_column = has_column(cursor, database, "mes_pro_process_pool_event", "recordbook_entry_id")
    idempotency_filter = ""
    if has_idempotency_column:
        idempotency_filter = """
          AND (event.`event_idempotency_key` IS NULL OR TRIM(event.`event_idempotency_key`) = '')
        """
    recordbook_filter = ""
    if has_recordbook_entry_column:
        recordbook_filter = "AND event.`recordbook_entry_id` IS NULL"

    entry_join = recordbook_entry_join(has_recordbook_entry_column)
    base_join = f"""
        LEFT JOIN `mes_pro_edhr_recordbook_event` recordbook_event
          ON recordbook_event.`tenant_id` = event.`tenant_id`
         AND recordbook_event.`deleted` = b'0'
         AND event.`recordbook_source_type` = %s
         AND recordbook_event.`id` = event.`recordbook_source_id`
        LEFT JOIN `mes_pro_edhr_recordbook_entry` entry
          ON entry.`tenant_id` = event.`tenant_id`
         AND entry.`deleted` = b'0'
         AND ({entry_join})
    """
    params = (EVENT_SOURCE_TYPE, ENTRY_SOURCE_TYPES, EVENT_SOURCE_TYPE)

    idempotency_stats = row_from_sql(
        cursor,
        f"""
        SELECT
            COUNT(*) AS targetRows,
            SUM(CASE WHEN entry_count = 1 AND has_idempotency_key = 1 THEN 1 ELSE 0 END) AS derivableRows,
            SUM(CASE WHEN entry_count = 0 THEN 1 ELSE 0 END) AS missingEntryRows,
            SUM(CASE WHEN entry_count > 1 THEN 1 ELSE 0 END) AS multipleEntryRows,
            SUM(CASE WHEN entry_count = 1 AND has_idempotency_key = 0 THEN 1 ELSE 0 END) AS missingIdempotencyRows
        FROM (
            SELECT event.`id`,
                   COUNT(DISTINCT entry.`id`) AS entry_count,
                   MAX(CASE WHEN entry.`idempotency_key` IS NOT NULL AND TRIM(entry.`idempotency_key`) <> '' THEN 1 ELSE 0 END) AS has_idempotency_key
            FROM `mes_pro_process_pool_event` event
            {base_join}
            WHERE event.`deleted` = b'0'
              AND event.`event_type` = 'PRODUCTION_SUBMIT'
              {idempotency_filter}
            GROUP BY event.`id`
        ) source_summary
        """,
        params,
    )

    recordbook_stats = row_from_sql(
        cursor,
        f"""
        SELECT
            COUNT(*) AS targetRows,
            SUM(CASE WHEN entry_count = 1 THEN 1 ELSE 0 END) AS derivableRows,
            SUM(CASE WHEN entry_count = 0 THEN 1 ELSE 0 END) AS missingEntryRows,
            SUM(CASE WHEN entry_count > 1 THEN 1 ELSE 0 END) AS multipleEntryRows
        FROM (
            SELECT event.`id`,
                   COUNT(DISTINCT entry.`id`) AS entry_count
            FROM `mes_pro_process_pool_event` event
            {base_join}
            WHERE event.`deleted` = b'0'
              AND event.`event_type` = 'PRODUCTION_SUBMIT'
              {recordbook_filter}
            GROUP BY event.`id`
        ) source_summary
        """,
        params,
    )

    blockers = []
    idempotency_underivable = (
        int_value(idempotency_stats, "missingEntryRows")
        + int_value(idempotency_stats, "multipleEntryRows")
        + int_value(idempotency_stats, "missingIdempotencyRows")
    )
    if idempotency_underivable > 0:
        blockers.append(
            {
                "code": "P0_RUNTIME_BACKFILL_EVENT_IDEMPOTENCY_SOURCE_UNDERIVABLE",
                "table": "mes_pro_process_pool_event",
                "column": "event_idempotency_key",
                "count": idempotency_underivable,
                "missingEntryRows": int_value(idempotency_stats, "missingEntryRows"),
                "multipleEntryRows": int_value(idempotency_stats, "multipleEntryRows"),
                "missingIdempotencyRows": int_value(idempotency_stats, "missingIdempotencyRows"),
                "message": "Production submit event idempotency keys cannot be derived from a unique formal recordbook source.",
            }
        )

    recordbook_underivable = int_value(recordbook_stats, "missingEntryRows") + int_value(
        recordbook_stats, "multipleEntryRows"
    )
    if recordbook_underivable > 0:
        blockers.append(
            {
                "code": "P0_RUNTIME_BACKFILL_RECORDBOOK_ENTRY_SOURCE_UNDERIVABLE",
                "table": "mes_pro_process_pool_event",
                "column": "recordbook_entry_id",
                "count": recordbook_underivable,
                "missingEntryRows": int_value(recordbook_stats, "missingEntryRows"),
                "multipleEntryRows": int_value(recordbook_stats, "multipleEntryRows"),
                "message": "Production submit events cannot be bound to a unique formal recordbook entry.",
            }
        )

    return idempotency_stats, recordbook_stats, blockers


def audit_fragment_sources(cursor: Any, database: str) -> tuple[dict[str, int], list[dict[str, Any]]]:
    has_submit_column = has_column(cursor, database, "mes_pro_process_pool_quantity_fragment", "production_submit_event_id")
    target_filter = ""
    if has_submit_column:
        target_filter = """
          AND (
              fragment.`production_submit_event_id` IS NULL
              OR fragment.`production_submit_event_id` <= 0
              OR fragment.`production_submit_event_id` <> fragment.`event_id`
          )
        """

    stats = row_from_sql(
        cursor,
        f"""
        SELECT
            COUNT(*) AS targetRows,
            SUM(CASE WHEN submit_event.`id` IS NOT NULL THEN 1 ELSE 0 END) AS derivableRows,
            SUM(CASE WHEN submit_event.`id` IS NULL THEN 1 ELSE 0 END) AS missingSubmitEventRows
        FROM `mes_pro_process_pool_quantity_fragment` fragment
        LEFT JOIN `mes_pro_process_pool_event` submit_event
          ON submit_event.`tenant_id` = fragment.`tenant_id`
         AND submit_event.`id` = fragment.`event_id`
         AND submit_event.`deleted` = b'0'
         AND submit_event.`event_type` = 'PRODUCTION_SUBMIT'
        WHERE fragment.`deleted` = b'0'
        {target_filter}
        """,
    )
    blockers = []
    if int_value(stats, "missingSubmitEventRows") > 0:
        blockers.append(
            {
                "code": "P0_RUNTIME_BACKFILL_FRAGMENT_ROOT_SOURCE_UNDERIVABLE",
                "table": "mes_pro_process_pool_quantity_fragment",
                "column": "production_submit_event_id",
                "count": int_value(stats, "missingSubmitEventRows"),
                "message": "Quantity fragments cannot be bound to an existing formal PRODUCTION_SUBMIT root event.",
            }
        )
    return stats, blockers


def contract_payload() -> dict[str, Any]:
    return {
        "requiredEnv": REQUIRED_ENV,
        "sourceChecks": SOURCE_CHECKS,
        "formalSources": [
            "mes_pro_process_pool_event.PRODUCTION_SUBMIT",
            "mes_pro_edhr_recordbook_entry",
            "mes_pro_edhr_recordbook_event",
        ],
    }


def collect_backfill_source_audit(cursor: Any, database: str) -> tuple[list[dict[str, Any]], dict[str, Any]]:
    blockers = require_source_tables(cursor, database)
    pqc_stats, pqc_blockers = audit_pqc_sources(cursor, database)
    idempotency_stats, recordbook_stats, recordbook_blockers = audit_recordbook_sources(cursor, database)
    fragment_stats, fragment_blockers = audit_fragment_sources(cursor, database)
    blockers.extend(pqc_blockers)
    blockers.extend(recordbook_blockers)
    blockers.extend(fragment_blockers)
    summary = {
        "pqc": pqc_stats,
        "eventIdempotency": idempotency_stats,
        "recordbookEntry": recordbook_stats,
        "quantityFragment": fragment_stats,
    }
    if blockers:
        blockers = [
            {
                "code": "P0_RUNTIME_BACKFILL_SOURCE_BLOCKED",
                "message": "Runtime P0 historical data lacks unique formal sources for safe backfill.",
            },
            *blockers,
        ]
    return blockers, summary


def run_audit() -> tuple[str, list[dict[str, Any]], dict[str, Any], dict[str, Any]]:
    config = load_config()
    with connect(config) as connection:
        with connection.cursor() as cursor:
            blockers, summary = collect_backfill_source_audit(cursor, config.database)
    status = "PASS" if not blockers else "BLOCKED"
    return status, blockers, summary, {"database": config.database, "host": config.host, "port": config.port}


def emit(payload: dict[str, Any]) -> None:
    print(json.dumps(payload, ensure_ascii=False, indent=2, sort_keys=True))


def main() -> int:
    parser = argparse.ArgumentParser(description="Audit formal sources for P0 runtime backfill")
    parser.add_argument("--print-contract", action="store_true")
    args = parser.parse_args()

    if args.print_contract:
        emit({"status": "PASS", **contract_payload()})
        return 0

    try:
        status, blockers, summary, runtime = run_audit()
        emit({"status": status, "runtime": runtime, "summary": summary, "blockers": blockers, **contract_payload()})
        return 0 if status == "PASS" else 2
    except RuntimeVerifierError as exc:
        emit({"status": "BLOCKED", "blockers": [{"code": exc.code, "message": exc.message}], **contract_payload()})
        return 2
    except Exception as exc:
        emit(
            {
                "status": "FAIL",
                "blockers": [{"code": "P0_RUNTIME_BACKFILL_SOURCE_AUDIT_FAILED", "message": str(exc)}],
                **contract_payload(),
            }
        )
        return 1


if __name__ == "__main__":
    sys.exit(main())
