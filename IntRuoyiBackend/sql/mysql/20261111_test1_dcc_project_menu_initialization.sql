-- release-migration: allowedEnvironments=test; dependsOn=; type=menu; riskLevel=medium
-- Local test1 repair: retain existing business permissions and package scope.
-- The later PQC navigation boundary intentionally removed project access from the PQC leader.
-- Repair ancestors only for the QA and process engineer roles that retain project query access.
-- Preconditions deliberately identify this tenant, package, roles and menu definitions.
-- Recovery: restore menu 990210's captured parent/updater/time and remove ONLY the
-- newly inserted role-menu IDs from the execution before/after record, in a reviewed
-- transaction with authority revision invalidation. Never replay the older migration:
-- it additionally hides menu 991501 and grants ancestors outside this repair scope.
SET NAMES utf8mb4;
DROP PROCEDURE IF EXISTS repair_test1_dcc_project_menu;
DELIMITER $$
CREATE PROCEDURE repair_test1_dcc_project_menu()
BEGIN
  DECLARE finished BOOLEAN DEFAULT FALSE;
  DECLARE current_tenant BIGINT;
  DECLARE locked_revision BIGINT;
  DECLARE parent_changed INT DEFAULT 0;
  DECLARE grants_added INT DEFAULT 0;
  -- Same tenant enumeration and lock order as GlobalApprovalAuthorityCoordinator.
  DECLARE tenants CURSOR FOR
    SELECT DISTINCT r.tenant_id FROM system_role r JOIN system_tenant t ON t.id=r.tenant_id
    WHERE r.deleted=0 AND t.deleted=0 AND r.tenant_id>0 ORDER BY r.tenant_id;
  DECLARE CONTINUE HANDLER FOR NOT FOUND SET finished=TRUE;
  DECLARE EXIT HANDLER FOR SQLEXCEPTION
  BEGIN
    ROLLBACK;
    RESIGNAL;
  END;
  START TRANSACTION;
  IF NOT EXISTS (SELECT 1 FROM system_tenant WHERE id=186 AND name='测试1'
      AND package_id=116 AND status=0 AND deleted=0)
    OR NOT EXISTS (SELECT 1 FROM system_tenant_package WHERE id=116 AND deleted=0
      AND JSON_VALID(menu_ids) AND JSON_CONTAINS(CAST(menu_ids AS JSON),'990200')
      AND JSON_CONTAINS(CAST(menu_ids AS JSON),'990210'))
    OR (SELECT COUNT(*) FROM system_role WHERE tenant_id=186 AND status=0 AND deleted=0
      AND ((id=990819801 AND code='qa') OR (id=990819803 AND code='pqc_leader_permission')
        OR (id=990819819 AND code='process_engineer')))<>3
    OR (SELECT COUNT(DISTINCT role_id) FROM system_role_menu WHERE tenant_id=186
      AND role_id IN(990819801,990819819) AND menu_id=990210 AND deleted=0)<>2
    OR NOT EXISTS (SELECT 1 FROM system_menu WHERE id=990200 AND parent_id=0
      AND path='/mdm' AND type=1 AND status=0 AND deleted=0)
    OR NOT EXISTS (SELECT 1 FROM system_menu WHERE id=990210 AND parent_id IN(991515,990200)
      AND path='/mes/md/dcc-project-code' AND permission='dcc:project-code:query'
      AND component='dcc/controlled-file/basic-data/project-code/index'
      AND type=2 AND status=0 AND deleted=0) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='test1 DCC repair prerequisites changed';
  END IF;
  IF EXISTS (SELECT 1 FROM system_role r JOIN system_tenant t ON t.id=r.tenant_id
      LEFT JOIN system_approval_authority_revision a ON a.tenant_id=r.tenant_id
      WHERE r.deleted=0 AND t.deleted=0 AND r.tenant_id>0 AND a.tenant_id IS NULL) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Missing formal tenant authority revision';
  END IF;
  OPEN tenants;
  lock_loop: LOOP
    FETCH tenants INTO current_tenant;
    IF finished THEN LEAVE lock_loop; END IF;
    SELECT revision INTO locked_revision FROM system_approval_authority_revision
      WHERE tenant_id=current_tenant FOR UPDATE;
  END LOOP;
  CLOSE tenants;

  UPDATE system_menu SET parent_id=990200,updater='test1-dcc-menu-repair',update_time=NOW()
    WHERE id=990210 AND parent_id=991515;
  SET parent_changed=ROW_COUNT();
  INSERT INTO system_role_menu (role_id,menu_id,creator,create_time,updater,update_time,deleted,tenant_id)
  SELECT r.id,990200,'test1-dcc-menu-repair',NOW(),'test1-dcc-menu-repair',NOW(),b'0',186
  FROM system_role r WHERE r.tenant_id=186 AND r.id IN(990819801,990819819)
    AND r.deleted=0 AND NOT EXISTS (SELECT 1 FROM system_role_menu target
      WHERE target.tenant_id=186 AND target.role_id=r.id AND target.menu_id=990200 AND target.deleted=0);
  SET grants_added=ROW_COUNT();
  -- Menu hierarchy is global; invalidate the same authority scope as a menu update.
  UPDATE system_approval_authority_revision a
    SET revision=revision+1,updated_at=NOW()
    WHERE (parent_changed>0 AND EXISTS (SELECT 1 FROM system_role r JOIN system_tenant t ON t.id=r.tenant_id
      WHERE r.tenant_id=a.tenant_id AND r.deleted=0 AND t.deleted=0 AND r.tenant_id>0))
      OR (grants_added>0 AND a.tenant_id=186);
  IF (SELECT COUNT(DISTINCT role_id) FROM system_role_menu WHERE tenant_id=186
      AND role_id IN(990819801,990819819) AND menu_id=990200 AND deleted=0)<>2 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='test1 directory grant verification failed';
  END IF;
  COMMIT;
  SELECT parent_changed AS menu_parents_changed,grants_added AS directory_grants_added;
END$$
DELIMITER ;
CALL repair_test1_dcc_project_menu();
DROP PROCEDURE repair_test1_dcc_project_menu;
