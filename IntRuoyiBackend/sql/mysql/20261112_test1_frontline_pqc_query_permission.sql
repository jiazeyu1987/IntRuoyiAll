-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260805_mes_edhr_frontline_pqc_menu; type=permission; riskLevel=low
-- Repair only the current Test1 PQC role. Do not expose the production feedback page.
-- Recovery: remove this role's grant to the child query menu and advance its authority revision.
SET NAMES utf8mb4;
DROP PROCEDURE IF EXISTS repair_test1_frontline_pqc_query;
DELIMITER $$
CREATE PROCEDURE repair_test1_frontline_pqc_query()
BEGIN
  DECLARE tenantId BIGINT;
  DECLARE roleId BIGINT;
  DECLARE packageId BIGINT;
  DECLARE queryMenuId BIGINT;
  DECLARE authorityRevision BIGINT;
  DECLARE EXIT HANDLER FOR SQLEXCEPTION BEGIN ROLLBACK; RESIGNAL; END;
  START TRANSACTION;
  IF (SELECT COUNT(*) FROM system_tenant WHERE name='测试1' AND deleted=0 AND status=0)<>1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Expected one enabled Test1 tenant';
  END IF;
  SELECT id,package_id INTO tenantId,packageId FROM system_tenant WHERE name='测试1' AND deleted=0 AND status=0;
  IF (SELECT COUNT(*) FROM system_role WHERE tenant_id=tenantId AND code='pqc_permission' AND deleted=0 AND status=0)<>1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Expected one enabled Test1 PQC role';
  END IF;
  SELECT id INTO roleId FROM system_role WHERE tenant_id=tenantId AND code='pqc_permission' AND deleted=0 AND status=0;
  IF NOT EXISTS (SELECT 1 FROM system_menu WHERE id=900438 AND deleted=0 AND status=0
      AND component='mes/pro/edhr-batch/BatchPqcFillPage')
    OR NOT EXISTS (SELECT 1 FROM system_role_menu WHERE tenant_id=tenantId AND role_id=roleId AND menu_id=900438 AND deleted=0)
    OR NOT EXISTS (SELECT 1 FROM system_approval_authority_revision WHERE tenant_id=tenantId) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Missing frontline PQC page grant or authority revision';
  END IF;
  SELECT revision INTO authorityRevision FROM system_approval_authority_revision WHERE tenant_id=tenantId FOR UPDATE;
  IF packageId<>-1 AND NOT EXISTS (SELECT 1 FROM system_tenant_package WHERE id=packageId AND deleted=0 AND status=0
      AND JSON_VALID(menu_ids) AND JSON_CONTAINS(menu_ids,CAST('900438' AS JSON),'$')) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Missing valid frontline PQC tenant package';
  END IF;
  IF (SELECT COUNT(*) FROM system_menu WHERE parent_id=900438 AND permission='mes:pro-feedback:query' AND deleted=0)>1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Duplicate frontline PQC query menu';
  END IF;
  INSERT INTO system_menu (name,permission,type,sort,parent_id,path,status,visible,keep_alive,always_show,
      creator,create_time,updater,update_time,deleted)
    SELECT 'PQC任务查询','mes:pro-feedback:query',3,1,900438,'',0,b'1',b'1',b'1',
      'test1-pqc-query-fix',NOW(),'test1-pqc-query-fix',NOW(),b'0'
    WHERE NOT EXISTS (SELECT 1 FROM system_menu WHERE parent_id=900438 AND permission='mes:pro-feedback:query' AND deleted=0);
  SELECT id INTO queryMenuId FROM system_menu WHERE parent_id=900438 AND permission='mes:pro-feedback:query' AND deleted=0 AND status=0;
  IF queryMenuId IS NULL THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Frontline PQC query menu is disabled';
  END IF;
  UPDATE system_tenant_package SET menu_ids=JSON_ARRAY_APPEND(menu_ids,'$',queryMenuId),
      updater='test1-pqc-query-fix',update_time=NOW()
    WHERE id=packageId AND NOT JSON_CONTAINS(menu_ids,CAST(queryMenuId AS JSON),'$');
  INSERT INTO system_role_menu (role_id,menu_id,creator,create_time,updater,update_time,deleted,tenant_id)
    SELECT roleId,queryMenuId,'test1-pqc-query-fix',NOW(),'test1-pqc-query-fix',NOW(),b'0',tenantId
    WHERE NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id=roleId AND tenant_id=tenantId AND menu_id=queryMenuId AND deleted=0);
  UPDATE system_approval_authority_revision SET revision=revision+1,updated_at=NOW() WHERE tenant_id=tenantId;
  COMMIT;
END$$
DELIMITER ;
CALL repair_test1_frontline_pqc_query();
DROP PROCEDURE repair_test1_frontline_pqc_query;
