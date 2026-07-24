-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=; type=schema; riskLevel=medium
-- eDHR V1 execution runtime schema upgrade.
-- Safe to run repeatedly: adds missing execution context columns, relaxes
-- legacy template snapshot columns, and creates MES-owned signature storage.

DROP PROCEDURE IF EXISTS ensure_mes_batch_record_execution_edhr_v1;
DELIMITER $$
CREATE PROCEDURE ensure_mes_batch_record_execution_edhr_v1()
BEGIN
  IF EXISTS (
      SELECT 1
      FROM information_schema.TABLES
      WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME = 'mes_pro_batch_record_execution'
  ) THEN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'mes_pro_batch_record_execution'
          AND COLUMN_NAME = 'route_process_id'
    ) THEN
      ALTER TABLE `mes_pro_batch_record_execution`
        ADD COLUMN `route_process_id` bigint DEFAULT NULL COMMENT '工艺路线工序ID' AFTER `work_order_code`;
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'mes_pro_batch_record_execution'
          AND COLUMN_NAME = 'task_id'
    ) THEN
      ALTER TABLE `mes_pro_batch_record_execution`
        ADD COLUMN `task_id` bigint DEFAULT NULL COMMENT '生产任务ID' AFTER `route_process_id`;
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'mes_pro_batch_record_execution'
          AND COLUMN_NAME = 'workstation_id'
    ) THEN
      ALTER TABLE `mes_pro_batch_record_execution`
        ADD COLUMN `workstation_id` bigint DEFAULT NULL COMMENT '工作站ID' AFTER `task_id`;
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'mes_pro_batch_record_execution'
          AND COLUMN_NAME = 'batch_record_report_id'
    ) THEN
      ALTER TABLE `mes_pro_batch_record_execution`
        ADD COLUMN `batch_record_report_id` varchar(64) DEFAULT NULL COMMENT '默认批记录报表ID' AFTER `workstation_id`;
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'mes_pro_batch_record_execution'
          AND COLUMN_NAME = 'execution_snapshot_json'
    ) THEN
      ALTER TABLE `mes_pro_batch_record_execution`
        ADD COLUMN `execution_snapshot_json` longtext COMMENT '执行运行态快照JSON' AFTER `meta_json`;
    END IF;

    IF EXISTS (
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'mes_pro_batch_record_execution'
          AND COLUMN_NAME = 'template_id'
          AND IS_NULLABLE = 'NO'
    ) THEN
      ALTER TABLE `mes_pro_batch_record_execution`
        MODIFY COLUMN `template_id` bigint DEFAULT NULL COMMENT '模板ID';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'mes_pro_batch_record_execution'
          AND COLUMN_NAME = 'template_code'
          AND IS_NULLABLE = 'NO'
    ) THEN
      ALTER TABLE `mes_pro_batch_record_execution`
        MODIFY COLUMN `template_code` varchar(64) DEFAULT NULL COMMENT '模板编码快照';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'mes_pro_batch_record_execution'
          AND COLUMN_NAME = 'template_name'
          AND IS_NULLABLE = 'NO'
    ) THEN
      ALTER TABLE `mes_pro_batch_record_execution`
        MODIFY COLUMN `template_name` varchar(255) DEFAULT NULL COMMENT '模板名称快照';
    END IF;
  END IF;

  CREATE TABLE IF NOT EXISTS `mes_pro_batch_record_execution_signature` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
    `execution_id` bigint NOT NULL COMMENT '执行实例ID',
    `actor_id` bigint NOT NULL COMMENT '签名人用户ID',
    `action_type` varchar(32) NOT NULL COMMENT '签名动作',
    `signature_mode` varchar(32) NOT NULL COMMENT '签名方式',
    `password_verified` bit(1) NOT NULL DEFAULT b'0' COMMENT '密码是否校验通过',
    `comment` varchar(500) DEFAULT NULL COMMENT '签名意见',
    `signed_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '签名时间',
    `creator` varchar(64) DEFAULT '' COMMENT '创建者',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updater` varchar(64) DEFAULT '' COMMENT '更新者',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
    PRIMARY KEY (`id`),
    KEY `idx_mes_pro_batch_record_execution_signature_execution_id` (`execution_id`),
    KEY `idx_mes_pro_batch_record_execution_signature_tenant_id` (`tenant_id`)
  ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MES 执行电子签名记录';
END$$
DELIMITER ;

CALL ensure_mes_batch_record_execution_edhr_v1();

DROP PROCEDURE IF EXISTS ensure_mes_batch_record_execution_edhr_v1;
