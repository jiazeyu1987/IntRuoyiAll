-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260829_mes_old_form_template_binding_switch; type=schema; riskLevel=low
-- Persist the DCC project code chosen during main batch-record Word import for form-center list display.

DROP PROCEDURE IF EXISTS ensure_mes_batch_record_report_project_code_column;
DELIMITER $$
CREATE PROCEDURE ensure_mes_batch_record_report_project_code_column()
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM information_schema.TABLES
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'mes_pro_batch_record_report'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'mes_pro_batch_record_report is missing';
  END IF;

  IF NOT EXISTS (
    SELECT 1
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'mes_pro_batch_record_report'
      AND COLUMN_NAME = 'project_code'
  ) THEN
    ALTER TABLE `mes_pro_batch_record_report`
      ADD COLUMN `project_code` varchar(64) DEFAULT NULL COMMENT 'DCC项目代码' AFTER `product_name`;
  END IF;
END$$
DELIMITER ;

CALL ensure_mes_batch_record_report_project_code_column();

DROP PROCEDURE IF EXISTS ensure_mes_batch_record_report_project_code_column;
