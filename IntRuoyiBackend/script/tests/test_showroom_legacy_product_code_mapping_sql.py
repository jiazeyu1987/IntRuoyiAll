from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
MIGRATION = ROOT / "sql/mysql/20260705_showroom_legacy_product_code_mapping.sql"
BASE_SCHEMA = ROOT / "sql/showroom/20260519_showroom_v1_schema.sql"


def test_showroom_legacy_product_code_mapping_migration_adds_explicit_legacy_code_column():
    sql = MIGRATION.read_text(encoding="utf-8")
    normalized = " ".join(sql.lower().split())

    assert "alter table `showroom_product`" in normalized
    assert (
        "add column `legacy_product_code` varchar(64) default null "
        "comment '旧展厅底表产品编码，如 product_001' after `product_code`"
    ) in normalized
    assert (
        "create unique index `uk_showroom_product_legacy_code` "
        "on `showroom_product` (`tenant_id`, `legacy_product_code`)"
    ) in normalized

    upper_sql = sql.upper()
    assert "UPDATE `SHOWROOM_PRODUCT`" not in upper_sql
    assert "DELETE FROM" not in upper_sql
    assert "DROP TABLE" not in upper_sql


def test_showroom_v1_schema_declares_legacy_product_code_mapping_contract():
    schema = BASE_SCHEMA.read_text(encoding="utf-8")
    normalized = " ".join(schema.lower().split())

    assert "`legacy_product_code` varchar(64) default null comment '旧展厅底表产品编码，如 product_001'" in normalized
    assert "unique key `uk_showroom_product_legacy_code` (`tenant_id`, `legacy_product_code`)" in normalized
