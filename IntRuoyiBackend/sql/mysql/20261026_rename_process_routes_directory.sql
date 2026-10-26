-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20261026_edhr_lifecycle_directories; type=menu; riskLevel=low
-- Rename only directory 991515; preserve child menus, role and tenant-package grants.
SET NAMES utf8mb4;
DROP PROCEDURE IF EXISTS rename_process_routes_directory;
DELIMITER $$
CREATE PROCEDURE rename_process_routes_directory()
BEGIN
  DECLARE EXIT HANDLER FOR SQLEXCEPTION
  BEGIN
    ROLLBACK;
    RESIGNAL;
  END;
  START TRANSACTION;
  IF NOT EXISTS (SELECT 1 FROM system_menu WHERE id=991515
      AND path='/mes/edhr-process-routes' AND parent_id=900220
      AND type=1 AND deleted=b'0' AND name IN ('工艺流程','工艺路线')) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Missing or changed process routes directory';
  END IF;
  UPDATE system_menu SET name='工艺路线', updater='rename-route-directory', update_time=NOW()
    WHERE id=991515;
  COMMIT;
END$$
DELIMITER ;
CALL rename_process_routes_directory();
DROP PROCEDURE rename_process_routes_directory;
