from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]


def read_text(relative_path: str) -> str:
    path = REPO_ROOT / relative_path
    assert path.exists(), f"{relative_path} must exist"
    return path.read_text(encoding="utf-8")


def test_label_print_controllers_expose_queue_api_and_permissions() -> None:
    label_template_controller = read_text(
        "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/batchrecord/"
        "MesProEdhrLabelTemplateController.java"
    )
    label_controller = read_text(
        "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/batchrecord/"
        "MesProEdhrLabelController.java"
    )
    print_task_controller = read_text(
        "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/batchrecord/"
        "MesProEdhrPrintTaskController.java"
    )

    for fragment in [
        '@RequestMapping("/mes/pro/edhr-label-template")',
        '@GetMapping("/page")',
        '@PostMapping("/create")',
        '@PostMapping("/activate")',
        "mes:pro-edhr-label-template:query",
        "mes:pro-edhr-label-template:create",
        "mes:pro-edhr-label-template:activate",
    ]:
        assert fragment in label_template_controller

    for fragment in [
        '@RequestMapping("/mes/pro/edhr-label")',
        '@GetMapping("/page")',
        '@PostMapping("/preview")',
        "mes:pro-edhr-label:query",
        "mes:pro-edhr-label:preview",
    ]:
        assert fragment in label_controller

    for fragment in [
        '@RequestMapping("/mes/pro/edhr-print-task")',
        '@GetMapping("/page")',
        '@PostMapping("/create")',
        '@PostMapping("/mark-failed")',
        '@PostMapping("/confirm")',
        "mes:pro-edhr-print-task:query",
        "mes:pro-edhr-print-task:create",
        "mes:pro-edhr-print-task:mark-failed",
        "mes:pro-edhr-print-task:confirm",
    ]:
        assert fragment in print_task_controller


def test_label_print_service_blocks_fake_success_and_requires_reprint_reason() -> None:
    service_impl = read_text(
        "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/"
        "MesProEdhrLabelPrintServiceImpl.java"
    )

    for fragment in [
        "PENDING_CONFIRM",
        "SUCCESS_CONFIRMED",
        "FAILED",
        "VOID_RESTRICTED",
        "requireReprintReason",
        "requireOriginalPrintTask",
        "requireConfirmationEvidence",
        "printCountDeducted",
        "selectByIdempotencyKey",
        "PRO_EDHR_PRINT_REPRINT_REASON_REQUIRED",
        "PRO_EDHR_PRINT_ORIGINAL_TASK_REQUIRED",
        "PRO_EDHR_PRINT_CONFIRMATION_EVIDENCE_REQUIRED",
        "MesProEdhrPrintEventDO",
        "PRINT_REPRINT_REQUESTED",
        "PRINT_MARK_FAILED",
        "PRINT_CONFIRM_SUCCESS",
    ]:
        assert fragment in service_impl

    for forbidden in [
        "catch (Exception",
        "catch (RuntimeException",
        "window.print",
        "MOCK_PRINT",
        "DEFAULT_SUCCESS",
        "setStatus(SUCCESS_CONFIRMED)",
    ]:
        assert forbidden not in service_impl


def test_label_print_data_objects_and_mappers_match_schema_contract() -> None:
    expected_files = {
        "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/dal/dataobject/pro/batchrecord/"
        "MesProEdhrLabelTemplateDO.java": [
            '@TableName("mes_pro_edhr_label_template")',
            "private String templateCode;",
            "private String fieldModelJson;",
            "private String parserVersion;",
        ],
        "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/dal/dataobject/pro/batchrecord/"
        "MesProEdhrLabelInstanceDO.java": [
            '@TableName("mes_pro_edhr_label_instance")',
            "private String labelCode;",
            "private String renderSnapshotJson;",
            "private String businessKeyHash;",
        ],
        "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/dal/dataobject/pro/batchrecord/"
        "MesProEdhrPrintTaskDO.java": [
            '@TableName("mes_pro_edhr_print_task")',
            "private String taskCode;",
            "private String printConfirmStatus;",
            "private Boolean printCountDeducted;",
        ],
        "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/dal/dataobject/pro/batchrecord/"
        "MesProEdhrPrintEventDO.java": [
            '@TableName("mes_pro_edhr_print_event")',
            "private Long printTaskId;",
            "private String eventType;",
            "private String evidenceHash;",
        ],
        "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/dal/mysql/pro/batchrecord/"
        "MesProEdhrLabelTemplateMapper.java": ["selectByTemplateCode", "selectActiveTemplate"],
        "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/dal/mysql/pro/batchrecord/"
        "MesProEdhrLabelInstanceMapper.java": ["selectByBusinessKeyHash", "selectPage"],
        "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/dal/mysql/pro/batchrecord/"
        "MesProEdhrPrintTaskMapper.java": ["selectByIdempotencyKey", "selectPage"],
        "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/dal/mysql/pro/batchrecord/"
        "MesProEdhrPrintEventMapper.java": ["selectPage"],
    }

    for relative_path, fragments in expected_files.items():
        source = read_text(relative_path)
        for fragment in fragments:
            assert fragment in source, f"{relative_path} must contain {fragment}"
