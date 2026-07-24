-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=; type=schema; riskLevel=medium
DROP PROCEDURE IF EXISTS intruoyi_add_mes_md_workstation_shift_hours;
DELIMITER $$
CREATE PROCEDURE intruoyi_add_mes_md_workstation_shift_hours()
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'mes_md_workstation'
      AND column_name = 'shift_hours'
  ) THEN
    ALTER TABLE `mes_md_workstation`
      ADD COLUMN `shift_hours` decimal(10,2) NULL COMMENT '班次小时数' AFTER `single_standard_hourly_capacity`;
  END IF;
END$$
DELIMITER ;

CALL intruoyi_add_mes_md_workstation_shift_hours();

DROP PROCEDURE IF EXISTS intruoyi_add_mes_md_workstation_shift_hours;
