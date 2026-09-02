from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]


def test_project_code_recognition_json_schema_and_mapping():
    migration = (ROOT / "sql/mysql/20260902_dcc_project_code_batch_record_total_recognition_json.sql").read_text(encoding="utf-8")
    do_source = (ROOT / "yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/dal/dataobject/projectcode/DccProjectCodeDO.java").read_text(encoding="utf-8")
    vo_source = (ROOT / "yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/controller/admin/projectcode/vo/DccProjectCodeRespVO.java").read_text(encoding="utf-8")
    import_service = (ROOT / "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecordreport/MesProBatchRecordReportServiceImpl.java").read_text(encoding="utf-8")

    assert "`batch_record_total_recognition_json` longtext" in migration
    assert "private String batchRecordTotalRecognitionJson;" in do_source
    assert "private String batchRecordTotalRecognitionJson;" in vo_source
    assert "saveProjectCodeBatchRecordTotalRecognitionJson(selectedDccProjectCode.getId(), totalRecognitionJson);" in import_service
    assert "return result.withTotalRecognitionJson(totalRecognitionJson);" in import_service
