-- release-migration: allowedEnvironments=test; dependsOn=; type=schema; riskLevel=medium
-- LOCAL REPAIR ONLY. Excluded from application releases; not a tenant isolation migration.
-- Operator must verify the running shared FileDO/FileContentDO model and set
-- @confirm_local_shared_file_schema = 'LOCAL_SHARED_FILE_MODEL_20261123'.
-- Preserve every existing tenant value, business reference, index and completion marker.
-- MySQL DDL commits independently. If the second ALTER fails, inspect both columns before retry.
-- Recovery cannot blindly restore NOT NULL after new NULL rows exist; never assign a guessed owner.
SET NAMES utf8mb4;
DROP PROCEDURE IF EXISTS repair_local_shared_file_schema;
DELIMITER $$
CREATE PROCEDURE repair_local_shared_file_schema()
BEGIN
  IF DATABASE() <> 'ruoyi-vue-pro'
     OR COALESCE(@confirm_local_shared_file_schema, '') <> 'LOCAL_SHARED_FILE_MODEL_20261123' THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='LOCAL_SHARED_FILE_MODEL_CONFIRMATION_REQUIRED';
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema=DATABASE()
      AND table_name='infra_file_tenant_migration') THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='EXPECTED_LOCAL_LEGACY_FILE_SCHEMA_REQUIRED';
  END IF;
  IF NOT EXISTS (SELECT 1 FROM infra_file_tenant_migration WHERE id=1
      AND completed_at='2026-11-14 20:06:35') THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='EXPECTED_LOCAL_LEGACY_FILE_MARKER_REQUIRED';
  END IF;
  IF (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE()
      AND table_name IN ('infra_file','infra_file_content') AND column_name='tenant_id'
      AND column_type='bigint' AND column_default IS NULL AND column_comment=''
      AND extra='' AND is_nullable IN ('YES','NO')) <> 2 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='UNEXPECTED_FILE_TENANT_COLUMN_CONTRACT';
  END IF;
  IF EXISTS (SELECT 1 FROM information_schema.statistics WHERE table_schema=DATABASE()
      AND table_name IN ('infra_file','infra_file_content') AND column_name='tenant_id')
     OR EXISTS (SELECT 1 FROM information_schema.key_column_usage WHERE table_schema=DATABASE()
      AND table_name IN ('infra_file','infra_file_content') AND column_name='tenant_id') THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='FILE_TENANT_COLUMN_DEPENDENCY_REVIEW_REQUIRED';
  END IF;
  IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE()
      AND table_name='infra_file' AND column_name='tenant_id' AND is_nullable='NO') THEN
    ALTER TABLE infra_file MODIFY COLUMN tenant_id bigint NULL DEFAULT NULL;
  END IF;
  IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE()
      AND table_name='infra_file_content' AND column_name='tenant_id' AND is_nullable='NO') THEN
    ALTER TABLE infra_file_content MODIFY COLUMN tenant_id bigint NULL DEFAULT NULL;
  END IF;
END$$
DELIMITER ;
CALL repair_local_shared_file_schema();
DROP PROCEDURE repair_local_shared_file_schema;
