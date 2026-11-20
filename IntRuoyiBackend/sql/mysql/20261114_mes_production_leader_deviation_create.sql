-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20261113_mes_production_leader_deviation_read; type=permission; riskLevel=low
-- Scoped order entry only; does not grant standalone deviation creation or QA decisions.
SET NAMES utf8mb4;
DROP PROCEDURE IF EXISTS grant_production_deviation_create;
DELIMITER $$
CREATE PROCEDURE grant_production_deviation_create()
migration: BEGIN
  DECLARE EXIT HANDLER FOR SQLEXCEPTION BEGIN ROLLBACK; RESIGNAL; END;
  START TRANSACTION;
  IF NOT EXISTS (SELECT 1 FROM system_menu WHERE id=900436 AND deleted=0 AND status=0 AND type=2
      AND permission='mes:pro-process-pool-team-leader:query'
      AND component='mes/pro/processpool/ProductionLeaderWorkbenchPage') THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Missing production leader page';
  END IF;
  IF EXISTS (SELECT 1 FROM system_menu WHERE id=9915822 AND
      (deleted<>0 OR status<>0 OR type<>3 OR parent_id<>900436
       OR permission<>'mes:pro-process-pool-team-leader:deviation-create'))
    OR EXISTS (SELECT 1 FROM system_menu WHERE deleted=0 AND id<>9915822
       AND permission='mes:pro-process-pool-team-leader:deviation-create') THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Production deviation create button identity conflict';
  END IF;
  DROP TEMPORARY TABLE IF EXISTS tmp_production_deviation_create_role;
  CREATE TEMPORARY TABLE tmp_production_deviation_create_role (id BIGINT PRIMARY KEY,tenant_id BIGINT,package_id BIGINT);
  INSERT INTO tmp_production_deviation_create_role SELECT r.id,r.tenant_id,t.package_id FROM system_role r
    JOIN system_tenant t ON t.id=r.tenant_id AND t.deleted=0 AND t.status=0
    WHERE r.code='production_leader_permission' AND r.deleted=0 AND r.status=0;
  -- This migration only changes an existing permission role; do not invent or remap one.
  IF NOT EXISTS (SELECT 1 FROM tmp_production_deviation_create_role) THEN
    DROP TEMPORARY TABLE tmp_production_deviation_create_role;
    COMMIT;
    SELECT 'NOT_APPLICABLE: no active production_leader_permission role' AS migration_scope;
    LEAVE migration;
  END IF;
  IF EXISTS (SELECT 1 FROM tmp_production_deviation_create_role r
      LEFT JOIN system_approval_authority_revision a ON a.tenant_id=r.tenant_id
      WHERE a.tenant_id IS NULL OR NOT EXISTS (SELECT 1 FROM system_role_menu rm
        WHERE rm.role_id=r.id AND rm.tenant_id=r.tenant_id AND rm.menu_id=900436 AND rm.deleted=0)) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Missing production page grant or authority revision';
  END IF;
  IF EXISTS (SELECT 1 FROM tmp_production_deviation_create_role r
      LEFT JOIN system_tenant_package p ON p.id=r.package_id AND p.deleted=0 AND p.status=0
      WHERE NOT (r.tenant_id=1 AND r.package_id=0) AND r.package_id<>-1
        AND (p.id IS NULL OR NOT JSON_VALID(p.menu_ids))) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Missing or invalid production tenant package';
  END IF;
  IF EXISTS (SELECT 1 FROM tmp_production_deviation_create_role r JOIN system_tenant_package p ON p.id=r.package_id
      WHERE JSON_TYPE(CAST(p.menu_ids AS JSON))<>'ARRAY'
        OR NOT JSON_CONTAINS(CAST(p.menu_ids AS JSON),'900436','$')) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Tenant package does not authorize production page';
  END IF;
  DROP TEMPORARY TABLE IF EXISTS tmp_production_deviation_create_changed;
  CREATE TEMPORARY TABLE tmp_production_deviation_create_changed (id BIGINT PRIMARY KEY);
  INSERT INTO tmp_production_deviation_create_changed SELECT DISTINCT r.tenant_id FROM tmp_production_deviation_create_role r
    WHERE NOT EXISTS (SELECT 1 FROM system_role_menu rm WHERE rm.role_id=r.id AND rm.tenant_id=r.tenant_id
      AND rm.menu_id=9915822 AND rm.deleted=0)
    OR EXISTS (SELECT 1 FROM system_tenant_package p WHERE p.id=r.package_id
      AND NOT JSON_CONTAINS(CAST(p.menu_ids AS JSON),'9915822','$'));
  INSERT INTO system_menu
    (id,name,permission,type,sort,parent_id,path,icon,component,component_name,status,
     visible,keep_alive,always_show,creator,create_time,updater,update_time,deleted)
  SELECT 9915822,'生产组长订单偏差发起','mes:pro-process-pool-team-leader:deviation-create',3,22,900436,'','','','',0,
    b'0',b'0',b'0','production-deviation-create',NOW(),'production-deviation-create',NOW(),b'0'
  WHERE NOT EXISTS (SELECT 1 FROM system_menu WHERE id=9915822);
  UPDATE system_tenant_package p JOIN (SELECT DISTINCT package_id FROM tmp_production_deviation_create_role) r
    ON r.package_id=p.id SET p.menu_ids=JSON_ARRAY_APPEND(CAST(p.menu_ids AS JSON),'$',9915822),
    p.updater='production-deviation-create',p.update_time=NOW()
    WHERE NOT JSON_CONTAINS(CAST(p.menu_ids AS JSON),'9915822','$');
  INSERT INTO system_role_menu (role_id,menu_id,creator,create_time,updater,update_time,deleted,tenant_id)
    SELECT r.id,9915822,'production-deviation-create',NOW(),'production-deviation-create',NOW(),b'0',r.tenant_id
    FROM tmp_production_deviation_create_role r
    WHERE NOT EXISTS (SELECT 1 FROM system_role_menu rm WHERE rm.role_id=r.id AND rm.tenant_id=r.tenant_id
      AND rm.menu_id=9915822 AND rm.deleted=0);
  UPDATE system_approval_authority_revision a JOIN tmp_production_deviation_create_changed t ON t.id=a.tenant_id
    SET a.revision=a.revision+1,a.updated_at=NOW();
  COMMIT;
  DROP TEMPORARY TABLE tmp_production_deviation_create_changed;
  DROP TEMPORARY TABLE tmp_production_deviation_create_role;
END$$
DELIMITER ;
CALL grant_production_deviation_create();
DROP PROCEDURE grant_production_deviation_create;
