from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]


def read_text(relative_path: str) -> str:
    path = REPO_ROOT / relative_path
    assert path.exists(), f"{relative_path} must exist"
    return path.read_text(encoding="utf-8")


def test_form_template_controller_exposes_first_slice_api_and_permissions() -> None:
    controller = read_text(
        "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/batchrecord/"
        "MesProEdhrFormTemplateController.java"
    )

    for fragment in [
        '@RequestMapping("/mes/pro/edhr-form-template")',
        '@GetMapping("/page")',
        '@PostMapping("/create")',
        '@PostMapping("/activate")',
        "mes:pro-edhr-form-template:query",
        "mes:pro-edhr-form-template:create",
        "mes:pro-edhr-form-template:activate",
    ]:
        assert fragment in controller

    for forbidden in ['@PostMapping("/approve")', '@PostMapping("/sign")', '@PostMapping("/recordbook")']:
        assert forbidden not in controller


def test_form_instance_controller_exposes_draft_submit_and_event_contract() -> None:
    controller = read_text(
        "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/batchrecord/"
        "MesProEdhrFormInstanceController.java"
    )

    for fragment in [
        '@RequestMapping("/mes/pro/edhr-form-instance")',
        '@GetMapping("/page")',
        '@GetMapping("/get")',
        '@PostMapping("/create")',
        '@PutMapping("/save-draft")',
        '@PutMapping("/submit")',
        '@GetMapping("/event/page")',
        "mes:pro-edhr-form-instance:query",
        "mes:pro-edhr-form-instance:create",
        "mes:pro-edhr-form-instance:save",
        "mes:pro-edhr-form-instance:submit",
    ]:
        assert fragment in controller

    for forbidden in [
        '@PostMapping("/approve")',
        '@PostMapping("/reject")',
        '@PostMapping("/signature")',
        '@PostMapping("/attachment")',
    ]:
        assert forbidden not in controller


def test_form_service_validates_active_template_required_fields_and_locked_submit() -> None:
    service_impl = read_text(
        "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/"
        "MesProEdhrFormServiceImpl.java"
    )

    for fragment in [
        "TEMPLATE_STATUS_ACTIVE",
        "INSTANCE_STATUS_DRAFT",
        "INSTANCE_STATUS_SUBMITTED",
        "requireActiveTemplate",
        "validateSubmissionValues",
        "validateRequiredField",
        "validateNumberRange",
        "validateEnumOptions",
        "assertDraftEditable",
        "replaceInstanceValues",
        "recordEvent",
        "PRO_EDHR_FORM_TEMPLATE_STATUS_INVALID",
        "PRO_EDHR_FORM_FIELD_REQUIRED",
        "PRO_EDHR_FORM_FIELD_RANGE_INVALID",
        "PRO_EDHR_FORM_FIELD_ENUM_INVALID",
        "PRO_EDHR_FORM_INSTANCE_STATUS_INVALID",
    ]:
        assert fragment in service_impl

    for forbidden in [
        "MesProBatchRecordExecution",
        "openOrCreateByContext",
        "catch (Exception",
        "catch (RuntimeException",
        "SIGNATURE_PASSWORD",
        "RECORD_BOOK",
    ]:
        assert forbidden not in service_impl


def test_form_data_objects_and_mappers_match_contract() -> None:
    expectations = {
        "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/dal/dataobject/pro/batchrecord/"
        "MesProEdhrFormTemplateDO.java": [
            '@TableName("mes_pro_edhr_form_template")',
            "private String templateCode;",
            "private String fieldSchemaJson;",
            "private String status;",
        ],
        "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/dal/dataobject/pro/batchrecord/"
        "MesProEdhrFormInstanceDO.java": [
            '@TableName("mes_pro_edhr_form_instance")',
            "private String instanceCode;",
            "private Long templateId;",
            "private String businessScope;",
            "private Integer version;",
            "private Long submittedBy;",
        ],
        "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/dal/dataobject/pro/batchrecord/"
        "MesProEdhrFormValueDO.java": [
            '@TableName("mes_pro_edhr_form_value")',
            "private Long instanceId;",
            "private String fieldKey;",
            "private String valueJson;",
        ],
        "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/dal/dataobject/pro/batchrecord/"
        "MesProEdhrFormEventDO.java": [
            '@TableName("mes_pro_edhr_form_event")',
            "private Long instanceId;",
            "private Long templateId;",
            "private String eventType;",
            "private String resultStatus;",
        ],
        "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/dal/mysql/pro/batchrecord/"
        "MesProEdhrFormTemplateMapper.java": ["selectByTemplateCode", "selectActiveByTemplateCode", "selectPage"],
        "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/dal/mysql/pro/batchrecord/"
        "MesProEdhrFormInstanceMapper.java": ["selectPage"],
        "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/dal/mysql/pro/batchrecord/"
        "MesProEdhrFormValueMapper.java": ["selectListByInstanceId", "deleteByInstanceId"],
        "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/dal/mysql/pro/batchrecord/"
        "MesProEdhrFormEventMapper.java": ["selectPage"],
    }

    for relative_path, expected_fragments in expectations.items():
        source = read_text(relative_path)
        for fragment in expected_fragments:
            assert fragment in source, f"{relative_path} must contain {fragment}"
