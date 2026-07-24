from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]
JAVA_ROOT = (
    REPO_ROOT
    / "yudao-module-mes"
    / "src"
    / "main"
    / "java"
    / "cn"
    / "iocoder"
    / "yudao"
    / "module"
    / "mes"
)


def read_java(relative_path: str) -> str:
    path = JAVA_ROOT / relative_path
    assert path.exists(), f"Missing Java contract file: {relative_path}"
    return path.read_text(encoding="utf-8")


def test_report_controllers_expose_required_endpoint_contracts() -> None:
    catalog = read_java("controller/admin/pro/batchrecord/MesProEdhrReportCatalogController.java")
    definition = read_java("controller/admin/pro/batchrecord/MesProEdhrReportDefinitionController.java")
    query = read_java("controller/admin/pro/batchrecord/MesProEdhrReportQueryController.java")

    assert '@RequestMapping("/mes/pro/edhr-report-catalog")' in catalog
    assert '@GetMapping("/page")' in catalog
    assert '@GetMapping("/detail")' in catalog
    assert "mes:pro-edhr-report:query" in catalog

    assert '@RequestMapping("/mes/pro/edhr-report-definition")' in definition
    assert '@GetMapping("/page")' in definition
    assert '@GetMapping("/detail")' in definition
    assert "mes:pro-edhr-report:query" in definition

    assert '@RequestMapping("/mes/pro/edhr-report-query")' in query
    assert '@PostMapping("/run")' in query
    assert '@PostMapping("/export-audit")' in query
    assert '@GetMapping("/export-audit/page")' in query
    assert "mes:pro-edhr-report:query" in query
    assert "mes:pro-edhr-report:export" in query


def test_report_service_contract_declares_no_default_success_paths() -> None:
    service = read_java("service/pro/batchrecord/MesProEdhrReportService.java")
    impl = read_java("service/pro/batchrecord/MesProEdhrReportServiceImpl.java")

    for method_name in [
        "getCatalogPage",
        "getCatalogDetail",
        "getDefinitionPage",
        "getDefinitionDetail",
        "runReportQuery",
        "recordExportAudit",
        "getExportAuditPage",
    ]:
        assert method_name in service
        assert method_name in impl

    for required_failure in [
        "PRO_EDHR_REPORT_NOT_PUBLISHED",
        "PRO_EDHR_REPORT_DATA_SOURCE_INVALID",
        "PRO_EDHR_REPORT_CALIBER_MISSING",
    ]:
        assert required_failure in impl

    assert "catch (Exception ignored)" not in impl
    assert "return success" not in impl
    assert "Collections.emptyList()" not in impl


def test_report_vo_contract_exposes_caliber_filter_permission_and_audit_fields() -> None:
    for relative_path in [
        "controller/admin/pro/batchrecord/vo/MesProEdhrReportCatalogRespVO.java",
        "controller/admin/pro/batchrecord/vo/MesProEdhrReportDefinitionRespVO.java",
        "controller/admin/pro/batchrecord/vo/MesProEdhrReportQueryReqVO.java",
        "controller/admin/pro/batchrecord/vo/MesProEdhrReportQueryRespVO.java",
        "controller/admin/pro/batchrecord/vo/MesProEdhrReportExportAuditReqVO.java",
        "controller/admin/pro/batchrecord/vo/MesProEdhrReportExportAuditRespVO.java",
    ]:
        read_java(relative_path)

    resp = read_java("controller/admin/pro/batchrecord/vo/MesProEdhrReportQueryRespVO.java")
    for field in [
        "caliberVersion",
        "dataUpdatedAt",
        "filterSnapshotJson",
        "permissionSummaryJson",
        "dataSourceSummary",
        "rows",
    ]:
        assert field in resp

    audit_req = read_java("controller/admin/pro/batchrecord/vo/MesProEdhrReportExportAuditReqVO.java")
    for field in ["reportCode", "filterSnapshotJson", "permissionSummaryJson", "dataRangeSummary"]:
        assert field in audit_req


def test_report_data_objects_expose_tenant_boundary() -> None:
    for relative_path in [
        "dal/dataobject/pro/batchrecord/MesProEdhrDatasetDefinitionDO.java",
        "dal/dataobject/pro/batchrecord/MesProEdhrReportCatalogDO.java",
        "dal/dataobject/pro/batchrecord/MesProEdhrReportDefinitionDO.java",
        "dal/dataobject/pro/batchrecord/MesProEdhrExportAuditDO.java",
    ]:
        text = read_java(relative_path)
        assert "private Long tenantId;" in text
