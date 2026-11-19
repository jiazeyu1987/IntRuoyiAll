-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260829_registration_certificate_management_menu_hierarchy; type=menu; riskLevel=medium
-- Grant deletion only to certificate managers and tenant/system administrators with the certificate page.
SET NAMES utf8mb4;
DROP PROCEDURE IF EXISTS ensure_registration_certificate_delete_menu;
DELIMITER $$
CREATE PROCEDURE ensure_registration_certificate_delete_menu()
BEGIN
  DECLARE certificate_page_id BIGINT;
  DECLARE EXIT HANDLER FOR SQLEXCEPTION BEGIN ROLLBACK; RESIGNAL; END;
  START TRANSACTION;
  IF (SELECT COUNT(*) FROM system_menu WHERE deleted=0 AND status=0 AND type=2 AND path='/mdm/registration-certificate' AND component='dcc/registration-certificate/index/index' AND permission='dcc:registration-certificate:query-current')<>1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Missing registration certificate page';
  END IF;
  SELECT id INTO certificate_page_id FROM system_menu WHERE deleted=0 AND status=0 AND type=2 AND path='/mdm/registration-certificate' AND component='dcc/registration-certificate/index/index' AND permission='dcc:registration-certificate:query-current';
  IF EXISTS (SELECT 1 FROM system_menu WHERE id=2026111801 AND
      (deleted<>0 OR permission<>'dcc:registration-certificate:delete' OR parent_id<>certificate_page_id OR type<>3))
    OR EXISTS (SELECT 1 FROM system_menu WHERE permission='dcc:registration-certificate:delete'
      AND id<>2026111801 AND deleted=0) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Registration certificate delete permission conflict';
  END IF;
  IF EXISTS (SELECT 1 FROM system_tenant_package WHERE deleted=0 AND NOT JSON_VALID(menu_ids)) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Invalid tenant package menu JSON';
  END IF;
  INSERT INTO system_menu
    (id,name,permission,type,sort,parent_id,path,icon,component,component_name,status,
     visible,keep_alive,always_show,creator,create_time,updater,update_time,deleted)
    SELECT 2026111801,'删除注册证','dcc:registration-certificate:delete',3,140,certificate_page_id,'','','','',0,
      1,1,1,'registration-delete',NOW(),'registration-delete',NOW(),0
    WHERE NOT EXISTS (SELECT 1 FROM system_menu WHERE id=2026111801);
  UPDATE system_menu SET status=0,updater='registration-delete',update_time=NOW()
    WHERE id=2026111801 AND deleted=0;
  -- Only packages already containing the registration certificate page receive its delete permission.
  UPDATE system_tenant_package SET menu_ids=JSON_ARRAY_APPEND(CAST(menu_ids AS JSON),'$',2026111801),
      updater='registration-delete',update_time=NOW()
    WHERE deleted=0 AND JSON_CONTAINS(CAST(menu_ids AS JSON),CAST(certificate_page_id AS JSON),'$')
      AND NOT JSON_CONTAINS(CAST(menu_ids AS JSON),'2026111801','$');
  UPDATE system_role_menu rm JOIN system_role r ON r.id=rm.role_id AND r.tenant_id=rm.tenant_id
    JOIN system_role_menu page ON page.role_id=r.id AND page.tenant_id=r.tenant_id
      AND page.menu_id=certificate_page_id AND page.deleted=0
    SET rm.deleted=0,rm.updater='registration-delete',rm.update_time=NOW()
    WHERE rm.menu_id=2026111801 AND r.deleted=0 AND r.status=0
      AND r.code IN ('dcc_registration_certificate_approver','tenant_admin','super_admin');
  INSERT INTO system_role_menu
    (role_id,menu_id,creator,create_time,updater,update_time,deleted,tenant_id)
    SELECT r.id,2026111801,'registration-delete',NOW(),'registration-delete',NOW(),0,r.tenant_id
    FROM system_role r WHERE r.deleted=0 AND r.status=0
      AND r.code IN ('dcc_registration_certificate_approver','tenant_admin','super_admin')
      AND EXISTS (SELECT 1 FROM system_role_menu page
        WHERE page.role_id=r.id AND page.tenant_id=r.tenant_id AND page.menu_id=certificate_page_id AND page.deleted=0)
      AND NOT EXISTS (SELECT 1 FROM system_role_menu rm
        WHERE rm.role_id=r.id AND rm.tenant_id=r.tenant_id AND rm.menu_id=2026111801 AND rm.deleted=0);
  COMMIT;
END$$
DELIMITER ;
CALL ensure_registration_certificate_delete_menu();
DROP PROCEDURE ensure_registration_certificate_delete_menu;
