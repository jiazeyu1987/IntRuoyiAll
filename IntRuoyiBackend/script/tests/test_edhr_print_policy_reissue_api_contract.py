from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]


def read_text(relative_path: str) -> str:
    path = REPO_ROOT / relative_path
    assert path.exists(), f"{relative_path} must exist"
    return path.read_text(encoding="utf-8")


def test_print_policy_controller_and_print_task_extensions_are_exposed() -> None:
    policy_controller = read_text(
        "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/batchrecord/"
        "MesProEdhrPrintPolicyController.java"
    )
    print_task_controller = read_text(
        "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/batchrecord/"
        "MesProEdhrPrintTaskController.java"
    )

    for fragment in [
        '@RequestMapping("/mes/pro/edhr-print-policy")',
        '@GetMapping("/page")',
        '@PostMapping("/create")',
        '@PostMapping("/activate")',
        "mes:pro-edhr-print-policy:query",
        "mes:pro-edhr-print-policy:create",
        "mes:pro-edhr-print-policy:activate",
    ]:
        assert fragment in policy_controller

    for fragment in [
        '@PostMapping("/reprint/apply")',
        '@PostMapping("/history-copy")',
        '@PostMapping("/export-history")',
        "mes:pro-edhr-print-task:reprint",
        "mes:pro-edhr-print-task:history-copy",
        "mes:pro-edhr-print-task:export",
        "applyReprint",
        "createVoidHistoryCopy",
        "exportPrintHistory",
    ]:
        assert fragment in print_task_controller


def test_print_policy_service_guards_reprint_limit_void_copy_and_export_audit() -> None:
    service_api = read_text(
        "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/"
        "MesProEdhrLabelPrintService.java"
    )
    service_impl = read_text(
        "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/"
        "MesProEdhrLabelPrintServiceImpl.java"
    )

    for fragment in [
        "getPrintPolicyPage",
        "createPrintPolicy",
        "activatePrintPolicy",
        "applyReprint",
        "createVoidHistoryCopy",
        "exportPrintHistory",
    ]:
        assert fragment in service_api

    for fragment in [
        "requireActivePrintPolicy",
        "requireReasonInPolicy",
        "requireReprintLimit",
        "requireVoidRestrictedSource",
        "requireVoidWatermark",
        "requireExportIdempotency",
        "PRO_EDHR_PRINT_POLICY_NOT_EXISTS",
        "PRO_EDHR_PRINT_POLICY_STATUS_INVALID",
        "PRO_EDHR_REPRINT_REASON_INVALID",
        "PRO_EDHR_REPRINT_LIMIT_EXCEEDED",
        "PRO_EDHR_PRINT_VOID_COPY_SOURCE_INVALID",
        "PRO_EDHR_PRINT_VOID_COPY_WATERMARK_REQUIRED",
        "PRINT_REPRINT_POLICY_ACCEPTED",
        "PRINT_VOID_HISTORY_COPY_CREATED",
        "PRINT_HISTORY_EXPORTED",
        "MesProEdhrReprintRequestDO",
        "MesProEdhrPrintHistoryCopyDO",
        "MesProEdhrPrintExportAuditDO",
    ]:
        assert fragment in service_impl

    for forbidden in [
        "catch (Exception",
        "catch (RuntimeException",
        "window.print",
        "MOCK_PRINT",
        "DEFAULT_SUCCESS",
        "setPrintCountDeducted(true).setStatus(STATUS_SUCCESS_CONFIRMED)",
    ]:
        assert forbidden not in service_impl


def test_print_policy_data_objects_mappers_and_vos_match_contract() -> None:
    expected_files = {
        "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/dal/dataobject/pro/batchrecord/"
        "MesProEdhrPrintPolicyDO.java": [
            '@TableName("mes_pro_edhr_print_policy")',
            "private String policyCode;",
            "private Integer firstPrintLimit;",
            "private Integer reprintLimit;",
            "private String reasonDictJson;",
            "private String voidCopyWatermark;",
        ],
        "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/dal/dataobject/pro/batchrecord/"
        "MesProEdhrReprintRequestDO.java": [
            '@TableName("mes_pro_edhr_reprint_request")',
            "private String requestCode;",
            "private Long originalPrintTaskId;",
            "private String reprintReasonCode;",
            "private Integer usedReprintCount;",
            "private Integer reprintLimit;",
        ],
        "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/dal/dataobject/pro/batchrecord/"
        "MesProEdhrPrintHistoryCopyDO.java": [
            '@TableName("mes_pro_edhr_print_history_copy")',
            "private String copyCode;",
            "private Long sourcePrintTaskId;",
            "private String watermarkText;",
            "private String evidenceHash;",
        ],
        "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/dal/dataobject/pro/batchrecord/"
        "MesProEdhrPrintExportAuditDO.java": [
            '@TableName("mes_pro_edhr_print_export_audit")',
            "private String exportCode;",
            "private String filterSnapshotJson;",
            "private String evidenceHash;",
            "private String idempotencyKey;",
        ],
        "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/dal/mysql/pro/batchrecord/"
        "MesProEdhrPrintPolicyMapper.java": ["selectByPolicyCode", "selectActivePolicy", "selectPage"],
        "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/dal/mysql/pro/batchrecord/"
        "MesProEdhrReprintRequestMapper.java": ["selectByIdempotencyKey", "countByOriginalPrintTaskId"],
        "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/dal/mysql/pro/batchrecord/"
        "MesProEdhrPrintHistoryCopyMapper.java": ["selectByIdempotencyKey", "selectPage"],
        "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/dal/mysql/pro/batchrecord/"
        "MesProEdhrPrintExportAuditMapper.java": ["selectByIdempotencyKey", "selectPage"],
        "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/batchrecord/vo/"
        "MesProEdhrPrintPolicyCreateReqVO.java": ["private Integer reprintLimit;", "private String reasonDictJson;"],
        "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/batchrecord/vo/"
        "MesProEdhrReprintApplyReqVO.java": ["private String reprintReasonCode;", "private String idempotencyKey;"],
        "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/batchrecord/vo/"
        "MesProEdhrPrintHistoryExportReqVO.java": ["private String filterSnapshotJson;", "private String idempotencyKey;"],
    }

    for relative_path, fragments in expected_files.items():
        source = read_text(relative_path)
        for fragment in fragments:
            assert fragment in source, f"{relative_path} must contain {fragment}"
