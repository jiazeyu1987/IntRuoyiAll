from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]

EDHR_SQL = REPO_ROOT / "sql/mysql/20260615_mes_edhr_tail_four_goals.sql"
DCC_HARDENING_SQL = REPO_ROOT / "sql/mysql/20260526_dcc_electronic_signature_hardening.sql"
DCC_BASE_SCHEMA_SQL = REPO_ROOT / "sql/mysql/20260513_dcc_base_schema.sql"

SIGNATURE_SNAPSHOT_COLUMNS = [
    "actor_username_snapshot",
    "actor_nickname_snapshot",
    "actor_dept_id_snapshot",
    "actor_dept_name_snapshot",
    "actor_post_names_snapshot",
    "actor_role_names_snapshot",
    "signature_purpose",
    "authorization_basis",
    "authentication_method",
    "record_version_snapshot",
    "record_hash_snapshot",
    "client_ip_snapshot",
    "user_agent_snapshot",
    "snapshot_status",
]


def read_sql(path: Path) -> str:
    assert path.exists(), f"{path.relative_to(REPO_ROOT)} must exist"
    return path.read_text(encoding="utf-8")


def assert_no_destructive_sql(sql: str) -> None:
    upper_sql = sql.upper()
    assert "DROP TABLE" not in upper_sql
    assert "TRUNCATE TABLE" not in upper_sql
    assert "DELETE FROM `MES_" not in upper_sql
    assert "DELETE FROM `DCC_" not in upper_sql


def test_edhr_signature_fda_snapshot_migration_is_idempotent_and_complete() -> None:
    sql = read_sql(EDHR_SQL)

    assert_no_destructive_sql(sql)
    assert "CREATE PROCEDURE ensure_mes_edhr_tail_goal_column" in sql
    assert "ADD COLUMN `', p_column_name, '`" in sql
    assert "mes_pro_batch_record_execution_signature" in sql
    for column in SIGNATURE_SNAPSHOT_COLUMNS:
        assert column in sql, f"eDHR migration must include {column}"
        assert (
            f"'mes_pro_batch_record_execution_signature',\n  '{column}'," in sql
        ), f"eDHR migration must add {column} through the idempotent column procedure"


def test_dcc_signature_fda_snapshot_migration_is_idempotent_and_complete() -> None:
    sql = read_sql(DCC_HARDENING_SQL)

    assert_no_destructive_sql(sql)
    assert "dcc_controlled_file_signature" in sql
    for column in SIGNATURE_SNAPSHOT_COLUMNS[4:]:
        assert column in sql, f"DCC hardening migration must include {column}"
        assert f"ADD COLUMN `{column}`" in sql, f"DCC hardening migration must add {column} idempotently"


def test_dcc_base_schema_contains_signature_fda_snapshot_columns() -> None:
    sql = read_sql(DCC_BASE_SCHEMA_SQL)

    assert "CREATE TABLE IF NOT EXISTS `dcc_controlled_file_signature`" in sql
    for column in SIGNATURE_SNAPSHOT_COLUMNS:
        assert f"`{column}`" in sql, f"DCC base schema must include {column}"
