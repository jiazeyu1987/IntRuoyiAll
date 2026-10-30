-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20261026_edhr_erp_form_menu_names; type=menu; riskLevel=low
-- Rename the existing menu only; role and tenant-package grants remain unchanged.
SET NAMES utf8mb4;
DROP PROCEDURE IF EXISTS rename_erp_material_list_menu;
DELIMITER $$
CREATE PROCEDURE rename_erp_material_list_menu()
BEGIN
  DECLARE EXIT HANDLER FOR SQLEXCEPTION
  BEGIN
    ROLLBACK;
    RESIGNAL;
  END;
  START TRANSACTION;
  IF (SELECT COUNT(*) FROM system_menu WHERE id=2565 AND deleted=b'0'
      AND type=2 AND component='erp/product/product/index'
      AND name IN ('产品信息','物料','物料列表')) <> 1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Material menu identity mismatch';
  END IF;
  UPDATE system_menu SET name='物料列表', updater='material-list-menu', update_time=NOW()
    WHERE id=2565;
  COMMIT;
END$$
DELIMITER ;
CALL rename_erp_material_list_menu();
DROP PROCEDURE rename_erp_material_list_menu;
