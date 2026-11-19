-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=none; type=schema; riskLevel=low
-- Existing-order synchronization reads its active items by tenant and order ID.
-- The tenant-only index scans roughly 74,000 items for every order update.
-- Non-unique access index only: no business records or identity rules change.
ALTER TABLE erp_sale_order_items
  ADD INDEX idx_erp_sale_items_tenant_order_deleted (tenant_id, order_id, deleted),
  ALGORITHM=INPLACE, LOCK=NONE;
-- Rollback (only if requested): ALTER TABLE erp_sale_order_items DROP INDEX idx_erp_sale_items_tenant_order_deleted;
