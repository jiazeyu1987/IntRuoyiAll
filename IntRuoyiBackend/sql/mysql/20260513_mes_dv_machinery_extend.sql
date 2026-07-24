-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=; type=schema; riskLevel=medium
DROP PROCEDURE IF EXISTS intruoyi_add_mes_dv_machinery_column;
DELIMITER $$
CREATE PROCEDURE intruoyi_add_mes_dv_machinery_column(
    IN p_column_name varchar(64),
    IN p_column_definition varchar(1000)
)
BEGIN
    IF NOT EXISTS (
        SELECT 1
          FROM information_schema.columns
         WHERE table_schema = DATABASE()
           AND table_name = 'mes_dv_machinery'
           AND column_name = p_column_name
    ) THEN
        SET @sql = CONCAT('ALTER TABLE `mes_dv_machinery` ADD COLUMN `', p_column_name, '` ', p_column_definition);
        PREPARE stmt FROM @sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END$$
DELIMITER ;

CALL intruoyi_add_mes_dv_machinery_column('process_name', 'varchar(255) DEFAULT NULL AFTER `workshop_id`');
CALL intruoyi_add_mes_dv_machinery_column('standard_hourly_capacity', 'decimal(18,6) DEFAULT NULL AFTER `process_name`');

DROP PROCEDURE IF EXISTS intruoyi_add_mes_dv_machinery_column;

CREATE TABLE IF NOT EXISTS `mes_dv_machinery_process` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `machinery_id` bigint DEFAULT NULL,
  `process_id` bigint DEFAULT NULL,
  `machinery_code` varchar(64) DEFAULT NULL,
  `line_name` varchar(255) DEFAULT NULL,
  `process_name` varchar(255) DEFAULT NULL,
  `device_name` varchar(255) DEFAULT NULL,
  `device_quantity` decimal(18,6) DEFAULT NULL,
  `ten_half_hour_daily_capacity` decimal(18,6) DEFAULT NULL,
  `standard_hourly_capacity` decimal(18,6) DEFAULT NULL,
  `source_row_no` int DEFAULT NULL,
  `remark` varchar(500) DEFAULT NULL,
  `creator` varchar(64) DEFAULT '',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updater` varchar(64) DEFAULT '',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bit(1) NOT NULL DEFAULT b'0',
  `tenant_id` bigint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_mes_dv_machinery_process_machinery_id` (`machinery_id`),
  KEY `idx_mes_dv_machinery_process_code` (`machinery_code`),
  KEY `idx_mes_dv_machinery_process_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MesDvMachineryProcessDO';
