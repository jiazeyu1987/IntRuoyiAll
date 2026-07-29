from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]
CLEANUP_SQL = REPO_ROOT / "sql/mysql/20260729_dcc_product_catalog_remove_subsidiary_source.sql"


def test_product_catalog_cleanup_migration_removes_subsidiary_source_only() -> None:
    assert CLEANUP_SQL.exists(), "DCC product catalog subsidiary-source cleanup migration must exist"
    sql = CLEANUP_SQL.read_text(encoding="utf-8")

    assert "release-migration:" in sql
    assert "dependsOn=20260729_dcc_product_catalog_project_code_columns; type=data; riskLevel=medium" in sql
    assert "DELETE FROM `dcc_product_catalog`" in sql
    assert "HEX(`data_source`) = 'E5AD90E585ACE58FB8E4BAA7E59381'" in sql
    assert "HEX(`data_source`) = 'E7919BE6B3B0E4BAA7E59381'" not in sql
    assert "UPDATE `dcc_product_catalog`" not in sql
    assert "DROP TABLE" not in sql
    assert "TRUNCATE TABLE" not in sql

