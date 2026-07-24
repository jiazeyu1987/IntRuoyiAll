-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260611_mes_edhr_batch_processing_tabs; type=menu; riskLevel=low
-- Purpose: Retire the obsolete eDHR execution list menu if it exists; batch execution is the replacement entry.
SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS ensure_mes_edhr_execution_list_hide_menu;

DELIMITER //
CREATE PROCEDURE ensure_mes_edhr_execution_list_hide_menu()
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM `system_menu`
    WHERE `id` = 900033
      AND `deleted` = b'0'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing eDHR batch execution replacement menu 900033; cannot retire obsolete eDHR execution list menu';
  END IF;

  UPDATE `system_menu`
  SET `path` = '/mes/pro/feedback/edhr-batch-execution',
      `component` = 'mes/pro/edhr-batch/BatchExecutionListPage',
      `component_name` = 'MesProEdhrBatchExecutionListPage',
      `visible` = b'1',
      `status` = 0,
      `deleted` = b'0',
      `updater` = '1',
      `update_time` = NOW()
  WHERE `id` = 900033;

  UPDATE `system_menu`
  SET `name` = '已废弃-eDHR执行列表',
      `permission` = 'RETIRED_EDHR_EXECUTION_LIST',
      `type` = 2,
      `sort` = 99,
      `parent_id` = 900220,
      `path` = '/retired/edhr-execution-list',
      `icon` = '',
      `component` = '',
      `component_name` = 'RETIRED_EDHR_EXECUTION_LIST',
      `status` = 1,
      `visible` = b'0',
      `keep_alive` = b'0',
      `always_show` = b'0',
      `deleted` = b'1',
      `updater` = '1',
      `update_time` = NOW()
  WHERE `id` = 900023;

  IF EXISTS (
    SELECT 1
    FROM `system_menu`
    WHERE `id` = 900023
      AND `deleted` = b'0'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Obsolete eDHR execution list menu 900023 must be retired';
  END IF;
END//
DELIMITER ;

CALL ensure_mes_edhr_execution_list_hide_menu();

DROP PROCEDURE IF EXISTS ensure_mes_edhr_execution_list_hide_menu;
