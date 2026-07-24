from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]


def read_text(relative_path: str) -> str:
    path = REPO_ROOT / relative_path
    assert path.exists(), f"{relative_path} must exist"
    return path.read_text(encoding="utf-8")


def test_edhr_release_precheck_backend_files_exist() -> None:
    for relative_path in [
        "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/batchrecord/MesProEdhrReleaseController.java",
        "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProEdhrReleaseService.java",
        "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProEdhrReleaseServiceImpl.java",
        "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/dal/mysql/pro/batchrecord/MesProEdhrReleaseTransactionMapper.java",
        "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/dal/mysql/pro/batchrecord/MesProEdhrReleaseCheckItemMapper.java",
        "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/dal/dataobject/pro/batchrecord/MesProEdhrReleaseTransactionDO.java",
        "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/dal/dataobject/pro/batchrecord/MesProEdhrReleaseCheckItemDO.java",
        "yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/MesProEdhrReleasePrecheckContractTest.java",
    ]:
        read_text(relative_path)


def test_edhr_release_controller_keeps_precheck_contract_after_lifecycle_slice() -> None:
    source = read_text(
        "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/batchrecord/MesProEdhrReleaseController.java"
    )

    for fragment in [
        '@RequestMapping("/mes/pro/edhr-release")',
        '@GetMapping("/page")',
        '@GetMapping("/get")',
        '@PostMapping("/precheck")',
        '@GetMapping("/check-item/page")',
        "mes:pro-edhr-release:query",
        "mes:pro-edhr-release:precheck",
    ]:
        assert fragment in source

    assert "/intervene" not in source


def test_edhr_release_service_contract_is_fail_fast_structured_and_idempotent() -> None:
    source = read_text(
        "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProEdhrReleaseServiceImpl.java"
    )

    for fragment in [
        "STATUS_PRECHECK_REQUIRED",
        "STATUS_PRECHECK_FAILED",
        "STATUS_PRECHECK_PASSED",
        "ITEM_STATUS_OPEN",
        "ITEM_STATUS_SUPERSEDED",
        "CHECK_RESULT_PASS",
        "CHECK_RESULT_FAIL",
        "CHECK_RESULT_BLOCKER",
        "CHECK_DHR_COMPLETENESS",
        "CHECK_INSPECTION_RESULT",
        "CHECK_DEVIATION_CLOSED",
        "CHECK_REWORK_CLOSED",
        "CHECK_SCRAP_RECORDED",
        "CHECK_INVENTORY_CONSISTENCY",
        "closeOpenByReleaseTransactionId",
        "batchExecutionId",
        "precheckSnapshotJson",
    ]:
        assert fragment in source

    for forbidden in [
        "submitRelease",
        "approveRelease",
        "intervene",
        "return true;",
        "catch (",
        "catch{",
    ]:
        assert forbidden not in source


def test_edhr_release_do_and_vo_declare_traceability_fields() -> None:
    transaction_do = read_text(
        "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/dal/dataobject/pro/batchrecord/MesProEdhrReleaseTransactionDO.java"
    )
    item_do = read_text(
        "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/dal/dataobject/pro/batchrecord/MesProEdhrReleaseCheckItemDO.java"
    )
    resp_vo = read_text(
        "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/batchrecord/vo/MesProEdhrReleaseRespVO.java"
    )

    for fragment in [
        '@TableName("mes_pro_edhr_release_transaction")',
        "batchExecutionId",
        "batchExecutionCode",
        "workOrderCode",
        "batchCode",
        "productCode",
        "dhrStatus",
        "inspectionStatus",
        "deviationStatus",
        "inventoryStatus",
        "releaseStatus",
        "blockingCheckCount",
    ]:
        assert fragment in transaction_do

    for fragment in [
        '@TableName("mes_pro_edhr_release_check_item")',
        "checkCode",
        "checkCategory",
        "checkResult",
        "itemStatus",
        "responsibilityModule",
        "sourceObjectType",
        "sourceObjectCode",
        "failureReason",
        "remediationSuggestion",
        "impactScopeJson",
        "evidenceHash",
    ]:
        assert fragment in item_do

    for fragment in [
        "batchExecutionId",
        "releaseTransactionId",
        "batchExecutionCode",
        "precheckSummary",
        "blockingCheckCount",
    ]:
        assert fragment in resp_vo
