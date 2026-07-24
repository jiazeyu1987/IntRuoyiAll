from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
MIGRATION = ROOT / "sql/showroom/20260605_showroom_product_revision_attachment_schema.sql"


def test_showroom_product_revision_attachment_migration_is_idempotent_and_indexed():
    sql = MIGRATION.read_text(encoding="utf-8")
    normalized = " ".join(sql.lower().split())

    assert "create table if not exists `showroom_product_revision_attachment`" in normalized
    for column in [
        "`tenant_id` bigint not null default 0",
        "`product_id` bigint not null",
        "`product_revision_id` bigint not null",
        "`asset_type` varchar(16) not null",
        "`file_id` bigint not null",
        "`display_order` int not null default 0",
        "`deleted` bit(1) not null default b'0'",
    ]:
        assert column in normalized

    assert (
        "unique key `uk_showroom_product_revision_attachment_file` "
        "(`tenant_id`, `product_revision_id`, `file_id`)"
    ) in normalized
    assert (
        "key `idx_showroom_product_revision_attachment_revision` "
        "(`tenant_id`, `product_revision_id`, `display_order`, `id`)"
    ) in normalized
    assert (
        "key `idx_showroom_product_revision_attachment_product` "
        "(`tenant_id`, `product_id`, `product_revision_id`)"
    ) in normalized
