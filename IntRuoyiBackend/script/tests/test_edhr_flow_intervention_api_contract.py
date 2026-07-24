from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]


def read_text(relative_path: str) -> str:
    path = ROOT / relative_path
    assert path.exists(), f"缺少文件：{relative_path}"
    return path.read_text(encoding="utf-8")


def test_flow_intervention_controller_exposes_separate_actions() -> None:
    controller = read_text(
        "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/batchrecord/"
        "MesProEdhrFlowInterventionController.java"
    )

    for fragment in [
        '@RequestMapping("/mes/pro/edhr-flow-intervention")',
        '@GetMapping("/page")',
        '@GetMapping("/event/page")',
        '@PostMapping("/return")',
        '@PostMapping("/withdraw")',
        '@PostMapping("/transfer")',
        '@PostMapping("/add-sign")',
        '@PostMapping("/admin-intervene")',
        "mes:pro-edhr-flow-intervention:query",
        "mes:pro-edhr-flow-intervention:event-query",
        "mes:pro-edhr-flow-intervention:return",
        "mes:pro-edhr-flow-intervention:withdraw",
        "mes:pro-edhr-flow-intervention:transfer",
        "mes:pro-edhr-flow-intervention:add-sign",
        "mes:pro-edhr-flow-intervention:admin-intervene",
        "MesProEdhrFlowInterventionReturnReqVO",
        "MesProEdhrFlowInterventionWithdrawReqVO",
        "MesProEdhrFlowInterventionTransferReqVO",
        "MesProEdhrFlowInterventionAddSignReqVO",
        "MesProEdhrFlowInterventionAdminReqVO",
        "returnBack(",
        "withdraw(",
        "transfer(",
        "addSign(",
        "adminIntervene(",
        "getEventPage(",
    ]:
        assert fragment in controller


def test_flow_intervention_service_guards_reason_signoff_authorization_and_idempotency() -> None:
    service_impl = read_text(
        "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/"
        "MesProEdhrFlowInterventionServiceImpl.java"
    )

    for fragment in [
        "ACTION_RETURN",
        "ACTION_WITHDRAW",
        "ACTION_TRANSFER",
        "ACTION_ADD_SIGN",
        "ACTION_ADMIN_INTERVENE",
        "STATUS_RECORDED",
        "EVENT_TYPE_FLOW_INTERVENTION",
        "requireReason",
        "requireSignoffEvidence",
        "requireAuthorizationBasis",
        "requireIdempotencyKey",
        "rejectBackendMutationPath",
        "recordFlowEvent",
        "runIntegrityRecheck",
        "integrityCheckResult",
        "authorizationBasis",
        "signoffEvidenceHash",
        "idempotencyKey",
        "selectByBusinessObjectTypeAndBusinessObjectIdAndInterventionActionAndIdempotencyKey",
        "PRO_EDHR_FLOW_INTERVENTION_REASON_REQUIRED",
        "PRO_EDHR_FLOW_INTERVENTION_SIGNOFF_REQUIRED",
        "PRO_EDHR_FLOW_INTERVENTION_AUTHORIZATION_REQUIRED",
        "PRO_EDHR_FLOW_INTERVENTION_IDEMPOTENCY_KEY_REQUIRED",
        "PRO_EDHR_FLOW_INTERVENTION_BACKEND_MUTATION_FORBIDDEN",
        "PRO_EDHR_FLOW_INTERVENTION_ACTION_INVALID",
    ]:
        assert fragment in service_impl

    for forbidden in [
        "catch (Exception",
        "catch (RuntimeException",
        "DEFAULT_SUCCESS",
        "MOCK_SIGNOFF",
        "DIRECT_STATUS_UPDATE",
        "return true;",
        'setInterventionStatus("SUCCESS")',
    ]:
        assert forbidden not in service_impl


def test_flow_intervention_persistence_and_vo_contract_exists() -> None:
    for relative_path in [
        "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/dal/dataobject/pro/batchrecord/"
        "MesProEdhrFlowEventDO.java",
        "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/dal/dataobject/pro/batchrecord/"
        "MesProEdhrFlowInterventionDO.java",
        "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/dal/mysql/pro/batchrecord/"
        "MesProEdhrFlowEventMapper.java",
        "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/dal/mysql/pro/batchrecord/"
        "MesProEdhrFlowInterventionMapper.java",
        "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/batchrecord/vo/"
        "MesProEdhrFlowInterventionRespVO.java",
        "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/batchrecord/vo/"
        "MesProEdhrFlowEventRespVO.java",
    ]:
        assert (ROOT / relative_path).exists(), f"缺少流程干预对象：{relative_path}"

    mapper = read_text(
        "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/dal/mysql/pro/batchrecord/"
        "MesProEdhrFlowInterventionMapper.java"
    )
    assert "selectByBusinessObjectTypeAndBusinessObjectIdAndInterventionActionAndIdempotencyKey" in mapper
    assert "selectPage" in mapper


def test_flow_intervention_java_contract_test_exists() -> None:
    java_test = read_text(
        "yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/controller/admin/pro/batchrecord/"
        "MesProEdhrFlowInterventionContractTest.java"
    )

    for fragment in [
        "returnShouldRequireReturnPermission",
        "withdrawShouldRequireWithdrawPermission",
        "transferShouldRequireTransferPermission",
        "addSignShouldRequireAddSignPermission",
        "adminInterveneShouldRequireAdminPermission",
        "eventPageShouldRequireEventQueryPermission",
        "MesProEdhrFlowInterventionController",
        "/mes/pro/edhr-flow-intervention",
    ]:
        assert fragment in java_test
