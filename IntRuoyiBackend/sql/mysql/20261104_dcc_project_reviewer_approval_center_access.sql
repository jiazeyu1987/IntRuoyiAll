-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20261013_dcc_project_reviewer_role,20260714_approval_center_workflow_menu_consolidation; type=permission; riskLevel=medium
-- Existing project reviewers need the shared query gate and its TODO/DONE navigation.
-- Review continues in the project module; no bpm update or global approval role is granted.
SET NAMES utf8mb4;
DROP PROCEDURE IF EXISTS grant_project_reviewer_approval_center;
DELIMITER //
CREATE PROCEDURE grant_project_reviewer_approval_center()
BEGIN
  DECLARE EXIT HANDLER FOR SQLEXCEPTION BEGIN ROLLBACK; RESIGNAL; END;
  IF NOT EXISTS (SELECT 1 FROM system_menu WHERE permission='bpm:task:query' AND status=0 AND deleted=0)
     OR (SELECT COUNT(*) FROM system_menu WHERE component='approval-center/index'
       AND path IN ('todo','done') AND status=0 AND deleted=0)<>2 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Existing approval query or TODO/DONE menu missing';
  END IF;
  DROP TEMPORARY TABLE IF EXISTS project_reviewer_approval_menus;
  CREATE TEMPORARY TABLE project_reviewer_approval_menus (id BIGINT PRIMARY KEY);
  INSERT INTO project_reviewer_approval_menus
  WITH RECURSIVE menu_tree AS (
    SELECT id,parent_id FROM system_menu WHERE deleted=0 AND status=0
      AND (permission='bpm:task:query' OR (component='approval-center/index' AND path IN ('todo','done')))
    UNION DISTINCT
    SELECT parent.id,parent.parent_id FROM system_menu parent JOIN menu_tree child ON child.parent_id=parent.id
      WHERE parent.deleted=0 AND parent.status=0
  ) SELECT id FROM menu_tree;
  IF EXISTS (SELECT 1 FROM system_tenant_package p JOIN system_tenant t ON t.package_id=p.id
      JOIN system_role r ON r.tenant_id=t.id AND r.code='dcc_project_reviewer' AND r.deleted=0 AND r.status=0
      WHERE p.deleted=0 AND t.deleted=0 AND t.status=0 AND NOT JSON_VALID(p.menu_ids)) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Project reviewer tenant package menu JSON invalid';
  END IF;
  START TRANSACTION;
  INSERT INTO system_role_menu(role_id,menu_id,creator,updater,deleted,tenant_id)
    SELECT r.id,m.id,'project-review-access','project-review-access',0,r.tenant_id
    FROM system_role r JOIN system_tenant t ON t.id=r.tenant_id AND t.deleted=0 AND t.status=0
    CROSS JOIN project_reviewer_approval_menus m
    WHERE r.code='dcc_project_reviewer' AND r.status=0 AND r.deleted=0
      AND NOT EXISTS (SELECT 1 FROM system_role_menu x WHERE x.role_id=r.id AND x.tenant_id=r.tenant_id AND x.menu_id=m.id AND x.deleted=0);
  UPDATE system_tenant_package p JOIN (
    SELECT existing.id,JSON_ARRAYAGG(m.id) AS added_menu_ids
    FROM system_tenant_package existing CROSS JOIN project_reviewer_approval_menus m
    WHERE existing.deleted=0 AND existing.status=0 AND JSON_VALID(existing.menu_ids)
      AND NOT JSON_CONTAINS(CAST(existing.menu_ids AS JSON),CAST(m.id AS JSON),'$')
    GROUP BY existing.id
  ) additions ON additions.id=p.id
    SET p.menu_ids=JSON_MERGE_PRESERVE(CAST(p.menu_ids AS JSON),additions.added_menu_ids),
      p.updater='project-review-access',p.update_time=NOW()
    WHERE p.deleted=0 AND p.status=0
      AND EXISTS (SELECT 1 FROM system_tenant t JOIN system_role r ON r.tenant_id=t.id
        WHERE t.package_id=p.id AND t.deleted=0 AND t.status=0
          AND r.code='dcc_project_reviewer' AND r.deleted=0 AND r.status=0);
  COMMIT;
  DROP TEMPORARY TABLE project_reviewer_approval_menus;
END//
DELIMITER ;
CALL grant_project_reviewer_approval_center();
DROP PROCEDURE grant_project_reviewer_approval_center;
