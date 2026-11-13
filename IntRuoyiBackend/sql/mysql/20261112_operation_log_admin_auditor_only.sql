-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20261112_unified_operation_log_navigation; type=permission; riskLevel=medium
-- Remove only the operation-log shell and system log query/export grants from other roles.
-- Business log-source permissions and tenant package scopes remain unchanged.
-- Recovery: restore captured role-menu rows and advance affected authority revisions.
SET NAMES utf8mb4;
DROP PROCEDURE IF EXISTS restrict_operation_log_roles;
DELIMITER $$
CREATE PROCEDURE restrict_operation_log_roles()
BEGIN
  DECLARE done BOOLEAN DEFAULT FALSE;
  DECLARE target_tenant BIGINT;
  DECLARE locked_revision BIGINT;
  DECLARE targets CURSOR FOR SELECT DISTINCT r.tenant_id FROM system_role r
    JOIN system_tenant t ON t.id=r.tenant_id AND t.deleted=0 AND t.status=0
    WHERE r.deleted=0 ORDER BY r.tenant_id;
  DECLARE CONTINUE HANDLER FOR NOT FOUND SET done=TRUE;
  DECLARE EXIT HANDLER FOR SQLEXCEPTION BEGIN ROLLBACK; RESIGNAL; END;
  START TRANSACTION;
  IF (SELECT COUNT(*) FROM system_menu WHERE deleted=0 AND status=0 AND
      ((id=500 AND component='system/operatelog/index')
        OR (id=1040 AND permission='system:operate-log:query')
        OR (id=1042 AND permission='system:operate-log:export')))<>3 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Operation log menu definitions changed';
  END IF;
  IF EXISTS (SELECT 1 FROM system_role r
    JOIN system_tenant t ON t.id=r.tenant_id AND t.deleted=0 AND t.status=0
    LEFT JOIN system_approval_authority_revision a ON a.tenant_id=r.tenant_id
    WHERE r.deleted=0 AND a.tenant_id IS NULL) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Missing tenant authority revision';
  END IF;
  OPEN targets;
  lock_loop: LOOP
    FETCH targets INTO target_tenant;
    IF done THEN LEAVE lock_loop; END IF;
    SELECT revision INTO locked_revision FROM system_approval_authority_revision
      WHERE tenant_id=target_tenant FOR UPDATE;
  END LOOP;
  CLOSE targets;
  DROP TEMPORARY TABLE IF EXISTS tmp_operation_log_restricted_tenant;
  CREATE TEMPORARY TABLE tmp_operation_log_restricted_tenant (id BIGINT PRIMARY KEY);
  INSERT INTO tmp_operation_log_restricted_tenant SELECT DISTINCT r.tenant_id FROM system_role r
    JOIN system_tenant t ON t.id=r.tenant_id AND t.deleted=0 AND t.status=0
    JOIN system_role_menu rm ON rm.role_id=r.id AND rm.tenant_id=r.tenant_id AND rm.deleted=0
    WHERE r.deleted=0 AND r.code NOT IN('super_admin','tenant_admin','audit_admin','edhr_auditor')
      AND rm.menu_id IN(500,1040,1042);
  UPDATE system_role_menu rm JOIN system_role r ON r.id=rm.role_id AND r.tenant_id=rm.tenant_id
    JOIN tmp_operation_log_restricted_tenant t ON t.id=r.tenant_id
    SET rm.deleted=b'1',rm.updater='operation-log-role-boundary',rm.update_time=NOW()
    WHERE r.deleted=0 AND r.code NOT IN('super_admin','tenant_admin','audit_admin','edhr_auditor')
      AND rm.deleted=0 AND rm.menu_id IN(500,1040,1042);
  UPDATE system_approval_authority_revision a JOIN tmp_operation_log_restricted_tenant t ON t.id=a.tenant_id
    SET a.revision=a.revision+1,a.updated_at=NOW();
  COMMIT;
  DROP TEMPORARY TABLE tmp_operation_log_restricted_tenant;
END$$
DELIMITER ;
CALL restrict_operation_log_roles();
DROP PROCEDURE restrict_operation_log_roles;
