-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20261026_edhr_lifecycle_directories; type=menu; riskLevel=low
-- Remove the navigation entry only. Existing role and tenant-package grants remain
-- necessary for template queries and authorized business deep links.
SET NAMES utf8mb4;
DROP PROCEDURE IF EXISTS hide_form_template_menu;
DELIMITER $$
CREATE PROCEDURE hide_form_template_menu()
BEGIN
  DECLARE EXIT HANDLER FOR SQLEXCEPTION
  BEGIN
    ROLLBACK;
    RESIGNAL;
  END;
  START TRANSACTION;
  IF NOT EXISTS (SELECT 1 FROM system_menu WHERE id=605071201
      AND path='/mdm/form-center/template' AND component='form-center/template/index'
      AND type=2 AND deleted=b'0') THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Missing or changed form template menu';
  END IF;
  UPDATE system_menu SET visible=b'0', updater='hide-form-template', update_time=NOW()
    WHERE id=605071201;
  COMMIT;
END$$
DELIMITER ;
CALL hide_form_template_menu();
DROP PROCEDURE hide_form_template_menu;
