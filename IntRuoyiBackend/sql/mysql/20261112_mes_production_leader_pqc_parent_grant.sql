-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20261112_mes_production_leader_frontline_tab; type=permission; riskLevel=low
-- AuthController expands granted button menus to their parents. Grant 900439 therefore exposes PQC page 900435.
-- Production page 900436 already supplies the identical team-leader query permission.
-- Recovery: restore only the captured 900439 bindings and advance affected authority revisions.
SET NAMES utf8mb4;
DROP PROCEDURE IF EXISTS remove_production_leader_pqc_parent_grant;
DELIMITER $$
CREATE PROCEDURE remove_production_leader_pqc_parent_grant()
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
  IF (SELECT COUNT(*) FROM system_menu WHERE deleted=0 AND status=0
    AND permission='mes:pro-process-pool-team-leader:query'
    AND ((id=900439 AND type=3 AND parent_id=900435)
      OR (id=900436 AND type=2 AND component='mes/pro/processpool/ProductionLeaderWorkbenchPage')))<>2 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Production and PQC shared query menu definitions changed';
  END IF;
  IF EXISTS (SELECT 1 FROM system_role r
    JOIN system_tenant t ON t.id=r.tenant_id AND t.deleted=0 AND t.status=0
    LEFT JOIN system_approval_authority_revision a ON a.tenant_id=r.tenant_id
    WHERE r.code='production_leader_permission' AND r.deleted=0 AND r.status=0
      AND (a.tenant_id IS NULL OR NOT EXISTS (SELECT 1 FROM system_role_menu rm
        WHERE rm.role_id=r.id AND rm.tenant_id=r.tenant_id AND rm.menu_id=900436 AND rm.deleted=0))) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Missing production query grant or tenant authority revision';
  END IF;
  OPEN targets;
  lock_loop: LOOP
    FETCH targets INTO target_tenant;
    IF done THEN LEAVE lock_loop; END IF;
    SELECT revision INTO locked_revision FROM system_approval_authority_revision
      WHERE tenant_id=target_tenant FOR UPDATE;
  END LOOP;
  CLOSE targets;
  DROP TEMPORARY TABLE IF EXISTS tmp_production_pqc_parent_tenant;
  CREATE TEMPORARY TABLE tmp_production_pqc_parent_tenant (id BIGINT PRIMARY KEY);
  INSERT INTO tmp_production_pqc_parent_tenant SELECT DISTINCT r.tenant_id FROM system_role r
    JOIN system_tenant t ON t.id=r.tenant_id AND t.deleted=0 AND t.status=0
    JOIN system_role_menu rm ON rm.role_id=r.id AND rm.tenant_id=r.tenant_id AND rm.deleted=0 AND rm.menu_id=900439
    WHERE r.code='production_leader_permission' AND r.deleted=0 AND r.status=0;
  UPDATE system_role_menu rm
    JOIN system_role r ON r.id=rm.role_id AND r.tenant_id=rm.tenant_id
    JOIN tmp_production_pqc_parent_tenant t ON t.id=r.tenant_id
    SET rm.deleted=b'1',rm.updater='production-pqc-parent-fix',rm.update_time=NOW()
    WHERE r.code='production_leader_permission' AND r.deleted=0 AND r.status=0
      AND rm.menu_id=900439 AND rm.deleted=0;
  UPDATE system_approval_authority_revision a JOIN tmp_production_pqc_parent_tenant t ON t.id=a.tenant_id
    SET a.revision=a.revision+1,a.updated_at=NOW();
  COMMIT;
  DROP TEMPORARY TABLE tmp_production_pqc_parent_tenant;
END$$
DELIMITER ;
CALL remove_production_leader_pqc_parent_grant();
DROP PROCEDURE remove_production_leader_pqc_parent_grant;
