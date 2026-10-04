-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260930_dcc_a_lifecycle,20260920_dcc_project_product_create_approval,20260710_dcc_product_catalog_database; type=schema; riskLevel=medium
-- LD01: explicit server-verified product source snapshots; add only, no historical DML.
SET NAMES utf8mb4;
DROP PROCEDURE IF EXISTS dcc_product_identity_source;
DELIMITER $$
CREATE PROCEDURE dcc_product_identity_source()
BEGIN
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='dcc_controlled_file' AND COLUMN_NAME='product_source') THEN
    ALTER TABLE dcc_controlled_file ADD COLUMN product_source VARCHAR(32) NULL;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='dcc_controlled_file' AND COLUMN_NAME='product_catalog_id') THEN
    ALTER TABLE dcc_controlled_file ADD COLUMN product_catalog_id BIGINT NULL;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='dcc_controlled_file' AND COLUMN_NAME='product_relation_id') THEN
    ALTER TABLE dcc_controlled_file ADD COLUMN product_relation_id BIGINT NULL;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='dcc_controlled_file' AND COLUMN_NAME='product_create_request_id') THEN
    ALTER TABLE dcc_controlled_file ADD COLUMN product_create_request_id BIGINT NULL;
  END IF;
END$$
DELIMITER ;
CALL dcc_product_identity_source();
DROP PROCEDURE dcc_product_identity_source;
