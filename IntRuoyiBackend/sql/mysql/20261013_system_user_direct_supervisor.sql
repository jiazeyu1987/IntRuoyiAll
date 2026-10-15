-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260830_system_user_lifecycle_deactivation; type=schema; riskLevel=medium
-- IC32: independently maintained supervisor relation; no historical supervisor inference or HR/department backfill.
-- Existing users begin unconfigured with relation revision 0. Existing columns/rows are not rewritten.
-- Rollback: export new relations before reverting application code; do not drop columns with saved relations.
SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;
DROP PROCEDURE IF EXISTS ensure_system_user_direct_supervisor;
DELIMITER //
CREATE PROCEDURE ensure_system_user_direct_supervisor()
BEGIN
  IF (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE()
      AND TABLE_NAME='system_users' AND ENGINE='InnoDB')<>1
     OR (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE()
      AND TABLE_NAME='system_users' AND COLUMN_NAME='id' AND DATA_TYPE='bigint' AND IS_NULLABLE='NO')<>1
     OR (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE()
      AND TABLE_NAME='system_users' AND COLUMN_NAME='tenant_id' AND DATA_TYPE='bigint' AND IS_NULLABLE='NO')<>1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='IC32 system user identity schema missing or drift';
  END IF;
  IF NOT EXISTS(SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE()
      AND TABLE_NAME='system_users' AND COLUMN_NAME='direct_supervisor_user_id') THEN
    ALTER TABLE system_users ADD COLUMN direct_supervisor_user_id BIGINT NULL DEFAULT NULL COMMENT '独立维护的直属主管账号';
  END IF;
  IF NOT EXISTS(SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE()
      AND TABLE_NAME='system_users' AND COLUMN_NAME='direct_supervisor_revision') THEN
    ALTER TABLE system_users ADD COLUMN direct_supervisor_revision BIGINT NOT NULL DEFAULT 0 COMMENT '直属主管关系服务端CAS版本';
  END IF;
  IF (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE()
      AND TABLE_NAME='system_users' AND COLUMN_NAME='direct_supervisor_user_id'
      AND LOWER(COLUMN_TYPE)='bigint' AND IS_NULLABLE='YES' AND COLUMN_DEFAULT IS NULL
      AND COALESCE(EXTRA,'')='' AND COALESCE(GENERATION_EXPRESSION,'')='')<>1
     OR (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE()
      AND TABLE_NAME='system_users' AND COLUMN_NAME='direct_supervisor_revision'
      AND LOWER(COLUMN_TYPE)='bigint' AND IS_NULLABLE='NO' AND COLUMN_DEFAULT='0'
      AND COALESCE(EXTRA,'')='' AND COALESCE(GENERATION_EXPRESSION,'')='')<>1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='IC32 direct supervisor column definition drift';
  END IF;
  IF EXISTS(SELECT 1 FROM system_users WHERE direct_supervisor_revision<0
      OR (direct_supervisor_user_id IS NOT NULL AND (direct_supervisor_user_id<=0 OR direct_supervisor_user_id=id))) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='IC32 illegal existing supervisor relation facts';
  END IF;
END//
DELIMITER ;
CALL ensure_system_user_direct_supervisor();
DROP PROCEDURE ensure_system_user_direct_supervisor;
