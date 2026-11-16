-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20261026_edhr_lifecycle_directories; type=menu; riskLevel=low
-- Remove the form-trace child navigation entry and rename the history entry.
-- Preserve the parent directory, business data, role and tenant-package grants
-- and authorized deep links used by existing business pages.
SET NAMES utf8mb4;
DROP PROCEDURE IF EXISTS simplify_form_trace_menus;
DELIMITER $$
CREATE PROCEDURE simplify_form_trace_menus()
BEGIN
  DECLARE EXIT HANDLER FOR SQLEXCEPTION
  BEGIN
    ROLLBACK;
    RESIGNAL;
  END;
  START TRANSACTION;
  IF NOT EXISTS (SELECT 1 FROM system_menu WHERE id=900025 AND parent_id=991517
      AND path='/mes/pro/feedback/edhr-form-trace' AND component='mes/pro/edhr/FormTracePage'
      AND type=2 AND deleted=b'0')
    OR NOT EXISTS (SELECT 1 FROM system_menu WHERE id=991510 AND parent_id=991517
      AND path='/mes/pro/feedback/edhr-batch-history'
      AND component='mes/pro/edhr-batch/BatchRecordHistoryPage'
      AND name IN ('eDHR历史追溯','历史追溯') AND type=2 AND deleted=b'0') THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Missing or changed form trace / history menus';
  END IF;
  UPDATE system_menu SET visible=b'0', updater='simplify-form-trace', update_time=NOW()
    WHERE id=900025;
  UPDATE system_menu SET name='历史追溯', updater='simplify-form-trace', update_time=NOW()
    WHERE id=991510;
  COMMIT;
END$$
DELIMITER ;
CALL simplify_form_trace_menus();
DROP PROCEDURE simplify_form_trace_menus;
