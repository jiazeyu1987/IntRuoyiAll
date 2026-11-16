-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260806_mes_pqc_leader_role_permission_tab,20261026_edhr_lifecycle_directories,20261112_test1_frontline_pqc_query_permission; type=permission; riskLevel=medium
-- Existing inspector/leader roles in tenants 1 and 186 only. No user-role assignment changes.
-- Both roles retain approval-center query access, frontline PQC and the existing hidden test page.
-- Leader adds only its workbench. No approval update or global approval administrator role is granted.
-- Shared button permissions are cloned beneath the leader page, so parent expansion cannot expose other jobs.
-- Recovery: restore captured role-menu/package rows, remove only these new button menus and advance authority revisions.
SET NAMES utf8mb4;
DROP PROCEDURE IF EXISTS fix_mes_pqc_role_navigation_boundary;
DELIMITER $$
CREATE PROCEDURE fix_mes_pqc_role_navigation_boundary()
BEGIN
  DECLARE current_revision BIGINT;
  DECLARE pqc_query_menu BIGINT;
  DECLARE EXIT HANDLER FOR SQLEXCEPTION BEGIN ROLLBACK; RESIGNAL; END;
  START TRANSACTION;
  IF (SELECT COUNT(*) FROM system_role WHERE tenant_id IN(1,186)
      AND code IN('pqc_permission','pqc_leader_permission') AND deleted=0 AND status=0)<>4
    OR (SELECT COUNT(*) FROM system_tenant WHERE id IN(1,186) AND deleted=0 AND status=0)<>2
    OR (SELECT COUNT(*) FROM system_approval_authority_revision WHERE tenant_id IN(1,186))<>2 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Missing active inspector/leader roles, tenants or authority revisions';
  END IF;
  SELECT revision INTO current_revision FROM system_approval_authority_revision WHERE tenant_id=1 FOR UPDATE;
  SELECT revision INTO current_revision FROM system_approval_authority_revision WHERE tenant_id=186 FOR UPDATE;
  DROP TEMPORARY TABLE IF EXISTS tmp_pqc_navigation_button;
  CREATE TEMPORARY TABLE tmp_pqc_navigation_button (id BIGINT PRIMARY KEY, source_id BIGINT, permission VARCHAR(100));
  INSERT INTO tmp_pqc_navigation_button VALUES
    (9915801,5321,'mes:dv-machinery:query'),
    (9915802,5551,'mes:pro-feedback:query'),
    (9915803,5552,'mes:pro-feedback:create'),
    (9915804,5631,'mes:qc-template:query'),
    (9915805,900312,'mes:pro-process-pool-team-leader:review'),
    (9915806,900313,'mes:pro-process-pool-team-leader:abnormal'),
    (9915807,900314,'mes:pro-process-pool-team-leader:maintain'),
    (9915808,9008301,'mes:pro-edhr-nonconformance-review:query'),
    (9915809,9008302,'mes:pro-edhr-nonconformance-review:create');
  IF (SELECT COUNT(*) FROM system_menu m JOIN tmp_pqc_navigation_button b ON b.source_id=m.id
      AND BINARY b.permission=BINARY m.permission WHERE m.type=3 AND m.deleted=0 AND m.status=0)<>9 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='PQC leader shared API menu definitions changed';
  END IF;
  IF EXISTS (SELECT 1 FROM system_menu m JOIN tmp_pqc_navigation_button b ON b.id=m.id
      WHERE m.deleted<>0 OR m.status<>0 OR m.type<>3 OR m.parent_id<>900435 OR BINARY m.permission<>BINARY b.permission) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='PQC hidden button ID collision';
  END IF;
  IF (SELECT COUNT(*) FROM system_menu WHERE deleted=0 AND status=0
      AND ((id=900435 AND component='mes/pro/processpool/PqcLeaderWorkbenchPage')
        OR (id=900438 AND component='mes/pro/edhr-batch/BatchPqcFillPage')
        OR id IN(900220,991516,991512,900440,900439)))<>7 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='PQC navigation prerequisites changed';
  END IF;
  IF (SELECT COUNT(*) FROM system_menu WHERE deleted=0 AND status=0 AND
      ((id=1200 AND path='/approval-center') OR (id=1207 AND parent_id=1200 AND path='todo')
        OR (id=1221 AND parent_id=1207 AND type=3 AND permission='bpm:task:query')
        OR (id=1201 AND parent_id=1200 AND path='my-initiated')
        OR (id=1208 AND parent_id=1200 AND path='done')
        OR (id=2713 AND parent_id=1200 AND path='cc' AND permission='bpm:process-instance-cc:query')))<>6 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Missing approval center task query menu';
  END IF;
  IF (SELECT COUNT(*) FROM system_menu WHERE parent_id=900438
      AND permission='mes:pro-feedback:query' AND type=3 AND deleted=0 AND status=0)<>1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Missing unique frontline PQC task query permission';
  END IF;
  SELECT id INTO pqc_query_menu FROM system_menu WHERE parent_id=900438
    AND permission='mes:pro-feedback:query' AND type=3 AND deleted=0 AND status=0;
  INSERT INTO system_menu (id,name,permission,type,sort,parent_id,path,icon,component,component_name,
      status,visible,keep_alive,always_show,creator,create_time,updater,update_time,deleted)
    SELECT b.id,CONCAT('PQC工作台-',m.name),b.permission,3,m.sort,900435,'','','','',
      0,b'0',b'1',b'0','pqc-navigation-boundary',NOW(),'pqc-navigation-boundary',NOW(),b'0'
    FROM tmp_pqc_navigation_button b JOIN system_menu m ON m.id=b.source_id
    WHERE NOT EXISTS (SELECT 1 FROM system_menu existing WHERE existing.id=b.id);
  DROP TEMPORARY TABLE IF EXISTS tmp_pqc_navigation_role;
  CREATE TEMPORARY TABLE tmp_pqc_navigation_role (id BIGINT PRIMARY KEY,tenant_id BIGINT,code VARCHAR(100));
  INSERT INTO tmp_pqc_navigation_role SELECT id,tenant_id,code FROM system_role
    WHERE tenant_id IN(1,186) AND code IN('pqc_permission','pqc_leader_permission') AND deleted=0 AND status=0;
  DROP TEMPORARY TABLE IF EXISTS tmp_pqc_navigation_grant;
  CREATE TEMPORARY TABLE tmp_pqc_navigation_grant (role_id BIGINT,tenant_id BIGINT,menu_id BIGINT,
    PRIMARY KEY(role_id,menu_id));
  INSERT INTO tmp_pqc_navigation_grant SELECT r.id,r.tenant_id,m.id FROM tmp_pqc_navigation_role r
    JOIN system_menu m ON m.id IN(1200,1201,1207,1208,1221,2713,900220,991516,991512,900438,900440);
  INSERT INTO tmp_pqc_navigation_grant SELECT r.id,r.tenant_id,pqc_query_menu FROM tmp_pqc_navigation_role r;
  INSERT INTO tmp_pqc_navigation_grant SELECT r.id,r.tenant_id,m.id FROM tmp_pqc_navigation_role r
    JOIN system_menu m ON m.id IN(900435,900439) WHERE r.code='pqc_leader_permission';
  INSERT INTO tmp_pqc_navigation_grant SELECT r.id,r.tenant_id,b.id FROM tmp_pqc_navigation_role r
    CROSS JOIN tmp_pqc_navigation_button b WHERE r.code='pqc_leader_permission';
  -- Only the already selected tenants' packages receive the same exact menus; no other role is granted them.
  IF EXISTS (SELECT 1 FROM system_tenant t LEFT JOIN system_tenant_package p
      ON p.id=t.package_id AND p.deleted=0 AND p.status=0 WHERE t.id IN(1,186)
      AND NOT(t.id=1 AND t.package_id=0) AND t.package_id<>-1
      AND (p.id IS NULL OR NOT JSON_VALID(p.menu_ids))) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Missing valid PQC tenant package';
  END IF;
  DROP TEMPORARY TABLE IF EXISTS tmp_pqc_navigation_package;
  CREATE TEMPORARY TABLE tmp_pqc_navigation_package (id BIGINT PRIMARY KEY);
  INSERT INTO tmp_pqc_navigation_package SELECT DISTINCT p.id FROM system_tenant t
    JOIN system_tenant_package p ON p.id=t.package_id AND p.deleted=0 WHERE t.id IN(1,186);
  DROP TEMPORARY TABLE IF EXISTS tmp_pqc_navigation_package_menu;
  CREATE TEMPORARY TABLE tmp_pqc_navigation_package_menu (package_id BIGINT,menu_id BIGINT,
    PRIMARY KEY(package_id,menu_id));
  INSERT IGNORE INTO tmp_pqc_navigation_package_menu SELECT p.id,j.id FROM system_tenant_package p
    JOIN tmp_pqc_navigation_package target ON target.id=p.id
    JOIN JSON_TABLE(p.menu_ids,'$[*]' COLUMNS(id BIGINT PATH '$')) j;
  INSERT IGNORE INTO tmp_pqc_navigation_package_menu SELECT p.id,g.menu_id FROM tmp_pqc_navigation_package p
    CROSS JOIN tmp_pqc_navigation_grant g;
  UPDATE system_tenant_package p JOIN (SELECT package_id,JSON_ARRAYAGG(menu_id) ids
    FROM tmp_pqc_navigation_package_menu GROUP BY package_id) merged ON merged.package_id=p.id
    SET p.menu_ids=merged.ids,p.updater='pqc-navigation-boundary',p.update_time=NOW();
  UPDATE system_role_menu rm JOIN tmp_pqc_navigation_role r ON r.id=rm.role_id AND r.tenant_id=rm.tenant_id
    LEFT JOIN tmp_pqc_navigation_grant g ON g.role_id=rm.role_id AND g.menu_id=rm.menu_id
    SET rm.deleted=b'1',rm.updater='pqc-navigation-boundary',rm.update_time=NOW()
    WHERE rm.deleted=0 AND g.menu_id IS NULL;
  INSERT INTO system_role_menu (role_id,menu_id,creator,create_time,updater,update_time,deleted,tenant_id)
    SELECT g.role_id,g.menu_id,'pqc-navigation-boundary',NOW(),'pqc-navigation-boundary',NOW(),b'0',g.tenant_id
    FROM tmp_pqc_navigation_grant g WHERE NOT EXISTS (SELECT 1 FROM system_role_menu rm
      WHERE rm.role_id=g.role_id AND rm.tenant_id=g.tenant_id AND rm.menu_id=g.menu_id AND rm.deleted=0);
  UPDATE system_approval_authority_revision SET revision=revision+1,updated_at=NOW() WHERE tenant_id IN(1,186);
  COMMIT;
  DROP TEMPORARY TABLE tmp_pqc_navigation_package_menu;
  DROP TEMPORARY TABLE tmp_pqc_navigation_package;
  DROP TEMPORARY TABLE tmp_pqc_navigation_grant;
  DROP TEMPORARY TABLE tmp_pqc_navigation_role;
  DROP TEMPORARY TABLE tmp_pqc_navigation_button;
END$$
DELIMITER ;
CALL fix_mes_pqc_role_navigation_boundary();
DROP PROCEDURE fix_mes_pqc_role_navigation_boundary;
