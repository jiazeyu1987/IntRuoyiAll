-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20261011_dcc_product_catalog_source_sequence; type=schema; riskLevel=low
-- Preserve a catalog's business origin across tenant workbook transfers and repeated imports.
-- This sidecar leaves the existing global source-row constraint and allocator intact.
-- Rollback: retain this table while imported catalogs exist; dropping it loses merge identity.
CREATE TABLE IF NOT EXISTS dcc_catalog_workbook_origin (
  tenant_id BIGINT NOT NULL,
  product_catalog_id BIGINT NOT NULL,
  data_source VARCHAR(64) NOT NULL,
  origin_tenant_id BIGINT NOT NULL,
  origin_row_no INT NOT NULL,
  PRIMARY KEY (tenant_id, product_catalog_id),
  UNIQUE KEY uk_dcc_catalog_workbook_origin (tenant_id, data_source, origin_tenant_id, origin_row_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='DCC catalog workbook merge identity';
