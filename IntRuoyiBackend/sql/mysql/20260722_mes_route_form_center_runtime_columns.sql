-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260717_bpm_form_center,20260720_mes_batch_shared_form_binding; type=schema; riskLevel=medium
-- Optional local/E2E guard: set @mes_route_form_binding_target_tenant_id before sourcing to limit legacy data cleanup.
SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS ensure_mes_route_form_center_runtime_columns;
DELIMITER $$
CREATE PROCEDURE ensure_mes_route_form_center_runtime_columns()
BEGIN
  DECLARE v_target_tenant_id bigint DEFAULT NULL;
  SET v_target_tenant_id = @mes_route_form_binding_target_tenant_id;

  IF NOT EXISTS (
    SELECT 1
    FROM information_schema.TABLES
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'mes_pro_route_flow_process_batch_record'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'mes_pro_route_flow_process_batch_record is missing';
  END IF;

  IF NOT EXISTS (
    SELECT 1
    FROM information_schema.TABLES
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'mes_pro_edhr_batch_execution_task'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'mes_pro_edhr_batch_execution_task is missing';
  END IF;

  IF NOT EXISTS (
    SELECT 1
    FROM information_schema.TABLES
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'bpm_form_template_version'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'bpm_form_template_version is missing';
  END IF;

  ALTER TABLE `mes_pro_route_flow_process_batch_record`
    MODIFY COLUMN `batch_record_report_id` varchar(64) DEFAULT NULL COMMENT '历史字段：旧批记录报表 ID';

  ALTER TABLE `mes_pro_route_flow_process_batch_record`
    MODIFY COLUMN `form_slot_type` varchar(32) DEFAULT NULL COMMENT '历史字段：旧固定表单槽位类型';

  ALTER TABLE `mes_pro_edhr_batch_execution_task`
    MODIFY COLUMN `form_slot_type` varchar(32) DEFAULT NULL COMMENT '历史字段：旧固定表单槽位类型';

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'mes_pro_route_flow_process_batch_record'
      AND COLUMN_NAME = 'form_binding_key'
  ) THEN
    ALTER TABLE `mes_pro_route_flow_process_batch_record`
      ADD COLUMN `form_binding_key` varchar(128) DEFAULT NULL COMMENT '表单中心绑定键'
      AFTER `form_slot_type`;
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'mes_pro_route_flow_process_batch_record'
      AND COLUMN_NAME = 'form_template_id'
  ) THEN
    ALTER TABLE `mes_pro_route_flow_process_batch_record`
      ADD COLUMN `form_template_id` bigint DEFAULT NULL COMMENT '表单中心模板 ID'
      AFTER `form_binding_key`;
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'mes_pro_route_flow_process_batch_record'
      AND COLUMN_NAME = 'form_template_name_snapshot'
  ) THEN
    ALTER TABLE `mes_pro_route_flow_process_batch_record`
      ADD COLUMN `form_template_name_snapshot` varchar(128) DEFAULT NULL COMMENT '表单中心模板名称快照'
      AFTER `form_template_id`;
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'mes_pro_route_flow_process_batch_record'
      AND COLUMN_NAME = 'last_published_template_version_id'
  ) THEN
    ALTER TABLE `mes_pro_route_flow_process_batch_record`
      ADD COLUMN `last_published_template_version_id` bigint DEFAULT NULL COMMENT '最近已发布模板版本 ID'
      AFTER `form_template_name_snapshot`;
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'mes_pro_route_flow_process_batch_record'
      AND COLUMN_NAME = 'last_published_template_version_no'
  ) THEN
    ALTER TABLE `mes_pro_route_flow_process_batch_record`
      ADD COLUMN `last_published_template_version_no` varchar(64) DEFAULT NULL COMMENT '最近已发布模板版本号'
      AFTER `last_published_template_version_id`;
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'mes_pro_edhr_batch_execution_task'
      AND COLUMN_NAME = 'form_binding_key'
  ) THEN
    ALTER TABLE `mes_pro_edhr_batch_execution_task`
      ADD COLUMN `form_binding_key` varchar(128) DEFAULT NULL COMMENT '表单中心绑定键'
      AFTER `form_slot_type`;
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'mes_pro_edhr_batch_execution_task'
      AND COLUMN_NAME = 'form_template_id'
  ) THEN
    ALTER TABLE `mes_pro_edhr_batch_execution_task`
      ADD COLUMN `form_template_id` bigint DEFAULT NULL COMMENT '表单中心模板 ID'
      AFTER `form_binding_key`;
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'mes_pro_edhr_batch_execution_task'
      AND COLUMN_NAME = 'form_template_name_snapshot'
  ) THEN
    ALTER TABLE `mes_pro_edhr_batch_execution_task`
      ADD COLUMN `form_template_name_snapshot` varchar(128) DEFAULT NULL COMMENT '表单中心模板名称快照'
      AFTER `form_template_id`;
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'mes_pro_edhr_batch_execution_task'
      AND COLUMN_NAME = 'form_template_version_id'
  ) THEN
    ALTER TABLE `mes_pro_edhr_batch_execution_task`
      ADD COLUMN `form_template_version_id` bigint DEFAULT NULL COMMENT '表单中心模板版本 ID'
      AFTER `form_template_name_snapshot`;
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'mes_pro_edhr_batch_execution_task'
      AND COLUMN_NAME = 'form_template_version_no'
  ) THEN
    ALTER TABLE `mes_pro_edhr_batch_execution_task`
      ADD COLUMN `form_template_version_no` varchar(64) DEFAULT NULL COMMENT '表单中心模板版本号'
      AFTER `form_template_version_id`;
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'mes_pro_edhr_batch_execution_task'
      AND COLUMN_NAME = 'form_center_instance_id'
  ) THEN
    ALTER TABLE `mes_pro_edhr_batch_execution_task`
      ADD COLUMN `form_center_instance_id` bigint DEFAULT NULL COMMENT '表单中心实例 ID'
      AFTER `form_template_version_no`;
  END IF;

  UPDATE `mes_pro_route_flow_process_batch_record`
  SET `deleted` = b'1',
      `updater` = 'route-flow-dynamic-form-slots',
      `update_time` = NOW()
  WHERE `deleted` = b'0'
    AND `form_template_id` IS NULL
    AND `batch_record_report_id` IS NOT NULL
    AND `batch_record_report_id` <> ''
    AND (v_target_tenant_id IS NULL OR `tenant_id` = v_target_tenant_id);

  IF EXISTS (
    SELECT 1
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'mes_pro_route_flow_process_batch_record'
      AND INDEX_NAME = 'uk_mes_pro_route_flow_process_report'
  ) THEN
    ALTER TABLE `mes_pro_route_flow_process_batch_record`
      DROP INDEX `uk_mes_pro_route_flow_process_report`;
  END IF;

  IF EXISTS (
    SELECT 1
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'mes_pro_route_flow_process_batch_record'
      AND INDEX_NAME = 'uk_mes_pro_route_flow_process_batch_record'
  ) THEN
    ALTER TABLE `mes_pro_route_flow_process_batch_record`
      DROP INDEX `uk_mes_pro_route_flow_process_batch_record`;
  END IF;

  IF NOT EXISTS (
    SELECT 1
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'mes_pro_route_flow_process_batch_record'
      AND INDEX_NAME = 'uk_mes_route_flow_process_form_binding_key'
  ) THEN
    ALTER TABLE `mes_pro_route_flow_process_batch_record`
      ADD UNIQUE KEY `uk_mes_route_flow_process_form_binding_key`
        (`tenant_id`, `route_process_id`, `use_type`, `form_binding_key`, `deleted`);
  END IF;

  IF NOT EXISTS (
    SELECT 1
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'mes_pro_route_flow_process_batch_record'
      AND INDEX_NAME = 'uk_mes_route_flow_process_form_template'
  ) THEN
    ALTER TABLE `mes_pro_route_flow_process_batch_record`
      ADD UNIQUE KEY `uk_mes_route_flow_process_form_template`
        (`tenant_id`, `route_process_id`, `use_type`, `form_template_id`, `deleted`);
  END IF;

  IF NOT EXISTS (
    SELECT 1
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'mes_pro_edhr_batch_execution_task'
      AND INDEX_NAME = 'idx_mes_edhr_batch_task_form_instance'
  ) THEN
    ALTER TABLE `mes_pro_edhr_batch_execution_task`
      ADD INDEX `idx_mes_edhr_batch_task_form_instance`
        (`tenant_id`, `form_center_instance_id`, `deleted`);
  END IF;
END$$
DELIMITER ;

CALL ensure_mes_route_form_center_runtime_columns();
DROP PROCEDURE IF EXISTS ensure_mes_route_form_center_runtime_columns;
