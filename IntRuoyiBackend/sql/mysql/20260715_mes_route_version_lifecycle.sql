-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260613_mes_smart_scheduling_t1_schema,20260608_edhr_batch_execution_schema; type=schema; riskLevel=medium
-- 工艺路线版本生命周期与 eDHR 生产引用冻结。
-- 数据安全：本迁移只新增字段、索引与唯一约束；不猜测回填历史 eDHR route_version_id。

DROP PROCEDURE IF EXISTS ensure_mes_route_version_lifecycle_schema;
DELIMITER $$
CREATE PROCEDURE ensure_mes_route_version_lifecycle_schema()
BEGIN
  IF EXISTS (
    SELECT 1
      FROM `mes_pro_route_version`
     WHERE `deleted` = b'0'
       AND `active` = b'1'
     GROUP BY `tenant_id`, `route_id`
    HAVING COUNT(*) > 1
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'duplicate active route versions must be resolved before migration';
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE()
       AND TABLE_NAME = 'mes_pro_route_version'
       AND COLUMN_NAME = 'lifecycle_status'
  ) THEN
    ALTER TABLE `mes_pro_route_version`
      ADD COLUMN `lifecycle_status` varchar(32) NOT NULL DEFAULT 'ACTIVE' COMMENT '生命周期状态：DRAFT/PENDING_APPROVAL/READY_TO_PUBLISH/ACTIVE/SUPERSEDED/REJECTED/CANCELLED' AFTER `active`;
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE()
       AND TABLE_NAME = 'mes_pro_route_version'
       AND COLUMN_NAME = 'change_summary_json'
  ) THEN
    ALTER TABLE `mes_pro_route_version`
      ADD COLUMN `change_summary_json` json NULL COMMENT '版本变更摘要' AFTER `route_snapshot_json`;
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE()
       AND TABLE_NAME = 'mes_pro_route_version'
       AND COLUMN_NAME = 'validation_result_json'
  ) THEN
    ALTER TABLE `mes_pro_route_version`
      ADD COLUMN `validation_result_json` json NULL COMMENT '发布校验结果' AFTER `change_summary_json`;
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE()
       AND TABLE_NAME = 'mes_pro_route_version'
       AND COLUMN_NAME = 'submitted_by'
  ) THEN
    ALTER TABLE `mes_pro_route_version`
      ADD COLUMN `submitted_by` bigint NULL COMMENT '提交人' AFTER `validation_result_json`;
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE()
       AND TABLE_NAME = 'mes_pro_route_version'
       AND COLUMN_NAME = 'submitted_time'
  ) THEN
    ALTER TABLE `mes_pro_route_version`
      ADD COLUMN `submitted_time` datetime NULL COMMENT '提交时间' AFTER `submitted_by`;
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE()
       AND TABLE_NAME = 'mes_pro_route_version'
       AND COLUMN_NAME = 'approval_process_instance_id'
  ) THEN
    ALTER TABLE `mes_pro_route_version`
      ADD COLUMN `approval_process_instance_id` varchar(64) NULL COMMENT '审批流程实例ID' AFTER `submitted_time`;
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE()
       AND TABLE_NAME = 'mes_pro_route_version'
       AND COLUMN_NAME = 'published_by'
  ) THEN
    ALTER TABLE `mes_pro_route_version`
      ADD COLUMN `published_by` bigint NULL COMMENT '发布人' AFTER `approval_process_instance_id`;
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE()
       AND TABLE_NAME = 'mes_pro_route_version'
       AND COLUMN_NAME = 'published_time'
  ) THEN
    ALTER TABLE `mes_pro_route_version`
      ADD COLUMN `published_time` datetime NULL COMMENT '发布时间' AFTER `published_by`;
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE()
       AND TABLE_NAME = 'mes_pro_route_version'
       AND COLUMN_NAME = 'active_unique_flag'
  ) THEN
    ALTER TABLE `mes_pro_route_version`
      ADD COLUMN `active_unique_flag` tinyint
        GENERATED ALWAYS AS (IF(`deleted` = b'0' AND `active` = b'1', 1, NULL)) STORED
        COMMENT '当前生效版本唯一约束标记' AFTER `deleted`;
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.STATISTICS
     WHERE TABLE_SCHEMA = DATABASE()
       AND TABLE_NAME = 'mes_pro_route_version'
       AND INDEX_NAME = 'uk_mes_pro_route_version_active_one'
  ) THEN
    ALTER TABLE `mes_pro_route_version`
      ADD UNIQUE INDEX `uk_mes_pro_route_version_active_one`
        (`tenant_id`, `route_id`, `active_unique_flag`) USING BTREE;
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE()
       AND TABLE_NAME = 'mes_pro_edhr_batch_execution'
       AND COLUMN_NAME = 'route_version_id'
  ) THEN
    ALTER TABLE `mes_pro_edhr_batch_execution`
      ADD COLUMN `route_version_id` bigint NULL COMMENT '创建时冻结路线版本ID' AFTER `route_id`;
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE()
       AND TABLE_NAME = 'mes_pro_edhr_batch_execution'
       AND COLUMN_NAME = 'route_version_no'
  ) THEN
    ALTER TABLE `mes_pro_edhr_batch_execution`
      ADD COLUMN `route_version_no` varchar(32) NULL COMMENT '创建时冻结路线版本号' AFTER `route_version_id`;
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE()
       AND TABLE_NAME = 'mes_pro_edhr_batch_execution'
       AND COLUMN_NAME = 'route_snapshot_json'
  ) THEN
    ALTER TABLE `mes_pro_edhr_batch_execution`
      ADD COLUMN `route_snapshot_json` json NULL COMMENT '创建时冻结路线快照' AFTER `route_version_no`;
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.STATISTICS
     WHERE TABLE_SCHEMA = DATABASE()
       AND TABLE_NAME = 'mes_pro_edhr_batch_execution'
       AND INDEX_NAME = 'idx_mes_pro_edhr_batch_execution_route_version'
  ) THEN
    ALTER TABLE `mes_pro_edhr_batch_execution`
      ADD INDEX `idx_mes_pro_edhr_batch_execution_route_version`
        (`tenant_id`, `route_version_id`) USING BTREE;
  END IF;
END$$
DELIMITER ;

CALL ensure_mes_route_version_lifecycle_schema();

DROP PROCEDURE IF EXISTS ensure_mes_route_version_lifecycle_schema;
