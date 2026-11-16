-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20261022_dcc_product_catalog_tenant_schema_guard,20261011_dcc_product_catalog_source_sequence,20260920_dcc_project_product_create_approval,20261004_dcc_product_identity_source; type=schema; riskLevel=medium
-- Catalog business identities are unique inside each tenant, including archived rows.
-- No ownership inference or data copying: historical tenant_id=0 rows remain owned by tenant 0.
-- Before deployment, audit historical cross-tenant catalog references and explicitly resolve ownership.
-- Rollback: reverting the unique index requires first resolving any duplicate (data_source, original_row_no)
-- across tenants. Never delete catalog rows to make the old global index fit.
SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS migrate_dcc_product_catalog_isolation_20261102;
DELIMITER $$
CREATE PROCEDURE migrate_dcc_product_catalog_isolation_20261102()
BEGIN
  DECLARE old_columns TEXT;
  DECLARE new_columns TEXT;
  DECLARE old_non_unique INT;
  DECLARE new_non_unique INT;
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'dcc_product_catalog'
      AND COLUMN_NAME = 'tenant_id' AND DATA_TYPE = 'bigint' AND IS_NULLABLE = 'NO'
  ) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'dcc_product_catalog.tenant_id prerequisite missing or invalid';
  END IF;
  IF EXISTS (
    SELECT 1 FROM dcc_data_relation r JOIN dcc_product_catalog c ON c.id = r.product_catalog_id
    WHERE r.deleted = 0 AND r.tenant_id <> c.tenant_id
  ) OR EXISTS (
    SELECT 1 FROM dcc_project_product_relation r JOIN dcc_product_catalog c ON c.id = r.product_catalog_id
    WHERE r.deleted = 0 AND r.tenant_id <> c.tenant_id
  ) OR EXISTS (
    SELECT 1 FROM dcc_project_product_create_request r JOIN dcc_product_catalog c ON c.id = r.generated_product_catalog_id
    WHERE r.deleted = 0 AND r.tenant_id <> c.tenant_id
  ) OR EXISTS (
    SELECT 1 FROM dcc_controlled_file f JOIN dcc_product_catalog c ON c.id = f.product_catalog_id
    WHERE f.deleted = 0 AND f.tenant_id <> c.tenant_id
  ) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Cross-tenant catalog references exist; resolve ownership before tenant isolation';
  END IF;
  SELECT GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX), MIN(NON_UNIQUE)
    INTO old_columns, old_non_unique
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'dcc_product_catalog'
      AND INDEX_NAME = 'uk_dcc_product_catalog_source_row';
  SELECT GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX), MIN(NON_UNIQUE)
    INTO new_columns, new_non_unique
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'dcc_product_catalog'
      AND INDEX_NAME = 'uk_dcc_product_catalog_tenant_source_row';
  IF old_columns IS NOT NULL AND (old_columns <> 'data_source,original_row_no' OR old_non_unique <> 0) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'dcc_product_catalog old unique index schema mismatch';
  END IF;
  IF new_columns IS NOT NULL AND (new_columns <> 'tenant_id,data_source,original_row_no' OR new_non_unique <> 0) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'dcc_product_catalog tenant unique index schema mismatch';
  END IF;
  IF old_columns IS NULL AND new_columns IS NULL THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'dcc_product_catalog business unique index prerequisite missing';
  END IF;
  IF old_columns IS NOT NULL AND new_columns IS NULL THEN
    ALTER TABLE dcc_product_catalog
      DROP INDEX uk_dcc_product_catalog_source_row,
      ADD UNIQUE INDEX uk_dcc_product_catalog_tenant_source_row (tenant_id, data_source, original_row_no);
  ELSEIF old_columns IS NOT NULL THEN
    ALTER TABLE dcc_product_catalog DROP INDEX uk_dcc_product_catalog_source_row;
  END IF;
END$$
DELIMITER ;
CALL migrate_dcc_product_catalog_isolation_20261102();
DROP PROCEDURE migrate_dcc_product_catalog_isolation_20261102;
