-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=; type=schema; riskLevel=medium
DROP PROCEDURE IF EXISTS ensure_mes_batch_record_report_name;
DELIMITER $$
CREATE PROCEDURE ensure_mes_batch_record_report_name()
BEGIN
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'mes_pro_batch_record_report' AND COLUMN_NAME = 'batch_record_name') THEN
    ALTER TABLE `mes_pro_batch_record_report`
      ADD COLUMN `batch_record_name` varchar(100) NOT NULL DEFAULT '棘突球囊' COMMENT '批记录名称' AFTER `sample_key`;
  END IF;

  UPDATE `mes_pro_batch_record_report`
  SET `batch_record_name` = '棘突球囊'
  WHERE `batch_record_name` IS NULL OR `batch_record_name` = '';
END$$
DELIMITER ;

CALL ensure_mes_batch_record_report_name();

DROP PROCEDURE IF EXISTS ensure_mes_batch_record_report_name;
