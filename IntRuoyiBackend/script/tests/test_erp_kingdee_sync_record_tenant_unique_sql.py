from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]


def test_kingdee_sync_record_migration_scopes_unique_keys_by_tenant() -> None:
    sql = (REPO_ROOT / "sql" / "mysql" / "20260613_erp_kingdee_sync_record_tenant_unique.sql").read_text(
        encoding="utf-8"
    )

    assert "INFORMATION_SCHEMA.STATISTICS" in sql
    assert "ensure_erp_kingdee_sync_record_tenant_unique" in sql
    assert "CALL ensure_erp_kingdee_sync_record_tenant_unique" in sql
    assert "DROP INDEX `" in sql
    assert "'erp_kingdee_supplier_sync_record'" in sql
    assert "'uk_erp_kingdee_supplier_source'" in sql
    assert (
        "'uk_erp_kingdee_supplier_tenant_source',\n"
        "    '(`tenant_id`, `source_supplier_number`, `deleted`)'"
    ) in sql
    assert "'erp_kingdee_customer_sync_record'" in sql
    assert "'uk_erp_kingdee_customer_source'" in sql
    assert (
        "'uk_erp_kingdee_customer_tenant_source',\n"
        "    '(`tenant_id`, `source_customer_number`, `deleted`)'"
    ) in sql
    assert "'erp_kingdee_purchase_order_sync_record'" in sql
    assert "'uk_erp_kingdee_po_sync_source'" in sql
    assert (
        "'uk_erp_kingdee_po_sync_tenant_source',\n"
        "    '(`tenant_id`, `source_form_id`, `source_fid`, `deleted`)'"
    ) in sql
    assert "'erp_kingdee_sale_order_sync_record'" in sql
    assert "'uk_erp_kingdee_sale_source'" in sql
    assert (
        "'uk_erp_kingdee_sale_tenant_source',\n"
        "    '(`tenant_id`, `source_form_id`, `source_fid`, `deleted`)'"
    ) in sql


def test_kingdee_sync_record_migration_is_replay_safe() -> None:
    sql = (REPO_ROOT / "sql" / "mysql" / "20260613_erp_kingdee_sync_record_tenant_unique.sql").read_text(
        encoding="utf-8"
    )

    assert "IF v_old_index_count > 0 THEN" in sql
    assert "IF v_new_index_count = 0 THEN" in sql
    assert "SET @kingdee_tenant_unique_sql = v_sql;" in sql
    assert "PREPARE stmt FROM @kingdee_tenant_unique_sql;" in sql
    assert "PREPARE stmt FROM v_sql;" not in sql
    assert "SIGNAL SQLSTATE '45000'" in sql
    assert "Missing required old or new Kingdee sync unique index" in sql


def test_kingdee_sync_record_initial_schema_scopes_unique_keys_by_tenant() -> None:
    schema_files = [
        REPO_ROOT / "sql" / "mysql" / "20260512_erp_kingdee_purchase_order_sync.sql",
        REPO_ROOT / "sql" / "mysql" / "20260513_kingdee_multi_sync.sql",
        REPO_ROOT / "sql" / "mysql" / "20260513_erp_kingdee_supplier_sync_record.sql",
    ]
    sql = "\n".join(path.read_text(encoding="utf-8") for path in schema_files)

    assert "UNIQUE KEY `uk_erp_kingdee_po_sync_tenant_source` (`tenant_id`, `source_form_id`, `source_fid`, `deleted`)" in sql
    assert "UNIQUE KEY `uk_erp_kingdee_sale_tenant_source` (`tenant_id`, `source_form_id`, `source_fid`, `deleted`)" in sql
    assert "UNIQUE KEY `uk_erp_kingdee_supplier_tenant_source` (`tenant_id`, `source_supplier_number`, `deleted`)" in sql
    assert "UNIQUE KEY `uk_erp_kingdee_customer_tenant_source` (`tenant_id`, `source_customer_number`, `deleted`)" in sql

    assert "UNIQUE KEY `uk_erp_kingdee_po_sync_source` (`source_form_id`, `source_fid`)" not in sql
    assert "UNIQUE KEY `uk_erp_kingdee_sale_source` (`source_form_id`, `source_fid`)" not in sql
    assert "UNIQUE KEY `uk_erp_kingdee_supplier_source` (`source_supplier_number`)" not in sql
    assert "UNIQUE KEY `uk_erp_kingdee_customer_source` (`source_customer_number`)" not in sql
