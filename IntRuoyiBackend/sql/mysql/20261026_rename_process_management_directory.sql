-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20261026_edhr_lifecycle_directories; type=menu; riskLevel=low
-- Rename only the parent directory. Child menu 5710 remains unchanged.
-- Existing role and tenant-package grants remain valid; no authorization changes.
SET NAMES utf8mb4;
DROP PROCEDURE IF EXISTS rename_process_management_directory;
DELIMITER $$
CREATE PROCEDURE rename_process_management_directory()
BEGIN
  DECLARE EXIT HANDLER FOR SQLEXCEPTION
  BEGIN
    ROLLBACK;
    RESIGNAL;
  END;
  START TRANSACTION;
  IF NOT EXISTS (SELECT 1 FROM system_menu WHERE id=991514
      AND path='/mes/edhr-process-settings' AND parent_id=900220
      AND type=1 AND deleted=b'0' AND name IN ('工序设置','工序管理')) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Missing or changed process settings directory';
  END IF;
  UPDATE system_menu SET name='工序管理', updater='rename-process-directory', update_time=NOW()
    WHERE id=991514;
  COMMIT;
END$$
DELIMITER ;
CALL rename_process_management_directory();
DROP PROCEDURE rename_process_management_directory;
