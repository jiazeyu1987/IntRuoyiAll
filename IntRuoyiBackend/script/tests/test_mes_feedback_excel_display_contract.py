from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
FRONTEND_ROOT = ROOT.parent / "yudao-ui-admin-vue3"


def read_text(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def test_feedback_resp_vo_exposes_excel_display_fields():
    source = read_text(
        ROOT
        / "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/feedback/vo/MesProFeedbackRespVO.java"
    )

    for field in [
        "excelProductCode",
        "excelProductName",
        "excelProcessCode",
        "excelProcessName",
        "excelDepartment",
        "excelEmployeeNo",
        "excelEmployeeName",
        "excelSectionLeader",
        "excelFeedbackTime",
    ]:
        assert f"private " in source and field in source


def test_feedback_page_maps_excel_payload_into_response_fields():
    controller = read_text(
        ROOT
        / "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/feedback/MesProFeedbackController.java"
    )

    assert "ThirdPartyFeedbackImportPayload" in controller
    assert "JsonUtils.parseObject" in controller
    for setter in [
        "setExcelProductCode(payload.getItemCode())",
        "setExcelProductName(payload.getItemName())",
        "setExcelProcessCode(payload.getProcessCode())",
        "setExcelProcessName(payload.getProcessName())",
        "setExcelDepartment(payload.getDepartment())",
        "setExcelEmployeeNo(payload.getFeedbackUserCode())",
        "setExcelEmployeeName(payload.getFeedbackUserName())",
        "setExcelSectionLeader(payload.getApproverName())",
        "setExcelFeedbackTime(payload.getFeedbackTime())",
    ]:
        assert setter in controller


def test_direct_work_report_import_persists_payload_for_feedback_list():
    importer = read_text(
        ROOT
        / "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/feedback/importer/ThirdPartyFeedbackImportServiceImpl.java"
    )

    assert "DirectWorkReportExcelRow" in importer
    assert "ThirdPartyFeedbackImportPayload payload = toPayload(row)" in importer
    assert ".sourcePayloadJson(JsonUtils.toJsonString(payload))" in importer
    assert ".sourceImportRecordId(record.getId())" in importer


def test_frontend_api_declares_excel_display_fields():
    api = read_text(FRONTEND_ROOT / "src/api/mes/pro/feedback/index.ts")

    for field in [
        "excelProductCode",
        "excelProductName",
        "excelProcessCode",
        "excelProcessName",
        "excelDepartment",
        "excelEmployeeNo",
        "excelEmployeeName",
        "excelSectionLeader",
        "excelFeedbackTime",
    ]:
        assert field in api
