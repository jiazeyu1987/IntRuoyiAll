-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20261026_edhr_lifecycle_directories; type=menu; riskLevel=low
-- Restore only read permissions for the retained standalone form page.
SET NAMES utf8mb4;
DROP PROCEDURE IF EXISTS restore_edhr_form_read_permissions;
DELIMITER $$
CREATE PROCEDURE restore_edhr_form_read_permissions()
BEGIN
  DECLARE EXIT HANDLER FOR SQLEXCEPTION
  BEGIN
    ROLLBACK;
    RESIGNAL;
  END;
  START TRANSACTION;
  IF NOT EXISTS (SELECT 1 FROM system_menu WHERE id=900272 AND deleted=b'0'
      AND type=2 AND component='mes/pro/edhr-form/FormPage'
      AND permission='mes:pro-edhr-form-instance:query') THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Standalone form page identity mismatch';
  END IF;
  IF NOT EXISTS (SELECT 1 FROM system_menu WHERE id=991512 AND path='/mes/edhr-production-forms'
      AND deleted=b'0' AND status=0) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Missing production forms directory';
  END IF;
  INSERT INTO system_menu
    (name,permission,type,sort,parent_id,path,icon,component,status,visible,keep_alive,always_show,creator,updater)
  SELECT '独立表单实例查询','mes:pro-edhr-form-instance:query',3,31,991512,'','','',0,b'0',b'0',b'0','edhr-form-read-fix','edhr-form-read-fix'
  WHERE NOT EXISTS (SELECT 1 FROM system_menu WHERE permission='mes:pro-edhr-form-instance:query' AND deleted=b'0');
  INSERT INTO system_menu
    (name,permission,type,sort,parent_id,path,icon,component,status,visible,keep_alive,always_show,creator,updater)
  SELECT '独立表单模板查询','mes:pro-edhr-form-template:query',3,32,991512,'','','',0,b'0',b'0',b'0','edhr-form-read-fix','edhr-form-read-fix'
  WHERE NOT EXISTS (SELECT 1 FROM system_menu WHERE permission='mes:pro-edhr-form-template:query' AND deleted=b'0');
  UPDATE system_menu SET status=0,updater='edhr-form-read-fix',update_time=NOW()
  WHERE permission IN ('mes:pro-edhr-form-instance:query','mes:pro-edhr-form-template:query') AND deleted=b'0';
  -- Authorize packages already holding the production forms directory or standalone form page.
  UPDATE system_tenant_package p JOIN (
    SELECT MIN(id) AS id FROM system_menu WHERE permission='mes:pro-edhr-form-instance:query' AND deleted=b'0'
  ) m ON m.id IS NOT NULL
  SET p.menu_ids=JSON_ARRAY_APPEND(p.menu_ids,'$',m.id),p.updater='edhr-form-read-fix',p.update_time=NOW()
  WHERE p.deleted=b'0' AND (JSON_CONTAINS(p.menu_ids,CAST('991512' AS JSON),'$')
      OR JSON_CONTAINS(p.menu_ids,CAST('900272' AS JSON),'$'))
    AND NOT JSON_CONTAINS(p.menu_ids,CAST(m.id AS JSON),'$');
  UPDATE system_tenant_package p JOIN (
    SELECT MIN(id) AS id FROM system_menu WHERE permission='mes:pro-edhr-form-template:query' AND deleted=b'0'
  ) m ON m.id IS NOT NULL
  SET p.menu_ids=JSON_ARRAY_APPEND(p.menu_ids,'$',m.id),p.updater='edhr-form-read-fix',p.update_time=NOW()
  WHERE p.deleted=b'0' AND (JSON_CONTAINS(p.menu_ids,CAST('991512' AS JSON),'$')
      OR JSON_CONTAINS(p.menu_ids,CAST('900272' AS JSON),'$'))
    AND NOT JSON_CONTAINS(p.menu_ids,CAST(m.id AS JSON),'$');
  INSERT INTO system_role_menu (role_id,menu_id,tenant_id,creator,updater)
  SELECT r.id,m.id,r.tenant_id,'edhr-form-read-fix','edhr-form-read-fix'
  FROM system_role r JOIN system_tenant t ON t.id=r.tenant_id AND t.deleted=b'0'
  JOIN system_tenant_package p ON p.id=t.package_id AND p.deleted=b'0'
  JOIN system_menu m ON m.permission IN ('mes:pro-edhr-form-instance:query','mes:pro-edhr-form-template:query') AND m.deleted=b'0'
  WHERE r.deleted=b'0' AND r.status=0 AND r.code='tenant_admin'
    AND JSON_CONTAINS(p.menu_ids,CAST(m.id AS JSON),'$')
    AND (JSON_CONTAINS(p.menu_ids,CAST('991512' AS JSON),'$') OR JSON_CONTAINS(p.menu_ids,CAST('900272' AS JSON),'$'))
    AND NOT EXISTS (SELECT 1 FROM system_role_menu rm WHERE rm.role_id=r.id AND rm.tenant_id=r.tenant_id AND rm.menu_id=m.id AND rm.deleted=b'0');
  COMMIT;
END$$
DELIMITER ;
CALL restore_edhr_form_read_permissions();
DROP PROCEDURE restore_edhr_form_read_permissions;
