-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20261026_merge_production_tasks_into_workbench,20260918_mes_edhr_batch_execution_upload_permission,20261028_mes_edhr_dossier_completion_notify; type=menu; riskLevel=medium
-- Four existing attachment roles receive market-release read/upload and personal task read only.
SET NAMES utf8mb4;
DROP PROCEDURE IF EXISTS grant_mes_release_attachment_upload;
DELIMITER $$
CREATE PROCEDURE grant_mes_release_attachment_upload()
BEGIN
  DECLARE added INT DEFAULT 1;
  DECLARE EXIT HANDLER FOR SQLEXCEPTION BEGIN ROLLBACK; RESIGNAL; END;
  IF (SELECT COUNT(*) FROM system_menu WHERE deleted=b'0' AND status=0
      AND ((id=900033 AND type=2 AND permission='mes:pro-edhr-batch-execution:query')
        OR (id=900034 AND parent_id=900033 AND type=3 AND permission='mes:pro-edhr-batch-execution:query')
        OR (id=900043 AND parent_id=900033 AND type=3 AND permission='mes:pro-edhr-batch-execution:upload')
        OR (id=900230 AND parent_id=991516 AND type=3 AND path='' AND component='' AND permission='mes:pro-edhr-work-task:query')
        OR (id=900231 AND parent_id=991516 AND type=3 AND permission='mes:pro-edhr-work-task:query'))) <> 5 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Missing or invalid release upload/read/task menus';
  END IF;
  DROP TEMPORARY TABLE IF EXISTS tmp_release_upload_roles;
  CREATE TEMPORARY TABLE tmp_release_upload_roles (id BIGINT PRIMARY KEY, tenant_id BIGINT);
  INSERT INTO tmp_release_upload_roles SELECT id,tenant_id FROM system_role WHERE deleted=b'0' AND status=0 AND code IN
    ('BATCH_ATTACHMENT_INCOMING_INSPECTION_REPORT_UPLOAD_1','BATCH_ATTACHMENT_STERILIZATION_REPORT_UPLOAD_1',
     'BATCH_ATTACHMENT_FINISHED_PRODUCT_INSPECTION_REPORT_UPLOAD_1','BATCH_ATTACHMENT_FINISHED_PRODUCT_INSPECTION_RECORD_UPLOAD_1');
  DROP TEMPORARY TABLE IF EXISTS tmp_release_upload_menus;
  CREATE TEMPORARY TABLE tmp_release_upload_menus (id BIGINT PRIMARY KEY);
  -- Retain the merged work-task permission buttons; grant only the selected IDs and ancestors.
  INSERT INTO tmp_release_upload_menus SELECT id FROM system_menu WHERE deleted=b'0' AND status=0
    AND id IN (900033,900034,900043,900230,900231);
  DROP TEMPORARY TABLE IF EXISTS tmp_release_upload_parents;
  CREATE TEMPORARY TABLE tmp_release_upload_parents (id BIGINT PRIMARY KEY);
  WHILE added > 0 DO
    TRUNCATE tmp_release_upload_parents;
    INSERT INTO tmp_release_upload_parents SELECT DISTINCT parent.id FROM tmp_release_upload_menus chosen
      JOIN system_menu child ON child.id=chosen.id
      JOIN system_menu parent ON parent.id=child.parent_id AND parent.deleted=b'0' AND parent.status=0;
    INSERT IGNORE INTO tmp_release_upload_menus SELECT id FROM tmp_release_upload_parents;
    SET added=ROW_COUNT();
  END WHILE;
  DROP TEMPORARY TABLE IF EXISTS tmp_release_upload_packages;
  CREATE TEMPORARY TABLE tmp_release_upload_packages (id BIGINT PRIMARY KEY);
  INSERT INTO tmp_release_upload_packages SELECT DISTINCT p.id FROM tmp_release_upload_roles r
    JOIN system_tenant t ON t.id=r.tenant_id AND t.deleted=b'0' AND t.status=0
    JOIN system_tenant_package p ON p.id=t.package_id AND p.deleted=b'0' AND p.status=0;
  IF EXISTS (SELECT 1 FROM system_tenant_package p JOIN tmp_release_upload_packages chosen ON chosen.id=p.id
      WHERE p.menu_ids IS NULL OR NOT JSON_VALID(p.menu_ids)) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Invalid attachment-role tenant package menus';
  END IF;
  START TRANSACTION;
  -- Remove market-release write actions from these upload roles; other roles retain their own permissions.
  UPDATE system_role_menu rm JOIN tmp_release_upload_roles r ON r.id=rm.role_id AND r.tenant_id=rm.tenant_id
    JOIN system_menu m ON m.id=rm.menu_id
    SET rm.deleted=b'1',rm.updater='release-attachment-upload',rm.update_time=NOW()
    WHERE rm.deleted=b'0' AND (m.permission IN ('mes:pro-production-release:pqc-approve','mes:pro-production-release:pqc-reject',
      'mes:pro-edhr-release:approve','mes:pro-edhr-release:reject','mes:pro-edhr-change:void')
      OR (m.parent_id=900033 AND m.type=3 AND m.permission<>'mes:pro-edhr-batch-execution:upload'));
  INSERT INTO system_role_menu (role_id,menu_id,creator,create_time,updater,update_time,deleted,tenant_id)
    SELECT r.id,m.id,'release-attachment-upload',NOW(),'release-attachment-upload',NOW(),b'0',r.tenant_id
    FROM tmp_release_upload_roles r CROSS JOIN tmp_release_upload_menus m
    WHERE NOT EXISTS (SELECT 1 FROM system_role_menu rm WHERE rm.role_id=r.id AND rm.menu_id=m.id
      AND rm.tenant_id=r.tenant_id AND rm.deleted=b'0');
  DROP TEMPORARY TABLE IF EXISTS tmp_release_upload_package_menus;
  CREATE TEMPORARY TABLE tmp_release_upload_package_menus (package_id BIGINT,menu_id BIGINT,PRIMARY KEY(package_id,menu_id));
  INSERT INTO tmp_release_upload_package_menus SELECT p.id,j.id FROM system_tenant_package p
    JOIN tmp_release_upload_packages chosen ON chosen.id=p.id
    JOIN JSON_TABLE(p.menu_ids,'$[*]' COLUMNS(id BIGINT PATH '$')) j;
  INSERT IGNORE INTO tmp_release_upload_package_menus SELECT p.id,m.id
    FROM tmp_release_upload_packages p CROSS JOIN tmp_release_upload_menus m;
  UPDATE system_tenant_package p JOIN (SELECT package_id,JSON_ARRAYAGG(menu_id) ids
    FROM tmp_release_upload_package_menus GROUP BY package_id) merged ON merged.package_id=p.id
    SET p.menu_ids=merged.ids,p.updater='release-attachment-upload',p.update_time=NOW();
  COMMIT;
  DROP TEMPORARY TABLE tmp_release_upload_package_menus;
  DROP TEMPORARY TABLE tmp_release_upload_packages;
  DROP TEMPORARY TABLE tmp_release_upload_parents;
  DROP TEMPORARY TABLE tmp_release_upload_menus;
  DROP TEMPORARY TABLE tmp_release_upload_roles;
END$$
DELIMITER ;
CALL grant_mes_release_attachment_upload();
DROP PROCEDURE grant_mes_release_attachment_upload;
