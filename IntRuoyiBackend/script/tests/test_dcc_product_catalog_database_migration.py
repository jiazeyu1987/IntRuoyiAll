from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]
MIGRATION_SQL = REPO_ROOT / "sql/mysql/20260710_dcc_product_catalog_database.sql"
APPLICATION_YAML = REPO_ROOT / "yudao-server/src/main/resources/application.yaml"


def test_product_catalog_migration_declares_global_table_and_seed_rows() -> None:
    sql = MIGRATION_SQL.read_text(encoding="utf-8")

    assert "CREATE TABLE IF NOT EXISTS `dcc_product_catalog`" in sql
    assert "`tenant_id`" not in sql
    assert "`uk_dcc_product_catalog_source_row`" in sql
    assert "ON DUPLICATE KEY UPDATE `id` = `id`" in sql
    assert sql.count("('子公司产品',") == 32
    assert sql.count("('瑛泰产品',") == 181


def test_product_catalog_table_is_excluded_from_tenant_interceptor() -> None:
    application_yaml = APPLICATION_YAML.read_text(encoding="utf-8")

    assert "ignore-tables:\n      - dcc_product_catalog" in application_yaml
