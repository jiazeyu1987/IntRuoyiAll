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


def test_oq_pq_controller_exposes_case_run_step_deviation_endpoint_contracts() -> None:
    controller = read_java("controller/admin/pro/batchrecord/MesProEdhrOqPqController.java")

    for fragment in [
        '@RequestMapping("/mes/pro/edhr-oq-pq")',
        '@GetMapping("/case/page")',
        '@PostMapping("/case/create")',
        '@GetMapping("/run/page")',
        '@PostMapping("/run/create")',
        '@PostMapping("/run/submit-step")',
        '@PostMapping("/run/complete")',
        '@GetMapping("/deviation/page")',
        '@PostMapping("/deviation/remediate")',
        '@PostMapping("/deviation/retest")',
        '@PostMapping("/deviation/close")',
        "mes:pro-edhr-oq-pq:query",
        "mes:pro-edhr-oq-pq:create",
        "mes:pro-edhr-oq-pq:execute",
        "mes:pro-edhr-oq-pq:retest",
        "mes:pro-edhr-oq-pq:close",
    ]:
        assert fragment in controller


def test_oq_pq_service_declares_execution_and_deviation_rules_without_default_success() -> None:
    service = read_java("service/pro/batchrecord/MesProEdhrOqPqService.java")
    impl = read_java("service/pro/batchrecord/MesProEdhrOqPqServiceImpl.java")

    for method_name in [
        "getCasePage",
        "createCase",
        "getRunPage",
        "createRun",
        "submitStepResult",
        "completeRun",
        "getDeviationPage",
        "remediateDeviation",
        "retestDeviation",
        "closeDeviation",
    ]:
        assert method_name in service
        assert method_name in impl

    for required_failure in [
        "PRO_EDHR_OQ_PQ_PACKAGE_NOT_OQ_READY",
        "PRO_EDHR_OQ_PQ_CASE_NOT_EXISTS",
        "PRO_EDHR_OQ_PQ_RUN_NOT_EXISTS",
        "PRO_EDHR_OQ_PQ_RUN_EVIDENCE_MISSING",
        "PRO_EDHR_OQ_PQ_PQ_REAL_DATA_REQUIRED",
        "PRO_EDHR_OQ_PQ_DEVIATION_OPEN",
        "PRO_EDHR_OQ_PQ_DEVIATION_CLOSE_REQUIRED",
    ]:
        assert required_failure in impl

    for required_rule in [
        "CASE_TYPE_OQ",
        "CASE_TYPE_PQ",
        "RUN_STATUS_CREATED",
        "RUN_STATUS_RUNNING",
        "RUN_STATUS_DEVIATION_OPEN",
        "RUN_STATUS_PASSED",
        "RUN_STATUS_BLOCKED",
        "STEP_RESULT_PASS",
        "STEP_RESULT_FAIL",
        "DEVIATION_STATUS_OPEN",
        "DEVIATION_STATUS_REMEDIATED",
        "DEVIATION_STATUS_RETESTED",
        "DEVIATION_STATUS_CLOSED",
    ]:
        assert required_rule in impl

    assert "catch (Exception ignored)" not in impl
    assert "return success" not in impl
    assert "Collections.emptyList()" not in impl
    assert "RUN_STATUS_PASSED" in impl and "DEFAULT_SUCCESS" not in impl


def test_oq_pq_vo_contract_exposes_execution_real_data_and_deviation_fields() -> None:
    for relative_path in [
        "controller/admin/pro/batchrecord/vo/MesProEdhrOqPqCaseCreateReqVO.java",
        "controller/admin/pro/batchrecord/vo/MesProEdhrOqPqCasePageReqVO.java",
        "controller/admin/pro/batchrecord/vo/MesProEdhrOqPqCaseRespVO.java",
        "controller/admin/pro/batchrecord/vo/MesProEdhrOqPqRunCreateReqVO.java",
        "controller/admin/pro/batchrecord/vo/MesProEdhrOqPqRunPageReqVO.java",
        "controller/admin/pro/batchrecord/vo/MesProEdhrOqPqRunRespVO.java",
        "controller/admin/pro/batchrecord/vo/MesProEdhrOqPqStepSubmitReqVO.java",
        "controller/admin/pro/batchrecord/vo/MesProEdhrOqPqStepResultRespVO.java",
        "controller/admin/pro/batchrecord/vo/MesProEdhrOqPqDeviationPageReqVO.java",
        "controller/admin/pro/batchrecord/vo/MesProEdhrOqPqDeviationRespVO.java",
        "controller/admin/pro/batchrecord/vo/MesProEdhrOqPqDeviationRemediateReqVO.java",
        "controller/admin/pro/batchrecord/vo/MesProEdhrOqPqDeviationRetestReqVO.java",
        "controller/admin/pro/batchrecord/vo/MesProEdhrOqPqDeviationCloseReqVO.java",
    ]:
        read_java(relative_path)

    case_resp = read_java("controller/admin/pro/batchrecord/vo/MesProEdhrOqPqCaseRespVO.java")
    for field in [
        "packageId",
        "caseCode",
        "caseName",
        "caseType",
        "caseVersion",
        "stepNo",
        "stepTitle",
        "expectedResult",
        "evidenceRequirement",
        "ownerName",
        "reviewerName",
    ]:
        assert field in case_resp

    run_resp = read_java("controller/admin/pro/batchrecord/vo/MesProEdhrOqPqRunRespVO.java")
    for field in [
        "runCode",
        "runStatus",
        "executionEnvironment",
        "releaseTag",
        "schemaVersion",
        "executorName",
        "reviewerName",
        "realBusinessPath",
        "realTestDataSource",
        "targetEnvironmentProof",
        "attachmentEvidence",
        "evidenceChecksum",
        "openDeviationCount",
        "blockedReason",
    ]:
        assert field in run_resp

    deviation_resp = read_java("controller/admin/pro/batchrecord/vo/MesProEdhrOqPqDeviationRespVO.java")
    for field in [
        "deviationCode",
        "deviationStatus",
        "rootCause",
        "remediationAction",
        "retestResult",
        "retestEvidence",
        "retestReviewerName",
        "closeSignoffName",
        "closedAt",
        "nextAction",
    ]:
        assert field in deviation_resp


def test_oq_pq_data_objects_expose_tenant_boundary() -> None:
    for relative_path in [
        "dal/dataobject/pro/batchrecord/MesProEdhrValidationCaseDO.java",
        "dal/dataobject/pro/batchrecord/MesProEdhrValidationRunDO.java",
        "dal/dataobject/pro/batchrecord/MesProEdhrValidationStepResultDO.java",
        "dal/dataobject/pro/batchrecord/MesProEdhrValidationDeviationDO.java",
    ]:
        text = read_java(relative_path)
        assert "private Long tenantId;" in text
