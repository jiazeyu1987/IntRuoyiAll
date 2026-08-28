from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
SQL_PATH = ROOT / "sql" / "mysql" / "20260818_dcc_registration_certificate_access.sql"


def read_sql() -> str:
    assert SQL_PATH.exists(), f"Missing SQL script: {SQL_PATH}"
    return SQL_PATH.read_text(encoding="utf-8")


def extract_block(sql: str, start: str, end: str) -> str:
    assert start in sql, f"Missing block start: {start}"
    assert end in sql, f"Missing block end: {end}"
    return sql[sql.index(start) : sql.index(end, sql.index(start))]


def test_access_contract_temp_tables_declare_explicit_text_collation() -> None:
    sql = read_sql()

    expected_column_table = extract_block(
        sql,
        "CREATE TEMPORARY TABLE tmp_dcc_reg_cert_access_expected_column",
        "INSERT INTO tmp_dcc_reg_cert_access_expected_column",
    )
    expected_index_table = extract_block(
        sql,
        "CREATE TEMPORARY TABLE tmp_dcc_reg_cert_access_expected_index",
        "INSERT INTO tmp_dcc_reg_cert_access_expected_index",
    )

    for table_ddl in (expected_column_table, expected_index_table):
        assert "DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci" in table_ddl
