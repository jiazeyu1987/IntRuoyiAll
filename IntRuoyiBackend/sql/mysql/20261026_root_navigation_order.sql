-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20261026_mes_smart_scheduling_root_menu,20261026_edhr_root_menu_business_order,20261026_showroom_master_data_menu; type=menu; riskLevel=low
-- Personal center is a static frontend route. Order existing dynamic roots after it.
-- Existing role and tenant-package grants are unchanged: no menu or permission is added.
SET NAMES utf8mb4;
DROP PROCEDURE IF EXISTS order_root_navigation;
DELIMITER $$
CREATE PROCEDURE order_root_navigation()
BEGIN
  DECLARE EXIT HANDLER FOR SQLEXCEPTION
  BEGIN
    ROLLBACK;
    RESIGNAL;
  END;
  START TRANSACTION;
  IF (SELECT COUNT(*) FROM system_menu WHERE parent_id=0 AND deleted=b'0' AND status=0
      AND ((id=1200 AND path='/approval-center') OR (id=990200 AND path='/mdm')
        OR (id=980100 AND path IN ('showroom','/showroom')) OR (id=6800 AND path='/dcc')
        OR (id=900120 AND path='/mes/smart-scheduling')
        OR (id=900220 AND path='/mes/edhr-batch-processing') OR (id=1 AND path='/system')
        OR (id=5100 AND path='/mes'))) <> 8 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Missing or changed root navigation menus';
  END IF;
  UPDATE system_menu SET sort=CASE id
      WHEN 1200 THEN 20 WHEN 990200 THEN 30 WHEN 980100 THEN 40 WHEN 6800 THEN 50
      WHEN 900120 THEN 60 WHEN 900220 THEN 70 WHEN 1 THEN 80 END,
    updater='root-navigation-order',update_time=NOW()
  WHERE id IN (1200,990200,980100,6800,900120,900220,1);
  UPDATE system_menu SET visible=b'0',updater='root-navigation-order',update_time=NOW()
  WHERE id=5100;
  COMMIT;
END$$
DELIMITER ;
CALL order_root_navigation();
DROP PROCEDURE order_root_navigation;
