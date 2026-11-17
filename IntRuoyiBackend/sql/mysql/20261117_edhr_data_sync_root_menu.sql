-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20261026_edhr_data_sync_directory,20261026_root_navigation_order; type=menu; riskLevel=medium
-- Promote the existing data synchronization directory to the root navigation.
SET NAMES utf8mb4;
DROP PROCEDURE IF EXISTS migrate_edhr_data_sync_root_menu;
DELIMITER $$
CREATE PROCEDURE migrate_edhr_data_sync_root_menu()
BEGIN
  DECLARE EXIT HANDLER FOR SQLEXCEPTION
  BEGIN
    ROLLBACK;
    RESIGNAL;
  END;
  START TRANSACTION;
  IF NOT EXISTS (SELECT 1 FROM system_menu WHERE id=990200 AND parent_id=0
      AND name='基础数据' AND sort=30 AND type=1 AND deleted=b'0')
    OR NOT EXISTS (SELECT 1 FROM system_menu WHERE id=991511 AND name='数据同步'
      AND path='/mes/edhr-data-sync' AND type=1 AND status=0 AND deleted=b'0')
    OR EXISTS (SELECT 1 FROM system_menu WHERE deleted=b'0' AND id<>991511
      AND parent_id=0 AND sort=36) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Data sync root menu position is unavailable';
  END IF;
  UPDATE system_menu SET parent_id=0,sort=36,updater='edhr-sync-root',update_time=NOW()
  WHERE id=991511 AND parent_id=900220;
  IF NOT EXISTS (SELECT 1 FROM system_menu WHERE id=991511 AND parent_id=0
      AND sort=36 AND path='/mes/edhr-data-sync' AND deleted=b'0') THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Data sync root menu promotion failed';
  END IF;
  COMMIT;
END$$
DELIMITER ;
CALL migrate_edhr_data_sync_root_menu();
DROP PROCEDURE migrate_edhr_data_sync_root_menu;
