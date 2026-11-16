-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20261026_edhr_erp_config_menu_order,20260707_system_role_category_management; type=permission; riskLevel=medium
-- Explicit ERP data roles. No user-role assignments are made by this migration.
SET NAMES utf8mb4;
DROP PROCEDURE IF EXISTS migrate_erp_data_roles_20261116;
DELIMITER $$
CREATE PROCEDURE migrate_erp_data_roles_20261116()
BEGIN
 DECLARE EXIT HANDLER FOR SQLEXCEPTION BEGIN ROLLBACK; RESIGNAL; END;
 DROP TEMPORARY TABLE IF EXISTS tmp_erp_data_roles;
 CREATE TEMPORARY TABLE tmp_erp_data_roles(page_id BIGINT PRIMARY KEY,permission_id BIGINT UNIQUE,name VARCHAR(50),code VARCHAR(100),permission VARCHAR(100),sort INT) DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
 INSERT INTO tmp_erp_data_roles VALUES
 (2565,605073600,'物料列表','erp_data_product_operator','erp:data-product:operate',1),
 (2590,605073601,'即时库存汇总','erp_data_stock_operator','erp:data-stock:operate',2),
 (6023,605073602,'物料清单','erp_data_bom_operator','erp:data-bom:operate',3),
 (6021,605073603,'生产用料清单','erp_data_material_operator','erp:data-material:operate',4),
 (6032,605073604,'生产领料单','erp_data_pick_operator','erp:data-pick:operate',5),
 (605071322,605073605,'生产补料单','erp_data_replenishment_operator','erp:data-replenishment:operate',6),
 (6025,605073606,'即时库存','erp_data_inventory_operator','erp:data-inventory:operate',7),
 (5530,605073607,'生产订单','erp_data_order_operator','erp:data-order:operate',8),
 (6013,605073608,'ERP配置','erp_data_config_operator','erp:data-config:operate',9);
 IF NOT EXISTS(SELECT 1 FROM system_menu WHERE id=991511 AND path='/mes/edhr-data-sync' AND deleted=0 AND status=0)
 OR EXISTS(SELECT 1 FROM tmp_erp_data_roles n LEFT JOIN system_menu m ON m.id=n.page_id
           WHERE m.id IS NULL OR m.deleted<>0 OR m.status<>0 OR m.parent_id<>991511 OR m.type<>2) THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='ERP data page prerequisite missing or changed';
 END IF;
 IF EXISTS(SELECT 1 FROM system_menu m JOIN tmp_erp_data_roles n ON m.id=n.permission_id OR m.permission=n.permission
           WHERE m.id<>n.permission_id OR m.permission<>n.permission OR m.parent_id<>n.page_id OR m.type<>3 OR m.deleted<>0 OR m.status<>0) THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='ERP data permission identity conflict';
 END IF;
 IF EXISTS(SELECT 1 FROM system_tenant_package WHERE deleted=0 AND (NOT JSON_VALID(menu_ids) OR JSON_TYPE(CAST(menu_ids AS JSON))<>'ARRAY')) THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Invalid tenant package menu_ids';
 END IF;
 DROP TEMPORARY TABLE IF EXISTS tmp_erp_data_nodes;
 CREATE TEMPORARY TABLE tmp_erp_data_nodes(page_id BIGINT,id BIGINT,PRIMARY KEY(page_id,id));
 INSERT INTO tmp_erp_data_nodes
 WITH RECURSIVE nodes AS (
  SELECT page_id,page_id id FROM tmp_erp_data_roles UNION ALL
  SELECT n.page_id,m.id FROM system_menu m JOIN nodes n ON m.parent_id=n.id WHERE m.deleted=0
 ) SELECT DISTINCT page_id,id FROM nodes;
 -- Hidden legacy ERP configuration is part of the same page, without batch-record golden-finger grants.
 IF NOT EXISTS(SELECT 1 FROM system_menu WHERE id=991502 AND parent_id=991511 AND component='erp/sync/config/index' AND deleted=0) THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Legacy ERP config prerequisite missing';
 END IF;
 INSERT INTO tmp_erp_data_nodes VALUES(6013,991502);
 DROP TEMPORARY TABLE IF EXISTS tmp_erp_data_tenants;
 CREATE TEMPORARY TABLE tmp_erp_data_tenants(tenant_id BIGINT PRIMARY KEY);
 INSERT INTO tmp_erp_data_tenants
 SELECT DISTINCT r.tenant_id FROM system_role r JOIN system_tenant t ON t.id=r.tenant_id AND t.deleted=0 WHERE r.deleted=0
 AND r.code IN('tenant_admin','super_admin','mes_workshop_director','mes_team_leader','mes_scheduler');
 IF NOT EXISTS(SELECT 1 FROM tmp_erp_data_tenants) THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='No ERP data target tenants';
 END IF;
 IF EXISTS(SELECT 1 FROM system_role_category c JOIN tmp_erp_data_tenants t ON t.tenant_id=c.tenant_id
           WHERE c.code='erp_data' AND (c.deleted<>0 OR c.status<>0 OR c.name<>'ERP数据'))
 THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='ERP data category identity conflict';
 END IF;
 IF EXISTS(SELECT 1 FROM system_role r JOIN tmp_erp_data_roles n ON r.code=n.code JOIN tmp_erp_data_tenants t ON t.tenant_id=r.tenant_id
           WHERE r.deleted<>0 OR r.status<>0 OR COALESCE(r.remark,'')<>'ERP data operation roles 20261116') THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='ERP data role/category identity conflict';
 END IF;
 START TRANSACTION;
 INSERT INTO system_menu(id,name,permission,type,sort,parent_id,path,icon,component,component_name,status,visible,keep_alive,always_show,creator,updater)
 SELECT n.permission_id,CONCAT(n.name,'操作'),n.permission,3,90,n.page_id,'','','','',0,1,1,1,'erp-data-roles','erp-data-roles'
 FROM tmp_erp_data_roles n WHERE NOT EXISTS(SELECT 1 FROM system_menu m WHERE m.id=n.permission_id);
 -- This retired navigation entry must not grant unrelated batch-record write bypass.
 UPDATE system_menu SET permission='',updater='erp-data-roles',update_time=NOW() WHERE id=991502;
 INSERT INTO system_role_category(name,code,sort,status,remark,tenant_id,creator,updater)
 SELECT 'ERP数据','erp_data',56,0,'ERP数据独立页签操作角色',t.tenant_id,'erp-data-roles','erp-data-roles'
 FROM tmp_erp_data_tenants t WHERE NOT EXISTS(SELECT 1 FROM system_role_category c WHERE c.tenant_id=t.tenant_id AND c.code='erp_data' AND c.deleted=0);
 INSERT INTO system_role(name,code,sort,category_id,data_scope,status,type,remark,tenant_id,creator,updater)
 SELECT CONCAT(n.name,'操作'),n.code,n.sort,c.id,1,0,2,'ERP data operation roles 20261116',t.tenant_id,'erp-data-roles','erp-data-roles'
 FROM tmp_erp_data_tenants t CROSS JOIN tmp_erp_data_roles n
 JOIN system_role_category c ON c.tenant_id=t.tenant_id AND c.code='erp_data' AND c.deleted=0
 WHERE NOT EXISTS(SELECT 1 FROM system_role r WHERE r.tenant_id=t.tenant_id AND r.code=n.code AND r.deleted=0);
 -- Revoke only this directory/subtree from the five original roles.
 DELETE rm FROM system_role_menu rm JOIN system_role r ON r.id=rm.role_id AND r.tenant_id=rm.tenant_id
 WHERE r.deleted=0 AND r.code IN('tenant_admin','super_admin','mes_workshop_director','mes_team_leader','mes_scheduler')
 AND (rm.menu_id=991511 OR rm.menu_id IN(SELECT id FROM tmp_erp_data_nodes) OR rm.menu_id IN(SELECT permission_id FROM tmp_erp_data_roles));
 DROP TEMPORARY TABLE IF EXISTS tmp_erp_data_grants;
 CREATE TEMPORARY TABLE tmp_erp_data_grants(code VARCHAR(100),menu_id BIGINT,PRIMARY KEY(code,menu_id));
 INSERT IGNORE INTO tmp_erp_data_grants SELECT n.code,d.id FROM tmp_erp_data_roles n JOIN tmp_erp_data_nodes d ON d.page_id=n.page_id;
 INSERT IGNORE INTO tmp_erp_data_grants SELECT code,permission_id FROM tmp_erp_data_roles;
 INSERT IGNORE INTO tmp_erp_data_grants SELECT code,991511 FROM tmp_erp_data_roles;
 -- Existing lookup dependencies do not grant other ERP data pages.
 INSERT IGNORE INTO tmp_erp_data_grants
 SELECT 'erp_data_product_operator',id FROM system_menu WHERE deleted=0 AND permission IN('erp:product-category:query','erp:product-unit:query');
 INSERT IGNORE INTO tmp_erp_data_grants
 SELECT 'erp_data_stock_operator',id FROM system_menu WHERE deleted=0 AND permission='erp:warehouse:query';
 INSERT IGNORE INTO tmp_erp_data_grants
 SELECT 'erp_data_order_operator',id FROM system_menu WHERE deleted=0 AND permission='mes:md-item:query';
 INSERT IGNORE INTO tmp_erp_data_grants
 SELECT 'erp_data_config_operator',id FROM system_menu WHERE deleted=0 AND permission IN('erp:kingdee-config:query','erp:kingdee-config:save');
 INSERT INTO system_role_menu(role_id,menu_id,tenant_id,creator,updater)
 SELECT r.id,g.menu_id,r.tenant_id,'erp-data-roles','erp-data-roles' FROM system_role r
 JOIN tmp_erp_data_tenants t ON t.tenant_id=r.tenant_id JOIN tmp_erp_data_grants g ON g.code=r.code
 WHERE r.deleted=0 AND NOT EXISTS(SELECT 1 FROM system_role_menu rm WHERE rm.role_id=r.id AND rm.menu_id=g.menu_id AND rm.tenant_id=r.tenant_id AND rm.deleted=0);
 -- Only packages already containing a given page receive its new operation marker.
 UPDATE system_tenant_package SET menu_ids=JSON_ARRAY_APPEND(menu_ids,'$',605073600),updater='erp-data-roles',update_time=NOW()
 WHERE deleted=0 AND JSON_CONTAINS(menu_ids,'2565','$') AND NOT JSON_CONTAINS(menu_ids,'605073600','$');
 UPDATE system_tenant_package SET menu_ids=JSON_ARRAY_APPEND(menu_ids,'$',605073601),updater='erp-data-roles',update_time=NOW()
 WHERE deleted=0 AND JSON_CONTAINS(menu_ids,'2590','$') AND NOT JSON_CONTAINS(menu_ids,'605073601','$');
 UPDATE system_tenant_package SET menu_ids=JSON_ARRAY_APPEND(menu_ids,'$',605073602),updater='erp-data-roles',update_time=NOW()
 WHERE deleted=0 AND JSON_CONTAINS(menu_ids,'6023','$') AND NOT JSON_CONTAINS(menu_ids,'605073602','$');
 UPDATE system_tenant_package SET menu_ids=JSON_ARRAY_APPEND(menu_ids,'$',605073603),updater='erp-data-roles',update_time=NOW()
 WHERE deleted=0 AND JSON_CONTAINS(menu_ids,'6021','$') AND NOT JSON_CONTAINS(menu_ids,'605073603','$');
 UPDATE system_tenant_package SET menu_ids=JSON_ARRAY_APPEND(menu_ids,'$',605073604),updater='erp-data-roles',update_time=NOW()
 WHERE deleted=0 AND JSON_CONTAINS(menu_ids,'6032','$') AND NOT JSON_CONTAINS(menu_ids,'605073604','$');
 UPDATE system_tenant_package SET menu_ids=JSON_ARRAY_APPEND(menu_ids,'$',605073605),updater='erp-data-roles',update_time=NOW()
 WHERE deleted=0 AND JSON_CONTAINS(menu_ids,'605071322','$') AND NOT JSON_CONTAINS(menu_ids,'605073605','$');
 UPDATE system_tenant_package SET menu_ids=JSON_ARRAY_APPEND(menu_ids,'$',605073606),updater='erp-data-roles',update_time=NOW()
 WHERE deleted=0 AND JSON_CONTAINS(menu_ids,'6025','$') AND NOT JSON_CONTAINS(menu_ids,'605073606','$');
 UPDATE system_tenant_package SET menu_ids=JSON_ARRAY_APPEND(menu_ids,'$',605073607),updater='erp-data-roles',update_time=NOW()
 WHERE deleted=0 AND JSON_CONTAINS(menu_ids,'5530','$') AND NOT JSON_CONTAINS(menu_ids,'605073607','$');
 UPDATE system_tenant_package SET menu_ids=JSON_ARRAY_APPEND(menu_ids,'$',605073608),updater='erp-data-roles',update_time=NOW()
 WHERE deleted=0 AND JSON_CONTAINS(menu_ids,'6013','$') AND NOT JSON_CONTAINS(menu_ids,'605073608','$');
 UPDATE system_approval_authority_revision a JOIN tmp_erp_data_tenants t ON t.tenant_id=a.tenant_id
 SET a.revision=a.revision+1,a.updated_at=NOW();
 IF EXISTS(SELECT 1 FROM system_role_menu rm JOIN system_role r ON r.id=rm.role_id AND r.tenant_id=rm.tenant_id
           WHERE r.deleted=0 AND rm.deleted=0 AND r.code IN('tenant_admin','super_admin','mes_workshop_director','mes_team_leader','mes_scheduler')
           AND (rm.menu_id=991511 OR rm.menu_id IN(SELECT id FROM tmp_erp_data_nodes))) THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='ERP data revocation postcondition failed';
 END IF;
 COMMIT;
 DROP TEMPORARY TABLE tmp_erp_data_grants;
 DROP TEMPORARY TABLE tmp_erp_data_tenants;
 DROP TEMPORARY TABLE tmp_erp_data_nodes;
 DROP TEMPORARY TABLE tmp_erp_data_roles;
END$$
DELIMITER ;
CALL migrate_erp_data_roles_20261116();
DROP PROCEDURE migrate_erp_data_roles_20261116;
