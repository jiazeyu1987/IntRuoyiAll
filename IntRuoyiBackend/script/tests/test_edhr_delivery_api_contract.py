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


def test_delivery_controller_exposes_first_slice_endpoint_contracts() -> None:
    controller = read_java("controller/admin/pro/batchrecord/MesProEdhrDeliveryCockpitController.java")

    for fragment in [
        '@RequestMapping("/mes/pro/edhr-delivery-cockpit")',
        '@GetMapping("/project/page")',
        '@PostMapping("/project/create")',
        '@GetMapping("/project/detail")',
        '@GetMapping("/evidence-package/page")',
        '@GetMapping("/gate-summary")',
        "mes:pro-edhr-delivery:query",
        "mes:pro-edhr-delivery:create",
    ]:
        assert fragment in controller


def test_delivery_service_declares_gate_and_evidence_methods_without_default_success() -> None:
    service = read_java("service/pro/batchrecord/MesProEdhrDeliveryService.java")
    impl = read_java("service/pro/batchrecord/MesProEdhrDeliveryServiceImpl.java")

    for method_name in [
        "getProjectPage",
        "createProject",
        "getProjectDetail",
        "getEvidencePackagePage",
        "getGateSummary",
    ]:
        assert method_name in service
        assert method_name in impl

    for required_failure in [
        "PRO_EDHR_DELIVERY_PROJECT_NOT_EXISTS",
        "PRO_EDHR_DELIVERY_PROJECT_CREATE_FAILED",
        "PRO_EDHR_DELIVERY_GATE_ITEM_MISSING",
    ]:
        assert required_failure in impl

    assert "catch (Exception ignored)" not in impl
    assert "return success" not in impl
    assert "Collections.emptyList()" not in impl


def test_delivery_vo_contract_exposes_missing_evidence_owner_action_and_signoff_fields() -> None:
    for relative_path in [
        "controller/admin/pro/batchrecord/vo/MesProEdhrDeliveryProjectCreateReqVO.java",
        "controller/admin/pro/batchrecord/vo/MesProEdhrDeliveryProjectPageReqVO.java",
        "controller/admin/pro/batchrecord/vo/MesProEdhrDeliveryProjectRespVO.java",
        "controller/admin/pro/batchrecord/vo/MesProEdhrEvidencePackagePageReqVO.java",
        "controller/admin/pro/batchrecord/vo/MesProEdhrEvidencePackageRespVO.java",
        "controller/admin/pro/batchrecord/vo/MesProEdhrDeliveryGateSummaryRespVO.java",
        "controller/admin/pro/batchrecord/vo/MesProEdhrDeliveryGateItemRespVO.java",
    ]:
        read_java(relative_path)

    project = read_java("controller/admin/pro/batchrecord/vo/MesProEdhrDeliveryProjectRespVO.java")
    for field in [
        "projectCode",
        "projectStatus",
        "releaseTag",
        "schemaVersion",
        "signoffAllowed",
        "gateSummaryJson",
    ]:
        assert field in project

    package = read_java("controller/admin/pro/batchrecord/vo/MesProEdhrEvidencePackageRespVO.java")
    for field in [
        "packageCode",
        "packageStatus",
        "evidenceStatus",
        "missingEvidenceJson",
        "ownerName",
        "nextAction",
        "signoffImpact",
    ]:
        assert field in package

    gate = read_java("controller/admin/pro/batchrecord/vo/MesProEdhrDeliveryGateItemRespVO.java")
    for field in ["gateCode", "gateStatus", "missingEvidence", "ownerName", "nextAction", "signoffImpact"]:
        assert field in gate


def test_delivery_data_objects_expose_tenant_boundary() -> None:
    for relative_path in [
        "dal/dataobject/pro/batchrecord/MesProEdhrDeliveryProjectDO.java",
        "dal/dataobject/pro/batchrecord/MesProEdhrEvidencePackageDO.java",
        "dal/dataobject/pro/batchrecord/MesProEdhrDeliveryGateItemDO.java",
    ]:
        text = read_java(relative_path)
        assert "private Long tenantId;" in text
