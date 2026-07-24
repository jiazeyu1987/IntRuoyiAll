from __future__ import annotations

import copy
import importlib.util
import json
from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]
CONTRACT_PATH = REPO_ROOT / "docs" / "edhr" / "commercial-page-menu-contract.json"
TEMPLATE_PATH = REPO_ROOT / "docs" / "edhr" / "templates" / "edhr-commercial-menu-template.sql"
VALIDATOR_PATH = REPO_ROOT / "script" / "tools" / "validate_edhr_commercial_menu_contract.py"

EXPECTED_PAGES = {
    "edhr-init-batch": ("T1", "mes:pro-edhr-init-batch:query"),
    "edhr-dhr-template": ("T1", "mes:pro-edhr-dhr-template:query"),
    "edhr-form-template": ("T2", "mes:pro-edhr-form-template:query"),
    "edhr-form-instance": ("T2", "mes:pro-edhr-form-instance:query"),
    "edhr-recordbook": ("T2", "mes:pro-edhr-recordbook:query"),
    "edhr-recordbook-entry": ("T2", "mes:pro-edhr-recordbook-entry:query"),
    "edhr-traveler": ("T3", "mes:pro-edhr-traveler:query"),
    "edhr-label": ("T3", "mes:pro-edhr-label:query"),
    "edhr-print-task": ("T3", "mes:pro-edhr-print-task:query"),
    "edhr-release": ("T4", "mes:pro-edhr-release:query"),
    "edhr-change": ("T4", "mes:pro-edhr-change:query"),
    "edhr-flow-intervention": ("T4", "mes:pro-edhr-flow-intervention:query"),
    "edhr-report": ("T5", "mes:pro-edhr-report:query"),
    "edhr-dashboard": ("T5", "mes:pro-edhr-dashboard:query"),
    "edhr-project-package": ("T5", "mes:pro-edhr-project-package:query"),
    "edhr-delivery": ("T6", "mes:pro-edhr-delivery:query"),
    "edhr-validation": ("T6", "mes:pro-edhr-validation:query"),
    "edhr-training": ("T6", "mes:pro-edhr-training:query"),
    "edhr-deployment-evidence": ("T6", "mes:pro-edhr-deployment-evidence:query"),
}


def load_contract() -> dict:
    assert CONTRACT_PATH.exists(), "eDHR commercial page/menu contract JSON must exist."
    return json.loads(CONTRACT_PATH.read_text(encoding="utf-8"))


def load_validator_module():
    assert VALIDATOR_PATH.exists(), "eDHR commercial menu contract validator must exist."
    spec = importlib.util.spec_from_file_location("edhr_commercial_contract_validator", VALIDATOR_PATH)
    assert spec is not None and spec.loader is not None
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


def test_contract_declares_all_coding_ready_pages() -> None:
    contract = load_contract()

    assert contract["schemaVersion"] == 1
    assert contract["parentMenu"]["id"] == 900220
    assert contract["roleBinding"]["roleCode"] == "tenant_admin"
    assert contract["roleBinding"]["packageAnchorMenuId"] == 900220
    assert contract["roleBinding"]["failFast"] is True

    pages = {page["key"]: page for page in contract["pages"]}
    assert set(pages) == set(EXPECTED_PAGES)

    for key, (module, permission) in EXPECTED_PAGES.items():
        page = pages[key]
        assert page["module"] == module
        assert page["permission"] == permission
        assert page["menuPath"].startswith("/mes/pro/feedback/edhr-")
        if page["status"] == "planned":
            assert page["component"].startswith("mes/pro/edhr-commercial/")
            assert page["componentName"].startswith("MesProEdhr")
        assert page["pageType"] in {"list", "workbench", "matrix"}
        assert page["status"] in {"planned", "existing"}


def test_contract_has_unique_ids_routes_components_and_permissions() -> None:
    pages = load_contract()["pages"]

    for field in ["menuId", "menuPath", "component", "componentName", "permission"]:
        values = [page[field] for page in pages]
        assert len(values) == len(set(values)), f"{field} must be unique in eDHR commercial menu contract."

    planned_ids = [page["menuId"] for page in pages if page["status"] == "planned"]
    assert all(920100 <= menu_id < 920300 for menu_id in planned_ids)

    existing = [page for page in pages if page["status"] == "existing"]
    assert existing == [
        {
            "key": "edhr-change",
            "module": "T4",
            "name": "eDHR变更记录",
            "menuId": 900235,
            "menuPath": "/mes/pro/feedback/edhr-change",
            "component": "mes/pro/edhr/RecordChangePage",
            "componentName": "MesProFeedbackEdhrRecordChange",
            "permission": "mes:pro-edhr-change:query",
            "pageType": "list",
            "status": "existing",
        }
    ]


def test_menu_sql_template_is_fail_fast_and_not_a_runtime_migration() -> None:
    assert TEMPLATE_PATH.exists(), "eDHR commercial menu SQL template must exist."
    text = TEMPLATE_PATH.read_text(encoding="utf-8")
    upper_text = text.upper()

    for required in [
        "edhr-commercial-menu-template",
        "{{MENU_ROWS}}",
        "{{BUTTON_ROWS}}",
        "{{TASK_ID}}",
        "SIGNAL SQLSTATE '45000'",
        "JSON_VALID",
        "JSON_TABLE",
        "JSON_ARRAYAGG",
        "system_tenant_package",
        "system_role_menu",
        "tenant_admin",
        "900220",
    ]:
        assert required in text

    assert "INSERT IGNORE" not in upper_text
    assert "ON DUPLICATE KEY UPDATE" not in upper_text
    assert "DELETE FROM `SYSTEM_ROLE_MENU`" not in upper_text
    assert "DROP TABLE" not in upper_text


def test_validator_rejects_duplicate_permission_and_bad_template() -> None:
    validator = load_validator_module()
    contract = load_contract()
    template_text = TEMPLATE_PATH.read_text(encoding="utf-8")

    validator.validate_contract(contract)
    validator.validate_template(template_text)

    duplicate = copy.deepcopy(contract)
    duplicate["pages"][1]["permission"] = duplicate["pages"][0]["permission"]
    try:
        validator.validate_contract(duplicate)
    except ValueError as exc:
        assert "duplicate permission" in str(exc)
    else:
        raise AssertionError("duplicate permission must fail fast")

    try:
        validator.validate_template(template_text.replace("SIGNAL SQLSTATE '45000'", ""))
    except ValueError as exc:
        assert "SIGNAL SQLSTATE" in str(exc)
    else:
        raise AssertionError("template without SIGNAL SQLSTATE must fail fast")
