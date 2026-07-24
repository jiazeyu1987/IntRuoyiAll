-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260514_mes_batch_record_report,20260612_mes_edhr_multi_batch_route; type=schema; riskLevel=medium
DROP PROCEDURE IF EXISTS ensure_mes_batch_record_extra_form_slots;
DELIMITER $$
CREATE PROCEDURE ensure_mes_batch_record_extra_form_slots()
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'mes_pro_batch_record_report'
      AND COLUMN_NAME = 'form_slot_type'
  ) THEN
    ALTER TABLE `mes_pro_batch_record_report`
      ADD COLUMN `form_slot_type` varchar(32) NOT NULL DEFAULT 'MAIN'
        COMMENT '表单槽位类型：MAIN/LOSS_REPORT/PROCESS_INSPECTION/PARAMETER_RECORD'
        AFTER `batch_record_name`;
  END IF;

  UPDATE `mes_pro_batch_record_report`
  SET `form_slot_type` = 'MAIN'
  WHERE `form_slot_type` IS NULL OR `form_slot_type` COLLATE utf8mb4_bin = '' COLLATE utf8mb4_bin;

  IF EXISTS (
    SELECT 1
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'mes_pro_batch_record_report'
      AND INDEX_NAME = 'uk_mes_batch_record_report_sample_route_table'
  ) THEN
    ALTER TABLE `mes_pro_batch_record_report`
      DROP INDEX `uk_mes_batch_record_report_sample_route_table`;
  END IF;

  IF NOT EXISTS (
    SELECT 1
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'mes_pro_batch_record_report'
      AND INDEX_NAME = 'uk_mes_batch_record_report_sample_route_table'
  ) THEN
    ALTER TABLE `mes_pro_batch_record_report`
      ADD UNIQUE KEY `uk_mes_batch_record_report_sample_route_table`
        (`sample_key`, `form_slot_type`, `route_key`, `source_table_index`);
  END IF;

  IF NOT EXISTS (
    SELECT 1
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'mes_pro_route_use_process_batch_record'
      AND COLUMN_NAME = 'form_slot_type'
  ) THEN
    ALTER TABLE `mes_pro_route_use_process_batch_record`
      ADD COLUMN `form_slot_type` varchar(32) NOT NULL DEFAULT 'MAIN'
        COMMENT '表单槽位类型：MAIN/LOSS_REPORT/PROCESS_INSPECTION/PARAMETER_RECORD'
        AFTER `batch_record_report_id`;
  END IF;

  IF NOT EXISTS (
    SELECT 1
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'mes_pro_route_use_process_batch_record'
      AND COLUMN_NAME = 'record_category'
  ) THEN
    ALTER TABLE `mes_pro_route_use_process_batch_record`
      ADD COLUMN `record_category` varchar(32) NOT NULL DEFAULT 'BATCH_RECORD'
        COMMENT '记录分类：BATCH_RECORD/QUALITY_RECORD/EQUIPMENT_RECORD'
        AFTER `form_slot_type`;
  END IF;

  IF NOT EXISTS (
    SELECT 1
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'mes_pro_route_use_process_batch_record'
      AND COLUMN_NAME = 'validation_profile'
  ) THEN
    ALTER TABLE `mes_pro_route_use_process_batch_record`
      ADD COLUMN `validation_profile` varchar(32) NOT NULL DEFAULT 'CONTROLLED_BATCH'
        COMMENT '校验档案：CONTROLLED_BATCH/QUALITY_PROCESS/EQUIPMENT_PARAMETER'
        AFTER `record_category`;
  END IF;

  IF NOT EXISTS (
    SELECT 1
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'mes_pro_route_use_process_batch_record'
      AND COLUMN_NAME = 'permission_scope_id'
  ) THEN
    ALTER TABLE `mes_pro_route_use_process_batch_record`
      ADD COLUMN `permission_scope_id` bigint DEFAULT NULL
        COMMENT 'eDHR权限范围ID'
        AFTER `validation_profile`;
  END IF;

  IF NOT EXISTS (
    SELECT 1
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'mes_pro_route_use_process_batch_record'
      AND COLUMN_NAME = 'record_category_snapshot_hash'
  ) THEN
    ALTER TABLE `mes_pro_route_use_process_batch_record`
      ADD COLUMN `record_category_snapshot_hash` char(64) DEFAULT NULL
        COMMENT '记录分类配置快照哈希'
        AFTER `permission_scope_id`;
  END IF;

  IF NOT EXISTS (
    SELECT 1
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'mes_pro_route_use_process_batch_record'
      AND COLUMN_NAME = 'required_policy'
  ) THEN
    ALTER TABLE `mes_pro_route_use_process_batch_record`
      ADD COLUMN `required_policy` varchar(32) NOT NULL DEFAULT 'REQUIRED'
        COMMENT '必填策略：REQUIRED/CONDITIONAL/OPTIONAL'
        AFTER `record_category_snapshot_hash`;
  END IF;

  IF NOT EXISTS (
    SELECT 1
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'mes_pro_route_use_process_batch_record'
      AND COLUMN_NAME = 'required_condition_json'
  ) THEN
    ALTER TABLE `mes_pro_route_use_process_batch_record`
      ADD COLUMN `required_condition_json` json DEFAULT NULL
        COMMENT '条件必填规则JSON'
        AFTER `required_policy`;
  END IF;

  IF NOT EXISTS (
    SELECT 1
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'mes_pro_route_use_process_batch_record'
      AND COLUMN_NAME = 'owner_role_key'
  ) THEN
    ALTER TABLE `mes_pro_route_use_process_batch_record`
      ADD COLUMN `owner_role_key` varchar(32) NOT NULL DEFAULT 'PRODUCTION'
        COMMENT '表单责任角色：PRODUCTION/QUALITY/EQUIPMENT'
        AFTER `required_condition_json`;
  END IF;

  IF NOT EXISTS (
    SELECT 1
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'mes_pro_route_use_process_batch_record'
      AND COLUMN_NAME = 'archive_visibility'
  ) THEN
    ALTER TABLE `mes_pro_route_use_process_batch_record`
      ADD COLUMN `archive_visibility` varchar(32) NOT NULL DEFAULT 'FINAL_DHR'
        COMMENT '归档可见性：FINAL_DHR/INTERNAL_REVIEW/AUDIT_ONLY/ATTACHMENT_REFERENCE'
        AFTER `owner_role_key`;
  END IF;

  UPDATE `mes_pro_route_use_process_batch_record`
  SET `archive_visibility` = CASE `archive_visibility` COLLATE utf8mb4_bin
    WHEN 'DOSSIER' COLLATE utf8mb4_bin THEN 'FINAL_DHR'
    WHEN 'CONTROLLED' COLLATE utf8mb4_bin THEN 'FINAL_DHR'
    WHEN 'INTERNAL' COLLATE utf8mb4_bin THEN 'INTERNAL_REVIEW'
    ELSE `archive_visibility`
  END
  WHERE `archive_visibility` COLLATE utf8mb4_bin IN ('DOSSIER' COLLATE utf8mb4_bin, 'CONTROLLED' COLLATE utf8mb4_bin, 'INTERNAL' COLLATE utf8mb4_bin);

  IF NOT EXISTS (
    SELECT 1
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'mes_pro_route_use_process_batch_record'
      AND COLUMN_NAME = 'slot_config_snapshot_hash'
  ) THEN
    ALTER TABLE `mes_pro_route_use_process_batch_record`
      ADD COLUMN `slot_config_snapshot_hash` char(64) DEFAULT NULL
        COMMENT '表单槽位配置快照哈希'
        AFTER `archive_visibility`;
  END IF;

  UPDATE `mes_pro_route_use_process_batch_record` br
  LEFT JOIN `mes_pro_batch_record_report` r
    ON r.`report_id` COLLATE utf8mb4_bin = br.`batch_record_report_id` COLLATE utf8mb4_bin
   AND r.`tenant_id` = br.`tenant_id`
   AND r.`deleted` = b'0'
  SET br.`form_slot_type` = COALESCE(NULLIF(r.`form_slot_type` COLLATE utf8mb4_bin, '' COLLATE utf8mb4_bin), 'MAIN')
  WHERE br.`form_slot_type` IS NULL OR br.`form_slot_type` COLLATE utf8mb4_bin = '' COLLATE utf8mb4_bin;

  IF NOT EXISTS (
    SELECT 1
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'mes_pro_batch_record_execution'
      AND COLUMN_NAME = 'form_slot_type'
  ) THEN
    ALTER TABLE `mes_pro_batch_record_execution`
      ADD COLUMN `form_slot_type` varchar(32) NOT NULL DEFAULT 'MAIN'
        COMMENT '表单槽位类型：MAIN/LOSS_REPORT/PROCESS_INSPECTION/PARAMETER_RECORD'
        AFTER `batch_record_report_id`;
  END IF;

  IF NOT EXISTS (
    SELECT 1
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'mes_pro_batch_record_execution'
      AND COLUMN_NAME = 'record_category'
  ) THEN
    ALTER TABLE `mes_pro_batch_record_execution`
      ADD COLUMN `record_category` varchar(32) NOT NULL DEFAULT 'BATCH_RECORD'
        COMMENT '记录分类：BATCH_RECORD/QUALITY_RECORD/EQUIPMENT_RECORD'
        AFTER `form_slot_type`;
  END IF;

  IF NOT EXISTS (
    SELECT 1
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'mes_pro_batch_record_execution'
      AND COLUMN_NAME = 'validation_profile'
  ) THEN
    ALTER TABLE `mes_pro_batch_record_execution`
      ADD COLUMN `validation_profile` varchar(32) NOT NULL DEFAULT 'CONTROLLED_BATCH'
        COMMENT '校验档案：CONTROLLED_BATCH/QUALITY_PROCESS/EQUIPMENT_PARAMETER'
        AFTER `record_category`;
  END IF;

  IF NOT EXISTS (
    SELECT 1
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'mes_pro_batch_record_execution'
      AND COLUMN_NAME = 'permission_scope_id'
  ) THEN
    ALTER TABLE `mes_pro_batch_record_execution`
      ADD COLUMN `permission_scope_id` bigint DEFAULT NULL
        COMMENT 'eDHR权限范围ID'
        AFTER `validation_profile`;
  END IF;

  IF NOT EXISTS (
    SELECT 1
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'mes_pro_batch_record_execution'
      AND COLUMN_NAME = 'route_binding_id'
  ) THEN
    ALTER TABLE `mes_pro_batch_record_execution`
      ADD COLUMN `route_binding_id` bigint DEFAULT NULL
        COMMENT '路线工序附属表单绑定ID'
        AFTER `permission_scope_id`;
  END IF;

  IF NOT EXISTS (
    SELECT 1
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'mes_pro_batch_record_execution'
      AND COLUMN_NAME = 'route_binding_snapshot_hash'
  ) THEN
    ALTER TABLE `mes_pro_batch_record_execution`
      ADD COLUMN `route_binding_snapshot_hash` char(64) DEFAULT NULL
        COMMENT '路线工序附属表单绑定快照哈希'
        AFTER `route_binding_id`;
  END IF;

  IF NOT EXISTS (
    SELECT 1
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'mes_pro_batch_record_execution'
      AND COLUMN_NAME = 'archive_visibility'
  ) THEN
    ALTER TABLE `mes_pro_batch_record_execution`
      ADD COLUMN `archive_visibility` varchar(32) NOT NULL DEFAULT 'FINAL_DHR'
        COMMENT '归档可见性：FINAL_DHR/INTERNAL_REVIEW/AUDIT_ONLY/ATTACHMENT_REFERENCE'
        AFTER `route_binding_snapshot_hash`;
  END IF;

  UPDATE `mes_pro_batch_record_execution`
  SET `archive_visibility` = CASE `archive_visibility` COLLATE utf8mb4_bin
    WHEN 'DOSSIER' COLLATE utf8mb4_bin THEN 'FINAL_DHR'
    WHEN 'CONTROLLED' COLLATE utf8mb4_bin THEN 'FINAL_DHR'
    WHEN 'INTERNAL' COLLATE utf8mb4_bin THEN 'INTERNAL_REVIEW'
    ELSE `archive_visibility`
  END
  WHERE `archive_visibility` COLLATE utf8mb4_bin IN ('DOSSIER' COLLATE utf8mb4_bin, 'CONTROLLED' COLLATE utf8mb4_bin, 'INTERNAL' COLLATE utf8mb4_bin);

  IF NOT EXISTS (
    SELECT 1
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'mes_pro_batch_record_execution'
      AND COLUMN_NAME = 'slot_config_snapshot_hash'
  ) THEN
    ALTER TABLE `mes_pro_batch_record_execution`
      ADD COLUMN `slot_config_snapshot_hash` char(64) DEFAULT NULL
        COMMENT '表单槽位配置快照哈希'
        AFTER `archive_visibility`;
  END IF;

  IF NOT EXISTS (
    SELECT 1
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'mes_pro_edhr_batch_execution_task'
      AND COLUMN_NAME = 'form_slot_type'
  ) THEN
    ALTER TABLE `mes_pro_edhr_batch_execution_task`
      ADD COLUMN `form_slot_type` varchar(32) NOT NULL DEFAULT 'MAIN'
        COMMENT '表单槽位类型：MAIN/LOSS_REPORT/PROCESS_INSPECTION/PARAMETER_RECORD'
        AFTER `batch_record_report_name`;
  END IF;

  IF NOT EXISTS (
    SELECT 1
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'mes_pro_edhr_batch_execution_task'
      AND COLUMN_NAME = 'record_category'
  ) THEN
    ALTER TABLE `mes_pro_edhr_batch_execution_task`
      ADD COLUMN `record_category` varchar(32) NOT NULL DEFAULT 'BATCH_RECORD'
        COMMENT '记录分类：BATCH_RECORD/QUALITY_RECORD/EQUIPMENT_RECORD'
        AFTER `execution_mode`;
  END IF;

  IF NOT EXISTS (
    SELECT 1
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'mes_pro_edhr_batch_execution_task'
      AND COLUMN_NAME = 'validation_profile'
  ) THEN
    ALTER TABLE `mes_pro_edhr_batch_execution_task`
      ADD COLUMN `validation_profile` varchar(32) NOT NULL DEFAULT 'CONTROLLED_BATCH'
        COMMENT '校验档案：CONTROLLED_BATCH/QUALITY_PROCESS/EQUIPMENT_PARAMETER'
        AFTER `record_category`;
  END IF;

  IF NOT EXISTS (
    SELECT 1
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'mes_pro_edhr_batch_execution_task'
      AND COLUMN_NAME = 'permission_scope_id'
  ) THEN
    ALTER TABLE `mes_pro_edhr_batch_execution_task`
      ADD COLUMN `permission_scope_id` bigint DEFAULT NULL
        COMMENT 'eDHR权限范围ID'
        AFTER `validation_profile`;
  END IF;

  IF NOT EXISTS (
    SELECT 1
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'mes_pro_edhr_batch_execution_task'
      AND COLUMN_NAME = 'route_binding_id'
  ) THEN
    ALTER TABLE `mes_pro_edhr_batch_execution_task`
      ADD COLUMN `route_binding_id` bigint DEFAULT NULL
        COMMENT '路线工序附属表单绑定ID'
        AFTER `permission_scope_id`;
  END IF;

  IF NOT EXISTS (
    SELECT 1
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'mes_pro_edhr_batch_execution_task'
      AND COLUMN_NAME = 'route_binding_snapshot_hash'
  ) THEN
    ALTER TABLE `mes_pro_edhr_batch_execution_task`
      ADD COLUMN `route_binding_snapshot_hash` char(64) DEFAULT NULL
        COMMENT '路线工序附属表单绑定快照哈希'
        AFTER `route_binding_id`;
  END IF;

  IF NOT EXISTS (
    SELECT 1
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'mes_pro_edhr_batch_execution_task'
      AND COLUMN_NAME = 'required_policy'
  ) THEN
    ALTER TABLE `mes_pro_edhr_batch_execution_task`
      ADD COLUMN `required_policy` varchar(32) NOT NULL DEFAULT 'REQUIRED'
        COMMENT '必填策略：REQUIRED/CONDITIONAL/OPTIONAL'
        AFTER `route_binding_snapshot_hash`;
  END IF;

  IF NOT EXISTS (
    SELECT 1
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'mes_pro_edhr_batch_execution_task'
      AND COLUMN_NAME = 'required_condition_json'
  ) THEN
    ALTER TABLE `mes_pro_edhr_batch_execution_task`
      ADD COLUMN `required_condition_json` json DEFAULT NULL
        COMMENT '条件必填规则JSON'
        AFTER `required_policy`;
  END IF;

  IF NOT EXISTS (
    SELECT 1
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'mes_pro_edhr_batch_execution_task'
      AND COLUMN_NAME = 'owner_role_key'
  ) THEN
    ALTER TABLE `mes_pro_edhr_batch_execution_task`
      ADD COLUMN `owner_role_key` varchar(32) NOT NULL DEFAULT 'PRODUCTION'
        COMMENT '表单责任角色：PRODUCTION/QUALITY/EQUIPMENT'
        AFTER `required_condition_json`;
  END IF;

  IF NOT EXISTS (
    SELECT 1
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'mes_pro_edhr_batch_execution_task'
      AND COLUMN_NAME = 'archive_visibility'
  ) THEN
    ALTER TABLE `mes_pro_edhr_batch_execution_task`
      ADD COLUMN `archive_visibility` varchar(32) NOT NULL DEFAULT 'FINAL_DHR'
        COMMENT '归档可见性：FINAL_DHR/INTERNAL_REVIEW/AUDIT_ONLY/ATTACHMENT_REFERENCE'
        AFTER `owner_role_key`;
  END IF;

  UPDATE `mes_pro_edhr_batch_execution_task`
  SET `archive_visibility` = CASE `archive_visibility` COLLATE utf8mb4_bin
    WHEN 'DOSSIER' COLLATE utf8mb4_bin THEN 'FINAL_DHR'
    WHEN 'CONTROLLED' COLLATE utf8mb4_bin THEN 'FINAL_DHR'
    WHEN 'INTERNAL' COLLATE utf8mb4_bin THEN 'INTERNAL_REVIEW'
    ELSE `archive_visibility`
  END
  WHERE `archive_visibility` COLLATE utf8mb4_bin IN ('DOSSIER' COLLATE utf8mb4_bin, 'CONTROLLED' COLLATE utf8mb4_bin, 'INTERNAL' COLLATE utf8mb4_bin);

  IF NOT EXISTS (
    SELECT 1
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'mes_pro_edhr_batch_execution_task'
      AND COLUMN_NAME = 'slot_config_snapshot_hash'
  ) THEN
    ALTER TABLE `mes_pro_edhr_batch_execution_task`
      ADD COLUMN `slot_config_snapshot_hash` char(64) DEFAULT NULL
        COMMENT '表单槽位配置快照哈希'
        AFTER `archive_visibility`;
  END IF;

  UPDATE `mes_pro_batch_record_execution` e
  LEFT JOIN `mes_pro_batch_record_report` r
    ON r.`report_id` COLLATE utf8mb4_bin = e.`batch_record_report_id` COLLATE utf8mb4_bin
   AND r.`tenant_id` = e.`tenant_id`
   AND r.`deleted` = b'0'
  SET e.`form_slot_type` = COALESCE(NULLIF(r.`form_slot_type` COLLATE utf8mb4_bin, '' COLLATE utf8mb4_bin), 'MAIN')
  WHERE e.`form_slot_type` IS NULL OR e.`form_slot_type` COLLATE utf8mb4_bin = '' COLLATE utf8mb4_bin;
END$$
DELIMITER ;

CALL ensure_mes_batch_record_extra_form_slots();

DROP PROCEDURE IF EXISTS ensure_mes_batch_record_extra_form_slots;
