from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
APPROVAL_SQL_PATH = (
    ROOT / "sql" / "mysql" / "20260831_dcc_registration_certificate_change_approval_mvp.sql"
)
PERMISSION_SQL_PATH = (
    ROOT / "sql" / "mysql" / "20260901_dcc_registration_certificate_change_submit_role_permission.sql"
)


def read_sql(path: Path) -> str:
    assert path.exists(), f"Missing SQL script: {path}"
    return path.read_text(encoding="utf-8").lower()


def test_change_approval_mvp_adds_pending_approval_contract() -> None:
    sql = read_sql(APPROVAL_SQL_PATH)

    assert "release-migration:" in sql
    assert "dependsOn=20260818_dcc_registration_certificate_lifecycle".lower() in sql
    assert "20260828_dcc_registration_certificate_upload_approval_request_type" in sql
    assert "alter table `dcc_registration_certificate_change`" in sql
    assert "add column `approval_request_id` bigint" in sql
    assert "add column `reviewer_user_id` bigint" in sql
    assert "add column `reviewed_at` datetime" in sql
    assert "modify column `applied_at` datetime default null" in sql
    assert "'pending_approval'" in sql
    assert "'rejected'" in sql
    assert "unique key `uk_dcc_reg_cert_change_approval_request`" in sql
    assert "'change_submitted'" in sql


def test_change_submit_permission_sql_only_grants_existing_upload_or_renewal_roles() -> None:
    sql = read_sql(PERMISSION_SQL_PATH)

    assert "release-migration:" in sql
    assert "dcc:registration-certificate:change:submit" in sql
    assert "dcc:registration-certificate:upload:create" in sql
    assert "dcc:registration-certificate:renewal:upload" in sql
    assert "insert into `system_role_menu`" in sql
    assert "update `system_role_menu`" in sql
    assert "delete from" not in sql
    assert "truncate table `tmp_dcc_reg_cert_change_submit_roles`" in sql
