-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260707_mes_batch_record_extra_form_slots; type=schema; riskLevel=low

DROP PROCEDURE IF EXISTS intruoyi_add_mes_route_use_config_enabled;

DELIMITER //

CREATE PROCEDURE intruoyi_add_mes_route_use_config_enabled()
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'mes_pro_route_use_config'
      AND column_name = 'enabled'
  ) THEN
    ALTER TABLE `mes_pro_route_use_config`
      ADD COLUMN `enabled` bit(1) NOT NULL DEFAULT b'0' COMMENT '用途级启用状态' AFTER `use_type`;
  END IF;
END//

DELIMITER ;

CALL intruoyi_add_mes_route_use_config_enabled();

DROP PROCEDURE IF EXISTS intruoyi_add_mes_route_use_config_enabled;
