from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]


def read_text(relative_path: str) -> str:
    path = REPO_ROOT / relative_path
    assert path.exists(), f"{relative_path} must exist"
    return path.read_text(encoding="utf-8")


def test_recordbook_template_controller_exposes_template_api_and_permissions() -> None:
    controller = read_text(
        "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/batchrecord/"
        "MesProEdhrRecordbookTemplateController.java"
    )

    for fragment in [
        '@RequestMapping("/mes/pro/edhr-recordbook-template")',
        '@GetMapping("/page")',
        '@PostMapping("/create")',
        '@PostMapping("/activate")',
        "mes:pro-edhr-recordbook-template:query",
        "mes:pro-edhr-recordbook-template:create",
        "mes:pro-edhr-recordbook-template:activate",
    ]:
        assert fragment in controller

    for forbidden in ['@PostMapping("/approve")', '@PostMapping("/sign")', '@PostMapping("/batch-record")']:
        assert forbidden not in controller


def test_recordbook_controller_exposes_my_recordbook_and_create_contract() -> None:
    controller = read_text(
        "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/batchrecord/"
        "MesProEdhrRecordbookController.java"
    )

    for fragment in [
        '@RequestMapping("/mes/pro/edhr-recordbook")',
        '@GetMapping("/page")',
        '@GetMapping("/my-page")',
        '@PostMapping("/create")',
        "mes:pro-edhr-recordbook:query",
        "mes:pro-edhr-recordbook:create",
    ]:
        assert fragment in controller

    for forbidden in ['@PostMapping("/approve")', '@PostMapping("/signature")', '@PostMapping("/print")']:
        assert forbidden not in controller


def test_recordbook_entry_and_tag_controllers_expose_entry_tag_event_contract() -> None:
    entry_controller = read_text(
        "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/batchrecord/"
        "MesProEdhrRecordbookEntryController.java"
    )
    tag_controller = read_text(
        "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/batchrecord/"
        "MesProEdhrTagController.java"
    )

    for fragment in [
        '@RequestMapping("/mes/pro/edhr-recordbook-entry")',
        '@GetMapping("/page")',
        '@GetMapping("/get")',
        '@PostMapping("/create")',
        '@PutMapping("/save-draft")',
        '@PutMapping("/submit")',
        '@GetMapping("/event/page")',
        "mes:pro-edhr-recordbook-entry:query",
        "mes:pro-edhr-recordbook-entry:create",
        "mes:pro-edhr-recordbook-entry:save",
        "mes:pro-edhr-recordbook-entry:submit",
    ]:
        assert fragment in entry_controller

    for fragment in [
        '@RequestMapping("/mes/pro/edhr-tag")',
        '@GetMapping("/page")',
        '@PostMapping("/create")',
        '@PostMapping("/activate")',
        '@PostMapping("/disable")',
        "mes:pro-edhr-tag:query",
        "mes:pro-edhr-tag:create",
        "mes:pro-edhr-tag:activate",
        "mes:pro-edhr-tag:disable",
    ]:
        assert fragment in tag_controller


def test_recordbook_service_validates_active_template_open_book_tags_and_locked_submit() -> None:
    service_impl = read_text(
        "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/"
        "MesProEdhrRecordbookServiceImpl.java"
    )

    for fragment in [
        "RECORD_BOOK_TEMPLATE_STATUS_ACTIVE",
        "RECORD_BOOK_STATUS_OPEN",
        "ENTRY_STATUS_DRAFT",
        "ENTRY_STATUS_SUBMITTED",
        "TAG_STATUS_ACTIVE",
        "requireActiveTemplate",
        "requireOpenRecordbook",
        "assertEntryEditable",
        "validateEntryContent",
        "validateControlledTags",
        "replaceTagBindings",
        "recordEvent",
        "PRO_EDHR_RECORDBOOK_TEMPLATE_STATUS_INVALID",
        "PRO_EDHR_RECORDBOOK_STATUS_INVALID",
        "PRO_EDHR_RECORDBOOK_ENTRY_STATUS_INVALID",
        "PRO_EDHR_RECORDBOOK_ENTRY_CONTENT_EMPTY",
        "PRO_EDHR_RECORDBOOK_TAG_INVALID",
    ]:
        assert fragment in service_impl

    for forbidden in [
        "MesProBatchRecordExecution",
        "MesProEdhrFormInstance",
        "catch (Exception",
        "catch (RuntimeException",
        "SIGNATURE_PASSWORD",
        "BpmTask",
        "DEFAULT_SUCCESS",
        "MOCK_TAG",
    ]:
        assert forbidden not in service_impl


def test_recordbook_data_objects_and_mappers_match_contract() -> None:
    expectations = {
        "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/dal/dataobject/pro/batchrecord/"
        "MesProEdhrRecordbookTemplateDO.java": [
            '@TableName("mes_pro_edhr_recordbook_template")',
            "private String templateCode;",
            "private String entrySchemaJson;",
            "private String tagPolicyJson;",
            "private String status;",
        ],
        "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/dal/dataobject/pro/batchrecord/"
        "MesProEdhrRecordbookDO.java": [
            '@TableName("mes_pro_edhr_recordbook")',
            "private String recordbookCode;",
            "private Long templateId;",
            "private Long ownerUserId;",
            "private Integer entryCount;",
        ],
        "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/dal/dataobject/pro/batchrecord/"
        "MesProEdhrRecordbookEntryDO.java": [
            '@TableName("mes_pro_edhr_recordbook_entry")',
            "private Long recordbookId;",
            "private String entryContentJson;",
            "private String tagSnapshotJson;",
            "private String idempotencyKey;",
        ],
        "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/dal/dataobject/pro/batchrecord/"
        "MesProEdhrControlledTagDO.java": [
            '@TableName("mes_pro_edhr_controlled_tag")',
            "private String tagCode;",
            "private String tagName;",
            "private String tagStatus;",
        ],
        "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/dal/dataobject/pro/batchrecord/"
        "MesProEdhrRecordbookTagBindingDO.java": [
            '@TableName("mes_pro_edhr_recordbook_tag_binding")',
            "private Long entryId;",
            "private String tagCode;",
            "private String tagStatus;",
        ],
        "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/dal/dataobject/pro/batchrecord/"
        "MesProEdhrRecordbookEventDO.java": [
            '@TableName("mes_pro_edhr_recordbook_event")',
            "private Long recordbookId;",
            "private Long entryId;",
            "private String eventType;",
            "private String eventSnapshotJson;",
        ],
        "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/dal/mysql/pro/batchrecord/"
        "MesProEdhrRecordbookTemplateMapper.java": ["selectByTemplateCode", "selectPage"],
        "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/dal/mysql/pro/batchrecord/"
        "MesProEdhrRecordbookMapper.java": ["selectByRecordbookCode", "selectPage"],
        "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/dal/mysql/pro/batchrecord/"
        "MesProEdhrRecordbookEntryMapper.java": ["selectByEntryCode", "selectByRecordbookIdAndIdempotencyKey", "selectPage"],
        "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/dal/mysql/pro/batchrecord/"
        "MesProEdhrControlledTagMapper.java": ["selectByTagCode", "selectActiveByTagCode", "selectPage"],
        "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/dal/mysql/pro/batchrecord/"
        "MesProEdhrRecordbookTagBindingMapper.java": ["selectListByEntryId", "deleteByEntryId"],
        "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/dal/mysql/pro/batchrecord/"
        "MesProEdhrRecordbookEventMapper.java": ["selectPage"],
    }

    for relative_path, expected_fragments in expectations.items():
        source = read_text(relative_path)
        for fragment in expected_fragments:
            assert fragment in source, f"{relative_path} must contain {fragment}"
