-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20261104_dcc_independent_libraries; type=data; riskLevel=medium
-- Target roles: one migrated role per tenant/library/access level, assigned only to the original users.
-- Retire the old rules in the same transaction so reruns cannot restore a subsequently revoked role.
SET NAMES utf8mb4;
DROP PROCEDURE IF EXISTS migrate_dcc_library_roles_20261105;
DELIMITER $$
CREATE PROCEDURE migrate_dcc_library_roles_20261105()
BEGIN
 DECLARE EXIT HANDLER FOR SQLEXCEPTION BEGIN ROLLBACK; RESIGNAL; END;
 DROP TEMPORARY TABLE IF EXISTS dcc_library_role_nodes;
 CREATE TEMPORARY TABLE dcc_library_role_nodes(id BIGINT PRIMARY KEY, scope VARCHAR(16), level VARCHAR(8), name VARCHAR(30), code VARCHAR(100), permission VARCHAR(100)) DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
 INSERT INTO dcc_library_role_nodes VALUES
 (605072141,'QMS','VIEW','QMS文件夹浏览','dcc_lib_qms_view_legacy','dcc:library:qms:view'),
 (605072142,'QMS','EDIT','QMS文件夹编辑','dcc_lib_qms_edit_legacy','dcc:library:qms:edit'),
 (605072143,'GENERAL','VIEW','通用文件夹浏览','dcc_lib_general_view_legacy','dcc:library:general:view'),
 (605072144,'GENERAL','EDIT','通用文件夹编辑','dcc_lib_general_edit_legacy','dcc:library:general:edit');
 IF NOT EXISTS(SELECT 1 FROM system_menu WHERE id=6807 AND permission='dcc:controlled-file:query' AND deleted=0 AND status=0) THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='DCC browser menu missing or disabled';
 END IF;
 IF EXISTS(SELECT 1 FROM system_menu m JOIN dcc_library_role_nodes n ON m.id=n.id OR m.permission=n.permission
           WHERE m.id<>n.id OR m.permission<>n.permission OR m.deleted<>0 OR m.status<>0 OR m.parent_id<>6807 OR m.type<>3) THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='DCC library menu identity conflict';
 END IF;
 IF EXISTS(SELECT 1 FROM system_tenant_package WHERE deleted=0 AND NOT JSON_VALID(menu_ids)) THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Invalid tenant package menu JSON';
 END IF;
 START TRANSACTION;
 DROP TEMPORARY TABLE IF EXISTS dcc_library_legacy_grants;
 CREATE TEMPORARY TABLE dcc_library_legacy_grants AS
 SELECT r.id rule_id,r.tenant_id,r.subject_type,r.subject_id user_id,r.access_level,r.valid_from,r.expire_time,p.library_scope
 FROM dcc_project_access_rule r JOIN dcc_project_code p ON p.id=r.dcc_project_code_id AND p.tenant_id=r.tenant_id
 WHERE p.library_scope IN('QMS','GENERAL') AND p.deleted=0 AND r.deleted=0 AND r.active=1;
 -- The retired UI only created permanent USER VIEW/EDIT grants. Other grant types need an explicit migration design.
 IF EXISTS(SELECT 1 FROM dcc_library_legacy_grants WHERE subject_type<>'USER' OR access_level NOT IN('VIEW','EDIT') OR valid_from IS NOT NULL OR expire_time IS NOT NULL) THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Library migration requires permanent USER VIEW/EDIT grants';
 END IF;
 IF EXISTS(SELECT 1 FROM dcc_library_legacy_grants g LEFT JOIN system_users u ON u.id=g.user_id AND u.tenant_id=g.tenant_id AND u.deleted=0 WHERE u.id IS NULL) THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Library grant user missing in tenant';
 END IF;
 IF EXISTS(SELECT 1 FROM dcc_library_legacy_grants g LEFT JOIN system_approval_authority_revision a ON a.tenant_id=g.tenant_id WHERE a.tenant_id IS NULL) THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Library tenant authority revision missing';
 END IF;
 UPDATE system_approval_authority_revision a JOIN (SELECT DISTINCT tenant_id FROM dcc_library_legacy_grants) g ON g.tenant_id=a.tenant_id
 SET a.revision=a.revision+1,a.updated_at=NOW();
 IF EXISTS(SELECT 1 FROM system_role r JOIN dcc_library_role_nodes n ON n.code=r.code
           WHERE r.deleted<>0 OR r.status<>0 OR COALESCE(r.remark,'')<>'DCC library role migration 20261105') THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Library migration role code conflict';
 END IF;
 IF EXISTS(SELECT 1 FROM system_role_category WHERE code='dcc_library_legacy' AND (deleted<>0 OR status<>0 OR COALESCE(remark,'')<>'DCC library role migration 20261105')) THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Library migration category code conflict';
 END IF;
 INSERT INTO system_menu(id,name,permission,type,sort,parent_id,path,icon,component,component_name,status,visible,keep_alive,always_show,creator,updater)
 SELECT n.id,n.name,n.permission,3,96+n.id-605072141,6807,'','','','',0,1,1,1,'dcc-lib-role','dcc-lib-role'
 FROM dcc_library_role_nodes n WHERE NOT EXISTS(SELECT 1 FROM system_menu m WHERE m.id=n.id);
 INSERT INTO system_role_category(name,code,sort,status,remark,tenant_id,creator,updater)
 SELECT DISTINCT '文件夹原授权','dcc_library_legacy',90,0,'DCC library role migration 20261105',g.tenant_id,'dcc-lib-role','dcc-lib-role'
 FROM dcc_library_legacy_grants g WHERE NOT EXISTS(SELECT 1 FROM system_role_category c WHERE c.code='dcc_library_legacy' AND c.tenant_id=g.tenant_id AND c.deleted=0);
 INSERT INTO system_role(name,code,sort,category_id,data_scope,status,type,remark,tenant_id,creator,updater)
 SELECT DISTINCT CONCAT(n.name,'（原授权）'),n.code,n.id-605072140,c.id,5,0,2,'DCC library role migration 20261105',g.tenant_id,'dcc-lib-role','dcc-lib-role'
 FROM dcc_library_legacy_grants g JOIN dcc_library_role_nodes n ON n.scope=g.library_scope AND n.level=g.access_level
 JOIN system_role_category c ON c.tenant_id=g.tenant_id AND c.code='dcc_library_legacy' AND c.deleted=0
 WHERE NOT EXISTS(SELECT 1 FROM system_role r WHERE r.code=n.code AND r.tenant_id=g.tenant_id AND r.deleted=0);
 INSERT INTO system_role_menu(role_id,menu_id,tenant_id,creator,updater)
 SELECT DISTINCT r.id,m.id,r.tenant_id,'dcc-lib-role','dcc-lib-role' FROM system_role r
 JOIN dcc_library_role_nodes n ON r.code=n.code JOIN system_menu m ON m.id IN(6800,6807,n.id)
 JOIN dcc_library_legacy_grants g ON g.tenant_id=r.tenant_id AND g.library_scope=n.scope AND g.access_level=n.level
 WHERE r.deleted=0 AND r.remark='DCC library role migration 20261105'
 AND NOT EXISTS(SELECT 1 FROM system_role_menu rm WHERE rm.role_id=r.id AND rm.menu_id=m.id AND rm.tenant_id=r.tenant_id AND rm.deleted=0);
 INSERT INTO system_user_role(user_id,role_id,tenant_id,creator,updater)
 SELECT DISTINCT g.user_id,r.id,g.tenant_id,'dcc-lib-role','dcc-lib-role' FROM dcc_library_legacy_grants g
 JOIN dcc_library_role_nodes n ON n.scope=g.library_scope AND n.level=g.access_level
 JOIN system_role r ON r.code=n.code AND r.tenant_id=g.tenant_id AND r.deleted=0
 WHERE NOT EXISTS(SELECT 1 FROM system_user_role ur WHERE ur.user_id=g.user_id AND ur.role_id=r.id AND ur.tenant_id=g.tenant_id AND ur.deleted=0);
 UPDATE system_tenant_package SET menu_ids=JSON_MERGE_PRESERVE(menu_ids,JSON_ARRAY(605072141,605072142,605072143,605072144)),updater='dcc-lib-role',update_time=NOW()
 WHERE deleted=0 AND JSON_CONTAINS(menu_ids,'6807','$') AND NOT JSON_CONTAINS(menu_ids,'605072141','$');
 IF EXISTS(SELECT 1 FROM system_tenant_package p CROSS JOIN dcc_library_role_nodes n
           WHERE p.deleted=0 AND JSON_CONTAINS(p.menu_ids,'6807','$') AND NOT JSON_CONTAINS(p.menu_ids,CAST(n.id AS CHAR),'$')) THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Incomplete library tenant package permissions';
 END IF;
 IF EXISTS(SELECT 1 FROM dcc_library_legacy_grants g JOIN dcc_library_role_nodes n ON n.scope=g.library_scope AND n.level=g.access_level
           WHERE NOT EXISTS(SELECT 1 FROM system_role r JOIN system_user_role ur ON ur.role_id=r.id AND ur.tenant_id=r.tenant_id AND ur.deleted=0
                            JOIN system_role_menu rm ON rm.role_id=r.id AND rm.tenant_id=r.tenant_id AND rm.deleted=0 AND rm.menu_id=n.id
                            WHERE r.code=n.code AND r.tenant_id=g.tenant_id AND r.deleted=0 AND r.status=0 AND ur.user_id=g.user_id)) THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Library grant preservation verification failed';
 END IF;
 UPDATE dcc_project_access_rule r JOIN dcc_library_legacy_grants g ON g.rule_id=r.id AND g.tenant_id=r.tenant_id
 SET r.active=0,r.updater='dcc-lib-role',r.update_time=NOW();
 COMMIT;
 DROP TEMPORARY TABLE dcc_library_legacy_grants;
 DROP TEMPORARY TABLE dcc_library_role_nodes;
END$$
DELIMITER ;
CALL migrate_dcc_library_roles_20261105();
DROP PROCEDURE migrate_dcc_library_roles_20261105;
