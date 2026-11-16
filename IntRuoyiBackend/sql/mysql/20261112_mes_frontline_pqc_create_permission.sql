-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20261112_mes_pqc_role_navigation_boundary; type=permission; riskLevel=low
-- Grant existing frontline inspectors/leaders only; retain their current navigation.
-- Recovery: remove only grants/menu created by frontline-pqc-create and advance tenant authority revisions.
SET NAMES utf8mb4;
DROP PROCEDURE IF EXISTS grant_mes_frontline_pqc_create;
DELIMITER $$
CREATE PROCEDURE grant_mes_frontline_pqc_create()
BEGIN
  DECLARE createMenuId BIGINT;
  DECLARE EXIT HANDLER FOR SQLEXCEPTION BEGIN ROLLBACK; RESIGNAL; END;
  START TRANSACTION;
  IF NOT EXISTS (SELECT 1 FROM system_menu WHERE id=900438 AND deleted=0 AND status=0
      AND component='mes/pro/edhr-batch/BatchPqcFillPage') THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Missing active frontline PQC page';
  END IF;
  DROP TEMPORARY TABLE IF EXISTS tmp_frontline_pqc_create_role;
  CREATE TEMPORARY TABLE tmp_frontline_pqc_create_role (id BIGINT PRIMARY KEY,tenant_id BIGINT,package_id BIGINT);
  INSERT INTO tmp_frontline_pqc_create_role
    SELECT r.id,r.tenant_id,t.package_id FROM system_role r
    JOIN system_tenant t ON t.id=r.tenant_id AND t.deleted=0 AND t.status=0
    WHERE r.code IN('pqc_permission','pqc_leader_permission') AND r.deleted=0 AND r.status=0
      AND EXISTS (SELECT 1 FROM system_role_menu rm WHERE rm.role_id=r.id AND rm.tenant_id=r.tenant_id
        AND rm.menu_id=900438 AND rm.deleted=0);
  IF NOT EXISTS (SELECT 1 FROM tmp_frontline_pqc_create_role) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Missing enabled frontline PQC roles';
  END IF;
  IF EXISTS (SELECT 1 FROM tmp_frontline_pqc_create_role r LEFT JOIN system_approval_authority_revision a
      ON a.tenant_id=r.tenant_id WHERE a.tenant_id IS NULL) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Missing frontline PQC authority revision';
  END IF;
  IF EXISTS (SELECT 1 FROM tmp_frontline_pqc_create_role r LEFT JOIN system_tenant_package p
      ON p.id=r.package_id AND p.deleted=0 AND p.status=0
      WHERE r.package_id<>-1 AND NOT(r.tenant_id=1 AND r.package_id=0)
        AND (p.id IS NULL OR NOT JSON_VALID(p.menu_ids)
          OR NOT JSON_CONTAINS(p.menu_ids,CAST('900438' AS JSON),'$'))) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Missing authority revision or valid frontline PQC tenant package';
  END IF;
  -- Serialize permission changes against the same tenant authority records as application commands.
  UPDATE system_approval_authority_revision a JOIN
    (SELECT DISTINCT tenant_id FROM tmp_frontline_pqc_create_role) r ON r.tenant_id=a.tenant_id
    SET a.revision=a.revision;
  IF (SELECT COUNT(*) FROM system_menu WHERE parent_id=900438
      AND permission='mes:pro-feedback:create' AND deleted=0)>1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Duplicate frontline PQC create permission';
  END IF;
  INSERT INTO system_menu(name,permission,type,sort,parent_id,path,status,visible,keep_alive,always_show,
      creator,create_time,updater,update_time,deleted)
    SELECT 'PQC巡检操作','mes:pro-feedback:create',3,2,900438,'',0,b'0',b'1',b'0',
      'frontline-pqc-create',NOW(),'frontline-pqc-create',NOW(),b'0'
    WHERE NOT EXISTS (SELECT 1 FROM system_menu WHERE parent_id=900438
      AND permission='mes:pro-feedback:create' AND deleted=0);
  SELECT id INTO createMenuId FROM system_menu WHERE parent_id=900438
    AND permission='mes:pro-feedback:create' AND deleted=0 AND status=0 AND type=3;
  IF createMenuId IS NULL THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Frontline PQC create permission is disabled or invalid';
  END IF;
  INSERT INTO system_role_menu(role_id,menu_id,creator,create_time,updater,update_time,deleted,tenant_id)
    SELECT r.id,createMenuId,'frontline-pqc-create',NOW(),'frontline-pqc-create',NOW(),b'0',r.tenant_id
    FROM tmp_frontline_pqc_create_role r WHERE NOT EXISTS (SELECT 1 FROM system_role_menu rm
      WHERE rm.role_id=r.id AND rm.tenant_id=r.tenant_id AND rm.menu_id=createMenuId AND rm.deleted=0);
  UPDATE system_tenant_package p JOIN
    (SELECT DISTINCT package_id FROM tmp_frontline_pqc_create_role) r ON r.package_id=p.id
    SET p.menu_ids=JSON_ARRAY_APPEND(p.menu_ids,'$',createMenuId),p.updater='frontline-pqc-create',p.update_time=NOW()
    WHERE NOT JSON_CONTAINS(p.menu_ids,CAST(createMenuId AS JSON),'$');
  UPDATE system_approval_authority_revision a JOIN
    (SELECT DISTINCT tenant_id FROM tmp_frontline_pqc_create_role) r ON r.tenant_id=a.tenant_id
    SET a.revision=a.revision+1,a.updated_at=NOW();
  COMMIT;
  DROP TEMPORARY TABLE tmp_frontline_pqc_create_role;
END$$
DELIMITER ;
CALL grant_mes_frontline_pqc_create();
DROP PROCEDURE grant_mes_frontline_pqc_create;
