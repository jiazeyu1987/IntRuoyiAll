from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
MIGRATION = ROOT / "sql" / "mysql" / "20260903_dcc_registration_certificate_upload_remove_project_code_permission.sql"
UPLOAD_DIALOG = ROOT.parents[0] / "IntRuoyiFronted" / "src" / "views" / "dcc" / "registration-certificate" / "upload" / "UploadDialog.vue"
UPLOAD_CONTROLLER = ROOT / "yudao-module-dcc" / "src" / "main" / "java" / "cn" / "iocoder" / "yudao" / "module" / "dcc" / "registrationcertificate" / "controller" / "admin" / "upload" / "DccRegistrationCertificateUploadController.java"


def test_upload_role_project_code_permission_removal_is_precise_and_idempotent() -> None:
    text = MIGRATION.read_text(encoding="utf-8")

    assert text.splitlines()[0] == (
        "-- release-migration: allowedEnvironments=test,backup,prod; "
        "dependsOn=20260903_dcc_registration_certificate_upload_action_permissions; "
        "type=permission; riskLevel=low"
    )
    assert "'dcc_registration_certificate_upload'" in text
    assert "'dcc:project-code:query'" in text
    assert "SET `role_menu`.`deleted` = b'1'" in text
    assert "DELETE FROM" not in text.upper()


def test_upload_dialog_requires_project_code_from_upload_specific_candidates() -> None:
    text = UPLOAD_DIALOG.read_text(encoding="utf-8")

    assert 'label="实际项目代码"' in text
    assert "getProjectCodePage" not in text
    assert "getUploadProjectCodes" in text
    assert "projectCodeId: [{ required: true" in text


def test_upload_project_code_candidates_use_upload_permission_not_project_code_page_permission() -> None:
    text = UPLOAD_CONTROLLER.read_text(encoding="utf-8")

    assert '@GetMapping("/project-codes")' in text
    assert "获得注册证上传实际项目代码候选" in text
    assert "@PreAuthorize(\"@ss.hasPermission('dcc:registration-certificate:upload:create')\")" in text
