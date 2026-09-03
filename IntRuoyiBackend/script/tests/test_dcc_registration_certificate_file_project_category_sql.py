from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
SQL_PATH = ROOT / "sql/mysql/20260901_dcc_registration_certificate_file_project_category.sql"


def read_sql() -> str:
    assert SQL_PATH.exists(), f"Missing SQL script: {SQL_PATH}"
    return SQL_PATH.read_text(encoding="utf-8").lower()


def test_registration_certificate_file_project_category_sql_adds_nullable_project_and_taxonomy_snapshot_columns():
    sql = read_sql()

    assert "release-migration:" in sql
    assert "dependson=20260817_dcc_registration_certificate_core" in sql
    assert "missing dcc_registration_certificate_file for project category migration" in sql
    assert "add column `dcc_project_code_id` bigint default null" in sql
    assert "add column `file_type_taxonomy_id` bigint default null" in sql
    assert "add column `file_type_level1` varchar(64) default null" in sql
    assert "add column `file_type_level2` varchar(128) default null" in sql
    assert "add column `file_type_level3` varchar(128) default null" in sql
    assert "add column `file_type_level4` varchar(128) default null" in sql
    assert "add column `file_type_level5` varchar(128) default null" in sql


def test_registration_certificate_file_project_category_sql_verifies_indexes_and_does_not_backfill_history():
    sql = read_sql()

    assert "idx_dcc_reg_cert_file_project_code" in sql
    assert "add key `idx_dcc_reg_cert_file_project_code` (`tenant_id`, `dcc_project_code_id`)" in sql
    assert "idx_dcc_reg_cert_file_taxonomy" in sql
    assert "add key `idx_dcc_reg_cert_file_taxonomy` (`tenant_id`, `file_type_taxonomy_id`, `deleted`)" in sql
    assert "registration certificate file project category columns incomplete" in sql
    assert "registration certificate file project category indexes incomplete" in sql
    assert "update `dcc_registration_certificate_file`" not in sql
    assert "delete from `dcc_registration_certificate_file`" not in sql
