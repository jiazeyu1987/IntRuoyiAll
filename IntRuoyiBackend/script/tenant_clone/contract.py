from __future__ import annotations

import json
from pathlib import Path
from typing import Any

from .result import error_result, success_result


CONTRACT_PHASE = "CONTRACT_VALIDATE"
MISSING_TABLE_CODE = "TENANT_CLONE_CONTRACT_MISSING_TABLE"
INVALID_REFERENCE_CODE = "TENANT_CLONE_CONTRACT_INVALID_REFERENCE"


def load_json_file(path: str | Path) -> dict[str, Any]:
    payload = json.loads(Path(path).read_text(encoding="utf-8"))
    if not isinstance(payload, dict):
        raise ValueError(f"JSON file must contain an object: {path}")
    return payload


def validate_contract(
    contract: dict[str, Any],
    candidate_inventory: dict[str, Any],
    *,
    require_reference_fields: bool = False,
) -> dict[str, Any]:
    contract_tables = _contract_tables_by_name(contract)
    candidate_tables = _candidate_tables(candidate_inventory)

    missing_tables = [table["table"] for table in candidate_tables if table["table"] not in contract_tables]
    if missing_tables and not require_reference_fields:
        return error_result(
            MISSING_TABLE_CODE,
            f"Contract is missing candidate tables: {', '.join(missing_tables)}",
            phase=CONTRACT_PHASE,
            missingTables=missing_tables,
        )

    invalid_references = _find_invalid_references(candidate_tables, contract_tables)
    if require_reference_fields and invalid_references:
        return error_result(
            INVALID_REFERENCE_CODE,
            "Contract contains undeclared reference fields",
            phase=CONTRACT_PHASE,
            invalidReferences=invalid_references,
        )

    if missing_tables:
        return error_result(
            MISSING_TABLE_CODE,
            f"Contract is missing candidate tables: {', '.join(missing_tables)}",
            phase=CONTRACT_PHASE,
            missingTables=missing_tables,
        )

    return success_result(
        phase=CONTRACT_PHASE,
        classifiedTables=len(contract_tables),
        candidateTables=len(candidate_tables),
    )


def validate_contract_files(
    contract_path: str | Path,
    candidate_inventory_path: str | Path,
    *,
    require_reference_fields: bool = False,
) -> dict[str, Any]:
    return validate_contract(
        load_json_file(contract_path),
        load_json_file(candidate_inventory_path),
        require_reference_fields=require_reference_fields,
    )


def _contract_tables_by_name(contract: dict[str, Any]) -> dict[str, dict[str, Any]]:
    tables = contract.get("tables")
    if not isinstance(tables, list):
        raise ValueError("contract.tables must be a list")
    result: dict[str, dict[str, Any]] = {}
    for table in tables:
        if not isinstance(table, dict) or not isinstance(table.get("table"), str):
            raise ValueError("each contract table must be an object with table name")
        result[table["table"]] = table
    return result


def _candidate_tables(candidate_inventory: dict[str, Any]) -> list[dict[str, Any]]:
    tables = candidate_inventory.get("tenantTables")
    if not isinstance(tables, list):
        raise ValueError("candidateInventory.tenantTables must be a list")
    for table in tables:
        if not isinstance(table, dict) or not isinstance(table.get("table"), str):
            raise ValueError("each candidate table must be an object with table name")
    return tables


def _find_invalid_references(
    candidate_tables: list[dict[str, Any]],
    contract_tables: dict[str, dict[str, Any]],
) -> list[dict[str, str]]:
    invalid: list[dict[str, str]] = []
    for candidate in candidate_tables:
        table_name = candidate["table"]
        contract_table = contract_tables.get(table_name)
        if contract_table is None:
            continue
        declared_fields = {
            reference["field"]
            for reference in contract_table.get("referenceFields", [])
            if isinstance(reference, dict) and isinstance(reference.get("field"), str)
        }
        for reference in candidate.get("referenceCandidates", []):
            if not isinstance(reference, dict):
                raise ValueError(f"referenceCandidates for {table_name} must contain objects")
            field = reference.get("field")
            if not isinstance(field, str):
                raise ValueError(f"reference candidate for {table_name} must include field")
            if field not in declared_fields:
                invalid.append(
                    {
                        "table": table_name,
                        "field": field,
                        "refTable": str(reference.get("refTable", "")),
                        "reason": "reference field is not declared in contract",
                    }
                )
    return invalid

