from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]


def read_text(relative_path: str) -> str:
    path = ROOT / relative_path
    assert path.exists(), f"缺少文件：{relative_path}"
    return path.read_text(encoding="utf-8")


def test_unified_change_controller_exposes_controlled_actions() -> None:
    controller = read_text(
        "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/batchrecord/"
        "MesProEdhrUnifiedChangeController.java"
    )

    for fragment in [
        '@RequestMapping("/mes/pro/edhr-change/unified")',
        '@GetMapping("/page")',
        '@GetMapping("/impact/page")',
        '@GetMapping("/event/page")',
        '@PostMapping("/create")',
        '@PostMapping("/submit")',
        '@PostMapping("/recalculate-impact")',
        '@PostMapping("/approve")',
        '@PostMapping("/effect")',
        "mes:pro-edhr-change:unified-query",
        "mes:pro-edhr-change:impact-query",
        "mes:pro-edhr-change:event-query",
        "mes:pro-edhr-change:unified-create",
        "mes:pro-edhr-change:unified-submit",
        "mes:pro-edhr-change:unified-approve",
        "mes:pro-edhr-change:unified-effect",
        "MesProEdhrUnifiedChangeCreateReqVO",
        "MesProEdhrUnifiedChangeSubmitReqVO",
        "MesProEdhrUnifiedChangeRecalculateImpactReqVO",
        "MesProEdhrUnifiedChangeApproveReqVO",
        "MesProEdhrUnifiedChangeEffectReqVO",
        "create(",
        "submit(",
        "recalculateImpact(",
        "approve(",
        "requestEffect(",
    ]:
        assert fragment in controller


def test_unified_change_service_guards_change_impact_and_effect_paths() -> None:
    service_impl = read_text(
        "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/"
        "MesProEdhrUnifiedChangeServiceImpl.java"
    )

    for fragment in [
        "OBJECT_TYPE_FORM_TEMPLATE",
        "OBJECT_TYPE_DHR_TEMPLATE",
        "OBJECT_TYPE_RECORDBOOK_TEMPLATE",
        "STATUS_DRAFT",
        "STATUS_SUBMITTED",
        "STATUS_APPROVED",
        "STATUS_EFFECT_BLOCKED",
        "EVENT_TYPE_CREATE",
        "EVENT_TYPE_SUBMIT",
        "EVENT_TYPE_IMPACT_RECALCULATE",
        "EVENT_TYPE_APPROVE",
        "EVENT_TYPE_EFFECT_REQUEST_BLOCKED",
        "requireObjectType",
        "requireReason",
        "requireDiffSnapshot",
        "requireImpactScope",
        "requireSignoffEvidence",
        "requireIdempotencyKey",
        "rejectOverwriteCurrentVersion",
        "recalculateImpactScope",
        "recordUnifiedChangeEvent",
        "impactRecalculationHash",
        "diffSnapshotJson",
        "impactSummaryJson",
        "selectByControlledObjectTypeAndControlledObjectIdAndChangeTypeAndIdempotencyKey",
        "PRO_EDHR_UNIFIED_CHANGE_OBJECT_TYPE_INVALID",
        "PRO_EDHR_UNIFIED_CHANGE_REASON_REQUIRED",
        "PRO_EDHR_UNIFIED_CHANGE_DIFF_REQUIRED",
        "PRO_EDHR_UNIFIED_CHANGE_IMPACT_REQUIRED",
        "PRO_EDHR_UNIFIED_CHANGE_SIGNOFF_REQUIRED",
        "PRO_EDHR_UNIFIED_CHANGE_IDEMPOTENCY_KEY_REQUIRED",
        "PRO_EDHR_UNIFIED_CHANGE_OVERWRITE_FORBIDDEN",
        "PRO_EDHR_UNIFIED_CHANGE_STATUS_INVALID",
    ]:
        assert fragment in service_impl

    for forbidden in [
        "catch (Exception",
        "catch (RuntimeException",
        "DEFAULT_SUCCESS",
        "MOCK_SIGNOFF",
        "DIRECT_STATUS_UPDATE",
        "OVERWRITE_CURRENT_VERSION_SUCCESS",
        "FORCE_EFFECT_SUCCESS",
        "return true;",
        'setChangeStatus("EFFECTIVE")',
    ]:
        assert forbidden not in service_impl


def test_unified_change_persistence_and_vo_contract_exists() -> None:
    for relative_path in [
        "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/dal/dataobject/pro/batchrecord/"
        "MesProEdhrUnifiedChangeRequestDO.java",
        "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/dal/dataobject/pro/batchrecord/"
        "MesProEdhrUnifiedChangeImpactDO.java",
        "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/dal/dataobject/pro/batchrecord/"
        "MesProEdhrUnifiedChangeEventDO.java",
        "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/dal/mysql/pro/batchrecord/"
        "MesProEdhrUnifiedChangeRequestMapper.java",
        "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/dal/mysql/pro/batchrecord/"
        "MesProEdhrUnifiedChangeImpactMapper.java",
        "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/dal/mysql/pro/batchrecord/"
        "MesProEdhrUnifiedChangeEventMapper.java",
        "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/batchrecord/vo/"
        "MesProEdhrUnifiedChangeRespVO.java",
        "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/batchrecord/vo/"
        "MesProEdhrUnifiedChangeImpactRespVO.java",
        "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/batchrecord/vo/"
        "MesProEdhrUnifiedChangeEventRespVO.java",
    ]:
        assert (ROOT / relative_path).exists(), f"缺少统一变更对象：{relative_path}"

    mapper = read_text(
        "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/dal/mysql/pro/batchrecord/"
        "MesProEdhrUnifiedChangeRequestMapper.java"
    )
    assert "selectByControlledObjectTypeAndControlledObjectIdAndChangeTypeAndIdempotencyKey" in mapper
    assert "selectPage" in mapper


def test_unified_change_java_contract_test_exists() -> None:
    java_test = read_text(
        "yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/controller/admin/pro/batchrecord/"
        "MesProEdhrUnifiedChangeContractTest.java"
    )

    for fragment in [
        "pageShouldRequireUnifiedQueryPermission",
        "createShouldRequireUnifiedCreatePermission",
        "submitShouldRequireUnifiedSubmitPermission",
        "approveShouldRequireUnifiedApprovePermission",
        "effectShouldRequireUnifiedEffectPermission",
        "impactAndEventPagesShouldRequireExplicitPermissions",
        "MesProEdhrUnifiedChangeController",
        "/mes/pro/edhr-change/unified",
    ]:
        assert fragment in java_test
