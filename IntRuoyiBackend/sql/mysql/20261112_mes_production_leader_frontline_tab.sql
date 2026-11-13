-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260804_mes_edhr_qa_menu,20260806_mes_pqc_leader_role_permission_tab,20261026_edhr_lifecycle_directories; type=permission; riskLevel=medium
-- Only existing production_leader_permission roles are changed. Shared leader APIs remain granted.
-- Recovery: restore the captured target role-menu/package rows and advance authority revisions.
SET NAMES utf8mb4;
DROP PROCEDURE IF EXISTS fix_production_leader_frontline_tab;
DELIMITER $$
CREATE PROCEDURE fix_production_leader_frontline_tab()
BEGIN
  DECLARE done BOOLEAN DEFAULT FALSE;
  DECLARE target_tenant BIGINT;
  DECLARE locked_revision BIGINT;
  DECLARE added INT DEFAULT 1;
  DECLARE targets CURSOR FOR SELECT DISTINCT r.tenant_id FROM system_role r
    JOIN system_tenant t ON t.id=r.tenant_id AND t.deleted=0 AND t.status=0
    WHERE r.code='production_leader_permission' AND r.deleted=0 AND r.status=0 ORDER BY r.tenant_id;
  DECLARE CONTINUE HANDLER FOR NOT FOUND SET done=TRUE;
  DECLARE EXIT HANDLER FOR SQLEXCEPTION BEGIN ROLLBACK; RESIGNAL; END;
  START TRANSACTION;
  IF (SELECT COUNT(*) FROM system_menu WHERE deleted=0 AND status=0 AND
      ((id=900437 AND type=2 AND component='mes/pro/edhr-batch/BatchProductionFillPage'
        AND permission='mes:pro-edhr-batch-execution:query')
       OR (id=900435 AND type=2 AND component='mes/pro/processpool/PqcLeaderWorkbenchPage'
        AND permission='mes:pro-process-pool-pqc-leader:query')
       OR (id=5551 AND type=3 AND permission='mes:pro-feedback:query')
       OR (id=5552 AND type=3 AND permission='mes:pro-feedback:create')))<>4 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Missing or changed production/PQC frontline menus';
  END IF;
  DROP TEMPORARY TABLE IF EXISTS tmp_production_frontline_role;
  CREATE TEMPORARY TABLE tmp_production_frontline_role (id BIGINT PRIMARY KEY,tenant_id BIGINT,package_id BIGINT);
  INSERT INTO tmp_production_frontline_role SELECT r.id,r.tenant_id,t.package_id FROM system_role r
    JOIN system_tenant t ON t.id=r.tenant_id AND t.deleted=0 AND t.status=0
    WHERE r.code='production_leader_permission' AND r.deleted=0 AND r.status=0;
  IF NOT EXISTS (SELECT 1 FROM tmp_production_frontline_role) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Missing active production leader role';
  END IF;
  IF EXISTS (SELECT 1 FROM tmp_production_frontline_role GROUP BY tenant_id HAVING COUNT(*)>1) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Duplicate production leader role in tenant';
  END IF;
  IF EXISTS (SELECT 1 FROM tmp_production_frontline_role r
      LEFT JOIN system_approval_authority_revision a ON a.tenant_id=r.tenant_id WHERE a.tenant_id IS NULL) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Missing authority revision';
  END IF;
  IF EXISTS (SELECT 1 FROM tmp_production_frontline_role r
      LEFT JOIN system_tenant_package p ON p.id=r.package_id AND p.deleted=0 AND p.status=0
      WHERE NOT (r.tenant_id=1 AND r.package_id=0) AND r.package_id<>-1
        AND (p.id IS NULL OR NOT JSON_VALID(p.menu_ids))) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Missing valid tenant package';
  END IF;
  OPEN targets;
  lock_loop: LOOP
    FETCH targets INTO target_tenant;
    IF done THEN LEAVE lock_loop; END IF;
    SELECT revision INTO locked_revision FROM system_approval_authority_revision
      WHERE tenant_id=target_tenant FOR UPDATE;
  END LOOP;
  CLOSE targets;
  DROP TEMPORARY TABLE IF EXISTS tmp_production_frontline_menu;
  CREATE TEMPORARY TABLE tmp_production_frontline_menu (id BIGINT PRIMARY KEY);
  INSERT INTO tmp_production_frontline_menu VALUES (900437),(5551),(5552);
  -- Only directory ancestors of the frontline page; button parents must not expose other pages.
  DROP TEMPORARY TABLE IF EXISTS tmp_production_frontline_parent;
  CREATE TEMPORARY TABLE tmp_production_frontline_parent (id BIGINT PRIMARY KEY);
  WHILE added>0 DO
    TRUNCATE tmp_production_frontline_parent;
    INSERT INTO tmp_production_frontline_parent SELECT DISTINCT p.id FROM tmp_production_frontline_menu chosen
      JOIN system_menu child ON child.id=chosen.id JOIN system_menu p ON p.id=child.parent_id
      WHERE p.type=1 AND p.deleted=0 AND p.status=0;
    INSERT IGNORE INTO tmp_production_frontline_menu SELECT id FROM tmp_production_frontline_parent;
    SET added=ROW_COUNT();
  END WHILE;
  DROP TEMPORARY TABLE IF EXISTS tmp_production_frontline_package_menu;
  CREATE TEMPORARY TABLE tmp_production_frontline_package_menu
    (package_id BIGINT,menu_id BIGINT,PRIMARY KEY(package_id,menu_id));
  INSERT IGNORE INTO tmp_production_frontline_package_menu SELECT p.id,j.id FROM system_tenant_package p
    JOIN (SELECT DISTINCT package_id FROM tmp_production_frontline_role) r ON r.package_id=p.id
    JOIN JSON_TABLE(p.menu_ids,'$[*]' COLUMNS(id BIGINT PATH '$')) j WHERE p.deleted=0;
  INSERT IGNORE INTO tmp_production_frontline_package_menu SELECT p.id,m.id FROM system_tenant_package p
    JOIN (SELECT DISTINCT package_id FROM tmp_production_frontline_role) r ON r.package_id=p.id
    CROSS JOIN tmp_production_frontline_menu m WHERE p.deleted=0;
  UPDATE system_tenant_package p JOIN
    (SELECT package_id,JSON_ARRAYAGG(menu_id) ids FROM tmp_production_frontline_package_menu GROUP BY package_id) merged
    ON merged.package_id=p.id SET p.menu_ids=merged.ids,p.updater='production-frontline-tab',p.update_time=NOW();
  INSERT INTO system_role_menu (role_id,menu_id,creator,create_time,updater,update_time,deleted,tenant_id)
    SELECT r.id,m.id,'production-frontline-tab',NOW(),'production-frontline-tab',NOW(),b'0',r.tenant_id
    FROM tmp_production_frontline_role r CROSS JOIN tmp_production_frontline_menu m
    WHERE NOT EXISTS (SELECT 1 FROM system_role_menu rm WHERE rm.role_id=r.id AND rm.tenant_id=r.tenant_id
      AND rm.menu_id=m.id AND rm.deleted=0);
  UPDATE system_role_menu rm JOIN tmp_production_frontline_role r ON r.id=rm.role_id AND r.tenant_id=rm.tenant_id
    JOIN system_menu m ON m.id=rm.menu_id AND m.deleted=0
    SET rm.deleted=b'1',rm.updater='production-frontline-tab',rm.update_time=NOW()
    WHERE rm.deleted=0 AND m.permission='mes:pro-process-pool-pqc-leader:query';
  IF EXISTS (SELECT 1 FROM tmp_production_frontline_role r CROSS JOIN tmp_production_frontline_menu m
    WHERE NOT EXISTS (SELECT 1 FROM system_role_menu rm WHERE rm.role_id=r.id AND rm.tenant_id=r.tenant_id
      AND rm.menu_id=m.id AND rm.deleted=0)) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Production frontline role grant incomplete';
  END IF;
  UPDATE system_approval_authority_revision a JOIN
    (SELECT DISTINCT tenant_id FROM tmp_production_frontline_role) r ON r.tenant_id=a.tenant_id
    SET a.revision=a.revision+1,a.updated_at=NOW();
  COMMIT;
  DROP TEMPORARY TABLE tmp_production_frontline_package_menu;
  DROP TEMPORARY TABLE tmp_production_frontline_parent;
  DROP TEMPORARY TABLE tmp_production_frontline_menu;
  DROP TEMPORARY TABLE tmp_production_frontline_role;
END$$
DELIMITER ;
CALL fix_production_leader_frontline_tab();
DROP PROCEDURE fix_production_leader_frontline_tab;
