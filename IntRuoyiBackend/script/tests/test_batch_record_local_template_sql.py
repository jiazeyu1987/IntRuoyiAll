from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]


def test_batch_record_import_table_is_removed_from_mysql_schema_sources() -> None:
    schema_paths = [
        REPO_ROOT / "sql" / "mysql" / "20260512_mes_base_schema.sql",
        REPO_ROOT / "sql" / "mysql" / "ruoyi-vue-pro.sql",
    ]

    for schema_path in schema_paths:
        text = schema_path.read_text(encoding="utf-8")
        assert "mes_pro_batch_record_import" not in text
        assert "mes_pro_batch_record_template" in text


def test_batch_record_import_table_is_removed_from_kingbase_schema_source() -> None:
    schema_path = REPO_ROOT / "sql" / "kingbase" / "ruoyi-vue-pro.sql"
    text = schema_path.read_text(encoding="utf-8")

    assert "mes_pro_batch_record_import" not in text
    assert "mes_pro_batch_record_template" in text
