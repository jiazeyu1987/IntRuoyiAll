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


def test_validation_controller_exposes_package_item_trace_endpoint_contracts() -> None:
    package_controller = read_java("controller/admin/pro/batchrecord/MesProEdhrValidationPackageController.java")
    item_controller = read_java("controller/admin/pro/batchrecord/MesProEdhrValidationRequirementItemController.java")
    trace_controller = read_java("controller/admin/pro/batchrecord/MesProEdhrValidationTraceLinkController.java")

    for fragment in [
        '@RequestMapping("/mes/pro/edhr-validation-package")',
        '@GetMapping("/page")',
        '@PostMapping("/create")',
        '@GetMapping("/detail")',
        '@PostMapping("/evaluate-trace")',
        "mes:pro-edhr-validation:query",
        "mes:pro-edhr-validation:create",
        "mes:pro-edhr-validation:evaluate-trace",
    ]:
        assert fragment in package_controller

    for fragment in [
        '@RequestMapping("/mes/pro/edhr-validation-requirement-item")',
        '@GetMapping("/page")',
        '@PostMapping("/create")',
        "mes:pro-edhr-validation:query",
        "mes:pro-edhr-validation:create",
    ]:
        assert fragment in item_controller

    for fragment in [
        '@RequestMapping("/mes/pro/edhr-validation-trace-link")',
        '@PostMapping("/create")',
        "mes:pro-edhr-validation:create",
    ]:
        assert fragment in trace_controller


def test_validation_service_declares_trace_gate_methods_without_default_success() -> None:
    service = read_java("service/pro/batchrecord/MesProEdhrValidationService.java")
    impl = read_java("service/pro/batchrecord/MesProEdhrValidationServiceImpl.java")

    for method_name in [
        "getPackagePage",
        "createPackage",
        "getPackageDetail",
        "getRequirementItemPage",
        "createRequirementItem",
        "createTraceLink",
        "evaluateTrace",
    ]:
        assert method_name in service
        assert method_name in impl

    for required_failure in [
        "PRO_EDHR_VALIDATION_PACKAGE_NOT_EXISTS",
        "PRO_EDHR_VALIDATION_PACKAGE_CREATE_FAILED",
        "PRO_EDHR_VALIDATION_ITEM_NOT_EXISTS",
        "PRO_EDHR_VALIDATION_ITEM_TYPE_INVALID",
        "PRO_EDHR_VALIDATION_TRACE_LINK_INVALID",
        "PRO_EDHR_VALIDATION_TRACE_GATE_BLOCKED",
    ]:
        assert required_failure in impl

    for required_rule in [
        "ITEM_TYPE_URS",
        "ITEM_TYPE_FRS",
        "ITEM_TYPE_RISK",
        "ITEM_TYPE_IQ",
        "ITEM_TYPE_OQ",
        "ITEM_TYPE_PQ",
        "LINK_TYPE_URS_FRS",
        "LINK_TYPE_URS_RISK",
        "LINK_TYPE_URS_VERIFICATION",
        "VALIDATION_STATUS_BLOCKED",
        "VALIDATION_STATUS_PREPARED",
    ]:
        assert required_rule in impl

    assert "catch (Exception ignored)" not in impl
    assert "return success" not in impl
    assert "Collections.emptyList()" not in impl
    assert "VALIDATION_STATUS_PASSED" not in impl


def test_validation_vo_contract_exposes_csv_trace_owner_action_and_gate_fields() -> None:
    for relative_path in [
        "controller/admin/pro/batchrecord/vo/MesProEdhrValidationPackageCreateReqVO.java",
        "controller/admin/pro/batchrecord/vo/MesProEdhrValidationPackagePageReqVO.java",
        "controller/admin/pro/batchrecord/vo/MesProEdhrValidationPackageRespVO.java",
        "controller/admin/pro/batchrecord/vo/MesProEdhrValidationRequirementItemCreateReqVO.java",
        "controller/admin/pro/batchrecord/vo/MesProEdhrValidationRequirementItemPageReqVO.java",
        "controller/admin/pro/batchrecord/vo/MesProEdhrValidationRequirementItemRespVO.java",
        "controller/admin/pro/batchrecord/vo/MesProEdhrValidationTraceLinkCreateReqVO.java",
        "controller/admin/pro/batchrecord/vo/MesProEdhrValidationTraceLinkRespVO.java",
        "controller/admin/pro/batchrecord/vo/MesProEdhrValidationTraceEvaluateRespVO.java",
        "controller/admin/pro/batchrecord/vo/MesProEdhrValidationTraceIssueRespVO.java",
    ]:
        read_java(relative_path)

    package = read_java("controller/admin/pro/batchrecord/vo/MesProEdhrValidationPackageRespVO.java")
    for field in [
        "packageCode",
        "customerProjectName",
        "releaseTag",
        "schemaVersion",
        "targetEnvironment",
        "validationStatus",
        "oqReady",
        "validationOwnerName",
        "qaOwnerName",
        "blockedReason",
        "traceSummaryJson",
    ]:
        assert field in package

    item = read_java("controller/admin/pro/batchrecord/vo/MesProEdhrValidationRequirementItemRespVO.java")
    for field in ["itemCode", "itemType", "itemVersion", "itemStatus", "ownerName", "signoffRole"]:
        assert field in item

    trace = read_java("controller/admin/pro/batchrecord/vo/MesProEdhrValidationTraceEvaluateRespVO.java")
    for field in [
        "packageId",
        "validationStatus",
        "oqReady",
        "traceStatus",
        "ursCount",
        "brokenTraceCount",
        "brokenItems",
        "nextAction",
    ]:
        assert field in trace


def test_validation_data_objects_expose_tenant_boundary() -> None:
    for relative_path in [
        "dal/dataobject/pro/batchrecord/MesProEdhrValidationPackageDO.java",
        "dal/dataobject/pro/batchrecord/MesProEdhrValidationRequirementItemDO.java",
        "dal/dataobject/pro/batchrecord/MesProEdhrValidationTraceLinkDO.java",
    ]:
        text = read_java(relative_path)
        assert "private Long tenantId;" in text
