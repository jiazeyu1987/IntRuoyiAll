-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260624_unified_approval_phase5_retire_legacy_menus,20260630_approval_center_role_visibility; type=menu; riskLevel=medium
-- Consolidate visible workflow navigation into Approval Center.
-- legacy /bpm route is retained by frontend hidden routes; this migration only moves menu ownership.

SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS ensure_approval_center_workflow_menu_consolidation;

DELIMITER //
CREATE PROCEDURE ensure_approval_center_workflow_menu_consolidation()
BEGIN
  IF EXISTS (
    SELECT 1
    FROM `system_tenant_package` AS `package`
    WHERE `package`.`deleted` = b'0'
      AND NOT JSON_VALID(`package`.`menu_ids`)
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Invalid system_tenant_package.menu_ids JSON; cannot consolidate approval center workflow menus';
  END IF;

  IF NOT EXISTS (
    SELECT 1
    FROM `system_menu`
    WHERE `id` = 1185
      AND `name` = '工作流程'
      AND `parent_id` = 0
      AND `path` = '/bpm'
      AND `deleted` = b'0'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing workflow root menu 1185 or occupied by another meaning';
  END IF;

  IF NOT EXISTS (
    SELECT 1
    FROM `system_menu`
    WHERE `id` = 1200
      AND `name` = '审批中心'
      AND `parent_id` IN (0, 1185)
      AND `path` IN ('task', '/approval-center')
      AND `deleted` = b'0'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing approval center menu 1200 or occupied by another meaning';
  END IF;

  IF NOT EXISTS (
    SELECT 1
    FROM `system_menu`
    WHERE `id` = 1186
      AND `name` = '流程管理'
      AND `parent_id` IN (1185, 1200)
      AND `path` = 'manager'
      AND `deleted` = b'0'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing workflow management menu 1186 or occupied by another meaning';
  END IF;

  IF NOT EXISTS (
    SELECT 1
    FROM `system_menu`
    WHERE `id` = 5
      AND `name` = 'OA 示例'
      AND `parent_id` IN (1185, 1200)
      AND `path` = 'oa'
      AND `deleted` = b'0'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing OA example menu 5 or occupied by another meaning';
  END IF;

  IF (
    SELECT COUNT(*)
    FROM `system_menu`
    WHERE `id` IN (1207, 1208, 1201, 2713, 1118)
      AND `deleted` = b'0'
  ) <> 5 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing approval center task menus; cannot preserve BPM task navigation';
  END IF;

  IF NOT EXISTS (
    SELECT 1
    FROM `system_menu`
    WHERE `permission` = 'bpm:task:query'
      AND `deleted` = b'0'
  ) OR NOT EXISTS (
    SELECT 1
    FROM `system_menu`
    WHERE `permission` = 'bpm:task:update'
      AND `deleted` = b'0'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing BPM task permission nodes bpm:task:query / bpm:task:update';
  END IF;

  DROP TEMPORARY TABLE IF EXISTS `tmp_approval_center_workflow_required_menu_ids`;
  CREATE TEMPORARY TABLE `tmp_approval_center_workflow_required_menu_ids` (
    `menu_id` bigint NOT NULL PRIMARY KEY
  );

  INSERT INTO `tmp_approval_center_workflow_required_menu_ids` (`menu_id`)
  SELECT 1200 AS `menu_id`
  UNION ALL SELECT 1186
  UNION ALL SELECT 1193
  UNION ALL SELECT 1194
  UNION ALL SELECT 1195
  UNION ALL SELECT 1197
  UNION ALL SELECT 1198
  UNION ALL SELECT 1199
  UNION ALL SELECT 2913
  UNION ALL SELECT 1187
  UNION ALL SELECT 1188
  UNION ALL SELECT 1189
  UNION ALL SELECT 1190
  UNION ALL SELECT 1191
  UNION ALL SELECT 1192
  UNION ALL SELECT 2714
  UNION ALL SELECT 2715
  UNION ALL SELECT 2716
  UNION ALL SELECT 2717
  UNION ALL SELECT 2718
  UNION ALL SELECT 1209
  UNION ALL SELECT 1210
  UNION ALL SELECT 1211
  UNION ALL SELECT 1212
  UNION ALL SELECT 1213
  UNION ALL SELECT 2731
  UNION ALL SELECT 2732
  UNION ALL SELECT 2733
  UNION ALL SELECT 2734
  UNION ALL SELECT 2735
  UNION ALL SELECT 1207
  UNION ALL SELECT 1221
  UNION ALL SELECT 1222
  UNION ALL SELECT 1208
  UNION ALL SELECT 1201
  UNION ALL SELECT 1202
  UNION ALL SELECT 1219
  UNION ALL SELECT 1220
  UNION ALL SELECT 2713
  UNION ALL SELECT 5
  UNION ALL SELECT 1118
  UNION ALL SELECT 1119
  UNION ALL SELECT 1120;

  IF EXISTS (
    SELECT 1
    FROM `tmp_approval_center_workflow_required_menu_ids` AS `required_menu`
    LEFT JOIN `system_menu` AS `menu`
      ON `menu`.`id` = `required_menu`.`menu_id`
     AND `menu`.`deleted` = b'0'
    WHERE `menu`.`id` IS NULL
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing approval center workflow required menu ids; cannot update tenant packages';
  END IF;

  UPDATE `system_menu`
  SET `type` = 1,
      `sort` = 50,
      `parent_id` = 0,
      `path` = '/approval-center',
      `icon` = 'ep:finished',
      `component` = NULL,
      `component_name` = NULL,
      `visible` = b'1',
      `keep_alive` = b'1',
      `always_show` = b'1',
      `deleted` = b'0',
      `updater` = 'approval-center-workflow-consolidation',
      `update_time` = NOW()
  WHERE `id` = 1200;

  UPDATE `system_menu`
  SET `visible` = b'0',
      `always_show` = b'0',
      `deleted` = b'0',
      `updater` = 'approval-center-workflow-consolidation',
      `update_time` = NOW()
  WHERE `id` = 1185;

  UPDATE `system_menu`
  SET `name` = '流程管理',
      `type` = 1,
      `sort` = 10,
      `parent_id` = 1200,
      `path` = 'manager',
      `icon` = 'fa:dedent',
      `component` = NULL,
      `component_name` = NULL,
      `visible` = b'1',
      `keep_alive` = b'1',
      `always_show` = b'1',
      `deleted` = b'0',
      `updater` = 'approval-center-workflow-consolidation',
      `update_time` = NOW()
  WHERE `id` = 1186;

  UPDATE `system_menu`
  SET `name` = '流程模型',
      `type` = 2,
      `sort` = 1,
      `parent_id` = 1186,
      `path` = 'model',
      `component` = 'bpm/model/index',
      `component_name` = 'BpmModel',
      `visible` = b'1',
      `deleted` = b'0',
      `updater` = 'approval-center-workflow-consolidation',
      `update_time` = NOW()
  WHERE `id` = 1193;

  UPDATE `system_menu`
  SET `name` = '流程表单',
      `type` = 2,
      `sort` = 2,
      `parent_id` = 1186,
      `path` = 'form',
      `component` = 'bpm/form/index',
      `component_name` = 'BpmForm',
      `visible` = b'1',
      `deleted` = b'0',
      `updater` = 'approval-center-workflow-consolidation',
      `update_time` = NOW()
  WHERE `id` = 1187;

  UPDATE `system_menu`
  SET `name` = '流程分类',
      `type` = 2,
      `sort` = 3,
      `parent_id` = 1186,
      `path` = 'category',
      `component` = 'bpm/category/index',
      `component_name` = 'BpmCategory',
      `visible` = b'1',
      `deleted` = b'0',
      `updater` = 'approval-center-workflow-consolidation',
      `update_time` = NOW()
  WHERE `id` = 2714;

  UPDATE `system_menu`
  SET `name` = '用户分组',
      `type` = 2,
      `sort` = 4,
      `parent_id` = 1186,
      `path` = 'user-group',
      `component` = 'bpm/group/index',
      `component_name` = 'BpmUserGroup',
      `visible` = b'1',
      `deleted` = b'0',
      `updater` = 'approval-center-workflow-consolidation',
      `update_time` = NOW()
  WHERE `id` = 1209;

  UPDATE `system_menu`
  SET `name` = '流程表达式',
      `type` = 2,
      `sort` = 5,
      `parent_id` = 1186,
      `path` = 'process-expression',
      `component` = 'bpm/processExpression/index',
      `component_name` = 'BpmProcessExpression',
      `visible` = b'1',
      `deleted` = b'0',
      `updater` = 'approval-center-workflow-consolidation',
      `update_time` = NOW()
  WHERE `id` = 2731;

  UPDATE `system_menu`
  SET `visible` = b'0',
      `deleted` = b'0',
      `updater` = 'approval-center-workflow-consolidation',
      `update_time` = NOW()
  WHERE `id` IN (2720, 2721, 2724, 2726);

  UPDATE `system_menu`
  SET `name` = '待办',
      `type` = 2,
      `sort` = 20,
      `parent_id` = 1200,
      `path` = 'todo',
      `icon` = 'fa:slack',
      `component` = 'approval-center/index',
      `component_name` = 'ApprovalCenterTodo',
      `visible` = b'1',
      `keep_alive` = b'1',
      `always_show` = b'1',
      `deleted` = b'0',
      `updater` = 'approval-center-workflow-consolidation',
      `update_time` = NOW()
  WHERE `id` = 1207;

  UPDATE `system_menu`
  SET `name` = '已办',
      `type` = 2,
      `sort` = 30,
      `parent_id` = 1200,
      `path` = 'done',
      `icon` = 'fa:delicious',
      `component` = 'approval-center/index',
      `component_name` = 'ApprovalCenterDone',
      `visible` = b'1',
      `keep_alive` = b'1',
      `always_show` = b'1',
      `deleted` = b'0',
      `updater` = 'approval-center-workflow-consolidation',
      `update_time` = NOW()
  WHERE `id` = 1208;

  UPDATE `system_menu`
  SET `name` = '我发起的',
      `type` = 2,
      `sort` = 40,
      `parent_id` = 1200,
      `path` = 'my-initiated',
      `icon` = 'fa-solid:book',
      `component` = 'approval-center/index',
      `component_name` = 'ApprovalCenterMyInitiated',
      `visible` = b'1',
      `keep_alive` = b'1',
      `always_show` = b'1',
      `deleted` = b'0',
      `updater` = 'approval-center-workflow-consolidation',
      `update_time` = NOW()
  WHERE `id` = 1201;

  UPDATE `system_menu`
  SET `name` = '抄送我的',
      `type` = 2,
      `sort` = 50,
      `parent_id` = 1200,
      `path` = 'cc',
      `icon` = 'ep:copy-document',
      `component` = 'approval-center/index',
      `component_name` = 'ApprovalCenterCc',
      `visible` = b'1',
      `keep_alive` = b'1',
      `always_show` = b'1',
      `deleted` = b'0',
      `updater` = 'approval-center-workflow-consolidation',
      `update_time` = NOW()
  WHERE `id` = 2713;

  UPDATE `system_menu`
  SET `name` = 'OA 示例',
      `type` = 1,
      `sort` = 60,
      `parent_id` = 1200,
      `path` = 'oa',
      `icon` = 'fa:road',
      `component` = NULL,
      `component_name` = NULL,
      `visible` = b'0',
      `keep_alive` = b'1',
      `always_show` = b'0',
      `deleted` = b'0',
      `updater` = 'approval-center-workflow-consolidation',
      `update_time` = NOW()
  WHERE `id` = 5;

  UPDATE `system_menu`
  SET `name` = '请假查询',
      `type` = 2,
      `sort` = 10,
      `parent_id` = 5,
      `path` = 'leave',
      `component` = 'bpm/oa/leave/index',
      `component_name` = 'BpmOALeave',
      `visible` = b'0',
      `deleted` = b'0',
      `updater` = 'approval-center-workflow-consolidation',
      `update_time` = NOW()
  WHERE `id` = 1118;

  DROP TEMPORARY TABLE IF EXISTS `tmp_approval_center_workflow_target_packages`;
  CREATE TEMPORARY TABLE `tmp_approval_center_workflow_target_packages` AS
  SELECT DISTINCT `package`.`id` AS `package_id`
  FROM `system_tenant_package` AS `package`
  WHERE `package`.`deleted` = b'0'
    AND JSON_VALID(`package`.`menu_ids`)
    AND (
      JSON_CONTAINS(CAST(`package`.`menu_ids` AS JSON), CAST('1185' AS JSON), '$')
      OR JSON_CONTAINS(CAST(`package`.`menu_ids` AS JSON), CAST('1200' AS JSON), '$')
      OR JSON_CONTAINS(CAST(`package`.`menu_ids` AS JSON), CAST('1186' AS JSON), '$')
      OR JSON_CONTAINS(CAST(`package`.`menu_ids` AS JSON), CAST('5' AS JSON), '$')
    );

  DROP TEMPORARY TABLE IF EXISTS `tmp_approval_center_workflow_package_menu_ids`;
  CREATE TEMPORARY TABLE `tmp_approval_center_workflow_package_menu_ids` (
    `package_id` bigint NOT NULL,
    `menu_id` bigint NOT NULL,
    PRIMARY KEY (`package_id`, `menu_id`)
  );

  INSERT IGNORE INTO `tmp_approval_center_workflow_package_menu_ids` (`package_id`, `menu_id`)
  SELECT
    `target`.`package_id`,
    CAST(`existing_menu`.`menu_id` AS UNSIGNED)
  FROM `tmp_approval_center_workflow_target_packages` AS `target`
  JOIN `system_tenant_package` AS `package`
    ON `package`.`id` = `target`.`package_id`
  JOIN JSON_TABLE(
    `package`.`menu_ids`,
    '$[*]' COLUMNS (`menu_id` bigint PATH '$')
  ) AS `existing_menu`
  WHERE `package`.`deleted` = b'0'
    AND JSON_VALID(`package`.`menu_ids`)
    AND `existing_menu`.`menu_id` <> 1185;

  INSERT IGNORE INTO `tmp_approval_center_workflow_package_menu_ids` (`package_id`, `menu_id`)
  SELECT
    `target`.`package_id`,
    `required_menu`.`menu_id`
  FROM `tmp_approval_center_workflow_target_packages` AS `target`
  CROSS JOIN `tmp_approval_center_workflow_required_menu_ids` AS `required_menu`;

  UPDATE `system_tenant_package` AS `package`
  JOIN (
    SELECT DISTINCT
      `package_id`,
      JSON_ARRAYAGG(`menu_id`) OVER (
        PARTITION BY `package_id`
        ORDER BY `menu_id`
        ROWS BETWEEN UNBOUNDED PRECEDING AND UNBOUNDED FOLLOWING
      ) AS `menu_ids`
    FROM `tmp_approval_center_workflow_package_menu_ids`
  ) AS `merged`
    ON `merged`.`package_id` = `package`.`id`
  SET `package`.`menu_ids` = CAST(`merged`.`menu_ids` AS CHAR),
      `package`.`updater` = 'approval-center-workflow-consolidation',
      `package`.`update_time` = NOW();

  DROP TEMPORARY TABLE IF EXISTS `tmp_approval_center_workflow_package_menu_ids`;
  DROP TEMPORARY TABLE IF EXISTS `tmp_approval_center_workflow_target_packages`;
  DROP TEMPORARY TABLE IF EXISTS `tmp_approval_center_workflow_required_menu_ids`;
END//
DELIMITER ;

CALL ensure_approval_center_workflow_menu_consolidation();

DROP PROCEDURE IF EXISTS ensure_approval_center_workflow_menu_consolidation;
