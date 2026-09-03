from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
MIGRATION = ROOT / "sql" / "mysql" / "20260903_dcc_registration_certificate_upload_action_permissions.sql"
PAGE = ROOT.parents[0] / "IntRuoyiFronted" / "src" / "views" / "dcc" / "registration-certificate" / "index" / "index.vue"


def test_upload_role_action_permission_migration_grants_required_actions_only() -> None:
    text = MIGRATION.read_text(encoding="utf-8")

    assert text.splitlines()[0] == (
        "-- release-migration: allowedEnvironments=test,backup,prod; "
        "dependsOn=20260903_dcc_registration_certificate_upload_view_permission; "
        "type=permission; riskLevel=low"
    )
    assert "'dcc_registration_certificate_upload'" in text
    for permission in [
        "dcc:registration-certificate:upload:create",
        "dcc:registration-certificate:renewal:upload",
        "dcc:registration-certificate:change:submit",
        "dcc:project-code:query",
    ]:
        assert f"'{permission}'" in text
    for forbidden in [
        "upload:approve",
        "renewal:void",
        "registration-certificate:void",
        "config:update",
    ]:
        assert forbidden not in text


def test_registration_certificate_page_has_no_test_tab() -> None:
    text = PAGE.read_text(encoding="utf-8")

    assert 'name="test" label="注册测试"' not in text
    assert "handleSimulateDailyRun" not in text
    assert "activeTab.value === 'test'" not in text
