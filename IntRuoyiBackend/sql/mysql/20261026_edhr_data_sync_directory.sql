-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20261026_edhr_root_menu_business_order; type=menu; riskLevel=medium
-- Group only the ten existing ERP synchronization pages; preserve every leaf contract and grant.
SET NAMES utf8mb4;
DROP PROCEDURE IF EXISTS migrate_edhr_data_sync_directory;
DELIMITER $$
CREATE PROCEDURE migrate_edhr_data_sync_directory()
BEGIN
  DECLARE EXIT HANDLER FOR SQLEXCEPTION
  BEGIN
    ROLLBACK;
    RESIGNAL;
  END;
  DROP TEMPORARY TABLE IF EXISTS tmp_edhr_sync_pages;
  CREATE TEMPORARY TABLE tmp_edhr_sync_pages (
    id BIGINT PRIMARY KEY, name VARCHAR(50) CHARACTER SET utf8mb4,
    path VARCHAR(200) CHARACTER SET utf8mb4, component VARCHAR(255) CHARACTER SET utf8mb4, sort INT
  );
  INSERT INTO tmp_edhr_sync_pages VALUES
    (6013,'金蝶同步运行','/erp/kingdee-sync','erp/sync/index',10),
    (991502,'自动同步配置','/erp/sync/config','erp/sync/config/index',20),
    (2565,'产品信息','/mes/md/erp-product','erp/product/product/index',30),
    (2590,'产品库存','/mes/md/erp-stock','erp/stock/stock/index',40),
    (6023,'物料清单','/erp/production/bom-list','erp/production/bom-list/index',50),
    (6021,'生产用料清单','/erp/production/material-list','erp/production/material-list/index',60),
    (6032,'生产领料单列表','/erp/production/pick-list','erp/production/pick-list/index',70),
    (605071318,'生产补料单列表','/erp/production/replenishment-list','erp/production/replenishment-list/index',80),
    (6025,'即时库存','/erp/production/inventory-list','erp/production/inventory-list/index',90),
    (5530,'生产工单','/mes/pro/work-order','mes/pro/workorder/index',100);
  START TRANSACTION;
  IF NOT EXISTS (SELECT 1 FROM system_menu WHERE id=900220 AND parent_id=0
      AND path='/mes/edhr-batch-processing' AND type=1 AND status=0 AND deleted=b'0') THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Missing promoted eDHR root directory';
  END IF;
  IF EXISTS (SELECT 1 FROM tmp_edhr_sync_pages p LEFT JOIN system_menu m ON m.id=p.id
      WHERE m.id IS NULL OR m.deleted<>b'0' OR m.status<>0 OR m.type<>2
      OR BINARY m.name<>BINARY p.name OR BINARY m.path<>BINARY p.path
      OR BINARY m.component<>BINARY p.component OR m.parent_id NOT IN (900220,991511)) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='ERP synchronization page identity changed';
  END IF;
  IF EXISTS (SELECT 1 FROM system_menu WHERE id=991511 AND NOT (
      name='数据同步' AND parent_id=900220 AND path='/mes/edhr-data-sync' AND type=1
      AND permission='' AND component='' AND status=0 AND deleted=b'0'))
    OR EXISTS (SELECT 1 FROM system_menu WHERE deleted=b'0' AND id<>991511
      AND path='/mes/edhr-data-sync') THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Data sync directory identity occupied';
  END IF;
  IF EXISTS (SELECT 1 FROM system_tenant_package WHERE deleted=b'0'
      AND (NOT JSON_VALID(menu_ids) OR JSON_TYPE(CAST(menu_ids AS JSON))<>'ARRAY')) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Invalid tenant package menu_ids';
  END IF;
  INSERT INTO system_menu
    (id,name,permission,type,sort,parent_id,path,icon,component,component_name,status,
     visible,keep_alive,always_show,creator,create_time,updater,update_time,deleted)
  SELECT 991511,'数据同步','',1,-1,900220,'/mes/edhr-data-sync','ep:refresh','','',0,
     b'1',b'1',b'1','edhr-sync-group',NOW(),'edhr-sync-group',NOW(),b'0'
  WHERE NOT EXISTS (SELECT 1 FROM system_menu WHERE id=991511);
  UPDATE system_menu m JOIN tmp_edhr_sync_pages p ON m.id=p.id
    SET m.parent_id=991511,m.sort=p.sort,m.updater='edhr-sync-group',m.update_time=NOW();
  DROP TEMPORARY TABLE IF EXISTS tmp_edhr_sync_descendants;
  CREATE TEMPORARY TABLE tmp_edhr_sync_descendants (id BIGINT PRIMARY KEY);
  INSERT INTO tmp_edhr_sync_descendants
  WITH RECURSIVE descendants AS (
    SELECT id FROM tmp_edhr_sync_pages
    UNION ALL
    SELECT m.id FROM system_menu m JOIN descendants d ON m.parent_id=d.id WHERE m.deleted=b'0'
  ) SELECT DISTINCT id FROM descendants;
  -- Only holders of existing pages/buttons receive the necessary navigation parent.
  INSERT INTO system_role_menu (role_id,menu_id,creator,create_time,updater,update_time,deleted,tenant_id)
  SELECT DISTINCT s.role_id,991511,'edhr-sync-group',NOW(),'edhr-sync-group',NOW(),b'0',s.tenant_id
  FROM system_role_menu s JOIN tmp_edhr_sync_descendants d ON d.id=s.menu_id
  WHERE s.deleted=b'0' AND NOT EXISTS (SELECT 1 FROM system_role_menu t
    WHERE t.deleted=b'0' AND t.role_id=s.role_id AND t.tenant_id=s.tenant_id AND t.menu_id=991511);
  UPDATE system_tenant_package p
  SET menu_ids=JSON_ARRAY_APPEND(CAST(p.menu_ids AS JSON),'$',991511),updater='edhr-sync-group',update_time=NOW()
  WHERE p.deleted=b'0' AND NOT JSON_CONTAINS(CAST(p.menu_ids AS JSON),CAST('991511' AS JSON),'$')
    AND EXISTS (SELECT 1 FROM tmp_edhr_sync_descendants d
      WHERE JSON_CONTAINS(CAST(p.menu_ids AS JSON),CAST(d.id AS JSON),'$'));
  IF (SELECT COUNT(*) FROM system_menu m JOIN tmp_edhr_sync_pages p ON m.id=p.id
      WHERE m.parent_id=991511 AND m.sort=p.sort)<>10 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Data sync grouping postcondition failed';
  END IF;
  COMMIT;
  DROP TEMPORARY TABLE tmp_edhr_sync_descendants;
  DROP TEMPORARY TABLE tmp_edhr_sync_pages;
END$$
DELIMITER ;
CALL migrate_edhr_data_sync_directory();
DROP PROCEDURE migrate_edhr_data_sync_directory;
