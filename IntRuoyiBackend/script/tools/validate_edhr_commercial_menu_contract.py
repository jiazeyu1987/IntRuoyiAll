from __future__ import annotations

import json
import re
import sys
from pathlib import Path
from typing import Any


REQUIRED_PAGE_FIELDS = {
    "key",
    "module",
    "name",
    "menuId",
    "menuPath",
    "component",
    "componentName",
    "permission",
    "pageType",
    "status",
}
ALLOWED_MODULES = {"T1", "T2", "T3", "T4", "T5", "T6"}
ALLOWED_PAGE_TYPES = {"list", "workbench", "matrix"}
ALLOWED_STATUS = {"planned", "existing"}
PERMISSION_PATTERN = re.compile(r"^mes:pro-edhr-[a-z0-9-]+:query$")


def _require(condition: bool, message: str) -> None:
    if not condition:
        raise ValueError(message)


def _check_unique(pages: list[dict[str, Any]], field: str) -> None:
    seen: dict[Any, str] = {}
    for page in pages:
        value = page[field]
        if value in seen:
            raise ValueError(f"duplicate {field}: {value} on {seen[value]} and {page['key']}")
        seen[value] = page["key"]


def validate_contract(contract: dict[str, Any]) -> None:
    _require(contract.get("schemaVersion") == 1, "schemaVersion must be 1")
    parent_menu = contract.get("parentMenu")
    _require(isinstance(parent_menu, dict), "parentMenu must be an object")
    _require(parent_menu.get("id") == 900220, "parentMenu.id must be 900220")

    role_binding = contract.get("roleBinding")
    _require(isinstance(role_binding, dict), "roleBinding must be an object")
    _require(role_binding.get("roleCode") == "tenant_admin", "roleBinding.roleCode must be tenant_admin")
    _require(role_binding.get("packageAnchorMenuId") == 900220, "package anchor must be 900220")
    _require(role_binding.get("failFast") is True, "roleBinding.failFast must be true")

    pages = contract.get("pages")
    _require(isinstance(pages, list) and pages, "pages must be a non-empty list")
    modules = set()
    for page in pages:
        _require(isinstance(page, dict), "each page must be an object")
        missing = REQUIRED_PAGE_FIELDS - set(page)
        _require(not missing, f"page {page.get('key', '<unknown>')} missing fields: {sorted(missing)}")
        _require(page["module"] in ALLOWED_MODULES, f"invalid module for {page['key']}")
        modules.add(page["module"])
        _require(page["pageType"] in ALLOWED_PAGE_TYPES, f"invalid pageType for {page['key']}")
        _require(page["status"] in ALLOWED_STATUS, f"invalid status for {page['key']}")
        _require(str(page["key"]).startswith("edhr-"), f"page key must start with edhr-: {page['key']}")
        _require(
            str(page["menuPath"]).startswith("/mes/pro/feedback/edhr-"),
            f"menuPath must be under /mes/pro/feedback/edhr-: {page['key']}",
        )
        _require(PERMISSION_PATTERN.match(page["permission"]) is not None, f"invalid permission: {page['permission']}")
        if page["status"] == "planned":
            _require(920100 <= int(page["menuId"]) < 920300, f"planned menuId outside 920100-920299: {page['key']}")
            _require(
                str(page["component"]).startswith("mes/pro/edhr-commercial/"),
                f"planned component must use commercial skeleton root: {page['key']}",
            )
            _require(str(page["componentName"]).startswith("MesProEdhr"), f"planned componentName invalid: {page['key']}")

    _require(modules == ALLOWED_MODULES, f"contract must cover T1-T6, got {sorted(modules)}")
    for field in ["key", "menuId", "menuPath", "component", "componentName", "permission"]:
        _check_unique(pages, field)


def validate_template(template_text: str) -> None:
    required_tokens = [
        "edhr-commercial-menu-template",
        "{{TASK_ID}}",
        "{{MENU_ROWS}}",
        "{{BUTTON_ROWS}}",
        "SIGNAL SQLSTATE '45000'",
        "JSON_VALID",
        "JSON_TABLE",
        "JSON_ARRAYAGG",
        "system_tenant_package",
        "system_role_menu",
        "tenant_admin",
        "900220",
    ]
    for token in required_tokens:
        _require(token in template_text, f"template missing required token: {token}")

    upper_text = template_text.upper()
    for forbidden in ["INSERT IGNORE", "ON DUPLICATE KEY UPDATE", "DELETE FROM `SYSTEM_ROLE_MENU`", "DROP TABLE"]:
        _require(forbidden not in upper_text, f"template contains forbidden token: {forbidden}")


def main(argv: list[str]) -> int:
    if len(argv) != 3:
        print("Usage: validate_edhr_commercial_menu_contract.py <contract.json> <template.sql>", file=sys.stderr)
        return 2

    contract_path = Path(argv[1])
    template_path = Path(argv[2])
    contract = json.loads(contract_path.read_text(encoding="utf-8"))
    template = template_path.read_text(encoding="utf-8")
    validate_contract(contract)
    validate_template(template)
    print("eDHR commercial menu contract is valid.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main(sys.argv))
