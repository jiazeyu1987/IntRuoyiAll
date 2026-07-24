-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260715_mes_route_version_lifecycle; type=schema; riskLevel=low
-- Purpose: Flowable process instance IDs are strings, so MES route versions must not store them as bigint.

DROP PROCEDURE IF EXISTS ensure_mes_route_version_approval_instance_id_string;
DELIMITER $$
CREATE PROCEDURE ensure_mes_route_version_approval_instance_id_string()
BEGIN
  IF EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE()
       AND TABLE_NAME = 'mes_pro_route_version'
       AND COLUMN_NAME = 'approval_process_instance_id'
       AND (DATA_TYPE <> 'varchar' OR CHARACTER_MAXIMUM_LENGTH < 64)
  ) THEN
    ALTER TABLE `mes_pro_route_version`
      MODIFY COLUMN `approval_process_instance_id` varchar(64) NULL COMMENT '审批流程实例ID';
  END IF;
END $$
DELIMITER ;

CALL ensure_mes_route_version_approval_instance_id_string();
DROP PROCEDURE IF EXISTS ensure_mes_route_version_approval_instance_id_string;
