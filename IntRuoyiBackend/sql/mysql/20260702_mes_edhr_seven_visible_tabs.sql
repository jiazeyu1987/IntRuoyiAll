-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260624_unified_approval_phase5_retire_legacy_menus,20260624_unified_electronic_signature_menu,20260630_approval_center_role_visibility; type=menu; riskLevel=medium
SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS ensure_mes_edhr_seven_visible_tabs;

DELIMITER //
CREATE PROCEDURE ensure_mes_edhr_seven_visible_tabs()
BEGIN
  IF EXISTS (
    SELECT 1
    FROM `system_tenant_package`
    WHERE `deleted` = b'0'
      AND NOT JSON_VALID(`menu_ids`)
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Invalid system_tenant_package.menu_ids JSON; cannot merge eDHR visible tabs';
  END IF;

  IF NOT EXISTS (
    SELECT 1
    FROM `system_menu`
    WHERE `id` = 900220
      AND `deleted` = b'0'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing eDHR batch processing parent menu 900220; cannot apply visible tabs';
  END IF;

  IF NOT EXISTS (
    SELECT 1
    FROM `system_menu`
    WHERE `id` = 1200
      AND `deleted` = b'0'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing approval center menu 1200; cannot fuse eDHR approval entry';
  END IF;

  IF NOT EXISTS (
    SELECT 1
    FROM `system_menu`
    WHERE `id` = 900412
      AND `deleted` = b'0'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing unified electronic signature batch-signature menu 900412; cannot unify eDHR signature tab';
  END IF;

  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_edhr_visible_tabs`;
  CREATE TEMPORARY TABLE `tmp_mes_edhr_visible_tabs` (
    `id` bigint NOT NULL PRIMARY KEY,
    `name` varchar(64) NOT NULL,
    `permission` varchar(128) NOT NULL,
    `sort` int NOT NULL,
    `path` varchar(255) NOT NULL,
    `icon` varchar(64) NOT NULL,
    `component` varchar(255) NOT NULL,
    `component_name` varchar(128) NOT NULL,
    `remark` varchar(255) NOT NULL
  );

  INSERT INTO `tmp_mes_edhr_visible_tabs`
  SELECT 900002 AS `id`, '模板与配置' AS `name`, 'mes:pro-batch-record-template:query' AS `permission`, 0 AS `sort`,
         '/mes/pro/batch-record-template' AS `path`, 'ep:document-copy' AS `icon`,
         'mes/pro/batchrecordtemplate/index' AS `component`, 'MesProBatchRecordTemplate' AS `component_name`,
         '电子批记录模板、Word 导入、签名位和单元格规则配置' AS `remark`
  UNION ALL
  SELECT 900365 AS `id`, '批记录表单' AS `name`, 'mes:pro-batch-record-template:query' AS `permission`, 1 AS `sort`,
         '/mes/pro/batch-record-form-list' AS `path`, 'ep:tickets' AS `icon`,
         'mes/pro/batchrecordformlist/index' AS `component`, 'MesProBatchRecordFormList' AS `component_name`,
         '按产品拆行展示批记录、损耗单、过程检验单和参数记录表模板并预览真实表单' AS `remark`
  UNION ALL
  SELECT 900033 AS `id`, '批次执行' AS `name`, 'mes:pro-edhr-batch-execution:query' AS `permission`, 2 AS `sort`,
         '/mes/pro/feedback/edhr-batch-execution' AS `path`, 'ep:document-checked' AS `icon`,
         'mes/pro/edhr-batch/BatchExecutionListPage' AS `component`, 'MesProEdhrBatchExecutionListPage' AS `component_name`,
         'eDHR 批次打开、执行、关闭、归档和历史入口' AS `remark`
  UNION ALL
  SELECT 900025 AS `id`, '审计与追溯' AS `name`, 'mes:pro-batch-record-execution:track' AS `permission`, 3 AS `sort`,
         '/mes/pro/feedback/edhr-tracking' AS `path`, 'ep:position' AS `icon`,
         'mes/pro/edhr/TrackingPage' AS `component`, 'MesProFeedbackEdhrTracking' AS `component_name`,
         '执行追踪、字段审计链、主数据追溯和操作审计从详情入口承接' AS `remark`
  UNION ALL
  SELECT 900235 AS `id`, '变更与异常' AS `name`, 'mes:pro-edhr-change:query' AS `permission`, 4 AS `sort`,
         '/mes/pro/feedback/edhr-change' AS `path`, 'ep:document' AS `icon`,
         'mes/pro/edhr/RecordChangePage' AS `component`, 'MesProFeedbackEdhrRecordChange' AS `component_name`,
         '重开、作废、补录、返工和异常处理统一入口' AS `remark`
  UNION ALL
  SELECT 900260 AS `id`, '放行追溯' AS `name`, 'mes:pro-edhr-release:query' AS `permission`, 5 AS `sort`,
         '/mes/pro/feedback/edhr-release' AS `path`, 'ep:finished' AS `icon`,
         'mes/pro/edhr-release/ReleasePage' AS `component`, 'MesProEdhrReleasePage' AS `component_name`,
         '放行检查项、审批事务事件和历史状态追溯' AS `remark`;

  IF (SELECT COUNT(*) FROM `tmp_mes_edhr_visible_tabs`) <> 6 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'eDHR batch processing visible tab contract must declare exactly six tabs';
  END IF;

  INSERT INTO `system_menu` (
    `id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`,
    `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`,
    `creator`, `create_time`, `updater`, `update_time`, `deleted`
  )
  SELECT `id`, `name`, `permission`, 2, `sort`, 900220, `path`, `icon`,
         `component`, `component_name`, 0, b'1', b'1', b'1',
         'edhr-seven-visible-tabs', NOW(), 'edhr-seven-visible-tabs', NOW(), b'0'
  FROM `tmp_mes_edhr_visible_tabs`
  ON DUPLICATE KEY UPDATE
    `name` = VALUES(`name`),
    `permission` = VALUES(`permission`),
    `type` = VALUES(`type`),
    `sort` = VALUES(`sort`),
    `parent_id` = VALUES(`parent_id`),
    `path` = VALUES(`path`),
    `icon` = VALUES(`icon`),
    `component` = VALUES(`component`),
    `component_name` = VALUES(`component_name`),
    `status` = VALUES(`status`),
    `visible` = VALUES(`visible`),
    `keep_alive` = VALUES(`keep_alive`),
    `always_show` = VALUES(`always_show`),
    `updater` = VALUES(`updater`),
    `update_time` = VALUES(`update_time`),
    `deleted` = VALUES(`deleted`);

  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_edhr_hidden_retained_tabs`;
  CREATE TEMPORARY TABLE `tmp_mes_edhr_hidden_retained_tabs` (
    `id` bigint NOT NULL PRIMARY KEY,
    `reason` varchar(255) NOT NULL
  );

  INSERT INTO `tmp_mes_edhr_hidden_retained_tabs`
  SELECT 900024 AS `id`, 'eDHR审批统一到审批中心一级页签，详情路由保留但菜单隐藏' AS `reason`
  UNION ALL SELECT 900026 AS `id`, 'eDHR签名记录统一到电子签名一级页签下的批记录签名记录，原功能保留但隐藏' AS `reason`
  UNION ALL SELECT 900230 AS `id`, 'eDHR工作任务保留为批次详情和待办承接能力' AS `reason`
  UNION ALL SELECT 900241 AS `id`, 'eDHR操作审计保留为详情页审计入口' AS `reason`
  UNION ALL SELECT 900243 AS `id`, 'eDHR对象权限保留为后台权限评估入口' AS `reason`
  UNION ALL SELECT 900266 AS `id`, 'eDHR流转单保留为后台能力' AS `reason`
  UNION ALL SELECT 900272 AS `id`, 'eDHR独立表单保留为后台能力' AS `reason`
  UNION ALL SELECT 900280 AS `id`, 'eDHR报表目录保留为后台报表能力' AS `reason`
  UNION ALL SELECT 900283 AS `id`, 'eDHR交付驾驶舱保留为交付后台能力' AS `reason`
  UNION ALL SELECT 900286 AS `id`, 'eDHR验证包矩阵保留为验证后台能力' AS `reason`
  UNION ALL SELECT 900290 AS `id`, 'eDHR DHR模板保留为模板后台能力' AS `reason`
  UNION ALL SELECT 900293 AS `id`, 'eDHR统一变更保留为变更后台能力' AS `reason`
  UNION ALL SELECT 900301 AS `id`, 'eDHR记录本保留为记录本后台能力' AS `reason`
  UNION ALL SELECT 900315 AS `id`, 'eDHR部署授权接口保留为部署后台能力' AS `reason`
  UNION ALL SELECT 900332 AS `id`, 'eDHR OQ/PQ保留为验证执行后台能力' AS `reason`
  UNION ALL SELECT 900338 AS `id`, 'eDHR打印策略保留为打印后台能力' AS `reason`
  UNION ALL SELECT 900356 AS `id`, 'eDHR流程干预管理保留为管理员干预能力' AS `reason`;

  UPDATE `system_menu` AS `menu`
  JOIN `tmp_mes_edhr_hidden_retained_tabs` AS `hidden_tab`
    ON `hidden_tab`.`id` = `menu`.`id`
  SET `menu`.`visible` = b'0',
      `menu`.`deleted` = b'0',
      `menu`.`updater` = 'edhr-seven-visible-tabs',
      `menu`.`update_time` = NOW();

  UPDATE `system_menu` AS `menu`
  JOIN `tmp_mes_edhr_visible_tabs` AS `visible_tab`
    ON `menu`.`id` = `visible_tab`.`id`
  SET `menu`.`name` = `visible_tab`.`name`,
      `menu`.`permission` = `visible_tab`.`permission`,
      `menu`.`type` = 2,
      `menu`.`sort` = `visible_tab`.`sort`,
      `menu`.`parent_id` = 900220,
      `menu`.`path` = `visible_tab`.`path`,
      `menu`.`icon` = `visible_tab`.`icon`,
      `menu`.`component` = `visible_tab`.`component`,
      `menu`.`component_name` = `visible_tab`.`component_name`,
      `menu`.`status` = 0,
      `menu`.`visible` = b'1',
      `menu`.`keep_alive` = b'1',
      `menu`.`always_show` = b'1',
      `menu`.`deleted` = b'0',
      `menu`.`updater` = 'edhr-seven-visible-tabs',
      `menu`.`update_time` = NOW()
  WHERE `menu`.`id` = `visible_tab`.`id`;

  UPDATE `system_menu`
  SET `type` = 3,
      `parent_id` = 900412,
      `path` = '',
      `component` = '',
      `component_name` = '',
      `visible` = b'0',
      `deleted` = b'0',
      `updater` = 'edhr-seven-visible-tabs',
      `update_time` = NOW()
  WHERE `id` = 900026;

  UPDATE `system_menu`
  SET `name` = 'eDHR审批',
      `type` = 2,
      `sort` = 99,
      `parent_id` = 1200,
      `path` = '/mes/pro/feedback/edhr-approval',
      `icon` = 'ep:stamp',
      `component` = 'mes/pro/edhr/ApprovalPage',
      `component_name` = 'MesProFeedbackEdhrApproval',
      `visible` = b'0',
      `deleted` = b'0',
      `updater` = 'edhr-seven-visible-tabs',
      `update_time` = NOW()
  WHERE `id` = 900024;

  -- 多余 eDHR 功能保留但隐藏：只隐藏菜单可见性，不删除 system_menu、权限、路由、页面或 API。
  UPDATE `system_menu` AS `menu`
  SET `menu`.`visible` = b'0',
      `menu`.`deleted` = b'0',
      `menu`.`updater` = 'edhr-seven-visible-tabs',
      `menu`.`update_time` = NOW()
  WHERE `menu`.`parent_id` = 900220
    AND `menu`.`type` = 2
    AND `menu`.`id` NOT IN (SELECT `id` FROM `tmp_mes_edhr_visible_tabs`);

  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_edhr_target_packages`;
  CREATE TEMPORARY TABLE `tmp_mes_edhr_target_packages` AS
  SELECT DISTINCT `package`.`id` AS `package_id`
  FROM `system_tenant_package` AS `package`
  WHERE `package`.`deleted` = b'0'
    AND JSON_VALID(`package`.`menu_ids`)
    AND JSON_CONTAINS(CAST(`package`.`menu_ids` AS JSON), CAST('900220' AS JSON), '$');

  UPDATE `system_tenant_package` AS `package`
  JOIN `tmp_mes_edhr_target_packages` AS `target`
    ON `target`.`package_id` = `package`.`id`
  SET `package`.`menu_ids` = CAST(JSON_ARRAY_APPEND(CAST(`package`.`menu_ids` AS JSON), '$', 900002) AS CHAR),
      `package`.`updater` = 'edhr-seven-visible-tabs',
      `package`.`update_time` = NOW()
  WHERE JSON_VALID(`package`.`menu_ids`)
    AND NOT JSON_CONTAINS(CAST(`package`.`menu_ids` AS JSON), CAST('900002' AS JSON), '$');

  UPDATE `system_tenant_package` AS `package`
  JOIN `tmp_mes_edhr_target_packages` AS `target`
    ON `target`.`package_id` = `package`.`id`
  SET `package`.`menu_ids` = CAST(JSON_ARRAY_APPEND(CAST(`package`.`menu_ids` AS JSON), '$', 900365) AS CHAR),
      `package`.`updater` = 'edhr-seven-visible-tabs',
      `package`.`update_time` = NOW()
  WHERE JSON_VALID(`package`.`menu_ids`)
    AND NOT JSON_CONTAINS(CAST(`package`.`menu_ids` AS JSON), CAST('900365' AS JSON), '$');

  UPDATE `system_tenant_package` AS `package`
  JOIN `tmp_mes_edhr_target_packages` AS `target`
    ON `target`.`package_id` = `package`.`id`
  SET `package`.`menu_ids` = CAST(JSON_ARRAY_APPEND(CAST(`package`.`menu_ids` AS JSON), '$', 900033) AS CHAR),
      `package`.`updater` = 'edhr-seven-visible-tabs',
      `package`.`update_time` = NOW()
  WHERE JSON_VALID(`package`.`menu_ids`)
    AND NOT JSON_CONTAINS(CAST(`package`.`menu_ids` AS JSON), CAST('900033' AS JSON), '$');

  UPDATE `system_tenant_package` AS `package`
  JOIN `tmp_mes_edhr_target_packages` AS `target`
    ON `target`.`package_id` = `package`.`id`
  SET `package`.`menu_ids` = CAST(JSON_ARRAY_APPEND(CAST(`package`.`menu_ids` AS JSON), '$', 900025) AS CHAR),
      `package`.`updater` = 'edhr-seven-visible-tabs',
      `package`.`update_time` = NOW()
  WHERE JSON_VALID(`package`.`menu_ids`)
    AND NOT JSON_CONTAINS(CAST(`package`.`menu_ids` AS JSON), CAST('900025' AS JSON), '$');

  UPDATE `system_tenant_package` AS `package`
  JOIN `tmp_mes_edhr_target_packages` AS `target`
    ON `target`.`package_id` = `package`.`id`
  SET `package`.`menu_ids` = CAST(JSON_ARRAY_APPEND(CAST(`package`.`menu_ids` AS JSON), '$', 900235) AS CHAR),
      `package`.`updater` = 'edhr-seven-visible-tabs',
      `package`.`update_time` = NOW()
  WHERE JSON_VALID(`package`.`menu_ids`)
    AND NOT JSON_CONTAINS(CAST(`package`.`menu_ids` AS JSON), CAST('900235' AS JSON), '$');

  UPDATE `system_tenant_package` AS `package`
  JOIN `tmp_mes_edhr_target_packages` AS `target`
    ON `target`.`package_id` = `package`.`id`
  SET `package`.`menu_ids` = CAST(JSON_ARRAY_APPEND(CAST(`package`.`menu_ids` AS JSON), '$', 900260) AS CHAR),
      `package`.`updater` = 'edhr-seven-visible-tabs',
      `package`.`update_time` = NOW()
  WHERE JSON_VALID(`package`.`menu_ids`)
    AND NOT JSON_CONTAINS(CAST(`package`.`menu_ids` AS JSON), CAST('900260' AS JSON), '$');

  INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`)
  SELECT DISTINCT `role_menu`.`role_id`, `visible_tab`.`id`,
         'edhr-seven-visible-tabs', NOW(), 'edhr-seven-visible-tabs', NOW()
  FROM `system_role_menu` AS `role_menu`
  JOIN `tmp_mes_edhr_visible_tabs` AS `visible_tab`
  LEFT JOIN `system_role_menu` AS `existing`
    ON `existing`.`role_id` = `role_menu`.`role_id`
   AND `existing`.`menu_id` = `visible_tab`.`id`
  WHERE `role_menu`.`menu_id` = 900220
    AND `existing`.`role_id` IS NULL;
END//
DELIMITER ;

CALL ensure_mes_edhr_seven_visible_tabs();
DROP PROCEDURE IF EXISTS ensure_mes_edhr_seven_visible_tabs;
