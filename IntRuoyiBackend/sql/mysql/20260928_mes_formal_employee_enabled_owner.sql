-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260731_mes_process_pool_team_leader_p1_runtime_config; type=schema; riskLevel=medium
-- One enabled formal system user per tenant; disabled/deleted employee profiles remain as history.
-- Existing ownership conflicts must be resolved explicitly by the business owner before deployment.
DROP PROCEDURE IF EXISTS upgrade_mes_formal_employee_enabled_owner;
DELIMITER $$
CREATE PROCEDURE upgrade_mes_formal_employee_enabled_owner()
BEGIN
  IF NOT EXISTS (SELECT 1 FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE()
      AND TABLE_NAME='mes_pro_process_pool_team_employee_profile') THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Missing team employee profile prerequisite table';
  END IF;
  IF EXISTS (SELECT 1 FROM mes_pro_process_pool_team_employee_profile
      WHERE deleted=0 AND enabled=1 AND system_user_id IS NOT NULL
      GROUP BY tenant_id, system_user_id HAVING COUNT(*) > 1) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Multiple enabled owners exist; disable old team profiles explicitly before migration';
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE()
      AND TABLE_NAME='mes_pro_process_pool_team_employee_profile' AND COLUMN_NAME='enabled_formal_user_id') THEN
    ALTER TABLE mes_pro_process_pool_team_employee_profile
      ADD COLUMN enabled_formal_user_id BIGINT GENERATED ALWAYS AS
        (CASE WHEN deleted=0 AND enabled=1 AND system_user_id IS NOT NULL
         THEN system_user_id ELSE NULL END) STORED;
  END IF;
  IF (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE()
      AND TABLE_NAME='mes_pro_process_pool_team_employee_profile' AND COLUMN_NAME='enabled_formal_user_id'
      AND DATA_TYPE='bigint' AND EXTRA LIKE '%STORED GENERATED%'
      AND REPLACE(REPLACE(REPLACE(REPLACE(LOWER(GENERATION_EXPRESSION), '`', ''), ' ', ''), '(', ''), ')', '')
          ='casewhendeleted=0andenabled=1andsystem_user_idisnotnullthensystem_user_idelsenullend') <> 1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Employee enabled owner generated column is incompatible';
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.STATISTICS WHERE TABLE_SCHEMA=DATABASE()
      AND TABLE_NAME='mes_pro_process_pool_team_employee_profile' AND INDEX_NAME='uk_mes_pp_employee_enabled_user') THEN
    ALTER TABLE mes_pro_process_pool_team_employee_profile
      ADD UNIQUE KEY uk_mes_pp_employee_enabled_user (tenant_id, enabled_formal_user_id);
  END IF;
  IF (SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA=DATABASE()
      AND TABLE_NAME='mes_pro_process_pool_team_employee_profile' AND INDEX_NAME='uk_mes_pp_employee_enabled_user') <> 2
      OR (SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA=DATABASE()
      AND TABLE_NAME='mes_pro_process_pool_team_employee_profile' AND INDEX_NAME='uk_mes_pp_employee_enabled_user'
      AND NON_UNIQUE=0 AND SUB_PART IS NULL
      AND ((SEQ_IN_INDEX=1 AND COLUMN_NAME='tenant_id') OR (SEQ_IN_INDEX=2 AND COLUMN_NAME='enabled_formal_user_id'))) <> 2 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Employee enabled owner unique index is incompatible';
  END IF;
END$$
DELIMITER ;
CALL upgrade_mes_formal_employee_enabled_owner();
DROP PROCEDURE upgrade_mes_formal_employee_enabled_owner;
