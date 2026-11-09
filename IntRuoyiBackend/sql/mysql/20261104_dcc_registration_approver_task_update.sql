-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260829_dcc_registration_certificate_upload_approver_role,20260714_approval_center_workflow_menu_consolidation; type=permission; riskLevel=medium
-- Registration managers approve their native BPM tasks through the shared review endpoint.
-- Grant only the existing task update permission and its navigation ancestors to this role.
SET NAMES utf8mb4;
DROP PROCEDURE IF EXISTS grant_reg_cert_approver_task_update;
DELIMITER //
CREATE PROCEDURE grant_reg_cert_approver_task_update()
BEGIN
  DECLARE EXIT HANDLER FOR SQLEXCEPTION BEGIN ROLLBACK; RESIGNAL; END;
  IF (SELECT COUNT(*) FROM system_menu WHERE permission='bpm:task:update' AND status=0 AND deleted=0)<>1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Existing BPM task update permission missing or duplicated';
  END IF;
  DROP TEMPORARY TABLE IF EXISTS reg_cert_approver_update_menus;
  CREATE TEMPORARY TABLE reg_cert_approver_update_menus (id BIGINT PRIMARY KEY);
  INSERT INTO reg_cert_approver_update_menus
  WITH RECURSIVE menu_tree AS (
    SELECT id,parent_id FROM system_menu WHERE deleted=0 AND status=0
      AND permission='bpm:task:update'
    UNION DISTINCT
    SELECT parent.id,parent.parent_id FROM system_menu parent JOIN menu_tree child ON child.parent_id=parent.id
      WHERE parent.deleted=0 AND parent.status=0
  ) SELECT id FROM menu_tree;
  IF EXISTS (SELECT 1 FROM system_tenant_package p JOIN system_tenant t ON t.package_id=p.id
      JOIN system_role r ON r.tenant_id=t.id AND r.code='dcc_registration_certificate_approver' AND r.deleted=0 AND r.status=0
      WHERE p.deleted=0 AND t.deleted=0 AND t.status=0 AND (p.menu_ids IS NULL OR NOT JSON_VALID(p.menu_ids))) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Registration manager tenant package menu JSON invalid';
  END IF;
  START TRANSACTION;
  INSERT INTO system_role_menu(role_id,menu_id,creator,updater,deleted,tenant_id)
    SELECT r.id,m.id,'reg-cert-review-access','reg-cert-review-access',0,r.tenant_id
    FROM system_role r JOIN system_tenant t ON t.id=r.tenant_id AND t.deleted=0 AND t.status=0
    CROSS JOIN reg_cert_approver_update_menus m
    WHERE r.code='dcc_registration_certificate_approver' AND r.status=0 AND r.deleted=0
      AND NOT EXISTS (SELECT 1 FROM system_role_menu x WHERE x.role_id=r.id AND x.tenant_id=r.tenant_id AND x.menu_id=m.id AND x.deleted=0);
  UPDATE system_tenant_package p JOIN (
    SELECT existing.id,JSON_ARRAYAGG(m.id) AS added_menu_ids
    FROM system_tenant_package existing CROSS JOIN reg_cert_approver_update_menus m
    WHERE existing.deleted=0 AND existing.status=0 AND JSON_VALID(existing.menu_ids)
      AND NOT JSON_CONTAINS(CAST(existing.menu_ids AS JSON),CAST(m.id AS JSON),'$')
    GROUP BY existing.id
  ) additions ON additions.id=p.id
    SET p.menu_ids=JSON_MERGE_PRESERVE(CAST(p.menu_ids AS JSON),additions.added_menu_ids),
      p.updater='reg-cert-review-access',p.update_time=NOW()
    WHERE p.deleted=0 AND p.status=0
      AND EXISTS (SELECT 1 FROM system_tenant t JOIN system_role r ON r.tenant_id=t.id
        WHERE t.package_id=p.id AND t.deleted=0 AND t.status=0
          AND r.code='dcc_registration_certificate_approver' AND r.deleted=0 AND r.status=0);
  COMMIT;
  DROP TEMPORARY TABLE reg_cert_approver_update_menus;
END//
DELIMITER ;
CALL grant_reg_cert_approver_task_update();
DROP PROCEDURE grant_reg_cert_approver_task_update;
