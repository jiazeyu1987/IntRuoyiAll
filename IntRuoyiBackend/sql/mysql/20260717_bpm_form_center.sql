-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260615_system_config_package_menu; type=schema; riskLevel=medium
SET NAMES utf8mb4;

ALTER TABLE `system_tenant_package`
  MODIFY COLUMN `menu_ids` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '关联的菜单编号';

CREATE TABLE IF NOT EXISTS `bpm_form_template_version` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `template_id` bigint DEFAULT NULL COMMENT '模板编号',
  `tenant_id` bigint NOT NULL COMMENT '租户编号',
  `template_name` varchar(128) NOT NULL COMMENT '模板名称',
  `version_no` varchar(64) NOT NULL COMMENT '版本号',
  `status` varchar(32) NOT NULL COMMENT '状态',
  `source_file_name` varchar(255) NOT NULL COMMENT '源文件名',
  `source_file_content` longtext DEFAULT NULL COMMENT '源文件 Base64 内容',
  `recognized_schema_json` longtext DEFAULT NULL COMMENT '识别结果 JSON',
  `jimu_schema_json` longtext DEFAULT NULL COMMENT 'Jimu schema JSON',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_bpm_form_template_version` (`tenant_id`, `template_id`, `version_no`),
  KEY `idx_bpm_form_template_version_name` (`tenant_id`, `template_name`),
  KEY `idx_bpm_form_template_version_status` (`tenant_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='表单中心模板版本';

CREATE TABLE IF NOT EXISTS `bpm_form_action_policy` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `tenant_id` bigint NOT NULL COMMENT '租户编号',
  `data_domain` varchar(64) NOT NULL COMMENT '数据域',
  `system_code` varchar(64) NOT NULL COMMENT '系统编码',
  `object_type` varchar(64) NOT NULL COMMENT '对象类型',
  `action_code` varchar(64) NOT NULL COMMENT '动作编码',
  `object_state` varchar(64) NOT NULL COMMENT '对象状态',
  `policy_type` varchar(32) NOT NULL COMMENT '策略类型',
  `approval_mode` varchar(32) NOT NULL DEFAULT 'BPM_REQUIRED' COMMENT '审批模式：BPM_REQUIRED/DIRECT',
  `bpm_process_key` varchar(128) DEFAULT NULL COMMENT 'BPM 流程 key',
  `effect_executor_code` varchar(128) DEFAULT NULL COMMENT '生效执行器编码',
  `status` varchar(32) NOT NULL COMMENT '状态',
  `slots_json` longtext DEFAULT NULL COMMENT '表单槽位 JSON',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  KEY `idx_bpm_form_action_policy_match` (`tenant_id`, `data_domain`, `system_code`, `object_type`, `action_code`, `object_state`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='表单中心业务动作策略';

CREATE TABLE IF NOT EXISTS `bpm_form_action_instance` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `instance_code` varchar(128) NOT NULL COMMENT '实例编码',
  `tenant_id` bigint NOT NULL COMMENT '租户编号',
  `policy_id` bigint NOT NULL COMMENT '策略编号',
  `applicant_user_id` bigint NOT NULL COMMENT '申请人',
  `status` varchar(32) NOT NULL COMMENT '状态',
  `data_domain` varchar(64) NOT NULL COMMENT '数据域',
  `system_code` varchar(64) NOT NULL COMMENT '系统编码',
  `object_type` varchar(64) NOT NULL COMMENT '对象类型',
  `action_code` varchar(64) NOT NULL COMMENT '动作编码',
  `object_state` varchar(64) NOT NULL COMMENT '对象状态',
  `object_id` varchar(128) NOT NULL COMMENT '对象编号',
  `object_version` varchar(128) NOT NULL COMMENT '对象版本',
  `idempotency_key` varchar(128) NOT NULL COMMENT '幂等键',
  `business_context_json` longtext NOT NULL COMMENT '业务上下文 JSON',
  `form_data_json` longtext DEFAULT NULL COMMENT '表单数据 JSON',
  `bpm_process_instance_id` varchar(128) DEFAULT NULL COMMENT 'BPM 流程实例编号',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_bpm_form_action_instance_idem` (`tenant_id`, `system_code`, `object_type`, `object_id`, `object_version`, `action_code`, `idempotency_key`),
  KEY `idx_bpm_form_action_instance_context` (`tenant_id`, `system_code`, `object_type`, `object_id`, `object_version`, `action_code`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='表单中心实例';


CREATE TABLE IF NOT EXISTS `bpm_form_action_snapshot` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'id',
  `instance_id` bigint NOT NULL COMMENT 'instance id',
  `tenant_id` bigint NOT NULL COMMENT 'tenant id',
  `snapshot_type` varchar(32) NOT NULL COMMENT 'snapshot type',
  `snapshot_version` int NOT NULL COMMENT 'snapshot version',
  `form_data_json` longtext DEFAULT NULL COMMENT 'form data json',
  `business_context_json` longtext NOT NULL COMMENT 'business context json',
  `attachment_ids_json` longtext DEFAULT NULL COMMENT 'attachment ids json',
  `creator` varchar(64) DEFAULT '' COMMENT 'creator',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'create time',
  `updater` varchar(64) DEFAULT '' COMMENT 'updater',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'update time',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT 'deleted',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_bpm_form_action_snapshot_version` (`tenant_id`, `instance_id`, `snapshot_version`),
  KEY `idx_bpm_form_action_snapshot_instance` (`tenant_id`, `instance_id`, `snapshot_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='form center action snapshot';

CREATE TABLE IF NOT EXISTS `bpm_form_task_permission` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'id',
  `instance_id` bigint NOT NULL COMMENT 'instance id',
  `tenant_id` bigint NOT NULL COMMENT 'tenant id',
  `bpm_process_instance_id` varchar(128) NOT NULL COMMENT 'bpm process instance id',
  `task_id` varchar(128) NOT NULL COMMENT 'active task id',
  `user_id` bigint NOT NULL COMMENT 'user id',
  `permission_codes_json` longtext NOT NULL COMMENT 'permission codes json',
  `status` varchar(32) NOT NULL COMMENT 'status',
  `creator` varchar(64) DEFAULT '' COMMENT 'creator',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'create time',
  `updater` varchar(64) DEFAULT '' COMMENT 'updater',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'update time',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT 'deleted',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_bpm_form_task_permission_task_user` (`tenant_id`, `instance_id`, `task_id`, `user_id`),
  KEY `idx_bpm_form_task_permission_active` (`tenant_id`, `bpm_process_instance_id`, `task_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='form center active task permission';

CREATE TABLE IF NOT EXISTS `bpm_form_effect_execution` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'id',
  `instance_id` bigint NOT NULL COMMENT 'instance id',
  `tenant_id` bigint NOT NULL COMMENT 'tenant id',
  `execution_code` varchar(128) NOT NULL COMMENT 'execution code',
  `idempotency_key` varchar(128) NOT NULL COMMENT 'idempotency key',
  `status` varchar(32) NOT NULL COMMENT 'status',
  `result_ref` varchar(255) DEFAULT NULL COMMENT 'result ref',
  `failure_reason` varchar(500) DEFAULT NULL COMMENT 'failure reason',
  `creator` varchar(64) DEFAULT '' COMMENT 'creator',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'create time',
  `updater` varchar(64) DEFAULT '' COMMENT 'updater',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'update time',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT 'deleted',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_bpm_form_effect_execution_idem` (`tenant_id`, `instance_id`, `idempotency_key`),
  KEY `idx_bpm_form_effect_execution_pending` (`tenant_id`, `status`, `instance_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='form center effect execution';

DROP PROCEDURE IF EXISTS ensure_bpm_form_center_menu;

DELIMITER //
CREATE PROCEDURE ensure_bpm_form_center_menu()
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM `system_menu`
    WHERE `id` = 1186
      AND `deleted` = b'0'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing workflow management parent menu 1186 for form center';
  END IF;

  DROP TEMPORARY TABLE IF EXISTS `tmp_form_center_menu_defs`;
  CREATE TEMPORARY TABLE `tmp_form_center_menu_defs` (
    `id` bigint NOT NULL,
    `name` varchar(50) NOT NULL,
    `permission` varchar(100) NOT NULL,
    `type` tinyint NOT NULL,
    `sort` int NOT NULL,
    `parent_id` bigint NOT NULL,
    `path` varchar(200) NOT NULL,
    `icon` varchar(100) NOT NULL,
    `component` varchar(255) NOT NULL,
    `component_name` varchar(255) NOT NULL,
    `status` tinyint NOT NULL,
    `visible` bit(1) NOT NULL,
    `keep_alive` bit(1) NOT NULL,
    `always_show` bit(1) NOT NULL,
    PRIMARY KEY (`id`)
  ) DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

  INSERT INTO `tmp_form_center_menu_defs`
    (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`)
  VALUES
    (605071200, '表单中心', '', 1, 90, 1186, 'form-center', 'ep:document', '', '', 0, b'1', b'1', b'1'),
    (605071201, '表单模板', 'form:template:query', 2, 1, 605071200, 'template', 'ep:document-copy', 'form-center/template/index', 'FormCenterTemplate', 0, b'1', b'1', b'1'),
    (605071202, '模板查询', 'form:template:query', 3, 1, 605071201, '', '', '', '', 0, b'1', b'1', b'1'),
    (605071203, '模板导入', 'form:template:create', 3, 2, 605071201, '', '', '', '', 0, b'1', b'1', b'1'),
    (605071204, '模板更新', 'form:template:update', 3, 3, 605071201, '', '', '', '', 0, b'1', b'1', b'1'),
    (605071205, '模板发布', 'form:template:publish', 3, 4, 605071201, '', '', '', '', 0, b'1', b'1', b'1'),
    (605071206, '模板停用', 'form:template:disable', 3, 5, 605071201, '', '', '', '', 0, b'1', b'1', b'1'),
    (605071207, '模板作废', 'form:template:obsolete', 3, 6, 605071201, '', '', '', '', 0, b'1', b'1', b'1'),
    (605071208, '源文件下载', 'form:template-source:download', 3, 7, 605071201, '', '', '', '', 0, b'1', b'1', b'1'),
    (605071210, '实例创建', 'form:instance:create', 3, 20, 605071200, '', '', '', '', 0, b'1', b'1', b'1'),
    (605071211, '实例保存', 'form:instance:update', 3, 21, 605071200, '', '', '', '', 0, b'1', b'1', b'1'),
    (605071212, '实例提交', 'form:instance:submit', 3, 22, 605071200, '', '', '', '', 0, b'1', b'1', b'1'),
    (605071213, '实例放弃', 'form:instance:abandon', 3, 23, 605071200, '', '', '', '', 0, b'1', b'1', b'1'),
    (605071214, '表单策略', 'form:policy:query', 2, 2, 605071200, 'policy', 'ep:operation', 'form-center/policy/index', 'FormCenterPolicy', 0, b'1', b'1', b'1'),
    (605071215, '策略查询', 'form:policy:query', 3, 1, 605071214, '', '', '', '', 0, b'1', b'1', b'1'),
    (605071216, '策略创建', 'form:policy:create', 3, 2, 605071214, '', '', '', '', 0, b'1', b'1', b'1'),
    (605071217, '策略发布', 'form:policy:publish', 3, 3, 605071214, '', '', '', '', 0, b'1', b'1', b'1'),
    (605071218, 'BPM回调处理', 'form:bpm-callback:handle', 3, 24, 605071200, '', '', '', '', 0, b'1', b'1', b'1'),
    (605071219, '实例快照查询', 'form:instance:snapshot:query', 3, 25, 605071200, '', '', '', '', 0, b'1', b'1', b'1'),
    (605071220, '生效待处理', 'form:effect:query', 2, 3, 605071200, 'effect', 'ep:warning', 'form-center/effect/index', 'FormCenterEffect', 0, b'1', b'1', b'1'),
    (605071221, '生效重试', 'form:effect:retry', 3, 1, 605071220, '', '', '', '', 0, b'1', b'1', b'1');

  UPDATE `system_menu`
  SET `parent_id` = 605071200,
      `sort` = CASE `id`
        WHEN 605071210 THEN 20
        WHEN 605071211 THEN 21
        WHEN 605071212 THEN 22
        WHEN 605071213 THEN 23
        WHEN 605071218 THEN 24
        WHEN 605071219 THEN 25
        ELSE `sort`
      END,
      `updater` = '1',
      `update_time` = NOW()
  WHERE `deleted` = b'0'
    AND `parent_id` = 605071209
    AND `id` IN (605071210, 605071211, 605071212, 605071213, 605071218, 605071219);

  UPDATE `system_menu`
  SET `visible` = b'0',
      `status` = 1,
      `deleted` = b'1',
      `updater` = '1',
      `update_time` = NOW()
  WHERE `id` = 605071209
    AND `deleted` = b'0';

  UPDATE `system_role_menu`
  SET `deleted` = b'1',
      `updater` = '1',
      `update_time` = NOW()
  WHERE `menu_id` = 605071209
    AND `deleted` = b'0';

  IF EXISTS (
    SELECT 1
    FROM `system_menu` AS `existing`
    JOIN `tmp_form_center_menu_defs` AS `def`
      ON `def`.`id` = `existing`.`id`
    WHERE `existing`.`deleted` <> b'0'
       OR COALESCE(`existing`.`permission`, '') <> `def`.`permission`
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Conflicting form center system_menu id or permission exists';
  END IF;

  IF EXISTS (
    SELECT 1
    FROM `system_menu` AS `existing`
    JOIN `tmp_form_center_menu_defs` AS `def`
      ON `def`.`permission` <> ''
     AND `def`.`permission` = `existing`.`permission`
     AND `def`.`id` <> `existing`.`id`
    WHERE `existing`.`deleted` = b'0'
      AND `existing`.`id` NOT IN (
        605071200, 605071201, 605071202, 605071203, 605071204, 605071205, 605071206,
        605071207, 605071208, 605071210, 605071211, 605071212, 605071213,
        605071214, 605071215, 605071216, 605071217, 605071218, 605071219, 605071220, 605071221
      )
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Conflicting form center system_menu id or permission exists';
  END IF;

  INSERT INTO `system_menu` (
    `id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`,
    `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`,
    `creator`, `create_time`, `updater`, `update_time`, `deleted`
  )
  SELECT
    `def`.`id`, `def`.`name`, `def`.`permission`, `def`.`type`, `def`.`sort`, `def`.`parent_id`,
    `def`.`path`, `def`.`icon`, `def`.`component`, `def`.`component_name`, `def`.`status`,
    `def`.`visible`, `def`.`keep_alive`, `def`.`always_show`, '1', NOW(), '1', NOW(), b'0'
  FROM `tmp_form_center_menu_defs` AS `def`
  LEFT JOIN `system_menu` AS `existing`
    ON `existing`.`id` = `def`.`id`
  WHERE `existing`.`id` IS NULL;

  UPDATE `system_menu` AS `existing`
  JOIN `tmp_form_center_menu_defs` AS `def`
    ON `def`.`id` = `existing`.`id`
  SET `existing`.`name` = `def`.`name`,
      `existing`.`permission` = `def`.`permission`,
      `existing`.`type` = `def`.`type`,
      `existing`.`sort` = `def`.`sort`,
      `existing`.`parent_id` = `def`.`parent_id`,
      `existing`.`path` = `def`.`path`,
      `existing`.`icon` = `def`.`icon`,
      `existing`.`component` = `def`.`component`,
      `existing`.`component_name` = `def`.`component_name`,
      `existing`.`status` = `def`.`status`,
      `existing`.`visible` = `def`.`visible`,
      `existing`.`keep_alive` = `def`.`keep_alive`,
      `existing`.`always_show` = `def`.`always_show`,
      `existing`.`updater` = '1',
      `existing`.`update_time` = NOW()
  WHERE `existing`.`deleted` = b'0';

  DROP TEMPORARY TABLE IF EXISTS `tmp_form_center_menu_ids`;
  CREATE TEMPORARY TABLE `tmp_form_center_menu_ids` AS
  SELECT `id`
  FROM `system_menu`
  WHERE `deleted` = b'0'
    AND `id` IN (
      605071200, 605071201, 605071202, 605071203, 605071204, 605071205, 605071206,
      605071207, 605071208, 605071210, 605071211, 605071212, 605071213,
      605071214, 605071215, 605071216, 605071217, 605071218, 605071219, 605071220, 605071221
    );

  IF (SELECT COUNT(*) FROM `tmp_form_center_menu_ids`) <> 21 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing or duplicated form center menu rows';
  END IF;

  IF EXISTS (
    SELECT 1
    FROM `system_tenant_package` AS `package`
    WHERE `package`.`deleted` = b'0'
      AND NOT JSON_VALID(`package`.`menu_ids`)
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Invalid system_tenant_package.menu_ids JSON; cannot merge form center menus';
  END IF;

  DROP TEMPORARY TABLE IF EXISTS `tmp_form_center_package_menu_ids`;
  CREATE TEMPORARY TABLE `tmp_form_center_package_menu_ids` (
    `package_id` bigint NOT NULL,
    `menu_id` bigint NOT NULL,
    PRIMARY KEY (`package_id`, `menu_id`)
  );

  INSERT IGNORE INTO `tmp_form_center_package_menu_ids` (`package_id`, `menu_id`)
  SELECT
    `package`.`id`,
    CAST(`existing_menu`.`menu_id` AS UNSIGNED)
  FROM `system_tenant_package` AS `package`
  JOIN JSON_TABLE(
    `package`.`menu_ids`,
    '$[*]' COLUMNS (`menu_id` bigint PATH '$')
  ) AS `existing_menu`
  WHERE `package`.`deleted` = b'0'
    AND JSON_VALID(`package`.`menu_ids`)
    AND CAST(`existing_menu`.`menu_id` AS UNSIGNED) <> 605071209;

  INSERT IGNORE INTO `tmp_form_center_package_menu_ids` (`package_id`, `menu_id`)
  SELECT
    `package`.`id`,
    `form_menu`.`id`
  FROM `system_tenant_package` AS `package`
  CROSS JOIN `tmp_form_center_menu_ids` AS `form_menu`
  WHERE `package`.`deleted` = b'0'
    AND JSON_VALID(`package`.`menu_ids`)
    AND JSON_CONTAINS(`package`.`menu_ids`, CAST('1186' AS JSON), '$');

  UPDATE `system_tenant_package` AS `package`
  JOIN (
    SELECT DISTINCT
      `package_id`,
      JSON_ARRAYAGG(`menu_id`) OVER (
        PARTITION BY `package_id`
        ORDER BY `menu_id`
        ROWS BETWEEN UNBOUNDED PRECEDING AND UNBOUNDED FOLLOWING
      ) AS `menu_ids`
    FROM `tmp_form_center_package_menu_ids`
  ) AS `merged`
    ON `merged`.`package_id` = `package`.`id`
  SET `package`.`menu_ids` = `merged`.`menu_ids`,
      `package`.`updater` = '1',
      `package`.`update_time` = NOW();

  INSERT INTO `system_role_menu` (
    `role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`
  )
  SELECT
    `role`.`id`,
    `form_menu`.`id`,
    '1',
    NOW(),
    '1',
    NOW(),
    b'0',
    `role`.`tenant_id`
  FROM `system_role` AS `role`
  CROSS JOIN `tmp_form_center_menu_ids` AS `form_menu`
  LEFT JOIN `system_tenant` AS `tenant`
    ON `tenant`.`id` = `role`.`tenant_id`
   AND `tenant`.`deleted` = b'0'
  LEFT JOIN `system_tenant_package` AS `package`
    ON `package`.`id` = `tenant`.`package_id`
   AND `package`.`deleted` = b'0'
   AND JSON_VALID(`package`.`menu_ids`)
  WHERE `role`.`deleted` = b'0'
    AND (
      `role`.`code` = 'super_admin'
      OR (
        `role`.`code` = 'tenant_admin'
        AND `package`.`id` IS NOT NULL
        AND JSON_CONTAINS(`package`.`menu_ids`, CAST(`form_menu`.`id` AS JSON), '$')
      )
    )
    AND NOT EXISTS (
      SELECT 1
      FROM `system_role_menu` AS `existing`
      WHERE `existing`.`role_id` = `role`.`id`
        AND `existing`.`menu_id` = `form_menu`.`id`
        AND `existing`.`deleted` = b'0'
    );

  DROP TEMPORARY TABLE IF EXISTS `tmp_form_center_package_menu_ids`;
  DROP TEMPORARY TABLE IF EXISTS `tmp_form_center_menu_ids`;
  DROP TEMPORARY TABLE IF EXISTS `tmp_form_center_menu_defs`;
END//
DELIMITER ;

CALL ensure_bpm_form_center_menu();

DROP PROCEDURE IF EXISTS ensure_bpm_form_center_menu;
