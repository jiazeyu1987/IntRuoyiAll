from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]


def read_text(relative_path: str) -> str:
    path = REPO_ROOT / relative_path
    assert path.exists(), f"{relative_path} must exist"
    return path.read_text(encoding="utf-8")


def test_traveler_controllers_expose_first_slice_api_and_permissions() -> None:
    template_controller = read_text(
        "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/batchrecord/"
        "MesProEdhrTravelerTemplateController.java"
    )
    traveler_controller = read_text(
        "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/batchrecord/"
        "MesProEdhrTravelerController.java"
    )

    for fragment in [
        '@RequestMapping("/mes/pro/edhr-traveler-template")',
        '@GetMapping("/page")',
        '@PostMapping("/create")',
        '@PostMapping("/activate")',
        "mes:pro-edhr-traveler-template:query",
        "mes:pro-edhr-traveler-template:create",
        "mes:pro-edhr-traveler-template:activate",
    ]:
        assert fragment in template_controller

    for fragment in [
        '@RequestMapping("/mes/pro/edhr-traveler")',
        '@GetMapping("/page")',
        '@GetMapping("/get")',
        '@PostMapping("/generate")',
        '@GetMapping("/event/page")',
        "mes:pro-edhr-traveler:query",
        "mes:pro-edhr-traveler:generate",
    ]:
        assert fragment in traveler_controller

    for forbidden in ['@PostMapping("/print")', '@PostMapping("/reprint")', "printSuccess"]:
        assert forbidden not in traveler_controller


def test_traveler_service_validates_real_business_bindings_and_duplicate_key() -> None:
    service_impl = read_text(
        "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/"
        "MesProEdhrTravelerServiceImpl.java"
    )

    for fragment in [
        "batchExecutionMapper.selectById",
        "routeProcessMapper.selectById",
        "processMapper.selectById",
        "snMapper.selectOne",
        "businessKeyHash",
        "selectByBusinessKeyHash",
        "PRO_EDHR_TRAVELER_ALREADY_EXISTS",
        "PRO_EDHR_TRAVELER_ROUTE_PROCESS_MISMATCH",
        "PRO_EDHR_TRAVELER_SN_MISMATCH",
        "MesProEdhrTravelerEventDO",
        "GENERATE_DUPLICATE",
        "BATCH_LEVEL",
        "SN_LEVEL",
    ]:
        assert fragment in service_impl

    for forbidden in [
        "catch (Exception",
        "catch (RuntimeException",
        "PRINT_SUCCESS",
        "DEDUCT_PRINT",
        "REPRINT",
    ]:
        assert forbidden not in service_impl


def test_traveler_data_objects_and_mappers_match_schema_contract() -> None:
    for relative_path, expected in {
        "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/dal/dataobject/pro/batchrecord/"
        "MesProEdhrTravelerTemplateDO.java": [
            '@TableName("mes_pro_edhr_traveler_template")',
            "private String templateCode;",
            "private String templateVersion;",
            "private String status;",
        ],
        "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/dal/dataobject/pro/batchrecord/"
        "MesProEdhrTravelerInstanceDO.java": [
            '@TableName("mes_pro_edhr_traveler_instance")',
            "private String travelerCode;",
            "private Long batchExecutionId;",
            "private Long routeProcessId;",
            "private String businessKeyHash;",
        ],
        "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/dal/dataobject/pro/batchrecord/"
        "MesProEdhrTravelerEventDO.java": [
            '@TableName("mes_pro_edhr_traveler_event")',
            "private Long travelerId;",
            "private String eventType;",
            "private String resultStatus;",
        ],
        "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/dal/mysql/pro/batchrecord/"
        "MesProEdhrTravelerTemplateMapper.java": ["selectActiveTemplate"],
        "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/dal/mysql/pro/batchrecord/"
        "MesProEdhrTravelerInstanceMapper.java": ["selectByBusinessKeyHash", "selectPage"],
        "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/dal/mysql/pro/batchrecord/"
        "MesProEdhrTravelerEventMapper.java": ["selectPage"],
    }.items():
        source = read_text(relative_path)
        for fragment in expected:
            assert fragment in source, f"{relative_path} must contain {fragment}"
