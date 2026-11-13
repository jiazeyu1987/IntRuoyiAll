-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20261112_tenant_file_configuration; type=schema; riskLevel=high
-- Populate verified ownership evidence before running this migration. No guessed/default tenant.
-- Coordinate with application cutover: legacy writers must be stopped before ownership validation.
SET NAMES utf8mb4;
CREATE TABLE IF NOT EXISTS infra_file_tenant_owner (
 file_id bigint NOT NULL PRIMARY KEY,
 tenant_id bigint NOT NULL,
 evidence varchar(1024) NOT NULL
) ENGINE=InnoDB;
CREATE TABLE IF NOT EXISTS infra_file_tenant_migration (
 id int NOT NULL PRIMARY KEY,
 completed_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;
DROP PROCEDURE IF EXISTS migrate_tenant_file_ownership;
DELIMITER $$
CREATE PROCEDURE migrate_tenant_file_ownership()
main: BEGIN
 DECLARE EXIT HANDLER FOR SQLEXCEPTION BEGIN ROLLBACK; RESIGNAL; END;
 IF EXISTS (SELECT 1 FROM infra_file_tenant_migration WHERE id=1) THEN
  LEAVE main;
 END IF;
 IF EXISTS (SELECT 1 FROM infra_file f LEFT JOIN infra_file_tenant_owner o ON o.file_id=f.id
    LEFT JOIN system_tenant t ON t.id=o.tenant_id
    WHERE o.file_id IS NULL OR t.id IS NULL OR CHAR_LENGTH(TRIM(o.evidence))=0) THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='TENANT_FILE_VERIFIED_OWNER_REQUIRED';
 END IF;
 IF EXISTS (SELECT 1 FROM infra_file f JOIN infra_file_tenant_owner o ON o.file_id=f.id
    LEFT JOIN infra_tenant_file_config c ON c.tenant_id=o.tenant_id AND c.id=f.config_id
    WHERE c.id IS NULL AND (f.config_id IS NULL OR f.config_id<>-1040750314)) THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='TENANT_FILE_CONFIG_REQUIRED';
 END IF;
 -- Shared physical keys must be copied and references separated before assigning ownership.
 IF EXISTS (SELECT 1 FROM infra_file f JOIN infra_file_tenant_owner o ON o.file_id=f.id
    GROUP BY f.config_id,BINARY f.path HAVING COUNT(DISTINCT o.tenant_id)>1) THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='TENANT_FILE_SHARED_OBJECT_REQUIRES_COPY';
 END IF;
 IF EXISTS (SELECT 1 FROM infra_file_content c WHERE NOT EXISTS (
    SELECT 1 FROM infra_file f WHERE f.config_id=c.config_id AND BINARY f.path=BINARY c.path)) THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='TENANT_FILE_CONTENT_OWNER_REQUIRED';
 END IF;
 IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE()
    AND table_name='infra_file' AND column_name='tenant_id') THEN
  ALTER TABLE infra_file ADD COLUMN tenant_id bigint NULL;
 END IF;
 IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE()
    AND table_name='infra_file_content' AND column_name='tenant_id') THEN
  ALTER TABLE infra_file_content ADD COLUMN tenant_id bigint NULL;
 END IF;
 START TRANSACTION;
 UPDATE infra_file f JOIN infra_file_tenant_owner o ON o.file_id=f.id
 SET f.tenant_id=o.tenant_id,f.update_time=f.update_time;
 UPDATE infra_file_content c JOIN infra_file f ON f.config_id=c.config_id AND BINARY f.path=BINARY c.path
 SET c.tenant_id=f.tenant_id,c.update_time=c.update_time;
 COMMIT;
 -- MySQL DDL commits independently; failed DDL must be repaired before application cutover.
 ALTER TABLE infra_file MODIFY COLUMN tenant_id bigint NOT NULL;
 ALTER TABLE infra_file_content MODIFY COLUMN tenant_id bigint NOT NULL;
 INSERT INTO infra_file_tenant_migration(id) VALUES(1);
END$$
DELIMITER ;
CALL migrate_tenant_file_ownership();
DROP PROCEDURE migrate_tenant_file_ownership;
