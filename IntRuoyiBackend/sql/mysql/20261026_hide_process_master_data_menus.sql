-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20261026_edhr_lifecycle_directories; type=menu; riskLevel=low
-- Navigation visibility only. Preserve role and tenant-package grants, ERP sync,
-- business data and authorized business deep links.
SET NAMES utf8mb4;
DROP PROCEDURE IF EXISTS hide_process_master_data_menus;
DELIMITER $$
CREATE PROCEDURE hide_process_master_data_menus()
BEGIN
  DECLARE EXIT HANDLER FOR SQLEXCEPTION
  BEGIN
    ROLLBACK;
    RESIGNAL;
  END;
  START TRANSACTION;
  IF (SELECT COUNT(*) FROM system_menu WHERE type=2 AND deleted=b'0' AND (
      (id=5110 AND path='/mes/md/item-type' AND component='mes/md/item/type/index')
      OR (id=5130 AND path='/mes/md/unit-measure' AND component='mes/md/unitmeasure/index')
      OR (id=5120 AND path='/mes/md/item' AND component='mes/md/item/index')
      OR (id=5718 AND path='/mes/pro/mes-process' AND component='mes/pro/mes-process/index')
    ))<>4 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Missing or changed process master data menus';
  END IF;
  UPDATE system_menu SET visible=b'0', updater='hide-process-master-data', update_time=NOW()
    WHERE id IN (5110,5130,5120,5718);
  COMMIT;
END$$
DELIMITER ;
CALL hide_process_master_data_menus();
DROP PROCEDURE hide_process_master_data_menus;
