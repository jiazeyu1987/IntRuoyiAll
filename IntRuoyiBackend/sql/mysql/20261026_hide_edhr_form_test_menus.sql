-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20261026_edhr_lifecycle_directories; type=menu; riskLevel=low
-- Remove only the two navigation entries. Preserve existing role and tenant-package
-- grants for authorized business deep links and retain all business data.
SET NAMES utf8mb4;
DROP PROCEDURE IF EXISTS hide_edhr_form_test_menus;
DELIMITER $$
CREATE PROCEDURE hide_edhr_form_test_menus()
BEGIN
  DECLARE EXIT HANDLER FOR SQLEXCEPTION
  BEGIN
    ROLLBACK;
    RESIGNAL;
  END;
  START TRANSACTION;
  IF NOT EXISTS (SELECT 1 FROM system_menu WHERE id=900272
      AND path='/mes/pro/feedback/edhr-form' AND component='mes/pro/edhr-form/FormPage'
      AND type=2 AND deleted=b'0')
    OR NOT EXISTS (SELECT 1 FROM system_menu WHERE id=900440
      AND path='/mes/pro/feedback/edhr-batch-test'
      AND component='mes/pro/edhr-batch/BatchRecordTestPage'
      AND type=2 AND deleted=b'0') THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Missing or changed eDHR form / batch record test menu';
  END IF;
  UPDATE system_menu SET visible=b'0', updater='hide-edhr-form-test', update_time=NOW()
    WHERE id IN (900272,900440);
  COMMIT;
END$$
DELIMITER ;
CALL hide_edhr_form_test_menus();
DROP PROCEDURE hide_edhr_form_test_menus;
