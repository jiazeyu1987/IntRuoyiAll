from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]


def read_text(relative_path: str) -> str:
    path = ROOT / relative_path
    assert path.exists(), f"缺少文件：{relative_path}"
    return path.read_text(encoding="utf-8")


def test_release_controller_exposes_transaction_lifecycle_actions() -> None:
    controller = read_text(
        "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/batchrecord/"
        "MesProEdhrReleaseController.java"
    )

    for fragment in [
        '@PostMapping("/submit")',
        '@PostMapping("/approve")',
        '@PostMapping("/reject")',
        '@PostMapping("/withdraw")',
        '@GetMapping("/event/page")',
        "mes:pro-edhr-release:submit",
        "mes:pro-edhr-release:approve",
        "mes:pro-edhr-release:reject",
        "mes:pro-edhr-release:withdraw",
        "mes:pro-edhr-release:event-query",
        "MesProEdhrReleaseSubmitReqVO",
        "MesProEdhrReleaseApproveReqVO",
        "MesProEdhrReleaseRejectReqVO",
        "MesProEdhrReleaseWithdrawReqVO",
        "MesProEdhrReleaseEventPageReqVO",
        "submit(",
        "approve(",
        "reject(",
        "withdraw(",
        "getEventPage(",
    ]:
        assert fragment in controller


def test_release_service_guards_precheck_signature_reason_and_idempotency() -> None:
    service_impl = read_text(
        "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/"
        "MesProEdhrReleaseServiceImpl.java"
    )

    for fragment in [
        "STATUS_PENDING_APPROVAL",
        "STATUS_RELEASED",
        "STATUS_REJECTED",
        "STATUS_WITHDRAWN",
        "EVENT_TYPE_SUBMIT",
        "EVENT_TYPE_APPROVE",
        "EVENT_TYPE_REJECT",
        "EVENT_TYPE_WITHDRAW",
        "submit(",
        "approve(",
        "reject(",
        "withdraw(",
        "requirePrecheckPassed",
        "requirePendingApproval",
        "requireReason",
        "requireSignoffEvidence",
        "requireIdempotencyKey",
        "recordTransactionEvent",
        "failedCheckCount",
        "blockingCheckCount",
        "idempotencyKey",
        "signoffEvidenceHash",
        "selectByReleaseTransactionIdAndEventTypeAndIdempotencyKey",
        "PRO_EDHR_RELEASE_PRECHECK_REQUIRED",
        "PRO_EDHR_RELEASE_STATUS_INVALID",
        "PRO_EDHR_RELEASE_REASON_REQUIRED",
        "PRO_EDHR_RELEASE_SIGNOFF_REQUIRED",
        "PRO_EDHR_RELEASE_IDEMPOTENCY_KEY_REQUIRED",
    ]:
        assert fragment in service_impl

    for forbidden in [
        "catch (Exception",
        "catch (RuntimeException",
        "DEFAULT_SUCCESS",
        "MOCK_SIGNOFF",
        "return true;",
        "setReleaseStatus(STATUS_RELEASED);",
    ]:
        assert forbidden not in service_impl


def test_release_transaction_lifecycle_persistence_contract_exists() -> None:
    for relative_path in [
        "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/dal/dataobject/pro/batchrecord/"
        "MesProEdhrReleaseTransactionEventDO.java",
        "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/dal/mysql/pro/batchrecord/"
        "MesProEdhrReleaseTransactionEventMapper.java",
        "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/batchrecord/vo/"
        "MesProEdhrReleaseSubmitReqVO.java",
        "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/batchrecord/vo/"
        "MesProEdhrReleaseApproveReqVO.java",
        "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/batchrecord/vo/"
        "MesProEdhrReleaseRejectReqVO.java",
        "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/batchrecord/vo/"
        "MesProEdhrReleaseWithdrawReqVO.java",
        "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/batchrecord/vo/"
        "MesProEdhrReleaseEventRespVO.java",
    ]:
        assert (ROOT / relative_path).exists(), f"缺少放行事务生命周期对象：{relative_path}"

    event_mapper = read_text(
        "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/dal/mysql/pro/batchrecord/"
        "MesProEdhrReleaseTransactionEventMapper.java"
    )
    assert "selectByReleaseTransactionIdAndEventTypeAndIdempotencyKey" in event_mapper
    assert "selectPage" in event_mapper


def test_release_transaction_lifecycle_java_contract_test_exists() -> None:
    java_test = read_text(
        "yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/controller/admin/pro/batchrecord/"
        "MesProEdhrReleaseTransactionLifecycleContractTest.java"
    )

    for fragment in [
        "submitShouldRequireSubmitPermission",
        "approveShouldRequireApprovePermission",
        "rejectShouldRequireRejectPermission",
        "withdrawShouldRequireWithdrawPermission",
        "eventPageShouldRequireEventQueryPermission",
        "MesProEdhrReleaseController",
        "/mes/pro/edhr-release",
    ]:
        assert fragment in java_test
