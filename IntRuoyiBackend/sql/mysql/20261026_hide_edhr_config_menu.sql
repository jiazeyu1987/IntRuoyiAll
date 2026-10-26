-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20261026_edhr_lifecycle_directories; type=menu; riskLevel=low
-- Remove the navigation tab only; retain existing role and tenant-package grants
-- for the shared golden-finger permission and existing configuration data.
SET NAMES utf8mb4;
DROP PROCEDURE IF EXISTS hide_edhr_config_menu;
DELIMITER $$
CREATE PROCEDURE hide_edhr_config_menu()
BEGIN
  DECLARE EXIT HANDLER FOR SQLEXCEPTION
  BEGIN
    ROLLBACK;
    RESIGNAL;
  END;
  START TRANSACTION;
  IF NOT EXISTS (SELECT 1 FROM system_menu WHERE id=991501
      AND component='mes/pro/edhr-config/index'
      AND type=2 AND deleted=b'0') THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Missing or changed eDHR config menu';
  END IF;
  UPDATE system_menu SET visible=b'0', updater='hide-edhr-config', update_time=NOW()
    WHERE id=991501;
  COMMIT;
END$$
DELIMITER ;
CALL hide_edhr_config_menu();
DROP PROCEDURE hide_edhr_config_menu;
