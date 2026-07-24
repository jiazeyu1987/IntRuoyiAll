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


def test_deployment_controller_exposes_deployment_license_interface_contracts() -> None:
    controller = read_java("controller/admin/pro/batchrecord/MesProEdhrDeploymentController.java")

    for fragment in [
        '@RequestMapping("/mes/pro/edhr-deployment")',
        '@GetMapping("/page")',
        '@PostMapping("/create")',
        '@GetMapping("/detail")',
        '@PostMapping("/update-evidence")',
        '@PostMapping("/precheck")',
        "mes:pro-edhr-deployment:query",
        "mes:pro-edhr-deployment:create",
        "mes:pro-edhr-deployment:update",
        "mes:pro-edhr-deployment:precheck",
    ]:
        assert fragment in controller


def test_deployment_service_declares_gate_rules_without_default_success() -> None:
    service = read_java("service/pro/batchrecord/MesProEdhrDeploymentService.java")
    impl = read_java("service/pro/batchrecord/MesProEdhrDeploymentServiceImpl.java")

    for method_name in [
        "getPage",
        "createEvidence",
        "getDetail",
        "updateEvidence",
        "precheckEvidence",
    ]:
        assert method_name in service
        assert method_name in impl

    for required_failure in [
        "PRO_EDHR_DEPLOYMENT_PROJECT_NOT_EXISTS",
        "PRO_EDHR_DEPLOYMENT_EVIDENCE_NOT_EXISTS",
        "PRO_EDHR_DEPLOYMENT_MANIFEST_REQUIRED",
        "PRO_EDHR_DEPLOYMENT_LICENSE_REQUIRED",
        "PRO_EDHR_DEPLOYMENT_INTERFACE_RESPONSE_REQUIRED",
        "PRO_EDHR_DEPLOYMENT_VERSION_INCONSISTENT",
    ]:
        assert required_failure in impl

    for required_rule in [
        "STATUS_DELIVERY_DRAFT",
        "STATUS_ENVIRONMENT_CHECKED",
        "STATUS_INSTALLED",
        "STATUS_INTEGRATED",
        "STATUS_DELIVERY_BLOCKED",
        "GATE_ENVIRONMENT_AUTHORIZED",
        "GATE_RELEASE_MANIFEST",
        "GATE_SCHEMA_REQUIRED_SQL",
        "GATE_LICENSE_VALID",
        "GATE_INTERFACE_RESPONSE",
    ]:
        assert required_rule in impl

    assert "catch (Exception ignored)" not in impl
    assert "return success" not in impl
    assert "Collections.emptyList()" not in impl
    assert "DEFAULT_SUCCESS" not in impl


def test_deployment_vo_contract_exposes_environment_version_license_and_interface_fields() -> None:
    for relative_path in [
        "controller/admin/pro/batchrecord/vo/MesProEdhrDeploymentCreateReqVO.java",
        "controller/admin/pro/batchrecord/vo/MesProEdhrDeploymentPageReqVO.java",
        "controller/admin/pro/batchrecord/vo/MesProEdhrDeploymentUpdateReqVO.java",
        "controller/admin/pro/batchrecord/vo/MesProEdhrDeploymentRespVO.java",
        "controller/admin/pro/batchrecord/vo/MesProEdhrDeploymentGateItemRespVO.java",
        "controller/admin/pro/batchrecord/vo/MesProEdhrDeploymentPrecheckRespVO.java",
    ]:
        read_java(relative_path)

    create_req = read_java("controller/admin/pro/batchrecord/vo/MesProEdhrDeploymentCreateReqVO.java")
    for field in [
        "projectId",
        "deploymentName",
        "customerProjectName",
        "targetEnvironment",
        "environmentAuthorized",
        "environmentCheckSummary",
        "serverSummary",
        "networkSummary",
        "objectStorageSummary",
        "capacitySummary",
        "permissionSummary",
        "releaseTag",
        "artifactVersion",
        "artifactChecksum",
        "schemaVersion",
        "migrationManifest",
        "requiredSqlManifest",
        "appImportResult",
    ]:
        assert field in create_req

    update_req = read_java("controller/admin/pro/batchrecord/vo/MesProEdhrDeploymentUpdateReqVO.java")
    for field in [
        "deploymentId",
        "targetEnvironment",
        "environmentAuthorized",
        "environmentCheckSummary",
        "serverSummary",
        "networkSummary",
        "objectStorageSummary",
        "capacitySummary",
        "permissionSummary",
        "releaseTag",
        "artifactVersion",
        "artifactChecksum",
        "schemaVersion",
        "migrationManifest",
        "requiredSqlManifest",
        "appImportResult",
        "licenseScope",
        "licenseValidUntil",
        "licenseFileEvidence",
        "licenseCheckResult",
        "customerLicenseConfirmation",
        "interfaceScope",
        "interfaceVersion",
        "integrationEnvironment",
        "requestEvidence",
        "responseEvidence",
        "interfaceFailureCount",
        "remediationAction",
        "retestEvidence",
        "interfaceConfirmedBy",
    ]:
        assert field in update_req

    resp = read_java("controller/admin/pro/batchrecord/vo/MesProEdhrDeploymentRespVO.java")
    for field in [
        "deploymentCode",
        "deploymentStatus",
        "gatePassed",
        "gateCheckedAt",
        "blockedReason",
        "nextAction",
        "evidenceSnapshotChecksum",
        "gateItems",
    ]:
        assert field in resp

    gate_resp = read_java("controller/admin/pro/batchrecord/vo/MesProEdhrDeploymentGateItemRespVO.java")
    for field in [
        "gateCode",
        "gateName",
        "gateStatus",
        "evidenceSource",
        "missingEvidence",
        "ownerName",
        "nextAction",
        "signoffImpact",
    ]:
        assert field in gate_resp


def test_deployment_data_objects_expose_tenant_boundary() -> None:
    for relative_path in [
        "dal/dataobject/pro/batchrecord/MesProEdhrDeploymentEvidenceDO.java",
        "dal/dataobject/pro/batchrecord/MesProEdhrDeploymentGateItemDO.java",
    ]:
        text = read_java(relative_path)
        assert "private Long tenantId;" in text
