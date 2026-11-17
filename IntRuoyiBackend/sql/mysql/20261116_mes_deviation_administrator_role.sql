-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260707_system_role_category_management,20261026_deviation_under_production_execution,20260924_mes_edhr_deviation_ncr,20261026_merge_production_tasks_into_workbench; type=menu; riskLevel=medium
-- Dedicated business role for every currently enabled, unexpired tenant. No user is assigned automatically.
-- Deviation node signatures still require the explicitly assigned person; NCR disposition is not granted.
-- UserSelectV2 selection-page/list and profile workbench are authenticated read endpoints with no extra menu permission.
-- Keep existing roles and user assignments intact. A conflicting existing role fails instead of being repurposed.
SET NAMES utf8mb4;
DROP PROCEDURE IF EXISTS ensure_deviation_administrator_role;
DELIMITER $$
CREATE PROCEDURE ensure_deviation_administrator_role()
BEGIN
  DECLARE EXIT HANDLER FOR SQLEXCEPTION BEGIN ROLLBACK; RESIGNAL; END;
  DROP TEMPORARY TABLE IF EXISTS tmp_deviation_admin_permissions;
  CREATE TEMPORARY TABLE tmp_deviation_admin_permissions(
    permission VARCHAR(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci PRIMARY KEY);
  INSERT INTO tmp_deviation_admin_permissions VALUES
    ('mes:pro-edhr-deviation:query'),
    ('mes:pro-edhr-deviation:create'),
    ('mes:pro-edhr-deviation:handle'),
    ('mes:pro-edhr-deviation:verify'),
    ('mes:pro-edhr-deviation:department-confirm'),
    ('mes:pro-edhr-deviation:qa-close'),
    ('mes:pro-edhr-deviation:quality-approve'),
    ('mes:pro-edhr-deviation:critical-management-approve'),
    ('mes:pro-edhr-deviation:ncr-create'),
    ('mes:pro-edhr-nonconformance-review:query'),
    ('mes:pro-edhr-nonconformance-review:deviation-create'),
    ('mes:pro-edhr-work-task:query');
  DROP TEMPORARY TABLE IF EXISTS tmp_deviation_admin_menus;
  CREATE TEMPORARY TABLE tmp_deviation_admin_menus(id BIGINT PRIMARY KEY);
  INSERT INTO tmp_deviation_admin_menus SELECT DISTINCT m.id FROM system_menu m
    JOIN tmp_deviation_admin_permissions p ON p.permission=m.permission WHERE m.deleted=0 AND m.status=0;
  IF EXISTS (SELECT 1 FROM tmp_deviation_admin_permissions p WHERE NOT EXISTS
      (SELECT 1 FROM system_menu m WHERE m.permission=p.permission AND m.deleted=0 AND m.status=0)) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Missing deviation administrator permission menu';
  END IF;
  IF NOT EXISTS (SELECT 1 FROM system_menu WHERE id=991530 AND parent_id=991516
      AND component='mes/pro/edhr-deviation/index' AND deleted=0 AND status=0)
    OR NOT EXISTS (SELECT 1 FROM system_menu WHERE id=9008300 AND parent_id=991516 AND component='mes/pro/edhr-nonconformance/NonconformanceReviewPage' AND deleted=0 AND status=0)
    OR NOT EXISTS (SELECT 1 FROM system_menu WHERE id=900220 AND parent_id=0 AND type=1
      AND path='/mes/edhr-batch-processing' AND permission='mes:pro-edhr-batch-processing:query'
      AND deleted=0 AND status=0)
    OR NOT EXISTS (SELECT 1 FROM system_menu WHERE id=991516 AND parent_id=900220 AND type=1
      AND path='/mes/edhr-production-execution' AND permission='' AND deleted=0 AND status=0) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Deviation administrator navigation identity mismatch';
  END IF;
  -- Navigation only: do not grant the old 5700 parent with unrelated production functions.
  INSERT IGNORE INTO tmp_deviation_admin_menus VALUES (991530),(900220),(991516);
  DROP TEMPORARY TABLE IF EXISTS tmp_deviation_admin_tenants;
  CREATE TEMPORARY TABLE tmp_deviation_admin_tenants AS
    SELECT id,package_id FROM system_tenant WHERE deleted=0 AND status=0 AND expire_time>CURRENT_TIMESTAMP;
  IF EXISTS (SELECT 1 FROM tmp_deviation_admin_tenants t LEFT JOIN system_tenant_package p ON p.id=t.package_id
      WHERE t.package_id<>0 AND (p.id IS NULL OR p.deleted<>0 OR p.status<>0
        OR NOT JSON_VALID(p.menu_ids) OR JSON_TYPE(CAST(p.menu_ids AS JSON))<>'ARRAY')) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Enabled tenant has missing or invalid package';
  END IF;
  IF EXISTS (SELECT t.id FROM tmp_deviation_admin_tenants t LEFT JOIN system_role_category c
      ON c.tenant_id=t.id AND c.code='menu' AND c.deleted=0 AND c.status=0
      GROUP BY t.id HAVING COUNT(c.id)<>1) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Missing or duplicate enabled menu role category';
  END IF;
  IF EXISTS (SELECT 1 FROM system_role r JOIN tmp_deviation_admin_tenants t ON t.id=r.tenant_id
      WHERE r.code='deviation_admin_permission' AND (r.deleted<>0 OR r.status<>0 OR r.name<>'偏差管理员' OR r.type<>2
        OR r.category_id<>(SELECT c.id FROM system_role_category c WHERE c.tenant_id=t.id AND c.code='menu' AND c.deleted=0 AND c.status=0)
        OR r.category_id IS NULL)) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Conflicting deviation administrator role';
  END IF;
  IF EXISTS (SELECT r.tenant_id FROM system_role r JOIN tmp_deviation_admin_tenants t ON t.id=r.tenant_id
      WHERE r.code='deviation_admin_permission' GROUP BY r.tenant_id HAVING COUNT(*)>1) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Conflicting deviation administrator role';
  END IF;
  START TRANSACTION;
  INSERT INTO system_role (name,code,sort,category_id,data_scope,data_scope_dept_ids,status,type,remark,
      creator,create_time,updater,update_time,deleted,tenant_id)
    SELECT '偏差管理员','deviation_admin_permission',85,c.id,1,'',0,2,
      '偏差接收调查专岗；仅偏差业务、相关NCR查询转审及个人任务；不自动绑定用户，不授NCR处置。',
      'deviation-admin-role',NOW(),'deviation-admin-role',NOW(),0,t.id
    FROM tmp_deviation_admin_tenants t JOIN system_role_category c ON c.tenant_id=t.id AND c.code='menu' AND c.deleted=0 AND c.status=0
    WHERE NOT EXISTS (SELECT 1 FROM system_role r
      WHERE r.tenant_id=t.id AND r.code='deviation_admin_permission');
  INSERT INTO system_role_menu (role_id,menu_id,creator,create_time,updater,update_time,deleted,tenant_id)
    SELECT r.id,m.id,'deviation-admin-role',NOW(),'deviation-admin-role',NOW(),0,r.tenant_id
    FROM system_role r JOIN tmp_deviation_admin_tenants t ON t.id=r.tenant_id
    CROSS JOIN tmp_deviation_admin_menus m
    WHERE r.code='deviation_admin_permission' AND r.deleted=0 AND NOT EXISTS
      (SELECT 1 FROM system_role_menu rm WHERE rm.role_id=r.id AND rm.tenant_id=r.tenant_id
       AND rm.menu_id=m.id AND rm.deleted=0);
  -- Expand only packages used by eligible tenants; deduplicate each explicit reviewed menu ID.
  BEGIN
    DECLARE finished INT DEFAULT 0;
    DECLARE required_menu BIGINT;
    DECLARE menus CURSOR FOR SELECT id FROM tmp_deviation_admin_menus ORDER BY id;
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET finished=1;
    OPEN menus;
    menu_loop: LOOP
      FETCH menus INTO required_menu;
      IF finished=1 THEN LEAVE menu_loop; END IF;
      UPDATE system_tenant_package p JOIN (SELECT DISTINCT package_id FROM tmp_deviation_admin_tenants WHERE package_id<>0) t
        ON t.package_id=p.id
        SET p.menu_ids=JSON_ARRAY_APPEND(CAST(p.menu_ids AS JSON),'$',required_menu),
            p.updater='deviation-admin-role',p.update_time=NOW()
        WHERE NOT JSON_CONTAINS(CAST(p.menu_ids AS JSON),CAST(required_menu AS JSON),'$');
    END LOOP;
    CLOSE menus;
  END;
  COMMIT;
  DROP TEMPORARY TABLE tmp_deviation_admin_permissions;
  DROP TEMPORARY TABLE tmp_deviation_admin_menus;
  DROP TEMPORARY TABLE tmp_deviation_admin_tenants;
END$$
DELIMITER ;
CALL ensure_deviation_administrator_role();
DROP PROCEDURE ensure_deviation_administrator_role;
