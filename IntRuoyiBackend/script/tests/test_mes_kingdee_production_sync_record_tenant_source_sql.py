from pathlib import Path


SQL_PATH = (
    Path(__file__).resolve().parents[2]
    / "sql"
    / "mysql"
    / "20260612_mes_kingdee_production_sync_record_tenant_source.sql"
)


def test_production_sync_source_unique_key_includes_tenant():
    sql = SQL_PATH.read_text(encoding="utf-8")

    assert "DROP INDEX `uk_mes_kingdee_production_source`" in sql
    assert "ADD UNIQUE INDEX `uk_mes_kingdee_production_source`" in sql
    assert "`tenant_id`, `source_fid`, `source_material_number`, `deleted`" in sql


def test_production_sync_source_sql_has_no_destructive_table_operations():
    sql = SQL_PATH.read_text(encoding="utf-8").lower()

    forbidden_tokens = [
        "drop table",
        "truncate",
        "delete from",
        "drop database",
    ]
    for token in forbidden_tokens:
        assert token not in sql
