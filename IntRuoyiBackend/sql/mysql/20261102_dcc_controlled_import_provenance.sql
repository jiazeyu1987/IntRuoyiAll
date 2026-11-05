-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260718_controlled_content_lifecycle; type=schema; riskLevel=medium
-- Deploy before starting code that maps these DCC columns. Existing files retain NULL provenance.
SET NAMES utf8mb4;
DROP PROCEDURE IF EXISTS migrate_dcc_controlled_import_provenance_20261102;
DELIMITER $$
CREATE PROCEDURE migrate_dcc_controlled_import_provenance_20261102()
BEGIN
 IF NOT EXISTS(SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='dcc_controlled_file' AND COLUMN_NAME='import_source') THEN
  ALTER TABLE dcc_controlled_file ADD COLUMN import_source VARCHAR(32) NULL COMMENT 'CONTROLLED_IMPORT: admission of an already controlled source', ALGORITHM=INSTANT;
 END IF;
 IF NOT EXISTS(SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='dcc_controlled_file' AND COLUMN_NAME='imported_by') THEN
  ALTER TABLE dcc_controlled_file ADD COLUMN imported_by BIGINT NULL COMMENT 'Actual importing user, independent of original control', ALGORITHM=INSTANT;
 END IF;
 IF NOT EXISTS(SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='dcc_controlled_file' AND COLUMN_NAME='imported_time') THEN
  ALTER TABLE dcc_controlled_file ADD COLUMN imported_time DATETIME NULL COMMENT 'Import business-zone time, independent of original control/effect', ALGORITHM=INSTANT;
 END IF;
END$$
DELIMITER ;
CALL migrate_dcc_controlled_import_provenance_20261102();
DROP PROCEDURE migrate_dcc_controlled_import_provenance_20261102;
