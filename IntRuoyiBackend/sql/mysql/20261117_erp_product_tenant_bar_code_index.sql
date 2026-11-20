-- release-migration: allowedEnvironments=test,backup,prod; type=schema; riskLevel=low; requiresTargetPreflight=true
-- ERP full order synchronization resolves each source material by tenant/code.
-- The tenant-only index scans the entire material catalogue for every lookup.
-- Non-unique: this migration changes access cost, not data or uniqueness rules.
ALTER TABLE erp_product
  ADD INDEX idx_erp_product_tenant_bar_deleted (tenant_id, bar_code, deleted),
  ALGORITHM=INPLACE, LOCK=NONE;
-- Rollback (only if requested): ALTER TABLE erp_product DROP INDEX idx_erp_product_tenant_bar_deleted;
