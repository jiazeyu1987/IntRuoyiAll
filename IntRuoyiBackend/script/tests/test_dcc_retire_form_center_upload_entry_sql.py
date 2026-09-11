from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]
SQL = REPO_ROOT / "sql" / "mysql" / "20260911_dcc_retire_form_center_upload_entry.sql"


def test_retire_form_center_upload_entry_has_release_metadata():
    sql = SQL.read_text(encoding="utf-8")

    assert sql.startswith(
        "-- release-migration: allowedEnvironments=test,backup,prod; "
        "dependsOn=20260719_business_approval_policy,20260719_dcc_upload_form_policy_seed; "
        "type=data; riskLevel=medium"
    )


def test_retire_form_center_upload_entry_fails_fast_and_retires_only_legacy_dcc_upload():
    sql = SQL.read_text(encoding="utf-8")

    assert "DCC_RETIRE_UPLOAD_POLICY_TABLE_MISSING" in sql
    assert "DCC_RETIRE_UPLOAD_POLICY_INCOMPLETE" in sql
    assert "`system_code` = 'DCC'" in sql
    assert "`object_type` = 'CONTROLLED_FILE'" in sql
    assert "`action_code` = 'UPLOAD'" in sql
    assert "`effect_executor_code` = 'DCC_UPLOAD'" in sql
    assert "`deleted` = b'1'" in sql
