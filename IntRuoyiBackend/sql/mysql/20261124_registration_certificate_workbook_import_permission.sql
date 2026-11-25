-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260829_registration_certificate_management_menu_hierarchy; type=menu; riskLevel=medium
-- Restore the permission as a button under the current certificate page. Never revive retired page 605071321.
-- No package is opted in automatically: select this button AND upload approval in the dedicated package UI.
-- Only administrators of explicitly opted-in packages are synchronized on replay; unrelated tenants stay unchanged.
-- Apply through the menu UI on a running local instance, or before application startup for release cache consistency.
-- Rollback: remove this button from the dedicated package/roles, then delete menu 2026112401 through menu management.
SET NAMES utf8mb4;
DROP PROCEDURE IF EXISTS ensure_regcert_workbook_permission_20261124;
DELIMITER $$
CREATE PROCEDURE ensure_regcert_workbook_permission_20261124()
BEGIN
  DECLARE page_id BIGINT;
  DECLARE approval_id BIGINT;
  DECLARE EXIT HANDLER FOR SQLEXCEPTION BEGIN ROLLBACK; RESIGNAL; END;
  START TRANSACTION;
  IF (SELECT COUNT(*) FROM system_menu WHERE deleted=0 AND status=0 AND type=2
      AND path='/mdm/registration-certificate' AND component='dcc/registration-certificate/index/index'
      AND permission='dcc:registration-certificate:query-current') <> 1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Expected one enabled registration certificate page';
  END IF;
  SELECT id INTO page_id FROM system_menu WHERE deleted=0 AND status=0 AND type=2
    AND path='/mdm/registration-certificate' AND component='dcc/registration-certificate/index/index'
    AND permission='dcc:registration-certificate:query-current';
  IF (SELECT COUNT(*) FROM system_menu WHERE deleted=0 AND status=0 AND type=3 AND parent_id=page_id
      AND permission='dcc:registration-certificate:upload:approve') <> 1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Expected one enabled registration upload approval permission';
  END IF;
  SELECT id INTO approval_id FROM system_menu WHERE deleted=0 AND status=0 AND type=3 AND parent_id=page_id
    AND permission='dcc:registration-certificate:upload:approve';
  IF EXISTS (SELECT 1 FROM system_menu WHERE id=2026112401 AND
      (deleted<>0 OR status<>0 OR type<>3 OR parent_id<>page_id
       OR permission<>'dcc:registration-certificate:historical-import'))
    OR EXISTS (SELECT 1 FROM system_menu WHERE deleted=0 AND id<>2026112401
      AND permission='dcc:registration-certificate:historical-import') THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Registration workbook import menu identity conflict';
  END IF;
  IF EXISTS (SELECT 1 FROM system_tenant_package WHERE deleted=0 AND
      (menu_ids IS NULL OR NOT JSON_VALID(menu_ids))) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Invalid tenant package menu JSON';
  END IF;
  IF EXISTS (SELECT 1 FROM system_tenant_package WHERE deleted=0
      AND JSON_CONTAINS(CAST(menu_ids AS JSON),'2026112401','$')
      AND NOT JSON_CONTAINS(CAST(menu_ids AS JSON),CAST(approval_id AS JSON),'$')) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Workbook import package must explicitly include upload approval';
  END IF;
  INSERT INTO system_menu
    (id,name,permission,type,sort,parent_id,path,icon,component,component_name,status,
     visible,keep_alive,always_show,creator,create_time,updater,update_time,deleted)
    SELECT 2026112401,'注册证历史导入','dcc:registration-certificate:historical-import',3,150,page_id,'','','','',0,
      1,1,1,'registration-workbook',NOW(),'registration-workbook',NOW(),0
    WHERE NOT EXISTS (SELECT 1 FROM system_menu WHERE id=2026112401);
  -- Add only the owning page to packages that already explicitly selected the import permission.
  UPDATE system_tenant_package SET menu_ids=JSON_ARRAY_APPEND(CAST(menu_ids AS JSON),'$',page_id),
    updater='registration-workbook',update_time=NOW()
    WHERE deleted=0 AND JSON_CONTAINS(CAST(menu_ids AS JSON),'2026112401','$')
      AND NOT JSON_CONTAINS(CAST(menu_ids AS JSON),CAST(page_id AS JSON),'$');
  UPDATE system_role_menu rm
    JOIN system_role r ON r.id=rm.role_id AND r.tenant_id=rm.tenant_id AND r.deleted=0 AND r.status=0
    JOIN system_tenant t ON t.id=r.tenant_id AND t.deleted=0 AND t.status=0
    JOIN system_tenant_package p ON p.id=t.package_id AND p.deleted=0 AND p.status=0
    SET rm.deleted=0,rm.updater='registration-workbook',rm.update_time=NOW()
    WHERE rm.menu_id=2026112401 AND rm.deleted<>0 AND r.code='tenant_admin'
      AND JSON_CONTAINS(CAST(p.menu_ids AS JSON),'2026112401','$');
  INSERT INTO system_role_menu (role_id,menu_id,creator,create_time,updater,update_time,deleted,tenant_id)
    SELECT r.id,2026112401,'registration-workbook',NOW(),'registration-workbook',NOW(),0,r.tenant_id
    FROM system_role r
    JOIN system_tenant t ON t.id=r.tenant_id AND t.deleted=0 AND t.status=0
    JOIN system_tenant_package p ON p.id=t.package_id AND p.deleted=0 AND p.status=0
    WHERE r.deleted=0 AND r.status=0 AND r.code='tenant_admin'
      AND JSON_CONTAINS(CAST(p.menu_ids AS JSON),'2026112401','$')
      AND NOT EXISTS (SELECT 1 FROM system_role_menu rm WHERE rm.role_id=r.id AND rm.tenant_id=r.tenant_id
        AND rm.menu_id=2026112401 AND rm.deleted=0);
  COMMIT;
END$$
DELIMITER ;
CALL ensure_regcert_workbook_permission_20261124();
DROP PROCEDURE ensure_regcert_workbook_permission_20261124;
