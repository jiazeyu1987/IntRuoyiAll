from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
SQL_PATH = ROOT / "sql" / "mysql" / "20260828_dcc_registration_certificate_minimal_upload_schema.sql"


def read_sql() -> str:
    assert SQL_PATH.exists(), f"Missing SQL script: {SQL_PATH}"
    return SQL_PATH.read_text(encoding="utf-8").lower()


def test_minimal_upload_schema_replaces_production_relation_check() -> None:
    sql = read_sql()

    assert "release-migration:" in sql
    assert "dependsOn=20260817_dcc_registration_certificate_core".lower() in sql
    assert "20260818_dcc_registration_certificate_access" in sql
    assert "alter table `dcc_registration_certificate_snapshot`" in sql
    assert "drop check `chk_dcc_reg_cert_production_relation`" in sql
    assert "add constraint `chk_dcc_reg_cert_production_relation` check" in sql
    assert "`entrusted_production` = b'0'" in sql
    assert "`self_production` = b'0'" in sql
    assert "`entrusted_enterprise_count` = 0" in sql
    assert "json_type(`entrusted_enterprises_json`) = 'array'" in sql
    assert "update `dcc_registration_certificate_snapshot`" not in sql


def test_minimal_upload_schema_allows_upload_certificate_request_type() -> None:
    sql = read_sql()

    assert "alter table `dcc_registration_certificate_access_request`" in sql
    assert "drop check `chk_dcc_reg_cert_access_request_type`" in sql
    assert "add constraint `chk_dcc_reg_cert_access_request_type` check" in sql
    assert "'view_old_certificate'" in sql
    assert "'download_file'" in sql
    assert "'upload_certificate'" in sql
