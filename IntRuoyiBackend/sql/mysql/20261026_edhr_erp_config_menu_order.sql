-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20261026_erp_configuration_merged_menu,20261026_edhr_erp_form_menu_names; type=menu; riskLevel=low
-- Reorder the existing ERP configuration leaf only; role and tenant-package grants are unchanged.
SET NAMES utf8mb4;
DROP PROCEDURE IF EXISTS reorder_edhr_erp_config_menu;
DELIMITER $$
CREATE PROCEDURE reorder_edhr_erp_config_menu()
BEGIN
  DECLARE EXIT HANDLER FOR SQLEXCEPTION
  BEGIN
    ROLLBACK;
    RESIGNAL;
  END;
  START TRANSACTION;
  IF NOT EXISTS (SELECT 1 FROM system_menu WHERE id=991511 AND parent_id=900220
      AND path='/mes/edhr-data-sync' AND type=1 AND status=0 AND deleted=b'0')
    OR NOT EXISTS (SELECT 1 FROM system_menu WHERE id=6013 AND parent_id=991511
      AND name='ERP配置' AND path='/erp/kingdee-sync' AND component='erp/sync/index'
      AND type=2 AND status=0 AND visible=b'1' AND deleted=b'0')
    OR NOT EXISTS (SELECT 1 FROM system_menu WHERE id=5530 AND parent_id=991511
      AND name='生产订单' AND path='/mes/pro/work-order' AND sort=100
      AND type=2 AND status=0 AND visible=b'1' AND deleted=b'0') THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Missing or changed data sync / ERP configuration / production order menu';
  END IF;
  IF EXISTS (SELECT 1 FROM system_menu WHERE parent_id=991511 AND id NOT IN (6013,5530)
      AND type IN (1,2) AND status=0 AND visible=b'1' AND deleted=b'0' AND sort>=100) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Data sync order changed; production order must precede ERP configuration at the end';
  END IF;
  UPDATE system_menu SET sort=110,updater='edhr-erp-config-order',update_time=NOW() WHERE id=6013;
  COMMIT;
END$$
DELIMITER ;
CALL reorder_edhr_erp_config_menu();
DROP PROCEDURE reorder_edhr_erp_config_menu;
