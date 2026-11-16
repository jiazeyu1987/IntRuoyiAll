-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20261026_edhr_lifecycle_directories; type=menu; riskLevel=low
-- Align existing leaf labels with their ERP business objects; permissions, routes and data are unchanged.
SET NAMES utf8mb4;
DROP PROCEDURE IF EXISTS rename_edhr_erp_form_menu_names;
DELIMITER $$
CREATE PROCEDURE rename_edhr_erp_form_menu_names()
BEGIN
  DECLARE EXIT HANDLER FOR SQLEXCEPTION
  BEGIN
    ROLLBACK;
    RESIGNAL;
  END;
  DROP TEMPORARY TABLE IF EXISTS tmp_edhr_erp_menu_names;
  CREATE TEMPORARY TABLE tmp_edhr_erp_menu_names (
    id BIGINT PRIMARY KEY,
    old_name VARCHAR(50) CHARACTER SET utf8mb4 NOT NULL,
    name VARCHAR(50) CHARACTER SET utf8mb4 NOT NULL,
    path VARCHAR(200) CHARACTER SET utf8mb4 NOT NULL
  );
  INSERT INTO tmp_edhr_erp_menu_names VALUES
    (2565,'产品信息','物料','/mes/md/erp-product'),
    (2590,'产品库存','即时库存汇总','/mes/md/erp-stock'),
    (6032,'生产领料单列表','生产领料单','/erp/production/pick-list'),
    (605071322,'生产补料单列表','生产补料单','/erp/production/replenishment-list'),
    (5530,'生产工单','生产订单','/mes/pro/work-order');
  START TRANSACTION;
  IF EXISTS (SELECT 1 FROM tmp_edhr_erp_menu_names plan LEFT JOIN system_menu menu ON menu.id=plan.id
      WHERE menu.id IS NULL OR menu.deleted<>b'0' OR menu.type<>2 OR menu.parent_id<>991511
        OR BINARY menu.path<>BINARY plan.path
        OR (BINARY menu.name<>BINARY plan.old_name AND BINARY menu.name<>BINARY plan.name)) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='ERP source menu identity changed; refusing partial label updates';
  END IF;
  UPDATE system_menu menu JOIN tmp_edhr_erp_menu_names plan ON menu.id=plan.id
    SET menu.name=plan.name,menu.updater='edhr-erp-menu-name',menu.update_time=NOW();
  IF (SELECT COUNT(*) FROM system_menu menu JOIN tmp_edhr_erp_menu_names plan ON menu.id=plan.id
      WHERE BINARY menu.name=BINARY plan.name)<>5 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='ERP menu label postcondition failed';
  END IF;
  COMMIT;
  DROP TEMPORARY TABLE tmp_edhr_erp_menu_names;
END$$
DELIMITER ;
CALL rename_edhr_erp_form_menu_names();
DROP PROCEDURE rename_edhr_erp_form_menu_names;
