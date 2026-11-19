-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=none; type=schema; riskLevel=low
-- New-sale-order creation checks a generated local number within the tenant.
-- Actual synchronization sampled repeated tenant-only scans (~24,159 rows).
-- Non-unique access index: retain existing number validation and business data.
ALTER TABLE erp_sale_order
  ADD INDEX idx_erp_sale_tenant_no_deleted (tenant_id, no, deleted),
  ALGORITHM=INPLACE, LOCK=NONE;
-- Rollback (only if requested): ALTER TABLE erp_sale_order DROP INDEX idx_erp_sale_tenant_no_deleted;
