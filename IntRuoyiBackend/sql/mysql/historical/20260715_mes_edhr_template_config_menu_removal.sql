-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260714_mes_edhr_form_trace_menu,20260713_mes_edhr_form_fill_log_menu; type=menu; riskLevel=low
-- Remove the legacy eDHR "模板与配置" page entry while keeping its query permission for retained batch-record form features.
SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS ensure_mes_edhr_template_config_menu_removed;

DELIMITER $$
CREATE PROCEDURE ensure_mes_edhr_template_config_menu_removed()
BEGIN
  IF EXISTS (
    SELECT 1
    FROM `system_tenant_package`
    WHERE `deleted` = b'0'
      AND NOT JSON_VALID(`menu_ids`)
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Invalid system_tenant_package.menu_ids JSON; cannot remove eDHR template config menu';
  END IF;

  IF NOT EXISTS (
    SELECT 1
    FROM `system_menu`
    WHERE `id` = 900220
      AND `deleted` = b'0'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing eDHR parent menu 900220; cannot remove template config menu';
  END IF;

  UPDATE `system_menu`
  SET `name` = 'eDHR批记录',
      `updater` = 'edhr-template-config-removal',
      `update_time` = NOW()
  WHERE `id` = 900220
    AND `deleted` = b'0';

  IF NOT EXISTS (
    SELECT 1
    FROM `system_menu`
    WHERE `id` = 900002
      AND `deleted` = b'0'
      AND `permission` = 'mes:pro-batch-record-template:query'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing eDHR template config permission row 900002; cannot remove menu entry';
  END IF;

  IF EXISTS (
    SELECT 1
    FROM `system_menu`
    WHERE `id` = 900002
      AND `deleted` = b'0'
      AND `type` <> 3
      AND NOT (
        `parent_id` = 900220
        AND `name` IN ('模板与配置', '电子批记录')
        AND `path` = '/mes/pro/batch-record-template'
        AND `component` = 'mes/pro/batchrecordtemplate/index'
        AND `component_name` = 'MesProBatchRecordTemplate'
      )
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Menu 900002 is not the legacy eDHR template config page; refusing to mutate';
  END IF;

  UPDATE `system_menu`
  SET `name` = '模板与配置',
      `type` = 3,
      `sort` = 0,
      `parent_id` = 900220,
      `path` = '',
      `icon` = '',
      `component` = '',
      `component_name` = '',
      `status` = 0,
      `visible` = b'0',
      `keep_alive` = b'1',
      `always_show` = b'1',
      `deleted` = b'0',
      `updater` = 'edhr-template-config-removal',
      `update_time` = NOW()
  WHERE `id` = 900002
    AND `permission` = 'mes:pro-batch-record-template:query';

  IF (SELECT COUNT(*) FROM `system_menu`
      WHERE `deleted` = b'0'
        AND `id` IN (900365, 900033, 900025, 900235, 900260, 900432)) <> 6 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing retained eDHR visible menu rows; cannot reorder after template config removal';
  END IF;

  UPDATE `system_menu`
  SET `name` = CASE `id`
        WHEN 900365 THEN '批记录表单'
        WHEN 900033 THEN '批次执行'
        WHEN 900025 THEN '表单追溯'
        WHEN 900235 THEN '变更与异常'
        WHEN 900260 THEN '放行与归档'
        WHEN 900432 THEN '表单日志'
      END,
      `permission` = CASE `id`
        WHEN 900365 THEN 'mes:pro-batch-record-template:query'
        WHEN 900033 THEN 'mes:pro-edhr-batch-execution:query'
        WHEN 900025 THEN 'mes:pro-batch-record-execution:track'
        WHEN 900235 THEN 'mes:pro-edhr-change:query'
        WHEN 900260 THEN 'mes:pro-edhr-release:query'
        WHEN 900432 THEN 'mes:pro-edhr-form-fill-log:query'
      END,
      `sort` = CASE `id`
        WHEN 900365 THEN 0
        WHEN 900033 THEN 1
        WHEN 900025 THEN 2
        WHEN 900235 THEN 3
        WHEN 900260 THEN 4
        WHEN 900432 THEN 5
      END,
      `parent_id` = 900220,
      `type` = 2,
      `path` = CASE `id`
        WHEN 900365 THEN '/mes/pro/batch-record-form-list'
        WHEN 900033 THEN '/mes/pro/feedback/edhr-batch-execution'
        WHEN 900025 THEN '/mes/pro/feedback/edhr-form-trace'
        WHEN 900235 THEN '/mes/pro/feedback/edhr-change'
        WHEN 900260 THEN '/mes/pro/feedback/edhr-release'
        WHEN 900432 THEN '/mes/pro/feedback/edhr-form-fill-log'
      END,
      `icon` = CASE `id`
        WHEN 900365 THEN 'ep:tickets'
        WHEN 900033 THEN 'ep:document-checked'
        WHEN 900025 THEN 'ep:position'
        WHEN 900235 THEN 'ep:document'
        WHEN 900260 THEN 'ep:finished'
        WHEN 900432 THEN 'ep:document-copy'
      END,
      `component` = CASE `id`
        WHEN 900365 THEN 'mes/pro/batchrecordformlist/index'
        WHEN 900033 THEN 'mes/pro/edhr-batch/BatchExecutionListPage'
        WHEN 900025 THEN 'mes/pro/edhr/FormTracePage'
        WHEN 900235 THEN 'mes/pro/edhr/RecordChangePage'
        WHEN 900260 THEN 'mes/pro/edhr-release/ReleasePage'
        WHEN 900432 THEN 'mes/pro/edhr/FormFillLogPage'
      END,
      `component_name` = CASE `id`
        WHEN 900365 THEN 'MesProBatchRecordFormList'
        WHEN 900033 THEN 'MesProEdhrBatchExecutionListPage'
        WHEN 900025 THEN 'MesProFeedbackEdhrFormTrace'
        WHEN 900235 THEN 'MesProFeedbackEdhrRecordChange'
        WHEN 900260 THEN 'MesProEdhrReleasePage'
        WHEN 900432 THEN 'MesProEdhrFormFillLogPage'
      END,
      `status` = 0,
      `visible` = b'1',
      `keep_alive` = b'1',
      `always_show` = b'1',
      `deleted` = b'0',
      `updater` = 'edhr-template-config-removal',
      `update_time` = NOW()
  WHERE `id` IN (900365, 900033, 900025, 900235, 900260, 900432);

  IF (SELECT COUNT(*)
      FROM `system_menu`
      WHERE `deleted` = b'0'
        AND `parent_id` = 900220
        AND `type` = 2
        AND `visible` = b'1'
        AND (
          (`id` = 900365 AND `name` = '批记录表单' AND `sort` = 0 AND `path` = '/mes/pro/batch-record-form-list' AND `component` = 'mes/pro/batchrecordformlist/index')
          OR (`id` = 900033 AND `name` = '批次执行' AND `sort` = 1 AND `path` = '/mes/pro/feedback/edhr-batch-execution' AND `component` = 'mes/pro/edhr-batch/BatchExecutionListPage')
          OR (`id` = 900025 AND `name` = '表单追溯' AND `sort` = 2 AND `path` = '/mes/pro/feedback/edhr-form-trace' AND `component` = 'mes/pro/edhr/FormTracePage')
          OR (`id` = 900235 AND `name` = '变更与异常' AND `sort` = 3 AND `path` = '/mes/pro/feedback/edhr-change' AND `component` = 'mes/pro/edhr/RecordChangePage')
          OR (`id` = 900260 AND `name` = '放行与归档' AND `sort` = 4 AND `path` = '/mes/pro/feedback/edhr-release' AND `component` = 'mes/pro/edhr-release/ReleasePage')
          OR (`id` = 900432 AND `name` = '表单日志' AND `sort` = 5 AND `path` = '/mes/pro/feedback/edhr-form-fill-log' AND `component` = 'mes/pro/edhr/FormFillLogPage')
        )) <> 6 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Retained eDHR visible menu order is incomplete after template config removal';
  END IF;

  IF EXISTS (
    SELECT 1
    FROM `system_menu`
    WHERE `id` = 900002
      AND `deleted` = b'0'
      AND (`type` <> 3 OR `visible` <> b'0' OR `path` <> '' OR `component` <> '' OR `component_name` <> '')
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Legacy eDHR template config page route still visible after removal';
  END IF;
END $$
DELIMITER ;

CALL ensure_mes_edhr_template_config_menu_removed();
DROP PROCEDURE IF EXISTS ensure_mes_edhr_template_config_menu_removed;
