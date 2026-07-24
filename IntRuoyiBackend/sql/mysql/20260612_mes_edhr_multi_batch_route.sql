-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=; type=schema; riskLevel=medium
DROP PROCEDURE IF EXISTS ensure_mes_route_use_process_config_execution_mode;
DELIMITER $$
CREATE PROCEDURE ensure_mes_route_use_process_config_execution_mode()
BEGIN
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'mes_pro_route_use_process_config' AND COLUMN_NAME = 'execution_mode') THEN
    ALTER TABLE `mes_pro_route_use_process_config`
      ADD COLUMN `execution_mode` varchar(16) NOT NULL DEFAULT 'SEQUENTIAL' COMMENT '批记录执行模式：SEQUENTIAL/PARALLEL' AFTER `enabled`;
  END IF;
END$$
DELIMITER ;

CALL ensure_mes_route_use_process_config_execution_mode();

DROP PROCEDURE IF EXISTS ensure_mes_route_use_process_config_execution_mode;

CREATE TABLE IF NOT EXISTS `mes_pro_route_use_process_batch_record` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `route_use_process_config_id` bigint NOT NULL COMMENT '路线用途工序配置ID',
  `route_id` bigint NOT NULL COMMENT '工艺路线ID',
  `route_process_id` bigint NOT NULL COMMENT '路线工序ID',
  `use_type` varchar(32) NOT NULL COMMENT '用途类型',
  `batch_record_report_id` varchar(64) NOT NULL COMMENT '批记录报表ID',
  `form_slot_type` varchar(32) NOT NULL DEFAULT 'MAIN' COMMENT '表单槽位类型：MAIN/LOSS_REPORT/PROCESS_INSPECTION/PARAMETER_RECORD',
  `report_sort` int NOT NULL COMMENT '同工序批记录顺序',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_mes_pro_route_use_process_report_sort` (`tenant_id`, `route_process_id`, `use_type`, `report_sort`, `deleted`),
  UNIQUE KEY `uk_mes_pro_route_use_process_report` (`tenant_id`, `route_process_id`, `use_type`, `batch_record_report_id`, `deleted`),
  KEY `idx_mes_pro_route_use_process_batch_record_config` (`tenant_id`, `route_use_process_config_id`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MES 工艺路线用途工序批记录表';

ALTER TABLE `mes_pro_route_use_process_batch_record`
  CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

DROP PROCEDURE IF EXISTS ensure_mes_edhr_multi_batch_execution_task_columns;
DELIMITER $$
CREATE PROCEDURE ensure_mes_edhr_multi_batch_execution_task_columns()
BEGIN
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'mes_pro_edhr_batch_execution_task' AND COLUMN_NAME = 'batch_record_sort') THEN
    ALTER TABLE `mes_pro_edhr_batch_execution_task`
      ADD COLUMN `batch_record_sort` int NOT NULL DEFAULT 1 COMMENT '同工序批记录执行顺序' AFTER `batch_record_report_name`;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'mes_pro_edhr_batch_execution_task' AND COLUMN_NAME = 'execution_mode') THEN
    ALTER TABLE `mes_pro_edhr_batch_execution_task`
      ADD COLUMN `execution_mode` varchar(16) NOT NULL DEFAULT 'SEQUENTIAL' COMMENT '同工序执行模式：SEQUENTIAL/PARALLEL' AFTER `batch_record_sort`;
  END IF;
  IF EXISTS (SELECT 1 FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'mes_pro_edhr_batch_execution_task' AND INDEX_NAME = 'uk_mes_pro_edhr_batch_task_process') THEN
    ALTER TABLE `mes_pro_edhr_batch_execution_task` DROP INDEX `uk_mes_pro_edhr_batch_task_process`;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'mes_pro_edhr_batch_execution_task' AND INDEX_NAME = 'uk_mes_pro_edhr_batch_task_process_report') THEN
    ALTER TABLE `mes_pro_edhr_batch_execution_task`
      ADD UNIQUE KEY `uk_mes_pro_edhr_batch_task_process_report` (`tenant_id`, `batch_execution_id`, `route_process_id`, `batch_record_sort`, `deleted`);
  END IF;
END$$
DELIMITER ;

CALL ensure_mes_edhr_multi_batch_execution_task_columns();

DROP PROCEDURE IF EXISTS ensure_mes_edhr_multi_batch_execution_task_columns;

DROP PROCEDURE IF EXISTS migrate_mes_edhr_multi_batch_route;
DELIMITER $$
CREATE PROCEDURE migrate_mes_edhr_multi_batch_route()
BEGIN
  IF EXISTS (
    SELECT 1
    FROM `mes_pro_route_use_process_config` c
    LEFT JOIN `mes_pro_batch_record_report` r
      ON r.`report_id` = c.`batch_record_report_id`
     AND r.`tenant_id` = c.`tenant_id`
     AND r.`deleted` = b'0'
    WHERE c.`deleted` = b'0'
      AND c.`use_type` = 'BATCH'
      AND c.`batch_record_report_id` IS NOT NULL
      AND c.`batch_record_report_id` <> ''
      AND r.`id` IS NULL
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing batch record report referenced by eDHR batch route configuration';
  END IF;

  IF EXISTS (
    SELECT 1
    FROM `mes_pro_route_process` rp
    LEFT JOIN `mes_pro_batch_record_report` r
      ON r.`report_id` = rp.`batch_record_report_id`
     AND r.`tenant_id` = rp.`tenant_id`
     AND r.`deleted` = b'0'
    WHERE rp.`deleted` = b'0'
      AND rp.`batch_record_report_id` IS NOT NULL
      AND rp.`batch_record_report_id` <> ''
      AND r.`id` IS NULL
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing batch record report referenced by eDHR batch route configuration';
  END IF;

  INSERT INTO `mes_pro_route_use_process_batch_record`
  (`route_use_process_config_id`, `route_id`, `route_process_id`, `use_type`, `batch_record_report_id`, `report_sort`,
   `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
  SELECT c.`id`, c.`route_id`, c.`route_process_id`, c.`use_type`, c.`batch_record_report_id`, 1,
         c.`remark`, c.`creator`, NOW(), c.`updater`, NOW(), c.`deleted`, c.`tenant_id`
  FROM `mes_pro_route_use_process_config` c
  LEFT JOIN `mes_pro_route_use_process_batch_record` br
    ON br.`tenant_id` = c.`tenant_id`
   AND br.`route_process_id` = c.`route_process_id`
   AND br.`use_type` = c.`use_type`
   AND br.`report_sort` = 1
   AND br.`deleted` = c.`deleted`
  WHERE c.`deleted` = b'0'
    AND c.`use_type` = 'BATCH'
    AND c.`batch_record_report_id` IS NOT NULL
    AND c.`batch_record_report_id` <> ''
    AND br.`id` IS NULL;

  INSERT INTO `mes_pro_route_use_config`
  (`route_id`, `use_type`, `config_version`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
  SELECT DISTINCT rp.`route_id`, 'BATCH', 'legacy-route-process-migration', '从原始工艺路线批记录绑定迁移',
         rp.`creator`, NOW(), rp.`updater`, NOW(), b'0', rp.`tenant_id`
  FROM `mes_pro_route_process` rp
  LEFT JOIN `mes_pro_route_use_config` uc
    ON uc.`tenant_id` = rp.`tenant_id`
   AND uc.`route_id` = rp.`route_id`
   AND uc.`use_type` = 'BATCH'
   AND uc.`deleted` = b'0'
  WHERE rp.`deleted` = b'0'
    AND rp.`batch_record_report_id` IS NOT NULL
    AND rp.`batch_record_report_id` <> ''
    AND uc.`id` IS NULL;

  INSERT INTO `mes_pro_route_use_process_config`
  (`route_use_config_id`, `route_id`, `route_process_id`, `use_type`, `enabled`, `execution_mode`, `batch_record_report_id`,
   `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
  SELECT uc.`id`, rp.`route_id`, rp.`id`, 'BATCH', b'1', 'SEQUENTIAL', NULL,
         '从原始工艺路线批记录绑定迁移', rp.`creator`, NOW(), rp.`updater`, NOW(), b'0', rp.`tenant_id`
  FROM `mes_pro_route_process` rp
  JOIN `mes_pro_route_use_config` uc
    ON uc.`tenant_id` = rp.`tenant_id`
   AND uc.`route_id` = rp.`route_id`
   AND uc.`use_type` = 'BATCH'
   AND uc.`deleted` = b'0'
  LEFT JOIN `mes_pro_route_use_process_config` c
    ON c.`tenant_id` = rp.`tenant_id`
   AND c.`route_process_id` = rp.`id`
   AND c.`use_type` = 'BATCH'
   AND c.`deleted` = b'0'
  WHERE rp.`deleted` = b'0'
    AND rp.`batch_record_report_id` IS NOT NULL
    AND rp.`batch_record_report_id` <> ''
    AND c.`id` IS NULL;

  INSERT INTO `mes_pro_route_use_process_batch_record`
  (`route_use_process_config_id`, `route_id`, `route_process_id`, `use_type`, `batch_record_report_id`, `report_sort`,
   `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
  SELECT c.`id`, c.`route_id`, c.`route_process_id`, c.`use_type`, rp.`batch_record_report_id`, 1,
         '从原始工艺路线批记录绑定迁移', rp.`creator`, NOW(), rp.`updater`, NOW(), b'0', rp.`tenant_id`
  FROM `mes_pro_route_process` rp
  JOIN `mes_pro_route_use_process_config` c
    ON c.`tenant_id` = rp.`tenant_id`
   AND c.`route_process_id` = rp.`id`
   AND c.`use_type` = 'BATCH'
   AND c.`deleted` = b'0'
  LEFT JOIN `mes_pro_route_use_process_batch_record` br
    ON br.`tenant_id` = c.`tenant_id`
   AND br.`route_process_id` = c.`route_process_id`
   AND br.`use_type` = c.`use_type`
   AND br.`report_sort` = 1
   AND br.`deleted` = c.`deleted`
  WHERE rp.`deleted` = b'0'
    AND rp.`batch_record_report_id` IS NOT NULL
    AND rp.`batch_record_report_id` <> ''
    AND br.`id` IS NULL;
END$$
DELIMITER ;

CALL migrate_mes_edhr_multi_batch_route();

DROP PROCEDURE IF EXISTS migrate_mes_edhr_multi_batch_route;
