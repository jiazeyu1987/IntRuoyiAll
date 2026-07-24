from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
DOMAIN_CONTRACT_DOC = ROOT / "docs/system/mes-scheduling-domain-contracts.md"
FEEDBACK_LINKAGE_GUARD = ROOT / (
    "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/"
    "service/pro/feedback/FeedbackScheduleLinkageGuard.java"
)
FEEDBACK_SERVICE = ROOT / (
    "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/"
    "service/pro/feedback/MesProFeedbackServiceImpl.java"
)
DIRECT_IMPORT_SERVICE = ROOT / (
    "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/"
    "service/pro/feedback/importer/ThirdPartyFeedbackImportServiceImpl.java"
)
FEEDBACK_SERVICE_TEST = ROOT / (
    "yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/"
    "service/pro/feedback/MesProFeedbackServiceImplTest.java"
)
DIRECT_IMPORT_TEST = ROOT / (
    "yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/"
    "service/pro/feedback/importer/ThirdPartyFeedbackImportServiceImplTest.java"
)


def read_text(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def test_stage4_domain_contract_declares_feedback_linkage_boundaries():
    text = read_text(DOMAIN_CONTRACT_DOC)

    required_snippets = [
        "taskId -> scheduleOrderProcessId -> scheduleOrderId -> workOrderId",
        "禁止只凭产品号、工序名或 Excel 行文本归属报工",
        "正向核导入记录已归属、正式业务单、进度数量增减",
        "负向核记录仍待归属且页面错误可见",
        "缺任务、缺排产链、缺活动任务、缺报工人、缺审批人或重复指纹",
    ]
    for snippet in required_snippets:
        assert snippet in text


def test_stage4_feedback_linkage_guard_is_named_and_shared():
    assert FEEDBACK_LINKAGE_GUARD.exists(), "阶段 4 必须集中命名报工归属链校验出口"

    guard = read_text(FEEDBACK_LINKAGE_GUARD)
    required_guard_snippets = [
        "class FeedbackScheduleLinkageGuard",
        "validateProvidedSnapshotRemaining",
        "validateDirectImportAttributionChain",
        "resolveUniqueTaskForScheduleProcess",
        "rejectTextOnlyAttribution",
        "taskId -> scheduleOrderProcessId -> scheduleOrderId -> workOrderId",
    ]
    for snippet in required_guard_snippets:
        assert snippet in guard

    feedback_service = read_text(FEEDBACK_SERVICE)
    direct_import_service = read_text(DIRECT_IMPORT_SERVICE)
    assert "FeedbackScheduleLinkageGuard" in feedback_service
    assert "feedbackScheduleLinkageGuard.validateProvidedSnapshotRemaining(" in feedback_service
    assert "FeedbackScheduleLinkageGuard" in direct_import_service
    assert "feedbackScheduleLinkageGuard.validateDirectImportAttributionChain(" in direct_import_service


def test_stage4_direct_import_no_longer_uses_stable_task_fallback():
    direct_import_service = read_text(DIRECT_IMPORT_SERVICE)
    guard = read_text(FEEDBACK_LINKAGE_GUARD)

    forbidden_snippets = [
        "shouldFallbackToStableTask",
        "quantityCompare",
        "rightQuantity.compareTo(leftQuantity)",
        "Long.MAX_VALUE",
        ".findFirst();",
    ]
    for snippet in forbidden_snippets:
        assert snippet not in direct_import_service
        assert snippet not in guard

    assert "feedbackScheduleLinkageGuard.resolveUniqueTaskForScheduleProcess(" in direct_import_service
    assert "buildFailedTraceRemark" in direct_import_service
    assert "result.setPendingCount(pendingCount)" in direct_import_service
    assert "private Optional<MesProTaskDO> resolveUniqueTaskForScheduleProcess" not in direct_import_service
    assert "taskCodeMatches.size() == 1" in guard
    assert "tasks.size() == 1" in guard
    assert "return Optional.empty()" in guard


def test_stage4_tests_cover_snapshot_over_remaining_and_missing_task_chain():
    feedback_test = read_text(FEEDBACK_SERVICE_TEST)
    direct_import_test = read_text(DIRECT_IMPORT_TEST)

    assert "testCreateFeedbackWithScheduleSnapshot_shouldRejectWhenExceedScheduleOrderProcessRemainingQuantity" in feedback_test
    assert "createFeedbackWithScheduleSnapshot(reqVO)" in feedback_test
    assert "PRO_FEEDBACK_IMPORT_SCHEDULE_ORDER_PROCESS_REMAINING_NOT_ENOUGH" in feedback_test

    assert "importDirectWorkReportWorkbook_shouldRejectTextOnlyAttributionWhenScheduleProcessLinksMultipleTasks" in direct_import_test
    assert "verify(feedbackService, never()).createFeedbackWithScheduleSnapshot(any())" in direct_import_test
