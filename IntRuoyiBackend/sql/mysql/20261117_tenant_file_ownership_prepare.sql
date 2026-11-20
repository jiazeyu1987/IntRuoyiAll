-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20261112_tenant_file_configuration; type=schema; riskLevel=high
-- Staged cutover: NULL means historical ownership is pending and tenant queries cannot read it.
-- Populate infra_file_tenant_owner only from verified evidence. Does not mark full migration complete.
SET NAMES utf8mb4;
CREATE TABLE IF NOT EXISTS infra_file_tenant_owner (
 file_id bigint NOT NULL PRIMARY KEY,
 tenant_id bigint NOT NULL,
 evidence varchar(1024) NOT NULL
) ENGINE=InnoDB;
DROP PROCEDURE IF EXISTS prepare_tenant_file_ownership;
DELIMITER $$
CREATE PROCEDURE prepare_tenant_file_ownership()
BEGIN
 DECLARE EXIT HANDLER FOR SQLEXCEPTION BEGIN ROLLBACK; RESIGNAL; END;
 IF EXISTS (SELECT 1 FROM infra_file_tenant_owner o
   LEFT JOIN infra_file f ON f.id=o.file_id LEFT JOIN system_tenant t ON t.id=o.tenant_id
   WHERE f.id IS NULL OR t.id IS NULL OR CHAR_LENGTH(TRIM(o.evidence))=0) THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='TENANT_FILE_VERIFIED_OWNER_REQUIRED';
 END IF;
 IF EXISTS (SELECT 1 FROM infra_file f JOIN infra_file_tenant_owner o ON o.file_id=f.id
   LEFT JOIN infra_tenant_file_config c ON c.tenant_id=o.tenant_id AND c.id=f.config_id
   WHERE c.id IS NULL AND (f.config_id IS NULL OR f.config_id<>-1040750314)) THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='TENANT_FILE_CONFIG_REQUIRED';
 END IF;
 -- Leave the entire physical-key group pending if any metadata row lacks evidence.
 IF EXISTS (SELECT 1 FROM infra_file f LEFT JOIN infra_file_tenant_owner o ON o.file_id=f.id
   GROUP BY f.config_id,BINARY f.path
   HAVING COUNT(o.file_id)>0 AND (COUNT(o.file_id)<>COUNT(*) OR COUNT(DISTINCT o.tenant_id)>1)) THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='TENANT_FILE_SHARED_OBJECT_REQUIRES_VERIFICATION';
 END IF;
 IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE()
   AND table_name='infra_file' AND column_name='tenant_id') THEN
  ALTER TABLE infra_file ADD COLUMN tenant_id bigint NULL;
 END IF;
 IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE()
   AND table_name='infra_file_content' AND column_name='tenant_id') THEN
  ALTER TABLE infra_file_content ADD COLUMN tenant_id bigint NULL;
 END IF;
 IF EXISTS (SELECT 1 FROM infra_file f JOIN infra_file_tenant_owner o ON o.file_id=f.id
   WHERE f.tenant_id IS NOT NULL AND f.tenant_id<>o.tenant_id)
 OR EXISTS (SELECT 1 FROM infra_file_content c JOIN infra_file f
   ON f.config_id=c.config_id AND BINARY f.path=BINARY c.path
   JOIN infra_file_tenant_owner o ON o.file_id=f.id
   WHERE c.tenant_id IS NOT NULL AND c.tenant_id<>o.tenant_id) THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='TENANT_FILE_EXISTING_OWNER_CONFLICT';
 END IF;
 START TRANSACTION;
 UPDATE infra_file f JOIN infra_file_tenant_owner o ON o.file_id=f.id
 SET f.tenant_id=o.tenant_id,f.update_time=f.update_time WHERE f.tenant_id IS NULL;
 UPDATE infra_file_content c JOIN infra_file f ON f.config_id=c.config_id AND BINARY f.path=BINARY c.path
 JOIN infra_file_tenant_owner o ON o.file_id=f.id
 SET c.tenant_id=o.tenant_id,c.update_time=c.update_time WHERE c.tenant_id IS NULL;
 COMMIT;
END$$
DELIMITER ;
CALL prepare_tenant_file_ownership();
DROP PROCEDURE prepare_tenant_file_ownership;
