-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20261026_edhr_device_management_directory; type=menu; riskLevel=low
-- Place equipment immediately after form trace at the same navigation level.
-- Existing role and tenant-package grants are unchanged; only sibling sort changes.
SET NAMES utf8mb4;
DROP PROCEDURE IF EXISTS reorder_edhr_device_menu;
DELIMITER $$
CREATE PROCEDURE reorder_edhr_device_menu()
BEGIN
  DECLARE EXIT HANDLER FOR SQLEXCEPTION
  BEGIN
    ROLLBACK;
    RESIGNAL;
  END;
  START TRANSACTION;
  IF (SELECT COUNT(*) FROM system_menu WHERE parent_id=900220 AND type=1
      AND status=0 AND deleted=b'0'
      AND ((id=5300 AND path='/mes/dv')
        OR (id=991515 AND path='/mes/edhr-process-routes')
        OR (id=991516 AND path='/mes/edhr-production-execution')
        OR (id=991517 AND path='/mes/edhr-form-trace')))<>4 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Missing or changed eDHR sibling directories';
  END IF;
  UPDATE system_menu
    SET sort=CASE id WHEN 991515 THEN 4 WHEN 991516 THEN 5 WHEN 991517 THEN 6 WHEN 5300 THEN 7 END,
        updater='edhr-device-order',update_time=NOW()
    WHERE id IN (991515,991516,991517,5300);
  COMMIT;
END$$
DELIMITER ;
CALL reorder_edhr_device_menu();
DROP PROCEDURE reorder_edhr_device_menu;
