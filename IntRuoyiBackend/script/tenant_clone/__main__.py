from __future__ import annotations

import argparse
import json
import sys
from typing import Any

from . import workflow


class JsonArgumentParser(argparse.ArgumentParser):
    def error(self, message: str) -> None:
        raise workflow.TenantCloneError(
            workflow.error_payload(
                "TENANT_CLONE_INVALID_ARGUMENT",
                message,
                phase="ARG_PARSE",
            )
        )


def build_parser() -> argparse.ArgumentParser:
    parser = JsonArgumentParser(prog="tenant_clone")
    subparsers = parser.add_subparsers(dest="command", required=True)

    precheck = subparsers.add_parser("precheck")
    precheck.add_argument("--source-tenant-id", type=int, required=True)
    precheck.add_argument("--target-tenant-id", type=int, required=True)
    precheck.add_argument("--profile", required=True)
    precheck.add_argument("--contract", required=True)
    precheck.add_argument("--schema-inventory")
    precheck.add_argument("--candidate-inventory")
    precheck.add_argument("--tenant-field", default="tenant_id")
    precheck.add_argument("--job-code")
    precheck.add_argument("--offline", action="store_true")

    create_job = subparsers.add_parser("create-job")
    create_job.add_argument("--source-tenant-id", type=int, required=True)
    create_job.add_argument("--target-tenant-id", type=int, required=True)
    create_job.add_argument("--profile", required=True)
    create_job.add_argument("--mode", required=True)
    create_job.add_argument("--contract", required=True)
    create_job.add_argument("--job-code", required=True)
    create_job.add_argument("--job-store", required=True)
    create_job.add_argument("--offline", action="store_true")

    status = subparsers.add_parser("status")
    status.add_argument("--job-code", required=True)
    status.add_argument("--job-store", required=True)

    execute = subparsers.add_parser("execute")
    execute.add_argument("--job-code", required=True)
    execute.add_argument("--job-store")
    execute.add_argument("--contract")
    execute.add_argument("--offline-data-store")
    execute.add_argument("--target-counts")
    execute.add_argument("--confirm-clear-target", action="store_true")
    execute.add_argument("--backup-dir")
    execute.add_argument("--backup-index")

    rollback = subparsers.add_parser("rollback")
    rollback.add_argument("--job-code", required=True)
    rollback.add_argument("--job-store")
    rollback.add_argument("--job-state")
    rollback.add_argument("--offline-data-store")
    rollback.add_argument("--backup-index")
    rollback.add_argument("--confirm-restore-target", action="store_true")

    validate_contract = subparsers.add_parser("validate-contract")
    validate_contract.add_argument("--contract", required=True)
    validate_contract.add_argument("--candidate-inventory", required=True)
    validate_contract.add_argument("--require-reference-fields", action="store_true")

    check_schema = subparsers.add_parser("check-schema")
    check_schema.add_argument("--schema-inventory", required=True)
    check_schema.add_argument("--tenant-field", required=True)

    schema_ddl = subparsers.add_parser("schema-ddl")
    schema_ddl.add_argument("--name", required=True)

    return parser


def run(args: argparse.Namespace) -> workflow.CommandResult:
    if args.command == "precheck":
        return workflow.precheck(args)
    if args.command == "create-job":
        return workflow.create_job(args)
    if args.command == "status":
        return workflow.status(args)
    if args.command == "execute":
        return workflow.execute(args)
    if args.command == "rollback":
        return workflow.rollback(args)
    if args.command == "validate-contract":
        return workflow.validate_contract(args)
    if args.command == "check-schema":
        return workflow.check_schema(args)
    if args.command == "schema-ddl":
        return workflow.schema_ddl(args)
    raise workflow.TenantCloneError(
        workflow.error_payload(
            "TENANT_CLONE_INVALID_ARGUMENT",
            f"unsupported command: {args.command}",
            phase="ARG_PARSE",
        )
    )


def write_json(payload: dict[str, Any]) -> None:
    sys.stdout.write(json.dumps(payload, ensure_ascii=False, separators=(",", ":")) + "\n")


def main(argv: list[str] | None = None) -> int:
    try:
        args = build_parser().parse_args(argv)
        result = run(args)
    except workflow.TenantCloneError as exc:
        result = workflow.command_result(exc.payload)
    except Exception as exc:
        result = workflow.command_result(
            workflow.error_payload(
                "TENANT_CLONE_UNHANDLED_ERROR",
                str(exc),
                phase="UNHANDLED_ERROR",
            )
        )
    write_json(result.payload)
    return result.exit_code


if __name__ == "__main__":
    raise SystemExit(main())
