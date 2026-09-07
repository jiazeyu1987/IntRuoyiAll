from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
SQL_PATH = ROOT / "sql" / "mysql" / "20260903_dcc_controlled_file_related_file.sql"


def read_sql() -> str:
    assert SQL_PATH.exists(), "missing DCC controlled-file relation migration"
    return SQL_PATH.read_text(encoding="utf-8")


def test_related_file_schema_declares_release_metadata() -> None:
    sql = read_sql()

    assert sql.startswith(
        "-- release-migration: allowedEnvironments=test,backup,prod; "
        "dependsOn=20260513_dcc_base_schema; type=schema; riskLevel=medium"
    )


def test_related_file_schema_keeps_relation_identity_and_lookup_indexes() -> None:
    sql = read_sql()

    assert "CREATE TABLE IF NOT EXISTS `dcc_controlled_file_related_file`" in sql
    assert "UNIQUE KEY `uk_dcc_related_file`" in sql
    assert "KEY `idx_dcc_related_file_target`" in sql
    assert "KEY `idx_dcc_related_file_project`" in sql
    assert "`related_master_id` BIGINT NULL" in sql
    assert "`relation_source` VARCHAR(32) NOT NULL" in sql
