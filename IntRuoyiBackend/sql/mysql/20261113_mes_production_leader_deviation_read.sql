-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20261112_mes_production_leader_pqc_parent_grant,20260924_mes_edhr_deviation_management; type=permission; riskLevel=low
-- Read-only deviation trace (including linked NCR disposition) in production submission details.
-- Dedicated buttons avoid exposing the standalone deviation/NCR/batch pages through parent expansion.
-- Recovery: remove only bindings/buttons created with production-deviation-read, restore captured
-- package menu_ids, and advance affected authority revisions. Preserve pre-existing grants.
SET NAMES utf8mb4;
DROP PROCEDURE IF EXISTS grant_production_deviation_read;
DELIMITER $$
CREATE PROCEDURE grant_production_deviation_read()
BEGIN
  DECLARE done BOOLEAN DEFAULT FALSE;
  DECLARE target_tenant BIGINT;
  DECLARE locked_revision BIGINT;
  DECLARE targets CURSOR FOR SELECT DISTINCT r.tenant_id FROM system_role r
    JOIN system_tenant t ON t.id=r.tenant_id AND t.deleted=0 AND t.status=0
    WHERE r.code='production_leader_permission' AND r.deleted=0 AND r.status=0 ORDER BY r.tenant_id;
  DECLARE CONTINUE HANDLER FOR NOT FOUND SET done=TRUE;
  DECLARE EXIT HANDLER FOR SQLEXCEPTION BEGIN ROLLBACK; RESIGNAL; END;
  START TRANSACTION;
  IF NOT EXISTS (SELECT 1 FROM system_menu WHERE id=900436 AND deleted=0 AND status=0 AND type=2
      AND permission='mes:pro-process-pool-team-leader:query'
      AND component='mes/pro/processpool/ProductionLeaderWorkbenchPage')
    OR NOT EXISTS (SELECT 1 FROM system_menu WHERE deleted=0 AND status=0
      AND permission='mes:pro-edhr-deviation:query')
    OR NOT EXISTS (SELECT 1 FROM system_menu WHERE deleted=0 AND status=0
      AND permission='mes:pro-edhr-nonconformance-review:query') THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Missing production page or deviation/NCR query definition';
  END IF;
  IF EXISTS (SELECT 1 FROM system_menu WHERE id IN (9915820,9915821) AND
      (deleted<>0 OR status<>0 OR type<>3 OR parent_id<>900436
       OR (id=9915820 AND permission<>'mes:pro-edhr-deviation:query')
       OR (id=9915821 AND permission<>'mes:pro-edhr-nonconformance-review:query'))) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Production deviation read button identity conflict';
  END IF;
  DROP TEMPORARY TABLE IF EXISTS tmp_production_deviation_role;
  CREATE TEMPORARY TABLE tmp_production_deviation_role (id BIGINT PRIMARY KEY,tenant_id BIGINT,package_id BIGINT);
  INSERT INTO tmp_production_deviation_role SELECT r.id,r.tenant_id,t.package_id FROM system_role r
    JOIN system_tenant t ON t.id=r.tenant_id AND t.deleted=0 AND t.status=0
    WHERE r.code='production_leader_permission' AND r.deleted=0 AND r.status=0;
  IF NOT EXISTS (SELECT 1 FROM tmp_production_deviation_role) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Missing active production leader permission role';
  END IF;
  IF EXISTS (SELECT 1 FROM tmp_production_deviation_role r
      LEFT JOIN system_approval_authority_revision a ON a.tenant_id=r.tenant_id
      WHERE a.tenant_id IS NULL OR NOT EXISTS (SELECT 1 FROM system_role_menu rm
        WHERE rm.role_id=r.id AND rm.tenant_id=r.tenant_id AND rm.menu_id=900436 AND rm.deleted=0)) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Missing production page grant or authority revision';
  END IF;
  IF EXISTS (SELECT 1 FROM tmp_production_deviation_role r
      LEFT JOIN system_tenant_package p ON p.id=r.package_id AND p.deleted=0 AND p.status=0
      WHERE NOT (r.tenant_id=1 AND r.package_id=0) AND r.package_id<>-1
        AND (p.id IS NULL OR NOT JSON_VALID(p.menu_ids))) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Missing or invalid production tenant package';
  END IF;
  IF EXISTS (SELECT 1 FROM tmp_production_deviation_role r JOIN system_tenant_package p ON p.id=r.package_id
      WHERE JSON_TYPE(CAST(p.menu_ids AS JSON))<>'ARRAY'
        OR NOT JSON_CONTAINS(CAST(p.menu_ids AS JSON),'900436','$')) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Tenant package does not authorize production page';
  END IF;
  OPEN targets;
  lock_loop: LOOP
    FETCH targets INTO target_tenant;
    IF done THEN LEAVE lock_loop; END IF;
    SELECT revision INTO locked_revision FROM system_approval_authority_revision
      WHERE tenant_id=target_tenant FOR UPDATE;
  END LOOP;
  CLOSE targets;
  INSERT INTO system_menu
    (id,name,permission,type,sort,parent_id,path,icon,component,component_name,status,
     visible,keep_alive,always_show,creator,create_time,updater,update_time,deleted)
  SELECT 9915820,'生产组长偏差追溯查询','mes:pro-edhr-deviation:query',3,20,900436,'','','','',0,
    b'0',b'0',b'0','production-deviation-read',NOW(),'production-deviation-read',NOW(),b'0'
  WHERE NOT EXISTS (SELECT 1 FROM system_menu WHERE id=9915820);
  INSERT INTO system_menu
    (id,name,permission,type,sort,parent_id,path,icon,component,component_name,status,
     visible,keep_alive,always_show,creator,create_time,updater,update_time,deleted)
  SELECT 9915821,'生产组长偏差关联处置查询','mes:pro-edhr-nonconformance-review:query',3,21,900436,'','','','',0,
    b'0',b'0',b'0','production-deviation-read',NOW(),'production-deviation-read',NOW(),b'0'
  WHERE NOT EXISTS (SELECT 1 FROM system_menu WHERE id=9915821);
  DROP TEMPORARY TABLE IF EXISTS tmp_production_deviation_changed;
  CREATE TEMPORARY TABLE tmp_production_deviation_changed (id BIGINT PRIMARY KEY);
  INSERT INTO tmp_production_deviation_changed SELECT DISTINCT r.tenant_id FROM tmp_production_deviation_role r
    WHERE EXISTS (SELECT 1 FROM system_menu m WHERE m.id IN (9915820,9915821)
      AND NOT EXISTS (SELECT 1 FROM system_role_menu rm WHERE rm.role_id=r.id AND rm.tenant_id=r.tenant_id
        AND rm.menu_id=m.id AND rm.deleted=0))
    OR EXISTS (SELECT 1 FROM system_tenant_package p WHERE p.id=r.package_id AND
      (NOT JSON_CONTAINS(CAST(p.menu_ids AS JSON),'9915820','$')
       OR NOT JSON_CONTAINS(CAST(p.menu_ids AS JSON),'9915821','$')));
  UPDATE system_tenant_package p JOIN (SELECT DISTINCT package_id FROM tmp_production_deviation_role) r
    ON r.package_id=p.id SET p.menu_ids=JSON_ARRAY_APPEND(CAST(p.menu_ids AS JSON),'$',9915820),
    p.updater='production-deviation-read',p.update_time=NOW()
    WHERE NOT JSON_CONTAINS(CAST(p.menu_ids AS JSON),'9915820','$');
  UPDATE system_tenant_package p JOIN (SELECT DISTINCT package_id FROM tmp_production_deviation_role) r
    ON r.package_id=p.id SET p.menu_ids=JSON_ARRAY_APPEND(CAST(p.menu_ids AS JSON),'$',9915821),
    p.updater='production-deviation-read',p.update_time=NOW()
    WHERE NOT JSON_CONTAINS(CAST(p.menu_ids AS JSON),'9915821','$');
  INSERT INTO system_role_menu (role_id,menu_id,creator,create_time,updater,update_time,deleted,tenant_id)
    SELECT r.id,m.id,'production-deviation-read',NOW(),'production-deviation-read',NOW(),b'0',r.tenant_id
    FROM tmp_production_deviation_role r JOIN system_menu m ON m.id IN (9915820,9915821)
    WHERE NOT EXISTS (SELECT 1 FROM system_role_menu rm WHERE rm.role_id=r.id AND rm.tenant_id=r.tenant_id
      AND rm.menu_id=m.id AND rm.deleted=0);
  UPDATE system_approval_authority_revision a JOIN tmp_production_deviation_changed t ON t.id=a.tenant_id
    SET a.revision=a.revision+1,a.updated_at=NOW();
  COMMIT;
  DROP TEMPORARY TABLE tmp_production_deviation_changed;
  DROP TEMPORARY TABLE tmp_production_deviation_role;
END$$
DELIMITER ;
CALL grant_production_deviation_read();
DROP PROCEDURE grant_production_deviation_read;
