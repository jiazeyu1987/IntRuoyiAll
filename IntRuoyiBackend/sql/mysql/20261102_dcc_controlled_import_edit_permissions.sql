-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20261102_dcc_controlled_import_provenance; type=data; riskLevel=medium
-- Only formal tenant-1 doc_control role 910233 receives import + independent edit qualification.
-- Current audited membership: 910250,1,910325,1468,910329,910341,910495 (7 users).
-- This does not create project/type EDIT rules. Those remain separately authorized per target.
-- Packages already containing DCC root 6800 receive the two new menu definitions; no new role is inferred.
SET NAMES utf8mb4;
DROP PROCEDURE IF EXISTS migrate_dcc_controlled_import_menus_20261102;
DELIMITER $$
CREATE PROCEDURE migrate_dcc_controlled_import_menus_20261102()
BEGIN
 IF NOT EXISTS(SELECT 1 FROM system_menu WHERE id=6800 AND deleted=b'0')
    OR NOT EXISTS(SELECT 1 FROM system_menu WHERE id=6807 AND parent_id=6800 AND permission='dcc:controlled-file:query' AND deleted=b'0')
    OR NOT EXISTS(SELECT 1 FROM system_role WHERE id=910233 AND tenant_id=1 AND code='doc_control' AND status=0 AND deleted=b'0') THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Exact DCC parent/formal doc_control role missing';
 END IF;
 IF EXISTS(SELECT 1 FROM system_tenant_package WHERE deleted=b'0' AND NOT JSON_VALID(menu_ids)) THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Invalid tenant package menu JSON';
 END IF;
 IF EXISTS(SELECT 1 FROM system_menu WHERE (id=605072130 OR permission='dcc:controlled-file:import')
   AND NOT(id=605072130 AND permission='dcc:controlled-file:import' AND parent_id=6800 AND component='dcc/controlled-file/controlled-upload/index' AND deleted=b'0'))
 OR EXISTS(SELECT 1 FROM system_menu WHERE (id=605072131 OR permission='dcc:controlled-file:edit')
   AND NOT(id=605072131 AND permission='dcc:controlled-file:edit' AND parent_id=6807 AND type=3 AND deleted=b'0')) THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='DCC import/edit menu identity conflict';
 END IF;
 INSERT INTO system_menu(id,name,permission,type,sort,parent_id,path,icon,component,component_name,status,visible,keep_alive,always_show,creator,create_time,updater,update_time,deleted)
 SELECT 605072130,'受控文件上传','dcc:controlled-file:import',2,25,6800,'controlled-upload','ep:upload','dcc/controlled-file/controlled-upload/index','DccControlledFileControlledUpload',0,b'1',b'1',b'1','word1030-import',NOW(),'word1030-import',NOW(),b'0'
 WHERE NOT EXISTS(SELECT 1 FROM system_menu WHERE id=605072130);
 INSERT INTO system_menu(id,name,permission,type,sort,parent_id,path,icon,component,component_name,status,visible,keep_alive,always_show,creator,create_time,updater,update_time,deleted)
 SELECT 605072131,'DCC文件独立编辑资格','dcc:controlled-file:edit',3,90,6807,'','','','',0,b'1',b'1',b'1','word1030-import',NOW(),'word1030-import',NOW(),b'0'
 WHERE NOT EXISTS(SELECT 1 FROM system_menu WHERE id=605072131);
 UPDATE system_role_menu SET deleted=b'0',updater='word1030-import',update_time=NOW()
 WHERE tenant_id=1 AND role_id=910233 AND menu_id IN(605072130,605072131) AND deleted=b'1';
 INSERT INTO system_role_menu(role_id,menu_id,creator,create_time,updater,update_time,deleted,tenant_id)
 SELECT 910233,m.id,'word1030-import',NOW(),'word1030-import',NOW(),b'0',1 FROM system_menu m
 WHERE m.id IN(605072130,605072131) AND NOT EXISTS(SELECT 1 FROM system_role_menu r WHERE r.role_id=910233 AND r.tenant_id=1 AND r.menu_id=m.id AND r.deleted=b'0');
 UPDATE system_tenant_package SET menu_ids=JSON_ARRAY_APPEND(menu_ids,'$',605072130),updater='word1030-import',update_time=NOW()
 WHERE deleted=b'0' AND JSON_CONTAINS(menu_ids,'6800','$') AND NOT JSON_CONTAINS(menu_ids,'605072130','$');
 UPDATE system_tenant_package SET menu_ids=JSON_ARRAY_APPEND(menu_ids,'$',605072131),updater='word1030-import',update_time=NOW()
 WHERE deleted=b'0' AND JSON_CONTAINS(menu_ids,'6800','$') AND NOT JSON_CONTAINS(menu_ids,'605072131','$');
END$$
DELIMITER ;
CALL migrate_dcc_controlled_import_menus_20261102();
DROP PROCEDURE migrate_dcc_controlled_import_menus_20261102;
