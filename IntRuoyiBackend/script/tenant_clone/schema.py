from __future__ import annotations

from pathlib import Path
from typing import Any

from .contract import load_json_file
from .result import error_result, success_result


SCHEMA_PHASE = "SCHEMA_CHECK"
UNSCOPED_UNIQUE_CODE = "TENANT_CLONE_SCHEMA_UNIQUE_INDEX_NOT_TENANT_SCOPED"


def check_schema_unique_indexes(schema_inventory: dict[str, Any], *, tenant_field: str) -> dict[str, Any]:
    tables = schema_inventory.get("tables")
    if not isinstance(tables, list):
        raise ValueError("schemaInventory.tables must be a list")

    violations: list[dict[str, Any]] = []
    for table in tables:
        if not isinstance(table, dict) or not isinstance(table.get("table"), str):
            raise ValueError("each schema table must be an object with table name")
        if table.get("tenantField") != tenant_field:
            continue
        for index in _unique_indexes(table):
            if index.get("primary") is True:
                continue
            columns = index.get("columns")
            if not isinstance(columns, list) or not all(isinstance(column, str) for column in columns):
                raise ValueError(f"unique index columns must be a string list: {table['table']}")
            if tenant_field not in columns:
                violations.append(
                    {
                        "table": table["table"],
                        "index": str(index.get("name", "")),
                        "columns": columns,
                        "requiredTenantField": tenant_field,
                    }
                )

    if violations:
        return error_result(
            UNSCOPED_UNIQUE_CODE,
            f"{len(violations)} unique indexes are not tenant-scoped",
            phase=SCHEMA_PHASE,
            violations=violations,
        )

    return success_result(phase=SCHEMA_PHASE, violations=[])


def check_schema_inventory_file(schema_inventory_path: str | Path, *, tenant_field: str) -> dict[str, Any]:
    return check_schema_unique_indexes(load_json_file(schema_inventory_path), tenant_field=tenant_field)


def _unique_indexes(table: dict[str, Any]) -> list[dict[str, Any]]:
    indexes = table.get("uniqueIndexes", [])
    if not isinstance(indexes, list):
        raise ValueError(f"uniqueIndexes must be a list: {table['table']}")
    for index in indexes:
        if not isinstance(index, dict):
            raise ValueError(f"uniqueIndexes must contain objects: {table['table']}")
    return indexes

