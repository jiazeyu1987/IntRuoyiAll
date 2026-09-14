#!/usr/bin/env python3
"""Read-only verifier for P0 production execution runtime migrations."""

from __future__ import annotations

import argparse
import json
import os
import sys
from dataclasses import dataclass
from typing import Any


REQUIRED_ENV = [
    "P0_RUNTIME_DB_HOST",
    "P0_RUNTIME_DB_PORT",
    "P0_RUNTIME_DB_NAME",
    "P0_RUNTIME_DB_USER",
    "P0_RUNTIME_DB_PASSWORD",
]

REQUIRED_COLUMNS = [
    {
        "table": "mes_pro_process_pool_pqc_record",
        "column": "production_submit_event_id",
        "nullable": "NO",
    },
    {
        "table": "mes_pro_process_pool_event",
        "column": "event_idempotency_key",
        "nullable": "YES",
    },
    {
        "table": "mes_pro_process_pool_event",
        "column": "recordbook_entry_id",
        "nullable": "YES",
    },
    {
        "table": "mes_pro_process_pool_quantity_fragment",
        "column": "production_submit_event_id",
        "nullable": "NO",
    },
    {
        "table": "mes_pro_process_pool_submission_review",
        "column": "review_signature_id",
        "nullable": "YES",
    },
    {
        "table": "mes_pro_process_pool_submission_review",
        "column": "review_signature_user_id",
        "nullable": "YES",
    },
    {
        "table": "mes_pro_process_pool_submission_review",
        "column": "review_signature_snapshot_json",
        "nullable": "YES",
    },
]

REQUIRED_INDEXES = [
    {
        "table": "mes_pro_process_pool_pqc_record",
        "index": "idx_mes_pro_process_pool_pqc_submit_event",
    },
    {
        "table": "mes_pro_process_pool_event",
        "index": "uk_mes_pro_process_pool_event_idem",
    },
    {
        "table": "mes_pro_process_pool_quantity_fragment",
        "index": "idx_mes_pro_process_pool_fragment_submit_event",
    },
    {
        "table": "mes_pro_process_pool_submission_review",
        "index": "idx_mes_pp_review_signature",
    },
]

HISTORICAL_CHECKS = [
    {
        "key": "pqc_record_missing_production_submit_event",
        "sql": """
            SELECT COUNT(*)
            FROM mes_pro_process_pool_pqc_record
            WHERE deleted = b'0'
              AND production_submit_event_id IS NULL
        """,
    },
    {
        "key": "production_submit_missing_event_idempotency_key",
        "sql": """
            SELECT COUNT(*)
            FROM mes_pro_process_pool_event
            WHERE deleted = b'0'
              AND event_type = 'PRODUCTION_SUBMIT'
              AND (event_idempotency_key IS NULL OR TRIM(event_idempotency_key) = '')
        """,
    },
    {
        "key": "production_submit_missing_recordbook_entry",
        "sql": """
            SELECT COUNT(*)
            FROM mes_pro_process_pool_event
            WHERE deleted = b'0'
              AND event_type = 'PRODUCTION_SUBMIT'
              AND recordbook_entry_id IS NULL
        """,
    },
    {
        "key": "quantity_fragment_missing_production_submit_event",
        "sql": """
            SELECT COUNT(*)
            FROM mes_pro_process_pool_quantity_fragment fragment
            LEFT JOIN mes_pro_process_pool_event event
              ON event.tenant_id = fragment.tenant_id
             AND event.id = fragment.event_id
             AND event.deleted = b'0'
            WHERE fragment.deleted = b'0'
              AND (
                  fragment.production_submit_event_id IS NULL
                  OR event.id IS NULL
                  OR event.event_type <> 'PRODUCTION_SUBMIT'
                  OR fragment.production_submit_event_id <> fragment.event_id
              )
        """,
    },
]


@dataclass(frozen=True)
class RuntimeConfig:
    host: str
    port: int
    database: str
    user: str
    password: str


class RuntimeVerifierError(Exception):
    def __init__(self, code: str, message: str):
        super().__init__(message)
        self.code = code
        self.message = message


def collect_missing_env() -> list[str]:
    return [key for key in REQUIRED_ENV if not os.environ.get(key, "").strip()]


def load_config() -> RuntimeConfig:
    missing = collect_missing_env()
    if missing:
        raise RuntimeVerifierError(
            "P0_RUNTIME_ENV_MISSING",
            "Missing runtime database environment keys: " + ", ".join(missing),
        )
    try:
        port = int(os.environ["P0_RUNTIME_DB_PORT"])
    except ValueError as exc:
        raise RuntimeVerifierError("P0_RUNTIME_ENV_INVALID", "P0_RUNTIME_DB_PORT must be an integer") from exc
    return RuntimeConfig(
        host=os.environ["P0_RUNTIME_DB_HOST"].strip(),
        port=port,
        database=os.environ["P0_RUNTIME_DB_NAME"].strip(),
        user=os.environ["P0_RUNTIME_DB_USER"].strip(),
        password=os.environ["P0_RUNTIME_DB_PASSWORD"],
    )


def connect(config: RuntimeConfig):
    try:
        import pymysql
    except ImportError as exc:
        raise RuntimeVerifierError(
            "P0_RUNTIME_MYSQL_CLIENT_MISSING",
            "Python package pymysql is required for runtime migration verification",
        ) from exc
    return pymysql.connect(
        host=config.host,
        port=config.port,
        user=config.user,
        password=config.password,
        database=config.database,
        charset="utf8mb4",
        cursorclass=pymysql.cursors.DictCursor,
        autocommit=True,
        read_timeout=10,
        write_timeout=10,
        connect_timeout=10,
    )


def fetch_one(cursor: Any, sql: str, params: tuple[Any, ...]) -> dict[str, Any] | None:
    cursor.execute(sql, params)
    return cursor.fetchone()


def verify_columns(cursor: Any, database: str) -> list[dict[str, Any]]:
    blockers = []
    for item in REQUIRED_COLUMNS:
        row = fetch_one(
            cursor,
            """
            SELECT COLUMN_NAME, IS_NULLABLE
            FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = %s
              AND TABLE_NAME = %s
              AND COLUMN_NAME = %s
            """,
            (database, item["table"], item["column"]),
        )
        if row is None:
            blockers.append(
                {
                    "code": "P0_RUNTIME_MIGRATION_MISSING_COLUMN",
                    "table": item["table"],
                    "column": item["column"],
                }
            )
            continue
        if row["IS_NULLABLE"] != item["nullable"]:
            blockers.append(
                {
                    "code": "P0_RUNTIME_MIGRATION_COLUMN_NULLABILITY_MISMATCH",
                    "table": item["table"],
                    "column": item["column"],
                    "expected": item["nullable"],
                    "actual": row["IS_NULLABLE"],
                }
            )
    return blockers


def verify_indexes(cursor: Any, database: str) -> list[dict[str, Any]]:
    blockers = []
    for item in REQUIRED_INDEXES:
        row = fetch_one(
            cursor,
            """
            SELECT COUNT(*) AS count
            FROM information_schema.STATISTICS
            WHERE TABLE_SCHEMA = %s
              AND TABLE_NAME = %s
              AND INDEX_NAME = %s
            """,
            (database, item["table"], item["index"]),
        )
        if int(row["count"] if row else 0) == 0:
            blockers.append(
                {
                    "code": "P0_RUNTIME_MIGRATION_MISSING_INDEX",
                    "table": item["table"],
                    "index": item["index"],
                }
            )
    return blockers


def verify_history(cursor: Any) -> list[dict[str, Any]]:
    blockers = []
    for item in HISTORICAL_CHECKS:
        row = fetch_one(cursor, item["sql"], ())
        count = int(row["COUNT(*)"] if row and "COUNT(*)" in row else row["count"])
        if count > 0:
            blockers.append(
                {
                    "code": "P0_RUNTIME_HISTORICAL_BROKEN_LINK",
                    "check": item["key"],
                    "count": count,
                }
            )
    return blockers


def contract_payload() -> dict[str, Any]:
    return {
        "requiredEnv": REQUIRED_ENV,
        "requiredColumns": REQUIRED_COLUMNS,
        "requiredIndexes": REQUIRED_INDEXES,
        "historicalChecks": [item["key"] for item in HISTORICAL_CHECKS],
    }


def run_verification() -> tuple[str, list[dict[str, Any]], dict[str, Any]]:
    config = load_config()
    with connect(config) as connection:
        with connection.cursor() as cursor:
            schema_blockers = []
            schema_blockers.extend(verify_columns(cursor, config.database))
            schema_blockers.extend(verify_indexes(cursor, config.database))
            if schema_blockers:
                blockers = [
                    {
                        "code": "P0_RUNTIME_SCHEMA_BLOCKED",
                        "message": "Required runtime schema is missing; historical checks were skipped.",
                    },
                    *schema_blockers,
                ]
                return "BLOCKED", blockers, {
                    "database": config.database,
                    "host": config.host,
                    "port": config.port,
                }
            blockers = []
            blockers.extend(verify_history(cursor))
    status = "PASS" if not blockers else "BLOCKED"
    return status, blockers, {"database": config.database, "host": config.host, "port": config.port}


def emit(payload: dict[str, Any]) -> None:
    print(json.dumps(payload, ensure_ascii=False, indent=2, sort_keys=True))


def main() -> int:
    parser = argparse.ArgumentParser(description="Verify P0 runtime migration state")
    parser.add_argument("--print-contract", action="store_true")
    args = parser.parse_args()

    if args.print_contract:
        emit({"status": "PASS", **contract_payload()})
        return 0

    try:
        status, blockers, runtime = run_verification()
        emit(
            {
                "status": status,
                "runtime": runtime,
                "blockers": blockers,
                **contract_payload(),
            }
        )
        return 0 if status == "PASS" else 2
    except RuntimeVerifierError as exc:
        emit(
            {
                "status": "BLOCKED",
                "blockers": [{"code": exc.code, "message": exc.message}],
                **contract_payload(),
            }
        )
        return 2
    except Exception as exc:
        emit(
            {
                "status": "FAIL",
                "blockers": [{"code": "P0_RUNTIME_VERIFIER_FAILED", "message": str(exc)}],
                **contract_payload(),
            }
        )
        return 1


if __name__ == "__main__":
    sys.exit(main())
