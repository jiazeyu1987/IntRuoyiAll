from __future__ import annotations

import json
from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]


REQUIRED_FILES = [
    # P0 contracts
    "docs/edhr/existing-edhr-contract.md",
    "docs/edhr/commercial-page-menu-contract.json",
    "docs/edhr/templates/edhr-commercial-menu-template.sql",
    "script/tools/validate_edhr_commercial_menu_contract.py",
    "script/tests/test_edhr_unified_contract_static.py",
    "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProEdhrCommonStatus.java",
    "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProEdhrAuditEventContract.java",
    "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProEdhrIdempotencySupport.java",
    # T1 initialization
    "sql/mysql/20260618_mes_edhr_init_batch_precheck.sql",
    "script/tests/test_edhr_init_batch_schema_sql.py",
    "script/tests/test_edhr_init_batch_api_contract.py",
    "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/batchrecord/MesProEdhrInitBatchController.java",
    "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProEdhrInitBatchServiceImpl.java",
    # T2 forms
    "sql/mysql/20260618_mes_edhr_form_instance.sql",
    "script/tests/test_edhr_form_schema_sql.py",
    "script/tests/test_edhr_form_api_contract.py",
    "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/batchrecord/MesProEdhrFormTemplateController.java",
    "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/batchrecord/MesProEdhrFormInstanceController.java",
    "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProEdhrFormServiceImpl.java",
    # T3 traveler
    "sql/mysql/20260618_mes_edhr_traveler_instance_binding.sql",
    "script/tests/test_edhr_traveler_schema_sql.py",
    "script/tests/test_edhr_traveler_api_contract.py",
    "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/batchrecord/MesProEdhrTravelerController.java",
    "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProEdhrTravelerServiceImpl.java",
    # T4 release
    "sql/mysql/20260618_mes_edhr_release_precheck_engine.sql",
    "script/tests/test_edhr_release_precheck_schema_sql.py",
    "script/tests/test_edhr_release_precheck_api_contract.py",
    "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/batchrecord/MesProEdhrReleaseController.java",
    "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProEdhrReleaseServiceImpl.java",
    # T5 reports
    "sql/mysql/20260618_mes_edhr_report_catalog.sql",
    "script/tests/test_edhr_report_schema_sql.py",
    "script/tests/test_edhr_report_api_contract.py",
    "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/batchrecord/MesProEdhrReportCatalogController.java",
    "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProEdhrReportServiceImpl.java",
    # T6 delivery
    "sql/mysql/20260618_mes_edhr_delivery_cockpit.sql",
    "script/tests/test_edhr_delivery_schema_sql.py",
    "script/tests/test_edhr_delivery_api_contract.py",
    "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/batchrecord/MesProEdhrDeliveryCockpitController.java",
    "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProEdhrDeliveryServiceImpl.java",
]

SQL_FILES = [
    "sql/mysql/20260618_mes_edhr_init_batch_precheck.sql",
    "sql/mysql/20260618_mes_edhr_form_instance.sql",
    "sql/mysql/20260618_mes_edhr_traveler_instance_binding.sql",
    "sql/mysql/20260618_mes_edhr_release_precheck_engine.sql",
    "sql/mysql/20260618_mes_edhr_report_catalog.sql",
    "sql/mysql/20260618_mes_edhr_delivery_cockpit.sql",
]

EXPECTED_QUERY_PERMISSIONS = {
    "mes:pro-edhr-init-batch:query",
    "mes:pro-edhr-form-template:query",
    "mes:pro-edhr-form-instance:query",
    "mes:pro-edhr-traveler:query",
    "mes:pro-edhr-release:query",
    "mes:pro-edhr-report:query",
    "mes:pro-edhr-delivery:query",
}


def read_text(relative_path: str) -> str:
    path = REPO_ROOT / relative_path
    assert path.exists(), f"missing first-wave backend artifact: {relative_path}"
    return path.read_text(encoding="utf-8")


def test_first_wave_backend_artifacts_exist_on_one_baseline() -> None:
    missing = [relative for relative in REQUIRED_FILES if not (REPO_ROOT / relative).exists()]
    assert not missing, "first-wave backend integration is missing artifacts: " + ", ".join(missing)


def test_first_wave_menu_permissions_are_present_and_bound() -> None:
    sql_text = "\n".join(read_text(relative) for relative in SQL_FILES)

    for permission in EXPECTED_QUERY_PERMISSIONS:
        assert permission in sql_text, f"missing menu permission {permission}"

    for required in ["system_role_menu", "system_tenant_package", "tenant_admin", "JSON_VALID", "SIGNAL SQLSTATE '45000'"]:
        assert required in sql_text, f"first-wave SQL must retain fail-fast menu binding token {required}"

    forbidden_upper = sql_text.upper()
    assert "INSERT IGNORE" not in forbidden_upper
    assert "DELETE FROM `SYSTEM_ROLE_MENU`" not in forbidden_upper


def test_first_wave_commercial_menu_contract_covers_actual_first_wave_pages() -> None:
    contract = json.loads(read_text("docs/edhr/commercial-page-menu-contract.json"))
    pages = {page["key"]: page for page in contract["pages"]}

    for key in [
        "edhr-init-batch",
        "edhr-form-template",
        "edhr-form-instance",
        "edhr-traveler",
        "edhr-release",
        "edhr-report",
        "edhr-delivery",
    ]:
        assert key in pages, f"commercial page menu contract missing {key}"
        assert pages[key]["permission"] in EXPECTED_QUERY_PERMISSIONS


def test_first_wave_common_contract_is_available_for_later_slices() -> None:
    for relative in [
        "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProEdhrCommonStatus.java",
        "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProEdhrAuditEventContract.java",
        "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProEdhrIdempotencySupport.java",
    ]:
        text = read_text(relative)
        assert "PRECHECK_FAILED" in text or "idempotencyKey" in text or "sourceModule" in text
