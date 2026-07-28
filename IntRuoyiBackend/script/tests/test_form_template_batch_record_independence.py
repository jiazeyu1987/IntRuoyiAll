from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]


def test_form_template_batch_record_binding_migration_is_removed() -> None:
    assert not (
        REPO_ROOT / "sql" / "mysql" / "20260727_bpm_form_template_batch_record_binding.sql"
    ).exists()
    assert not (
        REPO_ROOT / "script" / "tests" / "test_form_template_batch_record_binding_sql.py"
    ).exists()


def test_form_center_sources_do_not_define_batch_record_binding_fields() -> None:
    source_paths = [
        REPO_ROOT
        / "yudao-module-bpm"
        / "src"
        / "main"
        / "java"
        / "cn"
        / "iocoder"
        / "yudao"
        / "module"
        / "bpm"
        / "controller"
        / "admin"
        / "formcenter"
        / "vo"
        / "FormCenterTemplateRespVO.java",
        REPO_ROOT
        / "yudao-module-bpm"
        / "src"
        / "main"
        / "java"
        / "cn"
        / "iocoder"
        / "yudao"
        / "module"
        / "bpm"
        / "dal"
        / "dataobject"
        / "formcenter"
        / "FormTemplateVersionDO.java",
        REPO_ROOT
        / "yudao-module-bpm"
        / "src"
        / "main"
        / "java"
        / "cn"
        / "iocoder"
        / "yudao"
        / "module"
        / "bpm"
        / "formcenter"
        / "runtime"
        / "FormCenterRuntimeServiceImpl.java",
    ]
    forbidden_fields = [
        "batchRecordReportId",
        "batchRecordReportName",
        "batchRecordName",
        "batchRecordVersionNo",
        "batchRecordFormSlotType",
        "batchRecordBindingStatus",
        "batchRecordBindingError",
    ]

    for source_path in source_paths:
        source = source_path.read_text(encoding="utf-8")
        for field in forbidden_fields:
            assert field not in source, f"{source_path.name} must not contain {field}"
