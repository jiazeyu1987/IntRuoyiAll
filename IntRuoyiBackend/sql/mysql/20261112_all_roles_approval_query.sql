-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260714_approval_center_workflow_menu_consolidation; type=permission; riskLevel=medium
-- Add query-only approval access and all four personal views to every existing non-deleted role of non-deleted tenants.
-- Includes disabled roles without enabling them. Existing grants and user-role assignments are preserved.
-- Recovery: remove only role-menu rows inserted by this migration using the captured before-state;
-- restore affected package menu lists and advance tenant authority revisions. No review/update grant is added.
SET NAMES utf8mb4;
DROP PROCEDURE IF EXISTS grant_all_roles_approval_query;
DELIMITER $$
CREATE PROCEDURE grant_all_roles_approval_query()
BEGIN
  DECLARE finished INT DEFAULT 0;
  DECLARE target_tenant BIGINT;
  DECLARE current_revision BIGINT;
  DECLARE tenants CURSOR FOR
    SELECT DISTINCT t.id FROM system_tenant t JOIN system_role r ON r.tenant_id=t.id
      WHERE t.deleted=0 AND r.deleted=0 ORDER BY t.id;
  DECLARE CONTINUE HANDLER FOR NOT FOUND SET finished=1;
  DECLARE EXIT HANDLER FOR SQLEXCEPTION BEGIN ROLLBACK; RESIGNAL; END;
  START TRANSACTION;
  IF (SELECT COUNT(*) FROM system_menu WHERE deleted=0 AND status=0 AND
      ((id=1200 AND path='/approval-center') OR (id=1207 AND parent_id=1200 AND path='todo')
        OR (id=1221 AND parent_id=1207 AND type=3 AND permission='bpm:task:query')
        OR (id=1201 AND parent_id=1200 AND path='my-initiated' AND component='approval-center/index')
        OR (id=1208 AND parent_id=1200 AND path='done' AND component='approval-center/index')
        OR (id=2713 AND parent_id=1200 AND path='cc' AND component='approval-center/index'
          AND permission='bpm:process-instance-cc:query')))<>6 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Missing approval-center task query menu';
  END IF;
  IF EXISTS (SELECT 1 FROM system_tenant t JOIN system_role r ON r.tenant_id=t.id AND r.deleted=0
      LEFT JOIN system_approval_authority_revision ar ON ar.tenant_id=t.id
      WHERE t.deleted=0 AND ar.tenant_id IS NULL) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Missing target tenant authority revision';
  END IF;
  OPEN tenants;
  tenant_loop: LOOP
    FETCH tenants INTO target_tenant;
    IF finished=1 THEN LEAVE tenant_loop; END IF;
    SELECT revision INTO current_revision FROM system_approval_authority_revision
      WHERE tenant_id=target_tenant FOR UPDATE;
  END LOOP;
  CLOSE tenants;
  DROP TEMPORARY TABLE IF EXISTS tmp_approval_query_role;
  CREATE TEMPORARY TABLE tmp_approval_query_role (id BIGINT PRIMARY KEY,tenant_id BIGINT);
  INSERT INTO tmp_approval_query_role SELECT r.id,r.tenant_id FROM system_role r
    JOIN system_tenant t ON t.id=r.tenant_id AND t.deleted=0 WHERE r.deleted=0;
  IF EXISTS (SELECT 1 FROM system_tenant t JOIN tmp_approval_query_role r ON r.tenant_id=t.id
      LEFT JOIN system_tenant_package p ON p.id=t.package_id AND p.deleted=0 AND p.status=0
      WHERE NOT(t.id=1 AND t.package_id=0) AND t.package_id<>-1
        AND (p.id IS NULL OR NOT JSON_VALID(p.menu_ids))) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Missing valid target tenant menu package';
  END IF;
  DROP TEMPORARY TABLE IF EXISTS tmp_approval_query_package_menu;
  CREATE TEMPORARY TABLE tmp_approval_query_package_menu (package_id BIGINT,menu_id BIGINT,
    PRIMARY KEY(package_id,menu_id));
  INSERT IGNORE INTO tmp_approval_query_package_menu SELECT p.id,j.id FROM system_tenant_package p
    JOIN system_tenant t ON t.package_id=p.id AND t.deleted=0
    JOIN tmp_approval_query_role r ON r.tenant_id=t.id
    JOIN JSON_TABLE(p.menu_ids,'$[*]' COLUMNS(id BIGINT PATH '$')) j;
  INSERT IGNORE INTO tmp_approval_query_package_menu SELECT p.id,m.id FROM system_tenant_package p
    JOIN system_tenant t ON t.package_id=p.id AND t.deleted=0
    JOIN tmp_approval_query_role r ON r.tenant_id=t.id
    CROSS JOIN system_menu m WHERE m.id IN(1200,1201,1207,1208,1221,2713);
  UPDATE system_tenant_package p JOIN
    (SELECT package_id,JSON_ARRAYAGG(menu_id) menu_ids FROM tmp_approval_query_package_menu GROUP BY package_id) n
      ON n.package_id=p.id
    SET p.menu_ids=n.menu_ids,p.updater='all-roles-approval-query',p.update_time=NOW();
  INSERT INTO system_role_menu(role_id,menu_id,creator,create_time,updater,update_time,deleted,tenant_id)
    SELECT r.id,m.id,'all-roles-approval-query',NOW(),'all-roles-approval-query',NOW(),b'0',r.tenant_id
    FROM tmp_approval_query_role r CROSS JOIN system_menu m WHERE m.id IN(1200,1201,1207,1208,1221,2713)
      AND NOT EXISTS (SELECT 1 FROM system_role_menu rm WHERE rm.role_id=r.id AND rm.tenant_id=r.tenant_id
        AND rm.menu_id=m.id AND rm.deleted=0);
  UPDATE system_approval_authority_revision ar JOIN
    (SELECT DISTINCT tenant_id FROM tmp_approval_query_role) t ON t.tenant_id=ar.tenant_id
    SET ar.revision=ar.revision+1,ar.updated_at=NOW();
  COMMIT;
  DROP TEMPORARY TABLE tmp_approval_query_package_menu;
  DROP TEMPORARY TABLE tmp_approval_query_role;
END$$
DELIMITER ;
CALL grant_all_roles_approval_query();
DROP PROCEDURE grant_all_roles_approval_query;
