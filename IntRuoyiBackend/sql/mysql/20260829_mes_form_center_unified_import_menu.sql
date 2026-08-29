-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260804_mes_edhr_qa_menu; type=menu; riskLevel=low
-- Purpose: rename the MES unified import tab to Form Center without changing route, component, permission, or binding.

SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS `ensure_mes_form_center_unified_import_menu`;
DELIMITER //

CREATE PROCEDURE `ensure_mes_form_center_unified_import_menu`()
BEGIN
  IF NOT EXISTS (
    SELECT 1
      FROM `system_menu`
     WHERE `id` = 900365
       AND `deleted` = b'0'
       AND `visible` = b'1'
       AND `path` = '/mes/pro/batch-record-form-list'
       AND `component` = 'mes/pro/batchrecordformlist/index'
       AND `component_name` = 'MesProBatchRecordFormList'
       AND `permission` = 'mes:pro-batch-record-template:query'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing MES batch record form list menu 900365; cannot rename unified import tab';
  END IF;

  UPDATE `system_menu`
     SET `name` = CONVERT(UNHEX('E8A1A8E58D95E4B8ADE5BF83') USING utf8mb4) COLLATE utf8mb4_unicode_ci,
         `updater` = 'codex',
         `update_time` = NOW()
   WHERE `id` = 900365
     AND `deleted` = b'0'
     AND `visible` = b'1'
     AND `path` = '/mes/pro/batch-record-form-list'
     AND `component` = 'mes/pro/batchrecordformlist/index'
     AND `component_name` = 'MesProBatchRecordFormList'
     AND `permission` = 'mes:pro-batch-record-template:query';

  IF NOT EXISTS (
    SELECT 1
      FROM `system_menu`
     WHERE `id` = 900365
       AND `deleted` = b'0'
       AND `visible` = b'1'
       AND `name` = CONVERT(UNHEX('E8A1A8E58D95E4B8ADE5BF83') USING utf8mb4) COLLATE utf8mb4_unicode_ci
       AND `path` = '/mes/pro/batch-record-form-list'
       AND `component` = 'mes/pro/batchrecordformlist/index'
       AND `component_name` = 'MesProBatchRecordFormList'
       AND `permission` = 'mes:pro-batch-record-template:query'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'MES batch record form list menu rename failed';
  END IF;
END//

DELIMITER ;

CALL `ensure_mes_form_center_unified_import_menu`();
DROP PROCEDURE IF EXISTS `ensure_mes_form_center_unified_import_menu`;
