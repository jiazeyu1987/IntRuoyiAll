-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260708_mes_batch_record_version_phase_one; type=schema; riskLevel=medium
-- Purpose: repair eDHR process form permission rule indexes so route-versioned form rules can coexist with legacy null-version rules.

SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS ensure_mes_edhr_process_form_permission_rule_version_index;
DELIMITER //
CREATE PROCEDURE ensure_mes_edhr_process_form_permission_rule_version_index()
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM information_schema.tables
    WHERE table_schema = DATABASE()
      AND table_name = 'mes_pro_edhr_process_form_permission_rule'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'MES process form permission rule index repair requires rule table';
  END IF;

  IF NOT EXISTS (
    SELECT 1
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'mes_pro_edhr_process_form_permission_rule'
      AND column_name = 'batch_record_version_id'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'MES process form permission rule index repair requires batch_record_version_id';
  END IF;

  IF EXISTS (
    SELECT 1
    FROM `mes_pro_edhr_process_form_permission_rule`
    WHERE `batch_record_version_id` IS NOT NULL
    GROUP BY `tenant_id`, `route_process_id`, `batch_record_report_id`,
             `batch_record_version_id`, `rule_type`, `signature_cell_key`, `deleted`
    HAVING COUNT(*) > 1
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'MES process form permission rule index repair found duplicate versioned rules';
  END IF;

  SET @drop_old_unique := (
    SELECT IF(
      COUNT(*) > 0
      AND SUM(CASE WHEN column_name = 'batch_record_version_id' THEN 1 ELSE 0 END) = 0,
      'ALTER TABLE `mes_pro_edhr_process_form_permission_rule` DROP INDEX `uk_mes_pro_edhr_process_form_rule`',
      'SELECT 1'
    )
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'mes_pro_edhr_process_form_permission_rule'
      AND index_name = 'uk_mes_pro_edhr_process_form_rule'
  );
  PREPARE stmt FROM @drop_old_unique;
  EXECUTE stmt;
  DEALLOCATE PREPARE stmt;

  SET @add_version_unique := (
    SELECT IF(
      COUNT(*) = 0,
      'ALTER TABLE `mes_pro_edhr_process_form_permission_rule` ADD UNIQUE KEY `uk_mes_pro_edhr_process_form_rule` (`tenant_id`, `route_process_id`, `batch_record_report_id`, `batch_record_version_id`, `rule_type`, `signature_cell_key`, `deleted`)',
      'SELECT 1'
    )
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'mes_pro_edhr_process_form_permission_rule'
      AND index_name = 'uk_mes_pro_edhr_process_form_rule'
  );
  PREPARE stmt FROM @add_version_unique;
  EXECUTE stmt;
  DEALLOCATE PREPARE stmt;

  SET @drop_old_lookup := (
    SELECT IF(
      COUNT(*) > 0
      AND SUM(CASE WHEN column_name = 'batch_record_version_id' THEN 1 ELSE 0 END) = 0,
      'ALTER TABLE `mes_pro_edhr_process_form_permission_rule` DROP INDEX `idx_mes_pro_edhr_process_form_rule_route_report`',
      'SELECT 1'
    )
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'mes_pro_edhr_process_form_permission_rule'
      AND index_name = 'idx_mes_pro_edhr_process_form_rule_route_report'
  );
  PREPARE stmt FROM @drop_old_lookup;
  EXECUTE stmt;
  DEALLOCATE PREPARE stmt;

  SET @add_version_lookup := (
    SELECT IF(
      COUNT(*) = 0,
      'ALTER TABLE `mes_pro_edhr_process_form_permission_rule` ADD KEY `idx_mes_pro_edhr_process_form_rule_route_report` (`tenant_id`, `route_process_id`, `batch_record_report_id`, `batch_record_version_id`)',
      'SELECT 1'
    )
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'mes_pro_edhr_process_form_permission_rule'
      AND index_name = 'idx_mes_pro_edhr_process_form_rule_route_report'
  );
  PREPARE stmt FROM @add_version_lookup;
  EXECUTE stmt;
  DEALLOCATE PREPARE stmt;
END//
DELIMITER ;

CALL ensure_mes_edhr_process_form_permission_rule_version_index();

DROP PROCEDURE IF EXISTS ensure_mes_edhr_process_form_permission_rule_version_index;
