-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260709_mes_route_process_flow_graph; type=schema; riskLevel=high
-- MES 工艺流程配置统一承载排产配置与批记录配置。
-- Rollback: verify *_legacy_20260709 tables, then rename mes_pro_route_use_*_legacy_20260709 back to mes_pro_route_use_* and drop mes_pro_route_flow_* tables.

DROP PROCEDURE IF EXISTS intruoyi_unify_mes_route_flow_config;

DELIMITER //

CREATE PROCEDURE intruoyi_unify_mes_route_flow_config()
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.tables
    WHERE table_schema = DATABASE() AND table_name = 'mes_pro_route_use_config'
  ) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'missing legacy mes_pro_route_use_config before route flow migration';
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.tables
    WHERE table_schema = DATABASE() AND table_name = 'mes_pro_route_use_process_config'
  ) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'missing legacy mes_pro_route_use_process_config before route flow migration';
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.tables
    WHERE table_schema = DATABASE() AND table_name = 'mes_pro_route_use_process_batch_record'
  ) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'missing legacy mes_pro_route_use_process_batch_record before route flow migration';
  END IF;

  IF EXISTS (
    SELECT 1
    FROM `mes_pro_route_use_config` c
    LEFT JOIN `mes_pro_route` r
      ON r.`id` = c.`route_id`
     AND r.`tenant_id` = c.`tenant_id`
     AND r.`deleted` = b'0'
    WHERE c.`deleted` = b'0'
      AND r.`id` IS NULL
  ) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'route flow migration found legacy config with missing route';
  END IF;

  IF EXISTS (
    SELECT 1
    FROM `mes_pro_route_use_process_config` pc
    LEFT JOIN `mes_pro_route_process` rp
      ON rp.`id` = pc.`route_process_id`
     AND rp.`tenant_id` = pc.`tenant_id`
     AND rp.`deleted` = b'0'
    WHERE pc.`deleted` = b'0'
      AND rp.`id` IS NULL
  ) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'route flow migration found legacy process config with missing route process';
  END IF;

  IF EXISTS (
    SELECT 1
    FROM `mes_pro_route_use_process_batch_record` br
    LEFT JOIN `mes_pro_batch_record_report` report
      ON report.`report_id` = br.`batch_record_report_id`
     AND report.`tenant_id` = br.`tenant_id`
     AND report.`deleted` = b'0'
    WHERE br.`deleted` = b'0'
      AND br.`batch_record_report_id` IS NOT NULL
      AND br.`batch_record_report_id` <> ''
      AND report.`id` IS NULL
  ) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'route flow migration found legacy batch record binding with missing report';
  END IF;

  CREATE TABLE IF NOT EXISTS `mes_pro_route_flow_config` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
    `route_id` bigint NOT NULL COMMENT '工艺路线ID',
    `use_type` varchar(16) NOT NULL COMMENT '配置类型：SCHEDULE/BATCH',
    `enabled` bit(1) NOT NULL DEFAULT b'0' COMMENT '配置启用状态',
    `config_version` varchar(64) DEFAULT NULL COMMENT '配置版本',
    `remark` varchar(500) DEFAULT NULL COMMENT '备注',
    `creator` varchar(64) NOT NULL DEFAULT '',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updater` varchar(64) DEFAULT '',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` bit(1) NOT NULL DEFAULT b'0',
    `tenant_id` bigint NOT NULL DEFAULT '0',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_mes_pro_route_flow_config` (`tenant_id`,`route_id`,`use_type`,`deleted`)
  ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MES 工艺流程配置';

  CREATE TABLE IF NOT EXISTS `mes_pro_route_flow_process_config` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
    `route_flow_config_id` bigint NOT NULL COMMENT '工艺流程配置ID',
    `route_id` bigint NOT NULL COMMENT '工艺路线ID',
    `route_process_id` bigint NOT NULL COMMENT '工艺路线工序ID',
    `use_type` varchar(16) NOT NULL COMMENT '配置类型：SCHEDULE/BATCH',
    `enabled` bit(1) NOT NULL DEFAULT b'1' COMMENT '当前配置下是否启用',
    `execution_mode` varchar(16) NOT NULL DEFAULT 'SEQUENTIAL' COMMENT '批记录执行模式：SEQUENTIAL/PARALLEL',
    `production_quantity_factor` decimal(18,6) NOT NULL DEFAULT 1.000000 COMMENT '生产数量系数，工序计划数量=成品数量*生产数量系数',
    `batch_record_report_id` varchar(64) DEFAULT NULL COMMENT '历史字段：批记录报表ID迁移来源',
    `remark` varchar(500) DEFAULT NULL COMMENT '备注',
    `creator` varchar(64) NOT NULL DEFAULT '',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updater` varchar(64) DEFAULT '',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` bit(1) NOT NULL DEFAULT b'0',
    `tenant_id` bigint NOT NULL DEFAULT '0',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_mes_pro_route_flow_process_config` (`tenant_id`,`route_process_id`,`use_type`,`deleted`),
    KEY `idx_mes_pro_route_flow_process_config_flow` (`tenant_id`,`route_flow_config_id`,`deleted`)
  ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MES 工艺流程工序配置';

  CREATE TABLE IF NOT EXISTS `mes_pro_route_flow_process_batch_record` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
    `route_flow_process_config_id` bigint NOT NULL COMMENT '工艺流程工序配置ID',
    `route_id` bigint NOT NULL COMMENT '工艺路线ID',
    `route_process_id` bigint NOT NULL COMMENT '路线工序ID',
    `use_type` varchar(32) NOT NULL COMMENT '配置类型',
    `batch_record_report_id` varchar(64) NOT NULL COMMENT '批记录报表ID',
    `batch_record_definition_id` bigint DEFAULT NULL COMMENT '批记录定义ID',
    `batch_record_version_id` bigint DEFAULT NULL COMMENT '批记录版本ID',
    `form_slot_type` varchar(32) NOT NULL DEFAULT 'MAIN' COMMENT '表单槽位类型',
    `record_category` varchar(32) NOT NULL DEFAULT 'BATCH_RECORD' COMMENT '记录类型',
    `validation_profile` varchar(32) NOT NULL DEFAULT 'CONTROLLED_BATCH' COMMENT '校验策略',
    `permission_scope_id` bigint DEFAULT NULL COMMENT '对象级权限范围ID',
    `record_category_snapshot_hash` varchar(128) DEFAULT NULL COMMENT '记录类型快照哈希',
    `required_policy` varchar(32) DEFAULT NULL COMMENT '必填策略',
    `required_condition_json` text DEFAULT NULL COMMENT '条件必填表达式 JSON',
    `owner_role_key` varchar(32) DEFAULT NULL COMMENT '责任角色',
    `archive_visibility` varchar(32) DEFAULT NULL COMMENT '归档可见性',
    `slot_config_snapshot_hash` varchar(128) DEFAULT NULL COMMENT '槽位配置快照 Hash',
    `report_sort` int NOT NULL COMMENT '同工序批记录顺序',
    `remark` varchar(500) DEFAULT NULL COMMENT '备注',
    `creator` varchar(64) DEFAULT '',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updater` varchar(64) DEFAULT '',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` bit(1) NOT NULL DEFAULT b'0',
    `tenant_id` bigint NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_mes_pro_route_flow_process_report_sort` (`tenant_id`, `route_process_id`, `use_type`, `report_sort`, `deleted`),
    UNIQUE KEY `uk_mes_pro_route_flow_process_report` (`tenant_id`, `route_process_id`, `use_type`, `batch_record_report_id`, `deleted`),
    KEY `idx_mes_pro_route_flow_process_batch_record_config` (`tenant_id`, `route_flow_process_config_id`, `deleted`)
  ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MES 工艺流程工序批记录表';

  INSERT INTO `mes_pro_route_flow_config`
  (`id`, `route_id`, `use_type`, `enabled`, `config_version`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
  SELECT `id`, `route_id`, `use_type`, `enabled`, `config_version`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`
  FROM `mes_pro_route_use_config`
  WHERE NOT EXISTS (SELECT 1 FROM `mes_pro_route_flow_config` LIMIT 1);

  INSERT INTO `mes_pro_route_flow_process_config`
  (`id`, `route_flow_config_id`, `route_id`, `route_process_id`, `use_type`, `enabled`, `execution_mode`, `production_quantity_factor`, `batch_record_report_id`, `remark`,
   `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
  SELECT `id`, `route_use_config_id`, `route_id`, `route_process_id`, `use_type`, `enabled`, `execution_mode`, `production_quantity_factor`, `batch_record_report_id`, `remark`,
         `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`
  FROM `mes_pro_route_use_process_config`
  WHERE NOT EXISTS (SELECT 1 FROM `mes_pro_route_flow_process_config` LIMIT 1);

  INSERT INTO `mes_pro_route_flow_process_batch_record`
  (`id`, `route_flow_process_config_id`, `route_id`, `route_process_id`, `use_type`, `batch_record_report_id`, `batch_record_definition_id`, `batch_record_version_id`,
   `form_slot_type`, `record_category`, `validation_profile`, `permission_scope_id`, `record_category_snapshot_hash`, `required_policy`, `required_condition_json`,
   `owner_role_key`, `archive_visibility`, `slot_config_snapshot_hash`, `report_sort`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
  SELECT `id`, `route_use_process_config_id`, `route_id`, `route_process_id`, `use_type`, `batch_record_report_id`, `batch_record_definition_id`, `batch_record_version_id`,
         `form_slot_type`, `record_category`, `validation_profile`, `permission_scope_id`, `record_category_snapshot_hash`, `required_policy`, `required_condition_json`,
         `owner_role_key`, `archive_visibility`, `slot_config_snapshot_hash`, `report_sort`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`
  FROM `mes_pro_route_use_process_batch_record`
  WHERE NOT EXISTS (SELECT 1 FROM `mes_pro_route_flow_process_batch_record` LIMIT 1);

  IF (
    SELECT COUNT(*) FROM `mes_pro_route_use_config` WHERE `deleted` = b'0'
  ) <> (
    SELECT COUNT(*) FROM `mes_pro_route_flow_config` WHERE `deleted` = b'0'
  ) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'route flow migration config count mismatch';
  END IF;

  IF (
    SELECT COUNT(*) FROM `mes_pro_route_use_process_config` WHERE `deleted` = b'0'
  ) <> (
    SELECT COUNT(*) FROM `mes_pro_route_flow_process_config` WHERE `deleted` = b'0'
  ) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'route flow migration process config count mismatch';
  END IF;

  IF (
    SELECT COUNT(*) FROM `mes_pro_route_use_process_batch_record` WHERE `deleted` = b'0'
  ) <> (
    SELECT COUNT(*) FROM `mes_pro_route_flow_process_batch_record` WHERE `deleted` = b'0'
  ) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'route flow migration batch record count mismatch';
  END IF;

  INSERT INTO `system_menu`
  (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
  VALUES
  (5726, '工艺流程排产配置查询', 'mes:pro-route:schedule-config:query', 3, 6, 5720, '', '', '', '', 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
  (5727, '工艺流程排产配置更新', 'mes:pro-route:schedule-config:update', 3, 7, 5720, '', '', '', '', 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
  (5728, '工艺流程批记录配置查询', 'mes:pro-route:batch-record-config:query', 3, 8, 5720, '', '', '', '', 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
  (5729, '工艺流程批记录配置更新', 'mes:pro-route:batch-record-config:update', 3, 9, 5720, '', '', '', '', 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0')
  ON DUPLICATE KEY UPDATE
    `name` = VALUES(`name`),
    `permission` = VALUES(`permission`),
    `type` = VALUES(`type`),
    `sort` = VALUES(`sort`),
    `parent_id` = VALUES(`parent_id`),
    `status` = VALUES(`status`),
    `visible` = VALUES(`visible`),
    `deleted` = b'0',
    `updater` = '1',
    `update_time` = NOW();

  INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
  SELECT DISTINCT rm.`role_id`,
         CASE rm.`menu_id`
           WHEN 900121 THEN 5726
           WHEN 900122 THEN 5727
           WHEN 900221 THEN 5728
           WHEN 900222 THEN 5729
         END AS `menu_id`,
         '1', NOW(), '1', NOW(), b'0', rm.`tenant_id`
  FROM `system_role_menu` rm
  WHERE rm.`deleted` = b'0'
    AND rm.`menu_id` IN (900121, 900122, 900221, 900222)
    AND NOT EXISTS (
      SELECT 1
      FROM `system_role_menu` existing
      WHERE existing.`role_id` = rm.`role_id`
        AND existing.`tenant_id` = rm.`tenant_id`
        AND existing.`menu_id` = CASE rm.`menu_id`
          WHEN 900121 THEN 5726
          WHEN 900122 THEN 5727
          WHEN 900221 THEN 5728
          WHEN 900222 THEN 5729
        END
        AND existing.`deleted` = b'0'
    );

  UPDATE `system_role_menu`
  SET `deleted` = b'1', `updater` = '1', `update_time` = NOW()
  WHERE `menu_id` IN (900121, 900122, 900221, 900222)
    AND `deleted` = b'0';

  UPDATE `system_menu`
  SET `deleted` = b'1', `visible` = b'0', `status` = 1, `updater` = '1', `update_time` = NOW()
  WHERE `id` IN (900121, 900122, 900221, 900222);

  IF EXISTS (
    SELECT 1 FROM `system_menu`
    WHERE `id` IN (900121, 900122, 900221, 900222)
      AND `deleted` = b'0'
  ) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'old process route menus must be deleted after route flow migration';
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.tables
    WHERE table_schema = DATABASE() AND table_name = 'mes_pro_route_use_config_legacy_20260709'
  ) THEN
    RENAME TABLE `mes_pro_route_use_config` TO `mes_pro_route_use_config_legacy_20260709`;
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.tables
    WHERE table_schema = DATABASE() AND table_name = 'mes_pro_route_use_process_config_legacy_20260709'
  ) THEN
    RENAME TABLE `mes_pro_route_use_process_config` TO `mes_pro_route_use_process_config_legacy_20260709`;
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.tables
    WHERE table_schema = DATABASE() AND table_name = 'mes_pro_route_use_process_batch_record_legacy_20260709'
  ) THEN
    RENAME TABLE `mes_pro_route_use_process_batch_record` TO `mes_pro_route_use_process_batch_record_legacy_20260709`;
  END IF;

  IF EXISTS (
    SELECT 1 FROM information_schema.tables
    WHERE table_schema = DATABASE()
      AND table_name IN ('mes_pro_route_use_config', 'mes_pro_route_use_process_config', 'mes_pro_route_use_process_batch_record')
  ) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'legacy route use active table names must not remain after migration';
  END IF;
END//

DELIMITER ;

CALL intruoyi_unify_mes_route_flow_config();

DROP PROCEDURE IF EXISTS intruoyi_unify_mes_route_flow_config;
