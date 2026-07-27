-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260717_bpm_form_center; type=schema; riskLevel=medium
SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS ensure_bpm_form_template_batch_record_binding;
DELIMITER //
CREATE PROCEDURE ensure_bpm_form_template_batch_record_binding()
BEGIN
  IF NOT EXISTS (
    SELECT 1
      FROM information_schema.TABLES
     WHERE TABLE_SCHEMA = DATABASE()
       AND TABLE_NAME = 'bpm_form_template_version'
  ) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'bpm_form_template_version is missing';
  END IF;

  IF NOT EXISTS (
    SELECT 1
      FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE()
       AND TABLE_NAME = 'bpm_form_template_version'
       AND COLUMN_NAME = 'batch_record_report_id'
  ) THEN
    ALTER TABLE `bpm_form_template_version`
      ADD COLUMN `batch_record_report_id` varchar(64) DEFAULT NULL COMMENT '绑定批记录报表 ID' AFTER `jimu_schema_json`;
  END IF;

  IF NOT EXISTS (
    SELECT 1
      FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE()
       AND TABLE_NAME = 'bpm_form_template_version'
       AND COLUMN_NAME = 'batch_record_report_name'
  ) THEN
    ALTER TABLE `bpm_form_template_version`
      ADD COLUMN `batch_record_report_name` varchar(255) DEFAULT NULL COMMENT '绑定批记录报表名称' AFTER `batch_record_report_id`;
  END IF;

  IF NOT EXISTS (
    SELECT 1
      FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE()
       AND TABLE_NAME = 'bpm_form_template_version'
       AND COLUMN_NAME = 'batch_record_name'
  ) THEN
    ALTER TABLE `bpm_form_template_version`
      ADD COLUMN `batch_record_name` varchar(255) DEFAULT NULL COMMENT '绑定批记录名称' AFTER `batch_record_report_name`;
  END IF;

  IF NOT EXISTS (
    SELECT 1
      FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE()
       AND TABLE_NAME = 'bpm_form_template_version'
       AND COLUMN_NAME = 'batch_record_version_no'
  ) THEN
    ALTER TABLE `bpm_form_template_version`
      ADD COLUMN `batch_record_version_no` varchar(64) DEFAULT NULL COMMENT '绑定批记录版本号' AFTER `batch_record_name`;
  END IF;

  IF NOT EXISTS (
    SELECT 1
      FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE()
       AND TABLE_NAME = 'bpm_form_template_version'
       AND COLUMN_NAME = 'batch_record_form_slot_type'
  ) THEN
    ALTER TABLE `bpm_form_template_version`
      ADD COLUMN `batch_record_form_slot_type` varchar(32) DEFAULT NULL COMMENT '绑定批记录表单槽位类型' AFTER `batch_record_version_no`;
  END IF;

  IF NOT EXISTS (
    SELECT 1
      FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE()
       AND TABLE_NAME = 'bpm_form_template_version'
       AND COLUMN_NAME = 'batch_record_binding_status'
  ) THEN
    ALTER TABLE `bpm_form_template_version`
      ADD COLUMN `batch_record_binding_status` varchar(32) DEFAULT NULL COMMENT '绑定状态：BOUND/UNBOUND/BROKEN' AFTER `batch_record_form_slot_type`;
  END IF;

  IF NOT EXISTS (
    SELECT 1
      FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE()
       AND TABLE_NAME = 'bpm_form_template_version'
       AND COLUMN_NAME = 'batch_record_binding_error'
  ) THEN
    ALTER TABLE `bpm_form_template_version`
      ADD COLUMN `batch_record_binding_error` varchar(500) DEFAULT NULL COMMENT '绑定异常说明' AFTER `batch_record_binding_status`;
  END IF;

  IF NOT EXISTS (
    SELECT 1
      FROM information_schema.STATISTICS
     WHERE TABLE_SCHEMA = DATABASE()
       AND TABLE_NAME = 'bpm_form_template_version'
       AND INDEX_NAME = 'idx_bpm_form_template_batch_record_report'
  ) THEN
    ALTER TABLE `bpm_form_template_version`
      ADD KEY `idx_bpm_form_template_batch_record_report` (`tenant_id`, `batch_record_report_id`, `deleted`);
  END IF;
END//
DELIMITER ;

CALL ensure_bpm_form_template_batch_record_binding();

DROP PROCEDURE IF EXISTS ensure_bpm_form_template_batch_record_binding;
