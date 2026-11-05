-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20261001_dcc_b_manual_project_folder_origin,20261102_dcc_controlled_import_edit_permissions,20261104_dcc_archive_download_request,20261104_dcc_project_reviewer_approval_center_access; type=data; riskLevel=medium
SET NAMES utf8mb4;
DROP PROCEDURE IF EXISTS migrate_dcc_libraries_20261104;
DELIMITER $$
CREATE PROCEDURE migrate_dcc_libraries_20261104()
BEGIN
 DECLARE EXIT HANDLER FOR SQLEXCEPTION BEGIN ROLLBACK; RESIGNAL; END;
 IF NOT EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='dcc_project_code' AND column_name='library_scope') THEN
  ALTER TABLE dcc_project_code ADD COLUMN library_scope VARCHAR(16) NOT NULL DEFAULT 'PROJECT';
 END IF;
 IF EXISTS(SELECT 1 FROM dcc_project_code WHERE library_scope NOT IN('PROJECT','QMS','GENERAL')) THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Invalid DCC library scope';
 END IF;
 IF EXISTS(SELECT tenant_id,library_scope FROM dcc_project_code WHERE library_scope<>'PROJECT' AND deleted=0 GROUP BY tenant_id,library_scope HAVING COUNT(*)>1) THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Duplicate DCC library scope';
 END IF;
 IF EXISTS(SELECT 1 FROM system_menu WHERE id=605072140 AND permission<>'dcc:controlled-file:archive:query') THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='DCC archive menu identity conflict';
 END IF;
 IF NOT EXISTS(SELECT 1 FROM system_menu WHERE id=6807 AND permission='dcc:controlled-file:query' AND deleted=0) THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='DCC browser menu missing';
 END IF;
 IF EXISTS(SELECT 1 FROM system_tenant_package WHERE deleted=0 AND NOT JSON_VALID(menu_ids)) THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Invalid tenant package menu JSON';
 END IF;
 START TRANSACTION;
 INSERT INTO dcc_project_code(project_name,project_code,status,library_scope,tenant_id,creator,updater,create_time,update_time,deleted)
 SELECT s.name,CONCAT('DCC-LIBRARY-',s.scope),'ENABLE',s.scope,t.id,'word1104','word1104',NOW(),NOW(),0
 FROM system_tenant t CROSS JOIN (SELECT 'QMS' scope,'QMS文件夹' name UNION ALL SELECT 'GENERAL','通用文件夹') s
 WHERE t.deleted=0 AND NOT EXISTS(SELECT 1 FROM dcc_project_code p WHERE p.tenant_id=t.id AND p.library_scope=s.scope AND p.deleted=0);
 INSERT INTO dcc_project_folder(project_code_id,parent_id,name,sort_order,active,source_template_id,source_node_key,tenant_id,creator,updater,create_time,update_time,deleted)
 SELECT p.id,0,p.project_name,0,b'1',NULL,NULL,p.tenant_id,'word1104','word1104',NOW(),NOW(),b'0'
 FROM dcc_project_code p WHERE p.library_scope IN('QMS','GENERAL') AND p.deleted=0
 AND NOT EXISTS(SELECT 1 FROM dcc_project_folder f WHERE f.project_code_id=p.id AND f.tenant_id=p.tenant_id AND f.deleted=0);
 INSERT INTO system_menu(id,name,permission,type,sort,parent_id,path,icon,component,component_name,status,visible,keep_alive,always_show,creator,create_time,updater,update_time,deleted)
 SELECT 605072140,'文控作废归档','dcc:controlled-file:archive:query',3,95,6807,'','','','',0,b'1',b'1',b'1','word1104',NOW(),'word1104',NOW(),b'0'
 WHERE NOT EXISTS(SELECT 1 FROM system_menu WHERE id=605072140);
 INSERT INTO system_role_menu(role_id,menu_id,creator,create_time,updater,update_time,deleted,tenant_id)
 SELECT r.id,605072140,'word1104',NOW(),'word1104',NOW(),b'0',r.tenant_id FROM system_role r
 WHERE r.code IN('doc_control','wenkong') AND r.status=0 AND r.deleted=0
 AND NOT EXISTS(SELECT 1 FROM system_role_menu m WHERE m.role_id=r.id AND m.menu_id=605072140 AND m.tenant_id=r.tenant_id AND m.deleted=0);
 UPDATE system_tenant_package SET menu_ids=JSON_ARRAY_APPEND(menu_ids,'$',605072140),updater='word1104',update_time=NOW()
 WHERE deleted=0 AND JSON_CONTAINS(menu_ids,'6807','$') AND NOT JSON_CONTAINS(menu_ids,'605072140','$');
 COMMIT;
END$$
DELIMITER ;
CALL migrate_dcc_libraries_20261104();
DROP PROCEDURE migrate_dcc_libraries_20261104;
