-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260804_mes_edhr_qa_menu,20260707_system_role_category_management; type=menu; riskLevel=medium
-- Packaging leaders reuse the production workbench. Only tenants/packages already using it are eligible.
-- No user-role binding is created; existing orders and employee ownership are unchanged.
SET NAMES utf8mb4;
DROP PROCEDURE IF EXISTS seed_mes_packaging_leader;
DELIMITER $$
CREATE PROCEDURE seed_mes_packaging_leader()
BEGIN
  DECLARE root_menu BIGINT;
  DECLARE added INT DEFAULT 1;
  DECLARE EXIT HANDLER FOR SQLEXCEPTION BEGIN ROLLBACK; RESIGNAL; END;
  IF (SELECT COUNT(*) FROM system_menu WHERE deleted=b'0' AND status=0
      AND component='mes/pro/processpool/ProductionLeaderWorkbenchPage') <> 1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Missing or duplicate production leader workbench';
  END IF;
  SELECT id INTO root_menu FROM system_menu WHERE deleted=b'0' AND status=0
    AND component='mes/pro/processpool/ProductionLeaderWorkbenchPage';
  DROP TEMPORARY TABLE IF EXISTS tmp_packaging_menu;
  CREATE TEMPORARY TABLE tmp_packaging_menu (id BIGINT PRIMARY KEY);
  INSERT INTO tmp_packaging_menu SELECT id FROM system_menu WHERE deleted=b'0' AND status=0
    AND (id=root_menu OR permission LIKE 'mes:pro-process-pool-team-leader:%'
      OR permission IN ('mes:pro-edhr-work-task:query','mes:pro-feedback:query','mes:pro-feedback:create'));
  DROP TEMPORARY TABLE IF EXISTS tmp_packaging_parent;
  CREATE TEMPORARY TABLE tmp_packaging_parent (id BIGINT PRIMARY KEY);
  WHILE added > 0 DO
    TRUNCATE tmp_packaging_parent;
    INSERT INTO tmp_packaging_parent SELECT DISTINCT parent.id FROM tmp_packaging_menu chosen
      JOIN system_menu child ON child.id=chosen.id
      JOIN system_menu parent ON parent.id=child.parent_id AND parent.deleted=b'0' AND parent.status=0;
    INSERT IGNORE INTO tmp_packaging_menu SELECT id FROM tmp_packaging_parent;
    SET added=ROW_COUNT();
  END WHILE;
  DROP TEMPORARY TABLE IF EXISTS tmp_packaging_package;
  CREATE TEMPORARY TABLE tmp_packaging_package (id BIGINT PRIMARY KEY);
  INSERT INTO tmp_packaging_package SELECT id FROM system_tenant_package
    WHERE deleted=b'0' AND status=0 AND JSON_VALID(menu_ids)
      AND JSON_CONTAINS(menu_ids,CAST(root_menu AS JSON),'$');
  DROP TEMPORARY TABLE IF EXISTS tmp_packaging_tenant;
  CREATE TEMPORARY TABLE tmp_packaging_tenant (id BIGINT PRIMARY KEY);
  INSERT INTO tmp_packaging_tenant SELECT t.id FROM system_tenant t
    WHERE t.deleted=b'0' AND t.status=0 AND (t.package_id=-1
      OR t.package_id IN (SELECT id FROM tmp_packaging_package));
  IF EXISTS (SELECT 1 FROM tmp_packaging_tenant t WHERE
      (SELECT COUNT(*) FROM system_role_category c WHERE c.tenant_id=t.id
        AND c.code='menu' AND c.deleted=b'0' AND c.status=0) <> 1) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Eligible tenant missing unique enabled menu role category';
  END IF;
  IF EXISTS (SELECT 1 FROM system_role r JOIN tmp_packaging_tenant t ON t.id=r.tenant_id
      WHERE r.code='packaging_leader_permission' GROUP BY r.tenant_id HAVING COUNT(*)>1)
    OR EXISTS (SELECT 1 FROM system_role r JOIN tmp_packaging_tenant t ON t.id=r.tenant_id
      WHERE r.code='packaging_leader_permission' AND (r.deleted<>b'0' OR r.status<>0)) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Existing packaging role is duplicate, deleted or disabled';
  END IF;
  START TRANSACTION;
  INSERT INTO system_role (name,code,sort,category_id,data_scope,data_scope_dept_ids,status,type,remark,
      creator,create_time,updater,update_time,deleted,tenant_id)
  SELECT '包装组长','packaging_leader_permission',900438,c.id,1,'',0,2,
      '本租户全部活跃工单；仅审核所属员工；允许整单完工','packaging-leader',NOW(),'packaging-leader',NOW(),b'0',t.id
    FROM tmp_packaging_tenant t JOIN system_role_category c ON c.tenant_id=t.id
      AND c.code='menu' AND c.deleted=b'0' AND c.status=0
    WHERE NOT EXISTS (SELECT 1 FROM system_role r WHERE r.tenant_id=t.id AND r.code='packaging_leader_permission');
  DROP TEMPORARY TABLE IF EXISTS tmp_packaging_package_menu;
  CREATE TEMPORARY TABLE tmp_packaging_package_menu (package_id BIGINT,menu_id BIGINT,PRIMARY KEY(package_id,menu_id));
  INSERT INTO tmp_packaging_package_menu SELECT p.id,j.id FROM system_tenant_package p
    JOIN tmp_packaging_package target ON target.id=p.id
    JOIN JSON_TABLE(p.menu_ids,'$[*]' COLUMNS(id BIGINT PATH '$')) j;
  INSERT IGNORE INTO tmp_packaging_package_menu SELECT p.id,m.id FROM tmp_packaging_package p CROSS JOIN tmp_packaging_menu m;
  UPDATE system_tenant_package p JOIN (SELECT package_id,JSON_ARRAYAGG(menu_id) ids
    FROM tmp_packaging_package_menu GROUP BY package_id) merged ON merged.package_id=p.id
    SET p.menu_ids=merged.ids,p.updater='packaging-leader',p.update_time=NOW();
  INSERT INTO system_role_menu (role_id,menu_id,creator,create_time,updater,update_time,deleted,tenant_id)
    SELECT r.id,m.id,'packaging-leader',NOW(),'packaging-leader',NOW(),b'0',r.tenant_id
    FROM system_role r JOIN tmp_packaging_tenant t ON t.id=r.tenant_id CROSS JOIN tmp_packaging_menu m
    WHERE r.code='packaging_leader_permission' AND r.deleted=b'0' AND r.status=0
      AND NOT EXISTS (SELECT 1 FROM system_role_menu rm WHERE rm.role_id=r.id AND rm.menu_id=m.id
        AND rm.tenant_id=r.tenant_id AND rm.deleted=b'0');
  COMMIT;
  DROP TEMPORARY TABLE tmp_packaging_package_menu;
  DROP TEMPORARY TABLE tmp_packaging_tenant;
  DROP TEMPORARY TABLE tmp_packaging_package;
  DROP TEMPORARY TABLE tmp_packaging_parent;
  DROP TEMPORARY TABLE tmp_packaging_menu;
END$$
DELIMITER ;
CALL seed_mes_packaging_leader();
DROP PROCEDURE seed_mes_packaging_leader;
